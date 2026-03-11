<template>
  <div :class="isDark ? 'dark' : 'light'" class="bg-background-light dark:bg-background-dark font-display text-slate-900 dark:text-slate-100 min-h-screen">
    <div class="relative flex h-auto min-h-screen w-full flex-col overflow-x-hidden">
      <div class="layout-container flex h-full grow flex-col">
        <StudentTopHeader />

        <main class="relative flex-1 flex flex-col items-center justify-center px-4 py-12 lg:px-40 overflow-hidden">
          <div class="pointer-events-none absolute -top-16 -left-16 size-72 rounded-full bg-primary/15 blur-3xl animate-float-slow"></div>
          <div class="pointer-events-none absolute -bottom-24 -right-12 size-80 rounded-full bg-primary/10 blur-3xl animate-float-delay"></div>

          <div class="relative max-w-[640px] w-full flex flex-col items-center">
            <div class="mb-8 relative">
              <div class="absolute inset-0 bg-primary/20 blur-3xl rounded-full scale-150"></div>
              <div class="relative h-24 w-24 bg-primary text-white rounded-full flex items-center justify-center shadow-lg shadow-primary/30">
                <span class="material-symbols-outlined text-6xl">check_circle</span>
              </div>
            </div>

            <h1 class="text-3xl lg:text-4xl font-bold text-center mb-3 animate-fade-up">Nộp bài thành công</h1>
            <p class="text-slate-600 dark:text-slate-400 text-lg text-center mb-10">Câu trả lời của bạn đã được ghi nhận và gửi cho giảng viên để đánh giá.</p>

            <div class="w-full bg-white dark:bg-slate-800/50 rounded-xl border border-primary/10 p-6 mb-8 shadow-sm hover:-translate-y-0.5 hover:shadow-md transition-all duration-200 animate-fade-up-delay">
              <h3 class="text-sm font-bold uppercase tracking-wider text-primary mb-6">Tóm tắt bài nộp</h3>
              <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
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

            <p v-if="loadError" class="mb-4 text-sm text-rose-600">{{ loadError }}</p>

            <div class="flex flex-col sm:flex-row gap-4 w-full justify-center mt-4">
              <button @click="goToDashboard" class="flex items-center justify-center gap-2 bg-primary text-white px-8 py-3.5 rounded-xl font-bold shadow-lg shadow-primary/20 hover:bg-primary/90 hover:-translate-y-0.5 active:translate-y-0 transition-all duration-200" type="button">
                <span class="material-symbols-outlined">dashboard</span>
                Quay lại trang chủ
              </button>
              <button class="flex items-center justify-center gap-2 bg-primary/10 text-primary border border-primary/20 px-8 py-3.5 rounded-xl font-bold hover:bg-primary/20 hover:-translate-y-0.5 hover:shadow-md transition-all duration-200" type="button">
                <span class="material-symbols-outlined">download</span>
                Xem biên nhận nộp bài
              </button>
            </div>

            <p class="mt-12 text-slate-400 text-sm flex items-center gap-2 italic">
              <span class="material-symbols-outlined text-sm">info</span>
              Một bản sao biên nhận này đã được gửi đến email đã đăng ký của bạn.
            </p>
          </div>
        </main>

      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ApiError } from '../../services/apiClient'
import { getAttemptDetail, getAttemptReport } from '../../services/attemptService'
import { useRoute, useRouter } from 'vue-router'
import StudentTopHeader from './StudentTopHeader.vue'

const route = useRoute()
const router = useRouter()
const isDark = ref(false)
const detail = ref(null)
const report = ref(null)
const loadError = ref('')

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

  loadError.value = ''
  try {
    const [detailPayload, reportPayload] = await Promise.all([
      getAttemptDetail(attemptId.value),
      getAttemptReport(attemptId.value)
    ])
    detail.value = detailPayload
    report.value = reportPayload
  } catch (error) {
    loadError.value = error instanceof ApiError ? error.message : 'Không thể tải tóm tắt bài nộp.'
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
