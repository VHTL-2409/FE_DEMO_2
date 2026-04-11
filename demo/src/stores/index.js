import { createPinia } from 'pinia'

export const pinia = createPinia()

// Export all stores for convenient importing
export { useAuthStore } from './authStore'
export { useExamSessionStore } from './examSessionStore'
export { useProctorDashboardStore } from './proctorDashboardStore'
export { useImportJobStore } from './importJobStore'
export { useToastStore } from './toastStore'
