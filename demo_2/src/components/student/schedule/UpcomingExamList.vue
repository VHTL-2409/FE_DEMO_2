<template>
  <div class="uel">
    <!-- Loading state -->
    <div v-if="loading" class="uel__loading">
      <div v-for="i in 4" :key="i" class="uel__skel-card">
        <div class="uel__skel uel__skel--icon" />
        <div class="uel__skel-body">
          <div class="uel__skel uel__skel--title" />
          <div class="uel__skel uel__skel--meta" />
        </div>
        <div class="uel__skel uel__skel--btn" />
      </div>
    </div>

    <!-- Empty state -->
    <div v-else-if="!exams.length" class="uel__empty">
      <div class="uel__empty-icon">
        <LucideIcon :name="emptyIcon" />
      </div>
      <h3 class="uel__empty-title">{{ emptyTitle }}</h3>
      <p class="uel__empty-desc">{{ emptyDesc }}</p>
      <button
        v-if="showCta"
        type="button"
        class="uel__empty-cta"
        @click="$emit('cta-click')"
      >
        <LucideIcon name="login" />
        Vao thi ngay
      </button>
    </div>

    <!-- Exam list -->
    <div v-else class="uel__list">
      <div v-if="sectionLabel" class="uel__section-label">
        {{ sectionLabel }}
      </div>

      <ExamScheduleCard
        v-for="exam in exams"
        :key="exam.id || exam.examId"
        :exam="exam"
        @click="$emit('exam-click', exam)"
        @enter="$emit('exam-enter', exam)"
        @details="$emit('exam-details', exam)"
      />

      <button
        v-if="hasMore"
        type="button"
        class="uel__load-more"
        @click="$emit('load-more')"
      >
        <LucideIcon name="expand_more" />
        Xem thêm {{ remainingCount }} kỳ thi
      </button>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import ExamScheduleCard from './ExamScheduleCard.vue'

const props = defineProps({
  exams: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false },
  sectionLabel: { type: String, default: '' },
  emptyType: {
    type: String,
    default: 'no-exams',
    validator: (v) => ['no-exams', 'no-results', 'all-done'].includes(v)
  },
  hasMore: { type: Boolean, default: false },
  remainingCount: { type: Number, default: 0 },
  showCta: { type: Boolean, default: true }
})

defineEmits(['exam-click', 'exam-enter', 'exam-details', 'load-more', 'cta-click'])

const emptyConfig = {
  'no-exams': { icon: 'event_available', title: 'Chưa có kỳ thi nào', desc: 'Không có kỳ thi nào trong hệ thống. Vui lòng chờ giáo viên tạo kỳ thi.' },
  'no-results': { icon: 'search_off', title: 'Không tìm thấy', desc: 'Không có kỳ thi phù hợp với bộ lọc của bạn.' },
  'all-done': { icon: 'celebration', title: 'Tất cả đã xong!', desc: 'Bạn đã hoàn thành tất cả các kỳ thi. Hãy luyện tập thêm để cải thiện điểm số.' }
}

const emptyIcon = computed(() => emptyConfig[props.emptyType]?.icon || 'event_available')
const emptyTitle = computed(() => emptyConfig[props.emptyType]?.title || 'Không có kỳ thi')
const emptyDesc = computed(() => emptyConfig[props.emptyType]?.desc || '')
</script>


<style scoped>
.uel__loading {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.uel__skel-card {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 1rem;
  border-radius: var(--ds-radius-2xl);
  border: 1.5px solid var(--ds-border);
  background: var(--ds-surface);
}

.uel__skel {
  background: linear-gradient(90deg, var(--ds-gray-100) 25%, var(--ds-gray-200) 50%, var(--ds-gray-100) 75%);
  background-size: 200% 100%;
  animation: uelShimmer 1.5s infinite;
  border-radius: var(--ds-radius-md);
}

.dark .uel__skel {
  background: linear-gradient(90deg, var(--ds-gray-800) 25%, var(--ds-gray-700) 50%, var(--ds-gray-800) 75%);
  background-size: 200% 100%;
}

@keyframes uelShimmer {
  0% { background-position: -200% 0; }
  100% { background-position: 200% 0; }
}

.uel__skel--icon { width: 80px; height: 60px; border-radius: var(--ds-radius-xl); flex-shrink: 0; }
.uel__skel-body { flex: 1; display: flex; flex-direction: column; gap: 0.5rem; }
.uel__skel--title { height: 14px; width: 60%; }
.uel__skel--meta { height: 10px; width: 40%; }
.uel__skel--btn { width: 120px; height: 36px; border-radius: var(--ds-radius-xl); flex-shrink: 0; }

/* Empty */
.uel__empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.75rem;
  padding: 3rem 1.5rem;
  text-align: center;
  background: var(--ds-surface);
  border: 1.5px solid var(--ds-border);
  border-radius: var(--ds-radius-2xl);
}

.dark .uel__empty { border-color: var(--ds-border-strong); }

.uel__empty-icon {
  width: 64px;
  height: 64px;
  border-radius: var(--ds-radius-2xl);
  background: var(--ds-gray-100);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--ds-gray-400);
}

.dark .uel__empty-icon { background: var(--ds-gray-800); color: var(--ds-gray-500); }


.uel__empty-title {
  font-size: 1rem;
  font-weight: 800;
  color: var(--ds-text);
  margin: 0;
}

.dark .uel__empty-title { color: #f1f5f9; }

.uel__empty-desc {
  font-size: 0.8rem;
  color: var(--ds-text-muted);
  margin: 0;
  max-width: 320px;
  line-height: 1.5;
}

.uel__empty-cta {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.625rem 1.25rem;
  border-radius: var(--ds-radius-xl);
  background: var(--ds-primary);
  color: white;
  font-size: 0.875rem;
  font-weight: 700;
  border: none;
  cursor: pointer;
  transition: all 0.12s ease;
  font-family: inherit;
  box-shadow: 0 2px 8px rgba(79, 70, 229, 0.2);
  margin-top: 0.5rem;
}

.uel__empty-cta:hover {
  background: var(--ds-primary-hover);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(79, 70, 229, 0.3);
}


/* Section label */
.uel__section-label {
  font-size: 0.7rem;
  font-weight: 800;
  color: var(--ds-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.08em;
  padding: 0.5rem 0.25rem;
  margin-bottom: 0.25rem;
}

/* Load more */
.uel__load-more {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.375rem;
  width: 100%;
  padding: 0.875rem;
  font-size: 0.8rem;
  font-weight: 700;
  color: var(--ds-primary);
  background: var(--ds-gray-50);
  border: none;
  border-top: 1px solid var(--ds-border);
  cursor: pointer;
  transition: all 0.12s ease;
  font-family: inherit;
  border-radius: 0 0 var(--ds-radius-2xl) var(--ds-radius-2xl);
}

.dark .uel__load-more { background: var(--ds-gray-800); border-top-color: var(--ds-border-strong); }
.uel__load-more:hover { background: var(--ds-primary-soft); }
</style>
