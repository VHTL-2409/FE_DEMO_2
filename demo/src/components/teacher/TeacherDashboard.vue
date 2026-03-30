<template>
  <div class="td-page">
    <div class="td-page__inner mx-auto max-w-7xl px-4 pb-10 pt-6 sm:px-6 lg:px-8">

      <!-- 1. Hero Section -->
      <div class="mb-5">
        <DashboardHero
          :teacher-name="teacherName"
          :live-exam-count="statusCounts.started"
          :active-student-count="activeStudentCount"
          :alert-count="alertCount"
          @go-monitoring="goToMonitoring"
          @create-exam="goToCreate"
        />
      </div>

      <!-- 2. KPI Grid -->
      <div class="mb-5">
        <DashboardKpiGrid
          :stats="kpiStats"
          :alert-count="alertCount"
        />
      </div>

      <!-- 3. Active Monitoring (most important) -->
      <div v-if="liveExam" class="mb-5">
        <MonitoringCard
          :live-exam="liveExam"
          @go-monitoring="goToMonitoring"
        />
      </div>

      <!-- 4. Recent Submissions + Quick Actions -->
      <div class="mb-5 grid grid-cols-1 gap-5 lg:grid-cols-[1fr_300px]">
        <!-- Recent Submissions -->
        <div class="td-panel">
          <SubmissionList
            :exams="examTableData"
            @view-exam="openExamResult"
            @view-all="goToList"
            @create-exam="goToCreate"
          />
        </div>

        <!-- Quick Actions -->
        <div class="td-panel">
          <QuickActionGrid @action="handleQuickAction" />
        </div>
      </div>

      <!-- 5. Weekly Schedule + Insights -->
      <div class="grid grid-cols-1 gap-5 lg:grid-cols-[1fr_320px]">
        <div class="td-panel">
          <SchedulePanel :schedule-series="scheduleSeries" />
        </div>
        <div class="td-panel">
          <InsightCard
            :exams="examTableData"
            :stats="kpiStats"
          />
        </div>
      </div>

    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ApiError } from '../../services/apiClient'
import { listExams } from '../../services/examService'
import { useToast } from '../../composables/useToast'

// Dashboard sub-components
import DashboardHero from './dashboard/DashboardHero.vue'
import DashboardKpiGrid from './dashboard/DashboardKpiGrid.vue'
import MonitoringCard from './dashboard/MonitoringCard.vue'
import SubmissionList from './dashboard/SubmissionList.vue'
import QuickActionGrid from './dashboard/QuickActionGrid.vue'
import SchedulePanel from './dashboard/SchedulePanel.vue'
import InsightCard from './dashboard/InsightCard.vue'

const router = useRouter()
const toast = useToast()

const rawExams = ref([])

// ─── Exam helpers ────────────────────────────────────────────
const getExamStatusMeta = (exam) => {
  if (!exam.isActive) {
    return { key: 'draft', label: 'Bản nháp' }
  }
  const nowMs = Date.now()
  const startMs = new Date(exam.startTime || '').getTime()
  const endMs = new Date(exam.endTime || '').getTime()
  if (!Number.isNaN(startMs) && nowMs < startMs) {
    return { key: 'upcoming', label: 'Chưa bắt đầu' }
  }
  if (!Number.isNaN(startMs) && !Number.isNaN(endMs) && nowMs >= startMs && nowMs <= endMs) {
    return { key: 'started', label: 'Đang diễn ra' }
  }
  return { key: 'ended', label: 'Đã kết thúc' }
}

const getSortTime = (exam) => {
  for (const value of [exam.endTime, exam.startTime, exam.updatedAt, exam.createdAt]) {
    const t = new Date(value || '').getTime()
    if (!Number.isNaN(t)) return t
  }
  return 0
}

const formatDateTime = (value) => {
  const d = new Date(value || '')
  return Number.isNaN(d.getTime()) ? '—' : d.toLocaleString('vi-VN', {
    day: '2-digit', month: '2-digit', year: '2-digit', hour: '2-digit', minute: '2-digit'
  })
}

const exams = computed(() =>
  rawExams.value.slice().sort((a, b) => getSortTime(b) - getSortTime(a))
)

const statusCounts = computed(() =>
  exams.value.reduce((acc, exam) => {
    const { key } = getExamStatusMeta(exam)
    acc[key] = (acc[key] || 0) + 1
    return acc
  }, { draft: 0, upcoming: 0, started: 0, ended: 0 })
)

const activeStudentCount = computed(() => statusCounts.value.started * 3)
const alertCount = ref(0)

// Live exam data for the monitoring card
const liveExam = computed(() => {
  const started = exams.value.find(e => getExamStatusMeta(e).key === 'started')
  if (!started) return null
  return {
    ...started,
    isLive: true,
    studentCount: 0,
    answeredCount: 0,
    questionCount: started.questionCount || 0,
    alertCount: 0
  }
})

// ─── KPI Stats ────────────────────────────────────────────────
const kpiStats = computed(() => ({
  total: rawExams.value.length,
  active: statusCounts.value.started,
  ended: statusCounts.value.ended,
  draft: statusCounts.value.draft,
  upcoming: statusCounts.value.upcoming,
  activeTrend: null
}))

// ─── Table data ──────────────────────────────────────────────
const examTableData = computed(() =>
  exams.value.slice(0, 8).map(exam => {
    const { key, label } = getExamStatusMeta(exam)
    return {
      id: exam.id,
      title: exam.title,
      subtitle: exam.description || '—',
      date: formatDateTime(exam.endTime || exam.startTime),
      status: label,
      statusKey: key,
      participants: `${exam.questionCount ?? 0} câu`,
      disabled: key !== 'ended',
      resultPath: '/teacher/exams/review/summary'
    }
  })
)

// ─── Schedule series ──────────────────────────────────────────
const padDateKey = (d) => {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

const scheduleSeries = computed(() => {
  const days = []
  const now = new Date()
  for (let i = 6; i >= 0; i--) {
    const d = new Date(now)
    d.setHours(0, 0, 0, 0)
    d.setDate(d.getDate() - i)
    days.push(d)
  }
  const keys = days.map(padDateKey)
  const startCount = new Map(keys.map(k => [k, 0]))
  const endCount = new Map(keys.map(k => [k, 0]))
  for (const exam of rawExams.value) {
    const sk = padDateKey(new Date(exam.startTime || ''))
    const ek = padDateKey(new Date(exam.endTime || ''))
    if (startCount.has(sk)) startCount.set(sk, startCount.get(sk) + 1)
    if (endCount.has(ek)) endCount.set(ek, endCount.get(ek) + 1)
  }
  return {
    labels: days.map(d => d.toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit' })),
    starts: keys.map(k => startCount.get(k)),
    ends: keys.map(k => endCount.get(k))
  }
})

// ─── Teacher name ─────────────────────────────────────────────
const teacherName = ref('Giáo viên')

// ─── Navigation ───────────────────────────────────────────────
const goToMonitoring = () => router.push('/teacher/live-monitoring')
const goToList = () => router.push('/teacher/exams/list')
const goToCreate = () => router.push('/teacher/exams/create')
const goToProfile = () => router.push('/teacher/profile')

const openExamResult = (exam) => {
  if (exam.disabled) return
  router.push({ path: exam.resultPath, query: { title: exam.title, examId: exam.id } })
}

const handleQuickAction = (actionId) => {
  switch (actionId) {
    case 'create': goToCreate(); break
    case 'list': goToList(); break
    case 'monitoring': goToMonitoring(); break
    case 'analytics': router.push('/teacher/exams/review/summary'); break
    case 'profile': goToProfile(); break
    case 'help': router.push('/teacher/profile'); break
  }
}

// ─── Data loading ────────────────────────────────────────────
const loadExams = async () => {
  try {
    rawExams.value = await listExams()
  } catch (error) {
    toast.error(error instanceof ApiError ? error.message : 'Không thể tải dữ liệu dashboard.')
  }
}

onMounted(async () => {
  await loadExams()
})
</script>

<style scoped>
.td-page {
  min-height: 100vh;
}

.td-panel {
  background: var(--ds-surface);
  border: 1px solid var(--ds-border);
  border-radius: var(--ds-radius-2xl);
  padding: 1.375rem;
  box-shadow: var(--ds-shadow-xs);
}
</style>
