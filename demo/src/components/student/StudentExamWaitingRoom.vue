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
          :require-camera-mic="requireCameraMic"
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
              :class="!cameraVerified ? 'bg-gray-500/10 text-gray-500 dark:text-gray-400' : cameraReady ? 'bg-emerald-500/10 text-emerald-700 dark:text-emerald-400' : 'bg-red-500/10 text-red-700 dark:text-red-400'"
            >
              <LucideIcon name="videocam" class="text-[1rem]" />
              Camera {{ !cameraVerified ? 'chưa kiểm tra' : cameraReady ? 'OK' : 'chưa OK' }}
            </span>
            <span
              class="inline-flex items-center gap-1 rounded-lg px-2 py-1 text-xs font-semibold"
              :class="!cameraVerified ? 'bg-gray-500/10 text-gray-500 dark:text-gray-400' : micReady ? 'bg-emerald-500/10 text-emerald-700 dark:text-emerald-400' : 'bg-red-500/10 text-red-700 dark:text-red-400'"
            >
              <LucideIcon name="mic" class="text-[1rem]" />
              Mic {{ !cameraVerified ? 'chưa kiểm tra' : micReady ? 'OK' : 'chưa OK' }}
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
          :require-camera-mic="requireCameraMic"
          @start="goToExamInterface"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
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
  if (route.query.requireCameraMic === 'false') return false
  return examDetail.value?.requireCameraMic !== false
})

/**
 * True when devices have been verified to be working.
 * Always checked on mount — never bypassed even if requireCameraMic = false.
 */
const devicesVerified = computed(() =>
  cameraVerified.value && cameraReady.value && micReady.value
)

/**
 * Whether devices must be ready before starting.
 * When requireCameraMic = true: must be verified.
 * When requireCameraMic = false: still show status but allow starting if not checked.
 */
const devicesReady = computed(() => {
  if (!requireCameraMic.value) return true // not a hard requirement
  return devicesVerified.value
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
    const stream = await navigator.mediaDevices.getUserMedia({ video: true, audio: true })
    const videoTrack = stream.getVideoTracks()[0]
    const audioTrack = stream.getAudioTracks()[0]
    cameraReady.value = Boolean(videoTrack)
    micReady.value = Boolean(audioTrack)
    deviceError.value = ''
    cameraVerified.value = true
    stream.getTracks().forEach((track) => track.stop())
  } catch (error) {
    cameraReady.value = false
    micReady.value = false
    deviceError.value = error?.name === 'NotAllowedError'
      ? 'Bạn cần cấp quyền camera và micro để vào phòng thi.'
      : 'Không thể truy cập camera/micro. Vui lòng kiểm tra thiết bị.'
    toast.error(deviceError.value)
    cameraVerified.value = true
  } finally {
    isCheckingDevices.value = false
  }
}

const goToExamInterface = async () => {
  if (!examId.value) {
    toast.error('Thiếu mã bài thi. Vui lòng vào lại từ trang chủ.')
    return
  }

  await refreshExamDetail()

  if (isEnded.value) {
    toast.error('Bài thi đã kết thúc.')
    return
  }

  if (!canStart.value) {
    if (!devicesVerified.value && requireCameraMic.value) {
      toast.error('Bạn cần bật camera để vào phòng thi.')
    } else if (startAtDate.value && nowMs.value < startAtDate.value.getTime()) {
      toast.error('Bài thi chưa bắt đầu.')
    }
    return
  }

  isStarting.value = true
  try {
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
    toast.error('Không thể bắt đầu bài thi lúc này.')
  } finally {
    isStarting.value = false
  }
}

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
