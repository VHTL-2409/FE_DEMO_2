<template>
  <div class="ec-section">
    <div class="ec-section__header">
      <div class="ec-section__icon">
        <LucideIcon name="schedule" />
      </div>
      <div>
        <h3 class="ec-section__title">Lịch thi</h3>
      </div>
    </div>

    <div class="ec-section__body">

      <!-- Exam mode selector -->
      <div class="ec-field">
        <label class="ec-field__label">Hình thức thi</label>
        <div class="ec-mode-group">
          <button
            v-for="m in modes"
            :key="m.value"
            type="button"
            class="ec-mode-btn"
            :class="localMode === m.value && 'ec-mode-btn--active'"
            :title="m.desc"
            @click="localMode = m.value"
          >
            <LucideIcon :name="m.icon" />
            <div class="ec-mode-btn__text">
              <span class="ec-mode-btn__title">{{ m.label }}</span>
              <span class="ec-mode-btn__desc">{{ m.desc }}</span>
            </div>
          </button>
        </div>
      </div>

      <!-- Scheduled mode -->
      <template v-if="localMode === 'scheduled'">

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
              @change="onStartDateChanged"
              @input="onStartDateChanged"
            />
            <input
              v-model="localStartTime"
              type="time"
              step="300"
              class="ec-input"
              @change="onStartTimeChanged"
              @input="onStartTimeChanged"
            />
            <div class="ec-sched-quick">
              <button type="button" class="ec-sched-quick-btn" @click="setStartNow">Bây giờ</button>
              <button type="button" class="ec-sched-quick-btn" @click="setStartIn(15)">+15p</button>
              <button type="button" class="ec-sched-quick-btn" @click="setStartIn(30)">+30p</button>
              <button type="button" class="ec-sched-quick-btn" @click="setStartIn(60)">+1h</button>
            </div>
          </div>
        </div>

        <!-- End time -->
        <div class="ec-sched-block" :class="{ 'ec-sched-block--error': !!scheduleError }">
          <div class="ec-sched-block__label">
            <LucideIcon name="stop" />
            <div>
              <p class="ec-sched-block__label-title">Kết thúc</p>
              <p class="ec-sched-block__label-desc">
                Thời điểm đóng cửa và khóa bài thi
                <span v-if="localStartTimeValue" class="ec-sched-block__label-hint">
                  · cần ≥ {{ localDuration }} phút từ lúc bắt đầu
                </span>
              </p>
            </div>
          </div>
          <div class="ec-sched-block__inputs">
            <input
              v-model="localEndDate"
              type="date"
              class="ec-input"
              :min="localStartDate"
              @change="onEndDateChanged"
              @input="onEndDateChanged"
            />
            <input
              v-model="localEndTime"
              type="time"
              step="300"
              class="ec-input"
              :min="minEndTime"
              @change="onEndTimeChanged"
              @input="onEndTimeChanged"
            />
            <div class="ec-sched-quick">
              <button type="button" class="ec-sched-quick-btn ec-sched-quick-btn--accent" @click="setEndByDuration">
                Bắt đầu + {{ localDuration }} phút
              </button>
              <button type="button" class="ec-sched-quick-btn" @click="setEndAfter(localDuration + 30)">+{{ localDuration + 30 }}p</button>
              <button type="button" class="ec-sched-quick-btn" @click="setEndAfter(localDuration + 60)">+{{ localDuration + 60 }}p</button>
              <button type="button" class="ec-sched-quick-btn" @click="setEndAfter(localDuration + 120)">+{{ localDuration + 120 }}p</button>
            </div>
          </div>
        </div>

        <!-- Schedule error -->
        <div v-if="scheduleError" class="ec-sched-warn">
          <LucideIcon name="warning" />
          <p>{{ scheduleError }}</p>
        </div>

      </template>

      <!-- Flexible mode -->
      <template v-else>
        <div class="ec-sched-block ec-sched-block--info">
          <div class="ec-sched-block__label">
            <LucideIcon name="bolt" />
            <div>
              <p class="ec-sched-block__label-title">Thi không giới hạn thời gian</p>
              <p class="ec-sched-block__label-desc">
                Học sinh có thể làm bài bất cứ lúc nào — không có giới hạn cửa sổ thi hay thời gian làm bài.
                Kết quả được ghi nhận ngay khi nộp bài.
              </p>
            </div>
          </div>
        </div>
      </template>

      <!-- Schedule preview -->
      <div v-if="schedulePreview" class="ec-sched-preview" :class="{ 'ec-sched-preview--error': !!scheduleError }">
        <LucideIcon :name="scheduleError ? 'warning' : 'event_available'" />
        <div>
          <p class="ec-sched-preview__main">{{ scheduleError ? scheduleError : schedulePreview }}</p>
          <p v-if="durationDiff" class="ec-sched-preview__sub">Cửa sổ thi: {{ durationDiff }}</p>
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

// ─── Mode ──────────────────────────────────────────────────────────────────────
const modes = [
  {
    value: 'scheduled',
    label: 'Thi có lịch',
    desc: 'Cửa sổ thi cố định',
    icon: 'calendar_today'
  },
  {
    value: 'flexible',
    label: 'Thi tự do',
    desc: 'Không giới hạn thời gian',
    icon: 'all_inclusive'
  }
]

const localMode = computed({
  get() {
    if (!props.startTime && !props.endTime) return 'flexible'
    return 'scheduled'
  },
  set(v) {
    if (v === 'flexible') {
      emit('update:startTime', '')
      emit('update:endTime', '')
    } else {
      if (!props.startTime) {
        const now = new Date()
        emit('update:startTime', toIsoDefaultStart(now))
      }
    }
  }
})

const localDuration = computed({
  get: () => Number(props.durationMinutes),
  set: (v) => emit('update:durationMinutes', Math.max(5, Math.min(480, Number(v))))
})

// ─── Date / Time formatters ────────────────────────────────────────────────────
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

const toIsoDefaultStart = (d) => `${fmtDate(d)}T${fmtTime(d)}:00`

// ─── Local state ───────────────────────────────────────────────────────────────
const parseLocal = (v) => v ? new Date(v) : null

const localStartDate = computed({
  get() {
    const d = parseLocal(props.startTime)
    return d ? fmtDate(d) : ''
  },
  set(v) {
    const existing = parseLocal(props.startTime)
    const h = existing ? fmtTime(existing) : '08:00'
    emit('update:startTime', v ? `${v}T${h}:00` : '')
  }
})

const localStartTime = computed({
  get() {
    const d = parseLocal(props.startTime)
    return d ? fmtTime(d) : ''
  },
  set(v) {
    const existing = parseLocal(props.startTime)
    const d = existing ? fmtDate(existing) : fmtDate(new Date())
    emit('update:startTime', d && v ? `${d}T${v}:00` : '')
  }
})

const localEndDate = computed({
  get() {
    const d = parseLocal(props.endTime)
    return d ? fmtDate(d) : ''
  },
  set(v) {
    const existing = parseLocal(props.endTime)
    const h = existing ? fmtTime(existing) : '09:00'
    emit('update:endTime', v ? `${v}T${h}:00` : '')
  }
})

const localEndTime = computed({
  get() {
    const d = parseLocal(props.endTime)
    return d ? fmtTime(d) : ''
  },
  set(v) {
    const existing = parseLocal(props.endTime)
    const d = existing ? fmtDate(existing) : localStartDate.value || fmtDate(new Date())
    emit('update:endTime', d && v ? `${d}T${v}:00` : '')
  }
})

const todayStr = computed(() => fmtDate(new Date()))

// ─── Parsed values ─────────────────────────────────────────────────────────────
const localStartTimeValue = computed(() => parseLocal(props.startTime))
const localEndTimeValue = computed(() => parseLocal(props.endTime))

// ─── Minimum end time constraint ──────────────────────────────────────────────
// Khi endDate == startDate → endTime không được nhỏ hơn startTime + duration
const minEndTime = computed(() => {
  if (localMode.value !== 'scheduled') return ''
  const start = localStartTimeValue.value
  const endDate = localEndDate.value
  const startDate = localStartDate.value

  if (!start || !endDate || endDate !== startDate) return ''

  // endDate == startDate → end phải >= start + duration
  const minEnd = new Date(start.getTime() + localDuration.value * 60000)
  return fmtTime(minEnd)
})

// ─── Validation ────────────────────────────────────────────────────────────────
const scheduleError = computed(() => {
  if (localMode.value !== 'scheduled') return ''
  const start = localStartTimeValue.value
  const end = localEndTimeValue.value
  if (!start || !end) return ''
  if (end <= start) return 'Thời gian kết thúc phải sau thời gian bắt đầu.'
  const windowMin = Math.round((end - start) / 60000)
  if (windowMin < localDuration.value) {
    return `Cửa sổ thi (${windowMin} phút) nhỏ hơn thời lượng làm bài (${localDuration.value} phút).`
  }
  return ''
})

const schedulePreview = computed(() => {
  if (localMode.value === 'flexible') return 'Mở 24/7 — không giới hạn thời gian'
  const start = localStartTimeValue.value
  const end = localEndTimeValue.value
  if (!start || !end) return ''
  if (end <= start) return ''
  const opts = { day: '2-digit', month: '2-digit', hour: '2-digit', minute: '2-digit' }
  return `${start.toLocaleString('vi-VN', opts)} → ${end.toLocaleString('vi-VN', opts)}`
})

const durationDiff = computed(() => {
  if (localMode.value !== 'scheduled') return ''
  const start = localStartTimeValue.value
  const end = localEndTimeValue.value
  if (!start || !end || end <= start) return ''
  const diffMin = Math.round((end - start) / 60000)
  if (diffMin < 60) return `${diffMin} phút`
  return `${Math.floor(diffMin / 60)}h ${diffMin % 60 > 0 ? (diffMin % 60) + 'p' : ''}`
})

// ─── Helpers ───────────────────────────────────────────────────────────────────
const addMins = (d, mins) => new Date(d.getTime() + mins * 60000)

// ─── Quick actions ─────────────────────────────────────────────────────────────
const setStartNow = () => {
  const now = new Date()
  emit('update:startTime', toIsoDefaultStart(now))
  // Reset end nếu end <= start (sau khi start đổi)
  ensureEndAfterConstraint()
}

const setStartIn = (mins) => {
  const base = localStartTimeValue.value || new Date()
  const nd = addMins(base, mins)
  emit('update:startTime', `${fmtDate(nd)}T${fmtTime(nd)}:00`)
  ensureEndAfterConstraint()
}

const setEndByDuration = () => {
  const start = localStartTimeValue.value
  if (!start) return
  const nd = addMins(start, localDuration.value)
  emit('update:endTime', `${fmtDate(nd)}T${fmtTime(nd)}:00`)
}

const setEndAfter = (mins) => {
  const start = localStartTimeValue.value
  if (!start) return
  const nd = addMins(start, mins)
  emit('update:endTime', `${fmtDate(nd)}T${fmtTime(nd)}:00`)
}

// ─── Auto-correct end time when start changes ─────────────────────────────────
/**
 * Sau khi start time thay đổi: nếu end hiện tại <= start + duration
 * thì tự động đẩy end về min hợp lệ.
 */
const ensureEndAfterConstraint = () => {
  const start = localStartTimeValue.value
  const end = localEndTimeValue.value
  if (!start || !end) return

  const minEnd = addMins(start, localDuration.value)
  if (end < minEnd) {
    emit('update:endTime', `${fmtDate(minEnd)}T${fmtTime(minEnd)}:00`)
  }
}

// ─── On input/change handlers ─────────────────────────────────────────────────
// Khi start date thay đổi
const onStartDateChanged = () => {
  closePicker(event)
  ensureEndAfterConstraint()
}

// Khi start time thay đổi
const onStartTimeChanged = () => {
  closePicker(event)
  ensureEndAfterConstraint()
}

// Khi end date thay đổi
const onEndDateChanged = () => {
  closePicker(event)
  const start = localStartTimeValue.value
  const end = localEndTimeValue.value
  if (!start || !end) return
  // Nếu đổi sang ngày khác → reset end time về min hợp lệ
  const minEnd = addMins(start, localDuration.value)
  if (end < minEnd) {
    emit('update:endTime', `${fmtDate(minEnd)}T${fmtTime(minEnd)}:00`)
  }
}

// Khi end time thay đổi
const onEndTimeChanged = () => {
  closePicker(event)
  const start = localStartTimeValue.value
  const end = localEndTimeValue.value
  if (!start || !end) return
  // Nếu end < min → tự đẩy về min
  const minEnd = addMins(start, localDuration.value)
  if (end < minEnd) {
    emit('update:endTime', `${fmtDate(minEnd)}T${fmtTime(minEnd)}:00`)
  }
}

// ─── Picker helpers ─────────────────────────────────────────────────────────────
const closePicker = (event) => {
  const target = event?.target
  if (!target) return
  window.setTimeout(() => target.blur(), 0)
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

/* Field */
.ec-field {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.ec-field__label {
  font-size: 0.8rem;
  font-weight: 700;
  color: var(--ds-text);
}

.dark .ec-field__label { color: #f1f5f9; }

/* Mode selector */
.ec-mode-group {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 0.625rem;
}

@media (max-width: 640px) {
  .ec-mode-group { grid-template-columns: 1fr; }
}

.ec-mode-btn {
  display: flex;
  align-items: center;
  gap: 0.625rem;
  padding: 0.75rem 1rem;
  border-radius: var(--ds-radius-xl);
  border: 1.5px solid var(--ds-border);
  background: var(--ds-gray-50);
  color: var(--ds-text-secondary);
  cursor: pointer;
  transition: color 0.15s ease, background-color 0.15s ease, border-color 0.15s ease, box-shadow 0.15s ease, transform 0.15s ease;
  text-align: left;
}

.dark .ec-mode-btn {
  background: var(--ds-gray-800);
  border-color: var(--ds-border-strong);
}

.ec-mode-btn:hover {
  border-color: var(--ds-primary-border);
  background: var(--ds-primary-soft);
  color: var(--ds-primary);
}

.ec-mode-btn--active {
  border-color: var(--ds-primary);
  background: var(--ds-primary);
  color: white;
  box-shadow: 0 4px 12px rgba(79, 70, 229, 0.25);
}

.ec-mode-btn--active .ec-mode-btn__desc { color: rgba(255,255,255,0.75); }
.ec-mode-btn--active:hover {
  background: var(--ds-primary);
  color: white;
}

.ec-mode-btn__text {
  display: flex;
  flex-direction: column;
  gap: 0.125rem;
  min-width: 0;
}

.ec-mode-btn__title {
  font-size: 0.8rem;
  font-weight: 700;
  line-height: 1.2;
}

.ec-mode-btn__desc {
  font-size: 0.65rem;
  font-weight: 500;
  opacity: 0.75;
  line-height: 1.3;
}

/* Schedule blocks */
.ec-sched-block {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  padding: 1.25rem;
  background: var(--ds-gray-50);
  border: 1.5px solid var(--ds-border);
  border-radius: var(--ds-radius-2xl);
  transition: border-color 0.15s ease;
}

.dark .ec-sched-block {
  background: var(--ds-gray-800);
  border-color: var(--ds-border-strong);
}

.ec-sched-block--error { border-color: var(--ds-danger); }
.ec-sched-block--info {
  background: var(--ds-primary-soft);
  border-color: var(--ds-primary-border);
}

.dark .ec-sched-block--info { background: rgba(79,70,229,0.1); }

.ec-sched-block__label {
  display: flex;
  align-items: flex-start;
  gap: 0.75rem;
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
  line-height: 1.4;
}

.ec-sched-block__label-hint {
  font-style: italic;
  opacity: 0.8;
}

.ec-sched-block__inputs {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  padding-left: calc(24px + 0.75rem);
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
  transition: color 0.15s ease, background-color 0.15s ease, border-color 0.15s ease, box-shadow 0.15s ease, transform 0.15s ease;
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
  transition: color 0.15s ease, background-color 0.15s ease, border-color 0.15s ease, box-shadow 0.15s ease, transform 0.15s ease;
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

/* Warning */
.ec-sched-warn {
  display: flex;
  align-items: flex-start;
  gap: 0.75rem;
  padding: 0.875rem 1.125rem;
  background: var(--ds-danger-soft);
  border: 1px solid rgba(220, 38, 38, 0.2);
  border-radius: var(--ds-radius-xl);
  color: var(--ds-danger);
}

.ec-sched-warn p {
  font-size: 0.8rem;
  font-weight: 600;
  margin: 0;
  line-height: 1.5;
  color: var(--ds-danger);
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

.ec-sched-preview__main {
  font-size: 0.875rem;
  font-weight: 700;
  color: var(--ds-success);
  margin: 0;
  line-height: 1.4;
}

.ec-sched-preview--error .ec-sched-preview__main { color: var(--ds-danger); }

.ec-sched-preview__sub {
  font-size: 0.75rem;
  color: var(--ds-text-secondary);
  margin: 0.25rem 0 0;
}
</style>