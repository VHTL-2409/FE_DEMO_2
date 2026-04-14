"""
Trích đáp án từ khối đáp án / bảng / DOCX *C.
"""

from __future__ import annotations

import re
from typing import Optional

_LINE_KEY = re.compile(
    r'^\s*(\d+)\s*[.)]\s*([A-Da-d])\b',
    re.MULTILINE,
)
_INLINE_NEAR = re.compile(
    r'(\d+)\s*[.)]\s*([A-Da-d])(?=\s|$|,)',
)


def extract_answer_key_table(text: str) -> dict[int, str]:
    """Parse dòng dạng '12. D' trong phần cuối tài liệu."""
    out: dict[int, str] = {}
    for m in _LINE_KEY.finditer(text):
        try:
            n = int(m.group(1))
        except ValueError:
            continue
        letter = m.group(2).upper()
        if letter in 'ABCD':
            out[n] = letter
    return out


def extract_inline_answer_run(text: str) -> dict[int, str]:
    out: dict[int, str] = {}
    for m in _INLINE_NEAR.finditer(text):
        try:
            n = int(m.group(1))
        except ValueError:
            continue
        letter = m.group(2).upper()
        if letter in 'ABCD':
            out[n] = letter
    return out


def extract_docx_star_answer(block: str) -> Optional[str]:
    """Dòng đáp án đánh dấu * trước chữ cái."""
    for line in block.splitlines():
        m = re.search(r'\*\s*([A-D])\s*[\.)．:]', line, re.IGNORECASE)
        if m:
            return m.group(1).upper()
    return None


def merge_answer_keys(*parts: dict[int, str]) -> dict[int, str]:
    merged: dict[int, str] = {}
    for p in parts:
        merged.update(p)
    return merged
