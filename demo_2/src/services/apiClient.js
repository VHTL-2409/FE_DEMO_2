const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL || 'http://localhost:8082').replace(/\/$/, '')

const getAuthToken = () => localStorage.getItem('auth_token')

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
  if (typeof payload.message === 'string' && payload.message.trim()) return payload.message
  if (typeof payload.error === 'string' && payload.error.trim()) return payload.error
  return fallbackMessage
}

const invalidateSession = () => {
  import('./authService')
    .then(({ invalidateSession: invalidate }) => {
      if (typeof invalidate === 'function') {
        invalidate()
      } else {
        localStorage.removeItem('auth_token')
        localStorage.removeItem('auth_user')
      }
    })
    .catch(() => {
      localStorage.removeItem('auth_token')
      localStorage.removeItem('auth_user')
    })
}

export class ApiError extends Error {
  constructor(message, status, payload = null) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.payload = payload
  }
}

export const apiRequest = async (path, options = {}) => {
  const token = getAuthToken()
  const method = options.method || 'GET'
  const headers = {
    ...(options.body instanceof FormData ? {} : { 'Content-Type': 'application/json' }),
    ...(options.headers || {})
  }

  if (token) {
    headers.Authorization = `Bearer ${token}`
  }

  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    method,
    headers
  })

  const payload = await parseJsonSafe(response)

  if (!response.ok) {
    if (response.status === 401) {
      invalidateSession()
    }

    const errorMessage = resolveErrorMessage(payload, 'Request failed')
    if (response.status !== 401) {
      import('./toastService').then(({ toastService }) => {
        toastService.error(errorMessage)
      })
    }
    throw new ApiError(errorMessage, response.status, payload)
  }

  return payload
}

export const unwrapApiData = (payload) => {
  if (payload && typeof payload === 'object' && 'data' in payload) {
    return payload.data
  }
  return payload
}

/** Ảnh/file tĩnh trên BE (vd: /avatars/...) — ghép với API gốc khi chạy FE cổng khác */
export const resolveBackendUrl = (path) => {
  if (path == null || path === '') return ''
  const s = String(path).trim()
  if (!s || s === '-') return ''
  if (/^https?:\/\//i.test(s)) return s
  const base = API_BASE_URL.replace(/\/$/, '')
  return `${base}${s.startsWith('/') ? '' : '/'}${s}`
}

export { API_BASE_URL }
