<template>
  <div class="flex flex-col gap-2">
    <div v-if="label || $slots.label" class="flex items-center justify-between gap-2">
      <label
        v-if="label || $slots.label"
        :for="fieldId"
        class="text-sm font-medium text-slate-700 dark:text-slate-300"
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
    <p v-if="hint && !error" :id="hintId" class="text-xs text-slate-500 dark:text-slate-400">
      {{ hint }}
    </p>
    <p v-if="error" :id="errorId" class="text-sm font-medium text-rose-600 dark:text-rose-400" role="alert">
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
