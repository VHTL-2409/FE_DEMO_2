<template>
  <div :class="isDark ? 'dark' : 'light'" class="portal-viewport flex h-full min-h-0 flex-col bg-background-light font-display text-slate-900 dark:bg-background-dark dark:text-slate-100">
    <div class="relative flex h-full min-h-0 flex-1 w-full flex-col overflow-x-hidden">
      <div class="layout-container flex h-full min-h-0 flex-1 grow flex-col">
        <StudentTopHeader class="shrink-0" />

        <main class="teacher-page-shell relative flex min-h-0 flex-1 flex-col overflow-hidden">
          <div class="portal-scrollbar relative flex min-h-0 flex-1 flex-col items-center justify-center overflow-y-auto px-4 py-5 md:py-6">
          <div class="relative flex w-full max-w-2xl flex-col items-center">
            <div class="relative mb-5 md:mb-6">
              <div class="relative h-24 w-24 bg-primary text-white rounded-full flex items-center justify-center shadow-lg shadow-primary/30 ring-4 ring-primary/15">
                <span class="material-symbols-outlined text-6xl">check_circle</span>
              </div>
            </div>

            <div class="mb-4 w-full max-w-lg animate-fade-up text-center">
              <p class="staff-kicker mb-2">Submission</p>
              <h1 class="teacher-page-title">Nộp bài thành công</h1>
              <p class="teacher-page-lead mt-2 text-slate-600 dark:text-slate-400">Bài làm đã được ghi nhận. Xem tóm tắt bên dưới.</p>
            </div>

            <div class="staff-surface-strong mt-4 w-full animate-fade-up-delay rounded-[1.75rem] p-5 md:mt-5 md:p-7">
              <h3 class="mb-4 text-sm font-bold uppercase tracking-wider text-primary">Tóm tắt bài nộp</h3>
              <div class="grid grid-cols-1 gap-4 md:grid-cols-2 md:gap-5">
                <div class="flex items-start gap-4">
                  <div class="bg-primary/10 p-2 rounded-lg text-primary">
                    <span class="material-symbols-outlined">calendar_today</span>
                  </div>
                  <div>
                    <p class="text-xs text-slate-500 dark:text-slate-400">Ngày &amp; giờ</p>
                    <p class="font-semibold">{{ submittedAtDisplay }}</p>
                  </div>
                </div>
                <div class="flex items-start gap-4">
                  <div class="bg-primary/10 p-2 rounded-lg text-primary">
                    <span class="material-symbols-outlined">timer</span>
                  </div>
                  <div>
                    <p class="text-xs text-slate-500 dark:text-slate-400">Thời gian làm bài</p>
                    <p class="font-semibold">{{ timeTakenDisplay }}</p>
                  </div>
                </div>
                <div class="flex items-start gap-4">
                  <div class="bg-primary/10 p-2 rounded-lg text-primary">
                    <span class="material-symbols-outlined">inventory_2</span>
                  </div>
                  <div>
                    <p class="text-xs text-slate-500 dark:text-slate-400">Số câu đã làm</p>
                    <p class="font-semibold">{{ answeredSummary }}</p>
                  </div>
                </div>
                <div class="flex items-start gap-4">
                  <div class="bg-primary/10 p-2 rounded-lg text-primary">
                    <span class="material-symbols-outlined">verified_user</span>
                  </div>
                  <div>
                    <p class="text-xs text-slate-500 dark:text-slate-400">Mã bài nộp</p>
                    <p class="font-semibold">#AT-{{ attemptId }}</p>
                  </div>
                </div>
              </div>
            </div>

            <div class="mt-4 flex w-full flex-col justify-center gap-3 sm:flex-row sm:gap-4">
              <BaseButton size="lg" class="w-full sm:w-auto" @click="goToDashboard">
                <span class="material-symbols-outlined">dashboard</span>
                Về Dashboard
              </BaseButton>
              <BaseButton variant="secondary" size="lg" class="w-full sm:w-auto" type="button">
                <span class="material-symbols-outlined">download</span>
                Lưu bài làm
              </BaseButton>
            </div>

            <p class="mt-8 flex items-center gap-2 text-sm italic text-slate-400 md:mt-10">
              <span class="material-symbols-outlined text-sm">info</span>
              Đã Lưu Bài Làm
            </p>
          </div>
          </div>
        </main>

      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { getAttemptDetail, getAttemptReport } from '../../services/attemptService'
import { useToast } from '../../composables/useToast'
import { useRoute, useRouter } from 'vue-router'
import StudentTopHeader from './StudentTopHeader.vue'
import BaseButton from '../shared/BaseButton.vue'

const route = useRoute()
const router = useRouter()
const isDark = ref(false)
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
  const answeredCount = Number(report.value?.answeredCount ?? detail.value?.answeredCount ?? 0)
  const totalQuestions = Number(detail.value?.totalQuestions ?? 0)
  if (!totalQuestions) return `${answeredCount}`
  return `${answeredCount} / ${totalQuestions}`
})

const goToDashboard = () => {
  router.push('/student/dashboard')
}

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
.font-display {
  font-family: 'Public Sans', sans-serif;
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
