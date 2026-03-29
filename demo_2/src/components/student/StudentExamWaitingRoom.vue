<template>
  <div :class="isDark ? 'dark' : 'light'" class="portal-viewport flex h-full min-h-0 flex-col bg-background-light font-display text-slate-900 dark:bg-background-dark dark:text-slate-100">
    <div class="relative flex h-full min-h-0 flex-1 w-full flex-col overflow-x-hidden">
      <div class="layout-container flex h-full min-h-0 flex-1 grow flex-col">
        <StudentTopHeader active-section="examJoin" class="shrink-0" />

        <main class="teacher-page-shell student-stitch-main relative flex min-h-0 w-full max-w-screen-2xl flex-1 flex-col overflow-hidden">
          <div
            class="portal-scrollbar student-stitch-paper-bg relative flex min-h-0 flex-1 flex-col overflow-y-auto overflow-x-hidden px-4 py-5 md:py-6"
          >
            <div class="pointer-events-none absolute left-0 top-24 -z-0 h-80 w-80 rounded-full bg-primary/10 blur-3xl dark:bg-primary/5" aria-hidden="true" />
            <div class="pointer-events-none absolute bottom-16 right-0 -z-0 h-72 w-72 rounded-full bg-amber-200/35 blur-3xl dark:bg-amber-900/25" aria-hidden="true" />

            <div class="relative z-10 flex w-full flex-1 flex-col gap-6 md:gap-8">
              <div class="flex flex-col gap-2 animate-fade-up md:gap-3">
                <nav class="mb-1 flex flex-wrap items-center gap-2 text-xs font-medium text-slate-500">
                  <RouterLink to="/student/dashboard" class="transition hover:text-primary">Trang chủ</RouterLink>
                  <span class="material-symbols-outlined text-[14px] text-slate-400">chevron_right</span>
                  <RouterLink to="/student/exam-join" class="transition hover:text-primary">Thi qua mã</RouterLink>
                  <span class="material-symbols-outlined text-[14px] text-slate-400">chevron_right</span>
                  <span class="font-semibold text-primary">Phòng chờ</span>
                </nav>
                <div
                  class="inline-flex w-fit items-center gap-2 rounded-full bg-primary-100/90 px-3 py-1 text-sm font-semibold text-primary-800 dark:bg-primary-900/40 dark:text-primary-200"
                >
                  <span class="relative flex h-2 w-2">
                    <span class="animate-ping absolute inline-flex h-full w-full rounded-full bg-primary opacity-75"></span>
                    <span class="relative inline-flex h-2 w-2 rounded-full bg-primary"></span>
                  </span>
                  Phòng chờ đang hoạt động
                </div>
                <div class="flex flex-wrap items-center gap-2 text-xs">
                  <span class="inline-flex items-center gap-1.5 rounded-full border border-slate-200 bg-white/80 px-2 py-1 text-slate-600 dark:border-slate-700 dark:bg-slate-900/70 dark:text-slate-300">
                    <span :class="isSyncing ? 'bg-amber-500' : 'bg-emerald-500'" class="size-2 rounded-full"></span>
                    {{ isSyncing ? 'Đang đồng bộ...' : 'Đồng bộ ổn định' }}
                  </span>
                  <span class="text-slate-500 dark:text-slate-400">Cập nhật: {{ lastSyncedLabel }}</span>
                </div>
              </div>

              <!-- stitch_new/online_waiting_room — lưới 7+5 -->
              <div class="mx-auto grid w-full max-w-4xl grid-cols-1 gap-6 animate-fade-up-delay lg:grid-cols-12 lg:gap-8">
                <div
                  class="rounded-xl border border-[#dbc2b0]/10 bg-white p-6 shadow-sm stitch-editorial-shadow dark:border-slate-600/50 dark:bg-slate-900/40 md:p-8 lg:col-span-7"
                >
                  <div class="mb-6 flex flex-col justify-between gap-4 sm:flex-row sm:items-start">
                    <div class="min-w-0">
                      <span class="mb-2 block text-xs font-bold uppercase tracking-[0.12em] text-primary">Phòng chờ trực tuyến</span>
                      <h1 class="stitch-font-headline text-2xl font-bold leading-tight text-[#1b1c1a] dark:text-amber-100 md:text-3xl">
                        {{ examTitle }}
                      </h1>
                    </div>
                    <div class="shrink-0 rounded-lg bg-[#e9e8e4] px-4 py-2 text-center dark:bg-slate-800">
                      <span class="block text-[10px] font-bold uppercase tracking-wider text-[#554336] dark:text-slate-400">Mã phòng</span>
                      <span class="stitch-font-headline text-lg font-bold tracking-widest text-primary">{{ examCode }}</span>
                    </div>
                  </div>

                  <div class="mb-6 flex items-center gap-5 rounded-xl bg-[#f4f4f0] p-5 dark:bg-slate-800/60">
                    <div class="relative flex h-16 w-16 shrink-0 items-center justify-center">
                      <div class="absolute inset-0 rounded-full border-4 border-primary/15" />
                      <div
                        class="absolute inset-0 animate-spin rounded-full border-4 border-transparent border-t-primary"
                        style="animation-duration: 3s"
                      />
                      <span class="material-symbols-outlined relative text-2xl text-primary">hourglass_empty</span>
                    </div>
                    <div>
                      <h2 class="stitch-font-headline text-lg font-semibold text-[#1b1c1a] dark:text-slate-100">Đang chờ giám thị</h2>
                      <p class="mt-1 text-sm text-[var(--stitch-on-surface-variant)] dark:text-slate-400">
                        Giữ kết nối. Bài thi mở khi đủ điều kiện và giám thị cho phép.
                      </p>
                    </div>
                  </div>

                  <div class="space-y-3 border-b border-[#dbc2b0]/15 pb-6 text-sm dark:border-slate-600/50">
                    <div class="flex items-center justify-between py-2">
                      <span class="text-[var(--stitch-on-surface-variant)] dark:text-slate-400">Thời lượng</span>
                      <span class="font-semibold text-[#1b1c1a] dark:text-slate-100">{{ examDuration }} phút</span>
                    </div>
                    <div class="flex items-center justify-between py-2">
                      <span class="text-[var(--stitch-on-surface-variant)] dark:text-slate-400">Số câu</span>
                      <span class="font-semibold text-[#1b1c1a] dark:text-slate-100">{{ totalQuestions }} câu</span>
                    </div>
                  </div>

                  <div class="mt-6 flex flex-col items-center gap-5">
                    <div class="text-center">
                      <p class="mb-2 text-xs font-bold uppercase tracking-widest text-slate-500 dark:text-slate-400">Thời gian đến lúc bắt đầu</p>
                      <div class="text-5xl font-black tabular-nums text-primary">{{ countdownLabel }}</div>
                      <p class="mt-2 text-xs text-slate-500 dark:text-slate-400">Bắt đầu lúc: {{ startAtDisplay }}</p>
                    </div>
                    <div class="w-full max-w-md space-y-4">
                      <button
                        type="button"
                        class="silk-press-gradient inline-flex w-full min-h-[3.25rem] items-center justify-center gap-2 rounded-xl px-4 text-lg font-bold text-white shadow-lg shadow-primary/20 transition hover:opacity-95 disabled:cursor-not-allowed disabled:opacity-60"
                        :disabled="isCheckingDevices || !canStart || isStarting"
                        @click="goToExamInterface"
                      >
                        <span v-if="isStarting" class="material-symbols-outlined animate-spin text-xl">progress_activity</span>
                        <template v-else>
                          <span class="material-symbols-outlined">play_arrow</span>
                          Bắt đầu làm bài
                        </template>
                      </button>
                      <p v-if="isEnded" class="text-center text-sm italic text-rose-600">Bài thi đã kết thúc.</p>
                      <p v-if="!canStart && !isEnded" class="text-center text-sm italic text-slate-500 dark:text-slate-400">Bài thi chưa bắt đầu.</p>
                    </div>
                  </div>
                </div>

                <div class="flex flex-col gap-5 lg:col-span-5">
                  <BaseCard hoverable class="stitch-editorial-shadow border border-[#dbc2b0]/25 dark:border-slate-600/50 dark:bg-primary/5">
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
                      <p v-if="isCheckingDevices" class="text-xs text-slate-500">Đang kiểm tra camera và micro...</p>
                      <div class="flex items-center justify-between">
                        <div class="flex items-center gap-3">
                          <span class="material-symbols-outlined text-green-600 dark:text-green-400">wifi</span>
                          <span class="text-sm font-medium">Kết nối Internet</span>
                        </div>
                        <span class="text-xs font-bold uppercase text-green-600 dark:text-green-400">Ổn định</span>
                      </div>
                    </div>
                  </BaseCard>

                  <div class="rounded-xl border border-primary/15 bg-primary/5 p-6 text-center dark:bg-primary/10">
                    <div class="mx-auto mb-3 flex size-16 items-center justify-center rounded-full bg-primary text-white shadow-inner">
                      <span class="material-symbols-outlined text-3xl">school</span>
                    </div>
                    <h3 class="stitch-font-headline text-base font-bold text-amber-900 dark:text-amber-100">Quy chế phòng thi</h3>
                    <p class="mt-1 text-[10px] font-semibold uppercase tracking-widest text-slate-500">Chuẩn phòng thi an toàn</p>
                  </div>

                  <div class="space-y-5 rounded-xl bg-[#f4f4f0] p-6 text-sm dark:bg-slate-800/50">
                    <div class="flex gap-3">
                      <div class="mt-0.5 flex size-8 shrink-0 items-center justify-center rounded-lg bg-[#e3e2df] dark:bg-slate-700">
                        <span class="material-symbols-outlined text-lg text-primary">videocam</span>
                      </div>
                      <div>
                        <p class="font-semibold text-[#1b1c1a] dark:text-slate-100">Giữ camera bật</p>
                        <p class="mt-0.5 text-xs leading-relaxed text-[var(--stitch-on-surface-variant)] dark:text-slate-400">
                          Khuôn mặt rõ trong khung hình suốt phiên thi.
                        </p>
                      </div>
                    </div>
                    <div class="flex gap-3">
                      <div class="mt-0.5 flex size-8 shrink-0 items-center justify-center rounded-lg bg-[#e3e2df] dark:bg-slate-700">
                        <span class="material-symbols-outlined text-lg text-primary">tab_unselected</span>
                      </div>
                      <div>
                        <p class="font-semibold text-[#1b1c1a] dark:text-slate-100">Không chuyển tab</p>
                        <p class="mt-0.5 text-xs leading-relaxed text-[var(--stitch-on-surface-variant)] dark:text-slate-400">
                          Hệ thống ghi nhận khi rời cửa sổ làm bài.
                        </p>
                      </div>
                    </div>
                    <div class="flex gap-3">
                      <div class="mt-0.5 flex size-8 shrink-0 items-center justify-center rounded-lg bg-[#e3e2df] dark:bg-slate-700">
                        <span class="material-symbols-outlined text-lg text-primary">volume_off</span>
                      </div>
                      <div>
                        <p class="font-semibold text-[#1b1c1a] dark:text-slate-100">Giữ yên lặng</p>
                        <p class="mt-0.5 text-xs leading-relaxed text-[var(--stitch-on-surface-variant)] dark:text-slate-400">
                          Micro có thể được dùng để giám sát môi trường.
                        </p>
                      </div>
                    </div>
                  </div>
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
import { RouterLink, useRoute, useRouter } from 'vue-router'
import StudentTopHeader from './StudentTopHeader.vue'
import BaseCard from '../shared/BaseCard.vue'

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
