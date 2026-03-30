<template>
  <Teleport to="body">
    <Transition name="edm-fade">
      <div v-if="modelValue" class="edm-overlay" @click.self="$emit('update:modelValue', false)">
        <div class="edm" role="dialog" :aria-labelledby="titleId">
          <div class="edm__header">
            <div class="edm__header-icon edm__header-icon--danger">
              <LucideIcon name="delete" />
            </div>
            <div>
              <h2 :id="titleId" class="edm__title">Xóa đề thi</h2>
              <p class="edm__subtitle">{{ isBulk ? 'Xóa nhiều đề thi' : 'Xóa đề thi vĩnh viễn' }}</p>
            </div>
            <button type="button" class="edm__close" @click="$emit('update:modelValue', false)" aria-label="Đóng">
              <LucideIcon name="close" />
            </button>
          </div>

          <div class="edm__body">
            <p v-if="isBulk" class="edm__msg">
              Bạn có chắc muốn xóa <strong>{{ count }} đề thi</strong> đã chọn? Hành động này không thể hoàn tác.
            </p>
            <p v-else class="edm__msg">
              Bạn có chắc muốn xóa đề thi "<strong>{{ examTitle }}</strong>"? Tất cả câu hỏi, lượt thi và dữ liệu liên quan sẽ bị xóa vĩnh viễn.
            </p>
          </div>

          <div class="edm__footer">
            <button type="button" class="edm__btn edm__btn--outline" @click="$emit('update:modelValue', false)">
              Huy bo
            </button>
            <button type="button" class="edm__btn edm__btn--danger" :disabled="loading" @click="$emit('confirm')">
              <span v-if="loading" class="edm-spin"><LucideIcon name="loader-2" class="animate-spin" /></span>
              {{ loading ? 'Đang xóa...' : 'Xóa đề thi' }}
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  exam: { type: Object, default: null },
  count: { type: Number, default: 0 },
  loading: { type: Boolean, default: false }
})

defineEmits(['update:modelValue', 'confirm'])

const isBulk = computed(() => !props.exam)
const examTitle = computed(() => props.exam?.title || '')
const titleId = 'edm-title'
</script>


<style scoped>
.edm-overlay {
  position: fixed; inset: 0;
  background: rgba(0, 0, 0, 0.45);
  backdrop-filter: blur(4px);
  display: flex; align-items: center; justify-content: center;
  z-index: 1000; padding: 1.5rem;
}

.edm {
  background: var(--ds-surface);
  border-radius: var(--ds-radius-2xl);
  width: 100%; max-width: 440px;
  box-shadow: 0 24px 64px rgba(0, 0, 0, 0.18);
  overflow: hidden;
}

.dark .edm { background: var(--ds-gray-800); }

/* Header */
.edm__header {
  display: flex; align-items: flex-start; gap: 0.875rem;
  padding: 1.375rem 1.5rem;
  border-bottom: 1px solid var(--ds-border);
}
.dark .edm__header { border-bottom-color: var(--ds-border-strong); }

.edm__header-icon {
  width: 44px; height: 44px;
  border-radius: var(--ds-radius-xl);
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0;
}
.edm__header-icon--danger { background: var(--ds-danger-soft); color: var(--ds-danger); }

.edm__title {
  font-family: var(--ds-font-display);
  font-size: 1rem; font-weight: 800;
  color: var(--ds-text); margin: 0; line-height: 1.3;
}
.dark .edm__title { color: var(--ds-gray-100); }

.edm__subtitle {
  font-size: 0.8rem; color: var(--ds-text-muted);
  margin: 0.25rem 0 0;
}

.edm__close {
  margin-left: auto;
  width: 32px; height: 32px;
  border: none; background: var(--ds-gray-100);
  color: var(--ds-text-muted);
  border-radius: var(--ds-radius-md);
  display: flex; align-items: center; justify-content: center;
  cursor: pointer; transition: all 0.12s ease; flex-shrink: 0;
}
.dark .edm__close { background: var(--ds-gray-700); }
.edm__close:hover { background: var(--ds-gray-200); color: var(--ds-text); }
.dark .edm__close:hover { background: var(--ds-gray-600); }

/* Body */
.edm__body { padding: 1.25rem 1.5rem; }

.edm__msg {
  font-size: 0.875rem; color: var(--ds-text-secondary);
  line-height: 1.6; margin: 0;
}
.dark .edm__msg { color: var(--ds-gray-300); }
.edm__msg strong { color: var(--ds-text); }
.dark .edm__msg strong { color: var(--ds-gray-100); }

/* Footer */
.edm__footer {
  display: flex; align-items: center; justify-content: flex-end;
  gap: 0.75rem;
  padding: 1rem 1.5rem;
  border-top: 1px solid var(--ds-border);
  background: var(--ds-gray-50);
}
.dark .edm__footer { border-top-color: var(--ds-border-strong); background: var(--ds-gray-800); }

.edm__btn {
  display: inline-flex; align-items: center; gap: 0.5rem;
  padding: 0.625rem 1.125rem;
  border-radius: var(--ds-radius-xl);
  font-size: 0.875rem; font-weight: 700;
  cursor: pointer; transition: all 0.12s ease;
  border: 1.5px solid transparent; white-space: nowrap;
}
.edm__btn:disabled { opacity: 0.5; cursor: not-allowed; }

.edm__btn--outline {
  background: var(--ds-surface); border-color: var(--ds-border);
  color: var(--ds-text-secondary);
}
.dark .edm__btn--outline { background: var(--ds-gray-700); border-color: var(--ds-border-strong); color: var(--ds-gray-300); }
.edm__btn--outline:hover { border-color: var(--ds-primary); color: var(--ds-primary); background: var(--ds-primary-soft); }

.edm__btn--danger {
  background: var(--ds-danger); color: white; border-color: var(--ds-danger);
}
.edm__btn--danger:hover:not(:disabled) { background: #dc2626; }

.edm-spin { animation: edmSpin 1s linear infinite; }
@keyframes edmSpin { to { transform: rotate(360deg); } }

/* Transition */
.edm-fade-enter-active, .edm-fade-leave-active { transition: opacity 0.2s ease; }
.edm-fade-enter-from, .edm-fade-leave-to { opacity: 0; }
.edm-fade-enter-from .edm, .edm-fade-leave-to .edm { transform: scale(0.95); }
</style>
