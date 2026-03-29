<template>
  <div :class="isDark ? 'dark' : 'light'" class="flex h-full min-h-0 flex-1 flex-col bg-background-light font-display text-slate-900 dark:bg-background-dark dark:text-slate-100">
    <div class="layout-container flex min-h-0 flex-1 flex-col">
      <TeacherTopHeader class="shrink-0" active-section="monitoring" />

      <main class="teacher-stitch-main teacher-page-shell portal-scrollbar relative min-h-0 w-full max-w-none flex-1 overflow-x-hidden overflow-y-auto px-3 py-3 sm:px-4 lg:px-5">
        <div class="live-monitoring-shell mx-auto w-full max-w-screen-2xl space-y-3 sm:space-y-4">
        <header class="relative flex w-full shrink-0 flex-wrap items-end justify-between gap-2 sm:gap-3 animate-fade-up">
          <div class="min-w-0">
            <p class="portal-kicker mb-0.5">
              <RouterLink to="/teacher/dashboard" class="text-slate-500 transition hover:text-[var(--role-primary)] dark:text-slate-400">Trang chủ</RouterLink>
              <span class="mx-1.5 text-slate-300 dark:text-slate-600">/</span>
              <RouterLink to="/teacher/live-monitoring" class="text-slate-500 transition hover:text-[var(--role-primary)] dark:text-slate-400">Chọn phiên</RouterLink>
              <span class="mx-1.5 text-slate-300 dark:text-slate-600">/</span>
              <span class="font-semibold text-[var(--role-primary)]">Theo dõi</span>
            </p>
            <h1 class="stitch-font-headline flex flex-wrap items-center gap-2 text-xl font-bold tracking-tight text-amber-900 dark:text-amber-100 md:text-2xl lg:text-3xl">
              <span class="min-w-0 break-words">{{ selectedExamTitle }}</span>
              <span class="inline-flex shrink-0 items-center gap-1.5 rounded-full border border-emerald-500/30 bg-emerald-500/15 px-2.5 py-1 text-[11px] font-semibold text-emerald-700 dark:text-emerald-400">
                <span class="h-1.5 w-1.5 animate-pulse rounded-full bg-emerald-500"></span>
                LIVE
              </span>
            </h1>
            <div class="mt-1.5 flex flex-wrap items-center gap-2 text-xs text-slate-500 dark:text-slate-400 sm:text-sm">
              <span>{{ selectedExamMeta }}</span>
              <div v-if="selectedExamCode" class="flex items-center gap-2 rounded-lg border border-slate-200 bg-slate-100 px-3 py-1.5 dark:border-slate-600 dark:bg-slate-700/50">
                <span class="text-xs font-semibold uppercase text-slate-500 dark:text-slate-400">Mã đề</span>
                <code class="text-sm font-mono font-bold text-slate-900 dark:text-white">{{ selectedExamCode }}</code>
              </div>
            </div>
          </div>
          <button
            type="button"
            class="shrink-0 rounded-xl border border-primary/25 bg-white/80 px-4 py-2 text-sm font-bold text-primary shadow-sm transition hover:bg-primary/10 dark:bg-slate-900/60"
            @click="goBackToSessionList"
          >
            Chọn phiên khác
          </button>
        </header>

        <!-- KPI — một hàng, padding nhỏ hơn -->
        <div class="grid w-full shrink-0 grid-cols-3 gap-2 sm:gap-3 animate-fade-up-delay">
          <div class="stitch-stat-bento border-l-4 border-primary py-2.5 pl-3 sm:pl-4">
            <p class="text-[9px] font-bold uppercase tracking-widest text-slate-500 dark:text-slate-400">Có mặt</p>
            <p class="font-display text-xl font-bold tabular-nums text-primary dark:text-amber-200 sm:text-2xl">{{ attempts.length }}</p>
          </div>
          <div class="stitch-stat-bento border-l-4 border-amber-600 py-2.5 pl-3 sm:pl-4" :class="flaggedCount > 0 ? 'ring-1 ring-red-500/15' : ''">
            <p class="text-[9px] font-bold uppercase tracking-widest text-slate-500 dark:text-slate-400">Gắn cờ</p>
            <p class="font-display text-xl font-bold tabular-nums sm:text-2xl" :class="flaggedCount > 0 ? 'text-red-600 dark:text-red-400' : 'text-slate-900 dark:text-white'">{{ flaggedCount }}</p>
          </div>
          <div class="stitch-stat-bento border-l-4 border-[#795900] py-2.5 pl-3 sm:pl-4">
            <p class="text-[9px] font-bold uppercase tracking-widest text-slate-500 dark:text-slate-400">Thời lượng</p>
            <p class="font-display text-xl font-bold tabular-nums text-amber-900 dark:text-amber-100 sm:text-2xl">{{ examDurationLabel }}</p>
          </div>
        </div>

        <!-- Trạng thái kết nối -->
        <div class="flex w-full shrink-0 flex-wrap items-center justify-between gap-2 text-xs sm:text-sm animate-fade-up-delay">
          <span class="inline-flex items-center gap-2 rounded-full border border-[color:rgba(219,194,176,0.2)] bg-white px-3 py-1.5 text-xs font-semibold shadow-sm dark:border-slate-700 dark:bg-slate-900" :class="isSocketConnected ? 'text-emerald-600 dark:text-emerald-400' : 'text-slate-500'">
            <span class="material-symbols-outlined text-base">{{ isSocketConnected ? 'wifi' : 'wifi_off' }}</span>
            {{ isSocketConnected ? 'Realtime' : 'Polling' }}
          </span>
          <span class="text-xs text-slate-500 dark:text-slate-400">Cập nhật: {{ lastUpdatedLabel }}</span>
        </div>

        <!-- Modal gửi cảnh báo -->
        <div v-if="showWarningModal" class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm" @click.self="closeWarningModal">
          <div class="glass-card rounded-2xl p-6 max-w-md w-full shadow-2xl animate-fade-up">
            <div class="flex items-center gap-3 mb-4">
              <div class="size-12 rounded-xl bg-amber-100 dark:bg-amber-900/30 flex items-center justify-center">
                <span class="material-symbols-outlined text-amber-600 dark:text-amber-400 text-2xl">warning</span>
              </div>
              <div>
                <h3 class="text-lg font-bold text-slate-900 dark:text-white">Gửi cảnh báo</h3>
                <p class="text-sm text-slate-500 dark:text-slate-400 mt-0.5">Học sinh: {{ warningModalTarget?.student }}</p>
              </div>
            </div>
            <div class="mb-4">
              <label class="block text-sm font-medium text-slate-700 dark:text-slate-300 mb-2">Nội dung cảnh báo (tùy chọn)</label>
              <textarea v-model="warningMessageInput" rows="3" class="w-full px-4 py-3 rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-slate-100 focus:ring-2 focus:ring-amber-500/50 focus:border-amber-500 transition-all" placeholder="Để trống sẽ dùng mặc định: Bạn đang bị cảnh báo bởi giám thị. Vui lòng tuân thủ quy định phòng thi."></textarea>
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
          <div class="glass-card rounded-2xl p-6 max-w-md w-full shadow-2xl animate-fade-up">
            <div class="flex items-center gap-3 mb-4">
              <div class="size-12 rounded-xl bg-rose-100 dark:bg-rose-900/30 flex items-center justify-center">
                <span class="material-symbols-outlined text-rose-600 dark:text-rose-400 text-2xl">block</span>
              </div>
              <div>
                <h3 class="text-lg font-bold text-slate-900 dark:text-white">Đình chỉ bài thi</h3>
                <p class="text-sm text-slate-500 dark:text-slate-400 mt-0.5">Học sinh: {{ invalidateModalTarget?.student }}</p>
              </div>
            </div>
            <div class="mb-4">
              <label class="block text-sm font-medium text-slate-700 dark:text-slate-300 mb-2">Lý do đình chỉ (tùy chọn)</label>
              <textarea v-model="invalidateReasonInput" rows="3" class="w-full px-4 py-3 rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-slate-100 focus:ring-2 focus:ring-rose-500/50 focus:border-rose-500 transition-all" placeholder="Để trống sẽ dùng mặc định: Bài thi đã bị đình chỉ bởi giám thị."></textarea>
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

        <!-- Bảng thí sinh — cuộn theo trang (main overflow-y-auto) -->
        <section class="stitch-ambient-shadow animate-fade-up-delay relative w-full overflow-hidden rounded-xl border border-[color:rgba(219,194,176,0.12)] bg-white shadow-sm dark:border-slate-700 dark:bg-slate-900">
          <div class="flex shrink-0 flex-col gap-3 border-b border-[color:rgba(219,194,176,0.12)] px-4 py-3 dark:border-slate-800 sm:flex-row sm:items-center sm:justify-between sm:py-3.5">
            <h3 class="flex items-center gap-2 text-sm font-bold text-slate-900 dark:text-white sm:text-base">
              <span class="material-symbols-outlined text-lg text-primary sm:text-xl">person_pin</span>
              Thí sinh trong phiên
            </h3>
            <div class="flex w-full flex-wrap items-center gap-2 sm:w-auto sm:justify-end">
              <div class="relative min-w-0 w-full sm:w-64 sm:max-w-sm">
                <span class="pointer-events-none absolute left-3 top-1/2 z-[1] -translate-y-1/2 text-primary/50">
                  <span class="material-symbols-outlined text-lg">search</span>
                </span>
                <input
                  v-model.trim="searchKeyword"
                  type="search"
                  class="stitch-registry-search w-full rounded-xl py-2.5 pl-10 pr-4 text-sm font-medium text-slate-700 outline-none transition-all focus:ring-2 focus:ring-primary/30 dark:text-slate-200"
                  placeholder="Tên, attempt, trạng thái…"
                />
              </div>
              <select v-model="filterMode" class="teacher-stitch-field teacher-stitch-select min-h-[2.75rem] min-w-[9.5rem] rounded-xl px-3 py-2 text-sm font-medium text-slate-700 dark:text-slate-200">
                <option value="ALL">Tất cả</option>
                <option value="RISK">Có rủi ro</option>
                <option value="STOPPED">Đã đình chỉ</option>
              </select>
              <select v-model="sortMode" class="teacher-stitch-field teacher-stitch-select min-h-[2.75rem] min-w-[11rem] rounded-xl px-3 py-2 text-sm font-medium text-slate-700 dark:text-slate-200">
                <option value="RISK_DESC">Rủi ro cao nhất</option>
                <option value="PROGRESS_DESC">Tiến độ cao</option>
                <option value="RECENT">Mới nhất</option>
              </select>
            </div>
          </div>
          <div class="teacher-stitch-table-scroll teacher-stitch-table-scroll--slate-head overflow-x-auto">
            <table class="w-full border-collapse text-left text-sm">
              <thead>
                <tr class="teacher-stitch-table-head border-b border-[color:rgba(219,194,176,0.15)] dark:border-slate-800">
                  <th class="px-4 py-3 text-[10px] font-bold uppercase tracking-widest text-slate-400 dark:text-slate-500 sm:px-6">Học sinh</th>
                  <th class="hidden px-4 py-3 text-[10px] font-bold uppercase tracking-widest text-slate-400 dark:text-slate-500 md:table-cell sm:px-6">Tiến độ</th>
                  <th class="px-4 py-3 text-[10px] font-bold uppercase tracking-widest text-slate-400 dark:text-slate-500 sm:px-6">Trạng thái</th>
                  <th class="hidden px-4 py-3 text-[10px] font-bold uppercase tracking-widest text-slate-400 dark:text-slate-500 lg:table-cell sm:px-6">Thiết bị</th>
                  <th class="px-4 py-3 text-right text-[10px] font-bold uppercase tracking-widest text-slate-400 dark:text-slate-500 sm:px-6">Thao tác</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-[color:rgba(219,194,176,0.12)] dark:divide-slate-800">
                <tr v-if="isSyncing && !attempts.length">
                  <td colspan="5" class="px-6 py-12 text-center">
                    <span class="material-symbols-outlined mb-2 inline-block animate-spin text-2xl text-primary">progress_activity</span>
                    <p class="text-sm font-medium text-slate-600 dark:text-slate-400">Đang tải danh sách thí sinh…</p>
                  </td>
                </tr>
                <tr v-else-if="!visibleStudents.length">
                  <td colspan="5" class="px-4 py-10 sm:px-6">
                    <div
                      class="flex flex-col items-center justify-center rounded-xl border border-dashed border-slate-200 bg-slate-50/80 px-4 py-8 text-center dark:border-slate-700 dark:bg-slate-900/40"
                    >
                      <span class="material-symbols-outlined mb-2 text-4xl text-slate-300 dark:text-slate-600">groups</span>
                      <p class="text-sm font-semibold text-slate-800 dark:text-slate-200">
                        {{ attempts.length ? 'Không khớp bộ lọc.' : 'Chưa có thí sinh.' }}
                      </p>
                    </div>
                  </td>
                </tr>
                <tr v-for="student in visibleStudents" :key="student.id" :class="student.rowClass" class="border-l-4 border-l-transparent transition-colors hover:bg-amber-50/40 dark:hover:bg-slate-800/30 group">
                  <td class="px-4 sm:px-6 py-3 sm:py-4">
                    <div class="flex items-center gap-4">
                      <div class="size-11 rounded-xl overflow-hidden shrink-0 ring-2 ring-offset-2 ring-offset-white dark:ring-offset-slate-900" :class="student.suspicious ? 'ring-red-500/50' : 'ring-slate-200 dark:ring-slate-700'">
                        <img class="w-full h-full object-cover" :src="student.image" :alt="student.name" />
                      </div>
                      <div>
                        <p class="font-semibold text-slate-900 dark:text-white">{{ student.name }}</p>
                        <p class="text-xs text-slate-500 font-mono">#{{ student.attemptId }}</p>
                      </div>
                    </div>
                  </td>
                  <td class="px-4 sm:px-6 py-3 sm:py-4 hidden md:table-cell">
                    <div class="w-full max-w-[160px]">
                      <div :class="student.progressTextClass" class="flex justify-between text-xs font-semibold mb-1.5">
                        <span>{{ student.progress }}%</span>
                        <span class="text-slate-500">{{ student.questions }}</span>
                      </div>
                      <div class="h-2 w-full bg-slate-100 dark:bg-slate-800 rounded-full overflow-hidden">
                        <div :class="student.progressBarClass" class="h-full rounded-full transition-all duration-500" :style="{ width: `${student.progress}%` }"></div>
                      </div>
                    </div>
                  </td>
                  <td class="px-4 sm:px-6 py-3 sm:py-4">
                    <span :class="student.statusClass" class="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-semibold">
                      <span v-if="student.suspicious" class="w-1.5 h-1.5 rounded-full bg-current animate-pulse"></span>
                      {{ student.status }}
                    </span>
                  </td>
                  <td class="px-4 sm:px-6 py-3 sm:py-4 hidden lg:table-cell">
                    <div class="flex gap-2">
                      <span :class="student.cameraClass" class="material-symbols-outlined text-xl p-1 rounded-lg bg-slate-100 dark:bg-slate-800" :title="student.suspicious ? 'Tắt' : 'Bật'">{{ student.cameraIcon }}</span>
                      <span :class="student.micClass" class="material-symbols-outlined text-xl p-1 rounded-lg bg-slate-100 dark:bg-slate-800">{{ student.micIcon }}</span>
                    </div>
                  </td>
                  <td class="px-4 sm:px-6 py-3 sm:py-4 text-right">
                    <div class="flex items-center justify-end gap-1">
                      <template v-if="student.statusRaw !== 'STOPPED'">
                        <button :disabled="quickActionAttemptId === student.attemptId" class="p-2 rounded-lg text-amber-600 hover:bg-amber-500/10 transition-all" type="button" title="Cảnh báo" @click="openWarningModal(student)">
                          <span class="material-symbols-outlined text-lg">warning</span>
                        </button>
                        <button :disabled="quickActionAttemptId === student.attemptId" class="p-2 rounded-lg text-rose-600 hover:bg-rose-500/10 transition-all" type="button" title="Đình chỉ" @click="openInvalidateModal(student)">
                          <span class="material-symbols-outlined text-lg">block</span>
                        </button>
                      </template>
                      <button class="p-2.5 rounded-xl text-slate-400 hover:text-primary hover:bg-primary/10 transition-all" type="button" title="Chi tiết" @click="openStudentDetail(student)">
                        <span class="material-symbols-outlined">arrow_forward</span>
                      </button>
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
          <div class="flex shrink-0 flex-col gap-2 border-t border-[color:rgba(219,194,176,0.12)] bg-slate-50/60 px-4 py-2.5 dark:border-slate-800 dark:bg-slate-800/40 sm:flex-row sm:items-center sm:justify-between sm:px-6">
            <p class="text-sm text-slate-600 dark:text-slate-400">
              <span class="font-semibold text-slate-900 dark:text-white">{{ visibleStudents.length }}</span>
              <span class="mx-1">/</span>
              <span>{{ students.length }} học sinh</span>
              <span class="ml-3 text-xs px-2 py-0.5 rounded-md" :class="isSocketConnected ? 'bg-emerald-500/20 text-emerald-700 dark:text-emerald-400' : 'bg-slate-200 dark:bg-slate-700 text-slate-600 dark:text-slate-300'">
                {{ isSocketConnected ? '● Live' : '○ Polling' }}
              </span>
            </p>
          </div>
        </section>
        </div>
      </main>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { ApiError } from '../../services/apiClient'
import { getAttemptDetail, listExamAttempts } from '../../services/attemptService'
import { invalidateAttempt, sendTeacherWarning } from '../../services/monitoringService'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { useToast } from '../../composables/useToast'
import TeacherTopHeader from './TeacherTopHeader.vue'

const route = useRoute()
const router = useRouter()

const goBackToSessionList = () => {
  router.push('/teacher/live-monitoring')
}

const isDark = ref(false)
const attempts = ref([])
const detailsByAttemptId = ref({})
const isSyncing = ref(false)
const lastUpdatedAt = ref(null)
const quickActionAttemptId = ref(null)
const quickActionType = ref('')
const toast = useToast()
const showWarningModal = ref(false)
const showInvalidateModal = ref(false)
const warningModalTarget = ref(null)
const invalidateModalTarget = ref(null)
const warningMessageInput = ref('')
const invalidateReasonInput = ref('')
let refreshTimer = null
let stompClient = null
let lastRealtimeRefreshAt = 0

const isSocketConnected = ref(false)
const filterMode = ref('ALL')
const sortMode = ref('RISK_DESC')
const searchKeyword = ref('')

const examId = computed(() => Number.parseInt(String(route.query.examId || ''), 10) || null)
const selectedExamTitle = computed(() => route.query.title || 'Đề thi đã chọn')
const selectedExamCode = computed(() => route.query.code || '')
const selectedExamMeta = computed(() => route.query.meta || 'Phiên giám sát trực tiếp')
const lastUpdatedLabel = computed(() => {
  if (!lastUpdatedAt.value) return 'chưa có dữ liệu'
  return new Date(lastUpdatedAt.value).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' })
})

const examDurationLabel = computed(() => {
  const mins = Number(route.query.durationMinutes || 0)
  if (mins > 0) return `${mins} phút`
  const match = selectedExamMeta.value.match(/Thời lượng:\s*(\d+)\s*phút/i)
  if (!match) return '-'
  return `${match[1]} phút`
})

const flaggedCount = computed(() => attempts.value.filter((attempt) => Number(attempt.riskScore || 0) > 0 || attempt.suspicious).length)

const toProgressPercent = (attempt) => {
  const detail = detailsByAttemptId.value[attempt.id]
  const answered = Number(detail?.answeredCount || 0)
  const total = Number(detail?.totalQuestions || 0)
  if (!total) return 0
  return Math.max(0, Math.min(100, Math.round((answered / total) * 100)))
}

const students = computed(() => attempts.value.map((attempt) => {
  const progress = toProgressPercent(attempt)
  const riskScore = Number(attempt.riskScore || 0)
  const suspicious = Boolean(attempt.suspicious) || riskScore > 0
  const statusRaw = String(attempt.status || '').toUpperCase()

  return {
    name: attempt.student || 'Học sinh (chưa rõ)',
    student: attempt.student || 'Học sinh (chưa rõ)',
    id: `AT-${attempt.id}`,
    studentId: attempt.studentId || attempt.userId || `AT-${attempt.id}`,
    image: 'data:image/svg+xml,%3Csvg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="%2394a3b8"%3E%3Cpath d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/%3E%3C/svg%3E',
    progress,
    questions: `${detailsByAttemptId.value[attempt.id]?.answeredCount || 0}/${detailsByAttemptId.value[attempt.id]?.totalQuestions || 0} câu`,
    status: suspicious ? 'Hoạt động đáng ngờ' : statusRaw || 'Không rõ',
    statusClass: suspicious
      ? 'bg-red-100 text-red-800 dark:bg-red-900/30 dark:text-red-400 border-red-200 dark:border-red-800'
      : 'bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-400 border-green-200 dark:border-green-800',
    progressTextClass: suspicious ? 'text-red-600' : '',
    progressBarClass: suspicious ? 'bg-red-500' : 'bg-primary',
    cameraIcon: Boolean(attempt.cameraOn) ? 'videocam' : 'videocam_off',
    micIcon: Boolean(attempt.micOn) ? 'mic' : 'mic_off',
    cameraClass: Boolean(attempt.cameraOn) ? 'text-green-500' : 'text-red-500',
    micClass: Boolean(attempt.micOn) ? 'text-green-500' : 'text-red-500',
    rowClass: suspicious ? 'bg-red-50/30 dark:bg-red-900/5' : '',
    attemptId: attempt.id,
    examId: attempt.examId,
    riskScore,
    statusRaw,
    suspicious,
    sortAt: new Date(attempt.startedAt || 0).getTime() || 0
  }
}))

const visibleStudents = computed(() => {
  const keyword = searchKeyword.value.trim().toLowerCase()
  const filtered = students.value.filter((row) => {
    const status = String(row.statusRaw || '').toUpperCase()
    const matchesKeyword = !keyword
      || row.student.toLowerCase().includes(keyword)
      || String(row.studentId || '').toLowerCase().includes(keyword)
      || String(row.attemptId || '').toLowerCase().includes(keyword)
      || String(row.status || '').toLowerCase().includes(keyword)
    if (!matchesKeyword) return false
    if (filterMode.value === 'RISK') return row.riskScore > 0 || Boolean(row.suspicious)
    if (filterMode.value === 'STOPPED') return status === 'STOPPED'
    return true
  })

  const sorted = [...filtered].sort((a, b) => {
    if (sortMode.value === 'PROGRESS_DESC') return b.progress - a.progress
    if (sortMode.value === 'RECENT') return b.sortAt - a.sortAt
    // RISK_DESC default
    if (b.riskScore !== a.riskScore) return b.riskScore - a.riskScore
    return b.sortAt - a.sortAt
  })

  return sorted
})

const openStudentDetail = (item) => {
  router.push({
    path: '/teacher/live-monitoring/student-detail',
    query: {
      attemptId: item.attemptId,
      examId: item.examId,
      student: item.student || item.name,
      studentId: item.studentId || item.id,
      exam: selectedExamTitle.value,
      avatar: item.image
    }
  })
}

const openWarningModal = (item) => {
  warningModalTarget.value = { ...item, student: item.student || item.name, attemptId: item.attemptId }
  warningMessageInput.value = ''
  showWarningModal.value = true
}

const closeWarningModal = () => {
  showWarningModal.value = false
  warningModalTarget.value = null
  warningMessageInput.value = ''
}

const confirmSendWarning = async () => {
  const item = warningModalTarget.value
  if (!item) return
  quickActionType.value = 'warning'
  quickActionAttemptId.value = item.attemptId

  try {
    await sendTeacherWarning(item.attemptId, warningMessageInput.value.trim())
    toast.success(`Đã gửi cảnh báo realtime tới ${item.student}.`)
    closeWarningModal()
    await loadAttempts()
  } catch (error) {
    toast.error(error instanceof ApiError ? error.message : 'Không gửi được cảnh báo. Vui lòng thử lại.')
  } finally {
    quickActionAttemptId.value = null
    quickActionType.value = ''
  }
}

const openInvalidateModal = (item) => {
  invalidateModalTarget.value = { ...item, student: item.student || item.name, attemptId: item.attemptId }
  invalidateReasonInput.value = ''
  showInvalidateModal.value = true
}

const closeInvalidateModal = () => {
  showInvalidateModal.value = false
  invalidateModalTarget.value = null
  invalidateReasonInput.value = ''
}

const confirmInvalidate = async () => {
  const item = invalidateModalTarget.value
  if (!item) return
  quickActionType.value = 'invalidate'
  quickActionAttemptId.value = item.attemptId

  try {
    await invalidateAttempt(item.attemptId, invalidateReasonInput.value.trim())
    toast.success(`Đã đình chỉ bài thi của ${item.student}.`)
    closeInvalidateModal()
    await loadAttempts()
  } catch (error) {
    toast.error(error instanceof ApiError ? error.message : 'Không thể đình chỉ bài thi. Vui lòng thử lại.')
  } finally {
    quickActionAttemptId.value = null
    quickActionType.value = ''
  }
}

const loadAttempts = async () => {
  if (!examId.value) {
    toast.error('Thiếu mã đề thi. Vui lòng mở phiên giám sát từ danh sách đề thi.')
    return
  }

  isSyncing.value = true
  try {
    const fetchedAttempts = await listExamAttempts(examId.value)
    const detailPairs = await Promise.all(
      fetchedAttempts.map(async (attempt) => {
        try {
          const detail = await getAttemptDetail(attempt.id)
          return [attempt.id, detail]
        } catch {
          return [attempt.id, null]
        }
      })
    )
    attempts.value = fetchedAttempts
    detailsByAttemptId.value = Object.fromEntries(detailPairs)
    lastUpdatedAt.value = Date.now()
  } catch (error) {
    toast.error(error instanceof ApiError ? error.message : 'Không thể tải dữ liệu giám sát trực tiếp.')
  } finally {
    isSyncing.value = false
  }
}

const disconnectRealtime = () => {
  if (stompClient) {
    stompClient.deactivate()
    stompClient = null
  }
  isSocketConnected.value = false
}

const getAuthToken = async () => {
  if (typeof window === 'undefined') return ''
  const { getStoredToken } = await import('../../services/authService')
  return String(getStoredToken() || '')
}

const connectRealtime = async () => {
  if (!examId.value) return

  disconnectRealtime()

  const [{ Client }, { default: SockJS }] = await Promise.all([
    import('@stomp/stompjs'),
    import('sockjs-client')
  ])

  const { API_BASE_URL } = await import('../../services/apiClient')
  const wsUrl = `${API_BASE_URL.replace(/\/$/, '')}/ws`
  const token = await getAuthToken()

  stompClient = new Client({
    reconnectDelay: 1000,
    connectHeaders: token ? { Authorization: `Bearer ${token}` } : {},
    webSocketFactory: () => new SockJS(wsUrl)
  })

  stompClient.onConnect = () => {
    isSocketConnected.value = true
    stompClient.subscribe(`/topic/exams/${examId.value}/alerts`, () => {
      const now = Date.now()
      if (now - lastRealtimeRefreshAt < 200) return
      lastRealtimeRefreshAt = now
      void loadAttempts()
    })
  }

  stompClient.onStompError = () => {
    isSocketConnected.value = false
  }

  stompClient.onWebSocketClose = () => {
    isSocketConnected.value = false
  }

  stompClient.activate()
}

onMounted(async () => {
  await loadAttempts()
  await connectRealtime()
  refreshTimer = window.setInterval(() => {
    if (!isSocketConnected.value) {
      loadAttempts()
    }
  }, 1500)
})

onUnmounted(() => {
  if (refreshTimer) {
    window.clearInterval(refreshTimer)
  }
  disconnectRealtime()
})
</script>

