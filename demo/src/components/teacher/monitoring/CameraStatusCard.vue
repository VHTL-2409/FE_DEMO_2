<template>
  <article
    class="camera-card"
    :class="`camera-card--${statusClass}`"
    @click="$emit('click')"
    @keydown.enter.prevent="$emit('click')"
    @keydown.space.prevent="$emit('click')"
    role="button"
    tabindex="0"
  >
    <!-- Header -->
    <div class="camera-card__header">
      <div class="camera-card__student">
        <span class="camera-card__name">{{ camera.studentName || 'Chưa rõ' }}</span>
        <span class="camera-card__code">{{ camera.studentCode || '' }}</span>
      </div>
      <span
        class="camera-card__status-badge"
        :class="`camera-card__status-badge--${statusClass}`"
      >
        {{ statusLabel }}
      </span>
    </div>

    <!-- Quick summary -->
    <div class="camera-card__summary">
      <span class="camera-card__summary-item">
        <LucideIcon :name="camera.cameraActive ? 'videocam' : 'videocam_off'" :size="12" />
        {{ camera.cameraActive ? 'Camera bật' : 'Không camera' }}
      </span>
      <span class="camera-card__summary-item">
        <LucideIcon name="scan-face" :size="12" />
        {{ faceStatusText }}
      </span>
      <span class="camera-card__summary-item">
        <LucideIcon name="focus" :size="12" />
        {{ gazeStatusText }}
      </span>
    </div>

    <!-- Stats -->
    <div class="camera-card__stats">
      <div class="camera-card__stat">
        <LucideIcon name="alert-triangle" :size="14" title="Số cảnh báo" />
        <span>{{ camera.alertCount || 0 }}</span>
      </div>
      <div class="camera-card__stat">
        <LucideIcon name="eye" :size="14" />
        <span>{{ eyeStatusText }}</span>
      </div>
      <div class="camera-card__stat">
        <LucideIcon name="sun" :size="14" />
        <span>{{ lightStatusText }}</span>
      </div>
    </div>

    <!-- Signals -->
    <div v-if="activeSignals.length" class="camera-card__signals">
      <span
        v-for="signal in activeSignalsPreview"
        :key="signal"
        class="camera-card__signal-tag"
        :class="`camera-card__signal-tag--${getSignalSeverity(signal)}`"
      >
        {{ formatSignalType(signal) }}
      </span>
      <span v-if="activeSignals.length > 3" class="camera-card__signal-more">
        +{{ activeSignals.length - 3 }}
      </span>
    </div>

    <!-- Footer -->
    <div class="camera-card__footer">
      <span class="camera-card__time">{{ formatTimeAgo(camera.lastUpdate) }}</span>
      <span class="camera-card__risk" :class="riskClass" title="Điểm rủi ro">
        {{ Math.round(camera.riskScore || 0) }}/100
      </span>
    </div>
  </article>
</template>

<script setup>
import { computed } from 'vue'
import LucideIcon from '../../common/LucideIcon.vue'
import { normalizeSignalType, uniqueSignalTypes } from '../../../utils/proctorSignalTypes'

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
  if (!props.camera.faceDetected) return 'Không thấy mặt'
  if (props.camera.multipleFaces) return 'Nhiều mặt'
  if (props.camera.faceQuality === 'GOOD') return 'Tốt'
  if (props.camera.faceQuality === 'POOR') return 'Kém'
  return props.camera.faceQuality || 'Ổn'
})

const lightStatusText = computed(() => {
  const brightness = props.camera.averageBrightness
  if (brightness == null) return 'Không rõ'
  if (brightness < 40) return 'Rất tối'
  if (brightness < 60) return 'Tối'
  if (brightness > 240) return 'Quá sáng'
  return 'Tốt'
})

const eyeStatusText = computed(() => {
  if (!props.camera.faceDetected) return 'Không có mắt'
  const state = String(props.camera.eyeState || '').toUpperCase()
  if (state === 'CLOSED') return 'Đóng'
  if (state === 'PARTIAL') return 'Hở'
  if (state === 'OPEN') return 'Mở'
  if (props.camera.eyeCount != null) {
    return `${props.camera.eyeCount} mắt`
  }
  return '—'
})

const gazeStatusText = computed(() => {
  const direction = String(props.camera.gazeDirection || '').toUpperCase()
  const gazeOffScreen = props.camera.gazeOffScreen === true
  if (!props.camera.faceDetected) return 'Không xác định'
  if (gazeOffScreen && direction && direction !== 'CENTER') {
    return 'Nhìn lệch'
  }
  if (direction && direction !== 'CENTER') {
    return 'Lệch hướng'
  }
  return 'Chính giữa'
})

const riskClass = computed(() => {
  const score = props.camera.riskScore || 0
  if (score >= 61) return 'camera-card__risk--high'
  if (score >= 31) return 'camera-card__risk--medium'
  return 'camera-card__risk--low'
})

const activeSignals = computed(() => {
  const signals = Array.isArray(props.camera.activeSignals) ? props.camera.activeSignals : []
  return uniqueSignalTypes(signals)
})

const activeSignalsPreview = computed(() => activeSignals.value.slice(0, 3))

// Methods
function getSignalSeverity(signal) {
  const signalType = normalizeSignalType(signal)
  const severityMap = {
    NO_CAMERA: 'high',
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
  return severityMap[signalType] || 'low'
}

function formatSignalType(signal) {
  const signalType = normalizeSignalType(signal)
  const labelMap = {
    NO_CAMERA: 'Camera tắt',
    FACE_NOT_DETECTED: 'Không mặt',
    MULTIPLE_FACES: 'Nhiều mặt',
    FACE_SPOOFING_SUSPECTED: 'Giả mạo',
    FACE_OBSTRUCTED_MASK: 'Khẩu trang',
    EYES_OBSTRUCTED: 'Kính',
    PARTIAL_FACE_VISIBLE: 'Che khuất',
    FACE_TOO_FAR: 'Quá xa',
    FACE_TOO_CLOSE: 'Quá gần',
    FACE_TURNED_AWAY: 'Quay đi',
    FACE_NOT_CENTERED: 'Lệch tâm',
    EYES_NOT_DETECTED: 'Không mắt',
    VERY_LOW_LIGHTING: 'Rất tối',
    LOW_LIGHTING: 'Thiếu sáng',
    OVEREXPOSED_FRAME: 'Cháy sáng',
    VERY_BLURRY_FRAME: 'Rất mờ',
    BLURRY_FRAME: 'Mờ',
    EYE_BLINK_ANOMALY: 'Chớp mắt',
    EYES_CLOSED_PROLONGED: 'Nhắm mắt',
    GAZE_OFF_SCREEN: 'Nhìn lệch',
    RAPID_EYE_MOVEMENT: 'Mắt đảo nhanh',
    PRINTED_PHOTO: 'Ảnh in',
    SCREEN_REPLAY: 'Phát lại',
    DEEPFAKE: 'Deepfake',
    FLAT_IMAGE: 'Ảnh phẳng',
    SCREEN_DISPLAY: 'Màn hình'
  }
  return labelMap[signalType] || signalType
}

function formatTimeAgo(timestamp) {
  if (!timestamp) return 'Không rõ'
  const now = new Date()
  const date = new Date(timestamp)
  const diffMs = now - date
  const diffSec = Math.floor(diffMs / 1000)
  const diffMin = Math.floor(diffSec / 60)

  if (diffSec < 60) return 'Vừa xong'
  if (diffMin < 60) return `${diffMin} phút trước`
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

/* Summary */
.camera-card__summary {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.camera-card__summary-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 3px 6px;
  border-radius: var(--ds-radius-xs);
  background: var(--ds-gray-100);
  color: var(--ds-text-secondary);
  font-size: 10px;
  font-weight: 700;
}

/* Stats */
.camera-card__stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 4px;
}

.camera-card__stat {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: var(--ds-text-xs);
  color: var(--ds-text-secondary);
  min-width: 0;
  padding: 4px 6px;
  border-radius: var(--ds-radius-xs);
  background: var(--ds-gray-50);
}

.camera-card__stat span {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
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
