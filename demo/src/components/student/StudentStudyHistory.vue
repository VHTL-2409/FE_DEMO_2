<template>
  <div class="bg-[var(--ds-bg)] min-h-full">
    <div class="mx-auto w-full px-3 pb-10 pt-4 sm:px-5 lg:px-8 xl:px-10" style="max-width: 1640px">

      <!-- Header -->
      <div class="mb-6 flex items-start justify-between ds-animate-fade-up">
        <div>
          <h1 class="m-0 text-xl font-extrabold leading-tight" style="color: var(--ds-text)">Lịch sử học tập</h1>
          <p class="mt-1 mb-0 text-sm font-medium" style="color: var(--ds-text-muted)">
            {{ totalCount }} bài đã làm · {{ totalPractice }} luyện tập · {{ totalExam }} bài thi
          </p>
        </div>
        <div class="hidden flex-wrap gap-2 sm:flex">
          <div class="flex items-center gap-1.5 rounded-full px-3.5 py-1.5 text-xs font-bold" style="background-color: var(--ds-success-bg); color: var(--ds-success)">
            <LucideIcon name="check_circle" size="14" />
            Đã hoàn thành
          </div>
          <div class="flex items-center gap-1.5 rounded-full px-3.5 py-1.5 text-xs font-bold" style="background-color: var(--ds-info-bg); color: var(--ds-info)">
            <LucideIcon name="school" size="14" />
            Theo thứ tự thời gian
          </div>
        </div>
      </div>

      <!-- Summary + Trend row -->
      <div class="mb-6 grid grid-cols-1 gap-5 xl:grid-cols-2 xl:items-start min-w-0 isolate ds-animate-fade-up" style="animation-delay: 0.06s">
        <ResultSummaryCard :attempts="allAttempts" />
        <ScoreTrendCard :attempts="allAttempts" />
      </div>

      <!-- History section header -->
      <div class="mb-4 flex items-center justify-between ds-animate-fade-up" style="animation-delay: 0.1s">
        <h2 class="m-0 text-base font-extrabold" style="color: var(--ds-text)">Danh sách bài đã làm</h2>
        <span class="text-xs font-semibold" style="color: var(--ds-text-muted)">
          Hiển thị {{ sessions.length }} / {{ totalCount }} · Trang {{ currentPage }} / {{ totalPages }}
        </span>
      </div>

      <!-- History list -->
      <div class="ds-animate-fade-up" style="animation-delay: 0.13s">
        <ResultHistoryList
          :sessions="sessions"
          :loading="isLoading"
          :current-page="currentPage"
          :total-pages="totalPages"
          :total-count="totalCount"
          :selected-id="selectedSessionId"
          @session-click="openSession"
          @go-dashboard="goDashboard"
          @prev-page="goToPrevPage"
          @next-page="goToNextPage"
        />
      </div>

      <!-- Inline result panel -->
      <Transition name="result-expand">
        <div v-if="selectedSession" class="mt-5">
          <!-- Panel header -->
          <div class="mb-3 flex items-center justify-between">
            <div class="flex items-center gap-2.5">
              <div class="flex items-center gap-1.5 rounded-full px-3.5 py-1.5 text-xs font-bold" style="background-color: var(--ds-primary-soft); color: var(--ds-primary)">
                <LucideIcon :name="selectedSession.isPractice ? 'model_training' : 'menu_book'" size="14" />
                {{ selectedSession.isPractice ? 'Luyện tập' : 'Bài thi' }}
              </div>
              <h3 class="m-0 text-sm font-extrabold" style="color: var(--ds-text)">
                {{ selectedSession.examTitle || 'Kết quả chi tiết' }}
              </h3>
            </div>
            <button
              type="button"
              class="flex items-center gap-1.5 rounded-lg px-3 py-1.5 text-xs font-semibold cursor-pointer transition-all"
              style="background-color: var(--ds-gray-100); color: var(--ds-text-secondary); border: 1.5px solid var(--ds-border)"
              @click="closePanel"
            >
              <LucideIcon name="close" size="14" />
              Đóng
            </button>
          </div>

          <!-- Loading detail -->
          <div v-if="detailLoading" class="mb-4 rounded-2xl border p-5" style="border-color: var(--ds-border); background-color: var(--ds-surface)">
            <div class="flex items-center justify-center gap-3 py-8">
              <div class="size-5 rounded-full border-2 animate-spin" style="border-color: var(--ds-primary); border-top-color: transparent"></div>
              <span class="text-sm font-medium" style="color: var(--ds-text-secondary)">Đang tải kết quả chi tiết...</span>
            </div>
          </div>

          <!-- Result detail -->
          <ResultDetailPanel
            v-else-if="detailData"
            :loading="detailLoading"
            :score="scorePercent"
            :time-taken="timeTaken"
            :rank-order="rankOrder"
            :correct-count="correctCount"
            :incorrect-count="incorrectCount"
            :skipped-count="skippedCount"
            :total-questions="totalQuestions"
            :review-answers="reviewAnswers"
          />

          <!-- Detail error -->
          <div v-else-if="!detailLoading && !detailData && detailError" class="mb-4 flex flex-col items-center justify-center gap-3 rounded-2xl border p-5 text-center" style="border-color: var(--ds-border); background-color: var(--ds-surface)">
            <div class="size-10 rounded-full flex items-center justify-center" style="background-color: var(--ds-danger-bg)">
              <LucideIcon name="error_outline" size="20" style="color: var(--ds-danger)" />
            </div>
            <p class="m-0 text-sm font-medium" style="color: var(--ds-text-secondary)">Không thể tải chi tiết bài thi này.</p>
            <button
              type="button"
              class="flex items-center gap-1.5 rounded-lg px-4 py-2 text-sm font-semibold cursor-pointer transition-all"
              style="background-color: var(--ds-primary); color: white; box-shadow: 0 2px 8px rgba(79,70,229,0.2)"
              @click="loadDetail(selectedSessionId)"
            >
              <LucideIcon name="refresh" size="14" />
              Thử lại
            </button>
          </div>
        </div>
      </Transition>

    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { listMyAttempts, getAttemptDetail, getAttemptReport } from '../../services/attemptService'
import { useToast } from '../../composables/useToast'
import LucideIcon from '../common/LucideIcon.vue'
import ResultSummaryCard from './results/ResultSummaryCard.vue'
import ResultHistoryList from './results/ResultHistoryList.vue'
import ScoreTrendCard from './results/ScoreTrendCard.vue'
import ResultDetailPanel from './results/ResultDetailPanel.vue'

const router = useRouter()
const toast = useToast()

// ── Session list ────────────────────────────────────────────────────────────────
const attempts = ref([])
const isLoading = ref(false)
const PAGE_SIZE = 10
const currentPage = ref(1)

const allAttempts = computed(() =>
  [...attempts.value].sort((a, b) => {
    const at = new Date(a.submittedAt || a.startedAt || 0).getTime()
    const bt = new Date(b.submittedAt || b.startedAt || 0).getTime()
    return (Number.isNaN(bt) ? 0 : bt) - (Number.isNaN(at) ? 0 : at)
  })
)

const totalCount = computed(() => allAttempts.value.length)
const totalPractice = computed(() => allAttempts.value.filter(a => a.practiceExamId).length)
const totalExam = computed(() => allAttempts.value.filter(a => !a.practiceExamId).length)

const sessions = computed(() => {
  const start = (currentPage.value - 1) * PAGE_SIZE
  return allAttempts.value.slice(start, start + PAGE_SIZE).map(a => {
    const startedAt = a.startedAt ? new Date(a.startedAt) : null
    const submittedAt = a.submittedAt ? new Date(a.submittedAt) : null
    const durationMinutes = startedAt && submittedAt
      ? Math.max(1, Math.round((submittedAt.getTime() - startedAt.getTime()) / 60000))
      : 0
    return {
      subject: a.examTitle || 'Bài thi',
      date: submittedAt
        ? submittedAt.toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric' })
        : '-',
      score: (Number(a.score || 0) / 10).toFixed(1),
      timeTaken: durationMinutes ? `${durationMinutes}m` : '-',
      isPractice: !!a.practiceExamId,
      attemptId: a.id,
      examId: a.examId,
      examTitle: a.examTitle || 'Bài thi',
      attemptedAt: submittedAt ? submittedAt.toLocaleString('vi-VN') : '-',
      isNew: Date.now() - (submittedAt?.getTime() || 0) < 7 * 24 * 60 * 60 * 1000,
    }
  })
})

const totalPages = computed(() => Math.max(1, Math.ceil(totalCount.value / PAGE_SIZE)))

const goToPrevPage = () => { if (currentPage.value > 1) currentPage.value-- }
const goToNextPage = () => { if (currentPage.value < totalPages.value) currentPage.value++ }
const goDashboard = () => router.push('/student/dashboard')

// ── Inline result panel ─────────────────────────────────────────────────────────
const selectedSessionId = ref(null)
const selectedSession = computed(() =>
  selectedSessionId.value != null
    ? sessions.value.find(s => s.attemptId === selectedSessionId.value) ?? null
    : null
)

const detailData = ref(null)
const reportData = ref(null)
const detailLoading = ref(false)
const detailError = ref(false)

const openSession = (session) => {
  if (selectedSessionId.value === session.attemptId) { closePanel(); return }
  selectedSessionId.value = session.attemptId
  loadDetail(session.attemptId)
}

const closePanel = () => {
  selectedSessionId.value = null
  detailData.value = null
  reportData.value = null
  detailError.value = false
}

const scorePercent = computed(() =>
  Math.round(Number(reportData.value?.score ?? detailData.value?.score ?? 0))
)

const timeTaken = computed(() => {
  if (!detailData.value?.startedAt || !detailData.value?.submittedAt) return '-'
  const ms = new Date(detailData.value.submittedAt).getTime() - new Date(detailData.value.startedAt).getTime()
  return `${Math.floor(ms / 60000)} phút`
})

const rankOrder = computed(() => '')

const totalQuestions = computed(() =>
  Number(
    detailData.value?.totalQuestions ??
    detailData.value?.questionCount ??
    reportData.value?.totalQuestions ??
    0
  )
)

const normalizeOptId = (val) => {
  if (!val) return ''
  const s = String(val).trim().toUpperCase()
  return ['A', 'B', 'C', 'D'].includes(s) ? s : s.charAt(0)
}

const parseOpts = (opts) => {
  if (!opts) return []
  if (!Array.isArray(opts)) return []
  return opts.map(opt => {
    if (typeof opt === 'string') {
      const m = opt.match(/^([A-D])\.\s*(.*)/)
      return m ? { id: m[1].trim(), text: m[2].trim() } : { id: '?', text: opt.trim() }
    }
    return {
      id: normalizeOptId(opt.id || opt.key || opt.letter || opt.label),
      text: String(opt.text || opt.content || opt.value || '').replace(/^[A-D]\.\s*/, '').trim()
    }
  })
}

const normalizeAns = (val) => {
  if (!val) return []
  if (Array.isArray(val)) return val.map(normalizeOptId)
  if (typeof val === 'object') return [normalizeOptId(val)]
  try { const p = JSON.parse(String(val)); return Array.isArray(p) ? p.map(normalizeOptId) : [normalizeOptId(p)] }
  catch { return [normalizeOptId(val)] }
}

const reviewAnswers = computed(() => {
  const items = reportData.value?.answers || []
  return items.map((item, index) => {
    const question = typeof item.question === 'string'
      ? item.question.replace(/^[A-D]\.\s*/, '')
      : (item.question?.content || item.question?.text || 'Câu hỏi không có nội dung')

    let rawOpts = null
    if (item.options) rawOpts = item.options
    else if (item.questionOptions) rawOpts = item.questionOptions
    else if (item.question?.options) rawOpts = item.question.options

    const selAns = normalizeAns(item.selectedAnswer || item.selectedIds || item.selectedIds_)
    const corAns = normalizeAns(item.correctAnswer || item.correctIds || item.correctIds_)
    const isCorrect = selAns.length > 0 &&
      selAns.every(id => corAns.includes(id)) &&
      corAns.length === selAns.length

    return {
      questionId: item.questionId || index,
      index: index + 1,
      question,
      options: parseOpts(rawOpts),
      selectedIds: selAns,
      correctIds: corAns,
      correct: isCorrect
    }
  })
})

const correctCount = computed(() => reviewAnswers.value.filter(a => a.correct).length)
const answeredCount = computed(() => reviewAnswers.value.filter(a => a.selectedIds.length > 0).length)
const incorrectCount = computed(() => Math.max(answeredCount.value - correctCount.value, 0))
const skippedCount = computed(() => Math.max(reviewAnswers.value.length - answeredCount.value, 0))

const loadDetail = async (attemptId) => {
  if (!attemptId) return
  detailLoading.value = true
  detailError.value = false
  try {
    const [detail, report] = await Promise.all([
      getAttemptDetail(attemptId),
      getAttemptReport(attemptId)
    ])
    detailData.value = detail
    reportData.value = report
  } catch {
    detailData.value = null
    reportData.value = null
    detailError.value = true
    toast.error('Không thể tải chi tiết bài thi.')
  } finally {
    detailLoading.value = false
  }
}

const loadAttempts = async () => {
  isLoading.value = true
  try { attempts.value = await listMyAttempts() }
  catch { attempts.value = []; toast.error('Không thể tải lịch sử học tập.') }
  finally { isLoading.value = false }
}

onMounted(loadAttempts)
</script>

<style scoped>
.result-expand-enter-active,
.result-expand-leave-active {
  transition: opacity 0.25s ease, transform 0.25s ease;
}
.result-expand-enter-from,
.result-expand-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}
</style>
