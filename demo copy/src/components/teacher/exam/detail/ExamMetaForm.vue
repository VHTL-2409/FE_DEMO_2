<template>
  <div class="emf">
    <div class="emf__section">
      <div class="emf__section-header">
        <div class="emf__section-icon">
          <LucideIcon name="info" />
        </div>
        <div>
          <h3 class="emf__section-title">Thông tin đề thi</h3>
          <p class="emf__section-desc">Tiêu đề, môn học, lớp và mô tả</p>
        </div>
      </div>

      <div class="emf__body">
        <!-- Title -->
        <div class="emf__field">
          <label class="emf__label">
            Tiêu đề đề thi
            <span class="emf__required">*</span>
          </label>
          <input
            v-model="localTitle"
            type="text"
            class="emf__input"
            placeholder="VD: Kiểm tra giữa kỳ Giải tích nâng cao Q3"
            maxlength="200"
          />
          <div class="emf__footer">
            <span v-if="localTitle.length < 3 && localTitle.length > 0" class="emf__hint emf__hint--error">
              Tiêu đề phải có ít nhất 3 ký tự
            </span>
            <span v-else class="emf__hint">{{ localTitle.length }}/200 ký tự</span>
          </div>
        </div>

        <!-- Subject + Class -->
        <div class="emf__field-row">
          <div class="emf__field">
            <label class="emf__label">Môn học</label>
            <input
              v-model="localSubject"
              type="text"
              class="emf__input"
              placeholder="VD: Toán học"
            />
          </div>
          <div class="emf__field">
            <label class="emf__label">Lớp / Section</label>
            <input
              v-model="localClassName"
              type="text"
              class="emf__input"
              placeholder="VD: 12A1"
            />
          </div>
        </div>

        <!-- Description -->
        <div class="emf__field">
          <label class="emf__label">Mô tả</label>
          <textarea
            v-model="localDescription"
            rows="3"
            class="emf__input emf__input--textarea"
            placeholder="Mô tả ngắn về nội dung, phạm vi kiến thức..."
            maxlength="500"
          />
          <div class="emf__footer">
            <span class="emf__hint">{{ localDescription.length }}/500</span>
          </div>
        </div>
      </div>
    </div>

    <!-- Config -->
    <div class="emf__section">
      <div class="emf__section-header">
        <div class="emf__section-icon">
          <LucideIcon name="settings" />
        </div>
        <div>
          <h3 class="emf__section-title">Cấu hình thi</h3>
          <p class="emf__section-desc">Thời lượng, xáo trộn, số lần thi</p>
        </div>
      </div>

      <div class="emf__body">
        <!-- Duration -->
        <div class="emf__field">
          <label class="emf__label">Thời lượng làm bài</label>
          <div class="emf__duration">
            <input
              v-model.number="localDuration"
              type="number"
              min="5"
              max="480"
              step="5"
              class="emf__input emf__input--number"
            />
            <span class="emf__duration-unit">phút</span>
          </div>
          <div class="emf__duration-presets">
            <button
              v-for="preset in durationPresets"
              :key="preset"
              type="button"
              class="emf__preset-btn"
              :class="localDuration === preset && 'emf__preset-btn--active'"
              @click="localDuration = preset"
            >
              {{ preset }}p
            </button>
          </div>
        </div>

        <!-- Toggles -->
        <div class="emf__toggles">
          <div class="emf__toggle-item">
            <div>
              <p class="emf__toggle-title">Xáo câu hỏi</p>
              <p class="emf__toggle-desc">Câu hỏi hiển thị ngẫu nhiên</p>
            </div>
            <button
              type="button"
              class="emf__toggle"
              :class="localShuffleQuestions && 'emf__toggle--on'"
              @click="localShuffleQuestions = !localShuffleQuestions"
            >
              <span class="emf__toggle-knob" />
            </button>
          </div>

          <div class="emf__toggle-item">
            <div>
              <p class="emf__toggle-title">Xáo đáp án</p>
              <p class="emf__toggle-desc">Đáp án hiển thị ngẫu nhiên</p>
            </div>
            <button
              type="button"
              class="emf__toggle"
              :class="localShuffleAnswers && 'emf__toggle--on'"
              @click="localShuffleAnswers = !localShuffleAnswers"
            >
              <span class="emf__toggle-knob" />
            </button>
          </div>

          <div class="emf__toggle-item">
            <div>
              <p class="emf__toggle-title">Hiển thị điểm ngay sau nộp</p>
              <p class="emf__toggle-desc">Học sinh thấy điểm trên màn hình xác nhận. Tắt nếu muốn công bố điểm sau.</p>
            </div>
            <button
              type="button"
              class="emf__toggle"
              :class="localShowScoreAfterSubmit && 'emf__toggle--on'"
              @click="localShowScoreAfterSubmit = !localShowScoreAfterSubmit"
            >
              <span class="emf__toggle-knob" />
            </button>
          </div>
        </div>

        <!-- Max attempts -->
        <div class="emf__field">
          <label class="emf__label">Số lần thi tối đa</label>
          <div class="emf__attempts">
            <button
              v-for="n in [1, 2, 3, 5]"
              :key="n"
              type="button"
              class="emf__attempt-btn"
              :class="localMaxAttempts === n && 'emf__attempt-btn--active'"
              @click="localMaxAttempts = n"
            >
              {{ n }}
            </button>
            <input
              v-model.number="localMaxAttempts"
              type="number"
              min="1"
              max="99"
              class="emf__input emf__input--sm"
            />
            <span class="emf__attempts-label">lần</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  title: { type: String, default: '' },
  subject: { type: String, default: '' },
  className: { type: String, default: '' },
  description: { type: String, default: '' },
  duration: { type: [Number, String], default: 60 },
  shuffleQuestions: { type: Boolean, default: false },
  shuffleAnswers: { type: Boolean, default: false },
  showScoreAfterSubmit: { type: Boolean, default: true },
  maxAttempts: { type: [Number, String], default: 1 }
})

const emit = defineEmits([
  'update:title', 'update:subject', 'update:className', 'update:description',
  'update:duration', 'update:shuffleQuestions', 'update:shuffleAnswers', 'update:showScoreAfterSubmit',
  'update:maxAttempts'
])

const durationPresets = [15, 30, 45, 60, 90, 120]

const localTitle = computed({
  get: () => props.title,
  set: (v) => emit('update:title', v)
})
const localSubject = computed({
  get: () => props.subject,
  set: (v) => emit('update:subject', v)
})
const localClassName = computed({
  get: () => props.className,
  set: (v) => emit('update:className', v)
})
const localDescription = computed({
  get: () => props.description,
  set: (v) => emit('update:description', v)
})
const localDuration = computed({
  get: () => Number(props.duration),
  set: (v) => emit('update:duration', Math.max(5, Math.min(480, Number(v) || 60)))
})
const localShuffleQuestions = computed({
  get: () => props.shuffleQuestions,
  set: (v) => emit('update:shuffleQuestions', v)
})
const localShuffleAnswers = computed({
  get: () => props.shuffleAnswers,
  set: (v) => emit('update:shuffleAnswers', v)
})
const localShowScoreAfterSubmit = computed({
  get: () => props.showScoreAfterSubmit,
  set: (v) => emit('update:showScoreAfterSubmit', v)
})
const localMaxAttempts = computed({
  get: () => Number(props.maxAttempts),
  set: (v) => emit('update:maxAttempts', Math.max(1, Math.min(99, Number(v) || 1)))
})
</script>


<style scoped>
.emf {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.emf__section {
  background: var(--ds-surface);
  border: 1px solid var(--ds-border);
  border-radius: var(--ds-radius-2xl);
  overflow: hidden;
}

.dark .emf__section {
  border-color: var(--ds-border-strong);
}

.emf__section-header {
  display: flex;
  align-items: center;
  gap: 0.875rem;
  padding: 1.25rem 1.5rem;
  border-bottom: 1px solid var(--ds-border);
  background: linear-gradient(135deg, var(--ds-primary-soft) 0%, transparent 100%);
}

.dark .emf__section-header { border-bottom-color: var(--ds-border-strong); }

.emf__section-icon {
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


.emf__section-title {
  font-family: var(--ds-font-display);
  font-size: 1.05rem;
  font-weight: 700;
  color: var(--ds-text);
  margin: 0;
  line-height: 1.2;
}

.dark .emf__section-title { color: #f1f5f9; }

.emf__section-desc {
  font-size: 0.75rem;
  color: var(--ds-text-muted);
  margin: 0.25rem 0 0;
}

.emf__body {
  padding: 1.5rem;
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

/* Field */
.emf__field {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.emf__field-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0.75rem;
}

@media (max-width: 480px) {
  .emf__field-row { grid-template-columns: 1fr; }
}

.emf__label {
  font-size: 0.8rem;
  font-weight: 700;
  color: var(--ds-text);
}

.dark .emf__label { color: #f1f5f9; }

.emf__required { color: var(--ds-danger); margin-left: 2px; }

.emf__input {
  width: 100%;
  padding: 0.6875rem 1rem;
  background: var(--ds-surface);
  border: 1.5px solid var(--ds-border);
  border-radius: var(--ds-radius-xl);
  font-size: 0.875rem;
  color: var(--ds-text);
  transition: color 0.15s ease, background-color 0.15s ease, border-color 0.15s ease, box-shadow 0.15s ease, transform 0.15s ease;
  outline: none;
}

.dark .emf__input {
  background: var(--ds-gray-800);
  border-color: var(--ds-border-strong);
  color: #f1f5f9;
}

.emf__input::placeholder { color: var(--ds-text-muted); }
.dark .emf__input::placeholder { color: var(--ds-gray-500); }

.emf__input:focus {
  border-color: var(--ds-primary);
  box-shadow: 0 0 0 3px var(--ds-primary-ring);
}

.emf__input--textarea {
  resize: vertical;
  min-height: 80px;
  line-height: 1.5;
}

.emf__input--number {
  width: 80px;
  text-align: center;
  font-weight: 700;
  font-size: 1rem;
  font-family: var(--ds-font-display);
}

.emf__input--sm {
  width: 64px;
  padding: 0.375rem 0.5rem;
  font-size: 0.875rem;
  font-weight: 700;
  text-align: center;
}

.emf__footer {
  display: flex;
  justify-content: flex-end;
}

.emf__hint {
  font-size: 0.7rem;
  color: var(--ds-text-muted);
}

.emf__hint--error { color: var(--ds-danger); }

/* Duration */
.emf__duration {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.emf__duration-unit {
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--ds-text-muted);
}

.emf__duration-presets {
  display: flex;
  flex-wrap: wrap;
  gap: 0.375rem;
  margin-top: 0.5rem;
}

.emf__preset-btn {
  padding: 0.375rem 0.75rem;
  border-radius: var(--ds-radius-full);
  border: 1.5px solid var(--ds-border);
  background: var(--ds-surface);
  color: var(--ds-text-secondary);
  font-size: 0.8rem;
  font-weight: 700;
  cursor: pointer;
  transition: color 0.15s ease, background-color 0.15s ease, border-color 0.15s ease, box-shadow 0.15s ease, transform 0.15s ease;
}

.dark .emf__preset-btn {
  background: var(--ds-gray-800);
  border-color: var(--ds-border-strong);
  color: #94a3b8;
}

.emf__preset-btn:hover {
  border-color: var(--ds-primary);
  background: var(--ds-primary-soft);
  color: var(--ds-primary);
}

.emf__preset-btn--active {
  background: var(--ds-primary);
  border-color: var(--ds-primary);
  color: white;
  box-shadow: 0 4px 12px rgba(79, 70, 229, 0.25);
}

/* Toggles */
.emf__toggles {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.emf__toggle-item {
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

.dark .emf__toggle-item {
  background: var(--ds-gray-800);
  border-color: var(--ds-border-strong);
}

.emf__toggle-title {
  font-size: 0.875rem;
  font-weight: 700;
  color: var(--ds-text);
  margin: 0;
}

.dark .emf__toggle-title { color: #f1f5f9; }

.emf__toggle-desc {
  font-size: 0.7rem;
  color: var(--ds-text-muted);
  margin: 0.25rem 0 0;
}

.emf__toggle {
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

.dark .emf__toggle { background: var(--ds-gray-600); }

.emf__toggle--on { background: var(--ds-primary); }

.emf__toggle-knob {
  position: absolute;
  top: 3px;
  left: 3px;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: white;
  box-shadow: 0 1px 4px rgba(0,0,0,0.2);
  transition: transform 0.2s ease;
}

.emf__toggle--on .emf__toggle-knob { transform: translateX(20px); }

/* Attempts */
.emf__attempts {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.emf__attempt-btn {
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

.dark .emf__attempt-btn {
  background: var(--ds-gray-800);
  border-color: var(--ds-border-strong);
  color: #94a3b8;
}

.emf__attempt-btn:hover {
  border-color: var(--ds-primary);
  background: var(--ds-primary-soft);
  color: var(--ds-primary);
}

.emf__attempt-btn--active {
  background: var(--ds-primary);
  border-color: var(--ds-primary);
  color: white;
  box-shadow: 0 4px 12px rgba(79, 70, 229, 0.25);
}

.emf__attempts-label {
  font-size: 0.8rem;
  color: var(--ds-text-muted);
  font-weight: 600;
}
</style>