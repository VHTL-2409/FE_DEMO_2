import { apiRequest, unwrapApiData } from './apiClient'

const toLocalDateTimeOrNull = (value) => {
  if (!value) return null
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return null

  const yyyy = date.getFullYear()
  const mm = String(date.getMonth() + 1).padStart(2, '0')
  const dd = String(date.getDate()).padStart(2, '0')
  const hh = String(date.getHours()).padStart(2, '0')
  const min = String(date.getMinutes()).padStart(2, '0')
  const sec = String(date.getSeconds()).padStart(2, '0')
  return `${yyyy}-${mm}-${dd}T${hh}:${min}:${sec}`
}

export const listExams = async () => {
  const payload = await apiRequest('/api/exams')
  return unwrapApiData(payload) || []
}

export const getExamDetail = async (examId) => {
  const payload = await apiRequest(`/api/exams/${examId}`)
  return unwrapApiData(payload)
}

export const createExam = async ({ title, description = '', durationMinutes = 60, startTime = null, endTime = null, isActive = false }) => {
  const payload = await apiRequest('/api/exams', {
    method: 'POST',
    body: JSON.stringify({
      title,
      description,
      durationMinutes,
      startTime: toLocalDateTimeOrNull(startTime),
      endTime: toLocalDateTimeOrNull(endTime),
      isActive
    })
  })

  return unwrapApiData(payload)
}

export const updateExam = async (examId, { title, description = '', durationMinutes, startTime = null, endTime = null, isActive = false }) => {
  const payload = await apiRequest(`/api/exams/${examId}`, {
    method: 'PUT',
    body: JSON.stringify({
      title,
      description,
      durationMinutes,
      startTime: toLocalDateTimeOrNull(startTime),
      endTime: toLocalDateTimeOrNull(endTime),
      isActive
    })
  })

  return unwrapApiData(payload)
}

export const deleteExam = async (examId) => {
  await apiRequest(`/api/exams/${examId}`, {
    method: 'DELETE'
  })
}
