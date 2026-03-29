<template>
  <div
    :class="isDark ? 'dark' : 'light'"
    class="flex h-full min-h-0 flex-1 flex-col bg-background-light font-display text-slate-900 dark:bg-background-dark dark:text-slate-100"
  >
    <div class="layout-container flex min-h-0 flex-1 grow flex-col">
      <TeacherTopHeader active-section="review" />

      <main class="teacher-stitch-main teacher-page-shell relative mx-auto w-full max-w-none min-h-0 flex-1 overflow-x-hidden overflow-y-auto px-3 py-4 sm:px-4 sm:py-5 lg:px-5">

        <header class="relative mb-8 w-full max-w-screen-2xl animate-fade-up">
          <p class="portal-kicker mb-2">
            <RouterLink to="/teacher/dashboard" class="text-slate-500 transition hover:text-[var(--role-primary)] dark:text-slate-400">Trang chủ</RouterLink>
            <span class="mx-1.5 text-slate-300 dark:text-slate-600">/</span>
            <RouterLink class="text-slate-500 transition hover:text-[var(--role-primary)] dark:text-slate-400" to="/teacher/exams">Đề thi</RouterLink>
            <span class="mx-1.5 text-slate-300 dark:text-slate-600">/</span>
            <span class="font-semibold text-[var(--role-primary)]">Báo cáo thí sinh</span>
          </p>

          <h1 class="stitch-font-headline text-3xl font-bold tracking-tight text-amber-900 dark:text-amber-100 md:text-5xl">
            {{ studentName }} — Chi tiết báo cáo
          </h1>
          <p class="mt-0.5 truncate text-xs text-[var(--role-on-surface-variant)] dark:text-slate-400">{{ examTitle }} · {{ studentId }}</p>
        </header>

        <div class="mb-8 grid grid-cols-1 gap-4 animate-fade-up-delay sm:grid-cols-3">
          <div class="stitch-stat-bento">
            <p class="text-xs font-bold uppercase tracking-wider text-slate-500">Điểm</p>
            <p class="stitch-font-headline mt-2 text-2xl font-bold text-amber-900 dark:text-amber-100">{{ score }}</p>
          </div>
          <div class="stitch-stat-bento">
            <p class="text-xs font-bold uppercase tracking-wider text-slate-500">Độ chính xác</p>
            <p class="stitch-font-headline mt-2 text-2xl font-bold text-amber-900 dark:text-amber-100">{{ accuracy }}</p>
          </div>
          <div class="stitch-stat-bento">
            <p class="text-xs font-bold uppercase tracking-wider text-slate-500">Thời gian làm bài</p>
            <p class="stitch-font-headline mt-2 text-2xl font-bold text-amber-900 dark:text-amber-100">{{ timeSpent }}</p>
          </div>
        </div>

        <div class="stitch-ambient-shadow overflow-hidden rounded-xl border border-slate-200 dark:border-slate-800 bg-white shadow-sm animate-fade-up-delay dark:bg-slate-900">
          <div class="border-b border-[color:rgba(219,194,176,0.4)] px-4 py-3 dark:border-slate-800">
            <h2 class="text-sm font-bold md:text-base">Câu trả lời</h2>
          </div>
          <p v-if="isLoading" class="p-6 text-sm text-slate-500">Đang tải…</p>
          <p v-else-if="!answerRows.length" class="p-6 text-sm text-slate-500">Không có dữ liệu.</p>
          <div v-else class="teacher-stitch-table-scroll">
            <table class="w-full border-collapse text-left text-sm">
              <thead>
                <tr class="teacher-stitch-table-head border-b border-[color:rgba(219,194,176,0.15)] dark:border-slate-800">
                  <th class="w-12 px-3 py-3 text-[10px] font-bold uppercase tracking-widest text-slate-400">#</th>
                  <th class="px-3 py-3 text-[10px] font-bold uppercase tracking-widest text-slate-400">Kết quả</th>
                  <th class="min-w-[12rem] px-3 py-3 text-[10px] font-bold uppercase tracking-widest text-slate-400">Câu hỏi</th>
                  <th class="min-w-[8rem] px-3 py-3 text-[10px] font-bold uppercase tracking-widest text-slate-400">Trả lời</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-[color:rgba(219,194,176,0.12)] dark:divide-slate-800">
                <tr
                  v-for="item in answerRows"
                  :key="item.questionId"
                  :class="item.correct ? 'bg-emerald-50/50 dark:bg-emerald-950/20' : 'bg-rose-50/50 dark:bg-rose-950/15'"
                  class="align-top"
                >
                  <td class="px-3 py-3 font-mono text-xs text-slate-500">{{ item.index }}</td>
                  <td class="px-3 py-3">
                    <span
                      :class="item.correct
                        ? 'bg-emerald-100 text-emerald-800 dark:bg-emerald-900/40 dark:text-emerald-300'
                        : 'bg-rose-100 text-rose-800 dark:bg-rose-900/40 dark:text-rose-300'"
                      class="inline-block rounded px-2 py-0.5 text-[10px] font-bold uppercase"
                    >
                      {{ item.correct ? 'Đúng' : 'Sai' }}
                    </span>
                  </td>
                  <td class="px-3 py-3 text-slate-800 dark:text-slate-200">
                    <span class="line-clamp-3 break-words leading-snug">{{ item.question }}</span>
                  </td>
                  <td class="px-3 py-3 text-slate-600 dark:text-slate-300">
                    <span class="line-clamp-2 break-words text-xs leading-snug">{{ item.selectedAnswer || '—' }}</span>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </main>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ApiError } from '../../services/apiClient'
import { getAttemptDetail, getAttemptReport } from '../../services/attemptService'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { useToast } from '../../composables/useToast'
import TeacherTopHeader from './TeacherTopHeader.vue'

const route = useRoute()
const router = useRouter()
const isDark = ref(false)
const isLoading = ref(false)
const detail = ref(null)
const report = ref(null)
const toast = useToast()

const attemptId = computed(() => Number.parseInt(String(route.query.attemptId || ''), 10) || null)
const examId = computed(() => Number.parseInt(String(route.query.examId || ''), 10) || null)
const examTitle = computed(() => detail.value?.examTitle || route.query.title || 'Đề thi đã chọn')
const studentName = computed(() => report.value?.student || detail.value?.student || 'Sinh viên')
const studentId = computed(() => attemptId.value ? `AT-${attemptId.value}` : 'N/A')

const score = computed(() => {
  const scoreValue = Number(report.value?.score ?? detail.value?.score)
  if (Number.isNaN(scoreValue)) return 'Không có'
  return `${(scoreValue / 10).toFixed(1)} / 10`
})

const accuracy = computed(() => {
  const correctCount = Number(report.value?.correctCount ?? 0)
  const answeredCount = Number(report.value?.answeredCount ?? detail.value?.answeredCount ?? 0)
  if (!answeredCount) return '-'
  const percent = (correctCount / answeredCount) * 100
  return `${percent.toFixed(0)}%`
})

const timeSpent = computed(() => {
  const startedAt = detail.value?.startedAt || report.value?.startedAt
  const submittedAt = detail.value?.submittedAt || report.value?.submittedAt
  if (!startedAt || !submittedAt) return '-'

  const diffMs = new Date(String(submittedAt)).getTime() - new Date(String(startedAt)).getTime()
  if (Number.isNaN(diffMs) || diffMs < 0) return '-'

  const totalMinutes = Math.floor(diffMs / 60000)
  const hours = Math.floor(totalMinutes / 60)
  const minutes = totalMinutes % 60
  return hours > 0 ? `${hours}h ${minutes}m` : `${minutes}m`
})

const answerRows = computed(() => (report.value?.answers || []).map((answer, index) => ({
  index: index + 1,
  questionId: answer.questionId,
  question: answer.question || 'Câu hỏi',
  selectedAnswer: answer.selectedAnswer,
  correct: Boolean(answer.correct)
})))

const goBack = () => {
  router.push({
    path: '/teacher/exams/review/summary',
    query: {
      examId: examId.value,
      title: examTitle.value
    }
  })
}

const loadAttemptData = async () => {
  if (!attemptId.value) {
    toast.error('Thiếu mã lượt làm bài. Vui lòng mở trang này từ tổng quan điểm.')
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
    toast.error(error instanceof ApiError ? error.message : 'Không thể tải chi tiết báo cáo sinh viên.')
  } finally {
    isLoading.value = false
  }
}

onMounted(loadAttemptData)
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
