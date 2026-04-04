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

/** A–Z theo thứ tự cho MCQ khi server/importer không gán id. */
const defaultOptionLetterId = (index) => {
  if (index >= 0 && index < 26) return String.fromCharCode(65 + index)
  return String(index + 1)
}

/**
 * Chuẩn hoá options từ API (JSON string hoặc đã parse).
 * Hỗ trợ: [{id,text}], mảng chuỗi, object có value/title/body, v.v.
 */
export const parseQuestionOptions = (rawOptions) => {
  const parsed = parseQuestionJson(rawOptions, [])
  if (!Array.isArray(parsed)) {
    return []
  }

  return parsed.map((item, index) => {
    const letter = defaultOptionLetterId(index)
    let id = letter
    let text = ''

    if (item == null) {
      id = letter
    } else if (typeof item === 'string' || typeof item === 'number') {
      const s = String(item).trim()
      if (/^[A-Za-z]$/.test(s)) {
        id = s.toUpperCase()
        text = s
      } else {
        id = letter
        text = s
      }
    } else if (typeof item === 'object') {
      const rawId = item.id
      id =
        rawId != null && String(rawId).trim() !== ''
          ? String(rawId).trim()
          : letter
      text = String(
        item.text ??
          item.content ??
          item.label ??
          item.value ??
          item.title ??
          item.body ??
          item.option ??
          ''
      ).trim()
    }

    if (!text) {
      text = `Phương án ${id}`
    }

    return { id, text }
  })
}

export const listExamQuestions = async (examId) => {
  const payload = await apiRequest(`/api/exams/${examId}/questions`)
  const data = unwrapApiData(payload)
  return Array.isArray(data) ? data : []
}

/**
 * Map câu hỏi từ form (QuestionBuilder / QuestionEditor) sang payload API create.
 * Hỗ trợ options dạng mảng { id, text } hoặc optionA–D.
 */
export function buildCreatePayloadFromFormQuestion(q) {
  const content = (q?.content || '').trim()
  const type = (q?.type || 'SINGLE_CHOICE').toString().toUpperCase()
  let options = []
  if (Array.isArray(q?.options) && q.options.length > 0) {
    options = q.options.map((o, i) => ({
      id: String(o?.id ?? i + 1),
      text: String(o?.text ?? o?.content ?? o?.label ?? '').trim() || `Lựa chọn ${i + 1}`
    }))
  } else {
    const labels = ['A', 'B', 'C', 'D']
    ;['optionA', 'optionB', 'optionC', 'optionD'].forEach((key, i) => {
      const text = (q?.[key] || '').trim()
      if (text) options.push({ id: labels[i], text })
    })
  }
  if ((type === 'SINGLE_CHOICE' || type === 'MULTIPLE_CHOICE') && options.length < 2) {
    options = [
      { id: 'A', text: (q?.optionA || '').trim() || 'A' },
      { id: 'B', text: (q?.optionB || '').trim() || 'B' },
      { id: 'C', text: (q?.optionC || '').trim() || 'C' },
      { id: 'D', text: (q?.optionD || '').trim() || 'D' }
    ]
  }
  const sw = Number(q?.scoreWeight ?? q?.score ?? 1)
  const scoreWeight = Number.isFinite(sw) && sw > 0 ? sw : 1
  let correctAnswer = q?.correctAnswer != null ? String(q.correctAnswer).trim() : ''
  if (
    (type === 'SINGLE_CHOICE' || type === 'MULTIPLE_CHOICE') &&
    !correctAnswer &&
    options.length > 0
  ) {
    correctAnswer = options[0].id
  }
  return {
    content,
    type,
    scoreWeight,
    options,
    correctAnswer,
    difficulty: q?.difficulty || null,
    metadata: q?.metadata ?? null,
    attachments: q?.attachments ?? null
  }
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

export const updateQuestion = async (examId, questionId, {
  content,
  type = 'SINGLE_CHOICE',
  scoreWeight = 1,
  options = [],
  correctAnswer = '',
  difficulty = null,
  metadata = null,
  attachments = null
}) => {
  const payload = await apiRequest(`/api/exams/${examId}/questions/${questionId}`, {
    method: 'PUT',
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

export const deleteQuestion = async (examId, questionId) => {
  const payload = await apiRequest(`/api/exams/${examId}/questions/${questionId}`, {
    method: 'DELETE'
  })
  return payload
}

/** Ghi toàn bộ câu hỏi form lên server (dùng khi xuất bản đề từ wizard tạo đề). */
export async function persistExamQuestionsFromForm(examId, questions) {
  if (!examId || !Array.isArray(questions) || questions.length === 0) return
  for (const q of questions) {
    const payload = buildCreatePayloadFromFormQuestion(q)
    if (!payload.content) continue
    await createQuestion(examId, payload)
  }
}

// previewImportFile và importQuestionsFromFile đã được chuyển sang importService.js
// Xem re-export ở đầu file
