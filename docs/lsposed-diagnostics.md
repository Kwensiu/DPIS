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

## Encoding

If PowerShell `Select-String` prints one-character or garbled output, the log
may contain NUL bytes. Strip them and decode locally before filtering:

```powershell
$bytes = [System.IO.File]::ReadAllBytes("$out\modules_current.log")
$text = [System.Text.Encoding]::UTF8.GetString($bytes.Where({$_ -ne 0}))
$text | Select-String -Pattern '<your-probe-pattern>'
```
