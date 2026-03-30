<template>
  <div class="admin-shell dark min-h-full">
    <AppShell
      layout="admin"
      :active-section="activeSection"
      :sidebar-items="adminSidebarItems"
      :user="authUser"
      role="admin"
    >
      <slot />
    </AppShell>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import AppShell from '../layout/AppShell.vue'
import { useAuthStore } from '../../stores/authStore'
import { storeToRefs } from 'pinia'

const route = useRoute()
const auth = useAuthStore()
const { user: authUser } = storeToRefs(auth)

const activeSection = computed(() => {
  const path = route.path || ''
  if (path === '/admin' || path === '/admin/dashboard') return 'dashboard'
  if (path.startsWith('/admin/users')) return 'users'
  if (path.startsWith('/admin/teachers')) return 'teachers'
  if (path.startsWith('/admin/students')) return 'students'
  if (path.startsWith('/admin/exams')) return 'exams'
  return 'dashboard'
})

const adminSidebarItems = [
  { to: '/admin/dashboard', icon: 'dashboard', label: 'Tổng quan' },
  { to: '/admin/users', icon: 'group', label: 'Người dùng' },
  { to: '/admin/teachers', icon: 'school', label: 'Giáo viên' },
  { to: '/admin/students', icon: 'person', label: 'Học sinh' },
  { to: '/admin/exams', icon: 'quiz', label: 'Đề thi' }
]
</script>

<style>
/* Admin dark sidebar overrides — applied via .admin-shell.dark */
.admin-shell.dark .ds-sidebar {
  background: #0f172a;
  border-right-color: rgba(51, 65, 85, 0.55);
}

.admin-shell.dark .ds-sidebar .border-b {
  border-bottom-color: rgba(51, 65, 85, 0.55) !important;
}

.admin-shell.dark .ds-sidebar .border-t {
  border-top-color: rgba(51, 65, 85, 0.55) !important;
}

.admin-shell.dark .ds-sidebar .border-r {
  border-right-color: rgba(51, 65, 85, 0.55) !important;
}

.admin-shell.dark .ds-sidebar .text-\[\#64748b\] {
  color: #64748b !important;
}

.admin-shell.dark .ds-sidebar .hover\:bg-\[\#e2e8f0\] {
  background: rgba(255, 255, 255, 0.05);
}

.admin-shell.dark .ds-sidebar .hover\:text-\[\#e2e8f0\] {
  color: #e2e8f0;
}

.admin-shell.dark .ds-sidebar .hover\:bg-slate-800 {
  background: rgba(255, 255, 255, 0.07);
}

.admin-shell.dark .ds-sidebar .text-\[\#94a3b8\] {
  color: #94a3b8 !important;
}

.admin-shell.dark .ds-sidebar .hover\:bg-\[\#94a3b8\] {
  background: rgba(255, 255, 255, 0.05);
}

/* Admin main area dark */
.admin-shell.dark {
  background: #0f172a;
}
</style>
