<template>
  <div class="rrc">
    <!-- Header -->
    <div class="rrc__header">
      <div class="rrc__header-left">
        <div class="rrc__icon">
          <LucideIcon name="grade" />
        </div>
        <div>
          <h3 class="rrc__title">Kết quả gần đây</h3>
          <p class="rrc__subtitle">{{ results.length }} bài thi</p>
        </div>
      </div>
      <button type="button" class="rrc__see-all" @click="$emit('see-all')">
        Xem tất cả
        <LucideIcon name="chevron_right" />
      </button>
    </div>

    <!-- Loading -->
    <div v-if="loading" class="rrc__loading">
      <div v-for="i in 4" :key="i" class="rrc__skel-item" :style="{ animationDelay: `${i * 0.08}s` }">
        <div class="rrc__skel rrc__skel--icon" />
        <div class="rrc__skel-content">
          <div class="rrc__skel rrc__skel--title" />
          <div class="rrc__skel rrc__skel--meta" />
        </div>
        <div class="rrc__skel rrc__skel--score" />
      </div>
    </div>

    <!-- Empty -->
    <div v-else-if="!results.length" class="rrc__empty">
      <LucideIcon name="history" />
      <p>Chưa có kết quả thi nào</p>
      <span>Tìm kỳ thi để bắt đầu</span>
    </div>

    <!-- Results list -->
    <div v-else class="rrc__list">
      <div
        v-for="result in displayResults"
        :key="result.id || result.attemptId"
        class="rrc__item"
        @click="$emit('result-click', result)"
      >
        <!-- Score circle -->
        <div class="rrc__score-wrap" :class="scoreClass(result.score)">
          <span class="rrc__score-val">{{ formatScore(result.score) }}</span>
          <span class="rrc__score-max">/10</span>
        </div>

        <!-- Info -->
        <div class="rrc__item-info">
          <p class="rrc__item-title">{{ result.title || result.examTitle || 'Bài thi' }}</p>
          <div class="rrc__item-meta">
            <span class="rrc__meta-item">
              <LucideIcon name="schedule" />
              {{ formatDate(result.submittedAt || result.completedAt) }}
            </span>
            <span v-if="result.timeTaken" class="rrc__meta-item">
              <LucideIcon name="timer" />
              {{ result.timeTaken }}
            </span>
          </div>
        </div>

        <!-- Status chip -->
        <div class="rrc__item-status">
          <span class="rrc__status-chip" :class="passChipClass(result.score)">
            <LucideIcon :name="result.score >= 5 ? 'check_circle' : 'cancel'" />
            {{ result.score >= 5 ? 'Đạt' : 'Không đạt' }}
          </span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  results: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false },
  displayLimit: { type: Number, default: 5 }
})

defineEmits(['result-click', 'see-all'])

const displayResults = computed(() => props.results.slice(0, props.displayLimit))

const formatScore = (score) => {
  if (score === null || score === undefined) return '—'
  return (Number(score) / 10).toFixed(1)
}

const formatDate = (value) => {
  if (!value) return '—'
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return '—'
  return d.toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit' })
}

const scoreClass = (score) => {
  const s = Number(score)
  if (isNaN(s)) return ''
  if (s >= 80) return 'rrc__score-wrap--excellent'
  if (s >= 50) return 'rrc__score-wrap--pass'
  return 'rrc__score-wrap--fail'
}

const passChipClass = (score) => {
  const s = Number(score)
  if (isNaN(s)) return ''
  return s >= 50 ? 'rrc__status-chip--pass' : 'rrc__status-chip--fail'
}
</script>


<style scoped>
/* GPU-accelerated animations */
@keyframes fadeUpSm {
  from { opacity: 0; transform: translateY(10px) translateZ(0); }
  to   { opacity: 1; transform: translateY(0) translateZ(0); }
}

.rrc {
  background: var(--ds-surface);
  border: 1.5px solid var(--ds-border);
  border-radius: var(--ds-radius-2xl);
  overflow: hidden;
  /* GPU optimization - animate once */
  animation: fadeUpSm 0.45s cubic-bezier(0.16, 1, 0.3, 1) 0.3s both;
  will-change: transform, opacity;
  transform: translateZ(0);
  /* Optimize paint */
  contain: layout style;
}

.dark .rrc {
  border-color: var(--ds-border-strong);
}

/* Header */
.rrc__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  padding: 1rem 1.125rem;
  border-bottom: 1px solid var(--ds-border);
  background: var(--ds-gray-50);
}

.dark .rrc__header {
  border-bottom-color: var(--ds-border-strong);
  background: var(--ds-gray-800);
}

.rrc__header-left {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.rrc__icon {
  width: 40px;
  height: 40px;
  border-radius: var(--ds-radius-lg);
  background: var(--ds-success-soft);
  color: var(--ds-success);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}


.rrc__title {
  font-family: var(--ds-font-display);
  font-size: 0.875rem;
  font-weight: 800;
  color: var(--ds-text);
  margin: 0;
}

.dark .rrc__title { color: #f1f5f9; }

.rrc__subtitle {
  font-size: 0.7rem;
  color: var(--ds-text-muted);
  margin: 0.2rem 0 0;
  font-weight: 500;
}

.rrc__see-all {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  font-size: 0.8rem;
  font-weight: 700;
  color: var(--ds-primary);
  background: transparent;
  border: none;
  cursor: pointer;
  transition: color 0.12s ease;
  padding: 0.25rem;
  font-family: inherit;
  white-space: nowrap;
}

.rrc__see-all:hover {
  color: var(--ds-primary-hover, #4338ca);
}


/* Loading */
.rrc__loading {
  padding: 0.75rem;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.rrc__skel-item {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  animation: rrcFadeIn 0.4s cubic-bezier(0.34, 1.2, 0.64, 1) both;
}

@keyframes rrcFadeIn {
  from { opacity: 0; transform: translateX(-8px); }
  to { opacity: 1; transform: translateX(0); }
}

.rrc__skel {
  background: linear-gradient(90deg, var(--ds-gray-100) 25%, var(--ds-gray-200) 50%, var(--ds-gray-100) 75%);
  background-size: 200% 100%;
  animation: rrcShimmer 1.2s ease-in-out infinite;
  border-radius: var(--ds-radius-md);
}

.dark .rrc__skel {
  background: linear-gradient(90deg, var(--ds-gray-800) 25%, var(--ds-gray-700) 50%, var(--ds-gray-800) 75%);
  background-size: 200% 100%;
}

@keyframes rrcShimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

.rrc__skel-content { flex: 1; display: flex; flex-direction: column; gap: 0.375rem; }
.rrc__skel--title { height: 14px; width: 70%; }
.rrc__skel--meta { height: 10px; width: 50%; }
.rrc__skel--score { width: 48px; height: 48px; border-radius: 50%; flex-shrink: 0; }

/* Empty */
.rrc__empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.5rem;
  padding: 2rem 1rem;
  text-align: center;
}


.dark .rrc__empty p { color: #f1f5f9; }

.rrc__empty p {
  font-size: 0.875rem;
  font-weight: 700;
  color: var(--ds-text);
  margin: 0;
}

.rrc__empty span:last-child {
  font-size: 0.75rem;
  color: var(--ds-text-muted);
  font-weight: 500;
}

/* List */
.rrc__list {
  display: flex;
  flex-direction: column;
}

/* Item */
.rrc__item {
  display: flex;
  align-items: center;
  gap: 0.875rem;
  padding: 0.875rem 1.125rem;
  border-bottom: 1px solid var(--ds-border);
  cursor: pointer;
  transition: background 0.1s ease;
}

.dark .rrc__item { border-bottom-color: var(--ds-border-strong); }
.rrc__item:last-child { border-bottom: none; }
.rrc__item:hover { background: var(--ds-gray-50); }
.dark .rrc__item:hover { background: var(--ds-gray-800); }

/* Score badge */
.rrc__score-wrap {
  display: flex;
  align-items: baseline;
  gap: 1px;
  padding: 0.3rem 0.75rem;
  border-radius: var(--ds-radius-xl);
  border: 1px solid;
  flex-shrink: 0;
  transition: color 0.2s ease, background-color 0.2s ease, border-color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease;
}

.rrc__score-wrap--excellent {
  background: rgba(16, 185, 129, 0.1);
  border-color: rgba(16, 185, 129, 0.3);
  color: #059669;
}

.rrc__score-wrap--pass {
  background: rgba(79, 70, 229, 0.08);
  border-color: rgba(79, 70, 229, 0.25);
  color: var(--ds-primary);
}

.rrc__score-wrap--fail {
  background: rgba(220, 38, 38, 0.07);
  border-color: rgba(220, 38, 38, 0.22);
  color: #dc2626;
}

.dark .rrc__score-wrap--excellent { background: rgba(16, 185, 129, 0.15); border-color: rgba(16, 185, 129, 0.4); color: #6ee7b7; }
.dark .rrc__score-wrap--pass { background: rgba(79, 70, 229, 0.12); border-color: rgba(79, 70, 229, 0.35); color: #818cf8; }
.dark .rrc__score-wrap--fail { background: rgba(220, 38, 38, 0.12); border-color: rgba(220, 38, 38, 0.35); color: #fca5a5; }

.rrc__score-val {
  font-family: var(--ds-font-display);
  font-size: 1rem;
  font-weight: 900;
  line-height: 1;
}

.rrc__score-max {
  font-size: 0.6rem;
  font-weight: 600;
  opacity: 0.7;
}

/* Info */
.rrc__item-info {
  flex: 1;
  min-width: 0;
}

.rrc__item-title {
  font-size: 0.875rem;
  font-weight: 700;
  color: var(--ds-text);
  margin: 0 0 0.3rem;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.dark .rrc__item-title { color: #f1f5f9; }

.rrc__item-meta {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.rrc__meta-item {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  font-size: 0.7rem;
  color: var(--ds-text-muted);
  font-weight: 500;
}


/* Status chip */
.rrc__status-chip {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  padding: 0.25rem 0.625rem;
  border-radius: var(--ds-radius-full);
  font-size: 0.7rem;
  font-weight: 700;
  white-space: nowrap;
  flex-shrink: 0;
}


.rrc__status-chip--pass {
  background: var(--ds-success-soft);
  color: var(--ds-success);
}

.rrc__status-chip--fail {
  background: var(--ds-danger-soft);
  color: var(--ds-danger);
}
</style>
@media (prefers-reduced-motion: reduce) {
  * {
    transition-duration: 0.01ms !important;
    animation-duration: 0.01ms !important;
  }
}