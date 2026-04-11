<template>
  <div class="math-renderer" :class="{ 'math-renderer--compact': compact }">
    <div v-if="!compact" class="math-renderer__toolbar">
      <button
        type="button"
        class="math-renderer__toggle-btn"
        :class="{ 'math-renderer__toggle-btn--active': previewMode }"
        @click="togglePreview"
        title="Xem trước công thức"
      >
        <LucideIcon name="eye" size="13" />
        {{ previewMode ? 'Tắt Preview' : 'Xem Preview' }}
      </button>
      <span v-if="hasMath" class="math-renderer__badge">
        <LucideIcon name="sigma" size="11" />
        Math
      </span>
    </div>
    <div v-else class="math-renderer__toolbar math-renderer__toolbar--compact">
      <button
        type="button"
        class="math-renderer__toggle-btn"
        :class="{ 'math-renderer__toggle-btn--active': previewMode }"
        @click="togglePreview"
        title="Xem trước công thức"
      >
        <LucideIcon name="eye" size="13" />
      </button>
      <span v-if="hasMath" class="math-renderer__badge">
        <LucideIcon name="sigma" size="11" />
      </span>
    </div>

    <div class="math-renderer__input-wrapper">
      <textarea
        v-bind="$attrs"
        class="math-renderer__textarea"
        :value="modelValue"
        :rows="rows"
        :placeholder="placeholder"
        @input="onInput"
        @focus="onFocus"
        @blur="onBlur"
      />
    </div>

    <!-- Preview panel: shows rendered math -->
    <div v-if="previewMode" class="math-renderer__preview">
      <div class="math-renderer__preview-label">
        <LucideIcon name="eye" size="12" />
        <span>Xem trước:</span>
      </div>
      <div
        class="math-renderer__preview-content"
        v-html="renderedHtml"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import katex from 'katex'
import 'katex/dist/katex.min.css'
import LucideIcon from '../common/LucideIcon.vue'

interface Props {
  modelValue?: string
  rows?: number
  placeholder?: string
  renderMode?: 'auto' | 'katex' | 'plain'
  /** Gọn hơn cho đáp án trắc nghiệm (toolbar một hàng) */
  compact?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: '',
  rows: 3,
  placeholder: 'Nhập nội dung...',
  renderMode: 'auto',
  compact: false,
})

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void
  (e: 'focus'): void
  (e: 'blur'): void
}>()

const previewMode = ref(false)

// Detect if text contains math content
const hasMath = computed(() => {
  const text = props.modelValue || ''
  return /(\\$[^$]+\$|\\\[[\s\S]*?\\\]|\$\$[\s\S]*?\$\$|[²³¹⁰⁴⁵⁶⁷⁸⁹⁺⁻⁼ⁿ]|[−×÷·±∓√∛∜∫∑∏∂∆∇∈∉⊂⊃⊆⊇⊄⊅∪∩∅∞ℝℤℕℚℂ→←↔⇒⇐⇔≥≤≠≈≡∝]|\\frac|\\sqrt|\\sum|\\int|\\lim|\\sin|\\cos|\\tan|\\log|\\alpha|\\beta|\\gamma|\\theta|\\pi|\\lambda)/.test(text)
})

// Render text with math support
function renderMathText(text: string): string {
  if (!text) return ''

  // Escape HTML first
  let html = text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')

  // Strategy 1: Process KaTeX blocks first ($...$ and $$...$$)
  // Block math: $$...$$
  html = html.replace(/\$\$([\s\S]*?)\$\$/g, (_, math) => {
    try {
      return `<div class="math-block">${katex.renderToString(math.trim(), { displayMode: true, throwOnError: false, strict: false, trust: true })}</div>`
    } catch {
      return `<div class="math-block math-error">${escapeHtml(math)}</div>`
    }
  })

  // Inline math: $...$
  html = html.replace(/\$([^$\n]+?)\$/g, (_, math) => {
    try {
      return katex.renderToString(math.trim(), { displayMode: false, throwOnError: false, strict: false, trust: true })
    } catch {
      return `<span class="math-error">${escapeHtml(math)}</span>`
    }
  })

  // Strategy 2: Wrap Unicode math operators in styled spans
  // Superscripts (standalone, not already in KaTeX)
  html = html.replace(/([A-Za-z0-9])([²³¹⁰⁴⁵⁶⁷⁸⁹⁺⁻⁼ⁿ])/g, '$1<sup class="superscript">$2</sup>')
  html = html.replace(/([A-Za-z0-9])([⁺⁻⁼ⁿ])/g, '$1<sup class="superscript">$2</sup>')

  // Subscripts
  html = html.replace(/([A-Za-z0-9])([₀₁₂₃₄₅₆₇₈₉])/g, '$1<sub class="subscript">$2</sub>')

  // Style math operators
  const mathOps = '−×÷·±∓√∛∜∫∑∏∂∆∇∈∉⊂⊃⊆⊇⊄⊅∪∩∅∞→←↔⇒⇐⇔≥≤≠≈≡∝'
  const opRegex = new RegExp(`([${mathOps}])`, 'g')
  html = html.replace(opRegex, '<span class="math-op">$1</span>')

  // Style Greek letters
  const greek = 'αβγδεζηθικλμνξοπρστυφχψωΑΒΓΔΕΖΗΘΙΚΛΜΝΞΟΠΡΣΤΥΦΧΨΩ'
  const greekRegex = new RegExp(`([${greek}])`, 'g')
  html = html.replace(greekRegex, '<span class="math-greek">$1</span>')

  // Style sets (ℝ ℤ ℕ ℚ ℂ)
  html = html.replace(/(ℝ|ℤ|ℕ|ℚ|ℂ|ℼ)/g, '<span class="math-set">$1</span>')

  // Preserve line breaks
  html = html.replace(/\n/g, '<br>')

  return html
}

function escapeHtml(text: string): string {
  return text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
}

const renderedHtml = computed(() => renderMathText(props.modelValue))

function togglePreview() {
  previewMode.value = !previewMode.value
}

function onInput(event: Event) {
  const target = event.target as HTMLTextAreaElement
  emit('update:modelValue', target.value)
}

function onFocus() {
  emit('focus')
}

function onBlur() {
  emit('blur')
}

// Auto-enable preview when math detected
watch(hasMath, (val) => {
  if (val && !previewMode.value) {
    previewMode.value = true
  }
})
</script>

<style scoped>
.math-renderer {
  display: flex;
  flex-direction: column;
  gap: 0.375rem;
  width: 100%;
}

.math-renderer--compact {
  gap: 0.25rem;
}

.math-renderer--compact .math-renderer__textarea {
  min-height: 44px;
  padding: 0.45rem 0.65rem;
  font-size: 0.8125rem;
}

.math-renderer--compact .math-renderer__preview {
  padding: 0.5rem 0.65rem;
}

.math-renderer__toolbar--compact {
  gap: 0.35rem;
}

.math-renderer__toolbar {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.math-renderer__toggle-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  padding: 0.2rem 0.5rem;
  border-radius: var(--ds-radius-md, 6px);
  border: 1px solid var(--ds-border, #e5e7eb);
  background: var(--ds-gray-50, #f9fafb);
  color: var(--ds-text-muted, #6b7280);
  font-size: 0.7rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s ease;
}

.math-renderer__toggle-btn:hover {
  background: var(--ds-primary-soft, rgba(79, 70, 229, 0.08));
  border-color: var(--ds-primary-border, rgba(79, 70, 229, 0.3));
  color: var(--ds-primary, #4f46e5);
}

.math-renderer__toggle-btn--active {
  background: var(--ds-primary-soft, rgba(79, 70, 229, 0.08));
  border-color: var(--ds-primary-border, rgba(79, 70, 229, 0.3));
  color: var(--ds-primary, #4f46e5);
}

.dark .math-renderer__toggle-btn {
  background: var(--ds-gray-700, #374151);
  border-color: var(--ds-border-strong, #4b5563);
  color: var(--ds-text-secondary, #9ca3af);
}

.dark .math-renderer__toggle-btn:hover,
.dark .math-renderer__toggle-btn--active {
  background: rgba(129, 140, 248, 0.15);
  border-color: rgba(129, 140, 248, 0.4);
  color: #a5b4fc;
}

.math-renderer__badge {
  display: inline-flex;
  align-items: center;
  gap: 0.2rem;
  padding: 0.15rem 0.4rem;
  border-radius: 9999px;
  background: rgba(234, 179, 8, 0.1);
  color: #b45309;
  font-size: 0.65rem;
  font-weight: 700;
}

.dark .math-renderer__badge {
  background: rgba(234, 179, 8, 0.15);
  color: #fcd34d;
}

.math-renderer__input-wrapper {
  width: 100%;
}

.math-renderer__textarea {
  width: 100%;
  padding: 0.625rem 0.875rem;
  border: 1px solid var(--ds-border, #e5e7eb);
  border-radius: var(--ds-radius-lg, 8px);
  font-size: 0.875rem;
  line-height: 1.5;
  background: var(--ds-surface, #fff);
  color: var(--ds-text, #111827);
  resize: vertical;
  min-height: 60px;
  font-family: 'Noto Sans Math', 'STIX Two Math', 'Cambria Math', 'DejaVu Serif',
    'Times New Roman', Georgia, 'Nimbus Roman No9 L', serif;
  transition: border-color 0.15s ease, box-shadow 0.15s ease;
}

.dark .math-renderer__textarea {
  background: var(--ds-gray-700, #374151);
  border-color: var(--ds-border-strong, #4b5563);
  color: var(--ds-text, #f3f4f6);
}

.math-renderer__textarea:focus {
  outline: none;
  border-color: var(--ds-primary, #4f46e5);
  box-shadow: 0 0 0 3px var(--ds-primary-ring, rgba(79, 70, 229, 0.15));
}

.math-renderer__preview {
  padding: 0.75rem;
  border: 1.5px solid var(--ds-primary-border, rgba(79, 70, 229, 0.3));
  border-radius: var(--ds-radius-xl, 12px);
  background: var(--ds-gray-50, #f9fafb);
  animation: fadeIn 0.2s ease;
}

.dark .math-renderer__preview {
  background: var(--ds-gray-800, #1f2937);
  border-color: rgba(129, 140, 248, 0.3);
}

.math-renderer__preview-label {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  font-size: 0.65rem;
  font-weight: 700;
  color: var(--ds-primary, #4f46e5);
  text-transform: uppercase;
  letter-spacing: 0.05em;
  margin-bottom: 0.5rem;
}

.dark .math-renderer__preview-label {
  color: #a5b4fc;
}

.math-renderer__preview-content {
  font-size: 0.9rem;
  line-height: 1.8;
  color: var(--ds-text, #111827);
  font-family: 'Noto Sans Math', 'STIX Two Math', 'Cambria Math', 'DejaVu Serif',
    'Times New Roman', Georgia, 'Nimbus Roman No9 L', serif;
}

.dark .math-renderer__preview-content {
  color: var(--ds-text, #f3f4f6);
}

/* Math inline styles */
:deep(.math-block) {
  margin: 0.5rem 0;
  text-align: center;
  overflow-x: auto;
}

:deep(.math-error) {
  color: var(--ds-danger, #dc2626);
  font-family: monospace;
  font-size: 0.85em;
  background: rgba(220, 38, 38, 0.08);
  padding: 0.1em 0.3em;
  border-radius: 4px;
}

:deep(.math-op) {
  color: var(--ds-primary, #4f46e5);
  font-weight: 700;
  font-family: 'Times New Roman', serif;
}

:deep(.math-greek) {
  font-style: italic;
  color: #7c3aed;
}

:deep(.math-set) {
  font-weight: 700;
  color: #0891b2;
}

:deep(.superscript) {
  font-size: 0.75em;
  vertical-align: super;
  color: var(--ds-primary, #4f46e5);
}

:deep(.subscript) {
  font-size: 0.75em;
  vertical-align: sub;
  color: var(--ds-info, #0284c7);
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(-4px); }
  to { opacity: 1; transform: translateY(0); }
}
</style>
