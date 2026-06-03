import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { isCameraRelatedCategory, isCameraRelatedSignal, normalizeSignalCategory, normalizeSignalType } from '../utils/proctorSignalTypes'

const normalizeText = (value) => String(value || '').trim().toLowerCase()
const resolveAttemptId = (card) => {
  const raw = card?.attemptId ?? card?.id
  if (raw == null || raw === '') return raw
  const numeric = Number(raw)
  return Number.isFinite(numeric) ? numeric : raw
}
const normalizeTime = (value) => {
  if (!value) return 0
  const time = new Date(value).getTime()
  return Number.isFinite(time) ? time : 0
}

const resolveRiskBand = (score) => {
  const numeric = Number(score || 0)
  if (numeric >= 81) return 'CRITICAL'
  if (numeric >= 61) return 'HIGH_RISK'
  if (numeric >= 31) return 'SUSPICIOUS'
  return 'CLEAN'
}

const resolveStatusToken = (card) => {
  const raw = String(card.status || '').toUpperCase()
  if (raw === 'ACTIVE' || raw === 'IN_PROGRESS') return 'ONLINE'
  if (raw === 'PAUSED') return 'PAUSED'
  if (raw === 'SUBMITTED' || raw === 'AUTO_SUBMITTED' || raw === 'COMPLETED') return 'SUBMITTED'
  if (raw === 'STOPPED') return 'STOPPED'
  if (raw === 'OFFLINE') return 'OFFLINE'
  return raw || 'UNKNOWN'
}

// ── Event deduplication ──────────────────────────────────────────────────────
const recentEventKeys = new Map() // key → timestamp
const EVENT_DEDUPE_WINDOW_MS = 5_000

const makeEventKey = (event) =>
  `${event.attemptId || ''}:${event.type || event.eventType || ''}:${event.occurredAt || event.issuedAt || event.at || ''}`

const isCameraAlert = (alert = {}) => {
  const category = normalizeSignalCategory(alert.category || alert.warningCategory || alert.latestSignalCategory)
  const signalType = normalizeSignalType(alert.signalType || alert.warningType || alert.type)
  return signalType === 'AI_CAMERA_SIGNAL'
    || isCameraRelatedCategory(category)
    || isCameraRelatedSignal(signalType)
}

const buildAlertKey = (alert = {}) => {
  const attemptId = alert.attemptId || ''
  const signalType = normalizeSignalType(alert.signalType || alert.warningType || alert.type)
  const category = normalizeSignalCategory(alert.category || alert.warningCategory || alert.latestSignalCategory)
  const relatedKey = Array.isArray(alert.relatedAttemptIds) ? alert.relatedAttemptIds.join(',') : ''

  if (isCameraAlert(alert)) {
    return `${attemptId}|CAMERA|${signalType}`
  }

  return `${attemptId}|${category}|${signalType}|${normalizeSignalType(alert.severity)}|${relatedKey}`
}

export const useProctorDashboardStore = defineStore('proctorDashboard', () => {
  const selectedExamId = ref(null)
  const cards = ref([])
  const detailsByAttemptId = ref({})
  const selectedAttemptIds = ref([])
  const connectionMode = ref('polling')
  const lastUpdatedAt = ref(null)
  const previousRiskScores = ref({})
  const filters = ref({
    search: '',
    riskBand: 'ALL',
    status: 'ALL',
    timeRange: 'all',
    category: 'ALL',
    reviewOnly: false
  })

  // ── Alert & event state ────────────────────────────────────────────────────
  const recentAlertKeys = new Map()
  const ALERT_DEDUPE_WINDOW_MS = 10_000
  let alertIdCounter = 0
  let eventIdCounter = 0
  const liveAlerts = ref([])
  const liveEvents = ref([])

  // ── Map-backed lookup for O(1) patch ───────────────────────────────────────
  const cardsMap = computed(() => {
    const m = {}
    for (const card of cards.value) {
      const id = resolveAttemptId(card)
      if (id != null) m[id] = card
    }
    return m
  })

  const cardsWithMeta = computed(() => cards.value.map((card) => {
    const riskBand = resolveRiskBand(card.riskScore)
    const statusToken = resolveStatusToken(card)
    const prevScore = previousRiskScores.value[resolveAttemptId(card)]
    const riskDelta = prevScore != null && card.riskScore != null
      ? card.riskScore - prevScore
      : null
    return {
      ...card,
      _riskBand: riskBand,
      _statusToken: statusToken,
      _riskDelta: riskDelta
    }
  }))

  const visibleCards = computed(() => {
    const search = normalizeText(filters.value.search)
    return cardsWithMeta.value.filter((card) => {
      const nameMatches = !search || normalizeText(card.student || card.name).includes(search)
      const statusMatches = filters.value.status === 'ALL' || card._statusToken === filters.value.status
      const riskMatches = filters.value.riskBand === 'ALL' || card._riskBand === filters.value.riskBand
      const reviewMatches = !filters.value.reviewOnly || card.reviewRequired
      const timeMatches = matchesTimeRange(card, filters.value.timeRange)
      return nameMatches && statusMatches && riskMatches && reviewMatches && timeMatches
    })
  })

  const alertsByCategory = computed(() => {
    return liveAlerts.value.reduce((groups, alert) => {
      const category = alert.category || alert.warningCategory || alert.latestSignalCategory || 'UNCATEGORIZED'
      if (!groups[category]) groups[category] = []
      groups[category].push(alert)
      return groups
    }, {})
  })

  const setSelectedExam = (examId) => {
    selectedExamId.value = examId
  }

  const setCards = (nextCards) => {
    for (const card of cards.value) {
      const id = resolveAttemptId(card)
      const newCard = Array.isArray(nextCards) ? nextCards.find(c => resolveAttemptId(c) === id) : null
      if (newCard && card.riskScore != null) {
        previousRiskScores.value[id] = card.riskScore
      }
    }
    cards.value = Array.isArray(nextCards)
      ? nextCards.map(card => ({ ...card, attemptId: resolveAttemptId(card) }))
      : []
    lastUpdatedAt.value = Date.now()
  }

  const trackRiskDelta = (attemptId, newScore) => {
    const id = String(attemptId)
    if (previousRiskScores.value[id] == null) {
      previousRiskScores.value[id] = newScore
    }
  }

  /**
   * Universal realtime event handler — patches or creates a card from a realtime event.
   * No full reload, no extra fetches. O(1) map lookup.
   */
  const patchAttemptFromRealtime = (event) => {
    const attemptId = resolveAttemptId({ attemptId: event.attemptId ?? event.id })
    if (!attemptId) return false

    // Dedupe: skip if same event within window
    const eventKey = makeEventKey(event)
    const now = Date.now()
    const lastSeen = recentEventKeys.get(eventKey)
    if (lastSeen && (now - lastSeen) < EVENT_DEDUPE_WINDOW_MS) {
      return false
    }
    recentEventKeys.set(eventKey, now)

    const existing = cardsMap.value[attemptId]
    const prevScore = existing?.riskScore

    const patch = compactPatch(buildCardPatch(event))
    if (existing) {
      const idx = cards.value.findIndex(c => String(resolveAttemptId(c)) === String(attemptId))
      if (idx >= 0) {
        if (patch.riskScore != null && prevScore != null && patch.riskScore !== prevScore) {
          previousRiskScores.value[attemptId] = prevScore
        }
        const violationCount = shouldIncrementViolationCount(event)
          ? Number(existing.violationCount || 0) + 1
          : existing.violationCount
        cards.value[idx] = { ...existing, ...patch, attemptId, violationCount }
      }
    } else {
      // New card — only push minimal data if we have enough info
      if (attemptId && (patch.riskScore != null || patch.student)) {
        const violationCount = shouldIncrementViolationCount(event) ? 1 : patch.violationCount
        cards.value.push({ attemptId, ...patch, violationCount })
      }
    }
    lastUpdatedAt.value = now
    return true
  }

  /**
   * Build a card patch from a realtime event payload.
   */
  const buildCardPatch = (event) => {
    const signal = event.latestSignal || {}
    const occurredAt = signal.occurredAt || event.issuedAt || event.updatedAt || new Date().toISOString()

    return {
      examId: event.examId ?? event.sessionId,
      attemptId: resolveAttemptId({ attemptId: event.attemptId ?? event.id }),
      student: event.student || event.studentName,
      studentName: event.studentName || event.student || event.studentCode,
      email: event.email,
      studentCode: event.studentCode,
      riskScore: event.riskScore ?? event.scores?.totalScore,
      riskLevel: event.riskLevel,
      status: event.status,
      startedAt: event.startedAt,
      submittedAt: event.submittedAt,
      deadlineAt: event.deadlineAt,
      remainingSeconds: event.remainingSeconds,
      cameraOn: event.cameraOn,
      micOn: event.micOn,
      clientIp: event.clientIp,
      deviceFingerprint: event.deviceFingerprint,
      originalDeviceFingerprint: event.originalDeviceFingerprint,
      saveCount: event.saveCount,
      submitCount: event.submitCount,
      reviewRequired: event.reviewRequired,
      recommendedAction: event.recommendedAction,
      reasons: event.reasons,
      evidenceSummary: event.evidenceSummary,
      activeFlagId: event.activeFlag?.id ?? event.activeFlagId,
      activeFlagStatus: event.activeFlag?.status ?? event.activeFlagStatus,
      activeFlagTitle: event.activeFlag?.title ?? event.latestFlagTitle,
      lastSignalAt: occurredAt,
      lastRiskUpdatedAt: occurredAt,
      latestSignalType: normalizeSignalType(signal.signalType || event.signalType || event.warningType),
      latestSignalSeverity: signal.severity || event.severity,
      latestSignalCategory: signal.category,
      latestSignalDisplayMessage: signal.displayMessage,
      latestSignalRiskImpact: signal.riskImpact,
      scores: event.scores,
      latestWarningCategory: event.type === 'FRAUD_WARNING_RECORDED' ? normalizeSignalType(event.warningCategory) : undefined,
      latestWarningType: event.type === 'FRAUD_WARNING_RECORDED' ? normalizeSignalType(event.warningType) : undefined,
      latestWarningRiskImpact: event.type === 'FRAUD_WARNING_RECORDED' ? event.riskImpact : undefined,
      latestWarningReviewStatus: event.type === 'FRAUD_WARNING_RECORDED' ? event.reviewStatus : undefined,
      lastWarning: event.type === 'WARNING_SENT' || event.type === 'FRAUD_WARNING_RECORDED' ? event.message : undefined,
      lastWarningAt: event.type === 'FRAUD_WARNING_RECORDED' ? occurredAt : undefined
    }
  }

  const shouldIncrementViolationCount = (event) => {
    const type = String(event?.type || event?.eventType || '').toUpperCase()
    if (type === 'FRAUD_WARNING_RECORDED') return false
    return type === 'FRAUD_SIGNAL_RECORDED' || Boolean(event?.signalType || event?.latestSignal?.signalType)
  }

  const compactPatch = (patch) => Object.fromEntries(
    Object.entries(patch || {}).filter(([, value]) => value !== undefined)
  )

  const upsertCard = (card) => {
    const cardAttemptId = resolveAttemptId(card)
    if (cardAttemptId == null) return
    const next = Array.isArray(cards.value) ? [...cards.value] : []
    const index = next.findIndex((item) => resolveAttemptId(item) === cardAttemptId)
    if (index >= 0) {
      if (card.riskScore != null && next[index].riskScore != null && card.riskScore !== next[index].riskScore) {
        previousRiskScores.value[cardAttemptId] = next[index].riskScore
      }
      next[index] = { ...next[index], ...card, attemptId: cardAttemptId }
    } else {
      next.push({ ...card, attemptId: cardAttemptId })
    }
    cards.value = next
    lastUpdatedAt.value = Date.now()
  }

  const setDetail = (attemptId, detail) => {
    detailsByAttemptId.value = {
      ...detailsByAttemptId.value,
      [attemptId]: detail
    }
  }

  const setFilters = (nextFilters = {}) => {
    filters.value = {
      ...filters.value,
      ...nextFilters
    }
  }

  const toggleSelection = (attemptId) => {
    const raw = Number(attemptId)
    if (selectedAttemptIds.value.includes(raw)) {
      selectedAttemptIds.value = selectedAttemptIds.value.filter((id) => id !== raw)
      return
    }
    selectedAttemptIds.value = [...selectedAttemptIds.value, raw]
  }

  const clearSelection = () => {
    selectedAttemptIds.value = []
  }

  const sortCards = (sortBy) => {
    const arr = [...visibleCards.value]
    switch (sortBy) {
      case 'risk':
        return arr.sort((a, b) => (resolveRiskBand(b.riskScore) !== resolveRiskBand(a.riskScore)
          ? bandOrder(resolveRiskBand(b.riskScore)) - bandOrder(resolveRiskBand(a.riskScore))
          : (Number(b.riskScore || 0) - Number(a.riskScore || 0))))
      case 'name':
        return arr.sort((a, b) => (a.student || b.name || '').localeCompare(b.student || b.name || ''))
      case 'violations':
        return arr.sort((a, b) => (Number(b.violationCount || 0) - Number(a.violationCount || 0)))
      case 'status':
        return arr.sort((a, b) => statusOrder(a) - statusOrder(b))
      default:
        return arr
    }
  }

  const bandOrder = (band) => {
    const order = { CRITICAL: 0, HIGH_RISK: 1, SUSPICIOUS: 2, CLEAN: 3 }
    return order[band] ?? 4
  }

  const statusOrder = (card) => {
    const s = String(card.status || '').toUpperCase()
    if (s === 'ACTIVE' || s === 'IN_PROGRESS') return 0
    if (s === 'PAUSED' || s === 'STOPPED') return 1
    if (s === 'SUBMITTED' || s === 'COMPLETED') return 2
    return 3
  }

  const setConnectionMode = (mode) => {
    connectionMode.value = mode || 'polling'
  }

  const reset = () => {
    selectedExamId.value = null
    cards.value = []
    detailsByAttemptId.value = {}
    selectedAttemptIds.value = []
    connectionMode.value = 'polling'
    lastUpdatedAt.value = null
    previousRiskScores.value = {}
    liveAlerts.value = []
    liveEvents.value = []
    recentAlertKeys.clear()
    recentEventKeys.clear()
    alertIdCounter = 0
    eventIdCounter = 0
    filters.value = {
      search: '',
      riskBand: 'ALL',
      status: 'ALL',
      timeRange: 'all',
      category: 'ALL',
      reviewOnly: false
    }
  }

  const roomsGroupedByExam = computed(() => {
    const groups = {}
    for (const card of cards.value) {
      const examId = card.examId || 'unknown'
      if (!groups[examId]) groups[examId] = []
      groups[examId].push(card)
    }
    return groups
  })

  const alertsBySeverity = computed(() => ({
    CRITICAL: cardsWithMeta.value.filter(c => c._riskBand === 'CRITICAL'),
    HIGH_RISK: cardsWithMeta.value.filter(c => c._riskBand === 'HIGH_RISK'),
    SUSPICIOUS: cardsWithMeta.value.filter(c => c._riskBand === 'SUSPICIOUS'),
    CLEAN: cardsWithMeta.value.filter(c => c._riskBand === 'CLEAN')
  }))

  const connectionHealth = computed(() => {
    const total = cardsWithMeta.value.length
    if (total === 0) return { online: 0, offline: 0, percent: 100, level: 'unknown' }
    const online = cardsWithMeta.value.filter(c => c._statusToken === 'ONLINE').length
    const offline = total - online
    const percent = Math.round((online / total) * 100)
    let level = 'healthy'
    if (percent < 50) level = 'critical'
    else if (percent < 80) level = 'degraded'
    return { online, offline, total, percent, level }
  })

  const flagStats = computed(() => {
    const flagged = cardsWithMeta.value.filter(c => Number(c.riskScore || 0) > 0 || c.suspicious).length
    const critical = cardsWithMeta.value.filter(c => c._riskBand === 'CRITICAL').length
    return { flagged, critical }
  })

  const addAlert = (alert) => {
    const signalType = normalizeSignalType(alert.signalType || alert.warningType || alert.type)
    const category = normalizeSignalType(alert.category || alert.warningCategory || alert.latestSignalCategory)
    const key = buildAlertKey(alert)
    const now = Date.now()
    const lastSeen = recentAlertKeys.get(key)
    if (lastSeen && (now - lastSeen) < ALERT_DEDUPE_WINDOW_MS) return false
    recentAlertKeys.set(key, now)
    const eventTime = normalizeTime(alert.issuedAt || alert.at || alert.timestamp || alert.ts) || now
    liveAlerts.value = [
      { ...alert, attemptId: resolveAttemptId({ attemptId: alert.attemptId }), signalType, category, id: `alert-${++alertIdCounter}`, ts: eventTime },
      ...liveAlerts.value
    ].slice(0, 200)
    if (alertIdCounter % 50 === 0) {
      for (const [k, t] of recentAlertKeys) {
        if (now - t > ALERT_DEDUPE_WINDOW_MS) recentAlertKeys.delete(k)
      }
    }
    return true
  }

  const addEvent = (event) => {
    const eventTime = normalizeTime(event.issuedAt || event.at || event.timestamp || event.ts) || Date.now()
    liveEvents.value = [
      { ...event, attemptId: resolveAttemptId({ attemptId: event.attemptId }), id: `event-${++eventIdCounter}`, ts: eventTime },
      ...liveEvents.value
    ].slice(0, 200)
  }

  // ── Dev logging helper ─────────────────────────────────────────────────────
  const debugLog = (context, event) => {
    if (import.meta.env.DEV && import.meta.env.MODE !== 'test') {
      const occurredAt = event.occurredAt || event.issuedAt || event.updatedAt
      const latencyMs = occurredAt ? Date.now() - new Date(occurredAt).getTime() : null
      console.debug(`[Realtime][${context}]`, {
        type: event.type,
        attemptId: event.attemptId,
        latencyMs,
        payload: event
      })
    }
  }

  return {
    selectedExamId,
    cards,
    detailsByAttemptId,
    selectedAttemptIds,
    connectionMode,
    lastUpdatedAt,
    previousRiskScores,
    filters,
    cardsWithMeta,
    visibleCards,
    roomsGroupedByExam,
    alertsBySeverity,
    connectionHealth,
    flagStats,
    liveAlerts,
    liveEvents,
    alertsByCategory,
    patchAttemptFromRealtime,
    buildCardPatch,
    addAlert,
    addEvent,
    setSelectedExam,
    setCards,
    upsertCard,
    setDetail,
    setFilters,
    toggleSelection,
    clearSelection,
    setConnectionMode,
    sortCards,
    trackRiskDelta,
    reset,
    resolveRiskBand,
    debugLog
  }
})

function matchesTimeRange(card, timeRange) {
  if (!timeRange || timeRange === 'all') return true
  const source = normalizeTime(card.lastSignalAt || card.startedAt || card.submittedAt)
  if (!source) return true
  const now = Date.now()
  const diff = now - source
  switch (timeRange) {
    case '5m': return diff <= 5 * 60 * 1000
    case '15m': return diff <= 15 * 60 * 1000
    case '1h': return diff <= 60 * 60 * 1000
    case 'today': {
      const d = new Date(source), n = new Date(now)
      return d.getFullYear() === n.getFullYear() && d.getMonth() === n.getMonth() && d.getDate() === n.getDate()
    }
    default: return true
  }
}
