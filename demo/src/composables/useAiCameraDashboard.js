import { ref, computed, onMounted, onUnmounted } from 'vue'
import { fetchCameraStatus, fetchCameraAlerts, dismissCameraAlert } from '../services/proctorService'

const RECONNECT_DELAY_MS = 5000
const POLL_INTERVAL_WHEN_DISCONNECTED_MS = 8000

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
        cameraStatuses.value = Array.isArray(statusResult.value)
          ? statusResult.value
          : (statusResult.value.students || [])
      }

      if (alertsResult.status === 'fulfilled' && alertsResult.value) {
        recentAlerts.value = Array.isArray(alertsResult.value)
          ? alertsResult.value
          : (alertsResult.value.alerts || [])
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
    const signal = event.latestSignal || {}

    // Find existing camera status or create new one
    const existingIndex = cameraStatuses.value.findIndex(c => c.attemptId === attemptId)
    const camera = existingIndex >= 0 ? cameraStatuses.value[existingIndex] : null

    if (type === 'AI_CAMERA_SIGNAL' || type === 'FRAUD_SIGNAL_RECORDED') {
      // Update existing or add new
      const signalType = signal.signalType

      if (camera) {
        // Update existing camera status
        if (!camera.activeSignals) camera.activeSignals = []
        if (!camera.criticalSignals) camera.criticalSignals = []
        camera.cameraActive = true

        if (!camera.activeSignals.includes(signalType)) {
          camera.activeSignals.push(signalType)
        }

        // Update risk score if provided
        if (event.riskScore != null) {
          camera.riskScore = event.riskScore
        }

        // Update status based on severity
        if (signal.severity === 'CRITICAL' || signal.severity === 'HIGH') {
          camera.status = signal.severity === 'CRITICAL' ? 'CRITICAL' : 'WARNING'
          camera.alertCount = (camera.alertCount || 0) + 1
          camera.criticalSignals.push({
            signalType,
            severity: signal.severity,
            confidence: signal.confidence,
            occurredAt: signal.occurredAt || event.issuedAt
          })
        }

        camera.lastUpdate = new Date().toISOString()
        cameraStatuses.value[existingIndex] = { ...camera }
      } else {
        // Add new camera status
        const newCamera = {
          attemptId,
          studentName: event.student || event.studentName || 'Unknown',
          cameraActive: true,
          status: signal.severity === 'CRITICAL' ? 'CRITICAL' :
                 signal.severity === 'HIGH' ? 'WARNING' : 'OK',
          riskScore: event.riskScore || 0,
          alertCount: (signal.severity === 'CRITICAL' || signal.severity === 'HIGH') ? 1 : 0,
          activeSignals: [signalType],
          criticalSignals: (signal.severity === 'CRITICAL' || signal.severity === 'HIGH') ? [{
            signalType,
            severity: signal.severity,
            confidence: signal.confidence,
            occurredAt: signal.occurredAt || event.issuedAt
          }] : [],
          lastUpdate: new Date().toISOString()
        }
        cameraStatuses.value.push(newCamera)
      }

      // Add to alerts
      if (signal.severity === 'CRITICAL' || signal.severity === 'HIGH') {
        const newAlert = {
          id: signal.id || Date.now(),
          attemptId,
          studentName: event.student || event.studentName,
          signalType,
          severity: signal.severity,
          confidence: signal.confidence,
          createdAt: signal.occurredAt || event.issuedAt || new Date().toISOString()
        }
        // Add to beginning, keep only last 50
        recentAlerts.value = [newAlert, ...recentAlerts.value].slice(0, 50)
      }
    } else if (type === 'RISK_UPDATED' && attemptId) {
      // Update risk score only
      if (camera) {
        camera.riskScore = event.riskScore
        camera.lastUpdate = new Date().toISOString()
        cameraStatuses.value[existingIndex] = { ...camera }
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
