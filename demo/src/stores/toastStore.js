import { ref, computed } from 'vue'
import { defineStore } from 'pinia'

const DEFAULT_DURATION = 4500
const MAX_TOASTS = 3
let idCounter = 0

export const useToastStore = defineStore('toast', () => {
  const toasts = ref([])

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
  }

  const show = ({ type = 'info', message, title = '', duration = DEFAULT_DURATION } = {}) => {
    if (!message) return null

    const id = `${Date.now()}-${idCounter++}`
    const toast = { id, type, message, title, duration }
    enqueue(toast)

    if (duration > 0) {
      window.setTimeout(() => dismiss(id), duration)
    }

    return id
  }

  const success = (message, options = {}) => show({ ...options, type: 'success', message })
  const error = (message, options = {}) => show({ ...options, type: 'error', message })
  const info = (message, options = {}) => show({ ...options, type: 'info', message })
  const warning = (message, options = {}) => show({ ...options, type: 'warning', message })
  const clearAll = () => {
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
    clearAll
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
