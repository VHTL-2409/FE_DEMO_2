<template>
  <div class="td-kpi-grid">
    <!-- Card: Total -->
    <div class="td-kpi-card td-kpi-card--primary td-kpi-card--1">
      <div class="td-kpi-card__header">
        <div class="td-kpi-card__icon-wrap">
          <LucideIcon name="inventory_2" />
        </div>
        <span class="td-kpi-card__badge td-kpi-card__badge--total">Tổng</span>
      </div>
      <div class="td-kpi-card__body">
        <span class="td-kpi-card__value">{{ stats.total }}</span>
        <span class="td-kpi-card__sub">đề thi đã tạo</span>
      </div>
      <div class="td-kpi-card__footer">
        <span class="td-kpi-card__trend">
          <LucideIcon name="bar_chart" />
          Tất cả các trạng thái
        </span>
      </div>
    </div>

    <!-- Card: Ongoing (highlighted) -->
    <div class="td-kpi-card td-kpi-card--success" :class="{ 'td-kpi-card--live': stats.active > 0 }">
      <div class="td-kpi-card__glow" v-if="stats.active > 0" />
      <div class="td-kpi-card__header">
        <div class="td-kpi-card__icon-wrap">
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
        <span v-else class="td-kpi-card__trend">
          <LucideIcon name="schedule" />
          Không có kỳ thi đang thi
        </span>
      </div>
    </div>

    <!-- Card: Scheduled -->
    <div class="td-kpi-card td-kpi-card--info td-kpi-card--3">
      <div class="td-kpi-card__header">
        <div class="td-kpi-card__icon-wrap">
          <LucideIcon name="schedule" />
        </div>
        <span class="td-kpi-card__badge td-kpi-card__badge--scheduled">Sắp tới</span>
      </div>
      <div class="td-kpi-card__body">
        <span class="td-kpi-card__value">{{ stats.upcoming }}</span>
        <span class="td-kpi-card__sub">kỳ thi sắp tới</span>
      </div>
      <div class="td-kpi-card__footer">
        <span class="td-kpi-card__trend">
          <LucideIcon name="event" />
          {{ upcomingDaysLabel }}
        </span>
      </div>
    </div>

    <!-- Card: Alerts (danger when > 0) -->
    <div class="td-kpi-card td-kpi-card--4" :class="{ 'td-kpi-card--danger': alertCount > 0, 'td-kpi-card--safe': alertCount === 0 }">
      <div class="td-kpi-card__header">
        <div class="td-kpi-card__icon-wrap">
          <LucideIcon :name="alertIcon" />
        </div>
        <span v-if="alertCount > 0" class="td-kpi-card__badge td-kpi-card__badge--danger">
          Cần xử lý
        </span>
        <span v-else class="td-kpi-card__badge td-kpi-card__badge--safe">Không có cảnh báo</span>
      </div>
      <div class="td-kpi-card__body">
        <span class="td-kpi-card__value">{{ alertCount }}</span>
        <span class="td-kpi-card__sub">cảnh báo</span>
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

const alertIcon = computed(() => props.alertCount > 0 ? 'warning' : 'verified')

const alertTrendClass = computed(() => {
  return props.alertCount > 0 ? 'td-kpi-card__trend td-kpi-card__trend--danger' : 'td-kpi-card__trend td-kpi-card__trend--success'
})

const alertTrendIcon = computed(() => props.alertCount > 0 ? 'flag' : 'check_circle')

const alertTrendLabel = computed(() => {
  if (props.alertCount === 0) return 'Hệ thống ổn định'
  return `${props.alertCount} cần xử lý`
})
</script>


<style scoped>
/* Ultra Simplified */

.td-kpi-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  border: 1px solid var(--ds-gray-200);
  border-radius: var(--ds-radius-lg, 14px);
  overflow: hidden;
  background: var(--ds-surface);
}

.td-kpi-card {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 0.875rem;
  min-width: 0;
  padding: 1.125rem 1.25rem;
  background: var(--ds-surface);
  border: none;
  border-right: 1px solid var(--ds-gray-200);
  animation: fadeInUp 0.5s cubic-bezier(0.34, 1.56, 0.64, 1) both;
}

.td-kpi-card--1 { animation-delay: 0s; }
.td-kpi-card--2 { animation-delay: 0.06s; }
.td-kpi-card--3 { animation-delay: 0.12s; }
.td-kpi-card--4 { animation-delay: 0.18s; }

.td-kpi-card:last-child { border-right: none; }
.td-kpi-card:hover { background: var(--ds-gray-50); }

.td-kpi-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 3px;
  height: 100%;
  opacity: 0.7;
  transition: opacity 0.15s ease;
}

.td-kpi-card--primary::before  { background: var(--ds-primary); }
.td-kpi-card--success::before { background: var(--ds-success); }
.td-kpi-card--info::before    { background: var(--ds-info); }
.td-kpi-card--danger::before  { background: var(--ds-danger); }
.td-kpi-card--safe::before    { background: var(--ds-success); }

.td-kpi-card:hover::before { opacity: 1; }

.td-kpi-card__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 0.5rem;
  min-width: 0;
}

.td-kpi-card__icon-wrap {
  width: 44px;
  height: 44px;
  border-radius: var(--ds-radius-xl);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: transform 0.2s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.td-kpi-card:hover .td-kpi-card__icon-wrap {
  transform: scale(1.08);
}

.td-kpi-card__icon-wrap { background: var(--ds-primary-soft); color: var(--ds-primary); }

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
}

.td-kpi-card__live-ring {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: white;
  animation: pulse 2s ease-in-out infinite;
}

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

.td-kpi-card__badge {
  padding: 0.25rem 0.625rem;
  border-radius: var(--ds-radius-full);
  font-size: 0.65rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.08em;
}

.td-kpi-card__badge--total     { background: var(--ds-primary-soft); color: var(--ds-primary); }
.td-kpi-card__badge--ongoing   { background: var(--ds-success-soft); color: var(--ds-success); }
.td-kpi-card__badge--scheduled { background: var(--ds-info-soft); color: var(--ds-info); }
.td-kpi-card__badge--danger     { background: var(--ds-danger-soft); color: var(--ds-danger); }
.td-kpi-card__badge--safe       { background: var(--ds-success-soft); color: var(--ds-success); }

.td-kpi-card__body {
  display: flex;
  flex-direction: column;
  gap: 0.125rem;
}

.td-kpi-card__value {
  font-family: var(--ds-font-display);
  font-size: clamp(1.65rem, 2.8vw + 0.9rem, 2.5rem);
  font-weight: 800;
  color: var(--ds-text);
  letter-spacing: -0.03em;
  line-height: 1;
  font-variant-numeric: tabular-nums;
}

.dark .td-kpi-card__value { color: #f1f5f9; }
.td-kpi-card--live .td-kpi-card__value { color: var(--ds-success); }
.td-kpi-card--danger .td-kpi-card__value { color: var(--ds-danger); }

.td-kpi-card__sub {
  font-size: 0.8rem;
  font-weight: 500;
  color: var(--ds-text-muted);
  margin-top: 0.25rem;
  line-height: 1.35;
  overflow-wrap: break-word;
  word-break: normal;
}

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
  color: var(--ds-text-muted);
}

.td-kpi-card__trend--success { color: var(--ds-success); }
.td-kpi-card__trend--danger  { color: var(--ds-danger); }

@media (max-width: 1280px) {
  .td-kpi-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .td-kpi-card:nth-child(2) { border-right: none; }
  .td-kpi-card:nth-child(3) { border-top: 1px solid var(--ds-gray-200); }
  .td-kpi-card:nth-child(4) { border-top: 1px solid var(--ds-gray-200); border-right: none; }
}

@media (max-width: 480px) {
  .td-kpi-grid { grid-template-columns: 1fr; }
  .td-kpi-card { border-right: none; border-bottom: 1px solid var(--ds-gray-200); }
  .td-kpi-card:last-child { border-bottom: none; }
}

@media (min-width: 1600px) {
  .td-kpi-card__value { font-size: 3rem; }
  .td-kpi-card { padding: 1.25rem; }
}

@media (prefers-reduced-motion: reduce) {
  .td-kpi-card { animation: none; }
  .td-kpi-card__live-ring { animation: none; }
  .td-kpi-card:hover .td-kpi-card__icon-wrap { transform: none; }
}
</style>
