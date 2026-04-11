<template>
  <div class="mch" :class="{ 'mch--alerting': isAlerting }">
    <!-- Back + Title Group -->
    <div class="mch__left">
      <button type="button" class="mch__back-btn" @click="$emit('back')">
        <LucideIcon name="arrow_back" />
      </button>

      <div class="mch__title-group">
        <!-- Live badge -->
        <div class="mch__live-badge" :class="isConnected ? 'mch__live-badge--live' : 'mch__live-badge--off'">
          <span class="mch__live-dot" />
          <span class="mch__live-label">{{ isConnected ? 'LIVE' : 'ĐANG KẾT NỐI' }}</span>
        </div>

        <!-- Title -->
        <div class="mch__title-text">
          <h1 class="mch__title">{{ examTitle }}</h1>
          <p v-if="classInfo" class="mch__subtitle">{{ classInfo }}</p>
        </div>
      </div>
    </div>

    <!-- Stats Row -->
    <div class="mch__stats">
      <!-- Online Students -->
      <div class="mch__stat" :class="{ 'mch__stat--success': onlineCount > 0 }">
        <div class="mch__stat-icon-wrap mch__stat-icon-wrap--success">
          <LucideIcon name="group" />
        </div>
        <div class="mch__stat-body">
          <p class="mch__stat-val">{{ onlineCount }}</p>
          <p class="mch__stat-label">Online</p>
        </div>
      </div>

      <!-- Alerts -->
      <div
        class="mch__stat"
        :class="{ 'mch__stat--danger': alertCount > 0 }"
      >
        <div class="mch__stat-icon-wrap" :class="alertCount > 0 ? 'mch__stat-icon-wrap--danger' : 'mch__stat-icon-wrap--muted'">
          <LucideIcon name="warning" />
        </div>
        <div class="mch__stat-body">
          <p class="mch__stat-val">{{ alertCount }}</p>
          <p class="mch__stat-label">Cảnh báo</p>
        </div>
        <span v-if="alertCount > 0" class="mch__stat-pulse" />
      </div>

      <!-- Total Students -->
      <div class="mch__stat">
        <div class="mch__stat-icon-wrap mch__stat-icon-wrap--primary">
          <LucideIcon name="people" />
        </div>
        <div class="mch__stat-body">
          <p class="mch__stat-val">{{ totalCount }}</p>
          <p class="mch__stat-label">Tổng</p>
        </div>
      </div>
    </div>

    <!-- Right: Actions -->
    <div class="mch__right">
      <!-- Countdown if exam has end time -->
      <div v-if="countdownLabel" class="mch__countdown" :class="countdownClass">
        <LucideIcon name="timer" />
        <span class="mch__countdown-val">{{ countdownLabel }}</span>
      </div>

      <!-- Connection toggle -->
      <div class="mch__conn-toggle">
        <button
          type="button"
          class="mch__conn-btn"
          :class="{ 'mch__conn-btn--active': connectionMode === 'realtime' }"
          title="WebSocket thời gian thực"
          @click="$emit('connection-mode', 'realtime')"
        >
          <LucideIcon name="satellite_alt" />
          <span class="hidden lg:inline">TT</span>
        </button>
        <button
          type="button"
          class="mch__conn-btn"
          :class="{ 'mch__conn-btn--active': connectionMode === 'polling' }"
          title="Polling"
          @click="$emit('connection-mode', 'polling')"
        >
          <LucideIcon name="sync" />
          <span class="hidden lg:inline">CP</span>
        </button>
      </div>

      <!-- Toggle all panels -->
      <button
        type="button"
        class="mch__action-btn"
        :class="{ 'mch__action-btn--active-panel': allPanelsCollapsed }"
        title="Tắt/bật tất cả panel giám sát"
        @click="$emit('toggle-all-panels')"
      >
        <LucideIcon :name="allPanelsCollapsed ? 'expand_more' : 'vertical_distribute'" />
      </button>

      <!-- Alert panel toggle -->
      <button
        type="button"
        class="mch__action-btn"
        :class="{ 'mch__action-btn--alert': alertCount > 0 }"
        title="Bật/tắt panel cảnh báo"
        @click="$emit('toggle-alerts')"
      >
        <LucideIcon name="notifications" />
        <span v-if="alertCount > 0" class="mch__alert-badge">{{ alertCount > 99 ? '99+' : alertCount }}</span>
      </button>

      <!-- Refresh -->
      <button
        type="button"
        class="mch__action-btn"
        :disabled="loading"
        title="Làm mới dữ liệu"
        @click="$emit('refresh')"
      >
        <LucideIcon name="refresh" />
      </button>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'

const props = defineProps({
  examTitle: { type: String, default: 'Trung tâm giám sát' },
  classInfo: { type: String, default: '' },
  onlineCount: { type: Number, default: 0 },
  alertCount: { type: Number, default: 0 },
  issueCount: { type: Number, default: 0 },
  totalCount: { type: Number, default: 0 },
  isConnected: { type: Boolean, default: false },
  connectionMode: { type: String, default: 'realtime' },
  loading: { type: Boolean, default: false },
  endTime: { type: String, default: '' },
  allPanelsCollapsed: { type: Boolean, default: false }
})

defineEmits(['back', 'connection-mode', 'refresh', 'toggle-alerts', 'toggle-all-panels'])

// ── Online percent ────────────────────────────────────────────────────────────────
const onlinePercent = computed(() => {
  if (!props.totalCount) return 0
  return Math.round((props.onlineCount / props.totalCount) * 100)
})

// ── Health icon ───────────────────────────────────────────────────────────────────
const healthIcon = computed(() => {
  if (props.issueCount > 0) return 'signal_wifi_connected_no_internet_4'
  if (props.onlinePercent >= 100) return 'signal_wifi_4bar'
  if (props.onlinePercent >= 80) return 'signal_wifi_4_bar'
  if (props.onlinePercent >= 50) return 'network_wifi_3_bar'
  return 'signal_wifi_off'
})

const healthColorClass = computed(() => {
  if (props.issueCount > 0) return 'mch__stat--danger'
  if (props.onlinePercent >= 80) return 'mch__stat--success'
  if (props.onlinePercent >= 50) return 'mch__stat--warning'
  return 'mch__stat--muted'
})

const healthIconWrapClass = computed(() => {
  if (props.issueCount > 0) return 'mch__stat-icon-wrap--danger'
  if (props.onlinePercent >= 80) return 'mch__stat-icon-wrap--success'
  if (props.onlinePercent >= 50) return 'mch__stat-icon-wrap--warning'
  return 'mch__stat-icon-wrap--muted'
})

// ── Countdown timer ──────────────────────────────────────────────────────────────
const countdownLabel = computed(() => {
  if (!props.endTime) return null
  const end = new Date(props.endTime).getTime()
  const now = Date.now()
  if (now >= end) return null
  const diff = end - now
  const hours = Math.floor(diff / 3600000)
  const mins = Math.floor((diff % 3600000) / 60000)
  const secs = Math.floor((diff % 60000) / 1000)
  if (hours > 0) return `${hours}h ${mins}m`
  if (mins > 0) return `${mins}m ${secs}s`
  return `${secs}s`
})

const countdownClass = computed(() => {
  if (!countdownLabel.value) return ''
  const hours = countdownLabel.value.includes('h')
  if (hours) return 'mch__countdown--normal'
  return 'mch__countdown--urgent'
})

// ── Alert flash ───────────────────────────────────────────────────────────────────
const isAlerting = ref(false)
let alertTimer = null

watch(() => props.alertCount, (newVal, oldVal) => {
  if (newVal > oldVal) {
    isAlerting.value = true
    clearTimeout(alertTimer)
    alertTimer = setTimeout(() => {
      isAlerting.value = false
    }, 2000)
  }
})
</script>


<style scoped>
/* ── Base ─────────────────────────────────────────────────────────────────── */
.mch {
  display: flex;
  align-items: center;
  gap: 1.5rem;
  padding: 0.875rem 1.5rem;
  background: var(--ds-surface);
  border-bottom: 2px solid var(--ds-border);
  min-height: 80px;
  flex-wrap: wrap;
  position: relative;
  overflow: hidden;
  transition: background 0.3s ease;
}

/* Alert flash overlay */
.mch::before {
  content: '';
  position: absolute;
  inset: 0;
  background: rgba(220, 38, 38, 0.04);
  opacity: 0;
  pointer-events: none;
  transition: opacity 0.3s ease;
}

.mch--alerting::before {
  animation: mchAlertFlash 2s ease-out forwards;
}

@keyframes mchAlertFlash {
  0% { opacity: 1; }
  20% { opacity: 0; }
  40% { opacity: 0.6; }
  100% { opacity: 0; }
}

.dark .mch { background: rgba(30, 41, 59, 0.98); border-bottom-color: var(--ds-border-strong); }

/* ── Left ─────────────────────────────────────────────────────────────────── */
.mch__left {
  display: flex;
  align-items: center;
  gap: 0.875rem;
  flex-shrink: 0;
}

.mch__back-btn {
  width: 2.5rem;
  height: 2.5rem;
  border-radius: var(--ds-radius-lg);
  border: 1.5px solid var(--ds-border);
  background: transparent;
  color: var(--ds-text-muted);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: color 0.15s ease, background-color 0.15s ease, border-color 0.15s ease;
}

.dark .mch__back-btn {
  background: var(--ds-gray-800);
  border-color: var(--ds-border-strong);
}

.mch__back-btn:hover {
  background: var(--ds-gray-100);
  color: var(--ds-text);
  border-color: var(--ds-gray-300);
}

.dark .mch__back-btn:hover {
  background: var(--ds-gray-700);
  border-color: var(--ds-gray-600);
}


.mch__title-group {
  display: flex;
  align-items: center;
  gap: 0.875rem;
}

/* Live badge */
.mch__live-badge {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  padding: 0.25rem 0.75rem;
  border-radius: var(--ds-radius-full);
  border: 1.5px solid var(--ds-border);
  background: var(--ds-gray-100);
  font-size: 0.65rem;
  font-weight: 800;
  letter-spacing: 0.1em;
}

.dark .mch__live-badge { background: var(--ds-gray-800); border-color: var(--ds-border-strong); }

.mch__live-badge--live {
  background: rgba(22, 163, 74, 0.08);
  border-color: rgba(22, 163, 74, 0.25);
  color: var(--ds-success);
}

.mch__live-badge--off {
  color: var(--ds-text-muted);
}

.mch__live-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--ds-gray-400);
  flex-shrink: 0;
}

.dark .mch__live-dot { background: var(--ds-gray-600); }

.mch__live-badge--live .mch__live-dot {
  background: var(--ds-success);
  animation: mchLivePulse 1.5s ease-in-out infinite;
}

@keyframes mchLivePulse {
  0%, 100% { box-shadow: 0 0 0 0 rgba(22, 163, 74, 0.4); }
  50% { box-shadow: 0 0 0 6px rgba(22, 163, 74, 0); }
}

.mch__live-label { line-height: 1; }

/* Title */
.mch__title {
  font-family: var(--ds-font-display);
  font-size: 1.125rem;
  font-weight: 800;
  color: var(--ds-text);
  margin: 0;
  line-height: 1.2;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 280px;
}

.dark .mch__title { color: #f1f5f9; }

.mch__subtitle {
  font-size: 0.7rem;
  color: var(--ds-text-muted);
  margin: 0.25rem 0 0;
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 280px;
}

/* ── Stats ────────────────────────────────────────────────────────────────── */
.mch__stats {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex: 1;
  justify-content: center;
  flex-wrap: wrap;
}

.mch__stat {
  display: flex;
  align-items: center;
  gap: 0.625rem;
  padding: 0.5rem 1rem;
  background: var(--ds-gray-50);
  border: 1.5px solid var(--ds-border);
  border-radius: var(--ds-radius-xl);
  position: relative;
  transition: border-color 0.15s ease, background-color 0.15s ease, box-shadow 0.15s ease;
  min-width: 110px;
}

.dark .mch__stat {
  background: var(--ds-gray-800);
  border-color: var(--ds-border-strong);
}

.mch__stat:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(0,0,0,0.08);
}

/* Stat icon wrap */
.mch__stat-icon-wrap {
  width: 36px;
  height: 36px;
  border-radius: var(--ds-radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  background: var(--ds-gray-100);
  color: var(--ds-text-muted);
}

.dark .mch__stat-icon-wrap { background: var(--ds-gray-700); }


.mch__stat-icon-wrap--primary { background: var(--ds-primary-soft); color: var(--ds-primary); }
.mch__stat-icon-wrap--success { background: var(--ds-success-soft); color: var(--ds-success); }
.mch__stat-icon-wrap--warning { background: rgba(234, 179, 8, 0.1); color: var(--ds-warning); }
.mch__stat-icon-wrap--danger { background: var(--ds-danger-soft); color: var(--ds-danger); }
.mch__stat-icon-wrap--muted { background: var(--ds-gray-100); color: var(--ds-text-muted); }
.dark .mch__stat-icon-wrap--muted { background: var(--ds-gray-700); }

/* Stat body */
.mch__stat-body {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.mch__stat-val {
  font-size: 1.25rem;
  font-weight: 900;
  color: var(--ds-text);
  margin: 0;
  line-height: 1;
  font-family: var(--ds-font-display);
}

.dark .mch__stat-val { color: #f1f5f9; }

.mch__stat-label {
  font-size: 0.6rem;
  color: var(--ds-text-muted);
  margin: 0.2rem 0 0;
  font-weight: 600;
  white-space: nowrap;
}

/* Stat color variants */
.mch__stat--success .mch__stat-val { color: var(--ds-success); }
.mch__stat--warning .mch__stat-val { color: var(--ds-warning); }
.mch__stat--danger .mch__stat-val { color: var(--ds-danger); }
.mch__stat--muted .mch__stat-val { color: var(--ds-text-muted); }
.dark .mch__stat--muted .mch__stat-val { color: var(--ds-gray-500); }

/* Alert pulse ring */
.mch__stat-pulse {
  position: absolute;
  inset: -3px;
  border-radius: calc(var(--ds-radius-xl) + 3px);
  border: 2px solid var(--ds-danger);
  animation: mchStatPulse 1.5s ease-in-out infinite;
  pointer-events: none;
}

@keyframes mchStatPulse {
  0%, 100% { opacity: 0.6; transform: scale(1); }
  50% { opacity: 0; transform: scale(1.04); }
}

/* ── Right ─────────────────────────────────────────────────────────────────── */
.mch__right {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex-shrink: 0;
}

/* Countdown */
.mch__countdown {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  padding: 0.375rem 0.875rem;
  border-radius: var(--ds-radius-full);
  border: 1.5px solid var(--ds-border);
  background: var(--ds-gray-50);
  font-size: 0.8rem;
  font-weight: 700;
  color: var(--ds-text-secondary);
}

.dark .mch__countdown { background: var(--ds-gray-800); border-color: var(--ds-border-strong); }


.mch__countdown--urgent {
  background: var(--ds-danger-soft);
  border-color: rgba(220, 38, 38, 0.25);
  color: var(--ds-danger);
}

.mch__countdown--normal {
  background: var(--ds-primary-soft);
  border-color: var(--ds-primary-border);
  color: var(--ds-primary);
}

.mch__countdown-val { font-family: var(--ds-font-display); }

/* Connection toggle */
.mch__conn-toggle {
  display: flex;
  align-items: center;
  gap: 0.125rem;
  padding: 0.25rem;
  background: var(--ds-gray-100);
  border-radius: var(--ds-radius-full);
  border: 1.5px solid var(--ds-border);
}

.dark .mch__conn-toggle {
  background: var(--ds-gray-800);
  border-color: var(--ds-border-strong);
}

.mch__conn-btn {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  padding: 0.375rem 0.75rem;
  border-radius: var(--ds-radius-full);
  border: none;
  background: transparent;
  font-size: 0.75rem;
  font-weight: 700;
  color: var(--ds-text-muted);
  cursor: pointer;
  transition: color 0.15s ease, background-color 0.15s ease;
  white-space: nowrap;
}

.mch__conn-btn:hover {
  background: var(--ds-gray-200);
  color: var(--ds-text);
}

.dark .mch__conn-btn:hover { background: var(--ds-gray-700); }

.mch__conn-btn--active {
  background: var(--ds-primary);
  color: white;
  box-shadow: 0 2px 8px rgba(79, 70, 229, 0.25);
}

.mch__conn-btn--active:hover {
  background: var(--ds-primary);
  color: white;
}


/* Action buttons */
.mch__action-btn {
  position: relative;
  width: 2.5rem;
  height: 2.5rem;
  border-radius: var(--ds-radius-lg);
  border: 1.5px solid var(--ds-border);
  background: transparent;
  color: var(--ds-text-muted);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: color 0.15s ease, background-color 0.15s ease, border-color 0.15s ease;
}

.dark .mch__action-btn {
  background: var(--ds-gray-800);
  border-color: var(--ds-border-strong);
}

.mch__action-btn:hover {
  background: var(--ds-gray-100);
  color: var(--ds-text);
  border-color: var(--ds-gray-300);
}

.dark .mch__action-btn:hover {
  background: var(--ds-gray-700);
  border-color: var(--ds-gray-600);
}

.mch__action-btn--alert {
  color: var(--ds-warning);
  border-color: rgba(217, 119, 6, 0.3);
  background: rgba(217, 119, 6, 0.05);
}

.mch__action-btn--active-panel {
  color: var(--ds-primary);
  border-color: var(--ds-primary-border);
  background: var(--ds-primary-soft);
}

.dark .mch__action-btn--active-panel { background: rgba(79, 70, 229, 0.1); }

.mch__action-btn--active-panel:hover {
  background: var(--ds-primary);
  color: white;
  border-color: var(--ds-primary);
}

.dark .mch__action-btn--active-panel:hover { background: rgba(79, 70, 229, 0.2); }

.dark .mch__action-btn--alert { background: rgba(217, 119, 6, 0.1); }

.mch__action-btn--alert:hover {
  background: rgba(217, 119, 6, 0.1);
  color: var(--ds-warning);
  border-color: rgba(217, 119, 6, 0.4);
}

.mch__action-btn:disabled { opacity: 0.5; cursor: not-allowed; }

.mch__alert-badge {
  position: absolute;
  top: -5px;
  right: -5px;
  min-width: 18px;
  height: 18px;
  border-radius: 9px;
  background: var(--ds-danger);
  color: white;
  font-size: 0.6rem;
  font-weight: 800;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 4px;
  box-shadow: 0 2px 4px rgba(220, 38, 38, 0.3);
}

/* Spin */
.mch__spin { animation: mchSpin 1s linear infinite; }
@keyframes mchSpin { to { transform: rotate(360deg) translateZ(0); } }

/* ── Responsive ─────────────────────────────────────────────────────────────── */
@media (max-width: 900px) {
  .mch__stats { display: none; }
}

@media (max-width: 640px) {
  .mch__conn-toggle .hidden { display: none; }
  .mch__countdown { display: none; }
}

/* ── prefers-reduced-motion ── */
@media (prefers-reduced-motion: reduce) {
  .mch,
  .mch::before {
    transition: none;
  }

  .mch__back-btn,
  .mch__conn-btn,
  .mch__action-btn {
    transition: none;
  }

  .mch__stat {
    transition: none;
  }

  .mch__stat:hover {
    transform: none;
  }

  .mch__live-badge--live .mch__live-dot,
  .mch__stat-pulse {
    animation: none;
  }

  .mch__spin {
    animation: none;
  }
}
</style>
