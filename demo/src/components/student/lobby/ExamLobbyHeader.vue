<template>
  <div class="elh">
    <!-- Top bar: sync status + support -->
    <div class="elh__topbar">
      <div class="elh__sync-badge">
        <span class="elh__sync-dot" :class="isSyncing ? 'elh__sync-dot--syncing' : 'elh__sync-dot--ok'" />
        <span class="elh__sync-text">{{ isSyncing ? 'Đang đồng bộ...' : 'Đồng bộ ổn định' }}</span>
        <span v-if="lastSyncedLabel" class="elh__sync-time">{{ lastSyncedLabel }}</span>
      </div>

      <button type="button" class="elh__support-link" @click="$emit('support')">
        <LucideIcon name="help" />
        Cần hỗ trợ?
      </button>
    </div>

    <!-- Hero card -->
    <div class="elh__hero">
      <!-- Status + countdown -->
      <div class="elh__hero-top">
        <StatusChip :status="statusKey" size="md" />
        <div v-if="countdownData && !isEnded" class="elh__countdown-wrap">
          <span class="elh__countdown-label">Bắt đầu sau</span>
          <span class="elh__countdown-val" :class="urgencyClass">
            {{ countdownData.hours }}:{{ countdownData.minutes }}:{{ countdownData.seconds }}
          </span>
        </div>
        <div v-else-if="isEnded" class="elh__countdown-wrap elh__countdown-wrap--ended">
          <span class="elh__countdown-val elh__countdown-val--ended">Đã kết thúc</span>
        </div>
        <div v-else class="elh__countdown-wrap elh__countdown-wrap--live">
          <span class="elh__countdown-val elh__countdown-val--live">San sang!</span>
        </div>
      </div>

      <!-- Exam title & meta -->
      <h1 class="elh__title">{{ examTitle }}</h1>

      <div class="elh__meta-grid">
        <div class="elh__meta-item">
          <LucideIcon name="schedule" />
          <div>
            <span class="elh__meta-val">{{ startAtDisplay || '—' }}</span>
            <span class="elh__meta-lbl">Thời gian bắt đầu</span>
          </div>
        </div>
        <div class="elh__meta-item">
          <LucideIcon name="timer" />
          <div>
            <span class="elh__meta-val">{{ duration }} phút</span>
            <span class="elh__meta-lbl">Thời gian làm bài</span>
          </div>
        </div>
        <div v-if="questionCount > 0" class="elh__meta-item">
          <LucideIcon name="help" />
          <div>
            <span class="elh__meta-val">{{ questionCount }} câu</span>
            <span class="elh__meta-lbl">Tổng số câu hỏi</span>
          </div>
        </div>
        <div v-if="examCode && examCode !== '-'" class="elh__meta-item">
          <LucideIcon name="tag" />
          <div>
            <span class="elh__meta-val">{{ examCode }}</span>
            <span class="elh__meta-lbl">Mã bài thi</span>
          </div>
        </div>
      </div>

      <!-- Proctoring badge if needed -->
      <div v-if="requireCameraMic" class="elh__proctor-badge">
        <LucideIcon name="videocam" />
        Có giám sát camera
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import StatusChip from '../../ui/StatusChip.vue'

const props = defineProps({
  examTitle: { type: String, default: 'Bài thi' },
  examCode: { type: String, default: '' },
  duration: { type: [Number, String], default: 60 },
  questionCount: { type: [Number, String], default: 0 },
  startAt: { type: [Date, String], default: null },
  endAt: { type: [Date, String], default: null },
  nowMs: { type: Number, default: () => Date.now() },
  isSyncing: { type: Boolean, default: false },
  lastSyncedLabel: { type: String, default: '' },
  requireCameraMic: { type: Boolean, default: false }
})

defineEmits(['support'])

// Status key
const statusKey = computed(() => {
  if (!props.startAt) return 'upcoming'
  const startMs = new Date(props.startAt).getTime()
  const endMs = props.endAt ? new Date(props.endAt).getTime() : null
  const now = props.nowMs

  if (endMs && now > endMs) return 'ended'
  if (!Number.isNaN(startMs) && now >= startMs) {
    if (endMs && now <= endMs) return 'live'
    if (!endMs) return 'live'
  }
  return 'upcoming'
})

const isEnded = computed(() => {
  if (!props.endAt) return false
  return props.nowMs > new Date(props.endAt).getTime()
})

// Countdown
const countdownData = computed(() => {
  if (!props.startAt || isEnded.value) return null
  const startMs = new Date(props.startAt).getTime()
  if (Number.isNaN(startMs)) return null
  const diff = startMs - props.nowMs
  if (diff <= 0) return null
  const totalSeconds = Math.floor(diff / 1000)
  const hours = Math.floor(totalSeconds / 3600)
  const minutes = Math.floor((totalSeconds % 3600) / 60)
  const seconds = totalSeconds % 60
  return {
    hours: String(hours).padStart(2, '0'),
    minutes: String(minutes).padStart(2, '0'),
    seconds: String(seconds).padStart(2, '0')
  }
})

const urgencyClass = computed(() => {
  if (!props.startAt) return ''
  const startMs = new Date(props.startAt).getTime()
  if (Number.isNaN(startMs)) return ''
  const hoursLeft = (startMs - props.nowMs) / 1000 / 60 / 60
  if (hoursLeft <= 1) return 'elh__countdown-val--critical'
  if (hoursLeft <= 6) return 'elh__countdown-val--urgent'
  return 'elh__countdown-val--soon'
})

const startAtDisplay = computed(() => {
  if (!props.startAt) return null
  const d = new Date(props.startAt)
  if (Number.isNaN(d.getTime())) return null
  return d.toLocaleString('vi-VN', {
    weekday: 'short', day: '2-digit', month: '2-digit',
    hour: '2-digit', minute: '2-digit'
  })
})
</script>


<style scoped>
.elh {
  display: flex;
  flex-direction: column;
  gap: 0.875rem;
}

/* Top bar */
.elh__topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
}

.elh__sync-badge {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.375rem 0.75rem;
  border-radius: var(--ds-radius-full);
  background: var(--ds-surface);
  border: 1px solid var(--ds-border);
}

.dark .elh__sync-badge { border-color: var(--ds-border-strong); }

.elh__sync-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}

.elh__sync-dot--ok { background: var(--ds-success); }
.elh__sync-dot--syncing {
  background: var(--ds-warning);
  animation: elhPulse 1s ease-in-out infinite;
}

@keyframes elhPulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.4; }
}

.elh__sync-text {
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--ds-text-muted);
}

.elh__sync-time {
  font-size: 0.7rem;
  color: var(--ds-text-muted);
  opacity: 0.7;
}

.elh__support-link {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  font-size: 0.8rem;
  font-weight: 600;
  color: var(--ds-text-muted);
  background: transparent;
  border: none;
  cursor: pointer;
  padding: 0.25rem 0.5rem;
  border-radius: var(--ds-radius-lg);
  transition: all 0.12s ease;
  font-family: inherit;
}

.elh__support-link:hover {
  color: var(--ds-primary);
  background: var(--ds-primary-soft);
}


/* Hero card */
.elh__hero {
  padding: 1.5rem 1.75rem;
  background: linear-gradient(135deg, #ffffff 0%, #f0f4ff 60%, #ede9fe 100%);
  border: 1.5px solid var(--ds-border);
  border-radius: var(--ds-radius-2xl);
  box-shadow: var(--ds-shadow-sm);
}

.dark .elh__hero {
  background: linear-gradient(135deg, #1e293b 0%, #1e1b4b 60%, #1e1b4b 100%);
  border-color: rgba(79, 70, 229, 0.15);
}

/* Hero top */
.elh__hero-top {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  margin-bottom: 1rem;
  flex-wrap: wrap;
}

/* Countdown */
.elh__countdown-wrap {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.375rem 0.875rem;
  border-radius: var(--ds-radius-full);
  background: rgba(79, 70, 229, 0.08);
  border: 1px solid rgba(79, 70, 229, 0.15);
  margin-left: auto;
}

.elh__countdown-wrap--live {
  background: rgba(22, 163, 74, 0.08);
  border-color: rgba(22, 163, 74, 0.2);
}

.elh__countdown-wrap--ended {
  background: var(--ds-gray-100);
  border-color: var(--ds-border);
}

.dark .elh__countdown-wrap--ended { background: var(--ds-gray-800); }

.elh__countdown-label {
  font-size: 0.65rem;
  font-weight: 700;
  color: var(--ds-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.elh__countdown-val {
  font-family: var(--ds-font-display);
  font-size: 1.125rem;
  font-weight: 900;
  color: var(--ds-primary);
  letter-spacing: 0.05em;
}

.elh__countdown-val--critical {
  color: var(--ds-danger);
  animation: elhBlink 1.5s ease-in-out infinite;
}

.elh__countdown-val--urgent { color: #c2410c; }
.elh__countdown-val--soon { color: var(--ds-primary); }
.elh__countdown-val--live { color: var(--ds-success); }
.elh__countdown-val--ended { color: var(--ds-text-muted); font-size: 0.875rem; }

@keyframes elhBlink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.6; }
}

/* Title */
.elh__title {
  font-family: var(--ds-font-display);
  font-size: 1.5rem;
  font-weight: 900;
  color: var(--ds-text);
  margin: 0 0 1.125rem;
  letter-spacing: -0.02em;
  line-height: 1.2;
}

.dark .elh__title { color: #f1f5f9; }

/* Meta grid */
.elh__meta-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 0.875rem;
  padding: 1rem 1.125rem;
  background: rgba(255, 255, 255, 0.75);
  border: 1px solid var(--ds-border);
  border-radius: var(--ds-radius-xl);
  backdrop-filter: blur(8px);
}

.dark .elh__meta-grid { background: rgba(30, 41, 59, 0.8); }

.elh__meta-item {
  display: flex;
  align-items: center;
  gap: 0.625rem;
}

.elh__meta-icon {
  font-size: 1.25rem;
  color: var(--ds-primary);
  flex-shrink: 0;
}

.elh__meta-item > div {
  display: flex;
  flex-direction: column;
  gap: 0.125rem;
}

.elh__meta-val {
  font-size: 0.875rem;
  font-weight: 700;
  color: var(--ds-text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.dark .elh__meta-val { color: #f1f5f9; }

.elh__meta-lbl {
  font-size: 0.65rem;
  font-weight: 600;
  color: var(--ds-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.05em;
  white-space: nowrap;
}

/* Proctoring badge */
.elh__proctor-badge {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 0.875rem;
  border-radius: var(--ds-radius-full);
  background: rgba(14, 165, 233, 0.08);
  border: 1px solid rgba(14, 165, 233, 0.2);
  color: var(--ds-info);
  font-size: 0.75rem;
  font-weight: 700;
  margin-top: 0.75rem;
}


/* Responsive */
@media (max-width: 480px) {
  .elh__hero { padding: 1.25rem; }
  .elh__meta-grid { grid-template-columns: 1fr; }
}
</style>
