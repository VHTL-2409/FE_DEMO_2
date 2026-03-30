import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

const normalizeText = (value) => String(value || '').trim().toLowerCase()

const resolveRiskBand = (score) => {
  const numeric = Number(score || 0)
  if (numeric >= 81) return 'CRITICAL'
  if (numeric >= 61) return 'HIGH_RISK'
  if (numeric >= 31) return 'SUSPICIOUS'
  return 'CLEAN'
}

export const useProctorDashboardStore = defineStore('proctorDashboard', () => {
  const selectedExamId = ref(null)
  const cards = ref([])
  const detailsByAttemptId = ref({})
  const selectedAttemptIds = ref([])
  const connectionMode = ref('polling')
  const lastUpdatedAt = ref(null)
  const filters = ref({
    search: '',
    riskBand: 'ALL',
    status: 'ALL'
  })

  const visibleCards = computed(() => {
    const search = normalizeText(filters.value.search)
    return cards.value.filter((card) => {
      const nameMatches = !search || normalizeText(card.student || card.name).includes(search)
      const statusMatches = filters.value.status === 'ALL' || String(card.status || '').toUpperCase() === filters.value.status
      const riskBand = resolveRiskBand(card.riskScore)
      const riskMatches = filters.value.riskBand === 'ALL' || riskBand === filters.value.riskBand
      return nameMatches && statusMatches && riskMatches
    })
  })

  const setSelectedExam = (examId) => {
    selectedExamId.value = examId
  }

  const setCards = (nextCards) => {
    cards.value = Array.isArray(nextCards) ? nextCards : []
    lastUpdatedAt.value = new Date().toISOString()
  }

  const upsertCard = (card) => {
    const next = Array.isArray(cards.value) ? [...cards.value] : []
    const index = next.findIndex((item) => item.attemptId === card.attemptId)
    if (index >= 0) {
      next[index] = { ...next[index], ...card }
    } else {
      next.push(card)
    }
    cards.value = next
    lastUpdatedAt.value = new Date().toISOString()
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
    filters.value = {
      search: '',
      riskBand: 'ALL',
      status: 'ALL'
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
      CRITICAL: cards.value.filter(c => resolveRiskBand(c.riskScore) === 'CRITICAL'),
      HIGH_RISK: cards.value.filter(c => resolveRiskBand(c.riskScore) === 'HIGH_RISK'),
      SUSPICIOUS: cards.value.filter(c => resolveRiskBand(c.riskScore) === 'SUSPICIOUS'),
      CLEAN: cards.value.filter(c => resolveRiskBand(c.riskScore) === 'CLEAN')
    }
  })

  const connectionHealth = computed(() => {
    const total = cards.value.length
    if (total === 0) return { online: 0, offline: 0, percent: 100, level: 'unknown' }
    const online = cards.value.filter(c => {
      const s = String(c.status || '').toUpperCase()
      return s === 'ACTIVE' || s === 'IN_PROGRESS'
    }).length
    const offline = total - online
    const percent = Math.round((online / total) * 100)
    let level = 'healthy'
    if (percent < 50) level = 'critical'
    else if (percent < 80) level = 'degraded'
    return { online, offline, total, percent, level }
  })

  const flagStats = computed(() => {
    const flagged = cards.value.filter(c => Number(c.riskScore || 0) > 0 || c.suspicious).length
    const critical = cards.value.filter(c => Number(c.riskScore || 0) >= 81).length
    return { flagged, critical }
  })

  return {
    selectedExamId,
    cards,
    detailsByAttemptId,
    selectedAttemptIds,
    connectionMode,
    lastUpdatedAt,
    filters,
    visibleCards,
    roomsGroupedByExam,
    alertsBySeverity,
    connectionHealth,
    flagStats,
    setSelectedExam,
    setCards,
    upsertCard,
    setDetail,
    setFilters,
    toggleSelection,
    clearSelection,
    setConnectionMode,
    reset
  }
})
