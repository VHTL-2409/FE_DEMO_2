<template>
  <div class="sq">
    <!-- Header -->
    <div class="sq__header">
      <div class="sq__header-icon">
        <LucideIcon name="bolt" />
      </div>
      <div>
        <h3 class="sq__title">Thao tác nhanh</h3>
      </div>
    </div>

    <!-- Actions grid -->
    <div class="sq__grid">
      <button
        v-for="action in actions"
        :key="action.id"
        type="button"
        class="sq__action"
        :class="[`sq__action--${action.accent || 'primary'}`]"
        @click="$emit('action', action.id)"
      >
        <div class="sq__action-icon" :class="`sq__action-icon--${action.accent || 'primary'}`">
          <LucideIcon :name="action.icon" />
        </div>
        <span class="sq__action-label">{{ action.label }}</span>
        <span class="sq__action-desc">{{ action.description }}</span>
      </button>
    </div>
  </div>
</template>

<script setup>
defineEmits(['action'])

const actions = [
  { id: 'join-exam', label: 'Vào thi', description: 'Nhập mã đề thi', icon: 'login', accent: 'primary' },
  { id: 'practice', label: 'Luyện tập', description: 'Tạo bài luyện tự chọn', icon: 'model_training', accent: 'info' },
  { id: 'results', label: 'Kết quả', description: 'Xem điểm thi gần đây', icon: 'grade', accent: 'success' },
  { id: 'history', label: 'Lịch sử', description: 'Xem lại bài thi đã làm', icon: 'history', accent: 'neutral' },
  { id: 'schedule', label: 'Lịch thi', description: 'Xem lịch thi sắp tới', icon: 'calendar_month', accent: 'warning' },
  { id: 'profile', label: 'Hồ sơ', description: 'Cập nhật thông tin', icon: 'person', accent: 'neutral' }
]
</script>


<style scoped>
/* Fade-only entrance keeps labels crisp */
@keyframes fadeInUp {
  from { opacity: 0; }
  to   { opacity: 1; }
}

.sq {
  background: var(--ds-surface);
  border: 1.5px solid var(--ds-border);
  border-radius: var(--ds-radius-2xl);
  overflow: hidden;
  /* Optimize paint */
  contain: layout style;
  content-visibility: auto;
}

.dark .sq {
  border-color: var(--ds-border-strong);
}

/* Header */
.sq__header {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 1rem 1.125rem;
  border-bottom: 1px solid var(--ds-border);
  background: var(--ds-gray-50);
}

.dark .sq__header {
  border-bottom-color: var(--ds-border-strong);
  background: var(--ds-gray-800);
}

.sq__header-icon {
  width: 40px;
  height: 40px;
  border-radius: var(--ds-radius-lg);
  background: rgba(79, 70, 229, 0.08);
  color: var(--ds-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.sq__title {
  font-family: var(--ds-font-display);
  font-size: 0.875rem;
  font-weight: 800;
  color: var(--ds-text);
  margin: 0;
}

.dark .sq__title { color: var(--ds-text); }

/* Grid */
.sq__grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 0.75rem;
  padding: 1rem;
}

/* Action button */
.sq__action {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.5rem;
  padding: 1rem 0.75rem;
  border-radius: var(--ds-radius-xl);
  border: 1.5px solid var(--ds-border);
  background: var(--ds-gray-50);
  cursor: pointer;
  transition:
    transform 0.25s cubic-bezier(0.34, 1.56, 0.64, 1),
    box-shadow 0.25s ease,
    border-color 0.2s ease,
    background 0.2s ease;
  text-align: center;
  font-family: inherit;
  animation: fadeInUp 0.4s cubic-bezier(0.16, 1, 0.3, 1) backwards;
}

.sq__grid .sq__action:nth-child(1) { animation-delay: 0.32s; }
.sq__grid .sq__action:nth-child(2) { animation-delay: 0.35s; }
.sq__grid .sq__action:nth-child(3) { animation-delay: 0.38s; }
.sq__grid .sq__action:nth-child(4) { animation-delay: 0.41s; }
.sq__grid .sq__action:nth-child(5) { animation-delay: 0.44s; }
.sq__grid .sq__action:nth-child(6) { animation-delay: 0.47s; }

.sq__action:hover {
  transform: translateY(-3px);
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.1);
}

.dark .sq__action {
  background: var(--ds-gray-800);
  border-color: var(--ds-border-strong);
}

.sq__action:active {
  transform: translateY(-1px) scale(0.97);
}

.sq__action--primary:hover { border-color: var(--ds-primary-border); background: var(--ds-primary-soft); }
.sq__action--info:hover { border-color: rgba(14, 165, 233, 0.3); background: var(--ds-info-soft); }
.sq__action--success:hover { border-color: rgba(22, 163, 74, 0.3); background: var(--ds-success-soft); }
.sq__action--warning:hover { border-color: rgba(217, 119, 6, 0.3); background: rgba(234, 179, 8, 0.08); }
.sq__action--neutral:hover { border-color: var(--ds-gray-300); background: var(--ds-gray-100); }

.dark .sq__action--neutral:hover { background: var(--ds-gray-700); }

/* Icon */
.sq__action-icon {
  width: 48px;
  height: 48px;
  border-radius: var(--ds-radius-xl);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  will-change: transform;
  transform: translateZ(0);
  transition: transform 0.3s cubic-bezier(0.34, 1.56, 0.64, 1), background 0.25s ease, color 0.2s ease;
}

.sq__action:hover .sq__action-icon { transform: scale(1.15) rotate(-5deg) translateZ(0); }
.sq__action:hover .sq__action-icon--primary { background: var(--ds-primary); color: white; transform: scale(1.15) rotate(-6deg) translateZ(0); }
.sq__action:hover .sq__action-icon--info { background: var(--ds-info); color: white; transform: scale(1.15) rotate(-6deg) translateZ(0); }
.sq__action:hover .sq__action-icon--success { background: var(--ds-success); color: white; transform: scale(1.15) rotate(-6deg) translateZ(0); }
.sq__action:hover .sq__action-icon--warning { background: var(--ds-warning); color: white; transform: scale(1.15) rotate(-6deg) translateZ(0); }
.sq__action:hover .sq__action-icon--neutral { background: var(--ds-gray-200); color: var(--ds-text); transform: scale(1.15) rotate(-6deg) translateZ(0); }

.dark .sq__action-icon--neutral { background: var(--ds-gray-700); color: var(--ds-text-muted); }
.dark .sq__action:hover .sq__action-icon--neutral { background: var(--ds-gray-600); color: var(--ds-text); transform: scale(1.15) rotate(-6deg) translateZ(0); }

.sq__action-label {
  font-size: 0.8rem;
  font-weight: 800;
  color: var(--ds-text);
}

.dark .sq__action-label { color: var(--ds-text); }

.sq__action-desc {
  font-size: 0.65rem;
  color: var(--ds-text-muted);
  font-weight: 500;
  line-height: 1.3;
}

/* Reduced motion */
@media (prefers-reduced-motion: reduce) {
  .sq__action,
  .sq__action-icon,
  .sh__wave-icon {
    animation-duration: 0.01ms !important;
    transition-duration: 0.01ms !important;
  }
}

/* Responsive */
@media (max-width: 768px) {
  .sq__grid {
    gap: 0.5rem;
    padding: 0.875rem;
  }

  .sq__action {
    padding: 0.875rem 0.5rem;
  }

  .sq__action-icon {
    width: 44px;
    height: 44px;
  }

  .sq__action-label {
    font-size: 0.75rem;
  }
}

@media (max-width: 640px) {
  .sq__grid {
    grid-template-columns: repeat(3, 1fr);
    gap: 0.5rem;
  }

  .sq__action-desc {
    display: none;
  }

  .sq__action {
    padding: 0.75rem 0.5rem;
    gap: 0.375rem;
  }
}

@media (max-width: 480px) {
  .sq__grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .sq__action-desc {
    display: block;
    font-size: 0.6rem;
  }
}
</style>
