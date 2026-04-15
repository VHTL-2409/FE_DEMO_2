<template>
  <div class="td-hero" :class="{ 'td-hero--embedded': embedded }">
    <div class="td-hero__main">
      <div class="td-hero__eyebrow">
        <span>{{ todayLabel }}</span>
      </div>
      <h1 class="td-hero__title">
        {{ copy.greetingPrefix }}{{ timeOfDayLabel }}, <span class="td-hero__name">{{ teacherName }}</span>
      </h1>
      <p class="td-hero__subtitle">{{ heroSubtitle }}</p>
    </div>

    <div class="td-hero__stats" role="group" :aria-label="copy.statGroup">
      <div class="td-hero__stat" :class="{ 'td-hero__stat--active': liveExamCount > 0 }">
        <div class="td-hero__stat-icon-wrap" aria-hidden="true">
          <LucideIcon name="timer" :size="20" />
        </div>
        <div class="td-hero__stat-body">
          <span class="td-hero__stat-value">{{ liveExamCount }}</span>
          <span class="td-hero__stat-label">{{ copy.statLive }}</span>
        </div>
      </div>
      <div class="td-hero__stat">
        <div class="td-hero__stat-icon-wrap" aria-hidden="true">
          <LucideIcon name="group" :size="20" />
        </div>
        <div class="td-hero__stat-body">
          <span class="td-hero__stat-value">{{ activeStudentCount }}</span>
          <span class="td-hero__stat-label">{{ copy.statStudents }}</span>
        </div>
      </div>
      <div class="td-hero__stat" :class="{ 'td-hero__stat--alert': alertCount > 0 }">
        <div class="td-hero__stat-icon-wrap" aria-hidden="true">
          <LucideIcon name="warning" :size="20" />
        </div>
        <div class="td-hero__stat-body">
          <span class="td-hero__stat-value">{{ alertCount }}</span>
          <span class="td-hero__stat-label">{{ copy.statAlerts }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import LucideIcon from '../../common/LucideIcon.vue'

const copy = {
  greetingPrefix: 'Ch\u00e0o bu\u1ed5i ',
  statGroup: 'T\u00f3m t\u1eaft nhanh',
  statLive: '\u0110ang di\u1ec5n ra',
  statStudents: 'HS ho\u1ea1t \u0111\u1ed9ng',
  statAlerts: 'C\u1ea3nh b\u00e1o',
  subLive: (n) =>
    `${n} k\u1ef3 thi \u0111ang di\u1ec5n ra. V\u00e0o m\u1ee5c Gi\u00e1m s\u00e1t tr\u00ean menu ho\u1eb7c \u00f4 phi\u00ean live b\u00ean d\u01b0\u1edbi.`,
  subAlerts: (n) => `C\u00f3 ${n} c\u1ea3nh b\u00e1o c\u1ea7n x\u1eed l\u00fd.`,
  subIdle:
    'Kh\u00f4ng c\u00f3 k\u1ef3 thi \u0111ang di\u1ec5n ra. D\u00f9ng m\u1ee5c \u0110\u1ec1 thi ho\u1eb7c T\u1ea1o v\u00e0 xu\u1ea5t b\u1ea3n tr\u00ean menu \u0111\u1ec3 qu\u1ea3n l\u00fd \u0111\u1ec1.'
}

const props = defineProps({
  teacherName: { type: String, default: 'Giáo viên' },
  liveExamCount: { type: Number, default: 0 },
  activeStudentCount: { type: Number, default: 0 },
  alertCount: { type: Number, default: 0 },
  /** Gộp trong shell chung — không viền/nền riêng */
  embedded: { type: Boolean, default: false }
})

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
    return copy.subLive(props.liveExamCount)
  }
  if (props.alertCount > 0) {
    return copy.subAlerts(props.alertCount)
  }
  return copy.subIdle
})
</script>

<style scoped>
.td-hero {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1.25rem 1.75rem;
  padding: 1.25rem 1.5rem;
  background: var(--ds-surface);
  border: 1px solid var(--ds-border);
  border-radius: var(--ds-radius-2xl);
}

.dark .td-hero {
  background: var(--ds-gray-900);
  border-color: var(--ds-border);
}

.td-hero--embedded {
  padding: 0;
  background: transparent;
  border: none;
  border-radius: 0;
}

.dark .td-hero--embedded {
  background: transparent;
  border: none;
}

.td-hero__main {
  flex: 1;
  min-width: min(100%, 280px);
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.td-hero__eyebrow {
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--ds-text-muted);
  text-transform: capitalize;
}

.td-hero__title {
  font-family: var(--ds-font-display);
  font-size: clamp(1.125rem, 2.2vw, 1.5rem);
  font-weight: 800;
  color: var(--ds-text);
  letter-spacing: -0.02em;
  line-height: 1.25;
  margin: 0;
}

.dark .td-hero__title {
  color: var(--ds-gray-100, #f1f5f9);
}

.td-hero__name {
  color: var(--ds-primary);
}

.td-hero__subtitle {
  font-size: 0.8125rem;
  color: var(--ds-text-secondary);
  margin: 0;
  line-height: 1.45;
  max-width: 36rem;
}

.td-hero__stats {
  display: flex;
  align-items: stretch;
  gap: 0.5rem;
  flex-shrink: 0;
}

.td-hero__stat {
  display: flex;
  flex-direction: row;
  align-items: center;
  gap: 0.625rem;
  min-width: 5.5rem;
  padding: 0.5rem 0.75rem;
  border-radius: var(--ds-radius-lg);
  background: var(--ds-gray-50);
  border: 1px solid var(--ds-border);
}

.td-hero__stat-icon-wrap {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 2.25rem;
  height: 2.25rem;
  border-radius: var(--ds-radius-md);
  background: var(--ds-gray-100);
  color: var(--ds-text-muted);
  flex-shrink: 0;
}

.dark .td-hero__stat-icon-wrap {
  background: var(--ds-gray-700);
}

.td-hero__stat--active .td-hero__stat-icon-wrap {
  background: color-mix(in srgb, var(--ds-success) 18%, transparent);
  color: var(--ds-success);
}

.td-hero__stat--alert .td-hero__stat-icon-wrap {
  background: var(--ds-danger-soft);
  color: var(--ds-danger);
}

.td-hero__stat-body {
  display: flex;
  flex-direction: column;
  gap: 0.125rem;
  min-width: 0;
}

.dark .td-hero__stat {
  background: var(--ds-gray-800);
}

.td-hero__stat--active {
  border-color: var(--ds-success-border, var(--ds-success));
  background: var(--ds-success-soft);
}

.td-hero__stat--alert .td-hero__stat-value {
  color: var(--ds-danger);
}

.td-hero__stat-value {
  font-size: 1.125rem;
  font-weight: 800;
  font-variant-numeric: tabular-nums;
  color: var(--ds-text);
  line-height: 1.1;
}

.dark .td-hero__stat-value {
  color: var(--ds-gray-100, #f1f5f9);
}

.td-hero__stat-label {
  font-size: 0.65rem;
  font-weight: 600;
  color: var(--ds-text-muted);
  line-height: 1.2;
}

@media (max-width: 640px) {
  .td-hero__stats {
    width: 100%;
    justify-content: space-between;
  }
  .td-hero__stat {
    flex: 1;
    min-width: 0;
    flex-direction: column;
    align-items: center;
    text-align: center;
  }
  .td-hero__stat-body {
    align-items: center;
  }
}
</style>
