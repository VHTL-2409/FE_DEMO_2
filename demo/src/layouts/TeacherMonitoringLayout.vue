<template>
  <AppShell
    layout="monitoring"
    :active-section="activeSection"
    :monitoring-sidebar-items="monitoringSidebarItems"
    :user="authUser"
    role="teacher"
  >
    <slot />
  </AppShell>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import AppShell from '../components/layout/AppShell.vue'
import { useAuthStore } from '../stores/authStore'
import { storeToRefs } from 'pinia'
import { readMonitoringSessionQuery } from '../services/monitoringContextStorage'

const route = useRoute()
const auth = useAuthStore()
const { user: authUser } = storeToRefs(auth)

const activeSection = computed(() => {
  const path = route.path || ''
  if (path === '/teacher/live-monitoring') return 'select-exam'
  if (path.startsWith('/teacher/live-monitoring/session')) return 'session'
  if (path.startsWith('/teacher/live-monitoring/student-detail')) return 'student'
  return 'select-exam'
})

const sessionNavTo = computed(() => {
  const q = readMonitoringSessionQuery()
  return q
    ? { path: '/teacher/live-monitoring/session', query: q }
    : '/teacher/live-monitoring'
})

const monitoringSidebarItems = computed(() => [
  { section: 'dashboard', to: '/teacher/dashboard', icon: 'home', label: 'Trang chủ' },
  { section: 'select-exam', to: '/teacher/live-monitoring', icon: 'quiz', label: 'Chọn đề thi' },
  { section: 'session', to: sessionNavTo.value, icon: 'live_tv', label: 'Giám sát' },
  { section: 'incidents', to: '/teacher/exams/review/incidents', icon: 'warning', label: 'Sự cố' },
  { section: 'profile', to: '/teacher/profile', icon: 'account_circle', label: 'Hồ sơ' }
])
</script>
