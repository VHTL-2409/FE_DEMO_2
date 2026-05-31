const OPTION_LABEL_PATTERN = /^([A-Z0-9]+)[.):-]\s*(.*)$/i

export const scorePercentValue = (value) => {
  const numeric = Number(value)
  if (!Number.isFinite(numeric)) return 0
  return Math.max(0, Math.min(100, numeric))
}

export const formatScoreTen = (value, digits = 1) => {
  const numeric = Number(value)
  if (!Number.isFinite(numeric)) return '0.0'
  return (scorePercentValue(numeric) / 10).toFixed(digits)
}

export const normalizeOptionId = (value) => {
  if (value == null) return ''
  const normalized = String(value).trim().toUpperCase()
  if (!normalized) return ''
  const matched = normalized.match(/^[A-Z0-9]+/)
  return matched ? matched[0] : normalized.charAt(0)
}

const parseOptionLine = (value, fallbackId) => {
  const text = String(value || '').trim()
  if (!text) {
    return { id: fallbackId, text: '' }
  }
  const match = text.match(OPTION_LABEL_PATTERN)
  if (!match) {
    return { id: fallbackId, text }
  }
  return {
    id: normalizeOptionId(match[1]) || fallbackId,
    text: String(match[2] || '').trim()
  }
}

const normalizeOptionObject = (option, fallbackId) => {
  const resolvedId = normalizeOptionId(
    option?.id ??
    option?.key ??
    option?.letter ??
    option?.label ??
    fallbackId
  ) || fallbackId
  const rawText = String(
    option?.text ??
    option?.content ??
    option?.value ??
    option?.option ??
    option?.title ??
    option?.body ??
    ''
  ).trim()
  const parsed = parseOptionLine(rawText, resolvedId)
  return {
    id: resolvedId,
    text: parsed.id === resolvedId ? parsed.text : rawText
  }
}

export const parseResultOptions = (rawOptions) => {
  if (!rawOptions) return []

  if (Array.isArray(rawOptions)) {
    return rawOptions
      .map((option, index) => {
        const fallbackId = String.fromCharCode(65 + index)
        if (typeof option === 'string' || typeof option === 'number') {
          return parseOptionLine(option, fallbackId)
        }
        if (option && typeof option === 'object') {
          return normalizeOptionObject(option, fallbackId)
        }
        return null
      })
      .filter(Boolean)
  }

  if (typeof rawOptions === 'string') {
    const trimmed = rawOptions.trim()
    if (!trimmed) return []

    if (trimmed.startsWith('[') || trimmed.startsWith('{')) {
      try {
        const parsed = JSON.parse(trimmed)
        if (Array.isArray(parsed)) {
          return parseResultOptions(parsed)
        }
        const nested = parsed?.options || parsed?.choices || parsed?.answers || parsed?.items || []
        return parseResultOptions(nested)
      } catch {
        // Fall through to line-based parsing.
      }
    }

    return trimmed
      .split(/\r?\n/)
      .map((line, index) => parseOptionLine(line, String.fromCharCode(65 + index)))
      .filter((option) => option.text || option.id)
  }

  return []
}

export const normalizeAnswerIds = (value) => {
  if (value == null || value === '') return []

  if (Array.isArray(value)) {
    return value.map(normalizeOptionId).filter(Boolean)
  }

  if (typeof value === 'string') {
    const trimmed = value.trim()
    if (!trimmed) return []

    if (trimmed.startsWith('[') || trimmed.startsWith('{')) {
      try {
        const parsed = JSON.parse(trimmed)
        return normalizeAnswerIds(parsed)
      } catch {
        // Fall back to comma/plain string handling.
      }
    }

    if (trimmed.includes(',')) {
      return trimmed.split(',').map(normalizeOptionId).filter(Boolean)
    }

    return [normalizeOptionId(trimmed)].filter(Boolean)
  }

  return [normalizeOptionId(value)].filter(Boolean)
}

export const answerIdsMatch = (selectedIds, correctIds) => {
  const normalizedSelected = normalizeAnswerIds(selectedIds)
  const normalizedCorrect = normalizeAnswerIds(correctIds)
  if (normalizedSelected.length !== normalizedCorrect.length) return false
  return normalizedSelected.every((id) => normalizedCorrect.includes(id))
}

export const formatAttemptAnswer = (value, options = []) => {
  const ids = normalizeAnswerIds(value)
  if (!ids.length) return ''

  const optionMap = new Map(
    parseResultOptions(options).map((option) => [normalizeOptionId(option.id), option.text])
  )

  return ids
    .map((id) => {
      const text = optionMap.get(normalizeOptionId(id))
      return text ? `${id}. ${text}` : id
    })
    .join(', ')
}

export const getAttemptStatusMeta = (status) => {
  const normalized = String(status || '').toUpperCase()
  const metaMap = {
    SUBMITTED: { label: 'Đã nộp bài', key: 'ended' },
    AUTO_SUBMITTED: { label: 'Tự động nộp', key: 'warning' },
    IN_PROGRESS: { label: 'Đang làm bài', key: 'live' },
    ACTIVE: { label: 'Đang làm bài', key: 'live' },
    PAUSED: { label: 'Tạm dừng', key: 'warning' },
    DRAFT: { label: 'Bản nháp', key: 'draft' }
  }
  return metaMap[normalized] || { label: normalized || 'Không rõ', key: 'neutral' }
}
