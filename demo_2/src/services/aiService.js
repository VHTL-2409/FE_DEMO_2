import { apiRequest, unwrapApiData } from './apiClient'

export const generateQuestionsFromTopic = async (topic, count = 5, difficulty = 'MEDIUM', language = 'vi') => {
  const payload = await apiRequest('/api/v1/ai/generate/from-topic', {
    method: 'POST',
    body: JSON.stringify({
      topic,
      count,
      difficulty,
      language
    })
  })
  return unwrapApiData(payload)
}

export const generateQuestionsFromText = async (text, count = 5, difficulty = 'MEDIUM', language = 'vi') => {
  const payload = await apiRequest('/api/v1/ai/generate/from-text', {
    method: 'POST',
    body: JSON.stringify({
      text,
      count,
      difficulty,
      language
    })
  })
  return unwrapApiData(payload)
}

export const evaluateEssay = async (question, answer, rubric = null, maxScore = 10) => {
  const payload = await apiRequest('/api/v1/ai/evaluate/essay', {
    method: 'POST',
    body: JSON.stringify({
      question,
      answer,
      rubric,
      maxScore
    })
  })
  return unwrapApiData(payload)
}

export const predictPerformance = async (studentId, examId = null, history = []) => {
  const payload = await apiRequest('/api/v1/ai/analytics/predict', {
    method: 'POST',
    body: JSON.stringify({
      studentId,
      examId,
      history
    })
  })
  return unwrapApiData(payload)
}

export const getStudyRecommendations = async (studentId, history = []) => {
  const payload = await apiRequest(`/api/v1/ai/analytics/recommendations/${studentId}?history=${encodeURIComponent(JSON.stringify(history))}`)
  return unwrapApiData(payload)
}

export const analyzeQuestionQuality = async (questionContent, options, correctAnswer, difficulty = null) => {
  const payload = await apiRequest('/api/v1/ai/analytics/question-quality', {
    method: 'POST',
    body: JSON.stringify({
      questionContent,
      options,
      correctAnswer,
      difficulty
    })
  })
  return unwrapApiData(payload)
}

export const analyzeDifficultyDistribution = async (questions) => {
  const payload = await apiRequest('/api/v1/ai/analytics/difficulty-distribution', {
    method: 'POST',
    body: JSON.stringify({
      questions
    })
  })
  return unwrapApiData(payload)
}

export const getAiServiceStatus = async () => {
  const payload = await apiRequest('/api/v1/ai/status')
  return unwrapApiData(payload)
}

export const getGeneratorStatus = async () => {
  const payload = await apiRequest('/api/v1/ai/generate/status')
  return unwrapApiData(payload)
}

export const getEvaluatorStatus = async () => {
  const payload = await apiRequest('/api/v1/ai/evaluate/status')
  return unwrapApiData(payload)
}

/**
 * @param {{ role: string, content: string }[]} messages
 * @param {string} [model]
 */
export const sendChatMessage = async (messages, model) => {
  const body = { messages }
  if (model) body.model = model
  const payload = await apiRequest('/api/v1/ai/chat', {
    method: 'POST',
    body: JSON.stringify(body)
  })
  return unwrapApiData(payload)
}

export const getChatModels = async () => {
  const payload = await apiRequest('/api/v1/ai/chat/models')
  return unwrapApiData(payload)
}
