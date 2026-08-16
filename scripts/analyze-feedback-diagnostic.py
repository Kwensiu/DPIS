#!/usr/bin/env python3
"""Create an evidence-oriented Perfetto report from a DPIS feedback ZIP."""

import argparse
import csv
import io
import shutil
import subprocess
import sys
import tempfile
import urllib.request
import zipfile
from pathlib import Path


TRACE_ENTRY = "perfetto-trace.pftrace"
DIAGNOSTIC_ENTRY = "diagnostic.txt"
TRACE_PROCESSOR_URL = "https://get.perfetto.dev/trace_processor"


def parse_args():
    parser = argparse.ArgumentParser(
        description="Analyze the Perfetto trace exported in a DPIS feedback diagnostic ZIP."
    )
    parser.add_argument("diagnostic_zip", type=Path)
    parser.add_argument(
        "--output",
        type=Path,
        help="Write the Markdown report here instead of stdout.",
    )
    parser.add_argument(
        "--trace-processor",
        type=Path,
        help="Use an existing official trace_processor wrapper instead of the repo-local one.",
    )
    return parser.parse_args()


def repo_root():
    return Path(__file__).resolve().parents[1]


def ensure_trace_processor(explicit_path):
    processor = explicit_path or repo_root() / "trace_processor"
    if processor.is_file():
        return processor

    if explicit_path:
        raise RuntimeError(f"trace_processor does not exist: {processor}")

    print(f"Downloading official trace_processor wrapper to {processor}", file=sys.stderr)
    try:
        urllib.request.urlretrieve(TRACE_PROCESSOR_URL, processor)
    except OSError as error:
        raise RuntimeError(f"Unable to download trace_processor: {error}") from error
    return processor


def package_name(diagnostic):
    for line in diagnostic.splitlines():
        if line.startswith("package: "):
            return line.removeprefix("package: ").strip()
    return ""


def perfetto_summary(diagnostic):
    wanted = {"available", "exported", "sizeBytes", "truncated", "note"}
    values = {}
    in_section = False
    for line in diagnostic.splitlines():
        if line == "[perfetto]":
            in_section = True
            continue
        if in_section and line.startswith("["):
            break
        if in_section and ": " in line:
            key, value = line.split(": ", 1)
            if key in wanted:
                values[key] = value
    return values


def font_aggregates(diagnostic):
    return [line for line in diagnostic.splitlines() if line.startswith("route: ")]


def sql_literal(value):
    return "'" + value.replace("'", "''") + "'"


def run_query(processor, trace_path, sql):
    command = [sys.executable, str(processor), "query", str(trace_path), sql]
    completed = subprocess.run(command, capture_output=True, text=True, encoding="utf-8")
    if completed.returncode != 0:
        message = (completed.stderr or completed.stdout).strip()
        return None, message or f"trace_processor exited with {completed.returncode}"
    return completed.stdout.strip(), ""


def compact_query_error(error):
    lines = [line.strip() for line in error.splitlines() if line.strip()]
    missing_table = next((line for line in lines if line.startswith("no such table:")), "")
    if missing_table:
        return missing_table
    return "\n".join(lines[-3:]) if lines else "trace_processor query failed"


def table_or_text(text):
    if not text:
        return "(no rows)"
    try:
        rows = list(csv.reader(io.StringIO(text)))
    except csv.Error:
        return text
    if not rows:
        return "(no rows)"
    if len(rows) == 1:
        return "(no matching rows)"
    widths = [max(len(row[index]) if index < len(row) else 0 for row in rows)
              for index in range(max(len(row) for row in rows))]
    rendered = []
    for row_index, row in enumerate(rows):
        padded = row + [""] * (len(widths) - len(row))
        rendered.append(" | ".join(value.ljust(widths[index]) for index, value in enumerate(padded)))
        if row_index == 0:
            rendered.append("-+-".join("-" * width for width in widths))
    return "\n".join(rendered)


def query_section(title, processor, trace_path, sql):
    output, error = run_query(processor, trace_path, sql)
    lines = [f"## {title}", "", "```text"]
    lines.append(
        table_or_text(output) if not error else f"Unavailable: {compact_query_error(error)}"
    )
    lines.extend(["```", ""])
    return lines


def build_report(source_zip, diagnostic, trace_path, processor):
    package = package_name(diagnostic)
    package_match = sql_literal(package if package else "%")
    report = [
        "# DPIS Feedback Diagnostic Perfetto Report",
        "",
        f"- Source ZIP: `{source_zip}`",
        f"- Target package: `{package or 'unknown'}`",
        f"- Trace bytes: `{trace_path.stat().st_size}`",
        f"- Trace processor: `{processor}`",
        "",
        "## Export Metadata",
        "",
    ]
    for key, value in perfetto_summary(diagnostic).items():
        report.append(f"- {key}: `{value}`")
    report.extend(["", "## DPIS Font Aggregates", ""])
    report.extend([f"- `{line}`" for line in font_aggregates(diagnostic)] or ["- unavailable"])
    report.append("")

    # Use raw trace tables so the report remains useful even when a capture did not include
    # SurfaceFlinger frame metrics. A missing jank table is reported as unavailable, not zero.
    process_filter = (
        f"p.name = {package_match} OR p.name GLOB {sql_literal(package + ':*')}"
        if package
        else "1 = 1"
    )
    report.extend(query_section(
        "Target Processes",
        processor,
        trace_path,
        f"SELECT p.pid, p.name FROM process p WHERE {process_filter} ORDER BY p.pid;",
    ))
    report.extend(query_section(
        "Frame Jank",
        processor,
        trace_path,
        "SELECT COUNT(*) AS frame_count, SUM(was_jank) AS jank_count "
        "FROM android_frame_stats;",
    ))
    report.extend(query_section(
        "FrameTimeline Jank Types",
        processor,
        trace_path,
        "SELECT p.name, a.jank_type, COUNT(*) AS frame_count, "
        "ROUND(SUM(a.dur) / 1000000.0, 3) AS total_ms "
        "FROM actual_frame_timeline_slice a LEFT JOIN process p USING (upid) "
        f"WHERE {process_filter} GROUP BY p.name, a.jank_type "
        "ORDER BY frame_count DESC;",
    ))
    report.extend(query_section(
        "Longest FrameTimeline Frames",
        processor,
        trace_path,
        "SELECT ROUND(a.ts / 1000000000.0, 6) AS ts_s, "
        "ROUND(a.dur / 1000000.0, 3) AS dur_ms, a.jank_type, "
        "a.layer_name, p.name "
        "FROM actual_frame_timeline_slice a LEFT JOIN process p USING (upid) "
        f"WHERE {process_filter} ORDER BY a.dur DESC LIMIT 30;",
    ))
    report.extend(query_section(
        "Target CPU Scheduling",
        processor,
        trace_path,
        "SELECT p.name, ROUND(SUM(s.dur) / 1000000.0, 3) AS running_ms, "
        "COUNT(*) AS sched_slices "
        "FROM sched s JOIN thread t USING (utid) JOIN process p USING (upid) "
        f"WHERE {process_filter} GROUP BY p.upid, p.name ORDER BY running_ms DESC;",
    ))
    report.extend(query_section(
        "Longest Target Thread Runs",
        processor,
        trace_path,
        "SELECT ROUND(s.ts / 1000000000.0, 6) AS ts_s, "
        "ROUND(s.dur / 1000000.0, 3) AS dur_ms, t.tid, t.name "
        "FROM sched s JOIN thread t USING (utid) JOIN process p USING (upid) "
        f"WHERE {process_filter} ORDER BY s.dur DESC LIMIT 20;",
    ))
    report.extend(query_section(
        "Longest Target Atrace Slices",
        processor,
        trace_path,
        "SELECT ROUND(s.ts / 1000000000.0, 6) AS ts_s, "
        "ROUND(s.dur / 1000000.0, 3) AS dur_ms, t.tid, t.name AS thread_name, s.name "
        "FROM slice s JOIN thread_track tt ON s.track_id = tt.id "
        "JOIN thread t ON tt.utid = t.utid JOIN process p ON t.upid = p.upid "
        f"WHERE {process_filter} ORDER BY s.dur DESC LIMIT 30;",
    ))
    report.extend(query_section(
        "Target Thread States",
        processor,
        trace_path,
        "SELECT t.name, ts.state, ROUND(SUM(ts.dur) / 1000000.0, 3) AS state_ms, "
        "COUNT(*) AS slices "
        "FROM thread_state ts JOIN thread t USING (utid) JOIN process p USING (upid) "
        f"WHERE {process_filter} GROUP BY t.name, ts.state ORDER BY state_ms DESC;",
    ))
    report.extend([
        "## Interpretation Boundary",
        "",
        "- `Frame Jank` is authoritative only when `android_frame_stats` is available.",
        "- `FrameTimeline Jank Types` is authoritative when the actual FrameTimeline table is available.",
        "- A zero jank count is meaningful only when that query succeeds and returns frames.",
        "- CPU and slice rows are evidence for follow-up; attribute a stall only after checking "
        "the matching thread-state and dependency window.",
        "",
    ])
    return "\n".join(report)


def main():
    args = parse_args()
    archive = args.diagnostic_zip.resolve()
    if not archive.is_file():
        raise SystemExit(f"Diagnostic ZIP does not exist: {archive}")

    with zipfile.ZipFile(archive) as source:
        if TRACE_ENTRY not in source.namelist():
            raise SystemExit(f"Diagnostic ZIP does not contain {TRACE_ENTRY}")
        diagnostic = source.read(DIAGNOSTIC_ENTRY).decode("utf-8", errors="replace")
        with tempfile.TemporaryDirectory(prefix="dpis-perfetto-") as temp_dir:
            trace_path = Path(temp_dir) / TRACE_ENTRY
            trace_path.write_bytes(source.read(TRACE_ENTRY))
            processor = ensure_trace_processor(args.trace_processor)
            report = build_report(archive, diagnostic, trace_path, processor)

    if args.output:
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(report, encoding="utf-8")
        print(f"Wrote {args.output}")
    else:
        print(report)


if __name__ == "__main__":
    main()
