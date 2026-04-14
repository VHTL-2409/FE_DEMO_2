"""
mcq_option_markers.py — Marker-based MCQ option extraction (A./B./C./D.).

Shared by template_01 (math PDF primary path) and template_03 to avoid drift.
Ported from template_03_math_answer_grid.py with the same matching semantics.
"""

from __future__ import annotations

import re
from typing import Optional


def expand_glued_mcq_lines(text: str) -> str:
    """
    Chèn newline trước B./C./D. khi PDF/Word dính cột trên một dòng.

    Bao gồm:
      - Sau chữ số: ...3B.  (pdf_mau_1)
      - Sau ngoặc đóng: ...)B.  (pdf_mau_3, công thức y=...B.)
      - Kiểu C.S dính D. (một số đề toán)
    """
    if not text:
        return text
    out = text
    for _ in range(14):
        prev = out
        out = re.sub(r"(?<=\))(?=[B-D]\.)", "\n", out)
        out = re.sub(r"(?<=\])(?=[B-D]\.)", "\n", out)
        out = re.sub(r"(?<=[0-9])(?=[B-D]\.)", "\n", out)
        if prev == out:
            break
    out = re.sub(r"(?<=\.S)(?=D\.)", "\n", out)
    # WordEquation / PUA: '….B. y=' — chỉ trên dòng đã có 'A.' (tránh '1.B' nhầm)
    lines = out.split("\n")
    out_lines: list[str] = []
    for line in lines:
        if re.search(r"A\s*[\.)．:]", line):
            line = re.sub(r"(?<=\.)(?=[B-D]\.)", "\n", line)
        out_lines.append(line)
    return "\n".join(out_lines)


def collect_option_markers(text: str) -> list[re.Match[str]]:
    """
    Mọi vị trí A./B./C./D. hợp lệ: đầu dòng, hoặc sau khoảng trắng nhưng không sau chữ
    (tránh khớp D trong 'ABCD .').
    """
    found: list[re.Match[str]] = []
    for m in re.finditer(
        r"(?:^|\n)\s*([A-D])\s*[\.)．:]\s+\S", text, re.MULTILINE
    ):
        found.append(m)
    for m in re.finditer(
        r"(?:^|\n)\s*([A-D])\s*[\.)．:]\s*$", text, re.MULTILINE
    ):
        found.append(m)
    for m in re.finditer(
        r"(?:^|\n)\s*([A-D])\s*[\.)．:]\s+", text, re.MULTILINE
    ):
        found.append(m)
    for m in re.finditer(r"(?<=\s)([A-D])\s*[\.)．:]\s+(?=\S)", text):
        if m.start() > 0 and text[m.start() - 1].isalpha():
            continue
        found.append(m)
    # Kiểu đề in / Word→PDF: "A.S = …" / "B.x =" (không có khoảng sau dấu chấm)
    for m in re.finditer(
        r"(?:^|\n)\s*([A-D])\s*[\.)．:](?=\S)",
        text,
        re.MULTILINE,
    ):
        found.append(m)
    found.sort(key=lambda x: x.start())
    out: list[re.Match[str]] = []
    for m in found:
        if not out or m.start() != out[-1].start():
            out.append(m)
    return out


def first_option_match(text: str) -> Optional[re.Match[str]]:
    """
    Đáp án đầu tiên có nội dung (bỏ 'A. B.' rỗng).
    """
    for m in collect_option_markers(text):
        tail = text[m.end(): m.end() + 14].lstrip()
        if re.match(r"^[A-D]\s*[\.)．:]", tail):
            continue
        return m
    return None


def stem_before_first_option(text: str, m: re.Match[str]) -> str:
    """Phần đề; bỏ 'A.' treo nếu đáp án đầu là B. (kiểu A. B. V = …)."""
    stem = text[: m.start()]
    letter = m.group(1)
    if letter != "A":
        stem = re.sub(r"A\s*[\.)．:]\s*$", "", stem.rstrip())
    return stem.strip()


def parse_options_by_markers(text: str) -> dict[str, str]:
    """
    Tách A. … B. … trên một hoặc nhiều dòng (sau khi layout-aware đã gom hàng).
    """
    options: dict[str, str] = {}
    m0 = first_option_match(text)
    if not m0:
        return options
    rest = text[m0.start():]
    matches = collect_option_markers(rest)
    if len(matches) < 2:
        return options
    for i, m in enumerate(matches):
        letter = m.group(1)
        a = m.end()
        b = matches[i + 1].start() if i + 1 < len(matches) else len(rest)
        chunk = rest[a:b].strip()
        chunk = re.sub(r"\s+", " ", chunk)
        if chunk:
            options[letter] = chunk
    return options
