<template>
  <div class="sdi">
    <div class="sdi__grid">
      <div class="sdi__card sdi__card--trend">
        <ScoreTrendCard :attempts="chartAttempts" />
      </div>
      <div class="sdi__card sdi__card--summary">
        <div class="sdi__sum-head">
          <div class="sdi__sum-icon">
            <LucideIcon name="bar_chart" />
          </div>
          <div>
            <h3 class="sdi__sum-title">Đạt / chưa đạt (bài thi)</h3>
            <p class="sdi__sum-sub">{{ submittedExamAttempts.length }} bài đã nộp</p>
          </div>
        </div>
        <div v-if="!submittedExamAttempts.length" class="sdi__sum-empty">
          <LucideIcon name="quiz" />
          <span>Chưa có bài thi để thống kê</span>
        </div>
        <div v-else class="sdi__sum-body">
          <div class="sdi__bar-col">
            <div
              class="sdi__split-bar"
              role="img"
              :aria-label="`Đạt ${passCount}, không đạt ${failCount}`"
              :style="splitBarStyle"
            />
            <div class="sdi__bar-pct" aria-hidden="true">
              <span v-if="passCount" class="sdi__pct sdi__pct--ok">{{ passPercent }}% đạt</span>
              <span v-if="failCount" class="sdi__pct sdi__pct--no">{{ failPercent }}% chưa đạt</span>
            </div>
          </div>
          <ul class="sdi__legend">
            <li class="sdi__legend-item">
              <span class="sdi__dot sdi__dot--ok" />
              <span>Đạt (&ge;5/10)</span>
              <strong>{{ passCount }}</strong>
            </li>
            <li class="sdi__legend-item">
              <span class="sdi__dot sdi__dot--no" />
              <span>Chưa đạt</span>
              <strong>{{ failCount }}</strong>
            </li>
          </ul>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import LucideIcon from '../../common/LucideIcon.vue'
import ScoreTrendCard from '../results/ScoreTrendCard.vue'

const props = defineProps({
  attempts: { type: Array, default: () => [] }
})

const SUBMITTED = new Set(['SUBMITTED', 'AUTO_SUBMITTED', 'COMPLETED'])

const submittedExamAttempts = computed(() =>
  props.attempts.filter((a) => {
    if (a.isPractice) return false
    return SUBMITTED.has(String(a.status || '').toUpperCase())
  })
)

const chartAttempts = computed(() => submittedExamAttempts.value)

const passCount = computed(() =>
  submittedExamAttempts.value.filter((a) => Number(a.score || 0) >= 50).length
)

const failCount = computed(() =>
  Math.max(0, submittedExamAttempts.value.length - passCount.value)
)

const totalSubmitted = computed(() => passCount.value + failCount.value)

const passPercent = computed(() => {
  const t = totalSubmitted.value
  if (!t) return 0
  return Math.round((passCount.value / t) * 100)
})

const failPercent = computed(() => {
  const t = totalSubmitted.value
  if (!t) return 0
  return Math.round((failCount.value / t) * 100)
})

const splitBarStyle = computed(() => {
  const p = passCount.value
  const f = failCount.value
  const t = p + f
  if (!t) return {}
  const pp = (p / t) * 100
  if (f === 0) {
    return { background: 'var(--ds-success)' }
  }
  if (p === 0) {
    return { background: 'var(--ds-danger)' }
  }
  return {
    background: `linear-gradient(to right, var(--ds-success) 0%, var(--ds-success) ${pp}%, var(--ds-danger) ${pp}%, var(--ds-danger) 100%)`
  }
})
</script>

<style scoped>
.sdi__grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
  align-items: stretch;
}

@media (max-width: 900px) {
  .sdi__grid {
    grid-template-columns: 1fr;
  }
}

.sdi__card {
  background: var(--ds-surface);
  border: 1.5px solid var(--ds-border);
  border-radius: var(--ds-radius-2xl);
  overflow: hidden;
  min-width: 0;
}

.dark .sdi__card {
  border-color: var(--ds-border-strong);
}

.sdi__card--trend :deep(.stc) {
  border: none;
  border-radius: 0;
  box-shadow: none;
}

.sdi__card--summary {
  padding: 1rem 1.125rem;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.sdi__sum-head {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.sdi__sum-icon {
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

.sdi__sum-title {
  font-family: var(--ds-font-display);
  font-size: 0.875rem;
  font-weight: 800;
  color: var(--ds-text);
  margin: 0;
}

.sdi__sum-sub {
  font-size: 0.7rem;
  color: var(--ds-text-muted);
  margin: 0.2rem 0 0;
  font-weight: 500;
}

.sdi__sum-empty {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 1.25rem;
  color: var(--ds-text-muted);
  font-size: 0.8rem;
  font-weight: 600;
  justify-content: center;
  flex: 1;
}

.sdi__sum-body {
  display: flex;
  align-items: stretch;
  gap: 1.25rem;
  flex-wrap: wrap;
}

.sdi__bar-col {
  flex: 1;
  min-width: 160px;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  justify-content: center;
}

.sdi__split-bar {
  width: 100%;
  height: 14px;
  border-radius: 999px;
  border: 1px solid var(--ds-border);
  box-sizing: border-box;
}

.dark .sdi__split-bar {
  border-color: var(--ds-border-strong);
}

.sdi__bar-pct {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem 0.75rem;
  font-size: 0.7rem;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}

.sdi__pct--ok {
  color: var(--ds-success);
}

.sdi__pct--no {
  color: var(--ds-danger);
}

.sdi__legend {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  flex: 0 0 auto;
  min-width: 140px;
}

.sdi__legend-item {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.78rem;
  font-weight: 600;
  color: var(--ds-text-muted);
}

.sdi__legend-item strong {
  margin-left: auto;
  color: var(--ds-text);
  font-variant-numeric: tabular-nums;
}

.sdi__dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}

.sdi__dot--ok {
  background: var(--ds-success);
}

.sdi__dot--no {
  background: var(--ds-danger);
}
</style>
