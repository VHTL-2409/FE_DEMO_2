<template>
  <div
    :class="isDark ? 'dark' : 'light'"
    class="portal-viewport flex h-full min-h-0 flex-col bg-background-light font-display text-slate-900 dark:bg-background-dark dark:text-slate-100"
  >
    <div class="relative flex h-full min-h-0 flex-1 w-full flex-col overflow-x-hidden">
      <div class="layout-container flex h-full min-h-0 flex-1 grow flex-col">
        <StudentTopHeader active-section="examJoin" class="shrink-0" />

        <main
          class="teacher-page-shell student-stitch-main relative flex min-h-0 w-full max-w-screen-2xl flex-1 flex-col overflow-hidden"
        >
          <!-- stitch_new/submission_successful -->
          <div
            class="portal-scrollbar student-stitch-paper-bg relative flex min-h-0 flex-1 flex-col overflow-y-auto px-4 py-8 md:py-12"
          >
            <div class="mx-auto flex w-full max-w-3xl flex-col items-center">
              <nav class="mb-6 w-full text-left text-xs font-medium text-slate-500">
                <RouterLink to="/student/dashboard" class="transition hover:text-primary">Trang chủ</RouterLink>
                <span class="material-symbols-outlined mx-1 inline align-middle text-[14px] text-slate-400">chevron_right</span>
                <span class="font-semibold text-primary">Nộp bài</span>
              </nav>

              <div
                class="stitch-ghost-border stitch-editorial-shadow w-full rounded-xl border border-[#dbc2b0]/20 bg-white p-8 text-center dark:border-slate-700 dark:bg-slate-900/50 md:p-12"
              >
                <div class="relative mb-8 flex justify-center">
                  <div class="absolute inset-0 scale-150 rounded-full bg-primary/10 blur-2xl" aria-hidden="true" />
                  <div
                    class="relative flex size-24 items-center justify-center rounded-full border border-primary/15 bg-[#f4f4f0] text-primary dark:bg-slate-800"
                  >
                    <span class="material-symbols-outlined text-5xl" style="font-variation-settings: 'FILL' 1">check_circle</span>
                  </div>
                </div>

                <p class="mb-2 text-xs font-bold uppercase tracking-[0.2em] text-primary">Hoàn tất</p>
                <h1
                  class="stitch-font-headline mb-3 text-3xl font-bold tracking-tight text-[#1b1c1a] dark:text-slate-100 md:text-4xl lg:text-5xl"
                >
                  Nộp bài thành công
                </h1>
                <p class="mx-auto mb-10 max-w-md text-base text-[var(--stitch-on-surface-variant)] dark:text-slate-400">
                  Bài làm đã được ghi nhận trong hệ thống.
                </p>

                <div
                  class="mb-10 flex w-full flex-col items-stretch gap-6 rounded-xl bg-[#f4f4f0] p-6 text-left dark:bg-slate-800/80 md:flex-row md:items-center md:justify-between md:p-8"
                >
                  <div class="min-w-0 flex-1 space-y-1">
                    <span class="text-xs font-medium uppercase tracking-widest text-[#887364]">Tên bài</span>
                    <h2 class="stitch-font-headline truncate text-xl font-semibold text-[#1b1c1a] dark:text-slate-100 md:text-2xl">
                      {{ examTitleDisplay }}
                    </h2>
                  </div>
                  <div class="hidden h-12 w-px shrink-0 bg-[#dbc2b0]/40 md:block" aria-hidden="true" />
                  <div class="flex flex-wrap justify-center gap-8 md:justify-end">
                    <div class="text-center">
                      <span class="mb-1 block text-[10px] font-medium uppercase tracking-widest text-[#887364]">Thời điểm nộp</span>
                      <div class="flex items-center justify-center gap-2 text-[#1b1c1a] dark:text-slate-100">
                        <span class="material-symbols-outlined text-xl text-primary/80">schedule</span>
                        <span class="font-semibold">{{ submittedTimeOnly }}</span>
                      </div>
                      <span class="text-xs text-[var(--stitch-on-surface-variant)]">{{ submittedDateOnly }}</span>
                    </div>
                    <div class="text-center">
                      <span class="mb-1 block text-[10px] font-medium uppercase tracking-widest text-[#887364]">Tiến độ</span>
                      <div class="flex items-center justify-center gap-2 text-[#1b1c1a] dark:text-slate-100">
                        <span class="material-symbols-outlined text-xl text-primary/80">task_alt</span>
                        <span class="font-bold">{{ answeredSummary }}</span>
                      </div>
                      <span class="text-xs text-[var(--stitch-on-surface-variant)]">câu đã làm</span>
                    </div>
                  </div>
                </div>

                <div class="flex w-full flex-col items-stretch justify-center gap-3 md:flex-row md:flex-wrap md:justify-center">
                  <RouterLink
                    v-if="attemptId"
                    :to="{ path: '/student/exam-result', query: { attemptId: String(attemptId), examTitle: examTitleDisplay } }"
                    class="silk-press-gradient inline-flex min-h-[3rem] items-center justify-center gap-2 rounded-xl px-8 py-3 text-sm font-semibold text-white shadow-lg shadow-primary/20 transition hover:opacity-95"
                  >
                    <span class="material-symbols-outlined">visibility</span>
                    Xem kết quả
                  </RouterLink>
                  <button
                    type="button"
                    class="stitch-ghost-border inline-flex min-h-[3rem] items-center justify-center gap-2 rounded-xl bg-white px-8 py-3 text-sm font-semibold text-[#1b1c1a] transition hover:bg-[#faf9f5] dark:bg-slate-900 dark:text-slate-100 dark:hover:bg-slate-800"
                    @click="goToDashboard"
                  >
                    <span class="material-symbols-outlined">dashboard</span>
                    Về Dashboard
                  </button>
                  <RouterLink
                    to="/student/study-history"
                    class="inline-flex min-h-[3rem] items-center justify-center gap-2 px-4 py-3 text-sm font-semibold text-primary hover:underline"
                  >
                    <span class="material-symbols-outlined">history</span>
                    Lịch sử
                  </RouterLink>
                </div>
              </div>

              <div class="mt-8 flex w-full max-w-3xl items-center justify-between px-2 text-[var(--stitch-on-surface-variant)] opacity-70">
                <div class="flex items-center gap-2">
                  <span class="material-symbols-outlined text-sm">verified_user</span>
                  <span class="text-[10px] font-medium uppercase tracking-widest">Mã nộp #AT-{{ attemptId || '—' }}</span>
                </div>
              </div>
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
import { RouterLink, useRoute, useRouter } from 'vue-router'
import StudentTopHeader from './StudentTopHeader.vue'

const route = useRoute()
const router = useRouter()
const isDark = ref(false)
const detail = ref(null)
const report = ref(null)

const toast = useToast()

const attemptId = computed(() => Number.parseInt(String(route.query.attemptId || ''), 10) || null)

const examTitleDisplay = computed(() => {
  return detail.value?.examTitle || route.query.examTitle || route.query.exam || 'Bài thi'
})

const submittedAtDate = computed(() => {
  const submittedAt = detail.value?.submittedAt || route.query.submittedAt
  if (!submittedAt) return new Date()
  const date = new Date(String(submittedAt))
  return Number.isNaN(date.getTime()) ? new Date() : date
})

const submittedTimeOnly = computed(() =>
  submittedAtDate.value.toLocaleTimeString(undefined, { hour: '2-digit', minute: '2-digit' })
)
const submittedDateOnly = computed(() => submittedAtDate.value.toLocaleDateString())

const answeredSummary = computed(() => {
  const answeredCount = Number(report.value?.answeredCount ?? detail.value?.answeredCount ?? 0)
  const totalQuestions = Number(detail.value?.totalQuestions ?? 0)
  if (!totalQuestions) return `${answeredCount}`
  return `${answeredCount}/${totalQuestions}`
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
</style>
