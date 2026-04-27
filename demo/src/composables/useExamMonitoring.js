import { ref, onUnmounted } from 'vue'
import { useRealtimeChannel } from './useRealtimeChannel'
import { useProctorDashboardStore } from '../stores/proctorDashboardStore'

const STOMP_CONNECT_TIMEOUT_MS = 1500
const POLL_INTERVAL_WHEN_CONNECTED_MS = 60_000   // reconcile only when WS is healthy
const POLL_INTERVAL_WHEN_DISCONNECTED_MS = 8_000 // fallback polling

const resolveAttemptId = (card) => card?.attemptId ?? card?.id

export function useExamMonitoring() {
  const realtime = useRealtimeChannel()
  const store = useProctorDashboardStore()

  const examId = ref(null)
  const attemptSubscriptions = new Map()
  let stompTimeoutId = null
  let pollIntervalId = null
  let currentPollInterval = POLL_INTERVAL_WHEN_DISCONNECTED_MS

  const isConnected = () => realtime.isConnected.value
  const isConnecting = () => realtime.isConnecting.value

  function getAttemptTopic(attemptId) {
    return `/topic/attempts/${attemptId}/proctor-actions`
  }

  function toEventTime(payload) {
    return payload?.issuedAt || payload?.updatedAt || payload?.createdAt || new Date().toISOString()
  }

  // ── Dev logging ─────────────────────────────────────────────────────────────
  function logRealtime(type, attemptId, payload) {
    if (import.meta.env.DEV) {
      const occurredAt = payload?.occurredAt || payload?.issuedAt || payload?.updatedAt
      const latencyMs = occurredAt ? Date.now() - new Date(occurredAt).getTime() : null
      console.debug(`[Realtime] type=${type} attemptId=${attemptId} latencyMs=${latencyMs}`, payload)
    }
  }

  // ── Core: applyRealtimeEvent ───────────────────────────────────────────────
  function applyRealtimeEvent(event) {
    if (!event) return
    const type = String(event.type || '').toUpperCase()
    const attemptId = String(event.attemptId || event.id || '')
    if (!attemptId) return

    logRealtime(type, attemptId, event)

    // 1. Patch card (dashboard row)
    store.patchAttemptFromRealtime(event)

    // 2. Add alert (live alert panel)
    addAlertFromEvent(event, type, attemptId)

    // 3. Add system event
    addSystemEventFromEvent(event, type, attemptId)
  }

  function addAlertFromEvent(event, type, attemptId) {
    const signal = event.latestSignal || {}
    store.addAlert({
      attemptId,
      signalType: signal.signalType || event.signalType || type,
      type,
      severity: signal.severity || event.severity || event.riskLevel,
      confidence: signal.confidence,
      evidence: signal.evidence,
      issuedAt: signal.occurredAt || event.issuedAt
    })
  }

  function addSystemEventFromEvent(event, type, attemptId) {
    const systemEventTypes = ['WARNING_SENT', 'ATTEMPT_STOPPED', 'ATTEMPT_PAUSED', 'ATTEMPT_RESUMED']
    if (!systemEventTypes.includes(type)) return
    store.addEvent({
      attemptId,
      eventType: type,
      details: event.message,
      issuedAt: event.issuedAt || new Date().toISOString()
    })
  }

  // ── Message dispatch ───────────────────────────────────────────────────────
  function handleExamMessage(payload) {
    applyRealtimeEvent(payload)
  }

  function handleAttemptMessage(attemptId, payload) {
    applyRealtimeEvent({ ...payload, attemptId })
  }

  // ── STOMP connect ─────────────────────────────────────────────────────────
  function connect(id) {
    const nextExamId = id == null ? null : String(id)
    const currentExamId = examId.value == null ? null : String(examId.value)
    disconnect({ clearAttemptSubscriptions: currentExamId !== nextExamId })
    examId.value = id

    const topics = [
      { destination: `/topic/exams/${id}/alerts`, handler: handleExamMessage }
    ]

    // Fallback: if STOMP not connected within timeout, start polling
    stompTimeoutId = setTimeout(() => {
      if (!isConnected()) {
        store.setConnectionMode('polling')
        startPolling(POLL_INTERVAL_WHEN_DISCONNECTED_MS)
      }
    }, STOMP_CONNECT_TIMEOUT_MS)

    realtime.connect({
      reconnectDelay: 5000,
      topics,
      onConnect: () => {
        clearTimeout(stompTimeoutId)
        stompTimeoutId = null
        store.setConnectionMode('realtime')
        stopPolling()
        // Re-subscribe to per-attempt channels
        attemptSubscriptions.forEach((handler, aId) => {
          realtime.subscribe(getAttemptTopic(aId), handler)
        })
      },
      onDisconnect: () => {
        store.setConnectionMode('polling')
        if (examId.value) startPolling(POLL_INTERVAL_WHEN_DISCONNECTED_MS)
      }
    })
  }

  function disconnect({ clearAttemptSubscriptions = true } = {}) {
    clearTimeout(stompTimeoutId)
    stompTimeoutId = null
    stopPolling()
    if (clearAttemptSubscriptions) {
      unsubscribeFromAllAttempts()
    }
    realtime.disconnect()
    examId.value = null
  }

  // ── Per-student subscriptions ───────────────────────────────────────────────
  function subscribeToAttempt(attemptId, onMessage) {
    if (attemptSubscriptions.has(attemptId)) return
    const handler = (payload) => {
      handleAttemptMessage(attemptId, payload)
      onMessage?.({ ...payload, attemptId })
    }
    attemptSubscriptions.set(attemptId, handler)
    if (isConnected()) {
      realtime.subscribe(getAttemptTopic(attemptId), handler)
    } else if (!isConnecting() && examId.value == null) {
      void realtime.connect({
        reconnectDelay: 5000,
        topics: [{ destination: getAttemptTopic(attemptId), handler }]
      })
    }
  }

  function unsubscribeFromAttempt(attemptId) {
    const handler = attemptSubscriptions.get(attemptId)
    if (handler) {
      if (isConnected()) {
        realtime.unsubscribe(getAttemptTopic(attemptId))
      }
      attemptSubscriptions.delete(attemptId)
    }
  }

  function unsubscribeFromAllAttempts() {
    attemptSubscriptions.forEach((handler, attemptId) => {
      if (isConnected()) {
        realtime.unsubscribe(getAttemptTopic(attemptId))
      }
    })
    attemptSubscriptions.clear()
  }

  // ── Polling: adaptive interval ────────────────────────────────────────────
  function startPolling(intervalMs) {
    if (pollIntervalId) return
    currentPollInterval = intervalMs || POLL_INTERVAL_WHEN_DISCONNECTED_MS
    pollIntervalId = setInterval(async () => {
      if (!examId.value) return
      const { fetchExamAttempts } = await import('../services/examMonitoringService').catch(() => ({ fetchExamAttempts: null }))
      if (!fetchExamAttempts || !examId.value) return
      try {
        const attempts = await fetchExamAttempts(examId.value)
        if (Array.isArray(attempts)) {
          store.setCards(attempts)
        }
      } catch { /* silent fallback */
      }
    }, currentPollInterval)
  }

  function stopPolling() {
    if (pollIntervalId) {
      clearInterval(pollIntervalId)
      pollIntervalId = null
    }
  }

  // ── Reconciliation: called after websocket reconnect ───────────────────────
  async function reconcile(examIdVal) {
    if (!examIdVal) return
    const { fetchExamAttempts } = await import('../services/examMonitoringService').catch(() => ({ fetchExamAttempts: null }))
    if (!fetchExamAttempts) return
    try {
      const attempts = await fetchExamAttempts(examIdVal)
      if (Array.isArray(attempts)) {
        store.setCards(attempts)
      }
    } catch { /* silent */
    }
  }

  onUnmounted(() => {
    disconnect()
  })

  return {
    isConnected,
    isConnecting,
    connect,
    disconnect,
    subscribeToAttempt,
    unsubscribeFromAttempt,
    applyRealtimeEvent,
    reconcile
  }
}
