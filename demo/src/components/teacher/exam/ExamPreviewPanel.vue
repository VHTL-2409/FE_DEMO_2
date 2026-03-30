<template>
  <div class="epp">

    <!-- Header -->
    <div class="epp__header">
      <div class="epp__header-icon">
        <LucideIcon name="preview" />
      </div>
      <div>
        <h3 class="epp__header-title">Tổng quan đề thi</h3>
        <p class="epp__header-desc">Kiểm tra trạng thái hoàn thiện</p>
      </div>
    </div>

    <!-- Exam preview card -->
    <div class="epp__preview-card">
      <div class="epp__preview-title">
        <LucideIcon name="assignment" />
        <div>
          <p class="epp__preview-title-text">{{ form.title || '— Đề thi chưa có tên —' }}</p>
          <p v-if="form.subject || form.className" class="epp__preview-meta">
            {{ form.subject }}{{ form.subject && form.className ? ' · ' : '' }}{{ form.className }}
          </p>
        </div>
      </div>

      <!-- Mini stats -->
      <div class="epp__stats">
        <div class="epp__stat">
          <LucideIcon name="timer" />
          <div>
            <p class="epp__stat-val">{{ form.durationMinutes || 0 }} phút</p>
            <p class="epp__stat-label">Thời lượng</p>
          </div>
        </div>
        <div class="epp__stat">
          <LucideIcon name="quiz" />
          <div>
            <p class="epp__stat-val">{{ form.questions ? form.questions.length : 0 }}</p>
            <p class="epp__stat-label">Câu hỏi</p>
          </div>
        </div>
        <div class="epp__stat">
          <LucideIcon name="repeat" />
          <div>
            <p class="epp__stat-val">{{ form.maxAttempts || '∞' }}</p>
            <p class="epp__stat-label">Lần thi</p>
          </div>
        </div>
      </div>

      <!-- Schedule -->
      <div v-if="form.startTime || form.endTime" class="epp__schedule">
        <LucideIcon name="event" />
        <span>{{ scheduleSummary }}</span>
      </div>
    </div>

    <!-- Progress checklist -->
    <div class="epp__checklist">
      <h4 class="epp__checklist-title">
        <LucideIcon name="checklist" />
        Tiến độ hoàn thiện
      </h4>

      <div class="epp__checklist-items">
        <div
          v-for="item in checklist"
          :key="item.key"
          class="epp__check-item"
          :class="{ 'epp__check-item--done': item.done, 'epp__check-item--warn': item.warn }"
        >
          <span class="epp__check-icon">
            <LucideIcon name="check_circle" v-if="item.done" />
            <LucideIcon name="warning" v-else-if="item.warn"  epp__check-icon--warn/>
            <LucideIcon name="radio_button_unchecked" v-else />
          </span>
          <div class="epp__check-body">
            <p class="epp__check-label">{{ item.label }}</p>
            <p v-if="item.sub" class="epp__check-sub">{{ item.sub }}</p>
          </div>
        </div>
      </div>

      <!-- Progress bar -->
      <div class="epp__progress">
        <div class="epp__progress-bar">
          <div class="epp__progress-fill" :style="{ width: progressPct + '%' }" />
        </div>
        <p class="epp__progress-label">{{ doneCount }}/{{ totalCount }} hoàn tất</p>
      </div>
    </div>

    <!-- Quick tips -->
    <div class="epp__tips">
      <h4 class="epp__tips-title">
        <LucideIcon name="tips_and_updates" />
        Mẹo nhanh
      </h4>
      <ul class="epp__tips-list">
        <li v-for="tip in tips" :key="tip">{{ tip }}</li>
      </ul>
    </div>

  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  form: { type: Object, default: () => ({}) }
})

const checklist = computed(() => [
  {
    key: 'info',
    label: 'Thông tin đề thi',
    done: !!(props.form.title && props.form.subject),
    warn: !props.form.title && !!props.form.subject,
    sub: props.form.title ? props.form.title : 'Cần có tên đề và môn học'
  },
  {
    key: 'config',
    label: 'Cấu hình thi',
    done: !!(props.form.durationMinutes && props.form.durationMinutes > 0),
    sub: props.form.durationMinutes ? `Thời lượng ${props.form.durationMinutes} phút` : 'Thiết lập thời lượng thi'
  },
  {
    key: 'questions',
    label: 'Câu hỏi',
    done: props.form.questions && props.form.questions.length > 0,
    warn: false,
    sub: props.form.questions && props.form.questions.length > 0
      ? `${props.form.questions.length} câu hỏi đã thêm`
      : 'Thêm ít nhất 1 câu hỏi'
  },
  {
    key: 'schedule',
    label: 'Lịch thi',
    done: !!(props.form.startTime && props.form.endTime),
    warn: !!(props.form.startTime || props.form.endTime) && !(props.form.startTime && props.form.endTime),
    sub: props.form.startTime && props.form.endTime ? 'Đã thiết lập thời gian' : 'Cần đặt thời gian bắt đầu và kết thúc'
  },
  {
    key: 'publish',
    label: 'Sẵn sàng xuất bản',
    done: !!(props.form.title && props.form.subject && props.form.questions && props.form.questions.length > 0 && props.form.startTime && props.form.endTime),
    warn: false,
    sub: computed(() => props.form.title && props.form.subject && props.form.questions && props.form.questions.length > 0 && props.form.startTime && props.form.endTime ? 'Có thể xuất bản ngay' : 'Hoàn tất các bước trên trước').value
  }
])

const totalCount = computed(() => checklist.value.length)
const doneCount = computed(() => checklist.value.filter(c => c.done).length)
const progressPct = computed(() => Math.round((doneCount.value / totalCount.value) * 100))

const scheduleSummary = computed(() => {
  if (!props.form.startTime && !props.form.endTime) return ''
  const opts = { day: '2-digit', month: '2-digit', hour: '2-digit', minute: '2-digit' }
  const parts = []
  if (props.form.startTime) {
    try {
      parts.push(new Date(props.form.startTime).toLocaleString('vi-VN', opts))
    } catch { parts.push('...') }
  }
  if (props.form.endTime) {
    try {
      parts.push('→ ' + new Date(props.form.endTime).toLocaleString('vi-VN', opts))
    } catch {}
  }
  return parts.join(' ')
})

const tips = [
  'Đặt thời gian thi trước ít nhất 1 giờ để học sinh chuẩn bị.',
  'Bật giám sát để giảm thiểu gian lện trong kỳ thi trực tuyến.',
  'Xáo trộn câu hỏi giúp mỗi học sinh có đề thi khác nhau.',
  'Lưu nháp trước khi xuất bản để tránh mất dữ liệu.',
]
</script>


<style scoped>
.epp {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.epp__header {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.epp__header-icon {
  width: 36px;
  height: 36px;
  border-radius: var(--ds-radius-lg);
  background: var(--ds-primary-soft);
  color: var(--ds-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}


.epp__header-title {
  font-family: var(--ds-font-display);
  font-size: 0.9rem;
  font-weight: 700;
  color: var(--ds-text);
  margin: 0;
  line-height: 1.2;
}

.dark .epp__header-title { color: #f1f5f9; }

.epp__header-desc {
  font-size: 0.7rem;
  color: var(--ds-text-muted);
  margin: 0.25rem 0 0;
}

/* Preview card */
.epp__preview-card {
  padding: 1.25rem;
  background: linear-gradient(135deg, var(--ds-primary-soft) 0%, var(--ds-info-soft) 100%);
  border: 1px solid var(--ds-primary-border);
  border-radius: var(--ds-radius-2xl);
  display: flex;
  flex-direction: column;
  gap: 0.875rem;
}

.epp__preview-title {
  display: flex;
  align-items: flex-start;
  gap: 0.75rem;
}

.epp__preview-icon {
  font-size: 1.5rem;
  color: var(--ds-primary);
  flex-shrink: 0;
  margin-top: 0.125rem;
}

.epp__preview-title-text {
  font-size: 0.875rem;
  font-weight: 700;
  color: var(--ds-text);
  margin: 0;
  line-height: 1.3;
}

.dark .epp__preview-title-text { color: #f1f5f9; }

.epp__preview-meta {
  font-size: 0.75rem;
  color: var(--ds-text-secondary);
  margin: 0.25rem 0 0;
}

.epp__stats {
  display: flex;
  gap: 0.75rem;
}

.epp__stat {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  flex: 1;
  min-width: 0;
}

.epp__stat-icon {
  font-size: 1rem;
  color: var(--ds-primary);
  flex-shrink: 0;
}

.epp__stat-val {
  font-size: 0.8rem;
  font-weight: 800;
  color: var(--ds-text);
  margin: 0;
  white-space: nowrap;
}

.dark .epp__stat-val { color: #f1f5f9; }

.epp__stat-label {
  font-size: 0.65rem;
  color: var(--ds-text-muted);
  margin: 0;
}

.epp__schedule {
  display: flex;
  align-items: flex-start;
  gap: 0.5rem;
  font-size: 0.75rem;
  color: var(--ds-text-secondary);
  line-height: 1.4;
}

.epp__schedule-icon {
  font-size: 1rem;
  color: var(--ds-primary);
  flex-shrink: 0;
  margin-top: 0.125rem;
}

/* Checklist */
.epp__checklist {
  padding: 1.125rem;
  background: var(--ds-surface);
  border: 1px solid var(--ds-border);
  border-radius: var(--ds-radius-2xl);
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.dark .epp__checklist {
  background: var(--ds-gray-800);
  border-color: var(--ds-border-strong);
}

.epp__checklist-title {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.8rem;
  font-weight: 700;
  color: var(--ds-text-secondary);
  text-transform: uppercase;
  letter-spacing: 0.08em;
  margin: 0;
}


.epp__checklist-items {
  display: flex;
  flex-direction: column;
  gap: 0.375rem;
}

.epp__check-item {
  display: flex;
  align-items: flex-start;
  gap: 0.625rem;
  padding: 0.625rem 0.75rem;
  border-radius: var(--ds-radius-lg);
  background: var(--ds-gray-50);
  border: 1px solid var(--ds-border);
  transition: all 0.15s ease;
}

.dark .epp__check-item {
  background: var(--ds-gray-700);
  border-color: var(--ds-border-strong);
}

.epp__check-item--done {
  background: var(--ds-success-soft);
  border-color: rgba(22, 163, 74, 0.2);
}

.epp__check-item--warn {
  background: var(--ds-warning-soft);
  border-color: rgba(234, 179, 8, 0.2);
}

.epp__check-icon {
  font-size: 1rem;
  color: var(--ds-gray-400);
  flex-shrink: 0;
  margin-top: 0.125rem;
}

.epp__check-icon--warn { color: var(--ds-warning); }

.epp__check-item--done .epp__check-icon { color: var(--ds-success); }

.epp__check-body { flex: 1; min-width: 0; }

.epp__check-label {
  font-size: 0.8rem;
  font-weight: 700;
  color: var(--ds-text);
  margin: 0;
  line-height: 1.2;
}

.dark .epp__check-label { color: #f1f5f9; }

.epp__check-sub {
  font-size: 0.7rem;
  color: var(--ds-text-muted);
  margin: 0.25rem 0 0;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.epp__check-item--done .epp__check-sub { color: var(--ds-success); }
.epp__check-item--warn .epp__check-sub { color: var(--ds-warning); }

/* Progress */
.epp__progress {
  display: flex;
  flex-direction: column;
  gap: 0.375rem;
}

.epp__progress-bar {
  height: 6px;
  background: var(--ds-gray-200);
  border-radius: 3px;
  overflow: hidden;
}

.dark .epp__progress-bar { background: var(--ds-gray-600); }

.epp__progress-fill {
  height: 100%;
  background: linear-gradient(90deg, var(--ds-primary) 0%, var(--ds-success) 100%);
  border-radius: 3px;
  transition: width 0.3s ease;
}

.epp__progress-label {
  font-size: 0.7rem;
  font-weight: 700;
  color: var(--ds-text-muted);
  text-align: right;
  margin: 0;
}

/* Tips */
.epp__tips {
  padding: 1rem 1.125rem;
  background: var(--ds-gray-50);
  border: 1px solid var(--ds-border);
  border-radius: var(--ds-radius-2xl);
}

.dark .epp__tips {
  background: var(--ds-gray-800);
  border-color: var(--ds-border-strong);
}

.epp__tips-title {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.75rem;
  font-weight: 700;
  color: var(--ds-text-secondary);
  text-transform: uppercase;
  letter-spacing: 0.08em;
  margin: 0 0 0.625rem;
}


.epp__tips-list {
  margin: 0;
  padding: 0 0 0 1.25rem;
  display: flex;
  flex-direction: column;
  gap: 0.375rem;
}

.epp__tips-list li {
  font-size: 0.75rem;
  color: var(--ds-text-muted);
  line-height: 1.5;
}
</style>
