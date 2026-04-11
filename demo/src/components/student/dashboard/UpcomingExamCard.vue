<template>
  <div class="uec">
    <!-- Header -->
    <div class="uec__header">
      <div class="uec__header-left">
        <div class="uec__icon">
          <LucideIcon name="event" />
        </div>
        <div>
          <h3 class="uec__title">Kỳ thi sắp tới</h3>
          <p class="uec__subtitle">{{ upcomingExams.length }} kỳ thi</p>
        </div>
      </div>
      <button type="button" class="uec__see-all" @click="$emit('see-all')">
        Tất cả
        <LucideIcon name="chevron_right" />
      </button>
    </div>

    <!-- Loading -->
    <div v-if="loading" class="uec__loading">
      <div v-for="i in 3" :key="i" class="uec__skel-item" :style="{ animationDelay: `${i * 0.08}s` }">
        <div class="uec__skel uec__skel--icon" />
        <div class="uec__skel-content">
          <div class="uec__skel uec__skel--title" />
          <div class="uec__skel uec__skel--meta" />
        </div>
      </div>
    </div>

    <!-- Empty -->
    <div v-else-if="!upcomingExams.length" class="uec__empty">
      <LucideIcon name="event_available" />
      <p>Không có kỳ thi sắp tới</p>
      <span>Luyện tập thêm để cải thiện điểm số</span>
    </div>

    <!-- Exam list -->
    <div v-else class="uec__list">
      <div
        v-for="exam in displayExams"
        :key="exam.id || exam.examId"
        class="uec__item"
        :class="{ 'uec__item--urgent': isUrgent(exam) }"
        @click="$emit('exam-click', exam)"
      >
        <!-- Urgency badge -->
        <div v-if="isUrgent(exam)" class="uec__urgent-badge">
          <LucideIcon name="schedule" />
          Sắp hết hạn
        </div>

        <div class="uec__item-main">
          <div class="uec__item-icon" :class="urgencyClass(exam)">
            <LucideIcon name="quiz" />
          </div>
          <div class="uec__item-info">
            <p class="uec__item-title">{{ exam.title || exam.examTitle || 'Kỳ thi' }}</p>
            <div class="uec__item-meta">
              <span class="uec__meta-item">
                <LucideIcon name="schedule" />
                {{ formatDateTime(exam.startTime || exam.startDate) }}
              </span>
              <span v-if="exam.duration" class="uec__meta-item">
                <LucideIcon name="timer" />
                {{ exam.duration }} phút
              </span>
              <span v-if="exam.questionCount" class="uec__meta-item">
                <LucideIcon name="help" />
                {{ exam.questionCount }} câu
              </span>
            </div>
          </div>
        </div>

        <!-- Countdown -->
        <div class="uec__countdown" :class="urgencyClass(exam)">
          <span class="uec__countdown-val">{{ countdownLabel(exam) }}</span>
          <span class="uec__countdown-lbl">{{ countdownSub(exam) }}</span>
        </div>
      </div>

    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  upcomingExams: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false },
  displayLimit: { type: Number, default: 3 }
})

defineEmits(['exam-click', 'see-all'])

const displayExams = computed(() =>
  props.upcomingExams.slice(0, props.displayLimit)
)

const isUrgent = (exam) => {
  if (!exam.startTime && !exam.startDate) return false
  const start = new Date(exam.startTime || exam.startDate).getTime()
  const now = Date.now()
  const hoursLeft = (start - now) / 1000 / 60 / 60
  return hoursLeft > 0 && hoursLeft <= 24
}

const urgencyClass = (exam) => {
  if (isUrgent(exam)) return 'uec__item-icon--urgent'
  if (!exam.startTime && !exam.startDate) return ''
  const start = new Date(exam.startTime || exam.startDate).getTime()
  const daysLeft = (start - Date.now()) / 1000 / 60 / 60 / 24
  if (daysLeft <= 3) return 'uec__item-icon--soon'
  return ''
}

const countdownLabel = (exam) => {
  if (!exam.startTime && !exam.startDate) return '—'
  const start = new Date(exam.startTime || exam.startDate).getTime()
  const now = Date.now()
  const diff = start - now
  if (diff <= 0) return 'Đã bắt đầu'
  const mins = Math.floor(diff / 1000 / 60)
  const hours = Math.floor(diff / 1000 / 60 / 60)
  const days = Math.floor(diff / 1000 / 60 / 60 / 24)
  if (days > 0) return `${days}d`
  if (hours > 0) return `${hours}h`
  return `${mins}m`
}

const countdownSub = (exam) => {
  if (!exam.startTime && !exam.startDate) return ''
  const start = new Date(exam.startTime || exam.startDate).getTime()
  const now = Date.now()
  const diff = start - now
  if (diff <= 0) return 'bắt đầu rồi'
  const mins = Math.floor(diff / 1000 / 60)
  const hours = Math.floor(diff / 1000 / 60 / 60)
  const days = Math.floor(diff / 1000 / 60 / 60 / 24)
  if (days > 0) return `ngày nữa`
  if (hours > 0) return `giờ nữa`
  return `phút nữa`
}

const formatDateTime = (value) => {
  if (!value) return '—'
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return '—'
  return d.toLocaleString('vi-VN', {
    day: '2-digit', month: '2-digit', hour: '2-digit', minute: '2-digit'
  })
}
</script>


<style scoped>
@media (prefers-reduced-motion: reduce) {
  * {
    transition-duration: 0.01ms !important;
    animation-duration: 0.01ms !important;
  }
}.uec {
  background: var(--ds-surface);
  border: 1.5px solid var(--ds-border);
  border-radius: var(--ds-radius-2xl);
  overflow: hidden;
  /* Optimize paint layers */
  content-visibility: auto;
  contain: layout style;
  will-change: auto;
  transform: translateZ(0);
}

.dark .uec {
  border-color: var(--ds-border-strong);
}

/* Entrance: handled by parent sdl__main — no duplicate animation layer */

/* Header */
.uec__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  padding: 1rem 1.125rem;
  border-bottom: 1px solid var(--ds-border);
  background: var(--ds-gray-50);
}

.dark .uec__header {
  border-bottom-color: var(--ds-border-strong);
  background: var(--ds-gray-800);
}

.uec__header-left {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.uec__icon {
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


.uec__title {
  font-family: var(--ds-font-display);
  font-size: 0.875rem;
  font-weight: 800;
  color: var(--ds-text);
  margin: 0;
}

.dark .uec__title {
  color: #f1f5f9;
}

.uec__subtitle {
  font-size: 0.7rem;
  color: var(--ds-text-muted);
  margin: 0.2rem 0 0;
  font-weight: 500;
}

.uec__see-all {
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

.uec__see-all:hover {
  color: var(--ds-primary-hover, #4338ca);
}


/* Loading */
.uec__loading {
  padding: 0.75rem;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.uec__skel-item {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  animation: uecFadeIn 0.4s cubic-bezier(0.34, 1.2, 0.64, 1) both;
}

@keyframes uecFadeIn {
  from { opacity: 0; transform: translateY(6px); }
  to { opacity: 1; transform: translateY(0); }
}

.uec__skel {
  background: var(--ds-gray-200);
  border-radius: var(--ds-radius-md);
  will-change: opacity;
  animation: uecShimmer 1.4s ease-in-out infinite;
}

.dark .uec__skel {
  background: var(--ds-gray-700);
}

@keyframes uecShimmer {
  0%   { opacity: 0.6; transform: scaleX(0.3) translateZ(0); }
  50%  { opacity: 1;   transform: scaleX(1)   translateZ(0); }
  100% { opacity: 0.6; transform: scaleX(0.3) translateZ(0); }
}

.uec__skel--icon { width: 40px; height: 40px; border-radius: var(--ds-radius-xl); flex-shrink: 0; }
.uec__skel-content { flex: 1; display: flex; flex-direction: column; gap: 0.375rem; }
.uec__skel--title { height: 14px; width: 70%; }
.uec__skel--meta { height: 10px; width: 50%; }

/* Empty */
.uec__empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.5rem;
  padding: 2rem 1rem;
  text-align: center;
}


.uec__empty p {
  font-size: 0.875rem;
  font-weight: 700;
  color: var(--ds-text);
  margin: 0;
}

.dark .uec__empty p { color: #f1f5f9; }

.uec__empty span:last-child {
  font-size: 0.75rem;
  color: var(--ds-text-muted);
  font-weight: 500;
}

/* List */
.uec__list {
  display: flex;
  flex-direction: column;
}

/* Item */
.uec__item {
  padding: 0.875rem 1.125rem;
  border-bottom: 1px solid var(--ds-border);
  cursor: pointer;
  transition: background 0.1s ease;
  position: relative;
}

.dark .uec__item {
  border-bottom-color: var(--ds-border-strong);
}

.uec__item:last-child { border-bottom: none; }
.uec__item:hover { background: var(--ds-gray-50); }
.dark .uec__item:hover { background: var(--ds-gray-800); }

.uec__item--urgent {
  background: rgba(220, 38, 38, 0.03);
}

.dark .uec__item--urgent {
  background: rgba(220, 38, 38, 0.05);
}

/* Urgent badge */
.uec__urgent-badge {
  display: inline-flex;
  align-items: center;
  gap: 0.3rem;
  padding: 0.2rem 0.5rem;
  border-radius: var(--ds-radius-full);
  background: var(--ds-danger-soft);
  color: var(--ds-danger);
  font-size: 0.65rem;
  font-weight: 800;
  margin-bottom: 0.5rem;
}


.uec__item-main {
  display: flex;
  align-items: flex-start;
  gap: 0.75rem;
}

.uec__item-icon {
  width: 40px;
  height: 40px;
  border-radius: var(--ds-radius-xl);
  background: var(--ds-primary-soft);
  color: var(--ds-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}


.uec__item-icon--urgent {
  background: var(--ds-danger-soft);
  color: var(--ds-danger);
}

.uec__item-icon--soon {
  background: rgba(234, 179, 8, 0.1);
  color: var(--ds-warning);
}

.uec__item-info {
  flex: 1;
  min-width: 0;
}

.uec__item-title {
  font-size: 0.875rem;
  font-weight: 700;
  color: var(--ds-text);
  margin: 0 0 0.375rem;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.dark .uec__item-title { color: #f1f5f9; }

.uec__item-meta {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.uec__meta-item {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  font-size: 0.7rem;
  color: var(--ds-text-muted);
  font-weight: 500;
}


/* Countdown */
.uec__countdown {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 0.125rem;
  flex-shrink: 0;
  margin-left: 0.5rem;
}

.uec__countdown-val {
  font-family: var(--ds-font-display);
  font-size: 1.25rem;
  font-weight: 900;
  color: var(--ds-text);
  line-height: 1;
}

.dark .uec__countdown-val { color: #f1f5f9; }

.uec__countdown.uec__item-icon--urgent .uec__countdown-val {
  color: var(--ds-danger);
}

.uec__countdown.uec__item-icon--soon .uec__countdown-val {
  color: var(--ds-warning);
}

.uec__countdown-lbl {
  font-size: 0.6rem;
  font-weight: 600;
  color: var(--ds-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

/* Show more */
.uec__show-more {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.375rem;
  width: 100%;
  padding: 0.75rem;
  font-size: 0.8rem;
  font-weight: 700;
  color: var(--ds-primary);
  background: var(--ds-gray-50);
  border: none;
  border-top: 1px solid var(--ds-border);
  cursor: pointer;
  transition: color 0.12s ease, background-color 0.12s ease, border-color 0.12s ease, box-shadow 0.12s ease, transform 0.12s ease;
  font-family: inherit;
}

.dark .uec__show-more {
  background: var(--ds-gray-800);
  border-top-color: var(--ds-border-strong);
}

.uec__show-more:hover {
  background: var(--ds-primary-soft);
}

</style>
@media (prefers-reduced-motion: reduce) {
  * {
    transition-duration: 0.01ms !important;
    animation-duration: 0.01ms !important;
  }
}
