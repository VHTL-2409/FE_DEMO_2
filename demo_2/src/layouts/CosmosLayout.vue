<template>
  <!-- Top navigation bar — clean, minimal, light mode -->
  <div class="cosmos-layout" :data-theme="theme">
    <!-- Top bar -->
    <header class="cosmos-topbar">
      <div class="cosmos-topbar__inner">
        <!-- Brand -->
        <RouterLink to="/" class="cosmos-topbar__brand">
          <div class="cosmos-topbar__logo">
            <svg width="28" height="28" viewBox="0 0 28 28" fill="none" xmlns="http://www.w3.org/2000/svg">
              <rect width="28" height="28" rx="8" fill="url(#topbarLogoGrad)"/>
              <path d="M14 6L8 10V18L14 22L20 18V10L14 6Z" stroke="white" stroke-width="1.5" stroke-linejoin="round" fill="none"/>
              <circle cx="14" cy="14" r="3" fill="white"/>
              <defs>
                <linearGradient id="topbarLogoGrad" x1="0" y1="0" x2="28" y2="28">
                  <stop offset="0%" stop-color="#06b6d4"/>
                  <stop offset="100%" stop-color="#0891b2"/>
                </linearGradient>
              </defs>
            </svg>
          </div>
          <span class="cosmos-topbar__brand-name">EduExam</span>
        </RouterLink>

        <!-- Center nav links -->
        <nav class="cosmos-topbar__nav" aria-label="Main navigation">
          <RouterLink
            v-for="item in navItems"
            :key="item.path"
            :to="item.path"
            class="cosmos-topbar__nav-link"
            :class="{ 'cosmos-topbar__nav-link--active': isActive(item.path) }"
          >
            <svg v-if="item.icon === 'home'" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="m3 9 9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/><polyline points="9 22 9 12 15 12 15 22"/></svg>
            <svg v-else-if="item.icon === 'list'" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="8" y1="6" x2="21" y2="6"/><line x1="8" y1="12" x2="21" y2="12"/><line x1="8" y1="18" x2="21" y2="18"/><line x1="3" y1="6" x2="3.01" y2="6"/><line x1="3" y1="12" x2="3.01" y2="12"/><line x1="3" y1="18" x2="3.01" y2="18"/></svg>
            <svg v-else-if="item.icon === 'quiz'" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14.5 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7.5L14.5 2z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/><line x1="10" y1="9" x2="8" y2="9"/></svg>
            <svg v-else-if="item.icon === 'monitor'" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="2" y="3" width="20" height="14" rx="2" ry="2"/><line x1="8" y1="21" x2="16" y2="21"/><line x1="12" y1="17" x2="12" y2="21"/></svg>
            <svg v-else-if="item.icon === 'report'" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="20" x2="18" y2="10"/><line x1="12" y1="20" x2="12" y2="4"/><line x1="6" y1="20" x2="6" y2="14"/></svg>
            <svg v-else-if="item.icon === 'groups'" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>
            <svg v-else-if="item.icon === 'calendar'" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="4" width="18" height="18" rx="2" ry="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/></svg>
            <svg v-else-if="item.icon === 'practice'" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"/></svg>
            <svg v-else-if="item.icon === 'history'" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>
            <svg v-else-if="item.icon === 'user'" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
            <svg v-else-if="item.icon === 'analytics'" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="20" x2="18" y2="10"/><line x1="12" y1="20" x2="12" y2="4"/><line x1="6" y1="20" x2="6" y2="14"/></svg>
            <span>{{ item.label }}</span>
          </RouterLink>
        </nav>

        <!-- Mobile hamburger -->
        <button
          type="button"
          class="cosmos-topbar__hamburger"
          :class="{ 'cosmos-topbar__hamburger--open': mobileMenuOpen }"
          aria-label="Menu"
          @click="mobileMenuOpen = !mobileMenuOpen"
        >
          <span /><span /><span />
        </button>

        <!-- Right: user menu + notifications -->
        <div class="cosmos-topbar__actions">
          <!-- Notification -->
          <div class="relative">
            <button
              type="button"
              class="cosmos-topbar__icon-btn"
              aria-label="Thông báo"
              @click="showNotifications = !showNotifications"
            >
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"/><path d="M13.73 21a2 2 0 0 1-3.46 0"/></svg>
              <span v-if="hasUnread" class="cosmos-topbar__badge" />
            </button>

            <!-- Notification dropdown -->
            <Transition name="cosmos-fade-up">
              <div
                v-if="showNotifications"
                class="cosmos-topbar__dropdown"
              >
                <div class="cosmos-topbar__dropdown-header">
                  <span class="cosmos-topbar__dropdown-title">Thông báo</span>
                  <button v-if="hasUnread" type="button" class="cosmos-topbar__dropdown-link" @click="markAllAsRead">Đánh dấu đã đọc</button>
                </div>
                <div class="cosmos-topbar__dropdown-body">
                  <div v-if="!notifications.length" class="cosmos-topbar__dropdown-empty">
                    Chưa có thông báo.
                  </div>
                  <div
                    v-for="n in notifications"
                    :key="n.id"
                    class="cosmos-topbar__notif-item"
                    :class="{ 'cosmos-topbar__notif-item--unread': !n.read }"
                    @click="markAsRead(n.id)"
                  >
                    <div class="cosmos-topbar__notif-icon">
                      <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
                    </div>
                    <div>
                      <p class="cosmos-topbar__notif-title">{{ n.title }}</p>
                      <p class="cosmos-topbar__notif-msg">{{ n.message }}</p>
                    </div>
                  </div>
                </div>
              </div>
            </Transition>
            <div v-if="showNotifications" class="fixed inset-0 z-40" @click="showNotifications = false" />
          </div>

          <!-- User avatar + dropdown -->
          <div class="relative">
            <button
              type="button"
              class="cosmos-topbar__user-btn"
              @click="showUserMenu = !showUserMenu"
            >
              <div class="cosmos-topbar__avatar">{{ userInitial }}</div>
              <span class="cosmos-topbar__username">{{ user?.username }}</span>
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="transition-transform" :class="showUserMenu ? 'rotate-180' : ''"><polyline points="6 9 12 15 18 9"/></svg>
            </button>

            <Transition name="cosmos-fade-up">
              <div v-if="showUserMenu" class="cosmos-topbar__dropdown cosmos-topbar__dropdown--right">
                <div class="cosmos-topbar__dropdown-user">
                  <div class="cosmos-topbar__avatar cosmos-topbar__avatar--lg">{{ userInitial }}</div>
                  <div>
                    <p class="cosmos-topbar__dropdown-name">{{ user?.username }}</p>
                    <p class="cosmos-topbar__dropdown-role">{{ roleLabel }}</p>
                  </div>
                </div>
                <div class="cosmos-topbar__dropdown-divider" />
                <RouterLink
                  :to="profilePath"
                  class="cosmos-topbar__dropdown-item"
                  @click="showUserMenu = false"
                >
                  <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
                  Hồ sơ cá nhân
                </RouterLink>
                <button type="button" class="cosmos-topbar__dropdown-item cosmos-topbar__dropdown-item--danger" @click="handleLogout">
                  <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/><polyline points="16 17 21 12 16 7"/><line x1="21" y1="12" x2="9" y2="12"/></svg>
                  Đăng xuất
                </button>
              </div>
            </Transition>
            <div v-if="showUserMenu" class="fixed inset-0 z-40" @click="showUserMenu = false" />
          </div>
        </div>
      </div>
    </header>

    <!-- Mobile menu overlay -->
    <Transition name="cosmos-mobile-menu">
      <div v-if="mobileMenuOpen" class="cosmos-mobile-menu">
        <div class="cosmos-mobile-menu__header">
          <RouterLink to="/" class="cosmos-topbar__brand" @click="mobileMenuOpen = false">
            <div class="cosmos-topbar__logo">
              <svg width="28" height="28" viewBox="0 0 28 28" fill="none">
                <rect width="28" height="28" rx="8" fill="url(#topbarLogoGrad2)"/>
                <path d="M14 6L8 10V18L14 22L20 18V10L14 6Z" stroke="white" stroke-width="1.5" stroke-linejoin="round" fill="none"/>
                <circle cx="14" cy="14" r="3" fill="white"/>
                <defs>
                  <linearGradient id="topbarLogoGrad2" x1="0" y1="0" x2="28" y2="28">
                    <stop offset="0%" stop-color="#06b6d4"/>
                    <stop offset="100%" stop-color="#0891b2"/>
                  </linearGradient>
                </defs>
              </svg>
            </div>
            <span class="cosmos-topbar__brand-name">EduExam</span>
          </RouterLink>
          <button
            type="button"
            class="cosmos-mobile-menu__close"
            aria-label="Đóng menu"
            @click="mobileMenuOpen = false"
          >
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
          </button>
        </div>
        <nav class="cosmos-mobile-menu__nav">
          <RouterLink
            v-for="item in navItems"
            :key="item.path"
            :to="item.path"
            class="cosmos-mobile-menu__link"
            :class="{ 'cosmos-mobile-menu__link--active': isActive(item.path) }"
            @click="mobileMenuOpen = false"
          >
            <svg v-if="item.icon === 'home'" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="m3 9 9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/><polyline points="9 22 9 12 15 12 15 22"/></svg>
            <svg v-else-if="item.icon === 'list'" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="8" y1="6" x2="21" y2="6"/><line x1="8" y1="12" x2="21" y2="12"/><line x1="8" y1="18" x2="21" y2="18"/><line x1="3" y1="6" x2="3.01" y2="6"/><line x1="3" y1="12" x2="3.01" y2="12"/><line x1="3" y1="18" x2="3.01" y2="18"/></svg>
            <svg v-else-if="item.icon === 'quiz'" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14.5 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7.5L14.5 2z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/><line x1="10" y1="9" x2="8" y2="9"/></svg>
            <svg v-else-if="item.icon === 'monitor'" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="2" y="3" width="20" height="14" rx="2" ry="2"/><line x1="8" y1="21" x2="16" y2="21"/><line x1="12" y1="17" x2="12" y2="21"/></svg>
            <svg v-else-if="item.icon === 'report'" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="20" x2="18" y2="10"/><line x1="12" y1="20" x2="12" y2="4"/><line x1="6" y1="20" x2="6" y2="14"/></svg>
            <svg v-else-if="item.icon === 'groups'" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>
            <svg v-else-if="item.icon === 'calendar'" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="4" width="18" height="18" rx="2" ry="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/></svg>
            <svg v-else-if="item.icon === 'practice'" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"/></svg>
            <svg v-else-if="item.icon === 'history'" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>
            <svg v-else-if="item.icon === 'user'" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
            <svg v-else-if="item.icon === 'analytics'" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="20" x2="18" y2="10"/><line x1="12" y1="20" x2="12" y2="4"/><line x1="6" y1="20" x2="6" y2="14"/></svg>
            <span>{{ item.label }}</span>
          </RouterLink>
        </nav>
        <div class="cosmos-mobile-menu__footer">
          <RouterLink
            :to="profilePath"
            class="cosmos-mobile-menu__link cosmos-mobile-menu__link--profile"
            @click="mobileMenuOpen = false"
          >
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
            <span>Hồ sơ cá nhân</span>
          </RouterLink>
          <button
            type="button"
            class="cosmos-mobile-menu__link cosmos-mobile-menu__link--danger"
            @click="handleLogout"
          >
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/><polyline points="16 17 21 12 16 7"/><line x1="21" y1="12" x2="9" y2="12"/></svg>
            <span>Đăng xuất</span>
          </button>
        </div>
      </div>
    </Transition>
    <!-- Mobile overlay backdrop -->
    <Transition name="cosmos-mobile-backdrop">
      <div v-if="mobileMenuOpen" class="cosmos-mobile-backdrop" @click="mobileMenuOpen = false" />
    </Transition>

    <!-- Main content area -->
    <main class="cosmos-main">
      <slot />
    </main>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { clearAuthSession } from '../services/authService'
import { useNotifications } from '../composables/useNotifications'
import { useAuthStore } from '../stores/authStore'
import { storeToRefs } from 'pinia'

const props = defineProps({
  navItems: { type: Array, default: () => [] },
  user: { type: Object, default: null },
  role: { type: String, default: 'teacher' }
})

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const { user: authUser } = storeToRefs(authStore)

const showNotifications = ref(false)
const showUserMenu = ref(false)
const mobileMenuOpen = ref(false)

const { notifications, hasUnread, markAsRead, markAllAsRead } = useNotifications()

const theme = 'light'

const userInitial = computed(() => {
  const u = props.user || authUser.value
  if (!u?.username) return '?'
  return u.username.slice(0, 1).toUpperCase()
})

const roleLabel = computed(() => {
  const labels = { teacher: 'Giáo viên', student: 'Học sinh', admin: 'Quản trị viên' }
  return labels[props.role] || props.role
})

const profilePath = computed(() => {
  if (props.role === 'teacher') return '/teacher/profile'
  if (props.role === 'student') return '/student/profile'
  if (props.role === 'admin') return '/admin/profile'
  return '/'
})

const isActive = (path) => {
  const current = route.path
  if (path === '/' || path === '') return current === '/'
  return current.startsWith(path)
}

const handleLogout = () => {
  mobileMenuOpen.value = false
  showUserMenu.value = false
  clearAuthSession()
  router.push('/login')
}
</script>

<style scoped>
/* ── Layout shell ── */
.cosmos-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f8fafc;
}

/* ── Top bar ── */
.cosmos-topbar {
  position: sticky;
  top: 0;
  z-index: 200;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-bottom: 1px solid #e2e8f0;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
  height: 64px;
}

.cosmos-topbar__inner {
  display: flex;
  align-items: center;
  height: 100%;
  padding: 0 1.5rem;
  gap: 1.5rem;
  max-width: 1400px;
  margin: 0 auto;
  width: 100%;
}

/* Brand */
.cosmos-topbar__brand {
  display: flex;
  align-items: center;
  gap: 0.625rem;
  text-decoration: none;
  flex-shrink: 0;
}

.cosmos-topbar__logo {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 10px;
  box-shadow: 0 2px 8px rgba(6, 182, 212, 0.25);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}
.cosmos-topbar__brand:hover .cosmos-topbar__logo {
  transform: scale(1.05);
  box-shadow: 0 4px 12px rgba(6, 182, 212, 0.35);
}

.cosmos-topbar__brand-name {
  font-family: 'Space Grotesk', system-ui, sans-serif;
  font-size: 1.125rem;
  font-weight: 800;
  color: #0f172a;
  letter-spacing: -0.03em;
  white-space: nowrap;
}

/* Nav */
.cosmos-topbar__nav {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  flex: 1;
  justify-content: center;
}

.cosmos-topbar__nav-link {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  padding: 0.5rem 0.875rem;
  border-radius: 10px;
  font-size: 0.875rem;
  font-weight: 600;
  color: #64748b;
  text-decoration: none;
  transition: all 0.15s ease;
  white-space: nowrap;
}

.cosmos-topbar__nav-link:hover {
  color: #06b6d4;
  background: rgba(6, 182, 212, 0.06);
}

.cosmos-topbar__nav-link--active {
  color: #06b6d4;
  background: rgba(6, 182, 212, 0.1);
  font-weight: 700;
}

/* Actions */
.cosmos-topbar__actions {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex-shrink: 0;
}

.cosmos-topbar__icon-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 38px;
  height: 38px;
  border-radius: 10px;
  border: none;
  background: transparent;
  color: #64748b;
  cursor: pointer;
  transition: all 0.15s ease;
  position: relative;
}
.cosmos-topbar__icon-btn:hover {
  background: rgba(6, 182, 212, 0.08);
  color: #06b6d4;
}

.cosmos-topbar__badge {
  position: absolute;
  top: 7px;
  right: 7px;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #ef4444;
  border: 2px solid white;
}

/* User button */
.cosmos-topbar__user-btn {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.375rem 0.75rem 0.375rem 0.375rem;
  border-radius: 12px;
  border: 1.5px solid #e2e8f0;
  background: white;
  cursor: pointer;
  transition: all 0.15s ease;
  color: #374151;
  font-size: 0.875rem;
  font-weight: 600;
}
.cosmos-topbar__user-btn:hover {
  border-color: #06b6d4;
  box-shadow: 0 0 0 3px rgba(6, 182, 212, 0.1);
}

.cosmos-topbar__avatar {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  background: linear-gradient(135deg, #06b6d4, #0891b2);
  color: white;
  font-weight: 700;
  font-size: 0.75rem;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.cosmos-topbar__avatar--lg {
  width: 42px;
  height: 42px;
  border-radius: 10px;
  font-size: 0.875rem;
}

.cosmos-topbar__username {
  max-width: 100px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #374151;
}

/* Dropdown */
.cosmos-topbar__dropdown {
  position: absolute;
  top: calc(100% + 8px);
  left: 0;
  width: 320px;
  background: white;
  border: 1px solid #e2e8f0;
  border-radius: 16px;
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.12);
  z-index: 300;
  overflow: hidden;
}

.cosmos-topbar__dropdown--right {
  left: auto;
  right: 0;
}

.cosmos-topbar__dropdown-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1rem 1.25rem 0.75rem;
  border-bottom: 1px solid #f1f5f9;
}

.cosmos-topbar__dropdown-title {
  font-size: 0.875rem;
  font-weight: 700;
  color: #0f172a;
}

.cosmos-topbar__dropdown-link {
  background: none;
  border: none;
  font-size: 0.75rem;
  font-weight: 600;
  color: #06b6d4;
  cursor: pointer;
  padding: 0;
}
.cosmos-topbar__dropdown-link:hover { text-decoration: underline; }

.cosmos-topbar__dropdown-body {
  max-height: 300px;
  overflow-y: auto;
}

.cosmos-topbar__dropdown-empty {
  padding: 1.5rem;
  text-align: center;
  font-size: 0.875rem;
  color: #94a3b8;
}

.cosmos-topbar__notif-item {
  display: flex;
  gap: 0.75rem;
  padding: 0.75rem 1.25rem;
  cursor: pointer;
  transition: background 0.1s ease;
  border-bottom: 1px solid #f8fafc;
}
.cosmos-topbar__notif-item:last-child { border-bottom: none; }
.cosmos-topbar__notif-item:hover { background: #f8fafc; }
.cosmos-topbar__notif-item--unread { background: rgba(6, 182, 212, 0.04); }

.cosmos-topbar__notif-icon {
  width: 28px;
  height: 28px;
  border-radius: 8px;
  background: rgba(6, 182, 212, 0.1);
  color: #06b6d4;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.cosmos-topbar__notif-title {
  font-size: 0.8125rem;
  font-weight: 600;
  color: #0f172a;
  line-height: 1.3;
}

.cosmos-topbar__notif-msg {
  font-size: 0.75rem;
  color: #64748b;
  margin-top: 0.125rem;
  line-height: 1.4;
}

/* User dropdown content */
.cosmos-topbar__dropdown-user {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 1rem 1.25rem;
}

.cosmos-topbar__dropdown-name {
  font-size: 0.875rem;
  font-weight: 700;
  color: #0f172a;
}

.cosmos-topbar__dropdown-role {
  font-size: 0.75rem;
  color: #64748b;
  margin-top: 0.125rem;
  text-transform: capitalize;
}

.cosmos-topbar__dropdown-divider {
  height: 1px;
  background: #f1f5f9;
  margin: 0.25rem 0;
}

.cosmos-topbar__dropdown-item {
  display: flex;
  align-items: center;
  gap: 0.625rem;
  padding: 0.625rem 1.25rem;
  font-size: 0.875rem;
  font-weight: 500;
  color: #374151;
  text-decoration: none;
  cursor: pointer;
  background: none;
  border: none;
  width: 100%;
  text-align: left;
  transition: background 0.1s ease, color 0.1s ease;
}
.cosmos-topbar__dropdown-item:hover {
  background: #f8fafc;
  color: #06b6d4;
}
.cosmos-topbar__dropdown-item--danger:hover {
  background: rgba(239, 68, 68, 0.05);
  color: #ef4444;
}

/* Main content */
.cosmos-main {
  flex: 1;
  padding: 1.5rem;
  max-width: 1400px;
  margin: 0 auto;
  width: 100%;
  box-sizing: border-box;
}

/* Fade-up transition */
.cosmos-fade-up-enter-active { transition: opacity 0.15s ease, transform 0.15s ease; }
.cosmos-fade-up-leave-active { transition: opacity 0.1s ease, transform 0.1s ease; }
.cosmos-fade-up-enter-from,
.cosmos-fade-up-leave-to { opacity: 0; transform: translateY(-6px); }

/* ── Hamburger button ── */
.cosmos-topbar__hamburger {
  display: none;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  width: 38px;
  height: 38px;
  gap: 5px;
  background: none;
  border: none;
  cursor: pointer;
  padding: 0;
  border-radius: 10px;
  transition: background 0.15s ease;
  flex-shrink: 0;
}

.cosmos-topbar__hamburger:hover { background: rgba(6, 182, 212, 0.08); }

.cosmos-topbar__hamburger span {
  display: block;
  width: 20px;
  height: 2px;
  background: #64748b;
  border-radius: 2px;
  transition: all 0.25s ease;
}

.cosmos-topbar__hamburger--open span:nth-child(1) {
  transform: translateY(7px) rotate(45deg);
  background: #06b6d4;
}

.cosmos-topbar__hamburger--open span:nth-child(2) {
  opacity: 0;
  transform: scaleX(0);
}

.cosmos-topbar__hamburger--open span:nth-child(3) {
  transform: translateY(-7px) rotate(-45deg);
  background: #06b6d4;
}

/* ── Mobile menu overlay ── */
.cosmos-mobile-menu {
  position: fixed;
  top: 0;
  left: 0;
  width: 300px;
  height: 100vh;
  background: rgba(255, 255, 255, 0.98);
  backdrop-filter: blur(24px);
  -webkit-backdrop-filter: blur(24px);
  border-right: 1px solid #e2e8f0;
  box-shadow: 4px 0 30px rgba(0, 0, 0, 0.12);
  z-index: 500;
  display: flex;
  flex-direction: column;
  padding: 1.25rem 1rem;
}

.cosmos-mobile-menu__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 1.5rem;
}

.cosmos-mobile-menu__close {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 10px;
  border: none;
  background: #f1f5f9;
  color: #64748b;
  cursor: pointer;
  transition: all 0.15s ease;
}

.cosmos-mobile-menu__close:hover {
  background: rgba(6, 182, 212, 0.1);
  color: #06b6d4;
}

.cosmos-mobile-menu__nav {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
  flex: 1;
}

.cosmos-mobile-menu__link {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.75rem 1rem;
  border-radius: 12px;
  font-size: 0.9375rem;
  font-weight: 600;
  color: #64748b;
  text-decoration: none;
  transition: all 0.15s ease;
  background: none;
  border: none;
  cursor: pointer;
  width: 100%;
  text-align: left;
}

.cosmos-mobile-menu__link:hover {
  color: #06b6d4;
  background: rgba(6, 182, 212, 0.06);
}

.cosmos-mobile-menu__link--active {
  color: #06b6d4;
  background: rgba(6, 182, 212, 0.1);
  font-weight: 700;
}

.cosmos-mobile-menu__footer {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
  padding-top: 1rem;
  border-top: 1px solid #f1f5f9;
  margin-top: 0.5rem;
}

.cosmos-mobile-menu__link--profile:hover {
  color: #0891b2;
  background: rgba(6, 182, 212, 0.04);
}

.cosmos-mobile-menu__link--danger:hover {
  color: #ef4444;
  background: rgba(239, 68, 68, 0.06);
}

/* Mobile backdrop */
.cosmos-mobile-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.4);
  z-index: 499;
  backdrop-filter: blur(2px);
}

/* Mobile transitions */
.cosmos-mobile-menu-enter-active { transition: transform 0.25s cubic-bezier(0.4, 0, 0.2, 1); }
.cosmos-mobile-menu-leave-active { transition: transform 0.2s cubic-bezier(0.4, 0, 0.2, 1); }
.cosmos-mobile-menu-enter-from,
.cosmos-mobile-menu-leave-to { transform: translateX(-100%); }

.cosmos-mobile-backdrop-enter-active { transition: opacity 0.2s ease; }
.cosmos-mobile-backdrop-leave-active { transition: opacity 0.15s ease; }
.cosmos-mobile-backdrop-enter-from,
.cosmos-mobile-backdrop-leave-to { opacity: 0; }

/* Responsive */
@media (max-width: 768px) {
  .cosmos-topbar__hamburger { display: flex; }
  .cosmos-topbar__actions { display: none; }
  .cosmos-topbar__nav { display: none; }
  .cosmos-topbar__brand-name { display: none; }
  .cosmos-main { padding: 1rem; }
}
</style>
