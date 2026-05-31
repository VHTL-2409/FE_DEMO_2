<template>
  <section class="ipw">
    <!-- ── Header: stats summary ── -->
    <div class="ipw__header">
      <div class="ipw__header-left">
        <div class="ipw__header-icon">
          <LucideIcon name="quiz" />
        </div>
        <div>
          <h3 class="ipw__header-title">Câu hỏi đã parse</h3>
          <p class="ipw__header-sub">
            {{ questions.length }} câu
            <span v-if="summary?.needsReview" class="ipw__review-badge">
              · {{ summary.needsReview }} cần review
            </span>
          </p>
        </div>
      </div>

      <div class="ipw__header-meta">
        <span v-if="summary?.parseMethod" class="ipw__meta-chip">
          <LucideIcon name="settings" size="13" />
          {{ summary.parseMethod }}
        </span>
        <span v-if="summary?.fileType" class="ipw__meta-chip">
          <LucideIcon name="file" size="13" />
          {{ summary.fileType }}
        </span>
        <span v-if="totalScore > 0" class="ipw__meta-chip ipw__meta-chip--score">
          <LucideIcon name="grade" size="13" />
          {{ totalScore }} điểm
        </span>
      </div>
    </div>

    <!-- ── Quick stats bar ── -->
    <div v-if="questions.length > 0" class="ipw__stats-bar">
      <div class="ipw__stat">
        <span class="ipw__stat-dot ipw__stat-dot--single" />
        <span class="ipw__stat-label">Trắc nghiệm</span>
        <span class="ipw__stat-count">{{ typeCount('SINGLE_CHOICE') }}</span>
      </div>
      <div class="ipw__stat">
        <span class="ipw__stat-dot ipw__stat-dot--multi" />
        <span class="ipw__stat-label">Nhiều đáp án</span>
        <span class="ipw__stat-count">{{ typeCount('MULTIPLE_CHOICE') }}</span>
      </div>
      <div class="ipw__stat">
        <span class="ipw__stat-dot ipw__stat-dot--essay" />
        <span class="ipw__stat-label">Tự luận</span>
        <span class="ipw__stat-count">{{ typeCount('ESSAY') }}</span>
      </div>
      <div class="ipw__stat">
        <span class="ipw__stat-dot ipw__stat-dot--fill" />
        <span class="ipw__stat-label">Điền trống</span>
        <span class="ipw__stat-count">{{ typeCount('FILL_IN_BLANK') }}</span>
      </div>
      <div v-if="questions.length > 3" class="ipw__stat ipw__stat--mlauto">
        <button
          type="button"
          class="ipw__expand-all-btn"
          @click="allExpanded = !allExpanded"
        >
          <LucideIcon :name="allExpanded ? 'expand_less' : 'expand_more'" size="15" />
          {{ allExpanded ? 'Thu gọn tất cả' : 'Mở rộng tất cả' }}
        </button>
      </div>
    </div>

    <!-- ── Empty state ── -->
    <div v-if="!questions.length" class="ipw__empty">
      <div class="ipw__empty-icon">
        <LucideIcon name="quiz" />
      </div>
      <p class="ipw__empty-title">Chưa có câu hỏi nào</p>
      <p class="ipw__empty-sub">Nhập file để xem trước và chỉnh sửa câu hỏi.</p>
    </div>

    <!-- ── Question list ── -->
    <div v-else class="ipw__list portal-scrollbar">
      <article
        v-for="question in questions"
        :key="question.index"
        class="ipw__card"
        :class="{
          'ipw__card--expanded': expandedIds.has(question.index),
          'ipw__card--low-conf': (question.parseConfidence || 0) < 0.7
        }"
      >
        <!-- Card header: always visible -->
        <button
          type="button"
          class="ipw__card-header"
          :disabled="disabled"
          @click="toggleExpand(question.index)"
        >
          <div class="ipw__card-left">
            <!-- Question number -->
            <div class="ipw__qnum">
              <LucideIcon name="quiz" size="14" />
              {{ question.index }}
            </div>

            <!-- Type badge -->
            <span class="ipw__type-badge" :class="`ipw__type-badge--${typeSlug(question.type)}`">
              {{ typeLabel(question.type) }}
            </span>

            <!-- Low confidence warning -->
            <span v-if="(question.parseConfidence || 0) < 0.7" class="ipw__conf-warn">
              <LucideIcon name="warning" size="13" />
              confidence thấp
            </span>

            <!-- Render mode badge -->
            <span v-if="question.renderMode" class="ipw__render-badge"
              :class="`ipw__render-badge--${question.renderMode?.toLowerCase()}`">
              <LucideIcon :name="question.renderMode === 'IMAGE' ? 'image' : 'text_fields'" size="11" />
              {{ question.renderMode === 'IMAGE' ? 'Ảnh' : 'Text' }}
            </span>

            <!-- Has issues badge -->
            <span v-if="question.issues?.length" class="ipw__issues-badge">
              <LucideIcon name="warning" size="11" />
              {{ question.issues.length }}
            </span>
          </div>

          <div class="ipw__card-right">
            <!-- Score -->
            <span class="ipw__score-chip">
              {{ Number(question.scoreWeight || 1).toFixed(1) }}đ
            </span>

            <!-- Difficulty -->
            <span
              v-if="question.difficulty"
              class="ipw__diff-badge"
              :class="`ipw__diff-badge--${question.difficulty?.toLowerCase()}`"
            >
              {{ question.difficulty }}
            </span>

            <!-- Correct answer preview (for single choice) -->
            <span v-if="usesOptions(question.type) && question.correctAnswer" class="ipw__answer-preview">
              <LucideIcon name="check" size="13" />
              Đáp án: {{ question.correctAnswer }}
            </span>

            <!-- Expand toggle -->
            <LucideIcon
              :name="expandedIds.has(question.index) ? 'expand_less' : 'expand_more'"
              size="18"
              class="ipw__expand-icon"
            />
          </div>
        </button>

        <!-- Card body: editable (shown when expanded) -->
        <div v-if="expandedIds.has(question.index)" class="ipw__card-body">

          <!-- Image toggle bar (show for IMAGE mode or when user wants to toggle) -->
          <div v-if="question.render?.imagePath || canToggleRenderMode(question)" class="ipw__render-toggle">
            <button
              type="button"
              class="ipw__render-toggle-btn"
              :class="{ 'ipw__render-toggle-btn--active': activeRenderMode(question.index) === 'text' }"
              @click="setRenderMode(question.index, 'text')"
            >
              <LucideIcon name="text_fields" size="13" />
              Text
            </button>
            <button
              v-if="question.render?.imagePath"
              type="button"
              class="ipw__render-toggle-btn"
              :class="{ 'ipw__render-toggle-btn--active': activeRenderMode(question.index) === 'image' }"
              @click="setRenderMode(question.index, 'image')"
            >
              <LucideIcon name="image" size="13" />
              Ảnh
            </button>
          </div>

          <!-- Image mode -->
          <div v-if="activeRenderMode(question.index) === 'image' && question.render?.imagePath" class="ipw__image-viewer">
            <img
              :src="questionImageUrl(question)"
              :alt="`Câu ${question.index}`"
              class="ipw__cropped-img"
              @error="onImageError(question.index, $event)"
            />
          </div>

          <!-- Text mode (question content) -->
          <div v-else class="ipw__field">
            <label class="ipw__field-label">
              <LucideIcon name="text_fields" size="14" />
              Nội dung câu hỏi
              <span class="ipw__field-required">*</span>
            </label>
            <textarea
              :value="question.content"
              :disabled="disabled"
              rows="3"
              class="ipw__textarea"
              placeholder="Nhập nội dung câu hỏi..."
              @input="updateQuestion(question.index, { content: $event.target.value })"
            />
          </div>

          <!-- Options (for choice types) -->
          <div v-if="usesOptions(question.type)" class="ipw__options">
            <label
              v-for="opt in question.options || []"
              :key="`${question.index}-${opt.id}`"
              class="ipw__opt-row"
              :class="{ 'ipw__opt-row--correct': opt.id === question.correctAnswer }"
            >
              <div class="ipw__opt-badge" :class="{ 'ipw__opt-badge--correct': opt.id === question.correctAnswer }">
                {{ opt.id }}
              </div>
              <input
                :value="opt.text"
                :disabled="disabled"
                type="text"
                class="ipw__opt-input"
                :placeholder="`Đáp án ${opt.id}`"
                @input="updateOption(question.index, opt.id, $event.target.value)"
              />
            </label>
          </div>

          <!-- Essay: show correct answer as text -->
          <div v-else-if="question.type === 'ESSAY'" class="ipw__field">
            <label class="ipw__field-label">
              <LucideIcon name="check" size="14" />
              Đáp án mẫu (tự luận)
            </label>
            <textarea
              :value="question.correctAnswer"
              :disabled="disabled"
              rows="2"
              class="ipw__textarea"
              placeholder="Gợi ý đáp án cho giáo viên chấm điểm..."
              @input="updateQuestion(question.index, { correctAnswer: $event.target.value })"
            />
          </div>

          <!-- Meta row -->
          <div class="ipw__meta-row">
            <div class="ipw__field ipw__field--inline">
              <label class="ipw__field-label ipw__field-label--sm">Đáp án đúng</label>
              <div class="ipw__correct-group">
                <button
                  v-for="opt in ['A','B','C','D']"
                  :key="opt"
                  type="button"
                  class="ipw__correct-btn"
                  :class="{ 'ipw__correct-btn--active': question.correctAnswer === opt }"
                  :disabled="disabled || !usesOptions(question.type)"
                  @click="updateQuestion(question.index, { correctAnswer: opt })"
                >
                  {{ opt }}
                </button>
              </div>
            </div>

            <div class="ipw__field ipw__field--inline">
              <label class="ipw__field-label ipw__field-label--sm">Điểm</label>
              <input
                :value="question.scoreWeight"
                :disabled="disabled"
                type="number"
                min="0.1"
                step="0.1"
                class="ipw__input ipw__input--sm"
                @input="updateQuestion(question.index, { scoreWeight: Number($event.target.value) || 1 })"
              />
            </div>

            <div class="ipw__field ipw__field--inline">
              <label class="ipw__field-label ipw__field-label--sm">Độ khó</label>
              <select
                :value="question.difficulty || 'MEDIUM'"
                :disabled="disabled"
                class="ipw__select ipw__select--sm"
                @change="updateQuestion(question.index, { difficulty: $event.target.value })"
              >
                <option value="EASY">EASY</option>
                <option value="MEDIUM">MEDIUM</option>
                <option value="HARD">HARD</option>
              </select>
            </div>

            <div class="ipw__field ipw__field--inline">
              <label class="ipw__field-label ipw__field-label--sm">Loại</label>
              <select
                :value="question.type || 'SINGLE_CHOICE'"
                :disabled="disabled"
                class="ipw__select ipw__select--sm"
                @change="updateQuestion(question.index, { type: $event.target.value })"
              >
                <option value="SINGLE_CHOICE">Trắc nghiệm</option>
                <option value="MULTIPLE_CHOICE">Nhiều đáp án</option>
                <option value="FILL_IN_BLANK">Điền trống</option>
                <option value="ESSAY">Tự luận</option>
                <option value="CODE">Code</option>
              </select>
            </div>

            <!-- Confidence -->
            <div class="ipw__conf-display">
              <LucideIcon name="analytics" size="13" />
              <span>{{ Number(question.parseConfidence || 0).toFixed(2) }}</span>
            </div>
          </div>

          <!-- Explanation (from solution section) -->
          <div v-if="question.explanation" class="ipw__field">
            <label class="ipw__field-label">
              <LucideIcon name="lightbulb" size="14" />
              Lời giải
            </label>
            <div class="ipw__explanation-box">{{ question.explanation }}</div>
          </div>

          <!-- Issues list -->
          <div v-if="question.issues?.length" class="ipw__issues-list">
            <div class="ipw__issues-title">
              <LucideIcon name="warning" size="13" />
              Cảnh báo
            </div>
            <span
              v-for="(issue, iIdx) in question.issues"
              :key="iIdx"
              class="ipw__issue-pill"
            >
              {{ issue }}
            </span>
          </div>
        </div>
      </article>
    </div>
  </section>
</template>

<script setup>
import { ref, computed } from 'vue'

const props = defineProps({
  summary: { type: Object, default: () => ({}) },
  questions: { type: Array, default: () => [] },
  disabled: { type: Boolean, default: false },
  sessionId: { type: [Number, String], default: null }
})

const emit = defineEmits(['update-question'])

const expandedIds = ref(new Set([1, 2, 3])) // expand first 3 by default
const allExpanded = ref(false)

// Track active render mode per question (TEXT | IMAGE)
const renderModeOverrides = ref({})

// Image error tracking
const imageErrors = ref({})

const questionImageUrl = (question) => {
  if (!question.render?.imagePath) return ''
  // Backend serves image via /api/v1/exam-import/image/{sessionId}/{questionIndex}
  const sid = props.sessionId || question.sessionId || ''
  return `/api/v1/exam-import/image/${sid}/${question.index}`
}

const activeRenderMode = (index) => {
  if (renderModeOverrides.value[index] !== undefined) {
    return renderModeOverrides.value[index]
  }
  const q = props.questions.find(q => q.index === index)
  return q?.renderMode === 'IMAGE' ? 'image' : 'text'
}

const setRenderMode = (index, mode) => {
  renderModeOverrides.value = { ...renderModeOverrides.value, [index]: mode }
}

const canToggleRenderMode = (question) => {
  return question.renderMode === 'IMAGE' || question.render?.imagePath
}

const onImageError = (index, event) => {
  imageErrors.value = { ...imageErrors.value, [index]: true }
  console.warn(`[ImportPreviewWorkbench] Failed to load image for question ${index}`)
}

const toggleAll = () => {
  if (allExpanded.value) {
    expandedIds.value = new Set(props.questions.map(q => q.index))
  } else {
    expandedIds.value = new Set()
  }
}

const toggleExpand = (index) => {
  const next = new Set(expandedIds.value)
  if (next.has(index)) {
    next.delete(index)
  } else {
    next.add(index)
  }
  expandedIds.value = next
}

const totalScore = computed(() =>
  props.questions.reduce((sum, q) => sum + Number(q.scoreWeight || q.score || 1), 0)
)

const typeCount = (type) =>
  props.questions.filter(q => q.type === type).length

const typeSlug = (type) => {
  const m = {
    SINGLE_CHOICE: 'single',
    MULTIPLE_CHOICE: 'multi',
    FILL_IN_BLANK: 'fill',
    ESSAY: 'essay',
    CODE: 'code'
  }
  return m[String(type || '').toUpperCase()] || 'single'
}

const typeLabel = (type) => {
  const m = {
    SINGLE_CHOICE: 'Trắc nghiệm',
    MULTIPLE_CHOICE: 'Nhiều đáp án',
    FILL_IN_BLANK: 'Điền trống',
    ESSAY: 'Tự luận',
    CODE: 'Code'
  }
  return m[String(type || '').toUpperCase()] || 'Trắc nghiệm'
}

const usesOptions = (type) =>
  ['SINGLE_CHOICE', 'MULTIPLE_CHOICE', 'MATCHING', 'ORDERING'].includes(String(type || 'SINGLE_CHOICE').toUpperCase())

const updateQuestion = (questionIndex, patch) => {
  emit('update-question', { questionIndex, patch })
}

const updateOption = (questionIndex, optionId, value) => {
  const question = props.questions.find(q => q.index === questionIndex)
  if (!question) return
  const nextOptions = (question.options || []).map(opt =>
    opt.id === optionId ? { ...opt, text: value } : opt
  )
  emit('update-question', { questionIndex, patch: { options: nextOptions } })
}

defineExpose({ toggleAll })
</script>


<style scoped>
/* ── Container ── */
.ipw {
  display: flex;
  flex-direction: column;
  gap: 0;
  height: 100%;
  overflow: hidden;
  border-radius: var(--ds-radius-2xl, 1.5rem);
  border: 1px solid var(--ds-border);
  background: var(--ds-surface);
}

.dark .ipw {
  border-color: var(--ds-border-strong);
}

/* ── Header ── */
.ipw__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  padding: 1rem 1.25rem;
  border-bottom: 1px solid var(--ds-border);
  background: linear-gradient(135deg, var(--ds-primary-soft) 0%, transparent 80%);
  flex-shrink: 0;
}

.dark .ipw__header {
  border-bottom-color: var(--ds-border-strong);
}

.ipw__header-left {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.ipw__header-icon {
  width: 36px;
  height: 36px;
  border-radius: var(--ds-radius-lg);
  background: var(--ds-primary);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.ipw__header-title {
  font-family: var(--ds-font-display);
  font-size: 0.95rem;
  font-weight: 700;
  color: var(--ds-text);
  margin: 0;
}

.dark .ipw__header-title { color: #f1f5f9; }

.ipw__header-sub {
  font-size: 0.72rem;
  color: var(--ds-text-muted);
  margin: 0.2rem 0 0;
}

.ipw__review-badge {
  color: var(--ds-warning);
}

.ipw__header-meta {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.ipw__meta-chip {
  display: inline-flex;
  align-items: center;
  gap: 0.3rem;
  padding: 0.25rem 0.625rem;
  border-radius: var(--ds-radius-full, 9999px);
  background: var(--ds-gray-100);
  color: var(--ds-text-muted);
  font-size: 0.7rem;
  font-weight: 600;
  border: 1px solid var(--ds-border);
}

.dark .ipw__meta-chip { background: var(--ds-gray-700); border-color: var(--ds-border-strong); color: #94a3b8; }

.ipw__meta-chip--score {
  background: var(--ds-primary-soft);
  color: var(--ds-primary);
  border-color: var(--ds-primary-border);
  font-variant-numeric: tabular-nums;
}

/* ── Stats bar ── */
.ipw__stats-bar {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  padding: 0.5rem 1.25rem;
  background: var(--ds-gray-50);
  border-bottom: 1px solid var(--ds-border);
  flex-shrink: 0;
  flex-wrap: wrap;
}

.dark .ipw__stats-bar { background: var(--ds-gray-800); border-bottom-color: var(--ds-border-strong); }

.ipw__stat {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  padding: 0.2rem 0.5rem;
  border-radius: var(--ds-radius-md);
  font-size: 0.72rem;
  font-weight: 600;
  color: var(--ds-text-secondary);
}

.ipw__stat--mlauto { margin-left: auto; }

.ipw__stat-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}

.ipw__stat-dot--single { background: var(--ds-primary); }
.ipw__stat-dot--multi { background: var(--ds-info); }
.ipw__stat-dot--essay { background: var(--ds-warning); }
.ipw__stat-dot--fill { background: var(--ds-success); }

.ipw__stat-count {
  font-weight: 700;
  color: var(--ds-text);
  font-variant-numeric: tabular-nums;
  min-width: 18px;
  text-align: right;
}

.dark .ipw__stat-count { color: #f1f5f9; }

.ipw__expand-all-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  padding: 0.25rem 0.625rem;
  border-radius: var(--ds-radius-md);
  border: 1px solid var(--ds-border);
  background: var(--ds-surface);
  color: var(--ds-text-muted);
  font-size: 0.7rem;
  font-weight: 600;
  cursor: pointer;
  transition: color 0.12s ease, background-color 0.12s ease, border-color 0.12s ease, box-shadow 0.12s ease, transform 0.12s ease;
}

.dark .ipw__expand-all-btn { background: var(--ds-gray-700); border-color: var(--ds-border-strong); color: #94a3b8; }

.ipw__expand-all-btn:hover { border-color: var(--ds-primary); color: var(--ds-primary); background: var(--ds-primary-soft); }

/* ── Empty ── */
.ipw__empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.5rem;
  padding: 4rem 2rem;
  text-align: center;
}

.ipw__empty-icon {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background: var(--ds-gray-100);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--ds-text-muted);
  opacity: 0.5;
  margin-bottom: 0.5rem;
}

.dark .ipw__empty-icon { background: var(--ds-gray-700); }

.ipw__empty-title {
  font-size: 0.95rem;
  font-weight: 700;
  color: var(--ds-text);
  margin: 0;
}

.dark .ipw__empty-title { color: #f1f5f9; }

.ipw__empty-sub {
  font-size: 0.8rem;
  color: var(--ds-text-muted);
  margin: 0;
}

/* ── List ── */
.ipw__list {
  flex: 1;
  overflow-y: auto;
  overscroll-contain;
  padding: 0.75rem;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

/* ── Card ── */
.ipw__card {
  border: 1.5px solid var(--ds-border);
  border-radius: var(--ds-radius-xl, 1.125rem);
  background: var(--ds-surface);
  overflow: hidden;
  transition: border-color 0.15s ease, box-shadow 0.15s ease;
}

.dark .ipw__card { border-color: var(--ds-border-strong); }

.ipw__card--expanded {
  border-color: var(--ds-primary-border);
  box-shadow: 0 0 0 3px var(--ds-primary-ring);
}

.ipw__card--low-conf {
  border-left: 3px solid var(--ds-warning);
}

/* Card header (always visible, acts as toggle) */
.ipw__card-header {
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

.ipw__card-header:hover:not(:disabled) { background: var(--ds-gray-50); }
.ipw__card-header:disabled { cursor: default; opacity: 0.7; }

.dark .ipw__card-header:hover:not(:disabled) { background: var(--ds-gray-800); }

.ipw__card-left {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  min-width: 0;
  flex: 1;
}

.ipw__card-right {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex-shrink: 0;
}

/* Question number */
.ipw__qnum {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  font-size: 0.72rem;
  font-weight: 800;
  color: var(--ds-primary);
  background: var(--ds-primary-soft);
  padding: 0.2rem 0.5rem;
  border-radius: var(--ds-radius-md);
  flex-shrink: 0;
}

/* Type badge */
.ipw__type-badge {
  padding: 0.2rem 0.5rem;
  border-radius: var(--ds-radius-full);
  font-size: 0.68rem;
  font-weight: 700;
  flex-shrink: 0;
}

.ipw__type-badge--single { background: var(--ds-primary-soft); color: var(--ds-primary); }
.ipw__type-badge--multi { background: var(--ds-info-soft); color: var(--ds-info); }
.ipw__type-badge--essay { background: rgba(217, 119, 6, 0.1); color: #d97706; }
.ipw__type-badge--fill { background: var(--ds-success-soft); color: var(--ds-success); }
.ipw__type-badge--code { background: rgba(139, 92, 246, 0.1); color: #7c3aed; }

/* Confidence warning */
.ipw__conf-warn {
  display: flex;
  align-items: center;
  gap: 0.2rem;
  font-size: 0.65rem;
  font-weight: 600;
  color: var(--ds-warning);
  background: rgba(217, 119, 6, 0.08);
  padding: 0.15rem 0.4rem;
  border-radius: var(--ds-radius-full);
}

/* Score chip */
.ipw__score-chip {
  font-size: 0.72rem;
  font-weight: 700;
  color: var(--ds-success);
  background: var(--ds-success-soft);
  padding: 0.15rem 0.45rem;
  border-radius: var(--ds-radius-full);
  font-variant-numeric: tabular-nums;
}

/* Difficulty badge */
.ipw__diff-badge {
  font-size: 0.65rem;
  font-weight: 700;
  padding: 0.15rem 0.4rem;
  border-radius: var(--ds-radius-full);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.ipw__diff-badge--easy { background: var(--ds-success-soft); color: var(--ds-success); }
.ipw__diff-badge--medium { background: rgba(217, 119, 6, 0.08); color: #d97706; }
.ipw__diff-badge--hard { background: var(--ds-danger-soft); color: var(--ds-danger); }

/* Answer preview */
.ipw__answer-preview {
  display: flex;
  align-items: center;
  gap: 0.2rem;
  font-size: 0.68rem;
  font-weight: 600;
  color: var(--ds-text-muted);
}

.ipw__expand-icon {
  color: var(--ds-text-muted);
  transition: transform 0.15s ease;
}

/* Card body */
.ipw__card-body {
  padding: 0 1rem 1rem;
  display: flex;
  flex-direction: column;
  gap: 0.875rem;
  border-top: 1px solid var(--ds-border);
  margin-top: 0;
  padding-top: 0.875rem;
  animation: ipw-slide-down 0.2s ease-out;
}

.dark .ipw__card-body { border-top-color: var(--ds-border-strong); }

@keyframes ipw-slide-down {
  from { opacity: 0; transform: translateY(-6px); }
  to { opacity: 1; transform: translateY(0); }
}

/* Field */
.ipw__field {
  display: flex;
  flex-direction: column;
  gap: 0.375rem;
}

.ipw__field--inline {
  flex-direction: row;
  align-items: center;
  gap: 0.5rem;
}

.ipw__field-label {
  display: flex;
  align-items: center;
  gap: 0.35rem;
  font-size: 0.72rem;
  font-weight: 700;
  color: var(--ds-text-secondary);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.ipw__field-label--sm {
  font-size: 0.7rem;
  white-space: nowrap;
  text-transform: none;
  letter-spacing: 0;
}

.ipw__field-required { color: var(--ds-danger); }

/* Textarea */
.ipw__textarea {
  padding: 0.625rem 0.875rem;
  border: 1.5px solid var(--ds-border);
  border-radius: var(--ds-radius-xl, 1rem);
  background: var(--ds-gray-50);
  color: var(--ds-text);
  font-size: 0.85rem;
  font-family: inherit;
  line-height: 1.5;
  outline: none;
  transition: border-color 0.12s ease, box-shadow 0.12s ease;
  resize: vertical;
  min-height: 64px;
}

.dark .ipw__textarea { background: var(--ds-gray-800); border-color: var(--ds-border-strong); color: #f1f5f9; }

.ipw__textarea:focus {
  border-color: var(--ds-primary);
  box-shadow: 0 0 0 3px var(--ds-primary-ring);
  background: var(--ds-surface);
}

.ipw__textarea:disabled { opacity: 0.6; cursor: not-allowed; }

/* Options */
.ipw__options {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0.5rem;
}

@media (max-width: 480px) {
  .ipw__options { grid-template-columns: 1fr; }
}

.ipw__opt-row {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.375rem 0.625rem;
  border: 1.5px solid var(--ds-border);
  border-radius: var(--ds-radius-lg);
  background: var(--ds-gray-50);
  transition: border-color 0.12s ease;
}

.dark .ipw__opt-row { background: var(--ds-gray-800); border-color: var(--ds-border-strong); }

.ipw__opt-row--correct {
  border-color: var(--ds-success);
  background: var(--ds-success-soft);
}

.dark .ipw__opt-row--correct { background: rgba(22, 163, 74, 0.08); }

.ipw__opt-badge {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: var(--ds-gray-200);
  color: var(--ds-text-muted);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.65rem;
  font-weight: 800;
  flex-shrink: 0;
  transition: color 0.12s ease, background-color 0.12s ease, border-color 0.12s ease, box-shadow 0.12s ease, transform 0.12s ease;
}

.dark .ipw__opt-badge { background: var(--ds-gray-700); }

.ipw__opt-badge--correct {
  background: var(--ds-success);
  color: white;
}

.ipw__opt-input {
  flex: 1;
  border: none;
  background: transparent;
  color: var(--ds-text);
  font-size: 0.82rem;
  outline: none;
  min-width: 0;
}

.dark .ipw__opt-input { color: #f1f5f9; }
.ipw__opt-input::placeholder { color: var(--ds-text-muted); }
.ipw__opt-input:disabled { opacity: 0.6; cursor: not-allowed; }

/* Meta row */
.ipw__meta-row {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex-wrap: wrap;
}

/* Correct answer buttons */
.ipw__correct-group {
  display: flex;
  gap: 0.25rem;
}

.ipw__correct-btn {
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
  transition: color 0.12s ease, background-color 0.12s ease, border-color 0.12s ease, box-shadow 0.12s ease, transform 0.12s ease;
}

.dark .ipw__correct-btn { background: var(--ds-gray-800); border-color: var(--ds-border-strong); }

.ipw__correct-btn--active {
  background: var(--ds-success);
  border-color: var(--ds-success);
  color: white;
}

.ipw__correct-btn:hover:not(:disabled):not(.ipw__correct-btn--active) {
  border-color: var(--ds-success);
  color: var(--ds-success);
}

.ipw__correct-btn:disabled { opacity: 0.4; cursor: not-allowed; }

/* Input / select */
.ipw__input {
  padding: 0.375rem 0.625rem;
  border: 1.5px solid var(--ds-border);
  border-radius: var(--ds-radius-lg);
  background: var(--ds-gray-50);
  color: var(--ds-text);
  font-size: 0.8rem;
  font-weight: 600;
  outline: none;
  transition: border-color 0.12s ease;
  font-family: inherit;
}

.dark .ipw__input { background: var(--ds-gray-800); border-color: var(--ds-border-strong); color: #f1f5f9; }

.ipw__input:focus { border-color: var(--ds-primary); }
.ipw__input:disabled { opacity: 0.6; cursor: not-allowed; }
.ipw__input--sm { width: 64px; text-align: center; font-variant-numeric: tabular-nums; }

.ipw__select {
  padding: 0.375rem 0.625rem;
  border: 1.5px solid var(--ds-border);
  border-radius: var(--ds-radius-lg);
  background: var(--ds-gray-50);
  color: var(--ds-text);
  font-size: 0.8rem;
  font-weight: 600;
  outline: none;
  cursor: pointer;
  transition: border-color 0.12s ease;
  font-family: inherit;
}

.dark .ipw__select { background: var(--ds-gray-800); border-color: var(--ds-border-strong); color: #f1f5f9; }
.ipw__select:focus { border-color: var(--ds-primary); }
.ipw__select:disabled { opacity: 0.6; cursor: not-allowed; }
.ipw__select--sm { padding: 0.25rem 0.5rem; font-size: 0.75rem; }

/* Confidence display */
.ipw__conf-display {
  display: flex;
  align-items: center;
  gap: 0.3rem;
  font-size: 0.7rem;
  color: var(--ds-text-muted);
  font-variant-numeric: tabular-nums;
  margin-left: auto;
}

/* Render mode badge */
.ipw__render-badge {
  display: inline-flex;
  align-items: center;
  gap: 0.2rem;
  font-size: 0.62rem;
  font-weight: 700;
  padding: 0.12rem 0.35rem;
  border-radius: var(--ds-radius-full);
  flex-shrink: 0;
}

.ipw__render-badge--image {
  background: rgba(139, 92, 246, 0.12);
  color: #7c3aed;
}

.ipw__render-badge--text {
  background: var(--ds-gray-100);
  color: var(--ds-text-muted);
}

.dark .ipw__render-badge--text { background: var(--ds-gray-700); }

/* Issues badge */
.ipw__issues-badge {
  display: inline-flex;
  align-items: center;
  gap: 0.2rem;
  font-size: 0.62rem;
  font-weight: 700;
  padding: 0.12rem 0.35rem;
  border-radius: var(--ds-radius-full);
  background: rgba(220, 38, 38, 0.1);
  color: var(--ds-danger);
  flex-shrink: 0;
}

/* Render toggle bar */
.ipw__render-toggle {
  display: flex;
  gap: 0.25rem;
  margin-bottom: 0.25rem;
}

.ipw__render-toggle-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.3rem;
  padding: 0.3rem 0.75rem;
  border-radius: var(--ds-radius-lg);
  border: 1.5px solid var(--ds-border);
  background: var(--ds-gray-50);
  color: var(--ds-text-muted);
  font-size: 0.72rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.12s ease;
}

.dark .ipw__render-toggle-btn { background: var(--ds-gray-700); border-color: var(--ds-border-strong); color: #94a3b8; }

.ipw__render-toggle-btn--active {
  border-color: var(--ds-primary);
  background: var(--ds-primary-soft);
  color: var(--ds-primary);
}

.dark .ipw__render-toggle-btn--active { background: rgba(59, 130, 246, 0.12); }

/* Image viewer */
.ipw__image-viewer {
  border-radius: var(--ds-radius-xl);
  overflow: hidden;
  border: 1.5px solid var(--ds-border);
  background: var(--ds-gray-50);
}

.dark .ipw__image-viewer { background: var(--ds-gray-800); border-color: var(--ds-border-strong); }

.ipw__cropped-img {
  width: 100%;
  max-height: 480px;
  object-fit: contain;
  display: block;
}

/* Explanation box */
.ipw__explanation-box {
  padding: 0.625rem 0.875rem;
  border-radius: var(--ds-radius-xl);
  background: rgba(139, 92, 246, 0.06);
  border: 1.5px solid rgba(139, 92, 246, 0.2);
  color: var(--ds-text-secondary);
  font-size: 0.82rem;
  line-height: 1.5;
  font-style: italic;
}

.dark .ipw__explanation-box { background: rgba(139, 92, 246, 0.08); color: #c4b5fd; }

/* Issues list */
.ipw__issues-list {
  display: flex;
  flex-direction: column;
  gap: 0.375rem;
}

.ipw__issues-title {
  display: flex;
  align-items: center;
  gap: 0.3rem;
  font-size: 0.72rem;
  font-weight: 700;
  color: var(--ds-danger);
}

.ipw__issue-pill {
  display: inline-flex;
  align-items: center;
  padding: 0.2rem 0.5rem;
  border-radius: var(--ds-radius-full);
  background: rgba(220, 38, 38, 0.06);
  border: 1px solid rgba(220, 38, 38, 0.15);
  color: var(--ds-danger);
  font-size: 0.7rem;
  font-weight: 500;
  width: fit-content;
}

.dark .ipw__issue-pill { background: rgba(220, 38, 38, 0.1); }
</style>
