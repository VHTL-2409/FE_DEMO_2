<template>
  <div :class="rootClass" :aria-hidden="ariaHidden" role="presentation">
    <template v-if="variant === 'text'">
      <div
        v-for="i in lineCount"
        :key="i"
        class="gs-shimmer rounded-md"
        :style="{ height: '0.875rem', width: i === lineCount ? '70%' : '100%' }"
      />
    </template>

    <div v-else-if="variant === 'card'" class="space-y-3 w-full">
      <div class="h-4 gs-shimmer rounded-md w-2/3" />
      <div class="h-20 gs-shimmer rounded-xl w-full" />
      <div class="h-3 gs-shimmer rounded-md w-full" />
    </div>

    <div v-else-if="variant === 'table-row'" class="flex gap-3 w-full items-center">
      <div class="h-10 w-10 gs-shimmer rounded-lg shrink-0" />
      <div class="flex-1 space-y-2">
        <div class="h-3 gs-shimmer rounded w-3/4" />
        <div class="h-3 gs-shimmer rounded w-1/2" />
      </div>
    </div>

    <div
      v-else-if="variant === 'avatar'"
      class="gs-shimmer rounded-full"
      :style="{ width: size, height: size }"
    />

    <div v-else-if="variant === 'chart'" class="flex items-end gap-1 w-full h-24">
      <div
        v-for="i in 6"
        :key="i"
        class="flex-1 gs-shimmer rounded-t-sm"
        :style="{ height: `${Math.random() * 60 + 40}%` }"
      />
    </div>

    <div v-else-if="variant === 'table'" class="w-full">
      <!-- Table header -->
      <div class="flex gap-4 items-center pb-3 mb-3 border-b border-slate-200 dark:border-slate-700">
        <div v-for="i in 5" :key="'h' + i" class="gs-shimmer rounded h-3" :class="i === 1 ? 'w-12' : i === 2 ? 'w-40' : i === 3 ? 'w-20' : i === 4 ? 'w-16' : 'w-24'" />
      </div>
      <!-- Table rows -->
      <div
        v-for="row in lineCount"
        :key="'r' + row"
        class="flex gap-4 items-center py-3 border-b border-slate-100 dark:border-slate-800"
      >
        <div v-for="i in 5" :key="'c' + row + i" class="gs-shimmer rounded h-3" :class="i === 1 ? 'w-12' : i === 2 ? 'w-40' : i === 3 ? 'w-20' : i === 4 ? 'w-16' : 'w-24'" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  variant: {
    type: String,
    default: 'text',
    validator: (v) => ['text', 'card', 'table-row', 'avatar', 'chart', 'table'].includes(v)
  },
  lines: { type: Number, default: 3 },
  size: { type: String, default: '2.5rem' }
})

const lineCount = computed(() => Math.max(1, Math.min(props.lines, 12)))
const ariaHidden = computed(() => true)

const rootClass = computed(() => {
  if (props.variant === 'text') return 'space-y-2 w-full'
  if (props.variant === 'avatar') return 'inline-block'
  if (props.variant === 'chart') return ''
  if (props.variant === 'table') return 'w-full'
  return 'w-full'
})
</script>

<style scoped>
/* Skeleton variants use .gs-shimmer from glass-system.css */
/* .gs-shimmer includes the shimmer animation via background gradient */

/* Dark mode shimmer override */
.dark .gs-shimmer {
  background: linear-gradient(
    90deg,
    var(--glass-bg-mid) 25%,
    rgba(255, 255, 255, 0.08) 50%,
    var(--glass-bg-mid) 75%
  );
  background-size: 200% 100%;
}
</style>
