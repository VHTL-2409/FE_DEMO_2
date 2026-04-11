<template>
  <div class="adm-shell">
    <!-- Sidebar -->
    <aside class="adm-sidebar" :class="{ 'adm-sidebar--collapsed': collapsed }">
      <!-- Brand -->
      <RouterLink to="/admin/dashboard" class="adm-brand">
        <AppLogo size="sm" variant="brand" show-text />
      </RouterLink>

      <!-- Nav -->
      <nav class="adm-nav" role="navigation" aria-label="Admin navigation">
        <RouterLink
          v-for="item in navItems"
          :key="item.to"
          :to="item.to"
          class="adm-nav-item"
          :class="{ 'adm-nav-item--active': isActive(item.to) }"
        >
          <LucideIcon :name="item.icon" :size="18" />
          <span class="adm-nav-label">{{ item.label }}</span>
        </RouterLink>
      </nav>

      <!-- Bottom: logout -->
      <div class="adm-sidebar-footer">
        <button type="button" class="adm-nav-item adm-nav-item--logout" @click="logout">
          <LucideIcon name="log_out" :size="18" />
          <span class="adm-nav-label">Đăng xuất</span>
        </button>
      </div>
    </aside>

    <!-- Mobile overlay -->
    <div v-if="mobileOpen" class="adm-mobile-overlay" @click="mobileOpen = false" />

    <!-- Mobile sidebar -->
    <aside class="adm-sidebar adm-sidebar--mobile" :class="{ 'adm-sidebar--mobile-open': mobileOpen }">
      <div class="adm-sidebar-header">
        <AppLogo size="sm" variant="brand" show-text />
        <button type="button" class="adm-close-btn" @click="mobileOpen = false">
          <LucideIcon name="x" :size="20" />
        </button>
      </div>
      <nav class="adm-nav">
        <RouterLink
          v-for="item in navItems"
          :key="item.to"
          :to="item.to"
          class="adm-nav-item"
          :class="{ 'adm-nav-item--active': isActive(item.to) }"
          @click="mobileOpen = false"
        >
          <LucideIcon :name="item.icon" :size="18" />
          <span class="adm-nav-label">{{ item.label }}</span>
        </RouterLink>
      </nav>
      <div class="adm-sidebar-footer">
        <button type="button" class="adm-nav-item adm-nav-item--logout" @click="logout">
          <LucideIcon name="log_out" :size="18" />
          <span class="adm-nav-label">Đăng xuất</span>
        </button>
      </div>
    </aside>

    <!-- Main: no topbar, page fills remaining height -->
    <div ref="mainScrollEl" class="adm-main">
      <router-view v-slot="{ Component }">
        <Transition name="page" mode="out-in">
          <component :is="Component" />
        </Transition>
      </router-view>
    </div>
  </div>
</template>

<script setup>
import { nextTick, ref, watch } from 'vue'
import { useRoute, useRouter, RouterLink } from 'vue-router'
import AppLogo from '../common/AppLogo.vue'
import LucideIcon from '../common/LucideIcon.vue'
import { useAuthStore } from '../../stores/authStore'
import { storeToRefs } from 'pinia'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const { user: authUser } = storeToRefs(auth)

const collapsed = ref(false)
const mobileOpen = ref(false)
const mainScrollEl = ref(null)

const navItems = [
  { to: '/admin/dashboard', icon: 'layout_dashboard', label: 'Tổng quan' },
  { to: '/admin/students',  icon: 'graduation_cap',   label: 'Học sinh' },
  { to: '/admin/teachers',  icon: 'co_present',      label: 'Giáo viên' },
  { to: '/admin/admins',    icon: 'shield_check',     label: 'Quản trị' },
  { to: '/admin/exams',     icon: 'clipboard_list',   label: 'Đề thi' },
  { to: '/admin/classes',   icon: 'grid_view',       label: 'Quản lý lớp' },
]

const isActive = (path) => {
  const p = route.path
  if (path === '/admin/dashboard') return p === '/admin' || p === '/admin/dashboard'
  if (path === '/admin/classes') return p.startsWith('/admin/classes')
  return p.startsWith(path)
}

const logout = async () => {
  auth.logout()
  router.push('/gioi-thieu')
}

watch(
  () => route.path,
  async () => {
    await nextTick()
    requestAnimationFrame(() => {
      const pageScrollEl = mainScrollEl.value?.querySelector('.tui-page-wrap')

      if (pageScrollEl) {
        pageScrollEl.scrollTop = 0
        pageScrollEl.scrollLeft = 0
      } else if (mainScrollEl.value) {
        mainScrollEl.value.scrollTop = 0
        mainScrollEl.value.scrollLeft = 0
      }

      window.scrollTo({ top: 0, left: 0, behavior: 'auto' })
    })
  },
  { flush: 'post' }
)
</script>

<style scoped>
/* ─── Shell ─────────────────────────────────────────────────── */
.adm-shell {
  display: flex;
  height: 100vh;
  background: var(--ds-gray-50);
  font-family: var(--ds-font, 'Be Vietnam Pro', -apple-system, sans-serif);
  overflow: hidden; /* prevent page bounce */
}

/* ─── Sidebar ───────────────────────────────────────────────── */
.adm-sidebar {
  width: 240px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  background: var(--ds-surface);
  border-right: 1px solid var(--ds-gray-200);
  transition: width 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: hidden;
  z-index: 50;
}

.adm-sidebar--collapsed {
  width: 64px;
}

/* Brand */
.adm-brand {
  display: flex;
  align-items: center;
  padding: 1rem 1rem 0.75rem;
  border-bottom: 1px solid var(--ds-gray-200);
  text-decoration: none;
  gap: 0.5rem;
  flex-shrink: 0;
  min-height: 60px;
}

/* Nav */
.adm-nav {
  flex: 1;
  padding: 0.75rem 0.5rem;
  display: flex;
  flex-direction: column;
  gap: 0.125rem;
  overflow-y: auto;
}

.adm-nav::-webkit-scrollbar { width: 0; }

.adm-nav-item {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.625rem 0.75rem;
  border-radius: var(--ds-radius-lg, 14px);
  text-decoration: none;
  color: var(--ds-text-secondary);
  font-size: 0.875rem;
  font-weight: 600;
  transition:
    background 0.18s cubic-bezier(0.4, 0, 0.2, 1),
    color 0.18s cubic-bezier(0.4, 0, 0.2, 1),
    transform 0.18s cubic-bezier(0.34, 1.56, 0.64, 1),
    box-shadow 0.18s cubic-bezier(0.4, 0, 0.2, 1);
  cursor: pointer;
  border: none;
  background: transparent;
  font-family: inherit;
  white-space: nowrap;
  overflow: hidden;
  min-height: 40px;
}

.adm-nav-item:hover {
  background: var(--ds-gray-100);
  color: var(--ds-text);
  transform: translateX(3px);
}

.adm-nav-item--active {
  background: var(--ds-primary-soft);
  color: var(--ds-primary);
}

.adm-nav-item--active:hover {
  background: var(--ds-primary-soft);
  color: var(--ds-primary);
  transform: translateX(3px);
}

.adm-nav-label {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
}

.adm-sidebar--collapsed .adm-nav-label {
  display: none;
}

.adm-sidebar--collapsed .adm-nav-item {
  justify-content: center;
  padding: 0.625rem;
}

/* Sidebar footer */
.adm-sidebar-footer {
  padding: 0.5rem;
  border-top: 1px solid var(--ds-gray-200);
  flex-shrink: 0;
}

.adm-nav-item--logout {
  color: var(--ds-danger);
}

.adm-nav-item--logout:hover {
  background: rgba(220, 38, 38, 0.08);
  color: var(--ds-danger);
  transform: translateX(3px);
}

/* ─── Main ───────────────────────────────────────────────── */
/* CRITICAL: height:100% so .tui-page-wrap (height:100%) gets its reference */
.adm-main {
  flex: 1;
  min-width: 0;
  height: 100%;   /* ← key fix: reference for .tui-page-wrap's height:100% */
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

/* ─── Mobile ──────────────────────────────────────────────── */
.adm-mobile-overlay {
  display: none;
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.5);
  z-index: 49;
  animation: fadeIn 0.15s ease;
}

.adm-sidebar--mobile {
  display: none;
  position: fixed;
  top: 0;
  left: 0;
  bottom: 0;
  width: 280px;
  z-index: 50;
  transform: translateX(-100%);
  transition: transform 0.25s cubic-bezier(0.4, 0, 0.2, 1);
}

.adm-sidebar--mobile-open {
  transform: translateX(0);
}

.adm-sidebar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1rem;
  border-bottom: 1px solid var(--ds-gray-200);
  min-height: 60px;
}

.adm-close-btn {
  width: 36px;
  height: 36px;
  border-radius: var(--ds-radius-lg, 14px);
  background: transparent;
  border: none;
  color: var(--ds-text-secondary);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.15s ease;
}

.adm-close-btn:hover {
  background: var(--ds-gray-100);
  color: var(--ds-text);
}

/* Hamburger: always visible on mobile, hidden on desktop */
.adm-hamburger {
  display: none; /* hidden because no topbar */
}

@media (max-width: 768px) {
  .adm-sidebar {
    display: none;
  }

  .adm-mobile-overlay {
    display: block;
  }

  .adm-sidebar--mobile {
    display: flex;
    flex-direction: column;
  }
}

/* ─── Dark mode ─────────────────────────────────────────── */
.dark .adm-shell {
  background: #0f172a;
}

.dark .adm-sidebar {
  background: #1e293b;
  border-color: #334155;
}

.dark .adm-brand {
  border-color: #334155;
}

.dark .adm-nav-item:hover {
  background: #334155;
  color: #f1f5f9;
}

.dark .adm-nav-item--active {
  background: rgba(79, 70, 229, 0.2);
  color: #a5b4fc;
}

.dark .adm-nav-item--logout:hover {
  background: rgba(220, 38, 38, 0.15);
  color: #f87171;
}

.dark .adm-sidebar-footer {
  border-color: #334155;
}

.dark .adm-close-btn:hover {
  background: #334155;
  color: #f1f5f9;
}

@media (prefers-reduced-motion: reduce) {
  .adm-sidebar,
  .adm-sidebar--mobile {
    transition: none;
  }
  /* page transition is handled globally in animation.css */
}

@keyframes fadeIn {
  from { opacity: 0; }
  to   { opacity: 1; }
}
</style>
