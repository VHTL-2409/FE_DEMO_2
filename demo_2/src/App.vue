<template>
  <div class="app-route-host" data-demo2="1" style="background: var(--cosmos-bg-0); min-height: 100vh;">
    <!-- Route loading indicator -->
    <div
      class="route-loading-bar"
      :class="{ 'route-loading-bar--active': routeNavPending }"
      aria-hidden="true"
    />

    <router-view v-slot="{ Component, route }">
      <template v-if="usePortalTransition(route)">
        <Transition name="gs-page" mode="out-in">
          <component
            :is="resolveLayout(route)"
            :key="route.fullPath || route.path"
          >
            <component :is="Component" />
          </component>
        </Transition>
      </template>
      <component
        :is="resolveLayout(route)"
        v-else
        :key="route.fullPath || route.path"
      >
        <component :is="Component" />
      </component>
    </router-view>
  </div>
  <ToastHost />
</template>

<script setup>
import ToastHost from './components/common/ToastHost.vue'
import PortalLayout from './layouts/PortalLayout.vue'
import StudentPortalLayout from './layouts/StudentPortalLayout.vue'
import StudentExamLayout from './layouts/StudentExamLayout.vue'
import TeacherMonitoringLayout from './layouts/TeacherMonitoringLayout.vue'
import { routeNavPending } from './router'

const layouts = {
  portal: PortalLayout,
  studentPortal: StudentPortalLayout,
  exam: StudentExamLayout,
  monitoring: TeacherMonitoringLayout
}

const usePortalTransition = (r) => {
  const p = r?.path || ''
  return p.startsWith('/student/') || p.startsWith('/teacher/')
}

const resolveLayout = (route) => {
  const layout = route?.meta?.layout
  if (layout === 'studentPortal') return layouts.studentPortal
  if (layout === 'portal') return layouts.portal
  if (layout === 'exam') return layouts.exam
  if (layout === 'monitoring') return layouts.monitoring
  return 'div'
}
</script>
