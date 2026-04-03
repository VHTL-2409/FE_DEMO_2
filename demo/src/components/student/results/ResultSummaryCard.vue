<template>
  <div class="rsc">
    <!-- Summary grid -->
    <div class="rsc__grid">
      <div class="rsc__card rsc__card--primary hover:shadow-md transition-shadow">
        <div class="rsc__card-icon">
          <LucideIcon name="quiz" />
        </div>
        <div class="rsc__card-content">
          <span class="rsc__card-val">{{ total }}</span>
          <span class="rsc__card-lbl">Tổng bài đã thi</span>
        </div>
      </div>

      <div class="rsc__card hover:shadow-md transition-shadow">
        <div class="rsc__card-icon rsc__card-icon--avg">
          <LucideIcon name="analytics" />
        </div>
        <div class="rsc__card-content">
          <span class="rsc__card-val">{{ avgScore }}</span>
          <span class="rsc__card-lbl">Điểm trung bình / 10</span>
        </div>
      </div>

      <div class="rsc__card hover:shadow-md transition-shadow">
        <div class="rsc__card-icon rsc__card-icon--latest">
          <LucideIcon name="stars" />
        </div>
        <div class="rsc__card-content">
          <span class="rsc__card-val">{{ latestScore }}</span>
          <span class="rsc__card-lbl">Kết quả mới nhất</span>
        </div>
      </div>

      <div class="rsc__card hover:shadow-md transition-shadow">
        <div class="rsc__card-icon rsc__card-icon--best">
          <LucideIcon name="emoji_events" />
        </div>
        <div class="rsc__card-content">
          <span class="rsc__card-val">{{ bestScore }}</span>
          <span class="rsc__card-lbl">Điểm cao nhất</span>
        </div>
      </div>
    </div>

    <!-- Progress bar -->
    <div v-if="total > 0" class="rsc__progress-section">
      <div class="rsc__progress-header">
        <span>Tiến độ hoàn thành</span>
        <span>{{ completedCount }} / {{ total }} bài</span>
      </div>
      <div class="rsc__progress-bar">
        <div class="rsc__progress-fill" :style="{ width: `${completionRate}%` }" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  attempts: { type: Array, default: () => [] }
})

const total = computed(() => props.attempts.length)

const scores = computed(() =>
  props.attempts
    .map(a => Number(a.score || 0))
    .filter(s => s > 0)
)

const avgScore = computed(() => {
  if (!scores.value.length) return '-'
  const avg = scores.value.reduce((a, b) => a + b, 0) / scores.value.length
  return (avg / 10).toFixed(1)
})

const latestScore = computed(() => {
  if (!props.attempts.length) return '-'
  const sorted = [...props.attempts].sort((a, b) => {
    const at = new Date(a.submittedAt || a.startedAt || 0).getTime()
    const bt = new Date(b.submittedAt || b.startedAt || 0).getTime()
    return bt - at
  })
  return sorted.length ? (Number(sorted[0].score || 0) / 10).toFixed(1) : '-'
})

const bestScore = computed(() => {
  if (!scores.value.length) return '-'
  return (Math.max(...scores.value) / 10).toFixed(1)
})

const completedCount = computed(() => props.attempts.filter(a => a.status === 'COMPLETED' || a.score > 0).length)

const completionRate = computed(() => {
  if (!total.value) return 0
  return Math.round((completedCount.value / total.value) * 100)
})
</script>


<style scoped>
.rsc {
  display: flex;
  flex-direction: column;
  gap: 0.875rem;
  background: var(--ds-surface);
  border: 1.5px solid var(--ds-border);
  border-radius: var(--ds-radius-2xl);
  padding: 1.125rem 1.25rem;
  min-width: 0;
  width: 100%;
  max-width: 100%;
  box-sizing: border-box;
}

.dark .rsc { border-color: var(--ds-border-strong); }

.rsc__grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(9.25rem, 1fr));
  gap: 0.625rem;
}

.rsc__card {
  display: flex;
  align-items: center;
  gap: 0.625rem;
  padding: 0.75rem 1rem;
  border-radius: var(--ds-radius-xl);
  background: var(--ds-gray-50);
  border: 1px solid var(--ds-border);
  transition: all 0.12s ease;
}

.dark .rsc__card { background: var(--ds-gray-800); border-color: var(--ds-border-strong); }

.rsc__card--primary {
  background: var(--ds-primary-soft);
  border-color: rgba(79, 70, 229, 0.15);
}

.rsc__card-icon {
  width: 34px;
  height: 34px;
  border-radius: var(--ds-radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  background: var(--ds-gray-100);
  color: var(--ds-gray-500);
}

.dark .rsc__card-icon { background: var(--ds-gray-700); }

.rsc__card--primary .rsc__card-icon { background: rgba(79, 70, 229, 0.15); color: var(--ds-primary); }
.rsc__card-icon--avg { background: rgba(14, 165, 233, 0.1); color: var(--ds-info); }
.rsc__card-icon--latest { background: rgba(16, 185, 129, 0.1); color: var(--ds-success); }
.rsc__card-icon--best { background: rgba(234, 179, 8, 0.1); color: var(--ds-warning); }


.rsc__card > div:last-child { min-width: 0; }

.rsc__card-val {
  display: block;
  font-family: var(--ds-font-display);
  font-size: 1.125rem;
  font-weight: 900;
  color: var(--ds-text);
  line-height: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.dark .rsc__card-val { color: #f1f5f9; }

.rsc__card-lbl {
  display: block;
  font-size: 0.6rem;
  font-weight: 700;
  color: var(--ds-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.06em;
  margin-top: 0.2rem;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* Progress */
.rsc__progress-section {
  padding: 0.75rem 0 0;
  border-top: 1px solid var(--ds-border);
}

.dark .rsc__progress-section { border-top-color: var(--ds-border-strong); }

.rsc__progress-header {
  display: flex;
  justify-content: space-between;
  font-size: 0.72rem;
  font-weight: 600;
  color: var(--ds-text-muted);
  margin-bottom: 0.5rem;
}

.rsc__progress-bar {
  height: 8px;
  border-radius: var(--ds-radius-full);
  background: var(--ds-gray-100);
  overflow: hidden;
}

.dark .rsc__progress-bar { background: var(--ds-gray-800); }

.rsc__progress-fill {
  height: 100%;
  border-radius: var(--ds-radius-full);
  background: linear-gradient(90deg, var(--ds-primary), #818cf8);
  transition: width 0.8s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  overflow: hidden;
  box-shadow: 0 0 10px rgba(79, 70, 229, 0.3);
}

.rsc__progress-fill::after {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255,255,255,0.4), transparent);
  animation: progressShine 2.5s ease-in-out infinite;
}

@keyframes progressShine {
  0% { left: -100%; }
  50%, 100% { left: 100%; }
}

/* Card hover effects */
.rsc__card {
  transition: all 0.25s ease;
}

.rsc__card:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06);
}

.rsc__card:hover .rsc__card-icon {
  transform: scale(1.08);
}

.rsc__card-icon {
  transition: transform 0.25s ease;
}

/* auto-fit đã bọc hàng; giữ 2 cột tối thiểu trên màn hẹp */
@media (max-width: 380px) {
  .rsc__grid { grid-template-columns: 1fr; }
}
</style>
