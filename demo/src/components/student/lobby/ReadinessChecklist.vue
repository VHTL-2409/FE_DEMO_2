<template>
  <div class="rcl">
    <!-- Header -->
    <div class="rcl__header">
      <div class="rcl__header-icon">
        <LucideIcon name="tune" />
      </div>
      <div>
        <h2 class="rcl__header-title">Kiểm tra kỹ thuật</h2>
        <p class="rcl__header-sub">Đảm bảo máy tính của bạn sẵn sàng</p>
      </div>
      <button
        v-if="showRetry"
        type="button"
        class="rcl__retry-btn"
        @click="$emit('retry')"
      >
        <LucideIcon name="refresh" />
        Kiểm tra lại
      </button>
    </div>

    <!-- Overall status -->
    <div class="rcl__summary" :class="overallStatusClass">
      <div class="rcl__summary-icon" :class="overallIconClass">
        <LucideIcon :name="overallIconName" />
      </div>
      <div class="rcl__summary-text">
        <p class="rcl__summary-title">{{ overallStatusTitle }}</p>
        <p class="rcl__summary-sub">{{ overallStatusSub }}</p>
      </div>
      <div v-if="isChecking" class="rcl__summary-spinner">
        <LucideIcon name="progress_activity" class="rcl__spinner" />
      </div>
    </div>

    <!-- Checklist items -->
    <div class="rcl__items">
      <div
        v-for="item in displayItems"
        :key="item.id"
        class="rcl__item"
        :class="item.status === 'ok' ? 'rcl__item--ok' : item.status === 'fail' ? 'rcl__item--fail' : 'rcl__item--pending'"
      >
        <div class="rcl__item-icon" :class="item.iconClass">
          <LucideIcon :name="item.icon" />
        </div>
        <div class="rcl__item-content">
          <span class="rcl__item-name">{{ item.name }}</span>
          <span v-if="item.desc" class="rcl__item-desc">{{ item.desc }}</span>
        </div>
        <div class="rcl__item-status">
          <LucideIcon name="check_circle" v-if="item.status === 'ok'" class="rcl__status-icon rcl__status-icon--ok"/>
          <LucideIcon name="cancel" v-else-if="item.status === 'fail'" class="rcl__status-icon rcl__status-icon--fail"/>
          <LucideIcon name="progress_activity" v-else class="rcl__status-icon rcl__status-icon--pending rcl__spinner"/>
        </div>
      </div>
    </div>

    <!-- Error message -->
    <div v-if="errorMsg" class="rcl__error">
      <LucideIcon name="error" />
      <p>{{ errorMsg }}</p>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  cameraReady: { type: Boolean, default: false },
  micReady: { type: Boolean, default: false },
  networkReady: { type: Boolean, default: true },
  isChecking: { type: Boolean, default: false },
  errorMsg: { type: String, default: '' },
  requireCameraMic: { type: Boolean, default: false }
})

defineEmits(['retry'])

const showRetry = computed(() =>
  !props.isChecking && (!props.cameraReady || !props.micReady || !props.networkReady)
)

const items = computed(() => {
  const camera = {
    id: 'camera', name: 'Camera', icon: 'videocam',
    status: props.isChecking ? 'pending' : props.cameraReady ? 'ok' : 'fail',
    iconClass: props.isChecking ? 'rcl__item-icon--pending' : props.cameraReady ? 'rcl__item-icon--ok' : 'rcl__item-icon--fail',
    desc: props.cameraReady ? 'Camera hoạt động tốt' : props.requireCameraMic ? 'Chưa cấp quyền hoặc không có camera' : 'Không bắt buộc'
  }
  const mic = {
    id: 'mic', name: 'Microphone', icon: 'mic',
    status: props.isChecking ? 'pending' : props.micReady ? 'ok' : 'fail',
    iconClass: props.isChecking ? 'rcl__item-icon--pending' : props.micReady ? 'rcl__item-icon--ok' : 'rcl__item-icon--fail',
    desc: props.micReady ? 'Microphone hoạt động tốt' : props.requireCameraMic ? 'Chưa cấp quyền hoặc không có micro' : 'Không bắt buộc'
  }
  const network = {
    id: 'network', name: 'Mạng Internet', icon: 'wifi',
    status: 'ok',
    iconClass: 'rcl__item-icon--ok',
    desc: 'Kết nối mạng ổn định'
  }
  const screen = {
    id: 'screen', name: 'Màn hình', icon: 'fullscreen',
    status: 'ok',
    iconClass: 'rcl__item-icon--ok',
    desc: 'Sẵn sàng hiển thị đầy đủ'
  }

  if (!props.requireCameraMic) return [network, screen]
  return [camera, mic, network, screen]
})

const displayItems = computed(() => items.value)

const allReady = computed(() => items.value.every(i => i.status === 'ok'))

const overallStatusClass = computed(() => {
  if (props.isChecking) return 'rcl__summary--checking'
  if (allReady.value) return 'rcl__summary--ok'
  return 'rcl__summary--fail'
})

const overallIconClass = computed(() => {
  if (props.isChecking) return 'rcl__summary-icon--checking'
  if (allReady.value) return 'rcl__summary-icon--ok'
  return 'rcl__summary-icon--fail'
})

const overallIconName = computed(() => {
  if (props.isChecking) return 'progress_activity'
  if (allReady.value) return 'check_circle'
  return 'warning'
})

const overallStatusTitle = computed(() => {
  if (props.isChecking) return 'Đang kiểm tra...'
  if (allReady.value) return 'Máy tính sẵn sàng'
  return 'Có vấn đề cần kiểm tra'
})

const overallStatusSub = computed(() => {
  if (props.isChecking) return 'Vui lòng đợi trong giây lát...'
  if (allReady.value) return 'Bạn có thể bắt đầu làm bài thi'
  return 'Vui lòng kiểm tra lại các mục chưa đạt'
})
</script>

<style scoped>
.rcl {
  background: var(--ds-surface);
  border: 1.5px solid var(--ds-border);
  border-radius: var(--ds-radius-2xl);
  overflow: hidden;
}

.dark .rcl { border-color: var(--ds-border-strong); }

/* Header */
.rcl__header {
  display: flex;
  align-items: center;
  gap: 0.875rem;
  padding: 1.125rem 1.25rem;
  border-bottom: 1px solid var(--ds-border);
  background: var(--ds-gray-50);
  flex-wrap: wrap;
}

.dark .rcl__header {
  background: var(--ds-gray-800);
  border-bottom-color: var(--ds-border-strong);
}

.rcl__header-icon {
  width: 44px;
  height: 44px;
  border-radius: var(--ds-radius-xl);
  background: linear-gradient(135deg, rgba(14, 165, 233, 0.12) 0%, rgba(14, 165, 233, 0.06) 100%);
  color: var(--ds-info);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  box-shadow: 0 2px 8px rgba(14, 165, 233, 0.15);
}

.dark .rcl__header-icon {
  background: linear-gradient(135deg, rgba(14, 165, 233, 0.2) 0%, rgba(14, 165, 233, 0.1) 100%);
}

.rcl__header-title {
  font-family: var(--ds-font-display);
  font-size: 0.9rem;
  font-weight: 800;
  color: var(--ds-text);
  margin: 0;
}

.dark .rcl__header-title { color: var(--ds-text); }

.rcl__header-sub {
  font-size: 0.7rem;
  color: var(--ds-text-muted);
  margin: 0.2rem 0 0;
  font-weight: 500;
}

.rcl__retry-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.375rem;
  padding: 0.5rem 0.875rem;
  border-radius: var(--ds-radius-xl);
  background: var(--ds-gray-100);
  border: 1px solid var(--ds-border);
  color: var(--ds-text);
  font-size: 0.8rem;
  font-weight: 700;
  cursor: pointer;
  transition: color 0.12s ease, background-color 0.12s ease, border-color 0.12s ease, box-shadow 0.12s ease, transform 0.12s ease;
  font-family: inherit;
  margin-left: auto;
}

.dark .rcl__retry-btn {
  background: var(--ds-gray-700);
  border-color: var(--ds-border-strong);
  color: var(--ds-text);
}

.rcl__retry-btn:hover {
  background: var(--ds-primary-soft);
  border-color: var(--ds-primary-border);
  color: var(--ds-primary);
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(79, 70, 229, 0.15);
}

/* Summary */
.rcl__summary {
  display: flex;
  align-items: center;
  gap: 0.875rem;
  padding: 1rem 1.25rem;
  border-bottom: 1px solid var(--ds-border);
  transition: color 0.3s ease, background-color 0.3s ease, border-color 0.3s ease, box-shadow 0.3s ease, transform 0.3s ease;
}

.dark .rcl__summary { border-bottom-color: var(--ds-border-strong); }

.rcl__summary--ok { background: rgba(22, 163, 74, 0.04); border-bottom-color: rgba(22, 163, 74, 0.15); }
.rcl__summary--fail { background: rgba(220, 38, 38, 0.04); border-bottom-color: rgba(220, 38, 38, 0.15); }
.rcl__summary--checking { background: rgba(245, 158, 11, 0.04); border-bottom-color: rgba(245, 158, 11, 0.15); }

.rcl__summary-icon {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: color 0.3s ease, background-color 0.3s ease, border-color 0.3s ease, box-shadow 0.3s ease, transform 0.3s ease;
}

.rcl__summary-icon--ok {
  background: var(--ds-success-soft);
  color: var(--ds-success);
  box-shadow: 0 2px 8px rgba(22, 163, 74, 0.2);
  animation: rclOkPulse 3s ease-in-out infinite;
}

@keyframes rclOkPulse {
  0%, 100% { box-shadow: 0 2px 8px rgba(22, 163, 74, 0.2); }
  50% { box-shadow: 0 2px 16px rgba(22, 163, 74, 0.35); }
}

.rcl__summary-icon--fail {
  background: var(--ds-danger-soft);
  color: var(--ds-danger);
  box-shadow: 0 2px 8px rgba(220, 38, 38, 0.2);
}

.rcl__summary-icon--checking {
  background: rgba(245, 158, 11, 0.1);
  color: var(--ds-warning);
  box-shadow: 0 2px 8px rgba(245, 158, 11, 0.2);
}

.rcl__summary-text { flex: 1; }

.rcl__summary-title {
  font-size: 0.9rem;
  font-weight: 800;
  color: var(--ds-text);
  margin: 0;
  transition: color 0.3s ease;
}

.dark .rcl__summary-title { color: var(--ds-text); }
.rcl__summary--ok .rcl__summary-title { color: var(--ds-success); }
.rcl__summary--fail .rcl__summary-title { color: var(--ds-danger); }

.rcl__summary-sub {
  font-size: 0.75rem;
  color: var(--ds-text-muted);
  margin: 0.2rem 0 0;
  font-weight: 500;
}

.rcl__summary-spinner { flex-shrink: 0; }
.rcl__summary-spinner .lucide { color: var(--ds-warning); }

/* Items */
.rcl__items {
  display: flex;
  flex-direction: column;
}

.rcl__item {
  display: flex;
  align-items: center;
  gap: 0.875rem;
  padding: 0.875rem 1.25rem;
  border-bottom: 1px solid var(--ds-border);
  transition: color 0.15s ease, background-color 0.15s ease, border-color 0.15s ease, box-shadow 0.15s ease, transform 0.15s ease;
}

.dark .rcl__item { border-bottom-color: var(--ds-border-strong); }
.rcl__item:last-child { border-bottom: none; }
.rcl__item:hover { background: var(--ds-gray-50); padding-left: 1.5rem; }

.dark .rcl__item:hover { background: var(--ds-gray-800); }

/* Item icon */
.rcl__item-icon {
  width: 40px;
  height: 40px;
  border-radius: var(--ds-radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: color 0.2s ease, background-color 0.2s ease, border-color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease;
}

.rcl__item-icon--pending {
  background: var(--ds-gray-100);
  color: var(--ds-gray-400);
}

.dark .rcl__item-icon--pending { background: var(--ds-gray-700); color: var(--ds-gray-500); }

.rcl__item-icon--ok {
  background: var(--ds-success-soft);
  color: var(--ds-success);
  box-shadow: 0 1px 4px rgba(22, 163, 74, 0.15);
}

.rcl__item-icon--fail {
  background: var(--ds-danger-soft);
  color: var(--ds-danger);
  box-shadow: 0 1px 4px rgba(220, 38, 38, 0.15);
}

.rcl__item--ok:hover .rcl__item-icon--ok { transform: scale(1.1); }
.rcl__item--fail:hover .rcl__item-icon--fail { transform: scale(1.1) rotate(-5deg); }

.rcl__item-content {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 0.125rem;
}

.rcl__item-name {
  font-size: 0.875rem;
  font-weight: 700;
  color: var(--ds-text);
}

.dark .rcl__item-name { color: var(--ds-text); }

.rcl__item-desc {
  font-size: 0.72rem;
  color: var(--ds-text-muted);
  font-weight: 500;
}

.rcl__item--ok .rcl__item-desc { color: var(--ds-success); }
.rcl__item--fail .rcl__item-desc { color: var(--ds-danger); }

.rcl__item-status { flex-shrink: 0; }

.rcl__status-icon { font-size: 1.375rem; }
.rcl__status-icon--ok { color: var(--ds-success); }
.rcl__status-icon--fail { color: var(--ds-danger); }
.rcl__status-icon--pending { color: var(--ds-text-muted); }

.rcl__spinner { animation: rclSpin 1s linear infinite; }

@keyframes rclSpin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

/* Error */
.rcl__error {
  display: flex;
  align-items: flex-start;
  gap: 0.625rem;
  padding: 0.875rem 1.25rem;
  background: var(--ds-danger-soft);
  border-top: 1px solid rgba(220, 38, 38, 0.15);
  margin-top: auto;
}

.dark .rcl__error { background: rgba(220, 38, 38, 0.08); }

.rcl__error .lucide {
  color: var(--ds-danger);
  flex-shrink: 0;
  margin-top: 0.1rem;
}

.rcl__error p {
  font-size: 0.8rem;
  color: var(--ds-danger);
  font-weight: 600;
  margin: 0;
  line-height: 1.4;
}
</style>
@media (prefers-reduced-motion: reduce) {
  * {
    transition-duration: 0.01ms !important;
    animation-duration: 0.01ms !important;
  }
}