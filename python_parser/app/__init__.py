"""Exam Parser — Template-based PDF exam import service."""

from .exam_document_parse import (
    parse_exam_document,
    summarize_question_types,
    SUPPORTED_EXTENSIONS,
)
from .schemas import ParseResponse, TemplateType

__version__ = "1.0.0"

__all__ = [
    "SUPPORTED_EXTENSIONS",
    "ParseResponse",
    "TemplateType",
    "parse_exam_document",
    "summarize_question_types",
]
