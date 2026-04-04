"""
profiler.py — Build a structural profile of a PDF to guide parser selection.
"""

from __future__ import annotations

import re
from dataclasses import dataclass, field
from pathlib import Path
from typing import Optional

import fitz  # PyMuPDF


@dataclass
class PdfProfile:
    """Structural fingerprint of a PDF exam document."""
    file_path: str
    total_pages: int = 0
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


def build_pdf_profile(pdf_path: str) -> PdfProfile:
    """Analyze a PDF and return a structural profile."""
    profile = PdfProfile(file_path=pdf_path)

    try:
        doc = fitz.open(pdf_path)
        profile.total_pages = len(doc)
    except Exception:
        return profile

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

    for page_num, page in enumerate(doc, 1):
        blocks = page.get_text("dict")["blocks"]

        for block in blocks:
            if block["type"] == 0:  # text block
                block_text = ""
                for line in block.get("lines", []):
                    for span in line.get("spans", []):
                        t = span["text"]
                        block_text += t + "\n"
                        total_char_count += len(t)

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

        total_chars += total_char_count

    doc.close()

    profile.total_text_chars = total_chars
    profile.avg_chars_per_page = total_chars / max(profile.total_pages, 1)
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

    # Select most likely
    scores = [
        ("template_01_math_broken", profile.score_template_01),
        ("template_02_clean_mcq", profile.score_template_02),
        ("template_03_math_answer_grid", profile.score_template_03),
    ]
    profile.likely_template = max(scores, key=lambda x: x[1])[0]

    return profile


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

    return min(score, 1.0)


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
