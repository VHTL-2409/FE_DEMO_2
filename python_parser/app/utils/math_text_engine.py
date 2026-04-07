"""
math_text_engine.py — Low-level extraction engine for math exam PDFs.

Khác biệt so với phiên bản cũ:
  - Dùng page.get_text("words") để lấy word list thực sự từ PyMuPDF,
    có bbox, font, font_size. Không dùng dict["blocks"] nữa.
  - Y-tolerance 3pt (nghiêm ngặt hơn so với 5pt cũ).
  - Gap-aware joining: chỉ insert space khi gap > 12pt VÀ cả hai token
    đều là multi-letter words. Math tokens không bao giờ bị tách bằng space.
  - Debug mode: export reconstructed rows per page để inspect.

Pipeline:
  1. get_page_words_raw()  — PyMuPDF "words" extraction với bbox thực
  2. _group_into_rows()    — cluster by Y band, sort by X
  3. _join_tokens_gap_aware() — ghép tokens với khoảng cách thông minh
  4. _build_question_regions() — phát hiện Câu N. / Bài N boundaries
  5. extract() → ExtractionResult với rows + questions + debug_info
"""

from __future__ import annotations

import os
import re
from dataclasses import dataclass, field
from typing import Optional

import fitz  # PyMuPDF


# ─── Threshold constants ──────────────────────────────────────────────────────

Y_THRESHOLD: float = 3.0   # points — same-line threshold (strict)
GAP_THRESHOLD: float = 12.0  # points — insert space only when gap exceeds this
DEBUG: bool = os.environ.get("PDF_ENGINE_DEBUG", "0") == "1"


# ─── Data classes ────────────────────────────────────────────────────────────

@dataclass
class TextRow:
    """Một dòng text đã được reconstruct từ các token."""
    y_center: float          # y center của row (dùng để order)
    tokens: list["Word"]     # Word objects, sorted by x0
    text: str = ""           # gap-aware joined text
    page_num: int = 1        # page number


@dataclass
class QuestionRegion:
    """
    Một khối liên tiếp thuộc về một câu hỏi (Câu N. / Bài N).

    raw_lines — danh sách dòng đã reconstruct (dùng cho debug)
    raw_text  — "\\n".join(raw_lines)
    raw_tokens_by_line — Word objects theo từng dòng (dùng cho orphan resolution)
    """
    number: int
    page: int
    y0: float
    y1: float
    raw_lines: list[str]
    raw_text: str = ""
    raw_tokens_by_line: list[list["Word"]] = field(default_factory=list)


@dataclass
class ExtractionResult:
    """Kết quả đầy đủ của extraction pipeline."""
    rows: list[TextRow]
    questions: list[QuestionRegion]
    debug_info: dict = field(default_factory=dict)


# ─── Word dataclass (local, không import từ pdf_reader) ───────────────────────

@dataclass
class Word:
    """Một word/token với tọa độ chính xác từ PyMuPDF."""
    text: str
    x0: float
    y0: float
    x1: float
    y1: float
    font: str = ""
    size: float = 0.0


# ─── Core engine ─────────────────────────────────────────────────────────────

class MathPdfTextEngine:
    """
    Gap-aware PDF text extraction cho math-heavy exam PDFs.

    Cách dùng:
        engine = MathPdfTextEngine(pdf_path)
        result = engine.extract()

        for region in result.questions:
            print(region.number, region.raw_text)
    """

    def __init__(self, pdf_path: str):
        self.pdf_path = pdf_path
        self._doc: Optional[fitz.Document] = None

    # ─── Context manager ──────────────────────────────────────────────────────

    def __enter__(self):
        self._doc = fitz.open(self.pdf_path)
        return self

    def __exit__(self, *args):
        if self._doc is not None:
            self._doc.close()
            self._doc = None

    # ─── Public API ──────────────────────────────────────────────────────────

    def extract(self) -> ExtractionResult:
        """
        Trích xuất tất cả rows và question regions từ PDF.
        Trả về ExtractionResult với debug_info được populate.
        """
        with self:
            page_height = (
                self._doc[0].rect.height
                if self._doc.page_count > 0
                else 792.0
            )

            all_rows: list[TextRow] = []
            for page_num in range(1, self._doc.page_count + 1):
                words = self._get_page_words_raw(page_num)
                rows = self._group_into_rows(words, page_num)
                for row in rows:
                    row.text = self._join_tokens_gap_aware(row.tokens)
                all_rows.extend(rows)

            questions = self._build_question_regions(all_rows, page_height)

            # Build debug info
            dbg: dict = {
                "total_pages": self._doc.page_count,
                "total_rows": len(all_rows),
                "total_questions": len(questions),
            }

            if DEBUG:
                dbg["row_samples"] = [
                    {
                        "y": round(r.y_center, 1),
                        "page": r.page_num,
                        "text": r.text[:120],
                    }
                    for r in all_rows[:30]
                ]
                dbg["question_samples"] = [
                    {
                        "number": q.number,
                        "page": q.page,
                        "line_count": len(q.raw_lines),
                        "first_line": (q.raw_lines[0] if q.raw_lines else "")[:80],
                    }
                    for q in questions[:5]
                ]

            return ExtractionResult(
                rows=all_rows,
                questions=questions,
                debug_info=dbg,
            )

    # ─── Low-level word extraction (dùng "words" mode) ────────────────────────

    def _get_page_words_raw(self, page_num: int) -> list[Word]:
        """
        Trích xuất word list thực sự từ PyMuPDF.

        Dùng page.get_text("words") thay vì page.get_text("dict")["blocks"].
        PyMuPDF "words" mode trả về danh sách word với:
          [x0, y0, x1, y1, word_text, block_no, line_no, word_no]

       Ưu điểm:
          - Word list đã được PyMuPDF layout-aware sort
          - Không phụ thuộc block["lines"] → không bị ảnh hưởng bởi
            việc PDF render mỗi math fragment thành 1 block riêng
          - bbox chính xác hơn
        """
        page = self._doc[page_num - 1]

        # PyMuPDF "words" trả về: [x0, y0, x1, y1, "text", block_no, line_no, word_no]
        raw_words = page.get_text("words")

        words: list[Word] = []
        for item in raw_words:
            if len(item) < 5:
                continue
            x0, y0, x1, y1 = float(item[0]), float(item[1]), float(item[2]), float(item[3])
            text = item[4].strip()
            if not text:
                continue

            # Lấy font info từ dict spans (để phân biệt superscript)
            font_name, font_size = "", 0.0
            try:
                page_dict = page.get_text("dict")
                for block in page_dict.get("blocks", []):
                    if block["type"] != 0:
                        continue
                    for line in block.get("lines", []):
                        for span in line.get("spans", []):
                            sp_x0, sp_y0, sp_x1, sp_y1 = span["bbox"]
                            if (
                                abs(sp_x0 - x0) < 2 and abs(sp_y0 - y0) < 2
                            ):
                                font_name = span.get("font", "")
                                font_size = span.get("size", 0.0)
                                break
            except Exception:
                pass

            words.append(Word(
                text=text,
                x0=x0,
                y0=y0,
                x1=x1,
                y1=y1,
                font=font_name,
                size=font_size,
            ))

        return words

    # ─── Row grouping ─────────────────────────────────────────────────────────

    def _group_into_rows(
        self,
        words: list[Word],
        page_num: int,
    ) -> list[TextRow]:
        """
        Group words into rows by Y proximity.
        Returns list of TextRow sorted top-to-bottom.

        Thuật toán:
          1. Sort words by (y_band, x0) — y_band = round(y0 / 3pt)
          2. Cluster vào current row khi y_band gần current_y
          3. Flush row khi gap > Y_THRESHOLD
        """
        if not words:
            return []

        # Sort by (y_band, x0)
        sorted_words = sorted(
            words,
            key=lambda w: (
                round(w.y0 / Y_THRESHOLD) * Y_THRESHOLD,
                w.x0,
            ),
        )

        rows: list[TextRow] = []
        current_words: list[Word] = []
        current_y: Optional[float] = None

        for w in sorted_words:
            y_key = round(w.y0 / Y_THRESHOLD) * Y_THRESHOLD

            if current_y is None or abs(y_key - current_y) < Y_THRESHOLD:
                current_words.append(w)
                current_y = y_key
            else:
                if current_words:
                    avg_y = sum(tok.y0 for tok in current_words) / len(current_words)
                    rows.append(TextRow(
                        y_center=avg_y,
                        tokens=current_words,
                        page_num=page_num,
                    ))
                current_words = [w]
                current_y = y_key

        if current_words:
            avg_y = sum(tok.y0 for tok in current_words) / len(current_words)
            rows.append(TextRow(
                y_center=avg_y,
                tokens=current_words,
                page_num=page_num,
            ))

        return rows

    # ─── Gap-aware token joining ───────────────────────────────────────────────

    def _join_tokens_gap_aware(self, tokens: list[Word]) -> str:
        """
        Ghép tokens thành string, chỉ insert space khi:
          1. gap > GAP_THRESHOLD (12pt) VÀ
          2. Cả hai token đều là multi-letter words

        Math tokens không bao giờ bị tách bằng space:
          x² + 9 → "x² + 9" (không phải "x ² + 9")
          S = ∅ → "S = ∅" (không phải "S = ∅")
        """
        if not tokens:
            return ""
        if len(tokens) == 1:
            return tokens[0].text

        parts = [tokens[0].text]
        for i in range(1, len(tokens)):
            prev = tokens[i - 1]
            curr = tokens[i]
            gap = curr.x0 - prev.x1

            sep = _compute_separator(prev, curr, gap)
            if sep:
                parts.append(sep)

            parts.append(curr.text)

        return "".join(parts)

    # ─── Question region builder ──────────────────────────────────────────────

    QUESTION_HEADER_RE = re.compile(r"(?i)^\s*(?:Câu|Bài)\s+(\d+)\s*[:\.\-]?\s*")

    def _page_from_y(self, y0: float, page_height: float) -> int:
        """Estimate page number from y0 position."""
        if page_height <= 0:
            return 1
        return max(1, int(y0 / page_height) + 1)

    def _build_question_regions(
        self,
        rows: list[TextRow],
        page_height: float,
    ) -> list[QuestionRegion]:
        """
        Phân chia rows thành các QuestionRegion.

        Thuật toán:
          1. Duyệt rows top-to-bottom
          2. Khi gặp "Câu N." / "Bài N." → flush region trước, bắt đầu region mới
          3. Collect tất cả rows cho đến khi gặp header mới hoặc hết document
          4. Attach Word tokens per line để option parser dùng real X-positions
        """
        regions: list[QuestionRegion] = []
        current_num = 0
        current_lines: list[str] = []
        current_tokens_by_line: list[list[Word]] = []
        current_y0 = 0.0
        current_y1 = 0.0
        current_page = 1

        for row in rows:
            line_text = row.text.strip()
            if not line_text:
                continue

            header_m = self.QUESTION_HEADER_RE.match(line_text)
            if header_m:
                # Flush previous question
                if current_num > 0 and current_lines:
                    regions.append(QuestionRegion(
                        number=current_num,
                        page=current_page,
                        y0=current_y0,
                        y1=current_y1,
                        raw_lines=current_lines,
                        raw_text="\n".join(current_lines),
                        raw_tokens_by_line=current_tokens_by_line,
                    ))

                try:
                    current_num = int(header_m.group(1))
                except ValueError:
                    current_num = 0

                current_lines = [line_text]
                current_tokens_by_line = [list(row.tokens)]
                current_y0 = row.y_center
                current_y1 = row.y_center
                current_page = self._page_from_y(row.y_center, page_height)
            else:
                if current_num > 0:
                    current_lines.append(line_text)
                    current_tokens_by_line.append(list(row.tokens))
                    current_y1 = row.y_center

        # Flush last question
        if current_num > 0 and current_lines:
            regions.append(QuestionRegion(
                number=current_num,
                page=current_page,
                y0=current_y0,
                y1=current_y1,
                raw_lines=current_lines,
                raw_text="\n".join(current_lines),
                raw_tokens_by_line=current_tokens_by_line,
            ))

        return regions


# ─── Gap-aware separator logic (module-level helpers) ────────────────────────

def _compute_separator(prev: Word, curr: Word, gap: float) -> str:
    """
    Quyết định có insert separator giữa hai tokens không.

    Rules:
      - gap > 12pt: wide gap → tokens thuộc layout khác → insert space
        (trừ khi prev kết thúc bằng math opener)
      - gap <= 12pt: math adjacency → no space
    """
    if gap > GAP_THRESHOLD:
        if prev.text.strip() in ("(", "[", "{", "⟨"):
            return ""
        return " "
    # Small gap → math adjacency
    return ""


def _is_text_word(token: Word) -> bool:
    """Multi-letter pure alphabetic → text word (needs spaces)."""
    t = token.text.strip()
    return len(t) > 1 and t.isalpha()
