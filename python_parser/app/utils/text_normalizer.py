"""
text_normalizer.py — Normalize extracted PDF text for reliable parsing.
"""

from __future__ import annotations

import re
import unicodedata
from typing import Optional

from .pdf_reader import TextBlock, TextSpan


# ─── Fullwidth character map ──────────────────────────────────────────────────

_FW_MAP = {
    "\uff01": "!", "\uff02": '"', "\uff03": "#", "\uff04": "$",
    "\uff05": "%", "\uff06": "&", "\uff07": "'", "\uff08": "(",
    "\uff09": ")", "\uff0a": "*", "\uff0b": "+", "\uff0c": ",",
    "\uff0d": "-", "\uff0e": ".", "\uff0f": "/",
    "\uff1a": ":", "\uff1b": ";", "\uff1c": "<", "\uff1d": "=",
    "\uff1e": ">", "\uff1f": "?", "\uff20": "@",
    "\uff3b": "[", "\uff3c": "\\", "\uff3d": "]", "\uff3e": "^",
    "\uff3f": "_", "\uff40": "`",
    "\uff5b": "{", "\uff5c": "|", "\uff5d": "}", "\uff5e": "~",
    "\uff0e": ".",  # fullwidth period
}

# Unicode superscript / subscript → normal
SUPERSCRIPT_MAP = {
    "⁰": "0", "¹": "1", "²": "2", "³": "3", "⁴": "4",
    "⁵": "5", "⁶": "6", "⁷": "7", "⁸": "8", "⁹": "9",
    "⁺": "+", "⁻": "-", "⁼": "=", "ⁿ": "n",
}
SUBSCRIPT_MAP = {
    "₀": "0", "₁": "1", "₂": "2", "₃": "3", "₄": "4",
    "₅": "5", "₆": "6", "₇": "7", "₈": "8", "₉": "9",
}

# Math symbols to PRESERVE (never strip or modify)
_MATH_SYMBOLS = frozenset({
    "−", "×", "÷", "·", "±", "∓", "√", "∛", "∜",
    "∫", "∑", "∏", "∂", "∆", "∇",
    "∈", "∉", "⊂", "⊃", "⊆", "⊇", "⊄", "⊅",
    "∪", "∩", "∅", "∞",
    "ℝ", "ℤ", "ℕ", "ℚ", "ℂ", "ℼ",
    "→", "←", "↔", "⇒", "⇐", "⇔",
    "≥", "≤", "≠", "≈", "≡", "∝",
    "⊕", "⊗", "⊙", "⊖",
    "∀", "∃", "∄", "∧", "∨", "¬", "⊤", "⊥",
    "°", "′", "″", "‰", "‱",
    "⊂⊃", "⊆⊇",
})

# Greek letters to PRESERVE
_GREEK_SYMBOLS = frozenset({
    "α", "β", "γ", "δ", "ε", "ζ", "η", "θ", "ι", "κ",
    "λ", "μ", "ν", "ξ", "ο", "π", "ρ", "σ", "τ", "υ",
    "φ", "χ", "ψ", "ω",
    "Α", "Β", "Γ", "Δ", "Ε", "Ζ", "Η", "Θ", "Ι", "Κ",
    "Λ", "Μ", "Ν", "Ξ", "Ο", "Π", "Ρ", "Σ", "Τ", "Υ",
    "Φ", "Χ", "Ψ", "Ω",
})


def normalize(text: str) -> str:
    """
    Normalize raw PDF text: fix fullwidth, strip garbage chars, fix line breaks.
    """
    if not text:
        return text

    # Normalize unicode
    text = unicodedata.normalize("NFC", text)

    # Fix fullwidth punctuation
    for fw, normal in _FW_MAP.items():
        text = text.replace(fw, normal)

    # Replace UTF-8 BOM / replacement chars
    text = text.replace("\ufeff", "")
    text = text.replace("\ufffd", "")

    # Fix broken bars / dashes
    text = re.sub(r"[_\u2015\u2014\u2500]{2,}", "---", text)

    # Fix multiple spaces
    text = re.sub(r"[ \t]{2,}", " ", text)

    # Collapse multiple newlines
    text = re.sub(r"\n{3,}", "\n\n", text)

    return text.strip()


def sanitize_word_equation_pua(text: str, *, strip_private_use: bool = True) -> str:
    """
    Word/Equation → PDF thường nhét ký tự vào vùng Private Use (U+F000–U+F8FF).

    - strip_private_use=True: xóa các ký tự PUA (dùng cho phần đề — bỏ khung rỗng).
    - strip_private_use=False: chỉ đổi vô cực; giữ PUA trong đáp án để không mất phân số Word.
    """
    if not text:
        return text
    pua_infinity = {
        "\uf0a5": "∞",
        "\uf0b5": "∞",
    }
    out: list[str] = []
    for ch in text:
        if ch in pua_infinity:
            out.append(pua_infinity[ch])
            continue
        o = ord(ch)
        if 0xF000 <= o <= 0xF8FF:
            if strip_private_use:
                out.append(" ")
            else:
                out.append(ch)
            continue
        out.append(ch)
    s = "".join(out)
    s = re.sub(r" {2,}", " ", s)
    s = re.sub(r" *\n *", "\n", s)
    return s.strip()


def unsuperscript(text: str) -> str:
    """Replace Unicode superscripts with normal characters."""
    for sup, normal in SUPERSCRIPT_MAP.items():
        text = text.replace(sup, normal)
    return text


def unsubscript(text: str) -> str:
    """Replace Unicode subscripts with normal characters."""
    for sub, normal in SUBSCRIPT_MAP.items():
        text = text.replace(sub, normal)
    return text


def split_merged_options(text: str) -> str:
    """
    Split merged option lines like:
    "A. x² B. y² C. z² D. t²"
    into separate lines.

    pdf_mau_1: đề và 4 đáp án thường nằm một dòng hoặc cách nhau bởi | (cột).
    """
    # Cột song song trong PDF: "... A. xxx | B. xxx"
    text = re.sub(
        r"[ \t]*\|[ \t]*(?=[A-D](?:[\.．]|[\)\:])\s)",
        "\n",
        text,
        flags=re.IGNORECASE,
    )

    # Đề bài kết thúc bằng : hoặc ; rồi tới A./B. trên cùng dòng → xuống dòng trước đáp án
    text = re.sub(
        r"([:;])([ \t]+)([A-D])([\.．])(\s+)",
        r"\1\n\3\4\5",
        text,
        flags=re.IGNORECASE,
    )

    # Pattern: option letter followed by content, repeating on same line
    merged_re = re.compile(
        r"(?<!\n)([A-Da-d])([\.．])\s*([^\nA-D]{1,120}?)(?=\s+[A-Da-d](?:[\.．]|[\)\:])\s|\s*$)",
        re.MULTILINE,
    )

    def replace_one(m: re.Match[str]) -> str:
        letter = m.group(1).upper()
        dot = m.group(2)
        content = m.group(3).strip()
        return f"{letter}{dot} {content}"

    result = merged_re.sub(replace_one, text)

    # "A. x² B. y² C." — split on "B. " "C. " "D. "
    result = re.sub(
        r"\s+([B-Db-d])([\.．])\s+",
        lambda m: "\n" + m.group(1).upper() + m.group(2) + " ",
        result,
    )
    result = re.sub(
        r"^([A-Da-d])([\.．])\s*",
        lambda m: m.group(1).upper() + m.group(2) + " ",
        result,
        flags=re.MULTILINE,
    )

    return result


def fix_dashed_options(text: str) -> str:
    """
    Fix options that were extracted as separate dashed lines, e.g.:
    A. Đáp án A
    __________
    B. Đáp án B
    __________
    """
    # Remove isolated dash lines between option content
    text = re.sub(r"([A-D])\.\s*([^\n]+)\n[-–—_]{3,}\n", r"\1. \2\n", text)
    return text


def reconstruct_math_from_spans(
    blocks: list[TextBlock],
    page_height: float,
) -> list[TextBlock]:
    """
    Reconstruct math expressions split across vertical spans.
    Uses Y-proximity to group superscripts/subscripts with their base.

    Example: x [bbox y=200] then ² [bbox y=188] → x²
    """
    if not blocks:
        return blocks

    # Group spans from all blocks
    all_spans: list[tuple[TextBlock, TextSpan, float]] = []
    for block in blocks:
        for span in block.spans:
            all_spans.append((block, span, span.bbox[1]))  # y0

    all_spans.sort(key=lambda x: x[2])  # sort by y position

    result_blocks = []
    for block in blocks:
        spans = block.spans
        if not spans:
            result_blocks.append(block)
            continue

        # Sort spans by x position within the block
        spans = sorted(spans, key=lambda s: s.bbox[0])
        merged_lines: list[list[TextSpan]] = []
        current_line: list[TextSpan] = [spans[0]]

        for i in range(1, len(spans)):
            span = spans[i]
            prev_span = current_line[-1]

            # If on roughly same Y (within 5 pts), same line
            if abs(span.bbox[1] - prev_span.bbox[1]) < 5:
                current_line.append(span)
            else:
                merged_lines.append(current_line)
                current_line = [span]

        if current_line:
            merged_lines.append(current_line)

        # Build reconstructed text from merged lines
        reconstructed_lines = []
        for line in merged_lines:
            line_text = ""
            for span in sorted(line, key=lambda s: s.bbox[0]):
                t = span.text.strip()
                if not t:
                    continue

                # Check if this looks like a superscript
                is_sup = (
                    len(t) <= 3
                    and (t in SUPERSCRIPT_MAP or re.match(r"^[\d+\-=n]+$", t))
                    and span.font_size < 8
                )

                # Check if this looks like a subscript
                is_sub = (
                    len(t) <= 3
                    and (t in SUBSCRIPT_MAP or re.match(r"^[\dn]+$", t))
                    and span.font_size < 8
                )

                if is_sup:
                    line_text += "^" + unsuperscript(t)
                elif is_sub:
                    line_text += "_" + unsubscript(t)
                else:
                    if line_text and not line_text.endswith(" ") and not line_text.endswith("("):
                        line_text += " "
                    line_text += t

            reconstructed_lines.append(line_text.strip())

        new_text = "\n".join(reconstructed_lines)
        new_block = TextBlock(
            bbox=block.bbox,
            text=new_text,
            spans=block.spans,
            page_num=block.page_num,
            block_type=block.block_type,
        )
        result_blocks.append(new_block)

    return result_blocks


def fix_question_number_spacing(text: str) -> str:
    """Ensure question numbers are on their own line or followed by a period."""
    # "Câu1:" → "Câu 1:"
    text = re.sub(r"Câu\s*(\d)", r"Câu \1", text)
    # "Câu 1" → "Câu 1."
    text = re.sub(r"Câu\s+(\d+)(?:\s|$|:)", r"Câu \1. ", text)
    # "Question 1" → "Question 1:"
    text = re.sub(r"Question\s+(\d+)\s*[:\.]?\s*(?=[A-Z(])", r"Question \1: ", text)
    # "Bài 1." → "Câu 1."
    text = re.sub(r"Bài\s+(\d+)", r"Câu \1", text)
    return text


def remove_header_footer(text: str, page_height: float = 792) -> str:
    """Remove likely header/footer lines based on position."""
    # Lines in top 5% or bottom 5% of page are likely headers/footers
    lines = text.split("\n")
    result = []
    for line in lines:
        # Simple heuristic: skip common header/footer patterns
        line_upper = line.strip()
        if re.match(r"^(SỞ|TRƯỜNG|KỲ THI|ĐỀ THI)\s+", line_upper):
            continue
        if re.match(r"^\s*(Năm học|Môn|Thời gian)\s*:?\s*\d", line_upper):
            continue
        if re.match(r"^\s*Page\s+\d+\s*$", line_upper, re.IGNORECASE):
            continue
        result.append(line)
    return "\n".join(result)


def normalize_full(text: str, blocks: Optional[list[TextBlock]] = None) -> str:
    """
    Full normalization pipeline.
    """
    text = normalize(text)
    text = fix_question_number_spacing(text)
    text = split_merged_options(text)
    text = fix_dashed_options(text)
    return text


def fix_pdf_ambiguous_exponents_to_unicode(text: str) -> str:
    """
    PDF (đặc biệt pdf_mau_3 / Word→PDF) thường mất chỉ số trên: x² thành x2.

    Heuristic bảo thủ: chỉ chữ biến thường gặp (x,y,z,t,n) + một chữ số 2–9,
    khi sau đó là toán tử / dấu câu / kết dòng — tránh khớp mã câu kiểu \"Câu 2\".
    """
    if not text:
        return text
    sup_map = {
        "2": "²",
        "3": "³",
        "4": "⁴",
        "5": "⁵",
        "6": "⁶",
        "7": "⁷",
        "8": "⁸",
        "9": "⁹",
    }

    def repl(m: re.Match[str]) -> str:
        d = m.group(2)
        sup = sup_map.get(d)
        if not sup:
            return m.group(0)
        return m.group(1) + sup

    return re.sub(
        r"(?<![A-Za-z0-9À-ỹà-ỹ])([xyztn])([2-9])(?=\s*[-+×÷*,=)\]\s]|$)",
        repl,
        text,
        flags=re.IGNORECASE,
    )


# Word / PDF → Unicode: map trước khi xóa vùng PUA (tránh mất {}, ∅)
_PDF_PUA_MATH_GLYPH_MAP: tuple[tuple[str, str], ...] = (
    ("\uf0c6", "\u2205"),
    ("\uf0a5", "\u221e"),
    ("\uf0b5", "\u221e"),
    ("\uf07b", "{"),
    ("\uf07d", "}"),
)


def map_pdf_private_use_math_glyphs(text: str) -> str:
    """Đổi một số glyph Private Use (Word→PDF) sang ký hiệu Unicode tương đương."""
    if not text:
        return text
    for old, new in _PDF_PUA_MATH_GLYPH_MAP:
        text = text.replace(old, new)
    return text


def normalize_mcq_stem_display(text: str) -> str:
    """
    Chuẩn hóa stem MCQ từ word-level PDF (template_01): PUA → Unicode, x2→x²,
    không flatten superscript Unicode (giữ ² cho FE / convert_to_latex).
    """
    if not text:
        return text
    text = unicodedata.normalize("NFC", text)
    for fw, normal in _FW_MAP.items():
        text = text.replace(fw, normal)
    text = text.replace("\ufeff", "")
    text = text.replace("\ufffd", "")
    text = map_pdf_private_use_math_glyphs(text)
    text = re.sub(r"[\uf000-\uf0ff]", "", text)
    text = fix_pdf_ambiguous_exponents_to_unicode(text)
    text = fix_question_number_spacing(text)
    text = re.sub(r"[ \t]{2,}", " ", text)
    return text.strip()


_VIETNAMESE_LETTER_RE = re.compile(
    r"[àáảãạăằẳẵặâầẩẫậèéẻẽẹêềểễệìíỉĩịòóỏõọôồổốộơờởỡợùúủũụưừửữựỳýỷỹỵđĐ]"
)
_OPTION_LETTER_LINE_RE = re.compile(r"^[A-Da-d]\s*[\.)．:]")
_MATHISH_CHAR_RE = re.compile(r"[\d+\-−×÷*/=().,;:^√∫_{}\\]")


def collapse_vertical_math_fragments(text: str) -> str:
    """
    PDF kiểu Word / pdf_mau_3: một phân số hoặc biểu thức bị tách thành nhiều dòng,
    mỗi dòng vài ký tự → hiển thị dọc trong textarea.

    Gom các dòng \"mảnh\" liên tiếp (ngắn, không tiếng Việt, không marker A./B.)
    thành một dòng, cách nhau bằng khoảng trắng.
    """
    if not text or "\n" not in text:
        return text

    lines = text.split("\n")
    out: list[str] = []
    buf: list[str] = []

    def flush_buf() -> None:
        if not buf:
            return
        if len(buf) >= 2:
            out.append(" ".join(buf))
        else:
            out.extend(buf)
        buf.clear()

    for raw in lines:
        line = raw.rstrip("\r")
        s = line.strip()
        if not s:
            flush_buf()
            out.append("")
            continue

        is_frag = (
            len(s) <= 16
            and not _VIETNAMESE_LETTER_RE.search(s)
            and not s.lower().startswith("câu")
            and not _OPTION_LETTER_LINE_RE.match(s)
            and (len(s) <= 3 or bool(_MATHISH_CHAR_RE.search(s)))
        )
        if is_frag:
            buf.append(s)
        else:
            flush_buf()
            out.append(s)

    flush_buf()
    return "\n".join(out).strip()


_LONG_LATIN_RUN_RE = re.compile(r"[a-zA-Z]{5,}")


def _is_math_bridge_line(s: str) -> bool:
    """
    Dòng chỉ chứa mảnh công thức (không tiếng Việt), nằm giữa hai câu tiếng Việt
    trong đề pdf_mau_1 — ví dụ '1 x 5 + =' giữa 'Phương trình 3x' và 'có nghiệm'.
    """
    s = s.strip()
    if not s:
        return False
    if _VIETNAMESE_LETTER_RE.search(s):
        return False
    if _OPTION_LETTER_LINE_RE.match(s):
        return False
    low = s.lower()
    if low.startswith("câu") or low.startswith("bài "):
        return False
    if len(s) > 88:
        return False
    if len(s) <= 16:
        return len(s) <= 3 or bool(_MATHISH_CHAR_RE.search(s))
    if not _MATHISH_CHAR_RE.search(s):
        return False
    return _LONG_LATIN_RUN_RE.search(s) is None


def merge_math_bridge_between_vietnamese_lines(text: str) -> str:
    """
    Gom các dòng toán (bridge) liên tiếp vào cuối dòng tiếng Việt ngay phía trên,
    khi dòng tiếp theo (sau bridge) lại là tiếng Việt (vd. 'có nghiệm').
    Không gộp khi gặp dòng bắt đầu bằng A./B./C./D.
    """
    if "\n" not in text:
        return text

    lines = text.split("\n")
    out: list[str] = []
    i = 0
    n = len(lines)

    while i < n:
        raw_cur = lines[i]
        cur = raw_cur.strip()
        if (
            cur
            and _VIETNAMESE_LETTER_RE.search(cur)
            and not _OPTION_LETTER_LINE_RE.match(cur)
        ):
            j = i + 1
            math_parts: list[str] = []
            while j < n:
                raw_n = lines[j]
                nxt = raw_n.strip()
                if not nxt:
                    j += 1
                    continue
                if _VIETNAMESE_LETTER_RE.search(nxt):
                    break
                if _OPTION_LETTER_LINE_RE.match(nxt):
                    break
                low_n = nxt.lower()
                if low_n.startswith("câu") or low_n.startswith("bài "):
                    break
                if not _is_math_bridge_line(nxt):
                    break
                math_parts.append(nxt)
                j += 1
            if math_parts and j < n and _VIETNAMESE_LETTER_RE.search(lines[j].strip()):
                out.append(raw_cur.rstrip() + " " + " ".join(math_parts))
                i = j
                continue
        out.append(lines[i])
        i += 1

    return "\n".join(out)


def classify_content_type(text: str, latex: Optional[str]) -> str:
    """
    Gợi ý FE: plain | math | mixed.
    - plain: không có latexContent khả dụng, hoặc latex là cả câu tiếng Việt (không bọc $)
    - mixed: text/latex đã có delimiter toán ($, \\(, \\[)
    - math: chỉ công thức / không có chữ tiếng Việt trong latex thuần
    """
    l = (latex or "").strip()
    if not l:
        return "plain"
    t = (text or "").strip()
    if re.search(r"\$|\\\(|\\\[", t):
        return "mixed"
    if re.search(r"\$|\\\(|\\\[", l):
        return "mixed"
    # Sau khi không bọc $ cho dòng có tiếng Việt: latex ≈ text — FE render plain để giữ khoảng trắng.
    if _VIETNAMESE_LETTER_RE.search(l):
        return "plain"
    return "math"


def normalize_math_text(text: str, *, preserve_private_use: bool = False) -> str:
    """
    Normalize math exam text while PRESERVING math symbols.

    Rules:
      - Preserve: all math operators (−×÷·√∫∑∏∂∆∈∉⊂⊃∪∩ℝℤℕ→←↔≥≤≠≈)
      - Preserve: Greek letters (αβγδεθλμπσφψω)
      - Convert superscripts: ²→^2, ³→^3, ¹→^1
      - Convert: (− → (-), (−) → (-)
      - Fix fullwidth punctuation
      - Fix broken formula characters

    preserve_private_use: True = giữ U+F000–F0FF (đáp án Word Equation trong PDF).
    """
    if not text:
        return text

    # Normalize unicode (NFC)
    text = unicodedata.normalize("NFC", text)

    # Fix fullwidth punctuation
    for fw, normal in _FW_MAP.items():
        text = text.replace(fw, normal)

    # Replace UTF-8 BOM / replacement chars
    text = text.replace("\ufeff", "")
    text = text.replace("\ufffd", "")

    text = map_pdf_private_use_math_glyphs(text)

    # Remove garbled private-use chars (stem); giữ lại nếu preserve_private_use (đáp án)
    if not preserve_private_use:
        text = re.sub(r"[\uf000-\uf0ff]", "", text)

    # Convert superscripts to ASCII form (for readability in textarea)
    for sup, normal in SUPERSCRIPT_MAP.items():
        text = text.replace(sup, normal)
    for sub, normal in SUBSCRIPT_MAP.items():
        text = text.replace(sub, normal)

    # Fix broken dashes: multiple ___ or broken bars
    text = re.sub(r"[_\u2015\u2014\u2500]{2,}", "---", text)

    # Fix math expression fragments that got separated
    # "(- " → "(-", "(-)" → "(-)"
    text = re.sub(r"\(\s*-\s+", "(-", text)
    text = re.sub(r"\(\s*-", "(-", text)
    text = re.sub(r"-\s+\)", "-)", text)
    text = re.sub(r"-\s+,", "-,", text)

    # Fix multiple spaces in math expressions
    text = re.sub(r"[ \t]{2,}", " ", text)

    # pdf_mau_1: Câu1 → Câu 1.; tách đáp án dính cùng dòng / cột |
    text = fix_question_number_spacing(text)
    text = split_merged_options(text)

    # Khôi phục lũy thừa dạng chữ+số sau khi đã flatten Unicode (pdf_mau_3…)
    text = fix_pdf_ambiguous_exponents_to_unicode(text)

    # Gom dòng mảnh công thức bị PDF tách dọc
    text = collapse_vertical_math_fragments(text)

    # pdf_mau_1: khối toán giữa hai câu tiếng Việt → nối vào dòng đề
    text = merge_math_bridge_between_vietnamese_lines(text)

    return text.strip()


def has_math_content(text: str) -> bool:
    """
    Detect if text contains math formula content.
    Used to decide whether to render as IMAGE or TEXT.
    """
    if not text:
        return False

    math_chars = len(re.findall(
        r"[²³¹⁰⁴⁵⁶⁷⁸⁹⁺⁻⁼ⁿ"
        r"−×÷·±∓√∛∜∫∑∏∂∆∇"
        r"∈∉⊂⊃⊆⊇⊄⊅∪∩∅∞"
        r"ℝℤℕℚℂℼ→←↔⇒⇐⇔≥≤≠≈≡∝",
        text
    ))
    # Also check for math keywords
    math_keywords = len(re.findall(
        r"(?:sqrt|sin|cos|tan|log|lim|sum|prod|int|delta|theta|pi|alpha|beta|gamma)",
        text,
        re.IGNORECASE
    ))
    return math_chars >= 3 or math_keywords >= 2


def formula_analysis(text: str) -> dict:
    """
    Analyze math content in text and return rendering hints.
    Used by parsers to set formulaHints on ParsedQuestion.
    """
    if not text:
        return {"hasSuperscript": False, "hasGreek": False,
                "hasOperators": False, "hasFractions": False, "formulaScore": 0.0}

    superscripts = len(re.findall(r"[²³¹⁰⁴⁵⁶⁷⁸⁹⁺⁻⁼ⁿ]", text))
    greek = len(re.findall(r"[αβγδεζηθικλμνξοπρστυφχψωΑΒΓΔΕΖΗΘΙΚΛΜΝΞΟΠΡΣΤΥΦΧΨΩ]", text))
    operators = len(re.findall(
        r"[−×÷·±∓√∫∑∏∂∆∈∉⊂⊃⊆⊇∪∩→←↔⇒⇐⇔≥≤≠≈≡]", text
    ))
    fractions = len(re.findall(r"\([^)]*?\)", text))  # rough fraction detection

    # Count total math-relevant chars vs total
    math_relevant = re.findall(
        r"[²³¹⁰⁴⁵⁶⁷⁸⁹⁺⁻⁼ⁿ−×÷·±∓√∫∑∏∂∆∈∉⊂⊃⊆⊇∪∩→←↔⇒⇐⇔≥≤≠≈≡"
        r"αβγδεζηθικλμνξοπρστυφχψωΑΒΓΔΕΖΗΘΙΚΛΜΝΞΟΠΡΣΤΥΦΧΨΩ]",
        text
    )
    formula_score = min(len(math_relevant) / max(len(text), 1) * 3, 1.0)

    return {
        "hasSuperscript": superscripts >= 2,
        "hasGreek": greek >= 2,
        "hasOperators": operators >= 3,
        "hasFractions": fractions >= 2,
        "formulaScore": round(formula_score, 2),
    }
