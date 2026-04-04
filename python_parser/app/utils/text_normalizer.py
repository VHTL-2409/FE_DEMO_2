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
    """
    # Pattern: option letter followed by content, repeating on same line
    # Matches "A. content" or "A content" where next letter starts
    merged_re = re.compile(
        r"(?<!\n)([A-D])\.\s*([^\nA-D]{1,60}?)(?=\s+[A-D]\.|\s*$)",
        re.MULTILINE
    )

    def replace_one(m: re.Match) -> str:
        letter = m.group(1)
        content = m.group(2).strip()
        return f"{letter}. {content}"

    result = merged_re.sub(replace_one, text)

    # If nothing changed, try simpler pattern
    if result == text:
        # "A. x² B. y² C." — split on "B. " "C. " "D. "
        result = re.sub(r"\s+([B-D])\. ", r"\n\1. ", text)
        result = re.sub(r"^([A-D])\.\s*", r"\1. ", result, flags=re.MULTILINE)

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
