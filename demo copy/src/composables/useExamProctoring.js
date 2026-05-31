import { onMounted, onUnmounted, ref } from 'vue'
import { fetchProctorRisk, startProctoringSession } from '../services/proctorService'

const currentViewport = () => ({
  screenWidth: window.screen?.width || null,
  screenHeight: window.screen?.height || null,
  visibility: document.visibilityState || 'visible',
  fullscreen: Boolean(document.fullscreenElement)
})

export function useExamProctoring({
  attemptId,
  sessionId,
  getDeviceFingerprint,
  getCameraState,
  getMicState,
  autoStart = true,
  onRiskUpdate
}) {
  const started = ref(false)
  const busy = ref(false)
  const riskState = ref(null)

  const syncRisk = (payload) => {
    if (!payload) return
    const normalized = {
      ...(riskState.value || {}),
      ...payload,
      score: payload.score ?? payload.riskScore ?? payload.initialRiskScore ?? riskState.value?.score ?? 0,
      level: payload.level ?? payload.riskLevel ?? riskState.value?.level ?? 'LOW',
      activeFlagId: payload.activeFlagId ?? riskState.value?.activeFlagId ?? null,
      activeFlagStatus: payload.activeFlagStatus ?? riskState.value?.activeFlagStatus ?? null
    }
    riskState.value = normalized
    onRiskUpdate?.(normalized)
  }

  const start = async () => {
    if (!attemptId || !sessionId || busy.value) return null
    busy.value = true
    try {
      const payload = await startProctoringSession({
        attemptId,
        sessionId,
        deviceFingerprint: getDeviceFingerprint?.() || '',
        userAgent: navigator.userAgent || '',
        timezone: Intl.DateTimeFormat().resolvedOptions().timeZone || '',
        cameraOn: getCameraState?.() ?? null,
        micOn: getMicState?.() ?? null,
        ...currentViewport()
      })
      started.value = true
      syncRisk(payload)
      return payload
    } finally {
      busy.value = false
    }
  }

  const refreshRisk = async () => {
    if (!attemptId) return null
    const payload = await fetchProctorRisk(attemptId)
    syncRisk(payload)
    return payload
  }

  onMounted(() => {
    if (autoStart) {
      void start()
    }
  })

  onUnmounted(() => {
    // cleanup if needed
  })

  return {
    started,
    busy,
    riskState,
    start,
    refreshRisk
  }
}
