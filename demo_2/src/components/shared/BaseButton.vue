<template>
  <button
    :type="type"
    :disabled="disabled || loading"
    :class="buttonClass"
    class="gs-btn portal-focus"
    @click="$emit('click', $event)"
  >
    <span
      v-if="loading"
      class="gs-btn__spinner"
      aria-hidden="true"
    />
    <slot />
  </button>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  variant: {
    type: String,
    default: 'primary',
    validator: (v) => ['primary', 'secondary', 'danger', 'ghost', 'glass', 'outline'].includes(v)
  },
  size: {
    type: String,
    default: 'md',
    validator: (v) => ['sm', 'md', 'lg'].includes(v)
  },
  loading: { type: Boolean, default: false },
  disabled: { type: Boolean, default: false },
  type: { type: String, default: 'button' }
})

defineEmits(['click'])

const sizeClass = computed(() => {
  return `gs-btn--${props.size}`
})

const variantClass = computed(() => {
  // Map old variant names to glass system
  const variantMap = {
    primary: 'gs-btn--primary gs-btn--shimmer',
    secondary: 'gs-btn--glass',
    danger: 'gs-btn--danger',
    ghost: 'gs-btn--ghost',
    glass: 'gs-btn--glass',
    outline: 'gs-btn--outline'
  }
  return variantMap[props.variant] || 'gs-btn--primary'
})

const buttonClass = computed(() => [
  sizeClass.value,
  variantClass.value,
  { 'gs-btn--loading': props.loading }
])
</script>

<style scoped>
/* Base button uses .gs-btn from glass-system.css */
/* Variant and size classes are applied via the computed class binding */
/* Additional scoped refinements if needed */

/* Ensure portal-focus works */
.portal-focus:focus-visible {
  outline: none;
  box-shadow: 0 0 0 3px var(--glass-amber-soft);
}

/* Loading state adjustments */
.gs-btn--loading {
  position: relative;
}

/* Dark mode adjustments for glass variant */
.dark .gs-btn--glass {
  background: var(--glass-surface);
  border-color: var(--glass-border);
  color: var(--glass-text);
}

.dark .gs-btn--glass:hover {
  background: var(--glass-surface-hover);
  color: var(--glass-text);
}
</style>
