"""Mô hình trung gian trước khi map sang schema API."""

from __future__ import annotations

from dataclasses import dataclass, field
from enum import Enum


class QuestionTypeGuess(str, Enum):
    MULTIPLE_CHOICE = 'multiple_choice'
    ESSAY = 'essay'
    FILL_BLANK = 'fill_blank'
    UNKNOWN = 'unknown'


@dataclass
class RawQuestionBlock:
    number: int
    page: int
    raw_text: str
    normalized_text: str
    type_guess: QuestionTypeGuess = QuestionTypeGuess.UNKNOWN
    options: dict[str, str] = field(default_factory=dict)
    answer: str | None = None
    confidence: float = 0.5
    issues: list[str] = field(default_factory=list)
    section: str | None = None
    section_kind: str | None = None
