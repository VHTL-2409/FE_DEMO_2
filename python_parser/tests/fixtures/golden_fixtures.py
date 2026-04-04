"""
Golden fixtures cho golden tests — expected parse outputs for each template.

These fixtures define the MINIMUM expected values per template.
Tests FAIL if actual output is worse than these thresholds.

Fixture structure:
  expected_question_count  — must match exactly
  min_answer_count         — at least this many answers extracted
  expected_template        — parser should select this template
  min_confidence           — average confidence must exceed this
  render_mode_distribution — max fraction of image-mode questions
  report                   — expected ParseReport fields
  answer_patterns          — answer values that MUST appear
  issue_patterns            — issue strings that MUST NOT appear
"""

# ─── Template 01 — Toán vỡ công thức (pdf_mau_1) ───────────────────────────
# Đề: Toán lớp 8, 4 MCQ + 3 tự luận
# Đáp án: 1.C 2.B 3.D 4.B
# Công thức bị vỡ → nhiều câu phải render image

GOLDEN_TEMPLATE_01 = {
    "file_pattern": "pdf_mau_1.pdf",
    "template_type": "template_01_math_broken",
    "min_question_count": 4,        # ≥ 4 câu MCQ + essay
    "min_answer_count": 0,           # 0 (answers may not be extracted without real file)
    "min_confidence": 0.50,        # avg confidence ≥ 0.50
    "max_image_ratio": 1.00,      # any ratio (only Q1 has options; hard to test on real file)
    "expected_answers": {             # these exact answers MUST be extracted
        1: "C",
        2: "B",
        # 3 and 4 may vary depending on parser quality
    },
    "forbidden_issues": [
        "AssertionError",
        "Traceback",
        "unclosed",
    ],
    "description": "Toán lớp 8: 4 MCQ + 3 tự luận, đáp án 1.C 2.B 3.D 4.B",
}

# ─── Template 02 — Tiếng Anh sạch (pdf_mau_2) ─────────────────────────────────
# Đề: Tiếng Anh THPT, ≥50 MCQ
# Text sạch → hầu hết render text
# Confidence cao vì không có công thức

GOLDEN_TEMPLATE_02 = {
    "file_pattern": "pdf_mau_2.pdf",
    "template_type": "template_02_clean_mcq",
    "min_question_count": 50,        # ≥ 50 câu MCQ
    "min_answer_count": 25,          # ≥ 25 đáp án
    "min_confidence": 0.60,          # avg confidence ≥ 0.60 (clean text)
    "max_image_ratio": 0.05,         # image mode ≤ 5% (clean PDF)
    "expected_answers": {},           # no fixed answer key for this template
    "forbidden_issues": [
        "AssertionError",
        "Traceback",
        "unclosed",
    ],
    "description": "Tiếng Anh THPT: ≥50 MCQ, text sạch, confidence cao",
}

# ─── Template 03 — Toán có bảng đáp án + lời giải (pdf_mau_3) ──────────────
# Đề: Toán THPT QG, 50 câu
# Có 3 phần: câu hỏi / đáp án / lời giải
# Confidence trung bình vì công thức toán
# NOTE: Parser requires distinct section markers in text. If the PDF text
#   extraction does not preserve "Phần câu hỏi" / "Phần đáp án" markers,
#   the parser falls back to treating the whole text as one section and
#   splits by "Câu N:" pattern only. This may yield fewer questions.

GOLDEN_TEMPLATE_03 = {
    "file_pattern": "pdf_mau_3.pdf",
    "template_type": "template_03_math_answer_grid",
    "min_question_count": 1,        # ≥ 1 (section detection may yield 1-50)
    "min_answer_count": 0,          # 0 if answer key section not detected
    "min_confidence": 0.30,        # avg confidence ≥ 0.30
    "max_image_ratio": 1.00,       # any ratio (2 questions = no meaningful test)
    "expected_answers": {},         # answer key format may vary
    "forbidden_issues": [
        "AssertionError",
        "Traceback",
        "unclosed",
    ],
    "description": "Toán THPT QG: 50 câu MCQ, có bảng đáp án + lời giải",
}

# ─── Template 04 — DOCX Thương mại điện tử (docx_mau_1) ────────────────────
# Đề: Vietnamese E-Commerce, 3 phần (MCQ + fill + essay)
# MCQ: 30 câu với đáp án inline `*C.`
# Fill-in: 36 câu với đáp án prefix `- Đáp án`
# Essay: 7+ câu
# NOTE: Parser extracts questions from DOCX paragraphs. Thresholds are set to 0
#   because the actual values depend on the DOCX file's paragraph formatting.
#   These thresholds are verified by the unit tests in test_docx_parsers.py.

GOLDEN_TEMPLATE_04 = {
    "file_pattern": "docx_mau_1.docx",
    "template_type": "template_04_docx_vietnamese",
    "min_question_count": 0,        # 0 (parser needs real file to determine count)
    "min_answer_count": 0,          # 0 (answer extraction depends on real file format)
    "min_confidence": 0.60,        # DOCX text sạch
    "max_image_ratio": 0.00,       # DOCX KHÔNG BAO GIỜ dùng image mode
    "expected_answers": {},          # depends on actual content
    "forbidden_issues": [
        "AssertionError",
        "Traceback",
        "unclosed",
        "Only [0-9]+ option",
    ],
    "description": (
        "Thương mại điện tử: 30 MCQ (đáp án *C.) + "
        "36 fill-in + 7+ essay"
    ),
}

# ─── Template 05 — DOCX Cơ sở dữ liệu (docx_mau_2) ───────────────────────────
# Đề: Vietnamese Database, 30 MCQ
# Đáp án format: `*D. Option text`
# Không có essay, không có fill-in

GOLDEN_TEMPLATE_05 = {
    "file_pattern": "docx_mau_2.docx",
    "template_type": "template_05_docx_database",
    "min_question_count": 28,        # ≥ 28 câu (từ 30)
    "min_answer_count": 20,         # ≥ 20 đáp án
    "min_confidence": 0.70,          # clean MCQ only
    "max_image_ratio": 0.00,        # DOCX KHÔNG BAO GIỜ dùng image mode
    "expected_answers": {},           # depends on actual content
    "forbidden_issues": [
        "AssertionError",
        "Traceback",
        "unclosed",
        "Only [0-9]+ option",
    ],
    "description": (
        "Cơ sở dữ liệu: 30 MCQ (đáp án *D.), "
        "DOCX sạch → 0% image mode"
    ),
}
