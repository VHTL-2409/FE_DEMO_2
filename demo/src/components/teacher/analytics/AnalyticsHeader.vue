<template>
  <div class="ah">
    <!-- Left: back + title -->
    <div class="ah__left">
      <button type="button" class="ah__back-btn" @click="$emit('back')">
        <LucideIcon name="arrow_back" />
      </button>

      <div class="ah__title-group">
        <div class="ah__title-icon">
          <LucideIcon name="analytics" />
        </div>
        <div>
          <h1 class="ah__title">{{ title }}</h1>
          <p class="ah__subtitle">{{ subtitle }}</p>
        </div>
      </div>
    </div>

    <!-- Center: selectors -->
    <div class="ah__selectors">
      <!-- Exam selector -->
      <select
        v-if="showExamSelector"
        :value="selectedExam"
        class="ah__select"
        @change="$emit('update:selectedExam', $event.target.value)"
      >
        <option value="">{{ examPlaceholder }}</option>
        <option v-for="exam in exams" :key="exam.id" :value="exam.id">
          {{ exam.title }}
        </option>
      </select>

      <!-- Date range -->
      <select
        v-if="showDateRange"
        :value="dateRange"
        class="ah__select"
        @change="$emit('update:dateRange', $event.target.value)"
      >
        <option v-for="opt in dateOptions" :key="opt.value" :value="opt.value">
          {{ opt.label }}
        </option>
      </select>
    </div>

    <!-- Right: actions -->
    <div class="ah__right">
      <!-- CSV Export -->
      <button
        type="button"
        class="ah__export-btn ah__export-btn--primary"
        :disabled="exportLoading"
        @click="$emit('export-csv')"
      >
        <LucideIcon name="download" />
        Xuất CSV
      </button>

      <!-- PDF Export -->
      <button
        v-if="showPdfExport"
        type="button"
        class="ah__export-btn"
        :disabled="exportLoading"
        @click="$emit('export-pdf')"
      >
        <LucideIcon name="picture_as_pdf" />
      </button>
    </div>
  </div>
</template>

<script setup>
defineProps({
  title: { type: String, default: 'Phân tích kết quả' },
  subtitle: { type: String, default: '' },
  showExamSelector: { type: Boolean, default: false },
  exams: { type: Array, default: () => [] },
  selectedExam: { type: [String, Number], default: '' },
  examPlaceholder: { type: String, default: 'Chọn kỳ thi...' },
  showDateRange: { type: Boolean, default: false },
  dateRange: { type: String, default: 'week' },
  dateOptions: {
    type: Array,
    default: () => [
      { value: 'week', label: 'Tuần này' },
      { value: 'month', label: 'Tháng này' },
      { value: 'semester', label: 'Học kỳ này' },
      { value: 'all', label: 'Tất cả' }
    ]
  },
  showPdfExport: { type: Boolean, default: false },
  exportLoading: { type: Boolean, default: false }
})

defineEmits([
  'back',
  'update:selectedExam',
  'update:dateRange',
  'export-csv',
  'export-pdf'
])
</script>


<style scoped>
.ah {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 0.875rem 1.5rem;
  background: var(--ds-surface);
  border-bottom: 1px solid var(--ds-border);
  flex-wrap: wrap;
}

.dark .ah {
  background: rgba(30, 41, 59, 0.98);
  border-bottom-color: var(--ds-border-strong);
}

/* Left */
.ah__left {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex-shrink: 0;
}

.ah__back-btn {
  width: 2.25rem;
  height: 2.25rem;
  border-radius: var(--ds-radius-md);
  border: 1px solid var(--ds-border);
  background: transparent;
  color: var(--ds-text-muted);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: color 0.15s ease, background-color 0.15s ease, border-color 0.15s ease, box-shadow 0.15s ease, transform 0.15s ease;
}

.dark .ah__back-btn {
  background: var(--ds-gray-800);
  border-color: var(--ds-border-strong);
}

.ah__back-btn:hover {
  background: var(--ds-gray-100);
  color: var(--ds-text);
}

.dark .ah__back-btn:hover { background: var(--ds-gray-700); }


.ah__title-group {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.ah__title-icon {
  width: 40px;
  height: 40px;
  border-radius: var(--ds-radius-lg);
  background: var(--ds-primary-soft);
  color: var(--ds-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}


.ah__title {
  font-family: var(--ds-font-display);
  font-size: 1.125rem;
  font-weight: 800;
  color: var(--ds-text);
  margin: 0;
  line-height: 1.2;
}

.dark .ah__title { color: #f1f5f9; }

.ah__subtitle {
  font-size: 0.7rem;
  color: var(--ds-text-muted);
  margin: 0.25rem 0 0;
  font-weight: 500;
}

/* Selectors */
.ah__selectors {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex: 1;
  justify-content: center;
}

.ah__select {
  padding: 0.5rem 2.5rem 0.5rem 0.875rem;
  background: var(--ds-surface);
  border: 1.5px solid var(--ds-border);
  border-radius: var(--ds-radius-xl);
  font-size: 0.8rem;
  font-weight: 600;
  color: var(--ds-text-secondary);
  outline: none;
  cursor: pointer;
  appearance: none;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 12 12'%3E%3Cpath fill='%2364748b' d='M2 4l4 4 4-4'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 0.75rem center;
  background-color: var(--ds-surface);
  transition: color 0.15s ease, background-color 0.15s ease, border-color 0.15s ease, box-shadow 0.15s ease, transform 0.15s ease;
  min-width: 160px;
}

.dark .ah__select {
  background-color: var(--ds-gray-800);
  border-color: var(--ds-border-strong);
  color: #94a3b8;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 12 12'%3E%3Cpath fill='%2394a3b8' d='M2 4l4 4 4-4'/%3E%3C/svg%3E");
}

.ah__select:focus {
  border-color: var(--ds-primary);
  box-shadow: 0 0 0 3px var(--ds-primary-ring);
}

/* Right */
.ah__right {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex-shrink: 0;
}

.ah__export-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 1rem;
  border-radius: var(--ds-radius-xl);
  font-size: 0.8rem;
  font-weight: 700;
  cursor: pointer;
  transition: color 0.15s ease, background-color 0.15s ease, border-color 0.15s ease, box-shadow 0.15s ease, transform 0.15s ease;
  border: 1.5px solid var(--ds-border);
  background: var(--ds-surface);
  color: var(--ds-text-secondary);
  font-family: inherit;
}

.dark .ah__export-btn { background: var(--ds-gray-800); border-color: var(--ds-border-strong); color: #94a3b8; }

.ah__export-btn:hover:not(:disabled) {
  border-color: var(--ds-gray-300);
  color: var(--ds-text);
}

.dark .ah__export-btn:hover:not(:disabled) { background: var(--ds-gray-700); }


.ah__export-btn--primary {
  background: var(--ds-primary);
  border-color: var(--ds-primary);
  color: white;
  box-shadow: 0 2px 8px rgba(79, 70, 229, 0.2);
}

.ah__export-btn--primary:hover:not(:disabled) {
  background: var(--ds-primary-hover, #4338ca);
  color: white;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(79, 70, 229, 0.3);
}

.ah__export-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  transform: none;
}
</style>
