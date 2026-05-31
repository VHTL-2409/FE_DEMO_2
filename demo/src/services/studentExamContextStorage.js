const WAITING_ROOM_KEY = 'studentExam:waitingRoom'
const RULES_KEY = 'studentExam:rules'
const ATTEMPT_KEY = 'studentExam:attempt'
const SUBMISSION_KEY = 'studentExam:submission'
const RESULT_KEY = 'studentExam:result'

function canUseSessionStorage() {
  return typeof sessionStorage !== 'undefined'
}

function readKey(key) {
  if (!canUseSessionStorage()) return null
  try {
    const raw = sessionStorage.getItem(key)
    return raw ? JSON.parse(raw) : null
  } catch {
    return null
  }
}

function writeKey(key, value) {
  if (!canUseSessionStorage() || !value) return
  try {
    sessionStorage.setItem(key, JSON.stringify(value))
  } catch {
    /* ignore */
  }
}

function toStringOrEmpty(value) {
  return value == null ? '' : String(value)
}

export function buildWaitingRoomQuery(exam = {}) {
  return {
    examId: toStringOrEmpty(exam.id ?? exam.examId),
    examCode: toStringOrEmpty(exam.code),
    exam: toStringOrEmpty(exam.title || exam.examTitle || 'Bài thi'),
    duration: toStringOrEmpty(exam.durationMinutes ?? exam.duration ?? 60),
    questions: toStringOrEmpty(exam.questionCount ?? 0),
    startAt: toStringOrEmpty(exam.startTime || exam.startDate),
    endAt: toStringOrEmpty(exam.endTime || exam.endDate),
    className: toStringOrEmpty(exam.className),
    requireCameraMic: exam.requireCameraMic === false ? 'false' : 'true',
    enableAiProctoring: exam.enableAiProctoring === true || exam.aiFaceDetection === true || exam.aiEyeTracking === true ? 'true' : 'false',
    aiFaceDetection: exam.aiFaceDetection === true ? 'true' : 'false',
    aiEyeTracking: exam.aiEyeTracking === true ? 'true' : 'false'
  }
}

export function buildAttemptQuery({
  examTitle,
  examId,
  attemptId,
  deadlineAt,
  remainingSeconds,
  startedAt,
  isPractice,
  identityCheckId,
  verificationStatus
}) {
  return {
    exam: toStringOrEmpty(examTitle || 'Bài thi'),
    examId: toStringOrEmpty(examId),
    attemptId: toStringOrEmpty(attemptId),
    deadlineAt: toStringOrEmpty(deadlineAt),
    remainingSeconds: toStringOrEmpty(remainingSeconds),
    startedAt: toStringOrEmpty(startedAt),
    isPractice: isPractice ? 'true' : undefined,
    identityCheckId: toStringOrEmpty(identityCheckId),
    verificationStatus: toStringOrEmpty(verificationStatus)
  }
}

export function buildSubmissionQuery({ attemptId, examTitle, score, submittedAt, showScoreAfterSubmit, allowReviewAfterSubmit }) {
  return {
    attemptId: toStringOrEmpty(attemptId),
    exam: toStringOrEmpty(examTitle || 'Bài thi'),
    score: toStringOrEmpty(score),
    submittedAt: toStringOrEmpty(submittedAt),
    showScoreAfterSubmit: typeof showScoreAfterSubmit === 'boolean' ? String(showScoreAfterSubmit) : undefined,
    allowReviewAfterSubmit: typeof allowReviewAfterSubmit === 'boolean' ? String(allowReviewAfterSubmit) : undefined
  }
}

export function buildResultQuery({ attemptId, examTitle, score, rankOrder }) {
  return {
    attemptId: toStringOrEmpty(attemptId),
    examTitle: toStringOrEmpty(examTitle || 'Kết quả bài thi'),
    score: toStringOrEmpty(score),
    rankOrder: toStringOrEmpty(rankOrder)
  }
}

export function writeWaitingRoomQuery(query) {
  if (!query?.examId) return
  writeKey(WAITING_ROOM_KEY, {
    examId: toStringOrEmpty(query.examId),
    examCode: toStringOrEmpty(query.examCode),
    exam: toStringOrEmpty(query.exam),
    duration: toStringOrEmpty(query.duration),
    questions: toStringOrEmpty(query.questions),
    startAt: toStringOrEmpty(query.startAt),
    endAt: toStringOrEmpty(query.endAt),
    className: toStringOrEmpty(query.className),
    requireCameraMic: toStringOrEmpty(query.requireCameraMic || 'true'),
    enableAiProctoring: toStringOrEmpty(query.enableAiProctoring || 'false'),
    aiFaceDetection: toStringOrEmpty(query.aiFaceDetection || 'false'),
    aiEyeTracking: toStringOrEmpty(query.aiEyeTracking || 'false')
  })
}

export function buildRulesQuery({
  examTitle,
  examId,
  attemptId,
  examCode,
  duration,
  questions,
  startAt,
  endAt,
  className,
  identityCheckId,
  verificationStatus,
  requireCameraMic,
  enableAiProctoring
}) {
  return {
    exam: toStringOrEmpty(examTitle || 'BÃ i thi'),
    examId: toStringOrEmpty(examId),
    attemptId: toStringOrEmpty(attemptId),
    examCode: toStringOrEmpty(examCode),
    duration: toStringOrEmpty(duration),
    questions: toStringOrEmpty(questions),
    startAt: toStringOrEmpty(startAt),
    endAt: toStringOrEmpty(endAt),
    className: toStringOrEmpty(className),
    identityCheckId: toStringOrEmpty(identityCheckId),
    verificationStatus: toStringOrEmpty(verificationStatus),
    requireCameraMic: toStringOrEmpty(requireCameraMic || 'true'),
    enableAiProctoring: toStringOrEmpty(enableAiProctoring || 'false')
  }
}

export function readWaitingRoomQuery() {
  const value = readKey(WAITING_ROOM_KEY)
  if (!value?.examId) return null
  return {
    examId: toStringOrEmpty(value.examId),
    examCode: toStringOrEmpty(value.examCode),
    exam: toStringOrEmpty(value.exam),
    duration: toStringOrEmpty(value.duration),
    questions: toStringOrEmpty(value.questions),
    startAt: toStringOrEmpty(value.startAt),
    endAt: toStringOrEmpty(value.endAt),
    className: toStringOrEmpty(value.className),
    requireCameraMic: toStringOrEmpty(value.requireCameraMic || 'true'),
    enableAiProctoring: toStringOrEmpty(value.enableAiProctoring || 'false'),
    aiFaceDetection: toStringOrEmpty(value.aiFaceDetection || 'false'),
    aiEyeTracking: toStringOrEmpty(value.aiEyeTracking || 'false')
  }
}

export function writeRulesQuery(query) {
  if (!query?.attemptId || !query?.examId) return
  writeKey(RULES_KEY, {
    exam: toStringOrEmpty(query.exam),
    examId: toStringOrEmpty(query.examId),
    attemptId: toStringOrEmpty(query.attemptId),
    examCode: toStringOrEmpty(query.examCode),
    duration: toStringOrEmpty(query.duration),
    questions: toStringOrEmpty(query.questions),
    startAt: toStringOrEmpty(query.startAt),
    endAt: toStringOrEmpty(query.endAt),
    className: toStringOrEmpty(query.className),
    identityCheckId: toStringOrEmpty(query.identityCheckId),
    verificationStatus: toStringOrEmpty(query.verificationStatus),
    requireCameraMic: toStringOrEmpty(query.requireCameraMic || 'true'),
    enableAiProctoring: toStringOrEmpty(query.enableAiProctoring || 'false')
  })
}

export function readRulesQuery() {
  const value = readKey(RULES_KEY)
  if (!value?.attemptId || !value?.examId) return null
  return {
    exam: toStringOrEmpty(value.exam),
    examId: toStringOrEmpty(value.examId),
    attemptId: toStringOrEmpty(value.attemptId),
    examCode: toStringOrEmpty(value.examCode),
    duration: toStringOrEmpty(value.duration),
    questions: toStringOrEmpty(value.questions),
    startAt: toStringOrEmpty(value.startAt),
    endAt: toStringOrEmpty(value.endAt),
    className: toStringOrEmpty(value.className),
    identityCheckId: toStringOrEmpty(value.identityCheckId),
    verificationStatus: toStringOrEmpty(value.verificationStatus),
    requireCameraMic: toStringOrEmpty(value.requireCameraMic || 'true'),
    enableAiProctoring: toStringOrEmpty(value.enableAiProctoring || 'false')
  }
}

export function writeAttemptQuery(query) {
  if (!query?.attemptId || !query?.examId) return
  writeKey(ATTEMPT_KEY, {
    exam: toStringOrEmpty(query.exam),
    examId: toStringOrEmpty(query.examId),
    attemptId: toStringOrEmpty(query.attemptId),
    deadlineAt: toStringOrEmpty(query.deadlineAt),
    remainingSeconds: toStringOrEmpty(query.remainingSeconds),
    startedAt: toStringOrEmpty(query.startedAt),
    isPractice: toStringOrEmpty(query.isPractice),
    identityCheckId: toStringOrEmpty(query.identityCheckId),
    verificationStatus: toStringOrEmpty(query.verificationStatus)
  })
}

export function readAttemptQuery() {
  const value = readKey(ATTEMPT_KEY)
  if (!value?.attemptId || !value?.examId) return null
  return {
    exam: toStringOrEmpty(value.exam),
    examId: toStringOrEmpty(value.examId),
    attemptId: toStringOrEmpty(value.attemptId),
    deadlineAt: toStringOrEmpty(value.deadlineAt),
    remainingSeconds: toStringOrEmpty(value.remainingSeconds),
    startedAt: toStringOrEmpty(value.startedAt),
    isPractice: toStringOrEmpty(value.isPractice),
    identityCheckId: toStringOrEmpty(value.identityCheckId),
    verificationStatus: toStringOrEmpty(value.verificationStatus)
  }
}

export function writeSubmissionQuery(query) {
  if (!query?.attemptId) return
  writeKey(SUBMISSION_KEY, {
    attemptId: toStringOrEmpty(query.attemptId),
    exam: toStringOrEmpty(query.exam),
    score: toStringOrEmpty(query.score),
    submittedAt: toStringOrEmpty(query.submittedAt)
  })
}

export function readSubmissionQuery() {
  const value = readKey(SUBMISSION_KEY)
  if (!value?.attemptId) return null
  return {
    attemptId: toStringOrEmpty(value.attemptId),
    exam: toStringOrEmpty(value.exam),
    score: toStringOrEmpty(value.score),
    submittedAt: toStringOrEmpty(value.submittedAt)
  }
}

export function writeResultQuery(query) {
  if (!query?.attemptId) return
  writeKey(RESULT_KEY, {
    attemptId: toStringOrEmpty(query.attemptId),
    examTitle: toStringOrEmpty(query.examTitle || query.exam),
    score: toStringOrEmpty(query.score),
    rankOrder: toStringOrEmpty(query.rankOrder)
  })
}

export function readResultQuery() {
  const value = readKey(RESULT_KEY)
  if (!value?.attemptId) return null
  return {
    attemptId: toStringOrEmpty(value.attemptId),
    examTitle: toStringOrEmpty(value.examTitle),
    score: toStringOrEmpty(value.score),
    rankOrder: toStringOrEmpty(value.rankOrder)
  }
}
