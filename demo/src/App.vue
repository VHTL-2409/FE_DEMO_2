<template>
  <div class="app-route-host">
    <!-- Teacher portal: dashboard, exams, classes, live monitoring — shared sidebar -->
    <PortalLayout v-if="currentLayout === 'portal'">
      <router-view v-slot="{ Component }">
        <Transition name="page" mode="out-in">
          <component :is="Component" />
        </Transition>
      </router-view>
    </PortalLayout>

    <!-- Student portal routes -->
    <StudentPortalLayout v-else-if="currentLayout === 'studentPortal'">
      <router-view v-slot="{ Component }">
        <Transition name="page" mode="out-in">
          <component :is="Component" />
        </Transition>
      </router-view>
    </StudentPortalLayout>

    <!-- Student exam layout -->
    <StudentExamLayout v-else-if="currentLayout === 'exam'">
      <router-view v-slot="{ Component }">
        <Transition name="page" mode="out-in">
          <component :is="Component" />
        </Transition>
      </router-view>
    </StudentExamLayout>

    <!-- Admin (nested children), public, fallback -->
    <router-view v-else />
  </div>
  <ToastHost />
</template>

<script setup>
import { computed, defineAsyncComponent } from 'vue'
import { useRoute } from 'vue-router'
import ToastHost from './components/common/ToastHost.vue'

const PortalLayout = defineAsyncComponent(() => import('./layouts/PortalLayout.vue'))
const StudentPortalLayout = defineAsyncComponent(() => import('./layouts/StudentPortalLayout.vue'))
const StudentExamLayout = defineAsyncComponent(() => import('./layouts/StudentExamLayout.vue'))

const route = useRoute()
const currentLayout = computed(() => route.meta?.layout || 'default')

</script>

<style>
/* ── Page transition — opacity only to keep text crisp ── */
.page-enter-active {
  transition: opacity 0.18s cubic-bezier(0.4, 0, 0.2, 1);
}
.page-leave-active {
  transition: opacity 0.14s cubic-bezier(0.4, 0, 1, 1);
}
.page-enter-from {
  opacity: 0;
}
.page-leave-to {
  opacity: 0;
}
.page-enter-to,
.page-leave-from {
  opacity: 1;
}

@media (prefers-reduced-motion: reduce) {
  .page-enter-active,
  .page-leave-active {
    transition-duration: 0.01ms !important;
  }
  .page-enter-from,
  .page-leave-to {
    filter: none !important;
  }
}
</style>
