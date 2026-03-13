<template>
  <div :class="isDark ? 'dark' : 'light'" class="bg-background-light dark:bg-background-dark font-display text-slate-900 dark:text-slate-100 min-h-screen">
    <div class="layout-container flex h-full grow flex-col">
      <StudentTopHeader />

      <main class="teacher-page-shell max-w-6xl">
        <div class="pointer-events-none absolute -top-16 -left-16 size-72 rounded-full bg-primary/15 blur-3xl animate-float-slow"></div>
        <div class="pointer-events-none absolute -bottom-24 -right-12 size-80 rounded-full bg-primary/10 blur-3xl animate-float-delay"></div>

        <div class="relative space-y-8">
          <header class="flex flex-col md:flex-row md:items-end justify-between gap-4 animate-fade-up">
            <div>
              <h1 class="text-3xl font-black tracking-tight">Hồ sơ sinh viên</h1>
              <p class="text-slate-500 dark:text-slate-400 mt-1">Thông tin tài khoản và thống kê học tập của bạn.</p>
            </div>
            <button
              @click="goToDashboard"
              class="px-5 py-2.5 rounded-xl border border-primary/20 bg-primary/10 text-primary font-bold hover:bg-primary/20 hover:-translate-y-0.5 hover:shadow-md transition-all duration-200"
              type="button"
            >
              Quay lại trang chủ
            </button>
          </header>

          <p v-if="isLoading" class="text-sm text-slate-500">Đang tải hồ sơ...</p>
          <p v-else-if="loadError" class="text-sm text-rose-600">{{ loadError }}</p>

          <section class="grid grid-cols-1 xl:grid-cols-3 gap-6 animate-fade-up-delay">
            <div class="xl:col-span-1 teacher-card p-6 hover:-translate-y-0.5 hover:shadow-md transition-all duration-200">
              <div class="flex flex-col items-center text-center">
                <div class="size-24 rounded-full border-4 border-primary/20 bg-primary/10 flex items-center justify-center text-3xl font-black text-primary">
                  {{ profileInitial }}
                </div>
                <h2 class="mt-4 text-xl font-bold">{{ profileName }}</h2>
                <p class="text-sm text-slate-500 dark:text-slate-400">ID: {{ profileId }}</p>
                <p class="text-sm text-slate-500 dark:text-slate-400">{{ profileEmail }}</p>
              </div>

              <div class="mt-6 space-y-3 text-sm">
                <div class="flex justify-between">
                  <span class="text-slate-500">Vai trò</span>
                  <span class="font-semibold text-emerald-600 dark:text-emerald-400">{{ profileRole }}</span>
                </div>
                <div class="flex justify-between">
                  <span class="text-slate-500">Tài khoản</span>
                  <span class="font-semibold">{{ profileEmail === '-' ? 'Chưa cập nhật email' : 'Đã xác định' }}</span>
                </div>
                <div class="flex justify-between">
                  <span class="text-slate-500">Trạng thái học tập</span>
                  <span class="font-semibold">Đang hoạt động</span>
                </div>
              </div>
            </div>

            <div class="xl:col-span-2 space-y-6">
              <div class="grid grid-cols-1 sm:grid-cols-3 gap-4">
                <div class="teacher-card p-5 shadow-sm hover:-translate-y-0.5 hover:shadow-md transition-all duration-200">
                  <p class="text-xs uppercase tracking-wider text-slate-500">Tổng lượt đã làm</p>
                  <p class="text-2xl font-bold mt-2">{{ totalAttempts }}</p>
                </div>
                <div class="teacher-card p-5 shadow-sm hover:-translate-y-0.5 hover:shadow-md transition-all duration-200">
                  <p class="text-xs uppercase tracking-wider text-slate-500">Điểm trung bình</p>
                  <p class="text-2xl font-bold mt-2">{{ averageScoreTen }} / 10</p>
                </div>
                <div class="teacher-card p-5 shadow-sm hover:-translate-y-0.5 hover:shadow-md transition-all duration-200">
                  <p class="text-xs uppercase tracking-wider text-slate-500">Điểm cao nhất</p>
                  <p class="text-2xl font-bold mt-2">{{ bestScoreTen }} / 10</p>
                </div>
              </div>

              <div class="teacher-card shadow-sm overflow-hidden">
                <div class="px-6 py-4 border-b border-slate-200 dark:border-slate-800 bg-slate-50 dark:bg-slate-800/50">
                  <h3 class="font-bold">Hoạt động gần đây</h3>
                </div>

                <div v-if="recentActivities.length" class="divide-y divide-slate-200 dark:divide-slate-800">
                  <div
                    v-for="item in recentActivities"
                    :key="item.id"
                    class="px-6 py-4 flex items-center justify-between hover:bg-primary/5 transition-colors"
                  >
                    <div>
                      <p class="font-semibold">{{ item.title }}</p>
                      <p class="text-sm text-slate-500 dark:text-slate-400">{{ item.meta }}</p>
                    </div>
                    <span class="text-sm font-bold text-primary">{{ item.score }} / 10</span>
                  </div>
                </div>

                <p v-else class="px-6 py-8 text-sm text-slate-500">Chưa có hoạt động nào để hiển thị.</p>
              </div>

              <div class="teacher-card p-6 shadow-sm">
                <h3 class="font-bold mb-3">Nguồn dữ liệu</h3>
                <p class="text-sm text-slate-600 dark:text-slate-300">
                  Hồ sơ được lấy từ hệ thống profile sinh viên (<span class="font-semibold">/api/profile/student</span>) và thống kê học tập được tổng hợp từ lịch sử lượt làm bài.
                </p>
              </div>
            </div>
          </section>
        </div>
      </main>

      <footer class="mt-auto px-6 md:px-20 lg:px-40 py-8 border-t border-slate-200 dark:border-slate-800 text-center">
        <p class="text-slate-500 dark:text-slate-400 text-sm">© 2026 Hệ thống thi trực tuyến ExamPortal. Bảo lưu mọi quyền.</p>
      </footer>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ApiError } from '../../services/apiClient'
import { fetchStudentProfile } from '../../services/authService'
import { listMyAttempts } from '../../services/attemptService'
import { listExams } from '../../services/examService'
import StudentTopHeader from './StudentTopHeader.vue'

const isDark = ref(false)
const router = useRouter()
const profile = ref(null)
const attempts = ref([])
const examTitleById = ref(new Map())
const isLoading = ref(false)
const loadError = ref('')

const profileName = computed(() => profile.value?.displayName || profile.value?.username || 'Sinh viên')
const profileInitial = computed(() => String(profileName.value).trim().charAt(0).toUpperCase() || 'S')
const profileId = computed(() => profile.value?.id || '-')
const profileEmail = computed(() => profile.value?.email || '-')
const profileRole = computed(() => 'Sinh viên')

const totalAttempts = computed(() => attempts.value.length)

const averageScoreTen = computed(() => {
  if (!attempts.value.length) return '0.0'
  const total = attempts.value.reduce((sum, item) => sum + Number(item.score || 0), 0)
  return (total / attempts.value.length / 10).toFixed(1)
})

const bestScoreTen = computed(() => {
  if (!attempts.value.length) return '0.0'
  return (Math.max(...attempts.value.map((item) => Number(item.score || 0))) / 10).toFixed(1)
})

const recentActivities = computed(() => attempts.value
  .slice()
  .sort((a, b) => {
    const aTime = new Date(a.submittedAt || a.startedAt || 0).getTime()
    const bTime = new Date(b.submittedAt || b.startedAt || 0).getTime()
    return (Number.isNaN(bTime) ? 0 : bTime) - (Number.isNaN(aTime) ? 0 : aTime)
  })
  .slice(0, 5)
  .map((attempt) => {
    const submittedAt = attempt.submittedAt ? new Date(attempt.submittedAt) : null
    const startedAt = attempt.startedAt ? new Date(attempt.startedAt) : null
    const minutes = submittedAt && startedAt
      ? Math.max(1, Math.round((submittedAt.getTime() - startedAt.getTime()) / 60000))
      : null

    return {
      id: attempt.id,
      title: examTitleById.value.get(attempt.examId) || attempt.examTitle || 'Bài thi',
      meta: submittedAt
        ? `${submittedAt.toLocaleDateString()} • ${minutes ? `${minutes}m` : '-'}`
        : '-',
      score: (Number(attempt.score || 0) / 10).toFixed(1)
    }
  }))

const loadProfileData = async () => {
  isLoading.value = true
  loadError.value = ''

  try {
    const [profilePayload, attemptsPayload, examsPayload] = await Promise.all([
      fetchStudentProfile(),
      listMyAttempts(),
      listExams().catch(() => [])
    ])

    profile.value = profilePayload
    attempts.value = attemptsPayload || []
    examTitleById.value = new Map((examsPayload || []).map((exam) => [exam.id, exam.title || 'Bài thi']))
  } catch (error) {
    profile.value = null
    attempts.value = []
    examTitleById.value = new Map()
    loadError.value = error instanceof ApiError ? error.message : 'Không thể tải hồ sơ sinh viên.'
  } finally {
    isLoading.value = false
  }
}

const goToDashboard = () => {
  router.push('/student/dashboard')
}

onMounted(() => {
  loadProfileData()
})
</script>
