<template>
  <div class="prp">
    <!-- Header -->
    <div class="prp__header">
      <div class="prp__header-left">
        <div class="prp__header-icon">
          <LucideIcon name="analytics" />
        </div>
        <div>
          <h3 class="prp__header-title">Báo cáo Parse</h3>
          <p class="prp__header-sub">
            {{ report?.questionCount ?? 0 }} câu ·
            {{ report?.answerCount ?? 0 }} đáp án
          </p>
        </div>
      </div>

      <div class="prp__header-right">
        <!-- Confidence chip -->
        <span
          v-if="report?.confidence != null"
          class="prp__conf-chip"
          :class="confClass"
        >
          <LucideIcon :name="confIcon" size="12" />
          {{ ((report.confidence || 0) * 100).toFixed(0) }}%
        </span>

        <!-- Template chip -->
        <span v-if="templateLabel" class="prp__template-chip">
          <LucideIcon name="template" size="12" />
          {{ templateLabel }}
        </span>

        <!-- Parse time -->
        <span v-if="report?.parseTimeMs" class="prp__time-chip">
          <LucideIcon name="timer" size="12" />
          {{ (report.parseTimeMs / 1000).toFixed(1) }}s
        </span>
      </div>
    </div>

    <!-- Stats row -->
    <div v-if="report" class="prp__stats">
      <div class="prp__stat">
        <span class="prp__stat-icon prp__stat-icon--mcq">
          <LucideIcon name="check_box" size="13" />
        </span>
        <div>
          <span class="prp__stat-val">{{ report.multipleChoiceCount ?? 0 }}</span>
          <span class="prp__stat-label">Trắc nghiệm</span>
        </div>
      </div>
      <div class="prp__stat">
        <span class="prp__stat-icon prp__stat-icon--essay">
          <LucideIcon name="edit_note" size="13" />
        </span>
        <div>
          <span class="prp__stat-val">{{ report.essayCount ?? 0 }}</span>
          <span class="prp__stat-label">Tự luận</span>
        </div>
      </div>
      <div class="prp__stat">
        <span class="prp__stat-icon prp__stat-icon--answer">
          <LucideIcon name="key" size="13" />
        </span>
        <div>
          <span class="prp__stat-val">{{ report.answerCount ?? 0 }}</span>
          <span class="prp__stat-label">Đáp án</span>
        </div>
      </div>
      <div v-if="invalidCount > 0" class="prp__stat prp__stat--warn">
        <span class="prp__stat-icon prp__stat-icon--warn">
          <LucideIcon name="warning" size="13" />
        </span>
        <div>
          <span class="prp__stat-val">{{ invalidCount }}</span>
          <span class="prp__stat-label">Cần review</span>
        </div>
      </div>
    </div>

    <!-- Warnings -->
    <div v-if="report?.warnings?.length" class="prp__section">
      <div class="prp__section-title">
        <LucideIcon name="info" size="13" />
        Cảnh báo ({{ report.warnings.length }})
      </div>
      <ul class="prp__warn-list">
        <li
          v-for="(warn, idx) in report.warnings"
          :key="idx"
          class="prp__warn-item"
        >
          <LucideIcon name="warning" size="11" class="prp__warn-icon" />
          {{ warn }}
        </li>
      </ul>
    </div>

    <!-- Errors -->
    <div v-if="report?.errors?.length" class="prp__section">
      <div class="prp__section-title prp__section-title--error">
        <LucideIcon name="error" size="13" />
        Lỗi ({{ report.errors.length }})
      </div>
      <ul class="prp__warn-list prp__warn-list--error">
        <li
          v-for="(err, idx) in report.errors"
          :key="idx"
          class="prp__warn-item prp__warn-item--error"
        >
          <LucideIcon name="error" size="11" class="prp__warn-icon" />
          {{ err }}
        </li>
      </ul>
    </div>

    <!-- Invalid questions -->
    <div v-if="report?.invalidQuestions?.length" class="prp__section">
      <div class="prp__section-title prp__section-title--warn">
        <LucideIcon name="flag" size="13" />
        Câu hỏi không hợp lệ ({{ report.invalidQuestions.length }})
      </div>
      <div class="prp__invalid-nums">
        <span
          v-for="num in report.invalidQuestions"
          :key="num"
          class="prp__inv-num"
          :class="highlightInvalid?.(num) && 'prp__inv-num--active'"
          @click="$emit('goto-question', num)"
        >
          #{{ num }}
        </span>
      </div>
    </div>

    <!-- Empty state -->
    <div v-if="!report" class="prp__empty">
      <LucideIcon name="analytics" size="28" />
      <p>Chưa có báo cáo</p>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  report: {
    type: Object,
    default: null
  },
  highlightInvalid: {
    type: Function,
    default: null
  }
})

const emit = defineEmits(['goto-question'])

const invalidCount = computed(() =>
  (props.report?.invalidQuestions?.length ?? 0)
)

const templateLabel = computed(() => {
  const t = props.report?.selectedTemplate
  if (!t) return null
  if (t.includes('template_01') || t.includes('math_broken')) return 'Toán vỡ'
  if (t.includes('template_02') || t.includes('clean_mcq')) return 'Tiếng Anh'
  if (t.includes('template_03') || t.includes('math_answer_grid')) return 'Toán đáp án lưới'
  return t.replace(/_/g, ' ')
})

const confClass = computed(() => {
  const c = props.report?.confidence ?? 0
  if (c >= 0.8) return 'prp__conf-chip--high'
  if (c >= 0.5) return 'prp__conf-chip--mid'
  return 'prp__conf-chip--low'
})

const confIcon = computed(() => {
  const c = props.report?.confidence ?? 0
  if (c >= 0.8) return 'check_circle'
  if (c >= 0.5) return 'progress_activity'
  return 'error'
})
</script>

<style scoped>
.prp {
  display: flex;
  flex-direction: column;
  border: 1.5px solid var(--ds-border);
  border-radius: var(--ds-radius-2xl);
  background: var(--ds-surface);
  overflow: hidden;
}

.dark .prp {
  border-color: var(--ds-border-strong);
}

/* Header */
.prp__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  padding: 0.875rem 1rem;
  background: linear-gradient(135deg, var(--ds-primary-soft) 0%, transparent 70%);
  border-bottom: 1px solid var(--ds-border);
}

.dark .prp__header {
  border-bottom-color: var(--ds-border-strong);
}

.prp__header-left {
  display: flex;
  align-items: center;
  gap: 0.625rem;
}

.prp__header-icon {
  width: 32px;
  height: 32px;
  border-radius: var(--ds-radius-lg);
  background: var(--ds-primary);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.prp__header-title {
  font-size: 0.85rem;
  font-weight: 700;
  color: var(--ds-text);
  margin: 0;
}

.dark .prp__header-title { color: #f1f5f9; }

.prp__header-sub {
  font-size: 0.7rem;
  color: var(--ds-text-muted);
  margin: 0.15rem 0 0;
}

.prp__header-right {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  flex-wrap: wrap;
}

/* Chips */
.prp__conf-chip,
.prp__template-chip,
.prp__time-chip {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  padding: 0.2rem 0.5rem;
  border-radius: var(--ds-radius-full);
  font-size: 0.68rem;
  font-weight: 700;
}

.prp__conf-chip {
  background: var(--ds-gray-100);
  color: var(--ds-text-muted);
  border: 1px solid var(--ds-border);
}

.dark .prp__conf-chip {
  background: var(--ds-gray-700);
  border-color: var(--ds-border-strong);
  color: #94a3b8;
}

.prp__conf-chip--high { background: var(--ds-success-soft); color: var(--ds-success); border-color: rgba(22,163,74,0.2); }
.prp__conf-chip--mid  { background: var(--ds-warning-soft); color: var(--ds-warning); border-color: rgba(245,158,11,0.2); }
.prp__conf-chip--low  { background: var(--ds-danger-soft);  color: var(--ds-danger);  border-color: rgba(239,68,68,0.2); }

.prp__template-chip {
  background: var(--ds-primary-soft);
  color: var(--ds-primary);
  border: 1px solid var(--ds-primary-border);
}

.prp__time-chip {
  background: var(--ds-gray-100);
  color: var(--ds-text-muted);
  border: 1px solid var(--ds-border);
  font-variant-numeric: tabular-nums;
}

.dark .prp__time-chip {
  background: var(--ds-gray-700);
  border-color: var(--ds-border-strong);
  color: #94a3b8;
}

/* Stats */
.prp__stats {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(80px, 1fr));
  gap: 0.5rem;
  padding: 0.75rem 1rem;
  background: var(--ds-gray-50);
  border-bottom: 1px solid var(--ds-border);
}

.dark .prp__stats {
  background: var(--ds-gray-800);
  border-bottom-color: var(--ds-border-strong);
}

.prp__stat {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.375rem 0.5rem;
  border-radius: var(--ds-radius-lg);
  background: var(--ds-surface);
  border: 1px solid var(--ds-border);
}

.dark .prp__stat {
  background: var(--ds-gray-700);
  border-color: var(--ds-border-strong);
}

.prp__stat--warn {
  border-color: rgba(245, 158, 11, 0.3);
  background: rgba(245, 158, 11, 0.04);
}

.dark .prp__stat--warn {
  background: rgba(245, 158, 11, 0.06);
}

.prp__stat-icon {
  width: 24px;
  height: 24px;
  border-radius: var(--ds-radius-sm);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.prp__stat-icon--mcq    { background: var(--ds-primary-soft); color: var(--ds-primary); }
.prp__stat-icon--essay  { background: rgba(245,158,11,0.1);  color: #d97706; }
.prp__stat-icon--answer { background: var(--ds-success-soft); color: var(--ds-success); }
.prp__stat-icon--warn   { background: var(--ds-warning-soft);  color: var(--ds-warning); }

.prp__stat-val {
  display: block;
  font-size: 1rem;
  font-weight: 800;
  color: var(--ds-text);
  line-height: 1;
  font-variant-numeric: tabular-nums;
}

.dark .prp__stat-val { color: #f1f5f9; }

.prp__stat-label {
  display: block;
  font-size: 0.65rem;
  color: var(--ds-text-muted);
  font-weight: 500;
  margin-top: 0.1rem;
}

/* Sections */
.prp__section {
  padding: 0.75rem 1rem;
  border-bottom: 1px solid var(--ds-border);
}

.dark .prp__section {
  border-bottom-color: var(--ds-border-strong);
}

.prp__section-title {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  font-size: 0.72rem;
  font-weight: 700;
  color: var(--ds-text-secondary);
  text-transform: uppercase;
  letter-spacing: 0.05em;
  margin-bottom: 0.5rem;
}

.prp__section-title--error { color: var(--ds-danger); }
.prp__section-title--warn  { color: var(--ds-warning); }

/* Warning list */
.prp__warn-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.prp__warn-list--error .prp__warn-item {
  color: var(--ds-danger);
  background: var(--ds-danger-soft);
  border-color: rgba(239,68,68,0.15);
}

.prp__warn-item {
  display: flex;
  align-items: flex-start;
  gap: 0.375rem;
  padding: 0.375rem 0.5rem;
  border-radius: var(--ds-radius-md);
  background: var(--ds-gray-50);
  border: 1px solid var(--ds-border);
  font-size: 0.75rem;
  color: var(--ds-text-secondary);
  line-height: 1.4;
}

.dark .prp__warn-item {
  background: var(--ds-gray-800);
  border-color: var(--ds-border-strong);
  color: #94a3b8;
}

.prp__warn-item--error {
  color: var(--ds-danger);
  background: var(--ds-danger-soft);
  border-color: rgba(239,68,68,0.15);
}

.dark .prp__warn-item--error {
  background: rgba(239,68,68,0.06);
  color: #f87171;
}

.prp__warn-icon {
  flex-shrink: 0;
  margin-top: 1px;
  color: var(--ds-warning);
}

.prp__warn-item--error .prp__warn-icon {
  color: var(--ds-danger);
}

/* Invalid question numbers */
.prp__invalid-nums {
  display: flex;
  flex-wrap: wrap;
  gap: 0.375rem;
}

.prp__inv-num {
  display: inline-flex;
  align-items: center;
  padding: 0.2rem 0.5rem;
  border-radius: var(--ds-radius-md);
  background: var(--ds-gray-100);
  border: 1px solid var(--ds-border);
  font-size: 0.72rem;
  font-weight: 700;
  color: var(--ds-warning);
  cursor: pointer;
  transition: all 0.12s ease;
}

.dark .prp__inv-num {
  background: var(--ds-gray-700);
  border-color: var(--ds-border-strong);
  color: #fbbf24;
}

.prp__inv-num:hover {
  border-color: var(--ds-warning);
  background: var(--ds-warning-soft);
}

.prp__inv-num--active {
  background: var(--ds-warning-soft);
  border-color: var(--ds-warning);
}

/* Empty */
.prp__empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.5rem;
  padding: 2rem;
  color: var(--ds-text-muted);
  font-size: 0.8rem;
}
</style>

