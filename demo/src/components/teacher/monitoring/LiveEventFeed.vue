<template>
  <div class="lef">
    <!-- Header -->
    <div class="lef__header">
      <div class="lef__header-left">
        <div class="lef__live-badge" :class="connected ? 'lef__live-badge--live' : 'lef__live-badge--off'">
          <span class="lef__live-dot" />
          LIVE
        </div>
        <h4 class="lef__title">Sự kiện gần đây</h4>
        <span class="lef__count-badge">{{ displayEvents.length }}</span>
      </div>

      <div class="lef__header-right">
        <button
          v-if="events.length > 0"
          type="button"
          class="lef__icon-btn"
          title="Xóa tất cả sự kiện"
          @click="$emit('clear')"
        >
          <LucideIcon name="delete_sweep" />
        </button>

        <button type="button" class="lef__icon-btn" @click="collapsed = !collapsed">
          <LucideIcon :name="collapsed ? 'expand_more' : 'expand_less'" />
        </button>
      </div>
    </div>

    <!-- Events list -->
    <div
      v-show="!collapsed"
      class="lef__list"
      ref="listEl"
      @mouseenter="pauseScroll = true"
      @mouseleave="pauseScroll = false"
    >
      <!-- Empty -->
      <div v-if="displayEvents.length === 0 && !loading" class="lef__empty">
        <LucideIcon name="history" />
        <p>Chưa có sự kiện nào</p>
      </div>

      <!-- Loading skeleton -->
      <div v-else-if="loading" class="lef__loading">
        <div v-for="i in 6" :key="i" class="lef__skeleton-row">
          <div class="lef__skeleton-dot" />
          <div class="lef__skeleton-content">
            <div class="lef__skeleton-line" />
          </div>
          <div class="lef__skeleton-time" />
        </div>
      </div>

      <!-- Events -->
      <TransitionGroup v-else name="lef-event" tag="div">
        <div
          v-for="(event, i) in displayEvents"
          :key="event.id || i"
          class="lef__event-item"
          :class="getEventItemClass(event)"
        >
          <!-- Type badge -->
          <span class="lef__type-badge" :class="getTypeBadgeClass(event)">
            {{ eventTypeShort(event) }}
          </span>

          <div class="lef__event-icon" :class="getIconClass(event)">
            <LucideIcon :name="eventIcon(event)" />
          </div>

          <div class="lef__event-content">
            <p class="lef__event-text">
              <strong v-if="event.studentName">{{ event.studentName }}</strong>
              {{ eventText(event) }}
            </p>
            <p class="lef__event-room">{{ event.examTitle || event.room || '' }}</p>
          </div>

          <span class="lef__event-time" :title="absoluteTime(event.timestamp || event.time)">
            {{ relativeTime(event.timestamp || event.time) }}
          </span>
        </div>
      </TransitionGroup>
    </div>

    <!-- Footer: auto-scroll toggle -->
    <div v-if="displayEvents.length > 0" class="lef__footer">
      <label class="lef__auto-scroll">
        <input v-model="autoScroll" type="checkbox" class="lef__checkbox" />
        <LucideIcon :name="autoScroll ? 'check_box' : 'check_box_outline_blank'" />
        Tự cuộn
      </label>

      <button
        type="button"
        class="lef__footer-clear"
        title="Xóa tất cả sự kiện"
        @click="$emit('clear')"
      >
        <LucideIcon name="delete_sweep" />
        Xóa
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, nextTick } from 'vue'

const props = defineProps({
  events: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false },
  maxEvents: { type: Number, default: 20 },
  connected: { type: Boolean, default: false }
})

defineEmits(['clear'])

const listEl = ref(null)
const collapsed = ref(false)
const autoScroll = ref(true)
const pauseScroll = ref(false)

const displayEvents = computed(() => props.events.slice(0, props.maxEvents))

const now = () => new Date()

const relativeTime = (ts) => {
  if (!ts) return ''
  const diff = now() - new Date(ts)
  const secs = Math.floor(diff / 1000)
  if (secs < 5) return 'Vừa'
  if (secs < 60) return `${secs}s`
  const mins = Math.floor(secs / 60)
  if (mins < 60) return `${mins}m`
  const hours = Math.floor(mins / 60)
  return `${hours}h`
}

const absoluteTime = (ts) => {
  if (!ts) return ''
  try {
    return new Date(ts).toLocaleString('vi-VN', {
      day: '2-digit', month: '2-digit', hour: '2-digit', minute: '2-digit', second: '2-digit'
    })
  } catch { return '' }
}

const eventColor = (event) => {
  const type = event.type || event.eventType || event.action || ''
  if (type.includes('ENTER') || type.includes('JOIN') || type.includes('START') || type.includes('RESUME')) return 'success'
  if (type.includes('EXIT') || type.includes('LEAVE') || type.includes('DISCONNECT') || type.includes('NETWORK')) return 'danger'
  if (type.includes('TAB') || type.includes('SWITCH') || type.includes('DEVTOOLS') || type.includes('COPY')) return 'warning'
  if (type.includes('WARN') || type.includes('ALERT') || type.includes('SUSPICIOUS')) return 'caution'
  if (type.includes('SUBMIT') || type.includes('COMPLETE') || type.includes('FINISH')) return 'info'
  if (type.includes('PAUSE') || type.includes('STOP')) return 'muted'
  if (type.includes('TEACHER') || type.includes('ADMIN')) return 'primary'
  return 'default'
}

const eventIcon = (event) => {
  const type = event.type || event.eventType || event.action || ''
  if (type.includes('ENTER') || type.includes('JOIN') || type.includes('START')) return 'login'
  if (type.includes('EXIT') || type.includes('LEAVE')) return 'logout'
  if (type.includes('TAB') || type.includes('SWITCH')) return 'tab'
  if (type.includes('DEVTOOLS') || type.includes('CONSOLE')) return 'code'
  if (type.includes('COPY') || type.includes('PASTE')) return 'content_copy'
  if (type.includes('DISCONNECT') || type.includes('NETWORK') || type.includes('WIFI')) return 'wifi_off'
  if (type.includes('WARN') || type.includes('ALERT')) return 'warning'
  if (type.includes('SUSPICIOUS')) return 'report'
  if (type.includes('SUBMIT') || type.includes('COMPLETE') || type.includes('FINISH')) return 'check_circle'
  if (type.includes('PAUSE') || type.includes('STOP')) return 'pause'
  if (type.includes('RESUME')) return 'play_arrow'
  if (type.includes('CAMERA')) return 'videocam_off'
  if (type.includes('MIC')) return 'mic_off'
  if (type.includes('TEACHER')) return 'person'
  return 'event'
}

const eventText = (event) => {
  const type = (event.type || event.eventType || event.action || event.message || '').toUpperCase()
  if (type.includes('ENTER') || type.includes('JOIN')) return ' vào phòng thi'
  if (type.includes('EXIT') || type.includes('LEAVE')) return ' rời phòng thi'
  if (type.includes('START') || type.includes('BEGIN')) return ' bắt đầu làm bài'
  if (type.includes('TAB') || type.includes('SWITCH')) return ' chuyển tab'
  if (type.includes('DEVTOOLS')) return ' mở DevTools'
  if (type.includes('COPY')) return ' copy nội dung'
  if (type.includes('PASTE')) return ' paste nội dung'
  if (type.includes('DISCONNECT') || type.includes('NETWORK')) return ' mất kết nối'
  if (type.includes('WARN')) return ' được cảnh báo'
  if (type.includes('ALERT')) return ' cảnh báo được gửi'
  if (type.includes('SUSPICIOUS')) return ' hành vi đáng nghi'
  if (type.includes('SUBMIT')) return ' nộp bài'
  if (type.includes('COMPLETE') || type.includes('FINISH')) return ' hoàn thành bài thi'
  if (type.includes('PAUSE')) return ' tạm dừng'
  if (type.includes('RESUME')) return ' tiếp tục làm bài'
  if (type.includes('CAMERA')) return ' tắt camera'
  if (type.includes('MIC')) return ' tắt microphone'
  if (type.includes('REFRESH') || type.includes('SYNC')) return ' dữ liệu được cập nhật'
  if (type.includes('TEACHER') || type.includes('ADMIN')) return ' tham gia giám sát'
  // Fallback to message if available
  if (event.message) return ': ' + event.message
  return ''
}

const eventTypeShort = (event) => {
  const type = event.type || event.eventType || event.action || ''
  if (type.includes('ENTER') || type.includes('JOIN') || type.includes('START')) return 'JOIN'
  if (type.includes('EXIT') || type.includes('LEAVE') || type.includes('DISCONNECT')) return 'EXIT'
  if (type.includes('TAB') || type.includes('SWITCH') || type.includes('DEVTOOLS')) return 'SWITCH'
  if (type.includes('WARN') || type.includes('ALERT') || type.includes('SUSPICIOUS')) return 'ALERT'
  if (type.includes('SUBMIT') || type.includes('COMPLETE')) return 'DONE'
  if (type.includes('PAUSE')) return 'PAUSE'
  return 'EVT'
}

// Flat class helpers to avoid Tailwind v4 BEM scanning issues
const getEventItemClass = (event) => {
  const color = eventColor(event)
  return `lef__event-item lef__ei--${color}`
}

const getTypeBadgeClass = (event) => {
  const color = eventColor(event)
  return `lef__tb--${color}`
}

const getIconClass = (event) => {
  const color = eventColor(event)
  return `lef__icon--${color}`
}

// Auto-scroll on new events - with debounce
let scrollTimeout = null
watch(() => props.events.length, () => {
  if (autoScroll.value && !pauseScroll.value && listEl.value) {
    // Clear existing timeout
    if (scrollTimeout) clearTimeout(scrollTimeout)
    // Debounce scroll to avoid multiple jumps
    scrollTimeout = setTimeout(() => {
      nextTick(() => {
        if (listEl.value) {
          listEl.value.scrollTop = 0
        }
      })
    }, 100)
  }
})
</script>


<style scoped>
.lef {
  display: flex;
  flex-direction: column;
  background: var(--ds-surface);
  border: 1.5px solid var(--ds-border);
  border-radius: var(--ds-radius-2xl);
  overflow: hidden;
  min-height: 200px;
  width: 100%;
  box-sizing: border-box;
}

.dark .lef {
  border-color: var(--ds-border-strong);
}

/* Header */
.lef__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.5rem;
  padding: 0.75rem 1rem;
  border-bottom: 1px solid var(--ds-border);
  background: linear-gradient(135deg, rgba(2, 132, 199, 0.04) 0%, transparent 100%);
}

.dark .lef__header {
  border-bottom-color: var(--ds-border-strong);
}

.lef__header-left {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.lef__header-right {
  display: flex;
  align-items: center;
  gap: 0.25rem;
}

.lef__live-badge {
  display: flex;
  align-items: center;
  gap: 0.3rem;
  padding: 0.15rem 0.5rem;
  border-radius: var(--ds-radius-full);
  background: rgba(2, 132, 199, 0.08);
  border: 1.5px solid rgba(2, 132, 199, 0.2);
  font-size: 0.6rem;
  font-weight: 800;
  letter-spacing: 0.08em;
  color: var(--ds-info);
  transition: color 0.3s ease, background-color 0.3s ease, border-color 0.3s ease, box-shadow 0.3s ease, transform 0.3s ease;
}

.lef__live-badge--off {
  background: var(--ds-gray-100);
  border-color: var(--ds-border);
  color: var(--ds-text-muted);
}

.dark .lef__live-badge--off {
  background: var(--ds-gray-800);
  border-color: var(--ds-border-strong);
}

.lef__live-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--ds-info);
  animation: lefPulse 1.5s ease-in-out infinite;
  transform: translateZ(0);
}

.lef__live-badge--off .lef__live-dot {
  background: var(--ds-gray-400);
  animation: none;
}

.dark .lef__live-badge--off .lef__live-dot { background: var(--ds-gray-600); }

@keyframes lefPulse {
  0%, 100% { box-shadow: 0 0 0 0 rgba(2, 132, 199, 0.4); }
  50% { box-shadow: 0 0 0 5px rgba(2, 132, 199, 0); }
}

.lef__title {
  font-size: 0.8rem;
  font-weight: 700;
  color: var(--ds-text);
  margin: 0;
}

.dark .lef__title { color: #f1f5f9; }

.lef__count-badge {
  background: var(--ds-gray-100);
  padding: 0.05rem 0.4rem;
  border-radius: var(--ds-radius-full);
  font-size: 0.65rem;
  font-weight: 800;
  color: var(--ds-text-muted);
}

.dark .lef__count-badge { background: var(--ds-gray-700); }

.lef__icon-btn {
  width: 1.75rem;
  height: 1.75rem;
  border: none;
  border-radius: var(--ds-radius-md);
  background: transparent;
  color: var(--ds-text-muted);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: color 0.12s ease, background-color 0.12s ease, border-color 0.12s ease, box-shadow 0.12s ease, transform 0.12s ease;
}

.dark .lef__icon-btn { color: #94a3b8; }
.lef__icon-btn:hover { background: var(--ds-gray-100); color: var(--ds-danger); }
.dark .lef__icon-btn:hover { background: var(--ds-gray-700); }

/* List */
.lef__list {
  max-height: 280px;
  overflow-x: hidden;
  overflow-y: auto;
  scrollbar-width: thin;
  scrollbar-color: var(--ds-gray-200) transparent;
}

.lef__list::-webkit-scrollbar { width: 3px; }
.lef__list::-webkit-scrollbar-track { background: transparent; }
.lef__list::-webkit-scrollbar-thumb { background: var(--ds-gray-200); border-radius: 2px; }
.dark .lef__list::-webkit-scrollbar-thumb { background: var(--ds-gray-600); }

/* Event item */
.lef__event-item {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 0.875rem;
  transition: background 0.1s ease;
  border-left: 3px solid transparent;
  min-height: 48px;
  flex-shrink: 0;
}

.lef__event-item:hover {
  background: var(--ds-gray-50);
}

.dark .lef__event-item:hover { background: var(--ds-gray-800); }

/* Flat color variant classes */
.lef__ei--success { border-left-color: var(--ds-success); }
.lef__ei--danger { border-left-color: var(--ds-danger); }
.lef__ei--warning { border-left-color: #d97706; }
.lef__ei--caution { border-left-color: #eab308; }
.lef__ei--info { border-left-color: var(--ds-info); }
.lef__ei--primary { border-left-color: var(--ds-primary); }
.lef__ei--muted { border-left-color: var(--ds-gray-300); }
.lef__ei--default { border-left-color: var(--ds-gray-200); }
.dark .lef__ei--default { border-left-color: var(--ds-gray-600); }

/* Type badge — flat */
.lef__type-badge {
  font-size: 0.5rem;
  font-weight: 900;
  letter-spacing: 0.05em;
  padding: 0.1rem 0.35rem;
  border-radius: var(--ds-radius-sm);
  text-transform: uppercase;
  white-space: nowrap;
  flex-shrink: 0;
}

.lef__tb--success { background: var(--ds-success-soft); color: var(--ds-success); }
.lef__tb--danger { background: var(--ds-danger-soft); color: var(--ds-danger); }
.lef__tb--warning { background: rgba(234, 179, 8, 0.1); color: #d97706; }
.lef__tb--caution { background: rgba(234, 179, 8, 0.08); color: #ca8a04; }
.lef__tb--info { background: var(--ds-info-soft); color: var(--ds-info); }
.lef__tb--primary { background: var(--ds-primary-soft); color: var(--ds-primary); }
.lef__tb--muted { background: var(--ds-gray-100); color: var(--ds-gray-500); }
.lef__tb--default { background: var(--ds-gray-100); color: var(--ds-gray-500); }
.dark .lef__tb--muted,
.dark .lef__tb--default { background: var(--ds-gray-700); }

/* Icon — flat */
.lef__event-icon {
  width: 24px;
  height: 24px;
  border-radius: var(--ds-radius-sm);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}


.lef__icon--success { background: var(--ds-success-soft); color: var(--ds-success); }
.lef__icon--danger { background: var(--ds-danger-soft); color: var(--ds-danger); }
.lef__icon--warning { background: rgba(234, 179, 8, 0.1); color: #d97706; }
.lef__icon--caution { background: rgba(234, 179, 8, 0.08); color: #eab308; }
.lef__icon--info { background: var(--ds-info-soft); color: var(--ds-info); }
.lef__icon--primary { background: var(--ds-primary-soft); color: var(--ds-primary); }
.lef__icon--muted { background: var(--ds-gray-100); color: var(--ds-gray-500); }
.lef__icon--default { background: var(--ds-gray-100); color: var(--ds-gray-500); }
.dark .lef__icon--muted,
.dark .lef__icon--default { background: var(--ds-gray-700); }

/* Content */
.lef__event-content {
  flex: 1;
  min-width: 0;
}

.lef__event-text {
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--ds-text);
  margin: 0;
  line-height: 1.3;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.dark .lef__event-text { color: #f1f5f9; }

.lef__event-text strong {
  color: var(--ds-primary);
}

.lef__event-room {
  font-size: 0.65rem;
  color: var(--ds-text-muted);
  margin: 0.125rem 0 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* Time */
.lef__event-time {
  font-size: 0.65rem;
  color: var(--ds-text-muted);
  font-weight: 600;
  flex-shrink: 0;
  white-space: nowrap;
}

/* Empty */
.lef__empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.375rem;
  padding: 2rem 1rem;
  color: var(--ds-text-muted);
  text-align: center;
  font-size: 0.75rem;
  font-weight: 500;
}

.lef__empty p { margin: 0; }

/* Loading */
.lef__loading {
  padding: 0.5rem 0;
}

.lef__skeleton-row {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 0.875rem;
}

.lef__skeleton {
  background: linear-gradient(90deg, var(--ds-gray-100) 25%, var(--ds-gray-200) 50%, var(--ds-gray-100) 75%);
  background-size: 200% 100%;
  animation: lefShimmer 1.2s ease-in-out infinite;
  border-radius: var(--ds-radius-sm);
  transform: translateZ(0);
}

.dark .lef__skeleton {
  background: linear-gradient(90deg, var(--ds-gray-800) 25%, var(--ds-gray-700) 50%, var(--ds-gray-800) 75%);
  background-size: 200% 100%;
}

@keyframes lefShimmer {
  0% { background-position: -200% 0; }
  100% { background-position: 200% 0; }
}

.lef__skeleton-dot { width: 24px; height: 24px; border-radius: var(--ds-radius-sm); flex-shrink: 0; }
.lef__skeleton-line { height: 0.75rem; }
.lef__skeleton-time { width: 32px; height: 0.625rem; flex-shrink: 0; }
.lef__skeleton-content { flex: 1; min-width: 0; }

/* Footer */
.lef__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.5rem 0.875rem;
  border-top: 1px solid var(--ds-border);
  background: var(--ds-gray-50);
}

.dark .lef__footer {
  border-top-color: var(--ds-border-strong);
  background: var(--ds-gray-800);
}

.lef__auto-scroll {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  font-size: 0.7rem;
  font-weight: 600;
  color: var(--ds-text-muted);
  cursor: pointer;
  user-select: none;
}

.dark .lef__auto-scroll { color: #94a3b8; }

.lef__checkbox { display: none; }

.lef__checkbox-icon {
  font-size: 1rem;
  color: var(--ds-text-muted);
}

.dark .lef__checkbox-icon { color: #94a3b8; }

.lef__auto-scroll input:checked ~ .lef__checkbox-icon {
  color: var(--ds-primary);
}

.lef__footer-clear {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  font-size: 0.7rem;
  font-weight: 700;
  color: var(--ds-text-muted);
  background: transparent;
  border: none;
  cursor: pointer;
  transition: color 0.12s ease;
  padding: 0.25rem;
  border-radius: var(--ds-radius-sm);
}

.dark .lef__footer-clear { color: #94a3b8; }
.lef__footer-clear:hover { color: var(--ds-danger); background: var(--ds-danger-soft); }
.dark .lef__footer-clear:hover { background: rgba(220, 38, 38, 0.1); }

/* Transition — GPU optimized */
.lef-event-enter-active {
  transition: opacity 0.25s ease, transform 0.25s cubic-bezier(0.34, 1.56, 0.64, 1);
  transform: translateZ(0);
}
.lef-event-enter-from { opacity: 0; transform: translateY(-8px) translateZ(0); }
.lef-event-leave-active {
  transition: opacity 0.15s ease, transform 0.15s ease;
  transform: translateZ(0);
  position: absolute;
}
.lef-event-leave-to { opacity: 0; transform: translateY(-4px) translateZ(0); }

@media (prefers-reduced-motion: reduce) {
  .lef__live-dot { animation: none; }
  .lef__skeleton { animation: none; }
  .lef-event-enter-active, .lef-event-leave-active { transition: none; }
  .lef-event-enter-from { opacity: 1; transform: none; }
  .lef-event-leave-to { opacity: 0; transform: none; }
}
</style>