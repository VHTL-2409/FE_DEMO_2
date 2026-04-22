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
  </div>
</template>

<script setup>
import { computed } from 'vue'

const copy = {
  greetingPrefix: 'Chào buổi ',
  subLive: (n) =>
    `${n} kỳ thi đang diễn ra. Vào mục Giám sát trên menu hoặc ô phiên live bên dưới.`,
  subAlerts: (n) => `Có ${n} cảnh báo cần xử lý.`,
  subIdle: 'Không có kỳ thi đang diễn ra. Dùng mục Đề thi hoặc Tạo và xuất bản trên menu để quản lý đề.'
}

const props = defineProps({
  teacherName: { type: String, default: 'Giáo viên' },
  liveExamCount: { type: Number, default: 0 },
  alertCount: { type: Number, default: 0 },
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

@media (max-width: 640px) {
  .td-hero {
    flex-direction: column;
  }
}
</style>
