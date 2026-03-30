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
          @action-click="handleHeroAction"
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
        <!-- Upcoming Exams -->
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

      <!-- Sidebar -->
      <template #sidebar>
        <!-- Quick Actions -->
        <StudentQuickActions @action="handleQuickAction" />

        <!-- Notifications -->
        <NotificationList
          :notifications="notifications"
          :loading="false"
          :display-limit="4"
          :dismissible="false"
          @item-click="handleNotificationClick"
        />
      </template>

    </StudentDashboardLayout>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { listMyAttempts } from '../../services/attemptService'
import { useToast } from '../../composables/useToast'

// Dashboard components
import StudentDashboardLayout from './dashboard/StudentDashboardLayout.vue'
import StudentHero from './dashboard/StudentHero.vue'
import StudentStatCard from './dashboard/StudentStatCard.vue'
import UpcomingExamCard from './dashboard/UpcomingExamCard.vue'
import RecentResultCard from './dashboard/RecentResultCard.vue'
import StudentQuickActions from './dashboard/StudentQuickActions.vue'
import NotificationList from './dashboard/NotificationList.vue'

const router = useRouter()
const toast = useToast()

// Data
const attempts = ref([])
const isLoadingAttempts = ref(false)
const studentName = ref('Học sinh')

// Upcoming exams (placeholder - would come from exam schedule API)
const upcomingExams = ref([])

// Computed: submitted attempts
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

// Average score
const avgScoreValue = computed(() => {
  if (!submittedAttempts.value.length) return null
  const total = submittedAttempts.value.reduce((sum, a) => sum + Number(a.score || 0), 0)
  return (total / submittedAttempts.value.length).toFixed(1)
})

const avgScoreDisplay = computed(() => {
  if (!avgScoreValue.value) return '—'
  return avgScoreValue.value
})

const avgScoreAccent = computed(() => {
  const v = Number(avgScoreValue.value)
  if (!v) return 'neutral'
  if (v >= 80) return 'success'
  if (v >= 50) return 'primary'
  return 'danger'
})

// Pass rate
const passRateValue = computed(() => {
  if (!submittedAttempts.value.length) return 0
  const passCount = submittedAttempts.value.filter(a => Number(a.score || 0) >= 50).length
  return (passCount / submittedAttempts.value.length) * 100
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

// Recent results for RecentResultCard
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

// Notifications
const notifications = computed(() => {
  const items = []

  // Upcoming exams as notifications
  for (const exam of upcomingExams.value.slice(0, 2)) {
    items.push({
      id: `exam-${exam.id}`,
      title: exam.title || 'Kỳ thi sắp tới',
      description: `Diem: ${formatDateTime(exam.startTime || exam.startDate)}`,
      type: 'exam',
      read: false,
      createdAt: exam.startTime || exam.startDate
    })
  }

  // Recent new scores
  const sevenDaysAgo = Date.now() - 7 * 24 * 60 * 60 * 1000
  for (const attempt of submittedAttempts.value.slice(0, 3)) {
    const submitted = new Date(attempt.submittedAt || 0).getTime()
    if (submitted >= sevenDaysAgo) {
      items.push({
        id: `score-${attempt.id}`,
        title: 'Kết quả thi mới',
        description: `${attempt.examTitle || 'Bài thi'}: ${(Number(attempt.score || 0) / 10).toFixed(1)} điểm`,
        type: 'score',
        read: false,
        createdAt: attempt.submittedAt
      })
    }
  }

  // Sort by date desc
  return items.sort((a, b) => {
    const aTime = new Date(a.createdAt || 0).getTime()
    const bTime = new Date(b.createdAt || 0).getTime()
    return bTime - aTime
  })
})

// Navigation
const goToExamJoin = () => router.push('/student/exam-join')
const goToPractice = () => router.push('/student/generate-practice-test')
const goToSchedule = () => router.push('/student/schedule')
const goToStudyHistory = () => router.push('/student/study-history')
const goToProfile = () => router.push('/student/profile')
const goToResult = (result) => {
  router.push({
    path: '/student/exam-result',
    query: {
      attemptId: result.attemptId,
      examTitle: result.title || result.examTitle
    }
  })
}

const goToUpcoming = (exam) => {
  router.push({ path: '/student/exam-join', query: { examId: exam.id } })
}

const handleHeroAction = (action) => {
  switch (action) {
    case 'exam': goToSchedule(); break
    case 'pending': goToExamJoin(); break
    case 'results': goToStudyHistory(); break
    case 'practice': goToPractice(); break
  }
}

const handleQuickAction = (actionId) => {
  switch (actionId) {
    case 'join-exam': goToExamJoin(); break
    case 'practice': goToPractice(); break
    case 'results': goToStudyHistory(); break
    case 'history': goToStudyHistory(); break
    case 'schedule': goToSchedule(); break
    case 'profile': goToProfile(); break
  }
}

const handleNotificationClick = (item) => {
  if (item.type === 'exam') {
    goToExamJoin()
  } else if (item.type === 'score') {
    const result = recentResults.value.find(r => r.id === Number(String(item.id).replace('score-', '')))
    if (result) goToResult(result)
  }
}

const formatDateTime = (value) => {
  if (!value) return '—'
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return '—'
  return d.toLocaleString('vi-VN', {
    day: '2-digit', month: '2-digit', hour: '2-digit', minute: '2-digit'
  })
}

// Load data
const loadData = async () => {
  isLoadingAttempts.value = true
  try {
    attempts.value = await listMyAttempts()
    // Try to get student name from first attempt or profile
    if (attempts.value.length > 0) {
      studentName.value = attempts.value[0]?.studentName || 'Học sinh'
    }
  } catch (error) {
    attempts.value = []
    toast.error('Không thể tải lịch sử bài thi.')
  } finally {
    isLoadingAttempts.value = false
  }
}

onMounted(loadData)
</script>
