<template>
  <div class="af">
    <!-- Header -->
    <div class="af__header">
      <div class="af__header-left">
        <div class="af__header-icon-wrap" :class="hasCritical ? 'af__header-icon-wrap--danger' : (alertsCount > 0 ? 'af__header-icon-wrap--warning' : 'af__header-icon-wrap--info')">
          <LucideIcon :name="alertsCount > 0 ? 'warning' : 'check_circle'" />
        </div>
        <div class="af__title-row">
          <span class="af__title">Cảnh báo</span>
          <span v-if="alertsCount > 0" class="af__badge af__badge--warn">{{ alertsCount }}</span>
          <span v-if="eventsCount > 0" class="af__badge">{{ eventsCount }} sự kiện</span>
        </div>
      </div>
      <div class="af__header-right">
        <button type="button" class="af__icon-btn" title="Đánh dấu đã đọc" @click="$emit('mark-all-read')">
          <LucideIcon name="done_all" />
        </button>
        <button type="button" class="af__icon-btn" @click="collapsed = !collapsed">
          <LucideIcon :name="collapsed ? 'expand_more' : 'expand_less'" />
        </button>
      </div>
    </div>

    <!-- Body -->
    <div v-show="!collapsed" class="af__body">
      <!-- List -->
      <div class="af__list" ref="listEl">
        <div v-if="loading" class="af__loading">
          <div v-for="i in 4" :key="i" class="af__skeleton-row">
            <div class="af__skeleton af__skeleton--dot" />
            <div class="af__skeleton-content">
              <div class="af__skeleton af__skeleton--line" style="width: 65%" />
              <div class="af__skeleton af__skeleton--line" style="width: 40%" />
            </div>
          </div>
        </div>

        <div v-else-if="mergedItems.length === 0" class="af__empty">
          <LucideIcon name="check_circle" />
          <p class="af__empty-title">Không có cảnh báo</p>
          <p class="af__empty-desc">Phòng thi đang ổn định</p>
        </div>

        <TransitionGroup v-else name="af-item" tag="div">
          <div
            v-for="item in mergedItems"
            :key="item.id"
            class="af__item"
            :class="[
              item.type === 'alert' ? `af__item--${severityVariant(item.severity || item.riskBand)}` : `af__item--${eventVariant(item)}`,
              { 'af__item--flash': item.type === 'alert' && isNewAlert(item) }
            ]"
            @click="$emit('alert-click', item)"
          >
            <div class="af__item-icon" :class="item.type === 'alert' ? `af__item-icon--${severityVariant(item.severity || item.riskBand)}` : `af__item-icon--${eventVariant(item)}`">
              <LucideIcon :name="item.type === 'alert' ? alertIcon(item) : eventIcon(item)" />
            </div>
            <div class="af__item-body">
              <div class="af__item-title-row">
                <p class="af__item-title">
                  <strong v-if="item.studentName || item.userName">{{ item.studentName || item.userName }}</strong>
                  {{ item.type === 'alert' ? alertText(item) : eventText(item) }}
                </p>
                <span v-if="item.type === 'alert'" class="af__item-badge" :class="`af__item-badge--${severityVariant(item.severity || item.riskBand)}`">
                  {{ severityLabel(item.severity || item.riskBand) }}
                </span>
              </div>
              <p v-if="item.examTitle || item.room || item.studentEmail" class="af__item-sub">
                {{ item.examTitle || item.room || item.studentEmail }}
              </p>
            </div>
            <div class="af__item-meta">
              <span class="af__item-time">{{ relativeTime(item.timestamp || item.createdAt || item.time) }}</span>
              <div v-if="item.type === 'alert'" class="af__item-actions">
                <button type="button" class="af__item-action" title="Bỏ qua" @click.stop="$emit('dismiss-alert', item.id)">
                  <LucideIcon name="close" />
                </button>
              </div>
            </div>
            <span v-if="item.type === 'alert' && !item.read" class="af__unread-dot" />
          </div>
        </TransitionGroup>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'

const props = defineProps({
  alerts: { type: Array, default: () => [] },
  events: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false },
  totalAlertCount: { type: Number, default: 0 },
  maxEvents: { type: Number, default: 30 }
})

const emit = defineEmits(['alert-click', 'dismiss-alert', 'mark-all-read', 'load-more', 'clear-events'])

const collapsed = ref(false)
const listEl = ref(null)

const alertsCount = computed(() => props.alerts.length)
const eventsCount = computed(() => Math.min(props.events.length, props.maxEvents))

const hasCritical = computed(() => props.alerts.some(a => (a.severity || a.riskBand) === 'CRITICAL'))

// Merge alerts and events into one chronological list
const mergedItems = computed(() => {
  const alertItems = props.alerts.map(a => ({ ...a, type: 'alert' }))
  const eventItems = props.events.slice(0, props.maxEvents).map(e => ({ ...e, type: 'event' }))
  return [...alertItems, ...eventItems].sort((a, b) => {
    const ta = new Date(a.timestamp || a.createdAt || a.time || 0).getTime()
    const tb = new Date(b.timestamp || b.createdAt || b.time || 0).getTime()
    return tb - ta
  })
})

// New alert flash
const newAlertIds = ref(new Set())
const prevAlertCount = ref(0)

watch(() => props.alerts.length, (newLen) => {
  if (newLen > prevAlertCount.value && props.alerts.length > 0) {
    const newest = props.alerts[0]
    if (newest?.id) {
      newAlertIds.value.add(newest.id)
      setTimeout(() => { newAlertIds.value.delete(newest.id) }, 3000)
    }
  }
  prevAlertCount.value = newLen
})

const isNewAlert = (alert) => newAlertIds.value.has(alert.id)

const severityVariant = (sev) => {
  const map = { CRITICAL: 'danger', HIGH_RISK: 'warning', SUSPICIOUS: 'caution', CLEAN: 'success' }
  return map[sev] || 'default'
}

const severityLabel = (sev) => {
  const map = { CRITICAL: 'Nghiêm trọng', HIGH_RISK: 'Nguy cơ cao', SUSPICIOUS: 'Đáng ngờ', CLEAN: 'Sạch', '': '' }
  return map[sev] || sev || ''
}

const alertText = (a) => {
  if (a.eventType) return a.eventType
  if (a.message) return a.message
  return 'Cảnh báo'
}

const alertIcon = (a) => {
  const map = { CRITICAL: 'error', HIGH_RISK: 'warning', SUSPICIOUS: 'help', CLEAN: 'check_circle' }
  return map[a.severity || a.riskBand] || 'warning'
}

const eventVariant = (e) => {
  const type = String(e.eventType || '').toUpperCase()
  if (type.includes('WARNING') || type.includes('VIOLATION')) return 'warning'
  if (type.includes('PAUSE') || type.includes('STOP') || type.includes('INVALIDATE')) return 'danger'
  if (type.includes('SUBMIT') || type.includes('COMPLETE')) return 'success'
  return 'info'
}

const eventIcon = (e) => {
  const type = String(e.eventType || '').toUpperCase()
  if (type.includes('WARNING')) return 'warning'
  if (type.includes('PAUSE')) return 'pause_circle'
  if (type.includes('SUBMIT') || type.includes('COMPLETE')) return 'check_circle'
  if (type.includes('START')) return 'play_circle'
  return 'bolt'
}

const eventText = (e) => {
  const type = String(e.eventType || '').toLowerCase()
  if (type.includes('warning')) return 'nhận cảnh báo'
  if (type.includes('pause')) return 'bị tạm dừng'
  if (type.includes('stop') || type.includes('invalidate')) return 'bị đình chỉ'
  if (type.includes('submit')) return 'đã nộp bài'
  if (type.includes('start')) return 'bắt đầu thi'
  return e.eventType || 'sự kiện'
}

const relativeTime = (ts) => {
  if (!ts) return ''
  const diff = new Date() - new Date(ts)
  const secs = Math.floor(diff / 1000)
  if (secs < 5) return 'Vừa'
  if (secs < 60) return `${secs}s`
  const mins = Math.floor(secs / 60)
  if (mins < 60) return `${mins}m`
  const hours = Math.floor(mins / 60)
  if (hours < 24) return `${hours}h`
  return `${Math.floor(hours / 24)}d`
}
</script>

<style scoped>
.af {
  background: var(--ds-surface);
  border: 1.5px solid var(--ds-border);
  border-radius: var(--ds-radius-2xl);
  overflow: hidden;
}
.dark .af { border-color: var(--ds-border-strong); }

.af__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.75rem 1rem;
  border-bottom: 1px solid var(--ds-border);
  gap: 0.5rem;
}
.dark .af__header { border-bottom-color: var(--ds-border-strong); }

.af__header-left {
  display: flex;
  align-items: center;
  gap: 0.625rem;
}

.af__header-icon-wrap {
  width: 32px;
  height: 32px;
  border-radius: var(--ds-radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.af__header-icon-wrap--warning { background: rgba(245,158,11,0.1); color: var(--ds-warning); }
.af__header-icon-wrap--danger { background: rgba(220,38,38,0.1); color: var(--ds-danger); }
.af__header-icon-wrap--info { background: var(--ds-primary-soft); color: var(--ds-primary); }

.af__title-row {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.af__title {
  font-size: 0.8rem;
  font-weight: 800;
  color: var(--ds-text);
}

.af__badge {
  padding: 0.1rem 0.5rem;
  border-radius: var(--ds-radius-full);
  background: var(--ds-gray-100);
  color: var(--ds-text-muted);
  font-size: 0.6rem;
  font-weight: 700;
}
.af__badge--warn {
  background: rgba(245,158,11,0.1);
  color: var(--ds-warning);
}
.dark .af__badge { background: var(--ds-gray-700); color: var(--ds-gray-400); }

.af__header-right {
  display: flex;
  align-items: center;
  gap: 0.25rem;
}

.af__icon-btn {
  width: 28px;
  height: 28px;
  border-radius: var(--ds-radius-md);
  border: none;
  background: transparent;
  color: var(--ds-text-muted);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: background 0.15s ease, color 0.15s ease;
}
.af__icon-btn:hover { background: var(--ds-gray-100); color: var(--ds-text); }
.dark .af__icon-btn:hover { background: var(--ds-gray-700); }

.af__body { display: flex; flex-direction: column; }

.af__list {
  max-height: 480px;
  overflow-y: auto;
}

.af__loading { padding: 0.75rem; display: flex; flex-direction: column; gap: 0.75rem; }

.af__skeleton-row {
  display: flex;
  align-items: flex-start;
  gap: 0.625rem;
  animation: afFadeIn 0.4s ease both;
}

.af__skeleton {
  background: var(--ds-gray-200);
  border-radius: var(--ds-radius-md);
}
.dark .af__skeleton { background: var(--ds-gray-700); }

@keyframes afFadeIn { from { opacity: 0; } to { opacity: 1; } }

.af__skeleton--dot { width: 28px; height: 28px; border-radius: 50%; flex-shrink: 0; }
.af__skeleton-content { flex: 1; display: flex; flex-direction: column; gap: 0.375rem; }
.af__skeleton--line { height: 10px; }

.af__empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.375rem;
  padding: 1.5rem 1rem;
  text-align: center;
  color: var(--ds-text-muted);
}
.af__empty-title { font-size: 0.8rem; font-weight: 700; color: var(--ds-text); margin: 0; }
.af__empty-desc { font-size: 0.7rem; margin: 0; }

.af__item {
  display: flex;
  align-items: flex-start;
  gap: 0.625rem;
  padding: 0.625rem 0.875rem;
  border-bottom: 1px solid var(--ds-border);
  cursor: pointer;
  transition: background 0.1s ease;
  position: relative;
}
.dark .af__item { border-bottom-color: var(--ds-border-strong); }
.af__item:last-child { border-bottom: none; }
.af__item:hover { background: var(--ds-gray-50); }
.dark .af__item:hover { background: var(--ds-gray-800); }

.af__item--danger { background: rgba(220,38,38,0.02); }
.af__item--warning { background: rgba(245,158,11,0.02); }
.dark .af__item--danger { background: rgba(220,38,38,0.04); }
.dark .af__item--warning { background: rgba(245,158,11,0.04); }

.af__item--flash { animation: afFlash 0.5s ease; }

@keyframes afFlash {
  0% { background: rgba(245,158,11,0.15); }
  100% { background: transparent; }
}

.af__item-icon {
  width: 28px;
  height: 28px;
  border-radius: var(--ds-radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  margin-top: 0.125rem;
}
.af__item-icon--danger { background: rgba(220,38,38,0.1); color: var(--ds-danger); }
.af__item-icon--warning { background: rgba(245,158,11,0.1); color: var(--ds-warning); }
.af__item-icon--caution { background: rgba(234,179,8,0.1); color: #ca8a04; }
.af__item-icon--success { background: rgba(22,163,74,0.1); color: var(--ds-success); }
.af__item-icon--info { background: var(--ds-primary-soft); color: var(--ds-primary); }

.af__item-body { flex: 1; min-width: 0; }

.af__item-title-row {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  flex-wrap: wrap;
}

.af__item-title {
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--ds-text);
  margin: 0;
  line-height: 1.4;
}
.af__item-title strong { font-weight: 700; }

.af__item-badge {
  padding: 0.1rem 0.375rem;
  border-radius: var(--ds-radius-full);
  font-size: 0.58rem;
  font-weight: 700;
  flex-shrink: 0;
}
.af__item-badge--danger { background: rgba(220,38,38,0.1); color: var(--ds-danger); }
.af__item-badge--warning { background: rgba(245,158,11,0.1); color: var(--ds-warning); }
.af__item-badge--caution { background: rgba(234,179,8,0.1); color: #ca8a04; }

.af__item-sub {
  font-size: 0.65rem;
  color: var(--ds-text-muted);
  margin: 0.2rem 0 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.af__item-meta {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 0.25rem;
  flex-shrink: 0;
}

.af__item-time {
  font-size: 0.6rem;
  color: var(--ds-text-muted);
  white-space: nowrap;
}

.af__item-actions {
  display: flex;
  gap: 0.125rem;
}

.af__item-action {
  width: 22px;
  height: 22px;
  border-radius: var(--ds-radius-sm);
  border: none;
  background: transparent;
  color: var(--ds-text-muted);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: background 0.1s ease, color 0.1s ease;
}
.af__item-action:hover { background: var(--ds-gray-100); color: var(--ds-text); }
.dark .af__item-action:hover { background: var(--ds-gray-700); }

.af__unread-dot {
  position: absolute;
  top: 0.625rem;
  right: 0.5rem;
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--ds-primary);
}

/* TransitionGroup */
.af-item-enter-active { transition: opacity 0.3s ease, transform 0.3s ease; }
.af-item-leave-active { transition: opacity 0.2s ease; }
.af-item-enter-from { opacity: 0; transform: translateY(-8px); }
.af-item-leave-to { opacity: 0; }
</style>
