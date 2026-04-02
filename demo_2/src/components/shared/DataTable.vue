<template>
  <div class="data-table-wrapper">
    <!-- Toolbar -->
    <div v-if="$slots.toolbar || search || filters.length" class="data-table-toolbar">
      <div class="data-table-toolbar-left">
        <slot name="toolbar" />
      </div>
      <div class="data-table-toolbar-right">
        <!-- Search -->
        <div v-if="search" class="data-table-search">
          <LucideIcon name="search" class="data-table-search-icon" />
          <input
            v-model="searchQuery"
            type="search"
            :placeholder="searchPlaceholder"
            class="data-table-search-input"
            @input="onSearch"
          />
        </div>
        <!-- Filters -->
        <div v-if="filters.length" class="data-table-filters">
          <button
            v-for="f in filters"
            :key="f.key"
            :class="['data-table-filter-btn', { 'is-active': activeFilters.has(f.key) }]"
            @click="toggleFilter(f)"
          >
            <LucideIcon :name="f.icon || 'filter_list'" />
            {{ f.label }}
            <LucideIcon name="expand_more" class="filter-chevron" />
          </button>
        </div>
      </div>
    </div>

    <!-- Table container -->
    <div class="data-table-container">
      <table class="data-table">
        <!-- Header -->
        <thead>
          <tr>
            <th v-if="selectable" class="data-table-th--check">
              <input
                type="checkbox"
                :checked="allSelected"
                class="data-table-checkbox"
                @change="toggleSelectAll"
              />
            </th>
            <th
              v-for="col in columns"
              :key="col.key"
              :class="['data-table-th', col.thClass]"
              :style="col.width ? { width: col.width } : {}"
              @click="col.sortable ? toggleSort(col.key) : null"
            >
              <div class="data-table-th-inner">
                <span>{{ col.label }}</span>
                <span v-if="col.sortable" class="data-table-sort-icon">
                  <LucideIcon v-if="sortKey === col.key && sortOrder === 'asc'" name="expand_less" class="data-table-sort-icon" />
                  <LucideIcon v-else-if="sortKey === col.key && sortOrder === 'desc'" name="expand_more" class="data-table-sort-icon" />
                  <LucideIcon v-else name="unfold_more" class="data-table-sort-icon data-table-sort-inactive" />
                </span>
              </div>
            </th>
            <th v-if="$slots['actions'] || actions.length" class="data-table-th--actions">
              Thao tác
            </th>
          </tr>
        </thead>

        <!-- Body -->
        <tbody>
          <!-- Loading skeleton -->
          <template v-if="loading">
            <tr v-for="i in skeletonRows" :key="`sk-${i}`" class="data-table-row--skeleton">
              <td v-if="selectable" class="data-table-td--check">
                <div class="skeleton skeleton-check" />
              </td>
              <td v-for="col in columns" :key="col.key" class="data-table-td">
                <div class="skeleton" :style="{ width: col.skeletonWidth || '80%' }" />
              </td>
              <td v-if="$slots['actions'] || actions.length" class="data-table-td--actions">
                <div class="skeleton skeleton-actions" />
              </td>
            </tr>
          </template>

          <!-- Empty state -->
          <tr v-else-if="paginatedRows.length === 0">
            <td :colspan="totalCols" class="data-table-empty">
              <slot name="empty">
                <div class="data-table-empty-inner">
                  <LucideIcon name="search_x" class="data-table-empty-icon" />
                  <p class="data-table-empty-title">Không tìm thấy dữ liệu</p>
                  <p v-if="searchQuery" class="data-table-empty-desc">
                    Thử thay đổi từ khóa tìm kiếm
                  </p>
                </div>
              </slot>
            </td>
          </tr>

          <!-- Data rows -->
          <template v-else>
            <tr
              v-for="(row, idx) in paginatedRows"
              :key="rowKey ? row[rowKey] : idx"
              class="data-table-row"
              :class="{
                'is-selected': selectedKeys.has(row[rowKey] || idx),
                'data-table-row--clickable': clickable
              }"
              @click="clickable && $emit('row-click', row)"
            >
              <td v-if="selectable" class="data-table-td--check">
                <input
                  type="checkbox"
                  :checked="selectedKeys.has(row[rowKey] || idx)"
                  class="data-table-checkbox"
                  @click.stop
                  @change="toggleSelect(row, idx)"
                />
              </td>
              <td
                v-for="col in columns"
                :key="col.key"
                :class="['data-table-td', col.tdClass]"
              >
                <slot :name="`cell-${col.key}`" :row="row" :value="row[col.key]">
                  {{ col.formatter ? col.formatter(row[col.key], row) : row[col.key] }}
                </slot>
              </td>
              <td v-if="$slots['actions'] || actions.length" class="data-table-td--actions">
                <slot name="actions" :row="row" />
              </td>
            </tr>
          </template>
        </tbody>
      </table>
    </div>

    <!-- Pagination -->
    <div v-if="pagination && totalPages > 1" class="data-table-pagination">
      <div class="data-table-pagination-info">
        Hiển thị {{ startItem }}–{{ endItem }} trên {{ filteredRows.length }} kết quả
      </div>
      <div class="data-table-pagination-controls">
        <button
          class="data-table-page-btn"
          :disabled="currentPage === 1"
          @click="currentPage = 1"
        >
          <LucideIcon name="first_page" size="18" />
        </button>
        <button
          class="data-table-page-btn"
          :disabled="currentPage === 1"
          @click="currentPage--"
        >
          <LucideIcon name="chevron_left" />
        </button>
        <button
          v-for="p in pageNumbers"
          :key="p"
          class="data-table-page-btn"
          :class="{ 'is-active': p === currentPage, 'is-ellipsis': p === '...' }"
          :disabled="p === '...'"
          @click="p !== '...' && (currentPage = p)"
        >
          {{ p }}
        </button>
        <button
          class="data-table-page-btn"
          :disabled="currentPage === totalPages"
          @click="currentPage++"
        >
          <LucideIcon name="chevron_right" />
        </button>
        <button
          class="data-table-page-btn"
          :disabled="currentPage === totalPages"
          @click="currentPage = totalPages"
        >
          <LucideIcon name="last_page" size="18" />
        </button>
      </div>
    </div>

    <!-- Selected bar -->
    <Transition name="selection-bar">
      <div v-if="selectedKeys.size > 0" class="data-table-selection-bar">
        <LucideIcon name="check_circle" class="selection-bar-icon" size="20" />
        <span><strong>{{ selectedKeys.size }}</strong> mục đã chọn</span>
        <div class="selection-bar-actions">
          <slot name="bulk-actions" :selected="selectedKeys" :clear="clearSelection" />
          <button class="selection-bar-clear" @click="clearSelection">
            <LucideIcon name="x" />
          </button>
        </div>
      </div>
    </Transition>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import LucideIcon from '../common/LucideIcon.vue'

const props = defineProps({
  columns: {
    type: Array,
    required: true
  },
  rows: {
    type: Array,
    default: () => []
  },
  rowKey: {
    type: String,
    default: 'id'
  },
  loading: { type: Boolean, default: false },
  skeletonRows: { type: Number, default: 5 },
  search: { type: Boolean, default: false },
  searchPlaceholder: { type: String, default: 'Tìm kiếm...' },
  searchKeys: {
    type: Array,
    default: null
  },
  filters: {
    type: Array,
    default: () => []
  },
  sortable: { type: Boolean, default: false },
  selectable: { type: Boolean, default: false },
  clickable: { type: Boolean, default: false },
  pagination: { type: Boolean, default: true },
  perPage: { type: Number, default: 10 }
})

const emit = defineEmits(['row-click', 'selection-change', 'sort-change', 'search', 'filter'])

const searchQuery = ref('')
const sortKey = ref('')
const sortOrder = ref('asc')
const currentPage = ref(1)
const selectedKeys = ref(new Set())
const activeFilters = ref(new Set())

const filteredRows = computed(() => {
  let data = [...props.rows]

  if (searchQuery.value) {
    const q = searchQuery.value.toLowerCase()
    const keys = props.searchKeys || props.columns.map(c => c.key)
    data = data.filter(row =>
      keys.some(k => String(row[k] || '').toLowerCase().includes(q))
    )
  }

  if (activeFilters.value.size > 0) {
    data = data.filter(row => {
      for (const [key, value] of activeFilters.value) {
        if (row[key] !== value) return false
      }
      return true
    })
  }

  if (props.sortable && sortKey.value) {
    data.sort((a, b) => {
      const va = a[sortKey.value]
      const vb = b[sortKey.value]
      if (va == null) return 1
      if (vb == null) return -1
      const cmp = String(va).localeCompare(String(vb), 'vi', { numeric: true })
      return sortOrder.value === 'asc' ? cmp : -cmp
    })
  }

  return data
})

const totalPages = computed(() => Math.ceil(filteredRows.value.length / props.perPage))

const paginatedRows = computed(() => {
  if (!props.pagination) return filteredRows.value
  const start = (currentPage.value - 1) * props.perPage
  return filteredRows.value.slice(start, start + props.perPage)
})

const startItem = computed(() => {
  if (!filteredRows.value.length) return 0
  return (currentPage.value - 1) * props.perPage + 1
})
const endItem = computed(() =>
  Math.min(currentPage.value * props.perPage, filteredRows.value.length)
)

const pageNumbers = computed(() => {
  const total = totalPages.value
  const cur = currentPage.value
  if (total <= 7) return Array.from({ length: total }, (_, i) => i + 1)
  const pages = [1]
  if (cur > 3) pages.push('...')
  for (let i = Math.max(2, cur - 1); i <= Math.min(total - 1, cur + 1); i++) pages.push(i)
  if (cur < total - 2) pages.push('...')
  pages.push(total)
  return pages
})

const totalCols = computed(() => {
  let n = props.columns.length
  if (props.selectable) n++
  if (true) n++ // actions column always shown if slot exists
  return n
})

const allSelected = computed(() =>
  paginatedRows.value.length > 0 &&
  paginatedRows.value.every((row, i) => selectedKeys.value.has(row[props.rowKey] ?? i))
)

const onSearch = () => {
  currentPage.value = 1
  emit('search', searchQuery.value)
}

const toggleSort = (key) => {
  if (sortKey.value === key) {
    sortOrder.value = sortOrder.value === 'asc' ? 'desc' : 'asc'
  } else {
    sortKey.value = key
    sortOrder.value = 'asc'
  }
  emit('sort-change', { key: sortKey.value, order: sortOrder.value })
}

const toggleSelect = (row, idx) => {
  const k = row[props.rowKey] ?? idx
  if (selectedKeys.value.has(k)) {
    selectedKeys.value.delete(k)
  } else {
    selectedKeys.value.add(k)
  }
  selectedKeys.value = new Set(selectedKeys.value)
  emit('selection-change', [...selectedKeys.value])
}

const toggleSelectAll = () => {
  if (allSelected.value) {
    paginatedRows.value.forEach((row, i) => {
      selectedKeys.value.delete(row[props.rowKey] ?? i)
    })
  } else {
    paginatedRows.value.forEach((row, i) => {
      selectedKeys.value.add(row[props.rowKey] ?? i)
    })
  }
  selectedKeys.value = new Set(selectedKeys.value)
  emit('selection-change', [...selectedKeys.value])
}

const clearSelection = () => {
  selectedKeys.value = new Set()
  emit('selection-change', [])
}

const toggleFilter = (f) => {
  if (activeFilters.value.has(f.key)) {
    activeFilters.value.delete(f.key)
  } else {
    activeFilters.value.set(f.key, f.value ?? true)
  }
  activeFilters.value = new Map(activeFilters.value)
  currentPage.value = 1
  emit('filter', Object.fromEntries(activeFilters.value))
}

watch(() => props.rows, () => {
  currentPage.value = 1
})
</script>

<style scoped>
/* Wrapper */
.data-table-wrapper {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 0;
}

/* Toolbar */
.data-table-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  padding: 0.875rem 1rem;
  background: var(--glass-surface-muted, #faf7f2);
  border: 1px solid var(--glass-border, rgba(0,0,0,0.07));
  border-bottom: none;
  border-radius: var(--radius-glass, 16px) var(--radius-glass, 16px) 0 0;
  flex-wrap: wrap;
}
.data-table-toolbar-left,
.data-table-toolbar-right {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex-wrap: wrap;
}

/* Search */
.data-table-search {
  position: relative;
  display: flex;
  align-items: center;
}
.data-table-search-icon {
  position: absolute;
  left: 0.625rem;
  font-size: 1.125rem;
  color: var(--glass-text-muted, #a8a29e);
  pointer-events: none;
}
.data-table-search-input {
  padding: 0.5rem 0.75rem 0.5rem 2.25rem;
  background: var(--glass-surface, rgba(255,255,255,0.8));
  border: 1px solid var(--glass-border, rgba(0,0,0,0.07));
  border-radius: var(--radius-glass-sm, 12px);
  font-size: 0.875rem;
  color: var(--glass-text, #1c1917);
  width: 220px;
  outline: none;
  transition: border-color 0.15s, box-shadow 0.15s;
}
.data-table-search-input:focus {
  border-color: var(--glass-amber, #8d4b00);
  box-shadow: 0 0 0 3px var(--glass-amber-soft, rgba(139,75,0,0.1));
}
.data-table-search-input::placeholder { color: var(--glass-text-muted, #a8a29e); }

/* Filters */
.data-table-filters {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  flex-wrap: wrap;
}
.data-table-filter-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  padding: 0.375rem 0.75rem;
  border-radius: var(--radius-glass-pill, 999px);
  border: 1px solid var(--glass-border, rgba(0,0,0,0.07));
  background: var(--glass-surface, rgba(255,255,255,0.8));
  color: var(--glass-text-secondary, #57534e);
  font-size: 0.8rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s;
}
/* empty — LucideIcon handles its own sizing */
.data-table-filter-btn:hover {
  background: var(--glass-amber-soft, rgba(139,75,0,0.08));
  color: var(--glass-amber, #8d4b00);
  border-color: var(--glass-amber-border, rgba(139,75,0,0.25));
}
.data-table-filter-btn.is-active {
  background: var(--glass-amber-soft, rgba(139,75,0,0.1));
  color: var(--glass-amber, #8d4b00);
  border-color: var(--glass-amber-border, rgba(139,75,0,0.35));
}
.filter-chevron {
  font-size: 1rem;
  transition: transform 0.15s;
}

/* Table */
.data-table-container {
  overflow-x: auto;
  border: 1px solid var(--glass-border, rgba(0,0,0,0.07));
  border-radius: 0;
  background: var(--glass-surface-raised, rgba(255,255,255,0.96));
  max-height: min(68vh, calc(100dvh - 10rem));
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.875rem;
}

.data-table thead tr {
  position: sticky;
  top: 0;
  z-index: 2;
}

.data-table-th {
  padding: 0.75rem 1rem;
  text-align: left;
  font-size: 0.72rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.07em;
  color: var(--glass-text-muted, #a8a29e);
  background: var(--glass-surface-muted, #faf7f2);
  border-bottom: 1px solid var(--glass-border, rgba(0,0,0,0.07));
  white-space: nowrap;
  user-select: none;
}
.data-table-th[style] { /* custom width */ }

.data-table-th-inner {
  display: flex;
  align-items: center;
  gap: 0.375rem;
}

.data-table-th--check,
.data-table-td--check {
  width: 2.5rem;
  padding: 0.75rem 0.75rem 0.75rem 1rem;
}

.data-table-th--actions,
.data-table-td--actions {
  width: 120px;
  text-align: right !important;
}

.data-table-sort-icon {
  display: flex;
  align-items: center;
}
/* empty — LucideIcon handles sizing */
.data-table-sort-inactive {
  opacity: 0.35;
}

.data-table-checkbox {
  width: 1.125rem;
  height: 1.125rem;
  cursor: pointer;
  accent-color: var(--glass-amber, #8d4b00);
}

.data-table-row td {
  padding: 0.875rem 1rem;
  border-bottom: 1px solid var(--glass-border, rgba(0,0,0,0.05));
  color: var(--glass-text, #1c1917);
  vertical-align: middle;
  transition: background 0.1s;
}

.data-table-row:last-child td {
  border-bottom: none;
}

.data-table-row:hover td {
  background: rgba(139, 75, 0, 0.04);
}

.data-table-row.is-selected td {
  background: rgba(139, 75, 0, 0.07);
}

.data-table-row--clickable {
  cursor: pointer;
}

/* Skeleton */
.skeleton {
  height: 0.875rem;
  border-radius: 6px;
  background: linear-gradient(
    90deg,
    var(--glass-bg-mid, #efeeea) 25%,
    rgba(255,255,255,0.7) 50%,
    var(--glass-bg-mid, #efeeea) 75%
  );
  background-size: 200% 100%;
  animation: shimmer 1.5s ease-in-out infinite;
}
@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}
.skeleton-check { width: 1.125rem; height: 1.125rem; border-radius: 4px; }
.skeleton-actions { width: 60px; height: 1.5rem; margin-left: auto; }

/* Empty */
.data-table-empty {
  padding: 3rem 2rem;
  text-align: center;
}
.data-table-empty-inner {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.5rem;
}
.data-table-empty-icon {
  font-size: 3rem;
  color: var(--glass-text-muted, #a8a29e);
}
.data-table-empty-title {
  font-family: var(--font-serif, 'Playfair Display', serif);
  font-weight: 700;
  font-size: 1rem;
  color: var(--glass-text, #1c1917);
}
.data-table-empty-desc {
  font-size: 0.875rem;
  color: var(--glass-text-muted, #a8a29e);
}

/* Pagination */
.data-table-pagination {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  padding: 0.875rem 1rem;
  background: var(--glass-surface-muted, #faf7f2);
  border: 1px solid var(--glass-border, rgba(0,0,0,0.07));
  border-top: none;
  border-radius: 0 0 var(--radius-glass, 16px) var(--radius-glass, 16px);
  flex-wrap: wrap;
}
.data-table-pagination-info {
  font-size: 0.8rem;
  color: var(--glass-text-muted, #a8a29e);
  font-weight: 500;
}
.data-table-pagination-controls {
  display: flex;
  align-items: center;
  gap: 0.25rem;
}
.data-table-page-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 2rem;
  height: 2rem;
  padding: 0 0.25rem;
  border-radius: 8px;
  border: 1px solid var(--glass-border, rgba(0,0,0,0.07));
  background: var(--glass-surface, rgba(255,255,255,0.8));
  color: var(--glass-text-secondary, #57534e);
  font-size: 0.8rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s;
}
.data-table-page-btn:hover:not(:disabled) {
  background: var(--glass-amber-soft, rgba(139,75,0,0.08));
  color: var(--glass-amber, #8d4b00);
  border-color: var(--glass-amber-border, rgba(139,75,0,0.25));
}
.data-table-page-btn.is-active {
  background: linear-gradient(135deg, var(--glass-amber, #8d4b00), var(--glass-amber-hover, #6e3900));
  color: white;
  border-color: transparent;
  box-shadow: var(--shadow-glass-sm, 0 2px 8px rgba(139,75,0,0.15));
}
.data-table-page-btn.is-ellipsis {
  border: none;
  background: transparent;
  cursor: default;
}
.data-table-page-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}
/* empty — LucideIcon handles sizing */

/* Selection bar */
.data-table-selection-bar {
  position: fixed;
  bottom: 1.5rem;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.75rem 1.25rem;
  border-radius: var(--radius-glass-pill, 999px);
  background: linear-gradient(135deg, var(--glass-amber, #8d4b00), var(--glass-amber-hover, #6e3900));
  color: white;
  font-size: 0.875rem;
  font-weight: 600;
  box-shadow: var(--shadow-glass-xl, 0 16px 48px rgba(139,75,0,0.3));
  z-index: 100;
  white-space: nowrap;
}
.selection-bar-icon { font-size: 1.25rem; }
.selection-bar-actions {
  display: flex;
  align-items: center;
  gap: 0.375rem;
}
.selection-bar-clear {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 1.75rem;
  height: 1.75rem;
  border: 1px solid rgba(255,255,255,0.4);
  border-radius: 50%;
  background: transparent;
  color: white;
  cursor: pointer;
  transition: background 0.15s;
}
.selection-bar-clear:hover { background: rgba(255,255,255,0.2); }
/* empty — LucideIcon handles sizing */

/* Selection bar transition */
.selection-bar-enter-active,
.selection-bar-leave-active {
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
}
.selection-bar-enter-from,
.selection-bar-leave-to {
  opacity: 0;
  transform: translateX(-50%) translateY(20px) scale(0.95);
}

/* Dark mode */
.dark .data-table-container {
  background: var(--glass-surface-raised, rgba(17,24,39,0.97));
}
.dark .data-table-th {
  background: var(--glass-surface-muted, rgba(10,14,24,0.8));
  border-bottom-color: rgba(255,255,255,0.07);
  color: var(--glass-text-muted, #64748b);
}
.dark .data-table-row td {
  border-bottom-color: rgba(255,255,255,0.04);
  color: var(--glass-text, #f1f5f9);
}
.dark .data-table-row:hover td {
  background: rgba(6,182,212,0.06);
}
.dark .data-table-row.is-selected td {
  background: rgba(6,182,212,0.1);
}
.dark .data-table-toolbar {
  background: var(--glass-surface-muted, rgba(10,14,24,0.8));
  border-color: rgba(255,255,255,0.07);
}
.dark .data-table-search-input {
  background: rgba(17,24,39,0.8);
  border-color: rgba(255,255,255,0.07);
  color: var(--glass-text, #f1f5f9);
}
.dark .data-table-search-input:focus {
  border-color: var(--glass-amber, #f59e0b);
  box-shadow: 0 0 0 3px rgba(245,158,11,0.15);
}
.dark .data-table-filter-btn {
  background: rgba(17,24,39,0.8);
  border-color: rgba(255,255,255,0.07);
  color: var(--glass-text-secondary, #94a3b8);
}
.dark .data-table-filter-btn:hover,
.dark .data-table-filter-btn.is-active {
  background: rgba(245,158,11,0.1);
  color: var(--glass-amber, #f59e0b);
  border-color: rgba(245,158,11,0.3);
}
.dark .data-table-page-btn {
  background: rgba(17,24,39,0.8);
  border-color: rgba(255,255,255,0.07);
  color: var(--glass-text-secondary, #94a3b8);
}
.dark .skeleton {
  background: linear-gradient(90deg, rgba(17,24,39,0.8) 25%, rgba(255,255,255,0.05) 50%, rgba(17,24,39,0.8) 75%);
  background-size: 200% 100%;
}
.dark .data-table-pagination {
  background: var(--glass-surface-muted, rgba(10,14,24,0.8));
  border-color: rgba(255,255,255,0.07);
}
</style>
