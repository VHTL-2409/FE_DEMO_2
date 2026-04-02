<template>
  <!-- Teacher portal layout — top nav + content, no sidebar -->
  <CosmosLayout
    :nav-items="teacherNavItems"
    :user="authUser"
    role="teacher"
  >
    <slot />
  </CosmosLayout>
  <AiChatBubble v-if="isTeacherRoute" />
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRoute } from 'vue-router'
import CosmosLayout from './CosmosLayout.vue'
import { useAuthStore } from '../stores/authStore'
import { storeToRefs } from 'pinia'
import { defineAsyncComponent } from 'vue'

const AiChatBubble = defineAsyncComponent(() =>
  import('../components/common/AiChatBubble.vue')
)

const route = useRoute()
const auth = useAuthStore()
const { user: authUser } = storeToRefs(auth)

const alertCount = ref(0)
const isTeacherRoute = computed(() => (route.path || '').startsWith('/teacher/'))

const teacherNavItems = computed(() => [
  { path: '/teacher/dashboard', icon: 'home', label: 'Trang chủ' },
  { path: '/teacher/classes', icon: 'groups', label: 'Lớp học' },
  { path: '/teacher/exams/create', icon: 'quiz', label: 'Tạo đề thi' },
  { path: '/teacher/exams/list', icon: 'list', label: 'Danh sách đề' },
  { path: '/teacher/live-monitoring', icon: 'monitor', label: 'Giám sát', badge: alertCount > 0 ? String(alertCount) : null },
  { path: '/teacher/exams/review/summary', icon: 'report', label: 'Báo cáo' },
])
</script>
