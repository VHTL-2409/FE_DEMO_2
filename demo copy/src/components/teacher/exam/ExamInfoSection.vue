<template>
  <div class="ec-section">
    <div class="ec-section__header">
      <div class="ec-section__icon">
        <LucideIcon name="info" />
      </div>
      <div>
        <h3 class="ec-section__title">Thông tin đề thi</h3>
      </div>
    </div>

    <div class="ec-section__body">
      <!-- Exam Title -->
      <div class="ec-field">
        <label class="ec-field__label">
          Tiêu đề đề thi
          <span class="ec-field__required">*</span>
        </label>
        <input
          v-model="localTitle"
          type="text"
          class="ec-input"
          placeholder="VD: Kiểm tra giữa kỳ Giải tích nâng cao Q3"
          maxlength="200"
        />
        <div class="ec-field__footer">
          <span v-if="localTitle.length < 3 && localTitle.length > 0" class="ec-field__hint ec-field__hint--error">
            Tiêu đề phải có ít nhất 3 ký tự
          </span>
          <span v-else class="ec-field__hint">
            {{ localTitle.length }}/200 ký tự
          </span>
        </div>
      </div>

      <!-- Subject (for free exam) -->
      <div v-if="examType === 'free'" class="ec-field">
        <label class="ec-field__label">
          Môn học
          <span class="ec-field__required">*</span>
        </label>
        <input
          v-model="localSubject"
          type="text"
          class="ec-input"
          placeholder="VD: Toán học"
        />
      </div>

      <!-- Class selector (for private exam) -->
      <div v-if="examType === 'private'" class="ec-field">
        <label class="ec-field__label">
          Lớp học
          <span class="ec-field__required">*</span>
        </label>
        <div class="ec-class-select">
          <select
            v-model="localClassId"
            class="ec-input ec-input--select"
            :disabled="isLoadingClasses"
          >
            <option value="" disabled>
              <template v-if="isLoadingClasses">Đang tải lớp học...</template>
              <template v-else-if="availableClasses.length === 0">Chưa có lớp học nào</template>
              <template v-else>— Chọn lớp học —</template>
            </option>
            <option
              v-for="cls in availableClasses"
              :key="cls.id"
              :value="cls.id"
            >
              {{ cls.name }}
            </option>
          </select>
          <p v-if="localClassId" class="ec-field__hint ec-field__hint--success">
            <LucideIcon name="check_circle" size="13" />
            Đề thi sẽ chỉ hiển thị với học sinh trong lớp đã chọn
          </p>
        </div>
      </div>

      <!-- Description -->
      <div class="ec-field">
        <label class="ec-field__label">
          Mô tả
          <span class="ec-field__optional">(tùy chọn)</span>
        </label>
        <textarea
          v-model="localDescription"
          rows="3"
          class="ec-input ec-input--textarea"
          placeholder="Mô tả ngắn về nội dung, phạm vi kiến thức, hoặc hướng dẫn cho học sinh..."
          maxlength="500"
        />
        <div class="ec-field__footer">
          <span class="ec-field__hint">{{ localDescription.length }}/500 ký tự</span>
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
  description: { type: String, default: '' },
  examType: { type: String, default: 'free' },
  classId: { type: [String, Number], default: '' },
  availableClasses: { type: Array, default: () => [] },
  isLoadingClasses: { type: Boolean, default: false }
})

const emit = defineEmits([
  'update:title',
  'update:subject',
  'update:description',
  'update:classId'
])

const localTitle = computed({
  get: () => props.title,
  set: (v) => emit('update:title', v)
})

const localSubject = computed({
  get: () => props.subject,
  set: (v) => emit('update:subject', v)
})

const localDescription = computed({
  get: () => props.description,
  set: (v) => emit('update:description', v)
})

const localClassId = computed({
  get: () => props.classId,
  set: (v) => emit('update:classId', v)
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

.ec-section__body {
  padding: 1.5rem;
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.ec-field {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
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

.ec-field__optional {
  font-weight: 500;
  color: var(--ds-text-muted);
  font-size: 0.75rem;
}

.ec-field__hint {
  font-size: 0.7rem;
  color: var(--ds-text-muted);
  display: flex;
  align-items: center;
  gap: 0.25rem;
}

.ec-field__hint--error {
  color: var(--ds-danger);
}

.ec-field__hint--success {
  color: var(--ds-success);
}

.ec-field__footer {
  display: flex;
  justify-content: flex-end;
}

.ec-input {
  width: 100%;
  padding: 0.75rem 1.25rem;
  background: var(--ds-surface);
  border: 2px solid var(--ds-border);
  border-radius: var(--ds-radius-2xl);
  font-size: 0.9375rem;
  color: var(--ds-text);
  transition: color 0.25s cubic-bezier(0.4, 0, 0.2, 1), background-color 0.25s cubic-bezier(0.4, 0, 0.2, 1), border-color 0.25s cubic-bezier(0.4, 0, 0.2, 1), box-shadow 0.25s cubic-bezier(0.4, 0, 0.2, 1), transform 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  outline: none;
}

.ec-input::placeholder {
  color: var(--ds-text-muted);
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

.dark .ec-input::placeholder {
  color: var(--ds-gray-500);
}

.ec-input--textarea {
  resize: vertical;
  min-height: 100px;
  line-height: 1.6;
  border-radius: var(--ds-radius-xl);
}

.ec-input--select {
  cursor: pointer;
}

.ec-class-select {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}
</style>