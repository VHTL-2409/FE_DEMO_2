<template>
  <div class="cmp">
    <!-- Page Header -->
    <TeacherPageHeader
      icon="groups"
      icon-color="primary"
      title="Quản lý lớp học"
    >
      <template #actions>
        <button type="button" class="cmp__btn cmp__btn--primary tui-btn tui-btn--primary" @click="openCreateModal">
          <LucideIcon name="add" />
          Tạo lớp mới
        </button>
      </template>
    </TeacherPageHeader>

    <div class="cmp__overview">
      <div class="cmp__overview-item">
        <span class="cmp__overview-label">Lớp học</span>
        <strong class="cmp__overview-value">{{ allClasses.length }}</strong>
      </div>
      <div class="cmp__overview-item">
        <span class="cmp__overview-label">Tổng học sinh</span>
        <strong class="cmp__overview-value">{{ totalStudents }}</strong>
      </div>
      <div class="cmp__overview-item">
        <span class="cmp__overview-label">Trung bình mỗi lớp</span>
        <strong class="cmp__overview-value">{{ averageStudents }}</strong>
      </div>
    </div>

    <!-- Search & Filter Bar -->
    <div class="cmp__toolbar">
      <div class="cmp__search">
        <LucideIcon name="search" class="cmp__search-icon" />
        <input
          v-model="searchQuery"
          type="text"
          class="cmp__search-input"
          aria-label="Tìm lớp theo tên hoặc môn học"
          placeholder="Tìm theo tên lớp hoặc môn học"
          @input="currentPage = 1"
        />
        <button v-if="searchQuery" type="button" class="cmp__search-clear" aria-label="Xóa từ khóa tìm kiếm" @click="searchQuery = ''">
          <LucideIcon name="x" />
        </button>
      </div>
      <div class="cmp__toolbar-right">
        <span class="cmp__result-count">
          {{ filteredClasses.length }} lớp
        </span>
      </div>
    </div>

    <!-- Loading State -->
    <div v-if="isLoading" class="cmp__loading">
      <div class="cmp__spinner"></div>
      <p>Đang tải danh sách lớp học...</p>
    </div>

    <!-- Empty State -->
    <EmptyState
      v-if="showEmptyState"
      :variant="loadError ? 'no-data' : (searchQuery ? 'no-results' : 'no-data')"
      :title="emptyStateTitle"
      :description="emptyStateDescription"
      :action-label="emptyStateActionLabel"
      :action-icon="emptyStateActionIcon"
      @action="handleEmptyStateAction"
    />

    <!-- Classes Grid -->
    <div v-else class="cmp__grid">
      <TransitionGroup name="card">
        <div
          v-for="cls in paginatedClasses"
          :key="cls.id"
          class="cmp__card"
        >
          <button
            type="button"
            class="cmp__card-main"
            :aria-label="`Mở chi tiết lớp ${cls.name || ''}`"
            @click="openClassDetail(cls)"
          >
            <div class="cmp__card-header">
              <div class="cmp__card-heading">
                <div class="cmp__card-title-row">
                  <div class="cmp__card-icon" aria-hidden="true">
                    <LucideIcon :name="cls.subject ? 'book-open' : 'graduation-cap'" />
                  </div>
                  <div>
                    <h3 class="cmp__card-title">{{ cls.name }}</h3>
                    <p v-if="cls.subject" class="cmp__card-subtitle">{{ cls.subject }}</p>
                  </div>
                </div>
              </div>
            </div>
            <div class="cmp__card-body">
              <div class="cmp__card-meta">
                <span v-if="cls.classCode" class="cmp__card-chip">
                  <LucideIcon name="key-round" />
                  {{ cls.classCode }}
                </span>
                <span class="cmp__card-chip cmp__card-chip--muted">
                  <LucideIcon name="users" />
                  {{ cls.studentCount || 0 }} học sinh
                </span>
              </div>
              <div class="cmp__card-inline">
                <span class="cmp__card-inline-label">Cập nhật</span>
                <span class="cmp__card-inline-value">{{ formatDate(cls.updatedAt) }}</span>
              </div>
            </div>
          </button>
          <div class="cmp__card-actions">
            <div class="cmp__card-actions-inner">
              <button type="button" class="cmp__action-btn" @click="openEditModal(cls)" title="Sửa lớp">
                <span class="sr-only">Sửa lớp {{ cls.name }}</span>
                <LucideIcon name="pencil" />
              </button>
              <button type="button" class="cmp__action-btn cmp__action-btn--danger" @click="confirmDelete(cls)" title="Xóa lớp">
                <span class="sr-only">Xóa lớp {{ cls.name }}</span>
                <LucideIcon name="trash-2" />
              </button>
            </div>
          </div>
        </div>
      </TransitionGroup>
    </div>

    <!-- Pagination -->
    <div v-if="totalPages > 1" class="cmp__pagination">
      <button
        type="button"
        class="cmp__page-btn"
        aria-label="Trang trước"
        :disabled="currentPage === 1"
        @click="currentPage--"
      >
        <LucideIcon name="chevron_left" />
      </button>
      <div class="cmp__page-numbers">
        <button
          v-for="page in visiblePages"
          :key="page"
          type="button"
          class="cmp__page-num"
          :class="{ 'cmp__page-num--active': page === currentPage }"
          @click="currentPage = page"
        >
          {{ page }}
        </button>
      </div>
      <button
        type="button"
        class="cmp__page-btn"
        aria-label="Trang sau"
        :disabled="currentPage === totalPages"
        @click="currentPage++"
      >
        <LucideIcon name="chevron_right" />
      </button>
    </div>

    <!-- Create/Edit Modal -->
    <ClassFormModal
      v-model="showFormModal"
      :class-item="selectedClass"
      :loading="formLoading"
      @submit="handleSubmit"
    />

    <!-- Class Created Success Modal -->
    <Transition name="modal">
      <div v-if="showSuccessModal" class="cmp__modal-overlay" @click.self="showSuccessModal = false">
        <div class="cmp__modal cmp__modal--success" role="dialog" aria-modal="true" aria-labelledby="cmp-success-title">
          <div class="cmp__modal__icon-wrap cmp__modal__icon-wrap--success">
            <LucideIcon name="check-circle" />
          </div>
          <h3 id="cmp-success-title" class="cmp__modal__title">Tạo lớp thành công!</h3>
          <p class="cmp__modal__desc">Chia sẻ mã lớp với học sinh để họ tham gia:</p>
          <div class="cmp__class-code-box">
            <span class="cmp__class-code">{{ createdClassCode }}</span>
            <button type="button" class="cmp__copy-btn" aria-label="Sao chép mã lớp" @click="copyClassCode" title="Sao chép mã">
              <LucideIcon :name="copied ? 'check' : 'copy'" />
            </button>
          </div>
          <p class="cmp__modal__hint">Học sinh có thể nhập mã này để tham gia lớp</p>
          <div class="cmp__modal__actions">
            <button type="button" class="cmp__btn cmp__btn--primary" @click="showSuccessModal = false">
              Đã hiểu
            </button>
          </div>
        </div>
      </div>
    </Transition>

    <!-- Delete Confirm Modal -->
    <Transition name="modal">
      <div v-if="showDeleteModal" class="cmp__modal-overlay" @click.self="showDeleteModal = false">
        <div class="cmp__modal cmp__modal--confirm" role="dialog" aria-modal="true" aria-labelledby="cmp-delete-title">
          <div class="cmp__modal__icon-wrap cmp__modal__icon-wrap--danger">
            <LucideIcon name="alert-triangle" />
          </div>
          <h3 id="cmp-delete-title" class="cmp__modal__title">Xóa lớp học?</h3>
          <p class="cmp__modal__desc">
            Bạn có chắc muốn xóa lớp <strong>{{ classToDelete?.name }}</strong>?
            <span v-if="classToDelete?.studentCount > 0" class="cmp__modal__warn">
              Lớp hiện có {{ classToDelete.studentCount }} học sinh.
            </span>
          </p>
          <div class="cmp__modal__actions">
            <button type="button" class="cmp__btn cmp__btn--outline" @click="showDeleteModal = false">Hủy</button>
            <button type="button" class="cmp__btn cmp__btn--danger" :disabled="deleteLoading" @click="doDelete">
              <span v-if="deleteLoading" class="cmp__btn-spinner"></span>
              <template v-else>Xóa lớp</template>
            </button>
          </div>
        </div>
      </div>
    </Transition>

  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ApiError } from '../../../services/apiClient'
import { listClasses, createClass, updateClass, deleteClass } from '../../../services/classService'
import { useToast } from '../../../composables/useToast'
import LucideIcon from '../../common/LucideIcon.vue'
import TeacherPageHeader from '../common/TeacherPageHeader.vue'
import EmptyState from '../../common/EmptyState.vue'
import ClassFormModal from './ClassFormModal.vue'

const router = useRouter()
const toast = useToast()

// ─── State ───────────────────────────────────────────────────────
const allClasses = ref([])
const isLoading = ref(false)
const loadError = ref('')
const searchQuery = ref('')
const currentPage = ref(1)
const PAGE_SIZE = 9

// Form modal
const showFormModal = ref(false)
const selectedClass = ref(null)
const formLoading = ref(false)

// Success modal
const showSuccessModal = ref(false)
const createdClassCode = ref('')
const copied = ref(false)

// Delete modal
const showDeleteModal = ref(false)
const classToDelete = ref(null)
const deleteLoading = ref(false)

// ─── Computed ───────────────────────────────────────────────────
const totalStudents = computed(() =>
  allClasses.value.reduce((sum, c) => sum + (c.studentCount || 0), 0)
)

const averageStudents = computed(() => {
  if (allClasses.value.length === 0) return 0
  return Math.round(totalStudents.value / allClasses.value.length)
})

const formatDate = (value) => {
  const date = new Date(value || '')
  return Number.isNaN(date.getTime())
    ? 'Mới cập nhật'
    : date.toLocaleDateString('vi-VN', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric'
      })
}

const filteredClasses = computed(() => {
  if (!searchQuery.value.trim()) return allClasses.value
  const q = searchQuery.value.toLowerCase().trim()
  return allClasses.value.filter(c =>
    (c.name || '').toLowerCase().includes(q) ||
    (c.subject || '').toLowerCase().includes(q)
  )
})

const showEmptyState = computed(() => !isLoading.value && (Boolean(loadError.value) || filteredClasses.value.length === 0))

const emptyStateTitle = computed(() => {
  if (loadError.value) return 'Không thể tải lớp học'
  return searchQuery.value ? 'Không tìm thấy lớp học' : 'Bắt đầu với lớp học đầu tiên'
})

const emptyStateDescription = computed(() => {
  if (loadError.value) return loadError.value
  return searchQuery.value ? 'Thử thay đổi từ khóa tìm kiếm' : 'Tạo lớp học để quản lý học sinh dễ dàng hơn'
})

const emptyStateActionLabel = computed(() => {
  if (loadError.value) return 'Thử lại'
  return searchQuery.value ? '' : 'Tạo lớp học đầu tiên'
})

const emptyStateActionIcon = computed(() => (loadError.value ? 'refresh' : 'add'))

const totalPages = computed(() => Math.max(1, Math.ceil(filteredClasses.value.length / PAGE_SIZE)))

const paginatedClasses = computed(() => {
  const start = (currentPage.value - 1) * PAGE_SIZE
  return filteredClasses.value.slice(start, start + PAGE_SIZE)
})

const visiblePages = computed(() => {
  const pages = []
  const total = totalPages.value
  const current = currentPage.value
  let start = Math.max(1, current - 2)
  let end = Math.min(total, current + 2)
  
  if (end - start < 4) {
    if (start === 1) {
      end = Math.min(total, start + 4)
    } else {
      start = Math.max(1, end - 4)
    }
  }
  
  for (let i = start; i <= end; i++) {
    pages.push(i)
  }
  return pages
})

// ─── API ───────────────────────────────────────────────────────
const loadClasses = async () => {
  isLoading.value = true
  loadError.value = ''
  try {
    const data = await listClasses()
    allClasses.value = data || []
    currentPage.value = Math.min(currentPage.value, Math.max(1, Math.ceil(allClasses.value.length / PAGE_SIZE)))
  } catch (err) {
    allClasses.value = []
    currentPage.value = 1
    loadError.value = err instanceof ApiError ? err.message : 'Không thể tải danh sách lớp học.'
    toast.error(loadError.value)
  } finally {
    isLoading.value = false
  }
}

// ─── Handlers ───────────────────────────────────────────────────
const openCreateModal = () => {
  selectedClass.value = null
  showFormModal.value = true
}

const openEditModal = (cls) => {
  selectedClass.value = cls
  showFormModal.value = true
}

const openClassDetail = (cls) => {
  router.push(`/teacher/class/${cls.id}`)
}

const handleSubmit = async (formData) => {
  formLoading.value = true
  try {
    if (selectedClass.value) {
      await updateClass(selectedClass.value.id, formData)
      toast.success('Cập nhật lớp học thành công.')
    } else {
      const result = await createClass(formData)
      if (result && result.classCode) {
        createdClassCode.value = result.classCode
        showSuccessModal.value = true
        copied.value = false
      } else {
        toast.success('Tạo lớp học thành công.')
      }
    }
    showFormModal.value = false
    await loadClasses()
  } catch (err) {
    toast.error(err instanceof ApiError ? err.message : 'Không thể lưu lớp học.')
  } finally {
    formLoading.value = false
  }
}

const copyClassCode = async () => {
  try {
    await navigator.clipboard.writeText(createdClassCode.value)
    copied.value = true
    toast.success('Đã sao chép mã lớp!')
    setTimeout(() => { copied.value = false }, 2000)
  } catch {
    toast.error('Không thể sao chép mã lớp.')
  }
}

const handleEmptyStateAction = async () => {
  if (loadError.value) {
    await loadClasses()
    return
  }
  openCreateModal()
}

const confirmDelete = (cls) => {
  classToDelete.value = cls
  showDeleteModal.value = true
}

const doDelete = async () => {
  deleteLoading.value = true
  try {
    await deleteClass(classToDelete.value.id)
    toast.success('Đã xóa lớp học.')
    showDeleteModal.value = false
    classToDelete.value = null
    await loadClasses()
  } catch (err) {
    toast.error(err instanceof ApiError ? err.message : 'Không thể xóa lớp học.')
  } finally {
    deleteLoading.value = false
  }
}

onMounted(loadClasses)
</script>

<style scoped>
.cmp {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
  max-width: 1280px;
  margin: 0 auto;
  padding: 1.5rem;
}

@media (min-width: 1400px) {
  .cmp { max-width: 1500px; }
}

@media (min-width: 1600px) {
  .cmp { max-width: 1800px; }
}

@media (min-width: 1920px) {
  .cmp { max-width: 1920px; }
}

.cmp__overview {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 1rem;
}

.cmp__overview-item {
  padding: 1rem 1.125rem;
  border: 1px solid var(--ds-border);
  border-radius: var(--ds-radius-xl);
  background: var(--ds-surface);
  box-shadow: 0 1px 3px rgba(15, 23, 42, 0.05);
}

.dark .cmp__overview-item {
  background: var(--ds-gray-800);
  border-color: var(--ds-border-strong);
}

.cmp__overview-label {
  display: block;
  font-size: 0.75rem;
  font-weight: 700;
  color: var(--ds-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.04em;
}

.cmp__overview-value {
  display: block;
  margin-top: 0.375rem;
  font-size: 1.4rem;
  font-weight: 800;
  color: var(--ds-text);
}

/* Header */
.cmp__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1.5rem;
  flex-wrap: wrap;
}

.cmp__header-left {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.cmp__header-icon {
  width: 56px;
  height: 56px;
  border-radius: var(--ds-radius-2xl);
  background: linear-gradient(135deg, var(--ds-primary) 0%, #818cf8 100%);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  box-shadow: 0 8px 24px rgba(79, 70, 229, 0.25);
}

.cmp__header-title {
  font-family: var(--ds-font-display);
  font-size: 1.5rem;
  font-weight: 800;
  color: var(--ds-text);
  margin: 0;
  line-height: 1.2;
}

.dark .cmp__header-title { color: var(--ds-text); }

.cmp__header-sub {
  font-size: 0.875rem;
  color: var(--ds-text-muted);
  margin: 0.375rem 0 0;
}

.cmp__header-actions {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

/* Stats */
.cmp__stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 0;
  border: 1px solid var(--ds-gray-200);
  border-radius: var(--ds-radius-lg, 14px);
  overflow: hidden;
  background: var(--ds-surface);
  box-shadow: 0 1px 3px rgba(15, 23, 42, 0.05);
}

@media (max-width: 640px) {
  .cmp__stats { grid-template-columns: 1fr; }
}

@keyframes fadeInUp {
  from { opacity: 0; }
  to   { opacity: 1; }
}

@keyframes slideInPanel {
  from { opacity: 0; }
  to   { opacity: 1; }
}

.cmp__stat-card {
  position: relative;
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 1.125rem 1.25rem;
  background: var(--ds-surface);
  border: none;
  border-right: 1px solid var(--ds-gray-200);
  border-radius: 0;
  overflow: hidden;
  animation: fadeInUp 0.4s cubic-bezier(0.16, 1, 0.3, 1) both;
}

/* Staggered animations using nth-child */
.cmp__stat-1 { animation-delay: 0.05s; }
.cmp__stat-2 { animation-delay: 0.1s; }
.cmp__stat-3 { animation-delay: 0.15s; }

/* KPI left accent border */
.cmp__stat-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 3px;
  height: 100%;
  opacity: 0.7;
  transition: opacity 0.2s ease;
}

.tui-kpi-card--primary::before  { background: var(--ds-primary); }
.tui-kpi-card--success::before { background: var(--ds-success); }
.tui-kpi-card--warning::before  { background: var(--ds-warning); }

.cmp__stat-card:last-child { border-right: none; }
.cmp__stat-card:hover { background: var(--ds-gray-50); }
.cmp__stat-card:hover::before { opacity: 1; }

.dark .cmp__stat-card {
  background: var(--ds-gray-800);
  border-color: var(--ds-border-strong);
}

.dark .cmp__stat-card:hover { background: rgba(255,255,255,0.03); }

.cmp__stat-icon {
  width: 48px;
  height: 48px;
  border-radius: var(--ds-radius-xl);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  font-size: 1.25rem;
  transition: transform 0.25s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.cmp__stat-card:hover .cmp__stat-icon {
  transform: scale(1.08) rotate(-3deg);
}

.cmp__stat-card--primary .cmp__stat-icon {
  background: var(--ds-primary-soft);
  color: var(--ds-primary);
}

.cmp__stat-card--success .cmp__stat-icon {
  background: var(--ds-success-soft);
  color: var(--ds-success);
}

.cmp__stat-card--amber .cmp__stat-icon {
  background: rgba(245, 158, 11, 0.1);
  color: var(--ds-warning);
}

.cmp__stat-content {
  position: relative;
  z-index: 1;
}

.cmp__stat-val {
  font-size: 1.625rem;
  font-weight: 800;
  color: var(--ds-text);
  margin: 0;
  line-height: 1;
  letter-spacing: -0.02em;
  font-variant-numeric: tabular-nums;
}

.dark .cmp__stat-val { color: #f1f5f9; }

.cmp__stat-label {
  font-size: 0.7rem;
  color: var(--ds-text-muted);
  margin: 0.25rem 0 0;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.06em;
}

/* Toolbar */
.cmp__toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  flex-wrap: wrap;
  background: var(--ds-surface);
  border: 1px solid var(--ds-gray-200);
  border-radius: var(--ds-radius-lg, 14px);
  padding: 1rem 1.25rem;
  box-shadow: 0 1px 3px rgba(15, 23, 42, 0.05);
}

.cmp__toolbar--anim {
  animation: slideInPanel 0.4s cubic-bezier(0.16, 1, 0.3, 1) 0.1s both;
}

.cmp__search {
  position: relative;
  flex: 1;
  max-width: 400px;
}

.cmp__search-icon {
  position: absolute;
  left: 1rem;
  top: 50%;
  transform: translateY(-50%);
  color: var(--ds-text-muted);
  width: 18px;
  height: 18px;
  pointer-events: none;
}

.cmp__search-input {
  width: 100%;
  padding: 0.875rem 1.25rem 0.875rem 3rem;
  border: 2px solid var(--ds-border);
  border-radius: var(--ds-radius-2xl);
  font-size: 0.9375rem;
  background: var(--ds-surface);
  color: var(--ds-text);
  transition: color 0.25s cubic-bezier(0.4, 0, 0.2, 1), background-color 0.25s cubic-bezier(0.4, 0, 0.2, 1), border-color 0.25s cubic-bezier(0.4, 0, 0.2, 1), box-shadow 0.25s cubic-bezier(0.4, 0, 0.2, 1), transform 0.25s cubic-bezier(0.4, 0, 0.2, 1);
}

.dark .cmp__search-input {
  background: var(--ds-gray-800);
  border-color: var(--ds-border-strong);
  color: var(--ds-text);
}

.cmp__search-input:focus {
  outline: none;
  border-color: var(--ds-primary);
  box-shadow: 0 0 0 4px var(--ds-primary-ring), 0 8px 24px rgba(79, 70, 229, 0.15);
  transform: translateY(-1px);
}

.dark .cmp__search-input:focus {
  box-shadow: 0 0 0 4px rgba(79, 70, 229, 0.3), 0 8px 24px rgba(0, 0, 0, 0.3);
}

.cmp__search-clear {
  position: absolute;
  right: 0.75rem;
  top: 50%;
  transform: translateY(-50%);
  width: 24px;
  height: 24px;
  border: none;
  background: var(--ds-gray-200);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: var(--ds-text-muted);
  transition: color 0.15s ease, background-color 0.15s ease, border-color 0.15s ease, box-shadow 0.15s ease, transform 0.15s ease;
}

.dark .cmp__search-clear { background: var(--ds-gray-700); color: var(--ds-text-muted); }
.cmp__search-clear:hover { background: var(--ds-gray-300); color: var(--ds-text); }
.dark .cmp__search-clear:hover { background: var(--ds-gray-600); }

.cmp__toolbar-right {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.cmp__result-count {
  font-size: 0.8rem;
  font-weight: 600;
  color: var(--ds-text-muted);
  padding: 0.5rem 0.875rem;
  background: var(--ds-gray-100);
  border-radius: var(--ds-radius-lg);
}

.dark .cmp__result-count { background: var(--ds-gray-700); color: var(--ds-text-muted); }

/* Loading */
.cmp__loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 4rem 2rem;
  gap: 1rem;
  color: var(--ds-text-muted);
}

.cmp__spinner {
  width: 48px;
  height: 48px;
  border: 4px solid var(--ds-border);
  border-top-color: var(--ds-primary);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin { to { transform: rotate(360deg); } }

/* Grid */
.cmp__grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 1.25rem;
}

/* Card Animation - GPU Optimized */
.card-enter-active,
.card-leave-active {
  transition: opacity 0.3s ease;
}

.card-enter-from,
.card-leave-to {
  opacity: 0;
}

.card-enter-from .cmp__card,
.card-leave-to .cmp__card {
  transform: scale(0.98);
}

.card-move {
  transition: transform 0.3s ease;
}

.cmp__card {
  position: relative;
  background: var(--ds-surface);
  border: 1px solid var(--ds-border);
  border-radius: var(--ds-radius-2xl);
  padding: 0;
  transition: border-color 0.25s ease, box-shadow 0.25s ease, transform 0.25s ease;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.dark .cmp__card {
  background: var(--ds-gray-800);
  border-color: var(--ds-border-strong);
}

.cmp__card:hover,
.cmp__card:focus-within {
  border-color: var(--ds-primary);
  box-shadow: 0 8px 32px rgba(79, 70, 229, 0.12);
  transform: translateY(-4px);
}

.cmp__card-main {
  width: 100%;
  border: none;
  background: transparent;
  padding: 0;
  color: inherit;
  text-align: left;
  cursor: pointer;
}

.cmp__card-main:focus-visible {
  outline: none;
}

.cmp__card-main:focus-visible .cmp__card-header,
.cmp__card-main:focus-visible .cmp__card-body {
  box-shadow: inset 0 0 0 2px var(--ds-primary);
}

.cmp__card-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  padding: 1.25rem 1.25rem 0;
}

.cmp__card-icon {
  width: 48px;
  height: 48px;
  border-radius: var(--ds-radius-xl);
  background: linear-gradient(135deg, var(--ds-primary-soft) 0%, rgba(99, 102, 241, 0.15) 100%);
  color: var(--ds-primary);
  display: flex;
  align-items: center;
  justify-content: center;
}

.dark .cmp__card-icon {
  background: linear-gradient(135deg, rgba(99, 102, 241, 0.2) 0%, rgba(99, 102, 241, 0.1) 100%);
}

.cmp__card-actions {
  position: absolute;
  top: 1.25rem;
  right: 1.25rem;
  opacity: 0;
  pointer-events: none;
  transform: translateY(-4px);
  transition: color 0.2s ease, background-color 0.2s ease, border-color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease;
}

.cmp__card-actions-inner {
  display: flex;
  gap: 0.375rem;
}

.cmp__card:hover .cmp__card-actions,
.cmp__card:focus-within .cmp__card-actions {
  opacity: 1;
  pointer-events: auto;
  transform: translateY(0);
}

@media (hover: none), (pointer: coarse) {
  .cmp__card-actions {
    opacity: 1;
    pointer-events: auto;
    transform: translateY(0);
  }
}

.cmp__card-heading {
  min-width: 0;
}

.cmp__card-title-row {
  display: flex;
  align-items: flex-start;
  gap: 0.875rem;
}

.cmp__action-btn {
  width: 36px;
  height: 36px;
  border: none;
  background: var(--ds-surface);
  border: 1px solid var(--ds-border);
  color: var(--ds-text-muted);
  border-radius: var(--ds-radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: color 0.15s ease, background-color 0.15s ease, border-color 0.15s ease, box-shadow 0.15s ease, transform 0.15s ease;
}

.dark .cmp__action-btn {
  background: var(--ds-gray-700);
  border-color: var(--ds-border-strong);
}

.cmp__action-btn:hover {
  background: var(--ds-gray-100);
  color: var(--ds-primary);
  border-color: var(--ds-primary);
}

.dark .cmp__action-btn:hover { background: var(--ds-gray-600); }

.cmp__action-btn--danger:hover {
  background: var(--ds-danger-soft);
  color: var(--ds-danger);
  border-color: var(--ds-danger);
}

.cmp__card-body {
  flex: 1;
  padding: 0.75rem 1.25rem;
  padding-top: 0.875rem;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.cmp__card-title {
  font-size: 1.125rem;
  font-weight: 800;
  color: var(--ds-text);
  margin: 0;
  line-height: 1.3;
}

.dark .cmp__card-title { color: var(--ds-text); }

.cmp__card-subtitle {
  margin: 0.2rem 0 0;
  font-size: 0.82rem;
  color: var(--ds-text-muted);
}

.cmp__card-subject {
  display: inline-flex;
  align-items: center;
  gap: 0.375rem;
  font-size: 0.75rem;
  font-weight: 700;
  color: var(--ds-primary);
  background: var(--ds-primary-soft);
  padding: 0.375rem 0.75rem;
  border-radius: var(--ds-radius-lg);
  width: fit-content;
}

.dark .cmp__card-subject {
  background: rgba(99, 102, 241, 0.15);
}

.cmp__card-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.cmp__card-chip {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  padding: 0.35rem 0.7rem;
  border-radius: 9999px;
  background: var(--ds-primary-soft);
  color: var(--ds-primary);
  font-size: 0.75rem;
  font-weight: 700;
}

.cmp__card-chip--muted {
  background: var(--ds-gray-100);
  color: var(--ds-text-secondary);
}

.dark .cmp__card-chip--muted {
  background: var(--ds-gray-700);
  color: var(--ds-text-muted);
}

.cmp__card-inline {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  font-size: 0.78rem;
}

.cmp__card-inline-label {
  color: var(--ds-text-muted);
  font-weight: 600;
}

.cmp__card-inline-value {
  color: var(--ds-text);
  font-weight: 700;
}

.cmp__card-stat {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.85rem;
  color: var(--ds-text-secondary);
  font-weight: 600;
}

.dark .cmp__card-stat { color: var(--ds-text-muted); }

/* Pagination */
.cmp__pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  margin-top: 1rem;
}

.cmp__page-btn {
  width: 40px;
  height: 40px;
  border-radius: var(--ds-radius-xl);
  border: 1.5px solid var(--ds-border);
  background: var(--ds-surface);
  color: var(--ds-text-secondary);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: color 0.15s ease, background-color 0.15s ease, border-color 0.15s ease, box-shadow 0.15s ease, transform 0.15s ease;
}

.dark .cmp__page-btn { background: var(--ds-gray-800); border-color: var(--ds-border-strong); color: var(--ds-text-muted); }
.cmp__page-btn:hover:not(:disabled) { border-color: var(--ds-primary); color: var(--ds-primary); background: var(--ds-primary-soft); }
.cmp__page-btn:disabled { opacity: 0.4; cursor: not-allowed; }

.cmp__page-numbers {
  display: flex;
  align-items: center;
  gap: 0.25rem;
}

.cmp__page-num {
  min-width: 40px;
  height: 40px;
  border-radius: var(--ds-radius-lg);
  border: 1.5px solid transparent;
  background: transparent;
  color: var(--ds-text-secondary);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: color 0.15s ease, background-color 0.15s ease, border-color 0.15s ease, box-shadow 0.15s ease, transform 0.15s ease;
  font-weight: 600;
  font-size: 0.875rem;
}

.cmp__page-num:hover:not(.cmp__page-num--active) {
  background: var(--ds-gray-100);
  color: var(--ds-text);
}

.dark .cmp__page-num:hover:not(.cmp__page-num--active) { background: var(--ds-gray-700); color: var(--ds-text); }

.cmp__page-num--active {
  background: var(--ds-primary);
  color: white;
  border-color: var(--ds-primary);
}

/* Buttons - GPU Optimized */
.cmp__btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  padding: 0.75rem 1.25rem;
  border-radius: var(--ds-radius-xl);
  font-size: 0.875rem;
  font-weight: 700;
  cursor: pointer;
  /* GPU optimization */
  transform: translateZ(0);
  will-change: transform;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
  border: 1.5px solid transparent;
  white-space: nowrap;
}

.cmp__btn--primary {
  background: linear-gradient(135deg, var(--ds-primary) 0%, #6366f1 100%);
  color: white;
  box-shadow: 0 4px 16px rgba(79, 70, 229, 0.25);
}

.cmp__btn--primary:hover {
  transform: translateY(-2px) translateZ(0);
  box-shadow: 0 8px 24px rgba(79, 70, 229, 0.35);
}

.cmp__btn--outline {
  background: var(--ds-surface);
  border-color: var(--ds-border);
  color: var(--ds-text-secondary);
}

.dark .cmp__btn--outline {
  background: var(--ds-gray-800);
  border-color: var(--ds-border-strong);
  color: var(--ds-text-muted);
}

.cmp__btn--outline:hover { border-color: var(--ds-primary); color: var(--ds-primary); }

.cmp__btn--danger {
  background: var(--ds-danger);
  color: white;
  border-color: var(--ds-danger);
  box-shadow: 0 4px 16px rgba(220, 38, 38, 0.2);
}

.cmp__btn--danger:hover { background: #dc2626; }
.cmp__btn:disabled { opacity: 0.5; cursor: not-allowed; transform: none !important; box-shadow: none !important; }

.cmp__btn-spinner {
  width: 16px;
  height: 16px;
  border: 2px solid rgba(255,255,255,0.3);
  border-top-color: white;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  transform: translateZ(0);
}

.sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}

/* Modal - GPU Optimized */
.cmp__modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.56);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: 1.5rem;
}

.cmp__modal {
  background: var(--ds-surface);
  border-radius: var(--ds-radius-2xl);
  width: 100%;
  max-width: 400px;
  padding: 2rem;
  box-shadow: 0 24px 64px rgba(0,0,0,0.25);
  text-align: center;
  /* GPU optimization */
  transform: translateZ(0);
  animation: fadeInUp 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

.dark .cmp__modal { background: var(--ds-gray-800); }

.cmp__modal--confirm {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1rem;
}

.cmp__modal__icon-wrap {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.cmp__modal__icon-wrap--danger {
  background: var(--ds-danger-soft);
  color: var(--ds-danger);
}

.cmp__modal__title {
  font-family: var(--ds-font-display);
  font-size: 1.25rem;
  font-weight: 800;
  color: var(--ds-text);
  margin: 0;
}

.dark .cmp__modal__title { color: var(--ds-text); }

.cmp__modal__desc {
  font-size: 0.9rem;
  color: var(--ds-text-secondary);
  margin: 0;
  line-height: 1.6;
}

.dark .cmp__modal__desc { color: var(--ds-text-muted); }
.cmp__modal__desc strong { color: var(--ds-text); }
.dark .cmp__modal__desc strong { color: var(--ds-text); }

.cmp__modal__warn {
  display: block;
  margin-top: 0.75rem;
  padding: 0.75rem;
  background: var(--ds-danger-soft);
  border-radius: var(--ds-radius-lg);
  color: var(--ds-danger);
  font-weight: 600;
  font-size: 0.85rem;
}

.cmp__modal__actions {
  display: flex;
  gap: 0.75rem;
  margin-top: 0.5rem;
  width: 100%;
  justify-content: center;
}

/* Modal Transitions - GPU Optimized */
.modal-enter-active,
.modal-leave-active {
  transition: opacity 0.2s ease;
}

.modal-enter-from,
.modal-leave-to {
  opacity: 0;
}

.modal-enter-from .cmp__modal,
.modal-leave-to .cmp__modal {
  opacity: 0;
}

/* Success Modal */
.cmp__modal--success {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1rem;
}

.cmp__modal__icon-wrap--success {
  background: var(--ds-success-soft);
  color: var(--ds-success);
}

.cmp__class-code-box {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 1rem 1.5rem;
  background: var(--ds-primary-soft);
  border: 2px dashed var(--ds-primary);
  border-radius: var(--ds-radius-2xl);
  margin: 0.5rem 0;
}

.dark .cmp__class-code-box {
  background: rgba(79, 70, 229, 0.15);
}

.cmp__class-code {
  font-family: var(--ds-font-display);
  font-size: 1.75rem;
  font-weight: 800;
  color: var(--ds-primary);
  letter-spacing: 0.15em;
}

.dark .cmp__class-code { color: var(--ds-primary); }

.cmp__copy-btn {
  width: 40px;
  height: 40px;
  border: none;
  background: var(--ds-primary);
  color: white;
  border-radius: var(--ds-radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: color 0.15s ease, background-color 0.15s ease, border-color 0.15s ease, box-shadow 0.15s ease, transform 0.15s ease;
}

.cmp__copy-btn:hover {
  background: var(--ds-primary-hover);
  transform: scale(1.05);
}

.cmp__modal__hint {
  font-size: 0.8rem;
  color: var(--ds-text-muted);
  margin: 0;
}

@media (max-width: 768px) {
  .cmp {
    padding: 1rem;
    gap: 1rem;
  }

  .cmp__overview {
    grid-template-columns: 1fr;
  }
}
</style>