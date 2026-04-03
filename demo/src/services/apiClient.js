import { toastService } from './toastService'
import { invalidateSession } from './authService'

const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL || 'http://localhost:8082').replace(/\/$/, '')

const getAuthToken = () => localStorage.getItem('auth_token')
const REFRESH_TOKEN_KEY = 'auth_refresh_token'
const REFRESH_BUFFER_MS = 5 * 60 * 1000
let _refreshPromise = null

const parseJsonSafe = async (response) => {
  const text = await response.text()
  if (!text) return null

  try {
    return JSON.parse(text)
  } catch {
    return null
  }
}

const resolveErrorMessage = (payload, fallbackMessage) => {
  if (!payload) return fallbackMessage
  // Prefer user-friendly message field from API
  if (typeof payload.message === 'string' && payload.message.trim()) return payload.message
  if (typeof payload.error === 'string' && payload.error.trim()) return payload.error
  if (typeof payload.userMessage === 'string' && payload.userMessage.trim()) return payload.userMessage
  return fallbackMessage
}

export class ApiError extends Error {
  constructor(message, status, payload = null) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.payload = payload
  }
}

const isTokenExpiringSoon = () => {
  const raw = localStorage.getItem('auth_token')
  if (!raw) return false
  try {
    const payload = JSON.parse(atob(raw.split('.')[1]))
    return (payload.exp * 1000) - Date.now() < REFRESH_BUFFER_MS
  } catch {
    return false
  }
}

const silentRefresh = async () => {
  if (_refreshPromise) return _refreshPromise

  const refreshToken = localStorage.getItem(REFRESH_TOKEN_KEY)
  if (!refreshToken) return null

  _refreshPromise = (async () => {
    try {
      const response = await fetch(`${API_BASE_URL}/api/auth/refresh`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ refreshToken })
      })
      if (!response.ok) {
        localStorage.removeItem('auth_token')
        localStorage.removeItem('auth_refresh_token')
        return null
      }
      const payload = await parseJsonSafe(response)
      const data = payload?.data || payload
      if (data?.token) {
        localStorage.setItem('auth_token', data.token)
      }
      if (data?.refreshToken) {
        localStorage.setItem(REFRESH_TOKEN_KEY, data.refreshToken)
      }
      return data
    } catch {
      return null
    } finally {
      _refreshPromise = null
    }
  })()

  return _refreshPromise
}

export const apiRequest = async (path, options = {}) => {
  // Silently refresh token if it's expiring soon (before making the request)
  if (isTokenExpiringSoon()) {
    await silentRefresh()
  }

  const token = getAuthToken()
  const method = options.method || 'GET'
  const headers = {
    ...(options.body instanceof FormData ? {} : { 'Content-Type': 'application/json' }),
    ...(options.headers || {})
  }

  if (token) {
    headers.Authorization = `Bearer ${token}`
  }

  let response
  try {
    response = await fetch(`${API_BASE_URL}${path}`, {
      ...options,
      method,
      headers
    })
  } catch (networkError) {
    toastService.error('Không thể kết nối đến máy chủ. Vui lòng kiểm tra kết nối mạng.')
    throw new ApiError('Network error', 0, null)
  }

  const payload = await parseJsonSafe(response)

  if (response.ok) return payload

  if (response.status === 401) {
    const refreshed = await silentRefresh()
    if (refreshed) {
      const newToken = localStorage.getItem('auth_token')
      headers.Authorization = `Bearer ${newToken}`
      try {
        const retryResponse = await fetch(`${API_BASE_URL}${path}`, { ...options, method, headers })
        const retryPayload = await parseJsonSafe(retryResponse)
        if (retryResponse.ok) return retryPayload
        response = retryResponse
        payload = retryPayload
      } catch (err) {
        invalidateSession()
        throw new ApiError('Session expired', 401, null)
      }
    }
    invalidateSession()
  }

  const errorMessage = resolveErrorMessage(payload, getDefaultErrorMessage(response.status))
  if (response.status !== 401) toastService.error(errorMessage)
  throw new ApiError(errorMessage, response.status, payload)
}

const getDefaultErrorMessage = (status) => {
  switch (status) {
    case 400: return 'Yêu cầu không hợp lệ. Vui lòng kiểm tra lại thông tin.'
    case 403: return 'Bạn không có quyền thực hiện thao tác này.'
    case 404: return 'Không tìm thấy dữ liệu yêu cầu.'
    case 409: return 'Dữ liệu bị xung đột. Vui lòng làm mới trang.'
    case 422: return 'Dữ liệu không hợp lệ. Vui lòng kiểm tra lại.'
    case 429: return 'Quá nhiều yêu cầu. Vui lòng thử lại sau.'
    case 500: return 'Lỗi máy chủ. Vui lòng thử lại sau.'
    case 502: case 503: case 504: return 'Máy chủ đang bận. Vui lòng thử lại sau.'
    default: return 'Đã xảy ra lỗi. Vui lòng thử lại.'
  }
}

export const unwrapApiData = (payload) => {
  if (payload && typeof payload === 'object' && 'data' in payload) {
    return payload.data
  }
  return payload
}

export { API_BASE_URL }
