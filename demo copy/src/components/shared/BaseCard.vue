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
    validator: (v) => ['sm', 'md', 'lg'].includes(v)
  },
  hoverable: { type: Boolean, default: false }
})

const paddingClass = {
  sm: 'p-4',
  md: 'p-6',
  lg: 'p-8'
}

const rootClass = computed(() => {
  const base = [
    'rounded-[var(--ds-radius-xl)] border bg-[var(--ds-surface)] dark:bg-slate-900/88',
    'border-[var(--ds-border)] shadow-[var(--ds-shadow-sm)]',
    paddingClass[props.padding] || paddingClass.md
  ]
  if (props.hoverable) {
    base.push('portal-card-lift dark:border-slate-700')
  } else {
    base.push('dark:border-slate-700')
  }
  return base
})
</script>
