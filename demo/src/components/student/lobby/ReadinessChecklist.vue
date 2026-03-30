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
    <div class="rcl__summary">
      <div class="rcl__summary-icon" :class="overallStatusIcon">
        <LucideIcon :name="overallStatusIcon" />
      </div>
      <div>
        <p class="rcl__summary-title">{{ overallStatusTitle }}</p>
        <p class="rcl__summary-sub">{{ overallStatusSub }}</p>
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
        <div class="rcl__item-icon">
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
      <LucideIcon name="info" />
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
    status: props.isChecking ? 'checking' : props.cameraReady ? 'ok' : 'fail',
    desc: props.cameraReady ? 'Camera hoạt động bình thường' : props.requireCameraMic ? 'Chưa cấp quyền hoặc không có camera' : 'Không bắt buộc'
  }
  const mic = {
    id: 'mic', name: 'Microphone', icon: 'mic',
    status: props.isChecking ? 'checking' : props.micReady ? 'ok' : 'fail',
    desc: props.micReady ? 'Microphone hoạt động bình thường' : props.requireCameraMic ? 'Chưa cấp quyền hoặc không có micro' : 'Không bắt buộc'
  }
  const network = {
    id: 'network', name: 'Mạng Internet', icon: 'wifi',
    status: 'ok',
    desc: 'Kết nối mạng ổn định'
  }
  const screen = {
    id: 'screen', name: 'Màn hình', icon: 'fullscreen',
    status: 'ok',
    desc: 'Sẵn sàng hiển thị đầy đủ'
  }

  if (!props.requireCameraMic) {
    return [network, screen]
  }
  return [camera, mic, network, screen]
})

const displayItems = computed(() => items.value)

const allReady = computed(() =>
  items.value.every(i => i.status === 'ok')
)

const overallStatusIcon = computed(() => {
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
  if (props.isChecking) return 'Đợi một chút...'
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

.dark .rcl__header { background: var(--ds-gray-800); border-bottom-color: var(--ds-border-strong); }

.rcl__header-icon {
  width: 44px;
  height: 44px;
  border-radius: var(--ds-radius-xl);
  background: rgba(14, 165, 233, 0.08);
  color: var(--ds-info);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}


.rcl__header-title {
  font-family: var(--ds-font-display);
  font-size: 0.9rem;
  font-weight: 800;
  color: var(--ds-text);
  margin: 0;
}

.dark .rcl__header-title { color: #f1f5f9; }

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
  transition: all 0.12s ease;
  font-family: inherit;
  margin-left: auto;
}

.dark .rcl__retry-btn { background: var(--ds-gray-700); border-color: var(--ds-border-strong); color: #f1f5f9; }

.rcl__retry-btn:hover {
  background: var(--ds-primary-soft);
  border-color: var(--ds-primary-border);
  color: var(--ds-primary);
}


/* Summary */
.rcl__summary {
  display: flex;
  align-items: center;
  gap: 0.875rem;
  padding: 1rem 1.25rem;
  border-bottom: 1px solid var(--ds-border);
}

.dark .rcl__summary { border-bottom-color: var(--ds-border-strong); }

.rcl__summary-icon {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}


.rcl__summary >
.rcl__summary >
.rcl__summary >

.rcl__summary > div:last-child {
  display: flex;
  flex-direction: column;
  gap: 0.125rem;
}

.rcl__summary-title {
  font-size: 0.9rem;
  font-weight: 800;
  color: var(--ds-text);
  margin: 0;
}

.dark .rcl__summary-title { color: #f1f5f9; }

.rcl__summary-sub {
  font-size: 0.75rem;
  color: var(--ds-text-muted);
  margin: 0;
  font-weight: 500;
}

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
  transition: background 0.1s ease;
}

.dark .rcl__item { border-bottom-color: var(--ds-border-strong); }
.rcl__item:last-child { border-bottom: none; }
.rcl__item:hover { background: var(--ds-gray-50); }
.dark .rcl__item:hover { background: var(--ds-gray-800); }

.rcl__item-icon {
  width: 40px;
  height: 40px;
  border-radius: var(--ds-radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  background: var(--ds-gray-100);
  color: var(--ds-gray-500);
}

.dark .rcl__item-icon { background: var(--ds-gray-700); color: var(--ds-gray-400); }

.rcl__item--ok .rcl__item-icon { background: var(--ds-success-soft); color: var(--ds-success); }
.rcl__item--fail .rcl__item-icon { background: var(--ds-danger-soft); color: var(--ds-danger); }


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

.dark .rcl__item-name { color: #f1f5f9; }

.rcl__item-desc {
  font-size: 0.72rem;
  color: var(--ds-text-muted);
  font-weight: 500;
}

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


.rcl__error p {
  font-size: 0.8rem;
  color: var(--ds-danger);
  font-weight: 600;
  margin: 0;
  line-height: 1.4;
}
</style>
