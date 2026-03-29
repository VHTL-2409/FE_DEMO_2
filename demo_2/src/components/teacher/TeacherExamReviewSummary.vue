<template>
  <div
    :class="isDark ? 'dark' : 'light'"
    class="flex h-full min-h-0 flex-1 flex-col bg-background-light font-display text-slate-900 dark:bg-background-dark dark:text-slate-100"
  >
    <div class="layout-container flex min-h-0 flex-1 grow flex-col">
      <TeacherTopHeader active-section="review" />

      <main class="teacher-stitch-main teacher-page-shell portal-scrollbar relative w-full max-w-none min-h-0 flex-1 overflow-x-hidden overflow-y-auto px-3 py-6 sm:px-4 lg:px-5">

        <div class="relative mb-6 w-full max-w-screen-2xl animate-fade-up">
          <p class="portal-kicker mb-2">
            <RouterLink to="/teacher/dashboard" class="text-slate-500 transition hover:text-[var(--role-primary)] dark:text-slate-400">Trang chủ</RouterLink>
            <span class="mx-1.5 text-slate-300 dark:text-slate-600">/</span>
            <RouterLink class="text-slate-500 transition hover:text-[var(--role-primary)] dark:text-slate-400" to="/teacher/exams">Đề thi</RouterLink>
            <span class="mx-1.5 text-slate-300 dark:text-slate-600">/</span>
            <span class="font-semibold text-[var(--role-primary)]">Tổng quan điểm</span>
          </p>
          <div class="flex flex-col justify-between gap-4 md:flex-row md:items-end">
            <div>
              <h1 class="stitch-font-headline text-4xl font-bold tracking-tight text-amber-900 dark:text-amber-100 md:text-5xl">
                {{ selectedExamTitle }}
              </h1>
            </div>
            <div class="flex flex-wrap gap-2">
              <RouterLink
                :to="{ path: '/teacher/exams/new-session', query: { examId: examId, title: selectedExamTitle } }"
                class="inline-flex items-center justify-center gap-2 rounded-lg bg-[var(--role-primary)] px-4 py-2.5 font-bold text-white shadow-sm transition-all duration-200 hover:-translate-y-0.5 hover:opacity-95"
              >
                <span class="material-symbols-outlined text-xl">add_circle</span>
                <span>Tạo đợt thi mới</span>
              </RouterLink>
              <button
                type="button"
                @click="downloadReport"
                class="inline-flex items-center justify-center gap-2 px-4 py-2.5 bg-primary text-white font-bold rounded-lg hover:bg-primary/90 hover:-translate-y-0.5 transition-all duration-200 shadow-sm"
              >
                <span class="material-symbols-outlined text-xl">download</span>
                <span>Tải báo cáo</span>
              </button>
            </div>
          </div>
        </div>

        <div class="mb-6 flex w-full animate-fade-up-delay justify-start">
          <div class="teacher-stitch-segment inline-flex rounded-xl p-1 shadow-sm">
            <RouterLink :to="summaryTabLink" class="px-4 py-2 rounded-lg text-sm font-bold bg-primary text-white">
              Tổng quan điểm &amp; báo cáo
            </RouterLink>
            <RouterLink :to="incidentTabLink" class="px-4 py-2 rounded-lg text-sm font-bold text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors">
              Tổng quan hành vi gian lận
            </RouterLink>
          </div>
        </div>

        <div class="relative mb-8 grid grid-cols-1 gap-4 animate-fade-up-delay sm:grid-cols-2 lg:grid-cols-4">
          <div class="stitch-stat-bento transition-all duration-200 hover:-translate-y-0.5 hover:shadow-md" v-for="card in summaryCards" :key="card.title">
            <div class="flex items-center justify-between mb-2">
              <p class="text-slate-500 dark:text-slate-400 text-sm font-medium">{{ card.title }}</p>
              <span class="material-symbols-outlined text-primary">{{ card.icon }}</span>
            </div>
            <div class="flex items-baseline gap-2">
              <h3 class="text-2xl font-bold text-slate-900 dark:text-slate-100">{{ card.value }}</h3>
              <span :class="card.trendClass" class="text-sm font-bold">{{ card.trend }}</span>
            </div>
          </div>
        </div>

        <div v-if="questionWrongStats.length > 0" class="stitch-ambient-shadow bg-white dark:bg-slate-900 rounded-xl border border-slate-200 dark:border-slate-800 shadow-sm overflow-hidden animate-fade-up-delay mb-8">
          <div class="px-6 py-4 border-b border-slate-200 dark:border-slate-800 bg-amber-50 dark:bg-amber-900/20 flex items-center justify-between">
            <h2 class="flex items-center gap-2 text-lg font-bold text-slate-900 dark:text-slate-100">
              <span class="material-symbols-outlined text-amber-600 dark:text-amber-400">warning</span>
              Câu sai nhiều
            </h2>
          </div>
          <div class="teacher-stitch-table-scroll teacher-stitch-table-scroll--slate-head">
            <table class="w-full text-left border-collapse">
              <thead>
                <tr class="bg-slate-50/70 dark:bg-slate-800/50 border-b border-slate-200 dark:border-slate-800">
                  <th class="px-6 py-4 text-xs font-bold text-slate-500 uppercase tracking-wider">#</th>
                  <th class="px-6 py-4 text-xs font-bold text-slate-500 uppercase tracking-wider">Nội dung câu hỏi</th>
                  <th class="px-6 py-4 text-xs font-bold text-slate-500 uppercase tracking-wider">Số thí sinh</th>
                  <th class="px-6 py-4 text-xs font-bold text-slate-500 uppercase tracking-wider">Sai</th>
                  <th class="px-6 py-4 text-xs font-bold text-slate-500 uppercase tracking-wider">Đúng</th>
                  <th class="px-6 py-4 text-xs font-bold text-slate-500 uppercase tracking-wider">Tỷ lệ sai</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-slate-100 dark:divide-slate-800">
                <tr v-for="item in paginatedWrongStats" :key="item.questionId" class="hover:bg-slate-50 dark:hover:bg-slate-800/30 transition-colors">
                  <td class="px-6 py-4">
                    <span :class="item.rank <= 3 ? 'bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-400' : 'bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-400'" class="inline-flex items-center justify-center size-8 rounded-lg font-bold text-sm">{{ item.rank }}</span>
                  </td>
                  <td class="px-6 py-4 text-sm text-slate-800 dark:text-slate-200 max-w-md" :title="item.questionContent">
                    <span class="whitespace-normal break-words leading-5">{{ item.questionContent }}</span>
                  </td>
                  <td class="px-6 py-4 text-sm font-medium text-slate-700 dark:text-slate-300">{{ item.totalAnswered }}</td>
                  <td class="px-6 py-4 text-sm font-bold text-rose-600 dark:text-rose-400">{{ item.wrongCount }}</td>
                  <td class="px-6 py-4 text-sm font-medium text-emerald-600 dark:text-emerald-400">{{ item.correctCount }}</td>
                  <td class="px-6 py-4">
                    <span :class="item.wrongRatePercent >= 50 ? 'text-rose-600 dark:text-rose-400 font-bold' : 'text-slate-600 dark:text-slate-400'" class="text-sm">{{ item.wrongRatePercent }}%</span>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
          <div
            v-if="questionWrongStats.length > LIST_PAGE_SIZE"
            class="flex flex-col gap-2 border-t border-slate-200 bg-slate-50/80 px-4 py-3 dark:border-slate-800 dark:bg-slate-800/30 sm:flex-row sm:items-center sm:justify-between"
          >
            <p class="text-xs text-slate-500">
              {{ questionWrongStats.length }} câu · trang {{ pageWrong }} / {{ totalPagesWrong }}
            </p>
            <div class="flex items-center gap-2">
              <button
                type="button"
                class="rounded-lg border border-slate-200 bg-white px-3 py-1 text-xs font-semibold disabled:opacity-40 dark:border-slate-600 dark:bg-slate-800"
                :disabled="pageWrong <= 1"
                @click="pageWrong--"
              >
                Trước
              </button>
              <button
                type="button"
                class="rounded-lg border border-slate-200 bg-white px-3 py-1 text-xs font-semibold disabled:opacity-40 dark:border-slate-600 dark:bg-slate-800"
                :disabled="pageWrong >= totalPagesWrong"
                @click="pageWrong++"
              >
                Sau
              </button>
            </div>
          </div>
        </div>

        <div class="bg-white dark:bg-slate-900 rounded-xl border border-slate-200 dark:border-slate-800 shadow-sm overflow-hidden animate-fade-up-delay">
          <div class="px-6 py-4 border-b border-slate-200 dark:border-slate-800 bg-slate-50 dark:bg-slate-800/40 flex items-center justify-between">
            <h2 class="text-lg font-bold">Tổng quan điểm sinh viên</h2>
            <p class="text-xs text-slate-500">Điểm trung bình lớp: {{ classAverageLabel }} / 10</p>
          </div>
          <div class="teacher-stitch-table-scroll teacher-stitch-table-scroll--slate-head">
            <table class="w-full text-left border-collapse">
              <thead>
                <tr class="bg-slate-50/70 dark:bg-slate-800/50 border-b border-slate-200 dark:border-slate-800">
                  <th class="px-6 py-4 text-xs font-bold text-slate-500 uppercase tracking-wider">Sinh viên</th>
                  <th class="px-6 py-4 text-xs font-bold text-slate-500 uppercase tracking-wider">Mã sinh viên</th>
                  <th class="px-6 py-4 text-xs font-bold text-slate-500 uppercase tracking-wider">Điểm</th>
                  <th class="px-6 py-4 text-xs font-bold text-slate-500 uppercase tracking-wider">Độ chính xác</th>
                  <th class="px-6 py-4 text-xs font-bold text-slate-500 uppercase tracking-wider">Thời gian làm bài</th>
                  <th class="px-6 py-4 text-xs font-bold text-slate-500 uppercase tracking-wider text-right">Trạng thái</th>
                  <th class="px-6 py-4 text-xs font-bold text-slate-500 uppercase tracking-wider">Thao tác</th>

                </tr>
              </thead>
              <tbody class="divide-y divide-slate-100 dark:divide-slate-800">
                <tr v-if="isLoading">
                  <td colspan="7" class="px-6 py-10 text-center text-sm text-slate-500">Đang tải lượt làm bài...</td>
                </tr>
                <tr v-else-if="loadError">
                  <td colspan="7" class="px-6 py-10 text-center text-sm text-rose-600">{{ loadError }}</td>
                </tr>
                <tr v-else-if="!resultRows.length">
                  <td colspan="7" class="px-6 py-10 text-center text-sm text-slate-500">Chưa có lượt làm bài nào cho đề thi này.</td>
                </tr>
                <template v-else>
                  <tr v-for="item in paginatedResultRows" :key="item.attemptId" class="hover:bg-slate-50 dark:hover:bg-slate-800/30 transition-colors">
                    <td class="px-6 py-4 text-sm font-bold text-slate-900 dark:text-slate-100">{{ item.student }}</td>
                    <td class="px-6 py-4 text-xs text-slate-500">{{ item.studentId }}</td>
                    <td class="px-6 py-4 text-sm font-semibold">{{ item.score }}</td>
                    <td class="px-6 py-4 text-sm text-slate-600 dark:text-slate-300">{{ item.accuracy }}</td>
                    <td class="px-6 py-4 text-sm text-slate-600 dark:text-slate-300">{{ item.timeSpent }}</td>
                    <td class="px-6 py-4 text-right">
                      <span :class="item.statusClass" class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-bold">
                        {{ item.status }}
                      </span>
                    </td>
                    <td class="px-6 py-4">
                      <button
                        class="px-3 py-1.5 text-xs font-semibold text-primary bg-primary/10 hover:bg-primary/20 rounded transition-colors"
                        type="button"
                        @click="viewStudentReport(item)"
                      >
                        Xem báo cáo
                      </button>
                    </td>
                  </tr>
                </template>
              </tbody>
            </table>
          </div>
          <div
            v-if="resultRows.length > LIST_PAGE_SIZE"
            class="flex flex-col gap-2 border-t border-slate-200 bg-slate-50/80 px-4 py-3 dark:border-slate-800 dark:bg-slate-800/30 sm:flex-row sm:items-center sm:justify-between"
          >
            <p class="text-xs text-slate-500">
              {{ resultRows.length }} lượt · trang {{ pageResults }} / {{ totalPagesResults }}
            </p>
            <div class="flex items-center gap-2">
              <button
                type="button"
                class="rounded-lg border border-slate-200 bg-white px-3 py-1 text-xs font-semibold disabled:opacity-40 dark:border-slate-600 dark:bg-slate-800"
                :disabled="pageResults <= 1"
                @click="pageResults--"
              >
                Trước
              </button>
              <button
                type="button"
                class="rounded-lg border border-slate-200 bg-white px-3 py-1 text-xs font-semibold disabled:opacity-40 dark:border-slate-600 dark:bg-slate-800"
                :disabled="pageResults >= totalPagesResults"
                @click="pageResults++"
              >
                Sau
              </button>
            </div>
          </div>
        </div>
      </main>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { ApiError } from '../../services/apiClient'
import { listExamAttempts } from '../../services/attemptService'
import { getQuestionWrongStats } from '../../services/examService'
import { exportToCsv } from '../../utils/reportExport'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import TeacherTopHeader from './TeacherTopHeader.vue'

const route = useRoute()
const router = useRouter()
const isDark = ref(false)
const isLoading = ref(false)
const loadError = ref('')
const attempts = ref([])
const questionWrongStats = ref([])

const LIST_PAGE_SIZE = 10
const pageWrong = ref(1)
const pageResults = ref(1)

const paginatedWrongStats = computed(() => {
  const list = questionWrongStats.value
  const start = (pageWrong.value - 1) * LIST_PAGE_SIZE
  return list.slice(start, start + LIST_PAGE_SIZE)
})
const totalPagesWrong = computed(() => Math.max(1, Math.ceil(questionWrongStats.value.length / LIST_PAGE_SIZE)))

watch(questionWrongStats, () => {
  pageWrong.value = 1
})

const examId = computed(() => Number.parseInt(String(route.query.examId || ''), 10) || null)
const selectedExamTitle = computed(() => route.query.title || 'Đề thi đã chọn')
const summaryTabLink = computed(() => ({ path: '/teacher/exams/review/summary', query: route.query }))
const incidentTabLink = computed(() => ({ path: '/teacher/exams/review/incidents', query: route.query }))

const submittedAttempts = computed(() => attempts.value.filter((attempt) => [
  'SUBMITTED',
  'AUTO_SUBMITTED'
].includes(String(attempt.status || '').toUpperCase())))

const averageScoreValue = computed(() => {
  if (!submittedAttempts.value.length) return 0
  const total = submittedAttempts.value.reduce((sum, attempt) => sum + Number(attempt.score || 0), 0)
  return total / submittedAttempts.value.length
})

const classAverageLabel = computed(() => submittedAttempts.value.length ? (averageScoreValue.value / 10).toFixed(1) : '-')

const passRateValue = computed(() => {
  if (!submittedAttempts.value.length) return 0
  const passCount = submittedAttempts.value.filter((attempt) => Number(attempt.score || 0) >= 50).length
  return (passCount / submittedAttempts.value.length) * 100
})

const highestScoreValue = computed(() => {
  if (!submittedAttempts.value.length) return 0
  return Math.max(...submittedAttempts.value.map((attempt) => Number(attempt.score || 0)))
})

const averageCompletionMinutes = computed(() => {
  const durations = submittedAttempts.value
    .map((attempt) => {
      const startedAt = new Date(String(attempt.startedAt || '')).getTime()
      const submittedAt = new Date(String(attempt.submittedAt || '')).getTime()
      if (Number.isNaN(startedAt) || Number.isNaN(submittedAt) || submittedAt < startedAt) return null
      return Math.floor((submittedAt - startedAt) / 60000)
    })
    .filter((value) => value !== null)

  if (!durations.length) return 0
  return Math.round(durations.reduce((sum, value) => sum + value, 0) / durations.length)
})

const summaryCards = computed(() => {
  const attemptCount = submittedAttempts.value.length
  return [
    {
      title: 'Điểm trung bình',
      icon: 'insights',
      value: attemptCount ? (averageScoreValue.value / 10).toFixed(1) : '-',
      trend: `${attemptCount} lượt làm`,
      trendClass: 'text-slate-500 dark:text-slate-400'
    },
    {
      title: 'Tỷ lệ đạt',
      icon: 'check_circle',
      value: attemptCount ? `${passRateValue.value.toFixed(0)}%` : '-',
      trend: 'Đạt ≥ 5.0/10',
      trendClass: 'text-slate-500 dark:text-slate-400'
    },
    {
      title: 'Điểm cao nhất',
      icon: 'emoji_events',
      value: attemptCount ? (highestScoreValue.value / 10).toFixed(1) : '-',
      trend: attemptCount ? 'Chấm tự động' : 'Chưa có dữ liệu',
      trendClass: 'text-primary'
    },
    {
      title: 'TB hoàn thành',
      icon: 'timer',
      value: averageCompletionMinutes.value ? `${averageCompletionMinutes.value}m` : '-',
      trend: attemptCount ? 'Lượt đã nộp' : 'Chưa có dữ liệu',
      trendClass: 'text-slate-500 dark:text-slate-400'
    }
  ]
})

const statusConfig = {
  SUBMITTED: {
    label: 'Đã nộp',
    className: 'bg-emerald-100 text-emerald-700 dark:bg-emerald-900/30 dark:text-emerald-400'
  },
  AUTO_SUBMITTED: {
    label: 'Tự động nộp',
    className: 'bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-400'
  },
  IN_PROGRESS: {
    label: 'Đang làm bài',
    className: 'bg-[#006096]/10 text-[#006096] dark:bg-cyan-900/25 dark:text-cyan-300'
  }
}

const formatDuration = (attempt) => {
  const startedAt = new Date(String(attempt.startedAt || '')).getTime()
  const submittedAt = new Date(String(attempt.submittedAt || '')).getTime()
  if (Number.isNaN(startedAt) || Number.isNaN(submittedAt) || submittedAt < startedAt) return '-'

  const totalMinutes = Math.floor((submittedAt - startedAt) / 60000)
  const hours = Math.floor(totalMinutes / 60)
  const minutes = totalMinutes % 60
  if (hours > 0) return `${hours}h ${minutes}m`
  return `${minutes}m`
}

const resultRows = computed(() => attempts.value.map((attempt) => {
  const statusKey = String(attempt.status || '').toUpperCase()
  const statusMeta = statusConfig[statusKey] || {
    label: statusKey || 'Không rõ',
    className: 'bg-slate-100 text-slate-700 dark:bg-slate-800 dark:text-slate-300'
  }
  const scoreValue = Number(attempt.score || 0)

  return {
    attemptId: attempt.id,
    student: attempt.student || 'Sinh viên không rõ',
    studentId: `AT-${attempt.id}`,
    score: `${(scoreValue / 10).toFixed(1)} / 10`,
    accuracy: '-',
    timeSpent: formatDuration(attempt),
    status: statusMeta.label,
    statusClass: statusMeta.className,
    examId: attempt.examId
  }
}))

const paginatedResultRows = computed(() => {
  const list = resultRows.value
  const start = (pageResults.value - 1) * LIST_PAGE_SIZE
  return list.slice(start, start + LIST_PAGE_SIZE)
})
const totalPagesResults = computed(() => Math.max(1, Math.ceil(resultRows.value.length / LIST_PAGE_SIZE)))

watch(attempts, () => {
  pageResults.value = 1
})

const downloadReport = () => {
  const columns = [
    { key: 'student', label: 'Sinh viên' },
    { key: 'studentId', label: 'Mã sinh viên' },
    { key: 'score', label: 'Điểm' },
    { key: 'accuracy', label: 'Độ chính xác' },
    { key: 'timeSpent', label: 'Thời gian làm bài' },
    { key: 'status', label: 'Trạng thái' }
  ]
  const safeTitle = (selectedExamTitle.value || 'bao-cao').replace(/[^a-zA-Z0-9\u00C0-\u024F]/g, '-')
  exportToCsv(resultRows.value, columns, `bao-cao-diem-${safeTitle}.csv`)
}

const viewStudentReport = (item) => {
  router.push({
    path: '/teacher/exams/review/student-report',
    query: {
      examId: examId.value,
      attemptId: item.attemptId,
      title: selectedExamTitle.value
    }
  })
}

const loadAttempts = async () => {
  if (!examId.value) {
    loadError.value = 'Thiếu mã đề thi. Vui lòng mở báo cáo này từ trang Quản lý đề thi.'
    return
  }

  isLoading.value = true
  loadError.value = ''
  try {
    const [attemptsData, wrongStats] = await Promise.all([
      listExamAttempts(examId.value),
      getQuestionWrongStats(examId.value)
    ])
    attempts.value = attemptsData
    questionWrongStats.value = wrongStats
  } catch (error) {
    loadError.value = error instanceof ApiError ? error.message : 'Không thể tải lượt làm bài của đề thi.'
  } finally {
    isLoading.value = false
  }
}

onMounted(loadAttempts)
</script>

