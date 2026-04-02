<template>
  <div class="app-shell min-h-full" style="background: var(--cosmos-bg-0);">
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

    <!-- Main content — GPU-accelerated transform transition instead of margin reflow -->
    <main
      class="app-shell__main flex-1 overflow-y-auto"
      :class="showSidebar ? (sidebarCollapsed ? 'app-shell__main--collapsed' : 'app-shell__main--expanded') : ''"
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

// Collapse state persisted via sessionStorage — shared across all layouts/routes
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

// Persist collapse state via sessionStorage
watch(sidebarCollapsed, (val) => {
  if (typeof window !== 'undefined') {
    sessionStorage.setItem('sidebarCollapsed', String(val))
  }
})
</script>


<style scoped>
.app-shell {
  height: 100dvh;
  overflow: hidden;
}

.app-shell__main {
  background: var(--cosmos-bg-0);
  height: 100dvh;
  overflow-y: auto;
  will-change: margin;
  transition: margin 0.28s cubic-bezier(0.4, 0, 0.2, 1);
}

.app-shell__main--expanded {
  margin-left: 260px;
}

.app-shell__main--collapsed {
  margin-left: 72px;
}

/* Scrollbar */
.app-shell__main::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}
.app-shell__main::-webkit-scrollbar-track {
  background: transparent;
}
.app-shell__main::-webkit-scrollbar-thumb {
  background: rgba(148, 163, 184, 0.4);
  border-radius: 999px;
}
.app-shell__main::-webkit-scrollbar-thumb:hover {
  background: rgba(148, 163, 184, 0.6);
}
</style>
