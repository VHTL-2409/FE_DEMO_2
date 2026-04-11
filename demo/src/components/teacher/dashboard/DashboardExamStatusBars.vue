<template>
  <div class="dsb">
    <div class="dsb__header">
      <h3 class="dsb__title">{{ copy.title }}</h3>
      <p class="dsb__sub">{{ copy.subLine(total) }}</p>
    </div>

    <div v-if="total === 0" class="dsb__empty">
      <LucideIcon name="bar_chart" class="dsb__empty-icon" />
      <p class="dsb__empty-text">{{ copy.empty }}</p>
    </div>

    <div v-else class="dsb__body">
      <div
        class="dsb__bars"
        role="img"
        :aria-label="ariaLabel"
      >
        <div
          v-for="row in legendRows"
          :key="row.key"
          class="dsb__row"
        >
          <div class="dsb__row-label">
            <span class="dsb__row-dot" :style="{ background: row.color }" />
            <span class="dsb__row-name">{{ row.label }}</span>
          </div>
          <div class="dsb__row-track">
            <div
              class="dsb__row-fill"
              :style="{
                width: pct(row.n) + '%',
                background: row.color
              }"
            />
          </div>
          <span class="dsb__row-val">{{ row.n }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const copy = {
  title: 'Ph\u00e2n b\u1ed1 tr\u1ea1ng th\u00e1i \u0111\u1ec1',
  subLine: (n) =>
    `T\u1ed5ng c\u1ed9ng ${n} \u0111\u1ec1 thi \u0111ang qu\u1ea3n l\u00fd`,
  empty:
    'Ch\u01b0a c\u00f3 d\u1eef li\u1ec7u. T\u1ea1o \u0111\u1ec1 thi \u0111\u1ec3 th\u1ea5y bi\u1ec3u \u0111\u1ed3.',
  ariaPrefix: 'Bi\u1ec3u \u0111\u1ed3 thanh tr\u1ea1ng th\u00e1i.',
  ended: '\u0110\u00e3 k\u1ebft th\u00fac',
  started: '\u0110ang di\u1ec5n ra',
  upcoming: 'Ch\u01b0a b\u1eaft \u0111\u1ea7u',
  draft: 'B\u1ea3n nh\u00e1p'
}

const props = defineProps({
  counts: {
    type: Object,
    default: () => ({
      draft: 0,
      upcoming: 0,
      started: 0,
      ended: 0
    })
  }
})

const legendRows = computed(() => {
  const c = props.counts || {}
  return [
    { key: 'ended', label: copy.ended, color: 'var(--ds-success)', n: Number(c.ended) || 0 },
    { key: 'started', label: copy.started, color: 'var(--ds-accent)', n: Number(c.started) || 0 },
    { key: 'upcoming', label: copy.upcoming, color: 'var(--ds-info)', n: Number(c.upcoming) || 0 },
    { key: 'draft', label: copy.draft, color: 'var(--ds-text-muted)', n: Number(c.draft) || 0 }
  ]
})

const total = computed(() =>
  legendRows.value.reduce((s, r) => s + r.n, 0)
)

const ariaLabel = computed(() => {
  const parts = legendRows.value
    .filter(r => r.n > 0)
    .map(r => `${r.label}: ${r.n}`)
  return `${copy.ariaPrefix} ${parts.join(', ')}`
})

function pct(n) {
  return total.value ? Math.round((n / total.value) * 1000) / 10 : 0
}

</script>

<style scoped>
.dsb {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.dsb__header {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.dsb__title {
  font-family: var(--ds-font-display);
  font-size: 1rem;
  font-weight: 700;
  color: var(--ds-text);
  margin: 0;
}

.dsb__sub {
  font-size: 0.75rem;
  color: var(--ds-text-muted);
  margin: 0;
}

.dsb__empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  padding: 2rem 1rem;
  text-align: center;
  border-radius: var(--ds-radius-xl);
  border: 1px dashed var(--ds-border);
  background: var(--ds-gray-50);
}

.dark .dsb__empty {
  background: var(--ds-gray-900);
}

.dsb__empty-icon {
  color: var(--ds-text-muted);
  width: 2rem;
  height: 2rem;
}

.dsb__empty-text {
  margin: 0;
  font-size: 0.875rem;
  color: var(--ds-text-muted);
  max-width: 16rem;
  line-height: 1.45;
}

.dsb__body {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.dsb__bars {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.dsb__row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(5.5rem, 48%) 2.25rem;
  align-items: center;
  gap: 0.5rem 0.85rem;
}

.dsb__row-label {
  display: flex;
  align-items: center;
  gap: 0.45rem;
  min-width: 0;
}

.dsb__row-dot {
  width: 8px;
  height: 8px;
  border-radius: 2px;
  flex-shrink: 0;
}

.dsb__row-name {
  font-size: 0.78rem;
  color: var(--ds-text-secondary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.dsb__row-track {
  height: 10px;
  border-radius: 999px;
  background: color-mix(in srgb, var(--ds-border) 50%, transparent);
  overflow: hidden;
}

.dsb__row-fill {
  height: 100%;
  border-radius: 999px;
  min-width: 0;
  transition: width 0.2s ease;
}

@media (prefers-reduced-motion: reduce) {
  .dsb__row-fill {
    transition: none;
  }
}

.dsb__row-val {
  font-size: 0.78rem;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  color: var(--ds-text);
  text-align: right;
}
</style>
