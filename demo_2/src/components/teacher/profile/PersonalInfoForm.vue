<template>
  <div class="pif">
    <!-- Card header -->
    <div class="pif__header">
      <div class="pif__header-icon">
        <LucideIcon name="person" />
      </div>
      <div>
        <h3 class="pif__title">Thông tin cá nhân</h3>
        <p class="pif__subtitle">Cập nhật thông tin họ tên, email, số điện thoại</p>
      </div>
      <button
        type="button"
        class="pif__edit-toggle"
        :class="{ 'pif__edit-toggle--active': isEditing }"
        @click="toggleEdit"
      >
        <LucideIcon :name="isEditing ? 'close' : 'edit'" />
        {{ isEditing ? 'Hủy' : 'Chỉnh sửa' }}
      </button>
    </div>

    <!-- View mode -->
    <div v-if="!isEditing" class="pif__view">
      <div class="pif__field-grid">
        <div class="pif__field">
          <LucideIcon name="badge" pif__field-icon />
          <div class="pif__field-content">
            <span class="pif__field-label">Tên đăng nhập</span>
            <span class="pif__field-value">{{ form.username || '—' }}</span>
          </div>
        </div>

        <div class="pif__field">
          <LucideIcon name="label" pif__field-icon />
          <div class="pif__field-content">
            <span class="pif__field-label">Tên hiển thị</span>
            <span class="pif__field-value">{{ form.displayName || '—' }}</span>
          </div>
        </div>

        <div class="pif__field">
          <LucideIcon name="person" pif__field-icon />
          <div class="pif__field-content">
            <span class="pif__field-label">Họ và tên</span>
            <span class="pif__field-value">{{ form.fullName || '—' }}</span>
          </div>
        </div>

        <div class="pif__field">
          <LucideIcon name="cake" pif__field-icon />
          <div class="pif__field-content">
            <span class="pif__field-label">Ngày sinh</span>
            <span class="pif__field-value">{{ formatDate(form.dateOfBirth) }}</span>
          </div>
        </div>

        <div class="pif__field pif__field--span-2">
          <LucideIcon name="mail" pif__field-icon />
          <div class="pif__field-content">
            <span class="pif__field-label">Email</span>
            <span class="pif__field-value">{{ form.email || '—' }}</span>
          </div>
        </div>

        <div class="pif__field pif__field--span-2">
          <LucideIcon name="phone" pif__field-icon />
          <div class="pif__field-content">
            <span class="pif__field-label">Số điện thoại</span>
            <span class="pif__field-value">{{ form.phone || '—' }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- Edit mode -->
    <div v-else class="pif__edit">
      <form class="pif__form" @submit.prevent="submitForm">
        <div class="pif__form-grid">
          <div class="pif__form-group">
            <label class="pif__label">
              Tên hiển thị
              <span class="pif__required">*</span>
            </label>
            <input
              v-model="form.displayName"
              class="pif__input"
              :class="{ 'pif__input--error': errors.displayName }"
              type="text"
              placeholder="Nhập tên hiển thị"
            />
            <span v-if="errors.displayName" class="pif__error-msg">{{ errors.displayName }}</span>
          </div>

          <div class="pif__form-group">
            <label class="pif__label">Họ và tên</label>
            <input
              v-model="form.fullName"
              class="pif__input"
              type="text"
              placeholder="Nhập họ và tên đầy đủ"
            />
          </div>

          <div class="pif__form-group">
            <label class="pif__label">Ngày sinh</label>
            <input
              v-model="form.dateOfBirth"
              class="pif__input"
              type="date"
            />
          </div>

          <div class="pif__form-group">
            <label class="pif__label">Email</label>
            <input
              v-model="form.email"
              class="pif__input"
              :class="{ 'pif__input--error': errors.email }"
              type="email"
              placeholder="Nhập địa chỉ email"
            />
            <span v-if="errors.email" class="pif__error-msg">{{ errors.email }}</span>
          </div>

          <div class="pif__form-group pif__form-group--span-2">
            <label class="pif__label">Số điện thoại</label>
            <input
              v-model="form.phone"
              class="pif__input pif__input--half"
              type="tel"
              placeholder="Nhập số điện thoại"
            />
          </div>
        </div>

        <!-- Actions -->
        <div class="pif__form-actions">
          <button
            type="button"
            class="pif__btn pif__btn--cancel"
            :disabled="isSaving"
            @click="toggleEdit"
          >
            Hủy bỏ
          </button>
          <button
            type="submit"
            class="pif__btn pif__btn--save"
            :disabled="isSaving"
          >
            <LucideIcon name="progress_activity" v-if="isSaving"  pif__spinner/>
            {{ isSaving ? 'Đang lưu...' : 'Lưu thay đổi' }}
          </button>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, watch } from 'vue'

const props = defineProps({
  profile: {
    type: Object,
    default: () => ({})
  }
})

const emit = defineEmits(['save'])

const isEditing = ref(false)
const isSaving = ref(false)
const errors = ref({})

const form = reactive({
  username: '',
  displayName: '',
  fullName: '',
  dateOfBirth: '',
  email: '',
  phone: ''
})

watch(
  () => props.profile,
  (p) => {
    if (p) {
      form.username = p.username || ''
      form.displayName = p.displayName || ''
      form.fullName = p.fullName || ''
      form.dateOfBirth = p.dateOfBirth || ''
      form.email = p.email || ''
      form.phone = p.phone || ''
    }
  },
  { immediate: true, deep: true }
)

const formatDate = (value) => {
  if (!value) return '—'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '—'
  return new Intl.DateTimeFormat('vi-VN').format(date)
}

const isValidEmail = (email) => {
  if (!email) return true
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)
}

const isValidPhone = (phone) => {
  if (!phone) return true
  const normalized = phone.replace(/[\s.-]/g, '')
  return /^(0[1-9][0-9]{8,9})$/.test(normalized)
}

const validate = () => {
  errors.value = {}
  if (!form.displayName?.trim()) {
    errors.value.displayName = 'Tên hiển thị không được để trống.'
  }
  if (form.email && !isValidEmail(form.email)) {
    errors.value.email = 'Định dạng email không hợp lệ. Vui lòng nhập email đúng định dạng (ví dụ: name@example.com).'
  }
  if (form.phone && !isValidPhone(form.phone)) {
    errors.value.phone = 'Số điện thoại không hợp lệ. Vui lòng nhập số điện thoại Việt Nam (VD: 0912345678).'
  }
  return Object.keys(errors.value).length === 0
}

const toggleEdit = () => {
  isEditing.value = !isEditing.value
  if (isEditing.value) {
    form.displayName = props.profile?.displayName || ''
    form.fullName = props.profile?.fullName || ''
    form.dateOfBirth = props.profile?.dateOfBirth || ''
    form.email = props.profile?.email || ''
    form.phone = props.profile?.phone || ''
    errors.value = {}
  }
}

const submitForm = async () => {
  if (!validate()) return
  isSaving.value = true
  emit('save', {
    displayName: form.displayName?.trim() || null,
    fullName: form.fullName?.trim() || null,
    dateOfBirth: form.dateOfBirth || null,
    email: form.email?.trim() || null,
    phone: form.phone?.trim() || null,
    avatarUrl: props.profile?.avatarUrl || null
  })
  isSaving.value = false
  isEditing.value = false
}

// expose for parent to call when save succeeds
defineExpose({ isSaving, isEditing })
</script>


<style scoped>
.pif {
  background: var(--ds-surface);
  border: 1.5px solid var(--ds-border);
  border-radius: var(--ds-radius-2xl);
  overflow: hidden;
}

.dark .pif {
  border-color: var(--ds-border-strong);
}

/* Header */
.pif__header {
  display: flex;
  align-items: center;
  gap: 0.875rem;
  padding: 1.125rem 1.25rem;
  border-bottom: 1px solid var(--ds-border);
  background: var(--ds-gray-50);
}

.dark .pif__header {
  border-bottom-color: var(--ds-border-strong);
  background: var(--ds-gray-800);
}

.pif__header-icon {
  width: 40px;
  height: 40px;
  border-radius: var(--ds-radius-lg);
  background: var(--ds-primary-soft);
  color: var(--ds-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}


.pif__title {
  font-family: var(--ds-font-display);
  font-size: 0.9rem;
  font-weight: 800;
  color: var(--ds-text);
  margin: 0;
}

.dark .pif__title {
  color: #f1f5f9;
}

.pif__subtitle {
  font-size: 0.7rem;
  color: var(--ds-text-muted);
  margin: 0.2rem 0 0;
  font-weight: 500;
}

.pif__edit-toggle {
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

.dark .pif__edit-toggle {
  background: var(--ds-gray-700);
  border-color: var(--ds-border-strong);
  color: #94a3b8;
}

.pif__edit-toggle:hover {
  border-color: var(--ds-primary-border);
  color: var(--ds-primary);
  background: var(--ds-primary-soft);
}

.dark .pif__edit-toggle:hover {
  background: rgba(79, 70, 229, 0.1);
}

.pif__edit-toggle--active {
  border-color: var(--ds-danger-border, #fca5a5);
  color: var(--ds-danger);
  background: var(--ds-danger-soft);
}


/* View mode */
.pif__view {
  padding: 1.125rem 1.25rem;
}

.pif__field-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
}

.pif__field {
  display: flex;
  align-items: flex-start;
  gap: 0.75rem;
  padding: 0.75rem;
  border-radius: var(--ds-radius-xl);
  background: var(--ds-gray-50);
  transition: background 0.1s ease;
}

.dark .pif__field {
  background: var(--ds-gray-800);
}

.pif__field:hover {
  background: var(--ds-gray-100);
}

.dark .pif__field:hover {
  background: var(--ds-gray-700);
}

.pif__field--span-2 {
  grid-column: span 2;
}

.pif__field-icon {
  font-size: 1.125rem;
  color: var(--ds-text-muted);
  flex-shrink: 0;
  margin-top: 0.125rem;
}

.pif__field-content {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
  min-width: 0;
}

.pif__field-label {
  font-size: 0.7rem;
  font-weight: 700;
  color: var(--ds-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.06em;
}

.pif__field-value {
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--ds-text);
}

.dark .pif__field-value {
  color: #f1f5f9;
}

/* Edit mode */
.pif__edit {
  padding: 1.125rem 1.25rem;
  background: var(--ds-gray-50);
}

.dark .pif__edit {
  background: var(--ds-gray-800);
}

.pif__form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
}

.pif__form-group {
  display: flex;
  flex-direction: column;
  gap: 0.375rem;
}

.pif__form-group--span-2 {
  grid-column: span 2;
}

.pif__label {
  font-size: 0.8rem;
  font-weight: 700;
  color: var(--ds-text-secondary);
}

.pif__required {
  color: var(--ds-danger);
}

.pif__input {
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

.dark .pif__input {
  background: var(--ds-gray-700);
  border-color: var(--ds-border-strong);
  color: #f1f5f9;
}

.pif__input::placeholder {
  color: var(--ds-text-muted);
}

.pif__input:focus {
  border-color: var(--ds-primary);
  box-shadow: 0 0 0 3px var(--ds-primary-ring);
}

.dark .pif__input:focus {
  border-color: var(--ds-primary);
  box-shadow: 0 0 0 3px var(--ds-primary-ring);
}

.pif__input--error {
  border-color: var(--ds-danger);
}

.pif__input--error:focus {
  box-shadow: 0 0 0 3px var(--ds-danger-ring, rgba(220, 38, 38, 0.2));
}

.pif__input--half {
  max-width: 50%;
}

.pif__error-msg {
  font-size: 0.7rem;
  color: var(--ds-danger);
  font-weight: 600;
}

/* Form actions */
.pif__form-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 0.75rem;
  margin-top: 1.25rem;
  padding-top: 1.125rem;
  border-top: 1px solid var(--ds-border);
}

.dark .pif__form-actions {
  border-top-color: var(--ds-border-strong);
}

.pif__btn {
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

.pif__btn--cancel {
  background: var(--ds-surface);
  border-color: var(--ds-border);
  color: var(--ds-text-secondary);
}

.dark .pif__btn--cancel {
  background: var(--ds-gray-700);
  border-color: var(--ds-border-strong);
  color: #94a3b8;
}

.pif__btn--cancel:hover:not(:disabled) {
  background: var(--ds-gray-100);
  color: var(--ds-text);
}

.dark .pif__btn--cancel:hover:not(:disabled) {
  background: var(--ds-gray-600);
}

.pif__btn--save {
  background: var(--ds-primary);
  border-color: var(--ds-primary);
  color: white;
  box-shadow: 0 2px 8px rgba(139, 75, 0, 0.18);
}

.pif__btn--save:hover:not(:disabled) {
  background: var(--ds-primary-hover);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(79, 70, 229, 0.3);
}

.pif__btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none;
}

.pif__spinner {
  font-size: 1rem;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

/* Responsive */
@media (max-width: 640px) {
  .pif__field-grid {
    grid-template-columns: 1fr;
  }

  .pif__field--span-2 {
    grid-column: span 1;
  }

  .pif__form-grid {
    grid-template-columns: 1fr;
  }

  .pif__form-group--span-2 {
    grid-column: span 1;
  }

  .pif__input--half {
    max-width: 100%;
  }

  .pif__header {
    flex-wrap: wrap;
  }

  .pif__edit-toggle {
    width: 100%;
    justify-content: center;
  }
}
</style>
