"""
template_04_docx_viet.py — Parser for Vietnamese E-Commerce exam DOCX (docx_mau_1).

DOCX structure:
  - Phần 1 (30 câu MCQ):  câu N. (0.200 Point)
      Options A/B/C/D; answer letter is on option text prefixed by "*"
      Example:
        1. (0.200 Point)
        Câu hỏi?
        A. Option A
        B. Option B
        *C. Option C (correct — asterisk on same line)
        D. Option D
  - Phần 2 (36 câu fill-in): N. (0.250 Point)
      Short text answers prefixed by "- " or "=>"
      Example: "- Đáp án đúng"
  - Phần 3 (7+ câu essay): TỰ LUẬN section
      No options; full-text answer after question
"""

from __future__ import annotations

import re

from .docx_base import DocxBaseParser
from ..schemas import ExamMeta, ParsedQuestion, QuestionType, RenderMode, RenderInfo, TemplateType
from ..utils.section_detector import SectionKind


class Template04DocxVietParser(DocxBaseParser):
    """Parser for Vietnamese commerce exam with 3 distinct sections."""

    template_type = TemplateType.TEMPLATE_04_DOCX_VIETNAMESE
    parser_name = "template_04_docx_viet"

    def can_handle(self, profile: PdfProfile, file_path: str = "") -> float:
        # Primary: distinguish by filename pattern
        fp = file_path or getattr(profile, "file_path", "")
        if fp.lower().endswith(".docx"):
            if re.search(r"docx_mau_1|thuong_mai|commerce", fp, re.IGNORECASE):
                return 0.95
            return 0.5
        score = 0.0
        if profile.language in ("vi", "mixed"):
            score += 0.4
        if profile.has_cau_pattern:
            score += 0.2
        return score

    def _parse_impl(self):
        paragraphs = self._get_paragraphs()
        full_text = self._get_full_text()
        meta = self.extract_meta(full_text)

        questions: list[ParsedQuestion] = []
        q_number = 0

        # ── Split into sections ──────────────────────────────────────────────────
        section_idx, fill_idx, essay_idx = self._find_sections(paragraphs)

        # Section 1: MCQ (Phần 1)
        mcq_paras = paragraphs[section_idx:fill_idx] if fill_idx > section_idx else []
        mcq_questions = self._parse_mcq_section(mcq_paras)
        questions.extend(mcq_questions)

        # Section 2: Fill-in (Phần 2)
        fill_paras = paragraphs[fill_idx:essay_idx] if essay_idx > fill_idx else []
        fill_questions = self._parse_fill_section(fill_paras, len(questions))
        questions.extend(fill_questions)

        # Section 3: Essay (Tự luận)
        essay_paras = paragraphs[essay_idx:] if essay_idx < len(paragraphs) else []
        essay_questions = self._parse_essay_section(essay_paras, len(questions))
        questions.extend(essay_questions)

        meta.totalQuestions = len(questions)
        return questions, meta

    def _find_sections(self, paragraphs):
        """Return start indices of: (mcq_section, fill_section, essay_section)."""
        section_idx = 0
        fill_idx = len(paragraphs)
        essay_idx = len(paragraphs)

        for i, p in enumerate(paragraphs):
            t = p.text.strip().lstrip('\ufeff').strip()
            if not t:
                continue

            # Case-insensitive, whitespace-flexible, BOM-safe section detection
            if re.match(r"^\s*Phần\s*2", t, re.IGNORECASE) or "ĐIỀN ĐÁP ÁN" in t.upper():
                if fill_idx == len(paragraphs):
                    fill_idx = i
            if re.match(r"^\s*TỰ\s*LUẬN", t, re.IGNORECASE) or "TỰ LUẬN" in t.upper():
                essay_idx = i
                break

        return section_idx, fill_idx, essay_idx

    def _parse_mcq_section(self, paragraphs):
        """Parse MCQ questions from Phần 1 (docx_mau_1 style).

        Each question block spans one or more paragraph lines:
          N. (0.200 Point)  OR:  N.
                                     (0.200 Point)
          Câu hỏi text
          A. Option A
          B. Option B
          *C. Option C  ← asterisk before letter
          D. Option D
        """
        questions = []
        i = 0

        while i < len(paragraphs):
            p = paragraphs[i]
            t = p.text.strip().lstrip('\ufeff').strip()

            # Detect question number marker — possibly split across two lines:
            # Line 1: "1."   Line 2: "(0.200 Point)"
            is_header = False
            header_text = t

            if re.match(r"^\d+\.\s*\(\d+\.\d+\s*Point\)$", t):
                # Single-line format: "1. (0.200 Point)"
                is_header = True
            elif re.match(r"^\d+\.\s*$", t):
                # Line 1 of split format: "1."
                if i + 1 < len(paragraphs):
                    next_t = paragraphs[i + 1].text.strip().lstrip('\ufeff').strip()
                    if re.match(r"^\(\d+\.\d+\s*Point\)$", next_t):
                        header_text = t + " " + next_t
                        is_header = True
                        i += 1  # skip the "(0.200 Point)" line

            if is_header:
                m = re.match(r"^(\d+)\.", header_text)
                q_num = int(m.group(1)) if m else 0
                block_lines = [header_text]
                j = i + 1

                # Collect all lines belonging to this question
                while j < len(paragraphs):
                    next_t = paragraphs[j].text.strip().lstrip('\ufeff').strip()
                    # Stop at next question marker or section marker
                    if re.match(r"^\d+\.\s*\(\d+\.\d+\s*Point\)$", next_t):
                        break
                    if re.match(r"^\d+\.\s*$", next_t):
                        # Peek: if next line after this looks like "(0.200 Point)", it's a header
                        if j + 1 < len(paragraphs) and re.match(r"^\(\d+\.\d+\s*Point\)$", paragraphs[j + 1].text.strip().lstrip('\ufeff').strip()):
                            break
                    if re.match(r"^\s*Phần\s*2", next_t, re.IGNORECASE) or "ĐIỀN ĐÁP ÁN" in next_t.upper() or re.match(r"^\s*TỰ\s*LUẬN", next_t, re.IGNORECASE):
                        break
                    block_lines.append(next_t)
                    j += 1

                q = self._parse_mcq_block(q_num, block_lines, section="Phần 1 - Trắc nghiệm")
                questions.append(q)
                i = j
            else:
                i += 1

        return questions

    def _parse_mcq_block(self, q_num: int, lines: list[str], section: str = None) -> ParsedQuestion:
        """Parse a single MCQ block into a ParsedQuestion with section awareness."""
        # First line is the header: "N. (0.200 Point)"
        header = lines[0] if lines else ""
        question_text_lines = []
        options: dict[str, str] = {}
        answer: str | None = None
        issues: list[str] = []

        for line in lines[1:]:
            t = line.strip()
            if not t:
                continue

            # Option line: "A. ..." or "*C. ..." (asterisk before letter)
            m = re.match(r"^(\*?)([A-D])\.\s*(.*)$", t)
            if m:
                asterisk, letter, content = m.group(1), m.group(2).upper(), m.group(3).strip()
                options[letter] = content
                if asterisk == "*":
                    answer = letter
            else:
                # Could be multi-line question stem or options without letter
                # Heuristic: if starts with A-D followed by period → option, else → stem
                stem_m = re.match(r"^([A-D])\.\s*(.*)$", t)
                if stem_m and len(t) < 200:
                    letter = stem_m.group(1).upper()
                    content = stem_m.group(2).strip()
                    if letter not in options:
                        options[letter] = content
                else:
                    question_text_lines.append(t)

        stem = " ".join(question_text_lines).strip()

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
        question.section = section or "Phần 1 - Trắc nghiệm"
        question.sectionKind = SectionKind.MCQ.value
        question.answerLocation = "inline" if answer else "none"
        question.needsGrading = False

        return question

    def _parse_fill_section(self, paragraphs, start_num: int) -> list[ParsedQuestion]:
        """Parse fill-in-the-blank questions from Phần 2."""
        questions = []
        q_num = start_num + 1
        i = 0

        while i < len(paragraphs):
            p = paragraphs[i]
            t = p.text.strip().lstrip('\ufeff').strip()
            if not t:
                i += 1
                continue

            # Skip section headers (BOM-safe, case-insensitive)
            if re.match(r"^\s*Phần\s*\d", t, re.IGNORECASE) or "ĐIỀN ĐÁP ÁN" in t.upper() or re.match(r"^\s*TỰ\s*LUẬN", t, re.IGNORECASE):
                i += 1
                continue

            # Question line: "N. (0.250 Point)Câu hỏi?"
            m = re.match(r"^(\d+)\.\s*\(0\.250\s*Point\)(.*)$", t)
            if m:
                q_num = int(m.group(1))
                raw_question = m.group(2).strip()
                i += 1

                # Collect answer lines (start with "- " or "=>")
                answer_parts: list[str] = []
                while i < len(paragraphs):
                    next_t = paragraphs[i].text.strip().lstrip('\ufeff').strip()
                    if re.match(r"^(\d+)\.\s*\(0\.250\s*Point\)", next_t):
                        break
                    if next_t.startswith("- ") or next_t.startswith("=>"):
                        answer_parts.append(next_t.lstrip("-=> ").strip())
                    elif next_t.startswith("-"):
                        answer_parts.append(next_t.lstrip("-").strip())
                    elif re.match(r"^\s*Phần", next_t, re.IGNORECASE) or re.match(r"^\s*TỰ\s*LUẬN", next_t, re.IGNORECASE):
                        break
                    elif not next_t:
                        pass
                    else:
                        # Multi-line answer text
                        answer_parts.append(next_t)
                    i += 1

                answer_text = " ".join(answer_parts).strip()

                question = self._build_question(
                    number=q_num,
                    text=raw_question,
                    q_type=QuestionType.FILL_BLANK,
                    answer=answer_text or None,
                    confidence=0.9 if answer_text else 0.5,
                    issues=[] if answer_text else ["Fill-in without extracted answer."],
                )

                # Apply section awareness fields
                question.section = "Phần 2 - Điền đáp án"
                question.sectionKind = SectionKind.QUESTION.value
                question.answerLocation = "inline" if answer_text else "none"
                question.needsGrading = False

                questions.append(question)
            else:
                i += 1

        return questions

    def _parse_essay_section(self, paragraphs, start_num: int) -> list[ParsedQuestion]:
        """Parse essay questions from TỰ LUẬN section with section awareness."""
        questions = []
        q_num = start_num + 1

        for p in paragraphs:
            t = p.text.strip().lstrip('\ufeff').strip()
            if not t:
                continue
            if re.match(r"^\s*TỰ\s*LUẬN", t, re.IGNORECASE) or re.match(r"^\s*Phần", t, re.IGNORECASE):
                continue

            # Essay question: starts with number
            m = re.match(r"^(\d+)\.\s*\(0\.250\s*Point\)\s*(.*)$", t)
            if m:
                q_num = int(m.group(1))
                raw_question = m.group(2).strip()

                question = self._build_question(
                    number=q_num,
                    text=raw_question,
                    q_type=QuestionType.ESSAY,
                    confidence=0.8,
                    issues=["Essay — requires teacher grading."],
                )

                # Apply section awareness fields
                question.section = "Phần 3 - Tự luận"
                question.sectionKind = SectionKind.ESSAY.value
                question.answerLocation = "none"
                question.needsGrading = True

                questions.append(question)
            elif not re.match(r"^[A-D]\.|\*|Phần", t):
                # Possibly a multi-line essay question; append as continuation
                if questions and questions[-1].type == QuestionType.ESSAY:
                    questions[-1].text += " " + t

        return questions

    def _calc_confidence(self, stem, options, answer, issues):
        """Calculate confidence score for a parsed question."""
        score = 0.8
        if answer:
            score += 0.15
        if len(options) == 4:
            score += 0.05
        if len(stem) > 20:
            score += 0.05
        score -= len(issues) * 0.1
        return max(0.0, min(1.0, score))
