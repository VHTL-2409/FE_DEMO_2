<template>
  <div class="al-shell">
    <!-- Mobile hamburger -->
    <button
      type="button"
      class="al-hamburger"
      :class="{ 'al-hamburger--open': mobileNavOpen }"
      aria-label="Mở menu"
      @click="mobileNavOpen = !mobileNavOpen"
    >
      <span /><span /><span />
    </button>

    <!-- Mobile backdrop -->
    <Transition name="al-backdrop">
      <div
        v-if="mobileNavOpen"
        class="al-backdrop"
        aria-hidden="true"
        @click="mobileNavOpen = false"
      />
    </Transition>

    <!-- Sidebar -->
    <Transition name="al-sidebar">
      <aside
        v-if="mobileNavOpen || !isMobile"
        class="al-sidebar"
        :class="mobileNavOpen ? 'al-sidebar--open' : ''"
      >
        <!-- Brand -->
        <div class="al-brand">
          <div class="al-brand__logo">
            <LucideIcon name="shield_check" />
          </div>
          <div class="al-brand__text">
            <p class="al-brand__name">EduExam</p>
            <p class="al-brand__sub">Quản trị hệ thống</p>
          </div>
        </div>

        <!-- Nav -->
        <nav class="al-nav" aria-label="Admin navigation">
          <RouterLink
            v-for="link in navLinks"
            :key="link.to"
            :to="link.to"
            class="al-nav-link"
            active-class="al-nav-link--active"
            @click="mobileNavOpen = false"
          >
            <LucideIcon :name="link.icon" />
            <span>{{ link.label }}</span>
          </RouterLink>
        </nav>

        <!-- Footer -->
        <button
          type="button"
          class="al-logout"
          @click="logout"
        >
          <LucideIcon name="log_out" />
          <span>Đăng xuất</span>
        </button>
      </aside>
    </Transition>

    <!-- Main content -->
    <div class="al-main">
      <RouterView v-slot="{ Component }">
        <Transition name="al-view" mode="out-in">
          <component :is="Component" :key="route.fullPath" />
        </Transition>
      </RouterView>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter, RouterLink, RouterView } from 'vue-router'
import { clearAuthSession } from '../../services/authService'
import LucideIcon from '../common/LucideIcon.vue'

const route = useRoute()
const router = useRouter()
const mobileNavOpen = ref(false)
const windowWidth = ref(window.innerWidth)

const isMobile = computed(() => windowWidth.value < 768)

const handleResize = () => {
  windowWidth.value = window.innerWidth
  if (windowWidth.value >= 768) mobileNavOpen.value = false
}

onMounted(() => window.addEventListener('resize', handleResize))
onUnmounted(() => window.removeEventListener('resize', handleResize))

const navLinks = [
  { to: '/admin/dashboard', label: 'Tổng quan', icon: 'layout_dashboard' },
  { to: '/admin/students', label: 'Học sinh', icon: 'graduation_cap' },
  { to: '/admin/teachers', label: 'Giáo viên', icon: 'users' },
  { to: '/admin/admins', label: 'Admin', icon: 'shield' },
  { to: '/admin/exams', label: 'Đề thi', icon: 'clipboard_list' }
]

const logout = () => {
  mobileNavOpen.value = false
  clearAuthSession()
  router.push('/login')
}
</script>

<style scoped>
.al-shell {
  display: flex;
  min-height: 100vh;
  background: var(--ds-bg);
  font-family: var(--ds-font-display);
}

/* ── Hamburger ── */
.al-hamburger {
  display: none;
  position: fixed;
  top: 1rem;
  left: 1rem;
  z-index: 60;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  width: 42px;
  height: 42px;
  gap: 5px;
  background: var(--ds-surface);
  border: 1.5px solid var(--ds-border);
  border-radius: 12px;
  cursor: pointer;
  box-shadow: 0 2px 8px rgba(0,0,0,0.08);
  transition: background 0.15s ease;
}

.al-hamburger:hover { background: var(--ds-gray-50); }
.dark .al-hamburger:hover { background: var(--ds-gray-800); }

.al-hamburger span {
  display: block;
  width: 18px;
  height: 2px;
  background: var(--ds-text-secondary);
  border-radius: 2px;
  transition: all 0.25s ease;
}

.al-hamburger--open span:nth-child(1) {
  transform: translateY(7px) rotate(45deg);
  background: var(--ds-primary);
}
.al-hamburger--open span:nth-child(2) { opacity: 0; transform: scaleX(0); }
.al-hamburger--open span:nth-child(3) {
  transform: translateY(-7px) rotate(-45deg);
  background: var(--ds-primary);
}

/* ── Backdrop ── */
.al-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.5);
  z-index: 49;
  backdrop-filter: blur(2px);
}

.al-backdrop-enter-active { transition: opacity 0.2s ease; }
.al-backdrop-leave-active { transition: opacity 0.15s ease; }
.al-backdrop-enter-from, .al-backdrop-leave-to { opacity: 0; }

/* ── Sidebar ── */
.al-sidebar {
  width: 260px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  background: var(--ds-surface);
  border-right: 1.5px solid var(--ds-border);
  padding: 1.5rem 1rem;
  height: 100vh;
  position: sticky;
  top: 0;
  overflow-y: auto;
}

.dark .al-sidebar {
  background: #0f172a;
  border-right-color: var(--ds-border-strong);
}

/* Brand */
.al-brand {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0 0.5rem;
  margin-bottom: 2rem;
}

.al-brand__logo {
  width: 42px;
  height: 42px;
  border-radius: 12px;
  background: linear-gradient(135deg, var(--ds-primary), var(--ds-primary-600));
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  box-shadow: 0 2px 8px rgba(99, 102, 241, 0.3);
}

.al-brand__logo :deep(.lucide) { font-size: 1.25rem; }

.al-brand__name {
  font-family: var(--ds-font-display);
  font-size: 1.0625rem;
  font-weight: 900;
  color: var(--ds-text);
  margin: 0;
  line-height: 1.2;
}

.dark .al-brand__name { color: #f1f5f9; }

.al-brand__sub {
  font-size: 0.625rem;
  font-weight: 700;
  color: var(--ds-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.1em;
  margin: 0.125rem 0 0;
}

/* Nav */
.al-nav {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
  flex: 1;
}

.al-nav-link {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.75rem 1rem;
  border-radius: 12px;
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--ds-text-secondary);
  text-decoration: none;
  transition: all 0.15s ease;
}

.dark .al-nav-link { color: #94a3b8; }

.al-nav-link :deep(.lucide) {
  font-size: 1.125rem;
  flex-shrink: 0;
  transition: color 0.15s ease;
}

.al-nav-link:hover {
  color: var(--ds-primary);
  background: var(--ds-primary-soft);
}

.al-nav-link--active {
  color: var(--ds-primary);
  background: var(--ds-primary-soft);
  font-weight: 700;
}

.dark .al-nav-link--active { background: rgba(99, 102, 241, 0.12); }

/* Logout */
.al-logout {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.75rem 1rem;
  border-radius: 12px;
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--ds-danger);
  background: none;
  border: none;
  cursor: pointer;
  transition: all 0.15s ease;
  margin-top: 1rem;
  width: 100%;
  text-align: left;
}

.dark .al-logout { color: #f87171; }

.al-logout :deep(.lucide) {
  font-size: 1.125rem;
  flex-shrink: 0;
}

.al-logout:hover {
  background: var(--ds-danger-soft);
}

/* Main */
.al-main {
  flex: 1;
  min-width: 0;
  overflow: auto;
}

/* View transition */
.al-view-enter-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}
.al-view-leave-active {
  transition: opacity 0.15s ease, transform 0.15s ease;
}
.al-view-enter-from { opacity: 0; transform: translateY(8px); }
.al-view-leave-to { opacity: 0; transform: translateY(-4px); }

/* Responsive */
@media (max-width: 768px) {
  .al-hamburger { display: flex; }
  .al-sidebar {
    position: fixed;
    top: 0;
    left: 0;
    height: 100vh;
    z-index: 50;
    transform: translateX(-100%);
    transition: transform 0.25s cubic-bezier(0.4, 0, 0.2, 1);
    box-shadow: 4px 0 20px rgba(0, 0, 0, 0.15);
  }
  .al-sidebar--open { transform: translateX(0); }
  .al-sidebar-enter-active { transition: transform 0.25s cubic-bezier(0.4, 0, 0.2, 1); }
  .al-sidebar-leave-active { transition: transform 0.2s cubic-bezier(0.4, 0, 0.2, 1); }
  .al-sidebar-enter-from, .al-sidebar-leave-to { transform: translateX(-100%); }
  .al-main { padding: 0; }
}
</style>
