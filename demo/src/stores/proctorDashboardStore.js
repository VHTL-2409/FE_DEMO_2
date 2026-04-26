import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

const normalizeText = (value) => String(value || '').trim().toLowerCase()
const resolveAttemptId = (card) => card?.attemptId ?? card?.id
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

export const useProctorDashboardStore = defineStore('proctorDashboard', () => {
  const selectedExamId = ref(null)
  const cards = ref([])
  const detailsByAttemptId = ref({})
  const selectedAttemptIds = ref([])
  const connectionMode = ref('polling')
  const lastUpdatedAt = ref(null)
  // Tracks previous risk scores for delta computation
  const previousRiskScores = ref({})
  const filters = ref({
    search: '',
    riskBand: 'ALL',
    status: 'ALL',
    timeRange: 'all',
    reviewOnly: false
  })

  // ── Alert & event deduplication ──────────────────────────────────────────
  const recentAlertKeys = new Map() // key → timestamp
  const ALERT_DEDUPE_WINDOW_MS = 10_000
  let alertIdCounter = 0
  let eventIdCounter = 0
  const liveAlerts = ref([])
  const liveEvents = ref([])

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

  const setSelectedExam = (examId) => {
    selectedExamId.value = examId
  }

  const setCards = (nextCards) => {
    // Save current scores before replacing cards
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

  /**
   * Update previous risk score for a specific card (e.g. when receiving realtime update).
   */
  const trackRiskDelta = (attemptId, newScore) => {
    const id = String(attemptId)
    if (previousRiskScores.value[id] == null) {
      previousRiskScores.value[id] = newScore
    }
  }

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
    alertIdCounter = 0
    eventIdCounter = 0
    filters.value = {
      search: '',
      riskBand: 'ALL',
      status: 'ALL',
      timeRange: 'all',
      reviewOnly: false
    }
  }

  const roomsGroupedByExam = computed(() => {
    const groups = {}
    for (const card of cards.value) {
      const examId = card.examId || 'unknown'
      if (!groups[examId]) {
        groups[examId] = []
      }
      groups[examId].push(card)
    }
    return groups
  })

  const alertsBySeverity = computed(() => {
    return {
      CRITICAL: cardsWithMeta.value.filter(c => c._riskBand === 'CRITICAL'),
      HIGH_RISK: cardsWithMeta.value.filter(c => c._riskBand === 'HIGH_RISK'),
      SUSPICIOUS: cardsWithMeta.value.filter(c => c._riskBand === 'SUSPICIOUS'),
      CLEAN: cardsWithMeta.value.filter(c => c._riskBand === 'CLEAN')
    }
  })

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

  /**
   * Add a fraud signal alert with deduplication.
   * Returns true if the alert was added, false if it was suppressed as a duplicate.
   * Dedupe key = `${attemptId}|${signalType}|${severity}`
   */
  const addAlert = (alert) => {
    const signalType = alert.signalType || alert.type || ''
    const key = `${alert.attemptId || ''}|${signalType}|${alert.severity || ''}`
    const now = Date.now()
    const lastSeen = recentAlertKeys.get(key)
    if (lastSeen && (now - lastSeen) < ALERT_DEDUPE_WINDOW_MS) {
      return false // duplicate, skip
    }
    recentAlertKeys.set(key, now)
    const eventTime = normalizeTime(alert.issuedAt || alert.at || alert.timestamp || alert.ts) || now
    liveAlerts.value = [
      { ...alert, signalType, id: `alert-${++alertIdCounter}`, ts: eventTime },
      ...liveAlerts.value
    ].slice(0, 200) // keep last 200
    // Prune old entries every 50 adds
    if (alertIdCounter % 50 === 0) {
      for (const [k, t] of recentAlertKeys) {
        if (now - t > ALERT_DEDUPE_WINDOW_MS) recentAlertKeys.delete(k)
      }
    }
    return true
  }

  /**
   * Add a system/proctoring event (warning, pause, resume, etc.) without deduplication.
   */
  const addEvent = (event) => {
    const eventTime = normalizeTime(event.issuedAt || event.at || event.timestamp || event.ts) || Date.now()
    liveEvents.value = [
      { ...event, id: `event-${++eventIdCounter}`, ts: eventTime },
      ...liveEvents.value
    ].slice(0, 200)
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
    resolveRiskBand
  }
})

function matchesTimeRange(card, timeRange) {
  if (!timeRange || timeRange === 'all') return true
  const source = normalizeTime(card.lastSignalAt || card.startedAt || card.submittedAt)
  if (!source) return true
  const now = Date.now()
  const diff = now - source
  switch (timeRange) {
    case '5m':
      return diff <= 5 * 60 * 1000
    case '15m':
      return diff <= 15 * 60 * 1000
    case '1h':
      return diff <= 60 * 60 * 1000
    case 'today': {
      const d = new Date(source)
      const n = new Date(now)
      return d.getFullYear() === n.getFullYear()
        && d.getMonth() === n.getMonth()
        && d.getDate() === n.getDate()
    }
    default:
      return true
  }
}
