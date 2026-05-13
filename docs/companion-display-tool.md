# Companion Display Tool

The companion display tool is a standalone Android app module used as a stable DPIS/LSPosed display and font validation target.

It is not an Xposed module and does not read DPIS private config.

## Package And APK

- Module: `:companion-display-tool`
- Package: `io.github.kwensiu.dpis.displaytool`
- Debug APK: `companion-display-tool/build/outputs/apk/debug/companion-display-tool-debug.apk`

Build and install:

```powershell
.\gradlew.bat :companion-display-tool:assembleDebug
adb -s <device> install -r "companion-display-tool\build\outputs\apk\debug\companion-display-tool-debug.apk"
```

## Control Commands

All adb control uses one package-owned broadcast action:

```powershell
adb -s <device> shell am broadcast `
  -a io.github.kwensiu.dpis.displaytool.CONTROL `
  -n io.github.kwensiu.dpis.displaytool/com.dpis.displaytool.ControlReceiver `
  --es action run_all
```

Supported `action` values:

- `run_all`
- `run_scene`
- `show_scene`
- `dump_summary`
- `reset_state`

Run one fragile scene:

```powershell
adb -s <device> shell am broadcast `
  -a io.github.kwensiu.dpis.displaytool.CONTROL `
  -n io.github.kwensiu.dpis.displaytool/com.dpis.displaytool.ControlReceiver `
  --es action run_scene `
  --es scene nested_scroll_text `
  --es variant fragile
```

Default cold start and `run_all` execute only the six core scenes with `variant=normal`.
Fragile variants are only executed through explicit `run_scene` requests.

## Core Scenes

Cold start order:

1. `baseline_text_sp`
2. `nested_scroll_text`
3. `recycler_text_bind`
4. `dialog_text_sp`
5. `styled_text_appearance`
6. `programmatic_text_px`

Fragile phase 1 coverage:

- `nested_scroll_text`
- `recycler_text_bind`
- `styled_text_appearance`
- `programmatic_text_px`

## Logcat Contract

Primary report surface:

```powershell
adb -s <device> logcat -d -v raw | Select-String -Pattern "DPIS_TEST"
```

Logs are single-line ASCII `key=value` records. Run boundaries use:

- `event=run_start`
- `event=run_end`

Scene events keep the required prefix:

```text
stage run_id scene variant event pkg font_scale density_dpi scaled_density width_dp height_dp
```

Useful phase 1 extension fields:

- `density`
- `width_px`
- `height_px`
- `width_dp_from_density`
- `height_dp_from_density`
- `expected_text_px`
- `rendered_scale`

For a 1080 px wide device, `width_dp_from_density` near `500.0` indicates an effective 500 dp density path.
For 300 percent font replacement, `rendered_scale` near `3.00` indicates the rendered text path is scaled 3x relative to current `scaled_density`.

If `font_scale=1.00` and `rendered_scale=3.00`, the tool reports `suspicious_reason=inconsistent_readings`.
That means configuration/display metrics readings and rendered text readings disagree; it is not automatically a double-scale failure.
