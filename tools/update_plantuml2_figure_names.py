from __future__ import annotations

import re
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
PLANTUML_ROOT = ROOT / "docs" / "uml-diagrams" / "plantuml_2"
IMAGE_ROOT = PLANTUML_ROOT / "_images"
MANIFEST = IMAGE_ROOT / "FIGURE_NAMES.md"

ROLE_LABELS = {
    "00-common": ("Chung", "Common"),
    "01-student": ("Sinh viên", "Student"),
    "02-teacher": ("Giảng viên", "Teacher"),
    "03-admin": ("Quản trị viên", "Admin"),
}

DOMAIN_LABELS = {
    "analytics": ("Thống kê", "Analytics"),
    "auth": ("Xác thực", "Auth"),
    "class": ("Lớp học", "Class"),
    "dashboard": ("Dashboard", "Dashboard"),
    "exam": ("Kỳ thi", "Exam"),
    "monitoring": ("Theo dõi", "Monitoring"),
    "profile": ("Hồ sơ", "Profile"),
    "proctoring": ("Giám sát gian lận", "Proctoring"),
    "question": ("Câu hỏi", "Question"),
    "results": ("Kết quả", "Results"),
    "schedule": ("Lịch thi", "Schedule"),
    "system": ("Hệ thống", "System"),
    "user": ("Người dùng", "User"),
}


def read_utf8(path: Path) -> str:
    return path.read_text(encoding="utf-8-sig", errors="ignore")


def write_utf8(path: Path, text: str) -> None:
    path.write_text(text, encoding="utf-8", newline="\n")


def parse_startuml_id(text: str, fallback: str) -> str:
    match = re.search(r"(?m)^@startuml(?:\s+(.+?))?\s*$", text)
    if not match:
        return fallback
    value = (match.group(1) or fallback).strip()
    return value.strip('"')


def diagram_type(path: Path, start_id: str) -> tuple[str, str, str]:
    key = f"{path.stem} {start_id}".lower()
    if "sequence" in key:
        return "sequence", "Biểu đồ tuần tự", "Sequence"
    if "activity" in key:
        return "activity", "Biểu đồ hoạt động", "Activity"
    raise ValueError(f"Not an activity/sequence diagram: {path}")


def suffix_from_name(path: Path, start_id: str) -> tuple[str, str]:
    key = f"{path.stem} {start_id}".lower()
    part = re.search(r"part[-_](\d+)([a-z]?)", key)
    if part:
        number = int(part.group(1))
        letter = part.group(2).upper()
        return f"Phần {number:02d}{letter}", f"Part-{number:02d}{letter}"
    if "overview" in key:
        return "Tổng quan", "Overview"
    if "flow" in key:
        return "Luồng tổng quan", "Flow"
    return "", ""


def build_labels(path: Path, start_id: str) -> tuple[str, str]:
    rel = path.relative_to(PLANTUML_ROOT)
    parts = list(rel.parts)
    role_label, role_slug = ROLE_LABELS.get(parts[0], (parts[0], clean_slug(parts[0])))

    domain_label = ""
    domain_slug = ""
    if len(parts) > 2:
        domain_label, domain_slug = DOMAIN_LABELS.get(parts[1], (parts[1], clean_slug(parts[1])))

    _, type_label, type_slug = diagram_type(path, start_id)
    suffix_label, suffix_slug = suffix_from_name(path, start_id)

    title_parts = ["Hình", role_label]
    if domain_label:
        title_parts.append(domain_label)
    title_parts.append(type_label)
    if suffix_label:
        title_parts.append(suffix_label)
    title = " - ".join(title_parts)

    slug_parts = ["Hinh", role_slug]
    if domain_slug:
        slug_parts.append(domain_slug)
    slug_parts.append(type_slug)
    if suffix_slug:
        slug_parts.append(suffix_slug)
    slug = "-".join(slug_parts)
    return slug, title


def clean_slug(value: str) -> str:
    value = re.sub(r"^\d+-", "", value)
    value = re.sub(r"[^A-Za-z0-9]+", "-", value).strip("-")
    return value or "Diagram"


def replace_startuml_id(text: str, new_id: str) -> str:
    return re.sub(r"(?m)^@startuml(?:\s+.+?)?\s*$", f"@startuml {new_id}", text, count=1)


def replace_or_insert_title(text: str, title: str) -> str:
    title_line = f"title {title}"
    if re.search(r"(?m)^title\s+", text):
        return re.sub(r"(?m)^title\s+.*$", title_line, text, count=1)

    lines = text.splitlines()
    insert_at = 1
    for i, line in enumerate(lines[:8]):
        if line.strip().startswith("!theme"):
            insert_at = i + 1
            break
    lines.insert(insert_at, title_line)
    return "\n".join(lines) + ("\n" if text.endswith("\n") else "")


def update_image_name(rel_parent: Path, old_id: str, new_id: str) -> tuple[Path, Path]:
    old_png = IMAGE_ROOT / rel_parent / f"{old_id}.png"
    new_png = IMAGE_ROOT / rel_parent / f"{new_id}.png"
    if old_png == new_png:
        return old_png, new_png
    if old_png.exists():
        new_png.parent.mkdir(parents=True, exist_ok=True)
        if new_png.exists():
            new_png.unlink()
        old_png.rename(new_png)
    return old_png, new_png


def update_diagrams() -> list[dict[str, str]]:
    rows = []
    for path in sorted(PLANTUML_ROOT.rglob("*.puml")):
        if "_images" in path.parts:
            continue
        text = read_utf8(path)
        start_id = parse_startuml_id(text, path.stem)
        key = f"{path.stem} {start_id}".lower()
        if "activity" not in key and "sequence" not in key:
            continue

        new_id, title = build_labels(path, start_id)
        rel_parent = path.relative_to(PLANTUML_ROOT).parent
        old_png, new_png = update_image_name(rel_parent, start_id, new_id)

        new_text = replace_startuml_id(text, new_id)
        new_text = replace_or_insert_title(new_text, title)
        if new_text != text:
            write_utf8(path, new_text)

        rows.append({
            "puml": str(path.relative_to(ROOT)).replace("\\", "/"),
            "old_image": str(old_png.relative_to(ROOT)).replace("\\", "/"),
            "new_image": str(new_png.relative_to(ROOT)).replace("\\", "/"),
            "title": title,
        })
    return rows


def write_manifest(rows: list[dict[str, str]]) -> None:
    IMAGE_ROOT.mkdir(parents=True, exist_ok=True)
    lines = [
        "# Tên Hình PlantUML 2",
        "",
        "Danh sách tên ảnh cho các biểu đồ hoạt động và biểu đồ tuần tự trong `plantuml_2/_images`.",
        "",
        "| STT | File ảnh | Tên hình | Source |",
        "| --- | --- | --- | --- |",
    ]
    for index, row in enumerate(rows, start=1):
        lines.append(f"| {index} | `{row['new_image']}` | {row['title']} | `{row['puml']}` |")
    write_utf8(MANIFEST, "\n".join(lines) + "\n")


def main() -> None:
    rows = update_diagrams()
    write_manifest(rows)
    print(f"updated {len(rows)} diagrams")
    print(f"manifest {MANIFEST}")


if __name__ == "__main__":
    main()
