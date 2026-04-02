/**
 * useToast — Toast notification composable
 * Manages a queue of toast messages with auto-dismiss
 */
import { ref, readonly } from 'vue'

const toasts = ref([])
let toastIdCounter = 0

export function useToast() {
  function addToast({ message, type = 'info', duration = 4500, title = '' }) {
    const id = ++toastIdCounter
    const toast = { id, message, type, title, duration }
    toasts.value.push(toast)

    if (duration > 0) {
      setTimeout(() => removeToast(id), duration)
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
