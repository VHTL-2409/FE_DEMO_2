/**
 * Analytics Service — Teacher analytics and reporting
 */
import { apiRequest, unwrapApiData } from './apiClient'

// ─── Teacher Dashboard Stats ────────────────────────────────────────────────

export const getTeacherDashboardStats = async () => {
  const payload = await apiRequest('/api/teacher/dashboard/stats')
  return unwrapApiData(payload)
}

// ─── Exam Analytics ──────────────────────────────────────────────────────────

export const getExamScoreStats = async (examId) => {
  const payload = await apiRequest(`/api/exams/${examId}/score-stats`)
  return unwrapApiData(payload)
}

export const getExamTrend = async ({ days = 30, examId } = {}) => {
  const query = examId ? `?examId=${examId}&days=${days}` : `?days=${days}`
  const payload = await apiRequest(`/api/exams/analytics/trend${query}`)
  return unwrapApiData(payload)
}

export const getScoreDistribution = async (examId) => {
  const payload = await apiRequest(`/api/exams/${examId}/score-distribution`)
  return unwrapApiData(payload)
}

export const getQuestionDifficultyStats = async (examId) => {
  const payload = await apiRequest(`/api/exams/${examId}/question-difficulty`)
  return unwrapApiData(payload)
}

export const getStudentRanking = async (examId) => {
  const payload = await apiRequest(`/api/exams/${examId}/ranking`)
  return unwrapApiData(payload)
}

export const getTimeOnExamStats = async (examId) => {
  const payload = await apiRequest(`/api/exams/${examId}/time-stats`)
  return unwrapApiData(payload)
}

// ─── Class-based analytics ─────────────────────────────────────────────────

export const getClassPerformance = async (classId) => {
  const payload = await apiRequest(`/api/teacher/classes/${classId}/performance`)
  return unwrapApiData(payload)
}

export const getClassExamList = async (classId) => {
  const payload = await apiRequest(`/api/teacher/classes/${classId}/exams`)
  return unwrapApiData(payload) || []
}

// ─── Recent activity ──────────────────────────────────────────────────────

export const getRecentSubmissions = async ({ limit = 10 } = {}) => {
  const payload = await apiRequest(`/api/teacher/recent-activity?limit=${limit}`)
  return unwrapApiData(payload) || []
}

// ─── Alert summary ─────────────────────────────────────────────────────────

export const getAlertSummary = async () => {
  const payload = await apiRequest('/api/teacher/alerts/summary')
  return unwrapApiData(payload)
}

// ─── Upcoming exams ─────────────────────────────────────────────────────────

export const getUpcomingExams = async ({ limit = 5 } = {}) => {
  const payload = await apiRequest(`/api/teacher/upcoming-exams?limit=${limit}`)
  return unwrapApiData(payload) || []
}

// ─── Active monitoring summary ──────────────────────────────────────────────

export const getActiveMonitoringSummary = async () => {
  const payload = await apiRequest('/api/teacher/monitoring/summary')
  return unwrapApiData(payload)
}

// ─── Export ─────────────────────────────────────────────────────────────────

export const exportExamReportCsv = (examId, examTitle) => {
  // Client-side CSV generation for exam results
  // The actual data should be fetched via listExamAttempts + getAttemptReport
  return import('../services/attemptService').then(({ listExamAttempts, getAttemptReport }) =>
    listExamAttempts(examId).then(async (attempts) => {
      const rows = [
        ['STT', 'Họ tên', 'Mã sinh viên', 'Điểm', 'Thời gian nộp', 'Trạng thái']
      ]
      for (let i = 0; i < attempts.length; i++) {
        const attempt = attempts[i]
        const report = await getAttemptReport(attempt.id).catch(() => null)
        rows.push([
          i + 1,
          attempt.studentName || attempt.student?.fullName || '',
          attempt.studentCode || attempt.student?.username || '',
          report?.score ?? attempt.score ?? '-',
          attempt.submittedAt ? new Date(attempt.submittedAt).toLocaleString('vi-VN') : '-',
          attempt.status || 'N/A'
        ])
      }
      const csv = rows.map(r => r.join(',')).join('\n')
      const blob = new Blob(['\ufeff' + csv], { type: 'text/csv;charset=utf-8' })
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = `bao-cao-${examTitle || examId}-${Date.now()}.csv`
      a.click()
      URL.revokeObjectURL(url)
    })
  )
}
