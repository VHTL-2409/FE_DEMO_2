<template>
  <div class="bg-[var(--ds-bg)] min-h-full">
    <div class="mx-auto max-w-5xl px-4 pb-10 pt-4 sm:px-6 lg:px-8">

      <!-- Breadcrumb + header -->
      <div class="mb-5 ds-animate-fade-up">
        <div class="mb-2 flex items-center gap-2 text-sm text-[var(--ds-text-muted)]">
          <RouterLink to="/student/study-history" class="flex items-center gap-1 hover:text-[var(--ds-primary)] transition-colors">
            <LucideIcon name="history" size="16" />
            Lịch sử
          </RouterLink>
          <LucideIcon name="chevron_right" size="12" />
          <span class="text-[var(--ds-text)] font-medium">Chi tiết kết quả</span>
        </div>
        <div v-if="!isLoading && detail" class="flex items-center gap-2 rounded-full px-3 py-1 text-xs font-bold" style="background-color: var(--ds-success-bg); color: var(--ds-success);">
          <span class="size-2 rounded-full animate-pulse" style="background-color: var(--ds-success);"></span>
          Đã hoàn thành bài thi
        </div>
        <div v-else-if="isLoading" class="flex items-center gap-2 rounded-full px-3 py-1 text-xs font-bold bg-gray-100 dark:bg-gray-800">
          <span class="size-2 rounded-full bg-gray-400 animate-pulse"></span>
          <span class="text-gray-500">Đang tải...</span>
        </div>
        <PageHeader
          class="!mb-0 mt-3"
          eyebrow="Kết quả"
          :title="examTitle"
          :subtitle="attemptedAt"
        />
      </div>

      <!-- Loading skeleton -->
      <div v-if="isLoading" class="ds-animate-fade-up" style="animation-delay: 0.1s">
        <div class="rounded-[var(--ds-radius-xl)] border border-[var(--ds-border)] bg-[var(--ds-surface)] p-4 mb-3">
          <div class="grid grid-cols-1 md:grid-cols-3 gap-3">
            <div class="p-4 rounded-xl border border-[var(--ds-border)] bg-[var(--ds-gray-50)] flex flex-col items-center gap-3">
              <div class="h-3 w-16 rounded bg-[var(--ds-gray-200)] animate-pulse"></div>
              <div class="size-12 rounded-full border-4 border-[var(--ds-gray-200)] animate-pulse"></div>
              <div class="h-2 w-20 rounded bg-[var(--ds-gray-200)] animate-pulse"></div>
            </div>
            <div class="p-4 rounded-xl border border-[var(--ds-border)] bg-[var(--ds-gray-50)]">
              <div class="h-3 w-14 rounded bg-[var(--ds-gray-200)] animate-pulse mb-2"></div>
              <div class="h-5 w-16 rounded bg-[var(--ds-gray-200)] animate-pulse"></div>
            </div>
            <div class="p-4 rounded-xl border border-[var(--ds-border)] bg-[var(--ds-gray-50)]">
              <div class="h-3 w-10 rounded bg-[var(--ds-gray-200)] animate-pulse mb-2"></div>
              <div class="h-5 w-14 rounded bg-[var(--ds-gray-200)] animate-pulse"></div>
            </div>
          </div>
        </div>
        <div class="rounded-[var(--ds-radius-xl)] border border-[var(--ds-border)] bg-[var(--ds-surface)] p-4 mb-3">
          <div class="flex items-center gap-2 mb-3">
            <div class="h-4 w-4 rounded bg-[var(--ds-gray-200)] animate-pulse"></div>
            <div class="h-4 w-28 rounded bg-[var(--ds-gray-200)] animate-pulse"></div>
          </div>
          <div v-for="i in 3" :key="i" class="p-3 rounded-xl border border-[var(--ds-border)] mb-2 last:mb-0">
            <div class="flex justify-between items-center mb-2">
              <div class="h-3 w-14 rounded bg-[var(--ds-gray-200)] animate-pulse"></div>
              <div class="h-3 w-10 rounded bg-[var(--ds-gray-200)] animate-pulse"></div>
            </div>
            <div class="h-4 w-full rounded bg-[var(--ds-gray-200)] animate-pulse mb-2"></div>
            <div class="space-y-1.5">
              <div v-for="j in 4" :key="j" class="h-8 rounded-lg border border-[var(--ds-border)] bg-[var(--ds-gray-50)] animate-pulse"></div>
            </div>
          </div>
        </div>
      </div>

      <!-- Detail panel with transition -->
      <Transition name="fade-slide" mode="out-in">
        <div v-if="!isLoading" :key="attemptId" class="ds-animate-fade-up">
          <ResultDetailPanel
            :loading="isLoading"
            :score="scorePercent"
            :time-taken="timeTaken"
            :rank-order="rankOrder"
            :correct-count="correctCount"
            :incorrect-count="incorrectCount"
            :skipped-count="skippedCount"
            :total-questions="totalQuestions"
            :review-answers="reviewAnswers"
          />
        </div>
      </Transition>

    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { getAttemptDetail, getAttemptReport } from '../../services/attemptService'
import { useRoute } from 'vue-router'
import { useToast } from '../../composables/useToast'
import {
  answerIdsMatch,
  normalizeAnswerIds,
  parseResultOptions,
  scorePercentValue
} from '../../utils/attemptResult'
import PageHeader from '../ui/PageHeader.vue'
import ResultDetailPanel from './results/ResultDetailPanel.vue'

const route = useRoute()
const toast = useToast()
const detail = ref(null)
const report = ref(null)
const isLoading = ref(false)

const attemptId = computed(() => Number.parseInt(String(route.query.attemptId || ''), 10) || null)
const examTitle = computed(() => detail.value?.examTitle || route.query.examTitle || route.query.exam || 'Kết quả bài thi')
const attemptedAt = computed(() => {
  const ts = detail.value?.submittedAt
  return ts ? new Date(ts).toLocaleString('vi-VN') : '-'
})
const scorePercent = computed(() => scorePercentValue(report.value?.score ?? route.query.score ?? 0))
const timeTaken = computed(() => {
  if (!detail.value?.startedAt || !detail.value?.submittedAt) return '-'
  const ms = new Date(detail.value.submittedAt).getTime() - new Date(detail.value.startedAt).getTime()
  const minutes = Math.floor(ms / 60000)
  return `${minutes} phút`
})
const rankOrder = computed(() => {
  const rank = route.query.rankOrder
  return rank ? String(rank) : ''
})
const totalQuestions = computed(() => Number(
  detail.value?.totalQuestions ??
  detail.value?.questionCount ??
  report.value?.totalQuestions ??
  0
))
const correctCount = computed(() => Number(report.value?.correctCount || 0))
const incorrectCount = computed(() => Math.max(Number(report.value?.answeredCount || 0) - correctCount.value, 0))
const skippedCount = computed(() => Math.max(totalQuestions.value - Number(report.value?.answeredCount || 0), 0))

const isSelectedOption = (item, option) => {
  const selected = normalizeAnswerIds(
    item.selectedAnswer || item.selectedIds || item.selectedIds_
  )
  return selected.includes(String(option.id || '').toUpperCase())
}

const isCorrectOption = (item, option) => {
  const correct = normalizeAnswerIds(
    item.correctAnswer || item.correctIds || item.correctIds_
  )
  return correct.includes(String(option.id || '').toUpperCase())
}

const reviewAnswers = computed(() => (report.value?.answers || []).map((item, index) => {
  const question = typeof item.question === 'string'
    ? item.question.replace(/^[A-D]\.\s*/, '')
    : (item.question?.content || item.question?.text || 'Câu hỏi không có nội dung')
  
  // Handle options from various sources
  let options = []
  if (item.options) {
    options = parseResultOptions(item.options)
  } else if (item.questionOptions) {
    options = parseResultOptions(item.questionOptions)
  } else if (item.question?.options) {
    options = parseResultOptions(item.question.options)
  }
  
  // Handle answer values - API returns selectedAnswer/correctAnswer
  const selectedAnswer = item.selectedAnswer || item.selectedIds || item.selectedIds_ || ''
  const correctAnswer = item.correctAnswer || item.correctIds || item.correctIds_ || ''
  
  // Parse answers for comparison
  const selectedIds = normalizeAnswerIds(selectedAnswer)
  const correctIds = normalizeAnswerIds(correctAnswer)
  const isCorrect = selectedIds.length > 0 && answerIdsMatch(selectedIds, correctIds)
  
  return { 
    questionId: item.questionId || index, 
    index: index + 1, 
    question, 
    options, 
    selectedAnswer, 
    correctAnswer,
    selectedIds,
    correctIds,
    correct: isCorrect 
  }
}))

onMounted(async () => {
  // Force scroll to top when component mounts
  window.scrollTo({ top: 0, behavior: 'instant' })

  if (!attemptId.value) return
  isLoading.value = true
  try {
    const [detailPayload, reportPayload] = await Promise.all([
      getAttemptDetail(attemptId.value),
      getAttemptReport(attemptId.value)
    ])
    detail.value = detailPayload
    report.value = reportPayload
  } catch {
    toast.error('Không thể tải kết quả bài thi.')
  } finally {
    isLoading.value = false
  }
})
</script>

<style scoped>
/* Fade slide transition */
.fade-slide-enter-active,
.fade-slide-leave-active {
  transition: opacity 0.3s ease, transform 0.3s ease;
}

.fade-slide-enter-from {
  opacity: 0;
  transform: translateY(12px);
}

.fade-slide-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}

/* Smooth skeleton shimmer */
@keyframes shimmer {
  0% { background-position: -200% 0; }
  100% { background-position: 200% 0; }
}

@keyframes gpu-shimmer {
  0%   { opacity: 0.5; transform: scaleX(0.3); }
  50%  { opacity: 1;   transform: scaleX(1); }
  100% { opacity: 0.5; transform: scaleX(0.3); }
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

@media (prefers-reduced-motion: reduce) {
  .animate-pulse {
    animation: none;
  }

  .fade-slide-enter-active,
  .fade-slide-leave-active {
    transition: none;
  }
}
</style>
