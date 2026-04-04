<template>
  <div class="elp">

    <!-- Page Header -->
    <TeacherPageHeader
      icon="assignment"
      icon-color="primary"
      title="Quản lý đề thi"
      subtitle="Tạo, chỉnh sửa và theo dõi các kỳ thi của bạn"
    >
      <template #actions>
        <button type="button" class="elp__btn elp__btn--outline" @click="handleExport">
          <LucideIcon name="download" />
          Xuất danh sách
        </button>
        <button type="button" class="elp__btn elp__btn--primary" @click="goToCreate">
          <LucideIcon name="add" />
          Tạo đề thi mới
        </button>
      </template>
    </TeacherPageHeader>

    <!-- Stats bar -->
    <div class="elp__stats">
      <div class="elp__stat-card">
        <LucideIcon name="assignment" />
        <div>
          <p class="elp__stat-val">{{ allExams.length }}</p>
          <p class="elp__stat-label">Tổng đề thi</p>
        </div>
      </div>
      <div class="elp__stat-card">
        <LucideIcon name="play_circle" />
        <div>
          <p class="elp__stat-val">{{ liveCount }}</p>
          <p class="elp__stat-label">Đang diễn ra</p>
        </div>
      </div>
      <div class="elp__stat-card">
        <LucideIcon name="schedule" />
        <div>
          <p class="elp__stat-val">{{ upcomingCount }}</p>
          <p class="elp__stat-label">Sắp diễn ra</p>
        </div>
      </div>
      <div class="elp__stat-card">
        <LucideIcon name="group" />
        <div>
          <p class="elp__stat-val">{{ totalStudents }}</p>
          <p class="elp__stat-label">Tổng học sinh</p>
        </div>
      </div>
    </div>

    <!-- Toolbar -->
    <ExamListToolbar
      v-model:searchQuery="searchQuery"
      v-model:statusFilter="statusFilter"
      v-model:sortBy="sortBy"
      @reset="resetFilters"
    />

    <!-- Table -->
    <ExamTable
      :exams="paginatedExams"
      :loading="isLoading"
      :selected-ids="selectedIds"
      :total="filteredExams.length"
      :empty-title="isFiltered ? 'Không tìm thấy đề thi' : 'Chưa có đề thi nào'"
      :empty-sub="isFiltered ? 'Thử thay đổi bộ lọc hoặc từ khóa tìm kiếm' : 'Tạo đề thi đầu tiên để bắt đầu'"
      @selection-change="selectedIds = $event"
      @row-click="openDetail"
      @action="handleAction"
    />

    <!-- Pagination -->
    <div v-if="totalPages > 1" class="elp__pagination">
      <button
        type="button"
        class="elp__page-btn"
        :disabled="currentPage === 1"
        @click="currentPage--"
      >
        <LucideIcon name="chevron_left" />
      </button>
      <div class="elp__page-info">
        Trang <strong>{{ currentPage }}</strong> / {{ totalPages }}
      </div>
      <button
        type="button"
        class="elp__page-btn"
        :disabled="currentPage === totalPages"
        @click="currentPage++"
      >
        <LucideIcon name="chevron_right" />
      </button>
    </div>

    <!-- Bulk Action Bar -->
    <BulkActionBar
      :selected="selectedIds"
      :loading="bulkLoading"
      @publish="bulkPublish"
      @archive="bulkArchive"
      @delete="bulkDelete"
      @clear="selectedIds = []"
    />

    <!-- Delete Confirm Modal -->
    <ExamDeleteModal
      v-model="showDeleteModal"
      :exam="examToDelete"
      :count="selectedIds.length"
      :loading="deleteLoading"
      @confirm="confirmDelete"
    />

    <!-- Detail Modal -->
    <ExamDetailModal
      v-model="showDetailModal"
      :exam="activeExam"
      @detail="goToResultAnalysis"
      @edit="editExam"
      @duplicate="duplicateExam"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ApiError } from '../../../../services/apiClient'
import {
  listExams,
  publishExam,
  unpublishExam,
  archiveExam,
  unarchiveExam,
  duplicateExam as duplicateExamApi,
  deleteExam,
  bulkPublishExams,
  bulkArchiveExams,
  bulkDeleteExams,
  exportExamsAsCsv
} from '../../../../services/examService'
import { useToast } from '../../../../composables/useToast'
import LucideIcon from '../../../common/LucideIcon.vue'
import TeacherPageHeader from '../../common/TeacherPageHeader.vue'
import ExamListToolbar from './ExamListToolbar.vue'
import ExamTable from './ExamTable.vue'
import BulkActionBar from './BulkActionBar.vue'
import ExamDeleteModal from './ExamDeleteModal.vue'
import ExamDetailModal from './ExamDetailModal.vue'

const router = useRouter()
const toast = useToast()

// ─── State ───────────────────────────────────────────────────────
const allExams = ref([])
const isLoading = ref(false)
const selectedIds = ref([])
const searchQuery = ref('')
const statusFilter = ref('all')
const sortBy = ref('newest')
const currentPage = ref(1)
const PAGE_SIZE = 10

// Bulk actions
const bulkLoading = ref(false)

// Delete modal
const showDeleteModal = ref(false)
const examToDelete = ref(null)
const deleteLoading = ref(false)
const isBulkDelete = computed(() => !examToDelete.value)

// Detail modal
const showDetailModal = ref(false)
const activeExam = ref(null)

// ─── Computed ───────────────────────────────────────────────────

const liveCount = computed(() =>
  allExams.value.filter(e => {
    if (!e.isActive || e.isArchived) return false
    const now = new Date()
    const s = e.startTime ? new Date(e.startTime) : null
    const en = e.endTime ? new Date(e.endTime) : null
    return ((!en || now <= en) && (!s || now >= s))
  }).length
)

const upcomingCount = computed(() =>
  allExams.value.filter(e => {
    if (!e.isActive || e.isArchived) return false
    const now = new Date()
    const s = e.startTime ? new Date(e.startTime) : null
    return s && now < s
  }).length
)

const totalStudents = computed(() =>
  allExams.value.reduce((sum, e) => sum + (e.participantCount || 0), 0)
)

const isFiltered = computed(() =>
  searchQuery.value || statusFilter.value !== 'all'
)

const filteredExams = computed(() => {
  let list = [...allExams.value]

  // Status filter
  if (statusFilter.value === 'archived') {
    list = list.filter(e => e.isArchived)
  } else if (statusFilter.value !== 'all') {
    list = list.filter(e => {
      if (e.isArchived) return false
      const key = getStatusKey(e)
      return key === statusFilter.value
    })
  }

  // Search filter
  if (searchQuery.value.trim()) {
    const q = searchQuery.value.toLowerCase().trim()
    list = list.filter(e =>
      (e.title || '').toLowerCase().includes(q) ||
      (e.className || '').toLowerCase().includes(q) ||
      (e.code || '').toLowerCase().includes(q) ||
      (e.description || '').toLowerCase().includes(q)
    )
  }

  // Sort
  switch (sortBy.value) {
    case 'upcoming':
      list.sort((a, b) => {
        const at = a.startTime ? new Date(a.startTime).getTime() : Infinity
        const bt = b.startTime ? new Date(b.startTime).getTime() : Infinity
        return at - bt
      })
      break
    case 'recently_updated':
      list.sort((a, b) => new Date(b.updatedAt || 0) - new Date(a.updatedAt || 0))
      break
    case 'most_students':
      list.sort((a, b) => (b.participantCount || 0) - (a.participantCount || 0))
      break
    default: // newest
      list.sort((a, b) => new Date(b.id || 0) - new Date(a.id || 0))
  }

  return list
})

const totalPages = computed(() => Math.max(1, Math.ceil(filteredExams.value.length / PAGE_SIZE)))

const paginatedExams = computed(() => {
  const start = (currentPage.value - 1) * PAGE_SIZE
  return filteredExams.value.slice(start, start + PAGE_SIZE)
})

// ─── Helpers ───────────────────────────────────────────────────

const getStatusKey = (exam) => {
  if (!exam.isActive) return 'draft'
  const now = new Date()
  const start = exam.startTime ? new Date(exam.startTime) : null
  const end = exam.endTime ? new Date(exam.endTime) : null
  if (start && now < start) return 'upcoming'
  if ((!end || now <= end) && (!start || now >= start)) return 'live'
  return 'ended'
}

const formatDateTime = (d) => {
  try {
    return new Date(d).toLocaleString('vi-VN', {
      day: '2-digit', month: '2-digit', year: 'numeric',
      hour: '2-digit', minute: '2-digit'
    })
  } catch { return '—' }
}

// ─── API ───────────────────────────────────────────────────────

const loadExams = async () => {
  isLoading.value = true
  try {
    const data = await listExams()
    allExams.value = data || []
  } catch (err) {
    toast.error(err instanceof ApiError ? err.message : 'Không thể tải danh sách đề thi.')
  } finally {
    isLoading.value = false
  }
}

// ─── Action handlers ────────────────────────────────────────────

const handleAction = async ({ type, exam }) => {
  switch (type) {
    case 'view': openDetail(exam); break
    case 'edit': editExam(exam); break
    case 'duplicate': duplicateExam(exam); break
    case 'schedule': goToSchedule(exam); break
    case 'publish': await doPublish(exam); break
    case 'unpublish': await doUnpublish(exam); break
    case 'archive': await doArchive(exam); break
    case 'unarchive': await doUnarchive(exam); break
    case 'delete': openDeleteConfirm(exam); break
  }
}

const doPublish = async (exam) => {
  try {
    await publishExam(exam.id)
    toast.success(`"${exam.title}" đã được xuất bản.`)
    await loadExams()
  } catch (err) {
    toast.error(err instanceof ApiError ? err.message : 'Không thể xuất bản đề thi.')
  }
}

const doUnpublish = async (exam) => {
  try {
    await unpublishExam(exam.id)
    toast.success(`"${exam.title}" đã bị hủy xuất bản.`)
    await loadExams()
  } catch (err) {
    toast.error(err instanceof ApiError ? err.message : 'Không thể hủy xuất bản.')
  }
}

const doArchive = async (exam) => {
  try {
    await archiveExam(exam.id)
    toast.success(`"${exam.title}" đã được lưu trữ.`)
    await loadExams()
  } catch (err) {
    toast.error(err instanceof ApiError ? err.message : 'Không thể lưu trữ đề thi.')
  }
}

const doUnarchive = async (exam) => {
  try {
    await unarchiveExam(exam.id)
    toast.success(`"${exam.title}" đã được khôi phục.`)
    await loadExams()
  } catch (err) {
    toast.error(err instanceof ApiError ? err.message : 'Không thể khôi phục đề thi.')
  }
}

const duplicateExam = async (exam) => {
  try {
    const created = await duplicateExamApi(exam.id)
    toast.success(`Đã tạo bản sao: "${created.title}"`)
    showDetailModal.value = false
    await loadExams()
  } catch (err) {
    toast.error(err instanceof ApiError ? err.message : 'Không thể nhân bản đề thi.')
  }
}

const editExam = (exam) => {
  showDetailModal.value = false
  router.push({ path: `/teacher/exams/${exam.id}` })
}

const goToResultAnalysis = (exam) => {
  showDetailModal.value = false
  router.push({
    path: '/teacher/exams/review/summary',
    query: { examId: String(exam.id), title: exam.title || '' }
  })
}

const goToSchedule = (exam) => {
  router.push({ path: '/teacher/exams/schedule', query: { examId: exam.id, title: exam.title, mode: 'edit' } })
}

const openDetail = (exam) => {
  activeExam.value = exam
  showDetailModal.value = true
}

const openDeleteConfirm = (exam) => {
  examToDelete.value = exam
  showDeleteModal.value = true
}

const confirmDelete = async () => {
  deleteLoading.value = true
  try {
    if (isBulkDelete.value) {
      await bulkDeleteExams(selectedIds.value)
      toast.success(`Đã xóa ${selectedIds.value.length} đề thi.`)
      selectedIds.value = []
    } else {
      await deleteExam(examToDelete.value.id)
      toast.success(`Đã xóa đề thi "${examToDelete.value.title}".`)
    }
    showDeleteModal.value = false
    examToDelete.value = null
    await loadExams()
  } catch (err) {
    toast.error(err instanceof ApiError ? err.message : 'Không thể xóa đề thi. Vui lòng thử lại.')
  } finally {
    deleteLoading.value = false
  }
}

// Bulk
const bulkPublish = async () => {
  bulkLoading.value = true
  try {
    await bulkPublishExams(selectedIds.value)
    toast.success(`Đã xuất bản ${selectedIds.value.length} đề thi.`)
    selectedIds.value = []
    await loadExams()
  } catch (err) {
    toast.error(err instanceof ApiError ? err.message : 'Không thể xuất bản hàng loạt.')
  } finally {
    bulkLoading.value = false
  }
}

const bulkArchive = async () => {
  bulkLoading.value = true
  try {
    await bulkArchiveExams(selectedIds.value)
    toast.success(`Đã lưu trữ ${selectedIds.value.length} đề thi.`)
    selectedIds.value = []
    await loadExams()
  } catch (err) {
    toast.error(err instanceof ApiError ? err.message : 'Không thể lưu trữ hàng loạt.')
  } finally {
    bulkLoading.value = false
  }
}

const bulkDelete = () => {
  examToDelete.value = null
  showDeleteModal.value = true
}

const handleExport = () => {
  if (filteredExams.value.length === 0) {
    toast.info('Không có đề thi nào để xuất.')
    return
  }
  exportExamsAsCsv(filteredExams.value)
  toast.success('Đã tải file CSV.')
}

const goToCreate = () => router.push('/teacher/exams/create')

const resetFilters = () => {
  searchQuery.value = ''
  statusFilter.value = 'all'
  currentPage.value = 1
}

// Reset page when filters change
const unwatchSearch = computed(() => {
  if (searchQuery.value || statusFilter.value !== 'all') currentPage.value = 1
})

onMounted(() => {
  // Force scroll to top when component mounts
  window.scrollTo({ top: 0, behavior: 'instant' })

  loadExams()
})
</script>


<style scoped>
.elp {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
  max-width: 1280px;
  margin: 0 auto;
  padding: 1.5rem;
}

@media (min-width: 1400px) {
  .elp { max-width: 1500px; }
}

@media (min-width: 1600px) {
  .elp { max-width: 1600px; }
}

/* Header */
.elp__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  flex-wrap: wrap;
}

.elp__header-left {
  display: flex;
  align-items: center;
  gap: 0.875rem;
}

.elp__header-icon {
  width: 48px;
  height: 48px;
  border-radius: var(--ds-radius-xl);
  background: var(--ds-primary-soft);
  color: var(--ds-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}


.elp__header-title {
  font-family: var(--ds-font-display);
  font-size: 1.25rem;
  font-weight: 800;
  color: var(--ds-text);
  margin: 0;
  line-height: 1.2;
}

.dark .elp__header-title { color: var(--ds-text); }

.elp__header-sub {
  font-size: 0.8rem;
  color: var(--ds-text-muted);
  margin: 0.25rem 0 0;
}

.elp__header-actions {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex-wrap: wrap;
}

/* Stats */
.elp__stats {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 0.875rem;
}

@media (max-width: 640px) {
  .elp__stats { grid-template-columns: repeat(2, 1fr); }
}

.elp__stat-card {
  display: flex;
  align-items: center;
  gap: 0.875rem;
  padding: 1rem 1.25rem;
  background: var(--ds-surface);
  border: 1px solid var(--ds-border);
  border-radius: var(--ds-radius-2xl);
}

.dark .elp__stat-card {
  background: var(--ds-gray-800);
  border-color: var(--ds-border-strong);
}

.elp__stat-icon {
  width: 40px;
  height: 40px;
  border-radius: var(--ds-radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  font-size: 1.25rem;
}

.elp__stat-icon--total { background: var(--ds-primary-soft); color: var(--ds-primary); }
.elp__stat-icon--live { background: rgba(245,158,11,0.1); color: var(--ds-warning); }
.elp__stat-icon--upcoming { background: rgba(2,132,199,0.1); color: #0284c7; }
.elp__stat-icon--students { background: rgba(22,163,74,0.1); color: var(--ds-success); }

.elp__stat-val {
  font-size: 1.375rem;
  font-weight: 800;
  color: var(--ds-text);
  margin: 0;
  line-height: 1;
}

.dark .elp__stat-val { color: var(--ds-text); }

.elp__stat-label {
  font-size: 0.7rem;
  color: var(--ds-text-muted);
  margin: 0.25rem 0 0;
  font-weight: 600;
}

/* Buttons */
.elp__btn {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.625rem 1.125rem;
  border-radius: var(--ds-radius-xl);
  font-size: 0.875rem;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.15s ease;
  border: 1.5px solid transparent;
  white-space: nowrap;
}


.elp__btn--primary {
  background: linear-gradient(135deg, var(--ds-primary) 0%, #6366f1 100%);
  color: white;
  box-shadow: 0 4px 12px rgba(79, 70, 229, 0.25);
}

.elp__btn--primary:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 20px rgba(79, 70, 229, 0.35);
}

.elp__btn--outline {
  background: var(--ds-surface);
  border-color: var(--ds-border);
  color: var(--ds-text-secondary);
}

.dark .elp__btn--outline {
  background: var(--ds-gray-800);
  border-color: var(--ds-border-strong);
  color: var(--ds-text-muted);
}

.elp__btn--outline:hover {
  border-color: var(--ds-primary);
  color: var(--ds-primary);
  background: var(--ds-primary-soft);
}

.elp__btn--danger {
  background: var(--ds-danger);
  color: white;
  border-color: var(--ds-danger);
}

.elp__btn--danger:hover { background: #dc2626; }

.elp__btn:disabled { opacity: 0.5; cursor: not-allowed; transform: none !important; }

.elp-spin { animation: spin 1s linear infinite; }

@keyframes spin { to { transform: rotate(360deg); } }

/* Pagination */
.elp__pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 1rem;
}

.elp__page-btn {
  width: 36px;
  height: 36px;
  border-radius: var(--ds-radius-lg);
  border: 1.5px solid var(--ds-border);
  background: var(--ds-surface);
  color: var(--ds-text-secondary);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.15s ease;
}

.dark .elp__page-btn {
  background: var(--ds-gray-800);
  border-color: var(--ds-border-strong);
  color: var(--ds-text-muted);
}

.elp__page-btn:hover:not(:disabled) {
  border-color: var(--ds-primary);
  color: var(--ds-primary);
}

.elp__page-btn:disabled { opacity: 0.4; cursor: not-allowed; }

.elp__page-info {
  font-size: 0.875rem;
  color: var(--ds-text-muted);
  font-weight: 600;
}

.dark .elp__page-info { color: var(--ds-text-muted); }

/* Modal */
.elp-modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,0.4);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: 1.5rem;
}

.elp-modal {
  background: white;
  border-radius: var(--ds-radius-2xl);
  width: 100%;
  max-width: 480px;
  box-shadow: 0 24px 64px rgba(0,0,0,0.2);
  overflow: hidden;
}

.dark .elp-modal { background: var(--ds-gray-800); }

.elp-modal--lg { max-width: 600px; }

.elp-modal__header {
  display: flex;
  align-items: flex-start;
  gap: 0.875rem;
  padding: 1.5rem;
  border-bottom: 1px solid var(--ds-border);
}

.dark .elp-modal__header { border-bottom-color: var(--ds-border-strong); }

.elp-modal__icon {
  width: 44px;
  height: 44px;
  border-radius: var(--ds-radius-xl);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.elp-modal__icon--danger { background: var(--ds-danger-soft); color: var(--ds-danger); }


.elp-modal__title {
  font-family: var(--ds-font-display);
  font-size: 1rem;
  font-weight: 800;
  color: var(--ds-text);
  margin: 0;
  line-height: 1.3;
}

.dark .elp-modal__title { color: var(--ds-text); }

.elp-modal__subtitle {
  font-size: 0.8rem;
  color: var(--ds-text-muted);
  margin: 0.25rem 0 0;
}

.elp-modal__subtitle code {
  font-family: monospace;
  background: var(--ds-gray-100);
  padding: 0.125rem 0.375rem;
  border-radius: 4px;
}

.dark .elp-modal__subtitle code { background: var(--ds-gray-700); }

.elp-modal__close {
  margin-left: auto;
  width: 32px;
  height: 32px;
  border: none;
  background: var(--ds-gray-100);
  color: var(--ds-text-muted);
  border-radius: var(--ds-radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.12s ease;
  flex-shrink: 0;
}

.dark .elp-modal__close { background: var(--ds-gray-700); }

.elp-modal__close:hover { background: var(--ds-gray-200); color: var(--ds-text); }

.dark .elp-modal__close:hover { background: var(--ds-gray-600); }

.elp-modal__body {
  padding: 1.5rem;
  font-size: 0.875rem;
  color: var(--ds-text-secondary);
  line-height: 1.6;
}

.dark .elp-modal__body { color: var(--ds-text-muted); }

.elp-modal__body strong { color: var(--ds-text); }
.dark .elp-modal__body strong { color: var(--ds-text); }

.elp-modal__footer {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 0.75rem;
  padding: 1.25rem 1.5rem;
  border-top: 1px solid var(--ds-border);
  background: var(--ds-gray-50);
}

.dark .elp-modal__footer {
  border-top-color: var(--ds-border-strong);
  background: var(--ds-gray-800);
}

/* Detail grid */
.elp-detail-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
  margin-bottom: 1rem;
}

@media (max-width: 480px) {
  .elp-detail-grid { grid-template-columns: 1fr; }
}

.elp-detail-item {
  display: flex;
  align-items: flex-start;
  gap: 0.75rem;
  padding: 0.875rem 1rem;
  background: var(--ds-gray-50);
  border: 1px solid var(--ds-border);
  border-radius: var(--ds-radius-xl);
}

.dark .elp-detail-item {
  background: var(--ds-gray-700);
  border-color: var(--ds-border-strong);
}


.elp-detail-item__label {
  font-size: 0.7rem;
  font-weight: 700;
  color: var(--ds-text-muted);
  margin: 0;
  text-transform: uppercase;
  letter-spacing: 0.06em;
}

.elp-detail-item__val {
  font-size: 0.875rem;
  font-weight: 700;
  color: var(--ds-text);
  margin: 0.25rem 0 0;
}

.dark .elp-detail-item__val { color: var(--ds-text); }

.elp-detail-desc {
  padding: 1rem;
  background: var(--ds-gray-50);
  border: 1px solid var(--ds-border);
  border-radius: var(--ds-radius-xl);
}

.dark .elp-detail-desc {
  background: var(--ds-gray-700);
  border-color: var(--ds-border-strong);
}

.elp-detail-desc__label {
  font-size: 0.7rem;
  font-weight: 700;
  color: var(--ds-text-muted);
  margin: 0 0 0.375rem;
  text-transform: uppercase;
  letter-spacing: 0.06em;
}

.elp-detail-desc__val {
  font-size: 0.875rem;
  color: var(--ds-text-secondary);
  margin: 0;
  line-height: 1.5;
}

.dark .elp-detail-desc__val { color: var(--ds-text-muted); }

/* Modal transition */
.elp-modal-enter-active,
.elp-modal-leave-active {
  transition: all 0.2s ease;
}

.elp-modal-enter-from,
.elp-modal-leave-to {
  opacity: 0;
}

.elp-modal-enter-from .elp-modal,
.elp-modal-leave-to .elp-modal {
  transform: scale(0.95);
}
</style>
