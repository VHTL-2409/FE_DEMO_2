<template>
  <div class="esp">

    <!-- Status Timeline -->
    <div class="esp__section">
      <h4 class="esp__section-title">
        <LucideIcon name="timeline" />
        Trạng thái
      </h4>
      <ExamStatusTimeline :exam="exam" />
    </div>

    <!-- Stats grid -->
    <div class="esp__stats-grid">
      <div class="esp__stat-card" :class="{ 'esp__stat-card--highlight': exam?.questionCount > 0 }">
        <LucideIcon name="quiz" />
        <div class="esp__stat-body">
          <p class="esp__stat-val">{{ exam?.questionCount || 0 }}</p>
          <p class="esp__stat-label">Câu hỏi</p>
        </div>
      </div>
      <div class="esp__stat-card">
        <LucideIcon name="timer" />
        <div class="esp__stat-body">
          <p class="esp__stat-val">{{ exam?.durationMinutes || '—' }}</p>
          <p class="esp__stat-label">Phút thi</p>
        </div>
      </div>
      <div class="esp__stat-card">
        <LucideIcon name="group" />
        <div class="esp__stat-body">
          <p class="esp__stat-val">{{ exam?.participantCount || 0 }}</p>
          <p class="esp__stat-label">Học sinh</p>
        </div>
      </div>
      <div class="esp__stat-card">
        <LucideIcon name="repeat" />
        <div class="esp__stat-body">
          <p class="esp__stat-val">{{ exam?.maxAttempts || '∞' }}</p>
          <p class="esp__stat-label">Lần thi</p>
        </div>
      </div>
    </div>

    <!-- Checklist + Progress -->
    <div v-if="checklistItems.length > 0" class="esp__section">
      <h4 class="esp__section-title">
        <LucideIcon name="checklist" />
        Hoàn thiện
        <span class="esp__progress-label">{{ checklistDone }}/{{ checklistItems.length }}</span>
      </h4>

      <!-- Progress bar -->
      <div class="esp__progress-bar">
        <div
          class="esp__progress-fill"
          :class="{
            'esp__progress-fill--warning': checklistDone < checklistItems.length,
            'esp__progress-fill--success': checklistDone === checklistItems.length
          }"
          :style="{ width: progressPercent + '%' }"
        />
      </div>

      <div class="esp__checklist">
        <div
          v-for="item in checklistItems"
          :key="item.key"
          class="esp__check-item"
          :class="{
            'esp__check-item--done': item.done,
            'esp__check-item--warn': item.warn
          }"
          @click="item.action && item.action()"
          :style="item.action ? 'cursor:pointer' : ''"
        >
          <span class="esp__check-icon"><LucideIcon :name="item.done ? 'check_circle' : item.warn ? 'alert_triangle' : 'circle'" /></span>
          <span class="esp__check-label">{{ item.label }}</span>
          <span v-if="item.action && !item.done" class="esp__check-arrow">
            <LucideIcon name="chevron_right" />
          </span>
        </div>
      </div>
    </div>

    <!-- Schedule info -->
    <div class="esp__section">
      <h4 class="esp__section-title">
        <LucideIcon name="schedule" />
        Thời gian thi
      </h4>
      <div v-if="exam?.startTime || exam?.endTime" class="esp__schedule">
        <div class="esp__schedule-item">
          <span class="esp__schedule-label">Bắt đầu</span>
          <span class="esp__schedule-val">{{ formatDateTime(exam?.startTime) }}</span>
        </div>
        <div class="esp__schedule-item">
          <span class="esp__schedule-label">Kết thúc</span>
          <span class="esp__schedule-val">{{ formatDateTime(exam?.endTime) }}</span>
        </div>
      </div>
      <div v-else class="esp__empty-hint">
        <LucideIcon name="event_busy" />
        Chưa thiết lập lịch thi
      </div>
    </div>

    <!-- Proctoring status -->
    <div class="esp__section">
      <h4 class="esp__section-title">
        <LucideIcon name="shield" />
        Giám sát
      </h4>
      <div class="esp__proctor-badge" :class="isProctoringEnabled ? 'esp__proctor-badge--on' : 'esp__proctor-badge--off'">
        <LucideIcon :name="isProctoringEnabled ? 'verified_user' : 'shield_off'" />
        {{ isProctoringEnabled ? 'Đã bật giám sát' : 'Tắt giám sát' }}
      </div>
      <div v-if="isProctoringEnabled && activeRules.length > 0" class="esp__rules">
        <span
          v-for="rule in activeRules"
          :key="rule"
          class="esp__rule-tag"
        >
          <LucideIcon :name="proctorIcons[rule] || 'check'" />
          {{ proctorLabels[rule] || rule }}
        </span>
      </div>
    </div>

    <!-- Warnings -->
    <div v-if="warnings.length > 0" class="esp__section">
      <h4 class="esp__section-title">
        <LucideIcon name="warning" />
        Cảnh báo
      </h4>
      <div class="esp__warnings">
        <div v-for="w in warnings" :key="w.id" class="esp__warning" :class="`esp__warning--${w.severity}`">
          <LucideIcon :name="w.icon" />
          <span>{{ w.message }}</span>
        </div>
      </div>
    </div>

    <!-- Quick actions -->
    <div class="esp__section">
      <h4 class="esp__section-title">
        <LucideIcon name="bolt" />
        Thao tác nhanh
      </h4>
      <div class="esp__quick-actions">
        <button type="button" class="esp__quick-btn" @click="$emit('tab-click', 'overview')">
          <LucideIcon name="info" />
          Thông tin
        </button>
        <button type="button" class="esp__quick-btn" @click="$emit('tab-click', 'questions')">
          <LucideIcon name="quiz" />
          Câu hỏi
          <span v-if="exam?.questionCount" class="esp__quick-badge">{{ exam.questionCount }}</span>
        </button>
        <button type="button" class="esp__quick-btn" @click="$emit('tab-click', 'schedule')">
          <LucideIcon name="schedule" />
          Lịch thi
        </button>
        <button type="button" class="esp__quick-btn" @click="$emit('tab-click', 'proctoring')">
          <LucideIcon name="shield" />
          Giám sát
        </button>
        <button v-if="exam?.participantCount > 0" type="button" class="esp__quick-btn" @click="$emit('tab-click', 'results')">
          <LucideIcon name="assessment" />
          Kết quả
        </button>
      </div>
    </div>

  </div>
</template>

<script setup>
import { computed } from 'vue'
import ExamStatusTimeline from './ExamStatusTimeline.vue'

const props = defineProps({
  exam: { type: Object, default: null }
})

const emit = defineEmits(['tab-click'])

const isProctoringEnabled = computed(() => {
  if (!props.exam) return false
  return props.exam.monitorTabSwitch || props.exam.monitorBlur || props.exam.monitorDevtools
})

const proctorLabels = {
  monitorTabSwitch: 'Theo dõi tab',
  monitorBlur: 'Blur focus',
  monitorExitFullscreen: 'Thoát toàn màn hình',
  monitorCopyPaste: 'Copy/Paste',
  monitorIdleTime: 'Nhàn rỗi',
  monitorDevtools: 'DevTools',
  monitorDuplicateIp: "IP trùng lặp",
  monitorFastSubmit: 'Nộp nhanh',
  monitorRightClick: 'Chuột phải',
  monitorPrintScreen: 'Chụp màn hình',
  monitorRapidQuestionSwitch: 'Đổi câu',
  monitorMultiMonitor: 'Đa màn hình',
  requireCameraMic: 'Camera/Mic'
}

const proctorIcons = {
  monitorTabSwitch: 'tab',
  monitorBlur: 'visibility_off',
  monitorExitFullscreen: 'fullscreen_exit',
  monitorCopyPaste: 'content_copy',
  monitorIdleTime: 'timer_off',
  monitorDevtools: 'code',
  monitorDuplicateIp: 'router',
  monitorFastSubmit: 'bolt',
  monitorRightClick: 'mouse',
  monitorPrintScreen: 'screenshot',
  monitorRapidQuestionSwitch: 'skip_next',
  monitorMultiMonitor: 'desktop_windows',
  requireCameraMic: 'videocam'
}

const activeRules = computed(() => {
  if (!props.exam) return []
  const keys = Object.keys(proctorLabels)
  return keys.filter(k => props.exam[k])
})

const warnings = computed(() => {
  const w = []
  if (props.exam) {
    if ((props.exam.questionCount || 0) === 0) {
      w.push({ id: 'no-questions', message: 'Chưa có câu hỏi nào', icon: 'quiz', severity: 'danger' })
    }
    if (!props.exam.startTime || !props.exam.endTime) {
      w.push({ id: 'no-schedule', message: 'Chưa thiết lập lịch thi', icon: 'schedule', severity: 'warning' })
    }
    if (!props.exam.title || props.exam.title.trim().length < 3) {
      w.push({ id: 'no-title', message: 'Tiêu đề quá ngắn', icon: 'edit_note', severity: 'warning' })
    }
    if (!props.exam.durationMinutes) {
      w.push({ id: 'no-duration', message: 'Chưa thiết lập thời lượng', icon: 'timer_off', severity: 'danger' })
    }
  }
  return w
})

const checklistItems = computed(() => {
  if (!props.exam) return []

  const items = []

  // Title
  const hasTitle = !!props.exam.title && props.exam.title.trim().length >= 3
  items.push({
    key: 'title',
    label: 'Có tiêu đề',
    done: hasTitle,
    warn: !hasTitle,
    action: !hasTitle ? () => emit('tab-click', 'overview') : null
  })

  // Questions
  const hasQuestions = (props.exam.questionCount || 0) > 0
  items.push({
    key: 'questions',
    label: 'Có câu hỏi',
    done: hasQuestions,
    warn: !hasQuestions,
    action: !hasQuestions ? () => emit('tab-click', 'questions') : null
  })

  // Schedule
  const hasSchedule = !!props.exam.startTime && !!props.exam.endTime
  items.push({
    key: 'schedule',
    label: 'Có lịch thi',
    done: hasSchedule,
    warn: !hasSchedule,
    action: !hasSchedule ? () => emit('tab-click', 'schedule') : null
  })

  // Published
  items.push({
    key: 'active',
    label: 'Đã xuất bản',
    done: !!props.exam.isActive,
    warn: false,
    action: !props.exam.isActive ? () => null : null
  })

  return items
})

const checklistDone = computed(() => checklistItems.value.filter(i => i.done).length)
const progressPercent = computed(() => {
  if (!checklistItems.value.length) return 0
  return Math.round((checklistDone.value / checklistItems.value.length) * 100)
})

const formatDateTime = (d) => {
  if (!d) return '—'
  try {
    return new Date(d).toLocaleString('vi-VN', {
      day: '2-digit', month: '2-digit', year: 'numeric',
      hour: '2-digit', minute: '2-digit'
    })
  } catch { return '—' }
}
</script>


<style scoped>
.esp {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

/* Section */
.esp__section {
  padding: 1rem;
  background: var(--ds-surface);
  border: 1px solid var(--ds-border);
  border-radius: var(--ds-radius-2xl);
}

.dark .esp__section {
  background: var(--ds-gray-800);
  border-color: var(--ds-border-strong);
}

.esp__section-title {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.75rem;
  font-weight: 700;
  color: var(--ds-text-secondary);
  text-transform: uppercase;
  letter-spacing: 0.08em;
  margin: 0 0 0.75rem;
}


/* Stats */
.esp__stats-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0.5rem;
}

.esp__stat-card {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem;
  background: var(--ds-gray-50);
  border: 1px solid var(--ds-border);
  border-radius: var(--ds-radius-xl);
  transition: all 0.15s ease;
}

.dark .esp__stat-card {
  background: var(--ds-gray-800);
  border-color: var(--ds-border-strong);
}

.esp__stat-card--highlight {
  border-color: var(--ds-primary-border);
  background: var(--ds-primary-soft);
}

.esp__stat-icon {
  font-size: 1.25rem;
  color: var(--ds-primary);
  flex-shrink: 0;
}

.esp__stat-body {
  flex: 1;
  min-width: 0;
}

.esp__stat-val {
  font-size: 1.125rem;
  font-weight: 800;
  color: var(--ds-text);
  margin: 0;
  line-height: 1;
  font-family: var(--ds-font-display);
}

.dark .esp__stat-val { color: #f1f5f9; }

.esp__stat-label {
  font-size: 0.65rem;
  color: var(--ds-text-muted);
  margin: 0.25rem 0 0;
  font-weight: 600;
}

/* Checklist */
.esp__progress-label {
  margin-left: auto;
  font-size: 0.7rem;
  font-weight: 700;
  color: var(--ds-primary);
  background: var(--ds-primary-soft);
  padding: 0.1rem 0.5rem;
  border-radius: var(--ds-radius-full);
}

.esp__progress-bar {
  height: 6px;
  background: var(--ds-gray-200);
  border-radius: 3px;
  overflow: hidden;
  margin-bottom: 0.75rem;
}

.dark .esp__progress-bar { background: var(--ds-gray-700); }

.esp__progress-fill {
  height: 100%;
  border-radius: 3px;
  transition: width 0.4s ease;
}

.esp__progress-fill--warning { background: var(--ds-warning); }
.esp__progress-fill--success { background: var(--ds-success); }

.esp__checklist {
  display: flex;
  flex-direction: column;
  gap: 0.375rem;
}

.esp__check-item {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 0.625rem;
  border-radius: var(--ds-radius-lg);
  font-size: 0.8rem;
  font-weight: 600;
  background: var(--ds-gray-50);
  color: var(--ds-text-muted);
  transition: all 0.12s ease;
}

.dark .esp__check-item { background: var(--ds-gray-700); }

.esp__check-item--done {
  background: var(--ds-success-soft);
  color: var(--ds-success);
}

.esp__check-item--warn {
  background: var(--ds-warning-soft);
  color: var(--ds-warning);
}


.esp__check-label {
  flex: 1;
}

.esp__check-arrow {
  margin-left: auto;
  opacity: 0.5;
}


/* Schedule */
.esp__schedule {
  display: flex;
  flex-direction: column;
  gap: 0.375rem;
}

.esp__schedule-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 0.5rem;
}

.esp__schedule-label {
  font-size: 0.75rem;
  color: var(--ds-text-muted);
  font-weight: 600;
}

.esp__schedule-val {
  font-size: 0.8rem;
  font-weight: 700;
  color: var(--ds-text);
  text-align: right;
}

.dark .esp__schedule-val { color: #f1f5f9; }

.esp__empty-hint {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.8rem;
  color: var(--ds-text-muted);
  font-style: italic;
}


/* Proctoring */
.esp__proctor-badge {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.375rem 0.75rem;
  border-radius: var(--ds-radius-full);
  font-size: 0.8rem;
  font-weight: 700;
  margin-bottom: 0.5rem;
}

.esp__proctor-badge--on {
  background: var(--ds-success-soft);
  color: var(--ds-success);
}

.esp__proctor-badge--off {
  background: var(--ds-gray-100);
  color: var(--ds-text-muted);
}

.dark .esp__proctor-badge--off { background: var(--ds-gray-700); }


.esp__rules {
  display: flex;
  flex-wrap: wrap;
  gap: 0.375rem;
}

.esp__rule-tag {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  padding: 0.25rem 0.5rem;
  background: var(--ds-gray-100);
  border-radius: var(--ds-radius-full);
  font-size: 0.7rem;
  font-weight: 600;
  color: var(--ds-text-secondary);
}

.dark .esp__rule-tag { background: var(--ds-gray-700); }


/* Warnings */
.esp__warnings {
  display: flex;
  flex-direction: column;
  gap: 0.375rem;
}

.esp__warning {
  display: flex;
  align-items: flex-start;
  gap: 0.5rem;
  padding: 0.5rem 0.75rem;
  border-radius: var(--ds-radius-lg);
  font-size: 0.8rem;
  font-weight: 600;
  line-height: 1.4;
}


.esp__warning--danger {
  background: var(--ds-danger-soft);
  color: var(--ds-danger);
}

.esp__warning--warning {
  background: var(--ds-warning-soft);
  color: var(--ds-warning);
}

.esp__warning--info {
  background: var(--ds-info-soft);
  color: var(--ds-info);
}

/* Quick actions */
.esp__quick-actions {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.esp__quick-btn {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 0.625rem;
  border: none;
  border-radius: var(--ds-radius-lg);
  background: transparent;
  color: var(--ds-text-secondary);
  font-size: 0.8rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.12s ease;
  text-align: left;
  font-family: inherit;
}

.dark .esp__quick-btn { color: #94a3b8; }

.esp__quick-btn:hover {
  background: var(--ds-gray-50);
  color: var(--ds-primary);
}

.dark .esp__quick-btn:hover { background: var(--ds-gray-700); }


.esp__quick-btn:hover

.esp__quick-badge {
  margin-left: auto;
  background: var(--ds-primary-soft);
  color: var(--ds-primary);
  font-size: 0.65rem;
  font-weight: 800;
  padding: 0.1rem 0.5rem;
  border-radius: var(--ds-radius-full);
}
</style>
