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
.sq {
  background: var(--ds-surface);
  border: 1.5px solid var(--ds-border);
  border-radius: var(--ds-radius-2xl);
  overflow: hidden;
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

.dark .sq__title { color: #f1f5f9; }

/* Grid */
.sq__grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 0.625rem;
  padding: 1rem;
}

/* Action */
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
  transition: all 0.15s ease;
  text-align: center;
  font-family: inherit;
}

.dark .sq__action {
  background: var(--ds-gray-800);
  border-color: var(--ds-border-strong);
}

.sq__action:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.sq__action--primary:hover { border-color: var(--ds-primary-border); background: var(--ds-primary-soft); }
.sq__action--info:hover { border-color: rgba(14, 165, 233, 0.3); background: var(--ds-info-soft); }
.sq__action--success:hover { border-color: rgba(22, 163, 74, 0.3); background: var(--ds-success-soft); }
.sq__action--warning:hover { border-color: rgba(217, 119, 6, 0.3); background: rgba(234, 179, 8, 0.08); }
.sq__action--neutral:hover { border-color: var(--ds-gray-300); background: var(--ds-gray-100); }

.dark .sq__action--neutral:hover { background: var(--ds-gray-700); }

.sq__action:active { transform: translateY(0); }

.sq__action-icon {
  width: 44px;
  height: 44px;
  border-radius: var(--ds-radius-xl);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: all 0.15s ease;
}


.sq__action-icon--primary { background: var(--ds-primary-soft); color: var(--ds-primary); }
.sq__action-icon--info { background: var(--ds-info-soft); color: var(--ds-info); }
.sq__action-icon--success { background: var(--ds-success-soft); color: var(--ds-success); }
.sq__action-icon--warning { background: rgba(234, 179, 8, 0.1); color: var(--ds-warning); }
.sq__action-icon--neutral { background: var(--ds-gray-100); color: var(--ds-gray-500); }

.dark .sq__action-icon--neutral { background: var(--ds-gray-700); color: #94a3b8; }

.sq__action:hover .sq__action-icon--primary { background: var(--ds-primary); color: white; }
.sq__action:hover .sq__action-icon--info { background: var(--ds-info); color: white; }
.sq__action:hover .sq__action-icon--success { background: var(--ds-success); color: white; }
.sq__action:hover .sq__action-icon--warning { background: var(--ds-warning); color: white; }
.sq__action:hover .sq__action-icon--neutral { background: var(--ds-gray-200); color: var(--ds-text); }

.dark .sq__action:hover .sq__action-icon--neutral { background: var(--ds-gray-600); color: #f1f5f9; }

.sq__action-label {
  font-size: 0.8rem;
  font-weight: 800;
  color: var(--ds-text);
}

.dark .sq__action-label { color: #f1f5f9; }

.sq__action-desc {
  font-size: 0.65rem;
  color: var(--ds-text-muted);
  font-weight: 500;
  line-height: 1.3;
}

@media (max-width: 640px) {
  .sq__grid { grid-template-columns: repeat(2, 1fr); }
}
</style>
