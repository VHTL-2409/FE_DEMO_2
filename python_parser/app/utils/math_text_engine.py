"""
math_text_engine.py — Gap-aware PDF text extraction engine for math exams.

Replaces get_all_text_layout_aware() + _group_words_into_rows() with a
robust, debuggable pipeline:

  1. get_page_words() → Word objects with precise bbox
  2. Y-tolerance row grouping  (strict: 3pt, not 5pt)
  3. Gap-aware token joining   (measure x-spacing; only insert space when gap > threshold)
  4. Question-region builder   (detect Câu N. headers)
  5. Per-region parsing        (marker-based options + stem extraction)

Preserves math tokens { } ( ) ; = + - ∅ ^ ² ³ ¹ without blindly inserting spaces.
"""

from __future__ import annotations

import re
from dataclasses import dataclass, field
from typing import Optional

from .pdf_reader import PdfReader, Word


# ─── Threshold constants ────────────────────────────────────────────────────

Y_THRESHOLD: float = 3.0   # points — same-line threshold (was 5pt, too loose)
GAP_THRESHOLD: float = 12.0  # points — insert space only when gap exceeds this
# Tokens whose glyphs are close enough horizontally to be the same formula fragment
# are joined with NO space.  Wide gaps (headers, column breaks) get a space.


# ─── Data classes ────────────────────────────────────────────────────────────

@dataclass
class TextRow:
    """One reconstructed text row."""
    y_center: float          # y center of the row (for ordering)
    tokens: list[Word]       # original Word objects (sorted by x0)
    text: str = ""           # gap-aware joined text
    page_num: int = 1        # page number this row belongs to


@dataclass
class QuestionRegion:
    """A contiguous block belonging to one question (Câu N.)."""
    number: int
    page: int
    y0: float
    y1: float
    raw_lines: list[str]     # reconstructed line strings (NOT joined into one blob)
    raw_text: str = ""      # "\n".join(raw_lines)
    # Raw Word tokens per line — used by the option parser to determine
    # exact x-positions of option markers and orphan fragments
    raw_tokens_by_line: list[list[Word]] = field(default_factory=list)


@dataclass
class ExtractionResult:
    """Full result of text extraction."""
    rows: list[TextRow]                    # all reconstructed rows
    questions: list[QuestionRegion]         # question regions
    debug_info: dict = field(default_factory=dict)  # for debugging


# ─── Core engine ─────────────────────────────────────────────────────────────

class MathPdfTextEngine:
    """
    Gap-aware PDF text extraction for math-heavy exam PDFs.

    Usage:
        engine = MathPdfTextEngine(pdf_path)
        result = engine.extract()

        for region in result.questions:
            print(region.number, region.raw_text)
    """

    def __init__(self, pdf_path: str):
        self.pdf_path = pdf_path
        self._reader: Optional[PdfReader] = None

    # ─── Public API ───────────────────────────────────────────────────────────

    def extract(self, page_height: Optional[float] = None) -> ExtractionResult:
        """
        Extract all rows and question regions from the PDF.
        Returns ExtractionResult with debug_info populated.

        Args:
            page_height: height of each page in points (default: detect from PDF).
                        When provided, used to estimate page numbers from y positions.
        """
        with PdfReader(self.pdf_path) as reader:
            self._reader = reader

            if page_height is None:
                page_height = reader.get_page_size(1)[1] if reader.page_count > 0 else 792.0

            all_rows: list[TextRow] = []
            for page_num in range(1, reader.page_count + 1):
                words = reader.get_page_words(page_num)
                rows = self._group_into_rows(words, page_num)
                for row in rows:
                    row.text = self._join_tokens_gap_aware(row.tokens)
                    # Tag each row with its page number for page-aware region building
                    row.page_num = page_num
                all_rows.extend(rows)

            # Build question regions from rows (using page-aware tracking)
            questions = self._build_question_regions(all_rows, page_height)

            debug_info = {
                "total_pages": reader.page_count,
                "total_rows": len(all_rows),
                "total_questions": len(questions),
                "row_samples": [
                    {"y": r.y_center, "text": r.text[:80]}
                    for r in all_rows[:20]
                ],
            }

            return ExtractionResult(
                rows=all_rows,
                questions=questions,
                debug_info=debug_info,
            )

    def debug_extract(self) -> dict:
        """Return debug dict only (no parsing)."""
        return self.extract().debug_info

    # ─── Row grouping ─────────────────────────────────────────────────────────

    def _group_into_rows(
        self,
        words: list[Word],
        page_num: int,
    ) -> list[TextRow]:
        """
        Group words into rows by Y proximity.
        Returns list of TextRow sorted top-to-bottom.
        """
        if not words:
            return []

        # Sort by (y_band, x0) — stable sort preserves input order for tie-breaking
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
                    rows.append(TextRow(
                        y_center=current_y,
                        tokens=current_words,
                    ))
                current_words = [w]
                current_y = y_key

        if current_words:
            rows.append(TextRow(
                y_center=current_y,
                tokens=current_words,
            ))

        return rows

    # ─── Gap-aware joining ────────────────────────────────────────────────────

    def _join_tokens_gap_aware(self, tokens: list[Word]) -> str:
        """
        Join tokens into a string, inserting space ONLY when the horizontal
        gap between consecutive tokens justifies it.

        Rules:
          - gap > GAP_THRESHOLD (12pt): insert space (separate words / columns)
          - gap ≤ GAP_THRESHOLD: no space (math adjacency)
          - both tokens are multi-letter text words: always space
          - both tokens are math fragments: never space
          - mixed: never space (e.g. "equation" + "x²" → "equation x²")
        """
        if not tokens:
            return ""
        if len(tokens) == 1:
            return tokens[0].text

        parts = [tokens[0].text]
        for i in range(1, len(tokens)):
            prev = tokens[i - 1]
            curr = tokens[i]
            gap = curr.x0 - prev.x1  # horizontal gap between tokens

            sep = _compute_separator(prev, curr, gap)
            if sep:
                parts.append(sep)

            parts.append(curr.text)

        return "".join(parts)

    # ─── Question region builder ──────────────────────────────────────────────

    QUESTION_HEADER_RE = re.compile(r"(?i)^\s*(?:Câu|Bài)\s+(\d+)\s*[:\.\-]?\s*")

    def _page_from_tokens(self, tokens: list[Word], page_height: float) -> int:
        """
        Estimate page number from y0 position of the first token.
        PDF pages start at y=0 at the top, so y_center / page_height gives
        an estimate of which page the token is on.
        """
        if not tokens or page_height <= 0:
            return 1
        y0 = tokens[0].y0
        # y0 increases as you go down the page. Round to nearest page.
        page_est = int(y0 / page_height) + 1
        return max(1, page_est)

    def _build_question_regions(
        self,
        rows: list[TextRow],
        page_height: float,
    ) -> list[QuestionRegion]:
        """
        Partition rows into QuestionRegion blocks.
        Detects question headers via Câu N. / Bài N. patterns.
        Also attaches raw token data (x positions) to each row for use by
        the option parser (so it can match orphan fragments to correct columns).
        """
        regions: list[QuestionRegion] = []
        current_num = 0
        current_lines: list[str] = []
        current_tokens_by_line: list[list[Word]] = []  # x positions for each line
        current_y0 = 0.0
        current_page = 1
        current_row_y1 = 0.0

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
                        y1=current_row_y1,
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
                current_page = self._page_from_tokens(row.tokens, page_height)
            else:
                if current_num > 0:
                    current_lines.append(line_text)
                    current_tokens_by_line.append(list(row.tokens))
                    current_row_y1 = row.y_center

        # Flush last question
        if current_num > 0 and current_lines:
            regions.append(QuestionRegion(
                number=current_num,
                page=current_page,
                y0=current_y0,
                y1=current_row_y1,
                raw_lines=current_lines,
                raw_text="\n".join(current_lines),
                raw_tokens_by_line=current_tokens_by_line,
            ))

        return regions


# ─── Gap-aware separator logic (module-level helpers) ────────────────────────

def _compute_separator(prev: Word, curr: Word, gap: float) -> str:
    """
    Decide whether to insert a separator between two adjacent tokens,
    and what separator (space or empty).

    Principles:
      - Large gap (>12pt): tokens are on different "columns" → insert space
      - Small gap (≤12pt): same formula / expression → no space
      - Multi-letter text word before anything: space (word boundary)
      - Unicode minus (−) before number: no space (e.g. "-3")
      - Opening bracket after text: space (e.g. "n (" → "n (")
    """
    if gap > GAP_THRESHOLD:
        # Wide gap — tokens are separated by layout; use space
        # Exception: if prev ends with math opener, no space
        if prev.text.strip() in ("(", "[", "{", "⟨"):
            return ""
        return " "

    # Small gap — math adjacency, no space
    return ""


def _is_text_word(word: Word) -> bool:
    """Multi-letter pure alphabetic → text word (needs spaces)."""
    t = word.text.strip()
    return len(t) > 1 and t.isalpha()


def _is_math_token(word: Word) -> bool:
    """
    Heuristic: should this PDF span be treated as a math fragment
    (joined without spaces to adjacent math tokens)?
    """
    t = word.text.strip()
    if not t:
        return True
    if len(t) > 12:
        return False

    MATH_SYMBOLS = set(
        "²³¹⁰⁴⁵⁶⁷⁸⁹⁺⁻⁼×÷·±∓√∫∑∏∂∆∇∈∉⊂⊃∪∩∅∞→←↔⇒⇐⇔≥≤≠≈≡∝⊕⊗⊙⊖°′″"
        "=+−±∓×÷·<>≤≥≈≡≠"
    )
    if any(ch in MATH_SYMBOLS for ch in t):
        return True

    has_digits = any(c.isdigit() for c in t)
    has_alpha = any(c.isalpha() for c in t)
    if has_digits and has_alpha:
        return True

    if len(t) == 1 and t.isalpha():
        return True

    if any(ch in "²³¹⁰⁴⁵⁶⁷⁸⁹" for ch in t):
        return True

    GREEK = (
        "αβγδεζηθικλμνξοπρστυφχψωΑΒΓΔΕΖΗΘΙΚΛΜΝΞΟΠΡΣΤΥΦΧΨΩ"
    )
    if any(ch in GREEK for ch in t):
        return True

    return False
