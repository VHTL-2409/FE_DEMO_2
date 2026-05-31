<template>
  <Modal v-model="open" size="md" :persistent="isJoining">
    <template #header="{ titleId }">
      <div class="sjcm__header">
        <div class="sjcm__header-icon">
          <LucideIcon name="school" />
        </div>
        <div class="sjcm__header-text">
          <h2 :id="titleId" class="sjcm__title">Tham gia lớp học</h2>
          <p class="sjcm__subtitle">Nhập mã lớp được chia sẻ bởi giáo viên</p>
        </div>
      </div>
    </template>

    <div class="sjcm__body">
      <div v-if="error" class="sjcm__alert sjcm__alert--error">
        <LucideIcon name="alert-circle" />
        <span>{{ error }}</span>
      </div>

      <div class="sjcm__field">
        <label class="sjcm__label" for="student-join-class-code">Mã lớp học</label>
        <div class="sjcm__input-wrap">
          <LucideIcon name="hash" class="sjcm__input-icon" />
          <input
            id="student-join-class-code"
            ref="codeInput"
            v-model="classCode"
            type="text"
            class="sjcm__input"
            placeholder="Ví dụ: ABC123"
            maxlength="20"
            :disabled="isJoining"
            @keyup.enter="handleJoin"
            @input="handleInput"
          />
        </div>
      </div>
    </div>

    <template #footer>
      <div class="sjcm__footer">
        <button
          type="button"
          class="sjcm__btn sjcm__btn--outline"
          :disabled="isJoining"
          @click="handleClose"
        >
          Hủy
        </button>
        <button
          type="button"
          class="sjcm__btn sjcm__btn--primary"
          :disabled="isJoining || !classCode.trim()"
          @click="handleJoin"
        >
          <div v-if="isJoining" class="sjcm__spinner" />
          <template v-else>
            <LucideIcon name="login" />
            Tham gia
          </template>
        </button>
      </div>
    </template>
  </Modal>
</template>

<script setup>
import { computed, nextTick, ref, watch } from 'vue'
import Modal from '../../ui/Modal.vue'
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
const codeInput = ref(null)

const open = computed({
  get: () => props.modelValue,
  set: (value) => {
    if (!value && isJoining.value) return
    emit('update:modelValue', value)
  }
})

watch(open, async (value) => {
  if (!value) return
  classCode.value = ''
  error.value = ''
  await nextTick()
  codeInput.value?.focus()
})

const handleClose = () => {
  if (isJoining.value) return
  emit('update:modelValue', false)
}

const handleInput = () => {
  error.value = ''
}

const handleJoin = async () => {
  const code = classCode.value.trim().toUpperCase()
  if (!code) {
    error.value = 'Vui lòng nhập mã lớp học.'
    return
  }

  isJoining.value = true
  error.value = ''
  classCode.value = code

  try {
    const result = await joinClassByCode(code)
    if (result) {
      const classLabel = result.name || result.className || code
      toast.success(`Đã tham gia lớp "${classLabel}"!`)
      emit('joined', result)
      emit('update:modelValue', false)
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
.sjcm__header {
  display: flex;
  align-items: flex-start;
  gap: 1rem;
}

.sjcm__header-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
  width: 48px;
  height: 48px;
  color: var(--ds-primary);
  background: var(--ds-primary-soft);
  border-radius: var(--ds-radius-xl);
  box-shadow: 0 4px 12px rgba(79, 70, 229, 0.2);
}

.sjcm__header-text {
  min-width: 0;
}

.sjcm__title {
  margin: 0;
  color: var(--ds-text);
  font-family: var(--ds-font-display);
  font-size: 1.125rem;
  font-weight: 800;
  line-height: 1.3;
}

.sjcm__subtitle {
  margin: 0.25rem 0 0;
  color: var(--ds-text-muted);
  font-size: 0.875rem;
  line-height: 1.4;
}

.sjcm__body {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.sjcm__alert {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.875rem 1rem;
  border-radius: var(--ds-radius-xl);
  font-size: 0.875rem;
  font-weight: 600;
  animation: sjcmSlideDown 0.2s ease;
}

.sjcm__alert--error {
  color: var(--ds-danger);
  background: var(--ds-danger-soft);
}

.sjcm__field {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.sjcm__label {
  color: var(--ds-text);
  font-size: 0.875rem;
  font-weight: 700;
}

.sjcm__input-wrap {
  position: relative;
}

.sjcm__input-icon {
  position: absolute;
  top: 50%;
  left: 1rem;
  color: var(--ds-text-muted);
  pointer-events: none;
  transform: translateY(-50%);
}

.sjcm__input {
  width: 100%;
  padding: 0.875rem 1rem 0.875rem 3rem;
  color: var(--ds-text);
  background: var(--ds-surface);
  border: 2px solid var(--ds-border);
  border-radius: var(--ds-radius-xl);
  font-size: 1rem;
  font-weight: 600;
  letter-spacing: 0.1em;
  text-align: center;
  text-transform: uppercase;
  outline: none;
  transition: border-color 0.18s ease, box-shadow 0.18s ease, background-color 0.18s ease;
}

.dark .sjcm__input {
  color: var(--ds-text);
  background: var(--ds-gray-800);
  border-color: var(--ds-border-strong);
}

.sjcm__input::placeholder {
  color: var(--ds-text-muted);
  font-weight: 400;
  letter-spacing: 0;
  text-transform: none;
}

.sjcm__input:focus {
  border-color: var(--ds-primary);
  box-shadow: 0 0 0 4px var(--ds-primary-ring);
}

.sjcm__input:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

.sjcm__footer {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
  width: 100%;
}

.sjcm__btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  min-width: 100px;
  min-height: 44px;
  padding: 0.75rem 1.25rem;
  border: 1.5px solid transparent;
  border-radius: var(--ds-radius-xl);
  font-family: inherit;
  font-size: 0.875rem;
  font-weight: 700;
  cursor: pointer;
  transition: background-color 0.18s ease, border-color 0.18s ease, color 0.18s ease, box-shadow 0.18s ease, transform 0.18s ease;
}

.sjcm__btn--primary {
  color: white;
  background: linear-gradient(135deg, var(--ds-primary) 0%, #6366f1 100%);
  box-shadow: 0 4px 16px rgba(79, 70, 229, 0.25);
}

.sjcm__btn--primary:hover:not(:disabled) {
  box-shadow: 0 8px 24px rgba(79, 70, 229, 0.35);
  transform: translateY(-1px);
}

.sjcm__btn--outline {
  color: var(--ds-text-secondary);
  background: var(--ds-surface);
  border-color: var(--ds-border);
}

.dark .sjcm__btn--outline {
  color: var(--ds-text-muted);
  background: var(--ds-gray-800);
  border-color: var(--ds-border-strong);
}

.sjcm__btn--outline:hover:not(:disabled) {
  color: var(--ds-primary);
  background: var(--ds-primary-soft);
  border-color: var(--ds-primary);
}

.sjcm__btn:disabled {
  cursor: not-allowed;
  opacity: 0.55;
  box-shadow: none;
  transform: none;
}

.sjcm__spinner {
  width: 18px;
  height: 18px;
  border: 2.5px solid rgba(255, 255, 255, 0.3);
  border-top-color: white;
  border-radius: 50%;
  animation: sjcmSpin 0.8s linear infinite;
}

@keyframes sjcmSlideDown {
  from {
    opacity: 0;
    transform: translateY(-8px);
  }

  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes sjcmSpin {
  to {
    transform: rotate(360deg);
  }
}

@media (max-width: 480px) {
  .sjcm__footer {
    flex-direction: column-reverse;
  }

  .sjcm__btn {
    width: 100%;
  }
}

@media (prefers-reduced-motion: reduce) {
  * {
    transition-duration: 0.01ms !important;
    animation-duration: 0.01ms !important;
  }
}
</style>
