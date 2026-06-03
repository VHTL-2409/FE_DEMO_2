import { apiRequest, unwrapApiData } from './apiClient'

const normalizeClassSummary = (item) => {
  if (!item || typeof item !== 'object') return item

  const name = item.name || item.className || ''
  const classCode = item.classCode || item.code || ''

  return {
    ...item,
    name,
    className: item.className || name,
    classCode,
    code: item.code || classCode
  }
}

export const listClasses = async () => {
  const payload = await apiRequest('/api/teacher/classes')
  return unwrapApiData(payload) || []
}

export const getClassDetail = async (classId) => {
  const payload = await apiRequest(`/api/teacher/classes/${classId}`)
  return unwrapApiData(payload)
}

export const createClass = async ({ name, description = '', subject = '' }) => {
  const payload = await apiRequest('/api/teacher/classes', {
    method: 'POST',
    body: JSON.stringify({ name, description, subject })
  })
  return unwrapApiData(payload)
}

export const createClassWithRoster = async ({ name, description = '', subject = '', file }) => {
  const form = new FormData()
  form.append('name', name)
  if (description) form.append('description', description)
  if (subject) form.append('subject', subject)
  form.append('file', file)
  const payload = await apiRequest('/api/teacher/classes/create-with-roster', {
    method: 'POST',
    body: form
  })
  return unwrapApiData(payload)
}

export const updateClass = async (classId, { name, description, subject }) => {
  const payload = await apiRequest(`/api/teacher/classes/${classId}`, {
    method: 'PUT',
    body: JSON.stringify({ name, description, subject })
  })
  return unwrapApiData(payload)
}

export const deleteClass = async (classId) => {
  await apiRequest(`/api/teacher/classes/${classId}`, { method: 'DELETE' })
}

export const getClassStudents = async (classId) => {
  const payload = await apiRequest(`/api/teacher/classes/${classId}/students`)
  return unwrapApiData(payload) || []
}

export const getTeacherClassExams = async (classId) => {
  const payload = await apiRequest(`/api/teacher/classes/${classId}/exams`)
  return unwrapApiData(payload) || []
}

export const addStudentsToClass = async (classId, studentIds) => {
  const payload = await apiRequest(`/api/teacher/classes/${classId}/students`, {
    method: 'POST',
    body: JSON.stringify({ studentIds })
  })
  return unwrapApiData(payload) || []
}

export const removeStudentFromClass = async (classId, studentId) => {
  await apiRequest(`/api/teacher/classes/${classId}/students/${studentId}`, { method: 'DELETE' })
}

export const getAvailableStudents = async (classId, search = '') => {
  const query = search ? `?search=${encodeURIComponent(search)}` : ''
  const payload = await apiRequest(`/api/teacher/classes/${classId}/available-students${query}`)
  return unwrapApiData(payload) || []
}

export const searchAllStudents = async (query = '') => {
  const url = query
    ? `/api/admin/users/students?search=${encodeURIComponent(query)}`
    : '/api/admin/users/students'
  const payload = await apiRequest(url)
  return unwrapApiData(payload) || []
}

export const joinClassByCode = async (classCode) => {
  const payload = await apiRequest('/api/student/classes/join', {
    method: 'POST',
    body: JSON.stringify({ classCode })
  })
  return normalizeClassSummary(unwrapApiData(payload))
}

export const getMyClasses = async () => {
  const payload = await apiRequest('/api/student/classes/my-classes')
  return (unwrapApiData(payload) || []).map(normalizeClassSummary)
}

export const leaveClass = async (classId) => {
  await apiRequest(`/api/student/classes/${classId}/leave`, { method: 'DELETE' })
}

export const getStudentClassExams = async (classId) => {
  const payload = await apiRequest(`/api/student/classes/${classId}/exams`)
  return unwrapApiData(payload) || []
}

export const importStudentsToClass = async (classId, students) => {
  const payload = await apiRequest(`/api/teacher/classes/${classId}/students/import`, {
    method: 'POST',
    body: JSON.stringify({ students, mandatory: true })
  })
  return unwrapApiData(payload)
}

/**
 * Upload CSV / XLSX / XLS; backend parses file and imports students.
 * @param {number|string} classId
 * @param {File} file
 * @param {boolean} [mandatory=true]
 */
export const importStudentsFileToClass = async (classId, file, mandatory = true) => {
  const form = new FormData()
  form.append('file', file)
  form.append('mandatory', String(mandatory))
  const payload = await apiRequest(`/api/teacher/classes/${classId}/students/import-file`, {
    method: 'POST',
    body: form
  })
  return unwrapApiData(payload)
}
