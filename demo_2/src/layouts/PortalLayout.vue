<template>
  <div
    :class="[
      'route-layout route-layout-portal relative min-h-full flex flex-1 flex-col overflow-hidden',
      { 'teacher-portal-stitch': isTeacherRoute, 'student-portal-stitch': isStudentRoute }
    ]"
  >
    <slot />
    <AiChatBubble v-if="isTeacherRoute" />
  </div>
</template>

<script setup>
import { computed, provide, ref } from 'vue'
import { useRoute } from 'vue-router'
import { useTeacherShellDocumentSync } from '../composables/useTeacherShellDocumentSync'
import AiChatBubble from '../components/common/AiChatBubble.vue'

const route = useRoute()
useTeacherShellDocumentSync()

const isTeacherRoute = computed(() => (route.path || '').startsWith('/teacher/'))
const isStudentRoute = computed(() => (route.path || '').startsWith('/student/'))

/** Đồng bộ mở drawer sidebar (mobile) với header */
const mobileNavOpen = ref(false)
provide('portalMobileNavOpen', mobileNavOpen)
</script>

<style scoped>
.route-layout-portal {
  min-height: 100%;
}
</style>
