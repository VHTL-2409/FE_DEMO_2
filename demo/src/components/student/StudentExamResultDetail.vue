<template>
  <div :class="isDark ? 'dark' : 'light'" class="portal-viewport flex h-full min-h-0 flex-col bg-background-light font-display text-slate-900 antialiased dark:bg-background-dark dark:text-slate-100">
    <div class="relative flex h-full min-h-0 flex-1 w-full flex-col overflow-x-hidden">
      <div class="layout-container flex h-full min-h-0 flex-1 grow flex-col">
        <StudentTopHeader class="shrink-0" />

        <main class="teacher-page-shell relative flex min-h-0 flex-1 flex-col overflow-hidden">
          <div class="portal-scrollbar relative flex min-h-0 flex-1 flex-col overflow-y-auto">
          <div class="relative flex w-full flex-1 flex-col px-4 pb-6 md:px-6 md:pb-8">
            <div class="mb-5 flex animate-fade-up flex-col justify-between gap-3 md:mb-6 md:flex-row md:items-end md:gap-4">
              <div class="flex min-w-0 flex-1 flex-col gap-2">
                <div
                  class="inline-flex w-fit items-center gap-2 rounded-full bg-green-100 px-3 py-1 text-xs font-bold text-green-700 dark:bg-green-900/30 dark:text-green-400"
                >
                  <span class="size-2 rounded-full bg-green-500"></span>
                  ĐÃ HOÀN THÀNH BÀI THI
                </div>
                <PageHeader class="!mb-0" eyebrow="Kết quả" :title="examTitle" :subtitle="attemptedAt" />
              </div>
            </div>

            <div v-if="isLoading" class="mb-10 animate-fade-up-delay space-y-4" aria-busy="true">
              <div class="grid gap-4 [grid-template-columns:repeat(auto-fit,minmax(220px,1fr))]">
                <SkeletonLoader variant="card" />
                <SkeletonLoader variant="card" />
                <SkeletonLoader variant="card" />
              </div>
              <SkeletonLoader variant="card" />
            </div>

            <div v-else class="mb-6 grid animate-fade-up-delay gap-4 [grid-template-columns:repeat(auto-fit,minmax(220px,1fr))] md:mb-8">
              <BaseCard hoverable class="flex flex-col gap-2 !shadow-sm">
                <p class="text-slate-500 dark:text-slate-400 text-sm font-medium uppercase tracking-wider">Tổng điểm</p>
                <div class="flex items-baseline gap-2">
                  <p class="text-slate-900 dark:text-slate-100 text-3xl font-bold">{{ scoreTen }} / 10</p>
                </div>
                <div class="w-full bg-slate-100 dark:bg-slate-800 h-1.5 rounded-full mt-2 overflow-hidden">
                  <div class="bg-primary h-full rounded-full" :style="{ width: `${scorePercent}%` }"></div>
                </div>
              </BaseCard>

              <BaseCard hoverable class="flex flex-col gap-2 !shadow-sm">
                <p class="text-slate-500 dark:text-slate-400 text-sm font-medium uppercase tracking-wider">Thời gian làm bài</p>
                <p class="text-slate-900 dark:text-slate-100 text-3xl font-bold">{{ timeTaken }}</p>
              </BaseCard>

              <BaseCard hoverable class="flex flex-col gap-2 !shadow-sm">
                <p class="text-slate-500 dark:text-slate-400 text-sm font-medium uppercase tracking-wider">Thứ tự xếp hạng</p>
                <p class="text-slate-900 dark:text-slate-100 text-3xl font-bold">{{ rankOrder }}</p>
              </BaseCard>
            </div>

            <div v-if="!isLoading" class="mb-6 animate-fade-up-delay md:mb-8">
              <h2 class="teacher-section-title mb-4 text-slate-900 dark:text-slate-100">Phân tích kết quả</h2>
              <div class="grid grid-cols-1 gap-4 md:gap-5">
                <BaseCard padding="lg" class="!shadow-sm">
                  <p class="text-slate-900 dark:text-slate-100 text-base font-semibold mb-6">Phân bố câu trả lời</p>
                  <div class="grid h-48 grid-cols-3 items-end gap-4 px-4 md:gap-6">
                    <div class="flex flex-col items-center gap-3 h-full justify-end">
                      <div class="bg-green-500/20 border-t-4 border-green-500 w-full rounded-t-lg" :style="{ height: `${Math.max(8, correctRatio)}%` }"></div>
                      <p class="text-slate-600 dark:text-slate-400 text-xs font-bold uppercase">Đúng</p>
                      <span class="text-green-600 font-bold">{{ correctCount }}</span>
                    </div>
                    <div class="flex flex-col items-center gap-3 h-full justify-end">
                      <div class="bg-red-500/20 border-t-4 border-red-500 w-full rounded-t-lg" :style="{ height: `${Math.max(8, incorrectRatio)}%` }"></div>
                      <p class="text-slate-600 dark:text-slate-400 text-xs font-bold uppercase">Sai</p>
                      <span class="text-red-600 font-bold">{{ incorrectCount }}</span>
                    </div>
                    <div class="flex flex-col items-center gap-3 h-full justify-end">
                      <div class="bg-slate-300 dark:bg-slate-700 border-t-4 border-slate-400 w-full rounded-t-lg" :style="{ height: `${Math.max(8, skippedRatio)}%` }"></div>
                      <p class="text-slate-600 dark:text-slate-400 text-xs font-bold uppercase">Bỏ qua</p>
                      <span class="text-slate-500 font-bold">{{ skippedCount }}</span>
                    </div>
                  </div>
                </BaseCard>
              </div>
            </div>

            <div v-if="!isLoading" class="mb-6 animate-fade-up-delay md:mb-8">
              <div class="mb-4 flex items-center justify-between md:mb-5">
                <h2 class="teacher-section-title text-slate-900 dark:text-slate-100">Xem lại câu hỏi</h2>
                <div class="flex gap-2">
                  <span class="flex items-center gap-1 text-xs font-semibold px-2 py-1 bg-green-100 dark:bg-green-900/30 text-green-700 dark:text-green-400 rounded">
                    <span class="material-symbols-outlined text-sm">check_circle</span> Đúng
                  </span>
                  <span class="flex items-center gap-1 text-xs font-semibold px-2 py-1 bg-red-100 dark:bg-red-900/30 text-red-700 dark:text-red-400 rounded">
                    <span class="material-symbols-outlined text-sm">cancel</span> Sai
                  </span>
                </div>
              </div>

              <EmptyState
                v-if="!reviewAnswers.length"
                icon="quiz"
                title="Không có dữ liệu câu hỏi"
                description="Báo cáo chưa có chi tiết từng câu hoặc dữ liệu chưa được tải đầy đủ."
                dense
                fill
              />

              <div v-else class="flex flex-col gap-4">
                <div
                  v-for="item in reviewAnswers"
                  :key="item.questionId"
                  class="bg-white dark:bg-slate-900 rounded-xl shadow-sm border-y border-r border-slate-200 dark:border-slate-800 overflow-hidden"
                  :class="item.correct ? 'border-l-4 border-green-500' : 'border-l-4 border-red-500'"
                >
                  <div class="p-6">
                    <div class="flex justify-between items-start mb-4">
                      <span class="text-xs font-bold text-slate-500 dark:text-slate-400 uppercase tracking-widest">Câu {{ item.index }}</span>
                      <span :class="item.correct ? 'text-green-600 dark:text-green-400' : 'text-red-600 dark:text-red-400'" class="font-bold text-sm">
                        {{ item.correct ? 'Đúng' : 'Sai' }}
                      </span>
                    </div>
                    <p class="text-slate-900 dark:text-slate-100 text-lg font-medium mb-6">{{ item.question }}</p>

                    <div v-if="item.options.length" class="grid grid-cols-1 md:grid-cols-2 gap-3">
                      <div
                        v-for="option in item.options"
                        :key="`${item.questionId}-${option.id}`"
                        class="flex items-center justify-between p-4 rounded-lg border"
                        :class="[
                          isCorrectOption(item, option)
                            ? 'bg-green-50 dark:bg-green-900/10 border-green-200 dark:border-green-800/50'
                            : 'bg-slate-50 dark:bg-slate-800/50 border-slate-100 dark:border-slate-800',
                          isSelectedOption(item, option) && !isCorrectOption(item, option)
                            ? 'bg-red-50 dark:bg-red-900/10 border-red-200 dark:border-red-800/50'
                            : ''
                        ]"
                      >
                        <div class="flex items-center gap-3">
                          <span
                            class="w-6 h-6 flex items-center justify-center rounded-full text-xs font-bold"
                            :class="isCorrectOption(item, option)
                              ? 'bg-green-500 text-white'
                              : isSelectedOption(item, option) && !isCorrectOption(item, option)
                                ? 'bg-red-500 text-white'
                                : 'bg-slate-200 dark:bg-slate-700 text-slate-600 dark:text-slate-400'"
                          >
                            {{ option.id }}
                          </span>
                          <span class="text-slate-700 dark:text-slate-300">{{ option.text }}</span>
                        </div>

                        <div class="flex items-center gap-2">
                          <span
                            v-if="isSelectedOption(item, option)"
                            class="inline-flex items-center gap-1 text-[10px] font-bold uppercase"
                            :class="isCorrectOption(item, option) ? 'text-green-600 dark:text-green-400' : 'text-red-600 dark:text-red-400'"
                          >
                            <span class="material-symbols-outlined text-sm">{{ isCorrectOption(item, option) ? 'check_circle' : 'cancel' }}</span>
                            {{ isCorrectOption(item, option) ? 'Đúng' : 'Sai' }}
                          </span>
                          <span
                            v-if="isCorrectOption(item, option) && !isSelectedOption(item, option)"
                            class="inline-flex items-center gap-1 text-[10px] font-bold uppercase text-green-600 dark:text-green-400"
                          >
                            <span class="material-symbols-outlined text-sm">check_circle</span>
                            Đúng
                          </span>
                        </div>
                      </div>
                    </div>

                    <div v-else class="rounded-lg border border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-800/50 px-4 py-3">
                      <p class="text-xs uppercase font-bold text-slate-500 dark:text-slate-400 mb-1">Câu trả lời đã chọn</p>
                      <p class="text-sm font-medium text-slate-800 dark:text-slate-200">{{ item.selectedAnswerLabel }}</p>
                      <p class="mt-2 text-xs text-slate-500">Đúng: {{ item.correctAnswerLabel || '-' }}</p>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <BaseCard v-if="!isLoading" class="mb-6 flex flex-col items-center justify-center gap-4 border-primary/20 bg-primary/5 text-center md:mb-8">
              <p class="text-slate-600 dark:text-slate-400 font-medium">Cần trao đổi kết quả này với giảng viên của bạn?</p>
              <BaseButton size="lg" @click="openTeacherContact">
                <span class="material-symbols-outlined text-xl">mail</span>
                Liên hệ giảng viên
              </BaseButton>
            </BaseCard>
          </div>
          </div>
        </main>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useNotifications } from '../../composables/useNotifications'
import { getAttemptDetail, getAttemptReport } from '../../services/attemptService'
import { useRoute } from 'vue-router'
import { useToast } from '../../composables/useToast'
import StudentTopHeader from './StudentTopHeader.vue'
import BaseCard from '../shared/BaseCard.vue'
import BaseButton from '../shared/BaseButton.vue'
import PageHeader from '../shared/PageHeader.vue'
import SkeletonLoader from '../shared/SkeletonLoader.vue'
import EmptyState from '../shared/EmptyState.vue'

const route = useRoute()
const { openTeacherContact } = useNotifications()
const isDark = ref(false)
const detail = ref(null)
const report = ref(null)
const isLoading = ref(false)
const toast = useToast()

const attemptId = computed(() => Number.parseInt(String(route.query.attemptId || ''), 10) || null)
const examTitle = computed(() => detail.value?.examTitle || route.query.examTitle || route.query.exam || 'Kết quả bài thi')
const attemptedAt = computed(() => {
  const submittedAt = detail.value?.submittedAt
  if (!submittedAt) return route.query.attempted || '-'
  return `Đã làm lúc ${new Date(submittedAt).toLocaleString()}`
})
const scorePercent = computed(() => Math.round(Number(report.value?.score ?? route.query.score ?? 0)))
const scoreTen = computed(() => (scorePercent.value / 10).toFixed(1))
const timeTaken = computed(() => {
  const startedAt = detail.value?.startedAt
  const submittedAt = detail.value?.submittedAt
  if (!startedAt || !submittedAt) return route.query.time || '-'
  const diffMinutes = Math.max(1, Math.round((new Date(submittedAt).getTime() - new Date(startedAt).getTime()) / 60000))
  return `${diffMinutes}m`
})
const rankOrder = computed(() => {
  const raw = route.query.rank ?? route.query.rankOrder ?? route.query.percentile
  if (raw === undefined || raw === null || raw === '') return '-'
  const parsed = Number.parseInt(String(raw), 10)
  if (Number.isNaN(parsed)) return String(raw)
  return `#${parsed}`
})
const correctCount = computed(() => Number(report.value?.correctCount || 0))
const incorrectCount = computed(() => Math.max(Number(report.value?.answeredCount || 0) - correctCount.value, 0))
const skippedCount = computed(() => Math.max(Number(detail.value?.totalQuestions || 0) - Number(report.value?.answeredCount || 0), 0))

const answerTotal = computed(() => {
  const total = correctCount.value + incorrectCount.value + skippedCount.value
  return total || 1
})
const correctRatio = computed(() => Math.round((correctCount.value / answerTotal.value) * 100))
const incorrectRatio = computed(() => Math.round((incorrectCount.value / answerTotal.value) * 100))
const skippedRatio = computed(() => Math.round((skippedCount.value / answerTotal.value) * 100))

const normalizeOptionId = (value) => {
  const normalized = String(value || '').trim().toUpperCase()
  if (normalized === '0') return 'A'
  if (normalized === '1') return 'B'
  if (normalized === '2') return 'C'
  if (normalized === '3') return 'D'
  return normalized
}

const parseOptions = (optionsJson) => {
  if (!optionsJson) return []

  const normalizeOptionItem = (rawId, rawText, fallbackIndex) => {
    const normalizedId = normalizeOptionId(rawId || String.fromCharCode(65 + fallbackIndex))
    return {
      id: normalizedId,
      normalizedId,
      text: String(rawText || '').trim()
    }
  }

  try {
    const parsed = JSON.parse(optionsJson)

    if (Array.isArray(parsed)) {
      return parsed
        .map((option, index) => {
          if (typeof option === 'string') {
            return normalizeOptionItem('', option, index)
          }
          return normalizeOptionItem(option?.id, option?.text || option?.content || option?.label, index)
        })
        .filter((option) => option.id && option.text)
    }

    if (parsed && typeof parsed === 'object') {
      return Object.entries(parsed)
        .map(([key, value], index) => normalizeOptionItem(key, value, index))
        .filter((option) => option.id && option.text)
    }
  } catch {
    const text = String(optionsJson)
    const matches = [...text.matchAll(/([A-D0-3])[\.:\)-]\s*([^\n\|;]+)/gi)]
    if (matches.length) {
      return matches.map((match, index) => normalizeOptionItem(match[1], match[2], index))
    }
  }

  return []
}

const parseAnswerValue = (value) => {
  if (value == null || value === '') return null
  if (typeof value !== 'string') return value
  try {
    return JSON.parse(value)
  } catch {
    return value
  }
}

const normalizeAnswerIds = (value) => {
  const parsed = parseAnswerValue(value)
  if (Array.isArray(parsed)) {
    return parsed.map((item) => normalizeOptionId(item)).filter(Boolean)
  }
  if (parsed && typeof parsed === 'object') {
    return Object.values(parsed).map((item) => normalizeOptionId(item)).filter(Boolean)
  }
  if (typeof parsed === 'string') {
    return [normalizeOptionId(parsed)].filter(Boolean)
  }
  return []
}

const formatAnswerValue = (value) => {
  const parsed = parseAnswerValue(value)
  if (Array.isArray(parsed)) return parsed.join(', ')
  if (parsed && typeof parsed === 'object') {
    return Object.entries(parsed).map(([left, right]) => `${left} -> ${right}`).join('; ')
  }
  return String(parsed || 'Không trả lời')
}

const isSelectedOption = (item, option) => item.selectedIds.includes(option.normalizedId)
const isCorrectOption = (item, option) => item.correctIds.includes(option.normalizedId)

const reviewAnswers = computed(() => (report.value?.answers || []).map((item, index) => {
  const options = parseOptions(item.options)
  const selectedIds = normalizeAnswerIds(item.selectedAnswer)
  const correctIds = normalizeAnswerIds(item.correctAnswer)

  return {
    questionId: item.questionId,
    question: item.question || `Câu hỏi #${item.questionId}`,
    questionType: item.questionType || 'SINGLE_CHOICE',
    selectedAnswer: item.selectedAnswer || 'Không trả lời',
    correctAnswer: item.correctAnswer || '',
    selectedAnswerLabel: formatAnswerValue(item.selectedAnswer),
    correctAnswerLabel: formatAnswerValue(item.correctAnswer),
    options,
    selectedIds,
    correctIds,
    correct: Boolean(item.correct),
    scoreWeight: Number(item.scoreWeight || 0),
    index: index + 1
  }
}))

onMounted(async () => {
  if (!attemptId.value) {
    toast.error('Thiếu mã bài làm. Vui lòng mở kết quả từ lịch sử bài thi.')
    return
  }

  isLoading.value = true
  try {
    const [detailPayload, reportPayload] = await Promise.all([
      getAttemptDetail(attemptId.value),
      getAttemptReport(attemptId.value)
    ])
    detail.value = detailPayload
    report.value = reportPayload
  } catch (error) {
    toast.error('Không thể tải báo cáo lượt làm bài.')
  } finally {
    isLoading.value = false
  }
})
</script>

<style scoped>
.font-display {
  font-family: 'Inter', sans-serif;
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

@keyframes floatSlow {
  0%,
  100% {
    transform: translate3d(0, 0, 0);
  }
  50% {
    transform: translate3d(0, -14px, 0);
  }
}

@keyframes floatDelay {
  0%,
  100% {
    transform: translate3d(0, 0, 0);
  }
  50% {
    transform: translate3d(0, 12px, 0);
  }
}

.animate-fade-up {
  animation: fadeUp 0.5s ease-out;
}

.animate-fade-up-delay {
  animation: fadeUp 0.65s ease-out;
}

.animate-float-slow {
  animation: floatSlow 7s ease-in-out infinite;
}

.animate-float-delay {
  animation: floatDelay 8s ease-in-out infinite;
}
</style>
