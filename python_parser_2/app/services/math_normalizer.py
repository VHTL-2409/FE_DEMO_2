"""Chuẩn hóa toán an toàn — giữ ký hiệu, thứ tự token."""

from __future__ import annotations

import re
import unicodedata

_MINUS = frozenset({'\u2212', '\u2013', '\u2014', '\ufe63', '\uff0d'})


def normalize_unicode_minus(text: str) -> str:
    return ''.join('-' if ch in _MINUS else ch for ch in text)


def collapse_whitespace(text: str) -> str:
    text = unicodedata.normalize('NFC', text)
    text = re.sub(r'[ \t]+', ' ', text)
    text = re.sub(r'\n{3,}', '\n\n', text)
    return text.strip()


def normalize_exam_text(text: str) -> str:
    if not text:
        return text
    t = normalize_unicode_minus(text)
    t = collapse_whitespace(t)
    return t
