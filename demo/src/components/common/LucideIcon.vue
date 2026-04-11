<template>
  <component
    :is="iconComponent"
    :stroke-width="strokeWidth"
    :size="resolvedSize"
    :style="{ minWidth: resolvedSize + 'px', minHeight: resolvedSize + 'px' }"
    :class="$attrs.class"
  />
</template>

<script setup>
import { computed } from 'vue'
import { resolveIcon } from './OptimizedIcons'

const props = defineProps({
  name: { type: String, required: true },
  size: { type: [Number, String], default: null },
  strokeWidth: { type: [Number, String], default: 2 }
})

const resolvedSize = computed(() => {
  if (props.size === null) return 20
  const n = Number(props.size)
  return isNaN(n) ? 20 : n
})

/** Tên icon dùng trực tiếp khóa trong OptimizedIcons (Material/snake/kebab). Không map sang PascalCase. */
const iconComponent = computed(() => resolveIcon(props.name))
</script>

<script>
export default {
  name: 'LucideIcon'
}
</script>
