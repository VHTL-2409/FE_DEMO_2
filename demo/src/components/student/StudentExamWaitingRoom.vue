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

      <div
        v-if="identityRequired"
        class="mt-4 rounded-2xl border px-4 py-4 sm:px-5"
        style="border-color: var(--ds-border); background: var(--ds-surface)"
      >
        <div class="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
          <div>
            <div class="flex items-center gap-2 text-sm font-bold text-[var(--ds-text)]">
              <LucideIcon name="verified_user" class="text-[var(--ds-primary)]" />
              Xác minh danh tính
            </div>
            <p class="mt-1 text-xs font-medium text-[var(--ds-text-secondary)]">
              Chụp giấy tờ hoặc thẻ sinh viên và selfie trực tiếp trước khi vào phòng thi.
            </p>
          </div>
          <span
            class="inline-flex items-center gap-1 rounded-lg px-2 py-1 text-xs font-bold"
            :class="identityBadgeClass"
          >
            <LucideIcon :name="identityStatusIcon" />
            {{ identityStatusText }}
          </span>
        </div>

        <div class="mt-4 grid gap-3 sm:grid-cols-2">
          <label class="block rounded-xl border border-dashed px-3 py-3 text-xs font-semibold text-[var(--ds-text-secondary)]" style="border-color: var(--ds-border)">
            <span class="mb-2 flex items-center gap-1 text-[var(--ds-text)]">
              <LucideIcon name="id-card" />
              Ảnh giấy tờ
            </span>
            <input type="file" accept="image/*" class="block w-full text-xs" @change="handleDocumentFile" />
            <span v-if="documentImageBase64" class="mt-2 inline-flex text-emerald-600">
              Đã chọn {{ documentFileName || 'ảnh giấy tờ' }}
            </span>
          </label>
          <div class="rounded-xl border border-dashed px-3 py-3 text-xs font-semibold text-[var(--ds-text-secondary)]" style="border-color: var(--ds-border)">
            <span class="mb-2 flex items-center gap-1 text-[var(--ds-text)]">
              <LucideIcon name="scan-face" />
              Selfie từ camera
            </span>
            <button
              type="button"
              class="inline-flex items-center gap-1 rounded-lg border px-2.5 py-1 text-xs font-bold transition hover:bg-[var(--ds-gray-50)] dark:hover:bg-[var(--ds-gray-800)]"
              style="border-color: var(--ds-border); color: var(--ds-primary)"
              :disabled="isCapturingSelfie || !cameraReady"
              @click="captureSelfie"
            >
              <LucideIcon name="camera" />
              {{ isCapturingSelfie ? 'Đang chụp...' : 'Chụp selfie' }}
            </button>
            <span v-if="selfieImageBase64" class="ml-2 text-emerald-600">Đã chụp</span>
          </div>
        </div>

        <div class="mt-4 flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
          <p class="text-xs font-medium" :class="identityMessageClass">
            {{ identityMessage }}
          </p>
          <button
            type="button"
            class="inline-flex items-center justify-center gap-1 rounded-lg px-3 py-2 text-xs font-bold text-white transition disabled:cursor-not-allowed disabled:opacity-60"
            style="background: var(--ds-primary)"
            :disabled="!canRunIdentityCheck"
            @click="runIdentityVerification"
          >
            <LucideIcon name="shield_check" />
            {{ isVerifyingIdentity ? 'Đang xác minh...' : 'Xác minh danh tính' }}
          </button>
        </div>
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
          join-label="Xem quy chế và cam kết"
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
import { prepareAttempt } from '../../services/attemptService'
import { getExamDetail } from '../../services/examService'
import { fetchIdentityCheck, verifyStudentIdentity } from '../../services/proctorService'
import { updateDeviceStatus } from '../../services/monitoringService'
import { useToast } from '../../composables/useToast'
import { useRoute, useRouter } from 'vue-router'
import { parseBackendDate } from '../../utils/dateUtils'
import { buildRulesQuery } from '../../services/studentExamContextStorage'

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
const documentImageBase64 = ref('')
const documentFileName = ref('')
const selfieImageBase64 = ref('')
const identityStatus = ref('NOT_CHECKED')
const identityResult = ref(null)
const isVerifyingIdentity = ref(false)
const isCapturingSelfie = ref(false)
const preparedAttempt = ref(null)
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
  if (examDetail.value?.aiFaceDetection === true || examDetail.value?.aiEyeTracking === true) return true
  if (examDetail.value?.enableAiProctoring != null) {
    return examDetail.value.enableAiProctoring === true
  }
  if (route.query.aiFaceDetection === 'true' || route.query.aiEyeTracking === 'true') return true
  if (route.query.enableAiProctoring === 'true') return true
  return false
})
const cameraGateRequired = computed(() => requireCameraMic.value || enableAiProctoring.value)
const identityRequired = computed(() => cameraGateRequired.value)
const identityAccepted = computed(() => ['VERIFIED', 'NEEDS_REVIEW'].includes(String(identityStatus.value || '').toUpperCase()))
const identityStatusText = computed(() => {
  switch (String(identityStatus.value || '').toUpperCase()) {
    case 'VERIFIED': return 'Đã xác minh'
    case 'NEEDS_REVIEW': return 'Cần duyệt'
    case 'REJECTED': return 'Cần chụp lại'
    case 'CHECKING': return 'Đang kiểm tra'
    default: return 'Chưa xác minh'
  }
})
const identityStatusIcon = computed(() => {
  switch (String(identityStatus.value || '').toUpperCase()) {
    case 'VERIFIED': return 'check_circle'
    case 'NEEDS_REVIEW': return 'flag'
    case 'REJECTED': return 'error'
    case 'CHECKING': return 'hourglass_empty'
    default: return 'shield'
  }
})
const identityBadgeClass = computed(() => {
  switch (String(identityStatus.value || '').toUpperCase()) {
    case 'VERIFIED': return 'bg-emerald-500/10 text-emerald-700 dark:text-emerald-400'
    case 'NEEDS_REVIEW': return 'bg-amber-500/10 text-amber-700 dark:text-amber-400'
    case 'REJECTED': return 'bg-red-500/10 text-red-700 dark:text-red-400'
    case 'CHECKING': return 'bg-blue-500/10 text-blue-700 dark:text-blue-400'
    default: return 'bg-gray-500/10 text-gray-500 dark:text-gray-400'
  }
})
const identityMessage = computed(() => {
  const status = String(identityStatus.value || '').toUpperCase()
  if (status === 'VERIFIED') return 'Danh tính đã được xác minh.'
  if (status === 'NEEDS_REVIEW') return identityResult.value?.reviewReason || identityResult.value?.review_reason || 'Giám thị sẽ kiểm tra lại bằng chứng.'
  if (status === 'REJECTED') return identityResult.value?.reviewReason || identityResult.value?.review_reason || 'Vui lòng chụp lại giấy tờ và selfie rõ hơn.'
  if (status === 'CHECKING') return 'Hệ thống đang đối chiếu giấy tờ, selfie và hồ sơ thí sinh.'
  return 'Cần hoàn tất xác minh danh tính trước khi vào bài thi.'
})
const identityMessageClass = computed(() => {
  const status = String(identityStatus.value || '').toUpperCase()
  if (status === 'VERIFIED') return 'text-emerald-600 dark:text-emerald-400'
  if (status === 'REJECTED') return 'text-red-600 dark:text-red-400'
  if (status === 'NEEDS_REVIEW') return 'text-amber-600 dark:text-amber-400'
  return 'text-[var(--ds-text-secondary)]'
})
const canRunIdentityCheck = computed(() =>
  !isVerifyingIdentity.value &&
  Boolean(documentImageBase64.value) &&
  Boolean(selfieImageBase64.value) &&
  devicesReady.value &&
  !isEnded.value
)

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
  if (identityRequired.value && !identityAccepted.value) return false
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

const prepareLobbyAttempt = async () => {
  if (!examId.value) return null
  try {
    const prepared = await prepareAttempt(examId.value)
    preparedAttempt.value = prepared
    if (prepared?.attemptId && identityRequired.value) {
      try {
        const latestCheck = await fetchIdentityCheck(prepared.attemptId)
        if (latestCheck?.available && latestCheck?.verificationStatus) {
          identityResult.value = latestCheck
          identityStatus.value = String(latestCheck.verificationStatus).toUpperCase()
        }
      } catch {
        // Identity check restore is best-effort; a new check can still be run.
      }
    }
    return prepared
  } catch {
    return null
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

const fileToDataUrl = (file) => new Promise((resolve, reject) => {
  const reader = new FileReader()
  reader.onload = () => resolve(String(reader.result || ''))
  reader.onerror = reject
  reader.readAsDataURL(file)
})

const handleDocumentFile = async (event) => {
  const file = event?.target?.files?.[0]
  if (!file) return
  if (!file.type?.startsWith('image/')) {
    toast.error('Vui lòng chọn file ảnh giấy tờ.')
    event.target.value = ''
    return
  }
  if (file.size > 6 * 1024 * 1024) {
    toast.error('Ảnh giấy tờ tối đa 6MB.')
    event.target.value = ''
    return
  }
  try {
    documentImageBase64.value = await fileToDataUrl(file)
    documentFileName.value = file.name
    identityResult.value = null
    identityStatus.value = 'NOT_CHECKED'
  } catch {
    toast.error('Không thể đọc ảnh giấy tờ.')
  }
}

const captureSelfie = async () => {
  if (!navigator?.mediaDevices?.getUserMedia) {
    toast.error('Trình duyệt không hỗ trợ camera.')
    return
  }
  isCapturingSelfie.value = true
  let stream = null
  try {
    stream = await navigator.mediaDevices.getUserMedia({ video: true, audio: false })
    const video = document.createElement('video')
    video.playsInline = true
    video.muted = true
    video.srcObject = stream
    await video.play()
    await new Promise((resolve, reject) => {
      const startedAt = Date.now()
      const tick = () => {
        if (video.videoWidth > 0 && video.videoHeight > 0) {
          resolve()
          return
        }
        if (Date.now() - startedAt > 2500) {
          reject(new Error('Camera chưa sẵn sàng.'))
          return
        }
        window.setTimeout(tick, 80)
      }
      tick()
    })
    const width = video.videoWidth
    const height = video.videoHeight
    const canvas = document.createElement('canvas')
    canvas.width = width
    canvas.height = height
    const context = canvas.getContext('2d')
    context.drawImage(video, 0, 0, width, height)
    selfieImageBase64.value = canvas.toDataURL('image/jpeg', 0.82)
    identityResult.value = null
    identityStatus.value = 'NOT_CHECKED'
  } catch {
    toast.error('Không thể chụp selfie từ camera. Vui lòng kiểm tra quyền camera và thử lại.')
  } finally {
    stream?.getTracks?.()?.forEach((track) => track.stop())
    isCapturingSelfie.value = false
  }
}

const ensurePreparedAttempt = async () => {
  if (preparedAttempt.value?.attemptId) return preparedAttempt.value
  return prepareLobbyAttempt()
}

const runIdentityVerification = async () => {
  if (!canRunIdentityCheck.value || !examId.value) return
  isVerifyingIdentity.value = true
  identityStatus.value = 'CHECKING'
  try {
    const prepared = await ensurePreparedAttempt()
    if (!prepared?.attemptId) {
      throw new Error('Missing prepared attempt')
    }
    const result = await verifyStudentIdentity({
      attemptId: prepared.attemptId,
      documentImageBase64: documentImageBase64.value,
      selfieImageBase64: selfieImageBase64.value,
      documentType: 'STUDENT_ID',
      capturedAt: new Date().toISOString(),
      metadata: {
        examId: examId.value,
        examCode: examCode.value,
        cameraOn: cameraReady.value,
        micOn: micReady.value,
        captureSource: 'student_waiting_room'
      }
    })
    identityResult.value = result
    identityStatus.value = String(result?.verificationStatus || result?.verification_status || 'NEEDS_REVIEW').toUpperCase()
    if (identityStatus.value === 'VERIFIED') {
      toast.success('Đã xác minh danh tính.')
    } else if (identityStatus.value === 'NEEDS_REVIEW') {
      toast.warning('Danh tính cần giám thị duyệt. Bạn vẫn có thể vào thi.')
    } else {
      toast.error('Xác minh không đạt. Vui lòng chụp lại.')
    }
  } catch {
    identityStatus.value = 'NOT_CHECKED'
    toast.error('Không thể xác minh danh tính lúc này.')
  } finally {
    isVerifyingIdentity.value = false
  }
}

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
    } else if (identityRequired.value && !identityAccepted.value) {
      toast.error('Bạn cần xác minh danh tính trước khi vào phòng thi.')
    } else if (startAtDate.value && nowMs.value < startAtDate.value.getTime()) {
      toast.error('Bài thi chưa bắt đầu.')
    }
    return
  }

  isStarting.value = true
  try {
    await refreshExamDetail()

    if (isEnded.value) {
      toast.error('Bài thi đã kết thúc.')
      return
    }

    const prepared = await ensurePreparedAttempt()
    if (!prepared?.attemptId) {
      throw new Error('Missing prepared attempt')
    }
    try {
      await updateDeviceStatus(prepared.attemptId, cameraReady.value, micReady.value)
    } catch {
      // The rules page will show backend blocked reasons if device status was not stored.
    }
    preparedAttempt.value = prepared
    router.push({
      path: '/student/exam-rules',
      query: buildRulesQuery({
        examTitle: examTitle.value,
        examId: examId.value,
        attemptId: prepared.attemptId,
        examCode: examCode.value,
        duration: examDuration.value,
        questions: totalQuestions.value,
        startAt: startAtRaw.value,
        endAt: endAtRaw.value,
        className: examClassName.value,
        identityCheckId: identityResult.value?.identityCheckId || '',
        verificationStatus: identityStatus.value,
        requireCameraMic: cameraGateRequired.value ? 'true' : 'false',
        enableAiProctoring: enableAiProctoring.value ? 'true' : 'false'
      })
    })
    return

    /*
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
      } else if (identityRequired.value && !identityAccepted.value) {
        toast.error('Bạn cần xác minh danh tính trước khi vào phòng thi.')
      } else if (startAtDate.value && nowMs.value < startAtDate.value.getTime()) {
        toast.error('Bài thi chưa bắt đầu.')
      }
      return
    }

    const started = await startAttempt(examId.value)
    preparedAttempt.value = started
    router.push({
      path: '/student/exam-interface',
      query: buildAttemptQuery({
        examTitle: examTitle.value,
        examId: examId.value,
        attemptId: started.attemptId,
        deadlineAt: started.deadlineAt,
        remainingSeconds: started.remainingSeconds,
        identityCheckId: identityResult.value?.identityCheckId || '',
        verificationStatus: identityStatus.value
      })
    })
    */
  } catch {
    await exitExamFullscreen()
    toast.error('Không thể chuyển sang trang quy chế lúc này.')
  } finally {
    isStarting.value = false
  }
}

onMounted(async () => {
  await refreshExamDetail()
  await prepareLobbyAttempt()
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
