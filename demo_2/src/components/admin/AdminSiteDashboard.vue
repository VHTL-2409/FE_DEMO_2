<template>
  <div class="staff-page-wrap admin-stitch-dashboard min-h-0 flex-1 gap-8 pb-2">
    <div class="w-full max-w-screen-2xl">
      <header class="mb-8 flex flex-col gap-4 md:flex-row md:items-end md:justify-between">
        <div class="space-y-2">
          <span class="admin-stitch-eyebrow block">Hệ thống quản trị trực tuyến</span>
          <h1 class="admin-stitch-page-title">Tổng quan học thuật</h1>
          <p class="max-w-2xl text-sm leading-relaxed text-slate-400">
            Người dùng, đề thi, câu hỏi và lượt thi — làm mới khi cần.
          </p>
        </div>
        <button
          type="button"
          class="inline-flex items-center gap-2 rounded-xl border border-slate-600/80 bg-slate-800/80 px-5 py-3 text-sm font-semibold text-slate-200 shadow-sm transition hover:bg-slate-700/90 disabled:opacity-50"
          :disabled="loading"
          @click="loadStats"
        >
          <span class="material-symbols-outlined text-lg" :class="{ 'animate-spin': loading }">refresh</span>
          Làm mới
        </button>
      </header>

      <section class="grid grid-cols-1 gap-6 md:grid-cols-2 lg:grid-cols-4">
        <article
          v-for="card in kpiCardsStyled"
          :key="card.label"
          class="admin-stitch-kpi-card group relative overflow-hidden rounded-xl border-l-4 p-6 transition-shadow hover:shadow-lg"
          :class="[card.borderClass]"
        >
          <span
            class="material-symbols-outlined pointer-events-none absolute -bottom-3 -right-2 text-8xl text-primary/10 transition-transform group-hover:scale-110"
            aria-hidden="true"
          >
            {{ card.icon }}
          </span>
          <div class="relative z-10 space-y-3">
            <div
              class="flex size-10 items-center justify-center rounded-lg text-primary"
              :class="card.iconWrapClass"
            >
              <span class="material-symbols-outlined">{{ card.icon }}</span>
            </div>
            <p class="font-mono text-3xl font-bold tabular-nums tracking-tight text-slate-50 md:text-4xl">
              {{ formatNum(card.value) }}
            </p>
            <p class="text-[11px] font-semibold uppercase tracking-wider text-slate-400">
              {{ card.label }}
            </p>
          </div>
        </article>
      </section>

    <section class="grid grid-cols-1 gap-4 xl:grid-cols-[0.9fr_1.1fr]">
      <div class="rounded-[1.75rem] border border-slate-700/50 bg-slate-900/60 p-5 shadow-sm md:p-6">
        <div class="mb-4 flex items-center justify-between gap-3">
          <div>
            <p class="mb-2 text-[10px] font-bold uppercase tracking-[0.18em] text-amber-500/90">Truy cập nhanh</p>
            <h3 class="font-serif text-[1.15rem] font-bold tracking-tight text-slate-50">Các nhóm quản trị</h3>
          </div>
        </div>

        <div class="grid grid-cols-2 gap-3 sm:grid-cols-2">
          <RouterLink
            v-for="q in quickLinks"
            :key="q.to"
            :to="q.to"
            class="group flex flex-col rounded-2xl border border-slate-700/40 bg-[#131c2e]/90 p-4 text-left transition-colors hover:border-amber-600/30 hover:bg-slate-800/80"
          >
            <span class="material-symbols-outlined text-2xl text-amber-500/90 transition group-hover:text-amber-400">{{ q.icon }}</span>
            <p class="mt-3 text-sm font-semibold text-slate-100">{{ q.label }}</p>
            <p v-if="q.hint" class="mt-1 text-xs leading-5 text-slate-500">{{ q.hint }}</p>
          </RouterLink>
        </div>
      </div>

      <div class="stitch-chart-card min-h-0 border border-slate-700/50 bg-slate-900/40">
        <div class="stitch-chart-card__head">
          <p class="mb-2 text-[10px] font-bold uppercase tracking-[0.18em] text-amber-500/90">7 ngày</p>
          <h3 class="font-serif text-[1.15rem] font-bold tracking-tight text-slate-50">Lượt thi</h3>
        </div>
        <div class="stitch-chart-card__body">
          <div class="stitch-chart-well flex min-h-0 flex-col overflow-hidden">
            <div ref="lineChartRef" class="h-[240px] w-full max-w-full shrink-0 overflow-hidden sm:h-[260px]" />
          </div>
        </div>
      </div>
    </section>

    <section class="grid grid-cols-1 gap-4 xl:grid-cols-[0.92fr_1.08fr]">
      <div class="rounded-[1.75rem] border border-slate-700/50 bg-slate-900/60 p-5 shadow-sm md:p-6">
        <div class="mb-4 flex items-start justify-between gap-3">
          <div>
            <p class="mb-2 text-[10px] font-bold uppercase tracking-[0.18em] text-amber-500/90">Ops</p>
            <h3 class="font-serif text-[1.15rem] font-bold tracking-tight text-slate-50">Trạng thái dịch vụ</h3>
            <p class="mt-2 text-sm text-slate-500">Backend, email, AI và snapshot.</p>
          </div>
          <span class="rounded-full bg-slate-800 px-3 py-1 text-xs font-semibold text-slate-300">
            {{ opsGeneratedLabel }}
          </span>
        </div>

        <div class="space-y-3">
          <article
            v-for="check in healthChecks"
            :key="check.id"
            class="rounded-2xl border px-4 py-3"
            :class="healthCheckCardClass(check.status)"
          >
            <div class="flex items-start justify-between gap-3">
              <div>
                <p class="text-sm font-semibold text-slate-100">{{ check.label }}</p>
                <p class="mt-1 text-xs text-slate-500">{{ check.detail }}</p>
              </div>
              <span class="rounded-full px-2.5 py-1 text-[11px] font-bold uppercase tracking-wider" :class="healthCheckBadgeClass(check.status)">
                {{ check.status }}
              </span>
            </div>
          </article>
        </div>

        <div class="mt-4 rounded-2xl border border-slate-700/40 bg-[#131c2e]/80 p-4">
          <p class="text-sm font-bold text-slate-100">Ops snapshot</p>
          <ul class="mt-3 space-y-2 text-sm text-slate-400">
            <li v-for="item in opsActivityFeed" :key="item.id" class="flex gap-2">
              <span class="material-symbols-outlined text-base text-amber-500/80">chevron_right</span>
              {{ item.text }}
            </li>
          </ul>
        </div>
      </div>

      <div class="rounded-[1.75rem] border border-slate-700/50 bg-slate-900/60 p-5 shadow-sm md:p-6">
        <div class="mb-4 flex items-start justify-between gap-3">
          <div>
            <p class="mb-2 text-[10px] font-bold uppercase tracking-[0.18em] text-amber-500/90">Hàng chờ</p>
            <h3 class="font-serif text-[1.15rem] font-bold tracking-tight text-slate-50">Xử lý & bulk</h3>
            <p class="mt-2 text-sm text-slate-500">Các queue cần xem xét.</p>
          </div>
          <span class="rounded-full bg-amber-900/30 px-3 py-1 text-xs font-semibold text-amber-200">
            {{ moderationQueueTotal }} mục
          </span>
        </div>

        <div class="grid gap-3 lg:grid-cols-[0.95fr_1.05fr]">
          <div class="space-y-3">
            <RouterLink
              v-for="item in moderationQueue"
              :key="item.id"
              :to="item.route"
              class="block rounded-2xl border px-4 py-3 transition-colors"
              :class="queueCardClass(item.severity)"
            >
              <div class="flex items-start justify-between gap-3">
                <div>
                  <p class="text-sm font-semibold text-slate-100">{{ item.label }}</p>
                  <p class="mt-1 text-xs text-slate-500">Mở đúng màn để xử lý.</p>
                </div>
                <span class="text-2xl font-black tabular-nums">{{ formatNum(item.count) }}</span>
              </div>
            </RouterLink>
          </div>

          <div class="rounded-[1.5rem] border border-slate-700/40 bg-[#131c2e]/80 p-4">
            <p class="text-sm font-bold text-slate-100">Bulk actions</p>
            <div class="mt-3 space-y-3">
              <RouterLink
                v-for="action in bulkActions"
                :key="action.id"
                :to="action.route"
                class="flex items-start gap-3 rounded-2xl border border-slate-700/40 bg-slate-900/50 px-4 py-3 transition-colors hover:border-amber-600/25"
              >
                <span class="material-symbols-outlined rounded-xl bg-amber-900/30 p-2 text-amber-400">{{ action.icon }}</span>
                <div>
                  <p class="text-sm font-semibold text-slate-100">{{ action.label }}</p>
                  <p class="mt-1 text-xs text-slate-500">{{ action.description }}</p>
                </div>
              </RouterLink>
            </div>
          </div>
        </div>
      </div>
    </section>

    <section class="stitch-chart-card min-h-0 border border-slate-700/50 bg-slate-900/40">
      <div class="stitch-chart-card__head">
        <p class="mb-2 text-[10px] font-bold uppercase tracking-[0.18em] text-amber-500/90">Vai trò</p>
        <h3 class="font-serif text-[1.15rem] font-bold tracking-tight text-slate-50">Phân bổ người dùng</h3>
        <p class="mt-2 text-sm text-slate-500">
          Học sinh, giáo viên và admin.
        </p>
        <p v-if="errorMsg" class="mt-4 text-sm font-medium text-rose-500">{{ errorMsg }}</p>
      </div>
      <div class="stitch-chart-card__body">
        <div class="stitch-chart-well flex min-h-0 flex-col overflow-hidden">
          <div ref="pieChartRef" class="h-[260px] w-full max-w-full shrink-0 overflow-hidden lg:h-[280px]" />
        </div>
      </div>
    </section>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { fetchAdminDashboardStats, fetchAdminOpsSummary } from '../../services/adminService'
import { createRafThrottle } from '../../utils/chartResizeThrottle'
const quickLinks = [
  { to: '/admin/students', label: 'Học sinh', hint: 'Tìm, xem chi tiết, xóa tài khoản', icon: 'school' },
  { to: '/admin/teachers', label: 'Giáo viên', hint: 'Danh sách & thao tác quản trị', icon: 'co_present' },
  { to: '/admin/admins', label: 'Admin', hint: 'Tài khoản quản trị nội bộ', icon: 'admin_panel_settings' },
  { to: '/admin/exams', label: 'Đề thi', hint: 'Bật/tắt đề, thời gian & thống kê', icon: 'quiz' }
]
const loading = ref(true)
const errorMsg = ref('')
const stats = ref(null)
const opsSummary = ref(null)

const lineChartRef = ref(null)
const pieChartRef = ref(null)
let lineChart = null
let pieChart = null
let echartsLib = null

const kpiCards = computed(() => {
  const s = stats.value
  if (!s) {
    return [
      { label: 'Người dùng', value: 0, icon: 'group' },
      { label: 'Đề thi', value: 0, icon: 'quiz' },
      { label: 'Câu hỏi', value: 0, icon: 'help' },
      { label: 'Lượt thi', value: 0, icon: 'assignment_turned_in' }
    ]
  }
  return [
    { label: 'Người dùng', value: s.totalUsers, icon: 'group' },
    { label: 'Đề thi', value: s.totalExams, icon: 'quiz' },
    { label: 'Câu hỏi', value: s.totalQuestions, icon: 'help' },
    { label: 'Lượt thi', value: s.totalAttempts, icon: 'assignment_turned_in' }
  ]
})

const kpiCardsStyled = computed(() => {
  const accent = [
    { borderClass: 'border-l-[#8d4b00]', iconWrapClass: 'bg-[#8d4b00]/15 text-amber-500' },
    { borderClass: 'border-l-amber-600', iconWrapClass: 'bg-amber-600/10 text-amber-500' },
    { borderClass: 'border-l-cyan-600', iconWrapClass: 'bg-cyan-600/10 text-cyan-400' },
    { borderClass: 'border-l-emerald-500', iconWrapClass: 'bg-emerald-500/10 text-emerald-400' }
  ]
  return kpiCards.value.map((card, i) => ({ ...card, ...accent[i] }))
})

const healthChecks = computed(() => opsSummary.value?.healthChecks || [])
const moderationQueue = computed(() => opsSummary.value?.moderationQueue || [])
const bulkActions = computed(() => opsSummary.value?.bulkActions || [])
const moderationQueueTotal = computed(() => Number(opsSummary.value?.moderationQueueSize || 0))
const opsGeneratedLabel = computed(() => {
  const generatedAt = opsSummary.value?.generatedAt
  if (!generatedAt) return 'Chưa cập nhật'
  return new Date(generatedAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
})

const opsActivityFeed = computed(() => {
  if (!opsSummary.value) {
    return [
      { id: 'pending', text: 'Đang chờ dữ liệu vận hành từ backend.' }
    ]
  }

  return [
    { id: 'queue', text: `Tổng moderation queue hiện tại: ${moderationQueueTotal.value} mục.` },
    { id: 'health', text: `${healthChecks.value.filter((item) => ['ATTENTION', 'DOWN', 'DISABLED'].includes(item.status)).length} dịch vụ đang cần theo dõi sát hơn.` },
    { id: 'imports', text: `Queue import và trạng thái đề thi được gom về cùng một hub để xử lý nhanh.` }
  ]
})

const formatNum = (n) => (n == null ? '—' : new Intl.NumberFormat('vi-VN').format(n))
const healthCheckBadgeClass = (status) => {
  if (status === 'UP' || status === 'READY') return 'bg-emerald-900/50 text-emerald-300'
  if (status === 'ATTENTION') return 'bg-amber-900/40 text-amber-200'
  if (status === 'DOWN') return 'bg-rose-900/40 text-rose-300'
  return 'bg-slate-800 text-slate-400'
}

const healthCheckCardClass = (status) => {
  if (status === 'UP' || status === 'READY') return 'border-emerald-800/40 bg-emerald-950/25'
  if (status === 'ATTENTION') return 'border-amber-800/40 bg-amber-950/20'
  if (status === 'DOWN') return 'border-rose-800/40 bg-rose-950/25'
  return 'border-slate-700/50 bg-slate-800/40'
}

const queueCardClass = (severity) => {
  if (severity === 'HIGH') return 'border-rose-800/40 bg-rose-950/20 hover:border-rose-600/40'
  if (severity === 'MEDIUM') return 'border-amber-800/40 bg-amber-950/15 hover:border-amber-600/40'
  return 'border-slate-700/50 bg-slate-800/40 hover:border-amber-600/25'
}

const disposeCharts = () => {
  lineChart?.dispose()
  pieChart?.dispose()
  lineChart = null
  pieChart = null
}

const buildLineOption = (echarts, series) => ({
  backgroundColor: 'transparent',
  tooltip: {
    trigger: 'axis',
    backgroundColor: 'rgba(15, 23, 42, 0.92)',
    borderColor: 'rgba(177, 95, 0, 0.4)',
    textStyle: { color: '#e2e8f0' }
  },
  grid: { left: '3%', right: '4%', bottom: '3%', top: '12%', containLabel: true },
  xAxis: {
    type: 'category',
    data: series.map((d) => d.date),
    axisLine: { lineStyle: { color: 'rgba(148, 163, 184, 0.25)' } },
    axisLabel: { color: '#94a3b8', fontSize: 11 }
  },
  yAxis: {
    type: 'value',
    splitLine: { lineStyle: { color: 'rgba(148, 163, 184, 0.12)' } },
    axisLabel: { color: '#94a3b8', fontSize: 11 }
  },
  series: [
    {
      name: 'Attempts',
      type: 'line',
      smooth: true,
      symbol: 'circle',
      symbolSize: 8,
      lineStyle: { width: 3, color: '#2dd4bf' },
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(45, 212, 191, 0.35)' },
          { offset: 1, color: 'rgba(45, 212, 191, 0)' }
        ])
      },
      data: series.map((d) => d.count)
    }
  ]
})

const buildPieOption = (s) => ({
  backgroundColor: 'transparent',
  tooltip: { trigger: 'item', backgroundColor: 'rgba(15, 23, 42, 0.92)', borderColor: 'rgba(177, 95, 0, 0.4)' },
  legend: { bottom: 0, textStyle: { color: '#94a3b8' } },
  series: [
    {
      type: 'pie',
      radius: ['42%', '68%'],
      center: ['50%', '46%'],
      avoidLabelOverlap: true,
      itemStyle: { borderRadius: 8, borderColor: '#0f172a', borderWidth: 2 },
      label: { color: '#cbd5e1' },
      data: [
        { value: s.totalTeachers, name: 'Giáo viên', itemStyle: { color: '#b15f00' } },
        { value: s.totalStudents, name: 'Học sinh', itemStyle: { color: '#38bdf8' } },
        { value: s.totalAdmins, name: 'Admin', itemStyle: { color: '#fbbf24' } }
      ]
    }
  ]
})

const renderCharts = async () => {
  await nextTick()
  if (!stats.value) return
  if (!echartsLib) {
    echartsLib = await import('echarts')
  }
  const echarts = echartsLib.default || echartsLib

  const series = stats.value.attemptsLast7Days || []
  if (lineChartRef.value) {
    if (!lineChart) lineChart = echarts.init(lineChartRef.value, null, { renderer: 'canvas' })
    lineChart.setOption(buildLineOption(echarts, series))
  }
  if (pieChartRef.value) {
    if (!pieChart) pieChart = echarts.init(pieChartRef.value, null, { renderer: 'canvas' })
    pieChart.setOption(buildPieOption(stats.value))
  }
}

const onResize = createRafThrottle(() => {
  lineChart?.resize()
  pieChart?.resize()
})

const loadStats = async () => {
  loading.value = true
  errorMsg.value = ''
  try {
    const [dashboardStats, ops] = await Promise.all([
      fetchAdminDashboardStats(),
      fetchAdminOpsSummary()
    ])
    stats.value = dashboardStats
    opsSummary.value = ops
    await renderCharts()
  } catch (e) {
    errorMsg.value = e?.payload?.message || e?.message || 'Không tải được dữ liệu.'
    stats.value = null
    opsSummary.value = null
    disposeCharts()
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  await loadStats()
  window.addEventListener('resize', onResize, { passive: true })
  await nextTick()
  onResize()
})

onUnmounted(() => {
  window.removeEventListener('resize', onResize)
  disposeCharts()
})
</script>

