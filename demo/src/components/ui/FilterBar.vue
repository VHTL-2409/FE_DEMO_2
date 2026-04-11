<template>
  <div class="filter-bar flex flex-wrap items-center gap-3">
    <!-- Search -->
    <div v-if="showSearch" class="filter-bar__field relative min-w-[200px] flex-1">
      <LucideIcon name="search" class="filter-bar__icon filter-bar__icon--leading" />
      <input
        v-model="searchValue"
        type="text"
        :placeholder="searchPlaceholder"
        class="ds-input w-full pl-10 pr-4 py-2.5 rounded-[var(--ds-radius-lg)] text-sm"
      />
    </div>

    <!-- Date -->
    <div v-if="showDate" class="filter-bar__field relative">
      <LucideIcon name="event" size="18" class="filter-bar__icon filter-bar__icon--leading" />
      <input
        v-model="dateValue"
        type="date"
        class="ds-input w-full pl-10 pr-4 py-2.5 rounded-[var(--ds-radius-lg)] text-sm min-w-[140px]"
      />
    </div>

    <!-- Status filter -->
    <div v-if="statusOptions?.length" class="filter-bar__field filter-bar__field--select">
      <LucideIcon name="filter_list" size="18" class="filter-bar__icon filter-bar__icon--leading" />
      <select
        v-model="statusValue"
        class="filter-bar__select"
      >
        <option v-for="opt in statusOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
      </select>
      <LucideIcon name="expand_more" size="18" class="filter-bar__icon filter-bar__icon--trailing" />
    </div>

    <!-- Extra slot -->
    <div v-if="$slots.extra" class="flex items-center gap-2">
      <slot name="extra" />
    </div>

    <!-- Reset -->
    <button
      v-if="showReset && hasActiveFilters"
      type="button"
      class="inline-flex items-center gap-1.5 rounded-[var(--ds-radius-lg)] border border-[var(--ds-border)] bg-[var(--ds-surface)] px-3.5 py-2 text-sm font-semibold text-[var(--ds-text-secondary)] transition-colors hover:bg-[var(--ds-gray-100)] hover:text-[var(--ds-text)]"
      @click="resetAll"
    >
      <LucideIcon name="filter_alt_off" size="18" />
      Xóa lọc
    </button>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'

const props = defineProps({
  modelSearch: { type: String, default: '' },
  modelDate: { type: String, default: '' },
  modelStatus: { type: String, default: '' },
  statusOptions: { type: Array, default: () => [] },
  searchPlaceholder: { type: String, default: 'Tìm kiếm...' },
  showSearch: { type: Boolean, default: true },
  showDate: { type: Boolean, default: false },
  showReset: { type: Boolean, default: true },
  debounceMs: { type: Number, default: 300 }
})

const emit = defineEmits(['update:modelSearch', 'update:modelDate', 'update:modelStatus', 'search', 'reset'])

const searchValue = ref(props.modelSearch)
const dateValue = ref(props.modelDate)
const statusValue = ref(props.modelStatus)
let debounceTimer = null

const hasActiveFilters = computed(() =>
  searchValue.value || dateValue.value || statusValue.value
)

watch(searchValue, (val) => {
  clearTimeout(debounceTimer)
  debounceTimer = setTimeout(() => {
    emit('update:modelSearch', val)
    emit('search', { search: val, date: dateValue.value, status: statusValue.value })
  }, props.debounceMs)
})

watch(dateValue, (val) => {
  emit('update:modelDate', val)
  emit('search', { search: searchValue.value, date: val, status: statusValue.value })
})

watch(statusValue, (val) => {
  emit('update:modelStatus', val)
  emit('search', { search: searchValue.value, date: dateValue.value, status: val })
})

const resetAll = () => {
  searchValue.value = ''
  dateValue.value = ''
  statusValue.value = ''
  emit('update:modelSearch', '')
  emit('update:modelDate', '')
  emit('update:modelStatus', '')
  emit('reset')
  emit('search', { search: '', date: '', status: '' })
}
</script>


<style scoped>
.ds-input {
  background: var(--ds-surface);
  border: 1px solid var(--ds-border);
  color: var(--ds-text);
  outline: none;
  transition: border-color 0.15s, box-shadow 0.15s;
}
.ds-input::placeholder { color: var(--ds-text-muted); }
.ds-input:focus {
  border-color: var(--ds-primary);
  box-shadow: 0 0 0 3px var(--ds-primary-ring);
}

.filter-bar__field {
  position: relative;
}

.filter-bar__field--select {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: 0.625rem;
  min-width: 160px;
  padding: 0 0.875rem;
  background: var(--ds-surface);
  border: 1px solid var(--ds-border);
  border-radius: var(--ds-radius-lg);
  transition: border-color 0.15s, box-shadow 0.15s, background-color 0.15s;
}

.filter-bar__field--select:focus-within {
  border-color: var(--ds-primary);
  box-shadow: 0 0 0 3px var(--ds-primary-ring);
}

.filter-bar__icon {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  color: var(--ds-text-muted);
  pointer-events: none;
  display: flex;
  align-items: center;
  justify-content: center;
}

.filter-bar__icon--leading {
  left: 0.875rem;
}

.filter-bar__icon--trailing {
  right: 0.75rem;
}

.filter-bar__field--select .filter-bar__icon {
  position: static;
  transform: none;
}

.filter-bar__select {
  width: 100%;
  min-width: 0;
  padding: 0.625rem 0;
  border: none;
  background: transparent;
  color: var(--ds-text);
  font-size: 0.875rem;
  outline: none;
  appearance: none;
}
</style>
