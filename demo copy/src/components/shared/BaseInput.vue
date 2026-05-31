<template>
  <input
    :id="inputId"
    :value="modelValue"
    :type="type"
    :disabled="disabled"
    :placeholder="placeholder"
    :autocomplete="autocomplete"
    :name="name"
    :aria-invalid="invalid ? 'true' : undefined"
    :aria-describedby="errorId"
    class="w-full rounded-[var(--ds-radius-lg)] border border-[var(--ds-border)] bg-[var(--ds-surface)] px-3.5 py-2.5 text-[var(--ds-text)] outline-none transition-[border-color,box-shadow,background-color] duration-150 focus:border-[var(--ds-primary)] focus:ring-2 focus:ring-[var(--ds-primary-ring)] disabled:bg-[var(--ds-gray-100)] disabled:text-[var(--ds-text-muted)] placeholder:text-[var(--ds-text-muted)]"
    :class="inputClass"
    @input="$emit('update:modelValue', $event.target.value)"
  />
</template>

<script setup>
import { computed, useId } from 'vue'

const props = defineProps({
  modelValue: { type: [String, Number], default: '' },
  type: { type: String, default: 'text' },
  disabled: { type: Boolean, default: false },
  placeholder: { type: String, default: '' },
  autocomplete: { type: String, default: undefined },
  id: { type: String, default: undefined },
  name: { type: String, default: undefined },
  inputClass: { type: [String, Array, Object], default: undefined },
  invalid: { type: Boolean, default: false }
})

defineEmits(['update:modelValue'])

const generatedId = useId()
const inputId = computed(() => props.id || generatedId)
const errorId = computed(() => props.invalid ? `${inputId.value}-error` : undefined)
</script>
