#!/usr/bin/env python3
"""Report Android string resources that are not referenced by project files."""

from __future__ import annotations

import argparse
import re
import xml.etree.ElementTree as ET
from pathlib import Path


DEFAULT_SCAN_ROOTS = (
    Path("app/src/main"),
    Path("app/src/test"),
)
DEFAULT_EXTENSIONS = {
    ".java",
    ".kt",
    ".kts",
    ".xml",
    ".gradle",
    ".properties",
    ".pro",
    ".json",
    ".md",
}
REFERENCE_PATTERNS = (
    re.compile(r"@string/([A-Za-z0-9_]+)"),
    re.compile(r"R\.string\.([A-Za-z0-9_]+)"),
    re.compile(r"getIdentifier\(\s*\"([A-Za-z0-9_]+)\"\s*,\s*\"string\""),
)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description=(
            "Find default Android string resources that are not referenced by "
            "source, layout, test, or documentation files."
        )
    )
    parser.add_argument(
        "--strings",
        type=Path,
        default=Path("app/src/main/res/values/strings.xml"),
        help="Default strings.xml to inspect",
    )
    parser.add_argument(
        "--scan-root",
        type=Path,
        action="append",
        help="Directory to scan. Can be passed multiple times.",
    )
    parser.add_argument(
        "--include-tests",
        action="store_true",
        help="Also scan app/src/test as references.",
    )
    parser.add_argument(
        "--show-used",
        action="store_true",
        help="Also print used keys.",
    )
    return parser.parse_args()


def read_string_names(path: Path) -> list[str]:
    root = ET.parse(path).getroot()
    names: list[str] = []
    for node in root.findall("string"):
        name = node.attrib.get("name")
        if name:
            names.append(name)
    return names


def scan_roots(args: argparse.Namespace) -> list[Path]:
    if args.scan_root:
        return args.scan_root
    roots = [DEFAULT_SCAN_ROOTS[0]]
    if args.include_tests:
        roots.append(DEFAULT_SCAN_ROOTS[1])
    return roots


def iter_scan_files(roots: list[Path], strings_path: Path) -> list[Path]:
    files: list[Path] = []
    resolved_strings = strings_path.resolve()
    for root in roots:
        if not root.exists():
            continue
        for path in root.rglob("*"):
            if not path.is_file() or path.suffix not in DEFAULT_EXTENSIONS:
                continue
            if path.resolve() == resolved_strings or is_strings_definition_file(path):
                continue
            files.append(path)
    return files


def is_strings_definition_file(path: Path) -> bool:
    return path.name == "strings.xml" and any(part.startswith("values") for part in path.parts)


def read_text(path: Path) -> str:
    return path.read_text(encoding="utf-8", errors="ignore")


def referenced_names(files: list[Path], known_names: set[str]) -> dict[str, set[Path]]:
    references: dict[str, set[Path]] = {name: set() for name in known_names}
    for path in files:
        text = read_text(path)
        for pattern in REFERENCE_PATTERNS:
            for match in pattern.findall(text):
                if match in known_names:
                    references[match].add(path)
    return references


def print_key_list(title: str, keys: list[str], references: dict[str, set[Path]] | None = None) -> None:
    print(f"{title}: {len(keys)}")
    for key in keys:
        print(f"  {key}")
        if references is not None:
            for path in sorted(references[key]):
                print(f"    {path}")


def main() -> int:
    args = parse_args()
    names = read_string_names(args.strings)
    known_names = set(names)
    files = iter_scan_files(scan_roots(args), args.strings)
    references = referenced_names(files, known_names)
    unused = [name for name in names if not references[name]]
    used = [name for name in names if references[name]]

    print(f"Strings file: {args.strings}")
    print(f"Scanned files: {len(files)}")
    print(f"Total string keys: {len(names)}")
    print_key_list("Possibly unused keys", unused)
    if args.show_used:
        print_key_list("Used keys", used, references)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
