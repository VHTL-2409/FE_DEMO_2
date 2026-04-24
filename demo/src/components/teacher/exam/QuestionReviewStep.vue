<template>
  <div class="ec-section">
    <div class="ec-section__header">
      <div class="ec-section__icon">
        <LucideIcon name="edit_note" />
      </div>
      <div>
        <h3 class="ec-section__title">Chỉnh sửa câu hỏi</h3>
      </div>
    </div>

    <div class="ec-section__body">
      <button type="button" class="ec-back-btn" @click="$emit('back-to-import')">
        <LucideIcon name="arrow_back" />
        Về bước Nhập file
      </button>

      <div class="ec-qb-list">
        <div class="ec-qb-list__header">
          <div class="ec-qb-list__intro">
            <h4 class="ec-qb-list__title">
              <LucideIcon name="list" />
              Danh sách câu hỏi
              <span class="ec-qb-list__count">{{ localQuestions.length }}</span>
            </h4>
            <p class="ec-qb-list__sub">
              Kiểm tra lại nội dung, đáp án và điểm số trước khi xuất bản đề thi.
            </p>
          </div>
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
          <p>Quay lại bước Nhập file để tải lên và đọc câu hỏi trước khi chỉnh sửa</p>
        </div>

        <div v-else class="ec-qb-items">
          <template
            v-for="(q, i) in localQuestions"
            :key="q._localId"
          >
          <div
            v-if="shouldShowPartHeader(i)"
            class="ec-qb-part-divider"
            role="separator"
          >
            <span class="ec-qb-part-divider__line" aria-hidden="true" />
            <span class="ec-qb-part-divider__label">{{ sectionGroupLabel(q) }}</span>
            <span class="ec-qb-part-divider__line" aria-hidden="true" />
          </div>
          <article
            class="ec-qb-item"
            :class="{
              'ec-qb-item--expanded': expandedIds.has(i),
              'ec-qb-item--essay': q.type && q.type.toUpperCase().includes('ESSAY')
            }"
          >
            <!-- Card header (always visible, click to expand/collapse) -->
            <button type="button" class="ec-qb-item__header-btn" @click="toggleExpand(i)">
              <div class="ec-qb-item__header-left">
                <span class="ec-qb-item__num">{{ i + 1 }}</span>
                <span class="ec-qb-type-badge" :class="`ec-qb-type-badge--${typeSlug(q.type)}`">
                  {{ typeLabel(q.type) }}
                </span>
                <span
                  v-if="sectionHeaderChip(q)"
                  class="ec-qb-sec-chip"
                  :title="q.section || q.sectionKind || ''"
                >
                  {{ sectionHeaderChip(q) }}
                </span>
              </div>
              <div class="ec-qb-item__header-right">
                <span v-if="getQuestionAnswerValue(q)" class="ec-qb-answer-chip">
                  <LucideIcon name="check" size="12" />
                  Đáp án: {{ getQuestionAnswerValue(q) }}
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

              <!-- Câu hỏi — editor + preview hợp nhất -->
              <div class="ec-qb-section">
                <div class="ec-qb-section__header">
                  <LucideIcon name="quiz" size="14" />
                  <span>Câu hỏi</span>
                </div>
                <div class="ec-qb-section__body">
                  <div class="ec-qb-live-editor">
                    <div class="ec-qb-live-editor__pane ec-qb-live-editor__pane--source">
                      <div class="ec-qb-live-editor__header">
                        <span class="ec-qb-live-editor__label">Nguồn chỉnh sửa</span>
                        <span class="ec-qb-live-editor__hint">Nhập văn bản hoặc công thức `$...$`, `$$...$$` và xem kết quả ngay bên cạnh</span>
                      </div>
                      <div class="ec-qb-question-surface">
                        <textarea
                          class="ec-qb-question-surface__textarea"
                          :value="q.content"
                          rows="8"
                          placeholder="Chỉnh sửa nội dung (dùng $...$ hoặc $$...$$ cho công thức)..."
                          @input="updateQuestionContent(i, $event)"
                        />
                      </div>
                    </div>
                    <div class="ec-qb-live-editor__pane ec-qb-live-editor__pane--preview">
                      <div class="ec-qb-live-editor__header">
                        <span class="ec-qb-live-editor__label">Xem trước trực tiếp</span>
                        <span class="ec-qb-live-editor__hint">Preview cập nhật theo nội dung đang sửa</span>
                      </div>
                      <div class="ec-qb-preview-surface" aria-readonly="true">
                        <QuestionRenderer
                          :question="buildPreviewQuestion(q, i)"
                          :session-id="q.importSessionId ?? null"
                          :default-mode="previewDefaultMode(q)"
                          class="ec-qb-preview-surface__renderer"
                        />
                      </div>
                    </div>
                  </div>
                </div>
              </div>

              <!-- Đáp án — một lần hiển thị (latex-first), input chỉnh sửa riêng -->
              <div v-if="!isEssayType(q)" class="ec-qb-section">
                <div class="ec-qb-section__header">
                  <LucideIcon name="checklist" size="14" />
                  <span>Đáp án</span>
                  <span class="ec-qb-section__hint">
                    {{ isImageMathReviewQuestion(q) ? '— chọn đáp án đúng theo ảnh câu hỏi' : '— nhấn vào đáp án đúng để đánh dấu' }}
                  </span>
                </div>
                <div class="ec-qb-section__body">
                  <div class="ec-qb-options-grid" :class="{ 'ec-qb-options-grid--image-review': isImageMathReviewQuestion(q) }">
                    <component
                      v-for="opt in getSortedOptions(q)"
                      :key="opt.id"
                      :is="isImageMathReviewQuestion(q) ? 'button' : 'div'"
                      :type="isImageMathReviewQuestion(q) ? 'button' : undefined"
                      class="ec-qb-option-card"
                      :class="{
                        'ec-qb-option-card--correct': isCorrectAnswer(q, opt.id),
                        'ec-qb-option-card--empty': !isOptionSelectable(q, opt.id) && !isImageMathReviewQuestion(q),
                        'ec-qb-option-card--image-review': isImageMathReviewQuestion(q),
                        'ec-qb-option-card--disabled': !isOptionSelectable(q, opt.id),
                      }"
                      :title="!isOptionSelectable(q, opt.id) && !isImageMathReviewQuestion(q) ? 'Nhập nội dung đáp án trước khi đánh dấu đúng.' : ''"
                      :disabled="isImageMathReviewQuestion(q) ? !isOptionSelectable(q, opt.id) : undefined"
                      :tabindex="!isImageMathReviewQuestion(q) && isOptionSelectable(q, opt.id) ? 0 : undefined"
                      :role="!isImageMathReviewQuestion(q) ? 'button' : undefined"
                      :aria-pressed="isCorrectAnswer(q, opt.id)"
                      :aria-disabled="!isOptionSelectable(q, opt.id)"
                      @click="toggleCorrectAnswer(i, opt.id)"
                      @keydown.enter.prevent="!isImageMathReviewQuestion(q) && toggleCorrectAnswer(i, opt.id)"
                      @keydown.space.prevent="!isImageMathReviewQuestion(q) && toggleCorrectAnswer(i, opt.id)"
                    >
                      <div class="ec-qb-option-card__id" :class="{ 'ec-qb-option-card__id--correct': isCorrectAnswer(q, opt.id) }">
                        <LucideIcon v-if="isCorrectAnswer(q, opt.id)" name="check" size="14" />
                        <span v-else>{{ opt.id }}</span>
                      </div>
                      <div class="ec-qb-option-card__content">
                        <div v-if="isImageMathReviewQuestion(q)" class="ec-qb-option-card__image-choice">
                          <span class="ec-qb-option-card__image-choice-text">Chọn đáp án {{ opt.id }}</span>
                        </div>
                        <div
                          v-else-if="isEditingOption(i, opt.id)"
                          class="ec-qb-option-card__edit"
                          @click.stop
                        >
                          <input
                            type="text"
                            class="ec-qb-option-card__text"
                            :value="opt.text"
                            placeholder="Nhập nội dung đáp án..."
                            @input="updateOptionText(i, opt.id, $event)"
                            @blur="closeOptionEdit"
                            @keydown.escape.prevent="closeOptionEdit"
                            @click.stop
                          />
                        </div>
                        <div v-else class="ec-qb-option-card__display">
                          <MathDisplay
                            :content="opt.text"
                            :latex-content="getLatexOption(q, opt.id)"
                            source-preference="latex-first"
                            class="ec-qb-option-card__math"
                          />
                          <button
                            type="button"
                            class="ec-qb-option-edit-btn"
                            @click="openOptionEdit(i, opt.id, $event)"
                          >
                            Sửa
                          </button>
                        </div>
                      </div>
                    </component>
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
                    :value="getQuestionAnswerValue(q)"
                    rows="2"
                    placeholder="Nhập đáp án mẫu hoặc gợi ý..."
                    @input="updateEssayAnswer(i, $event)"
                  />
                </div>
              </div>

            </div>
          </article>
          </template>
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
import MathDisplay from '../../shared/MathDisplay.vue'
import QuestionRenderer from '../../import/QuestionRenderer.vue'

const props = defineProps({
  questions: { type: Array, default: () => [] }
})

const emit = defineEmits([
  'update:questions',
  'back-to-import'
])

const showDeleteAllConfirm = ref(false)
const expandedIds = ref(new Set([0, 1, 2]))
/** `${questionIndex}-${optionId}` — chỉ một ô đáp án đang sửa; tránh preview + input trùng nhau */
const editingOptionKey = ref(null)

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

const isImageMathReviewQuestion = (question) => {
  const rawMode = question?.render?.mode ?? question?.renderMode ?? question?.render_mode
  const normalizedMode = String(rawMode ?? '').trim().toLowerCase()
  const contentType = String(question?.contentType ?? question?.content_type ?? '').trim().toLowerCase()
  return normalizedMode === 'image' && (
    contentType === 'math' ||
    contentType === 'mixed' ||
    !hasStructuredMcqOptions(question)
  )
}

const getQuestionAnswerValue = (question) => {
  if (question?.correctAnswer !== undefined && question?.correctAnswer !== null) {
    return question.correctAnswer
  }
  return question?.correctOption ?? ''
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

/** Phần đề (import PDF/DOCX): nhóm + nhãn hiển thị */
function sectionGroupKey (q) {
  const s = (q.section != null && String(q.section).trim()) || ''
  const k = (q.sectionKind != null && String(q.sectionKind).trim()) || ''
  return `${k}::${s}`
}

function sectionKindLabel (kind) {
  if (kind == null || String(kind).trim() === '') return ''
  const k = String(kind).toLowerCase()
  const map = {
    mcq: 'Trắc nghiệm',
    essay: 'Tự luận',
    answer: 'Đáp án',
    question: 'Điền / Phần câu hỏi',
    solution: 'Lời giải'
  }
  return map[k] || String(kind)
}

function sectionGroupLabel (q) {
  const s = (q.section != null && String(q.section).trim()) || ''
  if (s) return s
  return sectionKindLabel(q.sectionKind) || 'Phần khác'
}

function sectionHeaderChip (q) {
  const s = (q.section != null && String(q.section).trim()) || ''
  if (s) {
    return s.length > 40 ? `${s.slice(0, 37)}…` : s
  }
  return sectionKindLabel(q.sectionKind) || ''
}

function shouldShowPartHeader (index) {
  const qs = localQuestions.value
  if (!qs.length || index >= qs.length) return false
  const q = qs[index]
  const key = sectionGroupKey(q)
  if (!key || key === '::') return false
  if (index === 0) return true
  return sectionGroupKey(qs[index - 1]) !== key
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
  const opts = normalizedOptionRows(question)
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
  const optionLineRe = /^\s*([A-Da-d])[).．]\s*(.*)$/

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
const normalizedOptionRows = (question) => {
  if (Array.isArray(question?.options) && question.options.length > 0) {
    return question.options
  }
  return ['A', 'B', 'C', 'D']
    .map((id) => ({
      id,
      text: question?.[`option${id}`] ?? '',
    }))
    .filter((option) => String(option.text ?? '').trim() !== '')
}

const getSortedOptions = (question) => {
  const optionIds = ['A', 'B', 'C', 'D']
  return optionIds.map((id) => {
    const found = normalizedOptionRows(question).find(
      (o) => String(o.id || '').toUpperCase() === id
    )
    return { id, text: found != null ? String(found.text ?? '') : '' }
  })
}

const optionHasText = (question, optionId) => {
  if (isImageMathReviewQuestion(question)) {
    return true
  }
  const normalizedId = String(optionId).toUpperCase()
  return getSortedOptions(question).some(
    (opt) => opt.id === normalizedId && String(opt.text ?? '').trim() !== ''
  )
}

const isOptionSelectable = (question, optionId) => optionHasText(question, optionId)

const normalizeCorrectAnswerList = (value) => {
  if (Array.isArray(value)) {
    return [...new Set(value.map((item) => String(item).trim().toUpperCase()).filter(Boolean))]
  }
  const text = String(value ?? '').trim()
  if (!text) return []
  return [...new Set(text.split(',').map((item) => item.trim().toUpperCase()).filter(Boolean))]
}

const stringifyCorrectAnswerList = (values) => values.join(',')

const removeCorrectAnswerOption = (value, optionId) => {
  const normalizedId = String(optionId).toUpperCase()
  return stringifyCorrectAnswerList(
    normalizeCorrectAnswerList(value).filter((item) => item !== normalizedId)
  )
}

const isCorrectAnswer = (question, optionId) => {
  const correctValue = getQuestionAnswerValue(question)
  if (!correctValue) return false
  const oid = String(optionId).toUpperCase()
  return normalizeCorrectAnswerList(correctValue).includes(oid)
}

const setQuestionAnswerValue = (question, value) => {
  question.correctAnswer = value
  question.correctOption = null
}

function optionEditKey(questionIndex, optionId) {
  return `${questionIndex}-${optionId}`
}

function isEditingOption(questionIndex, optionId) {
  return editingOptionKey.value === optionEditKey(questionIndex, optionId)
}

function openOptionEdit(questionIndex, optionId, e) {
  e?.stopPropagation()
  editingOptionKey.value = optionEditKey(questionIndex, optionId)
}

function closeOptionEdit() {
  editingOptionKey.value = null
}

const getLatexOption = (question, optionId) => {
  const lo = question.latexOptions
  if (!lo) return null
  if (typeof lo === 'object') {
    const v = lo[optionId] ?? lo[optionId.toUpperCase()]
    return v != null && String(v).trim() !== '' ? String(v) : null
  }
  if (typeof lo === 'string') {
    try {
      const parsed = JSON.parse(lo)
      const v = parsed[optionId] ?? parsed[optionId.toUpperCase()]
      return v != null && String(v).trim() !== '' ? String(v) : null
    } catch {
      return null
    }
  }
  return null
}

const buildPreviewQuestion = (question, index) => {
  const options = Object.fromEntries(
    getSortedOptions(question)
      .filter((opt) => String(opt.text ?? '').trim() !== '')
      .map((opt) => [opt.id, opt.text]),
  )

  return {
    number: question.questionNumber ?? index + 1,
    questionIndex: question.importQuestionIndex ?? question.questionIndex ?? index + 1,
    text: question.content ?? '',
    content: question.content ?? '',
    type: String(question.type || 'SINGLE_CHOICE').toUpperCase(),
    options,
    answer: getQuestionAnswerValue(question) || null,
    explanation: question.explanation ?? null,
    ...(question.parseConfidence != null ? { confidence: question.parseConfidence } : {}),
    render: question.render ?? null,
    renderMode: question.renderMode ?? question.render_mode ?? null,
    issues: Array.isArray(question.issues) ? question.issues : [],
    latexContent: question.latexContent ?? null,
    latexOptions: question.latexOptions ?? null,
    contentType: question.contentType ?? question.content_type ?? null,
  }
}

const previewDefaultMode = (question) => {
  const rawMode = question?.render?.mode ?? question?.renderMode ?? question?.render_mode
  return String(rawMode || '').toLowerCase() === 'image' ? 'image' : 'text'
}

const clearLegacyRenderState = (question) => ({
  ...question,
  render: null,
  renderMode: null,
  render_mode: null,
})

const updateQuestionContent = (index, event) => {
  const updated = [...localQuestions.value]
  updated[index] = {
    ...clearLegacyRenderState(updated[index]),
    content: event.target.value,
    latexContent: undefined,
    contentType: undefined,
    parseConfidence: null,
    issues: [],
    _manualEdited: true,
  }
  localQuestions.value = updated
}

const updateOptionText = (questionIndex, optionId, event) => {
  const updated = [...localQuestions.value]
  const question = clearLegacyRenderState(updated[questionIndex])
  const nextText = String(event.target.value ?? '')
  setQuestionAnswerValue(question, getQuestionAnswerValue(question))
  const options = [...(question.options || [])]
  const optIndex = options.findIndex(o => o.id === optionId)
  if (optIndex >= 0) {
    options[optIndex] = { ...options[optIndex], text: nextText }
  } else {
    options.push({ id: optionId, text: nextText })
  }
  question.options = options
  if (!nextText.trim()) {
    setQuestionAnswerValue(
      question,
      removeCorrectAnswerOption(getQuestionAnswerValue(question), optionId),
    )
  }
  // Bỏ latex đáp án đã sửa — tránh preview latex-first vẫn dùng bản cũ, trùng/lệch với input
  const lo = question.latexOptions
  if (lo != null) {
    if (typeof lo === 'object') {
      const next = { ...lo }
      delete next[optionId]
      delete next[optionId.toUpperCase()]
      question.latexOptions = Object.keys(next).length > 0 ? next : undefined
    } else if (typeof lo === 'string') {
      try {
        const parsed = JSON.parse(lo)
        delete parsed[optionId]
        delete parsed[optionId.toUpperCase()]
        question.latexOptions =
          Object.keys(parsed).length > 0 ? JSON.stringify(parsed) : undefined
      } catch {
        question.latexOptions = undefined
      }
    }
  }
  question.parseConfidence = null
  question.issues = []
  question._manualEdited = true
  updated[questionIndex] = question
  localQuestions.value = updated
}

const updateScore = (index, event) => {
  const updated = [...localQuestions.value]
  const score = parseFloat(event.target.value) || 1
  updated[index] = {
    ...updated[index],
    score: Math.max(0.5, Math.min(100, score)),
    _manualEdited: true,
  }
  localQuestions.value = updated
}

const updateEssayAnswer = (index, event) => {
  const updated = [...localQuestions.value]
  const nextQuestion = {
    ...clearLegacyRenderState(updated[index]),
    parseConfidence: null,
    issues: [],
    _manualEdited: true,
  }
  setQuestionAnswerValue(nextQuestion, event.target.value)
  updated[index] = nextQuestion
  localQuestions.value = updated
}

const toggleCorrectAnswer = (questionIndex, optionId) => {
  const updated = [...localQuestions.value]
  const question = { ...updated[questionIndex] }
  const oid = String(optionId).toUpperCase()
  if (!optionHasText(question, oid)) {
    return
  }
  const correctList = normalizeCorrectAnswerList(getQuestionAnswerValue(question))
  const isMultipleAnswer = String(question.type || '').toUpperCase().includes('MULTI')

  let newCorrect
  if (isMultipleAnswer) {
    if (correctList.includes(oid)) {
      newCorrect = stringifyCorrectAnswerList(correctList.filter((item) => item !== oid))
    } else {
      newCorrect = stringifyCorrectAnswerList([...correctList, oid])
    }
  } else {
    newCorrect = correctList[0] === oid && correctList.length === 1 ? '' : oid
  }

  setQuestionAnswerValue(question, newCorrect)
  question.parseConfidence = null
  question.issues = []
  question._manualEdited = true
  updated[questionIndex] = question
  localQuestions.value = updated
}

const removeQuestion = (index) => {
  const updated = [...localQuestions.value]
  const question = updated[index]
  if (question?._importFingerprint) {
    updated[index] = { ...question, _manualEdited: true, _deletedFromImportBatch: true }
    updated.splice(index, 1)
  } else {
    updated.splice(index, 1)
  }
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
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
  padding: 1.5rem;
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

.ec-qb-list { display: flex; flex-direction: column; gap: 0.75rem; }

.ec-qb-list__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.ec-qb-list__intro {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
  min-width: 0;
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

.ec-qb-list__sub {
  margin: 0;
  font-size: 0.75rem;
  color: var(--ds-text-muted);
  line-height: 1.5;
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

.ec-qb-live-editor {
  display: grid;
  grid-template-columns: minmax(0, 1.15fr) minmax(0, 1fr);
  gap: 0.875rem;
  align-items: stretch;
}

.ec-qb-live-editor__pane {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  min-width: 0;
}

.ec-qb-live-editor__header {
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
}

.ec-qb-live-editor__label {
  font-size: 0.7rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.04em;
  color: var(--ds-text-muted);
}

.ec-qb-live-editor__hint {
  font-size: 0.75rem;
  line-height: 1.5;
  color: var(--ds-text-secondary);
}

.ec-qb-preview-surface {
  border: 1px solid var(--ds-border);
  border-radius: var(--ds-radius-lg);
  padding: 0.625rem 0.875rem;
  background: var(--ds-gray-50, #f8fafc);
  min-height: 100%;
}

.dark .ec-qb-preview-surface {
  border-color: var(--ds-border-strong);
  background: var(--ds-gray-800, #1e293b);
}

.ec-qb-preview-surface__renderer {
  height: 100%;
}

.ec-qb-option-card__display {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 0.5rem;
  width: 100%;
}

.ec-qb-option-card__math {
  flex: 1;
  min-width: 0;
  font-size: 0.875rem;
}

.ec-qb-option-edit-btn {
  flex-shrink: 0;
  padding: 0.2rem 0.45rem;
  border: 1px solid var(--ds-border);
  border-radius: var(--ds-radius-md);
  background: var(--ds-surface);
  color: var(--ds-text-secondary);
  font-size: 0.7rem;
  font-weight: 600;
  cursor: pointer;
}

.ec-qb-option-edit-btn:hover {
  border-color: var(--ds-primary-border);
  color: var(--ds-primary);
}

.dark .ec-qb-option-edit-btn {
  background: var(--ds-gray-700);
  border-color: var(--ds-border-strong);
}

.ec-qb-option-card__edit {
  width: 100%;
}

/* Câu hỏi: preview đọc + textarea chỉnh sửa */
.ec-qb-question-surface {
  border: 1px solid var(--ds-border);
  border-radius: var(--ds-radius-lg);
  overflow: hidden;
  background: var(--ds-surface);
}

.dark .ec-qb-question-surface {
  border-color: var(--ds-border-strong);
  background: var(--ds-gray-700);
}

.ec-qb-question-surface__textarea {
  display: block;
  width: 100%;
  box-sizing: border-box;
  padding: 0.625rem 0.875rem;
  border: none;
  border-radius: 0;
  font-size: 0.875rem;
  line-height: 1.5;
  background: var(--ds-gray-50, #f8fafc);
  color: var(--ds-text);
  resize: vertical;
  min-height: 100px;
  font-family: ui-monospace, 'Cascadia Code', 'Source Code Pro', Menlo, monospace;
}

.dark .ec-qb-question-surface__textarea {
  background: var(--ds-gray-800, #1e293b);
  color: var(--ds-text);
}

.ec-qb-question-surface__textarea:focus {
  outline: none;
  box-shadow: inset 0 0 0 2px var(--ds-primary-ring);
}

@media (max-width: 980px) {
  .ec-qb-live-editor {
    grid-template-columns: minmax(0, 1fr);
  }

  .ec-qb-preview-surface {
    min-height: 2.5rem;
  }
}

.ec-qb-question-surface__editor-actions {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 0.75rem;
  border-top: 1px solid var(--ds-border);
  background: var(--ds-gray-50, #f8fafc);
}

.dark .ec-qb-question-surface__editor-actions {
  border-top-color: var(--ds-border-strong);
  background: var(--ds-gray-800, #1e293b);
}

.ec-qb-question-surface__editor-hint {
  flex: 1;
  font-size: 0.75rem;
  color: var(--ds-text-muted);
}

.ec-qb-question-surface__cancel-btn {
  padding: 0.25rem 0.75rem;
  border: 1px solid var(--ds-border);
  border-radius: var(--ds-radius-md);
  background: transparent;
  color: var(--ds-text-secondary);
  font-size: 0.8125rem;
  cursor: pointer;
  transition: background 0.15s, color 0.15s;
}

.ec-qb-question-surface__cancel-btn:hover {
  background: var(--ds-gray-100);
  color: var(--ds-text);
}

.dark .ec-qb-question-surface__cancel-btn {
  border-color: var(--ds-border-strong);
}

.dark .ec-qb-question-surface__cancel-btn:hover {
  background: var(--ds-gray-700);
  color: var(--ds-text);
}

.ec-qb-question-surface__save-btn {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  padding: 0.25rem 0.75rem;
  border: none;
  border-radius: var(--ds-radius-md);
  background: var(--ds-primary);
  color: #fff;
  font-size: 0.8125rem;
  cursor: pointer;
  transition: background 0.15s;
}

.ec-qb-question-surface__save-btn:hover {
  background: var(--ds-primary-hover, #1d4ed8);
}

/* Option card: input + preview bên dưới */
.ec-qb-option-card__content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 0.375rem;
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

.ec-qb-sec-chip {
  max-width: 14rem;
  padding: 0.125rem 0.45rem;
  border-radius: var(--ds-radius-md);
  font-size: 0.65rem;
  font-weight: 600;
  color: var(--ds-text-muted);
  background: var(--ds-surface-2, rgba(0, 0, 0, 0.04));
  border: 1px solid var(--ds-border);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.dark .ec-qb-sec-chip {
  background: rgba(255, 255, 255, 0.06);
  border-color: var(--ds-border-strong);
}

.ec-qb-part-divider {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  margin: 1rem 0 0.5rem;
  padding: 0 0.25rem;
}

.ec-qb-part-divider:first-child {
  margin-top: 0;
}

.ec-qb-part-divider__line {
  flex: 1;
  height: 1px;
  background: var(--ds-border);
  opacity: 0.85;
}

.ec-qb-part-divider__label {
  font-size: 0.72rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.06em;
  color: var(--ds-text-muted);
  white-space: nowrap;
  max-width: min(90vw, 28rem);
  overflow: hidden;
  text-overflow: ellipsis;
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

.ec-qb-options-grid--image-review {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.75rem;
}

.ec-qb-option-card {
  display: flex;
  align-items: center;
  gap: 0.625rem;
  width: 100%;
  padding: 0.625rem 0.75rem;
  border: 1.5px solid var(--ds-border);
  border-radius: var(--ds-radius-lg);
  background: var(--ds-gray-50);
  cursor: pointer;
  appearance: none;
  text-align: left;
  font: inherit;
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

.ec-qb-option-card:focus-visible {
  outline: none;
  border-color: var(--ds-primary);
  box-shadow: 0 0 0 3px var(--ds-primary-ring);
}

.ec-qb-option-card--correct {
  border-color: var(--ds-success);
  background: var(--ds-success-soft);
}

.ec-qb-option-card--image-review {
  min-height: 76px;
  align-items: center;
}

.ec-qb-option-card--disabled {
  opacity: 0.72;
}

.ec-qb-option-card--disabled:hover {
  border-color: var(--ds-border);
  background: var(--ds-gray-50);
}

.dark .ec-qb-option-card--disabled:hover {
  border-color: var(--ds-border-strong);
  background: var(--ds-gray-800);
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

.ec-qb-option-card__image-choice {
  display: flex;
  align-items: center;
  min-height: 32px;
}

.ec-qb-option-card__image-choice-text {
  font-size: 0.92rem;
  font-weight: 700;
  color: var(--ds-text);
}

.dark .ec-qb-option-card__image-choice-text {
  color: var(--ds-text);
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

@media (max-width: 720px) {
  .ec-qb-options-grid--image-review {
    grid-template-columns: 1fr;
  }
}
</style>
