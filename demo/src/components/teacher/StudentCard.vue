<template>
  <div
    class="sc-card"
    :class="[`sc-card--${riskBand}`, { 'sc-card--selected': selected }]"
    @click="$emit('view-detail')"
  >
    <!-- Status indicator strip -->
    <div class="sc-card__strip" />

    <div class="sc-card__body">
      <!-- Header row -->
      <div class="sc-card__header">
        <div class="sc-card__avatar" :style="{ background: avatarBg }">
          <span class="sc-card__avatar-text" :style="{ color: avatarColor }">
            {{ initials }}
          </span>
          <div class="sc-card__status-dot" :style="{ background: statusColor }" />
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
        </label>
      </div>

      <!-- Risk gauge -->
      <div class="sc-card__risk">
        <div class="sc-card__risk-bar-wrap">
          <div class="sc-card__risk-bar">
            <div
              class="sc-card__risk-fill"
              :style="{ width: riskScore + '%', background: riskColor }"
            />
          </div>
        </div>
        <span class="sc-card__risk-score" :style="{ color: riskColor }">
          {{ riskScore }}
        </span>
      </div>

      <!-- Violations -->
      <div v-if="violationCount > 0" class="sc-card__violations">
        <span
          v-for="v in topViolations"
          :key="v.type"
          class="sc-card__violation-tag"
          :class="v.tagClass"
        >
          <LucideIcon :name="v.icon" :size="9" />
          {{ v.label }}
        </span>
        <span v-if="violationCount > topViolations.length" class="sc-card__violation-more">
          +{{ violationCount - topViolations.length }}
        </span>
      </div>

      <!-- Actions -->
      <div class="sc-card__actions" @click.stop>
        <button class="sc-card__action-btn sc-card__action-btn--warn" @click="$emit('warn')" title="Gửi cảnh báo">
          <LucideIcon name="alert-triangle" :size="12" />
        </button>
        <button
          v-if="isPaused"
          class="sc-card__action-btn sc-card__action-btn--resume"
          @click="$emit('resume')"
          title="Cho phép tiếp tục thi"
        >
          <LucideIcon name="play" :size="12" />
        </button>
        <button
          v-else
          class="sc-card__action-btn sc-card__action-btn--pause"
          :disabled="isTerminal"
          @click="$emit('pause')"
          :title="isTerminal ? 'Bài thi đã kết thúc' : 'Tạm dừng'"
        >
          <LucideIcon name="pause" :size="12" />
        </button>
        <button class="sc-card__action-btn sc-card__action-btn--detail" @click="$emit('view-detail')" title="Xem chi tiết">
          <LucideIcon name="eye" :size="12" />
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
  TAB_SWITCH: { label: 'Tab', icon: 'layers', tagClass: 'sc-card__violation-tag--warn' },
  COPY_PASTE: { label: 'Copy', icon: 'copy', tagClass: 'sc-card__violation-tag--danger' },
  DEVTOOLS_OPEN: { label: 'DevTools', icon: 'code', tagClass: 'sc-card__violation-tag--danger' },
  EXIT_FULLSCREEN: { label: 'Fullscreen', icon: 'minimize', tagClass: 'sc-card__violation-tag--warn' },
  DUPLICATE_IP: { label: 'IP trùng', icon: 'globe', tagClass: 'sc-card__violation-tag--danger' },
  MULTI_MONITOR: { label: 'Multi', icon: 'monitor', tagClass: 'sc-card__violation-tag--danger' }
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
    ...(VIOLATION_MAP[r] || { label: r, icon: 'alert-circle', tagClass: 'sc-card__violation-tag--neutral' })
  }))
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
  transform: translateY(-1px);
}
.sc-card--selected {
  border-color: var(--ds-primary);
  box-shadow: var(--ds-shadow-focus);
}
.sc-card--danger { border-top: 2px solid var(--ds-danger); }
.sc-card--warn { border-top: 2px solid var(--ds-warning); }
.sc-card--clean { border-top: 2px solid var(--ds-success); }

.sc-card__strip { display: none; }

.sc-card__body { padding: 0.875rem; }

.sc-card__header {
  display: flex; align-items: center; gap: 0.625rem;
  margin-bottom: 0.75rem;
}
.sc-card__avatar {
  position: relative;
  width: 38px; height: 38px; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0;
}
.sc-card__avatar-text { font-size: 0.82rem; font-weight: 800; }
.sc-card__status-dot {
  position: absolute; bottom: 0; right: 0;
  width: 10px; height: 10px; border-radius: 50%;
  border: 2px solid var(--ds-surface);
}
.sc-card__identity { flex: 1; min-width: 0; }
.sc-card__name {
  font-size: 0.825rem; font-weight: 700; color: var(--ds-text);
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis; margin: 0 0 0.2rem;
}
.sc-card__status {
  display: inline-flex; align-items: center; gap: 0.3rem;
  font-size: 0.7rem; font-weight: 600;
}

.sc-card__check-wrap { flex-shrink: 0; }
.sc-card__check {
  width: 15px; height: 15px; cursor: pointer;
  accent-color: var(--ds-primary);
}

.sc-card__risk {
  display: flex; align-items: center; gap: 0.5rem;
  margin-bottom: 0.5rem;
}
.sc-card__risk-bar-wrap { flex: 1; }
.sc-card__risk-bar {
  height: 5px; border-radius: 9999px;
  background: var(--ds-gray-100); overflow: hidden;
}
.sc-card__risk-fill {
  height: 100%; border-radius: 9999px;
  transition: width 0.4s ease; will-change: width;
}
.sc-card__risk-score {
  font-size: 0.82rem; font-weight: 800;
  min-width: 26px; text-align: right;
  font-variant-numeric: tabular-nums;
}

.sc-card__violations {
  display: flex; gap: 0.3rem; flex-wrap: wrap;
  margin-bottom: 0.75rem;
}
.sc-card__violation-tag {
  display: inline-flex; align-items: center; gap: 0.25rem;
  padding: 0.18rem 0.5rem;
  border-radius: 9999px;
  font-size: 0.65rem; font-weight: 700;
}
.sc-card__violation-tag--danger { color: var(--ds-danger); background: var(--ds-danger-bg); }
.sc-card__violation-tag--warn { color: var(--ds-warning); background: var(--ds-warning-bg); }
.sc-card__violation-tag--neutral { color: var(--ds-text-secondary); background: var(--ds-surface-muted); }
.sc-card__violation-more {
  font-size: 0.65rem; color: var(--ds-text-muted);
  padding: 0.18rem 0.5rem;
  background: var(--ds-surface-muted);
  border-radius: 9999px; font-weight: 700;
}

.sc-card__actions {
  display: flex; gap: 0.375rem;
  padding-top: 0.625rem;
  border-top: 1px solid var(--ds-border);
}
.sc-card__action-btn {
  flex: 1; height: 30px;
  display: flex; align-items: center; justify-content: center;
  border-radius: var(--ds-radius-md);
  border: 1px solid var(--ds-border);
  background: var(--ds-surface);
  color: var(--ds-text-secondary);
  cursor: pointer; transition: all 0.15s;
}
.sc-card__action-btn:disabled { opacity: 0.4; cursor: not-allowed; }
.sc-card__action-btn--warn { color: var(--ds-warning); }
.sc-card__action-btn--warn:hover:not(:disabled) { background: var(--ds-warning-bg); border-color: var(--ds-warning-soft); }
.sc-card__action-btn--pause { color: var(--ds-primary); }
.sc-card__action-btn--pause:hover:not(:disabled) { background: var(--ds-primary-soft); border-color: var(--ds-primary-border); }
.sc-card__action-btn--resume { color: var(--ds-success); background: var(--ds-success-bg); border-color: var(--ds-success-soft); }
.sc-card__action-btn--resume:hover:not(:disabled) { background: var(--ds-success); color: #fff; border-color: var(--ds-success); }
.sc-card__action-btn--detail { color: var(--ds-text-secondary); }
.sc-card__action-btn--detail:hover:not(:disabled) { background: var(--ds-surface-muted); border-color: var(--ds-primary-border); color: var(--ds-text); }
</style>
