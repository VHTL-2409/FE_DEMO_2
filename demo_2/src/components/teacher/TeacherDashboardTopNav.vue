<template>
  <div
    :class="isDark ? 'dark' : 'light'"
    class="flex h-full min-h-0 flex-1 flex-col bg-background-light font-display text-slate-900 dark:bg-background-dark dark:text-slate-100"
  >
    <div class="layout-container flex min-h-0 flex-1 grow flex-col">
      <TeacherTopHeader active-section="dashboard" />

      <main
        class="teacher-stitch-main teacher-page-shell relative mx-auto flex w-full min-h-0 flex-1 flex-col overflow-x-hidden overflow-y-auto"
      >
        <div class="w-full space-y-6 px-3 pb-10 pt-2 sm:px-4 lg:px-5">
          <!-- Hero — stitch_new: surface-container-low + serif amber-900 + icon nền -->
          <section
            class="teacher-stitch-hero-panel relative mb-2 overflow-hidden rounded-xl border border-[rgba(219,194,176,0.45)] bg-[var(--role-surface-container-low)] p-8 transition-colors duration-200 hover:border-[var(--role-primary)]/35 hover:bg-[var(--role-surface-container-high)] dark:border-slate-600/50 dark:bg-slate-900/40 dark:hover:border-amber-700/50 dark:hover:bg-slate-900/60 md:p-10 lg:p-12"
          >
            <div class="relative z-10 min-w-0 max-w-2xl space-y-2">
              <p class="portal-kicker">
                <span class="text-slate-500 dark:text-slate-400">Trang chủ</span>
                <span class="mx-1.5 text-slate-300 dark:text-slate-600">/</span>
                <span class="font-semibold text-[var(--role-primary)]">Bảng điều khiển</span>
              </p>
              <h2
                class="teacher-page-title text-3xl font-bold leading-[1.08] tracking-tight text-amber-900 dark:text-amber-100 md:text-4xl lg:text-[3rem]"
              >
                <span class="block">Chào mừng trở lại,</span>
                <span class="text-[var(--role-primary)]">{{ teacherDisplayName }}</span>
              </h2>
            </div>
            <div class="pointer-events-none absolute right-0 top-0 flex h-full w-1/3 items-start justify-end opacity-[0.08] dark:opacity-[0.12]">
              <span class="material-symbols-outlined translate-x-2 text-[min(200px,28vw)] leading-none text-amber-900 dark:text-amber-200">school</span>
            </div>
          </section>

          <!-- KPI — stitch_new: surface-container-lowest, border-b-2 primary -->
          <section class="grid grid-cols-1 gap-6 md:grid-cols-2 lg:grid-cols-4">
            <article
              v-for="card in stitchDashboardKpis"
              :key="card.key"
              class="group relative flex flex-col justify-between rounded-xl p-8 shadow-sm transition-[box-shadow,background-color,border-color,filter] duration-200 hover:shadow-md"
              :class="
                card.accent
                  ? 'silk-press-gradient text-white ring-1 ring-[rgba(141,75,0,0.25)] hover:brightness-[1.03] hover:shadow-lg'
                  : 'border-b-2 border-[var(--role-primary)] bg-white hover:border-[var(--role-primary)]/80 hover:bg-[var(--role-surface-container-low)] dark:border-amber-600 dark:bg-slate-900/90 dark:hover:bg-slate-800/95'
              "
            >
              <div
                v-if="card.accent"
                class="pointer-events-none absolute -right-4 -top-4 size-24 rounded-full bg-white/15 blur-2xl"
              />
              <div class="relative z-10 mb-4 flex items-start justify-between">
                <div
                  class="rounded-lg p-3 transition-colors"
                  :class="
                    card.accent
                      ? 'bg-white/20 text-white'
                      : 'bg-[var(--role-primary-fixed)]/35 text-[var(--role-primary)] dark:bg-amber-950/50 dark:text-amber-300'
                  "
                >
                  <span class="material-symbols-outlined text-[24px]">{{ card.icon }}</span>
                </div>
                <span
                  v-if="!card.accent && card.badge"
                  class="rounded-full px-2 py-1 text-[10px] font-bold uppercase tracking-wider"
                  :class="card.badgeClass"
                >
                  {{ card.badge }}
                </span>
                <button
                  v-if="card.accent && card.to"
                  type="button"
                  class="text-[11px] font-bold text-white/95 underline-offset-2 hover:underline"
                  @click="goToPath(card.to)"
                >
                  Xem chi tiết
                </button>
              </div>
              <div class="relative z-10">
                <p
                  class="mb-1 text-xs font-bold uppercase tracking-widest"
                  :class="card.accent ? 'text-white/80' : 'text-[var(--role-outline)] dark:text-slate-500'"
                >
                  {{ card.label }}
                </p>
                <p
                  class="stitch-font-headline text-4xl font-bold tabular-nums"
                  :class="card.accent ? 'text-white' : 'text-amber-900 dark:text-amber-100'"
                >
                  {{ card.value }}
                  <span
                    v-if="card.subLabel"
                    class="text-sm font-semibold"
                    :class="card.accent ? 'text-white/75' : 'text-slate-500 dark:text-slate-400'"
                  >{{ card.subLabel }}</span>
                </p>
              </div>
            </article>
          </section>

          <!-- Lưới 8 + 4: biểu đồ trong card đầy đủ (head + well) | cột hành động + lịch -->
          <section class="grid grid-cols-12 gap-6 lg:items-stretch">
            <div class="col-span-12 flex h-full min-h-0 lg:col-span-8">
              <div class="teacher-dashboard-chart-card stitch-chart-card flex min-h-0 w-full flex-1 flex-col">
                <div class="stitch-chart-card__head teacher-dashboard-chart-card__head">
                  <div class="flex flex-col justify-between gap-3 sm:flex-row sm:items-start">
                    <div>
                      <h4 class="stitch-font-headline text-lg font-extrabold text-[#191c1e] dark:text-slate-100 md:text-xl">
                        Phân bố trạng thái đề thi
                      </h4>
                      <p class="mt-1 text-[11px] font-semibold uppercase tracking-wide text-slate-400">
                        Cập nhật khi tải trang
                      </p>
                    </div>
                  </div>
                </div>
                <div class="stitch-chart-card__body teacher-dashboard-chart-card__body">
                  <div class="stitch-chart-well teacher-dashboard-chart-well flex min-h-0 flex-1 flex-col overflow-hidden">
                    <div
                      ref="statusChartRef"
                      class="teacher-dashboard-echart min-h-[280px] w-full flex-1 overflow-hidden md:min-h-[300px]"
                    />
                  </div>
                </div>
              </div>
            </div>

            <div class="col-span-12 flex h-full min-h-0 flex-col gap-6 lg:col-span-4">
              <div class="teacher-dashboard-chart-card stitch-chart-card flex min-h-0 w-full flex-1 flex-col">
                <div class="stitch-chart-card__head teacher-dashboard-chart-card__head">
                  <div class="flex items-start justify-between gap-2">
                    <div>
                      <h4 class="stitch-font-headline text-base font-extrabold text-[#1b1b23] md:text-lg">
                        Lịch 7 ngày
                      </h4>
                    </div>
                    <span class="shrink-0 text-[11px] font-bold uppercase text-slate-400">Bắt đầu / kết thúc</span>
                  </div>
                </div>
                <div class="stitch-chart-card__body teacher-dashboard-chart-card__body">
                  <div class="stitch-chart-well teacher-dashboard-chart-well flex min-h-0 flex-1 flex-col overflow-hidden">
                    <div
                      ref="scheduleChartRef"
                      class="teacher-dashboard-echart min-h-[280px] w-full flex-1 overflow-hidden md:min-h-[300px]"
                    />
                  </div>
                </div>
              </div>
            </div>
          </section>

          <!-- Bảng đề thi — surface trắng, viền nhạt như Stitch -->
          <div
            class="overflow-hidden rounded-xl border border-[var(--stitch-outline)] bg-white shadow-sm transition-[box-shadow,border-color] duration-200 hover:border-[var(--role-primary)]/25 hover:shadow-md dark:bg-slate-900/40 dark:hover:border-amber-700/40"
          >
            <div class="flex flex-col gap-4 border-b border-[var(--stitch-outline)] p-6 sm:flex-row sm:items-center sm:justify-between sm:p-8">
              <h3 class="stitch-font-headline text-lg font-extrabold text-[#1b1b23] dark:text-slate-100">Đề thi gần đây</h3>
              <div class="flex flex-wrap gap-2">
                <button
                  :class="
                    listFilter === 'all'
                      ? 'bg-[var(--role-primary)] text-white shadow-sm'
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
            <div class="teacher-stitch-table-scroll teacher-stitch-table-scroll--slate-head">
              <table class="w-full text-left text-sm">
                <thead
                  class="bg-[var(--role-surface-container-low)] text-xs uppercase tracking-wider text-slate-600 dark:bg-slate-900/50 dark:text-slate-400"
                >
                  <tr>
                    <th class="px-6 py-4 font-bold sm:px-8">Tiêu đề</th>
                    <th class="px-6 py-4 font-bold sm:px-8">Ngày</th>
                    <th class="px-6 py-4 font-bold sm:px-8">Trạng thái</th>
                    <th class="px-6 py-4 font-bold sm:px-8">Câu hỏi</th>
                    <th class="px-6 py-4 text-right font-bold sm:px-8">Thao tác</th>
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
                      <p class="text-xs leading-5 text-slate-500 whitespace-normal break-words">{{ exam.subtitle }}</p>
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
                        :class="
                          exam.disabled
                            ? 'cursor-not-allowed text-slate-400'
                            : 'text-[var(--role-primary)] hover:opacity-90 dark:text-amber-300 dark:hover:text-amber-200'
                        "
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
            <div class="flex justify-start border-t border-slate-200/80 bg-slate-50/50 p-4 dark:border-slate-800 dark:bg-slate-900/30">
              <button
                class="text-sm font-semibold text-[var(--role-primary)] hover:underline dark:text-amber-300"
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
import { fetchMyProfile } from '../../services/authService'
import { listExams } from '../../services/examService'
import { useRouter } from 'vue-router'
import { useToast } from '../../composables/useToast'
import TeacherTopHeader from './TeacherTopHeader.vue'
import { createRafThrottle } from '../../utils/chartResizeThrottle'

const router = useRouter()
const isDark = ref(false)
const rawExams = ref([])
const profile = ref(null)
const listFilter = ref('all')
const toast = useToast()

const teacherDisplayName = computed(
  () => profile.value?.displayName || profile.value?.fullName || profile.value?.username || 'Giáo viên'
)

const statusChartRef = ref(null)
const scheduleChartRef = ref(null)
let statusChart = null
let scheduleChart = null
let echartsLib = null

const stitchQuickColumn = [
  { icon: 'edit_note', label: 'Soạn câu thủ công', path: '/teacher/exams/manual' },
  { icon: 'calendar_month', label: 'Lịch thi & đợt thi', path: '/teacher/exams/schedule' }
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
      className: 'bg-primary/10 text-primary dark:bg-primary/20 dark:text-amber-200'
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

const stitchDashboardKpis = computed(() => {
  const c = statusCounts.value
  const total = rawExams.value.length
  return [
    {
      key: 'kpi-total',
      label: 'Tổng số đề thi',
      value: total,
      icon: 'inventory_2',
      badge: total > 0 ? `${Math.min(total, 99)} đề` : '—',
      badgeClass: 'text-emerald-600 bg-emerald-50 dark:bg-emerald-950/40 dark:text-emerald-400',
      accent: false
    },
    {
      key: 'kpi-started',
      label: 'Đang diễn ra',
      value: c.started,
      icon: 'timer',
      badge: c.started > 0 ? 'Đang hoạt động' : 'Tạm nghỉ',
      badgeClass:
        c.started > 0
          ? 'text-[var(--role-primary-deep)] bg-[var(--role-primary-container)] dark:bg-amber-950/40 dark:text-amber-200'
          : 'text-slate-500 bg-slate-100 dark:bg-slate-800 dark:text-slate-400',
      accent: false
    },
    {
      key: 'kpi-ended',
      label: 'Đã kết thúc',
      value: c.ended,
      icon: 'analytics',
      badge: 'Theo lịch',
      badgeClass: 'text-amber-600 bg-amber-50 dark:bg-amber-950/40 dark:text-amber-400',
      accent: false
    },
    {
      key: 'kpi-monitor',
      label: 'Giám sát trực tiếp',
      value: c.started,
      subLabel: ' phiên mở',
      icon: 'live_tv',
      accent: true,
      to: '/teacher/live-monitoring'
    }
  ]
})

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

/** Cùng grid hai biểu đồ — vùng vẽ bằng nhau (legend lịch dùng tooltip, không chiếm đáy) */
const dashboardChartGrid = { left: '3%', right: '4%', bottom: '8%', top: '14%', containLabel: true }

const buildStatusLineOption = (echarts) => {
  const c = statusCounts.value
  const categories = ['Bản nháp', 'Chưa bắt đầu', 'Đang diễn ra', 'Đã kết thúc']
  const data = [c.draft, c.upcoming, c.started, c.ended]
  return {
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(15, 23, 42, 0.92)',
      borderColor: 'rgba(141, 75, 0, 0.5)',
      textStyle: { color: '#e2e8f0' }
    },
    grid: dashboardChartGrid,
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
        lineStyle: { width: 3, color: '#b15f00' },
        itemStyle: { color: '#d97706' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(141, 75, 0, 0.28)' },
            { offset: 1, color: 'rgba(141, 75, 0, 0.02)' }
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
      borderColor: 'rgba(141, 75, 0, 0.5)',
      textStyle: { color: '#e2e8f0' }
    },
    legend: { show: false },
    grid: dashboardChartGrid,
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
    if (!statusChart) statusChart = echarts.init(statusChartRef.value, null, { renderer: 'canvas' })
    statusChart.setOption(buildStatusLineOption(echarts), true)
  }
  if (scheduleChartRef.value) {
    if (!scheduleChart) scheduleChart = echarts.init(scheduleChartRef.value, null, { renderer: 'canvas' })
    scheduleChart.setOption(buildScheduleLineOption(), true)
  }
}

const onResize = createRafThrottle(() => {
  statusChart?.resize()
  scheduleChart?.resize()
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

const loadProfile = async () => {
  try {
    profile.value = await fetchMyProfile()
  } catch {
    profile.value = null
  }
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
  window.addEventListener('resize', onResize, { passive: true })
  await loadProfile()
  await loadExams()
  await nextTick()
  onResize()
})

onUnmounted(() => {
  window.removeEventListener('resize', onResize)
  disposeCharts()
})
</script>
