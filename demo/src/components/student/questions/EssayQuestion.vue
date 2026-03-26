<template>
  <div class="space-y-3">
    <textarea
      :value="normalizedValue"
      :disabled="disabled"
      rows="8"
      class="w-full rounded-2xl border border-slate-200 bg-white px-4 py-3 text-base text-slate-900 shadow-sm outline-none transition focus:border-primary focus:ring-4 focus:ring-primary/10 disabled:cursor-not-allowed disabled:bg-slate-100 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-100 dark:disabled:bg-slate-800"
      :placeholder="question?.placeholder || question?.metadata?.placeholder || 'Nhập câu trả lời của bạn...'"
      @input="$emit('update:modelValue', $event.target.value)"
    />
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
    type: String,
    default: ''
  },
  disabled: {
    type: Boolean,
    default: false
  }
})

defineEmits(['update:modelValue'])

const normalizedValue = computed(() => String(props.modelValue || ''))
</script>
