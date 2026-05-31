"""
profiler.py — Build a structural profile of a PDF to guide parser selection.
"""

from __future__ import annotations

import re
from dataclasses import dataclass, field
from pathlib import Path
from typing import Optional

import fitz  # PyMuPDF

from .config import env_int
from .utils.ocr_utils import is_ocr_available, ocr_image_bytes_to_text


PROFILER_OCR_DPI = env_int("PDF_PROFILER_OCR_DPI", 180)


@dataclass
class PdfProfile:
    """Structural fingerprint of a PDF exam document."""
    file_path: str
    total_pages: int = 0
    """First ~4 pages of text for routing heuristics (THPT / pdf_mau_3 style)."""
    head_text_sample: str = ""
    language: str = "unknown"          # "vi", "en", "mixed"
    has_cau_pattern: bool = False
    has_question_pattern: bool = False
    has_answer_section: bool = False
    has_answer_grid: bool = False      # answer key at bottom in grid format
    has_solution_section: bool = False  # has detailed solution/explanation section
    has_essay_section: bool = False
    formula_noise_score: float = 0.0    # 0.0 (clean) - 1.0 (very noisy)
    image_block_count: int = 0
    total_text_chars: int = 0
    avg_chars_per_page: float = 0.0
    inline_option_rate: float = 0.0    # fraction of option lines on same line as stem
    likely_template: str = "unknown"

    # Confidence scores per template
    score_template_01: float = 0.0
    score_template_02: float = 0.0
    score_template_03: float = 0.0
    score_template_04: float = 0.0  # DOCX Vietnamese
    score_template_05: float = 0.0  # DOCX Database
    score_template_06: float = 0.0  # English exam (pdf_mau_2_new)


def build_pdf_profile(pdf_path: str) -> PdfProfile:
    """Analyze a PDF and return a structural profile."""
    profile = PdfProfile(file_path=pdf_path)

    try:
        doc = fitz.open(pdf_path)
        profile.total_pages = len(doc)
    except Exception:
        return profile

    sampled_pages = _sample_page_indices(len(doc))
    head_page_indices = list(range(min(4, len(doc))))

    # Sample first pages for THPT / answer-grid style detection (pdf_mau_3, etc.)
    head_chunks: list[str] = []
    for pi in head_page_indices:
        try:
            head_chunks.append(doc[pi].get_text())
        except Exception:
            pass
    profile.head_text_sample = "\n".join(head_chunks)[:24000]

    total_chars = 0
    question_count = 0
    cau_count = 0
    english_question_count = 0
    answer_section_count = 0
    grid_answer_count = 0
    solution_count = 0
    essay_count = 0
    image_blocks = 0
    merged_option_count = 0
    total_option_lines = 0

    # Vietnamese patterns
    VI_PATTERNS = [
        r"Câu\s*\d+",           # Câu 1, Câu 2
        r"Bài\s*\d+",           # Bài 1
        r"Phần\s*[IVX]+\.?",
        r"Phần\s+(câu\s*hỏi|đáp\s*án|lời\s*giải|tự\s*luận|trắc\s*nghiệm)",
        r"Đáp\s*án",            # section marker
        r"Hướng\s*dẫn",         # instructions
        r"Giải\s*chi\s*tiết",   # solution
    ]
    VI_RE = re.compile("|".join(VI_PATTERNS), re.IGNORECASE)

    # English patterns
    EN_PATTERNS = [
        r"Question\s*\d+[:\.]",   # Question 1:
        r"Answer\s*Key",
        r"Solution",
        r"Explanation",
        r"Mark\s+the\s+letter",
    ]
    EN_RE = re.compile("|".join(EN_PATTERNS), re.IGNORECASE)

    # Answer key patterns
    ANSWER_KEY_RE = re.compile(
        r"(?:\d{1,3}[\.\-–—:]\s*[A-Da-d](?:\s|$|[,;])){3,}"  # 1.C 2.B 3.D or 1-D 2-C
        r"|(?:\d{1,3}\s*[A-Da-d]){4,}"                           # 1C 2B 3C 4D
    )

    # Solution section patterns — match either "lời giải" or "giải chi tiết"
    # with flexible spacing (some PDFs merge words) and various terminators
    SOLUTION_RE_VI = re.compile(
        r"(?:^|\n)\s*(?:lời\s*giải|giải\s*chi\s*tiết|giải\s*chi\s*tiết)[,\s]*"
        r"(?:Chi\s*tiết)?[:\.\-]?\s*(?:\n|$|\S)",
        re.IGNORECASE
    )
    SOLUTION_RE_EN = re.compile(
        r"(?:^|\n)\s*(?:solution|explanation)[,\s]*[:\.\-]?\s*(?:\n|$|\S)",
        re.IGNORECASE
    )

    # Essay section
    ESSAY_RE_VI = re.compile(
        r"(?:^|\n)\s*(?:phần\s*ii|tự\s*luận|phần\s*tự\s*luận|essay)[,\s]*[:\.\-]?\s*",
        re.IGNORECASE
    )

    # Merged options: A. x² B. y² C. z² D. t² on same line
    MERGED_OPT_RE = re.compile(r"^[A-D]\.\s*\S.*\s+[A-D]\.\s*\S")

    # Superscript/subscript noise: single digit or symbol separated vertically
    SUPERSCRIPT_RE = re.compile(r"[\d²³¹⁰⁴⁵⁶⁷⁸⁹⁺⁻⁼ⁿ]")
    FORMULA_CHARS_RE = re.compile(r"[∫∑∏√∂∆∈∉⊂⊃∪∩ℝℤℕ→←↔≥≤≡≈±×÷·]")

    noise_char_count = 0
    total_char_count = 0

    for page_idx in sampled_pages:
        page = doc[page_idx]
        blocks = page.get_text("dict")["blocks"]
        page_char_count = 0
        page_text_fragments: list[str] = []

        for block in blocks:
            if block["type"] == 0:  # text block
                block_text = ""
                for line in block.get("lines", []):
                    for span in line.get("spans", []):
                        t = span["text"]
                        block_text += t + "\n"
                        page_char_count += len(t)

                        # Count noise characters
                        if SUPERSCRIPT_RE.search(t) or FORMULA_CHARS_RE.search(t):
                            noise_char_count += len(t)

                        # Count merged options
                        line_text = line.get("spans", [{}])[0].get("text", "")
                        if MERGED_OPT_RE.match(line_text.strip()):
                            merged_option_count += 1

                        # Count options (lines starting with A. B. C. D.)
                        if re.match(r"^\s*[A-D]\.\s*\S", t):
                            total_option_lines += 1

                if block_text.strip():
                    page_text_fragments.append(block_text)

                # Run detection on block text
                block_upper = block_text.upper()

                block_text_lower = block_text.lower()
                if VI_RE.search(block_text):
                    cau_count += 1
                if EN_RE.search(block_text):
                    english_question_count += 1
                if ANSWER_KEY_RE.search(block_text):
                    answer_section_count += 1
                    # Check if it's a grid format
                    if re.search(r"[\.\-–—:]\s*[A-D](?:\s|$)", block_text):
                        grid_answer_count += 1
                if SOLUTION_RE_VI.search(block_text) or SOLUTION_RE_EN.search(block_text):
                    solution_count += 1
                if ESSAY_RE_VI.search(block_text):
                    essay_count += 1

                question_count += len(re.findall(r"Câu\s*\d+", block_text))
                question_count += len(re.findall(r"Question\s*\d+", block_text, re.IGNORECASE))

            elif block["type"] == 1:  # image block
                image_blocks += 1

        if page_char_count < 40 and is_ocr_available():
            try:
                dpi = PROFILER_OCR_DPI
                scale = dpi / 72.0
                pix = page.get_pixmap(matrix=fitz.Matrix(scale, scale), alpha=False)
                ocr_text = ocr_image_bytes_to_text(
                    pix.tobytes("png"),
                    lang="vie+eng",
                    config="--psm 6",
                )
            except Exception:
                ocr_text = ""

            if ocr_text.strip():
                page_text_fragments.append(ocr_text)
                page_char_count = max(page_char_count, len(ocr_text))
                if VI_RE.search(ocr_text):
                    cau_count += 1
                if EN_RE.search(ocr_text):
                    english_question_count += 1
                if ANSWER_KEY_RE.search(ocr_text):
                    answer_section_count += 1
                    if re.search(r"[\.\-–—:]\s*[A-D](?:\s|$)", ocr_text):
                        grid_answer_count += 1
                if SOLUTION_RE_VI.search(ocr_text) or SOLUTION_RE_EN.search(ocr_text):
                    solution_count += 1
                if ESSAY_RE_VI.search(ocr_text):
                    essay_count += 1
                question_count += len(re.findall(r"Câu\s*\d+", ocr_text))
                question_count += len(re.findall(r"Question\s*\d+", ocr_text, re.IGNORECASE))

        total_char_count += page_char_count
        total_chars += page_char_count

    doc.close()

    profile.total_text_chars = total_chars
    profile.avg_chars_per_page = total_chars / max(len(sampled_pages), 1)
    profile.has_cau_pattern = cau_count > 0
    profile.has_question_pattern = english_question_count > 0
    profile.has_answer_section = answer_section_count > 0
    profile.has_answer_grid = grid_answer_count > 0
    profile.has_solution_section = solution_count > 0
    profile.has_essay_section = essay_count > 0
    profile.image_block_count = image_blocks

    # Formula noise score: ratio of formula/noise chars to total chars
    profile.formula_noise_score = noise_char_count / max(total_chars, 1)

    # Inline option rate
    profile.inline_option_rate = merged_option_count / max(total_option_lines, 1)

    # Detect language
    if cau_count > english_question_count * 2:
        profile.language = "vi"
    elif english_question_count > cau_count * 2:
        profile.language = "en"
    else:
        profile.language = "mixed"

    # Compute template scores
    profile.score_template_01 = _score_template_01(profile)
    profile.score_template_02 = _score_template_02(profile)
    profile.score_template_03 = _score_template_03(profile)
    profile.score_template_04 = _score_template_04(profile)
    profile.score_template_05 = _score_template_05(profile)
    profile.score_template_06 = _score_template_06(profile)

    # Select most likely
    scores = [
        ("template_01_math_broken", profile.score_template_01),
        ("template_02_clean_mcq", profile.score_template_02),
        ("template_03_math_answer_grid", profile.score_template_03),
    ]
    profile.likely_template = max(scores, key=lambda x: x[1])[0]

    return profile


def _sample_page_indices(total_pages: int) -> list[int]:
    """
    Sample a few representative pages instead of scanning the whole PDF.
    This keeps routing fast even for long exams while still catching
    answer-grid / solution sections that often live near the end.
    """
    if total_pages <= 0:
        return []

    if total_pages <= 6:
        return list(range(total_pages))

    candidates = {
        0,
        1,
        2,
        max(total_pages // 2, 0),
        max(total_pages - 3, 0),
        max(total_pages - 2, 0),
        total_pages - 1,
    }
    return sorted(idx for idx in candidates if 0 <= idx < total_pages)


def _thpt_math_grid_exam_heuristic(head: str) -> bool:
    """
    Đề Toán THPT / Sở GD kiểu pdf_mau_3: nhiều câu 'Câu N:', có mô tả đáp án/lời giải,
    thường có '50 câu trắc nghiệm' hoặc 'THPT QG' trong phần đầu.
    PDF này không nên dùng template_01 (tách span từng ký tự).
    """
    if not head or len(head) < 80:
        return False
    h = head
    low = h.lower()
    cau_colon_hits = len(re.findall(r"Câu\s+\d+\s*:", h, flags=re.IGNORECASE))
    if cau_colon_hits >= 2 and re.search(
        r"THPT\s*QG|50\s*câu\s*trắc\s*nghiệm|ĐỀ\s*THI\s*THỬ|ĐỀ\s*THI\s*THPT",
        h,
        re.IGNORECASE,
    ):
        return True
    if "sở gd" in low or "sở giáo dục" in low:
        if cau_colon_hits >= 2 and "môn" in low and (
            "toán" in low or "trắc nghiệm" in low
        ):
            return True
    if "phần đáp án" in low and "câu hỏi" in low and cau_colon_hits >= 2:
        return True
    return False


def _thpt_math_grid_exam_heuristic_from_profile(profile: PdfProfile) -> bool:
    """
    Variant using profile-level signals (computed from full document scan).
    Returns True when the combination of four signals is strong enough to
    identify a THPT-style exam even if the heuristic on head_text_sample alone
    missed the markers.
    """
    if not profile.has_answer_grid or not profile.has_solution_section:
        return False
    cau_hits = len(re.findall(r"Câu\s+\d+\s*:", profile.head_text_sample, re.IGNORECASE))
    if profile.has_cau_pattern and cau_hits >= 2 and profile.total_pages >= 8:
        return True
    return False


def _score_template_01(profile: PdfProfile) -> float:
    """Score for Template 01: math broken text (pdf_mau_1 style)."""
    score = 0.0

    if profile.has_cau_pattern:
        score += 0.4
    if profile.language in ("vi", "mixed"):
        score += 0.2
    if profile.formula_noise_score > 0.05:
        score += 0.2 * profile.formula_noise_score * 5  # up to 0.2
    if profile.has_essay_section:
        score += 0.1
    if not profile.has_solution_section:
        score += 0.1
    if profile.total_pages <= 5:
        score += 0.1  # short exam

    # pdf_mau_3 / THPT đề dài: layout span-based parser 01 làm vỡ công thức từng ký tự
    if _thpt_math_grid_exam_heuristic(profile.head_text_sample):
        score -= 0.42

    if _thpt_math_grid_exam_heuristic_from_profile(profile):
        score -= 0.3

    return max(0.0, min(score, 1.0))


def _score_template_02(profile: PdfProfile) -> float:
    """Score for Template 02: clean MCQ English (pdf_mau_2 style)."""
    score = 0.0

    if profile.has_question_pattern:
        score += 0.5  # English "Question N:" pattern — strongest signal
    if profile.language == "en":
        score += 0.25
    elif profile.language == "mixed":
        score += 0.1
    if profile.formula_noise_score < 0.05:
        score += 0.15
    if not profile.has_solution_section:
        score += 0.1
    if profile.avg_chars_per_page > 300:
        score += 0.1  # dense text = clean

    return min(score, 1.0)


def _score_template_03(profile: PdfProfile) -> float:
    """Score for Template 03: math with answer grid + solutions (pdf_mau_3 style)."""
    score = 0.0

    if profile.has_cau_pattern:
        score += 0.2
    if profile.language in ("vi", "mixed"):
        score += 0.15
    if profile.has_answer_grid:
        score += 0.35  # answer grid — key signal for template 03
    if profile.has_solution_section:
        score += 0.25  # solution section
    if profile.total_pages >= 10:
        score += 0.05  # long exam

    # Extra bonus if BOTH answer_grid AND solution_section are present
    if profile.has_answer_grid and profile.has_solution_section:
        score += 0.1

    # Compound signal: long exam + answer grid + solution + Vietnamese Câu pattern
    # These four signals together are strong evidence of a THPT-style exam (pdf_mau_3).
    compound = (
        profile.has_answer_grid
        and profile.has_solution_section
        and profile.has_cau_pattern
        and profile.total_pages >= 8
    )
    if compound:
        score += 0.25

    # Đề kiểu pdf_mau_3: phần đáp án có thể nằm cuối file (head sample không bắt được grid)
    if _thpt_math_grid_exam_heuristic(profile.head_text_sample):
        score += 0.55

    # Profile-based fallback: long exam + all four signals detected from full scan
    if _thpt_math_grid_exam_heuristic_from_profile(profile):
        score += 0.4

    return min(score, 1.0)


def _score_template_04(profile: PdfProfile) -> float:
    """
    Score for Template 04: DOCX Vietnamese E-Commerce exam.
    DOCX files are auto-detected by extension in can_handle(), so this
    scoring function only handles PDF-based profiles (returns 0.0).
    """
    return 0.0


def _score_template_05(profile: PdfProfile) -> float:
    """
    Score for Template 05: DOCX Database exam.
    DOCX files are auto-detected by extension in can_handle(), so this
    scoring function only handles PDF-based profiles (returns 0.0).
    """
    return 0.0


def _score_template_06(profile: PdfProfile) -> float:
    """
    Score for Template 06: English THPT exam (pdf_mau_2_new style).
    Characteristics:
      - Vietnamese header with English content
      - "Question N:" format (English)
      - Answer key at end: "1-D 2-C 3-A" format
      - 50 MCQ questions
      - Clean text, no formulas
    """
    score = 0.0

    # Strong signal: has Question N: pattern
    if profile.has_question_pattern:
        score += 0.4

    # English language detected
    if profile.language == "en":
        score += 0.15

    # Vietnamese THPT exam header present (mixed language is typical)
    if profile.language == "mixed":
        score += 0.1

    # Clean text (no formula noise)
    if profile.formula_noise_score < 0.02:
        score += 0.15
    elif profile.formula_noise_score < 0.05:
        score += 0.1

    # Has answer section at end
    if profile.has_answer_section:
        score += 0.1

    # No essay section (pure MCQ)
    if not profile.has_essay_section:
        score += 0.1

    # Vietnamese exam patterns in content (typical for THPT exams with mixed content)
    score += 0.15  # Base score for this specific exam type

    return min(score, 1.0)
