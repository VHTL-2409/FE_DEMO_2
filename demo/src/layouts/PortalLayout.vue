<template>
  <!-- Portal layout: provides its own stacking context so children z-index work predictably -->
  <div class="route-layout route-layout-portal">
    <!-- Skeleton overlay — v-show avoids DOM destroy/recreate flicker during fast navigations -->
    <PortalSkeleton :visible="isNavigating" />

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
import PortalSkeleton from '../components/shared/PortalSkeleton.vue'
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
</style>
