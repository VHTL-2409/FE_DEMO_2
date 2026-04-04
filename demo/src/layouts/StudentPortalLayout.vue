<template>
  <!-- Portal layout: provides its own stacking context so children z-index work predictably -->
  <div class="route-layout route-layout-portal">
    <!-- Skeleton overlay — v-show avoids DOM destroy/recreate flicker during fast navigations -->
    <PortalSkeleton :visible="isNavigating" />

    <AppShell
      layout="portal"
      :active-section="activeSection"
      :sidebar-items="studentSidebarItems"
      :user="authUser"
      role="student"
    >
      <slot />
    </AppShell>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppShell from '../components/layout/AppShell.vue'
import PortalSkeleton from '../components/shared/PortalSkeleton.vue'
import { useAuthStore } from '../stores/authStore'
import { storeToRefs } from 'pinia'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const { user: authUser } = storeToRefs(auth)

const isNavigating = ref(false)
router.beforeEach(() => { isNavigating.value = true })
router.afterEach(() => { isNavigating.value = false })
router.onError(() => { isNavigating.value = false })

const activeSection = computed(() => {
  const path = route.path || ''
  if (path.startsWith('/student/dashboard')) return 'dashboard'
  if (path.startsWith('/student/classes')) return 'classes'
  if (path.startsWith('/student/exam-join')) return 'join'
  if (path.startsWith('/student/exam-waiting-room')) return 'waiting'
  if (path.startsWith('/student/study-history')) return 'history'
  if (path.startsWith('/student/profile')) return 'profile'
  return 'dashboard'
})

const studentSidebarItems = [
  { section: 'dashboard', to: '/student/dashboard', icon: 'home', label: 'Trang chủ' },
  { section: 'classes', to: '/student/classes', icon: 'school', label: 'Lớp học' },
  { section: 'join', to: '/student/exam-join', icon: 'login', label: 'Vào thi' },
  { section: 'history', to: '/student/study-history', icon: 'history', label: 'Lịch sử' },
  { section: 'profile', to: '/student/profile', icon: 'account_circle', label: 'Hồ sơ' }
]
</script>

<style scoped>
.route-layout-portal {
  position: relative;
  isolation: isolate;
  min-height: 100%;
}
</style>
