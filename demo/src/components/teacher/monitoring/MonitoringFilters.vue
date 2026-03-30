<template>
  <div class="mf">
    <!-- Status tabs -->
    <div class="mf__tabs">
      <button
        v-for="tab in tabs"
        :key="tab.id"
        type="button"
        class="mf__tab"
        :class="{
          'mf__tab--active': activeTab === tab.id,
          [`mf__tab--${tab.color}`]: activeTab === tab.id
        }"
        @click="$emit('update:activeTab', tab.id)"
      >
        <LucideIcon :name="tab.icon" />
        <span class="mf__tab-label">{{ tab.label }}</span>
        <span class="mf__tab-count" :class="[`mf__tab-count--${tab.color}`]">{{ tab.count }}</span>
      </button>
    </div>

    <!-- Search + filters row -->
    <div class="mf__controls">
      <!-- Search -->
      <div class="mf__search">
        <LucideIcon name="search" />
        <input
          :value="searchQuery"
          type="text"
          class="mf__search-input"
          placeholder="Tìm học sinh, phòng thi..."
          @input="$emit('update:searchQuery', $event.target.value)"
        />
        <button
          v-if="searchQuery"
          type="button"
          class="mf__search-clear"
          @click="$emit('update:searchQuery', '')"
        >
          <LucideIcon name="close" />
        </button>
      </div>

      <!-- Dropdown filters -->
      <div class="mf__filters">
        <!-- Risk band -->
        <select
          :value="riskBand"
          class="mf__select"
          @change="$emit('update:riskBand', $event.target.value)"
        >
          <option v-for="opt in riskOptions" :key="opt.value" :value="opt.value">
            {{ opt.label }}
          </option>
        </select>

        <!-- Status -->
        <select
          :value="status"
          class="mf__select"
          @change="$emit('update:status', $event.target.value)"
        >
          <option v-for="opt in statusOptions" :key="opt.value" :value="opt.value">
            {{ opt.label }}
          </option>
        </select>

        <!-- Time range -->
        <select
          :value="timeRange"
          class="mf__select"
          @change="$emit('update:timeRange', $event.target.value)"
        >
          <option v-for="opt in timeOptions" :key="opt.value" :value="opt.value">
            {{ opt.label }}
          </option>
        </select>

        <!-- Clear all -->
        <button
          v-if="hasActiveFilters"
          type="button"
          class="mf__clear-btn"
          @click="clearAll"
        >
          <LucideIcon name="filter_alt_off" />
          Xóa lọc
        </button>
      </div>

      <!-- View toggle -->
      <div class="mf__view-toggle">
        <button
          type="button"
          class="mf__view-btn"
          :class="{ 'mf__view-btn--active': viewMode === 'grid' }"
          title="Chế độ lưới"
          @click="$emit('update:viewMode', 'grid')"
        >
          <LucideIcon name="grid_view" />
        </button>
        <button
          type="button"
          class="mf__view-btn"
          :class="{ 'mf__view-btn--active': viewMode === 'table' }"
          title="Chế độ bảng"
          @click="$emit('update:viewMode', 'table')"
        >
          <LucideIcon name="view_list" />
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  activeTab: { type: String, default: 'all' },
  searchQuery: { type: String, default: '' },
  riskBand: { type: String, default: 'ALL' },
  status: { type: String, default: 'ALL' },
  timeRange: { type: String, default: 'all' },
  viewMode: { type: String, default: 'grid' },
  stats: {
    type: Object,
    default: () => ({ total: 0, stable: 0, attention: 0, critical: 0 })
  }
})

const emit = defineEmits([
  'update:activeTab',
  'update:searchQuery',
  'update:riskBand',
  'update:status',
  'update:timeRange',
  'update:viewMode',
  'clear-all'
])

const tabs = computed(() => [
  {
    id: 'all',
    label: 'Tất cả',
    icon: 'dashboard',
    color: 'default',
    count: props.stats.total || 0
  },
  {
    id: 'stable',
    label: 'Ổn định',
    icon: 'verified_user',
    color: 'success',
    count: props.stats.stable || 0
  },
  {
    id: 'attention',
    label: 'Cần chú ý',
    icon: 'warning',
    color: 'warning',
    count: props.stats.attention || 0
  },
  {
    id: 'critical',
    label: 'Nghiêm trọng',
    icon: 'gpp_bad',
    color: 'danger',
    count: props.stats.critical || 0
  }
])

const riskOptions = [
  { value: 'ALL', label: 'Mọi mức rủi ro' },
  { value: 'CLEAN', label: 'An toàn' },
  { value: 'SUSPICIOUS', label: 'Đáng nghi' },
  { value: 'HIGH_RISK', label: 'Nguy cơ cao' },
  { value: 'CRITICAL', label: 'Nghiêm trọng' }
]

const statusOptions = [
  { value: 'ALL', label: 'Mọi trạng thái' },
  { value: 'ONLINE', label: 'Online' },
  { value: 'OFFLINE', label: 'Offline' },
  { value: 'PAUSED', label: 'Tạm dừng' },
  { value: 'SUBMITTED', label: 'Đã nộp' }
]

const timeOptions = [
  { value: 'all', label: 'Mọi thời gian' },
  { value: '5m', label: '5 phút' },
  { value: '15m', label: '15 phút' },
  { value: '1h', label: '1 giờ' },
  { value: 'today', label: 'Hôm nay' }
]

const hasActiveFilters = computed(() =>
  props.searchQuery ||
  props.riskBand !== 'ALL' ||
  props.status !== 'ALL' ||
  props.timeRange !== 'all'
)

const clearAll = () => {
  emit('update:searchQuery', '')
  emit('update:riskBand', 'ALL')
  emit('update:status', 'ALL')
  emit('update:timeRange', 'all')
}
</script>


<style scoped>
.mf {
  display: flex;
  flex-direction: column;
  gap: 0.875rem;
  padding: 0.875rem 0;
  margin-bottom: 0.5rem;
}

/* Tabs */
.mf__tabs {
  display: flex;
  gap: 0.25rem;
  overflow-x: auto;
  scrollbar-width: none;
}

.mf__tabs::-webkit-scrollbar {
  display: none;
}

.mf__tab {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.625rem 1rem;
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

.dark .mf__tab {
  background: var(--ds-gray-800);
  border-color: var(--ds-border-strong);
}

.mf__tab:hover {
  background: var(--ds-gray-50);
  color: var(--ds-text);
}

.dark .mf__tab:hover {
  background: var(--ds-gray-700);
}

.mf__tab--active {
  background: var(--ds-gray-50);
  border-color: var(--ds-primary-border);
  color: var(--ds-primary);
}

.dark .mf__tab--active {
  background: rgba(79, 70, 229, 0.1);
}

.mf__tab-icon {
  font-size: 1.125rem;
}

.mf__tab-label {
  font-weight: 700;
}

.mf__tab-count {
  padding: 0.1rem 0.5rem;
  border-radius: var(--ds-radius-full);
  font-size: 0.7rem;
  font-weight: 800;
  background: var(--ds-gray-100);
  color: var(--ds-text-muted);
  transition: all 0.15s ease;
}

.dark .mf__tab-count {
  background: var(--ds-gray-700);
}

.mf__tab--active .mf__tab-count {
  color: var(--ds-primary);
  background: var(--ds-primary-soft);
}

.mf__tab--success.mf__tab--active .mf__tab-count {
  color: var(--ds-success);
  background: var(--ds-success-soft);
}

.mf__tab--warning.mf__tab--active .mf__tab-count {
  color: var(--ds-warning);
  background: rgba(234, 179, 8, 0.1);
}

.mf__tab--danger.mf__tab--active .mf__tab-count {
  color: var(--ds-danger);
  background: var(--ds-danger-soft);
}

/* Controls row */
.mf__controls {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex-wrap: wrap;
}

/* Search */
.mf__search {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex: 1;
  min-width: 200px;
  max-width: 360px;
  padding: 0.5rem 0.875rem;
  background: var(--ds-surface);
  border: 1.5px solid var(--ds-border);
  border-radius: var(--ds-radius-xl);
  transition: all 0.15s ease;
}

.dark .mf__search {
  background: var(--ds-gray-800);
  border-color: var(--ds-border-strong);
}

.mf__search:focus-within {
  border-color: var(--ds-primary);
  box-shadow: 0 0 0 3px var(--ds-primary-ring);
}

.mf__search-icon {
  font-size: 1.125rem;
  color: var(--ds-text-muted);
  flex-shrink: 0;
}

.mf__search-input {
  flex: 1;
  border: none;
  background: transparent;
  outline: none;
  font-size: 0.875rem;
  color: var(--ds-text);
  min-width: 0;
}

.dark .mf__search-input { color: #f1f5f9; }
.mf__search-input::placeholder { color: var(--ds-text-muted); }

.mf__search-clear {
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

.dark .mf__search-clear { background: var(--ds-gray-700); }
.mf__search-clear:hover { background: var(--ds-gray-200); color: var(--ds-text); }
.dark .mf__search-clear:hover { background: var(--ds-gray-600); }

/* Filters */
.mf__filters {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.mf__select {
  padding: 0.5rem 2rem 0.5rem 0.75rem;
  background: var(--ds-surface);
  border: 1.5px solid var(--ds-border);
  border-radius: var(--ds-radius-xl);
  font-size: 0.8rem;
  font-weight: 600;
  color: var(--ds-text-secondary);
  outline: none;
  cursor: pointer;
  transition: all 0.15s ease;
  appearance: none;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 12 12'%3E%3Cpath fill='%2364748b' d='M2 4l4 4 4-4'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 0.625rem center;
  padding-right: 2rem;
}

.dark .mf__select {
  background-color: var(--ds-gray-800);
  border-color: var(--ds-border-strong);
  color: #94a3b8;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 12 12'%3E%3Cpath fill='%2394a3b8' d='M2 4l4 4 4-4'/%3E%3C/svg%3E");
}

.mf__select:focus {
  border-color: var(--ds-primary);
  box-shadow: 0 0 0 3px var(--ds-primary-ring);
}

.mf__select:hover {
  border-color: var(--ds-gray-300);
}

.dark .mf__select:hover {
  border-color: var(--ds-gray-600);
}

.mf__clear-btn {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  padding: 0.5rem 0.875rem;
  border-radius: var(--ds-radius-xl);
  background: rgba(220, 38, 38, 0.06);
  border: 1.5px solid rgba(220, 38, 38, 0.2);
  background: var(--ds-danger-soft);
  color: var(--ds-danger);
  font-size: 0.8rem;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.15s ease;
}

.mf__clear-btn:hover {
  background: var(--ds-danger);
  color: white;
}


/* View toggle */
.mf__view-toggle {
  display: flex;
  gap: 0.125rem;
  padding: 0.25rem;
  background: var(--ds-gray-100);
  border: 1px solid var(--ds-border);
  border-radius: var(--ds-radius-lg);
  margin-left: auto;
}

.dark .mf__view-toggle {
  background: var(--ds-gray-800);
  border-color: var(--ds-border-strong);
}

.mf__view-btn {
  width: 2rem;
  height: 2rem;
  border-radius: var(--ds-radius-md);
  border: none;
  background: transparent;
  color: var(--ds-text-muted);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.15s ease;
}

.mf__view-btn:hover {
  background: var(--ds-gray-200);
  color: var(--ds-text);
}

.dark .mf__view-btn:hover {
  background: var(--ds-gray-700);
}

.mf__view-btn--active {
  background: var(--ds-surface);
  color: var(--ds-primary);
  box-shadow: var(--ds-shadow-xs);
}

.dark .mf__view-btn--active {
  background: var(--ds-gray-700);
}

</style>
