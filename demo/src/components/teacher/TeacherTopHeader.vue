<template>
  <header class="sticky top-0 z-50 w-full bg-white/90 dark:bg-slate-900/90 backdrop-blur-xl border-b border-slate-200/80 dark:border-slate-800/80 shadow-sm">
    <div class="w-full max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
      <div class="flex justify-between items-center h-16">
        <div class="flex items-center gap-6 lg:gap-10">
          <div class="flex items-center gap-3">
            <div class="size-9 rounded-xl bg-primary flex items-center justify-center text-white shadow-lg shadow-primary/25">
              <span class="material-symbols-outlined text-lg">school</span>
            </div>
            <div class="flex flex-col">
              <span class="text-slate-900 dark:text-white text-base font-bold leading-tight">Bảng điều khiển giáo viên</span>
              <span class="text-slate-500 dark:text-slate-400 text-[10px] font-semibold uppercase tracking-wider">Hệ thống thi trực tuyến</span>
            </div>
          </div>
          <nav class="hidden md:flex items-center gap-1">
            <RouterLink :class="navClass('dashboard')" to="/teacher/dashboard" class="px-3.5 py-2 rounded-lg text-sm font-medium transition-all duration-200">
              Trang chủ
            </RouterLink>
            <RouterLink :class="navClass('exam')" to="/teacher/exams" class="px-3.5 py-2 rounded-lg text-sm font-medium transition-all duration-200">
              Tạo đề thi
            </RouterLink>
            <RouterLink :class="navClass('exam-list')" to="/teacher/exams/list" class="px-3.5 py-2 rounded-lg text-sm font-medium transition-all duration-200">
              Danh sách đề thi
            </RouterLink>
            <RouterLink :class="navClass('monitoring')" to="/teacher/live-monitoring" class="px-3.5 py-2 rounded-lg text-sm font-medium transition-all duration-200">
              Giám sát
            </RouterLink>
            <RouterLink :class="navClass('profile')" to="/teacher/profile" class="px-3.5 py-2 rounded-lg text-sm font-medium transition-all duration-200">
              Hồ sơ
            </RouterLink>
          </nav>
        </div>
        <div class="flex items-center gap-2 sm:gap-3">
          <button class="p-2.5 rounded-xl text-slate-500 hover:text-primary hover:bg-primary/10 transition-all duration-200" type="button">
            <span class="material-symbols-outlined text-xl">notifications</span>
          </button>
          <button
            type="button"
            @click="goToLogin"
            class="text-sm font-semibold px-4 py-2 rounded-xl bg-slate-100 dark:bg-slate-800 text-slate-700 dark:text-slate-200 hover:bg-slate-200 dark:hover:bg-slate-700 transition-all duration-200"
          >
            Đăng xuất
          </button>
          <div class="flex items-center gap-3 pl-3 border-l border-slate-200 dark:border-slate-700">
            <div class="text-right hidden sm:block">
              <p class="text-sm font-bold text-slate-900 dark:text-white">{{ displayName }}</p>
            </div>
            <div class="size-10 rounded-xl bg-gradient-to-br from-primary to-primary-700 text-white flex items-center justify-center font-bold text-sm shadow-md">{{ avatarLabel }}</div>
          </div>
        </div>
      </div>
    </div>
  </header>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter, RouterLink } from 'vue-router'
import { clearAuthSession, fetchMyProfile } from '../../services/authService'

const props = defineProps({
  activeSection: {
    type: String,
    default: 'dashboard'
  }
})

const router = useRouter()
const profile = ref(null)

const displayName = computed(() => profile.value?.username || 'Giáo viên')
const avatarLabel = computed(() => displayName.value.slice(0, 1).toUpperCase())
const navClass = (section) => {
  if (props.activeSection === section) {
    return 'text-primary bg-primary/10'
  }
  return 'text-slate-600 dark:text-slate-400 hover:text-primary hover:bg-slate-100 dark:hover:bg-slate-800'
}

const loadProfile = async () => {
  try {
    profile.value = await fetchMyProfile()
  } catch {
    profile.value = null
  }
}

const goToLogin = () => {
  clearAuthSession()
  router.push('/login')
}

onMounted(() => {
  loadProfile()
})
</script>
