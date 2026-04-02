<template>
  <Teleport to="body">
    <div class="toast-container" aria-live="polite" aria-label="Thông báo">
      <TransitionGroup name="toast" tag="div" class="toast-stack">
        <div
          v-for="toast in toastService.state.toasts"
          :key="toast.id"
          :class="['toast', `toast--${toast.type}`]"
          role="alert"
        >
          <div class="toast__icon">
            <span class="material-symbols-outlined toast__icon-inner">
              {{ iconMap[toast.type] }}
            </span>
          </div>
          <div class="toast__body">
            <p v-if="toast.title" class="toast__title">{{ toast.title }}</p>
            <p class="toast__message">{{ toast.message }}</p>
          </div>
          <button
            type="button"
            class="toast__close gs-spring"
            aria-label="Đóng thông báo"
            @click="toastService.dismiss(toast.id)"
          >
            <span class="material-symbols-outlined">close</span>
          </button>
        </div>
      </TransitionGroup>
    </div>
  </Teleport>
</template>

<script setup>
import { toastService } from '../../services/toastService'

const iconMap = {
  success: 'check_circle',
  error: 'error',
  warning: 'warning',
  info: 'info'
}
</script>

<style scoped>
.toast-container {
  position: fixed;
  top: 1.25rem;
  right: 1.25rem;
  z-index: var(--z-glass-toast, 500);
  display: flex;
  flex-direction: column;
  gap: 0.625rem;
  pointer-events: none;
  max-width: 380px;
  width: 100%;
}

.toast-stack {
  display: flex;
  flex-direction: column;
  gap: 0.625rem;
}

.toast {
  display: flex;
  align-items: flex-start;
  gap: 0.75rem;
  padding: 0.875rem 1rem;
  border-radius: var(--radius-glass, 16px);
  border: 1px solid;
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  box-shadow: var(--shadow-glass-lg, 0 8px 32px rgba(0,0,0,0.12));
  pointer-events: auto;
  background: var(--glass-surface-raised, rgba(255,255,255,0.96));
}

.toast--success {
  border-color: var(--glass-success-border, rgba(22,163,74,0.25));
  background: var(--glass-success-soft, rgba(22,163,74,0.08));
}
.toast--success .toast__icon-inner { color: var(--glass-success, #16a34a); }

.toast--error {
  border-color: var(--glass-danger-border, rgba(220,38,38,0.25));
  background: var(--glass-danger-soft, rgba(220,38,38,0.08));
}
.toast--error .toast__icon-inner { color: var(--glass-danger, #dc2626); }

.toast--warning {
  border-color: var(--glass-warning-border, rgba(217,119,6,0.25));
  background: var(--glass-warning-soft, rgba(217,119,6,0.08));
}
.toast--warning .toast__icon-inner { color: var(--glass-warning, #d97706); }

.toast--info {
  border-color: var(--glass-info-border, rgba(2,132,199,0.25));
  background: var(--glass-info-soft, rgba(2,132,199,0.08));
}
.toast--info .toast__icon-inner { color: var(--glass-info, #0284c7); }

.toast__icon {
  flex-shrink: 0;
  width: 1.5rem;
  height: 1.5rem;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-top: 0.1rem;
}

.toast__icon-inner {
  font-size: 1.25rem;
  line-height: 1;
}

.toast__body { flex: 1; min-width: 0; }

.toast__title {
  font-weight: 700;
  font-size: 0.875rem;
  color: var(--glass-text, #1c1917);
  margin-bottom: 0.125rem;
}

.toast__message {
  font-size: 0.8125rem;
  color: var(--glass-text-secondary, #57534e);
  line-height: 1.5;
}

.toast__close {
  flex-shrink: 0;
  width: 1.75rem;
  height: 1.75rem;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  background: transparent;
  color: var(--glass-text-muted, #a8a29e);
  cursor: pointer;
  border-radius: 6px;
  padding: 0;
  transition: background 0.15s, color 0.15s;
}
.toast__close:hover {
  background: rgba(0,0,0,0.06);
  color: var(--glass-text, #1c1917);
}
.toast__close .material-symbols-outlined { font-size: 1rem; }

/* Transition animations */
.toast-enter-active {
  transition: all 0.3s cubic-bezier(0.22, 1, 0.36, 1);
}
.toast-leave-active {
  transition: all 0.2s cubic-bezier(0.64, 0, 0.78, 0);
}
.toast-enter-from {
  opacity: 0;
  transform: translateX(32px) scale(0.95);
}
.toast-leave-to {
  opacity: 0;
  transform: translateX(32px) scale(0.95);
}
.toast-move {
  transition: transform 0.3s cubic-bezier(0.22, 1, 0.36, 1);
}
</style>
