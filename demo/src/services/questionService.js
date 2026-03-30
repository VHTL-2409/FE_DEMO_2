import { apiRequest, unwrapApiData, API_BASE_URL } from './apiClient'

/**
 * Re-export tất cả hàm import từ importService.js
 * (backward compatibility — các component cũ vẫn import từ đây)
 */
export {
  uploadImportJob,
  getImportJobStatus,
  getImportJobPreview,
  reviewImportJob,
  confirmImportJob,
  cancelImportJob,
  importQuestionsFromAzota,
  importQuestionsFromFile,
  previewImportFile,
  waitForImportJob,
  validateImportFile,
  SUPPORTED_IMPORT_FORMATS,
  IMPORT_FILE_ACCEPT,
  IMPORT_FILE_MAX_SIZE_MB,
  IMPORT_FILE_MAX_SIZE,
} from './importService'

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

// previewImportFile và importQuestionsFromFile đã được chuyển sang importService.js
// Xem re-export ở đầu file
