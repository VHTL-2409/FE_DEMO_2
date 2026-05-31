<template>
  <div
    :class="[
      'ds-card ds-surface border transition-[border-color,box-shadow,transform] duration-150',
      hoverable ? 'cursor-pointer' : '',
      paddingClass,
      borderClass,
    ]"
    :style="cardStyle"
    @mouseenter="hovered = true"
    @mouseleave="hovered = false"
  >
    <div v-if="$slots.header" class="ds-card-header mb-4">
      <slot name="header" />
    </div>
    <slot />
    <div v-if="$slots.footer" class="ds-card-footer mt-4 pt-4" :class="footerBorderClass">
      <slot name="footer" />
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'

const props = defineProps({
  padding: { type: String, default: 'md', validator: (v) => ['sm', 'md', 'lg', 'none'].includes(v) },
  variant: { type: String, default: 'default', validator: (v) => ['default', 'bordered', 'shadow', 'accent', 'alert'].includes(v) },
  hoverable: { type: Boolean, default: false },
  accentColor: { type: String, default: 'primary' }
})

const hovered = ref(false)

const paddingClass = computed(() => {
  const map = { none: '', sm: 'p-4', md: 'p-5', lg: 'p-6' }
  return map[props.padding] || map.md
})

const borderClass = computed(() => {
  if (props.variant === 'bordered') return `border border-[var(--ds-border-strong)] rounded-[var(--ds-radius-xl)]`
  if (props.variant === 'shadow') return `border border-[var(--ds-border)] shadow-[var(--ds-shadow-md)] rounded-[var(--ds-radius-xl)]`
  if (props.variant === 'accent') return `border-l-2 rounded-[var(--ds-radius-xl)] border border-[var(--ds-border)]`
  if (props.variant === 'alert') return `border-l-2 border-[var(--ds-danger)] rounded-[var(--ds-radius-xl)] border border-[var(--ds-border)]`
  if (props.hoverable && hovered.value) {
    return `border border-[var(--ds-border)] rounded-[var(--ds-radius-xl)] -translate-y-0.5 shadow-[var(--ds-shadow-lg)]`
  }
  return `border border-[var(--ds-border)] rounded-[var(--ds-radius-xl)]`
})

const footerBorderClass = computed(() =>
  props.variant === 'accent' ? 'border-t border-[var(--ds-border)]' : ''
)

const accentColorMap = {
  primary: 'var(--ds-primary)',
  success: 'var(--ds-success)',
  warning: 'var(--ds-warning)',
  danger: 'var(--ds-danger)',
  info: 'var(--ds-info)'
}

const cardStyle = computed(() => {
  if (props.variant === 'accent') {
    const color = accentColorMap[props.accentColor] || accentColorMap.primary
    return { backgroundColor: 'var(--ds-surface)', borderLeftColor: color }
  }
  return { backgroundColor: 'var(--ds-surface)' }
})
</script>

<style scoped>
.ds-card {
  box-shadow: var(--ds-shadow-sm);
}
.ds-card:hover {
  box-shadow: var(--ds-shadow-md);
}
</style>
