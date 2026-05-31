import { apiRequest, unwrapApiData } from './apiClient'

/** Dang nhap Azota */
export const azotaLogin = async (phone, password) => {
  const payload = await apiRequest('/api/v1/azota/login', {
    method: 'POST',
    body: JSON.stringify({ phone, password })
  })
  return unwrapApiData(payload)
}

/** Lay danh sach de thi tu Azota */
export const getAzotaQuestionBanks = async (azotaToken) => {
  const payload = await apiRequest('/api/v1/azota/question-banks', {
    method: 'GET',
    headers: { 'X-Azota-Token': azotaToken }
  })
  return unwrapApiData(payload) || []
}

/** Lay cau hoi chi tiet tu 1 de thi Azota */
export const getAzotaExamQuestions = async (examId, azotaToken) => {
  const payload = await apiRequest(`/api/v1/azota/question-banks/${encodeURIComponent(examId)}/questions`, {
    method: 'GET',
    headers: { 'X-Azota-Token': azotaToken }
  })
  return unwrapApiData(payload)
}

/** Lay trang thai dich vu Azota */
export const getAzotaStatus = async () => {
  const payload = await apiRequest('/api/v1/azota/status')
  return unwrapApiData(payload)
}
