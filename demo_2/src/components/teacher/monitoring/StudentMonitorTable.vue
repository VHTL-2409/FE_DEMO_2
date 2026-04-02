<template>
  <div class="smt">

    <!-- Top controls bar (sticky) -->
    <div class="smt__controls">
      <!-- Search -->
      <div class="smt__search">
        <LucideIcon name="search" />
        <input
          v-model="searchInput"
          type="text"
          class="smt__search-input"
          placeholder="Tìm học sinh..."
          @input="debouncedSearch"
        />
        <button v-if="searchInput" type="button" class="smt__search-clear" @click="searchInput = ''">
          <LucideIcon name="close" />
        </button>
      </div>

      <!-- Result count -->
      <span class="smt__result-count">
        {{ filteredStudents.length }} học sinh
      </span>

      <!-- Sort -->
      <div class="smt__sort-group">
        <LucideIcon name="sort" />
        <select v-model="sortBy" class="smt__select">
          <option value="risk">Theo rủi ro</option>
          <option value="name">Theo tên</option>
          <option value="status">Theo trạng thái</option>
          <option value="violations">Theo vi phạm</option>
        </select>
      </div>
    </div>

    <!-- Batch actions bar (sticky when active) -->
    <div v-if="selectedIds.length > 0" class="smt__batch-bar">
      <div class="smt__batch-bar-inner">
        <span class="smt__batch-label">
          <LucideIcon name="check_circle" />
          {{ selectedIds.length }} học sinh đã chọn
        </span>
        <div class="smt__batch-actions">
          <button type="button" class="smt__batch-btn" @click="handleBatchWarn">
            <LucideIcon name="warning" />
            Cảnh báo
          </button>
          <button type="button" class="smt__batch-btn" @click="handleBatchPause">
            <LucideIcon name="pause" />
            Tạm dừng
          </button>
          <button type="button" class="smt__batch-btn smt__batch-btn--danger" @click="handleBatchInvalidate">
            <LucideIcon name="do_not_disturb_on" />
            Đình chỉ
          </button>
          <button type="button" class="smt__batch-btn smt__batch-btn--ghost" @click="clearSelection">
            <LucideIcon name="close" />
            Bỏ chọn
          </button>
        </div>
      </div>
    </div>

    <!-- Table -->
    <div class="smt__table-wrapper">
      <table class="smt__table">
        <thead>
          <tr>
            <th class="smt__th smt__th--checkbox">
              <input
                type="checkbox"
                class="smt__checkbox"
                :checked="allSelected"
                :indeterminate.prop="someSelected && !allSelected"
                @change="toggleAll"
              />
            </th>
            <th class="smt__th">Học sinh</th>
            <th class="smt__th">Thiết bị</th>
            <th class="smt__th smt__th--center">Điểm</th>
            <th class="smt__th smt__th--center">Vi phạm</th>
            <th class="smt__th">Rủi ro</th>
            <th class="smt__th">Trạng thái</th>
            <th class="smt__th smt__th--actions">Hành động</th>
          </tr>
        </thead>
        <tbody>
          <template v-for="student in sortedStudents" :key="student.id || student.attemptId">
            <tr
              class="smt__row"
              :class="{
                'smt__row--selected': selectedIds.includes(student.id || student.attemptId),
                [`smt__row--${riskLevel(student)}`]: true
              }"
            >
              <!-- Checkbox -->
              <td class="smt__td smt__td--checkbox">
                <input
                  type="checkbox"
                  class="smt__checkbox"
                  :checked="selectedIds.includes(student.id || student.attemptId)"
                  @change="toggleSelect(student.id || student.attemptId)"
                />
              </td>

              <!-- Student info -->
              <td class="smt__td">
                <div class="smt__student">
                  <!-- Avatar -->
                  <div class="smt__avatar" :class="`smt__avatar--${statusColor(student)}`">
                    <span v-if="student.userName">{{ student.userName.charAt(0).toUpperCase() }}</span>
                    <LucideIcon name="person" v-else />
                  </div>
                  <div class="smt__student-info">
                    <p class="smt__student-name">{{ student.userName || student.fullName || student.email || '—' }}</p>
                    <p class="smt__student-email">{{ student.email || student.studentCode || '' }}</p>
                  </div>
                </div>
              </td>

              <!-- Device status -->
              <td class="smt__td">
                <div class="smt__devices">
                  <!-- Camera -->
                  <span
                    class="smt__device-icon"
                    :class="{ 'smt__device-icon--off': !student.cameraOn }"
                    :title="`Camera: ${student.cameraOn ? 'Bật' : 'Tắt'}`"
                  >
                    <LucideIcon :name="student.cameraOn ? 'videocam' : 'videocam_off'" />
                  </span>
                  <!-- Mic -->
                  <span
                    class="smt__device-icon"
                    :class="{ 'smt__device-icon--off': !student.micOn }"
                    :title="`Micro: ${student.micOn ? 'Bật' : 'Tắt'}`"
                  >
                    <LucideIcon :name="student.micOn ? 'mic' : 'mic_off'" />
                  </span>
                  <!-- Screen share -->
                  <span
                    v-if="student.screenShare"
                    class="smt__device-icon"
                    title="Đang chia sẻ màn hình"
                  >
                    <LucideIcon name="cast" />
                  </span>
                  <!-- Tab count -->
                  <span
                    v-if="student.tabCount && student.tabCount > 1"
                    class="smt__device-badge"
                    :class="{ 'smt__device-badge--warn': student.tabCount > 3 }"
                    title="Số tab đang mở"
                  >
                    {{ student.tabCount }} tabs
                  </span>
                </div>
              </td>

              <!-- Score -->
              <td class="smt__td smt__td--center">
                <span v-if="student.score !== undefined && student.score !== null" class="smt__score">
                  {{ Number(student.score).toFixed(1) }}
                </span>
                <span v-else class="smt__score smt__score--none">—</span>
              </td>

              <!-- Violations -->
              <td class="smt__td smt__td--center">
                <span
                  v-if="student.violationCount > 0 || student.violationCount"
                  class="smt__violation-badge"
                  :class="{
                    'smt__violation-badge--high': (student.violationCount || 0) > 5,
                    'smt__violation-badge--medium': (student.violationCount || 0) > 0 && (student.violationCount || 0) <= 5
                  }"
                >
                  <LucideIcon name="report" />
                  {{ student.violationCount || 0 }}
                </span>
                <span v-else class="smt__violation-badge smt__violation-badge--clean">
                  <LucideIcon name="check" />
                  0
                </span>
              </td>

              <!-- Risk score -->
              <td class="smt__td">
                <div class="smt__risk-cell">
                  <div class="smt__risk-bar">
                    <div
                      class="smt__risk-fill"
                      :class="`smt__risk-fill--${riskLevel(student)}`"
                      :style="{ width: riskPercent(student) + '%' }"
                    />
                  </div>
                  <span class="smt__risk-val" :class="`smt__risk-val--${riskLevel(student)}`">
                    {{ riskScore(student) }}
                  </span>
                </div>
              </td>

              <!-- Status -->
              <td class="smt__td">
                <span class="smt__status-chip" :class="`smt__status-chip--${statusColor(student)}`">
                  <LucideIcon :name="statusIcon(student)" />
                  {{ statusLabel(student) }}
                </span>
              </td>

              <!-- Actions -->
              <td class="smt__td smt__td--actions">
                <div class="smt__actions">
                  <button
                    type="button"
                    class="smt__action-btn"
                    :class="{ 'smt__action-btn--warn': student.violationCount > 0 }"
                    title="Gửi cảnh báo"
                    @click="$emit('warn', student)"
                  >
                    <LucideIcon name="warning" />
                  </button>
                  <button
                    type="button"
                    class="smt__action-btn"
                    :class="{ 'smt__action-btn--danger': student.status !== 'PAUSED' }"
                    title="Tạm dừng"
                    @click="$emit('pause', student)"
                  >
                    <LucideIcon name="pause" />
                  </button>
                  <button
                    type="button"
                    class="smt__action-btn"
                    title="Xem chi tiết"
                    @click="$emit('view-detail', student)"
                  >
                    <LucideIcon name="visibility" />
                  </button>
                </div>
              </td>
            </tr>

            <!-- Expanded row (violations summary) -->
            <tr
              v-if="expandedIds.includes(student.id || student.attemptId)"
              class="smt__expand-row"
            >
              <td colspan="8">
                <div class="smt__expand-content">
                  <div class="smt__expand-section">
                    <h5 class="smt__expand-title">Thông tin thiết bị</h5>
                    <div class="smt__expand-grid">
                      <div class="smt__expand-item">
                        <span class="smt__expand-label">Browser</span>
                        <span>{{ student.browser || '—' }}</span>
                      </div>
                      <div class="smt__expand-item">
                        <span class="smt__expand-label">IP Address</span>
                        <span>{{ student.ipAddress || student.ip || '—' }}</span>
                      </div>
                      <div class="smt__expand-item">
                        <span class="smt__expand-label">Location</span>
                        <span>{{ student.location || student.city || '—' }}</span>
                      </div>
                      <div class="smt__expand-item">
                        <span class="smt__expand-label">Thời gian làm</span>
                        <span>{{ student.timeSpent || student.elapsedTime || '—' }}</span>
                      </div>
                      <div class="smt__expand-item">
                        <span class="smt__expand-label">Câu hỏi đã trả lời</span>
                        <span>{{ student.answeredCount || 0 }} / {{ student.totalQuestions || 0 }}</span>
                      </div>
                      <div class="smt__expand-item">
                        <span class="smt__expand-label">Started at</span>
                        <span>{{ formatTime(student.startedAt || student.startTime) }}</span>
                      </div>
                    </div>
                  </div>
                  <button type="button" class="smt__expand-close" @click="toggleExpand(student.id || student.attemptId)">
                    <LucideIcon name="expand_less" />
                    Thu gọn
                  </button>
                </div>
              </td>
            </tr>
          </template>
        </tbody>
      </table>

      <!-- Empty state -->
      <div v-if="sortedStudents.length === 0" class="smt__empty">
        <LucideIcon name="search_off" />
        <p class="smt__empty-title">
          {{ searchInput ? 'Không tìm thấy học sinh nào' : 'Chưa có học sinh nào' }}
        </p>
        <p class="smt__empty-desc">
          {{ searchInput ? 'Thử thay đổi từ khóa tìm kiếm' : 'Học sinh sẽ xuất hiện khi bắt đầu thi' }}
        </p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'

const props = defineProps({
  students: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false },
  examId: { type: String, default: '' }
})

const emit = defineEmits(['warn', 'pause', 'view-detail', 'batch-warn', 'batch-pause', 'batch-invalidate'])

const searchInput = ref('')
const sortBy = ref('risk')
const selectedIds = ref([])
const expandedIds = ref([])

// Filter
const filteredStudents = computed(() => {
  if (!searchInput.value.trim()) return props.students
  const q = searchInput.value.toLowerCase()
  return props.students.filter(s =>
    (s.userName || s.fullName || s.email || s.studentCode || '').toLowerCase().includes(q)
  )
})

// Sort
const sortedStudents = computed(() => {
  const arr = [...filteredStudents.value]
  switch (sortBy.value) {
    case 'risk':
      return arr.sort((a, b) => (riskScore(b) || 0) - (riskScore(a) || 0))
    case 'name':
      return arr.sort((a, b) => (a.userName || '').localeCompare(b.userName || ''))
    case 'violations':
      return arr.sort((a, b) => (b.violationCount || 0) - (a.violationCount || 0))
    case 'status':
      return arr.sort((a, b) => statusSortVal(a) - statusSortVal(b))
    default:
      return arr
  }
})

// Selection
const allSelected = computed(() =>
  sortedStudents.value.length > 0 &&
  sortedStudents.value.every(s => selectedIds.value.includes(s.id || s.attemptId))
)
const someSelected = computed(() => selectedIds.value.length > 0)

const toggleSelect = (id) => {
  if (selectedIds.value.includes(id)) {
    selectedIds.value = selectedIds.value.filter(i => i !== id)
  } else {
    selectedIds.value = [...selectedIds.value, id]
  }
}

const toggleAll = () => {
  if (allSelected.value) {
    selectedIds.value = []
  } else {
    selectedIds.value = sortedStudents.value.map(s => s.id || s.attemptId)
  }
}

const clearSelection = () => { selectedIds.value = [] }

const toggleExpand = (id) => {
  if (expandedIds.value.includes(id)) {
    expandedIds.value = expandedIds.value.filter(i => i !== id)
  } else {
    expandedIds.value = [...expandedIds.value, id]
  }
}

// Debounce search
let searchTimer = null
const debouncedSearch = () => {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(() => {}, 300)
}

// Risk helpers
const riskScore = (student) => {
  const s = student.riskScore || student.risk || 0
  return Math.min(100, Math.max(0, Number(s)))
}

const riskLevel = (student) => {
  const s = riskScore(student)
  if (s >= 81) return 'critical'
  if (s >= 61) return 'high_risk'
  if (s >= 31) return 'suspicious'
  return 'clean'
}

const riskPercent = (student) => riskScore(student)

// Status helpers
const statusColor = (student) => {
  if (student.status === 'SUBMITTED' || student.status === 'COMPLETED') return 'submitted'
  if (student.status === 'PAUSED') return 'paused'
  if (student.status === 'OFFLINE' || !student.isOnline) return 'offline'
  if (student.isOnline || student.status === 'IN_PROGRESS' || student.status === 'ACTIVE') return 'online'
  return 'unknown'
}

const statusIcon = (student) => {
  const map = { online: 'radio_button_checked', offline: 'radio_button_unchecked', paused: 'pause_circle', submitted: 'check_circle', unknown: 'help' }
  return map[statusColor(student)] || 'help'
}

const statusLabel = (student) => {
  const map = {
    online: 'Đang thi',
    offline: 'Offline',
    paused: 'Tạm dừng',
    submitted: 'Đã nộp',
    unknown: 'Không rõ'
  }
  return map[statusColor(student)] || '—'
}

const statusSortVal = (student) => {
  const map = { online: 0, paused: 1, offline: 2, submitted: 3, unknown: 4 }
  return map[statusColor(student)] ?? 5
}

// Batch actions
const handleBatchWarn = () => {
  emit('batch-warn', selectedIds.value)
  clearSelection()
}

const handleBatchPause = () => {
  emit('batch-pause', selectedIds.value)
  clearSelection()
}

const handleBatchInvalidate = () => {
  emit('batch-invalidate', selectedIds.value)
  clearSelection()
}

// Format time
const formatTime = (ts) => {
  if (!ts) return '—'
  try {
    return new Date(ts).toLocaleString('vi-VN', {
      day: '2-digit', month: '2-digit', hour: '2-digit', minute: '2-digit'
    })
  } catch { return '—' }
}
</script>


<style scoped>
.smt {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

/* Controls */
.smt__controls {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.smt__search {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex: 1;
  min-width: 180px;
  max-width: 280px;
  padding: 0.5rem 0.875rem;
  background: var(--ds-surface);
  border: 1.5px solid var(--ds-border);
  border-radius: var(--ds-radius-xl);
  transition: all 0.15s ease;
}

.dark .smt__search {
  background: var(--ds-gray-800);
  border-color: var(--ds-border-strong);
}

.smt__search:focus-within {
  border-color: var(--ds-primary);
  box-shadow: 0 0 0 3px var(--ds-primary-ring);
}

.smt__search-icon {
  font-size: 1.125rem;
  color: var(--ds-text-muted);
  flex-shrink: 0;
}

.smt__search-input {
  flex: 1;
  border: none;
  background: transparent;
  outline: none;
  font-size: 0.875rem;
  color: var(--ds-text);
  min-width: 0;
}

.dark .smt__search-input { color: #f1f5f9; }
.smt__search-input::placeholder { color: var(--ds-text-muted); }

.smt__search-clear {
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
  flex-shrink: 0;
}

.dark .smt__search-clear { background: var(--ds-gray-700); }
.smt__search-clear:hover { background: var(--ds-gray-200); color: var(--ds-text); }
.dark .smt__search-clear:hover { background: var(--ds-gray-600); }

.smt__result-count {
  font-size: 0.8rem;
  color: var(--ds-text-muted);
  font-weight: 600;
  white-space: nowrap;
}

/* Batch actions bar (sticky) */
.smt__batch-bar {
  position: sticky;
  top: 0;
  z-index: 10;
  padding: 0.5rem 1rem;
  background: rgba(79, 70, 229, 0.08);
  border: 1.5px solid var(--ds-primary-border);
  border-radius: var(--ds-radius-xl);
  backdrop-filter: blur(8px);
  animation: smtSlideDown 0.2s ease;
}

.dark .smt__batch-bar {
  background: rgba(79, 70, 229, 0.12);
  border-color: var(--ds-primary-border);
}

@keyframes smtSlideDown {
  from { opacity: 0; transform: translateY(-8px); }
  to { opacity: 1; transform: translateY(0); }
}

.smt__batch-bar-inner {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.smt__batch-label {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  font-size: 0.8rem;
  font-weight: 800;
  color: var(--ds-primary);
  white-space: nowrap;
}


.smt__batch-actions {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.smt__batch-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.375rem;
  padding: 0.375rem 0.75rem;
  border-radius: var(--ds-radius-full);
  border: 1.5px solid var(--ds-border);
  background: var(--ds-surface);
  color: var(--ds-text-secondary);
  font-size: 0.75rem;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.15s ease;
  font-family: inherit;
}

.dark .smt__batch-btn { background: var(--ds-gray-800); border-color: var(--ds-border-strong); color: #94a3b8; }
.smt__batch-btn:hover { border-color: var(--ds-primary); color: var(--ds-primary); background: rgba(79, 70, 229, 0.08); }
.dark .smt__batch-btn:hover { background: rgba(79, 70, 229, 0.1); }

.smt__batch-btn--danger { border-color: rgba(220, 38, 38, 0.3); color: var(--ds-danger); }
.smt__batch-btn--danger:hover { background: var(--ds-danger-soft); border-color: var(--ds-danger); }
.dark .smt__batch-btn--danger { color: #ef4444; }
.dark .smt__batch-btn--danger:hover { background: rgba(220, 38, 38, 0.1); }

/* Sort */
.smt__sort-group {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-left: auto;
}

.smt__sort-icon {
  font-size: 1rem;
  color: var(--ds-text-muted);
}

.smt__select {
  padding: 0.5rem 2rem 0.5rem 0.75rem;
  background: var(--ds-surface);
  border: 1.5px solid var(--ds-border);
  border-radius: var(--ds-radius-xl);
  font-size: 0.8rem;
  font-weight: 600;
  color: var(--ds-text-secondary);
  outline: none;
  cursor: pointer;
  appearance: none;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 12 12'%3E%3Cpath fill='%2364748b' d='M2 4l4 4 4-4'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 0.625rem center;
  background-color: var(--ds-surface);
}

.dark .smt__select {
  background-color: var(--ds-gray-800);
  border-color: var(--ds-border-strong);
  color: #94a3b8;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 12 12'%3E%3Cpath fill='%2394a3b8' d='M2 4l4 4 4-4'/%3E%3C/svg%3E");
}

.smt__select:focus {
  border-color: var(--ds-primary);
  box-shadow: 0 0 0 3px var(--ds-primary-ring);
}

/* Table wrapper */
.smt__table-wrapper {
  background: var(--ds-surface);
  border: 1.5px solid var(--ds-border);
  border-radius: var(--ds-radius-2xl);
  overflow: hidden;
  overflow-x: auto;
}

.dark .smt__table-wrapper {
  border-color: var(--ds-border-strong);
}

/* Table */
.smt__table {
  width: 100%;
  border-collapse: collapse;
  min-width: 700px;
}

.smt__th {
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

.dark .smt__th {
  background: var(--ds-gray-800);
  border-bottom-color: var(--ds-border-strong);
  color: #94a3b8;
}

.smt__th--checkbox { width: 2.5rem; padding: 0.75rem 0.5rem; }
.smt__th--center { text-align: center; }
.smt__th--actions { text-align: right; }

.smt__td {
  padding: 0.75rem 1rem;
  font-size: 0.875rem;
  border-bottom: 1px solid var(--ds-border);
  vertical-align: middle;
}

.dark .smt__td {
  border-bottom-color: var(--ds-border-strong);
}

.smt__td--checkbox { width: 2.5rem; padding: 0.75rem 0.5rem; }
.smt__td--center { text-align: center; }
.smt__td--actions { text-align: right; }

/* Row */
.smt__row {
  transition: background 0.1s ease;
  border-left: 3px solid transparent;
}

.smt__row:hover { background: var(--ds-gray-50); }
.dark .smt__row:hover { background: var(--ds-gray-800); }

.smt__row--selected { background: rgba(79, 70, 229, 0.04); }
.dark .smt__row--selected { background: rgba(79, 70, 229, 0.06); }

.smt__row--critical { border-left-color: var(--ds-danger); }
.smt__row--high_risk { border-left-color: #d97706; }
.smt__row--suspicious { border-left-color: #eab308; }
.smt__row--clean { border-left-color: transparent; }

/* Checkbox */
.smt__checkbox {
  width: 16px;
  height: 16px;
  cursor: pointer;
  accent-color: var(--ds-primary);
}

/* Student */
.smt__student {
  display: flex;
  align-items: center;
  gap: 0.625rem;
}

.smt__avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.8rem;
  font-weight: 800;
  flex-shrink: 0;
  background: var(--ds-primary-soft);
  color: var(--ds-primary);
}

.dark .smt__avatar { background: rgba(79, 70, 229, 0.15); }

.smt__avatar--online { background: var(--ds-success-soft); color: var(--ds-success); }
.smt__avatar--offline { background: var(--ds-gray-100); color: var(--ds-gray-400); }
.dark .smt__avatar--offline { background: var(--ds-gray-700); }
.smt__avatar--paused { background: rgba(234, 179, 8, 0.1); color: #d97706; }
.smt__avatar--submitted { background: var(--ds-info-soft); color: var(--ds-info); }


.smt__student-info {}
.smt__student-name {
  font-size: 0.875rem;
  font-weight: 700;
  color: var(--ds-text);
  margin: 0;
}

.dark .smt__student-name { color: #f1f5f9; }

.smt__student-email {
  font-size: 0.7rem;
  color: var(--ds-text-muted);
  margin: 0.125rem 0 0;
}

/* Devices */
.smt__devices {
  display: flex;
  align-items: center;
  gap: 0.375rem;
}

.smt__device-icon {
  width: 24px;
  height: 24px;
  border-radius: var(--ds-radius-sm);
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--ds-success-soft);
  color: var(--ds-success);
  flex-shrink: 0;
}

.smt__device-icon--off {
  background: var(--ds-gray-100);
  color: var(--ds-gray-400);
}

.dark .smt__device-icon--off { background: var(--ds-gray-700); }


.smt__device-badge {
  padding: 0.15rem 0.5rem;
  border-radius: var(--ds-radius-full);
  font-size: 0.65rem;
  font-weight: 700;
  background: var(--ds-warning-soft);
  color: var(--ds-warning);
}

.smt__device-badge--warn {
  background: var(--ds-danger-soft);
  color: var(--ds-danger);
}

/* Score */
.smt__score {
  font-family: var(--ds-font-display);
  font-size: 1rem;
  font-weight: 800;
  color: var(--ds-text);
}

.dark .smt__score { color: #f1f5f9; }

.smt__score--none { color: var(--ds-text-muted); font-weight: 400; }

/* Violations */
.smt__violation-badge {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  padding: 0.25rem 0.625rem;
  border-radius: var(--ds-radius-full);
  font-size: 0.75rem;
  font-weight: 800;
}


.smt__violation-badge--clean { background: var(--ds-success-soft); color: var(--ds-success); }
.smt__violation-badge--medium { background: rgba(234, 179, 8, 0.1); color: #d97706; }
.smt__violation-badge--high { background: var(--ds-danger-soft); color: var(--ds-danger); }

/* Risk */
.smt__risk-cell {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  min-width: 100px;
}

.smt__risk-bar {
  flex: 1;
  height: 6px;
  background: var(--ds-gray-200);
  border-radius: 3px;
  overflow: hidden;
}

.dark .smt__risk-bar { background: var(--ds-gray-700); }

.smt__risk-fill {
  height: 100%;
  border-radius: 3px;
  transition: width 0.4s ease;
}

.smt__risk-fill--clean { background: var(--ds-success); }
.smt__risk-fill--suspicious { background: #eab308; }
.smt__risk-fill--high_risk { background: #d97706; }
.smt__risk-fill--critical { background: var(--ds-danger); }

.smt__risk-val {
  font-size: 0.75rem;
  font-weight: 800;
  font-family: var(--ds-font-display);
  min-width: 24px;
  text-align: right;
}

.smt__risk-val--clean { color: var(--ds-success); }
.smt__risk-val--suspicious { color: #d97706; }
.smt__risk-val--high_risk { color: var(--ds-danger); }
.smt__risk-val--critical { color: var(--ds-danger); }

/* Status chip */
.smt__status-chip {
  display: inline-flex;
  align-items: center;
  gap: 0.375rem;
  padding: 0.3rem 0.75rem;
  border-radius: var(--ds-radius-full);
  font-size: 0.75rem;
  font-weight: 700;
  white-space: nowrap;
}


.smt__status-chip--online { background: var(--ds-success-soft); color: var(--ds-success); }
.smt__status-chip--offline { background: var(--ds-gray-100); color: var(--ds-gray-500); }
.dark .smt__status-chip--offline { background: var(--ds-gray-700); }
.smt__status-chip--paused { background: rgba(234, 179, 8, 0.1); color: #d97706; }
.smt__status-chip--submitted { background: var(--ds-info-soft); color: var(--ds-info); }
.smt__status-chip--unknown { background: var(--ds-gray-100); color: var(--ds-gray-500); }
.dark .smt__status-chip--unknown { background: var(--ds-gray-700); }

/* Actions */
.smt__actions {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  opacity: 0;
  transition: opacity 0.15s ease;
}

.smt__row:hover .smt__actions {
  opacity: 1;
}

.smt__action-btn {
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

.dark .smt__action-btn { background: var(--ds-gray-800); border-color: var(--ds-border-strong); }

.smt__action-btn:hover {
  background: var(--ds-gray-100);
  color: var(--ds-text);
  border-color: var(--ds-gray-300);
}

.dark .smt__action-btn:hover { background: var(--ds-gray-700); border-color: var(--ds-gray-600); }

.smt__action-btn--warn:hover {
  background: rgba(234, 179, 8, 0.1);
  color: #d97706;
  border-color: rgba(234, 179, 8, 0.3);
}

.smt__action-btn--danger:hover {
  background: rgba(234, 179, 8, 0.1);
  color: #d97706;
  border-color: rgba(234, 179, 8, 0.3);
}

.dark .smt__action-btn--warn:hover,
.dark .smt__action-btn--danger:hover { background: rgba(234, 179, 8, 0.1); }


/* Expand row */
.smt__expand-row td {
  padding: 0;
  border-bottom: 2px solid var(--ds-primary-border);
  background: rgba(79, 70, 229, 0.02);
}

.dark .smt__expand-row td { background: rgba(79, 70, 229, 0.04); }

.smt__expand-content {
  padding: 1rem 1.25rem;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  animation: smtExpand 0.2s ease;
}

@keyframes smtExpand {
  from { opacity: 0; transform: translateY(-4px); }
  to { opacity: 1; transform: translateY(0); }
}

.smt__expand-title {
  font-size: 0.75rem;
  font-weight: 800;
  color: var(--ds-text);
  text-transform: uppercase;
  letter-spacing: 0.08em;
  margin: 0 0 0.5rem;
}

.dark .smt__expand-title { color: #f1f5f9; }

.smt__expand-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
  gap: 0.625rem;
}

.smt__expand-item {
  display: flex;
  flex-direction: column;
  gap: 0.125rem;
}

.smt__expand-label {
  font-size: 0.65rem;
  font-weight: 700;
  color: var(--ds-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.06em;
}

.smt__expand-item > span:last-child {
  font-size: 0.8rem;
  font-weight: 600;
  color: var(--ds-text);
}

.dark .smt__expand-item > span:last-child { color: #f1f5f9; }

.smt__expand-close {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  font-size: 0.75rem;
  font-weight: 700;
  color: var(--ds-text-muted);
  background: transparent;
  border: none;
  cursor: pointer;
  transition: color 0.12s ease;
  padding: 0.25rem;
  font-family: inherit;
  align-self: flex-start;
}

.smt__expand-close:hover { color: var(--ds-primary); }

/* Empty */
.smt__empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.5rem;
  padding: 3rem 1rem;
  text-align: center;
}

.smt__empty-icon { font-size: 3rem !important; color: var(--ds-gray-300); }
.dark .smt__empty-icon { color: var(--ds-gray-600); }

.smt__empty-title {
  font-size: 0.875rem;
  font-weight: 700;
  color: var(--ds-text);
  margin: 0;
}

.dark .smt__empty-title { color: #f1f5f9; }

.smt__empty-desc {
  font-size: 0.8rem;
  color: var(--ds-text-muted);
  margin: 0;
}
</style>
