<template>
  <div class="app-route-host bg-background-light dark:bg-background-dark">
    <div
      class="route-loading-bar"
      :class="{ 'route-loading-bar--active': routeNavPending }"
      aria-hidden="true"
    />
    <router-view v-slot="{ Component, route }">
      <template v-if="usePortalTransition(route)">
        <transition name="portal-page" mode="out-in">
          <component :is="resolveLayout(route)" :key="route.fullPath || route.path" class="portal-route-root">
            <component :is="Component" />
          </component>
        </transition>
      </template>
      <component :is="resolveLayout(route)" v-else :key="route.fullPath || route.path" class="portal-route-root">
        <component :is="Component" />
      </component>
    </router-view>
  </div>
  <ToastHost />
</template>

<script setup>
import ToastHost from './components/common/ToastHost.vue'
import PortalLayout from './layouts/PortalLayout.vue'
import StudentExamLayout from './layouts/StudentExamLayout.vue'
import TeacherMonitoringLayout from './layouts/TeacherMonitoringLayout.vue'
import { routeNavPending } from './router'

const layouts = {
  portal: PortalLayout,
  exam: StudentExamLayout,
  monitoring: TeacherMonitoringLayout
}

const usePortalTransition = (r) => {
  const p = r?.path || ''
  return p.startsWith('/student/') || p.startsWith('/teacher/')
}

const resolveLayout = (route) => layouts[route?.meta?.layout] || 'div'
</script>
