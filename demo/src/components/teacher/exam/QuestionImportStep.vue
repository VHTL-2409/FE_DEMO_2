<template>
  <div class="ec-section">
    <div class="ec-section__header">
      <div class="ec-section__icon">
        <LucideIcon name="upload_file" />
      </div>
      <div>
        <h3 class="ec-section__title">Nhập file câu hỏi</h3>
      </div>
    </div>

    <div class="ec-section__body">

      <!-- File drop zone -->
      <div class="ec-field">
        <label class="ec-field__label">File câu hỏi</label>
        <div
          class="ec-dropzone"
          :class="{ 'ec-dropzone--dragover': isDragOver, 'ec-dropzone--has-file': selectedFile }"
          @dragenter.prevent="handleDragEnter"
          @dragover.prevent="handleDragOver"
          @dragleave.prevent="handleDragLeave"
          @drop.prevent="onDrop"
          @click="triggerFileInput"
        >
          <input
            ref="fileInput"
            type="file"
            :accept="IMPORT_FILE_ACCEPT"
            class="hidden"
            @change="onFileChange"
          />
          <div v-if="!selectedFile" class="ec-dropzone__empty">
            <div class="ec-dropzone__icon">
              <LucideIcon name="cloud_upload" />
            </div>
            <p class="ec-dropzone__title">Kéo thả file hoặc nhấn để chọn</p>
            <p class="ec-dropzone__sub">CSV, XLSX, PDF, Word · Tối đa {{ IMPORT_FILE_MAX_SIZE_MB }}MB</p>
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

        <div v-if="importError" class="ec-qb-error">
          <LucideIcon name="error" />
          {{ importError }}
        </div>
      </div>

      <div v-if="selectedFile && isServerParseCandidate" class="ec-field">
        <label class="ec-field__label">Nhận diện mẫu tự động</label>
        <div class="ec-import-template-picker ec-import-template-picker--auto">
          <p class="ec-import-template-picker__hint">
            Hệ thống sẽ tự động nhận diện mẫu PDF / DOCX và tối ưu cách đọc, không cần chọn thủ công.
          </p>
          <p class="ec-import-template-picker__status" :class="{ 'ec-import-template-picker__status--down': pythonAvailable === false }">
            {{ parserHealthLabel }}
          </p>
        </div>
      </div>

      <!-- Đọc câu hỏi — ngay dưới khung file, trước tùy chọn hiển thị -->
      <div class="ec-import-actions ec-import-actions--after-dropzone">
        <button
          type="button"
          class="ec-btn ec-btn--primary"
          :disabled="!selectedFile || isImporting"
          @click="handleImport"
        >
          <LucideIcon name="progress_activity" v-if="isImporting" class="ec-spin" />
          <LucideIcon name="upload" v-else />
          {{ isImporting ? parsingStatus || 'Đang xử lý...' : 'Đọc câu hỏi' }}
        </button>
      </div>

      <!-- Server-side parsing progress -->
      <div v-if="isParsingServer && isImporting" class="ec-qb-parse-progress">
        <div class="ec-qb-progress-bar">
          <div class="ec-qb-progress-fill" :style="{ width: `${parsingProgress}%` }" />
        </div>
        <p class="ec-qb-progress-status">{{ parsingStatus }}</p>
      </div>

      <!-- Import result summary — ngay dưới nút đọc, trước tùy chọn hiển thị -->
      <Transition name="ec-slide">
        <div v-if="importSuccess && importedCount > 0" class="ec-import-success">
          <div class="ec-import-success__icon">
            <LucideIcon name="check_circle" />
          </div>
          <div class="ec-import-success__content">
            <p class="ec-import-success__title">Đọc thành công {{ importedCount }} câu hỏi</p>
            <p class="ec-import-success__desc">Bạn có thể chuyển sang bước Chỉnh sửa câu hỏi để rà soát chi tiết</p>
          </div>
          <button
            type="button"
            class="ec-import-success__add-more"
            @click="showAddMore = !showAddMore"
          >
            <LucideIcon name="add" />
            Thêm file khác
          </button>
        </div>
      </Transition>

      <Transition name="ec-slide">
        <div
          v-if="canRetryWithTemplate && isServerParseCandidate"
          class="ec-import-retry"
        >
          <div class="ec-import-retry__copy">
            <LucideIcon name="tips_and_updates" />
            <span>
              {{ importError
                ? 'Có thể đọc lại để hệ thống tự nhận diện mẫu một lần nữa.'
                : 'Nếu còn câu đọc sai, hãy nhấn Đọc câu hỏi lại để hệ thống tự nhận diện lại.' }}
            </span>
          </div>
          <button
            type="button"
            class="ec-btn ec-btn--ghost"
            :disabled="!selectedFile || isImporting"
            @click="handleImport"
          >
            <LucideIcon name="refresh" />
            Đọc lại tự động
          </button>
        </div>
      </Transition>

      <!-- Display options (apply to the whole exam) -->
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

      <!-- Add more file section -->
      <Transition name="ec-slide">
        <div v-if="showAddMore && importedCount > 0" class="ec-add-more">
          <div class="ec-add-more__divider">
            <span>Thêm file khác</span>
          </div>
          <div
            class="ec-dropzone ec-dropzone--sm"
            :class="{ 'ec-dropzone--dragover': isDragOver, 'ec-dropzone--has-file': selectedFile }"
            @dragenter.prevent="handleDragEnter"
            @dragover.prevent="handleDragOver"
            @dragleave.prevent="handleDragLeave"
            @drop.prevent="onDrop"
            @click="triggerFileInput"
          >
            <input
              ref="fileInput2"
              type="file"
              :accept="IMPORT_FILE_ACCEPT"
              class="hidden"
              @change="onFileChange"
            />
            <div v-if="!selectedFile" class="ec-dropzone__empty">
              <LucideIcon name="attach_file" />
              <span>Chọn file để thêm</span>
            </div>
            <div v-else class="ec-dropzone__file">
              <div class="ec-dropzone__file-icon ec-dropzone__file-icon--sm">
                <LucideIcon name="description" />
              </div>
              <div class="ec-dropzone__file-info">
                <p class="ec-dropzone__file-name">{{ selectedFile.name }}</p>
              </div>
              <button type="button" class="ec-dropzone__file-remove" @click.stop="removeFile">
                <LucideIcon name="close" />
              </button>
            </div>
          </div>
          <div class="ec-import-actions ec-import-actions--add-more">
            <button
              type="button"
              class="ec-btn ec-btn--primary"
              :disabled="!selectedFile || isImporting"
              @click="handleImport"
            >
              <LucideIcon name="progress_activity" v-if="isImporting" class="ec-spin" />
              <LucideIcon name="upload" v-else />
              {{ isImporting ? 'Đang xử lý...' : 'Đọc câu hỏi' }}
            </button>
          </div>
        </div>
      </Transition>

    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { API_BASE_URL } from '../../../services/apiClient'
import {
  previewImportFile,
  uploadExamPdf,
  getExamImportPreview,
  waitForExamImportSession,
  checkPythonParserHealth,
  validateImportFile,
  IMPORT_FILE_ACCEPT,
  IMPORT_FILE_MAX_SIZE_MB
} from '../../../services/importService'
import { normalizeImportedExamQuestion } from '../../../utils/mathTextNormalize'

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
const fileInput2 = ref(null)
const isImporting = ref(false)
const importError = ref('')
const importSuccess = ref(false)
const importedCount = ref(0)
const showAddMore = ref(false)
const versionCount = ref(4)
const isParsingServer = ref(false)
const parsingStatus = ref('')
const parsingProgress = ref(0)
const sessionId = ref(null)
const sessionStatus = ref(null)
const pythonAvailable = ref(null)
const dragDepth = ref(0)
const importedBatchIdsByFingerprint = ref({})

const localQuestions = computed({
  get: () => props.questions,
  set: (value) => emit('update:questions', value)
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

const templateCsvUrl = `${API_BASE_URL}/api/questions/template`
const templateXlsxUrl = `${API_BASE_URL}/api/questions/template?format=xlsx`

const isServerParseCandidate = computed(() => {
  const ext = selectedFile.value?.name?.split('.').pop()?.toLowerCase()
  return ext === 'pdf' || ext === 'docx'
})

const canRetryWithTemplate = computed(() => {
  if (!selectedFile.value || !isServerParseCandidate.value) return false
  return (importSuccess.value && importedCount.value > 0) || Boolean(importError.value)
})

const parserHealthLabel = computed(() => {
  if (pythonAvailable.value == null) return 'Đang kiểm tra parser Python...'
  return Boolean(pythonAvailable.value)
    ? 'Parser Python đang sẵn sàng cho PDF/DOCX.'
    : 'Parser Python chưa sẵn sàng, file PDF/DOCX có thể không đọc được.'
})

const fileSizeLabel = computed(() => {
  if (!selectedFile.value) return ''
  const mb = selectedFile.value.size / (1024 * 1024)
  return `${mb.toFixed(2)} MB`
})

let localIdCounter = 1

const triggerFileInput = () => {
  if (showAddMore.value && fileInput2.value) {
    fileInput2.value.click()
    return
  }
  fileInput.value?.click()
}

const getFileFingerprint = (file) => {
  if (!file) return ''
  return [file.name, file.size, file.lastModified].join(':')
}

const collectLegacyOptionRows = (question) => {
  return ['A', 'B', 'C', 'D']
    .map((id) => ({
      id,
      text: String(
        question?.[`option${id}`]
        ?? question?.[`option_${id.toLowerCase()}`]
        ?? ''
      ),
    }))
    .filter((option) => option.text.trim() !== '')
}

const normalizeQuestionTypeForImport = (rawType, normalizedAnswer) => {
  const type = String(rawType || 'SINGLE_CHOICE').toUpperCase()
  if (type === 'MULTIPLE_CHOICE') {
    const answers = String(normalizedAnswer || '')
      .split(',')
      .map((item) => item.trim())
      .filter(Boolean)
    return answers.length > 1 ? 'MULTIPLE_CHOICE' : 'SINGLE_CHOICE'
  }
  return type
}

const normalizeImportedRenderState = (question) => {
  const rawRenderMode = question?.render?.mode ?? question?.renderMode ?? question?.render_mode
  const normalizedRenderMode = String(rawRenderMode ?? '').trim()
  const render = question?.render && typeof question.render === 'object'
    ? { ...question.render }
    : null

  if (render && normalizedRenderMode && !render.mode) {
    render.mode = normalizedRenderMode
  }

  return {
    render,
    ...(normalizedRenderMode ? { renderMode: normalizedRenderMode } : {}),
  }
}

const getTrackedBatchIdsForFingerprint = (fingerprint) => {
  if (!fingerprint) return []
  const knownIds = importedBatchIdsByFingerprint.value[fingerprint]
  if (!Array.isArray(knownIds) || knownIds.length === 0) return []
  return [...knownIds]
}

const getMissingTrackedBatchCountForFingerprint = (fingerprint) => {
  if (!fingerprint) return 0
  const knownIds = importedBatchIdsByFingerprint.value[fingerprint]
  if (!Array.isArray(knownIds) || knownIds.length === 0) return 0
  const existingIds = new Set(props.questions.map((q) => q?._localId).filter(Boolean))
  const existingTrackedCount = knownIds.filter((id) => existingIds.has(id)).length
  return Math.max(0, knownIds.length - existingTrackedCount)
}

const rememberImportedBatch = (fingerprint, batchIds) => {
  if (!fingerprint) return
  importedBatchIdsByFingerprint.value = {
    ...importedBatchIdsByFingerprint.value,
    [fingerprint]: batchIds,
  }
}

const resetDragState = () => {
  dragDepth.value = 0
  isDragOver.value = false
}

const handleDragEnter = () => {
  dragDepth.value += 1
  isDragOver.value = true
}

const handleDragOver = () => {
  isDragOver.value = true
}

const handleDragLeave = () => {
  dragDepth.value = Math.max(0, dragDepth.value - 1)
  if (dragDepth.value === 0) {
    isDragOver.value = false
  }
}

const onDrop = (e) => {
  resetDragState()
  const file = e.dataTransfer?.files?.[0]
  if (file) setFile(file)
}

const onFileChange = (e) => {
  const file = e.target.files?.[0]
  if (file) setFile(file)
}

const setFile = (file) => {
  const previousFingerprint = getFileFingerprint(selectedFile.value)
  importError.value = ''
  try {
    validateImportFile(file)
  } catch (err) {
    selectedFile.value = null
    importSuccess.value = false
    importedCount.value = 0
    sessionId.value = null
    sessionStatus.value = null
    importError.value = err.message
    if (fileInput.value) fileInput.value.value = ''
    if (fileInput2.value) fileInput2.value.value = ''
    return
  }
  selectedFile.value = file
  importSuccess.value = false
  importedCount.value = 0
  showAddMore.value = false
  sessionId.value = null
  sessionStatus.value = null
}

const removeFile = () => {
  selectedFile.value = null
  importError.value = ''
  importSuccess.value = false
  importedCount.value = 0
  sessionId.value = null
  sessionStatus.value = null
  resetDragState()
  if (fileInput.value) fileInput.value.value = ''
  if (fileInput2.value) fileInput2.value.value = ''
}

const parseFileClientSide = async (file) => {
  const ext = file.name.split('.').pop().toLowerCase()

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

  if (ext === 'xlsx') {
    return new Promise((resolve, reject) => {
      const reader = new FileReader()
      reader.onload = (e) => {
        try {
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

              const optionLabelMap = { a: 'A', b: 'B', c: 'C', d: 'D' }
              const optionCols = []
              for (let col = 0; col < headers.length; col++) {
                const h = headers[col]
                if (h.length === 1 && optionLabelMap[h]) {
                  optionCols.push({ index: col, id: optionLabelMap[h] })
                } else {
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

  return null
}

const parseCsvLine = (line) => {
  const cleaned = line.replace(/^\uFEFF/, '')
  const result = []
  let current = ''
  let inQuotes = false
  for (let i = 0; i < cleaned.length; i++) {
    const char = cleaned[i]
    if (char === '"') {
      inQuotes = !inQuotes
    } else if ((char === ';' || char === ',') && !inQuotes) {
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
  const selectedExt = selectedFile.value.name.split('.').pop().toLowerCase()
  const selectedFingerprint = getFileFingerprint(selectedFile.value)
  const trackedSelectedBatchIds = getTrackedBatchIdsForFingerprint(selectedFingerprint)
  const shouldReplaceTrackedBatch =
    (selectedExt === 'pdf' || selectedExt === 'docx')
    && selectedFingerprint !== ''
    && trackedSelectedBatchIds.length > 0

  if (shouldReplaceTrackedBatch) {
    const editedTrackedCount = props.questions.filter(
      (q) => trackedSelectedBatchIds.includes(q?._localId) && q?._manualEdited
    ).length
    const missingTrackedCount = getMissingTrackedBatchCountForFingerprint(selectedFingerprint)
    if (editedTrackedCount > 0 || missingTrackedCount > 0) {
      const changeSummary = []
      if (editedTrackedCount > 0) {
        changeSummary.push(`${editedTrackedCount} câu đã chỉnh sửa thủ công`)
      }
      if (missingTrackedCount > 0) {
        changeSummary.push(`${missingTrackedCount} câu đã bị xóa khỏi batch import trước đó`)
      }
      const confirmed = typeof window === 'undefined'
        ? true
        : window.confirm(
            `Đọc lại file này sẽ ghi đè ${changeSummary.join(' và ')}. Bạn có muốn tiếp tục không?`
          )
      if (!confirmed) {
        return
      }
    }
  }
  isImporting.value = true
  importError.value = ''
  importSuccess.value = false
  importedCount.value = 0
  isParsingServer.value = false
  sessionId.value = null
  sessionStatus.value = null
  try {
    const ext = selectedFile.value.name.split('.').pop().toLowerCase()
    let questions = null

    // ── PDF / DOCX → exam-import pipeline (Python parser) ──
    if (ext === 'pdf' || ext === 'docx') {
      isParsingServer.value = true
      parsingProgress.value = 5
      parsingStatus.value = 'Đang tải file lên...'
      const res = await uploadExamPdf(selectedFile.value)
      sessionId.value = res.sessionId
      sessionStatus.value = res.status || 'UPLOADED'
      parsingProgress.value = 30
      parsingStatus.value = 'Đang parse với Python...'

      const preview = await waitForExamImportSession(res.sessionId, {
        intervalMs: 1500,
        maxAttempts: 120
      })

      if (preview?.status === 'FAILED' || preview?.questions?.length === 0) {
        importError.value = preview?.report?.warnings?.[0]
          || 'Parse thất bại. Vui lòng thử file khác.'
        return
      }

      parsingProgress.value = 90
      parsingStatus.value = 'Đang tải kết quả...'
      questions = (preview.questions || []).map((q) => {
        const latexContent = q.latexContent ?? q.latex_content ?? null
        const rawLatexOpts = q.latexOptions ?? q.latex_options ?? null
        const rawAnswer = q.correctAnswer ?? q.correctOption ?? q.answer ?? q.correct ?? ''
        const questionNumber = q.number ?? q.questionNumber ?? q.question_index ?? null
        const importQuestionIndex = q.index ?? q.questionIndex ?? q.question_index ?? null
        const normalizedAnswer = /^[A-Da-d]$/.test(String(rawAnswer).trim())
          ? String(rawAnswer).trim().toUpperCase()
          : String(rawAnswer ?? '').trim()
        let optionRows = []
        if (Array.isArray(q.options)) {
          optionRows = q.options.map((o) => ({
            id: o.id || o.key || 'A',
            text: o.text || o.value || ''
          }))
        } else if (typeof q.options === 'object' && q.options !== null) {
          optionRows = Object.entries(q.options).map(([id, text]) => ({
            id: String(id),
            text: String(text ?? '')
          }))
          optionRows.sort((a, b) => a.id.localeCompare(b.id, undefined, { numeric: true }))
        }
        if (optionRows.length === 0) {
          optionRows = collectLegacyOptionRows(q)
        }
        const normalizedRenderState = normalizeImportedRenderState(q)
        return {
          _localId: localIdCounter++,
          content: q.content || q.text || q.question || '',
          correctAnswer: normalizedAnswer,
          score: parseFloat(q.scoreWeight || q.scoreWeight || q.score || q.points || 1),
          options: optionRows,
          type: normalizeQuestionTypeForImport(q.type, normalizedAnswer),
          parseConfidence: q.parseConfidence || q.confidence || null,
          ...normalizedRenderState,
          issues: Array.isArray(q.issues) ? q.issues : [],
          questionNumber,
          importQuestionIndex,
          importSessionId: preview.sessionId || res.sessionId,
          importTemplate: preview.templateUsed || q.template || null,
          importStatus: preview.status || res.status || null,
          _importFingerprint: getFileFingerprint(selectedFile.value),
          explanation: q.explanation || q.solution || q.note || '',
          ...(q.contentType != null && String(q.contentType).trim() !== ''
            ? { contentType: String(q.contentType).trim() }
            : {}),
          ...(latexContent != null && String(latexContent).trim() !== ''
            ? { latexContent: String(latexContent).trim() }
            : {}),
          ...(rawLatexOpts != null && typeof rawLatexOpts === 'object'
            ? { latexOptions: rawLatexOpts }
            : rawLatexOpts != null && String(rawLatexOpts).trim() !== ''
              ? { latexOptions: String(rawLatexOpts) }
              : {})
        }
      })
      parsingProgress.value = 100
      parsingStatus.value = 'Hoàn thành!'
      isParsingServer.value = false

    // ── CSV → BE server-side parser (QuizParserEngine) ──
    } else if (ext === 'csv') {
      try {
        const previewResult = await previewImportFile(selectedFile.value)
        if (previewResult && Array.isArray(previewResult) && previewResult.length > 0) {
          questions = previewResult.map((q) => ({
            _localId: localIdCounter++,
            content: q.content || q.question || q.text || '',
        correctAnswer: q.correctAnswer || q.correctOption || q.answer || q.correct || '',
            score: parseFloat(q.scoreWeight || q.score || q.points || 1),
            options: Array.isArray(q.options)
              ? q.options.map((o) => ({ id: o.id || o.key || 'A', text: o.text || o.value || '' }))
          : collectLegacyOptionRows(q),
            type: q.type || 'SINGLE_CHOICE'
          }))
        } else if (previewResult && previewResult.questions && previewResult.questions.length > 0) {
          questions = previewResult.questions.map((q) => ({
            _localId: localIdCounter++,
            content: q.content || q.question || q.text || '',
        correctAnswer: q.correctAnswer || q.correctOption || q.answer || q.correct || '',
            score: parseFloat(q.scoreWeight || q.score || q.points || 1),
            options: Array.isArray(q.options)
              ? q.options.map((o) => ({ id: o.id || o.key || 'A', text: o.text || o.value || '' }))
          : collectLegacyOptionRows(q),
            type: q.type || 'SINGLE_CHOICE'
          }))
        }
      } catch (apiErr) {
        console.warn('CSV API preview failed, falling back to client parse:', apiErr)
      }
      if (!questions || questions.length === 0) {
        questions = await parseFileClientSide(selectedFile.value)
      }

    // ── XLSX → BE server-side parser ──
    } else if (ext === 'xlsx') {
      try {
        const previewResult = await previewImportFile(selectedFile.value)
        if (previewResult && Array.isArray(previewResult) && previewResult.length > 0) {
          questions = previewResult.map((q) => ({
            _localId: localIdCounter++,
            content: q.content || q.question || q.text || '',
            correctAnswer: q.correctAnswer || q.correctOption || q.answer || q.correct || '',
            score: parseFloat(q.scoreWeight || q.score || q.points || 1),
            options: Array.isArray(q.options)
              ? q.options.map((o) => ({ id: o.id || o.key || 'A', text: o.text || o.value || '' }))
              : collectLegacyOptionRows(q),
            type: q.type || 'SINGLE_CHOICE'
          }))
        } else if (previewResult && previewResult.questions && previewResult.questions.length > 0) {
          questions = previewResult.questions.map((q) => ({
            _localId: localIdCounter++,
            content: q.content || q.question || q.text || '',
            correctAnswer: q.correctAnswer || q.correctOption || q.answer || q.correct || '',
            score: parseFloat(q.scoreWeight || q.score || q.points || 1),
            options: Array.isArray(q.options)
              ? q.options.map((o) => ({ id: o.id || o.key || 'A', text: o.text || o.value || '' }))
              : collectLegacyOptionRows(q),
            type: q.type || 'SINGLE_CHOICE'
          }))
        }
      } catch (apiErr) {
        console.warn('XLSX API preview failed, falling back to client parse:', apiErr)
      }
      if (!questions || questions.length === 0) {
        questions = await parseFileClientSide(selectedFile.value)
      }

    // ── Other formats (json, md…) → client-side only ──
    } else {
      questions = await parseFileClientSide(selectedFile.value)
    }

    if (!questions || questions.length === 0) {
      importError.value = 'Không thể đọc câu hỏi từ file. Vui lòng kiểm tra định dạng file.'
      return
    }

    const normalizedBatch = questions.map((q) => normalizeImportedExamQuestion(q))

    const currentFingerprint = getFileFingerprint(selectedFile.value)
    const trackedBatchIds = getTrackedBatchIdsForFingerprint(currentFingerprint)
    const shouldReplacePreviousBatch =
      (ext === 'pdf' || ext === 'docx')
      && currentFingerprint !== ''
      && trackedBatchIds.length > 0

    const currentQuestions = shouldReplacePreviousBatch
      ? props.questions.filter((q) => !trackedBatchIds.includes(q?._localId))
      : [...props.questions]
    const newQuestions = [...currentQuestions, ...normalizedBatch]
    emit('update:questions', newQuestions)
    rememberImportedBatch(currentFingerprint, normalizedBatch.map((q) => q._localId))

    importedCount.value = questions.length
    importSuccess.value = true

    if (ext !== 'pdf' && ext !== 'docx') {
      selectedFile.value = null
      if (fileInput.value) fileInput.value.value = ''
      if (fileInput2.value) fileInput2.value.value = ''
    }

  } catch (err) {
    if (err?.payload?.message) {
      importError.value = err.payload.message
    } else {
      importError.value = err.message || 'Không thể nhập file. Vui lòng thử lại.'
    }
  } finally {
    isImporting.value = false
    isParsingServer.value = false
    parsingProgress.value = 0
    parsingStatus.value = ''
    resetDragState()
  }
}

onMounted(async () => {
  try {
    const health = await checkPythonParserHealth()
    pythonAvailable.value = Boolean(health?.pythonParserAvailable)
  } catch {
    pythonAvailable.value = false
  }
})
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

.ec-dropzone--sm {
  padding: 1rem;
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

.ec-dropzone__file-icon--sm {
  width: 36px;
  height: 36px;
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

.ec-import-template-picker {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  padding: 0.875rem 1rem;
  border: 1px solid var(--ds-border);
  border-radius: var(--ds-radius-xl);
  background: var(--ds-gray-50);
}

.dark .ec-import-template-picker {
  border-color: var(--ds-border-strong);
  background: var(--ds-gray-800);
}

.ec-import-template-picker--auto {
  gap: 0.625rem;
}

.ec-import-template-picker__hint,
.ec-import-template-picker__status {
  margin: 0;
  font-size: 0.75rem;
  line-height: 1.5;
  color: var(--ds-text-muted);
}

.ec-import-template-picker__status {
  display: inline-flex;
  align-items: center;
  gap: 0.375rem;
}

.ec-import-template-picker__status--down {
  color: var(--ds-danger);
}

.ec-import-success {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 1rem 1.25rem;
  background: var(--ds-success-soft);
  border: 1px solid rgba(22, 163, 74, 0.2);
  border-radius: var(--ds-radius-xl);
}

.ec-import-success__icon {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: var(--ds-success);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.ec-import-success__content {
  flex: 1;
}

.ec-import-success__title {
  font-size: 0.9rem;
  font-weight: 700;
  color: var(--ds-success);
  margin: 0;
}

.ec-import-success__desc {
  font-size: 0.75rem;
  color: var(--ds-text-secondary);
  margin: 0.25rem 0 0;
}

.ec-import-success__add-more {
  display: inline-flex;
  align-items: center;
  gap: 0.375rem;
  padding: 0.5rem 0.875rem;
  border-radius: var(--ds-radius-lg);
  border: 1px solid var(--ds-success);
  background: transparent;
  color: var(--ds-success);
  font-size: 0.75rem;
  font-weight: 600;
  cursor: pointer;
  transition: color 0.15s ease, background-color 0.15s ease, border-color 0.15s ease, box-shadow 0.15s ease, transform 0.15s ease;
}

.ec-import-success__add-more:hover {
  background: var(--ds-success);
  color: white;
}

.ec-import-retry {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.875rem;
  padding: 0.875rem 1rem;
  border: 1px solid var(--ds-border);
  border-radius: var(--ds-radius-xl);
  background: var(--ds-warning-soft);
}

.dark .ec-import-retry {
  border-color: var(--ds-border-strong);
  background: rgba(245, 158, 11, 0.12);
}

.ec-import-retry__copy {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  color: var(--ds-text);
  font-size: 0.8rem;
  font-weight: 600;
}

.ec-add-more {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.ec-add-more__divider {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  font-size: 0.75rem;
  color: var(--ds-text-muted);
}

.ec-add-more__divider::before,
.ec-add-more__divider::after {
  content: '';
  flex: 1;
  height: 1px;
  background: var(--ds-border);
}

.ec-import-actions {
  display: flex;
  gap: 0.75rem;
}

.ec-import-actions--after-dropzone {
  margin-top: 0.25rem;
}

.ec-import-actions--add-more {
  margin-top: 0.75rem;
}

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

.ec-spin { animation: spin 1s linear infinite; display: inline-block; transform: translateZ(0); }
@keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg) translateZ(0); } }

.hidden { display: none; }

.ec-slide-enter-active, .ec-slide-leave-active { transition: color 0.28s cubic-bezier(0.4, 0, 0.2, 1), background-color 0.28s cubic-bezier(0.4, 0, 0.2, 1), border-color 0.28s cubic-bezier(0.4, 0, 0.2, 1), box-shadow 0.28s cubic-bezier(0.4, 0, 0.2, 1), transform 0.28s cubic-bezier(0.4, 0, 0.2, 1); overflow: hidden; }
.ec-slide-enter-from, .ec-slide-leave-to { opacity: 0; max-height: 0; }
.ec-slide-enter-to, .ec-slide-leave-from { opacity: 1; max-height: 200px; }

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

.exam-code-card {
  border: 1px solid var(--ds-border);
  border-radius: var(--ds-radius-xl);
  overflow: hidden;
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

.ecc__stepper-btn:disabled { opacity: 0.4; cursor: not-allowed; }

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

/* ── Server-side parse progress ──────────────────────────────── */

.ec-qb-parse-progress {
  margin-top: 0.75rem;
  padding: 0.875rem 1rem;
  background: var(--ds-primary-soft);
  border: 1px solid rgba(79,70,229,0.15);
  border-radius: var(--ds-radius-xl);
}

.ec-qb-progress-bar {
  height: 6px;
  background: rgba(79,70,229,0.15);
  border-radius: 3px;
  overflow: hidden;
  margin-bottom: 0.5rem;
}

.ec-qb-progress-fill {
  height: 100%;
  background: var(--ds-primary);
  border-radius: 3px;
  transition: width 0.3s ease;
}

.ec-qb-progress-status {
  font-size: 0.78rem;
  color: var(--ds-primary);
  font-weight: 600;
  margin: 0;
  text-align: center;
}
</style>
