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

export const listExamAssignments = async (examId) => {
  const payload = await apiRequest(`/api/exams/${examId}/assignments`)
  return Array.isArray(payload) ? payload : unwrapApiData(payload) || []
}

export const createExamAssignment = async (examId, {
  title,
  openAt = null,
  closeAt = null,
  maxAttempts = 1,
  allowReviewAfterSubmit = true,
  isPublished = true
}) => {
  const payload = await apiRequest(`/api/exams/${examId}/assignments`, {
    method: 'POST',
    body: JSON.stringify({
      title,
      openAt: toLocalDateTimeOrNull(openAt),
      closeAt: toLocalDateTimeOrNull(closeAt),
      maxAttempts,
      allowReviewAfterSubmit,
      isPublished
    })
  })

  return Array.isArray(payload) ? payload : unwrapApiData(payload)
}

export const updateExamAssignment = async (examId, assignmentId, {
  title,
  openAt = null,
  closeAt = null,
  maxAttempts = 1,
  allowReviewAfterSubmit = true,
  isPublished = true
}) => {
  const payload = await apiRequest(`/api/exams/${examId}/assignments/${assignmentId}`, {
    method: 'PUT',
    body: JSON.stringify({
      title,
      openAt: toLocalDateTimeOrNull(openAt),
      closeAt: toLocalDateTimeOrNull(closeAt),
      maxAttempts,
      allowReviewAfterSubmit,
      isPublished
    })
  })

  return unwrapApiData(payload)
}

export const publishExamAssignment = async (examId, assignmentId, isPublished = true) => {
  const payload = await apiRequest(`/api/exams/${examId}/assignments/${assignmentId}/publish?isPublished=${isPublished ? 'true' : 'false'}`, {
    method: 'PATCH'
  })

  return unwrapApiData(payload)
}

export const deleteExamAssignment = async (examId, assignmentId) => {
  await apiRequest(`/api/exams/${examId}/assignments/${assignmentId}`, {
    method: 'DELETE'
  })
}
