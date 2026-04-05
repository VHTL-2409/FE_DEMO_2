/**
 * Import Service — Tách riêng từ questionService.js
 * Chịu trách nhiệm upload, parse, preview, review, confirm các file câu hỏi
 */
import { apiRequest, unwrapApiData } from './apiClient'

// ─── Constants ────────────────────────────────────────────────────────────────

/** Các định dạng file được hỗ trợ */
export const SUPPORTED_IMPORT_FORMATS = ['csv', 'xlsx', 'pdf', 'docx', 'txt', 'json', 'md', 'markdown']

/** Giá trị accept cho input file (dùng trong <input accept="...">) */
export const IMPORT_FILE_ACCEPT = '.csv,.xlsx,.pdf,.docx,.txt,.json,.md,.markdown'

/** Dung lượng tối đa: 10 MB */
export const IMPORT_FILE_MAX_SIZE_MB = 10
export const IMPORT_FILE_MAX_SIZE = IMPORT_FILE_MAX_SIZE_MB * 1024 * 1024

// ─── Validation ───────────────────────────────────────────────────────────────

/**
 * Kiểm tra file trước khi upload.
 * @param {File} file
 * @throws {Error} nếu file không hợp lệ
 */
export function validateImportFile(file) {
  if (!file || !(file instanceof File)) {
    throw new Error('Dữ liệu không phải file hợp lệ.')
  }
  const ext = file.name.split('.').pop().toLowerCase()
  if (!SUPPORTED_IMPORT_FORMATS.includes(ext)) {
    throw new Error(
      `Định dạng .${ext} không được hỗ trợ. Chỉ chấp nhận: ${SUPPORTED_IMPORT_FORMATS.join(', ')}.`
    )
  }
  if (file.size > IMPORT_FILE_MAX_SIZE) {
    throw new Error(`File vượt quá ${IMPORT_FILE_MAX_SIZE_MB} MB.`)
  }
  return true
}

// ─── Upload & Process ─────────────────────────────────────────────────────────

/**
 * Upload file để bắt đầu import job.
 * @param {File} file
 * @param {{ examId?: number | null }} options
 * @returns {Promise<{ jobId: string, status: string }>}
 */
export const uploadImportJob = async (file, { examId } = {}) => {
  validateImportFile(file)
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

/**
 * Lấy trạng thái hiện tại của một import job.
 * @param {string} jobId
 */
export const getImportJobStatus = async (jobId) => {
  const payload = await apiRequest(`/api/v1/import/status/${jobId}`)
  return unwrapApiData(payload)
}

/**
 * Lấy danh sách câu hỏi đã parse (preview).
 * @param {string} jobId
 */
export const getImportJobPreview = async (jobId) => {
  const payload = await apiRequest(`/api/v1/import/preview/${jobId}`)
  return unwrapApiData(payload)
}

/**
 * Gửi bản sửa đổi (patch) lên job đang ở trạng thái NEEDS_REVIEW.
 * @param {string} jobId
 * @param {Object} reviewPayload — { questions: [...], options: {...} }
 */
export const reviewImportJob = async (jobId, reviewPayload) => {
  const payload = await apiRequest(`/api/v1/import/review/${jobId}`, {
    method: 'PUT',
    body: JSON.stringify(reviewPayload)
  })
  return unwrapApiData(payload)
}

/**
 * Xác nhận import — chính thức lưu câu hỏi vào đề thi.
 * @param {string} jobId
 * @param {{ examId?: number | null }} options
 */
export const confirmImportJob = async (jobId, { examId } = {}) => {
  const query = examId != null ? `?examId=${encodeURIComponent(examId)}` : ''
  const payload = await apiRequest(`/api/v1/import/confirm/${jobId}${query}`, {
    method: 'POST'
  })
  return unwrapApiData(payload)
}

/**
 * Hủy import job đang chờ / đang xử lý.
 * @param {string} jobId
 */
export const cancelImportJob = async (jobId) => {
  const payload = await apiRequest(`/api/v1/import/cancel/${jobId}`, {
    method: 'DELETE'
  })
  return unwrapApiData(payload)
}

// ─── Azota ───────────────────────────────────────────────────────────────────

/**
 * Import câu hỏi đã fetch từ Azota qua import job flow.
 * @param {Array} questions
 * @param {{ examId?: number | null }} options
 */
export const importQuestionsFromAzota = async (questions, { examId } = {}) => {
  const query = examId != null ? `?examId=${encodeURIComponent(examId)}` : ''
  const payload = await apiRequest(`/api/v1/import/from-azota${query}`, {
    method: 'POST',
    body: JSON.stringify({ questions })
  })
  return unwrapApiData(payload)
}

// ─── Non-job import (trả về kết quả ngay, không qua job flow) ────────────────

/**
 * Import trực tiếp — không qua job. Dùng khi muốn import nhanh mà không cần preview/sửa.
 * Endpoint: POST /api/exams/{examId}/questions/import
 * @param {number} examId
 * @param {File} file
 * @param {{ questionCount?: number | string }} options
 */
export const importQuestionsFromFile = async (examId, file, options = {}) => {
  validateImportFile(file)
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

/**
 * Đếm số câu hợp lệ trong tệp (dùng endpoint preview riêng).
 * @param {File} file
 */
export const previewImportFile = async (file) => {
  const formData = new FormData()
  formData.append('file', file)
  const payload = await apiRequest('/api/questions/file-preview', {
    method: 'POST',
    body: formData
  })
  return unwrapApiData(payload)
}

// ─── Polling helper ──────────────────────────────────────────────────────────

/**
 * Poll trạng thái cho đến khi job hoàn thành / thất bại / bị hủy.
 * @param {string} jobId
 * @param {{ intervalMs?: number, maxAttempts?: number }} options
 * @returns {Promise<Object>} job status cuối cùng
 */
export const waitForImportJob = async (
  jobId,
  { intervalMs = 800, maxAttempts = 60 } = {}
) => {
  for (let attempt = 0; attempt < maxAttempts; attempt += 1) {
    const status = await getImportJobStatus(jobId)
    if (
      status?.status === 'DONE' ||
      status?.status === 'FAILED' ||
      status?.status === 'CANCELLED'
    ) {
      return status
    }
    await new Promise((resolve) => window.setTimeout(resolve, intervalMs))
  }
  throw new Error('Import job xử lý quá lâu. Vui lòng thử tải lại preview.')
}

// ─── Exam PDF Import (Python Parser) ────────────────────────────────────────

/**
 * Upload PDF để parse bằng Python FastAPI service.
 * @param {File} file
 * @param {{ examId?: number, forceTemplate?: string }} options
 */
export const uploadExamPdf = async (file, { examId, forceTemplate } = {}) => {
  validateImportFile(file)
  const formData = new FormData()
  formData.append('file', file)
  if (examId != null) formData.append('examId', String(examId))
  if (forceTemplate) formData.append('forceTemplate', forceTemplate)
  const payload = await apiRequest('/api/v1/exam-import/upload', {
    method: 'POST',
    body: formData
  })
  return unwrapApiData(payload)
}

/**
 * Lấy preview của exam import session.
 * @param {number} sessionId
 */
export const getExamImportPreview = async (sessionId) => {
  const payload = await apiRequest(`/api/v1/exam-import/preview/${sessionId}`)
  return unwrapApiData(payload)
}

/**
 * Lấy danh sách exam import sessions của user.
 */
export const getExamImportSessions = async () => {
  const payload = await apiRequest('/api/v1/exam-import/sessions')
  return unwrapApiData(payload)
}

/**
 * Xác nhận import questions từ exam session vào exam.
 * @param {number} sessionId
 * @param {{ examId?: number }} options
 */
export const confirmExamImport = async (sessionId, { examId } = {}) => {
  const query = examId != null ? `?examId=${encodeURIComponent(examId)}` : ''
  const payload = await apiRequest(`/api/v1/exam-import/confirm/${sessionId}${query}`, {
    method: 'POST'
  })
  return unwrapApiData(payload)
}

/**
 * Lấy đường dẫn ảnh cropped của một câu hỏi.
 * @param {number} sessionId
 * @param {number} questionIndex
 * @returns {string|null}
 */
export const getExamQuestionImageUrl = (sessionId, questionIndex) => {
  // Return relative URL — apiClient handles auth via Authorization header
  return `/api/v1/exam-import/image/${sessionId}/${questionIndex}`
}

/**
 * Lấy ảnh cropped của một câu hỏi (returns blob với auth header).
 * @param {number} sessionId
 * @param {number} questionIndex
 * @returns {Promise<Blob|null>}
 */
export const fetchExamQuestionImage = async (sessionId, questionIndex) => {
  const token = localStorage.getItem('auth_token')
  const headers = {}
  if (token) {
    headers['Authorization'] = `Bearer ${token}`
  }
  try {
    const resp = await fetch(
      `${import.meta.env.VITE_API_BASE_URL || 'http://localhost:8082'}/api/v1/exam-import/image/${sessionId}/${questionIndex}`,
      { headers }
    )
    if (!resp.ok) return null
    return await resp.blob()
  } catch {
    return null
  }
}

/**
 * Kiểm tra Python parser service có đang chạy không.
 */
export const checkPythonParserHealth = async () => {
  const payload = await apiRequest('/api/v1/exam-import/health')
  return unwrapApiData(payload)
}

/**
 * Poll trạng thái exam import session.
 * @param {number} sessionId
 * @param {{ intervalMs?: number, maxAttempts?: number }} options
 */
export const waitForExamImportSession = async (
  sessionId,
  { intervalMs = 1200, maxAttempts = 100 } = {}
) => {
  for (let attempt = 0; attempt < maxAttempts; attempt += 1) {
    const preview = await getExamImportPreview(sessionId)
    const status = preview?.status
    if (status === 'DONE' || status === 'FAILED') {
      return preview
    }
    await new Promise((resolve) => window.setTimeout(resolve, intervalMs))
  }
  throw new Error('Quá thời gian chờ parse. Vui lòng tải lại trang.')
}
