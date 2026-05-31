<template>
  <div class="td-monitor ds-animate-fade-up" style="animation-delay: 0.08s">
    <!-- Card Header -->
    <div class="td-monitor__header">
      <div class="td-monitor__header-left">
        <div class="td-monitor__live-badge">
          <span class="td-monitor__live-dot" />
          <span>ĐANG GIÁM SÁT</span>
        </div>
        <h2 class="td-monitor__title">{{ liveExam.title }}</h2>
      </div>

      <div class="td-monitor__header-right">
        <div class="td-monitor__connection">
          <LucideIcon name="wifi" td-monitor__connection-icon />
          <span>Real-time</span>
        </div>
        <button type="button" class="td-monitor__manage-btn" @click="$emit('go-monitoring-exam', liveExam)">
          <LucideIcon name="open_in_new" />
          Quản lý Proctoring
        </button>
      </div>
    </div>

    <!-- Card Body: 4-column info grid -->
    <div class="td-monitor__info-grid">
      <!-- Subject / Title -->
      <div class="td-monitor__info-item">
        <div class="td-monitor__info-icon td-monitor__info-icon--primary">
          <LucideIcon name="menu_book" />
        </div>
        <div class="td-monitor__info-body">
          <span class="td-monitor__info-label">Môn thi</span>
          <span class="td-monitor__info-value">{{ liveExam.title }}</span>
        </div>
      </div>

      <!-- Students -->
      <div class="td-monitor__info-item">
        <div class="td-monitor__info-icon td-monitor__info-icon--success">
          <LucideIcon name="group" />
        </div>
        <div class="td-monitor__info-body">
          <span class="td-monitor__info-label">Thí sinh</span>
          <span class="td-monitor__info-value">{{ liveExam.studentCount }} học sinh</span>
        </div>
      </div>

      <!-- Time -->
      <div class="td-monitor__info-item">
        <div class="td-monitor__info-icon td-monitor__info-icon--accent">
          <LucideIcon name="schedule" />
        </div>
        <div class="td-monitor__info-body">
          <span class="td-monitor__info-label">Thời gian</span>
          <span class="td-monitor__info-value">{{ timeLabel }}</span>
        </div>
      </div>

      <!-- Alerts -->
      <div class="td-monitor__info-item">
        <div class="td-monitor__info-icon" :class="alertIconClass">
          <LucideIcon :name="alertIcon" />
        </div>
        <div class="td-monitor__info-body">
          <span class="td-monitor__info-label">Cảnh báo</span>
          <span class="td-monitor__info-value" :class="alertTextClass">
            {{ liveExam.alertCount }} cảnh báo
          </span>
        </div>
      </div>
    </div>

    <!-- Progress Bar -->
    <div class="td-monitor__progress">
      <div class="td-monitor__progress-header">
        <span class="td-monitor__progress-label">Tiến độ phiên thi</span>
        <span class="td-monitor__progress-stat">
          {{ liveExam.answeredCount }} / {{ liveExam.questionCount || 0 }} câu
        </span>
      </div>
      <div class="td-monitor__progress-track">
        <div class="td-monitor__progress-bar" :style="{ width: progressPercent + '%' }" />
      </div>
      <div class="td-monitor__progress-footer">
        <span class="td-monitor__time-spent">
          <LucideIcon name="hourglass_bottom" />
          Đã thi: {{ elapsedTime }}
        </span>
        <span class="td-monitor__time-left">
          <LucideIcon name="hourglass_top" />
          Còn lại: {{ remainingTime }}
        </span>
      </div>
    </div>

    <!-- Alert Banner (if alerts > 0) -->
    <div v-if="liveExam.alertCount > 0" class="td-monitor__alert-banner">
      <div class="td-monitor__alert-banner-icon">
        <LucideIcon name="warning" />
      </div>
      <div class="td-monitor__alert-banner-body">
        <span class="td-monitor__alert-banner-title">Có {{ liveExam.alertCount }} cảnh báo proctor cần xử lý</span>
        <span class="td-monitor__alert-banner-sub">Vào phòng giám sát để xem chi tiết và hành động</span>
      </div>
      <button type="button" class="td-monitor__alert-banner-cta" @click="$emit('go-monitoring-exam', liveExam)">
        Xử lý ngay
        <LucideIcon name="arrow_forward" />
      </button>
    </div>

  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  liveExam: {
    type: Object,
    required: true
  }
})

defineEmits(['go-monitoring', 'go-monitoring-exam'])

const progressPercent = computed(() => {
  const total = Number(props.liveExam.questionCount || 0)
  if (!total) return 0
  return Math.min(100, Math.round((Number(props.liveExam.answeredCount || 0) / total) * 100))
})

const timeLabel = computed(() => {
  const start = new Date(props.liveExam.startTime || '')
  const end = new Date(props.liveExam.endTime || '')
  if (Number.isNaN(start.getTime())) return '—'
  const opts = { hour: '2-digit', minute: '2-digit' }
  const startStr = start.toLocaleTimeString('vi-VN', opts)
  const endStr = Number.isNaN(end.getTime()) ? '...' : end.toLocaleTimeString('vi-VN', opts)
  return `${startStr} – ${endStr}`
})

const elapsedTime = computed(() => {
  const start = new Date(props.liveExam.startTime || '')
  if (Number.isNaN(start.getTime())) return '—'
  const diff = Math.max(0, Date.now() - start.getTime())
  const mins = Math.floor(diff / 60000)
  if (mins < 60) return `${mins} phút`
  return `${Math.floor(mins / 60)}h ${mins % 60}m`
})

const remainingTime = computed(() => {
  const end = new Date(props.liveExam.endTime || '')
  if (Number.isNaN(end.getTime())) return '—'
  const diff = Math.max(0, end.getTime() - Date.now())
  if (diff <= 0) return 'Hết giờ'
  const mins = Math.floor(diff / 60000)
  if (mins < 60) return `${mins} phút`
  return `${Math.floor(mins / 60)}h ${mins % 60}m`
})

const alertIconClass = computed(() =>
  props.liveExam.alertCount > 0 ? 'td-monitor__info-icon--danger' : 'td-monitor__info-icon--success'
)
const alertIcon = computed(() => props.liveExam.alertCount > 0 ? 'flag' : 'verified')
const alertTextClass = computed(() =>
  props.liveExam.alertCount > 0 ? 'td-monitor__info-value--danger' : ''
)
</script>


<style scoped>
.td-monitor {
  background: var(--ds-surface);
  border: 1px solid rgba(22, 163, 74, 0.25);
  border-radius: var(--ds-radius-2xl);
  box-shadow: var(--ds-shadow-md);
  overflow: hidden;
  position: relative;
}

/* Top accent stripe */
.td-monitor::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: linear-gradient(90deg, var(--ds-success) 0%, #34d399 50%, var(--ds-success) 100%);
  background-size: 200% 100%;
  animation: td-monitor-stripe 2s linear infinite;
}

@keyframes td-monitor-stripe {
  0% { background-position: 0% 0%; }
  100% { background-position: 200% 0%; }
}

/* Header */
.td-monitor__header {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
  padding: 1.5rem 1.5rem 1rem;
  border-bottom: 1px solid var(--ds-border);
}

.td-monitor__header-left {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.td-monitor__live-badge {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.25rem 0.75rem;
  background: var(--ds-success);
  border-radius: var(--ds-radius-full);
  font-size: 0.65rem;
  font-weight: 800;
  color: white;
  letter-spacing: 0.12em;
  width: fit-content;
}

.td-monitor__live-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: white;
  animation: td-live-blink 1.5s ease-in-out infinite;
}

@keyframes td-live-blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.3; }
}

.td-monitor__title {
  font-family: var(--ds-font-display);
  font-size: 1.25rem;
  font-weight: 800;
  color: var(--ds-text);
  letter-spacing: -0.02em;
  margin: 0;
  line-height: 1.2;
}

.dark .td-monitor__title {
  color: #f1f5f9;
}

.td-monitor__header-right {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.75rem;
  flex-shrink: 0;
  justify-content: flex-end;
}

.td-monitor__connection {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  padding: 0.375rem 0.75rem;
  background: var(--ds-success-soft);
  border-radius: var(--ds-radius-full);
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--ds-success);
}

.td-monitor__connection-icon {
  font-size: 1rem;
}

.td-monitor__manage-btn {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 1rem;
  border-radius: var(--ds-radius-lg);
  border: 1px solid var(--ds-primary-border);
  background: var(--ds-primary-soft);
  color: var(--ds-primary);
  font-size: 0.8rem;
  font-weight: 700;
  cursor: pointer;
  transition: color 0.15s ease, background-color 0.15s ease, border-color 0.15s ease, box-shadow 0.15s ease, transform 0.15s ease;
}

.td-monitor__manage-btn:hover {
  background: var(--ds-primary);
  color: white;
  box-shadow: 0 4px 12px rgba(79, 70, 229, 0.25);
}

/* Info Grid */
.td-monitor__info-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 0;
  border-bottom: 1px solid var(--ds-border);
}

@media (max-width: 1024px) {
  .td-monitor__info-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 520px) {
  .td-monitor__info-grid {
    grid-template-columns: 1fr;
  }
}

.td-monitor__info-item {
  display: flex;
  align-items: center;
  gap: 0.875rem;
  padding: 1.25rem 1.25rem;
  border-right: 1px solid var(--ds-border);
  min-width: 0;
}

.td-monitor__info-item:last-child {
  border-right: none;
}

@media (max-width: 1024px) {
  .td-monitor__info-item:nth-child(2) {
    border-right: none;
  }

  .td-monitor__info-item:nth-child(1),
  .td-monitor__info-item:nth-child(2) {
    border-bottom: 1px solid var(--ds-border);
  }
}

@media (max-width: 520px) {
  .td-monitor__info-item {
    border-right: none;
    border-bottom: 1px solid var(--ds-border);
  }

  .td-monitor__info-item:last-child {
    border-bottom: none;
  }
}

.td-monitor__info-icon {
  width: 40px;
  height: 40px;
  border-radius: var(--ds-radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}


.td-monitor__info-icon--primary {
  background: var(--ds-primary-soft);
  color: var(--ds-primary);
}

.td-monitor__info-icon--success {
  background: var(--ds-success-soft);
  color: var(--ds-success);
}

.td-monitor__info-icon--accent {
  background: var(--ds-accent-soft);
  color: var(--ds-accent);
}

.td-monitor__info-icon--danger {
  background: var(--ds-danger-soft);
  color: var(--ds-danger);
}

.td-monitor__info-body {
  display: flex;
  flex-direction: column;
  gap: 0.125rem;
  min-width: 0;
}

.td-monitor__info-label {
  font-size: 0.65rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  color: var(--ds-text-muted);
  white-space: nowrap;
}

.td-monitor__info-value {
  font-size: 0.875rem;
  font-weight: 700;
  color: var(--ds-text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.dark .td-monitor__info-value {
  color: #f1f5f9;
}

.td-monitor__info-value--danger {
  color: var(--ds-danger);
}

/* Progress */
.td-monitor__progress {
  padding: 1rem 1.5rem;
  border-bottom: 1px solid var(--ds-border);
  display: flex;
  flex-direction: column;
  gap: 0.625rem;
}

.td-monitor__progress-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.td-monitor__progress-label {
  font-size: 0.8rem;
  font-weight: 600;
  color: var(--ds-text-secondary);
}

.td-monitor__progress-stat {
  font-size: 0.8rem;
  font-weight: 700;
  color: var(--ds-primary);
}

.td-monitor__progress-track {
  height: 6px;
  background: var(--ds-gray-200);
  border-radius: var(--ds-radius-full);
  overflow: hidden;
}

.dark .td-monitor__progress-track {
  background: var(--ds-gray-700);
}

.td-monitor__progress-bar {
  height: 100%;
  background: linear-gradient(90deg, var(--ds-primary) 0%, #818cf8 100%);
  border-radius: var(--ds-radius-full);
  transition: width 0.5s ease;
  position: relative;
}

.td-monitor__progress-bar::after {
  content: '';
  position: absolute;
  top: 0;
  right: 0;
  bottom: 0;
  width: 30px;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.3));
}

.td-monitor__progress-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.td-monitor__time-spent,
.td-monitor__time-left {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--ds-text-muted);
}


/* Alert Banner */
.td-monitor__alert-banner {
  display: flex;
  align-items: center;
  gap: 0.875rem;
  padding: 0.875rem 1.5rem;
  background: var(--ds-danger-soft);
  border-bottom: 1px solid rgba(220, 38, 38, 0.1);
}

.td-monitor__alert-banner-icon {
  width: 36px;
  height: 36px;
  border-radius: var(--ds-radius-lg);
  background: var(--ds-danger);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}


.td-monitor__alert-banner-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 0.125rem;
}

.td-monitor__alert-banner-title {
  font-size: 0.8rem;
  font-weight: 700;
  color: var(--ds-danger);
}

.td-monitor__alert-banner-sub {
  font-size: 0.7rem;
  color: var(--ds-text-muted);
}

.td-monitor__alert-banner-cta {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  padding: 0.5rem 1rem;
  border-radius: var(--ds-radius-lg);
  border: none;
  background: var(--ds-danger);
  color: white;
  font-size: 0.8rem;
  font-weight: 700;
  cursor: pointer;
  transition: color 0.15s ease, background-color 0.15s ease, border-color 0.15s ease, box-shadow 0.15s ease, transform 0.15s ease;
  white-space: nowrap;
  flex-shrink: 0;
}

.td-monitor__alert-banner-cta:hover {
  background: #b91c1c;
  box-shadow: 0 4px 12px rgba(220, 38, 38, 0.3);
}

</style>
