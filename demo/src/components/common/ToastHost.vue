<template>
  <div class="fixed top-6 right-6 z-[9999] flex flex-col gap-3 w-[320px] max-w-[90vw]">
    <TransitionGroup name="toast" tag="div" class="flex flex-col gap-3">
      <div
        v-for="toast in toasts"
        :key="toast.id"
        class="rounded-xl border shadow-lg px-4 py-3 bg-white/95 dark:bg-slate-900/95 backdrop-blur text-slate-900 dark:text-slate-100"
        :class="typeClass(toast.type)"
      >
        <div class="flex items-start gap-3">
          <span class="material-symbols-outlined text-lg" :class="iconClass(toast.type)">
            {{ iconName(toast.type) }}
          </span>
          <div class="flex-1">
            <p v-if="toast.title" class="text-sm font-semibold">{{ toast.title }}</p>
            <p class="text-sm text-slate-600 dark:text-slate-300">{{ toast.message }}</p>
          </div>
          <button
            type="button"
            class="text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 transition"
            @click="dismissToast(toast.id)"
            aria-label="Đóng thông báo"
          >
            <span class="material-symbols-outlined text-base">close</span>
          </button>
        </div>
      </div>
    </TransitionGroup>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { toastService } from '../../services/toastService'

const toasts = computed(() => toastService.state.toasts)

const dismissToast = (id) => {
  toastService.dismiss(id)
}

const typeClass = (type) => {
  switch (type) {
    case 'success':
      return 'border-emerald-200/70 dark:border-emerald-500/30'
    case 'error':
      return 'border-rose-200/70 dark:border-rose-500/30'
    case 'warning':
      return 'border-amber-200/70 dark:border-amber-500/30'
    default:
      return 'border-slate-200/70 dark:border-slate-700/60'
  }
}

const iconClass = (type) => {
  switch (type) {
    case 'success':
      return 'text-emerald-500'
    case 'error':
      return 'text-rose-500'
    case 'warning':
      return 'text-amber-500'
    default:
      return 'text-slate-500'
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
.toast-enter-active,
.toast-leave-active {
  transition: all 0.25s ease;
}

.toast-enter-from,
.toast-leave-to {
  opacity: 0;
  transform: translateY(-8px) scale(0.98);
}
</style>
