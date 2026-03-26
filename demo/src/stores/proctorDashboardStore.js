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

  return {
    selectedExamId,
    cards,
    detailsByAttemptId,
    selectedAttemptIds,
    connectionMode,
    lastUpdatedAt,
    filters,
    visibleCards,
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
