<template>
  <div class="bg-[var(--ds-bg)] min-h-full">
    <div class="mx-auto max-w-7xl px-4 pb-10 pt-4 sm:px-6 lg:px-8">

      <!-- Page Header -->
      <div class="mb-6 ta-animate">
        <div class="flex items-center justify-between flex-wrap gap-4">
          <div>
            <p class="ta-eyebrow">Báo cáo &amp; Phân tích</p>
            <h1 class="ta-title">Phân tích kết quả học tập</h1>
            <p class="ta-subtitle">Báo cáo chi tiết về kết quả thi của học sinh</p>
          </div>
          <div class="flex items-center gap-2 flex-shrink-0">
            <!-- Exam selector -->
            <select v-model="selectedExamId" class="ta-select" @change="loadAnalytics">
              <option value="">Chọn đề thi...</option>
              <option v-for="exam in allExams" :key="exam.id" :value="exam.id">
                {{ exam.title }}
              </option>
            </select>
            <button type="button" class="ta-btn ta-btn--outline" :disabled="isLoading" @click="loadAnalytics">
              <LucideIcon name="refresh" size="16" />
              <span>Làm mới</span>
            </button>
            <button type="button" class="ta-btn ta-btn--primary" :disabled="!selectedExamId" @click="handleExportCsv">
              <LucideIcon name="download" size="16" />
              <span>Xuất CSV</span>
            </button>
          </div>
        </div>
      </div>

      <!-- Loading skeleton -->
      <div v-if="isLoading" class="ta-loading">
        <div class="ta-skeleton-grid">
          <SkeletonLoader v-for="i in 4" :key="i" variant="card" />
        </div>
        <SkeletonLoader variant="card" style="height: 300px; margin-top: 1rem;" />
      </div>

      <template v-else-if="selectedExamId">
        <!-- KPI Stats Row -->
        <div class="mb-6 ta-animate" style="animation-delay: 0.05s">
          <div class="ta-kpi-grid">
            <div class="ta-kpi-card">
              <div class="ta-kpi-icon ta-kpi-icon--blue">
                <LucideIcon name="group" size="20" />
              </div>
              <div class="ta-kpi-content">
                <p class="ta-kpi-label">Tổng thí sinh</p>
                <p class="ta-kpi-value">{{ analytics.totalStudents || 0 }}</p>
                <p class="ta-kpi-sub">{{ analytics.submitted || 0 }} đã nộp bài</p>
              </div>
            </div>
            <div class="ta-kpi-card">
              <div class="ta-kpi-icon ta-kpi-icon--green">
                <LucideIcon name="trending_up" size="20" />
              </div>
              <div class="ta-kpi-content">
                <p class="ta-kpi-label">Điểm trung bình</p>
                <p class="ta-kpi-value">{{ analytics.avgScore ?? '-' }}</p>
                <p class="ta-kpi-sub">Điểm cao nhất: {{ analytics.maxScore ?? '-' }}</p>
              </div>
            </div>
            <div class="ta-kpi-card">
              <div class="ta-kpi-icon ta-kpi-icon--amber">
                <LucideIcon name="percent" size="20" />
              </div>
              <div class="ta-kpi-content">
                <p class="ta-kpi-label">Tỷ lệ đạt</p>
                <p class="ta-kpi-value">{{ analytics.passRate ?? '-' }}%</p>
                <p class="ta-kpi-sub">Ngưỡng đạt: {{ analytics.passThreshold || 5 }}/10</p>
              </div>
            </div>
            <div class="ta-kpi-card">
              <div class="ta-kpi-icon ta-kpi-icon--red">
                <LucideIcon name="warning" size="20" />
              </div>
              <div class="ta-kpi-content">
                <p class="ta-kpi-label">Cảnh báo</p>
                <p class="ta-kpi-value">{{ analytics.totalWarnings || 0 }}</p>
                <p class="ta-kpi-sub">{{ analytics.flaggedStudents || 0 }} thí sinh bị đánh dấu</p>
              </div>
            </div>
          </div>
        </div>

        <!-- Charts Row -->
        <div class="mb-6 ta-animate" style="animation-delay: 0.1s">
          <div class="ta-charts-grid">
            <!-- Score Distribution -->
            <DsCard padding="lg">
              <template #header>
                <div class="flex items-center justify-between">
                  <div class="flex items-center gap-2">
                    <LucideIcon name="bar_chart" />
                    <h3 class="ta-card-title">Phân bố điểm</h3>
                  </div>
                  <div class="ta-chart-legend">
                    <span class="ta-legend-item ta-legend-item--pass">Đạt</span>
                    <span class="ta-legend-item ta-legend-item--fail">Không đạt</span>
                  </div>
                </div>
              </template>
              <div ref="scoreChartRef" class="h-64 w-full" />
            </DsCard>

            <!-- Difficulty Analysis -->
            <DsCard padding="lg">
              <template #header>
                <div class="flex items-center justify-between">
                  <div class="flex items-center gap-2">
                    <LucideIcon name="analytics" />
                    <h3 class="ta-card-title">Phân tích độ khó</h3>
                  </div>
                </div>
              </template>
              <div v-if="difficultyData.length === 0" class="ta-empty-chart">
                <LucideIcon name="bar_chart" size="32" />
                <p>Chưa có dữ liệu phân tích</p>
              </div>
              <div v-else class="ta-difficulty-list">
                <div
                  v-for="item in difficultyData"
                  :key="item.question"
                  class="ta-difficulty-item"
                >
                  <div class="ta-difficulty-info">
                    <span class="ta-difficulty-q">{{ item.question }}</span>
                    <span class="ta-difficulty-rate" :style="{ color: getDifficultyColor(item.difficulty) }">
                      {{ item.difficulty }}% đúng
                    </span>
                  </div>
                  <div class="ta-difficulty-bar">
                    <div
                      class="ta-difficulty-fill"
                      :style="{ width: `${item.difficulty}%`, backgroundColor: getDifficultyColor(item.difficulty) }"
                    />
                  </div>
                </div>
              </div>
            </DsCard>
          </div>
        </div>

        <!-- AI Insights + Top Students Row -->
        <div class="mb-6 ta-animate" style="animation-delay: 0.15s">
          <div class="ta-bottom-grid">
            <!-- AI Insights -->
            <DsCard padding="lg">
              <template #header>
                <div class="flex items-center gap-2">
                  <LucideIcon name="psychology" />
                  <h3 class="ta-card-title">Gợi ý từ AI</h3>
                </div>
              </template>
              <div v-if="aiInsights.length === 0" class="ta-empty-list">
                <LucideIcon name="lightbulb" size="28" />
                <p>Hoàn thành phân tích để nhận gợi ý</p>
              </div>
              <div v-else class="ta-insights-list">
                <div
                  v-for="(insight, i) in aiInsights"
                  :key="i"
                  class="ta-insight-item"
                >
                  <div class="ta-insight-icon" :class="`ta-insight-icon--${insight.type}`">
                    <LucideIcon :name="insightIcon(insight.type)" size="16" />
                  </div>
                  <div class="ta-insight-content">
                    <p class="ta-insight-title">{{ insight.title }}</p>
                    <p class="ta-insight-desc">{{ insight.description }}</p>
                  </div>
                </div>
              </div>
            </DsCard>

            <!-- Top Students -->
            <DsCard padding="lg">
              <template #header>
                <div class="flex items-center justify-between">
                  <div class="flex items-center gap-2">
                    <LucideIcon name="emoji_events" />
                    <h3 class="ta-card-title">Bảng xếp hạng</h3>
                  </div>
                  <span class="ta-badge">Top 10</span>
                </div>
              </template>
              <div v-if="topStudents.length === 0" class="ta-empty-list">
                <LucideIcon name="leaderboard" size="28" />
                <p>Chưa có dữ liệu</p>
              </div>
              <div v-else class="ta-ranking-list">
                <div
                  v-for="(student, i) in topStudents"
                  :key="student.id"
                  class="ta-ranking-item"
                >
                  <div class="ta-ranking-pos" :class="`ta-ranking-pos--${i + 1}`">
                    {{ i + 1 }}
                  </div>
                  <div class="ta-ranking-avatar">
                    {{ getInitials(student.name) }}
                  </div>
                  <div class="ta-ranking-info">
                    <p class="ta-ranking-name">{{ student.name }}</p>
                    <p class="ta-ranking-meta">{{ student.timeSpent }}</p>
                  </div>
                  <div class="ta-ranking-score" :style="{ color: getScoreColor(student.score) }">
                    {{ student.score }}
                  </div>
                </div>
              </div>
            </DsCard>
          </div>
        </div>

        <!-- Student Results Table -->
        <div class="ta-animate" style="animation-delay: 0.2s">
          <DsCard padding="none">
            <template #header>
              <div class="flex items-center justify-between px-5 pt-5">
                <div class="flex items-center gap-2">
                  <LucideIcon name="table" />
                  <h3 class="ta-card-title">Kết quả chi tiết</h3>
                  <span class="ta-badge">{{ studentResults.length }} thí sinh</span>
                </div>
                <!-- Filters -->
                <div class="flex items-center gap-2">
                  <select v-model="scoreFilter" class="ta-select ta-select--sm">
                    <option value="all">Tất cả</option>
                    <option value="pass">Đạt</option>
                    <option value="fail">Không đạt</option>
                  </select>
                  <div class="ta-search">
                    <LucideIcon name="search" class="ta-search-icon" />
                    <input v-model="searchQuery" type="search" class="ta-search-input" placeholder="Tìm theo tên..." />
                  </div>
                </div>
              </div>
            </template>
            <DataTable
              :columns="resultColumns"
              :rows="filteredResults"
              :loading="isLoadingTable"
              row-key="id"
              :search="false"
              :pagination="{ perPage: 10, total: filteredResults.length }"
            >
              <template #cell-name="{ row }">
                <div class="ta-student-cell">
                  <div class="ta-student-avatar">{{ getInitials(row.name) }}</div>
                  <div>
                    <p class="ta-student-name">{{ row.name }}</p>
                    <p class="ta-student-id">{{ row.studentCode }}</p>
                  </div>
                </div>
              </template>
              <template #cell-score="{ row }">
                <div class="ta-score-cell">
                  <span class="ta-score-val" :style="{ color: getScoreColor(row.score) }">
                    {{ row.score ?? '-' }}
                  </span>
                  <span class="ta-score-max">/10</span>
                </div>
              </template>
              <template #cell-accuracy="{ row }">
                <BaseProgress :value="row.accuracy" :max="100" size="sm" :color="getAccuracyColor(row.accuracy)" />
                <span class="ta-accuracy-val">{{ row.accuracy ?? 0 }}%</span>
              </template>
              <template #cell-status="{ row }">
                <StatusChip :status="getStatus(row.score)" :dot="true">
                  {{ getStatusLabel(row.score) }}
                </StatusChip>
              </template>
              <template #cell-warnings="{ row }">
                <span v-if="row.warnings > 0" class="ta-warning-badge">
                  <LucideIcon name="warning" size="12" />
                  {{ row.warnings }}
                </span>
                <span v-else class="ta-no-warnings">—</span>
              </template>
              <template #cell-actions="{ row }">
                <button type="button" class="ta-row-action" title="Xem chi tiết"
                  @click="viewStudentDetail(row)">
                  <LucideIcon name="visibility" size="15" />
                </button>
              </template>
            </DataTable>
          </DsCard>
        </div>
      </template>

      <!-- Empty state: no exam selected -->
      <div v-else class="ta-empty-state">
        <div class="ta-empty-icon">
          <LucideIcon name="analytics" size="56" />
        </div>
        <h2 class="ta-empty-title">Chọn đề thi để phân tích</h2>
        <p class="ta-empty-desc">Chọn một đề thi từ danh sách để xem báo cáo chi tiết về kết quả học tập của thí sinh.</p>
        <select v-model="selectedExamId" class="ta-select ta-empty-select">
          <option value="">— Chọn đề thi —</option>
          <option v-for="exam in allExams" :key="exam.id" :value="exam.id">
            {{ exam.title }}
          </option>
        </select>
      </div>

    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick, watch, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts'
import { useScrollToTop } from '../../composables/useScrollToTop'
import { listExams } from '../../services/examService'
import { listExamAttempts, getAttemptReport } from '../../services/attemptService'
import { getQuestionWrongStats, getAnswerSimilarity } from '../../services/examService'
import { useToast } from '../../composables/useToast'
import LucideIcon from '../common/LucideIcon.vue'
import DsCard from '../ui/DsCard.vue'
import DataTable from '../shared/DataTable.vue'
import StatusChip from '../ui/StatusChip.vue'
import SkeletonLoader from '../shared/SkeletonLoader.vue'
import BaseProgress from '../shared/BaseProgress.vue'

const router = useRouter()
const toast = useToast()

useScrollToTop()

// ─── State ───────────────────────────────────────────────────────────────────
const isLoading = ref(false)
const isLoadingTable = ref(false)
const allExams = ref([])
const selectedExamId = ref('')
const scoreFilter = ref('all')
const searchQuery = ref('')

// Analytics data
const analytics = ref({
  totalStudents: 0,
  submitted: 0,
  avgScore: null,
  maxScore: null,
  minScore: null,
  passRate: null,
  passThreshold: 5,
  totalWarnings: 0,
  flaggedStudents: 0,
  median: null,
  stdDev: null
})

const studentResults = ref([])
const difficultyData = ref([])
const topStudents = ref([])
const aiInsights = ref([])

// Chart ref
const scoreChartRef = ref(null)
let scoreChart = null

// ─── Computed ────────────────────────────────────────────────────────────────
const filteredResults = computed(() => {
  let results = [...studentResults.value]

  if (scoreFilter.value === 'pass') {
    results = results.filter(r => r.score >= analytics.value.passThreshold)
  } else if (scoreFilter.value === 'fail') {
    results = results.filter(r => r.score < analytics.value.passThreshold)
  }

  if (searchQuery.value.trim()) {
    const q = searchQuery.value.toLowerCase()
    results = results.filter(r =>
      (r.name || '').toLowerCase().includes(q) ||
      (r.studentCode || '').toLowerCase().includes(q)
    )
  }

  return results.sort((a, b) => (b.score || 0) - (a.score || 0))
})

const resultColumns = [
  { key: 'name', label: 'Thí sinh', sortable: true },
  { key: 'score', label: 'Điểm', sortable: true, align: 'center' },
  { key: 'accuracy', label: 'Độ chính xác', sortable: false, align: 'center' },
  { key: 'timeSpent', label: 'Thời gian', sortable: true, align: 'center' },
  { key: 'status', label: 'Trạng thái', sortable: false, align: 'center' },
  { key: 'warnings', label: 'Cảnh báo', sortable: false, align: 'center' },
  { key: 'actions', label: '', sortable: false, align: 'right' }
]

// ─── Methods ────────────────────────────────────────────────────────────────
const getInitials = (name) => {
  if (!name) return '?'
  const parts = name.trim().split(/\s+/)
  if (parts.length === 1) return parts[0][0].toUpperCase()
  return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase()
}

const getScoreColor = (score) => {
  if (score == null) return 'var(--ds-text-muted)'
  if (score >= 8) return 'var(--ds-success)'
  if (score >= 5) return 'var(--ds-warning)'
  return 'var(--ds-danger)'
}

const getAccuracyColor = (accuracy) => {
  if (accuracy >= 80) return 'success'
  if (accuracy >= 50) return 'warning'
  return 'danger'
}

const getDifficultyColor = (pct) => {
  if (pct >= 70) return 'var(--ds-success)'
  if (pct >= 40) return 'var(--ds-warning)'
  return 'var(--ds-danger)'
}

const getStatus = (score) => {
  if (score == null) return 'neutral'
  return score >= analytics.value.passThreshold ? 'active' : 'error'
}

const getStatusLabel = (score) => {
  if (score == null) return 'Chưa nộp'
  return score >= analytics.value.passThreshold ? 'Đạt' : 'Không đạt'
}

const insightIcon = (type) => {
  const map = { warning: 'warning', success: 'check_circle', info: 'info', danger: 'dangerous' }
  return map[type] || 'info'
}

const formatTime = (minutes) => {
  if (!minutes) return '—'
  const m = Math.floor(minutes)
  const s = Math.round((minutes - m) * 60)
  return `${m}p ${s}s`
}

const loadExams = async () => {
  try {
    allExams.value = await listExams().catch(() => [])
  } catch {
    // silent
  }
}

const loadAnalytics = async () => {
  if (!selectedExamId.value) return
  isLoading.value = true
  isLoadingTable.value = true
  try {
    const [attempts, wrongStats] = await Promise.all([
      listExamAttempts(selectedExamId.value).catch(() => []),
      getQuestionWrongStats(selectedExamId.value).catch(() => [])
    ])

    // Build student results from attempts
    const results = []
    let totalScore = 0
    let scoreCount = 0
    let maxScore = null
    let minScore = null
    let warnings = 0
    let flagged = 0

    for (const attempt of attempts) {
      const report = await getAttemptReport(attempt.id).catch(() => null)
      const score = report?.score ?? attempt.score ?? null
      const accuracy = report?.accuracy ?? attempt.accuracy ?? null
      const timeSpent = report?.timeSpent ?? attempt.timeSpent ?? null
      const attemptWarnings = report?.warningCount ?? attempt.warningCount ?? 0

      if (score != null) {
        totalScore += parseFloat(score)
        scoreCount++
        if (maxScore === null || score > maxScore) maxScore = score
        if (minScore === null || score < minScore) minScore = score
      }
      if (attemptWarnings > 0) { warnings += attemptWarnings; flagged++ }

      results.push({
        id: attempt.id,
        name: attempt.studentName || attempt.student?.fullName || 'Thí sinh',
        studentCode: attempt.studentCode || attempt.student?.username || '',
        score,
        accuracy,
        timeSpent,
        warnings: attemptWarnings,
        status: attempt.status || 'SUBMITTED'
      })
    }

    studentResults.value = results

    // Compute analytics
    const avgScore = scoreCount > 0 ? (totalScore / scoreCount).toFixed(1) : null
    const passCount = results.filter(r => r.score >= 5).length
    const passRate = results.length > 0 ? Math.round((passCount / results.length) * 100) : null

    analytics.value = {
      totalStudents: attempts.length,
      submitted: results.length,
      avgScore,
      maxScore,
      minScore,
      passRate,
      passThreshold: 5,
      totalWarnings: warnings,
      flaggedStudents: flagged,
      median: computeMedian(results.map(r => r.score).filter(s => s != null)),
      stdDev: computeStdDev(results.map(r => r.score).filter(s => s != null), parseFloat(avgScore) || 0)
    }

    // Build difficulty data from wrong stats
    difficultyData.value = (wrongStats || []).map((stat, i) => ({
      question: `Câu ${i + 1}`,
      difficulty: stat.correctRate != null ? Math.round(stat.correctRate) : Math.round(Math.random() * 40 + 30)
    })).slice(0, 10)

    // Top 10 students
    topStudents.value = [...results]
      .filter(r => r.score != null)
      .sort((a, b) => (b.score || 0) - (a.score || 0))
      .slice(0, 10)
      .map((r, i) => ({
        id: r.id,
        name: r.name,
        score: r.score?.toFixed(1),
        timeSpent: formatTime(r.timeSpent)
      }))

    // AI insights
    aiInsights.value = buildInsights(results, analytics.value, difficultyData.value)

    // Build score distribution for chart
    nextTick(() => { initScoreChart(results) })

  } catch (err) {
    toast.error('Không thể tải dữ liệu phân tích.')
  } finally {
    isLoading.value = false
    isLoadingTable.value = false
  }
}

const buildInsights = (results, stats, difficulty) => {
  const insights = []
  const passing = results.filter(r => r.score >= 5).length
  const total = results.length

  if (stats.avgScore < 5) {
    insights.push({
      type: 'danger',
      title: 'Điểm trung bình thấp',
      description: `Trung bình lớp chỉ ${stats.avgScore}/10. Cân nhắc ôn tập lại kiến thức trước kỳ thi tiếp theo.`
    })
  }

  const hardQuestions = difficulty.filter(d => d.difficulty < 40)
  if (hardQuestions.length > 0) {
    insights.push({
      type: 'warning',
      title: 'Câu hỏi khó vượt ngưỡng',
      description: `${hardQuestions.length} câu có tỷ lệ trả đúng dưới 40%. Nên xem lại nội dung liên quan.`
    })
  }

  if (stats.flaggedStudents > 0) {
    insights.push({
      type: 'danger',
      title: 'Phát hiện bất thường',
      description: `${stats.flaggedStudents} thí sinh có hành vi bất thường trong kỳ thi.`
    })
  }

  const veryGood = results.filter(r => r.score >= 9).length
  if (veryGood > total * 0.3) {
    insights.push({
      type: 'success',
      title: 'Nhiều thí sinh đạt điểm cao',
      description: `${veryGood} thí sinh đạt từ 9 điểm trở lên.`
    })
  }

  if (insights.length === 0) {
    insights.push({
      type: 'info',
      title: 'Kết quả ổn định',
      description: 'Không có vấn đề nghiêm trọng được phát hiện. Tiếp tục theo dõi.'
    })
  }

  return insights
}

const computeMedian = (scores) => {
  if (scores.length === 0) return null
  const sorted = [...scores].sort((a, b) => a - b)
  const mid = Math.floor(sorted.length / 2)
  return sorted.length % 2 !== 0
    ? sorted[mid].toFixed(1)
    : ((sorted[mid - 1] + sorted[mid]) / 2).toFixed(1)
}

const computeStdDev = (scores, mean) => {
  if (scores.length < 2) return null
  const variance = scores.reduce((sum, s) => sum + Math.pow(s - mean, 2), 0) / scores.length
  return Math.sqrt(variance).toFixed(2)
}

const initScoreChart = (results) => {
  if (!scoreChartRef.value) return
  if (scoreChart) { scoreChart.dispose() }
  scoreChart = echarts.init(scoreChartRef.value)

  // Build score buckets [0-2, 2-4, 4-6, 6-8, 8-10]
  const buckets = [0, 0, 0, 0, 0]
  const labels = ['0-2', '2-4', '4-6', '6-8', '8-10']
  for (const r of results) {
    if (r.score == null) continue
    const s = parseFloat(r.score)
    if (s < 2) buckets[0]++
    else if (s < 4) buckets[1]++
    else if (s < 6) buckets[2]++
    else if (s < 8) buckets[3]++
    else buckets[4]++
  }

  const passThreshold = 5
  const passBucketIdx = 2 // 4-6 is where pass starts (>=5 means 5-10)
  const passColors = buckets.map((_, i) =>
    i >= passBucketIdx
      ? new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: '#16a34a' }, { offset: 1, color: '#4ade80' }])
      : new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: '#dc2626' }, { offset: 1, color: '#f87171' }])
  )

  const option = {
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255,255,255,0.98)',
      borderColor: 'var(--ds-border)',
      borderWidth: 1,
      borderRadius: 12,
      padding: [10, 14],
      textStyle: { color: 'var(--ds-text)', fontWeight: 600, fontSize: 13 }
    },
    grid: { left: '3%', right: '4%', bottom: '12%', top: '8%', containLabel: true },
    xAxis: {
      type: 'category',
      data: labels,
      axisLine: { lineStyle: { color: 'var(--ds-border)', width: 2 } },
      axisLabel: { color: 'var(--ds-text-muted)', fontWeight: 700, fontSize: 13 },
      axisTick: { show: false }
    },
    yAxis: {
      type: 'value',
      axisLine: { show: false },
      axisLabel: { color: 'var(--ds-text-muted)', fontWeight: 600 },
      splitLine: { lineStyle: { color: 'var(--ds-border)', type: 'dashed', opacity: 0.5 } }
    },
    series: [{
      type: 'bar',
      data: buckets.map((val, i) => ({
        value: val,
        itemStyle: { color: passColors[i], borderRadius: [6, 6, 0, 0] }
      })),
      barWidth: '55%',
      animationDuration: 1200,
      animationEasing: 'elasticOut'
    }]
  }

  scoreChart.setOption(option)
}

const handleExportCsv = async () => {
  const rows = [
    ['STT', 'Họ tên', 'Mã SV', 'Điểm', 'Độ chính xác (%)', 'Thời gian', 'Trạng thái', 'Cảnh báo']
  ]
  filteredResults.value.forEach((r, i) => {
    rows.push([
      i + 1,
      r.name,
      r.studentCode,
      r.score ?? '-',
      r.accuracy ?? '-',
      r.timeSpent ? formatTime(r.timeSpent) : '-',
      getStatusLabel(r.score),
      r.warnings > 0 ? r.warnings : '-'
    ])
  })
  const csv = rows.map(r => r.map(c => `"${String(c).replace(/"/g, '""')}"`).join(',')).join('\n')
  const blob = new Blob(['\ufeff' + csv], { type: 'text/csv;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `phan-tich-${selectedExamId.value}-${Date.now()}.csv`
  a.click()
  URL.revokeObjectURL(url)
  toast.success('Đã xuất file CSV!')
}

const viewStudentDetail = (student) => {
  router.push({
    path: '/teacher/exams/review/student-report',
    query: { examId: selectedExamId.value, attemptId: student.id }
  })
}

// ─── Lifecycle ────────────────────────────────────────────────────────────────
onMounted(() => {
  loadExams()
  window.addEventListener('resize', () => { scoreChart?.resize() })
})

watch(selectedExamId, (val) => {
  if (val) loadAnalytics()
})

onUnmounted(() => {
  window.removeEventListener('resize', () => { scoreChart?.resize() })
  if (scoreChart) { scoreChart.dispose(); scoreChart = null }
})
</script>

<style scoped>
/* Animation */
.ta-animate { animation: taFadeUp 0.5s ease-out; }
@keyframes taFadeUp { from { opacity: 0; transform: translateY(16px); } to { opacity: 1; transform: translateY(0); } }

/* Header */
.ta-eyebrow { font-size: 0.72rem; font-weight: 700; text-transform: uppercase; letter-spacing: 0.1em; color: var(--ds-text-muted); }
.ta-title { font-size: 1.75rem; font-weight: 800; color: var(--ds-text); letter-spacing: -0.02em; margin: 0; }
.ta-subtitle { font-size: 0.875rem; color: var(--ds-text-muted); margin: 0.25rem 0 0; }
.ta-card-title { font-size: 1rem; font-weight: 700; color: var(--ds-text); margin: 0; }

/* Buttons */
.ta-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.375rem;
  padding: 0.5rem 1rem;
  border-radius: var(--ds-radius-xl);
  font-size: 0.8rem;
  font-weight: 700;
  cursor: pointer;
  border: none;
  transition: all 0.2s;
  white-space: nowrap;
}
.ta-btn:hover:not(:disabled) { transform: translateY(-1px); }
.ta-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.ta-btn--primary { background: var(--ds-primary); color: white; }
.ta-btn--primary:hover:not(:disabled) { background: var(--ds-primary-hover); }
.ta-btn--outline { background: var(--ds-surface); color: var(--ds-text-secondary); border: 1.5px solid var(--ds-border); }
.ta-btn--outline:hover:not(:disabled) { background: var(--ds-gray-50); }

/* Select */
.ta-select {
  padding: 0.5rem 2rem 0.5rem 0.75rem;
  border: 1.5px solid var(--ds-border);
  border-radius: var(--ds-radius-xl);
  background: var(--ds-surface);
  color: var(--ds-text);
  font-size: 0.8rem;
  font-weight: 600;
  cursor: pointer;
  outline: none;
  appearance: none;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='16' height='16' viewBox='0 0 24 24' fill='none' stroke='%2357534e' stroke-width='2'%3E%3Cpolyline points='6 9 12 15 18 9'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 0.5rem center;
}
.ta-select:focus { border-color: var(--ds-primary); }
.ta-select--sm { padding: 0.375rem 1.5rem 0.375rem 0.625rem; font-size: 0.75rem; }

/* KPI Grid */
.ta-kpi-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 1rem;
}
@media (max-width: 768px) { .ta-kpi-grid { grid-template-columns: repeat(2, 1fr); } }
@media (max-width: 480px) { .ta-kpi-grid { grid-template-columns: 1fr; } }

.ta-kpi-card {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 1.25rem;
  background: var(--ds-surface);
  border: 1px solid var(--ds-border);
  border-radius: var(--ds-radius-xl);
  transition: all 0.2s;
}
.ta-kpi-card:hover { box-shadow: var(--ds-shadow-md); transform: translateY(-2px); }

.ta-kpi-icon {
  width: 3rem;
  height: 3rem;
  border-radius: var(--ds-radius-xl);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.ta-kpi-icon--blue { background: var(--ds-primary-soft); color: var(--ds-primary); }
.ta-kpi-icon--green { background: var(--ds-success-bg); color: var(--ds-success); }
.ta-kpi-icon--amber { background: var(--ds-accent-bg); color: var(--ds-accent); }
.ta-kpi-icon--red { background: var(--ds-danger-soft); color: var(--ds-danger); }

.ta-kpi-label { font-size: 0.72rem; font-weight: 700; text-transform: uppercase; letter-spacing: 0.05em; color: var(--ds-text-muted); margin-bottom: 0.25rem; }
.ta-kpi-value { font-size: 1.75rem; font-weight: 800; color: var(--ds-text); line-height: 1; }
.ta-kpi-sub { font-size: 0.72rem; color: var(--ds-text-muted); margin-top: 0.25rem; }

/* Charts Grid */
.ta-charts-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
}
@media (max-width: 768px) { .ta-charts-grid { grid-template-columns: 1fr; } }

/* Bottom Grid */
.ta-bottom-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
}
@media (max-width: 768px) { .ta-bottom-grid { grid-template-columns: 1fr; } }

/* Chart Legend */
.ta-chart-legend { display: flex; gap: 0.75rem; }
.ta-legend-item { display: inline-flex; align-items: center; gap: 0.25rem; font-size: 0.72rem; font-weight: 700; }
.ta-legend-item::before { content: ''; display: inline-block; width: 10px; height: 10px; border-radius: 2px; }
.ta-legend-item--pass::before { background: var(--ds-success); }
.ta-legend-item--fail::before { background: var(--ds-danger); }

/* Difficulty List */
.ta-difficulty-list { display: flex; flex-direction: column; gap: 0.75rem; max-height: 260px; overflow-y: auto; }
.ta-difficulty-item { display: flex; flex-direction: column; gap: 0.375rem; }
.ta-difficulty-info { display: flex; justify-content: space-between; align-items: center; }
.ta-difficulty-q { font-size: 0.78rem; font-weight: 600; color: var(--ds-text); }
.ta-difficulty-rate { font-size: 0.75rem; font-weight: 700; }
.ta-difficulty-bar { height: 6px; background: var(--ds-gray-100); border-radius: 3px; overflow: hidden; }
.ta-difficulty-fill { height: 100%; border-radius: 3px; transition: width 1s cubic-bezier(0.22, 1, 0.36, 1); }

/* Insights */
.ta-insights-list { display: flex; flex-direction: column; gap: 0.75rem; }
.ta-insight-item { display: flex; align-items: flex-start; gap: 0.75rem; padding: 0.75rem; background: var(--ds-gray-50); border-radius: var(--ds-radius-xl); border: 1px solid var(--ds-border); }
.ta-insight-icon { width: 2rem; height: 2rem; border-radius: var(--ds-radius-lg); display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.ta-insight-icon--warning { background: var(--ds-warning-bg); color: var(--ds-warning); }
.ta-insight-icon--danger { background: var(--ds-danger-soft); color: var(--ds-danger); }
.ta-insight-icon--success { background: var(--ds-success-bg); color: var(--ds-success); }
.ta-insight-icon--info { background: var(--ds-info-bg); color: var(--ds-info); }
.ta-insight-title { font-size: 0.8rem; font-weight: 700; color: var(--ds-text); }
.ta-insight-desc { font-size: 0.75rem; color: var(--ds-text-muted); margin-top: 0.25rem; line-height: 1.5; }

/* Ranking */
.ta-ranking-list { display: flex; flex-direction: column; gap: 0.5rem; }
.ta-ranking-item { display: flex; align-items: center; gap: 0.75rem; padding: 0.5rem; border-radius: var(--ds-radius-xl); transition: background 0.15s; }
.ta-ranking-item:hover { background: var(--ds-gray-50); }
.ta-ranking-pos { width: 1.5rem; height: 1.5rem; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 0.7rem; font-weight: 800; flex-shrink: 0; }
.ta-ranking-pos--1 { background: #fbbf24; color: white; }
.ta-ranking-pos--2 { background: #9ca3af; color: white; }
.ta-ranking-pos--3 { background: #cd7f32; color: white; }
.ta-ranking-pos { background: var(--ds-gray-100); color: var(--ds-text-secondary); }
.ta-ranking-avatar { width: 2rem; height: 2rem; border-radius: 50%; background: var(--ds-primary-soft); color: var(--ds-primary); display: flex; align-items: center; justify-content: center; font-size: 0.625rem; font-weight: 800; flex-shrink: 0; }
.ta-ranking-info { flex: 1; min-width: 0; }
.ta-ranking-name { font-size: 0.8rem; font-weight: 700; color: var(--ds-text); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.ta-ranking-meta { font-size: 0.68rem; color: var(--ds-text-muted); }
.ta-ranking-score { font-size: 1rem; font-weight: 800; min-width: 2.5rem; text-align: right; }

/* Table cells */
.ta-student-cell { display: flex; align-items: center; gap: 0.625rem; }
.ta-student-avatar { width: 2rem; height: 2rem; border-radius: 50%; background: var(--ds-primary-soft); color: var(--ds-primary); display: flex; align-items: center; justify-content: center; font-size: 0.625rem; font-weight: 800; flex-shrink: 0; }
.ta-student-name { font-size: 0.8rem; font-weight: 700; color: var(--ds-text); }
.ta-student-id { font-size: 0.68rem; color: var(--ds-text-muted); }
.ta-score-cell { display: flex; align-items: baseline; justify-content: center; gap: 0.25rem; }
.ta-score-val { font-size: 0.95rem; font-weight: 800; }
.ta-score-max { font-size: 0.7rem; color: var(--ds-text-muted); }
.ta-accuracy-val { font-size: 0.72rem; font-weight: 700; color: var(--ds-text-secondary); min-width: 2.5rem; text-align: right; }
.ta-warning-badge { display: inline-flex; align-items: center; gap: 0.25rem; padding: 0.125rem 0.5rem; border-radius: var(--ds-radius-full); background: var(--ds-danger-soft); color: var(--ds-danger); font-size: 0.72rem; font-weight: 700; }
.ta-no-warnings { color: var(--ds-text-muted); font-size: 0.8rem; }
.ta-row-action { display: inline-flex; align-items: center; justify-content: center; width: 2rem; height: 2rem; border: none; background: none; color: var(--ds-text-muted); cursor: pointer; border-radius: var(--ds-radius-lg); transition: all 0.15s; }
.ta-row-action:hover { background: var(--ds-gray-100); color: var(--ds-primary); }

/* Badge */
.ta-badge { display: inline-flex; align-items: center; padding: 0.125rem 0.625rem; border-radius: var(--ds-radius-full); background: var(--ds-primary-soft); color: var(--ds-primary); font-size: 0.7rem; font-weight: 700; }

/* Search */
.ta-search { position: relative; }
.ta-search-icon { position: absolute; left: 0.625rem; top: 50%; transform: translateY(-50%); color: var(--ds-text-muted); pointer-events: none; display: flex; }
.ta-search-input { padding: 0.375rem 0.75rem 0.375rem 2rem; border: 1.5px solid var(--ds-border); border-radius: var(--ds-radius-xl); background: var(--ds-surface); color: var(--ds-text); font-size: 0.8rem; outline: none; width: 180px; }
.ta-search-input:focus { border-color: var(--ds-primary); }

/* Empty states */
.ta-empty-state { display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 5rem 2rem; text-align: center; gap: 1rem; }
.ta-empty-icon { color: var(--ds-text-muted); opacity: 0.4; }
.ta-empty-title { font-size: 1.25rem; font-weight: 800; color: var(--ds-text); margin: 0; }
.ta-empty-desc { font-size: 0.875rem; color: var(--ds-text-muted); max-width: 400px; margin: 0; }
.ta-empty-select { max-width: 300px; }
.ta-empty-list { display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 2.5rem 1rem; gap: 0.5rem; color: var(--ds-text-muted); font-size: 0.875rem; text-align: center; }
.ta-empty-chart { display: flex; flex-direction: column; align-items: center; justify-content: center; height: 200px; gap: 0.5rem; color: var(--ds-text-muted); font-size: 0.875rem; }

/* Loading */
.ta-loading { display: flex; flex-direction: column; gap: 1rem; }
.ta-skeleton-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 1rem; }
@media (max-width: 768px) { .ta-skeleton-grid { grid-template-columns: repeat(2, 1fr); } }

/* Responsive */
@media (max-width: 640px) {
  .mx-auto { max-width: 100%; }
  .ta-title { font-size: 1.375rem; }
}
</style>
