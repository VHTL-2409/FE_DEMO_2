<template>
  <Transition name="basm-modal">
    <div v-if="modelValue" class="basm-overlay" @click.self="close">
      <div class="basm-modal">
        <div class="basm-modal__header">
          <div class="basm-modal__icon">
            <LucideIcon name="users-plus" />
          </div>
          <div>
            <h3 class="basm-modal__title">Import học sinh vào lớp</h3>
            <p class="basm-modal__subtitle">{{ className }}</p>
          </div>
          <button type="button" class="basm-modal__close" @click="close">
            <LucideIcon name="x" />
          </button>
        </div>

        <!-- Tab Navigation -->
        <div class="basm-tabs">
          <button type="button" class="basm-tab" :class="{ 'basm-tab--active': activeTab === 'ids' }" @click="activeTab = 'ids'">
            <LucideIcon name="hash" />
            Nhập ID
          </button>
          <button type="button" class="basm-tab" :class="{ 'basm-tab--active': activeTab === 'csv' }" @click="activeTab = 'csv'">
            <LucideIcon name="file-spreadsheet" />
            Import CSV
          </button>
        </div>

        <div class="basm-modal__body">
          <!-- Tab: Nhập ID -->
          <div v-if="activeTab === 'ids'" class="basm-tab-content">
            <div class="basm-form-group">
              <label class="basm-label">Danh sách ID học sinh</label>
              <textarea
                v-model="studentIdsInput"
                class="basm-textarea"
                placeholder="Nhập ID học sinh, mỗi ID trên một dòng&#10;Ví dụ:&#10;1&#10;2&#10;3"
                rows="8"
              ></textarea>
              <span class="basm-hint">{{ validIds.length }} ID hợp lệ</span>
            </div>

            <div class="basm-form-group">
              <label class="basm-checkbox-label">
                <input
                  v-model="isMandatory"
                  type="checkbox"
                  class="basm-checkbox"
                />
                <span class="basm-checkbox-custom"></span>
                <span class="basm-checkbox-text">
                  <strong>Cưỡng chế thêm</strong>
                  <small>Thêm lại học sinh đã có trong lớp (cập nhật lại thời gian tham gia)</small>
                </span>
              </label>
            </div>

            <div v-if="validIds.length > 0" class="basm-preview">
              <div class="basm-preview__header">
                <LucideIcon name="info" />
                <span>Xem trước: {{ validIds.length }} học sinh sẽ được {{ isMandatory ? 'cưỡng chế thêm' : 'thêm' }}</span>
              </div>
              <div class="basm-preview__ids">
                <span v-for="id in validIds.slice(0, 10)" :key="id" class="basm-preview__id">{{ id }}</span>
                <span v-if="validIds.length > 10" class="basm-preview__more">
                  +{{ validIds.length - 10 }} khác
                </span>
              </div>
            </div>
          </div>

          <!-- Tab: Import CSV -->
          <div v-if="activeTab === 'csv'" class="basm-tab-content">
            <div class="basm-form-group">
              <div class="basm-csv-upload-area" :class="{ 'basm-csv-upload-area--dragover': isDragOver }" @dragover.prevent="isDragOver = true" @dragleave="isDragOver = false" @drop.prevent="handleFileDrop">
                <input ref="fileInput" type="file" accept=".csv" class="basm-file-input" @change="handleFileSelect" />
                <LucideIcon name="upload-cloud" class="basm-csv-upload-icon" />
                <p class="basm-csv-upload-text">
                  <strong>Kéo thả file CSV hoặc click để chọn</strong>
                </p>
                <p class="basm-csv-upload-hint">Định dạng: .csv</p>
              </div>
              <button type="button" class="basm-download-template-btn" @click="downloadTemplate">
                <LucideIcon name="download" />
                Tải template CSV mẫu
              </button>
            </div>

            <!-- CSV Preview Table -->
            <div v-if="csvPreview.length > 0" class="basm-csv-preview">
              <div class="basm-preview__header">
                <LucideIcon name="table" />
                <span>Xem trước: {{ csvPreview.length }} học sinh từ file CSV</span>
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
                    <tr v-for="(row, index) in csvPreview.slice(0, 10)" :key="index">
                      <td>{{ row.username || '—' }}</td>
                      <td>{{ row.email || '—' }}</td>
                      <td>{{ row.fullName || '—' }}</td>
                      <td>{{ row.studentCode || '—' }}</td>
                    </tr>
                  </tbody>
                </table>
              </div>
              <p v-if="csvPreview.length > 10" class="basm-csv-more">
                ... và {{ csvPreview.length - 10 }} học sinh khác
              </p>
            </div>
          </div>

          <!-- Import Results -->
          <div v-if="importResult" class="basm-import-result">
            <div class="basm-import-result__header">
              <LucideIcon name="check-circle" class="basm-import-result__icon" />
              <span>Kết quả Import</span>
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
                Danh sách lỗi:
              </p>
              <div class="basm-failed-items">
                <div v-for="(item, index) in failedResults.slice(0, 5)" :key="index" class="basm-failed-item">
                  <span class="basm-failed-item__username">{{ item.username }}</span>
                  <span class="basm-failed-item__message">{{ item.message }}</span>
                </div>
                <p v-if="failedResults.length > 5" class="basm-failed-list__more">
                  ... và {{ failedResults.length - 5 }} lỗi khác
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
            v-if="activeTab === 'ids'"
            type="button"
            class="basm-btn basm-btn--primary"
            :disabled="loading || validIds.length === 0"
            @click="handleSubmitByIds"
          >
            <span v-if="loading" class="basm-spinner"></span>
            <template v-else>
              <LucideIcon name="plus" />
              {{ isMandatory ? 'Cưỡng chế thêm' : 'Thêm' }} {{ validIds.length > 0 ? `${validIds.length} học sinh` : '' }}
            </template>
          </button>
          <button
            v-if="activeTab === 'csv'"
            type="button"
            class="basm-btn basm-btn--primary"
            :disabled="loading || csvPreview.length === 0"
            @click="handleSubmitByCsv"
          >
            <span v-if="loading" class="basm-spinner"></span>
            <template v-else>
              <LucideIcon name="upload" />
              Import {{ csvPreview.length > 0 ? `${csvPreview.length} học sinh` : '' }}
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
import { forceAddStudentsToClass, importStudentsToClass } from '../../../services/classService'
import { useToast } from '../../../composables/useToast'
import LucideIcon from '../../../common/LucideIcon.vue'

const props = defineProps({
  modelValue: Boolean,
  classId: [Number, String],
  className: String
})

const emit = defineEmits(['update:modelValue', 'added'])

const toast = useToast()

// Tab state
const activeTab = ref('csv')

// ID input state
const studentIdsInput = ref('')
const isMandatory = ref(true)

// CSV state
const fileInput = ref(null)
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
  studentIdsInput.value = ''
  isMandatory.value = true
  activeTab.value = 'ids'
  csvPreview.value = []
  csvData.value = []
  importResult.value = null
  errorMessage.value = ''
  loading.value = false
}

const validIds = computed(() => {
  const ids = studentIdsInput.value
    .split(/[\n,]+/)
    .map(s => s.trim())
    .filter(s => s && !isNaN(parseInt(s)))
    .map(s => parseInt(s))
    .filter((id, index, arr) => arr.indexOf(id) === index)
  return ids
})

const close = () => {
  emit('update:modelValue', false)
}

// File handling
const handleFileSelect = (event) => {
  const file = event.target.files?.[0]
  if (file) {
    parseCsvFile(file)
  }
}

const handleFileDrop = (event) => {
  isDragOver.value = false
  const file = event.dataTransfer.files?.[0]
  if (file && file.name.endsWith('.csv')) {
    parseCsvFile(file)
  } else {
    toast.error('Vui lòng chọn file CSV hợp lệ')
  }
}

const parseCsvFile = (file) => {
  const reader = new FileReader()
  reader.onload = (e) => {
    try {
      const content = e.target.result
      const lines = content.split('\n').filter(line => line.trim())
      if (lines.length < 2) {
        toast.error('File CSV không có dữ liệu hoặc thiếu header')
        return
      }

      const headers = lines[0].split(',').map(h => h.trim().toLowerCase())
      const requiredHeaders = ['username']
      const missingHeaders = requiredHeaders.filter(h => !headers.includes(h))

      if (missingHeaders.length > 0) {
        toast.error(`File CSV thiếu cột bắt buộc: ${missingHeaders.join(', ')}`)
        return
      }

      const data = []
      for (let i = 1; i < lines.length; i++) {
        const values = parseCsvLine(lines[i])
        if (values.length >= headers.length) {
          const row = {}
          headers.forEach((header, index) => {
            row[header] = values[index]?.trim() || ''
          })
          if (row.username) {
            data.push(row)
          }
        }
      }

      if (data.length === 0) {
        toast.error('Không đọc được dữ liệu từ file CSV')
        return
      }

      csvData.value = data
      csvPreview.value = data
      toast.success(`Đọc thành công ${data.length} học sinh từ file CSV`)
    } catch (error) {
      toast.error('Không thể đọc file CSV. Vui lòng kiểm tra định dạng.')
    }
  }
  reader.readAsText(file)
}

const parseCsvLine = (line) => {
  const result = []
  let current = ''
  let inQuotes = false

  for (let i = 0; i < line.length; i++) {
    const char = line[i]
    if (char === '"') {
      inQuotes = !inQuotes
    } else if (char === ',' && !inQuotes) {
      result.push(current)
      current = ''
    } else {
      current += char
    }
  }
  result.push(current)
  return result
}

const clearCsv = () => {
  csvPreview.value = []
  csvData.value = []
  if (fileInput.value) {
    fileInput.value.value = ''
  }
}

const downloadTemplate = () => {
  const template = `username,email,fullName,studentCode,birthDate,phone,address,grade,faculty
nguyenvana,nguyenvana@school.edu.vn,Nguyễn Văn A,SV001,2005-01-15,0912345678,Hà Nội,10,Khoa Toán
tranthib,tranthib@school.edu.vn,Trần Thị B,SV002,2005-03-20,0912345679,Hồ Chí Minh,10,Khoa Văn
levanc,levanc@school.edu.vn,Lê Văn C,SV003,2005-05-10,0912345680,Đà Nẵng,10,Khoa Lý`

  const blob = new Blob([template], { type: 'text/csv;charset=utf-8;' })
  const link = document.createElement('a')
  link.href = URL.createObjectURL(blob)
  link.download = 'student-import-template.csv'
  link.click()
  URL.revokeObjectURL(link.href)
  toast.success('Đã tải template CSV')
}

const handleSubmitByIds = async () => {
  if (validIds.value.length === 0) {
    errorMessage.value = 'Vui lòng nhập ít nhất một ID học sinh hợp lệ.'
    return
  }

  loading.value = true
  errorMessage.value = ''
  try {
    await forceAddStudentsToClass(props.classId, validIds.value)
    const action = isMandatory.value ? 'cưỡng chế thêm' : 'thêm'
    toast.success(`Đã ${action} ${validIds.value.length} học sinh vào lớp.`)
    emit('added')
    close()
  } catch (err) {
    errorMessage.value = err instanceof ApiError ? err.message : 'Không thể thêm học sinh.'
    toast.error(errorMessage.value)
  } finally {
    loading.value = false
  }
}

const handleSubmitByCsv = async () => {
  if (csvData.value.length === 0) {
    errorMessage.value = 'Vui lòng chọn file CSV hợp lệ.'
    return
  }

  loading.value = true
  errorMessage.value = ''
  try {
    const result = await importStudentsToClass(props.classId, csvData.value)
    importResult.value = result

    if (result.failedCount > 0 && result.successCount === 0) {
      toast.error(`Import thất bại: ${result.failedCount} học sinh`)
    } else if (result.failedCount > 0) {
      toast.warning(`Import hoàn thành: ${result.successCount} thành công, ${result.failedCount} thất bại`)
    } else {
      toast.success(`Import thành công ${result.successCount} học sinh!`)
    }
    emit('added')
  } catch (err) {
    errorMessage.value = err instanceof ApiError ? err.message : 'Không thể import học sinh.'
    toast.error(errorMessage.value)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.basm-overlay {
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

.basm-modal {
  background: white;
  border-radius: var(--ds-radius-2xl);
  width: 100%;
  max-width: 640px;
  max-height: 85vh;
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
  transition: all 0.12s ease;
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
  transition: all 0.15s ease;
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
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
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
  transition: all 0.12s ease;
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
  transition: all 0.12s ease;
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
  transition: all 0.15s ease;
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
  transition: all 0.12s ease;
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
  transition: all 0.12s ease;
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
  transition: all 0.15s ease;
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

@keyframes spin { to { transform: rotate(360deg); } }

/* Transition */
.basm-modal-enter-active,
.basm-modal-leave-active {
  transition: all 0.2s ease;
}

.basm-modal-enter-from,
.basm-modal-leave-to {
  opacity: 0;
}

.basm-modal-enter-from .basm-modal,
.basm-modal-leave-to .basm-modal {
  transform: scale(0.95) translateY(10px);
}
</style>
