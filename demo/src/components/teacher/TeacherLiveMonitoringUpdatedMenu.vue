<template>
  <div :class="isDark ? 'dark' : 'light'" class="bg-background-light dark:bg-background-dark font-display text-slate-900 dark:text-slate-100 min-h-screen">
    <div class="relative flex h-auto min-h-screen w-full flex-col overflow-x-hidden">
      <TeacherTopHeader active-section="monitoring" />

      <main class="relative flex-1 max-w-[1440px] mx-auto w-full p-4 md:p-8 overflow-hidden">
        <div class="pointer-events-none absolute -top-16 -left-16 size-72 rounded-full bg-primary/15 blur-3xl animate-float-slow"></div>
        <div class="pointer-events-none absolute -bottom-24 -right-20 size-80 rounded-full bg-primary/10 blur-3xl animate-float-delay"></div>

        <div class="relative flex flex-col md:flex-row md:items-end justify-between mb-8 gap-4 animate-fade-up">
          <div>
            <nav class="flex text-sm text-slate-500 dark:text-slate-400 mb-2 gap-2 items-center">
              <span class="material-symbols-outlined text-sm">home</span>
              <span>/</span>
              <span>Giám sát</span>
              <span>/</span>
              <span class="text-primary font-medium">{{ selectedExamTitle }}</span>
            </nav>
            <h1 class="text-3xl font-bold text-slate-900 dark:text-white flex items-center gap-3">
              Giám sát phiên trực tiếp
              <span class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-400">
                <span class="w-2 h-2 rounded-full bg-green-500 mr-1.5 animate-pulse"></span>
                Đang hoạt động
              </span>
            </h1>
            <p class="text-slate-500 dark:text-slate-400 mt-1">{{ selectedExamMeta }}</p>
          </div>
          <div class="flex gap-3">
            <div class="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-xl p-3 flex flex-col items-center min-w-[100px] hover:-translate-y-0.5 hover:shadow-md transition-all duration-200">
              <span class="text-2xl font-bold text-slate-900 dark:text-white">{{ attempts.length }}</span>
              <span class="text-[10px] uppercase font-bold text-slate-500 tracking-wider">Đang có mặt</span>
            </div>
            <div class="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-xl p-3 flex flex-col items-center min-w-[100px] hover:-translate-y-0.5 hover:shadow-md transition-all duration-200">
              <span class="text-2xl font-bold text-red-600">{{ flaggedCount }}</span>
              <span class="text-[10px] uppercase font-bold text-slate-500 tracking-wider">Bị gắn cờ</span>
            </div>
            <div class="bg-primary text-white rounded-xl p-3 flex flex-col items-center min-w-[100px]">
              <span class="text-2xl font-bold">{{ examDurationLabel }}</span>
              <span class="text-[10px] uppercase font-bold text-white/70 tracking-wider">Thời lượng</span>
            </div>
          </div>
        </div>

        <div class="mb-4 flex flex-wrap items-center gap-3 text-xs animate-fade-up-delay">
          <span class="inline-flex items-center gap-1.5 px-2 py-1 rounded-full border border-slate-200 dark:border-slate-700 text-slate-600 dark:text-slate-300 bg-white/80 dark:bg-slate-900/70">
            <span :class="isSyncing ? 'bg-amber-500' : 'bg-emerald-500'" class="size-2 rounded-full"></span>
            {{ isSyncing ? 'Đang đồng bộ...' : 'Đồng bộ ổn định' }}
          </span>
          <span class="text-slate-500 dark:text-slate-400">Cập nhật gần nhất: {{ lastUpdatedLabel }}</span>
        </div>
        <p v-if="loadError" class="mb-4 text-sm text-rose-600 animate-fade-up-delay">{{ loadError }}</p>

        <section class="relative mb-10 animate-fade-up-delay">
          <h3 class="text-red-600 dark:text-red-400 text-sm font-bold uppercase tracking-widest mb-4 flex items-center gap-2">
            <span class="material-symbols-outlined text-lg">warning</span>
            Cần chú ý ngay
          </h3>
          <div class="grid grid-cols-1 lg:grid-cols-3 gap-4">
            <div v-for="alert in alerts" :key="alert.key" :class="alert.cardClass" class="rounded-xl p-5 shadow-lg relative overflow-hidden group hover:-translate-y-0.5 hover:shadow-xl transition-all duration-200">
              <div v-if="alert.isCritical" class="absolute top-0 right-0 p-2">
                <span class="flex h-3 w-3 rounded-full bg-red-500 animate-ping"></span>
              </div>
              <div class="flex gap-4 items-start">
                <div :class="alert.avatarClass" class="h-16 w-16 rounded-lg overflow-hidden shrink-0 shadow-sm relative border-2">
                  <img class="w-full h-full object-cover" :src="alert.image" :alt="alert.student" />
                  <div :class="alert.feedClass" class="absolute bottom-0 inset-x-0 text-white text-[8px] text-center font-bold py-0.5 uppercase">Luồng trực tiếp</div>
                </div>
                <div class="flex-1">
                  <h4 class="font-bold text-slate-900 dark:text-white text-lg leading-none">{{ alert.student }}</h4>
                  <p :class="alert.titleClass" class="text-sm font-semibold mt-1">{{ alert.title }}</p>
                  <p class="text-xs text-slate-500 dark:text-slate-400 mt-1">{{ alert.meta }}</p>
                </div>
              </div>
              <div class="mt-4 flex gap-2">
                <button :class="alert.primaryActionClass" class="flex-1 py-2 text-white rounded-lg text-sm font-bold transition-all" type="button" @click="handlePrimaryAction(alert)">{{ alert.primaryAction }}</button>
                <button class="px-3 py-2 bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 text-slate-700 dark:text-slate-200 rounded-lg text-sm hover:bg-slate-50 transition-all flex items-center justify-center" type="button" @click="openStudentDetail(alert)">
                  <span class="material-symbols-outlined text-lg">videocam</span>
                </button>
              </div>
            </div>
          </div>
        </section>

        <section class="relative bg-white dark:bg-slate-900 rounded-2xl border border-slate-200 dark:border-slate-800 overflow-hidden animate-fade-up-delay">
          <div class="px-6 py-4 border-b border-slate-200 dark:border-slate-800 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
            <h3 class="text-lg font-bold text-slate-900 dark:text-white">Phiên sinh viên đang hoạt động</h3>
            <div class="flex items-center gap-2">
              <button class="flex items-center gap-2 px-3 py-1.5 rounded-lg border border-slate-200 dark:border-slate-800 text-xs font-semibold text-slate-600 dark:text-slate-300 bg-slate-50 dark:bg-slate-800/50" type="button">
                <span class="material-symbols-outlined text-sm">filter_list</span> Lọc: Trạng thái
              </button>
              <button class="flex items-center gap-2 px-3 py-1.5 rounded-lg border border-slate-200 dark:border-slate-800 text-xs font-semibold text-slate-600 dark:text-slate-300 bg-slate-50 dark:bg-slate-800/50" type="button">
                <span class="material-symbols-outlined text-sm">sort</span> Sắp xếp: Tiến độ
              </button>
            </div>
          </div>
          <div class="overflow-x-auto">
            <table class="w-full border-collapse">
              <thead>
                <tr class="bg-slate-50 dark:bg-slate-800/50 text-left border-b border-slate-200 dark:border-slate-800">
                  <th class="px-6 py-4 text-xs font-bold text-slate-500 uppercase tracking-wider">Sinh viên</th>
                  <th class="px-6 py-4 text-xs font-bold text-slate-500 uppercase tracking-wider">Tiến độ</th>
                  <th class="px-6 py-4 text-xs font-bold text-slate-500 uppercase tracking-wider">Trạng thái hoạt động</th>
                  <th class="px-6 py-4 text-xs font-bold text-slate-500 uppercase tracking-wider">Camera/Âm thanh</th>
                  <th class="px-6 py-4 text-xs font-bold text-slate-500 uppercase tracking-wider text-right">Thao tác</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-slate-100 dark:divide-slate-800">
                <tr v-for="student in students" :key="student.id" :class="student.rowClass" class="hover:bg-slate-50 dark:hover:bg-slate-800/30 transition-colors">
                  <td class="px-6 py-4">
                    <div class="flex items-center gap-3">
                      <div class="size-10 rounded-full bg-slate-200 overflow-hidden shrink-0">
                        <img class="w-full h-full object-cover" :src="student.image" :alt="student.name" />
                      </div>
                      <div>
                        <p class="font-bold text-slate-900 dark:text-white">{{ student.name }}</p>
                        <p class="text-xs text-slate-500">ID: {{ student.id }}</p>
                      </div>
                    </div>
                  </td>
                  <td class="px-6 py-4">
                    <div class="w-full max-w-[140px]">
                      <div :class="student.progressTextClass" class="flex justify-between text-[10px] font-bold mb-1">
                        <span>{{ student.progress }}%</span>
                        <span>{{ student.questions }}</span>
                      </div>
                      <div class="h-1.5 w-full bg-slate-100 dark:bg-slate-800 rounded-full overflow-hidden">
                        <div :class="student.progressBarClass" class="h-full rounded-full" :style="{ width: `${student.progress}%` }"></div>
                      </div>
                    </div>
                  </td>
                  <td class="px-6 py-4">
                    <span :class="student.statusClass" class="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium border">{{ student.status }}</span>
                  </td>
                  <td class="px-6 py-4">
                    <div class="flex gap-2">
                      <span :class="student.cameraClass" class="material-symbols-outlined text-lg">{{ student.cameraIcon }}</span>
                      <span :class="student.micClass" class="material-symbols-outlined text-lg">{{ student.micIcon }}</span>
                    </div>
                  </td>
                  <td class="px-6 py-4 text-right">
                    <button class="p-2 text-slate-400 hover:text-primary transition-colors" type="button" @click="openStudentDetail(student)">
                      <span class="material-symbols-outlined">more_vert</span>
                    </button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
          <div class="px-6 py-4 border-t border-slate-200 dark:border-slate-800 flex items-center justify-between">
            <p class="text-sm text-slate-500 dark:text-slate-400">Hiển thị {{ students.length }} sinh viên đang hoạt động</p>
            <div class="flex gap-2">
              <button class="px-4 py-2 text-sm font-semibold text-slate-600 dark:text-slate-300 border border-slate-200 dark:border-slate-800 rounded-lg disabled:opacity-50" disabled type="button">Trước</button>
              <button class="px-4 py-2 text-sm font-semibold text-white bg-primary rounded-lg disabled:opacity-50" disabled type="button">Tiếp</button>
            </div>
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
import { sendTeacherWarning } from '../../services/monitoringService'
import { useRoute, useRouter } from 'vue-router'
import TeacherTopHeader from './TeacherTopHeader.vue'

const route = useRoute()
const router = useRouter()
const isDark = ref(false)
const attempts = ref([])
const detailsByAttemptId = ref({})
const loadError = ref('')
const isSyncing = ref(false)
const lastUpdatedAt = ref(null)
let refreshTimer = null

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
  .filter((attempt) => Number(attempt.riskScore || 0) > 0 || attempt.suspicious)
  .sort((a, b) => {
    const aAt = new Date(a.startedAt || 0).getTime()
    const bAt = new Date(b.startedAt || 0).getTime()
    return bAt - aAt
  })
  .slice(0, 5)
  .map((attempt, index) => {
    const riskScore = Number(attempt.riskScore || 0)
    const critical = riskScore >= 10 || Boolean(attempt.suspicious)
    const sortAt = new Date(attempt.startedAt || 0).getTime() || 0
    return {
      key: `${attempt.id}-${sortAt}-${index}`,
      student: attempt.student || 'Sinh viên không rõ',
      studentId: `AT-${attempt.id}`,
      meta: `Risk: ${riskScore} • Trạng thái: ${String(attempt.status || '').toUpperCase()}`,
      image: 'https://lh3.googleusercontent.com/aida-public/AB6AXuAIJ4SxrDKzIOAxYgwMxfCl9VECA94Lz5URlQAHT8wlNhUW4MNw9D1HLca97xsa6unSv5ggAypK-LlUtn8ncy9v-bzAFa3_8M1DavSb0bTFXN0n5cGflRifrcKtMGsdiwoybTbp9NCUsP7loGo43vS15PYo7KCItvkic1tbE7vNg_JatXHLLpScEs05NVeQLNXTuq294VTZO_Ynq_xP5jG7khjmSKYRitHROXp4_84cfjODRxtCxXX59LP1gTMP-0pkrK6iKTNdhA',
      cardClass: critical ? 'bg-red-50 dark:bg-red-900/10 border-2 border-red-500' : 'bg-amber-50 dark:bg-amber-900/10 border-2 border-amber-500',
      avatarClass: critical ? 'border-red-200' : 'border-amber-200',
      feedClass: critical ? 'bg-red-600' : 'bg-amber-600',
      title: critical ? 'Mức độ rủi ro cao' : 'Có dấu hiệu bất thường',
      titleClass: critical ? 'text-red-600' : 'text-amber-600',
      primaryAction: critical ? 'Can thiệp ngay' : 'Gửi cảnh báo',
      primaryActionClass: critical ? 'bg-red-600 hover:bg-red-700' : 'bg-amber-600 hover:bg-amber-700',
      isCritical: critical,
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
    examId: attempt.examId
  }
}))

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
  try {
    await sendTeacherWarning(item.attemptId)
  } catch {
    // keep UX simple on live dashboard; detailed errors shown in detail page actions
  }
}

const handlePrimaryAction = (item) => {
  if (item.primaryAction === 'Gửi cảnh báo') {
    sendQuickWarning(item)
    return
  }
  openStudentDetail(item)
}

const loadAttempts = async () => {
  if (!examId.value) {
    loadError.value = 'Thiếu mã đề thi. Vui lòng mở phiên giám sát từ danh sách đề thi.'
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
    loadError.value = ''
    lastUpdatedAt.value = Date.now()
  } catch (error) {
    loadError.value = error instanceof ApiError ? error.message : 'Không thể tải dữ liệu giám sát trực tiếp.'
  } finally {
    isSyncing.value = false
  }
}

onMounted(async () => {
  await loadAttempts()
  refreshTimer = window.setInterval(() => {
    loadAttempts()
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
