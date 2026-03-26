import { apiRequest, unwrapApiData, API_BASE_URL } from './apiClient'

/** Chuẩn file: CSV, XLSX, PDF, DOCX. Tối đa 10MB. */
export const FILE_FORMAT_DESC = 'Hỗ trợ CSV, XLSX, PDF, Word. PDF/Word: Câu N. nội dung | A) B) C) D) | Đáp án: A'

/** URL tải mẫu CSV chuẩn */
export const getTemplateDownloadUrl = (format = 'csv') => {
  const q = format === 'xlsx' ? '?format=xlsx' : ''
  return `${API_BASE_URL}/api/questions/template${q}`
}

export const parseQuestionJson = (rawValue, fallback = null) => {
  if (rawValue == null || rawValue === '') return fallback
  if (typeof rawValue !== 'string') return rawValue
  try {
    return JSON.parse(rawValue)
  } catch {
    return fallback
  }
}

export const serializeQuestionValue = (value, fallback = '') => {
  if (value == null) return fallback
  if (typeof value === 'string') return value
  return JSON.stringify(value)
}

export const parseQuestionOptions = (rawOptions) => {
  const parsed = parseQuestionJson(rawOptions, [])
  if (!Array.isArray(parsed)) {
    return []
  }

  return parsed.map((item, index) => ({
    id: item?.id ? String(item.id) : String(index + 1),
    text: item?.text || item?.content || item?.label || ''
  }))
}

export const listExamQuestions = async (examId) => {
  const payload = await apiRequest(`/api/exams/${examId}/questions`)
  return unwrapApiData(payload) || []
}

export const createQuestion = async (examId, {
  content,
  type = 'SINGLE_CHOICE',
  scoreWeight = 1,
  options = [],
  correctAnswer = '',
  difficulty = null,
  metadata = null,
  attachments = null
}) => {
  const payload = await apiRequest(`/api/exams/${examId}/questions`, {
    method: 'POST',
    body: JSON.stringify({
      content,
      type,
      scoreWeight,
      options: serializeQuestionValue(options, '[]'),
      correctAnswer: serializeQuestionValue(correctAnswer, ''),
      difficulty,
      metadata: serializeQuestionValue(metadata, null),
      attachments: serializeQuestionValue(attachments, null)
    })
  })

  return unwrapApiData(payload)
}

/** Đếm số câu hợp lệ trong tệp (cùng logic parse với import). */
export const previewImportFile = async (file) => {
  const formData = new FormData()
  formData.append('file', file)
  const payload = await apiRequest('/api/questions/file-preview', {
    method: 'POST',
    body: formData
  })
  return unwrapApiData(payload)
}

export const importQuestionsFromFile = async (examId, file, options = {}) => {
  const formData = new FormData()
  formData.append('file', file)
  if (options.questionCount != null && options.questionCount !== '') {
    formData.append('questionCount', String(options.questionCount))
  }

  const payload = await apiRequest(`/api/exams/${examId}/questions/import`, {
    method: 'POST',
    body: formData
  })

  return unwrapApiData(payload)
}

export const importQuestionsFromXlsx = async (examId, file) => {
  return importQuestionsFromFile(examId, file)
}

export const uploadImportJob = async (file, { examId } = {}) => {
  const formData = new FormData()
  formData.append('file', file)
  if (examId != null) {
    formData.append('examId', String(examId))
  }
  const payload = await apiRequest('/api/v1/import/upload', {
    method: 'POST',
    body: formData
  })
  return unwrapApiData(payload)
}

export const getImportJobStatus = async (jobId) => {
  const payload = await apiRequest(`/api/v1/import/status/${jobId}`)
  return unwrapApiData(payload)
}

export const getImportJobPreview = async (jobId) => {
  const payload = await apiRequest(`/api/v1/import/preview/${jobId}`)
  return unwrapApiData(payload)
}

export const reviewImportJob = async (jobId, reviewPayload) => {
  const payload = await apiRequest(`/api/v1/import/review/${jobId}`, {
    method: 'PUT',
    body: JSON.stringify(reviewPayload)
  })
  return unwrapApiData(payload)
}

export const confirmImportJob = async (jobId, { examId } = {}) => {
  const query = examId != null ? `?examId=${encodeURIComponent(examId)}` : ''
  const payload = await apiRequest(`/api/v1/import/confirm/${jobId}${query}`, {
    method: 'POST'
  })
  return unwrapApiData(payload)
}

export const cancelImportJob = async (jobId) => {
  const payload = await apiRequest(`/api/v1/import/cancel/${jobId}`, {
    method: 'DELETE'
  })
  return unwrapApiData(payload)
}

export const waitForImportJob = async (jobId, { intervalMs = 800, maxAttempts = 60 } = {}) => {
  for (let attempt = 0; attempt < maxAttempts; attempt += 1) {
    const status = await getImportJobStatus(jobId)
    if (status?.status === 'DONE' || status?.status === 'FAILED' || status?.status === 'CANCELLED') {
      return status
    }
    await new Promise((resolve) => window.setTimeout(resolve, intervalMs))
  }
  throw new Error('Import job xử lý quá lâu. Vui lòng thử tải lại preview.')
}
