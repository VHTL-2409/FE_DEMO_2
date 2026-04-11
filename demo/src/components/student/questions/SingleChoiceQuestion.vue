<template>
  <div class="eq-opts">
    <label
      v-for="option in normalizedOptions"
      :key="option.id"
      :class="['eq-opt', isSelected(option) ? 'eq-opt--selected' : 'eq-opt--idle']"
    >
      <input
        :checked="isSelected(option)"
        :disabled="disabled"
        class="eq-opt__input"
        :name="`exam-option-${question?.id || 'single'}`"
        type="radio"
        :value="resolveOptionValue(option)"
        @change="onPick(option)"
      />
      <span class="eq-opt__radio" aria-hidden="true" />
      <span class="eq-opt__text">
        <MathDisplay
          :content="option.text"
          :latex-content="getLatexOption(option)"
          source-preference="latex-first"
        />
      </span>
    </label>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import MathDisplay from '../../shared/MathDisplay.vue'

const props = defineProps({
  question: {
    type: Object,
    default: () => ({})
  },
  modelValue: {
    type: [String, Number],
    default: ''
  },
  disabled: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:modelValue'])

const normalizedOptions = computed(() => {
  const opts = props.question?.options || []
  return Array.isArray(opts) ? opts : []
})

const resolveOptionValue = (option) => String(option?.originalId ?? option?.original_id ?? option?.id ?? '')

// Get LaTeX version of an option if available
const getLatexOption = (option) => {
  const optionId = resolveOptionValue(option)
  const latexOptions = props.question?.latexOptions
  if (!latexOptions) return null
  // latexOptions can be object or JSON string
  if (typeof latexOptions === 'string') {
    try {
      const parsed = JSON.parse(latexOptions)
      return parsed[optionId] || parsed[String(optionId).toUpperCase()] || null
    } catch {
      return null
    }
  }
  return latexOptions[optionId] || latexOptions[String(optionId).toUpperCase()] || null
}

const isSelected = (option) => String(props.modelValue ?? '') === resolveOptionValue(option)

const onPick = (option) => {
  emit('update:modelValue', resolveOptionValue(option))
}
</script>

<style scoped>
.eq-opts {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  padding-bottom: 0.5rem;
}

.eq-opt {
  position: relative;
  display: flex;
  align-items: flex-start;
  gap: 0.875rem;
  margin: 0;
  padding: 1rem 1.125rem;
  border-radius: var(--ds-radius-xl, 1rem);
  border: 2px solid var(--ds-border, #e2e8f0);
  background: var(--ds-surface, #fff);
  cursor: pointer;
  transition: border-color 0.15s ease, background-color 0.15s ease, box-shadow 0.15s ease;
}

.dark .eq-opt {
  border-color: var(--ds-border-strong, rgba(148, 163, 184, 0.35));
  background: rgba(30, 41, 59, 0.45);
}

.eq-opt--idle:hover:not(:has(input:disabled)) {
  border-color: var(--ds-primary-border, rgba(79, 70, 229, 0.35));
  background: var(--ds-primary-soft, rgba(79, 70, 229, 0.08));
}

.eq-opt--selected {
  border-color: var(--ds-primary, #4f46e5);
  background: var(--ds-primary-soft, rgba(79, 70, 229, 0.12));
  box-shadow: 0 0 0 3px var(--ds-primary-ring, rgba(79, 70, 229, 0.2));
}

.dark .eq-opt--selected {
  background: rgba(79, 70, 229, 0.18);
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.25);
}

.eq-opt__input {
  position: absolute;
  inset: 0;
  z-index: 2;
  margin: 0;
  opacity: 0;
  cursor: pointer;
  width: 100%;
  height: 100%;
}

.eq-opt__radio {
  flex-shrink: 0;
  width: 1.25rem;
  height: 1.25rem;
  margin-top: 0.15rem;
  border-radius: 9999px;
  border: 2px solid var(--ds-gray-400, #94a3b8);
  background: var(--ds-surface, #fff);
  box-shadow: inset 0 0 0 3px var(--ds-surface, #fff);
  transition: border-color 0.12s ease, background-color 0.12s ease;
}

.dark .eq-opt__radio {
  background: var(--ds-gray-800, #1e293b);
  box-shadow: inset 0 0 0 3px var(--ds-gray-800, #1e293b);
}

.eq-opt--selected .eq-opt__radio {
  border-color: var(--ds-primary, #4f46e5);
  background: var(--ds-primary, #4f46e5);
  box-shadow: inset 0 0 0 3px var(--ds-surface, #fff);
}

.dark .eq-opt--selected .eq-opt__radio {
  box-shadow: inset 0 0 0 3px var(--ds-gray-800, #1e293b);
}

.eq-opt__text {
  flex: 1;
  font-size: 1rem;
  font-weight: 600;
  line-height: 1.45;
  color: var(--ds-text, #0f172a);
}

.dark .eq-opt__text {
  color: #f1f5f9;
}

.eq-opt--selected .eq-opt__text {
  color: var(--ds-text, #0f172a);
}

.dark .eq-opt--selected .eq-opt__text {
  color: #f8fafc;
}

.eq-opt:has(input:disabled) {
  opacity: 0.55;
  cursor: not-allowed;
}

.eq-opt:has(.eq-opt__input:focus-visible) {
  outline: 2px solid var(--ds-primary, #4f46e5);
  outline-offset: 2px;
}

@media (prefers-reduced-motion: reduce) {
  .eq-opt,
  .eq-opt__radio {
    transition: none;
  }
}
</style>
