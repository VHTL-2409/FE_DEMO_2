import { beforeEach, describe, expect, it, vi } from 'vitest'

const clientInstances = []
let mockToken = 'test-token'

vi.mock('@stomp/stompjs', () => ({
  Client: vi.fn().mockImplementation((config) => {
    const instance = {
      ...config,
      connected: false,
      activate: vi.fn(function activate() {
        instance.connected = true
        instance.onConnect?.()
      }),
      deactivate: vi.fn(),
      subscribe: vi.fn(() => ({ unsubscribe: vi.fn() })),
      publish: vi.fn()
    }
    clientInstances.push(instance)
    return instance
  })
}))

vi.mock('sockjs-client', () => ({
  default: vi.fn(() => ({}))
}))

vi.mock('../services/authService', () => ({
  getStoredToken: () => mockToken
}))

describe('useRealtimeChannel', () => {
  beforeEach(() => {
    vi.useFakeTimers()
    vi.resetModules()
    clientInstances.length = 0
    mockToken = 'test-token'
    localStorage.clear()
  })

  it('does not publish custom app heartbeat frames after connect', async () => {
    const { useRealtimeChannel } = await import('./useRealtimeChannel')
    const channel = useRealtimeChannel()
    const handler = vi.fn()

    await channel.connect({
      topics: [{ destination: '/topic/exams/1/alerts', handler }]
    })

    expect(channel.isConnected.value).toBe(true)
    expect(clientInstances).toHaveLength(1)

    vi.advanceTimersByTime(60000)

    expect(clientInstances[0].publish).not.toHaveBeenCalled()
    expect(clientInstances[0].subscribe).toHaveBeenCalledWith('/topic/exams/1/alerts', expect.any(Function))
  })

  it('skips websocket connect and reconnect scheduling when auth token is missing', async () => {
    mockToken = ''
    const { useRealtimeChannel } = await import('./useRealtimeChannel')
    const channel = useRealtimeChannel()

    await channel.connect({
      topics: [{ destination: '/topic/exams/1/alerts', handler: vi.fn() }]
    })

    expect(channel.isConnected.value).toBe(false)
    expect(channel.isConnecting.value).toBe(false)
    expect(clientInstances).toHaveLength(0)
  })
})
