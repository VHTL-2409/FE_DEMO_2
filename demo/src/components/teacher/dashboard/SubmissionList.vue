<template>
  <div class="td-sub">
    <!-- Header -->
    <div class="td-sub__header">
      <div class="td-sub__header-left">
        <h3 class="td-sub__title">Đề thi gần đây</h3>
        <span class="td-sub__count">{{ exams.length }} đề thi</span>
      </div>
      <div class="td-sub__header-right">
        <!-- Filter tabs -->
        <div class="td-sub__filters">
          <button
            v-for="f in filters"
            :key="f.key"
            type="button"
            :class="['td-sub__filter-tab', activeFilter === f.key && 'td-sub__filter-tab--active']"
            @click="activeFilter = f.key"
          >
            {{ f.label }}
            <span v-if="f.count > 0" class="td-sub__filter-count">{{ f.count }}</span>
          </button>
        </div>
        <button type="button" class="td-sub__viewall" @click="$emit('view-all')">
          Xem tất cả
          <LucideIcon name="arrow_forward" />
        </button>
      </div>
    </div>

    <!-- Table -->
    <div class="td-sub__table-wrap">
      <table class="td-sub__table">
        <thead>
          <tr>
            <th class="td-sub__th">Tiêu đề</th>
            <th class="td-sub__th td-sub__th--hide">Ngày</th>
            <th class="td-sub__th">Trạng thái</th>
            <th class="td-sub__th td-sub__th--hide">Câu hỏi</th>
            <th class="td-sub__th td-sub__th--right">Thao tác</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="!displayExams.length">
            <td colspan="5" class="td-sub__empty">
              <div class="td-sub__empty-inner">
                <LucideIcon name="inbox" />
                <p>Không có đề thi phù hợp</p>
              </div>
            </td>
          </tr>
          <tr
            v-for="exam in displayExams"
            :key="exam.id"
            class="td-sub__row"
            :class="{ 'td-sub__row--clickable': !exam.disabled }"
            @click="!exam.disabled && $emit('view-exam', exam)"
          >
            <td class="td-sub__td">
              <div class="td-sub__exam-info">
                <span class="td-sub__exam-title">{{ exam.title }}</span>
                <span class="td-sub__exam-desc">{{ exam.subtitle || '—' }}</span>
              </div>
            </td>
            <td class="td-sub__td td-sub__td--hide td-sub__td--muted">
              {{ exam.date }}
            </td>
            <td class="td-sub__td">
              <span :class="['td-sub__status', `td-sub__status--${exam.statusKey}`]">
                <span class="td-sub__status-dot" />
                {{ exam.status }}
              </span>
            </td>
            <td class="td-sub__td td-sub__td--hide td-sub__td--muted td-sub__td--mono">
              {{ exam.participants }}
            </td>
            <td class="td-sub__td td-sub__td--right">
              <button
                :disabled="exam.disabled"
                :class="['td-sub__action-btn', exam.disabled ? 'td-sub__action-btn--disabled' : 'td-sub__action-btn--active']"
                type="button"
                @click.stop="$emit('view-exam', exam)"
              >
                <LucideIcon :name="exam.disabled ? 'lock' : 'visibility'" />
                <span>{{ exam.disabled ? 'Đang xử lý' : 'Xem kết quả' }}</span>
              </button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'

const props = defineProps({
  exams: {
    type: Array,
    default: () => []
  }
})

defineEmits(['view-exam', 'view-all', 'create-exam'])

const activeFilter = ref('all')

const filters = computed(() => {
  const all = props.exams.length
  const active = props.exams.filter(e => e.statusKey === 'started').length
  const ended = props.exams.filter(e => e.statusKey === 'ended').length
  return [
    { key: 'all', label: 'Tất cả', count: all },
    { key: 'started', label: 'Đang thi', count: active },
    { key: 'ended', label: 'Đã kết thúc', count: ended }
  ]
})

const displayExams = computed(() => {
  if (activeFilter.value === 'all') return props.exams
  return props.exams.filter(e => e.statusKey === activeFilter.value)
})
</script>


<style scoped>
@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(10px); }
  to   { opacity: 1; transform: translateY(0); }
}

.td-sub {
  display: flex;
  flex-direction: column;
  gap: 0;
  animation: fadeInUp 0.45s cubic-bezier(0.34, 1.2, 0.64, 1) 0.4s both;
}

/* Header */
.td-sub__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  margin-bottom: 1.25rem;
  flex-wrap: wrap;
}

.td-sub__header-left {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.td-sub__title {
  font-family: var(--ds-font-display);
  font-size: 1rem;
  font-weight: 700;
  color: var(--ds-text);
  margin: 0;
}

.dark .td-sub__title {
  color: #f1f5f9;
}

.td-sub__count {
  padding: 0.125rem 0.625rem;
  background: var(--ds-gray-100);
  border-radius: var(--ds-radius-full);
  font-size: 0.7rem;
  font-weight: 600;
  color: var(--ds-text-muted);
}

.dark .td-sub__count {
  background: var(--ds-gray-700);
}

.td-sub__header-right {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.td-sub__filters {
  display: flex;
  gap: 0.125rem;
  padding: 0.25rem;
  background: var(--ds-gray-50);
  border-radius: var(--ds-radius-lg);
  border: 1px solid var(--ds-border);
}

.dark .td-sub__filters {
  background: var(--ds-gray-800);
}

.td-sub__filter-tab {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  padding: 0.375rem 0.75rem;
  border-radius: var(--ds-radius-md);
  border: none;
  background: transparent;
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--ds-text-muted);
  cursor: pointer;
  transition: color 0.15s ease, background-color 0.15s ease, border-color 0.15s ease, box-shadow 0.15s ease, transform 0.15s ease;
}

.td-sub__filter-tab:hover {
  color: var(--ds-text);
  background: var(--ds-gray-200);
  transform: translateY(-1px);
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.06);
}

.dark .td-sub__filter-tab:hover {
  background: var(--ds-gray-700);
}

.td-sub__filter-tab:active {
  transform: translateY(0);
}

.td-sub__filter-tab--active {
  background: white;
  color: var(--ds-primary);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
}

.dark .td-sub__filter-tab--active {
  background: var(--ds-gray-700);
  color: #a5b4fc;
}

.td-sub__filter-count {
  padding: 0.1rem 0.375rem;
  background: var(--ds-gray-200);
  border-radius: var(--ds-radius-full);
  font-size: 0.65rem;
  font-weight: 700;
}

.dark .td-sub__filter-count {
  background: var(--ds-gray-600);
}

.td-sub__filter-tab--active .td-sub__filter-count {
  background: var(--ds-primary-soft);
  color: var(--ds-primary);
}

.td-sub__viewall {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  padding: 0.375rem 0.75rem;
  border-radius: var(--ds-radius-lg);
  border: none;
  background: transparent;
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--ds-primary);
  cursor: pointer;
  transition: color 0.15s ease, background-color 0.15s ease, border-color 0.15s ease, box-shadow 0.15s ease, transform 0.15s ease;
}


.td-sub__viewall:hover {
  background: var(--ds-primary-soft);
}


/* Table */
.td-sub__table-wrap {
  overflow-x: auto;
  border-radius: var(--ds-radius-xl);
  border: 1px solid var(--ds-border);
}

.td-sub__table {
  width: 100%;
  border-collapse: collapse;
}

.td-sub__th {
  padding: 0.75rem 1rem;
  text-align: left;
  font-size: 0.65rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.1em;
  color: var(--ds-text-muted);
  background: var(--ds-gray-50);
  border-bottom: 1px solid var(--ds-border);
  white-space: nowrap;
}

.dark .td-sub__th {
  background: var(--ds-gray-800);
}

.td-sub__th--right {
  text-align: right;
}

.td-sub__th--hide {
  display: none;
}

@media (min-width: 640px) {
  .td-sub__th--hide {
    display: table-cell;
  }
}

.td-sub__row {
  border-bottom: 1px solid var(--ds-border);
  transition: background 0.12s ease;
}

.td-sub__row:last-child {
  border-bottom: none;
}

.td-sub__row--clickable:hover {
  background: var(--ds-gray-50);
}

.dark .td-sub__row--clickable:hover {
  background: var(--ds-gray-800);
}

.td-sub__td {
  padding: 0.875rem 1rem;
  font-size: 0.875rem;
  color: var(--ds-text);
  vertical-align: middle;
}

.td-sub__td--muted {
  color: var(--ds-text-muted);
  font-size: 0.8rem;
}

.td-sub__td--mono {
  font-variant-numeric: tabular-nums;
}

.td-sub__td--right {
  text-align: right;
}

.td-sub__td--hide {
  display: none;
}

@media (min-width: 640px) {
  .td-sub__td--hide {
    display: table-cell;
  }
}

.td-sub__exam-info {
  display: flex;
  flex-direction: column;
  gap: 0.125rem;
}

.td-sub__exam-title {
  font-weight: 600;
  color: var(--ds-text);
  line-height: 1.3;
}

.dark .td-sub__exam-title {
  color: #f1f5f9;
}

.td-sub__exam-desc {
  font-size: 0.75rem;
  color: var(--ds-text-muted);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 200px;
}

/* Status chip */
.td-sub__status {
  display: inline-flex;
  align-items: center;
  gap: 0.375rem;
  padding: 0.25rem 0.625rem;
  border-radius: var(--ds-radius-full);
  font-size: 0.7rem;
  font-weight: 700;
  white-space: nowrap;
}

.td-sub__status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  flex-shrink: 0;
}

.td-sub__status--draft {
  background: var(--ds-gray-100);
  color: var(--ds-text-secondary);
}

.td-sub__status--draft .td-sub__status-dot {
  background: var(--ds-text-muted);
}

.td-sub__status--upcoming {
  background: var(--ds-info-soft);
  color: var(--ds-info);
}

.td-sub__status--upcoming .td-sub__status-dot {
  background: var(--ds-info);
}

.td-sub__status--started {
  background: var(--ds-success-soft);
  color: var(--ds-success);
}

.td-sub__status--started .td-sub__status-dot {
  background: var(--ds-success);
  animation: td-sub-pulse 1.5s ease-in-out infinite;
}

@keyframes td-sub-pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.4; }
}

.td-sub__status--ended {
  background: var(--ds-accent-soft);
  color: var(--ds-accent);
}

.td-sub__status--ended .td-sub__status-dot {
  background: var(--ds-accent);
}

/* Action buttons */
.td-sub__action-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.375rem;
  padding: 0.375rem 0.875rem;
  border-radius: var(--ds-radius-lg);
  font-size: 0.75rem;
  font-weight: 700;
  cursor: pointer;
  transition: color 0.15s ease, background-color 0.15s ease, border-color 0.15s ease, box-shadow 0.15s ease, transform 0.15s ease;
  border: none;
}


.td-sub__action-btn--active {
  background: var(--ds-primary-soft);
  color: var(--ds-primary);
}

.td-sub__action-btn--active:hover {
  background: var(--ds-primary);
  color: white;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(79, 70, 229, 0.25);
}

.td-sub__action-btn--disabled {
  background: var(--ds-gray-100);
  color: var(--ds-text-muted);
  cursor: not-allowed;
}

.dark .td-sub__action-btn--disabled {
  background: var(--ds-gray-700);
  color: var(--ds-gray-500);
}

/* Empty state */
.td-sub__empty {
  padding: 2.5rem 1rem;
}

.td-sub__empty-inner {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.5rem;
  color: var(--ds-text-muted);
  font-size: 0.875rem;
}

.td-sub__empty-icon {
  font-size: 2.5rem;
  opacity: 0.3;
}
</style>
@media (prefers-reduced-motion: reduce) {
  * {
    transition-duration: 0.01ms !important;
    animation-duration: 0.01ms !important;
  }
}