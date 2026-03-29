<template>
  <div :class="rootClass" class="base-card">
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
    'rounded-[1.6rem] border',
    paddingClass[props.padding] || paddingClass.md
  ]
  if (props.hoverable) {
    base.push('portal-card-lift')
  } else {
    base.push('')
  }
  return base
})
</script>

<style scoped>
.base-card {
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98) 0%, rgba(247, 250, 255, 0.94) 100%);
  border-color: rgba(255, 255, 255, 0.78);
  box-shadow: var(--shadow-md);
}

.dark .base-card {
  background: linear-gradient(180deg, rgba(16, 24, 38, 0.96) 0%, rgba(15, 23, 42, 0.92) 100%);
  border-color: rgba(148, 163, 184, 0.14);
}
</style>
