<template>
  <div class="ds-skeleton overflow-hidden" :class="[variantClass, roundedClass]" :style="skeletonStyle" />
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  variant: { type: String, default: 'text', validator: (v) => ['text', 'card', 'row', 'avatar', 'circle', 'chart'].includes(v) },
  width: { type: String, default: '' },
  height: { type: String, default: '' },
  rounded: { type: String, default: 'md' }
})

const variantClass = computed(() => {
  const map = {
    text: 'h-4',
    card: 'h-32',
    row: 'h-12',
    avatar: 'size-10 rounded-full',
    circle: 'size-10 rounded-full',
    chart: 'h-64'
  }
  return map[props.variant] || map.text
})

const roundedClass = computed(() => {
  const map = { none: 'rounded-none', sm: 'rounded-[var(--ds-radius-sm)]', md: 'rounded-[var(--ds-radius-md)]', lg: 'rounded-[var(--ds-radius-lg)]', xl: 'rounded-[var(--ds-radius-xl)]', full: 'rounded-full' }
  return map[props.rounded] || map.md
})

const skeletonStyle = computed(() => ({
  width: props.width || undefined,
  height: props.height || undefined
}))
</script>

<style scoped>
/* ds-shimmer animation uses .shimmer from animation.css (never tree-shaken) */
.ds-skeleton {
  background: linear-gradient(
    90deg,
    var(--ds-gray-100) 0%,
    var(--ds-gray-200) 50%,
    var(--ds-gray-100) 100%
  );
  background-size: 200% 100%;
  animation: shimmer 1.3s ease-in-out infinite;
}

@media (prefers-reduced-motion: reduce) {
  .ds-skeleton {
    animation: none;
    background: var(--ds-gray-200);
  }
}
</style>
