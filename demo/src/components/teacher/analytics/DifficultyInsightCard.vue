<template>
  <div class="dic">
    <!-- Header -->
    <div class="dic__header">
      <div class="dic__header-left">
        <div class="dic__icon">
          <LucideIcon name="psychology" />
        </div>
        <div>
          <h3 class="dic__title">Top câu hỏi khó</h3>
          <p class="dic__subtitle">Tỷ lệ sai cao nhất</p>
        </div>
      </div>
      <span class="dic__badge">{{ displayedQuestions.length }}</span>
    </div>

    <!-- Loading -->
    <div v-if="loading" class="dic__loading">
      <div v-for="i in 5" :key="i" class="dic__skeleton-item">
        <div class="dic__skeleton dic__skeleton--num" />
        <div class="dic__skeleton-content">
          <div class="dic__skeleton dic__skeleton--title" />
          <div class="dic__skeleton dic__skeleton--bar" />
        </div>
        <div class="dic__skeleton dic__skeleton--badge" />
      </div>
    </div>

    <!-- Empty -->
    <div v-else-if="!sortedQuestions.length" class="dic__empty">
      <LucideIcon name="check_circle" />
      <p>Tất cả câu hỏi đều có tỷ lệ đúng tốt</p>
    </div>

    <!-- Question list -->
    <div v-else class="dic__list">
      <div
        v-for="(q, index) in displayedQuestions"
        :key="q.id || index"
        class="dic__item"
        :class="{ 'dic__item--expanded': expandedId === q.id }"
        @click="$emit('question-click', q)"
      >
        <!-- Number + content -->
        <div class="dic__item-main">
          <span class="dic__item-num">#{{ index + 1 }}</span>
          <div class="dic__item-content">
            <p class="dic__item-text">{{ q.text || q.questionText || q.title || 'Cau ' + (q.number || q.order || index + 1) }}</p>
            <div class="dic__item-meta">
              <span v-if="q.type || q.questionType" class="dic__meta-tag">
                <LucideIcon name="quiz" />
                {{ q.type || q.questionType }}
              </span>
              <span v-if="q.chapter || q.section" class="dic__meta-tag">
                <LucideIcon name="book" />
                {{ q.chapter || q.section }}
              </span>
            </div>
          </div>
          <span
            class="dic__difficulty-badge"
            :class="difficultyClass(q.wrongRate || q.errorRate)"
            :title="`Tỷ lệ sai: ${wrongRateDisplay(q)}`"
          >
            {{ wrongRateDisplay(q) }}
          </span>
        </div>

        <!-- Progress bar -->
        <div class="dic__item-bar">
          <div class="dic__bar-track">
            <div
              class="dic__bar-fill"
              :class="difficultyClass(q.wrongRate || q.errorRate)"
              :style="{ width: barWidth(q.wrongRate || q.errorRate) + '%' }"
            />
          </div>
          <span class="dic__bar-label">{{ (q.wrongRate || q.errorRate || 0).toFixed(0) }}% sai</span>
        </div>
      </div>
    </div>

    <!-- Show more / less -->
    <div v-if="!loading && sortedQuestions.length > displayLimit" class="dic__footer">
      <button type="button" class="dic__show-more" @click="toggleExpand">
        <LucideIcon :name="expanded ? 'expand_less' : 'expand_more'" />
        {{ expanded ? 'Thu gọn' : `Xem ${sortedQuestions.length - displayLimit} câu khác` }}
      </button>
    </div>

    <!-- View all link -->
    <div v-if="!loading && sortedQuestions.length > 0" class="dic__all-link">
      <button type="button" class="dic__view-all" @click="$emit('view-all')">
        <LucideIcon name="list" />
        Xem tất cả câu hỏi
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'

const props = defineProps({
  questions: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false },
  displayLimit: { type: Number, default: 5 }
})

defineEmits(['question-click', 'view-all'])

const expanded = ref(false)
const expandedId = ref(null)

const sortedQuestions = computed(() => {
  return [...props.questions].sort((a, b) =>
    (b.wrongRatePercent ?? b.wrongRate ?? b.errorRate ?? 0)
    - (a.wrongRatePercent ?? a.wrongRate ?? a.errorRate ?? 0)
  )
})

const displayedQuestions = computed(() => {
  if (expanded.value) return sortedQuestions.value
  return sortedQuestions.value.slice(0, props.displayLimit)
})

const wrongRateDisplay = (q) => `${((q.wrongRate || q.errorRate || 0) * 100).toFixed(0)}%`

const barWidth = (rate) => Math.min(100, Math.max(0, (rate || 0) * 100))

const difficultyClass = (rate) => {
  const r = (rate || 0) * 100
  if (r >= 70) return 'dic__difficulty-badge--critical'
  if (r >= 50) return 'dic__difficulty-badge--warning'
  if (r >= 30) return 'dic__difficulty-badge--caution'
  return 'dic__difficulty-badge--ok'
}

const toggleExpand = () => {
  expanded.value = !expanded.value
}
</script>


<style scoped>
.dic {
  background: var(--ds-surface);
  border: 1.5px solid var(--ds-border);
  border-radius: var(--ds-radius-2xl);
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.dark .dic {
  border-color: var(--ds-border-strong);
}

/* Header */
.dic__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  padding: 1rem 1.125rem;
  border-bottom: 1px solid var(--ds-border);
}

.dark .dic__header { border-bottom-color: var(--ds-border-strong); }

.dic__header-left {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.dic__icon {
  width: 40px;
  height: 40px;
  border-radius: var(--ds-radius-lg);
  background: rgba(217, 119, 6, 0.08);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--ds-warning);
  flex-shrink: 0;
}


.dic__title {
  font-family: var(--ds-font-display);
  font-size: 0.875rem;
  font-weight: 800;
  color: var(--ds-text);
  margin: 0;
}

.dark .dic__title { color: #f1f5f9; }

.dic__subtitle {
  font-size: 0.7rem;
  color: var(--ds-text-muted);
  margin: 0.25rem 0 0;
  font-weight: 500;
}

.dic__badge {
  padding: 0.2rem 0.625rem;
  border-radius: var(--ds-radius-full);
  background: var(--ds-warning-soft);
  color: var(--ds-warning);
  font-size: 0.7rem;
  font-weight: 800;
}

/* Loading skeleton */
.dic__loading {
  padding: 0.75rem;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.dic__skeleton-item {
  display: flex;
  align-items: center;
  gap: 0.625rem;
}

.dic__skeleton {
  background: linear-gradient(90deg, var(--ds-gray-100) 25%, var(--ds-gray-200) 50%, var(--ds-gray-100) 75%);
  background-size: 200% 100%;
  animation: dicShimmer 1.5s infinite;
  border-radius: var(--ds-radius-md);
}

.dark .dic__skeleton {
  background: linear-gradient(90deg, var(--ds-gray-800) 25%, var(--ds-gray-700) 50%, var(--ds-gray-800) 75%);
  background-size: 200% 100%;
}

@keyframes dicShimmer {
  0% { background-position: -200% 0; }
  100% { background-position: 200% 0; }
}

.dic__skeleton--num { width: 28px; height: 28px; border-radius: 50%; flex-shrink: 0; }
.dic__skeleton--badge { width: 48px; height: 24px; flex-shrink: 0; }
.dic__skeleton-content { flex: 1; display: flex; flex-direction: column; gap: 0.375rem; }
.dic__skeleton--title { height: 14px; width: 80%; }
.dic__skeleton--bar { height: 6px; width: 100%; border-radius: 3px; }

/* Empty */
.dic__empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.5rem;
  padding: 2rem 1rem;
  text-align: center;
  color: var(--ds-success);
}

.dic__empty p { font-size: 0.8rem; font-weight: 600; margin: 0; }

/* List */
.dic__list {
  display: flex;
  flex-direction: column;
}

/* Item */
.dic__item {
  padding: 0.75rem 1rem;
  cursor: pointer;
  transition: background 0.1s ease;
  border-bottom: 1px solid var(--ds-border);
}

.dark .dic__item { border-bottom-color: var(--ds-border-strong); }
.dic__item:last-child { border-bottom: none; }
.dic__item:hover { background: var(--ds-gray-50); }
.dark .dic__item:hover { background: var(--ds-gray-800); }

.dic__item-main {
  display: flex;
  align-items: flex-start;
  gap: 0.625rem;
}

.dic__item-num {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: var(--ds-gray-100);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.65rem;
  font-weight: 800;
  color: var(--ds-text-muted);
  flex-shrink: 0;
  margin-top: 0.125rem;
}

.dark .dic__item-num { background: var(--ds-gray-700); }

.dic__item-content {
  flex: 1;
  min-width: 0;
}

.dic__item-text {
  font-size: 0.8rem;
  font-weight: 600;
  color: var(--ds-text);
  margin: 0;
  line-height: 1.3;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.dark .dic__item-text { color: #f1f5f9; }

.dic__item-meta {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-top: 0.25rem;
  flex-wrap: wrap;
}

.dic__meta-tag {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  font-size: 0.65rem;
  color: var(--ds-text-muted);
  font-weight: 600;
}


/* Difficulty badge */
.dic__difficulty-badge {
  padding: 0.2rem 0.5rem;
  border-radius: var(--ds-radius-full);
  font-size: 0.7rem;
  font-weight: 800;
  flex-shrink: 0;
  white-space: nowrap;
}

.dic__difficulty-badge--critical {
  background: var(--ds-danger-soft);
  color: var(--ds-danger);
}

.dic__difficulty-badge--warning {
  background: rgba(234, 179, 8, 0.1);
  color: #d97706;
}

.dic__difficulty-badge--caution {
  background: rgba(234, 179, 8, 0.08);
  color: #eab308;
}

.dic__difficulty-badge--ok {
  background: var(--ds-success-soft);
  color: var(--ds-success);
}

/* Bar */
.dic__item-bar {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-top: 0.5rem;
}

.dic__bar-track {
  flex: 1;
  height: 6px;
  background: var(--ds-gray-100);
  border-radius: 3px;
  overflow: hidden;
}

.dark .dic__bar-track { background: var(--ds-gray-700); }

.dic__bar-fill {
  height: 100%;
  border-radius: 3px;
  transition: width 0.4s ease;
}

.dic__bar-fill--critical { background: var(--ds-danger); }
.dic__bar-fill--warning { background: #d97706; }
.dic__bar-fill--caution { background: #eab308; }
.dic__bar-fill--ok { background: var(--ds-success); }

.dic__bar-label {
  font-size: 0.65rem;
  color: var(--ds-text-muted);
  font-weight: 600;
  white-space: nowrap;
  min-width: 48px;
  text-align: right;
}

/* Footer */
.dic__footer {
  padding: 0.5rem 1rem;
  border-top: 1px solid var(--ds-border);
  background: var(--ds-gray-50);
}

.dark .dic__footer {
  border-top-color: var(--ds-border-strong);
  background: var(--ds-gray-800);
}

.dic__show-more {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  font-size: 0.75rem;
  font-weight: 700;
  color: var(--ds-text-muted);
  background: transparent;
  border: none;
  cursor: pointer;
  transition: color 0.12s ease;
  padding: 0.25rem;
  font-family: inherit;
}

.dark .dic__show-more { color: #94a3b8; }
.dic__show-more:hover { color: var(--ds-primary); }

/* All link */
.dic__all-link {
  padding: 0.5rem 1rem;
  border-top: 1px solid var(--ds-border);
}

.dark .dic__all-link { border-top-color: var(--ds-border-strong); }

.dic__view-all {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.8rem;
  font-weight: 700;
  color: var(--ds-primary);
  background: transparent;
  border: none;
  cursor: pointer;
  transition: color 0.12s ease;
  padding: 0.25rem;
  font-family: inherit;
}

.dic__view-all:hover { color: var(--ds-primary-hover, #4338ca); }
</style>
