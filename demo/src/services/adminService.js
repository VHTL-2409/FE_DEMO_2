import { apiRequest, unwrapApiData } from './apiClient'

export const fetchAdminDashboardStats = async () => {
  const payload = await apiRequest('/api/admin/dashboard/stats')
  return unwrapApiData(payload)
}

/**
 * @param {{ page?: number, size?: number, q?: string }} params
 */
export const fetchAdminStudents = async (params = {}) => {
  const q = new URLSearchParams()
  if (params.page != null) q.set('page', String(params.page))
  if (params.size != null) q.set('size', String(params.size))
  if (params.q != null && String(params.q).trim() !== '') q.set('q', String(params.q).trim())
  const suffix = q.toString() ? `?${q.toString()}` : ''
  const payload = await apiRequest(`/api/admin/users/students${suffix}`)
  return unwrapApiData(payload)
}

export const fetchAdminStudentDetail = async (userId) => {
  const payload = await apiRequest(`/api/admin/users/students/${userId}`)
  return unwrapApiData(payload)
}

export const deleteAdminStudent = async (userId) => {
  const payload = await apiRequest(`/api/admin/users/students/${userId}`, { method: 'DELETE' })
  return unwrapApiData(payload)
}

/**
 * @param {{ page?: number, size?: number, q?: string }} params
 */
export const fetchAdminTeachers = async (params = {}) => {
  const q = new URLSearchParams()
  if (params.page != null) q.set('page', String(params.page))
  if (params.size != null) q.set('size', String(params.size))
  if (params.q != null && String(params.q).trim() !== '') q.set('q', String(params.q).trim())
  const suffix = q.toString() ? `?${q.toString()}` : ''
  const payload = await apiRequest(`/api/admin/users/teachers${suffix}`)
  return unwrapApiData(payload)
}

export const fetchAdminTeacherDetail = async (userId) => {
  const payload = await apiRequest(`/api/admin/users/teachers/${userId}`)
  return unwrapApiData(payload)
}

export const deleteAdminTeacher = async (userId) => {
  const payload = await apiRequest(`/api/admin/users/teachers/${userId}`, { method: 'DELETE' })
  return unwrapApiData(payload)
}

/**
 * @param {{ page?: number, size?: number }} params
 */
export const fetchAdminAdmins = async (params = {}) => {
  const q = new URLSearchParams()
  if (params.page != null) q.set('page', String(params.page))
  if (params.size != null) q.set('size', String(params.size))
  const suffix = q.toString() ? `?${q.toString()}` : ''
  const payload = await apiRequest(`/api/admin/users/admins${suffix}`)
  return unwrapApiData(payload)
}

/** Tạo tài khoản admin mới */
export const createAdminUser = async (data) => {
  const payload = await apiRequest('/api/admin/users/admins', {
    method: 'POST',
    body: JSON.stringify(data)
  })
  return unwrapApiData(payload)
}

/** Xóa tài khoản admin */
export const deleteAdminAdmin = async (userId) => {
  const payload = await apiRequest(`/api/admin/users/admins/${userId}`, { method: 'DELETE' })
  return unwrapApiData(payload)
}

/**
 * @param {{ page?: number, size?: number }} params
 */
/**
 * @param {{ page?: number, size?: number }} params
 */
export const fetchAdminClasses = async (params = {}) => {
  const q = new URLSearchParams()
  if (params.page != null) q.set('page', String(params.page))
  if (params.size != null) q.set('size', String(params.size))
  const suffix = q.toString() ? `?${q.toString()}` : ''
  const payload = await apiRequest(`/api/admin/classes${suffix}`)
  return unwrapApiData(payload)
}

export const fetchAdminExams = async (params = {}) => {
  const q = new URLSearchParams()
  if (params.page != null) q.set('page', String(params.page))
  if (params.size != null) q.set('size', String(params.size))
  const suffix = q.toString() ? `?${q.toString()}` : ''
  const payload = await apiRequest(`/api/admin/exams${suffix}`)
  return unwrapApiData(payload)
}

/**
 * @param {number} examId
 * @param {boolean} active
 */
export const patchAdminExamActive = async (examId, active) => {
  const payload = await apiRequest(`/api/admin/exams/${examId}/active`, {
    method: 'PATCH',
    body: JSON.stringify({ active })
  })
  return unwrapApiData(payload)
}

export const fetchAdminOpsSummary = async () => {
  const payload = await apiRequest('/api/admin/ops/summary')
  return unwrapApiData(payload)
}
