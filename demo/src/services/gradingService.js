/**
 * Intelligent Grading Service
 * API calls for IRT-based scoring, peer comparison, and question analysis.
 */
import apiClient from './apiClient'

const BASE = '/api/v1/grading'

export async function gradeAttempt(attemptId) {
  const response = await apiClient.post(`${BASE}/attempts/${attemptId}/grade`)
  return response.data.data
}

export async function getGradingResult(attemptId) {
  const response = await apiClient.get(`${BASE}/attempts/${attemptId}/result`)
  return response.data.data
}

export async function gradeAllAttempts(examId) {
  const response = await apiClient.post(`${BASE}/exams/${examId}/grade-all`)
  return response.data.data
}

/**
 * Parse grading result into chart-ready format.
 */
export function parseGradingForChart(result) {
  if (!result) return null

  return {
    rawScore: result.rawScore,
    maxScore: result.maxScore,
    finalScore: result.finalScore,
    irtScore: result.irtResult?.irtScore || null,
    theta: result.irtResult?.theta || null,
    peerPercentile: result.peerResult?.percentile || null,
    peerRank: result.peerResult?.rank || null,
    peerTotal: result.peerResult?.totalPeers || null,
    reliability: result.irtResult?.reliability || null,
    questions: (result.questionAnalyses || []).map(q => ({
      id: q.questionId,
      difficulty: q.difficulty,
      discrimination: q.discrimination,
      quality: q.quality,
      correctCount: q.correctCount,
      totalAttempts: q.totalAttempts,
    })),
  }
}

/**
 * Get grade color based on score.
 */
export function getGradeColor(score, maxScore = 10) {
  const pct = score / maxScore
  if (pct >= 0.9) return '#16a34a'   // green
  if (pct >= 0.7) return '#65a30d'   // lime
  if (pct >= 0.5) return '#ca8a04'   // yellow
  if (pct >= 0.3) return '#ea580c'   // orange
  return '#dc2626'                    // red
}

/**
 * Get IRT theta description.
 */
export function getThetaLabel(theta) {
  if (theta === null || theta === undefined) return 'N/A'
  if (theta >= 2) return 'Xuất sắc'
  if (theta >= 1) return 'Giỏi'
  if (theta >= 0) return 'Khá'
  if (theta >= -1) return 'Trung bình'
  return 'Yếu'
}
