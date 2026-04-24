import { apiRequest, unwrapApiData } from './apiClient'

export const updateDeviceStatus = async (attemptId, cameraOn, micOn) => {
  await apiRequest(`/api/attempts/${attemptId}/monitoring/device-status`, {
    method: 'PATCH',
    body: JSON.stringify({ cameraOn, micOn })
  })
}

export const sendMonitoringEvent = async (attemptId, eventType, details = '') => {
  const payload = await apiRequest(`/api/attempts/${attemptId}/monitoring/events`, {
    method: 'POST',
    body: JSON.stringify({ eventType, details })
  })
  return unwrapApiData(payload)
}

export const sendMonitoringEventBatch = async (attemptId, batchPayload) => {
  const payload = await apiRequest(`/api/attempts/${attemptId}/monitoring/events/batch`, {
    method: 'POST',
    body: JSON.stringify(batchPayload),
    suppressToast: true
  })
  return unwrapApiData(payload)
}

export const sendMonitoringHeartbeat = async (attemptId, heartbeatPayload) => {
  const payload = await apiRequest(`/api/attempts/${attemptId}/monitoring/heartbeat`, {
    method: 'POST',
    body: JSON.stringify(heartbeatPayload),
    suppressToast: true
  })
  return unwrapApiData(payload)
}

export const fetchAttemptRisk = async (attemptId) => {
  const payload = await apiRequest(`/api/attempts/${attemptId}/monitoring/risk`)
  return unwrapApiData(payload)
}

export const listMonitoringTimeline = async (attemptId) => {
  const payload = await apiRequest(`/api/attempts/${attemptId}/monitoring/timeline`)
  return unwrapApiData(payload) || []
}

export const listMonitoringAudit = async (attemptId) => {
  const payload = await apiRequest(`/api/attempts/${attemptId}/monitoring/audit`)
  return unwrapApiData(payload) || []
}

export const sendTeacherWarning = async (attemptId, message = '') => {
  const query = message ? `?message=${encodeURIComponent(message)}` : ''
  const payload = await apiRequest(`/api/attempts/${attemptId}/monitoring/warning${query}`, {
    method: 'POST'
  })
  return unwrapApiData(payload)
}

export const pauseAttempt = async (attemptId, reason = '') => {
  const query = reason ? `?reason=${encodeURIComponent(reason)}` : ''
  const payload = await apiRequest(`/api/attempts/${attemptId}/monitoring/pause${query}`, {
    method: 'POST'
  })
  return unwrapApiData(payload)
}

export const resumeAttempt = async (attemptId, message = '') => {
  const query = message ? `?message=${encodeURIComponent(message)}` : ''
  const payload = await apiRequest(`/api/attempts/${attemptId}/monitoring/resume${query}`, {
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
