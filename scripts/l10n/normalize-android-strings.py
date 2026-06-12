#!/usr/bin/env python3
"""Normalize a translated Android strings.xml against the default resource file."""

from __future__ import annotations

import argparse
import re
import sys
import xml.etree.ElementTree as ET
from dataclasses import dataclass
from pathlib import Path
from typing import Iterable
from xml.sax.saxutils import escape


PLACEHOLDER_PATTERN = re.compile(
    r"%(?:\d+\$)?[-#+ 0,(<]*(?:\d+)?(?:\.\d+)?[tT]?[a-zA-Z%]"
)

# Project-owned strings that should exist for a locale even if a contributor
# file was created before that language option existed. These are not counted
# as missing translations.
MAINTAINER_TRANSLATIONS_BY_OUTPUT = {
    "values-ru": {
        "settings_language_russian": "Русский",
    },
}


@dataclass(frozen=True)
class StringEntry:
    name: str
    value: str
    contains_markup: bool = False


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description=(
            "Sort and prune a translated Android strings.xml using the default "
            "values/strings.xml as the schema."
        )
    )
    parser.add_argument("source", type=Path, help="Contributor strings.xml file")
    parser.add_argument(
        "--base",
        type=Path,
        default=Path("app/src/main/res/values/strings.xml"),
        help="Default strings.xml used as the key order/schema",
    )
    parser.add_argument(
        "--output",
        type=Path,
        required=True,
        help="Normalized output strings.xml",
    )
    parser.add_argument(
        "--missing-report",
        type=Path,
        help=(
            "Optional maintainer-only full strings view. Existing translations "
            "are written normally and missing keys are commented out with the "
            "default string value."
        ),
    )
    parser.add_argument(
        "--allow-placeholder-mismatch",
        action="store_true",
        help="Write output even when placeholders differ from the default string",
    )
    return parser.parse_args()


def read_entries(path: Path) -> tuple[list[StringEntry], list[str]]:
    parser = ET.XMLParser(encoding="utf-8")
    root = ET.parse(path, parser=parser).getroot()
    if root.tag != "resources":
        raise ValueError(f"{path} root tag is {root.tag!r}, expected 'resources'")

    entries: list[StringEntry] = []
    duplicates: list[str] = []
    seen: set[str] = set()
    for node in root.findall("string"):
        name = node.attrib.get("name")
        if not name:
            continue
        if name in seen:
            duplicates.append(name)
        seen.add(name)
        entries.append(StringEntry(
            name=name,
            value=string_value(node),
            contains_markup=len(node) > 0,
        ))
    return entries, duplicates


def string_value(node: ET.Element) -> str:
    parts: list[str] = []
    if node.text:
        parts.append(escape_text_segment(node.text, has_markup=len(node) > 0))
    for child in node:
        parts.append(markup_without_tail(child))
        if child.tail:
            parts.append(escape_text_segment(child.tail, has_markup=True))
    return "".join(parts)


def placeholders(value: str) -> list[str]:
    return PLACEHOLDER_PATTERN.findall(value)


def maintainer_translations(output_path: Path) -> dict[str, str]:
    for part in output_path.parts:
        translations = MAINTAINER_TRANSLATIONS_BY_OUTPUT.get(part)
        if translations is not None:
            return translations
    return {}


def write_entries(path: Path, entries: Iterable[StringEntry]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    lines = ['<?xml version="1.0" encoding="utf-8"?>', "<resources>"]
    for entry in entries:
        lines.append(render_string_line(entry))
    lines.append("</resources>")
    path.write_text("\n".join(lines) + "\n", encoding="utf-8")


def write_missing_report(
        path: Path,
        base_entries: Iterable[StringEntry],
        source_by_name: dict[str, StringEntry],
        maintained_values: dict[str, str]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    lines = [
        "<?xml version=\"1.0\" encoding=\"utf-8\"?>",
        "<!--",
        "Maintainer-only full translation report.",
        "This file is not an Android resource and should not be placed under app/src/main/res.",
        "Translated strings are written normally.",
        "Commented strings are missing in the contributor file and currently fall back to app/src/main/res/values/strings.xml.",
        "-->",
        "<resources>",
    ]
    for base_entry in base_entries:
        source_entry = source_by_name.get(base_entry.name)
        if source_entry is not None:
            lines.append(render_string_line(source_entry))
        elif base_entry.name in maintained_values:
            lines.append(render_string_line(StringEntry(
                name=base_entry.name,
                value=maintained_values[base_entry.name],
            )))
        else:
            lines.append(f"    <!-- {comment_safe(render_string_line(base_entry).strip())} -->")
    lines.append("</resources>")
    path.write_text("\n".join(lines) + "\n", encoding="utf-8")


def render_string_line(entry: StringEntry) -> str:
    value = normalize_value(entry.value, contains_markup=entry.contains_markup)
    return f'    <string name="{entry.name}">{value}</string>'


def comment_safe(value: str) -> str:
    return value.replace("--", "- -")


def normalize_value(value: str, contains_markup: bool) -> str:
    value = value.replace("\r\n", "\n").replace("\r", "\n")
    value = value.replace("\n", "\\n")
    if contains_markup:
        return value
    return escape(value, {'"': "&quot;"})


def escape_text_segment(value: str, has_markup: bool) -> str:
    if not has_markup:
        return value
    return escape(value, {'"': "&quot;"})


def markup_without_tail(node: ET.Element) -> str:
    tail = node.tail
    node.tail = None
    try:
        return ET.tostring(node, encoding="unicode", method="xml")
    finally:
        node.tail = tail


def print_list(title: str, values: list[str], limit: int = 40) -> None:
    print(f"{title}: {len(values)}")
    for value in values[:limit]:
        print(f"  {value}")
    if len(values) > limit:
        print(f"  ... {len(values) - limit} more")


def main() -> int:
    args = parse_args()
    base_entries, base_duplicates = read_entries(args.base)
    source_entries, source_duplicates = read_entries(args.source)

    base_by_name = {entry.name: entry for entry in base_entries}
    source_by_name = {entry.name: entry for entry in source_entries}
    base_names = [entry.name for entry in base_entries]
    base_name_set = set(base_names)
    source_name_set = set(source_by_name)
    maintained_values = maintainer_translations(args.output)
    maintained_name_set = set(maintained_values)

    extras = [entry.name for entry in source_entries if entry.name not in base_name_set]
    missing = [name for name in base_names if name not in source_name_set and name not in maintained_name_set]
    empty_values = [entry.name for entry in source_entries if entry.name in base_name_set and entry.value == ""]

    placeholder_mismatches: list[str] = []
    for name in base_names:
        source_entry = source_by_name.get(name)
        if source_entry is None:
            continue
        base_placeholders = placeholders(base_by_name[name].value)
        source_placeholders = placeholders(source_entry.value)
        if base_placeholders != source_placeholders:
            placeholder_mismatches.append(
                f"{name}: base {base_placeholders} != source {source_placeholders}"
            )

    normalized_entries: list[StringEntry] = []
    for name in base_names:
        source_entry = source_by_name.get(name)
        if source_entry is not None:
            normalized_entries.append(source_entry)
        elif name in maintained_values:
            normalized_entries.append(StringEntry(
                name=name,
                value=maintained_values[name],
            ))

    if placeholder_mismatches and not args.allow_placeholder_mismatch:
        print("Refusing to write output because placeholder mismatches were found.", file=sys.stderr)
        print_list("Placeholder mismatches", placeholder_mismatches)
        return 1

    write_entries(args.output, normalized_entries)
    if args.missing_report:
        write_missing_report(args.missing_report, base_entries, source_by_name, maintained_values)

    print(f"Wrote: {args.output}")
    if args.missing_report:
        print(f"Wrote missing report: {args.missing_report}")
    print(f"Base keys: {len(base_entries)}")
    print(f"Source keys: {len(source_entries)}")
    print(f"Output keys: {len(normalized_entries)}")
    print_list("Base duplicate keys", base_duplicates)
    print_list("Source duplicate keys", source_duplicates)
    print_list("Maintainer-provided keys", [name for name in base_names if name in maintained_values])
    print_list("Pruned unknown keys", sorted(set(extras)))
    print_list("Missing keys that will fall back to default", missing)
    print_list("Empty translated values", empty_values)
    print_list("Placeholder mismatches", placeholder_mismatches)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
