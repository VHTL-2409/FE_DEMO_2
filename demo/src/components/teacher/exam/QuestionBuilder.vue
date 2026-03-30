<template>
  <div class="ec-section">
    <div class="ec-section__header">
      <div class="ec-section__icon">
        <LucideIcon name="quiz" />
      </div>
      <div>
        <h3 class="ec-section__title">Câu hỏi</h3>
        <p class="ec-section__desc">Thêm câu hỏi từ file hoặc nhập thủ công</p>
      </div>
    </div>

    <div class="ec-section__body">

      <!-- Import source tabs -->
      <div class="ec-qb-tabs">
        <button
          type="button"
          :class="['ec-qb-tab', activeTab === 'file' && 'ec-qb-tab--active']"
          @click="activeTab = 'file'"
        >
          <LucideIcon name="upload_file" />
          Nhập từ file
        </button>
        <button
          type="button"
          :class="['ec-qb-tab', activeTab === 'manual' && 'ec-qb-tab--active']"
          @click="activeTab = 'manual'"
        >
          <LucideIcon name="edit_note" />
          Nhập thủ công
        </button>
      </div>

      <!-- File import tab -->
      <div v-if="activeTab === 'file'" class="ec-qb-file">
        <!-- Drop zone -->
        <div
          class="ec-dropzone"
          :class="{ 'ec-dropzone--dragover': isDragOver, 'ec-dropzone--has-file': selectedFile }"
          @dragover.prevent="isDragOver = true"
          @dragleave="isDragOver = false"
          @drop.prevent="onDrop"
          @click="triggerFileInput"
        >
          <input
            ref="fileInput"
            type="file"
            accept=".csv,.xlsx,.pdf,.docx,.json,.md,.markdown"
            class="hidden"
            @change="onFileChange"
          />
          <div v-if="!selectedFile" class="ec-dropzone__empty">
            <div class="ec-dropzone__icon">
              <LucideIcon name="cloud_upload" />
            </div>
            <p class="ec-dropzone__title">Kéo thả file hoặc nhấn để chọn</p>
            <p class="ec-dropzone__sub">CSV, XLSX, PDF, Word · Tối đa 10MB</p>
            <div class="ec-dropzone__templates">
              <a :href="templateCsvUrl" download class="ec-dropzone__tpl">
                <LucideIcon name="download" />
                Mẫu CSV
              </a>
              <a :href="templateXlsxUrl" download class="ec-dropzone__tpl">
                <LucideIcon name="download" />
                Mẫu Excel
              </a>
            </div>
          </div>
          <div v-else class="ec-dropzone__file">
            <div class="ec-dropzone__file-icon">
              <LucideIcon name="description" />
            </div>
            <div class="ec-dropzone__file-info">
              <p class="ec-dropzone__file-name">{{ selectedFile.name }}</p>
              <p class="ec-dropzone__file-size">{{ fileSizeLabel }}</p>
            </div>
            <button type="button" class="ec-dropzone__file-remove" @click.stop="removeFile">
              <LucideIcon name="close" />
            </button>
          </div>
        </div>

        <!-- Import button -->
        <button
          type="button"
          class="ec-btn ec-btn--primary"
          :disabled="!selectedFile || isImporting"
          @click="handleImport"
        >
          <LucideIcon name="progress_activity" v-if="isImporting"  ec-spin/>
          <LucideIcon name="upload" v-else />
          {{ isImporting ? 'Đang xử lý...' : 'Nhập câu hỏi' }}
        </button>

        <!-- Error message -->
        <div v-if="importError" class="ec-qb-error">
          <LucideIcon name="error" />
          {{ importError }}
        </div>
      </div>

      <!-- Manual entry tab -->
      <div v-if="activeTab === 'manual'" class="ec-qb-manual">
        <!-- New question form -->
        <div class="ec-qb-form">
          <div class="ec-qb-form__row">
            <div class="ec-field">
              <label class="ec-field__label">
                Nội dung câu hỏi
                <span class="ec-field__required">*</span>
              </label>
              <textarea
                v-model="newQ.content"
                rows="2"
                class="ec-input ec-input--textarea"
                placeholder="Nhập nội dung câu hỏi..."
              />
            </div>
          </div>

          <div class="ec-qb-form__options">
            <div v-for="opt in ['A','B','C','D']" :key="opt" class="ec-qb-opt">
              <span
                class="ec-qb-opt__badge"
                :class="newQ.correctAnswer === opt && 'ec-qb-opt__badge--correct'"
              >{{ opt }}</span>
              <input
                v-model="newQ[`option${opt}`]"
                type="text"
                class="ec-input"
                :placeholder="`Đáp án ${opt}`"
              />
            </div>
          </div>

          <div class="ec-qb-form__meta">
            <div class="ec-field ec-field--inline">
              <label class="ec-field__label">Đáp án đúng</label>
              <div class="ec-qb-correct">
                <button
                  v-for="opt in ['A','B','C','D']"
                  :key="opt"
                  type="button"
                  class="ec-qb-correct-btn"
                  :class="newQ.correctAnswer === opt && 'ec-qb-correct-btn--active'"
                  @click="newQ.correctAnswer = opt"
                >
                  {{ opt }}
                </button>
              </div>
            </div>
            <div class="ec-field ec-field--inline">
              <label class="ec-field__label">Điểm</label>
              <input
                v-model.number="newQ.score"
                type="number"
                min="0.5"
                max="10"
                step="0.5"
                class="ec-input ec-input--sm"
              />
            </div>
          </div>

          <button
            type="button"
            class="ec-btn ec-btn--primary ec-btn--sm"
            :disabled="!isFormValid"
            @click="addQuestion"
          >
            <LucideIcon name="add" />
            Thêm câu hỏi
          </button>
        </div>

        <!-- Questions list -->
        <div class="ec-qb-list">
          <div class="ec-qb-list__header">
            <h4 class="ec-qb-list__title">
              <LucideIcon name="list" />
              Danh sách câu hỏi
              <span class="ec-qb-list__count">{{ localQuestions.length }}</span>
            </h4>
          </div>

          <div v-if="localQuestions.length === 0" class="ec-qb-empty">
            <LucideIcon name="quiz" />
            <p>Chưa có câu hỏi nào</p>
            <p>Điền form trên để thêm câu hỏi mới</p>
          </div>

          <div v-else class="ec-qb-items">
            <div
              v-for="(q, i) in localQuestions"
              :key="q._localId"
              class="ec-qb-item"
            >
              <span class="ec-qb-item__num">{{ i + 1 }}</span>
              <div class="ec-qb-item__body">
                <p class="ec-qb-item__content">{{ q.content }}</p>
                <p class="ec-qb-item__meta">
                  <span class="ec-qb-item__correct">Đúng: {{ q.correctAnswer }}</span>
                  <span>{{ q.score }} điểm</span>
                </p>
              </div>
              <button type="button" class="ec-qb-item__delete" @click="removeQuestion(i)">
                <LucideIcon name="delete" />
              </button>
            </div>
          </div>
        </div>
      </div>

    </div>
  </div>
</template>

<script setup>
import { ref, computed, reactive } from 'vue'
import { FILE_FORMAT_DESC, getTemplateDownloadUrl, importQuestionsFromFile } from '../../../services/questionService'
import { useToast } from '../../../composables/useToast'
import { API_BASE_URL } from '../../../services/apiClient'

const props = defineProps({
  questions: { type: Array, default: () => [] }
})

const emit = defineEmits(['update:questions'])

const toast = useToast()
const activeTab = ref('file')
const isDragOver = ref(false)
const selectedFile = ref(null)
const fileInput = ref(null)
const isImporting = ref(false)
const importError = ref('')

const templateCsvUrl = `${API_BASE_URL}/api/questions/template`
const templateXlsxUrl = `${API_BASE_URL}/api/questions/template?format=xlsx`

const localQuestions = computed({
  get: () => props.questions,
  set: (v) => emit('update:questions', v)
})

const newQ = reactive({
  content: '',
  optionA: '',
  optionB: '',
  optionC: '',
  optionD: '',
  correctAnswer: '',
  score: 1,
  _localId: 0
})

const isFormValid = computed(() =>
  newQ.content.trim() &&
  newQ.optionA.trim() &&
  newQ.optionB.trim() &&
  newQ.optionC.trim() &&
  newQ.optionD.trim() &&
  newQ.correctAnswer
)

const fileSizeLabel = computed(() => {
  if (!selectedFile.value) return ''
  const mb = selectedFile.value.size / (1024 * 1024)
  return `${mb.toFixed(2)} MB`
})

let localIdCounter = 1

const addQuestion = () => {
  if (!isFormValid.value) return
  localQuestions.value = [...localQuestions.value, {
    _localId: localIdCounter++,
    content: newQ.content.trim(),
    optionA: newQ.optionA.trim(),
    optionB: newQ.optionB.trim(),
    optionC: newQ.optionC.trim(),
    optionD: newQ.optionD.trim(),
    correctAnswer: newQ.correctAnswer,
    score: Number(newQ.score) || 1
  }]
  newQ.content = ''
  newQ.optionA = ''
  newQ.optionB = ''
  newQ.optionC = ''
  newQ.optionD = ''
  newQ.correctAnswer = ''
  newQ.score = 1
  toast.success('Đã thêm câu hỏi.')
}

const removeQuestion = (index) => {
  const updated = [...localQuestions.value]
  updated.splice(index, 1)
  localQuestions.value = updated
}

const triggerFileInput = () => fileInput.value?.click()

const onDrop = (e) => {
  isDragOver.value = false
  const file = e.dataTransfer?.files?.[0]
  if (file) setFile(file)
}

const onFileChange = (e) => {
  const file = e.target.files?.[0]
  if (file) setFile(file)
}

const setFile = (file) => {
  importError.value = ''
  const ext = file.name.toLowerCase().slice(-5)
  const allowed = ['.csv', '.xlsx', '.pdf', '.docx']
  if (!allowed.some(e => ext.endsWith(e))) {
    importError.value = 'Định dạng không hỗ trợ. Vui lòng chọn CSV, XLSX, PDF hoặc Word.'
    return
  }
  if (file.size > 10 * 1024 * 1024) {
    importError.value = 'File vượt quá 10MB.'
    return
  }
  selectedFile.value = file
}

const removeFile = () => {
  selectedFile.value = null
  importError.value = ''
  if (fileInput.value) fileInput.value.value = ''
}

const handleImport = async () => {
  if (!selectedFile.value) return
  isImporting.value = true
  importError.value = ''
  try {
    // Preview: just count questions from file name hint
    // In real flow, this would call an import API
    toast.success(`Đã chọn file: ${selectedFile.value.name}`)
  } catch {
    importError.value = 'Không thể nhập file. Vui lòng thử lại.'
  } finally {
    isImporting.value = false
  }
}
</script>


<style scoped>
.ec-section {
  background: var(--ds-surface);
  border: 1px solid var(--ds-border);
  border-radius: var(--ds-radius-2xl);
  overflow: hidden;
}

.ec-section__header {
  display: flex;
  align-items: center;
  gap: 0.875rem;
  padding: 1.25rem 1.5rem;
  border-bottom: 1px solid var(--ds-border);
  background: linear-gradient(135deg, var(--ds-primary-soft) 0%, transparent 100%);
}

.ec-section__icon {
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


.ec-section__title {
  font-family: var(--ds-font-display);
  font-size: 1.05rem;
  font-weight: 700;
  color: var(--ds-text);
  margin: 0;
  line-height: 1.2;
}

.dark .ec-section__title { color: #f1f5f9; }

.ec-section__desc {
  font-size: 0.75rem;
  color: var(--ds-text-muted);
  margin: 0.25rem 0 0;
}

.ec-section__body {
  padding: 1.5rem;
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

/* Tabs */
.ec-qb-tabs {
  display: flex;
  gap: 0.25rem;
  padding: 0.25rem;
  background: var(--ds-gray-50);
  border-radius: var(--ds-radius-xl);
  border: 1px solid var(--ds-border);
}

.dark .ec-qb-tabs { background: var(--ds-gray-800); }

.ec-qb-tab {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  padding: 0.625rem 1rem;
  border-radius: var(--ds-radius-lg);
  border: none;
  background: transparent;
  font-size: 0.8rem;
  font-weight: 600;
  color: var(--ds-text-muted);
  cursor: pointer;
  transition: all 0.15s ease;
}


.ec-qb-tab:hover { color: var(--ds-text); background: var(--ds-gray-100); }

.dark .ec-qb-tab:hover { background: var(--ds-gray-700); }

.ec-qb-tab--active {
  background: white;
  color: var(--ds-primary);
  box-shadow: 0 1px 4px rgba(0,0,0,0.08);
}

.dark .ec-qb-tab--active { background: var(--ds-gray-700); }

/* Dropzone */
.ec-dropzone {
  border: 2px dashed var(--ds-border);
  border-radius: var(--ds-radius-2xl);
  padding: 2rem;
  cursor: pointer;
  transition: all 0.2s ease;
  background: var(--ds-gray-50);
  text-align: center;
}

.dark .ec-dropzone { background: var(--ds-gray-800); }

.ec-dropzone:hover,
.ec-dropzone--dragover {
  border-color: var(--ds-primary);
  background: var(--ds-primary-soft);
}

.ec-dropzone--has-file {
  border-style: solid;
  border-color: var(--ds-primary-border);
  background: var(--ds-primary-soft);
  cursor: default;
}

.ec-dropzone__empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.5rem;
}

.ec-dropzone__icon {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background: var(--ds-primary-soft);
  color: var(--ds-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 0.5rem;
}


.ec-dropzone__title {
  font-size: 0.9rem;
  font-weight: 700;
  color: var(--ds-text);
  margin: 0;
}

.dark .ec-dropzone__title { color: #f1f5f9; }

.ec-dropzone__sub {
  font-size: 0.75rem;
  color: var(--ds-text-muted);
  margin: 0;
}

.ec-dropzone__templates {
  display: flex;
  gap: 0.75rem;
  margin-top: 0.75rem;
}

.ec-dropzone__tpl {
  display: inline-flex;
  align-items: center;
  gap: 0.375rem;
  padding: 0.375rem 0.875rem;
  border-radius: var(--ds-radius-full);
  border: 1.5px solid var(--ds-primary-border);
  background: var(--ds-primary-soft);
  color: var(--ds-primary);
  font-size: 0.75rem;
  font-weight: 700;
  text-decoration: none;
  transition: all 0.15s ease;
}

.ec-dropzone__tpl:hover { background: var(--ds-primary); color: white; }


.ec-dropzone__file {
  display: flex;
  align-items: center;
  gap: 0.875rem;
}

.ec-dropzone__file-icon {
  width: 48px;
  height: 48px;
  border-radius: var(--ds-radius-lg);
  background: var(--ds-primary);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}


.ec-dropzone__file-info { flex: 1; text-align: left; }

.ec-dropzone__file-name {
  font-size: 0.875rem;
  font-weight: 700;
  color: var(--ds-text);
  margin: 0;
}

.dark .ec-dropzone__file-name { color: #f1f5f9; }

.ec-dropzone__file-size {
  font-size: 0.75rem;
  color: var(--ds-text-muted);
  margin: 0.25rem 0 0;
}

.ec-dropzone__file-remove {
  width: 2rem;
  height: 2rem;
  border-radius: 50%;
  border: none;
  background: var(--ds-gray-200);
  color: var(--ds-text-muted);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.15s ease;
}

.ec-dropzone__file-remove:hover { background: var(--ds-danger-soft); color: var(--ds-danger); }

/* Error */
.ec-qb-error {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem 1rem;
  background: var(--ds-danger-soft);
  border: 1px solid rgba(220,38,38,0.2);
  border-radius: var(--ds-radius-xl);
  font-size: 0.8rem;
  color: var(--ds-danger);
  font-weight: 600;
}


/* Manual form */
.ec-qb-form {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  padding: 1.25rem;
  background: var(--ds-gray-50);
  border: 1px solid var(--ds-border);
  border-radius: var(--ds-radius-2xl);
}

.dark .ec-qb-form { background: var(--ds-gray-800); border-color: var(--ds-border-strong); }

.ec-qb-form__row { display: flex; flex-direction: column; gap: 0.75rem; }

.ec-qb-form__options {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0.75rem;
}

@media (max-width: 480px) {
  .ec-qb-form__options { grid-template-columns: 1fr; }
}

.ec-qb-opt {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.ec-qb-opt__badge {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: var(--ds-gray-200);
  color: var(--ds-text-muted);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.7rem;
  font-weight: 800;
  flex-shrink: 0;
  transition: all 0.15s ease;
}

.ec-qb-opt__badge--correct {
  background: var(--ds-success);
  color: white;
}

.dark .ec-qb-opt__badge { background: var(--ds-gray-700); }

.ec-qb-form__meta {
  display: flex;
  align-items: center;
  gap: 1rem;
  flex-wrap: wrap;
}

.ec-field {
  display: flex;
  flex-direction: column;
  gap: 0.375rem;
}

.ec-field--inline {
  flex-direction: row;
  align-items: center;
  gap: 0.75rem;
}

.ec-field__label {
  font-size: 0.8rem;
  font-weight: 700;
  color: var(--ds-text);
  white-space: nowrap;
}

.dark .ec-field__label { color: #f1f5f9; }

.ec-field__required { color: var(--ds-danger); }

.ec-qb-correct {
  display: flex;
  gap: 0.375rem;
}

.ec-qb-correct-btn {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: 1.5px solid var(--ds-border);
  background: var(--ds-surface);
  color: var(--ds-text-muted);
  font-size: 0.75rem;
  font-weight: 800;
  cursor: pointer;
  transition: all 0.15s ease;
}

.dark .ec-qb-correct-btn { background: var(--ds-gray-800); border-color: var(--ds-border-strong); }

.ec-qb-correct-btn--active {
  background: var(--ds-success);
  border-color: var(--ds-success);
  color: white;
}

.ec-qb-correct-btn:hover:not(.ec-qb-correct-btn--active) {
  border-color: var(--ds-success);
  color: var(--ds-success);
}

.ec-input {
  flex: 1;
  padding: 0.5625rem 0.875rem;
  background: var(--ds-surface);
  border: 1.5px solid var(--ds-border);
  border-radius: var(--ds-radius-xl);
  font-size: 0.875rem;
  color: var(--ds-text);
  outline: none;
  transition: all 0.15s ease;
}

.dark .ec-input { background: var(--ds-gray-800); border-color: var(--ds-border-strong); color: #f1f5f9; }

.ec-input:focus { border-color: var(--ds-primary); box-shadow: 0 0 0 3px var(--ds-primary-ring); }

.ec-input--textarea { resize: vertical; min-height: 60px; line-height: 1.5; }

.ec-input--sm { width: 80px; text-align: center; font-weight: 700; }

/* Questions list */
.ec-qb-list { display: flex; flex-direction: column; gap: 0.75rem; }

.ec-qb-list__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.ec-qb-list__title {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.875rem;
  font-weight: 700;
  color: var(--ds-text);
  margin: 0;
}


.ec-qb-list__count {
  padding: 0.125rem 0.5rem;
  background: var(--ds-primary-soft);
  color: var(--ds-primary);
  border-radius: var(--ds-radius-full);
  font-size: 0.7rem;
}

.ec-qb-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.375rem;
  padding: 2rem;
  color: var(--ds-text-muted);
  font-size: 0.875rem;
  border: 1px dashed var(--ds-border);
  border-radius: var(--ds-radius-xl);
}

.dark .ec-qb-empty { border-color: var(--ds-border-strong); }


.ec-qb-items { display: flex; flex-direction: column; gap: 0.5rem; }

.ec-qb-item {
  display: flex;
  align-items: flex-start;
  gap: 0.75rem;
  padding: 0.875rem 1rem;
  background: var(--ds-gray-50);
  border: 1px solid var(--ds-border);
  border-radius: var(--ds-radius-xl);
  transition: all 0.15s ease;
}

.dark .ec-qb-item { background: var(--ds-gray-800); border-color: var(--ds-border-strong); }

.ec-qb-item:hover { border-color: var(--ds-primary-border); }

.ec-qb-item__num {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: var(--ds-primary-soft);
  color: var(--ds-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.75rem;
  font-weight: 800;
  flex-shrink: 0;
}

.ec-qb-item__body { flex: 1; min-width: 0; }

.ec-qb-item__content {
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--ds-text);
  margin: 0;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.dark .ec-qb-item__content { color: #f1f5f9; }

.ec-qb-item__meta {
  display: flex;
  gap: 0.75rem;
  margin-top: 0.375rem;
  font-size: 0.7rem;
  color: var(--ds-text-muted);
}

.ec-qb-item__correct {
  background: var(--ds-success-soft);
  color: var(--ds-success);
  padding: 0.1rem 0.5rem;
  border-radius: var(--ds-radius-full);
  font-weight: 700;
}

.ec-qb-item__delete {
  width: 2rem;
  height: 2rem;
  border-radius: var(--ds-radius-lg);
  border: none;
  background: transparent;
  color: var(--ds-text-muted);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.15s ease;
  flex-shrink: 0;
}

.ec-qb-item__delete:hover { background: var(--ds-danger-soft); color: var(--ds-danger); }

/* Shared buttons */
.ec-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  padding: 0.6875rem 1.25rem;
  border-radius: var(--ds-radius-xl);
  font-size: 0.875rem;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.15s ease;
  border: 1px solid transparent;
  align-self: flex-start;
}


.ec-btn--sm { padding: 0.5rem 1rem; font-size: 0.8rem; }

.ec-btn--primary {
  background: linear-gradient(135deg, var(--ds-primary) 0%, #6366f1 100%);
  color: white;
  box-shadow: 0 4px 12px rgba(79, 70, 229, 0.25);
}

.ec-btn--primary:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 6px 20px rgba(79, 70, 229, 0.35);
}

.ec-btn--primary:disabled { opacity: 0.5; cursor: not-allowed; transform: none; box-shadow: none; }

.ec-spin { animation: spin 1s linear infinite; }

@keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }

.hidden { display: none; }
</style>
