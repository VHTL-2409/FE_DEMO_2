<template>
  <div
    class="qic"
    :class="{
      'qic--expanded': expanded,
      'qic--dragging': isDragging,
      'qic--drag-over': isDragOver
    }"
    @dragstart="onDragStart"
    @dragend="onDragEnd"
    @dragover.prevent="onDragOver"
    @dragleave="onDragLeave"
    @drop.prevent="onDrop"
  >
    <!-- Drag handle -->
    <div v-if="draggable" class="qic__drag-handle" title="Kéo để sắp xếp">
      <LucideIcon name="drag_indicator" />
    </div>

    <!-- Main row -->
    <div class="qic__main" @click="expanded = !expanded">
      <!-- Number badge -->
      <div class="qic__num">{{ index + 1 }}</div>

      <!-- Content -->
      <div class="qic__body">
        <p class="qic__content">{{ question.content }}</p>
        <div class="qic__meta">
          <span class="qic__correct">
            <LucideIcon name="check" />
            {{ displayCorrectAnswer }}
          </span>
          <span class="qic__score">{{ Number(question.scoreWeight || question.score || 1) }} điểm</span>
          <span class="qic__type" :class="`qic__type--${typeColor}`">{{ typeLabel }}</span>
        </div>
      </div>

      <!-- Actions -->
      <div class="qic__actions" @click.stop>
        <button type="button" class="qic__action" title="Sửa" @click="handleEdit">
          <LucideIcon name="edit" />
        </button>
        <button type="button" class="qic__action" title="Nhân bản" @click="handleDuplicate">
          <LucideIcon name="content_copy" />
        </button>
        <button type="button" class="qic__action qic__action--danger" title="Xóa" @click="handleDelete">
          <LucideIcon name="delete" />
        </button>
        <button type="button" class="qic__action" :title="expanded ? 'Thu gọn' : 'Mở rộng'">
          <LucideIcon :name="expanded ? 'expand_less' : 'expand_more'" />
        </button>
      </div>
    </div>

    <!-- Expanded options -->
    <div v-if="expanded" class="qic__detail">
      <div v-if="parsedOptions.length > 0" class="qic__options">
        <div
          v-for="opt in parsedOptions"
          :key="opt.id"
          class="qic__opt"
          :class="{ 'qic__opt--correct': isCorrectOption(opt.id) }"
        >
          <span class="qic__opt-badge">{{ opt.id }}</span>
          <span class="qic__opt-text">{{ opt.text || opt.label || opt.content }}</span>
          <span v-if="isCorrectOption(opt.id)" class="qic__opt-check">
            <LucideIcon name="check_circle" />
          </span>
        </div>
      </div>
      <div v-else-if="hasRawOptions" class="qic__options">
        <div v-if="question.optionA" class="qic__opt" :class="{ 'qic__opt--correct': question.correctAnswer === 'A' }">
          <span class="qic__opt-badge">A</span>
          <span class="qic__opt-text">{{ question.optionA }}</span>
          <span v-if="question.correctAnswer === 'A'" class="qic__opt-check">
            <LucideIcon name="check_circle" />
          </span>
        </div>
        <div v-if="question.optionB" class="qic__opt" :class="{ 'qic__opt--correct': question.correctAnswer === 'B' }">
          <span class="qic__opt-badge">B</span>
          <span class="qic__opt-text">{{ question.optionB }}</span>
          <span v-if="question.correctAnswer === 'B'" class="qic__opt-check">
            <LucideIcon name="check_circle" />
          </span>
        </div>
        <div v-if="question.optionC" class="qic__opt" :class="{ 'qic__opt--correct': question.correctAnswer === 'C' }">
          <span class="qic__opt-badge">C</span>
          <span class="qic__opt-text">{{ question.optionC }}</span>
          <span v-if="question.correctAnswer === 'C'" class="qic__opt-check">
            <LucideIcon name="check_circle" />
          </span>
        </div>
        <div v-if="question.optionD" class="qic__opt" :class="{ 'qic__opt--correct': question.correctAnswer === 'D' }">
          <span class="qic__opt-badge">D</span>
          <span class="qic__opt-text">{{ question.optionD }}</span>
          <span v-if="question.correctAnswer === 'D'" class="qic__opt-check">
            <LucideIcon name="check_circle" />
          </span>
        </div>
      </div>
      <div v-else class="qic__no-options">
        <LucideIcon name="text_snippet" />
        Câu hỏi tự luận — không có đáp án
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'

const props = defineProps({
  question: { type: Object, required: true },
  index: { type: Number, required: true },
  draggable: { type: Boolean, default: false }
})

const emit = defineEmits(['edit', 'duplicate', 'delete', 'drag-start', 'drag-end', 'drag-over', 'drag-leave', 'drop'])

const expanded = ref(false)
const isDragging = ref(false)
const isDragOver = ref(false)

const typeMap = {
  SINGLE_CHOICE: { label: 'Trắc nghiệm', color: 'primary' },
  MULTIPLE_CHOICE: { label: 'Nhiều đáp án', color: 'info' },
  ESSAY: { label: 'Tự luận', color: 'warning' },
  TRUE_FALSE: { label: 'Đúng/Sai', color: 'muted' }
}

const typeLabel = computed(() => typeMap[props.question.type]?.label || 'Trắc nghiệm')
const typeColor = computed(() => typeMap[props.question.type]?.color || 'primary')

const parsedOptions = computed(() => {
  if (!props.question.options) return []
  try {
    if (typeof props.question.options === 'string') {
      return JSON.parse(props.question.options)
    }
    return props.question.options
  } catch {
    return []
  }
})

const hasRawOptions = computed(() =>
  props.question.optionA || props.question.optionB || props.question.optionC || props.question.optionD
)

const displayCorrectAnswer = computed(() => {
  const correct = props.question.correctAnswer || props.question.correctOption || ''
  if (!correct) return '—'
  if (Array.isArray(correct)) return correct.join(', ')
  return String(correct)
})

const isCorrectOption = (optId) => {
  const correct = props.question.correctAnswer || props.question.correctOption || ''
  if (Array.isArray(correct)) return correct.includes(String(optId))
  return String(optId) === String(correct)
}

const handleEdit = () => emit('edit', props.question)
const handleDuplicate = () => emit('duplicate', props.question)
const handleDelete = () => emit('delete', props.question)

// Drag handlers
const onDragStart = (e) => {
  isDragging.value = true
  e.dataTransfer.effectAllowed = 'move'
  e.dataTransfer.setData('text/plain', String(props.index))
  emit('drag-start', e, props.index)
}

const onDragEnd = (e) => {
  isDragging.value = false
  emit('drag-end', e)
}

const onDragOver = (e) => {
  isDragOver.value = true
  emit('drag-over', e, props.index)
}

const onDragLeave = () => {
  isDragOver.value = false
  emit('drag-leave', e)
}

const onDrop = (e) => {
  isDragOver.value = false
  const fromIndex = parseInt(e.dataTransfer.getData('text/plain'), 10)
  emit('drop', e, fromIndex, props.index)
}
</script>


<style scoped>
.qic {
  background: var(--ds-surface);
  border: 1.5px solid var(--ds-border);
  border-radius: var(--ds-radius-xl);
  overflow: hidden;
  transition: all 0.15s ease;
  position: relative;
}

.dark .qic {
  border-color: var(--ds-border-strong);
}

.qic:hover {
  border-color: var(--ds-primary-border);
  box-shadow: 0 2px 8px rgba(79, 70, 229, 0.08);
}

.qic--expanded {
  border-color: var(--ds-primary-border);
  box-shadow: 0 4px 16px rgba(79, 70, 229, 0.12);
}

.qic--dragging {
  opacity: 0.5;
  transform: rotate(1deg);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.15);
}

.qic--drag-over {
  border-color: var(--ds-primary);
  border-style: dashed;
  background: var(--ds-primary-soft);
}

/* Drag handle */
.qic__drag-handle {
  position: absolute;
  top: 50%;
  left: -2px;
  transform: translateY(-50%);
  width: 20px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: grab;
  opacity: 0;
  transition: opacity 0.15s ease;
  color: var(--ds-text-muted);
  z-index: 2;
}

.qic:hover .qic__drag-handle {
  opacity: 1;
}

.qic__drag-handle:active {
  cursor: grabbing;
}


/* Main row */
.qic__main {
  display: flex;
  align-items: flex-start;
  gap: 0.75rem;
  padding: 0.875rem 1rem;
  cursor: pointer;
  transition: background 0.1s ease;
}

.qic__main:hover {
  background: var(--ds-gray-50);
}

.dark .qic__main:hover {
  background: var(--ds-gray-800);
}

/* Number */
.qic__num {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: var(--ds-primary-soft);
  color: var(--ds-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.75rem;
  font-weight: 800;
  flex-shrink: 0;
  margin-top: 0.125rem;
}

/* Body */
.qic__body {
  flex: 1;
  min-width: 0;
}

.qic__content {
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--ds-text);
  margin: 0;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.dark .qic__content {
  color: #f1f5f9;
}

.qic__meta {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-top: 0.375rem;
  flex-wrap: wrap;
}

.qic__correct {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  background: var(--ds-success-soft);
  color: var(--ds-success);
  padding: 0.1rem 0.5rem;
  border-radius: var(--ds-radius-full);
  font-size: 0.7rem;
  font-weight: 700;
}


.qic__score {
  font-size: 0.7rem;
  font-weight: 600;
  color: var(--ds-text-muted);
}

.qic__type {
  background: var(--ds-gray-100);
  padding: 0.1rem 0.5rem;
  border-radius: var(--ds-radius-full);
  font-size: 0.7rem;
  font-weight: 600;
}

.dark .qic__type {
  background: var(--ds-gray-700);
}

.qic__type--primary { background: var(--ds-primary-soft); color: var(--ds-primary); }
.qic__type--info { background: var(--ds-info-soft); color: var(--ds-info); }
.qic__type--warning { background: rgba(234, 179, 8, 0.1); color: #d97706; }
.qic__type--muted { background: var(--ds-gray-100); color: var(--ds-text-muted); }
.dark .qic__type--muted { background: var(--ds-gray-700); color: #94a3b8; }

/* Actions */
.qic__actions {
  display: flex;
  align-items: center;
  gap: 0.125rem;
  flex-shrink: 0;
}

.qic__action {
  width: 2rem;
  height: 2rem;
  border-radius: var(--ds-radius-md);
  border: none;
  background: transparent;
  color: var(--ds-text-muted);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.12s ease;
}

.qic__action:hover {
  background: var(--ds-gray-100);
  color: var(--ds-text);
}

.dark .qic__action:hover {
  background: var(--ds-gray-700);
}

.qic__action--danger:hover {
  background: var(--ds-danger-soft);
  color: var(--ds-danger);
}


/* Detail */
.qic__detail {
  padding: 0 1rem 1rem;
  animation: qicFadeIn 0.15s ease;
}

@keyframes qicFadeIn {
  from { opacity: 0; transform: translateY(-4px); }
  to { opacity: 1; transform: translateY(0); }
}

.qic__options {
  display: flex;
  flex-direction: column;
  gap: 0.375rem;
  padding-left: calc(28px + 0.75rem);
}

.qic__opt {
  display: flex;
  align-items: flex-start;
  gap: 0.5rem;
  padding: 0.5rem 0.75rem;
  background: var(--ds-gray-50);
  border: 1px solid var(--ds-border);
  border-radius: var(--ds-radius-lg);
  font-size: 0.8rem;
  color: var(--ds-text-secondary);
}

.dark .qic__opt {
  background: var(--ds-gray-800);
  border-color: var(--ds-border-strong);
  color: #94a3b8;
}

.qic__opt--correct {
  background: var(--ds-success-soft);
  border-color: rgba(22, 163, 74, 0.3);
  color: var(--ds-success);
}

.qic__opt-badge {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: var(--ds-gray-200);
  color: var(--ds-text-muted);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.65rem;
  font-weight: 800;
  flex-shrink: 0;
}

.dark .qic__opt-badge {
  background: var(--ds-gray-700);
}

.qic__opt--correct .qic__opt-badge {
  background: var(--ds-success);
  color: white;
}

.qic__opt-text {
  flex: 1;
  line-height: 1.4;
}

.qic__opt-check {
  flex-shrink: 0;
  color: var(--ds-success);
}


.qic__no-options {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem;
  padding-left: calc(28px + 0.75rem);
  font-size: 0.8rem;
  color: var(--ds-text-muted);
  font-style: italic;
}

</style>
