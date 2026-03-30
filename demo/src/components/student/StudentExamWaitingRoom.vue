<template>
  <div class="bg-[var(--ds-bg)] min-h-full">
    <div class="mx-auto max-w-3xl px-4 pb-10 pt-4 sm:px-6 lg:px-8">

      <!-- Header: exam info + countdown -->
      <div class="mb-4">
        <ExamLobbyHeader
          :exam-title="examTitle"
          :exam-code="examCode"
          :duration="examDuration"
          :question-count="totalQuestions"
          :start-at="startAtDate"
          :end-at="endAtDate"
          :now-ms="nowMs"
          :is-syncing="isSyncing"
          :last-synced-label="lastSyncedLabel"
          :require-camera-mic="requireCameraMic"
          @support="openSupport"
        />
      </div>

      <!-- Info + Readiness in 2 columns -->
      <div class="grid grid-cols-1 gap-4 mb-4 md:grid-cols-2">
        <!-- Rules -->
        <ExamRuleCard
          :duration="examDuration"
          :question-count="totalQuestions"
          :require-camera-mic="requireCameraMic"
        />

        <!-- Readiness checklist -->
        <ReadinessChecklist
          :camera-ready="cameraReady"
          :mic-ready="micReady"
          :network-ready="true"
          :is-checking="isCheckingDevices"
          :error-msg="deviceError"
          :require-camera-mic="requireCameraMic"
          @retry="checkDevices"
        />
      </div>

      <!-- Start button -->
      <div class="mb-4">
        <StartExamPanel
          :can-start="canStart"
          :is-starting="isStarting"
          :is-ended="isEnded"
          :is-before-start="!!startAtDate && nowMs < startAtDate.getTime()"
          :devices-ready="devicesReady"
          :require-camera-mic="requireCameraMic"
          @start="goToExamInterface"
        />
      </div>

    </div>

    <!-- Footer -->
    <footer class="shrink-0 border-t px-6 py-2 text-center text-xs md:px-20 lg:px-40" style="border-color: var(--ds-border); color: var(--ds-text-muted);">
      Cần hỗ trợ?
      <button type="button" class="font-semibold hover:underline" style="color: var(--ds-primary);" @click="openSupport">Liên hệ hỗ trợ</button>
    </footer>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useNotifications } from '../../composables/useNotifications'
import { startAttempt } from '../../services/attemptService'
import { getExamDetail } from '../../services/examService'
import { useToast } from '../../composables/useToast'
import { useRoute, useRouter } from 'vue-router'

// Lobby components
import ExamLobbyHeader from './lobby/ExamLobbyHeader.vue'
import ExamRuleCard from './lobby/ExamRuleCard.vue'
import ReadinessChecklist from './lobby/ReadinessChecklist.vue'
import StartExamPanel from './lobby/StartExamPanel.vue'

const route = useRoute()
const router = useRouter()
const { openSupport } = useNotifications()
const isStarting = ref(false)
const isSyncing = ref(false)
const startError = ref('')
const nowMs = ref(Date.now())
const examDetail = ref(null)
const lastSyncedAt = ref(null)
const cameraReady = ref(false)
const micReady = ref(false)
const deviceError = ref('')
const isCheckingDevices = ref(false)

const toast = useToast()
let timerId = null
let examRefreshTimerId = null

// ─── Computed from route / detail ──────────────────────────────────
const examId = computed(() => Number.parseInt(String(route.query.examId || ''), 10) || null)
const examCode = computed(() => String(route.query.examCode || '-'))
const examTitle = computed(() => examDetail.value?.title || route.query.exam || 'Kỳ thi')
const examDuration = computed(() =>
  Number.parseInt(String(examDetail.value?.durationMinutes ?? route.query.duration ?? ''), 10) || 60
)
const totalQuestions = computed(() =>
  Number.parseInt(String(examDetail.value?.questionCount ?? route.query.questions ?? ''), 10) || 0
)
const startAtRaw = computed(() => String(examDetail.value?.startTime || route.query.startAt || ''))
const endAtRaw = computed(() => String(examDetail.value?.endTime || route.query.endAt || ''))
const startAtDate = computed(() => {
  if (!startAtRaw.value) return null
  const date = new Date(startAtRaw.value)
  return Number.isNaN(date.getTime()) ? null : date
})
const endAtDate = computed(() => {
  if (!endAtRaw.value) return null
  const date = new Date(endAtRaw.value)
  return Number.isNaN(date.getTime()) ? null : date
})

const isEnded = computed(() => endAtDate.value ? nowMs.value > endAtDate.value.getTime() : false)

const requireCameraMic = computed(() => {
  if (route.query.requireCameraMic === 'false') return false
  return examDetail.value?.requireCameraMic !== false
})

const devicesReady = computed(() =>
  requireCameraMic.value ? (cameraReady.value && micReady.value) : true
)

const canStart = computed(() => {
  if (isEnded.value) return false
  if (!devicesReady.value) return false
  return !startAtDate.value || nowMs.value >= startAtDate.value.getTime()
})

const lastSyncedLabel = computed(() => {
  if (!lastSyncedAt.value) return ''
  return new Date(lastSyncedAt.value).toLocaleTimeString([], {
    hour: '2-digit', minute: '2-digit', second: '2-digit'
  })
})

// ─── API calls ─────────────────────────────────────────────────────
const refreshExamDetail = async () => {
  if (!examId.value) return
  isSyncing.value = true
  try {
    examDetail.value = await getExamDetail(examId.value)
    lastSyncedAt.value = Date.now()
  } catch {
    // keep fallback query data if refresh fails
  } finally {
    isSyncing.value = false
  }
}

const checkDevices = async () => {
  if (!requireCameraMic.value) {
    cameraReady.value = true
    micReady.value = true
    deviceError.value = ''
    return
  }
  if (!navigator?.mediaDevices?.getUserMedia) {
    cameraReady.value = false
    micReady.value = false
    deviceError.value = 'Trình duyệt không hỗ trợ kiểm tra camera/micro.'
    toast.error(deviceError.value)
    return
  }

  isCheckingDevices.value = true
  deviceError.value = ''
  try {
    const stream = await navigator.mediaDevices.getUserMedia({ video: true, audio: true })
    const videoTrack = stream.getVideoTracks()[0]
    const audioTrack = stream.getAudioTracks()[0]
    cameraReady.value = Boolean(videoTrack)
    micReady.value = Boolean(audioTrack)
    stream.getTracks().forEach((track) => track.stop())
  } catch (error) {
    cameraReady.value = false
    micReady.value = false
    deviceError.value = error?.name === 'NotAllowedError'
      ? 'Bạn cần cấp quyền camera và micro để vào phòng thi.'
      : 'Không thể truy cập camera/micro. Vui lòng kiểm tra thiết bị.'
    toast.error(deviceError.value)
  } finally {
    isCheckingDevices.value = false
  }
}

// ─── Navigation ────────────────────────────────────────────────────
const goToExamInterface = async () => {
  startError.value = ''

  if (!examId.value) {
    toast.error('Thiếu mã bài thi. Vui lòng vào lại từ trang chủ.')
    return
  }

  await refreshExamDetail()
  await checkDevices()

  if (!devicesReady.value) {
    if (!requireCameraMic.value) {
      // no-op
    } else {
      toast.error(deviceError.value || 'Bạn cần cấp quyền camera và micro để vào phòng thi.')
      return
    }
  }

  if (isEnded.value) {
    toast.error('Bài thi đã kết thúc.')
    return
  }

  if (!canStart.value) {
    toast.error('Bài thi chưa bắt đầu.')
    return
  }

  isStarting.value = true
  try {
    const started = await startAttempt(examId.value)
    router.push({
      path: '/student/exam-interface',
      query: {
        exam: examTitle.value,
        examId: examId.value,
        attemptId: started.attemptId,
        deadlineAt: started.deadlineAt,
        remainingSeconds: started.remainingSeconds
      }
    })
  } catch {
    toast.error('Không thể bắt đầu bài thi lúc này.')
  } finally {
    isStarting.value = false
  }
}

// ─── Lifecycle ────────────────────────────────────────────────────
onMounted(async () => {
  await refreshExamDetail()
  await checkDevices()
  timerId = window.setInterval(() => { nowMs.value = Date.now() }, 1000)
  examRefreshTimerId = window.setInterval(refreshExamDetail, 15000)
})

onUnmounted(() => {
  if (timerId) window.clearInterval(timerId)
  if (examRefreshTimerId) window.clearInterval(examRefreshTimerId)
})
</script>
