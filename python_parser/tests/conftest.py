"""
conftest.py — Shared pytest configuration and fixtures for template parser tests.
"""

from __future__ import annotations

import sys
from pathlib import Path
from unittest.mock import MagicMock, patch

import pytest

# Ensure app package is on path
sys.path.insert(0, str(Path(__file__).parent.parent))


def pytest_configure(config):
    """Register custom markers."""
    config.addinivalue_line(
        "markers",
        "integration: marks tests that require real PDF files "
        "(deselect with '-m \"not integration\"')"
    )


# ─── Fixtures ───────────────────────────────────────────────────────────────────

@pytest.fixture
def sample_vietnamese_mcq_text():
    """Sample Vietnamese MCQ text for testing."""
    return """
Phần câu hỏi
Câu 1: Tính giá trị biểu thức 2 + 2
A. 3
B. 4
C. 5
D. 6

Phần đáp án
1.B 2.A 3.C 4.D
"""


@pytest.fixture
def sample_english_mcq_text():
    """Sample English MCQ text for testing."""
    return """
Question 1: What is the capital of France?
A. London
B. Paris
C. Berlin
D. Madrid

Question 2: What is 2 + 2?
A. 3
B. 4
C. 5
D. 6
"""


@pytest.fixture
def sample_template_03_text():
    """Sample template 03 (math answer grid) text."""
    return """
PHẦN CÂU HỎI
Câu 1: Giá trị của biểu thức
A. 1
B. 2
C. 3
D. 4

Câu 2: Nghiệm của phương trình
A. x = 1
B. x = 2
C. x = 3
D. x = 4

PHẦN ĐÁP ÁN
1.B 2.A 3.C 4.D

PHẦN LỜI GIẢI
Câu 1. Ta có: 1 + 1 = 2 → Đáp án B.
Câu 2. Giải phương trình: x + 1 = 3 → x = 2 → Đáp án A.
"""


@pytest.fixture
def mock_pdf_reader():
    """
    Mock pdf_reader module to avoid real PDF file dependency in unit tests.
    Returns a dict mapping page_num -> list of TextBlock-like dicts.
    """
    sample_blocks = {
        1: [
            {
                "type": "text",
                "bbox": (50, 100, 550, 300),
                "text": "Câu 1: Tính giá trị\nA. 1\nB. 2\nC. 3\nD. 4",
                "y0": 100.0,
                "y1": 300.0,
            },
            {
                "type": "text",
                "bbox": (50, 310, 550, 450),
                "text": "Câu 2: Nghiệm của phương trình\nA. x=1\nB. x=2\nC. x=3\nD. x=4",
                "y0": 310.0,
                "y1": 450.0,
            },
        ],
        2: [
            {
                "type": "text",
                "bbox": (50, 100, 550, 250),
                "text": "Đáp án: 1.B 2.A 3.C 4.D",
                "y0": 100.0,
                "y1": 250.0,
            },
        ],
    }

    with patch("app.utils.pdf_reader.PdfReader") as MockPdfReader:
        instance = MagicMock()
        instance.get_text_blocks.side_effect = lambda page: sample_blocks.get(page, [])
        instance.get_all_text.return_value = "\n\n".join(
            " ".join(b["text"] for b in blocks)
            for blocks in sample_blocks.values()
        )
        instance.get_page_size.return_value = (595, 842)  # A4
        instance.page_count = len(sample_blocks)
        MockPdfReader.return_value = instance
        yield instance


@pytest.fixture
def mock_image_cropper():
    """Mock image_cropper to avoid file-system dependency."""
    with patch("app.utils.image_cropper.crop_question") as mock_crop:
        mock_crop.return_value = "/tmp/test_crop_1.png"
        yield mock_crop
