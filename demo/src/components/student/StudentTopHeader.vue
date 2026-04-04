<template>
  <div class="sth">
    <div class="sth__inner">
      <div class="sth__top">
        <!-- Brand -->
        <RouterLink to="/student/dashboard" class="sth__brand">
          <AppLogo size="sm" variant="default" :show-text="false" tag="div" />
          <div class="sth__brand-text">
            <p class="sth__brand-name">EduExam</p>
            <p class="sth__brand-sub">Thi · Luyện tập · Lịch sử</p>
          </div>
        </RouterLink>

        <!-- Right actions -->
        <div class="sth__actions">
          <slot name="rightActions" />

          <!-- Notifications -->
          <div v-if="showNotifications" class="sth__notif-wrap">
            <button
              type="button"
              class="sth__icon-btn"
              :class="{ 'sth__icon-btn--active': showNotificationPanel }"
              @click="showNotificationPanel = !showNotificationPanel"
            >
              <LucideIcon name="notifications" size="18" />
              <span
                v-if="hasUnread"
                class="sth__notif-badge"
              >{{ unreadCount > 9 ? '9+' : unreadCount }}</span>
            </button>
            <Transition name="sth-dropdown">
              <div
                v-if="showNotificationPanel"
                class="sth__dropdown sth__dropdown--notif"
              >
                <div class="sth__dropdown-header">
                  <span class="sth__dropdown-title">Thông báo</span>
                  <button v-if="hasUnread" type="button" class="sth__dropdown-link" @click="markAllAsRead">Đánh dấu đã đọc</button>
                </div>
                <div class="sth__notif-list">
                  <div v-if="!notifications.length" class="sth__notif-empty">Chưa có thông báo.</div>
                  <div
                    v-for="n in notifications"
                    :key="n.id"
                    :class="['sth__notif-item', { 'sth__notif-item--unread': !n.read }]"
                    @click="markAsRead(n.id)"
                  >
                    <p class="sth__notif-item-title">{{ n.title }}</p>
                    <p class="sth__notif-item-msg">{{ n.message }}</p>
                  </div>
                </div>
              </div>
            </Transition>
            <div v-if="showNotificationPanel" class="sth__backdrop" aria-hidden="true" @click="showNotificationPanel = false" />
          </div>

          <!-- Profile -->
          <div v-if="showProfile" class="sth__profile-wrap">
            <button
              type="button"
              class="sth__avatar-btn"
              @click="showProfileMenu = !showProfileMenu"
            >
              <div class="sth__avatar">{{ avatarLabel }}</div>
            </button>
            <Transition name="sth-dropdown">
              <div
                v-if="showProfileMenu"
                class="sth__dropdown sth__dropdown--profile"
              >
                <div class="sth__dropdown-user">
                  <p class="sth__dropdown-user-name">{{ displayName }}</p>
                  <p class="sth__dropdown-user-id">{{ displayId }}</p>
                </div>
                <div class="sth__dropdown-menu">
                  <button type="button" class="sth__dropdown-item" @click="handleGoToProfile">
                    <LucideIcon name="account_circle" size="16" />
                    Xem hồ sơ
                  </button>
                  <button v-if="showSignOut" type="button" class="sth__dropdown-item sth__dropdown-item--danger" @click="handleLogout">
                    <LucideIcon name="log_out" size="16" />
                    Đăng xuất
                  </button>
                </div>
              </div>
            </Transition>
            <div v-if="showProfileMenu" class="sth__backdrop" aria-hidden="true" @click="showProfileMenu = false" />
          </div>
        </div>
      </div>

      <!-- Nav tabs -->
      <nav v-if="showMenu" class="sth__nav" aria-label="Student navigation">
        <RouterLink
          v-for="(item, idx) in studentMenu"
          :key="item.key"
          :to="item.path"
          :class="['sth__nav-link', { 'sth__nav-link--active': isMenuActive(item.key) }]"
          :style="{ animationDelay: `${idx * 0.04}s` }"
        >
          <LucideIcon :name="item.icon" size="15" />
          {{ item.label }}
        </RouterLink>
      </nav>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { clearAuthSession, fetchMyProfile } from '../../services/authService'
import { useNotifications } from '../../composables/useNotifications'
import AppLogo from '../common/AppLogo.vue'

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
  { key: 'dashboard', label: 'Trang chủ', path: '/student/dashboard', icon: 'home' },
  { key: 'examJoin',   label: 'Vào thi',   path: '/student/exam-join',    icon: 'quiz' },
  { key: 'practice',   label: 'Luyện tập', path: '/student/generate-practice-test', icon: 'edit' },
  { key: 'history',    label: 'Kết quả',   path: '/student/study-history', icon: 'history' },
  { key: 'profile',    label: 'Hồ sơ',     path: '/student/profile',      icon: 'account_circle' }
]

const displayName = computed(() => profile.value?.username || 'Học sinh')
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
/* ─── Topbar shell ─────────────────────────────────────────────── */
.sth {
  position: sticky;
  top: 0;
  z-index: 50;
  width: 100%;
  background: rgb(255 255 255 / 0.88);
  backdrop-filter: blur(20px) saturate(200%);
  -webkit-backdrop-filter: blur(20px) saturate(200%);
  border-bottom: 1px solid var(--color-border);
  box-shadow: 0 1px 0 var(--color-border), 0 4px 20px rgba(15, 23, 42, 0.04);
}

.dark .sth {
  background: rgb(15 23 42 / 0.88);
  border-color: var(--color-border-strong);
  box-shadow: 0 1px 0 var(--color-border-strong), 0 4px 20px rgba(0, 0, 0, 0.2);
}

.sth__inner {
  max-width: 1400px;
  margin: 0 auto;
  padding: 0 1.5rem;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.sth__top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  padding: 0.75rem 0;
}

/* ─── Brand ──────────────────────────────────────────────────── */
.sth__brand {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  text-decoration: none;
}

.sth__brand-text { overflow: hidden; }

.sth__brand-name {
  font-size: 1rem;
  font-weight: 900;
  color: var(--color-text);
  letter-spacing: -0.025em;
  line-height: 1.2;
  margin: 0;
  white-space: nowrap;
  animation: fadeIn 0.4s ease 0.1s both;
}

.sth__brand-sub {
  font-size: 0.65rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.12em;
  color: var(--color-text-secondary);
  margin: 0.2rem 0 0;
  white-space: nowrap;
  animation: fadeIn 0.4s ease 0.18s both;
}

/* ─── Actions ─────────────────────────────────────────────────── */
.sth__actions {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

/* Icon button (notifications) */
.sth__icon-btn {
  position: relative;
  width: 40px;
  height: 40px;
  border-radius: 12px;
  border: 1.5px solid var(--color-border);
  background: var(--color-surface);
  color: var(--color-text-secondary);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: color 0.2s ease, background-color 0.2s ease, border-color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease cubic-bezier(0.4, 0, 0.2, 1);
}

.sth__icon-btn:hover,
.sth__icon-btn--active {
  border-color: var(--color-primary);
  color: var(--color-primary);
  background: var(--color-primary-soft);
  box-shadow: 0 0 0 3px var(--color-primary-ring);
  transform: translateY(-1px);
}

.sth__notif-badge {
  position: absolute;
  top: -4px;
  right: -4px;
  width: 18px;
  height: 18px;
  background: #ef4444;
  color: white;
  border-radius: 50%;
  font-size: 0.6rem;
  font-weight: 800;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 2px solid white;
  animation: badge-pop 0.3s cubic-bezier(0.34, 1.56, 0.64, 1) both,
             badge-pulse 2s ease-in-out 0.3s infinite;
  box-shadow: 0 2px 6px rgba(239, 68, 68, 0.4);
}

@keyframes badge-pop {
  from { transform: scale(0); }
  to   { transform: scale(1); }
}

@keyframes badge-pulse {
  0%, 100% { box-shadow: 0 2px 6px rgba(239, 68, 68, 0.4); }
  50% { box-shadow: 0 2px 10px rgba(239, 68, 68, 0.6); }
}

/* Avatar */
.sth__avatar-btn {
  background: none;
  border: none;
  cursor: pointer;
  padding: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.sth__avatar {
  width: 38px;
  height: 38px;
  border-radius: 12px;
  background: linear-gradient(135deg, var(--color-primary) 0%, #818cf8 100%);
  color: white;
  font-size: 0.875rem;
  font-weight: 800;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 12px rgba(79, 70, 229, 0.3);
  transition: transform 0.25s cubic-bezier(0.34, 1.56, 0.64, 1),
              box-shadow 0.25s ease;
}

.sth__avatar-btn:hover .sth__avatar {
  transform: scale(1.08) rotate(3deg);
  box-shadow: 0 8px 20px rgba(79, 70, 229, 0.4);
}

/* ─── Dropdown ────────────────────────────────────────────────── */
.sth__notif-wrap,
.sth__profile-wrap {
  position: relative;
}

.sth__dropdown {
  position: absolute;
  right: 0;
  top: calc(100% + 8px);
  z-index: 100;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  box-shadow: 0 20px 40px rgba(15, 23, 42, 0.12), 0 0 0 1px var(--color-border);
  border-radius: 16px;
  overflow: hidden;
}

.sth__dropdown--notif {
  width: 320px;
}

.sth__dropdown--profile {
  width: 220px;
}

.sth__backdrop {
  position: fixed;
  inset: 0;
  z-index: 90;
}

.sth__dropdown-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.875rem 1rem;
  border-bottom: 1px solid var(--color-border);
}

.sth__dropdown-title {
  font-size: 0.875rem;
  font-weight: 700;
  color: var(--color-text);
}

.sth__dropdown-link {
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--color-primary);
  background: none;
  border: none;
  cursor: pointer;
  padding: 0;
  text-decoration: underline;
}

.sth__dropdown-user {
  padding: 1rem;
  border-bottom: 1px solid var(--color-border);
  background: var(--color-surface-muted);
}

.sth__dropdown-user-name {
  font-size: 0.875rem;
  font-weight: 700;
  color: var(--color-text);
  margin: 0 0 0.2rem;
}

.sth__dropdown-user-id {
  font-size: 0.7rem;
  color: var(--color-text-secondary);
  margin: 0;
}

.sth__dropdown-menu {
  padding: 0.375rem;
}

.sth__dropdown-item {
  display: flex;
  align-items: center;
  gap: 0.625rem;
  width: 100%;
  padding: 0.625rem 0.75rem;
  border-radius: 10px;
  border: none;
  background: none;
  font-size: 0.875rem;
  font-weight: 500;
  font-family: inherit;
  color: var(--color-text-secondary);
  cursor: pointer;
  text-align: left;
  transition: color 0.18s ease, background-color 0.18s ease, border-color 0.18s ease, box-shadow 0.18s ease, transform 0.18s ease;
}

.sth__dropdown-item:hover {
  background: var(--color-surface-muted);
  color: var(--color-text);
}

.sth__dropdown-item--danger {
  color: #ef4444;
}

.sth__dropdown-item--danger:hover {
  background: rgba(239, 68, 68, 0.06);
  color: #dc2626;
}

/* Notification list */
.sth__notif-list {
  max-height: 300px;
  overflow-y: auto;
  overscroll-behavior: contain;
}

.sth__notif-empty {
  padding: 2rem;
  text-align: center;
  font-size: 0.875rem;
  color: var(--color-text-secondary);
}

.sth__notif-item {
  padding: 0.75rem 1rem;
  border-bottom: 1px solid var(--color-border);
  cursor: pointer;
  transition: background 0.15s ease;
}

.sth__notif-item:last-child { border-bottom: none; }

.sth__notif-item:hover { background: var(--color-surface-muted); }

.sth__notif-item--unread {
  background: rgba(79, 70, 229, 0.04);
}

.sth__notif-item--unread:hover { background: rgba(79, 70, 229, 0.08); }

.sth__notif-item-title {
  font-size: 0.8125rem;
  font-weight: 700;
  color: var(--color-text);
  margin: 0 0 0.25rem;
}

.sth__notif-item-msg {
  font-size: 0.75rem;
  color: var(--color-text-secondary);
  margin: 0;
}

/* Dropdown transition */
.sth-dropdown-enter-active {
  transition: color 0.25s cubic-bezier(0.34, 1.2, 0.64, 1), background-color 0.25s cubic-bezier(0.34, 1.2, 0.64, 1), border-color 0.25s cubic-bezier(0.34, 1.2, 0.64, 1), box-shadow 0.25s cubic-bezier(0.34, 1.2, 0.64, 1), transform 0.25s cubic-bezier(0.34, 1.2, 0.64, 1);
}
.sth-dropdown-leave-active {
  transition: color 0.18s cubic-bezier(0.4, 0, 1, 1), background-color 0.18s cubic-bezier(0.4, 0, 1, 1), border-color 0.18s cubic-bezier(0.4, 0, 1, 1), box-shadow 0.18s cubic-bezier(0.4, 0, 1, 1), transform 0.18s cubic-bezier(0.4, 0, 1, 1);
}
.sth-dropdown-enter-from {
  opacity: 0;
  transform: translateY(-8px) scale(0.96);
}
.sth-dropdown-leave-to {
  opacity: 0;
  transform: translateY(-4px) scale(0.98);
}

/* ─── Nav tabs ────────────────────────────────────────────────── */
.sth__nav {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  padding-bottom: 0.5rem;
  overflow-x: auto;
  scrollbar-width: none;
  mask-image: linear-gradient(90deg, transparent 0, black 12px, black calc(100% - 12px), transparent 100%);
}

.sth__nav::-webkit-scrollbar { display: none; }

.sth__nav-link {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  padding: 0.5rem 1rem;
  border-radius: 10px;
  font-size: 0.8125rem;
  font-weight: 600;
  color: var(--color-text-secondary);
  text-decoration: none;
  white-space: nowrap;
  flex-shrink: 0;
  transition: color 0.22s cubic-bezier(0.4, 0, 0.2, 1), background-color 0.22s cubic-bezier(0.4, 0, 0.2, 1), border-color 0.22s cubic-bezier(0.4, 0, 0.2, 1), box-shadow 0.22s cubic-bezier(0.4, 0, 0.2, 1), transform 0.22s cubic-bezier(0.4, 0, 0.2, 1);
  border: 1.5px solid transparent;
  background: transparent;
  animation: nav-slide-in 0.35s ease backwards;
}

@keyframes nav-slide-in {
  from { opacity: 0; transform: translateY(6px); }
  to   { opacity: 1; transform: translateY(0); }
}

.sth__nav-link:hover {
  color: var(--color-primary);
  background: var(--color-primary-soft);
  border-color: var(--color-primary-faint-border);
  transform: translateY(-2px);
}

.sth__nav-link--active {
  color: white;
  background: linear-gradient(135deg, var(--color-primary) 0%, #818cf8 100%);
  border-color: transparent;
  box-shadow: 0 4px 14px rgba(79, 70, 229, 0.35);
  transform: translateY(-2px);
}

.sth__nav-link--active:hover {
  color: white;
  background: linear-gradient(135deg, var(--color-primary-hover) 0%, var(--color-primary) 100%);
  transform: translateY(-3px);
  box-shadow: 0 6px 20px rgba(79, 70, 229, 0.45);
}

/* ─── Responsive ──────────────────────────────────────────────── */
@media (max-width: 640px) {
  .sth__inner { padding: 0 1rem; }
  .sth__brand-name { font-size: 0.875rem; }
  .sth__brand-sub { display: none; }
  .sth__dropdown--notif { width: 280px; }
}

/* ─── Reduced motion ──────────────────────────────────────────── */
@media (prefers-reduced-motion: reduce) {
  .sth__nav-link,
  .sth__brand-name,
  .sth__brand-sub,
  .sth__notif-badge {
    animation: none !important;
  }
  .sth__nav-link:hover,
  .sth__nav-link--active:hover,
  .sth__avatar-btn:hover .sth__avatar,
  .sth__icon-btn:hover,
  .sth__icon-btn--active {
    transform: none;
  }
}

@keyframes fadeIn {
  from { opacity: 0; }
  to   { opacity: 1; }
}
</style>
