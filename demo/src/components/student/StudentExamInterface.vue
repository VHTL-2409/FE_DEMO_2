<template>
  <div class="ei-root">
    <!-- Fixed Top Bar -->
    <div class="ei-topbar">
      <div class="ei-topbar__left">
        <div class="ei-topbar__exam-chip">
          <LucideIcon name="quiz" size="14" />
          <span class="ei-topbar__exam-title">{{ examTitle }}</span>
        </div>
        <div class="ei-topbar__device-badges">
          <span class="ei-device-badge" :class="cameraReady ? 'ei-device-badge--ok' : 'ei-device-badge--fail'">
            <LucideIcon :name="cameraReady ? 'videocam' : 'videocam_off'" size="11" />
            Cam
          </span>
          <span class="ei-device-badge" :class="micReady ? 'ei-device-badge--ok' : 'ei-device-badge--fail'">
            <LucideIcon :name="micReady ? 'mic' : 'mic_off'" size="11" />
            Mic
          </span>
          <span class="ei-device-badge" :class="isOnline ? 'ei-device-badge--ok' : 'ei-device-badge--fail'">
            <LucideIcon :name="isOnline ? 'wifi' : 'wifi_off'" size="11" />
            {{ isOnline ? 'Online' : 'Offline' }}
          </span>
          <span v-if="identityRecheckEnabled" class="ei-device-badge" :class="identityRecheckBadgeClass">
            <LucideIcon :name="identityRecheckIcon" size="11" />
            ID {{ identityRecheckLabel }}
          </span>
        </div>
      </div>

      <!-- Timer -->
      <div class="ei-topbar__timer" :class="timerBarAccentClass">
        <LucideIcon name="timer" size="15" class="ei-topbar__timer-icon" />
        <div class="ei-topbar__timer-digits">
          <span class="ei-timer-unit">
            <span class="ei-timer-val">{{ timerHours }}</span>
            <span class="ei-timer-lbl">:</span>
          </span>
          <span class="ei-timer-unit">
            <span class="ei-timer-val">{{ timerMinutes }}</span>
            <span class="ei-timer-lbl">:</span>
          </span>
          <span class="ei-timer-unit">
            <span class="ei-timer-val">{{ timerSeconds }}</span>
          </span>
        </div>
        <div class="ei-topbar__timer-progress">
          <div class="ei-topbar__timer-progress-fill" :style="{ width: timerProgressWidth }" />
        </div>
      </div>

      <!-- Right actions -->
      <div class="ei-topbar__right">
        <div v-if="saveStatusLabel" class="ei-save-status" role="status">
          <LucideIcon
            :name="saveStatus === 'saving' ? 'progress_activity' : saveStatus === 'saved' ? 'check_circle' : hasPendingChanges ? 'schedule' : 'cloud_off'"
            :class="saveStatus === 'saving' ? 'ei-save-icon--spin' : ''"
            size="12"
          />
          <span class="ei-save-label">{{ saveStatusLabel }}</span>
        </div>
        <button type="button" class="ei-btn ei-btn--save" :disabled="isSuspended || saveStatus === 'saving'" @click="manualSaveDraft">
          <LucideIcon name="save" size="13" />
          <span>Lưu</span>
        </button>
        <button type="button" class="ei-btn ei-btn--submit" :disabled="isSuspended" @click="openSubmitModal">
          <LucideIcon name="done_all" size="14" />
          <span>Nộp bài</span>
        </button>
      </div>
    </div>

    <!-- Fullscreen Warning -->
    <Transition name="ei-slide-down">
      <div v-if="showFullscreenPrompt && !isPracticeExam && !isSuspended" class="ei-fs-warn">
        <LucideIcon name="fullscreen" size="15" />
        <span>Yêu cầu chế độ toàn màn hình</span>
        <button type="button" class="ei-fs-warn__btn" @click="requestExamFullscreen">Vào toàn màn hình</button>
      </div>
    </Transition>

    <!-- ── Unified Modal (warning / error / success / info) ── -->
    <Modal
      v-model="modal.show"
      :title="modal.title"
      :subtitle="modal.subtitle"
      size="md"
      @close="closeModal"
    >
      <template #header="{ titleId }">
        <div class="ei-modal__header">
          <div class="ei-modal__icon" :class="`ei-modal__icon--${modal.type}`">
            <LucideIcon :name="modalIcon" size="20" />
          </div>
          <div class="ei-modal__header-copy">
            <h2 :id="titleId" class="ei-modal__title">{{ modal.title }}</h2>
            <p v-if="modal.subtitle" class="ei-modal__sub">{{ modal.subtitle }}</p>
          </div>
        </div>
      </template>

      <div class="ei-modal__body">
        <p class="ei-modal__msg">{{ modal.message }}</p>
      </div>

      <template #footer>
        <div class="ei-modal__actions">
          <button
            v-if="modal.type !== 'success' && modal.type !== 'info'"
            type="button"
            class="ei-modal__cancel"
            @click="closeModal"
          >
            Đóng
          </button>
          <button
            type="button"
            class="ei-modal__confirm"
            :class="`ei-modal__confirm--${modal.type}`"
            @click="handleModalConfirm"
          >
            {{ modal.confirmLabel || 'Đã hiểu' }}
          </button>
        </div>
      </template>
    </Modal>

    <!-- ── Suspended Overlay ─────────────────────────────── -->
    <Transition name="ei-fade">
      <div v-if="isSuspended" class="ei-suspend-overlay">
        <div class="ei-suspend-card">
          <div class="ei-suspend-icon">
            <LucideIcon name="pause_circle" size="36" />
          </div>
          <h2 class="ei-suspend-title">
            {{ attemptStatus === 'PAUSED' ? 'Phiên thi đang tạm dừng' : 'Bài thi đã bị đình chỉ' }}
          </h2>
          <p class="ei-suspend-desc">{{ suspensionMessage || 'Giám thị đã hủy hiệu lực bài thi của bạn.' }}</p>
        </div>
      </div>
    </Transition>

    <!-- ── Main Content ──────────────────────────────── -->
    <main class="ei-main">
      <!-- Left: Question Area -->
      <div class="ei-question-col">
        <div v-if="examLoadFailed" class="ei-empty-state">
          <LucideIcon name="error" size="32" class="ei-empty-icon" />
          <p class="ei-empty-title">Không tải được đề thi</p>
          <p class="ei-empty-desc">Vui lòng làm mới trang hoặc quay lại sau.</p>
        </div>

        <div v-else-if="!examSurfaceReady" class="ei-empty-state">
          <LucideIcon name="progress_activity" size="32" class="ei-empty-icon" />
          <p class="ei-empty-title">Đang tải đề thi…</p>
          <p class="ei-empty-desc">Vui lòng đợi trong giây lát.</p>
        </div>

        <div v-else-if="!questions.length" class="ei-empty-state">
          <LucideIcon name="quiz" size="32" class="ei-empty-icon" />
          <p class="ei-empty-title">Không có câu hỏi</p>
          <p class="ei-empty-desc">Đề thi chưa có nội dung. Hãy thoát và liên hệ giáo viên.</p>
        </div>

        <Transition name="ei-q-slide" mode="out-in">
          <div v-if="currentQuestion" :key="currentQuestion.id" class="ei-question-card">
            <!-- Question header -->
            <div class="ei-q-header">
              <div class="ei-q-progress-ring">
                <svg class="ei-q-ring-svg" viewBox="0 0 36 36" width="36" height="36">
                  <circle class="ei-q-ring-bg" cx="18" cy="18" r="15" />
                  <circle
                    class="ei-q-ring-prog"
                    cx="18" cy="18" r="15"
                    :stroke-dasharray="qRingCircumference"
                    :stroke-dashoffset="qRingOffset"
                  />
                </svg>
                <span class="ei-q-ring-label">{{ currentIndex + 1 }}</span>
              </div>
              <span class="ei-q-badge">/ {{ questions.length }} câu</span>
              <button
                type="button"
                class="ei-mark-btn"
                :disabled="isSuspended"
                @click="toggleMarkCurrentQuestion"
              >
                <LucideIcon :name="markedQuestions[String(currentQuestion.id)] ? 'bookmark_added' : 'bookmark_add'" size="13" />
                {{ markedQuestions[String(currentQuestion.id)] ? 'Đã đánh dấu' : 'Đánh dấu' }}
              </button>
            </div>

            <!-- Question text -->
            <div class="ei-q-content">
              <h2 class="ei-q-text">
                <MathDisplay
                  :content="currentQuestion.content"
                  :latex-content="currentQuestion.latexContent"
                  :content-type="currentQuestion.contentType ?? currentQuestion.content_type ?? null"
                  source-preference="latex-first"
                />
              </h2>
            </div>

            <!-- Answer options -->
            <div class="ei-q-answer">
              <QuestionRenderer :question="currentQuestion" v-model="currentQuestionAnswer" :disabled="isSuspended" />
            </div>

            <!-- Navigation footer -->
            <div class="ei-q-footer">
              <button
                type="button"
                class="ei-nav-btn ei-nav-btn--prev"
                :disabled="currentIndex === 0"
                @click="goPrevious"
              >
                <LucideIcon name="arrow_back" size="15" />
                Câu trước
              </button>

              <button
                type="button"
                class="ei-nav-btn ei-nav-btn--next"
                :disabled="currentIndex >= questions.length - 1"
                @click="goNext"
              >
                Câu tiếp
                <LucideIcon name="arrow_forward" size="15" />
              </button>
            </div>
          </div>
        </Transition>
      </div>

      <!-- Right: Sidebar -->
      <aside class="ei-sidebar">
        <div v-if="shouldCheckDevices && mediaStreamRef" class="ei-cam-card">
          <div class="ei-cam-header">
            <div class="ei-cam-header__copy">
              <span class="ei-cam-label">Thiết bị giám sát</span>
              <span class="ei-cam-mode">{{ aiCameraAnalysisRequested ? 'AI giám sát' : 'Camera cơ bản' }}</span>
            </div>
            <div class="ei-cam-actions">
              <button
                type="button"
                :disabled="isSuspended"
                class="ei-cam-btn"
                :class="cameraReady ? 'ei-cam-btn--on' : 'ei-cam-btn--off'"
                @click="toggleCamera"
                :title="cameraReady ? 'Tắt camera' : 'Bật camera'"
              >
                <LucideIcon :name="cameraReady ? 'videocam' : 'videocam_off'" size="13" />
              </button>
              <button
                type="button"
                :disabled="isSuspended"
                class="ei-cam-btn"
                :class="micReady ? 'ei-cam-btn--on' : 'ei-cam-btn--off'"
                @click="toggleMic"
                :title="micReady ? 'Tắt micro' : 'Bật micro'"
              >
                <LucideIcon :name="micReady ? 'mic' : 'mic_off'" size="13" />
              </button>
            </div>
          </div>

          <div class="ei-cam-status">
            <span class="ei-cam-status__chip" :class="cameraReady ? 'ei-cam-status__chip--on' : 'ei-cam-status__chip--off'">
              Cam {{ cameraReady ? 'bật' : 'tắt' }}
            </span>
            <span class="ei-cam-status__chip" :class="micReady ? 'ei-cam-status__chip--on' : 'ei-cam-status__chip--off'">
              Mic {{ micReady ? 'bật' : 'tắt' }}
            </span>
            <span class="ei-cam-status__chip ei-cam-status__chip--info">
              {{ aiCameraAnalysisRequested ? 'AI mỗi 3 giây' : 'Camera 1 fps' }}
            </span>
          </div>

          <div class="ei-cam-transport" :class="frameTransportToneClass" role="status">
            <span class="ei-cam-transport__label">{{ frameTransportLabel }}</span>
            <span class="ei-cam-transport__detail">{{ frameTransportDetail }}</span>
          </div>
        </div>
        <div v-else-if="shouldCheckDevices" class="ei-cam-card">
          <div class="ei-cam-header">
            <div class="ei-cam-header__copy">
              <span class="ei-cam-label">Thiết bị giám sát</span>
              <span class="ei-cam-mode">{{ aiCameraAnalysisRequested ? 'AI giám sát' : 'Camera cơ bản' }}</span>
            </div>
          </div>
          <div class="ei-cam-status">
            <span class="ei-cam-status__chip" :class="cameraReady ? 'ei-cam-status__chip--on' : 'ei-cam-status__chip--off'">
              Cam {{ cameraReady ? 'bật' : 'tắt' }}
            </span>
            <span class="ei-cam-status__chip" :class="micReady ? 'ei-cam-status__chip--on' : 'ei-cam-status__chip--off'">
              Mic {{ micReady ? 'bật' : 'tắt' }}
            </span>
          </div>
          <div class="ei-cam-transport" :class="deviceError ? 'ei-cam-transport--fail' : 'ei-cam-transport--pending'" role="status">
            <span class="ei-cam-transport__label">{{ deviceError ? 'Lỗi camera' : 'Đang khởi tạo camera' }}</span>
            <span class="ei-cam-transport__detail">{{ deviceError || (isCheckingDevices ? 'Đang chờ quyền truy cập camera' : 'Chưa nhận được luồng camera từ trình duyệt') }}</span>
          </div>
        </div>
        <div v-else-if="!isPracticeExam" class="ei-cam-card">
          <div class="ei-cam-header">
            <div class="ei-cam-header__copy">
              <span class="ei-cam-label">Thiết bị giám sát</span>
              <span class="ei-cam-mode">Tắt theo cấu hình bài thi</span>
            </div>
          </div>
          <div class="ei-cam-transport ei-cam-transport--idle" role="status">
            <span class="ei-cam-transport__label">Chưa gửi frame</span>
            <span class="ei-cam-transport__detail">Camera/AI đang tắt trong cấu hình, FE không khởi tạo stream và không gửi ảnh.</span>
          </div>
        </div>

        <!-- Progress card -->
        <div v-if="examSurfaceReady" class="ei-prog-card">
          <div class="ei-prog-header">
            <LucideIcon name="trending_up" size="13" />
            <span class="ei-prog-title">Tiến độ</span>
            <span class="ei-prog-pct">{{ progressPercent }}%</span>
          </div>
          <div class="ei-prog-bar">
            <div class="ei-prog-bar__fill" :style="{ width: `${progressPercent}%` }" />
          </div>
          <div class="ei-prog-stats">
            <span class="ei-prog-stat">
              <span class="ei-prog-dot ei-prog-dot--done" />
              Đã làm: {{ answeredCount }}
            </span>
            <span class="ei-prog-stat">
              <span class="ei-prog-dot ei-prog-dot--skip" />
              Chưa: {{ unansweredCount }}
            </span>
          </div>
          <div v-if="markedCount > 0" class="ei-prog-tags">
            <span class="ei-prog-tag ei-prog-tag--mark">
              <LucideIcon name="bookmark" size="10" />
              Đánh dấu: {{ markedCount }}
            </span>
          </div>
        </div>

        <!-- Question navigator -->
        <div v-if="examSurfaceReady" class="ei-nav-card">
          <div class="ei-nav-card__header">
            <LucideIcon name="list" size="13" />
            <span>Câu hỏi</span>
            <span class="ei-nav-card__count">{{ questions.length }}</span>
          </div>
          <div class="ei-q-grid">
            <button
              v-for="(question, idx) in questions"
              :key="question.id"
              type="button"
              :class="questionButtonClass(idx)"
              class="ei-q-grid-btn"
              @click="selectQuestion(idx)"
            >
              {{ idx + 1 }}
            </button>
          </div>
          <div class="ei-q-legend">
            <span class="ei-legend-item">
              <span class="ei-legend-dot ei-legend-dot--done" />
              Đã làm
            </span>
            <span class="ei-legend-item">
              <span class="ei-legend-dot ei-legend-dot--mark" />
              Đánh dấu
            </span>
          </div>
        </div>
      </aside>
    </main>

    <div class="ei-camera-capture" aria-hidden="true">
      <video ref="cameraPreviewRef" autoplay playsinline muted class="ei-camera-capture__video" tabindex="-1" />
    </div>

    <!-- ── Submit Modal ────────────────────────────────── -->
    <SubmitDialog
      v-model="showSubmitModal"
      :answered-count="answeredCount"
      :total-questions="questions.length"
      :unanswered-count="unansweredCount"
      :not-visited-count="notVisitedCount"
      :marked-count="markedCount"
      :skipped-count="skippedCount"
      :is-submitting="isSubmitting"
      @confirm="submitExamAction"
    />

    <ConfirmDialog
      v-model="showLeaveConfirm"
      title="Rời bài thi?"
      message="Bạn đang trong phiên thi. Đáp án đã được lưu cục bộ."
      description="Rời đi sẽ không mất đáp án đã lưu."
      confirm-label="Rời đi"
      cancel-label="Tiếp tục thi"
      variant="warning"
      icon="log-out"
      @confirm="onConfirmLeave"
      @cancel="onCancelLeave"
    />
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { getAttemptDetail, getDraftAnswers, saveDraftAnswers, submitAttempt } from '../../services/attemptService'
import { updateDeviceStatus } from '../../services/monitoringService'
import { listExamQuestions, parseQuestionJson, parseQuestionOptions } from '../../services/questionService'
import { analyzeProctorFrame, recheckStudentIdentity, sendProctorCameraFrame } from '../../services/proctorService'
import { useToast } from '../../composables/useToast'
import { useAutoSaveDraft } from '../../composables/useAutoSaveDraft'
import { useExamProctoring } from '../../composables/useExamProctoring'
import { useProctoringSession } from '../../composables/useProctoringSession'
import { useRealtimeChannel } from '../../composables/useRealtimeChannel'
import { useMicrophoneSpeechDetector } from '../../composables/useMicrophoneSpeechDetector'
import { onBeforeRouteLeave, useRoute, useRouter } from 'vue-router'
import { useExamSessionStore } from '../../stores/examSessionStore'
import { parseBackendDate } from '../../utils/dateUtils.js'
import { detectMultiMonitor } from '../../utils/multiMonitorDetection.js'
import { buildSubmissionQuery } from '../../services/studentExamContextStorage'
import QuestionRenderer from './questions/QuestionRenderer.vue'
import MathDisplay from '../shared/MathDisplay.vue'
import Modal from '../ui/Modal.vue'
import ConfirmDialog from '../ui/ConfirmDialog.vue'
import SubmitDialog from './exam/exam/SubmitDialog.vue'

const route = useRoute()
const router = useRouter()
const toast = useToast()

const showSubmitModal = ref(false)
const showLeaveConfirm = ref(false)
const pendingLeaveCallback = ref(null)
const isSubmitting = ref(false)
const allowConfirmedLeave = ref(false)
const questions = ref([])
const examSurfaceReady = ref(false)
const examLoadFailed = ref(false)
const isOnline = ref(typeof navigator === 'undefined' ? true : navigator.onLine !== false)

const examTitle = computed(() => route.query.exam || 'Bài thi')
const examId = computed(() => Number.parseInt(String(route.query.examId || ''), 10) || null)
const attemptId = computed(() => Number.parseInt(String(route.query.attemptId || ''), 10) || null)
const isPracticeExam = computed(() => String(route.query.isPractice || '') === 'true')

const examConfig = ref({
  monitorTabSwitch: true, monitorBlur: true, monitorExitFullscreen: true,
  monitorCopyPaste: true, monitorIdleTime: false, monitorDevtools: true,
  monitorDuplicateIp: true, monitorFastSubmit: false, monitorRightClick: false,
  monitorPrintScreen: false, monitorRapidQuestionSwitch: false,
  monitorMultiMonitor: false, requireCameraMic: true,
  enableAiProctoring: false, aiFaceDetection: false, aiEyeTracking: false,
  monitorNetworkInstability: true, monitorSessionRecovery: true,
  monitorQuestionTimingAnomaly: false, monitorAnswerChangeBurst: false,
  monitorClipboardBurst: false, monitorFullscreenEvasion: true
})

const MODAL_COOLDOWN_MS = 3000  // Prevent rapid-fire duplicate modals
let lastModalShownAt = 0

// ── Unified modal state (replaces 4 separate popup refs) ──
const modal = ref({
  show: false,
  type: 'info',   // warning | error | success | info
  title: '',
  subtitle: '',
  message: '',
  confirmLabel: '',
  onConfirm: null
})

const modalIconMap = { warning: 'warning', error: 'error', success: 'check_circle', info: 'info' }
const modalIcon = computed(() => modalIconMap[modal.value.type] || 'info')

const closeAllTransientModals = () => {
  if (modal.value.show) lastModalShownAt = 0
  modal.value.show = false
  showSubmitModal.value = false
  showLeaveConfirm.value = false
}

const showModal = (cfg) => {
  const now = Date.now()
  // Prevent showing if modal is already displayed or within cooldown
  if (modal.value.show) return
  if (now - lastModalShownAt < MODAL_COOLDOWN_MS) return
  closeAllTransientModals()
  lastModalShownAt = now
  modal.value = {
    show: true,
    type: 'info',
    title: '',
    subtitle: '',
    message: '',
    confirmLabel: '',
    onConfirm: null,
    ...cfg
  }
}

const closeModal = () => {
  modal.value.show = false
  // Reset cooldown when modal is closed to allow new modals immediately
  lastModalShownAt = 0
}

const handleModalConfirm = () => {
  const onConfirm = modal.value.onConfirm
  closeModal()
  if (typeof onConfirm === 'function') onConfirm()
}

const answers = ref({})
const markedQuestions = ref({})
const visitedQuestions = ref({})
const currentIndex = ref(0)
const remainingSeconds = ref(Number.parseInt(String(route.query.remainingSeconds || ''), 10) || 0)
const initialRemainingForProgress = ref(0)
const attemptStatus = ref('IN_PROGRESS')
const isSuspended = ref(false)
const suspensionMessage = ref('')
const lastViolationAtByType = ref({})
const answerSignatureByQuestion = ref({})
const focusLostAt = ref(null)
const hiddenStartedAt = ref(null)
const fullscreenExitStartedAt = ref(null)
const fullscreenExitCount = ref(0)
const multiMonitorDetected = ref(false)
const lastMultiMonitorEvidence = ref(null)
const questionEnteredAt = ref(Date.now())
const reconnectCount = ref(0)
const offlineStartedAt = ref(null)
const lastOfflineDurationMs = ref(0)
const cameraReady = ref(false)
const micReady = ref(false)
const isConnected = ref(true)
const deviceError = ref('')
const isCheckingDevices = ref(false)
const mediaStreamRef = ref(null)
const cameraPreviewRef = ref(null)
let deviceStatusInterval = null
let questionList = []
let timerId = null
let blurGraceTimer = null
let attemptStatusTimer = null
let blockBackHandler = null
let aiFrameInterval = null
let cameraFrameInterval = null
let identityRecheckInterval = null
let cameraFrameSequence = 0

const VIOLATION_COOLDOWN_MS = 5000  // 5s - prevents rapid-fire duplicates during batch, backend handles score dedup
const LONG_VIOLATION_COOLDOWN_MS = 10000  // 10s - longer cooldown for severe violations
const BLUR_GRACE_MS = 1200
const DEVTOOLS_GAP_PX = 160
const LONG_SCREEN_LEAVE_THRESHOLD_MS = 30_000
// Production default: keep AI analysis advisory and light on small VPS deployments.
const AI_FRAME_INTERVAL_MS = 3_000
const CAMERA_FRAME_INTERVAL_MS = 1_000
const FRAME_SEND_RETRY_DELAY_MS = 250
const FRAME_SEND_MAX_ATTEMPTS = 2
const AI_FRAME_BUSY_MIN_COOLDOWN_MS = 2_000
const AI_FRAME_BUSY_MAX_COOLDOWN_MS = 15_000
const AI_FRAME_UNAVAILABLE_COOLDOWN_MS = 10_000
const AI_FRAME_WIDTH = 640
const AI_FRAME_JPEG_QUALITY = 0.6
const SPEECH_VAD_INTERVAL_MS = 250
const SPEECH_VAD_MIN_DURATION_MS = 1200
const SPEECH_VAD_SILENCE_RESET_MS = 600
const SPEECH_VAD_THRESHOLD = 0.035
const SPEECH_VIOLATION_COOLDOWN_MS = 30000

const examSessionStore = useExamSessionStore()
const realtimeChannel = useRealtimeChannel()
const showFullscreenPrompt = ref(false)
const isFullscreenActive = ref(false)

const getFullscreenElement = () =>
  document.fullscreenElement || document.webkitFullscreenElement || null

const syncFullscreenState = () => {
  const inFullscreen = Boolean(getFullscreenElement())
  isFullscreenActive.value = inFullscreen
  examSessionStore.setFullscreenState({ required: !isPracticeExam.value, active: inFullscreen })
  showFullscreenPrompt.value = !isPracticeExam.value && !isSuspended.value && !inFullscreen && !allowConfirmedLeave.value
  if (inFullscreen) fullscreenExitStartedAt.value = null
  return inFullscreen
}

const questionById = computed(() => {
  const map = new Map()
  for (const question of questions.value) {
    map.set(Number(question.id), question)
  }
  return map
})

const normalizeQuestionType = (q) => String(q?.type || 'SINGLE_CHOICE').toUpperCase()

const hasAnswerValue = (value) => {
  if (Array.isArray(value)) return value.length > 0
  if (value && typeof value === 'object') return Object.keys(value).length > 0
  return Boolean(value)
}

const serializeAnswerValue = (question, value) => {
  const type = normalizeQuestionType(question)
  if (value == null) return ''
  if (type === 'MULTIPLE_CHOICE' || type === 'ORDERING') return JSON.stringify(Array.isArray(value) ? value : [])
  if (type === 'MATCHING') return JSON.stringify(value && typeof value === 'object' ? value : {})
  return String(value)
}

const deserializeAnswerValue = (question, value) => {
  const type = normalizeQuestionType(question)
  if (value == null) return type === 'MULTIPLE_CHOICE' || type === 'ORDERING' ? [] : (type === 'MATCHING' ? {} : '')
  if (type === 'MULTIPLE_CHOICE' || type === 'ORDERING') return Array.isArray(value) ? value : parseQuestionJson(value, [])
  if (type === 'MATCHING') return (value && typeof value === 'object' && !Array.isArray(value)) ? value : parseQuestionJson(value, {})
  return String(value)
}

const createOptionIdMap = (question) => {
  const options = Array.isArray(question?.options) ? question.options : []
  return options.reduce((acc, option) => {
    const displayId = String(option?.id ?? '').trim()
    const originalId = String(option?.originalId ?? option?.original_id ?? option?.id ?? '').trim()
    if (displayId) acc[displayId] = originalId || displayId
    if (originalId) acc[originalId] = originalId
    return acc
  }, {})
}

const normalizeAnswerIdsForQuestion = (question, value) => {
  const type = normalizeQuestionType(question)
  const optionIdMap = createOptionIdMap(question)
  const mapId = (raw) => {
    const normalized = String(raw ?? '').trim()
    if (!normalized) return ''
    return optionIdMap[normalized] || normalized
  }
  if (type === 'MULTIPLE_CHOICE' || type === 'ORDERING') {
    const arr = Array.isArray(value) ? value : deserializeAnswerValue(question, value)
    return arr.map(mapId).filter(Boolean)
  }
  if (type === 'MATCHING') {
    return deserializeAnswerValue(question, value)
  }
  return mapId(value)
}

const currentQuestion = computed(() => questions.value[currentIndex.value] || null)
const shouldCheckDevices = computed(() => !isPracticeExam.value && (
  examConfig.value.requireCameraMic !== false || aiCameraAnalysisRequested.value
))
const aiFrameInFlight = ref(false)
const cameraFrameInFlight = ref(false)
const aiFrameCooldownUntilMs = ref(0)
const cameraFrameTransport = ref({
  phase: 'idle',
  captured: false,
  sent: false,
  acknowledged: false,
  frameId: '',
  lastCapturedAt: '',
  lastSentAt: '',
  lastAckAt: '',
  lastSendError: '',
  lastPayloadSize: 0,
  lastCaptureSource: '',
  lastMode: '',
  lastAckStatus: '',
  lastAckMessage: '',
  lastRetryAfterMs: 0
})
const aiCameraAnalysisRequested = computed(() =>
  examConfig.value.enableAiProctoring === true
  || examConfig.value.aiFaceDetection === true
  || examConfig.value.aiEyeTracking === true
)
const cameraFrameTransportEnabled = computed(() => {
  const status = String(attemptStatus.value || '').toUpperCase()
  return !isPracticeExam.value
    && Boolean(attemptId.value)
    && !isSuspended.value
    && (status === 'IN_PROGRESS' || status === 'ACTIVE')
    && cameraReady.value
    && Boolean(mediaStreamRef.value)
    && shouldCheckDevices.value
})
const cameraFrameStreamingEnabled = computed(() =>
  cameraFrameTransportEnabled.value && !aiCameraAnalysisRequested.value
)
const aiFrameAnalysisEnabled = computed(() =>
  cameraFrameTransportEnabled.value && aiCameraAnalysisRequested.value
)
const identityRecheckStatus = ref(String(route.query.verificationStatus || 'NOT_CHECKED').toUpperCase())
const identityRecheckInFlight = ref(false)
const identityRecheckEnabled = computed(() => {
  if (isPracticeExam.value || isSuspended.value || !attemptId.value) return false
  if (String(route.query.inExamIdentityCheckEnabled || 'true') === 'false') return false
  return cameraFrameTransportEnabled.value && shouldCheckDevices.value
})
const identityRecheckIntervalMs = computed(() => {
  const seconds = Number.parseInt(String(route.query.identityCheckIntervalSeconds || '60'), 10)
  return Math.max(30, Number.isFinite(seconds) ? seconds : 60) * 1000
})
const identityRecheckBadgeClass = computed(() => {
  if (identityRecheckInFlight.value) return 'ei-device-badge--warn'
  if (identityRecheckStatus.value === 'VERIFIED') return 'ei-device-badge--ok'
  if (identityRecheckStatus.value === 'NEEDS_REVIEW') return 'ei-device-badge--warn'
  return 'ei-device-badge--fail'
})
const identityRecheckIcon = computed(() => {
  if (identityRecheckInFlight.value) return 'progress_activity'
  if (identityRecheckStatus.value === 'VERIFIED') return 'verified_user'
  if (identityRecheckStatus.value === 'NEEDS_REVIEW') return 'flag'
  return 'shield'
})
const identityRecheckLabel = computed(() => {
  if (identityRecheckInFlight.value) return 'kiểm tra'
  if (identityRecheckStatus.value === 'VERIFIED') return 'ổn'
  if (identityRecheckStatus.value === 'NEEDS_REVIEW') return 'cần duyệt'
  return 'chưa rõ'
})
const examProgressStats = computed(() => {
  let answered = 0; let marked = 0; let skipped = 0; let notVisited = 0
  for (const question of questions.value) {
    const key = String(question.id)
    const visited = Boolean(visitedQuestions.value[key])
    const hasAnswer = hasAnswerValue(answers.value[key])
    const isMarked = Boolean(markedQuestions.value[key])
    if (hasAnswer) answered += 1
    if (isMarked) marked += 1
    if (!visited) notVisited += 1
    else if (!hasAnswer && !isMarked) skipped += 1
  }
  return { answered, unanswered: Math.max(questions.value.length - answered, 0), marked, skipped, notVisited }
})
const answeredCount = computed(() => examProgressStats.value.answered)
const unansweredCount = computed(() => examProgressStats.value.unanswered)
const markedCount = computed(() => examProgressStats.value.marked)
const skippedCount = computed(() => examProgressStats.value.skipped)
const notVisitedCount = computed(() => examProgressStats.value.notVisited)
const progressPercent = computed(() => {
  if (!questions.value.length) return 0
  return Math.round((answeredCount.value / questions.value.length) * 100)
})

const timerHours = computed(() => String(Math.floor(remainingSeconds.value / 3600)).padStart(2, '0'))
const timerMinutes = computed(() => String(Math.floor((remainingSeconds.value % 3600) / 60)).padStart(2, '0'))
const timerSeconds = computed(() => String(remainingSeconds.value % 60).padStart(2, '0'))
const timerProgressWidth = computed(() => {
  if (!initialRemainingForProgress.value) return '0%'
  return `${Math.max(0, (remainingSeconds.value / initialRemainingForProgress.value) * 100)}%`
})

const prefersReducedMotion = () => typeof window !== 'undefined' && window.matchMedia?.('(prefers-reduced-motion: reduce)').matches

const timerBarAccentClass = computed(() => {
  const max = Math.max(initialRemainingForProgress.value, 1)
  const ratio = remainingSeconds.value / max
  if (ratio < 0.2) return prefersReducedMotion() ? 'ei-timer--danger' : 'ei-timer--danger ei-timer--pulse'
  if (ratio <= 0.5) return 'ei-timer--warn'
  return ''
})

const qRingCircumference = 2 * Math.PI * 15
const qRingOffset = computed(() => {
  if (!questions.value.length) return qRingCircumference
  const ratio = (currentIndex.value) / questions.value.length
  return qRingCircumference * (1 - ratio)
})

const markVisited = (questionId) => {
  const key = String(questionId)
  if (!visitedQuestions.value[key]) visitedQuestions.value[key] = true
}

const currentQuestionAnswer = computed({
  get: () => {
    const q = currentQuestion.value
    if (!q) return ''
    const cv = answers.value[q.id]
    const type = normalizeQuestionType(q)
    if (type === 'MULTIPLE_CHOICE' || type === 'ORDERING') return Array.isArray(cv) ? cv : []
    if (type === 'MATCHING') return (cv && typeof cv === 'object' && !Array.isArray(cv)) ? cv : {}
    return cv ?? ''
  },
  set: (value) => {
    const q = currentQuestion.value
    if (!q) return
    onSelectAnswer(q.id, value)
  }
})

const clearBlurGraceTimer = () => { if (blurGraceTimer) { window.clearTimeout(blurGraceTimer); blurGraceTimer = null } }

const syncDeviceStatusToBackend = async () => {
  if (!attemptId.value || isPracticeExam.value || isSuspended.value) return
  try { await updateDeviceStatus(attemptId.value, cameraReady.value, micReady.value) } catch { /* ignore */ }
}

const toggleCamera = () => {
  const videoTrack = mediaStreamRef.value?.getVideoTracks()[0]
  if (!videoTrack) return
  videoTrack.enabled = !videoTrack.enabled
  cameraReady.value = videoTrack.enabled && videoTrack.readyState !== 'ended'
  if (!cameraReady.value) {
    stopAiFrameSampler()
    stopCameraFrameStreamer()
  }
  void syncDeviceStatusToBackend()
}

const toggleMic = () => {
  const audioTrack = mediaStreamRef.value?.getAudioTracks()[0]
  if (!audioTrack) return
  audioTrack.enabled = !audioTrack.enabled
  micReady.value = audioTrack.enabled
  void syncDeviceStatusToBackend()
}

const requestCameraStream = async () => {
  return navigator.mediaDevices.getUserMedia({ video: true, audio: false })
}

const getLiveVideoTrack = () => {
  const tracks = mediaStreamRef.value?.getVideoTracks?.() || []
  return tracks.find(track => track && track.readyState !== 'ended') || null
}

const attachOptionalMicrophone = async (stream) => {
  if (!stream || examConfig.value.requireCameraMic === false) {
    micReady.value = true
    return stream
  }
  try {
    const audioStream = await navigator.mediaDevices.getUserMedia({ video: false, audio: true })
    const audioTrack = audioStream.getAudioTracks()[0]
    if (audioTrack) {
      const combined = new MediaStream([...stream.getVideoTracks(), audioTrack])
      micReady.value = Boolean(audioTrack.enabled)
      return combined
    }
    micReady.value = false
    deviceError.value = 'Camera đã bật, nhưng chưa nhận được micro.'
  } catch {
    micReady.value = false
    deviceError.value = 'Camera đã bật, nhưng trình duyệt chưa cấp được micro.'
  }
  return stream
}

const checkDevices = async () => {
  if (!shouldCheckDevices.value) { cameraReady.value = false; micReady.value = false; deviceError.value = ''; return }
  if (!navigator?.mediaDevices?.getUserMedia) { cameraReady.value = false; micReady.value = false; deviceError.value = 'Trình duyệt không hỗ trợ.'; return }
  isCheckingDevices.value = true; deviceError.value = ''
  try {
    const stream = await requestCameraStream()
    const vt = stream.getVideoTracks()[0]
    if (!vt) {
      stream.getTracks().forEach(t => t.stop())
      throw new Error('NO_VIDEO_TRACK')
    }
    const finalStream = await attachOptionalMicrophone(stream)
    cameraReady.value = Boolean(vt.enabled && vt.readyState !== 'ended')
    mediaStreamRef.value = finalStream
    deviceStatusInterval = setInterval(syncDeviceStatusToBackend, 15000)
    await syncDeviceStatusToBackend()
  } catch (error) {
    cameraReady.value = false; micReady.value = false
    deviceError.value = error?.name === 'NotAllowedError'
      ? 'Cần cấp quyền camera.'
      : error?.message === 'NO_VIDEO_TRACK'
        ? 'Không tìm thấy camera khả dụng.'
        : 'Không truy cập được camera.'
  } finally { isCheckingDevices.value = false }
}

const stopMediaStream = () => {
  speechDetector.stop()
  stopAiFrameSampler()
  stopCameraFrameStreamer()
  if (deviceStatusInterval) { clearInterval(deviceStatusInterval); deviceStatusInterval = null }
  if (mediaStreamRef.value) { mediaStreamRef.value.getTracks().forEach(t => t.stop()); mediaStreamRef.value = null }
  if (cameraPreviewRef.value) cameraPreviewRef.value.srcObject = null
  cameraReady.value = false
  micReady.value = false
}

const attachCameraPreview = async (stream) => {
  if (!stream) return
  await nextTick()
  const video = cameraPreviewRef.value
  if (!video) return
  video.srcObject = stream
  try {
    await video.play()
  } catch {
    // autoplay can be denied on some browsers; frame capture will retry once metadata is available
  }
}

const waitForCameraVideoFrame = async () => {
  const video = cameraPreviewRef.value
  if (!video) return Boolean(getLiveVideoTrack())
  if (!video.srcObject && mediaStreamRef.value) video.srcObject = mediaStreamRef.value
  if (video.readyState >= 2 && video.videoWidth && video.videoHeight) return true
  try {
    await video.play()
  } catch {
    // muted inline playback should normally be allowed; capture will retry on the next tick
  }
  if (video.readyState >= 2 && video.videoWidth && video.videoHeight) return true
  return new Promise((resolve) => {
    let timer = null
    const onReady = () => {
      if (timer) window.clearTimeout(timer)
      video.removeEventListener('loadeddata', onReady)
      video.removeEventListener('playing', onReady)
      resolve(Boolean(video.videoWidth && video.videoHeight))
    }
    timer = window.setTimeout(() => {
      video.removeEventListener('loadeddata', onReady)
      video.removeEventListener('playing', onReady)
      resolve(false)
    }, 700)
    video.addEventListener('loadeddata', onReady, { once: true })
    video.addEventListener('playing', onReady, { once: true })
  })
}

const buildCameraFramePayloadFromDrawable = (drawable, sourceWidth, sourceHeight, captureSource) => {
  if (!drawable || !sourceWidth || !sourceHeight) return null
  const width = Math.min(AI_FRAME_WIDTH, sourceWidth)
  const height = Math.max(1, Math.round((sourceHeight / sourceWidth) * width))
  const canvas = document.createElement('canvas')
  canvas.width = width
  canvas.height = height
  const ctx = canvas.getContext('2d')
  if (!ctx) return null
  ctx.drawImage(drawable, 0, 0, width, height)
  const videoTrack = getLiveVideoTrack()
  const frameId = nextCameraFrameId(captureSource)
  const cameraOn = Boolean(cameraReady.value && videoTrack?.enabled !== false && videoTrack?.readyState !== 'ended')
  return {
    frameId,
    attemptId: attemptId.value,
    imageBase64: canvas.toDataURL('image/jpeg', AI_FRAME_JPEG_QUALITY),
    capturedAt: new Date().toISOString(),
    metadata: {
      examId: examId.value,
      source: 'student_exam_interface',
      frameId,
      cameraOn,
      micOn: micReady.value,
      visibility: document.visibilityState || 'visible',
      fullscreen: Boolean(getFullscreenElement()),
      questionIndex: currentIndex.value + 1,
      viewportWidth: window.innerWidth || null,
      viewportHeight: window.innerHeight || null,
      aiEnabled: aiCameraAnalysisRequested.value,
      enableAiProctoring: aiCameraAnalysisRequested.value,
      aiFaceDetection: examConfig.value.aiFaceDetection === true,
      aiEyeTracking: examConfig.value.aiEyeTracking === true,
      requireCameraMic: examConfig.value.requireCameraMic !== false,
      aiAnalysisReason: aiCameraAnalysisRequested.value ? 'AI_PROCTORING' : 'CAMERA_REQUIRED',
      captureSource,
      sourceWidth,
      sourceHeight,
      trackEnabled: videoTrack?.enabled ?? null,
      trackReadyState: videoTrack?.readyState || null
    }
  }
}

const nextCameraFrameId = (captureSource) => {
  cameraFrameSequence += 1
  const attemptKey = attemptId.value || 'unknown'
  const sourceKey = captureSource || 'capture'
  return `frame-${attemptKey}-${Date.now()}-${cameraFrameSequence}-${sourceKey}`
}

const estimateFramePayloadBytes = (payload) => {
  if (!payload) return 0
  try {
    return new Blob([JSON.stringify(payload)]).size
  } catch {
    try {
      return JSON.stringify(payload).length
    } catch {
      return 0
    }
  }
}

const updateCameraFrameTransport = (patch) => {
  cameraFrameTransport.value = {
    ...cameraFrameTransport.value,
    ...patch
  }
}

const formatFrameBytes = (bytes) => {
  if (!Number.isFinite(bytes) || bytes <= 0) return '0 B'
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(bytes < 10 * 1024 ? 1 : 0)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(bytes < 10 * 1024 * 1024 ? 1 : 0)} MB`
}

const normalizeFrameSendError = (error) => {
  if (!error) return 'Không gửi được frame'
  const status = error?.status
  if (status === 0) return 'Không kết nối được BE'
  const payloadMessage = error?.payload?.message || error?.payload?.error || error?.payload?.userMessage
  const message = payloadMessage || error?.message || 'Không gửi được frame'
  return status ? `HTTP ${status}: ${message}` : message
}

const clampAiFrameCooldown = (retryAfterMs, fallbackMs) => {
  const parsed = Number.parseInt(String(retryAfterMs || ''), 10)
  const value = Number.isFinite(parsed) && parsed > 0 ? parsed : fallbackMs
  return Math.min(AI_FRAME_BUSY_MAX_COOLDOWN_MS, Math.max(AI_FRAME_BUSY_MIN_COOLDOWN_MS, value))
}

const applyAiFrameBackoffFromAck = (ack) => {
  const aiStatus = String(ack?.aiServiceStatus || ack?.status || ack?.message || '').toUpperCase()
  if (aiStatus.includes('AI_BUSY')) {
    const cooldownMs = clampAiFrameCooldown(ack?.retryAfterMs, AI_FRAME_BUSY_MIN_COOLDOWN_MS)
    aiFrameCooldownUntilMs.value = Date.now() + cooldownMs
    return cooldownMs
  }
  if (aiStatus.includes('AI_UNAVAILABLE') || aiStatus.includes('UNAVAILABLE')) {
    aiFrameCooldownUntilMs.value = Date.now() + AI_FRAME_UNAVAILABLE_COOLDOWN_MS
    return AI_FRAME_UNAVAILABLE_COOLDOWN_MS
  }
  if (aiStatus === 'OK' || aiStatus === 'DONE' || ack?.backendAnalysisReceived === true) {
    aiFrameCooldownUntilMs.value = 0
  }
  return 0
}

const applyAiFrameBackoffFromError = (error) => {
  const payload = error?.payload || {}
  if (error?.status === 429 || error?.status === 503) {
    const cooldownMs = clampAiFrameCooldown(payload.retryAfterMs || payload.retry_after_ms, AI_FRAME_BUSY_MIN_COOLDOWN_MS)
    aiFrameCooldownUntilMs.value = Date.now() + cooldownMs
    return cooldownMs
  }
  if (error?.status === 0 || error?.name === 'TypeError') {
    aiFrameCooldownUntilMs.value = Date.now() + AI_FRAME_UNAVAILABLE_COOLDOWN_MS
    return AI_FRAME_UNAVAILABLE_COOLDOWN_MS
  }
  return 0
}

const canRetryFrameSend = (error, attemptIndex) => {
  if (attemptIndex >= FRAME_SEND_MAX_ATTEMPTS - 1) return false
  const status = error?.status
  return status === 0 || status === 401 || error?.name === 'TypeError'
}

const sendFrameTransportRequest = async (payload, mode) => {
  const primarySender = mode === 'ai' ? analyzeProctorFrame : sendProctorCameraFrame
  let lastError = null

  for (let attemptIndex = 0; attemptIndex < FRAME_SEND_MAX_ATTEMPTS; attemptIndex += 1) {
    try {
      updateCameraFrameTransport({
        phase: 'sent',
        sent: true,
        lastSentAt: new Date().toISOString(),
        lastMode: mode
      })
      const ack = await primarySender(payload)
      const isAcked = Boolean(ack && (ack.accepted === true || ack.acknowledged === true || ack.transportStatus === 'ACKNOWLEDGED'))
      if (!isAcked) {
        throw new Error('BE không trả ACK frame')
      }
      const retryAfterMs = mode === 'ai' ? applyAiFrameBackoffFromAck(ack) : 0
      updateCameraFrameTransport({
        phase: 'acknowledged',
        captured: true,
        sent: true,
        acknowledged: true,
        lastAckAt: ack.receivedAt || new Date().toISOString(),
        lastAckStatus: ack.transportStatus || 'ACKNOWLEDGED',
        lastAckMessage: ack.transportMessage || ack.message || ack.status || '',
        lastRetryAfterMs: retryAfterMs,
        lastSendError: '',
        lastMode: mode,
        lastPayloadSize: ack.payloadBytes || estimateFramePayloadBytes(payload),
        frameId: ack.frameId || payload.frameId || ''
      })
      return ack
    } catch (error) {
      lastError = error
      if (mode === 'ai') applyAiFrameBackoffFromError(error)
      if (!canRetryFrameSend(error, attemptIndex)) break
      await new Promise((resolve) => window.setTimeout(resolve, FRAME_SEND_RETRY_DELAY_MS))
    }
  }

  if (mode === 'ai') {
    try {
      updateCameraFrameTransport({
        phase: 'sent',
        sent: true,
        lastSentAt: new Date().toISOString(),
        lastMode: 'camera'
      })
      const ack = await sendProctorCameraFrame(payload)
      const isAcked = Boolean(ack && (ack.accepted === true || ack.acknowledged === true || ack.transportStatus === 'ACKNOWLEDGED'))
      if (isAcked) {
        const retryAfterMs = applyAiFrameBackoffFromAck(ack)
        updateCameraFrameTransport({
          phase: 'acknowledged',
          captured: true,
          sent: true,
          acknowledged: true,
          lastAckAt: ack.receivedAt || new Date().toISOString(),
          lastAckStatus: ack.transportStatus || 'ACKNOWLEDGED',
          lastAckMessage: `AI chậm/tạm lỗi; ${ack.transportMessage || ack.message || ack.status || 'BE đã nhận camera'}`,
          lastRetryAfterMs: retryAfterMs,
          lastSendError: '',
          lastMode: 'ai_fallback',
          lastPayloadSize: ack.payloadBytes || estimateFramePayloadBytes(payload),
          frameId: ack.frameId || payload.frameId || ''
        })
        return ack
      }
      lastError = new Error('BE không trả ACK frame')
    } catch (fallbackError) {
      lastError = fallbackError
      applyAiFrameBackoffFromError(fallbackError)
    }
  }

  updateCameraFrameTransport({
    phase: 'failed',
    captured: true,
    sent: true,
    acknowledged: false,
    lastSendError: normalizeFrameSendError(lastError),
    lastMode: mode,
    lastRetryAfterMs: mode === 'ai' ? Math.max(0, aiFrameCooldownUntilMs.value - Date.now()) : 0,
    lastPayloadSize: estimateFramePayloadBytes(payload)
  })
  throw lastError || new Error('Không gửi được frame')
}

const frameTransportLabel = computed(() => {
  const state = cameraFrameTransport.value
  if (state.phase === 'acknowledged' && (state.lastMode === 'ai' || state.lastMode === 'ai_fallback')) {
    const aiStatus = String(state.lastAckMessage || state.lastAckStatus || '').toUpperCase()
    if (aiStatus.includes('AI_BUSY')) {
      return 'AI đang bận'
    }
    if (aiStatus.includes('AI_UNAVAILABLE') || aiStatus.includes('UNAVAILABLE')) {
      return 'AI chậm/tạm lỗi'
    }
  }
  switch (cameraFrameTransport.value.phase) {
    case 'captured':
      return 'Đã chụp'
    case 'sent':
      return 'Đang gửi'
    case 'acknowledged':
      return 'BE đã nhận'
    case 'failed':
      return 'Lỗi gửi frame'
    default:
      return 'Chưa gửi'
  }
})

const frameTransportDetail = computed(() => {
  const state = cameraFrameTransport.value
  if (state.phase === 'failed') {
    return state.lastSendError || 'Không gửi được frame'
  }
  if (state.phase === 'acknowledged') {
    const parts = []
    if (state.lastAckMessage) parts.push(state.lastAckMessage)
    if (state.lastRetryAfterMs) parts.push(`giãn ${Math.ceil(state.lastRetryAfterMs / 1000)}s`)
    if (state.lastAckAt) {
      try {
        parts.push(`ACK ${new Date(state.lastAckAt).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit', second: '2-digit' })}`)
      } catch {
        parts.push('ACK')
      }
    }
    if (state.lastPayloadSize) parts.push(formatFrameBytes(state.lastPayloadSize))
    if (state.lastCaptureSource) parts.push(state.lastCaptureSource)
    return parts.join(' · ') || 'Khung hình đã được BE xác nhận'
  }
  if (state.phase === 'sent') {
    return state.lastPayloadSize ? `${formatFrameBytes(state.lastPayloadSize)} · đang chờ ACK` : 'Đang chờ ACK'
  }
  if (state.phase === 'captured') {
    return state.lastCaptureSource ? `Nguồn: ${state.lastCaptureSource}` : 'Đã chụp xong'
  }
  return 'Chưa có frame mới'
})

const frameTransportToneClass = computed(() => {
  const state = cameraFrameTransport.value
  const aiStatus = String(state.lastAckMessage || state.lastAckStatus || '').toUpperCase()
  if (state.phase === 'acknowledged' && (aiStatus.includes('AI_BUSY') || aiStatus.includes('AI_UNAVAILABLE'))) {
    return 'ei-cam-transport--pending'
  }
  switch (cameraFrameTransport.value.phase) {
    case 'acknowledged':
      return 'ei-cam-transport--ok'
    case 'failed':
      return 'ei-cam-transport--fail'
    case 'sent':
      return 'ei-cam-transport--pending'
    case 'captured':
      return 'ei-cam-transport--captured'
    default:
      return 'ei-cam-transport--idle'
  }
})

const markFrameCapturedForSend = (payload, mode) => {
  updateCameraFrameTransport({
    phase: 'captured',
    captured: true,
    sent: false,
    acknowledged: false,
    frameId: payload.frameId || '',
    lastCapturedAt: payload.capturedAt || new Date().toISOString(),
    lastPayloadSize: estimateFramePayloadBytes(payload),
    lastCaptureSource: payload.metadata?.captureSource || 'unknown',
    lastMode: mode,
    lastAckStatus: '',
    lastAckMessage: '',
    lastRetryAfterMs: 0,
    lastSendError: ''
  })
}

const markFrameCaptureFailed = (mode, message) => {
  updateCameraFrameTransport({
    phase: 'failed',
    captured: false,
    sent: false,
    acknowledged: false,
    lastMode: mode,
    lastSendError: message || 'Không lấy được frame từ camera'
  })
}

const buildCameraFramePayloadFromVideo = () => {
  const video = cameraPreviewRef.value
  if (!video || video.readyState < 2 || !video.videoWidth || !video.videoHeight) return null
  return buildCameraFramePayloadFromDrawable(video, video.videoWidth, video.videoHeight, 'video_element')
}

const buildCameraFramePayloadFromTrack = async () => {
  const videoTrack = getLiveVideoTrack()
  if (!videoTrack || typeof window.ImageCapture !== 'function') return null
  try {
    const imageCapture = new window.ImageCapture(videoTrack)
    const bitmap = await imageCapture.grabFrame()
    try {
      return buildCameraFramePayloadFromDrawable(bitmap, bitmap.width, bitmap.height, 'image_capture')
    } finally {
      bitmap?.close?.()
    }
  } catch (error) {
    if (import.meta.env.DEV) {
      console.debug('[StudentExamInterface] ImageCapture frame grab failed', error)
    }
    return null
  }
}

const buildCameraFramePayload = async () => {
  return buildCameraFramePayloadFromVideo() || await buildCameraFramePayloadFromTrack()
}

const captureAndSendCameraFrame = async () => {
  if (!cameraFrameStreamingEnabled.value || cameraFrameInFlight.value) return
  cameraFrameInFlight.value = true
  try {
    await waitForCameraVideoFrame()
    if (!cameraFrameStreamingEnabled.value) return
    const payload = await buildCameraFramePayload()
    if (!payload) {
      markFrameCaptureFailed('camera', 'Không tạo được frame từ camera')
      return
    }
    markFrameCapturedForSend(payload, 'camera')
    await sendFrameTransportRequest(payload, 'camera')
  } catch (error) {
    if (import.meta.env.DEV) {
      console.warn('[StudentExamInterface] Camera frame send failed', error)
    }
  } finally {
    cameraFrameInFlight.value = false
  }
}

const captureAndSendAiFrame = async () => {
  if (!aiFrameAnalysisEnabled.value || aiFrameInFlight.value) return
  if (Date.now() < aiFrameCooldownUntilMs.value) return
  aiFrameInFlight.value = true
  try {
    await waitForCameraVideoFrame()
    if (!aiFrameAnalysisEnabled.value) return
    const payload = await buildCameraFramePayload()
    if (!payload) {
      markFrameCaptureFailed('ai', 'Không tạo được frame AI từ camera')
      return
    }
    markFrameCapturedForSend(payload, 'ai')
    await sendFrameTransportRequest(payload, 'ai')
  } catch (error) {
    if (import.meta.env.DEV) {
      console.warn('[StudentExamInterface] AI camera frame send failed', error)
    }
    // AI camera is advisory; keep the exam flow running if a sample fails.
  } finally {
    aiFrameInFlight.value = false
  }
}

const captureAndRecheckIdentity = async () => {
  if (!identityRecheckEnabled.value || identityRecheckInFlight.value) return
  identityRecheckInFlight.value = true
  try {
    await waitForCameraVideoFrame()
    if (!identityRecheckEnabled.value) return
    const payload = await buildCameraFramePayload()
    if (!payload) return
    payload.metadata = {
      ...(payload.metadata || {}),
      captureSource: 'identity_recheck',
      checkType: 'PERIODIC',
      initialIdentityCheckId: route.query.identityCheckId || ''
    }
    const result = await recheckStudentIdentity(payload)
    identityRecheckStatus.value = String(result?.verificationStatus || result?.status || identityRecheckStatus.value || 'NEEDS_REVIEW').toUpperCase()
  } catch (error) {
    identityRecheckStatus.value = 'NEEDS_REVIEW'
    if (import.meta.env.DEV) {
      console.warn('[StudentExamInterface] Identity recheck failed', error)
    }
  } finally {
    identityRecheckInFlight.value = false
  }
}

const startIdentityRecheckSampler = () => {
  if (identityRecheckInterval || !identityRecheckEnabled.value) return
  identityRecheckInterval = window.setInterval(() => {
    void captureAndRecheckIdentity()
  }, identityRecheckIntervalMs.value)
}

const stopIdentityRecheckSampler = () => {
  if (identityRecheckInterval) {
    window.clearInterval(identityRecheckInterval)
    identityRecheckInterval = null
  }
}

const startAiFrameSampler = () => {
  if (aiFrameInterval || !aiFrameAnalysisEnabled.value) return
  void captureAndSendAiFrame()
  aiFrameInterval = window.setInterval(() => {
    void captureAndSendAiFrame()
  }, AI_FRAME_INTERVAL_MS)
}

const stopAiFrameSampler = () => {
  if (aiFrameInterval) {
    window.clearInterval(aiFrameInterval)
    aiFrameInterval = null
  }
}

const startCameraFrameStreamer = () => {
  if (cameraFrameInterval || !cameraFrameStreamingEnabled.value) return
  void captureAndSendCameraFrame()
  cameraFrameInterval = window.setInterval(() => {
    void captureAndSendCameraFrame()
  }, CAMERA_FRAME_INTERVAL_MS)
}

const stopCameraFrameStreamer = () => {
  if (cameraFrameInterval) {
    window.clearInterval(cameraFrameInterval)
    cameraFrameInterval = null
  }
}

watch(mediaStreamRef, async (stream) => {
  if (!stream) {
    if (cameraPreviewRef.value) cameraPreviewRef.value.srcObject = null
    return
  }
  await attachCameraPreview(stream)
})

watch(shouldCheckDevices, (enabled) => {
  if (!enabled) {
    stopMediaStream()
    return
  }
  if (!mediaStreamRef.value && !isSuspended.value) {
    void checkDevices()
  }
})

watch(cameraFrameStreamingEnabled, (enabled) => {
  if (enabled) startCameraFrameStreamer()
  else stopCameraFrameStreamer()
})

watch(aiFrameAnalysisEnabled, (enabled) => {
  if (enabled) startAiFrameSampler()
  else stopAiFrameSampler()
})

watch(identityRecheckEnabled, (enabled) => {
  if (enabled) startIdentityRecheckSampler()
  else stopIdentityRecheckSampler()
})

const syncRiskState = (payload) => {
  if (!payload || typeof payload !== 'object') return
  const nextRiskScore = typeof payload.riskScore === 'number' ? payload.riskScore : payload.score
  const nextRiskLevel = payload.riskLevel || payload.level
  if (typeof nextRiskScore === 'number') {
    examSessionStore.setRiskState({ riskScore: nextRiskScore, riskLevel: nextRiskLevel || examSessionStore.riskState.riskLevel, status: payload.status || attemptStatus.value })
  }
}

const handleRealtimeProctorMessage = (payload = {}) => {
  const type = String(payload?.type || '').toUpperCase()
  if (!type) return

  if (type === 'RISK_UPDATED' || type === 'RISK_UPDATE') {
    syncRiskState(payload)
    handleProctorActions([payload.actionTaken], payload)
    return
  }

  if (type === 'WARNING_SENT' || type === 'TEACHER_WARNING') {
    handleProctorActions(['WARNING_SENT'], payload)
    return
  }

  if (type === 'MONITORING_CONFIG_UPDATED') {
    applyExamConfigFromAttempt(payload)
    syncFullscreenState()
    void checkMultiMonitor()
    return
  }

  if (type === 'ATTEMPT_STOPPED' || type === 'ATTEMPT_PAUSED') {
    applyAttemptStatus(payload.status || (type === 'ATTEMPT_PAUSED' ? 'PAUSED' : 'STOPPED'), payload.message || 'Bài thi đã bị đình chỉ.')
    return
  }

  if (type === 'ATTEMPT_RESUMED') {
    void resumeFromPause(payload.message)
  }
}

const handleProctorActions = (actions = [], payload = {}) => {
  const normalized = actions.map((item) => String(item || '').toUpperCase())
  if (normalized.includes('PAUSE_ATTEMPT') || normalized.includes('ATTEMPT_PAUSED')) {
    applyAttemptStatus(payload.status || 'PAUSED', payload.message || 'Phiên thi đang được giám thị kiểm tra.')
  }
  if (normalized.includes('RESUME_ATTEMPT') || normalized.includes('ATTEMPT_RESUMED')) {
    void resumeFromPause(payload.message)
  }
  if (normalized.includes('SHOW_WARNING') || normalized.includes('WARNING_SENT')) {
    showModal({
      type: 'warning',
      title: 'Cảnh báo từ giám thị',
      subtitle: 'Vui lòng chú ý và tuân thủ quy định phòng thi.',
      message: payload.message || 'Hệ thống phát hiện rủi ro cao. Vui lòng tiếp tục làm bài đúng quy định.',
      confirmLabel: 'Tôi đã hiểu'
    })
    toast.warning(payload.message || 'Bạn nhận được cảnh báo từ giám thị.')
  }
}

const buildDeviceFingerprintSeed = () => {
  const screen = window.screen || {}
  return JSON.stringify({
    screen: `${screen.width || 0}x${screen.height || 0}`,
    timezone: Intl.DateTimeFormat().resolvedOptions().timeZone || '',
    language: navigator.language || '',
    platform: navigator.platform || '',
    userAgent: navigator.userAgent || '',
    hardwareConcurrency: navigator.hardwareConcurrency || null,
    deviceMemory: navigator.deviceMemory || null,
    colorDepth: screen.colorDepth || null,
    touchPoints: navigator.maxTouchPoints || 0
  })
}

const getHeartbeatPayload = () => ({
  fullscreen: Boolean(getFullscreenElement()), visibility: document.visibilityState || 'visible',
  cameraOn: cameraReady.value, micOn: micReady.value,
  screenMetrics: { screenWidth: window.screen?.width || null, screenHeight: window.screen?.height || null,
    availWidth: window.screen?.availWidth || null, availHeight: window.screen?.availHeight || null, isExtended: Boolean(window.screen?.isExtended === true), viewportWidth: window.innerWidth || null, viewportHeight: window.innerHeight || null,
    networkType: navigator.connection?.effectiveType || '', online: navigator.onLine !== false,
    multiMonitor: lastMultiMonitorEvidence.value || null },
  telemetry: buildTelemetrySnapshot()
})

const {
  start: startPhaseOneProctoring,
  refreshRisk: refreshPhaseOneRisk
} = useExamProctoring({
  attemptId: attemptId.value,
  sessionId: examId.value,
  getDeviceFingerprint: buildDeviceFingerprintSeed,
  getCameraState: () => cameraReady.value,
  getMicState: () => micReady.value,
  autoStart: false,
  cooldownMs: VIOLATION_COOLDOWN_MS,
  onRiskUpdate: syncRiskState
})

const { queueEvent, flush: flushQueuedViolations, syncHeartbeat, startHeartbeat, stopHeartbeat } = useProctoringSession({
  getAttemptId: () => attemptId.value, getDeviceFingerprint: buildDeviceFingerprintSeed, getHeartbeatPayload,
  onRiskUpdate: syncRiskState, onActionRequired: handleProctorActions, batchWindowMs: 1200, heartbeatIntervalMs: 15000
})

const buildTelemetrySnapshot = (overrides = {}) => ({
  questionIndex: currentIndex.value + 1,
  questionId: currentQuestion.value?.id || null,
  networkOnline: navigator.onLine !== false,
  reconnectCount: reconnectCount.value,
  offlineDurationMs: lastOfflineDurationMs.value,
  networkType: navigator.connection?.effectiveType || '',
  screenExtended: Boolean(window.screen?.isExtended === true),
  multiMonitorDetected: multiMonitorDetected.value,
  multiMonitor: lastMultiMonitorEvidence.value,
  ...overrides
})

const speechDetectionEnabled = computed(() => {
  const audioTrack = (mediaStreamRef.value?.getAudioTracks?.() || []).find(track => track && track.readyState !== 'ended') || null
  return !isPracticeExam.value
    && shouldCheckDevices.value
    && !isSuspended.value
    && micReady.value
    && Boolean(audioTrack && audioTrack.enabled !== false)
})

const queueSpeechViolation = (evidence = {}) => {
  if (isPracticeExam.value || !attemptId.value || isSuspended.value) return
  const eventType = 'AI_SPEAKING_DETECTED'
  const now = Date.now()
  const lastAt = lastViolationAtByType.value[eventType] || 0
  if (now - lastAt < SPEECH_VIOLATION_COOLDOWN_MS) return
  lastViolationAtByType.value = { ...lastViolationAtByType.value, [eventType]: now }
  queueEvent(
    eventType,
    'Phát hiện tiếng ồn',
    {
      questionIndex: currentIndex.value + 1,
      questionId: currentQuestion.value?.id || null,
      speech: {
        ...evidence,
        eventSource: 'microphone_vad',
        micOn: micReady.value,
        cameraOn: cameraReady.value
      }
    },
    0.75,
    buildTelemetrySnapshot({ eventSource: 'microphone_vad' }),
    now
  )
}

const speechDetector = useMicrophoneSpeechDetector({
  streamRef: mediaStreamRef,
  enabledRef: speechDetectionEnabled,
  onSpeechDetected: queueSpeechViolation,
  options: {
    sampleIntervalMs: SPEECH_VAD_INTERVAL_MS,
    speechThreshold: SPEECH_VAD_THRESHOLD,
    minSpeechMs: SPEECH_VAD_MIN_DURATION_MS,
    silenceResetMs: SPEECH_VAD_SILENCE_RESET_MS,
    cooldownMs: SPEECH_VIOLATION_COOLDOWN_MS
  }
})

const ATTEMPT_CONFIG_KEYS = [
  'enableAiProctoring',
  'aiFaceDetection',
  'aiEyeTracking',
  'requireCameraMic',
  'monitorTabSwitch',
  'monitorBlur',
  'monitorExitFullscreen',
  'monitorCopyPaste',
  'monitorIdleTime',
  'monitorDevtools',
  'monitorDuplicateIp',
  'monitorFastSubmit',
  'monitorRightClick',
  'monitorPrintScreen',
  'monitorRapidQuestionSwitch',
  'monitorMultiMonitor',
  'monitorNetworkInstability',
  'monitorSessionRecovery',
  'monitorQuestionTimingAnomaly',
  'monitorAnswerChangeBurst',
  'monitorClipboardBurst',
  'monitorFullscreenEvasion'
]

const applyExamConfigFromAttempt = (detail = {}) => {
  const next = { ...examConfig.value }
  for (const key of ATTEMPT_CONFIG_KEYS) {
    if (typeof detail[key] === 'boolean') next[key] = detail[key]
  }
  examConfig.value = next
}

const reportViolation = async (eventType, details, cooldownMs = VIOLATION_COOLDOWN_MS, telemetry = {}) => {
  if (isPracticeExam.value || !attemptId.value || isSuspended.value) return
  const cfg = examConfig.value
  // Only send real browser-observed events
  const monitorMap = {
    TAB_SWITCH: 'monitorTabSwitch',
    WINDOW_BLUR: 'monitorBlur',
    LONG_SCREEN_LEAVE: 'monitorBlur',
    EXIT_FULLSCREEN: ['monitorExitFullscreen', 'monitorFullscreenEvasion'],
    COPY_PASTE: 'monitorCopyPaste',
    RIGHT_CLICK: 'monitorRightClick',
    PRINT_SCREEN: 'monitorPrintScreen',
    DEVTOOLS_OPEN: 'monitorDevtools',
    MULTI_MONITOR: 'monitorMultiMonitor'
  }
  const prop = monitorMap[eventType]
  if (Array.isArray(prop)) {
    if (!prop.some(key => cfg[key] !== false)) return
  } else if (prop && cfg[prop] === false) return
  const now = Date.now()
  const lastAt = lastViolationAtByType.value[eventType] || 0
  if (cooldownMs > 0 && now - lastAt < cooldownMs) return
  lastViolationAtByType.value = { ...lastViolationAtByType.value, [eventType]: now }
  try {
    queueEvent(
      eventType,
      details,
      { questionIndex: currentIndex.value + 1, questionId: currentQuestion.value?.id || null },
      null,
      buildTelemetrySnapshot({ eventSource: eventType, ...telemetry }),
      now
    )
  } catch { /* ignore */ }
}

const applyAttemptStatus = (status, message = '') => {
  const previous = attemptStatus.value
  const normalized = String(status || '').toUpperCase() || 'IN_PROGRESS'
  attemptStatus.value = normalized
  examSessionStore.setRiskState({ status: normalized })
  if (normalized === 'STOPPED' || normalized === 'PAUSED') {
    isSuspended.value = true
    if (message) suspensionMessage.value = message
    else if (normalized === 'PAUSED') suspensionMessage.value = 'Phiên thi đang được tạm dừng để giám thị kiểm tra.'
    closeAllTransientModals()
    stopMediaStream()
    return
  }
  // Active again — re-engage proctoring resources only if we just left a paused state
  if (previous === 'PAUSED' && (normalized === 'ACTIVE' || normalized === 'IN_PROGRESS')) {
    void resumeFromPause(message)
    return
  }
  isSuspended.value = false
  suspensionMessage.value = ''
}

const resumeFromPause = async (message = '') => {
  isSuspended.value = false
  suspensionMessage.value = ''
  attemptStatus.value = 'IN_PROGRESS'
  examSessionStore.setRiskState({ status: 'IN_PROGRESS' })

  // Re-init camera/mic stopped during pause so monitoring keeps working
  if (!isPracticeExam.value && shouldCheckDevices.value && !mediaStreamRef.value) {
    await checkDevices()
  }
  // Restart heartbeat (was stopped via stopMediaStream chain when paused)
  startHeartbeat()
  await syncAttemptStatus()

  showModal({
    type: 'info',
    title: 'Bạn được phép tiếp tục thi',
    subtitle: 'Giám thị đã hoàn tất kiểm tra.',
    message: message || 'Bạn có thể tiếp tục làm bài. Vui lòng tuân thủ quy định phòng thi.',
    confirmLabel: 'Tiếp tục thi'
  })
  toast.success('Giám thị đã cho phép bạn tiếp tục bài thi.')
}

const enforceDeviceAccess = async () => {
  if (isPracticeExam.value) return
  if (mediaStreamRef.value) {
    const vt = mediaStreamRef.value.getVideoTracks()[0]
    const at = mediaStreamRef.value.getAudioTracks()[0]
    const ve = !vt || vt.readyState === 'ended'
    if (ve) {
      isSuspended.value = true
      suspensionMessage.value = 'Camera đã bị thu hồi. Vui lòng tải lại trang.'
      return
    }
    cameraReady.value = Boolean(vt?.enabled && vt.readyState !== 'ended')
    micReady.value = at ? Boolean(at.enabled) : micReady.value
    await syncDeviceStatusToBackend()
    return
  }
  await checkDevices()
  if (shouldCheckDevices.value && !cameraReady.value) { isSuspended.value = true; suspensionMessage.value = deviceError.value || 'Cần cấp quyền camera.' }
}

const requestExamFullscreen = async () => {
  if (isPracticeExam.value) return false
  if (getFullscreenElement()) return syncFullscreenState()
  const target = document.documentElement
  const request = target?.requestFullscreen || target?.webkitRequestFullscreen
  if (!request) {
    showFullscreenPrompt.value = true
    return false
  }
  try {
    await request.call(target, { navigationUI: 'hide' })
  } catch {
    try {
      await request.call(target)
    } catch {
      showFullscreenPrompt.value = true
      return false
    }
  }
  return syncFullscreenState()
}

const exitExamFullscreen = async () => {
  if (typeof document === 'undefined') return false
  if (!getFullscreenElement()) return false
  const exit = document.exitFullscreen || document.webkitExitFullscreen
  if (!exit) return false
  try {
    await exit.call(document)
    return true
  } catch {
    return false
  }
}

const syncAttemptStatus = async () => {
  if (!attemptId.value) return
  try {
    const detail = await getAttemptDetail(attemptId.value)
    applyExamConfigFromAttempt(detail)
    applyAttemptStatus(detail?.status || 'IN_PROGRESS')
    if (typeof detail?.riskScore === 'number') examSessionStore.setRiskState({ riskScore: detail.riskScore, riskLevel: detail.riskLevel || examSessionStore.riskState.riskLevel, status: detail.status || attemptStatus.value })
    if (typeof detail?.remainingSeconds === 'number' && detail.remainingSeconds >= 0) {
      const diff = Math.abs(remainingSeconds.value - detail.remainingSeconds)
      if (diff > 5) remainingSeconds.value = Math.max(0, detail.remainingSeconds)
    }
    if (!isPracticeExam.value) {
      await refreshPhaseOneRisk()
      void checkMultiMonitor()
    }
  } catch { /* ignore */ }
}

const createAnswerSignature = (question, value) => {
  const normalized = normalizeAnswerIdsForQuestion(question, value)
  return JSON.stringify(normalized)
}

const connectProctorRealtime = async () => {
  if (isPracticeExam.value || !attemptId.value) return
  await realtimeChannel.connect({
    reconnectDelay: 5000,
    topics: [{
      destination: `/topic/attempts/${attemptId.value}/proctor-actions`,
      handler: handleRealtimeProctorMessage
    }]
  })
}

const setupBlockBackButton = () => {
  history.pushState({ examInProgress: true }, '', window.location.href)
  blockBackHandler = () => { history.pushState({ examInProgress: true }, '', window.location.href); toast.warning('Không thể quay lại khi đang làm bài thi.') }
  window.addEventListener('popstate', blockBackHandler)
}
const teardownBlockBackButton = () => { if (blockBackHandler) { window.removeEventListener('popstate', blockBackHandler); blockBackHandler = null } }

const handleVisibilityChange = () => {
  if (document.hidden) {
    hiddenStartedAt.value = Date.now()
    void reportViolation('TAB_SWITCH', 'Rời tab làm bài', VIOLATION_COOLDOWN_MS, { hidden: true })
    return
  }
  const hiddenDurationMs = hiddenStartedAt.value ? Math.max(0, Date.now() - hiddenStartedAt.value) : null
  hiddenStartedAt.value = null
  if (hiddenDurationMs && hiddenDurationMs >= 30_000) {
    void reportViolation('LONG_SCREEN_LEAVE', `Rời màn hình ${Math.round(hiddenDurationMs / 1000)}s`, LONG_VIOLATION_COOLDOWN_MS, { hiddenDurationMs })
  }
}

const handleWindowBlur = () => {
  clearBlurGraceTimer()
  focusLostAt.value = Date.now()
  blurGraceTimer = window.setTimeout(() => {
    void reportViolation('WINDOW_BLUR', 'Mất focus cửa sổ', VIOLATION_COOLDOWN_MS, { blur: true })
  }, BLUR_GRACE_MS)
}
const handleWindowFocus = () => {
  const blurDurationMs = focusLostAt.value ? Math.max(0, Date.now() - focusLostAt.value) : null
  focusLostAt.value = null
  clearBlurGraceTimer()
  if (blurDurationMs && blurDurationMs >= 30_000) {
    void reportViolation('LONG_SCREEN_LEAVE', `Mất focus ${Math.round(blurDurationMs / 1000)}s`, LONG_VIOLATION_COOLDOWN_MS, { blurDurationMs, focusRecoveryDelayMs: blurDurationMs })
  }
}

const handleFullscreenChange = () => {
  const wasFullscreen = isFullscreenActive.value
  const inFs = syncFullscreenState()
  if (isPracticeExam.value) return
  if (allowConfirmedLeave.value) {
    showFullscreenPrompt.value = false
    return
  }
  if (inFs) {
    void checkMultiMonitor()
    return
  }
  if (!wasFullscreen) return
  fullscreenExitStartedAt.value = Date.now()
  fullscreenExitCount.value += 1
  void reportViolation('EXIT_FULLSCREEN', 'Thoát toàn màn hình', VIOLATION_COOLDOWN_MS, {
    fullscreenExitCount: fullscreenExitCount.value,
    fullscreenReentryDelayMs: null
  })
}

const handleCopyPaste = (event) => {
  const target = event?.target
  if (target && (target.tagName === 'INPUT' || target.tagName === 'TEXTAREA')) return
  const txt = event?.clipboardData?.getData?.('text') || ''
  const summary = txt.length > 50 ? `${event.type} ${txt.length} ký tự` : `Phát hiện ${event.type}`
  void reportViolation('COPY_PASTE', summary, LONG_VIOLATION_COOLDOWN_MS, {
    clipboardAction: event.type,
    pasteLength: event.type === 'paste' ? txt.length : null,
    copiedTextLength: event.type === 'copy' || event.type === 'cut' ? txt.length : null,
    selectionSource: event.type
  })
}

const detectDevToolsOpen = () => { if (document.hidden) return false; const wg = Math.abs(window.outerWidth - window.innerWidth); const hg = Math.abs(window.outerHeight - window.innerHeight); return wg > DEVTOOLS_GAP_PX || hg > DEVTOOLS_GAP_PX }

const selectQuestion = (index) => {
  if (isSuspended.value) return
  currentIndex.value = index
  questionEnteredAt.value = Date.now()
  const q = questions.value[index]
  if (q?.id != null) { markVisited(q.id); examSessionStore.setCurrentQuestion(q.id) }
}

const goPrevious = () => {
  if (isSuspended.value) return
  if (currentIndex.value > 0) {
    currentIndex.value -= 1
    questionEnteredAt.value = Date.now()
    const q = questions.value[currentIndex.value]
    if (q?.id != null) { markVisited(q.id); examSessionStore.setCurrentQuestion(q.id) }
  }
}

const goNext = () => {
  if (isSuspended.value) return
  if (currentIndex.value < questions.value.length - 1) {
    currentIndex.value += 1
    questionEnteredAt.value = Date.now()
    const q = questions.value[currentIndex.value]
    if (q?.id != null) { markVisited(q.id); examSessionStore.setCurrentQuestion(q.id) }
  }
}

const handleRightClick = (e) => { if (examConfig.value.monitorRightClick !== false) { e.preventDefault(); e.stopPropagation(); void reportViolation('RIGHT_CLICK', 'Chuột phải bị chặn', VIOLATION_COOLDOWN_MS) } }
const handlePrintScreen = (e) => { if (examConfig.value.monitorPrintScreen !== false && (e.key === 'PrintScreen' || e.keyCode === 44)) { e.preventDefault(); void reportViolation('PRINT_SCREEN', 'Phát hiện phím Print Screen', LONG_VIOLATION_COOLDOWN_MS) } }

const checkMultiMonitor = async () => {
  if (examConfig.value.monitorMultiMonitor === false) return false
  const result = await detectMultiMonitor()
  lastMultiMonitorEvidence.value = result.evidence
  if (!result.detected) {
    multiMonitorDetected.value = false
    return false
  }
  if (multiMonitorDetected.value) return true
  multiMonitorDetected.value = true
  void reportViolation('MULTI_MONITOR', 'Phát hiện nhiều màn hình', LONG_VIOLATION_COOLDOWN_MS, {
    ...result.evidence,
    detected: true
  })
  return true
}

const handleNetworkOffline = () => {
  isOnline.value = false
  offlineStartedAt.value = Date.now()
}

const handleNetworkOnline = () => {
  isOnline.value = true
  reconnectCount.value += 1
  lastOfflineDurationMs.value = offlineStartedAt.value ? Math.max(0, Date.now() - offlineStartedAt.value) : 0
  offlineStartedAt.value = null
}

const handleViewportChange = () => {
  if (isPracticeExam.value || isSuspended.value) return
  void checkMultiMonitor()
}

const persistDraftToServer = async () => {
  if (!attemptId.value || isSuspended.value) return
  const payload = Object.entries(answers.value).filter(([, v]) => hasAnswerValue(v)).map(([questionId, selectedAnswer]) => {
    const q = questionById.value.get(Number(questionId))
    return { questionId: Number(questionId), selectedAnswer: serializeAnswerValue(q, selectedAnswer) }
  })
  await saveDraftAnswers(attemptId.value, payload)
}

const { saveStatus, lastSavedAt, hasPendingChanges, schedule, forceSave, mergeLocalIntoAnswers } = useAutoSaveDraft({ getAnswers: () => answers.value, getAttemptId: () => attemptId.value, saveToServer: persistDraftToServer, debounceMs: 30000 })

const saveStatusLabel = computed(() => {
  if (hasPendingChanges.value && saveStatus.value === 'idle') return 'Có thay đổi chưa đồng bộ'
  switch (saveStatus.value) {
    case 'saving': return 'Đang lưu...'
    case 'saved': return lastSavedAt.value ? `Đã lưu ${new Date(lastSavedAt.value).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}` : 'Đã lưu'
    case 'error': return 'Lỗi lưu'
    default: return lastSavedAt.value ? `Đã lưu ${new Date(lastSavedAt.value).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}` : ''
  }
})

const onSelectAnswer = (questionId, selectedAnswer) => {
  if (isSuspended.value) return
  const q = questionById.value.get(Number(questionId))
  const type = q ? normalizeQuestionType(q) : 'SINGLE_CHOICE'
  const stored =
    type === 'SINGLE_CHOICE'
      ? (selectedAnswer != null && selectedAnswer !== '' ? String(selectedAnswer) : '')
      : selectedAnswer
  const questionKey = String(questionId)
  const nextSignature = createAnswerSignature(q, stored)
  answerSignatureByQuestion.value = {
    ...answerSignatureByQuestion.value,
    [questionKey]: nextSignature
  }
  answers.value[questionId] = stored
  markVisited(questionId)
  examSessionStore.setAnswer(questionId, stored)
  schedule()
}

const toggleMarkCurrentQuestion = () => {
  const q = currentQuestion.value
  if (!q) return
  const key = String(q.id)
  markedQuestions.value[key] = !markedQuestions.value[key]
  examSessionStore.toggleMarked(q.id)
}

const manualSaveDraft = async () => {
  try {
    await forceSave()
    showModal({ type: 'success', title: 'Thành công', message: 'Đã lưu bài làm thành công!', confirmLabel: 'Đóng' })
  } catch {
    showModal({ type: 'error', title: 'Lỗi', message: 'Không lưu được. Vui lòng thử lại.', confirmLabel: 'Đóng' })
  }
}

const buildSubmitPayload = () => Object.entries(answers.value).filter(([, v]) => hasAnswerValue(v)).map(([questionId, selectedAnswer]) => {
  const q = questionById.value.get(Number(questionId))
  return { questionId: Number(questionId), selectedAnswer: serializeAnswerValue(q, selectedAnswer) }
})

const openSubmitModal = () => {
  if (isSuspended.value) return
  closeAllTransientModals()
  showSubmitModal.value = true
}

const autoSubmitOnTimeUp = async () => {
  if (!attemptId.value || isSuspended.value || isSubmitting.value) return
  isSubmitting.value = true
  try { await forceSave() } catch { /* ignore */ }
  toast.info('Hết giờ. Đang nộp...')
  try {
    const result = await submitAttempt(attemptId.value, buildSubmitPayload())
    showSubmitModal.value = false
    allowConfirmedLeave.value = true
    await exitExamFullscreen()
    router.push({ path: '/student/submission-confirmation', query: buildSubmissionQuery({ examTitle: examTitle.value, attemptId: attemptId.value, score: result?.showScoreAfterSubmit === false ? '' : Math.round(Number(result?.score || 0)), submittedAt: result?.submittedAt || '', showScoreAfterSubmit: result?.showScoreAfterSubmit !== false, allowReviewAfterSubmit: result?.allowReviewAfterSubmit !== false }) })
  } catch {
    showSubmitModal.value = false
    showModal({ type: 'error', title: 'Lỗi nộp bài', message: 'Không nộp tự động được. Vui lòng thử nộp lại.', confirmLabel: 'Đóng' })
  } finally { isSubmitting.value = false }
}

const submitExamAction = async () => {
  if (!attemptId.value || isSuspended.value) return
  isSubmitting.value = true
  try { await forceSave() } catch { /* ignore */ }
  try {
    const result = await submitAttempt(attemptId.value, buildSubmitPayload())
    showSubmitModal.value = false
    allowConfirmedLeave.value = true
    await exitExamFullscreen()
    router.push({ path: '/student/submission-confirmation', query: buildSubmissionQuery({ examTitle: examTitle.value, attemptId: attemptId.value, score: result?.showScoreAfterSubmit === false ? '' : Math.round(Number(result?.score || 0)), submittedAt: result?.submittedAt || '', showScoreAfterSubmit: result?.showScoreAfterSubmit !== false, allowReviewAfterSubmit: result?.allowReviewAfterSubmit !== false }) })
  } catch {
    showSubmitModal.value = false
    showModal({ type: 'error', title: 'Lỗi nộp bài', message: 'Không nộp được. Vui lòng thử nộp lại.', confirmLabel: 'Đóng' })
  } finally { isSubmitting.value = false }
}

const questionButtonClass = (index) => {
  const q = questions.value[index]
  if (!q) return 'ei-q-grid-btn--default'
  const key = String(q.id)
  const answered = hasAnswerValue(answers.value[key])
  const marked = Boolean(markedQuestions.value[key])
  const visited = Boolean(visitedQuestions.value[key])
  if (index === currentIndex.value) return 'ei-q-grid-btn--active'
  if (marked && answered) return 'ei-q-grid-btn--mark-done'
  if (marked) return 'ei-q-grid-btn--mark'
  if (answered) return 'ei-q-grid-btn--done'
  if (visited) return 'ei-q-grid-btn--skip'
  return 'ei-q-grid-btn--default'
}

const startTimer = () => {
  if (timerId) clearInterval(timerId)
  timerId = setInterval(() => {
    if (isSuspended.value) return
    if (remainingSeconds.value > 0) {
      remainingSeconds.value -= 1
      if (remainingSeconds.value === 0) void autoSubmitOnTimeUp()
    }
  }, 1000)
}

const restoreSession = async () => {
  if (!attemptId.value) return
  const draft = await getDraftAnswers(attemptId.value)
  if (!draft?.answers?.length) return
  for (const item of draft.answers) {
    const q = questionById.value.get(Number(item.questionId))
    if (q) {
      answers.value[item.questionId] = deserializeAnswerValue(q, item.selectedAnswer)
      answerSignatureByQuestion.value = {
        ...answerSignatureByQuestion.value,
        [String(item.questionId)]: createAnswerSignature(q, answers.value[item.questionId])
      }
    }
  }
}

const loadExam = async () => {
  if (!examId.value) return
  examLoadFailed.value = false
  examSurfaceReady.value = false
  try {
    const data = await listExamQuestions(examId.value, { attemptId: attemptId.value })
    questionList = Array.isArray(data) ? data : (data?.questions || [])
    questions.value = questionList.map(q => ({
      ...q,
      options: parseQuestionOptions(q.options)
    }))
    examSurfaceReady.value = true
    if (attemptId.value) void restoreSession()
  } catch {
    examLoadFailed.value = true
    examSurfaceReady.value = true
  }
}

const attachEventListeners = () => {
  document.addEventListener('visibilitychange', handleVisibilityChange)
  window.addEventListener('blur', handleWindowBlur)
  window.addEventListener('focus', handleWindowFocus)
  document.addEventListener('fullscreenchange', handleFullscreenChange)
  document.addEventListener('webkitfullscreenchange', handleFullscreenChange)

  document.addEventListener('contextmenu', handleRightClick)
  document.addEventListener('keydown', handlePrintScreen)
  document.addEventListener('copy', handleCopyPaste)
  document.addEventListener('cut', handleCopyPaste)
  document.addEventListener('paste', handleCopyPaste)
  window.addEventListener('offline', handleNetworkOffline)
  window.addEventListener('online', handleNetworkOnline)
  window.addEventListener('resize', handleViewportChange)
  window.addEventListener('orientationchange', handleViewportChange)
}

const removeEventListeners = () => {
  document.removeEventListener('visibilitychange', handleVisibilityChange)
  window.removeEventListener('blur', handleWindowBlur)
  window.removeEventListener('focus', handleWindowFocus)
  document.removeEventListener('fullscreenchange', handleFullscreenChange)
  document.removeEventListener('webkitfullscreenchange', handleFullscreenChange)

  document.removeEventListener('contextmenu', handleRightClick)
  document.removeEventListener('keydown', handlePrintScreen)
  document.removeEventListener('copy', handleCopyPaste)
  document.removeEventListener('cut', handleCopyPaste)
  document.removeEventListener('paste', handleCopyPaste)
  window.removeEventListener('offline', handleNetworkOffline)
  window.removeEventListener('online', handleNetworkOnline)
  window.removeEventListener('resize', handleViewportChange)
  window.removeEventListener('orientationchange', handleViewportChange)
}

onMounted(async () => {
  await loadExam()
  attachEventListeners()
  setupBlockBackButton()
  if (!isPracticeExam.value) {
    await syncAttemptStatus()
    syncFullscreenState()
    void checkMultiMonitor()
    if (!isSuspended.value) {
      await enforceDeviceAccess()
    }
    if (attemptId.value && examId.value) {
      await startPhaseOneProctoring()
    }
    void connectProctorRealtime()
    attemptStatusTimer = setInterval(syncAttemptStatus, 30000)
  }

  startHeartbeat()
  startTimer()

})

onUnmounted(() => {
  removeEventListeners()
  teardownBlockBackButton()
  stopAiFrameSampler()
  stopCameraFrameStreamer()
  stopIdentityRecheckSampler()
  stopMediaStream()
  if (timerId) clearInterval(timerId)
  if (attemptStatusTimer) clearInterval(attemptStatusTimer)
  stopHeartbeat()
  flushQueuedViolations()
  realtimeChannel.disconnect()
})

onBeforeRouteLeave(() => {
  if (!allowConfirmedLeave.value) {
    closeAllTransientModals()
    showLeaveConfirm.value = true
    return false
  }
})

const onConfirmLeave = async () => {
  allowConfirmedLeave.value = true
  showLeaveConfirm.value = false
  await exitExamFullscreen()
  router.push('/student/dashboard')
}

const onCancelLeave = () => {
  showLeaveConfirm.value = false
}
</script>

<style scoped>
/* ── Topbar ─────────────────────────────────────────── */
.ei-topbar {
  position: sticky;
  top: 0;
  z-index: 50;
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.5rem 1.25rem;
  background: var(--ds-surface);
  border-bottom: 1px solid var(--ds-border);
  box-shadow: var(--ds-shadow-sm);
}

.ei-topbar__left {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex: 1;
  min-width: 0;
}

.ei-topbar__exam-chip {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  padding: 0.3rem 0.75rem;
  background: var(--ds-primary-soft);
  color: var(--ds-primary);
  border-radius: var(--ds-radius-full);
  font-size: 0.78rem;
  font-weight: 700;
  flex-shrink: 0;
}

.ei-topbar__exam-title {
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ei-topbar__device-badges {
  display: flex;
  gap: 0.375rem;
  flex-wrap: wrap;
}

.ei-device-badge {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  padding: 0.2rem 0.5rem;
  border-radius: var(--ds-radius-full);
  font-size: 0.65rem;
  font-weight: 700;
}

.ei-device-badge--ok {
  background: var(--ds-success-soft);
  color: var(--ds-success);
}

.ei-device-badge--fail {
  background: var(--ds-danger-soft);
  color: var(--ds-danger);
}

.ei-device-badge--warn {
  background: var(--ds-warning-soft);
  color: var(--ds-warning);
}

.ei-topbar__timer {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.375rem 0.875rem;
  background: var(--ds-gray-50);
  border: 1.5px solid var(--ds-border);
  border-radius: var(--ds-radius-xl);
  flex-shrink: 0;
}

.ei-timer--warn { border-color: var(--ds-warning); background: var(--ds-warning-soft); color: var(--ds-warning); }
.ei-timer--danger { border-color: var(--ds-danger); background: var(--ds-danger-soft); color: var(--ds-danger); }
.ei-timer--pulse { animation: eiPulse 1s ease-in-out infinite; }
@keyframes eiPulse { 0%, 100% { opacity: 1; } 50% { opacity: 0.6; } }

.ei-topbar__timer-icon { color: var(--ds-text-muted); }
.ei-timer--warn .ei-topbar__timer-icon { color: var(--ds-warning); }
.ei-timer--danger .ei-topbar__timer-icon { color: var(--ds-danger); }

.ei-topbar__timer-digits {
  display: flex;
  align-items: baseline;
  gap: 0.125rem;
  font-variant-numeric: tabular-nums;
}

.ei-timer-unit {
  display: flex;
  align-items: baseline;
  gap: 0.125rem;
}

.ei-timer-val {
  font-family: var(--ds-font-display);
  font-size: 1rem;
  font-weight: 900;
  color: var(--ds-text);
  line-height: 1;
  min-width: 1.5ch;
  text-align: center;
}

.ei-timer-lbl {
  font-size: 0.6rem;
  font-weight: 600;
  color: var(--ds-text-muted);
  min-width: 0.5ch;
}

.ei-timer--warn .ei-timer-val { color: var(--ds-warning); }
.ei-timer--danger .ei-timer-val { color: var(--ds-danger); }

.ei-topbar__timer-progress {
  width: 48px;
  height: 4px;
  background: var(--ds-gray-100);
  border-radius: 2px;
  overflow: hidden;
}

.ei-topbar__timer-progress-fill {
  height: 100%;
  background: var(--ds-primary);
  border-radius: 2px;
  transition: width 1s linear;
}

.ei-timer--warn .ei-topbar__timer-progress-fill { background: var(--ds-warning); }
.ei-timer--danger .ei-topbar__timer-progress-fill { background: var(--ds-danger); }

.ei-topbar__right {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex-shrink: 0;
}

.ei-save-status {
  display: flex;
  align-items: center;
  gap: 0.3rem;
  font-size: 0.68rem;
  color: var(--ds-text-muted);
}

@keyframes eiSpin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }
.ei-save-icon--spin { animation: eiSpin 1s linear infinite; }

.ei-save-label { white-space: nowrap; }

.ei-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.375rem;
  padding: 0.4rem 0.875rem;
  border-radius: var(--ds-radius-xl);
  border: 1.5px solid var(--ds-border);
  font-size: 0.78rem;
  font-weight: 700;
  cursor: pointer;
  font-family: inherit;
  transition: color 0.15s ease, background-color 0.15s ease, border-color 0.15s ease, box-shadow 0.15s ease, transform 0.15s ease;
}
.ei-btn:disabled { opacity: 0.5; cursor: not-allowed; }

.ei-btn--save {
  background: var(--ds-surface);
  border-color: var(--ds-border);
  color: var(--ds-text-secondary);
}
.ei-btn--save:hover:not(:disabled) {
  border-color: var(--ds-primary-border);
  color: var(--ds-primary);
  background: var(--ds-primary-soft);
  transform: translateY(-1px);
}

.ei-btn--submit {
  background: var(--ds-success);
  color: white;
  border-color: var(--ds-success);
  box-shadow: 0 2px 12px rgba(22, 163, 74, 0.3);
}
.ei-btn--submit:hover:not(:disabled) {
  background: var(--ds-success-hover);
  transform: translateY(-1px);
  box-shadow: 0 4px 16px rgba(22, 163, 74, 0.4);
}

/* ── Fullscreen warning ──── */
.ei-fs-warn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.75rem;
  padding: 0.6rem 1.25rem;
  background: var(--ds-warning-soft);
  border-bottom: 1px solid rgba(217, 119, 6, 0.3);
  font-size: 0.78rem;
  font-weight: 600;
  color: var(--ds-warning);
}

.ei-fs-warn__btn {
  padding: 0.25rem 0.75rem;
  border-radius: var(--ds-radius-full);
  border: 1.5px solid var(--ds-warning);
  background: transparent;
  color: var(--ds-warning);
  font-size: 0.72rem;
  font-weight: 700;
  cursor: pointer;
  font-family: inherit;
  transition: background 0.15s ease, color 0.15s ease;
}
.ei-fs-warn__btn:hover { background: var(--ds-warning); color: white; }

.ei-modal__header {
  display: flex;
  align-items: flex-start;
  gap: 0.875rem;
}

.ei-modal__icon {
  width: 44px;
  height: 44px;
  border-radius: var(--ds-radius-xl);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.ei-modal__header-copy {
  min-width: 0;
}

.ei-modal__icon--warning { background: var(--ds-warning-soft); color: var(--ds-warning); }
.ei-modal__icon--error { background: var(--ds-danger-soft); color: var(--ds-danger); }
.ei-modal__icon--success { background: var(--ds-success-soft); color: var(--ds-success); }
.ei-modal__icon--info { background: var(--ds-primary-soft); color: var(--ds-primary); }

.ei-modal__title {
  font-family: var(--ds-font-display);
  font-size: 1.05rem;
  font-weight: 800;
  color: var(--ds-text);
  margin: 0;
}

.dark .ei-modal__title {
  color: #f1f5f9;
}

.ei-modal__sub {
  margin: 0.25rem 0 0;
  color: var(--ds-text-muted);
  font-size: 0.75rem;
}

.ei-modal__body {
  display: flex;
  flex-direction: column;
}

.ei-modal__msg {
  font-size: 0.875rem;
  color: var(--ds-text);
  line-height: 1.6;
  padding: 0.875rem;
  background: var(--ds-gray-50);
  border: 1px solid var(--ds-border);
  border-radius: var(--ds-radius-xl);
  margin: 0;
}

.ei-modal__actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.625rem;
  width: 100%;
}

.ei-modal__cancel {
  padding: 0.6rem 1.125rem;
  border-radius: var(--ds-radius-xl);
  border: 1.5px solid var(--ds-border);
  background: var(--ds-surface);
  color: var(--ds-text-secondary);
  font-size: 0.85rem;
  font-weight: 600;
  cursor: pointer;
  font-family: inherit;
  transition: border-color 0.15s ease, color 0.15s ease;
}
.ei-modal__cancel:hover { border-color: var(--ds-primary); color: var(--ds-primary); }

.ei-modal__confirm {
  padding: 0.6rem 1.125rem;
  border-radius: var(--ds-radius-xl);
  border: none;
  font-size: 0.85rem;
  font-weight: 700;
  color: white;
  cursor: pointer;
  font-family: inherit;
  transition: filter 0.15s ease, transform 0.15s ease;
}
.ei-modal__confirm:hover { filter: brightness(1.1); transform: translateY(-1px); }
.ei-modal__confirm--warning { background: var(--ds-warning); }
.ei-modal__confirm--error { background: var(--ds-danger); }
.ei-modal__confirm--success { background: var(--ds-success); }
.ei-modal__confirm--info { background: var(--ds-primary); }

@media (max-width: 520px) {
  .ei-modal__actions {
    flex-direction: column-reverse;
  }

  .ei-modal__cancel,
  .ei-modal__confirm {
    width: 100%;
  }
}

/* ── Suspended Overlay ──── */
.ei-suspend-overlay {
  position: fixed;
  inset: 0;
  z-index: 200;
  background: rgba(15, 23, 42, 0.7);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 1.5rem;
}

.ei-suspend-card {
  max-width: 440px;
  width: 100%;
  text-align: center;
  padding: 2.5rem 2rem;
  background: var(--ds-surface);
  border: 2px solid var(--ds-danger);
  border-radius: var(--ds-radius-2xl);
  box-shadow: 0 20px 60px rgba(0,0,0,0.3);
}

.ei-suspend-icon {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  background: var(--ds-danger-soft);
  color: var(--ds-danger);
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 1.25rem;
}

.ei-suspend-title {
  font-family: var(--ds-font-display);
  font-size: 1.35rem;
  font-weight: 900;
  color: var(--ds-text);
  margin: 0 0 0.75rem;
}

.ei-suspend-desc {
  font-size: 0.875rem;
  color: var(--ds-text-muted);
  margin: 0;
  line-height: 1.6;
}

/* ── Main Layout ──── */
.ei-main {
  flex: 1;
  display: grid;
  grid-template-columns: 1fr 280px;
  gap: 0.875rem;
  padding: 1rem 1.25rem;
  max-width: 1200px;
  width: 100%;
  margin: 0 auto;
  align-items: start;
}

@media (max-width: 900px) {
  .ei-main { grid-template-columns: 1fr; }
  .ei-sidebar { order: -1; }
}

/* ── Empty States ──── */
.ei-empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 0.625rem;
  padding: 3.5rem 2rem;
  text-align: center;
  background: var(--ds-surface);
  border: 1.5px dashed var(--ds-border);
  border-radius: var(--ds-radius-2xl);
  grid-column: 1 / -1;
}

.ei-empty-icon { color: var(--ds-text-muted); opacity: 0.5; }
.ei-empty-title { font-size: 0.95rem; font-weight: 700; color: var(--ds-text); margin: 0; }
.ei-empty-desc { font-size: 0.775rem; color: var(--ds-text-muted); margin: 0; max-width: 260px; }

/* ── Question Card ──── */
.ei-question-card {
  background: var(--ds-surface);
  border: 1px solid var(--ds-border);
  border-radius: var(--ds-radius-2xl);
  overflow: hidden;
  box-shadow: var(--ds-shadow-sm);
}

.ei-q-header {
  display: flex;
  align-items: center;
  gap: 0.625rem;
  padding: 0.875rem 1.125rem;
  border-bottom: 1px solid var(--ds-border);
  background: var(--ds-gray-50);
}

.ei-q-progress-ring {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.ei-q-ring-svg { transform: rotate(-90deg); }

.ei-q-ring-bg {
  fill: none;
  stroke: var(--ds-gray-200);
  stroke-width: 2.5;
}

.dark .ei-q-ring-bg { stroke: rgba(79,70,229,0.2); }

.ei-q-ring-prog {
  fill: none;
  stroke: var(--ds-primary);
  stroke-width: 2.5;
  stroke-linecap: round;
  transition: stroke-dashoffset 0.3s ease;
}

.ei-q-ring-label {
  position: absolute;
  font-family: var(--ds-font-display);
  font-size: 0.6rem;
  font-weight: 900;
  color: var(--ds-primary);
}

.ei-q-badge {
  font-size: 0.75rem;
  font-weight: 700;
  color: var(--ds-text-muted);
}

.ei-mark-btn {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 0.3rem;
  padding: 0.3rem 0.75rem;
  border: 1.5px solid var(--ds-border);
  background: var(--ds-surface);
  border-radius: var(--ds-radius-full);
  font-size: 0.72rem;
  font-weight: 600;
  color: var(--ds-text-muted);
  cursor: pointer;
  font-family: inherit;
  transition: border-color 0.15s ease, color 0.15s ease, background 0.15s ease;
}
.ei-mark-btn:hover:not(:disabled) { border-color: var(--ds-warning); color: var(--ds-warning); background: var(--ds-warning-soft); }
.ei-mark-btn:disabled { opacity: 0.5; cursor: not-allowed; }

.ei-q-content { padding: 1.25rem 1.125rem 0.875rem; }

.ei-q-text {
  font-size: 1.05rem;
  font-weight: 700;
  color: var(--ds-text);
  line-height: 1.6;
  margin: 0;
}

.ei-q-text :deep(.katex) { font-size: 1.05em; }
.ei-q-text :deep(.katex-display) { margin: 0.75em 0; overflow-x: auto; }

.ei-q-answer { padding: 0 1.125rem 0.875rem; }

.ei-q-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  padding: 0.875rem 1.125rem;
  border-top: 1px solid var(--ds-border);
  background: var(--ds-gray-50);
}

.ei-nav-btn {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  padding: 0.5rem 1rem;
  border-radius: var(--ds-radius-xl);
  font-size: 0.78rem;
  font-weight: 700;
  cursor: pointer;
  font-family: inherit;
  transition: all 0.15s ease;
  border: 1.5px solid var(--ds-border);
}
.ei-nav-btn:disabled { opacity: 0.4; cursor: not-allowed; }
.ei-nav-btn--prev { background: var(--ds-surface); color: var(--ds-text-secondary); }
.ei-nav-btn--prev:hover:not(:disabled) { border-color: var(--ds-primary); color: var(--ds-primary); }
.ei-nav-btn--next { background: var(--ds-primary); color: white; border-color: var(--ds-primary); }
.ei-nav-btn--next:hover:not(:disabled) { background: var(--ds-primary-hover); }

/* ── Sidebar ──── */
.ei-sidebar {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  position: sticky;
  top: 72px;
}

.ei-cam-card {
  background: var(--ds-surface);
  border: 1px solid var(--ds-border);
  border-radius: var(--ds-radius-2xl);
  padding: 0.75rem;
  box-shadow: var(--ds-shadow-sm);
}

.ei-cam-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
}

.ei-cam-header__copy {
  display: flex;
  flex-direction: column;
  min-width: 0;
  gap: 0.1rem;
}

.ei-cam-label { font-size: 0.68rem; font-weight: 700; color: var(--ds-text-muted); }
.ei-cam-mode {
  font-size: 0.72rem;
  font-weight: 800;
  color: var(--ds-text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.ei-cam-actions { display: flex; gap: 0.3rem; }

.ei-cam-btn {
  width: 26px;
  height: 26px;
  border-radius: var(--ds-radius-md);
  border: none;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.15s ease;
}
.ei-cam-btn--on { background: var(--ds-success-soft); color: var(--ds-success); }
.ei-cam-btn--off { background: var(--ds-gray-100); color: var(--ds-text-muted); }
.ei-cam-btn--on:hover { background: rgba(22,163,74,0.2); }
.ei-cam-btn--off:hover { background: var(--ds-gray-200); }

.ei-cam-status {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.35rem;
  margin-top: 0.65rem;
}

.ei-cam-status__chip {
  display: inline-flex;
  align-items: center;
  min-height: 24px;
  padding: 0.2rem 0.48rem;
  border-radius: var(--ds-radius-full);
  font-size: 0.62rem;
  font-weight: 800;
  line-height: 1.2;
}

.ei-cam-status__chip--on {
  background: var(--ds-success-soft);
  color: var(--ds-success);
}

.ei-cam-status__chip--off {
  background: var(--ds-danger-soft);
  color: var(--ds-danger);
}

.ei-cam-status__chip--info {
  background: var(--ds-info-soft);
  color: var(--ds-info);
}

.ei-cam-transport {
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
  margin-top: 0.65rem;
  padding: 0.5rem 0.6rem;
  border-radius: var(--ds-radius-lg);
  background: var(--ds-gray-50);
  font-size: 0.68rem;
  line-height: 1.35;
}

.ei-cam-transport__label {
  font-weight: 800;
  color: var(--ds-text);
}

.ei-cam-transport__detail {
  color: var(--ds-text-muted);
  word-break: break-word;
}

.ei-cam-transport--ok {
  background: var(--ds-success-soft);
}

.ei-cam-transport--pending,
.ei-cam-transport--captured {
  background: var(--ds-warning-soft);
}

.ei-cam-transport--fail {
  background: var(--ds-danger-soft);
}

.ei-cam-transport--idle {
  background: var(--ds-gray-50);
}

.ei-camera-capture {
  position: fixed;
  left: -9999px;
  top: 0;
  width: 1px;
  height: 1px;
  overflow: hidden;
  opacity: 0;
  pointer-events: none;
}

.ei-camera-capture__video {
  width: 1px;
  height: 1px;
}

/* Progress card */
.ei-prog-card {
  background: var(--ds-surface);
  border: 1px solid var(--ds-border);
  border-radius: var(--ds-radius-2xl);
  padding: 0.75rem 0.875rem;
  box-shadow: var(--ds-shadow-sm);
}

.ei-prog-header {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  margin-bottom: 0.5rem;
  color: var(--ds-text-secondary);
}

.ei-prog-title { font-size: 0.7rem; font-weight: 700; color: var(--ds-text-muted); flex: 1; }
.ei-prog-pct { font-family: var(--ds-font-display); font-size: 1rem; font-weight: 900; color: var(--ds-primary); }

.ei-prog-bar {
  height: 5px;
  background: var(--ds-gray-100);
  border-radius: 3px;
  overflow: hidden;
  margin-bottom: 0.5rem;
}
.ei-prog-bar__fill { height: 100%; background: var(--ds-primary); border-radius: 3px; transition: width 0.5s ease; }

.ei-prog-stats {
  display: flex;
  gap: 0.625rem;
}

.ei-prog-stat {
  display: flex;
  align-items: center;
  gap: 0.3rem;
  font-size: 0.68rem;
  font-weight: 600;
  color: var(--ds-text-muted);
}

.ei-prog-dot { width: 7px; height: 7px; border-radius: 50%; }
.ei-prog-dot--done { background: var(--ds-primary); }
.ei-prog-dot--skip { background: var(--ds-gray-300); }

.ei-prog-tags { display: flex; flex-wrap: wrap; gap: 0.3rem; margin-top: 0.4rem; }

.ei-prog-tag {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  padding: 0.15rem 0.5rem;
  border-radius: var(--ds-radius-full);
  font-size: 0.62rem;
  font-weight: 700;
}
.ei-prog-tag--mark { background: var(--ds-warning-soft); color: var(--ds-warning); }

/* Question Navigator */
.ei-nav-card {
  background: var(--ds-surface);
  border: 1px solid var(--ds-border);
  border-radius: var(--ds-radius-2xl);
  padding: 0.75rem 0.875rem;
  box-shadow: var(--ds-shadow-sm);
}

.ei-nav-card__header {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  font-size: 0.7rem;
  font-weight: 700;
  color: var(--ds-text-muted);
  margin-bottom: 0.625rem;
}

.ei-nav-card__count {
  margin-left: auto;
  padding: 0.1rem 0.4rem;
  background: var(--ds-primary-soft);
  color: var(--ds-primary);
  border-radius: var(--ds-radius-full);
  font-size: 0.62rem;
}

.ei-q-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 0.3rem;
  margin-bottom: 0.5rem;
}

.ei-q-grid-btn {
  aspect-ratio: 1;
  border-radius: var(--ds-radius-lg);
  border: 1.5px solid transparent;
  font-size: 0.68rem;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.15s ease;
  display: flex;
  align-items: center;
  justify-content: center;
}
.ei-q-grid-btn--default { background: var(--ds-gray-100); color: var(--ds-text-muted); border-color: var(--ds-border); }
.ei-q-grid-btn--active { background: var(--ds-primary); color: white; border-color: var(--ds-primary); transform: scale(1.08); box-shadow: 0 0 0 2px rgba(79,70,229,0.3); }
.ei-q-grid-btn--done { background: var(--ds-primary); color: white; border-color: var(--ds-primary); }
.ei-q-grid-btn--mark { background: var(--ds-warning-soft); color: var(--ds-warning); border-color: rgba(245,158,11,0.4); }
.ei-q-grid-btn--mark-done { background: var(--ds-warning); color: white; border-color: var(--ds-warning); }
.ei-q-grid-btn--skip { background: var(--ds-gray-200); color: var(--ds-text-secondary); border-color: var(--ds-gray-300); }
.ei-q-grid-btn:hover:not(:disabled) { transform: scale(1.1); }

.ei-q-legend {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.ei-legend-item {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  font-size: 0.6rem;
  font-weight: 600;
  color: var(--ds-text-muted);
}

.ei-legend-dot { width: 7px; height: 7px; border-radius: 50%; }
.ei-legend-dot--done { background: var(--ds-primary); }
.ei-legend-dot--mark { background: var(--ds-warning); }

/* ── Transitions ──── */
.ei-fade-enter-active, .ei-fade-leave-active { transition: opacity 0.2s ease; }
.ei-fade-enter-from, .ei-fade-leave-to { opacity: 0; }

.ei-slide-down-enter-active, .ei-slide-down-leave-active { transition: transform 0.25s ease, opacity 0.25s ease; }
.ei-slide-down-enter-from, .ei-slide-down-leave-to { transform: translateY(-100%); opacity: 0; }

.ei-q-slide-enter-active { transition: opacity 0.25s ease, transform 0.25s ease; }
.ei-q-slide-leave-active { transition: opacity 0.15s ease, transform 0.15s ease; }
.ei-q-slide-enter-from { opacity: 0; transform: translateX(20px); }
.ei-q-slide-leave-to { opacity: 0; transform: translateX(-20px); }

@media (prefers-reduced-motion: reduce) {
  * { transition-duration: 0.01ms !important; animation-duration: 0.01ms !important; }
}
</style>
