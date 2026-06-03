import { apiRequest, unwrapApiData } from './apiClient'

// Re-export existing monitoring actions from monitoringService for convenience
export {
  fetchAttemptRisk,
  listMonitoringTimeline,
  listMonitoringAudit,
  sendTeacherWarning,
  pauseAttempt,
  resumeAttempt,
  invalidateAttempt,
  batchWarn,
  batchPause,
  batchResume,
  batchInvalidate,
} from './monitoringService'

export {
  fetchProctorSessionAlerts,
  fetchProctorRisk,
  fetchProctorTimeline,
  fetchLatestCameraFrame,
  fetchIdentityCheck,
  fetchIdentityChecks,
  reviewIdentityCheck,
  reviewProctorFlag
} from './proctorService'

// ─── Exam-level monitoring ─────────────────────────────────────────────────

export const fetchExamSummary = async (examId) => {
  const payload = await apiRequest(`/api/exams/${examId}`, { suppressToast: true })
  return unwrapApiData(payload)
}

export const fetchExamAttempts = async (examId) => {
  const payload = await apiRequest(`/api/exams/${examId}/attempts`, { suppressToast: true })
  return unwrapApiData(payload) || []
}

export const fetchExamAttemptsFilter = async (examId, filters = {}) => {
  const params = new URLSearchParams()
  if (filters.status) params.set('status', filters.status)
  if (filters.search) params.set('student', filters.search)
  if (filters.riskBand === 'SUSPICIOUS') {
    params.set('riskMin', '31')
    params.set('suspicious', 'true')
  }
  const qs = params.toString()
  const path = qs ? `/api/exams/${examId}/attempts/filter?${qs}` : `/api/exams/${examId}/attempts/filter`
  const payload = await apiRequest(path, { suppressToast: true })
  const data = unwrapApiData(payload)
  return Array.isArray(data) ? data : (data?.items || [])
}

// ─── Attempt detail ───────────────────────────────────────────────────────

export const fetchAttemptDetail = async (attemptId) => {
  const payload = await apiRequest(`/api/attempts/${attemptId}`, { suppressToast: true })
  return unwrapApiData(payload)
}

// ─── Notes / Events ───────────────────────────────────────────────────────

export const addMonitoringNote = async (attemptId, note) => {
  const payload = await apiRequest(`/api/attempts/${attemptId}/monitoring/events`, {
    method: 'POST',
    body: JSON.stringify({ eventType: 'NOTE', details: note })
  })
  return unwrapApiData(payload)
}
