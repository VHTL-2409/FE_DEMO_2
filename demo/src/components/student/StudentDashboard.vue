<template>
  <div class="bg-[var(--ds-bg)] min-h-full">
    <StudentDashboardLayout>

      <!-- Hero slot -->
      <template #hero>
        <StudentHero
          :student-name="studentName"
          :exam-count="upcomingExams.length"
          :pending-count="pendingCount"
          :new-score-count="newScoresCount"
          :total-attempts="attemptCount"
          :avg-score="avgScoreDisplay"
        />
      </template>

      <!-- KPI strip -->
      <template #kpis>
        <StudentStatCard
          label="Kỳ thi sắp tới"
          :value="upcomingExams.length"
          icon="event"
          accent="primary"
          :sub="upcomingExams.length > 0 ? upcomingExams[0]?.title : 'Không có'"
          :loading="isLoadingAttempts"
        />
        <StudentStatCard
          label="Chưa hoàn thành"
          :value="pendingCount"
          icon="pending_actions"
          accent="warning"
          sub="Cần hoàn thành"
          :loading="isLoadingAttempts"
        />
        <StudentStatCard
          label="Điểm trung bình"
          :value="avgScoreValue"
          icon="insights"
          :accent="avgScoreAccent"
          :sub="`${attemptCount} bài thi`"
          :loading="isLoadingAttempts"
        />
        <StudentStatCard
          label="Tỷ lệ đạt"
          :value="passRateDisplay"
          icon="check_circle"
          :accent="passRateAccent"
          :sub="`Điểm >= 5/10`"
          :loading="isLoadingAttempts"
        />
      </template>

      <!-- Main column -->
      <template #main>
        <UpcomingExamCard
          :upcoming-exams="upcomingExams"
          :loading="isLoadingAttempts"
          :display-limit="3"
          @exam-click="goToUpcoming"
          @see-all="goToSchedule"
        />

        <!-- Recent Results -->
        <RecentResultCard
          :results="recentResults"
          :loading="isLoadingAttempts"
          :display-limit="5"
          @result-click="goToResult"
          @see-all="goToStudyHistory"
        />
      </template>

    </StudentDashboardLayout>
  </div>
</template>

<script setup>
import { computed, defineAsyncComponent, onMounted, ref, shallowRef } from 'vue'
import { useRouter } from 'vue-router'
import { listMyAttempts } from '../../services/attemptService'
import { getMyClasses, getStudentClassExams } from '../../services/classService'
import { useToast } from '../../composables/useToast'
import { formatScoreTen, scorePercentValue } from '../../utils/attemptResult'
import { buildResultQuery, buildWaitingRoomQuery } from '../../services/studentExamContextStorage'

// Dashboard components - using dynamic imports for better initial load
const StudentDashboardLayout = defineAsyncComponent(() =>
  import('./dashboard/StudentDashboardLayout.vue')
)
const StudentHero = defineAsyncComponent(() =>
  import('./dashboard/StudentHero.vue')
)
const StudentStatCard = defineAsyncComponent(() =>
  import('./dashboard/StudentStatCard.vue')
)
const UpcomingExamCard = defineAsyncComponent(() =>
  import('./dashboard/UpcomingExamCard.vue')
)
const RecentResultCard = defineAsyncComponent(() =>
  import('./dashboard/RecentResultCard.vue')
)

const router = useRouter()
const toast = useToast()

// Data - use shallowRef for large arrays to avoid deep reactivity overhead
const attempts = shallowRef([])
const isLoadingAttempts = ref(false)
const studentName = ref('Học sinh')

const upcomingExams = shallowRef([])

// Computed: submitted attempts - memoized with cache
const submittedAttempts = computed(() =>
  attempts.value.filter(a =>
    ['SUBMITTED', 'AUTO_SUBMITTED', 'COMPLETED'].includes(String(a.status || '').toUpperCase())
  )
)

const attemptCount = computed(() => submittedAttempts.value.length)

// Pending (in-progress but not submitted)
const pendingAttempts = computed(() =>
  attempts.value.filter(a =>
    ['IN_PROGRESS', 'ACTIVE', 'PAUSED'].includes(String(a.status || '').toUpperCase())
  )
)
const pendingCount = computed(() => pendingAttempts.value.length)

// Average score - optimized calculation
const avgScoreValue = computed(() => {
  const submitted = submittedAttempts.value
  if (!submitted.length) return null
  const total = submitted.reduce((sum, a) => sum + Number(a.score || 0), 0)
  return formatScoreTen(total / submitted.length)
})

const avgScoreDisplay = computed(() => {
  if (!avgScoreValue.value) return '—'
  return avgScoreValue.value
})

const avgScoreAccent = computed(() => {
  const v = scorePercentValue((Number(avgScoreValue.value) || 0) * 10)
  if (!v) return 'neutral'
  if (v >= 80) return 'success'
  if (v >= 50) return 'primary'
  return 'danger'
})

// Pass rate
const passRateValue = computed(() => {
  const submitted = submittedAttempts.value
  if (!submitted.length) return 0
  const passCount = submitted.filter(a => Number(a.score || 0) >= 50).length
  return (passCount / submitted.length) * 100
})

const passRateDisplay = computed(() => {
  if (!submittedAttempts.value.length) return '—'
  return passRateValue.value.toFixed(0) + '%'
})

const passRateAccent = computed(() => {
  const v = passRateValue.value
  if (v >= 70) return 'success'
  if (v >= 50) return 'warning'
  return 'danger'
})

// New scores (attempts submitted in last 7 days)
const newScoresCount = computed(() => {
  const sevenDaysAgo = Date.now() - 7 * 24 * 60 * 60 * 1000
  return submittedAttempts.value.filter(a => {
    const submitted = new Date(a.submittedAt || 0).getTime()
    return submitted >= sevenDaysAgo
  }).length
})

// Recent results - cached and memoized
const recentResults = computed(() => {
  return [...submittedAttempts.value]
    .sort((a, b) => {
      const aTime = new Date(a.submittedAt || a.startedAt || 0).getTime()
      const bTime = new Date(b.submittedAt || b.startedAt || 0).getTime()
      return (Number.isNaN(bTime) ? 0 : bTime) - (Number.isNaN(aTime) ? 0 : aTime)
    })
    .slice(0, 8)
    .map(a => ({
      id: a.id,
      attemptId: a.id,
      examTitle: a.examTitle || 'Bài thi',
      title: a.examTitle || 'Bài thi',
      score: Number(a.score || 0),
      submittedAt: a.submittedAt,
      completedAt: a.submittedAt,
      timeTaken: a.startedAt && a.submittedAt
        ? `${Math.max(1, Math.round((new Date(a.submittedAt).getTime() - new Date(a.startedAt).getTime()) / 60000))} phút`
        : null
    }))
})

// Navigation
const goToSchedule = () => router.push({ path: '/student/study-history', query: { tab: 'upcoming' } })
const goToStudyHistory = () => router.push({ path: '/student/study-history', query: { tab: 'exam' } })
const goToResult = (result) => {
  router.push({
    path: '/student/exam-result',
    query: buildResultQuery({
      attemptId: result.attemptId,
      examTitle: result.title || result.examTitle
    })
  })
}

const goToUpcoming = (exam) => {
  router.push({
    path: '/student/exam-waiting-room',
    query: buildWaitingRoomQuery(exam)
  })
}

// Load data — reads from sessionStorage if prefetched by RedirectPage
const loadData = async () => {
  isLoadingAttempts.value = true
  try {
    const cached = sessionStorage.getItem('prefetch_student_data')
    if (cached) {
      attempts.value = JSON.parse(cached)
      sessionStorage.removeItem('prefetch_student_data')
      if (attempts.value.length > 0) {
        studentName.value = attempts.value[0]?.studentName || 'Học sinh'
      }
    } else {
      attempts.value = await listMyAttempts()
      if (attempts.value.length > 0) {
        studentName.value = attempts.value[0]?.studentName || 'Học sinh'
      }
    }

  } catch (error) {
    attempts.value = []
    toast.error('Không thể tải lịch sử bài thi.')
  } finally {
    isLoadingAttempts.value = false
  }
}

const loadUpcomingClassExams = async () => {
  try {
    const classes = await getMyClasses()
    const examGroups = await Promise.all(
      (classes || []).map(async (cls) => {
        try {
          const exams = await getStudentClassExams(cls.id)
          return (exams || []).map((exam) => ({
            ...exam,
            classId: cls.id,
            className: cls.name || cls.className || '',
            classSubject: cls.subject || ''
          }))
        } catch {
          return []
        }
      })
    )

    const now = Date.now()
    upcomingExams.value = examGroups
      .flat()
      .filter((exam) => {
        const endMs = new Date(exam.endTime || '').getTime()
        return Number.isNaN(endMs) || endMs >= now
      })
      .sort((a, b) => new Date(a.startTime || 0).getTime() - new Date(b.startTime || 0).getTime())
      .slice(0, 8)
  } catch {
    upcomingExams.value = []
  }
}

onMounted(async () => {
  await loadData()
  await loadUpcomingClassExams()
})
</script>
