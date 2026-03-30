<template>
  <div class="staff-topbar sticky top-0 z-50 w-full shrink-0 backdrop-blur-lg bg-white/80 dark:bg-slate-900/80">
    <div class="w-full px-3 sm:px-5 lg:px-6">
      <div class="flex min-h-[4.25rem] flex-col justify-center gap-3 py-3">
        <div class="flex flex-wrap items-center justify-between gap-3">
          <div class="flex min-w-0 items-center gap-3">
            <div class="staff-brand-mark flex size-10 items-center justify-center rounded-2xl text-white shadow-lg shadow-primary/20">
              <LucideIcon name="menu_book" size="18" />
            </div>
            <div class="min-w-0">
              <p class="truncate text-base font-black tracking-[-0.025em] text-slate-900 dark:text-white">EduExam</p>
              <p class="truncate text-[11px] font-semibold uppercase tracking-[0.16em] text-slate-500 dark:text-slate-400">
                Thi · Luyện tập · Lịch sử
              </p>
            </div>
          </div>
          <div class="flex flex-1 flex-wrap items-center justify-end gap-2 sm:gap-3">
            <slot name="rightActions" />
            <div v-if="showNotifications" class="relative">
              <button
                type="button"
                class="inline-flex size-10 items-center justify-center rounded-xl border border-slate-200 bg-white text-slate-600 transition-all duration-200 hover:border-primary/30 hover:text-primary hover:shadow-md dark:border-slate-700 dark:bg-slate-800 dark:text-slate-300 dark:hover:border-primary/50"
                @click="showNotificationPanel = !showNotificationPanel"
              >
                <LucideIcon name="notifications" />
                <span
                  v-if="hasUnread"
                  class="absolute -right-0.5 -top-0.5 flex size-4 items-center justify-center rounded-full bg-rose-500 text-[10px] font-bold text-white animate-pulse"
                  >{{ unreadCount > 9 ? '9+' : unreadCount }}</span
                >
              </button>
              <Transition name="dropdown">
                <div
                  v-if="showNotificationPanel"
                  class="portal-scrollbar absolute right-0 top-full z-50 mt-2 w-80 overflow-y-auto rounded-2xl border border-slate-200 bg-white shadow-2xl dark:border-slate-700 dark:bg-slate-900"
                >
                  <div class="flex shrink-0 items-center justify-between border-b border-slate-100 p-4 dark:border-slate-800">
                    <span class="font-bold text-slate-900 dark:text-white">Thông báo</span>
                    <button v-if="hasUnread" type="button" class="text-xs text-primary hover:underline font-semibold" @click="markAllAsRead">Đánh dấu đã đọc</button>
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
              </Transition>
              <div v-if="showNotificationPanel" class="fixed inset-0 z-40 backdrop-blur-sm" aria-hidden="true" @click="showNotificationPanel = false"></div>
            </div>
            <!-- Avatar dropdown with profile/signout -->
            <div class="relative">
              <button
                v-if="showProfile"
                type="button"
                class="staff-user-avatar flex items-center gap-2 rounded-xl px-2 py-2 transition-all duration-200 hover:bg-slate-100 dark:hover:bg-slate-700 group"
                @click="showProfileMenu = !showProfileMenu"
              >
                <div class="flex size-10 items-center justify-center rounded-2xl bg-gradient-to-br from-primary to-indigo-600 text-sm font-black text-white shadow-lg shadow-primary/20 transition-transform duration-200 group-hover:scale-105">
                  {{ avatarLabel }}
                </div>
              </button>
              
              <!-- Profile dropdown menu -->
              <Transition name="dropdown">
                <div
                  v-if="showProfileMenu"
                  class="profile-dropdown absolute right-0 top-full z-50 mt-2 w-56 overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-2xl dark:border-slate-700 dark:bg-slate-900"
                >
                  <div class="border-b border-slate-100 px-4 py-4 dark:border-slate-800">
                    <p class="text-sm font-bold text-slate-900 dark:text-white">{{ displayName }}</p>
                    <p class="text-xs text-slate-500">{{ displayId }}</p>
                  </div>
                  <div class="py-2">
                    <button
                      type="button"
                      class="flex w-full items-center gap-3 px-4 py-2.5 text-sm text-slate-700 hover:bg-slate-50 hover:text-primary dark:text-slate-300 dark:hover:bg-slate-800 transition-colors"
                      @click="handleGoToProfile"
                    >
                      <LucideIcon name="account_circle" size="16" />
                      Xem hồ sơ
                    </button>
                    <button
                      v-if="showSignOut"
                      type="button"
                      class="flex w-full items-center gap-3 px-4 py-2.5 text-sm text-rose-600 hover:bg-rose-50 dark:text-rose-400 dark:hover:bg-rose-900/20 transition-colors"
                      @click="handleLogout"
                    >
                      <LucideIcon name="logout" size="16" />
                      Đăng xuất
                    </button>
                  </div>
                </div>
              </Transition>
              <!-- Click outside overlay -->
              <div v-if="showProfileMenu" class="fixed inset-0 z-40 backdrop-blur-sm" aria-hidden="true" @click="showProfileMenu = false"></div>
            </div>
          </div>
        </div>

        <nav v-if="showMenu" class="staff-nav-scroller -mx-1 flex items-center gap-2 overflow-x-auto px-1">
          <RouterLink
            v-for="item in studentMenu"
            :key="item.key"
            :to="item.path"
            :class="['staff-nav-link rounded-xl px-4 py-2.5 text-sm font-semibold transition-all duration-200', isMenuActive(item.key) ? 'bg-primary text-white shadow-lg shadow-primary/20' : 'text-slate-600 hover:bg-slate-100 hover:text-primary dark:text-slate-400 dark:hover:bg-slate-800']"
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
const showProfileMenu = ref(false)

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
  { key: 'dashboard', label: 'Trang chủ', path: '/student/dashboard' },
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

const handleLogout = () => {
  clearAuthSession()
  router.push('/login')
}

const handleGoToProfile = () => {
  showProfileMenu.value = false
  router.push('/student/profile')
}

onMounted(() => {
  loadProfile()
})
</script>

<style scoped>
/* Dropdown transition */
.dropdown-enter-active,
.dropdown-leave-active {
  transition: all 0.2s ease;
}

.dropdown-enter-from,
.dropdown-leave-to {
  opacity: 0;
  transform: translateY(-8px) scale(0.98);
}

.dropdown-enter-to,
.dropdown-leave-from {
  opacity: 1;
  transform: translateY(0) scale(1);
}
</style>
