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
