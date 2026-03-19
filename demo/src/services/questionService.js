import { apiRequest, unwrapApiData, API_BASE_URL } from './apiClient'

/** Chuẩn file: CSV, XLSX, PDF, DOCX. Tối đa 10MB. */
export const FILE_FORMAT_DESC = 'Hỗ trợ CSV, XLSX, PDF, Word. PDF/Word: Câu N. nội dung | A) B) C) D) | Đáp án: A'

/** URL tải mẫu CSV chuẩn */
export const getTemplateDownloadUrl = (format = 'csv') => {
  const q = format === 'xlsx' ? '?format=xlsx' : ''
  return `${API_BASE_URL}/api/questions/template${q}`
}

export const parseQuestionOptions = (rawOptions) => {
  if (!rawOptions) return []

  try {
    const parsed = JSON.parse(rawOptions)
    if (!Array.isArray(parsed)) return []

    return parsed.map((item, index) => ({
      id: item?.id ? String(item.id) : String(index + 1),
      text: item?.text || item?.content || item?.label || ''
    }))
  } catch {
    return []
  }
}

export const listExamQuestions = async (examId) => {
  const payload = await apiRequest(`/api/exams/${examId}/questions`)
  return unwrapApiData(payload) || []
}

export const createQuestion = async (examId, { content, scoreWeight = 1, options, correctAnswer }) => {
  const payload = await apiRequest(`/api/exams/${examId}/questions`, {
    method: 'POST',
    body: JSON.stringify({
      content,
      scoreWeight,
      options: JSON.stringify(options),
      correctAnswer
    })
  })

  return unwrapApiData(payload)
}

export const importQuestionsFromFile = async (examId, file) => {
  const formData = new FormData()
  formData.append('file', file)

  const payload = await apiRequest(`/api/exams/${examId}/questions/import`, {
    method: 'POST',
    body: formData
  })

  return unwrapApiData(payload)
}

export const importQuestionsFromXlsx = async (examId, file) => {
  return importQuestionsFromFile(examId, file)
}
