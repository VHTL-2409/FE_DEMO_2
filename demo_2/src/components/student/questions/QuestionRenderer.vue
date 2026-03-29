<template>
  <div class="student-question-renderer w-full min-h-0">
    <component
      :is="componentToRender"
      :question="question"
      :model-value="modelValue"
      :disabled="disabled"
      @update:model-value="$emit('update:modelValue', $event)"
    />
  </div>
</template>

<script setup>
import { computed } from 'vue'
import EssayQuestion from './EssayQuestion.vue'
import MultipleChoiceQuestion from './MultipleChoiceQuestion.vue'
import SingleChoiceQuestion from './SingleChoiceQuestion.vue'

const props = defineProps({
  question: {
    type: Object,
    default: () => ({})
  },
  modelValue: {
    type: [String, Array],
    default: ''
  },
  disabled: {
    type: Boolean,
    default: false
  }
})

defineEmits(['update:modelValue'])

const componentToRender = computed(() => {
  const type = String(props.question?.type || 'SINGLE_CHOICE').toUpperCase()
  if (type === 'MULTIPLE_CHOICE') return MultipleChoiceQuestion
  if (type === 'ESSAY' || type === 'FILL_IN_BLANK' || type === 'CODE' || type === 'FILE_UPLOAD' || type === 'MATCHING' || type === 'ORDERING') {
    return EssayQuestion
  }
  return SingleChoiceQuestion
})
</script>
