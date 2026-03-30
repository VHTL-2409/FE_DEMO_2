<template>
  <div class="sep">
    <!-- Start button -->
    <div class="sep__btn-wrap">
      <button
        type="button"
        class="sep__btn"
        :class="{
          'sep__btn--enabled': canStart,
          'sep__btn--disabled': !canStart,
          'sep__btn--loading': isStarting
        }"
        :disabled="!canStart || isStarting"
        @click="$emit('start')"
      >
        <template v-if="isStarting">
          <LucideIcon name="progress_activity" />
          <span>Đang chuyển...</span>
        </template>
        <template v-else>
          <LucideIcon name="play_arrow" />
          <span>{{ buttonLabel }}</span>
        </template>
      </button>
    </div>

    <!-- Disabled reason -->
    <div v-if="!canStart && !isStarting" class="sep__reason">
      <LucideIcon :name="reasonIcon" />
      <span class="sep__reason-text">{{ reasonText }}</span>
    </div>

    <!-- Tips -->
    <div v-if="canStart" class="sep__tips">
      <div class="sep__tip">
        <LucideIcon name="lightbulb" />
        <span>Đảm bảo ở yên, tắt cản có thể</span>
      </div>
      <div class="sep__tip">
        <LucideIcon name="wifi" />
        <span>Kiểm tra mạng WiFi ổn định</span>
      </div>
      <div v-if="requireCameraMic" class="sep__tip">
        <LucideIcon name="videocam" />
        <span>Camera sẽ bật trong suốt buổi thi</span>
      </div>
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
  requireCameraMic: { type: Boolean, default: false }
})

defineEmits(['start'])

const buttonLabel = computed(() => {
  if (props.isEnded) return 'Bài thi đã kết thúc'
  if (props.isBeforeStart) return 'Chưa đến giờ thi'
  if (!props.devicesReady && props.requireCameraMic) return 'Kiểm tra thiết bị'
  return 'Bắt đầu làm bài'
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
  padding: 1.5rem 1.25rem 1rem;
  background: var(--ds-gray-50);
}

.dark .sep__btn-wrap { background: var(--ds-gray-800); }

/* Start button */
.sep__btn {
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
  transition: all 0.2s ease;
  border: none;
  font-family: inherit;
  min-height: 56px;
  letter-spacing: -0.01em;
}

.sep__btn:active:not(:disabled) {
  transform: scale(0.98);
}

.sep__btn--enabled {
  background: linear-gradient(135deg, var(--ds-primary) 0%, #6366f1 100%);
  color: white;
  box-shadow: 0 6px 20px rgba(79, 70, 229, 0.3);
  animation: sepPulse 2s ease-in-out infinite;
}

@keyframes sepPulse {
  0%, 100% { box-shadow: 0 6px 20px rgba(79, 70, 229, 0.3); }
  50% { box-shadow: 0 6px 30px rgba(79, 70, 229, 0.5); }
}

.sep__btn--enabled:hover {
  transform: translateY(-3px);
  box-shadow: 0 12px 32px rgba(79, 70, 229, 0.45);
  animation: none;
}

.sep__btn--disabled {
  background: var(--ds-gray-100);
  color: var(--ds-text-muted);
  cursor: not-allowed;
  box-shadow: none;
}

.dark .sep__btn--disabled { background: var(--ds-gray-800); color: var(--ds-gray-500); }

.sep__btn--loading {
  background: var(--ds-primary);
  color: white;
  cursor: wait;
}


.sep__spinner { animation: sepSpin 1s linear infinite; }

@keyframes sepSpin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

/* Reason */
.sep__reason {
  display: flex;
  align-items: center;
  gap: 0.625rem;
  padding: 0.75rem 1.25rem 1.25rem;
  background: var(--ds-gray-50);
}

.dark .sep__reason { background: var(--ds-gray-800); }

.sep__reason-icon {
  font-size: 1.125rem;
  color: var(--ds-text-muted);
  flex-shrink: 0;
}

.sep__reason-text {
  font-size: 0.8rem;
  color: var(--ds-text-muted);
  font-weight: 600;
  line-height: 1.4;
}

/* Tips */
.sep__tips {
  padding: 1rem 1.25rem 1.25rem;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  border-top: 1px solid var(--ds-border);
}

.dark .sep__tips { border-top-color: var(--ds-border-strong); }

.sep__tip {
  display: flex;
  align-items: center;
  gap: 0.625rem;
}

.sep__tip-icon {
  font-size: 1rem;
  color: var(--ds-text-muted);
  flex-shrink: 0;
}

.sep__tip span:last-child {
  font-size: 0.775rem;
  color: var(--ds-text-muted);
  font-weight: 500;
  line-height: 1.4;
}
</style>
