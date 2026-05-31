<template>
  <div
    class="sc-card"
    :class="[`sc-card--${riskBand}`, { 'sc-card--selected': selected, 'sc-card--paused': isPaused }]"
    @click="$emit('view-detail')"
  >
    <!-- Risk accent top strip -->
    <div class="sc-card__accent" />

    <div class="sc-card__body">

      <!-- Header: Avatar + Identity + Checkbox -->
      <div class="sc-card__header">
        <div class="sc-card__avatar-wrap">
          <div class="sc-card__avatar" :style="{ background: avatarBg }">
            <span class="sc-card__initials" :style="{ color: avatarColor }">{{ initials }}</span>
            <div class="sc-card__status-ring" :style="{ borderColor: statusColor }" />
          </div>
        </div>

        <div class="sc-card__identity">
          <h3 class="sc-card__name">{{ studentName }}</h3>
          <span class="sc-card__status" :style="{ color: statusColor }">
            <LucideIcon :name="statusIcon" :size="11" />
            {{ statusLabel }}
          </span>
        </div>

        <label class="sc-card__check-wrap" @click.stop>
          <input
            type="checkbox"
            class="sc-card__check"
            :checked="selected"
            @change="$emit('toggle-select')"
          />
          <div class="sc-card__check-visual" :class="{ 'sc-card__check-visual--checked': selected }">
            <LucideIcon v-if="selected" name="check" :size="10" />
          </div>
        </label>
      </div>

      <!-- Risk Gauge Row -->
      <div class="sc-card__risk-row">
        <div class="sc-card__risk-bar-wrap">
          <div class="sc-card__risk-bar">
            <div
              class="sc-card__risk-fill"
              :class="{ 'sc-card__risk-fill--pulse': hasViolationPulse }"
              :style="{ width: riskScore + '%', background: riskColor }"
            />
          </div>
        </div>
        <div class="sc-card__risk-meta">
          <span class="sc-card__risk-score" :style="{ color: riskColor }">{{ riskScore }}</span>
          <span v-if="riskDelta !== null" class="sc-card__risk-delta" :class="riskDeltaClass">
            {{ riskDelta > 0 ? '+' : '' }}{{ riskDelta }}
          </span>
          <span class="sc-card__risk-band" :style="{ color: riskColor }">{{ riskBandLabel }}</span>
        </div>
      </div>

      <!-- Violation Tags -->
      <div v-if="violationCount > 0" class="sc-card__violations">
        <span
          v-for="v in topViolations"
          :key="v.type"
          class="sc-card__viol-tag"
          :class="v.tagClass"
        >
          <LucideIcon :name="v.icon" :size="9" />
          {{ v.label }}
        </span>
        <span v-if="violationCount > topViolations.length" class="sc-card__viol-more">
          +{{ violationCount - topViolations.length }}
        </span>
      </div>
      <div v-else class="sc-card__violations">
        <span class="sc-card__viol-tag sc-card__viol-tag--clean">
          <LucideIcon name="check-circle" :size="9" />
          Không có vi phạm
        </span>
      </div>

      <!-- Action Buttons -->
      <div class="sc-card__actions" @click.stop>
        <button
          class="sc-action sc-action--warn"
          @click="$emit('warn')"
          title="Gửi cảnh báo"
        >
          <LucideIcon name="alert-triangle" :size="13" />
        </button>

        <button
          v-if="isPaused"
          class="sc-action sc-action--resume"
          @click="$emit('resume')"
          title="Cho phép tiếp tục"
        >
          <LucideIcon name="play" :size="13" />
        </button>
        <button
          v-else
          class="sc-action sc-action--pause"
          :disabled="isTerminal"
          @click="$emit('pause')"
          :title="isTerminal ? 'Đã kết thúc' : 'Tạm dừng'"
        >
          <LucideIcon name="pause" :size="13" />
        </button>

        <button
          class="sc-action sc-action--detail"
          @click="$emit('view-detail')"
          title="Xem chi tiết"
        >
          <LucideIcon name="eye" :size="13" />
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import {
  getAttemptStatusMeta,
  getInitialsFromName,
  getRiskBandVisual,
  isAttemptPaused,
  isAttemptTerminal,
  resolveRiskBand
} from '../../utils/proctorStatusMeta'

const VIOLATION_MAP = {
  TAB_SWITCH: { label: 'Tab', icon: 'layers', tagClass: 'sc-card__viol-tag--warn' },
  COPY_PASTE: { label: 'Copy', icon: 'copy', tagClass: 'sc-card__viol-tag--danger' },
  DEVTOOLS_OPEN: { label: 'DevTools', icon: 'code', tagClass: 'sc-card__viol-tag--danger' },
  EXIT_FULLSCREEN: { label: 'Fullscreen', icon: 'minimize', tagClass: 'sc-card__viol-tag--warn' },
  DUPLICATE_IP: { label: 'IP trùng', icon: 'globe', tagClass: 'sc-card__viol-tag--danger' },
  MULTI_MONITOR: { label: 'Multi', icon: 'monitor', tagClass: 'sc-card__viol-tag--danger' }
}

const BAND_LABELS = { clean: 'Sạch', suspicious: 'Nghi ngờ', high: 'Cao', critical: 'Nguy hiểm' }

const props = defineProps({
  student: { type: Object, required: true },
  selected: { type: Boolean, default: false }
})
defineEmits(['toggle-select', 'view-detail', 'warn', 'pause', 'resume'])

const studentName = computed(() =>
  props.student.student || props.student.name || props.student.userName || '—'
)
const initials = computed(() => getInitialsFromName(studentName.value))

const riskScore = computed(() => Math.round(Number(props.student.riskScore || 0)))
const riskBand = computed(() => resolveRiskBand(riskScore.value))
const riskBandLabel = computed(() => BAND_LABELS[riskBand.value] || '')
const riskVisual = computed(() => getRiskBandVisual(riskScore.value))
const riskColor = computed(() => riskVisual.value.color)
const avatarBg = computed(() => riskVisual.value.bg)
const avatarColor = computed(() => riskColor.value)

const statusMeta = computed(() => getAttemptStatusMeta(props.student.status))
const statusLabel = computed(() => statusMeta.value.label)
const statusIcon = computed(() => statusMeta.value.icon)
const statusColor = computed(() => statusMeta.value.color)
const isPaused = computed(() => isAttemptPaused(props.student.status))
const isTerminal = computed(() => isAttemptTerminal(props.student.status))

const violationCount = computed(() => (props.student.reasons || []).length)
const topViolations = computed(() =>
  (props.student.reasons || []).slice(0, 2).map(r => ({
    type: r,
    ...(VIOLATION_MAP[r] || { label: r, icon: 'alert-circle', tagClass: 'sc-card__viol-tag--neutral' })
  }))
)

// Risk delta from previous score
const riskDelta = computed(() => {
  const delta = props.student._riskDelta
  return delta != null ? Number(delta) : null
})
const riskDeltaClass = computed(() => {
  const d = riskDelta.value
  if (d === null) return ''
  if (d > 0) return 'sc-card__risk-delta--up'
  if (d < 0) return 'sc-card__risk-delta--down'
  return ''
})

// Pulse animation when a new fraud signal arrived
const hasViolationPulse = computed(() =>
  props.student._recentSignal === true
)
</script>

<style scoped>
.sc-card {
  position: relative;
  background: var(--ds-surface);
  border: 1px solid var(--ds-border);
  border-radius: var(--ds-radius-xl);
  overflow: hidden;
  cursor: pointer;
  transition: border-color 0.15s, box-shadow 0.15s, transform 0.15s;
  box-shadow: var(--ds-shadow-xs);
}
.sc-card:hover {
  border-color: var(--ds-primary-border);
  box-shadow: var(--ds-shadow-md);
  transform: translateY(-2px);
}
.sc-card--selected {
  border-color: var(--ds-primary);
  box-shadow: var(--ds-shadow-focus);
}
.sc-card--paused { opacity: 0.85; }

/* Top accent strip */
.sc-card__accent {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  border-radius: var(--ds-radius-xl) var(--ds-radius-xl) 0 0;
}
.sc-card--clean .sc-card__accent { background: var(--ds-risk-clean); }
.sc-card--suspicious .sc-card__accent { background: var(--ds-risk-moderate); }
.sc-card--high .sc-card__accent { background: var(--ds-risk-high); }
.sc-card--critical .sc-card__accent { background: var(--ds-risk-critical); }

.sc-card__body {
  padding: 0.875rem 0.875rem 0.75rem;
}

/* Header */
.sc-card__header {
  display: flex;
  align-items: center;
  gap: 0.625rem;
  margin-bottom: 0.75rem;
}
.sc-card__avatar-wrap { flex-shrink: 0; }
.sc-card__avatar {
  position: relative;
  width: 42px;
  height: 42px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}
.sc-card__initials { font-size: 0.875rem; font-weight: 900; }
.sc-card__status-ring {
  position: absolute;
  inset: -2px;
  border-radius: 50%;
  border: 2px solid;
}
.sc-card__identity { flex: 1; min-width: 0; }
.sc-card__name {
  font-size: 0.825rem;
  font-weight: 800;
  color: var(--ds-text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin: 0 0 0.2rem;
}
.sc-card__status {
  display: inline-flex;
  align-items: center;
  gap: 0.3rem;
  font-size: 0.7rem;
  font-weight: 600;
}

/* Custom checkbox */
.sc-card__check-wrap {
  flex-shrink: 0;
  cursor: pointer;
  position: relative;
}
.sc-card__check {
  position: absolute;
  opacity: 0;
  width: 18px;
  height: 18px;
  cursor: pointer;
}
.sc-card__check-visual {
  width: 18px;
  height: 18px;
  border-radius: 5px;
  border: 2px solid var(--ds-border);
  background: var(--ds-surface);
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.15s;
  color: white;
}
.sc-card__check-visual--checked {
  background: var(--ds-primary);
  border-color: var(--ds-primary);
}

/* Risk row */
.sc-card__risk-row {
  display: flex;
  align-items: center;
  gap: 0.625rem;
  margin-bottom: 0.625rem;
}
.sc-card__risk-bar-wrap { flex: 1; }
.sc-card__risk-bar {
  height: 6px;
  border-radius: 9999px;
  background: var(--ds-gray-100);
  overflow: hidden;
}
.sc-card__risk-fill {
  height: 100%;
  border-radius: 9999px;
  transition: width 0.5s ease;
  will-change: width;
}
.sc-card__risk-meta {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 0;
}
.sc-card__risk-score {
  font-size: 1rem;
  font-weight: 900;
  line-height: 1;
  font-variant-numeric: tabular-nums;
}
.sc-card__risk-band {
  font-size: 0.6rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.04em;
}
.sc-card__risk-delta {
  font-size: 0.625rem;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  line-height: 1;
}
.sc-card__risk-delta--up { color: var(--ds-danger); }
.sc-card__risk-delta--down { color: var(--ds-success); }

/* Violation pulse animation */
@keyframes sc-violation-pulse {
  0% { opacity: 1; }
  50% { opacity: 0.4; }
  100% { opacity: 1; }
}
.sc-card__risk-fill--pulse {
  animation: sc-violation-pulse 0.6s ease-in-out 3;
}

/* Violations */
.sc-card__violations {
  display: flex;
  gap: 0.3rem;
  flex-wrap: wrap;
  margin-bottom: 0.5rem;
  min-height: 20px;
}
.sc-card__violations:has(.sc-card__viol-tag--clean) {
  display: none;
}
.sc-card__viol-tag {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  padding: 0.2rem 0.5rem;
  border-radius: 9999px;
  font-size: 0.65rem;
  font-weight: 700;
  border: 1px solid;
}
.sc-card__viol-tag--danger { color: var(--ds-danger); background: var(--ds-danger-bg); border-color: var(--ds-danger-soft); }
.sc-card__viol-tag--warn { color: var(--ds-warning); background: var(--ds-warning-bg); border-color: var(--ds-warning-soft); }
.sc-card__viol-tag--neutral { color: var(--ds-text-secondary); background: var(--ds-surface-muted); border-color: var(--ds-border); }
.sc-card__viol-tag--clean { color: var(--ds-success); background: var(--ds-success-bg); border-color: var(--ds-success-soft); }
.sc-card__viol-more {
  display: inline-flex;
  align-items: center;
  padding: 0.2rem 0.5rem;
  border-radius: 9999px;
  font-size: 0.65rem;
  font-weight: 700;
  color: var(--ds-text-muted);
  background: var(--ds-surface-muted);
  border: 1px solid var(--ds-border);
}

/* Actions */
.sc-card__actions {
  display: flex;
  gap: 0.375rem;
  padding-top: 0.5rem;
  border-top: 1px solid transparent;
  opacity: 0.45;
  transition: opacity 0.15s, border-color 0.15s;
}
.sc-card:hover .sc-card__actions,
.sc-card:focus-within .sc-card__actions,
.sc-card--selected .sc-card__actions {
  opacity: 1;
  border-top-color: var(--ds-border);
}
.sc-action {
  flex: 1;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--ds-radius-md);
  border: 1px solid var(--ds-border);
  background: transparent;
  color: var(--ds-text-secondary);
  cursor: pointer;
  transition: all 0.15s;
}
.sc-action:disabled { opacity: 0.4; cursor: not-allowed; }
.sc-action--warn { color: var(--ds-warning); }
.sc-action--warn:hover:not(:disabled) { background: var(--ds-warning-bg); border-color: var(--ds-warning-soft); }
.sc-action--pause { color: var(--ds-primary); }
.sc-action--pause:hover:not(:disabled) { background: var(--ds-primary-soft); border-color: var(--ds-primary-border); }
.sc-action--resume { color: var(--ds-success); background: var(--ds-success-bg); border-color: var(--ds-success-soft); }
.sc-action--resume:hover:not(:disabled) { background: var(--ds-success); color: #fff; border-color: var(--ds-success); }
.sc-action--detail { color: var(--ds-text-secondary); }
.sc-action--detail:hover:not(:disabled) { background: var(--ds-surface-muted); border-color: var(--ds-primary-border); color: var(--ds-text); }
</style>
