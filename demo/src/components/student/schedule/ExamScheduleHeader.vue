<template>
  <div class="esh">
    <div class="esh__header">
      <div class="esh__title-block">
        <div class="esh__icon">
          <LucideIcon name="calendar_month" />
        </div>
        <div>
          <h1 class="esh__title">Lịch thi</h1>
          <p class="esh__subtitle">{{ subtitle }}</p>
        </div>
      </div>

      <div class="esh__stats">
        <div class="esh__stat">
          <span class="esh__stat-val">{{ totalCount }}</span>
          <span class="esh__stat-lbl">Tat ca</span>
        </div>
        <div class="esh__stat-divider" />
        <div class="esh__stat">
          <span class="esh__stat-val esh__stat-val--live">{{ liveCount }}</span>
          <span class="esh__stat-lbl">Đang thi</span>
        </div>
        <div class="esh__stat-divider" />
        <div class="esh__stat">
          <span class="esh__stat-val esh__stat-val--upcoming">{{ upcomingCount }}</span>
          <span class="esh__stat-lbl">Sắp tới</span>
        </div>
      </div>
    </div>

    <!-- Filters slot -->
    <div class="esh__filters">
      <slot />
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  totalCount: { type: Number, default: 0 },
  liveCount: { type: Number, default: 0 },
  upcomingCount: { type: Number, default: 0 },
  completedCount: { type: Number, default: 0 }
})

const subtitle = computed(() => {
  if (props.liveCount > 0) return `${props.liveCount} kỳ thi đang diễn ra ngay bây giờ`
  if (props.upcomingCount > 0) return `${props.upcomingCount} kỳ thi sắp tới`
  return 'Theo dõi tất cả kỳ thi của bạn'
})
</script>


<style scoped>
.esh {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
  padding: 1.5rem 1.75rem;
  background: linear-gradient(135deg, #ffffff 0%, #f0f4ff 60%, #ede9fe 100%);
  border: 1.5px solid var(--ds-border);
  border-radius: var(--ds-radius-2xl);
  box-shadow: var(--ds-shadow-sm);
}

.dark .esh {
  background: linear-gradient(135deg, #1e293b 0%, #1e1b4b 60%, #1e1b4b 100%);
  border-color: rgba(79, 70, 229, 0.15);
}

.esh__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1.5rem;
  flex-wrap: wrap;
}

.esh__title-block {
  display: flex;
  align-items: center;
  gap: 0.875rem;
}

.esh__icon {
  width: 52px;
  height: 52px;
  border-radius: var(--ds-radius-2xl);
  background: var(--ds-primary-soft);
  color: var(--ds-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}


.esh__title {
  font-family: var(--ds-font-display);
  font-size: 1.375rem;
  font-weight: 900;
  color: var(--ds-text);
  margin: 0;
  letter-spacing: -0.02em;
}

.dark .esh__title { color: #f1f5f9; }

.esh__subtitle {
  font-size: 0.8rem;
  color: var(--ds-text-muted);
  margin: 0.25rem 0 0;
  font-weight: 500;
}

/* Stats strip */
.esh__stats {
  display: flex;
  align-items: center;
  gap: 0;
  padding: 0.75rem 1.25rem;
  background: rgba(255, 255, 255, 0.96);
  border: 1px solid var(--ds-border);
  border-radius: var(--ds-radius-2xl);
  box-shadow: var(--ds-shadow-xs);
}

.dark .esh__stats { background: rgba(15, 23, 42, 0.96); }

.esh__stat {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.125rem;
  padding: 0 1rem;
}

.esh__stat:first-child { padding-left: 0; }
.esh__stat:last-child { padding-right: 0; }

.esh__stat-divider {
  width: 1px;
  height: 28px;
  background: var(--ds-border);
  flex-shrink: 0;
}

.esh__stat-val {
  font-family: var(--ds-font-display);
  font-size: 1.375rem;
  font-weight: 900;
  color: var(--ds-text);
  line-height: 1;
}

.dark .esh__stat-val { color: #f1f5f9; }

.esh__stat-val--live { color: var(--ds-success); }
.esh__stat-val--upcoming { color: var(--ds-primary); }

.esh__stat-lbl {
  font-size: 0.6rem;
  font-weight: 700;
  color: var(--ds-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.08em;
  white-space: nowrap;
}

/* Filters */
.esh__filters {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

/* Responsive */
@media (max-width: 768px) {
  .esh { padding: 1.25rem; }
  .esh__header { flex-direction: column; align-items: flex-start; }
  .esh__stats { width: 100%; justify-content: center; }
}
</style>
