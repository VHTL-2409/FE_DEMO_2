<template>
  <span :class="chipClass" class="ds-status-chip inline-flex items-center gap-1.5 font-semibold" :style="chipStyle">
    <span v-if="dot" :class="dotClass" class="rounded-full shrink-0" />
    <slot>{{ label }}</slot>
  </span>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  status: {
    type: String,
    default: 'neutral',
    validator: (v) =>
      ['active', 'success', 'ended', 'draft', 'upcoming', 'live', 'warning', 'error', 'neutral', 'info'].includes(v)
  },
  label: { type: String, default: '' },
  size: { type: String, default: 'md', validator: (v) => ['sm', 'md'].includes(v) },
  dot: { type: Boolean, default: false }
})

const chipClass = computed(() => {
  const base = 'ds-status-chip rounded-full transition-colors'
  const sizes = { sm: 'px-2 py-0.5 text-[11px]', md: 'px-2.5 py-1 text-xs' }
  return [base, sizes[props.size]]
})

const chipStyle = computed(() => {
  const map = {
    active: { bg: 'var(--ds-success-bg)', color: 'var(--ds-success)', borderColor: 'rgba(22,163,74,0.2)' },
    success: { bg: 'var(--ds-success-bg)', color: 'var(--ds-success)', borderColor: 'rgba(22,163,74,0.2)' },
    ended: { bg: 'var(--ds-gray-100)', color: 'var(--ds-gray-600)', borderColor: 'rgba(100,116,139,0.16)' },
    draft: { bg: 'var(--ds-gray-100)', color: 'var(--ds-gray-600)', borderColor: 'transparent' },
    upcoming: { bg: 'var(--ds-info-bg)', color: 'var(--ds-info)', borderColor: 'rgba(2,132,199,0.2)' },
    live: { bg: 'var(--ds-accent-bg)', color: 'var(--ds-accent)', borderColor: 'rgba(245,158,11,0.25)' },
    warning: { bg: 'var(--ds-warning-bg)', color: 'var(--ds-warning)', borderColor: 'rgba(217,119,6,0.2)' },
    error: { bg: 'var(--ds-danger-bg)', color: 'var(--ds-danger)', borderColor: 'rgba(220,38,38,0.2)' },
    neutral: { bg: 'var(--ds-gray-100)', color: 'var(--ds-gray-600)', borderColor: 'rgba(100,116,139,0.12)' },
    info: { bg: 'var(--ds-info-bg)', color: 'var(--ds-info)', borderColor: 'rgba(2,132,199,0.2)' }
  }
  const s = map[props.status] || map.neutral
  return {
    backgroundColor: s.bg,
    color: s.color,
    borderWidth: '1px',
    borderStyle: 'solid',
    borderColor: s.borderColor
  }
})

const dotClass = computed(() => {
  const map = {
    active: 'animate-pulse bg-[var(--ds-success)] w-1.5 h-1.5',
    success: 'bg-[var(--ds-success)] w-1.5 h-1.5',
    live: 'animate-pulse bg-[var(--ds-accent)] w-1.5 h-1.5',
    warning: 'bg-[var(--ds-warning)] w-1.5 h-1.5',
    error: 'bg-[var(--ds-danger)] w-1.5 h-1.5',
    upcoming: 'bg-[var(--ds-info)] w-1.5 h-1.5',
    info: 'bg-[var(--ds-info)] w-1.5 h-1.5',
    ended: '',
    draft: '',
    neutral: ''
  }
  return map[props.status] || ''
})
</script>
