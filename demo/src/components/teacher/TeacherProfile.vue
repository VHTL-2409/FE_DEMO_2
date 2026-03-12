<template>
  <div :class="isDark ? 'dark' : 'light'" class="bg-background-light dark:bg-background-dark font-display text-slate-900 dark:text-slate-100 min-h-screen">
    <div class="relative flex h-auto min-h-screen w-full flex-col overflow-x-hidden">
      <div class="layout-container flex h-full grow flex-col">
        <TeacherTopHeader active-section="profile" />

        <main class="teacher-page-shell max-w-6xl">
          <div class="pointer-events-none absolute -top-16 -left-16 size-72 rounded-full bg-primary/15 blur-3xl animate-float-slow"></div>
          <div class="pointer-events-none absolute -bottom-24 -right-12 size-80 rounded-full bg-primary/10 blur-3xl animate-float-delay"></div>

          <div class="relative max-w-6xl mx-auto space-y-8">
            <header class="flex flex-col md:flex-row md:items-end justify-between gap-4 animate-fade-up">
              <div>
                <h1 class="text-3xl font-black tracking-tight">Hồ sơ giáo viên</h1>
                <p class="text-slate-500 dark:text-slate-400 mt-1">Tổng quan hồ sơ giảng dạy và hoạt động thi của bạn.</p>
              </div>
              <button @click="goToDashboard" class="px-5 py-2.5 rounded-xl border border-primary/20 bg-primary/10 text-primary font-bold hover:bg-primary/20 hover:-translate-y-0.5 hover:shadow-md transition-all duration-200" type="button">
                Quay lại trang chủ
              </button>
            </header>

            <section class="grid grid-cols-1 xl:grid-cols-3 gap-6 animate-fade-up-delay">
              <div class="xl:col-span-1 bg-white dark:bg-slate-900 rounded-xl border border-slate-200 dark:border-slate-800 p-6 shadow-sm hover:-translate-y-0.5 hover:shadow-md transition-all duration-200">
                <div class="flex flex-col items-center text-center">
                  <div class="size-24 rounded-full border-4 border-primary/20 bg-primary text-white flex items-center justify-center text-2xl font-black">{{ avatarLabel }}</div>
                  <h2 class="mt-4 text-xl font-bold">{{ profileName }}</h2>
                  <p class="text-sm text-slate-500 dark:text-slate-400">{{ roleLabel }}</p>
                  <p class="text-sm text-slate-500 dark:text-slate-400">{{ profileEmail }}</p>
                </div>
                <div class="mt-6 space-y-3 text-sm">
                  <div class="flex justify-between"><span class="text-slate-500">Mã nhân viên</span><span class="font-semibold">{{ profileId }}</span></div>
                  <div class="flex justify-between"><span class="text-slate-500">Văn phòng</span><span class="font-semibold">Tòa nhà A-204</span></div>
                  <div class="flex justify-between"><span class="text-slate-500">Trạng thái</span><span class="font-semibold text-emerald-600 dark:text-emerald-400">{{ roleLabel }}</span></div>
                </div>
              </div>

              <div class="xl:col-span-2 space-y-6">
                <div class="grid grid-cols-1 sm:grid-cols-3 gap-4">
                  <div class="bg-white dark:bg-slate-900 rounded-xl border border-slate-200 dark:border-slate-800 p-5 shadow-sm hover:-translate-y-0.5 hover:shadow-md transition-all duration-200">
                    <p class="text-xs uppercase tracking-wider text-slate-500">Đề thi đã tạo</p>
                    <p class="text-2xl font-bold mt-2">42</p>
                  </div>
                  <div class="bg-white dark:bg-slate-900 rounded-xl border border-slate-200 dark:border-slate-800 p-5 shadow-sm hover:-translate-y-0.5 hover:shadow-md transition-all duration-200">
                    <p class="text-xs uppercase tracking-wider text-slate-500">Phiên đang hoạt động</p>
                    <p class="text-2xl font-bold mt-2">3</p>
                  </div>
                  <div class="bg-white dark:bg-slate-900 rounded-xl border border-slate-200 dark:border-slate-800 p-5 shadow-sm hover:-translate-y-0.5 hover:shadow-md transition-all duration-200">
                    <p class="text-xs uppercase tracking-wider text-slate-500">Sinh viên đã giám sát</p>
                    <p class="text-2xl font-bold mt-2">428</p>
                  </div>
                </div>

                <div class="bg-white dark:bg-slate-900 rounded-xl border border-slate-200 dark:border-slate-800 shadow-sm overflow-hidden">
                  <div class="px-6 py-4 border-b border-slate-200 dark:border-slate-800 bg-slate-50 dark:bg-slate-800/50">
                    <h3 class="font-bold">Phiên thi gần đây</h3>
                  </div>
                  <div class="divide-y divide-slate-200 dark:divide-slate-800">
                    <div v-for="session in sessions" :key="session.title" class="px-6 py-4 flex items-center justify-between hover:bg-primary/5 transition-colors">
                      <div>
                        <p class="font-semibold">{{ session.title }}</p>
                        <p class="text-sm text-slate-500 dark:text-slate-400">{{ session.meta }}</p>
                      </div>
                      <span :class="session.statusClass" class="text-xs font-bold px-2 py-1 rounded-full">{{ session.status }}</span>
                    </div>
                  </div>
                </div>

                <div class="bg-white dark:bg-slate-900 rounded-xl border border-slate-200 dark:border-slate-800 p-6 shadow-sm">
                  <h3 class="font-bold mb-4">Lịch làm việc & liên hệ</h3>
                  <div class="grid grid-cols-1 md:grid-cols-2 gap-4 text-sm">
                    <div class="rounded-lg bg-slate-50 dark:bg-slate-800/50 p-4 border border-slate-200 dark:border-slate-700">
                      <p class="text-slate-500">Giờ làm việc tại văn phòng</p>
                      <p class="font-semibold mt-1">Thứ 2 - Thứ 5, 14:00 - 16:00</p>
                    </div>
                    <div class="rounded-lg bg-slate-50 dark:bg-slate-800/50 p-4 border border-slate-200 dark:border-slate-700">
                      <p class="text-slate-500">Kênh hỗ trợ</p>
                      <p class="font-semibold mt-1">teacher-support@examportal.edu</p>
                    </div>
                  </div>
                </div>
              </div>
            </section>
          </div>
        </main>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { fetchMyProfile } from '../../services/authService'
import TeacherTopHeader from './TeacherTopHeader.vue'

const isDark = ref(false)
const router = useRouter()
const profile = ref(null)

const profileName = computed(() => profile.value?.username || 'Giáo viên')
const profileId = computed(() => profile.value?.id || '-')
const profileEmail = computed(() => profile.value?.email || '-')
const roleLabel = computed(() => {
  const rawRole = (profile.value?.roles || [])[0]
  if (rawRole === 'TEACHER') return 'Giáo viên'
  if (rawRole === 'ADMIN') return 'Quản trị'
  return rawRole || 'Giáo viên'
})
const avatarLabel = computed(() => profileName.value.slice(0, 1).toUpperCase())

const sessions = [
  {
    title: 'Thi giữa kỳ Nhập môn AI',
    meta: '02/11/2026 • 120 sinh viên',
    status: 'Đã hoàn thành',
    statusClass: 'bg-emerald-100 text-emerald-700 dark:bg-emerald-900/20 dark:text-emerald-400'
  },
  {
    title: 'Bài kiểm tra Cấu trúc dữ liệu',
    meta: '05/11/2026 • 88 sinh viên',
    status: 'Đang diễn ra',
    statusClass: 'bg-blue-100 text-blue-700 dark:bg-blue-900/20 dark:text-blue-400'
  },
  {
    title: 'Thi cuối kỳ Hệ cơ sở dữ liệu',
    meta: '10/11/2026 • 132 sinh viên',
    status: 'Đã lên lịch',
    statusClass: 'bg-amber-100 text-amber-700 dark:bg-amber-900/20 dark:text-amber-400'
  }
]

const loadProfile = async () => {
  try {
    profile.value = await fetchMyProfile()
  } catch {
    profile.value = null
  }
}

const goToDashboard = () => {
  router.push('/teacher/dashboard')
}

onMounted(() => {
  loadProfile()
})
</script>

