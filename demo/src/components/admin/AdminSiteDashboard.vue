<template>
  <div class="tui-page-wrap">

    <!-- Page Header -->
    <header class="tui-header">
      <div class="tui-header-left">
        <div class="tui-header-icon">
          <LucideIcon name="shield_check" :size="22" />
        </div>
        <div>
          <p class="tui-header-eyebrow">EduExam Quản trị</p>
          <h1 class="tui-header-title">Tổng quan hệ thống</h1>
          <p class="tui-header-desc">Theo dõi người dùng, đề thi và hoạt động của hệ thống.</p>
        </div>
      </div>
      <div class="tui-header-right">
        <button type="button" class="tui-btn tui-btn--secondary" :disabled="loading" @click="loadStats">
          <LucideIcon name="refresh" :size="14" :class="loading ? 'tui-spin' : ''" />
          Làm mới
        </button>
      </div>
    </header>

    <!-- Error Alert -->
    <div v-if="errorMsg" class="tui-alert tui-alert--danger" style="margin-top: 1rem">
      <LucideIcon name="alert_circle" :size="16" />
      <span>{{ errorMsg }}</span>
      <button type="button" @click="errorMsg = ''"><LucideIcon name="x" :size="14" /></button>
    </div>

    <!-- Stat Strip — KPI cards with functional icons -->
    <div class="tui-stat-grid" style="margin: 1rem 1.5rem 0">
      <div
        v-for="(stat, i) in statItems"
        :key="stat.label"
        class="tui-kpi-card"
        :class="`tui-kpi-card--${stat.color}`"
        :style="{ animationDelay: `${i * 0.06}s` }"
      >
        <div class="tui-kpi-icon-wrap">
          <LucideIcon :name="stat.icon" :size="20" />
        </div>
        <div class="tui-kpi-body">
          <p class="tui-kpi-value">{{ formatNum(stat.value) }}</p>
          <p class="tui-kpi-label">{{ stat.label }}</p>
        </div>
        <div v-if="stat.trend != null" class="tui-kpi-trend" :class="stat.trend >= 0 ? 'tui-kpi-trend--up' : 'tui-kpi-trend--down'">
          <LucideIcon :name="stat.trend >= 0 ? 'trending_up' : 'trending_down'" :size="12" />
          <span>{{ stat.trend >= 0 ? '+' : '' }}{{ stat.trend }}%</span>
        </div>
      </div>
    </div>

    <!-- Main content: panels row -->
    <div style="flex: 1; display: flex; flex-direction: column; padding: 1rem 1.5rem 1.5rem; gap: 1rem; min-height: 0; overflow-y: auto">

      <!-- Row 1: So sanh KPI (bar ngang) + Radar tong quan -->
      <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 1rem">

        <div class="tui-panel tui-panel--anim" style="display: flex; flex-direction: column">
          <div class="tui-panel-header">
            <div>
              <h3 class="tui-panel-title">So sánh quy mô hệ thống</h3>
              <p class="tui-panel-meta">Theo chỉ số chính — {{ chartMetaTime }}</p>
            </div>
            <span class="tui-status tui-status--trend">
              <LucideIcon name="bar_chart_2" :size="12" />
              KPI
            </span>
          </div>
          <div style="position: relative; flex: 1; min-height: 240px; display: flex; align-items: center; justify-content: center; padding: 0.75rem">
            <div ref="kpiHBarChartRef" style="width: 100%; height: 100%; min-height: 220px" />
            <div v-if="loading && !stats" class="tui-skeleton" style="position: absolute; inset: 0.75rem; border-radius: 12px" />
          </div>
        </div>

        <div class="tui-panel tui-panel--anim" style="display: flex; flex-direction: column">
          <div class="tui-panel-header">
            <div>
              <h3 class="tui-panel-title">Tổng quan đa chiều</h3>
              <p class="tui-panel-meta">Người dùng, đề thi, câu hỏi, lượt thi</p>
            </div>
            <span class="tui-status tui-status--trend">
              <LucideIcon name="radar" :size="12" />
              Radar
            </span>
          </div>
          <div style="position: relative; flex: 1; min-height: 240px; display: flex; align-items: center; justify-content: center; padding: 0.75rem">
            <div ref="radarChartRef" style="width: 100%; height: 100%; min-height: 220px" />
            <div v-if="loading && !stats" class="tui-skeleton" style="position: absolute; inset: 0.75rem; border-radius: 12px" />
          </div>
        </div>
      </div>

      <!-- Row 2: Charts (3 columns) -->
      <div style="display: grid; grid-template-columns: 280px 1fr 1fr; gap: 1rem">

        <!-- Pie: Phan bo nguoi dung -->
        <div class="tui-panel tui-panel--anim" style="display: flex; flex-direction: column">
          <div class="tui-panel-header">
            <div>
              <h3 class="tui-panel-title">Phân bổ người dùng</h3>
              <p class="tui-panel-meta">Theo vai trò</p>
            </div>
          </div>
          <div style="position: relative; flex: 1; min-height: 200px; display: flex; align-items: center; justify-content: center; padding: 0.75rem">
            <div ref="pieChartRef" style="width: 100%; height: 100%; min-height: 200px" />
            <div v-if="loading && !stats" class="tui-skeleton" style="position: absolute; inset: 0.75rem; border-radius: 12px" />
          </div>
          <div style="padding: 0.5rem 1.25rem 1rem; display: flex; flex-direction: column; gap: 0.25rem">
            <div v-for="(item, i) in userDistLegend" :key="i" class="tui-metric-row" style="padding: 0.25rem 0; border: none">
              <div class="tui-metric-label-row">
                <span class="tui-metric-label" style="display: flex; align-items: center; gap: 0.4rem">
                  <span :style="{ width: 8, height: 8, borderRadius: 2, background: item.color, display: 'inline-block', flexShrink: 0 }" />
                  {{ item.label }}
                </span>
                <span class="tui-metric-value">{{ item.value }}</span>
              </div>
              <div class="tui-metric-bar">
                <div class="tui-metric-fill" :class="`tui-metric-fill--${item.metricColor}`" :style="{ width: item.pct + '%' }" />
              </div>
            </div>
          </div>
        </div>

        <!-- Bar: So luot thi theo ngay -->
        <div class="tui-panel tui-panel--anim" style="display: flex; flex-direction: column">
          <div class="tui-panel-header">
            <div>
              <h3 class="tui-panel-title">Số lượt thi 7 ngày gần</h3>
              <p class="tui-panel-meta">{{ attemptTotal }} lượt thi — Đỉnh {{ attemptPeak }}</p>
            </div>
            <span class="tui-status tui-status--trend">
              <LucideIcon name="trending_up" :size="12" />
              {{ attemptTrend }}
            </span>
          </div>
          <div style="position: relative; flex: 1; min-height: 200px; display: flex; align-items: center; justify-content: center; padding: 0.75rem">
            <div ref="barChartRef" style="width: 100%; height: 100%; min-height: 200px" />
            <div v-if="loading && !stats" class="tui-skeleton" style="position: absolute; inset: 0.75rem; border-radius: 12px" />
          </div>
        </div>

        <!-- Line: Xu huong luot thi -->
        <div class="tui-panel tui-panel--anim" style="display: flex; flex-direction: column">
          <div class="tui-panel-header">
            <div>
              <h3 class="tui-panel-title">Xu hướng lượt thi</h3>
              <p class="tui-panel-meta">Theo thời gian</p>
            </div>
            <span class="tui-status tui-status--trend">
              <LucideIcon name="activity" :size="12" />
              {{ attemptTrend }}
            </span>
          </div>
          <div style="position: relative; flex: 1; min-height: 200px; display: flex; align-items: center; justify-content: center; padding: 0.75rem">
            <div ref="lineChartRef" style="width: 100%; height: 100%; min-height: 200px" />
            <div v-if="loading && !stats" class="tui-skeleton" style="position: absolute; inset: 0.75rem; border-radius: 12px" />
          </div>
        </div>
      </div>

    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { fetchAdminDashboardStats } from '../../services/adminService'
import LucideIcon from '../common/LucideIcon.vue'

const loading = ref(true)
const errorMsg = ref('')
const stats = ref(null)
const chartMetaTime = ref('—')

const lineChartRef = ref(null)
const barChartRef = ref(null)
const pieChartRef = ref(null)
const kpiHBarChartRef = ref(null)
const radarChartRef = ref(null)
let lineChart = null
let barChart = null
let pieChart = null
let kpiHBarChart = null
let radarChart = null
let echartsLib = null

const formatNum = (n) => (n == null ? '0' : new Intl.NumberFormat('vi-VN').format(n))

const statItems = computed(() => {
  const s = stats.value || {}
  return [
    { label: 'Tổng người dùng', value: s.totalUsers || 0, color: 'primary', icon: 'users', trend: null },
    { label: 'Đề thi',           value: s.totalExams || 0, color: 'warning', icon: 'clipboard_list', trend: null },
    { label: 'Câu hỏi',         value: s.totalQuestions || 0, color: 'info', icon: 'message_circle', trend: null },
    { label: 'Lượt thi',         value: s.totalAttempts || 0, color: 'success', icon: 'file_check_2', trend: null }
  ]
})

const attemptTotal = computed(() => {
  const days = (stats.value?.attemptsLast7Days || []).reduce((s, d) => s + (d.count || 0), 0)
  return formatNum(days)
})

const attemptPeak = computed(() => {
  const days = stats.value?.attemptsLast7Days || []
  if (!days.length) return 0
  return formatNum(Math.max(...days.map(d => d.count || 0)))
})

const attemptTrend = computed(() => {
  const days = stats.value?.attemptsLast7Days || []
  if (days.length < 2) return '—'
  const recent = days.slice(-3).reduce((s, d) => s + (d.count || 0), 0)
  const older  = days.slice(0, 3).reduce((s, d) => s + (d.count || 0), 0)
  if (!older) return '—'
  const pct = Math.round(((recent - older) / older) * 100)
  return (pct >= 0 ? '+' : '') + pct + '%'
})

const userDistLegend = computed(() => {
  const s = stats.value || {}
  const total = (s.totalStudents || 0) + (s.totalTeachers || 0) + (s.totalAdmins || 0) || 1
  return [
    { label: 'Học sinh',  value: formatNum(s.totalStudents || 0), pct: Math.round(((s.totalStudents || 0) / total) * 100), color: '#4f46e5', metricColor: 'primary' },
    { label: 'Giáo viên', value: formatNum(s.totalTeachers || 0), pct: Math.round(((s.totalTeachers || 0) / total) * 100), color: '#f59e0b', metricColor: 'warning' },
    { label: 'Quản trị',  value: formatNum(s.totalAdmins || 0),  pct: Math.round(((s.totalAdmins || 0) / total) * 100), color: '#10b981', metricColor: 'success' }
  ]
})

const getEcharts = async () => {
  if (!echartsLib) {
    echartsLib = await import('echarts')
  }
  return echartsLib.default || echartsLib
}

const baseTooltip = () => ({
  trigger: 'axis',
  backgroundColor: 'rgba(255,255,255,0.97)',
  borderColor: 'rgba(79,70,229,0.25)',
  borderWidth: 1,
  borderRadius: 12,
  padding: [10, 14],
  textStyle: { color: '#0f172a', fontWeight: 600, fontSize: 12 },
  extraCssText: 'box-shadow: 0 8px 24px rgba(15,23,42,0.12);'
})

const buildLineOption = (echarts, series) => ({
  backgroundColor: 'transparent',
  tooltip: { ...baseTooltip(), trigger: 'axis' },
  grid: { left: '3%', right: '4%', bottom: '10%', top: '10%', containLabel: true },
  xAxis: {
    type: 'category',
    data: series.map(d => d.date?.slice(5, 10) || ''),
    axisLine: { lineStyle: { color: 'rgba(148,163,184,0.2)', width: 1 } },
    axisLabel: { color: '#64748b', fontSize: 11, fontWeight: 600, fontFamily: "'Be Vietnam Pro', sans-serif" },
    axisTick: { show: false }
  },
  yAxis: {
    type: 'value',
    splitLine: { lineStyle: { color: 'rgba(148,163,184,0.08)', type: 'dashed' } },
    axisLabel: { color: '#64748b', fontSize: 11, fontWeight: 600, fontFamily: "'Be Vietnam Pro', sans-serif" }
  },
  series: [{
    name: 'Lượt thi',
    type: 'line',
    smooth: 0.5,
    symbol: 'circle',
    symbolSize: 8,
    animationDuration: 1800,
    animationEasing: 'cubicOut',
    lineStyle: { width: 3, color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
      { offset: 0, color: '#6366f1' },
      { offset: 1, color: '#4f46e5' }
    ]) },
    itemStyle: { color: '#4f46e5', borderWidth: 2, borderColor: '#fff' },
    emphasis: { scale: true, scaleSize: 12, itemStyle: { shadowBlur: 20, shadowColor: 'rgba(79,70,229,0.5)' } },
    areaStyle: {
      color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
        { offset: 0, color: 'rgba(79,70,229,0.2)' },
        { offset: 1, color: 'rgba(79,70,229,0.01)' }
      ])
    },
    data: series.map(d => d.count)
  }]
})

const buildBarOption = (echarts, series) => ({
  backgroundColor: 'transparent',
  tooltip: { ...baseTooltip(), trigger: 'axis' },
  grid: { left: '3%', right: '4%', bottom: '10%', top: '10%', containLabel: true },
  xAxis: {
    type: 'category',
    data: series.map(d => d.date?.slice(5, 10) || ''),
    axisLine: { lineStyle: { color: 'rgba(148,163,184,0.2)', width: 1 } },
    axisLabel: { color: '#64748b', fontSize: 11, fontWeight: 600, fontFamily: "'Be Vietnam Pro', sans-serif" },
    axisTick: { show: false }
  },
  yAxis: {
    type: 'value',
    splitLine: { lineStyle: { color: 'rgba(148,163,184,0.08)', type: 'dashed' } },
    axisLabel: { color: '#64748b', fontSize: 11, fontWeight: 600, fontFamily: "'Be Vietnam Pro', sans-serif" }
  },
  series: [{
    name: 'Lượt thi',
    type: 'bar',
    barMaxWidth: 36,
    animationDuration: 1400,
    animationDelay: (idx) => idx * 80,
    animationEasing: 'elasticOut',
    itemStyle: {
      color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
        { offset: 0, color: '#818cf8' },
        { offset: 1, color: '#4f46e5' }
      ]),
      borderRadius: [6, 6, 0, 0]
    },
    emphasis: {
      itemStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: '#a5b4fc' },
          { offset: 1, color: '#6366f1' }
        ])
      }
    },
    data: series.map(d => d.count)
  }]
})

const buildPieOption = (echarts, s) => {
  const total = (s.totalStudents || 0) + (s.totalTeachers || 0) + (s.totalAdmins || 0) || 1
  const data = [
    { name: 'Học sinh',  value: s.totalStudents || 0, color: '#4f46e5' },
    { name: 'Giáo viên', value: s.totalTeachers || 0, color: '#f59e0b' },
    { name: 'Quản trị',   value: s.totalAdmins || 0,  color: '#10b981' }
  ]
  return {
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'item',
      backgroundColor: 'rgba(255,255,255,0.97)',
      borderColor: 'rgba(79,70,229,0.25)',
      borderWidth: 1,
      borderRadius: 12,
      padding: [10, 14],
      textStyle: { color: '#0f172a', fontWeight: 600, fontSize: 12 },
      formatter: '{b}: {c} ({d}%)'
    },
    legend: { show: false },
    graphic: [{
      type: 'text', left: 'center', top: '32%',
      style: { text: formatNum(total), textAlign: 'center', fill: '#0f172a', fontSize: 26, fontWeight: 900, fontFamily: "'Be Vietnam Pro', sans-serif" }
    }, {
      type: 'text', left: 'center', top: '48%',
      style: { text: 'Tổng', textAlign: 'center', fill: '#64748b', fontSize: 11, fontWeight: 600, fontFamily: "'Be Vietnam Pro', sans-serif" }
    }],
    series: [{
      type: 'pie',
      radius: ['48%', '72%'],
      center: ['50%', '50%'],
      padAngle: 3,
      itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 3 },
      label: { show: false },
      labelLine: { show: false },
      animationType: 'expansion',
      animationDuration: 1200,
      animationEasing: 'cubicOut',
      data: data.map(d => ({
        name: d.name,
        value: d.value,
        itemStyle: { color: d.color }
      }))
    }]
  }
}

const buildKpiHorizontalBarOption = (echarts, s) => {
  const names = ['Tổng người dùng', 'Đề thi', 'Câu hỏi', 'Lượt thi']
  const values = [s.totalUsers || 0, s.totalExams || 0, s.totalQuestions || 0, s.totalAttempts || 0]
  return {
    backgroundColor: 'transparent',
    tooltip: { ...baseTooltip(), trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: '3%', right: '14%', bottom: '3%', top: '3%', containLabel: true },
    xAxis: {
      type: 'value',
      splitLine: { lineStyle: { color: 'rgba(148,163,184,0.08)', type: 'dashed' } },
      axisLabel: { color: '#64748b', fontSize: 11, fontWeight: 600, fontFamily: "'Be Vietnam Pro', sans-serif" }
    },
    yAxis: {
      type: 'category',
      data: names,
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: { color: '#64748b', fontSize: 11, fontWeight: 600, fontFamily: "'Be Vietnam Pro', sans-serif" }
    },
    series: [{
      name: 'Số lượng',
      type: 'bar',
      barMaxWidth: 28,
      data: values,
      itemStyle: {
        borderRadius: [0, 8, 8, 0],
        color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
          { offset: 0, color: '#818cf8' },
          { offset: 1, color: '#4f46e5' }
        ])
      },
      label: { show: true, position: 'right', color: '#475569', fontWeight: 700, fontSize: 11 }
    }]
  }
}

const buildRadarStatsOption = (echarts, s) => {
  const vals = [
    s.totalUsers || 0,
    s.totalExams || 0,
    s.totalQuestions || 0,
    s.totalAttempts || 0
  ]
  const maxBase = Math.max(...vals, 1)
  const max = maxBase < 10 ? 10 : Math.ceil(maxBase * 1.15)
  const indicators = [
    { name: 'Người dùng', max },
    { name: 'Đề thi', max },
    { name: 'Câu hỏi', max },
    { name: 'Lượt thi', max }
  ]
  return {
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'item',
      backgroundColor: 'rgba(255,255,255,0.97)',
      borderColor: 'rgba(79,70,229,0.25)',
      borderWidth: 1,
      borderRadius: 12,
      padding: [10, 14],
      textStyle: { color: '#0f172a', fontWeight: 600, fontSize: 12 }
    },
    radar: {
      indicator: indicators,
      splitNumber: 5,
      axisName: { color: '#64748b', fontSize: 11, fontWeight: 600 },
      splitArea: { areaStyle: { color: ['rgba(79,70,229,0.06)', 'rgba(79,70,229,0.02)'] } },
      splitLine: { lineStyle: { color: 'rgba(148,163,184,0.2)' } },
      axisLine: { lineStyle: { color: 'rgba(148,163,184,0.25)' } }
    },
    series: [{
      type: 'radar',
      symbol: 'circle',
      symbolSize: 6,
      animationDuration: 1400,
      data: [{
        value: vals,
        name: 'Hệ thống',
        areaStyle: { color: 'rgba(79,70,229,0.2)' },
        lineStyle: { width: 2, color: '#4f46e5' },
        itemStyle: { color: '#4f46e5' }
      }]
    }]
  }
}

const disposeAll = () => {
  lineChart?.dispose(); barChart?.dispose(); pieChart?.dispose()
  kpiHBarChart?.dispose(); radarChart?.dispose()
  lineChart = null; barChart = null; pieChart = null
  kpiHBarChart = null; radarChart = null
}

const renderCharts = async () => {
  await nextTick()
  if (!stats.value) return
  const echarts = await getEcharts()
  const series = stats.value.attemptsLast7Days || []
  const s = stats.value

  if (lineChartRef.value) {
    if (!lineChart) lineChart = echarts.init(lineChartRef.value)
    lineChart.setOption(buildLineOption(echarts, series))
  }
  if (barChartRef.value) {
    if (!barChart) barChart = echarts.init(barChartRef.value)
    barChart.setOption(buildBarOption(echarts, series))
  }
  if (pieChartRef.value) {
    if (!pieChart) pieChart = echarts.init(pieChartRef.value)
    pieChart.setOption(buildPieOption(echarts, s))
  }
  if (kpiHBarChartRef.value) {
    if (!kpiHBarChart) kpiHBarChart = echarts.init(kpiHBarChartRef.value)
    kpiHBarChart.setOption(buildKpiHorizontalBarOption(echarts, s))
  }
  if (radarChartRef.value) {
    if (!radarChart) radarChart = echarts.init(radarChartRef.value)
    radarChart.setOption(buildRadarStatsOption(echarts, s))
  }
}

const onResize = () => {
  lineChart?.resize(); barChart?.resize(); pieChart?.resize()
  kpiHBarChart?.resize(); radarChart?.resize()
}

const loadStats = async () => {
  loading.value = true
  errorMsg.value = ''
  try {
    const dashboardStats = await fetchAdminDashboardStats()
    stats.value = dashboardStats
    chartMetaTime.value = new Date().toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })
    await renderCharts()
  } catch (e) {
    errorMsg.value = e?.payload?.message || e?.message || 'Không tải được dữ liệu.'
    stats.value = null
    chartMetaTime.value = '—'
    disposeAll()
  } finally {
    loading.value = false
  }
}

onMounted(async () => { await loadStats(); window.addEventListener('resize', onResize, { passive: true }) })
onUnmounted(() => { window.removeEventListener('resize', onResize); disposeAll() })
</script>
