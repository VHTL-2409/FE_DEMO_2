<template>
  <div class="space-y-3 pb-2">
    <label
      v-for="option in normalizedOptions"
      :key="option.id"
      :class="selectedIds.includes(option.id) ? activeClass : idleClass"
      class="group relative flex cursor-pointer items-center rounded-xl border-2 p-4 transition-all duration-200 sm:p-5"
    >
      <input
        :checked="selectedIds.includes(option.id)"
        :disabled="disabled"
        class="h-5 w-5 shrink-0 rounded border-slate-300 bg-white text-primary focus:ring-2 focus:ring-primary/50 dark:border-slate-600 dark:bg-slate-900"
        type="checkbox"
        @change="toggleValue(option.id)"
      />
      <span class="ml-4 text-base font-medium text-slate-800 dark:text-slate-200 sm:text-lg">{{ option.text }}</span>
    </label>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  question: {
    type: Object,
    default: () => ({})
  },
  modelValue: {
    type: Array,
    default: () => []
  },
  disabled: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:modelValue'])

const activeClass = 'border-primary bg-primary/5 dark:bg-primary/10 ring-2 ring-primary/30'
const idleClass = 'border-slate-200 dark:border-slate-700 hover:border-primary/40 bg-slate-50/50 dark:bg-slate-800/30'

const normalizedOptions = computed(() => Array.isArray(props.question?.options) ? props.question.options : [])
const selectedIds = computed(() => Array.isArray(props.modelValue) ? props.modelValue : [])

const toggleValue = (optionId) => {
  const next = selectedIds.value.includes(optionId)
    ? selectedIds.value.filter((id) => id !== optionId)
    : [...selectedIds.value, optionId]
  emit('update:modelValue', next)
}
</script>
