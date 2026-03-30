<template>
  <div class="prf">
    <!-- Card header -->
    <div class="prf__header">
      <div class="prf__header-icon">
        <LucideIcon name="school" />
      </div>
      <div>
        <h3 class="prf__title">Thông tin chuyên môn</h3>
        <p class="prf__subtitle">Bộ môn, tổ chuyên môn, trường học</p>
      </div>
      <button
        type="button"
        class="prf__edit-toggle"
        :class="{ 'prf__edit-toggle--active': isEditing }"
        @click="toggleEdit"
      >
        <LucideIcon :name="isEditing ? 'close' : 'edit'" />
        {{ isEditing ? 'Hủy' : 'Chỉnh sửa' }}
      </button>
    </div>

    <!-- View mode -->
    <div v-if="!isEditing" class="prf__view">
      <div class="prf__field-grid">
        <div class="prf__field">
          <LucideIcon name="psychology" prf__field-icon />
          <div class="prf__field-content">
            <span class="prf__field-label">Bộ môn</span>
            <span class="prf__field-value">{{ form.subject || '—' }}</span>
          </div>
        </div>

        <div class="prf__field">
          <LucideIcon name="groups" prf__field-icon />
          <div class="prf__field-content">
            <span class="prf__field-label">Tổ / Khoa</span>
            <span class="prf__field-value">{{ form.department || '—' }}</span>
          </div>
        </div>

        <div class="prf__field prf__field--span-2">
          <LucideIcon name="domain" prf__field-icon />
          <div class="prf__field-content">
            <span class="prf__field-label">Trường / Trung tâm</span>
            <span class="prf__field-value">{{ form.school || '—' }}</span>
          </div>
        </div>

        <div class="prf__field">
          <LucideIcon name="tag" prf__field-icon />
          <div class="prf__field-content">
            <span class="prf__field-label">Mã giáo viên</span>
            <span class="prf__field-value">{{ form.teacherCode || '—' }}</span>
          </div>
        </div>

        <div class="prf__field">
          <LucideIcon name="calendar_month" prf__field-icon />
          <div class="prf__field-content">
            <span class="prf__field-label">Ngày vào làm</span>
            <span class="prf__field-value">{{ formatDate(form.hireDate) }}</span>
          </div>
        </div>

        <div class="prf__field prf__field--span-2">
          <LucideIcon name="notes" prf__field-icon />
          <div class="prf__field-content">
            <span class="prf__field-label">Mô tả / Ghi chú</span>
            <span class="prf__field-value">{{ form.bio || '—' }}</span>
          </div>
        </div>
      </div>

      <!-- Empty state hint -->
      <div v-if="isAllEmpty" class="prf__empty-hint">
        <LucideIcon name="info" />
        <span>Nhấn "Chỉnh sửa" để thêm thông tin chuyên môn</span>
      </div>
    </div>

    <!-- Edit mode -->
    <div v-else class="prf__edit">
      <form class="prf__form" @submit.prevent="submitForm">
        <div class="prf__form-grid">
          <div class="prf__form-group">
            <label class="prf__label">Bộ môn</label>
            <input
              v-model="form.subject"
              class="prf__input"
              type="text"
              placeholder="VD: Toán học, Ngữ văn, Tiếng Anh..."
            />
          </div>

          <div class="prf__form-group">
            <label class="prf__label">Tổ / Khoa</label>
            <input
              v-model="form.department"
              class="prf__input"
              type="text"
              placeholder="VD: Tổ Toán, Khoa Khoa học Tự nhiên..."
            />
          </div>

          <div class="prf__form-group prf__form-group--span-2">
            <label class="prf__label">Trường / Trung tâm</label>
            <input
              v-model="form.school"
              class="prf__input"
              type="text"
              placeholder="Tên trường hoặc trung tâm giáo dục"
            />
          </div>

          <div class="prf__form-group">
            <label class="prf__label">Mã giáo viên</label>
            <input
              v-model="form.teacherCode"
              class="prf__input"
              type="text"
              placeholder="Mã số giáo viên"
              disabled
            />
            <span class="prf__hint">Mã giáo viên được cấp bởi quản trị viên</span>
          </div>

          <div class="prf__form-group">
            <label class="prf__label">Ngày vào làm</label>
            <input
              v-model="form.hireDate"
              class="prf__input"
              type="date"
            />
          </div>

          <div class="prf__form-group prf__form-group--span-2">
            <label class="prf__label">Mô tả / Ghi chú</label>
            <textarea
              v-model="form.bio"
              class="prf__textarea"
              type="text"
              placeholder="Mô tả ngắn về chuyên môn, kinh nghiệm giảng dạy..."
              rows="3"
            />
          </div>
        </div>

        <!-- Actions -->
        <div class="prf__form-actions">
          <button
            type="button"
            class="prf__btn prf__btn--cancel"
            :disabled="isSaving"
            @click="toggleEdit"
          >
            Hủy bỏ
          </button>
          <button
            type="submit"
            class="prf__btn prf__btn--save"
            :disabled="isSaving"
          >
            <LucideIcon name="progress_activity" v-if="isSaving"  prf__spinner/>
            {{ isSaving ? 'Đang lưu...' : 'Lưu thay đổi' }}
          </button>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch } from 'vue'

const props = defineProps({
  profile: {
    type: Object,
    default: () => ({})
  }
})

const emit = defineEmits(['save'])

const isEditing = ref(false)
const isSaving = ref(false)

const form = reactive({
  subject: '',
  department: '',
  school: '',
  teacherCode: '',
  hireDate: '',
  bio: ''
})

watch(
  () => props.profile,
  (p) => {
    if (p) {
      form.subject = p.subject || ''
      form.department = p.department || ''
      form.school = p.school || ''
      form.teacherCode = p.teacherCode || p.teacher_id || p.id || ''
      form.hireDate = p.hireDate || p.joiningDate || ''
      form.bio = p.bio || p.bioText || p.description || ''
    }
  },
  { immediate: true, deep: true }
)

const isAllEmpty = computed(() => {
  return !form.subject && !form.department && !form.school && !form.bio
})

const formatDate = (value) => {
  if (!value) return '—'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '—'
  return new Intl.DateTimeFormat('vi-VN').format(date)
}

const toggleEdit = () => {
  isEditing.value = !isEditing.value
  if (isEditing.value) {
    form.subject = props.profile?.subject || ''
    form.department = props.profile?.department || ''
    form.school = props.profile?.school || ''
    form.teacherCode = props.profile?.teacherCode || props.profile?.id || ''
    form.hireDate = props.profile?.hireDate || props.profile?.joiningDate || ''
    form.bio = props.profile?.bio || props.profile?.bioText || props.profile?.description || ''
  }
}

const submitForm = async () => {
  isSaving.value = true
  emit('save', {
    subject: form.subject?.trim() || null,
    department: form.department?.trim() || null,
    school: form.school?.trim() || null,
    teacherCode: form.teacherCode?.trim() || null,
    hireDate: form.hireDate || null,
    bio: form.bio?.trim() || null
  })
  isSaving.value = false
  isEditing.value = false
}

defineExpose({ isSaving, isEditing })
</script>


<style scoped>
.prf {
  background: var(--ds-surface);
  border: 1.5px solid var(--ds-border);
  border-radius: var(--ds-radius-2xl);
  overflow: hidden;
}

.dark .prf {
  border-color: var(--ds-border-strong);
}

/* Header */
.prf__header {
  display: flex;
  align-items: center;
  gap: 0.875rem;
  padding: 1.125rem 1.25rem;
  border-bottom: 1px solid var(--ds-border);
  background: var(--ds-gray-50);
}

.dark .prf__header {
  border-bottom-color: var(--ds-border-strong);
  background: var(--ds-gray-800);
}

.prf__header-icon {
  width: 40px;
  height: 40px;
  border-radius: var(--ds-radius-lg);
  background: rgba(234, 179, 8, 0.08);
  color: var(--ds-warning);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}


.prf__title {
  font-family: var(--ds-font-display);
  font-size: 0.9rem;
  font-weight: 800;
  color: var(--ds-text);
  margin: 0;
}

.dark .prf__title {
  color: #f1f5f9;
}

.prf__subtitle {
  font-size: 0.7rem;
  color: var(--ds-text-muted);
  margin: 0.2rem 0 0;
  font-weight: 500;
}

.prf__edit-toggle {
  margin-left: auto;
  display: inline-flex;
  align-items: center;
  gap: 0.375rem;
  padding: 0.5rem 1rem;
  border-radius: var(--ds-radius-xl);
  border: 1.5px solid var(--ds-border);
  background: var(--ds-surface);
  color: var(--ds-text-secondary);
  font-size: 0.8rem;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.15s ease;
  flex-shrink: 0;
}

.dark .prf__edit-toggle {
  background: var(--ds-gray-700);
  border-color: var(--ds-border-strong);
  color: #94a3b8;
}

.prf__edit-toggle:hover {
  border-color: var(--ds-primary-border);
  color: var(--ds-primary);
  background: var(--ds-primary-soft);
}

.dark .prf__edit-toggle:hover {
  background: rgba(79, 70, 229, 0.1);
}

.prf__edit-toggle--active {
  border-color: var(--ds-danger-border, #fca5a5);
  color: var(--ds-danger);
  background: var(--ds-danger-soft);
}


/* View mode */
.prf__view {
  padding: 1.125rem 1.25rem;
}

.prf__field-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0.875rem;
}

.prf__field {
  display: flex;
  align-items: flex-start;
  gap: 0.75rem;
  padding: 0.75rem;
  border-radius: var(--ds-radius-xl);
  background: var(--ds-gray-50);
  transition: background 0.1s ease;
}

.dark .prf__field {
  background: var(--ds-gray-800);
}

.prf__field:hover {
  background: var(--ds-gray-100);
}

.dark .prf__field:hover {
  background: var(--ds-gray-700);
}

.prf__field--span-2 {
  grid-column: span 2;
}

.prf__field-icon {
  font-size: 1.125rem;
  color: var(--ds-text-muted);
  flex-shrink: 0;
  margin-top: 0.125rem;
}

.prf__field-content {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
  min-width: 0;
}

.prf__field-label {
  font-size: 0.7rem;
  font-weight: 700;
  color: var(--ds-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.06em;
}

.prf__field-value {
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--ds-text);
}

.dark .prf__field-value {
  color: #f1f5f9;
}

/* Empty hint */
.prf__empty-hint {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-top: 1rem;
  padding: 0.75rem 1rem;
  border-radius: var(--ds-radius-xl);
  background: var(--ds-gray-50);
  font-size: 0.8rem;
  color: var(--ds-text-muted);
  font-weight: 500;
}

.dark .prf__empty-hint {
  background: var(--ds-gray-800);
}


/* Edit mode */
.prf__edit {
  padding: 1.125rem 1.25rem;
  background: var(--ds-gray-50);
}

.dark .prf__edit {
  background: var(--ds-gray-800);
}

.prf__form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
}

.prf__form-group {
  display: flex;
  flex-direction: column;
  gap: 0.375rem;
}

.prf__form-group--span-2 {
  grid-column: span 2;
}

.prf__label {
  font-size: 0.8rem;
  font-weight: 700;
  color: var(--ds-text-secondary);
}

.prf__input,
.prf__textarea {
  padding: 0.625rem 0.875rem;
  background: var(--ds-surface);
  border: 1.5px solid var(--ds-border);
  border-radius: var(--ds-radius-xl);
  font-size: 0.875rem;
  color: var(--ds-text);
  outline: none;
  transition: border-color 0.15s ease, box-shadow 0.15s ease;
  font-family: inherit;
  width: 100%;
  box-sizing: border-box;
}

.dark .prf__input,
.dark .prf__textarea {
  background: var(--ds-gray-700);
  border-color: var(--ds-border-strong);
  color: #f1f5f9;
}

.prf__input::placeholder,
.prf__textarea::placeholder {
  color: var(--ds-text-muted);
}

.prf__input:focus,
.prf__textarea:focus {
  border-color: var(--ds-primary);
  box-shadow: 0 0 0 3px var(--ds-primary-ring);
}

.prf__input:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  background: var(--ds-gray-100);
}

.dark .prf__input:disabled {
  background: var(--ds-gray-700);
}

.prf__hint {
  font-size: 0.7rem;
  color: var(--ds-text-muted);
  font-weight: 500;
}

.prf__textarea {
  resize: vertical;
  min-height: 80px;
}

/* Form actions */
.prf__form-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 0.75rem;
  margin-top: 1.25rem;
  padding-top: 1.125rem;
  border-top: 1px solid var(--ds-border);
}

.dark .prf__form-actions {
  border-top-color: var(--ds-border-strong);
}

.prf__btn {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.625rem 1.25rem;
  border-radius: var(--ds-radius-xl);
  font-size: 0.875rem;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.15s ease;
  border: 1.5px solid;
  font-family: inherit;
}

.prf__btn--cancel {
  background: var(--ds-surface);
  border-color: var(--ds-border);
  color: var(--ds-text-secondary);
}

.dark .prf__btn--cancel {
  background: var(--ds-gray-700);
  border-color: var(--ds-border-strong);
  color: #94a3b8;
}

.prf__btn--cancel:hover:not(:disabled) {
  background: var(--ds-gray-100);
  color: var(--ds-text);
}

.dark .prf__btn--cancel:hover:not(:disabled) {
  background: var(--ds-gray-600);
}

.prf__btn--save {
  background: var(--ds-primary);
  border-color: var(--ds-primary);
  color: white;
  box-shadow: 0 2px 8px rgba(79, 70, 229, 0.2);
}

.prf__btn--save:hover:not(:disabled) {
  background: var(--ds-primary-hover, #4338ca);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(79, 70, 229, 0.3);
}

.prf__btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none;
}

.prf__spinner {
  font-size: 1rem;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

/* Responsive */
@media (max-width: 640px) {
  .prf__field-grid {
    grid-template-columns: 1fr;
  }

  .prf__field--span-2 {
    grid-column: span 1;
  }

  .prf__form-grid {
    grid-template-columns: 1fr;
  }

  .prf__form-group--span-2 {
    grid-column: span 1;
  }

  .prf__header {
    flex-wrap: wrap;
  }

  .prf__edit-toggle {
    width: 100%;
    justify-content: center;
  }
}
</style>
