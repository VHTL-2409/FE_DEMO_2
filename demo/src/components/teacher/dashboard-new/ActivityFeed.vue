<template>
  <div class="ntd-feed">
    <div v-if="$slots.header" class="ntd-feed__header">
      <slot name="header" />
    </div>

    <div v-if="loading && !items.length" class="ntd-feed__loading">
      <div v-for="i in 4" :key="i" class="ntd-feed__skeleton">
        <div class="ntd-feed__skeleton-dot" />
        <div class="ntd-feed__skeleton-content">
          <div class="ntd-feed__skeleton-line short" />
          <div class="ntd-feed__skeleton-line" />
        </div>
      </div>
    </div>

    <div v-else-if="!items.length" class="ntd-feed__empty">
      <LucideIcon name="inbox" size="32" />
      <p>{{ emptyText }}</p>
    </div>

    <div v-else class="ntd-feed__list">
      <div
        v-for="(item, index) in items"
        :key="item.id || index"
        class="ntd-feed__item"
        :class="`ntd-feed__item--${item.type || 'info'}`"
        :style="{ animationDelay: `${index * 0.06}s` }"
      >
        <div class="ntd-feed__item-dot">
          <LucideIcon :name="getIcon(item)" :size="14" />
        </div>
        <div class="ntd-feed__item-body">
          <div class="ntd-feed__item-text">
            <span class="ntd-feed__item-highlight">{{ item.highlight }}</span>
            {{ item.text }}
          </div>
          <div class="ntd-feed__item-meta">
            <span v-if="item.time" class="ntd-feed__item-time">{{ item.time }}</span>
            <span v-if="item.badge" class="ntd-feed__item-badge" :class="`ntd-feed__item-badge--${item.badgeVariant || 'default'}`">
              {{ item.badge }}
            </span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
// LucideIcon is registered globally via main.js — no local import needed

const props = defineProps({
  items: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false },
  emptyText: { type: String, default: 'Không có hoạt động nào' },
  maxHeight: { type: String, default: 'none' }
})

const iconMap = {
  submit: 'send',
  violation: 'warning',
  warning: 'alert-circle',
  exam_start: 'play',
  exam_end: 'check_circle',
  student_join: 'user-plus',
  score: 'grade',
  info: 'info',
  review: 'file_check_2'
}

const getIcon = (item) => iconMap[item.type] || iconMap.info
</script>

<style scoped>
.ntd-feed {
  display: flex;
  flex-direction: column;
  gap: 0;
}

.ntd-feed__header {
  padding: 1.125rem 1.375rem 0.75rem;
  border-bottom: 1px solid var(--ds-border, #e5e7eb);
}

.ntd-feed__loading {
  padding: 1rem 1.375rem;
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.ntd-feed__skeleton {
  display: flex;
  align-items: flex-start;
  gap: 0.75rem;
}

.ntd-feed__skeleton-dot {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: skeleton-shimmer 1.4s ease infinite;
  flex-shrink: 0;
  margin-top: 2px;
}

.ntd-feed__skeleton-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
}

.ntd-feed__skeleton-line {
  height: 12px;
  border-radius: 6px;
  background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: skeleton-shimmer 1.4s ease infinite;
}

.ntd-feed__skeleton-line.short {
  width: 60%;
}

@keyframes skeleton-shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

.ntd-feed__empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 2.5rem 1rem;
  gap: 0.5rem;
  color: var(--ds-text-muted, #9ca3af);
  text-align: center;
}

.ntd-feed__empty p {
  font-size: 0.875rem;
  margin: 0;
}

.ntd-feed__list {
  padding: 0.75rem 0;
  overflow-y: auto;
  max-height: v-bind(maxHeight);
}

.ntd-feed__item {
  display: flex;
  align-items: flex-start;
  gap: 0.75rem;
  padding: 0.625rem 1.375rem;
  transition: background 0.15s;
  animation: feed-slide-in 0.4s ease-out both;
}

.ntd-feed__item:hover {
  background: var(--ds-gray-50, #f9fafb);
}

.ntd-feed__item-dot {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  margin-top: 1px;
  transition: transform 0.15s;
}

.ntd-feed__item:hover .ntd-feed__item-dot {
  transform: scale(1.1);
}

.ntd-feed__item--submit .ntd-feed__item-dot {
  background: rgba(16, 185, 129, 0.12);
  color: #10b981;
}

.ntd-feed__item--violation .ntd-feed__item-dot {
  background: rgba(239, 68, 68, 0.12);
  color: #ef4444;
}

.ntd-feed__item--warning .ntd-feed__item-dot {
  background: rgba(245, 158, 11, 0.12);
  color: #f59e0b;
}

.ntd-feed__item--exam_start .ntd-feed__item-dot,
.ntd-feed__item--exam_end .ntd-feed__item-dot {
  background: rgba(79, 70, 229, 0.12);
  color: #4f46e5;
}

.ntd-feed__item--student_join .ntd-feed__item-dot {
  background: rgba(14, 165, 233, 0.12);
  color: #0ea5e9;
}

.ntd-feed__item--score .ntd-feed__item-dot {
  background: rgba(168, 85, 247, 0.12);
  color: #a855f7;
}

.ntd-feed__item--review .ntd-feed__item-dot {
  background: rgba(6, 182, 212, 0.12);
  color: #06b6d4;
}

.ntd-feed__item--info .ntd-feed__item-dot {
  background: rgba(107, 114, 128, 0.12);
  color: #6b7280;
}

.ntd-feed__item-body {
  flex: 1;
  min-width: 0;
}

.ntd-feed__item-text {
  font-size: 0.8125rem;
  line-height: 1.5;
  color: var(--ds-text, #374151);
  word-break: break-word;
}

.ntd-feed__item-highlight {
  font-weight: 700;
  color: var(--ds-text, #1f2937);
}

.ntd-feed__item-meta {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-top: 0.25rem;
}

.ntd-feed__item-time {
  font-size: 0.6875rem;
  color: var(--ds-text-muted, #9ca3af);
}

.ntd-feed__item-badge {
  font-size: 0.6875rem;
  font-weight: 600;
  padding: 0.125rem 0.5rem;
  border-radius: 999px;
  background: rgba(107, 114, 128, 0.1);
  color: #6b7280;
}

.ntd-feed__item-badge--success {
  background: rgba(16, 185, 129, 0.1);
  color: #10b981;
}

.ntd-feed__item-badge--danger {
  background: rgba(239, 68, 68, 0.1);
  color: #ef4444;
}

.ntd-feed__item-badge--warning {
  background: rgba(245, 158, 11, 0.1);
  color: #f59e0b;
}

.ntd-feed__item-badge--info {
  background: rgba(14, 165, 233, 0.1);
  color: #0ea5e9;
}

@keyframes feed-slide-in {
  from {
    opacity: 0;
    transform: translateX(-8px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}
</style>
