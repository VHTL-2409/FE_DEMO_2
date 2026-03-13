<template>
  <div class="border-b border-solid border-primary/10 bg-white dark:bg-background-dark">
    <header class="flex items-center justify-between whitespace-nowrap px-6 md:px-20 py-4">
      <div class="flex items-center gap-3 text-primary dark:text-slate-100">
        <div class="size-8 bg-primary rounded-lg flex items-center justify-center text-white">
          <span class="material-symbols-outlined">menu_book</span>
        </div>
        <h2 class="text-xl font-bold leading-tight tracking-tight">ExamPortal</h2>
      </div>
      <div class="flex flex-1 justify-end gap-4 items-center">
        <slot name="rightActions" />
        <button v-if="showNotifications" class="flex items-center justify-center rounded-full size-10 bg-primary/10 text-primary hover:bg-primary/20 transition-colors" type="button">
          <span class="material-symbols-outlined">notifications</span>
        </button>
        <button
          v-if="showSignOut"
          type="button"
          @click="goToLogin"
          class="text-xs font-semibold px-3 py-1.5 rounded bg-slate-100 dark:bg-slate-800 hover:bg-slate-200 dark:hover:bg-slate-700"
        >
          Đăng xuất
        </button>
        <button v-if="showProfile" type="button" @click="goToProfile" class="flex items-center gap-3 pl-2 border-l border-primary/10 hover:-translate-y-0.5 transition-all duration-200">
          <div class="hidden md:block text-right">
            <p class="text-sm font-bold">{{ displayName }}</p>
            <p class="text-xs text-slate-500">{{ displayId }}</p>
          </div>
          <div
            class="rounded-full size-10 border-2 border-primary/20 bg-primary text-white flex items-center justify-center font-bold"
          >
            {{ avatarLabel }}
          </div>
        </button>
      </div>
    </header>

    <nav v-if="showMenu" class="px-6 md:px-20 pb-3 flex flex-wrap gap-2">
      <RouterLink
        v-for="item in studentMenu"
        :key="item.key"
        :to="item.path"
        :class="isMenuActive(item.key)
          ? 'bg-primary/10 text-primary border border-primary/20'
          : 'text-slate-500 dark:text-slate-400 hover:text-primary hover:bg-slate-100 dark:hover:bg-slate-800 border border-transparent'"
        class="px-3 py-1.5 rounded-lg text-sm font-semibold transition-colors"
      >
        {{ item.label }}
      </RouterLink>
    </nav>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { clearAuthSession, fetchMyProfile } from '../../services/authService'

defineProps({
  showSignOut: {
    type: Boolean,
    default: true
  },
  showProfile: {
    type: Boolean,
    default: true
  },
  showNotifications: {
    type: Boolean,
    default: true
  },
  showMenu: {
    type: Boolean,
    default: true
  }
})

const router = useRouter()
const route = useRoute()
const profile = ref(null)

const studentMenu = [
  { key: 'dashboard', label: 'Dashboard', path: '/student/dashboard' },
  { key: 'examJoin', label: 'Thi qua mã', path: '/student/exam-join' },
  { key: 'practice', label: 'Luyện tập', path: '/student/generate-practice-test' },
  { key: 'history', label: 'Lịch sử/Kết quả', path: '/student/study-history' },
  { key: 'profile', label: 'Hồ sơ', path: '/student/profile' }
]

const displayName = computed(() => profile.value?.username || 'Sinh viên')
const displayId = computed(() => (profile.value?.id ? `ID: ${profile.value.id}` : ''))
const avatarLabel = computed(() => displayName.value.slice(0, 1).toUpperCase())

const isMenuActive = (key) => {
  const path = route.path
  if (key === 'dashboard') {
    return path === '/student/dashboard'
  }
  if (key === 'examJoin') {
    return path === '/student/exam-join' || path === '/student/exam-waiting-room' || path === '/student/exam-interface' || path === '/student/submission-confirmation'
  }
  if (key === 'practice') {
    return path === '/student/generate-practice-test'
  }
  if (key === 'history') {
    return path === '/student/study-history' || path === '/student/exam-result'
  }
  if (key === 'profile') {
    return path === '/student/profile'
  }
  return false
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

const goToProfile = () => {
  router.push('/student/profile')
}

onMounted(() => {
  loadProfile()
})
</script>
