<template>
  <div class="ap" :class="{ 'ap--has-critical': hasCritical }">
    <!-- Header -->
    <div class="ap__header">
      <div class="ap__header-left">
        <div class="ap__header-icon-wrap" :class="hasCritical ? 'ap__header-icon-wrap--danger' : 'ap__header-icon-wrap--warning'">
          <LucideIcon name="warning" />
        </div>
        <div>
          <h3 class="ap__title">Cảnh báo</h3>
          <span v-if="unfilteredCount > 0" class="ap__count">{{ displayCount }}</span>
        </div>
      </div>

      <div class="ap__header-right">
        <!-- Sound toggle -->
        <button
          type="button"
          class="ap__sound-btn"
          :class="{ 'ap__sound-btn--active': soundEnabled }"
          :title="soundEnabled ? 'Tắt âm thanh' : 'Bật âm thanh'"
          @click="soundEnabled = !soundEnabled"
        >
          <LucideIcon :name="soundEnabled ? 'volume_up' : 'volume_off'" />
        </button>

        <!-- Mark all read -->
        <button
          type="button"
          class="ap__mark-read"
          title="Đánh dấu đã đọc tất cả"
          @click="$emit('mark-all-read')"
        >
          <LucideIcon name="done_all" />
        </button>
      </div>
    </div>

    <!-- Filter select -->
    <div class="ap__filter-row">
      <div class="ap__filter-select-wrap">
        <LucideIcon name="filter_list" class="ap__filter-icon" />
        <select v-model="localSeverity" class="ap__filter-select">
          <option v-for="tab in severityTabs" :key="tab.value" :value="tab.value">
            {{ tab.label }} ({{ tab.count }})
          </option>
        </select>
        <LucideIcon name="expand_more" class="ap__filter-arrow" />
      </div>
      <div class="ap__filter-select-wrap">
        <LucideIcon name="tag" class="ap__filter-icon" />
        <select v-model="localCategory" class="ap__filter-select">
          <option v-for="tab in categoryTabs" :key="tab.value" :value="tab.value">
            {{ tab.label }} ({{ tab.count }})
          </option>
        </select>
        <LucideIcon name="expand_more" class="ap__filter-arrow" />
      </div>
    </div>

    <!-- Scrollable alert list -->
    <div class="ap__list" ref="listEl">
      <!-- Loading -->
      <div v-if="loading" class="ap__loading">
        <div v-for="i in 5" :key="i" class="ap__skeleton-row">
          <div class="ap__skeleton ap__skeleton--dot" />
          <div class="ap__skeleton-content">
            <div class="ap__skeleton ap__skeleton--line" style="width: 60%" />
            <div class="ap__skeleton ap__skeleton--line" style="width: 40%" />
          </div>
          <div class="ap__skeleton ap__skeleton--time" />
        </div>
      </div>

      <!-- Empty -->
      <div v-else-if="displayedAlerts.length === 0" class="ap__empty">
        <LucideIcon name="check_circle" />
        <p class="ap__empty-title">Không có cảnh báo</p>
        <p class="ap__empty-desc">
          {{ localSeverity !== 'ALL' ? 'Thử chọn bộ lọc khác' : 'Phòng thi đang ổn định' }}
        </p>
      </div>

      <!-- Grouped alerts -->
      <div v-else>
        <div
          v-for="group in groupedAlerts"
          :key="group.key"
          class="ap__group"
        >
          <div class="ap__group-label">
            <LucideIcon :name="group.icon" />
            {{ group.label }}
            <span class="ap__group-count">{{ group.alerts.length }}</span>
          </div>

          <TransitionGroup name="ap-alert" tag="div" class="ap__group-items">
            <div
              v-for="alert in group.alerts"
              :key="alert.id"
              class="ap__alert-item"
              :class="[
                `ap__alert-item--${severityColor(alert.severity || alert.riskBand)}`,
                {
                  'ap__alert-item--unread': !alert.read,
                  'ap__alert-item--flash': isNewAlert(alert)
                }
              ]"
              @click="$emit('alert-click', alert)"
            >
              <!-- Severity border-left is 4px -->
              <div class="ap__alert-icon" :class="`ap__alert-icon--${severityColor(alert.severity || alert.riskBand)}`">
                <LucideIcon :name="alertIcon(alert)" />
              </div>

              <!-- Content -->
              <div class="ap__alert-body">
                <div class="ap__alert-title-row">
                  <p class="ap__alert-title">{{ alertTitle(alert) }}</p>
                  <!-- Alert type badge -->
                  <span class="ap__alert-type-badge" :class="`ap__alert-type-badge--${severityColor(alert.severity || alert.riskBand)}`">
                    {{ alertTypeLabel(alert) }}
                  </span>
                  <span v-if="alert.category || alert.warningCategory" class="ap__alert-category-badge">
                    {{ categoryLabel(alert.category || alert.warningCategory) }}
                  </span>
                  <span v-if="alert.riskImpact != null" class="ap__alert-risk-badge">
                    +{{ alert.riskImpact }} risk
                  </span>
                </div>
                <p class="ap__alert-student">{{ alert.studentName || alert.studentEmail || alert.userName || '—' }}</p>
              </div>

              <!-- Time + actions -->
              <div class="ap__alert-meta">
                <span class="ap__alert-time">{{ relativeTime(alert.timestamp || alert.createdAt || alert.time) }}</span>
                <div class="ap__alert-actions">
                  <button
                    type="button"
                    class="ap__alert-action"
                    title="Xem chi tiết"
                    @click.stop="$emit('alert-click', alert)"
                  >
                    <LucideIcon name="open_in_new" />
                  </button>
                  <button
                    type="button"
                    class="ap__alert-action"
                    title="Bỏ qua"
                    @click.stop="$emit('dismiss', alert.id)"
                  >
                    <LucideIcon name="close" />
                  </button>
                </div>
              </div>

              <!-- Unread dot -->
              <span v-if="!alert.read" class="ap__unread-dot" />
            </div>
          </TransitionGroup>
        </div>
      </div>
    </div>

    <!-- Footer -->
    <div v-if="totalCount > displayedAlerts.length" class="ap__footer">
      <button type="button" class="ap__show-more" @click="$emit('load-more')">
        Xem thêm {{ totalCount - displayedAlerts.length }} cảnh báo
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { relativeTime } from '../../utils/dateUtils'

const props = defineProps({
  alerts: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false },
  totalCount: { type: Number, default: 0 }
})

const emit = defineEmits(['alert-click', 'dismiss', 'mark-all-read', 'load-more'])

const listEl = ref(null)
const localSeverity = ref('ALL')
const localCategory = ref('ALL')
const soundEnabled = ref(false)

// Track new alerts for flash animation
const newAlertIds = ref(new Set())
const prevAlertCount = ref(0)

watch(() => props.alerts.length, (newLen) => {
  if (newLen > prevAlertCount.value && props.alerts.length > 0) {
    const newest = props.alerts[0]
    if (newest?.id) {
      newAlertIds.value.add(newest.id)
      setTimeout(() => {
        newAlertIds.value.delete(newest.id)
      }, 3000)
    }
    // Play sound if enabled
    if (soundEnabled.value) {
      try {
        const ctx = new window.AudioContext()
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
  }
  prevAlertCount.value = newLen
})

const isNewAlert = (alert) => newAlertIds.value.has(alert.id)

const unfilteredCount = computed(() => props.alerts.length)

const displayCount = computed(() => {
  const filtered = localSeverity.value === 'ALL'
    ? props.alerts
    : props.alerts.filter(a => (a.severity || a.riskBand) === localSeverity.value)
  const categoryFiltered = localCategory.value === 'ALL'
    ? filtered
    : filtered.filter(a => (a.category || a.warningCategory || a.latestSignalCategory || 'UNCATEGORIZED') === localCategory.value)
  return categoryFiltered.length
})

const displayedAlerts = computed(() => {
  let alerts = props.alerts
  if (localSeverity.value !== 'ALL') {
    alerts = alerts.filter(a => (a.severity || a.riskBand) === localSeverity.value)
  }
  if (localCategory.value !== 'ALL') {
    alerts = alerts.filter(a => (a.category || a.warningCategory || a.latestSignalCategory || 'UNCATEGORIZED') === localCategory.value)
  }
  return alerts
})

const hasCritical = computed(() =>
  props.alerts.some(a => (a.severity || a.riskBand) === 'CRITICAL')
)

// Severity tab counts
const severityTabs = computed(() => [
  {
    value: 'ALL',
    label: 'Tất cả',
    icon: 'list',
    color: 'default',
    count: unfilteredCount.value
  },
  {
    value: 'CRITICAL',
    label: 'Nghiêm trọng',
    icon: 'gpp_bad',
    color: 'danger',
    count: props.alerts.filter(a => (a.severity || a.riskBand) === 'CRITICAL').length
  },
  {
    value: 'HIGH_RISK',
    label: 'Cao',
    icon: 'warning',
    color: 'warning',
    count: props.alerts.filter(a => (a.severity || a.riskBand) === 'HIGH_RISK').length
  },
  {
    value: 'SUSPICIOUS',
    label: 'Thấp',
    icon: 'report',
    color: 'caution',
    count: props.alerts.filter(a => (a.severity || a.riskBand) === 'SUSPICIOUS').length
  }
])

const categoryTabs = computed(() => {
  const categoryOrder = [
    ['ALL', 'Tất cả'],
    ['ANSWER_PATTERN', 'Mẫu đáp án'],
    ['TIMING_PATTERN', 'Thời gian'],
    ['SESSION_INTEGRITY', 'Phiên thi'],
    ['IDENTITY_NETWORK', 'Mạng/thiết bị'],
    ['CAMERA_PROCTORING', 'Camera/Mic']
  ]
  return categoryOrder.map(([value, label]) => ({
    value,
    label,
    count: value === 'ALL'
      ? props.alerts.length
      : props.alerts.filter(a => (a.category || a.warningCategory || a.latestSignalCategory || 'UNCATEGORIZED') === value).length
  }))
})

const categoryLabel = (category) => {
  const map = {
    ANSWER_PATTERN: 'Mẫu đáp án',
    TIMING_PATTERN: 'Thời gian',
    SESSION_INTEGRITY: 'Phiên thi',
    IDENTITY_NETWORK: 'Mạng/thiết bị',
    CAMERA_PROCTORING: 'Camera/Mic'
  }
  return map[category] || category
}

const alertSignalType = (alert) =>
  String(alert.signalType || alert.warningType || alert.latestSignal?.signalType || alert.eventType || alert.type || '').toUpperCase()

const isSpeechAlert = (alert) => {
  const text = `${alertSignalType(alert)} ${String(alert.message || '').toUpperCase()}`
  return text.includes('SPEAK') || text.includes('VOICE')
}

const severityColor = (severity) => {
  const map = { CRITICAL: 'danger', HIGH_RISK: 'warning', SUSPICIOUS: 'caution', CLEAN: 'success' }
  return map[severity] || 'default'
}

const alertIcon = (alert) => {
  const type = alertSignalType(alert)
  if (isSpeechAlert(alert)) return 'mic'
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
  const type = alertSignalType(alert) || String(alert.message || '')
  if (isSpeechAlert(alert)) return 'Tiếng ồn'
  if (type.includes('TAB') || type.includes('SWITCH')) return 'Chuyển tab'
  if (type.includes('CAMERA')) return 'Camera tắt'
  if (type.includes('MIC')) return 'Micro tắt'
  if (type.includes('DEVTOOLS')) return 'Mở công cụ phát triển'
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

const alertTypeLabel = (alert) => {
  const sev = alert.severity || alert.riskBand || ''
  const map = { CRITICAL: 'Nghiêm trọng', HIGH_RISK: 'Nguy cơ cao', SUSPICIOUS: 'Đáng nghi', CLEAN: 'Sạch sẽ', '': '' }
  return map[sev] || sev
}

// Group by time bucket
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
</script>


<style scoped>
.ap {
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

.dark .ap {
  border-color: var(--ds-border-strong);
}

.ap--has-critical {
  border-color: rgba(220, 38, 38, 0.3);
}

/* Header */
.ap__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.5rem;
  padding: 0.875rem 1rem;
  border-bottom: 1px solid var(--ds-border);
  background: linear-gradient(135deg, rgba(220, 38, 38, 0.06) 0%, transparent 100%);
  flex-shrink: 0;
}

.dark .ap__header {
  border-bottom-color: var(--ds-border-strong);
}

.ap__header-left {
  display: flex;
  align-items: center;
  gap: 0.625rem;
}

.ap__header-icon-wrap {
  width: 36px;
  height: 36px;
  border-radius: var(--ds-radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.ap__header-icon-wrap--danger { background: var(--ds-danger-soft); color: var(--ds-danger); }
.ap__header-icon-wrap--warning { background: var(--ds-warning-soft); color: var(--ds-warning); }

.ap__header-icon { font-size: 1.125rem; }

.ap__title {
  font-family: var(--ds-font-display);
  font-size: 0.875rem;
  font-weight: 800;
  color: var(--ds-text);
  margin: 0;
  line-height: 1.2;
}

.dark .ap__title { color: var(--ds-text); }

.ap__count {
  background: var(--ds-warning);
  color: white;
  font-size: 0.6rem;
  font-weight: 800;
  padding: 0.1rem 0.5rem;
  border-radius: var(--ds-radius-full);
  min-width: 18px;
  text-align: center;
}

.ap__header-right {
  display: flex;
  align-items: center;
  gap: 0.375rem;
}

/* Sound button */
.ap__sound-btn {
  width: 1.875rem;
  height: 1.875rem;
  border: 1.5px solid var(--ds-border);
  border-radius: var(--ds-radius-md);
  background: transparent;
  color: var(--ds-text-muted);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: color 0.12s ease, background-color 0.12s ease, border-color 0.12s ease, box-shadow 0.12s ease, transform 0.12s ease;
}

.dark .ap__sound-btn {
  background: var(--ds-gray-800);
  border-color: var(--ds-border-strong);
}

.ap__sound-btn:hover { background: var(--ds-gray-100); color: var(--ds-text); }
.dark .ap__sound-btn:hover { background: var(--ds-gray-700); }

.ap__sound-btn--active {
  color: var(--ds-primary);
  border-color: var(--ds-primary-border);
  background: var(--ds-primary-soft);
}


.ap__mark-read {
  width: 1.875rem;
  height: 1.875rem;
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

.dark .ap__mark-read { color: var(--ds-text-secondary); }
.ap__mark-read:hover { background: var(--ds-gray-100); color: var(--ds-primary); }
.dark .ap__mark-read:hover { background: var(--ds-gray-700); }

/* Filter row */
.ap__filter-row {
  padding: 0.625rem 0.875rem;
  border-bottom: 1px solid var(--ds-border);
  flex-shrink: 0;
}

.dark .ap__filter-row { border-bottom-color: var(--ds-border-strong); }

.ap__filter-select-wrap {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: 0.625rem;
  width: 100%;
  box-sizing: border-box;
  padding: 0 0.625rem;
  background: var(--ds-surface);
  border: 1.5px solid var(--ds-border);
  border-radius: var(--ds-radius-lg);
  transition: border-color 0.15s ease, box-shadow 0.15s ease, background-color 0.15s ease;
}

.dark .ap__filter-select-wrap {
  background: var(--ds-gray-800);
  border-color: var(--ds-border-strong);
}

.ap__filter-select-wrap:focus-within {
  border-color: var(--ds-primary);
  box-shadow: 0 0 0 3px var(--ds-primary-ring);
}

.ap__filter-icon,
.ap__filter-arrow {
  width: 16px;
  height: 16px;
  color: var(--ds-text-muted);
  pointer-events: none;
}

.ap__filter-select {
  width: 100%;
  min-width: 0;
  padding: 0.5rem 0;
  border: none;
  background: transparent;
  color: var(--ds-text);
  font-size: 0.8rem;
  font-weight: 600;
  cursor: pointer;
  appearance: none;
  outline: none;
  font-family: inherit;
}

.dark .ap__filter-select {
  color: var(--ds-text);
}

/* List */
.ap__list {
  flex: 1;
  overflow-x: hidden;
  overflow-y: auto;
  scrollbar-width: thin;
  scrollbar-color: var(--ds-gray-300) transparent;
}

.ap__list::-webkit-scrollbar { width: 4px; }
.ap__list::-webkit-scrollbar-track { background: transparent; }
.ap__list::-webkit-scrollbar-thumb { background: var(--ds-gray-300); border-radius: 2px; }
.dark .ap__list::-webkit-scrollbar-thumb { background: var(--ds-gray-600); }

/* Group */
.ap__group {
  padding: 0.375rem 0;
}

.ap__group-label {
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


.ap__group-count {
  margin-left: auto;
  background: var(--ds-gray-100);
  padding: 0.05rem 0.4rem;
  border-radius: var(--ds-radius-full);
  font-size: 0.6rem;
}

.dark .ap__group-count { background: var(--ds-gray-700); }

.ap__group-items {
  display: flex;
  flex-direction: column;
}

/* Alert item */
.ap__alert-item {
  display: flex;
  align-items: flex-start;
  gap: 0.625rem;
  padding: 0.625rem 0.875rem;
  padding-right: 1.5rem;
  cursor: pointer;
  transition: background 0.1s ease;
  position: relative;
  border-left: 4px solid transparent;
  overflow: visible;
}

.ap__alert-item:hover {
  background: var(--ds-gray-50);
}

.dark .ap__alert-item:hover { background: var(--ds-gray-800); }

.ap__alert-item--unread {
  background: rgba(79, 70, 229, 0.04);
}

.dark .ap__alert-item--unread { background: rgba(79, 70, 229, 0.06); }

/* Flash animation for new alerts */
.ap__alert-item--flash {
  animation: apFlash 3s ease-out forwards;
}

@keyframes apFlash {
  0% { background: rgba(220, 38, 38, 0.08); }
  20% { background: transparent; }
  30% { background: rgba(220, 38, 38, 0.05); }
  100% { background: transparent; }
}

/* Severity borders — 4px */
.ap__alert-item--danger { border-left-color: var(--ds-danger); }
.ap__alert-item--warning { border-left-color: var(--ds-warning); }
.ap__alert-item--caution { border-left-color: var(--ds-accent); }
.ap__alert-item--success { border-left-color: var(--ds-success); }
.ap__alert-item--default { border-left-color: var(--ds-gray-300); }
.dark .ap__alert-item--default { border-left-color: var(--ds-gray-600); }

/* Alert icon — bigger for CRITICAL */
.ap__alert-icon {
  width: 32px;
  height: 32px;
  border-radius: var(--ds-radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  margin-top: 0.1rem;
}


.ap__alert-icon--danger {
  background: var(--ds-danger-soft);
  color: var(--ds-danger);
  box-shadow: 0 0 0 3px rgba(220, 38, 38, 0.1);
}

.ap__alert-icon--warning { background: var(--ds-warning-soft); color: var(--ds-warning); }
.ap__alert-icon--caution { background: var(--ds-accent-soft); color: var(--ds-accent); }
.ap__alert-icon--success { background: var(--ds-success-soft); color: var(--ds-success); }
.ap__alert-icon--default { background: var(--ds-gray-100); color: var(--ds-text-muted); }
.dark .ap__alert-icon--default { background: var(--ds-gray-700); }

/* Body */
.ap__alert-body {
  flex: 1;
  min-width: 0;
}

.ap__alert-title-row {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.ap__alert-title {
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

.dark .ap__alert-title { color: var(--ds-text); }

/* Alert type badge */
.ap__alert-type-badge {
  font-size: 0.55rem;
  font-weight: 800;
  padding: 0.1rem 0.4rem;
  border-radius: var(--ds-radius-full);
  text-transform: uppercase;
  letter-spacing: 0.05em;
  white-space: nowrap;
  flex-shrink: 0;
}

.ap__alert-type-badge--danger {
  background: var(--ds-danger-soft);
  color: var(--ds-danger);
  border: 1px solid rgba(220, 38, 38, 0.2);
}

.ap__alert-type-badge--warning {
  background: var(--ds-warning-soft);
  color: var(--ds-warning);
  border: 1px solid rgba(217, 119, 6, 0.2);
}

.ap__alert-type-badge--caution {
  background: var(--ds-accent-soft);
  color: var(--ds-accent);
  border: 1px solid rgba(245, 158, 11, 0.15);
}

.ap__alert-type-badge--success {
  background: var(--ds-success-soft);
  color: var(--ds-success);
  border: 1px solid rgba(22, 163, 74, 0.2);
}

.ap__alert-type-badge--default {
  background: var(--ds-gray-100);
  color: var(--ds-text-muted);
  border: 1px solid var(--ds-border);
}

.dark .ap__alert-type-badge--default { background: var(--ds-gray-700); }

.ap__alert-category-badge {
  font-size: 0.58rem;
  font-weight: 800;
  padding: 0.1rem 0.42rem;
  border-radius: var(--ds-radius-full);
  background: rgba(14, 165, 233, 0.1);
  color: var(--ds-info);
  border: 1px solid rgba(14, 165, 233, 0.2);
  white-space: nowrap;
  flex-shrink: 0;
}

.ap__alert-risk-badge {
  font-size: 0.58rem;
  font-weight: 800;
  padding: 0.1rem 0.42rem;
  border-radius: var(--ds-radius-full);
  background: var(--ds-primary-soft);
  color: var(--ds-primary);
  border: 1px solid var(--ds-primary-border);
  white-space: nowrap;
  flex-shrink: 0;
}

.ap__alert-student {
  font-size: 0.7rem;
  color: var(--ds-text-muted);
  margin: 0.2rem 0 0;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* Meta */
.ap__alert-meta {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 0.375rem;
  flex-shrink: 0;
}

.ap__alert-time {
  font-size: 0.65rem;
  color: var(--ds-text-muted);
  font-weight: 600;
  white-space: nowrap;
}

.ap__alert-actions {
  display: flex;
  gap: 0.125rem;
  opacity: 0;
  transition: opacity 0.15s ease;
}

.ap__alert-item:hover .ap__alert-actions {
  opacity: 1;
}

.ap__alert-action {
  width: 1.625rem;
  height: 1.625rem;
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

.dark .ap__alert-action { background: var(--ds-gray-700); }
.ap__alert-action:hover { background: var(--ds-gray-200); color: var(--ds-text); }
.dark .ap__alert-action:hover { background: var(--ds-gray-600); }

/* Unread dot */
.ap__unread-dot {
  position: absolute;
  top: 0.5rem;
  right: 0.75rem;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--ds-danger);
  box-shadow: 0 0 0 2px rgba(220, 38, 38, 0.2);
}

/* Loading skeleton */
.ap__loading {
  padding: 0.5rem 0;
}

.ap__skeleton-row {
  display: flex;
  align-items: center;
  gap: 0.625rem;
  padding: 0.625rem 0.875rem;
}

.ap__skeleton {
  background: linear-gradient(90deg, var(--ds-gray-100) 25%, var(--ds-gray-200) 50%, var(--ds-gray-100) 75%);
  background-size: 200% 100%;
  animation: apShimmer 1.2s ease-in-out infinite;
  border-radius: var(--ds-radius-md);
  transform: translateZ(0);
}

.dark .ap__skeleton {
  background: linear-gradient(90deg, var(--ds-gray-800) 25%, var(--ds-gray-700) 50%, var(--ds-gray-800) 75%);
  background-size: 200% 100%;
}

@keyframes apShimmer {
  0% { background-position: -200% 0; }
  100% { background-position: 200% 0; }
}

.ap__skeleton--dot { width: 32px; height: 32px; border-radius: var(--ds-radius-lg); flex-shrink: 0; }
.ap__skeleton--line { height: 0.75rem; margin-bottom: 0.25rem; }
.ap__skeleton--time { width: 36px; height: 0.625rem; margin-left: auto; flex-shrink: 0; }
.ap__skeleton-content { flex: 1; min-width: 0; }

/* Empty */
.ap__empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.5rem;
  padding: 2.5rem 1rem;
  text-align: center;
}

.ap__empty-icon {
  font-size: 2.5rem !important;
  color: var(--ds-success);
  opacity: 0.5;
}

.ap__empty-title {
  font-size: 0.875rem;
  font-weight: 700;
  color: var(--ds-text);
  margin: 0;
}

.dark .ap__empty-title { color: var(--ds-text); }

.ap__empty-desc {
  font-size: 0.75rem;
  color: var(--ds-text-muted);
  margin: 0;
}

/* Footer */
.ap__footer {
  padding: 0.75rem 0.875rem;
  border-top: 1px solid var(--ds-border);
  flex-shrink: 0;
}

.dark .ap__footer { border-top-color: var(--ds-border-strong); }

.ap__show-more {
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

.dark .ap__show-more { border-color: var(--ds-border-strong); }
.ap__show-more:hover { border-color: var(--ds-primary); color: var(--ds-primary); background: var(--ds-primary-soft); }
.dark .ap__show-more:hover { background: rgba(79, 70, 229, 0.1); }

/* Transition */
.ap-alert-enter-active {
  transition: color 0.3s ease, background-color 0.3s ease, border-color 0.3s ease, box-shadow 0.3s ease, transform 0.3s ease;
}

.ap-alert-enter-from {
  opacity: 0;
  transform: translateX(-12px);
  background: rgba(220, 38, 38, 0.06);
}

.ap-alert-leave-active {
  transition: color 0.2s ease, background-color 0.2s ease, border-color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease;
}

.ap-alert-leave-to {
  opacity: 0;
  transform: translateX(12px);
}

/* Responsive */
@media (max-width: 768px) {
  .ap__filter-row { overflow-x: auto; }
  .ap__alert-item { padding: 0.5rem; }
  .ap__alert-meta { flex-wrap: wrap; gap: 0.25rem; }
}
</style>
