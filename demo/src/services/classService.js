import { apiRequest, unwrapApiData } from './apiClient'

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

export const addStudentsToClass = async (classId, studentIds) => {
  const payload = await apiRequest(`/api/teacher/classes/${classId}/students`, {
    method: 'POST',
    body: JSON.stringify({ studentIds })
  })
  return unwrapApiData(payload) || []
}

export const forceAddStudentsToClass = async (classId, studentIds) => {
  const payload = await apiRequest(`/api/teacher/classes/${classId}/students/force`, {
    method: 'POST',
    body: JSON.stringify({ studentIds, mandatory: true })
  })
  return unwrapApiData(payload) || []
}

export const removeStudentFromClass = async (classId, studentId) => {
  await apiRequest(`/api/teacher/classes/${classId}/students/${studentId}`, { method: 'DELETE' })
}

export const getAvailableStudents = async (classId) => {
  const payload = await apiRequest(`/api/teacher/classes/${classId}/available-students`)
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
  return unwrapApiData(payload)
}

export const getMyClasses = async () => {
  const payload = await apiRequest('/api/student/classes/my-classes')
  return unwrapApiData(payload) || []
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
