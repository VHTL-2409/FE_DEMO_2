<template>
  <div class="elh">
    <!-- Top bar: sync status + support -->
    <div class="elh__topbar">
      <div class="elh__sync-badge" :class="isSyncing ? 'elh__sync-badge--syncing' : 'elh__sync-badge--ok'">
        <span class="elh__sync-dot" :class="isSyncing ? 'elh__sync-dot--syncing' : 'elh__sync-dot--ok'" />
        <span class="elh__sync-text">{{ isSyncing ? 'Đang đồng bộ...' : 'Kết nối ổn định' }}</span>
        <span v-if="lastSyncedLabel" class="elh__sync-time">{{ lastSyncedLabel }}</span>
      </div>

      <button type="button" class="elh__support-btn" @click="$emit('support')">
        <LucideIcon name="support_agent" />
        <span>Hỗ trợ</span>
      </button>
    </div>

    <!-- Hero card with animated mesh gradient -->
    <div class="elh__hero">
      <!-- Animated mesh background -->
      <div class="elh__hero-bg">
        <div class="elh__mesh elh__mesh--1" />
        <div class="elh__mesh elh__mesh--2" />
        <div class="elh__mesh elh__mesh--3" />
      </div>

      <!-- Hero content -->
      <div class="elh__hero-content">
        <!-- Status + Circular countdown -->
        <div class="elh__hero-top">
          <StatusChip :status="statusKey" size="md" />

          <!-- Circular countdown -->
          <div v-if="countdownData && !isEnded" class="elh__circular-wrap" :class="urgencyClass">
            <svg class="elh__circular-svg" viewBox="0 0 60 60" width="56" height="56">
              <circle class="elh__circular-bg" cx="30" cy="30" r="26" />
              <circle
                class="elh__circular-progress"
                cx="30" cy="30" r="26"
                :stroke-dasharray="circumference"
                :stroke-dashoffset="progressOffset"
                :class="urgencyClass"
              />
            </svg>
            <div class="elh__circular-inner">
              <span class="elh__circular-time">{{ countdownData.hours }}:{{ countdownData.minutes }}:{{ countdownData.seconds }}</span>
            </div>
          </div>

          <div v-else-if="isEnded" class="elh__ended-badge">
            <LucideIcon name="event_busy" />
            <span>Đã kết thúc</span>
          </div>
          <div v-else class="elh__live-badge">
            <span class="elh__live-dot" />
            <span>Sẵn sàng!</span>
          </div>
        </div>

        <!-- Exam title -->
        <h1 class="elh__title">{{ examTitle }}</h1>

        <!-- Glassmorphism meta grid -->
        <div class="elh__meta-grid">
          <div v-if="startAtDisplay" class="elh__meta-item elh__meta-item--glass">
            <div class="elh__meta-icon-wrap elh__meta-icon-wrap--time">
              <LucideIcon name="schedule" />
            </div>
            <div class="elh__meta-text">
              <span class="elh__meta-val">{{ startAtDisplay }}</span>
              <span class="elh__meta-lbl">Bắt đầu lúc</span>
            </div>
          </div>

          <div class="elh__meta-item elh__meta-item--glass">
            <div class="elh__meta-icon-wrap elh__meta-icon-wrap--duration">
              <LucideIcon name="timer" />
            </div>
            <div class="elh__meta-text">
              <span class="elh__meta-val">{{ duration }} phút</span>
              <span class="elh__meta-lbl">Thời gian làm bài</span>
            </div>
          </div>

          <div v-if="questionCount > 0" class="elh__meta-item elh__meta-item--glass">
            <div class="elh__meta-icon-wrap elh__meta-icon-wrap--question">
              <LucideIcon name="quiz" />
            </div>
            <div class="elh__meta-text">
              <span class="elh__meta-val">{{ questionCount }} câu</span>
              <span class="elh__meta-lbl">Tổng câu hỏi</span>
            </div>
          </div>

          <div v-if="examCode && examCode !== '-'" class="elh__meta-item elh__meta-item--glass">
            <div class="elh__meta-icon-wrap elh__meta-icon-wrap--code">
              <LucideIcon name="tag" />
            </div>
            <div class="elh__meta-text">
              <span class="elh__meta-val elh__meta-val--mono">{{ examCode }}</span>
              <span class="elh__meta-lbl">Mã bài thi</span>
            </div>
          </div>
        </div>

        <!-- Proctoring badge -->
        <div v-if="requireCameraMic" class="elh__proctor-badge">
          <div class="elh__proctor-icon">
            <LucideIcon name="videocam" />
          </div>
          <div class="elh__proctor-text">
            <span class="elh__proctor-title">Giám sát bằng camera</span>
            <span class="elh__proctor-desc">Camera sẽ bật trong suốt buổi thi</span>
          </div>
          <div class="elh__proctor-dot elh__proctor-dot--active" />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import StatusChip from '../../ui/StatusChip.vue'
import { parseBackendDate } from '../../../utils/dateUtils.js'

const props = defineProps({
  examTitle: { type: String, default: 'Bài thi' },
  examCode: { type: String, default: '' },
  duration: { type: [Number, String], default: 60 },
  questionCount: { type: [Number, String], default: 0 },
  startAt: { type: [Date, String], default: null },
  endAt: { type: [Date, String], default: null },
  nowMs: { type: Number, default: () => Date.now() },
  isSyncing: { type: Boolean, default: false },
  lastSyncedLabel: { type: String, default: '' },
  requireCameraMic: { type: Boolean, default: false }
})

defineEmits(['support'])

const statusKey = computed(() => {
  if (!props.startAt) return 'upcoming'
  const startMs = new Date(props.startAt).getTime()
  const endMs = props.endAt ? new Date(props.endAt).getTime() : null
  const now = props.nowMs
  if (endMs && now > endMs) return 'ended'
  if (!Number.isNaN(startMs) && now >= startMs) {
    if (endMs && now <= endMs) return 'live'
    if (!endMs) return 'live'
  }
  return 'upcoming'
})

const isEnded = computed(() => {
  if (!props.endAt) return false
  return props.nowMs > new Date(props.endAt).getTime()
})

const countdownData = computed(() => {
  if (!props.startAt || isEnded.value) return null
  const startMs = new Date(props.startAt).getTime()
  if (Number.isNaN(startMs)) return null
  const diff = startMs - props.nowMs
  if (diff <= 0) return null
  const totalSeconds = Math.floor(diff / 1000)
  const hours = Math.floor(totalSeconds / 3600)
  const minutes = Math.floor((totalSeconds % 3600) / 60)
  const seconds = totalSeconds % 60
  return {
    hours: String(hours).padStart(2, '0'),
    minutes: String(minutes).padStart(2, '0'),
    seconds: String(seconds).padStart(2, '0')
  }
})

const urgencyClass = computed(() => {
  if (!props.startAt) return ''
  const startMs = new Date(props.startAt).getTime()
  if (Number.isNaN(startMs)) return ''
  const hoursLeft = (startMs - props.nowMs) / 1000 / 60 / 60
  if (hoursLeft <= 1) return 'elh--critical'
  if (hoursLeft <= 6) return 'elh--urgent'
  return 'elh--soon'
})

// Circular countdown
const circumference = 2 * Math.PI * 26 // r=26
const progressOffset = computed(() => {
  if (!props.startAt) return 0
  const startMs = new Date(props.startAt).getTime()
  if (Number.isNaN(startMs)) return 0
  const totalHours = 24 // assume max 24h countdown
  const diff = startMs - props.nowMs
  const ratio = Math.max(0, Math.min(1, diff / (totalHours * 60 * 60 * 1000)))
  return circumference * (1 - ratio)
})

const startAtDisplay = computed(() => {
  if (!props.startAt) return null
  const d = parseBackendDate(props.startAt)
  if (!d) return null
  return d.toLocaleString('vi-VN', {
    timeZone: 'Asia/Ho_Chi_Minh',
    weekday: 'short', day: '2-digit', month: '2-digit',
    hour: '2-digit', minute: '2-digit'
  })
})
</script>

<style scoped>
.elh {
  display: flex;
  flex-direction: column;
  gap: 0.875rem;
}

/* ── Top bar ──────────────────────────────────────── */
.elh__topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
}

.elh__sync-badge {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.375rem 0.875rem;
  border-radius: var(--ds-radius-full);
  background: var(--ds-surface);
  border: 1px solid var(--ds-border);
  transition: color 0.2s ease, background-color 0.2s ease, border-color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease;
}

.dark .elh__sync-badge { border-color: var(--ds-border-strong); }

.elh__sync-badge--ok {
  border-color: rgba(22, 163, 74, 0.2);
  background: rgba(22, 163, 74, 0.04);
}

.dark .elh__sync-badge--ok { background: rgba(22, 163, 74, 0.08); }

.elh__sync-badge--syncing {
  border-color: rgba(245, 158, 11, 0.2);
  background: rgba(245, 158, 11, 0.04);
}

.dark .elh__sync-badge--syncing { background: rgba(245, 158, 11, 0.08); }

.elh__sync-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}

.elh__sync-dot--ok {
  background: var(--ds-success);
  box-shadow: 0 0 0 2px rgba(22, 163, 74, 0.2);
  animation: elhPulse 2s ease-in-out infinite;
}

.elh__sync-dot--syncing {
  background: var(--ds-warning);
  animation: elhPulse 1s ease-in-out infinite;
}

@keyframes elhPulse {
  0%, 100% { opacity: 1; box-shadow: 0 0 0 2px rgba(22, 163, 74, 0.2); }
  50% { opacity: 0.6; box-shadow: 0 0 0 4px rgba(22, 163, 74, 0.1); }
}

.elh__sync-text {
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--ds-text-muted);
}

.elh__sync-badge--ok .elh__sync-text { color: var(--ds-success); }
.elh__sync-badge--syncing .elh__sync-text { color: var(--ds-warning); }

.elh__sync-time {
  font-size: 0.7rem;
  color: var(--ds-text-muted);
  opacity: 0.7;
  padding-left: 0.25rem;
  border-left: 1px solid var(--ds-border);
}

.elh__support-btn {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  font-size: 0.8rem;
  font-weight: 600;
  color: var(--ds-text-muted);
  background: var(--ds-surface);
  border: 1px solid var(--ds-border);
  cursor: pointer;
  padding: 0.375rem 0.875rem;
  border-radius: var(--ds-radius-full);
  transition: color 0.15s ease, background-color 0.15s ease, border-color 0.15s ease, box-shadow 0.15s ease, transform 0.15s ease;
  font-family: inherit;
}

.dark .elh__support-btn { border-color: var(--ds-border-strong); }

.elh__support-btn:hover {
  color: var(--ds-primary);
  background: var(--ds-primary-soft);
  border-color: var(--ds-primary-border);
  transform: translateY(-1px);
}

/* ── Hero card ─────────────────────────────────────── */
.elh__hero {
  position: relative;
  border-radius: var(--ds-radius-2xl);
  overflow: hidden;
  border: 1.5px solid var(--ds-border);
  box-shadow: var(--ds-shadow-md);
}

.dark .elh__hero { border-color: rgba(79, 70, 229, 0.15); }

/* Animated mesh background */
.elh__hero-bg {
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, #f8faff 0%, #ede9fe 40%, #e0f2fe 70%, #fce7f3 100%);
  overflow: hidden;
}

.dark .elh__hero-bg {
  background: linear-gradient(135deg, #1e293b 0%, #1e1b4b 40%, #1c2a3a 70%, #2d1b3d 100%);
}

.elh__mesh {
  position: absolute;
  border-radius: 50%;
  opacity: 0.16;
  animation: elhMeshFloat 8s ease-in-out infinite;
}

.dark .elh__mesh { opacity: 0.2; }

.elh__mesh--1 {
  width: 300px;
  height: 300px;
  background: radial-gradient(circle, rgba(79, 70, 229, 0.2) 0%, rgba(79, 70, 229, 0) 72%);
  top: -80px;
  right: -60px;
  animation-delay: 0s;
}

.elh__mesh--2 {
  width: 250px;
  height: 250px;
  background: radial-gradient(circle, rgba(14, 165, 233, 0.18) 0%, rgba(14, 165, 233, 0) 72%);
  bottom: -60px;
  left: 20%;
  animation-delay: -3s;
}

.elh__mesh--3 {
  width: 200px;
  height: 200px;
  background: rgba(236, 72, 153, 0.2);
  top: 30%;
  right: 25%;
  animation-delay: -6s;
}

@keyframes elhMeshFloat {
  0%, 100% { transform: translate(0, 0) scale(1); }
  33% { transform: translate(15px, -20px) scale(1.05); }
  66% { transform: translate(-10px, 15px) scale(0.95); }
}

.elh__hero-content {
  position: relative;
  padding: 1.5rem 1.75rem 1.75rem;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

/* Hero top */
.elh__hero-top {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex-wrap: wrap;
}

/* Circular countdown */
.elh__circular-wrap {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-left: auto;
  transition: transform 0.3s ease;
}

.elh__circular-wrap:hover { transform: scale(1.05); }

.elh__circular-svg {
  transform: rotate(-90deg);
}

.elh__circular-bg {
  fill: none;
  stroke: var(--ds-border);
  stroke-width: 3;
}

.dark .elh__circular-bg { stroke: rgba(79, 70, 229, 0.2); }

.elh__circular-progress {
  fill: none;
  stroke-width: 3;
  stroke-linecap: round;
  transition: stroke-dashoffset 1s linear, stroke 0.5s ease;
}

.elh--soon .elh__circular-progress { stroke: var(--ds-primary); }
.elh--urgent .elh__circular-progress { stroke: var(--ds-warning); }
.elh--critical .elh__circular-progress {
  stroke: var(--ds-danger);
  animation: elhCriticalPulse 1s ease-in-out infinite;
}

@keyframes elhCriticalPulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

.elh__circular-inner {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.elh__circular-time {
  font-family: var(--ds-font-display);
  font-size: 0.65rem;
  font-weight: 900;
  letter-spacing: 0.02em;
  color: var(--ds-text);
}

.elh--urgent .elh__circular-time { color: var(--ds-warning); }
.elh--critical .elh__circular-time { color: var(--ds-danger); }

.dark .elh__circular-time { color: var(--ds-text); }

/* Ended badge */
.elh__ended-badge {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  padding: 0.4rem 0.875rem;
  border-radius: var(--ds-radius-full);
  background: var(--ds-gray-100);
  border: 1px solid var(--ds-border);
  color: var(--ds-text-muted);
  font-size: 0.8rem;
  font-weight: 700;
  margin-left: auto;
}

.dark .elh__ended-badge { background: var(--ds-gray-800); border-color: var(--ds-border-strong); }

/* Live badge */
.elh__live-badge {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.4rem 0.875rem;
  border-radius: var(--ds-radius-full);
  background: rgba(22, 163, 74, 0.08);
  border: 1px solid rgba(22, 163, 74, 0.25);
  color: var(--ds-success);
  font-size: 0.8rem;
  font-weight: 700;
  margin-left: auto;
}

.dark .elh__live-badge { background: rgba(22, 163, 74, 0.12); }

.elh__live-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--ds-success);
  animation: elhLivePulse 1.5s ease-in-out infinite;
}

@keyframes elhLivePulse {
  0%, 100% { transform: scale(1); opacity: 1; }
  50% { transform: scale(1.3); opacity: 0.7; }
}

/* Title */
.elh__title {
  font-family: var(--ds-font-display);
  font-size: clamp(1.25rem, 3vw, 1.75rem);
  font-weight: 900;
  color: var(--ds-text);
  margin: 0;
  letter-spacing: -0.02em;
  line-height: 1.2;
}

.dark .elh__title { color: var(--ds-text); }

/* Meta grid with glassmorphism */
.elh__meta-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
  gap: 0.625rem;
}

.elh__meta-item {
  display: flex;
  align-items: center;
  gap: 0.625rem;
  padding: 0.75rem;
  border-radius: var(--ds-radius-xl);
  background: rgba(255, 255, 255, 0.94);
  border: 1px solid rgba(226, 232, 240, 0.9);
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.dark .elh__meta-item {
  background: rgba(30, 41, 59, 0.7);
  border-color: rgba(79, 70, 229, 0.1);
}

.elh__meta-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.dark .elh__meta-item:hover { box-shadow: 0 4px 16px rgba(0, 0, 0, 0.3); }

.elh__meta-icon-wrap {
  width: 38px;
  height: 38px;
  border-radius: var(--ds-radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.elh__meta-icon-wrap--time { background: rgba(99, 102, 241, 0.1); color: var(--ds-primary); }
.elh__meta-icon-wrap--duration { background: rgba(14, 165, 233, 0.1); color: var(--ds-info); }
.elh__meta-icon-wrap--question { background: rgba(16, 185, 129, 0.1); color: var(--ds-success); }
.elh__meta-icon-wrap--code { background: var(--ds-warning-soft); color: var(--ds-warning); }

.dark .elh__meta-icon-wrap--time { background: rgba(99, 102, 241, 0.15); }
.dark .elh__meta-icon-wrap--duration { background: rgba(14, 165, 233, 0.15); }
.dark .elh__meta-icon-wrap--question { background: rgba(16, 185, 129, 0.15); }
.dark .elh__meta-icon-wrap--code { background: rgba(245, 158, 11, 0.15); }

.elh__meta-text {
  display: flex;
  flex-direction: column;
  gap: 0.125rem;
  min-width: 0;
}

.elh__meta-val {
  font-family: var(--ds-font-display);
  font-size: 0.875rem;
  font-weight: 800;
  color: var(--ds-text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.dark .elh__meta-val { color: var(--ds-text); }

.elh__meta-val--mono {
  font-family: 'Courier New', monospace;
  font-size: 0.9rem;
  letter-spacing: 0.08em;
  color: var(--ds-warning);
}

.dark .elh__meta-val--mono { color: var(--ds-accent); }

.elh__meta-lbl {
  font-size: 0.6rem;
  font-weight: 700;
  color: var(--ds-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.05em;
  white-space: nowrap;
}

/* Proctoring badge */
.elh__proctor-badge {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.625rem 1rem;
  border-radius: var(--ds-radius-xl);
  background: rgba(14, 165, 233, 0.06);
  border: 1px solid rgba(14, 165, 233, 0.2);
  animation: elhProctorIn 0.4s ease-out both;
}

@keyframes elhProctorIn {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
}

.dark .elh__proctor-badge { background: rgba(14, 165, 233, 0.1); }

.elh__proctor-icon {
  width: 36px;
  height: 36px;
  border-radius: var(--ds-radius-lg);
  background: rgba(14, 165, 233, 0.12);
  color: var(--ds-info);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  animation: elhCamPulse 3s ease-in-out infinite;
}

@keyframes elhCamPulse {
  0%, 100% { box-shadow: 0 0 0 0 rgba(14, 165, 233, 0.3); }
  50% { box-shadow: 0 0 0 6px rgba(14, 165, 233, 0); }
}

.dark .elh__proctor-icon { background: rgba(14, 165, 233, 0.15); }

.elh__proctor-text {
  display: flex;
  flex-direction: column;
  gap: 0.1rem;
  flex: 1;
}

.elh__proctor-title {
  font-size: 0.8rem;
  font-weight: 800;
  color: var(--ds-info);
}

.dark .elh__proctor-title { color: var(--ds-info); }

.elh__proctor-desc {
  font-size: 0.7rem;
  font-weight: 500;
  color: var(--ds-text-muted);
  opacity: 0.8;
}

.elh__proctor-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}

.elh__proctor-dot--active {
  background: var(--ds-info);
  box-shadow: 0 0 0 2px rgba(14, 165, 233, 0.2);
  animation: elhProctorDot 2s ease-in-out infinite;
}

@keyframes elhProctorDot {
  0%, 100% { box-shadow: 0 0 0 2px rgba(14, 165, 233, 0.2); }
  50% { box-shadow: 0 0 0 4px rgba(14, 165, 233, 0.1); }
}

/* ── Responsive ─────────────────────────────────────── */
@media (max-width: 480px) {
  .elh__hero-content { padding: 1.125rem; }
  .elh__meta-grid { grid-template-columns: 1fr 1fr; }
  .elh__circular-wrap { display: none; }
}

@media (max-width: 360px) {
  .elh__meta-grid { grid-template-columns: 1fr; }
}
</style>
@media (prefers-reduced-motion: reduce) {
  * {
    transition-duration: 0.01ms !important;
    animation-duration: 0.01ms !important;
  }
}