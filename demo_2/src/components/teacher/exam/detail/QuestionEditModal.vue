<template>
  <Teleport to="body">
    <Transition name="qemodal">
      <div v-if="modelValue" class="qemodal-overlay" @click.self="close">
        <div class="qemodal" :class="`qemodal--${size}`">
          <!-- Header -->
          <div class="qemodal__header">
            <div class="qemodal__header-left">
              <div class="qemodal__icon">
                <LucideIcon :name="isEditing ? 'edit' : 'add_circle'" />
              </div>
              <div>
                <h2 class="qemodal__title">{{ isEditing ? 'Sửa câu hỏi' : 'Thêm câu hỏi mới' }}</h2>
                <p class="qemodal__subtitle">{{ examTitle || 'Đề thi chưa có tiêu đề' }}</p>
              </div>
            </div>
            <button type="button" class="qemodal__close" @click="close">
              <LucideIcon name="close" />
            </button>
          </div>

          <!-- Type selector -->
          <div class="qemodal__type-bar">
            <button
              v-for="t in questionTypes"
              :key="t.value"
              type="button"
              class="qemodal__type-btn"
              :class="{ 'qemodal__type-btn--active': localType === t.value }"
              @click="localType = t.value"
            >
              <LucideIcon :name="t.icon" />
              {{ t.label }}
            </button>
          </div>

          <!-- Body -->
          <div class="qemodal__body">
            <!-- Content -->
            <div class="qemodal__field">
              <label class="qemodal__label">
                Nội dung câu hỏi
                <span class="qemodal__required">*</span>
              </label>
              <textarea
                v-model="localContent"
                rows="3"
                class="qemodal__textarea"
                :class="{ 'qemodal__textarea--error': errors.content }"
                placeholder="Nhập nội dung câu hỏi..."
                @input="errors.content = ''"
              />
              <div class="qemodal__field-footer">
                <span v-if="errors.content" class="qemodal__error">{{ errors.content }}</span>
                <span class="qemodal__hint">{{ localContent.length }}/1000</span>
              </div>
            </div>

            <!-- Options (for choice types) -->
            <div v-if="!isEssay" class="qemodal__field">
              <label class="qemodal__label">
                Đáp án
                <span class="qemodal__required">*</span>
              </label>
              <div class="qemodal__options-grid">
                <div v-for="opt in optionKeys" :key="opt" class="qemodal__opt">
                  <button
                    type="button"
                    class="qemodal__opt-badge"
                    :class="{ 'qemodal__opt-badge--correct': isCorrect(opt) }"
                    @click="setCorrect(opt)"
                    :title="isMultiple ? 'Click để toggle đáp án đúng' : 'Click để chọn đáp án đúng'"
                  >
                    <LucideIcon name="check" v-if="isCorrect(opt)" />
                    <span v-else>{{ opt }}</span>
                  </button>
                  <input
                    v-model="localOptions[opt]"
                    type="text"
                    class="qemodal__opt-input"
                    :class="{ 'qemodal__opt-input--correct': isCorrect(opt) }"
                    :placeholder="`Đáp án ${opt}`"
                    @input="errors.options = ''"
                  />
                </div>
              </div>
              <div class="qemodal__field-footer">
                <span v-if="errors.options" class="qemodal__error">{{ errors.options }}</span>
                <span v-else class="qemodal__hint">
                  <span v-if="isMultiple">Click để chọn nhiều đáp án đúng</span>
                  <span v-else>Click vào badge để chọn đáp án đúng</span>
                </span>
              </div>
            </div>

            <!-- Meta row -->
            <div class="qemodal__meta-row">
              <!-- Score -->
              <div class="qemodal__field qemodal__field--sm">
                <label class="qemodal__label">Điểm</label>
                <input
                  v-model.number="localScore"
                  type="number"
                  min="0.5"
                  max="100"
                  step="0.5"
                  class="qemodal__input qemodal__input--center"
                />
              </div>

              <!-- Difficulty -->
              <div class="qemodal__field qemodal__field--sm">
                <label class="qemodal__label">Độ khó</label>
                <div class="qemodal__difficulty-btns">
                  <button
                    v-for="d in difficulties"
                    :key="d.value"
                    type="button"
                    class="qemodal__diff-btn"
                    :class="[`qemodal__diff-btn--${d.color}`, { 'qemodal__diff-btn--active': localDifficulty === d.value }]"
                    @click="localDifficulty = d.value"
                  >
                    {{ d.label }}
                  </button>
                </div>
              </div>
            </div>

            <!-- Validation summary -->
            <div v-if="!isValid && attemptedSubmit" class="qemodal__summary">
              <LucideIcon name="error" />
              <span>Vui lòng điền đầy đủ thông tin trước khi lưu</span>
            </div>
          </div>

          <!-- Footer -->
          <div class="qemodal__footer">
            <button type="button" class="qemodal__btn qemodal__btn--ghost" @click="close">
              Hủy
            </button>
            <button
              type="button"
              class="qemodal__btn qemodal__btn--primary"
              :disabled="!isValid && attemptedSubmit"
              @click="handleSave"
            >
              <LucideIcon :name="isEditing ? 'save' : 'add'" />
              {{ isEditing ? 'Lưu thay đổi' : 'Thêm câu hỏi' }}
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
import { ref, computed, reactive, watch } from 'vue'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  question: { type: Object, default: null },
  examTitle: { type: String, default: '' },
  size: { type: String, default: 'lg' }
})

const emit = defineEmits(['update:modelValue', 'save'])

const isEditing = computed(() => !!props.question?.id || !!props.question?._localId)

const questionTypes = [
  { value: 'SINGLE_CHOICE', label: 'Một đáp án', icon: 'check_box' },
  { value: 'MULTIPLE_CHOICE', label: 'Nhiều đáp án', icon: 'checklist' },
  { value: 'ESSAY', label: 'Tự luận', icon: 'text_snippet' },
  { value: 'TRUE_FALSE', label: 'Đúng/Sai', icon: 'toggle_on' }
]

const difficulties = [
  { value: 'EASY', label: 'Dễ', color: 'success' },
  { value: 'MEDIUM', label: 'Trung bình', color: 'warning' },
  { value: 'HARD', label: 'Khó', color: 'danger' }
]

const optionKeys = ['A', 'B', 'C', 'D']

const localType = ref('SINGLE_CHOICE')
const localContent = ref('')
const localOptions = reactive({ A: '', B: '', C: '', D: '' })
const localCorrectAnswers = ref([])
const localScore = ref(1)
const localDifficulty = ref('MEDIUM')
const attemptedSubmit = ref(false)

const errors = reactive({
  content: '',
  options: ''
})

const isEssay = computed(() => localType.value === 'ESSAY')
const isMultiple = computed(() => localType.value === 'MULTIPLE_CHOICE')

const isCorrect = (opt) => {
  if (isMultiple.value) {
    return localCorrectAnswers.value.includes(opt)
  }
  return localCorrectAnswers.value[0] === opt
}

const setCorrect = (opt) => {
  if (isMultiple.value) {
    if (localCorrectAnswers.value.includes(opt)) {
      localCorrectAnswers.value = localCorrectAnswers.value.filter(a => a !== opt)
    } else {
      localCorrectAnswers.value = [...localCorrectAnswers.value, opt]
    }
  } else {
    localCorrectAnswers.value = [opt]
  }
  errors.options = ''
}

const isValid = computed(() => {
  if (!localContent.value.trim()) return false
  if (!isEssay.value) {
    const hasFilledOption = optionKeys.some(k => localOptions[k].trim())
    if (!hasFilledOption) return false
    if (localCorrectAnswers.value.length === 0) return false
  }
  return true
})

const buildQuestion = () => {
  const base = {
    _localId: props.question?._localId || Date.now(),
    content: localContent.value.trim(),
    type: localType.value,
    score: Number(localScore.value) || 1,
    scoreWeight: Number(localScore.value) || 1,
    difficulty: localDifficulty.value,
    optionA: localOptions.A.trim(),
    optionB: localOptions.B.trim(),
    optionC: localOptions.C.trim(),
    optionD: localOptions.D.trim()
  }

  if (isMultiple.value) {
    base.correctAnswer = [...localCorrectAnswers.value]
    base.options = JSON.stringify(optionKeys.map(k => ({
      id: k,
      text: localOptions[k].trim(),
      isCorrect: localCorrectAnswers.value.includes(k)
    })))
  } else if (isEssay.value) {
    base.correctAnswer = ''
    base.options = ''
  } else {
    base.correctAnswer = localCorrectAnswers.value[0] || ''
    base.options = JSON.stringify(optionKeys.map(k => ({
      id: k,
      text: localOptions[k].trim(),
      isCorrect: localCorrectAnswers.value[0] === k
    })))
  }

  if (props.question?.id) {
    base.id = props.question.id
  }

  return base
}

const handleSave = () => {
  attemptedSubmit.value = true

  if (!localContent.value.trim()) {
    errors.content = 'Vui lòng nhập nội dung câu hỏi'
    return
  }

  if (!isEssay.value) {
    const hasFilledOption = optionKeys.some(k => localOptions[k].trim())
    if (!hasFilledOption) {
      errors.options = 'Vui lòng nhập ít nhất một đáp án'
      return
    }
    if (localCorrectAnswers.value.length === 0) {
      errors.options = 'Vui lòng chọn đáp án đúng'
      return
    }
  }

  emit('save', buildQuestion())
  close()
}

const close = () => {
  emit('update:modelValue', false)
  attemptedSubmit.value = false
  errors.content = ''
  errors.options = ''
}

const resetForm = () => {
  if (props.question) {
    localContent.value = props.question.content || ''
    localType.value = props.question.type || 'SINGLE_CHOICE'
    localOptions.A = props.question.optionA || ''
    localOptions.B = props.question.optionB || ''
    localOptions.C = props.question.optionC || ''
    localOptions.D = props.question.optionD || ''
    const ca = props.question.correctAnswer || props.question.correctOption || []
    localCorrectAnswers.value = Array.isArray(ca) ? [...ca] : ca ? [String(ca)] : []
    localScore.value = Number(props.question.scoreWeight || props.question.score || 1)
    localDifficulty.value = props.question.difficulty || 'MEDIUM'
  } else {
    localContent.value = ''
    localType.value = 'SINGLE_CHOICE'
    localOptions.A = ''
    localOptions.B = ''
    localOptions.C = ''
    localOptions.D = ''
    localCorrectAnswers.value = []
    localScore.value = 1
    localDifficulty.value = 'MEDIUM'
  }
}

watch(() => props.modelValue, (val) => {
  if (val) resetForm()
})
</script>


<style scoped>
.qemodal-overlay {
  position: fixed;
  inset: 0;
  z-index: 1000;
  background: rgba(0, 0, 0, 0.4);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 1.5rem;
}

.qemodal {
  background: white;
  border-radius: var(--ds-radius-2xl);
  width: 100%;
  max-width: 600px;
  box-shadow: 0 24px 64px rgba(0, 0, 0, 0.2);
  overflow: hidden;
  display: flex;
  flex-direction: column;
  max-height: 90vh;
}

.dark .qemodal {
  background: var(--ds-gray-800);
}

.qemodal--lg { max-width: 680px; }
.qemodal--xl { max-width: 800px; }

/* Header */
.qemodal__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 0.875rem;
  padding: 1.5rem;
  border-bottom: 1px solid var(--ds-border);
  background: linear-gradient(135deg, var(--ds-primary-soft) 0%, transparent 100%);
}

.dark .qemodal__header {
  border-bottom-color: var(--ds-border-strong);
}

.qemodal__header-left {
  display: flex;
  align-items: center;
  gap: 0.875rem;
}

.qemodal__icon {
  width: 44px;
  height: 44px;
  border-radius: var(--ds-radius-xl);
  background: var(--ds-primary);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  box-shadow: 0 4px 12px rgba(79, 70, 229, 0.25);
}


.qemodal__title {
  font-family: var(--ds-font-display);
  font-size: 1.125rem;
  font-weight: 800;
  color: var(--ds-text);
  margin: 0;
  line-height: 1.3;
}

.dark .qemodal__title { color: #f1f5f9; }

.qemodal__subtitle {
  font-size: 0.8rem;
  color: var(--ds-text-muted);
  margin: 0.25rem 0 0;
}

.qemodal__close {
  width: 2rem;
  height: 2rem;
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

.dark .qemodal__close { background: var(--ds-gray-700); }
.qemodal__close:hover { background: var(--ds-gray-200); color: var(--ds-text); }
.dark .qemodal__close:hover { background: var(--ds-gray-600); }

/* Type bar */
.qemodal__type-bar {
  display: flex;
  gap: 0.5rem;
  padding: 0.875rem 1.5rem;
  border-bottom: 1px solid var(--ds-border);
  background: var(--ds-gray-50);
  overflow-x: auto;
}

.dark .qemodal__type-bar { background: var(--ds-gray-800); border-bottom-color: var(--ds-border-strong); }

.qemodal__type-btn {
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
  transition: all 0.15s ease;
  white-space: nowrap;
}

.dark .qemodal__type-btn { background: var(--ds-gray-700); border-color: var(--ds-border-strong); color: #94a3b8; }

.qemodal__type-btn:hover {
  border-color: var(--ds-primary);
  color: var(--ds-primary);
  background: var(--ds-primary-soft);
}

.qemodal__type-btn--active {
  background: var(--ds-primary);
  border-color: var(--ds-primary);
  color: white;
  box-shadow: 0 4px 12px rgba(79, 70, 229, 0.25);
}

.qemodal__type-btn--active:hover {
  background: var(--ds-primary);
  color: white;
}


/* Body */
.qemodal__body {
  flex: 1;
  overflow-y: auto;
  padding: 1.5rem;
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.qemodal__field {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.qemodal__field--sm {}

.qemodal__label {
  font-size: 0.8rem;
  font-weight: 700;
  color: var(--ds-text);
}

.dark .qemodal__label { color: #f1f5f9; }

.qemodal__required { color: var(--ds-danger); margin-left: 2px; }

.qemodal__textarea {
  width: 100%;
  padding: 0.6875rem 1rem;
  background: var(--ds-surface);
  border: 1.5px solid var(--ds-border);
  border-radius: var(--ds-radius-xl);
  font-size: 0.875rem;
  color: var(--ds-text);
  transition: all 0.15s ease;
  outline: none;
  resize: vertical;
  min-height: 80px;
  line-height: 1.5;
  font-family: inherit;
}

.dark .qemodal__textarea { background: var(--ds-gray-800); border-color: var(--ds-border-strong); color: #f1f5f9; }
.qemodal__textarea::placeholder { color: var(--ds-text-muted); }
.dark .qemodal__textarea::placeholder { color: var(--ds-gray-500); }
.qemodal__textarea:focus { border-color: var(--ds-primary); box-shadow: 0 0 0 3px var(--ds-primary-ring); }

.qemodal__textarea--error {
  border-color: var(--ds-danger);
  box-shadow: 0 0 0 3px var(--ds-danger-soft);
}

.qemodal__input {
  width: 100%;
  padding: 0.6875rem 1rem;
  background: var(--ds-surface);
  border: 1.5px solid var(--ds-border);
  border-radius: var(--ds-radius-xl);
  font-size: 0.875rem;
  color: var(--ds-text);
  transition: all 0.15s ease;
  outline: none;
}

.dark .qemodal__input { background: var(--ds-gray-800); border-color: var(--ds-border-strong); color: #f1f5f9; }
.qemodal__input::placeholder { color: var(--ds-text-muted); }
.dark .qemodal__input::placeholder { color: var(--ds-gray-500); }
.qemodal__input:focus { border-color: var(--ds-primary); box-shadow: 0 0 0 3px var(--ds-primary-ring); }

.qemodal__input--center {
  width: 100px;
  text-align: center;
  font-weight: 700;
  font-size: 1rem;
  font-family: var(--ds-font-display);
}

.qemodal__field-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.qemodal__hint {
  font-size: 0.7rem;
  color: var(--ds-text-muted);
  margin-left: auto;
}

.qemodal__error {
  font-size: 0.7rem;
  color: var(--ds-danger);
  font-weight: 600;
}

/* Options */
.qemodal__options-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0.75rem;
}

@media (max-width: 480px) {
  .qemodal__options-grid { grid-template-columns: 1fr; }
}

.qemodal__opt {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.qemodal__opt-badge {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: 2px solid var(--ds-border);
  background: var(--ds-gray-100);
  color: var(--ds-text-muted);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.7rem;
  font-weight: 800;
  flex-shrink: 0;
  cursor: pointer;
  transition: all 0.15s ease;
}

.dark .qemodal__opt-badge { background: var(--ds-gray-700); border-color: var(--ds-border-strong); }

.qemodal__opt-badge:hover {
  border-color: var(--ds-success);
  color: var(--ds-success);
}

.qemodal__opt-badge--correct {
  background: var(--ds-success);
  border-color: var(--ds-success);
  color: white;
  box-shadow: 0 4px 12px rgba(22, 163, 74, 0.25);
}

.qemodal__opt-badge--correct:hover {
  background: var(--ds-success);
  color: white;
}


.qemodal__opt-input {
  flex: 1;
  padding: 0.5rem 0.75rem;
  background: var(--ds-surface);
  border: 1.5px solid var(--ds-border);
  border-radius: var(--ds-radius-lg);
  font-size: 0.875rem;
  color: var(--ds-text);
  outline: none;
  transition: all 0.15s ease;
}

.dark .qemodal__opt-input { background: var(--ds-gray-800); border-color: var(--ds-border-strong); color: #f1f5f9; }
.qemodal__opt-input::placeholder { color: var(--ds-text-muted); font-size: 0.8rem; }
.dark .qemodal__opt-input::placeholder { color: var(--ds-gray-500); }
.qemodal__opt-input:focus { border-color: var(--ds-primary); box-shadow: 0 0 0 2px var(--ds-primary-ring); }
.qemodal__opt-input--correct { border-color: var(--ds-success); background: var(--ds-success-soft); }

/* Meta row */
.qemodal__meta-row {
  display: flex;
  gap: 1rem;
  align-items: flex-end;
  flex-wrap: wrap;
}

.qemodal__difficulty-btns {
  display: flex;
  gap: 0.375rem;
}

.qemodal__diff-btn {
  padding: 0.375rem 0.75rem;
  border-radius: var(--ds-radius-full);
  border: 1.5px solid var(--ds-border);
  background: var(--ds-surface);
  color: var(--ds-text-muted);
  font-size: 0.75rem;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.15s ease;
}

.dark .qemodal__diff-btn { background: var(--ds-gray-700); border-color: var(--ds-border-strong); color: #94a3b8; }

.qemodal__diff-btn:hover { border-color: currentColor; }

.qemodal__diff-btn--success { color: var(--ds-success); }
.qemodal__diff-btn--warning { color: #d97706; }
.qemodal__diff-btn--danger { color: var(--ds-danger); }

.qemodal__diff-btn--success:hover { background: var(--ds-success-soft); }
.qemodal__diff-btn--warning:hover { background: rgba(234, 179, 8, 0.1); }
.qemodal__diff-btn--danger:hover { background: var(--ds-danger-soft); }

.qemodal__diff-btn--active {
  color: white !important;
}

.qemodal__diff-btn--success.qemodal__diff-btn--active { background: var(--ds-success); border-color: var(--ds-success); }
.qemodal__diff-btn--warning.qemodal__diff-btn--active { background: #d97706; border-color: #d97706; }
.qemodal__diff-btn--danger.qemodal__diff-btn--active { background: var(--ds-danger); border-color: var(--ds-danger); }

/* Summary */
.qemodal__summary {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem 1rem;
  background: var(--ds-danger-soft);
  border: 1px solid rgba(220, 38, 38, 0.2);
  border-radius: var(--ds-radius-lg);
  font-size: 0.8rem;
  font-weight: 600;
  color: var(--ds-danger);
}


/* Footer */
.qemodal__footer {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 0.75rem;
  padding: 1.25rem 1.5rem;
  border-top: 1px solid var(--ds-border);
  background: var(--ds-gray-50);
}

.dark .qemodal__footer { border-top-color: var(--ds-border-strong); background: var(--ds-gray-800); }

.qemodal__btn {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.625rem 1.25rem;
  border-radius: var(--ds-radius-xl);
  font-size: 0.875rem;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.15s ease;
  border: 1px solid transparent;
}


.qemodal__btn--ghost {
  background: transparent;
  border-color: var(--ds-border);
  color: var(--ds-text-secondary);
}

.dark .qemodal__btn--ghost { border-color: var(--ds-border-strong); color: #94a3b8; }
.qemodal__btn--ghost:hover { border-color: var(--ds-gray-300); color: var(--ds-text); }
.dark .qemodal__btn--ghost:hover { background: var(--ds-gray-700); }

.qemodal__btn--primary {
  background: linear-gradient(135deg, var(--ds-primary) 0%, #6366f1 100%);
  color: white;
  box-shadow: 0 4px 12px rgba(79, 70, 229, 0.25);
}

.qemodal__btn--primary:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 6px 20px rgba(79, 70, 229, 0.35);
}

.qemodal__btn:disabled { opacity: 0.5; cursor: not-allowed; transform: none !important; }

/* Transition */
.qemodal-enter-active, .qemodal-leave-active { transition: all 0.2s ease; }
.qemodal-enter-from, .qemodal-leave-to { opacity: 0; }
.qemodal-enter-from .qemodal, .qemodal-leave-to .qemodal { transform: scale(0.95) translateY(8px); }
</style>
