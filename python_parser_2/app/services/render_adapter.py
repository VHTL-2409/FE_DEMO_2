"""Gợi ý latexContent / contentType cho FE — không phụ thuộc template cũ."""

from __future__ import annotations

import re

_SYM = {
    '∅': r'\emptyset',
    '×': r'\times',
    '÷': r'\div',
    '·': r'\cdot',
    '≤': r'\leq',
    '≥': r'\geq',
    '≠': r'\neq',
    '∞': r'\infty',
    '∈': r'\in',
    'π': r'\pi',
}


def to_latex_hint(text: str) -> str:
    if not text:
        return ''
    return ''.join(_SYM.get(ch, ch) for ch in text)


def classify_content_type(text: str, latex: str | None) -> str:
    if latex and '\\' in latex:
        return 'math' if not re.search(
            r'[àáảãạăằẳẵặâầẩẫậèéẻẽẹêềểễệìíỉĩịòóỏõọôồổốộơờởỡợùúủũụưừửữựỳýỷỹỵđĐ]',
            text,
        ) else 'mixed'
    if re.search(r'[∅×÷≤≥≠∞∈π²³⁰¹⁴⁵⁶⁷⁸⁹^=+\-*/(){}\[\]]', text):
        return 'mixed'
    return 'plain'
