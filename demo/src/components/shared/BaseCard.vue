<template>
  <div :class="rootClass">
    <slot />
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  padding: {
    type: String,
    default: 'md',
    validator: (v) => ['none', 'sm', 'md', 'lg'].includes(v)
  },
  hoverable: { type: Boolean, default: false },
  interactive: { type: Boolean, default: false }
})

const paddingClass = {
  none: '',
  sm: 'p-4',
  md: 'p-6',
  lg: 'p-8'
}

const rootClass = computed(() => {
  const base = [
    'rounded-[var(--ds-radius-xl)] border bg-[var(--ds-surface)] dark:bg-slate-900/88',
    'border-[var(--ds-border)] shadow-[var(--ds-shadow-sm)] dark:border-slate-700',
    paddingClass[props.padding] || paddingClass.md
  ]
  if (props.hoverable || props.interactive) {
    base.push('portal-card-lift transition-[box-shadow,transform,border-color] duration-150')
  }
  if (props.interactive) {
    base.push('cursor-pointer hover:border-[var(--ds-primary-border)]')
  }
  return base
})
</script>
