<template>
  <div class="ehead">
    <!-- Left: exam info -->
    <div class="ehead__left">
      <div class="ehead__exam-icon">
        <LucideIcon name="quiz" />
      </div>
      <div class="ehead__exam-info">
        <p class="ehead__exam-title">{{ examTitle }}</p>
        <p class="ehead__exam-meta">
          <span class="ehead__question-badge">
            Câu {{ currentIndex + 1 }} / {{ totalQuestions }}
          </span>
        </p>
      </div>
    </div>

    <!-- Center: timer (always visible, prominent) -->
    <div class="ehead__center">
      <div class="ehead__timer-wrap" :class="timerWrapClass">
        <!-- Circular ring -->
        <svg class="ehead__ring" viewBox="0 0 60 60" aria-hidden="true">
          <circle class="ehead__ring-track" cx="30" cy="30" r="26" />
          <circle
            class="ehead__ring-fill"
            cx="30" cy="30" r="26"
            :stroke-dasharray="ringDasharray"
            :stroke-dashoffset="ringDashoffset"
            :class="ringClass"
          />
        </svg>
        <!-- Time display -->
        <div class="ehead__timer-inner">
          <span class="ehead__timer-main" :class="timerMainClass">
            {{ timerHours }}:{{ timerMinutes }}
          </span>
          <span class="ehead__timer-sec" :class="timerSecClass">{{ timerSeconds }}s</span>
        </div>
      </div>
      <!-- Time label -->
      <div class="ehead__timer-label">
        <span v-if="timeWarning" class="ehead__timer-label-text" :class="timeWarningClass">
          {{ timeWarning }}
        </span>
        <span v-else class="ehead__timer-label-text">Còn lại</span>
      </div>
    </div>

    <!-- Right: device status + save + actions -->
    <div class="ehead__right">
      <!-- Device status badges -->
      <div v-if="showDevices" class="ehead__device-badges">
        <div class="ehead__badge" :class="cameraReady ? 'ehead__badge--ok' : 'ehead__badge--fail'">
          <LucideIcon :name="cameraReady ? 'videocam' : 'videocam_off'" />
          <span>Camera</span>
        </div>
        <div class="ehead__badge" :class="micReady ? 'ehead__badge--ok' : 'ehead__badge--fail'">
          <LucideIcon :name="micReady ? 'mic' : 'mic_off'" />
          <span>Mic</span>
        </div>
        <div class="ehead__badge ehead__badge--ok">
          <LucideIcon name="wifi" />
          <span>Kết nối</span>
        </div>
      </div>

      <!-- Save status -->
      <div v-if="saveStatusLabel" class="ehead__save" role="status" aria-live="polite">
        <LucideIcon name="progress_activity" v-if="saveStatus === 'saving'" class="ehead__save-icon ehead__save-icon--spin"/>
        <LucideIcon name="check_circle" v-else-if="saveStatus === 'saved'" class="ehead__save-icon ehead__save-icon--ok"/>
        <LucideIcon name="schedule" v-else-if="hasPendingChanges" class="ehead__save-icon ehead__save-icon--pending"/>
        <LucideIcon name="cloud_off" v-else-if="saveStatus === 'error'" class="ehead__save-icon ehead__save-icon--fail"/>
        <span class="ehead__save-text">{{ saveStatusLabel }}</span>
      </div>

      <!-- Manual save -->
      <button
        type="button"
        class="ehead__save-btn"
        :disabled="isSuspended || saveStatus === 'saving'"
        @click="$emit('manual-save')"
      >
        <LucideIcon name="save" />
        <span>Lưu</span>
      </button>

      <!-- Submit -->
      <button
        type="button"
        class="ehead__submit-btn"
        :class="{ 'ehead__submit-btn--critical': isTimeCritical }"
        :disabled="isSuspended"
        @click="$emit('open-submit')"
      >
        <LucideIcon name="done_all" />
        <span>Nộp bài</span>
      </button>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  examTitle: { type: String, default: 'Bài thi' },
  currentIndex: { type: Number, default: 0 },
  totalQuestions: { type: Number, default: 0 },
  remainingSeconds: { type: Number, default: 0 },
  initialRemainingForProgress: { type: Number, default: 0 },
  timerHours: { type: String, default: '00' },
  timerMinutes: { type: String, default: '00' },
  timerSeconds: { type: String, default: '00' },
  saveStatus: { type: String, default: 'idle' },
  saveStatusLabel: { type: String, default: '' },
  hasPendingChanges: { type: Boolean, default: false },
  isSuspended: { type: Boolean, default: false },
  cameraReady: { type: Boolean, default: false },
  micReady: { type: Boolean, default: false },
  showDevices: { type: Boolean, default: false }
})

defineEmits(['manual-save', 'open-submit'])

// ─── Time ratio ─────────────────────────────────────────────────────────────────
const prefersReducedMotion = () =>
  typeof window !== 'undefined' && window.matchMedia?.('(prefers-reduced-motion: reduce)').matches

const timeRatio = computed(() => {
  const max = Math.max(props.initialRemainingForProgress, 1)
  return props.remainingSeconds / max
})

const isTimeWarning = computed(() => timeRatio.value <= 0.5 && timeRatio.value >= 0.2)
const isTimeCritical = computed(() => timeRatio.value < 0.2)

// ─── Timer display class ─────────────────────────────────────────────────────────
const timerWrapClass = computed(() => ({
  'ehead__timer-wrap--warning': isTimeWarning.value,
  'ehead__timer-wrap--critical': isTimeCritical.value && !prefersReducedMotion()
}))

const timerMainClass = computed(() => ({
  'ehead__timer-main--warning': isTimeWarning.value,
  'ehead__timer-main--critical': isTimeCritical.value
}))

const timerSecClass = computed(() => ({
  'ehead__timer-sec--warning': isTimeWarning.value,
  'ehead__timer-sec--critical': isTimeCritical.value
}))

const timeWarning = computed(() => {
  if (isTimeCritical.value) return 'Sắp hết giờ!'
  if (isTimeWarning.value) return 'Còn ít thời gian'
  return null
})

const timeWarningClass = computed(() => ({
  'ehead__timer-label-text--warning': isTimeWarning.value,
  'ehead__timer-label-text--critical': isTimeCritical.value
}))

// ─── Ring SVG ──────────────────────────────────────────────────────────────────
const CIRCUMFERENCE = 2 * Math.PI * 26 // r=26

const ringDasharray = computed(() => `${CIRCUMFERENCE}`)
const ringDashoffset = computed(() => {
  const ratio = timeRatio.value
  return CIRCUMFERENCE * (1 - Math.min(1, Math.max(0, ratio)))
})

const ringClass = computed(() => ({
  'ehead__ring-fill--warning': isTimeWarning.value,
  'ehead__ring-fill--critical': isTimeCritical.value && !prefersReducedMotion()
}))
</script>

<style scoped>
.ehead {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 0.75rem 1.25rem;
  background: var(--ds-surface);
  border-bottom: 2px solid var(--ds-border);
  position: sticky;
  top: 0;
  z-index: 50;
  flex-wrap: wrap;
  min-height: 64px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.06);
}

.dark .ehead { border-bottom-color: var(--ds-border-strong); }

/* ─── Left ─────────────────────────────────────────────────────────────── */
.ehead__left {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex-shrink: 0;
}

.ehead__exam-icon {
  width: 40px;
  height: 40px;
  border-radius: var(--ds-radius-lg);
  background: linear-gradient(135deg, var(--ds-primary) 0%, #6366f1 100%);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  box-shadow: 0 4px 12px rgba(79, 70, 229, 0.3);
}

.ehead__exam-info { min-width: 0; }

.ehead__exam-title {
  font-size: 0.9rem;
  font-weight: 800;
  color: var(--ds-text);
  margin: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 180px;
}

.dark .ehead__exam-title { color: #f1f5f9; }

.ehead__exam-meta {
  margin: 0.125rem 0 0;
}

.ehead__question-badge {
  display: inline-block;
  padding: 0.125rem 0.5rem;
  background: var(--ds-primary-soft);
  color: var(--ds-primary);
  border-radius: var(--ds-radius-full);
  font-size: 0.65rem;
  font-weight: 800;
  letter-spacing: 0.02em;
}

/* ─── Center: Timer ───────────────────────────────────────────────────── */
.ehead__center {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.125rem;
  flex-shrink: 0;
}

.ehead__timer-wrap {
  position: relative;
  width: 60px;
  height: 60px;
  flex-shrink: 0;
  transition: color 0.3s ease, background-color 0.3s ease, border-color 0.3s ease, box-shadow 0.3s ease, transform 0.3s ease;
}

.ehead__timer-wrap--warning {
  filter: drop-shadow(0 0 6px rgba(234, 179, 8, 0.4));
}

.ehead__timer-wrap--critical {
  animation: eheadTimerPulse 1.5s ease-in-out infinite;
}

@keyframes eheadTimerPulse {
  0%, 100% { filter: drop-shadow(0 0 6px rgba(220, 38, 38, 0.4)); }
  50% { filter: drop-shadow(0 0 14px rgba(220, 38, 38, 0.7)); }
}

.ehead__ring {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  transform: rotate(-90deg);
}

.ehead__ring-track {
  fill: none;
  stroke: var(--ds-gray-200);
  stroke-width: 4;
}

.dark .ehead__ring-track { stroke: var(--ds-gray-700); }

.ehead__ring-fill {
  fill: none;
  stroke: var(--ds-primary);
  stroke-width: 4;
  stroke-linecap: round;
  transition: stroke-dashoffset 1s linear, stroke 0.3s ease;
}

.ehead__ring-fill--warning { stroke: var(--ds-warning); }
.ehead__ring-fill--critical { stroke: var(--ds-danger); }

.ehead__timer-inner {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 0;
}

.ehead__timer-main {
  font-family: var(--ds-font-display);
  font-size: 0.85rem;
  font-weight: 900;
  color: var(--ds-text);
  line-height: 1;
  font-variant-numeric: tabular-nums;
  letter-spacing: -0.01em;
}

.dark .ehead__timer-main { color: #f1f5f9; }

.ehead__timer-main--warning { color: var(--ds-warning); }
.ehead__timer-main--critical { color: var(--ds-danger); }

.ehead__timer-sec {
  font-size: 0.6rem;
  font-weight: 700;
  color: var(--ds-text-muted);
  line-height: 1;
  font-variant-numeric: tabular-nums;
}

.ehead__timer-sec--warning { color: var(--ds-warning); opacity: 0.8; }
.ehead__timer-sec--critical { color: var(--ds-danger); opacity: 0.9; }

.ehead__timer-label {
  display: flex;
  align-items: center;
  justify-content: center;
}

.ehead__timer-label-text {
  font-size: 0.6rem;
  font-weight: 700;
  color: var(--ds-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.05em;
  white-space: nowrap;
}

.ehead__timer-label-text--warning { color: var(--ds-warning); }
.ehead__timer-label-text--critical { color: var(--ds-danger); animation: eheadBlink 1.5s ease-in-out infinite; }

/* ─── Right ───────────────────────────────────────────────────────────── */
.ehead__right {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-left: auto;
  flex-wrap: wrap;
}

/* Device badges */
.ehead__device-badges {
  display: flex;
  align-items: center;
  gap: 0.375rem;
}

.ehead__badge {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  padding: 0.25rem 0.625rem;
  border-radius: var(--ds-radius-full);
  font-size: 0.65rem;
  font-weight: 800;
  border: 1.5px solid;
  letter-spacing: 0.02em;
}

.ehead__badge--ok {
  background: var(--ds-success-soft);
  border-color: rgba(22, 163, 74, 0.3);
  color: var(--ds-success);
}

.ehead__badge--fail {
  background: var(--ds-danger-soft);
  border-color: rgba(220, 38, 38, 0.3);
  color: var(--ds-danger);
  animation: eheadBlink 2s ease-in-out infinite;
}

/* Save status */
.ehead__save {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  padding: 0.25rem 0.625rem;
  border-radius: var(--ds-radius-full);
  font-size: 0.65rem;
  font-weight: 700;
  background: var(--ds-gray-50);
  border: 1px solid var(--ds-border);
  color: var(--ds-text-muted);
}

.dark .ehead__save { background: var(--ds-gray-800); border-color: var(--ds-border-strong); }

.ehead__save-icon { font-size: 0.75rem; }
.ehead__save-icon--spin { animation: eheadSpin 1s linear infinite; }
.ehead__save-icon--ok { color: var(--ds-success); }
.ehead__save-icon--pending { color: var(--ds-warning); }
.ehead__save-icon--fail { color: var(--ds-danger); }

.ehead__save-text { white-space: nowrap; }

/* Save button */
.ehead__save-btn {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  padding: 0.375rem 0.875rem;
  border-radius: var(--ds-radius-xl);
  font-size: 0.75rem;
  font-weight: 700;
  background: var(--ds-surface);
  border: 1.5px solid var(--ds-border);
  color: var(--ds-text-secondary);
  cursor: pointer;
  transition: color 0.15s ease, background-color 0.15s ease, border-color 0.15s ease, box-shadow 0.15s ease, transform 0.15s ease;
  font-family: inherit;
  white-space: nowrap;
  letter-spacing: 0.01em;
}

.dark .ehead__save-btn { border-color: var(--ds-border-strong); color: var(--ds-gray-300); }

.ehead__save-btn:hover:not(:disabled) {
  border-color: var(--ds-primary-border);
  color: var(--ds-primary);
  background: var(--ds-primary-soft);
}

.ehead__save-btn:disabled { opacity: 0.5; cursor: not-allowed; }

/* Submit button — most prominent */
.ehead__submit-btn {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 1.25rem;
  border-radius: var(--ds-radius-xl);
  font-size: 0.875rem;
  font-weight: 900;
  background: linear-gradient(135deg, var(--ds-success) 0%, #059669 100%);
  border: none;
  color: white;
  cursor: pointer;
  transition: color 0.15s ease, background-color 0.15s ease, border-color 0.15s ease, box-shadow 0.15s ease, transform 0.15s ease;
  font-family: inherit;
  white-space: nowrap;
  box-shadow: 0 4px 12px rgba(22, 163, 74, 0.3);
  letter-spacing: 0.02em;
}

.ehead__submit-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(22, 163, 74, 0.4);
  background: linear-gradient(135deg, #16a34a 0%, #15803d 100%);
}

.ehead__submit-btn:active:not(:disabled) {
  transform: translateY(0);
}

.ehead__submit-btn--critical {
  background: linear-gradient(135deg, var(--ds-danger) 0%, #b91c1c 100%);
  box-shadow: 0 4px 12px rgba(220, 38, 38, 0.35);
  animation: eheadSubmitPulse 1.5s ease-in-out infinite;
}

.ehead__submit-btn--critical:hover:not(:disabled) {
  box-shadow: 0 6px 20px rgba(220, 38, 38, 0.5);
  background: linear-gradient(135deg, #dc2626 0%, #991b1b 100%);
}

@keyframes eheadSubmitPulse {
  0%, 100% { box-shadow: 0 4px 12px rgba(220, 38, 38, 0.35); }
  50% { box-shadow: 0 4px 20px rgba(220, 38, 38, 0.6); }
}

.ehead__submit-btn:disabled { opacity: 0.5; cursor: not-allowed; transform: none !important; }

/* ─── Shared animations ───────────────────────────────────────────────── */
@keyframes eheadSpin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

@keyframes eheadBlink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.6; }
}

/* ─── Responsive ─────────────────────────────────────────────────────── */
@media (max-width: 768px) {
  .ehead__device-badges { display: none; }
  .ehead__save { display: none; }
  .ehead { gap: 0.5rem; padding: 0.5rem 0.75rem; }
  .ehead__exam-title { max-width: 120px; }
  .ehead__timer-wrap { width: 50px; height: 50px; }
  .ehead__timer-main { font-size: 0.75rem; }
  .ehead__timer-sec { font-size: 0.55rem; }
}

@media (max-width: 480px) {
  .ehead__save-btn span { display: none; }
  .ehead__submit-btn span { display: none; }
  .ehead__submit-btn { padding: 0.5rem 0.75rem; }
  .ehead__save-btn { padding: 0.375rem 0.625rem; }
}
</style>
@media (prefers-reduced-motion: reduce) {
  * {
    transition-duration: 0.01ms !important;
    animation-duration: 0.01ms !important;
  }
}