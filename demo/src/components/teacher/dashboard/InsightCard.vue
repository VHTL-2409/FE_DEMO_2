<template>
  <div class="insight-card">
    <div class="insight-card__header">
      <h3 class="insight-card__title">Tổng quan kỳ thi</h3>
      <span class="insight-card__badge">{{ exams.length }} kỳ thi</span>
    </div>

    <div class="insight-card__stats">
      <div class="insight-stat">
        <span class="insight-stat__value">{{ stats.active || 0 }}</span>
        <span class="insight-stat__label">Đang diễn ra</span>
      </div>
      <div class="insight-stat">
        <span class="insight-stat__value">{{ stats.upcoming || 0 }}</span>
        <span class="insight-stat__label">Sắp tới</span>
      </div>
      <div class="insight-stat">
        <span class="insight-stat__value">{{ stats.ended || 0 }}</span>
        <span class="insight-stat__label">Đã kết thúc</span>
      </div>
      <div class="insight-stat">
        <span class="insight-stat__value">{{ stats.draft || 0 }}</span>
        <span class="insight-stat__label">Bản nháp</span>
      </div>
    </div>

    <div v-if="exams.length > 0" class="insight-card__list">
      <div
        v-for="exam in exams.slice(0, 5)"
        :key="exam.id"
        class="insight-item"
      >
        <div class="insight-item__info">
          <span class="insight-item__name">{{ exam.title || exam.examTitle || 'Kỳ thi không tên' }}</span>
          <span class="insight-item__meta">{{ exam.questionCount || 0 }} câu · {{ exam.duration || 0 }} phút</span>
        </div>
        <span
          class="insight-item__status"
          :class="`insight-item__status--${getStatusKey(exam)}`"
        >
          {{ getStatusLabel(exam) }}
        </span>
      </div>
    </div>

    <div v-else class="insight-card__empty">
      <span>Chưa có kỳ thi nào</span>
    </div>
  </div>
</template>

<script setup>
const props = defineProps({
  exams: {
    type: Array,
    default: () => []
  },
  stats: {
    type: Object,
    default: () => ({})
  }
})

const getStatusKey = (exam) => {
  if (!exam.isActive) return 'draft'
  const nowMs = Date.now()
  const startMs = new Date(exam.startTime || '').getTime()
  const endMs = new Date(exam.endTime || '').getTime()
  if (!Number.isNaN(startMs) && nowMs < startMs) return 'upcoming'
  if (!Number.isNaN(startMs) && !Number.isNaN(endMs) && nowMs >= startMs && nowMs <= endMs) return 'started'
  return 'ended'
}

const getStatusLabel = (exam) => {
  const map = {
    draft: 'Nháp',
    upcoming: 'Sắp tới',
    started: 'Đang thi',
    ended: 'Đã kết thúc'
  }
  return map[getStatusKey(exam)] || '—'
}
</script>

<style scoped>
.insight-card {
  background: #1a1a2e;
  border: 1px solid rgba(255,255,255,0.08);
  border-radius: 12px;
  padding: 20px;
}

.insight-card__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.insight-card__title {
  font-size: 16px;
  font-weight: 600;
  color: #fff;
  margin: 0;
}

.insight-card__badge {
  background: rgba(99, 102, 241, 0.2);
  color: #818cf8;
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 999px;
}

.insight-card__stats {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  margin-bottom: 16px;
}

.insight-stat {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 12px 8px;
  background: rgba(255,255,255,0.04);
  border-radius: 8px;
}

.insight-stat__value {
  font-size: 24px;
  font-weight: 700;
  color: #fff;
}

.insight-stat__label {
  font-size: 11px;
  color: rgba(255,255,255,0.5);
  margin-top: 4px;
  text-align: center;
}

.insight-card__list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.insight-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 12px;
  background: rgba(255,255,255,0.03);
  border-radius: 8px;
}

.insight-item__info {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
  flex: 1;
}

.insight-item__name {
  font-size: 13px;
  font-weight: 500;
  color: #fff;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.insight-item__meta {
  font-size: 11px;
  color: rgba(255,255,255,0.4);
}

.insight-item__status {
  font-size: 11px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 999px;
  flex-shrink: 0;
  margin-left: 8px;
}

.insight-item__status--draft { background: rgba(107,114,128,0.3); color: #9ca3af; }
.insight-item__status--upcoming { background: rgba(59,130,246,0.2); color: #60a5fa; }
.insight-item__status--started { background: rgba(16,185,129,0.2); color: #34d399; }
.insight-item__status--ended { background: rgba(107,114,128,0.2); color: #9ca3af; }

.insight-card__empty {
  text-align: center;
  padding: 24px;
  color: rgba(255,255,255,0.4);
  font-size: 13px;
}
</style>
