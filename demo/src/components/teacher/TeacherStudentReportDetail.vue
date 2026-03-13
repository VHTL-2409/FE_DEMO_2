<template>
  <div
    :class="isDark ? 'dark' : 'light'"
    class="bg-background-light dark:bg-background-dark font-display text-slate-900 dark:text-slate-100 min-h-screen"
  >
    <div class="layout-container flex h-full grow flex-col">
      <TeacherTopHeader active-section="exam" />

      <main class="relative flex-1 max-w-[1100px] mx-auto w-full p-4 lg:p-8 overflow-hidden">
        <div class="pointer-events-none absolute -top-16 -left-16 size-72 rounded-full bg-primary/15 blur-3xl animate-float-slow"></div>
        <div class="pointer-events-none absolute -bottom-24 -right-12 size-80 rounded-full bg-primary/10 blur-3xl animate-float-delay"></div>

        <div class="relative mb-8 animate-fade-up">
          <button
            type="button"
            @click="goBack"
            class="mb-4 inline-flex items-center gap-1 text-sm font-semibold text-primary hover:underline"
          >
            <span class="material-symbols-outlined text-base">arrow_back</span>
            Quay lại tổng quan điểm
          </button>

          <h1 class="text-3xl font-black tracking-tight">{{ studentName }} - Chi tiết báo cáo</h1>
          <p class="text-slate-500 dark:text-slate-400 mt-1">{{ examTitle }} • {{ studentId }}</p>
          <p v-if="loadError" class="text-sm text-rose-600 mt-2">{{ loadError }}</p>
        </div>

        <div class="grid grid-cols-1 sm:grid-cols-3 gap-4 mb-8 animate-fade-up-delay">
          <div class="bg-white dark:bg-slate-900 rounded-xl border border-slate-200 dark:border-slate-800 p-5 shadow-sm">
            <p class="text-xs uppercase tracking-wider text-slate-500">Điểm</p>
            <p class="text-2xl font-bold mt-2">{{ score }}</p>
          </div>
          <div class="bg-white dark:bg-slate-900 rounded-xl border border-slate-200 dark:border-slate-800 p-5 shadow-sm">
            <p class="text-xs uppercase tracking-wider text-slate-500">Độ chính xác</p>
            <p class="text-2xl font-bold mt-2">{{ accuracy }}</p>
          </div>
          <div class="bg-white dark:bg-slate-900 rounded-xl border border-slate-200 dark:border-slate-800 p-5 shadow-sm">
            <p class="text-xs uppercase tracking-wider text-slate-500">Thời gian làm bài</p>
            <p class="text-2xl font-bold mt-2">{{ timeSpent }}</p>
          </div>
        </div>

        <div class="bg-white dark:bg-slate-900 rounded-xl border border-slate-200 dark:border-slate-800 shadow-sm overflow-hidden animate-fade-up-delay">
          <div class="px-6 py-4 border-b border-slate-200 dark:border-slate-800 bg-slate-50 dark:bg-slate-800/40">
            <h2 class="text-lg font-bold">Xem lại câu hỏi (rút gọn)</h2>
          </div>

          <div class="p-6">
            <p v-if="isLoading" class="text-sm text-slate-500">Đang tải chi tiết báo cáo...</p>
            <p v-else-if="!answerRows.length" class="text-sm text-slate-500">Không tìm thấy câu trả lời cho lượt làm này.</p>
            <div v-else class="space-y-4">
              <div
                v-for="item in answerRows"
                :key="item.questionId"
                :class="item.correct
                  ? 'rounded-lg border border-emerald-200 dark:border-emerald-900/40 bg-emerald-50 dark:bg-emerald-900/10 p-4'
                  : 'rounded-lg border border-rose-200 dark:border-rose-900/40 bg-rose-50 dark:bg-rose-900/10 p-4'"
              >
                <div class="flex items-center justify-between gap-3">
                  <p class="font-semibold">Q{{ item.index }}. {{ item.question }}</p>
                  <span
                    :class="item.correct
                      ? 'text-xs font-bold px-2 py-1 rounded bg-emerald-100 text-emerald-700 dark:bg-emerald-900/30 dark:text-emerald-400'
                      : 'text-xs font-bold px-2 py-1 rounded bg-rose-100 text-rose-700 dark:bg-rose-900/30 dark:text-rose-400'"
                  >
                    {{ item.correct ? 'Đúng' : 'Sai' }}
                  </span>
                </div>
                <p class="text-sm text-slate-600 dark:text-slate-300 mt-2">Trả lời: {{ item.selectedAnswer || 'Không trả lời' }}</p>
              </div>
            </div>
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
import { useRoute, useRouter } from 'vue-router'
import TeacherTopHeader from './TeacherTopHeader.vue'

const route = useRoute()
const router = useRouter()
const isDark = ref(false)
const isLoading = ref(false)
const loadError = ref('')
const detail = ref(null)
const report = ref(null)

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
    loadError.value = 'Thiếu mã lượt làm bài. Vui lòng mở trang này từ tổng quan điểm.'
    return
  }

  isLoading.value = true
  loadError.value = ''
  try {
    const [detailPayload, reportPayload] = await Promise.all([
      getAttemptDetail(attemptId.value),
      getAttemptReport(attemptId.value)
    ])
    detail.value = detailPayload
    report.value = reportPayload
  } catch (error) {
    loadError.value = error instanceof ApiError ? error.message : 'Không thể tải chi tiết báo cáo sinh viên.'
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
