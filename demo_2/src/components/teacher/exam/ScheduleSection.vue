<template>
  <div class="ec-section">
    <div class="ec-section__header">
      <div class="ec-section__icon">
        <LucideIcon name="schedule" />
      </div>
      <div>
        <h3 class="ec-section__title">Lịch thi</h3>
        <p class="ec-section__desc">Thiết lập thời gian bắt đầu, kết thúc và múi giờ</p>
      </div>
    </div>

    <div class="ec-section__body">

      <!-- Start time -->
      <div class="ec-sched-block">
        <div class="ec-sched-block__label">
          <LucideIcon name="play_arrow" />
          <div>
            <p class="ec-sched-block__label-title">Bắt đầu</p>
            <p class="ec-sched-block__label-desc">Thời điểm mở cửa cho học sinh</p>
          </div>
        </div>
        <div class="ec-sched-block__inputs">
          <input
            v-model="localStartDate"
            type="date"
            class="ec-input"
            :min="todayStr"
          />
          <input
            v-model="localStartTime"
            type="time"
            step="300"
            class="ec-input"
          />
          <div class="ec-sched-quick">
            <button type="button" class="ec-sched-quick-btn" @click="setStartNow">Bây giờ</button>
            <button type="button" class="ec-sched-quick-btn" @click="setStartIn15">+15 phút</button>
            <button type="button" class="ec-sched-quick-btn" @click="setStartIn30">+30 phút</button>
            <button type="button" class="ec-sched-quick-btn" @click="setStartIn1h">+1 giờ</button>
          </div>
        </div>
      </div>

      <!-- End time -->
      <div class="ec-sched-block">
        <div class="ec-sched-block__label">
          <LucideIcon name="stop" />
          <div>
            <p class="ec-sched-block__label-title">Kết thúc</p>
            <p class="ec-sched-block__label-desc">Thời điểm đóng cửa và khóa bài thi</p>
          </div>
        </div>
        <div class="ec-sched-block__inputs">
          <input
            v-model="localEndDate"
            type="date"
            class="ec-input"
            :min="localStartDate"
          />
          <input
            v-model="localEndTime"
            type="time"
            step="300"
            class="ec-input"
          />
          <div class="ec-sched-quick">
            <button type="button" class="ec-sched-quick-btn ec-sched-quick-btn--accent" @click="setEndByDuration">
              Bắt đầu + {{ localDuration }} phút
            </button>
            <button type="button" class="ec-sched-quick-btn" @click="setEndAfter(30)">+30 phút</button>
            <button type="button" class="ec-sched-quick-btn" @click="setEndAfter(60)">+1 giờ</button>
            <button type="button" class="ec-sched-quick-btn" @click="setEndAfter(120)">+2 giờ</button>
          </div>
        </div>
      </div>

      <!-- Preview -->
      <div v-if="schedulePreview" class="ec-sched-preview" :class="{ 'ec-sched-preview--error': scheduleError }">
        <LucideIcon :name="scheduleError ? 'warning' : 'event_available'" />
        <div>
          <p class="ec-sched-preview__main">{{ scheduleError ? scheduleError : schedulePreview }}</p>
          <p v-if="durationDiff" class="ec-sched-preview__sub">Tổng thời lượng: {{ durationDiff }}</p>
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

const durationPresets = [15, 30, 45, 60, 90, 120]

const localDuration = computed({
  get: () => Number(props.durationMinutes),
  set: (v) => emit('update:durationMinutes', Math.max(5, Math.min(480, Number(v))))
})

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

const parseLocal = (v) => {
  if (!v) return null
  return new Date(v)
}

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
  const start = parseLocal(props.startTime)
  const end = parseLocal(props.endTime)
  if (!start || !end) return ''
  if (end <= start) return ''
  const opts = { day: '2-digit', month: '2-digit', hour: '2-digit', minute: '2-digit' }
  return `${start.toLocaleString('vi-VN', opts)} → ${end.toLocaleString('vi-VN', opts)}`
})

const scheduleError = computed(() => {
  const start = parseLocal(props.startTime)
  const end = parseLocal(props.endTime)
  if (!start || !end) return ''
  if (end <= start) return 'Thời gian kết thúc phải sau thời gian bắt đầu.'
  return ''
})

const durationDiff = computed(() => {
  const start = parseLocal(props.startTime)
  const end = parseLocal(props.endTime)
  if (!start || !end || end <= start) return ''
  const diffMs = end - start
  const diffMin = Math.round(diffMs / 60000)
  if (diffMin < 60) return `${diffMin} phút`
  return `${Math.floor(diffMin / 60)}h ${diffMin % 60 > 0 ? (diffMin % 60) + 'p' : ''}`
})

const setStartNow = () => {
  const now = new Date()
  localStartDate.value = fmtDate(now)
  localStartTime.value = fmtTime(now)
}

const addMins = (d, mins) => {
  const nd = new Date(d.getTime() + mins * 60000)
  return nd
}

const setStartIn15 = () => {
  const base = parseLocal(props.startTime) || new Date()
  const nd = addMins(base, 15)
  localStartDate.value = fmtDate(nd)
  localStartTime.value = fmtTime(nd)
}

const setStartIn30 = () => {
  const base = parseLocal(props.startTime) || new Date()
  const nd = addMins(base, 30)
  localStartDate.value = fmtDate(nd)
  localStartTime.value = fmtTime(nd)
}

const setStartIn1h = () => {
  const base = parseLocal(props.startTime) || new Date()
  const nd = addMins(base, 60)
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
</script>


<style scoped>
.ec-section {
  background: var(--ds-surface);
  border: 1px solid var(--ds-border);
  border-radius: var(--ds-radius-2xl);
  overflow: hidden;
}

.ec-section__header {
  display: flex;
  align-items: center;
  gap: 0.875rem;
  padding: 1.25rem 1.5rem;
  border-bottom: 1px solid var(--ds-border);
  background: linear-gradient(135deg, var(--ds-primary-soft) 0%, transparent 100%);
}

.ec-section__icon {
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


.ec-section__title {
  font-family: var(--ds-font-display);
  font-size: 1.05rem;
  font-weight: 700;
  color: var(--ds-text);
  margin: 0;
  line-height: 1.2;
}

.dark .ec-section__title { color: #f1f5f9; }

.ec-section__desc {
  font-size: 0.75rem;
  color: var(--ds-text-muted);
  margin: 0.25rem 0 0;
}

.ec-section__body {
  padding: 1.5rem;
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

/* Schedule blocks */
.ec-sched-block {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  padding: 1.25rem;
  background: var(--ds-gray-50);
  border: 1px solid var(--ds-border);
  border-radius: var(--ds-radius-2xl);
}

.dark .ec-sched-block {
  background: var(--ds-gray-800);
  border-color: var(--ds-border-strong);
}

.ec-sched-block__label {
  display: flex;
  align-items: flex-start;
  gap: 0.75rem;
}

.ec-sched-block__label-icon {
  width: 36px;
  height: 36px;
  border-radius: var(--ds-radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.ec-sched-block__label-icon--start {
  background: var(--ds-success-soft);
  color: var(--ds-success);
}

.ec-sched-block__label-icon--end {
  background: var(--ds-accent-soft);
  color: var(--ds-accent);
}


.ec-sched-block__label-title {
  font-size: 0.875rem;
  font-weight: 700;
  color: var(--ds-text);
  margin: 0;
  line-height: 1.2;
}

.dark .ec-sched-block__label-title { color: #f1f5f9; }

.ec-sched-block__label-desc {
  font-size: 0.7rem;
  color: var(--ds-text-muted);
  margin: 0.25rem 0 0;
}

.ec-sched-block__inputs {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  padding-left: calc(36px + 0.75rem);
}

.ec-sched-quick {
  display: flex;
  flex-wrap: wrap;
  gap: 0.375rem;
  margin-top: 0.25rem;
}

.ec-sched-quick-btn {
  padding: 0.375rem 0.75rem;
  border-radius: var(--ds-radius-full);
  border: 1.5px solid var(--ds-border);
  background: var(--ds-surface);
  color: var(--ds-text-secondary);
  font-size: 0.75rem;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.15s ease;
}

.dark .ec-sched-quick-btn {
  background: var(--ds-gray-700);
  border-color: var(--ds-border-strong);
  color: #94a3b8;
}

.ec-sched-quick-btn:hover {
  border-color: var(--ds-primary);
  background: var(--ds-primary-soft);
  color: var(--ds-primary);
}

.ec-sched-quick-btn--accent {
  background: var(--ds-primary-soft);
  border-color: var(--ds-primary-border);
  color: var(--ds-primary);
}

.ec-sched-quick-btn--accent:hover {
  background: var(--ds-primary);
  color: white;
}

/* Inputs */
.ec-input {
  width: 100%;
  padding: 0.6875rem 1rem;
  background: white;
  border: 1.5px solid var(--ds-border);
  border-radius: var(--ds-radius-xl);
  font-size: 0.875rem;
  color: var(--ds-text);
  outline: none;
  transition: all 0.15s ease;
}

.dark .ec-input {
  background: var(--ds-gray-700);
  border-color: var(--ds-border-strong);
  color: #f1f5f9;
}

.ec-input:focus {
  border-color: var(--ds-primary);
  box-shadow: 0 0 0 3px var(--ds-primary-ring);
}

/* Field */
.ec-field {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.ec-field__label {
  font-size: 0.8rem;
  font-weight: 700;
  color: var(--ds-text);
}

.dark .ec-field__label { color: #f1f5f9; }

.ec-field__optional {
  font-weight: 500;
  color: var(--ds-text-muted);
  font-size: 0.75rem;
}

/* Timezone badge */
.ec-tz-badge {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 1rem;
  background: var(--ds-gray-100);
  border: 1px solid var(--ds-border);
  border-radius: var(--ds-radius-full);
  font-size: 0.8rem;
  font-weight: 600;
  color: var(--ds-text-secondary);
  width: fit-content;
}

.dark .ec-tz-badge {
  background: var(--ds-gray-700);
  border-color: var(--ds-border-strong);
  color: #94a3b8;
}


/* Preview */
.ec-sched-preview {
  display: flex;
  align-items: flex-start;
  gap: 0.75rem;
  padding: 1rem 1.25rem;
  background: var(--ds-success-soft);
  border: 1px solid rgba(22, 163, 74, 0.2);
  border-radius: var(--ds-radius-xl);
}

.ec-sched-preview--error {
  background: var(--ds-danger-soft);
  border-color: rgba(220, 38, 38, 0.2);
}


.ec-sched-preview--error

.ec-sched-preview__main {
  font-size: 0.875rem;
  font-weight: 700;
  color: var(--ds-success);
  margin: 0;
  line-height: 1.4;
}

.ec-sched-preview--error .ec-sched-preview__main {
  color: var(--ds-danger);
}

.ec-sched-preview__sub {
  font-size: 0.75rem;
  color: var(--ds-text-secondary);
  margin: 0.25rem 0 0;
}
</style>
