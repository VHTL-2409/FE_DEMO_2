<template>
  <div class="qe">
    <div class="qe__section">

      <!-- Section header -->
      <div class="qe__section-header">
        <div class="qe__header-left">
          <div class="qe__section-icon">
            <LucideIcon name="quiz" />
          </div>
          <div>
            <h3 class="qe__section-title">Câu hỏi</h3>
            <p class="qe__section-desc">
              {{ localQuestions.length }} câu ·
              Tổng {{ totalScore }} điểm
            </p>
          </div>
        </div>

        <div class="qe__header-actions">
          <!-- Filter -->
          <div class="qe__filter" v-if="localQuestions.length > 0">
            <LucideIcon name="search" class="qe__filter-icon" />
            <input
              v-model="searchQuery"
              type="text"
              class="qe__filter-input"
              placeholder="Tìm câu hỏi..."
            />
          </div>

          <!-- Import -->
          <button type="button" class="qe__header-btn" @click="openImportMenu = !openImportMenu">
            <LucideIcon name="upload_file" />
            Nhập file
          </button>

          <!-- Add question -->
          <button type="button" class="qe__header-btn qe__header-btn--primary" @click="openAddModal">
            <LucideIcon name="add" />
            Thêm câu hỏi
          </button>
        </div>
      </div>

      <!-- Import menu -->
      <div v-if="openImportMenu" class="qe__import-menu">
        <div class="qe__import-header">
          <LucideIcon name="upload_file" />
          <strong>Nhập từ file</strong>
        </div>
        <div class="qe__import-body">
          <p class="qe__import-hint">Hỗ trợ định dạng: .csv, .xlsx, .json</p>
          <div class="qe__import-drop" :class="{ 'qe__import-drop--active': isDragOverFile }"
            @dragover.prevent="isDragOverFile = true"
            @dragleave="isDragOverFile = false"
            @drop.prevent="handleFileDrop"
          >
            <LucideIcon name="cloud_upload" />
            <p>Kéo thả file vào đây hoặc</p>
            <label class="qe__import-file-btn">
              Chọn file
              <input ref="fileInput" type="file" accept=".csv,.xlsx,.pdf,.docx,.json,.md,.markdown" class="qe__import-file-input" @change="handleFileSelect" />
            </label>
          </div>
          <div v-if="importError" class="qe__import-error">
            <LucideIcon name="error" />
            {{ importError }}
          </div>
          <div v-if="importLoading" class="qe__import-loading">
            <LucideIcon name="progress_activity" />
            Đang xử lý...
          </div>
        </div>
        <button type="button" class="qe__import-close" @click="openImportMenu = false">
          <LucideIcon name="close" />
        </button>
      </div>

      <!-- Stats bar -->
      <div v-if="localQuestions.length > 0" class="qe__stats-bar">
        <div class="qe__stats-item">
          <LucideIcon name="quiz" />
          <strong>{{ filteredQuestions.length }}</strong> câu
          <span v-if="searchQuery">(lọc từ {{ localQuestions.length }})</span>
        </div>
        <div class="qe__stats-item">
          <LucideIcon name="grade" />
          <strong>{{ totalScore }}</strong> điểm
        </div>
        <div class="qe__stats-item qe__stats-item--types">
          <span v-if="typeCount('SINGLE_CHOICE') > 0" class="qe__type-chip">
            {{ typeCount('SINGLE_CHOICE') }}x TN
          </span>
          <span v-if="typeCount('MULTIPLE_CHOICE') > 0" class="qe__type-chip qe__type-chip--info">
            {{ typeCount('MULTIPLE_CHOICE') }}x Nhiều đáp án
          </span>
          <span v-if="typeCount('ESSAY') > 0" class="qe__type-chip qe__type-chip--warning">
            {{ typeCount('ESSAY') }}x Tự luận
          </span>
        </div>
        <div v-if="searchQuery" class="qe__stats-clear" @click="searchQuery = ''">
          <LucideIcon name="close" />
          Xóa lọc
        </div>
      </div>

      <!-- Drag hint -->
      <div v-if="localQuestions.length > 1" class="qe__drag-hint">
        <LucideIcon name="drag_indicator" />
        Kéo thả để sắp xếp lại thứ tự câu hỏi
      </div>

      <!-- Questions list -->
      <div class="qe__list">
        <div v-if="localQuestions.length === 0" class="qe__empty">
          <LucideIcon name="quiz" />
          <p class="qe__empty-title">Chưa có câu hỏi nào</p>
          <p class="qe__empty-desc">Thêm câu hỏi hoặc nhập từ file để bắt đầu</p>
          <div class="qe__empty-actions">
            <button type="button" class="qe__empty-btn qe__empty-btn--primary" @click="openAddModal">
              <LucideIcon name="add" />
              Thêm câu hỏi
            </button>
            <button type="button" class="qe__empty-btn" @click="openImportMenu = true">
              <LucideIcon name="upload_file" />
              Nhập từ file
            </button>
          </div>
        </div>

        <!-- Draggable list -->
        <div v-else class="qe__items" ref="itemsContainer">
          <template v-for="(q, i) in filteredQuestions" :key="q.id || q._localId || i">
            <div
              v-if="getOriginalIndex(q) !== -1"
              :data-index="getOriginalIndex(q)"
              class="qe__item-wrapper"
            >
              <QuestionItemCard
                :question="q"
                :index="getOriginalIndex(q)"
                :draggable="true"
                @edit="handleEdit"
                @duplicate="handleDuplicate"
                @delete="handleDelete"
                @drag-start="onDragStart"
                @drag-end="onDragEnd"
                @drag-over="onDragOver"
                @drag-leave="onDragLeave"
                @drop="onDrop"
              />
            </div>
          </template>

          <div v-if="filteredQuestions.length === 0 && searchQuery" class="qe__no-results">
            <LucideIcon name="search_off" />
            <p>Không tìm thấy câu hỏi nào phù hợp</p>
            <button type="button" class="qe__no-results-btn" @click="searchQuery = ''">
              Xóa bộ lọc
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- Question Edit Modal -->
    <QuestionEditModal
      v-model="showQuestionModal"
      :question="editingQuestion"
      :exam-title="examTitle"
      @save="handleSaveQuestion"
    />

    <!-- Delete confirm -->
    <Teleport to="body">
      <Transition name="qe-confirm">
        <div v-if="showDeleteConfirm" class="qe-confirm-overlay" @click.self="showDeleteConfirm = false">
          <div class="qe-confirm">
            <div class="qe-confirm__icon qe-confirm__icon--danger">
              <LucideIcon name="delete" />
            </div>
            <h3 class="qe-confirm__title">Xóa câu hỏi</h3>
            <p class="qe-confirm__body">
              Xóa "<strong>{{ deleteTarget?.content?.substring(0, 50) }}{{ (deleteTarget?.content?.length || 0) > 50 ? '...' : '' }}</strong>"?
              Không thể hoàn tác.
            </p>
            <div class="qe-confirm__actions">
              <button type="button" class="qe-confirm__btn qe-confirm__btn--ghost" @click="showDeleteConfirm = false">
                Hủy
              </button>
              <button type="button" class="qe-confirm__btn qe-confirm__btn--danger" @click="confirmDelete">
                Xóa câu hỏi
              </button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<script setup>
import { ref, computed, reactive } from 'vue'
import { uploadImportJob, getImportJobStatus, confirmImportJob } from '../../../../services/importService'
import QuestionItemCard from './QuestionItemCard.vue'
import QuestionEditModal from './QuestionEditModal.vue'

const props = defineProps({
  questions: { type: Array, default: () => [] },
  examTitle: { type: String, default: '' }
})

const emit = defineEmits(['update:questions'])

const localQuestions = computed({
  get: () => props.questions,
  set: (v) => emit('update:questions', v)
})

// Modal state
const showQuestionModal = ref(false)
const editingQuestion = ref(null)

// Delete confirm
const showDeleteConfirm = ref(false)
const deleteTarget = ref(null)

// Search/filter
const searchQuery = ref('')

// Import menu
const openImportMenu = ref(false)
const isDragOverFile = ref(false)
const importLoading = ref(false)
const importError = ref('')
const fileInput = ref(null)
const itemsContainer = ref(null)

// Drag state
const draggedFromIndex = ref(null)
const dragOverIndex = ref(null)

// Filtered questions
const filteredQuestions = computed(() => {
  if (!searchQuery.value.trim()) return localQuestions.value
  const q = searchQuery.value.toLowerCase()
  return localQuestions.value.filter(item =>
    (item.content || '').toLowerCase().includes(q) ||
    (item.optionA || '').toLowerCase().includes(q) ||
    (item.optionB || '').toLowerCase().includes(q) ||
    (item.optionC || '').toLowerCase().includes(q) ||
    (item.optionD || '').toLowerCase().includes(q)
  )
})

// Stats
const totalScore = computed(() =>
  localQuestions.value.reduce((sum, q) => sum + (Number(q.scoreWeight || q.score || 1)), 0)
)

const typeCount = (type) =>
  localQuestions.value.filter(q => q.type === type).length

const getOriginalIndex = (q) => {
  return localQuestions.value.findIndex(item =>
    (item.id || item._localId) === (q.id || q._localId)
  )
}

// Modal handlers
const openAddModal = () => {
  editingQuestion.value = null
  showQuestionModal.value = true
}

const handleEdit = (question) => {
  editingQuestion.value = { ...question }
  showQuestionModal.value = true
}

const handleSaveQuestion = (question) => {
  if (question.id || question._localId) {
    // Update existing
    const idx = localQuestions.value.findIndex(q =>
      (q.id || q._localId) === (question.id || question._localId)
    )
    if (idx !== -1) {
      const updated = [...localQuestions.value]
      updated[idx] = {
        ...updated[idx],
        ...question
      }
      localQuestions.value = updated
    }
  } else {
    // Add new
    localQuestions.value = [...localQuestions.value, question]
  }
}

const handleDuplicate = (question) => {
  const idx = localQuestions.value.findIndex(q =>
    (q.id || q._localId) === (question.id || question._localId)
  )
  if (idx === -1) return
  const copy = {
    ...question,
    _localId: Date.now(),
    id: undefined,
    content: question.content + ' (bản sao)'
  }
  const updated = [...localQuestions.value]
  updated.splice(idx + 1, 0, copy)
  localQuestions.value = updated
}

const handleDelete = (question) => {
  deleteTarget.value = question
  showDeleteConfirm.value = true
}

const confirmDelete = () => {
  if (!deleteTarget.value) return
  const idx = localQuestions.value.findIndex(q =>
    (q.id || q._localId) === (deleteTarget.value.id || deleteTarget.value._localId)
  )
  if (idx !== -1) {
    const updated = [...localQuestions.value]
    updated.splice(idx, 1)
    localQuestions.value = updated
  }
  showDeleteConfirm.value = false
  deleteTarget.value = null
}

// Drag & drop reorder
const onDragStart = (e, index) => {
  draggedFromIndex.value = index
  e.dataTransfer.effectAllowed = 'move'
}

const onDragEnd = () => {
  draggedFromIndex.value = null
  dragOverIndex.value = null
}

const onDragOver = (e, index) => {
  dragOverIndex.value = index
  e.dataTransfer.dropEffect = 'move'
}

const onDragLeave = () => {
  dragOverIndex.value = null
}

const onDrop = (e, fromIndex, toIndex) => {
  if (fromIndex === toIndex) return
  const updated = [...localQuestions.value]
  const [moved] = updated.splice(fromIndex, 1)
  updated.splice(toIndex, 0, moved)
  localQuestions.value = updated
}

// File import
const handleFileDrop = async (e) => {
  isDragOverFile.value = false
  const file = e.dataTransfer.files[0]
  if (file) await processImportFile(file)
}

const handleFileSelect = async (e) => {
  const file = e.target.files[0]
  if (file) await processImportFile(file)
}

const processImportFile = async (file) => {
  importError.value = ''
  const validTypes = ['.csv', '.xlsx', '.pdf', '.docx', '.json']
  const ext = '.' + file.name.split('.').pop().toLowerCase()
  if (!validTypes.includes(ext)) {
    importError.value = 'Dinh dang khong ho tro. Chi chap nhan: .csv, .xlsx, .pdf, .docx, .json'
    return
  }

  importLoading.value = true
  try {
    const formData = new FormData()
    formData.append('file', file)
    const job = await uploadImportJob(file, {})
    const jobId = job.id || job.jobId

    // Poll for status
    let attempts = 0
    let status = null
    while (attempts < 20) {
      await new Promise(r => setTimeout(r, 1000))
      status = await getImportJobStatus(jobId)
      if (status.status === 'COMPLETED' || status.status === 'FAILED') break
      attempts++
    }

    if (status.status === 'COMPLETED') {
      await confirmImportJob(jobId, {})
      const questions = status.questions || []
      if (questions.length > 0) {
        const mapped = questions.map((q, idx) => ({
          _localId: Date.now() + idx,
          content: q.content || '',
          optionA: q.optionA || '',
          optionB: q.optionB || '',
          optionC: q.optionC || '',
          optionD: q.optionD || '',
          correctAnswer: q.correctAnswer || q.correctOption || '',
          score: Number(q.scoreWeight || q.score || 1),
          type: q.type || 'SINGLE_CHOICE'
        }))
        localQuestions.value = [...localQuestions.value, ...mapped]
        openImportMenu.value = false
      } else {
        importError.value = 'Không tìm thấy câu hỏi nào trong file.'
      }
    } else if (status.status === 'FAILED') {
      importError.value = status.error || 'Xử lý file thất bại.'
    } else {
      importError.value = 'Hết thời gian chờ. Vui lòng thử lại.'
    }
  } catch (err) {
    importError.value = err instanceof Error ? err.message : 'Lỗi khi xử lý file.'
  } finally {
    importLoading.value = false
    if (fileInput.value) fileInput.value.value = ''
  }
}
</script>


<style scoped>
.qe {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.qe__section {
  background: var(--ds-surface);
  border: 1px solid var(--ds-border);
  border-radius: var(--ds-radius-2xl);
  overflow: hidden;
}

.dark .qe__section {
  border-color: var(--ds-border-strong);
}

/* Section header */
.qe__section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  padding: 1.25rem 1.5rem;
  border-bottom: 1px solid var(--ds-border);
  background: linear-gradient(135deg, var(--ds-primary-soft) 0%, transparent 100%);
  flex-wrap: wrap;
}

.dark .qe__section-header {
  border-bottom-color: var(--ds-border-strong);
}

.qe__header-left {
  display: flex;
  align-items: center;
  gap: 0.875rem;
}

.qe__section-icon {
  width: 40px;
  height: 40px;
  border-radius: var(--ds-radius-lg);
  background: var(--ds-primary);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  box-shadow: 0 4px 12px rgba(79, 70, 229, 0.25);
}


.qe__section-title {
  font-family: var(--ds-font-display);
  font-size: 1.05rem;
  font-weight: 700;
  color: var(--ds-text);
  margin: 0;
  line-height: 1.2;
}

.dark .qe__section-title { color: #f1f5f9; }

.qe__section-desc {
  font-size: 0.75rem;
  color: var(--ds-text-muted);
  margin: 0.25rem 0 0;
}

.qe__header-actions {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex-wrap: wrap;
}

/* Filter */
.qe__filter {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 0.875rem;
  background: var(--ds-gray-50);
  border: 1.5px solid var(--ds-border);
  border-radius: var(--ds-radius-xl);
  min-width: 180px;
}

.dark .qe__filter {
  background: var(--ds-gray-800);
  border-color: var(--ds-border-strong);
}

.qe__filter-icon {
  font-size: 1rem;
  color: var(--ds-text-muted);
  flex-shrink: 0;
}

.qe__filter-input {
  flex: 1;
  border: none;
  background: transparent;
  outline: none;
  font-size: 0.8rem;
  color: var(--ds-text);
  min-width: 0;
}

.dark .qe__filter-input { color: #f1f5f9; }
.qe__filter-input::placeholder { color: var(--ds-text-muted); }

/* Header buttons */
.qe__header-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.375rem;
  padding: 0.5rem 0.875rem;
  border-radius: var(--ds-radius-full);
  border: 1.5px solid var(--ds-border);
  background: var(--ds-surface);
  color: var(--ds-text-secondary);
  font-size: 0.8rem;
  font-weight: 700;
  cursor: pointer;
  transition: color 0.15s ease, background-color 0.15s ease, border-color 0.15s ease, box-shadow 0.15s ease, transform 0.15s ease;
}

.dark .qe__header-btn { background: var(--ds-gray-800); border-color: var(--ds-border-strong); color: #94a3b8; }

.qe__header-btn:hover {
  border-color: var(--ds-primary);
  color: var(--ds-primary);
  background: var(--ds-primary-soft);
}

.dark .qe__header-btn:hover { background: rgba(79, 70, 229, 0.15); }


.qe__header-btn--primary {
  background: var(--ds-primary);
  border-color: var(--ds-primary);
  color: white;
  box-shadow: 0 4px 12px rgba(79, 70, 229, 0.25);
}

.qe__header-btn--primary:hover {
  background: #6366f1;
  color: white;
  transform: translateY(-1px);
  box-shadow: 0 6px 20px rgba(79, 70, 229, 0.35);
}

/* Import menu */
.qe__import-menu {
  position: relative;
  padding: 1.25rem 1.5rem;
  background: var(--ds-gray-50);
  border-bottom: 1px solid var(--ds-border);
  display: flex;
  align-items: flex-start;
  gap: 1rem;
}

.dark .qe__import-menu { background: var(--ds-gray-800); border-bottom-color: var(--ds-border-strong); }

.qe__import-header {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.875rem;
  font-weight: 700;
  color: var(--ds-text);
  min-width: 120px;
}

.dark .qe__import-header { color: #f1f5f9; }

.qe__import-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.qe__import-hint {
  font-size: 0.75rem;
  color: var(--ds-text-muted);
  margin: 0;
}

.qe__import-drop {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.5rem;
  padding: 1.5rem;
  border: 2px dashed var(--ds-border);
  border-radius: var(--ds-radius-xl);
  background: var(--ds-surface);
  text-align: center;
  color: var(--ds-text-muted);
  font-size: 0.875rem;
  transition: color 0.15s ease, background-color 0.15s ease, border-color 0.15s ease, box-shadow 0.15s ease, transform 0.15s ease;
}

.dark .qe__import-drop { background: var(--ds-gray-700); border-color: var(--ds-border-strong); }

.qe__import-drop--active {
  border-color: var(--ds-primary);
  background: var(--ds-primary-soft);
  color: var(--ds-primary);
}

.qe__import-drop p { margin: 0; font-weight: 600; }

.qe__import-file-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.375rem;
  padding: 0.5rem 1rem;
  border-radius: var(--ds-radius-full);
  border: 1.5px solid var(--ds-primary);
  color: var(--ds-primary);
  font-size: 0.8rem;
  font-weight: 700;
  cursor: pointer;
  transition: color 0.15s ease, background-color 0.15s ease, border-color 0.15s ease, box-shadow 0.15s ease, transform 0.15s ease;
  margin-top: 0.25rem;
}

.qe__import-file-btn:hover { background: var(--ds-primary); color: white; }

.qe__import-file-input {
  display: none;
}

.qe__import-error {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 0.75rem;
  background: var(--ds-danger-soft);
  border-radius: var(--ds-radius-lg);
  font-size: 0.8rem;
  font-weight: 600;
  color: var(--ds-danger);
}


.qe__import-loading {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.8rem;
  font-weight: 600;
  color: var(--ds-primary);
}

.qe__spin { animation: spin 1s linear infinite; }
@keyframes spin { to { transform: rotate(360deg) translateZ(0); } }

.qe__import-close {
  position: absolute;
  top: 0.75rem;
  right: 0.75rem;
  width: 1.75rem;
  height: 1.75rem;
  border: none;
  border-radius: var(--ds-radius-md);
  background: var(--ds-gray-100);
  color: var(--ds-text-muted);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: color 0.12s ease, background-color 0.12s ease, border-color 0.12s ease, box-shadow 0.12s ease, transform 0.12s ease;
}

.dark .qe__import-close { background: var(--ds-gray-700); }
.qe__import-close:hover { background: var(--ds-gray-200); color: var(--ds-text); }
.dark .qe__import-close:hover { background: var(--ds-gray-600); }

/* Stats bar */
.qe__stats-bar {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 0.75rem 1.5rem;
  background: var(--ds-gray-50);
  border-bottom: 1px solid var(--ds-border);
  flex-wrap: wrap;
}

.dark .qe__stats-bar { background: var(--ds-gray-800); border-bottom-color: var(--ds-border-strong); }

.qe__stats-item {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  font-size: 0.8rem;
  color: var(--ds-text-secondary);
  font-weight: 600;
}

.dark .qe__stats-item { color: #94a3b8; }

.qe__stats-item strong { color: var(--ds-text); font-family: var(--ds-font-display); }
.dark .qe__stats-item strong { color: #f1f5f9; }

.qe__stats-item--types {
  flex-wrap: wrap;
  gap: 0.375rem;
}

.qe__type-chip {
  display: inline-flex;
  padding: 0.2rem 0.5rem;
  background: var(--ds-primary-soft);
  color: var(--ds-primary);
  border-radius: var(--ds-radius-full);
  font-size: 0.7rem;
  font-weight: 700;
}

.qe__type-chip--info {
  background: var(--ds-info-soft);
  color: var(--ds-info);
}

.qe__type-chip--warning {
  background: rgba(234, 179, 8, 0.1);
  color: #d97706;
}

.qe__stats-clear {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  margin-left: auto;
  font-size: 0.75rem;
  font-weight: 700;
  color: var(--ds-primary);
  cursor: pointer;
  padding: 0.25rem 0.5rem;
  border-radius: var(--ds-radius-md);
  transition: color 0.12s ease, background-color 0.12s ease, border-color 0.12s ease, box-shadow 0.12s ease, transform 0.12s ease;
}

.qe__stats-clear:hover { background: var(--ds-primary-soft); }

/* Drag hint */
.qe__drag-hint {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 1.5rem;
  font-size: 0.75rem;
  color: var(--ds-text-muted);
  font-weight: 500;
  font-style: italic;
  border-bottom: 1px solid var(--ds-border);
  background: rgba(79, 70, 229, 0.03);
}

.dark .qe__drag-hint { border-bottom-color: var(--ds-border-strong); background: rgba(79, 70, 229, 0.05); }

/* List */
.qe__list {
  display: flex;
  flex-direction: column;
}

.qe__items {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  padding: 0.75rem 1rem;
}

.qe__item-wrapper {
  display: block;
}

/* Empty state */
.qe__empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.75rem;
  padding: 4rem 2rem;
  text-align: center;
}

.qe__empty-icon {
  font-size: 4rem !important;
  opacity: 0.2;
  color: var(--ds-primary);
}

.qe__empty-title {
  font-family: var(--ds-font-display);
  font-size: 1.125rem;
  font-weight: 800;
  color: var(--ds-text);
  margin: 0;
}

.dark .qe__empty-title { color: #f1f5f9; }

.qe__empty-desc {
  font-size: 0.875rem;
  color: var(--ds-text-muted);
  margin: 0;
  max-width: 300px;
}

.qe__empty-actions {
  display: flex;
  gap: 0.75rem;
  margin-top: 0.5rem;
  flex-wrap: wrap;
  justify-content: center;
}

.qe__empty-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.625rem 1.25rem;
  border-radius: var(--ds-radius-xl);
  border: 1.5px solid var(--ds-border);
  background: var(--ds-surface);
  color: var(--ds-text-secondary);
  font-size: 0.875rem;
  font-weight: 700;
  cursor: pointer;
  transition: color 0.15s ease, background-color 0.15s ease, border-color 0.15s ease, box-shadow 0.15s ease, transform 0.15s ease;
  font-family: inherit;
}

.dark .qe__empty-btn { background: var(--ds-gray-800); border-color: var(--ds-border-strong); color: #94a3b8; }
.qe__empty-btn:hover { border-color: var(--ds-primary); color: var(--ds-primary); background: var(--ds-primary-soft); }
.dark .qe__empty-btn:hover { background: rgba(79, 70, 229, 0.15); }


.qe__empty-btn--primary {
  background: var(--ds-primary);
  border-color: var(--ds-primary);
  color: white;
  box-shadow: 0 4px 12px rgba(79, 70, 229, 0.25);
}

.qe__empty-btn--primary:hover {
  background: #6366f1;
  color: white;
  transform: translateY(-1px);
  box-shadow: 0 6px 20px rgba(79, 70, 229, 0.35);
}

/* No results */
.qe__no-results {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.5rem;
  padding: 3rem 2rem;
  color: var(--ds-text-muted);
  text-align: center;
}

.qe__no-results p { margin: 0; font-weight: 600; }

.qe__no-results-btn {
  margin-top: 0.5rem;
  padding: 0.5rem 1rem;
  border-radius: var(--ds-radius-full);
  border: 1.5px solid var(--ds-primary);
  background: var(--ds-primary-soft);
  color: var(--ds-primary);
  font-size: 0.8rem;
  font-weight: 700;
  cursor: pointer;
  transition: color 0.15s ease, background-color 0.15s ease, border-color 0.15s ease, box-shadow 0.15s ease, transform 0.15s ease;
  font-family: inherit;
}

.qe__no-results-btn:hover { background: var(--ds-primary); color: white; }

/* Delete confirm */
.qe-confirm-overlay {
  position: fixed;
  inset: 0;
  z-index: 2000;
  background: rgba(15, 23, 42, 0.56);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 1.5rem;
}

.qe-confirm {
  background: white;
  border-radius: var(--ds-radius-2xl);
  width: 100%;
  max-width: 400px;
  padding: 2rem;
  text-align: center;
  box-shadow: 0 24px 64px rgba(0, 0, 0, 0.2);
}

.dark .qe-confirm { background: var(--ds-gray-800); }

.qe-confirm__icon {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 1rem;
}

.qe-confirm__icon--danger {
  background: var(--ds-danger-soft);
  color: var(--ds-danger);
}


.qe-confirm__title {
  font-family: var(--ds-font-display);
  font-size: 1.125rem;
  font-weight: 800;
  color: var(--ds-text);
  margin: 0 0 0.5rem;
}

.dark .qe-confirm__title { color: #f1f5f9; }

.qe-confirm__body {
  font-size: 0.875rem;
  color: var(--ds-text-secondary);
  margin: 0 0 1.5rem;
  line-height: 1.6;
}

.dark .qe-confirm__body { color: #94a3b8; }
.qe-confirm__body strong { color: var(--ds-text); }
.dark .qe-confirm__body strong { color: #f1f5f9; }

.qe-confirm__actions {
  display: flex;
  gap: 0.75rem;
  justify-content: center;
}

.qe-confirm__btn {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.625rem 1.25rem;
  border-radius: var(--ds-radius-xl);
  font-size: 0.875rem;
  font-weight: 700;
  cursor: pointer;
  transition: color 0.15s ease, background-color 0.15s ease, border-color 0.15s ease, box-shadow 0.15s ease, transform 0.15s ease;
  border: 1px solid transparent;
  font-family: inherit;
}

.qe-confirm__btn--ghost {
  background: transparent;
  border-color: var(--ds-border);
  color: var(--ds-text-secondary);
}

.dark .qe-confirm__btn--ghost { border-color: var(--ds-border-strong); color: #94a3b8; }
.qe-confirm__btn--ghost:hover { border-color: var(--ds-gray-300); color: var(--ds-text); }
.dark .qe-confirm__btn--ghost:hover { background: var(--ds-gray-700); }

.qe-confirm__btn--danger {
  background: var(--ds-danger);
  color: white;
  box-shadow: 0 4px 12px rgba(220, 38, 38, 0.25);
}

.qe-confirm__btn--danger:hover {
  background: #dc2626;
  transform: translateY(-1px);
  box-shadow: 0 6px 20px rgba(220, 38, 38, 0.35);
}

/* Transition */
.qe-confirm-enter-active, .qe-confirm-leave-active { transition: color 0.2s ease, background-color 0.2s ease, border-color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease; }
.qe-confirm-enter-from, .qe-confirm-leave-to { opacity: 0; }
.qe-confirm-enter-from .qe-confirm, .qe-confirm-leave-to .qe-confirm { transform: scale(0.9); }
</style>