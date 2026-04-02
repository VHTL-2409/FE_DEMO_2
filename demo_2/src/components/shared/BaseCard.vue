<template>
  <div :class="rootClass" class="gs-card">
    <slot />
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  padding: {
    type: String,
    default: 'md',
    validator: (v) => ['sm', 'md', 'lg', 'none'].includes(v)
  },
  variant: {
    type: String,
    default: 'default',
    validator: (v) => ['default', 'flat', 'glass', 'elevated'].includes(v)
  },
  hoverable: { type: Boolean, default: false }
})

const paddingClass = {
  none: '',
  sm: 'p-4',
  md: 'p-6',
  lg: 'p-8'
}

const variantClass = computed(() => {
  const variantMap = {
    default: '',
    flat: 'gs-card--flat',
    glass: 'gs-card--glass',
    elevated: 'glass--elevated'
  }
  return variantMap[props.variant] || ''
})

const rootClass = computed(() => {
  const base = [
    paddingClass[props.padding] || paddingClass.md,
    variantClass.value
  ]
  return base.filter(Boolean)
})
</script>

<style scoped>
/* Hoverable uses .gs-card hover from glass-system.css via .gs-lift utility */
/* Additional scoped refinements */
.gs-card {
  will-change: transform, box-shadow;
}

/* Ensure hover effect is smooth */
.gs-card:hover {
  cursor: pointer;
}

/* If not hoverable, remove cursor pointer */
.gs-card:not(.gs-lift):hover {
  cursor: default;
}
</style>
