<template>
  <!--
    TeacherDashboardLayout — standalone content wrapper for the teacher dashboard.
    Does NOT include TopBar or Sidebar — those live in PortalLayout.vue (via AppShell).
    This layout is used when TeacherDashboard renders as a standalone page.
  -->
  <div class="tdl-root min-h-full bg-[var(--ds-bg)]">
    <div class="tdl-content">
      <slot />
    </div>
  </div>
</template>

<script setup>
defineProps({
  activeSection: { type: String, default: 'dashboard' },
  alertCount: { type: Number, default: 0 }
})

defineEmits(['create-exam', 'open-monitoring'])
</script>


<style scoped>
.tdl-root {
  display: flex;
  flex-direction: column;
  min-height: 100%;
}

.tdl-content {
  flex: 1;
  min-width: 0;
}
</style>
