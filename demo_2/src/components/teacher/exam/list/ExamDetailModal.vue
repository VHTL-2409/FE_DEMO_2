<template>
  <Teleport to="body">
    <Transition name="edm-fade">
      <div v-if="modelValue && exam" class="edm-overlay" @click.self="$emit('update:modelValue', false)">
        <div class="edm edm--lg" role="dialog">
          <div class="edm__header">
            <div>
              <h2 class="edm__title">{{ exam.title }}</h2>
              <p class="edm__subtitle">
                Mã đề: <code>{{ exam.code || '—' }}</code>
                <span v-if="exam.className"> · {{ exam.className }}</span>
              </p>
            </div>
            <button type="button" class="edm__close" @click="$emit('update:modelValue', false)" aria-label="Đóng">
              <LucideIcon name="close" />
            </button>
          </div>

          <div class="edm__body">
            <div class="edm__grid">
              <div class="edm__info-item">
                <LucideIcon name="schedule" />
                <div>
                  <p class="edm__info-label">Thời gian</p>
                  <p class="edm__info-val">
                    {{ exam.startTime ? formatDateTime(exam.startTime) : '—' }}
                    <span v-if="exam.endTime"> → {{ formatDateTime(exam.endTime) }}</span>
                  </p>
                </div>
              </div>
              <div class="edm__info-item">
                <LucideIcon name="timer" />
                <div>
                  <p class="edm__info-label">Thời lượng</p>
                  <p class="edm__info-val">{{ exam.durationMinutes }} phút</p>
                </div>
              </div>
              <div class="edm__info-item">
                <LucideIcon name="quiz" />
                <div>
                  <p class="edm__info-label">Câu hỏi</p>
                  <p class="edm__info-val">{{ exam.questionCount || 0 }} câu</p>
                </div>
              </div>
              <div class="edm__info-item">
                <LucideIcon name="group" />
                <div>
                  <p class="edm__info-label">Học sinh</p>
                  <p class="edm__info-val">{{ exam.participantCount || 0 }} người</p>
                </div>
              </div>
              <div class="edm__info-item">
                <LucideIcon name="shield" />
                <div>
                  <p class="edm__info-label">Giám sát</p>
                  <p class="edm__info-val">{{ exam.proctoringEnabled === false ? 'Tắt' : 'Bật' }}</p>
                </div>
              </div>
              <div class="edm__info-item">
                <LucideIcon name="person" />
                <div>
                  <p class="edm__info-label">Người tạo</p>
                  <p class="edm__info-val">{{ exam.createdBy || '—' }}</p>
                </div>
              </div>
            </div>
            <div v-if="exam.description" class="edm__desc">
              <p class="edm__desc-label">Mo ta</p>
              <p class="edm__desc-val">{{ exam.description }}</p>
            </div>
          </div>

          <div class="edm__footer">
            <button type="button" class="edm__btn edm__btn--outline" @click="$emit('update:modelValue', false)">
              Dong
            </button>
            <button type="button" class="edm__btn edm__btn--secondary" @click="$emit('view-analytics', exam)">
              <LucideIcon name="analytics" />
              Phan tich ket qua
            </button>
            <button type="button" class="edm__btn edm__btn--outline" @click="$emit('edit', exam)">
              <LucideIcon name="edit" />
              Chinh sua
            </button>
            <button type="button" class="edm__btn edm__btn--primary" @click="$emit('duplicate', exam)">
              <LucideIcon name="content_copy" />
              Nhan ban
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
defineProps({
  modelValue: { type: Boolean, default: false },
  exam: { type: Object, default: null }
})

defineEmits(['update:modelValue', 'edit', 'duplicate', 'view-analytics'])

const formatDateTime = (d) => {
  try {
    return new Date(d).toLocaleString('vi-VN', {
      day: '2-digit', month: '2-digit', year: 'numeric',
      hour: '2-digit', minute: '2-digit'
    })
  } catch { return '—' }
}
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
  width: 100%; max-width: 480px;
  box-shadow: 0 24px 64px rgba(0, 0, 0, 0.18);
  overflow: hidden;
}
.edm--lg { max-width: 580px; }
.dark .edm { background: var(--ds-gray-800); }

.edm__header {
  display: flex; align-items: flex-start; gap: 0.875rem;
  padding: 1.375rem 1.5rem;
  border-bottom: 1px solid var(--ds-border);
}
.dark .edm__header { border-bottom-color: var(--ds-border-strong); }

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
.edm__subtitle code {
  font-family: monospace;
  background: var(--ds-gray-100); padding: 0.125rem 0.375rem;
  border-radius: 4px;
}
.dark .edm__subtitle code { background: var(--ds-gray-700); }

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

.edm__body { padding: 1.25rem 1.5rem; }

.edm__grid {
  display: grid; grid-template-columns: 1fr 1fr;
  gap: 0.75rem; margin-bottom: 1rem;
}
@media (max-width: 480px) { .edm__grid { grid-template-columns: 1fr; } }

.edm__info-item {
  display: flex; align-items: flex-start; gap: 0.75rem;
  padding: 0.75rem 1rem;
  background: var(--ds-gray-50);
  border: 1px solid var(--ds-border);
  border-radius: var(--ds-radius-xl);
}
.dark .edm__info-item { background: var(--ds-gray-700); border-color: var(--ds-border-strong); }

.edm__info-label { font-size: 0.6rem; font-weight: 700; color: var(--ds-text-muted); text-transform: uppercase; letter-spacing: 0.06em; margin: 0; }
.edm__info-val { font-size: 0.875rem; font-weight: 700; color: var(--ds-text); margin: 0.2rem 0 0; }
.dark .edm__info-val { color: var(--ds-gray-100); }

.edm__desc {
  padding: 1rem;
  background: var(--ds-gray-50);
  border: 1px solid var(--ds-border);
  border-radius: var(--ds-radius-xl);
}
.dark .edm__desc { background: var(--ds-gray-700); border-color: var(--ds-border-strong); }
.edm__desc-label { font-size: 0.6rem; font-weight: 700; color: var(--ds-text-muted); text-transform: uppercase; letter-spacing: 0.06em; margin: 0 0 0.375rem; }
.edm__desc-val { font-size: 0.875rem; color: var(--ds-text-secondary); margin: 0; line-height: 1.5; }
.dark .edm__desc-val { color: var(--ds-gray-300); }

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

.edm__btn--outline {
  background: var(--ds-surface); border-color: var(--ds-border);
  color: var(--ds-text-secondary);
}
.dark .edm__btn--outline { background: var(--ds-gray-700); border-color: var(--ds-border-strong); color: var(--ds-gray-300); }
.edm__btn--outline:hover { border-color: var(--ds-primary); color: var(--ds-primary); background: var(--ds-primary-soft); }

.edm__btn--secondary {
  background: var(--ds-surface); border-color: var(--ds-border);
  color: var(--ds-text-secondary);
}
.dark .edm__btn--secondary { background: var(--ds-gray-700); border-color: var(--ds-border-strong); color: var(--ds-gray-300); }
.edm__btn--secondary:hover { border-color: var(--ds-primary); color: var(--ds-primary); background: var(--ds-primary-soft); }

.edm__btn--primary {
  background: linear-gradient(135deg, var(--ds-primary) 0%, #6366f1 100%);
  color: white; box-shadow: 0 2px 8px rgba(79, 70, 229, 0.2);
}
.edm__btn--primary:hover { transform: translateY(-1px); box-shadow: 0 4px 16px rgba(79, 70, 229, 0.3); }

.edm-fade-enter-active, .edm-fade-leave-active { transition: opacity 0.2s ease; }
.edm-fade-enter-from, .edm-fade-leave-to { opacity: 0; }
.edm-fade-enter-from .edm, .edm-fade-leave-to .edm { transform: scale(0.95); }
</style>
