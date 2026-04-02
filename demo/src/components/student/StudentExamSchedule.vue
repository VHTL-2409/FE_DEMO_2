<template>
  <div class="bg-[var(--ds-bg)] min-h-full">
    <div class="mx-auto max-w-6xl px-4 pb-10 pt-4 sm:px-6 lg:px-8">

      <!-- Header -->
      <div class="mb-4">
        <ExamScheduleHeader
          :total-count="filteredExams.length"
          :live-count="statusCounts.live"
          :upcoming-count="statusCounts.upcoming + statusCounts.opening"
          :completed-count="statusCounts.completed"
        />
      </div>

      <!-- Filters -->
      <div class="mb-4">
        <ExamScheduleFilters
          :active-tab="activeTab"
          :tab-counts="tabCounts"
          :search-value="searchQuery"
          placeholder="Tìm kiếm theo tên kỳ thi..."
          @tab-change="onTabChange"
          @search="onSearch"
        />
      </div>

      <!-- List -->
      <UpcomingExamList
        :exams="filteredExams"
        :loading="isLoading"
        :section-label="sectionLabel"
        :empty-type="emptyType"
        :has-more="false"
        :remaining-count="0"
        :show-cta="true"
        @exam-click="onExamClick"
        @exam-enter="onExamEnter"
        @exam-details="onExamDetails"
        @cta-click="goToExamJoin"
      />

    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { listMyAttempts } from '../../services/attemptService'
import { useToast } from '../../composables/useToast'

// Schedule components
import ExamScheduleHeader from './schedule/ExamScheduleHeader.vue'
import ExamScheduleFilters from './schedule/ExamScheduleFilters.vue'
import UpcomingExamList from './schedule/UpcomingExamList.vue'

const router = useRouter()
const toast = useToast()

// Filter state
const activeTab = ref('all')
const searchQuery = ref('')

// Data
const allExams = ref([])
const isLoading = ref(false)

// ─── Status helpers ─────────────────────────────────────────────────
const getExamStatus = (exam) => {
  const nowMs = Date.now()
  const startMs = new Date(exam.startTime || exam.startDate || '').getTime()
  const endMs = new Date(exam.endTime || exam.endDate || '').getTime()

  if (Number.isNaN(startMs)) return 'upcoming'
  if (Number.isNaN(endMs)) {
    if (nowMs >= startMs - 10 * 60 * 1000 && nowMs < startMs) return 'opening'
    if (nowMs >= startMs && nowMs < startMs + 60 * 60 * 1000) return 'live'
    if (nowMs < startMs) return 'upcoming'
    return 'completed'
  }

  if (nowMs > endMs) return 'completed'
  if (nowMs >= startMs - 10 * 60 * 1000 && nowMs < startMs) return 'opening'
  if (nowMs >= startMs && nowMs <= endMs) return 'live'
  return 'upcoming'
}

// ─── Tab counts ─────────────────────────────────────────────────────
const tabCounts = computed(() => ({
  all: allExams.value.length,
  today: allExams.value.filter(e => {
    const d = new Date(e.startTime || e.startDate || '')
    if (Number.isNaN(d.getTime())) return false
    const today = new Date()
    return d.toDateString() === today.toDateString()
  }).length,
  upcoming: allExams.value.filter(e => {
    const status = getExamStatus(e)
    return status === 'upcoming' || status === 'opening'
  }).length,
  completed: allExams.value.filter(e => getExamStatus(e) === 'completed').length
}))

const statusCounts = computed(() => ({
  live: allExams.value.filter(e => getExamStatus(e) === 'live').length,
  upcoming: allExams.value.filter(e => getExamStatus(e) === 'upcoming').length,
  opening: allExams.value.filter(e => getExamStatus(e) === 'opening').length,
  completed: allExams.value.filter(e => getExamStatus(e) === 'completed').length,
  missed: 0
}))

// ─── Filtered exams ────────────────────────────────────────────────
const filteredExams = computed(() => {
  let list = [...allExams.value]

  // Tab filter
  if (activeTab.value === 'today') {
    const today = new Date()
    list = list.filter(e => {
      const d = new Date(e.startTime || e.startDate || '')
      return !Number.isNaN(d.getTime()) && d.toDateString() === today.toDateString()
    })
  } else if (activeTab.value === 'upcoming') {
    list = list.filter(e => {
      const status = getExamStatus(e)
      return status === 'upcoming' || status === 'opening'
    })
  } else if (activeTab.value === 'completed') {
    list = list.filter(e => getExamStatus(e) === 'completed')
  }

  // Search filter
  if (searchQuery.value.trim()) {
    const q = searchQuery.value.toLowerCase().trim()
    list = list.filter(e => {
      const title = (e.title || e.examTitle || '').toLowerCase()
      const subject = (e.subject || '').toLowerCase()
      return title.includes(q) || subject.includes(q)
    })
  }

  // Sort: live first, then opening, upcoming, completed
  const order = { live: 0, opening: 1, upcoming: 2, missed: 3, completed: 4 }
  list.sort((a, b) => {
    const sa = order[getExamStatus(a)] ?? 99
    const sb = order[getExamStatus(b)] ?? 99
    if (sa !== sb) return sa - sb
    const ta = new Date(a.startTime || a.startDate || 0).getTime()
    const tb = new Date(b.startTime || b.startDate || 0).getTime()
    return ta - tb
  })

  return list
})

// ─── Section label & empty type ────────────────────────────────────
const sectionLabel = computed(() => {
  const labels = {
    all: 'Tất cả kỳ thi',
    today: 'Kỳ thi hôm nay',
    upcoming: 'Kỳ thi sắp tới',
    completed: 'Kỳ thi đã hoàn thành'
  }
  return labels[activeTab.value] || 'Kết quả lọc'
})

const emptyType = computed(() => {
  if (searchQuery.value.trim()) return 'no-results'
  if (activeTab.value === 'completed') return 'all-done'
  return 'no-exams'
})

// ─── Actions ────────────────────────────────────────────────────────
const onTabChange = (tab) => { activeTab.value = tab }
const onSearch = (q) => { searchQuery.value = q }

const onExamClick = (exam) => { onExamDetails(exam) }

const onExamEnter = (exam) => {
  router.push({
    path: '/student/exam-waiting-room',
    query: {
      examId: exam.id,
      exam: exam.title || exam.examTitle || 'Kỳ thi',
      duration: exam.duration || exam.durationMinutes || 60,
      questions: exam.questionCount || 0,
      startAt: exam.startTime || exam.startDate || '',
      endAt: exam.endTime || exam.endDate || '',
      requireCameraMic: exam.requireCameraMic === false ? 'false' : 'true'
    }
  })
}

const onExamDetails = (exam) => {
  const status = getExamStatus(exam)
  if (status === 'completed') {
    router.push({
      path: '/student/exam-result',
      query: {
        attemptId: exam.attemptId || exam.id,
        examTitle: exam.title || exam.examTitle
      }
    })
  } else if (status === 'live') {
    onExamEnter(exam)
  } else {
    router.push({
      path: '/student/exam-waiting-room',
      query: {
        examId: exam.id,
        exam: exam.title || exam.examTitle || 'Kỳ thi',
        duration: exam.duration || exam.durationMinutes || 60,
        questions: exam.questionCount || 0,
        startAt: exam.startTime || exam.startDate || '',
        endAt: exam.endTime || exam.endDate || ''
      }
    })
  }
}

const goToExamJoin = () => router.push('/student/exam-join')

// ─── Data loading ───────────────────────────────────────────────────
const loadData = async () => {
  isLoading.value = true
  try {
    const attempts = await listMyAttempts()
    // Build exam list from attempts
    allExams.value = attempts.map(a => ({
      id: a.examId,
      examId: a.examId,
      attemptId: a.id,
      title: a.examTitle || 'Kỳ thi',
      examTitle: a.examTitle || 'Kỳ thi',
      startTime: a.startTime,
      endTime: a.endTime,
      startDate: a.startedAt,
      endDate: a.submittedAt,
      duration: a.durationMinutes,
      questionCount: a.questionCount,
      status: a.status,
      score: a.score,
      subject: a.subject,
      className: a.className,
      room: a.room
    }))
  } catch (error) {
    allExams.value = []
    toast.error('Không thể tải danh sách kỳ thi.')
  } finally {
    isLoading.value = false
  }
}

onMounted(loadData)
</script>
