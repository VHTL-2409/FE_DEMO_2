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

export const createPracticeFromFile = async (file, durationMinutes = 30, questionCount = null) => {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('durationMinutes', String(durationMinutes))
  if (questionCount != null && questionCount !== '') {
    formData.append('questionCount', String(questionCount))
  }
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

export const createNewSession = async (examId, { startTime, endTime, durationMinutes }) => {
  const payload = await apiRequest(`/api/exams/${examId}/sessions`, {
    method: 'POST',
    body: JSON.stringify({
      startTime: toLocalDateTimeOrNull(startTime),
      endTime: toLocalDateTimeOrNull(endTime),
      durationMinutes: durationMinutes || null
    })
  })
  return unwrapApiData(payload)
}

export const getAnswerSimilarity = async (examId) => {
  const payload = await apiRequest(`/api/exams/${examId}/answer-similarity`)
  return unwrapApiData(payload) || []
}

export const getQuestionWrongStats = async (examId) => {
  const payload = await apiRequest(`/api/exams/${examId}/question-wrong-stats`)
  return unwrapApiData(payload) || []
}

// ─── Publish / Unpublish / Archive ─────────────────────────────────

export const publishExam = async (examId) => {
  const payload = await apiRequest(`/api/exams/${examId}/publish`, { method: 'PATCH' })
  return unwrapApiData(payload)
}

export const unpublishExam = async (examId) => {
  const payload = await apiRequest(`/api/exams/${examId}/unpublish`, { method: 'PATCH' })
  return unwrapApiData(payload)
}

export const archiveExam = async (examId) => {
  const payload = await apiRequest(`/api/exams/${examId}/archive`, { method: 'PATCH' })
  return unwrapApiData(payload)
}

export const unarchiveExam = async (examId) => {
  const payload = await apiRequest(`/api/exams/${examId}/unarchive`, { method: 'PATCH' })
  return unwrapApiData(payload)
}

export const duplicateExam = async (examId) => {
  const payload = await apiRequest(`/api/exams/${examId}/duplicate`, { method: 'POST' })
  return unwrapApiData(payload)
}

// ─── Bulk operations ────────────────────────────────────────────────

export const bulkPublishExams = async (examIds) => {
  const payload = await apiRequest('/api/exams/bulk/publish', {
    method: 'POST',
    body: JSON.stringify({ examIds })
  })
  return unwrapApiData(payload) || []
}

export const bulkArchiveExams = async (examIds) => {
  const payload = await apiRequest('/api/exams/bulk/archive', {
    method: 'POST',
    body: JSON.stringify({ examIds })
  })
  return unwrapApiData(payload) || []
}

export const bulkDeleteExams = async (examIds) => {
  await apiRequest('/api/exams/bulk/delete', {
    method: 'POST',
    body: JSON.stringify({ examIds })
  })
}

// ─── Export helpers ─────────────────────────────────────────────────

export const exportExamAsJson = (exam) => {
  const blob = new Blob([JSON.stringify(exam, null, 2)], { type: 'application/json' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `exam-${exam.id}-${Date.now()}.json`
  a.click()
  URL.revokeObjectURL(url)
}

export const exportExamsAsCsv = (exams) => {
  const headers = ['ID', 'Mã', 'Tên đề thi', 'Mô tả', 'Thời lượng (phút)', 'Bắt đầu', 'Kết thúc', 'Trạng thái', 'Câu hỏi', 'Học sinh']
  const rows = exams.map(e => [
    e.id,
    e.code || '',
    `"${(e.title || '').replace(/"/g, '""')}"`,
    `"${(e.description || '').replace(/"/g, '""')}"`,
    e.durationMinutes || '',
    e.startTime ? new Date(e.startTime).toLocaleString('vi-VN') : '',
    e.endTime ? new Date(e.endTime).toLocaleString('vi-VN') : '',
    e.isActive ? 'Hoạt động' : 'Nháp',
    e.questionCount || 0,
    e.participantCount || 0
  ])
  const csv = [headers.join(','), ...rows.map(r => r.join(','))].join('\n')
  const blob = new Blob(['\ufeff' + csv], { type: 'text/csv;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `danh-sach-de-thi-${Date.now()}.csv`
  a.click()
  URL.revokeObjectURL(url)
}
