<template>
  <div class="rhead">
    <!-- Title + tab tabs -->
    <div class="rhead__top">
      <div class="rhead__title-block">
        <div class="rhead__icon">
          <LucideIcon name="history" />
        </div>
        <div>
          <h1 class="rhead__title">Kết quả & Lịch sử</h1>
          <p class="rhead__subtitle">{{ subtitle }}</p>
        </div>
      </div>

      <!-- Tab switcher -->
      <div class="rhead__tabs">
        <button
          v-for="tab in tabs"
          :key="tab.key"
          type="button"
          class="rhead__tab"
          :class="{ 'rhead__tab--active': activeTab === tab.key }"
          @click="$emit('tab-change', tab.key)"
        >
          <LucideIcon :name="tab.icon" />
          {{ tab.label }}
          <span v-if="tabCounts[tab.key] > 0" class="rhead__tab-badge">{{ tabCounts[tab.key] }}</span>
        </button>
      </div>
    </div>

    <!-- Summary row -->
    <div v-if="showSummary" class="rhead__summary-row">
      <div class="rhead__stat">
        <LucideIcon name="quiz" />
        <div>
          <span class="rhead__stat-val">{{ summaryStats.totalCount }}</span>
          <span class="rhead__stat-lbl">Bài đã thi</span>
        </div>
      </div>
      <div class="rhead__stat-sep" />
      <div class="rhead__stat">
        <LucideIcon name="trending_up" />
        <div>
          <span class="rhead__stat-val">{{ summaryStats.avgScore }}</span>
          <span class="rhead__stat-lbl">Điểm TB / 10</span>
        </div>
      </div>
      <div class="rhead__stat-sep" />
      <div class="rhead__stat">
        <LucideIcon name="stars" />
        <div>
          <span class="rhead__stat-val">{{ summaryStats.latestScore }}</span>
          <span class="rhead__stat-lbl">Kết quả mới nhất</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  activeTab: { type: String, default: 'exam' },
  tabCounts: { type: Object, default: () => ({}) },
  showSummary: { type: Boolean, default: false },
  summaryStats: {
    type: Object,
    default: () => ({ totalCount: 0, avgScore: '-', latestScore: '-' })
  }
})

defineEmits(['tab-change'])

const tabs = [
  { key: 'exam', label: 'Bài thi', icon: 'menu_book' },
  { key: 'practice', label: 'Luyện tập', icon: 'model_training' }
]

const subtitle = computed(() =>
  props.activeTab === 'exam' ? 'Xem lại các phiên thi đã hoàn thành.' : 'Xem lại các phiên luyện tập đã hoàn thành.'
)
</script>


<style scoped>
.rhead {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  padding: 1.5rem 1.75rem;
  background: linear-gradient(135deg, #ffffff 0%, #f0f4ff 60%, #ede9fe 100%);
  border: 1.5px solid var(--ds-border);
  border-radius: var(--ds-radius-2xl);
  box-shadow: var(--ds-shadow-sm);
}

.dark .rhead {
  background: linear-gradient(135deg, #1e293b 0%, #1e1b4b 60%, #1e1b4b 100%);
  border-color: rgba(79, 70, 229, 0.15);
}

/* Top row */
.rhead__top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  flex-wrap: wrap;
}

.rhead__title-block {
  display: flex;
  align-items: center;
  gap: 0.875rem;
}

.rhead__icon {
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


.rhead__title {
  font-family: var(--ds-font-display);
  font-size: 1.375rem;
  font-weight: 900;
  color: var(--ds-text);
  margin: 0;
  letter-spacing: -0.02em;
}

.dark .rhead__title { color: #f1f5f9; }

.rhead__subtitle {
  font-size: 0.8rem;
  color: var(--ds-text-muted);
  margin: 0.25rem 0 0;
  font-weight: 500;
}

/* Tabs */
.rhead__tabs {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  background: var(--ds-gray-100);
  padding: 0.25rem;
  border-radius: var(--ds-radius-xl);
}

.dark .rhead__tabs { background: var(--ds-gray-800); }

.rhead__tab {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 1rem;
  border-radius: var(--ds-radius-lg);
  font-size: 0.8rem;
  font-weight: 700;
  color: var(--ds-text-muted);
  background: transparent;
  border: none;
  cursor: pointer;
  transition: all 0.12s ease;
  font-family: inherit;
  white-space: nowrap;
}


.rhead__tab:hover {
  color: var(--ds-text);
  background: var(--ds-surface);
}

.dark .rhead__tab:hover { color: #f1f5f9; background: var(--ds-gray-700); }

.rhead__tab--active {
  background: var(--ds-surface);
  color: var(--ds-primary);
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
}

.dark .rhead__tab--active { background: var(--ds-gray-700); }

.rhead__tab-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 20px;
  height: 20px;
  padding: 0 0.25rem;
  border-radius: var(--ds-radius-full);
  font-size: 0.65rem;
  font-weight: 800;
  background: var(--ds-gray-200);
  color: var(--ds-text-muted);
}

.dark .rhead__tab-badge { background: var(--ds-gray-600); color: var(--ds-gray-300); }

.rhead__tab--active .rhead__tab-badge {
  background: var(--ds-primary-soft);
  color: var(--ds-primary);
}

/* Summary row */
.rhead__summary-row {
  display: flex;
  align-items: center;
  gap: 0;
  padding: 0.875rem 1.25rem;
  background: rgba(255, 255, 255, 0.8);
  border: 1px solid var(--ds-border);
  border-radius: var(--ds-radius-xl);
  backdrop-filter: blur(8px);
  box-shadow: var(--ds-shadow-xs);
}

.dark .rhead__summary-row { background: rgba(30, 41, 59, 0.8); }

.rhead__stat {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex: 1;
  padding: 0 1rem;
}

.rhead__stat:first-child { padding-left: 0; }
.rhead__stat:last-child { padding-right: 0; }

.rhead__stat-icon {
  width: 40px;
  height: 40px;
  border-radius: var(--ds-radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}


.rhead__stat-icon--blue { background: rgba(14, 165, 233, 0.1); color: var(--ds-info); }
.rhead__stat-icon--green { background: rgba(16, 185, 129, 0.1); color: var(--ds-success); }
.rhead__stat-icon--purple { background: rgba(79, 70, 229, 0.1); color: var(--ds-primary); }

.rhead__stat > div {
  display: flex;
  flex-direction: column;
  gap: 0.125rem;
}

.rhead__stat-val {
  font-family: var(--ds-font-display);
  font-size: 1.375rem;
  font-weight: 900;
  color: var(--ds-text);
  line-height: 1;
}

.dark .rhead__stat-val { color: #f1f5f9; }

.rhead__stat-lbl {
  font-size: 0.6rem;
  font-weight: 700;
  color: var(--ds-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.08em;
  white-space: nowrap;
}

.rhead__stat-sep {
  width: 1px;
  height: 36px;
  background: var(--ds-border);
  flex-shrink: 0;
}

/* Responsive */
@media (max-width: 768px) {
  .rhead { padding: 1.25rem; }
  .rhead__top { flex-direction: column; align-items: flex-start; }
  .rhead__summary-row { flex-direction: column; gap: 0.75rem; align-items: flex-start; }
  .rhead__stat-sep { display: none; }
  .rhead__stat { padding: 0; border-bottom: 1px solid var(--ds-border); width: 100%; }
  .rhead__stat:last-child { border-bottom: none; }
}
</style>
