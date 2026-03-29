<template>
  <button
    :type="type"
    :disabled="disabled || loading"
    :class="buttonClass"
    class="base-button portal-focus"
    @click="$emit('click', $event)"
  >
    <span
      v-if="loading"
      class="material-symbols-outlined text-lg animate-spin shrink-0"
      aria-hidden="true"
    >
      progress_activity
    </span>
    <slot />
  </button>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  variant: {
    type: String,
    default: 'primary',
    validator: (v) => ['primary', 'secondary', 'danger', 'ghost'].includes(v)
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
  return `base-button--${props.size}`
})

const variantClass = computed(() => {
  return `base-button--${props.variant}`
})

const buttonClass = computed(() => [
  sizeClass.value,
  variantClass.value,
  { 'base-button--loading': props.loading }
])
</script>

<style scoped>
.base-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.55rem;
  border-radius: 1rem;
  font-weight: 700;
  letter-spacing: -0.01em;
  transition:
    transform 160ms ease,
    background-color 180ms ease,
    box-shadow 180ms ease,
    opacity 160ms ease,
    border-color 180ms ease,
    color 180ms ease;
}

.base-button:hover {
  transform: translateY(-1px);
}

.base-button:active {
  transform: translateY(0);
}

.base-button:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

.base-button:disabled:hover {
  transform: none;
}

.base-button--sm {
  min-height: 2.4rem;
  padding: 0.65rem 1rem;
  font-size: 0.86rem;
}

.base-button--md {
  min-height: 3rem;
  padding: 0.82rem 1.3rem;
  font-size: 0.92rem;
}

.base-button--lg {
  min-height: 3.35rem;
  padding: 0.96rem 1.5rem;
  font-size: 1rem;
}

.base-button--primary {
  border: 1px solid rgba(89, 98, 243, 0.22);
  background: linear-gradient(135deg, var(--color-primary) 0%, var(--color-primary-hover) 100%);
  color: white;
  box-shadow: 0 20px 38px -24px rgba(89, 98, 243, 0.46);
}

.base-button--primary:hover {
  box-shadow: 0 24px 44px -24px rgba(89, 98, 243, 0.54);
}

.base-button--secondary {
  border: 1px solid rgba(148, 163, 184, 0.24);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.95) 0%, rgba(247, 250, 255, 0.88) 100%);
  color: var(--color-text);
  box-shadow: 0 14px 28px -24px rgba(15, 23, 42, 0.18);
}

.base-button--secondary:hover {
  border-color: rgba(89, 98, 243, 0.2);
  background: rgba(255, 255, 255, 0.98);
}

.base-button--danger {
  border: 1px solid rgba(239, 68, 68, 0.24);
  background: linear-gradient(135deg, #fb7185 0%, #ef4444 38%, #dc2626 100%);
  color: white;
  box-shadow: 0 20px 38px -24px rgba(220, 38, 38, 0.42);
}

.base-button--ghost {
  border: 1px solid transparent;
  background: transparent;
  color: var(--color-primary);
}

.base-button--ghost:hover {
  background: rgba(89, 98, 243, 0.08);
}

.dark .base-button--secondary {
  border-color: rgba(148, 163, 184, 0.16);
  background: linear-gradient(180deg, rgba(16, 24, 38, 0.96) 0%, rgba(30, 41, 59, 0.92) 100%);
  color: var(--color-text);
}

.dark .base-button--secondary:hover {
  border-color: rgba(129, 140, 248, 0.22);
  background: rgba(30, 41, 59, 0.94);
}

.base-button--loading {
  pointer-events: none;
}
</style>
