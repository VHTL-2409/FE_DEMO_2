"""
Phát hiện ranh giới câu hỏi — nhiều pattern (VN/EN), không chỉ một định dạng mẫu.
"""

from __future__ import annotations

import re
from dataclasses import dataclass

from .math_normalizer import normalize_exam_text


@dataclass
class QuestionBoundary:
    number: int
    page: int
    start_char: int
    end_char: int
    header_match: str
    raw_block: str


# Thứ tự ưu tiên: cụ thể trước, số đứng một mình sau (heuristic chặt hơn)
_PATTERNS: list[tuple[str, re.Pattern[str]]] = [
    ('cau_bai', re.compile(r'^\s*(?:Câu|Bài|CAU|BAI)\s*(\d+)\s*[.:)\-–]?\s*', re.IGNORECASE)),
    ('question_en', re.compile(r'^\s*Question\s*(\d+)\s*[.:]\s*', re.IGNORECASE)),
    ('q_short', re.compile(r'^\s*Q\s*(\d+)\s*[.:)]\s*', re.IGNORECASE)),
    ('num_paren', re.compile(r'^\s*(\d+)\s*[.)]\s+(?=\S)')),
    ('num_point', re.compile(r'^\s*(\d+)\s*[.)]\s*\(\s*[\d.]+\s*[Pp]oint', re.IGNORECASE)),
]


def _line_starts_question(line: str) -> tuple[int, str, str] | None:
    s = line.strip()
    if not s:
        return None
    for kind, pat in _PATTERNS:
        m = pat.match(s)
        if not m:
            continue
        try:
            n = int(m.group(1))
        except (ValueError, IndexError):
            continue
        if kind == 'num_paren' and n > 200:
            continue
        return n, kind, m.group(0)
    return None


def detect_questions_from_lines(
    lines: list[str],
    line_pages: list[int] | None,
) -> list[QuestionBoundary]:
    """
    lines: danh sách dòng đã thứ tự đọc.
    line_pages: song song với lines nếu có (PDF); None nếu DOCX (page=1).
    """
    boundaries: list[QuestionBoundary] = []
    if not lines:
        return boundaries

    current_start = 0
    current_num = 0
    current_page = line_pages[0] if line_pages else 1
    current_header = ''
    blocks: list[str] = []

    def flush(end_idx: int, end_char: int) -> None:
        nonlocal current_start, current_num, current_page, current_header, blocks
        if current_num <= 0 or not blocks:
            return
        raw = '\n'.join(blocks)
        boundaries.append(
            QuestionBoundary(
                number=current_num,
                page=current_page,
                start_char=current_start,
                end_char=end_char,
                header_match=current_header,
                raw_block=raw,
            )
        )

    char_off = 0
    for i, line in enumerate(lines):
        page = line_pages[i] if line_pages and i < len(line_pages) else 1
        hit = _line_starts_question(line)
        if hit:
            n, _kind, hdr = hit
            if current_num > 0 and blocks:
                flush(i - 1, char_off)
            current_start = char_off
            current_num = n
            current_page = page
            current_header = hdr.strip()
            blocks = [line]
        else:
            if current_num > 0:
                blocks.append(line)
        char_off += len(line) + 1

    if current_num > 0 and blocks:
        flush(len(lines) - 1, char_off)

    return boundaries


def merge_overlapping_by_number(bounds: list[QuestionBoundary]) -> list[QuestionBoundary]:
    """Giữ bản mới nhất theo số câu nếu trùng (hiếm)."""
    by_num: dict[int, QuestionBoundary] = {}
    for b in sorted(bounds, key=lambda x: x.start_char):
        by_num[b.number] = b
    return sorted(by_num.values(), key=lambda x: x.number)
