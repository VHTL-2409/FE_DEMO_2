"""Kết quả trung gian và debug cho một lần ingest."""

from __future__ import annotations

from dataclasses import dataclass, field
from typing import Any


@dataclass
class IngestionDebug:
    reconstructed_row_samples: list[dict[str, Any]] = field(default_factory=list)
    segment_hints: list[str] = field(default_factory=list)
    question_boundaries: list[dict[str, Any]] = field(default_factory=list)
    classification_notes: list[str] = field(default_factory=list)
    option_extraction_log: list[dict[str, Any]] = field(default_factory=list)
    answer_key_hits: dict[str, Any] = field(default_factory=dict)
    raw_text_preview: str = ''
    normalized_preview: str = ''
