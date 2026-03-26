<template>
  <div
    :class="rootClass"
    role="status"
  >
    <span
      v-if="icon"
      :class="iconClass"
      aria-hidden="true"
    >
      {{ icon }}
    </span>
    <h3 :class="titleClass">
      {{ title }}
    </h3>
    <p v-if="description" :class="descriptionClass">
      {{ description }}
    </p>
    <BaseButton
      v-if="actionLabel"
      variant="primary"
      @click="$emit('action')"
    >
      {{ actionLabel }}
    </BaseButton>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import BaseButton from './BaseButton.vue'

const props = defineProps({
  icon: { type: String, default: '' },
  title: { type: String, required: true },
  description: { type: String, default: '' },
  actionLabel: { type: String, default: '' },
  /** Gọn hơn: trong ô bảng hoặc panel nhỏ */
  dense: { type: Boolean, default: false },
  /** Chiếm chiều cao vùng cha (scroll / card) để không còn “dải trống” mỏng */
  fill: { type: Boolean, default: false }
})

defineEmits(['action'])

const rootClass = computed(() => {
  const base = 'flex w-full flex-col items-center justify-center text-center'
  const pad = props.dense ? 'py-6 px-3 sm:px-4' : 'py-10 px-4'
  const fill = props.fill ? 'min-h-[min(42dvh,320px)] flex-1' : ''
  return [base, pad, fill].filter(Boolean).join(' ')
})

const iconClass = computed(() => {
  const color = 'text-[color:var(--color-text-secondary)] select-none'
  if (props.dense) {
    return `material-symbols-outlined text-4xl ${color} mb-2`
  }
  return `material-symbols-outlined text-5xl ${color} mb-4`
})

const titleClass = computed(() => {
  if (props.dense) {
    return 'text-base font-bold text-slate-900 dark:text-slate-100 mb-1'
  }
  return 'text-lg font-bold text-slate-900 dark:text-slate-100 mb-2'
})

const descriptionClass = computed(() => {
  const mb = props.actionLabel ? 'mb-4 sm:mb-6' : 'mb-0'
  if (props.dense) {
    return `text-xs sm:text-sm text-slate-600 dark:text-slate-400 max-w-md ${mb}`
  }
  return `text-sm text-slate-600 dark:text-slate-400 max-w-md ${mb}`
})
</script>
