<template>
  <div class="sep" :class="{ 'sep--compact': compact }">
    <!-- Start button -->
    <div class="sep__btn-wrap" :class="canStart ? 'sep__btn-wrap--ready' : 'sep__btn-wrap--disabled'">
      <button
        type="button"
        class="sep__btn"
        :class="{
          'sep__btn--enabled': canStart && !isStarting,
          'sep__btn--disabled': !canStart || isStarting,
          'sep__btn--loading': isStarting,
          'sep__btn--compact': compact
        }"
        :disabled="!canStart || isStarting"
        @click="$emit('start')"
      >
        <template v-if="isStarting">
          <LucideIcon name="progress_activity" />
          <span>Đang chuyển...</span>
        </template>
        <template v-else-if="canStart">
          <LucideIcon name="rocket_launch" />
          <span>{{ buttonLabel }}</span>
        </template>
        <template v-else>
          <LucideIcon :name="reasonIcon" />
          <span>{{ buttonLabel }}</span>
        </template>
      </button>

      <!-- Ripple/ready animation ring -->
      <div v-if="canStart && !isStarting && !compact" class="sep__ready-ring" />
    </div>

    <!-- Disabled reason -->
    <div v-if="!canStart && !isStarting" class="sep__reason" :class="[reasonClass, { 'sep__reason--compact': compact }]">
      <LucideIcon :name="reasonIcon" />
      <span class="sep__reason-text">{{ reasonText }}</span>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  canStart: { type: Boolean, default: false },
  isStarting: { type: Boolean, default: false },
  isEnded: { type: Boolean, default: false },
  isBeforeStart: { type: Boolean, default: false },
  devicesReady: { type: Boolean, default: true },
  requireCameraMic: { type: Boolean, default: false },
  compact: { type: Boolean, default: false },
  joinLabel: { type: String, default: 'Bắt đầu làm bài' }
})

defineEmits(['start'])

const buttonLabel = computed(() => {
  if (props.isEnded) return 'Bài thi đã kết thúc'
  if (props.isBeforeStart) return 'Chưa đến giờ thi'
  if (!props.devicesReady && props.requireCameraMic) return 'Kiểm tra thiết bị'
  return props.joinLabel
})

const reasonText = computed(() => {
  if (props.isEnded) return 'Bài thi đã kết thúc và không còn nhận bài.'
  if (props.isBeforeStart) return 'Vui lòng đợi đến giờ bắt đầu bài thi.'
  if (!props.devicesReady && props.requireCameraMic) return 'Bạn cần cấp quyền camera và microphone.'
  return 'Điều kiện chưa đủ để bắt đầu.'
})

const reasonIcon = computed(() => {
  if (props.isEnded) return 'event_busy'
  if (props.isBeforeStart) return 'schedule'
  if (!props.devicesReady && props.requireCameraMic) return 'videocam_off'
  return 'lock'
})

const reasonClass = computed(() => {
  if (props.isEnded) return 'sep__reason--ended'
  if (props.isBeforeStart) return 'sep__reason--waiting'
  if (!props.devicesReady && props.requireCameraMic) return 'sep__reason--cam'
  return 'sep__reason--locked'
})
</script>

<style scoped>
.sep {
  background: var(--ds-surface);
  border: 1.5px solid var(--ds-border);
  border-radius: var(--ds-radius-2xl);
  overflow: hidden;
}

.dark .sep { border-color: var(--ds-border-strong); }

/* Button wrap */
.sep__btn-wrap {
  position: relative;
  padding: 1.5rem 1.25rem 1rem;
  background: var(--ds-gray-50);
  display: flex;
  align-items: center;
  justify-content: center;
}

.dark .sep__btn-wrap { background: var(--ds-gray-800); }

.sep__btn-wrap--ready {
  background: linear-gradient(135deg, rgba(79, 70, 229, 0.04) 0%, rgba(16, 185, 129, 0.04) 100%);
}

/* Ready ring animation */
.sep__ready-ring {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 80%;
  height: 80%;
  border-radius: var(--ds-radius-2xl);
  border: 2px solid rgba(79, 70, 229, 0.2);
  animation: sepRing 2s ease-in-out infinite;
  pointer-events: none;
}

@keyframes sepRing {
  0%, 100% { opacity: 0.3; transform: translate(-50%, -50%) scale(1); }
  50% { opacity: 0.1; transform: translate(-50%, -50%) scale(1.05); }
}

/* Start button */
.sep__btn {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.625rem;
  width: 100%;
  padding: 1rem 1.5rem;
  border-radius: var(--ds-radius-2xl);
  font-size: 1.125rem;
  font-weight: 800;
  cursor: pointer;
  transition: color 0.2s ease, background-color 0.2s ease, border-color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease cubic-bezier(0.34, 1.56, 0.64, 1);
  border: none;
  font-family: inherit;
  min-height: 56px;
  letter-spacing: -0.01em;
}

.sep__btn:active:not(:disabled) {
  transform: scale(0.97);
}

.sep__btn--enabled {
  background: linear-gradient(135deg, var(--ds-primary) 0%, #6366f1 100%);
  color: white;
  box-shadow: 0 6px 20px rgba(79, 70, 229, 0.35), 0 0 0 0 rgba(79, 70, 229, 0.4);
  animation: sepPulse 2s ease-in-out infinite;
}

@keyframes sepPulse {
  0%, 100% {
    box-shadow: 0 6px 20px rgba(79, 70, 229, 0.35), 0 0 0 0 rgba(79, 70, 229, 0.3);
  }
  50% {
    box-shadow: 0 8px 28px rgba(79, 70, 229, 0.5), 0 0 0 8px rgba(79, 70, 229, 0);
  }
}

.sep__btn--enabled:hover {
  transform: translateY(-3px) scale(1.02);
  box-shadow: 0 12px 36px rgba(79, 70, 229, 0.5), 0 0 0 0 rgba(79, 70, 229, 0.3);
  animation: none;
}

.sep__btn--disabled {
  background: var(--ds-gray-100);
  color: var(--ds-text-muted);
  cursor: not-allowed;
  box-shadow: none;
}

.dark .sep__btn--disabled {
  background: var(--ds-gray-800);
  color: var(--ds-gray-500);
}

.sep__btn--loading {
  background: var(--ds-primary);
  color: white;
  cursor: wait;
  box-shadow: 0 4px 16px rgba(79, 70, 229, 0.3);
}

/* Reason */
.sep__reason {
  display: flex;
  align-items: center;
  gap: 0.625rem;
  padding: 0.75rem 1.25rem 1.25rem;
  background: var(--ds-gray-50);
  font-size: 0.8rem;
  font-weight: 600;
  line-height: 1.4;
  transition: color 0.3s ease, background-color 0.3s ease, border-color 0.3s ease, box-shadow 0.3s ease, transform 0.3s ease;
}

.dark .sep__reason { background: var(--ds-gray-800); }

.sep__reason .lucide {
  font-size: 1.125rem;
  flex-shrink: 0;
  transition: color 0.3s ease;
}

.sep__reason-text { flex: 1; }

.sep__reason--ended {
  color: var(--ds-text-muted);
}

.sep__reason--waiting {
  color: var(--ds-warning);
  background: rgba(245, 158, 11, 0.06);
}

.dark .sep__reason--waiting { background: rgba(245, 158, 11, 0.08); }
.sep__reason--waiting .lucide { color: var(--ds-warning); }

.sep__reason--cam {
  color: var(--ds-danger);
  background: rgba(220, 38, 38, 0.06);
}

.dark .sep__reason--cam { background: rgba(220, 38, 38, 0.08); }
.sep__reason--cam .lucide { color: var(--ds-danger); }

.sep__reason--locked {
  color: var(--ds-text-muted);
}

/* Compact — thanh CTA dưới cùng */
.sep--compact {
  border: none;
  background: transparent;
  backdrop-filter: none;
}

.dark .sep--compact {
  border: none;
}

.sep--compact .sep__btn-wrap {
  background: transparent;
  padding: 0;
}

.sep--compact .sep__btn-wrap--ready {
  background: transparent;
}

.sep__btn--compact {
  width: auto;
  min-width: min(100%, 280px);
  max-width: 100%;
  padding-left: 2rem;
  padding-right: 2rem;
}

.sep__reason--compact {
  justify-content: center;
  gap: 0.5rem;
  padding: 0.5rem 0 0;
  font-size: 0.75rem;
}

.sep__reason--compact .sep__reason-text {
  text-align: center;
}

/* Responsive */
@media (max-width: 480px) {
  .sep__btn {
    font-size: 1rem;
    min-height: 52px;
    padding: 0.875rem 1.25rem;
  }
}
</style>
@media (prefers-reduced-motion: reduce) {
  * {
    transition-duration: 0.01ms !important;
    animation-duration: 0.01ms !important;
  }
}