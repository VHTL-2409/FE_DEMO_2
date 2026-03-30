<template>
  <div class="td-schedule">
    <!-- Header -->
    <div class="td-schedule__header">
      <div>
        <h3 class="td-schedule__title">Lịch tuần này</h3>
        <p class="td-schedule__sub">Phân bố đề thi theo ngày</p>
      </div>
      <div class="td-schedule__legend">
        <span class="td-schedule__legend-item">
          <span class="td-schedule__legend-dot td-schedule__legend-dot--start" />
          Bắt đầu
        </span>
        <span class="td-schedule__legend-item">
          <span class="td-schedule__legend-dot td-schedule__legend-dot--end" />
          Kết thúc
        </span>
      </div>
    </div>

    <!-- Timeline -->
    <div class="td-schedule__timeline">
      <div
        v-for="(day, i) in scheduleSeries.labels"
        :key="i"
        class="td-schedule__day"
        :class="{ 'td-schedule__day--today': i === todayIndex }"
      >
        <!-- Day label -->
        <div class="td-schedule__day-label">
          <span class="td-schedule__day-name">{{ dayNames[i] }}</span>
          <span class="td-schedule__day-num">{{ dayNumbers[i] }}</span>
        </div>

        <!-- Bars -->
        <div class="td-schedule__bars">
          <!-- Start bar -->
          <div
            v-if="scheduleSeries.starts[i] > 0"
            class="td-schedule__bar td-schedule__bar--start"
            :style="{ height: barHeight(scheduleSeries.starts[i]) }"
            :title="`${scheduleSeries.starts[i]} đề bắt đầu`"
          >
            <span class="td-schedule__bar-label">{{ scheduleSeries.starts[i] }}</span>
          </div>
          <div v-else class="td-schedule__bar td-schedule__bar--empty" />

          <!-- End bar -->
          <div
            v-if="scheduleSeries.ends[i] > 0"
            class="td-schedule__bar td-schedule__bar--end"
            :style="{ height: barHeight(scheduleSeries.ends[i]) }"
            :title="`${scheduleSeries.ends[i]} đề kết thúc`"
          >
            <span class="td-schedule__bar-label">{{ scheduleSeries.ends[i] }}</span>
          </div>
          <div v-else class="td-schedule__bar td-schedule__bar--empty" />
        </div>
      </div>
    </div>

    <!-- Summary stats -->
    <div class="td-schedule__stats">
      <div class="td-schedule__stat">
        <span class="td-schedule__stat-value">{{ totalStarts }}</span>
        <span class="td-schedule__stat-label">Kỳ thi bắt đầu tuần này</span>
      </div>
      <div class="td-schedule__stat-divider" />
      <div class="td-schedule__stat">
        <span class="td-schedule__stat-value">{{ totalEnds }}</span>
        <span class="td-schedule__stat-label">Kỳ thi kết thúc tuần này</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  scheduleSeries: {
    type: Object,
    default: () => ({
      labels: [],
      starts: [],
      ends: []
    })
  }
})

const dayNames = computed(() =>
  props.scheduleSeries.labels.map(l => l.split('/')[0])
)

const dayNumbers = computed(() =>
  props.scheduleSeries.labels.map(l => l.split('/')[1])
)

const todayIndex = computed(() => {
  const today = new Date()
  return props.scheduleSeries.labels.findIndex(l => {
    const [d, m] = l.split('/').map(Number)
    return d === today.getDate()
  })
})

const maxVal = computed(() => {
  const all = [...(props.scheduleSeries.starts || []), ...(props.scheduleSeries.ends || [])]
  return Math.max(...all, 1)
})

const barHeight = (val) => {
  const min = 8
  const max = 56
  return `${min + ((val / maxVal.value) * (max - min))}px`
}

const totalStarts = computed(() =>
  (props.scheduleSeries.starts || []).reduce((s, v) => s + v, 0)
)

const totalEnds = computed(() =>
  (props.scheduleSeries.ends || []).reduce((s, v) => s + v, 0)
)
</script>

<style scoped>
.td-schedule {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

/* Header */
.td-schedule__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
}

.td-schedule__title {
  font-family: var(--ds-font-display);
  font-size: 1rem;
  font-weight: 700;
  color: var(--ds-text);
  margin: 0;
  line-height: 1.2;
}

.dark .td-schedule__title {
  color: #f1f5f9;
}

.td-schedule__sub {
  font-size: 0.75rem;
  color: var(--ds-text-muted);
  margin: 0.25rem 0 0;
}

.td-schedule__legend {
  display: flex;
  gap: 1rem;
  align-items: center;
}

.td-schedule__legend-item {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  font-size: 0.7rem;
  font-weight: 600;
  color: var(--ds-text-muted);
}

.td-schedule__legend-dot {
  width: 8px;
  height: 8px;
  border-radius: 2px;
}

.td-schedule__legend-dot--start {
  background: #2dd4bf;
}

.td-schedule__legend-dot--end {
  background: var(--ds-accent);
}

/* Timeline */
.td-schedule__timeline {
  display: flex;
  align-items: flex-end;
  gap: 0.5rem;
  height: 80px;
  padding: 0 0.25rem;
}

.td-schedule__day {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.5rem;
  min-width: 0;
}

.td-schedule__day--today .td-schedule__day-label {
  background: var(--ds-primary-soft);
  border-radius: var(--ds-radius-md);
  padding: 0.125rem 0.375rem;
}

.td-schedule__day-label {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0;
  width: 100%;
}

.td-schedule__day-name {
  font-size: 0.6rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.06em;
  color: var(--ds-text-muted);
}

.td-schedule__day-num {
  font-size: 0.8rem;
  font-weight: 800;
  color: var(--ds-text);
  line-height: 1.1;
}

.dark .td-schedule__day-num {
  color: #f1f5f9;
}

.td-schedule__day--today .td-schedule__day-name {
  color: var(--ds-primary);
}

.td-schedule__day--today .td-schedule__day-num {
  color: var(--ds-primary);
}

.td-schedule__bars {
  display: flex;
  gap: 0.25rem;
  align-items: flex-end;
  flex: 1;
  width: 100%;
  justify-content: center;
}

.td-schedule__bar {
  flex: 1;
  border-radius: 4px 4px 2px 2px;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding-top: 2px;
  transition: height 0.3s ease;
  min-height: 8px;
  max-width: 20px;
}

.td-schedule__bar--start {
  background: linear-gradient(180deg, #2dd4bf 0%, #14b8a6 100%);
}

.td-schedule__bar--end {
  background: linear-gradient(180deg, var(--ds-accent) 0%, #d97706 100%);
}

.td-schedule__bar--empty {
  background: var(--ds-gray-100);
  min-height: 4px;
  height: 4px;
}

.dark .td-schedule__bar--empty {
  background: var(--ds-gray-700);
}

.td-schedule__bar-label {
  font-size: 0.6rem;
  font-weight: 800;
  color: white;
  line-height: 1;
}

/* Stats */
.td-schedule__stats {
  display: flex;
  align-items: center;
  gap: 0;
  padding: 0.875rem 1rem;
  background: var(--ds-gray-50);
  border-radius: var(--ds-radius-xl);
  border: 1px solid var(--ds-border);
}

.dark .td-schedule__stats {
  background: var(--ds-gray-800);
}

.td-schedule__stat {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.125rem;
  flex: 1;
}

.td-schedule__stat-value {
  font-family: var(--ds-font-display);
  font-size: 1.5rem;
  font-weight: 800;
  color: var(--ds-text);
  line-height: 1;
}

.dark .td-schedule__stat-value {
  color: #f1f5f9;
}

.td-schedule__stat:first-child .td-schedule__stat-value {
  color: #14b8a6;
}

.td-schedule__stat:last-child .td-schedule__stat-value {
  color: var(--ds-accent);
}

.td-schedule__stat-label {
  font-size: 0.65rem;
  font-weight: 600;
  color: var(--ds-text-muted);
  text-align: center;
}

.td-schedule__stat-divider {
  width: 1px;
  height: 36px;
  background: var(--ds-border);
  margin: 0 0.5rem;
}
</style>
