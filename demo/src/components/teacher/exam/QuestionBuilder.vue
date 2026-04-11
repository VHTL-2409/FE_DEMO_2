<template>
  <div class="ec-section">
    <div class="ec-section__header">
      <div class="ec-section__icon">
        <LucideIcon name="quiz" />
      </div>
      <div>
        <h3 class="ec-section__title">Phân tích đề thi</h3>
        <p class="ec-section__desc">Nhập câu hỏi từ file và cấu hình cách hiển thị</p>
      </div>
    </div>

    <div class="ec-section__body">

      <!-- File drop zone -->
      <div class="ec-field">
        <label class="ec-field__label">File câu hỏi</label>
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

        <button
          type="button"
          class="ec-btn ec-btn--primary"
          style="margin-top: 0.75rem;"
          :disabled="!selectedFile || isImporting"
          @click="handleImport"
        >
          <LucideIcon name="progress_activity" v-if="isImporting" class="ec-spin" />
          <LucideIcon name="upload" v-else />
          {{ isImporting ? 'Đang xử lý...' : 'Nhập câu hỏi' }}
        </button>

        <div v-if="importError" class="ec-qb-error">
          <LucideIcon name="error" />
          {{ importError }}
        </div>
      </div>

      <!-- Questions list -->
      <div class="ec-qb-list">
        <div class="ec-qb-list__header">
          <h4 class="ec-qb-list__title">
            <LucideIcon name="list" />
            Danh sách câu hỏi
            <span class="ec-qb-list__count">{{ localQuestions.length }}</span>
          </h4>
          <button
            v-if="localQuestions.length > 0"
            type="button"
            class="ec-qb-delete-all-btn"
            @click="confirmDeleteAll"
          >
            <LucideIcon name="delete_sweep" />
            Xóa tất cả
          </button>
        </div>

        <div v-if="localQuestions.length === 0" class="ec-qb-empty">
          <LucideIcon name="quiz" />
          <p>Chưa có câu hỏi nào</p>
          <p>Nhập file để thêm câu hỏi</p>
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
                <span v-if="q.correctAnswer" class="ec-qb-item__correct">Đúng: {{ q.correctAnswer }}</span>
                <span v-if="q.score">{{ q.score }} điểm</span>
              </p>
            </div>
            <button type="button" class="ec-qb-item__delete" @click="removeQuestion(i)">
              <LucideIcon name="delete" />
            </button>
          </div>
        </div>
      </div>

      <!-- Toggle options -->
      <div class="ec-field">
        <label class="ec-field__label">Tùy chọn hiển thị</label>
        <div class="ec-toggle-list">
          <div class="ec-toggle-item">
            <div class="ec-toggle-item__body">
              <LucideIcon name="shuffle" />
              <div>
                <p class="ec-toggle-item__title">Xáo câu hỏi</p>
                <p class="ec-toggle-item__desc">Câu hỏi hiển thị ngẫu nhiên cho mỗi học sinh</p>
              </div>
            </div>
            <button
              type="button"
              class="ec-toggle"
              :class="localShuffleQuestions && 'ec-toggle--on'"
              @click="localShuffleQuestions = !localShuffleQuestions"
            >
              <span class="ec-toggle__knob" />
            </button>
          </div>

          <div class="ec-toggle-item">
            <div class="ec-toggle-item__body">
              <LucideIcon name="format_list_numbered" />
              <div>
                <p class="ec-toggle-item__title">Xáo đáp án</p>
                <p class="ec-toggle-item__desc">Đáp án hiển thị ngẫu nhiên cho mỗi học sinh</p>
              </div>
            </div>
            <button
              type="button"
              class="ec-toggle"
              :class="localShuffleAnswers && 'ec-toggle--on'"
              @click="localShuffleAnswers = !localShuffleAnswers"
            >
              <span class="ec-toggle__knob" />
            </button>
          </div>

          <div class="ec-toggle-item">
            <div class="ec-toggle-item__body">
              <LucideIcon name="call_split" />
              <div>
                <p class="ec-toggle-item__title">Chia mã đề</p>
                <p class="ec-toggle-item__desc">Tạo nhiều mã đề khác nhau từ file gốc</p>
              </div>
            </div>
            <button
              type="button"
              class="ec-toggle"
              :class="localMultipleVersions && 'ec-toggle--on'"
              @click="localMultipleVersions = !localMultipleVersions"
            >
              <span class="ec-toggle__knob" />
            </button>
          </div>
        </div>
      </div>

      <!-- Exam code display when multiple versions enabled -->
      <Transition name="ec-slide">
        <div v-if="localMultipleVersions" class="exam-code-card">
          <div class="ecc__header">
            <div class="ecc__icon">
              <LucideIcon name="vpn_key" />
            </div>
            <div class="ecc__title-group">
              <p class="ecc__label">Chia mã đề</p>
              <p class="ecc__desc">Mỗi mã đề sẽ có thứ tự câu hỏi riêng</p>
            </div>
          </div>
          <div class="ecc__body">
            <div class="ecc__stepper">
              <button
                type="button"
                class="ecc__stepper-btn"
                :disabled="versionCount <= 2"
                @click="versionCount = Math.max(2, versionCount - 1)"
              >
                <LucideIcon name="remove" />
              </button>
              <div class="ecc__stepper-value">
                <span class="ecc__stepper-num">{{ versionCount }}</span>
                <span class="ecc__stepper-unit">mã đề</span>
              </div>
              <button
                type="button"
                class="ecc__stepper-btn"
                :disabled="versionCount >= 20"
                @click="versionCount = Math.min(20, versionCount + 1)"
              >
                <LucideIcon name="add" />
              </button>
            </div>
            <input
              v-model.number="versionCount"
              type="range"
              min="2"
              max="20"
              class="ecc__slider"
            />
            <div class="ecc__hint-row">
              <LucideIcon name="info" size="13" />
              <span>Tối thiểu 2, tối đa 20 mã đề</span>
            </div>
          </div>
        </div>
      </Transition>

    </div>

    <!-- Version count modal -->
    <Teleport to="body">
      <Transition name="ec-modal">
        <div v-if="showVersionModal" class="ec-modal-overlay" @click.self="showVersionModal = false">
          <div class="ec-modal ec-modal--sm">
            <div class="ec-modal__header">
              <h2 class="ec-modal__title">Số mã đề</h2>
              <button type="button" class="ec-modal__close" @click="showVersionModal = false">
                <LucideIcon name="close" />
              </button>
            </div>
            <div class="ec-modal__body">
              <div class="ec-field">
                <label class="ec-field__label">Số lượng mã đề</label>
                <input
                  v-model.number="versionCount"
                  type="number"
                  min="2"
                  max="20"
                  class="ec-input ec-input--number"
                />
                <p class="ec-field__hint">Tối thiểu 2, tối đa 20 mã đề</p>
              </div>
            </div>
            <div class="ec-modal__footer">
              <button type="button" class="ec-btn ec-btn--outline" @click="showVersionModal = false">
                Hủy
              </button>
              <button type="button" class="ec-btn ec-btn--primary" @click="showVersionModal = false">
                Xác nhận
              </button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

    <ConfirmDialog
      v-model="showDeleteAllConfirm"
      title="Xóa tất cả câu hỏi"
      :message="`Bạn có chắc muốn xóa tất cả ${localQuestions.length} câu hỏi?`"
      confirm-label="Xóa tất cả"
      cancel-label="Hủy"
      variant="warning"
      icon="trash"
      @confirm="onConfirmDeleteAll"
    />
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { API_BASE_URL } from '../../../services/apiClient'
import { previewImportFile, validateImportFile } from '../../../services/importService'
import ConfirmDialog from '../../ui/ConfirmDialog.vue'

const props = defineProps({
  questions: { type: Array, default: () => [] },
  shuffleQuestions: { type: Boolean, default: false },
  shuffleAnswers: { type: Boolean, default: false }
})

const emit = defineEmits([
  'update:questions',
  'update:shuffleQuestions',
  'update:shuffleAnswers'
])

const isDragOver = ref(false)
const selectedFile = ref(null)
const fileInput = ref(null)
const isImporting = ref(false)
const importError = ref('')
const showVersionModal = ref(false)
const versionCount = ref(4)
const showDeleteAllConfirm = ref(false)

const templateCsvUrl = `${API_BASE_URL}/api/questions/template`
const templateXlsxUrl = `${API_BASE_URL}/api/questions/template?format=xlsx`

const localQuestions = computed({
  get: () => props.questions,
  set: (v) => emit('update:questions', v)
})

const localShuffleQuestions = computed({
  get: () => props.shuffleQuestions,
  set: (v) => emit('update:shuffleQuestions', v)
})

const localShuffleAnswers = computed({
  get: () => props.shuffleAnswers,
  set: (v) => emit('update:shuffleAnswers', v)
})

const localMultipleVersions = computed({
  get: () => versionCount.value > 1,
  set: (v) => {
    if (v) versionCount.value = Math.max(2, versionCount.value)
  }
})

const fileSizeLabel = computed(() => {
  if (!selectedFile.value) return ''
  const mb = selectedFile.value.size / (1024 * 1024)
  return `${mb.toFixed(2)} MB`
})

let localIdCounter = 1

const removeQuestion = (index) => {
  const updated = [...localQuestions.value]
  updated.splice(index, 1)
  localQuestions.value = updated
}

const deleteAllQuestions = () => {
  localQuestions.value = []
}

const confirmDeleteAll = () => {
  if (localQuestions.value.length === 0) return
  showDeleteAllConfirm.value = true
}

const onConfirmDeleteAll = () => {
  deleteAllQuestions()
  showDeleteAllConfirm.value = false
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
  try {
    validateImportFile(file)
  } catch (err) {
    importError.value = err.message
    return
  }
  selectedFile.value = file
}

const removeFile = () => {
  selectedFile.value = null
  importError.value = ''
  if (fileInput.value) fileInput.value.value = ''
}

/**
 * Parse file content client-side for CSV and XLSX
 * Returns array of question objects
 */
const parseFileClientSide = async (file) => {
  const ext = file.name.split('.').pop().toLowerCase()

  // CSV parsing
  if (ext === 'csv') {
    return new Promise((resolve, reject) => {
      const reader = new FileReader()
      reader.onload = (e) => {
        try {
          const content = e.target.result
          const lines = content.split(/\r?\n/).filter(line => line.trim())
          if (lines.length < 2) {
            reject(new Error('File CSV trống hoặc không có dữ liệu'))
            return
          }

          // Parse header
          const headers = parseCsvLine(lines[0]).map(h => h.toLowerCase().trim())
          const questionIndex = headers.findIndex(h => h.includes('câu') || h.includes('content') || h.includes('question'))
          const answerIndex = headers.findIndex(h => h.includes('đáp án') || h.includes('answer') || h.includes('correct') || h.includes('đúng'))
          const optionAIndex = headers.findIndex(h => h === 'a' || h === 'option a' || h === 'câu a')
          const optionBIndex = headers.findIndex(h => h === 'b' || h === 'option b' || h === 'câu b')
          const optionCIndex = headers.findIndex(h => h === 'c' || h === 'option c' || h === 'câu c')
          const optionDIndex = headers.findIndex(h => h === 'd' || h === 'option d' || h === 'câu d')
          const scoreIndex = headers.findIndex(h => h.includes('điểm') || h.includes('score') || h.includes('weight'))

          const questions = []
          for (let i = 1; i < lines.length; i++) {
            const values = parseCsvLine(lines[i])
            if (values.length === 0 || !values[0].trim()) continue

            const content = questionIndex >= 0 ? values[questionIndex]?.trim() : values[0]?.trim()
            if (!content) continue

            const correctAnswer = answerIndex >= 0 ? values[answerIndex]?.trim() || '' : ''
            const score = scoreIndex >= 0 ? parseFloat(values[scoreIndex]) || 1 : 1

            // Build options array
            const options = []
            if (optionAIndex >= 0 && values[optionAIndex]?.trim()) {
              options.push({ id: 'A', text: values[optionAIndex].trim() })
            }
            if (optionBIndex >= 0 && values[optionBIndex]?.trim()) {
              options.push({ id: 'B', text: values[optionBIndex].trim() })
            }
            if (optionCIndex >= 0 && values[optionCIndex]?.trim()) {
              options.push({ id: 'C', text: values[optionCIndex].trim() })
            }
            if (optionDIndex >= 0 && values[optionDIndex]?.trim()) {
              options.push({ id: 'D', text: values[optionDIndex].trim() })
            }

            questions.push({
              _localId: localIdCounter++,
              content,
              correctAnswer,
              score,
              options,
              type: 'SINGLE_CHOICE'
            })
          }

          if (questions.length === 0) {
            reject(new Error('Không tìm thấy câu hỏi nào trong file'))
            return
          }

          resolve(questions)
        } catch (err) {
          reject(new Error(`Lỗi đọc file CSV: ${err.message}`))
        }
      }
      reader.onerror = () => reject(new Error('Không thể đọc file'))
      reader.readAsText(file)
    })
  }

  // XLSX - need to use SheetJS library
  if (ext === 'xlsx') {
    return new Promise((resolve, reject) => {
      const reader = new FileReader()
      reader.onload = (e) => {
        try {
          // Dynamic import SheetJS
          import('xlsx').then((XLSX) => {
            const read = XLSX.read
            const data = new Uint8Array(e.target.result)
            const workbook = read(data, { type: 'array' })
            const sheetName = workbook.SheetNames[0]
            const sheet = workbook.Sheets[sheetName]
            const rows = XLSX.utils.sheet_to_json(sheet, { header: 1 })

            if (rows.length < 2) {
              reject(new Error('File Excel trống hoặc không có dữ liệu'))
              return
            }

            // Parse header row
            const headers = (rows[0] || []).map(h => String(h || '').toLowerCase().trim())
            const questionIndex = headers.findIndex(h => h.includes('câu') || h.includes('content') || h.includes('question') || h === 'nội dung')
            const answerIndex = headers.findIndex(h => h.includes('đáp án') || h.includes('answer') || h.includes('correct') || h.includes('đúng'))
            const scoreIndex = headers.findIndex(h => h.includes('điểm') || h.includes('score') || h.includes('weight'))

            const questions = []
            for (let i = 1; i < rows.length; i++) {
              const row = rows[i]
              if (!row || row.length === 0) continue

              const content = questionIndex >= 0 ? String(row[questionIndex] || '').trim() : String(row[0] || '').trim()
              if (!content) continue

              const correctAnswer = answerIndex >= 0 ? String(row[answerIndex] || '').trim() : ''
              const score = scoreIndex >= 0 ? parseFloat(row[scoreIndex]) || 1 : 1

              // Try to find option columns (A, B, C, D)
              // Match by first letter of header or known prefixes
              const optionLabelMap = { a: 'A', b: 'B', c: 'C', d: 'D' }
              const optionCols = []
              for (let col = 0; col < headers.length; col++) {
                const h = headers[col]
                // Match single letter A/B/C/D at start of header
                if (h.length === 1 && optionLabelMap[h]) {
                  optionCols.push({ index: col, id: optionLabelMap[h] })
                } else {
                  // Match "option A", "câu A", "A)", "Đáp án A", etc.
                  for (const [letter, label] of Object.entries(optionLabelMap)) {
                    const patterns = [
                      `option ${letter}`,
                      `câu ${letter}`,
                      `đáp án ${letter}`,
                      `${letter})`,
                      `${letter}.`,
                    ]
                    if (patterns.some(p => h.includes(p))) {
                      optionCols.push({ index: col, id: label })
                      break
                    }
                  }
                }
              }
              // Sort by ID to ensure correct order: A, B, C, D
              optionCols.sort((a, b) => a.id.localeCompare(b.id))

              const options = optionCols.slice(0, 4).map(({ index, id }) => ({
                id,
                text: String(row[index] || '').replace(/^\uFEFF/, '').trim()
              })).filter(opt => opt.text)

              questions.push({
                _localId: localIdCounter++,
                content,
                correctAnswer,
                score,
                options,
                type: 'SINGLE_CHOICE'
              })
            }

            if (questions.length === 0) {
              reject(new Error('Không tìm thấy câu hỏi nào trong file'))
              return
            }

            resolve(questions)
          }).catch(() => {
            // Fallback: try to use api preview
            resolve(null)
          })
        } catch (err) {
          reject(new Error(`Lỗi đọc file Excel: ${err.message}`))
        }
      }
      reader.onerror = () => reject(new Error('Không thể đọc file'))
      reader.readAsArrayBuffer(file)
    })
  }

  // PDF and DOCX - use API preview
  return null
}

const parseCsvLine = (line) => {
  // Strip BOM (UTF-8 signature) at start of file
  const cleaned = line.replace(/^\uFEFF/, '')
  const result = []
  let current = ''
  let inQuotes = false
  for (let i = 0; i < cleaned.length; i++) {
    const char = cleaned[i]
    if (char === '"') {
      inQuotes = !inQuotes
    } else if ((char === ';' || char === ',') && !inQuotes) {
      // Support both , and ; delimiters (Vietnam / Europe CSV)
      result.push(current.trim())
      current = ''
    } else {
      current += char
    }
  }
  result.push(current.trim())
  return result
}

const handleImport = async () => {
  if (!selectedFile.value) return
  isImporting.value = true
  importError.value = ''
  try {
    // Try client-side parsing first for CSV/XLSX
    let questions = await parseFileClientSide(selectedFile.value)

    // Fallback to API preview for PDF/DOCX or if client parsing failed
    if (!questions || questions.length === 0) {
      try {
        const previewResult = await previewImportFile(selectedFile.value)
        if (previewResult && Array.isArray(previewResult)) {
          questions = previewResult.map((q) => ({
            _localId: localIdCounter++,
            content: q.content || q.question || q.text || '',
            correctAnswer: q.correctAnswer || q.answer || q.correct || '',
            score: parseFloat(q.scoreWeight || q.score || q.points || 1),
            options: Array.isArray(q.options)
              ? q.options.map((o) => ({ id: o.id || o.key || 'A', text: o.text || o.value || '' }))
              : [],
            type: q.type || 'SINGLE_CHOICE'
          }))
        } else if (previewResult && previewResult.questions) {
          questions = previewResult.questions.map((q) => ({
            _localId: localIdCounter++,
            content: q.content || q.question || q.text || '',
            correctAnswer: q.correctAnswer || q.answer || q.correct || '',
            score: parseFloat(q.scoreWeight || q.score || q.points || 1),
            options: Array.isArray(q.options)
              ? q.options.map((o) => ({ id: o.id || o.key || 'A', text: o.text || o.value || '' }))
              : [],
            type: q.type || 'SINGLE_CHOICE'
          }))
        }
      } catch (apiErr) {
        console.warn('API preview failed, using file content directly:', apiErr)
        if (apiErr?.payload?.message) {
          importError.value = apiErr.payload.message
        } else if (apiErr?.message) {
          importError.value = apiErr.message
        }
      }
    }

    // If still no questions, show error
    if (!questions || questions.length === 0) {
      const existingError = importError.value
      importError.value = existingError || 'Không thể đọc câu hỏi từ file. Vui lòng kiểm tra định dạng file.'
      return
    }

    // Add parsed questions to the list
    localQuestions.value = [...localQuestions.value, ...questions]
    selectedFile.value = null
    if (fileInput.value) fileInput.value.value = ''

  } catch (err) {
    if (err?.payload?.message) {
      importError.value = err.payload.message
    } else {
      importError.value = err.message || 'Không thể nhập file. Vui lòng thử lại.'
    }
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

.dark .ec-section__title { color: var(--ds-text); }

.ec-section__desc {
  font-size: 0.75rem;
  color: var(--ds-text-muted);
  margin: 0.25rem 0 0;
}

.ec-section__body {
  padding: 1.5rem;
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

/* Field */
.ec-field {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.ec-field__label {
  font-size: 0.8rem;
  font-weight: 700;
  color: var(--ds-text);
}

.dark .ec-field__label { color: var(--ds-text); }

.ec-field__hint {
  font-size: 0.7rem;
  color: var(--ds-text-muted);
  margin: 0.25rem 0 0;
}

/* Dropzone */
.ec-dropzone {
  border: 2px dashed var(--ds-border);
  border-radius: var(--ds-radius-2xl);
  padding: 2rem;
  cursor: pointer;
  transition: color 0.2s ease, background-color 0.2s ease, border-color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease;
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

.dark .ec-dropzone__title { color: var(--ds-text); }

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
  transition: color 0.15s ease, background-color 0.15s ease, border-color 0.15s ease, box-shadow 0.15s ease, transform 0.15s ease;
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

.dark .ec-dropzone__file-name { color: var(--ds-text); }

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
  transition: color 0.15s ease, background-color 0.15s ease, border-color 0.15s ease, box-shadow 0.15s ease, transform 0.15s ease;
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

.ec-qb-delete-all-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.375rem;
  padding: 0.375rem 0.75rem;
  border-radius: var(--ds-radius-lg);
  border: 1px solid var(--ds-border);
  background: var(--ds-danger-soft);
  color: var(--ds-danger);
  font-size: 0.75rem;
  font-weight: 600;
  cursor: pointer;
  transition: color 0.15s ease, background-color 0.15s ease, border-color 0.15s ease, box-shadow 0.15s ease, transform 0.15s ease;
  margin-left: auto;
}

.ec-qb-delete-all-btn:hover {
  background: var(--ds-danger);
  color: white;
  border-color: var(--ds-danger);
}

.dark .ec-qb-delete-all-btn {
  background: rgba(239, 68, 68, 0.15);
}

.dark .ec-qb-delete-all-btn:hover {
  background: var(--ds-danger);
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
  transition: color 0.15s ease, background-color 0.15s ease, border-color 0.15s ease, box-shadow 0.15s ease, transform 0.15s ease;
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

.dark .ec-qb-item__content { color: var(--ds-text); }

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
  transition: color 0.15s ease, background-color 0.15s ease, border-color 0.15s ease, box-shadow 0.15s ease, transform 0.15s ease;
  flex-shrink: 0;
}

.ec-qb-item__delete:hover { background: var(--ds-danger-soft); color: var(--ds-danger); }

/* Toggle list */
.ec-toggle-list {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.ec-toggle-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  padding: 0.875rem 1rem;
  background: var(--ds-gray-50);
  border: 1px solid var(--ds-border);
  border-radius: var(--ds-radius-xl);
  transition: color 0.15s ease, background-color 0.15s ease, border-color 0.15s ease, box-shadow 0.15s ease, transform 0.15s ease;
}

.dark .ec-toggle-item { background: var(--ds-gray-800); border-color: var(--ds-border-strong); }
.ec-toggle-item:hover { border-color: var(--ds-primary-border); background: var(--ds-primary-soft); }

.ec-toggle-item__body {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex: 1;
  min-width: 0;
}

.ec-toggle-item__title {
  font-size: 0.875rem;
  font-weight: 700;
  color: var(--ds-text);
  margin: 0;
  line-height: 1.2;
}

.dark .ec-toggle-item__title { color: var(--ds-text); }

.ec-toggle-item__desc {
  font-size: 0.7rem;
  color: var(--ds-text-muted);
  margin: 0.25rem 0 0;
  line-height: 1.4;
}

/* Toggle switch */
.ec-toggle {
  position: relative;
  width: 44px;
  height: 24px;
  border-radius: 12px;
  border: none;
  background: var(--ds-gray-300);
  cursor: pointer;
  transition: background 0.2s ease;
  flex-shrink: 0;
}

.dark .ec-toggle { background: var(--ds-gray-600); }
.ec-toggle--on { background: var(--ds-primary); }

.ec-toggle__knob {
  position: absolute;
  top: 3px;
  left: 3px;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: white;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.2);
  transition: transform 0.2s ease;
}

.ec-toggle--on .ec-toggle__knob { transform: translateX(20px); }

/* Exam code card */
.exam-code-card {
  border: 1px solid var(--ds-border);
  border-radius: var(--ds-radius-xl);
  overflow: hidden;
  margin-top: 1rem;
}

.dark .exam-code-card {
  border-color: var(--ds-border-strong);
  background: rgba(2, 132, 199, 0.05);
}

.ecc__header {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 1rem 1.25rem;
  background: var(--ds-primary-soft);
  border-bottom: 1px solid var(--ds-border);
}

.dark .ecc__header { background: rgba(79, 70, 229, 0.15); border-color: var(--ds-border-strong); }

.ecc__icon {
  width: 2.5rem;
  height: 2.5rem;
  border-radius: var(--ds-radius-lg);
  background: var(--ds-primary);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.dark .ecc__icon { background: var(--ds-primary); }

.ecc__title-group { flex: 1; }

.ecc__label {
  font-size: 0.875rem;
  font-weight: 700;
  color: var(--ds-text);
  margin: 0;
}

.dark .ecc__label { color: white; }

.ecc__desc {
  font-size: 0.75rem;
  color: var(--ds-text-secondary);
  margin: 0.125rem 0 0;
}

.dark .ecc__desc { color: var(--ds-text-secondary); }

.ecc__body {
  padding: 1.25rem;
  background: var(--ds-surface);
}

.dark .ecc__body { background: var(--ds-surface); }

.ecc__stepper {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 1rem;
  margin-bottom: 1rem;
}

.ecc__stepper-btn {
  width: 2.5rem;
  height: 2.5rem;
  border-radius: var(--ds-radius-lg);
  border: 1px solid var(--ds-border);
  background: var(--ds-gray-50);
  color: var(--ds-text);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: color 0.15s ease, background-color 0.15s ease, border-color 0.15s ease, box-shadow 0.15s ease, transform 0.15s ease;
  font-size: 1.25rem;
}

.dark .ecc__stepper-btn {
  background: var(--ds-gray-800);
  border-color: var(--ds-border-strong);
  color: white;
}

.ecc__stepper-btn:hover:not(:disabled) {
  background: var(--ds-primary);
  color: white;
  border-color: var(--ds-primary);
}

.ecc__stepper-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.ecc__stepper-value {
  display: flex;
  flex-direction: column;
  align-items: center;
  min-width: 5rem;
}

.ecc__stepper-num {
  font-size: 2rem;
  font-weight: 800;
  color: var(--ds-primary);
  line-height: 1;
}

.dark .ecc__stepper-num { color: var(--ds-primary); }

.ecc__stepper-unit {
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--ds-text-secondary);
}

.ecc__slider {
  width: 100%;
  height: 4px;
  border-radius: 2px;
  background: var(--ds-gray-200);
  outline: none;
  -webkit-appearance: none;
  margin-bottom: 0.75rem;
}

.dark .ecc__slider { background: var(--ds-gray-700); }

.ecc__slider::-webkit-slider-thumb {
  -webkit-appearance: none;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: var(--ds-primary);
  cursor: pointer;
  box-shadow: 0 2px 6px rgba(79, 70, 229, 0.4);
}

.ecc__hint-row {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  font-size: 0.75rem;
  color: var(--ds-text-muted);
  justify-content: center;
}

/* Buttons */
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
  transition: color 0.15s ease, background-color 0.15s ease, border-color 0.15s ease, box-shadow 0.15s ease, transform 0.15s ease;
  border: 1px solid transparent;
  align-self: flex-start;
}

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

.ec-btn--outline { background: transparent; border-color: var(--ds-border); color: var(--ds-text-secondary); }
.ec-btn--outline:hover { background: var(--ds-gray-50); color: var(--ds-text); }
.dark .ec-btn--outline:hover { background: var(--ds-gray-700); }

/* Modal */
.ec-modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 100;
  background: rgba(15, 23, 42, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 1rem;
}

.ec-modal {
  background: var(--ds-surface);
  border-radius: var(--ds-radius-2xl);
  box-shadow: var(--ds-shadow-xl);
  width: 100%;
  max-width: 480px;
  max-height: 90vh;
  overflow-y: auto;
}

.ec-modal--sm { max-width: 360px; }

.ec-modal__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1.25rem 1.5rem;
  border-bottom: 1px solid var(--ds-border);
}

.ec-modal__title {
  font-family: var(--ds-font-display);
  font-size: 1.125rem;
  font-weight: 800;
  color: var(--ds-text);
  margin: 0;
}

.dark .ec-modal__title { color: var(--ds-text); }

.ec-modal__close {
  width: 2rem;
  height: 2rem;
  border-radius: var(--ds-radius-lg);
  border: none;
  background: transparent;
  color: var(--ds-text-muted);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: color 0.15s ease, background-color 0.15s ease, border-color 0.15s ease, box-shadow 0.15s ease, transform 0.15s ease;
}

.ec-modal__close:hover { background: var(--ds-gray-100); color: var(--ds-text); }
.dark .ec-modal__close:hover { background: var(--ds-gray-700); }

.ec-modal__body { padding: 1.5rem; }

.ec-modal__footer {
  padding: 1rem 1.5rem;
  border-top: 1px solid var(--ds-border);
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
}

/* Input */
.ec-input {
  width: 100%;
  padding: 0.6875rem 1.25rem;
  background: var(--ds-surface);
  border: 2px solid var(--ds-border);
  border-radius: var(--ds-radius-2xl);
  font-size: 0.875rem;
  color: var(--ds-text);
  transition: color 0.25s cubic-bezier(0.4, 0, 0.2, 1), background-color 0.25s cubic-bezier(0.4, 0, 0.2, 1), border-color 0.25s cubic-bezier(0.4, 0, 0.2, 1), box-shadow 0.25s cubic-bezier(0.4, 0, 0.2, 1), transform 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  outline: none;
}

.dark .ec-input { background: var(--ds-gray-800); border-color: var(--ds-border-strong); color: var(--ds-text); }

.ec-input:focus { border-color: var(--ds-primary); box-shadow: 0 0 0 4px var(--ds-primary-ring); }

.ec-input--number {
  width: 100px;
  text-align: center;
  font-weight: 700;
  font-size: 1.125rem;
  font-family: var(--ds-font-display);
  padding: 0.625rem 0.875rem;
}

/* Transitions */
.ec-slide-enter-active, .ec-slide-leave-active { transition: color 0.28s cubic-bezier(0.4, 0, 0.2, 1), background-color 0.28s cubic-bezier(0.4, 0, 0.2, 1), border-color 0.28s cubic-bezier(0.4, 0, 0.2, 1), box-shadow 0.28s cubic-bezier(0.4, 0, 0.2, 1), transform 0.28s cubic-bezier(0.4, 0, 0.2, 1); overflow: hidden; }
.ec-slide-enter-from, .ec-slide-leave-to { opacity: 0; max-height: 0; }
.ec-slide-enter-to, .ec-slide-leave-from { opacity: 1; max-height: 200px; }

.ec-modal-enter-active, .ec-modal-leave-active { transition: opacity 0.2s ease; }
.ec-modal-enter-from, .ec-modal-leave-to { opacity: 0; }
.ec-modal-enter-from .ec-modal, .ec-modal-leave-to .ec-modal { opacity: 0; }

.ec-spin { animation: spin 1s linear infinite; }
@keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg) translateZ(0); } }

.hidden { display: none; }
</style>