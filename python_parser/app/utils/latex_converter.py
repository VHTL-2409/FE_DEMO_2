"""
latex_converter.py — Convert math text to LaTeX format for frontend rendering.

This module converts extracted PDF math content to LaTeX notation
that KaTeX/MathJax can render on the frontend.

LaTeX Delimiters used:
  - Inline: $...$
  - Block/Display: $$...$$
"""

from __future__ import annotations

import re
from typing import Optional

# Giống text_normalizer — tránh bọc cả dòng tiếng Việt trong $...$ (KaTeX nuốt khoảng trắng).
_VIETNAMESE_CHARS_RE = re.compile(
    r"[àáảãạăằẳẵặâầẩẫậèéẻẽẹêềểễệìíỉĩịòóỏõọôồổốộơờởỡợùúủũụưừửữựỳýỷỹỵđĐ]"
)


# ─── Unicode to LaTeX Symbol Map ───────────────────────────────────────────────

UNICODE_TO_LATEX = {
    # Basic operators
    "−": "-",
    "×": r"\times",
    "÷": r"\div",
    "·": r"\cdot",
    "±": r"\pm",
    "∓": r"\mp",

    # Roots
    "√": r"\sqrt",
    "∛": r"\sqrt[3]",
    "∜": r"\sqrt[4]",

    # Calculus
    "∫": r"\int",
    "∑": r"\sum",
    "∏": r"\prod",
    "∂": r"\partial",

    # Set theory
    "∈": r"\in",
    "∉": r"\notin",
    "⊂": r"\subset",
    "⊃": r"\supset",
    "⊆": r"\subseteq",
    "⊇": r"\supseteq",
    "⊄": r"\not\subset",
    "⊅": r"\not\supset",
    "∪": r"\cup",
    "∩": r"\cap",
    "∅": r"\emptyset",

    # Number sets
    "ℝ": r"\mathbb{R}",
    "ℤ": r"\mathbb{Z}",
    "ℕ": r"\mathbb{N}",
    "ℚ": r"\mathbb{Q}",
    "ℂ": r"\mathbb{C}",
    "ℼ": r"\mathbb{I}",

    # Arrows
    "→": r"\rightarrow",
    "←": r"\leftarrow",
    "↔": r"\leftrightarrow",
    "⇒": r"\Rightarrow",
    "⇐": r"\Leftarrow",
    "⇔": r"\Leftrightarrow",
    "↦": r"\mapsto",
    "⟶": r"\longrightarrow",
    "⟵": r"\longleftarrow",

    # Comparisons
    "≥": r"\geq",
    "≤": r"\leq",
    "≠": r"\neq",
    "≈": r"\approx",
    "≡": r"\equiv",
    "∝": r"\propto",

    # Logic
    "∀": r"\forall",
    "∃": r"\exists",
    "∄": r"\nexists",
    "∧": r"\land",
    "∨": r"\lor",
    "¬": r"\neg",

    # Geometry
    "∠": r"\angle",
    "°": r"^\circ",
    "′": r"'",
    "″": r"''",
    "⊥": r"\perp",
    "∥": r"\parallel",

    # Greek letters
    "α": r"\alpha",
    "β": r"\beta",
    "γ": r"\gamma",
    "δ": r"\delta",
    "ε": r"\epsilon",
    "ζ": r"\zeta",
    "η": r"\eta",
    "θ": r"\theta",
    "ι": r"\iota",
    "κ": r"\kappa",
    "λ": r"\lambda",
    "μ": r"\mu",
    "ν": r"\nu",
    "ξ": r"\xi",
    "ο": r"\omicron",
    "π": r"\pi",
    "ρ": r"\rho",
    "σ": r"\sigma",
    "τ": r"\tau",
    "υ": r"\upsilon",
    "φ": r"\phi",
    "χ": r"\chi",
    "ψ": r"\psi",
    "ω": r"\omega",

    # Uppercase Greek
    "Γ": r"\Gamma",
    "Δ": r"\Delta",
    "Θ": r"\Theta",
    "Λ": r"\Lambda",
    "Ξ": r"\Xi",
    "Π": r"\Pi",
    "Σ": r"\Sigma",
    "Υ": r"\Upsilon",
    "Φ": r"\Phi",
    "Ψ": r"\Psi",
    "Ω": r"\Omega",

    # Other symbols
    "∞": r"\infty",
    "∇": r"\nabla",
    "⊕": r"\oplus",
    "⊗": r"\otimes",
    "⊙": r"\odot",
    "⊖": r"\ominus",
    "‰": r"\permil",
    "‱": r"\perten thousand",
    "⋮": r"\vdots",
    "⋯": r"\cdots",
    "⋰": r"\udots",
    "⋱": r"\ddots",

    # Brackets
    "⟨": r"\langle",
    "⟩": r"\rangle",
    "⟦": r"\llbracket",
    "⟧": r"\rrbracket",

    # Dots
    "…": r"\ldots",
    "⋯": r"\cdots",
}


# Superscript/subscript maps
SUPERSCRIPT_TO_LATEX = {
    "⁰": "^0", "¹": "^1", "²": "^2", "³": "^3", "⁴": "^4",
    "⁵": "^5", "⁶": "^6", "⁷": "^7", "⁸": "^8", "⁹": "^9",
    "⁺": "^+", "⁻": "^-", "⁼": "^=", "ⁿ": "^n",
}

SUBSCRIPT_TO_LATEX = {
    "₀": "_0", "₁": "_1", "₂": "_2", "₃": "_3", "₄": "_4",
    "₅": "_5", "₆": "_6", "₇": "_7", "₈": "_8", "₉": "_9",
}


def convert_unicode_to_latex(text: str) -> str:
    """
    Convert Unicode math symbols to LaTeX notation.

    Example:
      "x² + y² = r²" → "x^2 + y^2 = r^2"
      "∀x ∈ ℝ" → "\\forall x \\in \\mathbb{R}"
      "x ≤ y" → "x \\leq y"
    """
    if not text:
        return text

    result = text

    # Convert superscripts first (before other symbols)
    for uni, latex in SUPERSCRIPT_TO_LATEX.items():
        result = result.replace(uni, latex)

    # Convert subscripts
    for uni, latex in SUBSCRIPT_TO_LATEX.items():
        result = result.replace(uni, latex)

    # Convert other Unicode math symbols
    for uni, latex in UNICODE_TO_LATEX.items():
        result = result.replace(uni, latex)

    return result


def has_math_delimiters(text: str) -> bool:
    """Check if text already contains LaTeX delimiters."""
    return bool(re.search(r'\$[^$]+\$|\\\(|\\\[|\\begin\{', text))


def wrap_inline_math(text: str) -> str:
    """
    Wrap standalone math expressions with $ delimiters for KaTeX.

    Detects math content (formulas, equations) and wraps them.
    """
    if not text:
        return text

    # Skip if already has delimiters
    if has_math_delimiters(text):
        return text

    result = text

    # Pattern for equations: expressions with =, ≤, ≥, etc.
    # Wrap patterns like "x² + 1 = 0" or "a ≤ b"
    equation_pattern = r'(\S+\s*[=≤≥≠])\s*\S+'

    # Pattern for fractions: (a)/(b)
    fraction_pattern = r'\(([^)]+)\)\s*/\s*\(([^)]+)\)'

    # Pattern for square roots
    sqrt_pattern = r'√\s*([a-zA-Z0-9^_()]+)'

    # Convert existing Unicode sqrt to LaTeX
    result = re.sub(sqrt_pattern, r'\\sqrt{\1}', result)

    # Convert fraction notation (a)/(b) to \frac{a}{b}
    def replace_frac(m):
        numerator = m.group(1).strip()
        denominator = m.group(2).strip()
        return rf'\frac{{{numerator}}}{{{denominator}}}'

    result = re.sub(fraction_pattern, replace_frac, result)

    # Wrap expressions that look like standalone equations
    # Only wrap if the line is primarily math
    lines = result.split('\n')
    processed_lines = []

    for line in lines:
        stripped = line.strip()

        # Skip if empty or short
        if not stripped or len(stripped) < 3:
            processed_lines.append(line)
            continue

        # Skip if already has $ delimiters
        if '$' in stripped:
            processed_lines.append(line)
            continue

        # Câu hỏi kiểu pdf_mau_1: tiếng Việt + công thức trên cùng một dòng — không bọc cả dòng.
        if _VIETNAMESE_CHARS_RE.search(stripped):
            processed_lines.append(line)
            continue

        # Check if line looks like math (contains operators and variable-like content)
        math_indicators = [
            r'[=≤≥≠±∓]',  # Comparison operators
            r'[+\-×÷]',    # Arithmetic operators
            r'[∫∑∏√]',    # Calculus/set symbols
            r'\^',         # Power notation
            r'_',          # Subscript notation
        ]

        math_count = sum(1 for p in math_indicators if re.search(p, stripped))

        # If likely math, wrap with $ (inline)
        if math_count >= 1 and len(stripped) < 200:
            # Check for common non-math patterns
            non_math_patterns = [
                r'^[A-ZÀ-ỹ][a-zà-ỹ\s]+$',  # Pure text (Vietnamese names, etc.)
                r'^[\d\s,.:;!?]+$',         # Pure numbers
            ]

            is_likely_text = any(re.match(p, stripped) for p in non_math_patterns)

            if not is_likely_text:
                # Only wrap if significant math content
                if math_count >= 2 or re.search(r'[=≤≥≠]', stripped):
                    # Avoid double-wrapping
                    if not stripped.startswith('$'):
                        line = f'${stripped}$'

        processed_lines.append(line)

    return '\n'.join(processed_lines)


def collapse_wrapped_plain_duplicate_lines(text: str) -> str:
    """
    Hai dòng liên tiếp: một dạng $...$ và một plain (nội dung tương đương) → chỉ giữ một.
    Tránh preview hiển thị hai lần (KaTeX + dòng raw).
    """
    if not text or "\n" not in text:
        return text

    def strip_inline_delims(s: str) -> str:
        t = s.strip()
        if len(t) >= 2 and t.startswith("$") and t.endswith("$") and t.count("$") == 2:
            return t[1:-1].strip()
        return t

    def norm_compact(s: str) -> str:
        return re.sub(r"\s+", "", strip_inline_delims(s))

    raw_lines = text.splitlines()
    out: list[str] = []
    i = 0
    while i < len(raw_lines):
        cur = raw_lines[i].strip()
        nxt = raw_lines[i + 1].strip() if i + 1 < len(raw_lines) else ""
        if cur and nxt and norm_compact(cur) == norm_compact(nxt):
            prefer = cur if cur.startswith("$") else nxt
            out.append(prefer)
            i += 2
            continue
        out.append(raw_lines[i])
        i += 1
    return "\n".join(out)


def collapse_duplicate_math_lines(text: str) -> str:
    """PDF/parse đôi khi nhân đôi cùng một dòng $...$ — chỉ giữ một."""
    lines = [ln.strip() for ln in text.splitlines() if ln.strip()]
    if len(lines) >= 2 and len(set(lines)) == 1:
        return lines[0]
    return text


def repair_garbled_set_option_text(text: str) -> str:
    """
    PDF Toán: tập nghiệm dạng S = {-3} bị tách thành 'S3 = -' / 'S 3 = -' / 'S3=-'.
    Chỉ áp cho chuỗi ngắn (đáp án TN), không đụng nếu đã có {...}.
    """
    t = (text or "").strip()
    if not t or len(t) > 100 or "{" in t:
        return text
    had_wrap = t.startswith("$") and t.endswith("$") and t.count("$") == 2
    inner = t[1:-1].strip() if had_wrap else t
    if "{" in inner:
        return text
    compact = re.sub(r"\s+", "", inner)
    compact = compact.replace("\u2212", "-").replace("−", "-")

    def wrap_out(body: str) -> str:
        return f"${body}$" if had_wrap else body

    m = re.match(r"^S(\d+)=-$", compact, re.I)
    if m:
        return wrap_out(f"S = {{-{m.group(1)}}}")
    m = re.match(r"^S(\d+)=$", compact, re.I)
    if m:
        return wrap_out(f"S = {{{m.group(1)}}}")
    m = re.match(r"^S=-(\d+)$", compact, re.I)
    if m:
        return wrap_out(f"S = {{-{m.group(1)}}}")
    m = re.match(r"^S=(\d+)$", compact, re.I)
    if m:
        return wrap_out(f"S = {{{m.group(1)}}}")
    return text


def convert_to_latex(text: str, mode: str = "auto") -> str:
    """
    Main entry point: convert text to LaTeX format.

    Args:
        text: Input text with Unicode math symbols
        mode: "auto" (detect and wrap), "inline" (wrap as inline), "block" (wrap as display)

    Returns:
        Text with LaTeX notation, wrapped with $ or $$ delimiters
    """
    if not text:
        return text

    text = collapse_duplicate_math_lines(text)
    text = repair_garbled_set_option_text(text)

    # Step 1: Convert Unicode to LaTeX symbols
    result = convert_unicode_to_latex(text)

    # Step 2: Wrap math expressions
    if mode == "inline":
        result = wrap_inline_math(result)
    elif mode == "block":
        # For display math, wrap entire paragraphs
        lines = result.split('\n')
        for i, line in enumerate(lines):
            stripped = line.strip()
            if stripped and not stripped.startswith('$$') and not stripped.startswith('$'):
                if _VIETNAMESE_CHARS_RE.search(stripped):
                    continue
                if re.search(r'[=≤≥≠∫∑∏√]', stripped):
                    lines[i] = f'$${stripped}$$'
        result = '\n'.join(lines)
    else:  # auto
        result = wrap_inline_math(result)

    result = collapse_wrapped_plain_duplicate_lines(result)

    return result


def extract_latex_parts(text: str) -> dict:
    """
    Extract and categorize LaTeX parts from mixed content.

    Returns:
        {
            "plain": [...],      # Plain text parts
            "inline_math": [...], # $...$ parts
            "block_math": [...],  # $$...$$ parts
        }
    """
    parts = {
        "plain": [],
        "inline_math": [],
        "block_math": [],
    }

    # Split by math delimiters
    # First, extract block math ($$...$$)
    block_pattern = re.compile(r'\$\$([\s\S]*?)\$\$', re.MULTILINE)
    inline_pattern = re.compile(r'\$([^$]+)\$')

    # Remove block math, store separately
    text_without_block = block_pattern.sub(r'\x00BLOCK\x00', text)

    # Remove inline math, store separately
    text_without_any = inline_pattern.sub(r'\x00INLINE\x00', text_without_block)

    # Split remaining text by newlines for plain parts
    for line in text_without_any.split('\n'):
        if line.strip():
            parts["plain"].append(line)

    return parts


def preserve_latex_delimiters(text: str) -> str:
    """
    Ensure existing LaTeX delimiters are properly formatted.

    - Fix unclosed $ signs
    - Normalize spacing around delimiters
    """
    if not text:
        return text

    # Fix unclosed $ at end of expression
    # e.g., "x^2 $" → "x^2$"
    text = re.sub(r'([^\s])\s+\$', r'\1$', text)

    # Fix unopened $ at start
    # e.g., "$ x^2" → "$\displaystyle x^2$" or just "$x^2"
    text = re.sub(r'\$\s+([^\s])', r'$\1', text)

    # Normalize double $$ for display math
    # Replace lone $...$ that contains display commands
    def fix_display_math(m):
        content = m.group(1)
        # If contains \frac, \sqrt, \sum, \int at start, treat as display
        if re.match(r'^\\frac|^\\sqrt|^\\sum|^\\int|^\\begin', content.strip()):
            return f'$${content}$$'
        return m.group(0)

    text = re.sub(r'\$([^\$]+)\$', fix_display_math, text)

    return text
