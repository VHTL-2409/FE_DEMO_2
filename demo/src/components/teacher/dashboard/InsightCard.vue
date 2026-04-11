<template>
  <div class="td-insight">
    <!-- Header -->
    <div class="td-insight__header">
      <div>
        <h3 class="td-insight__title">Thông tin chi tiết</h3>
        <p class="td-insight__sub">Tổng quan &amp; gợi ý hành động</p>
      </div>
    </div>

    <!-- Completion Rate -->
    <div class="td-insight__metric">
      <div class="td-insight__metric-header">
        <span class="td-insight__metric-label">Tỷ lệ hoàn thành</span>
        <span class="td-insight__metric-value">{{ completionRate }}%</span>
      </div>
      <div class="td-insight__progress-track">
        <div
          class="td-insight__progress-bar"
          :style="{ width: completionRate + '%', background: progressColor }"
        />
      </div>
    </div>

    <!-- Mini breakdown -->
    <div class="td-insight__breakdown">
      <div class="td-insight__breakdown-item">
        <span class="td-insight__breakdown-dot" style="background: var(--ds-success)" />
        <span class="td-insight__breakdown-label">Đã kết thúc</span>
        <span class="td-insight__breakdown-val">{{ stats.ended }}</span>
      </div>
      <div class="td-insight__breakdown-item">
        <span class="td-insight__breakdown-dot" style="background: var(--ds-accent)" />
        <span class="td-insight__breakdown-label">Sắp tới</span>
        <span class="td-insight__breakdown-val">{{ stats.upcoming }}</span>
      </div>
      <div class="td-insight__breakdown-item">
        <span class="td-insight__breakdown-dot" style="background: var(--ds-info)" />
        <span class="td-insight__breakdown-label">Bản nháp</span>
        <span class="td-insight__breakdown-val">{{ stats.draft }}</span>
      </div>
    </div>

    <!-- Insight tip -->
    <div class="td-insight__tip">
      <div class="td-insight__tip-icon">
        <LucideIcon name="lightbulb" />
      </div>
      <p class="td-insight__tip-text">{{ insightTip }}</p>
    </div>

    <!-- Upcoming exams -->
    <div v-if="upcomingExams.length > 0" class="td-insight__upcoming">
      <div class="td-insight__upcoming-header">
        <LucideIcon name="event" />
        <span>Sắp diễn ra</span>
      </div>
      <div class="td-insight__upcoming-list">
        <div v-for="exam in upcomingExams" :key="exam.id" class="td-insight__upcoming-item">
          <div class="td-insight__upcoming-info">
            <span class="td-insight__upcoming-title">{{ exam.title }}</span>
            <span class="td-insight__upcoming-date">{{ exam.dateLabel }}</span>
          </div>
          <span class="td-insight__upcoming-badge">{{ exam.relativeTime }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  exams: {
    type: Array,
    default: () => []
  },
  stats: {
    type: Object,
    default: () => ({
      total: 0,
      ended: 0,
      upcoming: 0,
      draft: 0
    })
  }
})

const completionRate = computed(() => {
  if (!props.stats.total) return 0
  return Math.round((props.stats.ended / props.stats.total) * 100)
})

const progressColor = computed(() => {
  const r = completionRate.value
  if (r >= 70) return 'var(--ds-success)'
  if (r >= 40) return 'var(--ds-accent)'
  return 'var(--ds-danger)'
})

const insightTip = computed(() => {
  const ended = props.stats.ended
  const upcoming = props.stats.upcoming
  const active = props.exams.filter(e => e.statusKey === 'started').length

  if (active > 0) {
    return `Bạn đang có ${active} kỳ thi đang diễn ra. Vào phòng giám sát để theo dõi ngay.`
  }
  if (upcoming > 0) {
    return `Có ${upcoming} kỳ thi sắp tới. Kiểm tra lại cấu hình proctor và gửi thông báo cho học sinh.`
  }
  if (ended > 0) {
    return `${ended} kỳ thi đã hoàn thành. Đừng quên xem báo cáo chi tiết để cải thiện chất lượng đề thi.`
  }
  return 'Chưa có kỳ thi nào. Bắt đầu tạo đề thi đầu tiên để theo dõi tiến độ.'
})

const upcomingExams = computed(() => {
  return props.exams
    .filter(e => e.statusKey === 'upcoming')
    .slice(0, 3)
    .map(exam => {
      const start = new Date(exam.startTime || '')
      const days = Math.ceil((start.getTime() - Date.now()) / (1000 * 60 * 60 * 24))
      return {
        id: exam.id,
        title: exam.title,
        dateLabel: start.toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit' }),
        relativeTime: days === 1 ? 'Ngày mai' : `${days} ngày`
      }
    })
})
</script>


<style scoped>
/* InsightCard — uses .animate-fade-up from animation.css (canonical)
   delay 0.5s is preserved via inline style for staggered entry */
.td-insight {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
  animation: fadeUpSm 0.45s cubic-bezier(0.34, 1.2, 0.64, 1) 0.5s both;
}

/* Header */
.td-insight__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
}

.td-insight__title {
  font-family: var(--ds-font-display);
  font-size: 1rem;
  font-weight: 700;
  color: var(--ds-text);
  margin: 0;
}

.dark .td-insight__title {
  color: #f1f5f9;
}

.td-insight__sub {
  font-size: 0.75rem;
  color: var(--ds-text-muted);
  margin: 0.25rem 0 0;
}

/* Metric */
.td-insight__metric {
  display: flex;
  flex-direction: column;
  gap: 0.625rem;
}

.td-insight__metric-header {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
}

.td-insight__metric-label {
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--ds-text-secondary);
}

.td-insight__metric-value {
  font-family: var(--ds-font-display);
  font-size: 1.75rem;
  font-weight: 800;
  color: var(--ds-text);
  line-height: 1;
  letter-spacing: -0.03em;
}

.dark .td-insight__metric-value {
  color: #f1f5f9;
}

.td-insight__progress-track {
  height: 8px;
  background: var(--ds-gray-200);
  border-radius: var(--ds-radius-full);
  overflow: hidden;
}

.dark .td-insight__progress-track {
  background: var(--ds-gray-700);
}

.td-insight__progress-bar {
  height: 100%;
  border-radius: var(--ds-radius-full);
  transition: width 0.6s ease;
}

/* Breakdown */
.td-insight__breakdown {
  display: flex;
  flex-direction: column;
  gap: 0.375rem;
  padding: 0.875rem;
  background: var(--ds-gray-50);
  border-radius: var(--ds-radius-xl);
  border: 1px solid var(--ds-border);
}

.dark .td-insight__breakdown {
  background: var(--ds-gray-800);
}

.td-insight__breakdown-item {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.8rem;
}

.td-insight__breakdown-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}

.td-insight__breakdown-label {
  flex: 1;
  color: var(--ds-text-secondary);
  font-weight: 500;
}

.dark .td-insight__breakdown-label {
  color: #94a3b8;
}

.td-insight__breakdown-val {
  font-weight: 700;
  color: var(--ds-text);
  font-variant-numeric: tabular-nums;
}

.dark .td-insight__breakdown-val {
  color: #f1f5f9;
}

/* Tip */
.td-insight__tip {
  display: flex;
  gap: 0.75rem;
  padding: 0.875rem;
  background: linear-gradient(135deg, var(--ds-primary-soft) 0%, rgba(99, 102, 241, 0.04) 100%);
  border-radius: var(--ds-radius-xl);
  border: 1px solid var(--ds-primary-border);
}

.td-insight__tip-icon {
  width: 32px;
  height: 32px;
  border-radius: var(--ds-radius-md);
  background: var(--ds-primary);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}


.td-insight__tip-text {
  font-size: 0.8rem;
  color: var(--ds-text-secondary);
  line-height: 1.5;
  margin: 0;
  padding-top: 0.25rem;
}

.dark .td-insight__tip-text {
  color: #c7d2fe;
}

/* Upcoming */
.td-insight__upcoming {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.td-insight__upcoming-header {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  font-size: 0.75rem;
  font-weight: 700;
  color: var(--ds-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.08em;
}


.td-insight__upcoming-list {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.td-insight__upcoming-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  padding: 0.625rem 0.75rem;
  background: var(--ds-surface);
  border: 1px solid var(--ds-border);
  border-radius: var(--ds-radius-lg);
  transition: color 0.12s ease, background-color 0.12s ease, border-color 0.12s ease, box-shadow 0.12s ease, transform 0.12s ease;
}

.td-insight__upcoming-item:hover {
  background: var(--ds-gray-50);
  border-color: var(--ds-primary-border);
  transform: translateX(2px);
  box-shadow: 0 2px 8px rgba(79, 70, 229, 0.08);
  transition: background 0.15s ease, border-color 0.15s ease, transform 0.2s cubic-bezier(0.34, 1.2, 0.64, 1), box-shadow 0.2s ease;
}

.dark .td-insight__upcoming-item:hover {
  background: var(--ds-gray-800);
}

.td-insight__upcoming-info {
  display: flex;
  flex-direction: column;
  gap: 0.125rem;
  min-width: 0;
}

.td-insight__upcoming-title {
  font-size: 0.8rem;
  font-weight: 600;
  color: var(--ds-text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.dark .td-insight__upcoming-title {
  color: #f1f5f9;
}

.td-insight__upcoming-date {
  font-size: 0.7rem;
  color: var(--ds-text-muted);
}

.td-insight__upcoming-badge {
  padding: 0.2rem 0.5rem;
  background: var(--ds-info-soft);
  color: var(--ds-info);
  border-radius: var(--ds-radius-full);
  font-size: 0.65rem;
  font-weight: 700;
  white-space: nowrap;
  flex-shrink: 0;
}
</style>
@media (prefers-reduced-motion: reduce) {
  .td-insight { animation: none; }
  .td-insight__upcoming-item:hover { transform: none; box-shadow: none; }
}