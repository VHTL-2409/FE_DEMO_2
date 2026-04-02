<template>
  <!-- Mobile overlay -->
  <Teleport to="body">
    <div
      v-if="mobileOpen"
      class="fixed inset-0 z-30 bg-slate-950/40 backdrop-blur-sm"
      @click="$emit('close-mobile')"
    />
  </Teleport>

  <!-- Sidebar — GPU-composited via clip-path, zero layout reflow -->
  <aside
    :class="[
      'ds-sidebar fixed left-0 top-0 z-40 flex h-full flex-col border-r border-[var(--ds-border)] bg-[var(--ds-surface)]',
      collapsed ? 'ds-sidebar--collapsed' : 'ds-sidebar--expanded'
    ]"
  >
    <!-- Header: logo + brand -->
    <div class="ds-sidebar__header border-b border-[var(--ds-border)] px-4 py-3">
      <div class="flex items-center gap-3">
        <!-- Logo -->
        <div
          class="flex size-9 shrink-0 items-center justify-center rounded-[var(--ds-radius-lg)] bg-gradient-to-br from-[var(--ds-primary)] to-indigo-600 text-white shadow-md"
        >
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path d="M12 3L1 9L5 11.18V17.18L12 21L19 17.18V11.18L21 10.09V17H23V9L12 3ZM18.82 9L12 12.72L5.18 9L12 5.28L18.82 9ZM17 15.99L12 18.72L7 15.99V12.27L12 15L17 12.27V15.99Z" fill="currentColor"/>
          </svg>
        </div>

        <!-- Brand name — animate opacity + width when collapsed -->
        <div class="ds-sidebar__brand overflow-hidden">
          <p class="text-sm font-extrabold bg-gradient-to-r from-[var(--ds-primary)] to-indigo-500 bg-clip-text text-transparent font-display tracking-tight whitespace-nowrap">EduExam</p>
          <p class="text-[10px] font-bold uppercase tracking-widest text-[var(--ds-text-muted)] whitespace-nowrap">{{ roleLabel }}</p>
        </div>
      </div>
    </div>

    <!-- Navigation -->
    <nav class="ds-sidebar-nav flex-1 overflow-y-auto px-2.5 py-4 space-y-0.5">
      <RouterLink
        v-for="item in items"
        :key="item.section"
        :to="item.to"
        :class="navItemClass(item.section)"
        class="ds-sidebar-nav-item group relative flex items-center gap-3 rounded-[var(--ds-radius-xl)] px-3.5 py-2.5 text-sm font-semibold"
        @click="$emit('close-mobile')"
      >
        <!-- Active left bar — only when expanded (overflows when collapsed) -->
        <span
          v-if="activeSection === item.section && !collapsed"
          class="absolute left-0 top-1/2 -translate-y-1/2 h-6 w-[3px] rounded-r-full bg-[var(--ds-primary)]"
        />

        <!-- Icon — white when collapsed + active, primary otherwise -->
        <span class="ds-sidebar-nav-icon-wrap shrink-0 flex items-center justify-center w-5 h-5">
          <LucideIcon
            :name="item.icon"
            :size="20"
            class="ds-sidebar-nav-icon"
          />
        </span>

        <!-- Label — CSS transition thay vì v-show để tránh layout reflow -->
        <span class="ds-sidebar-nav-label shrink-0 truncate leading-snug whitespace-nowrap overflow-hidden">
          {{ item.label }}
        </span>

        <!-- Badge -->
        <span
          v-if="item.badge && !collapsed"
          class="ml-auto rounded-full px-2 py-0.5 text-[10px] font-bold flex-shrink-0"
          :class="item.badgeVariant === 'danger' ? 'bg-[var(--ds-danger-soft)] text-[var(--ds-danger)]' : 'bg-[var(--ds-primary-soft)] text-[var(--ds-primary)]'"
        >
          {{ item.badge }}
        </span>

        <!-- Hover tooltip for collapsed -->
        <span
          v-if="collapsed"
          class="ds-sidebar-tooltip absolute left-full ml-2 hidden group-hover:flex items-center px-2 py-1 bg-[var(--ds-surface)] border border-[var(--ds-border)] rounded-lg shadow-lg text-xs font-semibold text-[var(--ds-text)] whitespace-nowrap z-50"
        >
          {{ item.label }}
        </span>
      </RouterLink>
    </nav>

    <!-- Bottom: notifications + user + collapse toggle -->
    <div class="border-t border-[var(--ds-border)] p-2.5 space-y-0.5">
      <!-- Notification bell -->
      <div class="relative">
        <button
          type="button"
          class="ds-sidebar-nav-item group flex w-full items-center gap-3 rounded-[var(--ds-radius-xl)] px-3.5 py-2.5 text-sm font-semibold text-[var(--ds-text-secondary)]"
          aria-label="Thông báo"
          @click="showNotifications = !showNotifications"
        >
          <LucideIcon name="notifications" class="ds-sidebar-nav-icon shrink-0 relative" />
          <span
            v-if="hasUnread"
            class="absolute -top-1 -right-1 size-2 rounded-full bg-[var(--ds-danger)]"
          />
          <span class="ds-sidebar-nav-label shrink-0 truncate leading-snug whitespace-nowrap overflow-hidden text-left">Thông báo</span>
        </button>

        <!-- Notification dropdown -->
        <Teleport to="body">
          <div
            v-if="showNotifications"
            class="ds-notif-dropdown fixed z-[100] w-80 overflow-y-auto rounded-[var(--ds-radius-xl)] shadow-[var(--ds-shadow-lg)] bg-[var(--ds-surface)] border border-[var(--ds-border)]"
            :style="notifDropdownStyle"
          >
            <div class="flex items-center justify-between border-b border-[var(--ds-border)] px-4 py-3">
              <span class="font-bold text-sm text-[var(--ds-text)]">Thông báo</span>
              <button
                v-if="hasUnread"
                type="button"
                class="text-[11px] font-semibold text-[var(--ds-primary)] hover:underline"
                @click="markAllAsRead"
              >
                Đánh dấu đã đọc
              </button>
            </div>
            <div class="max-h-64 overflow-y-auto">
              <div v-if="!notifications.length" class="p-6 text-center text-sm text-[var(--ds-text-muted)]">
                Chưa có thông báo.
              </div>
              <div
                v-for="n in notifications"
                :key="n.id"
                :class="n.read ? '' : 'bg-[var(--ds-primary-soft)]'"
                class="cursor-pointer border-b border-[var(--ds-border)] px-4 py-3 transition-colors last:border-b-0 hover:bg-[var(--ds-gray-50)]"
                @click="markAsRead(n.id)"
              >
                <p class="text-sm font-semibold text-[var(--ds-text)]">{{ n.title }}</p>
                <p class="mt-0.5 text-xs text-[var(--ds-text-muted)]">{{ n.message }}</p>
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
      <div v-if="user" class="flex items-center gap-2.5 rounded-[var(--ds-radius-xl)] px-3.5 py-2.5 mt-1 hover:bg-[var(--ds-gray-50)] transition-colors">
        <div class="size-8 shrink-0 rounded-full bg-gradient-to-br from-[var(--ds-primary)] to-indigo-600 text-white flex items-center justify-center text-xs font-bold shadow-sm">
          {{ userInitial }}
        </div>
        <div class="ds-sidebar-nav-label shrink-0 min-w-0 flex-1 overflow-hidden">
          <p class="truncate text-xs font-semibold text-[var(--ds-text)]">{{ user.username }}</p>
          <p class="truncate text-[10px] text-[var(--ds-text-muted)] capitalize">{{ role }}</p>
        </div>
        <button
          type="button"
          class="flex size-6 shrink-0 items-center justify-center rounded-full text-[var(--ds-text-muted)] transition-colors hover:bg-[var(--ds-gray-200)] hover:text-[var(--ds-danger)]"
          title="Đăng xuất"
          @click="handleLogout"
        >
          <LucideIcon name="logout" size="16" />
        </button>
      </div>

      <!-- Collapse/expand toggle — bottom of sidebar, sticky -->
      <button
        type="button"
        class="ds-sidebar-collapse-btn group flex w-full items-center gap-3 rounded-[var(--ds-radius-xl)] px-3.5 py-2.5 text-sm font-semibold text-[var(--ds-text-muted)] hover:bg-[var(--ds-gray-100)] hover:text-[var(--ds-text)] transition-colors"
        :aria-label="collapsed ? 'Mở rộng menu' : 'Thu gọn menu'"
        :title="collapsed ? 'Mở rộng menu' : 'Thu gọn menu'"
        @click="$emit('toggle')"
      >
        <LucideIcon name="chevron_left" class="shrink-0 transition-transform duration-300" :class="collapsed ? '' : 'rotate-180'" />
        <span class="ds-sidebar-nav-label shrink-0 whitespace-nowrap overflow-hidden">Thu gọn menu</span>
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
  const labels = { teacher: 'Cổng giáo viên', student: 'Cổng học sinh', admin: 'Cổng quản trị' }
  return labels[props.role] || props.role
})

const userInitial = computed(() => {
  if (!props.user?.username) return '?'
  return props.user.username.slice(0, 1).toUpperCase()
})

const navItemClass = (section) => {
  const base = 'relative overflow-hidden transition-colors duration-150'
  if (props.activeSection === section) {
    if (props.collapsed) {
      return `${base} bg-[var(--ds-primary)] text-white`
    }
    return `${base} bg-[var(--ds-primary-soft)] text-[var(--ds-primary)]`
  }
  return `${base} text-[var(--ds-text-secondary)] hover:bg-[var(--ds-gray-100)] hover:text-[var(--ds-text)]`
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
/* ── Sidebar width transitions — GPU-optimized via will-change ── */
.ds-sidebar {
  transition: width 0.35s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: hidden;
  white-space: nowrap;
  will-change: width;
}

.ds-sidebar--expanded {
  width: var(--ds-sidebar-width, 256px);
}

.ds-sidebar--collapsed {
  width: var(--ds-sidebar-collapsed, 64px);
}

/* ── Nav label transitions — opacity fade, max-width collapse, zero reflow ── */
.ds-sidebar-nav-label {
  overflow: hidden;
  opacity: 1;
  flex-shrink: 0;
  flex-grow: 0;
  transition:
    opacity 0.25s cubic-bezier(0.4, 0, 0.2, 1),
    max-width 0.35s cubic-bezier(0.4, 0, 0.2, 1),
    transform 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  width: auto;
  max-width: 200px;
  transform: translateX(0);
}

.ds-sidebar--collapsed .ds-sidebar-nav-label {
  opacity: 0;
  max-width: 0;
  pointer-events: none;
  transform: translateX(-8px);
}

/* ── Nav item: icon centered when collapsed ── */
.ds-sidebar--collapsed .ds-sidebar-nav-item {
  justify-content: center !important;
  align-items: center !important;
  gap: 0 !important;
  padding-left: 0 !important;
  padding-right: 0 !important;
  margin-left: 0 !important;
  margin-right: 0 !important;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
}

/* ── Nav icon: stable sizing, fade opacity ── */
.ds-sidebar-nav-icon {
  flex-shrink: 0;
  transition: opacity 0.25s ease, color 0.15s ease, transform 0.2s ease;
}

.ds-sidebar--collapsed .ds-sidebar-nav-icon-wrap {
  margin: 0 auto;
}

.ds-sidebar--collapsed .ds-sidebar-nav-icon {
  opacity: 1;
}

.ds-sidebar-nav-item {
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
}

.ds-sidebar-nav-item:hover {
  transform: translateX(4px);
}

.ds-sidebar--collapsed .ds-sidebar-nav-item:hover {
  transform: translateX(0) scale(1.05);
}

/* ── Brand: fade + collapse when collapsed ── */
.ds-sidebar__brand {
  overflow: hidden;
  opacity: 1;
  transition:
    opacity 0.25s cubic-bezier(0.4, 0, 0.2, 1),
    max-width 0.35s cubic-bezier(0.4, 0, 0.2, 1);
  max-width: 200px;
}

.ds-sidebar--collapsed .ds-sidebar__brand {
  opacity: 0;
  max-width: 0;
}

/* ── Tooltip for collapsed nav items ── */
.ds-sidebar-tooltip {
  pointer-events: none;
  opacity: 0;
  transform: translateX(-4px);
  transition: opacity 0.2s ease, transform 0.2s ease;
}

.ds-sidebar-nav-item:hover .ds-sidebar-tooltip {
  opacity: 1;
  transform: translateX(0);
}

/* ── Nav scrollbar ── */
.ds-sidebar-nav::-webkit-scrollbar {
  width: 4px;
}
.ds-sidebar-nav::-webkit-scrollbar-thumb {
  background: rgba(148, 163, 184, 0.3);
  border-radius: 999px;
}
.ds-sidebar-nav::-webkit-scrollbar-thumb:hover {
  background: rgba(148, 163, 184, 0.5);
}

/* ── Notification dropdown ── */
.ds-notif-dropdown {
  animation: ds-dropdown-in 0.15s ease-out;
}

@keyframes ds-dropdown-in {
  from { opacity: 0; transform: translateY(4px) scale(0.98); }
  to   { opacity: 1; transform: translateY(0) scale(1); }
}

/* ── Header ── */
.ds-sidebar__header {
  display: flex;
  align-items: center;
  flex-shrink: 0;
}
</style>
