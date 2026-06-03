import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { createApp, nextTick, ref } from 'vue'

import { dismissCameraAlert, fetchCameraAlerts, fetchCameraStatus } from '../services/proctorService'
import { useAiCameraDashboard } from './useAiCameraDashboard'

vi.mock('../services/proctorService', () => ({
  fetchCameraStatus: vi.fn(),
  fetchCameraAlerts: vi.fn(),
  dismissCameraAlert: vi.fn()
}))

async function flushPromises() {
  await Promise.resolve()
  await nextTick()
  await Promise.resolve()
}

function mountComposable(factory) {
  let result
  const app = createApp({
    setup() {
      result = factory()
      return () => null
    }
  })
  const el = document.createElement('div')
  document.body.appendChild(el)
  app.mount(el)
  return {
    result,
    unmount() {
      app.unmount()
      el.remove()
    }
  }
}

describe('useAiCameraDashboard', () => {
  beforeEach(() => {
    vi.useFakeTimers()
    vi.setSystemTime(new Date('2026-05-07T10:00:20+07:00'))
    vi.clearAllMocks()
    fetchCameraStatus.mockResolvedValue([])
    fetchCameraAlerts.mockResolvedValue([])
    dismissCameraAlert.mockResolvedValue({ ok: true })
    window.Stomp = undefined
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('loads camera status and alerts for the exam', async () => {
    fetchCameraStatus.mockResolvedValue([
      { attemptId: 88, studentName: 'Student One', status: 'OK', cameraActive: true }
    ])
    fetchCameraAlerts.mockResolvedValue([
      { id: 1, attemptId: 88, signalType: 'FACE_NOT_DETECTED', severity: 'HIGH' }
    ])

    const { result: dashboard, unmount } = mountComposable(() => useAiCameraDashboard(ref(149)))
    await flushPromises()

    expect(fetchCameraStatus).toHaveBeenCalledWith(149)
    expect(fetchCameraAlerts).toHaveBeenCalledWith(149)
    expect(dashboard.cameraStatuses.value).toHaveLength(1)
    expect(dashboard.recentAlerts.value).toHaveLength(1)
    expect(dashboard.totalStudents.value).toBe(1)
    expect(dashboard.connectionLabel.value).toBe('Polling')

    unmount()
  })

  it('applies realtime AI camera signals with metrics and alerts', async () => {
    const { result: dashboard, unmount } = mountComposable(() => useAiCameraDashboard(ref(149)))
    await flushPromises()

    dashboard.cameraStatuses.value = [
      { attemptId: 88, studentName: 'Student One', status: 'OK', activeSignals: [], criticalSignals: [] }
    ]

    dashboard.applyRealtimeEvent({
      type: 'AI_CAMERA_SIGNAL',
      attemptId: 88,
      studentName: 'Student One',
      latestSignal: {
        signalType: 'MULTIPLE_FACES',
        severity: 'CRITICAL',
        confidence: 0.98,
        occurredAt: '2026-05-07T10:00:00+07:00'
      },
      face_count: 2,
      multiple_faces: true,
      face_quality: 'MULTIPLE_FACES',
      frame_quality: 'GOOD',
      attention_score: 0.82,
      gaze_confidence: 0.73,
      receivedAt: '2026-05-07T10:00:15+07:00',
      visualOverlay: {
        imageWidth: 160,
        imageHeight: 120,
        faceBoxes: [{ x: 10, y: 12, width: 80, height: 90 }],
        eyeBoxes: [],
        pupilPoints: [],
        status: 'TRACKING',
        label: 'Đang theo dõi',
        tone: 'success'
      }
    })

    let camera = dashboard.cameraStatuses.value[0]
    expect(camera.status).toBe('CRITICAL')
    expect(camera.faceCount).toBe(2)
    expect(camera.multipleFaces).toBe(true)
    expect(camera.lastFrameAt).toBe('2026-05-07T10:00:15+07:00')
    expect(camera.frameAgeMs).toBe(5000)
    expect(camera.connectionHealth).toBe('live')
    expect(camera.previewAvailable).toBe(true)
    expect(camera.attentionScore).toBe(0.82)
    expect(camera.gazeConfidence).toBe(0.73)
    expect(camera.visualOverlay).toMatchObject({ status: 'TRACKING', tone: 'success' })
    expect(camera.activeSignals).toContain('MULTIPLE_FACES')
    expect(camera.criticalSignals).toHaveLength(1)
    expect(dashboard.recentAlerts.value[0].signalType).toBe('MULTIPLE_FACES')

    dashboard.applyRealtimeEvent({
      type: 'FRAUD_WARNING_RECORDED',
      warningCategory: 'CAMERA_PROCTORING',
      warningType: 'MULTIPLE_FACES',
      severity: 'CRITICAL',
      confidence: 0.98,
      riskImpact: 25,
      attemptId: 88,
      studentName: 'Student One',
      issuedAt: '2026-05-07T10:00:01+07:00'
    })

    camera = dashboard.cameraStatuses.value[0]
    expect(camera.activeSignals).toEqual(['MULTIPLE_FACES'])
    expect(camera.criticalSignals).toHaveLength(1)
    expect(camera.alertCount).toBe(1)
    expect(dashboard.recentAlerts.value).toHaveLength(1)

    unmount()
  })

  it('normalizes stale camera health from loaded status rows', async () => {
    fetchCameraStatus.mockResolvedValue([
      {
        attemptId: 91,
        studentName: 'Student Four',
        status: 'OK',
        cameraActive: true,
        lastFrameAt: '2026-05-07T09:59:30+07:00',
        visualOverlay: {
          imageWidth: 320,
          imageHeight: 180,
          faceBoxes: [{ x: 20, y: 20, width: 80, height: 90 }]
        }
      },
      {
        attemptId: 92,
        studentName: 'Student Five',
        status: 'OK',
        cameraActive: true,
        lastFrameAt: '2026-05-07T09:58:00+07:00'
      }
    ])

    const { result: dashboard, unmount } = mountComposable(() => useAiCameraDashboard(ref(149)))
    await flushPromises()

    expect(dashboard.cameraStatuses.value[0]).toMatchObject({
      frameAgeMs: 50000,
      connectionHealth: 'stale',
      previewAvailable: true
    })
    expect(dashboard.cameraStatuses.value[1]).toMatchObject({
      connectionHealth: 'offline',
      previewAvailable: false
    })

    unmount()
  })

  it('normalizes AI unavailable separately from stale/offline camera health', async () => {
    fetchCameraStatus.mockResolvedValue([
      {
        attemptId: 93,
        studentName: 'Student Six',
        status: 'AI_UNAVAILABLE',
        cameraActive: true,
        aiServiceStatus: 'AI_UNAVAILABLE',
        lastFrameAt: '2026-05-07T10:00:18+07:00'
      }
    ])

    const { result: dashboard, unmount } = mountComposable(() => useAiCameraDashboard(ref(149)))
    await flushPromises()

    expect(dashboard.cameraStatuses.value[0]).toMatchObject({
      aiServiceStatus: 'AI_UNAVAILABLE',
      connectionHealth: 'ai_unavailable',
      frameAgeMs: 2000
    })

    unmount()
  })

  it('normalizes AI busy separately from stale/offline camera health', async () => {
    fetchCameraStatus.mockResolvedValue([
      {
        attemptId: 94,
        studentName: 'Student Seven',
        status: 'AI_BUSY',
        cameraActive: true,
        aiServiceStatus: 'AI_BUSY',
        lastFrameAt: '2026-05-07T10:00:18+07:00'
      }
    ])

    const { result: dashboard, unmount } = mountComposable(() => useAiCameraDashboard(ref(149)))
    await flushPromises()

    expect(dashboard.cameraStatuses.value[0]).toMatchObject({
      aiServiceStatus: 'AI_BUSY',
      connectionHealth: 'ai_busy',
      frameAgeMs: 2000
    })

    unmount()
  })

  it('uses websocket label after a successful STOMP subscription', async () => {
    const subscribe = vi.fn()
    const connect = vi.fn((headers, onConnect) => onConnect())
    window.Stomp = {
      client: vi.fn(() => ({ connect, subscribe, disconnect: vi.fn() }))
    }

    const { result: dashboard, unmount } = mountComposable(() => useAiCameraDashboard(ref(149)))
    await flushPromises()

    expect(dashboard.connectionLabel.value).toBe('Realtime')
    expect(subscribe).toHaveBeenCalledWith('/topic/exams/149/camera-updates', expect.any(Function))

    unmount()
  })

  it('handles camera fraud warnings and dismisses alerts', async () => {
    const { result: dashboard, unmount } = mountComposable(() => useAiCameraDashboard(ref(149)))
    await flushPromises()

    dashboard.applyRealtimeEvent({
      type: 'FRAUD_WARNING_RECORDED',
      warningCategory: 'CAMERA_PROCTORING',
      warningType: 'FACE_NOT_DETECTED',
      severity: 'HIGH',
      confidence: 0.92,
      riskImpact: 20,
      attemptId: 89,
      studentName: 'Student Two',
      faceCount: 0,
      faceDetected: false,
      frameQuality: 'POOR',
      issuedAt: '2026-05-07T10:01:00+07:00'
    })

    expect(dashboard.cameraStatuses.value).toHaveLength(1)
    expect(dashboard.cameraStatuses.value[0].status).toBe('WARNING')
    expect(dashboard.cameraStatuses.value[0].faceCount).toBe(0)
    expect(dashboard.cameraStatuses.value[0].riskImpact).toBe(20)
    expect(dashboard.recentAlerts.value).toHaveLength(1)
    expect(dashboard.recentAlerts.value[0].riskImpact).toBe(20)

    const alertId = dashboard.recentAlerts.value[0].id
    await expect(dashboard.dismissAlert(alertId)).resolves.toBe(true)
    expect(dismissCameraAlert).toHaveBeenCalledWith(alertId)
    expect(dashboard.recentAlerts.value).toHaveLength(0)

    unmount()
  })

  it('treats visual identity mismatch warnings as camera-related realtime alerts', async () => {
    const { result: dashboard, unmount } = mountComposable(() => useAiCameraDashboard(ref(149)))
    await flushPromises()

    dashboard.applyRealtimeEvent({
      type: 'FRAUD_WARNING_RECORDED',
      warningCategory: 'VISUAL_IDENTITY',
      warningType: 'IDENTITY_FACE_MISMATCH',
      severity: 'HIGH',
      confidence: 0.91,
      riskImpact: 22,
      attemptId: 95,
      studentName: 'Student Eight',
      issuedAt: '2026-05-07T10:01:30+07:00'
    })

    expect(dashboard.cameraStatuses.value).toHaveLength(1)
    expect(dashboard.cameraStatuses.value[0]).toMatchObject({
      attemptId: 95,
      status: 'WARNING'
    })
    expect(dashboard.cameraStatuses.value[0].activeSignals).toContain('IDENTITY_FACE_MISMATCH')
    expect(dashboard.recentAlerts.value).toHaveLength(1)
    expect(dashboard.recentAlerts.value[0].signalType).toBe('IDENTITY_FACE_MISMATCH')

    unmount()
  })

  it('updates risk score without creating camera alerts', async () => {
    const { result: dashboard, unmount } = mountComposable(() => useAiCameraDashboard(ref(149)))
    await flushPromises()

    dashboard.cameraStatuses.value = [
      { attemptId: 90, studentName: 'Student Three', status: 'OK', riskScore: 0 }
    ]

    dashboard.applyRealtimeEvent({
      type: 'RISK_UPDATED',
      attemptId: 90,
      riskScore: 15
    })

    expect(dashboard.cameraStatuses.value[0].riskScore).toBe(15)
    expect(dashboard.recentAlerts.value).toHaveLength(0)

    unmount()
  })
})
