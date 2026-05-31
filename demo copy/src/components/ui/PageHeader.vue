<template>
  <div class="ds-page-header flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between sm:gap-4">
    <div class="min-w-0 flex-1">
      <!-- Eyebrow -->
      <p v-if="eyebrow || $slots.eyebrow" class="ds-eyebrow mb-1.5">
        <slot name="eyebrow">{{ eyebrow }}</slot>
      </p>

      <!-- Title -->
      <h1 class="ds-title" :class="titleClass">
        <slot name="title">{{ title }}</slot>
      </h1>

      <!-- Subtitle -->
      <p v-if="subtitle || $slots.subtitle" class="ds-subtitle mt-1">
        <slot name="subtitle">{{ subtitle }}</slot>
      </p>
    </div>

    <!-- Actions slot -->
    <div v-if="$slots.actions" class="flex shrink-0 flex-wrap items-center gap-2">
      <slot name="actions" />
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  eyebrow: { type: String, default: '' },
  title: { type: String, default: '' },
  subtitle: { type: String, default: '' },
  size: { type: String, default: 'default', validator: (v) => ['sm', 'default', 'lg'].includes(v) }
})

const titleClass = computed(() => {
  const map = {
    sm: 'text-xl font-bold text-[var(--ds-text)] tracking-tight',
    default: 'text-2xl font-bold text-[var(--ds-text)] tracking-normal sm:text-3xl',
    lg: 'text-3xl font-bold text-[var(--ds-text)] tracking-normal sm:text-4xl'
  }
  return map[props.size] || map.default
})
</script>
