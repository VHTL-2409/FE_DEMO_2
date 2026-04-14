"""
Phân đoạn tài liệu theo heading / phần — generic, không gắn một template đề cụ thể.
"""

from __future__ import annotations

import re
from dataclasses import dataclass

_SECTION_LINE = re.compile(
    r'^(?:Phần|PHẦN|Part|PART)\s*[IVX\d]+',
    re.IGNORECASE,
)
_ESSAY_MARKERS = re.compile(
    r'TỰ\s*LUẬN|ESSAY|Phần\s*tự\s*luận',
    re.IGNORECASE,
)


@dataclass
class DocumentSection:
    name: str
    kind: str
    start_line: int
    text: str


def segment_by_headings(lines: list[str]) -> list[DocumentSection]:
    """Tách theo dòng bắt đầu bằng Phần/Part hoặc marker essay."""
    if not lines:
        return [DocumentSection(name='body', kind='mixed', start_line=0, text='\n'.join(lines))]

    sections: list[DocumentSection] = []
    current_name = 'body'
    current_kind = 'mixed'
    start = 0
    buffer: list[str] = []

    for i, line in enumerate(lines):
        stripped = line.strip()
        if _SECTION_LINE.match(stripped):
            if buffer:
                sections.append(
                    DocumentSection(
                        name=current_name,
                        kind=current_kind,
                        start_line=start,
                        text='\n'.join(buffer),
                    )
                )
            current_name = stripped[:120]
            current_kind = 'essay' if _ESSAY_MARKERS.search(stripped) else 'section'
            start = i
            buffer = [line]
        elif _ESSAY_MARKERS.search(stripped) and not buffer:
            current_kind = 'essay'
            buffer.append(line)
        else:
            if not buffer and not sections:
                start = i
            buffer.append(line)

    if buffer:
        sections.append(
            DocumentSection(
                name=current_name,
                kind=current_kind,
                start_line=start,
                text='\n'.join(buffer),
            )
        )

    return sections if sections else [
        DocumentSection(name='body', kind='mixed', start_line=0, text='\n'.join(lines))
    ]


def lines_from_full_text(full: str) -> list[str]:
    return [ln.strip() for ln in full.splitlines() if ln.strip()]
