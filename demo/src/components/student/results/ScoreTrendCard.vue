<template>
  <div class="stc">
    <!-- Header -->
    <div class="stc__header">
      <div class="stc__header-icon">
        <LucideIcon name="show_chart" />
      </div>
      <div>
        <h3 class="stc__header-title">Xu hướng điểm</h3>
        <p class="stc__header-sub">{{ attempts.length }} bài thi gần nhất</p>
      </div>
    </div>

    <!-- Bars -->
    <div v-if="attempts.length" class="stc__bars">
      <div
        v-for="(attempt, idx) in barAttempts"
        :key="attempt.id"
        class="stc__bar-wrap group"
        :title="`${attempt.examTitle || 'Bài thi'}: ${scoreOf(attempt)} / 10`"
      >
        <div class="stc__bar-track">
          <div
            class="stc__bar-fill"
            :class="barClass(scoreOf(attempt))"
            :style="{ 
              height: `${barHeight(scoreOf(attempt))}%`,
              animationDelay: `${idx * 0.08}s`
            }"
          >
            <div class="stc__bar-tooltip">
              <div class="stc__bar-tooltip-inner">
                <span class="stc__bar-tooltip-val">{{ scoreOf(attempt) }}</span>
                <span class="stc__bar-tooltip-max">/ 10</span>
              </div>
            </div>
          </div>
        </div>
        <span class="stc__bar-label">#{{ idx + 1 }}</span>
      </div>

      <!-- Avg line marker -->
      <div v-if="avgScore > 0" class="stc__avg-marker">
        <span class="stc__avg-label">
          <LucideIcon name="horizontal_rule" size="10" />
          TB: {{ avgScore }}
        </span>
      </div>
    </div>

    <!-- Empty -->
    <div v-else class="stc__empty">
      <LucideIcon name="timeline" />
      <span>Chưa có dữ liệu để vẽ biểu đồ</span>
    </div>

    <!-- Stats footer -->
    <div v-if="attempts.length" class="stc__footer">
      <div class="stc__footer-stat">
        <LucideIcon name="trending_up" />
        <span>Cao nhất: <strong>{{ best }}</strong></span>
      </div>
      <div class="stc__footer-stat">
        <LucideIcon name="trending_down" />
        <span>Thấp nhất: <strong>{{ worst }}</strong></span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import LucideIcon from '../common/LucideIcon.vue'

const props = defineProps({
  attempts: { type: Array, default: () => [] }
})

const LIMIT = 8

const barAttempts = computed(() => {
  return [...props.attempts]
    .sort((a, b) => {
      const at = new Date(a.submittedAt || a.startedAt || 0).getTime()
      const bt = new Date(b.submittedAt || b.startedAt || 0).getTime()
      return at - bt
    })
    .slice(-LIMIT)
})

const scoreOf = (attempt) => {
  const s = Number(attempt.score || 0)
  return s > 0 ? (s / 10).toFixed(1) : 0
}

const avgScore = computed(() => {
  const scores = props.attempts.map(a => Number(a.score || 0)).filter(s => s > 0)
  if (!scores.length) return 0
  return (scores.reduce((a, b) => a + b, 0) / scores.length / 10).toFixed(1)
})

const best = computed(() => {
  const scores = props.attempts.map(a => Number(a.score || 0)).filter(s => s > 0)
  if (!scores.length) return '-'
  return (Math.max(...scores) / 10).toFixed(1)
})

const worst = computed(() => {
  const scores = props.attempts.map(a => Number(a.score || 0)).filter(s => s > 0)
  if (!scores.length) return '-'
  return (Math.min(...scores) / 10).toFixed(1)
})

const barHeight = (score) => Math.max(8, Number(score) * 10)

const barClass = (score) => {
  const s = Number(score)
  if (s >= 8) return 'stc__bar-fill--high'
  if (s >= 5) return 'stc__bar-fill--mid'
  return 'stc__bar-fill--low'
}
</script>


<style scoped>
.stc {
  background: var(--ds-surface);
  border: 1.5px solid var(--ds-border);
  border-radius: var(--ds-radius-2xl);
  padding: 1.125rem 1.25rem;
  display: flex;
  flex-direction: column;
  gap: 0.875rem;
}

.dark .stc { border-color: var(--ds-border-strong); }

/* Header */
.stc__header {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.stc__header-icon {
  width: 40px;
  height: 40px;
  border-radius: var(--ds-radius-lg);
  background: var(--ds-primary-soft);
  color: var(--ds-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: transform 0.2s ease;
}

.stc__header:hover .stc__header-icon {
  transform: scale(1.1);
}

.stc__header-title {
  font-size: 0.875rem;
  font-weight: 800;
  color: var(--ds-text);
  margin: 0;
}

.dark .stc__header-title { color: #f1f5f9; }

.stc__header-sub {
  font-size: 0.7rem;
  color: var(--ds-text-muted);
  margin: 0.15rem 0 0;
  font-weight: 500;
}

/* Bars */
.stc__bars {
  position: relative;
  display: flex;
  align-items: flex-end;
  gap: 0.375rem;
  height: 100px;
  padding-bottom: 1.5rem;
}

.stc__avg-marker {
  position: absolute;
  left: 0;
  right: 0;
  border-top: 2px dashed rgba(79, 70, 229, 0.4);
  pointer-events: none;
  animation: avgPulse 2s ease-in-out infinite;
}

@keyframes avgPulse {
  0%, 100% { border-color: rgba(79, 70, 229, 0.4); }
  50% { border-color: rgba(79, 70, 229, 0.7); }
}

.stc__avg-label {
  position: absolute;
  right: 0;
  top: -18px;
  font-size: 0.6rem;
  font-weight: 800;
  color: var(--ds-primary);
  background: var(--ds-surface);
  padding: 2px 6px;
  border-radius: var(--ds-radius-md);
  display: flex;
  align-items: center;
  gap: 3px;
  box-shadow: 0 2px 8px rgba(79, 70, 229, 0.2);
}

.stc__bar-wrap {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.25rem;
  height: 100%;
  position: relative;
}

.stc__bar-track {
  flex: 1;
  width: 100%;
  display: flex;
  align-items: flex-end;
  border-radius: var(--ds-radius-md) var(--ds-radius-md) 0 0;
  background: var(--ds-gray-100);
  overflow: visible;
  transition: all 0.2s ease;
}

.dark .stc__bar-track { background: var(--ds-gray-800); }

.stc__bar-fill {
  width: 100%;
  border-radius: var(--ds-radius-md) var(--ds-radius-md) 0 0;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  min-height: 4px;
  animation: barGrow 0.6s ease-out forwards;
  transform-origin: bottom;
  opacity: 0;
  transform: scaleY(0);
}

@keyframes barGrow {
  from {
    opacity: 0;
    transform: scaleY(0);
  }
  to {
    opacity: 1;
    transform: scaleY(1);
  }
}

.stc__bar-wrap:hover .stc__bar-fill {
  filter: brightness(1.15);
}

.stc__bar-fill--high { 
  background: linear-gradient(180deg, #10b981 0%, rgba(16, 185, 129, 0.6) 100%);
  box-shadow: 0 0 12px rgba(16, 185, 129, 0.3);
}
.stc__bar-fill--mid { 
  background: linear-gradient(180deg, #f59e0b 0%, rgba(245, 158, 11, 0.6) 100%);
  box-shadow: 0 0 12px rgba(245, 158, 11, 0.3);
}
.stc__bar-fill--low { 
  background: linear-gradient(180deg, #ef4444 0%, rgba(239, 68, 68, 0.6) 100%);
  box-shadow: 0 0 12px rgba(239, 68, 68, 0.3);
}

.stc__bar-wrap:hover .stc__bar-fill--high { box-shadow: 0 0 20px rgba(16, 185, 129, 0.5); }
.stc__bar-wrap:hover .stc__bar-fill--mid { box-shadow: 0 0 20px rgba(245, 158, 11, 0.5); }
.stc__bar-wrap:hover .stc__bar-fill--low { box-shadow: 0 0 20px rgba(239, 68, 68, 0.5); }

/* Bar tooltip */
.stc__bar-tooltip {
  position: absolute;
  top: -36px;
  left: 50%;
  transform: translateX(-50%);
  opacity: 0;
  pointer-events: none;
  transition: all 0.2s ease;
}

.stc__bar-wrap:hover .stc__bar-tooltip {
  opacity: 1;
  transform: translateX(-50%) translateY(-4px);
}

.stc__bar-tooltip-inner {
  background: linear-gradient(135deg, var(--ds-text) 0%, #4b5563 100%);
  color: white;
  padding: 4px 10px;
  border-radius: var(--ds-radius-lg);
  font-size: 0.7rem;
  font-weight: 700;
  white-space: nowrap;
  display: flex;
  align-items: center;
  gap: 3px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
}

.stc__bar-tooltip-val { font-size: 0.8rem; }
.stc__bar-tooltip-max { opacity: 0.7; }

.stc__bar-label {
  font-size: 0.55rem;
  font-weight: 700;
  color: var(--ds-text-muted);
  position: absolute;
  bottom: 0;
}

/* Empty */
.stc__empty {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  padding: 1.5rem;
  color: var(--ds-text-muted);
  font-size: 0.8rem;
  font-weight: 500;
}

/* Footer */
.stc__footer {
  display: flex;
  gap: 1rem;
  padding-top: 0.625rem;
  border-top: 1px solid var(--ds-border);
}

.dark .stc__footer { border-top-color: var(--ds-border-strong); }

.stc__footer-stat {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  font-size: 0.72rem;
  color: var(--ds-text-muted);
  font-weight: 500;
  transition: color 0.2s ease;
}

.stc__footer-stat:hover {
  color: var(--ds-primary);
}

.stc__footer-stat strong { color: var(--ds-text); font-weight: 800; }
.dark .stc__footer-stat strong { color: #f1f5f9; }
</style>
