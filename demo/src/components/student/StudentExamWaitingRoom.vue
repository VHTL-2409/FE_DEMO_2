<template>
  <div :class="isDark ? 'dark' : 'light'" class="portal-viewport flex h-full min-h-0 flex-col bg-background-light font-display text-slate-900 dark:bg-background-dark dark:text-slate-100">
    <div class="relative flex h-full min-h-0 flex-1 w-full flex-col overflow-x-hidden">
      <div class="layout-container flex h-full min-h-0 flex-1 grow flex-col">
        <StudentTopHeader class="shrink-0" />

        <main class="teacher-page-shell relative flex min-h-0 flex-1 flex-col overflow-hidden">
          <div class="portal-scrollbar relative flex min-h-0 flex-1 flex-col overflow-y-auto px-4 py-5 md:py-6">
          <div class="relative flex w-full flex-1 flex-col gap-5 md:gap-6">
            <div class="flex flex-col gap-2 animate-fade-up md:gap-3">
              <div
                class="inline-flex w-fit items-center gap-2 rounded-full bg-primary/10 px-3 py-1 text-sm font-semibold text-primary"
              >
                <span class="relative flex h-2 w-2">
                  <span class="animate-ping absolute inline-flex h-full w-full rounded-full bg-primary opacity-75"></span>
                  <span class="relative inline-flex h-2 w-2 rounded-full bg-primary"></span>
                </span>
                Phòng chờ đang hoạt động
              </div>
              <PageHeader
                class="!mb-0 [&_.teacher-page-title]:text-slate-900 dark:[&_.teacher-page-title]:text-white"
                title="Đang chờ bài thi bắt đầu"
              />
              <div class="flex flex-wrap items-center gap-2 text-xs">
                <span class="inline-flex items-center gap-1.5 px-2 py-1 rounded-full border border-slate-200 dark:border-slate-700 text-slate-600 dark:text-slate-300 bg-white/80 dark:bg-slate-900/70">
                  <span :class="isSyncing ? 'bg-amber-500' : 'bg-emerald-500'" class="size-2 rounded-full"></span>
                  {{ isSyncing ? 'Đang đồng bộ...' : 'Đồng bộ ổn định' }}
                </span>
                <span class="text-slate-500 dark:text-slate-400">Cập nhật gần nhất: {{ lastSyncedLabel }}</span>
              </div>
            </div>

            <div class="grid grid-cols-1 gap-4 animate-fade-up-delay md:grid-cols-2 md:gap-5">
              <BaseCard hoverable class="dark:bg-primary/5 border-primary/10 dark:border-primary/20">
                <h2 class="teacher-section-title mb-3 text-slate-900 dark:text-white">
                  <span class="material-symbols-outlined text-primary">info</span>
                  Thông tin bài thi
                </h2>
                <div class="space-y-4">
                  <div class="flex justify-between border-b border-primary/5 pb-2">
                    <p class="text-slate-500 dark:text-slate-400 text-sm">Tên bài thi</p>
                    <p class="text-slate-900 dark:text-white text-sm font-semibold">{{ examTitle }}</p>
                  </div>
                  <div class="flex justify-between border-b border-primary/5 pb-2">
                    <p class="text-slate-500 dark:text-slate-400 text-sm">Mã bài thi</p>
                    <p class="text-slate-900 dark:text-white text-sm font-semibold">{{ examCode }}</p>
                  </div>
                  <div class="flex justify-between border-b border-primary/5 pb-2">
                    <p class="text-slate-500 dark:text-slate-400 text-sm">Thời lượng</p>
                    <p class="text-slate-900 dark:text-white text-sm font-semibold">{{ examDuration }} phút</p>
                  </div>
                  <div class="flex justify-between">
                    <p class="text-slate-500 dark:text-slate-400 text-sm">Tổng số câu hỏi</p>
                    <p class="text-slate-900 dark:text-white text-sm font-semibold">{{ totalQuestions }} câu</p>
                  </div>
                </div>
              </BaseCard>

              <BaseCard hoverable class="dark:bg-primary/5 border-primary/10 dark:border-primary/20">
                <h2 class="teacher-section-title mb-3 text-slate-900 dark:text-white">
                  <span class="material-symbols-outlined text-primary">check_circle</span>
                  Kiểm tra hệ thống
                </h2>
                <div class="space-y-4">
                  <div class="flex items-center justify-between">
                    <div class="flex items-center gap-3">
                      <span :class="cameraReady ? 'text-green-600 dark:text-green-400' : 'text-rose-500'" class="material-symbols-outlined">videocam</span>
                      <span class="text-sm font-medium">Camera</span>
                    </div>
                    <span :class="cameraReady ? 'text-green-600 dark:text-green-400' : 'text-rose-500'" class="text-xs font-bold uppercase">
                      {{ cameraReady ? 'Sẵn sàng' : 'Chưa cấp quyền' }}
                    </span>
                  </div>
                  <div class="flex items-center justify-between">
                    <div class="flex items-center gap-3">
                      <span :class="micReady ? 'text-green-600 dark:text-green-400' : 'text-rose-500'" class="material-symbols-outlined">mic</span>
                      <span class="text-sm font-medium">Micro</span>
                    </div>
                    <span :class="micReady ? 'text-green-600 dark:text-green-400' : 'text-rose-500'" class="text-xs font-bold uppercase">
                      {{ micReady ? 'Sẵn sàng' : 'Chưa cấp quyền' }}
                    </span>
                  </div>
                  <p v-if="isCheckingDevices" class="text-xs text-slate-500">
                    Đang kiểm tra camera và micro...
                  </p>
                  <div class="flex items-center justify-between">
                    <div class="flex items-center gap-3">
                      <span class="material-symbols-outlined text-green-600 dark:text-green-400">wifi</span>
                      <span class="text-sm font-medium">Kết nối Internet</span>
                    </div>
                    <span class="text-xs font-bold text-green-600 dark:text-green-400 uppercase">Ổn định</span>
                  </div>
                </div>
              </BaseCard>
            </div>
            <div class="flex flex-col items-center gap-5 border-t border-primary/10 py-6 dark:border-primary/20 animate-fade-up-delay md:gap-6 md:py-7">
              <div class="text-center">
                <p class="text-slate-500 dark:text-slate-400 text-sm mb-2 uppercase tracking-widest font-bold">Thời gian đến lúc bắt đầu</p>
                <div class="text-5xl font-black text-primary">{{ countdownLabel }}</div>
                <p class="mt-2 text-xs text-slate-500 dark:text-slate-400">Bắt đầu lúc: {{ startAtDisplay }}</p>
              </div>
              <div class="w-full max-w-md space-y-4">
                <BaseButton
                  class="w-full !min-h-[3.25rem] text-lg"
                  size="lg"
                  :disabled="isCheckingDevices || !canStart"
                  :loading="isStarting"
                  @click="goToExamInterface"
                >
                  <template v-if="!isStarting">
                    <span class="material-symbols-outlined">play_arrow</span>
                    Bắt đầu làm bài
                  </template>
                </BaseButton>
                <p v-if="isEnded" class="text-center text-rose-600 text-sm italic">
                  Bài thi đã kết thúc.
                </p>
                <p v-if="!canStart && !isEnded" class="text-center text-slate-500 dark:text-slate-400 text-sm italic">
                  Bài thi chưa bắt đầu.
                </p>
              </div>
            </div>
          </div>
          </div>
        </main>

        <footer class="shrink-0 border-t border-slate-200/80 px-6 py-2 text-center text-xs text-slate-500 dark:border-slate-800 dark:text-slate-400 md:px-20 lg:px-40">
          Cần hỗ trợ?
          <button type="button" class="text-primary font-semibold hover:underline" @click="openSupport">Liên hệ hỗ trợ</button>
        </footer>
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
import StudentTopHeader from './StudentTopHeader.vue'
import BaseCard from '../shared/BaseCard.vue'
import BaseButton from '../shared/BaseButton.vue'
import PageHeader from '../shared/PageHeader.vue'

const route = useRoute()
const router = useRouter()
const { openSupport } = useNotifications()
const isDark = ref(false)
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

const examId = computed(() => Number.parseInt(String(route.query.examId || ''), 10) || null)
const examCode = computed(() => String(route.query.examCode || '-'))
const examTitle = computed(() => examDetail.value?.title || route.query.exam || 'Giữa kỳ Tâm lý học nâng cao')
const examDuration = computed(() => Number.parseInt(String(examDetail.value?.durationMinutes ?? route.query.duration ?? ''), 10) || 60)
const totalQuestions = computed(() => Number.parseInt(String(examDetail.value?.questionCount ?? route.query.questions ?? ''), 10) || 0)
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
const devicesReady = computed(() => (requireCameraMic.value ? (cameraReady.value && micReady.value) : true))
const canStart = computed(() => {
  if (isEnded.value) return false
  if (!devicesReady.value) return false
  return !startAtDate.value || nowMs.value >= startAtDate.value.getTime()
})
const startAtDisplay = computed(() => startAtDate.value ? startAtDate.value.toLocaleString() : '-')
const countdownLabel = computed(() => {
  if (isEnded.value) return 'EXPIRED'
  if (!startAtDate.value) return '00:00'
  const diffMs = startAtDate.value.getTime() - nowMs.value
  if (diffMs <= 0) return '00:00'
  const totalSeconds = Math.floor(diffMs / 1000)
  const minutes = Math.floor(totalSeconds / 60)
  const seconds = totalSeconds % 60
  return `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`
})

const lastSyncedLabel = computed(() => {
  if (!lastSyncedAt.value) return 'chưa có dữ liệu'
  return new Date(lastSyncedAt.value).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' })
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
  } catch (error) {
    toast.error('Không thể bắt đầu bài thi lúc này.')
  } finally {
    isStarting.value = false
  }
}

onMounted(async () => {
  await refreshExamDetail()
  await checkDevices()
  timerId = window.setInterval(() => {
    nowMs.value = Date.now()
  }, 1000)
  examRefreshTimerId = window.setInterval(() => {
    refreshExamDetail()
  }, 15000)
})

onUnmounted(() => {
  if (timerId) {
    window.clearInterval(timerId)
  }
  if (examRefreshTimerId) {
    window.clearInterval(examRefreshTimerId)
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
