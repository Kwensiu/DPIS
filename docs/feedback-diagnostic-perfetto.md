# Feedback Diagnostic Perfetto Analysis

Feedback diagnostic ZIPs can include `perfetto-trace.pftrace`. Analyze one with:

```bash
python scripts/analyze-feedback-diagnostic.py /path/to/dpis-diagnostic.zip \
  --output /path/to/report.md
```

The script downloads Perfetto's official `trace_processor` wrapper to the repository
root on its first run. That wrapper is ignored by Git. Pass `--trace-processor` to use
an existing wrapper instead.

The report includes the exported trace metadata, DPIS font aggregates, target process
discovery, frame jank when the trace contains `android_frame_stats`, target CPU
scheduling, and long Atrace slices. A missing frame-stat table is reported as
unavailable; it must not be interpreted as zero jank.

The current DPIS trace preset includes process statistics, task lifecycle events,
scheduling, selected Atrace categories, and SurfaceFlinger FrameTimeline. Older ZIPs
may still lack those tables. When the report shows no target process rows or no
FrameTimeline data, use it to verify capture coverage first before making a jank or
per-thread attribution claim.
