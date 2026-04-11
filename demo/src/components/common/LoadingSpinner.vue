<template>
  <div
    :class="[sizeClass, 'inline-flex items-center justify-center']"
    role="status"
    aria-label="Đang tải"
  >
    <span
      class="animate-spin rounded-full border-2 border-primary/30 border-t-primary"
      :class="sizeClass"
    />
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  size: {
    type: String,
    default: 'md',
    validator: (v) => ['sm', 'md', 'lg'].includes(v)
  }
})

const sizeClass = computed(() => {
  const map = { sm: 'size-6', md: 'size-10', lg: 'size-14' }
  return map[props.size] || map.md
})
</script>
