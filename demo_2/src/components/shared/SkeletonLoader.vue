<template>
  <div :class="rootClass" :aria-hidden="ariaHidden" role="presentation">
    <template v-if="variant === 'text'">
      <div
        v-for="i in lineCount"
        :key="i"
        class="portal-skeleton rounded-md"
        :style="{ height: '0.875rem', width: i === lineCount ? '70%' : '100%' }"
      />
    </template>

    <div v-else-if="variant === 'card'" class="space-y-3 w-full">
      <div class="h-4 portal-skeleton rounded-md w-2/3" />
      <div class="h-20 portal-skeleton rounded-xl w-full" />
      <div class="h-3 portal-skeleton rounded-md w-full" />
    </div>

    <div v-else-if="variant === 'table-row'" class="flex gap-3 w-full items-center">
      <div class="h-10 w-10 portal-skeleton rounded-lg shrink-0" />
      <div class="flex-1 space-y-2">
        <div class="h-3 portal-skeleton rounded w-3/4" />
        <div class="h-3 portal-skeleton rounded w-1/2" />
      </div>
    </div>

    <div v-else-if="variant === 'avatar'" class="portal-skeleton rounded-full" :style="{ width: size, height: size }" />
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  variant: {
    type: String,
    default: 'text',
    validator: (v) => ['text', 'card', 'table-row', 'avatar'].includes(v)
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
