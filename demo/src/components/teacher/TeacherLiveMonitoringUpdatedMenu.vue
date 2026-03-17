<template>
  <div :class="isDark ? 'dark' : 'light'" class="bg-slate-50 dark:bg-slate-950 font-display text-slate-900 dark:text-slate-100 min-h-screen">
    <div class="relative flex h-auto min-h-screen w-full flex-col overflow-x-hidden">
      <TeacherTopHeader active-section="monitoring" />

      <main class="teacher-page-shell max-w-[1440px]">
        <div class="pointer-events-none absolute -top-16 -left-16 size-72 rounded-full bg-indigo-500/20 blur-3xl animate-float-slow"></div>
        <div class="pointer-events-none absolute -bottom-24 -right-20 size-80 rounded-full bg-violet-500/15 blur-3xl animate-float-delay"></div>

        <!-- Hero header -->
        <div class="relative flex flex-col md:flex-row md:items-end justify-between mb-8 gap-6 animate-fade-up">
          <div>
            <nav class="flex text-sm text-slate-500 dark:text-slate-400 mb-3 gap-2 items-center">
              <span class="material-symbols-outlined text-sm">monitoring</span>
              <span>/</span>
              <span>Giám sát trực tiếp</span>
              <span>/</span>
              <span class="text-indigo-600 dark:text-indigo-400 font-semibold">{{ selectedExamTitle }}</span>
            </nav>
            <h1 class="text-3xl md:text-4xl font-extrabold text-slate-900 dark:text-white tracking-tight flex items-center gap-3 flex-wrap">
              Live Monitoring
              <span class="inline-flex items-center gap-2 px-3 py-1.5 rounded-full text-xs font-semibold bg-emerald-500/15 text-emerald-700 dark:text-emerald-400 border border-emerald-500/30">
                <span class="w-2 h-2 rounded-full bg-emerald-500 animate-pulse"></span>
                LIVE
              </span>
            </h1>
            <p class="text-slate-500 dark:text-slate-400 mt-2">{{ selectedExamMeta }}</p>
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
          <span class="inline-flex items-center gap-2 px-3 py-1.5 rounded-full glass-card text-slate-600 dark:text-slate-300">
            <span :class="isSyncing ? 'bg-amber-500 animate-pulse' : 'bg-emerald-500'" class="size-2.5 rounded-full"></span>
            {{ isSyncing ? 'Đang đồng bộ...' : 'Đồng bộ ổn định' }}
          </span>
          <span class="inline-flex items-center gap-2 px-3 py-1.5 rounded-full glass-card" :class="isSocketConnected ? 'text-emerald-600 dark:text-emerald-400' : 'text-slate-500'">
            <span class="material-symbols-outlined text-base">{{ isSocketConnected ? 'wifi' : 'wifi_off' }}</span>
            {{ isSocketConnected ? 'Realtime' : 'Polling' }}
          </span>
          <span class="text-slate-500 dark:text-slate-400">Cập nhật: {{ lastUpdatedLabel }}</span>
        </div>

        <!-- Alerts section -->
        <section class="relative mb-8 animate-fade-up-delay">
          <div class="flex items-center justify-between mb-4">
            <h3 class="text-slate-900 dark:text-white text-base font-bold flex items-center gap-2">
              <span class="material-symbols-outlined text-red-500 text-xl">notifications_active</span>
              Cảnh báo hành vi bất thường
            </h3>
            <span class="text-xs text-slate-500 bg-slate-100 dark:bg-slate-800 px-2.5 py-1 rounded-lg">{{ alerts.length }}/5</span>
          </div>
          <div class="glass-card rounded-2xl overflow-hidden shadow-lg">
            <div v-if="alerts.length === 0" class="p-8 text-center text-slate-500 dark:text-slate-400">
              <span class="material-symbols-outlined text-4xl text-slate-300 dark:text-slate-600 mb-2 block">check_circle</span>
              <p class="font-medium">Chưa có cảnh báo cần xử lý</p>
            </div>
            <div v-for="alert in alerts" :key="alert.key" class="p-5 border-b border-slate-100 dark:border-slate-800/50 last:border-b-0 hover:bg-slate-50/50 dark:hover:bg-slate-800/30 transition-colors">
              <div class="flex items-start justify-between gap-4 flex-wrap">
                <div class="flex-1 min-w-0">
                  <div class="flex items-center gap-2 mb-2 flex-wrap">
                    <span :class="alert.badgeClass" class="px-2.5 py-1 rounded-lg text-[11px] font-bold uppercase tracking-wide">{{ alert.badge }}</span>
                    <span class="text-xs text-slate-500 font-medium">{{ alert.timeLabel }}</span>
                  </div>
                  <p class="font-semibold text-slate-900 dark:text-slate-100">{{ alert.student }}</p>
                  <p class="text-sm text-slate-600 dark:text-slate-400 mt-1">{{ alert.message }}</p>
                </div>
                <div class="flex items-center gap-2 shrink-0">
                  <button :disabled="!alert.canWarn || quickActionAttemptId === alert.attemptId" class="px-4 py-2.5 rounded-xl bg-amber-500 hover:bg-amber-600 text-white text-xs font-bold disabled:opacity-50 transition-all flex items-center gap-1.5" type="button" @click="sendQuickWarning(alert)">
                    <span class="material-symbols-outlined text-sm">warning</span>
                    {{ quickActionAttemptId === alert.attemptId && quickActionType === 'warning' ? 'Đang gửi...' : 'Cảnh báo' }}
                  </button>
                  <button :disabled="!alert.canStop || quickActionAttemptId === alert.attemptId" class="px-4 py-2.5 rounded-xl bg-rose-600 hover:bg-rose-700 text-white text-xs font-bold disabled:opacity-50 transition-all flex items-center gap-1.5" type="button" @click="handleQuickInvalidate(alert)">
                    <span class="material-symbols-outlined text-sm">block</span>
                    {{ quickActionAttemptId === alert.attemptId && quickActionType === 'invalidate' ? 'Đang xử lý...' : 'Đình chỉ' }}
                  </button>
                </div>
              </div>
            </div>
          </div>
        </section>

        <!-- Students table -->
        <section class="relative glass-card rounded-2xl overflow-hidden shadow-lg animate-fade-up-delay">
          <div class="px-6 py-5 border-b border-slate-200 dark:border-slate-700/50 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
            <h3 class="text-lg font-bold text-slate-900 dark:text-white flex items-center gap-2">
              <span class="material-symbols-outlined text-indigo-500">person_pin</span>
              Phiên sinh viên đang hoạt động
            </h3>
            <div class="flex items-center gap-2 flex-wrap">
              <select v-model="filterMode" class="px-4 py-2.5 rounded-xl border border-slate-200 dark:border-slate-700 text-sm font-medium text-slate-700 dark:text-slate-200 bg-white dark:bg-slate-800 focus:ring-2 focus:ring-indigo-500/50 focus:border-indigo-500">
                <option value="ALL">Tất cả</option>
                <option value="FLAGGED">Có rủi ro</option>
                <option value="SUSPICIOUS">Bị gắn cờ</option>
                <option value="STOPPED">Đã đình chỉ</option>
              </select>
              <select v-model="sortMode" class="px-4 py-2.5 rounded-xl border border-slate-200 dark:border-slate-700 text-sm font-medium text-slate-700 dark:text-slate-200 bg-white dark:bg-slate-800 focus:ring-2 focus:ring-indigo-500/50 focus:border-indigo-500">
                <option value="RISK_DESC">Risk cao nhất</option>
                <option value="PROGRESS_ASC">Tiến độ thấp</option>
                <option value="PROGRESS_DESC">Tiến độ cao</option>
                <option value="RECENT">Mới nhất</option>
              </select>
            </div>
          </div>
          <div class="overflow-x-auto">
            <table class="w-full border-collapse">
              <thead>
                <tr class="bg-slate-50/80 dark:bg-slate-800/50 text-left border-b border-slate-200 dark:border-slate-700">
                  <th class="px-6 py-4 text-xs font-bold text-slate-500 dark:text-slate-400 uppercase tracking-wider">Sinh viên</th>
                  <th class="px-6 py-4 text-xs font-bold text-slate-500 dark:text-slate-400 uppercase tracking-wider">Tiến độ</th>
                  <th class="px-6 py-4 text-xs font-bold text-slate-500 dark:text-slate-400 uppercase tracking-wider">Trạng thái</th>
                  <th class="px-6 py-4 text-xs font-bold text-slate-500 dark:text-slate-400 uppercase tracking-wider">Thiết bị</th>
                  <th class="px-6 py-4 text-xs font-bold text-slate-500 dark:text-slate-400 uppercase tracking-wider text-right">Thao tác</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-slate-100 dark:divide-slate-800/50">
                <tr v-for="student in visibleStudents" :key="student.id" :class="student.rowClass" class="hover:bg-slate-50/50 dark:hover:bg-slate-800/30 transition-colors group">
                  <td class="px-6 py-4">
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
                  <td class="px-6 py-4">
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
                  <td class="px-6 py-4">
                    <span :class="student.statusClass" class="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-semibold">
                      <span v-if="student.suspicious" class="w-1.5 h-1.5 rounded-full bg-current animate-pulse"></span>
                      {{ student.status }}
                    </span>
                  </td>
                  <td class="px-6 py-4">
                    <div class="flex gap-2">
                      <span :class="student.cameraClass" class="material-symbols-outlined text-xl p-1 rounded-lg bg-slate-100 dark:bg-slate-800" :title="student.suspicious ? 'Tắt' : 'Bật'">{{ student.cameraIcon }}</span>
                      <span :class="student.micClass" class="material-symbols-outlined text-xl p-1 rounded-lg bg-slate-100 dark:bg-slate-800">{{ student.micIcon }}</span>
                    </div>
                  </td>
                  <td class="px-6 py-4 text-right">
                    <button class="p-2.5 rounded-xl text-slate-400 hover:text-indigo-600 hover:bg-indigo-500/10 dark:hover:bg-indigo-500/20 transition-all group-hover:bg-slate-100 dark:group-hover:bg-slate-800/50" type="button" @click="openStudentDetail(student)">
                      <span class="material-symbols-outlined">arrow_forward</span>
                    </button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
          <div class="px-6 py-4 border-t border-slate-200 dark:border-slate-700/50 flex items-center justify-between bg-slate-50/50 dark:bg-slate-800/30">
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
let refreshTimer = null
let stompClient = null
let lastRealtimeRefreshAt = 0

const isSocketConnected = ref(false)
const filterMode = ref('ALL')
const sortMode = ref('RISK_DESC')

const examId = computed(() => Number.parseInt(String(route.query.examId || ''), 10) || null)
const selectedExamTitle = computed(() => route.query.title || 'Đề thi đã chọn')
const selectedExamMeta = computed(() => route.query.meta || 'Phiên giám sát trực tiếp')
const lastUpdatedLabel = computed(() => {
  if (!lastUpdatedAt.value) return 'chưa có dữ liệu'
  return new Date(lastUpdatedAt.value).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' })
})

const examDurationLabel = computed(() => {
  const match = selectedExamMeta.value.match(/Thời lượng:\s*(\d+)\s*phút/i)
  if (!match) return '-'
  return `${match[1]}m`
})

const flaggedCount = computed(() => attempts.value.filter((attempt) => Number(attempt.riskScore || 0) > 0 || attempt.suspicious).length)

const alerts = computed(() => attempts.value
  .filter((attempt) => Number(attempt.riskScore || 0) > 0 || attempt.suspicious || String(attempt.status || '').toUpperCase() === 'STOPPED')
  .sort((a, b) => {
    const aAt = new Date(a.startedAt || 0).getTime()
    const bAt = new Date(b.startedAt || 0).getTime()
    return bAt - aAt
  })
  .slice(0, 5)
  .map((attempt, index) => {
    const riskScore = Number(attempt.riskScore || 0)
    const status = String(attempt.status || '').toUpperCase()
    const critical = riskScore >= 10 || Boolean(attempt.suspicious)
    const sortAt = new Date(attempt.updatedAt || attempt.startedAt || 0).getTime() || 0

    let message = `Phát hiện dấu hiệu bất thường (risk ${riskScore}).`
    if (status === 'STOPPED') {
      message = 'Bài thi đã bị đình chỉ bởi giám thị.'
    } else if (critical) {
      message = `Mức rủi ro cao (${riskScore}) cần can thiệp ngay.`
    }

    return {
      key: `${attempt.id}-${sortAt}-${index}`,
      student: attempt.student || 'Sinh viên không rõ',
      studentId: `AT-${attempt.id}`,
      message,
      timeLabel: sortAt ? new Date(sortAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' }) : '-',
      badge: status === 'STOPPED' ? 'Đã đình chỉ' : (critical ? 'Mức cao' : 'Cảnh báo'),
      badgeClass: status === 'STOPPED'
        ? 'bg-slate-200 text-slate-700 dark:bg-slate-700 dark:text-slate-200'
        : (critical ? 'bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400' : 'bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-400'),
      canWarn: status !== 'STOPPED',
      canStop: status !== 'STOPPED',
      image: 'https://lh3.googleusercontent.com/aida-public/AB6AXuAIJ4SxrDKzIOAxYgwMxfCl9VECA94Lz5URlQAHT8wlNhUW4MNw9D1HLca97xsa6unSv5ggAypK-LlUtn8ncy9v-bzAFa3_8M1DavSb0bTFXN0n5cGflRifrcKtMGsdiwoybTbp9NCUsP7loGo43vS15PYo7KCItvkic1tbE7vNg_JatXHLLpScEs05NVeQLNXTuq294VTZO_Ynq_xP5jG7khjmSKYRitHROXp4_84cfjODRxtCxXX59LP1gTMP-0pkrK6iKTNdhA',
      attemptId: attempt.id,
      examId: attempt.examId
    }
  }))

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
    id: `AT-${attempt.id}`,
    image: 'https://lh3.googleusercontent.com/aida-public/AB6AXuDJGLtVqDWiqr09yUYr_EKVfQ-r70N3UjRMOqeWxaAprZiU6nZJws9kNPNFFMbWG7ZDXumDSi6Hwc8iSgpka-HkHG6-1322BcZsj7rwrYoGIObyDUwZyOPmezxi1A3CpRdGtoJ0XgFtcTcOsQC6CRy6biwHpDD6ilY1MrxbvsGzNQJDoyihPA6hliXu7tW2LoEHLgyAKrOFRpsV1hASERYghA7RP_UJXATXVNYnPO9qX-jkyA0ZHMxQdOZJoD60dT9lhxX518nKBg',
    progress,
    questions: `${detailsByAttemptId.value[attempt.id]?.answeredCount || 0}/${detailsByAttemptId.value[attempt.id]?.totalQuestions || 0} câu`,
    status: suspicious ? 'Hoạt động đáng ngờ' : statusRaw || 'Không rõ',
    statusClass: suspicious
      ? 'bg-red-100 text-red-800 dark:bg-red-900/30 dark:text-red-400 border-red-200 dark:border-red-800'
      : 'bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-400 border-green-200 dark:border-green-800',
    progressTextClass: suspicious ? 'text-red-600' : '',
    progressBarClass: suspicious ? 'bg-red-500' : 'bg-primary',
    cameraIcon: suspicious ? 'videocam_off' : 'videocam',
    micIcon: suspicious ? 'mic_off' : 'mic',
    cameraClass: suspicious ? 'text-red-500' : 'text-green-500',
    micClass: suspicious ? 'text-red-500' : 'text-green-500',
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
    if (filterMode.value === 'FLAGGED') return row.riskScore > 0
    if (filterMode.value === 'SUSPICIOUS') return Boolean(row.suspicious)
    if (filterMode.value === 'STOPPED') return status === 'STOPPED'
    return true
  })

  const sorted = [...filtered].sort((a, b) => {
    if (sortMode.value === 'PROGRESS_ASC') return a.progress - b.progress
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

const sendQuickWarning = async (item) => {
  quickActionType.value = 'warning'
  quickActionAttemptId.value = item.attemptId

  try {
    const response = await sendTeacherWarning(item.attemptId)
    toast.success(response?.message || `Đã gửi cảnh báo realtime tới ${item.student}.`)
    await loadAttempts()
  } catch (error) {
    toast.error(error instanceof ApiError ? error.message : 'Không gửi được cảnh báo. Vui lòng thử lại.')
  } finally {
    quickActionAttemptId.value = null
    quickActionType.value = ''
  }
}

const handleQuickInvalidate = async (item) => {
  quickActionType.value = 'invalidate'
  quickActionAttemptId.value = item.attemptId

  try {
    const response = await invalidateAttempt(item.attemptId)
    toast.success(response?.message || `Đã đình chỉ bài thi của ${item.student}.`)
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
    reconnectDelay: 3000,
    connectHeaders: token ? { Authorization: `Bearer ${token}` } : {},
    webSocketFactory: () => new SockJS(wsUrl)
  })

  stompClient.onConnect = () => {
    isSocketConnected.value = true
    stompClient.subscribe(`/topic/exams/${examId.value}/alerts`, () => {
      // Throttle: tránh spam reload khi có nhiều event dồn
      const now = Date.now()
      if (now - lastRealtimeRefreshAt < 800) return
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
  }, 5000)
})

onUnmounted(() => {
  if (refreshTimer) {
    window.clearInterval(refreshTimer)
  }
  disconnectRealtime()
})
</script>

