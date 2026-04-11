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

      <!-- Quick templates -->
      <div class="ec-field">
        <label class="ec-field__label">Mẫu nhanh</label>
        <div class="ec-templates">
          <button
            v-for="tpl in templates"
            :key="tpl.title"
            type="button"
            class="ec-template-btn"
            @click="applyTemplate(tpl)"
          >
            <LucideIcon :name="tpl.icon" />
            <span>{{ tpl.title }}</span>
          </button>
        </div>
        <p v-if="localTitle" class="ec-templates__hint">
          <LucideIcon name="info" size="12" />
          Nhấn mẫu để điền nhanh môn học và mô tả
        </p>
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

const templates = computed(() => {
  if (props.examType === 'private') {
    return [
      { title: 'Giữa kỳ', icon: 'history_edu', description: 'Đề thi giữa học kỳ I, phạm vi chương 1-3' },
      { title: 'Cuối kỳ', icon: 'school', description: 'Đề thi cuối học kỳ II, toàn bộ chương trình' },
      { title: '15 phút', icon: 'timer', description: 'Đề thi 15 phút, chương Kim loại' },
      { title: 'Thực hành', icon: 'science', description: 'Đề thi thực hành, phòng lab' }
    ]
  }
  return [
    { title: 'Giữa kỳ', icon: 'history_edu', subject: 'Toán học', description: 'Đề thi giữa học kỳ I, phạm vi chương 1-3' },
    { title: 'Cuối kỳ', icon: 'school', subject: 'Vật lý', description: 'Đề thi cuối học kỳ II, toàn bộ chương trình' },
    { title: '15 phút', icon: 'timer', subject: 'Hóa học', description: 'Đề thi 15 phút, chương Kim loại' },
    { title: 'Thực hành', icon: 'science', subject: 'Sinh học', description: 'Đề thi thực hành, phòng lab' }
  ]
})

const applyTemplate = (tpl) => {
  localDescription.value = tpl.description
  if (props.examType === 'free' && tpl.subject) {
    localSubject.value = tpl.subject
  }
}
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

.ec-templates {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.ec-template-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.375rem;
  padding: 0.5rem 0.875rem;
  border-radius: var(--ds-radius-full);
  border: 1.5px solid var(--ds-border);
  background: var(--ds-surface);
  color: var(--ds-text-secondary);
  font-size: 0.8rem;
  font-weight: 600;
  cursor: pointer;
  transition: color 0.15s ease, background-color 0.15s ease, border-color 0.15s ease, box-shadow 0.15s ease, transform 0.15s ease;
}

.ec-template-btn:hover {
  border-color: var(--ds-primary);
  background: var(--ds-primary-soft);
  color: var(--ds-primary);
  transform: translateY(-1px);
}

.dark .ec-template-btn {
  background: var(--ds-gray-800);
  border-color: var(--ds-border-strong);
}

.dark .ec-template-btn:hover {
  background: rgba(79, 70, 229, 0.15);
}

.ec-templates__hint {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  font-size: 0.7rem;
  color: var(--ds-text-muted);
  margin: 0;
}
</style>