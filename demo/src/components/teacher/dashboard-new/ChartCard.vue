<template>
  <div class="ntd-chart-card">
    <div v-if="$slots.header || title" class="ntd-chart-card__header">
      <slot name="header">
        <div class="ntd-chart-card__title-group">
          <LucideIcon v-if="icon" :name="icon" size="18" class="ntd-chart-card__icon" />
          <h3 class="ntd-chart-card__title">{{ title }}</h3>
        </div>
        <div v-if="$slots.actions" class="ntd-chart-card__actions">
          <slot name="actions" />
        </div>
      </slot>
    </div>

    <div v-if="$slots.toolbar" class="ntd-chart-card__toolbar">
      <slot name="toolbar" />
    </div>

    <div
      ref="chartRef"
      class="ntd-chart-card__body"
      :style="{ height: computedHeight }"
    >
      <div v-if="$slots.skeleton" class="ntd-chart-card__skeleton">
        <slot name="skeleton" />
      </div>
    </div>

    <div v-if="$slots.footer" class="ntd-chart-card__footer">
      <slot name="footer" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick, computed } from 'vue'
// LucideIcon registered globally

const props = defineProps({
  title: { type: String, default: '' },
  icon: { type: String, default: '' },
  height: { type: String, default: '300px' },
  noPadding: { type: Boolean, default: false }
})

const computedHeight = computed(() => props.height)

const chartRef = ref(null)
let chartInstance = null
let resizeObserver = null

const getChartInstance = () => chartInstance
const setChartInstance = (instance) => { chartInstance = instance }

const initChart = async (option) => {
  await nextTick()
  if (!chartRef.value) return

  const echartsCore = await import('../../utils/echartsCore.js')
  const echarts = echartsCore.default || echartsCore

  if (!chartInstance) {
    chartInstance = echarts.init(chartRef.value)
  }
  chartInstance.setOption(option, true)
}

const resizeChart = () => {
  if (chartInstance) {
    chartInstance.resize()
  }
}

onMounted(() => {
  if (chartRef.value) {
    resizeObserver = new ResizeObserver(() => {
      resizeChart()
    })
    resizeObserver.observe(chartRef.value)
  }
})

onUnmounted(() => {
  if (resizeObserver) {
    resizeObserver.disconnect()
    resizeObserver = null
  }
  if (chartInstance) {
    chartInstance.dispose()
    chartInstance = null
  }
})

defineExpose({ initChart, resizeChart, getChartInstance, setChartInstance })
</script>

<style scoped>
.ntd-chart-card {
  background: var(--ds-surface, #ffffff);
  border: 1px solid var(--ds-border, #e5e7eb);
  border-radius: var(--ds-radius-xl, 16px);
  overflow: hidden;
  transition: box-shadow 0.2s ease;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.ntd-chart-card:hover {
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06);
}

.ntd-chart-card__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1.125rem 1.375rem 0.75rem;
  gap: 1rem;
}

.ntd-chart-card__title-group {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.ntd-chart-card__icon {
  color: var(--ds-primary, #4f46e5);
  flex-shrink: 0;
}

.ntd-chart-card__title {
  font-size: 0.9375rem;
  font-weight: 700;
  color: var(--ds-text, #1f2937);
  margin: 0;
  line-height: 1.4;
}

.ntd-chart-card__actions {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.ntd-chart-card__toolbar {
  padding: 0 1.375rem 0.5rem;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.ntd-chart-card__body {
  position: relative;
  width: 100%;
  flex: 1;
  min-height: 0;
}

.ntd-chart-card__skeleton {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.ntd-chart-card__footer {
  padding: 0.75rem 1.375rem;
  border-top: 1px solid var(--ds-border, #e5e7eb);
  background: var(--ds-gray-50, #f9fafb);
}

:deep(.ntd-chart-card__body canvas) {
  border-radius: 0 0 16px 16px;
}
</style>
