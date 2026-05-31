<template>
  <Transition name="basm-modal">
    <div v-if="modelValue" class="basm-overlay" @click.self="close">
      <div class="basm-modal">
        <div class="basm-modal__header">
          <div class="basm-modal__icon">
            <LucideIcon name="users-plus" />
          </div>
          <div>
            <h3 class="basm-modal__title">Import học sinh</h3>
            <p v-if="className" class="basm-modal__subtitle">{{ className }}</p>
          </div>
          <button type="button" class="basm-modal__close" @click="close">
            <LucideIcon name="x" />
          </button>
        </div>

        <div class="basm-modal__body">
          <div class="basm-form-group">
            <div class="basm-csv-upload-area" :class="{ 'basm-csv-upload-area--dragover': isDragOver }" @dragover.prevent="isDragOver = true" @dragleave="isDragOver = false" @drop.prevent="handleFileDrop">
              <input ref="fileInput" type="file" accept=".csv,.xlsx,.xls" class="basm-file-input" @change="handleFileSelect" />
              <LucideIcon name="upload-cloud" class="basm-csv-upload-icon" />
              <p class="basm-csv-upload-text"><strong>Chọn hoặc kéo thả file</strong></p>
              <p class="basm-csv-upload-hint">.csv, .xlsx, .xls · cột username</p>
            </div>
            <div class="basm-template-actions">
              <button type="button" class="basm-download-template-btn" @click="downloadTemplateCsv">
                <LucideIcon name="download" />
                Tải mẫu CSV
              </button>
              <button type="button" class="basm-download-template-btn" @click="downloadTemplateExcel">
                <LucideIcon name="download" />
                Tải mẫu Excel
              </button>
            </div>
          </div>

          <div v-if="csvPreview.length > 0" class="basm-csv-preview">
            <div class="basm-preview__header">
              <LucideIcon name="table" />
              <span>Xem trước · {{ csvPreview.length }}</span>
              <button type="button" class="basm-clear-csv-btn" @click="clearCsv">
                <LucideIcon name="trash-2" />
              </button>
            </div>
            <div class="basm-table-wrapper">
              <table class="basm-table">
                <thead>
                  <tr>
                    <th>Username</th>
                    <th>Email</th>
                    <th>Họ tên</th>
                    <th>Mã SV</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="(row, index) in csvPreview.slice(0, 20)" :key="index">
                    <td>{{ row.username || '—' }}</td>
                    <td>{{ row.email || '—' }}</td>
                    <td>{{ row.fullName || '—' }}</td>
                    <td>{{ row.studentCode || '—' }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
            <p v-if="csvPreview.length > 20" class="basm-csv-more">
              +{{ csvPreview.length - 20 }}
            </p>
          </div>

          <!-- Import Results -->
          <div v-if="importResult" class="basm-import-result">
            <div class="basm-import-result__header">
              <LucideIcon name="check-circle" class="basm-import-result__icon" />
              <span>Kết quả</span>
            </div>
            <div class="basm-import-result__stats">
              <div class="basm-stat basm-stat--success">
                <span class="basm-stat__value">{{ importResult.successCount }}</span>
                <span class="basm-stat__label">Thành công</span>
              </div>
              <div class="basm-stat basm-stat--created">
                <span class="basm-stat__value">{{ importResult.createdCount }}</span>
                <span class="basm-stat__label">Tạo mới</span>
              </div>
              <div class="basm-stat basm-stat--updated">
                <span class="basm-stat__value">{{ importResult.updatedCount }}</span>
                <span class="basm-stat__label">Cập nhật</span>
              </div>
              <div class="basm-stat basm-stat--failed">
                <span class="basm-stat__value">{{ importResult.failedCount }}</span>
                <span class="basm-stat__label">Thất bại</span>
              </div>
            </div>
            <div v-if="failedResults.length > 0" class="basm-failed-list">
              <p class="basm-failed-list__title">
                <LucideIcon name="alert-triangle" />
                Lỗi
              </p>
              <div class="basm-failed-items">
                <div v-for="(item, index) in failedResults.slice(0, 5)" :key="index" class="basm-failed-item">
                  <span class="basm-failed-item__username">{{ item.username }}</span>
                  <span class="basm-failed-item__message">{{ item.message }}</span>
                </div>
                <p v-if="failedResults.length > 5" class="basm-failed-list__more">
                  +{{ failedResults.length - 5 }}
                </p>
              </div>
            </div>
          </div>

          <div v-if="errorMessage" class="basm-error">
            <LucideIcon name="alert-circle" />
            {{ errorMessage }}
          </div>
        </div>

        <div class="basm-modal__footer">
          <button type="button" class="basm-btn basm-btn--outline" @click="close">Đóng</button>
          <button
            type="button"
            class="basm-btn basm-btn--primary"
            :disabled="loading || !selectedFile"
            @click="handleSubmitByCsv"
          >
            <span v-if="loading" class="basm-spinner"></span>
            <template v-else>
              <LucideIcon name="upload" />
              Gửi
            </template>
          </button>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { ApiError } from '../../../services/apiClient'
import { importStudentsFileToClass } from '../../../services/classService'
import { useToast } from '../../../composables/useToast'
import LucideIcon from '../../common/LucideIcon.vue'

const props = defineProps({
  modelValue: Boolean,
  classId: [Number, String],
  className: String
})

const emit = defineEmits(['update:modelValue', 'added'])

const toast = useToast()

// File + preview (preview is client-side; import uses multipart upload)
const fileInput = ref(null)
const selectedFile = ref(null)
const csvPreview = ref([])
const csvData = ref([])
const isDragOver = ref(false)

// Result state
const importResult = ref(null)
const failedResults = computed(() => importResult.value?.results?.filter(r => !r.success) || [])

// Loading state
const loading = ref(false)
const errorMessage = ref('')

watch(() => props.modelValue, (val) => {
  if (val) {
    resetState()
  }
})

const resetState = () => {
  selectedFile.value = null
  csvPreview.value = []
  csvData.value = []
  importResult.value = null
  errorMessage.value = ''
  loading.value = false
}

const close = () => {
  emit('update:modelValue', false)
}

const isAllowedImportExt = (name) => {
  const n = (name || '').toLowerCase()
  return n.endsWith('.csv') || n.endsWith('.xlsx') || n.endsWith('.xls')
}

// File handling: client preview + keep File for multipart upload
const handleFileSelect = (event) => {
  const file = event.target.files?.[0]
  if (file) {
    parseImportFile(file)
  }
}

const handleFileDrop = (event) => {
  isDragOver.value = false
  const file = event.dataTransfer.files?.[0]
  if (!file) return
  if (isAllowedImportExt(file.name)) {
    parseImportFile(file)
  } else {
    toast.error('Vui lòng chọn file CSV hoặc Excel (.csv, .xlsx, .xls)')
  }
}

const parseImportFile = (file) => {
  clearCsv()
  importResult.value = null
  errorMessage.value = ''
  selectedFile.value = file
  const reader = new FileReader()
  const lower = (file.name || '').toLowerCase()

  reader.onload = (e) => {
    try {
      import('xlsx').then((XLSX) => {
        let rows
        if (lower.endsWith('.csv')) {
          const text = String(e.target.result || '').replace(/^\uFEFF/, '')
          const workbook = XLSX.read(text, { type: 'string', FS: ',', raw: false })
          const sheetName = workbook.SheetNames[0]
          const sheet = workbook.Sheets[sheetName]
          rows = XLSX.utils.sheet_to_json(sheet, {
            defval: '',
            raw: false,
            dateNF: 'yyyy-mm-dd'
          })
          normalizeImportedRows(rows, 'CSV')
        } else {
          const data = new Uint8Array(e.target.result)
          const workbook = XLSX.read(data, { type: 'array' })
          const sheetName = workbook.SheetNames[0]
          const sheet = workbook.Sheets[sheetName]
          rows = XLSX.utils.sheet_to_json(sheet, {
            defval: '',
            raw: false,
            dateNF: 'yyyy-mm-dd'
          })
          normalizeImportedRows(rows, 'Excel')
        }
      }).catch(() => {
        clearCsv()
        toast.error('Không thể đọc file. Vui lòng kiểm tra định dạng.')
      })
    } catch {
      clearCsv()
      toast.error('Không thể đọc file import. Vui lòng kiểm tra định dạng.')
    }
  }

  if (lower.endsWith('.csv')) {
    reader.readAsText(file, 'UTF-8')
  } else {
    reader.readAsArrayBuffer(file)
  }
}

const clearCsv = () => {
  selectedFile.value = null
  csvPreview.value = []
  csvData.value = []
  if (fileInput.value) {
    fileInput.value.value = ''
  }
}

const normalizeImportedRows = (rows, sourceLabel) => {
  const normalizedRows = (rows || []).map((row) => {
    const normalized = {}
    Object.entries(row || {}).forEach(([key, value]) => {
      normalized[String(key || '').trim().toLowerCase()] = typeof value === 'string' ? value.trim() : value
    })
    return {
      username: String(normalized.username || '').trim(),
      email: String(normalized.email || '').trim(),
      fullName: String(normalized.fullname || normalized.full_name || normalized['họ tên'] || '').trim(),
      studentCode: String(normalized.studentcode || normalized.student_code || normalized['mã sv'] || '').trim(),
      birthDate: String(normalized.birthdate || normalized.birth_date || '').trim(),
      phone: String(normalized.phone || '').trim(),
      address: String(normalized.address || '').trim(),
      grade: String(normalized.grade || '').trim(),
      faculty: String(normalized.faculty || '').trim()
    }
  }).filter((row) => row.username)

  if (normalizedRows.length === 0) {
    clearCsv()
    toast.error(`Không đọc được dữ liệu học sinh từ file ${sourceLabel}`)
    return
  }

  csvData.value = normalizedRows
  csvPreview.value = normalizedRows
  errorMessage.value = ''
  if (import.meta.env.DEV) {
    console.log(`[import preview] ${sourceLabel}: ${normalizedRows.length} rows`)
  }
}

const downloadTemplateExcel = () => {
  import('xlsx').then((XLSX) => {
    const rows = [
      ['username', 'email', 'fullName', 'studentCode', 'birthDate', 'phone', 'address', 'grade', 'faculty'],
      ['nguyenvana', 'nguyenvana@school.edu.vn', 'Nguyen Van A', 'SV001', '2005-01-15', '0912345678', 'Ha Noi', '10', 'Khoa Toan'],
      ['tranthib', 'tranthib@school.edu.vn', 'Tran Thi B', 'SV002', '2005-03-20', '0912345679', 'Ho Chi Minh', '10', 'Khoa Van'],
      ['levanc', 'levanc@school.edu.vn', 'Le Van C', 'SV003', '2005-05-10', '0912345680', 'Da Nang', '10', 'Khoa Ly']
    ]
    const worksheet = XLSX.utils.aoa_to_sheet(rows)
    const workbook = XLSX.utils.book_new()
    XLSX.utils.book_append_sheet(workbook, worksheet, 'Students')
    XLSX.writeFile(workbook, 'student-import-template.xlsx')
    toast.success('Đã tải mẫu Excel')
  }).catch(() => {
    toast.error('Không thể tạo file mẫu Excel.')
  })
}

const downloadTemplateCsv = () => {
  const lines = [
    'username,email,fullName,studentCode,birthDate,phone,address,grade,faculty',
    'nguyenvana,nguyenvana@school.edu.vn,Nguyen Van A,SV001,2005-01-15,0912345678,Ha Noi,10,Khoa Toan',
    'tranthib,tranthib@school.edu.vn,Tran Thi B,SV002,2005-03-20,0912345679,Ho Chi Minh,10,Khoa Van',
    'levanc,levanc@school.edu.vn,Le Van C,SV003,2005-05-10,0912345680,Da Nang,10,Khoa Ly'
  ]
  const blob = new Blob([lines.join('\n')], { type: 'text/csv;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = 'student-import-template.csv'
  a.click()
  URL.revokeObjectURL(url)
  toast.success('Đã tải mẫu CSV')
}

const handleSubmitByCsv = async () => {
  if (!selectedFile.value) {
    errorMessage.value = 'Vui lòng chọn file CSV hoặc Excel.'
    return
  }

  loading.value = true
  errorMessage.value = ''
  try {
    const result = await importStudentsFileToClass(props.classId, selectedFile.value)
    importResult.value = result
    const successCount = result?.successCount ?? 0
    const failedCount = result?.failedCount ?? 0
    if (failedCount > 0 && successCount === 0) {
      toast.error('Import thất bại — xem chi tiết bên dưới.')
    } else if (failedCount > 0) {
      toast.warning(`Import một phần: ${successCount} thành công, ${failedCount} lỗi.`)
    } else {
      toast.success(`Import thành công: ${successCount} học sinh.`)
    }
    emit('added')
  } catch (err) {
    const msg = err instanceof ApiError ? err.message : 'Không thể gửi file import. Vui lòng thử lại.'
    errorMessage.value = msg
    toast.error(msg)
  } finally {
    loading.value = false
  }
}

</script>

<style scoped>

.basm-template-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  margin-top: 0.25rem;
}
.basm-overlay {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.56);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: 1.5rem;
}

.basm-modal {
  background: white;
  border-radius: var(--ds-radius-2xl);
  width: 100%;
  max-width: min(960px, 96vw);
  max-height: 90vh;
  display: flex;
  flex-direction: column;
  box-shadow: 0 24px 64px rgba(0,0,0,0.2);
  overflow: hidden;
}

.dark .basm-modal { background: var(--ds-gray-800); }

.basm-modal__header {
  display: flex;
  align-items: flex-start;
  gap: 0.875rem;
  padding: 1.5rem;
  border-bottom: 1px solid var(--ds-border);
  flex-shrink: 0;
}

.dark .basm-modal__header { border-bottom-color: var(--ds-border-strong); }

.basm-modal__icon {
  width: 44px;
  height: 44px;
  border-radius: var(--ds-radius-xl);
  background: var(--ds-primary-soft);
  color: var(--ds-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.basm-modal__title {
  font-family: var(--ds-font-display);
  font-size: 1rem;
  font-weight: 800;
  color: var(--ds-text);
  margin: 0;
  line-height: 1.3;
}

.dark .basm-modal__title { color: var(--ds-text); }

.basm-modal__subtitle {
  font-size: 0.8rem;
  color: var(--ds-text-muted);
  margin: 0.25rem 0 0;
}

.basm-modal__close {
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
  transition: color 0.12s ease, background-color 0.12s ease, border-color 0.12s ease, box-shadow 0.12s ease, transform 0.12s ease;
  flex-shrink: 0;
}

.dark .basm-modal__close { background: var(--ds-gray-700); }
.basm-modal__close:hover { background: var(--ds-gray-200); color: var(--ds-text); }
.dark .basm-modal__close:hover { background: var(--ds-gray-600); }

/* Tabs */
.basm-tabs {
  display: flex;
  gap: 0;
  border-bottom: 2px solid var(--ds-border);
  padding: 0 1.5rem;
  background: var(--ds-gray-50);
  flex-shrink: 0;
}

.dark .basm-tabs {
  background: var(--ds-gray-800);
  border-bottom-color: var(--ds-border-strong);
}

.basm-tab {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem 1rem;
  border: none;
  background: transparent;
  color: var(--ds-text-muted);
  font-size: 0.875rem;
  font-weight: 600;
  cursor: pointer;
  border-bottom: 2px solid transparent;
  margin-bottom: -2px;
  transition: color 0.15s ease, background-color 0.15s ease, border-color 0.15s ease, box-shadow 0.15s ease, transform 0.15s ease;
}

.basm-tab:hover { color: var(--ds-text); }

.basm-tab--active {
  color: var(--ds-primary);
  border-bottom-color: var(--ds-primary);
}

.basm-tab-content {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.basm-modal__body {
  flex: 1;
  overflow-y: auto;
  padding: 1.5rem;
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.basm-form-group {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.basm-search-box {
  position: relative;
}

.basm-search-box__icon {
  position: absolute;
  left: 1rem;
  top: 50%;
  transform: translateY(-50%);
  color: var(--ds-text-muted);
}

.basm-search-box__input {
  width: 100%;
  padding: 0.875rem 1rem 0.875rem 3rem;
  border: 2px solid var(--ds-border);
  border-radius: var(--ds-radius-xl);
  background: var(--ds-surface);
  color: var(--ds-text);
  font-size: 0.9375rem;
}

.dark .basm-search-box__input {
  background: var(--ds-gray-800);
  border-color: var(--ds-border-strong);
}

.basm-search-state,
.basm-search-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.75rem;
  padding: 1rem;
  border: 1px dashed var(--ds-border);
  border-radius: var(--ds-radius-xl);
  color: var(--ds-text-muted);
}

.basm-student-list {
  display: flex;
  flex-direction: column;
  gap: 0.625rem;
  max-height: 280px;
  overflow-y: auto;
}

.basm-student-option {
  display: flex;
  align-items: flex-start;
  gap: 0.75rem;
  padding: 0.875rem 1rem;
  border: 1px solid var(--ds-border);
  border-radius: var(--ds-radius-xl);
  background: var(--ds-surface);
  cursor: pointer;
}

.dark .basm-student-option {
  background: var(--ds-gray-800);
  border-color: var(--ds-border-strong);
}

.basm-student-option__checkbox {
  margin-top: 0.2rem;
}

.basm-student-option__body {
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
  min-width: 0;
}

.basm-student-option__name {
  font-size: 0.875rem;
  font-weight: 700;
  color: var(--ds-text);
}

.basm-student-option__meta,
.basm-student-option__id {
  font-size: 0.75rem;
  color: var(--ds-text-muted);
}

.basm-label {
  font-size: 0.8rem;
  font-weight: 700;
  color: var(--ds-text);
}

.dark .basm-label { color: var(--ds-text); }

.basm-textarea {
  width: 100%;
  padding: 1rem 1.25rem;
  border: 2px solid var(--ds-border);
  border-radius: var(--ds-radius-2xl);
  font-size: 0.9375rem;
  background: var(--ds-surface);
  color: var(--ds-text);
  transition: color 0.25s cubic-bezier(0.4, 0, 0.2, 1), background-color 0.25s cubic-bezier(0.4, 0, 0.2, 1), border-color 0.25s cubic-bezier(0.4, 0, 0.2, 1), box-shadow 0.25s cubic-bezier(0.4, 0, 0.2, 1), transform 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  font-family: inherit;
  resize: vertical;
  line-height: 1.6;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.dark .basm-textarea {
  background: var(--ds-gray-800);
  border-color: var(--ds-border-strong);
  color: var(--ds-text);
}

.bas-m-textarea:hover {
  border-color: var(--ds-primary-border);
  box-shadow: 0 4px 16px rgba(79, 70, 229, 0.1);
}

.dark .basm-textarea:hover {
  border-color: var(--ds-primary-border);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);
}

.basm-textarea:focus {
  outline: none;
  border-color: var(--ds-primary);
  box-shadow: 0 0 0 4px var(--ds-primary-ring), 0 8px 24px rgba(79, 70, 229, 0.15);
  transform: translateY(-1px);
}

.dark .basm-textarea:focus {
  box-shadow: 0 0 0 4px rgba(79, 70, 229, 0.3), 0 8px 24px rgba(0, 0, 0, 0.3);
}

.basm-hint {
  font-size: 0.7rem;
  color: var(--ds-text-muted);
  text-align: right;
}

.dark .basm-hint { color: var(--ds-text-muted); }

.basm-checkbox-label {
  display: flex;
  align-items: flex-start;
  gap: 0.75rem;
  cursor: pointer;
  padding: 1rem;
  background: var(--ds-gray-50);
  border: 1.5px solid var(--ds-border);
  border-radius: var(--ds-radius-xl);
  transition: color 0.12s ease, background-color 0.12s ease, border-color 0.12s ease, box-shadow 0.12s ease, transform 0.12s ease;
}

.dark .basm-checkbox-label {
  background: var(--ds-gray-700);
  border-color: var(--ds-border-strong);
}

.basm-checkbox-label:hover {
  border-color: var(--ds-primary);
}

.basm-checkbox {
  display: none;
}

.basm-checkbox-custom {
  width: 22px;
  height: 22px;
  min-width: 22px;
  border: 2px solid var(--ds-border);
  border-radius: 6px;
  background: var(--ds-surface);
  display: flex;
  align-items: center;
  justify-content: center;
  transition: color 0.12s ease, background-color 0.12s ease, border-color 0.12s ease, box-shadow 0.12s ease, transform 0.12s ease;
  margin-top: 2px;
}

.dark .basm-checkbox-custom {
  background: var(--ds-gray-600);
  border-color: var(--ds-border-strong);
}

.basm-checkbox:checked + .basm-checkbox-custom {
  background: var(--ds-primary);
  border-color: var(--ds-primary);
}

.basm-checkbox:checked + .basm-checkbox-custom::after {
  content: '';
  width: 6px;
  height: 10px;
  border: solid white;
  border-width: 0 2px 2px 0;
  transform: rotate(45deg) translate(-1px, -1px);
}

.basm-checkbox-text {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.basm-checkbox-text strong {
  font-size: 0.875rem;
  color: var(--ds-text);
  font-weight: 700;
}

.dark .basm-checkbox-text strong { color: var(--ds-text); }

.basm-checkbox-text small {
  font-size: 0.75rem;
  color: var(--ds-text-muted);
  line-height: 1.4;
}

.dark .basm-checkbox-text small { color: var(--ds-text-muted); }

/* CSV Upload */
.basm-csv-upload-area {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 2rem;
  border: 2px dashed var(--ds-border);
  border-radius: var(--ds-radius-xl);
  background: var(--ds-gray-50);
  cursor: pointer;
  transition: color 0.15s ease, background-color 0.15s ease, border-color 0.15s ease, box-shadow 0.15s ease, transform 0.15s ease;
}

.dark .basm-csv-upload-area {
  background: var(--ds-gray-700);
  border-color: var(--ds-border-strong);
}

.basm-csv-upload-area:hover,
.basm-csv-upload-area--dragover {
  border-color: var(--ds-primary);
  background: var(--ds-primary-soft);
}

.basm-file-input {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  opacity: 0;
  cursor: pointer;
}

.basm-csv-upload-icon {
  width: 48px;
  height: 48px;
  color: var(--ds-text-muted);
  margin-bottom: 0.75rem;
}

.basm-csv-upload-area:hover .basm-csv-upload-icon,
.basm-csv-upload-area--dragover .basm-csv-upload-icon {
  color: var(--ds-primary);
}

.basm-csv-upload-text {
  font-size: 0.875rem;
  color: var(--ds-text);
  margin: 0;
  text-align: center;
}

.dark .basm-csv-upload-text { color: var(--ds-text); }

.basm-csv-upload-hint {
  font-size: 0.75rem;
  color: var(--ds-text-muted);
  margin: 0.25rem 0 0;
}

.basm-download-template-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 0.875rem;
  background: var(--ds-surface);
  border: 1.5px solid var(--ds-border);
  border-radius: var(--ds-radius-lg);
  font-size: 0.8rem;
  font-weight: 600;
  color: var(--ds-primary);
  cursor: pointer;
  transition: color 0.12s ease, background-color 0.12s ease, border-color 0.12s ease, box-shadow 0.12s ease, transform 0.12s ease;
  margin-top: 0.5rem;
  align-self: flex-start;
}

.dark .basm-download-template-btn {
  background: var(--ds-gray-700);
  border-color: var(--ds-border-strong);
  color: var(--ds-primary);
}

.basm-download-template-btn:hover {
  background: var(--ds-primary-soft);
  border-color: var(--ds-primary);
}

/* CSV Preview Table */
.basm-csv-preview {
  background: var(--ds-surface);
  border: 1px solid var(--ds-border);
  border-radius: var(--ds-radius-xl);
  overflow: hidden;
}

.dark .basm-csv-preview {
  background: var(--ds-gray-700);
  border-color: var(--ds-border-strong);
}

.basm-preview {
  background: var(--ds-info-soft);
  border: 1px solid rgba(2, 132, 199, 0.2);
  border-radius: var(--ds-radius-xl);
  padding: 1rem;
}

.dark .basm-preview {
  background: rgba(2, 132, 199, 0.1);
  border-color: rgba(2, 132, 199, 0.3);
}

.basm-preview__header {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.8rem;
  font-weight: 700;
  color: var(--ds-info);
  margin-bottom: 0.75rem;
}

.basm-preview__header button {
  margin-left: auto;
  padding: 0.25rem;
  border: none;
  background: transparent;
  color: var(--ds-text-muted);
  cursor: pointer;
  border-radius: 4px;
  transition: color 0.12s ease, background-color 0.12s ease, border-color 0.12s ease, box-shadow 0.12s ease, transform 0.12s ease;
}

.basm-preview__header button:hover {
  background: var(--ds-danger-soft);
  color: var(--ds-danger);
}

.dark .basm-preview__header { color: var(--ds-info); }

.basm-preview__ids {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.basm-preview__id {
  padding: 0.375rem 0.75rem;
  background: var(--ds-surface);
  border: 1px solid var(--ds-border);
  border-radius: var(--ds-radius-lg);
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--ds-text);
}

.dark .basm-preview__id {
  background: var(--ds-gray-700);
  border-color: var(--ds-border-strong);
  color: var(--ds-text);
}

.basm-preview__more {
  padding: 0.375rem 0.75rem;
  background: var(--ds-primary-soft);
  border-radius: var(--ds-radius-lg);
  font-size: 0.75rem;
  font-weight: 700;
  color: var(--ds-primary);
}

.dark .basm-preview__more { background: rgba(79, 70, 229, 0.2); color: var(--ds-primary); }

.basm-table-wrapper {
  overflow-x: auto;
}

.basm-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.8rem;
}

.basm-table th,
.basm-table td {
  padding: 0.625rem 0.875rem;
  text-align: left;
  border-bottom: 1px solid var(--ds-border);
}

.dark .basm-table th,
.dark .basm-table td {
  border-bottom-color: var(--ds-border-strong);
}

.basm-table th {
  background: var(--ds-gray-100);
  font-weight: 700;
  color: var(--ds-text);
}

.dark .basm-table th {
  background: var(--ds-gray-600);
  color: var(--ds-text);
}

.basm-table td {
  color: var(--ds-text-secondary);
}

.dark .basm-table td { color: var(--ds-text-muted); }

.basm-table tbody tr:last-child td {
  border-bottom: none;
}

.basm-csv-more {
  padding: 0.625rem 0.875rem;
  font-size: 0.75rem;
  color: var(--ds-text-muted);
  text-align: center;
  background: var(--ds-gray-50);
  margin: 0;
}

.dark .basm-csv-more {
  background: var(--ds-gray-600);
  color: var(--ds-text-muted);
}

/* Import Results */
.basm-import-result {
  background: var(--ds-success-soft);
  border: 1px solid rgba(34, 197, 94, 0.2);
  border-radius: var(--ds-radius-xl);
  padding: 1rem;
}

.dark .basm-import-result {
  background: rgba(34, 197, 94, 0.1);
  border-color: rgba(34, 197, 94, 0.3);
}

.basm-import-result__header {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.875rem;
  font-weight: 700;
  color: var(--ds-success);
  margin-bottom: 0.875rem;
}

.dark .basm-import-result__header { color: #4ade80; }

.basm-import-result__icon {
  width: 20px;
  height: 20px;
}

.basm-import-result__stats {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 0.75rem;
}

.basm-stat {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 0.75rem;
  background: var(--ds-surface);
  border-radius: var(--ds-radius-lg);
  text-align: center;
}

.dark .basm-stat { background: var(--ds-gray-700); }

.basm-stat__value {
  font-size: 1.25rem;
  font-weight: 800;
  line-height: 1;
}

.basm-stat__label {
  font-size: 0.65rem;
  color: var(--ds-text-muted);
  margin-top: 0.25rem;
  text-transform: uppercase;
  letter-spacing: 0.025em;
}

.dark .basm-stat__label { color: var(--ds-text-muted); }

.basm-stat--success .basm-stat__value { color: var(--ds-success); }
.basm-stat--created .basm-stat__value { color: var(--ds-primary); }
.basm-stat--updated .basm-stat__value { color: var(--ds-info); }
.basm-stat--failed .basm-stat__value { color: var(--ds-danger); }

.basm-failed-list {
  margin-top: 0.875rem;
  padding-top: 0.875rem;
  border-top: 1px solid rgba(220, 38, 38, 0.2);
}

.dark .basm-failed-list { border-top-color: rgba(220, 38, 38, 0.3); }

.basm-failed-list__title {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  font-size: 0.8rem;
  font-weight: 700;
  color: var(--ds-danger);
  margin: 0 0 0.5rem;
}

.basm-failed-items {
  display: flex;
  flex-direction: column;
  gap: 0.375rem;
}

.basm-failed-item {
  display: flex;
  flex-direction: column;
  gap: 0.125rem;
  padding: 0.5rem 0.75rem;
  background: var(--ds-surface);
  border-radius: var(--ds-radius-md);
  font-size: 0.75rem;
}

.dark .basm-failed-item { background: var(--ds-gray-600); }

.basm-failed-item__username {
  font-weight: 700;
  color: var(--ds-text);
}

.dark .basm-failed-item__username { color: var(--ds-text); }

.basm-failed-item__message {
  color: var(--ds-danger);
}

.basm-failed-list__more {
  font-size: 0.7rem;
  color: var(--ds-text-muted);
  text-align: center;
  margin: 0.5rem 0 0;
}

.dark .basm-failed-list__more { color: var(--ds-text-muted); }

.basm-error {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.875rem 1rem;
  background: var(--ds-danger-soft);
  border: 1px solid rgba(220, 38, 38, 0.2);
  border-radius: var(--ds-radius-xl);
  font-size: 0.8rem;
  color: var(--ds-danger);
  font-weight: 600;
}

.dark .basm-error {
  background: rgba(220, 38, 38, 0.1);
  border-color: rgba(220, 38, 38, 0.3);
}

.basm-modal__footer {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 0.75rem;
  padding: 1.25rem 1.5rem;
  border-top: 1px solid var(--ds-border);
  background: var(--ds-gray-50);
  flex-shrink: 0;
}

.dark .basm-modal__footer {
  border-top-color: var(--ds-border-strong);
  background: var(--ds-gray-800);
}

.basm-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.625rem 1.125rem;
  border-radius: var(--ds-radius-xl);
  font-size: 0.875rem;
  font-weight: 700;
  cursor: pointer;
  transition: color 0.15s ease, background-color 0.15s ease, border-color 0.15s ease, box-shadow 0.15s ease, transform 0.15s ease;
  border: 1.5px solid transparent;
  white-space: nowrap;
}

.basm-btn--primary {
  background: linear-gradient(135deg, var(--ds-primary) 0%, #6366f1 100%);
  color: white;
  box-shadow: 0 4px 12px rgba(79, 70, 229, 0.25);
}

.basm-btn--primary:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 6px 20px rgba(79, 70, 229, 0.35);
}

.basm-btn--outline {
  background: var(--ds-surface);
  border-color: var(--ds-border);
  color: var(--ds-text-secondary);
}

.dark .basm-btn--outline {
  background: var(--ds-gray-700);
  border-color: var(--ds-border-strong);
  color: var(--ds-text-muted);
}

.basm-btn--outline:hover { border-color: var(--ds-primary); color: var(--ds-primary); }
.basm-btn:disabled { opacity: 0.5; cursor: not-allowed; transform: none !important; }

.basm-spinner {
  width: 16px;
  height: 16px;
  border: 2px solid rgba(255,255,255,0.3);
  border-top-color: white;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

.basm-spinner--dark {
  border-color: rgba(79, 70, 229, 0.2);
  border-top-color: var(--ds-primary);
}

@keyframes spin { to { transform: rotate(360deg) translateZ(0); } }

/* Transition */
.basm-modal-enter-active,
.basm-modal-leave-active {
  transition: opacity 0.2s ease;
}

.basm-modal-enter-from,
.basm-modal-leave-to {
  opacity: 0;
}

.basm-modal-enter-from .basm-modal,
.basm-modal-leave-to .basm-modal {
  opacity: 0;
}
</style>
