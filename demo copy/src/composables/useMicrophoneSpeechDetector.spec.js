import { describe, expect, it, vi, beforeEach } from 'vitest'
import { createMicrophoneSpeechDetector } from './useMicrophoneSpeechDetector'

class FakeAnalyser {
  constructor(level = 0) {
    this.level = level
    this.fftSize = 1024
  }

  getFloatTimeDomainData(buffer) {
    buffer.fill(this.level)
  }
}

class FakeSourceNode {
  connect(node) {
    this.connectedTo = node
  }

  disconnect() {
    this.disconnected = true
  }
}

class FakeAudioContext {
  constructor(level = 0) {
    this.state = 'running'
    this.analyser = new FakeAnalyser(level)
    this.source = new FakeSourceNode()
    this.close = vi.fn().mockResolvedValue(undefined)
    this.resume = vi.fn().mockResolvedValue(undefined)
  }

  createAnalyser() {
    return this.analyser
  }

  createMediaStreamSource() {
    return this.source
  }
}

describe('createMicrophoneSpeechDetector', () => {
  let currentTime
  let intervalCallback
  let scheduledIntervals

  const track = { enabled: true, readyState: 'live' }
  const stream = {
    getAudioTracks: () => [track]
  }

  beforeEach(() => {
    currentTime = 0
    intervalCallback = null
    scheduledIntervals = []
  })

  const buildDetector = ({ level = 0.1, enabled = true, streamOverride = stream, onSpeechDetected = vi.fn() } = {}) => {
    const audioContext = new FakeAudioContext(level)
    const detector = createMicrophoneSpeechDetector({
      getStream: () => streamOverride,
      isEnabled: () => enabled,
      onSpeechDetected,
      audioContextFactory: () => audioContext,
      setIntervalFn: (callback) => {
        intervalCallback = callback
        scheduledIntervals.push(callback)
        return scheduledIntervals.length
      },
      clearIntervalFn: vi.fn(),
      now: () => currentTime,
      options: {
        sampleIntervalMs: 250,
        speechThreshold: 0.035,
        minSpeechMs: 1000,
        silenceResetMs: 500,
        cooldownMs: 1000,
        fftSize: 1024
      }
    })

    return { detector, audioContext, onSpeechDetected }
  }

  it('emits once per cooldown during sustained speech', () => {
    const { detector, onSpeechDetected } = buildDetector({ level: 0.1 })

    detector.sync()
    expect(scheduledIntervals).toHaveLength(1)

    for (let i = 0; i < 4; i += 1) {
      currentTime += 250
      intervalCallback()
    }

    expect(onSpeechDetected).toHaveBeenCalledTimes(1)
    expect(onSpeechDetected.mock.calls[0][0]).toMatchObject({
      eventSource: 'microphone_vad',
      speechDurationMs: 1000,
      trackEnabled: true,
      trackReadyState: 'live'
    })

    for (let i = 0; i < 4; i += 1) {
      currentTime += 250
      intervalCallback()
    }

    expect(onSpeechDetected).toHaveBeenCalledTimes(2)
  })

  it('ignores short noise bursts', () => {
    const { detector, onSpeechDetected } = buildDetector({ level: 0.01 })

    detector.sync()

    for (let i = 0; i < 8; i += 1) {
      currentTime += 250
      intervalCallback()
    }

    expect(onSpeechDetected).not.toHaveBeenCalled()
  })

  it('stops cleanly when microphone is unavailable', () => {
    const { detector, onSpeechDetected } = buildDetector({
      streamOverride: { getAudioTracks: () => [] }
    })

    expect(() => detector.sync()).not.toThrow()
    expect(onSpeechDetected).not.toHaveBeenCalled()
  })
})
