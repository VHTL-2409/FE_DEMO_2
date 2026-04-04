<template>
  <div class="af" :class="{ 'af--has-critical': hasCritical }">
    <!-- Header with tab switcher -->
    <div class="af__header">
      <div class="af__header-left">
        <div
          class="af__header-icon-wrap"
          :class="activeTab === 'alerts' ? (hasCritical ? 'af__header-icon-wrap--danger' : 'af__header-icon-wrap--warning') : 'af__header-icon-wrap--info'"
        >
          <LucideIcon :name="activeTab === 'alerts' ? 'warning' : 'bolt'" />
        </div>
        <div class="af__tabs">
          <button
            type="button"
            class="af__tab"
            :class="{ 'af__tab--active': activeTab === 'alerts' }"
            @click="activeTab = 'alerts'"
          >
            Cảnh báo
            <span v-if="alertsCount > 0" class="af__tab-badge af__tab-badge--warn">{{ alertsCount }}</span>
          </button>
          <button
            type="button"
            class="af__tab"
            :class="{ 'af__tab--active': activeTab === 'events' }"
            @click="activeTab = 'events'"
          >
            Sự kiện
            <span v-if="eventsCount > 0" class="af__tab-badge">{{ eventsCount }}</span>
          </button>
        </div>
      </div>

      <div class="af__header-right">
        <!-- Sound toggle (alerts tab only) -->
        <button
          v-if="activeTab === 'alerts'"
          type="button"
          class="af__icon-btn"
          :class="{ 'af__icon-btn--active': soundEnabled }"
          :title="soundEnabled ? 'Tắt âm thanh' : 'Bật âm thanh'"
          @click="soundEnabled = !soundEnabled"
        >
          <LucideIcon :name="soundEnabled ? 'volume_up' : 'volume_off'" />
        </button>

        <!-- Mark all read (alerts tab only) -->
        <button
          v-if="activeTab === 'alerts'"
          type="button"
          class="af__icon-btn"
          title="Đánh dấu đã đọc tất cả"
          @click="$emit('mark-all-read')"
        >
          <LucideIcon name="done_all" />
        </button>

        <!-- Clear events (events tab only) -->
        <button
          v-if="activeTab === 'events' && eventsCount > 0"
          type="button"
          class="af__icon-btn"
          title="Xóa tất cả sự kiện"
          @click="$emit('clear-events')"
        >
          <LucideIcon name="delete_sweep" />
        </button>

        <!-- Collapse toggle -->
        <button type="button" class="af__icon-btn" @click="collapsed = !collapsed">
          <LucideIcon :name="collapsed ? 'expand_more' : 'expand_less'" />
        </button>
      </div>
    </div>

    <!-- Content (collapsible) -->
    <div v-show="!collapsed" class="af__body">

      <!-- ── ALERTS TAB ─────────────────────────────────────────── -->
      <template v-if="activeTab === 'alerts'">
        <!-- Severity filter -->
        <div class="af__filter-row">
          <div class="af__filter-select-wrap">
            <LucideIcon name="filter_list" class="af__filter-icon" />
            <select v-model="localSeverity" class="af__filter-select">
              <option v-for="tab in severityTabs" :key="tab.value" :value="tab.value">
                {{ tab.label }} ({{ tab.count }})
              </option>
            </select>
            <LucideIcon name="expand_more" class="af__filter-arrow" />
          </div>
        </div>

        <!-- Scrollable alert list -->
        <div class="af__list" ref="listEl">
          <!-- Loading -->
          <div v-if="loading" class="af__loading">
            <div v-for="i in 5" :key="i" class="af__skeleton-row">
              <div class="af__skeleton af__skeleton--dot" />
              <div class="af__skeleton-content">
                <div class="af__skeleton af__skeleton--line" style="width: 60%" />
                <div class="af__skeleton af__skeleton--line" style="width: 40%" />
              </div>
              <div class="af__skeleton af__skeleton--time" />
            </div>
          </div>

          <!-- Empty -->
          <div v-else-if="displayedAlerts.length === 0" class="af__empty">
            <LucideIcon name="check_circle" />
            <p class="af__empty-title">Không có cảnh báo</p>
            <p class="af__empty-desc">
              {{ localSeverity !== 'ALL' ? 'Thử chọn bộ lọc khác' : 'Phòng thi đang ổn định' }}
            </p>
          </div>

          <!-- Grouped alerts -->
          <div v-else>
            <div
              v-for="group in groupedAlerts"
              :key="group.key"
              class="af__group"
            >
              <div class="af__group-label">
                <LucideIcon :name="group.icon" />
                {{ group.label }}
                <span class="af__group-count">{{ group.alerts.length }}</span>
              </div>

              <TransitionGroup name="af-item" tag="div" class="af__group-items">
                <div
                  v-for="alert in group.alerts"
                  :key="alert.id"
                  class="af__item"
                  :class="[
                    `af__item--${severityVariant(alert.severity || alert.riskBand)}`,
                    {
                      'af__item--unread': !alert.read,
                      'af__item--flash': isNewAlert(alert)
                    }
                  ]"
                  @click="$emit('alert-click', alert)"
                >
                  <div class="af__item-icon" :class="`af__item-icon--${severityVariant(alert.severity || alert.riskBand)}`">
                    <LucideIcon :name="alertIcon(alert)" />
                  </div>

                  <div class="af__item-body">
                    <div class="af__item-title-row">
                      <p class="af__item-title">{{ alertTitle(alert) }}</p>
                      <span class="af__item-badge" :class="`af__item-badge--${severityVariant(alert.severity || alert.riskBand)}`">
                        {{ severityLabel(alert.severity || alert.riskBand) }}
                      </span>
                    </div>
                    <p class="af__item-student">{{ alert.studentName || alert.studentEmail || alert.userName || '—' }}</p>
                  </div>

                  <div class="af__item-meta">
                    <span class="af__item-time">{{ relativeTime(alert.timestamp || alert.createdAt || alert.time) }}</span>
                    <div class="af__item-actions">
                      <button
                        type="button"
                        class="af__item-action"
                        title="Xem chi tiết"
                        @click.stop="$emit('alert-click', alert)"
                      >
                        <LucideIcon name="open_in_new" />
                      </button>
                      <button
                        type="button"
                        class="af__item-action"
                        title="Bỏ qua"
                        @click.stop="$emit('dismiss-alert', alert.id)"
                      >
                        <LucideIcon name="close" />
                      </button>
                    </div>
                  </div>

                  <span v-if="!alert.read" class="af__unread-dot" />
                </div>
              </TransitionGroup>
            </div>
          </div>
        </div>

        <!-- Footer -->
        <div v-if="totalAlertCount > displayedAlerts.length" class="af__footer">
          <button type="button" class="af__show-more" @click="$emit('load-more')">
            Xem thêm {{ totalAlertCount - displayedAlerts.length }} cảnh báo
          </button>
        </div>
      </template>

      <!-- ── EVENTS TAB ─────────────────────────────────────────── -->
      <template v-else>
        <!-- Auto-scroll toggle -->
        <div class="af__events-controls">
          <label class="af__auto-scroll">
            <input v-model="autoScroll" type="checkbox" class="af__checkbox" />
            <LucideIcon :name="autoScroll ? 'check_box' : 'check_box_outline_blank'" />
            Tự cuộn
          </label>
        </div>

        <div
          class="af__list"
          ref="eventsListEl"
          @mouseenter="pauseScroll = true"
          @mouseleave="pauseScroll = false"
        >
          <!-- Empty -->
          <div v-if="displayEvents.length === 0 && !loading" class="af__empty">
            <LucideIcon name="history" />
            <p class="af__empty-title">Chưa có sự kiện nào</p>
            <p class="af__empty-desc">Sự kiện sẽ xuất hiện khi có thay đổi</p>
          </div>

          <!-- Loading skeleton -->
          <div v-else-if="loading" class="af__loading">
            <div v-for="i in 6" :key="i" class="af__skeleton-row">
              <div class="af__skeleton af__skeleton--dot-sm" />
              <div class="af__skeleton-content">
                <div class="af__skeleton af__skeleton--line" />
              </div>
              <div class="af__skeleton af__skeleton--time" />
            </div>
          </div>

          <!-- Events -->
          <TransitionGroup v-else name="af-item" tag="div">
            <div
              v-for="(event, i) in displayEvents"
              :key="event.id || i"
              class="af__item"
              :class="`af__item--${eventVariant(event)}`"
            >
              <span class="af__type-badge" :class="`af__type-badge--${eventVariant(event)}`">
                {{ eventTypeShort(event) }}
              </span>

              <div class="af__item-icon" :class="`af__item-icon--${eventVariant(event)}`">
                <LucideIcon :name="eventIcon(event)" />
              </div>

              <div class="af__item-body">
                <p class="af__item-title">
                  <strong v-if="event.studentName">{{ event.studentName }}</strong>
                  {{ eventText(event) }}
                </p>
                <p class="af__item-student">{{ event.examTitle || event.room || '' }}</p>
              </div>

              <span class="af__item-time" :title="absoluteTime(event.timestamp || event.time)">
                {{ relativeTime(event.timestamp || event.time) }}
              </span>
            </div>
          </TransitionGroup>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, nextTick } from 'vue'

const props = defineProps({
  alerts: { type: Array, default: () => [] },
  events: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false },
  totalAlertCount: { type: Number, default: 0 },
  maxEvents: { type: Number, default: 20 }
})

const emit = defineEmits([
  'alert-click',
  'dismiss-alert',
  'mark-all-read',
  'load-more',
  'clear-events'
])

// ── State ───────────────────────────────────────────────────────────────────────
const listEl = ref(null)
const eventsListEl = ref(null)
const collapsed = ref(false)
const activeTab = ref('alerts') // 'alerts' | 'events'
const localSeverity = ref('ALL')
const soundEnabled = ref(false)
const autoScroll = ref(true)
const pauseScroll = ref(false)

// ── Alert helpers ───────────────────────────────────────────────────────────────
const newAlertIds = ref(new Set())
const prevAlertCount = ref(0)

watch(() => props.alerts.length, (newLen) => {
  if (newLen > prevAlertCount.value && props.alerts.length > 0) {
    const newest = props.alerts[0]
    if (newest?.id) {
      newAlertIds.value.add(newest.id)
      setTimeout(() => { newAlertIds.value.delete(newest.id) }, 3000)
    }
    if (soundEnabled.value) playAlertSound()
  }
  prevAlertCount.value = newLen
})

const isNewAlert = (alert) => newAlertIds.value.has(alert.id)

const playAlertSound = () => {
  try {
    const ctx = new (window.AudioContext || window.webkitAudioContext)()
    const osc = ctx.createOscillator()
    const gain = ctx.createGain()
    osc.connect(gain)
    gain.connect(ctx.destination)
    osc.frequency.value = 880
    gain.gain.value = 0.1
    osc.start()
    setTimeout(() => { osc.stop(); ctx.close() }, 200)
  } catch { /* ignore */ }
}

const alertsCount = computed(() => props.alerts.length)
const eventsCount = computed(() => Math.min(props.events.length, props.maxEvents))

const hasCritical = computed(() =>
  props.alerts.some(a => (a.severity || a.riskBand) === 'CRITICAL')
)

// Severity color map
const severityVariant = (severity) => {
  const map = { CRITICAL: 'danger', HIGH_RISK: 'warning', SUSPICIOUS: 'caution', CLEAN: 'success' }
  return map[severity] || 'default'
}

const severityLabel = (sev) => {
  const map = { CRITICAL: 'Nghiêm trọng', HIGH_RISK: 'Nguy cơ cao', SUSPICIOUS: 'Đáng nghi', CLEAN: 'Sạch sẽ', '': '' }
  return map[sev] || sev || ''
}

// Filtered alerts
const displayedAlerts = computed(() => {
  if (localSeverity.value === 'ALL') return props.alerts
  return props.alerts.filter(a => (a.severity || a.riskBand) === localSeverity.value)
})

const totalAlertCount = computed(() => props.totalAlertCount || props.alerts.length)

// Severity tab counts
const severityTabs = computed(() => [
  { value: 'ALL', label: 'Tất cả', count: alertsCount.value },
  { value: 'CRITICAL', label: 'Nghiêm trọng', count: props.alerts.filter(a => (a.severity || a.riskBand) === 'CRITICAL').length },
  { value: 'HIGH_RISK', label: 'Cao', count: props.alerts.filter(a => (a.severity || a.riskBand) === 'HIGH_RISK').length },
  { value: 'SUSPICIOUS', label: 'Thấp', count: props.alerts.filter(a => (a.severity || a.riskBand) === 'SUSPICIOUS').length }
])

const displayEvents = computed(() => props.events.slice(0, props.maxEvents))

// Grouped alerts by time bucket
const groupedAlerts = computed(() => {
  const now = new Date()
  const groups = {
    now: { key: 'now', label: 'Vừa xong', icon: 'fiber_new', alerts: [] },
    '5m': { key: '5m', label: '5 phút trước', icon: 'schedule', alerts: [] },
    '15m': { key: '15m', label: '15 phút trước', icon: 'schedule', alerts: [] },
    '1h': { key: '1h', label: '1 giờ trước', icon: 'history', alerts: [] },
    older: { key: 'older', label: 'Cũ hơn', icon: 'history_toggle_off', alerts: [] }
  }
  for (const alert of displayedAlerts.value) {
    const ts = new Date(alert.timestamp || alert.createdAt || alert.time)
    const diffMs = now - ts
    const diffMins = diffMs / 60000
    if (diffMins < 5) groups.now.alerts.push(alert)
    else if (diffMins < 15) groups['5m'].alerts.push(alert)
    else if (diffMins < 60) groups['15m'].alerts.push(alert)
    else if (diffMins < 3600) groups['1h'].alerts.push(alert)
    else groups.older.alerts.push(alert)
  }
  return Object.values(groups).filter(g => g.alerts.length > 0)
})

// Time helpers
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
  const days = Math.floor(hours / 24)
  return `${days}d`
}

const absoluteTime = (ts) => {
  if (!ts) return ''
  try {
    return new Date(ts).toLocaleString('vi-VN', {
      day: '2-digit', month: '2-digit', hour: '2-digit', minute: '2-digit', second: '2-digit'
    })
  } catch { return '' }
}

// Alert icon + title
const alertIcon = (alert) => {
  const type = alert.type || alert.eventType || ''
  if (type.includes('TAB') || type.includes('SWITCH')) return 'tab'
  if (type.includes('CAMERA') || type.includes('VIDEO')) return 'videocam_off'
  if (type.includes('MIC') || type.includes('AUDIO')) return 'mic_off'
  if (type.includes('DEVTOOLS') || type.includes('CONSOLE')) return 'code'
  if (type.includes('COPY') || type.includes('PASTE')) return 'content_paste_off'
  if (type.includes('FULLSCREEN') || type.includes('EXIT')) return 'fullscreen_exit'
  if (type.includes('NETWORK') || type.includes('DISCONNECT')) return 'wifi_off'
  if (type.includes('IP') || type.includes('LOCATION')) return 'router'
  if (type.includes('INACTIVE') || type.includes('IDLE')) return 'timer_off'
  if (type.includes('FAST') || type.includes('SPEED')) return 'bolt'
  if (type.includes('MULTI') || type.includes('SCREEN')) return 'desktop_windows'
  if (type.includes('RIGHT_CLICK') || type.includes('CONTEXT')) return 'mouse'
  if (type.includes('PRINT') || type.includes('SCREENSHOT')) return 'screenshot'
  const map = { CRITICAL: 'gpp_bad', HIGH_RISK: 'warning', SUSPICIOUS: 'report', CLEAN: 'check_circle' }
  return map[alert.severity || alert.riskBand] || 'warning'
}

const alertTitle = (alert) => {
  const type = alert.type || alert.eventType || alert.message || ''
  if (type.includes('TAB') || type.includes('SWITCH')) return 'Chuyển tab'
  if (type.includes('CAMERA')) return 'Camera tắt'
  if (type.includes('MIC')) return 'Micro tắt'
  if (type.includes('DEVTOOLS')) return 'Mở DevTools'
  if (type.includes('COPY') || type.includes('PASTE')) return 'Copy/Paste'
  if (type.includes('FULLSCREEN') || type.includes('EXIT')) return 'Thoát toàn màn hình'
  if (type.includes('NETWORK') || type.includes('DISCONNECT')) return 'Mất kết nối'
  if (type.includes('IP') || type.includes('LOCATION')) return 'Thay đổi IP'
  if (type.includes('INACTIVE') || type.includes('IDLE')) return 'Không hoạt động'
  if (type.includes('FAST')) return 'Nộp bài quá nhanh'
  if (type.includes('MULTI')) return 'Đa màn hình'
  if (type.includes('RIGHT_CLICK')) return 'Click chuột phải'
  if (type.includes('PRINT')) return 'Chụp màn hình'
  return type || alert.description || 'Cảnh báo'
}

// Event helpers
const eventVariant = (event) => {
  const type = event.type || event.eventType || event.action || ''
  if (type.includes('ENTER') || type.includes('JOIN') || type.includes('START') || type.includes('RESUME')) return 'success'
  if (type.includes('EXIT') || type.includes('LEAVE') || type.includes('DISCONNECT') || type.includes('NETWORK')) return 'danger'
  if (type.includes('TAB') || type.includes('SWITCH') || type.includes('DEVTOOLS') || type.includes('COPY')) return 'warning'
  if (type.includes('WARN') || type.includes('ALERT') || type.includes('SUSPICIOUS')) return 'caution'
  if (type.includes('SUBMIT') || type.includes('COMPLETE') || type.includes('FINISH')) return 'info'
  if (type.includes('PAUSE') || type.includes('STOP')) return 'default'
  if (type.includes('TEACHER') || type.includes('ADMIN') || type.includes('REFRESH')) return 'primary'
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
  if (type.includes('PAUSE')) return 'pause'
  if (type.includes('RESUME')) return 'play_arrow'
  if (type.includes('CAMERA')) return 'videocam_off'
  if (type.includes('MIC')) return 'mic_off'
  if (type.includes('TEACHER')) return 'person'
  if (type.includes('REFRESH')) return 'sync'
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

// Auto-scroll on new events
let scrollTimeout = null
watch(() => props.events.length, () => {
  if (autoScroll.value && !pauseScroll.value) {
    if (scrollTimeout) clearTimeout(scrollTimeout)
    scrollTimeout = setTimeout(() => {
      nextTick(() => {
        if (eventsListEl.value) eventsListEl.value.scrollTop = 0
      })
    }, 100)
  }
})
</script>

<style scoped>
.af {
  display: flex;
  flex-direction: column;
  background: var(--ds-surface);
  border: 1.5px solid var(--ds-border);
  border-radius: var(--ds-radius-2xl);
  overflow: hidden;
  min-height: 200px;
  transition: border-color 0.3s ease;
  width: 100%;
  box-sizing: border-box;
}

.dark .af { border-color: var(--ds-border-strong); }
.af--has-critical { border-color: rgba(220, 38, 38, 0.3); }

/* Header */
.af__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.5rem;
  padding: 0.75rem 1rem;
  border-bottom: 1px solid var(--ds-border);
  background: linear-gradient(135deg, rgba(220, 38, 38, 0.04) 0%, transparent 100%);
  flex-shrink: 0;
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
  border-radius: var(--ds-radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.af__header-icon-wrap--danger { background: var(--ds-danger-soft); color: var(--ds-danger); }
.af__header-icon-wrap--warning { background: rgba(234, 179, 8, 0.1); color: #d97706; }
.af__header-icon-wrap--info { background: var(--ds-info-soft); color: var(--ds-info); }

/* Tabs */
.af__tabs {
  display: flex;
  gap: 0.125rem;
  background: var(--ds-gray-100);
  border-radius: var(--ds-radius-lg);
  padding: 0.125rem;
  border: 1.5px solid var(--ds-border);
}

.dark .af__tabs { background: var(--ds-gray-800); border-color: var(--ds-border-strong); }

.af__tab {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  padding: 0.3rem 0.75rem;
  border-radius: calc(var(--ds-radius-lg) - 2px);
  border: none;
  background: transparent;
  font-size: 0.75rem;
  font-weight: 700;
  color: var(--ds-text-muted);
  cursor: pointer;
  transition: color 0.15s ease, background-color 0.15s ease, border-color 0.15s ease, box-shadow 0.15s ease, transform 0.15s ease;
  font-family: inherit;
  white-space: nowrap;
}

.af__tab:hover { color: var(--ds-text); }

.af__tab--active {
  background: var(--ds-surface);
  color: var(--ds-text);
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
}

.dark .af__tab--active { background: var(--ds-gray-700); }

.af__tab-badge {
  background: var(--ds-gray-200);
  color: var(--ds-text-muted);
  font-size: 0.6rem;
  font-weight: 800;
  padding: 0.05rem 0.4rem;
  border-radius: var(--ds-radius-full);
  min-width: 16px;
  text-align: center;
}

.dark .af__tab-badge { background: var(--ds-gray-600); }

.af__tab-badge--warn {
  background: var(--ds-warning);
  color: white;
}

/* Header right */
.af__header-right {
  display: flex;
  align-items: center;
  gap: 0.25rem;
}

.af__icon-btn {
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

.dark .af__icon-btn { color: #94a3b8; }
.af__icon-btn:hover { background: var(--ds-gray-100); color: var(--ds-text); }
.dark .af__icon-btn:hover { background: var(--ds-gray-700); }
.af__icon-btn--active {
  color: var(--ds-primary);
  background: var(--ds-primary-soft);
}

/* Body */
.af__body {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

/* Filter row */
.af__filter-row {
  padding: 0.5rem 0.875rem;
  border-bottom: 1px solid var(--ds-border);
  flex-shrink: 0;
}

.dark .af__filter-row { border-bottom-color: var(--ds-border-strong); }

.af__filter-select-wrap {
  position: relative;
  display: flex;
  align-items: center;
}

.af__filter-icon {
  position: absolute;
  left: 0.625rem;
  width: 16px;
  height: 16px;
  color: var(--ds-text-muted);
  pointer-events: none;
  z-index: 1;
}

.af__filter-arrow {
  position: absolute;
  right: 0.625rem;
  width: 16px;
  height: 16px;
  color: var(--ds-text-muted);
  pointer-events: none;
  z-index: 1;
}

.af__filter-select {
  width: 100%;
  padding: 0.5rem 2rem 0.5rem 2.25rem;
  border: 1.5px solid var(--ds-border);
  border-radius: var(--ds-radius-lg);
  background: var(--ds-surface);
  color: var(--ds-text);
  font-size: 0.8rem;
  font-weight: 600;
  cursor: pointer;
  appearance: none;
  outline: none;
  transition: color 0.15s ease, background-color 0.15s ease, border-color 0.15s ease, box-shadow 0.15s ease, transform 0.15s ease;
  font-family: inherit;
}

.dark .af__filter-select {
  background: var(--ds-gray-800);
  border-color: var(--ds-border-strong);
  color: var(--ds-text);
}

.af__filter-select:hover { border-color: var(--ds-primary-border); }
.af__filter-select:focus { border-color: var(--ds-primary); box-shadow: 0 0 0 3px var(--ds-primary-ring); }

/* Events controls */
.af__events-controls {
  padding: 0.5rem 0.875rem;
  border-bottom: 1px solid var(--ds-border);
  flex-shrink: 0;
}

.dark .af__events-controls { border-bottom-color: var(--ds-border-strong); }

.af__auto-scroll {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  font-size: 0.7rem;
  font-weight: 600;
  color: var(--ds-text-muted);
  cursor: pointer;
  user-select: none;
}

.dark .af__auto-scroll { color: #94a3b8; }
.af__checkbox { display: none; }

/* List */
.af__list {
  flex: 1;
  overflow-x: hidden;
  overflow-y: auto;
  scrollbar-width: thin;
  scrollbar-color: var(--ds-gray-300) transparent;
}

.af__list::-webkit-scrollbar { width: 3px; }
.af__list::-webkit-scrollbar-track { background: transparent; }
.af__list::-webkit-scrollbar-thumb { background: var(--ds-gray-300); border-radius: 2px; }
.dark .af__list::-webkit-scrollbar-thumb { background: var(--ds-gray-600); }

/* Group */
.af__group { padding: 0.25rem 0; }

.af__group-label {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  padding: 0.375rem 0.875rem;
  font-size: 0.625rem;
  font-weight: 800;
  color: var(--ds-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.1em;
}

.af__group-count {
  margin-left: auto;
  background: var(--ds-gray-100);
  padding: 0.05rem 0.4rem;
  border-radius: var(--ds-radius-full);
  font-size: 0.6rem;
}

.dark .af__group-count { background: var(--ds-gray-700); }
.af__group-items { display: flex; flex-direction: column; }

/* Item (shared between alerts and events) */
.af__item {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 0.875rem;
  cursor: pointer;
  transition: background 0.1s ease;
  position: relative;
  border-left: 3px solid transparent;
}

.af__item:hover { background: var(--ds-gray-50); }
.dark .af__item:hover { background: var(--ds-gray-800); }
.af__item--unread { background: rgba(79, 70, 229, 0.04); }
.dark .af__item--unread { background: rgba(79, 70, 229, 0.06); }
.af__item--flash { animation: afFlash 3s ease-out forwards; }

@keyframes afFlash {
  0% { background: rgba(220, 38, 38, 0.08); }
  20% { background: transparent; }
  30% { background: rgba(220, 38, 38, 0.05); }
  100% { background: transparent; }
}

/* Severity borders */
.af__item--danger { border-left-color: var(--ds-danger); }
.af__item--warning { border-left-color: #d97706; }
.af__item--caution { border-left-color: #eab308; }
.af__item--success { border-left-color: var(--ds-success); }
.af__item--info { border-left-color: var(--ds-info); }
.af__item--primary { border-left-color: var(--ds-primary); }
.af__item--default { border-left-color: var(--ds-gray-300); }
.dark .af__item--default { border-left-color: var(--ds-gray-600); }

/* Item icon */
.af__item-icon {
  width: 28px;
  height: 28px;
  border-radius: var(--ds-radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.af__item-icon--danger { background: var(--ds-danger-soft); color: var(--ds-danger); }
.af__item-icon--warning { background: rgba(234, 179, 8, 0.1); color: #d97706; }
.af__item-icon--caution { background: rgba(234, 179, 8, 0.08); color: #ca8a04; }
.af__item-icon--success { background: var(--ds-success-soft); color: var(--ds-success); }
.af__item-icon--info { background: var(--ds-info-soft); color: var(--ds-info); }
.af__item-icon--primary { background: var(--ds-primary-soft); color: var(--ds-primary); }
.af__item-icon--default { background: var(--ds-gray-100); color: var(--ds-text-muted); }
.dark .af__item-icon--default { background: var(--ds-gray-700); }

/* Type badge (events tab) */
.af__type-badge {
  font-size: 0.5rem;
  font-weight: 900;
  letter-spacing: 0.05em;
  padding: 0.1rem 0.35rem;
  border-radius: var(--ds-radius-sm);
  text-transform: uppercase;
  white-space: nowrap;
  flex-shrink: 0;
}

.af__type-badge--success { background: var(--ds-success-soft); color: var(--ds-success); }
.af__type-badge--danger { background: var(--ds-danger-soft); color: var(--ds-danger); }
.af__type-badge--warning { background: rgba(234, 179, 8, 0.1); color: #d97706; }
.af__type-badge--caution { background: rgba(234, 179, 8, 0.08); color: #ca8a04; }
.af__type-badge--info { background: var(--ds-info-soft); color: var(--ds-info); }
.af__type-badge--primary { background: var(--ds-primary-soft); color: var(--ds-primary); }
.af__type-badge--default { background: var(--ds-gray-100); color: var(--ds-gray-500); }
.dark .af__type-badge--default { background: var(--ds-gray-700); }

/* Item body */
.af__item-body {
  flex: 1;
  min-width: 0;
}

.af__item-title-row {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.af__item-title {
  font-size: 0.8rem;
  font-weight: 700;
  color: var(--ds-text);
  margin: 0;
  line-height: 1.3;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
  min-width: 0;
}

.dark .af__item-title { color: #f1f5f9; }
.af__item-title strong { color: var(--ds-primary); }

.af__item-badge {
  font-size: 0.55rem;
  font-weight: 800;
  padding: 0.1rem 0.4rem;
  border-radius: var(--ds-radius-full);
  text-transform: uppercase;
  letter-spacing: 0.05em;
  white-space: nowrap;
  flex-shrink: 0;
}

.af__item-badge--danger { background: var(--ds-danger-soft); color: var(--ds-danger); border: 1px solid rgba(220, 38, 38, 0.2); }
.af__item-badge--warning { background: rgba(234, 179, 8, 0.1); color: #d97706; border: 1px solid rgba(234, 179, 8, 0.2); }
.af__item-badge--caution { background: rgba(234, 179, 8, 0.07); color: #ca8a04; border: 1px solid rgba(234, 179, 8, 0.15); }
.af__item-badge--success { background: var(--ds-success-soft); color: var(--ds-success); border: 1px solid rgba(22, 163, 74, 0.2); }

.af__item-student {
  font-size: 0.7rem;
  color: var(--ds-text-muted);
  margin: 0.125rem 0 0;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* Item meta */
.af__item-meta {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 0.25rem;
  flex-shrink: 0;
}

.af__item-time {
  font-size: 0.65rem;
  color: var(--ds-text-muted);
  font-weight: 600;
  white-space: nowrap;
}

.af__item-actions {
  display: flex;
  gap: 0.125rem;
  opacity: 0;
  transition: opacity 0.15s ease;
}

.af__item:hover .af__item-actions { opacity: 1; }

.af__item-action {
  width: 1.5rem;
  height: 1.5rem;
  border: none;
  border-radius: var(--ds-radius-sm);
  background: var(--ds-gray-100);
  color: var(--ds-text-muted);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: color 0.12s ease, background-color 0.12s ease, border-color 0.12s ease, box-shadow 0.12s ease, transform 0.12s ease;
}

.dark .af__item-action { background: var(--ds-gray-700); }
.af__item-action:hover { background: var(--ds-gray-200); color: var(--ds-text); }
.dark .af__item-action:hover { background: var(--ds-gray-600); }

/* Unread dot */
.af__unread-dot {
  position: absolute;
  top: 0.5rem;
  right: 0.75rem;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--ds-danger);
  box-shadow: 0 0 0 2px rgba(220, 38, 38, 0.2);
}

/* Loading */
.af__loading { padding: 0.5rem 0; }

.af__skeleton-row {
  display: flex;
  align-items: center;
  gap: 0.625rem;
  padding: 0.5rem 0.875rem;
}

.af__skeleton {
  background: linear-gradient(90deg, var(--ds-gray-100) 25%, var(--ds-gray-200) 50%, var(--ds-gray-100) 75%);
  background-size: 200% 100%;
  animation: afShimmer 1.5s infinite;
  border-radius: var(--ds-radius-md);
}

.dark .af__skeleton {
  background: linear-gradient(90deg, var(--ds-gray-800) 25%, var(--ds-gray-700) 50%, var(--ds-gray-800) 75%);
  background-size: 200% 100%;
}

@keyframes afShimmer {
  0% { background-position: -200% 0; }
  100% { background-position: 200% 0; }
}

.af__skeleton--dot { width: 28px; height: 28px; border-radius: var(--ds-radius-md); flex-shrink: 0; }
.af__skeleton--dot-sm { width: 24px; height: 24px; border-radius: var(--ds-radius-sm); flex-shrink: 0; }
.af__skeleton--line { height: 0.75rem; margin-bottom: 0.25rem; }
.af__skeleton--time { width: 36px; height: 0.625rem; margin-left: auto; flex-shrink: 0; }
.af__skeleton-content { flex: 1; min-width: 0; }

/* Empty */
.af__empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.5rem;
  padding: 2.5rem 1rem;
  text-align: center;
}

.af__empty :deep(svg) {
  font-size: 2.5rem;
  color: var(--ds-success);
  opacity: 0.4;
}

.af__empty-title {
  font-size: 0.875rem;
  font-weight: 700;
  color: var(--ds-text);
  margin: 0;
}

.dark .af__empty-title { color: #f1f5f9; }
.af__empty-desc { font-size: 0.75rem; color: var(--ds-text-muted); margin: 0; }

/* Footer */
.af__footer {
  padding: 0.5rem 0.875rem;
  border-top: 1px solid var(--ds-border);
  flex-shrink: 0;
}

.dark .af__footer { border-top-color: var(--ds-border-strong); }

.af__show-more {
  width: 100%;
  padding: 0.5rem;
  border: 1.5px dashed var(--ds-border);
  border-radius: var(--ds-radius-lg);
  background: transparent;
  color: var(--ds-text-muted);
  font-size: 0.75rem;
  font-weight: 700;
  cursor: pointer;
  transition: color 0.15s ease, background-color 0.15s ease, border-color 0.15s ease, box-shadow 0.15s ease, transform 0.15s ease;
  font-family: inherit;
}

.dark .af__show-more { border-color: var(--ds-border-strong); }
.af__show-more:hover { border-color: var(--ds-primary); color: var(--ds-primary); background: var(--ds-primary-soft); }
.dark .af__show-more:hover { background: rgba(79, 70, 229, 0.1); }

/* Transition */
.af-item-enter-active { transition: color 0.3s ease, background-color 0.3s ease, border-color 0.3s ease, box-shadow 0.3s ease, transform 0.3s ease; }
.af-item-enter-from { opacity: 0; transform: translateX(-8px); background: rgba(79, 70, 229, 0.04); }
.af-item-leave-active { transition: color 0.2s ease, background-color 0.2s ease, border-color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease; }
.af-item-leave-to { opacity: 0; }
</style>
@media (prefers-reduced-motion: reduce) {
  * {
    transition-duration: 0.01ms !important;
    animation-duration: 0.01ms !important;
  }
}