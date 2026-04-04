import { useToastStore } from '../stores/toastStore'

let storeInstance = null

const getStore = () => {
  if (!storeInstance) {
    storeInstance = useToastStore()
  }
  return storeInstance
}

const DEFAULT_DURATION = 4000
const ERROR_DURATION = 0
const SUCCESS_DURATION = 4000
const WARNING_DURATION = 4000

const show = ({ type = 'info', message, title = '', duration = null } = {}) => {
  if (!message) return null

  const resolvedDuration = duration !== null
    ? duration
    : (type === 'error' ? ERROR_DURATION
       : type === 'success' ? SUCCESS_DURATION
       : type === 'warning' ? WARNING_DURATION
       : DEFAULT_DURATION)

  const id = `${Date.now()}-${Math.random().toString(36).slice(2)}`
  const toast = { id, type, message, title }
  getStore().show({ type, message, title, duration: resolvedDuration })
  return id
}

const success = (message, options = {}) => show({ ...options, type: 'success', message })
const error   = (message, options = {}) => show({ ...options, type: 'error',   message })
const info    = (message, options = {}) => show({ ...options, type: 'info',    message })
const warning = (message, options = {}) => show({ ...options, type: 'warning', message })

export const toastService = {
  show,
  success,
  error,
  info,
  warning,
  dismiss: (id) => getStore().dismiss(id),
  clearAll: () => getStore().clearAll()
}
