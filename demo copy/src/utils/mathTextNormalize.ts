/**
 * PDF / parser thường trả chữ Latin kiểu Mathematical Alphanumeric (U+1D400…)
 * mà font thường không có → ô vuông (□). Map về ASCII (idempotent).
 * Chỉ xử lý các dải Latin liên tục đã xác minh; bỏ qua Hy Lạp / Fraktur có lỗ.
 */

const LATIN_MATH_LETTER_RANGES: ReadonlyArray<readonly [start: number, end: number, baseLetter: number]> = [
  [0x1d400, 0x1d419, 0x41],
  [0x1d41a, 0x1d433, 0x61],
  [0x1d434, 0x1d44d, 0x41],
  [0x1d44e, 0x1d467, 0x61],
  [0x1d468, 0x1d481, 0x41],
  [0x1d482, 0x1d49b, 0x61],
  [0x1d5d4, 0x1d5ed, 0x41],
  [0x1d5ee, 0x1d607, 0x61],
  [0x1d608, 0x1d621, 0x41],
  [0x1d622, 0x1d63b, 0x61],
  [0x1d63c, 0x1d655, 0x41],
  [0x1d656, 0x1d66f, 0x61],
  [0x1d670, 0x1d689, 0x41],
  [0x1d68a, 0x1d6a3, 0x61],
]

function mapMathLatinLetter(cp: number): string | null {
  for (const [start, end, base] of LATIN_MATH_LETTER_RANGES) {
    if (cp >= start && cp <= end) {
      return String.fromCodePoint(base + (cp - start))
    }
  }
  return null
}

/** Chữ số Mathematical Alphanumeric (PDF/Word) → ASCII 0–9 */
function mapMathAlphanumericDigit(cp: number): string | null {
  const bases = [0x1d7ce, 0x1d7d8, 0x1d7e2, 0x1d7ec, 0x1d7f6]
  for (const b of bases) {
    if (cp >= b && cp < b + 10) {
      return String(cp - b)
    }
  }
  return null
}

/**
 * Thay các lệnh TeX "an toàn" (ký hiệu đơn) bằng Unicode khi không qua KaTeX.
 * Không thay \\frac, \\sqrt, \\sum… (cần cú pháp đầy đủ).
 */
const TEX_SYMBOL_REPLACEMENTS: ReadonlyArray<readonly [string, string]> = [
  ['\\Leftrightarrow', '⇔'],
  ['\\leftrightarrow', '↔'],
  ['\\Longleftrightarrow', '⟺'],
  ['\\Rightarrow', '⇒'],
  ['\\Leftarrow', '⇐'],
  ['\\longrightarrow', '⟶'],
  ['\\longleftarrow', '⟵'],
  ['\\rightarrow', '→'],
  ['\\leftarrow', '←'],
  ['\\infty', '∞'],
  ['\\times', '×'],
  ['\\cdot', '·'],
  ['\\pm', '±'],
  ['\\mp', '∓'],
  ['\\leq', '≤'],
  ['\\geq', '≥'],
  ['\\neq', '≠'],
  ['\\approx', '≈'],
  ['\\equiv', '≡'],
  ['\\sim', '∼'],
  ['\\propto', '∝'],
  ['\\partial', '∂'],
  ['\\nabla', '∇'],
  ['\\forall', '∀'],
  ['\\exists', '∃'],
  ['\\in', '∈'],
  ['\\notin', '∉'],
  ['\\subseteq', '⊆'],
  ['\\supseteq', '⊇'],
  ['\\subsetneq', '⊊'],
  ['\\supsetneq', '⊋'],
  ['\\subset', '⊂'],
  ['\\supset', '⊃'],
  ['\\cup', '∪'],
  ['\\cap', '∩'],
  ['\\emptyset', '∅'],
  ['\\varnothing', '∅'],
  ['\\angle', '∠'],
  ['\\perp', '⊥'],
  ['\\parallel', '∥'],
  ['\\degree', '°'],
  ['\\circ', '∘'],
  ['\\ldots', '…'],
  ['\\cdots', '⋯'],
  ['\\varepsilon', 'ε'],
  ['\\vartheta', 'ϑ'],
  ['\\varpi', 'ϖ'],
  ['\\varrho', 'ϱ'],
  ['\\varsigma', 'ς'],
  ['\\varphi', 'φ'],
  ['\\epsilon', 'ε'],
  ['\\theta', 'θ'],
  ['\\lambda', 'λ'],
  ['\\mu', 'μ'],
  ['\\pi', 'π'],
  ['\\sigma', 'σ'],
  ['\\phi', 'φ'],
  ['\\omega', 'ω'],
  ['\\alpha', 'α'],
  ['\\beta', 'β'],
  ['\\gamma', 'γ'],
  ['\\delta', 'δ'],
  ['\\zeta', 'ζ'],
  ['\\eta', 'η'],
  ['\\iota', 'ι'],
  ['\\kappa', 'κ'],
  ['\\nu', 'ν'],
  ['\\xi', 'ξ'],
  ['\\rho', 'ρ'],
  ['\\tau', 'τ'],
  ['\\upsilon', 'υ'],
  ['\\chi', 'χ'],
  ['\\psi', 'ψ'],
  ['\\Gamma', 'Γ'],
  ['\\Delta', 'Δ'],
  ['\\Theta', 'Θ'],
  ['\\Lambda', 'Λ'],
  ['\\Xi', 'Ξ'],
  ['\\Pi', 'Π'],
  ['\\Sigma', 'Σ'],
  ['\\Upsilon', 'Υ'],
  ['\\Phi', 'Φ'],
  ['\\Psi', 'Ψ'],
  ['\\Omega', 'Ω'],
]

export function applyTexSymbolCommandsToUnicode(text: string): string {
  if (text == null || text === '') return ''
  let out = text
  for (const [cmd, sym] of TEX_SYMBOL_REPLACEMENTS) {
    out = out.split(cmd).join(sym)
  }
  return out
}

/**
 * Chuẩn hóa chuỗi hiển thị khi font thường không có Mathematical Alphanumeric.
 * Không flatten superscript/subscript Unicode do parser PDF đã cố giữ các ký hiệu này.
 */
export function normalizeMathematicalUnicodeForEditor(text: string): string {
  if (text == null || text === '') return ''
  let out = ''
  for (const ch of text) {
    const cp = ch.codePointAt(0)
    if (cp === undefined) {
      out += ch
      continue
    }
    const mapped =
      mapMathLatinLetter(cp) ??
      mapMathAlphanumericDigit(cp)
    if (mapped !== null) {
      out += mapped
      continue
    }
    if (cp === 0xfffd) {
      continue
    }
    out += ch
  }
  return out
}

/**
 * Chuẩn hóa một câu hỏi sau import (content + options dạng mảng hoặc object).
 */
export function normalizeImportedExamQuestion<T extends Record<string, unknown>>(q: T): T {
  const content = normalizeMathematicalUnicodeForEditor(String(q.content ?? ''))
  const rawOpts = q.options
  let options: unknown = rawOpts
  if (Array.isArray(rawOpts)) {
    options = rawOpts.map((o) => {
      const row = o as { id?: unknown; text?: unknown; value?: unknown }
      return {
        ...row,
        text: normalizeMathematicalUnicodeForEditor(
          String(row.text ?? row.value ?? ''),
        ),
      }
    })
  } else if (rawOpts != null && typeof rawOpts === 'object') {
    const src = rawOpts as Record<string, unknown>
    const next: Record<string, unknown> = { ...src }
    for (const k of Object.keys(src)) {
      next[k] = normalizeMathematicalUnicodeForEditor(String(src[k] ?? ''))
    }
    options = next
  }

  const nextQ = { ...q, content, options } as T & Record<string, unknown>
  return nextQ as T
}
