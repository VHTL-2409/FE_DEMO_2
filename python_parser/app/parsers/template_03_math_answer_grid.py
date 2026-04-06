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
from ..utils.text_normalizer import normalize_math_text, sanitize_word_equation_pua
from ..utils.latex_converter import convert_to_latex


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
        Bo phan huong dan/gioi thieu truoc cau hoi that.
        Xu ly cac pattern:
        - "Câu 1:…" (voi cham cham)
        - "Ví dụ: Câu 1:…"
        - "Bắt đầu mỗi câu bằng từ Câu"
        """
        # Pattern cho phan gioi thieu huong dan
        guide_patterns = [
            r"Bắt\s*đầu\s*mỗi\s*câu",  # "Bắt đầu mỗi câu bằng từ Câu"
            r"Ví\s*dụ\s*:?\s*Câu",        # "Ví dụ: Câu 1:…"
            r"Kéo\s*đề\s*thi",            # "Kéo đề thi dạng pdf vào..."
        ]

        # Tìm vị trí cuối cùng của phần giới thiệu
        last_guide_end = 0
        for pattern in guide_patterns:
            for match in re.finditer(pattern, text, re.IGNORECASE):
                if match.start() < 2000:  # Chi bo neu o dau document
                    last_guide_end = max(last_guide_end, match.end())

        # Tìm "Câu 1:" thật sự sau phần giới thiệu
        search_start = last_guide_end
        search_text = text[search_start:search_start + 1000] if search_start > 0 else text

        for m in re.finditer(r"(?is)Câu\s+1\s*[:\.]\s*", search_text):
            before = search_text[max(0, m.start() - 150) : m.start()].lower()
            after = search_text[m.end() : m.end() + 100].strip()

            # Bo qua neu co "vi du" trong context
            if "ví dụ" in before:
                continue

            # Bo qua neu noi dung qua ngan hoac chi co cham cham
            if len(after) < 20:
                continue
            if after.startswith("…") or after.startswith(","):
                continue

            # Tinh toan vi tri that trong text goc
            absolute_start = search_start + m.start()
            return text[absolute_start:].strip()

        # Fallback: tim "Câu 1:" dau tien voi cac kiem tra nhu cu
        for m in re.finditer(r"(?is)Câu\s+1\s*[:\.]\s*", text):
            # Chi tim trong 3000 ky tu dau
            if m.start() > 3000:
                break

            before = text[max(0, m.start() - 150) : m.start()].lower()
            after = text[m.end() : m.end() + 80].strip()

            # Bo qua neu co "vi du" trong context
            if "ví dụ" in before:
                continue

            # Bo qua neu noi dung qua ngan
            if len(after) < 20 or after.startswith("…") or after.startswith(","):
                continue

            # Bo qua neu chi co "example" hoac huong dan
            if len(after) < 30 and any(k in before.lower() for k in ["hướng dẫn", "lưu ý", "chú ý"]):
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

        # Remove "Câu N:" header — tách đề / đáp án để xử lý PUA khác nhau (Word Equation)
        raw = re.sub(r"^Câu\s+\d+\s*[:\.]\s*", "", text, count=1, flags=re.IGNORECASE).strip()
        raw = self._trim_orphan_after_last_option_line(raw)
        m_opt = self._first_option_match(raw)
        if m_opt:
            stem_raw = self._stem_before_first_option(raw, m_opt)
            opt_raw = raw[m_opt.start() :].strip()
            stem_clean = sanitize_word_equation_pua(stem_raw, strip_private_use=True)
            stem_clean = normalize_math_text(stem_clean, preserve_private_use=False)
            opt_clean = sanitize_word_equation_pua(opt_raw, strip_private_use=False)
            opt_clean = normalize_math_text(opt_clean, preserve_private_use=True)
            clean = (stem_clean + "\n" + opt_clean).strip()
        else:
            clean = normalize_math_text(
                sanitize_word_equation_pua(raw, strip_private_use=True),
                preserve_private_use=False,
            )

        # Parse options (inline A. B. C. D. + multi-line)
        options = self._parse_options(clean)
        # PDF lỗi hàng "A. B.  V =" — mất A; giữ ô A trống để giáo viên bổ sung
        if "A" not in options and "B" in options:
            options = {**{"A": ""}, **{k: options[k] for k in "ABCD" if k in options}}

        # Extract stem
        stem = self._extract_stem(clean, options)
        stem, options = self._peel_stem_tail_into_hollow_options(stem, options)
        stem = self._trim_stem_equation_junk(stem)

        # Calculate formula noise
        formula_noise = self._calculate_formula_noise(stem)
        high_noise = formula_noise > 0.22

        # Prefer LaTeX for readable math on FE (giống template_01)
        render_mode = RenderMode.IMAGE if high_noise else RenderMode.LATEX
        valid_options = {k: v for k, v in options.items() if v.strip()}
        if options.get("A") == "":
            valid_options["A"] = ""
        nonempty_opts = sum(1 for v in valid_options.values() if v.strip())
        q_type = (
            QuestionType.MULTIPLE_CHOICE
            if nonempty_opts >= 2
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

        option_lines = "\n".join(
            f"{k}. {valid_options[k]}" for k in sorted(valid_options.keys())
        )
        parsed_block = ParsedBlock(
            raw_text=(stem + "\n" + option_lines).strip(),
            question_num=num,
            page=block.page,
            bbox=block.bbox,
        )

        q = self.build_question(
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
        if not high_noise:
            try:
                q.latexContent = convert_to_latex(q.text, mode="auto")
                if valid_options:
                    q.latexOptions = {
                        k: convert_to_latex(v, mode="inline")
                        for k, v in valid_options.items()
                    }
            except Exception:
                pass
        return q

    def _collect_option_markers(self, text: str) -> list[re.Match]:
        """
        Mọi vị trí A./B./C./D. hợp lệ: đầu dòng, hoặc sau khoảng trắng nhưng không sau chữ
        (tránh khớp D trong 'ABCD .').

        pdf_mau_3: dòng \"A. \" tách riêng, công thức ở dòng sau — cần khớp marker sát cuối dòng.
        """
        found: list[re.Match] = []
        # Nội dung cùng dòng: \"A. y = …\"
        for m in re.finditer(
            r"(?:^|\n)\s*([A-D])\s*[\.)．:]\s+\S", text, re.MULTILINE
        ):
            found.append(m)
        # Chỉ \"A.\" / \"A. \" rồi hết dòng
        for m in re.finditer(
            r"(?:^|\n)\s*([A-D])\s*[\.)．:]\s*$", text, re.MULTILINE
        ):
            found.append(m)
        # Bất kỳ khoảng trắng sau dấu (tương thích bản cũ)
        for m in re.finditer(
            r"(?:^|\n)\s*([A-D])\s*[\.)．:]\s+", text, re.MULTILINE
        ):
            found.append(m)
        for m in re.finditer(r"(?<=\s)([A-D])\s*[\.)．:]\s+(?=\S)", text):
            if m.start() > 0 and text[m.start() - 1].isalpha():
                continue
            found.append(m)
        found.sort(key=lambda x: x.start())
        out: list[re.Match] = []
        for m in found:
            if not out or m.start() != out[-1].start():
                out.append(m)
        return out

    def _first_option_match(self, text: str) -> Optional[re.Match]:
        """
        Đáp án đầu tiên có nội dung (bỏ 'A. B.' rỗng).
        """
        for m in self._collect_option_markers(text):
            tail = text[m.end() : m.end() + 14].lstrip()
            if re.match(r"^[A-D]\s*[\.)．:]", tail):
                continue
            return m
        return None

    def _stem_before_first_option(self, text: str, m: re.Match) -> str:
        """Phần đề; bỏ 'A.' treo nếu đáp án đầu là B. (kiểu A. B. V = …)."""
        stem = text[: m.start()]
        letter = m.group(1)
        if letter != "A":
            stem = re.sub(r"A\s*[\.)．:]\s*$", "", stem.rstrip())
        return stem.strip()

    def _parse_options_by_markers(self, text: str) -> dict[str, str]:
        """
        Tách A. … B. … trên một hoặc nhiều dòng (sau khi layout-aware đã gom hàng).
        """
        options: dict[str, str] = {}
        m0 = self._first_option_match(text)
        if not m0:
            return options
        rest = text[m0.start() :]
        matches = self._collect_option_markers(rest)
        if len(matches) < 2:
            return options
        for i, m in enumerate(matches):
            letter = m.group(1)
            a, b = m.end(), matches[i + 1].start() if i + 1 < len(matches) else len(rest)
            chunk = rest[a:b].strip()
            chunk = re.sub(r"\s+", " ", chunk)
            if chunk:
                options[letter] = chunk
        return options

    def _parse_options(self, text: str) -> dict[str, str]:
        """Parse MCQ options: ưu tiên marker-based (một hoặc nhiều dòng), sau đó header-only."""
        by_markers = self._parse_options_by_markers(text)
        if sum(1 for v in by_markers.values() if v.strip()) >= 2:
            return by_markers

        options: dict[str, str] = {}
        lines = text.split("\n")
        option_positions: list[tuple[int, str]] = []
        for i, line in enumerate(lines):
            m = re.match(r"^\s*([A-D])(?:[\.)．:])\s*$", line)
            if m:
                option_positions.append((i, m.group(1)))

        if not option_positions:
            inline_pattern = re.compile(
                r"^\s*([A-D])(?:[\.)．:])\s*(.+)$", re.MULTILINE
            )
            for m in inline_pattern.finditer(text):
                letter = m.group(1)
                content = m.group(2).strip()
                if content:
                    options[letter] = content
            return options if options else by_markers

        for line_idx, letter in option_positions:
            content_lines: list[str] = []
            for i in range(line_idx + 1, len(lines)):
                line = lines[i]
                if re.match(r"^\s*[A-D](?:[\.)．:])\s*$", line):
                    break
                stripped = line.strip()
                if stripped:
                    content_lines.append(stripped)
            option_text = " ".join(content_lines)
            if option_text:
                options[letter] = option_text

        return options if sum(1 for v in options.values() if v.strip()) >= 2 else (
            options or by_markers
        )

    def _trim_orphan_after_last_option_line(self, raw: str) -> str:
        """
        Bỏ các dòng rác ngay sau đáp án D (mảnh công thức của câu sau do layout PDF).
        """
        lines = raw.split("\n")
        last_d = -1
        for i, line in enumerate(lines):
            if re.match(r"^\s*D\s*[\.)．:]\s+", line):
                last_d = i
        if last_d < 0 or last_d >= len(lines) - 1:
            return raw
        head = lines[: last_d + 1]
        rest = lines[last_d + 1 :]
        cut = 0
        for line in rest:
            s = line.strip()
            if not s:
                cut += 1
                continue
            if re.search(
                r"[àáảãạăằẳẵặâầẩẫậèéẻẽẹêềểễệìíỉĩịòóỏõọôồổốộơờởỡợùúủũụưừửữựỳýỷỹỵđĐ]",
                s,
            ):
                break
            if len(s) > 56:
                break
            cut += 1
        if cut == 0:
            return raw
        return "\n".join(head + rest[cut:]).strip()

    def _peel_stem_tail_into_hollow_options(
        self, stem: str, options: dict[str, str]
    ) -> tuple[str, dict[str, str]]:
        """
        PDF kiểu 'A. B. V =' khiến marker-based chỉ lấy được B/C/D rỗng ('V =').
        Hai dòng công thức ngay trước thường là 4 phương án (hai dạng + hai V=).
        """
        opt = dict(options)
        a_empty = opt.get("A") == "" or ("A" not in opt)
        b_s = (opt.get("B") or "").strip()
        c_s = (opt.get("C") or "").strip()
        d_s = (opt.get("D") or "").strip()
        short_v = re.compile(r"^V\s*=\s*$")
        if not a_empty and not (short_v.match(b_s) and short_v.match(c_s)):
            return stem, opt
        if not stem.strip():
            return stem, opt

        lines = stem.split("\n")
        strong_tail: list[str] = []
        i = len(lines) - 1
        while i >= 0:
            line = lines[i].strip()
            if not line:
                i -= 1
                continue
            if self._is_strong_embedded_choice_line(line):
                strong_tail.insert(0, line)
                i -= 1
            else:
                break

        if len(strong_tail) < 2:
            return stem, opt

        head = "\n".join(lines[: i + 1]).strip()
        L2 = strong_tail[-1]
        L1 = strong_tail[-2]
        if not re.search(r"V\s*=", L2, re.IGNORECASE):
            return stem, opt

        v_parts = [
            p.strip()
            for p in re.split(r"\s+(?=V\s*=)", L2)
            if p.strip() and re.match(r"^V\s*=", p.strip(), re.IGNORECASE)
        ]
        if len(v_parts) >= 2:
            opt["C"] = v_parts[0]
            opt["D"] = v_parts[1]
        elif len(v_parts) == 1 and not d_s.strip():
            opt["D"] = v_parts[0]

        bits = [b.strip() for b in re.split(r"\s+\.\s+", L1) if b.strip()]
        if len(bits) >= 3:
            opt["A"] = bits[0]
            opt["B"] = f"{bits[1]} . {bits[2]}"
        elif len(bits) == 2:
            opt["A"] = bits[0]
            opt["B"] = bits[1]
        elif len(bits) == 1 and a_empty:
            opt["B"] = bits[0]

        return head, opt

    def _is_strong_embedded_choice_line(self, line: str) -> bool:
        """Dòng đủ tín hiệu để coi là hàng đáp án (tránh '3 3' nhiễu)."""
        s = line.strip()
        if re.fullmatch(r"\d{1,4}\s+\d{1,4}", s):
            return False
        if re.search(r"V\s*=", s, re.IGNORECASE):
            return True
        if re.search(r"\d+\s+a\s+\d+", s, re.IGNORECASE):
            return True
        return False

    _VIET_CHARS_RE = re.compile(
        r"[a-zA-ZàáảãạăằẳẵặâầẩẫậèéẻẽẹêềểễệìíỉĩịòóỏõọôồổỗộơờởỡợùúủũụưừửữựỳýỷỹỵđĐ]"
    )

    _VIET_WORD_RE = re.compile(r"[À-ỹà-ỹ]")

    def _is_orphan_formula_fragment_line(self, s: str) -> bool:
        """
        Dòng chỉ là mảnh công thức (số, +, x/e đơn lẻ…) trước khối A/B/C/D.
        Không dùng _VIET_CHARS_RE (có cả a-z) vì sẽ khớp chữ x trong \"2 x 3\".
        """
        if not s or len(s) > 96:
            return False
        if self._VIET_WORD_RE.search(s):
            return False
        if re.search(r"[a-zA-Z]{3,}", s):
            return False
        if not re.search(r"\d", s):
            return False
        return True

    def _trim_same_line_equation_bleed(self, stem: str) -> str:
        """Cắt cụm chỉ-toán dính sau dấu chấm/hỏi trên cùng dòng với đề."""
        if not stem.strip():
            return stem
        tail_re = re.compile(
            r"([\.\?…])\s+([\d\s\+\-\(\)\u2212\u00d7\u00f7xe²³⁰¹⁴⁵⁶⁷⁸⁹\.=;:,∞]+\s*)$",
            re.IGNORECASE,
        )
        out = stem
        m = tail_re.search(out)
        while m:
            frag = m.group(2).strip()
            if self._VIET_CHARS_RE.search(frag):
                break
            if re.search(r"[a-zA-Z]{2,}", frag):
                break
            if not re.search(r"\d", frag):
                break
            out = out[: m.start() + 1].rstrip()
            m = tail_re.search(out)
        return out

    def _trim_stem_equation_junk(self, stem: str) -> str:
        """Bỏ các dòng rác (x, số, mảnh công thức) giữa đề và đáp án."""
        if not stem:
            return stem
        lines = stem.split("\n")
        kept: list[str] = []
        for line in lines:
            s = line.strip()
            if not s:
                continue
            if self._is_orphan_formula_fragment_line(s):
                continue
            if re.match(r"^x(\s+x){0,12}$", s, re.IGNORECASE):
                continue
            if len(s) <= 2 and not self._VIET_CHARS_RE.search(s):
                continue
            if len(s) <= 20 and not self._VIET_CHARS_RE.search(s):
                if re.fullmatch(r"[\d\s\.\,\(\)\+\−\-\=;:²³⁰¹⁴⁵⁶⁷⁸⁹]+", s):
                    continue
            if len(s) <= 48 and not self._VIET_CHARS_RE.search(s):
                if re.fullmatch(
                    r"[\d\s\.\,\(\)\+\−\-\=;:xe²³⁰¹⁴⁵⁶⁷⁸⁹]{3,}", s, re.IGNORECASE
                ):
                    continue
            kept.append(s)
        merged = "\n".join(kept).strip()
        return self._trim_same_line_equation_bleed(merged)

    def _extract_stem(self, text: str, options: dict[str, str]) -> str:
        """Phần đề tới trước marker đáp án đầu tiên (bỏ 'A.' treo khi đáp án thật bắt đầu từ B.)."""
        m = self._first_option_match(text)
        if m:
            return self._stem_before_first_option(text, m)
        lines = text.split("\n")
        result: list[str] = []
        for line in lines:
            if re.match(r"^\s*[A-D](?:[\.)．:])\s+\S", line):
                break
            s = line.strip()
            if re.match(r"^[A-D](?:[\.)．:])\s*$", s):
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
