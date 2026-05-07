/**
 * Fraud Analysis Service
 * API calls for plagiarism, timing, statistical, behavior analysis, and IP reputation analysis.
 */
import { apiRequest, unwrapApiData } from './apiClient'

const BASE = '/api/v1/fraud'

export async function runPlagiarismAnalysis(examId) {
  const payload = await apiRequest(`${BASE}/plagiarism/exam/${examId}`, {
    method: 'POST',
    body: JSON.stringify({})
  })
  return unwrapApiData(payload)
}

export async function runStudentPlagiarismAnalysis(attemptId) {
  const payload = await apiRequest(`${BASE}/plagiarism/attempts/${attemptId}`, {
    method: 'POST',
    body: JSON.stringify({})
  })
  return unwrapApiData(payload)
}

export async function runTimingAnalysis(attemptId) {
  const payload = await apiRequest(`${BASE}/timing/attempts/${attemptId}`, {
    method: 'POST',
    body: JSON.stringify({})
  })
  return unwrapApiData(payload)
}

export async function runExamTimingAnalysis(examId) {
  const payload = await apiRequest(`${BASE}/timing/exam/${examId}`, {
    method: 'POST',
    body: JSON.stringify({})
  })
  return unwrapApiData(payload)
}

export async function runStatisticalAnalysis(examId) {
  const payload = await apiRequest(`${BASE}/statistical/exam/${examId}`, {
    method: 'POST',
    body: JSON.stringify({})
  })
  return unwrapApiData(payload)
}

export async function runStudentStatisticalAnalysis(attemptId) {
  const payload = await apiRequest(`${BASE}/statistical/attempts/${attemptId}`, {
    method: 'POST',
    body: JSON.stringify({})
  })
  return unwrapApiData(payload)
}

export async function runBehaviorAnalysis(attemptId) {
  const payload = await apiRequest(`${BASE}/behavior/attempts/${attemptId}`, {
    method: 'POST',
    body: JSON.stringify({})
  })
  return unwrapApiData(payload)
}

export async function runExamBehaviorAnalysis(examId) {
  const payload = await apiRequest(`${BASE}/behavior/exam/${examId}`, {
    method: 'POST',
    body: JSON.stringify({})
  })
  return unwrapApiData(payload)
}

export async function runIpReputationAnalysis(examId) {
  const payload = await apiRequest(`${BASE}/ip-reputation/exam/${examId}`, {
    method: 'POST',
    body: JSON.stringify({})
  })
  return unwrapApiData(payload)
}

export async function runComprehensiveAnalysis(attemptId) {
  const payload = await apiRequest(`${BASE}/analyze/attempts/${attemptId}`, {
    method: 'POST',
    body: JSON.stringify({})
  })
  return unwrapApiData(payload)
}

export async function runComprehensiveExamAnalysis(examId) {
  const payload = await apiRequest(`${BASE}/analyze/exam/${examId}`, {
    method: 'POST',
    body: JSON.stringify({})
  })
  return unwrapApiData(payload)
}

export async function runGradingByExam(examId) {
  const payload = await apiRequest(`${BASE}/grading/exam/${examId}`, {
    method: 'POST',
    body: JSON.stringify({})
  })
  return unwrapApiData(payload)
}

export async function runGradingByAttempt(attemptId) {
  const payload = await apiRequest(`${BASE}/grading/attempts/${attemptId}`, {
    method: 'POST',
    body: JSON.stringify({})
  })
  return unwrapApiData(payload)
}

export async function fetchFraudWarningsByExam(examId) {
  const payload = await apiRequest(`${BASE}/warnings/exam/${examId}`, { suppressToast: true })
  return unwrapApiData(payload)
}

export async function fetchFraudWarningsByAttempt(attemptId) {
  const payload = await apiRequest(`${BASE}/warnings/attempts/${attemptId}`, { suppressToast: true })
  return unwrapApiData(payload) || []
}

export async function reviewFraudWarning(warningId, reviewStatus, reviewNote = '') {
  const payload = await apiRequest(`${BASE}/warnings/${warningId}/review`, {
    method: 'POST',
    body: JSON.stringify({ reviewStatus, reviewNote })
  })
  return unwrapApiData(payload)
}

export async function generateFraudWarningsForExam(examId) {
  const payload = await apiRequest(`${BASE}/warnings/exam/${examId}/generate`, {
    method: 'POST',
    body: JSON.stringify({})
  })
  return unwrapApiData(payload)
}
