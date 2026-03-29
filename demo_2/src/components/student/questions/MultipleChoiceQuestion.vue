<template>
  <div class="student-choice-stack space-y-2.5 pb-1 sm:space-y-3">
    <label
      v-for="option in normalizedOptions"
      :key="option.id"
      :class="selectedIds.includes(option.id) ? 'student-choice-card student-choice-card--active' : 'student-choice-card student-choice-card--idle'"
      class="group relative block"
    >
      <input
        :checked="selectedIds.includes(option.id)"
        :disabled="disabled"
        class="student-choice-card__native"
        type="checkbox"
        @change="toggleValue(option.id)"
      />
      <span class="flex items-start gap-3 sm:gap-3.5">
        <span class="student-choice-card__check" aria-hidden="true">
          <span v-if="selectedIds.includes(option.id)" class="material-symbols-outlined leading-none">check</span>
        </span>
        <span class="min-w-0 flex-1 pt-[0.1rem] text-[0.95rem] font-medium leading-snug text-slate-800 dark:text-slate-200 sm:text-base sm:leading-relaxed">
          {{ option.text }}
        </span>
      </span>
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

const normalizedOptions = computed(() => (Array.isArray(props.question?.options) ? props.question.options : []))
const selectedIds = computed(() => (Array.isArray(props.modelValue) ? props.modelValue : []))

const toggleValue = (optionId) => {
  const next = selectedIds.value.includes(optionId)
    ? selectedIds.value.filter((id) => id !== optionId)
    : [...selectedIds.value, optionId]
  emit('update:modelValue', next)
}
</script>
