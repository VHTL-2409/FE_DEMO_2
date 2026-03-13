<template>
  <div
    :class="isDark ? 'dark' : 'light'"
    class="bg-background-light dark:bg-background-dark text-slate-900 dark:text-slate-100 font-display min-h-screen"
  >
    <div class="layout-container flex h-full grow flex-col">
      <TeacherTopHeader active-section="dashboard" />

      <main class="teacher-page-shell max-w-6xl">
        <div class="pointer-events-none absolute -top-16 -left-16 size-72 rounded-full bg-primary/15 blur-3xl animate-float-slow"></div>
        <div class="pointer-events-none absolute -bottom-24 -right-20 size-80 rounded-full bg-primary/10 blur-3xl animate-float-delay"></div>
        <div class="max-w-6xl mx-auto relative">
          <header class="mb-8 animate-fade-up">
            <h2 class="text-3xl font-black tracking-tight text-slate-900 dark:text-slate-100">Chào mừng quay lại, giảng viên</h2>
            <p class="text-slate-500 dark:text-slate-400 mt-1">
              Tổng quan về hiệu suất kỳ thi và các phiên đang hoạt động.
            </p>
          </header>

          <p v-if="loadError" class="mb-6 text-sm text-rose-600">{{ loadError }}</p>

          <div class="grid grid-cols-1 md:grid-cols-3 gap-6 mb-10 animate-fade-up-delay">
            <div class="bg-white dark:bg-background-dark border border-slate-200 dark:border-slate-800 p-6 rounded-xl shadow-sm hover:-translate-y-0.5 hover:shadow-md transition-all duration-200">
              <div class="flex justify-between items-start mb-4">
                <span class="material-symbols-outlined p-2 bg-primary/5 text-primary rounded-lg">inventory_2</span>
                <span class="text-primary text-xs font-bold px-2 py-1 bg-primary/10 rounded-full">Tổng quan</span>
              </div>
              <p class="text-slate-500 dark:text-slate-400 text-sm font-medium">Tổng số đề thi</p>
              <p class="text-3xl font-bold mt-1">{{ rawExams.length }}</p>
            </div>
            <div class="bg-white dark:bg-background-dark border border-slate-200 dark:border-slate-800 p-6 rounded-xl shadow-sm hover:-translate-y-0.5 hover:shadow-md transition-all duration-200">
              <div class="flex justify-between items-start mb-4">
                <span class="material-symbols-outlined p-2 bg-emerald-500/10 text-emerald-600 rounded-lg">timer</span>
                <span class="text-emerald-600 text-xs font-bold px-2 py-1 bg-emerald-50 dark:bg-emerald-900/20 rounded-full">Đang bắt đầu</span>
              </div>
              <p class="text-slate-500 dark:text-slate-400 text-sm font-medium">Đề thi đang diễn ra</p>
              <p class="text-3xl font-bold mt-1">{{ startedCount }}</p>
            </div>
            <div class="bg-white dark:bg-background-dark border border-slate-200 dark:border-slate-800 p-6 rounded-xl shadow-sm hover:-translate-y-0.5 hover:shadow-md transition-all duration-200">
              <div class="flex justify-between items-start mb-4">
                <span class="material-symbols-outlined p-2 bg-amber-500/10 text-amber-600 rounded-lg">event_busy</span>
                <span class="text-amber-700 text-xs font-bold px-2 py-1 bg-amber-50 dark:bg-amber-900/20 rounded-full">Hoàn tất</span>
              </div>
              <p class="text-slate-500 dark:text-slate-400 text-sm font-medium">Đề thi đã kết thúc</p>
              <p class="text-3xl font-bold mt-1">{{ endedCount }}</p>
            </div>
          </div>

          <div class="grid grid-cols-1 lg:grid-cols-3 gap-6 mb-10">
            <div class="lg:col-span-2 bg-white dark:bg-background-dark border border-slate-200 dark:border-slate-800 p-6 rounded-xl shadow-sm">
              <div class="flex justify-between items-center mb-6">
                <div>
                  <h3 class="font-bold text-lg">Phân bố trạng thái đề thi</h3>
                  <p class="text-slate-500 text-sm">4 trạng thái: bản nháp, chưa bắt đầu, đã bắt đầu, đã kết thúc</p>
                </div>
              </div>
              <div class="grid grid-cols-4 gap-4 items-end h-48 px-2">
                <div v-for="bar in statusBars" :key="bar.label" class="flex flex-col items-center gap-2 h-full justify-end">
                  <div :class="bar.className" class="rounded-t-md w-full transition-all" :style="{ height: bar.height }"></div>
                  <span class="text-[10px] font-bold text-slate-500 uppercase text-center">{{ bar.label }}</span>
                  <span class="text-xs font-semibold text-slate-700 dark:text-slate-200">{{ bar.value }}</span>
                </div>
              </div>
            </div>

            <div class="bg-white dark:bg-background-dark border border-slate-200 dark:border-slate-800 p-6 rounded-xl shadow-sm hover:-translate-y-0.5 hover:shadow-md transition-all duration-200">
              <h3 class="font-bold text-lg mb-4">Thao tác nhanh</h3>
              <div class="flex flex-col gap-3">
                <button
                  v-for="action in quickActions"
                  :key="action.title"
                  class="flex items-center gap-3 w-full p-3 rounded-lg border border-slate-100 dark:border-slate-800 hover:bg-slate-50 dark:hover:bg-slate-800 text-left transition-all"
                  type="button"
                  @click="goToPath(action.path)"
                >
                  <span class="material-symbols-outlined text-primary">{{ action.icon }}</span>
                  <div>
                    <p class="text-sm font-semibold">{{ action.title }}</p>
                    <p class="text-xs text-slate-500">{{ action.subtitle }}</p>
                  </div>
                </button>
              </div>
            </div>
          </div>

          <div class="bg-white dark:bg-background-dark border border-slate-200 dark:border-slate-800 rounded-xl shadow-sm overflow-hidden">
            <div class="p-6 border-b border-slate-200 dark:border-slate-800 flex justify-between items-center">
              <h3 class="font-bold text-lg">Đề thi gần đây</h3>
              <div class="flex gap-2">
                <button
                  :class="listFilter === 'all' ? 'bg-slate-100 dark:bg-slate-800 text-slate-900 dark:text-slate-100' : 'text-slate-500 hover:bg-slate-50 dark:hover:bg-slate-800/60'"
                  class="text-xs font-semibold px-3 py-1.5 rounded transition-colors"
                  type="button"
                  @click="listFilter = 'all'"
                >
                  Tất cả
                </button>
                <button
                  :class="listFilter === 'active' ? 'bg-emerald-100 dark:bg-emerald-900/20 text-emerald-700 dark:text-emerald-400' : 'text-slate-500 hover:bg-slate-50 dark:hover:bg-slate-800/60'"
                  class="text-xs font-semibold px-3 py-1.5 rounded transition-colors"
                  type="button"
                  @click="listFilter = 'active'"
                >
                  Đang diễn ra
                </button>
                <button
                  :class="listFilter === 'ended' ? 'bg-amber-100 dark:bg-amber-900/20 text-amber-700 dark:text-amber-400' : 'text-slate-500 hover:bg-slate-50 dark:hover:bg-slate-800/60'"
                  class="text-xs font-semibold px-3 py-1.5 rounded transition-colors"
                  type="button"
                  @click="listFilter = 'ended'"
                >
                  Đã kết thúc
                </button>
              </div>
            </div>
            <div class="overflow-x-auto">
              <table class="w-full text-left">
                <thead class="bg-slate-50 dark:bg-slate-900/50 text-slate-500 dark:text-slate-400 text-xs uppercase tracking-wider">
                  <tr>
                    <th class="px-6 py-4 font-bold">Tiêu đề đề thi</th>
                    <th class="px-6 py-4 font-bold">Ngày</th>
                    <th class="px-6 py-4 font-bold">Trạng thái</th>
                    <th class="px-6 py-4 font-bold">Số người tham gia</th>
                    <th class="px-6 py-4 font-bold text-right">Thao tác</th>
                  </tr>
                </thead>
                <tbody class="divide-y divide-slate-200 dark:divide-slate-800">
                  <tr v-if="!displayExams.length">
                    <td colspan="5" class="px-6 py-8 text-sm text-slate-500 dark:text-slate-400 text-center">
                      Không có đề thi phù hợp với bộ lọc hiện tại.
                    </td>
                  </tr>
                  <tr v-for="exam in displayExams" :key="exam.id" class="hover:bg-slate-50 dark:hover:bg-slate-900/40 transition-colors">
                    <td class="px-6 py-4">
                      <p class="text-sm font-semibold">{{ exam.title }}</p>
                      <p class="text-xs text-slate-500">{{ exam.subtitle }}</p>
                    </td>
                    <td class="px-6 py-4 text-sm">{{ exam.date }}</td>
                    <td class="px-6 py-4">
                      <span :class="exam.statusClass" class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium">
                        {{ exam.status }}
                      </span>
                    </td>
                    <td class="px-6 py-4 text-sm">{{ exam.participants }}</td>
                    <td class="px-6 py-4 text-right">
                      <button
                        :disabled="exam.disabled"
                        :class="exam.disabled ? 'text-slate-400 cursor-not-allowed' : 'text-primary hover:text-primary/80'"
                        class="font-semibold text-sm inline-flex items-center gap-1"
                        type="button"
                        @click="openExamResult(exam)"
                      >
                        <span class="material-symbols-outlined text-[18px]">{{ exam.disabled ? 'lock' : 'visibility' }}</span>
                        {{ exam.disabled ? 'Đang xử lý' : 'Xem kết quả' }}
                      </button>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
            <div class="p-4 bg-slate-50 dark:bg-slate-900/20 border-t border-slate-200 dark:border-slate-800 flex justify-center">
              <button class="text-sm font-semibold text-primary hover:underline" type="button">Xem tất cả hồ sơ kỳ thi</button>
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
import { listExams } from '../../services/examService'
import { useRouter } from 'vue-router'
import TeacherTopHeader from './TeacherTopHeader.vue'

const router = useRouter()
const isDark = ref(false)
const rawExams = ref([])
const loadError = ref('')
const listFilter = ref('all')

const quickActions = [
  { icon: 'add_circle', title: 'Tạo đề thi mới', subtitle: 'Lên lịch phiên thi mới', path: '/teacher/exams/create' },
  { icon: 'list_alt', title: 'Xem danh sách đề thi', subtitle: 'Quản lý toàn bộ đề thi', path: '/teacher/exams/list' },
  { icon: 'monitoring', title: 'Giám sát trực tiếp', subtitle: 'Theo dõi kỳ thi đang diễn ra', path: '/teacher/live-monitoring' },
  { icon: 'person', title: 'Hồ sơ giảng viên', subtitle: 'Cập nhật thông tin cá nhân', path: '/teacher/profile' }
]

const getExamStatusMeta = (exam) => {
  if (!exam.isActive) {
    return {
      key: 'draft',
      label: 'Bản nháp',
      className: 'bg-slate-100 text-slate-700 dark:bg-slate-800 dark:text-slate-300'
    }
  }

  const nowMs = Date.now()
  const startMs = new Date(exam.startTime || '').getTime()
  const endMs = new Date(exam.endTime || '').getTime()

  if (!Number.isNaN(startMs) && nowMs < startMs) {
    return {
      key: 'upcoming',
      label: 'Chưa bắt đầu',
      className: 'bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-400'
    }
  }

  if (!Number.isNaN(startMs) && !Number.isNaN(endMs) && nowMs >= startMs && nowMs <= endMs) {
    return {
      key: 'started',
      label: 'Đã bắt đầu',
      className: 'bg-emerald-100 text-emerald-700 dark:bg-emerald-900/30 dark:text-emerald-400'
    }
  }

  return {
    key: 'ended',
    label: 'Đã kết thúc',
    className: 'bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-400'
  }
}

const getSortTime = (exam) => {
  const candidates = [exam.endTime, exam.startTime, exam.updatedAt, exam.createdAt]
  for (const value of candidates) {
    const time = new Date(value || '').getTime()
    if (!Number.isNaN(time)) return time
  }
  return 0
}

const formatDateTime = (value) => {
  const date = new Date(value || '')
  return Number.isNaN(date.getTime()) ? '-' : date.toLocaleString()
}

const exams = computed(() => rawExams.value
  .slice()
  .sort((a, b) => getSortTime(b) - getSortTime(a))
  .map((exam) => {
    const statusMeta = getExamStatusMeta(exam)
    return {
      id: exam.id,
      title: exam.title,
      subtitle: exam.description || '-',
      date: formatDateTime(exam.endTime || exam.startTime),
      status: statusMeta.label,
      statusKey: statusMeta.key,
      statusClass: statusMeta.className,
      participants: `${exam.questionCount || 0} câu hỏi`,
      disabled: statusMeta.key !== 'ended',
      resultPath: '/teacher/exams/review/summary'
    }
  }))

const statusCounts = computed(() => exams.value.reduce((acc, exam) => {
  acc[exam.statusKey] += 1
  return acc
}, {
  draft: 0,
  upcoming: 0,
  started: 0,
  ended: 0
}))

const startedCount = computed(() => statusCounts.value.started)
const endedCount = computed(() => statusCounts.value.ended)

const statusBars = computed(() => {
  const total = rawExams.value.length || 1
  return [
    { label: 'Bản nháp', value: statusCounts.value.draft, className: 'bg-slate-400/80', height: `${Math.max((statusCounts.value.draft / total) * 100, 8)}%` },
    { label: 'Chưa bắt đầu', value: statusCounts.value.upcoming, className: 'bg-blue-500/80', height: `${Math.max((statusCounts.value.upcoming / total) * 100, 8)}%` },
    { label: 'Đã bắt đầu', value: statusCounts.value.started, className: 'bg-emerald-500/80', height: `${Math.max((statusCounts.value.started / total) * 100, 8)}%` },
    { label: 'Đã kết thúc', value: statusCounts.value.ended, className: 'bg-amber-500/80', height: `${Math.max((statusCounts.value.ended / total) * 100, 8)}%` }
  ]
})

const displayExams = computed(() => {
  if (listFilter.value === 'active') {
    return exams.value.filter((exam) => exam.statusKey === 'started')
  }
  if (listFilter.value === 'ended') {
    return exams.value.filter((exam) => exam.statusKey === 'ended')
  }
  return exams.value
})

const goToPath = (path) => {
  if (!path) return
  router.push(path)
}

const openExamResult = (exam) => {
  if (exam.disabled || !exam.resultPath) {
    return
  }

  router.push({
    path: exam.resultPath,
    query: { title: exam.title, examId: exam.id }
  })
}

const loadExams = async () => {
  loadError.value = ''
  try {
    rawExams.value = await listExams()
  } catch (error) {
    loadError.value = error instanceof ApiError ? error.message : 'Không thể tải dữ liệu dashboard.'
  }
}

onMounted(loadExams)
</script>

