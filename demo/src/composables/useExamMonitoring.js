import { ref, computed, onUnmounted } from 'vue'
import { useRealtimeChannel } from './useRealtimeChannel'
import { useProctorDashboardStore } from '../stores/proctorDashboardStore'

const STOMP_CONNECT_TIMEOUT_MS = 3000
const POLL_INTERVAL_MS = 30000
const resolveAttemptId = (card) => card?.attemptId ?? card?.id

export function useExamMonitoring() {
  const realtime = useRealtimeChannel()
  const store = useProctorDashboardStore()

  const examId = ref(null)
  const attemptSubscriptions = new Map()
  const pollTimers = new Map()
  let stompTimeoutId = null
  let pollIntervalId = null

  const isConnected = computed(() => realtime.isConnected.value)
  const isConnecting = computed(() => realtime.isConnecting.value)

  function getAttemptTopic(attemptId) {
    return `/topic/attempts/${attemptId}/proctor-actions`
  }

  function toEventTime(payload) {
    return payload?.issuedAt || payload?.updatedAt || payload?.createdAt || new Date().toISOString()
  }

  function signalLabel(payload, fallbackType) {
    return payload?.signalType || payload?.eventType || payload?.type || fallbackType
  }

  // ── Message dispatch ──────────────────────────────────────────────────────

  function handleExamMessage(payload) {
    if (!payload) return
    const type = String(payload.type || '').toUpperCase()

    const payloadAttemptId = payload.attemptId || payload.id

    if (type === 'RISK_UPDATED' || type === 'SUSPICIOUS' || type === 'SUSPICIOUS_ALERT') {
      store.upsertCard({
        id: payloadAttemptId,
        attemptId: payloadAttemptId,
        examId: payload.examId,
        student: payload.student || payload.studentName,
        riskScore: payload.riskScore,
        riskLevel: payload.riskLevel,
        reviewRequired: payload.reviewRequired,
        recommendedAction: payload.recommendedAction,
        reasons: payload.reasons,
        evidenceSummary: payload.evidenceSummary,
        activeFlagId: payload.activeFlagId,
        activeFlagStatus: payload.activeFlagStatus,
        latestFlagTitle: payload.latestFlagTitle,
        lastRiskUpdatedAt: toEventTime(payload)
      })
      store.addAlert({
        attemptId: payloadAttemptId,
        signalType: signalLabel(payload, type),
        type: signalLabel(payload, type),
        severity: payload.severity || payload.riskLevel,
        message: payload.message,
        issuedAt: toEventTime(payload)
      })
    } else if (type === 'FRAUD_SIGNAL_RECORDED') {
      const currentCard = store.cards.find((item) => String(item.attemptId || item.id) === String(payloadAttemptId))
      store.upsertCard({
        id: payloadAttemptId,
        attemptId: payloadAttemptId,
        examId: payload.examId,
        student: payload.student || payload.studentName,
        violationCount: Number(currentCard?.violationCount || 0) + 1,
        lastSignalAt: toEventTime(payload),
        latestSignalType: payload.signalType,
        latestSignalSeverity: payload.severity
      })
      store.addAlert({
        attemptId: payloadAttemptId,
        signalType: signalLabel(payload, type),
        type,
        severity: payload.severity,
        confidence: payload.confidence,
        evidence: payload.evidence,
        issuedAt: toEventTime(payload)
      })
    } else if (type === 'WARNING_SENT') {
      store.upsertCard({ id: payloadAttemptId, attemptId: payloadAttemptId, lastWarning: payload.message, lastSignalAt: toEventTime(payload) })
      store.addEvent({ attemptId: payloadAttemptId, eventType: 'WARNING_SENT', details: payload.message, issuedAt: toEventTime(payload) })
    } else if (type === 'ATTEMPT_STOPPED') {
      store.upsertCard({ id: payloadAttemptId, attemptId: payloadAttemptId, status: 'STOPPED', lastSignalAt: toEventTime(payload) })
      store.addEvent({ attemptId: payloadAttemptId, eventType: 'ATTEMPT_STOPPED', details: payload.message, issuedAt: toEventTime(payload) })
    } else if (type === 'ATTEMPT_PAUSED') {
      store.upsertCard({ id: payloadAttemptId, attemptId: payloadAttemptId, status: 'PAUSED', riskScore: payload.riskScore, lastSignalAt: toEventTime(payload) })
      store.addEvent({ attemptId: payloadAttemptId, eventType: 'ATTEMPT_PAUSED', details: payload.message, issuedAt: toEventTime(payload) })
    } else if (type === 'ATTEMPT_RESUMED') {
      store.upsertCard({ id: payloadAttemptId, attemptId: payloadAttemptId, status: 'IN_PROGRESS', riskScore: payload.riskScore, lastSignalAt: toEventTime(payload) })
      store.addEvent({ attemptId: payloadAttemptId, eventType: 'ATTEMPT_RESUMED', details: payload.message, issuedAt: toEventTime(payload) })
    } else if (type === 'DRAFT_SAVED') {
      store.upsertCard({ id: payloadAttemptId, attemptId: payloadAttemptId, draftSavedAt: payload.savedAt })
    }
  }

  function handleAttemptMessage(attemptId, payload) {
    handleExamMessage({ ...payload, attemptId })
  }

  // ── STOMP connect ────────────────────────────────────────────────────────

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
      if (!realtime.isConnected.value) {
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
        attemptSubscriptions.forEach((handler, aId) => {
          realtime.subscribe(getAttemptTopic(aId), handler)
        })
      },
      onDisconnect: () => {
        if (examId.value) startPolling()
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

  // ── Per-student subscriptions ─────────────────────────────────────────────

  function subscribeToAttempt(attemptId, onMessage) {
    if (attemptSubscriptions.has(attemptId)) return
    const handler = (payload) => {
      handleAttemptMessage(attemptId, payload)
      onMessage?.({ ...payload, attemptId })
    }
    attemptSubscriptions.set(attemptId, handler)
    if (realtime.isConnected.value) {
      realtime.subscribe(getAttemptTopic(attemptId), handler)
    } else if (!realtime.isConnecting.value && examId.value == null) {
      void realtime.connect({
        reconnectDelay: 5000,
        topics: [{ destination: getAttemptTopic(attemptId), handler }]
      })
    }
  }

  function unsubscribeFromAttempt(attemptId) {
    const handler = attemptSubscriptions.get(attemptId)
    if (handler) {
      if (realtime.isConnected.value) {
        realtime.unsubscribe(getAttemptTopic(attemptId))
      }
      attemptSubscriptions.delete(attemptId)
    }
  }

  function unsubscribeFromAllAttempts() {
    attemptSubscriptions.forEach((handler, attemptId) => {
      if (realtime.isConnected.value) {
        realtime.unsubscribe(getAttemptTopic(attemptId))
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
      const { fetchExamAttempts, fetchProctorSessionAlerts } = await import('../services/examMonitoringService').catch(() => ({
        fetchExamAttempts: null,
        fetchProctorSessionAlerts: null
      }))
      if (fetchExamAttempts && fetchProctorSessionAlerts && examId.value) {
        try {
          const [attempts, alerts] = await Promise.all([
            fetchExamAttempts(examId.value),
            fetchProctorSessionAlerts(examId.value)
          ])
          if (Array.isArray(attempts)) {
            const alertsByAttempt = new Map((alerts || []).map(alert => [String(resolveAttemptId(alert)), alert]))
            store.setCards(attempts.map((attempt) => {
              const alert = alertsByAttempt.get(String(resolveAttemptId(attempt)))
              if (!alert) return attempt
              return {
                ...attempt,
                riskScore: alert.riskScore ?? attempt.riskScore,
                riskLevel: alert.riskLevel ?? attempt.riskLevel,
                reviewRequired: alert.reviewRequired ?? attempt.reviewRequired,
                reasons: alert.reasons?.length ? alert.reasons : attempt.reasons,
                evidenceSummary: alert.evidenceSummary?.length ? alert.evidenceSummary : attempt.evidenceSummary,
                activeFlagId: alert.activeFlagId ?? attempt.activeFlagId,
                activeFlagStatus: alert.activeFlagStatus ?? attempt.activeFlagStatus,
                latestFlagTitle: alert.latestFlagTitle ?? attempt.latestFlagTitle,
                lastSignalAt: alert.updatedAt || attempt.lastSignalAt || attempt.startedAt || null
              }
            }))
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
