import { inject } from 'vue'
import { toastService } from '../services/toastService'

export const useToast = () => {
  const injected = inject('toast', null)
  return injected || toastService
}
