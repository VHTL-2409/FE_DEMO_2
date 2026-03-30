<template>
  <div :class="rootClass" :aria-hidden="ariaHidden" role="presentation">
    <template v-if="variant === 'text'">
      <div
        v-for="i in lineCount"
        :key="i"
        class="portal-skeleton rounded-md shimmer"
        :style="{ height: '0.875rem', width: i === lineCount ? '70%' : '100%', animationDelay: `${i * 0.1}s` }"
      />
    </template>

    <div v-else-if="variant === 'card'" class="space-y-3 w-full">
      <div class="h-4 portal-skeleton rounded-md w-2/3 shimmer" style="animation-delay: 0s" />
      <div class="h-20 portal-skeleton rounded-xl w-full shimmer" style="animation-delay: 0.1s" />
      <div class="h-3 portal-skeleton rounded-md w-full shimmer" style="animation-delay: 0.2s" />
    </div>

    <div v-else-if="variant === 'table-row'" class="flex gap-3 w-full items-center">
      <div class="h-10 w-10 portal-skeleton rounded-lg shrink-0 shimmer" style="animation-delay: 0s" />
      <div class="flex-1 space-y-2">
        <div class="h-3 portal-skeleton rounded w-3/4 shimmer" style="animation-delay: 0.05s" />
        <div class="h-3 portal-skeleton rounded w-1/2 shimmer" style="animation-delay: 0.1s" />
      </div>
    </div>

    <div v-else-if="variant === 'avatar'" class="portal-skeleton rounded-full shimmer" :style="{ width: size, height: size, animationDelay: '0s' }" />
    
    <!-- New: profile variant -->
    <div v-else-if="variant === 'profile'" class="flex flex-col items-center gap-6 w-full">
      <div class="w-24 h-24 portal-skeleton rounded-full shimmer" />
      <div class="space-y-3 w-full max-w-xs">
        <div class="h-6 portal-skeleton rounded-md w-3/4 mx-auto shimmer" style="animation-delay: 0.1s" />
        <div class="h-4 portal-skeleton rounded-md w-1/2 mx-auto shimmer" style="animation-delay: 0.15s" />
        <div class="space-y-2 pt-2">
          <div v-for="i in 4" :key="i" class="h-4 portal-skeleton rounded w-full shimmer" :style="{ animationDelay: `${0.2 + i * 0.05}s` }" />
        </div>
      </div>
    </div>
    
    <!-- New: stats-card variant -->
    <div v-else-if="variant === 'stats-card'" class="rounded-xl border border-[var(--ds-border)] p-5">
      <div class="flex items-center gap-3">
        <div class="w-10 h-10 portal-skeleton rounded-lg shimmer" />
        <div class="space-y-2">
          <div class="h-3 w-16 portal-skeleton rounded shimmer" style="animation-delay: 0.05s" />
          <div class="h-5 w-12 portal-skeleton rounded shimmer" style="animation-delay: 0.1s" />
        </div>
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
    validator: (v) => ['text', 'card', 'table-row', 'avatar', 'profile', 'stats-card'].includes(v)
  },
  lines: { type: Number, default: 3 },
  size: { type: String, default: '2.5rem' }
})

const lineCount = computed(() => Math.max(1, Math.min(props.lines, 12)))
const ariaHidden = computed(() => true)

const rootClass = computed(() => {
  if (props.variant === 'text') return 'space-y-2 w-full'
  if (props.variant === 'avatar') return 'inline-block'
  return 'w-full'
})
</script>

<style scoped>
.portal-skeleton {
  background: linear-gradient(90deg, var(--ds-gray-100, #f1f5f9) 25%, var(--ds-gray-200, #e2e8f0) 50%, var(--ds-gray-100, #f1f5f9) 75%);
  background-size: 200% 100%;
}

.dark .portal-skeleton {
  background: linear-gradient(90deg, var(--ds-gray-700, #334155) 25%, var(--ds-gray-600, #475569) 50%, var(--ds-gray-700, #334155) 75%);
  background-size: 200% 100%;
}

.shimmer {
  animation: shimmer 1.5s cubic-bezier(0.4, 0, 0.2, 1) infinite;
}

@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}
</style>
