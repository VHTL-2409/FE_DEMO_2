<template>
  <div class="af">
    <!-- Status tabs -->
    <div class="af__tabs">
      <button
        v-for="tab in statusTabs"
        :key="tab.id"
        type="button"
        class="af__tab"
        :class="{ 'af__tab--active': activeTab === tab.id }"
        @click="$emit('update:activeTab', tab.id)"
      >
        <LucideIcon :name="tab.icon" />
        {{ tab.label }}
        <span v-if="tab.count !== undefined" class="af__tab-count">{{ tab.count }}</span>
      </button>
    </div>

    <!-- Controls row -->
    <div class="af__controls">
      <!-- Search -->
      <div class="af__search">
        <LucideIcon name="search" />
        <input
          :value="searchQuery"
          type="text"
          class="af__search-input"
          placeholder="Tìm học sinh..."
          @input="$emit('update:searchQuery', $event.target.value)"
        />
        <button
          v-if="searchQuery"
          type="button"
          class="af__search-clear"
          @click="$emit('update:searchQuery', '')"
        >
          <LucideIcon name="close" />
        </button>
      </div>

      <!-- Score range filter -->
      <select
        :value="scoreFilter"
        class="af__select"
        @change="$emit('update:scoreFilter', $event.target.value)"
      >
        <option v-for="opt in scoreOptions" :key="opt.value" :value="opt.value">
          {{ opt.label }}
        </option>
      </select>

      <!-- Sort -->
      <select
        :value="sortBy"
        class="af__select"
        @change="$emit('update:sortBy', $event.target.value)"
      >
        <option v-for="opt in sortOptions" :key="opt.value" :value="opt.value">
          {{ opt.label }}
        </option>
      </select>

      <!-- Spacer -->
      <div class="af__spacer" />

      <!-- Export -->
      <button
        v-if="showExport"
        type="button"
        class="af__export-btn"
        :disabled="exportLoading"
        @click="$emit('export')"
      >
        <LucideIcon name="download" />
        Xuất CSV
      </button>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  activeTab: { type: String, default: 'all' },
  searchQuery: { type: String, default: '' },
  scoreFilter: { type: String, default: 'all' },
  sortBy: { type: String, default: 'score' },
  showExport: { type: Boolean, default: true },
  exportLoading: { type: Boolean, default: false },
  stats: {
    type: Object,
    default: () => ({ total: 0, passed: 0, failed: 0 })
  }
})

defineEmits([
  'update:activeTab',
  'update:searchQuery',
  'update:scoreFilter',
  'update:sortBy',
  'export'
])

const statusTabs = computed(() => [
  {
    id: 'all',
    label: 'Tất cả',
    icon: 'group',
    count: props.stats.total
  },
  {
    id: 'passed',
    label: 'Đạt',
    icon: 'check_circle',
    count: props.stats.passed
  },
  {
    id: 'failed',
    label: 'Không đạt',
    icon: 'cancel',
    count: props.stats.failed
  }
])

const scoreOptions = [
  { value: 'all', label: 'Mọi điểm' },
  { value: 'high', label: 'Xuất sắc (8-10)' },
  { value: 'medium', label: 'Trung bình (5-7.9)' },
  { value: 'low', label: 'Yếu (0-4.9)' }
]

const sortOptions = [
  { value: 'score', label: 'Theo điểm' },
  { value: 'score_desc', label: 'Điểm cao nhất' },
  { value: 'name', label: 'Theo tên' },
  { value: 'time', label: 'Theo thời gian' },
  { value: 'warnings', label: 'Theo cảnh báo' }
]
</script>


<style scoped>
.af {
  display: flex;
  flex-direction: column;
  gap: 0.875rem;
  padding: 0.875rem 0;
}

/* Tabs */
.af__tabs {
  display: flex;
  gap: 0.25rem;
  overflow-x: auto;
  scrollbar-width: none;
}

.af__tabs::-webkit-scrollbar { display: none; }

.af__tab {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 1rem;
  border-radius: var(--ds-radius-xl);
  border: 1.5px solid var(--ds-border);
  background: var(--ds-surface);
  color: var(--ds-text-muted);
  font-size: 0.8rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s ease;
  white-space: nowrap;
  flex-shrink: 0;
}

.dark .af__tab {
  background: var(--ds-gray-800);
  border-color: var(--ds-border-strong);
}

.af__tab:hover {
  background: var(--ds-gray-50);
  color: var(--ds-text);
}

.dark .af__tab:hover {
  background: var(--ds-gray-700);
}

.af__tab--active {
  background: var(--ds-primary-soft);
  border-color: var(--ds-primary-border);
  color: var(--ds-primary);
}

.dark .af__tab--active {
  background: rgba(79, 70, 229, 0.1);
}

.af__tab-icon {
  font-size: 1rem;
}

.af__tab-count {
  padding: 0.1rem 0.5rem;
  border-radius: var(--ds-radius-full);
  font-size: 0.7rem;
  font-weight: 800;
  background: var(--ds-gray-100);
  color: var(--ds-text-muted);
}

.dark .af__tab-count { background: var(--ds-gray-700); }

.af__tab--active .af__tab-count {
  background: var(--ds-primary-soft);
  color: var(--ds-primary);
}

/* Controls */
.af__controls {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex-wrap: wrap;
}

/* Search */
.af__search {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex: 1;
  min-width: 180px;
  max-width: 300px;
  padding: 0.5rem 0.875rem;
  background: var(--ds-surface);
  border: 1.5px solid var(--ds-border);
  border-radius: var(--ds-radius-xl);
  transition: all 0.15s ease;
}

.dark .af__search {
  background: var(--ds-gray-800);
  border-color: var(--ds-border-strong);
}

.af__search:focus-within {
  border-color: var(--ds-primary);
  box-shadow: 0 0 0 3px var(--ds-primary-ring);
}

.af__search-icon {
  font-size: 1.125rem;
  color: var(--ds-text-muted);
  flex-shrink: 0;
}

.af__search-input {
  flex: 1;
  border: none;
  background: transparent;
  outline: none;
  font-size: 0.875rem;
  color: var(--ds-text);
  min-width: 0;
}

.dark .af__search-input { color: #f1f5f9; }
.af__search-input::placeholder { color: var(--ds-text-muted); }

.af__search-clear {
  width: 1.5rem;
  height: 1.5rem;
  border: none;
  border-radius: 50%;
  background: var(--ds-gray-100);
  color: var(--ds-text-muted);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.12s ease;
  flex-shrink: 0;
}

.dark .af__search-clear { background: var(--ds-gray-700); }
.af__search-clear:hover { background: var(--ds-gray-200); color: var(--ds-text); }
.dark .af__search-clear:hover { background: var(--ds-gray-600); }

/* Select */
.af__select {
  padding: 0.5rem 2rem 0.5rem 0.75rem;
  background: var(--ds-surface);
  border: 1.5px solid var(--ds-border);
  border-radius: var(--ds-radius-xl);
  font-size: 0.8rem;
  font-weight: 600;
  color: var(--ds-text-secondary);
  outline: none;
  cursor: pointer;
  appearance: none;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 12 12'%3E%3Cpath fill='%2364748b' d='M2 4l4 4 4-4'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 0.625rem center;
  background-color: var(--ds-surface);
  transition: all 0.15s ease;
}

.dark .af__select {
  background-color: var(--ds-gray-800);
  border-color: var(--ds-border-strong);
  color: #94a3b8;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 12 12'%3E%3Cpath fill='%2394a3b8' d='M2 4l4 4 4-4'/%3E%3C/svg%3E");
}

.af__select:focus {
  border-color: var(--ds-primary);
  box-shadow: 0 0 0 3px var(--ds-primary-ring);
}

/* Spacer */
.af__spacer { flex: 1; }

/* Export */
.af__export-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 1.125rem;
  border-radius: var(--ds-radius-xl);
  border: 1.5px solid var(--ds-primary-border);
  background: var(--ds-primary);
  color: white;
  font-size: 0.8rem;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.15s ease;
  white-space: nowrap;
  box-shadow: 0 2px 8px rgba(79, 70, 229, 0.2);
  font-family: inherit;
}

.af__export-btn:hover:not(:disabled) {
  background: var(--ds-primary-hover, #4338ca);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(79, 70, 229, 0.3);
}

.af__export-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  transform: none;
}
</style>
