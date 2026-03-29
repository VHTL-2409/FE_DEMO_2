import { storeToRefs } from 'pinia'
import { pinia } from '../stores'
import { useAuthStore } from '../stores/authStore'

export const useAuth = () => {
  const authStore = useAuthStore(pinia)
  const {
    token,
    user,
    isBootstrapped,
    isAuthenticated,
    primaryRole,
    dashboardPath
  } = storeToRefs(authStore)
  authStore.syncFromStorage()

  return {
    token,
    user,
    isBootstrapped,
    isAuthenticated,
    primaryRole,
    dashboardPath,
    setAuthData: authStore.setSessionFromPayload,
    logout: authStore.logout,
    syncFromStorage: authStore.syncFromStorage,
    refreshSession: authStore.refreshSession
  }
}
