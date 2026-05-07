const STATUS_TOKEN_PATTERNS = [
  { token: 'SUBMITTED', pattern: /SUBMITTED|AUTO_SUBMITTED|COMPLETED/ },
  { token: 'STOPPED', pattern: /STOPPED/ },
  { token: 'PAUSED', pattern: /PAUSED/ },
  { token: 'ACTIVE', pattern: /ACTIVE|IN_PROGRESS/ }
]

const STATUS_META = {
  SUBMITTED: { label: 'Đã nộp', color: 'var(--ds-success)', bg: 'var(--ds-success-bg)', icon: 'check-circle' },
  STOPPED: { label: 'Đã dừng', color: 'var(--ds-danger)', bg: 'var(--ds-danger-bg)', icon: 'x-circle' },
  PAUSED: { label: 'Tạm dừng', color: 'var(--ds-warning)', bg: 'var(--ds-warning-bg)', icon: 'pause-circle' },
  ACTIVE: { label: 'Đang thi', color: 'var(--ds-primary)', bg: 'var(--ds-primary-soft)', icon: 'play-circle' },
  UNKNOWN: { label: '—', color: 'var(--ds-text-muted)', bg: 'var(--ds-surface-muted)', icon: 'help-circle' }
}

export const RISK_BAND_THRESHOLDS = Object.freeze({
  CRITICAL: 81,
  HIGH_RISK: 61,
  SUSPICIOUS: 31
})

const RISK_BAND_VISUAL = {
  danger: { color: 'var(--ds-danger)', bg: 'var(--ds-danger-bg)', soft: 'var(--ds-danger-soft)' },
  warn: { color: 'var(--ds-warning)', bg: 'var(--ds-warning-bg)', soft: 'var(--ds-warning-soft)' },
  clean: { color: 'var(--ds-success)', bg: 'var(--ds-success-bg)', soft: 'var(--ds-success-soft)' }
}

const SEVERITY_VISUAL = {
  HIGH: { color: 'var(--ds-danger)', bg: 'var(--ds-danger-bg)', soft: 'var(--ds-danger-soft)' },
  CRITICAL: { color: 'var(--ds-danger)', bg: 'var(--ds-danger-bg)', soft: 'var(--ds-danger-soft)' },
  MEDIUM: { color: 'var(--ds-warning)', bg: 'var(--ds-warning-bg)', soft: 'var(--ds-warning-soft)' },
  LOW: { color: 'var(--ds-info)', bg: 'var(--ds-info-bg)', soft: 'var(--ds-info-soft)' }
}

const PATTERN_LEVEL_VISUAL = {
  high: { color: 'var(--ds-danger)', bg: 'var(--ds-danger-bg)', border: 'var(--ds-danger-soft)' },
  medium: { color: 'var(--ds-warning)', bg: 'var(--ds-warning-bg)', border: 'var(--ds-warning-soft)' },
  low: { color: 'var(--ds-info)', bg: 'var(--ds-info-bg)', border: 'var(--ds-info-soft)' }
}

// AI Camera Detection Signal Labels
export const AI_CAMERA_SIGNALS = {
  NO_CAMERA: { label: 'Camera tắt', severity: 'HIGH', icon: 'videocam_off' },
  FACE_NOT_DETECTED: { label: 'Không có khuôn mặt', severity: 'HIGH', icon: 'user-x' },
  MULTIPLE_FACES: { label: 'Nhiều khuôn mặt', severity: 'CRITICAL', icon: 'users' },
  FACE_SPOOFING_SUSPECTED: { label: 'Nghi vấn giả mạo', severity: 'CRITICAL', icon: 'shield-alert' },
  FACE_OBSTRUCTED_MASK: { label: 'Khẩu trang che mặt', severity: 'HIGH', icon: 'mask' },
  EYES_OBSTRUCTED: { label: 'Kính che mắt', severity: 'MEDIUM', icon: 'glasses' },
  PARTIAL_FACE_VISIBLE: { label: 'Mặt không đầy đủ', severity: 'MEDIUM', icon: 'user-check' },
  FACE_TOO_FAR: { label: 'Mặt quá xa camera', severity: 'MEDIUM', icon: 'minimize' },
  FACE_TOO_CLOSE: { label: 'Mặt quá gần camera', severity: 'LOW', icon: 'maximize' },
  FACE_TURNED_AWAY: { label: 'Quay mặt đi', severity: 'MEDIUM', icon: 'rotate-ccw' },
  FACE_NOT_CENTERED: { label: 'Mặt lệch tâm', severity: 'LOW', icon: 'align-center' },
  EYES_NOT_DETECTED: { label: 'Không phát hiện mắt', severity: 'MEDIUM', icon: 'eye-off' },
  VERY_LOW_LIGHTING: { label: 'Ánh sáng rất yếu', severity: 'HIGH', icon: 'moon' },
  LOW_LIGHTING: { label: 'Ánh sáng yếu', severity: 'MEDIUM', icon: 'cloudy' },
  OVEREXPOSED_FRAME: { label: 'Ảnh quá sáng', severity: 'LOW', icon: 'sun' },
  VERY_BLURRY_FRAME: { label: 'Ảnh rất mờ', severity: 'HIGH', icon: 'blur' },
  BLURRY_FRAME: { label: 'Ảnh mờ', severity: 'LOW', icon: 'image' }
}

// Eye Tracking Signal Labels
export const EYE_TRACKING_SIGNALS = {
  EYE_BLINK_ANOMALY: { label: 'Nháy mắt bất thường', severity: 'MEDIUM', icon: 'eye' },
  EYES_CLOSED_PROLONGED: { label: 'Mắt nhắm lâu', severity: 'LOW', icon: 'eye-off' }
}

// Gaze Tracking Signal Labels
export const GAZE_TRACKING_SIGNALS = {
  GAZE_OFF_SCREEN: { label: 'Nhìn ra ngoài màn hình', severity: 'HIGH', icon: 'external-link' },
  RAPID_EYE_MOVEMENT: { label: 'Chuyển động mắt nhanh', severity: 'MEDIUM', icon: 'zap' }
}

// Deep Learning Spoofing Signal Labels
export const SPOOFING_SIGNALS = {
  PRINTED_PHOTO: { label: 'Ảnh in giả', severity: 'CRITICAL', icon: 'image' },
  SCREEN_REPLAY: { label: 'Phát lại màn hình', severity: 'CRITICAL', icon: 'monitor' },
  DEEPFAKE: { label: 'Deepfake', severity: 'CRITICAL', icon: 'alert-octagon' },
  FLAT_IMAGE: { label: 'Hình ảnh phẳng', severity: 'HIGH', icon: 'square' },
  SCREEN_DISPLAY: { label: 'Hình từ màn hình', severity: 'HIGH', icon: 'monitor' }
}

// Combine all AI signals
export const ALL_AI_SIGNALS = {
  ...AI_CAMERA_SIGNALS,
  ...EYE_TRACKING_SIGNALS,
  ...GAZE_TRACKING_SIGNALS,
  ...SPOOFING_SIGNALS
}

export const AI_SIGNAL_LIST = Object.keys(ALL_AI_SIGNALS)

export const AI_CAMERA_SIGNAL_LIST = Object.keys(AI_CAMERA_SIGNALS)

export const resolveAttemptStatusToken = (rawStatus) => {
  const value = String(rawStatus || '').toUpperCase()
  if (!value) return 'UNKNOWN'
  for (const { token, pattern } of STATUS_TOKEN_PATTERNS) {
    if (pattern.test(value)) return token
  }
  return 'UNKNOWN'
}

export const getAttemptStatusMeta = (rawStatus) => {
  const meta = STATUS_META[resolveAttemptStatusToken(rawStatus)]
  return { ...meta, raw: String(rawStatus || '').toUpperCase() }
}

export const isAttemptPaused = (rawStatus) =>
  resolveAttemptStatusToken(rawStatus) === 'PAUSED'

export const isAttemptActive = (rawStatus) =>
  resolveAttemptStatusToken(rawStatus) === 'ACTIVE'

export const isAttemptTerminal = (rawStatus) => {
  const token = resolveAttemptStatusToken(rawStatus)
  return token === 'SUBMITTED' || token === 'STOPPED'
}

export const resolveRiskBand = (riskScore) => {
  const score = Number(riskScore || 0)
  if (score >= RISK_BAND_THRESHOLDS.HIGH_RISK) return 'danger'
  if (score >= RISK_BAND_THRESHOLDS.SUSPICIOUS) return 'warn'
  return 'clean'
}

export const getRiskBandVisual = (riskScore) => {
  return RISK_BAND_VISUAL[resolveRiskBand(riskScore)]
}

export const getSeverityVisual = (severity) =>
  SEVERITY_VISUAL[String(severity || '').toUpperCase()] || SEVERITY_VISUAL.LOW

export const getPatternLevelVisual = (level) =>
  PATTERN_LEVEL_VISUAL[String(level || '').toLowerCase()] || PATTERN_LEVEL_VISUAL.low

export const getRiskScoreVisual = (score) => {
  const value = Number(score || 0)
  if (value >= 60) return SEVERITY_VISUAL.HIGH
  if (value >= 30) return SEVERITY_VISUAL.MEDIUM
  return SEVERITY_VISUAL.LOW
}

export const getInitialsFromName = (rawName) => {
  if (!rawName) return '?'
  const name = String(rawName).trim()
  if (!name || name === '—') return '?'
  const parts = name.split(/\s+/)
  const last = parts[parts.length - 1] || ''
  const first = parts[0] || ''
  return (last.charAt(0) + (parts.length > 1 ? first.charAt(0) : '')).toUpperCase()
}

// AI Camera helper functions
export const isAiCameraSignal = (signalType) => {
  return signalType in AI_CAMERA_SIGNALS
}

export const getAiCameraSignalInfo = (signalType) => {
  return AI_CAMERA_SIGNALS[signalType] || null
}

export const getAiCameraSignalLabel = (signalType) => {
  return AI_CAMERA_SIGNALS[signalType]?.label || signalType
}

export const getAiCameraSignalSeverity = (signalType) => {
  return AI_CAMERA_SIGNALS[signalType]?.severity || 'LOW'
}

export const getAiCameraSignalIcon = (signalType) => {
  return AI_CAMERA_SIGNALS[signalType]?.icon || 'alert-triangle'
}

// Helper functions for Eye Tracking signals
export const isEyeTrackingSignal = (signalType) => {
  return signalType in EYE_TRACKING_SIGNALS
}

export const getEyeTrackingSignalInfo = (signalType) => {
  return EYE_TRACKING_SIGNALS[signalType] || null
}

export const getEyeTrackingSignalLabel = (signalType) => {
  return EYE_TRACKING_SIGNALS[signalType]?.label || signalType
}

// Helper functions for Gaze Tracking signals
export const isGazeTrackingSignal = (signalType) => {
  return signalType in GAZE_TRACKING_SIGNALS
}

export const getGazeTrackingSignalInfo = (signalType) => {
  return GAZE_TRACKING_SIGNALS[signalType] || null
}

export const getGazeTrackingSignalLabel = (signalType) => {
  return GAZE_TRACKING_SIGNALS[signalType]?.label || signalType
}

// Helper functions for Spoofing signals
export const isSpoofingSignal = (signalType) => {
  return signalType in SPOOFING_SIGNALS
}

export const getSpoofingSignalInfo = (signalType) => {
  return SPOOFING_SIGNALS[signalType] || null
}

export const getSpoofingSignalLabel = (signalType) => {
  return SPOOFING_SIGNALS[signalType]?.label || signalType
}

// Unified helper: get signal info from any category
export const getSignalInfo = (signalType) => {
  if (signalType in ALL_AI_SIGNALS) {
    return ALL_AI_SIGNALS[signalType]
  }
  return null
}

export const getSignalLabel = (signalType) => {
  if (signalType in ALL_AI_SIGNALS) {
    return ALL_AI_SIGNALS[signalType].label
  }
  return signalType
}

export const getSignalSeverity = (signalType) => {
  if (signalType in ALL_AI_SIGNALS) {
    return ALL_AI_SIGNALS[signalType].severity
  }
  return 'LOW'
}

export const getSignalIcon = (signalType) => {
  if (signalType in ALL_AI_SIGNALS) {
    return ALL_AI_SIGNALS[signalType].icon
  }
  return 'alert-circle'
}
