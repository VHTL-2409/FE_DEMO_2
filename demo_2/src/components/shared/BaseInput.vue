<template>
  <!-- Float label mode -->
  <div
    v-if="float"
    class="gs-field"
    :class="{ 'gs-field--error': invalid }"
  >
    <input
      :id="inputId"
      :value="modelValue"
      :type="type"
      :disabled="disabled"
      placeholder=" "
      :autocomplete="autocomplete"
      :aria-invalid="invalid ? 'true' : undefined"
      :aria-describedby="describedBy"
      class="gs-field__input portal-focus"
      :class="inputClass"
      @input="onInput"
    />
    <label :for="inputId" class="gs-field__label">
      {{ placeholder }}
    </label>
    <p v-if="invalid && errorMessage" :id="errorId" class="gs-field__error">
      <LucideIcon name="error" class="gs-field__error-icon" />
      {{ errorMessage }}
    </p>
  </div>

  <!-- Standard input mode -->
  <input
    v-else
    :id="inputId"
    :value="modelValue"
    :type="type"
    :disabled="disabled"
    :placeholder="placeholder"
    :autocomplete="autocomplete"
    :aria-invalid="invalid ? 'true' : undefined"
    :aria-describedby="describedBy"
    class="gs-input portal-focus"
    :class="[{ 'gs-input--error': invalid }, inputClass]"
    @input="onInput"
  />
</template>

<script setup>
import { computed, useId } from 'vue'
import LucideIcon from '../common/LucideIcon.vue'

const props = defineProps({
  modelValue: { type: [String, Number], default: '' },
  type: { type: String, default: 'text' },
  disabled: { type: Boolean, default: false },
  placeholder: { type: String, default: '' },
  autocomplete: { type: String, default: undefined },
  id: { type: String, default: undefined },
  invalid: { type: Boolean, default: false },
  errorMessage: { type: String, default: '' },
  hintId: { type: String, default: undefined },
  errorId: { type: String, default: undefined },
  inputClass: { type: [String, Array, Object], default: undefined },
  float: { type: Boolean, default: false }
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
/* Error state for standard input */
.gs-input--error {
  border-color: var(--glass-danger) !important;
  animation: gsShake 0.4s var(--ease-out);
}

.gs-input--error:focus {
  box-shadow: 0 0 0 4px var(--glass-danger-soft) !important;
}

.gs-field__error {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--glass-danger);
  margin-top: 0.25rem;
}

.gs-field__error-icon {
  font-size: 1rem;
  flex-shrink: 0;
}

/* Dark mode adjustments */
.dark .gs-field__input,
.dark .gs-input {
  background: var(--glass-surface-warm);
  border-color: var(--glass-border);
  color: var(--glass-text);
}

.dark .gs-field__input:focus,
.dark .gs-input:focus {
  border-color: var(--glass-amber);
  box-shadow: 0 0 0 4px var(--glass-amber-soft);
  background: var(--glass-surface-hover);
}

/* Error in dark mode */
.dark .gs-input--error {
  border-color: var(--glass-danger) !important;
}

/* Portal focus override */
.portal-focus:focus-visible {
  outline: none;
}

@keyframes gsShake {
  0%, 100% { transform: translateX(0); }
  20%, 60% { transform: translateX(-4px); }
  40%, 80% { transform: translateX(4px); }
}
</style>
