/**
 * useToast — Toast notification composable
 * Manages a queue of toast messages with auto-dismiss
 * error toasts don't auto-dismiss — user must close manually
 */
import { ref, readonly } from 'vue'

const DEFAULT_DURATION = 4000
const ERROR_DURATION = 0        // error toasts don't auto-dismiss
const SUCCESS_DURATION = 4000
const WARNING_DURATION = 4000

const toasts = ref([])
let toastIdCounter = 0

export function useToast() {
  function addToast({ message, type = 'info', duration = null, title = '' }) {
    if (!message) return null

    const resolvedDuration = duration !== null
      ? duration
      : (type === 'error' ? ERROR_DURATION
         : type === 'success' ? SUCCESS_DURATION
         : type === 'warning' ? WARNING_DURATION
         : DEFAULT_DURATION)

    const id = ++toastIdCounter
    const toast = { id, message, type, title }
    toasts.value.push(toast)

    if (resolvedDuration > 0) {
      setTimeout(() => removeToast(id), resolvedDuration)
    }
    return id
  }

  function removeToast(id) {
    const idx = toasts.value.findIndex(t => t.id === id)
    if (idx !== -1) toasts.value.splice(idx, 1)
  }

  const success = (message, opts = {}) => addToast({ message, type: 'success', ...opts })
  const error   = (message, opts = {}) => addToast({ message, type: 'error',   ...opts })
  const warning = (message, opts = {}) => addToast({ message, type: 'warning', ...opts })
  const info    = (message, opts = {}) => addToast({ message, type: 'info',    ...opts })

  return {
    toasts: readonly(toasts),
    addToast,
    removeToast,
    success,
    error,
    warning,
    info
  }
}
