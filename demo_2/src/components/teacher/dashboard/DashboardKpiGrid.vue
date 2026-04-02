<template>
  <div class="td-kpi-grid ds-animate-fade-up" style="animation-delay: 0.05s">
    <!-- Card: Total -->
    <div class="td-kpi-card td-kpi-card--default">
      <div class="td-kpi-card__header">
        <div class="td-kpi-card__icon-wrap td-kpi-card__icon-wrap--primary">
          <LucideIcon name="inventory_2" />
        </div>
        <span class="td-kpi-card__badge td-kpi-card__badge--total">Tổng</span>
      </div>
      <div class="td-kpi-card__body">
        <span class="td-kpi-card__value">{{ stats.total }}</span>
        <span class="td-kpi-card__sub">đề thi đã tạo</span>
      </div>
      <div class="td-kpi-card__footer">
        <span class="td-kpi-card__trend td-kpi-card__trend--neutral">
          <LucideIcon name="bar_chart" />
          Tất cả các trạng thái
        </span>
      </div>
    </div>

    <!-- Card: Ongoing (highlighted) -->
    <div class="td-kpi-card td-kpi-card--ongoing" :class="{ 'td-kpi-card--live': stats.active > 0 }">
      <div class="td-kpi-card__glow" v-if="stats.active > 0" />
      <div class="td-kpi-card__header">
        <div class="td-kpi-card__icon-wrap td-kpi-card__icon-wrap--success">
          <LucideIcon name="timer" />
        </div>
        <span v-if="stats.active > 0" class="td-kpi-card__live-dot">
          <span class="td-kpi-card__live-ring" />
          ĐANG THI
        </span>
        <span v-else class="td-kpi-card__badge td-kpi-card__badge--ongoing">Đang diễn ra</span>
      </div>
      <div class="td-kpi-card__body">
        <span class="td-kpi-card__value">{{ stats.active }}</span>
        <span class="td-kpi-card__sub">kỳ thi đang thi</span>
      </div>
      <div class="td-kpi-card__footer">
        <span v-if="stats.active > 0" class="td-kpi-card__trend td-kpi-card__trend--success">
          <LucideIcon name="check_circle" />
          Có thể giám sát ngay
        </span>
        <span v-else class="td-kpi-card__trend td-kpi-card__trend--muted">
          <LucideIcon name="schedule" />
          Không có kỳ thi đang thi
        </span>
      </div>
    </div>

    <!-- Card: Scheduled -->
    <div class="td-kpi-card td-kpi-card--scheduled">
      <div class="td-kpi-card__header">
        <div class="td-kpi-card__icon-wrap td-kpi-card__icon-wrap--info">
          <LucideIcon name="schedule" />
        </div>
        <span class="td-kpi-card__badge td-kpi-card__badge--scheduled">Sắp tới</span>
      </div>
      <div class="td-kpi-card__body">
        <span class="td-kpi-card__value">{{ stats.upcoming }}</span>
        <span class="td-kpi-card__sub">kỳ thi sắp tới</span>
      </div>
      <div class="td-kpi-card__footer">
        <span class="td-kpi-card__trend td-kpi-card__trend--info">
          <LucideIcon name="event" />
          {{ upcomingDaysLabel }}
        </span>
      </div>
    </div>

    <!-- Card: Alerts (danger when > 0) -->
    <div class="td-kpi-card" :class="alertCardClass">
      <div class="td-kpi-card__header">
        <div class="td-kpi-card__icon-wrap" :class="alertIconClass">
          <LucideIcon :name="alertIcon" />
        </div>
        <span v-if="alertCount > 0" class="td-kpi-card__badge td-kpi-card__badge--danger">
          Cần xử lý
        </span>
        <span v-else class="td-kpi-card__badge td-kpi-card__badge--safe">Không có cảnh báo</span>
      </div>
      <div class="td-kpi-card__body">
        <span class="td-kpi-card__value">{{ alertCount }}</span>
        <span class="td-kpi-card__sub">cảnh báo / kỳ thi đã kết</span>
      </div>
      <div class="td-kpi-card__footer">
        <span :class="alertTrendClass">
          <LucideIcon :name="alertTrendIcon" />
          {{ alertTrendLabel }}
        </span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  stats: {
    type: Object,
    default: () => ({
      total: 0,
      active: 0,
      upcoming: 0,
      ended: 0,
      draft: 0
    })
  },
  alertCount: {
    type: Number,
    default: 0
  }
})

const upcomingDaysLabel = computed(() => {
  const u = props.stats.upcoming
  if (u === 0) return 'Không có lịch'
  if (u === 1) return '1 kỳ thi sắp tới'
  return `${u} kỳ thi sắp tới`
})

// Alert card logic
const alertCardClass = computed(() => {
  if (props.alertCount > 0) return 'td-kpi-card--alert'
  return 'td-kpi-card--safe'
})

const alertIconClass = computed(() => {
  if (props.alertCount > 0) return 'td-kpi-card__icon-wrap--danger'
  return 'td-kpi-card__icon-wrap--success'
})

const alertIcon = computed(() => props.alertCount > 0 ? 'warning' : 'verified')

const alertTrendClass = computed(() => {
  if (props.alertCount > 0) return 'td-kpi-card__trend td-kpi-card__trend--danger'
  return 'td-kpi-card__trend td-kpi-card__trend--success'
})

const alertTrendIcon = computed(() => props.alertCount > 0 ? 'flag' : 'check_circle')

const alertTrendLabel = computed(() => {
  if (props.alertCount > 0) return `${props.alertCount} cần xử lý ngay`
  return 'Không có cảnh báo'
})
</script>


<style scoped>
.td-kpi-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 1rem;
}

@media (max-width: 1024px) {
  .td-kpi-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 480px) {
  .td-kpi-grid {
    grid-template-columns: 1fr;
  }
}

/* ===== KPI Card Base ===== */
.td-kpi-card {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 0.875rem;
  padding: 1.375rem;
  background: var(--ds-surface);
  border: 1px solid var(--ds-border);
  border-radius: var(--ds-radius-2xl);
  box-shadow: var(--ds-shadow-xs);
  transition: all 0.2s ease;
  overflow: hidden;
}

.td-kpi-card:hover {
  box-shadow: var(--ds-shadow-md);
  transform: translateY(-2px);
  border-color: rgba(148, 163, 184, 0.3);
}

.dark .td-kpi-card:hover {
  border-color: var(--ds-border-strong);
}

/* Glow effect for live card */
.td-kpi-card--live {
  border-color: rgba(22, 163, 74, 0.25);
  background: linear-gradient(135deg, #ffffff 0%, #f0fdf4 100%);
}

.dark .td-kpi-card--live {
  background: linear-gradient(135deg, #1e293b 0%, rgba(22, 163, 74, 0.08) 100%);
}

.td-kpi-card__glow {
  position: absolute;
  top: -20px;
  right: -20px;
  width: 100px;
  height: 100px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(22, 163, 74, 0.15) 0%, transparent 70%);
  pointer-events: none;
}

/* Header */
.td-kpi-card__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.5rem;
}

.td-kpi-card__icon-wrap {
  width: 44px;
  height: 44px;
  border-radius: var(--ds-radius-xl);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.td-kpi-card__icon-wrap--primary {
  background: var(--ds-primary-soft);
}

.td-kpi-card__icon-wrap--success {
  background: var(--ds-success-soft);
}

.td-kpi-card__icon-wrap--info {
  background: var(--ds-info-soft);
}

.td-kpi-card__icon-wrap--danger {
  background: var(--ds-danger-soft);
}

.td-kpi-card__icon {
  font-size: 1.375rem;
  color: var(--ds-primary);
}

.td-kpi-card__icon-wrap--success .td-kpi-card__icon {
  color: var(--ds-success);
}

.td-kpi-card__icon-wrap--info .td-kpi-card__icon {
  color: var(--ds-info);
}

.td-kpi-card__icon-wrap--danger .td-kpi-card__icon {
  color: var(--ds-danger);
}

.td-kpi-card__icon-wrap--safe {
  background: var(--ds-success-soft);
}

/* Live dot */
.td-kpi-card__live-dot {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  padding: 0.25rem 0.625rem;
  background: var(--ds-success);
  border-radius: var(--ds-radius-full);
  font-size: 0.6rem;
  font-weight: 800;
  color: white;
  letter-spacing: 0.1em;
  position: relative;
}

.td-kpi-card__live-ring {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: white;
  animation: td-live-pulse 1.5s ease-in-out infinite;
}

@keyframes td-live-pulse {
  0%, 100% { transform: scale(1); opacity: 1; }
  50% { transform: scale(0.6); opacity: 0.5; }
}

/* Badge */
.td-kpi-card__badge {
  padding: 0.25rem 0.625rem;
  border-radius: var(--ds-radius-full);
  font-size: 0.65rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.08em;
}

.td-kpi-card__badge--total {
  background: var(--ds-primary-soft);
  color: var(--ds-primary);
}

.td-kpi-card__badge--ongoing {
  background: var(--ds-success-soft);
  color: var(--ds-success);
}

.td-kpi-card__badge--scheduled {
  background: var(--ds-info-soft);
  color: var(--ds-info);
}

.td-kpi-card__badge--danger {
  background: var(--ds-danger-soft);
  color: var(--ds-danger);
}

.td-kpi-card__badge--safe {
  background: var(--ds-success-soft);
  color: var(--ds-success);
}

/* Body */
.td-kpi-card__body {
  display: flex;
  flex-direction: column;
  gap: 0.125rem;
}

.td-kpi-card__value {
  font-family: var(--ds-font-display);
  font-size: 2.5rem;
  font-weight: 800;
  color: var(--ds-text);
  letter-spacing: -0.03em;
  line-height: 1;
  font-variant-numeric: tabular-nums;
}

.dark .td-kpi-card__value {
  color: #f1f5f9;
}

.td-kpi-card--live .td-kpi-card__value {
  color: var(--ds-success);
}

.td-kpi-card--alert .td-kpi-card__value {
  color: var(--ds-danger);
}

.td-kpi-card__sub {
  font-size: 0.8rem;
  font-weight: 500;
  color: var(--ds-text-muted);
  margin-top: 0.25rem;
}

/* Footer trend */
.td-kpi-card__footer {
  margin-top: auto;
  padding-top: 0.5rem;
  border-top: 1px solid var(--ds-border);
}

.td-kpi-card__trend {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  font-size: 0.75rem;
  font-weight: 600;
}


.td-kpi-card__trend--neutral {
  color: var(--ds-text-muted);
}

.td-kpi-card__trend--success {
  color: var(--ds-success);
}

.td-kpi-card__trend--info {
  color: var(--ds-info);
}

.td-kpi-card__trend--danger {
  color: var(--ds-danger);
}

.td-kpi-card__trend--muted {
  color: var(--ds-text-muted);
}
</style>
