<template>
  <div class="qn">
    <!-- Progress summary card -->
    <div class="qn__summary">
      <div class="qn__summary-header">
        <LucideIcon name="trending_up" />
        <span class="qn__summary-title">Tiến độ làm bài</span>
        <span class="qn__summary-pct">{{ progressPercent }}%</span>
      </div>
      <div class="qn__progress-bar">
        <div class="qn__progress-fill" :style="{ width: `${progressPercent}%` }" />
      </div>
      <div class="qn__summary-stats">
        <span class="qn__stat-item">
          <span class="qn__stat-dot qn__stat-dot--done" />
          Da lam: {{ answeredCount }}
        </span>
        <span class="qn__stat-item">
          <span class="qn__stat-dot qn__stat-dot--undone" />
          Chưa làm: {{ unansweredCount }}
        </span>
      </div>
      <div class="qn__summary-tags">
        <span v-if="markedCount > 0" class="qn__tag qn__tag--marked">
          <LucideIcon name="bookmark" />
          Danh dấu: {{ markedCount }}
        </span>
        <span v-if="skippedCount > 0" class="qn__tag qn__tag--skip">
          <LucideIcon name="skip_next" />
          Bỏ qua: {{ skippedCount }}
        </span>
      </div>
    </div>

    <!-- Question grid -->
    <div class="qn__grid-card">
      <div class="qn__grid-header">
        <LucideIcon name="list" />
        <span class="qn__grid-title">Danh sách câu hỏi</span>
        <span class="qn__grid-count">{{ questions.length }} câu</span>
      </div>

      <div class="qn__grid">
        <button
          v-for="(question, idx) in questions"
          :key="question.id"
          type="button"
          class="qn__btn"
          :class="questionButtonClass(question, idx)"
          :title="`Cau ${idx + 1}`"
          :aria-label="`Cau ${idx + 1}: ${questionLabel(question, idx)}`"
          @click="$emit('select', idx)"
        >
          {{ idx + 1 }}
        </button>
      </div>

      <!-- Legend -->
      <div class="qn__legend">
        <span class="qn__legend-item">
          <span class="qn__legend-dot qn__legend-dot--done" />
          Đã làm
        </span>
        <span class="qn__legend-item">
          <span class="qn__legend-dot qn__legend-dot--marked" />
          Đánh dấu
        </span>
        <span class="qn__legend-item">
          <span class="qn__legend-dot qn__legend-dot--skip" />
          Bỏ qua
        </span>
        <span class="qn__legend-item">
          <span class="qn__legend-dot qn__legend-dot--new" />
          Chưa mở
        </span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  questions: { type: Array, default: () => [] },
  answers: { type: Object, default: () => ({}) },
  markedQuestions: { type: Object, default: () => ({}) },
  visitedQuestions: { type: Object, default: () => ({}) },
  currentIndex: { type: Number, default: 0 },
  answeredCount: { type: Number, default: 0 },
  unansweredCount: { type: Number, default: 0 },
  markedCount: { type: Number, default: 0 },
  skippedCount: { type: Number, default: 0 }
})

defineEmits(['select'])

const progressPercent = computed(() => {
  if (!props.questions.length) return 0
  return Math.round((props.answeredCount / props.questions.length) * 100)
})

const hasAnswerValue = (value) => {
  if (Array.isArray(value)) return value.length > 0
  if (value && typeof value === 'object') return Object.keys(value).length > 0
  return Boolean(value)
}

const questionButtonClass = (question, idx) => {
  const key = String(question.id)
  const answered = hasAnswerValue(props.answers[key])
  const marked = Boolean(props.markedQuestions[key])
  const visited = Boolean(props.visitedQuestions[key])

  if (idx === props.currentIndex) return 'qn__btn--active'
  if (marked && answered) return 'qn__btn--marked-done'
  if (marked) return 'qn__btn--marked'
  if (answered) return 'qn__btn--done'
  if (visited) return 'qn__btn--skip'
  return 'qn__btn--new'
}

const questionLabel = (question, idx) => {
  const key = String(question.id)
  const answered = hasAnswerValue(props.answers[key])
  const marked = Boolean(props.markedQuestions[key])
  if (answered) return 'Đã trả lời'
  if (marked) return 'Đánh dấu xem lại'
  return 'Chưa trả lời'
}
</script>


<script>
import { computed } from 'vue'
export default { name: 'QuestionNavigator' }
</script>

<style scoped>
.qn {
  display: flex;
  flex-direction: column;
  gap: 0.875rem;
}

/* Summary */
.qn__summary {
  background: var(--ds-surface);
  border: 1.5px solid var(--ds-border);
  border-radius: var(--ds-radius-2xl);
  padding: 1rem;
}

.dark .qn__summary { border-color: var(--ds-border-strong); }

.qn__summary-header {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 0.625rem;
}

.qn__summary-icon {
  font-size: 1.125rem;
  color: var(--ds-primary);
}

.qn__summary-title {
  font-size: 0.8rem;
  font-weight: 800;
  color: var(--ds-text);
  flex: 1;
}

.dark .qn__summary-title { color: #f1f5f9; }

.qn__summary-pct {
  font-family: var(--ds-font-display);
  font-size: 1.25rem;
  font-weight: 900;
  color: var(--ds-primary);
  line-height: 1;
}

/* Progress bar */
.qn__progress-bar {
  height: 8px;
  border-radius: var(--ds-radius-full);
  background: var(--ds-gray-100);
  overflow: hidden;
  margin-bottom: 0.625rem;
}

.dark .qn__progress-bar { background: var(--ds-gray-800); }

.qn__progress-fill {
  height: 100%;
  border-radius: var(--ds-radius-full);
  background: var(--ds-primary);
  transition: width 0.5s ease;
}

/* Stats */
.qn__summary-stats {
  display: flex;
  gap: 0.875rem;
  margin-bottom: 0.5rem;
}

.qn__stat-item {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  font-size: 0.72rem;
  font-weight: 600;
  color: var(--ds-text-muted);
}

.qn__stat-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}

.qn__stat-dot--done { background: var(--ds-primary); }
.qn__stat-dot--undone { background: var(--ds-gray-300); }
.dark .qn__stat-dot--undone { background: var(--ds-gray-600); }

/* Tags */
.qn__summary-tags {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.qn__tag {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  padding: 0.25rem 0.5rem;
  border-radius: var(--ds-radius-full);
  font-size: 0.65rem;
  font-weight: 700;
}


.qn__tag--marked {
  background: rgba(234, 179, 8, 0.1);
  color: var(--ds-warning);
}

.qn__tag--skip {
  background: var(--ds-gray-100);
  color: var(--ds-text-muted);
}

.dark .qn__tag--skip { background: var(--ds-gray-800); }

/* Grid card */
.qn__grid-card {
  background: var(--ds-surface);
  border: 1.5px solid var(--ds-border);
  border-radius: var(--ds-radius-2xl);
  padding: 1rem;
  overflow: hidden;
}

.dark .qn__grid-card { border-color: var(--ds-border-strong); }

.qn__grid-header {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 0.875rem;
}

.qn__grid-icon {
  font-size: 1.125rem;
  color: var(--ds-primary);
}

.qn__grid-title {
  font-size: 0.8rem;
  font-weight: 800;
  color: var(--ds-text);
  flex: 1;
}

.dark .qn__grid-title { color: #f1f5f9; }

.qn__grid-count {
  font-size: 0.7rem;
  color: var(--ds-text-muted);
  font-weight: 600;
}

/* Question grid */
.qn__grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 0.5rem;
  margin-bottom: 0.875rem;
}

.qn__btn {
  aspect-ratio: 1;
  border-radius: var(--ds-radius-lg);
  border: 1.5px solid;
  font-size: 0.75rem;
  font-weight: 800;
  cursor: pointer;
  transition: all 0.12s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  font-family: inherit;
  min-height: 36px;
}

.qn__btn:hover { transform: scale(1.05); }

.qn__btn:active { transform: scale(0.97); }

/* Button states */
.qn__btn--active {
  background: var(--ds-primary-soft);
  border-color: var(--ds-primary);
  color: var(--ds-primary);
  box-shadow: 0 0 0 2px rgba(79, 70, 229, 0.15);
}

.qn__btn--done {
  background: var(--ds-primary);
  border-color: var(--ds-primary);
  color: white;
}

.qn__btn--marked {
  background: rgba(234, 179, 8, 0.15);
  border-color: rgba(234, 179, 8, 0.4);
  color: var(--ds-warning);
}

.qn__btn--marked-done {
  background: var(--ds-warning);
  border-color: var(--ds-warning);
  color: white;
}

.qn__btn--skip {
  background: var(--ds-gray-100);
  border-color: var(--ds-gray-300);
  color: var(--ds-text-muted);
}

.dark .qn__btn--skip {
  background: var(--ds-gray-800);
  border-color: var(--ds-gray-600);
  color: var(--ds-gray-400);
}

.qn__btn--new {
  background: transparent;
  border-color: var(--ds-border);
  color: var(--ds-text-muted);
}

.dark .qn__btn--new {
  background: transparent;
  border-color: var(--ds-border-strong);
  color: var(--ds-gray-500);
}

.qn__btn--new:hover:not(:disabled) {
  border-color: var(--ds-primary-border);
  color: var(--ds-primary);
  background: var(--ds-primary-soft);
}

/* Legend */
.qn__legend {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 0.375rem;
}

.qn__legend-item {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  font-size: 0.65rem;
  font-weight: 600;
  color: var(--ds-text-muted);
}

.qn__legend-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}

.qn__legend-dot--done { background: var(--ds-primary); }
.qn__legend-dot--marked { background: var(--ds-warning); }
.qn__legend-dot--skip { background: var(--ds-gray-300); }
.dark .qn__legend-dot--skip { background: var(--ds-gray-600); }
.qn__legend-dot--new { background: transparent; border: 1.5px solid var(--ds-border); }
.dark .qn__legend-dot--new { border-color: var(--ds-border-strong); }

/* Responsive */
@media (max-width: 768px) {
  .qn__grid { grid-template-columns: repeat(6, 1fr); }
}
</style>
