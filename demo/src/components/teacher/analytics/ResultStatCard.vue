<template>
  <div
    class="rscard"
    :class="[`rscard--${accent}`, { 'rscard--loading': loading }]"
  >
    <!-- Loading skeleton -->
    <template v-if="loading">
      <div class="rscard__skeleton-header">
        <div class="rscard__skeleton rscard__skeleton--icon" />
        <div class="rscard__skeleton rscard__skeleton--label" />
      </div>
      <div class="rscard__skeleton rscard__skeleton--value" />
      <div class="rscard__skeleton rscard__skeleton--sub" />
    </template>

    <!-- Content -->
    <template v-else>
      <div class="rscard__header">
        <div class="rscard__icon-wrap" :class="`rscard__icon-wrap--${accent}`">
          <LucideIcon :name="icon" />
        </div>
        <span class="rscard__label">{{ label }}</span>
      </div>

      <div class="rscard__body">
        <p class="rscard__value">{{ formattedValue }}</p>

        <!-- Trend -->
        <div v-if="trend !== null && trend !== undefined" class="rscard__trend" :class="trendClass">
          <LucideIcon :name="trendIcon" />
          <span class="rscard__trend-val">{{ Math.abs(trend) }}%</span>
        </div>
      </div>

      <p v-if="sub" class="rscard__sub">{{ sub }}</p>
    </template>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  label: { type: String, default: '' },
  value: { type: [Number, String], default: 0 },
  icon: { type: String, default: 'analytics' },
  trend: { type: Number, default: null },
  trendDirection: { type: String, default: 'neutral' },
  accent: {
    type: String,
    default: 'primary',
    validator: (v) => ['primary', 'success', 'warning', 'danger', 'info', 'neutral'].includes(v)
  },
  sub: { type: String, default: '' },
  loading: { type: Boolean, default: false }
})

const formattedValue = computed(() => {
  const v = props.value
  if (typeof v === 'number') {
    if (v >= 1000) return v.toLocaleString('vi-VN')
    if (v % 1 !== 0) return v.toFixed(1)
    return String(v)
  }
  return String(v)
})

const trendClass = computed(() => {
  const dir = props.trendDirection
  if (dir === 'up') return 'rscard__trend--up'
  if (dir === 'down') return 'rscard__trend--down'
  return 'rscard__trend--neutral'
})

const trendIcon = computed(() => {
  const dir = props.trendDirection
  if (dir === 'up') return 'trending_up'
  if (dir === 'down') return 'trending_down'
  return 'trending_flat'
})
</script>


<style scoped>
.rscard {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  padding: 1.125rem 1.25rem;
  background: var(--ds-surface);
  border: 1.5px solid var(--ds-border);
  border-radius: var(--ds-radius-2xl);
  transition: color 0.2s ease, background-color 0.2s ease, border-color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease;
  position: relative;
  overflow: hidden;
  min-width: 0;
}

.dark .rscard {
  border-color: var(--ds-border-strong);
}

/* Left accent stripe */
.rscard::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 4px;
  border-radius: var(--ds-radius-2xl) 0 0 var(--ds-radius-2xl);
}

.rscard--primary::before { background: var(--ds-primary); }
.rscard--success::before { background: var(--ds-success); }
.rscard--warning::before { background: var(--ds-warning); }
.rscard--danger::before { background: var(--ds-danger); }
.rscard--info::before { background: var(--ds-info); }
.rscard--neutral::before { background: var(--ds-gray-400); }

/* Hover */
.rscard:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
}

.rscard--primary:hover { border-color: var(--ds-primary-border); box-shadow: 0 4px 16px rgba(79, 70, 229, 0.1); }
.rscard--success:hover { border-color: rgba(22, 163, 74, 0.3); box-shadow: 0 4px 16px rgba(22, 163, 74, 0.08); }
.rscard--warning:hover { border-color: rgba(217, 119, 6, 0.3); box-shadow: 0 4px 16px rgba(217, 119, 6, 0.08); }
.rscard--danger:hover { border-color: rgba(220, 38, 38, 0.3); box-shadow: 0 4px 16px rgba(220, 38, 38, 0.08); }

/* Header */
.rscard__header {
  display: flex;
  align-items: center;
  gap: 0.625rem;
}

.rscard__icon-wrap {
  width: 36px;
  height: 36px;
  border-radius: var(--ds-radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}


.rscard__icon-wrap--primary { background: var(--ds-primary-soft); color: var(--ds-primary); }
.rscard__icon-wrap--success { background: var(--ds-success-soft); color: var(--ds-success); }
.rscard__icon-wrap--warning { background: rgba(234, 179, 8, 0.1); color: #d97706; }
.rscard__icon-wrap--danger { background: var(--ds-danger-soft); color: var(--ds-danger); }
.rscard__icon-wrap--info { background: var(--ds-info-soft); color: var(--ds-info); }
.rscard__icon-wrap--neutral { background: var(--ds-gray-100); color: var(--ds-gray-500); }

.dark .rscard__icon-wrap--neutral { background: var(--ds-gray-700); }

.rscard__label {
  font-size: 0.75rem;
  font-weight: 700;
  color: var(--ds-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.06em;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* Body */
.rscard__body {
  display: flex;
  align-items: baseline;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.rscard__value {
  font-family: var(--ds-font-display);
  font-size: 1.875rem;
  font-weight: 900;
  color: var(--ds-text);
  margin: 0;
  line-height: 1;
  letter-spacing: -0.02em;
}

.dark .rscard__value { color: #f1f5f9; }

/* Trend */
.rscard__trend {
  display: inline-flex;
  align-items: center;
  gap: 0.2rem;
  padding: 0.2rem 0.5rem;
  border-radius: var(--ds-radius-full);
  font-size: 0.75rem;
  font-weight: 800;
}

.rscard__trend--up {
  background: var(--ds-success-soft);
  color: var(--ds-success);
}

.rscard__trend--down {
  background: var(--ds-danger-soft);
  color: var(--ds-danger);
}

.rscard__trend--neutral {
  background: var(--ds-gray-100);
  color: var(--ds-text-muted);
}

.dark .rscard__trend--neutral { background: var(--ds-gray-700); }

.rscard__trend-icon { font-size: 1rem; }
.rscard__trend-val { font-size: 0.75rem; }

/* Sub */
.rscard__sub {
  font-size: 0.75rem;
  color: var(--ds-text-muted);
  margin: 0;
  font-weight: 500;
}

/* Loading skeleton */
.rscard__skeleton-header {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 0.25rem;
}

.rscard__skeleton {
  background: linear-gradient(90deg, var(--ds-gray-100) 25%, var(--ds-gray-200) 50%, var(--ds-gray-100) 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
  border-radius: var(--ds-radius-md);
}

.dark .rscard__skeleton {
  background: linear-gradient(90deg, var(--ds-gray-800) 25%, var(--ds-gray-700) 50%, var(--ds-gray-800) 75%);
  background-size: 200% 100%;
}

.rscard__skeleton--icon { width: 36px; height: 36px; border-radius: var(--ds-radius-lg); }
.rscard__skeleton--label { width: 80px; height: 12px; }
.rscard__skeleton--value { width: 60px; height: 28px; }
.rscard__skeleton--sub { width: 100px; height: 10px; }
</style>
@media (prefers-reduced-motion: reduce) {
  * {
    transition-duration: 0.01ms !important;
    animation-duration: 0.01ms !important;
  }
}