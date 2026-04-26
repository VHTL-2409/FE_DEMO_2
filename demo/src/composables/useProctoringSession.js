import { onUnmounted, ref } from 'vue'
import { sendMonitoringEventBatch, sendMonitoringHeartbeat } from '../services/monitoringService'

const getBrowserContextSnapshot = () => ({
  fullscreen: Boolean(document.fullscreenElement),
  visibility: document.visibilityState || 'visible',
  focused: document.hasFocus?.() ?? true,
  screenWidth: window.screen?.width || null,
  screenHeight: window.screen?.height || null,
  viewportWidth: window.innerWidth || null,
  viewportHeight: window.innerHeight || null,
  platform: navigator.platform || '',
  userAgent: navigator.userAgent || '',
  networkType: navigator.connection?.effectiveType || '',
  online: navigator.onLine !== false
})

export function useProctoringSession({
  getAttemptId,
  getDeviceFingerprint,
  getHeartbeatPayload,
  onRiskUpdate,
  onActionRequired,
  batchWindowMs = 1500,
  heartbeatIntervalMs = 15000
}) {
  const pendingCount = ref(0)
  const batchSequence = ref(Date.now())
  let pendingEvents = []
  let flushTimer = null
  let heartbeatTimer = null
  let lastHeartbeatStartedAt = 0

  const emitAction = (response) => {
    if (!response) return
    if (Array.isArray(response.requiredActions) && response.requiredActions.length) {
      onActionRequired?.(response.requiredActions, response)
      return
    }
    if (response.actionTaken && response.actionTaken !== 'NONE') {
      onActionRequired?.([response.actionTaken], response)
    }
  }

  const flush = async () => {
    if (!pendingEvents.length) return null
    const attemptId = getAttemptId?.()
    if (!attemptId) return null

    const queuedEvents = pendingEvents
    pendingEvents = []
    pendingCount.value = 0
    if (flushTimer) {
      window.clearTimeout(flushTimer)
      flushTimer = null
    }

    const payload = {
      sequence: batchSequence.value,
      deviceFingerprint: getDeviceFingerprint?.() || '',
      browserContext: getBrowserContextSnapshot(),
      events: queuedEvents
    }
    batchSequence.value += queuedEvents.length

    try {
      const response = await sendMonitoringEventBatch(attemptId, payload)
      onRiskUpdate?.(response)
      emitAction(response)
      return response
    } catch (error) {
      pendingEvents = [...queuedEvents, ...pendingEvents]
      pendingCount.value = pendingEvents.length
      throw error
    }
  }

  const queueEvent = (eventType, details = '', payload = null, confidence = null, telemetry = null, clientTimestamp = null) => {
    pendingEvents = [
      ...pendingEvents,
      {
        eventType,
        details,
        payload,
        confidence,
        telemetry,
        clientTimestamp: clientTimestamp || Date.now()
      }
    ]
    pendingCount.value = pendingEvents.length
    if (flushTimer) {
      window.clearTimeout(flushTimer)
    }
    flushTimer = window.setTimeout(() => {
      void flush()
    }, batchWindowMs)
  }

  const syncHeartbeat = async () => {
    const attemptId = getAttemptId?.()
    if (!attemptId) return null
    try {
      if (pendingEvents.length) {
        await flush()
      }
      const now = Date.now()
      const heartbeatLagMs = lastHeartbeatStartedAt ? Math.max(0, now - lastHeartbeatStartedAt - heartbeatIntervalMs) : 0
      lastHeartbeatStartedAt = now
      const heartbeatPayload = getHeartbeatPayload?.() || {}
      const response = await sendMonitoringHeartbeat(attemptId, {
        ...heartbeatPayload,
        deviceFingerprint: getDeviceFingerprint?.() || '',
        telemetry: {
          ...(heartbeatPayload.telemetry || {}),
          networkOnline: navigator.onLine !== false,
          networkType: navigator.connection?.effectiveType || '',
          heartbeatLagMs
        }
      })
      onRiskUpdate?.(response)
      emitAction(response)
      return response
    } catch {
      return null
    }
  }

  const startHeartbeat = () => {
    if (heartbeatTimer) {
      window.clearInterval(heartbeatTimer)
    }
    heartbeatTimer = window.setInterval(() => {
      void syncHeartbeat()
    }, heartbeatIntervalMs)
  }

  const stopHeartbeat = () => {
    if (heartbeatTimer) {
      window.clearInterval(heartbeatTimer)
      heartbeatTimer = null
    }
  }

  onUnmounted(() => {
    stopHeartbeat()
    if (flushTimer) {
      window.clearTimeout(flushTimer)
      flushTimer = null
    }
  })

  return {
    pendingCount,
    queueEvent,
    flush,
    syncHeartbeat,
    startHeartbeat,
    stopHeartbeat
  }
}
