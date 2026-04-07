<template>
  <Modal :model-value="modelValue" :title="''" size="md" @update:model-value="$emit('update:modelValue', $event)">
    <template #header>
      <div class="acm__header">
        <div class="acm__icon" :class="`acm__icon--${variant}`">
          <LucideIcon :name="icon" />
        </div>
        <div>
          <h3 class="acm__title">{{ title }}</h3>
          <p v-if="studentName" class="acm__subtitle">Học sinh: {{ studentName }}</p>
        </div>
      </div>
    </template>

    <div class="acm__body">
      <label class="acm__label">Lý do (tùy chọn)</label>
      <textarea
        v-model="localReason"
        rows="3"
        class="acm__textarea"
        :placeholder="placeholder"
      />
    </div>

    <template #footer>
      <div class="acm__footer">
        <button type="button" class="acm__btn acm__btn--ghost" @click="handleCancel">
          Hủy
        </button>
        <button type="button" class="acm__btn" :class="`acm__btn--${variant}`" :disabled="loading" @click="handleConfirm">
          <LucideIcon :name="icon" />
          {{ actionLabel }}
        </button>
      </div>
    </template>
  </Modal>
</template>

<script setup>
import { ref, watch } from 'vue'
import Modal from '../../ui/Modal.vue'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  variant: {
    type: String,
    default: 'warning',
    validator: (v) => ['warning', 'danger', 'info'].includes(v)
  },
  title: { type: String, default: '' },
  studentName: { type: String, default: '' },
  placeholder: { type: String, default: 'Nhập lý do...' },
  actionLabel: { type: String, default: 'Xác nhận' },
  loading: { type: Boolean, default: false }
})

const emit = defineEmits([
  'update:modelValue',
  'confirm'
])

const localReason = ref('')

// Reset reason when modal opens
watch(() => props.modelValue, (open) => {
  if (open) localReason.value = ''
})

const iconMap = { warning: 'warning', danger: 'block', info: 'info' }
const icon = iconMap[props.variant] || 'warning'

const handleCancel = () => {
  emit('update:modelValue', false)
}

const handleConfirm = () => {
  emit('confirm', localReason.value.trim())
}
</script>

<style scoped>
.acm__header {
  display: flex;
  align-items: flex-start;
  gap: 0.875rem;
}

.acm__icon {
  width: 44px;
  height: 44px;
  border-radius: var(--ds-radius-xl);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.acm__icon--warning { background: var(--ds-warning-soft); color: var(--ds-warning); }
.acm__icon--danger { background: var(--ds-danger-soft); color: var(--ds-danger); }
.acm__icon--info { background: var(--ds-info-soft); color: var(--ds-info); }

.acm__title {
  font-family: var(--ds-font-display);
  font-size: 1rem;
  font-weight: 800;
  color: var(--ds-text);
  margin: 0;
}

.dark .acm__title { color: var(--ds-text); }

.acm__subtitle {
  font-size: 0.8rem;
  color: var(--ds-text-muted);
  margin: 0.25rem 0 0;
}

.acm__body {
  padding-top: 0.5rem;
}

.acm__label {
  display: block;
  font-size: 0.8rem;
  font-weight: 700;
  color: var(--ds-text-secondary);
  margin-bottom: 0.5rem;
}

.dark .acm__label { color: var(--ds-text-muted); }

.acm__textarea {
  width: 100%;
  padding: 0.6875rem 1rem;
  background: var(--ds-surface);
  border: 1.5px solid var(--ds-border);
  border-radius: var(--ds-radius-xl);
  font-size: 0.875rem;
  color: var(--ds-text);
  outline: none;
  resize: vertical;
  transition: color 0.15s ease, background-color 0.15s ease, border-color 0.15s ease, box-shadow 0.15s ease, transform 0.15s ease;
  font-family: inherit;
  line-height: 1.5;
  min-height: 80px;
}

.dark .acm__textarea {
  background: var(--ds-gray-800);
  border-color: var(--ds-border-strong);
  color: var(--ds-text);
}

.acm__textarea::placeholder { color: var(--ds-text-muted); }
.dark .acm__textarea::placeholder { color: var(--ds-gray-500); }

.acm__textarea:focus {
  border-color: var(--ds-primary);
  box-shadow: 0 0 0 3px var(--ds-primary-ring);
}

.acm__footer {
  display: flex;
  gap: 0.75rem;
  justify-content: flex-end;
}

.acm__btn {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.625rem 1.25rem;
  border-radius: var(--ds-radius-xl);
  font-size: 0.875rem;
  font-weight: 700;
  cursor: pointer;
  transition: color 0.15s ease, background-color 0.15s ease, border-color 0.15s ease, box-shadow 0.15s ease, transform 0.15s ease;
  border: 1px solid transparent;
  font-family: inherit;
}

.acm__btn--ghost {
  background: transparent;
  border-color: var(--ds-border);
  color: var(--ds-text-secondary);
}

.dark .acm__btn--ghost {
  background: var(--ds-gray-800);
  border-color: var(--ds-border-strong);
  color: var(--ds-text-muted);
}

.acm__btn--ghost:hover {
  border-color: var(--ds-gray-300);
  color: var(--ds-text);
}

.dark .acm__btn--ghost:hover {
  background: var(--ds-gray-700);
}

.acm__btn--warning {
  background: var(--ds-warning);
  color: white;
  box-shadow: 0 4px 12px rgba(234, 179, 8, 0.25);
}

.acm__btn--warning:hover:not(:disabled) {
  background: #b45309;
  transform: translateY(-1px);
}

.acm__btn--danger {
  background: var(--ds-danger);
  color: white;
  box-shadow: 0 4px 12px rgba(220, 38, 38, 0.25);
}

.acm__btn--danger:hover:not(:disabled) {
  background: #b91c1c;
  transform: translateY(-1px);
}

.acm__btn--info {
  background: var(--ds-info);
  color: white;
  box-shadow: 0 4px 12px rgba(14, 165, 233, 0.25);
}

.acm__btn--info:hover:not(:disabled) {
  background: #0284c7;
  transform: translateY(-1px);
}

.acm__btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  transform: none !important;
}
</style>