<template>
  <button
    :type="type"
    :disabled="disabled || loading"
    :aria-busy="loading ? 'true' : undefined"
    :aria-disabled="disabled || loading ? 'true' : undefined"
    :class="buttonClass"
    :title="title || undefined"
    @click="$emit('click', $event)"
  >
    <LucideIcon v-if="loading" name="progress_activity" size="16" class="animate-spin shrink-0" />
    <LucideIcon v-else-if="icon" :name="icon" size="16" class="shrink-0" />
    <slot />
  </button>
</template>

<script setup>
import { computed } from 'vue'

defineEmits(['click'])

const props = defineProps({
  variant: { type: String, default: 'primary' },
  size: { type: String, default: 'md' },
  icon: { type: String, default: '' },
  title: { type: String, default: '' },
  loading: { type: Boolean, default: false },
  disabled: { type: Boolean, default: false },
  type: { type: String, default: 'button' }
})

const sizeClass = computed(() => ({
  xs: 'h-8 px-2.5 text-xs',
  sm: 'h-9 px-3 text-xs',
  md: 'h-10 px-4 text-sm',
  lg: 'h-11 px-5 text-sm',
  xl: 'h-12 px-6 text-base'
}[props.size] || 'h-10 px-4 text-sm'))

const variantClass = computed(() => ({
  primary: 'border-[var(--ds-primary)] bg-[var(--ds-primary)] text-white shadow-[var(--ds-shadow-xs)] hover:bg-[var(--ds-primary-hover)]',
  danger: 'border-[var(--ds-danger)] bg-[var(--ds-danger)] text-white shadow-[var(--ds-shadow-xs)] hover:bg-rose-700',
  ghost: 'border-transparent bg-transparent text-[var(--ds-primary)] hover:bg-[var(--ds-primary-soft)]',
  secondary: 'border-[var(--ds-border)] bg-[var(--ds-surface)] text-[var(--ds-text)] shadow-[var(--ds-shadow-xs)] hover:bg-[var(--ds-gray-100)]',
  subtle: 'border-transparent bg-[var(--ds-gray-100)] text-[var(--ds-text-secondary)] hover:text-[var(--ds-text)] hover:bg-[var(--ds-gray-200)]'
}[props.variant] || 'border-[var(--ds-primary)] bg-[var(--ds-primary)] text-white hover:bg-[var(--ds-primary-hover)]'))

const buttonClass = computed(() => [
  'inline-flex min-w-0 items-center justify-center gap-2 whitespace-nowrap rounded-[var(--ds-radius-lg)] border font-semibold leading-none',
  'transition-[background-color,border-color,box-shadow,color,transform] duration-150',
  'active:translate-y-px',
  'disabled:cursor-not-allowed disabled:opacity-55 disabled:active:translate-y-0',
  'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[var(--ds-primary-ring)] focus-visible:ring-offset-1',
  sizeClass.value,
  variantClass.value
])
</script>
