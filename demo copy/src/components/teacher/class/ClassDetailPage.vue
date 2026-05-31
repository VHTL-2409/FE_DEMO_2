<template>
  <div class="cdp">
    <!-- Page Header -->
    <div class="cdp__header">
      <button type="button" class="cdp__back-btn" @click="goBack">
        <LucideIcon name="arrow_back" />
        <span>Quay lại</span>
      </button>
      <div class="cdp__header-content">
        <div class="cdp__header-icon">
          <LucideIcon name="groups" />
        </div>
        <div class="cdp__header-text">
          <h1 class="cdp__title">{{ classData?.name || 'Đang tải...' }}</h1>
          <p class="cdp__subtitle">{{ classData?.subject || 'Chưa có môn học' }}</p>
        </div>
      </div>
    </div>

    <!-- Loading State -->
    <div v-if="isLoading" class="cdp__loading">
      <div class="cdp__spinner"></div>
      <p>Đang tải thông tin lớp học...</p>
    </div>

    <!-- Error State -->
    <div v-else-if="error" class="cdp__error">
      <LucideIcon name="alert-circle" size="48" />
      <h3>Không thể tải thông tin lớp học</h3>
      <p>{{ error }}</p>
      <button type="button" class="cdp__btn cdp__btn--primary" @click="reloadClassPage">
        <LucideIcon name="refresh" />
        Thử lại
      </button>
    </div>

    <!-- Content -->
    <div v-else class="cdp__content">
      <div class="cdp__overview">
        <div class="cdp__overview-main">
          <div class="cdp__overview-top">
            <button
              type="button"
              class="cdp__overview-chip cdp__overview-chip--action"
              :aria-label="copied ? 'Đã sao chép mã lớp' : 'Sao chép mã lớp'"
              @click="copyClassCode"
              :title="copied ? 'Đã sao chép mã lớp' : 'Sao chép mã lớp'"
            >
              <LucideIcon :name="copied ? 'check' : 'key-round'" />
              {{ classData?.classCode || '—' }}
            </button>
            <span class="cdp__overview-chip cdp__overview-chip--muted">
              <LucideIcon name="users" />
              {{ students.length }} học sinh
            </span>
          </div>
          <p v-if="classData?.description" class="cdp__overview-desc">{{ classData.description }}</p>
          <div class="cdp__overview-meta">
            <div class="cdp__overview-meta-item">
              <span class="cdp__overview-meta-label">Ngày tạo</span>
              <strong class="cdp__overview-meta-value">{{ formatDate(classData?.createdAt) }}</strong>
            </div>
            <div class="cdp__overview-meta-item">
              <span class="cdp__overview-meta-label">Cập nhật cuối</span>
              <strong class="cdp__overview-meta-value">{{ formatDate(classData?.updatedAt) }}</strong>
            </div>
          </div>
        </div>
        <div class="cdp__overview-actions">
          <button type="button" class="cdp__btn cdp__btn--secondary" @click="goToCreateExam">
            <LucideIcon name="assignment_add" />
            Tạo bài thi
          </button>
          <button type="button" class="cdp__btn cdp__btn--primary" @click="showBulkAddModal = true">
            <LucideIcon name="upload" />
            Import học sinh
          </button>
          <button type="button" class="cdp__btn cdp__btn--secondary" @click="openEditModal">
            <LucideIcon name="edit" />
            Chỉnh sửa lớp
          </button>
        </div>
      </div>

      <div class="cdp__main">
        <div class="cdp__card cdp__card--full">
          <div class="cdp__card-header">
            <h3 class="cdp__card-title">
              <LucideIcon name="assignment" />
              Bài thi của lớp
              <span class="cdp__badge">{{ classExams.length }}</span>
            </h3>
          </div>

          <div v-if="isLoadingClassExams" class="cdp__loading-inline">
            <div class="cdp__spinner cdp__spinner--sm"></div>
            <p>Đang tải bài thi của lớp...</p>
          </div>

          <div v-else-if="classExamsError" class="cdp__inline-error">
            <LucideIcon name="alert-circle" />
            <div>
              <p class="cdp__inline-error-title">Không thể tải bài thi của lớp</p>
              <p class="cdp__inline-error-text">{{ classExamsError }}</p>
            </div>
            <button type="button" class="cdp__btn cdp__btn--secondary" @click="loadClassExams">
              <LucideIcon name="refresh" />
              Thử lại
            </button>
          </div>

          <div v-else-if="classExams.length === 0" class="cdp__empty">
            <LucideIcon name="assignment" size="48" />
            <h4>Chưa có bài thi nào cho lớp này</h4>
            <p>Tạo bài thi mới và gán trực tiếp cho lớp để học sinh có thể vào thi đúng quyền.</p>
            <button type="button" class="cdp__btn cdp__btn--primary" @click="goToCreateExam">
              <LucideIcon name="assignment_add" />
              Tạo bài thi
            </button>
          </div>

          <div v-else class="cdp__exam-list">
            <button
              v-for="exam in classExams"
              :key="exam.id"
              type="button"
              class="cdp__exam-item"
              @click="openExamDetail(exam.id)"
            >
              <div class="cdp__exam-item-main">
                <div class="cdp__exam-item-icon">
                  <LucideIcon name="quiz" />
                </div>
                <div class="cdp__exam-item-copy">
                  <strong class="cdp__exam-item-title">{{ exam.title || 'Bài thi' }}</strong>
                  <span class="cdp__exam-item-meta">
                    {{ exam.questionCount || 0 }} câu · {{ exam.durationMinutes || 0 }} phút
                  </span>
                </div>
              </div>
              <div class="cdp__exam-item-side">
                <span class="cdp__exam-status" :class="{ 'cdp__exam-status--draft': !exam.isActive }">
                  {{ exam.isActive ? 'Đang mở' : 'Nháp' }}
                </span>
                <LucideIcon name="chevron_right" />
              </div>
            </button>
          </div>
        </div>

        <div class="cdp__card cdp__card--full">
            <div class="cdp__card-header">
              <h3 class="cdp__card-title">
                <LucideIcon name="users" />
                Danh sách học sinh
                <span class="cdp__badge">{{ students.length }}</span>
              </h3>
              <div class="cdp__card-actions">
                <div class="cdp__search">
                  <LucideIcon name="search" class="cdp__search-icon" />
                  <input
                    v-model="searchQuery"
                    type="text"
                    class="cdp__search-input"
                    aria-label="Tìm kiếm học sinh trong lớp"
                    placeholder="Tìm kiếm học sinh..."
                  />
                </div>
              </div>
            </div>

            <!-- Loading Students -->
            <div v-if="isLoadingStudents" class="cdp__loading-inline">
              <div class="cdp__spinner cdp__spinner--sm"></div>
              <p>Đang tải danh sách học sinh...</p>
            </div>

            <div v-else-if="studentsError" class="cdp__inline-error">
              <LucideIcon name="alert-circle" />
              <div>
                <p class="cdp__inline-error-title">Không thể tải danh sách học sinh</p>
                <p class="cdp__inline-error-text">{{ studentsError }}</p>
              </div>
              <button type="button" class="cdp__btn cdp__btn--secondary" @click="loadStudents">
                <LucideIcon name="refresh" />
                Thử lại
              </button>
            </div>

            <!-- Empty State -->
            <div v-else-if="filteredStudents.length === 0" class="cdp__empty">
              <LucideIcon name="users" size="48" />
              <h4>{{ searchQuery ? 'Không tìm thấy học sinh' : 'Chưa có học sinh trong lớp' }}</h4>
              <p>{{ searchQuery ? 'Thử thay đổi từ khóa tìm kiếm' : 'Bạn có thể import danh sách học sinh từ file Excel để bắt đầu.' }}</p>
              <button v-if="!searchQuery" type="button" class="cdp__btn cdp__btn--primary" @click="showBulkAddModal = true">
                <LucideIcon name="upload" />
                Import học sinh
              </button>
            </div>

            <!-- Students Table -->
            <div v-else class="cdp__table-wrapper">
              <table class="cdp__table">
                <thead>
                  <tr>
                    <th>Học sinh</th>
                    <th>Email</th>
                    <th>Ngày tham gia</th>
                    <th class="cdp__th-actions">Thao tác</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="student in paginatedStudents" :key="student.studentId || student.id" class="cdp__table-row">
                    <td>
                      <div class="cdp__student-info">
                        <div class="cdp__student-avatar">
                          {{ getInitials(student.studentUsername) }}
                        </div>
                        <span class="cdp__student-name">{{ student.studentUsername }}</span>
                      </div>
                    </td>
                    <td class="cdp__td-muted">{{ student.studentEmail }}</td>
                    <td class="cdp__td-muted">{{ formatDate(student.joinedAt) }}</td>
                    <td class="cdp__td-actions">
                      <button
                        type="button"
                        class="cdp__icon-btn cdp__icon-btn--danger cdp__icon-btn--compact"
                        :disabled="removingStudentId === student.studentId"
                        :title="`Xóa ${student.studentUsername || 'học sinh'} khỏi lớp`"
                        :aria-label="`Xóa ${student.studentUsername || 'học sinh'} khỏi lớp`"
                        @click="handleRemoveStudent(student)"
                      >
                        <span v-if="removingStudentId === student.studentId" class="cdp__spinner cdp__spinner--xs"></span>
                        <template v-else>
                          <LucideIcon name="user_minus" />
                        </template>
                      </button>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>

            <!-- Pagination -->
            <div v-if="totalPages > 1" class="cdp__pagination">
              <button
                type="button"
                class="cdp__page-btn"
                aria-label="Trang trước"
                :disabled="currentPage === 1"
                @click="currentPage--"
              >
                <LucideIcon name="chevron_left" />
              </button>
              <div class="cdp__page-numbers">
                <button
                  v-for="page in visiblePages"
                  :key="page"
                  type="button"
                  class="cdp__page-num"
                  :class="{ 'cdp__page-num--active': page === currentPage }"
                  @click="currentPage = page"
                >
                  {{ page }}
                </button>
              </div>
              <button
                type="button"
                class="cdp__page-btn"
                aria-label="Trang sau"
                :disabled="currentPage === totalPages"
                @click="currentPage++"
              >
                <LucideIcon name="chevron_right" />
              </button>
            </div>
          </div>
      </div>
    </div>

    <!-- Edit Class Modal -->
    <ClassFormModal
      v-model="showEditModal"
      :class-item="classData"
      :loading="isUpdating"
      @submit="handleUpdateClass"
    />

    <!-- Bulk Add Students Modal -->
    <BulkAddStudentsModal
      v-model="showBulkAddModal"
      :class-id="classId"
      :class-name="classData?.name"
      @added="handleStudentsAdded"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ApiError } from '../../../services/apiClient'
import { getClassDetail, updateClass, getClassStudents, removeStudentFromClass, getTeacherClassExams } from '../../../services/classService'
import { useToast } from '../../../composables/useToast'
import LucideIcon from '../../common/LucideIcon.vue'
import ClassFormModal from './ClassFormModal.vue'
import BulkAddStudentsModal from './BulkAddStudentsModal.vue'

const router = useRouter()
const route = useRoute()
const toast = useToast()

const classId = computed(() => route.params.id)

// State
const classData = ref(null)
const students = ref([])
const isLoading = ref(true)
const isLoadingStudents = ref(false)
const isLoadingClassExams = ref(false)
const isUpdating = ref(false)
const removingStudentId = ref(null)
const error = ref(null)
const studentsError = ref('')
const classExamsError = ref('')
const searchQuery = ref('')
const currentPage = ref(1)
const PAGE_SIZE = 10
const copied = ref(false)
const classExams = ref([])

// Modals
const showEditModal = ref(false)
const showBulkAddModal = ref(false)

// Computed
const filteredStudents = computed(() => {
  if (!searchQuery.value.trim()) return students.value
  const q = searchQuery.value.toLowerCase().trim()
  return students.value.filter(s =>
    (s.studentUsername || '').toLowerCase().includes(q) ||
    (s.studentEmail || '').toLowerCase().includes(q)
  )
})

const totalPages = computed(() => Math.max(1, Math.ceil(filteredStudents.value.length / PAGE_SIZE)))

const paginatedStudents = computed(() => {
  const start = (currentPage.value - 1) * PAGE_SIZE
  return filteredStudents.value.slice(start, start + PAGE_SIZE)
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

// Watch search to reset page
watch(searchQuery, () => { currentPage.value = 1 })

// Methods
const loadClassDetail = async () => {
  if (!classId.value) return
  isLoading.value = true
  error.value = null
  try {
    const data = await getClassDetail(classId.value)
    classData.value = data
  } catch (err) {
    const msg = err instanceof ApiError ? err.message : 'Không thể tải thông tin lớp học.'
    error.value = msg
    toast.error(msg)
  } finally {
    isLoading.value = false
  }
}

const loadStudents = async () => {
  if (!classId.value) return
  isLoadingStudents.value = true
  studentsError.value = ''
  try {
    const data = await getClassStudents(classId.value)
    students.value = data || []
    currentPage.value = Math.min(currentPage.value, totalPages.value)
    if (classData.value) {
      classData.value = {
        ...classData.value,
        studentCount: students.value.length
      }
    }
  } catch (err) {
    students.value = []
    studentsError.value = err instanceof ApiError ? err.message : 'Không thể tải danh sách học sinh.'
    toast.error(studentsError.value)
  } finally {
    isLoadingStudents.value = false
  }
}

const loadClassExams = async () => {
  if (!classId.value) return
  isLoadingClassExams.value = true
  classExamsError.value = ''
  try {
    classExams.value = await getTeacherClassExams(classId.value)
  } catch (err) {
    classExams.value = []
    classExamsError.value = err instanceof ApiError ? err.message : 'Không thể tải bài thi của lớp.'
  } finally {
    isLoadingClassExams.value = false
  }
}

const goBack = () => {
  router.push('/teacher/classes')
}

const goToCreateExam = () => {
  router.push({
    path: '/teacher/exams/build',
    query: {
      type: 'private',
      classId: String(classId.value || '')
    }
  })
}

const openExamDetail = (examId) => {
  if (!examId) return
  router.push(`/teacher/exams/${examId}`)
}

const reloadClassPage = async () => {
  await loadClassDetail()
  if (error.value) return
  await Promise.all([loadStudents(), loadClassExams()])
}

const getInitials = (name) => {
  if (!name) return '?'
  return name.slice(0, 2).toUpperCase()
}

const formatDate = (date) => {
  if (!date) return '—'
  try {
    return new Date(date).toLocaleDateString('vi-VN', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric'
    })
  } catch {
    return '—'
  }
}

const openEditModal = () => {
  showEditModal.value = true
}

const handleUpdateClass = async (formData) => {
  isUpdating.value = true
  try {
    await updateClass(classId.value, formData)
    toast.success('Cập nhật lớp học thành công.')
    showEditModal.value = false
    await loadClassDetail()
  } catch (err) {
    toast.error(err instanceof ApiError ? err.message : 'Không thể cập nhật lớp học.')
  } finally {
    isUpdating.value = false
  }
}

const handleStudentsAdded = async () => {
  await loadStudents()
}

const handleRemoveStudent = async (student) => {
  const studentId = student?.studentId
  const studentName = student?.studentUsername || 'học sinh này'
  if (!studentId) return

  const confirmed = window.confirm(`Xóa ${studentName} khỏi lớp "${classData.value?.name || ''}"?`)
  if (!confirmed) return

  removingStudentId.value = studentId
  try {
    await removeStudentFromClass(classId.value, studentId)
    toast.success(`Đã xóa ${studentName} khỏi lớp.`)
    await handleStudentsAdded()
  } catch (err) {
    toast.error(err instanceof ApiError ? err.message : 'Không thể xóa học sinh khỏi lớp.')
  } finally {
    removingStudentId.value = null
  }
}

const copyClassCode = async () => {
  if (!classData.value?.classCode) return
  try {
    await navigator.clipboard.writeText(classData.value.classCode)
    copied.value = true
    toast.success('Đã sao chép mã lớp!')
    setTimeout(() => { copied.value = false }, 2000)
  } catch {
    toast.error('Không thể sao chép mã lớp.')
  }
}

// Watch route params to reload data when navigating between classes
watch(() => route.params.id, async (newId, oldId) => {
  if (newId && newId !== oldId) {
    searchQuery.value = ''
    currentPage.value = 1
    await loadClassDetail()
    await Promise.all([loadStudents(), loadClassExams()])
  }
}, { immediate: false })

// Lifecycle
onMounted(async () => {
  await reloadClassPage()
})
</script>

<style scoped>
.cdp {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
  max-width: 1280px;
  margin: 0 auto;
  padding: 1.5rem;
}

@media (min-width: 1400px) {
  .cdp { max-width: 1500px; }
}

@media (min-width: 1600px) {
  .cdp { max-width: 1800px; }
}

@media (min-width: 1920px) {
  .cdp { max-width: 1920px; }
}

/* Header */
.cdp__header {
  display: flex;
  align-items: center;
  gap: 1rem;
  flex-wrap: wrap;
  background: var(--ds-surface);
  border: 1px solid var(--ds-border);
  border-radius: var(--ds-radius-2xl);
  padding: 1rem 1.5rem;
}

.dark .cdp__header {
  background: var(--ds-gray-800);
  border-color: var(--ds-border-strong);
}

.cdp__back-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 1rem;
  border: 1.5px solid var(--ds-border);
  background: transparent;
  color: var(--ds-text-secondary);
  border-radius: var(--ds-radius-xl);
  font-size: 0.875rem;
  font-weight: 600;
  cursor: pointer;
  transition: color 0.15s ease, background-color 0.15s ease, border-color 0.15s ease, box-shadow 0.15s ease, transform 0.15s ease;
}

.dark .cdp__back-btn {
  background: transparent;
  border-color: var(--ds-border-strong);
  color: var(--ds-text-muted);
}

.cdp__back-btn:hover {
  border-color: var(--ds-primary);
  color: var(--ds-primary);
}

.cdp__header-content {
  display: flex;
  align-items: center;
  gap: 1rem;
  flex: 1;
}

.cdp__header-icon {
  width: 48px;
  height: 48px;
  border-radius: var(--ds-radius-xl);
  background: linear-gradient(135deg, var(--ds-primary) 0%, #818cf8 100%);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  box-shadow: 0 4px 16px rgba(79, 70, 229, 0.25);
}

.cdp__header-text {
  flex: 1;
}

.cdp__title {
  font-family: var(--ds-font-display);
  font-size: 1.375rem;
  font-weight: 800;
  color: var(--ds-text);
  margin: 0;
  line-height: 1.2;
}

.dark .cdp__title { color: var(--ds-text); }

.cdp__subtitle {
  font-size: 0.8rem;
  color: var(--ds-text-muted);
  margin: 0.25rem 0 0;
}

/* Loading & Error */
.cdp__loading,
.cdp__error {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 4rem 2rem;
  gap: 1rem;
  text-align: center;
  color: var(--ds-text-muted);
}

.cdp__error {
  color: var(--ds-danger);
}

.cdp__error h3 {
  font-size: 1.125rem;
  font-weight: 700;
  margin: 0;
  color: var(--ds-text);
}

.dark .cdp__error h3 { color: var(--ds-text); }

.cdp__spinner {
  width: 48px;
  height: 48px;
  border: 4px solid var(--ds-border);
  border-top-color: var(--ds-primary);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

.cdp__spinner--sm {
  width: 24px;
  height: 24px;
  border-width: 3px;
}

.cdp__spinner--xs {
  width: 14px;
  height: 14px;
  border-width: 2px;
}

@keyframes spin { to { transform: rotate(360deg) translateZ(0); } }

.cdp__content {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.cdp__overview {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
  padding: 1.25rem 1.5rem;
  background: var(--ds-surface);
  border: 1px solid var(--ds-border);
  border-radius: var(--ds-radius-2xl);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.dark .cdp__overview {
  background: var(--ds-gray-800);
  border-color: var(--ds-border-strong);
}

.cdp__overview-main {
  display: flex;
  flex-direction: column;
  gap: 0.875rem;
  flex: 1;
}

.cdp__overview-top {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.cdp__overview-chip {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  padding: 0.4rem 0.75rem;
  border-radius: 9999px;
  background: var(--ds-primary-soft);
  color: var(--ds-primary);
  font-size: 0.78rem;
  font-weight: 700;
}

.cdp__overview-chip--action {
  border: none;
  cursor: pointer;
  transition: color 0.15s ease, background-color 0.15s ease, border-color 0.15s ease, box-shadow 0.15s ease, transform 0.15s ease;
}

.cdp__overview-chip--action:hover {
  transform: translateY(-1px);
  box-shadow: 0 0 0 1px rgba(12, 92, 171, 0.18);
}

.cdp__overview-chip--muted {
  background: var(--ds-gray-100);
  color: var(--ds-text-secondary);
}

.dark .cdp__overview-chip--muted {
  background: var(--ds-gray-700);
  color: var(--ds-text-muted);
}

.cdp__overview-desc {
  margin: 0;
  color: var(--ds-text-secondary);
  line-height: 1.6;
  font-size: 0.9rem;
}

.cdp__overview-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 1rem;
}

.cdp__overview-meta-item {
  min-width: 160px;
}

.cdp__overview-meta-label {
  display: block;
  font-size: 0.72rem;
  font-weight: 700;
  color: var(--ds-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.04em;
}

.cdp__overview-meta-value {
  display: block;
  margin-top: 0.35rem;
  font-size: 0.9rem;
  color: var(--ds-text);
}

.cdp__overview-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
  justify-content: flex-end;
}

.cdp__main {
  width: 100%;
}

/* Cards */
.cdp__card {
  background: var(--ds-surface);
  border: 1px solid var(--ds-border);
  border-radius: var(--ds-radius-2xl);
  padding: 1.25rem;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.dark .cdp__card {
  background: var(--ds-gray-800);
  border-color: var(--ds-border-strong);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
}

.cdp__card--full {
  width: 100%;
}

.cdp__exam-list {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.cdp__exam-item {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  padding: 1rem 1.125rem;
  border-radius: var(--ds-radius-xl);
  border: 1px solid var(--ds-border);
  background: var(--ds-gray-50);
  cursor: pointer;
  transition: color 0.15s ease, background-color 0.15s ease, border-color 0.15s ease, box-shadow 0.15s ease, transform 0.15s ease;
  text-align: left;
}

.cdp__exam-item:hover {
  border-color: var(--ds-primary-border);
  background: var(--ds-primary-soft);
  transform: translateY(-1px);
}

.dark .cdp__exam-item {
  background: var(--ds-gray-700);
  border-color: var(--ds-border-strong);
}

.cdp__exam-item-main {
  display: flex;
  align-items: center;
  gap: 0.875rem;
  min-width: 0;
  flex: 1;
}

.cdp__exam-item-icon {
  width: 40px;
  height: 40px;
  border-radius: var(--ds-radius-lg);
  background: var(--ds-primary-soft);
  color: var(--ds-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.cdp__exam-item-copy {
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
  min-width: 0;
}

.cdp__exam-item-title {
  font-size: 0.9rem;
  color: var(--ds-text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.cdp__exam-item-meta {
  font-size: 0.78rem;
  color: var(--ds-text-muted);
}

.cdp__exam-item-side {
  display: flex;
  align-items: center;
  gap: 0.625rem;
  color: var(--ds-text-muted);
  flex-shrink: 0;
}

.cdp__exam-status {
  display: inline-flex;
  align-items: center;
  padding: 0.25rem 0.625rem;
  border-radius: 9999px;
  background: var(--ds-success-soft);
  color: var(--ds-success);
  font-size: 0.72rem;
  font-weight: 700;
}

.cdp__exam-status--draft {
  background: var(--ds-gray-100);
  color: var(--ds-text-secondary);
}

.cdp__card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  margin-bottom: 1.25rem;
  flex-wrap: wrap;
}

.cdp__card-title {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.9rem;
  font-weight: 800;
  color: var(--ds-text);
  margin: 0 0 1rem;
}

.dark .cdp__card-title { color: var(--ds-text); }

.cdp__card-header .cdp__card-title {
  margin: 0;
}

.cdp__sidebar .cdp__card-title {
  font-size: 0.8rem;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: var(--ds-text-muted);
  margin-bottom: 0.875rem;
}

.cdp__sidebar .cdp__card-title > :deep(svg) {
  width: 16px;
  height: 16px;
}

.cdp__badge {
  background: var(--ds-primary);
  color: white;
  font-size: 0.75rem;
  font-weight: 700;
  padding: 0.25rem 0.625rem;
  border-radius: 9999px;
  margin-left: 0.5rem;
}

/* Info List */
.cdp__info-list {
  display: flex;
  flex-direction: column;
  gap: 0.875rem;
}

.cdp__info-item {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  padding: 1rem 1.125rem;
  background: var(--ds-gray-50);
  border: 1px solid var(--ds-border);
  border-radius: var(--ds-radius-xl);
  transition: color 0.2s ease, background-color 0.2s ease, border-color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease;
}

.cdp__info-item:hover {
  border-color: var(--ds-primary-border);
  background: var(--ds-primary-soft);
}

.dark .cdp__info-item {
  background: var(--ds-gray-700);
  border-color: var(--ds-border-strong);
}

.cdp__info-item--full { grid-column: 1 / -1; }

.cdp__info-label {
  font-size: 0.7rem;
  font-weight: 700;
  color: var(--ds-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.06em;
}

.cdp__info-value {
  font-size: 0.9rem;
  font-weight: 700;
  color: var(--ds-text);
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.dark .cdp__info-value { color: var(--ds-text); }

.cdp__info-value--code {
  font-family: var(--ds-font-display);
  font-size: 1.25rem;
  color: var(--ds-primary);
  letter-spacing: 0.1em;
}

.cdp__copy-small {
  width: 28px;
  height: 28px;
  border: none;
  background: var(--ds-primary-soft);
  color: var(--ds-primary);
  border-radius: var(--ds-radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: color 0.12s ease, background-color 0.12s ease, border-color 0.12s ease, box-shadow 0.12s ease, transform 0.12s ease;
}

.cdp__copy-small:hover {
  background: var(--ds-primary);
  color: white;
}

/* Actions List */
.cdp__actions-list {
  display: flex;
  flex-direction: column;
  gap: 0.625rem;
}

.cdp__action-btn {
  display: flex;
  align-items: center;
  gap: 0.875rem;
  padding: 1rem 1.125rem;
  background: var(--ds-surface);
  border: 1px solid var(--ds-border);
  border-radius: var(--ds-radius-xl);
  cursor: pointer;
  transition: color 0.2s ease, background-color 0.2s ease, border-color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease;
  text-align: left;
  width: 100%;
}

.cdp__action-btn:hover {
  border-color: var(--ds-primary);
  background: var(--ds-primary-soft);
  transform: translateX(4px);
}

.dark .cdp__action-btn {
  background: var(--ds-gray-700);
  border-color: var(--ds-border-strong);
}

.dark .cdp__action-btn:hover {
  background: rgba(79, 70, 229, 0.15);
}

.cdp__action-btn > :deep(svg) {
  width: 20px;
  height: 20px;
  color: var(--ds-primary);
  flex-shrink: 0;
}

.cdp__action-btn > div {
  flex: 1;
}

.cdp__action-title {
  display: block;
  font-size: 0.875rem;
  font-weight: 700;
  color: var(--ds-text);
}

.dark .cdp__action-title { color: var(--ds-text); }

.cdp__action-desc {
  display: block;
  font-size: 0.75rem;
  color: var(--ds-text-muted);
  margin-top: 0.125rem;
}

.cdp__action-arrow {
  color: var(--ds-text-muted) !important;
}

/* Search */
.cdp__search {
  position: relative;
}

.cdp__search-icon {
  position: absolute;
  left: 0.875rem;
  top: 50%;
  transform: translateY(-50%);
  color: var(--ds-text-muted);
  width: 18px;
  height: 18px;
  pointer-events: none;
}

.cdp__search-input {
  width: 250px;
  padding: 0.625rem 1rem 0.625rem 2.75rem;
  border: 2px solid var(--ds-border);
  border-radius: var(--ds-radius-xl);
  font-size: 0.875rem;
  background: var(--ds-surface);
  color: var(--ds-text);
  transition: color 0.2s ease, background-color 0.2s ease, border-color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease;
}

.dark .cdp__search-input {
  background: var(--ds-gray-700);
  border-color: var(--ds-border-strong);
  color: var(--ds-text);
}

.cdp__search-input:focus {
  outline: none;
  border-color: var(--ds-primary);
  box-shadow: 0 0 0 4px var(--ds-primary-ring);
}

/* Table */
.cdp__loading-inline {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.75rem;
  padding: 3rem;
  color: var(--ds-text-muted);
}

.cdp__empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 3rem;
  gap: 0.75rem;
  text-align: center;
  color: var(--ds-text-muted);
}

.cdp__empty h4 {
  font-size: 1rem;
  font-weight: 700;
  color: var(--ds-text);
  margin: 0;
}

.dark .cdp__empty h4 { color: var(--ds-text); }

.cdp__inline-error {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 1rem 1.125rem;
  border: 1px solid rgba(220, 38, 38, 0.22);
  border-radius: var(--ds-radius-xl);
  background: var(--ds-danger-soft);
  color: var(--ds-danger);
}

.cdp__inline-error > div {
  flex: 1;
}

.cdp__inline-error-title,
.cdp__inline-error-text {
  margin: 0;
}

.cdp__inline-error-title {
  font-size: 0.875rem;
  font-weight: 700;
}

.cdp__inline-error-text {
  font-size: 0.8rem;
  margin-top: 0.125rem;
}

.dark .cdp__inline-error {
  background: rgba(220, 38, 38, 0.12);
  border-color: rgba(220, 38, 38, 0.35);
}

.cdp__table-wrapper {
  overflow-x: auto;
}

.cdp__table {
  width: 100%;
  border-collapse: separate;
  border-spacing: 0;
}

.cdp__table th {
  text-align: left;
  padding: 0.875rem 1rem;
  font-size: 0.75rem;
  font-weight: 700;
  color: var(--ds-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.06em;
  border-bottom: 2px solid var(--ds-border);
  background: var(--ds-gray-50);
}

.dark .cdp__table th {
  border-bottom-color: var(--ds-border-strong);
  background: var(--ds-gray-700);
}

.cdp__th-actions {
  width: 170px;
  text-align: right !important;
}

.cdp__table-row {
  transition: color 0.15s ease, background-color 0.15s ease, border-color 0.15s ease, box-shadow 0.15s ease, transform 0.15s ease;
}

.cdp__table-row:hover {
  background: var(--ds-gray-50);
}

.dark .cdp__table-row:hover {
  background: var(--ds-gray-700);
}

.cdp__table td {
  padding: 1rem;
  font-size: 0.875rem;
  color: var(--ds-text);
  border-bottom: 1px solid var(--ds-border);
}

.dark .cdp__table td {
  border-bottom-color: var(--ds-border-strong);
}

.cdp__td-muted {
  color: var(--ds-text-muted) !important;
}

.cdp__td-actions {
  text-align: right;
}

.cdp__icon-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  min-height: 36px;
  padding: 0.5rem 0.875rem;
  border-radius: var(--ds-radius-lg);
  border: 1px solid var(--ds-border);
  background: var(--ds-surface);
  color: var(--ds-text-secondary);
  cursor: pointer;
  transition: color 0.15s ease, background-color 0.15s ease, border-color 0.15s ease, box-shadow 0.15s ease, transform 0.15s ease;
}

.cdp__icon-btn:hover:not(:disabled) {
  border-color: var(--ds-danger);
  background: var(--ds-danger-soft);
  color: var(--ds-danger);
}

.cdp__icon-btn:disabled {
  opacity: 0.6;
  cursor: wait;
}

.cdp__icon-btn--danger {
  min-width: 124px;
}

.cdp__icon-btn--compact {
  min-width: 40px;
  width: 40px;
  padding: 0.5rem;
}

.dark .cdp__icon-btn {
  background: var(--ds-gray-800);
  border-color: var(--ds-border-strong);
  color: var(--ds-text-muted);
}

.cdp__student-info {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.cdp__student-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: var(--ds-primary-soft);
  color: var(--ds-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.8rem;
  font-weight: 700;
  flex-shrink: 0;
}

.dark .cdp__student-avatar {
  background: rgba(99, 102, 241, 0.2);
}

.cdp__student-name {
  font-weight: 600;
  color: var(--ds-text);
}

.dark .cdp__student-name { color: var(--ds-text); }

/* Pagination */
.cdp__pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  margin-top: 1.5rem;
  padding-top: 1rem;
  border-top: 1px solid var(--ds-border);
}

.dark .cdp__pagination {
  border-top-color: var(--ds-border-strong);
}

.cdp__page-btn {
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

.dark .cdp__page-btn {
  background: var(--ds-gray-800);
  border-color: var(--ds-border-strong);
  color: var(--ds-text-muted);
}

.cdp__page-btn:hover:not(:disabled) {
  border-color: var(--ds-primary);
  color: var(--ds-primary);
  background: var(--ds-primary-soft);
}

.cdp__page-btn:disabled { opacity: 0.4; cursor: not-allowed; }

.cdp__page-numbers {
  display: flex;
  align-items: center;
  gap: 0.25rem;
}

.cdp__page-num {
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

.cdp__page-num:hover:not(.cdp__page-num--active) {
  background: var(--ds-gray-100);
  color: var(--ds-text);
}

.dark .cdp__page-num:hover:not(.cdp__page-num--active) {
  background: var(--ds-gray-700);
  color: var(--ds-text);
}

.cdp__page-num--active {
  background: var(--ds-primary);
  color: white;
  border-color: var(--ds-primary);
}

/* Buttons */
.cdp__btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  padding: 0.75rem 1.25rem;
  border-radius: var(--ds-radius-xl);
  font-size: 0.875rem;
  font-weight: 700;
  cursor: pointer;
  transition: color 0.2s ease, background-color 0.2s ease, border-color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease;
  border: 1.5px solid transparent;
  white-space: nowrap;
}

.cdp__btn--primary {
  background: linear-gradient(135deg, var(--ds-primary) 0%, #6366f1 100%);
  color: white;
  box-shadow: 0 4px 16px rgba(79, 70, 229, 0.25);
}

.cdp__btn--primary:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(79, 70, 229, 0.35);
}

.cdp__btn--outline {
  background: var(--ds-surface);
  border-color: var(--ds-border);
  color: var(--ds-text-secondary);
}

.dark .cdp__btn--outline {
  background: var(--ds-gray-800);
  border-color: var(--ds-border-strong);
  color: var(--ds-text-muted);
}

.cdp__btn--outline:hover { border-color: var(--ds-primary); color: var(--ds-primary); }

.cdp__btn--secondary {
  background: var(--ds-surface);
  border-color: var(--ds-border);
  color: var(--ds-text-secondary);
}

.dark .cdp__btn--secondary {
  background: var(--ds-gray-700);
  border-color: var(--ds-border-strong);
  color: var(--ds-text-muted);
}

.cdp__btn--secondary:hover { border-color: var(--ds-primary); color: var(--ds-primary); }

.cdp__btn--danger {
  background: var(--ds-danger);
  color: white;
  border-color: var(--ds-danger);
}

.cdp__btn--danger:hover { background: #dc2626; }
.cdp__btn:disabled { opacity: 0.5; cursor: not-allowed; transform: none !important; }

/* Modal */
.cdp__modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.56);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: 1.5rem;
}

.cdp__modal {
  background: var(--ds-surface);
  border-radius: var(--ds-radius-2xl);
  width: 100%;
  max-width: 420px;
  box-shadow: 0 24px 64px rgba(0, 0, 0, 0.25);
  overflow: hidden;
}

.dark .cdp__modal { background: var(--ds-gray-800); }

.cdp__modal__header {
  display: flex;
  align-items: flex-start;
  gap: 0.875rem;
  padding: 1.5rem;
  border-bottom: 1px solid var(--ds-border);
}

.dark .cdp__modal__header { border-bottom-color: var(--ds-border-strong); }

.cdp__modal__icon {
  width: 44px;
  height: 44px;
  border-radius: var(--ds-radius-xl);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.cdp__modal__icon--danger {
  background: var(--ds-danger-soft);
  color: var(--ds-danger);
}

.cdp__modal__title {
  font-family: var(--ds-font-display);
  font-size: 1rem;
  font-weight: 800;
  color: var(--ds-text);
  margin: 0;
  line-height: 1.3;
}

.dark .cdp__modal__title { color: var(--ds-text); }

.cdp__modal__subtitle {
  font-size: 0.8rem;
  color: var(--ds-text-muted);
  margin: 0.25rem 0 0;
}

.cdp__modal__close {
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
  flex-shrink: 0;
}

.dark .cdp__modal__close { background: var(--ds-gray-700); }
.cdp__modal__close:hover { background: var(--ds-gray-200); color: var(--ds-text); }
.dark .cdp__modal__close:hover { background: var(--ds-gray-600); }

.cdp__modal__body {
  padding: 1.5rem;
  font-size: 0.875rem;
  color: var(--ds-text-secondary);
  line-height: 1.6;
}

.dark .cdp__modal__body { color: var(--ds-text-muted); }
.cdp__modal__body strong { color: var(--ds-text); }
.dark .cdp__modal__body strong { color: var(--ds-text); }

.cdp__modal__footer {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 0.75rem;
  padding: 1.25rem 1.5rem;
  border-top: 1px solid var(--ds-border);
  background: var(--ds-gray-50);
}

.dark .cdp__modal__footer {
  border-top-color: var(--ds-border-strong);
  background: var(--ds-gray-800);
}

/* Responsive */
@media (max-width: 768px) {
  .cdp {
    padding: 1rem;
    gap: 1rem;
  }

  .cdp__header {
    padding: 1rem;
  }

  .cdp__header-content {
    flex-direction: column;
    align-items: flex-start;
    gap: 0.75rem;
  }

  .cdp__header-icon {
    width: 40px;
    height: 40px;
  }

  .cdp__title {
    font-size: 1.125rem;
  }

  .cdp__search-input {
    width: 100%;
  }

  .cdp__card-actions {
    width: 100%;
  }

  .cdp__card-actions .cdp__search {
    width: 100%;
  }

  .cdp__grid {
    gap: 1rem;
  }

  .cdp__overview {
    flex-direction: column;
  }

  .cdp__overview-actions {
    width: 100%;
    justify-content: stretch;
  }

  .cdp__overview-actions .cdp__btn {
    flex: 1;
  }
}
</style>