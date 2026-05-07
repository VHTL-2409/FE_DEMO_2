import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { ref } from 'vue'
import { useProctorDashboardStore } from '../stores/proctorDashboardStore'

const mockRealtime = {
  isConnected: ref(false),
  isConnecting: ref(false),
  connect: vi.fn(async ({ topics = [], onConnect } = {}) => {
    mockRealtime.isConnected.value = true
    mockRealtime.topics = topics
    onConnect?.()
  }),
  disconnect: vi.fn(() => {
    mockRealtime.isConnected.value = false
  }),
  subscribe: vi.fn(),
  unsubscribe: vi.fn()
}

vi.mock('./useRealtimeChannel', () => ({
  useRealtimeChannel: () => mockRealtime
}))

describe('useExamMonitoring', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    mockRealtime.isConnected.value = false
    mockRealtime.isConnecting.value = false
    mockRealtime.topics = []
    mockRealtime.connect.mockClear()
    mockRealtime.disconnect.mockClear()
    mockRealtime.subscribe.mockClear()
    mockRealtime.unsubscribe.mockClear()
  })

  it('updates dashboard state from RISK_UPDATED and WARNING_SENT events', async () => {
    const { useExamMonitoring } = await import('./useExamMonitoring')
    const monitoring = useExamMonitoring()
    const store = useProctorDashboardStore()

    await monitoring.connect(149)

    expect(mockRealtime.connect).toHaveBeenCalledTimes(1)
    expect(mockRealtime.topics).toHaveLength(1)

    const examHandler = mockRealtime.topics[0].handler
    examHandler({
      type: 'RISK_UPDATED',
      examId: 149,
      attemptId: 126,
      student: 'student1',
      riskScore: 67,
      riskLevel: 'HIGH_RISK',
      reviewRequired: true,
      recommendedAction: 'WARN_AND_ESCALATE',
      reasons: ['Can thiep ky thuat']
    })
    examHandler({
      type: 'WARNING_SENT',
      examId: 149,
      attemptId: 126,
      message: 'Keep the exam in focus'
    })

    expect(store.cards).toHaveLength(1)
    expect(store.cards[0].attemptId).toBe(126)
    expect(store.cards[0].riskScore).toBe(67)
    expect(store.cards[0].reviewRequired).toBe(true)
    expect(store.liveAlerts[0].attemptId).toBe(126)
    expect(store.liveEvents[0].eventType).toBe('WARNING_SENT')
  })

  it('creates a new attempt card from ATTEMPT_STARTED events without treating it as an alert', async () => {
    const { useExamMonitoring } = await import('./useExamMonitoring')
    const monitoring = useExamMonitoring()
    const store = useProctorDashboardStore()

    await monitoring.connect(149)

    const examHandler = mockRealtime.topics[0].handler
    examHandler({
      type: 'ATTEMPT_STARTED',
      examId: 149,
      attemptId: 301,
      student: 'student1',
      studentName: 'Student One',
      email: 'student1@example.com',
      studentCode: 'S001',
      status: 'IN_PROGRESS',
      startedAt: '2026-05-07T10:00:00+07:00',
      cameraOn: true,
      micOn: true,
      issuedAt: '2026-05-07T10:00:00+07:00'
    })

    expect(store.cards).toHaveLength(1)
    expect(store.cards[0].attemptId).toBe(301)
    expect(store.cards[0].status).toBe('IN_PROGRESS')
    expect(store.liveEvents[0].eventType).toBe('ATTEMPT_STARTED')
    expect(store.liveAlerts).toHaveLength(0)
  })

  it('handles ATTEMPT_JOINED events the same way as presence events', async () => {
    const { useExamMonitoring } = await import('./useExamMonitoring')
    const monitoring = useExamMonitoring()
    const store = useProctorDashboardStore()

    await monitoring.connect(149)

    const examHandler = mockRealtime.topics[0].handler
    examHandler({
      type: 'ATTEMPT_JOINED',
      examId: 149,
      attemptId: 302,
      student: 'student2',
      studentName: 'Student Two',
      email: 'student2@example.com',
      studentCode: 'S002',
      status: 'IN_PROGRESS',
      startedAt: '2026-05-07T10:05:00+07:00',
      cameraOn: true,
      micOn: true,
      issuedAt: '2026-05-07T10:05:00+07:00'
    })

    expect(store.cards).toHaveLength(1)
    expect(store.cards[0].attemptId).toBe(302)
    expect(store.cards[0].status).toBe('IN_PROGRESS')
    expect(store.liveEvents[0].eventType).toBe('ATTEMPT_JOINED')
    expect(store.liveAlerts).toHaveLength(0)
  })

  it('updates attempt cards immediately from raw fraud-signal events', async () => {
    const { useExamMonitoring } = await import('./useExamMonitoring')
    const monitoring = useExamMonitoring()
    const store = useProctorDashboardStore()

    store.setCards([{ id: 126, attemptId: 126, student: 'student1', violationCount: 2 }])
    await monitoring.connect(149)

    const examHandler = mockRealtime.topics[0].handler
    examHandler({
      type: 'FRAUD_SIGNAL_RECORDED',
      examId: 149,
      attemptId: 126,
      student: 'student1',
      signalType: 'TAB_SWITCH',
      severity: 'LOW',
      issuedAt: '2026-04-26T18:00:00+07:00'
    })

    expect(store.cards[0].violationCount).toBe(3)
    expect(store.cards[0].latestSignalType).toBe('TAB_SWITCH')
    expect(store.cards[0].lastSignalAt).toBe('2026-04-26T18:00:00+07:00')
    expect(store.liveAlerts[0].signalType).toBe('TAB_SWITCH')
  })

  it('clears stale attempt subscriptions when switching monitored exam', async () => {
    const { useExamMonitoring } = await import('./useExamMonitoring')
    const monitoring = useExamMonitoring()

    await monitoring.connect(149)
    monitoring.subscribeToAttempt(127, vi.fn())

    expect(mockRealtime.subscribe).toHaveBeenCalledWith('/topic/attempts/127/proctor-actions', expect.any(Function))

    await monitoring.connect(150)

    expect(mockRealtime.unsubscribe).toHaveBeenCalledWith('/topic/attempts/127/proctor-actions')
  })
})
