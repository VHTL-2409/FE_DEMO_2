import { useToastStore } from '../stores/toastStore'

export const useToast = () => {
  const store = useToastStore()
  return {
    show: (...args) => store.show(...args),
    success: (...args) => store.success(...args),
    error: (...args) => store.error(...args),
    info: (...args) => store.info(...args),
    warning: (...args) => store.warning(...args),
    dismiss: (...args) => store.dismiss(...args),
    clearAll: () => store.clearAll()
  }
}
