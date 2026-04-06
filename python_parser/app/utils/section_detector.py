"""Section detector for Vietnamese/English exam PDFs.

Scans text to identify exam sections (MCQ / Essay / Mixed) and returns
span metadata that parsers can use to apply different extraction strategies.

Usage:
    from app.utils.section_detector import detect_sections, SectionKind, Section

    sections = detect_sections(full_text)
    for section in sections:
        if section.kind == SectionKind.ESSAY:
            ...
"""
from dataclasses import dataclass
from enum import Enum
import re


class SectionKind(Enum):
    MCQ = "mcq"
    ESSAY = "essay"
    ANSWER_KEY = "answer"
    SOLUTION = "solution"
    QUESTION = "question"
    UNKNOWN = "unknown"


@dataclass(frozen=True)
class Section:
    start_line: int
    end_line: int
    kind: SectionKind
    title: str | None = None


def _normalize(line: str) -> str:
    """Lowercase and remove tabs/ideographic spaces."""
    return line.lower().replace("\t", "").replace("\u3000", "")


def _detect_kind(line: str) -> SectionKind | None:
    """
    Return the section kind for a single line, or None if it is not a header.

    Detection strategy: simple startswith() checks at the START of the normalized line,
    followed by immediate non-letter check. This avoids all complex Unicode/NFC edge cases.
    """
    n = _normalize(line).strip()

    # ── 1. Roman numeral sections (must be at START of line) ─────────────────
    # "Phần I" → "phần i ", "Phần III" → "phần iii ", "Phần II" → "phần ii "
    # We use startswith() so that "phần iii" does NOT match "phần ii" (ends differently)
    roman_section_mcq = [
        "phần i", "phần iii", "phần v",
        "phần vii", "phần ix",
        "section i", "section iii",
        "part i",
    ]
    roman_section_essay = [
        "phần ii", "phần iv", "phần vi",
        "phần viii", "phần x",
        "section ii", "part ii",
    ]

    for prefix in roman_section_mcq:
        # Check startswith and that what follows is not a letter
        if n.startswith(prefix) and (len(n) == len(prefix) or not (n[len(prefix)].isalpha())):
            return SectionKind.MCQ
    for prefix in roman_section_essay:
        if n.startswith(prefix) and (len(n) == len(prefix) or not (n[len(prefix)].isalpha())):
            return SectionKind.ESSAY

    # ── 2. Arabic digit sections (must be at START) ───────────────────────────
    # "phần 1", "phần 1.", "phần1"
    arabic_m = re.match(r"^(phần|section|part)\s*(\d)", n)
    if arabic_m:
        num = int(arabic_m.group(2))
        return SectionKind.MCQ if num % 2 == 1 else SectionKind.ESSAY

    # ── 3. Keyword-only sections ──────────────────────────────────────────────
    if n.startswith("tự luận") or n.startswith("tựluận"):
        return SectionKind.ESSAY
    if n.startswith("trắc nghiệm") or n.startswith("trắcnghiệm"):
        return SectionKind.MCQ

    # ── 4. Answer key patterns ───────────────────────────────────────────
    if re.match(r"^đáp án|^đápán|^bảng đáp án|^bảngđápán|^answer key", n):
        return SectionKind.ANSWER_KEY

    # ── 5. Solution section patterns ──────────────────────────────────────
    if re.match(r"^lời giải|^lờigiải|^giải chi tiết|^solution", n):
        return SectionKind.SOLUTION

    # ── 6. Question section pattern ───────────────────────────────────────
    if re.match(r"^phần câu hỏi|^phầncâuhỏi", n):
        return SectionKind.QUESTION

    # ── 7. Neutral fallback ───────────────────────────────────────────────
    if n.startswith("bàithi") or n.startswith("mục"):
        return SectionKind.UNKNOWN

    return None


def _title_for(n: str, kind: SectionKind | None) -> str | None:
    """Return a readable title for the detected section."""
    if kind is None:
        return None
    stripped = n.strip()

    if kind == SectionKind.MCQ:
        # Check from longest to shortest to avoid partial matches
        for pat, title in [
            ("phần viii", "PHẦN VIII"), ("phần vii", "PHẦN VII"),
            ("phần ix", "PHẦN IX"), ("phần x", "PHẦN X"),
            ("phần iii", "PHẦN III"), ("phần vi", "PHẦN VI"),
            ("phần iv", "PHẦN IV"), ("phần v", "PHẦN V"),
            ("phần ii", "PHẦN II"), ("phần i", "PHẦN I"),
            ("phần 10", "PHẦN X"), ("phần 9", "PHẦN IX"),
            ("phần 8", "PHẦN VIII"), ("phần 7", "PHẦN VII"),
            ("phần 6", "PHẦN VI"), ("phần 5", "PHẦN V"),
            ("phần 4", "PHẦN IV"), ("phần 3", "PHẦN III"),
            ("phần 2", "PHẦN II"), ("phần 1", "PHẦN I"),
            ("section 1", "Section I"), ("section i", "Section I"),
            ("part 3", "Part III"), ("part iii", "Part III"),
            ("part 2", "Part II"), ("part ii", "Part II"),
            ("part 1", "Part I"), ("part i", "Part I"),
        ]:
            if stripped.startswith(pat):
                return title
        if stripped.startswith("trắc nghiệm") or stripped.startswith("trắcnghiệm"):
            return "Trắc nghiệm"
        return "MCQ"

    if kind == SectionKind.ESSAY:
        for pat, title in [
            ("phần viii", "PHẦN VIII"), ("phần vii", "PHẦN VII"),
            ("phần ix", "PHẦN IX"), ("phần x", "PHẦN X"),
            ("phần iii", "PHẦN III"), ("phần vi", "PHẦN VI"),
            ("phần iv", "PHẦN IV"), ("phần v", "PHẦN V"),
            ("phần ii", "PHẦN II"), ("phần i", "PHẦN I"),
            ("phần 10", "PHẦN X"), ("phần 9", "PHẦN IX"),
            ("phần 8", "PHẦN VIII"), ("phần 7", "PHẦN VII"),
            ("phần 6", "PHẦN VI"), ("phần 5", "PHẦN V"),
            ("phần 4", "PHẦN IV"), ("phần 3", "PHẦN III"),
            ("phần 2", "PHẦN II"), ("phần 1", "PHẦN I"),
            ("section 2", "Section II"), ("section ii", "Section II"),
            ("section 1", "Section I"), ("section i", "Section I"),
            ("part 3", "Part III"), ("part ii", "Part II"),
            ("part 2", "Part II"), ("part i", "Part I"),
        ]:
            if stripped.startswith(pat):
                return title
        if stripped.startswith("tự luận") or stripped.startswith("tựluận"):
            return "Tự luận"
        return "Tự luận"

    if kind == SectionKind.ANSWER_KEY:
        if stripped.startswith("bảng đáp án") or stripped.startswith("bảngđápán"):
            return "Bảng đáp án"
        if stripped.startswith("answer key"):
            return "Answer Key"
        return "Đáp án"

    if kind == SectionKind.SOLUTION:
        if stripped.startswith("lời giải") or stripped.startswith("lờigiải"):
            return "Lời giải chi tiết"
        if stripped.startswith("giải chi tiết") or stripped.startswith("giảichi tiết"):
            return "Giải chi tiết"
        if stripped.startswith("solution"):
            return "Solution"
        return "Lời giải"

    if kind == SectionKind.QUESTION:
        return "Phần câu hỏi"

    return None


def detect_sections(text: str) -> list[Section]:
    """
    Scan text and return a list of Section spans.

    Detection rules:
      - "Phần I" / "Phần 1" → MCQ
      - "Phần II" / "Phần 2" / "Tự luận" → ESSAY
      - "Phần III" etc. → parity: odd=MCQ, even=ESSAY
      - "Đáp án" → ANSWER_KEY
      - "Lời giải" → SOLUTION
    """
    lines = text.split("\n")
    sections: list[Section] = []
    current_kind: SectionKind | None = None
    current_start = 0
    current_title: str | None = None

    for i, raw_line in enumerate(lines):
        line = raw_line.rstrip()
        if not line:
            continue
        detected = _detect_kind(line)
        if detected is not None:
            if current_kind is not None and current_kind != detected:
                if current_start < i:
                    sections.append(Section(
                        start_line=current_start,
                        end_line=i,
                        kind=current_kind,
                        title=current_title,
                    ))
                current_kind = detected
                current_start = i
                current_title = _title_for(_normalize(line), detected)
            elif current_kind is None:
                current_kind = detected
                current_start = i
                current_title = _title_for(_normalize(line), detected)
            else:
                t = _title_for(_normalize(line), detected)
                if t:
                    current_title = t

    if current_kind is not None and current_start < len(lines):
        sections.append(Section(
            start_line=current_start,
            end_line=len(lines),
            kind=current_kind,
            title=current_title,
        ))

    if not sections:
        sections.append(Section(0, len(lines), SectionKind.UNKNOWN, None))

    return sections


def has_essay_section(text: str) -> bool:
    return any(s.kind == SectionKind.ESSAY for s in detect_sections(text))


def is_essay_only(text: str) -> bool:
    sections = detect_sections(text)
    return len(sections) == 1 and sections[0].kind == SectionKind.ESSAY
