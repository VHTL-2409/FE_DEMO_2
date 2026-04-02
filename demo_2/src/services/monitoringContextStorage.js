const KEY = 'monitoring:lastCtx'

/**
 * Đọc query phiên giám sát cuối (examId + metadata) để khôi phục khi vào /session không có query.
 */
export function readMonitoringSessionQuery() {
  if (typeof sessionStorage === 'undefined') return null
  try {
    const raw = sessionStorage.getItem(KEY)
    if (!raw) return null
    const o = JSON.parse(raw)
    if (o == null || o.examId == null || String(o.examId).trim() === '') return null
    const q = { examId: String(o.examId) }
    if (o.title != null && String(o.title) !== '') q.title = String(o.title)
    if (o.code != null && String(o.code) !== '') q.code = String(o.code)
    if (o.meta != null && String(o.meta) !== '') q.meta = String(o.meta)
    if (o.durationMinutes != null && String(o.durationMinutes) !== '') {
      q.durationMinutes = String(o.durationMinutes)
    }
    if (o.startTime != null && String(o.startTime) !== '') q.startTime = String(o.startTime)
    return q
  } catch {
    return null
  }
}

export function writeMonitoringSessionQuery(query) {
  if (typeof sessionStorage === 'undefined' || !query) return
  const examId = query.examId
  if (examId == null || String(examId).trim() === '') return
  try {
    sessionStorage.setItem(
      KEY,
      JSON.stringify({
        examId: String(examId),
        title: query.title != null ? String(query.title) : '',
        code: query.code != null ? String(query.code) : '',
        meta: query.meta != null ? String(query.meta) : '',
        durationMinutes: query.durationMinutes != null ? String(query.durationMinutes) : '',
        startTime: query.startTime != null ? String(query.startTime) : ''
      })
    )
  } catch {
    /* ignore */
  }
}
