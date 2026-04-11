<template>
  <div class="sdc">
    <!-- Header -->
    <div class="sdc__header">
      <div class="sdc__header-left">
        <h3 class="sdc__title">Phân bố điểm</h3>
        <p class="sdc__subtitle">{{ examTitle || 'Tất cả kỳ thi' }}</p>
      </div>
      <div class="sdc__header-right">
        <button
          type="button"
          class="sdc__toggle"
          :class="{ 'sdc__toggle--active': chartType === 'bar' }"
          @click="chartType = 'bar'"
        >
          <LucideIcon name="bar_chart" />
        </button>
        <button
          type="button"
          class="sdc__toggle"
          :class="{ 'sdc__toggle--active': chartType === 'line' }"
          @click="chartType = 'line'"
        >
          <LucideIcon name="show_chart" />
        </button>
      </div>
    </div>

    <!-- Chart -->
    <div class="sdc__chart-wrap" ref="chartEl">
      <!-- Loading -->
      <div v-if="loading" class="sdc__loading">
        <div class="sdc__skeleton-bars">
          <div v-for="i in 11" :key="i" class="sdc__skeleton-bar" :style="{ height: `${20 + Math.random() * 60}%` }" />
        </div>
      </div>

      <!-- Empty -->
      <div v-else-if="!hasData" class="sdc__empty">
        <LucideIcon name="analytics" />
        <p>Chưa có dữ liệu điểm</p>
      </div>

      <!-- ECharts -->
      <div v-show="!loading && hasData" ref="chartSurfaceEl" class="sdc__canvas" />
    </div>

    <!-- Stats summary -->
    <div v-if="!loading && hasData" class="sdc__stats">
      <div class="sdc__stat">
        <span class="sdc__stat-label">Trung bình</span>
        <span class="sdc__stat-val" :class="avgColorClass">{{ average?.toFixed(1) || '—' }}</span>
      </div>
      <div class="sdc__stat">
        <span class="sdc__stat-label">Trung vị</span>
        <span class="sdc__stat-val">{{ median?.toFixed(1) || '—' }}</span>
      </div>
      <div class="sdc__stat">
        <span class="sdc__stat-label">Độ lệch chuẩn</span>
        <span class="sdc__stat-val">{{ stdDev?.toFixed(2) || '—' }}</span>
      </div>
      <div class="sdc__stat">
        <span class="sdc__stat-label">Cao nhất</span>
        <span class="sdc__stat-val sdc__stat-val--success">{{ maxScore?.toFixed(1) || '—' }}</span>
      </div>
      <div class="sdc__stat">
        <span class="sdc__stat-label">Thấp nhất</span>
        <span class="sdc__stat-val" :class="minScore < passingScore ? 'sdc__stat-val--danger' : ''">{{ minScore?.toFixed(1) || '—' }}</span>
      </div>
    </div>

    <!-- Pass/fail legend -->
    <div v-if="!loading && hasData" class="sdc__legend">
      <span class="sdc__legend-item sdc__legend-item--fail">
        <LucideIcon name="close" />
        Không đạt ({{ failedCount }}, {{ failedPercent }}%)
      </span>
      <span class="sdc__legend-item sdc__legend-item--pass">
        <LucideIcon name="check" />
        Đạt ({{ passedCount }}, {{ passedPercent }}%)
      </span>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted, nextTick } from 'vue'

const props = defineProps({
  distribution: { type: Array, default: () => [] },
  average: { type: Number, default: null },
  median: { type: Number, default: null },
  stdDev: { type: Number, default: null },
  passingScore: { type: Number, default: 5 },
  examTitle: { type: String, default: '' },
  loading: { type: Boolean, default: false }
})

const chartEl = ref(null)
const chartSurfaceEl = ref(null)
const chartType = ref('bar')
let chartInstance = null

const getChartPixelRatio = () => {
  if (typeof window === 'undefined') return 1
  return Math.min(Math.max(window.devicePixelRatio || 1, 1), 2)
}

const hasData = computed(() => props.distribution && props.distribution.length > 0)

const labels = computed(() => {
  if (!hasData.value) return []
  return props.distribution.map(d => String(d.score ?? d.label ?? 0))
})

const values = computed(() => {
  if (!hasData.value) return []
  return props.distribution.map(d => Number(d.count ?? d.value ?? 0))
})

const maxScore = computed(() => {
  if (!hasData.value) return null
  const withScores = props.distribution.filter(d => Number(d.count ?? 0) > 0)
  if (!withScores.length) return null
  return Math.max(...withScores.map(d => Number(d.score ?? d.label ?? 0)))
})

const minScore = computed(() => {
  if (!hasData.value) return null
  const withScores = props.distribution.filter(d => Number(d.count ?? 0) > 0)
  if (!withScores.length) return null
  return Math.min(...withScores.map(d => Number(d.score ?? d.label ?? 0)))
})

const totalStudents = computed(() => values.value.reduce((s, v) => s + v, 0))

const passedCount = computed(() => {
  if (!hasData.value) return 0
  const passIdx = props.distribution.findIndex(d => Number(d.score ?? d.label ?? 0) >= props.passingScore)
  if (passIdx < 0) return 0
  return props.distribution.slice(passIdx).reduce((s, d) => s + Number(d.count ?? 0), 0)
})

const failedCount = computed(() => Math.max(0, totalStudents.value - passedCount.value))
const passedPercent = computed(() => totalStudents.value ? Math.round(passedCount.value / totalStudents.value * 100) : 0)
const failedPercent = computed(() => Math.max(0, 100 - passedPercent.value))

const avgColorClass = computed(() => {
  const a = props.average
  if (a === null || a === undefined) return ''
  if (a >= 8) return 'sdc__stat-val--success'
  if (a >= props.passingScore) return 'sdc__stat-val--warning'
  return 'sdc__stat-val--danger'
})

// Bar colors based on pass threshold (softened palette for comfortable viewing)
const barColors = computed(() => {
  if (!hasData.value) return []
  return props.distribution.map(d => {
    const score = Number(d.score ?? d.label ?? 0)
    if (score < props.passingScore) return '#f87171'  // soft red
    if (score >= 8) return '#34d399'  // soft emerald
    return '#818cf8'  // soft indigo
  })
})

const initChart = async () => {
  if (!chartSurfaceEl.value || !hasData.value) return

  if (chartInstance) {
    chartInstance.dispose()
    chartInstance = null
  }

  const ec = await import('echarts')
  chartInstance = ec.init(chartSurfaceEl.value, null, {
    renderer: 'canvas',
    devicePixelRatio: getChartPixelRatio()
  })

  const option = chartType.value === 'bar' ? buildBarOption() : buildLineOption()
  chartInstance.setOption(option, true)
  chartInstance.resize()
}

const buildBarOption = () => {
  return {
    grid: {
      left: 8,
      right: 8,
      top: 12,
      bottom: 8,
      containLabel: true
    },
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255, 255, 255, 0.98)',
      borderColor: '#e2e8f0',
      borderWidth: 1,
      borderRadius: 10,
      padding: [10, 14],
      textStyle: {
        color: '#334155',
        fontWeight: 600,
        fontSize: 12
      },
      extraCssText: 'box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);'
    },
    xAxis: {
      type: 'category',
      data: labels.value,
      axisLabel: {
        fontSize: 11,
        color: '#64748b',
        fontFamily: 'var(--ds-font)',
        fontWeight: 600
      },
      axisLine: { lineStyle: { color: '#e2e8f0', width: 2 } },
      axisTick: { show: false },
      nameLocation: 'middle',
      nameGap: 32,
      nameTextStyle: {
        fontSize: 10,
        color: '#94a3b8',
        fontWeight: 700,
        fontFamily: 'var(--ds-font)'
      }
    },
    yAxis: {
      type: 'value',
      axisLabel: { fontSize: 11, color: '#64748b', fontFamily: 'var(--ds-font)', fontWeight: 600 },
      axisLine: { show: false },
      splitLine: { lineStyle: { color: '#f1f5f9', type: 'dashed' } },
      axisTick: { show: false }
    },
    series: [{
      type: 'bar',
      data: props.distribution.map((d, i) => ({
        value: Number(d.count ?? d.value ?? 0),
        itemStyle: {
          color: barColors.value[i],
          borderRadius: [6, 6, 0, 0]
        }
      })),
      barWidth: '70%',
      animationDuration: 800,
      animationEasing: 'cubicOut',
      label: {
        show: values.value.some(v => v > 0),
        position: 'top',
        fontSize: 10,
        color: '#64748b',
        fontFamily: 'var(--ds-font)',
        fontWeight: 700,
        formatter: (p) => p.value > 0 ? String(p.value) : ''
      },
      emphasis: {
        itemStyle: {
          shadowBlur: 2,
          shadowColor: 'rgba(0, 0, 0, 0.06)'
        }
      }
    }]
  }
}

const buildLineOption = () => {
  return {
    grid: {
      left: 8,
      right: 8,
      top: 12,
      bottom: 8,
      containLabel: true
    },
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255, 255, 255, 0.98)',
      borderColor: '#e2e8f0',
      borderWidth: 1,
      borderRadius: 10,
      padding: [10, 14],
      textStyle: {
        color: '#334155',
        fontWeight: 600,
        fontSize: 12
      },
      extraCssText: 'box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);'
    },
    xAxis: {
      type: 'category',
      data: labels.value,
      boundaryGap: false,
      axisLabel: {
        fontSize: 11,
        color: '#64748b',
        fontFamily: 'var(--ds-font)',
        fontWeight: 600
      },
      axisLine: { lineStyle: { color: '#e2e8f0', width: 2 } },
      axisTick: { show: false }
    },
    yAxis: {
      type: 'value',
      axisLabel: { fontSize: 11, color: '#64748b', fontFamily: 'var(--ds-font)', fontWeight: 600 },
      axisLine: { show: false },
      splitLine: { lineStyle: { color: '#f1f5f9', type: 'dashed' } },
      axisTick: { show: false }
    },
    series: [{
      type: 'line',
      data: props.distribution.map((d, i) => ({
        value: Number(d.count ?? d.value ?? 0),
        itemStyle: { 
          color: barColors.value[i],
          borderWidth: 2,
          borderColor: '#fff'
        }
      })),
      smooth: 0.4,
      symbol: 'circle',
      symbolSize: 8,
      animationDuration: 1000,
      animationEasing: 'cubicOut',
      areaStyle: {
        color: {
          type: 'linear',
          x: 0, y: 0, x2: 0, y2: 1,
          colorStops: [
            { offset: 0, color: 'rgba(79, 70, 229, 0.2)' },
            { offset: 1, color: 'rgba(79, 70, 229, 0.02)' }
          ]
        }
      },
      lineStyle: { 
        color: '#4f46e5', 
        width: 3
      },
      label: {
        show: values.value.some(v => v > 0),
        position: 'top',
        fontSize: 10,
        color: '#64748b',
        fontFamily: 'var(--ds-font)',
        fontWeight: 700,
        formatter: (p) => p.value > 0 ? String(p.value) : ''
      },
      emphasis: {
        scale: true,
        itemStyle: {
          shadowColor: 'rgba(79, 70, 229, 0.1)',
          shadowBlur: 2
        }
      }
    }]
  }
}

const resizeObserver = ref(null)

onMounted(async () => {
  await nextTick()
  if (hasData.value) {
    await initChart()
    if (typeof ResizeObserver !== 'undefined' && chartEl.value) {
      resizeObserver.value = new ResizeObserver(() => {
        if (chartInstance) chartInstance.resize()
      })
      resizeObserver.value.observe(chartEl.value)
    }
  }
})

onUnmounted(() => {
  if (chartInstance) {
    chartInstance.dispose()
    chartInstance = null
  }
  if (resizeObserver.value) {
    resizeObserver.value.disconnect()
  }
})

watch([() => props.distribution, chartType], async ([distribution]) => {
  if (distribution?.length) {
    await nextTick()
    await initChart()
  }
}, { deep: true })
</script>


<style scoped>
.sdc {
  background: var(--ds-surface);
  border: 1.5px solid var(--ds-border);
  border-radius: var(--ds-radius-2xl);
  overflow: hidden;
  display: flex;
  flex-direction: column;
  min-height: 400px;
}

.dark .sdc {
  border-color: var(--ds-border-strong);
}

/* Header */
.sdc__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 0.75rem;
  padding: 1rem 1.125rem 0.75rem;
}

.sdc__header-left {}

.sdc__title {
  font-family: var(--ds-font-display);
  font-size: 0.875rem;
  font-weight: 800;
  color: var(--ds-text);
  margin: 0;
}

.dark .sdc__title { color: #f1f5f9; }

.sdc__subtitle {
  font-size: 0.7rem;
  color: var(--ds-text-muted);
  margin: 0.25rem 0 0;
  font-weight: 500;
}

.sdc__header-right {
  display: flex;
  gap: 0.25rem;
}

.sdc__toggle {
  width: 2rem;
  height: 2rem;
  border-radius: var(--ds-radius-md);
  border: 1px solid var(--ds-border);
  background: transparent;
  color: var(--ds-text-muted);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: color 0.12s ease, background-color 0.12s ease, border-color 0.12s ease, box-shadow 0.12s ease, transform 0.12s ease;
}

.dark .sdc__toggle {
  background: var(--ds-gray-800);
  border-color: var(--ds-border-strong);
}

.sdc__toggle:hover {
  background: var(--ds-gray-100);
  color: var(--ds-text);
}

.dark .sdc__toggle:hover { background: var(--ds-gray-700); }

.sdc__toggle--active {
  background: var(--ds-primary-soft);
  border-color: var(--ds-primary-border);
  color: var(--ds-primary);
}

.dark .sdc__toggle--active { background: rgba(79, 70, 229, 0.1); }


/* Chart */
.sdc__chart-wrap {
  flex: 1;
  min-height: 320px;
  padding: 0.5rem 0.75rem;
  position: relative;
}

.sdc__canvas {
  width: 100%;
  height: 320px;
}

/* Loading */
.sdc__loading {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: flex-end;
  justify-content: center;
  padding: 0.5rem 1rem;
  gap: 0.375rem;
}

.sdc__skeleton-bars {
  display: flex;
  align-items: flex-end;
  justify-content: center;
  gap: 0.375rem;
  width: 100%;
  height: 100%;
}

.sdc__skeleton-bar {
  flex: 1;
  max-width: 40px;
  background: linear-gradient(90deg, var(--ds-gray-100) 25%, var(--ds-gray-200) 50%, var(--ds-gray-100) 75%);
  background-size: 200% 100%;
  animation: sdcShimmer 1.5s infinite;
  border-radius: 4px 4px 0 0;
}

.dark .sdc__skeleton-bar {
  background: linear-gradient(90deg, var(--ds-gray-800) 25%, var(--ds-gray-700) 50%, var(--ds-gray-800) 75%);
  background-size: 200% 100%;
}

@keyframes sdcShimmer {
  0% { background-position: -200% 0; transform: translateZ(0); }
  100% { background-position: 200% 0; transform: translateZ(0); }
}

/* Empty */
.sdc__empty {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  color: var(--ds-text-muted);
  font-size: 0.8rem;
}


.sdc__empty p { margin: 0; }

/* Stats */
.sdc__stats {
  display: flex;
  gap: 0;
  border-top: 1px solid var(--ds-border);
  padding: 0.625rem 1rem;
  background: var(--ds-gray-50);
}

.dark .sdc__stats {
  border-top-color: var(--ds-border-strong);
  background: var(--ds-gray-800);
}

.sdc__stat {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.125rem;
  border-right: 1px solid var(--ds-border);
  padding: 0 0.5rem;
}

.dark .sdc__stat { border-right-color: var(--ds-border-strong); }
.sdc__stat:last-child { border-right: none; }

.sdc__stat-label {
  font-size: 0.6rem;
  font-weight: 700;
  color: var(--ds-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.06em;
  white-space: nowrap;
}

.sdc__stat-val {
  font-family: var(--ds-font-display);
  font-size: 1.125rem;
  font-weight: 800;
  color: var(--ds-text);
  line-height: 1;
}

.dark .sdc__stat-val { color: #f1f5f9; }

.sdc__stat-val--success { color: var(--ds-success); }
.sdc__stat-val--warning { color: var(--ds-warning); }
.sdc__stat-val--danger { color: var(--ds-danger); }

/* Legend */
.sdc__legend {
  display: flex;
  gap: 1rem;
  justify-content: center;
  padding: 0.5rem 1rem;
  border-top: 1px solid var(--ds-border);
  background: var(--ds-gray-50);
}

.dark .sdc__legend {
  border-top-color: var(--ds-border-strong);
  background: var(--ds-gray-800);
}

.sdc__legend-item {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  font-size: 0.75rem;
  font-weight: 700;
}


.sdc__legend-item--fail { color: var(--ds-danger); }
.sdc__legend-item--pass { color: var(--ds-success); }
</style>