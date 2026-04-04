<template>
  <Transition name="modal">
    <div v-if="modelValue" class="sjcm-overlay" @click.self="handleClose">
      <div class="sjcm">
        <!-- Header -->
        <div class="sjcm__header">
          <div class="sjcm__header-icon">
            <LucideIcon name="school" />
          </div>
          <div class="sjcm__header-text">
            <h2 class="sjcm__title">Tham gia lớp học</h2>
            <p class="sjcm__subtitle">Nhập mã lớp được chia sẻ bởi giáo viên</p>
          </div>
          <button type="button" class="sjcm__close" @click="handleClose" :disabled="isJoining">
            <LucideIcon name="x" />
          </button>
        </div>

        <!-- Body -->
        <div class="sjcm__body">
          <!-- Error Alert -->
          <div v-if="error" class="sjcm__alert sjcm__alert--error">
            <LucideIcon name="alert-circle" />
            <span>{{ error }}</span>
          </div>

          <!-- Success Alert -->
          <div v-if="success" class="sjcm__alert sjcm__alert--success">
            <LucideIcon name="check_circle" />
            <span>{{ success }}</span>
          </div>

          <!-- Class Code Input -->
          <div class="sjcm__field">
            <label class="sjcm__label">Mã lớp học</label>
            <div class="sjcm__input-wrap">
              <LucideIcon name="hash" class="sjcm__input-icon" />
              <input
                ref="codeInput"
                v-model="classCode"
                type="text"
                class="sjcm__input"
                placeholder="Ví dụ: ABC123"
                maxlength="20"
                :disabled="isJoining"
                @keyup.enter="handleJoin"
                @input="error = ''; success = ''"
              />
            </div>
            <p class="sjcm__hint">Mã lớp có thể là chuỗi chữ và số, không phân biệt hoa thường</p>
          </div>
        </div>

        <!-- Footer -->
        <div class="sjcm__footer">
          <button
            type="button"
            class="sjcm__btn sjcm__btn--outline"
            @click="handleClose"
            :disabled="isJoining"
          >
            Hủy
          </button>
          <button
            type="button"
            class="sjcm__btn sjcm__btn--primary"
            @click="handleJoin"
            :disabled="isJoining || !classCode.trim()"
          >
            <div v-if="isJoining" class="sjcm__spinner"></div>
            <template v-else>
              <LucideIcon name="login" />
              Tham gia
            </template>
          </button>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup>
import { ref, watch, nextTick } from 'vue'
import { joinClassByCode } from '../../../services/classService'
import { ApiError } from '../../../services/apiClient'
import { useToast } from '../../../composables/useToast'
import LucideIcon from '../../common/LucideIcon.vue'

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:modelValue', 'joined'])

const toast = useToast()

const classCode = ref('')
const isJoining = ref(false)
const error = ref('')
const success = ref('')
const codeInput = ref(null)

// Focus input when modal opens
watch(() => props.modelValue, async (val) => {
  if (val) {
    await nextTick()
    codeInput.value?.focus()
    classCode.value = ''
    error.value = ''
    success.value = ''
  }
})

const handleClose = () => {
  if (isJoining.value) return
  emit('update:modelValue', false)
}

const handleJoin = async () => {
  const code = classCode.value.trim()
  if (!code) {
    error.value = 'Vui lòng nhập mã lớp học.'
    return
  }

  isJoining.value = true
  error.value = ''
  success.value = ''

  try {
    const result = await joinClassByCode(code)
    if (result) {
      toast.success(`Đã tham gia lớp "${result.name || code}"!`)
      setTimeout(() => {
        emit('joined', result)
        emit('update:modelValue', false)
      }, 1500)
    } else {
      error.value = 'Không thể tham gia lớp. Vui lòng thử lại.'
    }
  } catch (err) {
    if (err instanceof ApiError) {
      error.value = err.message || 'Mã lớp không hợp lệ hoặc lớp học không tồn tại.'
    } else {
      error.value = 'Đã xảy ra lỗi. Vui lòng thử lại.'
    }
  } finally {
    isJoining.value = false
  }
}
</script>

<style scoped>
.sjcm-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(8px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: 1.5rem;
}

.sjcm {
  background: var(--ds-surface);
  border-radius: var(--ds-radius-2xl);
  width: 100%;
  max-width: 440px;
  box-shadow: 0 24px 64px rgba(0, 0, 0, 0.25);
  overflow: hidden;
}

.dark .sjcm { background: var(--ds-gray-800); }

/* Header */
.sjcm__header {
  display: flex;
  align-items: flex-start;
  gap: 1rem;
  padding: 1.5rem;
  border-bottom: 1px solid var(--ds-border);
}

.dark .sjcm__header { border-bottom-color: var(--ds-border-strong); }

.sjcm__header-icon {
  width: 48px;
  height: 48px;
  border-radius: var(--ds-radius-xl);
  background: var(--ds-primary-soft);
  color: var(--ds-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  box-shadow: 0 4px 12px rgba(79, 70, 229, 0.2);
}

.sjcm__header-text {
  flex: 1;
}

.sjcm__title {
  font-family: var(--ds-font-display);
  font-size: 1.125rem;
  font-weight: 800;
  color: var(--ds-text);
  margin: 0;
  line-height: 1.3;
}

.dark .sjcm__title { color: var(--ds-text); }

.sjcm__subtitle {
  font-size: 0.875rem;
  color: var(--ds-text-muted);
  margin: 0.25rem 0 0;
}

.sjcm__close {
  width: 36px;
  height: 36px;
  border: none;
  background: var(--ds-gray-100);
  border-radius: var(--ds-radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: var(--ds-text-muted);
  transition: color 0.15s ease, background-color 0.15s ease, border-color 0.15s ease, box-shadow 0.15s ease, transform 0.15s ease;
  flex-shrink: 0;
}

.dark .sjcm__close { background: var(--ds-gray-700); color: var(--ds-text-muted); }
.sjcm__close:hover:not(:disabled) { background: var(--ds-gray-200); color: var(--ds-text); }
.sjcm__close:disabled { opacity: 0.5; cursor: not-allowed; }

/* Body */
.sjcm__body {
  padding: 1.5rem;
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

/* Alerts */
.sjcm__alert {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.875rem 1rem;
  border-radius: var(--ds-radius-xl);
  font-size: 0.875rem;
  font-weight: 600;
  animation: slideDown 0.2s ease;
}

@keyframes slideDown {
  from { opacity: 0; transform: translateY(-8px); }
  to { opacity: 1; transform: translateY(0); }
}

.sjcm__alert--error {
  background: var(--ds-danger-soft);
  color: var(--ds-danger);
}

.sjcm__alert--success {
  background: var(--ds-success-soft);
  color: var(--ds-success);
}

/* Field */
.sjcm__field {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.sjcm__label {
  font-size: 0.875rem;
  font-weight: 700;
  color: var(--ds-text);
}

.dark .sjcm__label { color: var(--ds-text); }

.sjcm__input-wrap {
  position: relative;
}

.sjcm__input-icon {
  position: absolute;
  left: 1rem;
  top: 50%;
  transform: translateY(-50%);
  color: var(--ds-text-muted);
  pointer-events: none;
}

.sjcm__input {
  width: 100%;
  padding: 0.875rem 1rem 0.875rem 3rem;
  border: 2px solid var(--ds-border);
  border-radius: var(--ds-radius-xl);
  font-size: 1rem;
  font-weight: 600;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  background: var(--ds-surface);
  color: var(--ds-text);
  transition: color 0.25s cubic-bezier(0.4, 0, 0.2, 1), background-color 0.25s cubic-bezier(0.4, 0, 0.2, 1), border-color 0.25s cubic-bezier(0.4, 0, 0.2, 1), box-shadow 0.25s cubic-bezier(0.4, 0, 0.2, 1), transform 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  text-align: center;
}

.dark .sjcm__input {
  background: var(--ds-gray-800);
  border-color: var(--ds-border-strong);
  color: var(--ds-text);
}

.sjcm__input::placeholder {
  text-transform: none;
  letter-spacing: normal;
  font-weight: 400;
  color: var(--ds-text-muted);
}

.sjcm__input:focus {
  outline: none;
  border-color: var(--ds-primary);
  box-shadow: 0 0 0 4px var(--ds-primary-ring);
}

.sjcm__input:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.sjcm__hint {
  font-size: 0.8rem;
  color: var(--ds-text-muted);
  margin: 0;
}

/* Footer */
.sjcm__footer {
  padding: 1rem 1.5rem;
  border-top: 1px solid var(--ds-border);
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
}

.dark .sjcm__footer { border-top-color: var(--ds-border-strong); }

/* Buttons */
.sjcm__btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  padding: 0.75rem 1.25rem;
  border-radius: var(--ds-radius-xl);
  font-size: 0.875rem;
  font-weight: 700;
  cursor: pointer;
  transition: color 0.2s ease, background-color 0.2s ease, border-color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease cubic-bezier(0.4, 0, 0.2, 1);
  border: 1.5px solid transparent;
  min-width: 100px;
}

.sjcm__btn--primary {
  background: linear-gradient(135deg, var(--ds-primary) 0%, #6366f1 100%);
  color: white;
  box-shadow: 0 4px 16px rgba(79, 70, 229, 0.25);
}

.sjcm__btn--primary:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(79, 70, 229, 0.35);
}

.sjcm__btn--outline {
  background: var(--ds-surface);
  border-color: var(--ds-border);
  color: var(--ds-text-secondary);
}

.dark .sjcm__btn--outline {
  background: var(--ds-gray-800);
  border-color: var(--ds-border-strong);
  color: var(--ds-text-muted);
}

.sjcm__btn--outline:hover:not(:disabled) {
  border-color: var(--ds-primary);
  color: var(--ds-primary);
}

.sjcm__btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  transform: none !important;
  box-shadow: none !important;
}

/* Spinner */
.sjcm__spinner {
  width: 18px;
  height: 18px;
  border: 2.5px solid rgba(255, 255, 255, 0.3);
  border-top-color: white;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin { to { transform: rotate(360deg); } }

/* Modal Transitions */
.modal-enter-active,
.modal-leave-active {
  transition: color 0.2s ease, background-color 0.2s ease, border-color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease;
}

.modal-enter-from,
.modal-leave-to {
  opacity: 0;
}

.modal-enter-from .sjcm,
.modal-leave-to .sjcm {
  transform: scale(0.95) translateY(8px);
}

/* Responsive */
@media (max-width: 480px) {
  .sjcm-overlay {
    padding: 1rem;
    align-items: flex-end;
  }

  .sjcm {
    border-radius: var(--ds-radius-2xl) var(--ds-radius-2xl) 0 0;
    max-width: 100%;
  }

  .sjcm__footer {
    flex-direction: column-reverse;
  }

  .sjcm__btn {
    width: 100%;
  }
}
</style>
@media (prefers-reduced-motion: reduce) {
  * {
    transition-duration: 0.01ms !important;
    animation-duration: 0.01ms !important;
  }
}