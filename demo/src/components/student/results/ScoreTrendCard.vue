<template>
  <div class="stc">
    <div class="stc__header">
      <div class="stc__header-icon">
        <LucideIcon name="show_chart" />
      </div>
      <div>
        <h3 class="stc__header-title">Xu hướng điểm theo thời gian</h3>
        <p class="stc__header-sub">
          {{ attempts.length }} bài gần nhất · đường nối theo thứ tự làm bài
        </p>
      </div>
    </div>

    <div v-if="attempts.length" class="stc__chart-wrap">
      <svg
        class="stc__svg"
        :viewBox="`0 0 ${vbW} ${vbH}`"
        xmlns="http://www.w3.org/2000/svg"
        role="img"
        :aria-label="ariaLabel"
      >
        <defs>
          <linearGradient id="stc-area-grad" x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stop-color="rgb(79, 70, 229)" stop-opacity="0.22" />
            <stop offset="100%" stop-color="rgb(79, 70, 229)" stop-opacity="0.02" />
          </linearGradient>
        </defs>

        <!-- Grid ngang + nhãn thang 0–10 -->
        <g class="stc__grid-lines" aria-hidden="true">
          <line
            v-for="(gy, i) in gridYs"
            :key="'g' + i"
            :x1="padL"
            :y1="gy"
            :x2="vbW - padR"
            :y2="gy"
          />
        </g>
        <g aria-hidden="true">
          <text
            v-for="row in yAxisLabels"
            :key="'y' + row.v"
            class="stc__y-tick"
            :x="4"
            :y="row.y + 4"
          >
            {{ row.v }}
          </text>
        </g>

        <!-- Đường trung bình -->
        <line
          v-if="avgY != null"
          class="stc__avg-line"
          :x1="padL"
          :y1="avgY"
          :x2="vbW - padR"
          :y2="avgY"
        />
        <text
          v-if="avgY != null"
          class="stc__avg-text"
          :x="vbW - padR - 4"
          :y="Math.max(avgY - 6, 14)"
          text-anchor="end"
        >
          TB {{ avgScore }}
        </text>

        <!-- Vùng tô dưới đường -->
        <path v-if="areaPath" :d="areaPath" fill="url(#stc-area-grad)" class="stc__area" />

        <!-- Đường xu hướng -->
        <path
          v-if="linePath"
          :d="linePath"
          fill="none"
          class="stc__line"
          :class="{ 'stc__line--single': points.length === 1 }"
        />

        <!-- Điểm + nhãn điểm -->
        <g v-for="(p, idx) in points" :key="p.id ?? idx">
          <circle
            :cx="p.x"
            :cy="p.y"
            :r="hoverIdx === idx ? 7 : 5.5"
            :class="['stc__dot', dotClass(p.score10)]"
            @mouseenter="hoverIdx = idx"
            @mouseleave="hoverIdx = null"
          />
          <text
            :x="p.x"
            :y="p.y - 12"
            text-anchor="middle"
            class="stc__score-tag"
          >
            {{ p.label }}
          </text>
        </g>
      </svg>

      <!-- Trục X: thứ tự bài -->
      <div class="stc__x-labels">
        <span
          v-for="(p, idx) in points"
          :key="'xl' + (p.id ?? idx)"
          class="stc__x-label"
        >
          #{{ idx + 1 }}
        </span>
      </div>
    </div>

    <div v-else class="stc__empty">
      <LucideIcon name="timeline" />
      <span>Chưa có dữ liệu để vẽ biểu đồ</span>
    </div>

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
import { computed, ref } from 'vue'
import LucideIcon from '../../common/LucideIcon.vue'

const props = defineProps({
  attempts: { type: Array, default: () => [] }
})

const LIMIT = 8
const hoverIdx = ref(null)

const vbW = 420
const vbH = 168
const padL = 36
const padR = 12
const padT = 28
const padB = 8

const barAttempts = computed(() => {
  return [...props.attempts]
    .sort((a, b) => {
      const at = new Date(a.submittedAt || a.startedAt || 0).getTime()
      const bt = new Date(b.submittedAt || b.startedAt || 0).getTime()
      return at - bt
    })
    .slice(-LIMIT)
})

const points = computed(() => {
  const list = barAttempts.value
  const n = list.length
  if (!n) return []
  const innerW = vbW - padL - padR
  const innerH = vbH - padT - padB
  return list.map((attempt, i) => {
    const raw = Number(attempt.score || 0)
    const score10 = Math.min(10, Math.max(0, raw / 10))
    const x =
      n === 1
        ? padL + innerW / 2
        : padL + (innerW * i) / (n - 1)
    const y = padT + innerH * (1 - score10 / 10)
    return {
      id: attempt.id,
      x,
      y,
      score10,
      label: score10 > 0 ? score10.toFixed(1) : '0'
    }
  })
})

const gridYs = computed(() => {
  const innerH = vbH - padT - padB
  return [0, 5, 10].map((mark) => padT + innerH * (1 - mark / 10))
})

const yAxisLabels = computed(() => {
  const innerH = vbH - padT - padB
  return [10, 5, 0].map((v) => ({
    v,
    y: padT + innerH * (1 - v / 10)
  }))
})

const avgScore = computed(() => {
  const scores = props.attempts.map(a => Number(a.score || 0)).filter(s => s > 0)
  if (!scores.length) return 0
  return (scores.reduce((a, b) => a + b, 0) / scores.length / 10).toFixed(1)
})

const avgY = computed(() => {
  const v = Number(avgScore.value)
  if (!v || Number.isNaN(v)) return null
  const innerH = vbH - padT - padB
  return padT + innerH * (1 - Math.min(10, v) / 10)
})

const linePath = computed(() => {
  const pts = points.value
  if (!pts.length) return ''
  if (pts.length === 1) return ''
  return pts.map((p, i) => `${i === 0 ? 'M' : 'L'} ${p.x.toFixed(1)} ${p.y.toFixed(1)}`).join(' ')
})

const areaPath = computed(() => {
  const pts = points.value
  if (!pts.length) return ''
  const bottom = vbH - padB
  if (pts.length === 1) {
    const p = pts[0]
    const w = 24
    return `M ${p.x - w} ${bottom} L ${p.x - w} ${p.y} L ${p.x + w} ${p.y} L ${p.x + w} ${bottom} Z`
  }
  const top = pts
    .map((p, i) => `${i === 0 ? 'M' : 'L'} ${p.x.toFixed(1)} ${p.y.toFixed(1)}`)
    .join(' ')
  const last = pts[pts.length - 1]
  const first = pts[0]
  return `${top} L ${last.x.toFixed(1)} ${bottom} L ${first.x.toFixed(1)} ${bottom} Z`
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

const dotClass = (s) => {
  if (s >= 8) return 'stc__dot--high'
  if (s >= 5) return 'stc__dot--mid'
  return 'stc__dot--low'
}

const ariaLabel = computed(() => {
  const vals = points.value.map((p) => p.label).join(', ')
  return `Biểu đồ điểm theo thời gian: ${vals || 'không có điểm'} trên thang 10`
})
</script>

<style scoped>
.stc {
  background: var(--ds-surface);
  border: 1.5px solid var(--ds-border);
  border-radius: var(--ds-radius-2xl);
  padding: 1.125rem 1.25rem;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  min-width: 0;
  width: 100%;
  max-width: 100%;
  box-sizing: border-box;
}

.dark .stc {
  border-color: var(--ds-border-strong);
}

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
}

.stc__header-title {
  font-size: 0.875rem;
  font-weight: 800;
  color: var(--ds-text);
  margin: 0;
}

.dark .stc__header-title {
  color: #f1f5f9;
}

.stc__header-sub {
  font-size: 0.7rem;
  color: var(--ds-text-muted);
  margin: 0.15rem 0 0;
  font-weight: 500;
}

.stc__chart-wrap {
  width: 100%;
  min-width: 0;
}

.stc__svg {
  display: block;
  width: 100%;
  height: auto;
  max-height: 200px;
  shape-rendering: geometricPrecision;
}

.stc__grid-lines line {
  stroke: var(--ds-gray-200);
  stroke-width: 1;
  stroke-dasharray: 4 4;
}

.dark .stc__grid-lines line {
  stroke: var(--ds-gray-700);
}

.stc__y-tick {
  fill: var(--ds-text-muted);
  font-size: 10px;
  font-weight: 700;
}

.dark .stc__y-tick {
  fill: #94a3b8;
}

.stc__avg-line {
  stroke: var(--ds-primary);
  stroke-width: 2;
  stroke-dasharray: 6 5;
  opacity: 0.85;
}

.stc__avg-text {
  fill: var(--ds-primary);
  font-size: 11px;
  font-weight: 800;
}

.dark .stc__avg-text {
  fill: #a5b4fc;
}

.stc__area {
  pointer-events: none;
}

.stc__line {
  stroke: #4f46e5;
  stroke-width: 2.75;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.dark .stc__line {
  stroke: #818cf8;
}

.stc__line--single {
  stroke: none;
}

.stc__dot {
  stroke: var(--ds-surface);
  stroke-width: 2;
  cursor: default;
  transition: r 0.12s ease;
}

.stc__dot--high {
  fill: #059669;
}

.stc__dot--mid {
  fill: #d97706;
}

.stc__dot--low {
  fill: #dc2626;
}

.stc__score-tag {
  fill: var(--ds-text);
  font-size: 11px;
  font-weight: 800;
  font-family: var(--ds-font-display), system-ui, sans-serif;
}

.dark .stc__score-tag {
  fill: #f1f5f9;
}

.stc__x-labels {
  display: flex;
  justify-content: space-between;
  padding: 0.25rem 0.5rem 0;
  margin-top: -0.125rem;
}

.stc__x-label {
  font-size: 0.65rem;
  font-weight: 700;
  color: var(--ds-text-muted);
  flex: 1;
  text-align: center;
}

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

.stc__footer {
  display: flex;
  flex-wrap: wrap;
  gap: 1rem;
  padding-top: 0.625rem;
  border-top: 1px solid var(--ds-border);
}

.dark .stc__footer {
  border-top-color: var(--ds-border-strong);
}

.stc__footer-stat {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  font-size: 0.72rem;
  color: var(--ds-text-muted);
  font-weight: 500;
}

.stc__footer-stat strong {
  color: var(--ds-text);
  font-weight: 800;
}

.dark .stc__footer-stat strong {
  color: #f1f5f9;
}

@media (prefers-reduced-motion: reduce) {
  .stc__dot {
    transition: none;
  }
}
</style>
