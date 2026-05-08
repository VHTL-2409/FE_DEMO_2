import { ref, computed, onMounted, onUnmounted } from 'vue'
import { fetchCameraStatus, fetchCameraAlerts, dismissCameraAlert } from '../services/proctorService'
import { normalizeSignalType, uniqueSignalTypes } from '../utils/proctorSignalTypes'

const RECONNECT_DELAY_MS = 5000
const POLL_INTERVAL_WHEN_DISCONNECTED_MS = 8000

function parseMaybeJson(value) {
  if (!value) return {}
  if (typeof value === 'object') return value
  try {
    const parsed = JSON.parse(value)
    return parsed && typeof parsed === 'object' ? parsed : {}
  } catch {
    return {}
  }
}

function firstDefined(...values) {
  return values.find(value => value !== undefined && value !== null)
}

function uniqueSignalEntries(entries = []) {
  const seen = new Set()
  const unique = []
  for (const entry of entries) {
    const type = normalizeSignalType(entry?.signalType || entry?.signal_type)
    if (!type || seen.has(type)) continue
    seen.add(type)
    unique.push({ ...entry, signalType: type })
  }
  return unique
}

function upsertSignalEntry(entries = [], entry, limit = 10) {
  const type = normalizeSignalType(entry?.signalType || entry?.signal_type)
  if (!type) return uniqueSignalEntries(entries).slice(0, limit)
  const normalized = { ...entry, signalType: type }
  const next = uniqueSignalEntries(entries).filter(item =>
    normalizeSignalType(item.signalType || item.signal_type) !== type
  )
  return [normalized, ...next].slice(0, limit)
}

function cameraAlertKey(alert = {}) {
  const attemptId = firstDefined(alert.attemptId, alert.attempt_id)
  const signalType = normalizeSignalType(firstDefined(alert.signalType, alert.signal_type, alert.warningType))
  if (attemptId != null && signalType) return `${attemptId}:${signalType}`
  return alert.id != null ? `id:${alert.id}` : ''
}

function dedupeRecentAlerts(alerts = []) {
  const seen = new Set()
  const unique = []
  for (const alert of alerts) {
    const key = cameraAlertKey(alert)
    if (key && seen.has(key)) continue
    if (key) seen.add(key)
    unique.push(alert)
  }
  return unique
}

function upsertRecentAlert(alerts = [], alert) {
  const key = cameraAlertKey(alert)
  const remaining = key
    ? alerts.filter(existing => cameraAlertKey(existing) !== key)
    : alerts
  return [alert, ...remaining].slice(0, 50)
}

function normalizeCameraStatus(camera = {}) {
  const activeSignals = uniqueSignalTypes(camera.activeSignals || [])
  const criticalSignals = uniqueSignalEntries(camera.criticalSignals || [])
  return {
    ...camera,
    activeSignals,
    criticalSignals,
    alertCount: criticalSignals.length || Number(camera.alertCount || 0)
  }
}

function extractAiCameraMetrics(signal = {}, event = {}) {
  const evidence = parseMaybeJson(signal.evidence || event.evidence)
  const signalEvidence = parseMaybeJson(evidence.signalEvidence)
  const source = { ...evidence, ...signalEvidence, ...signal, ...event }
  const patch = {
    riskImpact: firstDefined(source.riskImpact, source.risk_impact),
    eyeCount: firstDefined(source.eyeCount, source.eye_count),
    eyeState: firstDefined(source.eyeState, source.eye_state),
    eyeAspectRatio: firstDefined(source.eyeAspectRatio, source.eye_aspect_ratio),
    eyeTrackingConfidence: firstDefined(source.eyeTrackingConfidence, source.eye_tracking_confidence),
    closureDurationMs: firstDefined(source.closureDurationMs, source.closure_duration_ms),
    gazeDirection: firstDefined(source.gazeDirection, source.gaze_direction),
    gazeOffScreen: firstDefined(source.gazeOffScreen, source.gaze_off_screen),
    gazeConfidence: firstDefined(source.gazeConfidence, source.gaze_confidence),
    offScreenDurationMs: firstDefined(source.offScreenDurationMs, source.off_screen_duration_ms),
    attentionScore: firstDefined(source.attentionScore, source.attention_score),
    visualOverlay: firstDefined(source.visualOverlay, source.visual_overlay),
    faceDetected: firstDefined(source.faceDetected, source.face_detected),
    faceCount: firstDefined(source.faceCount, source.face_count),
    multipleFaces: firstDefined(source.multipleFaces, source.multiple_faces),
    faceQuality: firstDefined(source.faceQuality, source.face_quality),
    frameQuality: firstDefined(source.frameQuality, source.frame_quality),
    averageBrightness: firstDefined(source.averageBrightness, source.average_brightness)
  }
  return Object.fromEntries(Object.entries(patch).filter(([, value]) => value !== undefined))
}

export function useAiCameraDashboard(examId) {
  const loading = ref(false)
  const cameraStatuses = ref([])
  const recentAlerts = ref([])
  const isConnected = ref(false)
  const connectionMode = ref('polling')

  let stompClient = null
  let pollIntervalId = null
  let stompTimeoutId = null

  // Computed stats
  const totalStudents = computed(() => cameraStatuses.value.length)
  const okCount = computed(() => cameraStatuses.value.filter(c => c.status === 'OK').length)
  const warningCount = computed(() => cameraStatuses.value.filter(c => c.status === 'WARNING').length)
  const criticalCount = computed(() => cameraStatuses.value.filter(c =>
    c.status === 'CRITICAL' || c.status === 'NO_CAMERA').length)

  // Load data from API
  async function loadData() {
    if (!examId.value) return

    loading.value = true
    try {
      const [statusResult, alertsResult] = await Promise.allSettled([
        fetchCameraStatus(examId.value),
        fetchCameraAlerts(examId.value)
      ])

      if (statusResult.status === 'fulfilled' && statusResult.value) {
        const students = Array.isArray(statusResult.value)
          ? statusResult.value
          : (statusResult.value.students || [])
        cameraStatuses.value = students.map(normalizeCameraStatus)
      }

      if (alertsResult.status === 'fulfilled' && alertsResult.value) {
        const alerts = Array.isArray(alertsResult.value)
          ? alertsResult.value
          : (alertsResult.value.alerts || [])
        recentAlerts.value = dedupeRecentAlerts(alerts)
      }
    } catch (err) {
      console.error('Failed to load camera data:', err)
    } finally {
      loading.value = false
    }
  }

  // Apply realtime event to camera statuses
  function applyRealtimeEvent(event) {
    if (!event) return

    const attemptId = event.attemptId
    if (!attemptId) return

    const type = String(event.type || '').toUpperCase()
    const signal = event.latestSignal || (
      type === 'FRAUD_WARNING_RECORDED' && event.warningCategory === 'CAMERA_PROCTORING'
        ? {
            signalType: event.warningType,
            severity: event.severity,
            confidence: event.confidence,
            riskImpact: event.riskImpact,
            occurredAt: event.issuedAt
          }
        : {}
    )

    // Find existing camera status or create new one
    const existingIndex = cameraStatuses.value.findIndex(c => c.attemptId === attemptId)
    const camera = existingIndex >= 0 ? cameraStatuses.value[existingIndex] : null

    if (type === 'AI_CAMERA_SIGNAL' || type === 'FRAUD_SIGNAL_RECORDED' || (type === 'FRAUD_WARNING_RECORDED' && event.warningCategory === 'CAMERA_PROCTORING')) {
      // Update existing or add new
      const signalType = normalizeSignalType(firstDefined(signal.signalType, signal.signal_type, event.warningType))
      const severity = normalizeSignalType(signal.severity)
      const alertSeverity = severity === 'CRITICAL' || severity === 'HIGH'
      const aiMetrics = extractAiCameraMetrics(signal, event)
      const criticalEntry = {
        signalType,
        severity,
        confidence: signal.confidence,
        riskImpact: signal.riskImpact ?? event.riskImpact,
        occurredAt: signal.occurredAt || event.issuedAt
      }

      if (camera) {
        // Update existing camera status
        camera.activeSignals = uniqueSignalTypes(camera.activeSignals || [])
        camera.criticalSignals = uniqueSignalEntries(camera.criticalSignals || [])
        camera.cameraActive = true
        Object.assign(camera, aiMetrics)

        if (signalType) {
          camera.activeSignals = uniqueSignalTypes([...camera.activeSignals, signalType])
        }

        // Update risk score if provided
        if (event.riskScore != null) {
          camera.riskScore = event.riskScore
        }

        // Update status based on severity
        if (alertSeverity && signalType) {
          camera.status = severity === 'CRITICAL' ? 'CRITICAL' : 'WARNING'
          camera.criticalSignals = upsertSignalEntry(camera.criticalSignals, criticalEntry)
          camera.alertCount = camera.criticalSignals.length
        }

        camera.lastUpdate = new Date().toISOString()
        cameraStatuses.value[existingIndex] = normalizeCameraStatus(camera)
      } else {
        // Add new camera status
        const criticalSignals = alertSeverity && signalType ? [criticalEntry] : []
        const newCamera = {
          attemptId,
          studentName: event.student || event.studentName || 'Unknown',
          cameraActive: true,
          status: severity === 'CRITICAL' ? 'CRITICAL' :
                 severity === 'HIGH' ? 'WARNING' : 'OK',
          riskScore: event.riskScore ?? 0,
          alertCount: criticalSignals.length,
          activeSignals: signalType ? [signalType] : [],
          ...aiMetrics,
          criticalSignals,
          lastUpdate: new Date().toISOString()
        }
        cameraStatuses.value.push(normalizeCameraStatus(newCamera))
      }

      // Add to alerts
      if (alertSeverity && signalType) {
        const newAlert = {
          id: signal.id || event.id || Date.now(),
          attemptId,
          studentName: event.student || event.studentName,
          signalType,
          severity,
          confidence: signal.confidence,
          riskImpact: signal.riskImpact ?? event.riskImpact,
          createdAt: signal.occurredAt || event.issuedAt || new Date().toISOString()
        }
        recentAlerts.value = upsertRecentAlert(recentAlerts.value, newAlert)
      }
    } else if (type === 'RISK_UPDATED' && attemptId) {
      // Update risk score only
      if (camera) {
        camera.riskScore = event.riskScore
        camera.lastUpdate = new Date().toISOString()
        cameraStatuses.value[existingIndex] = normalizeCameraStatus(camera)
      }
    }
  }

  // STOMP/WebSocket connection
  function connectWebSocket() {
    if (typeof window === 'undefined' || !window.Stomp) {
      console.log('[AiCamera] STOMP not available, using polling')
      connectionMode.value = 'polling'
      startPolling(POLL_INTERVAL_WHEN_DISCONNECTED_MS)
      return
    }

    const wsUrl = import.meta.env.VITE_WS_URL || 'ws://localhost:8080/ws'
    try {
      stompClient = window.Stomp.client(wsUrl)

      stompClient.connect({}, () => {
        console.log('[AiCamera] WebSocket connected')
        isConnected.value = true
        connectionMode.value = 'websocket'
        stopPolling()

        // Subscribe to AI Camera updates topic
        const topic = `/topic/exams/${examId.value}/camera-updates`
        stompClient.subscribe(topic, (message) => {
          try {
            const payload = JSON.parse(message.body)
            applyRealtimeEvent(payload)
          } catch (err) {
            console.error('[AiCamera] Failed to parse message:', err)
          }
        })
      }, (error) => {
        console.error('[AiCamera] WebSocket error:', error)
        isConnected.value = false
        connectionMode.value = 'polling'
        startPolling(POLL_INTERVAL_WHEN_DISCONNECTED_MS)

        // Retry connection
        stompTimeoutId = setTimeout(() => {
          connectWebSocket()
        }, RECONNECT_DELAY_MS)
      })
    } catch (err) {
      console.error('[AiCamera] Failed to connect WebSocket:', err)
      connectionMode.value = 'polling'
      startPolling(POLL_INTERVAL_WHEN_DISCONNECTED_MS)
    }
  }

  function disconnectWebSocket() {
    if (stompClient) {
      try {
        stompClient.disconnect()
      } catch (err) {
        console.error('[AiCamera] Error disconnecting:', err)
      }
      stompClient = null
    }
    isConnected.value = false
    stopPolling()
  }

  // Polling fallback
  function startPolling(intervalMs) {
    if (pollIntervalId) return
    pollIntervalId = setInterval(() => {
      void loadData()
    }, intervalMs || POLL_INTERVAL_WHEN_DISCONNECTED_MS)
  }

  function stopPolling() {
    if (pollIntervalId) {
      clearInterval(pollIntervalId)
      pollIntervalId = null
    }
  }

  // Refresh data
  function refreshData() {
    void loadData()
  }

  // Dismiss alert
  async function dismissAlert(alertId) {
    try {
      await dismissCameraAlert(alertId)
      recentAlerts.value = recentAlerts.value.filter(a => a.id !== alertId)
      return true
    } catch {
      return false
    }
  }

  // Initialize
  onMounted(async () => {
    await loadData()
    // Try WebSocket first, fallback to polling
    connectWebSocket()

    // Timeout fallback to polling if WebSocket doesn't connect
    stompTimeoutId = setTimeout(() => {
      if (!isConnected.value) {
        console.log('[AiCamera] WebSocket connection timeout, using polling')
        connectionMode.value = 'polling'
        startPolling(POLL_INTERVAL_WHEN_DISCONNECTED_MS)
      }
    }, 3000)
  })

  onUnmounted(() => {
    clearTimeout(stompTimeoutId)
    disconnectWebSocket()
  })

  return {
    // State
    loading,
    cameraStatuses,
    recentAlerts,
    isConnected,
    connectionMode,

    // Computed
    totalStudents,
    okCount,
    warningCount,
    criticalCount,

    // Methods
    loadData,
    refreshData,
    dismissAlert,
    applyRealtimeEvent
  }
}
