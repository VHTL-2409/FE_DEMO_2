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
    class="base-input portal-focus w-full outline-none"
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

<style scoped>
.base-input {
  border-radius: 1rem;
  border: 1px solid rgba(148, 163, 184, 0.22);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.92) 0%, rgba(247, 250, 255, 0.84) 100%);
  padding: 0.88rem 1rem;
  color: var(--color-text);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.86),
    0 14px 28px -24px rgba(15, 23, 42, 0.2);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  transition: border-color 180ms ease, box-shadow 180ms ease, background-color 180ms ease;
}

.base-input::placeholder {
  color: #98a2b3;
}

.base-input:hover {
  border-color: rgba(148, 163, 184, 0.34);
}

.base-input:focus {
  border-color: rgba(89, 98, 243, 0.24);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.92),
    0 0 0 4px rgba(89, 98, 243, 0.12),
    0 16px 30px -24px rgba(15, 23, 42, 0.22);
  background: rgba(255, 255, 255, 0.98);
}

.base-input:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

.dark .base-input {
  border-color: rgba(148, 163, 184, 0.16);
  background: linear-gradient(180deg, rgba(16, 24, 38, 0.94) 0%, rgba(30, 41, 59, 0.88) 100%);
  color: var(--color-text);
}

.dark .base-input::placeholder {
  color: #64748b;
}

.dark .base-input:hover {
  border-color: rgba(148, 163, 184, 0.24);
}

.dark .base-input:focus {
  border-color: rgba(129, 140, 248, 0.26);
  background: rgba(16, 24, 38, 0.98);
}
</style>
