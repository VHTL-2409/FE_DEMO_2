import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import {
  clearAuthSession,
  getDashboardPathForUser,
  getStoredToken,
  getStoredUser,
  syncCurrentUserFromDatabase,
  validateSession
} from '../services/authService'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(getStoredToken())
  const user = ref(getStoredUser())
  const isBootstrapped = ref(false)

  const isAuthenticated = computed(() => Boolean(token.value))
  const primaryRole = computed(() => (user.value?.roles || [])[0] || null)
  const dashboardPath = computed(() => getDashboardPathForUser(user.value))

  const syncFromStorage = () => {
    token.value = getStoredToken()
    user.value = getStoredUser()
    isBootstrapped.value = true
  }

  const setSessionFromPayload = (authData) => {
    token.value = authData?.token || getStoredToken()
    user.value = {
      username: authData?.username || '',
      roles: Array.isArray(authData?.roles) ? authData.roles : []
    }
    isBootstrapped.value = true
  }

  const refreshSession = async ({ force = false } = {}) => {
    token.value = getStoredToken()
    if (!token.value) {
      user.value = null
      isBootstrapped.value = true
      return null
    }

    const currentUser = force ? await syncCurrentUserFromDatabase() : await validateSession()
    token.value = getStoredToken()
    user.value = currentUser
    isBootstrapped.value = true
    return currentUser
  }

  const logout = () => {
    clearAuthSession()
    token.value = null
    user.value = null
    isBootstrapped.value = true
  }

  return {
    token,
    user,
    isBootstrapped,
    isAuthenticated,
    primaryRole,
    dashboardPath,
    syncFromStorage,
    setSessionFromPayload,
    refreshSession,
    logout
  }
})
