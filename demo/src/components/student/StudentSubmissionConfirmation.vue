<template>
  <div class="bg-[var(--ds-bg)] min-h-full">
    <div class="mx-auto flex min-h-0 flex-1 flex-col items-center justify-center px-4 pb-10 pt-4 sm:px-6 lg:px-8">
      <div class="w-full max-w-2xl ds-animate-fade-up">

        <!-- Success Icon with animation -->
        <div class="mb-6 flex justify-center">
          <div class="relative flex h-24 w-24 items-center justify-center rounded-full shadow-[var(--ds-shadow-xl)]" style="background-color: var(--ds-primary); color: white;">
            <svg class="checkmark" viewBox="0 0 52 52">
              <circle class="checkmark-circle" cx="26" cy="26" r="25" fill="none"/>
              <path class="checkmark-check" fill="none" d="M14.1 27.2l7.1 7.2 16.7-16.8"/>
            </svg>
            <LucideIcon name="check_circle" class="absolute opacity-0 check-icon" />
          </div>
        </div>

        <!-- Title -->
        <div class="mb-6 text-center">
          <p class="mb-2 text-xs font-bold uppercase tracking-widest" style="color: var(--ds-primary);">Nộp bài</p>
          <h1 class="text-2xl font-extrabold text-[var(--ds-text)] sm:text-3xl">Nộp bài thành công</h1>
          <p class="mt-2 text-sm text-[var(--ds-text-secondary)]">Bài làm đã được ghi nhận. Xem tóm tắt bên dưới.</p>
        </div>

        <!-- Summary Card -->
        <div class="rounded-[var(--ds-radius-2xl)] border border-[var(--ds-border)] bg-[var(--ds-surface)] p-6 shadow-[var(--ds-shadow-md)] md:p-8 ds-animate-fade-up relative overflow-hidden" style="animation-delay: 0.08s">
          <!-- Decorative corner -->
          <div class="absolute top-0 right-0 w-24 h-24 bg-gradient-to-bl from-[var(--ds-primary-soft)] to-transparent rounded-bl-full"></div>
          
          <h3 class="mb-5 text-sm font-bold uppercase tracking-wider" style="color: var(--ds-primary);">Tóm tắt bài nộp</h3>
          <div class="grid grid-cols-1 gap-4 md:grid-cols-2 md:gap-5 relative">
            <div class="flex items-start gap-4 p-4 rounded-xl border border-[var(--ds-border)] hover:border-[var(--ds-primary)]/30 transition-colors">
              <div class="flex size-11 shrink-0 items-center justify-center rounded-[var(--ds-radius-lg)]" style="background-color: var(--ds-primary-soft); color: var(--ds-primary);">
                <LucideIcon name="calendar_today" size="20" />
              </div>
              <div>
                <p class="text-xs text-[var(--ds-text-muted)]">Ngày &amp; giờ</p>
                <p class="font-semibold text-[var(--ds-text)] mt-0.5">{{ submittedAtDisplay }}</p>
              </div>
            </div>
            <div class="flex items-start gap-4 p-4 rounded-xl border border-[var(--ds-border)] hover:border-[var(--ds-primary)]/30 transition-colors">
              <div class="flex size-11 shrink-0 items-center justify-center rounded-[var(--ds-radius-lg)]" style="background-color: var(--ds-primary-soft); color: var(--ds-primary);">
                <LucideIcon name="timer" size="20" />
              </div>
              <div>
                <p class="text-xs text-[var(--ds-text-muted)]">Thời gian làm bài</p>
                <p class="font-semibold text-[var(--ds-text)] mt-0.5">{{ timeTakenDisplay }}</p>
              </div>
            </div>
            <div class="flex items-start gap-4 p-4 rounded-xl border border-[var(--ds-border)] hover:border-[var(--ds-primary)]/30 transition-colors">
              <div class="flex size-11 shrink-0 items-center justify-center rounded-[var(--ds-radius-lg)]" style="background-color: var(--ds-primary-soft); color: var(--ds-primary);">
                <LucideIcon name="inventory_2" size="20" />
              </div>
              <div>
                <p class="text-xs text-[var(--ds-text-muted)]">Số câu đã làm</p>
                <p class="font-semibold text-[var(--ds-text)] mt-0.5">{{ answeredSummary }}</p>
              </div>
            </div>
            <div class="flex items-start gap-4 p-4 rounded-xl border border-[var(--ds-border)] hover:border-[var(--ds-primary)]/30 transition-colors">
              <div class="flex size-11 shrink-0 items-center justify-center rounded-[var(--ds-radius-lg)]" style="background-color: var(--ds-primary-soft); color: var(--ds-primary);">
                <LucideIcon name="verified_user" size="20" />
              </div>
              <div>
                <p class="text-xs text-[var(--ds-text-muted)]">Mã bài nộp</p>
                <p class="font-semibold text-[var(--ds-text)] mt-0.5">#AT-{{ attemptId || '-' }}</p>
              </div>
            </div>
          </div>
        </div>

        <!-- Action Buttons -->
        <div class="mt-6 flex flex-col justify-center gap-3 sm:flex-row sm:gap-4 ds-animate-fade-up" style="animation-delay: 0.12s">
          <button
            type="button"
            class="inline-flex items-center justify-center gap-2 rounded-[var(--ds-radius-xl)] px-6 py-3.5 text-sm font-bold text-white shadow-[var(--ds-shadow-md)] transition-all hover:-translate-y-1 hover:shadow-[var(--ds-shadow-lg)]"
            style="background-color: var(--ds-primary);"
            @click="goToDashboard"
          >
            <LucideIcon name="dashboard" size="18" />
            Về trang chủ
          </button>
          <button
            type="button"
            class="inline-flex items-center justify-center gap-2 rounded-[var(--ds-radius-xl)] border border-[var(--ds-border)] px-6 py-3.5 text-sm font-semibold text-[var(--ds-text-secondary)] shadow-[var(--ds-shadow-sm)] transition-all hover:-translate-y-1 hover:bg-[var(--ds-gray-50)] hover:shadow-[var(--ds-shadow-md)]"
          >
            <LucideIcon name="download" size="18" />
            Lưu bài làm
          </button>
        </div>

        <p class="mt-8 flex items-center justify-center gap-2 text-sm italic" style="color: var(--ds-text-muted);">
          <LucideIcon name="info" size="14" />
          Đã Lưu Bài Làm
        </p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { getAttemptDetail, getAttemptReport } from '../../services/attemptService'
import { useToast } from '../../composables/useToast'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()
const detail = ref(null)
const report = ref(null)
const toast = useToast()

const attemptId = computed(() => Number.parseInt(String(route.query.attemptId || ''), 10) || null)

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

onMounted(async () => {
  if (!attemptId.value) return
  try {
    const [detailPayload, reportPayload] = await Promise.all([
      getAttemptDetail(attemptId.value),
      getAttemptReport(attemptId.value)
    ])
    detail.value = detailPayload
    report.value = reportPayload
  } catch (error) {
    toast.error('Không thể tải tóm tắt bài nộp.')
  }
})
</script>

<style scoped>
/* Success checkmark animation */
.checkmark {
  width: 52px;
  height: 52px;
  border-radius: 50%;
  display: block;
  stroke-width: 2;
  stroke: #fff;
  stroke-miterlimit: 10;
  box-shadow: inset 0px 0px 0px var(--ds-primary);
  animation: fill 0.4s ease-in-out 0.4s forwards, scale 0.3s ease-in-out 0.9s both;
}

.checkmark-circle {
  stroke-dasharray: 166;
  stroke-dashoffset: 166;
  stroke-width: 2;
  stroke-miterlimit: 10;
  stroke: #fff;
  fill: none;
  animation: stroke 0.6s cubic-bezier(0.65, 0, 0.45, 1) forwards;
}

.checkmark-check {
  transform-origin: 50% 50%;
  stroke-dasharray: 48;
  stroke-dashoffset: 48;
  stroke-width: 3;
  animation: stroke 0.3s cubic-bezier(0.65, 0, 0.45, 1) 0.8s forwards;
}

@keyframes stroke {
  100% { stroke-dashoffset: 0; }
}

@keyframes scale {
  0%, 100% { transform: none; }
  50% { transform: scale3d(1.1, 1.1, 1); }
}

@keyframes fill {
  100% { box-shadow: inset 0px 0px 0px 30px var(--ds-primary); }
}

/* Hide LucideIcon and show SVG checkmark */
.check-icon {
  animation: fadeInScale 0.3s ease-out forwards;
}

@keyframes fadeInScale {
  from { opacity: 0; transform: scale(0.8); }
  to { opacity: 1; transform: scale(1); }
}
</style>
