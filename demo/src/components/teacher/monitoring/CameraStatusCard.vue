<template>
  <article
    class="camera-card"
    :class="`camera-card--${statusClass}`"
    role="button"
    tabindex="0"
    @click="$emit('click')"
    @keydown.enter.prevent="$emit('click')"
    @keydown.space.prevent="$emit('click')"
  >
    <div class="camera-card__header">
      <div class="camera-card__student">
        <span class="camera-card__name">{{ camera.studentName || 'Chưa rõ' }}</span>
        <span class="camera-card__code">{{ camera.studentCode || 'Không có mã' }}</span>
      </div>
      <div class="camera-card__badges">
        <span class="camera-card__health" :class="`camera-card__health--${connectionHealth}`">
          {{ connectionHealthLabel }}
        </span>
        <span
          class="camera-card__status-badge"
          :class="`camera-card__status-badge--${statusClass}`"
        >
          {{ statusLabel }}
        </span>
      </div>
    </div>

    <div v-if="camera.previewAvailable" class="camera-card__preview">
      <div class="camera-card__preview-grid">
        <span
          v-for="(box, index) in overlayBoxes"
          :key="index"
          class="camera-card__face-box"
          :style="box"
        />
        <span class="camera-card__preview-label">
          {{ overlayLabel }}
        </span>
      </div>
    </div>

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

    <div class="camera-card__stats">
      <div class="camera-card__stat">
        <LucideIcon name="alert-triangle" :size="14" title="Số cảnh báo" />
        <span>{{ camera.alertCount || 0 }} cảnh báo</span>
      </div>
      <div class="camera-card__stat">
        <LucideIcon name="eye" :size="14" />
        <span>{{ eyeStatusText }}</span>
      </div>
      <div class="camera-card__stat">
        <LucideIcon name="sun" :size="14" />
        <span>{{ lightStatusText }}</span>
      </div>
      <div class="camera-card__stat">
        <LucideIcon name="activity" :size="14" />
        <span>{{ attentionText }}</span>
      </div>
      <div class="camera-card__stat">
        <LucideIcon name="crosshair" :size="14" />
        <span>{{ confidenceText }}</span>
      </div>
      <div class="camera-card__stat">
        <LucideIcon name="clock" :size="14" />
        <span>{{ frameAgeText }}</span>
      </div>
    </div>

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

    <div class="camera-card__footer">
      <span class="camera-card__time">{{ formatTimeAgo(camera.lastFrameAt || camera.lastUpdate) }}</span>
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

const statusClass = computed(() => {
  if (!props.camera.cameraActive) return 'no-camera'
  return String(props.camera.status || 'OK').toLowerCase()
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

const connectionHealth = computed(() => props.camera.connectionHealth || 'unknown')

const connectionHealthLabel = computed(() => {
  const labelMap = {
    live: 'Live',
    stale: 'Chậm',
    offline: 'Mất tín hiệu',
    ai_busy: 'AI bận',
    ai_unavailable: 'AI lỗi',
    no_camera: 'Tắt',
    unknown: 'Chưa rõ'
  }
  return labelMap[connectionHealth.value] || 'Chưa rõ'
})

const overlay = computed(() => props.camera.visualOverlay || {})

const overlayLabel = computed(() => {
  return overlay.value.label || overlay.value.status || 'Có dữ liệu overlay'
})

const overlayBoxes = computed(() => {
  const width = Number(overlay.value.imageWidth || overlay.value.image_width || 0)
  const height = Number(overlay.value.imageHeight || overlay.value.image_height || 0)
  const boxes = overlay.value.faceBoxes || overlay.value.face_boxes || []
  if (!width || !height || !Array.isArray(boxes)) return []
  return boxes.slice(0, 3).map(box => ({
    left: `${Math.max(0, Number(box.x || 0) / width) * 100}%`,
    top: `${Math.max(0, Number(box.y || 0) / height) * 100}%`,
    width: `${Math.min(100, Math.max(0, Number(box.width || 0) / width) * 100)}%`,
    height: `${Math.min(100, Math.max(0, Number(box.height || 0) / height) * 100)}%`
  }))
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
  if (props.camera.eyeCount != null) return `${props.camera.eyeCount} mắt`
  return 'Không rõ'
})

const gazeStatusText = computed(() => {
  const direction = String(props.camera.gazeDirection || '').toUpperCase()
  const gazeOffScreen = props.camera.gazeOffScreen === true
  if (!props.camera.faceDetected) return 'Không xác định'
  if (gazeOffScreen && direction && direction !== 'CENTER') return 'Nhìn lệch'
  if (direction && direction !== 'CENTER') return 'Lệch hướng'
  return 'Chính giữa'
})

const attentionText = computed(() => {
  if (props.camera.attentionScore == null) return 'Chưa đo'
  return `${Math.round(Number(props.camera.attentionScore) * 100)}% chú ý`
})

const confidenceText = computed(() => {
  const confidence = props.camera.gazeConfidence ?? props.camera.eyeTrackingConfidence
  if (confidence == null) return 'Chưa đo'
  return `${Math.round(Number(confidence) * 100)}% tin cậy`
})

const frameAgeText = computed(() => {
  const age = props.camera.frameAgeMs
  if (age == null) return 'Chưa có frame'
  if (age < 1000) return 'Vừa nhận'
  if (age < 60_000) return `${Math.floor(age / 1000)} giây`
  return `${Math.floor(age / 60_000)} phút`
})

const riskClass = computed(() => {
  const score = props.camera.riskScore || 0
  if (score >= 61) return 'camera-card__risk--high'
  if (score >= 31) return 'camera-card__risk--medium'
  return 'camera-card__risk--low'
})

const activeSignals = computed(() => uniqueSignalTypes(props.camera.activeSignals || []))
const activeSignalsPreview = computed(() => activeSignals.value.slice(0, 3))

function getSignalSeverity(signal) {
  const signalType = normalizeSignalType(signal)
  const severityMap = {
    NO_CAMERA: 'high',
    NO_MIC: 'high',
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
    LOW_LIVENESS: 'high',
    FLAT_IMAGE: 'high',
    SCREEN_DISPLAY: 'high',
    EYES_OBSTRUCTED: 'medium',
    PARTIAL_FACE_VISIBLE: 'medium',
    FACE_TURNED_AWAY: 'medium',
    EYE_BLINK_ANOMALY: 'medium',
    RAPID_EYE_MOVEMENT: 'medium',
    AI_SPEAKING_DETECTED: 'medium',
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
    NO_MIC: 'Micro tắt',
    FACE_NOT_DETECTED: 'Không mặt',
    MULTIPLE_FACES: 'Nhiều mặt',
    FACE_SPOOFING_SUSPECTED: 'Giả mạo',
    FACE_OBSTRUCTED_MASK: 'Khẩu trang',
    EYES_OBSTRUCTED: 'Mắt bị che',
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
    AI_SPEAKING_DETECTED: 'Có tiếng nói',
    PRINTED_PHOTO: 'Ảnh in',
    SCREEN_REPLAY: 'Phát lại',
    DEEPFAKE: 'Deepfake',
    LOW_LIVENESS: 'Không thật',
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
  if (!Number.isFinite(diffMs)) return 'Không rõ'
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
  opacity: 0.82;
}

.camera-card__header,
.camera-card__summary,
.camera-card__signals,
.camera-card__footer,
.camera-card__badges {
  display: flex;
}

.camera-card__header {
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

.camera-card__badges {
  align-items: flex-end;
  flex-direction: column;
  gap: 4px;
}

.camera-card__status-badge,
.camera-card__health,
.camera-card__risk {
  border-radius: 9999px;
  font-size: var(--ds-text-xs);
  font-weight: 800;
  white-space: nowrap;
}

.camera-card__status-badge,
.camera-card__health {
  padding: 2px 8px;
}

.camera-card__status-badge--ok,
.camera-card__health--live,
.camera-card__risk--low {
  background: var(--ds-success-soft);
  color: var(--ds-success);
}

.camera-card__status-badge--warning,
.camera-card__health--stale,
.camera-card__health--ai_busy,
.camera-card__risk--medium {
  background: var(--ds-warning-soft);
  color: var(--ds-warning);
}

.camera-card__status-badge--critical,
.camera-card__health--offline,
.camera-card__health--ai_unavailable,
.camera-card__risk--high {
  background: var(--ds-danger-soft);
  color: var(--ds-danger);
}

.camera-card__status-badge--no-camera,
.camera-card__health--no_camera,
.camera-card__health--unknown {
  background: var(--ds-gray-100);
  color: var(--ds-text-muted);
}

.camera-card__preview {
  aspect-ratio: 16 / 9;
  border: 1px solid var(--ds-border);
  border-radius: var(--ds-radius-md);
  overflow: hidden;
  background: linear-gradient(135deg, var(--ds-gray-100), var(--ds-surface-muted));
}

.camera-card__preview-grid {
  position: relative;
  width: 100%;
  height: 100%;
  background-image:
    linear-gradient(to right, rgba(0, 0, 0, 0.08) 1px, transparent 1px),
    linear-gradient(to bottom, rgba(0, 0, 0, 0.08) 1px, transparent 1px);
  background-size: 24px 24px;
}

.camera-card__face-box {
  position: absolute;
  border: 2px solid var(--ds-success);
  border-radius: var(--ds-radius-xs);
  box-shadow: 0 0 0 1px rgba(255, 255, 255, 0.75);
}

.camera-card__preview-label {
  position: absolute;
  left: 8px;
  bottom: 8px;
  max-width: calc(100% - 16px);
  padding: 3px 8px;
  border-radius: var(--ds-radius-full);
  background: rgba(0, 0, 0, 0.58);
  color: white;
  font-size: 10px;
  font-weight: 800;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.camera-card__summary {
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

.camera-card__stats {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
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

.camera-card__signals {
  flex-wrap: wrap;
  gap: 4px;
}

.camera-card__signal-tag,
.camera-card__signal-more {
  padding: 2px 6px;
  border-radius: var(--ds-radius-xs);
  font-size: 10px;
  font-weight: 700;
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

.camera-card__signal-tag--low,
.camera-card__signal-more {
  background: var(--ds-gray-100);
  color: var(--ds-text-secondary);
}

.camera-card__footer {
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
  font-variant-numeric: tabular-nums;
}
</style>
