<template>
  <div class="mf">
    <!-- Search + filters row -->
    <div class="mf__controls">
      <!-- Search -->
      <div class="mf__search">
        <LucideIcon class="mf__search-icon" name="search" :size="18" />
        <input
          v-model="searchQuery"
          type="text"
          class="mf__search-input"
          placeholder="Tìm học sinh, phòng thi..."
        />
        <button
          v-if="searchQuery"
          type="button"
          class="mf__search-clear"
          @click="searchQuery = ''"
        >
          <LucideIcon name="close" :size="16" />
        </button>
      </div>

      <!-- Dropdown filters -->
      <div class="mf__filters">
        <!-- Risk band -->
        <select v-model="riskBand" class="mf__select">
          <option v-for="opt in riskOptions" :key="opt.value" :value="opt.value">
            {{ opt.label }}
          </option>
        </select>

        <!-- Status -->
        <select v-model="status" class="mf__select">
          <option v-for="opt in statusOptions" :key="opt.value" :value="opt.value">
            {{ opt.label }}
          </option>
        </select>

        <!-- Time range -->
        <select v-model="timeRange" class="mf__select">
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
          <LucideIcon name="filter_alt_off" :size="16" />
          Xóa lọc
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { storeToRefs } from 'pinia'
import LucideIcon from '../../common/LucideIcon.vue'
import { useProctorDashboardStore } from '../../../stores/proctorDashboardStore'

const store = useProctorDashboardStore()
const { filters } = storeToRefs(store)

const searchQuery = computed({
  get: () => filters.value.search,
  set: (v) => store.setFilters({ search: v })
})
const riskBand = computed({
  get: () => filters.value.riskBand,
  set: (v) => store.setFilters({ riskBand: v })
})
const status = computed({
  get: () => filters.value.status,
  set: (v) => store.setFilters({ status: v })
})
const timeRange = computed({
  get: () => filters.value.timeRange,
  set: (v) => store.setFilters({ timeRange: v })
})

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
  filters.value.search ||
  filters.value.riskBand !== 'ALL' ||
  filters.value.status !== 'ALL' ||
  filters.value.timeRange !== 'all'
)

const clearAll = () => {
  store.setFilters({ search: '', riskBand: 'ALL', status: 'ALL', timeRange: 'all' })
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

/* Controls row */
.mf__controls {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex-wrap: wrap;
}

/* Mobile responsive */
@media (max-width: 768px) {
  .mf {
    padding: 0.75rem 0;
  }

  .mf__tabs {
    gap: 0.375rem;
  }

  .mf__tab {
    padding: 0.5rem 0.75rem;
    font-size: 0.75rem;
  }

  .mf__controls {
    gap: 0.5rem;
  }

  .mf__search {
    flex: 1 1 100%;
    min-width: 100%;
  }

  .mf__filters {
    flex: 1 1 100%;
    gap: 0.375rem;
  }

  .mf__select {
    flex: 1 1 calc(50% - 0.25rem);
    min-width: 0;
    font-size: 0.75rem;
    padding: 0.5rem 2.5rem 0.5rem 0.75rem;
  }

  .mf__clear-btn {
    flex: 1 1 auto;
    justify-content: center;
  }
}

@media (max-width: 480px) {
  .mf__select {
    flex: 1 1 100%;
  }
}

/* Search */
.mf__search {
  position: relative;
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 0.875rem;
  background: var(--ds-surface);
  border: 1.5px solid var(--ds-border);
  border-radius: var(--ds-radius-xl);
  transition: color 0.15s ease, background-color 0.15s ease, border-color 0.15s ease, box-shadow 0.15s ease, transform 0.15s ease;
  flex-shrink: 0;
  min-width: 200px;
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
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  color: var(--ds-text-muted);
}

.mf__search-icon :deep(svg) {
  width: 18px !important;
  height: 18px !important;
  display: block;
}

.mf__search-input {
  flex: 1;
  border: none;
  background: transparent;
  outline: none;
  font-size: 0.875rem;
  color: var(--ds-text);
  min-width: 0;
  width: 100%;
}

.dark .mf__search-input { color: var(--ds-text); }
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
  transition: color 0.12s ease, background-color 0.12s ease, border-color 0.12s ease, box-shadow 0.12s ease, transform 0.12s ease;
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
  padding: 0.625rem 2.75rem 0.625rem 1rem;
  background: var(--ds-surface);
  border: 2px solid var(--ds-border);
  border-radius: var(--ds-radius-2xl);
  font-size: 0.8rem;
  font-weight: 600;
  color: var(--ds-text-secondary);
  outline: none;
  cursor: pointer;
  transition: color 0.25s cubic-bezier(0.4, 0, 0.2, 1), background-color 0.25s cubic-bezier(0.4, 0, 0.2, 1), border-color 0.25s cubic-bezier(0.4, 0, 0.2, 1), box-shadow 0.25s cubic-bezier(0.4, 0, 0.2, 1), transform 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  appearance: none;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='14' height='14' viewBox='0 0 12 12'%3E%3Cpath fill='%2364748b' d='M2 4l4 4 4-4'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 0.75rem center;
  background-color: var(--ds-surface);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  min-width: 140px;
}

.dark .mf__select {
  background-color: var(--ds-gray-800);
  border-color: var(--ds-border-strong);
  color: var(--ds-text-muted);
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='14' height='14' viewBox='0 0 12 12'%3E%3Cpath fill='%2394a3b8' d='M2 4l4 4 4-4'/%3E%3C/svg%3E");
}

.mf__select:hover {
  border-color: var(--ds-primary-border);
  background-color: white;
  box-shadow: 0 4px 16px rgba(79, 70, 229, 0.12);
  transform: translateY(-1px);
}

.dark .mf__select:hover {
  background-color: var(--ds-gray-700);
  border-color: var(--ds-primary-border);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.2);
  transform: translateY(-1px);
}

.mf__select:focus {
  border-color: var(--ds-primary);
  box-shadow: 0 0 0 4px var(--ds-primary-ring), 0 8px 24px rgba(79, 70, 229, 0.2);
  background-color: white;
  transform: scale(1.02);
}

.dark .mf__select:focus { 
  background-color: var(--ds-gray-700);
  box-shadow: 0 0 0 4px rgba(79, 70, 229, 0.3), 0 8px 24px rgba(0, 0, 0, 0.3);
  transform: scale(1.02);
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
  transition: color 0.15s ease, background-color 0.15s ease, border-color 0.15s ease, box-shadow 0.15s ease, transform 0.15s ease;
}

.mf__clear-btn:hover {
  background: var(--ds-danger);
  color: white;
}
</style>
@media (prefers-reduced-motion: reduce) {
  * {
    transition-duration: 0.01ms !important;
    animation-duration: 0.01ms !important;
  }
}