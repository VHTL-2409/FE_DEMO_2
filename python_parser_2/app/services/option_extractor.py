"""
Trích đáp án trắc nghiệm: marker-based + độ tin cậy; hỗ trợ nhiều biến thể A./A)/A:
"""

from __future__ import annotations

import re
from dataclasses import dataclass


@dataclass
class OptionExtractionResult:
    options: dict[str, str]
    confidence: float
    method: str
    issues: list[str]


def expand_glued_options(text: str) -> str:
    if not text:
        return text
    out = text
    for _ in range(14):
        prev = out
        out = re.sub(r'(?<=\))(?=[B-D]\.)', '\n', out)
        out = re.sub(r'(?<=\])(?=[B-D]\.)', '\n', out)
        out = re.sub(r'(?<=[0-9])(?=[B-D]\.)', '\n', out)
        if prev == out:
            break
    return out


def collect_option_markers(text: str) -> list[re.Match[str]]:
    found: list[re.Match[str]] = []
    patterns = [
        r'(?:^|\n)\s*([A-D])\s*[\.)．:]\s+\S',
        r'(?:^|\n)\s*([A-D])\s*[\.)．:](?=\S)',
        r'(?<=\s)([A-D])\s*[\.)．:]\s+(?=\S)',
    ]
    for p in patterns:
        for m in re.finditer(p, text, re.MULTILINE):
            if p.endswith(r'(?=\S)') and m.start() > 0 and text[m.start() - 1].isalpha():
                continue
            found.append(m)
    found.sort(key=lambda x: x.start())
    out: list[re.Match[str]] = []
    for m in found:
        if not out or m.start() != out[-1].start():
            out.append(m)
    return out


def first_option_match(text: str) -> re.Match[str] | None:
    for m in collect_option_markers(text):
        tail = text[m.end() : m.end() + 14].lstrip()
        if re.match(r'^[A-D]\s*[\.)．:]', tail):
            continue
        return m
    return None


def stem_before_first_option(text: str, m: re.Match[str]) -> str:
    stem = text[: m.start()]
    letter = m.group(1)
    if letter != 'A':
        stem = re.sub(r'A\s*[\.)．:]\s*$', '', stem.rstrip())
    return stem.strip()


def parse_options_by_markers(text: str) -> dict[str, str]:
    options: dict[str, str] = {}
    m0 = first_option_match(text)
    if not m0:
        return options
    rest = text[m0.start() :]
    matches = collect_option_markers(rest)
    if len(matches) < 2:
        return options
    for i, m in enumerate(matches):
        letter = m.group(1)
        a = m.end()
        b = matches[i + 1].start() if i + 1 < len(matches) else len(rest)
        chunk = rest[a:b].strip()
        chunk = re.sub(r'\s+', ' ', chunk)
        if chunk:
            options[letter] = chunk
    return options


def extract_options(block_text: str) -> OptionExtractionResult:
    issues: list[str] = []
    t = expand_glued_options(block_text)
    opts = parse_options_by_markers(t)
    if len(opts) >= 4:
        return OptionExtractionResult(
            options=opts,
            confidence=0.9,
            method='markers_4plus',
            issues=issues,
        )
    if len(opts) >= 2:
        issues.append('fewer_than_4_options')
        return OptionExtractionResult(
            options=opts,
            confidence=0.72,
            method='markers_2_3',
            issues=issues,
        )
    issues.append('no_options_detected')
    return OptionExtractionResult(options={}, confidence=0.35, method='none', issues=issues)
