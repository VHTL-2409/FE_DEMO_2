"""
template_06_english_exam.py — Parser for English exam PDFs (pdf_mau_2_new style).

PDF characteristics:
  - Vietnamese high school English exam (THPT)
  - Questions numbered: "Question 1:", "Question 2:", etc.
  - Options A, B, C, D on separate lines (clean text)
  - Answer key at the end: "1-D 2-C 3-A" (number + dash + letter)
  - Clean text, no formulas → RenderMode.TEXT
  - 50 MCQ questions

Examples:
  - pdf_mau_2_new.pdf (Đề thi THPT Tiếng Anh 2020-2021)
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
from ..utils.section_detector import SectionKind


class Template06EnglishExamParser(BaseParser):
    """
    Parser for English exam PDFs with clean "Question N:" format.

    Strategy:
      1. Extract full text from PDF
      2. Detect answer key section at the end
      3. Parse each "Question N:" block
      4. Extract options A, B, C, D
      5. Match questions with answer key
    """

    template_type = TemplateType.TEMPLATE_06_ENGLISH_EXAM
    parser_name = "template_06_english_exam"

    def can_handle(self, profile: PdfProfile, file_path: str = "") -> float:
        """Score: high for English THPT exams with Question N: format."""
        fp = file_path or getattr(profile, "file_path", "")
        if fp.lower().endswith(".pdf"):
            if re.search(r"mau_2_new|english|anh|tmph|thpt", fp, re.IGNORECASE):
                return 0.95
            return 0.5
        score = 0.0
        if profile.language in ("en", "mixed"):
            score += 0.4
        if profile.has_question_pattern:
            score += 0.3
        if not profile.has_essay_section:
            score += 0.2
        return score

    def _get_text_layout_aware(self) -> str:
        """Get text with proper line grouping for 2-column layouts."""
        return self.reader.get_all_text_layout_aware()

    def _parse_impl(self) -> tuple[list[ParsedQuestion], ExamMeta]:
        """Main parsing logic for template 06 (English exam)."""
        start = time.time()
        questions: list[ParsedQuestion] = []

        # Use layout-aware text extraction for 2-column PDFs
        full_text = self._get_text_layout_aware()

        # Extract metadata
        meta = self.extract_meta(full_text)

        # Extract answer key from the end of the document
        self._answer_key = self._extract_answer_key(full_text)

        # Parse all questions
        questions = self._parse_all_questions(full_text)

        # Set total questions in meta
        meta.totalQuestions = len(questions)

        parse_time = int((time.time() - start) * 1000)
        meta.template = TemplateType.TEMPLATE_06_ENGLISH_EXAM

        return questions, meta

    def _extract_answer_key(self, text: str) -> dict[int, str]:
        """Extract answer key from the tail of the document.

        Format: "1-D 2-C 3-A" (number + dash + letter)
        Also supports: "1.D 2.C 3.A" (dot format)
        """
        from ..utils.answer_extractor import AnswerExtractor

        lines = text.split("\n")
        tail_text = "\n".join(lines[-100:] if len(lines) > 100 else lines)

        # Find the "ĐÁP ÁN" section marker
        dap_an_idx = -1
        for i, line in enumerate(lines):
            if re.search(r"đáp\s*án|answer\s*key", line, re.IGNORECASE):
                dap_an_idx = i
                break

        if dap_an_idx >= 0:
            answer_section = "\n".join(lines[dap_an_idx:])
        else:
            answer_section = tail_text

        extractor = AnswerExtractor(answer_section)
        answers = extractor.get_as_dict()

        # Fallback: try dash-separated format "1-D 2-C"
        if not answers:
            answers = {}
            pattern = re.compile(r"(?<!\d)(\d{1,2})\s*[-–—]\s*([A-Da-d])(?:\s|$|[,;])", re.MULTILINE)
            for m in pattern.finditer(answer_section):
                try:
                    num = int(m.group(1))
                    ans = m.group(2).upper()
                    answers[num] = ans
                except ValueError:
                    continue

        return answers

    def _parse_all_questions(self, text: str) -> list[ParsedQuestion]:
        """Parse all questions from the text."""
        questions: list[ParsedQuestion] = []

        # Pattern: "Question N:" or "Question N."
        question_pattern = re.compile(
            r"Question\s+(\d+)[\.:]\s*",
            re.IGNORECASE
        )

        # Find all question positions
        matches = list(question_pattern.finditer(text))

        for i, match in enumerate(matches):
            q_num = int(match.group(1))
            start_pos = match.end()

            # End position is either next question or end of text
            end_pos = matches[i + 1].start() if (i + 1) < len(matches) else len(text)

            # Extract question block text
            block_text = text[start_pos:end_pos].strip()

            # Parse the question
            question = self._parse_question_block(q_num, block_text)
            if question:
                questions.append(question)

        return questions

    def _parse_question_block(self, q_num: int, block_text: str) -> Optional[ParsedQuestion]:
        """Parse a single question block.

        Format:
          Question text...
          A. Option A
          B. Option B
          C. Option C
          D. Option D
        """
        lines = block_text.split("\n")
        stem_lines: list[str] = []
        options: dict[str, str] = {}
        issues: list[str] = []

        current_option = None
        option_pattern = re.compile(r"^\s*([A-D])\.\s*(.*)$")

        for line in lines:
            stripped = line.strip()
            if not stripped:
                continue

            # Check for option line
            opt_match = option_pattern.match(stripped)
            if opt_match:
                letter = opt_match.group(1).upper()
                content = opt_match.group(2).strip()

                # Check if this is a continuation of previous option
                if letter in options and not options[letter]:
                    options[letter] = content
                else:
                    options[letter] = content
                current_option = letter
            else:
                # Check if this is the start of options (no letter prefix yet)
                # Sometimes options might be indented or on same line
                if current_option and options.get(current_option):
                    # Check if line starts with new option letter
                    if re.match(r"^[A-D]\.\s", stripped):
                        pass  # Will be caught in next iteration
                    elif not re.match(r"^[A-D]\.", stripped):
                        # Continuation of previous option
                        options[current_option] += " " + stripped
                else:
                    # This is question stem text
                    stem_lines.append(stripped)

        stem = " ".join(stem_lines).strip()

        # Clean up stem - remove leading question text fragments
        stem = self._clean_stem(stem)

        # Get answer from answer key
        answer = self._answer_key.get(q_num)
        answer_location = "answer_table" if q_num in self._answer_key else "inline"

        if answer is None:
            issues.append(f"No answer found in answer key for question {q_num}.")

        if len(options) < 2:
            issues.append(f"Only {len(options)} option(s) found.")

        confidence = self._calc_confidence(stem, options, answer)

        parsed_block = ParsedBlock(
            raw_text=stem + "\n" + "\n".join(f"{k}. {v}" for k, v in options.items()),
            question_num=q_num,
            page=1,  # TODO: detect page number
        )

        question = self.build_question(
            block=parsed_block,
            options=options,
            q_type=QuestionType.MULTIPLE_CHOICE,
            answer=answer,
            confidence=confidence,
            render_mode=RenderMode.TEXT,
            issues=issues,
            display_text=stem,
        )

        # Apply section awareness fields
        question.section = "Đề thi THPT - Tiếng Anh"
        question.sectionKind = SectionKind.MCQ.value
        question.answerLocation = answer_location
        question.needsGrading = False

        return question

    def _clean_stem(self, stem: str) -> str:
        """Clean up the question stem text."""
        if not stem:
            return stem

        # Remove any leading fragments that might be from formatting
        stem = re.sub(r"^\s*[-–—]\s*", "", stem)
        stem = re.sub(r"^\s*–\s*", "", stem)

        # Remove page numbers
        stem = re.sub(r"\b\d+\s*$", "", stem)

        return stem.strip()

    def _calc_confidence(self, stem: str, options: dict, answer: Optional[str]) -> float:
        """Calculate confidence score for the parsed question."""
        score = 0.7

        # More points for having all 4 options
        if len(options) == 4:
            score += 0.15

        # Points for having an answer
        if answer:
            score += 0.1

        # Points for having substantial question text
        if len(stem) > 30:
            score += 0.05

        return max(0.0, min(1.0, score))
