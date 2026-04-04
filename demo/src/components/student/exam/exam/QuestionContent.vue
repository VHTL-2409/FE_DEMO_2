<template>
  <div class="qcontent">
    <!-- Question header bar -->
    <div class="qcontent__header">
      <div class="qcontent__question-badge">
        <LucideIcon name="quiz" />
        Cau {{ currentIndex + 1 }} / {{ totalQuestions }}
      </div>
      <button
        type="button"
        class="qcontent__mark-btn"
        :disabled="isSuspended"
        @click="$emit('toggle-mark')"
      >
        <LucideIcon :name="isMarked ? 'bookmark_added' : 'bookmark_add'" />
        {{ isMarked ? 'Bo danh dau' : 'Danh dau xem lai' }}
      </button>
    </div>

    <!-- Question content -->
    <div class="qcontent__body">
      <div class="qcontent__question-text">
        {{ question.content }}
      </div>

      <!-- Answer renderer -->
      <div class="qcontent__answer">
        <QuestionRenderer
          :question="question"
          :model-value="modelValue"
          :disabled="isSuspended"
          @update:model-value="$emit('update:modelValue', $event)"
        />
      </div>
    </div>

    <!-- Navigation footer -->
    <div class="qcontent__footer">
      <button
        type="button"
        class="qcontent__nav-btn qcontent__nav-btn--prev"
        :disabled="currentIndex === 0 || isSuspended"
        @click="$emit('prev')"
      >
        <LucideIcon name="arrow_back" />
        Cau truoc
      </button>

      <div class="qcontent__nav-center">
        <button
          type="button"
          class="qcontent__mark-nav-btn"
          :disabled="isSuspended"
          @click="$emit('toggle-mark')"
        >
          <LucideIcon :name="isMarked ? 'bookmark_added' : 'bookmark_add'" />
          {{ isMarked ? 'Da danh dau' : 'Danh dau' }}
        </button>
      </div>

      <button
        type="button"
        class="qcontent__nav-btn qcontent__nav-btn--next"
        :disabled="currentIndex >= totalQuestions - 1 || isSuspended"
        @click="$emit('next')"
      >
        Cau tiep theo
        <LucideIcon name="arrow_forward" />
      </button>
    </div>
  </div>
</template>

<script setup>
import QuestionRenderer from '../questions/QuestionRenderer.vue'

const props = defineProps({
  question: { type: Object, required: true },
  currentIndex: { type: Number, default: 0 },
  totalQuestions: { type: Number, default: 0 },
  modelValue: { default: null },
  isMarked: { type: Boolean, default: false },
  isSuspended: { type: Boolean, default: false }
})

defineEmits(['toggle-mark', 'prev', 'next', 'update:modelValue'])
</script>


<style scoped>
.qcontent {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: var(--ds-surface);
  border: 1.5px solid var(--ds-border);
  border-radius: var(--ds-radius-2xl);
  overflow: hidden;
}

.dark .qcontent { border-color: var(--ds-border-strong); }

/* Header */
.qcontent__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  padding: 0.875rem 1.125rem;
  border-bottom: 1px solid var(--ds-border);
  background: var(--ds-gray-50);
  flex-shrink: 0;
}

.dark .qcontent__header {
  background: var(--ds-gray-800);
  border-bottom-color: var(--ds-border-strong);
}

.qcontent__question-badge {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 0.875rem;
  border-radius: var(--ds-radius-xl);
  background: var(--ds-primary-soft);
  color: var(--ds-primary);
  font-size: 0.8rem;
  font-weight: 800;
}


.qcontent__mark-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.375rem;
  padding: 0.5rem 0.875rem;
  border-radius: var(--ds-radius-xl);
  background: transparent;
  border: 1.5px solid var(--ds-border);
  color: var(--ds-text-muted);
  font-size: 0.75rem;
  font-weight: 700;
  cursor: pointer;
  transition: color 0.12s ease, background-color 0.12s ease, border-color 0.12s ease, box-shadow 0.12s ease, transform 0.12s ease;
  font-family: inherit;
}

.dark .qcontent__mark-btn { border-color: var(--ds-border-strong); color: var(--ds-gray-400); }

.qcontent__mark-btn:hover:not(:disabled) {
  border-color: rgba(234, 179, 8, 0.4);
  color: var(--ds-warning);
  background: rgba(234, 179, 8, 0.08);
}

.qcontent__mark-btn:disabled { opacity: 0.5; cursor: not-allowed; }

/* Body */
.qcontent__body {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 1.5rem 1.5rem;
}

.qcontent__question-text {
  font-family: var(--ds-font-display);
  font-size: 1.25rem;
  font-weight: 700;
  color: var(--ds-text);
  line-height: 1.5;
  margin-bottom: 1.5rem;
  letter-spacing: -0.01em;
}

.dark .qcontent__question-text { color: #f1f5f9; }

.qcontent__answer { margin-bottom: 1rem; }

/* Footer */
.qcontent__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  padding: 0.875rem 1.125rem;
  border-top: 1px solid var(--ds-border);
  background: var(--ds-gray-50);
  flex-shrink: 0;
}

.dark .qcontent__footer {
  background: var(--ds-gray-800);
  border-top-color: var(--ds-border-strong);
}

.qcontent__nav-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.625rem 1.125rem;
  border-radius: var(--ds-radius-xl);
  font-size: 0.875rem;
  font-weight: 700;
  cursor: pointer;
  transition: color 0.12s ease, background-color 0.12s ease, border-color 0.12s ease, box-shadow 0.12s ease, transform 0.12s ease;
  border: 1.5px solid;
  font-family: inherit;
}


.qcontent__nav-btn--prev {
  background: transparent;
  border-color: var(--ds-border);
  color: var(--ds-text);
}

.dark .qcontent__nav-btn--prev { border-color: var(--ds-border-strong); color: var(--ds-gray-300); }

.qcontent__nav-btn--prev:hover:not(:disabled) {
  border-color: var(--ds-primary-border);
  color: var(--ds-primary);
  background: var(--ds-primary-soft);
}

.qcontent__nav-btn--next {
  background: var(--ds-primary);
  border-color: var(--ds-primary);
  color: white;
  box-shadow: 0 2px 8px rgba(79, 70, 229, 0.2);
}

.qcontent__nav-btn--next:hover:not(:disabled) {
  background: var(--ds-primary-hover, #4338ca);
  box-shadow: 0 4px 12px rgba(79, 70, 229, 0.3);
  transform: translateY(-1px);
}

.qcontent__nav-btn:disabled { opacity: 0.4; cursor: not-allowed; transform: none !important; }

.qcontent__nav-center {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.qcontent__mark-nav-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.375rem;
  padding: 0.5rem 0.875rem;
  border-radius: var(--ds-radius-xl);
  background: transparent;
  border: 1.5px solid var(--ds-border);
  color: var(--ds-text-muted);
  font-size: 0.75rem;
  font-weight: 700;
  cursor: pointer;
  transition: color 0.12s ease, background-color 0.12s ease, border-color 0.12s ease, box-shadow 0.12s ease, transform 0.12s ease;
  font-family: inherit;
}

.dark .qcontent__mark-nav-btn { border-color: var(--ds-border-strong); color: var(--ds-gray-400); }

.qcontent__mark-nav-btn:hover:not(:disabled) {
  border-color: rgba(234, 179, 8, 0.4);
  color: var(--ds-warning);
  background: rgba(234, 179, 8, 0.08);
}

.qcontent__mark-nav-btn:disabled { opacity: 0.5; cursor: not-allowed; }

/* Responsive */
@media (max-width: 480px) {
  .qcontent__mark-nav-btn { display: none; }
  .qcontent__nav-btn { padding: 0.5rem 0.75rem; font-size: 0.8rem; }
  .qcontent__body { padding: 1rem; }
  .qcontent__question-text { font-size: 1.1rem; }
}
</style>
@media (prefers-reduced-motion: reduce) {
  * {
    transition-duration: 0.01ms !important;
    animation-duration: 0.01ms !important;
  }
}