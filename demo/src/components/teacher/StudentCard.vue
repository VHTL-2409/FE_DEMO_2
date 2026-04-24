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
        <button class="sc-card__action-btn sc-card__action-btn--pause" @click="$emit('pause')" title="Tạm dừng">
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
  if (/SUBMITTED|AUTO_SUBMITTED/.test(s)) return 'Đã nộp'
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
  TAB_SWITCH: { label: 'Tab', icon: 'layers', tagClass: 'sc-card__violation-tag--warn' },
  COPY_PASTE: { label: 'Copy', icon: 'copy', tagClass: 'sc-card__violation-tag--danger' },
  DEVTOOLS_OPEN: { label: 'DevTools', icon: 'code', tagClass: 'sc-card__violation-tag--danger' },
  EXIT_FULLSCREEN: { label: 'Fullscreen', icon: 'minimize', tagClass: 'sc-card__violation-tag--warn' },
  DUPLICATE_IP: { label: 'IP trùng', icon: 'globe', tagClass: 'sc-card__violation-tag--danger' },
  MULTI_MONITOR: { label: 'Multi', icon: 'monitor', tagClass: 'sc-card__violation-tag--danger' },
}
const topViolations = computed(() =>
  (props.student.reasons || []).slice(0, 3).map(r => ({
    type: r,
    ...(violationMap[r] || { label: r, icon: 'alert-circle', tagClass: 'sc-card__violation-tag--neutral' })
  }))
)
</script>

<style scoped>
.sc-card {
  position: relative;
  background: rgba(30, 41, 59, 0.7);
  backdrop-filter: blur(12px);
  border: 1px solid rgba(255,255,255,0.08);
  border-radius: var(--ds-radius-xl);
  overflow: hidden;
  cursor: pointer;
  transition: border-color 0.15s, box-shadow 0.15s, transform 0.15s;
}
.sc-card:hover {
  border-color: rgba(99,102,241,0.4);
  box-shadow: 0 0 0 1px rgba(99,102,241,0.2), 0 8px 24px rgba(0,0,0,0.3);
  transform: translateY(-1px);
}
.sc-card--selected {
  border-color: rgba(99,102,241,0.6);
  box-shadow: 0 0 0 2px rgba(99,102,241,0.3);
}
.sc-card--danger { border-top: 2px solid rgba(248,113,113,0.6); }
.sc-card--warn { border-top: 2px solid rgba(251,191,36,0.6); }
.sc-card--clean { border-top: 2px solid rgba(74,222,128,0.4); }

.sc-card__strip { display: none; }

.sc-card__body { padding: 0.875rem; }

.sc-card__header {
  display: flex; align-items: center; gap: 0.625rem;
  margin-bottom: 0.75rem;
}
.sc-card__avatar {
  position: relative;
  width: 36px; height: 36px; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0;
}
.sc-card__avatar-text { font-size: 0.8rem; font-weight: 900; }
.sc-card__status-dot {
  position: absolute; bottom: 0; right: 0;
  width: 10px; height: 10px; border-radius: 50%;
  border: 2px solid rgba(30,41,59,0.8);
}
.sc-card__identity { flex: 1; min-width: 0; }
.sc-card__name {
  font-size: 0.8rem; font-weight: 800; color: white;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis; margin: 0 0 0.2rem;
}
.sc-card__status {
  display: flex; align-items: center; gap: 0.25rem;
  font-size: 0.68rem; font-weight: 600;
}

.sc-card__check-wrap { flex-shrink: 0; }
.sc-card__check {
  width: 15px; height: 15px; cursor: pointer;
  accent-color: #6366f1;
}

.sc-card__risk {
  display: flex; align-items: center; gap: 0.5rem;
  margin-bottom: 0.5rem;
}
.sc-card__risk-bar-wrap { flex: 1; }
.sc-card__risk-bar {
  height: 4px; border-radius: 9999px;
  background: rgba(255,255,255,0.08); overflow: hidden;
}
.sc-card__risk-fill {
  height: 100%; border-radius: 9999px;
  transition: width 0.4s ease; will-change: width;
}
.sc-card__risk-score {
  font-size: 0.8rem; font-weight: 900;
  min-width: 24px; text-align: right;
  font-variant-numeric: tabular-nums;
}

.sc-card__violations {
  display: flex; gap: 0.25rem; flex-wrap: wrap;
  margin-bottom: 0.75rem;
}
.sc-card__violation-tag {
  display: inline-flex; align-items: center; gap: 0.2rem;
  padding: 0.15rem 0.5rem;
  border-radius: 9999px;
  font-size: 0.62rem; font-weight: 700;
}
.sc-card__violation-tag--danger { color: #f87171; background: rgba(248,113,113,0.12); }
.sc-card__violation-tag--warn { color: #fbbf24; background: rgba(251,191,36,0.12); }
.sc-card__violation-tag--neutral { color: #94a3b8; background: rgba(148,163,184,0.1); }
.sc-card__violation-more {
  font-size: 0.62rem; color: #94a3b8;
  padding: 0.15rem 0.4rem;
  background: rgba(148,163,184,0.08);
  border-radius: 9999px;
}

.sc-card__actions {
  display: flex; gap: 0.375rem;
  padding-top: 0.625rem;
  border-top: 1px solid rgba(255,255,255,0.06);
}
.sc-card__action-btn {
  flex: 1; height: 28px;
  display: flex; align-items: center; justify-content: center;
  border-radius: var(--ds-radius);
  border: 1px solid rgba(255,255,255,0.08);
  background: rgba(255,255,255,0.04);
  cursor: pointer; transition: all 0.15s;
}
.sc-card__action-btn--warn { color: #fbbf24; }
.sc-card__action-btn--warn:hover { background: rgba(251,191,36,0.12); border-color: rgba(251,191,36,0.3); }
.sc-card__action-btn--pause { color: #a5b4fc; }
.sc-card__action-btn--pause:hover { background: rgba(165,180,252,0.12); border-color: rgba(165,180,252,0.3); }
.sc-card__action-btn--detail { color: #94a3b8; }
.sc-card__action-btn--detail:hover { background: rgba(255,255,255,0.06); border-color: rgba(255,255,255,0.15); color: white; }
</style>
