<template>
  <div class="td-hero">
    <div class="td-hero__main">
      <div class="td-hero__eyebrow">
        <span class="td-hero__eyebrow-dot" />
        <span>{{ timeOfDayLabel }}</span>
        <span class="td-hero__eyebrow-date">{{ todayLabel }}</span>
      </div>
      <h1 class="td-hero__title">
        Chào buổi {{ timeOfDayLabel }}, <span class="td-hero__name">{{ teacherName }}</span>
      </h1>
      <p class="td-hero__subtitle">{{ heroSubtitle }}</p>
    </div>

    <div class="td-hero__stats">
      <div class="td-hero__stat" :class="{ 'td-hero__stat--active': liveExamCount > 0 }">
        <div class="td-hero__stat-icon-wrap">
          <LucideIcon name="timer" />
        </div>
        <div class="td-hero__stat-body">
          <span class="td-hero__stat-value">{{ liveExamCount }}</span>
          <span class="td-hero__stat-label">Kỳ thi đang diễn ra</span>
        </div>
      </div>

      <div class="td-hero__stat-divider" />

      <div class="td-hero__stat">
        <div class="td-hero__stat-icon-wrap">
          <LucideIcon name="group" />
        </div>
        <div class="td-hero__stat-body">
          <span class="td-hero__stat-value">{{ activeStudentCount }}</span>
          <span class="td-hero__stat-label">Học sinh đang hoạt động</span>
        </div>
      </div>

      <div class="td-hero__stat-divider" />

      <div class="td-hero__stat" :class="{ 'td-hero__stat--alert': alertCount > 0 }">
        <div class="td-hero__stat-icon-wrap">
          <LucideIcon name="warning" />
        </div>
        <div class="td-hero__stat-body">
          <span class="td-hero__stat-value">{{ alertCount }}</span>
          <span class="td-hero__stat-label">Cảnh báo cần xử lý</span>
        </div>
      </div>
    </div>

    <div class="td-hero__actions">
      <button
        v-if="liveExamCount > 0"
        type="button"
        class="td-hero__cta td-hero__cta--live"
        @click="$emit('go-monitoring')"
      >
        <div class="td-hero__cta-icon-wrap">
          <LucideIcon name="live_tv" />
        </div>
        <span class="td-hero__cta-text">
          <span class="td-hero__cta-label">Vào phòng giám sát</span>
          <span class="td-hero__cta-sub">{{ liveExamCount }} kỳ thi đang diễn ra</span>
        </span>
        <span class="td-hero__cta-live-dot" />
      </button>

      <button type="button" class="td-hero__cta td-hero__cta--primary" @click="$emit('create-exam')">
        <div class="td-hero__cta-icon-wrap">
          <LucideIcon name="add_circle" />
        </div>
        <span class="td-hero__cta-text">
          <span class="td-hero__cta-label">Tạo đề thi mới</span>
          <span class="td-hero__cta-sub">Bắt đầu lên lịch thi</span>
        </span>
      </button>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  teacherName: { type: String, default: 'Giáo viên' },
  liveExamCount: { type: Number, default: 0 },
  activeStudentCount: { type: Number, default: 0 },
  alertCount: { type: Number, default: 0 }
})

defineEmits(['go-monitoring', 'create-exam'])

const timeOfDayLabel = computed(() => {
  const h = new Date().getHours()
  if (h < 12) return 'sáng'
  if (h < 18) return 'chiều'
  return 'tối'
})

const todayLabel = computed(() =>
  new Date().toLocaleDateString('vi-VN', { weekday: 'long', day: '2-digit', month: '2-digit' })
)

const heroSubtitle = computed(() => {
  if (props.liveExamCount > 0) {
    return `Bạn có ${props.liveExamCount} kỳ thi đang diễn ra. Hãy kiểm tra trạng thái ngay.`
  }
  if (props.alertCount > 0) {
    return `Có ${props.alertCount} cảnh báo cần bạn xử lý.`
  }
  return 'Một ngày làm việc hiệu quả. Không có kỳ thi nào đang diễn ra.'
})
</script>


<style scoped>
/* Ultra Simplified Hero */

.td-hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 2rem;
  padding: 1.75rem 2rem;
  background: linear-gradient(135deg, #ffffff 0%, #f5f4ff 60%, #ede9fe 100%);
  border: 1px solid var(--ds-border);
  border-radius: var(--ds-radius-2xl);
  position: relative;
  overflow: hidden;
  animation: fadeUp 0.5s cubic-bezier(0.34, 1.56, 0.64, 1);
  transform: translateZ(0);
  will-change: transform, opacity;
  backface-visibility: hidden;
}

@keyframes fadeUp {
  from { opacity: 0; transform: translateY(14px) translateZ(0); }
  to   { opacity: 1; transform: translateY(0) translateZ(0); }
}

.dark .td-hero {
  background: linear-gradient(135deg, #1e293b 0%, #1e1b4b 60%, #1e1b4b 100%);
  border-color: rgba(79, 70, 229, 0.2);
}

.td-hero__main {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
  flex: 1;
  min-width: 0;
}

.td-hero__eyebrow {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.75rem;
  font-weight: 700;
  color: var(--ds-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.1em;
}

.td-hero__eyebrow-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--ds-primary);
  animation: dotPulse 2s ease-in-out infinite;
  transform: translateZ(0);
}

@keyframes dotPulse {
  0%, 100% { opacity: 1; transform: scale(1) translateZ(0); }
  50% { opacity: 0.4; transform: scale(0.9) translateZ(0); }
}

.td-hero__eyebrow-date {
  color: var(--ds-text-secondary);
  text-transform: none;
  letter-spacing: 0;
  font-weight: 500;
}

.td-hero__title {
  font-family: var(--ds-font-display);
  font-size: clamp(1.25rem, 2.5vw, 1.75rem);
  font-weight: 800;
  color: var(--ds-text);
  letter-spacing: -0.02em;
  line-height: 1.2;
  margin: 0;
}

.dark .td-hero__title { color: var(--ds-gray-100, #f1f5f9); }
.td-hero__name { color: var(--ds-primary); }
.dark .td-hero__name { opacity: 0.85; }

.td-hero__subtitle {
  font-size: 0.875rem;
  color: var(--ds-text-secondary);
  margin: 0;
  line-height: 1.5;
  max-width: 480px;
}

@media (min-width: 1400px) { .td-hero__subtitle { max-width: 600px; } }
@media (min-width: 1600px) { .td-hero__subtitle { max-width: 750px; } }
@media (min-width: 1920px) { .td-hero__subtitle { max-width: 850px; } }

.td-hero__stats {
  display: flex;
  align-items: center;
  gap: 0;
  padding: 0.875rem 1.25rem;
  background: rgba(255, 255, 255, 0.8);
  border: 1px solid var(--ds-border);
  border-radius: var(--ds-radius-xl);
  backdrop-filter: blur(4px);
  width: fit-content;
}

.dark .td-hero__stats { background: rgba(30, 41, 59, 0.8); }

.td-hero__stat {
  display: flex;
  align-items: center;
  gap: 0.625rem;
  padding: 0 1rem;
}

.td-hero__stat:first-child { padding-left: 0; }
.td-hero__stat:last-child { padding-right: 0; }

.td-hero__stat-divider {
  width: 1px;
  height: 28px;
  background: var(--ds-border);
}

.td-hero__stat-icon-wrap {
  width: 40px;
  height: 40px;
  border-radius: var(--ds-radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--ds-gray-100);
  color: var(--ds-text-muted);
  flex-shrink: 0;
  font-size: 1.125rem;
  transition: background 0.15s ease, color 0.15s ease;
}

.dark .td-hero__stat-icon-wrap { background: var(--ds-gray-700); }

.td-hero__stat--active .td-hero__stat-icon-wrap {
  background: var(--ds-success-soft);
  color: var(--ds-success);
}

.td-hero__stat--alert .td-hero__stat-icon-wrap {
  background: var(--ds-danger-soft);
  color: var(--ds-danger);
}

.td-hero__stat-body {
  display: flex;
  flex-direction: column;
  gap: 0;
}

.td-hero__stat-value {
  font-size: 1.25rem;
  font-weight: 800;
  color: var(--ds-text);
  line-height: 1.1;
  font-variant-numeric: tabular-nums;
}

.dark .td-hero__stat-value { color: var(--ds-gray-100, #f1f5f9); }
.td-hero__stat--alert .td-hero__stat-value { color: var(--ds-danger); }

.td-hero__stat-label {
  font-size: 0.65rem;
  font-weight: 600;
  color: var(--ds-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.08em;
  white-space: nowrap;
}

.td-hero__actions {
  display: flex;
  flex-direction: column;
  gap: 0.625rem;
  flex-shrink: 0;
}

.td-hero__cta {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.875rem 1.25rem;
  border-radius: var(--ds-radius-xl);
  border: 1px solid transparent;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
  text-align: left;
  min-width: 220px;
}

.td-hero__cta--live {
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
  color: white;
  box-shadow: 0 2px 8px rgba(22, 163, 74, 0.2);
}

.td-hero__cta--live:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(22, 163, 74, 0.35);
}

.td-hero__cta--primary {
  background: linear-gradient(135deg, #6366f1 0%, #4f46e5 100%);
  color: white;
  box-shadow: 0 2px 8px rgba(79, 70, 229, 0.2);
}

.td-hero__cta--primary:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(79, 70, 229, 0.3);
}

.td-hero__cta:active { transform: translateY(0) scale(0.98); }

.td-hero__cta-icon-wrap {
  width: 40px;
  height: 40px;
  border-radius: var(--ds-radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.2);
  color: white;
  flex-shrink: 0;
  font-size: 1.25rem;
  transition: transform 0.2s ease;
}

.td-hero__cta:hover .td-hero__cta-icon-wrap {
  transform: scale(1.1);
}

.td-hero__cta-live-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: white;
  animation: dotPulse 1.5s ease-in-out infinite;
  flex-shrink: 0;
  margin-left: auto;
}

.td-hero__cta-text {
  display: flex;
  flex-direction: column;
  gap: 0.125rem;
  flex: 1;
}

.td-hero__cta-label {
  font-size: 0.875rem;
  font-weight: 700;
  line-height: 1.2;
}

.td-hero__cta-sub {
  font-size: 0.7rem;
  opacity: 0.8;
  font-weight: 500;
}

@media (max-width: 900px) {
  .td-hero {
    flex-direction: column;
    align-items: flex-start;
    padding: 1.5rem;
  }
  .td-hero__actions { flex-direction: row; width: 100%; }
  .td-hero__cta { flex: 1; min-width: 0; }
}

@media (max-width: 600px) {
  .td-hero__stats { flex-direction: column; width: 100%; align-items: flex-start; gap: 0.75rem; }
  .td-hero__stat-divider { width: 100%; height: 1px; }
  .td-hero__stat { padding: 0; }
  .td-hero__actions { flex-direction: column; }
}

@media (prefers-reduced-motion: reduce) {
  .td-hero, .td-hero__eyebrow-dot, .td-hero__cta-live-dot { animation: none; }
  .td-hero:hover .td-hero__cta { transform: none; }
}
</style>
