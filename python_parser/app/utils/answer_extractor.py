"""
answer_extractor.py — Extract answer keys from parsed exam text.
Supports multiple formats found in Vietnamese and English exam papers.
"""

from __future__ import annotations

import re
from dataclasses import dataclass
from typing import Optional


@dataclass
class AnswerEntry:
    """A single answer extracted from the key."""
    number: int
    answer: str  # "A", "B", "C", "D"
    confidence: float = 1.0
    format_used: str = ""


class AnswerExtractor:
    """
    Extract answer keys from exam text supporting multiple formats.

    Supported formats:
      1. "1.C 2.B 3.D"          — space-separated (dot after number, space before letter)
      2. "1-D 2-C 3-B"          — dash-separated
      3. "1:D 2:C 3:B"          — colon-separated
      4. "1.D 2.C 3.B"          — dot-after-number, no space ← pdf_mau_3 style
      5. "1D 2C 3D"             — compact space (number + letter, space separated)
      6. "1C2C3C4D"             — truly compact (no separators at all)
      7. "Chọn B." / "→ Chọn đáp án A."  — Vietnamese per-question inline
      8. "Đáp án B"             — Vietnamese dapan standalone
      9. "Câu 1: A"             — câu format
    """

    # Priority order: most specific patterns first
    FORMATS = [
        # Format 1: "1.C 2.B" or "1.C, 2.B" or "1.C - 2.B"
        {
            "name": "spaced_dot",
            "pattern": re.compile(
                r"(?<![A-Za-z])(\d{1,3})\.\s*([A-Da-d])\b"
                r"(?:\s*[,;]\s*|\s+|(?=[A-Z]?\d)|$)",
                re.MULTILINE
            ),
        },
        # Format 2: "1-D 2-C" or "1 – D 2 – C" (en-dash)
        {
            "name": "dash_separated",
            "pattern": re.compile(
                r"(?<![A-Za-z])(\d{1,3})\s*[-–—]\s*([A-Da-d])\b"
                r"(?:\s+|(?=[A-Z]?\d)|$)",
                re.MULTILINE
            ),
        },
        # Format 3: "1:D 2:C" (colon separator)
        {
            "name": "colon_separated",
            "pattern": re.compile(
                r"(?<![A-Za-z])(\d{1,3})\s*:\s*([A-Da-d])\b",
                re.MULTILINE
            ),
        },
        # Format 4: "1.D 2.C 3.B" (dot immediately after number, no space) ← KEY for pdf_mau_3
        {
            "name": "dot_after_number",
            "pattern": re.compile(
                r"(?<!\d)(\d{1,3})\.\s*([A-Da-d])(?=\s|[,;]|$)",
                re.MULTILINE
            ),
        },
        # Format 5: "1D 2C 3D" (number then letter, space-separated OR tightly adjacent)
        {
            "name": "compact_pairs",
            "pattern": re.compile(
                r"(?<![A-Za-z\d])(\d{1,3})\s*([A-Da-d])\b"
                r"(?:\s+|(?=\d)|$)",
                re.MULTILINE
            ),
        },
        # Format 6: "1C2C3C4D" (truly compact — no separators at all)
        {
            "name": "compact_continuous",
            "pattern": re.compile(
                r"(?<![A-Za-z\d])(\d{1,3})([A-Da-d])(?=[A-Da-d\d]|$)",
            ),
        },
        # Format 7: Vietnamese per-question inline answers
        # "→ Chọn đáp án B." / "Chọn đáp án A." / "→ Chọn B."
        # These appear at the end of each question's solution
        {
            "name": "vietnamese_inline",
            "pattern": re.compile(
                r"(?i)(?:→\s*)?Chọn\s*(?:đáp\s*án\s*)?([A-Da-d])\s*[\.\)]",
            ),
        },
        # Format 8: "Đáp án B" standalone (found in solutions)
        {
            "name": "vietnamese_dapan",
            "pattern": re.compile(
                r"(?i)đáp\s*án\s*([A-Da-d])\b",
            ),
        },
        # Format 9: "Câu 1: A" / "Câu 1. A"
        {
            "name": "cau_format",
            "pattern": re.compile(
                r"Câu\s*(\d+)[\.:]\s*([A-Da-d])\b",
                re.IGNORECASE
            ),
        },
    ]

    # Markers that indicate the start of the answer section.
    # Used as fallback when the tail scan doesn't find enough answers.
    ANSWER_MARKERS = [
        "BẢNG ĐÁP ÁN",
        "ĐÁP ÁN",
        "ĐÁP ÁN VÀ GIẢI THÍCH",
        "ĐÁP ÁN VÀ HƯỚNG DẪN",
        "HƯỚNG DẪN GIẢI",
        "GIẢI CHI TIẾT",
        "ANSWER KEY",
        "ANSWERS",
        "ĐÁP ÁN BÀI",
        "ĐÁP ÁN CÁC CÂU",
        "ĐÁP ÁN CÂU HỎI",
    ]

    def __init__(self, text: str, tail_lines: int = 4000):
        self.text = text
        self.tail_lines = tail_lines
        self._answers: Optional[list[AnswerEntry]] = None

    def extract(self) -> list[AnswerEntry]:
        """Extract all answer entries from text.

        Strategy:
          1. Try in tail (last N lines).
          2. If < 5 answers found, search backward from ANSWER_MARKER sections.
          3. Try Vietnamese inline per-question format from full text.
        """
        if self._answers is not None:
            return self._answers

        answers: list[AnswerEntry] = []

        # ── Step 1: scan tail ─────────────────────────────────────────────────
        lines = self.text.split("\n")
        tail_text = "\n".join(lines[-self.tail_lines:]) if len(lines) > self.tail_lines else self.text

        for fmt in self.FORMATS:
            entries = self._extract_format(tail_text, fmt["name"], fmt["pattern"])
            if entries and len(entries) >= 2:
                answers = entries
                break

        # Also try câu format (in case it was missed above)
        if not answers:
            answers = self._extract_cau_format(tail_text)

        # ── Step 2: fallback to ANSWER_MARKER sections ───────────────────────
        if not answers or len(answers) < 5:
            marker_answers = self._extract_from_answer_markers()
            if marker_answers and len(marker_answers) > len(answers):
                answers = marker_answers

        # ── Step 3: try Vietnamese inline per-question from full text ───────
        if not answers or len(answers) < 2:
            inline_answers = self._extract_vietnamese_inline()
            if inline_answers and len(inline_answers) >= len(answers):
                answers = inline_answers

        # Normalize letter case
        for entry in answers:
            entry.answer = entry.answer.upper()

        self._answers = answers
        return answers

    def _extract_from_answer_markers(self) -> list[AnswerEntry]:
        """
        Search backward from each ANSWER_MARKER occurrence and extract
        answers from that section.
        """
        best: list[AnswerEntry] = []
        text_lower = self.text.lower()

        for marker in self.ANSWER_MARKERS:
            pos = text_lower.rfind(marker.lower())
            if pos < 0:
                continue
            # Extract from marker to end of file
            section = self.text[pos:]
            # Try each format in priority order
            for fmt in self.FORMATS:
                entries = self._extract_format(section, fmt["name"], fmt["pattern"])
                if entries and len(entries) >= len(best):
                    best = entries
            # Stop at first non-empty result with >= 5 entries
            if len(best) >= 5:
                break

        return best

    def _extract_vietnamese_inline(self) -> list[AnswerEntry]:
        """
        Extract Vietnamese inline answers: 'Chọn B.', '→ Chọn đáp án A.'
        These appear in per-question format (no sequential numbering).
        Returns entries without question numbers — use carefully.
        """
        entries: list[AnswerEntry] = []
        pat = re.compile(
            r"(?i)(?:→\s*)?Chọn\s*(?:đáp\s*án\s*)?([A-Da-d])\s*[\.\)]",
        )
        for m in pat.finditer(self.text):
            ans = m.group(1).upper()
            entries.append(AnswerEntry(
                number=0,  # no question number
                answer=ans,
                confidence=0.6,
                format_used="vietnamese_inline",
            ))
        return entries

    def _extract_format(
        self,
        text: str,
        name: str,
        pattern: re.Pattern,
    ) -> list[AnswerEntry]:
        """Apply a specific format pattern to extract answers."""
        entries: list[AnswerEntry] = []
        try:
            for m in pattern.finditer(text):
                if name in ("vietnamese_inline", "vietnamese_dapan"):
                    num = 0
                    ans = m.group(1)
                else:
                    num_str = m.group(1)
                    ans = m.group(2)
                    try:
                        num = int(num_str)
                    except ValueError:
                        continue
                entries.append(AnswerEntry(
                    number=num,
                    answer=ans,
                    confidence=1.0,
                    format_used=name,
                ))
        except Exception:
            pass
        return entries

    def _extract_cau_format(self, text: str) -> list[AnswerEntry]:
        """Extract 'Câu N: A' or 'Câu N. A' format."""
        pattern = re.compile(
            r"Câu\s*(\d+)[\.:]\s*([A-Da-d])\b",
            re.IGNORECASE
        )
        return self._extract_format(text, "cau_format", pattern)

    def get_as_dict(self) -> dict[int, str]:
        """Return answers as {number: letter} dict."""
        return {e.number: e.answer for e in self.extract()}

    def get(self, question_num: int) -> Optional[str]:
        """Get answer for a specific question number."""
        answers = self.get_as_dict()
        return answers.get(question_num)


def extract_answer_key(text: str, tail_lines: int = 4000) -> dict[int, str]:
    """
    Convenience function: extract answer key from text.
    Returns {question_number: answer_letter}.
    """
    extractor = AnswerExtractor(text, tail_lines=tail_lines)
    return extractor.get_as_dict()


def extract_inline_answer(question_text: str) -> Optional[str]:
    """
    Extract inline answer from within a question stem.
    Looks for patterns like: "Đáp án: B", "Answer: C", "Đáp án đúng: D"
    """
    patterns = [
        # Vietnamese: "Đáp án: B", "đáp án đúng: C", "đúng là A"
        r"(?i)(?:đáp\s*án|đáp\s*án\s*đúng|đúng\s*là)\s*[:=]?\s*([A-D])\b",
        # English: "answer is C", "answer: C", "correct: A", "The answer is: C"
        r"(?i)\b(?:answer|correct|right|key)\b.*?:\s*([A-D])\b",
    ]

    for pat in patterns:
        m = re.search(pat, question_text, re.IGNORECASE)
        if m:
            return m.group(1).upper()

    return None
