# LSPosed Diagnostics

This document describes how to pull and filter LSPosed module logs. It applies
to both `modern` and `legacy` flavors — both are LSPosed modules and write
to the same log directory.

## Log Sources

LSPosed module logs are the primary diagnostic source for proving whether a
module entry, app-process hook, or system_server hook actually executed.
`logcat` can show forwarded `LSPosedFramework` lines for cross-checking, but
absence in plain `logcat` is not a reliable negative signal — the module log
may contain entries that were never forwarded.

Primary log files on device:

- `/data/adb/lspd/log/modules_*.log` — module-level lifecycle and hook
  installation logs
- `/data/adb/lspd/log/verbose_*.log` — verbose hook callback and parameter
  logs

Both require root to read.

## In-App Log Page

The DPIS log page treats LSPosed logs as an external evidence source. Opening
the page and automatic refresh read only the DPIS local diagnostic log store.
When the user manually refreshes the LSPosed page, DPIS reads the complete
current `modules_*.log` and `verbose_*.log` files, then filters them in the app
to `io.github.kwensiu.dpis` entries. DPIS also keeps LSPosedService hot-reload
warnings that explicitly mention `io.github.kwensiu.dpis`, because those lines
are framework outcomes rather than module-emitted entries. Rotated historical
LSPosed files are not read by the normal in-app page.

## Feedback Diagnostics

Feedback diagnostic exports keep raw LSPosed evidence in `lsposed-log.txt`.
During export DPIS also parses DPIS-related LSPosed lines whose timestamps fall
inside the diagnostic session window and writes only structured timeline events
to `diagnostic.txt` with `source=lsposed-log`. Lines whose timestamps cannot be
parsed remain in `lsposed-log.txt` but are not promoted into the structured
timeline. LSPosedService hot-reload warning lines are promoted as
`route=hot_reload stage=skipped` when they match the diagnostic target window.

When diagnostic capture is active, selected runtime hook boundaries may also
emit `DPIS_DIAG_HOTPATH route=<...> stage=<...> routeName=<...>` lines into the
same LSPosed window. These are still auxiliary evidence, but they let the
exported timeline show specific callback/skip/apply points for hot runtime
routes without relying on post-hoc text classification alone.

Feedback diagnostics also aggregate hot-path performance measurements in memory
for the duration of the session. The export includes calls, applied/skipped
counts, skip-reason counts, and latency percentiles (`p50Us`, `p95Us`, `p99Us`,
and `maxUs`) under `[performance-summary]`. This first-stage capture is
intentionally broad; it does not write one log line per callback. Perfetto
trace capture is a separate follow-up layer and should be correlated with
these counters before adding tighter sampling or thresholds.

High-frequency `DPIS_DIAG_HOTPATH` bridge lines are dispatched asynchronously
from the injected process. This keeps LSPosed file logging off the hooked
callback thread while preserving detailed bridge evidence during normal short
diagnostic sessions. The queue is bounded; an overflow is an evidence
completeness warning, not proof that the route was inactive.

## Pull Path

Replace `<local-temp-dir>` with a writable local directory:

```powershell
adb shell su -c "ls -la /data/adb/lspd/log /data/adb/lspd/log.old"
$out = "<local-temp-dir>"
New-Item -ItemType Directory -Force -Path $out | Out-Null
adb exec-out su -c "cat /data/adb/lspd/log/<modules_current>.log" > "$out\modules_current.log"
adb exec-out su -c "cat /data/adb/lspd/log/<verbose_current>.log" > "$out\verbose_current.log"
```

Replace `<modules_current>` and `<verbose_current>` with the actual filenames
from the `ls` output.

On Windows, LSPosed filenames contain `:` characters and cannot always be
pulled directly with `adb pull`. Prefer the repository helper when collecting
current module logs:

```powershell
.\scripts\pull-lsposed-logs.ps1 -Device 192.168.5.130:5555
```

`-Device` is optional. When omitted, the helper uses the default `adb`
transport and reads the newest `modules_*.log` and `verbose_*.log` files
through `adb exec-out`, writes them with Windows-safe filenames, strips NUL
bytes for text filtering, and emits `summary.txt` with the last DPIS /
hot-reload matches.

Useful filters:

```powershell
.\scripts\pull-lsposed-logs.ps1 -Pattern 'io.github.kwensiu.dpis|DPIS|status='
.\scripts\pull-lsposed-logs.ps1 -Pattern 'com.salt.music|DPIS'
```

## API 102 Capability Evidence

The single Modern artifact targets API 102 but declares `minApiVersion=101`.
It also declares `autoHotReload=true`: API 102 hosts can use LSPosed automatic
hot reload, while API 101 hosts keep the normal install-and-restart path.

For API 102 hot reload, LSPosed bridge logs are the primary evidence. A
module-side reload reached DPIS only when the current `modules_*.log` or
`verbose_*.log` contains `DPIS hot reload begin`, `DPIS hot reload replay`,
and `DPIS hot reload end`.

Stable hook ids are a separate runtime enhancement. Validate them by checking
normal DPIS hook-install logs and route effects; the hook id path must still
fall back to the original builder on API 101 hosts.

If LSPosed still reports that DPIS requires API 102, inspect the installed APK's
`META-INF/xposed/module.prop` first. `minApiVersion=102` means the installed
artifact is stale or built from an older API 102-only metadata revision.

## Encoding

If PowerShell `Select-String` prints one-character or garbled output, the log
may contain NUL bytes. Strip them and decode locally before filtering:

```powershell
$bytes = [System.IO.File]::ReadAllBytes("$out\modules_current.log")
$text = [System.Text.Encoding]::UTF8.GetString($bytes.Where({$_ -ne 0}))
$text | Select-String -Pattern '<your-probe-pattern>'
```
