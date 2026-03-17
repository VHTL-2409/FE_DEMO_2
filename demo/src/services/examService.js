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

export const joinExamByCode = async (query) => {
  const payload = await apiRequest(`/api/exams/join?query=${encodeURIComponent(query)}`)
  return unwrapApiData(payload)
}

export const createExam = async ({
  title,
  description = '',
  durationMinutes = 60,
  startTime = null,
  endTime = null,
  isActive = false,
  monitorTabSwitch,
  monitorBlur,
  monitorExitFullscreen,
  monitorCopyPaste,
  monitorIdleTime,
  monitorDevtools,
  monitorDuplicateIp,
  monitorFastSubmit,
  monitorRightClick,
  monitorPrintScreen,
  monitorRapidQuestionSwitch,
  monitorMultiMonitor,
  requireCameraMic
}) => {
  const payload = await apiRequest('/api/exams', {
    method: 'POST',
    body: JSON.stringify({
      title,
      description,
      durationMinutes,
      startTime: toLocalDateTimeOrNull(startTime),
      endTime: toLocalDateTimeOrNull(endTime),
      isActive,
      monitorTabSwitch,
      monitorBlur,
      monitorExitFullscreen,
      monitorCopyPaste,
      monitorIdleTime,
      monitorDevtools,
      monitorDuplicateIp,
      monitorFastSubmit,
      monitorRightClick,
      monitorPrintScreen,
      monitorRapidQuestionSwitch,
      monitorMultiMonitor,
      requireCameraMic
    })
  })

  return unwrapApiData(payload)
}

export const updateExam = async (examId, {
  title,
  description = '',
  durationMinutes,
  startTime = null,
  endTime = null,
  isActive = false,
  monitorTabSwitch,
  monitorBlur,
  monitorExitFullscreen,
  monitorCopyPaste,
  monitorIdleTime,
  monitorDevtools,
  monitorDuplicateIp,
  monitorFastSubmit,
  monitorRightClick,
  monitorPrintScreen,
  monitorRapidQuestionSwitch,
  monitorMultiMonitor,
  requireCameraMic
}) => {
  const payload = await apiRequest(`/api/exams/${examId}`, {
    method: 'PUT',
    body: JSON.stringify({
      title,
      description,
      durationMinutes,
      startTime: toLocalDateTimeOrNull(startTime),
      endTime: toLocalDateTimeOrNull(endTime),
      isActive,
      monitorTabSwitch,
      monitorBlur,
      monitorExitFullscreen,
      monitorCopyPaste,
      monitorIdleTime,
      monitorDevtools,
      monitorDuplicateIp,
      monitorFastSubmit,
      monitorRightClick,
      monitorPrintScreen,
      monitorRapidQuestionSwitch,
      monitorMultiMonitor,
      requireCameraMic
    })
  })

  return unwrapApiData(payload)
}

export const getPracticeOptions = async () => {
  const payload = await apiRequest('/api/exams/practice-options')
  return unwrapApiData(payload) || { maxQuestions: 50 }
}

export const createPracticeExam = async ({ questionCount = 20, durationMinutes = 30 } = {}) => {
  const payload = await apiRequest('/api/exams/practice', {
    method: 'POST',
    body: JSON.stringify({
      questionCount,
      durationMinutes
    })
  })
  return unwrapApiData(payload)
}

export const createPracticeFromFile = async (file, durationMinutes = 30) => {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('durationMinutes', String(durationMinutes))
  const payload = await apiRequest('/api/exams/practice-from-file', {
    method: 'POST',
    body: formData
  })
  return unwrapApiData(payload)
}

export const deleteExam = async (examId) => {
  await apiRequest(`/api/exams/${examId}`, {
    method: 'DELETE'
  })
}

export const getAnswerSimilarity = async (examId) => {
  const payload = await apiRequest(`/api/exams/${examId}/answer-similarity`)
  return unwrapApiData(payload) || []
}
