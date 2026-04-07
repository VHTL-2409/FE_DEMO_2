<template>
  <div :class="rootClass" class="ds-empty" role="status">
    <!-- Illustration / Icon -->
    <div v-if="icon || variantIcon" class="ds-empty__icon">
      <LucideIcon :name="variantIcon || icon" />
    </div>
    <div v-else-if="variant === 'no-data'" class="ds-empty__icon ds-empty__icon--no-data">
      <LucideIcon name="inbox" />
    </div>
    <div v-else-if="variant === 'no-results'" class="ds-empty__icon ds-empty__icon--no-results">
      <LucideIcon name="search_x" />
    </div>
    <div v-else-if="variant === 'error'" class="ds-empty__icon ds-empty__icon--error">
      <LucideIcon name="alert_triangle" />
    </div>
    <div v-else-if="variant === 'loading'" class="ds-empty__icon ds-empty__icon--loading">
      <LucideIcon name="loader_2" class="ds-empty__spinner" />
    </div>
    <div v-else-if="variant === 'success'" class="ds-empty__icon ds-empty__icon--success">
      <LucideIcon name="check_circle" />
    </div>

    <!-- Title -->
    <h3 class="ds-empty__title">{{ title }}</h3>

    <!-- Description -->
    <p v-if="description" class="ds-empty__desc">{{ description }}</p>

    <!-- Action Button -->
    <button
      v-if="actionLabel"
      class="ds-empty__action ds-btn ds-btn--primary"
      @click="$emit('action')"
    >
      <LucideIcon v-if="actionIcon" :name="actionIcon" />
      {{ actionLabel }}
    </button>

    <!-- Secondary action -->
    <button
      v-if="secondaryActionLabel"
      class="ds-empty__secondary-action ds-empty__action ds-btn ds-btn--ghost"
      @click="$emit('secondary-action')"
    >
      {{ secondaryActionLabel }}
    </button>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import LucideIcon from './LucideIcon.vue'

const props = defineProps({
  icon: { type: String, default: '' },
  title: { type: String, required: true },
  description: { type: String, default: '' },
  actionLabel: { type: String, default: '' },
  actionIcon: { type: String, default: '' },
  secondaryActionLabel: { type: String, default: '' },
  dense: { type: Boolean, default: false },
  fill: { type: Boolean, default: false },
  variant: {
    type: String,
    default: 'default',
    validator: (v) => ['default', 'no-data', 'no-results', 'error', 'loading', 'success'].includes(v)
  }
})

defineEmits(['action', 'secondary-action'])

const rootClass = computed(() => {
  const classes = []
  if (props.dense) classes.push('ds-empty--dense')
  if (props.fill) classes.push('ds-empty--fill')
  return classes.join(' ')
})
</script>

<style scoped>
.ds-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  padding: 3rem 1.5rem;
  gap: 0.75rem;
}

.ds-empty--dense { padding: 2rem 1rem; }
.ds-empty--fill { width: 100%; height: 100%; min-height: 200px; }

.ds-empty__icon {
  width: 4rem;
  height: 4rem;
  border-radius: var(--ds-radius-2xl);
  background: var(--ds-gray-100);
  color: var(--ds-text-muted);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 0.5rem;
}

.dark .ds-empty__icon {
  background: var(--ds-gray-800);
  color: #94a3b8;
}

.ds-empty__icon :deep(.lucide) { font-size: 2rem; }

.ds-empty__icon--no-data {
  background: var(--ds-primary-soft);
  color: var(--ds-primary);
}

.ds-empty__icon--no-results {
  background: var(--ds-warning-soft);
  color: var(--ds-warning);
}

.ds-empty__icon--error {
  background: var(--ds-danger-soft);
  color: var(--ds-danger);
}

.ds-empty__icon--loading {
  background: var(--ds-warning-soft);
  color: var(--ds-warning);
}

.ds-empty__icon--success {
  background: var(--ds-success-soft);
  color: var(--ds-success);
}

.ds-empty__spinner {
  animation: dsSpin 1s linear infinite;
}

@keyframes dsSpin { to { transform: rotate(360deg) translateZ(0); } }

.ds-empty__title {
  font-family: var(--ds-font-display);
  font-size: 1rem;
  font-weight: 800;
  color: var(--ds-text);
  margin: 0;
  line-height: 1.3;
}

.dark .ds-empty__title { color: #f1f5f9; }

.ds-empty__desc {
  font-size: 0.875rem;
  color: var(--ds-text-muted);
  margin: 0;
  max-width: 360px;
  line-height: 1.6;
  font-weight: 500;
}

.ds-empty__action {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  margin-top: 0.5rem;
}

.ds-empty__action :deep(.lucide) { font-size: 1rem; }

.ds-empty__secondary-action {
  font-size: 0.8125rem;
  color: var(--ds-text-muted);
  font-weight: 600;
}

.ds-empty__secondary-action:hover {
  color: var(--ds-primary);
}
</style>
