<template>
  <div class="qn">
    <!-- Progress summary card — prominent -->
    <div class="qn__summary">
      <div class="qn__summary-header">
        <LucideIcon name="trending_up" class="qn__summary-icon" />
        <span class="qn__summary-title">Tiến độ làm bài</span>
        <span class="qn__summary-pct" :class="progressPctClass">{{ progressPercent }}%</span>
      </div>
      <div class="qn__progress-bar">
        <div
          class="qn__progress-fill"
          :style="{ width: `${progressPercent}%` }"
          :class="progressFillClass"
        />
      </div>
      <div class="qn__summary-stats">
        <div class="qn__stat qn__stat--done">
          <LucideIcon name="check_circle" class="qn__stat-icon" />
          <div class="qn__stat-body">
            <span class="qn__stat-val">{{ answeredCount }}</span>
            <span class="qn__stat-lbl">Đã làm</span>
          </div>
        </div>
        <div class="qn__stat qn__stat--undone">
          <LucideIcon name="radio_button_unchecked" class="qn__stat-icon" />
          <div class="qn__stat-body">
            <span class="qn__stat-val">{{ unansweredCount }}</span>
            <span class="qn__stat-lbl">Chưa làm</span>
          </div>
        </div>
        <div v-if="markedCount > 0" class="qn__stat qn__stat--marked">
          <LucideIcon name="bookmark" class="qn__stat-icon" />
          <div class="qn__stat-body">
            <span class="qn__stat-val">{{ markedCount }}</span>
            <span class="qn__stat-lbl">Đánh dấu</span>
          </div>
        </div>
        <div v-if="skippedCount > 0" class="qn__stat qn__stat--skip">
          <LucideIcon name="skip_next" class="qn__stat-icon" />
          <div class="qn__stat-body">
            <span class="qn__stat-val">{{ skippedCount }}</span>
            <span class="qn__stat-lbl">Bỏ qua</span>
          </div>
        </div>
      </div>
    </div>

    <!-- Question grid -->
    <div class="qn__grid-card">
      <div class="qn__grid-header">
        <LucideIcon name="list" class="qn__grid-icon" />
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
          :title="`Câu ${idx + 1}: ${questionLabel(question, idx)}`"
          :aria-label="`Câu ${idx + 1}: ${questionLabel(question, idx)}`"
          @click="$emit('select', idx)"
        >
          <span class="qn__btn-num">{{ idx + 1 }}</span>
          <span v-if="hasAnswer(question.id)" class="qn__btn-check">
            <LucideIcon name="check" />
          </span>
        </button>
      </div>

      <!-- Legend -->
      <div class="qn__legend">
        <div class="qn__legend-item">
          <span class="qn__legend-dot qn__legend-dot--done" />
          <span>Đã làm</span>
        </div>
        <div class="qn__legend-item">
          <span class="qn__legend-dot qn__legend-dot--marked" />
          <span>Đánh dấu</span>
        </div>
        <div class="qn__legend-item">
          <span class="qn__legend-dot qn__legend-dot--skip" />
          <span>Bỏ qua</span>
        </div>
        <div class="qn__legend-item">
          <span class="qn__legend-dot qn__legend-dot--new" />
          <span>Chưa mở</span>
        </div>
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

const hasAnswerValue = (value) => {
  if (Array.isArray(value)) return value.length > 0
  if (value && typeof value === 'object') return Object.keys(value).length > 0
  return Boolean(value)
}

const hasAnswer = (key) => {
  return hasAnswerValue(props.answers[String(key)])
}

const progressPercent = computed(() => {
  if (!props.questions.length) return 0
  return Math.round((props.answeredCount / props.questions.length) * 100)
})

const progressPctClass = computed(() => {
  const p = progressPercent.value
  if (p === 100) return 'qn__summary-pct--done'
  if (p >= 50) return 'qn__summary-pct--half'
  return ''
})

const progressFillClass = computed(() => {
  const p = progressPercent.value
  if (p === 100) return 'qn__progress-fill--done'
  if (p >= 50) return 'qn__progress-fill--half'
  return ''
})

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
export default { name: 'QuestionNavigator' }
</script>

<style scoped>
.qn {
  display: flex;
  flex-direction: column;
  gap: 0.875rem;
}

/* ─── Summary card ──────────────────────────────────────────────────────── */
.qn__summary {
  background: var(--ds-surface);
  border: 1.5px solid var(--ds-border);
  border-radius: var(--ds-radius-2xl);
  padding: 1rem;
  position: relative;
  overflow: hidden;
}

.qn__summary::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: linear-gradient(90deg, var(--ds-primary), #6366f1);
  border-radius: var(--ds-radius-2xl) var(--ds-radius-2xl) 0 0;
}

.dark .qn__summary { border-color: var(--ds-border-strong); }

.qn__summary-header {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 0.75rem;
}

.qn__summary-icon {
  font-size: 1rem;
  color: var(--ds-primary);
  flex-shrink: 0;
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
  font-size: 1.5rem;
  font-weight: 900;
  color: var(--ds-primary);
  line-height: 1;
}

.qn__summary-pct--done { color: var(--ds-success); }
.qn__summary-pct--half { color: var(--ds-warning); }

/* Progress bar */
.qn__progress-bar {
  height: 10px;
  border-radius: var(--ds-radius-full);
  background: var(--ds-gray-100);
  overflow: hidden;
  margin-bottom: 0.875rem;
}

.dark .qn__progress-bar { background: var(--ds-gray-800); }

.qn__progress-fill {
  height: 100%;
  border-radius: var(--ds-radius-full);
  background: var(--ds-primary);
  transition: width 0.5s ease;
}

.qn__progress-fill--done { background: linear-gradient(90deg, var(--ds-success), #059669); }
.qn__progress-fill--half { background: linear-gradient(90deg, var(--ds-primary), var(--ds-warning)); }

/* Stats grid */
.qn__summary-stats {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.qn__stat {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  padding: 0.375rem 0.75rem;
  border-radius: var(--ds-radius-xl);
  border: 1px solid;
  flex: 1;
  min-width: 70px;
}

.qn__stat--done {
  background: var(--ds-success-soft);
  border-color: rgba(22, 163, 74, 0.25);
  color: var(--ds-success);
}

.qn__stat--undone {
  background: var(--ds-gray-50);
  border-color: var(--ds-border);
  color: var(--ds-text-muted);
}

.dark .qn__stat--undone {
  background: var(--ds-gray-800);
  border-color: var(--ds-border-strong);
}

.qn__stat--marked {
  background: rgba(234, 179, 8, 0.1);
  border-color: rgba(234, 179, 8, 0.25);
  color: var(--ds-warning);
}

.qn__stat--skip {
  background: var(--ds-gray-50);
  border-color: var(--ds-border);
  color: var(--ds-text-muted);
}

.dark .qn__stat--skip { background: var(--ds-gray-800); border-color: var(--ds-border-strong); }

.qn__stat-icon {
  font-size: 0.875rem;
  flex-shrink: 0;
}

.qn__stat-body {
  display: flex;
  flex-direction: column;
  gap: 0;
}

.qn__stat-val {
  font-family: var(--ds-font-display);
  font-size: 1.125rem;
  font-weight: 900;
  line-height: 1;
}

.qn__stat-lbl {
  font-size: 0.55rem;
  font-weight: 700;
  opacity: 0.75;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  line-height: 1.2;
}

/* ─── Grid card ─────────────────────────────────────────────────────────── */
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
  font-size: 1rem;
  color: var(--ds-primary);
  flex-shrink: 0;
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
  font-weight: 700;
}

/* Question grid — larger buttons */
.qn__grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 0.5rem;
  margin-bottom: 0.875rem;
}

.qn__btn {
  position: relative;
  aspect-ratio: 1;
  border-radius: var(--ds-radius-xl);
  border: 2px solid;
  font-size: 0.8rem;
  font-weight: 900;
  cursor: pointer;
  transition: color 0.12s ease, background-color 0.12s ease, border-color 0.12s ease, box-shadow 0.12s ease, transform 0.12s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  font-family: inherit;
  min-height: 44px;
  padding: 0;
  background: transparent;
}

.qn__btn:hover { transform: scale(1.08); }
.qn__btn:active { transform: scale(0.95); }

.qn__btn-num { line-height: 1; font-variant-numeric: tabular-nums; }

.qn__btn-check {
  position: absolute;
  bottom: 2px;
  right: 2px;
  font-size: 0.5rem;
  line-height: 1;
  display: flex;
}

/* Button states */
.qn__btn--active {
  background: var(--ds-primary-soft);
  border-color: var(--ds-primary);
  color: var(--ds-primary);
  box-shadow: 0 0 0 3px rgba(79, 70, 229, 0.2), 0 2px 8px rgba(79, 70, 229, 0.15);
  transform: scale(1.05);
}

.qn__btn--done {
  background: linear-gradient(135deg, var(--ds-primary) 0%, #6366f1 100%);
  border-color: var(--ds-primary);
  color: white;
  box-shadow: 0 2px 8px rgba(79, 70, 229, 0.25);
}

.qn__btn--marked {
  background: rgba(234, 179, 8, 0.15);
  border-color: rgba(234, 179, 8, 0.5);
  color: var(--ds-warning);
}

.qn__btn--marked-done {
  background: linear-gradient(135deg, var(--ds-warning) 0%, #d97706 100%);
  border-color: var(--ds-warning);
  color: white;
  box-shadow: 0 2px 8px rgba(234, 179, 8, 0.3);
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

.dark .qn__btn--new { border-color: var(--ds-border-strong); color: var(--ds-gray-500); }

.qn__btn--new:hover:not(:disabled) {
  border-color: var(--ds-primary-border);
  color: var(--ds-primary);
  background: var(--ds-primary-soft);
}

/* ─── Legend ─────────────────────────────────────────────────────────────── */
.qn__legend {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 0.375rem;
}

.qn__legend-item {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  font-size: 0.65rem;
  font-weight: 700;
  color: var(--ds-text-muted);
}

.qn__legend-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  flex-shrink: 0;
}

.qn__legend-dot--done { background: var(--ds-primary); }
.qn__legend-dot--marked { background: var(--ds-warning); }
.qn__legend-dot--skip { background: var(--ds-gray-300); }
.dark .qn__legend-dot--skip { background: var(--ds-gray-600); }
.qn__legend-dot--new { background: transparent; border: 2px solid var(--ds-border); }
.dark .qn__legend-dot--new { border-color: var(--ds-border-strong); }

/* ─── Responsive ─────────────────────────────────────────────────────────── */
@media (max-width: 768px) {
  .qn__grid { grid-template-columns: repeat(6, 1fr); }
}

@media (max-width: 480px) {
  .qn__legend { grid-template-columns: repeat(2, 1fr); }
  .qn__summary-stats { flex-direction: column; }
  .qn__stat { min-width: unset; }
}
</style>
@media (prefers-reduced-motion: reduce) {
  * {
    transition-duration: 0.01ms !important;
    animation-duration: 0.01ms !important;
  }
}