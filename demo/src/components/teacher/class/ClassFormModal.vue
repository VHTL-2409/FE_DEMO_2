<template>
  <Transition name="cfm-modal">
    <div v-if="modelValue" class="cfm-overlay" @click.self="close">
      <div class="cfm-modal">
        <div class="cfm-modal__header">
          <div class="cfm-modal__icon">
            <LucideIcon name="book-open" />
          </div>
          <div>
            <h3 class="cfm-modal__title">{{ classItem ? 'Chỉnh sửa lớp học' : 'Tạo lớp học mới' }}</h3>
            <p class="cfm-modal__subtitle">{{ classItem ? 'Cập nhật thông tin lớp học' : 'Điền thông tin để tạo lớp học mới' }}</p>
          </div>
          <button type="button" class="cfm-modal__close" @click="close">
            <LucideIcon name="x" />
          </button>
        </div>

        <form class="cfm-modal__body" @submit.prevent="handleSubmit">
          <div class="cfm-form-group">
            <label class="cfm-label">Tên lớp <span class="cfm-required">*</span></label>
            <input
              v-model="form.name"
              type="text"
              class="cfm-input"
              placeholder="VD: 10A1, Lớp Toán nâng cao..."
              maxlength="100"
              required
            />
            <span class="cfm-hint">{{ form.name?.length || 0 }}/100 ký tự</span>
          </div>

          <div class="cfm-form-group">
            <label class="cfm-label">Môn học</label>
            <input
              v-model="form.subject"
              type="text"
              class="cfm-input"
              placeholder="VD: Toán, Văn, Anh..."
              maxlength="100"
            />
          </div>

          <div class="cfm-form-group">
            <label class="cfm-label">Mô tả</label>
            <textarea
              v-model="form.description"
              class="cfm-textarea"
              placeholder="Mô tả ngắn về lớp học..."
              rows="3"
              maxlength="500"
            ></textarea>
            <span class="cfm-hint">{{ form.description?.length || 0 }}/500 ký tự</span>
          </div>
        </form>

        <div class="cfm-modal__footer">
          <button type="button" class="cfm-btn cfm-btn--outline" @click="close">Hủy</button>
          <button type="button" class="cfm-btn cfm-btn--primary" :disabled="loading || !form.name?.trim()" @click="handleSubmit">
            <span v-if="loading" class="cfm-spinner"></span>
            <template v-else>
              <LucideIcon name="check" />
              {{ classItem ? 'Lưu thay đổi' : 'Tạo lớp học' }}
            </template>
          </button>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup>
import { ref, watch } from 'vue'
import LucideIcon from '../../common/LucideIcon.vue'

const props = defineProps({
  modelValue: Boolean,
  classItem: Object,
  loading: Boolean
})

const emit = defineEmits(['update:modelValue', 'submit'])

const form = ref({
  name: '',
  subject: '',
  description: ''
})

watch(() => props.modelValue, (val) => {
  if (val) {
    form.value = {
      name: props.classItem?.name || '',
      subject: props.classItem?.subject || '',
      description: props.classItem?.description || ''
    }
  }
})

watch(() => props.classItem, (val) => {
  if (val && props.modelValue) {
    form.value = {
      name: val.name || '',
      subject: val.subject || '',
      description: val.description || ''
    }
  }
})

const close = () => {
  emit('update:modelValue', false)
}

const handleSubmit = () => {
  if (!form.value.name?.trim()) return
  emit('submit', {
    name: form.value.name.trim(),
    subject: form.value.subject?.trim() || null,
    description: form.value.description?.trim() || null
  })
}
</script>

<style scoped>
.cfm-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,0.4);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: 1.5rem;
}

.cfm-modal {
  background: white;
  border-radius: var(--ds-radius-2xl);
  width: 100%;
  max-width: 480px;
  box-shadow: 0 24px 64px rgba(0,0,0,0.2);
  overflow: hidden;
}

.dark .cfm-modal { background: var(--ds-gray-800); }

.cfm-modal__header {
  display: flex;
  align-items: flex-start;
  gap: 0.875rem;
  padding: 1.5rem;
  border-bottom: 1px solid var(--ds-border);
}

.dark .cfm-modal__header { border-bottom-color: var(--ds-border-strong); }

.cfm-modal__icon {
  width: 44px;
  height: 44px;
  border-radius: var(--ds-radius-xl);
  background: var(--ds-primary-soft);
  color: var(--ds-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.cfm-modal__title {
  font-family: var(--ds-font-display);
  font-size: 1rem;
  font-weight: 800;
  color: var(--ds-text);
  margin: 0;
  line-height: 1.3;
}

.dark .cfm-modal__title { color: #f1f5f9; }

.cfm-modal__subtitle {
  font-size: 0.8rem;
  color: var(--ds-text-muted);
  margin: 0.25rem 0 0;
}

.cfm-modal__close {
  margin-left: auto;
  width: 32px;
  height: 32px;
  border: none;
  background: var(--ds-gray-100);
  color: var(--ds-text-muted);
  border-radius: var(--ds-radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: color 0.12s ease, background-color 0.12s ease, border-color 0.12s ease, box-shadow 0.12s ease, transform 0.12s ease;
  flex-shrink: 0;
}

.dark .cfm-modal__close { background: var(--ds-gray-700); }
.cfm-modal__close:hover { background: var(--ds-gray-200); color: var(--ds-text); }
.dark .cfm-modal__close:hover { background: var(--ds-gray-600); }

.cfm-modal__body {
  padding: 1.5rem;
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.cfm-form-group {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.cfm-label {
  font-size: 0.8rem;
  font-weight: 700;
  color: var(--ds-text);
}

.dark .cfm-label { color: #f1f5f9; }

.cfm-required {
  color: var(--ds-danger);
}

.cfm-input,
.cfm-textarea {
  width: 100%;
  padding: 0.875rem 1.5rem;
  border: 2px solid var(--ds-border);
  border-radius: var(--ds-radius-3xl);
  font-size: 0.9375rem;
  background: var(--ds-surface);
  color: var(--ds-text);
  transition: color 0.25s cubic-bezier(0.4, 0, 0.2, 1), background-color 0.25s cubic-bezier(0.4, 0, 0.2, 1), border-color 0.25s cubic-bezier(0.4, 0, 0.2, 1), box-shadow 0.25s cubic-bezier(0.4, 0, 0.2, 1), transform 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  font-family: inherit;
  resize: vertical;
  line-height: 1.5;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.dark .cfm-input,
.dark .cfm-textarea {
  background: var(--ds-gray-800);
  border-color: var(--ds-border-strong);
  color: #f1f5f9;
}

.cfm-input:hover,
.cfm-textarea:hover {
  border-color: var(--ds-primary-border);
  box-shadow: 0 4px 16px rgba(79, 70, 229, 0.1);
}

.dark .cfm-input:hover,
.dark .cfm-textarea:hover {
  border-color: var(--ds-primary-border);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);
}

.cfm-input:focus,
.cfm-textarea:focus {
  outline: none;
  border-color: var(--ds-primary);
  box-shadow: 0 0 0 4px var(--ds-primary-ring), 0 8px 24px rgba(79, 70, 229, 0.15);
  transform: translateY(-1px);
}

.dark .cfm-input:focus,
.dark .cfm-textarea:focus {
  box-shadow: 0 0 0 4px rgba(79, 70, 229, 0.3), 0 8px 24px rgba(0, 0, 0, 0.3);
}

.cfm-hint {
  font-size: 0.7rem;
  color: var(--ds-text-muted);
  text-align: right;
}

.dark .cfm-hint { color: #94a3b8; }

.cfm-modal__footer {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 0.75rem;
  padding: 1.25rem 1.5rem;
  border-top: 1px solid var(--ds-border);
  background: var(--ds-gray-50);
}

.dark .cfm-modal__footer {
  border-top-color: var(--ds-border-strong);
  background: var(--ds-gray-800);
}

.cfm-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.625rem 1.125rem;
  border-radius: var(--ds-radius-xl);
  font-size: 0.875rem;
  font-weight: 700;
  cursor: pointer;
  transition: color 0.15s ease, background-color 0.15s ease, border-color 0.15s ease, box-shadow 0.15s ease, transform 0.15s ease;
  border: 1.5px solid transparent;
  white-space: nowrap;
}

.cfm-btn--primary {
  background: linear-gradient(135deg, var(--ds-primary) 0%, #6366f1 100%);
  color: white;
  box-shadow: 0 4px 12px rgba(79, 70, 229, 0.25);
}

.cfm-btn--primary:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 6px 20px rgba(79, 70, 229, 0.35);
}

.cfm-btn--outline {
  background: var(--ds-surface);
  border-color: var(--ds-border);
  color: var(--ds-text-secondary);
}

.dark .cfm-btn--outline {
  background: var(--ds-gray-700);
  border-color: var(--ds-border-strong);
  color: #94a3b8;
}

.cfm-btn--outline:hover { border-color: var(--ds-primary); color: var(--ds-primary); }
.cfm-btn:disabled { opacity: 0.5; cursor: not-allowed; transform: none !important; }

.cfm-spinner {
  width: 16px;
  height: 16px;
  border: 2px solid rgba(255,255,255,0.3);
  border-top-color: white;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin { to { transform: rotate(360deg) translateZ(0); } }

/* Transition */
.cfm-modal-enter-active,
.cfm-modal-leave-active {
  transition: color 0.2s ease, background-color 0.2s ease, border-color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease;
}

.cfm-modal-enter-from,
.cfm-modal-leave-to {
  opacity: 0;
}

.cfm-modal-enter-from .cfm-modal,
.cfm-modal-leave-to .cfm-modal {
  transform: scale(0.95) translateY(10px);
}
</style>