<template>
  <header class="sticky top-0 z-50 w-full bg-white dark:bg-background-dark border-b border-slate-200 dark:border-slate-800">
    <div class="w-full px-6 md:px-20">
      <div class="flex justify-between items-center h-16">
        <div class="flex items-center gap-8">
          <div class="flex flex-col">
            <span class="text-primary dark:text-slate-100 text-lg font-bold leading-none">Bảng điều khiển giáo viên</span>
            <span class="text-slate-500 dark:text-slate-400 text-[10px] font-bold uppercase tracking-widest">
              Hệ thống thi trực tuyến
            </span>
          </div>
          <nav class="hidden md:flex items-center gap-1">
            <RouterLink :class="navClass('dashboard')" to="/teacher/dashboard">
              Trang chủ
            </RouterLink>
            <RouterLink :class="navClass('exam')" to="/teacher/exams">
              Tạo đề thi
            </RouterLink>
            <RouterLink :class="navClass('exam-list')" to="/teacher/exams/list">
              Danh sách đề thi
            </RouterLink>
            <RouterLink :class="navClass('monitoring')" to="/teacher/live-monitoring">
              Giám sát trực tiếp
            </RouterLink>
            <RouterLink :class="navClass('profile')" to="/teacher/profile">
              Hồ sơ
            </RouterLink>
          </nav>
        </div>
        <div class="flex items-center gap-4">
          <button class="p-2 text-slate-400 hover:text-primary transition-colors" type="button">
            <span class="material-symbols-outlined">notifications</span>
          </button>
          <button
            type="button"
            @click="goToLogin"
            class="text-xs font-semibold px-3 py-1.5 rounded bg-slate-100 dark:bg-slate-800 hover:bg-slate-200 dark:hover:bg-slate-700"
          >
            Đăng xuất
          </button>
          <div class="flex items-center gap-3 pl-4 border-l border-slate-200 dark:border-slate-800">
            <div class="text-right hidden sm:block">
              <p class="text-sm font-bold">{{ displayName }}</p>
            </div>
            <div class="h-9 w-9 rounded-full bg-primary text-white flex items-center justify-center font-bold text-sm">{{ avatarLabel }}</div>
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
    return 'px-4 py-2 rounded-lg text-sm text-primary border border-primary/10'
  }

  return 'px-4 py-2 rounded-lg text-sm text-slate-500 dark:text-slate-400 hover:text-primary transition-colors'
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
