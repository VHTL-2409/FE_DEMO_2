<template>
  <div
    class="rsc"
    :class="[`rsc--${statusLevel}`, { 'rsc--compact': compact }]"
    @click="$emit('view-room')"
  >
    <!-- Status indicator stripe -->
    <div class="rsc__stripe" :class="`rsc__stripe--${statusLevel}`" />

    <!-- Header row -->
    <div class="rsc__header">
      <div class="rsc__status-dot" :class="`rsc__status-dot--${statusLevel}`">
        <LucideIcon :name="statusIcon" />
      </div>

      <div class="rsc__exam-info">
        <p class="rsc__exam-name">{{ examName }}</p>
        <p class="rsc__exam-meta">
          <span v-if="subject">{{ subject }}</span>
          <span v-if="subject && className"> · </span>
          <span v-if="className">{{ className }}</span>
        </p>
      </div>

      <div class="rsc__header-right">
        <span v-if="timeRemaining" class="rsc__time-badge" :class="`rsc__time-badge--${timeColor}`">
          <LucideIcon name="timer" />
          {{ timeRemaining }}
        </span>
        <span v-else-if="isEnded" class="rsc__time-badge rsc__time-badge--ended">
          <LucideIcon name="check_circle" />
          Đã kết thúc
        </span>
        <span v-else class="rsc__time-badge rsc__time-badge--no-limit">
          <LucideIcon name="all_inclusive" />
          Không giới hạn
        </span>
      </div>
    </div>

    <!-- Divider -->
    <div class="rsc__divider" />

    <!-- Stats row -->
    <div class="rsc__stats">
      <!-- Online -->
      <div class="rsc__stat" :class="{ 'rsc__stat--success': onlineCount > 0 }">
        <LucideIcon name="group" />
        <div class="rsc__stat-body-col">
          <span class="rsc__stat-val">{{ onlineCount }}</span>
          <span class="rsc__stat-label">/ {{ totalCount }} online</span>
        </div>
        <!-- Mini progress -->
        <div class="rsc__mini-bar">
          <div class="rsc__mini-bar-fill rsc__mini-bar-fill--success"
            :style="{ width: totalCount ? (onlineCount / totalCount * 100) + '%' : '0%' }" />
        </div>
      </div>

      <!-- Alerts -->
      <div class="rsc__stat" :class="{ 'rsc__stat--warning': alertCount > 0 }">
        <LucideIcon name="warning" />
        <div class="rsc__stat-body-col">
          <span class="rsc__stat-val">{{ alertCount }}</span>
          <span class="rsc__stat-label">cảnh báo</span>
        </div>
        <!-- Mini progress for alerts -->
        <div class="rsc__mini-bar">
          <div class="rsc__mini-bar-fill rsc__mini-bar-fill--warn"
            :style="{ width: totalCount ? Math.min(alertCount / totalCount * 100, 100) + '%' : '0%' }" />
        </div>
      </div>

      <!-- Offline -->
      <div class="rsc__stat" :class="{ 'rsc__stat--danger': offlineCount > 0 }">
        <LucideIcon :name="offlineCount > 0 ? 'wifi_off' : 'wifi'" />
        <div class="rsc__stat-body-col">
          <span class="rsc__stat-val">{{ offlineCount }}</span>
          <span class="rsc__stat-label">offline</span>
        </div>
        <!-- Mini progress -->
        <div class="rsc__mini-bar">
          <div class="rsc__mini-bar-fill rsc__mini-bar-fill--danger"
            :style="{ width: totalCount ? (offlineCount / totalCount * 100) + '%' : '0%' }" />
        </div>
      </div>
    </div>

    <!-- Risk bar (larger, full width) -->
    <div class="rsc__risk-section">
      <div class="rsc__risk-bar">
        <div
          class="rsc__risk-fill"
          :class="`rsc__risk-fill--${riskLevel}`"
          :style="{ width: riskPercent + '%' }"
        />
      </div>
      <div class="rsc__risk-meta">
        <span class="rsc__risk-label" :class="`rsc__risk-label--${riskLevel}`">
          <LucideIcon :name="riskIcon" />
          {{ riskLabel }}
        </span>
        <span v-if="riskScore !== null" class="rsc__risk-score" :class="`rsc__risk-score--${riskLevel}`">
          {{ riskScore }}
        </span>
      </div>
    </div>

    <!-- Divider -->
    <div class="rsc__divider" />

    <!-- Actions row -->
    <div class="rsc__actions">
      <button type="button" class="rsc__action-btn rsc__action-btn--primary" @click.stop="$emit('view-room')">
        <LucideIcon name="visibility" />
        Xem phòng
      </button>
      <button type="button" class="rsc__action-btn" @click.stop="$emit('manage')">
        <LucideIcon name="settings" />
      </button>
      <button
        type="button"
        class="rsc__action-btn"
        :class="{ 'rsc__action-btn--danger': isActive }"
        @click.stop="$emit('pause')"
      >
        <LucideIcon :name="isActive ? 'pause' : 'play_arrow'" />
      </button>
    </div>

    <!-- Connection status -->
    <div v-if="connectionStatus !== 'online'" class="rsc__connection-banner" :class="`rsc__connection-banner--${connectionStatus}`">
      <LucideIcon :name="connectionStatus === 'offline' ? 'wifi_off' : 'signal_wifi_connected_no_internet_4'" />
      {{ connectionStatus === 'offline' ? 'Mất kết nối' : 'Kết nối không ổn định' }}
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  exam: { type: Object, default: null },
  compact: { type: Boolean, default: false },
  connectionStatus: {
    type: String,
    default: 'online',
    validator: (v) => ['online', 'offline', 'degraded'].includes(v)
  }
})

defineEmits(['view-room', 'manage', 'pause'])

// ── Computed data ──────────────────────────────────────────────────────────────

const examName = computed(() => props.exam?.title || '—')
const subject = computed(() => props.exam?.subject || '')
const className = computed(() => props.exam?.className || props.exam?.classInfo || '')
const totalCount = computed(() => props.exam?.studentCount || props.exam?.participantCount || 0)
const onlineCount = computed(() => props.exam?.answeredCount || 0)
const alertCount = computed(() => props.exam?.alertCount || 0)
const offlineCount = computed(() => Math.max(0, totalCount.value - onlineCount.value))
const isActive = computed(() => props.exam?.isLive || props.exam?.isActive || false)
const isEnded = computed(() => props.exam?.status === 'ended' || props.exam?.isEnded || false)

const riskScore = computed(() => {
  const s = props.exam?.riskScore
  return s !== undefined && s !== null ? Number(s) : null
})

const riskLevel = computed(() => {
  const s = riskScore.value
  if (s === null) return 'clean'
  if (s >= 81) return 'critical'
  if (s >= 61) return 'high_risk'
  if (s >= 31) return 'suspicious'
  return 'clean'
})

const riskPercent = computed(() => Math.min(100, Math.max(0, riskScore.value || 0)))
const riskLabel = computed(() => {
  const map = { clean: 'Sạch sẽ', suspicious: 'Đáng nghi', high_risk: 'Nguy cơ cao', critical: 'Nghiêm trọng' }
  return map[riskLevel.value] || '—'
})

const statusLevel = computed(() => {
  if (props.connectionStatus === 'offline') return 'offline'
  if (props.connectionStatus === 'degraded') return 'warning'
  if (riskLevel.value === 'critical' || riskLevel.value === 'high_risk') return 'critical'
  if (riskLevel.value === 'suspicious') return 'attention'
  return 'stable'
})

const riskIcon = computed(() => {
  const map = {
    clean: 'check_circle',
    suspicious: 'report',
    high_risk: 'warning',
    critical: 'gpp_bad'
  }
  return map[riskLevel.value] || 'check_circle'
})

const statusIcon = computed(() => {
  const map = {
    stable: 'verified_user',
    attention: 'warning',
    critical: 'gpp_bad',
    offline: 'wifi_off'
  }
  return map[statusLevel.value] || 'verified_user'
})

// Time remaining
const timeRemaining = computed(() => {
  if (!props.exam?.startTime || !props.exam?.durationMinutes) return null
  const start = new Date(props.exam.startTime)
  const end = new Date(start.getTime() + (props.exam.durationMinutes || 60) * 60000)
  const now = new Date()
  if (now < start) return 'Chưa bắt đầu'
  if (now > end) return null // ended
  const diff = Math.round((end - now) / 60000)
  if (diff < 60) return `${diff}m còn lại`
  const h = Math.floor(diff / 60)
  const m = diff % 60
  return `${h}h ${m}m còn lại`
})

const timeColor = computed(() => {
  if (!timeRemaining.value) return 'default'
  if (timeRemaining.value.includes('Chưa')) return 'default'
  const mins = parseInt(timeRemaining.value, 10)
  if (isNaN(mins)) return 'default'
  if (mins < 5) return 'danger'
  if (mins < 15) return 'warning'
  return 'success'
})
</script>


<style scoped>
.rsc {
  position: relative;
  background: var(--ds-surface);
  border: 1.5px solid var(--ds-border);
  border-radius: var(--ds-radius-2xl);
  overflow: visible;
  cursor: pointer;
  transition: all 0.2s ease;
  display: flex;
  flex-direction: column;
  min-height: 200px;
}

.dark .rsc {
  border-color: var(--ds-border-strong);
}

/* Status stripe */
.rsc__stripe {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  border-radius: var(--ds-radius-2xl) var(--ds-radius-2xl) 0 0;
}

.rsc__stripe--stable { background: var(--ds-success); }
.rsc__stripe--attention { background: var(--ds-warning); }
.rsc__stripe--critical { background: var(--ds-danger); }
.rsc__stripe--offline { background: var(--ds-gray-400); }

/* Hover effects */
.rsc:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
}

.rsc--stable:hover { border-color: rgba(22, 163, 74, 0.3); box-shadow: 0 8px 32px rgba(22, 163, 74, 0.08); }
.rsc--attention:hover { border-color: rgba(217, 119, 6, 0.3); box-shadow: 0 8px 32px rgba(217, 119, 6, 0.08); }
.rsc--critical:hover { border-color: rgba(220, 38, 38, 0.3); box-shadow: 0 8px 32px rgba(220, 38, 38, 0.1); }
.rsc--offline:hover { border-color: var(--ds-gray-300); box-shadow: 0 8px 32px rgba(0, 0, 0, 0.08); }

/* Compact */
.rsc--compact .rsc__stats { gap: 0.375rem; }
.rsc--compact .rsc__stat { padding: 0.375rem 0.5rem; }
.rsc--compact .rsc__actions { padding: 0.5rem 0.75rem; }

/* Header */
.rsc__header {
  display: flex;
  align-items: flex-start;
  gap: 0.75rem;
  padding: 1rem 1rem 0.875rem;
}

.rsc__status-dot {
  width: 32px;
  height: 32px;
  border-radius: var(--ds-radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}


.rsc__status-dot--stable {
  background: var(--ds-success-soft);
  color: var(--ds-success);
}

.rsc__status-dot--attention {
  background: rgba(234, 179, 8, 0.1);
  color: var(--ds-warning);
}

.rsc__status-dot--critical {
  background: var(--ds-danger-soft);
  color: var(--ds-danger);
  animation: rscPulse 2s ease-in-out infinite;
}

.rsc__status-dot--offline {
  background: var(--ds-gray-100);
  color: var(--ds-gray-500);
}

.dark .rsc__status-dot--offline {
  background: var(--ds-gray-700);
}

@keyframes rscPulse {
  0%, 100% { box-shadow: 0 0 0 0 rgba(220, 38, 38, 0.3); }
  50% { box-shadow: 0 0 0 6px rgba(220, 38, 38, 0); }
}

.rsc__exam-info {
  flex: 1;
  min-width: 0;
}

.rsc__exam-name {
  font-family: var(--ds-font-display);
  font-size: 0.875rem;
  font-weight: 800;
  color: var(--ds-text);
  margin: 0;
  line-height: 1.3;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.dark .rsc__exam-name { color: var(--ds-text); }

.rsc__exam-meta {
  font-size: 0.7rem;
  color: var(--ds-text-muted);
  margin: 0.25rem 0 0;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.rsc__header-right {
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 0.375rem;
}

/* Time badge */
.rsc__time-badge {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  padding: 0.25rem 0.625rem;
  border-radius: var(--ds-radius-full);
  font-size: 0.7rem;
  font-weight: 700;
  background: var(--ds-gray-100);
  color: var(--ds-text-muted);
}

.dark .rsc__time-badge {
  background: var(--ds-gray-700);
}


.rsc__time-badge--danger {
  background: var(--ds-danger-soft);
  color: var(--ds-danger);
}

.rsc__time-badge--warning {
  background: rgba(234, 179, 8, 0.1);
  color: var(--ds-warning);
}

.rsc__time-badge--success {
  background: var(--ds-success-soft);
  color: var(--ds-success);
}

.rsc__time-badge--ended {
  background: var(--ds-gray-100);
  color: var(--ds-text-muted);
}

.rsc__time-badge--no-limit {
  background: var(--ds-primary-soft);
  color: var(--ds-primary);
}

/* Divider */
.rsc__divider {
  height: 1px;
  background: var(--ds-border);
  margin: 0;
}

.dark .rsc__divider { background: var(--ds-border-strong); }

/* Stats */
.rsc__stats {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem 1rem;
}

.rsc__stat {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  padding: 0.5rem 0.625rem;
  border-radius: var(--ds-radius-lg);
  background: var(--ds-gray-50);
  font-size: 0.75rem;
  flex: 1;
  justify-content: flex-start;
  flex-direction: column;
  align-items: stretch;
  transition: all 0.15s ease;
}

.dark .rsc__stat { background: var(--ds-gray-800); }

/* Inline row within stat */
.rsc__stat > .lucide {
  font-size: 0.875rem;
  color: var(--ds-text-muted);
  flex-shrink: 0;
  align-self: flex-start;
  margin-bottom: 0.25rem;
}

/* Body column */
.rsc__stat-body-col {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  flex-wrap: wrap;
}

.rsc__stat--success > .lucide { color: var(--ds-success); }
.rsc__stat--warning > .lucide { color: var(--ds-warning); }
.rsc__stat--danger > .lucide { color: var(--ds-danger); }
.rsc__stat--success { background: var(--ds-success-soft); }
.rsc__stat--warning { background: rgba(234, 179, 8, 0.08); }
.rsc__stat--danger { background: var(--ds-danger-soft); }

.rsc__stat-val {
  font-weight: 900;
  color: var(--ds-text);
  font-family: var(--ds-font-display);
  font-size: 0.8rem;
  flex-shrink: 0;
}

.dark .rsc__stat-val { color: var(--ds-text); }
.rsc__stat--success .rsc__stat-val { color: var(--ds-success); }
.rsc__stat--warning .rsc__stat-val { color: var(--ds-warning); }
.rsc__stat--danger .rsc__stat-val { color: var(--ds-danger); }

.rsc__stat-label {
  color: var(--ds-text-muted);
  font-weight: 500;
  font-size: 0.65rem;
}

/* Mini progress bar */
.rsc__mini-bar {
  height: 3px;
  background: rgba(0,0,0,0.08);
  border-radius: 2px;
  overflow: hidden;
  margin-top: 0.375rem;
}

.dark .rsc__mini-bar { background: rgba(255,255,255,0.08); }

.rsc__mini-bar-fill {
  height: 100%;
  border-radius: 2px;
  transition: width 0.4s ease;
}

.rsc__mini-bar-fill--success { background: var(--ds-success); }
.rsc__mini-bar-fill--warn { background: var(--ds-warning); }
.rsc__mini-bar-fill--danger { background: var(--ds-danger); }

/* Risk section (larger full-width bar) */
.rsc__risk-section {
  padding: 0 1rem 0.75rem;
}

.rsc__risk-bar {
  height: 8px;
  background: var(--ds-gray-200);
  border-radius: 4px;
  overflow: hidden;
  margin-bottom: 0.5rem;
}

.dark .rsc__risk-bar { background: var(--ds-gray-700); }

.rsc__risk-fill {
  height: 100%;
  border-radius: 4px;
  transition: width 0.4s ease;
}

.rsc__risk-fill--clean { background: var(--ds-success); }
.rsc__risk-fill--suspicious { background: var(--ds-warning); }
.rsc__risk-fill--high_risk { background: var(--ds-warning); }
.rsc__risk-fill--critical { background: var(--ds-danger); }

.rsc__risk-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.5rem;
}

.rsc__risk-label {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  font-size: 0.7rem;
  font-weight: 700;
}

.rsc__risk-icon { font-size: 0.875rem; }

.rsc__risk-label--clean { color: var(--ds-success); }
.rsc__risk-label--suspicious { color: var(--ds-warning); }
.rsc__risk-label--high_risk { color: var(--ds-danger); }
.rsc__risk-label--critical { color: var(--ds-danger); }

.rsc__risk-score {
  font-size: 0.8rem;
  font-weight: 900;
  font-family: var(--ds-font-display);
  flex-shrink: 0;
}

.rsc__risk-score--clean { color: var(--ds-success); }
.rsc__risk-score--suspicious { color: var(--ds-warning); }
.rsc__risk-score--high_risk { color: var(--ds-danger); }
.rsc__risk-score--critical { color: var(--ds-danger); }

/* Actions */
.rsc__actions {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem 1rem;
}

.rsc__action-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.375rem;
  padding: 0.5rem 0.875rem;
  border-radius: var(--ds-radius-lg);
  border: 1.5px solid var(--ds-border);
  background: var(--ds-surface);
  color: var(--ds-text-secondary);
  font-size: 0.75rem;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.15s ease;
  flex: 1;
  justify-content: center;
  font-family: inherit;
}

.dark .rsc__action-btn {
  background: var(--ds-gray-800);
  border-color: var(--ds-border-strong);
  color: var(--ds-text-muted);
}

.rsc__action-btn:hover {
  border-color: var(--ds-primary);
  color: var(--ds-primary);
  background: var(--ds-primary-soft);
}

.dark .rsc__action-btn:hover { background: rgba(79, 70, 229, 0.1); }


.rsc__action-btn--primary {
  background: var(--ds-primary);
  border-color: var(--ds-primary);
  color: white;
  box-shadow: 0 2px 8px rgba(79, 70, 229, 0.2);
}

.rsc__action-btn--primary:hover {
  background: var(--ds-primary-hover);
  color: white;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(79, 70, 229, 0.3);
}

.rsc__action-btn--danger {
  color: var(--ds-warning);
  border-color: rgba(217, 119, 6, 0.3);
  background: rgba(234, 179, 8, 0.05);
}

.dark .rsc__action-btn--danger {
  background: rgba(234, 179, 8, 0.1);
}

.rsc__action-btn--danger:hover {
  background: rgba(234, 179, 8, 0.15);
  border-color: var(--ds-warning);
  color: var(--ds-warning);
}

.rsc__connection-banner--degraded {
  background: var(--ds-warning);
}
</style>
