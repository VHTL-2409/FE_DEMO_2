<template>
  <!-- Student portal layout — top nav + content, no sidebar -->
  <CosmosLayout
    :nav-items="studentNavItems"
    :user="authUser"
    role="student"
  >
    <slot />
  </CosmosLayout>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import CosmosLayout from './CosmosLayout.vue'
import { useAuthStore } from '../stores/authStore'
import { storeToRefs } from 'pinia'

const route = useRoute()
const auth = useAuthStore()
const { user: authUser } = storeToRefs(auth)

const studentNavItems = computed(() => [
  { path: '/student/dashboard', icon: 'home', label: 'Trang chủ' },
  { path: '/student/schedule', icon: 'calendar', label: 'Lịch thi' },
  { path: '/student/classes', icon: 'groups', label: 'Lớp học' },
  { path: '/student/generate-practice-test', icon: 'practice', label: 'Luyện tập' },
  { path: '/student/study-history', icon: 'history', label: 'Lịch sử' },
])
</script>
