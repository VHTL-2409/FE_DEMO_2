<template>
  <div class="ei-root">
    <!-- Fixed Top Bar -->
    <div class="ei-topbar">
      <div class="ei-topbar__left">
        <!-- Exam title chip -->
        <div class="ei-topbar__exam-chip">
          <LucideIcon name="quiz" size="15" />
          <span class="ei-topbar__exam-title">{{ examTitle }}</span>
        </div>
        <!-- Device status badges -->
        <div class="ei-topbar__device-badges">
          <div class="ei-device-badge" :class="cameraReady ? 'ei-device-badge--ok' : 'ei-device-badge--fail'">
            <LucideIcon :name="cameraReady ? 'videocam' : 'videocam_off'" size="12" />
            <span>Camera</span>
          </div>
          <div class="ei-device-badge" :class="micReady ? 'ei-device-badge--ok' : 'ei-device-badge--fail'">
            <LucideIcon :name="micReady ? 'mic' : 'mic_off'" size="12" />
            <span>Mic</span>
          </div>
          <div class="ei-device-badge ei-device-badge--ok">
            <LucideIcon name="wifi" size="12" />
            <span>Kết nối</span>
          </div>
        </div>
      </div>

      <!-- Timer — Rectangular countdown -->
      <div class="ei-topbar__timer" :class="timerBarAccentClass">
        <LucideIcon name="timer" size="16" class="ei-topbar__timer-icon" />
        <div class="ei-topbar__timer-digits">
          <span class="ei-timer-unit">
            <span class="ei-timer-val">{{ timerHours }}</span>
            <span class="ei-timer-lbl">Giờ</span>
          </span>
          <span class="ei-timer-colon">:</span>
          <span class="ei-timer-unit">
            <span class="ei-timer-val">{{ timerMinutes }}</span>
            <span class="ei-timer-lbl">Phút</span>
          </span>
          <span class="ei-timer-colon">:</span>
          <span class="ei-timer-unit">
            <span class="ei-timer-val">{{ timerSeconds }}</span>
            <span class="ei-timer-lbl">Giây</span>
          </span>
        </div>
        <!-- Progress bar below -->
        <div class="ei-topbar__timer-progress">
          <div class="ei-topbar__timer-progress-fill" :style="{ width: timerProgressWidth }" />
        </div>
      </div>

      <!-- Right actions -->
      <div class="ei-topbar__right">
        <!-- Save status -->
        <div v-if="saveStatusLabel" class="ei-save-status" role="status" aria-live="polite">
          <LucideIcon name="progress_activity" v-if="saveStatus === 'saving'" size="13" class="ei-save-icon ei-save-icon--spin" />
          <LucideIcon name="check_circle" v-else-if="saveStatus === 'saved'" size="13" class="ei-save-icon ei-save-icon--ok" />
          <LucideIcon name="schedule" v-else-if="hasPendingChanges" size="13" class="ei-save-icon ei-save-icon--pending" />
          <LucideIcon name="cloud_off" v-else-if="saveStatus === 'error'" size="13" class="ei-save-icon ei-save-icon--err" />
          <span class="ei-save-label">{{ saveStatusLabel }}</span>
        </div>

        <!-- Manual save -->
        <button type="button" class="ei-btn ei-btn--save" :disabled="isSuspended || saveStatus === 'saving'" @click="manualSaveDraft">
          <LucideIcon name="save" size="14" />
          <span>Lưu</span>
        </button>

        <!-- Submit — most prominent -->
        <button type="button" class="ei-btn ei-btn--submit" :disabled="isSuspended" @click="openSubmitModal">
          <LucideIcon name="done_all" size="15" />
          <span>Nộp bài</span>
        </button>
      </div>
    </div>

    <!-- ── Fullscreen Warning ──────────────────────────────── -->
    <Transition name="ei-slide-down">
      <div v-if="showFullscreenPrompt && !isPracticeExam && !isSuspended" class="ei-fs-warn">
        <LucideIcon name="fullscreen" size="16" />
        <span>Yêu cầu chế độ toàn màn hình</span>
        <button type="button" class="ei-fs-warn__btn" @click="requestExamFullscreen">Vào toàn màn hình</button>
      </div>
    </Transition>

    <!-- ── Suspended Overlay ──────────────────────────────── -->
    <Transition name="ei-fade">
      <div v-if="isSuspended" class="ei-suspend-overlay">
        <div class="ei-suspend-card">
          <div class="ei-suspend-icon">
            <LucideIcon name="pause_circle" size="40" />
          </div>
          <h2 class="ei-suspend-title">
            {{ attemptStatus === 'PAUSED' ? 'Phiên thi đang tạm dừng' : 'Bài thi đã bị đình chỉ' }}
          </h2>
          <p class="ei-suspend-desc">{{ suspensionMessage || 'Giám thị đã hủy hiệu lực bài thi của bạn.' }}</p>
        </div>
      </div>
    </Transition>

    <!-- ── Teacher Warning Modal ──────────────────────────── -->
    <Transition name="ei-fade">
      <div v-if="showTeacherWarningModal" class="ei-modal-overlay" @click.self="showTeacherWarningModal = false">
        <div class="ei-modal ei-modal--warn">
          <div class="ei-modal__header">
            <div class="ei-modal__icon ei-modal__icon--warn">
              <LucideIcon name="warning" size="22" />
            </div>
            <div>
              <h2 class="ei-modal__title">Cảnh báo từ giám thị</h2>
              <p class="ei-modal__sub">Vui lòng chú ý và tuân thủ quy định phòng thi.</p>
            </div>
          </div>
          <div class="ei-modal__body">
            <p class="ei-modal__msg">{{ teacherWarningMessage }}</p>
          </div>
          <button type="button" class="ei-modal__confirm" @click="showTeacherWarningModal = false">
            <LucideIcon name="check_circle" size="16" />
            Tôi đã hiểu
          </button>
        </div>
      </div>
    </Transition>

    <!-- ── Error Popup Modal ─────────────────────────────── -->
    <Transition name="ei-fade">
      <div v-if="showErrorPopup" class="ei-modal-overlay" @click.self="showErrorPopup = false">
        <div class="ei-modal ei-modal--error">
          <div class="ei-modal__header">
            <div class="ei-modal__icon ei-modal__icon--error">
              <LucideIcon name="error" size="22" />
            </div>
            <div>
              <h2 class="ei-modal__title">Đã xảy ra lỗi</h2>
            </div>
          </div>
          <div class="ei-modal__body">
            <p class="ei-modal__msg">{{ errorPopupMessage }}</p>
          </div>
          <button type="button" class="ei-modal__confirm ei-modal__confirm--error" @click="showErrorPopup = false">
            <LucideIcon name="close" size="16" />
            Đóng
          </button>
        </div>
      </div>
    </Transition>

    <!-- ── Success Popup Modal ──────────────────────────── -->
    <Transition name="ei-fade">
      <div v-if="showSuccessPopup" class="ei-modal-overlay" @click.self="showSuccessPopup = false">
        <div class="ei-modal ei-modal--success">
          <div class="ei-modal__header">
            <div class="ei-modal__icon ei-modal__icon--success">
              <LucideIcon name="check_circle" size="22" />
            </div>
            <div>
              <h2 class="ei-modal__title">Thành công</h2>
            </div>
          </div>
          <div class="ei-modal__body">
            <p class="ei-modal__msg">{{ successPopupMessage }}</p>
          </div>
          <button type="button" class="ei-modal__confirm ei-modal__confirm--success" @click="showSuccessPopup = false">
            <LucideIcon name="check" size="16" />
            Đóng
          </button>
        </div>
      </div>
    </Transition>

    <!-- ── Info Popup Modal ──────────────────────────────── -->
    <Transition name="ei-fade">
      <div v-if="showInfoPopup" class="ei-modal-overlay" @click.self="showInfoPopup = false">
        <div class="ei-modal ei-modal--info">
          <div class="ei-modal__header">
            <div class="ei-modal__icon ei-modal__icon--info">
              <LucideIcon name="info" size="22" />
            </div>
            <div>
              <h2 class="ei-modal__title">{{ infoPopupTitle || 'Thông báo' }}</h2>
            </div>
          </div>
          <div class="ei-modal__body">
            <p class="ei-modal__msg">{{ infoPopupMessage }}</p>
          </div>
          <button type="button" class="ei-modal__confirm ei-modal__confirm--info" @click="showInfoPopup = false">
            <LucideIcon name="check" size="16" />
            Đóng
          </button>
        </div>
      </div>
    </Transition>

    <!-- ── Main Content ──────────────────────────────────── -->
    <main class="ei-main">
      <!-- Left: Question Area -->
      <div class="ei-question-col">

        <!-- Loading -->
        <div v-if="!examSurfaceReady" class="ei-empty-state">
          <LucideIcon name="progress_activity" size="36" class="ei-empty-icon" />
          <p class="ei-empty-title">Đang tải đề thi…</p>
          <p class="ei-empty-desc">Vui lòng đợi trong giây lát.</p>
        </div>

        <!-- Load failed -->
        <div v-else-if="examLoadFailed" class="ei-empty-state">
          <LucideIcon name="error" size="36" class="ei-empty-icon" />
          <p class="ei-empty-title">Không tải được đề thi</p>
          <p class="ei-empty-desc">Vui lòng làm mới trang hoặc quay lại sau.</p>
        </div>

        <!-- No questions -->
        <div v-else-if="!questions.length" class="ei-empty-state">
          <LucideIcon name="quiz" size="36" class="ei-empty-icon" />
          <p class="ei-empty-title">Không có câu hỏi</p>
          <p class="ei-empty-desc">Đề thi chưa có nội dung. Hãy thoát và liên hệ giáo viên.</p>
        </div>

        <!-- Question Card — slide transition -->
        <Transition name="ei-q-slide" mode="out-in">
          <div v-if="currentQuestion" :key="currentQuestion.id" class="ei-question-card">
            <!-- Question header -->
            <div class="ei-q-header">
              <!-- Circular progress ring -->
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
              <span class="ei-q-badge">
                / {{ questions.length }} câu
              </span>
              <button type="button" class="ei-mark-btn" :disabled="isSuspended" @click="toggleMarkCurrentQuestion">
                <LucideIcon :name="markedQuestions[String(currentQuestion.id)] ? 'bookmark_added' : 'bookmark_add'" size="15" />
                {{ markedQuestions[String(currentQuestion.id)] ? 'Bỏ đánh dấu' : 'Đánh dấu xem lại' }}
              </button>
            </div>

            <!-- Question text -->
            <div class="ei-q-content">
              <h2 class="ei-q-text">{{ currentQuestion.content }}</h2>
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
                <LucideIcon name="arrow_back" size="16" />
                Câu trước
              </button>

              <button type="button" class="ei-mark-btn ei-mark-btn--sm" :disabled="isSuspended" @click="toggleMarkCurrentQuestion">
                <LucideIcon name="bookmark" size="14" />
                {{ markedQuestions[String(currentQuestion.id)] ? 'Đã đánh dấu' : 'Đánh dấu' }}
              </button>

              <button
                type="button"
                class="ei-nav-btn ei-nav-btn--next"
                :disabled="currentIndex >= questions.length - 1"
                @click="goNext"
              >
                Câu tiếp
                <LucideIcon name="arrow_forward" size="16" />
              </button>
            </div>
          </div>
        </Transition>
      </div>

      <!-- Right: Sidebar -->
      <aside class="ei-sidebar">

        <!-- Camera preview -->
        <div v-if="shouldCheckDevices && mediaStreamRef" class="ei-cam-card">
          <div class="ei-cam-header">
            <span class="ei-cam-label">Camera của bạn</span>
            <div class="ei-cam-actions">
              <button
                type="button"
                :disabled="isSuspended"
                class="ei-cam-btn"
                :class="cameraReady ? 'ei-cam-btn--on' : 'ei-cam-btn--off'"
                @click="toggleCamera"
                :title="cameraReady ? 'Tắt camera' : 'Bật camera'"
              >
                <LucideIcon :name="cameraReady ? 'videocam' : 'videocam_off'" size="15" />
              </button>
              <button
                type="button"
                :disabled="isSuspended"
                class="ei-cam-btn"
                :class="micReady ? 'ei-cam-btn--on' : 'ei-cam-btn--off'"
                @click="toggleMic"
                :title="micReady ? 'Tắt micro' : 'Bật micro'"
              >
                <LucideIcon :name="micReady ? 'mic' : 'mic_off'" size="15" />
              </button>
            </div>
          </div>
          <div class="ei-cam-preview">
            <video ref="cameraPreviewRef" autoplay playsinline muted class="ei-cam-video" :class="{ 'ei-cam-video--hidden': !cameraReady }" />
            <div v-if="!cameraReady" class="ei-cam-off">
              <LucideIcon name="videocam_off" size="28" />
            </div>
          </div>
        </div>

        <!-- Progress card -->
        <div v-if="examSurfaceReady" class="ei-prog-card">
          <div class="ei-prog-header">
            <LucideIcon name="trending_up" size="15" />
            <span class="ei-prog-title">Tiến độ làm bài</span>
            <span class="ei-prog-pct">{{ progressPercent }}%</span>
          </div>
          <div class="ei-prog-bar">
            <div class="ei-prog-bar__fill" :style="{ width: `${progressPercent}%` }" />
          </div>
          <div class="ei-prog-stats">
            <span class="ei-prog-stat ei-prog-stat--done">
              <span class="ei-prog-dot ei-prog-dot--done" />
              Đã làm: {{ answeredCount }}
            </span>
            <span class="ei-prog-stat ei-prog-stat--skip">
              <span class="ei-prog-dot ei-prog-dot--skip" />
              Chưa: {{ unansweredCount }}
            </span>
          </div>
          <div class="ei-prog-tags">
            <span class="ei-prog-tag ei-prog-tag--mark">
              <LucideIcon name="bookmark" size="11" />
              Đánh dấu: {{ markedCount }}
            </span>
            <span class="ei-prog-tag ei-prog-tag--skip">
              Bỏ qua: {{ skippedCount }}
            </span>
          </div>
        </div>

        <!-- Question navigator -->
        <div v-if="examSurfaceReady" class="ei-nav-card">
          <div class="ei-nav-card__header">
            <LucideIcon name="list" size="15" />
            <span>Danh sách câu hỏi</span>
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
            <span class="ei-legend-item">
              <span class="ei-legend-dot ei-legend-dot--skip" />
              Bỏ qua
            </span>
          </div>
        </div>
      </aside>
    </main>

    <!-- ── Submit Modal ────────────────────────────────── -->
    <BaseModal v-model="showSubmitModal" title="Xác nhận nộp bài" :persistent="true">
      <div class="ei-submit-info">
        <p class="ei-submit-count">
          Đã trả lời <strong>{{ answeredCount }}</strong> / {{ questions.length }} câu.
          <span v-if="unansweredCount > 0" class="ei-submit-warn">
            Còn {{ unansweredCount }} câu chưa trả lời.
          </span>
        </p>
        <div class="ei-submit-stats">
          <div class="ei-submit-stat">
            <p class="ei-submit-stat__label">Đánh dấu xem lại</p>
            <p class="ei-submit-stat__val ei-submit-stat__val--warn">{{ markedCount }}</p>
          </div>
          <div class="ei-submit-stat">
            <p class="ei-submit-stat__label">Đã mở nhưng bỏ qua</p>
            <p class="ei-submit-stat__val">{{ skippedCount }}</p>
          </div>
        </div>
        <p class="ei-submit-note">Bạn sẽ không thể thay đổi đáp án sau khi nộp.</p>
        <div v-if="unansweredCount > 0" class="ei-submit-unanswered">
          <LucideIcon name="warning" size="15" />
          <span>Bạn còn {{ unansweredCount }} câu chưa làm và {{ notVisitedCount }} câu chưa mở.</span>
        </div>
      </div>
      <template #footer>
        <div class="ei-modal-actions">
          <BaseButton variant="ghost" type="button" @click="closeSubmitModal">Tiếp tục làm bài</BaseButton>
          <BaseButton id="submit-exam-confirm" :loading="isSubmitting" type="button" @click="submitExamAction">
            Nộp bài
          </BaseButton>
        </div>
      </template>
    </BaseModal>

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
import { useToast } from '../../composables/useToast'
import { useAutoSaveDraft } from '../../composables/useAutoSaveDraft'
import { useProctoringSession } from '../../composables/useProctoringSession'
import { useRealtimeChannel } from '../../composables/useRealtimeChannel'
import { onBeforeRouteLeave, useRoute, useRouter } from 'vue-router'
import { useExamSessionStore } from '../../stores/examSessionStore'
import { parseBackendDate } from '../../utils/dateUtils.js'
import BaseModal from '../shared/BaseModal.vue'
import BaseButton from '../shared/BaseButton.vue'
import QuestionRenderer from './questions/QuestionRenderer.vue'
import ConfirmDialog from '../ui/ConfirmDialog.vue'

const route = useRoute()
const router = useRouter()
const toast = useToast()

const showSubmitModal = ref(false)
const showLeaveConfirm = ref(false)
const pendingLeaveCallback = ref(null)
const isSubmitting = ref(false)
const questions = ref([])
const examSurfaceReady = ref(false)
const examLoadFailed = ref(false)

const examTitle = computed(() => route.query.exam || 'Bài thi')
const examId = computed(() => Number.parseInt(String(route.query.examId || ''), 10) || null)
const attemptId = computed(() => Number.parseInt(String(route.query.attemptId || ''), 10) || null)
const isPracticeExam = computed(() => String(route.query.isPractice || '') === 'true')

const examConfig = ref({
  monitorTabSwitch: true, monitorBlur: true, monitorExitFullscreen: true,
  monitorCopyPaste: true, monitorIdleTime: true, monitorDevtools: true,
  monitorDuplicateIp: true, monitorFastSubmit: true, monitorRightClick: true,
  monitorPrintScreen: true, monitorRapidQuestionSwitch: true,
  monitorMultiMonitor: true, requireCameraMic: true
})

// Popup states
const showErrorPopup = ref(false)
const errorPopupMessage = ref('')
const showSuccessPopup = ref(false)
const successPopupMessage = ref('')
const showInfoPopup = ref(false)
const infoPopupMessage = ref('')
const infoPopupTitle = ref('')
const answers = ref({})
const markedQuestions = ref({})
const visitedQuestions = ref({})
const currentIndex = ref(0)
const remainingSeconds = ref(Number.parseInt(String(route.query.remainingSeconds || ''), 10) || 0)
const initialRemainingForProgress = ref(0)
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
let idleTimer = null
let devtoolsCheckTimer = null
let blockBackHandler = null

const VIOLATION_COOLDOWN_MS = 7000
const LONG_VIOLATION_COOLDOWN_MS = 60000
const BLUR_GRACE_MS = 1200
const IDLE_THRESHOLD_MS = 3 * 60 * 1000
const DEVTOOLS_GAP_PX = 160
const RAPID_SWITCH_WINDOW_MS = 10000
const RAPID_SWITCH_THRESHOLD = 6

const examSessionStore = useExamSessionStore()
const realtimeChannel = useRealtimeChannel()
const showFullscreenPrompt = ref(false)
const isFullscreenActive = ref(false)

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

const currentQuestion = computed(() => questions.value[currentIndex.value] || null)
const shouldCheckDevices = computed(() => !isPracticeExam.value && examConfig.value.requireCameraMic !== false)
const devicesReady = computed(() => cameraReady.value && micReady.value)
const answeredCount = computed(() => Object.values(answers.value).filter(hasAnswerValue).length)
const unansweredCount = computed(() => Math.max(questions.value.length - answeredCount.value, 0))
const markedCount = computed(() => Object.values(markedQuestions.value).filter(Boolean).length)
const skippedCount = computed(() => questions.value.filter((q) => {
  const key = String(q.id)
  return Boolean(visitedQuestions.value[key]) && !hasAnswerValue(answers.value[key]) && !Boolean(markedQuestions.value[key])
}).length)
const notVisitedCount = computed(() => questions.value.filter((q) => !visitedQuestions.value[String(q.id)]).length)
const progressPercent = computed(() => {
  if (!questions.value.length) return 0
  return Math.round((answeredCount.value / questions.value.length) * 100)
})
const displayHours = computed(() => {
  const h = Math.floor(remainingSeconds.value / 3600)
  if (h === 0) return ''
  return `${String(h).padStart(2, '0')}h `
})

const displayMinutes = computed(() => {
  const totalMin = Math.floor(remainingSeconds.value / 60)
  const h = Math.floor(totalMin / 60)
  const m = totalMin % 60
  return h > 0 ? String(m).padStart(2, '0') : String(totalMin).padStart(2, '0')
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

// Circular ring
const timerCircumference = 2 * Math.PI * 30 // r=30
const timerRingOffset = computed(() => {
  if (!initialRemainingForProgress.value) return timerCircumference
  const ratio = Math.max(0, Math.min(1, remainingSeconds.value / initialRemainingForProgress.value))
  return timerCircumference * (1 - ratio)
})

// Question progress ring
const qRingCircumference = 2 * Math.PI * 15 // r=15
const qRingOffset = computed(() => {
  if (!questions.value.length) return qRingCircumference
  const ratio = (currentIndex.value) / questions.value.length
  return qRingCircumference * (1 - ratio)
})

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

const clearBlurGraceTimer = () => {
  if (blurGraceTimer) { window.clearTimeout(blurGraceTimer); blurGraceTimer = null }
}

const syncDeviceStatusToBackend = async () => {
  if (!attemptId.value || isPracticeExam.value || isSuspended.value) return
  try { await updateDeviceStatus(attemptId.value, cameraReady.value, micReady.value) } catch {}
}

const toggleCamera = () => {
  const videoTrack = mediaStreamRef.value?.getVideoTracks()[0]
  if (!videoTrack) return
  videoTrack.enabled = !videoTrack.enabled
  cameraReady.value = videoTrack.enabled
  void syncDeviceStatusToBackend()
}

const toggleMic = () => {
  const audioTrack = mediaStreamRef.value?.getAudioTracks()[0]
  if (!audioTrack) return
  audioTrack.enabled = !audioTrack.enabled
  micReady.value = audioTrack.enabled
  void syncDeviceStatusToBackend()
}

const checkDevices = async () => {
  if (!shouldCheckDevices.value) { cameraReady.value = true; micReady.value = true; deviceError.value = ''; return }
  if (!navigator?.mediaDevices?.getUserMedia) { cameraReady.value = false; micReady.value = false; deviceError.value = 'Trình duyệt không hỗ trợ.'; return }
  isCheckingDevices.value = true; deviceError.value = ''
  try {
    const stream = await navigator.mediaDevices.getUserMedia({ video: true, audio: true })
    const vt = stream.getVideoTracks()[0]; const at = stream.getAudioTracks()[0]
    cameraReady.value = Boolean(vt?.enabled); micReady.value = Boolean(at?.enabled)
    mediaStreamRef.value = stream
    deviceStatusInterval = setInterval(syncDeviceStatusToBackend, 15000)
    await syncDeviceStatusToBackend()
  } catch (error) {
    cameraReady.value = false; micReady.value = false
    deviceError.value = error?.name === 'NotAllowedError' ? 'Cần cấp quyền camera và micro.' : 'Không truy cập được thiết bị.'
  } finally { isCheckingDevices.value = false }
}

const stopMediaStream = () => {
  if (deviceStatusInterval) { clearInterval(deviceStatusInterval); deviceStatusInterval = null }
  if (mediaStreamRef.value) { mediaStreamRef.value.getTracks().forEach(t => t.stop()); mediaStreamRef.value = null }
  if (cameraPreviewRef.value) cameraPreviewRef.value.srcObject = null
}

watch(mediaStreamRef, (stream) => {
  if (!stream) return
  nextTick(() => { if (cameraPreviewRef.value) cameraPreviewRef.value.srcObject = stream })
})

const syncRiskState = (payload) => {
  if (!payload || typeof payload !== 'object') return
  const nextRiskScore = typeof payload.riskScore === 'number' ? payload.riskScore : payload.score
  const nextRiskLevel = payload.riskLevel || payload.level
  if (typeof nextRiskScore === 'number') {
    examSessionStore.setRiskState({ riskScore: nextRiskScore, riskLevel: nextRiskLevel || examSessionStore.riskState.riskLevel, status: payload.status || attemptStatus.value })
  }
}

const handleProctorActions = (actions = [], payload = {}) => {
  const normalized = actions.map((item) => String(item || '').toUpperCase())
  if (normalized.includes('PAUSE_ATTEMPT') || normalized.includes('ATTEMPT_PAUSED')) {
    applyAttemptStatus(payload.status || 'PAUSED', payload.message || 'Phiên thi đang được giám thị kiểm tra.')
  }
  if (normalized.includes('SHOW_WARNING') || normalized.includes('WARNING_SENT')) {
    teacherWarningMessage.value = payload.message || 'Hệ thống phát hiện rủi ro cao. Vui lòng tiếp tục làm bài đúng quy định.'
    showTeacherWarningModal.value = true
    toast.warning(teacherWarningMessage.value)
  }
}

const buildDeviceFingerprintSeed = () => JSON.stringify({
  screen: `${window.screen?.width || 0}x${window.screen?.height || 0}`,
  timezone: Intl.DateTimeFormat().resolvedOptions().timeZone || '',
  language: navigator.language || '', platform: navigator.platform || '', userAgent: navigator.userAgent || ''
})

const getHeartbeatPayload = () => ({
  fullscreen: Boolean(document.fullscreenElement), visibility: document.visibilityState || 'visible',
  cameraOn: cameraReady.value, micOn: micReady.value,
  screenMetrics: { screenWidth: window.screen?.width || null, screenHeight: window.screen?.height || null,
    availWidth: window.screen?.availWidth || null, availHeight: window.screen?.availHeight || null, viewportWidth: window.innerWidth || null, viewportHeight: window.innerHeight || null }
})

const { queueEvent, flush: flushQueuedViolations, syncHeartbeat, startHeartbeat, stopHeartbeat } = useProctoringSession({
  getAttemptId: () => attemptId.value, getDeviceFingerprint: buildDeviceFingerprintSeed, getHeartbeatPayload,
  onRiskUpdate: syncRiskState, onActionRequired: handleProctorActions, batchWindowMs: 1200, heartbeatIntervalMs: 15000
})

const reportViolation = async (eventType, details, cooldownMs = VIOLATION_COOLDOWN_MS) => {
  if (isPracticeExam.value || !attemptId.value || isSuspended.value) return
  const cfg = examConfig.value
  const monitorMap = { TAB_SWITCH: 'monitorTabSwitch', BLUR: 'monitorBlur', EXIT_FULLSCREEN: 'monitorExitFullscreen', COPY_PASTE: 'monitorCopyPaste', IDLE_TIME: 'monitorIdleTime', DEVTOOLS_OPEN: 'monitorDevtools', RIGHT_CLICK: 'monitorRightClick', PRINT_SCREEN: 'monitorPrintScreen', RAPID_QUESTION_SWITCH: 'monitorRapidQuestionSwitch', MULTI_MONITOR: 'monitorMultiMonitor', FAST_SUBMIT: 'monitorFastSubmit' }
  const prop = monitorMap[eventType]
  if (prop && cfg[prop] === false) return
  const now = Date.now()
  const lastAt = lastViolationAtByType.value[eventType] || 0
  if (now - lastAt < cooldownMs) return
  lastViolationAtByType.value = { ...lastViolationAtByType.value, [eventType]: now }
  try { queueEvent(eventType, details, { questionIndex: currentIndex.value + 1 }) } catch {}
}

const applyAttemptStatus = (status, message = '') => {
  const normalized = String(status || '').toUpperCase() || 'IN_PROGRESS'
  attemptStatus.value = normalized
  examSessionStore.setRiskState({ status: normalized })
  if (normalized === 'STOPPED' || normalized === 'PAUSED') {
    isSuspended.value = true
    if (message) suspensionMessage.value = message
    else if (normalized === 'PAUSED') suspensionMessage.value = 'Phiên thi đang được tạm dừng để giám thị kiểm tra.'
    showSubmitModal.value = false
    stopMediaStream()
    return
  }
  isSuspended.value = false
}

const enforceDeviceAccess = async () => {
  if (isPracticeExam.value) return
  if (mediaStreamRef.value) {
    const vt = mediaStreamRef.value.getVideoTracks()[0]; const at = mediaStreamRef.value.getAudioTracks()[0]
    const ve = !vt || vt.readyState === 'ended'; const ae = !at || at.readyState === 'ended'
    if (ve || ae) { isSuspended.value = true; suspensionMessage.value = 'Camera hoặc micro đã bị thu hồi. Vui lòng tải lại trang.'; return }
    cameraReady.value = Boolean(vt?.enabled); micReady.value = Boolean(at?.enabled)
    await syncDeviceStatusToBackend()
    return
  }
  await checkDevices()
  if (!devicesReady.value) { isSuspended.value = true; suspensionMessage.value = deviceError.value || 'Cần cấp quyền camera và micro.' }
}

const requestExamFullscreen = async () => {
  if (isPracticeExam.value) return
  if (!document.documentElement?.requestFullscreen) return
  try { await document.documentElement.requestFullscreen(); isFullscreenActive.value = true; showFullscreenPrompt.value = false; examSessionStore.setFullscreenState({ required: true, active: true }) }
  catch { showFullscreenPrompt.value = true }
}

const syncAttemptStatus = async () => {
  if (!attemptId.value) return
  try {
    const detail = await getAttemptDetail(attemptId.value)
    applyAttemptStatus(detail?.status || 'IN_PROGRESS')
    if (typeof detail?.riskScore === 'number') examSessionStore.setRiskState({ riskScore: detail.riskScore, riskLevel: detail.riskLevel || examSessionStore.riskState.riskLevel, status: detail.status || attemptStatus.value })
    if (typeof detail?.remainingSeconds === 'number' && detail.remainingSeconds >= 0) {
      const diff = Math.abs(remainingSeconds.value - detail.remainingSeconds)
      if (diff > 5) remainingSeconds.value = Math.max(0, detail.remainingSeconds)
    }
  } catch {}
}

const connectProctorRealtime = async () => {
  if (isPracticeExam.value || !attemptId.value) return
  await realtimeChannel.connect({
    reconnectDelay: 5000,
    topics: [{
      destination: `/topic/attempts/${attemptId.value}/proctor-actions`,
      handler: (payload) => {
        const type = String(payload?.type || '').toUpperCase()
        if (type === 'TEACHER_WARNING') {
          teacherWarningMessage.value = payload.message || 'Bạn đang bị cảnh báo bởi giám thị.'
          showTeacherWarningModal.value = true
          toast.warning(teacherWarningMessage.value)
        }
        if (type === 'ATTEMPT_STOPPED' || type === 'ATTEMPT_PAUSED') {
          applyAttemptStatus(payload.status || (type === 'ATTEMPT_PAUSED' ? 'PAUSED' : 'STOPPED'), payload.message || 'Bài thi đã bị đình chỉ.')
        }
        if (type === 'RISK_UPDATE') {
          syncRiskState({ riskScore: payload.riskScore, riskLevel: payload.riskLevel, status: payload.status })
          handleProctorActions([payload.actionTaken], payload)
          // handleProctorActions đã hiện toast WARNING rồi → không toast lại
        }
      }
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
  if (document.hidden) { pendingViolationByType.value = { ...pendingViolationByType.value, TAB_SWITCH: true }; void reportViolation('TAB_SWITCH', 'TAB_SWITCH') }
  else if (pendingViolationByType.value.TAB_SWITCH) pendingViolationByType.value = { ...pendingViolationByType.value, TAB_SWITCH: false }
}

const handleWindowBlur = () => {
  clearBlurGraceTimer()
  blurGraceTimer = window.setTimeout(() => { pendingViolationByType.value = { ...pendingViolationByType.value, BLUR: true }; void reportViolation('BLUR', 'Cửa sổ mất tiêu điểm') }, BLUR_GRACE_MS)
}
const handleWindowFocus = () => { clearBlurGraceTimer(); if (pendingViolationByType.value.BLUR) pendingViolationByType.value = { ...pendingViolationByType.value, BLUR: false } }

const handleFullscreenChange = () => {
  const inFs = Boolean(document.fullscreenElement); isFullscreenActive.value = inFs; examSessionStore.setFullscreenState({ required: true, active: inFs })
  if (inFs) { pendingViolationByType.value = { ...pendingViolationByType.value, EXIT_FULLSCREEN: false }; showFullscreenPrompt.value = false; return }
  pendingViolationByType.value = { ...pendingViolationByType.value, EXIT_FULLSCREEN: true }; showFullscreenPrompt.value = !isPracticeExam.value; void reportViolation('EXIT_FULLSCREEN', 'Thoát toàn màn hình')
}

const onIdleActivity = () => {
  if (idleTimer) window.clearTimeout(idleTimer)
  idleTimer = window.setTimeout(() => { void reportViolation('IDLE_TIME', `Idle ${Math.round(IDLE_THRESHOLD_MS / 60000)} phút`, LONG_VIOLATION_COOLDOWN_MS) }, IDLE_THRESHOLD_MS)
}

const handleCopyPaste = (event) => {
  const target = event?.target
  if (target && (target.tagName === 'INPUT' || target.tagName === 'TEXTAREA')) return
  const txt = event?.clipboardData?.getData?.('text') || ''
  const summary = txt.length > 50 ? `${event.type} ${txt.length} ký tự` : `Phát hiện ${event.type}`
  void reportViolation('COPY_PASTE', summary, LONG_VIOLATION_COOLDOWN_MS)
}

const detectDevToolsOpen = () => { if (document.hidden) return false; const wg = Math.abs(window.outerWidth - window.innerWidth); const hg = Math.abs(window.outerHeight - window.innerHeight); return wg > DEVTOOLS_GAP_PX || hg > DEVTOOLS_GAP_PX }
const scheduleDevToolsCheck = () => { if (devtoolsCheckTimer) window.clearInterval(devtoolsCheckTimer); devtoolsCheckTimer = window.setInterval(() => { if (detectDevToolsOpen()) void reportViolation('DEVTOOLS_OPEN', 'DevTools được phát hiện', LONG_VIOLATION_COOLDOWN_MS) }, 5000) }

const checkRapidQuestionSwitch = () => {
  const now = Date.now()
  questionSwitchTimestamps.value = [...questionSwitchTimestamps.value, now].filter(t => now - t <= RAPID_SWITCH_WINDOW_MS)
  if (questionSwitchTimestamps.value.length >= RAPID_SWITCH_THRESHOLD) { void reportViolation('RAPID_QUESTION_SWITCH', `${questionSwitchTimestamps.value.length} lần đổi câu trong ${RAPID_SWITCH_WINDOW_MS / 1000}s`); questionSwitchTimestamps.value = [] }
}

const selectQuestion = (index) => {
  if (isSuspended.value) return
  if (index !== currentIndex.value && examConfig.value.monitorRapidQuestionSwitch !== false) checkRapidQuestionSwitch()
  currentIndex.value = index
  const q = questions.value[index]
  if (q?.id != null) { visitedQuestions.value = { ...visitedQuestions.value, [String(q.id)]: true }; examSessionStore.setCurrentQuestion(q.id) }
}

const goPrevious = () => {
  if (isSuspended.value) return
  if (currentIndex.value > 0) {
    if (examConfig.value.monitorRapidQuestionSwitch !== false) checkRapidQuestionSwitch()
    currentIndex.value -= 1
    const q = questions.value[currentIndex.value]
    if (q?.id != null) { visitedQuestions.value = { ...visitedQuestions.value, [String(q.id)]: true }; examSessionStore.setCurrentQuestion(q.id) }
  }
}

const goNext = () => {
  if (isSuspended.value) return
  if (currentIndex.value < questions.value.length - 1) {
    if (examConfig.value.monitorRapidQuestionSwitch !== false) checkRapidQuestionSwitch()
    currentIndex.value += 1
    const q = questions.value[currentIndex.value]
    if (q?.id != null) { visitedQuestions.value = { ...visitedQuestions.value, [String(q.id)]: true }; examSessionStore.setCurrentQuestion(q.id) }
  }
}

const handleRightClick = (e) => { if (examConfig.value.monitorRightClick !== false) { e.preventDefault(); e.stopPropagation(); void reportViolation('RIGHT_CLICK', 'Chuột phải bị chặn', VIOLATION_COOLDOWN_MS) } }

const handlePrintScreen = (e) => { if (examConfig.value.monitorPrintScreen !== false && (e.key === 'PrintScreen' || e.keyCode === 44)) { e.preventDefault(); void reportViolation('PRINT_SCREEN', 'Phát hiện phím Print Screen', LONG_VIOLATION_COOLDOWN_MS) } }

const checkMultiMonitor = () => {
  if (examConfig.value.monitorMultiMonitor === false) return
  const sw = window.screen?.width || 0; const sh = window.screen?.height || 0; const aw = window.screen?.availWidth || 0; const ah = window.screen?.availHeight || 0
  if (sw > 0 && aw > 0 && (sw - aw > 100 || sh - ah > 100)) void reportViolation('MULTI_MONITOR', 'Nhiều màn hình', LONG_VIOLATION_COOLDOWN_MS)
}

const persistDraftToServer = async () => {
  if (!attemptId.value || isSuspended.value) return
  const payload = Object.entries(answers.value).filter(([, v]) => hasAnswerValue(v)).map(([questionId, selectedAnswer]) => {
    const q = questions.value.find(item => Number(item.id) === Number(questionId))
    return { questionId: Number(questionId), selectedAnswer: serializeAnswerValue(q, selectedAnswer) }
  })
  if (!payload.length) return
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
  const q = questions.value.find(item => String(item.id) === String(questionId))
  const type = q ? normalizeQuestionType(q) : 'SINGLE_CHOICE'
  const stored =
    type === 'SINGLE_CHOICE'
      ? (selectedAnswer != null && selectedAnswer !== '' ? String(selectedAnswer) : '')
      : selectedAnswer
  answers.value = { ...answers.value, [questionId]: stored }
  visitedQuestions.value = { ...visitedQuestions.value, [String(questionId)]: true }
  examSessionStore.setAnswer(questionId, stored)
  schedule()
}

const toggleMarkCurrentQuestion = () => {
  const q = currentQuestion.value
  if (!q) return
  const key = String(q.id)
  markedQuestions.value = { ...markedQuestions.value, [key]: !markedQuestions.value[key] }
  examSessionStore.toggleMarked(q.id)
}

const manualSaveDraft = async () => {
  try { await forceSave(); showSuccessPopup.value = true; successPopupMessage.value = 'Đã lưu bài làm thành công!' } catch { showErrorPopup.value = true; errorPopupMessage.value = 'Không lưu được. Vui lòng thử lại.' }
}

const buildSubmitPayload = () => Object.entries(answers.value).filter(([, v]) => hasAnswerValue(v)).map(([questionId, selectedAnswer]) => {
  const q = questions.value.find(item => Number(item.id) === Number(questionId))
  return { questionId: Number(questionId), selectedAnswer: serializeAnswerValue(q, selectedAnswer) }
})

const openSubmitModal = () => { if (isSuspended.value) return; showSubmitModal.value = true }
const closeSubmitModal = () => { showSubmitModal.value = false }

const autoSubmitOnTimeUp = async () => {
  if (!attemptId.value || isSuspended.value || isSubmitting.value) return
  isSubmitting.value = true
  try { await forceSave() } catch {}
  toast.info('Hết giờ. Đang nộp...')
  try {
    const result = await submitAttempt(attemptId.value, buildSubmitPayload())
    showSubmitModal.value = false
    router.push({ path: '/student/submission-confirmation', query: { exam: examTitle.value, attemptId: attemptId.value, score: Math.round(Number(result?.score || 0)), submittedAt: result?.submittedAt || '' } })
  } catch { showErrorPopup.value = true; errorPopupMessage.value = 'Không nộp tự động được. Vui lòng thử nộp lại.' } finally { isSubmitting.value = false }
}

const submitExamAction = async () => {
  if (!attemptId.value || isSuspended.value) return
  isSubmitting.value = true
  try { await forceSave() } catch {}
  try {
    const result = await submitAttempt(attemptId.value, buildSubmitPayload())
    showSubmitModal.value = false
    router.push({ path: '/student/submission-confirmation', query: { exam: examTitle.value, attemptId: attemptId.value, score: Math.round(Number(result?.score || 0)), submittedAt: result?.submittedAt || '' } })
  } catch { showErrorPopup.value = true; errorPopupMessage.value = 'Không nộp được. Vui lòng thử lại.' } finally { isSubmitting.value = false }
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

const handleBeforeUnload = (e) => { if (isSubmitting.value || !attemptId.value || Object.keys(answers.value).length === 0) return; e.preventDefault(); e.returnValue = '' }

const handleExamKeydown = (e) => {
  if (isSuspended.value) return
  if (e.key === 'F12' || (e.ctrlKey && ['c', 'v', 'u'].includes(String(e.key || '').toLowerCase()))) { e.preventDefault(); void reportViolation('DEVTOOLS_OPEN', `Phát hiện phím tắt: ${e.key}`, LONG_VIOLATION_COOLDOWN_MS); return }
  if (!e.ctrlKey) return
  if (e.key === 'ArrowLeft') { e.preventDefault(); goPrevious() }
  else if (e.key === 'ArrowRight') { e.preventDefault(); goNext() }
}

onBeforeRouteLeave(() => {
  if (isSubmitting.value) return true
  if (isPracticeExam.value) return true
  pendingLeaveCallback.value = null
  showLeaveConfirm.value = true
  return false
})

const onConfirmLeave = () => {
  const cb = pendingLeaveCallback.value
  pendingLeaveCallback.value = null
  showLeaveConfirm.value = false
  if (cb) cb()
}

const onCancelLeave = () => {
  pendingLeaveCallback.value = null
  showLeaveConfirm.value = false
}

onMounted(async () => {
  try {
    if (!examId.value || !attemptId.value) { showErrorPopup.value = true; errorPopupMessage.value = 'Thiếu thông tin bài thi. Vui lòng quay lại và thử lại.'; examLoadFailed.value = true; examSurfaceReady.value = true; return }

    const [{ getExamDetail }] = await Promise.all([import('../../services/examService')])
    questionList = await listExamQuestions(examId.value)
    const emptyDraft = { answers: [], status: 'IN_PROGRESS' }
    const [draftOutcome, examOutcome] = await Promise.allSettled([
      getDraftAnswers(attemptId.value),
      getExamDetail(examId.value)
    ])
    const draftData =
      draftOutcome.status === 'fulfilled' && draftOutcome.value != null
        ? draftOutcome.value
        : emptyDraft
    const examDetail = examOutcome.status === 'fulfilled' ? examOutcome.value : null

    examConfig.value = {
      monitorTabSwitch: examDetail?.monitorTabSwitch !== false, monitorBlur: examDetail?.monitorBlur !== false,
      monitorExitFullscreen: examDetail?.monitorExitFullscreen !== false, monitorCopyPaste: examDetail?.monitorCopyPaste !== false,
      monitorIdleTime: examDetail?.monitorIdleTime !== false, monitorDevtools: examDetail?.monitorDevtools !== false,
      monitorDuplicateIp: examDetail?.monitorDuplicateIp !== false, monitorFastSubmit: examDetail?.monitorFastSubmit !== false,
      monitorRightClick: examDetail?.monitorRightClick !== false, monitorPrintScreen: examDetail?.monitorPrintScreen !== false,
      monitorRapidQuestionSwitch: examDetail?.monitorRapidQuestionSwitch !== false,
      monitorMultiMonitor: examDetail?.monitorMultiMonitor !== false, requireCameraMic: examDetail?.requireCameraMic !== false
    }

    questions.value = questionList.map(item => ({ id: item.id, content: item.content, type: item.type || 'SINGLE_CHOICE', options: parseQuestionOptions(item.options), metadata: parseQuestionJson(item.metadata, null), attachments: parseQuestionJson(item.attachments, []) }))
    examSurfaceReady.value = true

    const serverAnswers = (draftData?.answers || []).reduce((acc, answer) => {
      const q = questions.value.find(item => Number(item.id) === Number(answer.questionId))
      acc[answer.questionId] = deserializeAnswerValue(q, answer.selectedAnswer)
      return acc
    }, {})
    answers.value = mergeLocalIntoAnswers(serverAnswers)
    const answeredQuestionKeys = Object.keys(answers.value).reduce((acc, key) => { acc[String(key)] = true; return acc }, {})
    examSessionStore.hydrateSession({ attempt: { id: attemptId.value, status: draftData?.status || 'IN_PROGRESS' }, exam: { id: examId.value, title: examTitle.value }, questions: questions.value, answers: answers.value })
    if (questions.value[0]?.id != null) { visitedQuestions.value = { [String(questions.value[0].id)]: true, ...answeredQuestionKeys }; examSessionStore.setCurrentQuestion(questions.value[0].id) }

    applyAttemptStatus(draftData?.status || 'IN_PROGRESS')

    if (typeof draftData?.remainingSeconds === 'number' && draftData.remainingSeconds >= 0) remainingSeconds.value = Math.max(0, draftData.remainingSeconds)
    else if (draftData?.deadlineAt || route.query.deadlineAt) {
      const dlDate = parseBackendDate(String(draftData?.deadlineAt || route.query.deadlineAt))
      if (dlDate) remainingSeconds.value = Math.max(0, Math.floor((dlDate.getTime() - Date.now()) / 1000))
    }
    initialRemainingForProgress.value = Math.max(remainingSeconds.value, 1)

    if (remainingSeconds.value <= 0 && !isPracticeExam.value) { void autoSubmitOnTimeUp(); return }

    timerId = window.setInterval(() => {
      if (remainingSeconds.value > 0) {
        remainingSeconds.value -= 1
        if (remainingSeconds.value === 0) { clearInterval(timerId); timerId = null; void autoSubmitOnTimeUp() }
      }
    }, 1000)

    await enforceDeviceAccess()
    if (!devicesReady.value && shouldCheckDevices.value) { showErrorPopup.value = true; errorPopupMessage.value = suspensionMessage.value || 'Thiết bị của bạn chưa sẵn sàng. Vui lòng kiểm tra camera và micro.'; return }
    isSuspended.value = false
    isFullscreenActive.value = Boolean(document.fullscreenElement)
    if (!isPracticeExam.value && examConfig.value.monitorExitFullscreen !== false && !isFullscreenActive.value) { showFullscreenPrompt.value = true; await requestExamFullscreen() }

    setupBlockBackButton()
    window.addEventListener('beforeunload', handleBeforeUnload)
    window.addEventListener('keydown', handleExamKeydown)

    if (!isPracticeExam.value) {
      try {
        await connectProctorRealtime()
      } catch {
        /* WebSocket/STOMP lỗi không nên chặn làm bài khi đề đã tải */
      }
      startHeartbeat(); void syncHeartbeat()
      attemptStatusTimer = window.setInterval(() => { syncAttemptStatus(); enforceDeviceAccess() }, 5000)
      if (examConfig.value.monitorIdleTime !== false) { onIdleActivity(); window.addEventListener('mousemove', onIdleActivity); window.addEventListener('keydown', onIdleActivity); window.addEventListener('scroll', onIdleActivity) }
      if (examConfig.value.monitorDevtools !== false) scheduleDevToolsCheck()
      if (examConfig.value.monitorTabSwitch !== false) document.addEventListener('visibilitychange', handleVisibilityChange)
      if (examConfig.value.monitorBlur !== false) { window.addEventListener('blur', handleWindowBlur); window.addEventListener('focus', handleWindowFocus) }
      if (examConfig.value.monitorExitFullscreen !== false) document.addEventListener('fullscreenchange', handleFullscreenChange)
      if (examConfig.value.monitorCopyPaste !== false) { document.addEventListener('copy', handleCopyPaste); document.addEventListener('paste', handleCopyPaste) }
      if (examConfig.value.monitorPrintScreen !== false) document.addEventListener('keydown', handlePrintScreen)
      if (examConfig.value.monitorMultiMonitor !== false) setTimeout(checkMultiMonitor, 3000)
    }
  } catch (error) {
    examLoadFailed.value = true
    examSurfaceReady.value = true

    let userMessage = 'Không tải được nội dung bài thi. Vui lòng làm mới trang.'
    if (error?.message?.includes('403') || error?.status === 403) {
      userMessage = 'Bạn không có quyền làm bài thi này.'
    } else if (error?.message?.includes('404') || error?.status === 404) {
      userMessage = 'Bài thi không tồn tại hoặc đã bị xóa.'
    } else if (error?.message?.includes('401') || error?.status === 401) {
      userMessage = 'Phiên đăng nhập hết hạn. Vui lòng đăng nhập lại.'
    } else if (questionList?.length === 0) {
      userMessage = 'Bài thi chưa có câu hỏi. Vui lòng liên hệ giáo viên.'
    }
    errorPopupMessage.value = userMessage
    showErrorPopup.value = true
  }
})

onUnmounted(() => {
  window.removeEventListener('beforeunload', handleBeforeUnload)
  window.removeEventListener('keydown', handleExamKeydown)
  teardownBlockBackButton()
  stopMediaStream()
  stopHeartbeat()
  void flushQueuedViolations().catch(() => {})
  if (timerId) clearInterval(timerId)
  if (attemptStatusTimer) clearInterval(attemptStatusTimer)
  if (idleTimer) clearTimeout(idleTimer)
  if (devtoolsCheckTimer) clearInterval(devtoolsCheckTimer)
  clearBlurGraceTimer()
  realtimeChannel.disconnect()
  if (!isPracticeExam.value) {
    if (examConfig.value.monitorTabSwitch !== false) document.removeEventListener('visibilitychange', handleVisibilityChange)
    if (examConfig.value.monitorBlur !== false) { window.removeEventListener('blur', handleWindowBlur); window.removeEventListener('focus', handleWindowFocus) }
    if (examConfig.value.monitorExitFullscreen !== false) document.removeEventListener('fullscreenchange', handleFullscreenChange)
    if (examConfig.value.monitorCopyPaste !== false) { document.removeEventListener('copy', handleCopyPaste); document.removeEventListener('paste', handleCopyPaste) }
    if (examConfig.value.monitorPrintScreen !== false) document.removeEventListener('keydown', handlePrintScreen)
    if (examConfig.value.monitorIdleTime !== false) { window.removeEventListener('mousemove', onIdleActivity); window.removeEventListener('keydown', onIdleActivity); window.removeEventListener('scroll', onIdleActivity) }
  }
})
</script>

<style scoped>
/* ── Root ─────────────────────────────────────────────── */
.ei-root {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  background: var(--ds-bg);
}

/* ── Top Bar ──────────────────────────────────────────── */
.ei-topbar {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 0.75rem 1.25rem;
  background: var(--ds-surface);
  border-bottom: 2px solid var(--ds-border);
  flex-shrink: 0;
  flex-wrap: wrap;
  box-shadow: 0 4px 16px rgba(0,0,0,0.08);
  position: sticky;
  top: 0;
  z-index: 50;
}

.dark .ei-topbar { border-bottom-color: var(--ds-border-strong); }

.ei-topbar__left {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex: 1;
  min-width: 0;
}

/* Exam chip */
.ei-topbar__exam-chip {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.375rem 0.875rem;
  background: linear-gradient(135deg, var(--ds-primary) 0%, #6366f1 100%);
  border-radius: var(--ds-radius-full);
  color: white;
  font-size: 0.75rem;
  font-weight: 700;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 200px;
  box-shadow: 0 2px 8px rgba(79,70,229,0.3);
  flex-shrink: 0;
}

.ei-topbar__exam-title {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* Device badges */
.ei-topbar__device-badges {
  display: flex;
  align-items: center;
  gap: 0.375rem;
}

.ei-device-badge {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  padding: 0.25rem 0.625rem;
  border-radius: var(--ds-radius-full);
  font-size: 0.65rem;
  font-weight: 800;
  border: 1.5px solid;
  letter-spacing: 0.02em;
  white-space: nowrap;
}

.ei-device-badge--ok {
  background: var(--ds-success-soft);
  border-color: rgba(22, 163, 74, 0.3);
  color: var(--ds-success);
}

.ei-device-badge--fail {
  background: var(--ds-danger-soft);
  border-color: rgba(220, 38, 38, 0.3);
  color: var(--ds-danger);
  animation: eiDeviceBlink 2s ease-in-out infinite;
}

@keyframes eiDeviceBlink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.6; }
}

/* Timer — rectangular, prominent */
.ei-topbar__timer {
  display: flex;
  align-items: center;
  gap: 0.625rem;
  padding: 0.5rem 1rem;
  background: var(--ds-surface);
  border: 2px solid var(--ds-border);
  border-radius: var(--ds-radius-xl);
  transition: color 0.3s ease, background-color 0.3s ease, border-color 0.3s ease, box-shadow 0.3s ease, transform 0.3s ease;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
  flex-shrink: 0;
}

.dark .ei-topbar__timer { background: var(--ds-gray-800); border-color: var(--ds-border-strong); }

.ei-timer--warn {
  border-color: rgba(245,158,11,0.5);
  background: rgba(245,158,11,0.06);
}

.ei-timer--danger {
  border-color: rgba(220,38,38,0.5);
  background: rgba(220,38,38,0.06);
  animation: eiTimerPulse 1.5s ease-in-out infinite;
}

@keyframes eiTimerPulse {
  0%, 100% { box-shadow: 0 2px 8px rgba(220,38,38,0.2); }
  50% { box-shadow: 0 2px 20px rgba(220,38,38,0.45); }
}

.ei-topbar__timer-icon {
  color: var(--ds-primary);
  flex-shrink: 0;
}

.ei-timer--warn .ei-topbar__timer-icon { color: var(--ds-warning); }
.ei-timer--danger .ei-topbar__timer-icon { color: var(--ds-danger); }

.ei-topbar__timer-digits {
  display: flex;
  align-items: center;
  gap: 0.25rem;
}

.ei-timer-unit {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.05rem;
  min-width: 30px;
}

.ei-timer-val {
  font-family: var(--ds-font-display);
  font-size: 1.125rem;
  font-weight: 900;
  color: var(--ds-text);
  line-height: 1;
  font-variant-numeric: tabular-nums;
  letter-spacing: -0.02em;
}

.dark .ei-timer-val { color: #f1f5f9; }

.ei-timer--warn .ei-timer-val { color: var(--ds-warning); }
.ei-timer--danger .ei-timer-val { color: var(--ds-danger); }

.ei-timer-lbl {
  font-size: 0.48rem;
  font-weight: 700;
  color: var(--ds-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.06em;
  line-height: 1;
}

.ei-timer-colon {
  font-family: var(--ds-font-display);
  font-size: 1.125rem;
  font-weight: 300;
  color: var(--ds-gray-300);
  line-height: 1;
  align-self: flex-start;
  margin-top: 1px;
  min-width: 6px;
  text-align: center;
}

.dark .ei-timer-colon { color: var(--ds-gray-600); }
.ei-timer--warn .ei-timer-colon { color: rgba(234,179,8,0.35); }
.ei-timer--danger .ei-timer-colon { color: rgba(220,38,38,0.35); }

.ei-topbar__timer-progress {
  width: 100%;
  height: 4px;
  background: var(--ds-gray-100);
  border-radius: var(--ds-radius-full);
  overflow: hidden;
  margin-top: 0.25rem;
  flex-shrink: 0;
}

.dark .ei-topbar__timer-progress { background: var(--ds-gray-700); }

.ei-topbar__timer-progress-fill {
  height: 100%;
  background: linear-gradient(90deg, var(--ds-primary), #6366f1);
  border-radius: var(--ds-radius-full);
  transition: width 1s linear, background 0.3s ease;
}

.ei-timer--warn .ei-topbar__timer-progress-fill { background: var(--ds-warning); }
.ei-timer--danger .ei-topbar__timer-progress-fill { background: linear-gradient(90deg, var(--ds-danger), #b91c1c); }

/* Right actions */
.ei-topbar__right {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex-shrink: 0;
}

.ei-save-status {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  padding: 0.25rem 0.625rem;
  border-radius: var(--ds-radius-full);
  font-size: 0.65rem;
  font-weight: 700;
  background: var(--ds-gray-50);
  border: 1px solid var(--ds-border);
  color: var(--ds-text-muted);
}

.dark .ei-save-status { background: var(--ds-gray-800); border-color: var(--ds-border-strong); }

.ei-save-icon--spin { animation: eiSpin 1s linear infinite; color: var(--ds-info); }
.ei-save-icon--ok { color: var(--ds-success); }
.ei-save-icon--pending { color: var(--ds-warning); }
.ei-save-icon--err { color: var(--ds-danger); }
.ei-save-label { white-space: nowrap; }

@keyframes eiSpin { to { transform: rotate(360deg); } }

/* Buttons */
.ei-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.375rem;
  padding: 0.5rem 1rem;
  border-radius: var(--ds-radius-xl);
  font-size: 0.8rem;
  font-weight: 800;
  cursor: pointer;
  transition: color 0.15s ease, background-color 0.15s ease, border-color 0.15s ease, box-shadow 0.15s ease, transform 0.15s ease;
  border: 1.5px solid transparent;
  font-family: inherit;
  white-space: nowrap;
  letter-spacing: 0.02em;
}
.ei-btn:disabled { opacity: 0.5; cursor: not-allowed; transform: none !important; }

/* Save button */
.ei-btn--save {
  background: var(--ds-surface);
  border-color: var(--ds-border);
  color: var(--ds-text-secondary);
}

.dark .ei-btn--save { border-color: var(--ds-border-strong); color: var(--ds-gray-300); background: var(--ds-gray-800); }

.ei-btn--save:hover:not(:disabled) {
  border-color: var(--ds-primary-border);
  color: var(--ds-primary);
  background: var(--ds-primary-soft);
  transform: translateY(-1px);
}

/* Submit button — most prominent */
.ei-btn--submit {
  background: linear-gradient(135deg, var(--ds-success) 0%, #059669 100%);
  color: white;
  box-shadow: 0 4px 16px rgba(22, 163, 74, 0.35);
  padding: 0.5rem 1.25rem;
  font-size: 0.875rem;
}

.ei-btn--submit:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 6px 24px rgba(22, 163, 74, 0.45);
  filter: brightness(1.05);
}

.ei-btn--submit:active:not(:disabled) {
  transform: translateY(0);
}

/* ── Fullscreen warning ─────────────────────────────── */
.ei-fs-warn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.75rem;
  padding: 0.75rem 1.25rem;
  background: var(--ds-warning-soft);
  border-bottom: 1px solid var(--ds-warning-border, rgba(217,119,6,0.3));
  font-size: 0.8rem;
  font-weight: 600;
  color: var(--ds-warning);
}

.ei-fs-warn__btn {
  padding: 0.3rem 0.875rem;
  border-radius: var(--ds-radius-full);
  border: 1.5px solid var(--ds-warning);
  background: transparent;
  color: var(--ds-warning);
  font-size: 0.75rem;
  font-weight: 700;
  cursor: pointer;
  font-family: inherit;
  transition: color 0.15s ease, background-color 0.15s ease, border-color 0.15s ease, box-shadow 0.15s ease, transform 0.15s ease;
}
.ei-fs-warn__btn:hover { background: var(--ds-warning); color: white; }

/* ── Suspended Overlay ──────────────────────────── */
.ei-suspend-overlay {
  position: fixed;
  inset: 0;
  z-index: 200;
  background: rgba(0,0,0,0.7);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 1.5rem;
}

.ei-suspend-card {
  max-width: 480px;
  width: 100%;
  text-align: center;
  padding: 2.5rem 2rem;
  background: var(--ds-surface);
  border: 2px solid var(--ds-danger);
  border-radius: var(--ds-radius-2xl);
  box-shadow: 0 20px 60px rgba(0,0,0,0.3);
}

.ei-suspend-icon {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: var(--ds-danger-soft);
  color: var(--ds-danger);
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 1.5rem;
}

.ei-suspend-title {
  font-family: var(--ds-font-display);
  font-size: 1.5rem;
  font-weight: 900;
  color: var(--ds-text);
  margin: 0 0 0.75rem;
}

.ei-suspend-desc {
  font-size: 0.9rem;
  color: var(--ds-text-muted);
  margin: 0;
  line-height: 1.6;
}

/* ── Modal ─────────────────────────────────────────── */
.ei-modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 150;
  background: rgba(0,0,0,0.6);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 1.5rem;
}

.ei-modal {
  max-width: 520px;
  width: 100%;
  background: var(--ds-surface);
  border-radius: var(--ds-radius-2xl);
  box-shadow: 0 20px 60px rgba(0,0,0,0.3);
  overflow: hidden;
}

.ei-modal--warn { border: 2px solid var(--ds-warning); }
.ei-modal--error { border: 2px solid var(--ds-danger); }
.ei-modal--success { border: 2px solid var(--ds-success); }
.ei-modal--info { border: 2px solid var(--ds-info); }

.ei-modal__header {
  display: flex;
  align-items: flex-start;
  gap: 0.875rem;
  padding: 1.25rem 1.5rem;
  border-bottom: 1px solid var(--ds-border);
}

.ei-modal__icon {
  width: 48px;
  height: 48px;
  border-radius: var(--ds-radius-xl);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.ei-modal__icon--warn { background: var(--ds-warning-soft); color: var(--ds-warning); }
.ei-modal__icon--error { background: var(--ds-danger-soft); color: var(--ds-danger); }
.ei-modal__icon--success { background: var(--ds-success-soft); color: var(--ds-success); }
.ei-modal__icon--info { background: var(--ds-info-soft); color: var(--ds-info); }

.ei-modal__title {
  font-family: var(--ds-font-display);
  font-size: 1.1rem;
  font-weight: 800;
  color: var(--ds-text);
  margin: 0;
}
.ei-modal__sub { font-size: 0.78rem; color: var(--ds-text-muted); margin: 0.25rem 0 0; }

.ei-modal__body { padding: 1.25rem 1.5rem; }

.ei-modal__msg {
  font-size: 0.9rem;
  color: var(--ds-text);
  line-height: 1.6;
  padding: 1rem;
  background: rgba(245,158,11,0.06);
  border: 1px solid rgba(245,158,11,0.2);
  border-radius: var(--ds-radius-xl);
  margin: 0;
}

.ei-modal__confirm {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  width: calc(100% - 3rem);
  margin: 0 1.5rem 1.5rem;
  padding: 0.75rem;
  border-radius: var(--ds-radius-xl);
  border: none;
  background: var(--ds-warning);
  color: white;
  font-size: 0.9rem;
  font-weight: 700;
  cursor: pointer;
  font-family: inherit;
  transition: color 0.15s ease, background-color 0.15s ease, border-color 0.15s ease, box-shadow 0.15s ease, transform 0.15s ease;
}
.ei-modal__confirm:hover { background: var(--ds-primary-hover); transform: translateY(-1px); }
.ei-modal__confirm--error { background: var(--ds-danger); }
.ei-modal__confirm--error:hover { background: var(--ds-danger-hover); }
.ei-modal__confirm--success { background: var(--ds-success); }
.ei-modal__confirm--success:hover { background: var(--ds-success-hover); }
.ei-modal__confirm--info { background: var(--ds-info); }
.ei-modal__confirm--info:hover { background: var(--ds-info-hover); }

/* ── Main Layout ────────────────────────────────────── */
.ei-main {
  flex: 1;
  display: grid;
  grid-template-columns: 1fr 300px;
  gap: 1rem;
  padding: 1rem 1.25rem;
  max-width: 1280px;
  width: 100%;
  margin: 0 auto;
  align-items: start;
}

@media (max-width: 900px) {
  .ei-main { grid-template-columns: 1fr; }
  .ei-sidebar { order: -1; }
}

/* ── Empty States ─────────────────────────────────── */
.ei-empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 0.75rem;
  padding: 4rem 2rem;
  text-align: center;
  background: var(--ds-surface);
  border: 1.5px dashed var(--ds-border);
  border-radius: var(--ds-radius-2xl);
}

.ei-empty-icon { color: var(--ds-text-muted); opacity: 0.5; }
.ei-empty-title { font-size: 1rem; font-weight: 700; color: var(--ds-text); margin: 0; }
.ei-empty-desc { font-size: 0.8rem; color: var(--ds-text-muted); margin: 0; max-width: 280px; }

/* ── Question Card ─────────────────────────────────── */
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
  padding: 1rem 1.25rem;
  border-bottom: 1px solid var(--ds-border);
  background: var(--ds-gray-50);
}

/* Question progress ring */
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
  font-size: 0.65rem;
  font-weight: 900;
  color: var(--ds-primary);
}

.dark .ei-q-ring-label { color: var(--ds-primary-soft, #818cf8); }

.ei-q-badge {
  font-size: 0.78rem;
  font-weight: 700;
  color: var(--ds-text-muted);
}

.ei-mark-btn {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  padding: 0.4rem 0.875rem;
  border: 1.5px solid var(--ds-border);
  background: var(--ds-surface);
  border-radius: var(--ds-radius-full);
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--ds-text-muted);
  cursor: pointer;
  font-family: inherit;
  transition: color 0.15s ease, background-color 0.15s ease, border-color 0.15s ease, box-shadow 0.15s ease, transform 0.15s ease;
}
.ei-mark-btn:hover:not(:disabled) { border-color: var(--ds-warning); color: var(--ds-warning); background: var(--ds-warning-soft); }
.ei-mark-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.ei-mark-btn--sm { font-size: 0.7rem; padding: 0.3rem 0.75rem; }

.ei-q-content {
  padding: 1.5rem 1.25rem 1rem;
}

.ei-q-text {
  font-size: 1.1rem;
  font-weight: 700;
  color: var(--ds-text);
  line-height: 1.6;
  margin: 0;
}

.ei-q-answer {
  padding: 0 1.25rem 1rem;
}

.ei-q-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  padding: 1rem 1.25rem;
  border-top: 1px solid var(--ds-border);
  background: var(--ds-gray-50);
}

.ei-nav-btn {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  padding: 0.6rem 1.25rem;
  border-radius: var(--ds-radius-xl);
  font-size: 0.8rem;
  font-weight: 700;
  cursor: pointer;
  font-family: inherit;
  transition: color 0.15s ease, background-color 0.15s ease, border-color 0.15s ease, box-shadow 0.15s ease, transform 0.15s ease;
  border: 1.5px solid var(--ds-border);
}
.ei-nav-btn:disabled { opacity: 0.4; cursor: not-allowed; }
.ei-nav-btn--prev { background: var(--ds-surface); color: var(--ds-text-secondary); }
.ei-nav-btn--prev:hover:not(:disabled) { border-color: var(--ds-primary); color: var(--ds-primary); }
.ei-nav-btn--next { background: var(--ds-primary); color: white; border-color: var(--ds-primary); box-shadow: 0 2px 8px rgba(79,70,229,0.3); }
.ei-nav-btn--next:hover:not(:disabled) { background: var(--edu-primary-hover); transform: translateY(-1px); }

/* ── Sidebar ─────────────────────────────────────────── */
.ei-sidebar {
  display: flex;
  flex-direction: column;
  gap: 0.875rem;
  position: sticky;
  top: 80px;
}

/* Camera card */
.ei-cam-card {
  background: var(--ds-surface);
  border: 1px solid var(--ds-border);
  border-radius: var(--ds-radius-2xl);
  overflow: hidden;
  box-shadow: var(--ds-shadow-sm);
}

.ei-cam-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.625rem 0.875rem;
  border-bottom: 1px solid var(--ds-border);
}

.ei-cam-label { font-size: 0.72rem; font-weight: 700; color: var(--ds-text-muted); }

.ei-cam-actions { display: flex; gap: 0.375rem; }

.ei-cam-btn {
  width: 28px;
  height: 28px;
  border-radius: var(--ds-radius-md);
  border: none;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: color 0.15s ease, background-color 0.15s ease, border-color 0.15s ease, box-shadow 0.15s ease, transform 0.15s ease;
}
.ei-cam-btn--on { background: var(--ds-success-soft); color: var(--ds-success); }
.ei-cam-btn--off { background: var(--ds-gray-100); color: var(--ds-text-muted); }
.ei-cam-btn--on:hover { background: rgba(22,163,74,0.2); }
.ei-cam-btn--off:hover { background: var(--ds-gray-200); }

.ei-cam-preview {
  position: relative;
  aspect-ratio: 4/3;
  background: var(--ds-gray-900);
  border-radius: 0 0 var(--ds-radius-2xl) var(--ds-radius-2xl);
  overflow: hidden;
}

.ei-cam-video {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transform: scaleX(-1);
  transition: opacity 0.3s ease;
}
.ei-cam-video--hidden { opacity: 0; }

.ei-cam-off {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  background: linear-gradient(135deg, rgba(0,0,0,0.8) 0%, rgba(20,20,30,0.85) 100%);
  color: var(--ds-gray-500);
  transition: opacity 0.3s ease;
}

/* Progress card */
.ei-prog-card {
  background: var(--ds-surface);
  border: 1px solid var(--ds-border);
  border-radius: var(--ds-radius-2xl);
  padding: 0.875rem 1rem;
  box-shadow: var(--ds-shadow-sm);
}

.ei-prog-header {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 0.625rem;
  color: var(--ds-text-secondary);
}

.ei-prog-title { font-size: 0.75rem; font-weight: 700; color: var(--ds-text-muted); flex: 1; }
.ei-prog-pct { font-family: var(--ds-font-display); font-size: 1.1rem; font-weight: 900; color: var(--ds-primary); }

.ei-prog-bar {
  height: 6px;
  background: var(--ds-gray-100);
  border-radius: 3px;
  overflow: hidden;
  margin-bottom: 0.625rem;
}
.ei-prog-bar__fill { height: 100%; background: var(--ds-primary); border-radius: 3px; transition: width 0.5s ease; }

.ei-prog-stats {
  display: flex;
  gap: 0.75rem;
  margin-bottom: 0.5rem;
}

.ei-prog-stat {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  font-size: 0.7rem;
  font-weight: 600;
  color: var(--ds-text-muted);
}

.ei-prog-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}
.ei-prog-dot--done { background: var(--ds-primary); }
.ei-prog-dot--skip { background: var(--ds-gray-300); }

.ei-prog-tags { display: flex; flex-wrap: wrap; gap: 0.375rem; }

.ei-prog-tag {
  display: inline-flex;
  align-items: center;
  gap: 0.3rem;
  padding: 0.2rem 0.625rem;
  border-radius: var(--ds-radius-full);
  font-size: 0.65rem;
  font-weight: 700;
}
.ei-prog-tag--mark { background: var(--ds-warning-soft); color: var(--ds-warning); border: 1px solid rgba(245,158,11,0.2); }
.ei-prog-tag--skip { background: var(--ds-gray-100); color: var(--ds-text-muted); border: 1px solid var(--ds-border); }

/* Question Navigator Card */
.ei-nav-card {
  background: var(--ds-surface);
  border: 1px solid var(--ds-border);
  border-radius: var(--ds-radius-2xl);
  padding: 0.875rem 1rem;
  box-shadow: var(--ds-shadow-sm);
}

.ei-nav-card__header {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.75rem;
  font-weight: 700;
  color: var(--ds-text-muted);
  margin-bottom: 0.75rem;
}

.ei-nav-card__count {
  margin-left: auto;
  padding: 0.1rem 0.5rem;
  background: var(--ds-primary-soft);
  color: var(--ds-primary);
  border-radius: var(--ds-radius-full);
  font-size: 0.65rem;
}

.ei-q-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 0.375rem;
  margin-bottom: 0.625rem;
}

.ei-q-grid-btn {
  aspect-ratio: 1;
  border-radius: var(--ds-radius-lg);
  border: 1.5px solid transparent;
  font-size: 0.72rem;
  font-weight: 700;
  cursor: pointer;
  transition: color 0.15s ease, background-color 0.15s ease, border-color 0.15s ease, box-shadow 0.15s ease, transform 0.15s ease;
  display: flex;
  align-items: center;
  justify-content: center;
}

.ei-q-grid-btn--default { background: var(--ds-gray-100); color: var(--ds-text-muted); border-color: var(--ds-border); }
.ei-q-grid-btn--active {
  background: var(--ds-primary);
  color: white;
  border-color: var(--ds-primary);
  box-shadow: 0 0 0 2px rgba(79,70,229,0.3), 0 4px 12px rgba(79,70,229,0.3);
  transform: scale(1.05);
}
.ei-q-grid-btn--done { background: var(--ds-primary); color: white; border-color: var(--ds-primary); }
.ei-q-grid-btn--mark { background: var(--ds-warning-soft); color: var(--ds-warning); border-color: rgba(245,158,11,0.4); }
.ei-q-grid-btn--mark-done { background: var(--ds-warning); color: white; border-color: var(--ds-warning); }
.ei-q-grid-btn--skip { background: var(--ds-gray-200); color: var(--ds-text-secondary); border-color: var(--ds-gray-300); }
.ei-q-grid-btn:hover:not(:disabled) { transform: scale(1.08); }
.ei-q-grid-btn--active:hover { transform: scale(1.1); }

.ei-q-legend {
  display: flex;
  gap: 0.625rem;
  flex-wrap: wrap;
}

.ei-legend-item {
  display: flex;
  align-items: center;
  gap: 0.3rem;
  font-size: 0.62rem;
  font-weight: 600;
  color: var(--ds-text-muted);
}

.ei-legend-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}
.ei-legend-dot--done { background: var(--ds-primary); }
.ei-legend-dot--mark { background: var(--ds-warning); }
.ei-legend-dot--skip { background: var(--ds-gray-300); }

/* ── Submit Modal Content ─────────────────────────────── */
.ei-submit-info { padding: 0.25rem 0; }

.ei-submit-count {
  font-size: 0.9rem;
  color: var(--ds-text);
  line-height: 1.5;
  margin: 0 0 1rem;
}
.ei-submit-warn { color: var(--ds-warning); font-weight: 700; }

.ei-submit-stats {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0.625rem;
  margin-bottom: 1rem;
}

.ei-submit-stat {
  padding: 0.75rem;
  background: var(--ds-gray-50);
  border: 1px solid var(--ds-border);
  border-radius: var(--ds-radius-xl);
  text-align: center;
}
.ei-submit-stat__label { font-size: 0.65rem; font-weight: 600; color: var(--ds-text-muted); text-transform: uppercase; letter-spacing: 0.05em; margin-bottom: 0.25rem; }
.ei-submit-stat__val { font-family: var(--ds-font-display); font-size: 1.25rem; font-weight: 900; color: var(--ds-text); }
.ei-submit-stat__val--warn { color: var(--ds-warning); }

.ei-submit-note { font-size: 0.8rem; color: var(--ds-text-muted); margin: 0 0 1rem; }

.ei-submit-unanswered {
  display: flex;
  align-items: flex-start;
  gap: 0.5rem;
  padding: 0.875rem;
  background: rgba(245,158,11,0.06);
  border: 1px solid rgba(245,158,11,0.2);
  border-radius: var(--ds-radius-xl);
  font-size: 0.8rem;
  font-weight: 600;
  color: var(--ds-warning);
  line-height: 1.4;
}

.ei-modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.625rem;
  width: 100%;
}

/* ── Transitions ────────────────────────────────────── */
.ei-fade-enter-active, .ei-fade-leave-active { transition: opacity 0.2s ease; }
.ei-fade-enter-from, .ei-fade-leave-to { opacity: 0; }

.ei-slide-down-enter-active, .ei-slide-down-leave-active { transition: color 0.25s ease, background-color 0.25s ease, border-color 0.25s ease, box-shadow 0.25s ease, transform 0.25s ease; }
.ei-slide-down-enter-from, .ei-slide-down-leave-to { transform: translateY(-100%); opacity: 0; }

/* Question slide transition */
.ei-q-slide-enter-active {
  transition: color 0.3s cubic-bezier(0.34, 1.56, 0.64, 1), background-color 0.3s cubic-bezier(0.34, 1.56, 0.64, 1), border-color 0.3s cubic-bezier(0.34, 1.56, 0.64, 1), box-shadow 0.3s cubic-bezier(0.34, 1.56, 0.64, 1), transform 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
}
.ei-q-slide-leave-active {
  transition: color 0.2s ease, background-color 0.2s ease, border-color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease-in;
}
.ei-q-slide-enter-from {
  opacity: 0;
  transform: translateX(24px);
}
.ei-q-slide-leave-to {
  opacity: 0;
  transform: translateX(-24px);
}
</style>
@media (prefers-reduced-motion: reduce) {
  * {
    transition-duration: 0.01ms !important;
    animation-duration: 0.01ms !important;
  }
}