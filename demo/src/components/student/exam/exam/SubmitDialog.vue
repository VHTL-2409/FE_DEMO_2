<template>
  <Teleport to="body">
    <Transition name="submodal">
      <div v-if="modelValue" class="sd-overlay" @click.self="$emit('update:modelValue', false)">
        <div class="sd" role="dialog" aria-modal="true" aria-labelledby="submit-dialog-title">
          <!-- Header -->
          <div class="sd__header">
            <div class="sd__header-icon">
              <LucideIcon name="done_all" />
            </div>
            <div>
              <h2 id="submit-dialog-title" class="sd__header-title">Xác nhận nộp bài</h2>
              <p class="sd__header-sub">Vui lòng kiểm tra trước khi xác nhận</p>
            </div>
          </div>

          <!-- Summary -->
          <div class="sd__body">
            <div class="sd__summary-row">
              <div class="sd__stat-card">
                <p class="sd__stat-val">{{ answeredCount }}</p>
                <p class="sd__stat-lbl">Đã trả lời / {{ totalQuestions }} câu</p>
              </div>
              <div class="sd__stat-card">
                <p class="sd__stat-val sd__stat-val--warn">{{ markedCount }}</p>
                <p class="sd__stat-lbl">Đánh dấu xem lại</p>
              </div>
              <div class="sd__stat-card">
                <p class="sd__stat-val">{{ skippedCount }}</p>
                <p class="sd__stat-lbl">Đã mở nhưng bỏ qua</p>
              </div>
            </div>

            <!-- Warning if unanswered -->
            <div v-if="unansweredCount > 0" class="sd__warning">
              <LucideIcon name="warning" />
              <p>Bạn còn <strong>{{ unansweredCount }} câu chưa trả lời</strong> và {{ notVisitedCount }} câu chưa mở.</p>
            </div>

            <p class="sd__notice">
              Bạn sẽ <strong>không thể thay đổi đáp án</strong> sau khi nộp bài.
            </p>
          </div>

          <!-- Footer actions -->
          <div class="sd__footer">
            <button
              type="button"
              class="sd__btn sd__btn--cancel"
              :disabled="isSubmitting"
              @click="$emit('update:modelValue', false)"
            >
              Tiếp tục làm bài
            </button>
            <button
              type="button"
              id="submit-exam-confirm"
              class="sd__btn sd__btn--submit"
              :disabled="isSubmitting"
              @click="$emit('confirm')"
            >
              <LucideIcon name="progress_activity" v-if="isSubmitting"  sd__spinner/>
              <LucideIcon name="done_all" v-else />
              {{ isSubmitting ? 'Đang nộp...' : 'Nộp bài ngay' }}
            </button>
          </div>

        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
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

defineEmits(['confirm', 'update:modelValue'])
</script>


<style scoped>
/* Overlay */
.sd-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(4px);
  z-index: 999;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 1rem;
}

/* Modal */
.sd {
  background: var(--ds-surface);
  border: 1.5px solid var(--ds-border);
  border-radius: var(--ds-radius-2xl);
  box-shadow: 0 24px 64px rgba(0, 0, 0, 0.2);
  width: 100%;
  max-width: 480px;
  overflow: hidden;
}

.dark .sd {
  border-color: var(--ds-border-strong);
  box-shadow: 0 24px 64px rgba(0, 0, 0, 0.4);
}

/* Header */
.sd__header {
  display: flex;
  align-items: flex-start;
  gap: 0.875rem;
  padding: 1.25rem;
  border-bottom: 1px solid var(--ds-border);
  background: var(--ds-gray-50);
}

.dark .sd__header {
  background: var(--ds-gray-800);
  border-bottom-color: var(--ds-border-strong);
}

.sd__header-icon {
  width: 48px;
  height: 48px;
  border-radius: var(--ds-radius-xl);
  background: var(--ds-primary-soft);
  color: var(--ds-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}


.sd__header-title {
  font-family: var(--ds-font-display);
  font-size: 1.125rem;
  font-weight: 900;
  color: var(--ds-text);
  margin: 0 0 0.25rem;
}

.dark .sd__header-title { color: #f1f5f9; }

.sd__header-sub {
  font-size: 0.775rem;
  color: var(--ds-text-muted);
  margin: 0;
  font-weight: 500;
}

/* Body */
.sd__body {
  padding: 1.25rem;
  display: flex;
  flex-direction: column;
  gap: 0.875rem;
}

.sd__summary-row {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 0.625rem;
}

.sd__stat-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.25rem;
  padding: 0.75rem;
  border-radius: var(--ds-radius-xl);
  background: var(--ds-gray-50);
  border: 1px solid var(--ds-border);
  text-align: center;
}

.dark .sd__stat-card { background: var(--ds-gray-800); border-color: var(--ds-border-strong); }

.sd__stat-val {
  font-family: var(--ds-font-display);
  font-size: 1.5rem;
  font-weight: 900;
  color: var(--ds-primary);
  line-height: 1;
}

.sd__stat-val--warn { color: var(--ds-warning); }

.sd__stat-lbl {
  font-size: 0.6rem;
  font-weight: 700;
  color: var(--ds-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.05em;
  text-align: center;
}

/* Warning */
.sd__warning {
  display: flex;
  align-items: flex-start;
  gap: 0.625rem;
  padding: 0.875rem 1rem;
  border-radius: var(--ds-radius-xl);
  background: rgba(234, 179, 8, 0.08);
  border: 1px solid rgba(234, 179, 8, 0.25);
}

.sd__warning-icon {
  font-size: 1.25rem;
  color: var(--ds-warning);
  flex-shrink: 0;
  margin-top: 0.1rem;
}

.sd__warning p {
  font-size: 0.8rem;
  color: var(--ds-warning);
  font-weight: 600;
  margin: 0;
  line-height: 1.4;
}

.sd__warning strong { font-weight: 800; }

/* Notice */
.sd__notice {
  font-size: 0.8rem;
  color: var(--ds-text-muted);
  margin: 0;
  text-align: center;
  font-weight: 500;
}

.sd__notice strong { color: var(--ds-danger); font-weight: 700; }

/* Footer */
.sd__footer {
  display: flex;
  gap: 0.625rem;
  padding: 1rem 1.25rem;
  border-top: 1px solid var(--ds-border);
  background: var(--ds-gray-50);
}

.dark .sd__footer { background: var(--ds-gray-800); border-top-color: var(--ds-border-strong); }

.sd__btn {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  padding: 0.75rem 1rem;
  border-radius: var(--ds-radius-xl);
  font-size: 0.875rem;
  font-weight: 800;
  cursor: pointer;
  transition: all 0.12s ease;
  border: none;
  font-family: inherit;
  min-height: 48px;
}


.sd__btn:active:not(:disabled) { transform: scale(0.98); }

.sd__btn--cancel {
  background: var(--ds-gray-100);
  color: var(--ds-text);
  border: 1.5px solid var(--ds-border);
}

.dark .sd__btn--cancel { background: var(--ds-gray-800); border-color: var(--ds-border-strong); color: var(--ds-gray-300); }

.sd__btn--cancel:hover:not(:disabled) {
  background: var(--ds-gray-200);
  border-color: var(--ds-gray-300);
}

.dark .sd__btn--cancel:hover:not(:disabled) { background: var(--ds-gray-700); }

.sd__btn--submit {
  background: var(--ds-primary);
  color: white;
  box-shadow: 0 2px 8px rgba(79, 70, 229, 0.2);
}

.sd__btn--submit:hover:not(:disabled) {
  background: var(--ds-primary-hover, #4338ca);
  box-shadow: 0 4px 12px rgba(79, 70, 229, 0.3);
  transform: translateY(-1px);
}

.sd__btn:disabled { opacity: 0.5; cursor: not-allowed; transform: none !important; }

.sd__spinner { animation: sdSpin 1s linear infinite; }

@keyframes sdSpin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

/* Transition */
.submodal-enter-active { animation: sdFadeIn 0.2s ease-out; }
.submodal-leave-active { animation: sdFadeIn 0.15s ease-in reverse; }

@keyframes sdFadeIn {
  from { opacity: 0; transform: scale(0.95) translateY(8px); }
  to { opacity: 1; transform: scale(1) translateY(0); }
}
</style>
