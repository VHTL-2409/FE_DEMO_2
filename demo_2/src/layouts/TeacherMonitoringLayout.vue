<template>
  <div
    class="route-layout route-layout-monitoring teacher-portal-stitch relative flex min-h-0 flex-1 flex-col"
  >
    <slot />
  </div>
</template>

<script setup>
import { provide, ref } from 'vue'
import { useTeacherShellDocumentSync } from '../composables/useTeacherShellDocumentSync'

/** Giám sát không bọc PortalLayout — vẫn cần sync sidebar + provide drawer mobile. */
useTeacherShellDocumentSync()

const mobileNavOpen = ref(false)
provide('portalMobileNavOpen', mobileNavOpen)
</script>

<style scoped>
.route-layout-monitoring {
  min-height: 100%;
  isolation: isolate;
}
</style>
