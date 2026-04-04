"""
template_02_clean_mcq.py — Parser for clean English MCQ exam PDFs.

PDF characteristics:
  - English language
  - Clean text extraction (no broken formulas)
  - Question format: "Question 1:", "Question 2:"
  - Options: clearly separated A/B/C/D on individual lines
  - May have answer key
  - May have explanations / solution sections
  - No math-heavy content

Examples:
  - pdf_mau_2.pdf (Đề Tiếng Anh THPT)
"""

from __future__ import annotations

import re
import time
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
from ..utils.answer_extractor import AnswerExtractor
from ..utils.section_detector import detect_sections, has_essay_section, SectionKind


class Template02CleanMcqParser(BaseParser):
    """
    Parser for clean English MCQ PDFs (pdf_mau_2 style).

    Strategy:
      1. Parse "Question N:" headers
      2. Extract 4 options (A/B/C/D) on separate lines
      3. Try to find answer key at bottom
      4. Extract solution/explanation sections if present
      5. Most questions → render as text (confidence high)
    """

    template_type = TemplateType.TEMPLATE_02_CLEAN_MCQ
    parser_name = "template_02_clean_mcq"

    def can_handle(self, profile: PdfProfile, file_path: str = "") -> float:
        """Score: high for clean English MCQ PDFs."""
        return profile.score_template_02

    def _parse_impl(self) -> tuple[list[ParsedQuestion], ExamMeta]:
        """Main parsing logic for template 02."""
        start = time.time()
        questions: list[ParsedQuestion] = []

        full_text = self.get_all_text()
        meta = self.extract_meta(full_text)

        # Extract answer key
        self._answer_key = self._extract_answer_key(full_text)

        # Check for essay sections (mixed exam)
        if has_essay_section(full_text):
            import logging
            logging.getLogger(__name__).warning(
                "template_02: detected ESSAY section in what was expected to be clean MCQ. "
                "Consider using template_01 for mixed exams."
            )

        # Handle sequential Vietnamese inline answers
        sequential_answers: dict[int, str] = {}
        if "__sequential__" in self._answer_key:
            sequential_answers = self._answer_key.pop("__sequential__")

        # Split into question blocks
        blocks = self._split_question_blocks(full_text)

        # Parse each block
        for block in blocks:
            q = self._parse_question_block(block)
            if q:
                questions.append(q)

        # Sort by question number
        questions.sort(key=lambda x: x.number)

        # Apply sequential answers to questions by position
        if sequential_answers:
            for i, q in enumerate(questions):
                if q.answer is None:
                    ans = sequential_answers.get(i + 1)
                    if ans:
                        q.answer = ans

        # Set total
        meta.totalQuestions = len(questions)
        meta.template = TemplateType.TEMPLATE_02_CLEAN_MCQ

        return questions, meta

    def _split_question_blocks(self, text: str) -> list[ParsedBlock]:
        """
        Split text by 'Question N:' pattern.
        Handles various formats: "Question 1:", "Question 1.", etc.
        """
        blocks: list[ParsedBlock] = []

        # Pattern: "Question N:" with various separators
        question_re = re.compile(
            r"Question\s+(\d+)\s*[:\.\)]",
            re.IGNORECASE
        )

        # Find all question start positions
        positions: list[tuple[int, int, int, str]] = []  # (start, end, num, header)
        for m in question_re.finditer(text):
            start = m.start()
            end = m.end()
            num = int(m.group(1))
            header = m.group(0)
            positions.append((start, end, num, header))

        # Detect page numbers in text
        page_markers: dict[int, int] = {}  # text_pos -> page_num
        for m in re.finditer(r"\[---\s*Trang\s*(\d+)\s*---\]", text):
            try:
                page_num = int(m.group(1))
                page_markers[m.start()] = page_num
            except ValueError:
                pass

        def get_page(pos: int) -> int:
            """Find which page a text position belongs to."""
            pages = sorted(page_markers.keys())
            for p in reversed(pages):
                if pos >= p:
                    return page_markers[p]
            return 1

        # Extract content between questions
        for i, (start, end, num, header) in enumerate(positions):
            if i + 1 < len(positions):
                next_start = positions[i + 1][0]
            else:
                next_start = len(text)

            content = text[end:next_start].strip()

            # Remove page markers from content
            content = re.sub(r"\[---\s*Trang\s*\d+\s*---\]\s*", "", content)

            blocks.append(ParsedBlock(
                raw_text=header + "\n" + content,
                question_num=num,
                page=get_page(start),
            ))

        return blocks

    def _parse_question_block(self, block: ParsedBlock) -> Optional[ParsedQuestion]:
        """Parse a single question block."""
        text = block.raw_text
        num = block.question_num

        # ─── Step 1: Remove question header ─────────────────────────────────
        # "Question 1: ..." → "..."
        clean = re.sub(
            r"^Question\s+\d+\s*[:\.\)]\s*",
            "",
            text,
            count=1,
            flags=re.IGNORECASE
        ).strip()

        # ─── Step 2: Parse options ──────────────────────────────────────────
        options = self._parse_options(clean)

        # ─── Step 3: Extract stem (everything before first option) ─────────
        stem = self._extract_stem(clean, options)

        # ─── Step 4: Determine question type ───────────────────────────────
        valid_options = {k: v for k, v in options.items() if v.strip()}
        q_type = (
            QuestionType.MULTIPLE_CHOICE
            if len(valid_options) >= 2
            else QuestionType.ESSAY
        )

        # ─── Step 5: Get answer ─────────────────────────────────────────────
        answer = self._answer_key.get(num)
        if answer is None:
            # Try inline
            from ..utils.answer_extractor import extract_inline_answer
            answer = extract_inline_answer(stem)

        # ─── Step 6: Calculate confidence ─────────────────────────────────
        confidence = self._calculate_confidence(
            stem, valid_options, answer, q_type
        )

        # ─── Step 7: Build result ───────────────────────────────────────────
        parsed_block = ParsedBlock(
            raw_text=stem + "\n" + "\n".join(
                f"{k}. {v}" for k, v in valid_options.items()
            ),
            question_num=num,
            page=block.page,
            bbox=block.bbox,
        )

        return self.build_question(
            block=parsed_block,
            options=valid_options,
            q_type=q_type,
            answer=answer,
            confidence=confidence,
            render_mode=RenderMode.TEXT,
            bbox=block.bbox,
        )

    def _parse_options(self, text: str) -> dict[str, str]:
        """
        Parse A/B/C/D options from question text.
        Handles three formats:
          1. Standard: "A. content" on one line
          2. Inline merged: "A. content   B. content" on one line
          3. 2-column orphan: "A. word" without other options on same line
             (from pronunciation/stress questions with 2-column layout)
        """
        options: dict[str, str] = {}

        # Pass 1: Standard "A. content" on one line
        option_re = re.compile(r"^\s*([A-D])\.\s*(.+)$", re.MULTILINE)
        for m in option_re.finditer(text):
            letter = m.group(1)
            content = m.group(2).strip()
            if content:
                options[letter] = content

        # Pass 2: Inline merged "A. content   B. content" on one line
        # (from 2-column pronunciation layout where both A and B are on same line)
        if len(options) < 4:
            merged_re = re.compile(r"([A-D])\.\s*([^\n]{1,60}?)(?=\s+[A-D]\.|\s*$)")
            for m in merged_re.finditer(text):
                letter = m.group(1)
                content = m.group(2).strip()
                if letter not in options and content:
                    options[letter] = content
                elif not options.get(letter) and content:
                    options[letter] = content

        # Pass 3: Orphan lines — single letter.word without other options on same line
        # Detects 2-column pronunciation format:
        #   A. knowledgeable   B. prosperity
        #   C. open
        #   C. development   D. paper
        # Orphan lines have no period after the word (single word = option text).
        # Assign by detecting which letter is missing from options.
        if len(options) < 4:
            orphan_re = re.compile(r"^([A-D])\.\s*(\S+)\s*$", re.MULTILINE)
            found_letters = set(options.keys())
            expected_order = ["A", "B", "C", "D"]
            orphan_assignments: list[tuple[str, str]] = []
            for m in orphan_re.finditer(text):
                letter = m.group(1)
                word = m.group(2).strip()
                # Only consider if this letter is not yet filled
                if letter not in found_letters:
                    orphan_assignments.append((letter, word))
            # Sort orphans by expected letter order and assign
            orphan_assignments.sort(key=lambda x: expected_order.index(x[0]) if x[0] in expected_order else 99)
            for letter, word in orphan_assignments:
                if letter not in options:
                    options[letter] = word

        return options

    def _extract_stem(self, text: str, options: dict[str, str]) -> str:
        """Extract question stem (text before first option)."""
        lines = text.split("\n")

        # Find first option line
        option_letter = None
        for line in lines:
            m = re.match(r"^\s*([A-D])\.\s*", line)
            if m:
                option_letter = m.group(1)
                break

        if option_letter is None:
            return text.strip()

        # Take everything before the first option
        result_lines: list[str] = []
        for line in lines:
            if re.match(rf"^\s*{option_letter}\.\s*", line):
                break
            result_lines.append(line)

        return "\n".join(result_lines).strip()

    def _calculate_confidence(
        self,
        stem: str,
        options: dict[str, str],
        answer: Optional[str],
        q_type: QuestionType,
    ) -> float:
        """Calculate parsing confidence for this question."""
        score = 0.5  # base

        if q_type == QuestionType.MULTIPLE_CHOICE:
            if len(options) == 4:
                score += 0.2
            elif len(options) >= 2:
                score += 0.1

            if answer:
                score += 0.15

            if len(stem) > 20:
                score += 0.1

            if all(v.strip() for v in options.values()):
                score += 0.05

        elif q_type == QuestionType.ESSAY:
            if len(stem) > 30:
                score += 0.3

        return min(score, 1.0)

    def _extract_answer_key(self, text: str) -> dict[int, str]:
        """
        Extract answer key from the document.

        For Vietnamese inline format (Chọn B.), maps answers to
        question numbers by their sequential position in the text.
        """
        from ..utils.answer_extractor import AnswerExtractor

        extractor = AnswerExtractor(text, tail_lines=4000)
        raw_answers = extractor.extract()

        if not raw_answers:
            return {}

        # If answers have valid question numbers, use them directly
        numbered = {e.number: e.answer for e in raw_answers if e.number > 0}
        if len(numbered) >= 2:
            return numbered

        # Vietnamese inline format: answers appear in order (Q1, Q2, ...)
        # So we need to map them to questions by position
        # Return {0: list of answers} as a signal to use positional mapping
        # We'll handle this in _parse_impl by matching inline answers to questions by order
        if raw_answers and raw_answers[0].format_used == "vietnamese_inline":
            # Return dict with sequential answers indexed by position
            sequential: dict[int, str] = {}
            for i, entry in enumerate(raw_answers):
                sequential[i + 1] = entry.answer  # 1-indexed
            return {"__sequential__": sequential}

        return numbered
