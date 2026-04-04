"""Utility modules for PDF parsing."""
from .section_detector import detect_sections, has_essay_section, is_essay_only, Section, SectionKind

__version__ = "1.0.0"

__all__ = [
    "detect_sections",
    "has_essay_section",
    "is_essay_only",
    "Section",
    "SectionKind",
]