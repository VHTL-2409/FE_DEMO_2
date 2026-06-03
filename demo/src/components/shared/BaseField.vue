<template>
  <div class="flex flex-col gap-2">
    <div v-if="label || $slots.label" class="flex items-center justify-between gap-2">
      <label
        v-if="label || $slots.label"
        :for="fieldId"
        class="text-sm font-semibold text-[var(--ds-text)]"
      >
        <slot name="label">{{ label }}</slot>
        <span v-if="required" class="text-rose-500" aria-hidden="true"> *</span>
      </label>
      <slot name="labelTrailing" />
    </div>
    <slot
      :input-id="fieldId"
      :hint-id="hintId"
      :error-id="errorId"
    />
    <p v-if="hint && !error" :id="hintId" class="text-xs leading-relaxed text-[var(--ds-text-secondary)]">
      {{ hint }}
    </p>
    <p v-if="error" :id="errorId" class="text-sm font-semibold text-[var(--ds-danger)]" role="alert">
      {{ error }}
    </p>
  </div>
</template>

<script setup>
import { computed, useId } from 'vue'

const props = defineProps({
  label: { type: String, default: '' },
  /** Nếu không truyền, tự sinh id duy nhất cho input con */
  forId: { type: String, default: undefined },
  hint: { type: String, default: '' },
  error: { type: String, default: '' },
  required: { type: Boolean, default: false }
})

const uid = useId()
const fieldId = computed(() => props.forId || `fld-${uid}`)
const hintId = computed(() => (props.hint ? `${fieldId.value}-hint` : undefined))
const errorId = computed(() => (props.error ? `${fieldId.value}-err` : undefined))
</script>
