import { reactive } from 'vue'

const DEFAULT_DURATION = 4000
const ERROR_DURATION = 0        // error toasts don't auto-dismiss — user must close manually
const SUCCESS_DURATION = 4000
const WARNING_DURATION = 4000
const MAX_TOASTS = 3
let idCounter = 0

const state = reactive({
  toasts: []
})

const enqueue = (toast) => {
  if (state.toasts.length >= MAX_TOASTS) {
    state.toasts.shift()
  }
  state.toasts.push(toast)
}

const dismiss = (id) => {
  const index = state.toasts.findIndex((toast) => toast.id === id)
  if (index >= 0) {
    state.toasts.splice(index, 1)
  }
}

const show = ({ type = 'info', message, title = '', duration = null } = {}) => {
  if (!message) return null

  const resolvedDuration = duration !== null
    ? duration
    : (type === 'error' ? ERROR_DURATION
       : type === 'success' ? SUCCESS_DURATION
       : type === 'warning' ? WARNING_DURATION
       : DEFAULT_DURATION)

  const id = `${Date.now()}-${idCounter++}`
  const toast = { id, type, message, title }
  enqueue(toast)

  if (resolvedDuration > 0) {
    window.setTimeout(() => dismiss(id), resolvedDuration)
  }

  return id
}

const success = (message, options = {}) => show({ ...options, type: 'success', message })
const error   = (message, options = {}) => show({ ...options, type: 'error',   message })
const info    = (message, options = {}) => show({ ...options, type: 'info',    message })
const warning = (message, options = {}) => show({ ...options, type: 'warning', message })
const clearAll = () => {
  state.toasts.splice(0, state.toasts.length)
}

export const toastService = {
  state,
  show,
  success,
  error,
  info,
  warning,
  dismiss,
  clearAll
}
