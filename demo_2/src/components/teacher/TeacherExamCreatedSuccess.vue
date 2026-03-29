<template>
  <div :class="isDark ? 'dark' : 'light'" class="flex h-full min-h-0 flex-1 flex-col bg-background-light font-display text-slate-900 dark:bg-background-dark dark:text-slate-100">
    <div class="layout-container flex min-h-0 flex-1 grow flex-col">
      <TeacherTopHeader active-section="exam" />

      <main class="teacher-stitch-main teacher-page-shell relative mx-auto flex w-full max-w-none flex-1 justify-center px-3 py-8 sm:px-4 lg:px-5">
        <div class="flex w-full max-w-[980px] flex-col">
          <nav class="mb-6 flex flex-wrap items-center gap-2 text-xs font-medium text-slate-500">
            <RouterLink to="/teacher/dashboard" class="transition hover:text-[var(--role-primary)]">Trang chủ</RouterLink>
            <span class="material-symbols-outlined text-[14px] text-slate-400">chevron_right</span>
            <span class="font-semibold text-[var(--role-primary)]">Hoàn tất tạo đề</span>
          </nav>
          <div class="mb-8">
            <div class="flex flex-wrap items-center gap-3 text-xs font-semibold uppercase tracking-wider text-slate-500 dark:text-slate-400">
              <template v-for="(step, index) in steps" :key="step">
                <div class="flex items-center gap-3">
                  <span
                    class="size-7 rounded-full flex items-center justify-center text-[11px] font-bold"
                    :class="index + 1 <= currentStep ? 'bg-primary text-white' : 'bg-slate-200 text-slate-500 dark:bg-slate-800 dark:text-slate-400'"
                  >
                    {{ index + 1 }}
                  </span>
                  <span :class="index + 1 === currentStep ? 'text-slate-900 dark:text-white' : ''">{{ step }}</span>
                </div>
                <span v-if="index < steps.length - 1" class="h-px w-6 bg-slate-200 dark:bg-slate-700"></span>
              </template>
            </div>
          </div>

          <div class="flex flex-col items-center gap-6 mb-10">
            <div class="flex items-center justify-center w-24 h-24 rounded-full bg-emerald-100 dark:bg-emerald-900/30 text-emerald-600 dark:text-emerald-400 shadow-sm border border-emerald-200 dark:border-emerald-800">
              <span class="material-symbols-outlined !text-5xl">check_circle</span>
            </div>
            <div class="flex flex-col items-center gap-2 text-center">
              <h1 class="stitch-font-headline text-4xl font-bold tracking-tight text-amber-900 dark:text-amber-100 md:text-5xl">
                Tạo đề thi thành công!
              </h1>
            </div>
          </div>

          <div class="stitch-ambient-shadow bg-white dark:bg-slate-900 rounded-xl border border-[color:rgba(219,194,176,0.45)] dark:border-slate-700 p-8 shadow-sm overflow-hidden">
            <div class="flex flex-wrap justify-between items-start gap-4 mb-8">
              <div class="flex flex-col gap-1">
                <span class="text-xs font-bold uppercase tracking-wider text-primary">Đề thi đang mở</span>
                <h2 class="text-2xl font-bold">{{ examTitle }}</h2>
                <p class="text-slate-500 dark:text-slate-400">Mã đề thi: {{ examId }}</p>
              </div>
              <div class="flex items-center gap-2 px-3 py-1 bg-primary/10 text-primary rounded-full text-sm font-semibold">
                <span class="relative flex h-2 w-2">
                  <span class="animate-ping absolute inline-flex h-full w-full rounded-full bg-primary opacity-75"></span>
                  <span class="relative inline-flex rounded-full h-2 w-2 bg-primary"></span>
                </span>
                Đang mở &amp; nhận sinh viên
              </div>
            </div>

            <div class="relative bg-primary text-white rounded-xl p-8 flex flex-col items-center gap-4 overflow-hidden mb-8">
              <p class="text-sm font-medium uppercase tracking-[0.2em]">Mã tham gia của sinh viên</p>
              <span class="text-4xl md:text-5xl font-black tracking-widest font-mono">{{ joinCode }}</span>
              <button class="mt-2 flex items-center gap-2 px-6 py-2 bg-white/10 hover:bg-white/20 transition-colors border border-white/20 rounded-lg font-bold text-sm" type="button" @click="copyJoinCode">
                <span class="material-symbols-outlined text-sm">content_copy</span>
                Sao chép mã đề thi
              </button>
            </div>

            <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 mb-8">
              <div class="rounded-xl border border-slate-200 dark:border-slate-800 bg-slate-50 dark:bg-slate-800/40 p-4">
                <p class="text-xs uppercase text-slate-500 mb-1">Thời lượng</p>
                <p class="font-bold text-lg">{{ durationDisplay }}</p>
              </div>
              <div class="rounded-xl border border-slate-200 dark:border-slate-800 bg-slate-50 dark:bg-slate-800/40 p-4">
                <p class="text-xs uppercase text-slate-500 mb-1">Thời gian bắt đầu</p>
                <p class="font-bold text-sm">{{ startAtDisplay }}</p>
              </div>
              <div class="rounded-xl border border-slate-200 dark:border-slate-800 bg-slate-50 dark:bg-slate-800/40 p-4">
                <p class="text-xs uppercase text-slate-500 mb-1">Thời gian kết thúc</p>
                <p class="font-bold text-sm">{{ endAtDisplay }}</p>
              </div>
              <div class="rounded-xl border border-primary/30 bg-primary/10 p-4">
                <p class="text-xs uppercase text-primary mb-1">Đếm ngược đến giờ bắt đầu</p>
                <p class="font-black text-xl text-primary">{{ countdownLabel }}</p>
              </div>
            </div>

            <div class="rounded-xl border border-slate-200 dark:border-slate-800 overflow-hidden">
              <div class="px-5 py-4 bg-slate-50 dark:bg-slate-800/50 border-b border-slate-200 dark:border-slate-800 flex items-center justify-between">
                <h3 class="font-bold">Sinh viên trong phòng chờ</h3>
                <div class="flex items-center gap-3 text-xs">
                  <span class="inline-flex items-center gap-1.5 px-2 py-1 rounded-full border border-slate-200 dark:border-slate-700 text-slate-600 dark:text-slate-300 bg-white/80 dark:bg-slate-900/70">
                    <span :class="isRefreshing ? 'bg-amber-500' : 'bg-emerald-500'" class="size-2 rounded-full"></span>
                    {{ isRefreshing ? 'Đang đồng bộ...' : 'Đồng bộ ổn định' }}
                  </span>
                  <span class="text-slate-500">{{ joinedStudents.length }} sinh viên</span>
                </div>
              </div>

              <div class="teacher-stitch-table-scroll">
                <table class="w-full border-collapse">
                  <thead>
                    <tr class="teacher-stitch-table-head border-b border-[color:rgba(219,194,176,0.4)] dark:border-slate-800 text-left">
                      <th class="px-5 py-3 text-xs uppercase text-slate-500">Sinh viên</th>
                      <th class="px-5 py-3 text-xs uppercase text-slate-500">Trạng thái</th>
                      <th class="px-5 py-3 text-xs uppercase text-slate-500">Thời điểm vào</th>
                      <th class="px-5 py-3 text-xs uppercase text-slate-500">Điểm</th>
                    </tr>
                  </thead>
                  <tbody class="divide-y divide-slate-100 dark:divide-slate-800">
                    <tr v-if="isLoading">
                      <td colspan="4" class="px-5 py-4 text-sm text-slate-500">Đang tải danh sách sinh viên đã vào...</td>
                    </tr>
                    <tr v-else-if="joinedStudents.length === 0">
                      <td colspan="4" class="px-5 py-4 text-sm text-slate-500">Chưa có sinh viên nào tham gia.</td>
                    </tr>
                    <tr v-for="student in joinedStudents" :key="student.id" class="hover:bg-slate-50 dark:hover:bg-slate-800/30 transition-colors">
                      <td class="px-5 py-3 font-semibold">{{ student.name }}</td>
                      <td class="px-5 py-3">
                        <span class="px-2 py-1 rounded text-xs font-bold" :class="student.statusClass">{{ student.status }}</span>
                      </td>
                      <td class="px-5 py-3 text-sm text-slate-600 dark:text-slate-300">{{ student.joinedAt }}</td>
                      <td class="px-5 py-3 text-sm text-slate-600 dark:text-slate-300">{{ student.score }} / 10</td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>

            <p class="text-xs text-slate-500 mt-4">Cập nhật gần nhất: {{ lastUpdatedLabel }}</p>
          </div>

          <div class="mt-10 grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            <button class="flex items-center justify-center gap-3 px-6 py-4 bg-primary text-white rounded-xl font-bold text-lg hover:opacity-90 transition-opacity shadow-lg shadow-primary/20" @click="goToMonitoring" type="button">
              <span class="material-symbols-outlined">monitoring</span>
              Đi đến giám sát trực tiếp
            </button>
            <button v-if="canEditExam" class="flex items-center justify-center gap-3 px-6 py-4 bg-emerald-600 text-white rounded-xl font-bold text-lg hover:bg-emerald-700 transition-colors" @click="goToEdit" type="button">
              <span class="material-symbols-outlined">edit</span>
              Chỉnh sửa đề thi
            </button>
            <button class="flex items-center justify-center gap-3 px-6 py-4 bg-white dark:bg-slate-900 text-slate-900 dark:text-slate-100 border border-slate-200 dark:border-slate-800 rounded-xl font-bold text-lg hover:bg-slate-50 dark:hover:bg-slate-800 transition-colors" @click="goCreateAnother" type="button">
              <span class="material-symbols-outlined">add_circle</span>
              Tạo đề thi khác
            </button>
          </div>
        </div>
      </main>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { listExamAttempts } from '../../services/attemptService'
import { useToast } from '../../composables/useToast'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import TeacherTopHeader from './TeacherTopHeader.vue'

const route = useRoute()
const router = useRouter()
const isDark = ref(false)
const isLoading = ref(false)
const isRefreshing = ref(false)
const steps = ['Chọn cách tạo', 'Nhập đề', 'Lập lịch', 'Hoàn tất']
const currentStep = 4
const attempts = ref([])

const toast = useToast()
const nowMs = ref(Date.now())
const lastUpdatedAt = ref(null)
let countdownTimer = null
let refreshTimer = null

const examId = computed(() => {
  const q = route.query.examId
  const raw = Array.isArray(q) ? q[0] : q
  const n = Number.parseInt(String(raw || ''), 10)
  return Number.isFinite(n) && n > 0 ? n : null
})
const examTitle = computed(() => route.query.title || 'Đề thi')
const joinCode = computed(() => String(route.query.code || examId.value || 'N/A'))
const durationMinutes = computed(() => Number.parseInt(String(route.query.durationMinutes || ''), 10) || 0)
const startAtDate = computed(() => {
  const raw = String(route.query.startAt || '')
  if (!raw) return null
  const date = new Date(raw)
  return Number.isNaN(date.getTime()) ? null : date
})
const endAtDate = computed(() => {
  const raw = String(route.query.endAt || '')
  if (!raw) return null
  const date = new Date(raw)
  return Number.isNaN(date.getTime()) ? null : date
})

const durationDisplay = computed(() => durationMinutes.value > 0 ? `${durationMinutes.value} phút` : '-')
const startAtDisplay = computed(() => startAtDate.value ? startAtDate.value.toLocaleString() : '-')
const endAtDisplay = computed(() => endAtDate.value ? endAtDate.value.toLocaleString() : '-')
const canEditExam = computed(() => {
  if (!startAtDate.value) return true
  return nowMs.value < startAtDate.value.getTime()
})

const countdownLabel = computed(() => {
  if (!startAtDate.value) return '-'

  const diffMs = startAtDate.value.getTime() - nowMs.value
  if (diffMs <= 0) return 'Đã bắt đầu'

  const totalSeconds = Math.floor(diffMs / 1000)
  const hours = Math.floor(totalSeconds / 3600)
  const minutes = Math.floor((totalSeconds % 3600) / 60)
  const seconds = totalSeconds % 60

  return `${String(hours).padStart(2, '0')}:${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`
})

const lastUpdatedLabel = computed(() => {
  if (!lastUpdatedAt.value) return 'chưa có dữ liệu'
  return new Date(lastUpdatedAt.value).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' })
})

/** Điểm BE có thể 0–10 hoặc 0–100 — hiển thị thang /10 */
const formatAttemptScoreDisplay = (raw) => {
  if (raw == null || raw === '') return '-'
  const n = Number(raw)
  if (Number.isNaN(n)) return '-'
  const on10 = n > 10 ? n / 10 : n
  return on10.toFixed(1)
}

const statusLabelMap = {
  IN_PROGRESS: 'Đang làm bài',
  SUBMITTED: 'Đã nộp',
  AUTO_SUBMITTED: 'Đã nộp',
  STOPPED: 'Đã dừng'
}

const joinedStudents = computed(() => attempts.value.map((attempt) => {
  const statusKey = attempt.status || 'IN_PROGRESS'
  const isSubmitted = statusKey === 'SUBMITTED' || statusKey === 'AUTO_SUBMITTED'
  return {
    id: attempt.id,
    name: attempt.student || 'Không rõ',
    status: statusLabelMap[statusKey] || statusKey,
    statusClass: isSubmitted
      ? 'bg-emerald-100 text-emerald-700 dark:bg-emerald-900/30 dark:text-emerald-400'
      : statusKey === 'STOPPED'
        ? 'bg-slate-100 text-slate-700 dark:bg-slate-800 dark:text-slate-300'
        : 'bg-primary/10 text-primary dark:bg-primary/20 dark:text-amber-200',
    joinedAt: attempt.startedAt ? new Date(attempt.startedAt).toLocaleString() : '-',
    score: formatAttemptScoreDisplay(attempt.score)
  }
}))

const loadStudents = async ({ silent = false } = {}) => {
  if (!examId.value) return

  if (silent) {
    isRefreshing.value = true
  } else {
    isLoading.value = true
  }

  try {
    const fetched = await listExamAttempts(examId.value)
    attempts.value = fetched
    lastUpdatedAt.value = Date.now()
  } catch (error) {
    toast.error('Không thể tải danh sách sinh viên đã vào.')
  } finally {
    isLoading.value = false
    isRefreshing.value = false
  }
}

const copyJoinCode = async () => {
  try {
    await navigator.clipboard.writeText(joinCode.value)
  } catch {
    // ignore clipboard errors
  }
}

const goToMonitoring = () => {
  const meta = `Bắt đầu: ${startAtDisplay.value} • Kết thúc: ${endAtDisplay.value}`
  router.push({
    path: '/teacher/live-monitoring/session',
    query: {
      title: examTitle.value,
      examId: examId.value,
      code: route.query.code || '',
      meta,
      durationMinutes: durationMinutes.value || ''
    }
  })
}

const goToEdit = () => {
  router.push({
    path: '/teacher/exams/schedule',
    query: {
      examId: examId.value,
      title: examTitle.value,
      durationMinutes: durationMinutes.value,
      startAt: route.query.startAt,
      endAt: route.query.endAt,
      mode: 'edit'
    }
  })
}

const goCreateAnother = () => {
  router.push('/teacher/exams/create')
}

onMounted(async () => {
  await loadStudents()
  countdownTimer = window.setInterval(() => {
    nowMs.value = Date.now()
  }, 1000)
  refreshTimer = window.setInterval(() => {
    loadStudents({ silent: true })
  }, 5000)
})

onUnmounted(() => {
  if (countdownTimer) {
    window.clearInterval(countdownTimer)
  }
  if (refreshTimer) {
    window.clearInterval(refreshTimer)
  }
})
</script>

<style scoped>
.font-display {
  font-family: 'Inter', sans-serif;
}
</style>
