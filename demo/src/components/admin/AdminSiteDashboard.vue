<template>
  <div>
    <header class="flex flex-col sm:flex-row sm:items-end sm:justify-between gap-6 mb-10">
      <div>
        <p class="text-teal-400/90 text-xs font-semibold tracking-[0.25em] uppercase mb-2">Overview</p>
        <h2 class="text-2xl sm:text-3xl font-black tracking-tight bg-gradient-to-r from-white via-slate-100 to-slate-400 bg-clip-text text-transparent">
          Site overview
        </h2>
        <p class="mt-2 text-slate-500 text-sm max-w-xl">
          Bảng điều khiển tổng quan hệ thống — tải thực, không mẫu demo.
        </p>
      </div>
      <button
        type="button"
        @click="loadStats"
        :disabled="loading"
        class="inline-flex items-center gap-2 px-4 py-2.5 rounded-xl border border-white/10 bg-white/5 hover:bg-white/10 text-sm font-semibold transition-colors disabled:opacity-50 self-start sm:self-auto"
      >
        <span class="material-symbols-outlined text-lg" :class="{ 'animate-spin': loading }">refresh</span>
        Làm mới
      </button>
    </header>

    <section class="grid grid-cols-2 sm:grid-cols-4 gap-3 mb-8">
      <RouterLink
        v-for="q in quickLinks"
        :key="q.to"
        :to="q.to"
        class="group rounded-2xl border border-white/[0.08] bg-white/[0.03] p-4 hover:border-teal-500/30 hover:bg-white/[0.05] transition-colors flex flex-col gap-2"
      >
        <span class="material-symbols-outlined text-teal-500/80 text-2xl">{{ q.icon }}</span>
        <span class="text-sm font-semibold text-slate-200 group-hover:text-white">{{ q.label }}</span>
        <span class="text-[11px] text-slate-500 leading-snug">{{ q.hint }}</span>
      </RouterLink>
    </section>

    <!-- KPI bento -->
      <section class="grid grid-cols-2 lg:grid-cols-4 gap-3 sm:gap-4 mb-8">
        <article
          v-for="card in kpiCards"
          :key="card.label"
          class="group rounded-2xl border border-white/[0.08] bg-white/[0.03] backdrop-blur-sm p-4 sm:p-5 hover:border-teal-500/25 transition-colors"
        >
          <div class="flex items-start justify-between gap-2">
            <span class="text-slate-500 text-xs font-medium uppercase tracking-wide">{{ card.label }}</span>
            <span class="material-symbols-outlined text-teal-500/70 text-xl">{{ card.icon }}</span>
          </div>
          <p class="mt-3 font-mono text-2xl sm:text-3xl font-bold tabular-nums text-white tracking-tight">
            {{ formatNum(card.value) }}
          </p>
        </article>
      </section>

      <!-- Charts -->
      <section class="grid grid-cols-1 xl:grid-cols-3 gap-6">
        <div class="xl:col-span-2 rounded-2xl border border-white/[0.08] bg-white/[0.03] backdrop-blur-sm p-4 sm:p-6 min-h-[320px]">
          <div class="flex items-center justify-between mb-4">
            <h2 class="text-sm font-bold text-slate-300 uppercase tracking-wider">Lượt thi (7 ngày)</h2>
            <span class="text-xs text-slate-500">Theo ngày bắt đầu attempt</span>
          </div>
          <div ref="lineChartRef" class="w-full h-[260px]" />
        </div>
        <div class="rounded-2xl border border-white/[0.08] bg-white/[0.03] backdrop-blur-sm p-4 sm:p-6 min-h-[320px]">
          <h2 class="text-sm font-bold text-slate-300 uppercase tracking-wider mb-4">Phân bổ vai trò</h2>
          <div ref="pieChartRef" class="w-full h-[260px]" />
        </div>
      </section>

    <p v-if="errorMsg" class="mt-6 text-rose-400 text-sm font-medium">{{ errorMsg }}</p>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { fetchAdminDashboardStats } from '../../services/adminService'

const quickLinks = [
  { to: '/admin/students', label: 'Học sinh', hint: 'Danh sách tài khoản', icon: 'school' },
  { to: '/admin/teachers', label: 'Giáo viên', hint: 'Danh sách tài khoản', icon: 'co_present' },
  { to: '/admin/admins', label: 'Admin', hint: 'Tài khoản quản trị', icon: 'admin_panel_settings' },
  { to: '/admin/exams', label: 'Đề thi', hint: 'Toàn hệ thống', icon: 'quiz' }
]
const loading = ref(true)
const errorMsg = ref('')
const stats = ref(null)

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

const formatNum = (n) => (n == null ? '—' : new Intl.NumberFormat('vi-VN').format(n))

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
    borderColor: 'rgba(45, 212, 191, 0.35)',
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
  tooltip: { trigger: 'item', backgroundColor: 'rgba(15, 23, 42, 0.92)', borderColor: 'rgba(45, 212, 191, 0.35)' },
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
        { value: s.totalTeachers, name: 'Giáo viên', itemStyle: { color: '#2dd4bf' } },
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
    if (!lineChart) lineChart = echarts.init(lineChartRef.value)
    lineChart.setOption(buildLineOption(echarts, series))
  }
  if (pieChartRef.value) {
    if (!pieChart) pieChart = echarts.init(pieChartRef.value)
    pieChart.setOption(buildPieOption(stats.value))
  }
}

const onResize = () => {
  lineChart?.resize()
  pieChart?.resize()
}

const loadStats = async () => {
  loading.value = true
  errorMsg.value = ''
  try {
    stats.value = await fetchAdminDashboardStats()
    await renderCharts()
  } catch (e) {
    errorMsg.value = e?.payload?.message || e?.message || 'Không tải được dữ liệu.'
    stats.value = null
    disposeCharts()
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  await loadStats()
  window.addEventListener('resize', onResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', onResize)
  disposeCharts()
})
</script>

