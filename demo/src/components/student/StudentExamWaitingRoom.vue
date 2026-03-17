<template>
  <div :class="isDark ? 'dark' : 'light'" class="bg-background-light dark:bg-background-dark font-display text-slate-900 dark:text-slate-100 min-h-screen">
    <div class="relative flex h-auto min-h-screen w-full flex-col overflow-x-hidden">
      <div class="layout-container flex h-full grow flex-col">
        <StudentTopHeader />

        <main class="relative flex flex-1 justify-center py-8 px-4 overflow-hidden">
          <div class="pointer-events-none absolute -top-20 -left-16 size-72 rounded-full bg-primary/15 blur-3xl animate-float-slow"></div>
          <div class="pointer-events-none absolute -bottom-24 -right-12 size-80 rounded-full bg-primary/10 blur-3xl animate-float-delay"></div>

          <div class="layout-content-container relative flex flex-col max-w-[960px] flex-1 gap-8">
            <div class="flex flex-col gap-3 animate-fade-up">
              <div class="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-primary/10 text-primary w-fit text-sm font-semibold">
                <span class="relative flex h-2 w-2">
                  <span class="animate-ping absolute inline-flex h-full w-full rounded-full bg-primary opacity-75"></span>
                  <span class="relative inline-flex rounded-full h-2 w-2 bg-primary"></span>
                </span>
                Phòng chờ đang hoạt động
              </div>
              <h1 class="text-slate-900 dark:text-white text-4xl font-black leading-tight tracking-[-0.033em]">Đang chờ bài thi bắt đầu</h1>
              <p class="text-slate-600 dark:text-slate-400 text-lg font-normal leading-normal">Giám thị sẽ mở phiên thi trong giây lát. Vui lòng ở lại màn hình này và đảm bảo thiết bị đã sẵn sàng.</p>
              <div class="flex flex-wrap items-center gap-2 text-xs">
                <span class="inline-flex items-center gap-1.5 px-2 py-1 rounded-full border border-slate-200 dark:border-slate-700 text-slate-600 dark:text-slate-300 bg-white/80 dark:bg-slate-900/70">
                  <span :class="isSyncing ? 'bg-amber-500' : 'bg-emerald-500'" class="size-2 rounded-full"></span>
                  {{ isSyncing ? 'Đang đồng bộ...' : 'Đồng bộ ổn định' }}
                </span>
                <span class="text-slate-500 dark:text-slate-400">Cập nhật gần nhất: {{ lastSyncedLabel }}</span>
              </div>
            </div>

            <div class="grid grid-cols-1 md:grid-cols-2 gap-6 animate-fade-up-delay">
              <div class="bg-white dark:bg-primary/5 rounded-xl border border-primary/10 dark:border-primary/20 p-6 shadow-sm hover:-translate-y-0.5 hover:shadow-md transition-all duration-200">
                <h2 class="text-slate-900 dark:text-white text-xl font-bold leading-tight mb-4 flex items-center gap-2">
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
              </div>

              <div class="bg-white dark:bg-primary/5 rounded-xl border border-primary/10 dark:border-primary/20 p-6 shadow-sm hover:-translate-y-0.5 hover:shadow-md transition-all duration-200">
                <h2 class="text-slate-900 dark:text-white text-xl font-bold leading-tight mb-4 flex items-center gap-2">
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
              </div>
            </div>

            <div class="bg-white dark:bg-primary/5 rounded-xl border border-primary/10 dark:border-primary/20 p-6 shadow-sm hover:-translate-y-0.5 hover:shadow-md transition-all duration-200 animate-fade-up-delay">
              <h2 class="text-slate-900 dark:text-white text-xl font-bold leading-tight mb-4 flex items-center gap-2">
                <span class="material-symbols-outlined text-primary">gavel</span>
                Quy định phòng thi
              </h2>
              <ul class="grid grid-cols-1 md:grid-cols-2 gap-4">
                <li class="flex items-start gap-3">
                  <span class="material-symbols-outlined text-primary text-lg">error</span>
                  <p class="text-sm text-slate-600 dark:text-slate-300">Chuyển tab hoặc cửa sổ sẽ bị tính là vi phạm và có thể bị hủy kết quả.</p>
                </li>
                <li class="flex items-start gap-3">
                  <span class="material-symbols-outlined text-primary text-lg">error</span>
                  <p class="text-sm text-slate-600 dark:text-slate-300">Camera và micro phải luôn bật trong suốt thời gian làm bài.</p>
                </li>
                <li class="flex items-start gap-3">
                  <span class="material-symbols-outlined text-primary text-lg">error</span>
                  <p class="text-sm text-slate-600 dark:text-slate-300">Không được để tài liệu, sách hoặc ghi chú không cho phép trên bàn làm bài.</p>
                </li>
                <li class="flex items-start gap-3">
                  <span class="material-symbols-outlined text-primary text-lg">error</span>
                  <p class="text-sm text-slate-600 dark:text-slate-300">Hãy đảm bảo bạn ở một mình trong không gian yên tĩnh và đủ ánh sáng.</p>
                </li>
              </ul>
            </div>

            <div class="flex flex-col items-center gap-6 py-8 border-t border-primary/10 dark:border-primary/20 animate-fade-up-delay">
              <div class="text-center">
                <p class="text-slate-500 dark:text-slate-400 text-sm mb-2 uppercase tracking-widest font-bold">Thời gian đến lúc bắt đầu</p>
                <div class="text-5xl font-black text-primary">{{ countdownLabel }}</div>
                <p class="mt-2 text-xs text-slate-500 dark:text-slate-400">Bắt đầu lúc: {{ startAtDisplay }}</p>
              </div>
              <div class="w-full max-w-md space-y-4">
                <button :disabled="isStarting || isCheckingDevices || !canStart" @click="goToExamInterface" class="w-full py-4 px-6 rounded-xl bg-primary text-white font-bold text-lg flex items-center justify-center gap-2 hover:bg-primary/90 transition-colors disabled:opacity-70 disabled:cursor-not-allowed" type="button">
                  <span class="material-symbols-outlined">play_arrow</span>
                  {{ isStarting ? 'Đang bắt đầu...' : 'Bắt đầu làm bài' }}
                </button>
                <p v-if="isEnded" class="text-center text-rose-600 text-sm italic">
                  Bài thi đã kết thúc.
                </p>
                <p v-if="!canStart && !isEnded" class="text-center text-slate-500 dark:text-slate-400 text-sm italic">
                  Bài thi chưa bắt đầu.
                </p>
              </div>
            </div>
          </div>
        </main>

        <footer class="p-6 text-center text-slate-400 text-xs">
          © 2024 Bộ công cụ bảo mật ExamPortal. Bảo lưu mọi quyền.
          <br>
          Cần hỗ trợ?
          <button type="button" class="text-primary hover:underline">Liên hệ hỗ trợ</button>
        </footer>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { startAttempt } from '../../services/attemptService'
import { getExamDetail } from '../../services/examService'
import { useToast } from '../../composables/useToast'
import { useRoute, useRouter } from 'vue-router'
import StudentTopHeader from './StudentTopHeader.vue'

const route = useRoute()
const router = useRouter()
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
