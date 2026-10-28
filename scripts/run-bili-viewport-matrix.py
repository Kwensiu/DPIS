#!/usr/bin/env python3
"""Run a repeatable Bilibili viewport route matrix on an Android device.

The debug DPIS APK must be installed. This runner changes the target width through
DPIS_DEBUG_SET_PACKAGE_CONFIG, cold-starts Bilibili, executes the fixed gesture
workload, captures a scheduler/input Perfetto trace, and writes a CSV summary.
It intentionally does not claim to reproduce a human's exact swipe velocity; the
coordinates and duration are explicit so runs can be compared consistently.
"""
from __future__ import annotations

import argparse
import csv
import io
import json
import re
import subprocess
import sys
import time
from dataclasses import dataclass
from pathlib import Path

ACTION = "io.github.kwensiu.dpis.DEBUG_SET_PACKAGE_CONFIG"
DEBUG_RECEIVER = "io.github.kwensiu.dpis/com.dpis.module.DebugConfigBroadcastReceiver"
DEFAULT_PACKAGE = "tv.danmaku.bili"
TRACE_CONFIG = "dpis-matrix-config.pbtxt"
REMOTE_CONFIG = "/data/local/tmp/dpis-matrix-config.pbtxt"
REMOTE_TRACE = "/data/local/tmp/dpis-matrix-trace.pftrace"
REMOTE_PID = "/data/local/tmp/dpis-matrix-trace.pid"
REMOTE_ERROR = "/data/local/tmp/dpis-matrix-trace.error"
TRACE_PROCESSOR = Path(__file__).resolve().parents[1] / "trace_processor"


@dataclass(frozen=True)
class Case:
    name: str
    mode: str
    percent: int | None


CASES = [
    Case("off", "off", None),
    Case("system-128", "system", 128),
    Case("compat-101", "compat", 101),
    Case("compat-110", "compat", 110),
    Case("compat-120", "compat", 120),
    Case("compat-128", "compat", 128),
    Case("auto-101", "auto", 101),
    Case("auto-110", "auto", 110),
    Case("auto-120", "auto", 120),
    Case("auto-128", "auto", 128),
]


def run(command: list[str], *, check: bool = True, timeout: float | None = None) -> str:
    completed = subprocess.run(
        command,
        check=False,
        capture_output=True,
        text=True,
        encoding="utf-8",
        errors="replace",
        timeout=timeout,
    )
    if check and completed.returncode != 0:
        raise RuntimeError(
            f"command failed ({completed.returncode}): {' '.join(command)}\n"
            f"stdout: {completed.stdout[-1000:]}\nstderr: {completed.stderr[-1000:]}"
        )
    return completed.stdout.strip()


def adb(args: list[str], *, check: bool = True, timeout: float | None = None) -> str:
    return run(["adb", *args], check=check, timeout=timeout)


def shell(command: str, *, check: bool = True, timeout: float | None = None) -> str:
    return adb(["shell", "sh", "-c", command], check=check, timeout=timeout)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--device", help="ADB serial")
    parser.add_argument("--package", default=DEFAULT_PACKAGE)
    parser.add_argument("--output", type=Path, default=Path(".tmp/bili-viewport-matrix"))
    parser.add_argument("--base-width", type=int, default=360)
    parser.add_argument("--repeat", type=int, default=1)
    parser.add_argument("--wait-after-launch", type=float, default=10.0)
    parser.add_argument("--swipe-duration-ms", type=int, default=180)
    parser.add_argument("--settle-ms", type=int, default=180)
    parser.add_argument("--skip-trace", action="store_true")
    parser.add_argument("--cases", help="Comma-separated case names; default is the full matrix")
    return parser.parse_args()


def adb_prefix(args: argparse.Namespace) -> list[str]:
    return ["-s", args.device] if args.device else []


def adb_for(args: argparse.Namespace, values: list[str], **kwargs) -> str:
    return run(["adb", *adb_prefix(args), *values], **kwargs)


def shell_for(args: argparse.Namespace, command: str, **kwargs) -> str:
    return adb_for(args, ["shell", "sh", "-c", command], **kwargs)


def resolve_launcher(args: argparse.Namespace, package: str) -> str:
    output = adb_for(
        args,
        ["shell", "cmd", "package", "resolve-activity", "--brief", "-a",
         "android.intent.action.MAIN", "-c", "android.intent.category.LAUNCHER", package],
    )
    components = [line.strip() for line in output.splitlines() if "/" in line]
    if not components:
        raise RuntimeError(f"cannot resolve launcher for {package}: {output}")
    return components[-1]


def width_for(case: Case, base_width: int) -> int:
    if case.percent is None:
        return 0
    return max(1, round(base_width * case.percent / 100.0))


def set_config(args: argparse.Namespace, case: Case, width: int) -> None:
    values = [
        "shell", "am", "broadcast", "--user", "0", "-n", DEBUG_RECEIVER,
        "-a", ACTION,
        "--es", "package", args.package,
        "--es", "viewport_mode", case.mode,
    ]
    if case.percent is not None:
        values.extend([
            "--ei", "relative_scale_milli_percent", str(case.percent * 1000),
        ])
    else:
        values.extend(["--ei", "viewport_width_dp", str(width)])
    output = adb_for(args, values)
    if "permission denial" in output.lower() or "securityexception" in output.lower():
        raise RuntimeError(f"configuration broadcast failed: {output}")
    time.sleep(2.0)


def force_start(args: argparse.Namespace, launcher: str) -> str:
    adb_for(args, ["shell", "am", "force-stop", args.package])
    time.sleep(0.5)
    adb_for(args, ["shell", "am", "start", "-n", launcher])
    for _ in range(20):
        pids = adb_for(args, ["shell", "pidof", args.package], check=False).strip().split()
        if pids:
            return pids[-1]
        time.sleep(0.5)
    raise RuntimeError(f"target process did not start: {args.package}")


def swipe(args: argparse.Namespace, x1: int, y1: int, x2: int, y2: int) -> None:
    adb_for(
        args,
        ["shell", "input", "swipe", str(x1), str(y1), str(x2), str(y2),
         str(args.swipe_duration_ms)],
    )
    time.sleep(args.settle_ms / 1000.0)


def run_workload(args: argparse.Namespace) -> None:
    size = adb_for(args, ["shell", "wm", "size"])
    match = re.search(r"Physical size: (\d+)x(\d+)", size)
    screen_w, screen_h = (1080, 2400) if not match else (int(match.group(1)), int(match.group(2)))
    sx = screen_w / 1080.0
    sy = screen_h / 2400.0
    def point(x: int, y: int) -> tuple[int, int]:
        return round(x * sx), round(y * sy)

    up_start = point(540, 1900)
    up_end = point(540, 650)
    right_start = point(180, 1200)
    right_end = point(900, 1200)
    left_start = point(900, 1200)
    left_end = point(180, 1200)
    down_start = point(540, 650)
    down_end = point(540, 1900)

    time.sleep(args.wait_after_launch)
    for _ in range(5):
        swipe(args, *up_start, *up_end)
    swipe(args, *right_start, *right_end)
    for _ in range(5):
        swipe(args, *up_start, *up_end)
    for _ in range(2):
        swipe(args, *left_start, *left_end)
    for _ in range(5):
        swipe(args, *up_start, *up_end)


def start_trace(args: argparse.Namespace, duration_ms: int) -> subprocess.Popen[str]:
    config = (
        "buffers { size_kb: 131072 fill_policy: DISCARD }\n"
        f"duration_ms: {duration_ms}\n"
        "data_sources { config { name: \"linux.ftrace\" ftrace_config { "
        "ftrace_events: \"sched/sched_switch\" "
        "ftrace_events: \"sched/sched_wakeup\" "
        "ftrace_events: \"sched/sched_waking\" "
        "atrace_categories: \"input\" atrace_categories: \"gfx\" } } }\n"
        "data_sources { config { name: \"android.surfaceflinger.frametimeline\" } }\n"
    )
    local_config = args.output / TRACE_CONFIG
    local_config.write_text(config, encoding="utf-8")
    adb_for(args, ["push", str(local_config), REMOTE_CONFIG])
    adb_for(args, ["shell", "rm", "-f", REMOTE_TRACE, REMOTE_PID, REMOTE_ERROR])
    return subprocess.Popen(
        ["adb", *adb_prefix(args), "shell", "su", "-c",
         f"perfetto --background-wait --txt -c {REMOTE_CONFIG} -o {REMOTE_TRACE} >{REMOTE_PID} 2>{REMOTE_ERROR}"],
        stdout=subprocess.DEVNULL,
        stderr=subprocess.PIPE,
        text=True,
    )


def stop_trace(args: argparse.Namespace, process: subprocess.Popen[str] | None) -> None:
    shell_for(
        args,
        f"if [ -s {REMOTE_PID} ]; then kill -INT $(cat {REMOTE_PID}) 2>/dev/null || true; fi; "
        f"i=0; while [ $i -lt 40 ] && [ -s {REMOTE_PID} ] && kill -0 $(cat {REMOTE_PID}) 2>/dev/null; "
        f"do i=$((i+1)); sleep 0.1; done",
        check=False,
        timeout=20,
    )
    if process is not None:
        try:
            process.wait(timeout=5)
        except subprocess.TimeoutExpired:
            process.terminate()
            process.wait(timeout=5)
    for _ in range(20):
        if shell_for(args, f"test -s {REMOTE_TRACE}", check=False).strip() == "":
            time.sleep(0.25)
            continue
        break


def pull_trace(args: argparse.Namespace, destination: Path) -> None:
    destination.parent.mkdir(parents=True, exist_ok=True)
    time.sleep(8.0)
    command = ["adb", *adb_prefix(args), "exec-out", "su", "-c", f"cat {REMOTE_TRACE}"]
    for attempt in range(20):
        with destination.open("wb") as handle:
            completed = subprocess.run(
                command,
                stdout=handle,
                stderr=subprocess.PIPE,
                check=False,
                timeout=180,
            )
        if completed.returncode == 0 and destination.stat().st_size > 0:
            return
        if attempt < 19:
            time.sleep(1.0)
    raise RuntimeError(
        f"failed to read trace: {completed.stderr.decode('utf-8', errors='replace')}"
    )


def query(processor: Path, trace: Path, sql: str) -> list[list[str]]:
    output = run([sys.executable, str(processor), "--query-string", sql, str(trace)], timeout=180)
    return list(csv.reader(io.StringIO(output)))


def summarize_trace(args: argparse.Namespace, trace: Path, package: str, target_pid: str) -> dict[str, str]:

    processor = TRACE_PROCESSOR
    if not processor.exists():
        return {"trace_error": "repo trace_processor missing"}
    process_rows = query(processor, trace, f"SELECT upid,pid FROM process WHERE pid={target_pid} LIMIT 1;")

    if len(process_rows) < 2:
        return {"trace_error": "target process not found"}
    upid, pid = process_rows[1]
    thread_rows = query(processor, trace, f"SELECT utid FROM thread WHERE upid={upid} AND tid={pid} LIMIT 1;")
    if len(thread_rows) < 2:
        return {"trace_error": "main thread not found"}
    utid = thread_rows[1][0]
    input_rows = query(processor, trace, f"SELECT MAX(dur)/1000000.0, COUNT(*) FROM slice s JOIN thread_track tt ON s.track_id=tt.id WHERE tt.utid={utid} AND s.name GLOB 'dispatchInputEvent*';")
    state_rows = query(processor, trace, f"SELECT state,SUM(IIF(dur=-1,trace_end()-ts,dur))/1000000.0 FROM thread_state WHERE utid={utid} GROUP BY state;")
    states = {row[0]: row[1] for row in state_rows[1:]}
    result = {
        "pid": pid,
        "max_input_ms": input_rows[1][0] if len(input_rows) > 1 else "",
        "input_count": input_rows[1][1] if len(input_rows) > 1 else "",
        "running_ms": states.get("Running", "0"),
        "runnable_ms": states.get("R", "0"),
        "runnable_plus_ms": states.get("R+", "0"),
        "blocked_d_ms": states.get("D", "0"),
    }
    return result


def main() -> int:
    args = parse_args()
    args.output.mkdir(parents=True, exist_ok=True)
    launcher = resolve_launcher(args, args.package)
    selected = {case.name for case in CASES}
    if args.cases:
        selected = set(args.cases.split(","))
    cases = [case for case in CASES if case.name in selected]
    if not cases:
        raise SystemExit("no cases selected")
    rows: list[dict[str, str]] = []
    for repeat in range(1, args.repeat + 1):
        for case in cases:
            run_name = f"{repeat:02d}-{case.name}"
            run_dir = args.output / run_name
            run_dir.mkdir(parents=True, exist_ok=True)
            width = width_for(case, args.base_width)
            print(f"[{run_name}] configure mode={case.mode} widthDp={width}", flush=True)
            set_config(args, case, width)
            target_pid = force_start(args, launcher)

            trace_process = None
            try:
                if not args.skip_trace:
                    trace_process = start_trace(args, 45_000)
                    time.sleep(1.0)
                run_workload(args)
                if trace_process is not None:
                    stop_trace(args, trace_process)
                trace_path = run_dir / "perfetto-trace.pftrace"
                if not args.skip_trace:
                    pull_trace(args, trace_path)
                    metrics = summarize_trace(args, trace_path, args.package, target_pid)

                else:
                    metrics = {"trace_error": "skipped"}
            finally:
                if trace_process is not None:
                    stop_trace(args, trace_process)
            row = {"case": case.name, "mode": case.mode, "percent": str(case.percent or "off"), "width_dp": str(width), "repeat": str(repeat), **metrics}
            rows.append(row)
            print(json.dumps(row, ensure_ascii=False), flush=True)
    fields = sorted({key for row in rows for key in row})
    with (args.output / "results.csv").open("w", newline="", encoding="utf-8") as handle:
        writer = csv.DictWriter(handle, fieldnames=fields)
        writer.writeheader()
        writer.writerows(rows)
    print(f"wrote {args.output / 'results.csv'}")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except KeyboardInterrupt:
        raise SystemExit("interrupted")
    except Exception as error:
        print(f"error: {error}", file=sys.stderr)
        raise SystemExit(1)
