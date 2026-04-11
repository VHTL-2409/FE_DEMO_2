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

          <!-- Body -->
          <div class="sd__body">
            <!-- Stat cards -->
            <div class="sd__summary-row">
              <div class="sd__stat-card" :class="unansweredCount > 0 ? 'sd__stat-card--warn' : 'sd__stat-card--ok'">
                <div class="sd__stat-icon">
                  <LucideIcon :name="unansweredCount > 0 ? 'radio_button_unchecked' : 'check_circle'" />
                </div>
                <div class="sd__stat-body">
                  <p class="sd__stat-val" :class="unansweredCount > 0 ? 'sd__stat-val--warn' : 'sd__stat-val--ok'">
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

            <!-- Warning if unanswered -->
            <div v-if="unansweredCount > 0" class="sd__warning">
              <LucideIcon name="warning" class="sd__warning-icon" />
              <div class="sd__warning-text">
                <p class="sd__warning-title">Còn câu chưa trả lời!</p>
                <p class="sd__warning-desc">
                  {{ unansweredCount }} câu chưa trả lời và {{ notVisitedCount }} câu chưa mở.
                  Bạn có chắc muốn nộp bài?
                </p>
              </div>
            </div>

            <!-- Critical notice -->
            <div class="sd__notice">
              <LucideIcon name="lock" class="sd__notice-icon" />
              <p>Sau khi nộp, bạn <strong>không thể thay đổi đáp án</strong>.</p>
            </div>
          </div>

          <!-- Footer actions -->
          <div class="sd__footer">
            <button
              type="button"
              class="sd__btn sd__btn--cancel"
              :disabled="isSubmitting"
              @click="$emit('update:modelValue', false)"
            >
              <LucideIcon name="arrow_back" />
              Tiếp tục làm bài
            </button>
            <button
              type="button"
              id="submit-exam-confirm"
              class="sd__btn sd__btn--submit"
              :class="{ 'sd__btn--all-done': unansweredCount === 0 && skippedCount === 0 }"
              :disabled="isSubmitting"
              @click="$emit('confirm')"
            >
              <LucideIcon name="progress_activity" v-if="isSubmitting" class="sd__spinner"/>
              <LucideIcon name="check_circle" v-else />
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
/* ─── Overlay ─────────────────────────────────────────────────────────── */
.sd-overlay {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.68);
  z-index: 999;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 1rem;
}

/* ─── Modal ───────────────────────────────────────────────────────────── */
.sd {
  background: var(--ds-surface);
  border: 2px solid var(--ds-border);
  border-radius: var(--ds-radius-2xl);
  box-shadow: 0 24px 64px rgba(0, 0, 0, 0.25);
  width: 100%;
  max-width: 500px;
  overflow: hidden;
}

.dark .sd {
  border-color: var(--ds-border-strong);
  box-shadow: 0 24px 64px rgba(0, 0, 0, 0.5);
}

/* ─── Header ──────────────────────────────────────────────────────────── */
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
  background: linear-gradient(135deg, var(--ds-success), #059669);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  box-shadow: 0 4px 12px rgba(22, 163, 74, 0.3);
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

/* ─── Body ─────────────────────────────────────────────────────────────── */
.sd__body {
  padding: 1.25rem;
  display: flex;
  flex-direction: column;
  gap: 0.875rem;
}

/* Stat cards */
.sd__summary-row {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 0.625rem;
}

.sd__stat-card {
  display: flex;
  align-items: center;
  gap: 0.625rem;
  padding: 0.75rem;
  border-radius: var(--ds-radius-xl);
  border: 1.5px solid;
  transition: color 0.15s ease, background-color 0.15s ease, border-color 0.15s ease, box-shadow 0.15s ease, transform 0.15s ease;
}

.sd__stat-card--ok {
  background: var(--ds-success-soft);
  border-color: rgba(22, 163, 74, 0.3);
}

.sd__stat-card--warn {
  background: rgba(234, 179, 8, 0.08);
  border-color: rgba(234, 179, 8, 0.3);
}

.sd__stat-card--neutral {
  background: var(--ds-gray-50);
  border-color: var(--ds-border);
}

.dark .sd__stat-card--neutral { background: var(--ds-gray-800); border-color: var(--ds-border-strong); }

.sd__stat-icon {
  font-size: 1.125rem;
  flex-shrink: 0;
  display: flex;
  align-items: center;
}

.sd__stat-card--ok .sd__stat-icon { color: var(--ds-success); }
.sd__stat-card--warn .sd__stat-icon { color: var(--ds-warning); }
.sd__stat-card--neutral .sd__stat-icon { color: var(--ds-text-muted); }

.sd__stat-body { display: flex; flex-direction: column; gap: 0.125rem; }

.sd__stat-val {
  font-family: var(--ds-font-display);
  font-size: 1.375rem;
  font-weight: 900;
  line-height: 1;
  color: var(--ds-text);
}

.dark .sd__stat-val { color: #f1f5f9; }

.sd__stat-val--ok { color: var(--ds-success); }
.sd__stat-val--warn { color: var(--ds-warning); }
.sd__stat-val--muted { color: var(--ds-text-muted); }

.sd__stat-total {
  font-size: 0.875rem;
  font-weight: 600;
  opacity: 0.7;
}

.sd__stat-lbl {
  font-size: 0.6rem;
  font-weight: 700;
  color: var(--ds-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.05em;
  margin: 0;
}

/* Warning */
.sd__warning {
  display: flex;
  align-items: flex-start;
  gap: 0.75rem;
  padding: 0.875rem 1rem;
  border-radius: var(--ds-radius-xl);
  background: rgba(234, 179, 8, 0.08);
  border: 1.5px solid rgba(234, 179, 8, 0.3);
}

.sd__warning-icon {
  font-size: 1.25rem;
  color: var(--ds-warning);
  flex-shrink: 0;
  margin-top: 0.1rem;
}

.sd__warning-text { display: flex; flex-direction: column; gap: 0.25rem; }

.sd__warning-title {
  font-size: 0.8rem;
  font-weight: 800;
  color: var(--ds-warning);
  margin: 0;
  line-height: 1.2;
}

.sd__warning-desc {
  font-size: 0.75rem;
  color: var(--ds-text-secondary);
  margin: 0;
  line-height: 1.4;
}

/* Notice */
.sd__notice {
  display: flex;
  align-items: center;
  gap: 0.625rem;
  padding: 0.75rem 1rem;
  border-radius: var(--ds-radius-xl);
  background: var(--ds-gray-50);
  border: 1px solid var(--ds-border);
}

.dark .sd__notice { background: var(--ds-gray-800); border-color: var(--ds-border-strong); }

.sd__notice-icon {
  font-size: 1rem;
  color: var(--ds-danger);
  flex-shrink: 0;
}

.sd__notice p {
  font-size: 0.8rem;
  color: var(--ds-text-secondary);
  margin: 0;
  font-weight: 500;
  line-height: 1.4;
}

.sd__notice strong { color: var(--ds-danger); font-weight: 800; }

/* ─── Footer ────────────────────────────────────────────────────────────── */
.sd__footer {
  display: flex;
  gap: 0.625rem;
  padding: 1rem 1.25rem;
  border-top: 1px solid var(--ds-border);
  background: var(--ds-gray-50);
}

.dark .sd__footer {
  background: var(--ds-gray-800);
  border-top-color: var(--ds-border-strong);
}

.sd__btn {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  padding: 0.875rem 1rem;
  border-radius: var(--ds-radius-xl);
  font-size: 0.875rem;
  font-weight: 800;
  cursor: pointer;
  transition: color 0.15s ease, background-color 0.15s ease, border-color 0.15s ease, box-shadow 0.15s ease, transform 0.15s ease;
  border: none;
  font-family: inherit;
  min-height: 52px;
  letter-spacing: 0.01em;
}

.sd__btn:active:not(:disabled) { transform: scale(0.98); }

/* Cancel */
.sd__btn--cancel {
  background: var(--ds-surface);
  color: var(--ds-text);
  border: 1.5px solid var(--ds-border);
}

.dark .sd__btn--cancel { background: var(--ds-gray-700); border-color: var(--ds-border-strong); color: var(--ds-gray-200); }

.sd__btn--cancel:hover:not(:disabled) {
  background: var(--ds-gray-100);
  border-color: var(--ds-gray-300);
}

.dark .sd__btn--cancel:hover:not(:disabled) { background: var(--ds-gray-600); }

/* Submit — most prominent, danger/final action */
.sd__btn--submit {
  background: linear-gradient(135deg, var(--ds-danger) 0%, #b91c1c 100%);
  color: white;
  box-shadow: 0 4px 16px rgba(220, 38, 38, 0.35);
}

.sd__btn--submit:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(220, 38, 38, 0.5);
  background: linear-gradient(135deg, #dc2626 0%, #b91c1c 100%);
}

/* When all done — green to celebrate */
.sd__btn--all-done {
  background: linear-gradient(135deg, var(--ds-success) 0%, #059669 100%);
  box-shadow: 0 4px 16px rgba(22, 163, 74, 0.35);
}

.sd__btn--all-done:hover:not(:disabled) {
  box-shadow: 0 8px 24px rgba(22, 163, 74, 0.5);
}

.sd__btn:disabled { opacity: 0.5; cursor: not-allowed; transform: none !important; }

.sd__spinner { animation: sdSpin 1s linear infinite; }

@keyframes sdSpin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

/* ─── Transition ───────────────────────────────────────────────────────── */
.submodal-enter-active { animation: sdFadeIn 0.2s ease-out; }
.submodal-leave-active { animation: sdFadeIn 0.15s ease-in reverse; }

@keyframes sdFadeIn {
  from { opacity: 0; transform: scale(0.95) translateY(8px); }
  to { opacity: 1; transform: scale(1) translateY(0); }
}
</style>
@media (prefers-reduced-motion: reduce) {
  * {
    transition-duration: 0.01ms !important;
    animation-duration: 0.01ms !important;
  }
}