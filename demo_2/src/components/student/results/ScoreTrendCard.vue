/**
 * ScoreTrendCard.vue
 * Component to visualize score trends over time
 */
<template>
  <div class="score-trend">
    <!-- Header -->
    <div class="score-trend__header">
      <div class="score-trend__header-left">
        <div class="score-trend__icon">
          <LucideIcon name="trending_up" />
        </div>
        <div>
          <h3 class="score-trend__title">{{ title }}</h3>
          <p class="score-trend__subtitle">{{ subtitle }}</p>
        </div>
      </div>
      <div v-if="trend !== null" class="score-trend__trend-badge" :class="trendBadgeClass">
        <LucideIcon :name="trend >= 0 ? 'trending_up' : 'trending_down'" />
        <span>{{ Math.abs(trend) }}%</span>
      </div>
    </div>

    <!-- Loading state -->
    <div v-if="loading" class="score-trend__loading">
      <SkeletonLoader variant="chart" />
    </div>

    <!-- Empty state -->
    <div v-else-if="!dataPoints.length" class="score-trend__empty">
      <LucideIcon name="show_chart" />
      <p>Chưa có đủ dữ liệu để hiển thị xu hướng</p>
    </div>

    <!-- Chart -->
    <div v-else class="score-trend__chart-wrapper">
      <!-- Y-axis labels -->
      <div class="score-trend__y-axis">
        <span>10</span>
        <span>7.5</span>
        <span>5</span>
        <span>2.5</span>
        <span>0</span>
      </div>

      <!-- Chart area -->
      <div class="score-trend__chart">
        <!-- Grid lines -->
        <div class="score-trend__grid-lines">
          <div v-for="i in 5" :key="i" class="score-trend__grid-line" />
        </div>

        <!-- Area fill -->
        <svg class="score-trend__area" preserveAspectRatio="none" viewBox="0 0 100 100">
          <defs>
            <linearGradient id="scoreGradient" x1="0" y1="0" x2="0" y2="1">
              <stop offset="0%" :stop-color="areaColor" stop-opacity="0.3" />
              <stop offset="100%" :stop-color="areaColor" stop-opacity="0.02" />
            </linearGradient>
          </defs>
          <!-- Area path -->
          <path
            :d="areaPath"
            fill="url(#scoreGradient)"
          />
          <!-- Line path -->
          <path
            :d="linePath"
            fill="none"
            :stroke="lineColor"
            stroke-width="2"
            stroke-linecap="round"
            stroke-linejoin="round"
          />
          <!-- Data points -->
          <circle
            v-for="(point, index) in normalizedPoints"
            :key="index"
            :cx="point.x"
            :cy="point.y"
            r="3"
            :fill="lineColor"
            class="score-trend__point"
          />
        </svg>

        <!-- X-axis labels -->
        <div class="score-trend__x-axis">
          <span v-for="(label, index) in xAxisLabels" :key="index">{{ label }}</span>
        </div>
      </div>
    </div>

    <!-- Stats summary -->
    <div v-if="!loading && dataPoints.length > 1" class="score-trend__stats">
      <div class="score-trend__stat">
        <span class="score-trend__stat-label">Điểm cao nhất</span>
        <span class="score-trend__stat-value score-trend__stat-value--high">{{ maxScore.toFixed(1) }}</span>
      </div>
      <div class="score-trend__stat">
        <span class="score-trend__stat-label">Điểm thấp nhất</span>
        <span class="score-trend__stat-value score-trend__stat-value--low">{{ minScore.toFixed(1) }}</span>
      </div>
      <div class="score-trend__stat">
        <span class="score-trend__stat-label">Trung bình</span>
        <span class="score-trend__stat-value">{{ avgScore.toFixed(1) }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import LucideIcon from '../common/LucideIcon.vue'
import SkeletonLoader from '../shared/SkeletonLoader.vue'

const props = defineProps({
  title: {
    type: String,
    default: 'Xu hướng điểm số'
  },
  subtitle: {
    type: String,
    default: 'Biến động điểm thi qua các lần'
  },
  dataPoints: {
    type: Array,
    default: () => []
  },
  loading: {
    type: Boolean,
    default: false
  },
  maxValue: {
    type: Number,
    default: 10
  }
})

const normalizedPoints = computed(() => {
  if (!props.dataPoints.length) return []
  
  const step = 100 / (props.dataPoints.length - 1)
  return props.dataPoints.map((point, index) => ({
    x: index * step,
    y: 100 - (point.value / props.maxValue) * 100,
    raw: point
  }))
})

const linePath = computed(() => {
  if (normalizedPoints.value.length < 2) return ''
  return normalizedPoints.value.map((p, i) => 
    `${i === 0 ? 'M' : 'L'} ${p.x} ${p.y}`
  ).join(' ')
})

const areaPath = computed(() => {
  if (normalizedPoints.value.length < 2) return ''
  const line = normalizedPoints.value.map((p, i) => 
    `${i === 0 ? 'M' : 'L'} ${p.x} ${p.y}`
  ).join(' ')
  const last = normalizedPoints.value[normalizedPoints.value.length - 1]
  const first = normalizedPoints.value[0]
  return `${line} L ${last.x} 100 L ${first.x} 100 Z`
})

const xAxisLabels = computed(() => {
  return props.dataPoints.map(p => p.label || '')
})

const maxScore = computed(() => {
  if (!props.dataPoints.length) return 0
  return Math.max(...props.dataPoints.map(p => p.value))
})

const minScore = computed(() => {
  if (!props.dataPoints.length) return 0
  return Math.min(...props.dataPoints.map(p => p.value))
})

const avgScore = computed(() => {
  if (!props.dataPoints.length) return 0
  const total = props.dataPoints.reduce((sum, p) => sum + p.value, 0)
  return total / props.dataPoints.length
})

const trend = computed(() => {
  if (props.dataPoints.length < 2) return null
  const first = props.dataPoints[0].value
  const last = props.dataPoints[props.dataPoints.length - 1].value
  if (first === 0) return null
  return Math.round(((last - first) / first) * 100)
})

const lineColor = computed(() => {
  if (trend.value === null) return 'var(--ds-primary)'
  if (trend.value > 0) return 'var(--ds-success)'
  if (trend.value < 0) return 'var(--ds-danger)'
  return 'var(--ds-primary)'
})

const areaColor = computed(() => lineColor.value)

const trendBadgeClass = computed(() => {
  if (trend.value === null) return ''
  if (trend.value > 0) return 'score-trend__trend-badge--up'
  if (trend.value < 0) return 'score-trend__trend-badge--down'
  return ''
})
</script>

<style scoped>
.score-trend {
  background: var(--ds-surface);
  border: 1.5px solid var(--ds-border);
  border-radius: var(--ds-radius-2xl);
  overflow: hidden;
}

.dark .score-trend {
  border-color: var(--ds-border-strong);
}

/* Header */
.score-trend__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  padding: 1rem 1.125rem;
  border-bottom: 1px solid var(--ds-border);
  background: var(--ds-gray-50);
}

.dark .score-trend__header {
  border-bottom-color: var(--ds-border-strong);
  background: var(--ds-gray-800);
}

.score-trend__header-left {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.score-trend__icon {
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

.score-trend__title {
  font-family: var(--ds-font-display);
  font-size: 0.875rem;
  font-weight: 800;
  color: var(--ds-text);
  margin: 0;
}

.dark .score-trend__title {
  color: #f1f5f9;
}

.score-trend__subtitle {
  font-size: 0.7rem;
  color: var(--ds-text-muted);
  margin: 0.2rem 0 0;
  font-weight: 500;
}

.score-trend__trend-badge {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  padding: 0.375rem 0.75rem;
  border-radius: var(--ds-radius-full);
  font-size: 0.75rem;
  font-weight: 700;
}

.score-trend__trend-badge--up {
  background: var(--ds-success-soft);
  color: var(--ds-success);
}

.score-trend__trend-badge--down {
  background: var(--ds-danger-soft);
  color: var(--ds-danger);
}

/* Loading */
.score-trend__loading {
  padding: 1.5rem;
}

/* Empty */
.score-trend__empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.5rem;
  padding: 2.5rem 1rem;
  text-align: center;
  color: var(--ds-text-muted);
}

.score-trend__empty p {
  font-size: 0.8rem;
  margin: 0;
}

/* Chart */
.score-trend__chart-wrapper {
  display: flex;
  gap: 0.5rem;
  padding: 1rem 1.125rem;
}

.score-trend__y-axis {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 0.5rem 0;
  font-size: 0.6rem;
  color: var(--ds-text-muted);
  font-weight: 600;
  text-align: right;
  width: 1.5rem;
}

.score-trend__chart {
  flex: 1;
  position: relative;
  height: 120px;
}

.score-trend__grid-lines {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  pointer-events: none;
}

.score-trend__grid-line {
  height: 1px;
  background: var(--ds-border);
  opacity: 0.5;
}

.dark .score-trend__grid-line {
  background: var(--ds-border-strong);
}

.score-trend__area {
  width: 100%;
  height: 100%;
}

.score-trend__point {
  transition: r 0.2s ease;
  cursor: pointer;
}

.score-trend__point:hover {
  r: 5;
}

.score-trend__x-axis {
  position: absolute;
  bottom: -1.25rem;
  left: 0;
  right: 0;
  display: flex;
  justify-content: space-between;
  font-size: 0.6rem;
  color: var(--ds-text-muted);
  font-weight: 500;
}

/* Stats */
.score-trend__stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 0.5rem;
  padding: 0.875rem 1.125rem;
  border-top: 1px solid var(--ds-border);
  background: var(--ds-gray-50);
}

.dark .score-trend__stats {
  border-top-color: var(--ds-border-strong);
  background: var(--ds-gray-800);
}

.score-trend__stat {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.25rem;
  text-align: center;
}

.score-trend__stat-label {
  font-size: 0.6rem;
  font-weight: 600;
  color: var(--ds-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.score-trend__stat-value {
  font-family: var(--ds-font-display);
  font-size: 1rem;
  font-weight: 800;
  color: var(--ds-text);
}

.dark .score-trend__stat-value {
  color: #f1f5f9;
}

.score-trend__stat-value--high {
  color: var(--ds-success);
}

.score-trend__stat-value--low {
  color: var(--ds-danger);
}
</style>
