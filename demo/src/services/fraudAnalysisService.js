/**
 * Fraud Analysis Service
 * API calls for plagiarism, timing, statistical, biometrics, and IP reputation analysis.
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

export async function runBiometricsAnalysis(attemptId) {
  const payload = await apiRequest(`${BASE}/biometrics/attempts/${attemptId}`, {
    method: 'POST',
    body: JSON.stringify({})
  })
  return unwrapApiData(payload)
}

export async function runExamBiometricsAnalysis(examId) {
  const payload = await apiRequest(`${BASE}/biometrics/exam/${examId}`, {
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
