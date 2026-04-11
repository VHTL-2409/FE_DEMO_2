<template>
  <input
    :id="inputId"
    :value="modelValue"
    :type="type"
    :disabled="disabled"
    :placeholder="placeholder"
    :autocomplete="autocomplete"
    :aria-invalid="invalid ? 'true' : undefined"
    :aria-describedby="errorId"
    class="w-full rounded-lg border border-slate-300 bg-white px-4 py-3 text-slate-900 outline-none focus:border-indigo-500 focus:ring-2 focus:ring-indigo-500/20 disabled:bg-slate-100 disabled:text-slate-500 placeholder:text-slate-400"
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
