<template>
  <select
    :id="inputId"
    :value="modelValue"
    :disabled="disabled"
    :name="name"
    :aria-invalid="invalid ? 'true' : undefined"
    :aria-describedby="describedBy"
    class="w-full rounded-[var(--ds-radius-lg)] border border-[var(--ds-border)] bg-[var(--ds-surface)] px-3.5 py-2.5 text-sm text-[var(--ds-text)] shadow-[var(--ds-shadow-xs)] outline-none transition-[border-color,box-shadow,background-color] duration-150 focus:border-[var(--ds-primary)] focus:ring-2 focus:ring-[var(--ds-primary-ring)] disabled:bg-[var(--ds-gray-100)] disabled:text-[var(--ds-text-muted)]"
    :class="[invalid ? 'border-[var(--ds-danger)] focus:border-[var(--ds-danger)] focus:ring-[var(--ds-danger-soft)]' : '', inputClass]"
    @change="$emit('update:modelValue', $event.target.value)"
  >
    <option v-if="placeholder" value="" :disabled="placeholderDisabled">
      {{ placeholder }}
    </option>
    <option
      v-for="option in normalizedOptions"
      :key="option.value"
      :value="option.value"
      :disabled="option.disabled"
    >
      {{ option.label }}
    </option>
    <slot />
  </select>
</template>

<script setup>
import { computed, useId } from 'vue'

const props = defineProps({
  modelValue: { type: [String, Number, Boolean], default: '' },
  options: { type: Array, default: () => [] },
  disabled: { type: Boolean, default: false },
  placeholder: { type: String, default: '' },
  placeholderDisabled: { type: Boolean, default: true },
  id: { type: String, default: undefined },
  name: { type: String, default: undefined },
  inputClass: { type: [String, Array, Object], default: undefined },
  invalid: { type: Boolean, default: false },
  describedBy: { type: String, default: undefined }
})

defineEmits(['update:modelValue'])

const generatedId = useId()
const inputId = computed(() => props.id || generatedId)
const normalizedOptions = computed(() =>
  props.options.map((option) => {
    if (typeof option === 'string' || typeof option === 'number' || typeof option === 'boolean') {
      return { label: String(option), value: option, disabled: false }
    }
    return {
      label: option.label ?? String(option.value ?? ''),
      value: option.value ?? option.label ?? '',
      disabled: Boolean(option.disabled)
    }
  })
)
</script>
