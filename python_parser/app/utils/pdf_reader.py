"""
pdf_reader.py — PyMuPDF wrapper for reading PDF text blocks, images, and rendering.
"""

from __future__ import annotations

import io
from dataclasses import dataclass, field
from typing import Optional

import fitz  # PyMuPDF


@dataclass
class TextSpan:
    """A single text span with position."""
    text: str
    bbox: tuple[float, float, float, float]  # (x0, y0, x1, y1)
    font_name: str = ""
    font_size: float = 0.0


@dataclass
class TextBlock:
    """A block of text (paragraph or line)."""
    bbox: tuple[float, float, float, float]
    text: str
    spans: list[TextSpan] = field(default_factory=list)
    page_num: int = 1
    block_type: str = "text"  # "text" or "image"


@dataclass
class ImageBlock:
    """An image block in the PDF."""
    bbox: tuple[float, float, float, float]
    page_num: int = 1
    width: int = 0
    height: int = 0


class PdfReader:
    """Wrapper around PyMuPDF for PDF reading operations."""

    def __init__(self, pdf_path: str):
        self.pdf_path = pdf_path
        self._doc: Optional[fitz.Document] = None

    def _open(self) -> fitz.Document:
        if self._doc is None:
            self._doc = fitz.open(self.pdf_path)
        return self._doc

    def close(self) -> None:
        if self._doc is not None:
            self._doc.close()
            self._doc = None

    def __enter__(self):
        self._open()
        return self

    def __exit__(self, *args):
        self.close()

    @property
    def page_count(self) -> int:
        return len(self._open())

    def get_page(self, page_num: int) -> fitz.Page:
        """Get a page (0-indexed internally, 1-indexed in API)."""
        return self._open()[page_num - 1]

    def get_text_blocks(self, page_num: int) -> list[TextBlock]:
        """
        Get all text blocks from a page with position info.
        Returns list of TextBlock with spans.
        """
        page = self.get_page(page_num)
        blocks = []
        page_dict = page.get_text("dict")

        for block in page_dict.get("blocks", []):
            if block["type"] != 0:
                continue

            block_text_parts = []
            spans = []

            for line in block.get("lines", []):
                line_text = ""
                for span in line.get("spans", []):
                    t = span["text"]
                    line_text += t
                    spans.append(TextSpan(
                        text=t,
                        bbox=tuple(span["bbox"]),
                        font_name=span.get("font", ""),
                        font_size=span.get("size", 0.0),
                    ))
                block_text_parts.append(line_text)

            block_text = "\n".join(block_text_parts)
            blocks.append(TextBlock(
                bbox=tuple(block["bbox"]),
                text=block_text,
                spans=spans,
                page_num=page_num,
                block_type="text",
            ))

        return blocks

    def get_all_text_blocks(self) -> list[TextBlock]:
        """Get text blocks from all pages."""
        all_blocks = []
        for page_num in range(1, self.page_count + 1):
            all_blocks.extend(self.get_text_blocks(page_num))
        return all_blocks

    def get_images(self, page_num: int) -> list[ImageBlock]:
        """Get all image blocks from a page."""
        page = self.get_page(page_num)
        images = []

        page_dict = page.get_text("dict")
        for block in page_dict.get("blocks", []):
            if block["type"] != 1:
                continue

            images.append(ImageBlock(
                bbox=tuple(block["bbox"]),
                page_num=page_num,
                width=int(block.get("width", 0)),
                height=int(block.get("height", 0)),
            ))

        return images

    def get_all_images(self) -> list[ImageBlock]:
        """Get all images from all pages."""
        all_images = []
        for page_num in range(1, self.page_count + 1):
            all_images.extend(self.get_images(page_num))
        return all_images

    def get_page_text(self, page_num: int) -> str:
        """Get raw text from a page (simple mode)."""
        return self.get_page(page_num).get_text()

    def get_all_text(self) -> str:
        """Get all text from all pages."""
        parts = []
        for page_num in range(1, self.page_count + 1):
            text = self.get_page_text(page_num)
            parts.append(text)
            parts.append(f"\n[--- Trang {page_num} ---]\n")
        return "\n".join(parts)

    def get_all_text_layout_aware(self) -> str:
        """
        Extract text with proper line grouping for 2-column layouts.
        Groups spans by Y coordinate (within 5pt threshold), sorts by X, joins with space.
        This fixes issues where options from different columns get merged.
        """
        Y_THRESHOLD = 5  # points - spans within this Y range are on same line

        all_page_texts = []

        for page_num in range(1, self.page_count + 1):
            page = self._open()[page_num - 1]
            blocks = page.get_text("dict")["blocks"]

            # Collect all spans with coordinates
            spans = []
            for block in blocks:
                if block["type"] == 0:
                    for line in block.get("lines", []):
                        for span in line.get("spans", []):
                            x0, y0 = span["origin"]
                            text = span["text"]
                            if text.strip():
                                spans.append((y0, x0, text))

            # Sort by Y then X
            spans.sort(key=lambda s: (round(s[0] / Y_THRESHOLD) * Y_THRESHOLD, s[1]))

            # Group spans by Y (same line)
            lines: list[list[tuple[float, str]]] = []
            current_line: list[tuple[float, str]] = []
            current_y_group = None

            for y, x, text in spans:
                y_group = round(y / Y_THRESHOLD) * Y_THRESHOLD
                if current_y_group is None or abs(y_group - current_y_group) < Y_THRESHOLD:
                    current_line.append((x, text))
                    current_y_group = y_group
                else:
                    if current_line:
                        lines.append(current_line)
                    current_line = [(x, text)]
                    current_y_group = y_group

            if current_line:
                lines.append(current_line)

            # Format each line: sort by X, join with space
            formatted_lines = []
            for line in lines:
                line.sort(key=lambda l: l[0])
                line_text = " ".join(t for _, t in line)
                formatted_lines.append(line_text)

            all_page_texts.append("\n".join(formatted_lines))
            all_page_texts.append(f"\n[--- Trang {page_num} ---]\n")

        return "".join(all_page_texts)

    def render_page_as_image(
        self,
        page_num: int,
        dpi: int = 300,
        clip: Optional[tuple[float, float, float, float]] = None,
    ) -> bytes:
        """
        Render a PDF page (or a clip region) as a PNG image.
        Returns raw PNG bytes.
        """
        page = self.get_page(page_num)
        mat = fitz.Matrix(dpi / 72.0, dpi / 72.0)

        if clip is not None:
            page_rect = fitz.Rect(*clip)
            pix = page.get_pixmap(matrix=mat, clip=page_rect)
        else:
            pix = page.get_pixmap(matrix=mat)

        return pix.tobytes("png")

    def get_page_size(self, page_num: int) -> tuple[float, float]:
        """Get page dimensions (width, height) in points."""
        page = self.get_page(page_num)
        rect = page.rect
        return (rect.width, rect.height)

    def find_blocks_in_region(
        self,
        page_num: int,
        x0: float,
        y0: float,
        x1: float,
        y1: float,
    ) -> list[TextBlock]:
        """Find text blocks within a bounding box region."""
        region = fitz.Rect(x0, y0, x1, y1)
        all_blocks = self.get_text_blocks(page_num)
        return [
            b for b in all_blocks
            if fitz.Rect(*b.bbox).intersects(region)
        ]

    def find_question_block(
        self,
        page_num: int,
        question_num: int,
        patterns: list[str],
    ) -> Optional[TextBlock]:
        """Find the text block containing a question number."""
        blocks = self.get_text_blocks(page_num)
        for block in blocks:
            for pat in patterns:
                import re
                if re.search(pat.format(q=question_num), block.text, re.IGNORECASE):
                    return block
        return None


def get_text_blocks(pdf_path: str, page_num: int) -> list[TextBlock]:
    """Convenience function."""
    with PdfReader(pdf_path) as reader:
        return reader.get_text_blocks(page_num)


def get_all_text(pdf_path: str) -> str:
    """Convenience function."""
    with PdfReader(pdf_path) as reader:
        return reader.get_all_text()


def render_page_as_image(pdf_path: str, page_num: int, dpi: int = 300) -> bytes:
    """Convenience function."""
    with PdfReader(pdf_path) as reader:
        return reader.render_page_as_image(page_num, dpi)
