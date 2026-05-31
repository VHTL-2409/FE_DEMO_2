<template>
  <div
    class="math-content"
    :class="{ 'math-content--unicode-plain': renderBundle.kind === 'plain' }"
    ref="containerRef"
  >
    <div v-if="renderBundle.kind === 'plain'" class="math-plain">{{ renderBundle.text }}</div>
    <div
      v-else-if="renderBundle.kind === 'html'"
      v-html="renderBundle.html"
      class="math-rendered"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, nextTick } from 'vue'
import katex from 'katex'
import 'katex/dist/katex.min.css'
import {
  normalizeMathematicalUnicodeForEditor,
  applyTexSymbolCommandsToUnicode,
} from '../../utils/mathTextNormalize'
import {
  containsVietnameseLatin,
  collapseAdjacentDuplicateMathPlainLines,
} from '../../utils/mixedMathDisplay'

interface Props {
  /** Raw content text (may contain $...$ or $$...$$ delimiters) */
  content: string
  /** LaTeX từ backend — chỉ dùng khi content rỗng (tránh lệch với ô đang sửa) */
  latexContent?: string | null
  /** plain | math | mixed — từ parser; plain = ưu tiên văn bản, không KaTeX full-line */
  contentType?: string | null
  /** Display mode: 'auto' | 'inline' | 'block' */
  displayMode?: 'auto' | 'inline' | 'block'
  /** Throw error on invalid LaTeX instead of falling back */
  throwOnError?: boolean
  /**
   * 'content' (default) — dùng content, latexContent chỉ fallback khi content rỗng.
   * 'smart' — nếu latexContent non-empty VÀ content không có delimiter ($ \\\[ \\\])
   *           thì dùng latexContent làm nguồn render.
   * 'latex-first' — nếu có latexContent thì chỉ render latex (một nguồn), không trùng plain.
   */
  sourcePreference?: 'content' | 'smart' | 'latex-first'
}

const props = withDefaults(defineProps<Props>(), {
  content: '',
  latexContent: null,
  contentType: null,
  displayMode: 'auto',
  throwOnError: false,
  sourcePreference: 'content',
})

const containerRef = ref<HTMLElement | null>(null)

function shouldPreferPlainOverLatex(lc: string): boolean {
  const ct = props.contentType != null ? String(props.contentType).toLowerCase() : ''
  if (ct === 'plain') {
    return true
  }
  if (lc === '') {
    return false
  }
  if (containsVietnameseLatin(lc) && !hasExplicitMathDelimiters(lc)) {
    return true
  }
  return false
}

/** Nguồn render duy nhất — tránh hiển thị trùng plain + latex. */
const sourceText = computed(() => {
  const c = props.content != null ? String(props.content) : ''
  const lcRaw =
    props.latexContent != null && props.latexContent !== undefined
      ? String(props.latexContent)
      : ''
  const lc = lcRaw.trim()

  if (props.sourcePreference === 'latex-first' && lc !== '') {
    if (shouldPreferPlainOverLatex(lc) && c.trim() !== '') {
      return c
    }
    return lc
  }

  if (props.sourcePreference === 'smart' && lc !== '' && !hasExplicitMathDelimiters(c)) {
    if (shouldPreferPlainOverLatex(lc) && c.trim() !== '') {
      return c
    }
    return lc
  }

  if (c.trim() !== '') {
    return c
  }
  if (lc !== '') {
    return lc
  }
  return c
})

function dedupeIdenticalLines(s: string): string {
  const lines = s.split(/\n/).map((l) => l.trim()).filter((l) => l.length > 0)
  if (lines.length < 2) {
    return s
  }
  const first = lines[0]
  if (first != null && lines.every((l) => l === first)) {
    return first
  }
  return s
}

const normalizedSource = computed(() => {
  let t = sourceText.value
  t = dedupeIdenticalLines(t)
  t = collapseAdjacentDuplicateMathPlainLines(t)
  const unicodeFixed = normalizeMathematicalUnicodeForEditor(t)
  return applyTexSymbolCommandsToUnicode(unicodeFixed)
})

function hasExplicitMathDelimiters(t: string): boolean {
  return /\$|\\\(|\\\[/u.test(t)
}

function escapeHtml(s: string): string {
  return s
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
}

function renderKatexSegment(latex: string, displayMode: boolean): string {
  const trimmed = latex.trim()
  if (!trimmed) {
    return ''
  }
  try {
    return katex.renderToString(trimmed, {
      displayMode,
      throwOnError: props.throwOnError,
      strict: false,
      trust: true,
    })
  } catch {
    try {
      return katex.renderToString(trimmed, {
        displayMode,
        throwOnError: false,
        strict: false,
        trust: true,
      })
    } catch {
      return escapeHtml(trimmed)
    }
  }
}

/** Xử lý inline $...$ trong một đoạn text (không chứa block $$). */
function renderInlineOnly(segment: string): string {
  const inlinePattern = /\$((?!\$)[^$]+)\$/g
  let out = ''
  let last = 0
  let m: RegExpExecArray | null
  while ((m = inlinePattern.exec(segment)) !== null) {
    out += escapeHtml(segment.slice(last, m.index))
    out += renderKatexSegment(m[1] ?? '', false)
    last = m.index + m[0].length
  }
  out += escapeHtml(segment.slice(last))
  return out
}

/** Block $$...$$ / \\[...\\] rồi inline trong phần còn lại. */
function renderMixedMath(text: string): string {
  const blockPattern = /\$\$([\s\S]*?)\$\$|\\\[([\s\S]*?)\\\]/g
  let out = ''
  let last = 0
  let m: RegExpExecArray | null
  while ((m = blockPattern.exec(text)) !== null) {
    out += renderInlineOnly(text.slice(last, m.index))
    const inner = (m[1] ?? m[2] ?? '').trim()
    out += renderKatexSegment(inner, true)
    last = m.index + m[0].length
  }
  out += renderInlineOnly(text.slice(last))
  return out
}

/**
 * Chuỗi có lệnh \\foo nhưng không bọc $ — thử render cả chuỗi như công thức (một khối).
 */
function tryRenderBareLatexBlock(text: string): string | null {
  const t = text.trim()
  if (!t || hasExplicitMathDelimiters(t)) {
    return null
  }
  if (containsVietnameseLatin(t)) {
    return null
  }
  if (!/\\[a-zA-Z]+/.test(t)) {
    return null
  }
  try {
    return katex.renderToString(t, {
      displayMode: props.displayMode === 'block',
      throwOnError: true,
      strict: false,
      trust: true,
    })
  } catch {
    return null
  }
}

type RenderBundle =
  | { kind: 'empty' }
  | { kind: 'plain'; text: string }
  | { kind: 'html'; html: string }

const renderBundle = computed((): RenderBundle => {
  const text = normalizedSource.value
  if (!text.trim()) {
    return { kind: 'empty' }
  }

  try {
    if (hasExplicitMathDelimiters(text)) {
      return { kind: 'html', html: renderMixedMath(text) }
    }

    const bare = tryRenderBareLatexBlock(text)
    if (bare !== null) {
      return { kind: 'html', html: bare }
    }

    return { kind: 'plain', text }
  } catch (e) {
    if (import.meta.env.DEV) {
      console.warn('[MathDisplay] Render error:', e)
    }
    return { kind: 'plain', text }
  }
})

watch(
  () => [props.content, props.latexContent, props.contentType],
  async () => {
    await nextTick()
  },
)
</script>

<style scoped>
.math-content {
  font-size: 1rem;
  line-height: 1.8;
  word-wrap: break-word;
  overflow-wrap: break-word;
}

.math-content--unicode-plain,
.math-plain {
  font-family:
    'STIX Two Math',
    'Cambria Math',
    'Latin Modern Math',
    'DejaVu Math TeX Gyre',
    'Noto Sans Math',
    serif;
}

.math-plain {
  white-space: pre-wrap;
}

/* KaTeX rendered elements */
.math-content :deep(.katex) {
  font-size: 1.1em;
}

.math-content :deep(.katex-display) {
  margin: 1em 0;
  overflow-x: auto;
  overflow-y: hidden;
  padding: 0.5em 0;
}

.math-content :deep(.katex-display > .katex) {
  text-align: center;
}

.math-rendered {
  display: inline;
}
</style>
