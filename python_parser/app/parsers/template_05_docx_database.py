"""
template_05_docx_database.py — Parser for Vietnamese Database exam DOCX (docx_mau_2).

DOCX structure:
  - 30 MCQ questions, numbered 1-30
  - Each question block:
      N.                     (number on its own line)
      Question text line(s)
      A. Option A
      B. Option B
      C. Option C
      *D. Option D  ← asterisk BEFORE the letter
  - No essay, no fill-in, no section markers
  - Clean text, no formulas → RenderMode.TEXT always
"""

from __future__ import annotations

import re

from .docx_base import DocxBaseParser
from ..schemas import ExamMeta, ParsedQuestion, QuestionType, RenderMode, RenderInfo, TemplateType
from ..utils.section_detector import SectionKind


class Template05DocxDatabaseParser(DocxBaseParser):
    """Parser for Vietnamese Database exam — 30 MCQ with asterisk-before-letter answer format."""

    template_type = TemplateType.TEMPLATE_05_DOCX_DATABASE

    def can_handle(self, profile: PdfProfile, file_path: str = "") -> float:
        # Primary: distinguish by filename pattern
        fp = file_path or getattr(profile, "file_path", "")
        if fp.lower().endswith(".docx"):
            if re.search(r"docx_mau_2|co_so|database|data.bases|csdl", fp, re.IGNORECASE):
                return 0.95
            return 0.5
        score = 0.0
        if profile.language in ("vi", "mixed"):
            score += 0.4
        return score

    def _parse_impl(self):
        paragraphs = self._get_paragraphs()
        full_text = self._get_full_text()
        meta = self.extract_meta(full_text)
        questions = self._parse_questions(paragraphs)
        meta.totalQuestions = len(questions)
        return questions, meta

    def _parse_questions(self, paragraphs) -> list[ParsedQuestion]:
        """
        Parse all MCQ questions from the DOCX.

        Block pattern per question:
            N.                ← number line (only a number and period)
            Question text
            [optional more text lines]
            A. Option A
            B. Option B
            C. Option C
            *D. Option D     ← correct option has asterisk before letter

        We collect lines between two consecutive number markers.
        """
        questions: list[ParsedQuestion] = []
        i = 0

        while i < len(paragraphs):
            t = paragraphs[i].text.strip()

            # Detect question number line: only a number followed by period
            m = re.match(r"^(\d+)\.$", t)
            if m:
                q_num = int(m.group(1))
                block_lines: list[str] = []
                j = i + 1

                # Collect lines until next number marker or EOF
                while j < len(paragraphs):
                    next_t = paragraphs[j].text.strip()
                    if re.match(r"^\d+\.$", next_t):
                        break
                    block_lines.append(next_t)
                    j += 1

                q = self._parse_question_block(q_num, block_lines)
                questions.append(q)
                i = j
            else:
                i += 1

        return questions

    def _parse_question_block(self, q_num: int, lines: list[str]) -> ParsedQuestion:
        """
        Parse a single question block with section awareness:
        - Lines starting with "A.", "B.", "C.", "D." or "*A.", "*B.", etc. → options
        - The asterisk prefix marks the correct answer
        - All remaining lines → question stem
        Handles orphan single-word options: "C. development" (no extra text after letter).
        """
        stem_parts: list[str] = []
        options: dict[str, str] = {}
        answer: str | None = None
        issues: list[str] = []

        for line in lines:
            t = line.strip()
            if not t:
                continue

            # Option line: "A. ..." or "*D. ..." (asterisk before letter)
            opt_m = re.match(r"^(\*?)([A-D])\.\s*(.*)$", t)
            if opt_m:
                asterisk = opt_m.group(1)
                letter = opt_m.group(2).upper()
                content = opt_m.group(3).strip()
                # Only treat as option if content is non-empty OR it has an asterisk
                if content or asterisk:
                    options[letter] = content
                    if asterisk == "*":
                        answer = letter
                    continue

            # Orphan single-word option: "C. development" without extra text
            # These appear in vocabulary questions where options are single words
            orphan_m = re.match(r"^([A-D])\.\s*(\S+)\s*$", t)
            if orphan_m and orphan_m.group(1) not in options:
                letter = orphan_m.group(1).upper()
                word = orphan_m.group(2).strip()
                options[letter] = word
                continue

            # Not an option line → stem
            stem_parts.append(t)

        stem = " ".join(stem_parts).strip()

        if answer is None:
            issues.append("No answer marked with * in this question.")
        if len(options) < 2:
            issues.append(f"Only {len(options)} option(s) found.")

        confidence = self._calc_confidence(stem, options, answer, issues)

        question = self._build_question(
            number=q_num,
            text=stem,
            q_type=QuestionType.MULTIPLE_CHOICE,
            options=options,
            answer=answer,
            confidence=confidence,
            issues=issues,
        )

        # Apply section awareness fields
        question.section = "Cơ sở dữ liệu"
        question.sectionKind = SectionKind.MCQ.value
        question.answerLocation = "inline" if answer else "none"
        question.needsGrading = False

        return question

    def _calc_confidence(self, stem, options, answer, issues):
        """Calculate confidence score."""
        score = 0.85
        if answer:
            score += 0.1
        if len(options) == 4:
            score += 0.05
        if len(stem) > 15:
            score += 0.05
        score -= len(issues) * 0.1
        return max(0.0, min(1.0, score))
