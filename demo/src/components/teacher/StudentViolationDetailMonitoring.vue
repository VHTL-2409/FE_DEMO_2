<template>
  <div :class="isDark ? 'dark' : 'light'" class="bg-background-light dark:bg-background-dark text-slate-900 dark:text-slate-100 font-display min-h-screen">
    <div class="relative flex h-auto min-h-screen w-full flex-col overflow-x-hidden">
      <div class="layout-container flex h-full grow flex-col">
        <TeacherTopHeader active-section="monitoring" />

        <main class="teacher-page-shell max-w-[1400px] flex flex-col">
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
            <div class="w-full md:w-auto flex flex-col sm:flex-row gap-2">
              <button class="w-full md:w-auto flex items-center justify-center gap-2 px-6 py-2.5 rounded-lg border border-slate-200 dark:border-slate-700 text-slate-700 dark:text-slate-300 font-bold text-sm hover:bg-slate-50 dark:hover:bg-slate-800 hover:-translate-y-0.5 hover:shadow-md transition-all duration-200" type="button">
                <span class="material-symbols-outlined text-lg">history</span> Nhật ký vi phạm
              </button>
              <button class="w-full md:w-auto flex items-center justify-center gap-2 px-6 py-2.5 rounded-lg bg-primary text-white font-bold text-sm hover:opacity-90 transition-all" type="button" @click="openLivestreamModal">
                <span class="material-symbols-outlined text-lg">live_tv</span> Xem livestream
              </button>
            </div>
          </div>

          <div class="relative grid grid-cols-1 lg:grid-cols-3 gap-8 animate-fade-up-delay">
            <div class="lg:col-span-2 flex flex-col gap-6">
              <div class="grid grid-cols-1 md:grid-cols-4 gap-4">
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
                  </div>
                </div>
                <div class="bg-white dark:bg-slate-900 p-5 rounded-xl border border-slate-200 dark:border-slate-800 shadow-sm">
                  <p class="text-slate-500 dark:text-slate-400 text-xs font-bold uppercase tracking-wider mb-2">Trạng thái bài thi</p>
                  <div class="flex items-end justify-between">
                    <span class="text-slate-600 bg-slate-100 dark:bg-slate-800 dark:text-slate-200 text-xs font-bold px-2.5 py-1.5 rounded">{{ statusLabel }}</span>
                  </div>
                </div>
              </div>
            </div>

            <div class="flex flex-col gap-6">
              <div class="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-xl overflow-hidden shadow-sm flex-1">
                <div class="p-4 border-b border-slate-200 dark:border-slate-800 flex items-center justify-between gap-3">
                  <h3 class="font-bold text-slate-900 dark:text-slate-100 flex items-center gap-2">
                    <span class="material-symbols-outlined text-red-500">assignment_late</span>
                    Nhật ký vi phạm
                  </h3>
                  <span class="text-xs text-slate-500">{{ sortedTimelineItems.length }} sự kiện</span>
                </div>
                <div class="divide-y divide-slate-100 dark:divide-slate-800">
                  <div v-if="sortedTimelineItems.length === 0" class="p-4 text-sm text-slate-500 dark:text-slate-400">
                    Chưa có sự kiện vi phạm nào cho lần làm bài này.
                  </div>
                  <div
                    v-for="item in paginatedTimelineItems"
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
                <div class="p-3 border-t border-slate-200 dark:border-slate-800 flex items-center justify-between" v-if="totalTimelinePages > 0">
                  <span class="text-xs text-slate-500">Trang {{ timelinePage }} / {{ totalTimelinePages }}</span>
                  <div class="flex gap-2">
                    <button class="px-3 py-1.5 text-xs font-semibold border border-slate-200 dark:border-slate-700 rounded disabled:opacity-50" type="button" :disabled="timelinePage <= 1" @click="timelinePage -= 1">Trước</button>
                    <button class="px-3 py-1.5 text-xs font-semibold border border-slate-200 dark:border-slate-700 rounded disabled:opacity-50" type="button" :disabled="timelinePage >= totalTimelinePages" @click="timelinePage += 1">Tiếp</button>
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
          <div v-if="isLivestreamModalOpen" class="fixed inset-0 z-50 flex items-center justify-center bg-black/60 px-4">
            <div class="w-full max-w-5xl rounded-2xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-900 shadow-2xl overflow-hidden">
              <div class="px-5 py-4 border-b border-slate-200 dark:border-slate-800 flex items-center justify-between gap-3">
                <div>
                  <h3 class="text-sm font-bold text-slate-900 dark:text-slate-100">Theo dõi bài làm realtime - {{ studentName }}</h3>
                  <p class="text-xs text-slate-500 dark:text-slate-400 mt-1">{{ liveAnsweredCount }} / {{ liveTotalQuestions }} câu đã trả lời · Cập nhật: {{ liveLastUpdatedLabel }}</p>
                </div>
                <button class="text-slate-500 hover:text-slate-700 dark:hover:text-slate-200" type="button" @click="closeLivestreamModal">
                  <span class="material-symbols-outlined">close</span>
                </button>
              </div>
              <div class="p-5 max-h-[70vh] overflow-y-auto bg-slate-50 dark:bg-slate-950/40">
                <div v-if="liveModalLoading" class="rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-900 p-6 text-sm text-slate-500 dark:text-slate-300">
                  Đang tải đề thi và đáp án của thí sinh...
                </div>
                <div v-else>
                  <div class="flex items-center justify-between mb-4 text-xs">
                    <span :class="liveModalSyncing ? 'text-amber-600' : (isLiveSocketConnected ? 'text-emerald-600' : 'text-slate-500 dark:text-slate-300')" class="font-semibold">
                      {{ liveModalSyncing ? 'Đang đồng bộ đáp án...' : (isLiveSocketConnected ? 'Đồng bộ websocket tức thì' : 'Đang dùng fallback polling') }}
                    </span>
                    <span v-if="liveModalError" class="text-rose-600">{{ liveModalError }}</span>
                  </div>
                  <div v-if="liveExamQuestions.length === 0" class="rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-900 p-6 text-sm text-slate-500 dark:text-slate-300">
                    Không có dữ liệu câu hỏi để hiển thị.
                  </div>
                  <div v-else class="space-y-4">
                    <div v-for="(question, idx) in liveExamQuestions" :key="question.id" class="rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-900 p-4">
                      <p class="text-xs font-bold text-primary mb-2">Câu {{ idx + 1 }}</p>
                      <p class="text-sm font-semibold text-slate-900 dark:text-slate-100 mb-3">{{ question.content }}</p>
                      <div class="space-y-2">
                        <div
                          v-for="option in question.options"
                          :key="`${question.id}-${option.id}`"
                          :class="liveAnswersByQuestionId[question.id] === option.id ? 'border-primary bg-primary/5 dark:bg-primary/10 text-primary' : 'border-slate-200 dark:border-slate-700 text-slate-600 dark:text-slate-300'"
                          class="rounded-lg border px-3 py-2 text-sm font-medium"
                        >
                          {{ option.text }}
                        </div>
                      </div>
                      <p class="mt-3 text-xs" :class="liveAnswersByQuestionId[question.id] ? 'text-emerald-600' : 'text-slate-500 dark:text-slate-400'">
                        {{ liveAnswersByQuestionId[question.id] ? `Thí sinh chọn: ${getLiveSelectedAnswerLabel(question)}` : 'Chưa chọn đáp án' }}
                      </p>
                    </div>
                  </div>
                </div>
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
import { Client } from '@stomp/stompjs'
import { API_BASE_URL, ApiError } from '../../services/apiClient'
import { getAttemptDetail, getAttemptReport } from '../../services/attemptService'
import { invalidateAttempt, listMonitoringTimeline, sendTeacherWarning } from '../../services/monitoringService'
import { listExamQuestions, parseQuestionOptions } from '../../services/questionService'
import { useRoute } from 'vue-router'
import SockJS from 'sockjs-client/dist/sockjs'
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
const isLivestreamModalOpen = ref(false)
const liveModalLoading = ref(false)
const liveModalSyncing = ref(false)
const liveModalError = ref('')
const liveLastUpdatedAt = ref(null)
const liveExamQuestions = ref([])
const liveAnswersByQuestionId = ref({})
const timelinePage = ref(1)
const timelinePageSize = 3
let refreshTimer = null
let liveFallbackTimer = null
let liveStompClient = null
let isLiveAnswersFetching = false
let hasPendingLiveAnswersFetch = false
const isLiveSocketConnected = ref(false)

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

const examId = computed(() => {
  const routeExamId = Number.parseInt(String(route.query.examId || ''), 10)
  if (Number.isFinite(routeExamId) && routeExamId > 0) return routeExamId
  const detailExamId = Number.parseInt(String(attemptDetail.value?.examId || ''), 10)
  return Number.isFinite(detailExamId) && detailExamId > 0 ? detailExamId : null
})

const liveTotalQuestions = computed(() => liveExamQuestions.value.length)
const liveAnsweredCount = computed(() => Object.values(liveAnswersByQuestionId.value).filter(Boolean).length)
const liveLastUpdatedLabel = computed(() => {
  if (!liveLastUpdatedAt.value) return 'chưa có dữ liệu'
  return new Date(liveLastUpdatedAt.value).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' })
})

const getLiveSelectedAnswerLabel = (question) => {
  const selected = liveAnswersByQuestionId.value[question.id]
  if (!selected) return ''
  const matched = question.options.find((option) => option.id === selected)
  return matched?.text || selected
}

const getAuthToken = () => {
  if (typeof window === 'undefined') return ''
  return String(window.localStorage.getItem('auth_token') || '')
}

const stopLiveFallbackPolling = () => {
  if (liveFallbackTimer) {
    window.clearInterval(liveFallbackTimer)
    liveFallbackTimer = null
  }
}

const applyLiveAnswersReport = (report) => {
  const mapped = (report?.answers || []).reduce((acc, answer) => {
    acc[answer.questionId] = answer.selectedAnswer
    return acc
  }, {})
  liveAnswersByQuestionId.value = mapped
  liveLastUpdatedAt.value = Date.now()
  liveModalError.value = ''
}

const loadLiveAnswers = async (force = false) => {
  if (!attemptId.value) return
  if (isLiveAnswersFetching && !force) {
    hasPendingLiveAnswersFetch = true
    return
  }

  isLiveAnswersFetching = true
  liveModalSyncing.value = true
  try {
    const report = await getAttemptReport(attemptId.value)
    applyLiveAnswersReport(report)
  } catch (error) {
    liveModalError.value = error instanceof ApiError ? error.message : 'Đồng bộ đáp án bị gián đoạn tạm thời.'
  } finally {
    isLiveAnswersFetching = false
    liveModalSyncing.value = false
    if (hasPendingLiveAnswersFetch) {
      hasPendingLiveAnswersFetch = false
      await loadLiveAnswers(true)
    }
  }
}

const loadLiveQuestions = async () => {
  if (!examId.value) {
    liveModalError.value = 'Không xác định được đề thi để tải câu hỏi.'
    liveExamQuestions.value = []
    return
  }
  const questionList = await listExamQuestions(examId.value)
  liveExamQuestions.value = questionList.map((item) => ({
    id: item.id,
    content: item.content,
    options: parseQuestionOptions(item.options)
  }))
}

const startLiveFallbackPolling = () => {
  stopLiveFallbackPolling()
  liveFallbackTimer = window.setInterval(() => {
    if (isLivestreamModalOpen.value && !isLiveSocketConnected.value) {
      loadLiveAnswers()
    }
  }, 7000)
}

const disconnectLiveAnswersSocket = () => {
  if (liveStompClient) {
    liveStompClient.deactivate()
    liveStompClient = null
  }
  isLiveSocketConnected.value = false
}

const connectLiveAnswersSocket = () => {
  if (!attemptId.value) return

  disconnectLiveAnswersSocket()

  const wsUrl = `${API_BASE_URL.replace(/\/$/, '')}/ws`
  const token = getAuthToken()
  liveStompClient = new Client({
    reconnectDelay: 3000,
    connectHeaders: token ? { Authorization: `Bearer ${token}` } : {},
    webSocketFactory: () => new SockJS(wsUrl)
  })

  liveStompClient.onConnect = () => {
    isLiveSocketConnected.value = true
    liveStompClient.subscribe(`/topic/attempts/${attemptId.value}/draft-updates`, (frame) => {
      try {
        const payload = JSON.parse(frame.body || '{}')
        if (String(payload?.type || '').toUpperCase() !== 'DRAFT_SAVED') return
        void loadLiveAnswers()
      } catch {
        // ignore malformed draft update payload
      }
    })
    void loadLiveAnswers()
  }

  liveStompClient.onStompError = () => {
    isLiveSocketConnected.value = false
    startLiveFallbackPolling()
  }

  liveStompClient.onWebSocketClose = () => {
    isLiveSocketConnected.value = false
    startLiveFallbackPolling()
  }

  liveStompClient.activate()
}

const startLiveAnswersSync = async () => {
  liveModalLoading.value = true
  liveModalError.value = ''
  liveLastUpdatedAt.value = null
  try {
    await loadLiveQuestions()
    await loadLiveAnswers(true)
    if (!isLivestreamModalOpen.value) return
    connectLiveAnswersSocket()
    startLiveFallbackPolling()
  } catch (error) {
    liveModalError.value = error instanceof ApiError ? error.message : 'Không thể tải dữ liệu bài làm realtime.'
  } finally {
    liveModalLoading.value = false
  }
}

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
    case 'COPY_PASTE':
      return 'Copy/paste trong lúc làm bài'
    case 'IDLE_TIME':
      return 'Không thao tác trong thời gian dài'
    case 'DEVTOOLS_OPEN':
      return 'Mở DevTools trong lúc làm bài'
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

const sortedTimelineItems = computed(() => timeline.value
  .map((entry, index) => {
    const isEvent = entry.type === 'MONITORING_EVENT'
    const isSnapshot = entry.type === 'RISK_SNAPSHOT'
    const atTs = new Date(entry.at || 0).getTime() || 0

    return {
      key: `${entry.type || 'entry'}-${entry.at || index}-${index}`,
      atTs,
      icon: isSnapshot ? 'monitor_heart' : 'warning',
      iconClass: isSnapshot ? 'text-amber-500' : 'text-red-600',
      highlightClass: isEvent ? 'bg-red-50 dark:bg-red-900/10' : '',
      title: isEvent ? formatEventType(entry.eventType) : `Cập nhật risk score: ${entry.riskScore ?? 0}`,
      description: isEvent
        ? (entry.details || 'Không có mô tả chi tiết.')
        : `Suspicious: ${entry.suspicious ? 'Có' : 'Không'}`,
      timeLabel: formatTimelineTime(entry.at)
    }
  })
  .sort((a, b) => b.atTs - a.atTs))

const totalTimelinePages = computed(() => {
  if (!sortedTimelineItems.value.length) return 0
  return Math.ceil(sortedTimelineItems.value.length / timelinePageSize)
})

const paginatedTimelineItems = computed(() => {
  const start = (timelinePage.value - 1) * timelinePageSize
  return sortedTimelineItems.value.slice(start, start + timelinePageSize)
})

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
    timelinePage.value = 1
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

const resetLiveModalState = () => {
  liveModalSyncing.value = false
  liveModalError.value = ''
  liveLastUpdatedAt.value = null
  liveAnswersByQuestionId.value = {}
  liveExamQuestions.value = []
  isLiveSocketConnected.value = false
  hasPendingLiveAnswersFetch = false
  isLiveAnswersFetching = false
}

const openLivestreamModal = async () => {
  isLivestreamModalOpen.value = true
  await startLiveAnswersSync()
}

const closeLivestreamModal = () => {
  isLivestreamModalOpen.value = false
  stopLiveFallbackPolling()
  disconnectLiveAnswersSocket()
  resetLiveModalState()
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
  stopLiveFallbackPolling()
  disconnectLiveAnswersSocket()
  resetLiveModalState()
})
</script>

