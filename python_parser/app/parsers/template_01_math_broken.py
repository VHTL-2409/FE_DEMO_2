"""
template_01_math_broken.py — Parser for math exam PDFs with broken formula text.

PDF characteristics:
  - Vietnamese math exam (Đại số, Hình học)
  - Text layer: formulas broken across multiple spans/lines
  - Superscripts/subscripts extracted as separate text elements
  - Answer key at end: "1.C 2.B 3.D" or "1-C 2-B"
  - May have essay section (Phần II: Tự luận)
  - Short exam: 1-5 pages

Examples:
  - pdf_mau_1.pdf (Đề Toán lớp 8)
"""

from __future__ import annotations

import os
import re
import time
from dataclasses import dataclass
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
from ..utils.pdf_reader import PdfReader
from ..utils.text_normalizer import normalize
from ..utils.image_cropper import crop_question
from ..utils.section_detector import detect_sections, SectionKind


@dataclass
class MathBrokenBlock:
    """A math question block with its Y-range for grouping."""
    question_num: int
    raw_text: str
    page: int
    y0: float
    y1: float
    bbox: tuple[float, float, float, float]
    is_essay: bool = False


class Template01MathBrokenParser(BaseParser):
    """
    Parser for math PDFs with broken formula text (pdf_mau_1 style).

    Strategy:
      1. Use PyMuPDF to get text blocks with precise Y coordinates
      2. Group spans by vertical proximity to reconstruct math expressions
      3. Parse "Câu N." pattern for question headers
      4. Extract answer key from tail of document
      5. For questions with high formula noise → render as image
      6. Separate essay section (Phần II: Tự luận)
    """

    template_type = TemplateType.TEMPLATE_01_MATH_BROKEN
    parser_name = "template_01_math_broken"

    def __init__(self, pdf_path: str, session_id: str | None = None):
        super().__init__(pdf_path, session_id)
        self._essay_cutoff_idx: int = 0  # index in blocks after which is essay
        self._answer_key: dict[int, str] = {}
        self._essay_section_open: bool = False

    def can_handle(self, profile: PdfProfile, file_path: str = "") -> float:
        """Score: high for Vietnamese math with formula noise and answer key."""
        return profile.score_template_01

    def _get_text_layout_aware(self) -> str:
        """Get text with proper line grouping for 2-column layouts."""
        return self.reader.get_all_text_layout_aware()

    def _parse_impl(self) -> tuple[list[ParsedQuestion], ExamMeta]:
        """Main parsing logic for template 01 with section-aware parsing."""
        start = time.time()
        questions: list[ParsedQuestion] = []

        # Use layout-aware text extraction for 2-column PDFs
        full_text = self._get_text_layout_aware()

        # Extract metadata
        meta = self.extract_meta(full_text)

        # Get page count and size
        page_count = self.reader.page_count
        page_width, page_height = (
            self.reader.get_page_size(1)
            if page_count > 0 else (612.0, 792.0)
        )

        # ─── Step 1: Detect sections using section_detector ────────────────────
        sections = detect_sections(full_text)

        # Extract answer key from ANSWER_KEY section if found
        self._answer_key = self._extract_answer_key_from_sections(full_text, sections)
        if not self._answer_key:
            # Fallback to tail extraction
            self._answer_key = self._extract_answer_key(full_text)

        # ─── Step 2: Parse blocks using PyMuPDF with layout info ───────────────
        self._essay_section_open = False
        math_blocks = self._parse_layout_blocks(page_width, page_height)
        # Split merged "Câu 4 + Phần II" blocks; emit essay blocks with is_essay=True
        math_blocks = self._expand_blocks_at_essay_boundary(math_blocks)
        # Drop exam header text that shares the same PDF block as "Câu N"
        for b in math_blocks:
            b.raw_text = self._trim_preamble_before_question(b.raw_text, b.question_num)

        # ─── Step 3: Split into MCQ and essay sections ──────────────────────────
        mcq_blocks, essay_blocks = self._split_mcq_essay(math_blocks, full_text)

        # ─── Step 4: Parse MCQ blocks with section info ────────────────────────
        for block in mcq_blocks:
            ctx = self._section_context_for_block(is_essay=False, sections=sections)
            q = self._parse_mcq_block(block, page_width, page_height, ctx)
            if q:
                questions.append(q)

        # ─── Step 5: Parse essay blocks with section info ──────────────────────
        for block in essay_blocks:
            ctx = self._section_context_for_block(is_essay=True, sections=sections)
            q = self._parse_essay_block(block, ctx)
            if q:
                questions.append(q)

        # Sort by question number
        questions.sort(key=lambda x: x.number)

        # Set total questions in meta
        meta.totalQuestions = len(questions)

        parse_time = int((time.time() - start) * 1000)
        meta.template = TemplateType.TEMPLATE_01_MATH_BROKEN

        return questions, meta

    def _extract_answer_key_from_sections(
        self,
        full_text: str,
        sections: list,
    ) -> dict[int, str]:
        """Extract answer key specifically from ANSWER_KEY section."""
        from ..utils.answer_extractor import AnswerExtractor

        lines = full_text.split("\n")
        for section in sections:
            if section.kind == SectionKind.ANSWER_KEY:
                # Extract text for this section
                section_lines = lines[section.start_line:section.end_line]
                section_text = "\n".join(section_lines)
                extractor = AnswerExtractor(section_text)
                answers = extractor.get_as_dict()
                if answers:
                    return answers
        return {}

    def _section_context_for_block(self, is_essay: bool, sections: list) -> dict:
        """Pick MCQ vs essay section title from detect_sections (not only the first)."""
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

    def _trim_preamble_before_question(self, text: str, question_num: int) -> str:
        """Strip đề header / hướng dẫn merged in the same block as 'Câu N'."""
        if not text or question_num <= 0:
            return text
        for label in ("Câu", "Bài"):
            m = re.search(rf"(?i){label}\s+{question_num}(?!\d)", text)
            if m:
                return text[m.start():].strip()
        return text.strip()

    def _expand_blocks_at_essay_boundary(
        self,
        blocks: list[MathBrokenBlock],
    ) -> list[MathBrokenBlock]:
        """When Phần II is merged into the last MCQ block, split and build essay blocks."""
        out: list[MathBrokenBlock] = []
        for b in blocks:
            out.extend(self._split_one_block_at_essay_section(b))
        return out

    def _split_one_block_at_essay_section(
        self,
        b: MathBrokenBlock,
    ) -> list[MathBrokenBlock]:
        text = b.raw_text
        if self._essay_section_open:
            return [
                MathBrokenBlock(
                    question_num=b.question_num,
                    raw_text=b.raw_text,
                    page=b.page,
                    y0=b.y0,
                    y1=b.y1,
                    bbox=b.bbox,
                    is_essay=True,
                )
            ]

        parts = re.split(
            r"(?is)(?=(?:^|\n)\s*Phần\s*(?:II|2|hai)\b\s*[:\.\-]?)",
            text,
            maxsplit=1,
        )
        if len(parts) < 2 or not parts[1].strip():
            return [b]

        mcq_part = parts[0].strip()
        tail = parts[1].strip()
        result: list[MathBrokenBlock] = []
        if mcq_part:
            result.append(
                MathBrokenBlock(
                    question_num=b.question_num,
                    raw_text=mcq_part,
                    page=b.page,
                    y0=b.y0,
                    y1=b.y1,
                    bbox=b.bbox,
                    is_essay=False,
                )
            )
        essay_chunks = self._essay_blocks_from_tail(tail, b.page, b.bbox, b.y1)
        result.extend(essay_chunks)
        if tail.strip():
            # Đã gặp Phần II — các block tiếp theo (tách trang) vẫn là tự luận
            self._essay_section_open = True
        return result if result else [b]

    def _essay_blocks_from_tail(
        self,
        tail: str,
        page: int,
        bbox: tuple[float, float, float, float],
        y_anchor: float,
    ) -> list[MathBrokenBlock]:
        """Parse 'Phần II … Câu 5 … Câu 6 …' tail into separate essay blocks."""
        tail = re.sub(
            r"(?is)^\s*Phần\s*(?:II|2|hai)\s*[:\.\-]?\s*(?:Tự\s*luận|TỰ\s*LUẬN)?\s*",
            "",
            tail,
            count=1,
        ).strip()
        tail = re.sub(r"(?is)^\s*Tự\s*luận\s*[:\.\-]?\s*\n?", "", tail, count=1).strip()

        first_hdr = re.search(r"(?mi)^\s*(?:Câu|Bài)\s+(\d+)", tail)
        if not first_hdr:
            return []
        if first_hdr.start() > 0:
            tail = tail[first_hdr.start():].strip()

        headers = list(re.finditer(r"(?mi)^\s*(?:Câu|Bài)\s+(\d+)", tail))
        blocks: list[MathBrokenBlock] = []
        for i, m in enumerate(headers):
            try:
                num = int(m.group(1))
            except ValueError:
                continue
            start = m.start()
            end = headers[i + 1].start() if i + 1 < len(headers) else len(tail)
            chunk = tail[start:end].strip()
            if chunk:
                blocks.append(
                    MathBrokenBlock(
                        question_num=num,
                        raw_text=chunk,
                        page=page,
                        y0=y_anchor,
                        y1=y_anchor,
                        bbox=bbox,
                        is_essay=True,
                    )
                )
        return blocks

    def _parse_layout_blocks(
        self,
        page_width: float,
        page_height: float,
    ) -> list[MathBrokenBlock]:
        """
        Parse PDF using layout-aware block extraction.

        Groups consecutive blocks (same question) together before parsing.
        Each block in the result represents ONE question (header + options).
        """
        all_blocks: list[MathBrokenBlock] = []

        for page_num in range(1, self.reader.page_count + 1):
            blocks = self.reader.get_text_blocks(page_num)

            # Group blocks by question number using Y proximity
            # Strategy: each "Câu N." starts a new question group
            current_num = 0
            current_blocks: list[dict] = []

            for block in blocks:
                text = block.text.strip()
                if not text:
                    continue

                bbox = block.bbox
                y0, y1 = bbox[1], bbox[3]

                # Extract question number
                for pat in [r"Câu\s+(\d+)", r"Bài\s+(\d+)"]:
                    m = re.search(pat, text, re.IGNORECASE)
                    if m:
                        try:
                            new_num = int(m.group(1))
                        except ValueError:
                            new_num = 0
                        break
                else:
                    new_num = 0

                if new_num > 0:
                    # Save previous group
                    if current_num > 0 and current_blocks:
                        combined_text = self._combine_blocks_text(current_blocks)
                        if combined_text.strip():
                            all_blocks.append(MathBrokenBlock(
                                question_num=current_num,
                                raw_text=combined_text,
                                page=page_num,
                                y0=current_blocks[0]["y0"],
                                y1=current_blocks[-1]["y1"],
                                bbox=self._blocks_to_bbox(current_blocks),
                            ))
                    current_num = new_num
                    current_blocks = [{"text": text, "y0": y0, "y1": y1, "bbox": bbox}]
                else:
                    current_blocks.append({"text": text, "y0": y0, "y1": y1, "bbox": bbox})

            # Don't forget the last group
            if current_num > 0 and current_blocks:
                combined_text = self._combine_blocks_text(current_blocks)
                if combined_text.strip():
                    all_blocks.append(MathBrokenBlock(
                        question_num=current_num,
                        raw_text=combined_text,
                        page=page_num,
                        y0=current_blocks[0]["y0"],
                        y1=current_blocks[-1]["y1"],
                        bbox=self._blocks_to_bbox(current_blocks),
                    ))

        return all_blocks

    def _combine_blocks_text(self, blocks: list[dict]) -> str:
        """Combine multiple text blocks into one text, sorted by Y position."""
        # Sort by y0 (top to bottom)
        sorted_blocks = sorted(blocks, key=lambda b: b["y0"])
        lines = []
        for b in sorted_blocks:
            t = b["text"].strip()
            if t:
                lines.append(t)
        return "\n".join(lines)

    def _blocks_to_bbox(self, blocks: list[dict]) -> tuple[float, float, float, float]:
        """Compute bounding box that covers all blocks."""
        x0 = min(b["bbox"][0] for b in blocks)
        y0 = min(b["bbox"][1] for b in blocks)
        x1 = max(b["bbox"][2] for b in blocks)
        y1 = max(b["bbox"][3] for b in blocks)
        return (x0, y0, x1, y1)

    def _split_mcq_essay(
        self,
        blocks: list[MathBrokenBlock],
        full_text: str,
    ) -> tuple[list[MathBrokenBlock], list[MathBrokenBlock]]:
        """Split blocks into MCQ (Phần I) and Essay (Phần II) sections."""
        mcq_blocks: list[MathBrokenBlock] = []
        essay_blocks: list[MathBrokenBlock] = []
        in_essay = False

        essay_patterns = [
            r"Phần\s*II\s*[:\.\-]",
            r"Phần\s*2\s*[:\.\-]",
            r"Phần\s*Tự\s*Luận",
            r"(?i)(?:^|\n)\s*Tự\s*luận\s*[:\.\-]",
        ]

        for block in blocks:
            if block.is_essay:
                in_essay = True
            elif not in_essay:
                for pat in essay_patterns:
                    if re.search(pat, block.raw_text, re.IGNORECASE):
                        in_essay = True
                        break
            block.is_essay = in_essay
            if in_essay:
                essay_blocks.append(block)
            else:
                mcq_blocks.append(block)

        return mcq_blocks, essay_blocks

    def _parse_mcq_block(
        self,
        block: MathBrokenBlock,
        page_width: float,
        page_height: float,
        section_info: dict,
    ) -> Optional[ParsedQuestion]:
        """Parse a single MCQ block with section awareness."""
        if block.question_num <= 0:
            return None

        text = block.raw_text
        num = block.question_num

        # Check formula noise level
        formula_noise = self._calculate_formula_noise(text)
        high_noise = formula_noise > 0.2

        # Parse options
        options = self._parse_options(text)

        # Determine render mode
        render_mode = RenderMode.IMAGE if high_noise else RenderMode.TEXT
        issues: list[str] = []
        confidence = 0.9 if options else 0.5

        if high_noise:
            issues.append(
                f"High formula noise ({formula_noise:.1%}), rendered as image."
            )

        # Check if we actually got options
        valid_options = {k: v for k, v in options.items() if v.strip()}
        if len(valid_options) < 2:
            issues.append(
                f"Only {len(valid_options)} options found. "
                "Consider converting to essay."
            )
            # If truly no options, convert to essay
            if not valid_options:
                render_mode = RenderMode.IMAGE
                confidence = 0.6

        # Get answer from answer key
        answer = self._answer_key.get(num)
        answer_location = "answer_table" if num in self._answer_key else "inline"
        if answer is None:
            # Try inline
            from ..utils.answer_extractor import extract_inline_answer
            answer = extract_inline_answer(text)
            if answer:
                answer_location = "inline"

        parsed_block = ParsedBlock(
            raw_text=text,
            question_num=num,
            page=block.page,
            bbox=block.bbox,
        )

        question = self.build_question(
            block=parsed_block,
            options=valid_options,
            q_type=QuestionType.MULTIPLE_CHOICE if valid_options else QuestionType.ESSAY,
            answer=answer,
            confidence=confidence,
            render_mode=render_mode,
            bbox=block.bbox if high_noise else None,
            issues=issues,
        )

        # Apply section awareness fields
        question.section = section_info.get("section")
        question.sectionKind = section_info.get("sectionKind") or SectionKind.MCQ.value
        question.answerLocation = answer_location
        question.needsGrading = False

        return question

    def _parse_essay_block(
        self,
        block: MathBrokenBlock,
        section_info: dict,
    ) -> Optional[ParsedQuestion]:
        """Parse a single essay block with section awareness."""
        if block.question_num <= 0:
            return None

        text = block.raw_text
        # Remove first-line header: "Câu 5 (4đ)." / "Câu 7 (1đ)." (điểm trong ngoặc tiếng Việt)
        lines = text.split("\n")
        if lines:
            lines[0] = re.sub(
                r"^\s*(?:Câu|Bài)\s+\d+(?:\s*\([^)]*\))?\s*[\.:]?\s*",
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
        text = re.split(
            r"(?i)\n?\s*-{3,}\s*hết\s*-{0,}",
            text,
            maxsplit=1,
        )[0].strip()

        parsed_block = ParsedBlock(
            raw_text=text,
            question_num=block.question_num,
            page=block.page,
            bbox=block.bbox,
        )

        question = self.build_question(
            block=parsed_block,
            options={},
            q_type=QuestionType.ESSAY,
            confidence=0.7,
            render_mode=RenderMode.IMAGE,  # essay = always image mode for math
            bbox=block.bbox,
        )

        # Apply section awareness fields
        question.section = section_info.get("section")
        question.sectionKind = section_info.get("sectionKind") or SectionKind.ESSAY.value
        question.answerLocation = "none"
        question.needsGrading = True

        return question

    def _parse_options(self, text: str) -> dict[str, str]:
        """
        Parse answer options from block text.
        Handles FOUR formats:
          1. Standard: "A. x²" on one line
          2. Empty marker: "A." (no content on that line)
          3. Inline merged: "A. x² B. y² C. z² D. t²" on one line
          4. Orphan letter-word: "A x²" without period (math broken text)
        """
        options: dict[str, str] = {}

        # Pass 1: Standard "A. content" — allow empty content too
        lines = text.split("\n")
        option_letter = None
        option_letters_found: list[str] = []

        for i, line in enumerate(lines):
            stripped = line.strip()
            # Detect option marker — may have empty content after period
            m = re.match(r"^\s*([A-D])\.\s*(.*)$", stripped)
            if m:
                letter = m.group(1)
                content = m.group(2).strip()
                option_letter = letter
                option_letters_found.append(letter)
                if content:
                    options[letter] = content
                    option_letter = None  # content found, stop accumulating

        # Pass 2: Accumulate content lines after empty-option markers
        # When we found "A." with no content, subsequent lines are its content
        # until we hit another option marker or exhausted lines
        if option_letter is not None and option_letter in option_letters_found:
            i = lines.index(next(l for l in lines if re.match(rf"^\s*{option_letter}\.", l.strip()))) + 1
            content_parts: list[str] = []
            for j in range(i, len(lines)):
                next_line = lines[j].strip()
                if not next_line:
                    continue
                if re.match(r"^\s*[A-D]\.\s*", next_line):
                    break
                content_parts.append(next_line)
            if content_parts:
                options[option_letter] = " ".join(content_parts).strip()

        # Pass 3: Inline merged "A. x² B. y² C. z² D. t²" on one line
        if len(options) < 2:
            merged_pattern = re.compile(
                r"([A-D])\.\s*([^\nA-D]{0,80}?)(?=\s+[A-D]\.|\s*$)"
            )
            for m in merged_pattern.finditer(text):
                letter = m.group(1)
                content = m.group(2).strip()
                if letter not in options:
                    options[letter] = content
                elif not options[letter] and content:
                    options[letter] = content

        # Pass 4: Orphan letter-word "A x²" without period (broken math text)
        # In broken text, formula content may appear as:
        #   "A" on one line, "x²" on next line (no period separator)
        if len(options) < 2:
            orphan_letter_pattern = re.compile(r"^\s*([A-D])\s+(\S+.*)$", re.MULTILINE)
            last_letter = None
            for m in orphan_letter_pattern.finditer(text):
                letter = m.group(1)
                word = m.group(2).strip()
                if letter not in options:
                    options[letter] = word
                    last_letter = letter
                elif last_letter == letter and options[letter] == word:
                    # Possible continuation — add to existing
                    pass

        return options

    def _calculate_formula_noise(self, text: str) -> float:
        """Calculate fraction of formula-like characters in text."""
        if not text:
            return 0.0
        noise_chars = len(re.findall(
            r"[\d²³¹⁰⁴⁵⁶⁷⁸⁹⁺⁻⁼ⁿ∫∑∏√∂∆∈∉⊂⊃∪∩ℝℤℕ→←↔≥≤≡≈±×÷·]",
            text
        ))
        return noise_chars / max(len(text), 1)

    def _extract_answer_key(self, text: str) -> dict[int, str]:
        """Extract answer key from the tail of the document."""
        from ..utils.answer_extractor import AnswerExtractor
        extractor = AnswerExtractor(text, tail_lines=30)
        return extractor.get_as_dict()
