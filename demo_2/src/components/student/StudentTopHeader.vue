<template>
  <!-- Compact mode (exam interface) -->
  <template v-if="compact">
    <header
      class="student-topbar-compact sticky top-0 z-50 w-full border-b"
      style="background: var(--glass-surface); border-color: var(--glass-border); backdrop-filter: blur(20px); -webkit-backdrop-filter: blur(20px);"
    >
      <div class="flex w-full flex-nowrap items-center justify-between gap-2 px-4 py-2 sm:px-5 lg:px-6">
        <!-- Brand -->
        <div class="flex min-w-0 max-w-[13rem] shrink-0 items-center gap-2.5">
          <div class="shrink-0">
            <div class="topbar-brand-icon">
              <span class="material-symbols-outlined text-base" style="font-variation-settings:'FILL'1">school</span>
            </div>
          </div>
          <div class="min-w-0">
            <p class="text-sm font-bold truncate" style="color: var(--glass-text)">EduExam</p>
            <p v-if="compactExamKicker" class="text-[10px] font-semibold uppercase tracking-wider truncate" style="color: var(--glass-text-muted)">{{ compactExamKicker }}</p>
            <p v-if="compactExamTitle" class="text-xs font-bold truncate" style="color: var(--glass-amber)">{{ compactExamTitle }}</p>
            <p v-else class="text-[11px] font-semibold uppercase tracking-wider" style="color: var(--glass-text-muted)">Đang làm bài</p>
          </div>
        </div>
        <!-- Slot center -->
        <div v-if="$slots.compactCenter" class="hidden md:flex flex-1 justify-center px-2 max-w-[28rem] lg:max-w-none">
          <slot name="compactCenter" />
        </div>
        <!-- Right actions -->
        <div class="flex flex-nowrap items-center justify-end gap-1.5 overflow-x-auto sm:gap-2">
          <slot name="rightActions" />
        </div>
      </div>
    </header>
  </template>

  <!-- Full mode (dashboard) -->
  <template v-else>
    <!-- Fixed topbar (md+) -->
    <header
      class="student-topbar-full hidden md:flex fixed left-[var(--demo2-sidebar-offset,18rem)] right-0 top-0 h-14 items-center border-b px-6 z-40"
      style="background: var(--glass-surface); border-color: var(--glass-border); backdrop-filter: blur(16px); -webkit-backdrop-filter: blur(16px);"
    >
      <div class="flex w-full items-center justify-between gap-4">
        <div class="flex items-center gap-3">
          <p class="text-[11px] font-bold uppercase tracking-widest" style="color: var(--glass-text-muted)">EduExam</p>
          <span style="color: var(--glass-border)">/</span>
          <p class="text-sm font-bold" style="color: var(--glass-text)">Khu vực học sinh</p>
        </div>
      </div>
    </header>

    <!-- Mobile menu toggle -->
    <button
      type="button"
      class="mobile-menu-btn md:hidden"
      aria-label="Mở menu điều hướng"
      @click="mobileNavOpen = true"
    >
      <span class="material-symbols-outlined">menu</span>
    </button>

    <!-- Mobile overlay -->
    <div
      v-if="mobileNavOpen"
      class="fixed inset-0 z-[54] md:hidden"
      style="background: rgba(0,0,0,0.3); backdrop-filter: blur(4px)"
      aria-hidden="true"
      @click="mobileNavOpen = false"
    />

    <!-- Sidebar -->
    <aside
      class="student-sidebar"
      :class="{
        'sidebar--rail': isRailMd,
        'sidebar--open': mobileNavOpen,
        'sidebar--hidden': !mobileNavOpen && isMobile
      }"
    >
      <!-- Sidebar header -->
      <div class="sidebar__header" :class="isRailMd ? 'items-center justify-center' : ''">
        <div class="flex items-center gap-3" :class="isRailMd ? 'flex-col' : 'flex-1'">
          <div class="topbar-brand-icon shrink-0">
            <span class="material-symbols-outlined text-xl" style="font-variation-settings:'FILL'1">school</span>
          </div>
          <div v-if="!isRailMd" class="min-w-0">
            <p class="text-base font-black tracking-tight" style="color: var(--glass-amber)">EduExam Academy</p>
            <p class="text-[10px] font-semibold uppercase tracking-widest" style="color: var(--glass-text-muted)">Học sinh</p>
          </div>
        </div>
        <div :class="isRailMd ? 'flex-col items-center gap-2 mt-3' : 'flex items-center gap-2'">
          <button v-if="!isRailMd" type="button" class="sidebar-toggle-btn" title="Quay lại" @click="goBack">
            <span class="material-symbols-outlined text-xl">arrow_back</span>
          </button>
          <button type="button" class="sidebar-toggle-btn" :title="isRailMd ? 'Mở rộng' : 'Thu gọn'" @click="toggleNavDensity">
            <span class="material-symbols-outlined text-xl">{{ isRailMd ? 'menu_open' : 'menu' }}</span>
          </button>
        </div>
      </div>

      <!-- Nav -->
      <nav class="mt-4 flex flex-1 flex-col gap-1.5 px-2">
        <RouterLink
          v-for="item in primaryNav"
          :key="item.section"
          :to="item.to"
          class="sidebar-nav-link"
          :class="[activeSection === item.section ? 'is-active' : '', isRailMd ? 'justify-center px-2' : '']"
          @click="mobileNavOpen = false"
        >
          <span class="material-symbols-outlined shrink-0 text-xl">{{ item.icon }}</span>
          <span v-if="!isRailMd" class="text-xs font-semibold uppercase tracking-wide">{{ labelFor(item) }}</span>
        </RouterLink>
      </nav>

      <!-- Footer -->
      <div class="mt-auto border-t px-2 pt-4 pb-3 space-y-2" style="border-color: var(--glass-border)">
        <!-- Notifications -->
        <div v-if="showNotifications" class="relative">
          <button type="button" class="sidebar-icon-btn" aria-label="Thông báo" @click="showNotificationPanel = !showNotificationPanel">
            <span class="material-symbols-outlined text-xl">notifications</span>
            <span v-if="hasUnread" class="notif-dot" />
          </button>
          <div v-if="showNotificationPanel" class="notif-panel">
            <div class="flex items-center justify-between border-b px-4 py-3" style="border-color: var(--glass-border)">
              <span class="font-bold text-sm" style="color: var(--glass-text)">Thông báo</span>
              <button v-if="hasUnread" type="button" class="text-xs font-bold" style="color: var(--glass-amber)" @click="markAllAsRead">Đánh dấu đã đọc</button>
            </div>
            <div class="max-h-64 overflow-y-auto">
              <div v-if="!notifications.length" class="p-6 text-center text-sm" style="color: var(--glass-text-muted)">Chưa có thông báo.</div>
              <div
                v-for="n in notifications"
                :key="n.id"
                class="cursor-pointer border-b px-4 py-3 transition-colors hover:bg-[--glass-amber-soft]"
                style="border-color: var(--glass-border)"
                @click="markAsRead(n.id)"
              >
                <p class="text-sm font-semibold" style="color: var(--glass-text)">{{ n.title }}</p>
                <p class="text-xs mt-0.5" style="color: var(--glass-text-muted)">{{ n.message }}</p>
              </div>
            </div>
          </div>
        </div>

        <!-- Help -->
        <RouterLink v-if="showHelpLink" to="/help-center" class="sidebar-icon-btn" title="Trợ giúp" @click="mobileNavOpen = false">
          <span class="material-symbols-outlined text-xl">help_outline</span>
        </RouterLink>

        <!-- Profile -->
        <RouterLink
          v-if="showProfile"
          to="/student/profile"
          class="sidebar-nav-link"
          :class="[isRailMd ? 'justify-center px-2' : '']"
          @click="mobileNavOpen = false"
        >
          <span class="material-symbols-outlined shrink-0 text-xl">account_circle</span>
          <span v-if="!isRailMd" class="text-xs font-semibold uppercase tracking-wide">Hồ sơ</span>
        </RouterLink>

        <!-- CTA: Vào thi -->
        <RouterLink to="/student/exam-join" class="sidebar-cta-btn" @click="mobileNavOpen = false">
          <span class="material-symbols-outlined shrink-0 text-xl">rocket_launch</span>
          <span v-if="!isRailMd">Vào thi ngay</span>
        </RouterLink>

        <!-- Sign out -->
        <button v-if="showSignOut" type="button" class="sidebar-signout-btn" @click="goToLogin">
          <span class="material-symbols-outlined text-lg">logout</span>
          <span v-if="!isRailMd" class="text-xs font-semibold">Đăng xuất</span>
        </button>
      </div>
    </aside>
  </template>
</template>

<script setup>
import { computed, inject, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { clearAuthSession } from '../../services/authService'
import { useNotifications } from '../../composables/useNotifications'
import { useNavLabelMode } from '../../composables/useNavLabelMode'

const { notifications, hasUnread, markAsRead, markAllAsRead } = useNotifications()
const { mode, setMode, labelFor } = useNavLabelMode()

const isRailMd = computed(() => mode.value === 'icons')
const isMobile = ref(false)
const mobileNavOpen = inject('portalMobileNavOpen', ref(false))
const showNotificationPanel = ref(false)

const toggleNavDensity = () => {
  setMode(isRailMd.value ? 'full' : 'icons')
}

const props = defineProps({
  activeSection: { type: String, default: 'dashboard' },
  showSignOut: { type: Boolean, default: true },
  showProfile: { type: Boolean, default: true },
  showNotifications: { type: Boolean, default: true },
  showMenu: { type: Boolean, default: true },
  showHelpLink: { type: Boolean, default: true },
  compactExamKicker: { type: String, default: '' },
  compactExamTitle: { type: String, default: '' }
})

const compact = computed(() => !props.showMenu)
const router = useRouter()

const primaryNav = [
  { section: 'dashboard', to: '/student/', icon: 'dashboard', label: 'Bảng điều khiển', short: 'Bảng' },
  { section: 'examJoin', to: '/student/exam-join', icon: 'login', label: 'Thi qua mã', short: 'Mã' },
  { section: 'practice', to: '/student/generate-practice-test', icon: 'model_training', label: 'Luyện tập', short: 'LT' },
  { section: 'history', to: '/student/study-history', icon: 'history_edu', label: 'Lịch sử', short: 'LS' }
]

const goToLogin = () => {
  clearAuthSession()
  router.push('/login')
}

const goBack = () => {
  if (typeof window !== 'undefined' && window.history.length > 1) {
    router.back()
    return
  }
  router.push('/student/')
}
</script>

<style scoped>
.topbar-brand-icon {
  width: 2.25rem;
  height: 2.25rem;
  border-radius: var(--radius-glass-sm);
  background: linear-gradient(135deg, var(--glass-amber) 0%, var(--glass-amber-hover) 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  box-shadow: var(--shadow-glass-sm);
  flex-shrink: 0;
}

.mobile-menu-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 2.75rem;
  height: 2.75rem;
  border-radius: var(--radius-glass-sm);
  border: 1px solid var(--glass-border);
  background: var(--glass-surface);
  box-shadow: var(--shadow-glass-xs);
  color: var(--glass-text-secondary);
  cursor: pointer;
  transition: background var(--duration-fast) var(--ease-out), transform var(--duration-fast) var(--ease-spring);
  position: fixed;
  left: 0.75rem;
  top: 0.75rem;
  z-index: 60;
}
.mobile-menu-btn:hover {
  background: var(--glass-surface-hover);
  transform: scale(1.05);
  box-shadow: var(--shadow-glass-sm);
}

.student-sidebar {
  position: fixed;
  left: 0;
  top: 0;
  bottom: 0;
  width: 18rem;
  background: var(--glass-surface);
  backdrop-filter: blur(24px);
  -webkit-backdrop-filter: blur(24px);
  border-right: 1px solid var(--glass-border);
  box-shadow: var(--shadow-glass-md);
  z-index: var(--z-glass-sticky);
  display: flex;
  flex-direction: column;
  transition: width 0.3s var(--ease-out), transform 0.3s var(--ease-out);
  overflow-y: auto;
  overflow-x: hidden;
  padding: 0.75rem 0;
}

.student-sidebar.sidebar--rail { width: 4.5rem; }
.student-sidebar.sidebar--hidden { transform: translateX(-100%); }
.student-sidebar.sidebar--open { transform: translateX(0); box-shadow: var(--shadow-glass-float); }

.sidebar__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 0.75rem;
  padding: 0.5rem 0.75rem 1rem;
  border-bottom: 1px solid var(--glass-border);
}

.sidebar-toggle-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 2.25rem;
  height: 2.25rem;
  border-radius: var(--radius-glass-sm);
  border: 1px solid var(--glass-border);
  background: var(--glass-surface);
  color: var(--glass-text-secondary);
  cursor: pointer;
  flex-shrink: 0;
  transition: background var(--duration-fast) var(--ease-out), color var(--duration-fast) var(--ease-out), transform var(--duration-fast) var(--ease-spring);
}
.sidebar-toggle-btn:hover {
  background: var(--glass-amber-soft);
  color: var(--glass-amber);
  transform: scale(1.05);
}

.sidebar-nav-link {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.625rem 0.875rem;
  border-radius: var(--radius-glass-sm);
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--glass-text-secondary);
  text-decoration: none;
  cursor: pointer;
  border: none;
  background: transparent;
  width: 100%;
  text-align: left;
  transition: background var(--duration-fast) var(--ease-out), color var(--duration-fast) var(--ease-out), transform var(--duration-fast) var(--ease-out);
}
.sidebar-nav-link:hover {
  background: var(--glass-amber-soft);
  color: var(--glass-amber);
  transform: translateX(3px);
}
.sidebar-nav-link.is-active {
  background: var(--glass-amber-soft);
  color: var(--glass-amber);
  font-weight: 700;
  position: relative;
}
.sidebar-nav-link.is-active::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 3px;
  height: 60%;
  background: var(--glass-amber);
  border-radius: 0 2px 2px 0;
}

.sidebar-icon-btn {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 2.5rem;
  height: 2.5rem;
  border-radius: var(--radius-glass-sm);
  border: none;
  background: transparent;
  color: var(--glass-text-secondary);
  cursor: pointer;
  transition: background var(--duration-fast) var(--ease-out), color var(--duration-fast) var(--ease-out);
}
.sidebar-icon-btn:hover {
  background: var(--glass-amber-soft);
  color: var(--glass-amber);
}

.notif-dot {
  position: absolute;
  top: 0.35rem;
  right: 0.35rem;
  width: 0.5rem;
  height: 0.5rem;
  border-radius: 50%;
  background: #ef4444;
  border: 1.5px solid white;
}

.notif-panel {
  position: absolute;
  left: 0;
  bottom: calc(100% + 0.5rem);
  width: 20rem;
  background: var(--glass-surface-raised);
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-glass-lg);
  box-shadow: var(--shadow-glass-lg);
  z-index: 100;
  overflow: hidden;
}

.sidebar-cta-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  padding: 0.625rem 1rem;
  border-radius: var(--radius-glass-pill);
  background: linear-gradient(135deg, var(--glass-amber) 0%, var(--glass-amber-hover) 100%);
  color: white;
  font-size: 0.8rem;
  font-weight: 700;
  text-decoration: none;
  box-shadow: var(--shadow-glass-md);
  transition: transform var(--duration-base) var(--ease-spring), box-shadow var(--duration-base) var(--ease-smooth);
}
.sidebar-cta-btn:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-glass-xl);
  color: white;
}
.sidebar-cta-btn:active {
  transform: scale(0.97) translateY(0);
}

.sidebar-signout-btn {
  display: flex;
  align-items: center;
  gap: 0.625rem;
  padding: 0.5rem 0.875rem;
  border-radius: var(--radius-glass-sm);
  border: 1px solid var(--glass-border);
  background: transparent;
  color: var(--glass-text-secondary);
  cursor: pointer;
  width: 100%;
  transition: background var(--duration-fast) var(--ease-out), color var(--duration-fast) var(--ease-out), border-color var(--duration-fast) var(--ease-out);
}
.sidebar-signout-btn:hover {
  background: rgba(220, 38, 38, 0.06);
  color: var(--glass-danger);
  border-color: var(--glass-danger-border);
}

@media (max-width: 767px) {
  .student-sidebar {
    transform: translateX(-100%);
    z-index: var(--z-glass-overlay);
    width: 18rem;
    box-shadow: var(--shadow-glass-float);
  }
  .student-sidebar.sidebar--open { transform: translateX(0); }
  .student-sidebar.sidebar--rail { width: 18rem; }
}
</style>
