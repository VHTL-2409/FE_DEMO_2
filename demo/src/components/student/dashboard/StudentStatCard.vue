<template>
  <div
    class="ssc"
    :class="[`ssc--${accent}`, { 'ssc--loading': loading }]"
  >
    <template v-if="loading">
      <div class="ssc__skel-icon" />
      <div class="ssc__skel-label" />
      <div class="ssc__skel-value" />
    </template>
    <template v-else>
      <div class="ssc__icon-wrap" :class="`ssc__icon-wrap--${accent}`">
        <LucideIcon :name="icon" />
      </div>
      <div class="ssc__body">
        <span class="ssc__label">{{ label }}</span>
        <p class="ssc__value">{{ formattedValue }}</p>
        <span v-if="sub" class="ssc__sub">{{ sub }}</span>
      </div>
    </template>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  label: { type: String, default: '' },
  value: { type: [Number, String], default: 0 },
  icon: { type: String, default: 'analytics' },
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
</script>


<style scoped>
/* GPU-accelerated fadeInUp animation */
@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(10px) translateZ(0); }
  to   { opacity: 1; transform: translateY(0) translateZ(0); }
}

.ssc {
  display: flex;
  align-items: center;
  gap: 0.875rem;
  padding: 1rem 1.125rem;
  background: var(--ds-surface);
  border: 1.5px solid var(--ds-border);
  border-radius: var(--ds-radius-2xl);
  transition:
    transform 0.25s cubic-bezier(0.34, 1.56, 0.64, 1),
    box-shadow 0.25s ease,
    border-color 0.2s ease;
  min-width: 0;
  cursor: default;
  /* GPU optimization - animate once */
  animation: fadeInUp 0.45s cubic-bezier(0.16, 1, 0.3, 1) both;
  will-change: transform, opacity;
  transform: translateZ(0);
  /* Optimize paint */
  contain: layout style;
  content-visibility: auto;
}

/* Staggered entrance — tighter delays for faster perceived load (total spread: 0.21s) */
.sdl__kpis .ssc:nth-child(1) { animation-delay: 0.12s; }
.sdl__kpis .ssc:nth-child(2) { animation-delay: 0.16s; }
.sdl__kpis .ssc:nth-child(3) { animation-delay: 0.20s; }
.sdl__kpis .ssc:nth-child(4) { animation-delay: 0.24s; }

@media (prefers-reduced-motion: reduce) {
  .ssc { animation-duration: 0.01ms !important; }
  .ssc__icon-wrap { transition-duration: 0.01ms !important; }
}

.dark .ssc {
  border-color: var(--ds-border-strong);
}

.ssc:hover {
  transform: translateY(-3px) scale(1.02);
  box-shadow: 0 12px 28px rgba(15, 23, 42, 0.1);
}

.ssc--primary:hover { border-color: var(--ds-primary-border); box-shadow: 0 12px 28px rgba(79, 70, 229, 0.15); }
.ssc--success:hover { border-color: rgba(22, 163, 74, 0.3); box-shadow: 0 12px 28px rgba(22, 163, 74, 0.12); }
.ssc--warning:hover { border-color: rgba(217, 119, 6, 0.3); box-shadow: 0 12px 28px rgba(217, 119, 6, 0.12); }

/* Icon */
.ssc__icon-wrap {
  width: 44px;
  height: 44px;
  border-radius: var(--ds-radius-xl);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: transform 0.3s cubic-bezier(0.34, 1.56, 0.64, 1), background 0.25s ease;
}

.ssc:hover .ssc__icon-wrap {
  transform: scale(1.15) rotate(-6deg);
}

.ssc__icon-wrap--primary { background: var(--ds-primary-soft); color: var(--ds-primary); }
.ssc__icon-wrap--success { background: var(--ds-success-soft); color: var(--ds-success); }
.ssc__icon-wrap--warning { background: rgba(234, 179, 8, 0.1); color: var(--ds-warning); }
.ssc__icon-wrap--danger { background: var(--ds-danger-soft); color: var(--ds-danger); }
.ssc__icon-wrap--info { background: var(--ds-info-soft); color: var(--ds-info); }
.ssc__icon-wrap--neutral { background: var(--ds-gray-100); color: var(--ds-gray-500); }

.dark .ssc__icon-wrap--neutral { background: var(--ds-gray-700); }

/* Body */
.ssc__body {
  display: flex;
  flex-direction: column;
  gap: 0.125rem;
  min-width: 0;
}

.ssc__label {
  font-size: 0.65rem;
  font-weight: 700;
  color: var(--ds-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.06em;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.ssc__value {
  font-family: var(--ds-font-display);
  font-size: 1.5rem;
  font-weight: 900;
  color: var(--ds-text);
  margin: 0;
  line-height: 1;
  letter-spacing: -0.02em;
}

.dark .ssc__value { color: var(--ds-text); }

.ssc__sub {
  font-size: 0.7rem;
  color: var(--ds-text-muted);
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* Skeleton */
.ssc__skel-icon { width: 44px; height: 44px; border-radius: var(--ds-radius-xl); background: linear-gradient(90deg, var(--ds-gray-100) 25%, var(--ds-gray-200) 50%, var(--ds-gray-100) 75%); background-size: 200% 100%; animation: sscShimmer 1.5s infinite; flex-shrink: 0; }
.ssc__skel-label { width: 60px; height: 10px; border-radius: 4px; background: linear-gradient(90deg, var(--ds-gray-100) 25%, var(--ds-gray-200) 50%, var(--ds-gray-100) 75%); background-size: 200% 100%; animation: sscShimmer 1.5s infinite; }
.ssc__skel-value { width: 40px; height: 24px; border-radius: 4px; background: linear-gradient(90deg, var(--ds-gray-100) 25%, var(--ds-gray-200) 50%, var(--ds-gray-100) 75%); background-size: 200% 100%; animation: sscShimmer 1.5s infinite; }

.dark .ssc__skel-icon, .dark .ssc__skel-label, .dark .ssc__skel-value {
  background: linear-gradient(90deg, var(--ds-gray-800) 25%, var(--ds-gray-700) 50%, var(--ds-gray-800) 75%);
  background-size: 200% 100%;
}

@keyframes sscShimmer {
  0% { background-position: -200% 0; }
  100% { background-position: 200% 0; }
}

/* Responsive */
@media (max-width: 768px) {
  .ssc {
    padding: 0.875rem 1rem;
    gap: 0.75rem;
  }

  .ssc__icon-wrap {
    width: 40px;
    height: 40px;
  }

  .ssc__value {
    font-size: 1.375rem;
  }

  .ssc:hover {
    transform: translateY(-1px) scale(1.01);
  }
}

@media (max-width: 480px) {
  .ssc {
    padding: 0.75rem 0.875rem;
    gap: 0.625rem;
  }

  .ssc__icon-wrap {
    width: 36px;
    height: 36px;
  }

  .ssc__value {
    font-size: 1.25rem;
  }

  .ssc__label {
    font-size: 0.6rem;
  }
}
</style>
