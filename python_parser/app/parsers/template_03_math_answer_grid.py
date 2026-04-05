"""
template_03_math_answer_grid.py — Parser for math PDFs with answer grid + solutions.

PDF characteristics:
  - Vietnamese math exam
  - Three distinct sections:
      1. "Phần câu hỏi" — questions with Cau N:
      2. "Phần đáp án" — answer key table
      3. "Phần lời giải chi tiết" — detailed solutions
  - Answer key at bottom: "1.D 2.C 3.B..."
  - May have math formulas, graphs, diagrams
  - Long exam: 10-50 pages, 50 questions

Examples:
  - pdf_mau_3.pdf (Đề Toán THPT QG)
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
from ..utils.section_detector import detect_sections, SectionKind


class Template03MathAnswerGridParser(BaseParser):
    """
    Parser for math PDFs with structured answer grid + solutions (pdf_mau_3 style).

    Strategy:
      1. Identify three sections: câu hỏi → đáp án → lời giải
      2. Parse "Câu N:" pattern for questions
      3. Extract answer key from the answer section
      4. Parse solutions by matching "Câu N." in solution section
      5. For high noise questions → image mode
    """

    template_type = TemplateType.TEMPLATE_03_MATH_ANSWER_GRID
    parser_name = "template_03_math_answer_grid"

    def __init__(self, pdf_path: str, session_id: str | None = None):
        super().__init__(pdf_path, session_id)
        self._sections: dict[str, str] = {}
        self._solutions: dict[int, str] = {}

    def can_handle(self, profile: PdfProfile, file_path: str = "") -> float:
        """Score: high for math with answer grid and solution section."""
        return profile.score_template_03

    def _get_text_layout_aware(self) -> str:
        """Get text with proper line grouping for 2-column layouts."""
        return self.reader.get_all_text_layout_aware()

    def _parse_impl(self) -> tuple[list[ParsedQuestion], ExamMeta]:
        """Main parsing logic for template 03."""
        start = time.time()
        questions: list[ParsedQuestion] = []

        # Use layout-aware text extraction for 2-column PDFs
        full_text = self._get_text_layout_aware()
        meta = self.extract_meta(full_text)

        # ─── Step 1: Identify sections ──────────────────────────────────────
        self._sections = self._identify_sections(full_text)

        # Cross-check with section_detector for mixed exam detection
        detector_sections = detect_sections(full_text)
        has_essay = any(s.kind == SectionKind.ESSAY for s in detector_sections)
        if has_essay and "solution" not in self._sections:
            import logging
            logging.getLogger(__name__).warning(
                "template_03: detected ESSAY section but no solution header. "
                "Exam may be mixed MCQ + Essay format."
            )

        # ─── Step 2: Extract answer key ────────────────────────────────────
        answer_section = self._sections.get("answer", "")
        solution_section = self._sections.get("solution", "")
        self._answer_key = self._extract_answer_key(answer_section + "\n" + solution_section)

        # ─── Step 3: Extract solutions ─────────────────────────────────────
        self._solutions = self._extract_solutions(solution_section)

        # ─── Step 4: Parse questions ───────────────────────────────────────
        # The questions section now contains the actual questions (or is empty if not found)
        question_section = self._sections.get("questions", "")

        # If questions section is empty or too short, extract from full_text
        if not question_section.strip() or len(question_section.strip()) < 200:
            # Find questions before the solution section
            question_section = self._truncate_before_answer_or_solution(full_text)
            question_section = self._trim_before_first_real_cau_one(question_section)
        else:
            # Questions section exists, just trim before first real question
            question_section = self._trim_before_first_real_cau_one(question_section)

        section_spans = detect_sections(question_section)
        blocks = self._split_question_blocks(question_section)

        for block in blocks:
            q = self._parse_question_block(block)
            if q:
                self._apply_section_tags(q, question_section, section_spans)
                questions.append(q)

        # Sort
        questions.sort(key=lambda x: x.number)

        meta.totalQuestions = len(questions)
        meta.template = TemplateType.TEMPLATE_03_MATH_ANSWER_GRID

        return questions, meta

    def _identify_sections(self, text: str) -> dict[str, str]:
        """
        Identify the three main sections of the document.
        Returns dict: {"questions": "", "answer": "", "solution": ""}
        """
        sections: dict[str, str] = {"questions": "", "answer": "", "solution": ""}

        # Patterns that mark section boundaries
        question_start = re.compile(
            r"Phần\s*câu\s*hỏi|"
            r"Bắt\s*đầu\s*mỗi\s*câu|"
            r"Câu\s*1\s*:",
            re.IGNORECASE
        )
        answer_start = re.compile(
            r"Phần\s*đáp\s*án|"
            r"bảng\s*đáp\s*án|"
            r"đáp\s*án\s*(?:như\s*)?bên|"
            r"(?:^|\n)\s*1\.\s*[A-D]",
            re.IGNORECASE | re.MULTILINE
        )
        solution_start = re.compile(
            r"Phần\s*lời\s*giải|"
            r"Giải\s*chi\s*tiết|"
            r"lời\s*giải\s*chi\s*tiết",
            re.IGNORECASE
        )

        lines = text.split("\n")
        section_starts: dict[str, int] = {}
        section_order: list[str] = []

        for i, line in enumerate(lines):
            if question_start.search(line) and "questions" not in section_starts:
                section_starts["questions"] = i
                section_order.append("questions")
            elif answer_start.search(line) and "answer" not in section_starts:
                section_starts["answer"] = i
                section_order.append("answer")
            elif solution_start.search(line) and "solution" not in section_starts:
                section_starts["solution"] = i
                section_order.append("solution")

        # Build section texts
        for idx, section in enumerate(section_order):
            start_idx = section_starts[section]
            if idx + 1 < len(section_order):
                end_idx = section_starts[section_order[idx + 1]]
            else:
                end_idx = len(lines)
            sections[section] = "\n".join(lines[start_idx:end_idx])

        # If we only found "questions" section, use the whole text
        if not any(sections.values()):
            sections["questions"] = text

        return sections

    def _truncate_before_answer_or_solution(self, text: str) -> str:
        """
        Cắt phần lời giải chi tiết (trùng nhãn Câu 1…50).
        Không dùng 'Phần đáp án' đầu đề — thường nằm trong khung hướng dẫn ngắn.
        """
        m = re.search(r"(?is)HƯỚNG\s*DẪN\s*GIẢI\s*CHI\s*TIẾT", text)
        if m is not None and m.start() > 1500:
            return text[: m.start()].strip()
        return text

    def _trim_before_first_real_cau_one(self, text: str) -> str:
        """
        Bỏ phần đầu đề trước câu 1 thật.
        Bỏ qua 'Ví dụ: Câu 1:…' trong khung hướng dẫn.
        """
        for m in re.finditer(r"(?is)Câu\s+1\s*[:\.]\s*", text):
            before = text[max(0, m.start() - 120) : m.start()].lower()
            if "ví dụ" in before:
                continue
            after = text[m.end() : m.end() + 100].strip()
            if len(after) < 12:
                continue
            if after.startswith("…") or after.startswith(","):
                continue
            return text[m.start() :].strip()
        return text

    def _apply_section_tags(
        self,
        question: ParsedQuestion,
        question_section_text: str,
        sections: list,
    ) -> None:
        """Gắn Phần I (TN) / Phần II (TL) theo vị trí dòng trong phần câu hỏi."""
        m = re.search(rf"(?is)Câu\s+{question.number}\s*[:\.]", question_section_text)
        if not m:
            return
        line_idx = question_section_text.count("\n", 0, m.start())
        for section in sections:
            if section.kind not in (SectionKind.MCQ, SectionKind.ESSAY):
                continue
            if section.start_line <= line_idx < section.end_line:
                question.section = section.title
                question.sectionKind = section.kind.value
                if section.kind == SectionKind.ESSAY:
                    question.type = QuestionType.ESSAY
                    question.needsGrading = True
                    question.options = {}
                else:
                    question.needsGrading = False
                break

    def _split_question_blocks(self, text: str) -> list[ParsedBlock]:
        """Split question section into individual question blocks."""
        blocks: list[ParsedBlock] = []

        # Pattern: "Câu N:" (with colon)
        cau_pattern = re.compile(r"Câu\s+(\d+)\s*[:\.]")
        # Pattern: "Câu N." (no colon)

        # Also detect page markers
        page_markers: dict[int, int] = {}
        for m in re.finditer(r"\[---\s*Trang\s*(\d+)\s*---\]", text):
            try:
                page_markers[m.start()] = int(m.group(1))
            except ValueError:
                pass

        def get_page(pos: int) -> int:
            pages = sorted(page_markers.keys())
            for p in reversed(pages):
                if pos >= p:
                    return page_markers[p]
            return 1

        # Find all question starts
        positions: list[tuple[int, int, int]] = []  # (start, end_pos, num)
        for m in cau_pattern.finditer(text):
            num = int(m.group(1))
            positions.append((m.start(), m.end(), num))

        for i, (start, end, num) in enumerate(positions):
            if i + 1 < len(positions):
                content_end = positions[i + 1][0]
            else:
                content_end = len(text)

            content = text[end:content_end].strip()
            # Remove page markers
            content = re.sub(r"\[---\s*Trang\s*\d+\s*---\]\s*", "", content)

            blocks.append(ParsedBlock(
                raw_text=content,
                question_num=num,
                page=get_page(start),
            ))

        return blocks

    def _parse_question_block(self, block: ParsedBlock) -> Optional[ParsedQuestion]:
        """Parse a single question block."""
        text = block.raw_text
        num = block.question_num

        # Remove "Câu N:" header
        clean = re.sub(r"^Câu\s+\d+\s*[:\.]\s*", "", text, count=1, flags=re.IGNORECASE).strip()

        # Parse options
        options = self._parse_options(clean)

        # Extract stem
        stem = self._extract_stem(clean, options)

        # Calculate formula noise
        formula_noise = self._calculate_formula_noise(stem)
        high_noise = formula_noise > 0.15

        # Determine render mode
        render_mode = RenderMode.IMAGE if high_noise else RenderMode.TEXT
        valid_options = {k: v for k, v in options.items() if v.strip()}
        q_type = (
            QuestionType.MULTIPLE_CHOICE
            if len(valid_options) >= 2
            else QuestionType.ESSAY
        )

        if high_noise:
            issues = [f"High formula noise ({formula_noise:.1%}), rendered as image."]
        else:
            issues = []

        # Get answer
        answer = self._answer_key.get(num)

        # Get explanation
        explanation = self._solutions.get(num)

        confidence = 0.8 if valid_options else 0.5
        if answer:
            confidence += 0.1
        if explanation:
            confidence = min(confidence + 0.05, 1.0)

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
            explanation=explanation,
            confidence=confidence,
            render_mode=render_mode,
            bbox=block.bbox if high_noise else None,
            issues=issues,
        )

    def _parse_options(self, text: str) -> dict[str, str]:
        """Parse answer options from question text.

        Handles both single-line and multi-line options (common in math PDFs where
        formulas span multiple lines). Each option starts with "A." / "B." / etc.

        The PDF text extraction from pdf_mau_3 puts "A. " on its own line followed by
        the formula content on subsequent lines.
        """
        options: dict[str, str] = {}

        # Strategy: Find all option header positions, then extract content between them
        # Pattern: lines that START with "X. " (option header)
        lines = text.split("\n")

        # Find all option header positions
        option_positions: list[tuple[int, str]] = []  # (line_index, letter)
        for i, line in enumerate(lines):
            m = re.match(r"^\s*([A-D])\.\s*$", line)
            if m:
                option_positions.append((i, m.group(1)))

        # If no header-only options found, try to find inline options
        if not option_positions:
            inline_pattern = re.compile(r"^\s*([A-D])\.\s*(.+)$", re.MULTILINE)
            for m in inline_pattern.finditer(text):
                letter = m.group(1)
                content = m.group(2).strip()
                if content:
                    options[letter] = content
            return options

        # Extract content between option headers
        for idx, (line_idx, letter) in enumerate(option_positions):
            # Collect lines after the option header until next option or end
            content_lines = []
            for i in range(line_idx + 1, len(lines)):
                line = lines[i]
                # Stop if we hit another option header
                if re.match(r"^\s*[A-D]\.\s*$", line):
                    break
                # Collect non-empty lines
                stripped = line.strip()
                if stripped:
                    content_lines.append(stripped)

            option_text = " ".join(content_lines)
            if option_text:
                options[letter] = option_text

        return options

    def _extract_stem(self, text: str, options: dict[str, str]) -> str:
        """Extract question stem (text before the first option letter)."""
        lines = text.split("\n")
        result: list[str] = []

        for line in lines:
            # Stop when we hit an option letter
            if re.match(r"^\s*[A-D]\.\s*", line):
                break
            result.append(line)

        return "\n".join(result).strip()

    def _calculate_formula_noise(self, text: str) -> float:
        """Calculate formula noise score."""
        if not text:
            return 0.0
        noise = len(re.findall(
            r"[\d²³¹⁰⁴⁵⁶⁷⁸⁹⁺⁻⁼ⁿ∫∑∏√∂∆∈∉⊂⊃∪∩ℝℤℕ→←↔≥≤≡≈±×÷·]",
            text
        ))
        return noise / max(len(text), 1)

    def _extract_answer_key(self, text: str) -> dict[int, str]:
        """Extract answer key from answer/solution sections."""
        extractor = AnswerExtractor(text, tail_lines=4000)
        return extractor.get_as_dict()

    def _extract_solutions(self, solution_text: str) -> dict[int, str]:
        """
        Extract solutions keyed by question number.
        Pattern: "Câu N." followed by solution text.
        """
        solutions: dict[int, str] = {}

        if not solution_text.strip():
            return solutions

        # Pattern: "Câu N." at the start of a line/paragraph
        cau_solution = re.compile(
            r"^Câu\s+(\d+)\s*[\.:]\s*(.+?)(?=^Câu\s+\d+\s*[\.:]|\Z)",
            re.MULTILINE | re.DOTALL
        )

        for m in cau_solution.finditer(solution_text):
            try:
                num = int(m.group(1))
                sol = m.group(2).strip()
                # Clean up page markers
                sol = re.sub(r"\[---\s*Trang\s*\d+\s*---\]\s*", "", sol)
                if sol:
                    solutions[num] = sol
            except (ValueError, IndexError):
                continue

        return solutions
