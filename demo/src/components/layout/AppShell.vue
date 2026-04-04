<template>
  <div class="db-app-shell">
    <!-- Sidebar — unified for all layouts -->
    <Sidebar
      v-if="showSidebar"
      :items="currentSidebarItems"
      :active-section="activeSection"
      :collapsed="sidebarCollapsed"
      :role="layout === 'admin' ? 'admin' : (layout === 'monitoring' ? 'teacher' : role)"
      :user="user"
      :mobile-open="mobileOpen"
      @toggle="sidebarCollapsed = !sidebarCollapsed"
      @close-mobile="mobileOpen = false"
    />

    <!-- Main content area -->
    <main
      class="db-app-shell__main"
      :class="showSidebar ? (sidebarCollapsed ? 'db-app-shell__main--collapsed' : 'db-app-shell__main--expanded') : ''"
    >
      <slot />
    </main>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import Sidebar from './Sidebar.vue'

const props = defineProps({
  layout: {
    type: String,
    default: 'portal',
    validator: (v) => ['portal', 'exam', 'monitoring', 'admin', 'none'].includes(v)
  },
  activeSection: { type: String, default: '' },
  sidebarItems: { type: Array, default: () => [] },
  monitoringSidebarItems: { type: Array, default: () => [] },
  user: { type: Object, default: null },
  role: { type: String, default: 'teacher' },
  brand: { type: Object, default: null }
})

const sidebarCollapsed = ref(
  typeof window !== 'undefined'
    ? sessionStorage.getItem('sidebarCollapsed') === 'true'
    : false
)

const mobileOpen = ref(false)

const showSidebar = computed(() =>
  (props.layout === 'portal' || props.layout === 'monitoring' || props.layout === 'admin')
  && (props.sidebarItems?.length > 0 || props.monitoringSidebarItems?.length > 0)
)

const currentSidebarItems = computed(() => {
  if (props.layout === 'monitoring') return props.monitoringSidebarItems
  return props.sidebarItems
})

watch(sidebarCollapsed, (val) => {
  if (typeof window !== 'undefined') {
    sessionStorage.setItem('sidebarCollapsed', String(val))
    // Reset scroll to top when sidebar width changes to prevent mid-content flicker
    window.scrollTo({ top: 0, behavior: 'instant' })
  }
})
</script>

<style scoped>
.db-app-shell {
  height: 100dvh;
  overflow: hidden;
  background: var(--db-surface-2);
  font-family: var(--db-font);
}

.db-app-shell__main {
  height: 100dvh;
  overflow-y: auto;
  background: var(--db-surface-2);
  transition: padding-left 0.35s cubic-bezier(0.4, 0, 0.2, 1);
  will-change: padding-left;
}

.db-app-shell__main::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}

.db-app-shell__main::-webkit-scrollbar-track {
  background: transparent;
}

.db-app-shell__main::-webkit-scrollbar-thumb {
  background: rgba(148, 163, 184, 0.3);
  border-radius: 999px;
}

.db-app-shell__main::-webkit-scrollbar-thumb:hover {
  background: rgba(148, 163, 184, 0.5);
}

.db-app-shell__main--expanded {
  padding-left: var(--db-sidebar-w);
}

.db-app-shell__main--collapsed {
  padding-left: var(--db-sidebar-collapsed);
}
</style>
