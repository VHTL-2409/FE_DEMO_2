<template>
  <div class="mch" :class="{ 'mch--alerting': isAlerting }">
    <!-- Back + Title -->
    <div class="mch__left">
      <button type="button" class="mch__back-btn" @click="$emit('back')">
        <LucideIcon name="arrow_back" />
      </button>
      <div class="mch__title-group">
        <div class="mch__live-badge" :class="isConnected ? 'mch__live-badge--live' : 'mch__live-badge--off'">
          <span class="mch__live-dot" />
          <span class="mch__live-label">{{ isConnected ? 'LIVE' : 'KẾT NỐI' }}</span>
        </div>
        <div class="mch__title-text">
          <h1 class="mch__title">{{ examTitle }}</h1>
          <p v-if="classInfo" class="mch__subtitle">{{ classInfo }}</p>
        </div>
      </div>
    </div>

    <!-- Quick stats -->
    <div class="mch__stats">
      <div class="mch__stat" :class="{ 'mch__stat--success': onlineCount > 0 }">
        <div class="mch__stat-icon-wrap mch__stat-icon-wrap--success">
          <LucideIcon name="group" />
        </div>
        <div class="mch__stat-body">
          <p class="mch__stat-val">{{ onlineCount }}</p>
          <p class="mch__stat-label">Online / {{ totalCount }}</p>
        </div>
      </div>

      <div class="mch__stat" :class="{ 'mch__stat--danger': alertCount > 0 }">
        <div class="mch__stat-icon-wrap" :class="alertCount > 0 ? 'mch__stat-icon-wrap--danger' : 'mch__stat-icon-wrap--muted'">
          <LucideIcon name="warning" />
        </div>
        <div class="mch__stat-body">
          <p class="mch__stat-val">{{ alertCount }}</p>
          <p class="mch__stat-label">Cảnh báo</p>
        </div>
        <span v-if="alertCount > 0" class="mch__stat-pulse" />
      </div>
    </div>

    <!-- Right actions -->
    <div class="mch__right">
      <div v-if="countdownLabel" class="mch__countdown" :class="countdownClass">
        <LucideIcon name="timer" />
        <span class="mch__countdown-val">{{ countdownLabel }}</span>
      </div>

      <!-- Toggle alerts panel -->
      <button
        type="button"
        class="mch__action-btn"
        :class="{ 'mch__action-btn--alert': alertCount > 0 && !allPanelsCollapsed }"
        title="Bật/tắt panel cảnh báo"
        @click="$emit('toggle-alerts')"
      >
        <LucideIcon name="notifications" />
        <span v-if="alertCount > 0" class="mch__alert-badge">{{ alertCount > 99 ? '99+' : alertCount }}</span>
      </button>

      <!-- Toggle all panels -->
      <button
        type="button"
        class="mch__action-btn"
        :class="{ 'mch__action-btn--active-panel': allPanelsCollapsed }"
        title="Thu gọn / mở rộng panel"
        @click="$emit('toggle-all-panels')"
      >
        <LucideIcon :name="allPanelsCollapsed ? 'expand_more' : 'vertical_distribute'" />
      </button>

      <!-- Refresh -->
      <button
        type="button"
        class="mch__action-btn"
        :disabled="loading"
        title="Làm mới"
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
  examTitle: { type: String, default: 'Giám sát trực tiếp' },
  classInfo: { type: String, default: '' },
  onlineCount: { type: Number, default: 0 },
  alertCount: { type: Number, default: 0 },
  totalCount: { type: Number, default: 0 },
  isConnected: { type: Boolean, default: false },
  loading: { type: Boolean, default: false },
  endTime: { type: String, default: '' },
  allPanelsCollapsed: { type: Boolean, default: false }
})

defineEmits(['back', 'refresh', 'toggle-alerts', 'toggle-all-panels'])

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
  return hours ? 'mch__countdown--normal' : 'mch__countdown--urgent'
})

const isAlerting = ref(false)
let alertTimer = null

watch(() => props.alertCount, (newVal, oldVal) => {
  if (newVal > oldVal) {
    isAlerting.value = true
    clearTimeout(alertTimer)
    alertTimer = setTimeout(() => { isAlerting.value = false }, 2000)
  }
})
</script>

<style scoped>
.mch {
  display: flex;
  align-items: center;
  gap: 1.25rem;
  padding: 0.75rem 1.25rem;
  background: var(--ds-surface);
  border-bottom: 2px solid var(--ds-border);
  min-height: 72px;
  flex-wrap: wrap;
  position: relative;
  transition: background 0.3s ease;
}

.dark .mch { background: rgba(30, 41, 59, 0.98); border-bottom-color: var(--ds-border-strong); }

.mch::before {
  content: '';
  position: absolute;
  inset: 0;
  background: rgba(220, 38, 38, 0.03);
  opacity: 0;
  pointer-events: none;
  transition: opacity 0.3s ease;
}

.mch--alerting::before { animation: mchAlertFlash 2s ease-out forwards; }

@keyframes mchAlertFlash {
  0% { opacity: 1; } 20% { opacity: 0; } 40% { opacity: 0.6; } 100% { opacity: 0; }
}

.mch__left {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex-shrink: 0;
}

.mch__back-btn {
  width: 2.25rem;
  height: 2.25rem;
  border-radius: var(--ds-radius-lg);
  border: 1.5px solid var(--ds-border);
  background: transparent;
  color: var(--ds-text-muted);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: color 0.15s ease, background-color 0.15s ease;
}
.dark .mch__back-btn { background: var(--ds-gray-800); border-color: var(--ds-border-strong); }
.mch__back-btn:hover { background: var(--ds-gray-100); color: var(--ds-text); }

.mch__title-group {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.mch__live-badge {
  display: flex;
  align-items: center;
  gap: 0.3rem;
  padding: 0.2rem 0.625rem;
  border-radius: var(--ds-radius-full);
  border: 1.5px solid var(--ds-border);
  background: var(--ds-gray-100);
  font-size: 0.6rem;
  font-weight: 800;
  letter-spacing: 0.08em;
}
.dark .mch__live-badge { background: var(--ds-gray-800); border-color: var(--ds-border-strong); }

.mch__live-badge--live {
  background: rgba(22, 163, 74, 0.08);
  border-color: rgba(22, 163, 74, 0.25);
  color: var(--ds-success);
}

.mch__live-badge--off { color: var(--ds-text-muted); }

.mch__live-dot {
  width: 6px;
  height: 6px;
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
  50% { box-shadow: 0 0 0 5px rgba(22, 163, 74, 0); }
}

.mch__title-text { min-width: 0; }

.mch__title {
  font-family: var(--ds-font-display);
  font-size: 1rem;
  font-weight: 800;
  color: var(--ds-text);
  margin: 0;
  line-height: 1.2;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 240px;
}
.dark .mch__title { color: #f1f5f9; }

.mch__subtitle {
  font-size: 0.65rem;
  color: var(--ds-text-muted);
  margin: 0.2rem 0 0;
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 240px;
}

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
  gap: 0.5rem;
  padding: 0.375rem 0.875rem;
  background: var(--ds-gray-50);
  border: 1.5px solid var(--ds-border);
  border-radius: var(--ds-radius-xl);
  position: relative;
  transition: border-color 0.15s ease;
  min-width: 90px;
}
.dark .mch__stat { background: var(--ds-gray-800); border-color: var(--ds-border-strong); }

.mch__stat-icon-wrap {
  width: 32px;
  height: 32px;
  border-radius: var(--ds-radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.mch__stat-icon-wrap--success { background: var(--ds-success-soft); color: var(--ds-success); }
.mch__stat-icon-wrap--danger { background: var(--ds-danger-soft); color: var(--ds-danger); }
.mch__stat-icon-wrap--muted { background: var(--ds-gray-100); color: var(--ds-text-muted); }
.dark .mch__stat-icon-wrap--muted { background: var(--ds-gray-700); }

.mch__stat-body { display: flex; flex-direction: column; min-width: 0; }

.mch__stat-val {
  font-size: 1.1rem;
  font-weight: 900;
  color: var(--ds-text);
  margin: 0;
  line-height: 1;
  font-family: var(--ds-font-display);
}
.dark .mch__stat-val { color: #f1f5f9; }

.mch__stat-label {
  font-size: 0.58rem;
  color: var(--ds-text-muted);
  margin: 0.15rem 0 0;
  font-weight: 600;
  white-space: nowrap;
}

.mch__stat--success .mch__stat-val { color: var(--ds-success); }
.mch__stat--danger .mch__stat-val { color: var(--ds-danger); }

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

.mch__right {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex-shrink: 0;
}

.mch__countdown {
  display: flex;
  align-items: center;
  gap: 0.3rem;
  padding: 0.3rem 0.75rem;
  border-radius: var(--ds-radius-full);
  border: 1.5px solid var(--ds-border);
  background: var(--ds-gray-50);
  font-size: 0.75rem;
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

.mch__action-btn {
  position: relative;
  width: 2.25rem;
  height: 2.25rem;
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
.dark .mch__action-btn { background: var(--ds-gray-800); border-color: var(--ds-border-strong); }
.mch__action-btn:hover { background: var(--ds-gray-100); color: var(--ds-text); border-color: var(--ds-gray-300); }
.dark .mch__action-btn:hover { background: var(--ds-gray-700); border-color: var(--ds-gray-600); }
.mch__action-btn:disabled { opacity: 0.4; cursor: not-allowed; }

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

.mch__alert-badge {
  position: absolute;
  top: -4px;
  right: -4px;
  min-width: 16px;
  height: 16px;
  padding: 0 3px;
  border-radius: 999px;
  background: var(--ds-danger);
  color: white;
  font-size: 0.55rem;
  font-weight: 900;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
