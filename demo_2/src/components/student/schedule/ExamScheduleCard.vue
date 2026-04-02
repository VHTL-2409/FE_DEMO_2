<template>
  <div class="esfc" :class="cardClass" @click="$emit('click', exam)">
    <!-- Left accent bar for urgent items -->
    <div v-if="isUrgent" class="esfc__accent" :class="accentClass" />

    <div class="esfc__inner">
      <!-- Status chip -->
      <div class="esfc__status-row">
        <StatusChip :status="statusConfig.key" size="sm" />
        <CountdownBadge :target-time="exam.startTime || exam.startDate" />
      </div>

      <!-- Main info -->
      <div class="esfc__main">
        <div class="esfc__title-row">
          <h3 class="esfc__title">{{ exam.title || exam.examTitle || 'Kỳ thi' }}</h3>
          <span v-if="exam.subject" class="esfc__subject-chip">{{ exam.subject }}</span>
        </div>

        <!-- Meta row -->
        <div class="esfc__meta">
          <span class="esfc__meta-item">
            <LucideIcon name="schedule" />
            {{ formatDateTime(exam.startTime || exam.startDate) }}
          </span>
          <span v-if="exam.duration || exam.durationMinutes" class="esfc__meta-item">
            <LucideIcon name="timer" />
            {{ exam.duration || exam.durationMinutes }} phút
          </span>
          <span v-if="exam.questionCount" class="esfc__meta-item">
            <LucideIcon name="help" />
            {{ exam.questionCount }} câu
          </span>
          <span v-if="exam.className || exam.room" class="esfc__meta-item">
            <LucideIcon name="class" />
            {{ exam.className || exam.room }}
          </span>
        </div>
      </div>

      <!-- Right side: actions -->
      <div class="esfc__actions">
        <template v-if="statusConfig.key === 'live'">
          <button type="button" class="esfc__btn esfc__btn--primary" @click.stop="$emit('enter', exam)">
            <LucideIcon name="login" />
            Vào thi ngay
          </button>
        </template>
        <template v-else-if="statusConfig.key === 'opening'">
          <button type="button" class="esfc__btn esfc__btn--primary" @click.stop="$emit('enter', exam)">
            <LucideIcon name="login" />
            Vào phòng chờ
          </button>
        </template>
        <template v-else-if="statusConfig.key === 'completed'">
          <button type="button" class="esfc__btn esfc__btn--secondary" @click.stop="$emit('details', exam)">
            <LucideIcon name="visibility" />
            Xem kết quả
          </button>
        </template>
        <template v-else>
          <button type="button" class="esfc__btn esfc__btn--secondary" @click.stop="$emit('details', exam)">
            Chi tiết
            <LucideIcon name="chevron_right" />
          </button>
        </template>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import StatusChip from '../../ui/StatusChip.vue'
import CountdownBadge from './CountdownBadge.vue'

const props = defineProps({
  exam: { type: Object, required: true }
})

defineEmits(['click', 'enter', 'details'])

const getStatus = (exam) => {
  const nowMs = Date.now()
  const startMs = new Date(exam.startTime || exam.startDate || '').getTime()
  const endMs = new Date(exam.endTime || exam.endDate || '').getTime()

  if (Number.isNaN(startMs)) return { key: 'upcoming', label: 'Sắp tới', color: 'primary' }
  if (Number.isNaN(endMs)) {
    if (nowMs >= startMs - 10 * 60 * 1000 && nowMs < startMs) return { key: 'opening', label: 'Sắp mở', color: 'info' }
    if (nowMs >= startMs && nowMs < startMs + 60 * 60 * 1000) return { key: 'live', label: 'Đang diễn ra', color: 'success' }
    if (nowMs < startMs) return { key: 'upcoming', label: 'Sắp tới', color: 'primary' }
    return { key: 'completed', label: 'Đã kết thúc', color: 'default' }
  }

  if (nowMs > endMs) return { key: 'completed', label: 'Đã kết thúc', color: 'default' }
  if (nowMs >= startMs - 10 * 60 * 1000 && nowMs < startMs) return { key: 'opening', label: 'Sắp mở', color: 'info' }
  if (nowMs >= startMs && nowMs <= endMs) return { key: 'live', label: 'Đang diễn ra', color: 'success' }
  return { key: 'upcoming', label: 'Sắp tới', color: 'primary' }
}

const statusConfig = computed(() => getStatus(props.exam))

const isUrgent = computed(() => {
  const { key } = statusConfig.value
  return key === 'live' || key === 'opening'
})

const cardClass = computed(() => ({
  'esfc--urgent': isUrgent.value,
  'esfc--completed': statusConfig.value.key === 'completed',
  'esfc--live': statusConfig.value.key === 'live'
}))

const accentClass = computed(() => {
  const { key } = statusConfig.value
  if (key === 'live') return 'esfc__accent--live'
  if (key === 'opening') return 'esfc__accent--opening'
  return ''
})

const formatDateTime = (value) => {
  if (!value) return '—'
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return '—'
  return d.toLocaleString('vi-VN', {
    weekday: 'short', day: '2-digit', month: '2-digit',
    hour: '2-digit', minute: '2-digit'
  })
}
</script>


<style scoped>
.esfc {
  position: relative;
  border-radius: var(--ds-radius-2xl);
  border: 1.5px solid var(--ds-border);
  background: var(--ds-surface);
  transition: all 0.15s ease;
  cursor: pointer;
  overflow: hidden;
}

.dark .esfc { border-color: var(--ds-border-strong); }

.esfc:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
  border-color: var(--ds-primary-border);
}

.esfc--urgent {
  border-color: rgba(79, 70, 229, 0.2);
  background: rgba(79, 70, 229, 0.02);
}

.dark .esfc--urgent { background: rgba(79, 70, 229, 0.05); }

.esfc--live {
  border-color: rgba(22, 163, 74, 0.25);
  background: rgba(22, 163, 74, 0.02);
}

.dark .esfc--live { background: rgba(22, 163, 74, 0.05); }

.esfc--completed { opacity: 0.7; }
.esfc--completed:hover { opacity: 1; }

/* Left accent bar */
.esfc__accent {
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 4px;
  border-radius: 4px 0 0 4px;
}

.esfc__accent--live { background: var(--ds-success); }
.esfc__accent--opening { background: var(--ds-info); }

/* Inner layout */
.esfc__inner {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 1rem 1.125rem;
}

/* Status + countdown row */
.esfc__status-row {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 0.375rem;
  flex-shrink: 0;
}

/* Main content */
.esfc__main {
  flex: 1;
  min-width: 0;
}

.esfc__title-row {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex-wrap: wrap;
  margin-bottom: 0.375rem;
}

.esfc__title {
  font-family: var(--ds-font-display);
  font-size: 0.95rem;
  font-weight: 800;
  color: var(--ds-text);
  margin: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.dark .esfc__title { color: #f1f5f9; }

.esfc__subject-chip {
  font-size: 0.65rem;
  font-weight: 700;
  color: var(--ds-primary);
  background: var(--ds-primary-soft);
  padding: 0.125rem 0.5rem;
  border-radius: var(--ds-radius-full);
  white-space: nowrap;
  flex-shrink: 0;
}

/* Meta */
.esfc__meta {
  display: flex;
  align-items: center;
  gap: 0.875rem;
  flex-wrap: wrap;
}

.esfc__meta-item {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  font-size: 0.72rem;
  color: var(--ds-text-muted);
  font-weight: 500;
}


/* Actions */
.esfc__actions { flex-shrink: 0; }

.esfc__btn {
  display: inline-flex;
  align-items: center;
  gap: 0.375rem;
  padding: 0.5rem 1rem;
  border-radius: var(--ds-radius-xl);
  font-size: 0.8rem;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.12s ease;
  border: 1.5px solid transparent;
  white-space: nowrap;
  font-family: inherit;
}

.esfc__btn:active { transform: scale(0.97); }

.esfc__btn--primary {
  background: var(--ds-primary);
  color: white;
  border-color: var(--ds-primary);
  box-shadow: 0 2px 8px rgba(139, 75, 0, 0.18);
}

.esfc__btn--primary:hover {
  background: var(--ds-primary-hover);
  box-shadow: 0 4px 12px rgba(79, 70, 229, 0.3);
}

.esfc__btn--secondary {
  background: var(--ds-gray-50);
  color: var(--ds-text);
  border-color: var(--ds-border);
}

.dark .esfc__btn--secondary {
  background: var(--ds-gray-800);
  border-color: var(--ds-border-strong);
  color: #f1f5f9;
}

.esfc__btn--secondary:hover {
  background: var(--ds-primary-soft);
  border-color: var(--ds-primary-border);
  color: var(--ds-primary);
}


/* Responsive */
@media (max-width: 600px) {
  .esfc__inner { flex-wrap: wrap; }
  .esfc__actions { width: 100%; }
  .esfc__btn { width: 100%; justify-content: center; }
}
</style>
