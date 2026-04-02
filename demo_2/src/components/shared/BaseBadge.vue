<template>
  <span :class="rootClass">
    <span v-if="dot" class="badge-dot" />
    <slot>{{ label }}</slot>
  </span>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  label: { type: String, default: '' },
  variant: {
    type: String,
    default: 'neutral',
    validator: (v) => ['amber', 'success', 'danger', 'warning', 'info', 'neutral', 'violet'].includes(v)
  },
  size: {
    type: String,
    default: 'md',
    validator: (v) => ['sm', 'md', 'lg'].includes(v)
  },
  dot: { type: Boolean, default: false },
  outline: { type: Boolean, default: false }
})

const rootClass = computed(() => {
  const variant = `gs-badge--${props.variant}`
  const size = props.size !== 'md' ? `gs-badge--${props.size}` : ''
  const outline = props.outline ? 'gs-badge--outline' : ''
  return ['gs-badge', variant, size, outline].filter(Boolean).join(' ')
})
</script>

<style scoped>
.gs-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.3rem;
  border-radius: var(--radius-glass-pill, 999px);
  font-size: 0.72rem;
  font-weight: 700;
  letter-spacing: 0.05em;
  text-transform: uppercase;
  white-space: nowrap;
  line-height: 1;
}

.gs-badge--sm {
  padding: 0.2rem 0.5rem;
  font-size: 0.625rem;
}

.gs-badge--lg {
  padding: 0.3rem 0.875rem;
  font-size: 0.8rem;
}

.gs-badge--amber {
  background: var(--glass-amber-soft, rgba(139,75,0,0.08));
  color: var(--glass-amber, #8d4b00);
  border: 1px solid var(--glass-amber-border, rgba(139,75,0,0.35));
}

.gs-badge--success {
  background: var(--glass-success-soft, rgba(22,163,74,0.1));
  color: var(--glass-success, #16a34a);
  border: 1px solid var(--glass-success-border, rgba(22,163,74,0.25));
}

.gs-badge--danger {
  background: var(--glass-danger-soft, rgba(220,38,38,0.1));
  color: var(--glass-danger, #dc2626);
  border: 1px solid var(--glass-danger-border, rgba(220,38,38,0.25));
}

.gs-badge--warning {
  background: var(--glass-warning-soft, rgba(217,119,6,0.1));
  color: var(--glass-warning, #d97706);
  border: 1px solid var(--glass-warning-border, rgba(217,119,6,0.25));
}

.gs-badge--info {
  background: var(--glass-info-soft, rgba(2,132,199,0.1));
  color: var(--glass-info, #0284c7);
  border: 1px solid var(--glass-info-border, rgba(2,132,199,0.25));
}

.gs-badge--neutral {
  background: var(--glass-surface-muted, rgba(250,247,242,0.65));
  color: var(--glass-text-secondary, #57534e);
  border: 1px solid var(--glass-border, rgba(0,0,0,0.07));
}

.gs-badge--violet {
  background: var(--glass-violet-soft, rgba(99,102,241,0.08));
  color: #6366f1;
  border: 1px solid var(--glass-violet-border, rgba(99,102,241,0.2));
}

.gs-badge--outline {
  background: transparent !important;
}

/* Dot indicator */
.badge-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: currentColor;
  flex-shrink: 0;
}
</style>
