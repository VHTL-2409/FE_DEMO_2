import { apiRequest, unwrapApiData } from './apiClient'

export const startProctoringSession = async (payload) => {
  const response = await apiRequest('/api/v1/proctor/start', {
    method: 'POST',
    body: JSON.stringify(payload)
  })
  return unwrapApiData(response)
}

export const reportProctorSignal = async (payload) => {
  const response = await apiRequest('/api/v1/proctor/signals', {
    method: 'POST',
    body: JSON.stringify(payload),
    suppressToast: true
  })
  return unwrapApiData(response)
}

export const analyzeProctorFrame = async (payload) => {
  const response = await apiRequest('/api/v1/proctor/ai/frame', {
    method: 'POST',
    body: JSON.stringify(payload),
    suppressToast: true
  })
  return unwrapApiData(response)
}

export const sendProctorCameraFrame = async (payload) => {
  const response = await apiRequest('/api/v1/proctor/camera/frame', {
    method: 'POST',
    body: JSON.stringify(payload),
    suppressToast: true
  })
  return unwrapApiData(response)
}

export const fetchLatestCameraFrame = async (attemptId) => {
  const response = await apiRequest(`/api/v1/proctor/camera/frame/attempt/${attemptId}/latest`, {
    suppressToast: true
  })
  return unwrapApiData(response)
}

export const fetchProctorRisk = async (attemptId) => {
  const response = await apiRequest(`/api/v1/proctor/attempts/${attemptId}/risk`, { suppressToast: true })
  return unwrapApiData(response)
}

export const fetchProctorTimeline = async (attemptId, options = {}) => {
  const params = new URLSearchParams()
  if (options.page) params.set('page', String(options.page))
  if (options.size) params.set('size', String(options.size))
  if (options.eventType) params.set('eventType', options.eventType)
  const qs = params.toString()
  const response = await apiRequest(`/api/v1/proctor/attempts/${attemptId}/timeline${qs ? `?${qs}` : ''}`, { suppressToast: true })
  return unwrapApiData(response)
}

export const verifyStudentIdentity = async (payload) => {
  const response = await apiRequest('/api/v1/proctor/identity/verify', {
    method: 'POST',
    body: JSON.stringify(payload),
    suppressToast: true
  })
  return unwrapApiData(response)
}

export const fetchIdentityCheck = async (attemptId) => {
  const response = await apiRequest(`/api/v1/proctor/attempts/${attemptId}/identity-check`, {
    suppressToast: true
  })
  return unwrapApiData(response)
}

export const fetchProctorSessionAlerts = async (sessionId) => {
  const response = await apiRequest(`/api/v1/proctor/sessions/${sessionId}/alerts`, { suppressToast: true })
  return unwrapApiData(response) || []
}

export const reviewProctorFlag = async (flagId, payload) => {
  const response = await apiRequest(`/api/v1/proctor/flags/${flagId}`, {
    method: 'PATCH',
    body: JSON.stringify(payload)
  })
  return unwrapApiData(response)
}

// AI Camera Monitoring APIs
export const fetchCameraStatus = async (examId) => {
  const response = await apiRequest(`/api/v1/proctor/exams/${examId}/camera-status`, { suppressToast: true })
  return unwrapApiData(response)
}

export const fetchCameraAlerts = async (examId) => {
  const response = await apiRequest(`/api/v1/proctor/exams/${examId}/camera-alerts`, { suppressToast: true })
  return unwrapApiData(response)
}

export const acknowledgeCameraAlert = async (alertId) => {
  const response = await apiRequest(`/api/v1/proctor/alerts/${alertId}/acknowledge`, {
    method: 'POST'
  })
  return unwrapApiData(response)
}

export const dismissCameraAlert = async (alertId) => {
  const response = await apiRequest(`/api/v1/proctor/alerts/${alertId}/dismiss`, {
    method: 'POST'
  })
  return unwrapApiData(response)
}
