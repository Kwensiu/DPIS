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
to `io.github.kwensiu.dpis` entries. Rotated historical LSPosed files are not
read by the normal in-app page.

## Feedback Diagnostics

Feedback diagnostic exports keep raw LSPosed evidence in `lsposed-log.txt`.
During export DPIS also parses DPIS-related LSPosed lines whose timestamps fall
inside the diagnostic session window and writes only structured timeline events
to `diagnostic.txt` with `source=lsposed-log`. Lines whose timestamps cannot be
parsed remain in `lsposed-log.txt` but are not promoted into the structured
timeline.

When diagnostic capture is active, selected runtime hook boundaries may also
emit `DPIS_DIAG_HOTPATH route=<...> stage=<...> routeName=<...>` lines into the
same LSPosed window. These are still auxiliary evidence, but they let the
exported timeline show specific callback/skip/apply points for hot runtime
routes without relying on post-hoc text classification alone.

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
.\scripts\pull-lsposed-logs.ps1 -Pattern 'io.github.kwensiu.dpis|DPIS hot reload|Auto hot reload|status='
.\scripts\pull-lsposed-logs.ps1 -Pattern 'com.salt.music|DPIS|Auto hot reload'
```

## Hot Reload Evidence

For API 102 Modern builds, LSPosed bridge logs are the primary evidence for
hot reload. A module-side reload reached DPIS only when the current
`modules_*.log` or `verbose_*.log` contains:

- `DPIS hot reload begin`
- `DPIS hot reload replay`
- `DPIS hot reload end`

`Auto hot reload failed ... status=3, message=null` without a matching
`DPIS hot reload begin` means LSPosed did not reach DPIS's reload callback.
The common first-update case is an already-running target process that still
holds an older DPIS generation whose default `onHotReloading()` rejects reload.
Restart that target process once after installing the 102-capable build, then
use the next install/update to validate the hot-reload path.

`Auto hot reload failed ... status=4, message=null` has been observed when a
target process is being stopped or has stale process state during the reload
window. Treat it as framework/process-lifetime evidence first: confirm the
current PID with `adb shell pidof <package>`, relaunch the target, and validate
again from fresh LSPosed logs before blaming DPIS hook replay.

Feedback diagnostics can include hot-reload-related events when the diagnostic
session survives long enough, but reinstall-driven reload can end or disrupt
the diagnostic packaging window. Do not use a missing feedback-diagnostic entry
as negative evidence for hot reload.

## Encoding

If PowerShell `Select-String` prints one-character or garbled output, the log
may contain NUL bytes. Strip them and decode locally before filtering:

```powershell
$bytes = [System.IO.File]::ReadAllBytes("$out\modules_current.log")
$text = [System.Text.Encoding]::UTF8.GetString($bytes.Where({$_ -ne 0}))
$text | Select-String -Pattern '<your-probe-pattern>'
```
