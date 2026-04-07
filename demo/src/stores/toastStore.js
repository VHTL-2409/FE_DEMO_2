import { ref, computed } from 'vue'
import { defineStore } from 'pinia'

const DEFAULT_DURATION = 4000
const ERROR_DURATION = 3500
const SUCCESS_DURATION = 3500
const INFO_DURATION = 3500
const WARNING_DURATION = 3500
const MAX_TOASTS = 3
let idCounter = 0

export const useToastStore = defineStore('toast', () => {
  const toasts = ref([])

  // Timer registry: toastId -> { timerId, duration, startedAt, remaining }
  const timerRegistry = new Map()

  const hasToasts = computed(() => toasts.value.length > 0)

  const enqueue = (toast) => {
    if (toasts.value.length >= MAX_TOASTS) {
      toasts.value.shift()
    }
    toasts.value.push(toast)
  }

  const dismiss = (id) => {
    const index = toasts.value.findIndex((t) => t.id === id)
    if (index >= 0) {
      toasts.value.splice(index, 1)
    }
    clearRegisteredTimer(id)
  }

  /** Clear a timer registered in the registry */
  const clearRegisteredTimer = (toastId) => {
    const entry = timerRegistry.get(toastId)
    if (entry) {
      clearTimeout(entry.timerId)
      timerRegistry.delete(toastId)
    }
  }

  /**
   * Pause the auto-dismiss timer for a toast.
   * Returns the remaining time so ToastHost can render a progress bar.
   */
  const pauseTimer = (toastId) => {
    const entry = timerRegistry.get(toastId)
    if (!entry) return 0
    clearTimeout(entry.timerId)
    entry.timerId = null
    const elapsed = Date.now() - entry.startedAt
    entry.remaining = Math.max(0, entry.duration - elapsed)
    return entry.remaining
  }

  /**
   * Resume the auto-dismiss timer with the remaining time.
   */
  const resumeTimer = (toastId, remaining) => {
    const entry = timerRegistry.get(toastId)
    if (!entry) return
    entry.startedAt = Date.now()
    entry.timerId = window.setTimeout(() => dismiss(toastId), remaining)
  }

  /**
   * Get current timer info for a toast (duration, elapsed, remaining).
   */
  const getTimerInfo = (toastId) => {
    const entry = timerRegistry.get(toastId)
    if (!entry) return null
    const elapsed = entry.timerId !== null
      ? Date.now() - entry.startedAt
      : (entry.duration - (entry.remaining || 0))
    return {
      duration: entry.duration,
      elapsed,
      remaining: entry.duration - elapsed,
      totalRemaining: entry.remaining ?? entry.duration
    }
  }

  /**
   * Register an externally-managed timer.
   * External components (ToastHost) call this to take over timer management.
   * Pass null to clear the timer without dismissing.
   */
  const registerExternalTimer = (toastId, timerId, duration) => {
    // Clear any existing timer first
    clearRegisteredTimer(toastId)
    if (timerId === null) return
    timerRegistry.set(toastId, {
      timerId,
      duration,
      startedAt: Date.now(),
      remaining: null
    })
  }

  const show = ({ type = 'info', message, title = '', duration = null } = {}) => {
    if (!message) return null

    const resolvedDuration = duration !== null
      ? duration
      : (type === 'error' ? ERROR_DURATION
         : type === 'success' ? SUCCESS_DURATION
         : type === 'warning' ? WARNING_DURATION
         : INFO_DURATION)

    const id = `${Date.now()}-${idCounter++}`
    const toast = { id, type, message, title, _duration: resolvedDuration }
    enqueue(toast)

    if (resolvedDuration > 0) {
      const timerId = window.setTimeout(() => dismiss(id), resolvedDuration)
      timerRegistry.set(id, {
        timerId,
        duration: resolvedDuration,
        startedAt: Date.now(),
        remaining: null
      })
    }

    return id
  }

  const success = (message, options = {}) => show({ ...options, type: 'success', message })
  const error   = (message, options = {}) => show({ ...options, type: 'error',   message })
  const info    = (message, options = {}) => show({ ...options, type: 'info',    message })
  const warning = (message, options = {}) => show({ ...options, type: 'warning', message })
  const clearAll = () => {
    // Clear all registered timers first
    timerRegistry.forEach((entry) => clearTimeout(entry.timerId))
    timerRegistry.clear()
    toasts.value.splice(0, toasts.value.length)
  }

  return {
    toasts,
    hasToasts,
    show,
    success,
    error,
    info,
    warning,
    dismiss,
    clearAll,
    pauseTimer,
    resumeTimer,
    getTimerInfo,
    registerExternalTimer,
    clearRegisteredTimer
  }
})

// Backward compatibility: export singleton functions
let storeInstance = null

const getStore = () => {
  if (!storeInstance) {
    storeInstance = useToastStore()
  }
  return storeInstance
}

export const toastService = {
  get state() {
    return { toasts: getStore().toasts }
  },
  show: (...args) => getStore().show(...args),
  success: (...args) => getStore().success(...args),
  error: (...args) => getStore().error(...args),
  info: (...args) => getStore().info(...args),
  warning: (...args) => getStore().warning(...args),
  dismiss: (...args) => getStore().dismiss(...args),
  clearAll: () => getStore().clearAll()
}
