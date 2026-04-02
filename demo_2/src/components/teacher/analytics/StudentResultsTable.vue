<template>
  <div class="srt">
    <!-- Table wrapper -->
    <div class="srt__table-wrapper">
      <table class="srt__table">
        <thead>
          <tr>
            <th class="srt__th srt__th--student">Học sinh</th>
            <th class="srt__th srt__th--center">Trạng thái</th>
            <th class="srt__th srt__th--center">Điểm</th>
            <th class="srt__th srt__th--center">Thời gian</th>
            <th class="srt__th srt__th--center">Cảnh báo</th>
            <th class="srt__th srt__th--actions">Hành động</th>
          </tr>
        </thead>
        <tbody>
          <template v-for="student in sortedStudents" :key="student.id || student.attemptId">
            <tr
              class="srt__row"
              :class="{ 'srt__row--selected': selectedIds.includes(student.id) }"
              @click="$emit('row-click', student)"
            >
              <!-- Student info -->
              <td class="srt__td">
                <div class="srt__student">
                  <div class="srt__avatar" :class="avatarClass(student)">
                    <span v-if="student.userName">{{ student.userName.charAt(0).toUpperCase() }}</span>
                    <span v-else><LucideIcon name="user" /></span>
                  </div>
                  <div class="srt__student-info">
                    <p class="srt__student-name">{{ student.userName || student.fullName || '—' }}</p>
                    <p class="srt__student-email">{{ student.email || student.studentCode || '' }}</p>
                  </div>
                </div>
              </td>

              <!-- Status -->
              <td class="srt__td srt__td--center">
                <span class="srt__status-chip" :class="statusClass(student)">
                  <LucideIcon :name="statusIcon(student)" />
                  {{ statusLabel(student) }}
                </span>
              </td>

              <!-- Score -->
              <td class="srt__td srt__td--center">
                <span class="srt__score" :class="scoreClass(student)">
                  {{ formatScore(student.score) }}
                </span>
              </td>

              <!-- Time -->
              <td class="srt__td srt__td--center">
                <span class="srt__time">{{ formatDuration(student.submittedAt, student.startedAt) }}</span>
              </td>

              <!-- Warnings -->
              <td class="srt__td srt__td--center">
                <span
                  v-if="(student.warningCount || 0) > 0"
                  class="srt__warning-badge"
                  :class="warningBadgeClass(student.warningCount || 0)"
                >
                  <LucideIcon :name="warningIcon(student.warningCount || 0)" />
                  {{ student.warningCount || 0 }}
                </span>
                <span v-else class="srt__warning-badge srt__warning-badge--clean">
                  <LucideIcon name="check" />
                  0
                </span>
              </td>

              <!-- Actions -->
              <td class="srt__td srt__td--actions">
                <div class="srt__actions">
                  <button
                    type="button"
                    class="srt__action-btn"
                    title="Xem chi tiết"
                    @click.stop="$emit('row-click', student)"
                  >
                    <LucideIcon name="visibility" />
                  </button>
                </div>
              </td>
            </tr>
          </template>
        </tbody>
      </table>

      <!-- Empty state -->
      <div v-if="sortedStudents.length === 0" class="srt__empty">
        <LucideIcon name="search_off" />
        <p class="srt__empty-title">Không có kết quả nào</p>
        <p class="srt__empty-desc">{{ searchQuery ? 'Thử đổi từ khóa tìm kiếm' : 'Chưa có dữ liệu học sinh' }}</p>
      </div>
    </div>

    <!-- Pagination -->
    <div v-if="totalPages > 1" class="srt__pagination">
      <span class="srt__pagination-info">
        Hien {{ startItem }}-{{ endItem }} cua {{ totalCount }}
      </span>
      <div class="srt__pagination-controls">
        <button
          type="button"
          class="srt__page-btn"
          :disabled="currentPage <= 1"
          @click="currentPage = Math.max(1, currentPage - 1)"
        >
          <LucideIcon name="chevron_left" />
        </button>
        <button
          v-for="p in pageRange"
          :key="p"
          type="button"
          class="srt__page-btn"
          :class="{ 'srt__page-btn--active': p === currentPage }"
          @click="currentPage = p"
        >
          {{ p }}
        </button>
        <button
          type="button"
          class="srt__page-btn"
          :disabled="currentPage >= totalPages"
          @click="currentPage = Math.min(totalPages, currentPage + 1)"
        >
          <LucideIcon name="chevron_right" />
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'

const props = defineProps({
  students: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false },
  searchQuery: { type: String, default: '' },
  pageSize: { type: Number, default: 15 }
})

defineEmits(['row-click', 'export'])

const selectedIds = ref([])
const currentPage = ref(1)

// Filter
const filteredStudents = computed(() => {
  if (!props.searchQuery.trim()) return props.students
  const q = props.searchQuery.toLowerCase()
  return props.students.filter(s =>
    (s.userName || s.fullName || s.email || s.studentCode || '').toLowerCase().includes(q)
  )
})

// Pagination
const totalCount = computed(() => filteredStudents.value.length)
const totalPages = computed(() => Math.ceil(totalCount.value / props.pageSize))

watch(() => props.searchQuery, () => { currentPage.value = 1 })

const paginatedStudents = computed(() => {
  const start = (currentPage.value - 1) * props.pageSize
  return filteredStudents.value.slice(start, start + props.pageSize)
})

const sortedStudents = computed(() => paginatedStudents.value)

const startItem = computed(() => Math.min((currentPage.value - 1) * props.pageSize + 1, totalCount.value))
const endItem = computed(() => Math.min(currentPage.value * props.pageSize, totalCount.value))

const pageRange = computed(() => {
  const total = totalPages.value
  const current = currentPage.value
  const range = []
  let start = Math.max(1, current - 2)
  let end = Math.min(total, start + 4)
  if (end - start < 4) start = Math.max(1, end - 4)
  for (let i = start; i <= end; i++) range.push(i)
  return range
})

// Helpers
const formatScore = (score) => {
  if (score === null || score === undefined) return '—'
  return Number(score).toFixed(1)
}

const formatDuration = (submittedAt, startedAt) => {
  if (!startedAt) return '—'
  const start = new Date(startedAt)
  const end = submittedAt ? new Date(submittedAt) : new Date()
  const diffMs = end - start
  const mins = Math.floor(diffMs / 60000)
  if (mins < 60) return `${mins} phut`
  const h = Math.floor(mins / 60)
  const m = mins % 60
  return `${h}h ${m}m`
}

const scoreClass = (student) => {
  const s = Number(student.score)
  if (isNaN(s)) return ''
  if (s >= 8) return 'srt__score--excellent'
  if (s >= 5) return 'srt__score--pass'
  return 'srt__score--fail'
}

const statusClass = (student) => {
  const s = String(student.status || '').toUpperCase()
  if (s === 'SUBMITTED' || s === 'COMPLETED') return 'srt__status-chip--submitted'
  if (s === 'PAUSED' || s === 'STOPPED') return 'srt__status-chip--paused'
  if (s === 'ACTIVE' || s === 'IN_PROGRESS') return 'srt__status-chip--active'
  return 'srt__status-chip--unknown'
}

const statusIcon = (student) => {
  const s = String(student.status || '').toUpperCase()
  if (s === 'SUBMITTED' || s === 'COMPLETED') return 'check_circle'
  if (s === 'PAUSED' || s === 'STOPPED') return 'pause_circle'
  if (s === 'ACTIVE' || s === 'IN_PROGRESS') return 'radio_button_checked'
  return 'help'
}

const statusLabel = (student) => {
  const s = String(student.status || '').toUpperCase()
  if (s === 'SUBMITTED' || s === 'COMPLETED') return 'Da nop'
  if (s === 'PAUSED' || s === 'STOPPED') return 'Tạm dừng'
  if (s === 'ACTIVE' || s === 'IN_PROGRESS') return 'Đang thi'
  return 'Không rõ'
}

const avatarClass = (student) => {
  const s = String(student.status || '').toUpperCase()
  if (s === 'SUBMITTED' || s === 'COMPLETED') return 'srt__avatar--submitted'
  if (s === 'PAUSED' || s === 'STOPPED') return 'srt__avatar--paused'
  if (s === 'ACTIVE' || s === 'IN_PROGRESS') return 'srt__avatar--active'
  return 'srt__avatar--unknown'
}

const warningBadgeClass = (count) => {
  if (count >= 5) return 'srt__warning-badge--high'
  if (count >= 1) return 'srt__warning-badge--medium'
  return 'srt__warning-badge--clean'
}

const warningIcon = (count) => {
  if (count >= 5) return 'gpp_bad'
  if (count >= 1) return 'warning'
  return 'check'
}
</script>


<style scoped>
.srt {
  display: flex;
  flex-direction: column;
  gap: 0;
}

/* Table wrapper */
.srt__table-wrapper {
  background: var(--ds-surface);
  border: 1.5px solid var(--ds-border);
  border-radius: var(--ds-radius-2xl);
  overflow: hidden;
  overflow-x: auto;
}

.dark .srt__table-wrapper {
  border-color: var(--ds-border-strong);
}

/* Table */
.srt__table {
  width: 100%;
  border-collapse: collapse;
  min-width: 640px;
}

.srt__th {
  padding: 0.75rem 1rem;
  text-align: left;
  font-size: 0.7rem;
  font-weight: 800;
  color: var(--ds-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.08em;
  border-bottom: 1px solid var(--ds-border);
  background: var(--ds-gray-50);
  white-space: nowrap;
}

.dark .srt__th {
  background: var(--ds-gray-800);
  border-bottom-color: var(--ds-border-strong);
  color: #94a3b8;
}

.srt__th--student { min-width: 180px; }
.srt__th--center { text-align: center; }
.srt__th--actions { text-align: right; }

.srt__td {
  padding: 0.75rem 1rem;
  font-size: 0.875rem;
  border-bottom: 1px solid var(--ds-border);
  vertical-align: middle;
}

.dark .srt__td {
  border-bottom-color: var(--ds-border-strong);
}

.srt__td--center { text-align: center; }
.srt__td--actions { text-align: right; }

/* Row */
.srt__row {
  transition: background 0.1s ease;
  cursor: pointer;
}

.srt__row:hover { background: var(--ds-gray-50); }
.dark .srt__row:hover { background: var(--ds-gray-800); }

.srt__row--selected { background: rgba(79, 70, 229, 0.04); }
.dark .srt__row--selected { background: rgba(79, 70, 229, 0.06); }

/* Student */
.srt__student {
  display: flex;
  align-items: center;
  gap: 0.625rem;
}

.srt__avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.875rem;
  font-weight: 800;
  flex-shrink: 0;
  background: var(--ds-primary-soft);
  color: var(--ds-primary);
}

.dark .srt__avatar { background: rgba(79, 70, 229, 0.15); }

.srt__avatar--active { background: var(--ds-success-soft); color: var(--ds-success); }
.srt__avatar--paused { background: rgba(234, 179, 8, 0.1); color: #d97706; }
.srt__avatar--submitted { background: var(--ds-info-soft); color: var(--ds-info); }
.srt__avatar--unknown { background: var(--ds-gray-100); color: var(--ds-gray-400); }

.dark .srt__avatar--unknown { background: var(--ds-gray-700); }


.srt__student-name {
  font-size: 0.875rem;
  font-weight: 700;
  color: var(--ds-text);
  margin: 0;
}

.dark .srt__student-name { color: #f1f5f9; }

.srt__student-email {
  font-size: 0.7rem;
  color: var(--ds-text-muted);
  margin: 0.125rem 0 0;
}

/* Status chip */
.srt__status-chip {
  display: inline-flex;
  align-items: center;
  gap: 0.375rem;
  padding: 0.3rem 0.75rem;
  border-radius: var(--ds-radius-full);
  font-size: 0.75rem;
  font-weight: 700;
  white-space: nowrap;
}


.srt__status-chip--active { background: var(--ds-success-soft); color: var(--ds-success); }
.srt__status-chip--submitted { background: var(--ds-info-soft); color: var(--ds-info); }
.srt__status-chip--paused { background: rgba(234, 179, 8, 0.1); color: #d97706; }
.srt__status-chip--unknown { background: var(--ds-gray-100); color: var(--ds-gray-500); }

.dark .srt__status-chip--unknown { background: var(--ds-gray-700); }

/* Score */
.srt__score {
  font-family: var(--ds-font-display);
  font-size: 1rem;
  font-weight: 800;
}

.srt__score--excellent { color: var(--ds-success); }
.srt__score--pass { color: var(--ds-primary); }
.srt__score--fail { color: var(--ds-danger); }

/* Time */
.srt__time {
  font-size: 0.8rem;
  color: var(--ds-text-secondary);
  font-weight: 600;
}

/* Warning badge */
.srt__warning-badge {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  padding: 0.25rem 0.625rem;
  border-radius: var(--ds-radius-full);
  font-size: 0.75rem;
  font-weight: 800;
}


.srt__warning-badge--clean { background: var(--ds-success-soft); color: var(--ds-success); }
.srt__warning-badge--medium { background: rgba(234, 179, 8, 0.1); color: #d97706; }
.srt__warning-badge--high { background: var(--ds-danger-soft); color: var(--ds-danger); }

/* Actions */
.srt__actions {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  justify-content: flex-end;
  opacity: 0;
  transition: opacity 0.15s ease;
}

.srt__row:hover .srt__actions {
  opacity: 1;
}

.srt__action-btn {
  width: 2rem;
  height: 2rem;
  border-radius: var(--ds-radius-md);
  border: 1.5px solid var(--ds-border);
  background: var(--ds-surface);
  color: var(--ds-text-muted);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.12s ease;
}

.dark .srt__action-btn { background: var(--ds-gray-800); border-color: var(--ds-border-strong); }

.srt__action-btn:hover {
  background: var(--ds-gray-100);
  color: var(--ds-primary);
  border-color: var(--ds-primary-border);
}

.dark .srt__action-btn:hover { background: var(--ds-gray-700); }


/* Empty */
.srt__empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.5rem;
  padding: 3rem 1rem;
  text-align: center;
}

.srt__empty-icon {
  font-size: 3rem !important;
  color: var(--ds-gray-300);
}

.dark .srt__empty-icon { color: var(--ds-gray-600); }

.srt__empty-title {
  font-size: 0.875rem;
  font-weight: 700;
  color: var(--ds-text);
  margin: 0;
}

.dark .srt__empty-title { color: #f1f5f9; }

.srt__empty-desc {
  font-size: 0.8rem;
  color: var(--ds-text-muted);
  margin: 0;
}

/* Pagination */
.srt__pagination {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.75rem 1rem;
  border-top: 1px solid var(--ds-border);
  background: var(--ds-gray-50);
  flex-wrap: wrap;
  gap: 0.5rem;
}

.dark .srt__pagination {
  border-top-color: var(--ds-border-strong);
  background: var(--ds-gray-800);
}

.srt__pagination-info {
  font-size: 0.8rem;
  color: var(--ds-text-muted);
  font-weight: 600;
}

.srt__pagination-controls {
  display: flex;
  align-items: center;
  gap: 0.25rem;
}

.srt__page-btn {
  min-width: 2rem;
  height: 2rem;
  border-radius: var(--ds-radius-md);
  border: 1px solid var(--ds-border);
  background: var(--ds-surface);
  color: var(--ds-text-secondary);
  font-size: 0.8rem;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.12s ease;
  padding: 0 0.5rem;
  font-family: inherit;
}

.dark .srt__page-btn { background: var(--ds-gray-800); border-color: var(--ds-border-strong); color: #94a3b8; }

.srt__page-btn:hover:not(:disabled) {
  background: var(--ds-gray-100);
  color: var(--ds-text);
}

.dark .srt__page-btn:hover:not(:disabled) { background: var(--ds-gray-700); }

.srt__page-btn--active {
  background: var(--ds-primary);
  border-color: var(--ds-primary);
  color: white;
}

.srt__page-btn--active:hover {
  background: var(--ds-primary);
  color: white;
}

.srt__page-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

</style>
