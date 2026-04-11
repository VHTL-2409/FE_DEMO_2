<template>
  <div class="nl">
    <!-- Header -->
    <div class="nl__header">
      <div class="nl__header-left">
        <div class="nl__icon">
          <LucideIcon name="notifications" />
        </div>
        <div>
          <h3 class="nl__title">Thông báo</h3>
          <p class="nl__subtitle">{{ unreadCount > 0 ? `${unreadCount} chưa đọc` : 'Tất cả đã đọc' }}</p>
        </div>
      </div>
      <button v-if="notifications.length > 0" type="button" class="nl__mark-all" @click="$emit('mark-all-read')">
        Đánh dấu đã đọc
      </button>
    </div>

    <!-- Loading -->
    <div v-if="loading" class="nl__loading">
      <div v-for="i in 3" :key="i" class="nl__skel-item" :style="{ animationDelay: `${i * 0.08}s` }">
        <div class="nl__skel nl__skel--dot" />
        <div class="nl__skel-content">
          <div class="nl__skel nl__skel--title" />
          <div class="nl__skel nl__skel--desc" />
        </div>
      </div>
    </div>

    <!-- Empty -->
    <div v-else-if="!notifications.length" class="nl__empty">
      <LucideIcon name="notifications_off" />
      <p>Không có thông báo nào</p>
    </div>

    <!-- Notifications list -->
    <div v-else class="nl__list">
      <div
        v-for="(item, index) in displayNotifications"
        :key="item.id || index"
        class="nl__item"
        :class="{
          'nl__item--unread': !item.read,
          'nl__item--dismissible': dismissible
        }"
        @click="$emit('item-click', item)"
      >
        <!-- Type icon -->
        <div class="nl__item-icon" :class="typeClass(item.type)">
          <LucideIcon :name="typeIcon(item.type)" />
        </div>

        <!-- Content -->
        <div class="nl__item-content">
          <p class="nl__item-title">{{ item.title }}</p>
          <p class="nl__item-desc">{{ item.description || item.message }}</p>
          <span class="nl__item-time">{{ timeAgo(item.createdAt || item.timestamp) }}</span>
        </div>

        <!-- Unread dot -->
        <div v-if="!item.read" class="nl__unread-dot" />

        <!-- Dismiss -->
        <button
          v-if="dismissible"
          type="button"
          class="nl__dismiss"
          @click.stop="$emit('dismiss', item)"
        >
          <LucideIcon name="close" />
        </button>
      </div>

      <!-- View more -->
      <button
        v-if="notifications.length > displayLimit"
        type="button"
        class="nl__show-more"
        @click="$emit('see-all')"
      >
        <LucideIcon name="expand_more" />
        Xem thêm {{ notifications.length - displayLimit }} thông báo
      </button>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  notifications: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false },
  displayLimit: { type: Number, default: 5 },
  dismissible: { type: Boolean, default: false }
})

defineEmits(['item-click', 'dismiss', 'mark-all-read', 'see-all'])

const displayNotifications = computed(() =>
  props.notifications.slice(0, props.displayLimit)
)

const unreadCount = computed(() =>
  props.notifications.filter(n => !n.read).length
)

const typeIcon = (type) => {
  const t = String(type || '').toLowerCase()
  if (t.includes('exam') || t.includes('result')) return 'quiz'
  if (t.includes('warning') || t.includes('violation')) return 'warning'
  if (t.includes('reminder')) return 'schedule'
  if (t.includes('score') || t.includes('result')) return 'grade'
  if (t.includes('system')) return 'info'
  return 'notifications'
}

const typeClass = (type) => {
  const t = String(type || '').toLowerCase()
  if (t.includes('warning') || t.includes('violation')) return 'nl__item-icon--danger'
  if (t.includes('exam') || t.includes('result')) return 'nl__item-icon--primary'
  if (t.includes('reminder')) return 'nl__item-icon--warning'
  if (t.includes('score')) return 'nl__item-icon--success'
  return 'nl__item-icon--neutral'
}

const timeAgo = (value) => {
  if (!value) return ''
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return ''
  const diff = Date.now() - d.getTime()
  const mins = Math.floor(diff / 1000 / 60)
  const hours = Math.floor(diff / 1000 / 60 / 60)
  const days = Math.floor(diff / 1000 / 60 / 60 / 24)
  if (mins < 1) return 'Vừa xong'
  if (mins < 60) return `${mins} phút trước`
  if (hours < 24) return `${hours} giờ trước`
  if (days < 7) return `${days} ngày trước`
  return d.toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit' })
}
</script>


<style scoped>
/* GPU-accelerated animations */
@keyframes nlFadeIn {
  from { opacity: 0; transform: translateX(-8px) translateZ(0); }
  to { opacity: 1; transform: translateX(0) translateZ(0); }
}

.nl {
  background: var(--ds-surface);
  border: 1.5px solid var(--ds-border);
  border-radius: var(--ds-radius-2xl);
  overflow: hidden;
  /* Optimize paint */
  contain: layout style;
}

.dark .nl {
  border-color: var(--ds-border-strong);
}

/* Header */
.nl__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  padding: 1rem 1.125rem;
  border-bottom: 1px solid var(--ds-border);
  background: var(--ds-gray-50);
}

.dark .nl__header {
  border-bottom-color: var(--ds-border-strong);
  background: var(--ds-gray-800);
}

.nl__header-left {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.nl__icon {
  width: 40px;
  height: 40px;
  border-radius: var(--ds-radius-lg);
  background: rgba(14, 165, 233, 0.08);
  color: var(--ds-info);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}


.nl__title {
  font-family: var(--ds-font-display);
  font-size: 0.875rem;
  font-weight: 800;
  color: var(--ds-text);
  margin: 0;
}

.dark .nl__title { color: #f1f5f9; }

.nl__subtitle {
  font-size: 0.7rem;
  color: var(--ds-text-muted);
  margin: 0.2rem 0 0;
  font-weight: 500;
}

.nl__mark-all {
  font-size: 0.75rem;
  font-weight: 700;
  color: var(--ds-primary);
  background: transparent;
  border: none;
  cursor: pointer;
  transition: color 0.12s ease;
  padding: 0.25rem;
  font-family: inherit;
  white-space: nowrap;
}

.nl__mark-all:hover { color: var(--ds-primary-hover, #4338ca); }

/* Loading */
.nl__loading {
  padding: 0.75rem;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.nl__skel-item {
  display: flex;
  align-items: flex-start;
  gap: 0.75rem;
  animation: nlFadeIn 0.4s cubic-bezier(0.16, 1, 0.3, 1) both;
}

.nl__skel {
  background: var(--ds-gray-200);
  border-radius: var(--ds-radius-md);
  will-change: opacity;
  animation: nlShimmer 1.4s ease-in-out infinite;
}

.dark .nl__skel {
  background: var(--ds-gray-700);
}

/* GPU shimmer: transform-based — reduces paint/composite thrashing */
@keyframes nlShimmer {
  0%   { opacity: 0.6; transform: scaleX(0.3) translateZ(0); }
  50%  { opacity: 1;   transform: scaleX(1)   translateZ(0); }
  100% { opacity: 0.6; transform: scaleX(0.3) translateZ(0); }
}

.nl__skel--dot { width: 36px; height: 36px; border-radius: 50%; flex-shrink: 0; }
.nl__skel-content { flex: 1; display: flex; flex-direction: column; gap: 0.375rem; }
.nl__skel--title { height: 12px; width: 80%; }
.nl__skel--desc { height: 10px; width: 60%; }

/* Empty */
.nl__empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.5rem;
  padding: 2rem 1rem;
  text-align: center;
}


.dark .nl__empty {
  background: var(--ds-gray-800);
}

.nl__empty p {
  font-size: 0.875rem;
  font-weight: 700;
  color: var(--ds-text);
  margin: 0;
}

.dark .nl__empty p { color: #f1f5f9; }

/* List */
.nl__list {
  display: flex;
  flex-direction: column;
}

/* Item */
.nl__item {
  display: flex;
  align-items: flex-start;
  gap: 0.75rem;
  padding: 0.875rem 1.125rem;
  border-bottom: 1px solid var(--ds-border);
  cursor: pointer;
  transition: background 0.1s ease;
  position: relative;
}

.dark .nl__item { border-bottom-color: var(--ds-border-strong); }
.nl__item:last-child { border-bottom: none; }
.nl__item:hover { background: var(--ds-gray-50); }
.dark .nl__item:hover { background: var(--ds-gray-800); }

.nl__item--unread {
  background: rgba(79, 70, 229, 0.03);
}

.dark .nl__item--unread { background: rgba(79, 70, 229, 0.05); }

/* Type icon */
.nl__item-icon {
  width: 36px;
  height: 36px;
  border-radius: var(--ds-radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}


.nl__item-icon--primary { background: var(--ds-primary-soft); color: var(--ds-primary); }
.nl__item-icon--success { background: var(--ds-success-soft); color: var(--ds-success); }
.nl__item-icon--warning { background: rgba(234, 179, 8, 0.1); color: var(--ds-warning); }
.nl__item-icon--danger { background: var(--ds-danger-soft); color: var(--ds-danger); }
.nl__item-icon--neutral { background: var(--ds-gray-100); color: var(--ds-gray-500); }

.dark .nl__item-icon--neutral { background: var(--ds-gray-700); }

/* Content */
.nl__item-content {
  flex: 1;
  min-width: 0;
}

.nl__item-title {
  font-size: 0.875rem;
  font-weight: 700;
  color: var(--ds-text);
  margin: 0 0 0.2rem;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.dark .nl__item-title { color: #f1f5f9; }

.nl__item-desc {
  font-size: 0.75rem;
  color: var(--ds-text-muted);
  margin: 0 0 0.3rem;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  line-height: 1.4;
}

.nl__item-time {
  font-size: 0.65rem;
  color: var(--ds-text-muted);
  font-weight: 600;
}

/* Unread dot */
.nl__unread-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--ds-primary);
  flex-shrink: 0;
  margin-top: 0.25rem;
}

/* Dismiss */
.nl__dismiss {
  width: 1.5rem;
  height: 1.5rem;
  border-radius: 50%;
  border: none;
  background: var(--ds-gray-100);
  color: var(--ds-text-muted);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: color 0.12s ease, background-color 0.12s ease, border-color 0.12s ease, box-shadow 0.12s ease, transform 0.12s ease;
  flex-shrink: 0;
}

.dark .nl__dismiss { background: var(--ds-gray-700); }
.nl__dismiss:hover { background: var(--ds-gray-200); color: var(--ds-text); }
.dark .nl__dismiss:hover { background: var(--ds-gray-600); }

/* Show more */
.nl__show-more {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.375rem;
  width: 100%;
  padding: 0.75rem;
  font-size: 0.8rem;
  font-weight: 700;
  color: var(--ds-primary);
  background: var(--ds-gray-50);
  border: none;
  border-top: 1px solid var(--ds-border);
  cursor: pointer;
  transition: color 0.12s ease, background-color 0.12s ease, border-color 0.12s ease, box-shadow 0.12s ease, transform 0.12s ease;
  font-family: inherit;
}

.dark .nl__show-more {
  background: var(--ds-gray-800);
  border-top-color: var(--ds-border-strong);
}

.nl__show-more:hover { background: var(--ds-primary-soft); }
</style>
