/**
 * Fraud Analysis Service
 * API calls for plagiarism, timing, statistical, biometrics, and IP reputation analysis.
 */
import apiClient from './apiClient'

const BASE = '/api/v1/fraud'

export async function runPlagiarismAnalysis(examId) {
  const response = await apiClient.post(`${BASE}/plagiarism/exam/${examId}`)
  return response.data.data
}

export async function runStudentPlagiarismAnalysis(attemptId) {
  const response = await apiClient.post(`${BASE}/plagiarism/attempts/${attemptId}`)
  return response.data.data
}

export async function runTimingAnalysis(attemptId) {
  const response = await apiClient.post(`${BASE}/timing/attempts/${attemptId}`)
  return response.data.data
}

export async function runExamTimingAnalysis(examId) {
  const response = await apiClient.post(`${BASE}/timing/exam/${examId}`)
  return response.data.data
}

export async function runStatisticalAnalysis(examId) {
  const response = await apiClient.post(`${BASE}/statistical/exam/${examId}`)
  return response.data.data
}

export async function runStudentStatisticalAnalysis(attemptId) {
  const response = await apiClient.post(`${BASE}/statistical/attempts/${attemptId}`)
  return response.data.data
}

export async function runBiometricsAnalysis(attemptId) {
  const response = await apiClient.post(`${BASE}/biometrics/attempts/${attemptId}`)
  return response.data.data
}

export async function runExamBiometricsAnalysis(examId) {
  const response = await apiClient.post(`${BASE}/biometrics/exam/${examId}`)
  return response.data.data
}

export async function runIpReputationAnalysis(examId) {
  const response = await apiClient.post(`${BASE}/ip-reputation/exam/${examId}`)
  return response.data.data
}

export async function runComprehensiveAnalysis(attemptId) {
  const response = await apiClient.post(`${BASE}/analyze/attempts/${attemptId}`)
  return response.data.data
}

export async function runComprehensiveExamAnalysis(examId) {
  const response = await apiClient.post(`${BASE}/analyze/exam/${examId}`)
  return response.data.data
}
