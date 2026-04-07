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

        # Step 2: Build blocks from question regions
        math_blocks = self._regions_to_blocks(regions, raw_rows)

        # Step 3: Split into MCQ and essay sections
        full_text = "\n".join(
            r.text for r in raw_rows if r.text.strip()
        )
        sections = detect_sections(full_text)
        self._answer_key = self._extract_answer_key(full_text, sections)

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

            block = MathRebuiltBlock(
                question_num=region.number,
                raw_text=region.raw_text,
                raw_lines=region.raw_lines,
                page=region.page,
                y0=region.y0,
                y1=region.y1,
                is_essay=self._essay_section_open,
                raw_tokens_by_line=region.raw_tokens_by_line,
            )

            if re.search(r"(?i)Phần\s*(?:II|2|hai)\s*[:\.\-]?\s*(?:Tự\s*luận)?", block.raw_text):
                self._essay_section_open = True

            blocks.append(block)

        return blocks

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
        extractor = AnswerExtractor(full_text, tail_lines=30)
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
        return {
            "section": "PHẦN II" if is_essay else "PHẦN I",
            "sectionKind": SectionKind.ESSAY.value if is_essay else SectionKind.MCQ.value,
            "answerLocation": "none" if is_essay else "inline",
        }

    # ─── MCQ / Essay split ─────────────────────────────────────────────────

    ESSAY_PATTERNS = [
        r"Phần\s*II\s*[:\.\-]",
        r"Phần\s*2\s*[:\.\-]",
        r"Phần\s*Tự\s*Luận",
        r"(?i)(?:^|\n)\s*Tự\s*luận\s*[:\.\-]",
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

        # Check formula noise to decide render mode
        formula_noise = self._calculate_formula_noise(stem_clean)
        high_noise = formula_noise > 0.2
        render_mode = RenderMode.IMAGE if high_noise else RenderMode.LATEX

        valid_options = {k: v for k, v in options.items() if v.strip()}
        issues: list[str] = []
        confidence = 0.9 if len(valid_options) >= 2 else 0.5

        if high_noise:
            issues.append(f"High formula noise ({formula_noise:.1%}), rendered as image.")

        if len(valid_options) < 2:
            issues.append(f"Only {len(valid_options)} options found.")

        # Get answer from answer key
        answer = self._answer_key.get(block.question_num)
        answer_location = "answer_table" if answer else "inline"
        if answer is None:
            answer = extract_inline_answer(stem_clean)
            if answer:
                answer_location = "inline"

        # LaTeX conversion
        stem_latex = convert_to_latex(stem_clean, mode="auto")
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
                if len(valid_options) >= 2
                else QuestionType.ESSAY
            ),
            answer=answer,
            confidence=confidence,
            render_mode=render_mode,
            bbox=None,
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
        question.contentType = classify_content_type(stem_clean, stem_latex)

        if answer is not None:
            ans = str(answer).strip()
            if re.match(r"^[A-Da-d]$", ans):
                question.answer = ans.upper()

        return question

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

        OPT_MARKER_RE = re.compile(r"^\s*([A-D])\s*[\.)）．]\s*")

        # Step 1: find all option markers + their X-positions
        marker_info: dict[int, tuple[str, str, float]] = {}
        # line_idx -> (letter, content_after_marker, x0_of_marker)

        for i, line in enumerate(lines):
            stripped = line.strip()
            if not stripped:
                continue
            tokens = tokens_by_line[i] if i < len(tokens_by_line) else None
            m = OPT_MARKER_RE.match(stripped)
            if not m:
                continue
            letter = m.group(1)
            if letter in {mi[0] for mi in marker_info.values()}:
                continue
            marker_x0 = float(m.start()) + 20.0
            if tokens:
                marker_x0 = self._find_marker_x0(tokens, letter)
            marker_info[i] = (letter, stripped[m.end():].strip(), marker_x0)

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
                if OPT_MARKER_RE.match(line_j):
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
                m = OPT_MARKER_RE.match(line.strip())
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
        if re.match(r"^\s*[A-D]\.\s*", s):
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
        # Pattern: option letter followed by dot, then content, then next option letter
        merged_re = re.compile(
            r"(?<=[\s])([A-D])\s*[\.．)]\s*(.+?)(?=\s+[A-D]\s*[\.．)]|$)",
            re.DOTALL,
        )

        def replace_merged(m: re.Match[str]) -> str:
            letter = m.group(1).upper()
            content = m.group(2).strip()
            return f"{letter}. {content}"

        result = merged_re.sub(replace_merged, text)

        # Second pass: catch any remaining "X. text Y. text" patterns (e.g. "B. S=3 C. S")
        result = re.sub(
            r"([A-D])\s*\.\s*([^\n]+?)\s+([A-D])\s*\.\s*",
            lambda m: f"{m.group(1)}. {m.group(2).strip()}\n{m.group(3)}. ",
            result,
        )

        # Third pass: "A.x B.y C.z" → one option per line (compact, no spaces after dots)
        result = re.sub(
            r"([A-D])\.\s*([^\sA-D]+?)\s+(?=[A-D]\.)",
            lambda m: f"{m.group(1)}. {m.group(2)}\n",
            result,
        )

        return result

    # ─── Stem extraction ─────────────────────────────────────────────────────

    # Note: "Câu" starts with C — must require . or ) as separator to avoid
    # matching "C" in Vietnamese words like "Câu1". Colon is optional (some PDFs).
    OPT_MARKER_RE = re.compile(r"^\s*([A-D])\s*[\.)）．:]\s*")

    def _extract_stem(self, text: str) -> str:
        """Extract question stem (everything before the first option marker)."""
        m = self._first_option_marker(text)
        if m:
            stem = text[:m.start()]
            letter = m.group(1)
            if letter != "A":
                after_a = text[m.start():m.start() + 10]
                if re.match(r"[A-D]\s*[\.)）．:]", after_a):
                    stem = re.sub(r"A\s*[\.)）．:]\s*$", "", stem.rstrip())
            return stem.strip()
        return text.strip()

    def _first_option_marker(self, text: str) -> Optional[re.Match]:
        """First option marker that has actual content."""
        # Require dot/paren as separator — avoids matching "C" in "Câu1."
        for m in re.finditer(r"(?:^|\n)\s*([A-D])\s*[\.)）．]\s*\S", text):
            tail = text[m.end():m.end() + 14].lstrip()
            if re.match(r"^[A-D]\s*[\.)）．]", tail):
                continue
            return m
        return None

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
        noise_chars = len(re.findall(
            r"[\d²³¹⁰⁴⁵⁶⁷⁸⁹⁺⁻⁼ⁿ∫∑∏√∂∆∈∉⊂⊃∪∩ℝℤℕ→←↔≥≤≡≈±×÷·∅{}]",
            text,
        ))
        return noise_chars / max(len(text), 1)
