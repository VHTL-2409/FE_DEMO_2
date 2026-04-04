<template>
  <div class="app-route-host">
    <!-- Teacher portal routes -->
    <PortalLayout v-if="isTeacherRoute">
      <router-view v-slot="{ Component }">
        <Transition name="page" mode="out-in">
          <component :is="Component" />
        </Transition>
      </router-view>
    </PortalLayout>

    <!-- Student portal routes -->
    <StudentPortalLayout v-else-if="isStudentPortalRoute">
      <router-view v-slot="{ Component }">
        <Transition name="page" mode="out-in">
          <component :is="Component" />
        </Transition>
      </router-view>
    </StudentPortalLayout>

    <!-- Student exam layout -->
    <StudentExamLayout v-else-if="isExamRoute">
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

const isTeacherRoute = computed(() => (route.path || '').startsWith('/teacher/'))
const isStudentPortalRoute = computed(() =>
  (route.path || '').startsWith('/student/') && route.path !== '/student/exam-interface'
)
const isExamRoute = computed(() => route.path === '/student/exam-interface')

</script>

<style>
/* ── Page transition — admin-style: blur + scale + slide ── */
.page-enter-active {
  transition:
    opacity 0.28s cubic-bezier(0.4, 0, 0.2, 1),
    transform 0.35s cubic-bezier(0.22, 1, 0.36, 1),
    filter 0.28s ease;
}
.page-leave-active {
  transition:
    opacity 0.18s cubic-bezier(0.4, 0, 1, 1),
    transform 0.22s cubic-bezier(0.4, 0, 1, 1),
    filter 0.18s ease;
}
.page-enter-from {
  opacity: 0;
  transform: translateX(22px) scale(0.985);
  filter: blur(1px);
}
.page-leave-to {
  opacity: 0;
  transform: translateX(-14px) scale(0.99);
  filter: blur(0.5px);
}
.page-enter-to,
.page-leave-from {
  opacity: 1;
  transform: translateX(0) scale(1);
  filter: blur(0);
}

@media (prefers-reduced-motion: reduce) {
  .page-enter-active,
  .page-leave-active {
    transition-duration: 0.01ms !important;
  }
  .page-enter-from,
  .page-leave-to {
    transform: none !important;
    filter: none !important;
  }
}
</style>
