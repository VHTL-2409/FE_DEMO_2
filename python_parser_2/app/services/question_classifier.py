"""
Phân loại câu hỏi generic — tín hiệu từ options, section, mật độ, pattern fill-in.
"""

from __future__ import annotations

import re

from ..models.parsed_question import QuestionTypeGuess
from .option_extractor import OptionExtractionResult


_FILL_HINTS = re.compile(
    r'_{3,}|…{2,}|\.{4,}|\[\s*\]|\\fill',
)


def classify(
    block_text: str,
    opt_result: OptionExtractionResult,
    section_kind: str | None,
) -> tuple[QuestionTypeGuess, float, list[str]]:
    notes: list[str] = []
    sk = (section_kind or '').lower()

    if 'essay' in sk or 'tự luận' in sk:
        return QuestionTypeGuess.ESSAY, 0.75, notes + ['section_essay']

    if opt_result.options and len(opt_result.options) >= 2:
        conf = min(0.95, 0.55 + 0.1 * len(opt_result.options))
        notes.append('mcq_option_markers')
        return QuestionTypeGuess.MULTIPLE_CHOICE, conf, notes

    if _FILL_HINTS.search(block_text):
        notes.append('fill_blank_pattern')
        return QuestionTypeGuess.FILL_BLANK, 0.55, notes

    if len(block_text) > 400 and not opt_result.options:
        notes.append('long_text_no_options')
        return QuestionTypeGuess.ESSAY, 0.45, notes

    notes.append('insufficient_signals')
    return QuestionTypeGuess.UNKNOWN, 0.35, notes
