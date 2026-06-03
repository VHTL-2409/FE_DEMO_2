import { beforeEach, describe, expect, it, vi } from 'vitest'

import {
  fetchMlRiskStatus,
  reviewFraudWarning,
  runExamBehaviorAnalysis,
  runExamStatisticalAnalysis,
  runMlRiskByExam
} from './fraudAnalysisService'
import { apiRequest } from './apiClient'

vi.mock('./apiClient', () => ({
  apiRequest: vi.fn(),
  unwrapApiData: payload => payload?.data ?? payload
}))

describe('fraudAnalysisService', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    apiRequest.mockResolvedValue({ data: { ok: true } })
  })

  it('calls statistical and behavior exam analysis endpoints', async () => {
    await runExamStatisticalAnalysis(42)
    await runExamBehaviorAnalysis(42)

    expect(apiRequest).toHaveBeenNthCalledWith(1, '/api/v1/fraud/statistical/exam/42', {
      method: 'POST',
      body: JSON.stringify({})
    })
    expect(apiRequest).toHaveBeenNthCalledWith(2, '/api/v1/fraud/behavior/exam/42', {
      method: 'POST',
      body: JSON.stringify({})
    })
  })

  it('calls ml risk endpoints with quiet toast behavior', async () => {
    await runMlRiskByExam(42)
    await fetchMlRiskStatus()

    expect(apiRequest).toHaveBeenNthCalledWith(1, '/api/v1/fraud/ml-risk/exam/42', {
      method: 'POST',
      body: JSON.stringify({}),
      suppressToast: true
    })
    expect(apiRequest).toHaveBeenNthCalledWith(2, '/api/v1/fraud/ml-risk/status', {
      suppressToast: true
    })
  })

  it('submits warning review status and note', async () => {
    await reviewFraudWarning(9, 'FALSE_POSITIVE', 'camera glitch')

    expect(apiRequest).toHaveBeenCalledWith('/api/v1/fraud/warnings/9/review', {
      method: 'POST',
      body: JSON.stringify({ reviewStatus: 'FALSE_POSITIVE', reviewNote: 'camera glitch' })
    })
  })
})
