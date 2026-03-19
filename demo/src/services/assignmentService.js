import { apiRequest, unwrapApiData } from './apiClient'

const toIsoOrNull = (value) => {
  if (!value) return null
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? null : date.toISOString()
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
      openAt: toIsoOrNull(openAt),
      closeAt: toIsoOrNull(closeAt),
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
      openAt: toIsoOrNull(openAt),
      closeAt: toIsoOrNull(closeAt),
      maxAttempts,
      allowReviewAfterSubmit,
      isPublished
    })
  })

  return unwrapApiData(payload)
}
