<template>
  <Transition name="bab-slide">
    <div v-if="selected.length > 0" class="bab">
      <div class="bab__inner">
        <!-- Selection count -->
        <div class="bab__count">
          <span class="bab__count-check">
            <LucideIcon name="check_circle" />
          </span>
          <span>
            <strong>{{ selected.length }}</strong> đề thi đã chọn
          </span>
        </div>

        <!-- Bulk actions -->
        <div class="bab__actions">
          <button
            type="button"
            class="bab__btn bab__btn--success"
            :disabled="loading"
            @click="$emit('publish')"
          >
            <LucideIcon name="publish" />
            Xuất bản
          </button>

          <button
            type="button"
            class="bab__btn bab__btn--archive"
            :disabled="loading"
            @click="$emit('archive')"
          >
            <LucideIcon name="archive" />
            Lưu trữ
          </button>

          <button
            type="button"
            class="bab__btn bab__btn--danger"
            :disabled="loading"
            @click="$emit('delete')"
          >
            <LucideIcon name="delete" />
            Xóa
          </button>
        </div>

        <!-- Clear -->
        <button
          type="button"
          class="bab__clear"
          :disabled="loading"
          @click="$emit('clear')"
        >
          <LucideIcon name="close" />
          Bỏ chọn
        </button>
      </div>
    </div>
  </Transition>
</template>

<script setup>
defineProps({
  selected: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false }
})

defineEmits(['publish', 'archive', 'delete', 'clear'])
</script>


<style scoped>
.bab {
  position: fixed;
  bottom: 1.5rem;
  left: 50%;
  transform: translateX(-50%);
  z-index: 200;
  width: calc(100% - 3rem);
  max-width: 900px;
}

.bab__inner {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 0.875rem 1.25rem;
  background: white;
  border: 1px solid var(--ds-border);
  border-radius: var(--ds-radius-2xl);
  box-shadow: 0 8px 32px rgba(0,0,0,0.12), 0 2px 8px rgba(0,0,0,0.06);
  backdrop-filter: blur(12px);
}

.dark .bab__inner {
  background: var(--ds-gray-800);
  border-color: var(--ds-border-strong);
}

.bab__count {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--ds-text);
  white-space: nowrap;
  flex-shrink: 0;
}

.dark .bab__count { color: #f1f5f9; }

.bab__count strong { color: var(--ds-primary); }

.bab__count-check {
  color: var(--ds-primary);
  display: flex;
}


.bab__actions {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex: 1;
}

.bab__btn {
  display: inline-flex;
  align-items: center;
  gap: 0.375rem;
  padding: 0.5rem 0.875rem;
  border-radius: var(--ds-radius-xl);
  border: 1.5px solid;
  font-size: 0.8rem;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.15s ease;
  white-space: nowrap;
}


.bab__btn:disabled { opacity: 0.5; cursor: not-allowed; transform: none !important; }

.bab__btn--success {
  background: var(--ds-success-soft);
  border-color: rgba(22, 163, 74, 0.3);
  color: var(--ds-success);
}

.bab__btn--success:hover:not(:disabled) {
  background: var(--ds-success);
  color: white;
  border-color: var(--ds-success);
  transform: translateY(-1px);
}

.bab__btn--archive {
  background: var(--ds-gray-100);
  border-color: var(--ds-border);
  color: var(--ds-text-secondary);
}

.dark .bab__btn--archive {
  background: var(--ds-gray-700);
  border-color: var(--ds-border-strong);
  color: #94a3b8;
}

.bab__btn--archive:hover:not(:disabled) {
  background: var(--ds-gray-200);
  transform: translateY(-1px);
}

.dark .bab__btn--archive:hover:not(:disabled) {
  background: var(--ds-gray-600);
}

.bab__btn--danger {
  background: var(--ds-danger-soft);
  border-color: rgba(220, 38, 38, 0.25);
  color: var(--ds-danger);
}

.bab__btn--danger:hover:not(:disabled) {
  background: var(--ds-danger);
  color: white;
  border-color: var(--ds-danger);
  transform: translateY(-1px);
}

.bab__clear {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  padding: 0.5rem 0.75rem;
  border: none;
  background: transparent;
  font-size: 0.8rem;
  font-weight: 600;
  color: var(--ds-text-muted);
  cursor: pointer;
  border-radius: var(--ds-radius-lg);
  transition: all 0.12s ease;
  white-space: nowrap;
  flex-shrink: 0;
}

.bab__clear:hover {
  background: var(--ds-gray-100);
  color: var(--ds-text);
}

.dark .bab__clear:hover {
  background: var(--ds-gray-700);
  color: #f1f5f9;
}


/* Transition */
.bab-slide-enter-active,
.bab-slide-leave-active {
  transition: all 0.25s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.bab-slide-enter-from,
.bab-slide-leave-to {
  opacity: 0;
  transform: translateX(-50%) translateY(20px);
}
</style>
