"""
template_01_math_rebuilt.py — Production-ready parser for math PDF exams.

Replaces template_01_math_broken.py with a correct implementation:

  - MathPdfTextEngine: gap-aware row reconstruction from word-level PDF data
  - Orphan-aware option parsing: correctly reassembles multi-row options using real X-positions
  - Unicode superscript preservation: ²³ preserved in options, not destroyed
  - Stem/option separation: no more A./B./C./D. leaking into question text
  - Compatible output: same ParsedQuestion schema as the old parser

PDF characteristics:
  - Vietnamese math exam (Đại số, Hình học)
  - Formulas broken across multiple spans (superscripts, symbols as separate spans)
  - Short exam: 1-5 pages, 3-10 MCQ + optional essay
  - Answer key may be at end: "1.C 2.B 3.D"

Examples:
  - pdf_mau_1.pdf (Đề Toán lớp 8)
"""

from __future__ import annotations

import re
import time
from dataclasses import dataclass, field
from typing import Optional

from .base import BaseParser, ParsedBlock
from ..schemas import (
    ExamMeta,
    ParsedQuestion,
    QuestionType,
    RenderMode,
    TemplateType,
)
from ..profiler import PdfProfile
from ..utils.math_text_engine import MathPdfTextEngine, QuestionRegion
from ..utils.pdf_reader import PdfReader, Word
from ..utils.section_detector import detect_sections, SectionKind
from ..utils.answer_extractor import AnswerExtractor, extract_inline_answer
from ..utils.latex_converter import convert_to_latex
from ..utils.text_normalizer import (
    normalize_math_text,
    normalize_mcq_stem_display,
    classify_content_type,
    formula_analysis,
    map_pdf_private_use_math_glyphs,
    has_unmapped_pdf_private_use_math_glyphs,
    sanitize_pdf_private_use_math_glyphs,
)


# ─── Dataclasses ─────────────────────────────────────────────────────────────

@dataclass
class MathRebuiltBlock:
    """A question block rebuilt from MathPdfTextEngine."""
    question_num: int
    raw_text: str
    raw_lines: list[str]
    page: int
    y0: float
    y1: float
    x0: float = 0.0
    x1: float = 0.0
    is_essay: bool = False
    raw_tokens_by_line: list = field(default_factory=list)


# ─── Parser ───────────────────────────────────────────────────────────────────

class Template01MathRebuiltParser(BaseParser):
    """
    Production-ready parser for math PDFs with broken formula text.

    Strategy:
      1. MathPdfTextEngine: word-level extraction + gap-aware row reconstruction
      2. Orphan-aware option parsing: detects multi-row option fragments using real X-positions
      3. Unicode superscript preservation: options parsed from RAW text
      4. LaTeX conversion: convert_to_latex() for frontend rendering
      5. Section awareness: MCQ vs essay split
    """

    template_type = TemplateType.TEMPLATE_01_MATH_BROKEN
    parser_name = "template_01_math_rebuilt"

    def __init__(self, pdf_path: str, session_id: str | None = None):
        super().__init__(pdf_path, session_id)
        self._answer_key: dict[int, str] = {}
        self._essay_section_open: bool = False
        self._question_bounds_by_number: dict[int, tuple[int, float, float]] = {}

    def can_handle(self, profile: PdfProfile, file_path: str = "") -> float:
        """Score: high for Vietnamese math with formula noise and answer key."""
        return profile.score_template_01

    def _parse_impl(self) -> tuple[list[ParsedQuestion], ExamMeta]:
        """Main parsing logic."""
        start = time.time()

        # Step 1: Word-level text extraction + row reconstruction
        engine = MathPdfTextEngine(self.pdf_path)
        extraction = engine.extract()
        raw_rows = extraction.rows
        regions = extraction.questions
        answer_text = extraction.answer_text

        # Step 2: Build blocks from question regions
        math_blocks = self._regions_to_blocks(regions, raw_rows)
        self._question_bounds_by_number = {
            block.question_num: (block.page, block.y0, block.y1)
            for block in math_blocks
            if block.question_num > 0
        }

        # Step 3: Split into MCQ and essay sections
        full_text = "\n".join(
            r.text for r in raw_rows if r.text.strip()
        )
        sections = detect_sections(full_text)
        self._answer_key = self._extract_answer_key(
            answer_text if answer_text.strip() else full_text,
            sections,
        )

        mcq_blocks, essay_blocks = self._split_mcq_essay(math_blocks, full_text)

        # Step 4: Parse MCQ blocks
        questions: list[ParsedQuestion] = []
        for block in mcq_blocks:
            ctx = self._section_context_for_block(is_essay=False, sections=sections)
            q = self._parse_mcq_block(block, ctx)
            if q:
                questions.append(q)

        # Step 5: Parse essay blocks
        for block in essay_blocks:
            ctx = self._section_context_for_block(is_essay=True, sections=sections)
            q = self._parse_essay_block(block, ctx)
            if q:
                questions.append(q)

        questions.sort(key=lambda x: x.number)

        meta = self.extract_meta(full_text)
        meta.totalQuestions = len(questions)
        meta.template = TemplateType.TEMPLATE_01_MATH_BROKEN

        parse_time = int((time.time() - start) * 1000)
        print(f"[template_01_rebuilt] Parsed {len(questions)} questions in {parse_time}ms",
              flush=True)

        return questions, meta

    # ─── Block building ───────────────────────────────────────────────────────

    def _regions_to_blocks(
        self,
        regions: list[QuestionRegion],
        raw_rows: list,
    ) -> list[MathRebuiltBlock]:
        """Convert QuestionRegion objects to MathRebuiltBlock list."""
        blocks: list[MathRebuiltBlock] = []

        for region in regions:
            if region.number <= 0:
                continue

            current_is_essay = self._essay_section_open
            raw_lines = list(region.raw_lines)
            raw_tokens_by_line = list(region.raw_tokens_by_line)
            trimmed_lines: list[str] = []
            trimmed_tokens: list = []
            has_essay_marker = False
            for line_idx, line in enumerate(raw_lines):
                essay_marker = re.match(
                    r"(?i)^\s*Phần\s*(?:II|2|hai)\s*[:\.\-]?\s*(?:Tự\s*luận)?\s*",
                    line,
                )
                if essay_marker:
                    has_essay_marker = True
                    if trimmed_lines:
                        break
                    tail = line[essay_marker.end():].strip()
                    current_is_essay = True
                    if tail:
                        trimmed_lines.append(tail)
                        if line_idx < len(raw_tokens_by_line):
                            trimmed_tokens.append(raw_tokens_by_line[line_idx])
                    continue
                trimmed_lines.append(line)
                if line_idx < len(raw_tokens_by_line):
                    trimmed_tokens.append(raw_tokens_by_line[line_idx])

            x0, y0, x1, y1 = self._token_bounds(trimmed_tokens)
            if x0 is None or y0 is None or x1 is None or y1 is None:
                x0 = 0.0
                x1 = 0.0
                y0 = region.y0
                y1 = region.y1

            block = MathRebuiltBlock(
                question_num=region.number,
                raw_text="\n".join(trimmed_lines).strip(),
                raw_lines=trimmed_lines,
                page=region.page,
                y0=y0,
                y1=y1,
                x0=x0,
                x1=x1,
                is_essay=current_is_essay,
                raw_tokens_by_line=trimmed_tokens,
            )

            if has_essay_marker or re.search(
                r"(?i)(?:^|\n)\s*Phần\s*(?:II|2|hai)\s*[:\.\-]?\s*(?:Tự\s*luận)?",
                region.raw_text,
            ):
                self._essay_section_open = True

            blocks.append(block)

        return blocks

    def _token_bounds(
        self,
        token_lines: list,
    ) -> tuple[Optional[float], Optional[float], Optional[float], Optional[float]]:
        xs0: list[float] = []
        xs1: list[float] = []
        ys0: list[float] = []
        ys1: list[float] = []
        for line in token_lines:
            if not line:
                continue
            xs0.extend(getattr(tok, "x0", None) for tok in line if getattr(tok, "x0", None) is not None)
            xs1.extend(getattr(tok, "x1", None) for tok in line if getattr(tok, "x1", None) is not None)
            ys0.extend(getattr(tok, "y0", None) for tok in line if getattr(tok, "y0", None) is not None)
            ys1.extend(getattr(tok, "y1", None) for tok in line if getattr(tok, "y1", None) is not None)
        if not xs0 or not xs1 or not ys0 or not ys1:
            return None, None, None, None
        return min(xs0), min(ys0), max(xs1), max(ys1)

    # ─── Section detection ────────────────────────────────────────────────────

    def _extract_answer_key(
        self,
        full_text: str,
        sections: list,
    ) -> dict[int, str]:
        """Extract answer key from dedicated section or tail."""
        for section in sections:
            if section.kind == SectionKind.ANSWER_KEY:
                lines = full_text.split("\n")
                section_text = "\n".join(lines[section.start_line:section.end_line])
                extractor = AnswerExtractor(section_text)
                answers = extractor.get_as_dict()
                if answers:
                    return answers
        extractor = AnswerExtractor(full_text)
        return extractor.get_as_dict()

    def _section_context_for_block(
        self,
        is_essay: bool,
        sections: list,
    ) -> dict:
        """Get section title and kind for a block."""
        want = SectionKind.ESSAY if is_essay else SectionKind.MCQ
        for section in sections:
            if section.kind == want:
                return {
                    "section": section.title,
                    "sectionKind": section.kind.value,
                    "answerLocation": "none" if is_essay else "inline",
                }
        unknown_section = next(
            (section for section in sections if section.kind == SectionKind.UNKNOWN),
            None,
        )
        return {
            "section": unknown_section.title if unknown_section else None,
            "sectionKind": SectionKind.UNKNOWN.value,
            "answerLocation": "none" if is_essay else "inline",
        }

    # ─── MCQ / Essay split ─────────────────────────────────────────────────

    ESSAY_PATTERNS = [
        r"(?i)Phần\s*(?:II|IV|VI|VIII|X)\s*[:\.\-]",
        r"(?i)Phần\s*(?:2|4|6|8|10)\s*[:\.\-]",
        r"(?i)Phần\s*Tự\s*Luận",
        r"(?i)(?:^|\n)\s*Tự\s*luận\b\s*[:\.\-]?",
        r"(?i)(?:^|\n)\s*Essay\b\s*[:\.\-]?",
        r"(?i)Phần\s*.*\bTự\s*luận\b",
    ]

    def _split_mcq_essay(
        self,
        blocks: list[MathRebuiltBlock],
        full_text: str,
    ) -> tuple[list[MathRebuiltBlock], list[MathRebuiltBlock]]:
        """Split blocks into MCQ (Phần I) and Essay (Phần II) sections."""
        mcq: list[MathRebuiltBlock] = []
        essay: list[MathRebuiltBlock] = []
        in_essay = False

        for block in blocks:
            if block.is_essay:
                in_essay = True
            elif not in_essay:
                for pat in self.ESSAY_PATTERNS:
                    if re.search(pat, block.raw_text, re.IGNORECASE):
                        in_essay = True
                        break

            block.is_essay = in_essay
            if in_essay:
                essay.append(block)
            else:
                mcq.append(block)

        return mcq, essay

    # ─── MCQ parsing ─────────────────────────────────────────────────────────

    def _parse_mcq_block(
        self,
        block: MathRebuiltBlock,
        section_info: dict,
    ) -> Optional[ParsedQuestion]:
        """Parse a single MCQ block."""
        if block.question_num <= 0:
            return None

        raw_text = block.raw_text

        # Parse options using raw tokens (X-positions) for orphan attachment
        options = self._parse_options(raw_text, block.raw_tokens_by_line)

        # Extract stem (everything before first option marker)
        stem_raw = self._extract_stem(raw_text)
        stem_clean = self._trim_preamble_before_question(stem_raw, block.question_num)
        stem_clean = normalize_mcq_stem_display(stem_clean)

        display_options = {
            k: normalize_mcq_stem_display(sanitize_pdf_private_use_math_glyphs(v))
            for k, v in options.items()
        }
        valid_options = {k: v for k, v in display_options.items() if v.strip()}
        # Check formula noise to decide render mode
        formula_noise = self._calculate_formula_noise(stem_clean)
        marker_text = self._split_inline_merged_options(raw_text)
        marker_letters_found = {
            letter
            for _, letter, _, _ in self._collect_line_option_marker_entries(marker_text)
        }
        has_option_markers = bool(marker_letters_found)
        has_private_use = has_unmapped_pdf_private_use_math_glyphs(raw_text)
        has_orphan_formula = any(
            self._is_orphan_math_fragment(line.strip())
            for line in block.raw_lines
            if line.strip()
        )
        missing_marker_letters = marker_letters_found.difference(valid_options.keys())
        malformed_mcq = len(valid_options) < 2 and has_option_markers
        high_noise = (
            formula_noise > 0.2
            or has_private_use
            or has_orphan_formula
            or malformed_mcq
        )
        stem_latex = convert_to_latex(stem_clean, mode="auto")
        option_text_for_type = "\n".join(valid_options.values())
        content_type = classify_content_type(
            "\n".join(part for part in [stem_clean, option_text_for_type] if part.strip()),
            "\n".join(
                part for part in [
                    stem_latex,
                    "\n".join(convert_to_latex(v, mode="inline") for v in valid_options.values()),
                ] if part.strip()
            ),
        )
        mcq_candidate = len(valid_options) >= 2 or has_option_markers
        force_image_render = mcq_candidate
        render_mode = RenderMode.IMAGE if (high_noise or force_image_render) else RenderMode.LATEX

        issues: list[str] = []
        confidence = 0.9 if len(valid_options) >= 2 else 0.5

        if formula_noise > 0.2:
            issues.append(f"High formula noise ({formula_noise:.1%}), rendered as image.")
        elif high_noise:
            issues.append("Rendered as image to preserve formula fidelity.")
        elif force_image_render:
            issues.append("Math question rendered as image to preserve question layout.")
        if has_private_use:
            issues.append("Detected unresolved private-use math glyphs from PDF font mapping.")
        if malformed_mcq or missing_marker_letters:
            issues.append("Option markers detected but some answers could not be reconstructed cleanly.")

        if len(valid_options) < 2:
            issues.append(f"Only {len(valid_options)} options found.")

        # Get answer from answer key
        answer = self._answer_key.get(block.question_num)
        answer_location = "answer_table" if answer else "inline"
        if answer is None:
            answer = extract_inline_answer(stem_clean)
            if answer:
                answer_location = "inline"

        latex_options: Optional[dict[str, str]] = None
        if valid_options:
            latex_options = {
                k: convert_to_latex(v, mode="inline")
                for k, v in valid_options.items()
            }

        parsed_block = ParsedBlock(
            raw_text=stem_clean,
            question_num=block.question_num,
            page=block.page,
        )

        hints = formula_analysis(stem_clean)

        question = self.build_question(
            block=parsed_block,
            options=valid_options,
            q_type=(
                QuestionType.MULTIPLE_CHOICE
                if len(valid_options) >= 2 or has_option_markers
                else QuestionType.ESSAY
            ),
            answer=answer,
            confidence=confidence,
            render_mode=render_mode,
            bbox=self._block_bbox(block) if (high_noise or force_image_render) else None,
            issues=issues,
            display_text=stem_clean,
        )

        question.section = section_info.get("section")
        question.sectionKind = section_info.get("sectionKind") or SectionKind.MCQ.value
        question.answerLocation = answer_location
        question.needsGrading = False
        question.formulaHints = hints
        question.latexContent = stem_latex
        question.latexOptions = latex_options
        question.contentType = content_type

        if answer is not None:
            ans = str(answer).strip()
            if re.match(r"^[A-Da-d]$", ans):
                question.answer = ans.upper()

        return question

    def _block_bbox(
        self,
        block: MathRebuiltBlock,
        *,
        x_margin: float = 12.0,
        y_margin: float = 8.0,
    ) -> Optional[tuple[float, float, float, float]]:
        """Build a crop bbox for image fallback using trimmed token bounds."""
        if block.page <= 0:
            return None
        try:
            page_width, page_height = self.reader.get_page_size(block.page)
        except Exception:
            return None
        y0 = max(0.0, block.y0 - y_margin)
        y1 = min(page_height, self._question_crop_end_y(block, page_height, y_margin))
        if y1 <= y0:
            return None
        if block.x1 > block.x0:
            x0 = max(0.0, block.x0 - x_margin)
            x1 = min(page_width, block.x1 + x_margin)
        else:
            x0 = max(0.0, x_margin)
            x1 = max(x0 + 10.0, page_width - x_margin)
        if x1 <= x0:
            return None
        return (x0, y0, x1, y1)

    def _question_crop_end_y(
        self,
        block: MathRebuiltBlock,
        page_height: float,
        y_margin: float,
    ) -> float:
        default_y1 = min(page_height, block.y1 + y_margin)
        next_same_page_starts = sorted(
            next_y0
            for question_num, (page, next_y0, _next_y1) in (getattr(self, "_question_bounds_by_number", {}) or {}).items()
            if question_num != block.question_num
            and page == block.page
            and next_y0 > block.y0
        )
        if not next_same_page_starts:
            return default_y1
        next_start = next_same_page_starts[0]
        # Some pdf_mau_1 questions contain stray math fragments that make the
        # current block's token bounds spill into the next question header.
        # Clamp the image crop to the next question start instead of trusting
        # the current block's y1 in those overlap cases.
        safe_end = max(block.y0 + 2.0, next_start - 6.0)
        return min(default_y1, safe_end)

    # ─── Essay parsing ────────────────────────────────────────────────────────

    def _parse_essay_block(
        self,
        block: MathRebuiltBlock,
        section_info: dict,
    ) -> Optional[ParsedQuestion]:
        """Parse a single essay block."""
        if block.question_num <= 0:
            return None

        raw_text = block.raw_text

        lines = raw_text.split("\n")
        if lines:
            lines[0] = re.sub(
                r"^\s*(?:Câu|Bài)\s*\d+(?:\s*\([^)]*\))?\s*[\.:]?\s*",
                "",
                lines[0],
                flags=re.IGNORECASE,
            )
        text = "\n".join(lines).strip()
        text = re.sub(
            r"^\s*\(?\s*\d+\s*đ\s*\)?\s*[\.:]?\s*",
            "",
            text,
            flags=re.IGNORECASE,
        ).strip()

        raw_stem = text
        text = normalize_math_text(text)

        parsed_block = ParsedBlock(
            raw_text=text,
            question_num=block.question_num,
            page=block.page,
        )

        hints = formula_analysis(text)

        question = self.build_question(
            block=parsed_block,
            options={},
            q_type=QuestionType.ESSAY,
            confidence=0.7,
            render_mode=RenderMode.LATEX,
            bbox=None,
        )

        question.section = section_info.get("section")
        question.sectionKind = SectionKind.ESSAY.value
        question.answerLocation = "none"
        question.needsGrading = True
        question.formulaHints = hints

        stem_latex = convert_to_latex(raw_stem, mode="block")
        question.latexContent = stem_latex
        question.contentType = classify_content_type(raw_stem, stem_latex)

        return question

    # ─── Option parsing (orphan-aware, using real X-positions) ──────────────

    def _parse_options(
        self,
        text: str,
        raw_tokens_by_line: Optional[list] = None,
    ) -> dict[str, str]:
        """
        Parse MCQ options from raw text.

        Pipeline:
          1. Split merged inline options (A. B. C. on one line)
          2. Find all option markers and compute column X-ranges
          3. Remove orphan math fragments from question stem region (y < first_marker_y)
             and attach them to the correct option column by X-position
          4. Extract content after each marker
        """
        # Step 0: Pre-process merged inline options
        text = self._split_inline_merged_options(text)

        lines = text.split("\n")
        tokens_by_line: list = raw_tokens_by_line or [None] * len(lines)

        # Step 1: find all option markers + their X-positions
        marker_info: dict[int, tuple[str, str, float]] = {}
        # line_idx -> (letter, content_after_marker, x0_of_marker)
        marker_entries = self._collect_line_option_marker_entries(text)

        for i, letter, _, content_after_marker in marker_entries:
            tokens = tokens_by_line[i] if i < len(tokens_by_line) else None
            if letter in {mi[0] for mi in marker_info.values()}:
                continue
            marker_x0 = 20.0
            if tokens:
                marker_x0 = self._find_marker_x0(tokens, letter)
            marker_info[i] = (letter, content_after_marker, marker_x0)

        if not marker_info:
            return {}

        sorted_indices = sorted(marker_info.keys())
        col_ranges: list[tuple[str, float, float]] = []
        for idx_pos, line_idx in enumerate(sorted_indices):
            letter, _, x0 = marker_info[line_idx]
            next_x1 = marker_info[sorted_indices[idx_pos + 1]][2] \
                if idx_pos + 1 < len(sorted_indices) else x0 + 200.0
            col_ranges.append((letter, x0, next_x1))

        first_marker_line = sorted_indices[0]
        first_marker_y = float('inf')
        if first_marker_line < len(tokens_by_line) and tokens_by_line[first_marker_line]:
            first_marker_y = tokens_by_line[first_marker_line][0].y0

        # Step 2: find orphan math fragments ABOVE the options row (y < first_marker_y)
        # and map them to option columns by X-position
        orphan_by_col: dict[str, list[str]] = {ltr: [] for ltr, _, _ in col_ranges}
        marker_line_set = set(marker_info.keys())

        for i, line in enumerate(lines):
            stripped = line.strip()
            if not stripped or i in marker_line_set:
                continue

            # Only consider lines ABOVE the first marker (stem orphan area)
            tokens_i = tokens_by_line[i] if i < len(tokens_by_line) else None
            if tokens_i is not None and first_marker_y != float("inf"):
                orphan_y = tokens_i[0].y0
                # PDF y tăng xuống dưới. Chỉ bỏ qua orphan khi dòng nằm rõ *dưới* hàng A./B./C./D.
                # (>= first_marker_y - 5 đã skip nhầm mảnh "= ∅" / "{}" cùng cụm đáp án).
                if orphan_y > first_marker_y + 8.0:
                    continue
            elif i < sorted_indices[0]:
                # No token data but clearly before markers
                pass
            else:
                continue

            if self._contains_vietnamese(stripped):
                continue
            if not self._is_orphan_math_fragment(stripped):
                continue

            orphan_x0 = float('inf')
            if tokens_i:
                orphan_x0 = tokens_i[0].x0

            target = self._resolve_orphan_to_column(orphan_x0, stripped, col_ranges)
            if target:
                orphan_by_col[target].append(stripped)

        # Step 3: build final options dict
        result: dict[str, str] = {}
        for letter, x0, x1 in col_ranges:
            line_idx = next(lid for lid, (ltr, _, _) in marker_info.items() if ltr == letter)
            _, first_content, _ = marker_info[line_idx]

            # Collect continuation lines after the marker (for multi-row options)
            pos_in_sorted = sorted_indices.index(line_idx)
            next_line = sorted_indices[pos_in_sorted + 1] \
                if pos_in_sorted + 1 < len(sorted_indices) else len(lines)
            content_parts: list[str] = []
            if first_content:
                content_parts.append(first_content)

            for j in range(line_idx + 1, next_line):
                line_j = lines[j].strip()
                if not line_j:
                    continue
                if self.OPT_MARKER_RE.match(line_j):
                    break
                if j in marker_line_set:
                    break
                content_parts.append(line_j)

            # Attach orphans mapped to this column
            orphans = orphan_by_col.get(letter, [])
            cleaned_orphans = []
            for o in orphans:
                for old, new in [
                    ("\uf0c6", "\u2205"),
                    ("\uf0a5", "\u221e"),
                    ("\uf07b", "{"),
                    ("\uf07d", "}"),
                ]:
                    o = o.replace(old, new)
                cleaned_orphans.append(o)

            full_parts = content_parts + cleaned_orphans
            joined = " ".join(p for p in full_parts if p).strip()
            joined = re.sub(r" {2,}", " ", joined).strip()
            if joined:
                result[letter] = joined

        # Fallback: extract directly from split lines for any missing letter
        for letter in [ltr for ltr in "ABCD" if ltr not in result or not result.get(ltr, "").strip()]:
            for i, line in enumerate(lines):
                m = self.OPT_MARKER_RE.match(line.strip())
                if m and m.group(1) == letter:
                    content = line.strip()[m.end():].strip()
                    if content:
                        result[letter] = content
                        break

        return result

    def _find_marker_x0(self, tokens: list, letter: str) -> float:
        """Find x0 of an option marker letter in the token list."""
        for tok in tokens:
            if tok.text.strip().startswith(letter):
                return tok.x0
        return 0.0

    def _resolve_orphan_to_column(
        self,
        orphan_x0: float,
        orphan_text: str,
        col_ranges: list[tuple[str, float, float]],
    ) -> Optional[str]:
        """
        Map an orphan fragment to an option column by X-position intersection.

        Rules:
          1. Real X position → intersect with column ranges, return first match.
             This is the CRITICAL fix: orphan "= ∅" at x≈361 falls inside
             C range (335-464), not D range (464+). Previous code returned
             col_ranges[-1][0] (D) for everything without intersection — wrong.
          2. No X data + short "= X" suffix → find closest column to the RIGHT
             (uses previous marker position heuristic).
          3. No X data + other → None (let orphan be dropped).
        """
        # Step 1: real X position — intersect with column ranges
        if orphan_x0 != float('inf'):
            for letter, x0, x1 in col_ranges:
                if x0 <= orphan_x0 < x1:
                    return letter
            # Falls outside all known ranges.
            # In pdf_mau_1 orphan "=" at x=363 falls ON the boundary of B col (x1=335).
            # x1 > orphan_x0 matches C (x1=464 > 363) ✓
            # Use >= for the rightmost col to ensure orphan always gets a target.
            for letter, x0, x1 in col_ranges:
                if x1 >= orphan_x0:
                    return letter
            return col_ranges[-1][0]

        # Rule 2: no X data + short "= X" suffix fragment
        stripped = orphan_text.strip()
        if stripped.startswith("=") and len(stripped) <= 10:
            # In pdf_mau_1: "= ∅" at y≈240 (row above option row y≈243)
            # Column layout: A≈52-335, B≈335-464, C≈464-593, D≈593+
            # "= ∅" from C column (C.x0≈464, orphan.x0≈361 which is inside B range)
            # But the text "= ∅" is NOT from B — it's from a continuation line.
            # Use "second-to-last column" heuristic for "= X" fragments.
            # CORRECTED: "= X" suffix belongs to the SECOND column, not the last.
            # In 4-column layout: A, B, C, D — "= X" is continuation of C (penultimate).
            if len(col_ranges) >= 2:
                return col_ranges[-2][0]
            return col_ranges[-1][0] if col_ranges else None

        # Rule 3: no X data + other content → None
        return None

    def _is_orphan_math_fragment(self, text: str) -> bool:
        """Is this line an orphan math fragment (no option marker, math content only)?"""
        s = text.strip()
        if not s or len(s) > 120:
            return False
        if self._contains_vietnamese(s):
            return False
        MATH_OPS = set("={}+−×÷·±∓√∅∈∉⊂⊃⊪∩→←↔≥≤≠≈{}(\uff5b\uff5d")
        if not any(ch in MATH_OPS for ch in s):
            return False
        if re.match(r"^\s*[A-D]\s*[\.)）．:]\s*", s):
            return False
        if re.match(r"^[A-Za-z]\s*(=|≤|≥|<|>|≠)\s*[\w({\[]", s):
            return False
        return True

    def _contains_vietnamese(self, text: str) -> bool:
        """Check if text contains Vietnamese diacritical characters."""
        return bool(re.search(
            r"[àáảãạăằẳẵặâầẩẫậèéẻẽẹêềểễệìíỉĩịòóỏõọôồổốộơờởỡợùúủũụưừửữựỳýỷỹỵđĐ]",
            text,
            re.IGNORECASE,
        ))

    def _split_inline_merged_options(self, text: str) -> str:
        """
        Split merged inline options on a single line.

        Input:  "A. x² B. y² C. z² D. t²"
        Output: "A. x²\nB. y²\nC. z²\nD. t²"

        Handles: "A. x² B. y²", "A. x B. y C. z", "A.x B.y C.z"
        """
        text = self._split_inline_colon_coordinate_options(text)

        # Pattern: option letter followed by dot, then content, then next option letter
        merged_re = re.compile(
            r"(?<=[\s])([A-D])\s*(?:[\.．\)）]|:(?!\s*\())\s*(.+?)(?=\s+[A-D]\s*(?:[\.．\)）]|:(?!\s*\())|$)",
            re.DOTALL,
        )

        def replace_merged(m: re.Match[str]) -> str:
            letter = m.group(1).upper()
            content = m.group(2).strip()
            return f"{letter}. {content}"

        result = merged_re.sub(replace_merged, text)

        # Second pass: catch any remaining "X. text Y. text" patterns (e.g. "B. S=3 C. S")
        result = re.sub(
            r"([A-D])\s*(?:[\.．\)）]|:(?!\s*\())\s*([^\n]+?)\s+([A-D])\s*(?:[\.．\)）]|:(?!\s*\())\s*",
            lambda m: f"{m.group(1)}. {m.group(2).strip()}\n{m.group(3)}. ",
            result,
        )

        # Third pass: "A.x B.y C.z" → one option per line (compact, no spaces after dots)
        result = re.sub(
            r"([A-D])[\.\)）．]\s*([^\sA-D]+?)\s+(?=[A-D][\.\)）．])",
            lambda m: f"{m.group(1)}. {m.group(2)}\n",
            result,
        )

        # Fourth pass: split any remaining compact markers inside a line
        # Example: "A.x 3 B.x 3 và x≠−1" -> "A. x 3\nB. x 3 và x≠−1"
        result = re.sub(
            r"\s+(?=([A-D])\s*[\.)．](?=\S)|([A-D])\s*:(?!\s*\())",
            "\n",
            result,
        )

        return result

    def _split_inline_colon_coordinate_options(self, text: str) -> str:
        marker_re = re.compile(r"([A-D])\s*:\s*(?=[\(\{\[])")
        normalized_lines: list[str] = []

        for line in text.split("\n"):
            matches = list(marker_re.finditer(line))
            if len(matches) < 2:
                normalized_lines.append(line)
                continue

            prefix = line[:matches[0].start()].strip()
            normalized_prefix = self._normalize_colon_label_context(prefix)
            if (
                normalized_prefix
                and self._looks_like_geometry_label_intro(normalized_prefix)
                and not self._looks_like_mcq_selection_stem(normalized_prefix)
            ):
                normalized_lines.append(line)
                continue

            parts: list[str] = []
            if prefix:
                parts.append(prefix)
            for idx, match in enumerate(matches):
                start = match.start()
                end = matches[idx + 1].start() if idx + 1 < len(matches) else len(line)
                segment = line[start:end].strip()
                if segment:
                    parts.append(segment)
            normalized_lines.append("\n".join(parts))

        return "\n".join(normalized_lines)

    # ─── Stem extraction ─────────────────────────────────────────────────────

    # Note: "Câu" starts with C — must require . or ) as separator to avoid
    # matching "C" in Vietnamese words like "Câu1". Colon is optional (some PDFs).
    OPT_MARKER_RE = re.compile(r"^\s*([A-D])\s*([\.)）．:])\s*(.*)$")

    def _extract_stem(self, text: str) -> str:
        """Extract question stem (everything before the first option marker)."""
        marker = self._first_option_marker(text)
        if marker:
            marker_start, letter = marker
            stem = text[:marker_start]
            if letter != "A":
                after_a = text[marker_start:marker_start + 10]
                if re.match(r"[A-D]\s*[\.)）．:]", after_a):
                    stem = re.sub(r"A\s*[\.)）．:]\s*$", "", stem.rstrip())
            return stem.strip()
        return text.strip()

    def _first_option_marker(self, text: str) -> Optional[tuple[int, str]]:
        """First option marker that has actual content."""
        lines = text.splitlines(keepends=True)
        marker_entries = self._collect_line_option_marker_entries(text)
        if not marker_entries:
            return None
        first_line_idx, letter, _, _ = marker_entries[0]
        line = lines[first_line_idx] if first_line_idx < len(lines) else ""
        match = self.OPT_MARKER_RE.match(line)
        if not match:
            return None
        line_start = sum(len(lines[idx]) for idx in range(first_line_idx))
        return line_start + match.start(), letter
        return None

    def _collect_line_option_marker_entries(
        self,
        text: str,
    ) -> list[tuple[int, str, str, str]]:
        lines = text.split("\n")
        marker_entries: list[tuple[int, str, str, str]] = []
        for line_idx, line in enumerate(lines):
            match = self.OPT_MARKER_RE.match(line)
            if not match:
                continue
            letter = match.group(1).upper()
            separator = match.group(2)
            content = match.group(3).strip()
            if not content:
                continue
            marker_entries.append((line_idx, letter, separator, content))
        if self._looks_like_colon_label_block(lines, marker_entries):
            return []
        return marker_entries

    def _looks_like_colon_label_block(
        self,
        lines: list[str],
        marker_entries: list[tuple[int, str, str, str]],
    ) -> bool:
        if len(marker_entries) < 1:
            return False
        if any(separator != ":" for _, _, separator, _ in marker_entries):
            return False
        if not all(content.startswith(("(", "{", "[")) for _, _, _, content in marker_entries):
            return False
        first_marker_idx = min(line_idx for line_idx, _, _, _ in marker_entries)
        stem_lines = [lines[idx].strip() for idx in range(first_marker_idx) if lines[idx].strip()]
        stem_text = self._normalize_colon_label_context(" ".join(stem_lines))
        if self._looks_like_mcq_selection_stem(stem_text):
            return False
        last_marker_idx = max(line_idx for line_idx, _, _, _ in marker_entries)
        trailing_lines = [
            lines[idx].strip()
            for idx in range(last_marker_idx + 1, len(lines))
            if lines[idx].strip()
        ]
        normalized_trailing = [
            self._normalize_colon_label_context(line)
            for line in trailing_lines
        ]
        if any(self._looks_like_mcq_selection_stem(line) for line in normalized_trailing):
            return False
        if not trailing_lines:
            return not stem_text or self._looks_like_geometry_label_intro(stem_text)
        if self._looks_like_geometry_label_intro(stem_text):
            return True
        return any(self._contains_vietnamese(line) for line in trailing_lines)

    def _looks_like_mcq_selection_stem(self, text: str) -> bool:
        if not text:
            return False
        normalized = text.strip().lower()
        if not normalized:
            return False
        return bool(re.search(
            r"\b(chọn|đáp\s*án|phương\s*án|mệnh\s*đề|khẳng\s*định|phát\s*biểu|kết\s*quả|giá\s*trị|nghiệm|tập\s*nghiệm|đúng|sai)\b",
            normalized,
            re.IGNORECASE,
        ))

    def _looks_like_geometry_label_intro(self, text: str) -> bool:
        if not text:
            return False
        normalized = text.strip().lower()
        return bool(re.search(
            r"\b(cho|gọi|xét)\b.*\b(các\s+)?(điểm|vector|vectơ|tập\s*hợp|tọa\s*độ)\b",
            normalized,
            re.IGNORECASE,
        ))

    def _normalize_colon_label_context(self, text: str) -> str:
        return re.sub(
            r"^\s*(?:câu|bài)\s*\d+(?:\s*\([^)]*\))?\s*[\.:]?\s*",
            "",
            text or "",
            flags=re.IGNORECASE,
        ).strip()

    def _trim_preamble_before_question(self, text: str, question_num: int) -> str:
        """Strip exam header text merged in the same block as 'Câu N'."""
        if not text or question_num <= 0:
            return text
        for label in ("Câu", "Bài"):
            m = re.search(rf"(?i){label}\s*{question_num}(?!\d)", text)
            if m:
                return text[m.start():].strip()
        return text.strip()

    def _calculate_formula_noise(self, text: str) -> float:
        """Calculate fraction of formula-like characters in text."""
        if not text:
            return 0.0
        text = map_pdf_private_use_math_glyphs(text)
        noise_chars = len(re.findall(
            r"[\d²³¹⁰⁴⁵⁶⁷⁸⁹⁺⁻⁼ⁿ∫∑∏√∂∆∈∉⊂⊃∪∩ℝℤℕ→←↔≥≤≠≡≈±×÷·∅{}]",
            text,
        ))
        noise_chars += len(re.findall(r"[\uf000-\uf8ff]", text)) * 3
        return noise_chars / max(len(text), 1)
