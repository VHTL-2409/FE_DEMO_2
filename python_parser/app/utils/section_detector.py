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


class SectionKind(Enum):
    MCQ = "mcq"
    ESSAY = "essay"
    UNKNOWN = "unknown"


@dataclass(frozen=True)
class Section:
    start_line: int
    end_line: int
    kind: SectionKind  # ESSAY / MCQ / UNKNOWN
    title: str | None = None  # matched header text, if any


# ── Normalize helper ─────────────────────────────────────────────────────────

def _normalize(line: str) -> str:
    """Lowercase and remove spaces/tabs for compact matching."""
    return line.lower().replace(" ", "").replace("\t", "").replace("\u3000", "")


# ── Section detection ────────────────────────────────────────────────────────

def _has_roman_suffix(n: str, suffix: str) -> bool:
    """
    Check if n ends with suffix (roman numeral or digit) NOT followed by
    additional letters. E.g. "phầnii" matches but "phầniii" does NOT.
    """
    idx = n.rfind(suffix)
    if idx == -1:
        return False
    rest = n[idx + len(suffix):]
    return len(rest) == 0 or not rest[0].isalpha()


def _detect_kind(line: str) -> SectionKind | None:
    """
    Return the section kind for a single line, or None if it is not a header.

    Pattern ordering (IMPORTANT):
      1. Check longer suffixes FIRST to prevent partial matches
         (e.g. "phầnii" before "phầni", "phầniii" before "phầnii")
      2. Section number (Phần I/II/III…) takes priority over "Tự luận" keyword
      3. Roman numeral parity: I=1, III=3 → MCQ (odd); II=2, IV=4 → ESSAY (even)
    """
    n = _normalize(line)

    # ── Roman numeral and digit suffixes (higher priority) ─────────────────────
    # "phầnIII" / "sectionIII" → odd = MCQ, even = ESSAY
    # "phầnII" / "sectionII" / "partII" → ESSAY
    # "phầnI" / "sectionI" / "partI" → MCQ
    # "phần2" / "section2" / "part2" → ESSAY
    # "phần1" / "section1" / "part1" → MCQ
    import re

    # Roman numeral: III, IV, V, VI, VII, VIII, IX, X → parity determines kind
    roman_parity = {
        "iii": 1,  # MCQ
        "iv": 2,   # ESSAY
        "v": 1,    # MCQ
        "vi": 2,   # ESSAY
        "vii": 1,  # MCQ
        "viii": 2, # ESSAY
        "ix": 1,   # MCQ
        "x": 2,    # ESSAY
    }
    for roman, parity in roman_parity.items():
        if _has_roman_suffix(n, f"phần{roman}"):
            return SectionKind.MCQ if parity == 1 else SectionKind.ESSAY
        if _has_roman_suffix(n, f"section{roman}") or _has_roman_suffix(n, f"part{roman}"):
            return SectionKind.MCQ if parity == 1 else SectionKind.ESSAY

    # Roman numeral I / II (must check II before I to avoid "II" matching as "I")
    if _has_roman_suffix(n, "phầnii") or _has_roman_suffix(n, "sectionii") or _has_roman_suffix(n, "partii"):
        return SectionKind.ESSAY
    if _has_roman_suffix(n, "phầni") or _has_roman_suffix(n, "sectioni") or _has_roman_suffix(n, "parti"):
        return SectionKind.MCQ

    # Arabic digits 1-9
    m = re.search(r"phần(\d)", n) or re.search(r"section(\d)", n) or re.search(r"part(\d)", n)
    if m:
        num = int(m.group(1))
        return SectionKind.MCQ if num % 2 == 1 else SectionKind.ESSAY

    # Vietnamese keyword "tự luận" (only if no section number was found)
    if "tựluận" in n:
        return SectionKind.ESSAY

    # Neutral
    if "trắcnghiệm" in n:
        return SectionKind.MCQ
    if "bàithi" in n or n.startswith("mục"):
        return SectionKind.UNKNOWN

    return None


def _title_for(n: str, kind: SectionKind | None) -> str | None:
    if kind is None:
        return None
    if kind == SectionKind.MCQ:
        if _has_roman_suffix(n, "phầni") or _has_roman_suffix(n, "phần1"): return "PHẦN I"
        if "trắcnghiệm" in n: return "Trắc nghiệm"
        if _has_roman_suffix(n, "phầniii"): return "PHẦN III"
        if _has_roman_suffix(n, "sectioni") or _has_roman_suffix(n, "parti"): return "Section I"
        return "MCQ"
    if kind == SectionKind.ESSAY:
        if _has_roman_suffix(n, "phầnii") or _has_roman_suffix(n, "phần2"): return "PHẦN II"
        if "tựluận" in n: return "Tự luận"
        if _has_roman_suffix(n, "sectionii") or _has_roman_suffix(n, "partii"): return "Section II"
        return "Essay"
    return None


def detect_sections(text: str) -> list[Section]:
    """
    Scan *text* (multi-line string) and return a list of Section spans.

    Detection rules:
      - "Phần I" / "Phần 1" / "Section I" / "Part I" → MCQ
      - "Phần II" / "Phần 2" / "Tự luận" / "Section II" → ESSAY
      - "Phần III" etc. → use parity: odd=MCQ, even=ESSAY
      - Fallback: single UNKNOWN section covering the whole text

    The returned sections are non-overlapping and sorted by start_line.
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