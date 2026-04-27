<template>
  <div
    class="sr-row"
    :class="[`sr-row--${riskBand}`, { 'sr-row--selected': selected, 'sr-row--paused': isPaused }]"
    @click="$emit('view-detail')"
  >
    <!-- Left accent -->
    <div class="sr-row__accent" />

    <!-- Checkbox -->
    <label class="sr-row__check" @click.stop>
      <input
        type="checkbox"
        class="sr-row__checkbox"
        :checked="selected"
        @change="$emit('toggle-select')"
      />
      <div class="sr-row__check-visual" :class="{ 'sr-row__check-visual--checked': selected }">
        <LucideIcon v-if="selected" name="check" :size="10" />
      </div>
    </label>

    <!-- Avatar + Name -->
    <div class="sr-row__identity">
      <div class="sr-row__avatar-wrap">
        <div class="sr-row__avatar" :style="{ background: avatarBg }">
          <span class="sr-row__initials" :style="{ color: avatarColor }">{{ initials }}</span>
          <div class="sr-row__status-ring" :style="{ borderColor: statusColor }" />
        </div>
      </div>
      <div class="sr-row__name-group">
        <h4 class="sr-row__name">{{ studentName }}</h4>
        <span class="sr-row__status" :style="{ color: statusColor }">
          <LucideIcon :name="statusIcon" :size="10" />
          {{ statusLabel }}
        </span>
      </div>
    </div>

    <!-- Violation Count -->
    <div class="sr-row__cell sr-row__cell--violations">
      <span
        v-if="violationCount > 0"
        class="sr-row__viol-badge"
        :class="violationCount > 3 ? 'sr-row__viol-badge--danger' : 'sr-row__viol-badge--warn'"
      >
        {{ violationCount }}
      </span>
      <span v-else class="sr-row__muted">
        <LucideIcon name="check-circle" :size="12" class="sr-row__clean-icon" />
      </span>
    </div>

    <!-- Risk Bar -->
    <div class="sr-row__cell sr-row__cell--risk">
      <div class="sr-row__risk-bar">
        <div
          class="sr-row__risk-fill"
          :style="{ width: riskScore + '%', background: riskColor }"
        />
      </div>
      <span class="sr-row__risk-score" :style="{ color: riskColor }">{{ riskScore }}</span>
    </div>

    <!-- Violation Tags -->
    <div class="sr-row__cell sr-row__cell--tags">
      <span
        v-for="v in topViolations"
        :key="v.type"
        class="sr-row__tag"
        :class="v.tagClass"
        :title="v.label"
      >
        <LucideIcon :name="v.icon" :size="9" />
      </span>
    </div>

    <!-- Actions -->
    <div class="sr-row__cell sr-row__cell--actions" @click.stop>
      <button class="sr-action sr-action--warn" @click="$emit('warn')" title="Cảnh báo">
        <LucideIcon name="alert-triangle" :size="13" />
      </button>
      <button
        v-if="isPaused"
        class="sr-action sr-action--resume"
        @click="$emit('resume')"
        title="Cho phép tiếp tục"
      >
        <LucideIcon name="play" :size="13" />
      </button>
      <button
        v-else
        class="sr-action sr-action--pause"
        :disabled="isTerminal"
        @click="$emit('pause')"
        :title="isTerminal ? 'Đã kết thúc' : 'Tạm dừng'"
      >
        <LucideIcon name="pause" :size="13" />
      </button>
      <button class="sr-action sr-action--detail" @click="$emit('view-detail')" title="Xem chi tiết">
        <LucideIcon name="eye" :size="13" />
      </button>
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
  TAB_SWITCH: { icon: 'layers', tagClass: 'sr-row__tag--warn' },
  COPY_PASTE: { icon: 'copy', tagClass: 'sr-row__tag--danger' },
  DEVTOOLS_OPEN: { icon: 'code', tagClass: 'sr-row__tag--danger' },
  EXIT_FULLSCREEN: { icon: 'minimize', tagClass: 'sr-row__tag--warn' },
  DUPLICATE_IP: { icon: 'globe', tagClass: 'sr-row__tag--danger' },
  MULTI_MONITOR: { icon: 'monitor', tagClass: 'sr-row__tag--danger' }
}

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
  (props.student.reasons || []).slice(0, 3).map(r => ({
    type: r,
    ...(VIOLATION_MAP[r] || { icon: 'alert-circle', tagClass: 'sr-row__tag--neutral' })
  }))
)
</script>

<style scoped>
.sr-row {
  display: flex;
  align-items: center;
  gap: 0.875rem;
  padding: 0.7rem 1rem;
  background: var(--ds-surface);
  border: 1px solid var(--ds-border);
  border-radius: var(--ds-radius-lg);
  cursor: pointer;
  transition: all 0.15s;
  position: relative;
  overflow: hidden;
}
.sr-row:hover {
  background: var(--ds-surface-muted);
  border-color: var(--ds-primary-border);
  box-shadow: var(--ds-shadow-sm);
  transform: translateY(-1px);
}
.sr-row--selected {
  border-color: var(--ds-primary);
  box-shadow: var(--ds-shadow-focus);
}
.sr-row--paused { opacity: 0.85; }

/* Left accent */
.sr-row__accent {
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 3px;
}
.sr-row--clean .sr-row__accent { background: var(--ds-risk-clean); }
.sr-row--suspicious .sr-row__accent { background: var(--ds-risk-moderate); }
.sr-row--high .sr-row__accent { background: var(--ds-risk-high); }
.sr-row--critical .sr-row__accent { background: var(--ds-risk-critical); }

/* Checkbox */
.sr-row__check {
  flex-shrink: 0;
  cursor: pointer;
  position: relative;
}
.sr-row__checkbox {
  position: absolute;
  opacity: 0;
  width: 18px;
  height: 18px;
  cursor: pointer;
}
.sr-row__check-visual {
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
.sr-row__check-visual--checked {
  background: var(--ds-primary);
  border-color: var(--ds-primary);
}

/* Identity */
.sr-row__identity {
  display: flex;
  align-items: center;
  gap: 0.625rem;
  min-width: 160px;
  max-width: 200px;
  flex: 0 0 auto;
}
.sr-row__avatar-wrap { flex-shrink: 0; }
.sr-row__avatar {
  position: relative;
  width: 38px;
  height: 38px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}
.sr-row__initials { font-size: 0.8rem; font-weight: 900; }
.sr-row__status-ring {
  position: absolute;
  inset: -2px;
  border-radius: 50%;
  border: 2px solid;
}
.sr-row__name-group { min-width: 0; }
.sr-row__name {
  font-size: 0.825rem;
  font-weight: 800;
  color: var(--ds-text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin: 0;
}
.sr-row__status {
  display: inline-flex;
  align-items: center;
  gap: 0.3rem;
  font-size: 0.68rem;
  font-weight: 600;
}

/* Cells */
.sr-row__cell { display: flex; align-items: center; gap: 0.375rem; }
.sr-row__cell--violations { min-width: 52px; justify-content: center; }
.sr-row__cell--risk { flex: 1; min-width: 120px; gap: 0.5rem; }
.sr-row__cell--tags { min-width: 80px; gap: 0.25rem; }
.sr-row__cell--actions { display: flex; align-items: center; gap: 0.3rem; flex-shrink: 0; margin-left: auto; }

/* Violation badge */
.sr-row__viol-badge {
  font-size: 0.75rem;
  font-weight: 900;
  padding: 0.2rem 0.55rem;
  border-radius: 9999px;
  font-variant-numeric: tabular-nums;
}
.sr-row__viol-badge--danger { color: var(--ds-danger); background: var(--ds-danger-bg); }
.sr-row__viol-badge--warn { color: var(--ds-warning); background: var(--ds-warning-bg); }
.sr-row__muted { display: flex; align-items: center; justify-content: center; }
.sr-row__clean-icon { color: var(--ds-success); }

/* Risk bar */
.sr-row__risk-bar {
  flex: 1;
  height: 6px;
  border-radius: 9999px;
  background: var(--ds-gray-100);
  overflow: hidden;
}
.sr-row__risk-fill {
  height: 100%;
  border-radius: 9999px;
  transition: width 0.5s ease;
  will-change: width;
}
.sr-row__risk-score {
  font-size: 0.875rem;
  font-weight: 900;
  min-width: 26px;
  text-align: right;
  font-variant-numeric: tabular-nums;
}

/* Tags */
.sr-row__tag {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border-radius: var(--ds-radius-md);
}
.sr-row__tag--danger { color: var(--ds-danger); background: var(--ds-danger-bg); }
.sr-row__tag--warn { color: var(--ds-warning); background: var(--ds-warning-bg); }
.sr-row__tag--neutral { color: var(--ds-text-secondary); background: var(--ds-surface-muted); }

/* Actions */
.sr-action {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--ds-radius-md);
  border: 1px solid var(--ds-border);
  background: var(--ds-surface);
  color: var(--ds-text-secondary);
  cursor: pointer;
  transition: all 0.15s;
}
.sr-action:disabled { opacity: 0.4; cursor: not-allowed; }
.sr-action--warn { color: var(--ds-warning); }
.sr-action--warn:hover:not(:disabled) { background: var(--ds-warning-bg); border-color: var(--ds-warning-soft); }
.sr-action--pause { color: var(--ds-primary); }
.sr-action--pause:hover:not(:disabled) { background: var(--ds-primary-soft); border-color: var(--ds-primary-border); }
.sr-action--resume { color: var(--ds-success); background: var(--ds-success-bg); border-color: var(--ds-success-soft); }
.sr-action--resume:hover:not(:disabled) { background: var(--ds-success); color: #fff; border-color: var(--ds-success); }
.sr-action--detail { color: var(--ds-text-secondary); }
.sr-action--detail:hover:not(:disabled) { background: var(--ds-surface-muted); border-color: var(--ds-primary-border); color: var(--ds-text); }
</style>
