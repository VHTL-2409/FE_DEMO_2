<template>
  <Teleport to="body">
    <div class="toast-host"
      aria-live="polite"
      aria-label="Thông báo hệ thống">
      <div class="pointer-events-auto flex flex-col gap-3">
        <TransitionGroup name="toast" tag="div" class="flex flex-col gap-3">
          <div
            v-for="toast in toasts"
            :key="toast.id"
            class="toast-item rounded-2xl shadow-xl px-4 py-3.5 flex items-start gap-3 relative"
            :class="typeClass(toast.type)"
            @mouseenter="pauseTimer(toast.id)"
            @mouseleave="resumeTimer(toast.id)"
          >
            <!-- Auto-dismiss countdown progress bar -->
            <div
              v-if="toast._duration > 0"
              class="toast-progress-track"
            >
              <div
                class="toast-progress-bar"
                :class="progressClass(toast.type)"
                :style="{ animationDuration: `${toast._duration}ms` }"
              />
            </div>

            <div class="shrink-0 size-10 rounded-xl flex items-center justify-center" :class="iconBgClass(toast.type)">
              <LucideIcon :name="iconName(toast.type)" size="20" :class="iconClass(toast.type)" />
            </div>
            <div class="flex-1 min-w-0 pt-0.5">
              <p v-if="toast.title" class="text-sm font-bold text-slate-900 dark:text-slate-100 mb-0.5">{{ toast.title }}</p>
              <p class="text-sm leading-relaxed" :class="messageClass(toast.type)">{{ toast.message }}</p>
            </div>
            <button
              type="button"
              class="shrink-0 p-1.5 rounded-lg text-slate-400 hover:text-slate-600 hover:bg-slate-100 dark:hover:text-slate-300 dark:hover:bg-slate-700/50 transition-colors"
              @click="dismissToast(toast.id)"
              aria-label="Đóng thông báo"
            >
              <LucideIcon name="close" size="18" />
            </button>
          </div>
        </TransitionGroup>
      </div>
    </div>
  </Teleport>
</template>

<script setup>
import { computed, onBeforeUnmount } from 'vue'
import { useToastStore } from '../../stores/toastStore'

const toastStore = useToastStore()

const toasts = computed(() => toastStore.toasts)

const pauseTimer = (toastId) => {
  toastStore.pauseTimer(toastId)
}

const resumeTimer = (toastId) => {
  toastStore.resumeTimer(toastId)
}

const dismissToast = (toastId) => {
  toastStore.dismiss(toastId)
}

const progressClass = (type) => {
  switch (type) {
    case 'success': return 'toast-progress-bar--success'
    case 'error':   return 'toast-progress-bar--error'
    case 'warning': return 'toast-progress-bar--warning'
    default:        return 'toast-progress-bar--info'
  }
}

const typeClass = (type) => {
  switch (type) {
    case 'success': return 'toast-success'
    case 'error':   return 'toast-error'
    case 'warning': return 'toast-warning'
    default:        return 'toast-info'
  }
}

const iconBgClass = (type) => {
  switch (type) {
    case 'success': return 'bg-emerald-100 dark:bg-emerald-500/20'
    case 'error':   return 'bg-rose-100 dark:bg-rose-500/20'
    case 'warning': return 'bg-amber-100 dark:bg-amber-500/20'
    default:        return 'bg-indigo-100 dark:bg-indigo-500/20'
  }
}

const iconClass = (type) => {
  switch (type) {
    case 'success': return 'text-emerald-600 dark:text-emerald-400'
    case 'error':   return 'text-rose-600 dark:text-rose-400'
    case 'warning': return 'text-amber-600 dark:text-amber-400'
    default:        return 'text-indigo-600 dark:text-indigo-400'
  }
}

const messageClass = (type) => {
  switch (type) {
    case 'success': return 'text-slate-600 dark:text-slate-300'
    case 'error':   return 'text-slate-700 dark:text-slate-300'
    case 'warning': return 'text-slate-700 dark:text-slate-300'
    default:        return 'text-slate-600 dark:text-slate-300'
  }
}

const iconName = (type) => {
  switch (type) {
    case 'success': return 'check_circle'
    case 'error':   return 'error'
    case 'warning': return 'warning'
    default:        return 'info'
  }
}

onBeforeUnmount(() => {
  // Clear all timers on unmount — store handles the actual cleanup
})
</script>

<style scoped>
/* ── Progress bar: auto-dismiss countdown indicator ── */
.toast-progress-track {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 3px;
  overflow: hidden;
  border-radius: 0 0 1rem 1rem;
}

.toast-progress-bar {
  height: 100%;
  transform-origin: left;
  animation: toastCountdown linear forwards;
  width: 100%;
}

@keyframes toastCountdown {
  from { transform: scaleX(1); }
  to   { transform: scaleX(0); }
}

.toast-progress-bar--success { background: rgba(16, 185, 129, 0.8); }
.toast-progress-bar--error    { background: rgba(244, 63, 94, 0.8); }
.toast-progress-bar--warning { background: rgba(245, 158, 11, 0.8); }
.toast-progress-bar--info    { background: rgba(99, 102, 241, 0.8); }

/* ── Toast types ── */
.toast-success {
  background: rgba(255, 255, 255, 0.98);
  border: 1px solid rgba(16, 185, 129, 0.25);
  box-shadow: 0 4px 24px rgba(16, 185, 129, 0.12);
}
.dark .toast-success {
  background: rgba(15, 23, 42, 0.98);
  border-color: rgba(16, 185, 129, 0.3);
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.4);
}

.toast-error {
  background: rgba(255, 255, 255, 0.98);
  border: 1px solid rgba(244, 63, 94, 0.25);
  box-shadow: 0 4px 24px rgba(244, 63, 94, 0.12);
}
.dark .toast-error {
  background: rgba(15, 23, 42, 0.98);
  border-color: rgba(244, 63, 94, 0.3);
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.4);
}

.toast-warning {
  background: rgba(255, 255, 255, 0.98);
  border: 1px solid rgba(245, 158, 11, 0.25);
  box-shadow: 0 4px 24px rgba(245, 158, 11, 0.12);
}
.dark .toast-warning {
  background: rgba(15, 23, 42, 0.98);
  border-color: rgba(245, 158, 11, 0.3);
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.4);
}

.toast-info {
  background: rgba(255, 255, 255, 0.98);
  border: 1px solid rgba(99, 102, 241, 0.25);
  box-shadow: 0 4px 24px rgba(99, 102, 241, 0.12);
}
.dark .toast-info {
  background: rgba(15, 23, 42, 0.98);
  border-color: rgba(99, 102, 241, 0.3);
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.4);
}

/* ── Slide-in entrance from right ── */
.toast-enter-active {
  animation: toastSlideIn 0.35s cubic-bezier(0.34, 1.56, 0.64, 1) both;
}
.toast-leave-active {
  animation: toastSlideOut 0.2s cubic-bezier(0.4, 0, 0.2, 1) both;
}
.toast-move {
  transition: transform 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

@keyframes toastSlideIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

@keyframes toastSlideOut {
  from {
    opacity: 1;
  }
  to {
    opacity: 0;
  }
}

/* Desktop positioning */
.toast-host {
  position: fixed;
  top: 1rem;
  right: 1rem;
  z-index: 9999;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  pointer-events: none;
  width: 360px;
  max-width: calc(100vw - 2rem);
}

/* Mobile: toast at bottom center, full width */
@media (max-width: 640px) {
  .toast-host {
    position: fixed;
    bottom: 1.5rem;
    left: 1rem;
    right: 1rem;
    top: auto;
    width: auto;
    max-width: none;
  }
}

@media (prefers-reduced-motion: reduce) {
  .toast-progress-bar,
  .toast-enter-active,
  .toast-leave-active {
    animation-duration: 0.01ms !important;
  }
}
</style>
