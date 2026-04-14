"""Low-level layout types for PDF word/row reconstruction."""

from __future__ import annotations

from dataclasses import dataclass, field


@dataclass
class PdfToken:
    text: str
    x0: float
    y0: float
    x1: float
    y1: float
    page: int = 1


@dataclass
class TextRow:
    page: int
    y_center: float
    tokens: list[PdfToken]
    text: str = ''


@dataclass
class ReconstructedPage:
    page_num: int
    rows: list[TextRow]


@dataclass
class LayoutDocument:
    """Toàn bộ layout đã reconstruct + text đầy đủ."""
    pages: list[ReconstructedPage]
    full_text: str
    line_page_map: list[int] = field(default_factory=list)
