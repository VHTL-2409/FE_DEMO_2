import { apiRequest, unwrapApiData } from './apiClient'

export const startAttempt = async (examId) => {
  const payload = await apiRequest(`/api/exams/${examId}/attempts/start`, {
    method: 'POST'
  })
  return unwrapApiData(payload)
}

export const getDraftAnswers = async (attemptId) => {
  const payload = await apiRequest(`/api/attempts/${attemptId}/draft-answers`)
  return unwrapApiData(payload)
}

export const saveDraftAnswers = async (attemptId, answers) => {
  const payload = await apiRequest(`/api/attempts/${attemptId}/draft-answers`, {
    method: 'PUT',
    body: JSON.stringify(answers)
  })
  return unwrapApiData(payload)
}

export const submitAttempt = async (attemptId, answers) => {
  const payload = await apiRequest(`/api/attempts/${attemptId}/submit`, {
    method: 'POST',
    body: JSON.stringify({ answers })
  })
  return unwrapApiData(payload)
}

export const listMyAttempts = async ({ type } = {}) => {
  const query = type ? `?type=${encodeURIComponent(type)}` : ''
  const payload = await apiRequest(`/api/attempts/my${query}`)
  return unwrapApiData(payload) || []
}

export const listExamAttempts = async (examId) => {
  const payload = await apiRequest(`/api/exams/${examId}/attempts`)
  return unwrapApiData(payload) || []
}

export const getAttemptReport = async (attemptId) => {
  const payload = await apiRequest(`/api/attempts/${attemptId}/report`)
  return unwrapApiData(payload)
}

export const getAttemptDetail = async (attemptId) => {
  const payload = await apiRequest(`/api/attempts/${attemptId}`)
  return unwrapApiData(payload)
}
