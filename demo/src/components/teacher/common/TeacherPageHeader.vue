<template>
  <div class="tph">
    <div class="tph__breadcrumb">
      <RouterLink class="tph__bc-link" to="/teacher/dashboard">
        <LucideIcon name="home" />
        <span>Trang chủ</span>
      </RouterLink>
      <LucideIcon class="tph__bc-sep" name="chevron_right" />
      <span class="tph__bc-current">{{ title }}</span>
    </div>

    <div class="tph__content">
      <div class="tph__left">
        <div class="tph__icon-wrap" :class="`tph__icon-wrap--${iconColor}`">
          <LucideIcon :name="icon" />
        </div>
        <div class="tph__text">
          <h1 class="tph__title">{{ title }}</h1>
          <p v-if="subtitle" class="tph__subtitle">{{ subtitle }}</p>
        </div>
      </div>

      <div v-if="$slots.actions" class="tph__actions">
        <slot name="actions" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { RouterLink } from 'vue-router'
import LucideIcon from '../../common/LucideIcon.vue'

defineProps({
  icon: {
    type: String,
    default: 'quiz'
  },
  iconColor: {
    type: String,
    default: 'primary',
    validator: (v) => ['primary', 'success', 'warning', 'danger', 'info'].includes(v)
  },
  title: {
    type: String,
    required: true
  },
  subtitle: {
    type: String,
    default: ''
  }
})
</script>

<style scoped>
/* Ultra Simplified Page Header */

.tph {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  padding: 1.25rem 1.5rem;
  background: var(--ds-surface);
  border-bottom: 1px solid var(--ds-border);
  border-radius: var(--ds-radius-lg, 14px);
  animation: fadeUp 0.45s cubic-bezier(0.34, 1.56, 0.64, 1);
}

@keyframes fadeUp {
  from { opacity: 0; }
  to   { opacity: 1; }
}

.dark .tph {
  background: var(--ds-gray-800);
  border-color: var(--ds-border-strong);
}

.tph__breadcrumb {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  font-size: 0.75rem;
}

.tph__bc-link {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  color: var(--ds-text-muted);
  text-decoration: none;
  font-weight: 600;
  transition: color 0.15s ease;
}

.tph__bc-link:hover { color: var(--ds-primary); }
.dark .tph__bc-link:hover { color: var(--ds-primary); }

.tph__bc-sep {
  color: var(--ds-text-muted);
  width: 14px;
  height: 14px;
}

.tph__bc-current {
  font-weight: 600;
  color: var(--ds-text);
}

.dark .tph__bc-current { color: var(--ds-text); }

.tph__content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1.5rem;
  flex-wrap: wrap;
}

.tph__left {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.tph__icon-wrap {
  width: 52px;
  height: 52px;
  border-radius: var(--ds-radius-xl);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.tph__icon-wrap--primary {
  background: linear-gradient(135deg, var(--ds-primary) 0%, #818cf8 100%);
  color: white;
}

.tph__icon-wrap--success {
  background: linear-gradient(135deg, var(--ds-success) 0%, #34d399 100%);
  color: white;
}

.tph__icon-wrap--warning {
  background: linear-gradient(135deg, #d97706 0%, #fbbf24 100%);
  color: white;
}

.tph__icon-wrap--danger {
  background: linear-gradient(135deg, var(--ds-danger) 0%, #f87171 100%);
  color: white;
}

.tph__icon-wrap--info {
  background: linear-gradient(135deg, #0284c7 0%, #38bdf8 100%);
  color: white;
}

.tph__icon-wrap :deep(svg),
.tph__icon-wrap svg {
  width: 24px;
  height: 24px;
}

.tph__text {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.tph__title {
  font-family: var(--ds-font-display);
  font-size: 1.5rem;
  font-weight: 800;
  color: var(--ds-text);
  margin: 0;
  line-height: 1.2;
  letter-spacing: -0.02em;
}

.dark .tph__title { color: var(--ds-text); }

.tph__subtitle {
  font-size: 0.875rem;
  color: var(--ds-text-muted);
  margin: 0;
}

.dark .tph__subtitle { color: var(--ds-text-muted); }

.tph__actions {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex-wrap: wrap;
}

@media (max-width: 640px) {
  .tph { padding: 1rem; }
  .tph__content { flex-direction: column; align-items: flex-start; gap: 1rem; }
  .tph__icon-wrap { width: 44px; height: 44px; }
  .tph__icon-wrap :deep(svg), .tph__icon-wrap svg { width: 20px; height: 20px; }
  .tph__title { font-size: 1.25rem; }
}

@media (prefers-reduced-motion: reduce) {
  .tph { animation: none; }
}
</style>
