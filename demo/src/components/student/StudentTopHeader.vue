<template>
  <div class="staff-topbar sticky top-0 z-50 w-full shrink-0">
    <div class="w-full px-3 sm:px-5 lg:px-6">
      <div class="flex min-h-[4.25rem] flex-col justify-center gap-3 py-3">
        <div class="flex flex-wrap items-center justify-between gap-3">
          <div class="flex min-w-0 items-center gap-3">
            <div class="staff-brand-mark flex size-10 items-center justify-center rounded-2xl text-white">
              <span class="material-symbols-outlined text-lg">menu_book</span>
            </div>
            <div class="min-w-0">
              <p class="truncate text-base font-black tracking-[-0.025em] text-slate-900 dark:text-white">EduExam Student</p>
              <p class="truncate text-[11px] font-semibold uppercase tracking-[0.16em] text-slate-500 dark:text-slate-400">
                Thi · luyện tập · lịch sử
              </p>
            </div>
          </div>
          <div class="flex flex-1 flex-wrap items-center justify-end gap-2 sm:gap-3">
            <slot name="rightActions" />
            <div v-if="showNotifications" class="relative">
              <button
                type="button"
                class="inline-flex size-10 items-center justify-center rounded-xl border border-slate-200 bg-white text-slate-600 transition hover:border-primary/20 hover:text-primary dark:border-slate-700 dark:bg-slate-800 dark:text-slate-300"
                @click="showNotificationPanel = !showNotificationPanel"
              >
                <span class="material-symbols-outlined">notifications</span>
                <span
                  v-if="hasUnread"
                  class="absolute -right-0.5 -top-0.5 flex size-4 items-center justify-center rounded-full bg-rose-500 text-[10px] font-bold text-white"
                  >{{ unreadCount > 9 ? '9+' : unreadCount }}</span
                >
              </button>
              <div
                v-if="showNotificationPanel"
                class="portal-scrollbar absolute right-0 top-full z-50 mt-2 flex max-h-96 w-80 flex-col overflow-y-auto rounded-xl border border-slate-200 bg-white shadow-xl dark:border-slate-700 dark:bg-slate-900"
              >
                <div class="flex shrink-0 items-center justify-between border-b border-slate-200 p-3 dark:border-slate-700">
                  <span class="font-bold text-slate-900 dark:text-white">Thông báo</span>
                  <button v-if="hasUnread" type="button" class="text-xs text-primary hover:underline" @click="markAllAsRead">Đánh dấu đã đọc</button>
                </div>
                <div>
                  <div v-if="!notifications.length" class="p-6 text-center text-sm text-slate-500">Chưa có thông báo.</div>
                  <div
                    v-for="n in notifications"
                    :key="n.id"
                    :class="n.read ? 'bg-transparent' : 'bg-primary/5'"
                    class="cursor-pointer border-b border-slate-100 p-4 transition-colors hover:bg-slate-50 dark:border-slate-800 dark:hover:bg-slate-800/50"
                    @click="markAsRead(n.id)"
                  >
                    <p class="text-sm font-semibold text-slate-900 dark:text-white">{{ n.title }}</p>
                    <p class="mt-0.5 text-xs text-slate-500 dark:text-slate-400">{{ n.message }}</p>
                  </div>
                </div>
              </div>
              <div v-if="showNotificationPanel" class="fixed inset-0 z-40" aria-hidden="true" @click="showNotificationPanel = false"></div>
            </div>
            <button
              v-if="showSignOut"
              type="button"
              class="inline-flex items-center gap-2 rounded-xl border border-slate-200 bg-white px-4 py-2 text-sm font-semibold text-slate-700 transition hover:border-primary/20 hover:text-primary dark:border-slate-700 dark:bg-slate-800 dark:text-slate-200"
              @click="goToLogin"
            >
              <span class="material-symbols-outlined text-lg">logout</span>
              <span class="hidden sm:inline">Đăng xuất</span>
            </button>
            <button
              v-if="showProfile"
              type="button"
              class="staff-user-pill flex items-center gap-2 rounded-xl px-2 py-2 sm:gap-3 sm:px-3"
              @click="goToProfile"
            >
              <div class="hidden text-right md:block">
                <p class="text-sm font-bold text-slate-900 dark:text-white">{{ displayName }}</p>
                <p class="text-[11px] font-medium uppercase tracking-[0.12em] text-slate-500 dark:text-slate-400">{{ displayId }}</p>
              </div>
              <div class="staff-brand-mark flex size-10 items-center justify-center rounded-2xl text-sm font-black text-white">
                {{ avatarLabel }}
              </div>
            </button>
          </div>
        </div>

        <nav v-if="showMenu" class="staff-nav-scroller -mx-1 flex items-center gap-2 overflow-x-auto px-1">
          <RouterLink
            v-for="item in studentMenu"
            :key="item.key"
            :to="item.path"
            :class="['staff-nav-link rounded-xl px-3 py-2 text-sm font-semibold', isMenuActive(item.key) ? 'is-active' : '']"
          >
            {{ item.label }}
          </RouterLink>
        </nav>
      </div>
    </div>
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
