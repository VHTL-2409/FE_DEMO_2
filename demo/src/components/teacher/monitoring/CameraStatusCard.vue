<template>
  <article
    class="camera-card"
    :class="`camera-card--${statusClass}`"
    @click="$emit('click')"
  >
    <!-- Header -->
    <div class="camera-card__header">
      <div class="camera-card__student">
        <span class="camera-card__name">{{ camera.studentName || 'Unknown' }}</span>
        <span class="camera-card__code">{{ camera.studentCode || '' }}</span>
      </div>
      <span
        class="camera-card__status-badge"
        :class="`camera-card__status-badge--${statusClass}`"
      >
        {{ statusLabel }}
      </span>
    </div>

    <!-- Camera Preview Placeholder -->
    <div class="camera-card__preview">
      <div v-if="camera.cameraActive" class="camera-card__video-placeholder">
        <LucideIcon name="user" :size="32" />
        <span class="camera-card__preview-text">Camera Active</span>
      </div>
      <div v-else class="camera-card__no-camera">
        <LucideIcon name="camera-off" :size="32" />
        <span class="camera-card__preview-text">No Camera</span>
      </div>
    </div>

    <!-- Stats -->
    <div class="camera-card__stats">
      <div class="camera-card__stat">
        <LucideIcon name="alert-triangle" :size="14" />
        <span>{{ camera.alertCount || 0 }}</span>
      </div>
      <div class="camera-card__stat">
        <LucideIcon name="scan-face" :size="14" />
        <span>{{ faceStatusText }}</span>
      </div>
      <div class="camera-card__stat">
        <LucideIcon name="sun" :size="14" />
        <span>{{ lightStatusText }}</span>
      </div>
    </div>

    <!-- Signals -->
    <div v-if="camera.activeSignals?.length" class="camera-card__signals">
      <span
        v-for="signal in camera.activeSignals.slice(0, 3)"
        :key="signal"
        class="camera-card__signal-tag"
        :class="`camera-card__signal-tag--${getSignalSeverity(signal)}`"
      >
        {{ formatSignalType(signal) }}
      </span>
      <span v-if="camera.activeSignals.length > 3" class="camera-card__signal-more">
        +{{ camera.activeSignals.length - 3 }}
      </span>
    </div>

    <!-- Footer -->
    <div class="camera-card__footer">
      <span class="camera-card__time">{{ formatTimeAgo(camera.lastUpdate) }}</span>
      <span v-if="camera.riskScore" class="camera-card__risk" :class="riskClass">
        {{ camera.riskScore }}
      </span>
    </div>
  </article>
</template>

<script setup>
import { computed } from 'vue'
import LucideIcon from '../../common/LucideIcon.vue'

const props = defineProps({
  camera: {
    type: Object,
    required: true
  }
})

defineEmits(['click'])

// Computed
const statusClass = computed(() => {
  if (!props.camera.cameraActive) return 'no-camera'
  return props.camera.status?.toLowerCase() || 'ok'
})

const statusLabel = computed(() => {
  if (!props.camera.cameraActive) return 'Không camera'
  const statusMap = {
    ok: 'OK',
    warning: 'Cảnh báo',
    critical: 'Nguy hiểm',
    suspicious: 'Đáng ngờ'
  }
  return statusMap[statusClass.value] || 'OK'
})

const faceStatusText = computed(() => {
  if (!props.camera.faceDetected) return 'No Face'
  if (props.camera.multipleFaces) return 'Multi Face'
  if (props.camera.faceQuality === 'GOOD') return 'Good'
  if (props.camera.faceQuality === 'POOR') return 'Poor'
  return props.camera.faceQuality || 'OK'
})

const lightStatusText = computed(() => {
  const brightness = props.camera.averageBrightness
  if (!brightness) return 'N/A'
  if (brightness < 40) return 'Very Dark'
  if (brightness < 60) return 'Dark'
  if (brightness > 240) return 'Too Bright'
  return 'Good'
})

const riskClass = computed(() => {
  const score = props.camera.riskScore || 0
  if (score >= 61) return 'camera-card__risk--high'
  if (score >= 31) return 'camera-card__risk--medium'
  return 'camera-card__risk--low'
})

// Methods
function getSignalSeverity(signal) {
  const severityMap = {
    MULTIPLE_FACES: 'critical',
    FACE_SPOOFING_SUSPECTED: 'critical',
    FACE_NOT_DETECTED: 'high',
    FACE_OBSTRUCTED_MASK: 'high',
    VERY_LOW_LIGHTING: 'high',
    VERY_BLURRY_FRAME: 'high',
    GAZE_OFF_SCREEN: 'high',
    PRINTED_PHOTO: 'critical',
    SCREEN_REPLAY: 'critical',
    DEEPFAKE: 'critical',
    FLAT_IMAGE: 'high',
    SCREEN_DISPLAY: 'high',
    EYES_OBSTRUCTED: 'medium',
    PARTIAL_FACE_VISIBLE: 'medium',
    FACE_TURNED_AWAY: 'medium',
    EYE_BLINK_ANOMALY: 'medium',
    RAPID_EYE_MOVEMENT: 'medium',
    LOW_LIGHTING: 'low',
    BLURRY_FRAME: 'low',
    EYES_CLOSED_PROLONGED: 'low'
  }
  return severityMap[signal] || 'low'
}

function formatSignalType(signal) {
  const labelMap = {
    FACE_NOT_DETECTED: 'No Face',
    MULTIPLE_FACES: 'Multi Face',
    FACE_SPOOFING_SUSPECTED: 'Spoofing',
    FACE_OBSTRUCTED_MASK: 'Mask',
    EYES_OBSTRUCTED: 'Glasses',
    PARTIAL_FACE_VISIBLE: 'Partial',
    FACE_TOO_FAR: 'Too Far',
    FACE_TOO_CLOSE: 'Too Close',
    FACE_TURNED_AWAY: 'Turned Away',
    FACE_NOT_CENTERED: 'Off Center',
    EYES_NOT_DETECTED: 'No Eyes',
    VERY_LOW_LIGHTING: 'Dark',
    LOW_LIGHTING: 'Low Light',
    OVEREXPOSED_FRAME: 'Overexposed',
    VERY_BLURRY_FRAME: 'Very Blur',
    BLURRY_FRAME: 'Blur',
    EYE_BLINK_ANOMALY: 'Blink',
    EYES_CLOSED_PROLONGED: 'Eyes Closed',
    GAZE_OFF_SCREEN: 'Gaze Away',
    RAPID_EYE_MOVEMENT: 'Rapid Eyes',
    PRINTED_PHOTO: 'Printed Photo',
    SCREEN_REPLAY: 'Replay',
    DEEPFAKE: 'Deepfake',
    FLAT_IMAGE: 'Flat Image',
    SCREEN_DISPLAY: 'Screen'
  }
  return labelMap[signal] || signal
}

function formatTimeAgo(timestamp) {
  if (!timestamp) return 'N/A'
  const now = new Date()
  const date = new Date(timestamp)
  const diffMs = now - date
  const diffSec = Math.floor(diffMs / 1000)
  const diffMin = Math.floor(diffSec / 60)

  if (diffSec < 60) return `${diffSec}s ago`
  if (diffMin < 60) return `${diffMin}m ago`
  return date.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })
}
</script>

<style scoped>
.camera-card {
  background: var(--ds-surface);
  border: 1px solid var(--ds-border);
  border-radius: var(--ds-radius-lg);
  padding: var(--ds-space-3);
  cursor: pointer;
  transition: all var(--ds-duration-base);
  display: flex;
  flex-direction: column;
  gap: var(--ds-space-2);
}

.camera-card:hover {
  border-color: var(--ds-primary-border);
  box-shadow: var(--ds-shadow-md);
  transform: translateY(-2px);
}

.camera-card--warning {
  border-left: 3px solid var(--ds-warning);
}

.camera-card--critical {
  border-left: 3px solid var(--ds-danger);
}

.camera-card--no-camera {
  border-left: 3px solid var(--ds-text-muted);
  opacity: 0.8;
}

/* Header */
.camera-card__header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: var(--ds-space-2);
}

.camera-card__student {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.camera-card__name {
  font-size: var(--ds-text-sm);
  font-weight: 700;
  color: var(--ds-text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.camera-card__code {
  font-size: var(--ds-text-xs);
  color: var(--ds-text-muted);
}

.camera-card__status-badge {
  flex-shrink: 0;
  padding: 2px 8px;
  border-radius: 9999px;
  font-size: var(--ds-text-xs);
  font-weight: 700;
}

.camera-card__status-badge--ok {
  background: var(--ds-success-soft);
  color: var(--ds-success);
}

.camera-card__status-badge--warning {
  background: var(--ds-warning-soft);
  color: var(--ds-warning);
}

.camera-card__status-badge--critical {
  background: var(--ds-danger-soft);
  color: var(--ds-danger);
}

.camera-card__status-badge--no-camera {
  background: var(--ds-gray-100);
  color: var(--ds-text-muted);
}

/* Preview */
.camera-card__preview {
  aspect-ratio: 16 / 9;
  border-radius: var(--ds-radius-md);
  overflow: hidden;
  background: var(--ds-gray-900);
}

.camera-card__video-placeholder,
.camera-card__no-camera {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: var(--ds-space-1);
  color: var(--ds-gray-400);
}

.camera-card__preview-text {
  font-size: var(--ds-text-xs);
  color: var(--ds-gray-500);
}

/* Stats */
.camera-card__stats {
  display: flex;
  gap: var(--ds-space-3);
}

.camera-card__stat {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: var(--ds-text-xs);
  color: var(--ds-text-secondary);
}

/* Signals */
.camera-card__signals {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.camera-card__signal-tag {
  padding: 2px 6px;
  border-radius: var(--ds-radius-xs);
  font-size: 10px;
  font-weight: 600;
}

.camera-card__signal-tag--critical {
  background: var(--ds-danger-soft);
  color: var(--ds-danger);
}

.camera-card__signal-tag--high {
  background: var(--ds-warning-soft);
  color: var(--ds-warning);
}

.camera-card__signal-tag--medium {
  background: var(--ds-info-soft);
  color: var(--ds-info);
}

.camera-card__signal-tag--low {
  background: var(--ds-gray-100);
  color: var(--ds-text-secondary);
}

.camera-card__signal-more {
  padding: 2px 6px;
  background: var(--ds-gray-100);
  color: var(--ds-text-muted);
  border-radius: var(--ds-radius-xs);
  font-size: 10px;
  font-weight: 600;
}

/* Footer */
.camera-card__footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: var(--ds-space-2);
  border-top: 1px solid var(--ds-border);
}

.camera-card__time {
  font-size: var(--ds-text-xs);
  color: var(--ds-text-muted);
}

.camera-card__risk {
  padding: 2px 8px;
  border-radius: 9999px;
  font-size: var(--ds-text-xs);
  font-weight: 800;
  font-variant-numeric: tabular-nums;
}

.camera-card__risk--low {
  background: var(--ds-success-soft);
  color: var(--ds-success);
}

.camera-card__risk--medium {
  background: var(--ds-warning-soft);
  color: var(--ds-warning);
}

.camera-card__risk--high {
  background: var(--ds-danger-soft);
  color: var(--ds-danger);
}
</style>
