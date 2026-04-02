<template>
  <div class="ehl">
    <!-- Loading -->
    <div v-if="loading" class="ehl__loading">
      <div v-for="i in 4" :key="i" class="ehl__skel-card">
        <div class="ehl__skel ehl__skel--icon" />
        <div class="ehl__skel-body">
          <div class="ehl__skel ehl__skel--title" />
          <div class="ehl__skel ehl__skel--meta" />
        </div>
        <div class="ehl__skel ehl__skel--score" />
      </div>
    </div>

    <!-- Empty -->
    <div v-else-if="!sessions.length" class="ehl__empty">
      <div class="ehl__empty-icon">
        <LucideIcon :name="emptyIcon" />
      </div>
      <h3 class="ehl__empty-title">{{ emptyTitle }}</h3>
      <p class="ehl__empty-desc">{{ emptyDesc }}</p>
      <button type="button" class="ehl__empty-cta" @click="$emit('go-dashboard')">
        <LucideIcon name="home" />
        Về Dashboard
      </button>
    </div>

    <!-- List -->
    <div v-else class="ehl__list">
      <div
        v-for="session in sessions"
        :key="session.attemptId"
        class="ehl__card"
        :class="{ 'ehl__card--new': session.isNew }"
        @click="$emit('session-click', session)"
      >
        <!-- Type badge -->
        <div class="ehl__type-badge" :class="session.isPractice ? 'ehl__type-badge--practice' : 'ehl__type-badge--exam'">
          <LucideIcon :name="session.isPractice ? 'model_training' : 'menu_book'" />
        </div>

        <!-- Main content -->
        <div class="ehl__main">
          <div class="ehl__title-row">
            <h3 class="ehl__title">{{ session.subject }}</h3>
            <span v-if="session.isNew" class="ehl__new-chip">Moi</span>
          </div>
          <div class="ehl__meta-row">
            <span class="ehl__meta-item">
              <LucideIcon name="calendar_today" />
              {{ session.date }}
            </span>
            <span v-if="session.timeTaken && session.timeTaken !== '-'" class="ehl__meta-item">
              <LucideIcon name="timer" />
              {{ session.timeTaken }}
            </span>
          </div>
        </div>

        <!-- Score -->
        <div class="ehl__score-col">
          <div class="ehl__score-ring" :class="scoreRingClass(session.score)">
            <span class="ehl__score-val">{{ session.score }}</span>
          </div>
          <span class="ehl__score-lbl">/ 10</span>
        </div>

        <!-- Arrow -->
        <div class="ehl__arrow">
          <LucideIcon name="chevron_right" />
        </div>
      </div>

      <!-- Pagination -->
      <div v-if="totalPages > 1" class="ehl__pagination">
        <span class="ehl__pagination-info">Hiển thị {{ sessions.length }} / {{ totalCount }} phiên</span>
        <div class="ehl__pagination-btns">
          <button
            type="button"
            class="ehl__page-btn"
            :disabled="currentPage <= 1"
            @click="$emit('prev-page')"
          >
            <LucideIcon name="chevron_left" />
          </button>
          <span class="ehl__page-current">Trang {{ currentPage }} / {{ totalPages }}</span>
          <button
            type="button"
            class="ehl__page-btn"
            :disabled="currentPage >= totalPages"
            @click="$emit('next-page')"
          >
            <LucideIcon name="chevron_right" />
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  sessions: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false },
  emptyType: { type: String, default: 'exam' },
  currentPage: { type: Number, default: 1 },
  totalPages: { type: Number, default: 1 },
  totalCount: { type: Number, default: 0 }
})

defineEmits(['session-click', 'go-dashboard', 'prev-page', 'next-page'])

const emptyConfig = {
  exam: { icon: 'menu_book', title: 'Chưa có bài thi nào', desc: 'Tham gia bài thi để xem kết quả tại đây.' },
  practice: { icon: 'model_training', title: 'Chưa có luyện tập nào', desc: 'Bắt đầu luyện tập để xem tiến độ tại đây.' }
}

const emptyIcon = computed(() => emptyConfig[props.emptyType]?.icon || 'history')
const emptyTitle = computed(() => emptyConfig[props.emptyType]?.title || 'Không có dữ liệu')
const emptyDesc = computed(() => emptyConfig[props.emptyType]?.desc || '')

const scoreRingClass = (score) => {
  const s = Number(score || 0)
  if (s >= 80) return 'ehl__score-ring--high'
  if (s >= 50) return 'ehl__score-ring--mid'
  return 'ehl__score-ring--low'
}
</script>


<style scoped>
.ehl { display: flex; flex-direction: column; gap: 0.75rem; }

/* Skeleton */
.ehl__loading { display: flex; flex-direction: column; gap: 0.75rem; }

.ehl__skel-card {
  display: flex;
  align-items: center;
  gap: 0.875rem;
  padding: 1rem;
  border-radius: var(--ds-radius-2xl);
  border: 1.5px solid var(--ds-border);
  background: var(--ds-surface);
}

.ehl__skel {
  background: linear-gradient(90deg, var(--ds-gray-100) 25%, var(--ds-gray-200) 50%, var(--ds-gray-100) 75%);
  background-size: 200% 100%;
  animation: ehlShimmer 1.5s infinite;
  border-radius: var(--ds-radius-md);
}

.dark .ehl__skel {
  background: linear-gradient(90deg, var(--ds-gray-800) 25%, var(--ds-gray-700) 50%, var(--ds-gray-800) 75%);
  background-size: 200% 100%;
}

@keyframes ehlShimmer {
  0% { background-position: -200% 0; }
  100% { background-position: 200% 0; }
}

.ehl__skel--icon { width: 48px; height: 48px; border-radius: var(--ds-radius-xl); flex-shrink: 0; }
.ehl__skel-body { flex: 1; display: flex; flex-direction: column; gap: 0.5rem; }
.ehl__skel--title { height: 14px; width: 55%; }
.ehl__skel--meta { height: 10px; width: 35%; }
.ehl__skel--score { width: 56px; height: 56px; border-radius: 50%; flex-shrink: 0; }

/* Empty */
.ehl__empty {
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

.dark .ehl__empty { border-color: var(--ds-border-strong); }

.ehl__empty-icon {
  width: 64px;
  height: 64px;
  border-radius: var(--ds-radius-2xl);
  background: var(--ds-gray-100);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--ds-gray-400);
}

.dark .ehl__empty-icon { background: var(--ds-gray-800); color: var(--ds-gray-500); }

.ehl__empty-title { font-size: 1rem; font-weight: 800; color: var(--ds-text); margin: 0; }
.dark .ehl__empty-title { color: #f1f5f9; }

.ehl__empty-desc { font-size: 0.8rem; color: var(--ds-text-muted); margin: 0; max-width: 300px; line-height: 1.5; }

.ehl__empty-cta {
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

.ehl__empty-cta:hover { background: var(--ds-primary-hover, #4338ca); transform: translateY(-1px); }

/* List */
.ehl__list { display: flex; flex-direction: column; gap: 0.75rem; }

/* Card */
.ehl__card {
  display: flex;
  align-items: center;
  gap: 0.875rem;
  padding: 1rem 1.25rem;
  background: var(--ds-surface);
  border: 1.5px solid var(--ds-border);
  border-radius: var(--ds-radius-2xl);
  cursor: pointer;
  transition: all 0.2s ease;
  overflow: hidden;
  position: relative;
}

.dark .ehl__card { border-color: var(--ds-border-strong); }

.ehl__card:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06);
  border-color: var(--ds-primary-border);
}

.ehl__card--new {
  border-color: rgba(79, 70, 229, 0.2);
  background: rgba(79, 70, 229, 0.01);
}

.dark .ehl__card--new { background: rgba(79, 70, 229, 0.04); }

/* Type badge */
.ehl__type-badge {
  width: 48px;
  height: 48px;
  border-radius: var(--ds-radius-xl);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: transform 0.2s ease;
}

.ehl__card:hover .ehl__type-badge {
  transform: scale(1.05);
}


.ehl__type-badge--exam { background: var(--ds-info-soft); color: var(--ds-info); }
.ehl__type-badge--practice { background: var(--ds-primary-soft); color: var(--ds-primary); }

/* Main */
.ehl__main { flex: 1; min-width: 0; }

.ehl__title-row {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 0.375rem;
}

.ehl__title {
  font-size: 0.95rem;
  font-weight: 800;
  color: var(--ds-text);
  margin: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.dark .ehl__title { color: #f1f5f9; }

.ehl__new-chip {
  font-size: 0.6rem;
  font-weight: 800;
  color: white;
  background: var(--ds-primary);
  padding: 0.125rem 0.5rem;
  border-radius: var(--ds-radius-full);
  white-space: nowrap;
  flex-shrink: 0;
  animation: ehlPulse 2s ease-in-out infinite;
}

@keyframes ehlPulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.7; }
}

.ehl__meta-row {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.ehl__meta-item {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  font-size: 0.72rem;
  color: var(--ds-text-muted);
  font-weight: 500;
}


/* Score ring */
.ehl__score-col {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.125rem;
  flex-shrink: 0;
}

.ehl__score-ring {
  width: 52px;
  height: 52px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 3px solid;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
}

.ehl__card:hover .ehl__score-ring {
  transform: scale(1.08);
}

.ehl__score-ring--high { 
  border-color: var(--ds-success); 
  background: rgba(16, 185, 129, 0.1);
  box-shadow: 0 0 16px rgba(16, 185, 129, 0.2);
}
.ehl__score-ring--mid { 
  border-color: var(--ds-warning); 
  background: rgba(234, 179, 8, 0.1);
  box-shadow: 0 0 16px rgba(234, 179, 8, 0.2);
}
.ehl__score-ring--low { 
  border-color: var(--ds-danger); 
  background: rgba(220, 38, 38, 0.1);
  box-shadow: 0 0 16px rgba(220, 38, 38, 0.2);
}

.ehl__card:hover .ehl__score-ring--high { box-shadow: 0 0 12px rgba(16, 185, 129, 0.35); }
.ehl__card:hover .ehl__score-ring--mid { box-shadow: 0 0 12px rgba(234, 179, 8, 0.35); }
.ehl__card:hover .ehl__score-ring--low { box-shadow: 0 0 12px rgba(220, 38, 38, 0.35); }

.ehl__score-val {
  font-family: var(--ds-font-display);
  font-size: 0.95rem;
  font-weight: 900;
  color: var(--ds-text);
}

.dark .ehl__score-val { color: #f1f5f9; }

.ehl__score-lbl {
  font-size: 0.6rem;
  color: var(--ds-text-muted);
  font-weight: 700;
}

/* Arrow */
.ehl__arrow { color: var(--ds-text-muted); flex-shrink: 0; }
.ehl__card:hover .ehl__arrow { color: var(--ds-primary); }

/* Pagination */
.ehl__pagination {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.75rem 0 0;
  border-top: 1px solid var(--ds-border);
  margin-top: 0.25rem;
}

.dark .ehl__pagination { border-top-color: var(--ds-border-strong); }

.ehl__pagination-info { font-size: 0.75rem; color: var(--ds-text-muted); font-weight: 600; }

.ehl__pagination-btns { display: flex; align-items: center; gap: 0.5rem; }

.ehl__page-btn {
  width: 36px;
  height: 36px;
  border-radius: var(--ds-radius-lg);
  border: 1.5px solid var(--ds-border);
  background: var(--ds-surface);
  color: var(--ds-text-secondary);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.12s ease;
}

.dark .ehl__page-btn { border-color: var(--ds-border-strong); background: var(--ds-gray-800); }

.ehl__page-btn:hover:not(:disabled) { border-color: var(--ds-primary-border); color: var(--ds-primary); background: var(--ds-primary-soft); }
.ehl__page-btn:disabled { opacity: 0.4; cursor: not-allowed; }

.ehl__page-current { font-size: 0.8rem; font-weight: 600; color: var(--ds-text-muted); min-width: 80px; text-align: center; }

/* Responsive */
@media (max-width: 480px) {
  .ehl__card { flex-wrap: wrap; }
  .ehl__score-col { flex-direction: row; gap: 0.5rem; }
  .ehl__arrow { display: none; }
}
</style>
