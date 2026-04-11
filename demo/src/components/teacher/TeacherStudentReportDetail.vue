<template>
  <div class="bg-[var(--ds-bg)] min-h-full">
    <div class="mx-auto max-w-7xl px-4 pb-10 pt-4 sm:px-6 lg:px-8">

      <!-- Breadcrumb -->
      <div class="mb-4 ds-animate-fade-up">
        <div class="flex items-center gap-2 text-sm" style="color: var(--ds-text-muted)">
          <RouterLink
            class="flex items-center gap-1 transition-colors hover:text-[var(--ds-primary)]"
            style="color: var(--ds-text-muted)"
            to="/teacher/exams"
          >
            <LucideIcon name="assignment" size="16" />
            Đề thi
          </RouterLink>
          <LucideIcon name="chevron_right" size="12" />
          <RouterLink
            class="flex items-center gap-1 transition-colors hover:text-[var(--ds-primary)]"
            style="color: var(--ds-text-muted)"
            :to="{ path: '/teacher/exams/review/summary', query: { examId, title: examTitle } }"
          >
            Tổng quan điểm
          </RouterLink>
          <LucideIcon name="chevron_right" size="12" />
          <span class="font-medium" style="color: var(--ds-text)">Chi tiết học sinh</span>
        </div>
      </div>

      <!-- Page Header -->
      <div class="mb-6 ds-animate-fade-up" style="animation-delay: 0.05s">
        <PageHeader
          :eyebrow="'Báo cáo chi tiết'"
          :title="examTitle"
          :subtitle="pageSubtitle"
        >
          <template #actions>
            <button
              type="button"
              class="inline-flex items-center justify-center gap-2 rounded-lg px-4 py-2.5 text-sm font-bold transition-all hover:-translate-y-0.5"
              style="background-color: var(--ds-surface); color: var(--ds-text); border: 1px solid var(--ds-border); box-shadow: var(--ds-shadow-sm)"
              @click="goBack"
            >
              <LucideIcon name="arrow_back" size="18" />
              <span>Quay lại</span>
            </button>
          </template>
        </PageHeader>
      </div>

      <div
        v-if="loadError"
        class="mb-6 rounded-[var(--ds-radius-xl)] border px-4 py-3 text-sm font-medium"
        style="border-color: var(--ds-danger-border); background-color: var(--ds-danger-bg); color: var(--ds-danger)"
      >
        {{ loadError }}
      </div>

      <!-- Student Info Header -->
      <div class="mb-6 ds-animate-fade-up" style="animation-delay: 0.1s">
        <DsCard padding="lg">
          <div class="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
            <div class="flex items-center gap-4">
              <div
                class="flex size-14 items-center justify-center rounded-full text-xl font-bold"
                style="background-color: var(--ds-primary); color: white"
              >
                {{ studentInitials }}
              </div>
              <div>
                <h2 class="text-xl font-bold" style="color: var(--ds-text)">{{ studentInfo.name }}</h2>
                <p class="text-sm" style="color: var(--ds-text-muted)">Mã học sinh: {{ studentInfo.studentId }}</p>
                <p class="text-xs" style="color: var(--ds-text-secondary)">{{ studentInfo.email }}</p>
              </div>
            </div>
            <div class="flex items-center gap-2">
              <StatusChip :status="studentInfo.statusKey" :label="studentInfo.status" />
            </div>
          </div>
        </DsCard>
      </div>

      <!-- Score Summary Cards -->
      <div class="mb-6 grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4 ds-animate-fade-up" style="animation-delay: 0.15s">
        <DsStatCard
          :label="'Điểm tổng'"
          :value="scoreSummary.totalScore"
          :sub-value="'/ 10'"
          :icon="'grade'"
          :badge="'Điểm'"
          :badge-variant="scoreSummary.badgeVariant"
        />
        <DsStatCard
          :label="'Độ chính xác'"
          :value="scoreSummary.accuracy"
          :sub-value="'%'"
          :icon="'verified'"
          :sub="scoreSummary.accuracyNote"
        />
        <DsStatCard
          :label="'Thời gian làm bài'"
          :value="scoreSummary.timeSpent"
          :icon="'timer'"
          :sub="'Tối đa: ' + scoreSummary.maxTime"
        />
        <DsStatCard
          :label="'Xếp hạng'"
          :value="scoreSummary.rank"
          :sub-value="'trên ' + scoreSummary.totalStudents"
          :icon="'leaderboard'"
          :sub="'Lớp ' + scoreSummary.className"
        />
      </div>

      <!-- Questions Review Table -->
      <div class="mb-6 ds-animate-fade-up" style="animation-delay: 0.2s">
        <DsCard padding="none">
          <template #header>
            <div class="flex items-center justify-between px-5 pt-5">
              <div class="flex items-center gap-2">
                <LucideIcon name="quiz" />
                <h3 class="text-lg font-bold" style="color: var(--ds-text)">Chi tiết câu hỏi</h3>
              </div>
              <div class="flex items-center gap-4 text-sm">
                <span class="flex items-center gap-1.5">
                  <span class="size-2 rounded-full" style="background-color: var(--ds-success)"></span>
                  <span style="color: var(--ds-text-muted)">Đúng</span>
                </span>
                <span class="flex items-center gap-1.5">
                  <span class="size-2 rounded-full" style="background-color: var(--ds-danger)"></span>
                  <span style="color: var(--ds-text-muted)">Sai</span>
                </span>
                <span class="flex items-center gap-1.5">
                  <span class="size-2 rounded-full" style="background-color: var(--ds-warning)"></span>
                  <span style="color: var(--ds-text-muted)">Bỏ qua</span>
                </span>
              </div>
            </div>
          </template>
          <DataTable
            :columns="questionColumns"
            :data="questionsData"
            :row-key="'id'"
          >
            <template #cell-status="{ row }">
              <span
                class="inline-flex items-center justify-center size-7 rounded-full text-xs font-bold"
                :style="{
                  backgroundColor: getStatusBg(row.status),
                  color: getStatusColor(row.status)
                }"
              >
                <LucideIcon :name="getStatusIcon(row.status)" size="14" />
              </span>
            </template>
            <template #cell-selectedAnswer="{ row }">
              <span v-if="row.selectedAnswer" class="text-sm" style="color: var(--ds-text)">
                {{ row.selectedAnswer }}
              </span>
              <span v-else class="text-sm italic" style="color: var(--ds-text-muted)">Không chọn</span>
            </template>
            <template #cell-correctAnswer="{ row }">
              <span class="text-sm font-semibold" style="color: var(--ds-success)">
                {{ row.correctAnswer }}
              </span>
            </template>
          </DataTable>
        </DsCard>
      </div>

      <!-- Answer Distribution Chart (optional) -->
      <div class="ds-animate-fade-up" style="animation-delay: 0.25s">
        <DsCard padding="lg">
          <template #header>
            <div class="flex items-center gap-2">
              <LucideIcon name="pie_chart" />
              <h3 class="text-lg font-bold" style="color: var(--ds-text)">Phân bố câu trả lời</h3>
            </div>
          </template>
          <div class="grid grid-cols-3 gap-4 text-center">
            <div
              class="rounded-lg p-4"
              style="background-color: rgba(34, 197, 94, 0.06); border: 1px solid rgba(22, 163, 74, 0.15)"
            >
              <p class="text-2xl font-bold" style="color: #16a34a">{{ answerStats.correct }}</p>
              <p class="text-sm font-medium" style="color: #16a34a">Câu đúng</p>
            </div>
            <div
              class="rounded-lg p-4"
              style="background-color: rgba(220, 38, 38, 0.06); border: 1px solid rgba(220, 38, 38, 0.15)"
            >
              <p class="text-2xl font-bold" style="color: #dc2626">{{ answerStats.wrong }}</p>
              <p class="text-sm font-medium" style="color: #dc2626">Câu sai</p>
            </div>
            <div
              class="rounded-lg p-4"
              style="background-color: rgba(245, 158, 11, 0.06); border: 1px solid rgba(245, 158, 11, 0.15)"
            >
              <p class="text-2xl font-bold" style="color: #d97706">{{ answerStats.skipped }}</p>
              <p class="text-sm font-medium" style="color: #d97706">Bỏ qua</p>
            </div>
          </div>
        </DsCard>
      </div>

    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { RouterLink, useRouter, useRoute } from 'vue-router'
import { getAttemptDetail, getAttemptReport, listExamAttempts } from '../../services/attemptService'
import { ApiError } from '../../services/apiClient'
import {
  formatAttemptAnswer,
  formatScoreTen,
  getAttemptStatusMeta,
  parseResultOptions,
  scorePercentValue
} from '../../utils/attemptResult'
import PageHeader from '../ui/PageHeader.vue'
import DsCard from '../ui/DsCard.vue'
import DsStatCard from '../ui/DsStatCard.vue'
import DataTable from '../ui/DataTable.vue'
import StatusChip from '../ui/StatusChip.vue'

const router = useRouter()
const route = useRoute()

const examId = computed(() => Number.parseInt(String(route.query.examId || ''), 10) || null)
const attemptId = computed(() => Number.parseInt(String(route.query.attemptId || ''), 10) || null)
const examTitle = computed(() => route.query.title || 'Đề thi')
const attemptDetail = ref(null)
const attemptReport = ref(null)
const examAttempts = ref([])
const isLoading = ref(false)
const loadError = ref('')
let latestRequestId = 0

const selectedAttempt = computed(() =>
  examAttempts.value.find((attempt) => Number(attempt.id) === attemptId.value) || null
)

const studentInfo = computed(() => {
  const statusMeta = getAttemptStatusMeta(attemptDetail.value?.status || selectedAttempt.value?.status)
  return {
    name:
      selectedAttempt.value?.student ||
      attemptDetail.value?.student ||
      String(route.query.student || 'Học sinh không rõ'),
    studentId:
      selectedAttempt.value?.studentCode ||
      String(route.query.studentCode || `AT-${attemptId.value || 'N/A'}`),
    email:
      selectedAttempt.value?.email ||
      String(route.query.email || 'Chưa có email'),
    status: statusMeta.label,
    statusKey: statusMeta.key
  }
})

const studentInitials = computed(() => {
  const parts = String(studentInfo.value.name || '').trim().split(/\s+/).filter(Boolean)
  if (!parts.length) return 'HS'
  const first = parts[0]?.charAt(0) || 'H'
  const last = parts[parts.length - 1]?.charAt(0) || 'S'
  return `${last}${first}`.toUpperCase()
})

const scoreSummary = computed(() => {
  const scoreRaw = scorePercentValue(attemptReport.value?.score ?? attemptDetail.value?.score ?? 0)
  const totalScore = Number(formatScoreTen(scoreRaw))
  const submittedAttempts = examAttempts.value
    .filter((attempt) => ['SUBMITTED', 'AUTO_SUBMITTED'].includes(String(attempt.status || '').toUpperCase()))
    .sort((a, b) => Number(b.score || 0) - Number(a.score || 0))
  const rankIndex = submittedAttempts.findIndex((attempt) => Number(attempt.id) === attemptId.value)
  const timeSpentMinutes = (() => {
    const startedAt = attemptDetail.value?.startedAt
    const submittedAt = attemptDetail.value?.submittedAt
    if (!startedAt || !submittedAt) return '-'
    const elapsedMs = new Date(submittedAt).getTime() - new Date(startedAt).getTime()
    if (!Number.isFinite(elapsedMs) || elapsedMs <= 0) return '-'
    return `${Math.max(1, Math.round(elapsedMs / 60000))} phút`
  })()
  const maxTimeMinutes = (() => {
    const startedAt = attemptDetail.value?.startedAt
    const deadlineAt = attemptDetail.value?.deadlineAt
    if (!startedAt || !deadlineAt) return '-'
    const elapsedMs = new Date(deadlineAt).getTime() - new Date(startedAt).getTime()
    if (!Number.isFinite(elapsedMs) || elapsedMs <= 0) return '-'
    return `${Math.max(1, Math.round(elapsedMs / 60000))} phút`
  })()
  const totalStudents = submittedAttempts.length || examAttempts.value.length || 0
  const correctCount = Number(attemptReport.value?.correctCount || 0)
  const totalQuestions = Number(attemptDetail.value?.totalQuestions || attemptReport.value?.answers?.length || 0)
  const accuracy = totalQuestions > 0 ? Math.round((correctCount / totalQuestions) * 100) : 0
  const badgeVariant = totalScore >= 8 ? 'success' : totalScore >= 5 ? 'warning' : 'danger'
  return {
    totalScore: totalScore.toFixed(1),
    badgeVariant,
    accuracy: String(accuracy),
    accuracyNote: `${correctCount}/${totalQuestions} câu`,
    timeSpent: timeSpentMinutes,
    maxTime: maxTimeMinutes,
    rank: rankIndex >= 0 ? String(rankIndex + 1) : '-',
    totalStudents: String(totalStudents || '-'),
    className: selectedAttempt.value?.className || examTitle.value
  }
})

const answerStats = computed(() => {
  const totalQuestions = Number(attemptDetail.value?.totalQuestions || attemptReport.value?.answers?.length || 0)
  const answeredCount = Number(attemptReport.value?.answeredCount || 0)
  const correct = Number(attemptReport.value?.correctCount || 0)
  return {
    correct,
    wrong: Math.max(answeredCount - correct, 0),
    skipped: Math.max(totalQuestions - answeredCount, 0)
  }
})

const questionColumns = [
  { key: 'number', label: '#', width: '60px', align: 'center' },
  { key: 'content', label: 'Câu hỏi' },
  { key: 'selectedAnswer', label: 'Câu trả lời của bạn' },
  { key: 'correctAnswer', label: 'Đáp án đúng' },
  { key: 'status', label: 'Kết quả', width: '80px', align: 'center' }
]

const questionsData = computed(() => (attemptReport.value?.answers || []).map((answer, index) => {
  const questionContent = typeof answer.question === 'string'
    ? answer.question
    : (answer.question?.content || answer.question?.text || 'Câu hỏi không có nội dung')
  const questionOptions = answer.options || answer.questionOptions || answer.question?.options || []
  const normalizedOptions = parseResultOptions(questionOptions)
  const hasSelected = String(answer.selectedAnswer || '').trim() !== ''
  const status = !hasSelected
    ? 'skipped'
    : (answer.correct === true ? 'correct' : 'wrong')
  return {
    id: answer.questionId || index,
    number: index + 1,
    content: questionContent,
    selectedAnswer: hasSelected ? formatAttemptAnswer(answer.selectedAnswer, normalizedOptions) : '',
    correctAnswer: formatAttemptAnswer(answer.correctAnswer, normalizedOptions),
    status
  }
}))

const pageSubtitle = computed(() => {
  if (attemptDetail.value?.submittedAt) {
    return `Nộp bài lúc ${new Date(attemptDetail.value.submittedAt).toLocaleString('vi-VN')}`
  }
  return 'Xem chi tiết kết quả làm bài của học sinh'
})

const getStatusBg = (status) => {
  const bgs = {
    correct: 'var(--ds-success-bg)',
    wrong: 'var(--ds-danger-bg)',
    skipped: 'var(--ds-warning-bg)'
  }
  return bgs[status] || 'var(--ds-gray-100)'
}

const getStatusColor = (status) => {
  const colors = {
    correct: 'var(--ds-success)',
    wrong: 'var(--ds-danger)',
    skipped: 'var(--ds-warning)'
  }
  return colors[status] || 'var(--ds-text-muted)'
}

const getStatusIcon = (status) => {
  const icons = {
    correct: 'check',
    wrong: 'close',
    skipped: 'remove'
  }
  return icons[status] || 'help'
}

const goBack = () => {
  router.back()
}

const loadReport = async () => {
  const requestId = ++latestRequestId
  attemptDetail.value = null
  attemptReport.value = null
  examAttempts.value = []
  if (!attemptId.value) {
    loadError.value = 'Thiếu mã lượt làm. Vui lòng mở lại từ trang tổng quan kết quả.'
    return
  }

  isLoading.value = true
  loadError.value = ''
  try {
    const [detailPayload, reportPayload, attemptsPayload] = await Promise.all([
      getAttemptDetail(attemptId.value),
      getAttemptReport(attemptId.value),
      examId.value ? listExamAttempts(examId.value) : Promise.resolve([])
    ])
    if (requestId !== latestRequestId) {
      return
    }
    attemptDetail.value = detailPayload
    attemptReport.value = reportPayload
    examAttempts.value = Array.isArray(attemptsPayload) ? attemptsPayload : []
  } catch (error) {
    if (requestId !== latestRequestId) {
      return
    }
    loadError.value = error instanceof ApiError ? error.message : 'Không thể tải báo cáo học sinh.'
  } finally {
    if (requestId === latestRequestId) {
      isLoading.value = false
    }
  }
}

onMounted(loadReport)
watch(() => [route.query.examId, route.query.attemptId], loadReport)
</script>

<style scoped>
.ds-animate-fade-up {
  animation: fadeUp 0.5s ease-out;
}

@keyframes fadeUp {
  from {
    opacity: 0;
    transform: translateY(18px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
