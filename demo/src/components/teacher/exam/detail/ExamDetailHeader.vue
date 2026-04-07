<template>
  <div class="ed-header">
    <!-- Left: breadcrumb + title block -->
    <div class="ed-header__left">
      <div class="ed-header__breadcrumb">
          <RouterLink class="ed-header__bc-link" to="/teacher/exams/list">
          <LucideIcon name="assignment" />
          Đề thi
        </RouterLink>
        <LucideIcon name="chevron_right" />
        <span class="ed-header__bc-current">{{ exam?.title || 'Chi tiết đề thi' }}</span>
      </div>

      <div class="ed-header__title-row">
        <h1 class="ed-header__title">{{ exam?.title || 'Đang tải...' }}</h1>
        <ExamStatusChip
          v-if="exam"
          :status="examStatus"
          :is-archived="exam.isArchived"
        />
        <span v-if="exam?.isArchived" class="ed-header__archived-badge">
          <LucideIcon name="archive" />
          Đã lưu trữ
        </span>
      </div>

      <p v-if="exam" class="ed-header__meta">
        <button
          v-if="exam.code"
          type="button"
          class="ed-header__code-btn"
          title="Click để copy mã"
          @click="copyCode"
        >
          <LucideIcon name="tag" />
          {{ exam.code }}
          <LucideIcon :name="copied ? 'check' : 'content_copy'" />
        </button>

        <span v-if="exam.className" class="ed-header__meta-item">
          <LucideIcon name="school" />
          {{ exam.className }}
        </span>

        <span class="ed-header__meta-item">
          <LucideIcon name="person" />
          {{ exam.createdBy || '—' }}
        </span>

        <span class="ed-header__meta-item ed-header__meta-item--updated">
          <LucideIcon name="update" />
          {{ formatDate(exam.updatedAt) }}
        </span>
      </p>
    </div>

    <!-- Right: actions -->
    <div class="ed-header__actions">
      <!-- Save status -->
      <div class="ed-header__save-status" :class="saveStatusClass">
        <LucideIcon :name="saveStatusIcon" />
        <span>{{ saveStatusLabel }}</span>
      </div>

      <button
        type="button"
        class="ed-btn ed-btn--ghost"
        @click="handlePreview"
      >
        <LucideIcon name="preview" />
        Xem trước
      </button>

      <button
        type="button"
        class="ed-btn ed-btn--ghost"
        :disabled="saveLoading"
        @click="handleSave"
      >
        <LucideIcon name="progress_activity" v-if="saveLoading"  ed-spin/>
        <LucideIcon name="save" v-else />
        {{ saveLoading ? 'Đang lưu...' : 'Lưu' }}
      </button>

      <PublishControls
        v-if="exam"
        :exam="exam"
        @publish="handlePublish"
        @unpublish="handleUnpublish"
        @archive="handleArchive"
        @unarchive="handleUnarchive"
        @duplicate="handleDuplicate"
        @delete="openDeleteModal = true"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import ExamStatusChip from '../list/ExamStatusChip.vue'
import PublishControls from './PublishControls.vue'

const props = defineProps({
  exam: { type: Object, default: null },
  saveState: { type: String, default: 'idle' },
  saveLoading: { type: Boolean, default: false }
})

const emit = defineEmits([
  'save', 'publish', 'unpublish', 'archive', 'unarchive',
  'duplicate', 'delete', 'preview', 'go-back'
])

const copied = ref(false)

const examStatus = computed(() => {
  if (!props.exam) return 'unknown'
  if (props.exam.isArchived) return 'archived'
  if (!props.exam.isActive) return 'draft'
  const now = new Date()
  const start = props.exam.startTime ? new Date(props.exam.startTime) : null
  const end = props.exam.endTime ? new Date(props.exam.endTime) : null
  if (start && now < start) return 'upcoming'
  if ((!end || now <= end) && (!start || now >= start)) return 'live'
  return 'ended'
})

const saveStatusLabel = computed(() => {
  if (props.saveState === 'saving') return 'Đang lưu...'
  if (props.saveState === 'saved') return 'Đã lưu'
  if (props.saveState === 'error') return 'Lỗi lưu'
  return 'Chưa lưu'
})

const saveStatusIcon = computed(() => {
  if (props.saveState === 'saving') return 'progress_activity'
  if (props.saveState === 'saved') return 'check_circle'
  if (props.saveState === 'error') return 'error'
  return 'radio_button_unchecked'
})

const saveStatusClass = computed(() => ({
  'ed-header__save-status--saving': props.saveState === 'saving',
  'ed-header__save-status--saved': props.saveState === 'saved',
  'ed-header__save-status--error': props.saveState === 'error'
}))

const formatDate = (d) => {
  if (!d) return '—'
  try {
    return new Date(d).toLocaleString('vi-VN', {
      day: '2-digit', month: '2-digit', year: 'numeric',
      hour: '2-digit', minute: '2-digit'
    })
  } catch { return '—' }
}

const copyCode = async () => {
  if (!props.exam?.code) return
  try {
    await navigator.clipboard.writeText(props.exam.code)
    copied.value = true
    setTimeout(() => { copied.value = false }, 2000)
  } catch { /* fallback */ }
}

const handleSave = () => emit('save')
const handlePublish = () => emit('publish')
const handleUnpublish = () => emit('unpublish')
const handleArchive = () => emit('archive')
const handleUnarchive = () => emit('unarchive')
const handleDuplicate = () => emit('duplicate')
const handlePreview = () => emit('preview')
</script>

<style scoped>
.ed-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1.5rem;
  flex-wrap: wrap;
  padding: 1.25rem 1.5rem;
  background: rgba(255, 255, 255, 0.98);
  border-bottom: 1px solid var(--ds-border);
  backdrop-filter: blur(12px);
  position: sticky;
  top: 0;
  z-index: 100;
}

.dark .ed-header {
  background: rgba(30, 41, 59, 0.98);
  border-bottom-color: var(--ds-border-strong);
}

.ed-header__left {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  flex: 1;
  min-width: 0;
}

.ed-header__breadcrumb {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  font-size: 0.8rem;
  color: var(--ds-text-muted);
}

.ed-header__bc-link {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  color: var(--ds-text-muted);
  text-decoration: none;
  transition: color 0.15s ease;
}

.ed-header__bc-link:hover { color: var(--ds-primary); }

.ed-header__bc-sep {
  font-size: 0.875rem;
  opacity: 0.5;
}

.ed-header__bc-current {
  font-weight: 600;
  color: var(--ds-text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 300px;
}

.ed-header__title-row {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.ed-header__title {
  font-family: var(--ds-font-display);
  font-size: 1.375rem;
  font-weight: 800;
  color: var(--ds-text);
  margin: 0;
  line-height: 1.2;
}

.dark .ed-header__title { color: #f1f5f9; }

.ed-header__archived-badge {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  padding: 0.25rem 0.625rem;
  border-radius: var(--ds-radius-full);
  background: rgba(100, 116, 139, 0.1);
  border: 1px solid rgba(100, 116, 139, 0.2);
  color: var(--ds-gray-500);
  font-size: 0.7rem;
  font-weight: 700;
}


.ed-header__meta {
  display: flex;
  align-items: center;
  gap: 1rem;
  flex-wrap: wrap;
  font-size: 0.75rem;
  color: var(--ds-text-muted);
  margin: 0;
}

.ed-header__code-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  font-weight: 700;
  font-size: 0.75rem;
  color: var(--ds-primary);
  background: var(--ds-primary-soft);
  border: 1px solid var(--ds-primary-border);
  border-radius: var(--ds-radius-full);
  padding: 0.2rem 0.625rem;
  cursor: pointer;
  transition: color 0.15s ease, background-color 0.15s ease, border-color 0.15s ease, box-shadow 0.15s ease, transform 0.15s ease;
  font-family: inherit;
}

.ed-header__code-btn:hover {
  background: var(--ds-primary);
  color: white;
}

.ed-header__copy-icon { font-size: 0.75rem !important; opacity: 0.7; }
.ed-header__code-btn:hover .ed-header__copy-icon { opacity: 1; }

.ed-header__meta-item {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  font-weight: 600;
}


.ed-header__actions {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex-wrap: wrap;
  flex-shrink: 0;
}

/* Buttons */
.ed-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 1rem;
  border-radius: var(--ds-radius-xl);
  font-size: 0.8rem;
  font-weight: 700;
  cursor: pointer;
  transition: color 0.15s ease, background-color 0.15s ease, border-color 0.15s ease, box-shadow 0.15s ease, transform 0.15s ease;
  border: 1px solid transparent;
  white-space: nowrap;
}


.ed-btn--ghost {
  background: transparent;
  border-color: var(--ds-border);
  color: var(--ds-text-secondary);
}

.dark .ed-btn--ghost {
  background: var(--ds-gray-800);
  border-color: var(--ds-border-strong);
  color: #94a3b8;
}

.ed-btn--ghost:hover {
  border-color: var(--ds-primary);
  color: var(--ds-primary);
  background: var(--ds-primary-soft);
}

.ed-btn:disabled { opacity: 0.5; cursor: not-allowed; transform: none !important; }

.ed-spin { animation: spin 1s linear infinite; }
@keyframes spin { to { transform: rotate(360deg) translateZ(0); } }

/* Save status */
.ed-header__save-status {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  padding: 0.375rem 0.75rem;
  border-radius: var(--ds-radius-full);
  font-size: 0.75rem;
  font-weight: 600;
  background: var(--ds-gray-100);
  color: var(--ds-text-muted);
  border: 1px solid var(--ds-border);
  transition: color 0.2s ease, background-color 0.2s ease, border-color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease;
}

.ed-header__save-status--saving {
  background: var(--ds-info-soft);
  color: var(--ds-info);
  border-color: rgba(2, 132, 199, 0.2);
}

.ed-header__save-status--saved {
  background: var(--ds-success-soft);
  color: var(--ds-success);
  border-color: rgba(22, 163, 74, 0.2);
}

.ed-header__save-status--error {
  background: var(--ds-danger-soft);
  color: var(--ds-danger);
  border-color: rgba(220, 38, 38, 0.2);
}

.ed-header__save-icon { font-size: 1rem; }
</style>
