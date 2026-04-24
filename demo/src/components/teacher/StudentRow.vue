<template>
  <div
    class="sr-row"
    :class="[`sr-row--${riskBand}`, { 'sr-row--selected': selected }]"
    @click="$emit('view-detail')"
  >
    <!-- Checkbox -->
    <div class="sr-row__check" @click.stop>
      <input
        type="checkbox"
        class="sr-row__checkbox"
        :checked="selected"
        @change="$emit('toggle-select')"
      />
    </div>

    <!-- Avatar + name -->
    <div class="sr-row__identity">
      <div class="sr-row__avatar" :style="{ background: avatarBg }">
        <span class="sr-row__avatar-text" :style="{ color: avatarColor }">{{ initials }}</span>
        <div class="sr-row__status-dot" :style="{ background: statusColor }" />
      </div>
      <div class="sr-row__name-group">
        <h4 class="sr-row__name">{{ studentName }}</h4>
        <span class="sr-row__status" :style="{ color: statusColor }">
          <LucideIcon :name="statusIcon" :size="10" />
          {{ statusLabel }}
        </span>
      </div>
    </div>

    <!-- Violations -->
    <div class="sr-row__cell sr-row__cell--violations">
      <span
        v-if="violationCount > 0"
        class="sr-row__violation-badge"
        :class="violationCount > 3 ? 'sr-row__violation-badge--danger' : 'sr-row__violation-badge--warn'"
      >
        {{ violationCount }}
      </span>
      <span v-else class="sr-row__muted">—</span>
    </div>

    <!-- Risk -->
    <div class="sr-row__cell sr-row__cell--risk">
      <div class="sr-row__risk-bar">
        <div
          class="sr-row__risk-fill"
          :style="{ width: riskScore + '%', background: riskColor }"
        />
      </div>
      <span class="sr-row__risk-score" :style="{ color: riskColor }">{{ riskScore }}</span>
    </div>

    <!-- Violation tags (collapsed) -->
    <div class="sr-row__cell sr-row__cell--tags">
      <span
        v-for="v in topViolations"
        :key="v.type"
        class="sr-row__tag"
        :class="v.tagClass"
      >
        <LucideIcon :name="v.icon" :size="9" />
      </span>
    </div>

    <!-- Actions -->
    <div class="sr-row__cell sr-row__cell--actions" @click.stop>
      <button class="sr-row__action" title="Gửi cảnh báo" @click="$emit('warn')">
        <LucideIcon name="alert-triangle" :size="13" />
      </button>
      <button
        v-if="isPaused"
        class="sr-row__action sr-row__action--resume"
        title="Cho phép tiếp tục thi"
        @click="$emit('resume')"
      >
        <LucideIcon name="play" :size="13" />
      </button>
      <button
        v-else
        class="sr-row__action"
        :disabled="isTerminal"
        :title="isTerminal ? 'Bài thi đã kết thúc' : 'Tạm dừng'"
        @click="$emit('pause')"
      >
        <LucideIcon name="pause" :size="13" />
      </button>
      <button class="sr-row__action sr-row__action--primary" title="Xem chi tiết" @click="$emit('view-detail')">
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
  padding: 0.7rem 0.875rem;
  background: var(--ds-surface);
  border: 1px solid var(--ds-border);
  border-radius: var(--ds-radius-lg);
  cursor: pointer;
  transition: all 0.15s;
  border-left: 3px solid transparent;
  box-shadow: var(--ds-shadow-xs);
}
.sr-row:hover { background: var(--ds-surface-muted); border-color: var(--ds-primary-border); transform: translateY(-1px); box-shadow: var(--ds-shadow-sm); }
.sr-row--selected { border-color: var(--ds-primary); box-shadow: var(--ds-shadow-focus); }
.sr-row--danger { border-left-color: var(--ds-danger); }
.sr-row--warn { border-left-color: var(--ds-warning); }
.sr-row--clean { border-left-color: var(--ds-success); }

.sr-row__check { flex-shrink: 0; }
.sr-row__checkbox { width: 15px; height: 15px; cursor: pointer; accent-color: var(--ds-primary); }

.sr-row__identity {
  display: flex; align-items: center; gap: 0.625rem;
  min-width: 160px; max-width: 200px; flex: 0 0 auto;
}
.sr-row__avatar {
  position: relative;
  width: 36px; height: 36px; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0;
}
.sr-row__avatar-text { font-size: 0.78rem; font-weight: 800; }
.sr-row__status-dot {
  position: absolute; bottom: 0; right: 0;
  width: 9px; height: 9px; border-radius: 50%;
  border: 2px solid var(--ds-surface);
}
.sr-row__name-group { min-width: 0; }
.sr-row__name {
  font-size: 0.82rem; font-weight: 700; color: var(--ds-text);
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis; margin: 0;
}
.sr-row__status {
  display: inline-flex; align-items: center; gap: 0.3rem;
  font-size: 0.68rem; font-weight: 600;
}

.sr-row__cell { display: flex; align-items: center; gap: 0.375rem; }
.sr-row__cell--violations { min-width: 48px; justify-content: center; }
.sr-row__cell--risk { flex: 1; min-width: 120px; gap: 0.5rem; }
.sr-row__cell--tags { min-width: 80px; gap: 0.25rem; }
.sr-row__cell--actions { display: flex; align-items: center; gap: 0.3rem; flex-shrink: 0; margin-left: auto; }

.sr-row__violation-badge {
  font-size: 0.75rem; font-weight: 800;
  padding: 0.18rem 0.55rem; border-radius: 9999px;
  font-variant-numeric: tabular-nums;
}
.sr-row__violation-badge--danger { color: var(--ds-danger); background: var(--ds-danger-bg); }
.sr-row__violation-badge--warn { color: var(--ds-warning); background: var(--ds-warning-bg); }
.sr-row__muted { font-size: 0.78rem; color: var(--ds-text-muted); }

.sr-row__risk-bar { flex: 1; height: 5px; border-radius: 9999px; background: var(--ds-gray-100); overflow: hidden; }
.sr-row__risk-fill { height: 100%; border-radius: 9999px; transition: width 0.4s ease; will-change: width; }
.sr-row__risk-score { font-size: 0.82rem; font-weight: 800; min-width: 24px; text-align: right; font-variant-numeric: tabular-nums; }

.sr-row__tag {
  display: inline-flex; align-items: center; justify-content: center;
  width: 22px; height: 22px; border-radius: var(--ds-radius-md);
}
.sr-row__tag--danger { color: var(--ds-danger); background: var(--ds-danger-bg); }
.sr-row__tag--warn { color: var(--ds-warning); background: var(--ds-warning-bg); }
.sr-row__tag--neutral { color: var(--ds-text-secondary); background: var(--ds-surface-muted); }

.sr-row__action {
  width: 30px; height: 30px;
  display: flex; align-items: center; justify-content: center;
  border-radius: var(--ds-radius-md);
  border: 1px solid var(--ds-border);
  background: var(--ds-surface);
  color: var(--ds-text-secondary);
  cursor: pointer; transition: all 0.15s;
}
.sr-row__action:hover:not(:disabled) { background: var(--ds-surface-muted); color: var(--ds-text); border-color: var(--ds-primary-border); }
.sr-row__action:disabled { opacity: 0.4; cursor: not-allowed; }
.sr-row__action--primary { color: var(--ds-primary); border-color: var(--ds-primary-border); background: var(--ds-primary-soft); }
.sr-row__action--primary:hover:not(:disabled) { background: var(--ds-primary); color: #fff; border-color: var(--ds-primary); }
.sr-row__action--resume { color: var(--ds-success); background: var(--ds-success-bg); border-color: var(--ds-success-soft); }
.sr-row__action--resume:hover:not(:disabled) { background: var(--ds-success); color: #fff; border-color: var(--ds-success); }
</style>
