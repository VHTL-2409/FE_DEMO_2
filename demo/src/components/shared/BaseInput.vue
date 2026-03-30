<template>
  <input
    :id="inputId"
    :value="modelValue"
    :type="type"
    :disabled="disabled"
    :placeholder="placeholder"
    :autocomplete="autocomplete"
    class="w-full rounded-lg border border-slate-300 bg-white px-4 py-3 text-slate-900 outline-none focus:border-indigo-500 focus:ring-2 focus:ring-indigo-500/20 disabled:bg-slate-100 disabled:text-slate-500 placeholder:text-slate-400"
    :class="inputClass"
    @input="$emit('update:modelValue', $event.target.value)"
  />
</template>

<script setup>
import { useId } from 'vue'

defineProps({
  modelValue: { type: [String, Number], default: '' },
  type: { type: String, default: 'text' },
  disabled: { type: Boolean, default: false },
  placeholder: { type: String, default: '' },
  autocomplete: { type: String, default: undefined },
  id: { type: String, default: undefined },
  inputClass: { type: [String, Array, Object], default: undefined }
})

defineEmits(['update:modelValue'])

const inputId = useId()
</script>
