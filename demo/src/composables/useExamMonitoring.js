import { ref, computed, onUnmounted } from 'vue'
import { useRealtimeChannel } from './useRealtimeChannel'
import { useProctorDashboardStore } from '../stores/proctorDashboardStore'

const STOMP_CONNECT_TIMEOUT_MS = 3000
const POLL_INTERVAL_MS = 30000

export function useExamMonitoring() {
  const realtime = useRealtimeChannel()
  const store = useProctorDashboardStore()

  const examId = ref(null)
  const attemptSubscriptions = new Map()
  const pollTimers = new Map()
  let stompTimeoutId = null
  let pollIntervalId = null

  const isConnected = computed(() => realtime.isConnected)
  const isConnecting = computed(() => realtime.isConnecting)

  // ── Message dispatch ──────────────────────────────────────────────────────

  function handleExamMessage(payload) {
    if (!payload) return
    const type = String(payload.type || '').toUpperCase()

    if (type === 'RISK_UPDATED' || type === 'SUSPICIOUS') {
      store.upsertCard({ attemptId: payload.attemptId, riskScore: payload.riskScore, riskLevel: payload.riskLevel, reasons: payload.reasons })
      store.addAlert({ attemptId: payload.attemptId, type: payload.signalType || type, severity: payload.severity, message: payload.message })
    } else if (type === 'WARNING_SENT') {
      store.upsertCard({ attemptId: payload.attemptId, lastWarning: payload.message })
      store.addEvent({ attemptId: payload.attemptId, eventType: 'WARNING_SENT', details: payload.message })
    } else if (type === 'ATTEMPT_STOPPED') {
      store.upsertCard({ attemptId: payload.attemptId, status: 'STOPPED' })
      store.addEvent({ attemptId: payload.attemptId, eventType: 'ATTEMPT_STOPPED', details: payload.message })
    } else if (type === 'ATTEMPT_PAUSED') {
      store.upsertCard({ attemptId: payload.attemptId, status: 'PAUSED' })
      store.addEvent({ attemptId: payload.attemptId, eventType: 'ATTEMPT_PAUSED', details: payload.message })
    } else if (type === 'ATTEMPT_RESUMED') {
      store.upsertCard({ attemptId: payload.attemptId, status: 'IN_PROGRESS' })
      store.addEvent({ attemptId: payload.attemptId, eventType: 'ATTEMPT_RESUMED', details: payload.message })
    } else if (type === 'DRAFT_SAVED') {
      store.upsertCard({ attemptId: payload.attemptId, draftSavedAt: payload.savedAt })
    }
  }

  function handleAttemptMessage(attemptId, payload) {
    handleExamMessage({ ...payload, attemptId })
  }

  // ── STOMP connect ────────────────────────────────────────────────────────

  function connect(id) {
    examId.value = id
    disconnect()

    const topics = [
      { destination: `/topic/exams/${id}/alerts`, handler: handleExamMessage }
    ]
    // per-student subscriptions added by subscribeToAttempt()
    attemptSubscriptions.forEach((handler, aId) => {
      topics.push({ destination: `/topic/attempts/${aId}/proctor-actions`, handler: (p) => handleAttemptMessage(aId, p) })
    })

    // Fallback: if STOMP not connected within timeout, start polling
    stompTimeoutId = setTimeout(() => {
      if (!realtime.isConnected) {
        startPolling()
      }
    }, STOMP_CONNECT_TIMEOUT_MS)

    realtime.connect({
      reconnectDelay: 5000,
      topics,
      onConnect: () => {
        clearTimeout(stompTimeoutId)
        stompTimeoutId = null
        stopPolling()
      },
      onDisconnect: () => {
        if (examId.value) startPolling()
      }
    })
  }

  function disconnect() {
    clearTimeout(stompTimeoutId)
    stompTimeoutId = null
    stopPolling()
    unsubscribeFromAllAttempts()
    realtime.disconnect()
    examId.value = null
  }

  // ── Per-student subscriptions ─────────────────────────────────────────────

  function subscribeToAttempt(attemptId) {
    if (attemptSubscriptions.has(attemptId)) return
    const handler = (payload) => handleAttemptMessage(attemptId, payload)
    attemptSubscriptions.set(attemptId, handler)
    if (realtime.isConnected) {
      realtime.subscribe(`/topic/attempts/${attemptId}/proctor-actions`, handler)
    }
  }

  function unsubscribeFromAttempt(attemptId) {
    const handler = attemptSubscriptions.get(attemptId)
    if (handler) {
      if (realtime.isConnected) {
        realtime.unsubscribe(`/topic/attempts/${attemptId}/proctor-actions`, handler)
      }
      attemptSubscriptions.delete(attemptId)
    }
  }

  function unsubscribeFromAllAttempts() {
    attemptSubscriptions.forEach((handler, attemptId) => {
      if (realtime.isConnected) {
        realtime.unsubscribe(`/topic/attempts/${attemptId}/proctor-actions`, handler)
      }
    })
    attemptSubscriptions.clear()
  }

  // ── Polling fallback ─────────────────────────────────────────────────────

  function startPolling() {
    if (pollIntervalId) return
    pollIntervalId = setInterval(async () => {
      if (!examId.value) return
      // Import dynamically to avoid circular deps — poll is triggered
      // when STOMP is unavailable; polling is best-effort refresh.
      const { fetchExamAttempts } = await import('../services/examMonitoringService').catch(() => ({ fetchExamAttempts: null }))
      if (fetchExamAttempts && examId.value) {
        try {
          const attempts = await fetchExamAttempts(examId.value)
          if (Array.isArray(attempts)) {
            store.setCards(attempts)
          }
        } catch { /* silent */ }
      }
    }, POLL_INTERVAL_MS)
  }

  function stopPolling() {
    if (pollIntervalId) {
      clearInterval(pollIntervalId)
      pollIntervalId = null
    }
    pollTimers.forEach(t => clearInterval(t))
    pollTimers.clear()
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
    unsubscribeFromAttempt
  }
}
