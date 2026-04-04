<template>
  <div class="ccl">
    <!-- Header zone -->
    <header class="ccl__header">
      <slot name="header" />
    </header>

    <!-- Body zone -->
    <div class="ccl__body">
      <!-- Main content -->
      <main class="ccl__main">
        <div class="ccl__main-inner">
          <slot name="filters" />
          <slot name="content" />
        </div>
      </main>

      <!-- Right sidebar -->
      <aside
        v-if="$slots.sidebar"
        class="ccl__sidebar"
        :class="{ 'ccl__sidebar--collapsed': sidebarCollapsed, 'ccl__sidebar--mobile-hidden': !sidebarVisible }"
      >
        <!-- Collapse toggle -->
        <button
          type="button"
          class="ccl__sidebar-toggle"
          :title="sidebarCollapsed ? 'Mở rộng panel' : 'Thu gọn panel'"
          @click="sidebarCollapsed = !sidebarCollapsed"
        >
          <LucideIcon :name="sidebarCollapsed ? 'chevron_left' : 'chevron_right'" />
        </button>

        <div v-show="!sidebarCollapsed" class="ccl__sidebar-content">
          <slot name="sidebar" />
        </div>
      </aside>
    </div>

    <!-- Bottom action bar -->
    <footer v-if="$slots.footer" class="ccl__footer">
      <slot name="footer" />
    </footer>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'

const sidebarCollapsed = ref(false)
const sidebarVisible = ref(true)

const updateVisibility = () => {
  sidebarVisible.value = window.innerWidth >= 1100
}

onMounted(() => {
  updateVisibility()
  window.addEventListener('resize', updateVisibility)
})

onUnmounted(() => {
  window.removeEventListener('resize', updateVisibility)
})
</script>


<style scoped>
.ccl {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  background: var(--ds-bg);
}

/* Header */
.ccl__header {
  position: sticky;
  top: 0;
  z-index: 100;
  flex-shrink: 0;
}

/* Body */
.ccl__body {
  display: flex;
  flex: 1;
  gap: 0;
  min-height: 0;
}

/* Main */
.ccl__main {
  flex: 1;
  min-width: 0;
  overflow-y: auto;
  padding: 1rem 1rem 2rem;
  max-width: 100%;
}

.ccl__main-inner {
  max-width: 1280px;
  margin: 0 auto;
}

@media (min-width: 1400px) {
  .ccl__main-inner { max-width: 1500px; }
}

@media (min-width: 1600px) {
  .ccl__main-inner { max-width: 1600px; }
}

/* Sidebar */
.ccl__sidebar {
  width: 380px;
  flex-shrink: 0;
  display: flex;
  flex-direction: row;
  align-items: flex-start;
  position: sticky;
  top: 0;
  max-height: 100vh;
  transition: width 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: hidden;
}

.ccl__sidebar--collapsed {
  width: 0;
}

.ccl__sidebar--mobile-hidden {
  display: none;
}

@media (max-width: 1100px) {
  .ccl__sidebar {
    display: none;
  }
}

/* Toggle button */
.ccl__sidebar-toggle {
  width: 32px;
  height: 60px;
  flex-shrink: 0;
  border: 1.5px solid var(--ds-border);
  border-radius: var(--ds-radius-lg);
  background: var(--ds-surface);
  color: var(--ds-text-muted);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: color 0.15s ease, background-color 0.15s ease, border-color 0.15s ease, box-shadow 0.15s ease, transform 0.15s ease;
  margin-top: 1.25rem;
  align-self: flex-start;
  position: relative;
  z-index: 10;
}

.dark .ccl__sidebar-toggle {
  background: var(--ds-gray-800);
  border-color: var(--ds-border-strong);
}

.ccl__sidebar-toggle:hover {
  background: var(--ds-gray-100);
  color: var(--ds-primary);
  border-color: var(--ds-primary-border);
}

.dark .ccl__sidebar-toggle:hover {
  background: var(--ds-gray-700);
  border-color: var(--ds-primary-border);
}


/* Sidebar content */
.ccl__sidebar-content {
  flex: 1;
  min-width: 0;
  padding: 1rem 0.75rem 1rem 0.5rem;
  display: flex;
  flex-direction: column;
  gap: 1rem;
  overflow-x: hidden;
  overflow-y: auto;
  max-height: 100vh;
  position: sticky;
  top: 0;
  scrollbar-width: thin;
  scrollbar-color: var(--ds-gray-200) transparent;
}

.ccl__sidebar-content > * {
  width: 100%;
  box-sizing: border-box;
}

.ccl__sidebar-content::-webkit-scrollbar { width: 3px; }
.ccl__sidebar-content::-webkit-scrollbar-thumb { background: var(--ds-gray-200); border-radius: 2px; }
.dark .ccl__sidebar-content::-webkit-scrollbar-thumb { background: var(--ds-gray-600); }

/* Footer */
.ccl__footer {
  flex-shrink: 0;
  border-top: 2px solid var(--ds-border);
  background: var(--ds-surface);
}

.dark .ccl__footer {
  border-top-color: var(--ds-border-strong);
}
</style>
@media (prefers-reduced-motion: reduce) {
  * {
    transition-duration: 0.01ms !important;
    animation-duration: 0.01ms !important;
  }
}