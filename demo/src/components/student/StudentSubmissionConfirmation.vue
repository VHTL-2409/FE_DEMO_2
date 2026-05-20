<template>
  <div class="bg-[var(--ds-bg)] min-h-full">
    <div class="mx-auto flex min-h-0 flex-1 flex-col items-center justify-center px-4 pb-10 pt-4 sm:px-6 lg:px-8">
      <div class="w-full max-w-lg ds-animate-fade-up">

        <!-- Success Icon -->
        <div class="mb-5 flex justify-center">
          <div class="relative flex h-20 w-20 items-center justify-center rounded-full shadow-[var(--ds-shadow-xl)]" style="background-color: var(--ds-primary); color: white;">
            <LucideIcon name="check_circle" size="40" />
          </div>
        </div>

        <!-- Title -->
        <div class="mb-5 text-center">
          <h1 class="text-2xl font-extrabold text-[var(--ds-text)]">Nộp bài thành công</h1>
          <p class="mt-1.5 text-sm text-[var(--ds-text-secondary)]">Bài làm đã được ghi nhận. Xem tóm tắt bên dưới.</p>
        </div>

        <!-- Summary Card -->
        <div class="rounded-[var(--ds-radius-2xl)] border border-[var(--ds-border)] bg-[var(--ds-surface)] p-5 shadow-[var(--ds-shadow-md)] mb-5">
          <!-- 2-col stat row -->
          <div class="grid grid-cols-2 gap-3">
            <div class="flex items-center gap-3 rounded-xl border border-[var(--ds-border)] p-3">
              <div class="flex size-9 shrink-0 items-center justify-center rounded-[var(--ds-radius-lg)]" style="background-color: var(--ds-primary-soft); color: var(--ds-primary);">
                <LucideIcon name="schedule" size="17" />
              </div>
              <div class="min-w-0">
                <p class="text-xs text-[var(--ds-text-muted)]">Thời gian làm</p>
                <p class="font-semibold text-[var(--ds-text)] text-sm truncate mt-0.5">{{ timeTakenDisplay }}</p>
              </div>
            </div>
            <div class="flex items-center gap-3 rounded-xl border border-[var(--ds-border)] p-3">
              <div class="flex size-9 shrink-0 items-center justify-center rounded-[var(--ds-radius-lg)]" style="background-color: var(--ds-primary-soft); color: var(--ds-primary);">
                <LucideIcon name="checklist" size="17" />
              </div>
              <div class="min-w-0">
                <p class="text-xs text-[var(--ds-text-muted)]">Câu đã làm</p>
                <p class="font-semibold text-[var(--ds-text)] text-sm mt-0.5">{{ answeredSummary }}</p>
              </div>
            </div>
          </div>

          <!-- Meta row -->
          <div class="mt-3 flex flex-wrap items-center gap-x-4 gap-y-1 border-t border-[var(--ds-border)] pt-3 text-xs text-[var(--ds-text-muted)]">
            <span>
              <LucideIcon name="calendar_today" size="11" class="inline align-[-0.5px]" />
              {{ submittedAtDisplay }}
            </span>
            <span v-if="attemptId" class="font-mono">
              <LucideIcon name="tag" size="11" class="inline align-[-0.5px]" />
              #AT-{{ attemptId }}
            </span>
          </div>
        </div>

        <!-- Action Buttons -->
        <div class="flex flex-col justify-center gap-2.5 sm:flex-row">
          <button
            v-if="attemptId && allowReviewAfterSubmit"
            type="button"
            class="inline-flex items-center justify-center gap-2 rounded-[var(--ds-radius-xl)] px-6 py-3 text-sm font-bold text-white shadow-[var(--ds-shadow-md)] transition-all hover:-translate-y-0.5 hover:shadow-[var(--ds-shadow-lg)]"
            style="background-color: var(--ds-primary);"
            @click="goToResult"
          >
            <LucideIcon name="grade" size="16" />
            Xem kết quả
          </button>
          <button
            type="button"
            class="inline-flex items-center justify-center gap-2 rounded-[var(--ds-radius-xl)] border border-[var(--ds-border)] px-6 py-3 text-sm font-semibold text-[var(--ds-text-secondary)] shadow-[var(--ds-shadow-sm)] transition-all hover:-translate-y-0.5 hover:bg-[var(--ds-gray-50)] hover:shadow-[var(--ds-shadow-md)]"
            @click="goToDashboard"
          >
            <LucideIcon name="home" size="16" />
            Về trang chủ
          </button>
        </div>

      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { getAttemptDetail, getAttemptReport } from '../../services/attemptService'
import { useToast } from '../../composables/useToast'
import { useRoute, useRouter } from 'vue-router'
import { buildResultQuery } from '../../services/studentExamContextStorage'

const route = useRoute()
const router = useRouter()
const detail = ref(null)
const report = ref(null)
const toast = useToast()

const attemptId = computed(() => Number.parseInt(String(route.query.attemptId || ''), 10) || null)
const allowReviewAfterSubmit = computed(() => detail.value?.allowReviewAfterSubmit !== false && route.query.allowReviewAfterSubmit !== 'false')
const examTitle = computed(() => detail.value?.examTitle || route.query.exam || 'Bài thi')

const submittedAtDisplay = computed(() => {
  const submittedAt = detail.value?.submittedAt || route.query.submittedAt
  if (!submittedAt) return new Date().toLocaleString()
  const date = new Date(String(submittedAt))
  return Number.isNaN(date.getTime()) ? String(submittedAt) : date.toLocaleString()
})

const timeTakenDisplay = computed(() => {
  const startedAt = detail.value?.startedAt
  const submittedAt = detail.value?.submittedAt
  if (!startedAt || !submittedAt) return '-'
  const diffSeconds = Math.max(0, Math.floor((new Date(submittedAt).getTime() - new Date(startedAt).getTime()) / 1000))
  const hours = Math.floor(diffSeconds / 3600)
  const minutes = Math.floor((diffSeconds % 3600) / 60)
  const seconds = diffSeconds % 60
  return `${String(hours).padStart(2, '0')}:${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`
})

const answeredSummary = computed(() => {
  const answeredCount = Number(
    report.value?.totalAnswered ??
    report.value?.answeredCount ??
    report.value?.answers?.length ??
    detail.value?.totalAnswered ??
    detail.value?.answeredCount ??
    0
  )
  const totalQuestions = Number(
    detail.value?.totalQuestions ??
    detail.value?.questionCount ??
    0
  )
  if (!totalQuestions) return `${answeredCount}`
  return `${answeredCount} / ${totalQuestions}`
})

const goToDashboard = () => router.push('/student/dashboard')
const goToResult = () => {
  if (!attemptId.value) {
    goToDashboard()
    return
  }
  router.push({
    path: '/student/exam-result',
    query: buildResultQuery({
      attemptId: attemptId.value,
      examTitle: examTitle.value
    })
  })
}

onMounted(async () => {
  if (!attemptId.value) return
  try {
    const detailPayload = await getAttemptDetail(attemptId.value)
    detail.value = detailPayload
    if (detailPayload?.allowReviewAfterSubmit !== false) {
      report.value = await getAttemptReport(attemptId.value)
    }
  } catch {
    toast.error('Không thể tải tóm tắt bài nộp.')
  }
})
</script>

<style scoped>
</style>
