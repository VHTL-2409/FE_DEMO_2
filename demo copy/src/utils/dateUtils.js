/**
 * Parse date string từ backend thành Date object.
 * Backend gửi ISO string có hoặc không có timezone offset.
 * Nếu không có offset → coi là Asia/Ho_Chi_Minh (+07:00).
 */
export function parseBackendDate(value) {
  if (!value) return null
  if (value instanceof Date) return isNaN(value.getTime()) ? null : value
  const str = String(value).trim()
  if (!str) return null
  // Có offset (vd: +07:00, Z) → new Date parse đúng
  if (str.includes('+') || str.endsWith('Z')) {
    const d = new Date(str)
    return Number.isNaN(d.getTime()) ? null : d
  }
  // Không có offset → thêm +07:00 để browser hiểu đúng giờ VN
  const d = new Date(str + '+07:00')
  return Number.isNaN(d.getTime()) ? null : d
}

/**
 * Relative time display (for live feeds and alerts).
 * e.g. "Vừa xong", "5m trước", "2h trước", "3d trước"
 */
export function relativeTime(ts, { short = false } = {}) {
  if (!ts) return ''
  const nowMs = Date.now()
  const tsMs = new Date(ts).getTime()
  if (!Number.isFinite(tsMs)) return ''
  const diffMs = nowMs - tsMs
  const secs = Math.floor(diffMs / 1000)
  if (short) {
    if (secs < 5) return 'Vừa'
    if (secs < 60) return `${secs}s`
    const mins = Math.floor(secs / 60)
    if (mins < 60) return `${mins}m`
    const hours = Math.floor(mins / 60)
    return `${hours}h`
  }
  if (secs < 60) return 'Vừa xong'
  const mins = Math.floor(secs / 60)
  if (mins < 60) return `${mins}m trước`
  const hours = Math.floor(mins / 60)
  if (hours < 24) return `${hours}h trước`
  const days = Math.floor(hours / 24)
  return `${days}d trước`
}

/**
 * Format Date thành chuỗi hiển thị tiếng Việt, luôn theo múi giờ +07:00.
 */
export function formatVietnamDate(date, options = {}) {
  if (!date || !(date instanceof Date) || Number.isNaN(date.getTime())) return null
  const defaults = {
    timeZone: 'Asia/Ho_Chi_Minh',
    locale: 'vi-VN',
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    ...options,
  }
  return new Intl.DateTimeFormat(defaults.locale, defaults).format(date)
}

/**
 * Format Date thành short date string (dd/MM HH:mm).
 */
export function formatVietnamShort(date) {
  return formatVietnamDate(date, {
    day: '2-digit',
    month: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}
