import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

const toAnswerKey = (questionId) => String(questionId)

export const useExamSessionStore = defineStore('examSession', () => {
  const attempt = ref(null)
  const exam = ref(null)
  const questions = ref([])
  const answers = ref({})
  const markedQuestions = ref({})
  const visitedQuestions = ref({})
  const currentQuestionId = ref(null)
  const saveState = ref({
    status: 'idle',
    lastSavedAt: null,
    error: ''
  })
  const fullscreenState = ref({
    required: true,
    active: false
  })
  const deviceState = ref({
    cameraOn: false,
    micOn: false
  })
  const riskState = ref({
    riskScore: 0,
    riskLevel: 'CLEAN',
    status: 'IN_PROGRESS'
  })

  const answeredCount = computed(() =>
    Object.values(answers.value).filter((value) => {
      if (Array.isArray(value)) return value.length > 0
      return value !== null && value !== undefined && value !== ''
    }).length
  )

  const unansweredCount = computed(() => Math.max(questions.value.length - answeredCount.value, 0))
  const markedCount = computed(() => Object.values(markedQuestions.value).filter(Boolean).length)
  const visitedCount = computed(() => Object.values(visitedQuestions.value).filter(Boolean).length)

  const hydrateSession = ({ attempt: attemptPayload, exam: examPayload, questions: questionList, answers: answerMap }) => {
    attempt.value = attemptPayload || null
    exam.value = examPayload || null
    questions.value = Array.isArray(questionList) ? questionList : []
    answers.value = answerMap && typeof answerMap === 'object' ? { ...answerMap } : {}
    if (!currentQuestionId.value && questions.value.length > 0) {
      currentQuestionId.value = questions.value[0]?.id || null
    }
  }

  const setQuestionList = (questionList) => {
    questions.value = Array.isArray(questionList) ? questionList : []
    if (!currentQuestionId.value && questions.value.length > 0) {
      currentQuestionId.value = questions.value[0]?.id || null
    }
  }

  const setAnswer = (questionId, value) => {
    const key = toAnswerKey(questionId)
    answers.value[key] = value
    visitedQuestions.value[key] = true
  }

  const setEssayAnswer = (questionId, content) => {
    const key = toAnswerKey(questionId)
    answers.value[key] = content
    visitedQuestions.value[key] = true
  }

  const setMarked = (questionId, marked = true) => {
    markedQuestions.value[toAnswerKey(questionId)] = marked
  }

  const toggleMarked = (questionId) => {
    const key = toAnswerKey(questionId)
    setMarked(questionId, !markedQuestions.value[key])
  }

  const setVisited = (questionId) => {
    visitedQuestions.value[toAnswerKey(questionId)] = true
  }

  const setCurrentQuestion = (questionId) => {
    currentQuestionId.value = questionId
    if (questionId != null) {
      setVisited(questionId)
    }
  }

  const setSaveStatus = ({ status, lastSavedAt = null, error = '' }) => {
    saveState.value = {
      status: status || 'idle',
      lastSavedAt: lastSavedAt || saveState.value.lastSavedAt,
      error
    }
  }

  const setFullscreenState = ({ required, active }) => {
    fullscreenState.value = {
      required: required ?? fullscreenState.value.required,
      active: active ?? fullscreenState.value.active
    }
  }

  const setDeviceState = ({ cameraOn, micOn }) => {
    deviceState.value = {
      cameraOn: cameraOn ?? deviceState.value.cameraOn,
      micOn: micOn ?? deviceState.value.micOn
    }
  }

  const setRiskState = ({ riskScore, riskLevel, status }) => {
    riskState.value = {
      riskScore: riskScore ?? riskState.value.riskScore,
      riskLevel: riskLevel ?? riskState.value.riskLevel,
      status: status ?? riskState.value.status
    }
  }

  const reset = () => {
    attempt.value = null
    exam.value = null
    questions.value = []
    answers.value = {}
    markedQuestions.value = {}
    visitedQuestions.value = {}
    currentQuestionId.value = null
    saveState.value = {
      status: 'idle',
      lastSavedAt: null,
      error: ''
    }
    fullscreenState.value = {
      required: true,
      active: false
    }
    deviceState.value = {
      cameraOn: false,
      micOn: false
    }
    riskState.value = {
      riskScore: 0,
      riskLevel: 'CLEAN',
      status: 'IN_PROGRESS'
    }
  }

  return {
    attempt,
    exam,
    questions,
    answers,
    markedQuestions,
    visitedQuestions,
    currentQuestionId,
    saveState,
    fullscreenState,
    deviceState,
    riskState,
    answeredCount,
    unansweredCount,
    markedCount,
    visitedCount,
    hydrateSession,
    setQuestionList,
    setAnswer,
    setEssayAnswer,
    setMarked,
    toggleMarked,
    setVisited,
    setCurrentQuestion,
    setSaveStatus,
    setFullscreenState,
    setDeviceState,
    setRiskState,
    reset
  }
})
