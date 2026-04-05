<template>
  <div class="ec-section">
    <div class="ec-section__header">
      <div class="ec-section__icon">
        <LucideIcon name="edit_note" />
      </div>
      <div>
        <h3 class="ec-section__title">Chỉnh sửa câu hỏi</h3>
        <p class="ec-section__desc">Xem và chỉnh sửa các câu hỏi đã nhập từ file</p>
      </div>
    </div>

    <div class="ec-section__body">

      <!-- Back to import -->
      <button type="button" class="ec-back-btn" @click="$emit('back-to-import')">
        <LucideIcon name="arrow_back" />
        Quay lại nhập file
      </button>

      <!-- Questions list with full details -->
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
          <p>Vui lòng quay lại bước trước để nhập file</p>
        </div>

        <div v-else class="ec-qb-items">
          <div
            v-for="(q, i) in localQuestions"
            :key="q._localId"
            class="ec-qb-item"
          >
            <div class="ec-qb-item__header">
              <span class="ec-qb-item__num">{{ i + 1 }}</span>
              <div class="ec-qb-item__score">
                <input
                  type="number"
                  class="ec-score-input"
                  :value="q.score"
                  min="0.5"
                  max="100"
                  step="0.5"
                  @change="updateScore(i, $event)"
                />
                <span class="ec-score-label">điểm</span>
              </div>
              <button type="button" class="ec-qb-item__delete" @click="removeQuestion(i)">
                <LucideIcon name="delete" />
              </button>
            </div>

            <!-- Question content -->
            <div class="ec-qb-item__content-wrapper">
              <textarea
                class="ec-question-textarea"
                :value="q.content"
                rows="2"
                placeholder="Nội dung câu hỏi..."
                @input="updateQuestionContent(i, $event)"
              />
            </div>

            <!-- Options -->
            <div class="ec-qb-item__options">
              <div
                v-for="opt in getSortedOptions(q)"
                :key="opt.id"
                class="ec-qb-option"
                :class="{ 'ec-qb-option--correct': isCorrectAnswer(q, opt.id) }"
                @click="toggleCorrectAnswer(i, opt.id)"
              >
                <span class="ec-qb-option__id">{{ opt.id }}</span>
                <input
                  type="text"
                  class="ec-qb-option__text"
                  :value="opt.text"
                  placeholder="Nhập nội dung đáp án..."
                  @input="updateOptionText(i, opt.id, $event)"
                  @click.stop
                />
                <div class="ec-qb-option__actions">
                  <span v-if="isCorrectAnswer(q, opt.id)" class="ec-qb-option__check">
                    <LucideIcon name="check_circle" />
                  </span>
                </div>
              </div>
            </div>

            <!-- Chỉ hiện gợi ý khi chưa có đáp án được đánh dấu trên hàng (tránh trùng với highlight xanh) -->
            <div
              v-if="!questionShowsCorrectHighlight(q)"
              class="ec-qb-item__correct-hint"
            >
              <LucideIcon name="info" />
              <span>Chưa chọn đáp án đúng — nhấn vào một đáp án để đánh dấu</span>
            </div>
          </div>
        </div>
      </div>

    </div>

    <!-- Confirm delete dialog -->
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
import ConfirmDialog from '../../ui/ConfirmDialog.vue'

const props = defineProps({
  questions: { type: Array, default: () => [] }
})

const emit = defineEmits([
  'update:questions',
  'back-to-import'
])

const showDeleteAllConfirm = ref(false)

const localQuestions = computed({
  get: () => props.questions,
  set: (v) => emit('update:questions', v)
})

const getSortedOptions = (question) => {
  const optionIds = ['A', 'B', 'C', 'D']
  return optionIds
    .map(id => question.options?.find(o => o.id === id) || { id, text: '' })
    .filter(o => o.text || question.options?.find(opt => opt.id === o.id))
}

const isCorrectAnswer = (question, optionId) => {
  if (!question.correctAnswer) return false
  const correct = String(question.correctAnswer).toUpperCase()
  if (correct.includes(',')) {
    return correct.split(',').map(s => s.trim()).includes(optionId)
  }
  return correct === optionId
}

/** Có ít nhất một hàng đáp án đang được highlight là đúng (không lặp lại bằng dòng chữ) */
const questionShowsCorrectHighlight = (question) => {
  for (const opt of getSortedOptions(question)) {
    if (isCorrectAnswer(question, opt.id)) return true
  }
  return false
}

const updateQuestionContent = (index, event) => {
  const updated = [...localQuestions.value]
  updated[index] = { ...updated[index], content: event.target.value }
  localQuestions.value = updated
}

const updateOptionText = (questionIndex, optionId, event) => {
  const updated = [...localQuestions.value]
  const question = { ...updated[questionIndex] }
  const options = [...(question.options || [])]
  const optIndex = options.findIndex(o => o.id === optionId)
  if (optIndex >= 0) {
    options[optIndex] = { ...options[optIndex], text: event.target.value }
  } else {
    options.push({ id: optionId, text: event.target.value })
  }
  question.options = options
  updated[questionIndex] = question
  localQuestions.value = updated
}

const updateScore = (index, event) => {
  const updated = [...localQuestions.value]
  const score = parseFloat(event.target.value) || 1
  updated[index] = { ...updated[index], score: Math.max(0.5, Math.min(100, score)) }
  localQuestions.value = updated
}

const toggleCorrectAnswer = (questionIndex, optionId) => {
  const updated = [...localQuestions.value]
  const question = { ...updated[questionIndex] }
  const currentCorrect = String(question.correctAnswer || '').toUpperCase()

  let newCorrect
  if (currentCorrect.includes(',')) {
    const correctList = currentCorrect.split(',').map(s => s.trim())
    if (correctList.includes(optionId)) {
      correctList.splice(correctList.indexOf(optionId), 1)
      newCorrect = correctList.join(',')
    } else {
      correctList.push(optionId)
      newCorrect = correctList.sort().join(',')
    }
  } else {
    newCorrect = currentCorrect === optionId ? '' : optionId
  }

  question.correctAnswer = newCorrect
  updated[questionIndex] = question
  localQuestions.value = updated
}

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

.ec-back-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 0.875rem;
  border-radius: var(--ds-radius-lg);
  border: 1px solid var(--ds-border);
  background: var(--ds-gray-50);
  color: var(--ds-text-secondary);
  font-size: 0.8rem;
  font-weight: 600;
  cursor: pointer;
  transition: color 0.15s ease, background-color 0.15s ease, border-color 0.15s ease, box-shadow 0.15s ease, transform 0.15s ease;
  align-self: flex-start;
}

.dark .ec-back-btn {
  background: var(--ds-gray-800);
  border-color: var(--ds-border-strong);
  color: var(--ds-text-secondary);
}

.ec-back-btn:hover {
  background: var(--ds-primary-soft);
  border-color: var(--ds-primary-border);
  color: var(--ds-primary);
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
}

.ec-qb-delete-all-btn:hover {
  background: var(--ds-danger);
  color: white;
  border-color: var(--ds-danger);
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

.ec-qb-items { display: flex; flex-direction: column; gap: 0.75rem; }

.ec-qb-item {
  background: var(--ds-gray-50);
  border: 1px solid var(--ds-border);
  border-radius: var(--ds-radius-xl);
  padding: 1rem;
  transition: color 0.15s ease, background-color 0.15s ease, border-color 0.15s ease, box-shadow 0.15s ease, transform 0.15s ease;
}

.dark .ec-qb-item {
  background: var(--ds-gray-800);
  border-color: var(--ds-border-strong);
}

.ec-qb-item:hover { border-color: var(--ds-primary-border); }

.ec-qb-item__header {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  margin-bottom: 0.75rem;
}

.ec-qb-item__num {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: var(--ds-primary-soft);
  color: var(--ds-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.8rem;
  font-weight: 800;
  flex-shrink: 0;
}

.ec-qb-item__score {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  margin-left: auto;
}

.ec-score-input {
  width: 60px;
  padding: 0.375rem 0.5rem;
  border: 1px solid var(--ds-border);
  border-radius: var(--ds-radius-lg);
  font-size: 0.8rem;
  font-weight: 600;
  text-align: center;
  background: var(--ds-surface);
  color: var(--ds-text);
}

.dark .ec-score-input {
  background: var(--ds-gray-700);
  border-color: var(--ds-border-strong);
  color: var(--ds-text);
}

.ec-score-input:focus {
  outline: none;
  border-color: var(--ds-primary);
}

.ec-score-label {
  font-size: 0.75rem;
  color: var(--ds-text-muted);
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

.ec-qb-item__content-wrapper {
  margin-bottom: 0.75rem;
}

.ec-question-textarea {
  width: 100%;
  padding: 0.625rem 0.875rem;
  border: 1px solid var(--ds-border);
  border-radius: var(--ds-radius-lg);
  font-size: 0.875rem;
  line-height: 1.5;
  background: var(--ds-surface);
  color: var(--ds-text);
  resize: vertical;
  min-height: 60px;
  font-family: inherit;
}

.dark .ec-question-textarea {
  background: var(--ds-gray-700);
  border-color: var(--ds-border-strong);
  color: var(--ds-text);
}

.ec-question-textarea:focus {
  outline: none;
  border-color: var(--ds-primary);
  box-shadow: 0 0 0 3px var(--ds-primary-ring);
}

.ec-qb-item__options {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.ec-qb-option {
  display: flex;
  align-items: center;
  gap: 0.625rem;
  padding: 0.625rem 0.75rem;
  border: 1.5px solid var(--ds-border);
  border-radius: var(--ds-radius-lg);
  background: var(--ds-surface);
  cursor: pointer;
  transition: color 0.15s ease, background-color 0.15s ease, border-color 0.15s ease, box-shadow 0.15s ease, transform 0.15s ease;
}

.dark .ec-qb-option {
  background: var(--ds-gray-700);
  border-color: var(--ds-border-strong);
}

.ec-qb-option:hover {
  border-color: var(--ds-primary-border);
  background: var(--ds-primary-soft);
}

.ec-qb-option--correct {
  border-color: var(--ds-success);
  background: var(--ds-success-soft);
}

.dark .ec-qb-option--correct {
  background: rgba(22, 163, 74, 0.2);
}

.ec-qb-option__id {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: var(--ds-gray-200);
  color: var(--ds-text-secondary);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.75rem;
  font-weight: 800;
  flex-shrink: 0;
  transition: color 0.15s ease, background-color 0.15s ease, border-color 0.15s ease, box-shadow 0.15s ease, transform 0.15s ease;
}

.ec-qb-option--correct .ec-qb-option__id {
  background: var(--ds-success);
  color: white;
}

.ec-qb-option__text {
  flex: 1;
  padding: 0.375rem 0.5rem;
  border: none;
  background: transparent;
  font-size: 0.85rem;
  color: var(--ds-text);
}

.dark .ec-qb-option__text { color: var(--ds-text); }

.ec-qb-option__text:focus {
  outline: none;
  background: var(--ds-surface);
  border-radius: var(--ds-radius-md);
}

.dark .ec-qb-option__text:focus {
  background: var(--ds-gray-600);
}

.ec-qb-option__actions {
  display: flex;
  align-items: center;
  gap: 0.25rem;
}

.ec-qb-option__check {
  color: var(--ds-success);
}

.ec-qb-item__correct-hint {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-top: 0.75rem;
  padding: 0.5rem 0.75rem;
  background: var(--ds-gray-100);
  border: 1px solid var(--ds-border);
  border-radius: var(--ds-radius-lg);
  font-size: 0.75rem;
  color: var(--ds-text-muted);
}

.dark .ec-qb-item__correct-hint {
  background: var(--ds-gray-800);
  border-color: var(--ds-border-strong);
  color: var(--ds-text-secondary);
}

.ec-qb-item__correct-hint svg {
  flex-shrink: 0;
}
</style>
