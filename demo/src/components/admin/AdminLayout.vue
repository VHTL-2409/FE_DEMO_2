<template>
  <div class="db-admin-shell">
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
  { to: '/admin/dashboard', icon: 'dashboard', label: 'Tong quan', section: 'dashboard' },
  { to: '/admin/users', icon: 'group', label: 'Nguoi dung', section: 'users' },
  { to: '/admin/teachers', icon: 'school', label: 'Giao vien', section: 'teachers' },
  { to: '/admin/students', icon: 'person', label: 'Hoc sinh', section: 'students' },
  { to: '/admin/exams', icon: 'quiz', label: 'De thi', section: 'exams' }
]
</script>

<style scoped>
.db-admin-shell {
  min-height: 100vh;
  background: var(--db-surface-2);
  font-family: var(--db-font);
}
</style>
