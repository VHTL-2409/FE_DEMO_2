<template>
  <Teleport to="body">
    <div class="fixed top-4 right-4 z-[9999] flex flex-col gap-3 w-[360px] max-w-[calc(100vw-2rem)] pointer-events-none">
      <div class="pointer-events-auto flex flex-col gap-3">
        <TransitionGroup name="toast" tag="div" class="flex flex-col gap-3">
          <div
            v-for="toast in toasts"
            :key="toast.id"
            class="toast-item rounded-2xl shadow-xl px-4 py-3.5 flex items-start gap-3 backdrop-blur-xl"
            :class="typeClass(toast.type)"
          >
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
import { computed } from 'vue'
import { useToastStore } from '../../stores/toastStore'

const toastStore = useToastStore()

const toasts = computed(() => toastStore.toasts)

const dismissToast = (id) => {
  toastStore.dismiss(id)
}

const typeClass = (type) => {
  switch (type) {
    case 'success':
      return 'toast-success'
    case 'error':
      return 'toast-error'
    case 'warning':
      return 'toast-warning'
    default:
      return 'toast-info'
  }
}

const iconBgClass = (type) => {
  switch (type) {
    case 'success':
      return 'bg-emerald-100 dark:bg-emerald-500/20'
    case 'error':
      return 'bg-rose-100 dark:bg-rose-500/20'
    case 'warning':
      return 'bg-amber-100 dark:bg-amber-500/20'
    default:
      return 'bg-indigo-100 dark:bg-indigo-500/20'
  }
}

const iconClass = (type) => {
  switch (type) {
    case 'success':
      return 'text-emerald-600 dark:text-emerald-400'
    case 'error':
      return 'text-rose-600 dark:text-rose-400'
    case 'warning':
      return 'text-amber-600 dark:text-amber-400'
    default:
      return 'text-indigo-600 dark:text-indigo-400'
  }
}

const messageClass = (type) => {
  switch (type) {
    case 'success':
      return 'text-slate-600 dark:text-slate-300'
    case 'error':
      return 'text-slate-700 dark:text-slate-300'
    case 'warning':
      return 'text-slate-700 dark:text-slate-300'
    default:
      return 'text-slate-600 dark:text-slate-300'
  }
}

const iconName = (type) => {
  switch (type) {
    case 'success':
      return 'check_circle'
    case 'error':
      return 'error'
    case 'warning':
      return 'warning'
    default:
      return 'info'
  }
}
</script>

<style scoped>
.toast-success {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border: 1px solid rgba(16, 185, 129, 0.25);
  box-shadow: 0 4px 24px rgba(16, 185, 129, 0.12);
}
.dark .toast-success {
  background: rgba(15, 23, 42, 0.95);
  border-color: rgba(16, 185, 129, 0.3);
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.4);
}

.toast-error {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border: 1px solid rgba(244, 63, 94, 0.25);
  box-shadow: 0 4px 24px rgba(244, 63, 94, 0.12);
}
.dark .toast-error {
  background: rgba(15, 23, 42, 0.95);
  border-color: rgba(244, 63, 94, 0.3);
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.4);
}

.toast-warning {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border: 1px solid rgba(245, 158, 11, 0.25);
  box-shadow: 0 4px 24px rgba(245, 158, 11, 0.12);
}
.dark .toast-warning {
  background: rgba(15, 23, 42, 0.95);
  border-color: rgba(245, 158, 11, 0.3);
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.4);
}

.toast-info {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border: 1px solid rgba(99, 102, 241, 0.25);
  box-shadow: 0 4px 24px rgba(99, 102, 241, 0.12);
}
.dark .toast-info {
  background: rgba(15, 23, 42, 0.95);
  border-color: rgba(99, 102, 241, 0.3);
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.4);
}

.toast-enter-active {
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
}
.toast-leave-active {
  transition: all 0.25s ease-out;
}
.toast-enter-from,
.toast-leave-to {
  opacity: 0;
  transform: translateX(24px);
}
.toast-move {
  transition: transform 0.3s ease;
}
</style>
