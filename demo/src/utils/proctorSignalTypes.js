const SIGNAL_TYPE_ALIASES = Object.freeze({
  BLUR: 'WINDOW_BLUR',
  FULLSCREEN_EVASION: 'EXIT_FULLSCREEN',
  FULLSCREEN_VIOLATION: 'EXIT_FULLSCREEN',
  ATTEMPT_START: 'ATTEMPT_STARTED',
  ATTEMPT_SUBMIT: 'ATTEMPT_SUBMITTED',
  AUTO_SUBMIT: 'AUTO_SUBMITTED',
  DRAFT_SAVE: 'DRAFT_SAVED',
  TEACHER_WARNING: 'WARNING_SENT',
  TEACHER_PAUSE: 'ATTEMPT_PAUSED',
  TEACHER_RESUME: 'ATTEMPT_RESUMED',
  TEACHER_INVALIDATE: 'ATTEMPT_STOPPED',
  COPY: 'COPY_PASTE',
  PASTE: 'COPY_PASTE',
  CLIPBOARD_ABUSE: 'COPY_PASTE',
  CLIPBOARD_BURST: 'COPY_PASTE',
  COPY_ATTEMPT: 'COPY_PASTE',
  CUT_ATTEMPT: 'COPY_PASTE',
  PASTE_ATTEMPT: 'COPY_PASTE',
  LONG_PASTE: 'COPY_PASTE',
  DEVTOOLS: 'DEVTOOLS_OPEN',
  DEVTOOLS_DETECTED: 'DEVTOOLS_OPEN',
  INSPECT_ATTEMPT: 'DEVTOOLS_OPEN',
  IP_ANOMALY: 'IP_CHANGED',
  AI_MULTIPLE_FACES: 'MULTIPLE_FACES',
  AI_FACE_MISSING: 'FACE_NOT_DETECTED',
  AI_LOOKING_AWAY: 'GAZE_OFF_SCREEN',
  AI_NO_MIC: 'NO_MIC',
  NO_MICROPHONE: 'NO_MIC',
  MIC_OFF: 'NO_MIC',
  MIC_DISABLED: 'NO_MIC',
  MICROPHONE_OFF: 'NO_MIC'
})

export function normalizeSignalType(value) {
  const text = String(value || '').trim().toUpperCase()
  if (!text) return ''
  return SIGNAL_TYPE_ALIASES[text] || text
}

export function uniqueSignalTypes(values = []) {
  const seen = new Set()
  const unique = []
  for (const value of values) {
    const type = normalizeSignalType(value)
    if (!type || seen.has(type)) continue
    seen.add(type)
    unique.push(type)
  }
  return unique
}
