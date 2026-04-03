<template>
  <div class="db-page-wrap">

    <!-- Page Header -->
    <header class="db-header">
      <div class="db-header-left">
        <div class="db-header-icon">
          <LucideIcon name="shield_check" :size="24" />
        </div>
        <div>
          <p class="db-eyebrow">Admin overview</p>
          <h1 class="db-title">Tong quan he thong</h1>
          <p class="db-subtitle">Theo doi nguoi dung, de thi, cau hoi va luot lam bai — mot man duy nhat.</p>
        </div>
      </div>
      <button
        type="button"
        class="db-btn db-btn--secondary"
        :disabled="loading"
        @click="loadStats"
      >
        <LucideIcon name="refresh" :size="16" :class="loading ? 'db-spin' : ''" />
        Lam moi
      </button>
    </header>

    <!-- Error Alert -->
    <div v-if="errorMsg" class="db-alert db-alert--danger">
      <LucideIcon name="alert_circle" :size="18" />
      <span>{{ errorMsg }}</span>
      <button type="button" @click="errorMsg = ''">
        <LucideIcon name="x" :size="16" />
      </button>
    </div>

    <!-- KPI Cards -->
    <div class="db-kpi-grid">
      <div
        v-for="(card, i) in kpiCards"
        :key="card.label"
        class="db-kpi-card"
        :class="`db-kpi-card--${card.variant}`"
        :style="{ animationDelay: `${0.1 + i * 0.1}s` }"
      >
        <div class="db-kpi-icon" :class="`db-kpi-icon--${card.variant}`">
          <LucideIcon :name="card.icon" :size="22" />
        </div>
        <div class="db-kpi-body">
          <p
            class="db-kpi-value"
            :class="`db-kpi-value--${card.variant}`"
            :ref="el => { if (el) kpiValueRefs[i] = el }"
          >{{ formatNum(card.value) }}</p>
          <p class="db-kpi-label">{{ card.label }}</p>
        </div>
        <div class="db-kpi-spark" :class="`db-kpi-spark--${card.variant}`" />
      </div>
    </div>

    <!-- Main Grid: Quick Nav + Moderation Queue -->
    <div class="db-grid-2">

      <!-- Quick Navigation -->
      <div class="db-section-card" style="animation-delay: 0.2s">
        <div class="db-section-header">
          <div>
            <h3 class="db-section-title">Dieu huong nhanh</h3>
            <p class="db-section-desc">Truy cap cac nhom quan tri</p>
          </div>
        </div>
        <div class="db-quick-nav-grid" style="padding: 0.875rem 1rem 1.25rem">
          <RouterLink
            v-for="(link, i) in quickLinks"
            :key="link.to"
            :to="link.to"
            class="db-quick-link"
            :style="{ animationDelay: `${0.3 + i * 0.08}s` }"
          >
            <div class="db-quick-link-icon">
              <LucideIcon :name="link.icon" :size="20" />
            </div>
            <div class="db-quick-link-body">
              <p class="db-quick-link-title">{{ link.label }}</p>
              <p class="db-quick-link-hint">{{ link.hint }}</p>
            </div>
            <LucideIcon name="chevron_right" :size="16" class="db-quick-link-arrow" />
          </RouterLink>
        </div>
      </div>

      <!-- Attempt Trend (7d) -->
      <div class="db-section-card db-section-card--chart" style="animation-delay: 0.25s">
        <div class="db-section-header">
          <div>
            <h3 class="db-section-title">Luot thi 7 ngay gan day</h3>
            <p class="db-section-desc">Bien dong so luong thi sinh theo ngay</p>
          </div>
          <span class="db-section-badge db-section-badge--accent">
            <LucideIcon name="trending_up" :size="12" />
            {{ attemptTrendTotal }} luot
          </span>
        </div>
        <div class="db-chart-wrap">
          <div ref="lineChartRef" class="db-chart" />
          <div v-if="loading && !stats" class="db-chart-skeleton">
            <div class="db-shimmer db-shimmer-wide" />
          </div>
        </div>
      </div>
    </div>

    <!-- Role Distribution + Service Status -->
    <div class="db-grid-2">

      <!-- Role Distribution Pie -->
      <div class="db-section-card" style="animation-delay: 0.3s">
        <div class="db-section-header">
          <div>
            <h3 class="db-section-title">Phan bo vai tro nguoi dung</h3>
            <p class="db-section-desc">Theo doi nhanh ty le hoc sinh, giao vien va admin</p>
          </div>
        </div>
        <div class="db-chart-wrap">
          <div ref="pieChartRef" class="db-chart" />
          <div v-if="loading && !stats" class="db-chart-skeleton">
            <div class="db-shimmer db-shimmer-circle" />
          </div>
        </div>
      </div>

      <!-- Service Status / Ops Snapshot -->
      <div class="db-section-card" style="animation-delay: 0.35s">
        <div class="db-section-header">
          <div>
            <h3 class="db-section-title">Trang thai dich vu</h3>
            <p class="db-section-desc">Backend, AI, co so du lieu va snapshot.</p>
          </div>
          <span class="db-section-badge db-section-badge--success">
            <LucideIcon name="activity" :size="12" />
            {{ opsLabel }}
          </span>
        </div>

        <div style="padding: 0 1rem 0.5rem">
          <div
            v-for="check in healthChecks"
            :key="check.id"
            class="db-service-item"
            :class="`db-service-item--${serviceStatus(check.status)}`"
            style="margin-bottom: 0.5rem"
          >
            <div class="db-service-info">
              <div class="db-service-icon" :class="`db-service-icon--${serviceStatus(check.status)}`">
                <LucideIcon :name="serviceIcon(check.status)" :size="16" />
              </div>
              <div>
                <p class="db-service-name">{{ check.label }}</p>
                <p class="db-service-detail">{{ check.detail }}</p>
              </div>
            </div>
            <span class="db-service-badge" :class="`db-service-badge--${serviceStatus(check.status)}`">
              <span class="db-dot" :class="`db-dot--${serviceStatus(check.status)}`" />
              {{ check.status }}
            </span>
          </div>

          <div v-if="!healthChecks.length && !loading" class="db-empty">
            <div class="db-empty-icon">
              <LucideIcon name="server" :size="24" />
            </div>
            <p class="db-empty-title">Khong co du lieu</p>
            <p class="db-empty-desc">Dang cho backend tra ve thong tin he thong.</p>
          </div>
        </div>
      </div>
    </div>

  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { RouterLink } from 'vue-router'
import { fetchAdminDashboardStats, fetchAdminOpsSummary } from '../../services/adminService'
import LucideIcon from '../common/LucideIcon.vue'

const quickLinks = [
  { to: '/admin/students', label: 'Hoc sinh', hint: 'Tim, xem chi tiet, xoa tai khoan', icon: 'school' },
  { to: '/admin/teachers', label: 'Giao vien', hint: 'Danh sach & thao tac quan tri', icon: 'co_present' },
  { to: '/admin/admins', label: 'Admin', hint: 'Tai khoan quan tri noi bo', icon: 'admin_panel_settings' },
  { to: '/admin/exams', label: 'De thi', hint: 'Bat/tat de, thoi gian & thong ke', icon: 'quiz' }
]

const loading = ref(true)
const errorMsg = ref('')
const stats = ref(null)
const opsSummary = ref(null)
const kpiValueRefs = ref([])

const lineChartRef = ref(null)
const pieChartRef = ref(null)
let lineChart = null
let pieChart = null
let echartsLib = null

const kpiCards = computed(() => {
  const s = stats.value
  return [
    { label: 'Nguoi dung', value: s?.totalUsers || 0, icon: 'users', variant: 'primary' },
    { label: 'De thi', value: s?.totalExams || 0, icon: 'quiz', variant: 'warning' },
    { label: 'Cau hoi', value: s?.totalQuestions || 0, icon: 'help', variant: 'info' },
    { label: 'Luot thi', value: s?.totalAttempts || 0, icon: 'check_circle', variant: 'success' }
  ]
})

const attemptTrendTotal = computed(() =>
  (stats.value?.attemptsLast7Days || []).reduce((sum, d) => sum + (d.count || 0), 0)
)

const healthChecks = computed(() => opsSummary.value?.healthChecks || [])
const opsLabel = computed(() => {
  const ts = opsSummary.value?.generatedAt
  if (!ts) return 'Chua cap nhat'
  return new Date(ts).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })
})

const formatNum = (n) => (n == null ? '—' : new Intl.NumberFormat('vi-VN').format(n))

const serviceStatus = (status) => {
  if (status === 'UP' || status === 'READY') return 'up'
  if (status === 'ATTENTION') return 'warning'
  if (status === 'DOWN' || status === 'DISABLED') return 'danger'
  return 'up'
}

const serviceIcon = (status) => {
  if (status === 'UP' || status === 'READY') return 'server'
  if (status === 'ATTENTION') return 'alert_triangle'
  if (status === 'DOWN' || status === 'DISABLED') return 'server_off'
  return 'help_circle'
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
    backgroundColor: 'rgba(255, 255, 255, 0.96)',
    borderColor: 'rgba(12, 92, 171, 0.3)',
    borderWidth: 1,
    borderRadius: 10,
    padding: [10, 14],
    textStyle: { color: '#0f172a', fontWeight: 600, fontSize: 12 },
    extraCssText: 'box-shadow: 0 4px 16px rgba(15, 23, 42, 0.12);'
  },
  grid: { left: '3%', right: '4%', bottom: '3%', top: '12%', containLabel: true },
  xAxis: {
    type: 'category',
    data: series.map((d) => d.date),
    axisLine: { lineStyle: { color: 'rgba(148, 163, 184, 0.3)', width: 1 } },
    axisLabel: { color: '#64748b', fontSize: 11, fontWeight: 600 },
    axisTick: { show: false }
  },
  yAxis: {
    type: 'value',
    splitLine: { lineStyle: { color: 'rgba(148, 163, 184, 0.12)', type: 'dashed' } },
    axisLabel: { color: '#64748b', fontSize: 11, fontWeight: 600 }
  },
  series: [
    {
      name: 'Luot thi',
      type: 'line',
      smooth: 0.4,
      symbol: 'circle',
      symbolSize: 10,
      animationDuration: 2000,
      animationEasing: 'cubicOut',
      lineStyle: {
        width: 4,
        color: '#0c5cab',
        shadowColor: 'rgba(12, 92, 171, 0.5)',
        shadowBlur: 12
      },
      itemStyle: {
        color: '#0c5cab',
        borderWidth: 3,
        borderColor: '#09090b'
      },
      emphasis: {
        scale: true,
        itemStyle: { shadowColor: 'rgba(12, 92, 171, 0.7)', shadowBlur: 20 }
      },
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(12, 92, 171, 0.35)' },
          { offset: 1, color: 'rgba(12, 92, 171, 0.02)' }
        ])
      },
      data: series.map((d) => d.count)
    }
  ]
})

const buildPieOption = (s) => ({
  backgroundColor: 'transparent',
  tooltip: {
    trigger: 'item',
    backgroundColor: 'rgba(255, 255, 255, 0.96)',
    borderColor: 'rgba(12, 92, 171, 0.3)',
    borderWidth: 1,
    borderRadius: 10,
    padding: [10, 14],
    textStyle: { color: '#0f172a', fontWeight: 600 },
    extraCssText: 'box-shadow: 0 4px 16px rgba(15, 23, 42, 0.12);'
  },
  legend: {
    bottom: 8,
    textStyle: { color: '#64748b', fontWeight: 600 },
    itemWidth: 18,
    itemHeight: 12
  },
  series: [
    {
      type: 'pie',
      radius: ['42%', '70%'],
      center: ['50%', '46%'],
      avoidLabelOverlap: true,
      animationDuration: 1500,
      animationEasing: 'elasticOut',
      itemStyle: {
        borderRadius: 10,
        borderColor: '#ffffff',
        borderWidth: 3
      },
      label: { color: '#64748b', fontWeight: 600, fontSize: 12 },
      emphasis: {
        scale: true,
        scaleSize: 8,
        itemStyle: { shadowBlur: 20, shadowColor: 'rgba(0, 0, 0, 0.3)' }
      },
      data: [
        { value: s.totalStudents || 0, name: 'Hoc sinh', itemStyle: { color: '#0c5cab' } },
        { value: s.totalTeachers || 0, name: 'Giao vien', itemStyle: { color: '#f59e0b' } },
        { value: s.totalAdmins || 0, name: 'Admin', itemStyle: { color: '#10b981' } }
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

const animateKpiCounters = () => {
  kpiCards.value.forEach((card, i) => {
    const el = kpiValueRefs.value[i]
    if (!el) return
    const target = card.value
    const duration = 1500
    const startTime = performance.now()
    const step = (now) => {
      const elapsed = now - startTime
      const progress = Math.min(elapsed / duration, 1)
      const eased = 1 - Math.pow(1 - progress, 3)
      const current = Math.round(eased * target)
      el.textContent = formatNum(current)
      if (progress < 1) requestAnimationFrame(step)
    }
    setTimeout(() => requestAnimationFrame(step), 100 + i * 120)
  })
}

const onResize = () => {
  lineChart?.resize()
  pieChart?.resize()
}

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
    await nextTick()
    animateKpiCounters()
  } catch (e) {
    errorMsg.value = e?.payload?.message || e?.message || 'Khong tai duoc du lieu.'
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
})

onUnmounted(() => {
  window.removeEventListener('resize', onResize)
  disposeCharts()
})
</script>

<style scoped>
.db-quick-nav-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0.625rem;
}

@media (max-width: 600px) {
  .db-quick-nav-grid { grid-template-columns: 1fr; }
}

.db-section-card--chart {
  min-height: 320px;
}

.db-kpi-spark--primary { background: var(--db-primary); }
.db-kpi-spark--success { background: var(--db-success); }
.db-kpi-spark--warning { background: var(--db-warning); }
.db-kpi-spark--info { background: #06b6d4; }
.db-kpi-spark--danger { background: var(--db-danger); }

/* KPI card entrance stagger */
.db-kpi-card {
  animation: fadeInUp 0.5s ease backwards;
}

.db-kpi-card:nth-child(1) { animation-delay: 0.1s; }
.db-kpi-card:nth-child(2) { animation-delay: 0.2s; }
.db-kpi-card:nth-child(3) { animation-delay: 0.3s; }
.db-kpi-card:nth-child(4) { animation-delay: 0.4s; }

.db-section-card {
  animation: fadeInUp 0.5s ease backwards;
}

/* Service badges */
.db-service-badge {
  display: inline-flex;
  align-items: center;
  gap: 0.3rem;
  padding: 0.2rem 0.5rem;
  border-radius: 9999px;
  font-size: 0.65rem;
  font-weight: 800;
  text-transform: uppercase;
  white-space: nowrap;
  flex-shrink: 0;
}

.db-service-badge--up { background: var(--db-success-soft); color: var(--db-success); }
.db-service-badge--warning { background: var(--db-warning-soft); color: var(--db-warning); }
.db-service-badge--danger { background: var(--db-danger-soft); color: var(--db-danger); }
</style>
