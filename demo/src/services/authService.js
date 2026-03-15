import { apiRequest, ApiError, unwrapApiData } from './apiClient'

const AUTH_TOKEN_KEY = 'auth_token'
const AUTH_USER_KEY = 'auth_user'
const AUTH_USER_TS_KEY = 'auth_user_ts'
const USER_CACHE_TTL_MS = 5 * 60 * 1000

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

const getStoredUserTimestamp = () => {
  const raw = localStorage.getItem(AUTH_USER_TS_KEY)
  const parsed = raw ? Number.parseInt(raw, 10) : 0
  return Number.isNaN(parsed) ? 0 : parsed
}

const setStoredUserTimestamp = () => {
  localStorage.setItem(AUTH_USER_TS_KEY, String(Date.now()))
}

const isUserCacheFresh = () => {
  const ts = getStoredUserTimestamp()
  if (!ts) return false
  return Date.now() - ts < USER_CACHE_TTL_MS
}

export const storeAuthSession = (authData) => {
  if (authData?.token) {
    localStorage.setItem(AUTH_TOKEN_KEY, authData.token)
  }

  const normalizedUser = normalizeAuthUser(authData)
  localStorage.setItem(AUTH_USER_KEY, JSON.stringify(normalizedUser))
  setStoredUserTimestamp()
}

export const clearAuthSession = () => {
  localStorage.removeItem(AUTH_TOKEN_KEY)
  localStorage.removeItem(AUTH_USER_KEY)
  localStorage.removeItem(AUTH_USER_TS_KEY)
}

export const invalidateSession = () => {
  clearAuthSession()
  if (window.location.pathname !== '/login') {
    window.location.href = '/login'
  }
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

export const validateSession = async () => {
  const token = getStoredToken()
  if (!token) return null

  const cachedUser = getStoredUser()
  if (cachedUser && isUserCacheFresh()) {
    if (!cachedUser.username) {
      clearAuthSession()
      return null
    }
    return cachedUser
  }

  try {
    const data = await apiRequest('/api/me')
    const user = {
      username: data?.username || '',
      roles: Array.isArray(data?.roles) ? data.roles : []
    }
    localStorage.setItem(AUTH_USER_KEY, JSON.stringify(user))
    setStoredUserTimestamp()
    return user
  } catch (error) {
    if (error instanceof ApiError) {
      if (error.status === 404 && String(error?.payload?.message || '').includes('Student profile not found')) {
        return getStoredUser()
      }
    }
    clearAuthSession()
    return null
  }
}

export const fetchStudentProfile = async () => apiRequest('/api/profile/student')

export const fetchTeacherProfile = async () => apiRequest('/api/profile/teacher')

export const updateSharedProfile = async ({ displayName, fullName, dateOfBirth, email, phone, avatarUrl }) => {
  const response = await apiRequest('/api/profile', {
    method: 'PUT',
    body: JSON.stringify({ displayName, fullName, dateOfBirth, email, phone, avatarUrl })
  })
  return unwrapApiData(response)
}

export const uploadAvatar = async (file) => {
  const formData = new FormData()
  formData.append('file', file)
  const response = await apiRequest('/api/profile/avatar', {
    method: 'POST',
    body: formData
  })
  return unwrapApiData(response)
}

export const changePassword = async ({ currentPassword, newPassword }) => {
  const response = await apiRequest('/api/auth/change-password', {
    method: 'POST',
    body: JSON.stringify({ currentPassword, newPassword })
  })
  return unwrapApiData(response)
}
