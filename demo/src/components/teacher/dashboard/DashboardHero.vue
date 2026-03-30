<template>
  <div class="td-hero ds-animate-fade-up">
    <!-- Main hero card -->
    <div class="td-hero__main">
      <div class="td-hero__left">
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

      <!-- Quick stats strip -->
      <div class="td-hero__stats">
        <div class="td-hero__stat" :class="{ 'td-hero__stat--active': liveExamCount > 0 }">
          <LucideIcon name="timer" td-hero__stat-icon />
          <div class="td-hero__stat-body">
            <span class="td-hero__stat-value">{{ liveExamCount }}</span>
            <span class="td-hero__stat-label">Kỳ thi đang diễn ra</span>
          </div>
        </div>

        <div class="td-hero__stat-divider" />

        <div class="td-hero__stat">
          <LucideIcon name="group" td-hero__stat-icon />
          <div class="td-hero__stat-body">
            <span class="td-hero__stat-value">{{ activeStudentCount }}</span>
            <span class="td-hero__stat-label">Học sinh đang hoạt động</span>
          </div>
        </div>

        <div class="td-hero__stat-divider" />

        <div class="td-hero__stat" :class="{ 'td-hero__stat--alert': alertCount > 0 }">
          <LucideIcon name="warning" td-hero__stat-icon />
          <div class="td-hero__stat-body">
            <span class="td-hero__stat-value">{{ alertCount }}</span>
            <span class="td-hero__stat-label">Cảnh báo cần xử lý</span>
          </div>
        </div>
      </div>
    </div>

    <!-- CTA buttons -->
    <div class="td-hero__actions">
      <button
        v-if="liveExamCount > 0"
        type="button"
        class="td-hero__cta td-hero__cta--live"
        @click="$emit('go-monitoring')"
      >
        <LucideIcon name="live_tv" td-hero__cta-icon />
        <span class="td-hero__cta-text">
          <span class="td-hero__cta-label">Vào phòng giám sát</span>
          <span class="td-hero__cta-sub">{{ liveExamCount }} kỳ thi đang diễn ra</span>
        </span>
        <span class="td-hero__cta-live-dot" />
      </button>

      <button type="button" class="td-hero__cta td-hero__cta--primary" @click="$emit('create-exam')">
        <LucideIcon name="add_circle" td-hero__cta-icon />
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
.td-hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 2rem;
  padding: 1.75rem 2rem;
  background: linear-gradient(135deg, #ffffff 0%, #f5f4ff 60%, #ede9fe 100%);
  border: 1px solid var(--ds-border);
  border-radius: var(--ds-radius-2xl);
  box-shadow: var(--ds-shadow-sm);
  position: relative;
  overflow: hidden;
}

.dark .td-hero {
  background: linear-gradient(135deg, #1e293b 0%, #1e1b4b 60%, #1e1b4b 100%);
  border-color: rgba(79, 70, 229, 0.2);
}

/* Decorative background element */
.td-hero::before {
  content: '';
  position: absolute;
  top: -40px;
  right: -40px;
  width: 200px;
  height: 200px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(79, 70, 229, 0.06) 0%, transparent 70%);
  pointer-events: none;
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
  animation: td-hero-pulse 2s ease-in-out infinite;
}

@keyframes td-hero-pulse {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.6; transform: scale(0.85); }
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

.dark .td-hero__title {
  color: var(--ds-gray-100, #f1f5f9);
}

.td-hero__name {
  color: var(--ds-primary);
}

.dark .td-hero__name {
  color: var(--ds-primary);
  opacity: 0.85;
}

.td-hero__subtitle {
  font-size: 0.875rem;
  color: var(--ds-text-secondary);
  margin: 0;
  line-height: 1.5;
  max-width: 480px;
}

/* Quick stats strip */
.td-hero__stats {
  display: flex;
  align-items: center;
  gap: 0;
  padding: 0.875rem 1.25rem;
  background: rgba(255, 255, 255, 0.8);
  border: 1px solid var(--ds-border);
  border-radius: var(--ds-radius-xl);
  backdrop-filter: blur(8px);
  box-shadow: var(--ds-shadow-xs);
  width: fit-content;
}

.dark .td-hero__stats {
  background: rgba(30, 41, 59, 0.8);
}

.td-hero__stat {
  display: flex;
  align-items: center;
  gap: 0.625rem;
  padding: 0 1rem;
}

.td-hero__stat:first-child {
  padding-left: 0;
}

.td-hero__stat:last-child {
  padding-right: 0;
}

.td-hero__stat-divider {
  width: 1px;
  height: 28px;
  background: var(--ds-border);
}

.td-hero__stat-icon {
  font-size: 1.25rem;
  color: var(--ds-text-muted);
  flex-shrink: 0;
}

.td-hero__stat--active .td-hero__stat-icon {
  color: var(--ds-success);
}

.td-hero__stat--alert .td-hero__stat-icon {
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

.dark .td-hero__stat-value {
  color: var(--ds-gray-100, #f1f5f9);
}

.td-hero__stat--alert .td-hero__stat-value {
  color: var(--ds-danger);
}

.td-hero__stat-label {
  font-size: 0.65rem;
  font-weight: 600;
  color: var(--ds-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.08em;
  white-space: nowrap;
}

/* CTA Actions */
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
  transition: all 0.2s ease;
  position: relative;
  overflow: hidden;
  text-align: left;
  min-width: 220px;
}

.td-hero__cta--live {
  background: linear-gradient(135deg, var(--ds-success) 0%, #059669 100%);
  color: white;
  box-shadow: 0 4px 16px rgba(22, 163, 74, 0.3);
}

.td-hero__cta--live:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(22, 163, 74, 0.4);
}

.td-hero__cta--primary {
  background: linear-gradient(135deg, var(--ds-primary) 0%, #6366f1 100%);
  color: white;
  box-shadow: 0 4px 16px rgba(79, 70, 229, 0.25);
}

.td-hero__cta--primary:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(79, 70, 229, 0.35);
}

.td-hero__cta:active {
  transform: translateY(0);
}

.td-hero__cta-icon {
  font-size: 1.5rem;
  flex-shrink: 0;
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

.td-hero__cta-live-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: white;
  animation: td-hero-pulse 1.5s ease-in-out infinite;
  flex-shrink: 0;
  margin-left: auto;
}

/* Responsive */
@media (max-width: 900px) {
  .td-hero {
    flex-direction: column;
    align-items: flex-start;
    padding: 1.5rem;
  }

  .td-hero__actions {
    flex-direction: row;
    width: 100%;
  }

  .td-hero__cta {
    flex: 1;
    min-width: 0;
  }
}

@media (max-width: 600px) {
  .td-hero__stats {
    flex-direction: column;
    width: 100%;
    align-items: flex-start;
    gap: 0.75rem;
  }

  .td-hero__stat-divider {
    width: 100%;
    height: 1px;
  }

  .td-hero__stat {
    padding: 0;
  }

  .td-hero__actions {
    flex-direction: column;
  }
}
</style>
