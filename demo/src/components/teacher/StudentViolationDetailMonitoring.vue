<template>
  <div :class="isDark ? 'dark' : 'light'" class="bg-background-light dark:bg-background-dark text-slate-900 dark:text-slate-100 font-display min-h-screen">
    <div class="relative flex h-auto min-h-screen w-full flex-col overflow-x-hidden">
      <div class="layout-container flex h-full grow flex-col">
        <TeacherTopHeader active-section="monitoring" />

        <main class="relative flex flex-col flex-1 px-4 lg:px-20 py-8 max-w-[1400px] mx-auto w-full overflow-hidden">
          <div class="pointer-events-none absolute -top-16 -left-16 size-72 rounded-full bg-primary/15 blur-3xl animate-float-slow"></div>
          <div class="pointer-events-none absolute -bottom-24 -right-12 size-80 rounded-full bg-primary/10 blur-3xl animate-float-delay"></div>

          <div class="relative flex flex-wrap items-center gap-2 mb-6 animate-fade-up">
            <span class="text-slate-500 text-sm">Đề thi</span>
            <span class="material-symbols-outlined text-sm text-slate-400">chevron_right</span>
            <span class="text-slate-500 text-sm">{{ examName }}</span>
            <span class="material-symbols-outlined text-sm text-slate-400">chevron_right</span>
            <span class="text-slate-900 dark:text-slate-100 text-sm font-bold">Giám sát: {{ studentName }}</span>
          </div>

          <div class="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-xl p-6 mb-8 flex flex-col md:flex-row justify-between items-start md:items-center gap-6 shadow-sm animate-fade-up-delay hover:-translate-y-0.5 hover:shadow-md transition-all duration-200">
            <div class="flex gap-5 items-center">
              <div class="relative">
                <div class="bg-center bg-no-repeat aspect-square bg-cover rounded-full h-20 w-20 border-2 border-red-500" :style="`background-image: url('${studentAvatar}')`"></div>
                <div class="absolute bottom-0 right-0 bg-red-600 h-6 w-6 rounded-full border-2 border-white dark:border-slate-900 flex items-center justify-center">
                  <span class="material-symbols-outlined text-[14px] text-white font-bold">warning</span>
                </div>
              </div>
              <div>
                <div class="flex items-center gap-3 mb-1">
                  <h1 class="text-slate-900 dark:text-slate-100 text-2xl font-bold">{{ studentName }}</h1>
                  <span :class="riskBadgeClass" class="px-3 py-1 text-xs font-bold uppercase rounded-full tracking-wider border">
                    {{ riskBadgeText }}
                  </span>
                </div>
                <p class="text-slate-500 dark:text-slate-400 text-sm flex items-center gap-4">
                  <span>ID: <span class="font-medium text-slate-700 dark:text-slate-300">{{ studentId }}</span></span>
                  <span class="h-4 w-px bg-slate-300 dark:bg-slate-700"></span>
                  <span>Đề thi: <span class="font-medium text-slate-700 dark:text-slate-300">{{ examName }}</span></span>
                </p>
                <div class="mt-2 flex flex-wrap items-center gap-2 text-xs">
                  <span class="inline-flex items-center gap-1.5 px-2 py-1 rounded-full border border-slate-200 dark:border-slate-700 text-slate-600 dark:text-slate-300 bg-white/80 dark:bg-slate-900/70">
                    <span :class="isSyncing ? 'bg-amber-500' : 'bg-emerald-500'" class="size-2 rounded-full"></span>
                    {{ isSyncing ? 'Đang đồng bộ...' : 'Đồng bộ ổn định' }}
                  </span>
                  <span class="text-slate-500 dark:text-slate-400">Cập nhật gần nhất: {{ lastUpdatedLabel }}</span>
                </div>
                <p v-if="loadError" class="mt-2 text-xs text-rose-600">{{ loadError }}</p>
              </div>
            </div>
            <div class="flex gap-3 w-full md:w-auto">
              <button class="flex-1 md:flex-none flex items-center justify-center gap-2 px-6 py-2.5 rounded-lg border border-slate-200 dark:border-slate-700 text-slate-700 dark:text-slate-300 font-bold text-sm hover:bg-slate-50 dark:hover:bg-slate-800 hover:-translate-y-0.5 hover:shadow-md transition-all duration-200" type="button">
                <span class="material-symbols-outlined text-lg">history</span> Xem nhật ký
              </button>
              <button class="flex-1 md:flex-none flex items-center justify-center gap-2 px-6 py-2.5 rounded-lg bg-primary text-white font-bold text-sm hover:opacity-90 transition-all" type="button">
                <span class="material-symbols-outlined text-lg">chat</span> Trò chuyện trực tiếp
              </button>
            </div>
          </div>

          <div class="relative grid grid-cols-1 lg:grid-cols-3 gap-8 animate-fade-up-delay">
            <div class="lg:col-span-2 flex flex-col gap-6">
              <div class="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-xl overflow-hidden shadow-sm">
                <div class="p-4 border-b border-slate-200 dark:border-slate-800 flex justify-between items-center bg-slate-50 dark:bg-slate-800/50">
                  <div class="flex items-center gap-2">
                    <span class="flex h-2 w-2 rounded-full bg-red-500"></span>
                    <h3 class="font-bold text-slate-900 dark:text-slate-100">Luồng trực tiếp - Màn hình hiện tại</h3>
                  </div>
                  <div class="flex gap-2">
                    <span class="text-xs bg-slate-200 dark:bg-slate-700 px-2 py-1 rounded text-slate-600 dark:text-slate-400 font-medium uppercase">1080p</span>
                    <span class="text-xs bg-slate-200 dark:bg-slate-700 px-2 py-1 rounded text-slate-600 dark:text-slate-400 font-medium uppercase">Mã hóa</span>
                  </div>
                </div>
                <div class="relative aspect-video bg-slate-900 flex items-center justify-center group">
                  <div class="absolute inset-0 bg-cover bg-center opacity-80" style="background-image: url('https://lh3.googleusercontent.com/aida-public/AB6AXuCvhXV2sdkMp4W2ffIoqzyfpojUOUjoYWCgLENEw6jGPAEyakBzOLVV326qs53ZLP9cecR4r1jR90UfB64FpbFkIXb-iLSxtTbHjmV8fJbeX7qntdiGj4QGEKm4VbEwPs9EaZYKeUzJdJ4KTAJR25evdSsQCkDXXqJYAJhWrFonOM7_GSC6sG7m9Yp-AYxNkZ8HB5j_jJ7pOEIcyZC2HDBaSL2-_wxckc48ydhrSO1P037tcfqD_4uG8yGDSAoznGeLWY70D4viaw');"></div>
                  <div class="absolute inset-0 flex flex-col items-center justify-center bg-black/40">
                    <div class="bg-red-600 text-white px-4 py-2 rounded-lg font-bold flex items-center gap-2 shadow-xl mb-4">
                      <span class="material-symbols-outlined">visibility</span> Phát hiện cửa sổ ngoài
                    </div>
                  </div>
                </div>
              </div>

              <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
                <div class="bg-white dark:bg-slate-900 p-5 rounded-xl border border-slate-200 dark:border-slate-800 shadow-sm">
                  <p class="text-slate-500 dark:text-slate-400 text-xs font-bold uppercase tracking-wider mb-2">Tiến độ làm bài</p>
                  <div class="flex items-end justify-between">
                    <p class="text-2xl font-bold text-slate-900 dark:text-slate-100 tracking-tight">{{ answeredCount }} <span class="text-slate-400 font-normal">/ {{ totalQuestions }}</span></p>
                    <span class="text-emerald-600 bg-emerald-50 dark:bg-emerald-900/20 text-xs font-bold px-2 py-1 rounded">Hoàn thành {{ progressPercent }}%</span>
                  </div>
                  <div class="w-full bg-slate-100 dark:bg-slate-800 h-1.5 rounded-full mt-4">
                    <div class="bg-primary h-1.5 rounded-full" :style="{ width: `${progressPercent}%` }"></div>
                  </div>
                </div>
                <div class="bg-white dark:bg-slate-900 p-5 rounded-xl border border-slate-200 dark:border-slate-800 shadow-sm">
                  <p class="text-slate-500 dark:text-slate-400 text-xs font-bold uppercase tracking-wider mb-2">Sự kiện bị gắn cờ</p>
                  <div class="flex items-end justify-between">
                    <p class="text-2xl font-bold text-red-600 tracking-tight">{{ flaggedEvents.length }} <span class="text-slate-400 font-normal text-lg">Tổng</span></p>
                    <span class="text-red-600 bg-red-50 dark:bg-red-900/20 text-xs font-bold px-2 py-1 rounded">Risk {{ riskScore }}</span>
                  </div>
                </div>
                <div class="bg-white dark:bg-slate-900 p-5 rounded-xl border border-slate-200 dark:border-slate-800 shadow-sm">
                  <p class="text-slate-500 dark:text-slate-400 text-xs font-bold uppercase tracking-wider mb-2">Thời gian còn lại</p>
                  <div class="flex items-end justify-between">
                    <p class="text-2xl font-bold text-slate-900 dark:text-slate-100 tracking-tight">{{ remainingTimeLabel }}</p>
                    <span class="text-slate-500 bg-slate-100 dark:bg-slate-800 text-xs font-bold px-2 py-1 rounded">{{ statusLabel }}</span>
                  </div>
                </div>
              </div>
            </div>

            <div class="flex flex-col gap-6">
              <div class="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-xl overflow-hidden shadow-sm flex-1">
                <div class="p-4 border-b border-slate-200 dark:border-slate-800 flex justify-between items-center">
                  <h3 class="font-bold text-slate-900 dark:text-slate-100 flex items-center gap-2">
                    <span class="material-symbols-outlined text-red-500">assignment_late</span>
                    Nhật ký vi phạm
                  </h3>
                  <button class="text-xs text-primary font-bold hover:underline" type="button">Xóa tất cả</button>
                </div>
                <div class="divide-y divide-slate-100 dark:divide-slate-800">
                  <div v-if="timelineItems.length === 0" class="p-4 text-sm text-slate-500 dark:text-slate-400">
                    Chưa có sự kiện vi phạm nào cho lần làm bài này.
                  </div>
                  <div
                    v-for="item in timelineItems"
                    :key="item.key"
                    :class="item.highlightClass"
                    class="p-4 flex gap-4"
                  >
                    <div class="shrink-0" :class="item.iconClass"><span class="material-symbols-outlined">{{ item.icon }}</span></div>
                    <div class="flex-1">
                      <div class="flex justify-between items-start mb-1">
                        <p class="text-sm font-bold text-slate-900 dark:text-slate-100">{{ item.title }}</p>
                        <span class="text-[10px] text-slate-500 font-medium">{{ item.timeLabel }}</span>
                      </div>
                      <p class="text-xs text-slate-600 dark:text-slate-400 leading-relaxed">{{ item.description }}</p>
                    </div>
                  </div>
                </div>
              </div>

              <div class="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-xl p-6 shadow-sm">
                <h3 class="font-bold text-slate-900 dark:text-slate-100 mb-6 flex items-center gap-2">
                  <span class="material-symbols-outlined text-primary">settings_applications</span>
                  Điều khiển giám thị
                </h3>
                <div class="flex flex-col gap-3">
                  <button :disabled="isWarningSending || isInvalidating" class="w-full flex items-center justify-center gap-3 py-3 px-4 bg-amber-500 hover:bg-amber-600 text-white font-bold rounded-lg transition-colors disabled:opacity-60" type="button" @click="handleSendWarning">
                    <span class="material-symbols-outlined">warning</span> {{ isWarningSending ? 'Đang gửi cảnh báo...' : 'Gửi cảnh báo' }}
                  </button>
                  <button :disabled="isWarningSending || isInvalidating" class="w-full flex items-center justify-center gap-3 py-3 px-4 bg-red-600 hover:bg-red-700 text-white font-bold rounded-lg transition-colors disabled:opacity-60" type="button" @click="handleInvalidateAttempt">
                    <span class="material-symbols-outlined">block</span> {{ isInvalidating ? 'Đang đình chỉ...' : 'Hủy hiệu lực bài thi' }}
                  </button>
                </div>
                <p v-if="actionMessage" class="mt-3 text-xs text-emerald-600">{{ actionMessage }}</p>
                <p v-if="actionError" class="mt-2 text-xs text-rose-600">{{ actionError }}</p>
              </div>
            </div>
          </div>
        </main>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { ApiError } from '../../services/apiClient'
import { getAttemptDetail, getAttemptReport } from '../../services/attemptService'
import { invalidateAttempt, listMonitoringTimeline, sendTeacherWarning } from '../../services/monitoringService'
import { useRoute } from 'vue-router'
import TeacherTopHeader from './TeacherTopHeader.vue'

const route = useRoute()
const isDark = ref(false)
const loadError = ref('')
const isSyncing = ref(false)
const attemptDetail = ref(null)
const attemptReport = ref(null)
const timeline = ref([])
const lastUpdatedAt = ref(null)
const actionMessage = ref('')
const actionError = ref('')
const isWarningSending = ref(false)
const isInvalidating = ref(false)
let refreshTimer = null

const attemptId = computed(() => Number.parseInt(String(route.query.attemptId || ''), 10) || null)
const studentName = computed(() => attemptDetail.value?.student || route.query.student || 'Sinh viên không rõ')
const studentId = computed(() => route.query.studentId || (attemptId.value ? `AT-${attemptId.value}` : 'AT-?'))
const examName = computed(() => attemptDetail.value?.examTitle || route.query.exam || 'Đề thi đã chọn')
const studentAvatar = computed(
  () =>
    route.query.avatar ||
    'https://lh3.googleusercontent.com/aida-public/AB6AXuCS2RhYd0VS4fVVYoA2GgJuZzt6TTNNleFcdImbGSB3IZsBGlQY3W3_8DnpvWiAQ08yeiCuKP9ul1X8fZKIsAa2kb6Fz6jRM8rrBUK30-_8x487epcjiAGOetJYM1jwVrdUReYJacR8oMZG3jkyeYdbaMIJksyOphO4c7DzJeO8jaczGPgMZ5nrqWAw003cc3HOtSP0jxqftO4wuJCLWj7meS0yE7HT-vnt3XHDdKYANhCOuN2bCqpr_GQhbNfL4asVsRK5JTBzwQ'
)

const answeredCount = computed(() => Number(attemptDetail.value?.answeredCount ?? attemptReport.value?.answeredCount ?? 0))
const totalQuestions = computed(() => Number(attemptDetail.value?.totalQuestions || 0))
const progressPercent = computed(() => {
  if (!totalQuestions.value) return 0
  return Math.max(0, Math.min(100, Math.round((answeredCount.value / totalQuestions.value) * 100)))
})
const riskScore = computed(() => Number(attemptDetail.value?.riskScore ?? attemptReport.value?.riskScore ?? 0))
const suspicious = computed(() => Boolean(attemptDetail.value?.suspicious ?? attemptReport.value?.suspicious))
const statusLabel = computed(() => String(attemptDetail.value?.status || attemptReport.value?.status || 'IN_PROGRESS').toUpperCase())

const riskBadgeText = computed(() => (suspicious.value ? 'Hoạt động đáng ngờ' : 'Đang theo dõi'))
const riskBadgeClass = computed(() => (suspicious.value
  ? 'bg-red-100 dark:bg-red-900/30 text-red-700 dark:text-red-400 animate-pulse border-red-200 dark:border-red-800'
  : 'bg-emerald-100 dark:bg-emerald-900/30 text-emerald-700 dark:text-emerald-400 border-emerald-200 dark:border-emerald-800'))

const formatDuration = (totalSeconds) => {
  const safe = Math.max(0, Number(totalSeconds || 0))
  const hours = Math.floor(safe / 3600)
  const minutes = Math.floor((safe % 3600) / 60)
  const seconds = safe % 60
  if (hours > 0) {
    return `${String(hours).padStart(2, '0')}:${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`
  }
  return `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`
}

const remainingTimeLabel = computed(() => formatDuration(attemptDetail.value?.remainingSeconds ?? attemptReport.value?.remainingSeconds ?? 0))

const formatEventType = (eventType) => {
  const normalized = String(eventType || '').toUpperCase()
  switch (normalized) {
    case 'TAB_SWITCH':
      return 'Phát hiện chuyển tab'
    case 'BLUR':
      return 'Mất tiêu điểm cửa sổ'
    case 'EXIT_FULLSCREEN':
      return 'Thoát chế độ toàn màn hình'
    case 'FAST_SUBMIT':
      return 'Nộp bài quá nhanh'
    case 'DUPLICATE_IP':
      return 'Trùng địa chỉ IP'
    default:
      return normalized || 'Sự kiện giám sát'
  }
}

const formatTimelineTime = (raw) => {
  if (!raw) return '-'
  const date = new Date(raw)
  if (Number.isNaN(date.getTime())) return '-'
  return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
}

const timelineItems = computed(() => timeline.value.map((entry, index) => {
  const isEvent = entry.type === 'MONITORING_EVENT'
  const isSnapshot = entry.type === 'RISK_SNAPSHOT'
  return {
    key: `${entry.type || 'entry'}-${entry.at || index}-${index}`,
    icon: isSnapshot ? 'monitor_heart' : 'warning',
    iconClass: isSnapshot ? 'text-amber-500' : 'text-red-600',
    highlightClass: isEvent ? 'bg-red-50 dark:bg-red-900/10' : '',
    title: isEvent ? formatEventType(entry.eventType) : `Cập nhật risk score: ${entry.riskScore ?? 0}`,
    description: isEvent
      ? (entry.details || 'Không có mô tả chi tiết.')
      : `Suspicious: ${entry.suspicious ? 'Có' : 'Không'}`,
    timeLabel: formatTimelineTime(entry.at)
  }
}))

const flaggedEvents = computed(() => timeline.value.filter((entry) => entry.type === 'MONITORING_EVENT'))

const lastUpdatedLabel = computed(() => {
  if (!lastUpdatedAt.value) return 'chưa có dữ liệu'
  return new Date(lastUpdatedAt.value).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' })
})

const loadMonitoringDetail = async () => {
  if (!attemptId.value) {
    loadError.value = 'Thiếu attemptId. Vui lòng mở chi tiết từ trang giám sát trực tiếp.'
    return
  }

  isSyncing.value = true
  try {
    const [detail, report, timelineData] = await Promise.all([
      getAttemptDetail(attemptId.value),
      getAttemptReport(attemptId.value),
      listMonitoringTimeline(attemptId.value)
    ])
    attemptDetail.value = detail
    attemptReport.value = report
    timeline.value = Array.isArray(timelineData) ? timelineData : []
    loadError.value = ''
    lastUpdatedAt.value = Date.now()
  } catch (error) {
    loadError.value = error instanceof ApiError ? error.message : 'Không thể tải dữ liệu giám sát chi tiết.'
  } finally {
    isSyncing.value = false
  }
}

const handleSendWarning = async () => {
  if (!attemptId.value) return

  actionError.value = ''
  actionMessage.value = ''
  isWarningSending.value = true
  try {
    const res = await sendTeacherWarning(attemptId.value)
    actionMessage.value = res?.message || 'Đã gửi cảnh báo realtime đến thí sinh.'
  } catch (error) {
    actionError.value = error instanceof ApiError ? error.message : 'Không thể gửi cảnh báo lúc này.'
  } finally {
    isWarningSending.value = false
  }
}

const handleInvalidateAttempt = async () => {
  if (!attemptId.value) return

  const confirmed = window.confirm('Bạn chắc chắn muốn hủy hiệu lực bài thi này?')
  if (!confirmed) return

  actionError.value = ''
  actionMessage.value = ''
  isInvalidating.value = true
  try {
    const res = await invalidateAttempt(attemptId.value)
    actionMessage.value = res?.message || 'Đã đình chỉ bài thi thành công.'
    await loadMonitoringDetail()
  } catch (error) {
    actionError.value = error instanceof ApiError ? error.message : 'Không thể hủy hiệu lực bài thi.'
  } finally {
    isInvalidating.value = false
  }
}

onMounted(async () => {
  await loadMonitoringDetail()
  refreshTimer = window.setInterval(() => {
    loadMonitoringDetail()
  }, 5000)
})

onUnmounted(() => {
  if (refreshTimer) {
    window.clearInterval(refreshTimer)
  }
})
</script>

<style scoped>
.font-display {
  font-family: 'Inter', sans-serif;
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
