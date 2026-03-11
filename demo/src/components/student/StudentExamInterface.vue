<template>
  <div :class="isDark ? 'dark' : 'light'" class="bg-background-light dark:bg-background-dark text-slate-900 dark:text-slate-100 min-h-screen flex flex-col font-display">
    <StudentTopHeader :show-profile="false" :show-sign-out="false" :show-notifications="false">
      <template #rightActions>
        <div class="hidden xl:flex items-center gap-3 mr-2 text-xs font-medium text-emerald-600 dark:text-emerald-400">
          <span class="material-symbols-outlined text-sm leading-none">videocam</span>
          <span>Camera bật</span>
          <span class="w-1 h-1 rounded-full bg-slate-300 dark:bg-slate-700"></span>
          <span class="material-symbols-outlined text-sm leading-none">wifi</span>
          <span>Đã kết nối</span>
        </div>
        <div class="flex items-center gap-3 bg-slate-100 dark:bg-slate-800 px-4 py-2 rounded-xl border border-slate-200 dark:border-slate-700">
          <div class="flex flex-col items-center leading-none">
            <span class="text-sm font-bold tabular-nums">{{ timerHours }}</span>
            <span class="text-[9px] uppercase tracking-wider opacity-60">Giờ</span>
          </div>
          <span class="text-base font-light opacity-30">:</span>
          <div class="flex flex-col items-center leading-none">
            <span class="text-sm font-bold tabular-nums">{{ timerMinutes }}</span>
            <span class="text-[9px] uppercase tracking-wider opacity-60">Phút</span>
          </div>
          <span class="text-base font-light opacity-30">:</span>
          <div class="flex flex-col items-center leading-none">
            <span class="text-sm font-bold tabular-nums">{{ timerSeconds }}</span>
            <span class="text-[9px] uppercase tracking-wider opacity-60">Giây</span>
          </div>
        </div>
        <button :disabled="isSuspended" @click="openSubmitModal" class="bg-primary hover:bg-primary/90 text-white px-4 py-2 rounded-xl font-bold text-xs transition-colors shadow-lg shadow-primary/20 disabled:opacity-60" type="button">
          Nộp bài
        </button>
      </template>
    </StudentTopHeader>

    <main class="relative flex-1 flex max-w-[1440px] mx-auto w-full p-6 gap-6 overflow-hidden">
      <div v-if="isSuspended" class="absolute inset-0 z-20 bg-black/60 backdrop-blur-[1px] flex items-center justify-center px-4">
        <div class="max-w-lg w-full rounded-2xl border border-rose-300 bg-white p-6 text-center shadow-2xl">
          <h2 class="text-2xl font-bold text-rose-700 mb-2">Bài thi đã bị đình chỉ</h2>
          <p class="text-sm text-slate-600">{{ suspensionMessage || 'Giám thị đã hủy hiệu lực bài thi của bạn.' }}</p>
        </div>
      </div>
      <div class="pointer-events-none absolute -top-16 -left-16 size-72 rounded-full bg-primary/15 blur-3xl animate-float-slow"></div>
      <div class="pointer-events-none absolute -bottom-24 -right-12 size-80 rounded-full bg-primary/10 blur-3xl animate-float-delay"></div>

      <div class="relative flex-1 flex flex-col gap-6">
        <div v-if="loadError" class="rounded-xl border border-rose-200 bg-rose-50 text-rose-700 px-4 py-3 text-sm">
          {{ loadError }}
        </div>
        <div v-if="realtimeWarningMessage" class="rounded-xl border border-amber-200 bg-amber-50 text-amber-700 px-4 py-3 text-sm">
          {{ realtimeWarningMessage }}
        </div>

        <div v-if="currentQuestion" class="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-2xl p-8 shadow-sm flex-1 animate-fade-up">
          <div class="flex justify-between items-center mb-8">
            <span class="bg-primary/10 text-primary px-3 py-1 rounded-full text-xs font-bold uppercase tracking-wider">Câu {{ currentIndex + 1 }} / {{ questions.length }}</span>
          </div>
          <div class="prose dark:prose-invert max-w-none">
            <h2 class="text-2xl font-semibold leading-relaxed mb-8">
              {{ currentQuestion.content }}
            </h2>
          </div>

          <div class="space-y-4">
            <label
              v-for="option in currentQuestion.options"
              :key="option.id"
              :class="answers[currentQuestion.id] === option.id ? 'border-primary bg-primary/5 dark:bg-primary/10' : 'border-slate-100 dark:border-slate-800 hover:border-primary/50 bg-slate-50/50 dark:bg-slate-800/30'"
              class="group relative flex items-center p-5 rounded-xl border-2 cursor-pointer transition-all"
            >
              <input
                :checked="answers[currentQuestion.id] === option.id"
                :disabled="isSuspended"
                class="w-5 h-5 text-primary focus:ring-primary border-slate-300 dark:border-slate-600 bg-white dark:bg-slate-900"
                :name="`exam-option-${currentQuestion.id}`"
                type="radio"
                @change="onSelectAnswer(currentQuestion.id, option.id)"
              />
              <span class="ml-4 text-lg font-medium">{{ option.text }}</span>
            </label>
          </div>
        </div>

        <div class="flex items-center justify-between py-2 animate-fade-up-delay">
          <button @click="goPrevious" :disabled="currentIndex === 0" class="flex items-center gap-2 px-6 py-3 rounded-xl border border-slate-200 dark:border-slate-800 font-bold hover:bg-slate-100 dark:hover:bg-slate-800 hover:-translate-y-0.5 hover:shadow-md transition-all duration-200 disabled:opacity-50 disabled:hover:translate-y-0" type="button">
            <span class="material-symbols-outlined">arrow_back</span>
            Câu trước
          </button>
          <div class="flex gap-4">
            <button @click="goNext" :disabled="currentIndex >= questions.length - 1" class="flex items-center gap-2 px-6 py-3 rounded-xl bg-slate-900 dark:bg-slate-100 text-white dark:text-slate-900 font-bold hover:opacity-90 hover:-translate-y-0.5 hover:shadow-md transition-all duration-200 disabled:opacity-50 disabled:hover:translate-y-0" type="button">
              Câu tiếp theo
              <span class="material-symbols-outlined">arrow_forward</span>
            </button>
          </div>
        </div>
      </div>

      <aside class="relative w-80 flex flex-col gap-6 animate-fade-up-delay">
        <div class="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-2xl p-5 shadow-sm">
          <div class="flex justify-between items-end mb-3">
            <h3 class="font-bold text-sm">Tiến độ làm bài</h3>
            <span class="text-sm font-bold text-primary">{{ progressPercent }}%</span>
          </div>
          <div class="w-full bg-slate-100 dark:bg-slate-800 h-2 rounded-full overflow-hidden">
            <div class="bg-primary h-full rounded-full" :style="{ width: `${progressPercent}%` }"></div>
          </div>
          <div class="mt-4 grid grid-cols-2 gap-3">
            <div class="flex items-center gap-2 text-xs font-medium text-slate-500">
              <div class="w-2 h-2 rounded-full bg-primary"></div>
              Đã làm ({{ answeredCount }})
            </div>
            <div class="flex items-center gap-2 text-xs font-medium text-slate-500">
              <div class="w-2 h-2 rounded-full bg-slate-200 dark:bg-slate-700"></div>
              Chưa làm ({{ unansweredCount }})
            </div>
          </div>
        </div>

        <div class="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-2xl p-5 shadow-sm flex-1 overflow-y-auto">
          <div class="flex items-center justify-between mb-4">
            <h3 class="font-bold text-sm">Danh sách câu hỏi</h3>
            <span class="text-xs text-slate-500 font-medium">Tổng {{ questions.length }} câu</span>
          </div>
          <div class="grid grid-cols-5 gap-2">
            <button
              v-for="(question, idx) in questions"
              :key="question.id"
              :class="questionButtonClass(idx)"
              class="aspect-square flex items-center justify-center rounded-lg text-xs font-bold relative"
              type="button"
              @click="selectQuestion(idx)"
            >
              {{ idx + 1 }}
            </button>
          </div>
        </div>
      </aside>
    </main>

    <footer class="p-4 border-t border-slate-200 dark:border-slate-800 text-center">
      <p class="text-xs text-slate-400 font-medium">
        Cần hỗ trợ? Liên hệ bộ phận thi tại <span class="text-primary cursor-pointer hover:underline">support@edu-portal.com</span>
      </p>
    </footer>

    <div v-if="showSubmitModal" class="fixed inset-0 z-[60] flex items-center justify-center bg-black/50 px-4">
      <div class="w-full max-w-md rounded-2xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-700 shadow-xl p-6">
        <div class="flex items-start gap-3 mb-4">
          <span class="material-symbols-outlined text-primary text-3xl">help</span>
          <div>
            <h3 class="text-lg font-bold">Xác nhận nộp bài?</h3>
            <p class="text-sm text-slate-500 dark:text-slate-400 mt-1">Bạn sẽ không thể thay đổi đáp án sau khi nộp.</p>
          </div>
        </div>

        <div v-if="unansweredCount > 0" class="mb-5 rounded-xl bg-amber-50 dark:bg-amber-900/20 border border-amber-200 dark:border-amber-700 p-3">
          <p class="text-sm font-semibold text-amber-700 dark:text-amber-300">
            Bạn còn {{ unansweredCount }} câu chưa làm.
          </p>
        </div>

        <div class="flex justify-end gap-3">
          <button @click="closeSubmitModal" class="px-4 py-2 rounded-lg border border-slate-200 dark:border-slate-700 font-semibold hover:bg-slate-50 dark:hover:bg-slate-800" type="button">
            Quay lại làm bài
          </button>
          <button :disabled="isSubmitting" @click="submitExamAction" class="px-4 py-2 rounded-lg bg-primary text-white font-semibold hover:bg-primary/90 disabled:opacity-70" type="button">
            {{ isSubmitting ? 'Đang nộp...' : 'Xác nhận nộp' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { Client } from '@stomp/stompjs'
import { API_BASE_URL, ApiError } from '../../services/apiClient'
import { getAttemptDetail, getDraftAnswers, saveDraftAnswers, submitAttempt } from '../../services/attemptService'
import { sendMonitoringEvent } from '../../services/monitoringService'
import { listExamQuestions, parseQuestionOptions } from '../../services/questionService'
import { useRoute, useRouter } from 'vue-router'
import SockJS from 'sockjs-client/dist/sockjs'
import StudentTopHeader from './StudentTopHeader.vue'

const route = useRoute()
const router = useRouter()
const isDark = ref(false)

const examTitle = computed(() => route.query.exam || 'Giữa kỳ Tâm lý học nâng cao')
const examId = computed(() => Number.parseInt(String(route.query.examId || ''), 10) || null)
const attemptId = computed(() => Number.parseInt(String(route.query.attemptId || ''), 10) || null)
const showSubmitModal = ref(false)
const isSubmitting = ref(false)
const loadError = ref('')
const questions = ref([])
const answers = ref({})
const currentIndex = ref(0)
const remainingSeconds = ref(Number.parseInt(String(route.query.remainingSeconds || ''), 10) || 0)
const attemptStatus = ref('IN_PROGRESS')
const isSuspended = ref(false)
const suspensionMessage = ref('')
const realtimeWarningMessage = ref('')
const lastViolationAtByType = ref({})
const pendingViolationByType = ref({})
let timerId = null
let blurGraceTimer = null
let attemptStatusTimer = null
let stompClient = null

const VIOLATION_COOLDOWN_MS = 7000
const BLUR_GRACE_MS = 1200

const currentQuestion = computed(() => questions.value[currentIndex.value] || null)
const answeredCount = computed(() => Object.values(answers.value).filter(Boolean).length)
const unansweredCount = computed(() => Math.max(questions.value.length - answeredCount.value, 0))
const progressPercent = computed(() => {
  if (!questions.value.length) return 0
  return Math.round((answeredCount.value / questions.value.length) * 100)
})
const timerHours = computed(() => String(Math.floor(remainingSeconds.value / 3600)).padStart(2, '0'))
const timerMinutes = computed(() => String(Math.floor((remainingSeconds.value % 3600) / 60)).padStart(2, '0'))
const timerSeconds = computed(() => String(remainingSeconds.value % 60).padStart(2, '0'))

const clearBlurGraceTimer = () => {
  if (blurGraceTimer) {
    window.clearTimeout(blurGraceTimer)
    blurGraceTimer = null
  }
}

const reportViolation = async (eventType, details) => {
  if (!attemptId.value || isSuspended.value) return

  const now = Date.now()
  const lastAt = lastViolationAtByType.value[eventType] || 0
  if (now - lastAt < VIOLATION_COOLDOWN_MS) return

  lastViolationAtByType.value = {
    ...lastViolationAtByType.value,
    [eventType]: now
  }

  try {
    await sendMonitoringEvent(attemptId.value, eventType, details)
  } catch {
    // ignore monitoring send failures and keep exam flow stable
  }
}

const applyAttemptStatus = (status, message = '') => {
  const normalized = String(status || '').toUpperCase() || 'IN_PROGRESS'
  attemptStatus.value = normalized

  if (normalized === 'STOPPED') {
    isSuspended.value = true
    if (message) {
      suspensionMessage.value = message
    }
    showSubmitModal.value = false
    return
  }

  isSuspended.value = false
}

const syncAttemptStatus = async () => {
  if (!attemptId.value) return
  try {
    const detail = await getAttemptDetail(attemptId.value)
    applyAttemptStatus(detail?.status || 'IN_PROGRESS')
  } catch {
    // ignore status sync errors
  }
}

const connectProctorRealtime = () => {
  if (!attemptId.value) return

  const wsUrl = `${API_BASE_URL.replace(/\/$/, '')}/ws`
  stompClient = new Client({
    reconnectDelay: 5000,
    webSocketFactory: () => new SockJS(wsUrl)
  })

  stompClient.onConnect = () => {
    stompClient.subscribe(`/topic/attempts/${attemptId.value}/proctor-actions`, (frame) => {
      try {
        const payload = JSON.parse(frame.body || '{}')
        const type = String(payload.type || '').toUpperCase()
        if (type === 'TEACHER_WARNING') {
          realtimeWarningMessage.value = payload.message || 'Giám thị vừa gửi cảnh báo.'
        }
        if (type === 'ATTEMPT_STOPPED') {
          applyAttemptStatus(payload.status || 'STOPPED', payload.message || 'Bài thi đã bị đình chỉ.')
        }
      } catch {
        // ignore malformed realtime payload
      }
    })
  }

  stompClient.activate()
}

const handleVisibilityChange = () => {
  if (document.hidden) {
    pendingViolationByType.value = {
      ...pendingViolationByType.value,
      TAB_SWITCH: true
    }
    void reportViolation('TAB_SWITCH', 'Document hidden during exam attempt')
  } else if (pendingViolationByType.value.TAB_SWITCH) {
    pendingViolationByType.value = {
      ...pendingViolationByType.value,
      TAB_SWITCH: false
    }
  }
}

const handleWindowBlur = () => {
  clearBlurGraceTimer()
  blurGraceTimer = window.setTimeout(() => {
    pendingViolationByType.value = {
      ...pendingViolationByType.value,
      BLUR: true
    }
    void reportViolation('BLUR', 'Window lost focus during exam attempt')
  }, BLUR_GRACE_MS)
}

const handleWindowFocus = () => {
  clearBlurGraceTimer()
  if (pendingViolationByType.value.BLUR) {
    pendingViolationByType.value = {
      ...pendingViolationByType.value,
      BLUR: false
    }
  }
}

const handleFullscreenChange = () => {
  const inFullscreen = Boolean(document.fullscreenElement)
  if (inFullscreen) {
    pendingViolationByType.value = {
      ...pendingViolationByType.value,
      EXIT_FULLSCREEN: false
    }
    return
  }

  pendingViolationByType.value = {
    ...pendingViolationByType.value,
    EXIT_FULLSCREEN: true
  }
  void reportViolation('EXIT_FULLSCREEN', 'Exited fullscreen during exam attempt')
}

const selectQuestion = (index) => {
  if (isSuspended.value) return
  currentIndex.value = index
}

const goPrevious = () => {
  if (isSuspended.value) return
  if (currentIndex.value > 0) currentIndex.value -= 1
}

const goNext = () => {
  if (isSuspended.value) return
  if (currentIndex.value < questions.value.length - 1) currentIndex.value += 1
}

const persistDraft = async () => {
  if (!attemptId.value || isSuspended.value) return
  const payload = Object.entries(answers.value).map(([questionId, selectedAnswer]) => ({
    questionId: Number(questionId),
    selectedAnswer
  }))
  if (!payload.length) return
  await saveDraftAnswers(attemptId.value, payload)
}

const onSelectAnswer = async (questionId, selectedAnswer) => {
  if (isSuspended.value) return

  answers.value = {
    ...answers.value,
    [questionId]: selectedAnswer
  }

  try {
    await persistDraft()
  } catch {
    // keep local state
  }
}

const openSubmitModal = () => {
  if (isSuspended.value) return
  showSubmitModal.value = true
}

const closeSubmitModal = () => {
  showSubmitModal.value = false
}

const submitExamAction = async () => {
  if (!attemptId.value || isSuspended.value) return

  isSubmitting.value = true
  try {
    const payload = Object.entries(answers.value)
      .filter(([, selectedAnswer]) => Boolean(selectedAnswer))
      .map(([questionId, selectedAnswer]) => ({
        questionId: Number(questionId),
        selectedAnswer
      }))

    const result = await submitAttempt(attemptId.value, payload)
    showSubmitModal.value = false
    router.push({
      path: '/student/submission-confirmation',
      query: {
        exam: examTitle.value,
        attemptId: attemptId.value,
        score: Math.round(Number(result?.score || 0)),
        submittedAt: result?.submittedAt || ''
      }
    })
  } catch (error) {
    loadError.value = error instanceof ApiError ? error.message : 'Không thể nộp bài lúc này.'
  } finally {
    isSubmitting.value = false
  }
}

const questionButtonClass = (index) => {
  const question = questions.value[index]
  if (!question) return 'bg-slate-100 dark:bg-slate-800 text-slate-400'

  if (index === currentIndex.value) {
    return 'border-2 border-primary text-primary shadow-inner'
  }

  if (answers.value[question.id]) {
    return 'bg-primary text-white'
  }

  return 'bg-slate-100 dark:bg-slate-800 text-slate-400'
}

onMounted(async () => {
  loadError.value = ''
  try {
    if (!examId.value || !attemptId.value) {
      loadError.value = 'Thiếu thông tin bài thi/lượt làm bài.'
      return
    }

    const [questionList, draftData] = await Promise.all([
      listExamQuestions(examId.value),
      getDraftAnswers(attemptId.value)
    ])

    questions.value = questionList.map((item) => ({
      id: item.id,
      content: item.content,
      options: parseQuestionOptions(item.options)
    }))

    answers.value = (draftData?.answers || []).reduce((acc, answer) => {
      acc[answer.questionId] = answer.selectedAnswer
      return acc
    }, {})

    applyAttemptStatus(draftData?.status || 'IN_PROGRESS')

    if (remainingSeconds.value <= 0 && route.query.deadlineAt) {
      const deadlineMs = new Date(String(route.query.deadlineAt)).getTime()
      remainingSeconds.value = Math.max(0, Math.floor((deadlineMs - Date.now()) / 1000))
    }

    timerId = window.setInterval(() => {
      if (remainingSeconds.value > 0) remainingSeconds.value -= 1
    }, 1000)

    connectProctorRealtime()
    attemptStatusTimer = window.setInterval(() => {
      syncAttemptStatus()
    }, 5000)

    document.addEventListener('visibilitychange', handleVisibilityChange)
    window.addEventListener('blur', handleWindowBlur)
    window.addEventListener('focus', handleWindowFocus)
    document.addEventListener('fullscreenchange', handleFullscreenChange)
  } catch (error) {
    loadError.value = error instanceof ApiError ? error.message : 'Không thể tải nội dung bài thi.'
  }
})

onUnmounted(() => {
  if (timerId) window.clearInterval(timerId)
  if (attemptStatusTimer) window.clearInterval(attemptStatusTimer)
  clearBlurGraceTimer()
  if (stompClient) {
    stompClient.deactivate()
    stompClient = null
  }
  document.removeEventListener('visibilitychange', handleVisibilityChange)
  window.removeEventListener('blur', handleWindowBlur)
  window.removeEventListener('focus', handleWindowFocus)
  document.removeEventListener('fullscreenchange', handleFullscreenChange)
})
</script>

<style scoped>
.font-display {
  font-family: 'Public Sans', sans-serif;
}

@keyframes fadeUp {
  from {
    opacity: 0;
    transform: translateY(18px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes floatSlow {
  0%,
  100% {
    transform: translate3d(0, 0, 0);
  }
  50% {
    transform: translate3d(0, -14px, 0);
  }
}

@keyframes floatDelay {
  0%,
  100% {
    transform: translate3d(0, 0, 0);
  }
  50% {
    transform: translate3d(0, 12px, 0);
  }
}

.animate-fade-up {
  animation: fadeUp 0.5s ease-out;
}

.animate-fade-up-delay {
  animation: fadeUp 0.65s ease-out;
}

.animate-float-slow {
  animation: floatSlow 7s ease-in-out infinite;
}

.animate-float-delay {
  animation: floatDelay 8s ease-in-out infinite;
}
</style>
