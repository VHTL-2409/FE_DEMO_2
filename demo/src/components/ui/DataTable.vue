<template>
  <div class="ds-table-wrapper relative overflow-hidden rounded-[var(--ds-radius-xl)] border border-[var(--ds-border)] bg-[var(--ds-surface)]">
    <!-- Loading overlay -->
    <div v-if="loading" class="absolute inset-0 z-10 flex items-center justify-center bg-[var(--ds-surface)]/80 backdrop-blur-sm">
      <div class="flex flex-col items-center gap-3">
        <LucideIcon name="progress_activity" size="24" />
        <p class="text-sm text-[var(--ds-text-muted)]">{{ loadingText }}</p>
      </div>
    </div>

    <!-- Table -->
    <div class="overflow-x-auto">
      <table class="ds-table w-full text-sm" :class="{ 'ds-table--striped': striped }">
        <!-- Header -->
        <thead v-if="columns?.length" class="ds-table-head border-b border-[var(--ds-border)]">
          <tr>
            <th
              v-if="selectable"
              class="w-10 px-4 py-3"
            >
              <input
                type="checkbox"
                class="ds-checkbox rounded"
                :checked="allSelected"
                @change="toggleSelectAll"
              />
            </th>
            <th
              v-for="col in columns"
              :key="col.key"
              class="px-4 py-3 text-left text-xs font-bold uppercase tracking-wider"
              :class="[col.align === 'right' ? 'text-right' : col.align === 'center' ? 'text-center' : '', col.headerClass || '']"
              :style="col.width ? { width: col.width } : {}"
            >
              {{ col.label }}
            </th>
            <th v-if="$slots['row-actions']" class="px-4 py-3 text-right text-xs font-bold uppercase tracking-wider text-[var(--ds-text-muted)]">
              Thao tác
            </th>
          </tr>
        </thead>

        <!-- Body -->
        <tbody class="ds-table-body divide-y divide-[var(--ds-border)]">
          <!-- Empty state -->
          <tr v-if="!loading && (!data || data.length === 0)">
            <td :colspan="columns?.length + ($slots['row-actions'] ? 1 : 0) + (selectable ? 1 : 0)" class="px-4 py-12">
              <slot name="empty">
                <div class="flex flex-col items-center gap-2 text-center">
                  <LucideIcon name="inbox" size="36" />
                  <p class="text-sm font-medium text-[var(--ds-text-muted)]">{{ emptyText }}</p>
                </div>
              </slot>
            </td>
          </tr>

          <!-- Data rows -->
          <tr
            v-for="(row, index) in data"
            :key="rowKey ? (typeof rowKey === 'function' ? rowKey(row) : row[rowKey]) : index"
            class="transition-colors hover:bg-[var(--ds-gray-50)]"
            :class="[onRowClick ? 'cursor-pointer' : '', selectedRows.includes(rowKey ? (typeof rowKey === 'function' ? rowKey(row) : row[rowKey]) : index) ? 'bg-[var(--ds-primary-soft)]' : '']"
            @click="onRowClick && onRowClick(row)"
          >
            <td v-if="selectable" class="px-4 py-3">
              <input
                type="checkbox"
                class="ds-checkbox rounded"
                :checked="selectedRows.includes(rowKey ? (typeof rowKey === 'function' ? rowKey(row) : row[rowKey]) : index)"
                @click.stop
                @change="toggleSelect(row)"
              />
            </td>

            <td
              v-for="col in columns"
              :key="col.key"
              :class="['px-4 py-3 text-[var(--ds-text)]', col.align === 'right' ? 'text-right' : col.align === 'center' ? 'text-center' : '', col.cellClass || '']"
            >
              <slot :name="`cell-${col.key}`" :row="row" :value="row[col.key]">
                {{ col.render ? col.render(row[col.key], row) : row[col.key] }}
              </slot>
            </td>

            <td v-if="$slots['row-actions']" class="px-4 py-3 text-right" @click.stop>
              <slot name="row-actions" :row="row" />
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Footer slot -->
    <div v-if="$slots.footer" class="border-t border-[var(--ds-border)] px-4 py-3">
      <slot name="footer" />
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'

const props = defineProps({
  columns: { type: Array, default: () => [] },
  data: { type: Array, default: () => [] },
  rowKey: { type: [String, Function], default: 'id' },
  loading: { type: Boolean, default: false },
  loadingText: { type: String, default: 'Đang tải...' },
  emptyText: { type: String, default: 'Không có dữ liệu' },
  striped: { type: Boolean, default: false },
  selectable: { type: Boolean, default: false },
  onRowClick: { type: Function, default: null }
})

const emit = defineEmits(['row-click', 'selection-change'])

const selectedRows = ref([])

const allSelected = computed(() =>
  props.data.length > 0 && selectedRows.value.length === props.data.length
)

const getRowId = (row) =>
  typeof props.rowKey === 'function' ? props.rowKey(row) : row[props.rowKey]

const toggleSelect = (row) => {
  const id = getRowId(row)
  const idx = selectedRows.value.indexOf(id)
  if (idx === -1) selectedRows.value.push(id)
  else selectedRows.value.splice(idx, 1)
  emit('selection-change', selectedRows.value)
}

const toggleSelectAll = () => {
  if (allSelected.value) selectedRows.value = []
  else selectedRows.value = props.data.map((_, i) => i)
  emit('selection-change', selectedRows.value)
}
</script>

