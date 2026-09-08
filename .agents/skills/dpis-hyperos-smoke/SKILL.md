---
name: dpis-hyperos-smoke
description: Run automated DPIS HyperOS device smoke tests for package-specific dp/font emulation or replacement. Use when testing HyperOS Gallery/Weather or other HyperOS native/Rust/Flutter app behavior, collecting adb/root/logcat evidence, checking DPIS config/properties/native proxy state, and summarizing the failing layer without manually reading Android logs.
---

# DPIS HyperOS Smoke

Use this skill to automate DPIS HyperOS diagnostics from the host machine.

## Workflow

1. Confirm the repo root is the DPIS project when possible.
2. Ask only for missing essentials:
   - target package, for example `com.miui.gallery` or weather package name
   - device serial only if more than one adb device is connected
   - whether target app restart is allowed
3. Run `scripts/hyperos_smoke.ps1`.
4. Read the generated report and summarize by layer:
   - device/root access
   - DPIS config layer
   - runtime property layer
   - native proxy layer
   - system_server/RustProcess hook layer
   - Java font fallback layer
   - viewport/dp layer
5. If a layer is inconclusive, say what extra run is needed instead of guessing.

## Script

Run from any directory:

```powershell
powershell -ExecutionPolicy Bypass -File "scripts/hyperos_smoke.ps1" -Package com.miui.gallery
```

Useful options:

```powershell
-Device <serial>          # adb serial if multiple devices exist
-FontPercent 300         # expected font value, used for checks only
-RestartTarget           # force-stop and launch the package with am start
-DeployProxy             # copy DPIS libdpis_native.so into the target native lib dir
-TailLines 800           # logcat lines to inspect
-OutDir <path>           # report output directory
```

The script is mostly read-only. `-RestartTarget` changes runtime state by force-stopping and launching the target app. `-DeployProxy` changes the target app native library directory and requires explicit user approval.

## Interpretation

- Config present but runtime properties are empty: save/publish path likely failed or root `setprop` failed.
- Runtime properties correct but proxy missing: native proxy deployment is the blocker.
- Proxy present and properties correct but no RustProcess/env logs after restart: system_server hook is stale, disabled, or not loaded.
- RustProcess/env logs present but no paragraph/native font logs: native proxy loaded path, `DPIS_NATIVE` log visibility, or native offsets are the likely blocker.
- Java fallback logs present but HyperOS UI unchanged: target text likely bypasses Java TextView/Paint path.
- Viewport summary present but UI unchanged: target may cache display config or use a path not covered by current hooks.

## Defaults

Use `com.miui.gallery` as the default package only when the user has not named another target and the discussion is about HyperOS Gallery.
