"""Utility modules for PDF parsing."""
from .section_detector import detect_sections, has_essay_section, is_essay_only, Section, SectionKind
from .latex_converter import (
    convert_to_latex,
    convert_unicode_to_latex,
    preserve_latex_delimiters,
)

__version__ = "1.0.0"

__all__ = [
    "detect_sections",
    "has_essay_section",
    "is_essay_only",
    "Section",
    "SectionKind",
    "convert_to_latex",
    "convert_unicode_to_latex",
    "preserve_latex_delimiters",
]