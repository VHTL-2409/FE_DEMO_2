<template>
  <div
    :class="[
      'route-layout route-layout-portal min-h-full',
      { 'teacher-portal-stitch': isTeacherRoute }
    ]"
  >
    <slot />
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const isTeacherRoute = computed(() => (route.path || '').startsWith('/teacher/'))
</script>

<style scoped>
.route-layout-portal {
  min-height: 100%;
}
</style>
