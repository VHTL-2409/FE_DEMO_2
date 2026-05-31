<template>
  <div class="bg-[var(--ds-bg)] min-h-full ds-animate-fade-up">
    <div v-if="loadError" class="mx-auto max-w-7xl px-4 pb-4 pt-4 sm:px-6 lg:px-8">
      <EmptyState
        icon="warning"
        title="Không tải được tổng quan kết quả"
        :description="loadError"
        action-label="Quay lại danh sách đề"
        fill
        @action="router.push('/teacher/exams/list')"
      />
    </div>

    <AnalyticsLayout v-else>
      <!-- Header -->
      <template #header>
        <AnalyticsHeader
          title="Phân tích kết quả"
          :subtitle="selectedExamTitle"
          :show-exam-selector="false"
          :show-date-range="false"
          :show-pdf-export="true"
          :export-loading="isLoading"
          @back="router.push('/teacher/exams/list')"
          @export-csv="downloadReport"
          @export-pdf="downloadReport"
        />
      </template>

      <!-- KPI strip: 5 cards -->
      <template #kpis>
        <ResultStatCard
          label="Học sinh"
          :value="submittedAttempts.length"
          icon="group"
          accent="primary"
          :sub="`${submittedAttempts.length} lượt làm`"
          :loading="isLoading"
        />
        <ResultStatCard
          label="Điểm TB"
          :value="averageScoreValue > 0 ? (averageScoreValue / 10).toFixed(1) : '—'"
          icon="insights"
          accent="primary"
          :sub="submittedAttempts.length ? `${submittedAttempts.length} lượt làm` : 'Chưa có dữ liệu'"
          :loading="isLoading"
        />
        <ResultStatCard
          label="Tỷ lệ đạt"
          :value="passRateValue > 0 ? passRateValue.toFixed(0) + '%' : '—'"
          icon="check_circle"
          :accent="passRateValue >= 70 ? 'success' : passRateValue >= 50 ? 'warning' : 'danger'"
          :sub="`Đạt >= 5.0/10`"
          :loading="isLoading"
        />
        <ResultStatCard
          label="Tỷ lệ hoàn thành"
          :value="completionRate > 0 ? completionRate.toFixed(0) + '%' : '—'"
          icon="verified_user"
          accent="info"
          :sub="`Tổng ${attempts.length} HS`"
          :loading="isLoading"
        />
        <ResultStatCard
          label="Cảnh báo"
          :value="totalWarnings"
          icon="warning"
          :accent="totalWarnings > 0 ? 'warning' : 'success'"
          :sub="`Trên ${attempts.length} lượt`"
          :loading="isLoading"
        />
      </template>

      <!-- Charts row: ScoreDistribution (2/3) + DifficultyInsight (1/3) -->
      <template #chart>
        <ScoreDistributionChart
          :distribution="scoreDistribution"
          :average="averageScoreValue > 0 ? averageScoreValue / 10 : null"
          :median="medianScore"
          :std-dev="stdDevScore"
          :exam-title="selectedExamTitle"
          :loading="isLoading"
          :passing-score="5"
        />
      </template>

      <template #insights>
        <DifficultyInsightCard
          :questions="questionWrongStats"
          :loading="isLoading"
          :display-limit="5"
          @question-click="viewQuestionDetail"
          @view-all="router.push({ path: '/teacher/exams/review/incidents', query: route.query })"
        />
      </template>

      <!-- Filters -->
      <template #filters>
        <AnalyticsFilters
          v-model:active-tab="activeTab"
          v-model:search-query="searchQuery"
          v-model:score-filter="scoreFilter"
          v-model:sort-by="sortBy"
          :show-export="true"
          :stats="filterStats"
          :export-loading="isLoading"
          @export="downloadReport"
        />
      </template>

      <!-- Student results table -->
      <template #table>
        <StudentResultsTable
          :students="filteredResultStudents"
          :loading="isLoading"
          :search-query="searchQuery"
          @row-click="viewStudentReport"
        />
      </template>
    </AnalyticsLayout>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { ApiError } from '../../services/apiClient'
import { listExamAttempts } from '../../services/attemptService'
import { getQuestionWrongStats } from '../../services/examService'
import { exportToCsv } from '../../utils/reportExport'
import { useRoute, useRouter } from 'vue-router'

// Analytics components
import AnalyticsLayout from './analytics/AnalyticsLayout.vue'
import AnalyticsHeader from './analytics/AnalyticsHeader.vue'
import ResultStatCard from './analytics/ResultStatCard.vue'
import ScoreDistributionChart from './analytics/ScoreDistributionChart.vue'
import DifficultyInsightCard from './analytics/DifficultyInsightCard.vue'
import AnalyticsFilters from './analytics/AnalyticsFilters.vue'
import StudentResultsTable from './analytics/StudentResultsTable.vue'
import EmptyState from '../ui/EmptyState.vue'

const route = useRoute()
const router = useRouter()

// ── State ──────────────────────────────────────────────────────────────────
const isLoading = ref(false)
const loadError = ref('')
const attempts = ref([])
const questionWrongStats = ref([])

// Filter state
const activeTab = ref('all')
const searchQuery = ref('')
const scoreFilter = ref('all')
const sortBy = ref('score')

// ── Route ──────────────────────────────────────────────────────────────────
const examId = computed(() => Number.parseInt(String(route.query.examId || ''), 10) || null)
const selectedExamTitle = computed(() => route.query.title || 'Đề thi đã chọn')

// ── Computed: submitted attempts ────────────────────────────────────────────
const submittedAttempts = computed(() => attempts.value.filter((attempt) => [
  'SUBMITTED',
  'AUTO_SUBMITTED'
].includes(String(attempt.status || '').toUpperCase())))

const completionRate = computed(() => {
  if (!attempts.value.length) return 0
  return (submittedAttempts.value.length / attempts.value.length) * 100
})

const totalWarnings = computed(() => attempts.value.reduce((sum, a) => sum + (a.warningCount || 0), 0))

// ── Computed: score stats ────────────────────────────────────────────────────
const averageScoreValue = computed(() => {
  if (!submittedAttempts.value.length) return 0
  const total = submittedAttempts.value.reduce((sum, attempt) => sum + Number(attempt.score || 0), 0)
  return total / submittedAttempts.value.length
})

const sortedScores = computed(() => {
  return [...submittedAttempts.value]
    .map(a => Number(a.score || 0))
    .sort((a, b) => a - b)
})

const medianScore = computed(() => {
  const scores = sortedScores.value
  if (!scores.length) return null
  const mid = Math.floor(scores.length / 2)
  return scores.length % 2 !== 0
    ? scores[mid] / 10
    : ((scores[mid - 1] + scores[mid]) / 2) / 10
})

const stdDevScore = computed(() => {
  const scores = submittedAttempts.value.map(a => Number(a.score || 0))
  if (scores.length < 2) return null
  const mean = scores.reduce((s, v) => s + v, 0) / scores.length
  const variance = scores.reduce((s, v) => s + Math.pow(v - mean, 2), 0) / scores.length
  return Math.sqrt(variance) / 10
})

// ── Computed: score distribution for chart ──────────────────────────────────
const scoreDistribution = computed(() => {
  if (!submittedAttempts.value.length) return []
  // Build histogram: scores 0-10
  const buckets = Array.from({ length: 11 }, (_, i) => ({
    score: i,
    count: 0
  }))
  for (const attempt of submittedAttempts.value) {
    const raw = Number(attempt.score || 0)
    // Score is in 0-100 range, divide by 10 to get 0-10
    const bucket = Math.min(10, Math.max(0, Math.round(raw / 10)))
    buckets[bucket].count++
  }
  return buckets
})

// ── Computed: filter stats for tabs ────────────────────────────────────────
const filterStats = computed(() => ({
  total: attempts.value.length,
  passed: submittedAttempts.value.filter(a => Number(a.score || 0) >= 50).length,
  failed: submittedAttempts.value.filter(a => Number(a.score || 0) < 50).length
}))

// ── Computed: student rows ─────────────────────────────────────────────────────
const resultStudents = computed(() => submittedAttempts.value.map((attempt) => {
  const score = Number(attempt.score || 0)
  return {
    id: attempt.id,
    attemptId: attempt.id,
    userName: attempt.student || 'Học sinh không rõ',
    fullName: attempt.student || '',
    email: attempt.email || '',
    studentCode: attempt.studentCode || (attempt.studentId ? `SV-${attempt.studentId}` : `AT-${attempt.id}`),
    score,
    scoreDisplay: (score / 10).toFixed(1),
    warningCount: attempt.warningCount ?? attempt.riskScore ?? 0,
    status: String(attempt.status || '').toUpperCase(),
    startedAt: attempt.startedAt,
    submittedAt: attempt.submittedAt,
    examId: attempt.examId
  }
}))

const filteredResultStudents = computed(() => {
  let list = resultStudents.value

  // Tab filter
  if (activeTab.value === 'passed') {
    list = list.filter(s => s.score >= 50)
  } else if (activeTab.value === 'failed') {
    list = list.filter(s => s.score < 50)
  }

  // Score filter
  if (scoreFilter.value === 'high') {
    list = list.filter(s => s.score >= 80)
  } else if (scoreFilter.value === 'medium') {
    list = list.filter(s => s.score >= 50 && s.score < 80)
  } else if (scoreFilter.value === 'low') {
    list = list.filter(s => s.score < 50)
  }

  // Search
  if (searchQuery.value.trim()) {
    const q = searchQuery.value.toLowerCase()
    list = list.filter(s =>
      (s.userName || '').toLowerCase().includes(q) ||
      (s.email || '').toLowerCase().includes(q)
    )
  }

  // Sort
  if (sortBy.value === 'score') {
    list = [...list].sort((a, b) => b.score - a.score)
  } else if (sortBy.value === 'score_desc') {
    list = [...list].sort((a, b) => b.score - a.score)
  } else if (sortBy.value === 'name') {
    list = [...list].sort((a, b) => (a.userName || '').localeCompare(b.userName || ''))
  } else if (sortBy.value === 'warnings') {
    list = [...list].sort((a, b) => b.warningCount - a.warningCount)
  }

  return list
})

const passRateValue = computed(() => {
  if (!submittedAttempts.value.length) return 0
  const passCount = submittedAttempts.value.filter((attempt) => Number(attempt.score || 0) >= 50).length
  return (passCount / submittedAttempts.value.length) * 100
})

// ── Actions ────────────────────────────────────────────────────────────────
const viewStudentReport = (student) => {
  router.push({
    path: '/teacher/exams/review/student-report',
    query: {
      examId: examId.value,
      attemptId: student.attemptId,
      title: selectedExamTitle.value
    }
  })
}

const viewQuestionDetail = (question) => {
  router.push({
    path: '/teacher/exams/review/incidents',
    query: { examId: examId.value, title: selectedExamTitle.value }
  })
}

const downloadReport = () => {
  const columns = [
    { key: 'userName', label: 'Học sinh' },
    { key: 'studentCode', label: 'Mã học sinh' },
    { key: 'scoreDisplay', label: 'Điểm' },
    { key: 'warningCount', label: 'Cảnh báo' },
    { key: 'status', label: 'Trạng thái' }
  ]
  const safeTitle = (selectedExamTitle.value || 'bao-cao').replace(/[^a-zA-Z0-9]/g, '-')
  exportToCsv(filteredResultStudents.value, columns, `bao-cao-diem-${safeTitle}.csv`)
}

// ── Load data ──────────────────────────────────────────────────────────────
const loadAttempts = async () => {
  if (!examId.value) {
    loadError.value = 'Thiếu mã đề thi. Vui lòng mở lại báo cáo từ danh sách đề.'
    return
  }
  isLoading.value = true
  loadError.value = ''
  try {
    const [attemptsData, wrongStats] = await Promise.all([
      listExamAttempts(examId.value),
      getQuestionWrongStats(examId.value)
    ])
    attempts.value = attemptsData.map(a => ({
      ...a,
      warningCount: a.warningCount ?? a.riskScore ?? 0,
      wrongRate: (a.wrongRatePercent ?? a.wrongRate ?? a.errorRate ?? 0) / 100
    }))
    questionWrongStats.value = wrongStats.map(q => ({
      ...q,
      wrongRate: (q.wrongRatePercent ?? q.wrongRate ?? q.errorRate ?? 0) / 100
    }))
  } catch (error) {
    loadError.value = error instanceof ApiError ? error.message : 'Không thể tải dữ liệu tổng quan kết quả.'
  } finally {
    isLoading.value = false
  }
}

onMounted(loadAttempts)

watch(
  () => route.query.examId,
  () => {
    loadAttempts()
  }
)
</script>
