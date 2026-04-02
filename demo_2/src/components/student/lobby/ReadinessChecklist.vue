/**
 * ReadinessChecklist.vue
 * Checklist component for exam readiness verification
 */
<template>
  <div class="readiness-checklist">
    <!-- Header -->
    <div class="readiness-checklist__header">
      <div class="readiness-checklist__icon">
        <LucideIcon name="checklist" />
      </div>
      <div>
        <h3 class="readiness-checklist__title">{{ title }}</h3>
        <p class="readiness-checklist__subtitle">{{ subtitle }}</p>
      </div>
    </div>

    <!-- Checklist items -->
    <div class="readiness-checklist__items">
      <div
        v-for="item in items"
        :key="item.id"
        class="readiness-checklist__item"
        :class="{
          'readiness-checklist__item--completed': item.completed,
          'readiness-checklist__item--warning': item.warning,
          'readiness-checklist__item--error': item.error
        }"
      >
        <div class="readiness-checklist__item-icon">
          <LucideIcon
            :name="getItemIcon(item)"
            :class="{ 'animate-pulse': item.warning && !item.completed }"
          />
        </div>
        <div class="readiness-checklist__item-content">
          <span class="readiness-checklist__item-label">{{ item.label }}</span>
          <span v-if="item.description" class="readiness-checklist__item-desc">
            {{ item.description }}
          </span>
        </div>
        <div class="readiness-checklist__item-status">
          <span v-if="item.completed" class="readiness-checklist__check">
            <LucideIcon name="check_circle" />
          </span>
          <span v-else-if="item.warning" class="readiness-checklist__warning">
            <LucideIcon name="warning" />
          </span>
          <span v-else-if="item.error" class="readiness-checklist__error-icon">
            <LucideIcon name="error" />
          </span>
        </div>
      </div>
    </div>

    <!-- Progress bar -->
    <div class="readiness-checklist__progress-wrapper">
      <div class="readiness-checklist__progress-info">
        <span class="readiness-checklist__progress-label">Tiến độ kiểm tra</span>
        <span class="readiness-checklist__progress-value">{{ completedCount }}/{{ items.length }}</span>
      </div>
      <div class="readiness-checklist__progress-bar">
        <div
          class="readiness-checklist__progress-fill"
          :style="{ width: `${progressPercent}%` }"
          :class="{ 'readiness-checklist__progress-fill--complete': isAllComplete }"
        />
      </div>
    </div>

    <!-- Ready indicator -->
    <div v-if="isAllComplete" class="readiness-checklist__ready">
      <div class="readiness-checklist__ready-icon">
        <LucideIcon name="verified" />
      </div>
      <span>{{ readyMessage }}</span>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import LucideIcon from '../common/LucideIcon.vue'

const props = defineProps({
  title: {
    type: String,
    default: 'Kiểm tra sẵn sàng'
  },
  subtitle: {
    type: String,
    default: 'Hoàn thành các bước dưới đây để bắt đầu thi'
  },
  items: {
    type: Array,
    default: () => []
  },
  readyMessage: {
    type: String,
    default: 'Bạn đã sẵn sàng để thi!'
  }
})

const completedCount = computed(() => {
  return props.items.filter(item => item.completed).length
})

const progressPercent = computed(() => {
  if (!props.items.length) return 0
  return Math.round((completedCount.value / props.items.length) * 100)
})

const isAllComplete = computed(() => {
  return completedCount.value === props.items.length && props.items.length > 0
})

const getItemIcon = (item) => {
  if (item.completed) return 'check_circle'
  if (item.error) return 'error'
  if (item.warning) return 'warning'
  if (item.icon) return item.icon
  return 'radio_button_unchecked'
}
</script>

<style scoped>
.readiness-checklist {
  background: var(--ds-surface);
  border: 1.5px solid var(--ds-border);
  border-radius: var(--ds-radius-2xl);
  overflow: hidden;
}

.dark .readiness-checklist {
  border-color: var(--ds-border-strong);
}

/* Header */
.readiness-checklist__header {
  display: flex;
  align-items: center;
  gap: 0.875rem;
  padding: 1.125rem;
  border-bottom: 1px solid var(--ds-border);
  background: var(--ds-gray-50);
}

.dark .readiness-checklist__header {
  border-bottom-color: var(--ds-border-strong);
  background: var(--ds-gray-800);
}

.readiness-checklist__icon {
  width: 44px;
  height: 44px;
  border-radius: var(--ds-radius-xl);
  background: var(--ds-primary-soft);
  color: var(--ds-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.readiness-checklist__title {
  font-family: var(--ds-font-display);
  font-size: 0.9rem;
  font-weight: 800;
  color: var(--ds-text);
  margin: 0;
}

.dark .readiness-checklist__title {
  color: #f1f5f9;
}

.readiness-checklist__subtitle {
  font-size: 0.75rem;
  color: var(--ds-text-muted);
  margin: 0.25rem 0 0;
  font-weight: 500;
}

/* Items */
.readiness-checklist__items {
  padding: 0.875rem;
  display: flex;
  flex-direction: column;
  gap: 0.625rem;
}

.readiness-checklist__item {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.75rem;
  border-radius: var(--ds-radius-xl);
  background: var(--ds-gray-50);
  border: 1.5px solid transparent;
  transition: all 0.2s ease;
}

.dark .readiness-checklist__item {
  background: var(--ds-gray-800);
}

.readiness-checklist__item--completed {
  background: var(--ds-success-soft);
  border-color: rgba(22, 163, 74, 0.15);
}

.readiness-checklist__item--warning {
  background: rgba(234, 179, 8, 0.08);
  border-color: rgba(234, 179, 8, 0.2);
}

.readiness-checklist__item--error {
  background: var(--ds-danger-soft);
  border-color: rgba(220, 38, 38, 0.15);
}

.readiness-checklist__item-icon {
  width: 36px;
  height: 36px;
  border-radius: var(--ds-radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  background: var(--ds-surface);
  color: var(--ds-text-muted);
}

.dark .readiness-checklist__item-icon {
  background: var(--ds-gray-700);
}

.readiness-checklist__item--completed .readiness-checklist__item-icon {
  background: var(--ds-success-soft);
  color: var(--ds-success);
}

.readiness-checklist__item--warning .readiness-checklist__item-icon {
  background: rgba(234, 179, 8, 0.1);
  color: var(--ds-warning);
}

.readiness-checklist__item--error .readiness-checklist__item-icon {
  background: var(--ds-danger-soft);
  color: var(--ds-danger);
}

.readiness-checklist__item-content {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 0.125rem;
}

.readiness-checklist__item-label {
  font-size: 0.85rem;
  font-weight: 700;
  color: var(--ds-text);
}

.dark .readiness-checklist__item-label {
  color: #f1f5f9;
}

.readiness-checklist__item-desc {
  font-size: 0.7rem;
  color: var(--ds-text-muted);
  font-weight: 500;
}

.readiness-checklist__item-status {
  flex-shrink: 0;
}

.readiness-checklist__check {
  color: var(--ds-success);
  font-size: 1.25rem;
}

.readiness-checklist__warning {
  color: var(--ds-warning);
  font-size: 1.25rem;
}

.readiness-checklist__error-icon {
  color: var(--ds-danger);
  font-size: 1.25rem;
}

/* Progress */
.readiness-checklist__progress-wrapper {
  padding: 0.875rem 1.125rem;
  border-top: 1px solid var(--ds-border);
  background: var(--ds-gray-50);
}

.dark .readiness-checklist__progress-wrapper {
  border-top-color: var(--ds-border-strong);
  background: var(--ds-gray-800);
}

.readiness-checklist__progress-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.5rem;
}

.readiness-checklist__progress-label {
  font-size: 0.7rem;
  font-weight: 600;
  color: var(--ds-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.readiness-checklist__progress-value {
  font-family: var(--ds-font-display);
  font-size: 0.8rem;
  font-weight: 800;
  color: var(--ds-primary);
}

.readiness-checklist__progress-bar {
  height: 0.5rem;
  background: var(--ds-border);
  border-radius: var(--ds-radius-full);
  overflow: hidden;
}

.dark .readiness-checklist__progress-bar {
  background: var(--ds-border-strong);
}

.readiness-checklist__progress-fill {
  height: 100%;
  background: var(--ds-primary);
  border-radius: var(--ds-radius-full);
  transition: width 0.5s ease;
}

.readiness-checklist__progress-fill--complete {
  background: var(--ds-success);
}

/* Ready indicator */
.readiness-checklist__ready {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  padding: 0.875rem;
  background: var(--ds-success-soft);
  border-top: 1px solid rgba(22, 163, 74, 0.15);
  color: var(--ds-success);
  font-size: 0.85rem;
  font-weight: 700;
}

.readiness-checklist__ready-icon {
  font-size: 1.25rem;
}
</style>
