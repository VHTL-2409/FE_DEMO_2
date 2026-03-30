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
          Câu {{ currentIndex + 1 }} / {{ totalQuestions }}
        </p>
      </div>
    </div>

    <!-- Center: device status -->
    <div v-if="showDevices" class="ehead__devices">
      <div class="ehead__device-item" :class="cameraReady ? 'ehead__device-item--ok' : 'ehead__device-item--fail'">
        <LucideIcon :name="cameraReady ? 'videocam' : 'videocam_off'" />
        <span>{{ cameraReady ? 'Camera' : 'Camera tắt' }}</span>
      </div>
      <div class="ehead__device-dot" />
      <div class="ehead__device-item" :class="micReady ? 'ehead__device-item--ok' : 'ehead__device-item--fail'">
        <LucideIcon :name="micReady ? 'mic' : 'mic_off'" />
        <span>{{ micReady ? 'Mic' : 'Mic tắt' }}</span>
      </div>
      <div class="ehead__device-dot" />
      <div class="ehead__device-item ehead__device-item--ok">
        <LucideIcon name="wifi" />
        <span>Kết nối</span>
      </div>
    </div>

    <!-- Right: timer + save + actions -->
    <div class="ehead__right">
      <!-- Progress bar -->
      <div v-if="initialRemainingForProgress > 0" class="ehead__progress-wrap">
        <progress
          class="ehead__progress"
          :class="progressBarClass"
          :value="remainingSeconds"
          :max="initialRemainingForProgress"
          :aria-valuenow="remainingSeconds"
          :aria-valuemin="0"
          :aria-valuemax="initialRemainingForProgress"
          :aria-label="timerAriaLabel"
        />
      </div>

      <!-- Timer -->
      <div class="ehead__timer" :class="timerClass">
        <div class="ehead__timer-unit">
          <span class="ehead__timer-val">{{ timerHours }}</span>
          <span class="ehead__timer-lbl">Giờ</span>
        </div>
        <span class="ehead__timer-sep">:</span>
        <div class="ehead__timer-unit">
          <span class="ehead__timer-val">{{ timerMinutes }}</span>
          <span class="ehead__timer-lbl">Phút</span>
        </div>
        <span class="ehead__timer-sep">:</span>
        <div class="ehead__timer-unit">
          <span class="ehead__timer-val">{{ timerSeconds }}</span>
          <span class="ehead__timer-lbl">Giây</span>
        </div>
      </div>

      <!-- Save status -->
      <div v-if="saveStatusLabel" class="ehead__save" role="status" aria-live="polite">
        <LucideIcon name="progress_activity" v-if="saveStatus === 'saving'"  ehead__save-icon ehead__save-icon--spin/>
        <LucideIcon name="check_circle" v-else-if="saveStatus === 'saved'"  ehead__save-icon ehead__save-icon--ok/>
        <LucideIcon name="schedule" v-else-if="hasPendingChanges"  ehead__save-icon ehead__save-icon--pending/>
        <LucideIcon name="cloud_off" v-else-if="saveStatus === 'error'"  ehead__save-icon ehead__save-icon--fail/>
        <span class="ehead__save-text">{{ saveStatusLabel }}</span>
      </div>

      <!-- Manual save -->
      <button
        type="button"
        class="ehead__action-btn"
        :disabled="isSuspended || saveStatus === 'saving'"
        @click="$emit('manual-save')"
      >
        Lưu ngay
      </button>

      <!-- Submit -->
      <button
        type="button"
        class="ehead__submit-btn"
        :disabled="isSuspended"
        @click="$emit('open-submit')"
      >
        <LucideIcon name="done_all" />
        Nộp bài
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

const prefersReducedMotion = () =>
  typeof window !== 'undefined' && window.matchMedia?.('(prefers-reduced-motion: reduce)').matches

const timerClass = computed(() => {
  const max = Math.max(props.initialRemainingForProgress, 1)
  const ratio = props.remainingSeconds / max
  if (ratio < 0.2) return prefersReducedMotion() ? '' : 'ehead__timer--critical'
  if (ratio <= 0.5) return 'ehead__timer--warning'
  return ''
})

const progressBarClass = computed(() => {
  const max = Math.max(props.initialRemainingForProgress, 1)
  const ratio = props.remainingSeconds / max
  if (ratio < 0.2) return prefersReducedMotion() ? '' : 'ehead__progress--critical'
  if (ratio <= 0.5) return 'ehead__progress--warning'
  return ''
})

const timerAriaLabel = computed(() => {
  const m = Math.floor(props.remainingSeconds / 60)
  const sec = props.remainingSeconds % 60
  return `Thời gian còn lại: ${m} phút ${sec} giây`
})
</script>


<style scoped>
.ehead {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 0.75rem 1.25rem;
  background: var(--ds-surface);
  border-bottom: 1px solid var(--ds-border);
  box-shadow: var(--ds-shadow-xs);
  position: sticky;
  top: 0;
  z-index: 50;
  flex-wrap: wrap;
  min-height: 60px;
}

.dark .ehead { border-bottom-color: var(--ds-border-strong); }

/* Left */
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
  background: var(--ds-primary-soft);
  color: var(--ds-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}


.ehead__exam-info { min-width: 0; }

.ehead__exam-title {
  font-size: 0.875rem;
  font-weight: 800;
  color: var(--ds-text);
  margin: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 200px;
}

.dark .ehead__exam-title { color: #f1f5f9; }

.ehead__exam-meta {
  font-size: 0.7rem;
  color: var(--ds-text-muted);
  margin: 0.125rem 0 0;
  font-weight: 600;
}

/* Devices */
.ehead__devices {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.375rem 0.875rem;
  border-radius: var(--ds-radius-full);
  background: var(--ds-gray-50);
  border: 1px solid var(--ds-border);
  flex-shrink: 0;
}

.dark .ehead__devices { background: var(--ds-gray-800); border-color: var(--ds-border-strong); }

.ehead__device-item {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  font-size: 0.72rem;
  font-weight: 600;
}


.ehead__device-item--ok { color: var(--ds-success); }
.ehead__device-item--fail { color: var(--ds-danger); }

.ehead__device-dot {
  width: 3px;
  height: 3px;
  border-radius: 50%;
  background: var(--ds-gray-300);
  flex-shrink: 0;
}

.dark .ehead__device-dot { background: var(--ds-gray-600); }

/* Right */
.ehead__right {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  margin-left: auto;
  flex-wrap: wrap;
}

/* Progress bar */
.ehead__progress-wrap {
  width: 120px;
  min-width: 80px;
}

.ehead__progress {
  width: 100%;
  height: 6px;
  border-radius: var(--ds-radius-full);
  appearance: none;
  background: var(--ds-gray-200);
  overflow: hidden;
}

.ehead__progress::-webkit-progress-value {
  background: var(--ds-primary);
  border-radius: var(--ds-radius-full);
  transition: width 0.5s ease;
}

.ehead__progress--warning::-webkit-progress-value { background: var(--ds-warning); }
.ehead__progress--critical::-webkit-progress-value { background: var(--ds-danger); }

.ehead__progress--critical {
  animation: eheadBlink 1.5s ease-in-out infinite;
}

@keyframes eheadBlink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.65; }
}

/* Timer */
.ehead__timer {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  padding: 0.5rem 0.875rem;
  border-radius: var(--ds-radius-xl);
  background: var(--ds-gray-50);
  border: 1px solid var(--ds-border);
  flex-shrink: 0;
}

.dark .ehead__timer { background: var(--ds-gray-800); border-color: var(--ds-border-strong); }

.ehead__timer--warning {
  background: rgba(234, 179, 8, 0.08);
  border-color: rgba(234, 179, 8, 0.25);
}

.ehead__timer--critical {
  background: var(--ds-danger-soft);
  border-color: rgba(220, 38, 38, 0.25);
  animation: eheadBlink 1.5s ease-in-out infinite;
}

.ehead__timer-unit {
  display: flex;
  flex-direction: column;
  align-items: center;
  min-width: 28px;
}

.ehead__timer-val {
  font-family: var(--ds-font-display);
  font-size: 1rem;
  font-weight: 900;
  color: var(--ds-text);
  line-height: 1;
  font-variant-numeric: tabular-nums;
}

.dark .ehead__timer-val { color: #f1f5f9; }

.ehead__timer--warning .ehead__timer-val { color: var(--ds-warning); }
.ehead__timer--critical .ehead__timer-val { color: var(--ds-danger); }

.ehead__timer-lbl {
  font-size: 0.5rem;
  font-weight: 700;
  color: var(--ds-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.05em;
  line-height: 1;
  margin-top: 0.1rem;
}

.ehead__timer-sep {
  font-size: 1rem;
  font-weight: 300;
  color: var(--ds-gray-300);
  line-height: 1;
}

.dark .ehead__timer-sep { color: var(--ds-gray-600); }

/* Save status */
.ehead__save {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  font-size: 0.7rem;
  font-weight: 600;
  color: var(--ds-text-muted);
  max-width: 180px;
  min-width: 100px;
}

.ehead__save-icon { font-size: 0.875rem; }
.ehead__save-icon--spin { animation: eheadSpin 1s linear infinite; }
.ehead__save-icon--ok { color: var(--ds-success); }
.ehead__save-icon--pending { color: var(--ds-warning); }
.ehead__save-icon--fail { color: var(--ds-danger); }

.ehead__save-text {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

@keyframes eheadSpin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

/* Buttons */
.ehead__action-btn {
  padding: 0.4rem 0.75rem;
  border-radius: var(--ds-radius-lg);
  font-size: 0.75rem;
  font-weight: 700;
  background: transparent;
  border: 1.5px solid var(--ds-border);
  color: var(--ds-text-muted);
  cursor: pointer;
  transition: all 0.12s ease;
  font-family: inherit;
  white-space: nowrap;
}

.dark .ehead__action-btn { border-color: var(--ds-border-strong); color: var(--ds-gray-400); }

.ehead__action-btn:hover:not(:disabled) {
  border-color: var(--ds-primary-border);
  color: var(--ds-primary);
  background: var(--ds-primary-soft);
}

.ehead__action-btn:disabled { opacity: 0.5; cursor: not-allowed; }

.ehead__submit-btn {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  padding: 0.5rem 1rem;
  border-radius: var(--ds-radius-xl);
  font-size: 0.8rem;
  font-weight: 800;
  background: var(--ds-primary);
  border: none;
  color: white;
  cursor: pointer;
  transition: all 0.15s ease;
  font-family: inherit;
  white-space: nowrap;
  box-shadow: 0 2px 8px rgba(79, 70, 229, 0.2);
}

.ehead__submit-btn:hover:not(:disabled) {
  background: var(--ds-primary-hover, #4338ca);
  box-shadow: 0 4px 12px rgba(79, 70, 229, 0.3);
  transform: translateY(-1px);
}

.ehead__submit-btn:disabled { opacity: 0.5; cursor: not-allowed; transform: none; }


/* Responsive */
@media (max-width: 768px) {
  .ehead__devices { display: none; }
  .ehead__progress-wrap { display: none; }
  .ehead__save { display: none; }
  .ehead { gap: 0.5rem; padding: 0.5rem 0.75rem; }
}

@media (max-width: 480px) {
  .ehead__action-btn { display: none; }
}
</style>
