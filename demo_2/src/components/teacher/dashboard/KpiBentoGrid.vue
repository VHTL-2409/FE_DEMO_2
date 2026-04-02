<template>
  <div class="kpi-bento cosmos-bento cosmos-bento--4 cosmos-stagger">
    <div
      v-for="(stat, i) in stats"
      :key="stat.label"
      class="kpi-card cosmos-card p-5 cosmos-lift cosmos-reveal"
      :class="{ 'cosmos-card--teal': stat.variant === 'teal', 'cosmos-card--rose': stat.variant === 'rose' }"
    >
      <!-- Icon -->
      <div class="flex items-start justify-between mb-4">
        <div
          class="w-12 h-12 rounded-xl flex items-center justify-center"
          :style="`background: ${stat.bg};`"
        >
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" :stroke="stat.color" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">{{ stat.iconPath }}</svg>
        </div>
        <!-- Trend -->
        <span
          v-if="stat.trend !== null && stat.trend !== undefined"
          class="cosmos-badge cosmos-badge--sm"
          :style="stat.trend > 0
            ? 'background: var(--cosmos-success-soft); color: var(--cosmos-success); border: 1px solid var(--cosmos-success-border);'
            : stat.trend < 0
              ? 'background: var(--cosmos-danger-soft); color: var(--cosmos-danger); border: 1px solid var(--cosmos-danger-border);'
              : 'background: var(--cosmos-surface-muted); color: var(--cosmos-text-muted); border: 1px solid var(--cosmos-border);'"
        >
          <svg v-if="stat.trend > 0" width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"><polyline points="18 15 12 9 6 15"/></svg>
          <svg v-else-if="stat.trend < 0" width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"><polyline points="6 9 12 15 18 9"/></svg>
          {{ Math.abs(stat.trend) }}%
        </span>
      </div>

      <!-- Value -->
      <p class="text-3xl font-black cosmos-heading-display mb-1" :style="`color: var(--cosmos-text); font-family: var(--cosmos-font-mono);`">
        {{ stat.value }}
      </p>

      <!-- Label -->
      <p class="text-xs font-semibold uppercase tracking-wide" style="color: var(--cosmos-text-muted);">
        {{ stat.label }}
      </p>

      <!-- Progress bar (optional) -->
      <div v-if="stat.progress !== undefined" class="mt-3 cosmos-progress" style="height: 4px;">
        <div
          class="cosmos-progress__bar"
          :style="`width: ${stat.progress}%;`"
          :class="{
            'cosmos-progress__bar--success': stat.variant === 'success',
            'cosmos-progress__bar--warning': stat.variant === 'warning',
            'cosmos-progress__bar--danger': stat.variant === 'danger'
          }"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
const props = defineProps({
  stats: {
    type: Array,
    default: () => [
      { iconPath: '<circle cx="12" cy="12" r="10"/><path d="M9.09 9a3 3 0 0 1 5.83 1c0 2-3 3-3 3"/><line x1="12" y1="17" x2="12.01" y2="17"/>', label: 'Tong ky thi', value: '0', variant: 'teal', bg: 'var(--cosmos-teal-soft)', color: 'var(--cosmos-teal)', trend: null },
      { iconPath: '<circle cx="12" cy="12" r="10"/><polygon points="10 8 16 12 10 16 10 8"/>', label: 'Dang dien ra', value: '0', variant: 'success', bg: 'var(--cosmos-success-soft)', color: 'var(--cosmos-success)', trend: null },
      { iconPath: '<circle cx="12" cy="12" r="10"/><polyline points="9 11 12 14 15 11"/>', label: 'Da ket thuc', value: '0', variant: 'neutral', bg: 'var(--cosmos-surface-muted)', color: 'var(--cosmos-text-muted)', trend: null },
      { iconPath: '<circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/>', label: 'Sap dien ra', value: '0', variant: 'rose', bg: 'var(--cosmos-rose-soft)', color: 'var(--cosmos-rose)', trend: null }
    ]
  },
  alertCount: { type: Number, default: 0 }
})
</script>

<style scoped>
.kpi-card {
  position: relative;
  overflow: hidden;
}

.kpi-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 2px;
  background: linear-gradient(90deg, var(--cosmos-teal), var(--cosmos-rose));
  opacity: 0;
  transition: opacity 0.25s var(--cosmos-ease-out);
}

.kpi-card:hover::before {
  opacity: 1;
}

.kpi-card.cosmos-card--teal::before {
  opacity: 1;
}
</style>
