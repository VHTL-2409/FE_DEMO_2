<template>
  <div class="eip">
    <!-- ── Page header ── -->
    <div class="eip__header">
      <div class="eip__header-left">
        <div class="eip__header-icon">
          <LucideIcon name="upload_file" />
        </div>
        <div>
          <h2 class="eip__header-title">Nhập đề thi PDF</h2>
          <p class="eip__header-sub">Parse đề thi từ file PDF với template detection tự động</p>
        </div>
      </div>
      <div class="eip__header-right">
        <!-- Python service status -->
        <span
          v-if="pythonServiceStatus !== null"
          class="eip__service-chip"
          :class="pythonServiceStatus ? 'eip__service-chip--ok' : 'eip__service-chip--warn'"
        >
          <LucideIcon :name="pythonServiceStatus ? 'check_circle' : 'warning'" size="12" />
          Python Parser {{ pythonServiceStatus ? 'Online' : 'Offline' }}
        </span>
        <!-- Template selector -->
        <select v-model="forceTemplate" class="eip__template-select">
          <option value="">Tự động</option>
          <option value="template_01_math_broken">Toán vỡ (Template 1)</option>
          <option value="template_02_clean_mcq">Tiếng Anh sạch (Template 2)</option>
          <option value="template_03_math_answer_grid">Toán đáp án lưới (Template 3)</option>
        </select>
      </div>
    </div>

    <div class="eip__body">

      <!-- ── LEFT: Upload + Preview ── -->
      <div class="eip__main-col">

        <!-- Upload zone (hidden when session active) -->
        <div v-if="!activeSessionId" class="eip__upload-zone" :class="{ 'eip__upload-zone--dragover': isDragging }"
          @dragover.prevent="isDragging = true"
          @dragleave.prevent="isDragging = false"
          @drop.prevent="handleDrop"
        >
          <input
            ref="fileInputRef"
            type="file"
            accept=".pdf,.docx"
            class="eip__file-input"
            @change="handleFileChange"
          />

          <div class="eip__upload-icon">
            <LucideIcon name="cloud_upload" size="36" />
          </div>
          <h3 class="eip__upload-title">Kéo thả file PDF / DOCX vào đây</h3>
          <p class="eip__upload-sub">hoặc click để chọn file · hỗ trợ PDF, DOCX</p>
          <p class="eip__upload-sub eip__upload-sub--small">
            Template tự động: Toán vỡ · Tiếng Anh sạch · Toán đáp án lưới · Thương mại điện tử · Cơ sở dữ liệu
          </p>
          <button type="button" class="eip__upload-btn" @click="fileInputRef?.click()" :disabled="isUploading">
            <LucideIcon name="folder_open" size="16" />
            Chọn file PDF
          </button>
        </div>

        <!-- Parsing progress -->
        <div v-else-if="sessionStatus === 'UPLOADED' || sessionStatus === 'PARSING'" class="eip__parsing">
          <div class="eip__parsing-inner">
            <div class="eip__parsing-spinner">
              <LucideIcon name="sync" size="32" class="eip__spin-icon" />
            </div>
            <h3 class="eip__parsing-title">Đang parse file PDF...</h3>
            <p class="eip__parsing-sub">Template detection · Trích xuất câu hỏi · Crop hình ảnh</p>
            <div class="eip__progress-bar">
              <div class="eip__progress-fill" :style="{ width: `${parsingProgress}%` }" />
            </div>
            <p class="eip__parsing-status">{{ parsingStatus }}</p>
          </div>
        </div>

        <!-- Session actions bar -->
        <div v-if="activeSessionId && sessionStatus === 'DONE'" class="eip__session-bar">
          <div class="eip__session-info">
            <LucideIcon name="check_circle" size="16" class="eip__session-icon" />
            <span class="eip__session-label">Đã parse xong</span>
            <span class="eip__session-file">{{ fileName }}</span>
          </div>
          <div class="eip__session-actions">
            <button type="button" class="eip__action-btn eip__action-btn--outline" @click="resetSession">
              <LucideIcon name="add" size="14" />
              Upload file khác
            </button>
            <button type="button" class="eip__action-btn eip__action-btn--outline" @click="refreshPreview">
              <LucideIcon name="refresh" size="14" />
              Làm mới
            </button>
          </div>
        </div>

        <!-- Questions list -->
        <div v-if="parsedQuestions.length > 0" class="eip__questions">
          <div class="eip__qlist-header">
            <div class="eip__qlist-title">
              <LucideIcon name="quiz" size="16" />
              <span>Danh sách câu hỏi ({{ parsedQuestions.length }})</span>
            </div>
            <div class="eip__qlist-filters">
              <select v-model="filterType" class="eip__filter-select">
                <option value="">Tất cả</option>
                <option value="multiple_choice">Trắc nghiệm</option>
                <option value="essay">Tự luận</option>
              </select>
              <select v-model="filterConfidence" class="eip__filter-select">
                <option value="">Mọi độ tin</option>
                <option value="high">Độ tin cao (≥80%)</option>
                <option value="low">Độ tin thấp (&lt;70%)</option>
              </select>
              <button type="button" class="eip__filter-btn" @click="toggleAllExpanded">
                <LucideIcon :name="allExpanded ? 'unfold_less' : 'unfold_more'" size="14" />
                {{ allExpanded ? 'Thu gọn' : 'Mở rộng' }}
              </button>
            </div>
          </div>

          <div class="eip__qlist portal-scrollbar">
            <article
              v-for="(q, idx) in filteredQuestions"
              :key="q.number || idx"
              class="eip__qcard"
              :class="{
                'eip__qcard--expanded': expandedIds.has(idx),
                'eip__qcard--low-conf': (q.confidence || 0) < 0.7,
                'eip__qcard--essay': q.type === 'essay'
              }"
            >
              <!-- Card header -->
              <button type="button" class="eip__qcard-header" @click="toggleExpand(idx)">
                <div class="eip__qcard-left">
                  <span class="eip__qnum">#{{ q.number || idx + 1 }}</span>
                  <span class="eip__type-badge" :class="`eip__type-badge--${typeSlug(q.type)}`">
                    {{ typeLabel(q.type) }}
                  </span>
                  <span v-if="(q.confidence || 0) < 0.7" class="eip__conf-warn">
                    <LucideIcon name="warning" size="12" />
                    Thấp
                  </span>
                  <span v-if="q.render?.mode === 'image'" class="eip__img-badge">
                    <LucideIcon name="image" size="12" />
                    Hình
                  </span>
                </div>
                <div class="eip__qcard-right">
                  <span v-if="q.answer" class="eip__answer-preview">
                    <LucideIcon name="check" size="13" />
                    {{ q.answer }}
                  </span>
                  <LucideIcon
                    :name="expandedIds.has(idx) ? 'expand_less' : 'expand_more'"
                    size="18"
                    class="eip__expand-icon"
                  />
                </div>
              </button>

              <!-- Card body -->
              <div v-if="expandedIds.has(idx)" class="eip__qcard-body">
                <QuestionRenderer
                  :question="q"
                  :session-id="activeSessionId"
                  :default-mode="'auto'"
                />
                <!-- Quick edit: correct answer selector -->
                <div class="eip__qcard-edit">
                  <span class="eip__edit-label">Đáp án đúng:</span>
                  <div class="eip__correct-group">
                    <button
                      v-for="opt in ['A','B','C','D']"
                      :key="opt"
                      type="button"
                      class="eip__correct-btn"
                      :class="{ 'eip__correct-btn--active': q.answer === opt }"
                      @click="setAnswer(idx, opt)"
                    >{{ opt }}</button>
                  </div>
                </div>
              </div>
            </article>
          </div>
        </div>

        <!-- No questions yet -->
        <div v-if="activeSessionId && parsedQuestions.length === 0 && sessionStatus === 'DONE'" class="eip__no-questions">
          <LucideIcon name="search_off" size="32" />
          <p>Không tìm thấy câu hỏi trong file PDF.</p>
          <p class="eip__no-questions-sub">File có thể chứa hình ảnh thay vì text. Thử file khác.</p>
        </div>
      </div>

      <!-- ── RIGHT: Report panel + Confirm ── -->
      <div class="eip__side-col">
        <ParseReportPanel
          :report="report"
          @goto-question="scrollToQuestion"
        />

        <!-- Exam selection -->
        <div v-if="parsedQuestions.length > 0" class="eip__exam-select-card">
          <label class="eip__exam-select-label">
            <LucideIcon name="assignment" size="14" />
            Đề thi đích
          </label>
          <select v-model="selectedExamId" class="eip__exam-select">
            <option value="">Chọn đề thi...</option>
            <option v-for="exam in availableExams" :key="exam.id" :value="exam.id">
              {{ exam.title || `Đề #${exam.id}` }}
            </option>
          </select>
          <p v-if="!selectedExamId" class="eip__exam-hint">
            Có thể chọn sau khi import
          </p>
        </div>

        <!-- Confirm button -->
        <div v-if="parsedQuestions.length > 0" class="eip__confirm-card">
          <div class="eip__confirm-summary">
            <span>{{ parsedQuestions.length }} câu hỏi</span>
            <span class="eip__confirm-dot">·</span>
            <span>{{ answeredCount }} đáp án</span>
          </div>
          <button
            type="button"
            class="eip__confirm-btn"
            :disabled="isConfirming || parsedQuestions.length === 0"
            @click="handleConfirm"
          >
            <LucideIcon name="check_circle" size="16" />
            {{ isConfirming ? 'Đang import...' : 'Xác nhận Import' }}
          </button>
          <p v-if="confirmError" class="eip__confirm-error">{{ confirmError }}</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, reactive, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import QuestionRenderer from './QuestionRenderer.vue'
import ParseReportPanel from './ParseReportPanel.vue'
import {
  uploadExamPdf,
  getExamImportPreview,
  getExamImportSessions,
  confirmExamImport,
  waitForExamImportSession,
  checkPythonParserHealth
} from '../../../services/importService'
import { listExams } from '../../../services/examService'
import { useToast } from '../../../composables/useToast'

const router = useRouter()
const toast = useToast()

// ─── File input ──────────────────────────────────────────────
const fileInputRef = ref(null)
const isDragging = ref(false)
const isUploading = ref(false)
const selectedFile = ref(null)
const forceTemplate = ref('')

// ─── Session state ────────────────────────────────────────────
const activeSessionId = ref(null)
const sessionStatus = ref(null)
const parsedQuestions = ref([])
const report = ref(null)
const meta = ref(null)
const fileName = ref('')

// ─── Editable questions (with local answer edits) ─────────────
const editableQuestions = reactive({})

// ─── UI state ─────────────────────────────────────────────────
const expandedIds = ref(new Set([0, 1, 2]))
const allExpanded = ref(false)
const filterType = ref('')
const filterConfidence = ref('')
const parsingProgress = ref(0)
const parsingStatus = ref('Đang khởi tạo...')
const pythonServiceStatus = ref(null)
const availableExams = ref([])
const selectedExamId = ref('')
const isConfirming = ref(false)
const confirmError = ref('')

// ─── Computed ────────────────────────────────────────────────
const answeredCount = computed(() => {
  return parsedQuestions.value.filter(q => q.answer).length
})

const effectiveQuestions = computed(() => {
  return parsedQuestions.value.map((q, idx) => ({
    ...q,
    answer: editableQuestions[idx] ?? q.answer
  }))
})

const filteredQuestions = computed(() => {
  let qs = effectiveQuestions.value
  if (filterType.value) {
    qs = qs.filter(q => q.type === filterType.value)
  }
  if (filterConfidence.value === 'high') {
    qs = qs.filter(q => (q.confidence || 0) >= 0.8)
  } else if (filterConfidence.value === 'low') {
    qs = qs.filter(q => (q.confidence || 0) < 0.7)
  }
  return qs
})

// ─── Lifecycle ──────────────────────────────────────────────
onMounted(async () => {
  // Check Python parser health
  try {
    const health = await checkPythonParserHealth()
    pythonServiceStatus.value = health?.pythonParserAvailable ?? false
  } catch {
    pythonServiceStatus.value = false
  }

  // Load available exams
  try {
    const exams = await listExams()
    availableExams.value = (exams || []).map(e => ({
      id: e.id,
      title: e.title || `Đề #${e.id}`
    }))
  } catch {
    availableExams.value = []
  })
}
// ─── File handling ───────────────────────────────────────────
const handleFileChange = (e) => {
  const file = e.target.files?.[0]
  if (!file) return
  if (!file.name.toLowerCase().endsWith('.pdf') && !file.name.toLowerCase().endsWith('.docx')) {
    toast.error('Chỉ chấp nhận file PDF hoặc DOCX')
    return
  }
  startUpload(file)
}

const handleDrop = (e) => {
  isDragging.value = false
  const file = e.dataTransfer.files?.[0]
  if (!file) return
  if (!file.name.toLowerCase().endsWith('.pdf') && !file.name.toLowerCase().endsWith('.docx')) {
    toast.error('Chỉ chấp nhận file PDF hoặc DOCX')
    return
  }
  startUpload(file)
}

async function startUpload(file) {
  if (file.size > 20 * 1024 * 1024) {
    toast.error('File vượt quá 20 MB')
    return
  }
  selectedFile.value = file
  fileName.value = file.name
  isUploading.value = true
  parsingProgress.value = 10
  parsingStatus.value = 'Đang tải lên...'

  try {
    const res = await uploadExamPdf(file, {
      forceTemplate: forceTemplate.value || undefined
    })
    activeSessionId.value = res.sessionId
    sessionStatus.value = res.status || 'UPLOADED'
    toast.info('File đã upload, đang parse...')
    await pollSession()
  } catch (err) {
    toast.error(err?.message || 'Upload thất bại')
    activeSessionId.value = null
    sessionStatus.value = null
  } finally {
    isUploading.value = false
  }
}

async function pollSession() {
  parsingProgress.value = 30
  parsingStatus.value = 'Đang parse với Python...'
  try {
    const preview = await waitForExamImportSession(activeSessionId.value, {
      intervalMs: 1500,
      maxAttempts: 120
    })
    parsingProgress.value = 90
    parsingStatus.value = 'Đang tải kết quả...'
    await loadPreview()
    parsingProgress.value = 100
    parsingStatus.value = 'Hoàn thành!'
    if (preview?.status === 'FAILED') {
      toast.error('Parse thất bại. Vui lòng thử file khác.')
    }
  } catch (err) {
    toast.error('Parse quá thời gian: ' + (err?.message || ''))
    sessionStatus.value = 'FAILED'
  }
}

async function loadPreview() {
  try {
    const preview = await getExamImportPreview(activeSessionId.value)
    sessionStatus.value = preview?.status || 'DONE'
    parsedQuestions.value = preview?.questions || []
    report.value = preview?.report || null
    meta.value = preview?.meta || null

    // Sync editable answers
    for (let i = 0; i < parsedQuestions.value.length; i++) {
      editableQuestions[i] = parsedQuestions.value[i].answer
    }

    // Expand first few low-confidence questions
    const lowConf = parsedQuestions.value
      .map((q, i) => ({ i, c: q.confidence || 0 }))
      .filter(x => x.c < 0.7)
      .slice(0, 3)
      .map(x => x.i)
    if (lowConf.length > 0) {
      lowConf.forEach(i => expandedIds.value.add(i))
    }
  } catch (err) {
    toast.error('Không thể tải preview: ' + (err?.message || ''))
  }
}

async function refreshPreview() {
  await loadPreview()
  toast.success('Đã làm mới preview')
}

function resetSession() {
  activeSessionId.value = null
  sessionStatus.value = null
  parsedQuestions.value = []
  report.value = null
  meta.value = null
  Object.keys(editableQuestions).forEach(k => delete editableQuestions[k])
  selectedFile.value = null
  fileName.value = ''
  expandedIds.value = new Set([0, 1, 2])
  confirmError.value = ''
  if (fileInputRef.value) fileInputRef.value.value = ''
}

// ─── Question interactions ────────────────────────────────────
function toggleExpand(idx) {
  const next = new Set(expandedIds.value)
  if (next.has(idx)) {
    next.delete(idx)
  } else {
    next.add(idx)
  }
  expandedIds.value = next
}

function toggleAllExpanded() {
  if (allExpanded.value) {
    expandedIds.value = new Set()
    allExpanded.value = false
  } else {
    expandedIds.value = new Set(filteredQuestions.value.map((_, i) => i))
    allExpanded.value = true
  }
}

function scrollToQuestion(num) {
  const idx = parsedQuestions.value.findIndex(q => q.number === num || q.number === num)
  if (idx >= 0) {
    expandedIds.value.add(idx)
    const el = document.querySelector(`[data-qidx="${idx}"]`)
    el?.scrollIntoView({ behavior: 'smooth', block: 'center' })
  }
}

function setAnswer(idx, opt) {
  editableQuestions[idx] = opt
  // Also update the source
  if (parsedQuestions.value[idx]) {
    parsedQuestions.value[idx].answer = opt
  }
}

function typeSlug(type) {
  const m = { multiple_choice: 'mcq', essay: 'essay', ESSAY: 'essay' }
  return m[String(type || '').toLowerCase()] || 'mcq'
}

function typeLabel(type) {
  const m = {
    multiple_choice: 'Trắc nghiệm',
    essay: 'Tự luận',
    ESSAY: 'Tự luận'
  }
  return m[String(type || '').toLowerCase()] || 'Trắc nghiệm'
}

// ─── Confirm import ──────────────────────────────────────────
async function handleConfirm() {
  if (!activeSessionId.value) return
  isConfirming.value = true
  confirmError.value = ''
  try {
    const result = await confirmExamImport(activeSessionId.value, {
      examId: selectedExamId.value || undefined
    })
    toast.success(`Đã import ${result?.importedCount ?? 0} câu hỏi!`)
    if (result?.importedCount > 0) {
      await router.push(`/teacher/exams/detail/${selectedExamId.value || result.examId || ''}`)
    }
    resetSession()
  } catch (err) {
    confirmError.value = err?.message || 'Import thất bại'
    toast.error(confirmError.value)
  } finally {
    isConfirming.value = false
  }
}

// Watch expanded count to sync with allExpanded toggle
watch(expandedIds, (set) => {
  allExpanded.value = set.size >= filteredQuestions.value.length
}, { deep: true })
</script>

<style scoped>
/* ── Container ── */
.eip {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  background: var(--ds-bg);
}

/* ── Header ── */
.eip__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  padding: 1rem 1.5rem;
  background: var(--ds-surface);
  border-bottom: 1px solid var(--ds-border);
  flex-shrink: 0;
}

.dark .eip__header {
  border-bottom-color: var(--ds-border-strong);
}

.eip__header-left {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.eip__header-icon {
  width: 40px;
  height: 40px;
  border-radius: var(--ds-radius-xl);
  background: linear-gradient(135deg, var(--ds-primary) 0%, #6366f1 100%);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.eip__header-title {
  font-family: var(--ds-font-display);
  font-size: 1.125rem;
  font-weight: 800;
  color: var(--ds-text);
  margin: 0;
}

.dark .eip__header-title { color: #f1f5f9; }

.eip__header-sub {
  font-size: 0.75rem;
  color: var(--ds-text-muted);
  margin: 0.2rem 0 0;
}

.eip__header-right {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.eip__service-chip {
  display: inline-flex;
  align-items: center;
  gap: 0.3rem;
  padding: 0.3rem 0.75rem;
  border-radius: var(--ds-radius-full);
  font-size: 0.72rem;
  font-weight: 700;
  border: 1px solid transparent;
}

.eip__service-chip--ok {
  background: var(--ds-success-soft);
  color: var(--ds-success);
  border-color: rgba(22, 163, 74, 0.2);
}

.eip__service-chip--warn {
  background: var(--ds-warning-soft);
  color: var(--ds-warning);
  border-color: rgba(245, 158, 11, 0.2);
}

.eip__template-select {
  padding: 0.4rem 0.75rem;
  border: 1.5px solid var(--ds-border);
  border-radius: var(--ds-radius-lg);
  background: var(--ds-surface);
  color: var(--ds-text);
  font-size: 0.8rem;
  font-weight: 600;
  cursor: pointer;
  outline: none;
}

.dark .eip__template-select {
  background: var(--ds-gray-800);
  border-color: var(--ds-border-strong);
  color: #f1f5f9;
}

.eip__template-select:focus { border-color: var(--ds-primary); }

/* ── Body ── */
.eip__body {
  display: grid;
  grid-template-columns: 1fr 340px;
  gap: 1.5rem;
  padding: 1.5rem 2rem;
  max-width: 1400px;
  width: 100%;
  margin: 0 auto;
  align-items: start;
}

@media (max-width: 1024px) {
  .eip__body { grid-template-columns: 1fr; }
  .eip__side-col { order: -1; }
}

.eip__main-col {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.eip__side-col {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  position: sticky;
  top: 1.5rem;
}

/* ── Upload zone ── */
.eip__upload-zone {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 0.75rem;
  padding: 3rem 2rem;
  border: 2px dashed var(--ds-border);
  border-radius: var(--ds-radius-2xl);
  background: var(--ds-surface);
  cursor: pointer;
  transition: border-color 0.2s ease, background 0.2s ease;
  text-align: center;
}

.dark .eip__upload-zone {
  border-color: var(--ds-border-strong);
}

.eip__upload-zone:hover,
.eip__upload-zone--dragover {
  border-color: var(--ds-primary);
  background: var(--ds-primary-soft);
}

.eip__file-input {
  display: none;
}

.eip__upload-icon {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background: var(--ds-primary-soft);
  color: var(--ds-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 0.25rem;
}

.eip__upload-title {
  font-size: 1rem;
  font-weight: 700;
  color: var(--ds-text);
  margin: 0;
}

.dark .eip__upload-title { color: #f1f5f9; }

.eip__upload-sub {
  font-size: 0.8rem;
  color: var(--ds-text-muted);
  margin: 0;
}

.eip__upload-sub--small {
  font-size: 0.72rem;
  opacity: 0.7;
}

.eip__upload-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.6rem 1.25rem;
  border-radius: var(--ds-radius-xl);
  background: linear-gradient(135deg, var(--ds-primary) 0%, #6366f1 100%);
  color: white;
  font-size: 0.85rem;
  font-weight: 700;
  border: none;
  cursor: pointer;
  box-shadow: 0 4px 12px rgba(79, 70, 229, 0.3);
  transition: all 0.15s ease;
  margin-top: 0.5rem;
}

.eip__upload-btn:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 6px 20px rgba(79, 70, 229, 0.4);
}

.eip__upload-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* ── Parsing ── */
.eip__parsing {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 4rem 2rem;
}

.eip__parsing-inner {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.75rem;
  text-align: center;
  max-width: 320px;
}

.eip__parsing-spinner {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background: var(--ds-primary-soft);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--ds-primary);
}

.eip__spin-icon {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.eip__parsing-title {
  font-size: 1rem;
  font-weight: 700;
  color: var(--ds-text);
  margin: 0;
}

.dark .eip__parsing-title { color: #f1f5f9; }

.eip__parsing-sub {
  font-size: 0.78rem;
  color: var(--ds-text-muted);
  margin: 0;
}

.eip__progress-bar {
  width: 100%;
  height: 6px;
  background: var(--ds-gray-200);
  border-radius: 9999px;
  overflow: hidden;
}

.dark .eip__progress-bar { background: var(--ds-gray-700); }

.eip__progress-fill {
  height: 100%;
  background: linear-gradient(90deg, var(--ds-primary) 0%, #6366f1 100%);
  border-radius: 9999px;
  transition: width 0.5s ease;
}

.eip__parsing-status {
  font-size: 0.75rem;
  color: var(--ds-text-muted);
  font-variant-numeric: tabular-nums;
  margin: 0;
}

/* ── Session bar ── */
.eip__session-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  padding: 0.75rem 1rem;
  background: var(--ds-success-soft);
  border: 1px solid rgba(22, 163, 74, 0.2);
  border-radius: var(--ds-radius-xl);
}

.eip__session-info {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.eip__session-icon { color: var(--ds-success); }

.eip__session-label {
  font-size: 0.82rem;
  font-weight: 700;
  color: var(--ds-success);
}

.eip__session-file {
  font-size: 0.75rem;
  color: var(--ds-text-muted);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 200px;
}

.eip__session-actions {
  display: flex;
  gap: 0.5rem;
}

.eip__action-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.3rem;
  padding: 0.375rem 0.75rem;
  border-radius: var(--ds-radius-lg);
  border: 1px solid var(--ds-border);
  background: var(--ds-surface);
  color: var(--ds-text-secondary);
  font-size: 0.75rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.12s ease;
}

.dark .eip__action-btn {
  background: var(--ds-gray-700);
  border-color: var(--ds-border-strong);
  color: #94a3b8;
}

.eip__action-btn:hover {
  border-color: var(--ds-primary);
  color: var(--ds-primary);
}

.eip__action-btn--outline { }

/* ── Questions ── */
.eip__questions {
  display: flex;
  flex-direction: column;
  border: 1.5px solid var(--ds-border);
  border-radius: var(--ds-radius-2xl);
  background: var(--ds-surface);
  overflow: hidden;
}

.dark .eip__questions {
  border-color: var(--ds-border-strong);
}

.eip__qlist-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  padding: 0.875rem 1rem;
  border-bottom: 1px solid var(--ds-border);
  flex-wrap: wrap;
}

.dark .eip__qlist-header {
  border-bottom-color: var(--ds-border-strong);
}

.eip__qlist-title {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.85rem;
  font-weight: 700;
  color: var(--ds-text);
}

.dark .eip__qlist-title { color: #f1f5f9; }

.eip__qlist-filters {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.eip__filter-select {
  padding: 0.3rem 0.6rem;
  border: 1.5px solid var(--ds-border);
  border-radius: var(--ds-radius-md);
  background: var(--ds-gray-50);
  color: var(--ds-text);
  font-size: 0.75rem;
  font-weight: 600;
  cursor: pointer;
  outline: none;
}

.dark .eip__filter-select {
  background: var(--ds-gray-800);
  border-color: var(--ds-border-strong);
  color: #f1f5f9;
}

.eip__filter-select:focus { border-color: var(--ds-primary); }

.eip__filter-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.3rem;
  padding: 0.3rem 0.625rem;
  border: 1.5px solid var(--ds-border);
  border-radius: var(--ds-radius-md);
  background: var(--ds-surface);
  color: var(--ds-text-muted);
  font-size: 0.72rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.12s ease;
}

.dark .eip__filter-btn {
  background: var(--ds-gray-800);
  border-color: var(--ds-border-strong);
  color: #94a3b8;
}

.eip__filter-btn:hover {
  border-color: var(--ds-primary);
  color: var(--ds-primary);
}

.eip__qlist {
  max-height: 70vh;
  overflow-y: auto;
  overscroll-contain;
  padding: 0.75rem;
  display: flex;
  flex-direction: column;
  gap: 0.625rem;
}

.eip__qcard {
  border: 1.5px solid var(--ds-border);
  border-radius: var(--ds-radius-xl);
  background: var(--ds-surface);
  overflow: hidden;
  transition: border-color 0.15s ease, box-shadow 0.15s ease;
}

.dark .eip__qcard {
  border-color: var(--ds-border-strong);
}

.eip__qcard--expanded {
  border-color: var(--ds-primary-border);
  box-shadow: 0 0 0 3px var(--ds-primary-ring);
}

.eip__qcard--low-conf {
  border-left: 3px solid var(--ds-warning);
}

.eip__qcard--essay {
  border-left: 3px solid var(--ds-warning);
}

.eip__qcard-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  padding: 0.75rem 1rem;
  width: 100%;
  background: transparent;
  border: none;
  cursor: pointer;
  text-align: left;
  transition: background 0.1s ease;
}

.eip__qcard-header:hover { background: var(--ds-gray-50); }

.dark .eip__qcard-header:hover { background: var(--ds-gray-800); }

.eip__qcard-left {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  min-width: 0;
  flex: 1;
}

.eip__qcard-right {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex-shrink: 0;
}

.eip__qnum {
  font-size: 0.72rem;
  font-weight: 800;
  color: var(--ds-primary);
  background: var(--ds-primary-soft);
  padding: 0.2rem 0.5rem;
  border-radius: var(--ds-radius-md);
  flex-shrink: 0;
}

.eip__type-badge {
  padding: 0.2rem 0.5rem;
  border-radius: var(--ds-radius-full);
  font-size: 0.68rem;
  font-weight: 700;
  flex-shrink: 0;
}

.eip__type-badge--mcq { background: var(--ds-primary-soft); color: var(--ds-primary); }
.eip__type-badge--essay { background: rgba(245,158,11,0.1); color: #d97706; }

.eip__conf-warn {
  display: flex;
  align-items: center;
  gap: 0.2rem;
  font-size: 0.65rem;
  font-weight: 600;
  color: var(--ds-warning);
  background: rgba(245,158,11,0.08);
  padding: 0.15rem 0.4rem;
  border-radius: var(--ds-radius-full);
}

.eip__img-badge {
  display: flex;
  align-items: center;
  gap: 0.2rem;
  font-size: 0.65rem;
  font-weight: 600;
  color: var(--ds-info);
  background: var(--ds-info-soft);
  padding: 0.15rem 0.4rem;
  border-radius: var(--ds-radius-full);
}

.eip__answer-preview {
  display: flex;
  align-items: center;
  gap: 0.2rem;
  font-size: 0.72rem;
  font-weight: 700;
  color: var(--ds-success);
  background: var(--ds-success-soft);
  padding: 0.2rem 0.5rem;
  border-radius: var(--ds-radius-full);
}

.eip__expand-icon {
  color: var(--ds-text-muted);
}

.eip__qcard-body {
  padding: 0 1rem 1rem;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  border-top: 1px solid var(--ds-border);
  padding-top: 0.875rem;
}

.dark .eip__qcard-body { border-top-color: var(--ds-border-strong); }

.eip__qcard-edit {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.eip__edit-label {
  font-size: 0.72rem;
  font-weight: 600;
  color: var(--ds-text-secondary);
  white-space: nowrap;
}

.eip__correct-group {
  display: flex;
  gap: 0.25rem;
}

.eip__correct-btn {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  border: 1.5px solid var(--ds-border);
  background: var(--ds-surface);
  color: var(--ds-text-muted);
  font-size: 0.72rem;
  font-weight: 800;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.12s ease;
}

.dark .eip__correct-btn {
  background: var(--ds-gray-800);
  border-color: var(--ds-border-strong);
}

.eip__correct-btn--active {
  background: var(--ds-success);
  border-color: var(--ds-success);
  color: white;
}

.eip__correct-btn:hover:not(.eip__correct-btn--active) {
  border-color: var(--ds-success);
  color: var(--ds-success);
}

/* No questions */
.eip__no-questions {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.5rem;
  padding: 4rem 2rem;
  text-align: center;
  color: var(--ds-text-muted);
  font-size: 0.875rem;
}

.eip__no-questions-sub {
  font-size: 0.78rem;
  opacity: 0.7;
}

/* ── Exam select card ── */
.eip__exam-select-card {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  padding: 1rem;
  border: 1.5px solid var(--ds-border);
  border-radius: var(--ds-radius-2xl);
  background: var(--ds-surface);
}

.dark .eip__exam-select-card {
  border-color: var(--ds-border-strong);
}

.eip__exam-select-label {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  font-size: 0.72rem;
  font-weight: 700;
  color: var(--ds-text-secondary);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.eip__exam-select {
  padding: 0.5rem 0.75rem;
  border: 1.5px solid var(--ds-border);
  border-radius: var(--ds-radius-lg);
  background: var(--ds-gray-50);
  color: var(--ds-text);
  font-size: 0.82rem;
  font-weight: 600;
  cursor: pointer;
  outline: none;
}

.dark .eip__exam-select {
  background: var(--ds-gray-800);
  border-color: var(--ds-border-strong);
  color: #f1f5f9;
}

.eip__exam-select:focus { border-color: var(--ds-primary); }

.eip__exam-hint {
  font-size: 0.7rem;
  color: var(--ds-text-muted);
  font-style: italic;
  margin: 0;
}

/* ── Confirm card ── */
.eip__confirm-card {
  display: flex;
  flex-direction: column;
  gap: 0.625rem;
  padding: 1rem;
  border: 1.5px solid var(--ds-border);
  border-radius: var(--ds-radius-2xl);
  background: var(--ds-surface);
}

.dark .eip__confirm-card {
  border-color: var(--ds-border-strong);
}

.eip__confirm-summary {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.375rem;
  font-size: 0.78rem;
  color: var(--ds-text-secondary);
  font-weight: 600;
}

.eip__confirm-dot { opacity: 0.5; }

.eip__confirm-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  padding: 0.7rem 1.25rem;
  border-radius: var(--ds-radius-xl);
  background: linear-gradient(135deg, var(--ds-success) 0%, #059669 100%);
  color: white;
  font-size: 0.875rem;
  font-weight: 700;
  border: none;
  cursor: pointer;
  box-shadow: 0 4px 12px rgba(22, 163, 74, 0.3);
  transition: all 0.15s ease;
}

.eip__confirm-btn:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 6px 20px rgba(22, 163, 74, 0.4);
}

.eip__confirm-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}

.eip__confirm-error {
  font-size: 0.72rem;
  color: var(--ds-danger);
  text-align: center;
  margin: 0;
}

@media (prefers-reduced-motion: reduce) {
  .eip__spin-icon,
  .eip__progress-fill {
    transition: none;
    animation: none;
  }
  .eip__confirm-btn:hover:not(:disabled),
  .eip__upload-btn:hover:not(:disabled) {
    transform: none;
  }
}
</style>
