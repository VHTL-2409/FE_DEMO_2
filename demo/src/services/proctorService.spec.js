import { beforeEach, describe, expect, it, vi } from 'vitest'

import {
  acknowledgeCameraAlert,
  dismissCameraAlert,
  fetchCameraAlerts,
  fetchCameraStatus,
  fetchLatestCameraFrame,
  reviewProctorFlag
} from './proctorService'
import { apiRequest } from './apiClient'

vi.mock('./apiClient', () => ({
  apiRequest: vi.fn(),
  unwrapApiData: payload => payload?.data ?? payload
}))

describe('proctorService', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    apiRequest.mockResolvedValue({ data: { ok: true } })
  })

  it('calls camera monitoring endpoints with quiet fetch behavior', async () => {
    await fetchCameraStatus(42)
    await fetchCameraAlerts(42)
    await fetchLatestCameraFrame(9)

    expect(apiRequest).toHaveBeenNthCalledWith(1, '/api/v1/proctor/exams/42/camera-status', {
      suppressToast: true
    })
    expect(apiRequest).toHaveBeenNthCalledWith(2, '/api/v1/proctor/exams/42/camera-alerts', {
      suppressToast: true
    })
    expect(apiRequest).toHaveBeenNthCalledWith(3, '/api/v1/proctor/camera/frame/attempt/9/latest', {
      suppressToast: true
    })
  })

  it('calls camera alert action endpoints', async () => {
    await acknowledgeCameraAlert(11)
    await dismissCameraAlert(12)

    expect(apiRequest).toHaveBeenNthCalledWith(1, '/api/v1/proctor/alerts/11/acknowledge', {
      method: 'POST'
    })
    expect(apiRequest).toHaveBeenNthCalledWith(2, '/api/v1/proctor/alerts/12/dismiss', {
      method: 'POST'
    })
  })

  it('submits proctor flag review payload', async () => {
    const payload = { status: 'FALSE_POSITIVE', note: 'lighting issue' }

    await reviewProctorFlag(15, payload)

    expect(apiRequest).toHaveBeenCalledWith('/api/v1/proctor/flags/15', {
      method: 'PATCH',
      body: JSON.stringify(payload)
    })
  })
})
