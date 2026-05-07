<template>
  <Modal
    v-model="open"
    :title="dialogTitle"
    :subtitle="dialogSubtitle"
    :persistent="isSubmitting"
    size="md"
  >
    <template #header="{ titleId }">
      <div class="sd__header" :class="{ 'sd__header--warning': isBlankSubmission }">
        <div class="sd__header-icon" :class="{ 'sd__header-icon--warning': isBlankSubmission }">
          <LucideIcon :name="isBlankSubmission ? 'warning' : 'done_all'" />
        </div>
        <div class="sd__header-copy">
          <h2 :id="titleId" class="sd__header-title">{{ dialogTitle }}</h2>
          <p class="sd__header-sub">{{ dialogSubtitle }}</p>
        </div>
      </div>
    </template>

    <div class="sd__body">
      <div class="sd__summary-row">
        <div class="sd__stat-card" :class="answeredCount > 0 ? 'sd__stat-card--ok' : 'sd__stat-card--warn'">
          <div class="sd__stat-icon">
            <LucideIcon :name="answeredCount > 0 ? 'check_circle' : 'radio_button_unchecked'" />
          </div>
          <div class="sd__stat-body">
            <p class="sd__stat-val" :class="answeredCount > 0 ? 'sd__stat-val--ok' : 'sd__stat-val--warn'">
              {{ answeredCount }}<span class="sd__stat-total">/{{ totalQuestions }}</span>
            </p>
            <p class="sd__stat-lbl">Đã trả lời</p>
          </div>
        </div>

        <div class="sd__stat-card" :class="markedCount > 0 ? 'sd__stat-card--warn' : 'sd__stat-card--neutral'">
          <div class="sd__stat-icon">
            <LucideIcon name="bookmark" />
          </div>
          <div class="sd__stat-body">
            <p class="sd__stat-val" :class="markedCount > 0 ? 'sd__stat-val--warn' : 'sd__stat-val--muted'">
              {{ markedCount }}
            </p>
            <p class="sd__stat-lbl">Đánh dấu</p>
          </div>
        </div>

        <div class="sd__stat-card" :class="skippedCount > 0 ? 'sd__stat-card--warn' : 'sd__stat-card--neutral'">
          <div class="sd__stat-icon">
            <LucideIcon name="skip_next" />
          </div>
          <div class="sd__stat-body">
            <p class="sd__stat-val" :class="skippedCount > 0 ? 'sd__stat-val--warn' : 'sd__stat-val--muted'">
              {{ skippedCount }}
            </p>
            <p class="sd__stat-lbl">Bỏ qua</p>
          </div>
        </div>
      </div>

      <div v-if="showWarning" class="sd__warning" :class="{ 'sd__warning--blank': isBlankSubmission }">
        <LucideIcon name="warning" class="sd__warning-icon" />
        <div>
          <p class="sd__warning-title">{{ warningTitle }}</p>
          <p class="sd__warning-desc">{{ warningDescription }}</p>
        </div>
      </div>

      <div class="sd__notice">
        <LucideIcon name="lock" class="sd__notice-icon" />
        <p>Sau khi nộp, bạn <strong>không thể thay đổi đáp án</strong>.</p>
      </div>
    </div>

    <template #footer>
      <div class="sd__footer">
        <button
          type="button"
          class="sd__btn sd__btn--cancel"
          :disabled="isSubmitting"
          @click="close"
        >
          <LucideIcon name="arrow_back" />
          Tiếp tục làm bài
        </button>
        <button
          id="submit-exam-confirm"
          type="button"
          class="sd__btn sd__btn--submit"
          :class="{ 'sd__btn--all-done': isFullyAnswered, 'sd__btn--blank': isBlankSubmission }"
          :disabled="isSubmitting"
          @click="confirm"
        >
          <LucideIcon v-if="isSubmitting" name="progress_activity" class="sd__spinner" />
          <LucideIcon v-else name="check_circle" />
          {{ submitLabel }}
        </button>
      </div>
    </template>
  </Modal>
</template>

<script setup>
import { computed } from 'vue'
import Modal from '../../../ui/Modal.vue'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  answeredCount: { type: Number, default: 0 },
  totalQuestions: { type: Number, default: 0 },
  unansweredCount: { type: Number, default: 0 },
  notVisitedCount: { type: Number, default: 0 },
  markedCount: { type: Number, default: 0 },
  skippedCount: { type: Number, default: 0 },
  isSubmitting: { type: Boolean, default: false }
})

const emit = defineEmits(['confirm', 'update:modelValue'])

const open = computed({
  get: () => props.modelValue,
  set: (value) => {
    if (!value && props.isSubmitting) return
    emit('update:modelValue', value)
  }
})

const isBlankSubmission = computed(() => props.totalQuestions > 0 && props.answeredCount === 0)
const isFullyAnswered = computed(() => props.totalQuestions > 0 && props.unansweredCount === 0 && props.skippedCount === 0)
const showWarning = computed(() => props.unansweredCount > 0)

const dialogTitle = computed(() =>
  isBlankSubmission.value ? 'Bạn chưa trả lời câu nào' : 'Xác nhận nộp bài'
)

const dialogSubtitle = computed(() =>
  isBlankSubmission.value
    ? 'Bài sẽ được ghi nhận là chưa có đáp án.'
    : 'Kiểm tra tiến độ trước khi xác nhận.'
)

const warningTitle = computed(() =>
  isBlankSubmission.value ? 'Nộp bài trắng?' : 'Còn câu chưa trả lời'
)

const warningDescription = computed(() => {
  if (isBlankSubmission.value) {
    return `Bạn đang nộp bài với 0/${props.totalQuestions} câu đã trả lời. Chỉ xác nhận nếu bạn chắc chắn muốn kết thúc bài thi.`
  }
  return `Còn ${props.unansweredCount} câu chưa trả lời, trong đó ${props.notVisitedCount} câu chưa mở.`
})

const submitLabel = computed(() => {
  if (props.isSubmitting) return 'Đang nộp...'
  return isBlankSubmission.value ? 'Nộp bài trắng' : 'Nộp bài ngay'
})

const close = () => {
  if (props.isSubmitting) return
  emit('update:modelValue', false)
}

const confirm = () => {
  if (props.isSubmitting) return
  emit('confirm')
}
</script>

<style scoped>
.sd__header {
  display: flex;
  align-items: flex-start;
  gap: 0.875rem;
}

.sd__header-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
  width: 48px;
  height: 48px;
  color: white;
  background: linear-gradient(135deg, var(--ds-success), #059669);
  border-radius: var(--ds-radius-xl);
  box-shadow: 0 4px 12px rgba(22, 163, 74, 0.3);
}

.sd__header-icon--warning {
  background: linear-gradient(135deg, var(--ds-warning), #d97706);
  box-shadow: 0 4px 12px rgba(217, 119, 6, 0.28);
}

.sd__header-copy {
  min-width: 0;
}

.sd__header-title {
  margin: 0 0 0.25rem;
  color: var(--ds-text);
  font-family: var(--ds-font-display);
  font-size: 1.125rem;
  font-weight: 900;
  line-height: 1.25;
}

.sd__header--warning .sd__header-title {
  color: var(--ds-warning);
}

.sd__header-sub {
  margin: 0;
  color: var(--ds-text-muted);
  font-size: 0.8rem;
  font-weight: 500;
  line-height: 1.45;
}

.sd__body {
  display: flex;
  flex-direction: column;
  gap: 0.875rem;
}

.sd__summary-row {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.625rem;
}

.sd__stat-card {
  display: flex;
  align-items: center;
  gap: 0.625rem;
  min-width: 0;
  padding: 0.75rem;
  border: 1.5px solid;
  border-radius: var(--ds-radius-xl);
}

.sd__stat-card--ok {
  background: var(--ds-success-soft);
  border-color: rgba(22, 163, 74, 0.3);
}

.sd__stat-card--warn {
  background: rgba(245, 158, 11, 0.08);
  border-color: rgba(245, 158, 11, 0.3);
}

.sd__stat-card--neutral {
  background: var(--ds-gray-50);
  border-color: var(--ds-border);
}

.dark .sd__stat-card--neutral {
  background: var(--ds-gray-800);
  border-color: var(--ds-border-strong);
}

.sd__stat-icon {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  color: var(--ds-text-muted);
  font-size: 1.125rem;
}

.sd__stat-card--ok .sd__stat-icon {
  color: var(--ds-success);
}

.sd__stat-card--warn .sd__stat-icon {
  color: var(--ds-warning);
}

.sd__stat-body {
  min-width: 0;
}

.sd__stat-val {
  margin: 0;
  color: var(--ds-text);
  font-family: var(--ds-font-display);
  font-size: 1.35rem;
  font-weight: 900;
  line-height: 1;
}

.sd__stat-val--ok {
  color: var(--ds-success);
}

.sd__stat-val--warn {
  color: var(--ds-warning);
}

.sd__stat-val--muted {
  color: var(--ds-text-muted);
}

.sd__stat-total {
  font-size: 0.875rem;
  font-weight: 700;
  opacity: 0.72;
}

.sd__stat-lbl {
  margin: 0.2rem 0 0;
  color: var(--ds-text-muted);
  font-size: 0.6rem;
  font-weight: 800;
  line-height: 1.15;
  text-transform: uppercase;
}

.sd__warning,
.sd__notice {
  display: flex;
  align-items: flex-start;
  gap: 0.75rem;
  padding: 0.875rem 1rem;
  border-radius: var(--ds-radius-xl);
}

.sd__warning {
  background: rgba(245, 158, 11, 0.08);
  border: 1.5px solid rgba(245, 158, 11, 0.3);
}

.sd__warning--blank {
  background: var(--ds-danger-soft);
  border-color: rgba(220, 38, 38, 0.28);
}

.sd__warning-icon {
  flex: 0 0 auto;
  margin-top: 0.1rem;
  color: var(--ds-warning);
  font-size: 1.2rem;
}

.sd__warning--blank .sd__warning-icon {
  color: var(--ds-danger);
}

.sd__warning-title {
  margin: 0;
  color: var(--ds-warning);
  font-size: 0.84rem;
  font-weight: 900;
  line-height: 1.25;
}

.sd__warning--blank .sd__warning-title {
  color: var(--ds-danger);
}

.sd__warning-desc {
  margin: 0.25rem 0 0;
  color: var(--ds-text-secondary);
  font-size: 0.78rem;
  line-height: 1.45;
}

.sd__notice {
  align-items: center;
  background: var(--ds-gray-50);
  border: 1px solid var(--ds-border);
}

.dark .sd__notice {
  background: var(--ds-gray-800);
  border-color: var(--ds-border-strong);
}

.sd__notice-icon {
  flex: 0 0 auto;
  color: var(--ds-danger);
  font-size: 1rem;
}

.sd__notice p {
  margin: 0;
  color: var(--ds-text-secondary);
  font-size: 0.8rem;
  font-weight: 500;
  line-height: 1.4;
}

.sd__notice strong {
  color: var(--ds-danger);
  font-weight: 900;
}

.sd__footer {
  display: flex;
  gap: 0.625rem;
  width: 100%;
}

.sd__btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 1;
  gap: 0.5rem;
  min-height: 52px;
  padding: 0.875rem 1rem;
  border-radius: var(--ds-radius-xl);
  font-family: inherit;
  font-size: 0.875rem;
  font-weight: 850;
  line-height: 1.2;
  cursor: pointer;
  transition: background-color 0.15s ease, border-color 0.15s ease, color 0.15s ease, box-shadow 0.15s ease, transform 0.15s ease;
}

.sd__btn:active:not(:disabled) {
  transform: scale(0.98);
}

.sd__btn:disabled {
  cursor: not-allowed;
  opacity: 0.55;
  transform: none;
}

.sd__btn--cancel {
  color: var(--ds-text);
  background: var(--ds-surface);
  border: 1.5px solid var(--ds-border);
}

.sd__btn--cancel:hover:not(:disabled) {
  background: var(--ds-gray-100);
  border-color: var(--ds-gray-300);
}

.sd__btn--submit {
  color: white;
  background: linear-gradient(135deg, var(--ds-danger) 0%, #b91c1c 100%);
  border: 1.5px solid transparent;
  box-shadow: 0 4px 16px rgba(220, 38, 38, 0.35);
}

.sd__btn--submit:hover:not(:disabled) {
  background: linear-gradient(135deg, #dc2626 0%, #b91c1c 100%);
  box-shadow: 0 8px 24px rgba(220, 38, 38, 0.45);
  transform: translateY(-1px);
}

.sd__btn--all-done {
  background: linear-gradient(135deg, var(--ds-success) 0%, #059669 100%);
  box-shadow: 0 4px 16px rgba(22, 163, 74, 0.32);
}

.sd__btn--all-done:hover:not(:disabled) {
  background: linear-gradient(135deg, var(--ds-success) 0%, #047857 100%);
  box-shadow: 0 8px 24px rgba(22, 163, 74, 0.45);
}

.sd__btn--blank {
  background: linear-gradient(135deg, var(--ds-danger) 0%, #991b1b 100%);
}

.sd__spinner {
  animation: sdSpin 1s linear infinite;
}

@keyframes sdSpin {
  from {
    transform: rotate(0deg);
  }

  to {
    transform: rotate(360deg);
  }
}

@media (max-width: 520px) {
  .sd__summary-row {
    grid-template-columns: 1fr;
  }

  .sd__footer {
    flex-direction: column-reverse;
  }
}

@media (prefers-reduced-motion: reduce) {
  * {
    transition-duration: 0.01ms !important;
    animation-duration: 0.01ms !important;
  }
}
</style>
