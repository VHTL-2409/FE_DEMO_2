import { computed, ref } from 'vue'
import { clearAuthSession, getStoredToken, getStoredUser } from '../services/authService'

const token = ref(getStoredToken())
const user = ref(getStoredUser())

const syncFromStorage = () => {
  token.value = getStoredToken()
  user.value = getStoredUser()
}

const setAuthData = (authData) => {
  token.value = authData?.token || getStoredToken()
  user.value = {
    username: authData?.username || '',
    roles: Array.isArray(authData?.roles) ? authData.roles : []
  }
}

const logout = () => {
  clearAuthSession()
  token.value = null
  user.value = null
}

export const useAuth = () => {
  syncFromStorage()

  const isAuthenticated = computed(() => Boolean(token.value))
  const primaryRole = computed(() => (user.value?.roles || [])[0] || null)

  return {
    token,
    user,
    isAuthenticated,
    primaryRole,
    setAuthData,
    logout,
    syncFromStorage
  }
}
