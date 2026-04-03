<template>
  <!-- Portal layout: provides its own stacking context so children z-index work predictably -->
  <div class="route-layout route-layout-portal">
    <!-- Skeleton overlay — scoped to portal so dropdown z-indexes resolve correctly -->
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
      :sidebar-items="teacherSidebarItems"
      :user="authUser"
      role="teacher"
    >
      <slot />
    </AppShell>
    <AiChatBubble v-if="isTeacherRoute" />
  </div>
</template>

<script setup>
import { computed, ref, onMounted, onUnmounted, defineAsyncComponent } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppShell from '../components/layout/AppShell.vue'
import { useAuthStore } from '../stores/authStore'
import { storeToRefs } from 'pinia'

const AiChatBubble = defineAsyncComponent(() =>
  import('../components/common/AiChatBubble.vue')
)

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const { user: authUser } = storeToRefs(auth)

const isNavigating = ref(false)
router.beforeEach(() => { isNavigating.value = true })
router.afterEach(() => { isNavigating.value = false })
router.onError(() => { isNavigating.value = false })

const alertCount = ref(0)
const isTeacherRoute = computed(() => (route.path || '').startsWith('/teacher/'))

const activeSection = computed(() => {
  const path = route.path || ''
  if (path.startsWith('/teacher/dashboard')) return 'dashboard'
  if (path.startsWith('/teacher/classes')) return 'classes'
  if (path.startsWith('/teacher/exams/create') || path.startsWith('/teacher/exams/schedule') || path.startsWith('/teacher/exams/manual') || path.startsWith('/teacher/exams/new-session')) return 'exam'
  if (path.startsWith('/teacher/exams/list')) return 'exam-list'
  if (path.startsWith('/teacher/live-monitoring')) return 'monitoring'
  if (path.startsWith('/teacher/exams/review')) return 'review'
  if (path.startsWith('/teacher/profile')) return 'profile'
  return 'dashboard'
})

const teacherSidebarItems = computed(() => [
  { section: 'dashboard', to: '/teacher/dashboard', icon: 'dashboard', label: 'Trang chủ' },
  { section: 'classes', to: '/teacher/classes', icon: 'groups', label: 'Lớp học' },
  { section: 'exam', to: '/teacher/exams/create', icon: 'quiz', label: 'Tạo đề thi' },
  { section: 'exam-list', to: '/teacher/exams/list', icon: 'list_alt', label: 'Danh sách đề' },
  {
    section: 'monitoring',
    to: '/teacher/live-monitoring',
    icon: 'live_tv',
    label: 'Giám sát',
    badge: alertCount.value > 0 ? String(alertCount.value) : null,
    badgeVariant: 'danger'
  },
  { section: 'profile', to: '/teacher/profile', icon: 'account_circle', label: 'Hồ sơ' }
])
</script>

<style scoped>
.route-layout-portal {
  position: relative;
  isolation: isolate;
  min-height: 100%;
}

/* Skeleton overlay — inside portal stacking context, above all portal children */
.portal-skeleton-overlay {
  position: fixed;
  inset: 0;
  z-index: 9000;
  background: var(--color-bg);
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 32px 28px;
  pointer-events: none;
}

.portal-skeleton-bar {
  height: 18px;
  border-radius: 8px;
  background: linear-gradient(
    90deg,
    var(--color-border) 0%,
    var(--color-surface-muted) 40%,
    var(--color-border) 80%
  );
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
  background: linear-gradient(
    90deg,
    var(--color-border) 0%,
    var(--color-surface-muted) 40%,
    var(--color-border) 80%
  );
  background-size: 200% 100%;
  animation: skeleton-shimmer 1.4s ease-in-out infinite;
}

@keyframes skeleton-shimmer {
  0%   { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

/* Fade in/out skeleton */
.portal-skeleton-enter-active,
.portal-skeleton-leave-active {
  transition: opacity 0.25s ease;
}
.portal-skeleton-enter-from,
.portal-skeleton-leave-to {
  opacity: 0;
}

@media (prefers-reduced-motion: reduce) {
  .portal-skeleton-bar,
  .portal-skeleton-card {
    animation: none;
  }
}
</style>
