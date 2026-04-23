<template>
  <div class="qr" :class="`qr--${question.type?.toLowerCase() || 'mcq'}`">

    <!-- ── Render Mode Toggle (only for MCQ with both modes available) ── -->
    <div v-if="canToggleMode" class="qr__mode-bar">
      <button
        type="button"
        :class="['qr__mode-btn', activeMode === 'text' && 'qr__mode-btn--active']"
        @click="activeMode = 'text'"
      >
        <LucideIcon name="text_cursor_input" size="13" />
        Văn bản
      </button>
      <button
        type="button"
        :class="['qr__mode-btn', activeMode === 'image' && 'qr__mode-btn--active']"
        @click="activeMode = 'image'"
      >
        <LucideIcon name="image" size="13" />
        Hình ảnh
      </button>
    </div>

    <!-- ── Image Mode ── -->
    <div v-if="effectiveMode === 'image'" class="qr__image-wrap">
      <img
        v-if="imageUrl"
        :src="imageUrl"
        :alt="`Câu hỏi ${question.number}`"
        class="qr__image"
        loading="lazy"
      />
      <div v-else-if="loadingImage" class="qr__image-loading">
        <div class="qr__spinner" />
        <span>Đang tải hình...</span>
      </div>
      <div v-else class="qr__image-placeholder">
        <LucideIcon name="image_off" size="24" />
        <span>Không có hình</span>
      </div>
    </div>

    <!-- ── Text Mode ── -->
    <div v-else class="qr__text">
      <!-- Question stem -->
      <div v-if="question.text" class="qr__stem">
        <span class="qr__stem-label">Câu hỏi</span>
        <div class="qr__stem-content">
          <MathDisplay
            :content="stemPlainText"
            :latex-content="question.latexContent ?? question.latex_content ?? null"
            :content-type="question.contentType ?? question.content_type ?? null"
            source-preference="latex-first"
          />
        </div>
      </div>

      <!-- Options (for choice types) -->
      <div v-if="hasOptions" class="qr__options">
        <div
          v-for="opt in sortedOptions"
          :key="opt.id"
          class="qr__opt"
          :class="{
            'qr__opt--correct': isCorrectOption(opt.id),
            'qr__opt--show-answer': showAnswer && isCorrectOption(opt.id)
          }"
        >
          <span class="qr__opt-badge" :class="isCorrectOption(opt.id) && showAnswer ? 'qr__opt-badge--correct' : ''">
            {{ opt.id }}
          </span>
          <div class="qr__opt-text">
            <MathDisplay
              :content="opt.text"
              :latex-content="latexOptionFor(opt.id)"
              source-preference="latex-first"
            />
          </div>
          <span v-if="showAnswer && isCorrectOption(opt.id)" class="qr__opt-check">
            <LucideIcon name="check_circle" size="15" />
          </span>
        </div>
      </div>

      <!-- Essay / Fill blank answer -->
      <div v-if="question.type === 'ESSAY' || question.type === 'essay'" class="qr__essay-hint">
        <LucideIcon name="edit_note" size="14" />
        <span>Câu trả lời ngắn / Tự luận</span>
        <span v-if="question.answer" class="qr__essay-answer">
          Gợi ý: {{ question.answer }}
        </span>
      </div>

      <!-- Explanation accordion -->
      <div v-if="question.explanation" class="qr__explanation">
        <button type="button" class="qr__explanation-toggle" @click="showExplanation = !showExplanation">
          <LucideIcon name="lightbulb" size="14" />
          <span>Lời giải / Giải thích</span>
          <LucideIcon :name="showExplanation ? 'expand_less' : 'expand_more'" size="14" />
        </button>
        <div v-if="showExplanation" class="qr__explanation-body">
          {{ question.explanation }}
        </div>
      </div>
    </div>

    <!-- Confidence bar -->
    <div v-if="question.confidence !== undefined" class="qr__confidence">
      <span class="qr__conf-label">Độ tin</span>
      <div class="qr__conf-bar">
        <div
          class="qr__conf-fill"
          :class="confidenceClass"
          :style="{ width: `${(question.confidence || 0) * 100}%` }"
        />
      </div>
      <span class="qr__conf-value">{{ ((question.confidence || 0) * 100).toFixed(0) }}%</span>
    </div>

    <!-- Issues -->
    <div v-if="question.issues?.length" class="qr__issues">
      <div v-for="(issue, idx) in question.issues" :key="idx" class="qr__issue">
        <LucideIcon name="warning" size="12" />
        {{ issue }}
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onBeforeUnmount } from 'vue'
import { fetchExamQuestionImage } from '../../services/importService'
import MathDisplay from '../shared/MathDisplay.vue'

const props = defineProps({
  question: {
    type: Object,
    required: true
  },
  sessionId: {
    type: [Number, String],
    default: null
  },
  showAnswer: {
    type: Boolean,
    default: false
  },
  defaultMode: {
    type: String,
    default: 'auto'  // 'auto' | 'text' | 'image'
  }
})

const showExplanation = ref(false)
const loadingImage = ref(false)
const imageUrl = ref(null)
let imageRequestToken = 0

/** API exam-import dùng `text` (ParsedQuestionDto); bước review dùng `content`. */
const stemPlainText = computed(() => {
  const q = props.question
  const t = q?.text ?? q?.content
  return t != null ? String(t) : ''
})

const effectiveRenderMode = computed(() => {
  const rawMode =
    props.question?.render?.mode ??
    props.question?.renderMode ??
    props.question?.render_mode
  const normalizedMode = String(rawMode ?? '').trim().toLowerCase()
  return normalizedMode || 'text'
})

const canToggleMode = computed(() => {
  return effectiveRenderMode.value === 'image' ||
    (props.question.render?.bbox && props.sessionId)
})

const activeMode = ref(props.defaultMode === 'auto'
  ? effectiveRenderMode.value
  : props.defaultMode)

const effectiveMode = computed(() => {
  if (canToggleMode.value) return activeMode.value
  return effectiveRenderMode.value
})

function revokeImageUrl() {
  if (imageUrl.value && imageUrl.value.startsWith('blob:')) {
    URL.revokeObjectURL(imageUrl.value)
  }
  imageUrl.value = null
}

async function loadImage() {
  const sessionId = props.sessionId != null ? Number(props.sessionId) : null
  const questionIndex = props.question?.questionIndex != null
    ? Number(props.question.questionIndex)
    : (props.question?.number != null ? Number(props.question.number) : null)
  const shouldLoad =
    effectiveMode.value === 'image' &&
    sessionId != null &&
    Number.isFinite(sessionId) &&
    questionIndex != null &&
    Number.isFinite(questionIndex)

  imageRequestToken += 1
  const requestToken = imageRequestToken

  revokeImageUrl()
  if (!shouldLoad) {
    loadingImage.value = false
    return
  }

  loadingImage.value = true
  try {
    const blob = await fetchExamQuestionImage(sessionId, questionIndex)
    if (requestToken !== imageRequestToken) return
    imageUrl.value = blob ? URL.createObjectURL(blob) : null
  } finally {
    if (requestToken === imageRequestToken) {
      loadingImage.value = false
    }
  }
}

const hasOptions = computed(() => {
  return props.question.options &&
    Object.keys(props.question.options).length >= 2
})

const sortedOptions = computed(() => {
  if (!props.question.options) return []
  return ['A', 'B', 'C', 'D']
    .filter(k => props.question.options[k]?.trim())
    .map(k => ({
      id: k,
      text: props.question.options[k] || ''
    }))
})

function latexOptionFor(optionId) {
  const lo = props.question.latexOptions ?? props.question.latex_options
  if (!lo) return null
  if (typeof lo === 'string') {
    try {
      const p = JSON.parse(lo)
      const v = p[optionId] ?? p[String(optionId).toUpperCase()]
      return v != null && String(v).trim() !== '' ? String(v) : null
    } catch {
      return null
    }
  }
  const v = lo[optionId] ?? lo[String(optionId).toUpperCase()]
  return v != null && String(v).trim() !== '' ? String(v) : null
}

const isCorrectOption = (optId) => {
  const ans = props.question.answer
  if (!ans) return false
  const normalizedOpt = String(optId).toUpperCase()
  if (Array.isArray(ans)) {
    return ans.map((value) => String(value).toUpperCase()).includes(normalizedOpt)
  }
  const normalizedAnswer = String(ans).toUpperCase()
  if (normalizedAnswer.includes(',')) {
    return normalizedAnswer.split(',').map((value) => value.trim()).includes(normalizedOpt)
  }
  return normalizedAnswer === normalizedOpt
}

const confidenceClass = computed(() => {
  const c = props.question.confidence || 0
  if (c >= 0.8) return 'qr__conf-fill--high'
  if (c >= 0.5) return 'qr__conf-fill--mid'
  return 'qr__conf-fill--low'
})

watch(
  () => [
    props.sessionId,
    props.question?.number,
    props.question?.questionIndex,
    props.question?.render?.mode,
    effectiveMode.value,
  ],
  () => {
    void loadImage()
  },
  { immediate: true },
)

onBeforeUnmount(() => {
  imageRequestToken += 1
  revokeImageUrl()
})
</script>

<style scoped>
.qr {
  border: 1.5px solid var(--ds-border);
  border-radius: var(--ds-radius-xl);
  background: var(--ds-surface);
  overflow: hidden;
  transition: border-color 0.15s ease;
}

.dark .qr {
  border-color: var(--ds-border-strong);
}

.qr--essay,
.qr--ESSAY {
  border-left: 3px solid var(--ds-warning);
}

/* Mode toggle */
.qr__mode-bar {
  display: flex;
  gap: 0.25rem;
  padding: 0.5rem 0.75rem;
  background: var(--ds-gray-50);
  border-bottom: 1px solid var(--ds-border);
}

.dark .qr__mode-bar {
  background: var(--ds-gray-800);
  border-bottom-color: var(--ds-border-strong);
}

.qr__mode-btn {
  display: flex;
  align-items: center;
  gap: 0.3rem;
  padding: 0.3rem 0.625rem;
  border-radius: var(--ds-radius-md);
  border: 1px solid transparent;
  background: transparent;
  color: var(--ds-text-muted);
  font-size: 0.72rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.12s ease;
}

.qr__mode-btn:hover {
  background: var(--ds-surface);
  border-color: var(--ds-border);
  color: var(--ds-text);
}

.dark .qr__mode-btn:hover {
  background: var(--ds-gray-700);
}

.qr__mode-btn--active {
  background: var(--ds-primary-soft);
  border-color: var(--ds-primary-border);
  color: var(--ds-primary);
}

/* Image */
.qr__image-wrap {
  padding: 0;
  background: var(--ds-gray-100);
}

.dark .qr__image-wrap {
  background: var(--ds-gray-800);
}

.qr__image {
  width: 100%;
  height: auto;
  display: block;
  border-radius: 0;
}

.qr__image-loading,
.qr__image-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  padding: 2rem;
  color: var(--ds-text-muted);
  font-size: 0.8rem;
}

.qr__spinner {
  width: 24px;
  height: 24px;
  border: 2px solid var(--ds-border);
  border-top-color: var(--ds-primary);
  border-radius: 50%;
  animation: qr-spin 0.8s linear infinite;
}

@keyframes qr-spin {
  to { transform: rotate(360deg); }
}

/* Text mode */
.qr__text {
  padding: 0.875rem 1rem;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.qr__stem {
  display: flex;
  flex-direction: column;
  gap: 0.375rem;
}

.qr__stem-label {
  font-size: 0.68rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: var(--ds-text-muted);
}

.qr__stem-content {
  font-size: 0.875rem;
  line-height: 1.6;
  color: var(--ds-text);
}

/* Options */
.qr__options {
  display: flex;
  flex-direction: column;
  gap: 0.375rem;
}

.qr__opt {
  display: flex;
  align-items: flex-start;
  gap: 0.625rem;
  padding: 0.5rem 0.75rem;
  border: 1.5px solid var(--ds-border);
  border-radius: var(--ds-radius-lg);
  background: var(--ds-gray-50);
  transition: border-color 0.12s ease, background 0.12s ease;
}

.dark .qr__opt {
  background: var(--ds-gray-800);
  border-color: var(--ds-border-strong);
}

.qr__opt--correct {
  border-color: var(--ds-success) !important;
  background: var(--ds-success-soft) !important;
}

.dark .qr__opt--correct {
  background: rgba(22, 163, 74, 0.08) !important;
}

.qr__opt-badge {
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

.dark .qr__opt-badge {
  background: var(--ds-gray-700);
}

.qr__opt-badge--correct {
  background: var(--ds-success) !important;
  color: white !important;
}

.qr__opt-text {
  flex: 1;
  font-size: 0.85rem;
  color: var(--ds-text);
  line-height: 1.5;
  min-width: 0;
}

.dark .qr__opt-text {
  color: #e2e8f0;
}

.qr__opt-check {
  color: var(--ds-success);
  flex-shrink: 0;
}

/* Essay hint */
.qr__essay-hint {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  padding: 0.5rem 0.75rem;
  background: rgba(245, 158, 11, 0.06);
  border: 1px solid rgba(245, 158, 11, 0.2);
  border-radius: var(--ds-radius-lg);
  font-size: 0.8rem;
  color: #92400e;
}

.dark .qr__essay-hint {
  background: rgba(245, 158, 11, 0.08);
  color: #fbbf24;
}

.qr__essay-answer {
  margin-left: auto;
  font-style: italic;
  opacity: 0.8;
}

/* Explanation */
.qr__explanation {
  border: 1px solid var(--ds-border);
  border-radius: var(--ds-radius-lg);
  overflow: hidden;
}

.dark .qr__explanation {
  border-color: var(--ds-border-strong);
}

.qr__explanation-toggle {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  width: 100%;
  padding: 0.5rem 0.75rem;
  background: var(--ds-gray-50);
  border: none;
  cursor: pointer;
  font-size: 0.78rem;
  font-weight: 600;
  color: var(--ds-text-secondary);
  transition: background 0.1s ease;
}

.dark .qr__explanation-toggle {
  background: var(--ds-gray-800);
  color: #94a3b8;
}

.qr__explanation-toggle:hover {
  background: var(--ds-gray-100);
}

.dark .qr__explanation-toggle:hover {
  background: var(--ds-gray-700);
}

.qr__explanation-body {
  padding: 0.75rem;
  font-size: 0.82rem;
  line-height: 1.6;
  color: var(--ds-text-secondary);
  border-top: 1px solid var(--ds-border);
  background: var(--ds-surface);
}

.dark .qr__explanation-body {
  border-top-color: var(--ds-border-strong);
  color: #94a3b8;
}

/* Confidence */
.qr__confidence {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.375rem 0.75rem;
  background: var(--ds-gray-50);
  border-top: 1px solid var(--ds-border);
  font-size: 0.7rem;
  color: var(--ds-text-muted);
}

.dark .qr__confidence {
  background: var(--ds-gray-800);
  border-top-color: var(--ds-border-strong);
}

.qr__conf-label {
  font-weight: 600;
  white-space: nowrap;
}

.qr__conf-bar {
  flex: 1;
  height: 4px;
  background: var(--ds-gray-200);
  border-radius: 9999px;
  overflow: hidden;
}

.dark .qr__conf-bar {
  background: var(--ds-gray-700);
}

.qr__conf-fill {
  height: 100%;
  border-radius: 9999px;
  transition: width 0.3s ease;
}

.qr__conf-fill--high { background: var(--ds-success); }
.qr__conf-fill--mid { background: var(--ds-warning); }
.qr__conf-fill--low { background: var(--ds-danger); }

.qr__conf-value {
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  min-width: 32px;
  text-align: right;
}

/* Issues */
.qr__issues {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
  padding: 0.5rem 0.75rem;
  background: rgba(239, 68, 68, 0.05);
  border-top: 1px solid rgba(239, 68, 68, 0.15);
}

.qr__issue {
  display: flex;
  align-items: center;
  gap: 0.3rem;
  font-size: 0.72rem;
  color: var(--ds-danger);
  font-weight: 500;
}

@media (prefers-reduced-motion: reduce) {
  .qr__spinner,
  .qr__conf-fill {
    transition: none;
    animation: none;
  }
}
</style>
