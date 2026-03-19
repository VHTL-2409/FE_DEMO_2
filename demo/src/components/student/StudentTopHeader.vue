<template>
  <div class="border-b border-slate-200/80 dark:border-slate-800/80 bg-white/95 dark:bg-slate-900/95 backdrop-blur-xl shadow-sm">
    <header class="flex items-center justify-between whitespace-nowrap px-4 sm:px-6 lg:px-8 max-w-7xl mx-auto py-4">
      <div class="flex items-center gap-3">
        <div class="size-10 rounded-xl bg-primary flex items-center justify-center text-white shadow-lg shadow-primary/25">
          <span class="material-symbols-outlined">menu_book</span>
        </div>
        <h2 class="text-xl font-bold leading-tight tracking-tight text-slate-900 dark:text-white">ExamPortal</h2>
      </div>
      <div class="flex flex-1 justify-end gap-2 sm:gap-3 items-center">
        <slot name="rightActions" />
        <div v-if="showNotifications" class="relative">
          <button
            type="button"
            @click="showNotificationPanel = !showNotificationPanel"
            class="flex items-center justify-center rounded-xl size-10 bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-300 hover:bg-primary/10 hover:text-primary transition-all duration-200 relative"
          >
            <span class="material-symbols-outlined">notifications</span>
            <span v-if="hasUnread" class="absolute -top-0.5 -right-0.5 size-4 rounded-full bg-rose-500 text-white text-[10px] font-bold flex items-center justify-center">{{ unreadCount > 9 ? '9+' : unreadCount }}</span>
          </button>
          <div
            v-if="showNotificationPanel"
            class="absolute right-0 top-full mt-2 w-80 max-h-96 overflow-y-auto bg-white dark:bg-slate-900 rounded-xl shadow-xl border border-slate-200 dark:border-slate-700 z-50"
          >
            <div class="p-3 border-b border-slate-200 dark:border-slate-700 flex items-center justify-between">
              <span class="font-bold text-slate-900 dark:text-white">Thông báo</span>
              <button v-if="hasUnread" type="button" @click="markAllAsRead" class="text-xs text-primary hover:underline">Đánh dấu đã đọc</button>
            </div>
            <div class="max-h-64 overflow-y-auto">
              <div v-if="!notifications.length" class="p-6 text-center text-slate-500 text-sm">Chưa có thông báo.</div>
              <div
                v-for="n in notifications"
                :key="n.id"
                @click="markAsRead(n.id)"
                :class="n.read ? 'bg-transparent' : 'bg-primary/5'"
                class="p-4 border-b border-slate-100 dark:border-slate-800 hover:bg-slate-50 dark:hover:bg-slate-800/50 cursor-pointer transition-colors"
              >
                <p class="font-semibold text-slate-900 dark:text-white text-sm">{{ n.title }}</p>
                <p class="text-xs text-slate-500 dark:text-slate-400 mt-0.5">{{ n.message }}</p>
              </div>
            </div>
          </div>
          <div v-if="showNotificationPanel" class="fixed inset-0 z-40" @click="showNotificationPanel = false" aria-hidden="true"></div>
        </div>
        <button
          v-if="showSignOut"
          type="button"
          @click="goToLogin"
          class="text-sm font-semibold px-4 py-2 rounded-xl bg-slate-100 dark:bg-slate-800 text-slate-700 dark:text-slate-200 hover:bg-slate-200 dark:hover:bg-slate-700 transition-all duration-200"
        >
          Đăng xuất
        </button>
        <button v-if="showProfile" type="button" @click="goToProfile" class="flex items-center gap-3 pl-3 border-l border-slate-200 dark:border-slate-700 hover:opacity-90 transition-all duration-200">
          <div class="hidden md:block text-right">
            <p class="text-sm font-bold text-slate-900 dark:text-white">{{ displayName }}</p>
            <p class="text-xs text-slate-500 dark:text-slate-400">{{ displayId }}</p>
          </div>
          <div class="rounded-xl size-10 bg-gradient-to-br from-primary to-primary-700 text-white flex items-center justify-center font-bold shadow-md">
            {{ avatarLabel }}
          </div>
        </button>
      </div>
    </header>

    <nav v-if="showMenu" class="px-4 sm:px-6 lg:px-8 max-w-7xl mx-auto pb-4 flex flex-wrap gap-2">
      <RouterLink
        v-for="item in studentMenu"
        :key="item.key"
        :to="item.path"
        :class="isMenuActive(item.key)
          ? 'bg-primary/10 text-primary font-semibold'
          : 'text-slate-600 dark:text-slate-400 hover:text-primary hover:bg-slate-100 dark:hover:bg-slate-800'"
        class="px-4 py-2 rounded-xl text-sm font-medium transition-all duration-200"
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
import { useNotifications } from '../../composables/useNotifications'

const { notifications, hasUnread, unreadCount, markAsRead, markAllAsRead } = useNotifications()
const showNotificationPanel = ref(false)

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
