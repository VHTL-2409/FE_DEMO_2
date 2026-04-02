<template>
  <!-- Mobile overlay -->
  <Teleport to="body">
    <div
      v-if="mobileOpen"
      class="fixed inset-0 z-30"
      style="background: rgba(0,0,0,0.6); backdrop-filter: blur(4px);"
      @click="$emit('close-mobile')"
    />
  </Teleport>

  <!-- Sidebar — Prismatic Cosmos dark glass -->
  <aside
    :class="[
      'ln-sidebar fixed left-0 top-0 z-40 flex h-full flex-col border-r',
      collapsed ? 'ln-sidebar--collapsed' : 'ln-sidebar--expanded'
    ]"
  >
    <!-- Header: logo + brand -->
    <div class="ln-sidebar__header border-b px-4 py-3">
      <div class="flex items-center gap-3">
        <!-- Logo — gradient icon -->
        <div
          class="flex size-9 shrink-0 items-center justify-center rounded-lg"
          style="background: linear-gradient(135deg, var(--cosmos-teal) 0%, var(--cosmos-rose) 100%); box-shadow: var(--cosmos-shadow-teal);"
        >
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
            <circle cx="12" cy="12" r="10" stroke="rgba(255,255,255,0.8)" stroke-width="1.5" fill="none"/>
            <circle cx="12" cy="12" r="4" fill="rgba(255,255,255,0.8)"/>
            <defs>
              <linearGradient id="sidebarGrad" x1="0" y1="0" x2="24" y2="24">
                <stop offset="0%" stop-color="#06b6d4"/>
                <stop offset="100%" stop-color="#d946ef"/>
              </linearGradient>
            </defs>
          </svg>
        </div>

        <!-- Brand name -->
        <div class="ln-sidebar__brand overflow-hidden">
          <p class="text-sm font-extrabold ln-heading-display tracking-tight whitespace-nowrap" style="color: var(--cosmos-teal);">EduExam</p>
          <p class="text-[10px] font-bold uppercase tracking-widest whitespace-nowrap" style="color: var(--cosmos-text-muted);">{{ roleLabel }}</p>
        </div>
      </div>
    </div>

    <!-- Navigation -->
    <nav class="ln-sidebar-nav flex-1 overflow-y-auto px-2.5 py-4 space-y-0.5">
      <RouterLink
        v-for="item in items"
        :key="item.section"
        :to="item.to"
        :class="navItemClass(item.section)"
        class="ln-sidebar-nav-item group relative flex items-center gap-3 rounded-xl px-3.5 py-2.5 text-sm font-semibold"
        @click="$emit('close-mobile')"
      >
        <!-- Active left bar -->
        <span
          v-if="activeSection === item.section && !collapsed"
          class="absolute left-0 top-1/2 -translate-y-1/2 h-6 w-[3px] rounded-r-full"
          style="background: linear-gradient(180deg, var(--cosmos-teal), var(--cosmos-rose)); box-shadow: 0 0 12px var(--cosmos-teal);"
        />

        <!-- Icon -->
        <span class="ln-sidebar-nav-icon-wrap shrink-0 flex items-center justify-center w-5 h-5">
          <LucideIcon
            :name="item.icon"
            :size="20"
            class="ln-sidebar-nav-icon"
          />
        </span>

        <!-- Label -->
        <span class="ln-sidebar-nav-label shrink-0 truncate leading-snug whitespace-nowrap overflow-hidden">
          {{ item.label }}
        </span>

        <!-- Badge -->
        <span
          v-if="item.badge && !collapsed"
          class="ml-auto rounded-full px-2 py-0.5 text-[10px] font-bold flex-shrink-0"
          :style="item.badgeVariant === 'danger'
            ? 'background: rgba(239, 68, 68, 0.15); color: #ef4444;'
            : 'background: rgba(6, 182, 212, 0.15); color: var(--cosmos-teal);'"
        >
          {{ item.badge }}
        </span>

        <!-- Hover tooltip for collapsed -->
        <span
          v-if="collapsed"
          class="ln-sidebar-tooltip absolute left-full ml-2 hidden group-hover:flex items-center px-2 py-1 rounded-lg shadow-lg text-xs font-semibold whitespace-nowrap z-50"
          style="background: rgba(6, 11, 24, 0.98); border: 1px solid var(--cosmos-teal-border); color: var(--cosmos-text);"
        >
          {{ item.label }}
        </span>
      </RouterLink>
    </nav>

    <!-- Bottom: notifications + user + collapse toggle -->
    <div class="border-t p-2.5 space-y-0.5">
      <!-- Notification bell -->
      <div class="relative">
        <button
          type="button"
          class="ln-sidebar-nav-item group flex w-full items-center gap-3 rounded-xl px-3.5 py-2.5 text-sm font-semibold"
          style="color: var(--cosmos-text-secondary);"
          aria-label="Thông báo"
          @click="showNotifications = !showNotifications"
        >
          <LucideIcon name="notifications" class="ln-sidebar-nav-icon shrink-0 relative" />
          <span
            v-if="hasUnread"
            class="absolute -top-1 -right-1 size-2 rounded-full"
            style="background: var(--ln-danger);"
          />
          <span class="ln-sidebar-nav-label shrink-0 truncate leading-snug whitespace-nowrap overflow-hidden text-left">Thông báo</span>
        </button>

        <!-- Notification dropdown -->
        <Teleport to="body">
          <div
            v-if="showNotifications"
            class="fixed z-[100] w-80 overflow-y-auto rounded-2xl shadow-lg overflow-hidden"
            style="background: rgba(6, 11, 24, 0.98); border: 1px solid var(--cosmos-border); box-shadow: 0 8px 32px rgba(6, 182, 212, 0.15), var(--cosmos-shadow-xl);"
            :style="notifDropdownStyle"
          >
            <div class="flex items-center justify-between border-b px-4 py-3" style="border-color: var(--ln-border);">
              <span class="font-bold text-sm" style="color: var(--cosmos-text);">Thông báo</span>
              <button
                v-if="hasUnread"
                type="button"
                class="text-[11px] font-semibold hover:underline"
                style="color: var(--cosmos-teal);"
                @click="markAllAsRead"
              >
                Đánh dấu đã đọc
              </button>
            </div>
            <div class="max-h-64 overflow-y-auto">
              <div v-if="!notifications.length" class="p-6 text-center text-sm" style="color: var(--cosmos-text-muted);">
                Chưa có thông báo.
              </div>
              <div
                v-for="n in notifications"
                :key="n.id"
                :class="n.read ? '' : 'bg-[rgba(6,182,212,0.1)]'"
                class="cursor-pointer border-b px-4 py-3 transition-colors last:border-b-0"
                style="border-color: var(--cosmos-border);"
                :style="n.read ? '' : 'background: rgba(6, 182, 212, 0.1);'"
                @click="markAsRead(n.id)"
              >
                <p class="text-sm font-semibold" style="color: var(--cosmos-text);">{{ n.title }}</p>
                <p class="mt-0.5 text-xs" style="color: var(--cosmos-text-muted);">{{ n.message }}</p>
              </div>
            </div>
          </div>
          <div
            v-if="showNotifications"
            class="fixed inset-0 z-[99]"
            @click="showNotifications = false"
          />
        </Teleport>
      </div>

      <!-- User section -->
      <div v-if="user" class="flex items-center gap-2.5 rounded-xl px-3.5 py-2.5 mt-1 transition-colors cursor-pointer"
        style="color: var(--cosmos-text-secondary);"
        @click="handleLogout"
      >
        <div class="size-8 shrink-0 rounded-full flex items-center justify-center text-xs font-bold"
          style="background: linear-gradient(135deg, var(--cosmos-teal) 0%, var(--cosmos-rose) 100%); box-shadow: 0 0 16px rgba(6, 182, 212, 0.4); color: white;">
          {{ userInitial }}
        </div>
        <div class="ln-sidebar-nav-label shrink-0 min-w-0 flex-1 overflow-hidden">
          <p class="truncate text-xs font-semibold" style="color: var(--cosmos-text);">{{ user.username }}</p>
          <p class="truncate text-[10px] capitalize" style="color: var(--cosmos-text-muted);">{{ role }}</p>
        </div>
        <button
          type="button"
          class="flex size-6 shrink-0 items-center justify-center rounded-full transition-colors"
          style="color: var(--cosmos-text-muted);"
          title="Đăng xuất"
          @click.stop="handleLogout"
        >
          <LucideIcon name="logout" size="16" />
        </button>
      </div>

      <!-- Collapse/expand toggle -->
      <button
        type="button"
        class="ln-sidebar-collapse-btn group flex w-full items-center gap-3 rounded-xl px-3.5 py-2.5 text-sm font-semibold transition-colors"
        style="color: var(--cosmos-text-muted);"
        :title="collapsed ? 'Mở rộng menu' : 'Thu gọn menu'"
        @click="$emit('toggle')"
      >
        <LucideIcon name="chevron_left" class="shrink-0 transition-transform duration-300" :class="collapsed ? '' : 'rotate-180'" />
        <span class="ln-sidebar-nav-label shrink-0 whitespace-nowrap overflow-hidden">Thu gọn menu</span>
      </button>
    </div>
  </aside>
</template>

<script setup>
import { computed, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { clearAuthSession } from '../../services/authService'
import { useNotifications } from '../../composables/useNotifications'

const props = defineProps({
  items: { type: Array, default: () => [] },
  activeSection: { type: String, default: '' },
  collapsed: { type: Boolean, default: false },
  mobileOpen: { type: Boolean, default: false },
  user: { type: Object, default: null },
  role: { type: String, default: 'teacher' }
})

defineEmits(['toggle', 'close-mobile'])

const router = useRouter()
const { notifications, hasUnread, markAsRead, markAllAsRead } = useNotifications()
const showNotifications = ref(false)

const roleLabel = computed(() => {
  const labels = { teacher: 'Cong giao vien', student: 'Cong hoc sinh', admin: 'Cong quan tri' }
  return labels[props.role] || props.role
})

const userInitial = computed(() => {
  if (!props.user?.username) return '?'
  return props.user.username.slice(0, 1).toUpperCase()
})

const navItemClass = (section) => {
  if (props.activeSection === section) {
    if (props.collapsed) {
      return `text-white`
    }
    return `text-[var(--ln-cyan)]`
  }
  return `text-[var(--cosmos-text-secondary)]`
}

const notifDropdownStyle = computed(() => ({
  right: '8px',
  bottom: '80px',
  width: '320px'
}))

const handleLogout = () => {
  clearAuthSession()
  router.push('/login')
}
</script>

<style scoped>
/* Sidebar background — Prismatic Cosmos dark glass */
.ln-sidebar {
  background: rgba(6, 11, 24, 0.92);
  backdrop-filter: blur(40px);
  -webkit-backdrop-filter: blur(40px);
  border-color: var(--cosmos-border);
  box-shadow: var(--cosmos-shadow-lg);
  transition: width 0.35s var(--cosmos-ease-out);
  overflow: hidden;
  white-space: nowrap;
  will-change: width;
}

.ln-sidebar--expanded { width: 260px; }
.ln-sidebar--collapsed { width: 72px; }

/* Nav label transitions */
.ln-sidebar-nav-label {
  overflow: hidden;
  opacity: 1;
  flex-shrink: 0;
  flex-grow: 0;
  transition:
    opacity 0.25s var(--cosmos-ease-out),
    max-width 0.35s var(--cosmos-ease-out),
    transform 0.25s var(--cosmos-ease-out);
  width: auto;
  max-width: 200px;
  transform: translateX(0);
}

.ln-sidebar--collapsed .ln-sidebar-nav-label {
  opacity: 0;
  max-width: 0;
  pointer-events: none;
  transform: translateX(-8px);
}

/* Nav item: icon centered when collapsed */
.ln-sidebar--collapsed .ln-sidebar-nav-item {
  justify-content: center !important;
  align-items: center !important;
  gap: 0 !important;
  padding-left: 0 !important;
  padding-right: 0 !important;
  margin-left: 0 !important;
  margin-right: 0 !important;
  transition: all 0.25s var(--cosmos-ease-out);
}

/* Nav icon */
.ln-sidebar-nav-icon {
  flex-shrink: 0;
  transition: opacity 0.25s ease, color 0.15s ease, transform 0.2s ease;
}

.ln-sidebar--collapsed .ln-sidebar-nav-icon-wrap {
  margin: 0 auto;
}

/* Nav hover */
.ln-sidebar-nav-item {
  transition: all 0.2s var(--cosmos-ease-out);
}

.ln-sidebar-nav-item:hover:not(.router-link-active) {
  background: rgba(6, 182, 212, 0.12);
  color: var(--cosmos-teal) !important;
  transform: translateX(4px);
  border-color: rgba(6, 182, 212, 0.3);
}

.ln-sidebar-nav-item.router-link-active {
  background: linear-gradient(90deg, rgba(6, 182, 212, 0.18) 0%, rgba(217, 70, 239, 0.1) 100%);
  box-shadow: inset 0 0 0 1px var(--cosmos-teal-border);
  color: var(--cosmos-teal) !important;
}

.ln-sidebar--collapsed .ln-sidebar-nav-item:hover {
  transform: translateX(0) scale(1.05);
}

/* Brand: fade + collapse */
.ln-sidebar__brand {
  overflow: hidden;
  opacity: 1;
  transition:
    opacity 0.25s var(--cosmos-ease-out),
    max-width 0.35s var(--cosmos-ease-out);
  max-width: 200px;
}

.ln-sidebar--collapsed .ln-sidebar__brand {
  opacity: 0;
  max-width: 0;
}

/* Tooltip for collapsed */
.ln-sidebar-tooltip {
  pointer-events: none;
  opacity: 0;
  transform: translateX(-4px);
  transition: opacity 0.2s ease, transform 0.2s ease;
}

.ln-sidebar-nav-item:hover .ln-sidebar-tooltip {
  opacity: 1;
  transform: translateX(0);
}

/* Nav scrollbar */
.ln-sidebar-nav::-webkit-scrollbar { width: 4px; }
.ln-sidebar-nav::-webkit-scrollbar-thumb { background: rgba(255,255,255,0.1); border-radius: 999px; }
.ln-sidebar-nav::-webkit-scrollbar-thumb:hover { background: var(--cosmos-teal-border); }

/* Notification dropdown animation */
.ln-sidebar-nav-item:hover .ds-notif-dropdown,
.ln-sidebar-nav-item:focus-within .ds-notif-dropdown {
  animation: ln-dropdown-in 0.15s var(--cosmos-ease-out);
}

@keyframes ln-dropdown-in {
  from { opacity: 0; transform: translateY(4px) scale(0.98); }
  to   { opacity: 1; transform: translateY(0) scale(1); }
}

/* Header */
.ln-sidebar__header {
  display: flex;
  align-items: center;
  flex-shrink: 0;
  border-color: var(--cosmos-border);
}

/* Override glass-system classes in dark context */
:deep(.ds-sidebar) {
  background: rgba(6, 11, 24, 0.92) !important;
  backdrop-filter: blur(40px) !important;
  border-color: var(--cosmos-border) !important;
}
</style>
