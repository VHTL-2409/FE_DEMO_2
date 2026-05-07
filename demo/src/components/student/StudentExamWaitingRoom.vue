<template>
  <div class="flex min-h-full flex-col bg-[var(--ds-bg)]">
    <!-- Nội dung chính: đã bỏ 2 cột quy định + checklist dài -->
    <div class="mx-auto w-full max-w-3xl flex-1 px-4 pb-4 pt-4 sm:px-6 lg:px-8">
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
          :require-camera-mic="cameraGateRequired"
          @support="openSupport"
        />
      </div>

      <!-- Tóm tắt nhanh: thời gian, số câu, camera/mic, kiểm tra lại -->
      <div
        class="rounded-2xl border px-4 py-3 sm:px-5"
        style="border-color: var(--ds-border); background: var(--ds-surface)"
      >
        <div class="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div class="flex flex-wrap items-center gap-x-4 gap-y-2 text-sm font-semibold text-[var(--ds-text)]">
            <span class="inline-flex items-center gap-1.5">
              <LucideIcon name="timer" class="text-[var(--ds-primary)]" />
              {{ examDuration }} phút
            </span>
            <span class="inline-flex items-center gap-1.5">
              <LucideIcon name="help" class="text-[var(--ds-primary)]" />
              {{ totalQuestions }} câu
            </span>
          </div>
          <div class="flex flex-wrap items-center gap-2 sm:justify-end">
            <span
              class="inline-flex items-center gap-1 rounded-lg px-2 py-1 text-xs font-semibold"
              :class="cameraBadgeClass"
            >
              <LucideIcon name="videocam" class="text-[1rem]" />
              Camera {{ cameraStatusText }}
            </span>
            <span
              class="inline-flex items-center gap-1 rounded-lg px-2 py-1 text-xs font-semibold"
              :class="micBadgeClass"
            >
              <LucideIcon name="mic" class="text-[1rem]" />
              Mic {{ micStatusText }}
            </span>
            <button
              type="button"
              class="inline-flex items-center gap-1 rounded-lg border px-2.5 py-1 text-xs font-bold transition hover:bg-[var(--ds-gray-50)] dark:hover:bg-[var(--ds-gray-800)]"
              style="border-color: var(--ds-border); color: var(--ds-primary)"
              :disabled="isCheckingDevices"
              @click="checkDevices"
            >
              <LucideIcon name="refresh" />
              {{ isCheckingDevices ? 'Đang kiểm tra…' : 'Kiểm tra lại' }}
            </button>
          </div>
        </div>
        <div v-if="examClassName" class="mt-3 flex flex-wrap items-center gap-2 text-xs font-semibold text-[var(--ds-text-secondary)]">
          <span class="inline-flex items-center gap-1 rounded-lg px-2 py-1" style="background: var(--ds-primary-soft); color: var(--ds-primary)">
            <LucideIcon name="school" class="text-[1rem]" />
            Lớp được vào thi: {{ examClassName }}
          </span>
          <span class="inline-flex items-center gap-1 rounded-lg px-2 py-1" style="background: var(--ds-gray-100); color: var(--ds-text-secondary)">
            <LucideIcon name="verified_user" class="text-[1rem]" />
            Chỉ học sinh thuộc lớp này mới được bắt đầu
          </span>
        </div>
        <p v-if="deviceError" class="mt-2 text-xs font-medium text-red-600 dark:text-red-400">
          {{ deviceError }}
        </p>
        <p class="mt-3 text-center text-xs text-[var(--ds-text-muted)]">
          Cần hỗ trợ?
          <button type="button" class="font-semibold hover:underline" style="color: var(--ds-primary)" @click="openSupport">
            Liên hệ hỗ trợ
          </button>
        </p>
      </div>
    </div>

    <!-- CTA cố định phía dưới (sticky trong khung nội dung — không tràn sang sidebar) -->
    <div
      class="sticky bottom-0 z-20 mt-auto w-full border-t"
      style="border-color: var(--ds-border); background: color-mix(in srgb, var(--ds-bg) 97%, transparent); box-shadow: 0 -8px 32px rgba(15, 23, 42, 0.06)"
    >
      <div class="mx-auto flex w-full max-w-lg justify-center px-4 py-3">
        <StartExamPanel
          compact
          join-label="Tham gia bài thi"
          :can-start="canStart"
          :is-starting="isStarting"
          :is-ended="isEnded"
          :is-before-start="!!startAtDate && nowMs < startAtDate.getTime()"
          :devices-ready="devicesReady"
          :devices-verified="devicesVerified"
          :require-camera-mic="cameraGateRequired"
          @start="goToExamInterface"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useNotifications } from '../../composables/useNotifications'
import { startAttempt } from '../../services/attemptService'
import { getExamDetail } from '../../services/examService'
import { useToast } from '../../composables/useToast'
import { useRoute, useRouter } from 'vue-router'
import { parseBackendDate } from '../../utils/dateUtils'
import { buildAttemptQuery } from '../../services/studentExamContextStorage'

import ExamLobbyHeader from './lobby/ExamLobbyHeader.vue'
import StartExamPanel from './lobby/StartExamPanel.vue'

const route = useRoute()
const router = useRouter()
const { openSupport } = useNotifications()
const isStarting = ref(false)
const isSyncing = ref(false)
const nowMs = ref(Date.now())
const examDetail = ref(null)
const lastSyncedAt = ref(null)
const cameraReady = ref(false)
const micReady = ref(false)
const deviceError = ref('')
const isCheckingDevices = ref(false)
/** Tracks whether a camera check has ever completed (pass or fail).
 *  Used to show "Chưa kiểm tra" vs "OK" / "chưa OK" in the UI. */
const cameraVerified = ref(false)

const toast = useToast()
let timerId = null
let examRefreshTimerId = null

const examId = computed(() => Number.parseInt(String(route.query.examId || ''), 10) || null)
const examCode = computed(() => String(route.query.examCode || '-'))
const examTitle = computed(() => examDetail.value?.title || route.query.exam || 'Kỳ thi')
const examClassName = computed(() => String(examDetail.value?.className || route.query.className || '').trim())
const examDuration = computed(() =>
  Number.parseInt(String(examDetail.value?.durationMinutes ?? route.query.duration ?? ''), 10) || 60
)
const totalQuestions = computed(() =>
  Number.parseInt(String(examDetail.value?.questionCount ?? route.query.questions ?? ''), 10) || 0
)
const startAtRaw = computed(() => String(examDetail.value?.startTime || route.query.startAt || ''))
const endAtRaw = computed(() => String(examDetail.value?.endTime || route.query.endAt || ''))
const startAtDate = computed(() => parseBackendDate(startAtRaw.value))
const endAtDate = computed(() => parseBackendDate(endAtRaw.value))

const isEnded = computed(() => endAtDate.value ? nowMs.value > endAtDate.value.getTime() : false)

const requireCameraMic = computed(() => {
  if (examDetail.value?.requireCameraMic != null) {
    return examDetail.value.requireCameraMic !== false
  }
  if (route.query.requireCameraMic === 'false') return false
  return true
})
const enableAiProctoring = computed(() => {
  if (examDetail.value?.enableAiProctoring != null) {
    return examDetail.value.enableAiProctoring === true
  }
  if (route.query.enableAiProctoring === 'true') return true
  return false
})
const cameraGateRequired = computed(() => requireCameraMic.value || enableAiProctoring.value)

/**
 * True when the camera has been verified.
 * Mic remains informational in this flow so a missing audio device does not block video capture.
 */
const devicesVerified = computed(() =>
  cameraVerified.value && cameraReady.value
)

/**
 * Whether devices must be ready before starting.
 * When requireCameraMic = true: must be verified.
 * When requireCameraMic = false: still show status but allow starting if not checked.
 */
const devicesReady = computed(() => {
  if (!cameraGateRequired.value) return true // not a hard requirement
  return devicesVerified.value
})
const cameraStatusText = computed(() => {
  if (!cameraGateRequired.value && !cameraVerified.value) return 'tùy chọn'
  if (!cameraVerified.value) return 'chưa kiểm tra'
  if (cameraReady.value) return 'OK'
  return deviceError.value ? 'lỗi quyền' : 'chưa OK'
})
const micStatusText = computed(() => {
  if (!cameraGateRequired.value && !cameraVerified.value) return 'tùy chọn'
  if (!cameraVerified.value) return 'chưa kiểm tra'
  if (micReady.value) return 'OK'
  return 'chưa OK'
})
const cameraBadgeClass = computed(() => {
  if (!cameraGateRequired.value && !cameraVerified.value) return 'bg-slate-500/10 text-slate-500 dark:text-slate-400'
  if (!cameraVerified.value) return 'bg-gray-500/10 text-gray-500 dark:text-gray-400'
  return cameraReady.value
    ? 'bg-emerald-500/10 text-emerald-700 dark:text-emerald-400'
    : 'bg-red-500/10 text-red-700 dark:text-red-400'
})
const micBadgeClass = computed(() => {
  if (!cameraGateRequired.value && !cameraVerified.value) return 'bg-slate-500/10 text-slate-500 dark:text-slate-400'
  if (!cameraVerified.value) return 'bg-gray-500/10 text-gray-500 dark:text-gray-400'
  return micReady.value
    ? 'bg-emerald-500/10 text-emerald-700 dark:text-emerald-400'
    : 'bg-amber-500/10 text-amber-700 dark:text-amber-400'
})

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

const enterExamFullscreen = async () => {
  if (typeof document === 'undefined') return false
  if (document.fullscreenElement || document.webkitFullscreenElement) return true
  const target = document.documentElement
  const request = target?.requestFullscreen || target?.webkitRequestFullscreen
  if (!request) return false
  try {
    await request.call(target, { navigationUI: 'hide' })
  } catch {
    try {
      await request.call(target)
    } catch {
      return false
    }
  }
  return Boolean(document.fullscreenElement || document.webkitFullscreenElement)
}

const exitExamFullscreen = async () => {
  if (typeof document === 'undefined') return false
  if (!document.fullscreenElement && !document.webkitFullscreenElement) return false
  const exit = document.exitFullscreen || document.webkitExitFullscreen
  if (!exit) return false
  try {
    await exit.call(document)
    return true
  } catch {
    return false
  }
}

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
  if (!navigator?.mediaDevices?.getUserMedia) {
    cameraReady.value = false
    micReady.value = false
    cameraVerified.value = true
    deviceError.value = 'Trình duyệt không hỗ trợ kiểm tra camera/micro.'
    toast.error(deviceError.value)
    return
  }

  isCheckingDevices.value = true
  deviceError.value = ''
  try {
    const videoStream = await navigator.mediaDevices.getUserMedia({ video: true, audio: false })
    const videoTrack = videoStream.getVideoTracks()[0]
    cameraReady.value = Boolean(videoTrack && videoTrack.readyState !== 'ended')
    cameraVerified.value = true

    try {
      const audioStream = await navigator.mediaDevices.getUserMedia({ video: false, audio: true })
      const audioTrack = audioStream.getAudioTracks()[0]
      micReady.value = Boolean(audioTrack)
      audioStream.getTracks().forEach((track) => track.stop())
      deviceError.value = ''
    } catch (audioError) {
      micReady.value = false
      deviceError.value = 'Camera đã sẵn sàng, nhưng micro chưa khả dụng. Vẫn có thể vào thi.'
      if (audioError?.name === 'NotAllowedError') {
        deviceError.value = 'Camera đã sẵn sàng, nhưng micro chưa được cấp quyền. Vẫn có thể vào thi.'
      }
    }

    videoStream.getTracks().forEach((track) => track.stop())
  } catch (error) {
    cameraReady.value = false
    micReady.value = false
    deviceError.value = error?.name === 'NotAllowedError'
      ? 'Bạn cần cấp quyền camera để vào phòng thi.'
      : 'Không thể truy cập camera/micro. Vui lòng kiểm tra thiết bị.'
    toast.error(deviceError.value)
    cameraVerified.value = true
  } finally {
    isCheckingDevices.value = false
  }
}

watch(cameraGateRequired, (enabled, previous) => {
  if (enabled && !previous && !cameraVerified.value) {
    void checkDevices()
    return
  }
  if (!enabled) {
    cameraVerified.value = false
    cameraReady.value = false
    micReady.value = false
    deviceError.value = ''
  }
})

const goToExamInterface = async () => {
  if (!examId.value) {
    toast.error('Thiếu mã bài thi. Vui lòng vào lại từ trang chủ.')
    return
  }

  if (!canStart.value) {
    if (isEnded.value) {
      toast.error('Bài thi đã kết thúc.')
      return
    }
    if (!devicesVerified.value && cameraGateRequired.value) {
      toast.error('Bạn cần bật camera để vào phòng thi.')
    } else if (startAtDate.value && nowMs.value < startAtDate.value.getTime()) {
      toast.error('Bài thi chưa bắt đầu.')
    }
    return
  }

  isStarting.value = true
  try {
    const fullscreenReady = await enterExamFullscreen()
    if (!fullscreenReady) {
      toast.warning('Trình duyệt chưa cho phép vào toàn màn hình. Hãy thử lại để bắt đầu bài thi.')
      return
    }

    await refreshExamDetail()

    if (isEnded.value) {
      await exitExamFullscreen()
      toast.error('Bài thi đã kết thúc.')
      return
    }

    if (!canStart.value) {
      await exitExamFullscreen()
      if (!devicesVerified.value && cameraGateRequired.value) {
        toast.error('Bạn cần bật camera để vào phòng thi.')
      } else if (startAtDate.value && nowMs.value < startAtDate.value.getTime()) {
        toast.error('Bài thi chưa bắt đầu.')
      }
      return
    }

    const started = await startAttempt(examId.value)
    router.push({
      path: '/student/exam-interface',
      query: buildAttemptQuery({
        examTitle: examTitle.value,
        examId: examId.value,
        attemptId: started.attemptId,
        deadlineAt: started.deadlineAt,
        remainingSeconds: started.remainingSeconds
      })
    })
  } catch {
    await exitExamFullscreen()
    toast.error('Không thể bắt đầu bài thi lúc này.')
  } finally {
    isStarting.value = false
  }
}

onMounted(async () => {
  await refreshExamDetail()
  if (cameraGateRequired.value) {
    await checkDevices()
  } else {
    cameraVerified.value = false
    cameraReady.value = false
    micReady.value = false
    deviceError.value = ''
  }
  timerId = window.setInterval(() => { nowMs.value = Date.now() }, 1000)
  examRefreshTimerId = window.setInterval(refreshExamDetail, 15000)
})

onUnmounted(() => {
  if (timerId) window.clearInterval(timerId)
  if (examRefreshTimerId) window.clearInterval(examRefreshTimerId)
})
</script>
