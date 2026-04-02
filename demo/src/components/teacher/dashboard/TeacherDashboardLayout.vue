<template>
  <!--
    TeacherDashboardLayout — standalone content wrapper for the teacher dashboard.
    Does NOT include TopBar or Sidebar — those live in PortalLayout.vue (via AppShell).
    This layout is used when TeacherDashboard renders as a standalone page.
  -->
  <div class="tdl-root min-h-full bg-[var(--ds-bg)]">
    <!-- Optional: custom topbar overlay for premium feel (search bar, quick actions) -->
    <div class="tdl-topbar-overlay">
      <div class="tdl-topbar-overlay__inner">
        <!-- Quick search hint -->
        <div class="tdl-topbar-overlay__search-hint">
          <LucideIcon name="search" />
          <span class="tdl-topbar-overlay__search-text">Nhấn</span>
          <kbd class="tdl-topbar-overlay__kbd">/</kbd>
          <span class="tdl-topbar-overlay__search-text">để tìm kiếm nhanh</span>
        </div>
        <!-- Page title area -->
        <div class="tdl-topbar-overlay__breadcrumb">
          <LucideIcon name="dashboard" />
          <span class="tdl-topbar-overlay__breadcrumb-sep">/</span>
          <span class="tdl-topbar-overlay__breadcrumb-current">Trang chủ</span>
        </div>
      </div>
    </div>

    <!-- Main content slot -->
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

.tdl-topbar-overlay {
  padding: 1rem 0 0;
}

.tdl-topbar-overlay__inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  max-width: 1440px;
  margin: 0 auto;
  padding: 0 1rem;
}

@media (min-width: 1400px) {
  .tdl-topbar-overlay__inner { max-width: 1440px; }
}

@media (min-width: 1600px) {
  .tdl-topbar-overlay__inner { max-width: 1600px; }
}

@media (min-width: 1920px) {
  .tdl-topbar-overlay__inner { max-width: 1800px; }
}

.tdl-topbar-overlay__search-hint {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  padding: 0.375rem 0.875rem;
  background: var(--ds-gray-50);
  border: 1px solid var(--ds-border);
  border-radius: var(--ds-radius-full);
  font-size: 0.75rem;
  color: var(--ds-text-muted);
  cursor: default;
}

.tdl-topbar-overlay__search-icon {
  font-size: 1rem;
  color: var(--ds-text-muted);
}

.tdl-topbar-overlay__search-text {
  font-weight: 500;
}

.tdl-topbar-overlay__kbd {
  padding: 0.1rem 0.4rem;
  background: var(--ds-gray-200);
  border: 1px solid var(--ds-gray-300);
  border-radius: 4px;
  font-size: 0.65rem;
  font-family: monospace;
  color: var(--ds-text-secondary);
}

.tdl-topbar-overlay__breadcrumb {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  font-size: 0.75rem;
  color: var(--ds-text-muted);
}


.tdl-topbar-overlay__breadcrumb-sep {
  opacity: 0.5;
}

.tdl-topbar-overlay__breadcrumb-current {
  font-weight: 600;
  color: var(--ds-text-secondary);
}

.tdl-content {
  flex: 1;
  min-width: 0;
}
</style>
