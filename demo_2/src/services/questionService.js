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

// Re-export import job functions from importService.js (single source of truth)
export { uploadImportJob } from './importService.js'
export { getImportJobStatus } from './importService.js'
export { getImportJobPreview } from './importService.js'
export { reviewImportJob } from './importService.js'
export { confirmImportJob } from './importService.js'
export { cancelImportJob } from './importService.js'
export { waitForImportJob } from './importService.js'
export { importQuestionsFromFile } from './importService.js'
export { importQuestionsFromXlsx } from './importService.js'
export { previewImportFile } from './importService.js'
