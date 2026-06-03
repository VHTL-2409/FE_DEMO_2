<template>
  <div
    v-if="show"
    :class="['action-bar sticky bottom-0 z-20 border-t border-[var(--ds-border)] bg-[var(--ds-surface)]/95 px-6 py-4', shadow ? 'shadow-[var(--ds-shadow-md)]' : '']"
  >
    <div class="mx-auto flex flex-wrap items-center justify-end gap-3" :class="containerClass">
      <slot />
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  show: { type: Boolean, default: true },
  shadow: { type: Boolean, default: true },
  maxWidth: { type: String, default: '7xl' }
})

const containerClass = computed(() => ({
  none: '',
  '4xl': 'max-w-4xl',
  '5xl': 'max-w-5xl',
  '6xl': 'max-w-6xl',
  '7xl': 'max-w-7xl'
}[props.maxWidth] || 'max-w-7xl'))
</script>
