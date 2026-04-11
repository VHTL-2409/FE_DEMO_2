<template>
  <div class="eltb">
    <div class="eltb__inner">
      <!-- Search -->
      <div class="eltb__search-wrap">
        <LucideIcon class="eltb__search-icon" name="search" />
        <input
          :value="searchQuery"
          type="text"
          class="eltb__search"
          placeholder="Tìm theo tên đề, môn học, lớp..."
          @input="$emit('update:searchQuery', $event.target.value)"
        />
        <button
          v-if="searchQuery"
          type="button"
          class="eltb__search-clear"
          @click="$emit('update:searchQuery', '')"
        >
          <LucideIcon name="close" />
        </button>
      </div>

      <!-- Status filter -->
      <div class="eltb__select-wrap">
        <div class="eltb__select-shell">
          <LucideIcon class="eltb__select-icon" name="filter_list" />
          <select
            :value="statusFilter"
            class="eltb__select"
            @change="$emit('update:statusFilter', $event.target.value)"
          >
            <option v-for="opt in statusOptions" :key="opt.value" :value="opt.value">
              {{ opt.label }}
            </option>
          </select>
          <LucideIcon class="eltb__select-arrow" name="expand_more" />
        </div>
      </div>

      <!-- Sort -->
      <div class="eltb__select-wrap">
        <div class="eltb__select-shell">
          <LucideIcon class="eltb__select-icon" name="sort" />
          <select
            :value="sortBy"
            class="eltb__select"
            @change="$emit('update:sortBy', $event.target.value)"
          >
            <option v-for="opt in sortOptions" :key="opt.value" :value="opt.value">
              {{ opt.label }}
            </option>
          </select>
          <LucideIcon class="eltb__select-arrow" name="expand_more" />
        </div>
      </div>

      <!-- Reset -->
      <button
        v-if="hasActiveFilters"
        type="button"
        class="eltb__reset"
        @click="$emit('reset')"
      >
        <LucideIcon name="filter_alt_off" />
        Xóa lọc
        <span class="eltb__reset-badge">{{ activeFilterCount }}</span>
      </button>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import LucideIcon from '../../../common/LucideIcon.vue'

const props = defineProps({
  searchQuery: { type: String, default: '' },
  statusFilter: { type: String, default: 'all' },
  sortBy: { type: String, default: 'newest' }
})

defineEmits(['update:searchQuery', 'update:statusFilter', 'update:sortBy', 'reset'])

const statusOptions = [
  { value: 'all', label: 'Tất cả trạng thái' },
  { value: 'draft', label: 'Nháp' },
  { value: 'upcoming', label: 'Sắp diễn ra' },
  { value: 'live', label: 'Đang diễn ra' },
  { value: 'ended', label: 'Đã kết thúc' },
  { value: 'archived', label: 'Lưu trữ' }
]

const sortOptions = [
  { value: 'newest', label: 'Mới nhất' },
  { value: 'upcoming', label: 'Sắp diễn ra' },
  { value: 'recently_updated', label: 'Vừa cập nhật' },
  { value: 'most_students', label: 'Nhiều học sinh' }
]

const hasActiveFilters = computed(() =>
  props.searchQuery || props.statusFilter !== 'all'
)

const activeFilterCount = computed(() => {
  let n = 0
  if (props.searchQuery) n++
  if (props.statusFilter !== 'all') n++
  return n
})
</script>


<style scoped>
.eltb {
  background: var(--ds-surface);
  border: 1px solid var(--ds-border);
  border-radius: var(--ds-radius-2xl);
  padding: 0.875rem 1.25rem;
}

.dark .eltb {
  background: var(--ds-gray-800);
  border-color: var(--ds-border-strong);
}

.eltb__inner {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex-wrap: wrap;
}

/* Search */
.eltb__search-wrap {
  position: relative;
  flex: 1;
  min-width: 160px;
  width: 100%;
}

/* Mobile responsive */
@media (max-width: 768px) {
  .eltb {
    padding: 0.75rem;
  }

  .eltb__inner {
    gap: 0.5rem;
  }

  .eltb__search-wrap {
    flex: 1 1 100%;
    min-width: 100%;
  }

  .eltb__select-wrap {
    flex: 1 1 calc(50% - 0.25rem);
    min-width: 140px;
  }

  .eltb__reset {
    flex: 1 1 auto;
    justify-content: center;
  }

  .eltb__search {
    padding: 0.625rem 2.5rem 0.625rem 2.75rem;
  }

  .eltb__select {
    font-size: 0.75rem;
    padding: 0.5rem 0;
  }
}

@media (max-width: 480px) {
  .eltb__select-wrap {
    flex: 1 1 100%;
  }

  .eltb__select {
    width: 100%;
  }
}

.eltb__search-icon {
  position: absolute;
  left: 0.875rem;
  top: 50%;
  transform: translateY(-50%);
  color: var(--ds-text-muted);
  pointer-events: none;
  display: flex;
  align-items: center;
  justify-content: center;
}

.eltb__search-icon :deep(svg),
.eltb__search-icon svg {
  width: 18px !important;
  height: 18px !important;
  display: block;
}

.eltb__search {
  width: 100%;
  padding: 0.625rem 2.75rem;
  background: var(--ds-gray-50);
  border: 1.5px solid var(--ds-border);
  border-radius: var(--ds-radius-xl);
  font-size: 0.875rem;
  color: var(--ds-text);
  outline: none;
  transition: color 0.15s ease, background-color 0.15s ease, border-color 0.15s ease, box-shadow 0.15s ease, transform 0.15s ease;
}

.dark .eltb__search {
  background: var(--ds-gray-700);
  border-color: var(--ds-border-strong);
  color: var(--ds-gray-100);
}

.eltb__search::placeholder { color: var(--ds-text-muted); }

.eltb__search:focus {
  border-color: var(--ds-primary);
  box-shadow: 0 0 0 3px var(--ds-primary-ring);
  background: white;
}

.dark .eltb__search:focus { background: var(--ds-gray-700); box-shadow: 0 0 0 3px rgba(79, 70, 229, 0.3); }

.eltb__search-clear {
  position: absolute;
  right: 0.5rem;
  top: 50%;
  transform: translateY(-50%);
  width: 24px;
  height: 24px;
  border: none;
  background: var(--ds-gray-200);
  color: var(--ds-text-muted);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: color 0.12s ease, background-color 0.12s ease, border-color 0.12s ease, box-shadow 0.12s ease, transform 0.12s ease;
}

.eltb__search-clear:hover {
  background: var(--ds-danger-soft);
  color: var(--ds-danger);
}


/* Select */
.eltb__select-wrap {
  position: relative;
  transition: transform 0.2s cubic-bezier(0.34, 1.56, 0.64, 1);
  flex-shrink: 0;
  min-width: 160px;
}

.eltb__select-wrap:hover {
  transform: translateY(-1px);
}

.eltb__select-shell {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: 0.625rem;
  width: 100%;
  padding: 0 0.875rem;
  background: var(--ds-gray-50);
  border: 2px solid var(--ds-border);
  border-radius: var(--ds-radius-2xl);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  transition: color 0.25s cubic-bezier(0.4, 0, 0.2, 1), background-color 0.25s cubic-bezier(0.4, 0, 0.2, 1), border-color 0.25s cubic-bezier(0.4, 0, 0.2, 1), box-shadow 0.25s cubic-bezier(0.4, 0, 0.2, 1), transform 0.25s cubic-bezier(0.4, 0, 0.2, 1);
}

.dark .eltb__select-shell {
  background: var(--ds-gray-700);
  border-color: var(--ds-border-strong);
}

.eltb__select-shell:hover {
  border-color: var(--ds-primary-border);
  background: white;
  box-shadow: 0 4px 16px rgba(79, 70, 229, 0.12);
}

.dark .eltb__select-shell:hover {
  background: var(--ds-gray-600);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.2);
}

.eltb__select-shell:focus-within {
  border-color: var(--ds-primary);
  box-shadow: 0 0 0 4px var(--ds-primary-ring), 0 8px 24px rgba(79, 70, 229, 0.2);
  background: white;
}

.dark .eltb__select-shell:focus-within {
  background: var(--ds-gray-600);
  box-shadow: 0 0 0 4px rgba(79, 70, 229, 0.3), 0 8px 24px rgba(0, 0, 0, 0.3);
}

.eltb__select-icon {
  color: var(--ds-text-muted);
  pointer-events: none;
  display: flex;
  align-items: center;
  justify-content: center;
}

.eltb__select {
  appearance: none;
  padding: 0.625rem 0;
  background: transparent;
  border: none;
  font-size: 0.8rem;
  font-weight: 600;
  color: var(--ds-text);
  outline: none;
  cursor: pointer;
  width: 100%;
  min-width: 0;
  line-height: 1.4;
}

.dark .eltb__select {
  color: var(--ds-gray-100);
}

.eltb__select-arrow {
  color: var(--ds-text-muted);
  pointer-events: none;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* Reset */
.eltb__reset {
  display: inline-flex;
  align-items: center;
  gap: 0.375rem;
  padding: 0.5625rem 0.875rem;
  background: var(--ds-danger-soft);
  border: 1.5px solid rgba(220, 38, 38, 0.2);
  border-radius: var(--ds-radius-xl);
  font-size: 0.8rem;
  font-weight: 700;
  color: var(--ds-danger);
  cursor: pointer;
  transition: color 0.15s ease, background-color 0.15s ease, border-color 0.15s ease, box-shadow 0.15s ease, transform 0.15s ease;
  white-space: nowrap;
}

.eltb__reset:hover {
  background: var(--ds-danger);
  color: white;
  border-color: var(--ds-danger);
}


.eltb__reset-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: var(--ds-danger);
  color: white;
  font-size: 0.65rem;
}

.eltb__reset:hover .eltb__reset-badge {
  background: rgba(255,255,255,0.3);
}
</style>