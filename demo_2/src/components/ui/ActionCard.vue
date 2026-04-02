<template>
  <component
    :is="tag"
    :type="tag === 'button' ? type : undefined"
    :disabled="disabled || loading"
    :class="rootClass"
    class="ds-action-card group relative flex items-start gap-3 rounded-[var(--ds-radius-xl)] border border-[var(--ds-border)] bg-[var(--ds-surface)] p-4 transition-all duration-200 hover:-translate-y-0.5 hover:border-[var(--ds-primary-border)] hover:bg-[var(--ds-primary-soft)] hover:shadow-[var(--ds-shadow-md)] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[var(--ds-primary)] focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50"
    v-bind="tag === 'a' ? { href: to } : {}"
    @click="!disabled && !loading && handleClick"
  >
    <!-- Icon -->
    <span
      class="ds-action-card__icon flex size-9 shrink-0 items-center justify-center rounded-[var(--ds-radius-lg)] bg-[var(--ds-primary-soft)] text-[var(--ds-primary)] transition-colors group-hover:bg-[var(--ds-primary)] group-hover:text-white"
    >
      <LucideIcon :name="icon" />
    </span>

    <!-- Text -->
    <div class="min-w-0 flex-1">
      <p class="text-sm font-semibold text-[var(--ds-text)] group-hover:text-[var(--ds-primary)]">
        {{ title }}
      </p>
      <p v-if="description" class="mt-0.5 text-xs text-[var(--ds-text-muted)] leading-relaxed">
        {{ description }}
      </p>
    </div>

    <!-- Arrow -->
    <LucideIcon name="arrow_forward" />
  </component>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  icon: { type: String, default: 'arrow_forward' },
  title: { type: String, required: true },
  description: { type: String, default: '' },
  to: { type: String, default: '' },
  href: { type: String, default: '' },
  tag: { type: String, default: 'button' },
  type: { type: String, default: 'button' },
  disabled: { type: Boolean, default: false },
  loading: { type: Boolean, default: false },
  variant: {
    type: String,
    default: 'default',
    validator: (v) => ['default', 'primary', 'outline', 'danger'].includes(v)
  }
})

const emit = defineEmits(['click'])

const rootClass = computed(() => {
  if (props.variant === 'primary') {
    return 'ds-action-card--primary bg-[var(--ds-primary-soft)] border-[var(--ds-primary-border)] text-[var(--ds-primary)]'
  }
  if (props.variant === 'outline') {
    return 'ds-action-card--outline'
  }
  if (props.variant === 'danger') {
    return 'hover:bg-[var(--ds-danger-soft)] hover:border-[rgba(220,38,38,0.2)]'
  }
  return ''
})

const handleClick = (e) => {
  if (props.to) {
    const router = window.__VUE_DEVTOOLS_TOAST__ ? null : null
    // Use Vue Router if available through provide/inject
    return
  }
  emit('click', e)
}
</script>

