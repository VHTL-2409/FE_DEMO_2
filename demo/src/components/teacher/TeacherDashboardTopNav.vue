<template>
  <div
    :class="isDark ? 'dark' : 'light'"
    class="bg-background-light dark:bg-background-dark font-display text-slate-900 dark:text-slate-100 min-h-screen"
  >
    <div class="layout-container flex h-full grow flex-col">
      <TeacherTopHeader active-section="dashboard" />

      <main
        class="teacher-page-shell relative mx-auto flex w-full max-w-6xl min-h-0 flex-1 flex-col overflow-x-hidden overflow-y-auto"
      >
        <div class="staff-page-wrap gap-6 pb-10">
          <PageHeader
            eyebrow="Giảng viên"
            title="Bảng điều khiển"
            subtitle="Tổng quan đề thi, trạng thái và lịch trong 7 ngày gần đây — giao diện gọn, dễ quét."
          />

          <section class="staff-metric-grid">
            <article
              v-for="card in kpiCards"
              :key="card.label"
              class="staff-metric-card portal-card-lift rounded-3xl border border-slate-200/80 p-5 transition-shadow dark:border-slate-700/80"
            >
              <div class="mb-3 flex items-start justify-between gap-2">
                <span class="staff-metric-label">{{ card.label }}</span>
                <span
                  class="material-symbols-outlined rounded-2xl p-2"
                  :class="card.iconWrap"
                >{{ card.icon }}</span>
              </div>
              <p class="staff-metric-value font-mono tabular-nums">{{ card.value }}</p>
            </article>
          </section>

          <section class="grid grid-cols-1 gap-4 xl:grid-cols-[1.15fr_minmax(0,0.85fr)]">
            <div class="staff-surface rounded-[1.75rem] p-5 md:p-6">
              <p class="staff-kicker mb-2">Trạng thái</p>
              <h3 class="text-[1.15rem] font-bold tracking-tight text-slate-900 dark:text-white">
                Phân bố trạng thái đề thi
              </h3>
              <p class="staff-muted-copy mt-1 text-sm">
                Đường nối mượt giữa bốn trạng thái — dễ so sánh hơn biểu đồ cột.
              </p>
              <div ref="statusChartRef" class="mt-4 h-[240px] w-full min-h-[200px]" />
            </div>

            <div class="staff-surface rounded-[1.75rem] p-5 md:p-6">
              <p class="staff-kicker mb-2">Thao tác</p>
              <h3 class="text-[1.15rem] font-bold tracking-tight text-slate-900 dark:text-white">
                Đi nhanh
              </h3>
              <div class="mt-4 flex flex-col gap-2.5">
                <button
                  v-for="action in quickActions"
                  :key="action.title"
                  class="group flex w-full items-start gap-3 rounded-2xl border border-slate-200/80 bg-white/80 p-3.5 text-left transition-colors hover:border-indigo-500/30 hover:bg-indigo-500/[0.04] dark:border-slate-700/80 dark:bg-slate-900/40 dark:hover:bg-slate-800/80"
                  type="button"
                  @click="goToPath(action.path)"
                >
                  <span
                    class="material-symbols-outlined shrink-0 rounded-xl bg-indigo-500/12 p-2 text-indigo-500 transition group-hover:text-indigo-400"
                  >{{ action.icon }}</span>
                  <div class="min-w-0">
                    <p class="text-sm font-semibold text-slate-900 dark:text-slate-100">{{ action.title }}</p>
                    <p class="text-xs text-slate-500 dark:text-slate-400">{{ action.subtitle }}</p>
                  </div>
                </button>
              </div>
            </div>
          </section>

          <section class="staff-surface rounded-[1.75rem] p-5 md:p-6">
            <p class="staff-kicker mb-2">Lịch 7 ngày</p>
            <h3 class="text-[1.15rem] font-bold tracking-tight text-slate-900 dark:text-white">
              Đề bắt đầu &amp; kết thúc theo ngày
            </h3>
            <p class="staff-muted-copy mt-1 text-sm">
              Gom theo ngày của <span class="font-medium text-slate-600 dark:text-slate-300">startTime</span> và
              <span class="font-medium text-slate-600 dark:text-slate-300">endTime</span> (múi giờ trình duyệt).
            </p>
            <div ref="scheduleChartRef" class="mt-4 h-[260px] w-full min-h-[220px]" />
          </section>

          <div class="staff-surface-strong overflow-hidden rounded-[1.75rem]">
            <div class="flex flex-col gap-4 border-b border-slate-200/80 p-5 dark:border-slate-700/80 sm:flex-row sm:items-center sm:justify-between">
              <h3 class="text-[1.05rem] font-bold text-slate-900 dark:text-white">Đề thi gần đây</h3>
              <div class="flex flex-wrap gap-2">
                <button
                  :class="
                    listFilter === 'all'
                      ? 'bg-slate-900 text-white dark:bg-white dark:text-slate-900'
                      : 'text-slate-600 hover:bg-slate-100 dark:text-slate-400 dark:hover:bg-slate-800/80'
                  "
                  class="rounded-lg px-3 py-1.5 text-xs font-semibold transition-colors"
                  type="button"
                  @click="listFilter = 'all'"
                >
                  Tất cả
                </button>
                <button
                  :class="
                    listFilter === 'active'
                      ? 'bg-emerald-600 text-white'
                      : 'text-slate-600 hover:bg-slate-100 dark:text-slate-400 dark:hover:bg-slate-800/80'
                  "
                  class="rounded-lg px-3 py-1.5 text-xs font-semibold transition-colors"
                  type="button"
                  @click="listFilter = 'active'"
                >
                  Đang diễn ra
                </button>
                <button
                  :class="
                    listFilter === 'ended'
                      ? 'bg-amber-600 text-white'
                      : 'text-slate-600 hover:bg-slate-100 dark:text-slate-400 dark:hover:bg-slate-800/80'
                  "
                  class="rounded-lg px-3 py-1.5 text-xs font-semibold transition-colors"
                  type="button"
                  @click="listFilter = 'ended'"
                >
                  Đã kết thúc
                </button>
              </div>
            </div>
            <div class="overflow-x-auto">
              <table class="w-full text-left text-sm">
                <thead
                  class="bg-slate-50/90 text-xs uppercase tracking-wider text-slate-500 dark:bg-slate-900/50 dark:text-slate-400"
                >
                  <tr>
                    <th class="px-5 py-3 font-bold">Tiêu đề</th>
                    <th class="px-5 py-3 font-bold">Ngày</th>
                    <th class="px-5 py-3 font-bold">Trạng thái</th>
                    <th class="px-5 py-3 font-bold">Câu hỏi</th>
                    <th class="px-5 py-3 text-right font-bold">Thao tác</th>
                  </tr>
                </thead>
                <tbody class="divide-y divide-slate-200 dark:divide-slate-800">
                  <tr v-if="!displayExams.length">
                    <td colspan="5" class="px-5 py-10 text-center text-slate-500 dark:text-slate-400">
                      Không có đề thi phù hợp với bộ lọc hiện tại.
                    </td>
                  </tr>
                  <tr
                    v-for="exam in displayExams"
                    :key="exam.id"
                    class="transition-colors hover:bg-slate-50/80 dark:hover:bg-slate-900/35"
                  >
                    <td class="px-5 py-3.5">
                      <p class="font-semibold text-slate-900 dark:text-slate-100">{{ exam.title }}</p>
                      <p class="line-clamp-1 text-xs text-slate-500">{{ exam.subtitle }}</p>
                    </td>
                    <td class="px-5 py-3.5 text-slate-600 dark:text-slate-300">{{ exam.date }}</td>
                    <td class="px-5 py-3.5">
                      <span
                        :class="exam.statusClass"
                        class="inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium"
                      >
                        {{ exam.status }}
                      </span>
                    </td>
                    <td class="px-5 py-3.5 tabular-nums text-slate-600 dark:text-slate-300">
                      {{ exam.participants }}
                    </td>
                    <td class="px-5 py-3.5 text-right">
                      <button
                        :disabled="exam.disabled"
                        :class="exam.disabled ? 'cursor-not-allowed text-slate-400' : 'text-indigo-600 hover:text-indigo-500 dark:text-indigo-400'"
                        class="inline-flex items-center gap-1 text-sm font-semibold"
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
            <div class="flex justify-center border-t border-slate-200/80 bg-slate-50/50 p-4 dark:border-slate-800 dark:bg-slate-900/30">
              <button
                class="text-sm font-semibold text-indigo-600 hover:underline dark:text-indigo-400"
                type="button"
                @click="goToPath('/teacher/exams/list')"
              >
                Xem tất cả đề thi
              </button>
            </div>
          </div>
        </div>
      </main>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref, watch, nextTick } from 'vue'
import { ApiError } from '../../services/apiClient'
import { listExams } from '../../services/examService'
import { useRouter } from 'vue-router'
import { useToast } from '../../composables/useToast'
import TeacherTopHeader from './TeacherTopHeader.vue'
import PageHeader from '../shared/PageHeader.vue'

const router = useRouter()
const isDark = ref(false)
const rawExams = ref([])
const listFilter = ref('all')
const toast = useToast()

const statusChartRef = ref(null)
const scheduleChartRef = ref(null)
let statusChart = null
let scheduleChart = null
let echartsLib = null
let ro = null

const quickActions = [
  { icon: 'add_circle', title: 'Tạo đề thi mới', subtitle: 'Lên lịch phiên thi mới', path: '/teacher/exams/create' },
  { icon: 'list_alt', title: 'Danh sách đề thi', subtitle: 'Quản lý toàn bộ đề thi', path: '/teacher/exams/list' },
  { icon: 'monitoring', title: 'Giám sát trực tiếp', subtitle: 'Kỳ thi đang diễn ra', path: '/teacher/live-monitoring' },
  { icon: 'person', title: 'Hồ sơ giảng viên', subtitle: 'Thông tin cá nhân', path: '/teacher/profile' }
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
      label: 'Đang diễn ra',
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

const exams = computed(() =>
  rawExams.value
    .slice()
    .sort((a, b) => getSortTime(b) - getSortTime(a))
    .map((exam) => {
      const statusMeta = getExamStatusMeta(exam)
      return {
        id: exam.id,
        title: exam.title,
        subtitle: exam.description || '—',
        date: formatDateTime(exam.endTime || exam.startTime),
        status: statusMeta.label,
        statusKey: statusMeta.key,
        statusClass: statusMeta.className,
        participants: `${exam.questionCount ?? 0} câu`,
        disabled: statusMeta.key !== 'ended',
        resultPath: '/teacher/exams/review/summary'
      }
    })
)

const statusCounts = computed(() =>
  exams.value.reduce(
    (acc, exam) => {
      acc[exam.statusKey] += 1
      return acc
    },
    { draft: 0, upcoming: 0, started: 0, ended: 0 }
  )
)

const startedCount = computed(() => statusCounts.value.started)
const endedCount = computed(() => statusCounts.value.ended)

const kpiCards = computed(() => [
  {
    label: 'Tổng số đề thi',
    value: rawExams.value.length,
    icon: 'inventory_2',
    iconWrap: 'bg-indigo-500/12 text-indigo-500'
  },
  {
    label: 'Đang diễn ra',
    value: startedCount.value,
    icon: 'timer',
    iconWrap: 'bg-emerald-500/12 text-emerald-600 dark:text-emerald-400'
  },
  {
    label: 'Đã kết thúc',
    value: endedCount.value,
    icon: 'event_busy',
    iconWrap: 'bg-amber-500/12 text-amber-600 dark:text-amber-400'
  }
])

const padDateKey = (d) => {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

const dateKeyFromIso = (iso) => {
  const dt = new Date(iso || '')
  if (Number.isNaN(dt.getTime())) return null
  return padDateKey(dt)
}

const buildLastNDays = (n) => {
  const days = []
  const now = new Date()
  for (let i = n - 1; i >= 0; i -= 1) {
    const d = new Date(now)
    d.setHours(0, 0, 0, 0)
    d.setDate(d.getDate() - i)
    days.push(d)
  }
  return days
}

const scheduleSeries = computed(() => {
  const days = buildLastNDays(7)
  const keys = days.map((d) => padDateKey(d))
  const startCount = new Map(keys.map((k) => [k, 0]))
  const endCount = new Map(keys.map((k) => [k, 0]))
  for (const exam of rawExams.value) {
    const sk = dateKeyFromIso(exam.startTime)
    if (sk && startCount.has(sk)) startCount.set(sk, startCount.get(sk) + 1)
    const ek = dateKeyFromIso(exam.endTime)
    if (ek && endCount.has(ek)) endCount.set(ek, endCount.get(ek) + 1)
  }
  return {
    labels: days.map((d) =>
      d.toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit' })
    ),
    starts: keys.map((k) => startCount.get(k)),
    ends: keys.map((k) => endCount.get(k))
  }
})

const axisMuted = () => (isDark.value ? '#94a3b8' : '#64748b')
const splitLine = () => (isDark.value ? 'rgba(148, 163, 184, 0.12)' : 'rgba(148, 163, 184, 0.18)')

const buildStatusLineOption = (echarts) => {
  const c = statusCounts.value
  const categories = ['Bản nháp', 'Chưa bắt đầu', 'Đang diễn ra', 'Đã kết thúc']
  const data = [c.draft, c.upcoming, c.started, c.ended]
  return {
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(15, 23, 42, 0.92)',
      borderColor: 'rgba(99, 102, 241, 0.45)',
      textStyle: { color: '#e2e8f0' }
    },
    grid: { left: '3%', right: '4%', bottom: '8%', top: '14%', containLabel: true },
    xAxis: {
      type: 'category',
      data: categories,
      boundaryGap: false,
      axisLine: { lineStyle: { color: 'rgba(148, 163, 184, 0.35)' } },
      axisLabel: { color: axisMuted(), fontSize: 11, interval: 0, rotate: 0 }
    },
    yAxis: {
      type: 'value',
      minInterval: 1,
      splitLine: { lineStyle: { color: splitLine() } },
      axisLabel: { color: axisMuted(), fontSize: 11 }
    },
    series: [
      {
        name: 'Số đề',
        type: 'line',
        smooth: 0.35,
        symbol: 'circle',
        symbolSize: 9,
        lineStyle: { width: 3, color: '#6366f1' },
        itemStyle: { color: '#818cf8' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(99, 102, 241, 0.35)' },
            { offset: 1, color: 'rgba(99, 102, 241, 0.02)' }
          ])
        },
        data
      }
    ]
  }
}

const buildScheduleLineOption = () => {
  const s = scheduleSeries.value
  return {
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(15, 23, 42, 0.92)',
      borderColor: 'rgba(99, 102, 241, 0.45)',
      textStyle: { color: '#e2e8f0' }
    },
    legend: {
      bottom: 0,
      textStyle: { color: axisMuted(), fontSize: 11 }
    },
    grid: { left: '3%', right: '4%', bottom: '18%', top: '12%', containLabel: true },
    xAxis: {
      type: 'category',
      data: s.labels,
      axisLine: { lineStyle: { color: 'rgba(148, 163, 184, 0.35)' } },
      axisLabel: { color: axisMuted(), fontSize: 11 }
    },
    yAxis: {
      type: 'value',
      minInterval: 1,
      splitLine: { lineStyle: { color: splitLine() } },
      axisLabel: { color: axisMuted(), fontSize: 11 }
    },
    series: [
      {
        name: 'Bắt đầu',
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 7,
        lineStyle: { width: 2.5, color: '#2dd4bf' },
        itemStyle: { color: '#2dd4bf' },
        data: s.starts
      },
      {
        name: 'Kết thúc',
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 7,
        lineStyle: { width: 2.5, color: '#f59e0b' },
        itemStyle: { color: '#fbbf24' },
        data: s.ends
      }
    ]
  }
}

const disposeCharts = () => {
  statusChart?.dispose()
  scheduleChart?.dispose()
  statusChart = null
  scheduleChart = null
}

const renderCharts = async () => {
  await nextTick()
  if (!echartsLib) {
    echartsLib = await import('echarts')
  }
  const echarts = echartsLib.default || echartsLib

  if (statusChartRef.value) {
    if (!statusChart) statusChart = echarts.init(statusChartRef.value)
    statusChart.setOption(buildStatusLineOption(echarts), true)
  }
  if (scheduleChartRef.value) {
    if (!scheduleChart) scheduleChart = echarts.init(scheduleChartRef.value)
    scheduleChart.setOption(buildScheduleLineOption(), true)
  }
}

const onResize = () => {
  statusChart?.resize()
  scheduleChart?.resize()
}

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
  try {
    rawExams.value = await listExams()
    await renderCharts()
  } catch (error) {
    toast.error(error instanceof ApiError ? error.message : 'Không thể tải dữ liệu dashboard.')
  }
}

watch([rawExams, isDark], () => {
  renderCharts()
})

onMounted(async () => {
  window.addEventListener('resize', onResize)
  await loadExams()
  await nextTick()
  if (typeof ResizeObserver !== 'undefined') {
    ro = new ResizeObserver(onResize)
    if (statusChartRef.value) ro.observe(statusChartRef.value)
    if (scheduleChartRef.value) ro.observe(scheduleChartRef.value)
  }
})

onUnmounted(() => {
  window.removeEventListener('resize', onResize)
  ro?.disconnect()
  disposeCharts()
})
</script>
