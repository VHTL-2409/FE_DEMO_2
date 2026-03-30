<template>
  <button
    :type="type"
    :disabled="disabled || loading"
    class="inline-flex items-center justify-center gap-2 rounded-xl font-semibold px-5 py-2.5 text-sm border border-slate-200 bg-white text-slate-700 hover:bg-slate-50 disabled:opacity-50 disabled:cursor-not-allowed"
    :class="{ 'border border-indigo-600 bg-indigo-600 text-white hover:bg-indigo-700': variant === 'primary', 'border border-rose-500 bg-rose-500 text-white hover:bg-rose-600': variant === 'danger', 'border-transparent bg-transparent text-primary hover:bg-slate-100': variant === 'ghost' }"
    @click="$emit('click', $event)"
  >
    <LucideIcon v-if="loading" name="progress_activity" size="16" class="animate-spin shrink-0" />
    <slot />
  </button>
</template>

<script setup>
defineProps({
  variant: { type: String, default: 'primary' },
  size: { type: String, default: 'md' },
  loading: { type: Boolean, default: false },
  disabled: { type: Boolean, default: false },
  type: { type: String, default: 'button' }
})
defineEmits(['click'])
</script>
