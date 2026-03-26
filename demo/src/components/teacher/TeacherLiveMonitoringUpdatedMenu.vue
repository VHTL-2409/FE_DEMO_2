<template>
  <div :class="isDark ? 'dark' : 'light'" class="bg-slate-50 dark:bg-slate-950 font-display text-slate-900 dark:text-slate-100 min-h-screen">
    <div class="relative flex h-auto min-h-screen w-full flex-col overflow-x-hidden">
      <TeacherTopHeader active-section="monitoring" />

      <main class="teacher-page-shell relative mx-auto max-w-[1440px] overflow-x-hidden">
        <!-- Hero header -->
        <div class="relative flex flex-col md:flex-row md:items-end justify-between mb-5 gap-4 animate-fade-up">
          <div>
            <nav class="flex text-sm text-slate-500 dark:text-slate-400 mb-3 gap-2 items-center">
              <span class="material-symbols-outlined text-sm">monitoring</span>
              <span>/</span>
              <span>Giám sát trực tiếp</span>
              <span>/</span>
              <span class="text-indigo-600 dark:text-indigo-400 font-semibold">{{ selectedExamTitle }}</span>
            </nav>
            <h1 class="text-3xl md:text-4xl font-extrabold text-slate-900 dark:text-white tracking-tight flex items-center gap-3 flex-wrap">
              {{ selectedExamTitle }}
              <span class="inline-flex items-center gap-2 px-3 py-1.5 rounded-full text-xs font-semibold bg-emerald-500/15 text-emerald-700 dark:text-emerald-400 border border-emerald-500/30">
                <span class="w-2 h-2 rounded-full bg-emerald-500 animate-pulse"></span>
                LIVE
              </span>
            </h1>
            <div class="flex flex-wrap items-center gap-3 mt-2">
              <p class="text-slate-500 dark:text-slate-400">{{ selectedExamMeta }}</p>
              <div v-if="selectedExamCode" class="flex items-center gap-2 px-3 py-1.5 rounded-lg bg-slate-100 dark:bg-slate-700/50 border border-slate-200 dark:border-slate-600">
                <span class="text-xs font-semibold text-slate-500 dark:text-slate-400 uppercase">Mã đề thi</span>
                <code class="text-sm font-mono font-bold text-slate-900 dark:text-white">{{ selectedExamCode }}</code>
              </div>
            </div>
          </div>
          <!-- Stats cards -->
          <div class="flex gap-4 flex-wrap">
            <div class="glass-card rounded-2xl p-5 min-w-[120px] flex flex-col items-center gap-1 shadow-lg hover:shadow-xl transition-all duration-300 hover:-translate-y-0.5">
              <div class="size-10 rounded-xl bg-indigo-500/20 dark:bg-indigo-500/30 flex items-center justify-center mb-1">
                <span class="material-symbols-outlined text-indigo-600 dark:text-indigo-400 text-xl">group</span>
              </div>
              <span class="text-2xl font-bold text-slate-900 dark:text-white tabular-nums">{{ attempts.length }}</span>
              <span class="text-[10px] uppercase font-semibold text-slate-500 dark:text-slate-400 tracking-wider">Đang có mặt</span>
            </div>
            <div class="glass-card rounded-2xl p-5 min-w-[120px] flex flex-col items-center gap-1 shadow-lg hover:shadow-xl transition-all duration-300 hover:-translate-y-0.5" :class="flaggedCount > 0 ? 'gradient-card-alert ring-1 ring-red-500/20' : ''">
              <div class="size-10 rounded-xl flex items-center justify-center mb-1" :class="flaggedCount > 0 ? 'bg-red-500/20' : 'bg-slate-200/50 dark:bg-slate-700/50'">
                <span class="material-symbols-outlined text-xl" :class="flaggedCount > 0 ? 'text-red-600 dark:text-red-400' : 'text-slate-500'">flag</span>
              </div>
              <span class="text-2xl font-bold tabular-nums" :class="flaggedCount > 0 ? 'text-red-600 dark:text-red-400' : 'text-slate-900 dark:text-white'">{{ flaggedCount }}</span>
              <span class="text-[10px] uppercase font-semibold text-slate-500 dark:text-slate-400 tracking-wider">Bị gắn cờ</span>
            </div>
            <div class="gradient-card-primary rounded-2xl p-5 min-w-[120px] flex flex-col items-center gap-1 shadow-lg ring-1 ring-indigo-500/20">
              <div class="size-10 rounded-xl bg-white/30 dark:bg-white/10 flex items-center justify-center mb-1">
                <span class="material-symbols-outlined text-indigo-600 dark:text-indigo-300 text-xl">schedule</span>
              </div>
              <span class="text-2xl font-bold text-indigo-700 dark:text-indigo-300 tabular-nums">{{ examDurationLabel }}</span>
              <span class="text-[10px] uppercase font-semibold text-indigo-600/80 dark:text-indigo-400/80 tracking-wider">Thời lượng</span>
            </div>
          </div>
        </div>

        <!-- Status bar -->
        <div class="mb-6 flex flex-wrap items-center gap-3 text-sm animate-fade-up-delay">
          <span class="inline-flex items-center gap-2 px-3 py-1.5 rounded-full glass-card" :class="isSocketConnected ? 'text-emerald-600 dark:text-emerald-400' : 'text-slate-500'">
            <span class="material-symbols-outlined text-base">{{ isSocketConnected ? 'wifi' : 'wifi_off' }}</span>
            {{ isSocketConnected ? 'Realtime' : 'Polling' }}
          </span>
          <span class="text-slate-500 dark:text-slate-400">{{ lastUpdatedLabel }}</span>
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
                <p class="text-sm text-slate-500 dark:text-slate-400 mt-0.5">Sinh viên: {{ warningModalTarget?.student }}</p>
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
                <p class="text-sm text-slate-500 dark:text-slate-400 mt-0.5">Sinh viên: {{ invalidateModalTarget?.student }}</p>
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

        <!-- Students table -->
        <section class="relative glass-card rounded-2xl overflow-hidden shadow-lg animate-fade-up-delay">
          <div class="px-4 sm:px-6 py-4 sm:py-5 border-b border-slate-200 dark:border-slate-700/50 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
            <h3 class="text-lg font-bold text-slate-900 dark:text-white flex items-center gap-2">
              <span class="material-symbols-outlined text-indigo-500">person_pin</span>
              Phiên sinh viên đang hoạt động
            </h3>
            <div class="flex items-center gap-2 flex-wrap">
              <select v-model="filterMode" class="px-4 py-2.5 rounded-xl border border-slate-200 dark:border-slate-700 text-sm font-medium text-slate-700 dark:text-slate-200 bg-white dark:bg-slate-800 focus:ring-2 focus:ring-indigo-500/50 focus:border-indigo-500">
                <option value="ALL">Tất cả</option>
                <option value="RISK">Có rủi ro</option>
                <option value="STOPPED">Đã đình chỉ</option>
              </select>
              <select v-model="sortMode" class="px-4 py-2.5 rounded-xl border border-slate-200 dark:border-slate-700 text-sm font-medium text-slate-700 dark:text-slate-200 bg-white dark:bg-slate-800 focus:ring-2 focus:ring-indigo-500/50 focus:border-indigo-500">
                <option value="RISK_DESC">Risk cao nhất</option>
                <option value="PROGRESS_DESC">Tiến độ cao</option>
                <option value="RECENT">Mới nhất</option>
              </select>
            </div>
          </div>
          <div class="overflow-x-auto">
            <table class="w-full border-collapse">
              <thead>
                <tr class="bg-slate-50/80 dark:bg-slate-800/50 text-left border-b border-slate-200 dark:border-slate-700">
                  <th class="px-4 sm:px-6 py-3 sm:py-4 text-xs font-bold text-slate-500 dark:text-slate-400 uppercase tracking-wider">Sinh viên</th>
                  <th class="px-4 sm:px-6 py-3 sm:py-4 text-xs font-bold text-slate-500 dark:text-slate-400 uppercase tracking-wider hidden md:table-cell">Tiến độ</th>
                  <th class="px-4 sm:px-6 py-3 sm:py-4 text-xs font-bold text-slate-500 dark:text-slate-400 uppercase tracking-wider">Trạng thái</th>
                  <th class="px-4 sm:px-6 py-3 sm:py-4 text-xs font-bold text-slate-500 dark:text-slate-400 uppercase tracking-wider hidden lg:table-cell">Thiết bị</th>
                  <th class="px-4 sm:px-6 py-3 sm:py-4 text-xs font-bold text-slate-500 dark:text-slate-400 uppercase tracking-wider text-right">Thao tác</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-slate-100 dark:divide-slate-800/50">
                <tr v-if="isSyncing && !attempts.length">
                  <td colspan="5" class="px-6 py-12 text-center">
                    <span class="material-symbols-outlined mb-2 inline-block animate-spin text-2xl text-indigo-500">progress_activity</span>
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
                        {{ attempts.length ? 'Không có thí sinh phù hợp bộ lọc.' : 'Chưa có thí sinh nào trong phiên này.' }}
                      </p>
                      <p class="mt-1 max-w-md text-xs text-slate-500 dark:text-slate-400">
                        Khi có lượt làm bài, danh sách sẽ cập nhật tự động (Realtime hoặc làm mới định kỳ).
                      </p>
                    </div>
                  </td>
                </tr>
                <tr v-for="student in visibleStudents" :key="student.id" :class="student.rowClass" class="hover:bg-slate-50/50 dark:hover:bg-slate-800/30 transition-colors group">
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
                      <button class="p-2.5 rounded-xl text-slate-400 hover:text-indigo-600 hover:bg-indigo-500/10 transition-all" type="button" title="Chi tiết" @click="openStudentDetail(student)">
                        <span class="material-symbols-outlined">arrow_forward</span>
                      </button>
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
          <div class="px-4 sm:px-6 py-4 border-t border-slate-200 dark:border-slate-700/50 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 bg-slate-50/50 dark:bg-slate-800/30">
            <p class="text-sm text-slate-600 dark:text-slate-400">
              <span class="font-semibold text-slate-900 dark:text-white">{{ visibleStudents.length }}</span>
              <span class="mx-1">/</span>
              <span>{{ students.length }} sinh viên</span>
              <span class="ml-3 text-xs px-2 py-0.5 rounded-md" :class="isSocketConnected ? 'bg-emerald-500/20 text-emerald-700 dark:text-emerald-400' : 'bg-slate-200 dark:bg-slate-700 text-slate-600 dark:text-slate-300'">
                {{ isSocketConnected ? '● Live' : '○ Polling' }}
              </span>
            </p>
          </div>
        </section>
      </main>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { ApiError } from '../../services/apiClient'
import { getAttemptDetail, listExamAttempts } from '../../services/attemptService'
import { invalidateAttempt, sendTeacherWarning } from '../../services/monitoringService'
import { useRoute, useRouter } from 'vue-router'
import { useToast } from '../../composables/useToast'
import TeacherTopHeader from './TeacherTopHeader.vue'

const route = useRoute()
const router = useRouter()
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
    name: attempt.student || 'Sinh viên không rõ',
    student: attempt.student || 'Sinh viên không rõ',
    id: `AT-${attempt.id}`,
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
  const filtered = students.value.filter((row) => {
    const status = String(row.statusRaw || '').toUpperCase()
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

