<template>
  <div :class="isDark ? 'dark' : 'light'" class="portal-viewport flex h-full min-h-0 flex-col" style="background: var(--glass-bg)">
    <div class="relative flex h-full min-h-0 flex-1 w-full flex-col overflow-x-hidden">
      <div class="layout-container flex h-full min-h-0 flex-1 grow flex-col">
        <StudentTopHeader active-section="examJoin" class="shrink-0" />

        <main class="teacher-page-shell relative flex min-h-0 w-full max-w-screen-2xl flex-1 flex-col overflow-hidden">
          <div
            class="portal-scrollbar relative flex min-h-0 flex-1 flex-col overflow-y-auto overflow-x-hidden px-4 py-5 md:py-6"
            style="background: var(--glass-bg)"
          >
            <div class="pointer-events-none absolute left-0 top-24 -z-0 h-80 w-80 rounded-full opacity-20 blur-3xl" style="background: var(--glass-amber)" aria-hidden="true" />
            <div class="pointer-events-none absolute bottom-16 right-0 -z-0 h-72 w-72 rounded-full opacity-15 blur-3xl" style="background: var(--glass-violet)" aria-hidden="true" />

            <div class="relative z-10 flex w-full flex-1 flex-col gap-6 md:gap-8">
              <div class="flex flex-col gap-2 animate-fade-up md:gap-3">
                <nav class="gs-eyebrow mb-1 flex flex-wrap items-center gap-2">
                  <RouterLink to="/student/dashboard" class="hover:text-[--glass-amber] transition">Trang chủ</RouterLink>
                  <span class="material-symbols-outlined text-[14px]">chevron_right</span>
                  <RouterLink to="/student/exam-join" class="hover:text-[--glass-amber] transition">Thi qua mã</RouterLink>
                  <span class="material-symbols-outlined text-[14px]">chevron_right</span>
                  <span class="text-[--glass-amber]">Phòng chờ</span>
                </nav>
                <div class="gs-badge gs-badge--amber gs-pulse inline-flex w-fit items-center gap-2">
                  <span class="relative flex h-2 w-2">
                    <span class="animate-ping absolute inline-flex h-full w-full rounded-full bg-[--glass-amber] opacity-75"></span>
                    <span class="relative inline-flex h-2 w-2 rounded-full bg-[--glass-amber]"></span>
                  </span>
                  Phòng chờ đang hoạt động
                </div>
                <div class="flex flex-wrap items-center gap-2 text-xs">
                  <span class="text-xs font-bold px-2.5 py-1 rounded-full border" :style="isSyncing ? 'background: var(--glass-amber-soft); color: var(--glass-amber); border-color: var(--glass-amber-border)' : 'background: var(--glass-success-soft); color: var(--glass-success); border-color: var(--glass-success-border)'">
                    <span class="inline-block w-1.5 h-1.5 rounded-full mr-1.5" :style="isSyncing ? 'background: var(--glass-amber)' : 'background: var(--glass-success)'" />
                    {{ isSyncing ? 'Đang đồng bộ...' : 'Đồng bộ ổn định' }}
                  </span>
                  <span class="text-[--glass-text-muted]">Cập nhật: {{ lastSyncedLabel }}</span>
                </div>
              </div>

              <!-- Main waiting room grid -->
              <div class="mx-auto grid w-full max-w-4xl grid-cols-1 gap-6 animate-fade-up-delay lg:grid-cols-12 lg:gap-8">
                <!-- Main info card -->
                <div class="gs-card gs-scale rounded-xl p-6 md:p-8 lg:col-span-7">
                  <div class="mb-6 flex flex-col justify-between gap-4 sm:flex-row sm:items-start">
                    <div class="min-w-0">
                      <span class="gs-eyebrow mb-2 block">Phòng chờ trực tuyến</span>
                      <h1 class="text-2xl font-bold leading-tight md:text-3xl" style="font-family: 'Playfair Display', serif; color: var(--glass-text)">
                        {{ examTitle }}
                      </h1>
                    </div>
                    <div class="gs-card--flat glass shrink-0 rounded-lg px-4 py-2 text-center">
                      <span class="gs-eyebrow block !text-xs">Mã phòng</span>
                      <span class="gs-stat__value text-lg tracking-widest text-[--glass-amber]">{{ examCode }}</span>
                    </div>
                  </div>

                  <div class="glass-card flex items-center gap-5 rounded-xl p-5 mb-6" style="background: var(--glass-amber-soft)">
                    <div class="relative flex h-16 w-16 shrink-0 items-center justify-center">
                      <div class="absolute inset-0 rounded-full border-4 border-[--glass-amber]/15" />
                      <div
                        class="absolute inset-0 animate-spin rounded-full border-4 border-transparent border-t-[--glass-amber]"
                        style="animation-duration: 3s"
                      />
                      <span class="material-symbols-outlined relative text-2xl text-[--glass-amber]">hourglass_empty</span>
                    </div>
                    <div>
                      <h2 class="text-lg font-semibold" style="font-family: 'Playfair Display', serif; color: var(--glass-text)">Đang chờ giám thị</h2>
                      <p class="mt-1 text-sm text-[--glass-text-secondary]">
                        Giữ kết nối. Bài thi mở khi đủ điều kiện và giám thị cho phép.
                      </p>
                    </div>
                  </div>

                  <div class="space-y-3 border-b border-[--glass-border] pb-6 text-sm">
                    <div class="flex items-center justify-between py-2">
                      <span class="text-[--glass-text-secondary]">Thời lượng</span>
                      <span class="font-semibold text-[--glass-text]">{{ examDuration }} phút</span>
                    </div>
                    <div class="flex items-center justify-between py-2">
                      <span class="text-[--glass-text-secondary]">Số câu</span>
                      <span class="font-semibold text-[--glass-text]">{{ totalQuestions }} câu</span>
                    </div>
                  </div>

                  <div class="mt-6 flex flex-col items-center gap-5">
                    <div class="text-center">
                      <p class="gs-eyebrow mb-2">Thời gian đến lúc bắt đầu</p>
                      <div class="text-5xl font-black tabular-nums" style="font-family: 'Space Grotesk', monospace; color: var(--glass-amber)">{{ countdownLabel }}</div>
                      <p class="mt-2 text-xs text-[--glass-text-muted]">Bắt đầu lúc: {{ startAtDisplay }}</p>
                    </div>
                    <div class="w-full max-w-md space-y-4">
                      <button
                        type="button"
                        class="gs-btn gs-btn--primary gs-btn--shimmer gs-btn--lg inline-flex w-full items-center justify-center gap-2 rounded-xl shadow-lg"
                        :disabled="isCheckingDevices || !canStart || isStarting"
                        @click="goToExamInterface"
                      >
                        <span v-if="isStarting" class="material-symbols-outlined animate-spin text-xl">progress_activity</span>
                        <template v-else>
                          <span class="material-symbols-outlined">play_arrow</span>
                          Bắt đầu làm bài
                        </template>
                      </button>
                      <p v-if="isEnded" class="gs-badge gs-badge--danger text-center">Bài thi đã kết thúc.</p>
                      <p v-if="!canStart && !isEnded" class="text-center text-sm italic text-[--glass-text-muted]">Bài thi chưa bắt đầu.</p>
                    </div>
                  </div>
                </div>

                <!-- Sidebar cards -->
                <div class="flex flex-col gap-5 lg:col-span-5">
                  <!-- System check card -->
                  <BaseCard hoverable class="glass-card">
                    <h2 class="mb-3 flex items-center gap-2 text-base font-bold" style="font-family: 'Playfair Display', serif; color: var(--glass-text)">
                      <span class="material-symbols-outlined text-xl" style="color: var(--glass-amber)">check_circle</span>
                      Kiểm tra hệ thống
                    </h2>
                    <div class="space-y-4">
                      <div class="flex items-center justify-between">
                        <div class="flex items-center gap-3">
                          <span :style="cameraReady ? 'color: var(--glass-success)' : 'color: var(--glass-danger)'" class="material-symbols-outlined">videocam</span>
                          <span class="text-sm font-medium">Camera</span>
                        </div>
                        <span :style="cameraReady ? 'background: var(--glass-success-soft); color: var(--glass-success)' : 'background: var(--glass-danger-soft); color: var(--glass-danger)'" class="text-xs font-bold px-2.5 py-1 rounded-full border">
                          {{ cameraReady ? 'Sẵn sàng' : 'Chưa cấp quyền' }}
                        </span>
                      </div>
                      <div class="flex items-center justify-between">
                        <div class="flex items-center gap-3">
                          <span :style="micReady ? 'color: var(--glass-success)' : 'color: var(--glass-danger)'" class="material-symbols-outlined">mic</span>
                          <span class="text-sm font-medium">Micro</span>
                        </div>
                        <span :style="micReady ? 'background: var(--glass-success-soft); color: var(--glass-success)' : 'background: var(--glass-danger-soft); color: var(--glass-danger)'" class="text-xs font-bold px-2.5 py-1 rounded-full border">
                          {{ micReady ? 'Sẵn sàng' : 'Chưa cấp quyền' }}
                        </span>
                      </div>
                      <p v-if="isCheckingDevices" class="text-xs text-[--glass-text-muted]">Đang kiểm tra camera và micro...</p>
                      <div class="flex items-center justify-between">
                        <div class="flex items-center gap-3">
                          <span class="material-symbols-outlined" style="color: var(--glass-success)">wifi</span>
                          <span class="text-sm font-medium">Kết nối Internet</span>
                        </div>
                        <span class="text-xs font-bold px-2.5 py-1 rounded-full border" style="background: var(--glass-success-soft); color: var(--glass-success)">Ổn định</span>
                      </div>
                    </div>
                  </BaseCard>

                  <!-- Exam rules card -->
                  <div class="glass-card rounded-xl p-5" style="background: var(--glass-success-soft)">
                    <div class="gs-stat__icon mx-auto mb-3 flex size-16 items-center justify-center rounded-full" style="background: var(--glass-success)">
                      <span class="material-symbols-outlined text-3xl text-white">school</span>
                    </div>
                    <h3 class="text-base font-bold mb-1 text-center" style="font-family: 'Playfair Display', serif; color: var(--glass-text)">Quy chế phòng thi</h3>
                    <p class="text-xs text-center" style="color: var(--glass-text-muted)">Chuẩn phòng thi an toàn</p>
                  </div>

                  <!-- Rules list -->
                  <div class="glass-card space-y-5 rounded-xl p-5">
                    <div class="flex gap-3">
                      <div class="mt-0.5 flex size-8 shrink-0 items-center justify-center rounded-lg" style="background: var(--glass-amber-soft)">
                        <span class="material-symbols-outlined text-lg" style="color: var(--glass-amber)">videocam</span>
                      </div>
                      <div>
                        <p class="font-semibold" style="color: var(--glass-text)">Giữ camera bật</p>
                        <p class="mt-0.5 text-xs leading-relaxed text-[--glass-text-secondary]">
                          Khuôn mặt rõ trong khung hình suốt phiên thi.
                        </p>
                      </div>
                    </div>
                    <div class="flex gap-3">
                      <div class="mt-0.5 flex size-8 shrink-0 items-center justify-center rounded-lg" style="background: var(--glass-amber-soft)">
                        <span class="material-symbols-outlined text-lg" style="color: var(--glass-amber)">tab_unselected</span>
                      </div>
                      <div>
                        <p class="font-semibold" style="color: var(--glass-text)">Không chuyển tab</p>
                        <p class="mt-0.5 text-xs leading-relaxed text-[--glass-text-secondary]">
                          Hệ thống ghi nhận khi rời cửa sổ làm bài.
                        </p>
                      </div>
                    </div>
                    <div class="flex gap-3">
                      <div class="mt-0.5 flex size-8 shrink-0 items-center justify-center rounded-lg" style="background: var(--glass-amber-soft)">
                        <span class="material-symbols-outlined text-lg" style="color: var(--glass-amber)">volume_off</span>
                      </div>
                      <div>
                        <p class="font-semibold" style="color: var(--glass-text)">Giữ yên lặng</p>
                        <p class="mt-0.5 text-xs leading-relaxed text-[--glass-text-secondary]">
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

        <footer class="shrink-0 border-t px-6 py-2 text-center text-xs md:px-20 lg:px-40" style="background: var(--glass-surface); border-color: var(--glass-border); color: var(--glass-text-muted)">
          Cần hỗ trợ?
          <button type="button" class="font-semibold transition-colors" style="color: var(--glass-amber)" @click="openSupport">Liên hệ hỗ trợ</button>
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
