<template>
  <div class="ads">
    <div class="ads__inner">

      <!-- Header -->
      <header class="ads__header">
        <div class="ads__header-left">
          <div class="ads__header-icon">
            <LucideIcon name="shield_check" />
          </div>
          <div>
            <h1 class="ads__title">Tổng quan hệ thống</h1>
            <p class="ads__subtitle">Quản trị người dùng, đề thi, câu hỏi và lượt thi</p>
          </div>
        </div>
        <button
          type="button"
          class="ads__refresh-btn"
          :disabled="loading"
          @click="loadStats"
        >
          <LucideIcon name="refresh" :class="{ 'ads__spin': loading }" />
          <span>{{ loading ? 'Đang tải...' : 'Làm mới' }}</span>
        </button>
      </header>

      <!-- Error -->
      <div v-if="errorMsg" class="ads__error">
        <LucideIcon name="alert_circle" />
        <span>{{ errorMsg }}</span>
        <button type="button" @click="errorMsg = ''">
          <LucideIcon name="x" />
        </button>
      </div>

      <!-- KPI Cards -->
      <div class="ads__kpi-grid">
        <div
          v-for="(card, i) in kpiCardsStyled"
          :key="card.label"
          class="ads__kpi-card"
          :class="`ads__kpi-card--${card.variant}`"
        >
          <div class="ads__kpi-icon" :class="`ads__kpi-icon--${card.variant}`">
            <LucideIcon :name="card.icon" />
          </div>
          <div class="ads__kpi-body">
            <p class="ads__kpi-val" :class="`ads__kpi-val--${card.variant}`">{{ formatNum(card.value) }}</p>
            <p class="ads__kpi-label">{{ card.label }}</p>
          </div>
          <div class="ads__kpi-spark" :class="`ads__kpi-spark--${card.variant}`" />
        </div>
      </div>

      <!-- Main grid -->
      <div class="ads__main-grid">

        <!-- Left: Quick Actions + Service Status -->
        <div class="ads__left-col">

          <!-- Quick Navigation -->
          <div class="ads__card">
            <div class="ads__card-header">
              <div class="ads__card-title-group">
                <h3 class="ads__card-title">Điều hướng nhanh</h3>
                <p class="ads__card-desc">Truy cập các nhóm quản trị</p>
              </div>
            </div>
            <div class="ads__quick-links">
              <RouterLink
                v-for="link in quickLinks"
                :key="link.to"
                :to="link.to"
                class="ads__quick-link"
              >
                <div class="ads__quick-link-icon">
                  <LucideIcon :name="link.iconName" />
                </div>
                <div class="ads__quick-link-body">
                  <p class="ads__quick-link-title">{{ link.label }}</p>
                  <p class="ads__quick-link-hint">{{ link.hint }}</p>
                </div>
                <LucideIcon name="chevron_right" class="ads__quick-link-arrow" />
              </RouterLink>
            </div>
          </div>

          <!-- Service Status -->
          <div class="ads__card">
            <div class="ads__card-header">
              <div class="ads__card-title-group">
                <h3 class="ads__card-title">Trạng thái dịch vụ</h3>
                <p class="ads__card-desc">Backend, AI, cơ sở dữ liệu và snapshot.</p>
              </div>
              <span class="ads__card-badge">
                <LucideIcon name="activity" />
                {{ opsGeneratedLabel }}
              </span>
            </div>

            <div class="ads__service-list">
              <div
                v-for="check in healthChecks"
                :key="check.id"
                class="ads__service-item"
                :class="`ads__service-item--${serviceStatus(check.status)}`"
              >
                <div class="ads__service-info">
                  <div class="ads__service-icon" :class="`ads__service-icon--${serviceStatus(check.status)}`">
                    <LucideIcon :name="serviceIcon(check.status)" />
                  </div>
                  <div>
                    <p class="ads__service-name">{{ check.label }}</p>
                    <p class="ads__service-detail">{{ check.detail }}</p>
                  </div>
                </div>
                <span class="ads__service-status-badge" :class="`ads__service-badge--${serviceStatus(check.status)}`">
                  <LucideIcon :name="serviceBadgeIcon(check.status)" />
                  {{ check.status }}
                </span>
              </div>
            </div>

            <!-- Ops Snapshot -->
            <div class="ads__snapshot">
              <p class="ads__snapshot-title">
                <LucideIcon name="rss" />
                Ops snapshot
              </p>
              <ul class="ads__snapshot-feed">
                <li v-for="item in opsActivityFeed" :key="item.id">
                  <LucideIcon name="chevron_right" />
                  {{ item.text }}
                </li>
              </ul>
            </div>
          </div>
        </div>

        <!-- Right: Moderation Queue + Charts -->
        <div class="ads__right-col">

          <!-- Moderation Queue -->
          <div class="ads__card">
            <div class="ads__card-header">
              <div class="ads__card-title-group">
                <h3 class="ads__card-title">Xử lý &amp; Bulk</h3>
                <p class="ads__card-desc">Các queue cần xem xét ngay.</p>
              </div>
              <span class="ads__card-badge ads__card-badge--warning">
                <LucideIcon name="layers" />
                {{ moderationQueueTotal }} mục
              </span>
            </div>

            <div class="ads__queue-grid">
              <RouterLink
                v-for="item in moderationQueue"
                :key="item.id"
                :to="item.route"
                class="ads__queue-item"
                :class="`ads__queue-item--${queueSeverity(item.severity)}`"
              >
                <div class="ads__queue-info">
                  <p class="ads__queue-label">{{ item.label }}</p>
                  <p class="ads__queue-hint">Mở đúng màn để xử lý.</p>
                </div>
                <span class="ads__queue-count" :class="`ads__queue-count--${queueSeverity(item.severity)}`">
                  {{ formatNum(item.count) }}
                </span>
              </RouterLink>
            </div>

            <!-- Bulk Actions -->
            <div class="ads__bulk-section">
              <p class="ads__bulk-title">
                <LucideIcon name="zap" />
                Bulk actions
              </p>
              <div class="ads__bulk-list">
                <RouterLink
                  v-for="action in bulkActions"
                  :key="action.id"
                  :to="action.route"
                  class="ads__bulk-item"
                >
                  <div class="ads__bulk-icon">
                    <LucideIcon :name="action.iconName" />
                  </div>
                  <div>
                    <p class="ads__bulk-label">{{ action.label }}</p>
                    <p class="ads__bulk-desc">{{ action.description }}</p>
                  </div>
                </RouterLink>
              </div>
            </div>
          </div>

          <!-- User Distribution Chart -->
          <div class="ads__card ads__card--chart">
            <div class="ads__card-header">
              <div class="ads__card-title-group">
                <h3 class="ads__card-title">Phân bổ người dùng</h3>
                <p class="ads__card-desc">Học sinh, giáo viên và admin.</p>
              </div>
            </div>
            <div class="ads__chart-wrap">
              <div ref="pieChartRef" class="ads__chart" />
              <div v-if="loading && !stats" class="ads__chart-skeleton">
                <div class="ads__chart-skeleton-inner" />
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Attempt Trend Chart -->
      <div class="ads__card ads__card--full">
        <div class="ads__card-header">
          <div class="ads__card-title-group">
            <h3 class="ads__card-title">Lượt thi 7 ngày gần đây</h3>
            <p class="ads__card-desc">Biến động số lượng thí sinh theo ngày.</p>
          </div>
          <span class="ads__card-badge ads__card-badge--accent">
            <LucideIcon name="trending_up" />
            {{ attemptTrendTotal }} lượt thi
          </span>
        </div>
        <div class="ads__chart-wrap">
          <div ref="lineChartRef" class="ads__chart ads__chart--line" />
          <div v-if="loading && !stats" class="ads__chart-skeleton">
            <div class="ads__chart-skeleton-inner ads__chart-skeleton-inner--wide" />
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
import { createRafThrottle } from '../../utils/chartResizeThrottle'
import LucideIcon from '../common/LucideIcon.vue'

const quickLinks = [
  { to: '/admin/students', label: 'Học sinh', hint: 'Tìm, xem chi tiết, xóa tài khoản', iconName: 'school' },
  { to: '/admin/teachers', label: 'Giáo viên', hint: 'Danh sách & thao tác quản trị', iconName: 'co_present' },
  { to: '/admin/admins', label: 'Admin', hint: 'Tài khoản quản trị nội bộ', iconName: 'shield' },
  { to: '/admin/exams', label: 'Đề thi', hint: 'Bật/tắt đề, thời gian & thống kê', iconName: 'quiz' }
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
  return [
    { label: 'Người dùng', value: s?.totalUsers || 0, icon: 'users', variant: 'primary' },
    { label: 'Đề thi', value: s?.totalExams || 0, icon: 'quiz', variant: 'amber' },
    { label: 'Câu hỏi', value: s?.totalQuestions || 0, icon: 'help_circle', variant: 'cyan' },
    { label: 'Lượt thi', value: s?.totalAttempts || 0, icon: 'check_circle', variant: 'emerald' }
  ]
})

const kpiCardsStyled = computed(() => kpiCards.value)

const attemptTrendTotal = computed(() =>
  (stats.value?.attemptsLast7Days || []).reduce((sum, d) => sum + (d.count || 0), 0)
)

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
    return [{ id: 'pending', text: 'Đang chờ dữ liệu vận hành từ backend.' }]
  }
  return [
    { id: 'queue', text: `Tổng moderation queue hiện tại: ${moderationQueueTotal.value} mục.` },
    { id: 'health', text: `${healthChecks.value.filter((item) => ['ATTENTION', 'DOWN', 'DISABLED'].includes(item.status)).length} dịch vụ đang cần theo dõi sát hơn.` },
    { id: 'imports', text: `Queue import và trạng thái đề thi được gom về cùng một hub để xử lý nhanh.` }
  ]
})

const formatNum = (n) => (n == null ? '—' : new Intl.NumberFormat('vi-VN').format(n))

const serviceStatus = (status) => {
  if (status === 'UP' || status === 'READY') return 'up'
  if (status === 'ATTENTION') return 'attention'
  if (status === 'DOWN') return 'down'
  return 'unknown'
}

const serviceIcon = (status) => {
  if (status === 'UP' || status === 'READY') return 'server'
  if (status === 'ATTENTION') return 'alert_triangle'
  if (status === 'DOWN') return 'server_off'
  return 'help_circle'
}

const serviceBadgeIcon = (status) => {
  if (status === 'UP' || status === 'READY') return 'check_circle'
  if (status === 'ATTENTION') return 'alert_triangle'
  if (status === 'DOWN') return 'x_circle'
  return 'help_circle'
}

const queueSeverity = (severity) => {
  if (severity === 'HIGH') return 'high'
  if (severity === 'MEDIUM') return 'medium'
  return 'low'
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
    borderColor: 'rgba(99, 102, 241, 0.3)',
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
      name: 'Lượt thi',
      type: 'line',
      smooth: true,
      symbol: 'circle',
      symbolSize: 8,
      lineStyle: { width: 3, color: '#6366f1' },
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(99, 102, 241, 0.25)' },
          { offset: 1, color: 'rgba(99, 102, 241, 0)' }
        ])
      },
      data: series.map((d) => d.count)
    }
  ]
})

const buildPieOption = (s) => ({
  backgroundColor: 'transparent',
  tooltip: { trigger: 'item', backgroundColor: 'rgba(15, 23, 42, 0.92)', borderColor: 'rgba(99, 102, 241, 0.3)' },
  legend: { bottom: 0, textStyle: { color: '#94a3b8' } },
  series: [
    {
      type: 'pie',
      radius: ['42%', '68%'],
      center: ['50%', '46%'],
      avoidLabelOverlap: true,
      itemStyle: { borderRadius: 8, borderColor: '#1a1a2e', borderWidth: 3 },
      label: { color: '#cbd5e1' },
      data: [
        { value: s.totalStudents || 0, name: 'Học sinh', itemStyle: { color: '#6366f1' } },
        { value: s.totalTeachers || 0, name: 'Giáo viên', itemStyle: { color: '#d97706' } },
        { value: s.totalAdmins || 0, name: 'Admin', itemStyle: { color: '#8b5cf6' } }
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

<style scoped>
.ads {
  min-height: 100vh;
  background: var(--ds-bg);
}

.ads__inner {
  max-width: 1280px;
  margin: 0 auto;
  padding: 1.5rem 1.5rem 2.5rem;
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

/* Header */
.ads__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  flex-wrap: wrap;
}

.ads__header-left {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.ads__header-icon {
  width: 48px;
  height: 48px;
  border-radius: var(--ds-radius-xl);
  background: linear-gradient(135deg, var(--ds-primary), #8b5cf6);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  box-shadow: 0 4px 16px rgba(99, 102, 241, 0.3);
}

.ads__title {
  font-family: var(--ds-font-display);
  font-size: 1.5rem;
  font-weight: 900;
  color: var(--ds-text);
  margin: 0;
  line-height: 1.2;
}

.dark .ads__title { color: #f1f5f9; }

.ads__subtitle {
  font-size: 0.875rem;
  color: var(--ds-text-muted);
  margin: 0.25rem 0 0;
  font-weight: 500;
}

.ads__refresh-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.625rem 1.25rem;
  border-radius: var(--ds-radius-xl);
  border: 1.5px solid var(--ds-border);
  background: var(--ds-surface);
  color: var(--ds-text-secondary);
  font-size: 0.875rem;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.15s ease;
  font-family: inherit;
}

.dark .ads__refresh-btn {
  background: var(--ds-gray-800);
  border-color: var(--ds-border-strong);
  color: #94a3b8;
}

.ads__refresh-btn:hover:not(:disabled) {
  border-color: var(--ds-primary);
  color: var(--ds-primary);
  background: var(--ds-primary-soft);
  transform: translateY(-1px);
}

.ads__refresh-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.ads__spin { animation: adsSpin 1s linear infinite; }
@keyframes adsSpin { to { transform: rotate(360deg); } }

/* Error */
.ads__error {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem 1rem;
  background: var(--ds-danger-soft);
  border: 1.5px solid rgba(220, 38, 38, 0.2);
  border-radius: var(--ds-radius-xl);
  color: var(--ds-danger);
  font-size: 0.875rem;
  font-weight: 600;
}

.ads__error span { flex: 1; }
.ads__error button { background: none; border: none; cursor: pointer; color: var(--ds-danger); padding: 0.25rem; }

/* KPI Grid */
.ads__kpi-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 1rem;
}

@media (max-width: 900px) {
  .ads__kpi-grid { grid-template-columns: repeat(2, 1fr); }
}

@media (max-width: 480px) {
  .ads__kpi-grid { grid-template-columns: 1fr; }
}

.ads__kpi-card {
  position: relative;
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 1.25rem;
  background: var(--ds-surface);
  border: 1.5px solid var(--ds-border);
  border-radius: var(--ds-radius-2xl);
  overflow: hidden;
  transition: all 0.2s ease;
}

.dark .ads__kpi-card { border-color: var(--ds-border-strong); }

.ads__kpi-card:hover { transform: translateY(-2px); box-shadow: 0 8px 24px rgba(0,0,0,0.08); }
.dark .ads__kpi-card:hover { box-shadow: 0 8px 24px rgba(0,0,0,0.2); }

/* KPI stripe accent */
.ads__kpi-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
}
.ads__kpi-card--primary::before { background: var(--ds-primary); }
.ads__kpi-card--amber::before { background: #d97706; }
.ads__kpi-card--cyan::before { background: #06b6d4; }
.ads__kpi-card--emerald::before { background: var(--ds-success); }

/* KPI spark (decorative circle) */
.ads__kpi-spark {
  position: absolute;
  bottom: -20px;
  right: -20px;
  width: 80px;
  height: 80px;
  border-radius: 50%;
  opacity: 0.06;
}
.ads__kpi-spark--primary { background: var(--ds-primary); }
.ads__kpi-spark--amber { background: #d97706; }
.ads__kpi-spark--cyan { background: #06b6d4; }
.ads__kpi-spark--emerald { background: var(--ds-success); }

.ads__kpi-icon {
  width: 48px;
  height: 48px;
  border-radius: var(--ds-radius-xl);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.ads__kpi-icon--primary { background: var(--ds-primary-soft); color: var(--ds-primary); }
.ads__kpi-icon--amber { background: rgba(217, 119, 6, 0.1); color: #d97706; }
.ads__kpi-icon--cyan { background: rgba(6, 182, 212, 0.1); color: #06b6d4; }
.ads__kpi-icon--emerald { background: var(--ds-success-soft); color: var(--ds-success); }

.ads__kpi-body { flex: 1; min-width: 0; }

.ads__kpi-val {
  font-family: var(--ds-font-display);
  font-size: 1.75rem;
  font-weight: 900;
  line-height: 1;
  margin-bottom: 0.25rem;
}
.ads__kpi-val--primary { color: var(--ds-primary); }
.ads__kpi-val--amber { color: #d97706; }
.ads__kpi-val--cyan { color: #06b6d4; }
.ads__kpi-val--emerald { color: var(--ds-success); }

.ads__kpi-label {
  font-size: 0.75rem;
  font-weight: 700;
  color: var(--ds-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.08em;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* Main grid */
.ads__main-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1.25rem;
}

@media (max-width: 900px) {
  .ads__main-grid { grid-template-columns: 1fr; }
}

/* Card */
.ads__card {
  background: var(--ds-surface);
  border: 1.5px solid var(--ds-border);
  border-radius: var(--ds-radius-2xl);
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.dark .ads__card { border-color: var(--ds-border-strong); }

.ads__card--chart { min-height: 320px; }
.ads__card--full { width: 100%; }

.ads__card-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 0.75rem;
  padding: 1.25rem 1.25rem 0;
  flex-shrink: 0;
}

.ads__card-title-group { flex: 1; min-width: 0; }

.ads__card-title {
  font-family: var(--ds-font-display);
  font-size: 1rem;
  font-weight: 800;
  color: var(--ds-text);
  margin: 0;
  line-height: 1.3;
}

.dark .ads__card-title { color: #f1f5f9; }

.ads__card-desc {
  font-size: 0.75rem;
  color: var(--ds-text-muted);
  margin: 0.25rem 0 0;
  font-weight: 500;
}

.ads__card-badge {
  display: inline-flex;
  align-items: center;
  gap: 0.375rem;
  padding: 0.3rem 0.75rem;
  border-radius: var(--ds-radius-full);
  font-size: 0.7rem;
  font-weight: 700;
  background: var(--ds-gray-100);
  color: var(--ds-text-muted);
  white-space: nowrap;
  flex-shrink: 0;
}

.dark .ads__card-badge { background: var(--ds-gray-800); }

.ads__card-badge--warning { background: rgba(234, 179, 8, 0.1); color: #d97706; }
.ads__card-badge--accent { background: var(--ds-primary-soft); color: var(--ds-primary); }

/* Quick Links */
.ads__quick-links {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  padding: 0.875rem 1.25rem 1.25rem;
}

.ads__quick-link {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.875rem 1rem;
  border-radius: var(--ds-radius-xl);
  border: 1.5px solid var(--ds-border);
  background: var(--ds-gray-50);
  text-decoration: none;
  transition: all 0.15s ease;
}

.dark .ads__quick-link {
  background: var(--ds-gray-800);
  border-color: var(--ds-border-strong);
}

.ads__quick-link:hover {
  border-color: var(--ds-primary-border);
  background: var(--ds-primary-soft);
  transform: translateX(4px);
}

.dark .ads__quick-link:hover { background: rgba(79, 70, 229, 0.1); }

.ads__quick-link-icon {
  width: 40px;
  height: 40px;
  border-radius: var(--ds-radius-lg);
  background: var(--ds-primary-soft);
  color: var(--ds-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.dark .ads__quick-link-icon { background: rgba(79, 70, 229, 0.15); }

.ads__quick-link:hover .ads__quick-link-icon {
  background: var(--ds-primary);
  color: white;
}

.ads__quick-link-body { flex: 1; min-width: 0; }

.ads__quick-link-title {
  font-size: 0.875rem;
  font-weight: 700;
  color: var(--ds-text);
  margin: 0;
}

.dark .ads__quick-link-title { color: #f1f5f9; }

.ads__quick-link-hint {
  font-size: 0.7rem;
  color: var(--ds-text-muted);
  margin: 0.125rem 0 0;
}

.ads__quick-link-arrow {
  color: var(--ds-text-muted);
  flex-shrink: 0;
  transition: transform 0.15s ease;
}

.ads__quick-link:hover .ads__quick-link-arrow {
  transform: translateX(4px);
  color: var(--ds-primary);
}

/* Service Status */
.ads__service-list {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  padding: 0 1.25rem;
  overflow-y: auto;
  max-height: 280px;
}

.ads__service-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  padding: 0.75rem 1rem;
  border-radius: var(--ds-radius-xl);
  border: 1.5px solid var(--ds-border);
  background: var(--ds-gray-50);
  transition: all 0.15s ease;
}

.dark .ads__service-item {
  background: var(--ds-gray-800);
  border-color: var(--ds-border-strong);
}

.ads__service-item--up { border-color: rgba(22, 163, 74, 0.2); background: var(--ds-success-soft); }
.dark .ads__service-item--up { background: rgba(22, 163, 74, 0.06); }
.ads__service-item--attention { border-color: rgba(234, 179, 8, 0.2); background: rgba(234, 179, 8, 0.05); }
.dark .ads__service-item--attention { background: rgba(234, 179, 8, 0.06); }
.ads__service-item--down { border-color: rgba(220, 38, 38, 0.2); background: var(--ds-danger-soft); }
.dark .ads__service-item--down { background: rgba(220, 38, 38, 0.06); }

.ads__service-info {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex: 1;
  min-width: 0;
}

.ads__service-icon {
  width: 32px;
  height: 32px;
  border-radius: var(--ds-radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  background: var(--ds-gray-200);
  color: var(--ds-text-muted);
}
.dark .ads__service-icon { background: var(--ds-gray-700); }
.ads__service-icon--up { background: var(--ds-success-soft); color: var(--ds-success); }
.dark .ads__service-icon--up { background: rgba(22, 163, 74, 0.15); }
.ads__service-icon--attention { background: rgba(234, 179, 8, 0.1); color: #d97706; }
.ads__service-icon--down { background: var(--ds-danger-soft); color: var(--ds-danger); }

.ads__service-name {
  font-size: 0.8rem;
  font-weight: 700;
  color: var(--ds-text);
  margin: 0;
}

.dark .ads__service-name { color: #f1f5f9; }

.ads__service-detail {
  font-size: 0.7rem;
  color: var(--ds-text-muted);
  margin: 0.125rem 0 0;
}

.ads__service-status-badge {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  padding: 0.2rem 0.5rem;
  border-radius: var(--ds-radius-full);
  font-size: 0.65rem;
  font-weight: 800;
  text-transform: uppercase;
  white-space: nowrap;
  flex-shrink: 0;
}

.ads__service-badge--up { background: var(--ds-success-soft); color: var(--ds-success); }
.ads__service-badge--attention { background: rgba(234, 179, 8, 0.1); color: #d97706; }
.ads__service-badge--down { background: var(--ds-danger-soft); color: var(--ds-danger); }
.ads__service-badge--unknown { background: var(--ds-gray-100); color: var(--ds-text-muted); }
.dark .ads__service-badge--unknown { background: var(--ds-gray-700); }

/* Ops Snapshot */
.ads__snapshot {
  margin: 0.875rem 1.25rem 1.25rem;
  padding: 1rem;
  background: var(--ds-gray-50);
  border: 1.5px solid var(--ds-border);
  border-radius: var(--ds-radius-xl);
}

.dark .ads__snapshot {
  background: var(--ds-gray-800);
  border-color: var(--ds-border-strong);
}

.ads__snapshot-title {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  font-size: 0.8rem;
  font-weight: 800;
  color: var(--ds-text);
  margin: 0 0 0.625rem;
}

.dark .ads__snapshot-title { color: #f1f5f9; }

.ads__snapshot-feed {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 0.375rem;
}

.ads__snapshot-feed li {
  display: flex;
  align-items: flex-start;
  gap: 0.375rem;
  font-size: 0.75rem;
  color: var(--ds-text-muted);
  line-height: 1.4;
}

.ads__snapshot-feed li > .lucide {
  color: var(--ds-primary);
  flex-shrink: 0;
  margin-top: 0.1rem;
}

/* Queue */
.ads__queue-grid {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  padding: 0 1.25rem;
}

.ads__queue-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  padding: 0.875rem 1rem;
  border-radius: var(--ds-radius-xl);
  border: 1.5px solid var(--ds-border);
  background: var(--ds-gray-50);
  text-decoration: none;
  transition: all 0.15s ease;
}

.dark .ads__queue-item {
  background: var(--ds-gray-800);
  border-color: var(--ds-border-strong);
}

.ads__queue-item:hover { transform: translateX(4px); }
.ads__queue-item--high { border-color: rgba(220, 38, 38, 0.3); background: var(--ds-danger-soft); }
.dark .ads__queue-item--high { background: rgba(220, 38, 38, 0.06); }
.ads__queue-item--medium { border-color: rgba(234, 179, 8, 0.3); background: rgba(234, 179, 8, 0.05); }
.dark .ads__queue-item--medium { background: rgba(234, 179, 8, 0.06); }

.ads__queue-info { flex: 1; min-width: 0; }

.ads__queue-label {
  font-size: 0.875rem;
  font-weight: 700;
  color: var(--ds-text);
  margin: 0;
}

.dark .ads__queue-label { color: #f1f5f9; }

.ads__queue-hint {
  font-size: 0.7rem;
  color: var(--ds-text-muted);
  margin: 0.125rem 0 0;
}

.ads__queue-count {
  font-family: var(--ds-font-display);
  font-size: 1.5rem;
  font-weight: 900;
  color: var(--ds-text-muted);
  flex-shrink: 0;
}

.ads__queue-count--high { color: var(--ds-danger); }
.ads__queue-count--medium { color: #d97706; }

/* Bulk */
.ads__bulk-section {
  padding: 0.875rem 1.25rem 1.25rem;
}

.ads__bulk-title {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  font-size: 0.8rem;
  font-weight: 800;
  color: var(--ds-text);
  margin: 0 0 0.625rem;
}

.dark .ads__bulk-title { color: #f1f5f9; }

.ads__bulk-list {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.ads__bulk-item {
  display: flex;
  align-items: flex-start;
  gap: 0.75rem;
  padding: 0.875rem 1rem;
  border-radius: var(--ds-radius-xl);
  border: 1.5px solid var(--ds-border);
  background: var(--ds-gray-50);
  text-decoration: none;
  transition: all 0.15s ease;
}

.dark .ads__bulk-item {
  background: var(--ds-gray-800);
  border-color: var(--ds-border-strong);
}

.ads__bulk-item:hover {
  border-color: var(--ds-primary-border);
  background: var(--ds-primary-soft);
  transform: translateX(4px);
}

.dark .ads__bulk-item:hover { background: rgba(79, 70, 229, 0.1); }

.ads__bulk-icon {
  width: 36px;
  height: 36px;
  border-radius: var(--ds-radius-lg);
  background: rgba(139, 92, 246, 0.1);
  color: #8b5cf6;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.ads__bulk-label {
  font-size: 0.8rem;
  font-weight: 700;
  color: var(--ds-text);
  margin: 0;
}

.dark .ads__bulk-label { color: #f1f5f9; }

.ads__bulk-desc {
  font-size: 0.7rem;
  color: var(--ds-text-muted);
  margin: 0.125rem 0 0;
}

/* Chart */
.ads__chart-wrap {
  flex: 1;
  padding: 1rem 1.25rem 1.25rem;
  position: relative;
  min-height: 240px;
}

.ads__chart {
  width: 100%;
  height: 240px;
}

.ads__chart--line {
  height: 220px;
}

.ads__chart-skeleton {
  position: absolute;
  inset: 1rem 1.25rem 1.25rem;
  border-radius: var(--ds-radius-xl);
  background: var(--ds-gray-100);
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
}

.dark .ads__chart-skeleton { background: var(--ds-gray-800); }

.ads__chart-skeleton-inner {
  width: 160px;
  height: 160px;
  border-radius: 50%;
  background: linear-gradient(90deg, var(--ds-gray-100) 25%, var(--ds-gray-200) 50%, var(--ds-gray-100) 75%);
  background-size: 200% 100%;
  animation: adsShimmer 1.5s infinite;
}

.dark .ads__chart-skeleton-inner {
  background: linear-gradient(90deg, var(--ds-gray-800) 25%, var(--ds-gray-700) 50%, var(--ds-gray-800) 75%);
  background-size: 200% 100%;
}

.ads__chart-skeleton-inner--wide {
  width: 90%;
  height: 100%;
  border-radius: var(--ds-radius-xl);
}

@keyframes adsShimmer {
  0% { background-position: -200% 0; }
  100% { background-position: 200% 0; }
}

/* Left/Right columns */
.ads__left-col,
.ads__right-col {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}
</style>
