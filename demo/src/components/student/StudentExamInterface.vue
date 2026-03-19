<template>
  <div
    :class="isDark ? 'dark' : 'light'"
    class="bg-background-light dark:bg-background-dark text-slate-900 dark:text-slate-100 min-h-screen flex flex-col font-display"
    @contextmenu="handleRightClick"
  >
    <StudentTopHeader :show-profile="false" :show-sign-out="false" :show-notifications="false" :show-menu="false">
      <template #rightActions>
        <div class="hidden xl:flex items-center gap-3 mr-2 text-xs font-medium">
          <span :class="cameraReady ? 'text-emerald-600 dark:text-emerald-400' : 'text-rose-500'" class="material-symbols-outlined text-sm leading-none">videocam</span>
          <span :class="cameraReady ? 'text-emerald-600 dark:text-emerald-400' : 'text-rose-500'">{{ cameraReady ? 'Camera bật' : 'Camera tắt' }}</span>
          <span :class="micReady ? 'bg-emerald-400' : 'bg-rose-500'" class="w-1 h-1 rounded-full"></span>
          <span :class="micReady ? 'text-emerald-600 dark:text-emerald-400' : 'text-rose-500'" class="material-symbols-outlined text-sm leading-none">mic</span>
          <span :class="micReady ? 'text-emerald-600 dark:text-emerald-400' : 'text-rose-500'">{{ micReady ? 'Mic bật' : 'Mic tắt' }}</span>
          <span class="w-1 h-1 rounded-full bg-slate-300 dark:bg-slate-700"></span>
          <span class="material-symbols-outlined text-sm leading-none text-slate-500">wifi</span>
          <span class="text-slate-500">Đã kết nối</span>
        </div>
        <div class="flex items-center gap-3 bg-slate-100 dark:bg-slate-800 px-4 py-2.5 rounded-xl border border-slate-200 dark:border-slate-700 shadow-sm">
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

    <main class="relative flex-1 flex flex-col lg:flex-row max-w-[1440px] mx-auto w-full p-4 sm:p-6 gap-6 overflow-hidden">
      <div v-if="isSuspended" class="absolute inset-0 z-20 bg-black/60 backdrop-blur-[1px] flex items-center justify-center px-4">
        <div class="max-w-lg w-full rounded-2xl border border-rose-300 bg-white p-6 text-center shadow-2xl">
          <h2 class="text-2xl font-bold text-rose-700 mb-2">Bài thi đã bị đình chỉ</h2>
          <p class="text-sm text-slate-600">{{ suspensionMessage || 'Giám thị đã hủy hiệu lực bài thi của bạn.' }}</p>
        </div>
      </div>

      <!-- Modal cảnh báo từ giám thị -->
      <div v-if="showTeacherWarningModal" class="fixed inset-0 z-[70] flex items-center justify-center p-4 bg-amber-900/40 backdrop-blur-sm" @click.self="showTeacherWarningModal = false">
        <div class="max-w-lg w-full rounded-2xl border-2 border-amber-400 bg-white dark:bg-slate-900 p-6 shadow-2xl animate-fade-up ring-4 ring-amber-500/30">
          <div class="flex items-center gap-4 mb-4">
            <div class="size-14 rounded-2xl bg-amber-100 dark:bg-amber-900/40 flex items-center justify-center shrink-0">
              <span class="material-symbols-outlined text-amber-600 dark:text-amber-400 text-3xl">warning</span>
            </div>
            <div>
              <h2 class="text-xl font-bold text-amber-800 dark:text-amber-200">Cảnh báo từ giám thị</h2>
              <p class="text-sm text-slate-600 dark:text-slate-400 mt-1">Vui lòng chú ý và tuân thủ quy định phòng thi.</p>
            </div>
          </div>
          <div class="rounded-xl bg-amber-50 dark:bg-amber-900/20 border border-amber-200 dark:border-amber-800 p-4 mb-6">
            <p class="text-slate-800 dark:text-slate-200 font-medium">{{ teacherWarningMessage }}</p>
          </div>
          <button type="button" class="w-full py-3 rounded-xl bg-amber-500 hover:bg-amber-600 text-white font-bold flex items-center justify-center gap-2" @click="showTeacherWarningModal = false">
            <span class="material-symbols-outlined">check_circle</span>
            Tôi đã hiểu
          </button>
        </div>
      </div>
      <div class="pointer-events-none absolute -top-16 -left-16 size-72 rounded-full bg-primary/15 blur-3xl animate-float-slow"></div>
      <div class="pointer-events-none absolute -bottom-24 -right-12 size-80 rounded-full bg-primary/10 blur-3xl animate-float-delay"></div>

      <div class="relative flex-1 flex flex-col gap-6 min-w-0">

        <div v-if="currentQuestion" class="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-2xl p-6 sm:p-8 shadow-lg flex-1 animate-fade-up">
          <div class="flex justify-between items-center mb-6">
            <span class="inline-flex items-center gap-2 bg-primary/10 text-primary px-4 py-2 rounded-xl text-sm font-bold">
              <span class="material-symbols-outlined text-base">quiz</span>
              Câu {{ currentIndex + 1 }} / {{ questions.length }}
            </span>
          </div>
          <div class="prose dark:prose-invert max-w-none mb-8">
            <h2 class="text-xl sm:text-2xl font-semibold leading-relaxed text-slate-900 dark:text-slate-100">
              {{ currentQuestion.content }}
            </h2>
          </div>

          <div class="space-y-3">
            <label
              v-for="option in currentQuestion.options"
              :key="option.id"
              :class="answers[currentQuestion.id] === option.id ? 'border-primary bg-primary/5 dark:bg-primary/10 ring-2 ring-primary/30' : 'border-slate-200 dark:border-slate-700 hover:border-primary/40 bg-slate-50/50 dark:bg-slate-800/30'"
              class="group relative flex items-center p-4 sm:p-5 rounded-xl border-2 cursor-pointer transition-all duration-200"
            >
              <input
                :checked="answers[currentQuestion.id] === option.id"
                :disabled="isSuspended"
                class="w-5 h-5 text-primary focus:ring-2 focus:ring-primary/50 border-slate-300 dark:border-slate-600 bg-white dark:bg-slate-900 shrink-0"
                :name="`exam-option-${currentQuestion.id}`"
                type="radio"
                @change="onSelectAnswer(currentQuestion.id, option.id)"
              />
              <span class="ml-4 text-base sm:text-lg font-medium text-slate-800 dark:text-slate-200">{{ option.text }}</span>
            </label>
          </div>
        </div>

        <div class="flex flex-col-reverse sm:flex-row items-stretch sm:items-center justify-between gap-3 py-4 animate-fade-up-delay">
          <button @click="goPrevious" :disabled="currentIndex === 0" class="flex items-center justify-center gap-2 px-5 py-3 rounded-xl border-2 border-slate-200 dark:border-slate-700 font-bold text-slate-700 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800 hover:border-slate-300 dark:hover:border-slate-600 transition-all duration-200 disabled:opacity-40 disabled:cursor-not-allowed disabled:hover:bg-transparent" type="button">
            <span class="material-symbols-outlined text-xl">arrow_back</span>
            Câu trước
          </button>
          <button @click="goNext" :disabled="currentIndex >= questions.length - 1" class="flex-1 sm:flex-initial flex items-center justify-center gap-2 px-6 py-3 rounded-xl bg-primary hover:bg-primary/90 text-white font-bold shadow-lg shadow-primary/25 hover:shadow-primary/30 transition-all duration-200 disabled:opacity-40 disabled:cursor-not-allowed disabled:shadow-none" type="button">
            Câu tiếp theo
            <span class="material-symbols-outlined text-xl">arrow_forward</span>
          </button>
        </div>
      </div>

      <aside class="relative w-full lg:w-80 shrink-0 flex flex-col gap-6 animate-fade-up-delay">
        <div v-if="shouldCheckDevices && mediaStreamRef" class="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-2xl overflow-hidden shadow-sm">
          <div class="px-3 py-2 border-b border-slate-200 dark:border-slate-800 flex items-center justify-between gap-2">
            <span class="text-xs font-bold text-slate-700 dark:text-slate-300">Camera của bạn</span>
            <div class="flex items-center gap-1">
              <button
                :disabled="isSuspended"
                :title="cameraReady ? 'Tắt camera' : 'Bật camera'"
                :class="cameraReady ? 'bg-primary/15 text-primary hover:bg-primary/25' : 'bg-slate-200 dark:bg-slate-700 text-slate-500 hover:bg-slate-300 dark:hover:bg-slate-600'"
                class="p-2 rounded-lg transition-all disabled:opacity-50"
                type="button"
                @click="toggleCamera"
              >
                <span class="material-symbols-outlined text-lg">{{ cameraReady ? 'videocam' : 'videocam_off' }}</span>
              </button>
              <button
                :disabled="isSuspended"
                :title="micReady ? 'Tắt micro' : 'Bật micro'"
                :class="micReady ? 'bg-primary/15 text-primary hover:bg-primary/25' : 'bg-slate-200 dark:bg-slate-700 text-slate-500 hover:bg-slate-300 dark:hover:bg-slate-600'"
                class="p-2 rounded-lg transition-all disabled:opacity-50"
                type="button"
                @click="toggleMic"
              >
                <span class="material-symbols-outlined text-lg">{{ micReady ? 'mic' : 'mic_off' }}</span>
              </button>
            </div>
          </div>
          <div class="relative aspect-video bg-slate-900">
            <video ref="cameraPreviewRef" autoplay playsinline muted class="w-full h-full object-cover" :class="{ 'opacity-0': !cameraReady }" />
            <div v-if="!cameraReady" class="absolute inset-0 flex items-center justify-center bg-slate-800/90">
              <span class="material-symbols-outlined text-4xl text-slate-500">videocam_off</span>
            </div>
          </div>
        </div>
        <div class="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-2xl p-5 shadow-sm">
          <div class="flex justify-between items-center mb-3">
            <h3 class="font-bold text-sm text-slate-800 dark:text-slate-200 flex items-center gap-2">
              <span class="material-symbols-outlined text-primary text-lg">trending_up</span>
              Tiến độ làm bài
            </h3>
            <span class="text-lg font-bold text-primary tabular-nums">{{ progressPercent }}%</span>
          </div>
          <div class="w-full bg-slate-100 dark:bg-slate-800 h-2.5 rounded-full overflow-hidden">
            <div class="bg-primary h-full rounded-full transition-all duration-500" :style="{ width: `${progressPercent}%` }"></div>
          </div>
          <div class="mt-3 flex items-center justify-between text-xs text-slate-500 dark:text-slate-400">
            <span class="flex items-center gap-1.5"><span class="w-2 h-2 rounded-full bg-primary"></span> Đã làm: {{ answeredCount }}</span>
            <span class="flex items-center gap-1.5"><span class="w-2 h-2 rounded-full bg-slate-300 dark:bg-slate-600"></span> Chưa làm: {{ unansweredCount }}</span>
          </div>
        </div>

        <div class="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-2xl p-5 shadow-sm flex-1 overflow-y-auto min-h-0">
          <div class="flex items-center justify-between mb-4">
            <h3 class="font-bold text-sm text-slate-800 dark:text-slate-200 flex items-center gap-2">
              <span class="material-symbols-outlined text-primary text-lg">list</span>
              Danh sách câu hỏi
            </h3>
            <span class="text-xs text-slate-500 font-medium">{{ questions.length }} câu</span>
          </div>
          <div class="grid grid-cols-5 gap-2">
            <button
              v-for="(question, idx) in questions"
              :key="question.id"
              :class="questionButtonClass(idx)"
              class="aspect-square flex items-center justify-center rounded-xl text-xs font-bold transition-all duration-200 hover:scale-105"
              type="button"
              @click="selectQuestion(idx)"
            >
              {{ idx + 1 }}
            </button>
          </div>
        </div>
      </aside>
    </main>

    <footer class="py-3 px-4 border-t border-slate-200 dark:border-slate-800 bg-slate-50/50 dark:bg-slate-900/30 text-center">
      <p class="text-xs text-slate-500 dark:text-slate-400">
        Cần hỗ trợ? Liên hệ <span class="text-primary font-medium cursor-pointer hover:underline">support@edu-portal.com</span>
      </p>
    </footer>

    <div v-if="showSubmitModal" class="modal-overlay z-[60]" role="dialog" aria-modal="true" aria-labelledby="submit-exam-title" @click.self="closeSubmitModal">
      <div class="modal-content w-full max-w-md">
        <div class="modal-header">
          <div class="flex items-center gap-3">
            <div class="size-10 rounded-xl bg-indigo-100 dark:bg-indigo-500/20 flex items-center justify-center">
              <span class="material-symbols-outlined text-indigo-600 dark:text-indigo-400 text-xl">help</span>
            </div>
            <div>
              <h3 id="submit-exam-title" class="text-lg font-bold text-slate-900 dark:text-slate-100">Xác nhận nộp bài?</h3>
              <p class="text-sm text-slate-500 dark:text-slate-400 mt-0.5">Bạn sẽ không thể thay đổi đáp án sau khi nộp.</p>
            </div>
          </div>
          <button type="button" class="modal-close-btn" aria-label="Đóng" @click="closeSubmitModal">
            <span class="material-symbols-outlined">close</span>
          </button>
        </div>
        <div class="modal-body">
          <div v-if="unansweredCount > 0" class="rounded-xl bg-amber-50 dark:bg-amber-900/20 border border-amber-200 dark:border-amber-700 p-4 mb-4">
            <p class="text-sm font-semibold text-amber-700 dark:text-amber-300 flex items-center gap-2">
              <span class="material-symbols-outlined text-lg">warning</span>
              Bạn còn {{ unansweredCount }} câu chưa làm.
            </p>
          </div>
        </div>
        <div class="modal-footer">
          <button @click="closeSubmitModal" class="px-4 py-2.5 rounded-xl border border-slate-200 dark:border-slate-600 text-slate-700 dark:text-slate-300 font-semibold hover:bg-slate-50 dark:hover:bg-slate-800 transition-colors" type="button">
            Quay lại làm bài
          </button>
          <button id="submit-exam-confirm" :disabled="isSubmitting" @click="submitExamAction" class="px-4 py-2.5 rounded-xl bg-primary text-white font-semibold hover:bg-primary/90 disabled:opacity-70 flex items-center gap-2 transition-colors" type="button">
            <span class="material-symbols-outlined text-lg" v-if="isSubmitting">hourglass_empty</span>
            {{ isSubmitting ? 'Đang nộp...' : 'Xác nhận nộp' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { API_BASE_URL } from '../../services/apiClient'
import { getAttemptDetail, getDraftAnswers, saveDraftAnswers, submitAttempt } from '../../services/attemptService'
import { sendMonitoringEvent, updateDeviceStatus } from '../../services/monitoringService'
import { listExamQuestions, parseQuestionOptions } from '../../services/questionService'
import { useToast } from '../../composables/useToast'
import { useRoute, useRouter } from 'vue-router'
import StudentTopHeader from './StudentTopHeader.vue'

const route = useRoute()
const router = useRouter()
const isDark = ref(false)

const examTitle = computed(() => route.query.exam || 'Giữa kỳ Tâm lý học nâng cao')
const examId = computed(() => Number.parseInt(String(route.query.examId || ''), 10) || null)
const attemptId = computed(() => Number.parseInt(String(route.query.attemptId || ''), 10) || null)
const isPracticeExam = computed(() => String(route.query.isPractice || '') === 'true')
const examConfig = ref({
  monitorTabSwitch: true,
  monitorBlur: true,
  monitorExitFullscreen: true,
  monitorCopyPaste: true,
  monitorIdleTime: true,
  monitorDevtools: true,
  monitorDuplicateIp: true,
  monitorFastSubmit: true,
  monitorRightClick: true,
  monitorPrintScreen: true,
  monitorRapidQuestionSwitch: true,
  monitorMultiMonitor: true,
  requireCameraMic: true
})
const showSubmitModal = ref(false)
const isSubmitting = ref(false)
const questions = ref([])
const answers = ref({})
const currentIndex = ref(0)
const remainingSeconds = ref(Number.parseInt(String(route.query.remainingSeconds || ''), 10) || 0)

const toast = useToast()
const attemptStatus = ref('IN_PROGRESS')
const teacherWarningMessage = ref('')
const showTeacherWarningModal = ref(false)
const isSuspended = ref(false)
const suspensionMessage = ref('')
const lastViolationAtByType = ref({})
const pendingViolationByType = ref({})
const questionSwitchTimestamps = ref([])
const cameraReady = ref(false)
const micReady = ref(false)
const deviceError = ref('')
const isCheckingDevices = ref(false)
const mediaStreamRef = ref(null)
const cameraPreviewRef = ref(null)
let deviceStatusInterval = null
let timerId = null
let blurGraceTimer = null
let attemptStatusTimer = null
let stompClient = null
let idleTimer = null
let devtoolsCheckTimer = null
let blockBackHandler = null

const VIOLATION_COOLDOWN_MS = 7000
const LONG_VIOLATION_COOLDOWN_MS = 60000
const BLUR_GRACE_MS = 1200
const IDLE_THRESHOLD_MS = 3 * 60 * 1000
const DEVTOOLS_GAP_PX = 160

const currentQuestion = computed(() => questions.value[currentIndex.value] || null)
const devicesReady = computed(() => cameraReady.value && micReady.value)
const shouldCheckDevices = computed(() => !isPracticeExam.value && examConfig.value.requireCameraMic !== false)
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

const syncDeviceStatusToBackend = async () => {
  if (!attemptId.value || isPracticeExam.value || isSuspended.value) return
  try {
    await updateDeviceStatus(attemptId.value, cameraReady.value, micReady.value)
  } catch {
    // ignore
  }
}

const toggleCamera = () => {
  const stream = mediaStreamRef.value
  const videoTrack = stream?.getVideoTracks()[0]
  if (!videoTrack) return
  videoTrack.enabled = !videoTrack.enabled
  cameraReady.value = videoTrack.enabled
  void syncDeviceStatusToBackend()
}

const toggleMic = () => {
  const stream = mediaStreamRef.value
  const audioTrack = stream?.getAudioTracks()[0]
  if (!audioTrack) return
  audioTrack.enabled = !audioTrack.enabled
  micReady.value = audioTrack.enabled
  void syncDeviceStatusToBackend()
}

const checkDevices = async () => {
  if (!shouldCheckDevices.value) {
    cameraReady.value = true
    micReady.value = true
    deviceError.value = ''
    return
  }
  if (!navigator?.mediaDevices?.getUserMedia) {
    cameraReady.value = false
    micReady.value = false
    deviceError.value = 'Trình duyệt không hỗ trợ kiểm tra camera/micro.'
    return
  }

  isCheckingDevices.value = true
  deviceError.value = ''
  try {
    const stream = await navigator.mediaDevices.getUserMedia({ video: true, audio: true })
    const videoTrack = stream.getVideoTracks()[0]
    const audioTrack = stream.getAudioTracks()[0]
    cameraReady.value = Boolean(videoTrack?.enabled)
    micReady.value = Boolean(audioTrack?.enabled)
    mediaStreamRef.value = stream

    const onTrackEnded = () => {
      cameraReady.value = Boolean(stream.getVideoTracks().some((t) => t.enabled && t.readyState === 'live'))
      micReady.value = Boolean(stream.getAudioTracks().some((t) => t.enabled && t.readyState === 'live'))
      syncDeviceStatusToBackend()
    }
    videoTrack?.addEventListener('ended', onTrackEnded)
    audioTrack?.addEventListener('ended', onTrackEnded)

    await syncDeviceStatusToBackend()
    deviceStatusInterval = setInterval(syncDeviceStatusToBackend, 15000)
  } catch (error) {
    cameraReady.value = false
    micReady.value = false
    deviceError.value = error?.name === 'NotAllowedError'
      ? 'Bạn cần cấp quyền camera và micro để vào phòng thi.'
      : 'Không thể truy cập camera/micro. Vui lòng kiểm tra thiết bị.'
  } finally {
    isCheckingDevices.value = false
  }
}

const stopMediaStream = () => {
  if (deviceStatusInterval) {
    clearInterval(deviceStatusInterval)
    deviceStatusInterval = null
  }
  const stream = mediaStreamRef.value
  if (stream) {
    stream.getTracks().forEach((t) => t.stop())
    mediaStreamRef.value = null
  }
  if (cameraPreviewRef.value) {
    cameraPreviewRef.value.srcObject = null
  }
}

watch(mediaStreamRef, (stream) => {
  if (!stream) return
  nextTick(() => {
    if (cameraPreviewRef.value) {
      cameraPreviewRef.value.srcObject = stream
    }
  })
})

const reportViolation = async (eventType, details, cooldownMs = VIOLATION_COOLDOWN_MS) => {
  if (isPracticeExam.value || !attemptId.value || isSuspended.value) return
  if (!examConfig.value || examConfig.value.monitorFastSubmit === false && eventType === 'FAST_SUBMIT') return
  if (examConfig.value.monitorTabSwitch === false && eventType === 'TAB_SWITCH') return
  if (examConfig.value.monitorBlur === false && eventType === 'BLUR') return
  if (examConfig.value.monitorExitFullscreen === false && eventType === 'EXIT_FULLSCREEN') return
  if (examConfig.value.monitorCopyPaste === false && eventType === 'COPY_PASTE') return
  if (examConfig.value.monitorIdleTime === false && eventType === 'IDLE_TIME') return
  if (examConfig.value.monitorDevtools === false && eventType === 'DEVTOOLS_OPEN') return
  if (examConfig.value.monitorRightClick === false && eventType === 'RIGHT_CLICK') return
  if (examConfig.value.monitorPrintScreen === false && eventType === 'PRINT_SCREEN') return
  if (examConfig.value.monitorRapidQuestionSwitch === false && eventType === 'RAPID_QUESTION_SWITCH') return
  if (examConfig.value.monitorMultiMonitor === false && eventType === 'MULTI_MONITOR') return

  const now = Date.now()
  const lastAt = lastViolationAtByType.value[eventType] || 0
  if (now - lastAt < cooldownMs) return

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
    stopMediaStream()
    return
  }

  isSuspended.value = false
}

const enforceDeviceAccess = async () => {
  if (isPracticeExam.value) return
  if (mediaStreamRef.value) {
    const videoTrack = mediaStreamRef.value.getVideoTracks()[0]
    const audioTrack = mediaStreamRef.value.getAudioTracks()[0]
    const videoEnded = !videoTrack || videoTrack.readyState === 'ended'
    const audioEnded = !audioTrack || audioTrack.readyState === 'ended'
    if (videoEnded || audioEnded) {
      isSuspended.value = true
      suspensionMessage.value = 'Camera hoặc micro đã bị thu hồi. Vui lòng tải lại trang và cấp quyền lại.'
      return
    }
    cameraReady.value = Boolean(videoTrack?.enabled)
    micReady.value = Boolean(audioTrack?.enabled)
    await syncDeviceStatusToBackend()
    return
  }
  await checkDevices()
  if (!devicesReady.value) {
    isSuspended.value = true
    suspensionMessage.value = deviceError.value || 'Bạn cần cấp quyền camera và micro để tiếp tục làm bài.'
  }
}

const syncAttemptStatus = async () => {
  if (!attemptId.value) return
  try {
    const detail = await getAttemptDetail(attemptId.value)
    applyAttemptStatus(detail?.status || 'IN_PROGRESS')
    // Đồng bộ thời gian còn lại từ server (tránh lệch do client)
    if (typeof detail?.remainingSeconds === 'number' && detail.remainingSeconds >= 0) {
      const diff = Math.abs(remainingSeconds.value - detail.remainingSeconds)
      if (diff > 5) remainingSeconds.value = Math.max(0, detail.remainingSeconds)
    }
  } catch {
    // ignore status sync errors
  }
}

const getAuthToken = async () => {
  if (typeof window === 'undefined') return ''
  const { getStoredToken } = await import('../../services/authService')
  return String(getStoredToken() || '')
}

const connectProctorRealtime = async () => {
  if (isPracticeExam.value || !attemptId.value) return

  const [{ Client }, { default: SockJS }] = await Promise.all([
    import('@stomp/stompjs'),
    import('sockjs-client')
  ])

  const wsUrl = `${API_BASE_URL.replace(/\/$/, '')}/ws`
  const token = await getAuthToken()
  stompClient = new Client({
    reconnectDelay: 5000,
    connectHeaders: token ? { Authorization: `Bearer ${token}` } : {},
    webSocketFactory: () => new SockJS(wsUrl)
  })

  stompClient.onConnect = () => {
    stompClient.subscribe(`/topic/attempts/${attemptId.value}/proctor-actions`, (frame) => {
      try {
        const payload = JSON.parse(frame.body || '{}')
        const type = String(payload.type || '').toUpperCase()
        if (type === 'TEACHER_WARNING') {
          teacherWarningMessage.value = payload.message || 'Bạn đang bị cảnh báo bởi giám thị. Vui lòng tuân thủ quy định phòng thi.'
          showTeacherWarningModal.value = true
          toast.warning(teacherWarningMessage.value)
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

const setupBlockBackButton = () => {
  history.pushState({ examInProgress: true }, '', window.location.href)
  blockBackHandler = () => {
    history.pushState({ examInProgress: true }, '', window.location.href)
    toast.warning('Không thể quay lại khi đang làm bài thi. Vui lòng nộp bài để thoát.')
  }
  window.addEventListener('popstate', blockBackHandler)
}

const teardownBlockBackButton = () => {
  if (blockBackHandler) {
    window.removeEventListener('popstate', blockBackHandler)
    blockBackHandler = null
  }
}

const handleVisibilityChange = () => {
  if (document.hidden) {
    pendingViolationByType.value = {
      ...pendingViolationByType.value,
      TAB_SWITCH: true
    }
    void reportViolation('TAB_SWITCH', 'TAB_SWITCH')
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

const resetIdleTimer = () => {
  if (idleTimer) window.clearTimeout(idleTimer)
  idleTimer = window.setTimeout(() => {
    void reportViolation('IDLE_TIME', `Idle for ${Math.round(IDLE_THRESHOLD_MS / 60000)} minutes`, LONG_VIOLATION_COOLDOWN_MS)
  }, IDLE_THRESHOLD_MS)
}

const handleCopyPaste = (event) => {
  const target = event?.target
  if (target && (target.tagName === 'INPUT' || target.tagName === 'TEXTAREA')) return
  void reportViolation('COPY_PASTE', `Detected ${event.type} during exam`, LONG_VIOLATION_COOLDOWN_MS)
}

const detectDevToolsOpen = () => {
  if (document.hidden) return false
  const widthGap = Math.abs(window.outerWidth - window.innerWidth)
  const heightGap = Math.abs(window.outerHeight - window.innerHeight)
  return widthGap > DEVTOOLS_GAP_PX || heightGap > DEVTOOLS_GAP_PX
}

const scheduleDevToolsCheck = () => {
  if (devtoolsCheckTimer) window.clearInterval(devtoolsCheckTimer)
  devtoolsCheckTimer = window.setInterval(() => {
    if (detectDevToolsOpen()) {
      void reportViolation('DEVTOOLS_OPEN', 'DevTools detected during exam', LONG_VIOLATION_COOLDOWN_MS)
    }
  }, 5000)
}

const checkRapidQuestionSwitch = () => {
  const now = Date.now()
  questionSwitchTimestamps.value = [...questionSwitchTimestamps.value, now].filter(
    (t) => now - t <= RAPID_SWITCH_WINDOW_MS
  )
  if (questionSwitchTimestamps.value.length >= RAPID_SWITCH_THRESHOLD) {
    void reportViolation(
      'RAPID_QUESTION_SWITCH',
      `${questionSwitchTimestamps.value.length} lần đổi câu trong ${RAPID_SWITCH_WINDOW_MS / 1000}s`,
      VIOLATION_COOLDOWN_MS
    )
    questionSwitchTimestamps.value = []
  }
}

const selectQuestion = (index) => {
  if (isSuspended.value) return
  if (index !== currentIndex.value && examConfig.value.monitorRapidQuestionSwitch !== false) {
    checkRapidQuestionSwitch()
  }
  currentIndex.value = index
}

const goPrevious = () => {
  if (isSuspended.value) return
  if (currentIndex.value > 0) {
    if (examConfig.value.monitorRapidQuestionSwitch !== false) checkRapidQuestionSwitch()
    currentIndex.value -= 1
  }
}

const goNext = () => {
  if (isSuspended.value) return
  if (currentIndex.value < questions.value.length - 1) {
    if (examConfig.value.monitorRapidQuestionSwitch !== false) checkRapidQuestionSwitch()
    currentIndex.value += 1
  }
}

const handleRightClick = (e) => {
  if (examConfig.value.monitorRightClick !== false) {
    e.preventDefault()
    e.stopPropagation()
    void reportViolation('RIGHT_CLICK', 'Chuột phải bị chặn trong phòng thi', VIOLATION_COOLDOWN_MS)
  }
}

const handlePrintScreen = (e) => {
  if (examConfig.value.monitorPrintScreen !== false && (e.key === 'PrintScreen' || e.keyCode === 44)) {
    e.preventDefault()
    void reportViolation('PRINT_SCREEN', 'Phát hiện phím Print Screen', LONG_VIOLATION_COOLDOWN_MS)
  }
}

const checkMultiMonitor = () => {
  if (examConfig.value.monitorMultiMonitor === false) return
  const screenW = window.screen?.width || 0
  const screenH = window.screen?.height || 0
  const availW = window.screen?.availWidth || 0
  const availH = window.screen?.availHeight || 0
  if (screenW > 0 && availW > 0 && (screenW - availW > 100 || screenH - availH > 100)) {
    void reportViolation('MULTI_MONITOR', 'Có thể sử dụng nhiều màn hình', LONG_VIOLATION_COOLDOWN_MS)
  }
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
  setTimeout(() => {
    document.getElementById('submit-exam-confirm')?.focus()
  }, 0)
}

const closeSubmitModal = () => {
  showSubmitModal.value = false
}

const autoSubmitOnTimeUp = async () => {
  if (!attemptId.value || isSuspended.value || isSubmitting.value) return
  isSubmitting.value = true
  toast.info('Hết giờ làm bài. Đang tự động nộp bài...')
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
    toast.error('Không thể tự động nộp bài. Vui lòng thử nộp thủ công.')
  } finally {
    isSubmitting.value = false
  }
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
    toast.error('Không thể nộp bài lúc này.')  } finally {
    isSubmitting.value = false
  }
}

const questionButtonClass = (index) => {
  const question = questions.value[index]
  if (!question) return 'bg-slate-100 dark:bg-slate-800 text-slate-400'

  if (index === currentIndex.value) {
    return 'border-2 border-primary bg-primary/10 text-primary ring-2 ring-primary/20'
  }

  if (answers.value[question.id]) {
    return 'bg-primary text-white border-2 border-primary'
  }

  return 'bg-slate-100 dark:bg-slate-800 text-slate-500 dark:text-slate-400 border-2 border-transparent hover:border-slate-300 dark:hover:border-slate-600'
}

onMounted(async () => {
  try {
    if (!examId.value || !attemptId.value) {
      toast.error('Thiếu thông tin bài thi/lượt làm bài.')
      return
    }

    const [{ getExamDetail }] = await Promise.all([
      import('../../services/examService')
    ])

    const [questionList, draftData, examDetail] = await Promise.all([
      listExamQuestions(examId.value),
      getDraftAnswers(attemptId.value),
      getExamDetail(examId.value)
    ])

    examConfig.value = {
      monitorTabSwitch: examDetail?.monitorTabSwitch !== false,
      monitorBlur: examDetail?.monitorBlur !== false,
      monitorExitFullscreen: examDetail?.monitorExitFullscreen !== false,
      monitorCopyPaste: examDetail?.monitorCopyPaste !== false,
      monitorIdleTime: examDetail?.monitorIdleTime !== false,
      monitorDevtools: examDetail?.monitorDevtools !== false,
      monitorDuplicateIp: examDetail?.monitorDuplicateIp !== false,
      monitorFastSubmit: examDetail?.monitorFastSubmit !== false,
      monitorRightClick: examDetail?.monitorRightClick !== false,
      monitorPrintScreen: examDetail?.monitorPrintScreen !== false,
      monitorRapidQuestionSwitch: examDetail?.monitorRapidQuestionSwitch !== false,
      monitorMultiMonitor: examDetail?.monitorMultiMonitor !== false,
      requireCameraMic: examDetail?.requireCameraMic !== false
    }

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

    // Thời gian làm bài tính từ lúc sinh viên tham gia (startAttempt). Đồng bộ từ backend.
    const serverRemaining = draftData?.remainingSeconds
    const serverDeadline = draftData?.deadlineAt || route.query.deadlineAt
    if (typeof serverRemaining === 'number' && serverRemaining >= 0) {
      remainingSeconds.value = Math.max(0, serverRemaining)
    } else if (serverDeadline) {
      const deadlineMs = new Date(String(serverDeadline)).getTime()
      remainingSeconds.value = Math.max(0, Math.floor((deadlineMs - Date.now()) / 1000))
    }

    if (remainingSeconds.value <= 0 && !isPracticeExam.value) {
      void autoSubmitOnTimeUp()
      return
    }

    timerId = window.setInterval(() => {
      if (remainingSeconds.value > 0) {
        remainingSeconds.value -= 1
        if (remainingSeconds.value === 0) {
          if (timerId) {
            clearInterval(timerId)
            timerId = null
          }
          void autoSubmitOnTimeUp()
        }
      }
    }, 1000)

    await enforceDeviceAccess()

    if (!devicesReady.value) {
      if (!shouldCheckDevices.value) {
        isSuspended.value = false
      } else {
        toast.error(suspensionMessage.value)
        return
      }
    }

    isSuspended.value = false

    setupBlockBackButton()

    if (!isPracticeExam.value) {
      connectProctorRealtime()
      attemptStatusTimer = window.setInterval(() => {
        syncAttemptStatus()
        enforceDeviceAccess()
      }, 5000)

      if (examConfig.value.monitorIdleTime !== false) {
        resetIdleTimer()
        window.addEventListener('mousemove', resetIdleTimer)
        window.addEventListener('keydown', resetIdleTimer)
        window.addEventListener('scroll', resetIdleTimer)
      }

      if (examConfig.value.monitorDevtools !== false) {
        scheduleDevToolsCheck()
      }

      if (examConfig.value.monitorTabSwitch !== false) {
        document.addEventListener('visibilitychange', handleVisibilityChange)
      }
      if (examConfig.value.monitorBlur !== false) {
        window.addEventListener('blur', handleWindowBlur)
        window.addEventListener('focus', handleWindowFocus)
      }
      if (examConfig.value.monitorExitFullscreen !== false) {
        document.addEventListener('fullscreenchange', handleFullscreenChange)
      }
      if (examConfig.value.monitorCopyPaste !== false) {
        document.addEventListener('copy', handleCopyPaste)
        document.addEventListener('paste', handleCopyPaste)
      }
      if (examConfig.value.monitorPrintScreen !== false) {
        document.addEventListener('keydown', handlePrintScreen)
      }
      if (examConfig.value.monitorMultiMonitor !== false) {
        setTimeout(checkMultiMonitor, 3000)
      }
    }
  } catch (error) {
    toast.error('Không thể tải nội dung bài thi.')
  }
})

onUnmounted(() => {
  teardownBlockBackButton()
  stopMediaStream()
  if (timerId) window.clearInterval(timerId)
  if (attemptStatusTimer) window.clearInterval(attemptStatusTimer)
  if (idleTimer) window.clearTimeout(idleTimer)
  if (devtoolsCheckTimer) window.clearInterval(devtoolsCheckTimer)
  clearBlurGraceTimer()
  if (stompClient) {
    stompClient.deactivate()
    stompClient = null
  }
  if (!isPracticeExam.value) {
    if (examConfig.value.monitorTabSwitch !== false) {
      document.removeEventListener('visibilitychange', handleVisibilityChange)
    }
    if (examConfig.value.monitorBlur !== false) {
      window.removeEventListener('blur', handleWindowBlur)
      window.removeEventListener('focus', handleWindowFocus)
    }
    if (examConfig.value.monitorExitFullscreen !== false) {
      document.removeEventListener('fullscreenchange', handleFullscreenChange)
    }
    if (examConfig.value.monitorCopyPaste !== false) {
      document.removeEventListener('copy', handleCopyPaste)
      document.removeEventListener('paste', handleCopyPaste)
    }
    if (examConfig.value.monitorPrintScreen !== false) {
      document.removeEventListener('keydown', handlePrintScreen)
    }
    if (examConfig.value.monitorIdleTime !== false) {
      window.removeEventListener('mousemove', resetIdleTimer)
      window.removeEventListener('keydown', resetIdleTimer)
      window.removeEventListener('scroll', resetIdleTimer)
    }
  }
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
