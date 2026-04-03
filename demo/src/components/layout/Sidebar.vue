<template>
  <!-- Mobile overlay -->
  <Teleport to="body">
    <div
      v-if="mobileOpen"
      class="db-sidebar-backdrop"
      aria-hidden="true"
      @click="$emit('close-mobile')"
    />
  </Teleport>

  <!-- Sidebar -->
  <aside
    :class="[
      'db-sidebar',
      collapsed ? 'db-sidebar--collapsed' : 'db-sidebar--expanded'
    ]"
  >
    <!-- Header: logo + brand -->
    <div class="db-sidebar__header">
      <div class="db-sidebar__brand">
        <!-- Logo -->
        <div class="db-sidebar__logo">
          <AppLogo size="sm" variant="default" :show-text="false" tag="div" />
        </div>

        <!-- Brand name -->
        <div class="db-sidebar__brand-text">
          <p class="db-sidebar__brand-name">EduExam</p>
          <p class="db-sidebar__brand-sub">{{ roleLabel }}</p>
        </div>
      </div>
    </div>

    <!-- Navigation -->
    <nav class="db-sidebar__nav" aria-label="Admin navigation">
      <RouterLink
        v-for="(item, idx) in items"
        :key="item.section"
        :to="item.to"
        :class="navItemClass(item.section)"
        class="db-sidebar-nav-item"
        :style="{ animationDelay: `${0.05 + idx * 0.05}s` }"
        @click="$emit('close-mobile')"
      >
        <!-- Active left bar -->
        <span
          v-if="activeSection === item.section && !collapsed"
          class="db-sidebar-nav-item__bar"
        />

        <!-- Icon -->
        <span class="db-sidebar-nav-item__icon">
          <LucideIcon
            :name="item.icon"
            :size="20"
          />
        </span>

        <!-- Label -->
        <span class="db-sidebar-nav-item__label">
          {{ item.label }}
        </span>

        <!-- Badge -->
        <span
          v-if="item.badge && !collapsed"
          class="db-sidebar-nav-item__badge"
        >
          {{ item.badge }}
        </span>

        <!-- Hover tooltip for collapsed -->
        <span
          v-if="collapsed"
          class="db-sidebar-tooltip"
        >
          {{ item.label }}
        </span>
      </RouterLink>
    </nav>

    <!-- Bottom section -->
    <div class="db-sidebar__bottom">

      <!-- User section -->
      <div v-if="user" class="db-sidebar-user">
        <div class="db-sidebar-user__avatar">
          {{ userInitial }}
        </div>
        <div class="db-sidebar-user__info">
          <p class="db-sidebar-user__name">{{ user.username }}</p>
          <p class="db-sidebar-user__role">{{ role }}</p>
        </div>
        <button
          type="button"
          class="db-sidebar-user__logout"
          title="Dang xuat"
          @click="handleLogout"
        >
          <LucideIcon name="log_out" :size="16" />
        </button>
      </div>

      <!-- Collapse/expand toggle -->
      <button
        type="button"
        class="db-sidebar-collapse-btn"
        :aria-label="collapsed ? 'Mo rong menu' : 'Thu gon menu'"
        :title="collapsed ? 'Mo rong menu' : 'Thu gon menu'"
        @click="$emit('toggle')"
      >
        <LucideIcon
          name="chevron_left"
          :size="18"
          :class="collapsed ? '' : 'rotate-180'"
        />
        <span class="db-sidebar-collapse-btn__label">Thu gon</span>
      </button>
    </div>
  </aside>
</template>

<script setup>
import { computed, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { clearAuthSession } from '../../services/authService'
import AppLogo from '../common/AppLogo.vue'

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

const roleLabel = computed(() => {
  const labels = { teacher: 'Giao vien', student: 'Hoc sinh', admin: 'Quan tri' }
  return labels[props.role] || props.role
})

const userInitial = computed(() => {
  if (!props.user?.username) return '?'
  return props.user.username.slice(0, 1).toUpperCase()
})

const navItemClass = (section) => {
  if (props.activeSection === section) {
    return 'db-sidebar-nav-item--active'
  }
  return ''
}

const handleLogout = () => {
  clearAuthSession()
  router.push('/login')
}
</script>

<style scoped>
/* ─── Backdrop ─────────────────────────────────────────────────── */
.db-sidebar-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.3);
  z-index: 30;
  animation: fadeIn 0.2s ease;
}

/* ─── Sidebar shell ─────────────────────────────────────────────── */
.db-sidebar {
  position: fixed;
  left: 0;
  top: 0;
  height: 100dvh;
  display: flex;
  flex-direction: column;
  z-index: 40;
  overflow: hidden;
  will-change: width;
  transition: width 0.35s cubic-bezier(0.4, 0, 0.2, 1);

  /* Light mode */
  background: var(--db-surface);
  border-right: 1px solid var(--db-border);
  box-shadow: 2px 0 12px rgba(15, 23, 42, 0.06);

  /* Entrance animation */
  animation: slideInLeft 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.db-sidebar--expanded {
  width: var(--db-sidebar-w);
}

.db-sidebar--collapsed {
  width: var(--db-sidebar-collapsed);
}

/* ─── Header ────────────────────────────────────────────────────── */
.db-sidebar__header {
  padding: 1.25rem 1rem;
  border-bottom: 1px solid var(--db-border);
  flex-shrink: 0;
}

.db-sidebar--collapsed .db-sidebar__header {
  padding-left: 0;
  padding-right: 0;
  display: flex;
  justify-content: center;
}

.db-sidebar__brand {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  overflow: hidden;
}

.db-sidebar--collapsed .db-sidebar__brand {
  justify-content: center;
  width: 100%;
}

.db-sidebar__logo {
  width: 40px;
  height: 40px;
  border-radius: var(--db-radius);
  background: linear-gradient(135deg, var(--db-primary), #0a4a8a);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  box-shadow: 0 4px 14px rgba(12, 92, 171, 0.3);
  transition: transform var(--db-transition-spring);
}

.db-sidebar__brand:hover .db-sidebar__logo {
  transform: scale(1.05) rotate(-3deg);
}

.db-sidebar__brand-text {
  overflow: hidden;
  opacity: 1;
  transition: opacity 0.25s ease, max-width 0.35s cubic-bezier(0.4, 0, 0.2, 1);
  max-width: 200px;
}

.db-sidebar--collapsed .db-sidebar__brand-text {
  display: none;
}

.db-sidebar__brand-name {
  font-family: var(--db-font);
  font-size: 1rem;
  font-weight: 900;
  color: var(--db-text);
  margin: 0;
  line-height: 1.2;
  letter-spacing: -0.01em;
  white-space: nowrap;
}

.db-sidebar__brand-sub {
  font-size: 0.6rem;
  font-weight: 800;
  text-transform: uppercase;
  letter-spacing: 0.1em;
  color: var(--db-text-muted);
  margin: 0.125rem 0 0;
  white-space: nowrap;
}

/* ─── Nav ──────────────────────────────────────────────────────── */
.db-sidebar__nav {
  flex: 1;
  overflow-y: auto;
  padding: 0.75rem 0.625rem;
  display: flex;
  flex-direction: column;
  gap: 0.125rem;
}

.db-sidebar--collapsed .db-sidebar__nav {
  padding-left: 0;
  padding-right: 0;
  align-items: stretch;
}

.db-sidebar__nav::-webkit-scrollbar { width: 4px; }
.db-sidebar__nav::-webkit-scrollbar-thumb { background: rgba(148, 163, 184, 0.3); border-radius: 999px; }
.db-sidebar__nav::-webkit-scrollbar-thumb:hover { background: rgba(148, 163, 184, 0.5); }

/* Nav item */
.db-sidebar-nav-item {
  position: relative;
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.75rem 0.875rem;
  border-radius: var(--db-radius);
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--db-text-muted);
  text-decoration: none;
  transition: all var(--db-transition);
  overflow: hidden;
  white-space: nowrap;

  /* Entrance animation */
  animation: fadeInUp 0.3s ease backwards;
}

.db-sidebar-nav-item:hover {
  color: var(--db-text);
  background: var(--db-surface-3);
}

.db-sidebar--collapsed .db-sidebar-nav-item {
  justify-content: center;
  gap: 0;
  padding-left: 0;
  padding-right: 0;
  width: 100%;
}

.db-sidebar-nav-item:hover {
  transform: translateX(3px);
}

.db-sidebar--collapsed .db-sidebar-nav-item:hover {
  transform: translateX(0) scale(1.05);
}

/* Active state */
.db-sidebar-nav-item--active {
  color: var(--db-primary);
  background: var(--db-primary-soft);
  font-weight: 700;
}

.db-sidebar--collapsed .db-sidebar-nav-item--active {
  background: var(--db-primary);
  color: white;
}

/* Active left bar (only when expanded) */
.db-sidebar-nav-item__bar {
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 3px;
  height: 60%;
  border-radius: 0 3px 3px 0;
  background: var(--db-primary);
  animation: slideInLeft 0.2s ease;
}

.db-sidebar--collapsed .db-sidebar-nav-item__bar {
  display: none;
}

/* Icon */
.db-sidebar-nav-item__icon {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  transition: transform var(--db-transition-spring), color var(--db-transition);
}

.db-sidebar-nav-item:hover .db-sidebar-nav-item__icon {
  transform: scale(1.1);
}

.db-sidebar--collapsed .db-sidebar-nav-item__icon {
  margin: 0;
}

/* Label */
.db-sidebar-nav-item__label {
  flex: 1;
  overflow: hidden;
  opacity: 1;
  transition: opacity 0.25s ease, max-width 0.35s cubic-bezier(0.4, 0, 0.2, 1);
  max-width: 200px;
  white-space: nowrap;
}

.db-sidebar--collapsed .db-sidebar-nav-item__label {
  display: none;
}

/* Badge */
.db-sidebar-nav-item__badge {
  margin-left: auto;
  padding: 0.125rem 0.5rem;
  border-radius: 9999px;
  font-size: 0.6rem;
  font-weight: 800;
  background: var(--db-primary-soft);
  color: var(--db-primary);
  white-space: nowrap;
  opacity: 1;
  transition: opacity 0.2s ease;
}

.db-sidebar--collapsed .db-sidebar-nav-item__badge {
  display: none;
}

/* Tooltip */
.db-sidebar-tooltip {
  position: absolute;
  left: calc(100% + 10px);
  top: 50%;
  transform: translateY(-50%) translateX(-4px);
  padding: 0.375rem 0.75rem;
  background: var(--db-surface);
  border: 1px solid var(--db-border);
  border-radius: var(--db-radius);
  box-shadow: 0 4px 16px rgba(15, 23, 42, 0.12);
  font-size: 0.8rem;
  font-weight: 700;
  color: var(--db-text);
  white-space: nowrap;
  pointer-events: none;
  opacity: 0;
  z-index: 50;

  transition: opacity 0.15s ease, transform 0.15s ease;
}

.db-sidebar-nav-item:hover .db-sidebar-tooltip {
  opacity: 1;
  transform: translateY(-50%) translateX(0);
}

/* ─── Bottom ────────────────────────────────────────────────────── */
.db-sidebar__bottom {
  padding: 0.625rem;
  border-top: 1px solid var(--db-border);
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
  flex-shrink: 0;
}

/* User pill */
.db-sidebar-user {
  display: flex;
  align-items: center;
  gap: 0.625rem;
  padding: 0.625rem 0.75rem;
  border-radius: var(--db-radius);
  background: var(--db-surface-2);
  border: 1px solid var(--db-border);
  transition: all var(--db-transition);
  overflow: hidden;
}

.db-sidebar-user:hover {
  border-color: var(--db-border-strong);
  background: var(--db-surface-3);
}

.db-sidebar-user__avatar {
  width: 32px;
  height: 32px;
  border-radius: var(--db-radius-sm);
  background: linear-gradient(135deg, var(--db-primary), #0a4a8a);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.875rem;
  font-weight: 800;
  flex-shrink: 0;
  box-shadow: 0 2px 8px rgba(12, 92, 171, 0.25);
}

.db-sidebar-user__info {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  opacity: 1;
  transition: opacity 0.25s ease, max-width 0.35s cubic-bezier(0.4, 0, 0.2, 1);
  max-width: 200px;
}

.db-sidebar--collapsed .db-sidebar-user__info {
  display: none;
}

.db-sidebar-user__name {
  font-size: 0.8rem;
  font-weight: 700;
  color: var(--db-text);
  margin: 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.db-sidebar-user__role {
  font-size: 0.65rem;
  font-weight: 600;
  color: var(--db-text-muted);
  margin: 0.125rem 0 0;
  text-transform: capitalize;
  white-space: nowrap;
}

.db-sidebar-user__logout {
  flex-shrink: 0;
  width: 28px;
  height: 28px;
  border-radius: var(--db-radius-sm);
  background: none;
  border: none;
  color: var(--db-text-muted);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all var(--db-transition);
  opacity: 0;
}

.db-sidebar-user:hover .db-sidebar-user__logout {
  opacity: 1;
}

.db-sidebar-user__logout:hover {
  background: var(--db-danger-soft);
  color: var(--db-danger);
}

/* Collapse button */
.db-sidebar-collapse-btn {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.625rem 0.875rem;
  border-radius: var(--db-radius);
  background: none;
  border: none;
  font-size: 0.8rem;
  font-weight: 600;
  color: var(--db-text-muted);
  cursor: pointer;
  transition: all var(--db-transition);
  overflow: hidden;
  white-space: nowrap;
  width: 100%;
}

.db-sidebar-collapse-btn:hover {
  color: var(--db-text);
  background: var(--db-surface-3);
}

.db-sidebar-collapse-btn .lucide {
  flex-shrink: 0;
  transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.db-sidebar-collapse-btn__label {
  overflow: hidden;
  opacity: 1;
  transition: opacity 0.25s ease, max-width 0.35s cubic-bezier(0.4, 0, 0.2, 1);
  max-width: 200px;
  white-space: nowrap;
}

.db-sidebar--collapsed .db-sidebar-collapse-btn__label {
  display: none;
}

.db-sidebar--collapsed .db-sidebar-collapse-btn {
  justify-content: center;
  padding-left: 0;
  padding-right: 0;
}

.db-sidebar--collapsed .db-sidebar__bottom {
  padding-left: 0;
  padding-right: 0;
  align-items: stretch;
}

.db-sidebar--collapsed .db-sidebar-user {
  justify-content: center;
  padding-left: 0;
  padding-right: 0;
  gap: 0;
}

.db-sidebar--collapsed .db-sidebar-user__logout {
  display: none;
}
</style>
