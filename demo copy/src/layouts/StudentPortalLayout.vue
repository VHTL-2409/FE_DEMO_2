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
import { computed, ref, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppShell from '../components/layout/AppShell.vue'
import PortalSkeleton from '../components/shared/PortalSkeleton.vue'
import { useAuthStore } from '../stores/authStore'
import { storeToRefs } from 'pinia'
import { getStudentActiveSection, studentSidebarItems } from '../config/portalNavigation'

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

const activeSection = computed(() => getStudentActiveSection(route.path || ''))
</script>

<style scoped>
.route-layout-portal {
  position: relative;
  isolation: isolate;
  min-height: 100%;
}
</style>
