<template>
  <textarea
    :id="inputId"
    :value="modelValue"
    :disabled="disabled"
    :placeholder="placeholder"
    :name="name"
    :rows="rows"
    :maxlength="maxlength"
    :aria-invalid="invalid ? 'true' : undefined"
    :aria-describedby="describedBy"
    class="w-full resize-y rounded-[var(--ds-radius-lg)] border border-[var(--ds-border)] bg-[var(--ds-surface)] px-3.5 py-2.5 text-sm leading-relaxed text-[var(--ds-text)] shadow-[var(--ds-shadow-xs)] outline-none transition-[border-color,box-shadow,background-color] duration-150 placeholder:text-[var(--ds-text-muted)] focus:border-[var(--ds-primary)] focus:ring-2 focus:ring-[var(--ds-primary-ring)] disabled:bg-[var(--ds-gray-100)] disabled:text-[var(--ds-text-muted)]"
    :class="[invalid ? 'border-[var(--ds-danger)] focus:border-[var(--ds-danger)] focus:ring-[var(--ds-danger-soft)]' : '', inputClass]"
    @input="$emit('update:modelValue', $event.target.value)"
  />
</template>

<script setup>
import { computed, useId } from 'vue'

const props = defineProps({
  modelValue: { type: [String, Number], default: '' },
  disabled: { type: Boolean, default: false },
  placeholder: { type: String, default: '' },
  id: { type: String, default: undefined },
  name: { type: String, default: undefined },
  rows: { type: [String, Number], default: 4 },
  maxlength: { type: [String, Number], default: undefined },
  inputClass: { type: [String, Array, Object], default: undefined },
  invalid: { type: Boolean, default: false },
  describedBy: { type: String, default: undefined }
})

defineEmits(['update:modelValue'])

const generatedId = useId()
const inputId = computed(() => props.id || generatedId)
</script>
