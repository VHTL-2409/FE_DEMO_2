<template>
  <div
    :class="isDark ? 'dark' : 'light'"
    class="bg-background-light dark:bg-background-dark font-display text-slate-900 dark:text-slate-100 min-h-screen"
  >
    <div class="layout-container flex h-full grow flex-col">
      <TeacherTopHeader active-section="exam" />

      <main class="teacher-page-shell max-w-[1280px]">
        <div class="pointer-events-none absolute -top-16 -left-16 size-72 rounded-full bg-primary/15 blur-3xl animate-float-slow"></div>
        <div class="pointer-events-none absolute -bottom-24 -right-12 size-80 rounded-full bg-primary/10 blur-3xl animate-float-delay"></div>

        <div class="relative mb-6 animate-fade-up">
          <div class="flex items-center gap-2 text-sm text-slate-500 dark:text-slate-400 mb-2">
            <RouterLink class="hover:text-primary transition-colors flex items-center gap-1" to="/teacher/exams"><span class="material-symbols-outlined text-base">assignment</span> Đề thi</RouterLink>
            <span class="material-symbols-outlined text-xs">chevron_right</span>
            <span class="text-slate-900 dark:text-slate-100 font-medium">Tổng quan điểm</span>
          </div>
          <div class="flex flex-col md:flex-row md:items-end justify-between gap-4">
            <div>
              <h1 class="text-3xl font-extrabold text-slate-900 dark:text-slate-100 tracking-tight">{{ selectedExamTitle }}</h1>
              <p class="text-slate-600 dark:text-slate-400 mt-1">Báo cáo tổng điểm và tóm tắt kết quả học tập của sinh viên cho đề thi đã hoàn tất này.</p>
            </div>
            <div class="flex flex-wrap gap-2">
              <RouterLink
                :to="{ path: '/teacher/exams/new-session', query: { examId: examId, title: selectedExamTitle } }"
                class="inline-flex items-center justify-center gap-2 px-4 py-2.5 bg-indigo-600 text-white font-bold rounded-lg hover:bg-indigo-700 hover:-translate-y-0.5 transition-all duration-200 shadow-sm"
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

        <div class="mb-6 animate-fade-up-delay">
          <div class="inline-flex rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-900 p-1 shadow-sm">
            <RouterLink :to="summaryTabLink" class="px-4 py-2 rounded-lg text-sm font-bold bg-primary text-white">
              Tổng quan điểm &amp; báo cáo
            </RouterLink>
            <RouterLink :to="incidentTabLink" class="px-4 py-2 rounded-lg text-sm font-bold text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors">
              Tổng quan hành vi gian lận
            </RouterLink>
          </div>
        </div>

        <div class="relative grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 mb-8 animate-fade-up-delay">
          <div class="bg-white dark:bg-slate-900 p-5 rounded-xl border border-slate-200 dark:border-slate-800 shadow-sm hover:-translate-y-0.5 hover:shadow-md transition-all duration-200" v-for="card in summaryCards" :key="card.title">
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

        <div v-if="questionWrongStats.length > 0" class="bg-white dark:bg-slate-900 rounded-xl border border-slate-200 dark:border-slate-800 shadow-sm overflow-hidden animate-fade-up-delay mb-8">
          <div class="px-6 py-4 border-b border-slate-200 dark:border-slate-800 bg-amber-50 dark:bg-amber-900/20 flex items-center justify-between">
            <h2 class="text-lg font-bold text-slate-900 dark:text-slate-100 flex items-center gap-2">
              <span class="material-symbols-outlined text-amber-600 dark:text-amber-400">warning</span>
              Câu hỏi sai nhiều nhất
            </h2>
            <p class="text-xs text-slate-500">Điểm yếu cần ôn tập</p>
          </div>
          <div class="overflow-x-auto">
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
                <tr v-for="item in questionWrongStats" :key="item.questionId" class="hover:bg-slate-50 dark:hover:bg-slate-800/30 transition-colors">
                  <td class="px-6 py-4">
                    <span :class="item.rank <= 3 ? 'bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-400' : 'bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-400'" class="inline-flex items-center justify-center size-8 rounded-lg font-bold text-sm">{{ item.rank }}</span>
                  </td>
                  <td class="px-6 py-4 text-sm text-slate-800 dark:text-slate-200 max-w-md" :title="item.questionContent">
                    <span class="line-clamp-2">{{ item.questionContent }}</span>
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
        </div>

        <div class="bg-white dark:bg-slate-900 rounded-xl border border-slate-200 dark:border-slate-800 shadow-sm overflow-hidden animate-fade-up-delay">
          <div class="px-6 py-4 border-b border-slate-200 dark:border-slate-800 bg-slate-50 dark:bg-slate-800/40 flex items-center justify-between">
            <h2 class="text-lg font-bold">Tổng quan điểm sinh viên</h2>
            <p class="text-xs text-slate-500">Điểm trung bình lớp: {{ classAverageLabel }} / 10</p>
          </div>
          <div class="overflow-x-auto">
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
                  <tr v-for="item in resultRows" :key="item.attemptId" class="hover:bg-slate-50 dark:hover:bg-slate-800/30 transition-colors">
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
        </div>
      </main>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
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
    className: 'bg-sky-100 text-sky-700 dark:bg-sky-900/30 dark:text-sky-400'
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

