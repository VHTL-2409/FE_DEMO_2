<template>
  <div :class="isDark ? 'dark' : 'light'" class="bg-slate-50 dark:bg-slate-950 text-slate-900 dark:text-slate-100 font-display min-h-screen">
    <div class="relative flex h-auto min-h-screen w-full flex-col overflow-x-hidden">
      <div class="layout-container flex h-full grow flex-col">
        <TeacherTopHeader active-section="monitoring" />

        <main class="teacher-page-shell max-w-[1400px] flex flex-col">
          <div class="pointer-events-none absolute -top-16 -left-16 size-72 rounded-full bg-indigo-500/20 blur-3xl animate-float-slow"></div>
          <div class="pointer-events-none absolute -bottom-24 -right-12 size-80 rounded-full bg-violet-500/15 blur-3xl animate-float-delay"></div>

          <nav class="relative flex flex-wrap items-center gap-2 mb-6 animate-fade-up text-sm">
            <span class="text-slate-500">Đề thi</span>
            <span class="material-symbols-outlined text-slate-400 text-base">chevron_right</span>
            <span class="text-slate-500">{{ examName }}</span>
            <span class="material-symbols-outlined text-slate-400 text-base">chevron_right</span>
            <span class="text-indigo-600 dark:text-indigo-400 font-semibold">Giám sát: {{ studentName }}</span>
          </nav>

          <!-- Student profile card -->
          <div class="glass-card rounded-2xl p-6 mb-8 flex flex-col md:flex-row justify-between items-start md:items-center gap-6 shadow-lg animate-fade-up-delay">
            <div class="flex gap-5 items-center">
              <div class="relative shrink-0">
                <div class="size-20 rounded-2xl bg-center bg-no-repeat bg-cover ring-2 ring-offset-2" :class="suspicious ? 'ring-red-500/50' : 'ring-slate-200 dark:ring-slate-700'" :style="`background-image: url('${studentAvatar}')`"></div>
                <div v-if="suspicious" class="absolute -bottom-1 -right-1 size-7 rounded-lg bg-red-500 flex items-center justify-center shadow-lg">
                  <span class="material-symbols-outlined text-white text-sm">warning</span>
                </div>
              </div>
              <div>
                <div class="flex items-center gap-3 mb-1 flex-wrap">
                  <h1 class="text-2xl font-bold text-slate-900 dark:text-slate-100">{{ studentName }}</h1>
                  <span :class="riskBadgeClass" class="px-3 py-1.5 rounded-lg text-xs font-bold uppercase tracking-wider">
                    {{ riskBadgeText }}
                  </span>
                </div>
                <p class="text-slate-500 dark:text-slate-400 text-sm flex items-center gap-4 flex-wrap">
                  <span>ID: <span class="font-medium text-slate-700 dark:text-slate-300 font-mono">{{ studentId }}</span></span>
                  <span class="h-4 w-px bg-slate-300 dark:bg-slate-700"></span>
                  <span>Đề thi: <span class="font-medium text-slate-700 dark:text-slate-300">{{ examName }}</span></span>
                </p>
                <div class="mt-2 flex flex-wrap items-center gap-2 text-xs">
                  <span class="inline-flex items-center gap-2 px-2.5 py-1 rounded-lg glass-card text-slate-600 dark:text-slate-300">
                    <span :class="isSyncing ? 'bg-amber-500' : 'bg-emerald-500'" class="size-2 rounded-full"></span>
                    {{ isSyncing ? 'Đang đồng bộ...' : 'Đồng bộ ổn định' }}
                  </span>
                  <span class="text-slate-500 dark:text-slate-400">Cập nhật: {{ lastUpdatedLabel }}</span>
                </div>
              </div>
            </div>
            <div class="w-full md:w-auto">
              <button class="w-full md:w-auto flex items-center justify-center gap-2 px-5 py-2.5 rounded-xl bg-indigo-600 hover:bg-indigo-700 text-white font-semibold text-sm transition-all shadow-lg shadow-indigo-500/25" type="button" @click="openLivestreamModal">
                <span class="material-symbols-outlined text-lg">live_tv</span> Xem bài làm realtime
              </button>
            </div>
          </div>

          <div class="relative grid grid-cols-1 lg:grid-cols-3 gap-4 sm:gap-6 animate-fade-up-delay">
            <div class="lg:col-span-2 flex flex-col gap-4 sm:gap-6">
              <div class="grid grid-cols-2 sm:grid-cols-4 gap-3 sm:gap-4">
                <div class="glass-card rounded-2xl p-5 shadow-lg">
                  <div class="flex items-center gap-2 mb-3">
                    <span class="material-symbols-outlined text-indigo-500 text-xl">task_alt</span>
                    <p class="text-slate-500 dark:text-slate-400 text-xs font-bold uppercase tracking-wider">Tiến độ</p>
                  </div>
                  <p class="text-2xl font-bold text-slate-900 dark:text-slate-100 tracking-tight">{{ answeredCount }} <span class="text-slate-400 font-normal">/ {{ totalQuestions }}</span></p>
                  <div class="w-full bg-slate-100 dark:bg-slate-800 h-2 rounded-full mt-3">
                    <div class="bg-indigo-500 h-2 rounded-full transition-all duration-500" :style="{ width: `${progressPercent}%` }"></div>
                  </div>
                  <span class="text-indigo-600 dark:text-indigo-400 text-xs font-bold mt-2 inline-block">{{ progressPercent }}% hoàn thành</span>
                </div>
                <div class="glass-card rounded-2xl p-5 shadow-lg" :class="flaggedEvents.length > 0 ? 'gradient-card-alert ring-1 ring-red-500/20' : ''">
                  <div class="flex items-center gap-2 mb-3">
                    <span class="material-symbols-outlined text-xl" :class="flaggedEvents.length > 0 ? 'text-red-500' : 'text-slate-400'">flag</span>
                    <p class="text-slate-500 dark:text-slate-400 text-xs font-bold uppercase tracking-wider">Sự kiện gắn cờ</p>
                  </div>
                  <p class="text-2xl font-bold tracking-tight" :class="flaggedEvents.length > 0 ? 'text-red-600 dark:text-red-400' : 'text-slate-900 dark:text-slate-100'">{{ flaggedEvents.length }}</p>
                  <span class="text-xs font-bold mt-2 inline-block" :class="flaggedEvents.length > 0 ? 'text-red-600 dark:text-red-400' : 'text-slate-500'">Risk {{ riskScore }}</span>
                </div>
                <div class="glass-card rounded-2xl p-5 shadow-lg">
                  <div class="flex items-center gap-2 mb-3">
                    <span class="material-symbols-outlined text-emerald-500 text-xl">schedule</span>
                    <p class="text-slate-500 dark:text-slate-400 text-xs font-bold uppercase tracking-wider">Còn lại</p>
                  </div>
                  <p class="text-2xl font-bold text-slate-900 dark:text-slate-100 font-mono tracking-tight">{{ remainingTimeLabel }}</p>
                </div>
                <div class="glass-card rounded-2xl p-5 shadow-lg">
                  <div class="flex items-center gap-2 mb-3">
                    <span class="material-symbols-outlined text-slate-500 text-xl">info</span>
                    <p class="text-slate-500 dark:text-slate-400 text-xs font-bold uppercase tracking-wider">Trạng thái</p>
                  </div>
                  <span class="text-sm font-bold px-3 py-1.5 rounded-lg bg-slate-100 dark:bg-slate-800 text-slate-700 dark:text-slate-200">{{ statusLabel }}</span>
                </div>
              </div>
            </div>

            <div class="flex flex-col gap-4 sm:gap-6">
              <!-- Card hành vi gian lận -->
              <div class="glass-card rounded-2xl overflow-hidden shadow-lg flex-1">
                <div class="p-4 border-b border-slate-200 dark:border-slate-700/50 flex items-center justify-between gap-3">
                  <h3 class="font-bold text-slate-900 dark:text-slate-100 flex items-center gap-2">
                    <span class="material-symbols-outlined text-red-500">assignment_late</span>
                    Hành vi gian lận
                  </h3>
                  <span class="text-xs font-medium text-slate-500 bg-slate-100 dark:bg-slate-800 px-2.5 py-1 rounded-lg">{{ sortedTimelineItems.length }} sự kiện</span>
                </div>
                <div class="divide-y divide-slate-100 dark:divide-slate-800/50 max-h-[280px] overflow-y-auto">
                  <div v-if="sortedTimelineItems.length === 0" class="p-6 text-center text-slate-500 dark:text-slate-400">
                    <span class="material-symbols-outlined text-3xl text-slate-300 dark:text-slate-600 block mb-2">check_circle</span>
                    Chưa có hành vi gian lận nào được ghi nhận
                  </div>
                  <div
                    v-for="item in paginatedTimelineItems"
                    :key="item.key"
                    :class="item.highlightClass"
                    class="p-4 flex gap-4 hover:bg-slate-50/50 dark:hover:bg-slate-800/30 transition-colors"
                  >
                    <div class="shrink-0 size-9 rounded-lg flex items-center justify-center" :class="item.highlightClass ? 'bg-red-100 dark:bg-red-900/20' : 'bg-slate-100 dark:bg-slate-800'">
                      <span class="material-symbols-outlined text-lg" :class="item.iconClass">{{ item.icon }}</span>
                    </div>
                    <div class="flex-1 min-w-0">
                      <div class="flex justify-between items-start mb-1 gap-2">
                        <p class="text-sm font-semibold text-slate-900 dark:text-slate-100">{{ item.title }}</p>
                        <span class="text-[10px] text-slate-500 font-medium shrink-0">{{ item.timeLabel }}</span>
                      </div>
                      <p class="text-xs text-slate-600 dark:text-slate-400 leading-relaxed">{{ item.description }}</p>
                    </div>
                  </div>
                </div>
                <div class="p-3 border-t border-slate-200 dark:border-slate-700/50 flex items-center justify-between bg-slate-50/50 dark:bg-slate-800/30" v-if="totalTimelinePages > 0">
                  <span class="text-xs text-slate-500">Trang {{ timelinePage }} / {{ totalTimelinePages }}</span>
                  <div class="flex gap-2">
                    <button class="px-3 py-2 text-xs font-semibold rounded-lg border border-slate-200 dark:border-slate-700 hover:bg-slate-100 dark:hover:bg-slate-800 disabled:opacity-50 transition-all" type="button" :disabled="timelinePage <= 1" @click="timelinePage -= 1">Trước</button>
                    <button class="px-3 py-2 text-xs font-semibold rounded-lg border border-slate-200 dark:border-slate-700 hover:bg-slate-100 dark:hover:bg-slate-800 disabled:opacity-50 transition-all" type="button" :disabled="timelinePage >= totalTimelinePages" @click="timelinePage += 1">Tiếp</button>
                  </div>
                </div>
              </div>

              <div class="glass-card rounded-2xl p-6 shadow-lg">
                <h3 class="font-bold text-slate-900 dark:text-slate-100 mb-5 flex items-center gap-2">
                  <span class="material-symbols-outlined text-indigo-500">tune</span>
                  Điều khiển giám thị
                </h3>
                <div class="flex flex-col gap-3">
                  <button :disabled="isWarningSending || isInvalidating" class="w-full flex items-center justify-center gap-3 py-3.5 px-4 rounded-xl bg-amber-500 hover:bg-amber-600 text-white font-bold transition-all disabled:opacity-60 shadow-lg shadow-amber-500/20" type="button" @click="openWarningModal">
                    <span class="material-symbols-outlined">warning</span> {{ isWarningSending ? 'Đang gửi...' : 'Gửi cảnh báo' }}
                  </button>
                  <button :disabled="isWarningSending || isInvalidating" class="w-full flex items-center justify-center gap-3 py-3.5 px-4 rounded-xl bg-red-600 hover:bg-red-700 text-white font-bold transition-all disabled:opacity-60 shadow-lg shadow-red-500/20" type="button" @click="openInvalidateModal">
                    <span class="material-symbols-outlined">block</span> {{ isInvalidating ? 'Đang xử lý...' : 'Đình chỉ bài thi' }}
                  </button>
                </div>
              </div>

            </div>
          </div>

          <!-- Modal gửi cảnh báo -->
          <div v-if="showWarningModal" class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm" @click.self="closeWarningModal">
            <div class="glass-card rounded-2xl p-6 max-w-md w-full shadow-2xl">
              <div class="flex items-center gap-3 mb-4">
                <div class="size-12 rounded-xl bg-amber-100 dark:bg-amber-900/30 flex items-center justify-center">
                  <span class="material-symbols-outlined text-amber-600 dark:text-amber-400 text-2xl">warning</span>
                </div>
                <div>
                  <h3 class="text-lg font-bold text-slate-900 dark:text-white">Gửi cảnh báo</h3>
                  <p class="text-sm text-slate-500 dark:text-slate-400 mt-0.5">Sinh viên: {{ studentName }}</p>
                </div>
              </div>
              <div class="mb-4">
                <label class="block text-sm font-medium text-slate-700 dark:text-slate-300 mb-2">Nội dung cảnh báo (tùy chọn)</label>
                <textarea v-model="warningMessageInput" rows="3" class="w-full px-4 py-3 rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-slate-100 focus:ring-2 focus:ring-amber-500/50 focus:border-amber-500 transition-all" placeholder="Để trống sẽ dùng mặc định."></textarea>
              </div>
              <div class="flex gap-3 justify-end">
                <button type="button" class="px-4 py-2.5 rounded-xl border border-slate-200 dark:border-slate-700 text-slate-700 dark:text-slate-300 font-semibold hover:bg-slate-50 dark:hover:bg-slate-800" @click="closeWarningModal">Hủy</button>
                <button type="button" class="px-4 py-2.5 rounded-xl bg-amber-500 hover:bg-amber-600 text-white font-semibold flex items-center gap-2" @click="confirmSendWarning">
                  <span class="material-symbols-outlined text-lg">send</span>
                  Gửi cảnh báo
                </button>
              </div>
            </div>
          </div>

          <!-- Modal đình chỉ bài thi -->
          <div v-if="showInvalidateModal" class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm" @click.self="closeInvalidateModal">
            <div class="glass-card rounded-2xl p-6 max-w-md w-full shadow-2xl">
              <div class="flex items-center gap-3 mb-4">
                <div class="size-12 rounded-xl bg-rose-100 dark:bg-rose-900/30 flex items-center justify-center">
                  <span class="material-symbols-outlined text-rose-600 dark:text-rose-400 text-2xl">block</span>
                </div>
                <div>
                  <h3 class="text-lg font-bold text-slate-900 dark:text-white">Xác nhận đình chỉ bài thi</h3>
                  <p class="text-sm text-slate-500 dark:text-slate-400 mt-0.5">Sinh viên: {{ studentName }}</p>
                </div>
              </div>
              <p class="text-sm text-slate-600 dark:text-slate-400 mb-4">Hành động này không thể hoàn tác. Bạn chắc chắn muốn đình chỉ bài thi?</p>
              <div class="mb-4">
                <label class="block text-sm font-medium text-slate-700 dark:text-slate-300 mb-2">Lý do đình chỉ (tùy chọn)</label>
                <textarea v-model="invalidateReasonInput" rows="3" class="w-full px-4 py-3 rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-slate-100 focus:ring-2 focus:ring-rose-500/50 focus:border-rose-500 transition-all" placeholder="Để trống sẽ dùng mặc định."></textarea>
              </div>
              <div class="flex gap-3 justify-end">
                <button type="button" class="px-4 py-2.5 rounded-xl border border-slate-200 dark:border-slate-700 text-slate-700 dark:text-slate-300 font-semibold hover:bg-slate-50 dark:hover:bg-slate-800" @click="closeInvalidateModal">Hủy</button>
                <button type="button" class="px-4 py-2.5 rounded-xl bg-rose-600 hover:bg-rose-700 text-white font-semibold flex items-center gap-2" @click="confirmInvalidate">
                  <span class="material-symbols-outlined text-lg">block</span>
                  Xác nhận đình chỉ
                </button>
              </div>
            </div>
          </div>

          <div v-if="isLivestreamModalOpen" class="modal-overlay" role="dialog" aria-modal="true" aria-labelledby="livestream-modal-title" @click.self="closeLivestreamModal">
            <div class="modal-content modal-content-glass w-full max-w-5xl">
              <div class="modal-header bg-slate-50/50 dark:bg-slate-800/30">
                <div class="flex items-center gap-3">
                  <div class="size-10 rounded-xl bg-indigo-100 dark:bg-indigo-500/20 flex items-center justify-center">
                    <span class="material-symbols-outlined text-indigo-600 dark:text-indigo-400 text-xl">live_tv</span>
                  </div>
                  <div>
                    <h3 id="livestream-modal-title" class="text-base font-bold text-slate-900 dark:text-slate-100">Theo dõi bài làm realtime - {{ studentName }}</h3>
                    <p class="text-xs text-slate-500 dark:text-slate-400 mt-0.5">{{ liveAnsweredCount }} / {{ liveTotalQuestions }} câu · Cập nhật: {{ liveLastUpdatedLabel }}</p>
                  </div>
                </div>
                <button type="button" class="modal-close-btn" aria-label="Đóng" @click="closeLivestreamModal">
                  <span class="material-symbols-outlined">close</span>
                </button>
              </div>
              <div class="p-6 max-h-[70vh] overflow-y-auto bg-slate-50/50 dark:bg-slate-900/50">
                <div v-if="liveModalLoading" class="rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-900 p-6 text-sm text-slate-500 dark:text-slate-300">
                  Đang tải đề thi và đáp án của thí sinh...
                </div>
                <div v-else>
                  <div class="flex items-center justify-between mb-4 text-xs">
                    <span :class="liveModalSyncing ? 'text-amber-600' : (isLiveSocketConnected ? 'text-emerald-600' : 'text-slate-500 dark:text-slate-300')" class="font-semibold">
                      {{ liveModalSyncing ? 'Đang đồng bộ đáp án...' : (isLiveSocketConnected ? 'Đồng bộ websocket tức thì' : 'Đang dùng fallback polling') }}
                    </span>
                  </div>
                  <div v-if="liveExamQuestions.length === 0" class="rounded-2xl glass-card p-8 text-center text-slate-500 dark:text-slate-400">
                    <span class="material-symbols-outlined text-4xl text-slate-300 dark:text-slate-600 block mb-2">quiz</span>
                    Không có dữ liệu câu hỏi để hiển thị.
                  </div>
                  <div v-else class="space-y-4">
                    <div v-for="(question, idx) in liveExamQuestions" :key="question.id" class="rounded-2xl glass-card p-5 border border-slate-200/50 dark:border-slate-700/50 hover:shadow-lg transition-shadow">
                      <p class="text-xs font-bold text-indigo-600 dark:text-indigo-400 mb-2">Câu {{ idx + 1 }}</p>
                      <p class="text-sm font-semibold text-slate-900 dark:text-slate-100 mb-4">{{ question.content }}</p>
                      <div class="space-y-2">
                        <div
                          v-for="option in question.options"
                          :key="`${question.id}-${option.id}`"
                          :class="liveAnswersByQuestionId[question.id] === option.id ? 'border-indigo-500 bg-indigo-50 dark:bg-indigo-500/10 text-indigo-700 dark:text-indigo-300 ring-1 ring-indigo-500/30' : 'border-slate-200 dark:border-slate-700 text-slate-600 dark:text-slate-300'"
                          class="rounded-xl border px-4 py-2.5 text-sm font-medium transition-all"
                        >
                          {{ option.text }}
                        </div>
                      </div>
                      <p class="mt-3 text-xs font-medium" :class="liveAnswersByQuestionId[question.id] ? 'text-emerald-600 dark:text-emerald-400' : 'text-slate-500 dark:text-slate-400'">
                        {{ liveAnswersByQuestionId[question.id] ? `✓ Thí sinh chọn: ${getLiveSelectedAnswerLabel(question)}` : 'Chưa chọn đáp án' }}
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
import { API_BASE_URL, ApiError } from '../../services/apiClient'
import { getAttemptDetail, getAttemptReport } from '../../services/attemptService'
import { invalidateAttempt, listMonitoringTimeline, sendTeacherWarning } from '../../services/monitoringService'
import { listExamQuestions, parseQuestionOptions } from '../../services/questionService'
import { useRoute } from 'vue-router'
import { useToast } from '../../composables/useToast'
import TeacherTopHeader from './TeacherTopHeader.vue'

const route = useRoute()
const isDark = ref(false)
const isSyncing = ref(false)
const attemptDetail = ref(null)
const attemptReport = ref(null)
const timeline = ref([])
const lastUpdatedAt = ref(null)
const isWarningSending = ref(false)
const isInvalidating = ref(false)
const showWarningModal = ref(false)
const showInvalidateModal = ref(false)
const warningMessageInput = ref('')
const invalidateReasonInput = ref('')
const isLivestreamModalOpen = ref(false)
const liveModalLoading = ref(false)
const liveModalSyncing = ref(false)
const liveLastUpdatedAt = ref(null)
const liveExamQuestions = ref([])
const liveAnswersByQuestionId = ref({})
const toast = useToast()
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
  () => route.query.avatar || 'data:image/svg+xml,%3Csvg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="%2394a3b8"%3E%3Cpath d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/%3E%3C/svg%3E'
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

const getAuthToken = async () => {
  if (typeof window === 'undefined') return ''
  const { getStoredToken } = await import('../../services/authService')
  return String(getStoredToken() || '')
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
    toast.error(error instanceof ApiError ? error.message : 'Đồng bộ đáp án bị gián đoạn tạm thời.')
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
    toast.error('Không xác định được đề thi để tải câu hỏi.')
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
  }, 1500)
}

const disconnectLiveAnswersSocket = () => {
  if (liveStompClient) {
    liveStompClient.deactivate()
    liveStompClient = null
  }
  isLiveSocketConnected.value = false
}

const connectLiveAnswersSocket = async () => {
  if (!attemptId.value) return

  disconnectLiveAnswersSocket()

  const [{ Client }, { default: SockJS }] = await Promise.all([
    import('@stomp/stompjs'),
    import('sockjs-client')
  ])

  const wsUrl = `${API_BASE_URL.replace(/\/$/, '')}/ws`
  const token = await getAuthToken()
  liveStompClient = new Client({
    reconnectDelay: 1000,
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
  liveLastUpdatedAt.value = null
  try {
    await loadLiveQuestions()
    await loadLiveAnswers(true)
    if (!isLivestreamModalOpen.value) return
    connectLiveAnswersSocket()
    startLiveFallbackPolling()
  } catch (error) {
    toast.error(error instanceof ApiError ? error.message : 'Không thể tải dữ liệu bài làm realtime.')
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
  .filter((entry) => entry.type === 'MONITORING_EVENT')
  .map((entry, index) => {
    const atTs = new Date(entry.at || 0).getTime() || 0
    return {
      key: `${entry.type || 'entry'}-${entry.at || index}-${index}`,
      atTs,
      icon: 'warning',
      iconClass: 'text-red-600',
      highlightClass: 'bg-red-50 dark:bg-red-900/10',
      title: formatEventType(entry.eventType),
      description: entry.details || 'Không có mô tả chi tiết.',
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
    toast.error('Thiếu attemptId. Vui lòng mở chi tiết từ trang giám sát trực tiếp.')
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
    lastUpdatedAt.value = Date.now()
  } catch (error) {
    toast.error(error instanceof ApiError ? error.message : 'Không thể tải dữ liệu giám sát chi tiết.')
  } finally {
    isSyncing.value = false
  }
}

const openWarningModal = () => {
  warningMessageInput.value = ''
  showWarningModal.value = true
}

const closeWarningModal = () => {
  showWarningModal.value = false
  warningMessageInput.value = ''
}

const confirmSendWarning = async () => {
  if (!attemptId.value) return

  isWarningSending.value = true
  try {
    await sendTeacherWarning(attemptId.value, warningMessageInput.value.trim())
    toast.success('Đã gửi cảnh báo realtime đến thí sinh.')
    closeWarningModal()
    await loadMonitoringDetail()
  } catch (error) {
    toast.error(error instanceof ApiError ? error.message : 'Không thể gửi cảnh báo lúc này.')
  } finally {
    isWarningSending.value = false
  }
}

const openInvalidateModal = () => {
  invalidateReasonInput.value = ''
  showInvalidateModal.value = true
}

const closeInvalidateModal = () => {
  showInvalidateModal.value = false
  invalidateReasonInput.value = ''
}

const confirmInvalidate = async () => {
  if (!attemptId.value) return

  isInvalidating.value = true
  try {
    await invalidateAttempt(attemptId.value, invalidateReasonInput.value.trim())
    toast.success('Đã đình chỉ bài thi thành công.')
    closeInvalidateModal()
    await loadMonitoringDetail()
  } catch (error) {
    toast.error(error instanceof ApiError ? error.message : 'Không thể hủy hiệu lực bài thi.')
  } finally {
    isInvalidating.value = false
  }
}

const resetLiveModalState = () => {
  liveModalSyncing.value = false
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
  }, 2000)
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

