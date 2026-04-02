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

      <!-- AI Question Generation -->
      <div class="ec-field">
        <label class="ec-field__label">Tạo câu hỏi bằng AI</label>
        <div class="ec-ai-section">
          <button
            type="button"
            class="ec-btn ec-btn--ai"
            @click="showAiModal = true"
          >
            <LucideIcon name="auto_awesome" />
            <span>Tạo câu hỏi với AI</span>
          </button>
          <p class="ec-ai-hint">Tạo câu hỏi tự động từ chủ đề hoặc nội dung có sẵn</p>
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
          <div class="ecc__icon">
            <LucideIcon name="vpn_key" />
          </div>
          <div class="ecc__body">
            <p class="ecc__label">Số mã đề</p>
            <p class="ecc__code">
              {{ versionCount }} mã đề
              <button type="button" class="ecc__edit-btn" @click="showVersionModal = true" title="Chỉnh sửa">
                <LucideIcon name="edit" />
              </button>
            </p>
            <p class="ecc__hint">
              <LucideIcon name="info" size="13" />
              Mỗi mã đề sẽ có thứ tự câu hỏi riêng
            </p>
          </div>
        </div>
      </Transition>

    </div>

    <!-- AI Generation Modal -->
    <Teleport to="body">
      <Transition name="ec-modal">
        <div v-if="showAiModal" class="ec-modal-overlay" @click.self="showAiModal = false">
          <div class="ec-modal ec-modal--md">
            <div class="ec-modal__header">
              <div class="flex items-center gap-3">
                <div class="ec-modal__ai-icon">
                  <LucideIcon name="auto_awesome" />
                </div>
                <h2 class="ec-modal__title">Tạo câu hỏi bằng AI</h2>
              </div>
              <button type="button" class="ec-modal__close" @click="showAiModal = false">
                <LucideIcon name="close" />
              </button>
            </div>
            <div class="ec-modal__body">
              <!-- Generation mode -->
              <div class="ec-field">
                <label class="ec-field__label">Chế độ tạo</label>
                <div class="ec-ai-modes">
                  <button
                    type="button"
                    class="ec-ai-mode-btn"
                    :class="{ 'ec-ai-mode-btn--active': aiMode === 'topic' }"
                    @click="aiMode = 'topic'"
                  >
                    <LucideIcon name="category" />
                    <span>Theo chủ đề</span>
                  </button>
                  <button
                    type="button"
                    class="ec-ai-mode-btn"
                    :class="{ 'ec-ai-mode-btn--active': aiMode === 'text' }"
                    @click="aiMode = 'text'"
                  >
                    <LucideIcon name="text_snippet" />
                    <span>Từ nội dung</span>
                  </button>
                </div>
              </div>

              <!-- Topic input -->
              <div v-if="aiMode === 'topic'" class="ec-field">
                <label class="ec-field__label">Chủ đề câu hỏi</label>
                <input
                  v-model="aiTopic"
                  type="text"
                  class="ec-input"
                  placeholder="Ví dụ: Phương trình bậc hai, Lịch sử Việt Nam thế kỷ 20..."
                />
                <p class="ec-field__hint">Mô tả chủ đề hoặc từ khóa để AI tạo câu hỏi phù hợp</p>
              </div>

              <!-- Text input -->
              <div v-else class="ec-field">
                <label class="ec-field__label">Nội dung nguồn</label>
                <textarea
                  v-model="aiText"
                  class="ec-input ec-input--textarea"
                  placeholder="Dán nội dung bài giảng, tài liệu hoặc đề cương vào đây..."
                  rows="6"
                />
                <p class="ec-field__hint">AI sẽ phân tích nội dung và tạo câu hỏi tự động</p>
              </div>

              <!-- Question count -->
              <div class="ec-field">
                <label class="ec-field__label">Số lượng câu hỏi</label>
                <div class="ec-ai-count-grid">
                  <button
                    v-for="n in [3, 5, 10, 15]"
                    :key="n"
                    type="button"
                    class="ec-ai-count-btn"
                    :class="{ 'ec-ai-count-btn--active': aiCount === n }"
                    @click="aiCount = n"
                  >
                    {{ n }} câu
                  </button>
                </div>
              </div>

              <!-- Difficulty -->
              <div class="ec-field">
                <label class="ec-field__label">Mức độ khó</label>
                <div class="ec-ai-difficulty-grid">
                  <button
                    v-for="d in difficultyOptions"
                    :key="d.value"
                    type="button"
                    class="ec-ai-diff-btn"
                    :class="{ 'ec-ai-diff-btn--active': aiDifficulty === d.value, [`ec-ai-diff-btn--${d.value.toLowerCase()}`]: aiDifficulty === d.value }"
                    @click="aiDifficulty = d.value"
                  >
                    <LucideIcon :name="d.icon" />
                    <span>{{ d.label }}</span>
                  </button>
                </div>
              </div>

              <!-- Generated preview -->
              <div v-if="aiGeneratedQuestions.length > 0" class="ec-ai-preview">
                <div class="ec-ai-preview__header">
                  <LucideIcon name="check_circle" />
                  <span>{{ aiGeneratedQuestions.length }} câu hỏi đã tạo</span>
                </div>
                <div class="ec-ai-preview__list">
                  <div
                    v-for="(q, i) in aiGeneratedQuestions"
                    :key="i"
                    class="ec-ai-preview__item"
                  >
                    <span class="ec-ai-preview__num">{{ i + 1 }}</span>
                    <span class="ec-ai-preview__text">{{ q.content || q.question || q }}</span>
                  </div>
                </div>
              </div>

              <!-- Error -->
              <div v-if="aiError" class="ec-qb-error">
                <LucideIcon name="error" />
                {{ aiError }}
              </div>
            </div>
            <div class="ec-modal__footer">
              <button type="button" class="ec-btn ec-btn--outline" @click="showAiModal = false">
                Hủy
              </button>
              <button
                type="button"
                class="ec-btn ec-btn--ai"
                :disabled="aiGenerating || (!aiTopic.trim() && !aiText.trim())"
                @click="handleAiGenerate"
              >
                <LucideIcon name="auto_awesome" v-if="!aiGenerating" />
                <span class="ec-spin" v-if="aiGenerating">
                  <LucideIcon name="progress_activity" />
                </span>
                {{ aiGenerating ? 'Đang tạo...' : (aiGeneratedQuestions.length > 0 ? 'Tạo lại' : 'Tạo câu hỏi') }}
              </button>
              <button
                v-if="aiGeneratedQuestions.length > 0"
                type="button"
                class="ec-btn ec-btn--primary"
                @click="handleAddAiQuestions"
              >
                <LucideIcon name="add" />
                Thêm {{ aiGeneratedQuestions.length }} câu hỏi
              </button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

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
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { API_BASE_URL } from '../../../services/apiClient'
import { generateQuestionsFromTopic, generateQuestionsFromText, getGeneratorStatus } from '../../../services/aiService'
import { useToast } from '../../../composables/useToast'

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

// AI Generation state
const toast = useToast()
const showAiModal = ref(false)
const aiMode = ref('topic') // 'topic' | 'text'
const aiTopic = ref('')
const aiText = ref('')
const aiCount = ref(5)
const aiDifficulty = ref('MEDIUM')
const aiGenerating = ref(false)
const aiGeneratedQuestions = ref([])
const aiError = ref('')
const aiServiceAvailable = ref(true)

const difficultyOptions = [
  { value: 'EASY', label: 'Dễ', icon: 'sentiment_satisfied' },
  { value: 'MEDIUM', label: 'Trung bình', icon: 'sentiment_neutral' },
  { value: 'HARD', label: 'Khó', icon: 'sentiment_dissatisfied' }
]

const handleAiGenerate = async () => {
  aiError.value = ''
  if (!aiTopic.value.trim() && !aiText.value.trim()) {
    aiError.value = 'Vui lòng nhập chủ đề hoặc nội dung.'
    return
  }
  aiGenerating.value = true
  try {
    const count = aiCount.value
    const difficulty = aiDifficulty.value
    let result
    if (aiMode.value === 'topic') {
      result = await generateQuestionsFromTopic(aiTopic.value.trim(), count, difficulty, 'vi')
    } else {
      result = await generateQuestionsFromText(aiText.value.trim(), count, difficulty, 'vi')
    }
    if (!result || !result.questions) {
      // Try alternative response format
      if (Array.isArray(result)) {
        aiGeneratedQuestions.value = result
      } else {
        aiGeneratedQuestions.value = []
        aiError.value = 'Không có câu hỏi nào được tạo. Vui lòng thử lại.'
      }
    } else {
      aiGeneratedQuestions.value = result.questions.map(q => ({
        _localId: localIdCounter++,
        content: q.content || q.question || q.text || JSON.stringify(q),
        type: q.type || 'SINGLE_CHOICE',
        correctAnswer: q.correctAnswer || '',
        options: q.options || [],
        score: q.scoreWeight || 1,
        difficulty: q.difficulty || difficulty
      }))
    }
    if (aiGeneratedQuestions.value.length > 0) {
      toast.success(`Đã tạo ${aiGeneratedQuestions.value.length} câu hỏi!`)
    }
  } catch (err) {
    aiError.value = 'Không thể tạo câu hỏi. Dịch vụ AI có thể đang bận. Vui lòng thử lại sau.'
    aiServiceAvailable.value = false
  } finally {
    aiGenerating.value = false
  }
}

const handleAddAiQuestions = () => {
  if (aiGeneratedQuestions.value.length === 0) return
  const updated = [...localQuestions.value, ...aiGeneratedQuestions.value]
  localQuestions.value = updated
  toast.success(`Đã thêm ${aiGeneratedQuestions.value.length} câu hỏi!`)
  aiGeneratedQuestions.value = []
  showAiModal.value = false
  aiTopic.value = ''
  aiText.value = ''
}

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
    // Simulate import success - in real flow would call API
    await new Promise(resolve => setTimeout(resolve, 1000))
    // Add mock questions for demo
    const mockQuestions = [
      { _localId: localIdCounter++, content: 'Câu hỏi mẫu 1? Đây là nội dung câu hỏi đầu tiên được nhập từ file.', correctAnswer: 'A', score: 1 },
      { _localId: localIdCounter++, content: 'Câu hỏi mẫu 2? Đây là nội dung câu hỏi thứ hai.', correctAnswer: 'B', score: 1 },
      { _localId: localIdCounter++, content: 'Câu hỏi mẫu 3? Đây là nội dung câu hỏi thứ ba.', correctAnswer: 'C', score: 1 }
    ]
    localQuestions.value = [...localQuestions.value, ...mockQuestions]
    selectedFile.value = null
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
  transition: all 0.15s ease;
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
  transition: all 0.15s ease;
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
  display: flex;
  align-items: flex-start;
  gap: 0.875rem;
  padding: 1rem 1.125rem;
  border-radius: var(--ds-radius-xl);
  border: 1.5px dashed var(--ds-info);
  background: var(--ds-info-bg);
}

.dark .exam-code-card { background: rgba(2, 132, 199, 0.1); border-color: rgba(2, 132, 199, 0.3); }

.ecc__icon {
  width: 36px;
  height: 36px;
  border-radius: var(--ds-radius-md);
  background: rgba(2, 132, 199, 0.15);
  color: var(--ds-info);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.dark .ecc__icon { color: var(--ds-info); }

.ecc__body { flex: 1; min-width: 0; }

.ecc__label {
  font-size: 0.7rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.1em;
  color: var(--ds-text-muted);
  margin: 0 0 0.2rem;
}

.ecc__code {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-family: 'Courier New', monospace;
  font-size: 1.1rem;
  font-weight: 900;
  color: var(--ds-info);
  margin: 0 0 0.3rem;
}

.dark .ecc__code { color: var(--ds-info); }

.ecc__edit-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border: none;
  border-radius: var(--ds-radius-md);
  background: rgba(2, 132, 199, 0.15);
  color: var(--ds-info);
  cursor: pointer;
  transition: all 0.15s ease;
}

.ecc__edit-btn:hover { background: rgba(2, 132, 199, 0.25); }

.ecc__hint {
  display: flex;
  align-items: center;
  gap: 0.3rem;
  font-size: 0.72rem;
  color: var(--ds-text-muted);
  margin: 0;
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
  transition: all 0.15s ease;
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
  backdrop-filter: blur(4px);
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
  transition: all 0.15s ease;
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
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
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
.ec-slide-enter-active, .ec-slide-leave-active { transition: all 0.28s cubic-bezier(0.4, 0, 0.2, 1); overflow: hidden; }
.ec-slide-enter-from, .ec-slide-leave-to { opacity: 0; max-height: 0; }
.ec-slide-enter-to, .ec-slide-leave-from { opacity: 1; max-height: 200px; }

.ec-modal-enter-active, .ec-modal-leave-active { transition: all 0.2s ease; }
.ec-modal-enter-from, .ec-modal-leave-to { opacity: 0; }
.ec-modal-enter-from .ec-modal, .ec-modal-leave-to .ec-modal { transform: scale(0.95) translateY(10px); }

.ec-spin { animation: spin 1s linear infinite; }
@keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }

.hidden { display: none; }

/* AI Generation */
.ec-ai-section { display: flex; flex-direction: column; gap: 0.5rem; }
.ec-ai-hint { font-size: 0.75rem; color: var(--ds-text-muted); margin: 0; }

.ec-btn--ai {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.625rem 1.25rem;
  border-radius: var(--ds-radius-xl);
  border: 1.5px solid;
  font-size: 0.875rem;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s;
  background: linear-gradient(135deg, rgba(99,102,241,0.08), rgba(139,92,246,0.08));
  border-color: rgba(99,102,241,0.3);
  color: #6366f1;
}
.ec-btn--ai:hover:not(:disabled) {
  background: linear-gradient(135deg, rgba(99,102,241,0.15), rgba(139,92,246,0.15));
  border-color: #6366f1;
  transform: translateY(-1px);
  box-shadow: 0 4px 16px rgba(99,102,241,0.2);
}
.ec-btn--ai:disabled { opacity: 0.5; cursor: not-allowed; }

.ec-modal__ai-icon {
  width: 2.5rem;
  height: 2.5rem;
  border-radius: var(--ds-radius-xl);
  background: linear-gradient(135deg, rgba(99,102,241,0.12), rgba(139,92,246,0.12));
  color: #6366f1;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.ec-ai-modes {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
}
.ec-ai-mode-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.375rem;
  padding: 0.5rem 1rem;
  border-radius: var(--ds-radius-xl);
  border: 1.5px solid var(--ds-border);
  background: var(--ds-surface);
  color: var(--ds-text-secondary);
  font-size: 0.8rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s;
}
.ec-ai-mode-btn:hover { border-color: var(--ds-primary-border); color: var(--ds-primary); }
.ec-ai-mode-btn--active { background: var(--ds-primary-soft); border-color: var(--ds-primary); color: var(--ds-primary); }

.ec-ai-count-grid {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
}
.ec-ai-count-btn {
  padding: 0.375rem 0.875rem;
  border-radius: var(--ds-radius-xl);
  border: 1.5px solid var(--ds-border);
  background: var(--ds-surface);
  color: var(--ds-text-secondary);
  font-size: 0.8rem;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.15s;
}
.ec-ai-count-btn:hover { border-color: var(--ds-primary-border); color: var(--ds-primary); }
.ec-ai-count-btn--active { background: var(--ds-primary); border-color: var(--ds-primary); color: white; }

.ec-ai-difficulty-grid {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
}
.ec-ai-diff-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.375rem;
  padding: 0.5rem 1rem;
  border-radius: var(--ds-radius-xl);
  border: 1.5px solid var(--ds-border);
  background: var(--ds-surface);
  color: var(--ds-text-secondary);
  font-size: 0.8rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s;
}
.ec-ai-diff-btn:hover { border-color: var(--ds-border-strong); }
.ec-ai-diff-btn--easy.ec-ai-diff-btn--active { background: var(--ds-success-bg); border-color: var(--ds-success); color: var(--ds-success); }
.ec-ai-diff-btn--medium.ec-ai-diff-btn--active { background: var(--ds-accent-bg); border-color: var(--ds-accent); color: var(--ds-accent); }
.ec-ai-diff-btn--hard.ec-ai-diff-btn--active { background: var(--ds-danger-soft); border-color: var(--ds-danger); color: var(--ds-danger); }

.ec-input--textarea { resize: vertical; min-height: 120px; }

.ec-ai-preview {
  border: 1px solid var(--ds-primary-border);
  border-radius: var(--ds-radius-xl);
  background: var(--ds-primary-soft);
  overflow: hidden;
}
.ec-ai-preview__header {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem 1rem;
  background: rgba(99,102,241,0.12);
  color: #6366f1;
  font-size: 0.8rem;
  font-weight: 700;
  border-bottom: 1px solid rgba(99,102,241,0.15);
}
.ec-ai-preview__list { padding: 0.75rem; display: flex; flex-direction: column; gap: 0.5rem; max-height: 240px; overflow-y: auto; }
.ec-ai-preview__item { display: flex; align-items: flex-start; gap: 0.5rem; }
.ec-ai-preview__num {
  width: 1.5rem;
  height: 1.5rem;
  border-radius: 50%;
  background: #6366f1;
  color: white;
  font-size: 0.65rem;
  font-weight: 800;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.ec-ai-preview__text { font-size: 0.8rem; color: var(--ds-text); line-height: 1.4; }
</style>
