#!/usr/bin/env python3
"""Read a DPIS diagnostic ZIP or extracted directory.

The report answers the questions that a WeChat font/DPI pack actually needs:

- last ProcessPerformance aggregate per process (not the first snapshot)
- WeChat DPI hot-path evidence versus a module-effects "no effect" filler
- applied in=/out= pairs, including current_px_fallback rows that omit them
- timeline applied counts versus the last aggregate
- second-order *candidates*: incoming px equal to a first-scale output

A candidate is not a double-scale bug. Independent later sizes, including
another object's unscaled 28.2, must still rewrite. Do not treat
|incoming - earlier out| < epsilon (29.0 vs 29.14) as a match.
"""
from __future__ import annotations

import argparse
import collections
import io
import re
import sys
import zipfile
from dataclasses import dataclass
from pathlib import Path

KV = re.compile(r"([A-Za-z][A-Za-z0-9_]*)=([^,]+)")
ROUND_DP = 2


@dataclass
class AppliedRow:
    time: str
    route: str
    incoming: float | None
    outgoing: float | None
    view: str
    raw: str


@dataclass
class Pack:
    root_name: str
    files: dict[str, str]


def load_pack(path: Path) -> Pack:
    if path.is_file() and zipfile.is_zipfile(path):
        with zipfile.ZipFile(path) as zf:
            files = {
                name: zf.read(name).decode("utf-8", errors="replace")
                for name in zf.namelist()
                if not name.endswith("/")
            }
        return Pack(path.name, files)
    if path.is_dir():
        files = {
            child.name: child.read_text(encoding="utf-8", errors="replace")
            for child in path.iterdir()
            if child.is_file()
        }
        return Pack(path.name, files)
    raise SystemExit(f"not a diagnostic zip or directory: {path}")


def section(text: str, start: str, end: str | None = None) -> str:
    begin = text.find(start)
    if begin < 0:
        return ""
    begin += len(start)
    stop = text.find(end, begin) if end else len(text)
    if stop < 0:
        stop = len(text)
    return text[begin:stop]


def heading_section(text: str, start: str) -> str:
    body = section(text, start)
    lines: list[str] = []
    for line in body.splitlines():
        if line.startswith("[") and line.endswith("]"):
            break
        lines.append(line)
    return "\n".join(lines)


def token(line: str, name: str) -> str:
    match = re.search(rf"(?:^|\s){re.escape(name)}=(\S+)", line)
    return match.group(1) if match else ""


def kv_map(message: str) -> dict[str, str]:
    return {match.group(1): match.group(2).strip() for match in KV.finditer(message)}


def timeline_lines(pack: Pack) -> list[str]:
    diagnostic = pack.files.get("diagnostic.txt", "")
    body = section(diagnostic, "[runtime-timeline]\n", "[runtime-self-test]")
    if not body.strip():
        body = section(diagnostic, "[runtime-timeline]\n", "[raw-log]")
    lines = [line for line in body.splitlines() if line.strip()]
    if lines:
        return lines
    tsv = pack.files.get("timeline.tsv", "")
    parsed: list[str] = []
    for row in tsv.splitlines()[1:]:
        cols = row.split("\t")
        if len(cols) < 9:
            continue
        parsed.append(
            f"{cols[0]} source={cols[1]} category={cols[2]} route={cols[3]} "
            f"stage={cols[5]} routeName={cols[4]} process={cols[6]} "
            f"package={cols[7]} message={cols[8]}"
        )
    return parsed


def message_of(line: str) -> str:
    idx = line.find("message=")
    if idx >= 0:
        return line[idx + 8 :]
    idx = line.find("detail=")
    return line[idx + 7 :] if idx >= 0 else ""


def parse_applied(lines: list[str]) -> list[AppliedRow]:
    rows: list[AppliedRow] = []
    for line in lines:
        if "stage=applied" not in line:
            continue
        route = token(line, "routeName") or token(line, "route")
        msg = message_of(line)
        fields = kv_map(msg)
        incoming = _float(fields.get("in"))
        outgoing = _float(fields.get("out"))
        rows.append(
            AppliedRow(
                time=line[:18],
                route=route,
                incoming=incoming,
                outgoing=outgoing,
                view=fields.get("view") or fields.get("paint") or "",
                raw=line,
            )
        )
    return rows


def _float(value: str | None) -> float | None:
    if value is None:
        return None
    try:
        return float(value)
    except ValueError:
        return None


def last_aggregates(lines: list[str]) -> dict[str, tuple[str, str]]:
    by_process: dict[str, tuple[str, str]] = {}
    for line in lines:
        if "stage=aggregate" not in line or "category=performance" not in line:
            continue
        msg = message_of(line)
        match = re.search(r"process=([^,;]+)", msg)
        process = match.group(1) if match else "unknown"
        by_process[process] = (line[:18], msg)
    return by_process


def parse_route_counts(aggregate_message: str) -> dict[str, dict[str, int]]:
    counts: dict[str, dict[str, int]] = {}
    for part in aggregate_message.split(";"):
        part = part.strip()
        if not part.startswith("route="):
            continue
        body = part[len("route=") :]
        fields = body.split(",")
        if not fields:
            continue
        route = fields[0]
        parsed = {"calls": 0, "applied": 0, "skipped": 0, "kept": 0}
        for field in fields[1:]:
            name, _, value = field.partition("=")
            if name in parsed:
                try:
                    parsed[name] = int(value)
                except ValueError:
                    pass
        counts[route] = parsed
    return counts


def infer_factor(rows: list[AppliedRow], fallback: float) -> float:
    for row in rows:
        fields = kv_map(message_of(row.raw))
        factor = _float(fields.get("factor"))
        if factor is not None and 0 < factor < 1.5 and factor != 1.0:
            return factor
    return fallback


def second_order_candidates(rows: list[AppliedRow]) -> list[AppliedRow]:
    """Incoming equals a *first-scale output*, not merely near a previous out."""
    scaled_outs: set[float] = set()
    for row in rows:
        if row.incoming is None or row.outgoing is None:
            continue
        if abs(row.incoming - row.outgoing) < 1e-6:
            continue
        scaled_outs.add(round(row.outgoing, ROUND_DP))
    candidates: list[AppliedRow] = []
    for row in rows:
        if row.incoming is None:
            continue
        if round(row.incoming, ROUND_DP) in scaled_outs:
            candidates.append(row)
    return candidates


def wechat_events(lines: list[str]) -> list[str]:
    return [
        line
        for line in lines
        if "route=wechat_dpi" in line or "routeName=wechat_dpi" in line
    ]


def report(pack: Pack, factor_fallback: float) -> str:
    out = io.StringIO()
    lines = timeline_lines(pack)
    applied = parse_applied(lines)
    factor = infer_factor(applied, factor_fallback)
    module_effects = pack.files.get("module-effects.tsv", "")
    diagnostic = pack.files.get("diagnostic.txt", "")

    print(f"pack: {pack.root_name}", file=out)
    print(f"timeline events: {len(lines)}", file=out)
    print(f"applied with in/out: {sum(1 for row in applied if row.incoming is not None)}", file=out)
    print(f"applied missing in/out: {sum(1 for row in applied if row.incoming is None)}", file=out)
    print(f"factor used for notes: {factor}", file=out)

    plan = heading_section(diagnostic, "[diagnostic-plan]\n")
    if plan.strip():
        print("\n== diagnostic plan ==", file=out)
        print(plan.strip(), file=out)

    wechat_section = heading_section(diagnostic, "[wechat-dpi-evidence]\n")
    if wechat_section.strip():
        print("\n== wechat-dpi-evidence ==", file=out)
        print(wechat_section.strip(), file=out)

    print("\n== last performance aggregate per process ==", file=out)
    aggregates = last_aggregates(lines)
    if not aggregates:
        print("none", file=out)
    last_counts: dict[str, dict[str, int]] = {}
    primary_process = ""
    primary_applied = -1
    for process, (time, msg) in aggregates.items():
        print(f"{time} {process}", file=out)
        counts = parse_route_counts(msg)
        applied_total = sum(route.get("applied", 0) for route in counts.values())
        if applied_total > primary_applied:
            primary_applied = applied_total
            primary_process = process
            last_counts = counts
        for part in msg.split(";"):
            print(f"  {part[:220]}", file=out)

    print("\n== wechat vs module-effects ==", file=out)
    wechat = wechat_events(lines)
    filler = "selected but no WeChat DPI route effect observed"
    has_filler = filler in module_effects
    print(f"wechat_dpi timeline events: {len(wechat)}", file=out)
    print(f"module-effects filler present: {has_filler}", file=out)
    if wechat and has_filler:
        print(
            "CONTRADICTION: WeChat DPI hot-path evidence exists, but "
            "module-effects still claims no route effect. Observed modules "
            "must include timeline route=wechat_dpi, not only ProcessPerformance "
            "aggregates.",
            file=out,
        )
    elif wechat:
        print("ok: WeChat evidence present and filler absent.", file=out)
    elif has_filler:
        print("ok: filler matches missing WeChat timeline evidence.", file=out)
    else:
        print("no WeChat selection evidence in this pack.", file=out)
    for line in wechat[:12]:
        print(f"  {line[:240]}", file=out)

    print("\n== current_px_fallback in/out ==", file=out)
    current_px = [row for row in applied if row.route == "textview_current_px_fallback"]
    missing = [row for row in current_px if row.incoming is None]
    print(f"timeline applied: {len(current_px)}", file=out)
    print(f"timeline applied missing in=/out=: {len(missing)}", file=out)
    agg_applied = last_counts.get("textview_current_px_fallback", {}).get("applied", 0)
    if agg_applied:
        print(
            f"last aggregate applied: {agg_applied}; "
            f"timeline/aggregate gap: {agg_applied - len(current_px)}",
            file=out,
        )
    if missing:
        print(
            "current_px applied rows should carry in=/out= like SP/absolute "
            "rewrites; begin/end must keep the same detail string.",
            file=out,
        )

    print("\n== applied incoming histogram (in/out present) ==", file=out)
    sized = [row for row in applied if row.incoming is not None]
    incoming_hist = collections.Counter(round(row.incoming or 0.0, ROUND_DP) for row in sized)
    for value, count in incoming_hist.most_common(20):
        print(f"  in={value:8.2f}  n={count}", file=out)

    print("\n== second-order candidates (incoming == first-scale output) ==", file=out)
    candidates = second_order_candidates(sized)
    print(f"count: {len(candidates)}", file=out)
    print(
        "These are value-space observations only. Same-object already-scaled "
        "Paint must not re-multiply; an independent later size still must.",
        file=out,
    )
    grouped = collections.Counter(
        (
            round(row.incoming or 0.0, ROUND_DP),
            round(row.outgoing or 0.0, ROUND_DP) if row.outgoing is not None else None,
            row.route,
        )
        for row in candidates
    )
    for key, count in grouped.most_common(20):
        print(f"  n={count:4d} {key}", file=out)

    print("\n== timeline applied vs last aggregate ==", file=out)
    if primary_process:
        print(f"primary process: {primary_process}", file=out)
    timeline_applied = collections.Counter(row.route for row in applied)
    routes = sorted(set(timeline_applied) | set(last_counts))
    if not routes:
        print("none", file=out)
    for route in routes:
        timeline_n = timeline_applied.get(route, 0)
        agg_n = last_counts.get(route, {}).get("applied", 0)
        if timeline_n == 0 and agg_n == 0:
            continue
        print(f"  {route}: timeline={timeline_n} aggregate={agg_n}", file=out)

    return out.getvalue()


def self_check() -> int:
    synthetic = Pack(
        "self-check",
        {
            "diagnostic.txt": "\n".join(
                [
                    "[diagnostic-plan]",
                    "wechatDpiRoute: selected (targetDpi=380)",
                    "[runtime-summary]",
                    "[wechat-dpi-evidence]",
                    "displayMetrics: mutation applied",
                    "[runtime-timeline]",
                    "11-15 06:13:20.100 source=runtime-transport category=performance "
                    "route=runtime stage=aggregate package=com.tencent.mm "
                    "message=process=com.tencent.mm,pid=1;"
                    "route=textview_current_px_fallback,calls=10,applied=8,skipped=0,kept=2",
                    "11-15 06:13:20.200 source=runtime-hotpath category=runtime "
                    "route=font stage=applied routeName=textview_absolute_rewrite "
                    "message=view=TextView, in=29.0, out=27.26, factor=0.94",
                    "11-15 06:13:20.300 source=runtime-hotpath category=runtime "
                    "route=font stage=applied routeName=textview_current_px_fallback "
                    "message=view=TextView, factor=0.94, percent=94",
                    "11-15 06:13:20.400 source=runtime-hotpath category=runtime "
                    "route=wechat_dpi stage=mutation_applied routeName=displaymetrics "
                    "message=densityDpi=480->380",
                    "[runtime-self-test]",
                    "lsposedHotpathProbe: found",
                ]
            )
            + "\n",
            "module-effects.tsv": (
                "source\tprocess\tpid\tmodule\troute\tcalls\tapplied\tskipped\tkept\t"
                "measuredCalls\tp50Us\tp95Us\tp99Us\tmaxUs\tnote\n"
                "diagnostic-plan\tunknown\tunknown\twechat_dpi\twechat_dpi\t0\t0\t0\t0"
                "\t0\t0\t0\t0\t0\tselected but no WeChat DPI route effect observed\n"
            ),
        },
    )
    text = report(synthetic, 0.94)
    required = [
        "CONTRADICTION",
        "timeline applied missing in=/out=: 1",
        "count: 0",
        "textview_current_px_fallback: timeline=1 aggregate=8",
    ]
    failed = [item for item in required if item not in text]
    if failed:
        sys.stderr.write("self-check missing:\n" + "\n".join(failed) + "\n\n" + text)
        return 1
    # 29.0 vs a later 29.14 must not count as second-order.
    near = Pack(
        "near",
        {
            "diagnostic.txt": "\n".join(
                [
                    "[runtime-timeline]",
                    "11-15 06:13:20.200 source=runtime-hotpath category=runtime "
                    "route=font stage=applied routeName=textview_absolute_rewrite "
                    "message=in=31.0, out=29.14, factor=0.94",
                    "11-15 06:13:20.300 source=runtime-hotpath category=runtime "
                    "route=font stage=applied routeName=paint_text_size_fallback "
                    "message=in=29.0, out=27.26, factor=0.94",
                    "[runtime-self-test]",
                ]
            )
            + "\n",
            "module-effects.tsv": "",
        },
    )
    near_text = report(near, 0.94)
    if "count: 0" not in near_text:
        sys.stderr.write("epsilon near-miss was treated as second-order\n" + near_text)
        return 1
    sys.stdout.write("self-check ok\n")
    return 0


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "path",
        nargs="?",
        type=Path,
        help="diagnostic zip or extracted directory",
    )
    parser.add_argument(
        "--factor",
        type=float,
        default=0.94,
        help="fallback scale factor when events do not include factor=",
    )
    parser.add_argument(
        "--self-check",
        action="store_true",
        help="run synthetic packing checks and exit",
    )
    args = parser.parse_args(argv)
    if args.self_check:
        return self_check()
    if args.path is None:
        parser.error("path is required unless --self-check is set")
    pack = load_pack(args.path)
    sys.stdout.write(report(pack, args.factor))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
