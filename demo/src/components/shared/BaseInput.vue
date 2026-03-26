<template>
  <input
    :id="inputId"
    :value="modelValue"
    :type="type"
    :disabled="disabled"
    :placeholder="placeholder"
    :autocomplete="autocomplete"
    :aria-invalid="invalid ? 'true' : undefined"
    :aria-describedby="describedBy"
    class="portal-focus w-full rounded-lg border border-slate-200/85 bg-white/88 px-4 py-3 text-slate-900 shadow-[inset_0_1px_0_rgba(255,255,255,0.9),0_10px_24px_-22px_rgba(15,23,42,0.35)] outline-none backdrop-blur-sm transition-[box-shadow,border-color,background-color] duration-200 placeholder:text-slate-400 hover:border-slate-300 focus:border-primary/15 focus:ring-[3px] focus:ring-primary/12 disabled:cursor-not-allowed disabled:opacity-60 dark:border-slate-600/80 dark:bg-slate-800/82 dark:text-slate-100 dark:placeholder:text-slate-500 dark:hover:border-slate-500 dark:focus:border-primary/30"
    :class="inputClass"
    @input="onInput"
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
  invalid: { type: Boolean, default: false },
  hintId: { type: String, default: undefined },
  errorId: { type: String, default: undefined },
  inputClass: { type: [String, Array, Object], default: undefined }
})

const emit = defineEmits(['update:modelValue'])

const autoId = useId()
const inputId = computed(() => props.id || `field-${autoId}`)

const describedBy = computed(() => {
  const ids = [props.hintId, props.errorId].filter(Boolean)
  return ids.length ? ids.join(' ') : undefined
})

const onInput = (e) => {
  emit('update:modelValue', e.target.value)
}
</script>
