import { apiRequest, unwrapApiData } from './apiClient'

const AUTH_TOKEN_KEY = 'auth_token'
const AUTH_USER_KEY = 'auth_user'

const normalizeAuthUser = (authData) => ({
  username: authData?.username || '',
  roles: Array.isArray(authData?.roles) ? authData.roles : []
})

export const getStoredToken = () => localStorage.getItem(AUTH_TOKEN_KEY)

export const getStoredUser = () => {
  const raw = localStorage.getItem(AUTH_USER_KEY)
  if (!raw) return null

  try {
    return JSON.parse(raw)
  } catch {
    return null
  }
}

export const storeAuthSession = (authData) => {
  if (authData?.token) {
    localStorage.setItem(AUTH_TOKEN_KEY, authData.token)
  }

  localStorage.setItem(AUTH_USER_KEY, JSON.stringify(normalizeAuthUser(authData)))
}

export const clearAuthSession = () => {
  localStorage.removeItem(AUTH_TOKEN_KEY)
  localStorage.removeItem(AUTH_USER_KEY)
}

export const login = async ({ username, password }) => {
  const response = await apiRequest('/api/auth/login', {
    method: 'POST',
    body: JSON.stringify({ username, password })
  })

  const authData = unwrapApiData(response)
  storeAuthSession(authData)
  return authData
}

export const register = async ({ username, email, password }) => {
  const response = await apiRequest('/api/auth/register', {
    method: 'POST',
    body: JSON.stringify({ username, email, password })
  })

  const authData = unwrapApiData(response)
  storeAuthSession(authData)
  return authData
}

export const fetchMyProfile = async () => apiRequest('/api/me')
