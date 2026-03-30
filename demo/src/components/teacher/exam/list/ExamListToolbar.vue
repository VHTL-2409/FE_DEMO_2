<template>
  <div class="eltb">
    <div class="eltb__inner">
      <!-- Search -->
      <div class="eltb__search-wrap">
        <LucideIcon name="search" />
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
        <LucideIcon name="filter_list" />
        <select
          :value="statusFilter"
          class="eltb__select"
          @change="$emit('update:statusFilter', $event.target.value)"
        >
          <option v-for="opt in statusOptions" :key="opt.value" :value="opt.value">
            {{ opt.label }}
          </option>
        </select>
        <LucideIcon name="expand_more" />
      </div>

      <!-- Sort -->
      <div class="eltb__select-wrap">
        <LucideIcon name="sort" />
        <select
          :value="sortBy"
          class="eltb__select"
          @change="$emit('update:sortBy', $event.target.value)"
        >
          <option v-for="opt in sortOptions" :key="opt.value" :value="opt.value">
            {{ opt.label }}
          </option>
        </select>
        <LucideIcon name="expand_more" />
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
  min-width: 220px;
  max-width: 360px;
}

.eltb__search-icon {
  position: absolute;
  left: 0.75rem;
  top: 50%;
  transform: translateY(-50%);
  font-size: 1.125rem;
  color: var(--ds-text-muted);
  pointer-events: none;
}

.eltb__search {
  width: 100%;
  padding: 0.5625rem 2.25rem;
  background: var(--ds-gray-50);
  border: 1.5px solid var(--ds-border);
  border-radius: var(--ds-radius-xl);
  font-size: 0.875rem;
  color: var(--ds-text);
  outline: none;
  transition: all 0.15s ease;
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
  transition: all 0.12s ease;
}

.eltb__search-clear:hover {
  background: var(--ds-danger-soft);
  color: var(--ds-danger);
}


/* Select */
.eltb__select-wrap {
  position: relative;
  display: flex;
  align-items: center;
}

.eltb__select-icon {
  position: absolute;
  left: 0.625rem;
  font-size: 1rem;
  color: var(--ds-text-muted);
  pointer-events: none;
  z-index: 1;
}

.eltb__select {
  appearance: none;
  padding: 0.5625rem 2rem 0.5625rem 2.25rem;
  background: var(--ds-gray-50);
  border: 1.5px solid var(--ds-border);
  border-radius: var(--ds-radius-xl);
  font-size: 0.8rem;
  font-weight: 600;
  color: var(--ds-text);
  outline: none;
  cursor: pointer;
  transition: all 0.15s ease;
  min-width: 160px;
}

.dark .eltb__select {
  background: var(--ds-gray-700);
  border-color: var(--ds-border-strong);
  color: var(--ds-gray-100);
}

.eltb__select:focus {
  border-color: var(--ds-primary);
  box-shadow: 0 0 0 3px var(--ds-primary-ring);
}

.eltb__select-arrow {
  position: absolute;
  right: 0.5rem;
  font-size: 1.125rem;
  color: var(--ds-text-muted);
  pointer-events: none;
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
  transition: all 0.15s ease;
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
