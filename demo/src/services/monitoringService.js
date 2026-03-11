import { apiRequest, unwrapApiData } from './apiClient'

export const sendMonitoringEvent = async (attemptId, eventType, details = '') => {
  const payload = await apiRequest(`/api/attempts/${attemptId}/monitoring/events`, {
    method: 'POST',
    body: JSON.stringify({ eventType, details })
  })
  return unwrapApiData(payload)
}

export const listMonitoringTimeline = async (attemptId) => {
  const payload = await apiRequest(`/api/attempts/${attemptId}/monitoring/timeline`)
  return unwrapApiData(payload) || []
}

export const sendTeacherWarning = async (attemptId, message = '') => {
  const query = message ? `?message=${encodeURIComponent(message)}` : ''
  const payload = await apiRequest(`/api/attempts/${attemptId}/monitoring/warning${query}`, {
    method: 'POST'
  })
  return unwrapApiData(payload)
}

export const invalidateAttempt = async (attemptId, reason = '') => {
  const query = reason ? `?reason=${encodeURIComponent(reason)}` : ''
  const payload = await apiRequest(`/api/attempts/${attemptId}/monitoring/invalidate${query}`, {
    method: 'POST'
  })
  return unwrapApiData(payload)
}
