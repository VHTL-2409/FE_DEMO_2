<template>
  <div class="se">
    <div class="se__section">
      <div class="se__section-header">
        <div class="se__section-icon">
          <LucideIcon name="schedule" />
        </div>
        <div>
          <h3 class="se__section-title">Lịch thi</h3>
          <p class="se__section-desc">Thiết lập thời gian bắt đầu và kết thúc</p>
        </div>
      </div>

      <div class="se__body">

        <!-- Quick presets -->
        <div class="se__presets">
          <span class="se__presets-label">Đặt nhanh:</span>
          <button type="button" class="se__preset-btn" @click="applyPreset('today')">
            <LucideIcon name="today" />
            Hôm nay
          </button>
          <button type="button" class="se__preset-btn" @click="applyPreset('tomorrow')">
            <LucideIcon name="event" />
            Ngày mai
          </button>
          <button type="button" class="se__preset-btn" @click="applyPreset('nextWeek')">
            <LucideIcon name="date_range" />
            Tuần sau
          </button>
          <button type="button" class="se__preset-btn" @click="applyPreset('open24h')" :class="{ 'se__preset-btn--active': isOpen24h }">
            <LucideIcon name="all_inclusive" />
            Mở 24/7
          </button>
        </div>

        <!-- Visual timeline bar -->
        <div v-if="schedulePreview" class="se__timeline-bar" :class="{ 'se__timeline-bar--error': scheduleError }">
          <div class="se__timeline-track">
            <div class="se__timeline-start se__timeline-point">
              <LucideIcon name="play_arrow" />
              <span class="se__timeline-time">{{ localStartTime ? formatTimeOnly(localStartTime) : '--:--' }}</span>
            </div>
            <div class="se__timeline-range" :class="{ 'se__timeline-range--error': scheduleError }">
              <div class="se__timeline-fill" :style="{ width: timelinePercent + '%' }" />
              <span class="se__timeline-duration-label" v-if="!scheduleError && durationDiff">
                {{ durationDiff }}
              </span>
            </div>
            <div class="se__timeline-end se__timeline-point">
              <LucideIcon name="stop" />
              <span class="se__timeline-time">{{ localEndTime ? formatTimeOnly(localEndTime) : '--:--' }}</span>
            </div>
          </div>
        </div>

        <!-- Start time -->
        <div class="se__block">
          <div class="se__block-label">
            <span class="se__block-icon se__block-icon--start">
              <LucideIcon name="play_arrow" />
            </span>
            <div>
              <p class="se__block-title">Bắt đầu</p>
              <p class="se__block-desc">Thời điểm mở cửa cho học sinh</p>
            </div>
          </div>
          <div class="se__block-inputs">
            <input v-model="localStartDate" type="date" class="se__input" :min="todayStr" @change="closePicker" @input="closePicker" />
            <input v-model="localStartTime" type="time" step="300" class="se__input" @change="closePicker" @input="closePicker" />
            <div class="se__quick">
              <button type="button" class="se__quick-btn" @click="setStartNow">Bây giờ</button>
              <button type="button" class="se__quick-btn" @click="setStartIn(15)">+15p</button>
              <button type="button" class="se__quick-btn" @click="setStartIn(30)">+30p</button>
              <button type="button" class="se__quick-btn" @click="setStartIn(60)">+1h</button>
            </div>
          </div>
        </div>

        <!-- End time -->
        <div class="se__block" :class="{ 'se__block--error': scheduleError }">
          <div class="se__block-label">
            <span class="se__block-icon se__block-icon--end">
              <LucideIcon name="stop" />
            </span>
            <div>
              <p class="se__block-title">Kết thúc</p>
              <p class="se__block-desc">Thời điểm đóng cửa và khóa bài thi</p>
            </div>
          </div>
          <div class="se__block-inputs">
            <input v-model="localEndDate" type="date" class="se__input" :min="localStartDate" :disabled="isOpen24h" @change="closePicker" @input="closePicker" />
            <input v-model="localEndTime" type="time" step="300" class="se__input" :disabled="isOpen24h" @change="closePicker" @input="closePicker" />
            <div class="se__quick">
              <button type="button" class="se__quick-btn se__quick-btn--accent" @click="setEndByDuration">
                Bắt đầu + {{ localDuration }} phút
              </button>
              <button type="button" class="se__quick-btn" @click="setEndAfter(30)">+30p</button>
              <button type="button" class="se__quick-btn" @click="setEndAfter(60)">+1h</button>
              <button type="button" class="se__quick-btn" @click="setEndAfter(120)">+2h</button>
            </div>
          </div>
        </div>

        <!-- Preview / Error -->
        <div v-if="schedulePreview" class="se__preview" :class="{ 'se__preview--error': scheduleError }">
          <LucideIcon :name="scheduleError ? 'warning' : 'event_available'" />
          <div>
            <p class="se__preview-main">{{ scheduleError ? scheduleError : schedulePreview }}</p>
            <p v-if="durationDiff" class="se__preview-sub">Tổng thời lượng: {{ durationDiff }}</p>
          </div>
        </div>

      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  startTime: { type: String, default: '' },
  endTime: { type: String, default: '' },
  durationMinutes: { type: [Number, String], default: 60 }
})

const emit = defineEmits([
  'update:startTime', 'update:endTime', 'update:durationMinutes'
])

const localDuration = computed({
  get: () => Number(props.durationMinutes),
  set: (v) => emit('update:durationMinutes', Math.max(5, Math.min(480, Number(v))))
})

const isOpen24h = computed(() => !props.startTime && !props.endTime)

const fmtDate = (d) => {
  const dd = String(d.getDate()).padStart(2, '0')
  const mm = String(d.getMonth() + 1).padStart(2, '0')
  const yy = d.getFullYear()
  return `${yy}-${mm}-${dd}`
}

const fmtTime = (d) => {
  const hh = String(d.getHours()).padStart(2, '0')
  const min = String(d.getMinutes()).padStart(2, '0')
  return `${hh}:${min}`
}

const parseLocal = (v) => v ? new Date(v) : null

const localStartDate = computed({
  get: () => {
    const d = parseLocal(props.startTime)
    return d ? fmtDate(d) : ''
  },
  set: (v) => {
    const existing = parseLocal(props.startTime)
    const h = existing ? fmtTime(existing) : '08:00'
    emit('update:startTime', v ? `${v}T${h}:00` : '')
  }
})

const localStartTime = computed({
  get: () => {
    const d = parseLocal(props.startTime)
    return d ? fmtTime(d) : ''
  },
  set: (v) => {
    const existing = parseLocal(props.startTime)
    const d = existing ? fmtDate(existing) : fmtDate(new Date())
    emit('update:startTime', d && v ? `${d}T${v}:00` : '')
  }
})

const localEndDate = computed({
  get: () => {
    const d = parseLocal(props.endTime)
    return d ? fmtDate(d) : ''
  },
  set: (v) => {
    const existing = parseLocal(props.endTime)
    const h = existing ? fmtTime(existing) : '09:00'
    emit('update:endTime', v ? `${v}T${h}:00` : '')
  }
})

const localEndTime = computed({
  get: () => {
    const d = parseLocal(props.endTime)
    return d ? fmtTime(d) : ''
  },
  set: (v) => {
    const existing = parseLocal(props.endTime)
    const d = existing ? fmtDate(existing) : localStartDate.value || fmtDate(new Date())
    emit('update:endTime', d && v ? `${d}T${v}:00` : '')
  }
})

const todayStr = computed(() => fmtDate(new Date()))

const schedulePreview = computed(() => {
  if (isOpen24h.value) return 'Mở 24/7 — không giới hạn thời gian'
  const start = parseLocal(props.startTime)
  const end = parseLocal(props.endTime)
  if (!start || !end) return ''
  if (end <= start) return ''
  const opts = { day: '2-digit', month: '2-digit', hour: '2-digit', minute: '2-digit' }
  return `${start.toLocaleString('vi-VN', opts)} → ${end.toLocaleString('vi-VN', opts)}`
})

const scheduleError = computed(() => {
  if (isOpen24h.value) return ''
  const start = parseLocal(props.startTime)
  const end = parseLocal(props.endTime)
  if (!start || !end) return ''
  if (end <= start) return 'Thời gian kết thúc phải sau thời gian bắt đầu.'
  return ''
})

const durationDiff = computed(() => {
  if (isOpen24h.value) return ''
  const start = parseLocal(props.startTime)
  const end = parseLocal(props.endTime)
  if (!start || !end || end <= start) return ''
  const diffMs = end - start
  const diffMin = Math.round(diffMs / 60000)
  if (diffMin < 60) return `${diffMin} phút`
  return `${Math.floor(diffMin / 60)}h ${diffMin % 60 > 0 ? (diffMin % 60) + 'p' : ''}`
})

const timelinePercent = computed(() => {
  if (isOpen24h.value) return 100
  if (!localDuration.value) return 0
  return Math.min(100, Math.max(0, (Number(localDuration.value) / 480) * 100))
})

const formatTimeOnly = (isoString) => {
  if (!isoString) return '--:--'
  const d = parseLocal(isoString)
  return d ? d.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' }) : '--:--'
}

const addMins = (d, mins) => new Date(d.getTime() + mins * 60000)

const setStartNow = () => {
  const now = new Date()
  localStartDate.value = fmtDate(now)
  localStartTime.value = fmtTime(now)
}

const setStartIn = (mins) => {
  const base = parseLocal(props.startTime) || new Date()
  const nd = addMins(base, mins)
  localStartDate.value = fmtDate(nd)
  localStartTime.value = fmtTime(nd)
}

const setEndByDuration = () => {
  const start = parseLocal(props.startTime)
  if (!start) return
  const nd = addMins(start, localDuration.value)
  localEndDate.value = fmtDate(nd)
  localEndTime.value = fmtTime(nd)
}

const setEndAfter = (mins) => {
  const start = parseLocal(props.startTime)
  if (!start) return
  const nd = addMins(start, mins)
  localEndDate.value = fmtDate(nd)
  localEndTime.value = fmtTime(nd)
}

const applyPreset = (preset) => {
  if (preset === 'open24h') {
    emit('update:startTime', '')
    emit('update:endTime', '')
    return
  }
  const now = new Date()
  let target
  if (preset === 'today') {
    target = new Date(now.getFullYear(), now.getMonth(), now.getDate(), 8, 0, 0)
  } else if (preset === 'tomorrow') {
    target = new Date(now.getFullYear(), now.getMonth(), now.getDate() + 1, 8, 0, 0)
  } else if (preset === 'nextWeek') {
    const daysUntilMon = (8 - now.getDay()) % 7 || 7
    target = new Date(now.getFullYear(), now.getMonth(), now.getDate() + daysUntilMon, 8, 0, 0)
  }
  localStartDate.value = fmtDate(target)
  localStartTime.value = fmtTime(target)
  const endTarget = addMins(target, localDuration.value)
  localEndDate.value = fmtDate(endTarget)
  localEndTime.value = fmtTime(endTarget)
}

const closePicker = (event) => {
  const target = event?.target
  if (!target) return
  window.setTimeout(() => target.blur(), 0)
}
</script>


<style scoped>
.se {
  background: var(--ds-surface);
  border: 1px solid var(--ds-border);
  border-radius: var(--ds-radius-2xl);
  overflow: hidden;
}
.dark .se { border-color: var(--ds-border-strong); }

.se__section-header {
  display: flex;
  align-items: center;
  gap: 0.875rem;
  padding: 1.25rem 1.5rem;
  border-bottom: 1px solid var(--ds-border);
  background: linear-gradient(135deg, var(--ds-primary-soft) 0%, transparent 100%);
}
.dark .se__section-header { border-bottom-color: var(--ds-border-strong); }

.se__section-icon {
  width: 40px;
  height: 40px;
  border-radius: var(--ds-radius-lg);
  background: var(--ds-primary);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  box-shadow: 0 4px 12px rgba(79, 70, 229, 0.25);
}

.se__section-title {
  font-family: var(--ds-font-display);
  font-size: 1.05rem;
  font-weight: 700;
  color: var(--ds-text);
  margin: 0;
  line-height: 1.2;
}
.dark .se__section-title { color: #f1f5f9; }

.se__section-desc {
  font-size: 0.75rem;
  color: var(--ds-text-muted);
  margin: 0.25rem 0 0;
}

.se__body {
  padding: 1.5rem;
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

/* Quick presets */
.se__presets {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex-wrap: wrap;
  padding: 0.75rem 1rem;
  background: var(--ds-gray-50);
  border: 1px solid var(--ds-border);
  border-radius: var(--ds-radius-xl);
}
.dark .se__presets { background: var(--ds-gray-800); border-color: var(--ds-border-strong); }

.se__presets-label {
  font-size: 0.8rem;
  font-weight: 700;
  color: var(--ds-text-secondary);
  margin-right: 0.25rem;
}
.dark .se__presets-label { color: #94a3b8; }

.se__preset-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.375rem;
  padding: 0.375rem 0.75rem;
  border-radius: var(--ds-radius-full);
  border: 1.5px solid var(--ds-border);
  background: var(--ds-surface);
  color: var(--ds-text-secondary);
  font-size: 0.75rem;
  font-weight: 700;
  cursor: pointer;
  transition: color 0.15s ease, background-color 0.15s ease, border-color 0.15s ease, box-shadow 0.15s ease, transform 0.15s ease;
}
.dark .se__preset-btn { background: var(--ds-gray-700); border-color: var(--ds-border-strong); color: #94a3b8; }
.se__preset-btn:hover {
  border-color: var(--ds-primary);
  color: var(--ds-primary);
  background: var(--ds-primary-soft);
}
.se__preset-btn--active {
  background: var(--ds-primary);
  border-color: var(--ds-primary);
  color: white;
  box-shadow: 0 4px 12px rgba(79, 70, 229, 0.25);
}

/* Timeline bar */
.se__timeline-bar {
  padding: 0.75rem 1rem;
  background: var(--ds-gray-50);
  border: 1px solid var(--ds-border);
  border-radius: var(--ds-radius-xl);
}
.dark .se__timeline-bar { background: var(--ds-gray-800); border-color: var(--ds-border-strong); }
.se__timeline-bar--error {
  background: var(--ds-danger-soft);
  border-color: rgba(220, 38, 38, 0.2);
}

.se__timeline-track {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.se__timeline-point {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.25rem;
  flex-shrink: 0;
}

.se__timeline-time {
  font-size: 0.7rem;
  font-weight: 700;
  color: var(--ds-text-secondary);
  font-family: var(--ds-font-display);
}
.dark .se__timeline-time { color: #94a3b8; }

.se__timeline-range {
  flex: 1;
  position: relative;
  height: 8px;
  background: var(--ds-gray-200);
  border-radius: 4px;
  overflow: visible;
}
.dark .se__timeline-range { background: var(--ds-gray-700); }
.se__timeline-range--error { background: rgba(220, 38, 38, 0.2); }

.se__timeline-fill {
  height: 100%;
  background: linear-gradient(90deg, var(--ds-success), var(--ds-primary));
  border-radius: 4px;
  transition: width 0.4s ease;
  max-width: 100%;
}

.se__timeline-duration-label {
  position: absolute;
  top: -1.5rem;
  left: 50%;
  transform: translateX(-50%);
  font-size: 0.65rem;
  font-weight: 700;
  color: var(--ds-primary);
  white-space: nowrap;
}

/* Block */
.se__block {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  padding: 1.25rem;
  background: var(--ds-gray-50);
  border: 1.5px solid var(--ds-border);
  border-radius: var(--ds-radius-2xl);
  transition: border-color 0.15s ease;
}
.dark .se__block { background: var(--ds-gray-800); border-color: var(--ds-border-strong); }
.se__block--error { border-color: var(--ds-danger); }

.se__block-label {
  display: flex;
  align-items: flex-start;
  gap: 0.75rem;
}

.se__block-icon {
  width: 36px;
  height: 36px;
  border-radius: var(--ds-radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.se__block-icon--start { background: var(--ds-success-soft); color: var(--ds-success); }
.se__block-icon--end { background: rgba(234, 179, 8, 0.1); color: #d97706; }

.se__block-title {
  font-size: 0.875rem;
  font-weight: 700;
  color: var(--ds-text);
  margin: 0;
}
.dark .se__block-title { color: #f1f5f9; }

.se__block-desc {
  font-size: 0.7rem;
  color: var(--ds-text-muted);
  margin: 0.25rem 0 0;
}

.se__block-inputs {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  padding-left: calc(36px + 0.75rem);
}

.se__input {
  width: 100%;
  padding: 0.75rem 1.25rem;
  background: white;
  border: 2px solid var(--ds-border);
  border-radius: var(--ds-radius-2xl);
  font-size: 0.9375rem;
  color: var(--ds-text);
  outline: none;
  transition: color 0.25s cubic-bezier(0.4, 0, 0.2, 1), background-color 0.25s cubic-bezier(0.4, 0, 0.2, 1), border-color 0.25s cubic-bezier(0.4, 0, 0.2, 1), box-shadow 0.25s cubic-bezier(0.4, 0, 0.2, 1), transform 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.dark .se__input { background: var(--ds-gray-700); border-color: var(--ds-border-strong); color: #f1f5f9; }

.se__input:hover {
  border-color: var(--ds-primary-border);
  box-shadow: 0 4px 12px rgba(79, 70, 229, 0.1);
}

.dark .se__input:hover { border-color: var(--ds-primary-border); box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15); }

.se__input:focus { 
  border-color: var(--ds-primary); 
  box-shadow: 0 0 0 4px var(--ds-primary-ring), 0 8px 24px rgba(79, 70, 229, 0.15);
  transform: translateY(-1px);
}

.dark .se__input:focus { 
  box-shadow: 0 0 0 4px rgba(79, 70, 229, 0.3), 0 8px 24px rgba(0, 0, 0, 0.3);
}

.se__input:disabled { opacity: 0.5; cursor: not-allowed; }

.se__quick {
  display: flex;
  flex-wrap: wrap;
  gap: 0.375rem;
  margin-top: 0.25rem;
}

.se__quick-btn {
  padding: 0.375rem 0.75rem;
  border-radius: var(--ds-radius-full);
  border: 1.5px solid var(--ds-border);
  background: var(--ds-surface);
  color: var(--ds-text-secondary);
  font-size: 0.75rem;
  font-weight: 700;
  cursor: pointer;
  transition: color 0.15s ease, background-color 0.15s ease, border-color 0.15s ease, box-shadow 0.15s ease, transform 0.15s ease;
}
.dark .se__quick-btn { background: var(--ds-gray-700); border-color: var(--ds-border-strong); color: #94a3b8; }
.se__quick-btn:hover {
  border-color: var(--ds-primary);
  background: var(--ds-primary-soft);
  color: var(--ds-primary);
}
.se__quick-btn--accent {
  background: var(--ds-primary-soft);
  border-color: var(--ds-primary-border);
  color: var(--ds-primary);
}
.se__quick-btn--accent:hover { background: var(--ds-primary); color: white; }

/* Field */
.se__field {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.se__label {
  font-size: 0.8rem;
  font-weight: 700;
  color: var(--ds-text);
}
.dark .se__label { color: #f1f5f9; }

.se__optional {
  font-weight: 500;
  color: var(--ds-text-muted);
  font-size: 0.75rem;
}

.se__select {
  width: 100%;
  padding: 0.75rem 1.25rem;
  background: var(--ds-surface);
  border: 2px solid var(--ds-border);
  border-radius: var(--ds-radius-2xl);
  font-size: 0.9375rem;
  color: var(--ds-text);
  outline: none;
  transition: color 0.25s cubic-bezier(0.4, 0, 0.2, 1), background-color 0.25s cubic-bezier(0.4, 0, 0.2, 1), border-color 0.25s cubic-bezier(0.4, 0, 0.2, 1), box-shadow 0.25s cubic-bezier(0.4, 0, 0.2, 1), transform 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  cursor: pointer;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.dark .se__select { background: var(--ds-gray-800); border-color: var(--ds-border-strong); color: #f1f5f9; }

.se__select:hover {
  border-color: var(--ds-primary-border);
  box-shadow: 0 4px 12px rgba(79, 70, 229, 0.1);
  transform: translateY(-1px);
}

.dark .se__select:hover { border-color: var(--ds-primary-border); box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15); }

.se__select:focus { 
  border-color: var(--ds-primary); 
  box-shadow: 0 0 0 4px var(--ds-primary-ring), 0 8px 24px rgba(79, 70, 229, 0.15);
}

.dark .se__select:focus { 
  box-shadow: 0 0 0 4px rgba(79, 70, 229, 0.3), 0 8px 24px rgba(0, 0, 0, 0.3);
}

/* Preview */
.se__preview {
  display: flex;
  align-items: flex-start;
  gap: 0.75rem;
  padding: 1rem 1.25rem;
  background: var(--ds-success-soft);
  border: 1px solid rgba(22, 163, 74, 0.2);
  border-radius: var(--ds-radius-xl);
}
.se__preview--error { background: var(--ds-danger-soft); border-color: rgba(220, 38, 38, 0.2); }
.se__preview--error
.se__preview-main { font-size: 0.875rem; font-weight: 700; color: var(--ds-success); margin: 0; line-height: 1.4; }
.se__preview--error .se__preview-main { color: var(--ds-danger); }
.se__preview-sub { font-size: 0.75rem; color: var(--ds-text-secondary); margin: 0.25rem 0 0; }
</style>
@media (prefers-reduced-motion: reduce) {
  * {
    transition-duration: 0.01ms !important;
    animation-duration: 0.01ms !important;
  }
}