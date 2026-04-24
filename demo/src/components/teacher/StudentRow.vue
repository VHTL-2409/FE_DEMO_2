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
        :style="{ color: violationCount > 3 ? '#f87171' : '#fbbf24', background: violationCount > 3 ? 'rgba(248,113,113,0.12)' : 'rgba(251,191,36,0.12)' }"
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
      <button class="sr-row__action" title="Tạm dừng" @click="$emit('pause')">
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

const props = defineProps({
  student: { type: Object, required: true },
  selected: { type: Boolean, default: false }
})
defineEmits(['toggle-select', 'view-detail', 'warn', 'pause'])

const studentName = computed(() =>
  props.student.student || props.student.name || props.student.userName || '—'
)
const initials = computed(() => {
  const name = studentName.value
  if (!name || name === '—') return '?'
  const parts = name.split(' ')
  return (parts[parts.length - 1].charAt(0) + parts[0].charAt(0)).toUpperCase()
})

const riskScore = computed(() => Math.round(Number(props.student.riskScore || 0)))
const riskBand = computed(() => {
  if (riskScore.value >= 60) return 'danger'
  if (riskScore.value >= 30) return 'warn'
  return 'clean'
})
const riskColor = computed(() =>
  riskScore.value >= 60 ? '#f87171'
    : riskScore.value >= 30 ? '#fbbf24'
    : '#4ade80'
)
const avatarBg = computed(() =>
  riskScore.value >= 60 ? 'rgba(248,113,113,0.12)'
    : riskScore.value >= 30 ? 'rgba(251,191,36,0.12)'
    : 'rgba(74,222,128,0.12)'
)
const avatarColor = computed(() => riskColor.value)

const statusLabel = computed(() => {
  const s = String(props.student.status || '').toUpperCase()
  if (/SUBMITTED/.test(s)) return 'Đã nộp'
  if (/STOPPED/.test(s)) return 'Đã dừng'
  if (/PAUSED/.test(s)) return 'Tạm dừng'
  if (/ACTIVE|IN_PROGRESS/.test(s)) return 'Đang thi'
  return props.student.status || '—'
})
const statusIcon = computed(() => {
  const s = String(props.student.status || '').toUpperCase()
  if (/SUBMITTED/.test(s)) return 'check-circle'
  if (/STOPPED/.test(s)) return 'x-circle'
  if (/PAUSED/.test(s)) return 'pause-circle'
  if (/ACTIVE|IN_PROGRESS/.test(s)) return 'play-circle'
  return 'help-circle'
})
const statusColor = computed(() => {
  const s = String(props.student.status || '').toUpperCase()
  if (/SUBMITTED/.test(s)) return '#4ade80'
  if (/STOPPED/.test(s)) return '#f87171'
  if (/PAUSED/.test(s)) return '#fbbf24'
  if (/ACTIVE|IN_PROGRESS/.test(s)) return '#a5b4fc'
  return '#94a3b8'
})

const violationCount = computed(() => (props.student.reasons || []).length)
const violationMap = {
  TAB_SWITCH: { icon: 'layers', tagClass: 'sr-row__tag--warn' },
  COPY_PASTE: { icon: 'copy', tagClass: 'sr-row__tag--danger' },
  DEVTOOLS_OPEN: { icon: 'code', tagClass: 'sr-row__tag--danger' },
  EXIT_FULLSCREEN: { icon: 'minimize', tagClass: 'sr-row__tag--warn' },
  DUPLICATE_IP: { icon: 'globe', tagClass: 'sr-row__tag--danger' },
  MULTI_MONITOR: { icon: 'monitor', tagClass: 'sr-row__tag--danger' },
}
const topViolations = computed(() =>
  (props.student.reasons || []).slice(0, 3).map(r => ({
    type: r,
    ...(violationMap[r] || { icon: 'alert-circle', tagClass: 'sr-row__tag--neutral' })
  }))
)
</script>

<style scoped>
.sr-row {
  display: flex;
  align-items: center;
  gap: 0.875rem;
  padding: 0.625rem 0.875rem;
  background: rgba(30, 41, 59, 0.5);
  border: 1px solid rgba(255,255,255,0.06);
  border-radius: var(--ds-radius-lg);
  cursor: pointer;
  transition: background 0.15s, border-color 0.15s;
  border-left: 3px solid transparent;
}
.sr-row:hover { background: rgba(30, 41, 59, 0.8); border-color: rgba(99,102,241,0.3); }
.sr-row--selected { border-color: rgba(99,102,241,0.5); box-shadow: 0 0 0 1px rgba(99,102,241,0.2); }
.sr-row--danger { border-left-color: rgba(248,113,113,0.6); }
.sr-row--warn { border-left-color: rgba(251,191,36,0.6); }
.sr-row--clean { border-left-color: rgba(74,222,128,0.4); }

.sr-row__check { flex-shrink: 0; }
.sr-row__checkbox { width: 15px; height: 15px; cursor: pointer; accent-color: #6366f1; }

.sr-row__identity {
  display: flex; align-items: center; gap: 0.625rem;
  min-width: 160px; max-width: 200px; flex: 0 0 auto;
}
.sr-row__avatar {
  position: relative;
  width: 34px; height: 34px; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0;
}
.sr-row__avatar-text { font-size: 0.75rem; font-weight: 900; }
.sr-row__status-dot {
  position: absolute; bottom: 0; right: 0;
  width: 9px; height: 9px; border-radius: 50%;
  border: 2px solid rgba(30,41,59,0.8);
}
.sr-row__name-group { min-width: 0; }
.sr-row__name {
  font-size: 0.8rem; font-weight: 700; color: white;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis; margin: 0;
}
.sr-row__status {
  display: flex; align-items: center; gap: 0.25rem;
  font-size: 0.65rem; font-weight: 600;
}

.sr-row__cell { display: flex; align-items: center; gap: 0.375rem; }
.sr-row__cell--violations { min-width: 48px; justify-content: center; }
.sr-row__cell--risk { flex: 1; min-width: 120px; gap: 0.5rem; }
.sr-row__cell--tags { min-width: 80px; gap: 0.2rem; }
.sr-row__cell--actions { display: flex; align-items: center; gap: 0.25rem; flex-shrink: 0; margin-left: auto; }

.sr-row__violation-badge {
  font-size: 0.75rem; font-weight: 900;
  padding: 0.15rem 0.5rem; border-radius: 9999px;
  font-variant-numeric: tabular-nums;
}
.sr-row__muted { font-size: 0.75rem; color: rgba(255,255,255,0.3); }

.sr-row__risk-bar { flex: 1; height: 4px; border-radius: 9999px; background: rgba(255,255,255,0.08); overflow: hidden; }
.sr-row__risk-fill { height: 100%; border-radius: 9999px; transition: width 0.4s ease; will-change: width; }
.sr-row__risk-score { font-size: 0.8rem; font-weight: 900; min-width: 24px; text-align: right; font-variant-numeric: tabular-nums; }

.sr-row__tag {
  display: inline-flex; align-items: center; justify-content: center;
  width: 22px; height: 22px; border-radius: var(--ds-radius);
}
.sr-row__tag--danger { color: #f87171; background: rgba(248,113,113,0.1); }
.sr-row__tag--warn { color: #fbbf24; background: rgba(251,191,36,0.1); }
.sr-row__tag--neutral { color: #94a3b8; background: rgba(148,163,184,0.08); }

.sr-row__action {
  width: 28px; height: 28px;
  display: flex; align-items: center; justify-content: center;
  border-radius: var(--ds-radius);
  border: 1px solid rgba(255,255,255,0.08);
  background: rgba(255,255,255,0.04);
  color: rgba(255,255,255,0.5);
  cursor: pointer; transition: all 0.15s;
}
.sr-row__action:hover { background: rgba(255,255,255,0.08); color: white; border-color: rgba(255,255,255,0.15); }
.sr-row__action--primary { color: #a5b4fc; border-color: rgba(99,102,241,0.2); }
.sr-row__action--primary:hover { background: rgba(165,180,252,0.12); border-color: rgba(165,180,252,0.3); color: #a5b4fc; }
</style>
