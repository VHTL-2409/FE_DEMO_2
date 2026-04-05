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

      <!-- Questions list -->
      <div class="ec-qb-list">
        <div class="ec-qb-list__header">
          <h4 class="ec-qb-list__title">
            <LucideIcon name="list" />
            Danh sách câu hỏi
            <span class="ec-qb-list__count">{{ localQuestions.length }}</span>
          </h4>
          <div class="ec-qb-list__actions">
            <button
              v-if="localQuestions.length > 0"
              type="button"
              class="ec-qb-toggle-all-btn"
              @click="allExpanded ? collapseAll() : expandAll()"
            >
              <LucideIcon :name="allExpanded ? 'unfold_less' : 'unfold_more'" size="14" />
              {{ allExpanded ? 'Thu gọn tất cả' : 'Mở rộng tất cả' }}
            </button>
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
        </div>

        <div v-if="localQuestions.length === 0" class="ec-qb-empty">
          <LucideIcon name="quiz" />
          <p>Chưa có câu hỏi nào</p>
          <p>Vui lòng quay lại bước trước để nhập file</p>
        </div>

        <div v-else class="ec-qb-items">
          <article
            v-for="(q, i) in localQuestions"
            :key="q._localId"
            class="ec-qb-item"
            :class="{
              'ec-qb-item--expanded': expandedIds.has(i),
              'ec-qb-item--essay': q.type && q.type.toUpperCase().includes('ESSAY'),
              'ec-qb-item--low-conf': q.parseConfidence != null && q.parseConfidence < 0.7
            }"
          >
            <!-- Card header (always visible, click to expand/collapse) -->
            <button type="button" class="ec-qb-item__header-btn" @click="toggleExpand(i)">
              <div class="ec-qb-item__header-left">
                <span class="ec-qb-item__num">{{ i + 1 }}</span>
                <span class="ec-qb-type-badge" :class="`ec-qb-type-badge--${typeSlug(q.type)}`">
                  {{ typeLabel(q.type) }}
                </span>
                <span v-if="q.parseConfidence != null && q.parseConfidence < 0.7" class="ec-qb-conf-warn">
                  <LucideIcon name="warning" size="11" />
                  Confidence thấp
                </span>
              </div>
              <div class="ec-qb-item__header-right">
                <span v-if="q.correctAnswer" class="ec-qb-answer-chip">
                  <LucideIcon name="check" size="12" />
                  Đáp án: {{ q.correctAnswer }}
                </span>
                <span v-else class="ec-qb-no-answer-chip">
                  <LucideIcon name="help_outline" size="12" />
                  Chưa có đáp án
                </span>
                <LucideIcon
                  :name="expandedIds.has(i) ? 'expand_less' : 'expand_more'"
                  size="18"
                  class="ec-qb-expand-icon"
                />
              </div>
            </button>

            <!-- Expanded content -->
            <div v-if="expandedIds.has(i)" class="ec-qb-item__body">

              <!-- Score row -->
              <div class="ec-qb-item__meta-row">
                <div class="ec-qb-item__score-control">
                  <span class="ec-qb-meta-label">Điểm:</span>
                  <input
                    type="number"
                    class="ec-score-input"
                    :value="q.score"
                    min="0.5"
                    max="100"
                    step="0.5"
                    @change="updateScore(i, $event)"
                  />
                </div>
                <button type="button" class="ec-qb-item__delete-btn" @click="removeQuestion(i)">
                  <LucideIcon name="delete" size="15" />
                  Xóa câu hỏi
                </button>
              </div>

              <!-- Câu hỏi — section riêng -->
              <div class="ec-qb-section">
                <div class="ec-qb-section__header">
                  <LucideIcon name="quiz" size="14" />
                  <span>Câu hỏi</span>
                </div>
                <div class="ec-qb-section__body">
                  <textarea
                    class="ec-question-textarea"
                    :value="q.content"
                    rows="3"
                    placeholder="Nhập nội dung câu hỏi..."
                    @input="updateQuestionContent(i, $event)"
                  />
                </div>
              </div>

              <!-- Đáp án — section riêng -->
              <div v-if="!isEssayType(q)" class="ec-qb-section">
                <div class="ec-qb-section__header">
                  <LucideIcon name="checklist" size="14" />
                  <span>Đáp án</span>
                  <span class="ec-qb-section__hint">— nhấn vào đáp án đúng để đánh dấu</span>
                </div>
                <div class="ec-qb-section__body">
                  <div class="ec-qb-options-grid">
                    <div
                      v-for="opt in getSortedOptions(q)"
                      :key="opt.id"
                      class="ec-qb-option-card"
                      :class="{ 'ec-qb-option-card--correct': isCorrectAnswer(q, opt.id) }"
                      @click="toggleCorrectAnswer(i, opt.id)"
                    >
                      <div class="ec-qb-option-card__id" :class="{ 'ec-qb-option-card__id--correct': isCorrectAnswer(q, opt.id) }">
                        <LucideIcon v-if="isCorrectAnswer(q, opt.id)" name="check" size="14" />
                        <span v-else>{{ opt.id }}</span>
                      </div>
                      <input
                        type="text"
                        class="ec-qb-option-card__text"
                        :value="opt.text"
                        placeholder="Nhập nội dung đáp án..."
                        @input="updateOptionText(i, opt.id, $event)"
                        @click.stop
                      />
                    </div>
                  </div>
                </div>
              </div>

              <!-- Tự luận — đáp án mẫu -->
              <div v-else class="ec-qb-section">
                <div class="ec-qb-section__header">
                  <LucideIcon name="edit_note" size="14" />
                  <span>Đáp án mẫu / Gợi ý</span>
                </div>
                <div class="ec-qb-section__body">
                  <textarea
                    class="ec-question-textarea"
                    :value="q.correctAnswer"
                    rows="2"
                    placeholder="Nhập đáp án mẫu hoặc gợi ý..."
                    @input="updateEssayAnswer(i, $event)"
                  />
                </div>
              </div>

            </div>
          </article>
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
import { ref, computed, watch, nextTick } from 'vue'
import ConfirmDialog from '../../ui/ConfirmDialog.vue'

const props = defineProps({
  questions: { type: Array, default: () => [] }
})

const emit = defineEmits([
  'update:questions',
  'back-to-import'
])

const showDeleteAllConfirm = ref(false)
const expandedIds = ref(new Set([0, 1, 2]))

const allExpanded = computed(() => {
  return localQuestions.value.length > 0 && expandedIds.value.size === localQuestions.value.length
})

const localQuestions = computed({
  get: () => props.questions,
  set: (v) => emit('update:questions', v)
})

const isEssayType = (q) => {
  return q.type && q.type.toUpperCase().includes('ESSAY')
}

const typeLabel = (type) => {
  if (!type) return 'Trắc nghiệm'
  const t = type.toUpperCase()
  if (t.includes('ESSAY')) return 'Tự luận'
  if (t.includes('MULTI') || t.includes('MULTIPLE')) return 'Nhiều đáp án'
  return 'Trắc nghiệm'
}

const typeSlug = (type) => {
  if (!type) return 'mc'
  const t = type.toUpperCase()
  if (t.includes('ESSAY')) return 'essay'
  if (t.includes('MULTI') || t.includes('MULTIPLE')) return 'multi'
  return 'mc'
}

const toggleExpand = (i) => {
  const next = new Set(expandedIds.value)
  if (next.has(i)) {
    next.delete(i)
  } else {
    next.add(i)
  }
  expandedIds.value = next
}

const expandAll = () => {
  expandedIds.value = new Set(localQuestions.value.map((_, i) => i))
}

const collapseAll = () => {
  expandedIds.value = new Set()
}

/** Có ít nhất một đáp án có nội dung trong mảng options */
function hasStructuredMcqOptions(question) {
  const opts = question.options || []
  return opts.some((o) => o && String(o.text ?? '').trim().length > 0)
}

/**
 * Tách nội dung gộp (đề bài + A./B./C./D.) thành stem và 4 đáp án.
 * Hỗ trợ A. / A) / A． ở đầu dòng.
 */
function trySplitMcqFromContent(raw) {
  if (raw == null || typeof raw !== 'string') return null
  const text = raw.trim()
  if (!text) return null

  const lines = text.split(/\r?\n/)
  const optionLineRe = /^\s*([A-Da-d])[\.)．]\s*(.*)$/

  const stemLines = []
  const buffer = { A: '', B: '', C: '', D: '' }
  let inOptions = false
  let currentOpt = null

  for (const line of lines) {
    const m = line.match(optionLineRe)
    if (m) {
      const key = m[1].toUpperCase()
      if (key === 'A' || key === 'B' || key === 'C' || key === 'D') {
        inOptions = true
        currentOpt = key
        const rest = (m[2] ?? '').trim()
        if (rest) {
          buffer[key] = buffer[key] ? `${buffer[key]}\n${rest}` : rest
        }
      } else if (!inOptions) {
        stemLines.push(line)
      } else if (currentOpt) {
        const t = line.trim()
        if (t) {
          buffer[currentOpt] = buffer[currentOpt]
            ? `${buffer[currentOpt]}\n${t}`
            : t
        }
      } else {
        stemLines.push(line)
      }
    } else if (!inOptions) {
      stemLines.push(line)
    } else if (currentOpt) {
      const t = line.trim()
      if (t) {
        buffer[currentOpt] = buffer[currentOpt]
          ? `${buffer[currentOpt]}\n${t}`
          : t
      }
    } else {
      stemLines.push(line)
    }
  }

  const withText = ['A', 'B', 'C', 'D'].filter(
    (k) => (buffer[k] || '').trim().length > 0
  )
  if (withText.length < 2) return null

  const options = ['A', 'B', 'C', 'D'].map((id) => ({
    id,
    text: (buffer[id] || '').trim(),
  }))

  return {
    stem: stemLines.join('\n').trim(),
    options,
  }
}

const isApplyingSplit = ref(false)

watch(
  () => props.questions,
  (qs) => {
    if (!qs?.length || isApplyingSplit.value) return
    let changed = false
    const next = qs.map((q) => {
      if (isEssayType(q)) return q
      if (hasStructuredMcqOptions(q)) return q
      const parsed = trySplitMcqFromContent(q.content)
      if (!parsed) return q
      changed = true
      return {
        ...q,
        content: parsed.stem,
        options: parsed.options,
      }
    })
    if (!changed) return
    isApplyingSplit.value = true
    emit('update:questions', next)
    nextTick(() => {
      isApplyingSplit.value = false
    })
  },
  { deep: true, immediate: true }
)

/** Luôn hiển thị A–D; lấy text từ options (id không phân biệt hoa thường). */
const getSortedOptions = (question) => {
  const optionIds = ['A', 'B', 'C', 'D']
  return optionIds.map((id) => {
    const found = question.options?.find(
      (o) => String(o.id || '').toUpperCase() === id
    )
    return { id, text: found != null ? String(found.text ?? '') : '' }
  })
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

const updateEssayAnswer = (index, event) => {
  const updated = [...localQuestions.value]
  updated[index] = { ...updated[index], correctAnswer: event.target.value }
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

/* ── Expanded card layout ─────────────────────────────────────── */

.ec-qb-list__actions {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.ec-qb-toggle-all-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.375rem;
  padding: 0.375rem 0.75rem;
  border-radius: var(--ds-radius-lg);
  border: 1px solid var(--ds-border);
  background: var(--ds-gray-50);
  color: var(--ds-text-secondary);
  font-size: 0.75rem;
  font-weight: 600;
  cursor: pointer;
  transition: color 0.15s ease, background-color 0.15s ease, border-color 0.15s ease;
}

.dark .ec-qb-toggle-all-btn {
  background: var(--ds-gray-800);
  border-color: var(--ds-border-strong);
  color: var(--ds-text-secondary);
}

.ec-qb-toggle-all-btn:hover {
  background: var(--ds-primary-soft);
  border-color: var(--ds-primary-border);
  color: var(--ds-primary);
}

.ec-qb-item__header-btn {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  padding: 0;
  border: none;
  background: none;
  cursor: pointer;
  text-align: left;
}

.ec-qb-item__header-left {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.ec-qb-item__header-right {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex-shrink: 0;
}

.ec-qb-item--expanded {
  border-color: var(--ds-primary-border);
  box-shadow: 0 0 0 2px var(--ds-primary-ring);
}

.ec-qb-item--essay {
  border-left: 3px solid var(--ds-warning);
}

.ec-qb-item--low-conf {
  border-left: 3px solid var(--ds-danger);
}

.ec-qb-type-badge {
  padding: 0.125rem 0.5rem;
  border-radius: var(--ds-radius-full);
  font-size: 0.68rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.02em;
}

.ec-qb-type-badge--mc {
  background: rgba(79, 70, 229, 0.1);
  color: var(--ds-primary);
}

.ec-qb-type-badge--essay {
  background: rgba(234, 179, 8, 0.1);
  color: var(--ds-warning);
}

.ec-qb-type-badge--multi {
  background: rgba(14, 165, 233, 0.1);
  color: var(--ds-info);
}

.dark .ec-qb-type-badge--mc {
  background: rgba(129, 140, 248, 0.15);
  color: #a5b4fc;
}

.dark .ec-qb-type-badge--essay {
  background: rgba(234, 179, 8, 0.15);
  color: #fcd34d;
}

.dark .ec-qb-type-badge--multi {
  background: rgba(56, 189, 248, 0.15);
  color: #7dd3fc;
}

.ec-qb-conf-warn {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  padding: 0.125rem 0.5rem;
  border-radius: var(--ds-radius-full);
  font-size: 0.68rem;
  font-weight: 600;
  background: rgba(220, 38, 38, 0.1);
  color: var(--ds-danger);
}

.dark .ec-qb-conf-warn {
  background: rgba(220, 38, 38, 0.15);
  color: #fca5a5;
}

.ec-qb-answer-chip {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  padding: 0.25rem 0.625rem;
  border-radius: var(--ds-radius-full);
  font-size: 0.75rem;
  font-weight: 700;
  background: rgba(22, 163, 74, 0.1);
  color: var(--ds-success);
  border: 1px solid rgba(22, 163, 74, 0.2);
}

.dark .ec-qb-answer-chip {
  background: rgba(22, 163, 74, 0.15);
  color: #86efac;
  border-color: rgba(22, 163, 74, 0.25);
}

.ec-qb-no-answer-chip {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  padding: 0.25rem 0.625rem;
  border-radius: var(--ds-radius-full);
  font-size: 0.75rem;
  font-weight: 600;
  background: rgba(156, 163, 175, 0.1);
  color: var(--ds-text-muted);
  border: 1px dashed rgba(156, 163, 175, 0.3);
}

.dark .ec-qb-no-answer-chip {
  background: rgba(75, 75, 75, 0.3);
  color: var(--ds-text-secondary);
  border-color: rgba(100, 100, 100, 0.3);
}

.ec-qb-expand-icon {
  color: var(--ds-text-muted);
  flex-shrink: 0;
}

.ec-qb-item__body {
  margin-top: 1rem;
  padding-top: 1rem;
  border-top: 1px solid var(--ds-border);
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.dark .ec-qb-item__body {
  border-color: var(--ds-border-strong);
}

.ec-qb-item__meta-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
}

.ec-qb-item__score-control {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.ec-qb-meta-label {
  font-size: 0.8rem;
  font-weight: 600;
  color: var(--ds-text-secondary);
}

.ec-qb-item__delete-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.375rem;
  padding: 0.375rem 0.75rem;
  border-radius: var(--ds-radius-lg);
  border: 1px solid rgba(220, 38, 38, 0.3);
  background: rgba(220, 38, 38, 0.05);
  color: var(--ds-danger);
  font-size: 0.75rem;
  font-weight: 600;
  cursor: pointer;
  transition: color 0.15s, background-color 0.15s, border-color 0.15s;
}

.dark .ec-qb-item__delete-btn {
  background: rgba(220, 38, 38, 0.1);
  border-color: rgba(220, 38, 38, 0.4);
  color: #fca5a5;
}

.ec-qb-item__delete-btn:hover {
  background: var(--ds-danger);
  color: white;
  border-color: var(--ds-danger);
}

.ec-qb-field-label {
  display: block;
  font-size: 0.75rem;
  font-weight: 700;
  color: var(--ds-text-secondary);
  margin-bottom: 0.5rem;
  text-transform: uppercase;
  letter-spacing: 0.04em;
}

.ec-qb-field-hint {
  font-weight: 400;
  text-transform: none;
  letter-spacing: 0;
  color: var(--ds-text-muted);
}

.ec-question-textarea--sm {
  min-height: 48px;
}

.ec-qb-item__options-wrapper {
  display: flex;
  flex-direction: column;
  gap: 0;
}

.ec-qb-option__id-circle {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  border: 2px solid var(--ds-border);
  background: var(--ds-gray-100);
  color: var(--ds-text-muted);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.75rem;
  font-weight: 800;
  flex-shrink: 0;
  transition: all 0.15s ease;
}

.dark .ec-qb-option__id-circle {
  background: var(--ds-gray-700);
  border-color: var(--ds-border-strong);
  color: var(--ds-text-secondary);
}

.ec-qb-option__id-circle--correct {
  background: var(--ds-success) !important;
  border-color: var(--ds-success) !important;
  color: white !important;
  box-shadow: 0 2px 8px rgba(22, 163, 74, 0.35);
}

.ec-qb-option {
  cursor: pointer;
}

.ec-qb-option:hover .ec-qb-option__id-circle {
  border-color: var(--ds-primary);
  background: var(--ds-primary-soft);
}

.dark .ec-qb-option:hover .ec-qb-option__id-circle {
  background: rgba(129, 140, 248, 0.15);
}

/* ── Section layout: tách riêng câu hỏi / đáp án ──────────────── */

.ec-qb-section {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  background: var(--ds-surface);
  border: 1.5px solid var(--ds-border);
  border-radius: var(--ds-radius-xl);
  overflow: hidden;
}

.dark .ec-qb-section {
  background: var(--ds-gray-700);
  border-color: var(--ds-border-strong);
}

.ec-qb-section__header {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  padding: 0.5rem 0.875rem;
  background: var(--ds-gray-100);
  border-bottom: 1px solid var(--ds-border);
  font-size: 0.72rem;
  font-weight: 700;
  color: var(--ds-text-secondary);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.dark .ec-qb-section__header {
  background: var(--ds-gray-800);
  border-color: var(--ds-border-strong);
  color: var(--ds-text-secondary);
}

.ec-qb-section__hint {
  font-weight: 400;
  text-transform: none;
  letter-spacing: 0;
  color: var(--ds-text-muted);
  margin-left: 0.25rem;
}

.ec-qb-section__body {
  padding: 0.75rem;
}

/* ── Đáp án grid — mỗi đáp án 1 hàng riêng ──────────────────── */

.ec-qb-options-grid {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.ec-qb-option-card {
  display: flex;
  align-items: center;
  gap: 0.625rem;
  padding: 0.625rem 0.75rem;
  border: 1.5px solid var(--ds-border);
  border-radius: var(--ds-radius-lg);
  background: var(--ds-gray-50);
  cursor: pointer;
  transition: all 0.15s ease;
}

.dark .ec-qb-option-card {
  background: var(--ds-gray-800);
  border-color: var(--ds-border-strong);
}

.ec-qb-option-card:hover {
  border-color: var(--ds-primary-border);
  background: var(--ds-primary-soft);
}

.ec-qb-option-card--correct {
  border-color: var(--ds-success);
  background: var(--ds-success-soft);
}

.dark .ec-qb-option-card--correct {
  background: rgba(22, 163, 74, 0.15);
}

.ec-qb-option-card__id {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  border: 2px solid var(--ds-border);
  background: var(--ds-gray-200);
  color: var(--ds-text-secondary);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.8rem;
  font-weight: 800;
  flex-shrink: 0;
  transition: all 0.15s ease;
}

.dark .ec-qb-option-card__id {
  background: var(--ds-gray-700);
  border-color: var(--ds-border-strong);
  color: var(--ds-text-secondary);
}

.ec-qb-option-card__id--correct {
  background: var(--ds-success) !important;
  border-color: var(--ds-success) !important;
  color: white !important;
  box-shadow: 0 2px 8px rgba(22, 163, 74, 0.35);
}

.ec-qb-option-card__text {
  flex: 1;
  padding: 0.375rem 0.5rem;
  border: none;
  background: transparent;
  font-size: 0.875rem;
  color: var(--ds-text);
  line-height: 1.5;
  font-family: inherit;
}

.dark .ec-qb-option-card__text { color: var(--ds-text); }

.ec-qb-option-card__text:focus {
  outline: none;
  background: var(--ds-surface);
  border-radius: var(--ds-radius-md);
}

.dark .ec-qb-option-card__text:focus {
  background: var(--ds-gray-600);
}
</style>
