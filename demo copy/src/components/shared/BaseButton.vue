<template>
  <button
    :type="type"
    :disabled="disabled || loading"
    :aria-busy="loading ? 'true' : undefined"
    :aria-disabled="disabled || loading ? 'true' : undefined"
    :class="buttonClass"
    @click="$emit('click', $event)"
  >
    <LucideIcon v-if="loading" name="progress_activity" size="16" class="animate-spin shrink-0" />
    <slot />
  </button>
</template>

<script setup>
import { computed } from 'vue'

defineEmits(['click'])

const props = defineProps({
  variant: { type: String, default: 'primary' },
  size: { type: String, default: 'md' },
  loading: { type: Boolean, default: false },
  disabled: { type: Boolean, default: false },
  type: { type: String, default: 'button' }
})

const sizeClass = computed(() => ({
  sm: 'h-9 px-3 text-xs',
  md: 'h-10 px-4 text-sm',
  lg: 'h-11 px-5 text-sm'
}[props.size] || 'h-10 px-4 text-sm'))

const variantClass = computed(() => ({
  primary: 'border-[var(--ds-primary)] bg-[var(--ds-primary)] text-white hover:bg-[var(--ds-primary-hover)]',
  danger: 'border-[var(--ds-danger)] bg-[var(--ds-danger)] text-white hover:bg-rose-700',
  ghost: 'border-transparent bg-transparent text-[var(--ds-primary)] hover:bg-[var(--ds-primary-soft)]',
  secondary: 'border-[var(--ds-border)] bg-[var(--ds-surface)] text-[var(--ds-text)] hover:bg-[var(--ds-gray-100)]'
}[props.variant] || 'border-[var(--ds-primary)] bg-[var(--ds-primary)] text-white hover:bg-[var(--ds-primary-hover)]'))

const buttonClass = computed(() => [
  'inline-flex items-center justify-center gap-2 rounded-[var(--ds-radius-lg)] border font-semibold',
  'transition-[background-color,border-color,box-shadow,color,transform] duration-150',
  'disabled:cursor-not-allowed disabled:opacity-55',
  'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[var(--ds-primary-ring)]',
  sizeClass.value,
  variantClass.value
])
</script>
