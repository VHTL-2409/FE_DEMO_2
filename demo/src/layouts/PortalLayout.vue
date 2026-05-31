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
import { computed, ref, onUnmounted, defineAsyncComponent } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppShell from '../components/layout/AppShell.vue'
import PortalSkeleton from '../components/shared/PortalSkeleton.vue'
import { useAuthStore } from '../stores/authStore'
import { storeToRefs } from 'pinia'
import { getTeacherActiveSection, getTeacherSidebarItems } from '../config/portalNavigation'

const AiChatBubble = defineAsyncComponent(() =>
  import('../components/common/AiChatBubble.vue')
)

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const { user: authUser } = storeToRefs(auth)

const isNavigating = ref(false)
const removeBeforeEach = router.beforeEach(() => { isNavigating.value = true })
const removeAfterEach = router.afterEach(() => { isNavigating.value = false })
const removeErrorHandler = router.onError(() => { isNavigating.value = false })

onUnmounted(() => {
  removeBeforeEach()
  removeAfterEach()
  removeErrorHandler()
})

const alertCount = ref(0)
const isTeacherRoute = computed(() => (route.path || '').startsWith('/teacher/'))

const activeSection = computed(() => getTeacherActiveSection(route.path || '', route.query))
const teacherSidebarItems = computed(() => getTeacherSidebarItems(alertCount.value))
</script>

<style scoped>
.route-layout-portal {
  position: relative;
  isolation: isolate;
  min-height: 100%;
}
</style>
