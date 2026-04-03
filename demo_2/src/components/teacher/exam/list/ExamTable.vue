<template>
  <div class="eltable">
    <!-- Loading -->
    <div v-if="loading" class="eltable__loading">
      <div class="eltable__spinner">
        <LucideIcon name="progress_activity" />
      </div>
      <p>Đang tải danh sách đề thi...</p>
    </div>

    <!-- Empty -->
    <EmptyState
      v-else-if="!displayExams.length"
      variant="no-data"
      :title="emptyTitle"
      :description="emptySub"
      icon="assignment"
    />

    <!-- Table -->
    <div v-else class="eltable__wrap">
      <table class="eltable__table">
        <thead>
          <tr class="eltable__head">
            <th class="eltable__th eltable__th--check">
              <input
                type="checkbox"
                class="eltable__checkbox"
                :checked="allSelected"
                :indeterminate.prop="someSelected && !allSelected"
                @change="toggleAll"
              />
            </th>
            <th class="eltable__th eltable__th--title">Tiêu đề</th>
            <th class="eltable__th eltable__th--status">Trạng thái</th>
            <th class="eltable__th eltable__th--num">Học sinh</th>
            <th class="eltable__th eltable__th--date">Thời gian</th>
            <th class="eltable__th eltable__th--num eltable__th--right">Câu hỏi</th>
            <th class="eltable__th eltable__th--actions" />
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="exam in displayExams"
            :key="exam.id"
            class="eltable__row"
            :class="{
              'eltable__row--selected': selectedIds.includes(exam.id),
              'eltable__row--archived': exam.isArchived
            }"
            @click="$emit('row-click', exam)"
          >
            <!-- Checkbox -->
            <td class="eltable__td eltable__td--check" @click.stop>
              <input
                type="checkbox"
                class="eltable__checkbox"
                :checked="selectedIds.includes(exam.id)"
                @change="toggle(exam.id)"
              />
            </td>

            <!-- Title -->
            <td class="eltable__td eltable__td--title">
              <div class="eltable__title-cell">
                <p class="eltable__exam-title">{{ exam.title }}</p>
                <div class="eltable__exam-meta">
                  <span v-if="exam.className" class="eltable__badge">
                    <LucideIcon name="school" />
                    {{ exam.className }}
                  </span>
                  <span v-if="exam.code" class="eltable__code">
                    <LucideIcon name="tag" />
                    {{ exam.code }}
                  </span>
                </div>
              </div>
            </td>

            <!-- Status -->
            <td class="eltable__td eltable__td--status">
              <ExamStatusChip :status="getStatus(exam)" :is-archived="exam.isArchived" />
            </td>

            <!-- Students -->
            <td class="eltable__td eltable__td--num">
              <div class="eltable__stat">
                <LucideIcon name="group" />
                <span class="eltable__stat-val">{{ exam.participantCount || 0 }}</span>
              </div>
            </td>

            <!-- Time -->
            <td class="eltable__td eltable__td--date">
              <div v-if="exam.startTime" class="eltable__date-cell">
                <LucideIcon name="schedule" />
                <div>
                  <p class="eltable__date-main">{{ formatDate(exam.startTime) }}</p>
                  <p class="eltable__date-sub">{{ formatTime(exam.startTime) }}</p>
                </div>
              </div>
              <span v-else class="eltable__no-date">—</span>
            </td>

            <!-- Question count -->
            <td class="eltable__td eltable__td--num eltable__td--right">
              <span class="eltable__qcount">{{ exam.questionCount || 0 }}</span>
            </td>

            <!-- Actions -->
            <td class="eltable__td eltable__td--actions" @click.stop>
              <ExamRowActions
                :exam="exam"
                @view="$emit('action', { type: 'view', exam })"
                @edit="$emit('action', { type: 'edit', exam })"
                @duplicate="$emit('action', { type: 'duplicate', exam })"
                @schedule="$emit('action', { type: 'schedule', exam })"
                @publish="$emit('action', { type: 'publish', exam })"
                @unpublish="$emit('action', { type: 'unpublish', exam })"
                @archive="$emit('action', { type: 'archive', exam })"
                @unarchive="$emit('action', { type: 'unarchive', exam })"
                @delete="$emit('action', { type: 'delete', exam })"
              />
            </td>
          </tr>
        </tbody>
      </table>

      <!-- Footer summary -->
      <div class="eltable__footer">
        <span>
          Hiển thị <strong>{{ displayExams.length }}</strong> / <strong>{{ total }}</strong> đề thi
        </span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

import EmptyState from '../../common/EmptyState.vue'
import ExamStatusChip from './ExamStatusChip.vue'
import ExamRowActions from './ExamRowActions.vue'
import LucideIcon from '../../common/LucideIcon.vue'

const props = defineProps({
  exams: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false },
  selectedIds: { type: Array, default: () => [] },
  total: { type: Number, default: 0 },
  emptyTitle: { type: String, default: 'Chưa có đề thi nào' },
  emptySub: { type: String, default: 'Tạo đề thi đầu tiên để bắt đầu' }
})

const emit = defineEmits(['selection-change', 'row-click', 'action'])

const displayExams = computed(() => props.exams)

const allSelected = computed(() =>
  props.exams.length > 0 && props.exams.every(e => props.selectedIds.includes(e.id))
)

const someSelected = computed(() => props.selectedIds.length > 0)

const toggle = (id) => {
  const updated = props.selectedIds.includes(id)
    ? props.selectedIds.filter(i => i !== id)
    : [...props.selectedIds, id]
  emit('selection-change', updated)
}

const toggleAll = () => {
  if (allSelected.value) {
    emit('selection-change', [])
  } else {
    emit('selection-change', props.exams.map(e => e.id))
  }
}

const getStatus = (exam) => {
  if (!exam.isActive) return 'draft'
  const now = new Date()
  const start = exam.startTime ? new Date(exam.startTime) : null
  const end = exam.endTime ? new Date(exam.endTime) : null
  if (start && now < start) return 'upcoming'
  if ((!end || now <= end) && (!start || now >= start)) return 'live'
  return 'ended'
}

const fmt = (d, opts) => {
  try { return new Date(d).toLocaleDateString('vi-VN', opts) } catch { return '—' }
}
const formatDate = (d) => fmt(d, { day: '2-digit', month: '2-digit', year: 'numeric' })
const formatTime = (d) => fmt(d, { hour: '2-digit', minute: '2-digit' })
</script>


<style scoped>
.eltable {
  background: var(--ds-surface);
  border: 1px solid var(--ds-border);
  border-radius: var(--ds-radius-2xl);
  overflow: hidden;
}

.dark .eltable {
  background: var(--ds-gray-800);
  border-color: var(--ds-border-strong);
}

/* Loading */
.eltable__loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1rem;
  padding: 4rem 2rem;
  color: var(--ds-text-muted);
}

.eltable__spinner {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: var(--ds-gray-100);
  display: flex;
  align-items: center;
  justify-content: center;
}

.dark .eltable__spinner { background: var(--ds-gray-700); }

.eltable__spinner-icon {
  font-size: 1.5rem;
  color: var(--ds-primary);
  animation: spin 1s linear infinite;
}

@keyframes spin { to { transform: rotate(360deg); } }

.eltable__loading p {
  font-size: 0.875rem;
  font-weight: 600;
  margin: 0;
}

/* Table */
.eltable__wrap { overflow-x: auto; }

.eltable__table {
  width: 100%;
  border-collapse: collapse;
}

.eltable__head { background: var(--ds-gray-50); }

.dark .eltable__head { background: var(--ds-gray-800); }

.eltable__th {
  padding: 0.875rem 1rem;
  text-align: left;
  font-size: 0.75rem;
  font-weight: 700;
  color: var(--ds-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.06em;
  border-bottom: 1px solid var(--ds-border);
  white-space: nowrap;
}

.dark .eltable__th { border-bottom-color: var(--ds-border-strong); color: #94a3b8; }

.eltable__th--check { width: 48px; padding-left: 1.25rem; padding-right: 0.5rem; }
.eltable__th--title {}
.eltable__th--status { width: 140px; }
.eltable__th--num { width: 90px; text-align: center; }
.eltable__th--date { width: 150px; }
.eltable__th--actions { width: 56px; }
.eltable__th--right { text-align: right; }

.eltable__row {
  border-bottom: 1px solid var(--ds-border);
  cursor: pointer;
  transition: background 0.1s ease;
  overflow: visible;
}

.dark .eltable__row { border-bottom-color: var(--ds-border-strong); }

.eltable__row:last-child { border-bottom: none; }

.eltable__row:hover {
  background: var(--ds-gray-50);
  box-shadow: inset 3px 0 0 var(--ds-primary);
}

.dark .eltable__row:hover {
  background: var(--ds-gray-700);
}

.eltable__row--selected {
  background: rgba(79, 70, 229, 0.04);
}

.dark .eltable__row--selected {
  background: rgba(79, 70, 229, 0.08);
}

.eltable__row--archived { opacity: 0.7; }

.eltable__td {
  padding: 0.875rem 1rem;
  vertical-align: middle;
  overflow: visible;
}

.eltable__td--check { padding-left: 1.25rem; padding-right: 0.5rem; }

.eltable__td--right { text-align: right; }

/* Checkbox */
.eltable__checkbox {
  width: 16px;
  height: 16px;
  accent-color: var(--ds-primary);
  cursor: pointer;
}

/* Title cell */
.eltable__title-cell { display: flex; flex-direction: column; gap: 0.375rem; }

.eltable__exam-title {
  font-size: 0.875rem;
  font-weight: 700;
  color: var(--ds-text);
  margin: 0;
  line-height: 1.3;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.dark .eltable__exam-title { color: #f1f5f9; }

.eltable__exam-meta {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.eltable__badge,
.eltable__code {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  padding: 0.125rem 0.5rem;
  border-radius: var(--ds-radius-full);
  font-size: 0.7rem;
  font-weight: 600;
}

.eltable__badge {
  background: var(--ds-primary-soft);
  color: var(--ds-primary);
}

.eltable__code {
  background: var(--ds-gray-100);
  color: var(--ds-text-muted);
}

.dark .eltable__code { background: var(--ds-gray-700); color: #94a3b8; }


/* Stat */
.eltable__stat {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.375rem;
}

.eltable__stat-icon { font-size: 1rem; color: var(--ds-text-muted); }

.eltable__stat-val {
  font-size: 0.875rem;
  font-weight: 700;
  color: var(--ds-text);
  font-variant-numeric: tabular-nums;
}

.dark .eltable__stat-val { color: #f1f5f9; }

/* Date cell */
.eltable__date-cell { display: flex; align-items: flex-start; gap: 0.5rem; }

.eltable__date-icon {
  font-size: 1rem;
  color: var(--ds-text-muted);
  flex-shrink: 0;
  margin-top: 0.125rem;
}

.eltable__date-main {
  font-size: 0.8rem;
  font-weight: 600;
  color: var(--ds-text);
  margin: 0;
  white-space: nowrap;
}

.dark .eltable__date-main { color: #f1f5f9; }

.eltable__date-sub {
  font-size: 0.7rem;
  color: var(--ds-text-muted);
  margin: 0.125rem 0 0;
  white-space: nowrap;
}

.eltable__no-date { color: var(--ds-text-muted); font-size: 0.875rem; }

/* Q count */
.eltable__qcount {
  font-size: 0.875rem;
  font-weight: 700;
  color: var(--ds-text);
  font-variant-numeric: tabular-nums;
}

.dark .eltable__qcount { color: #f1f5f9; }

/* Footer */
.eltable__footer {
  padding: 0.75rem 1.25rem;
  border-top: 1px solid var(--ds-border);
  font-size: 0.75rem;
  color: var(--ds-text-muted);
  text-align: right;
}

.dark .eltable__footer { border-top-color: var(--ds-border-strong); }

.eltable__footer strong { color: var(--ds-text); }
.dark .eltable__footer strong { color: #f1f5f9; }
</style>
