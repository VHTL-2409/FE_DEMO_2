<template>
  <!-- Portal layout: provides its own stacking context so children z-index work predictably -->
  <div class="route-layout route-layout-portal">
    <!-- Skeleton overlay -->
    <Transition name="portal-skeleton">
      <div
        v-if="isNavigating"
        class="portal-skeleton-overlay"
        aria-hidden="true"
      >
        <div class="portal-skeleton-bar portal-skeleton-bar--short" />
        <div class="portal-skeleton-bar" />
        <div class="portal-skeleton-bar portal-skeleton-bar--medium" />
        <div class="portal-skeleton-bar portal-skeleton-bar--short" />
        <div class="portal-skeleton-cards">
          <div class="portal-skeleton-card" v-for="i in 3" :key="i" />
        </div>
      </div>
    </Transition>

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

.portal-skeleton-overlay {
  position: fixed;
  inset: 0;
  z-index: 9000;
  background: var(--ds-bg, #f8fafc);
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 32px 28px;
  pointer-events: none;
}

.portal-skeleton-bar {
  height: 18px;
  border-radius: 8px;
  background: linear-gradient(90deg, #e2e8f0 0%, #f1f5f9 40%, #e2e8f0 80%);
  background-size: 200% 100%;
  animation: skeleton-shimmer 1.4s ease-in-out infinite;
  width: 100%;
}
.portal-skeleton-bar--short { width: 45%; }
.portal-skeleton-bar--medium { width: 70%; }

.portal-skeleton-cards {
  display: flex;
  gap: 16px;
  margin-top: 8px;
}
.portal-skeleton-card {
  flex: 1;
  height: 120px;
  border-radius: 12px;
  background: linear-gradient(90deg, #e2e8f0 0%, #f1f5f9 40%, #e2e8f0 80%);
  background-size: 200% 100%;
  animation: skeleton-shimmer 1.4s ease-in-out infinite;
}

@keyframes skeleton-shimmer {
  0%   { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

.portal-skeleton-enter-active,
.portal-skeleton-leave-active {
  transition: opacity 0.25s ease;
}
.portal-skeleton-enter-from,
.portal-skeleton-leave-to {
  opacity: 0;
}
</style>
