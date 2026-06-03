<template>
  <div class="db-app-shell">
    <button
      v-if="showSidebar"
      type="button"
      class="db-app-shell__mobile-menu"
      :aria-label="mobileOpen ? 'Đóng menu' : 'Mở menu'"
      :aria-expanded="mobileOpen"
      @click="mobileOpen = true"
    >
      <LucideIcon name="menu" :size="20" />
    </button>
    <!-- Sidebar — unified for all layouts -->
    <Sidebar
      v-if="showSidebar"
      :items="currentSidebarItems"
      :active-section="activeSection"
      :collapsed="sidebarCollapsed"
      :role="layout === 'admin' ? 'admin' : role"
      :user="user"
      :mobile-open="mobileOpen"
      @toggle="sidebarCollapsed = !sidebarCollapsed"
      @close-mobile="mobileOpen = false"
    />

    <!-- Main content area -->
    <main
      ref="mainScrollEl"
      class="db-app-shell__main"
      :class="showSidebar ? (sidebarCollapsed ? 'db-app-shell__main--collapsed' : 'db-app-shell__main--expanded') : ''"
    >
      <slot />
    </main>
  </div>
</template>

<script setup>
import { computed, nextTick, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import Sidebar from './Sidebar.vue'
import LucideIcon from '../common/LucideIcon.vue'

const props = defineProps({
  layout: {
    type: String,
    default: 'portal',
    validator: (v) => ['portal', 'exam', 'admin', 'none'].includes(v)
  },
  activeSection: { type: String, default: '' },
  sidebarItems: { type: Array, default: () => [] },
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
const mainScrollEl = ref(null)
const route = useRoute()

const showSidebar = computed(() =>
  (props.layout === 'portal' || props.layout === 'admin')
  && (props.sidebarItems?.length > 0)
)

const currentSidebarItems = computed(() => props.sidebarItems)

const resetRouteScroll = () => {
  if (mainScrollEl.value) {
    mainScrollEl.value.scrollTop = 0
    mainScrollEl.value.scrollLeft = 0
  }

  if (typeof window !== 'undefined') {
    window.scrollTo({ top: 0, left: 0, behavior: 'auto' })
  }
}

watch(sidebarCollapsed, (val) => {
  if (typeof window !== 'undefined') {
    sessionStorage.setItem('sidebarCollapsed', String(val))
  }
})

watch(
  () => route.path,
  async () => {
    await nextTick()
    requestAnimationFrame(() => {
      resetRouteScroll()
    })
  },
  { flush: 'post' }
)
</script>

<style scoped>
.db-app-shell {
  height: 100dvh;
  overflow: hidden;
  background: var(--ds-bg);
  font-family: var(--ds-font);
}

.db-app-shell__main {
  height: 100dvh;
  overflow-y: auto;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.74) 0%, rgba(248, 250, 252, 0) 220px),
    var(--ds-bg);
  /* Keep the sidebar transition without forcing GPU text compositing. */
  transition: padding-left 0.35s cubic-bezier(0.4, 0, 0.2, 1);
  contain: layout;
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
  padding-left: var(--portal-sidebar-width);
}

.db-app-shell__main--collapsed {
  padding-left: var(--portal-sidebar-collapsed);
}

.db-app-shell__mobile-menu {
  display: none;
  position: fixed;
  top: 0.875rem;
  left: 0.875rem;
  z-index: 45;
  width: 40px;
  height: 40px;
  border-radius: var(--ds-radius-lg, 14px);
  border: 1px solid var(--ds-border);
  background: var(--ds-surface);
  color: var(--ds-text);
  box-shadow: var(--ds-shadow-md);
  align-items: center;
  justify-content: center;
  cursor: pointer;
}

.db-app-shell__mobile-menu:hover {
  background: var(--ds-surface-muted);
}

@media (max-width: 768px) {
  .db-app-shell__mobile-menu {
    display: flex;
  }

  .db-app-shell__main,
  .db-app-shell__main--expanded,
  .db-app-shell__main--collapsed {
    padding-left: 0;
    padding-top: 3.25rem;
  }
}

/* GPU: sidebar padding transitions smoothly; reduced-motion disables it */
@media (prefers-reduced-motion: reduce) {
  .db-app-shell__main {
    transition: none;
  }
  /* page transition is handled globally in animation.css */
}
</style>
