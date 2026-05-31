import { onUnmounted, watch } from 'vue'

export const DEFAULT_MICROPHONE_SPEECH_VAD_OPTIONS = Object.freeze({
  sampleIntervalMs: 250,
  speechThreshold: 0.035,
  minSpeechMs: 1200,
  silenceResetMs: 600,
  cooldownMs: 30000,
  fftSize: 1024
})

const defaultAudioContextFactory = () => {
  const AudioContextCtor =
    globalThis.AudioContext ||
    globalThis.webkitAudioContext ||
    null
  return AudioContextCtor ? new AudioContextCtor() : null
}

const computeRms = (samples) => {
  if (!samples || !samples.length) return 0
  let sum = 0
  for (let i = 0; i < samples.length; i += 1) {
    const value = samples[i] || 0
    sum += value * value
  }
  return Math.sqrt(sum / samples.length)
}

const createFloatBuffer = (size) => new Float32Array(Math.max(32, size || DEFAULT_MICROPHONE_SPEECH_VAD_OPTIONS.fftSize))

export function createMicrophoneSpeechDetector({
  getStream,
  isEnabled = () => true,
  onSpeechDetected = () => {},
  audioContextFactory = defaultAudioContextFactory,
  setIntervalFn = globalThis.setInterval?.bind(globalThis),
  clearIntervalFn = globalThis.clearInterval?.bind(globalThis),
  now = () => Date.now(),
  options = {}
} = {}) {
  const settings = {
    ...DEFAULT_MICROPHONE_SPEECH_VAD_OPTIONS,
    ...options
  }

  const state = {
    running: false,
    track: null,
    audioContext: null,
    analyser: null,
    sourceNode: null,
    intervalId: null,
    buffer: null,
    speechStartedAt: null,
    silenceMs: 0,
    lastEmitAt: 0,
    noiseFloor: null
  }

  const resetRuntime = () => {
    if (state.intervalId != null) {
      clearIntervalFn?.(state.intervalId)
      state.intervalId = null
    }

    if (state.sourceNode?.disconnect) {
      try {
        state.sourceNode.disconnect()
      } catch {
        // ignore
      }
    }

    if (state.analyser?.disconnect) {
      try {
        state.analyser.disconnect()
      } catch {
        // ignore
      }
    }

    if (state.audioContext?.close) {
      try {
        void state.audioContext.close()?.catch?.(() => {})
      } catch {
        // ignore
      }
    }

    state.audioContext = null
    state.analyser = null
    state.sourceNode = null
    state.buffer = null
    state.track = null
    state.running = false
    state.speechStartedAt = null
    state.silenceMs = 0
    state.lastEmitAt = 0
    state.noiseFloor = null
  }

  const emitSpeech = (evidence) => {
    try {
      onSpeechDetected?.(evidence)
    } catch {
      // ignore callback failure
    }
  }

  const getActiveTrack = () => {
    if (!isEnabled?.()) return null
    const stream = getStream?.()
    const track = stream?.getAudioTracks?.()?.[0] || null
    if (!track) return null
    if (track.readyState === 'ended' || track.enabled === false) return null
    return track
  }

  const sample = () => {
    if (!state.running) return

    const track = getActiveTrack()
    if (!track) {
      stop()
      return
    }

    const analyser = state.analyser
    if (!analyser) {
      stop()
      return
    }

    const buffer = state.buffer || createFloatBuffer(analyser.fftSize)
    state.buffer = buffer

    if (typeof analyser.getFloatTimeDomainData === 'function') {
      analyser.getFloatTimeDomainData(buffer)
    } else if (typeof analyser.getByteTimeDomainData === 'function') {
      const byteBuffer = new Uint8Array(buffer.length)
      analyser.getByteTimeDomainData(byteBuffer)
      for (let i = 0; i < byteBuffer.length; i += 1) {
        buffer[i] = (byteBuffer[i] - 128) / 128
      }
    } else {
      stop()
      return
    }

    const rms = computeRms(buffer)
    const threshold = settings.speechThreshold
    const sampleAt = now()

    if (rms >= threshold) {
      state.silenceMs = 0
      if (state.speechStartedAt == null) {
        state.speechStartedAt = sampleAt
      }

      if (
        sampleAt - state.speechStartedAt >= settings.minSpeechMs &&
        sampleAt - state.lastEmitAt >= settings.cooldownMs
      ) {
        state.lastEmitAt = sampleAt
        emitSpeech({
          eventSource: 'microphone_vad',
          speechDurationMs: Math.max(0, sampleAt - state.speechStartedAt),
          speechRms: Number(rms.toFixed(4)),
          speechThreshold: threshold,
          speechNoiseFloor: state.noiseFloor == null ? null : Number(state.noiseFloor.toFixed(4)),
          sampleIntervalMs: settings.sampleIntervalMs,
          trackEnabled: Boolean(track.enabled),
          trackReadyState: track.readyState || 'live',
          trackPresent: true
        })
      }
      return
    }

    if (state.speechStartedAt != null) {
      state.silenceMs += settings.sampleIntervalMs
      if (state.silenceMs >= settings.silenceResetMs) {
        state.speechStartedAt = null
        state.silenceMs = 0
      }
    }

    if (state.noiseFloor == null || rms < state.noiseFloor) {
      state.noiseFloor = rms
    }
  }

  const start = () => {
    if (state.running) return true

    const track = getActiveTrack()
    if (!track) return false

    let audioContext
    let analyser
    let sourceNode
    try {
      audioContext = audioContextFactory?.()
      if (!audioContext || typeof audioContext.createAnalyser !== 'function' || typeof audioContext.createMediaStreamSource !== 'function') {
        return false
      }

      analyser = audioContext.createAnalyser()
      analyser.fftSize = settings.fftSize

      sourceNode = audioContext.createMediaStreamSource(getStream?.())
      sourceNode.connect(analyser)
      if (typeof audioContext.resume === 'function') {
        void audioContext.resume().catch(() => {})
      }
    } catch {
      try {
        void audioContext?.close?.()?.catch?.(() => {})
      } catch {
        // ignore
      }
      return false
    }

    state.audioContext = audioContext
    state.analyser = analyser
    state.sourceNode = sourceNode
    state.track = track
    state.buffer = createFloatBuffer(analyser.fftSize)
    state.speechStartedAt = null
    state.silenceMs = 0
    state.lastEmitAt = 0
    state.noiseFloor = null
    state.running = true

    if (typeof setIntervalFn === 'function') {
      state.intervalId = setIntervalFn(sample, settings.sampleIntervalMs)
    }

    sample()
    return true
  }

  const stop = () => {
    if (!state.running && state.intervalId == null && !state.audioContext) {
      return
    }
    resetRuntime()
  }

  const sync = () => {
    if (!isEnabled?.()) {
      stop()
      return false
    }

    const track = getActiveTrack()
    if (!track) {
      stop()
      return false
    }

    if (!state.running) {
      return start()
    }

    if (state.track !== track) {
      stop()
      return start()
    }

    return true
  }

  return {
    start,
    stop,
    sync,
    isRunning: () => state.running
  }
}

export function useMicrophoneSpeechDetector({
  streamRef,
  enabledRef,
  onSpeechDetected,
  options,
  audioContextFactory,
  setIntervalFn,
  clearIntervalFn,
  now
} = {}) {
  const detector = createMicrophoneSpeechDetector({
    getStream: () => streamRef?.value ?? null,
    isEnabled: () => Boolean(enabledRef?.value),
    onSpeechDetected,
    options,
    audioContextFactory,
    setIntervalFn,
    clearIntervalFn,
    now
  })

  watch([streamRef, enabledRef], () => {
    detector.sync()
  }, { immediate: true })

  onUnmounted(() => {
    detector.stop()
  })

  return detector
}
