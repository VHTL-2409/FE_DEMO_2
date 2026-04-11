<template>
  <div class="ec-section">
    <div class="ec-section__header">
      <div class="ec-section__icon">
        <LucideIcon name="settings" />
      </div>
      <div>
        <h3 class="ec-section__title">Cấu hình thi</h3>
      </div>
    </div>

    <div class="ec-section__body">

      <!-- Duration -->
      <div class="ec-field">
        <label class="ec-field__label">
          Thời lượng làm bài
          <span class="ec-field__required">*</span>
        </label>
        <div class="ec-duration">
          <div class="ec-duration__input-wrap">
            <input
              v-model.number="localDuration"
              type="number"
              min="5"
              max="480"
              step="5"
              class="ec-input ec-input--number"
            />
            <span class="ec-duration__unit">phút</span>
          </div>
          <div class="ec-duration__presets">
            <button
              v-for="preset in durationPresets"
              :key="preset"
              type="button"
              class="ec-preset-btn"
              :class="localDuration === preset && 'ec-preset-btn--active'"
              @click="localDuration = preset"
            >
              {{ preset }}p
            </button>
          </div>
        </div>
      </div>

      <!-- Review options -->
      <div class="ec-field">
        <label class="ec-field__label">Tùy chọn xem lại</label>
        <div class="ec-toggle-list">
          <div class="ec-toggle-item">
            <div class="ec-toggle-item__body">
              <LucideIcon name="visibility" />
              <div>
                <p class="ec-toggle-item__title">Hiển thị đáp án sau thi</p>
                <p class="ec-toggle-item__desc">Học sinh được xem đáp án đúng sau khi nộp bài</p>
              </div>
            </div>
            <button
              type="button"
              class="ec-toggle"
              :class="localShowAnswers && 'ec-toggle--on'"
              @click="localShowAnswers = !localShowAnswers"
            >
              <span class="ec-toggle__knob" />
            </button>
          </div>

          <div class="ec-toggle-item">
            <div class="ec-toggle-item__body">
              <LucideIcon name="rate_review" />
              <div>
                <p class="ec-toggle-item__title">Cho phép xem lại bài</p>
                <p class="ec-toggle-item__desc">Học sinh có thể xem lại bài thi của mình</p>
              </div>
            </div>
            <button
              type="button"
              class="ec-toggle"
              :class="localAllowReview && 'ec-toggle--on'"
              @click="localAllowReview = !localAllowReview"
            >
              <span class="ec-toggle__knob" />
            </button>
          </div>
        </div>
      </div>

      <!-- Max attempts -->
      <div class="ec-field">
        <label class="ec-field__label">Số lần thi tối đa</label>
        <div class="ec-attempts">
          <button
            v-for="n in [1, 2, 3, 5]"
            :key="n"
            type="button"
            class="ec-attempt-btn"
            :class="localMaxAttempts === n && 'ec-attempt-btn--active'"
            @click="localMaxAttempts = n"
          >
            {{ n }}
          </button>
          <div class="ec-attempts__custom">
            <input
              v-model.number="localMaxAttempts"
              type="number"
              min="1"
              max="99"
              class="ec-input ec-input--number ec-input--sm"
            />
            <span class="ec-attempts__custom-label">lần</span>
          </div>
        </div>
      </div>

    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  duration: { type: [Number, String], default: 60 },
  showAnswers: { type: Boolean, default: false },
  allowReview: { type: Boolean, default: true },
  maxAttempts: { type: [Number, String], default: 1 }
})

const emit = defineEmits([
  'update:duration', 'update:showAnswers', 'update:allowReview', 'update:maxAttempts'
])

const durationPresets = [15, 30, 45, 60, 90, 120]

const localDuration = computed({
  get: () => Number(props.duration),
  set: (v) => emit('update:duration', Math.max(5, Math.min(480, Number(v) || 60)))
})

const localShowAnswers = computed({
  get: () => props.showAnswers,
  set: (v) => emit('update:showAnswers', v)
})

const localAllowReview = computed({
  get: () => props.allowReview,
  set: (v) => emit('update:allowReview', v)
})

const localMaxAttempts = computed({
  get: () => Number(props.maxAttempts),
  set: (v) => emit('update:maxAttempts', Math.max(1, Math.min(99, Number(v) || 1)))
})
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

.dark .ec-section__title {
  color: var(--ds-text);
}

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

/* Field */
.ec-field {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.ec-field__label {
  font-size: 0.8rem;
  font-weight: 700;
  color: var(--ds-text);
}

.dark .ec-field__label {
  color: var(--ds-text);
}

.ec-field__required {
  color: var(--ds-danger);
  margin-left: 2px;
}

/* Duration */
.ec-duration {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.ec-duration__input-wrap {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.ec-input {
  width: 100%;
  padding: 0.6875rem 1.25rem;
  background: var(--ds-surface);
  border: 2px solid var(--ds-border);
  border-radius: var(--ds-radius-2xl);
  font-size: 0.875rem;
  color: var(--ds-text);
  transition: color 0.25s cubic-bezier(0.4, 0, 0.2, 1), background-color 0.25s cubic-bezier(0.4, 0, 0.2, 1), border-color 0.25s cubic-bezier(0.4, 0, 0.2, 1), box-shadow 0.25s cubic-bezier(0.4, 0, 0.2, 1), transform 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  outline: none;
}

.ec-input:hover {
  border-color: var(--ds-primary-border);
  box-shadow: 0 4px 12px rgba(79, 70, 229, 0.1);
}

.ec-input:focus {
  border-color: var(--ds-primary);
  box-shadow: 0 0 0 4px var(--ds-primary-ring), 0 8px 24px rgba(79, 70, 229, 0.15);
  transform: translateY(-1px);
}

.dark .ec-input {
  background: var(--ds-gray-800);
  border-color: var(--ds-border-strong);
  color: var(--ds-text);
}

.dark .ec-input:hover {
  border-color: var(--ds-primary-border);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
}

.dark .ec-input:focus {
  box-shadow: 0 0 0 4px rgba(79, 70, 229, 0.3), 0 8px 24px rgba(0, 0, 0, 0.3);
  transform: translateY(-1px);
}

.ec-input--number {
  width: 100px;
  text-align: center;
  font-weight: 700;
  font-size: 1.125rem;
  font-family: var(--ds-font-display);
  padding: 0.625rem 0.875rem;
}

.ec-input--sm {
  width: 64px;
  padding: 0.5rem 0.75rem;
  font-size: 0.875rem;
  font-weight: 700;
}

.ec-duration__unit {
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--ds-text-muted);
}

.ec-duration__presets {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.ec-preset-btn {
  padding: 0.375rem 0.875rem;
  border-radius: var(--ds-radius-full);
  border: 1.5px solid var(--ds-border);
  background: var(--ds-surface);
  color: var(--ds-text-secondary);
  font-size: 0.8rem;
  font-weight: 700;
  cursor: pointer;
  transition: color 0.15s ease, background-color 0.15s ease, border-color 0.15s ease, box-shadow 0.15s ease, transform 0.15s ease;
}

.dark .ec-preset-btn {
  background: var(--ds-gray-800);
  border-color: var(--ds-border-strong);
}

.ec-preset-btn:hover {
  border-color: var(--ds-primary);
  background: var(--ds-primary-soft);
  color: var(--ds-primary);
}

.ec-preset-btn--active {
  background: var(--ds-primary);
  border-color: var(--ds-primary);
  color: white;
  box-shadow: 0 4px 12px rgba(79, 70, 229, 0.25);
}

/* Toggle list */
.ec-toggle-list {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.ec-toggle-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  padding: 0.875rem 1rem;
  background: var(--ds-gray-50);
  border: 1px solid var(--ds-border);
  border-radius: var(--ds-radius-xl);
  transition: color 0.15s ease, background-color 0.15s ease, border-color 0.15s ease, box-shadow 0.15s ease, transform 0.15s ease;
}

.dark .ec-toggle-item {
  background: var(--ds-gray-800);
  border-color: var(--ds-border-strong);
}

.ec-toggle-item:hover {
  border-color: var(--ds-primary-border);
  background: var(--ds-primary-soft);
}

.ec-toggle-item__body {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex: 1;
  min-width: 0;
}

.ec-toggle-item__icon {
  font-size: 1.25rem;
  color: var(--ds-text-muted);
  flex-shrink: 0;
}

.ec-toggle-item__title {
  font-size: 0.875rem;
  font-weight: 700;
  color: var(--ds-text);
  margin: 0;
  line-height: 1.2;
}

.dark .ec-toggle-item__title {
  color: var(--ds-text);
}

.ec-toggle-item__desc {
  font-size: 0.7rem;
  color: var(--ds-text-muted);
  margin: 0.25rem 0 0;
  line-height: 1.4;
}

/* Toggle switch */
.ec-toggle {
  position: relative;
  width: 44px;
  height: 24px;
  border-radius: 12px;
  border: none;
  background: var(--ds-gray-300);
  cursor: pointer;
  transition: background 0.2s ease;
  flex-shrink: 0;
}

.dark .ec-toggle {
  background: var(--ds-gray-600);
}

.ec-toggle--on {
  background: var(--ds-primary);
}

.ec-toggle__knob {
  position: absolute;
  top: 3px;
  left: 3px;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: white;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.2);
  transition: transform 0.2s ease;
}

.ec-toggle--on .ec-toggle__knob {
  transform: translateX(20px);
}

/* Attempts */
.ec-attempts {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.ec-attempt-btn {
  width: 40px;
  height: 40px;
  border-radius: var(--ds-radius-lg);
  border: 1.5px solid var(--ds-border);
  background: var(--ds-surface);
  color: var(--ds-text-secondary);
  font-size: 0.875rem;
  font-weight: 700;
  cursor: pointer;
  transition: color 0.15s ease, background-color 0.15s ease, border-color 0.15s ease, box-shadow 0.15s ease, transform 0.15s ease;
}

.dark .ec-attempt-btn {
  background: var(--ds-gray-800);
  border-color: var(--ds-border-strong);
}

.ec-attempt-btn:hover {
  border-color: var(--ds-primary);
  background: var(--ds-primary-soft);
  color: var(--ds-primary);
}

.ec-attempt-btn--active {
  background: var(--ds-primary);
  border-color: var(--ds-primary);
  color: white;
  box-shadow: 0 4px 12px rgba(79, 70, 229, 0.25);
}

.ec-attempts__custom {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  margin-left: 0.5rem;
}

.ec-attempts__custom-label {
  font-size: 0.8rem;
  color: var(--ds-text-muted);
  font-weight: 600;
}
</style>