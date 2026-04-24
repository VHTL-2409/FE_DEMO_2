import { apiRequest, unwrapApiData } from './apiClient'

/**
 * Teacher dashboard — aggregates stats across all teacher resources.
 * Relies on the same auth token as the rest of the app.
 */

/** GET /api/teacher/dashboard/stats */
export const fetchTeacherDashboardStats = async () => {
  const payload = await apiRequest('/api/teacher/dashboard/stats', { suppressToast: true })
  return unwrapApiData(payload)
}

/** GET /api/teacher/dashboard/activity */
export const fetchTeacherActivity = async () => {
  const payload = await apiRequest('/api/teacher/dashboard/activity', { suppressToast: true })
  return unwrapApiData(payload) || []
}

/** GET /api/teacher/dashboard/score-distribution?examId= */
export const fetchScoreDistribution = async (examId) => {
  const url = examId ? `/api/teacher/dashboard/score-distribution?examId=${examId}` : '/api/teacher/dashboard/score-distribution'
  const payload = await apiRequest(url, { suppressToast: true })
  return unwrapApiData(payload) || {}
}

/** GET /api/teacher/dashboard/trend?days=7 */
export const fetchTeacherTrend = async (days = 7) => {
  const payload = await apiRequest(`/api/teacher/dashboard/trend?days=${days}`, { suppressToast: true })
  return unwrapApiData(payload) || {}
}

/** GET /api/teacher/dashboard/upcoming-exams */
export const fetchUpcomingExams = async () => {
  const payload = await apiRequest('/api/teacher/dashboard/upcoming-exams', { suppressToast: true })
  return unwrapApiData(payload) || []
}
