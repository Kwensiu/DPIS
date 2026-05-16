# Flutter Font Scaling Investigation Handoff

Last updated: 2026-05-17

This document summarizes the DPIS Flutter font-scaling investigation around a
generic Flutter app font replacement failure. The package
`com.mfcloudcalculate.networkdisk` was used as the live reproduction target, but
it must remain a test case only. Runtime code should not contain package-specific
or version-specific behavior for that app.

## Current Status

The latest device test still does not scale the Flutter homepage text.

Verified facts from the latest run on device `d7121fb5`:

- DPIS `modern101` debug build was installed.
- Target font replacement was configured to 200%.
- The target process received Android-side font configuration:
  `mCurrentConfig={2.0 ...}`.
- `dumpsys activity top` shows the active UI is Flutter:
  `FlutterFragment` and `io.flutter.embedding.android.FlutterView`.
- LSPosed module logs show the Java Flutter semantic installer was installed.
- LSPosed module logs do not show any successful semantic Flutter hit.
- `logcat -d` did not show a native generic Flutter hook hit.

Current conclusion: DPIS reaches the app process and Android configuration
state, but the current implementation has not yet reached the actual Flutter
text scaling consumption path for this app/runtime.

## Non-goals

- Do not add code that checks for `com.mfcloudcalculate.networkdisk`.
- Do not add per-version or per-device app adaptation logic.
- Do not continue the `libflutter.so` fingerprint/offset route as the default
  strategy. The user explicitly rejected that route because it is not
  maintainable when app or Flutter engine versions change.
- Do not claim success from `mCurrentConfig={2.0 ...}` alone. That only proves
  Android-side configuration reached the process, not that Flutter text consumed
  it.

## Latest Evidence

Latest LSPosed log file on the device:

```powershell
adb -s d7121fb5 shell su -c 'ls -t /data/adb/lspd/log | head -10'
```

Relevant output:

```text
verbose_2026-05-17T03:29:57.964305.log
modules_2026-05-17T03:29:57.964836.log
props.txt
kmsg.log
```

Latest UI/config confirmation:

```powershell
adb -s d7121fb5 shell dumpsys activity top |
  Select-String -Pattern 'mCurrentConfig|FlutterView|FlutterFragment|com.mfcloudcalculate.networkdisk'
```

Relevant facts from output:

```text
TASK ... com.mfcloudcalculate.networkdisk
ACTIVITY com.mfcloudcalculate.networkdisk/.MainActivity ...
mCurrentConfig={2.0 ... sw360dp w360dp h734dp 480dpi ...}
io.flutter.embedding.android.FlutterView{... 0,0-1080,2292 ...}
FlutterFragment{... tag=flutter_fragment}
mView=io.flutter.embedding.android.FlutterView{...}
```

Latest LSPosed module log query:

```powershell
adb -s d7121fb5 shell su -c 'cat /data/adb/lspd/log/modules_2026-05-17T03:29:57.964836.log' |
  Select-String -Pattern 'DPIS_FONT|DPIS_NATIVE|GENERIC_PUSH_STYLE_D11|Generic Flutter ParagraphBuilder|pushStyle override|settings override|BaseDex findClass hit|view found|resend|Flutter semantic|Generic Flutter mapped|hook result|native poll|loadedApk classloader|application classloader|content provider classloader|application onCreate classloader|active activity|view-root|attach bridge|fragment view'
```

Observed logs:

```text
DPIS DPIS_FONT Flutter semantic install active: package=com.mfcloudcalculate.networkdisk, percent=200, targetScale=2.0
DPIS DPIS_FONT Flutter semantic loadedApk classloader hook ready for com.mfcloudcalculate.networkdisk
DPIS DPIS_FONT Flutter semantic content provider classloader hook ready for com.mfcloudcalculate.networkdisk
DPIS DPIS_FONT Flutter semantic application classloader hook ready for com.mfcloudcalculate.networkdisk
DPIS DPIS_FONT Flutter semantic application onCreate classloader hook ready for com.mfcloudcalculate.networkdisk
DPIS DPIS_FONT Flutter semantic class-loader hook ready for com.mfcloudcalculate.networkdisk
DPIS DPIS_FONT Flutter semantic BaseDex findClass hook ready for com.mfcloudcalculate.networkdisk
DPIS DPIS_FONT Flutter semantic attach bridge ready for com.mfcloudcalculate.networkdisk
DPIS DPIS_FONT Flutter semantic activity scan ready for com.mfcloudcalculate.networkdisk
DPIS DPIS_FONT Flutter semantic view-root scan ready for com.mfcloudcalculate.networkdisk
DPIS DPIS_FONT Flutter semantic active activity scans scheduled: package=com.mfcloudcalculate.networkdisk
DPIS DPIS_FONT Flutter semantic active activity scan enter: package=com.mfcloudcalculate.networkdisk, source=active-activity-immediate
DPIS DPIS_FONT Flutter semantic active activity scan empty: package=com.mfcloudcalculate.networkdisk, source=active-activity-immediate, records=0
DPIS DPIS_FONT Flutter semantic active activity probe thread scheduled: package=com.mfcloudcalculate.networkdisk
DPIS DPIS_FONT Flutter semantic active activity probe thread entered: package=com.mfcloudcalculate.networkdisk
```

Important missing logs:

```text
BaseDex findClass hit
loadedApk classloader:
application classloader:
content provider classloader:
application onCreate classloader:
fragment view created
view found
resend user settings
settings override
Generic Flutter ParagraphBuilder::pushStyle hook result
Generic Flutter ParagraphBuilder::pushStyle fontSize override
GENERIC_PUSH_STYLE_D11
```

The missing logs matter because they are the first evidence points that would
prove the hook reached Flutter classes, Flutter views, Flutter settings traffic,
or native Flutter paragraph styling.

Latest normal `logcat -d` query:

```powershell
adb -s d7121fb5 logcat -d |
  Select-String -Pattern 'DPIS|DPIS_FONT|DPIS_NATIVE|GENERIC_PUSH_STYLE_D11|Generic Flutter|pushStyle|settings override|Flutter semantic|FlutterJNI|ParagraphBuilder'
```

Observed result: only unrelated/system DPIS entries appeared. No relevant
Flutter native hook hit or settings override was visible.

## Implementation Timeline

### 1. Initial crash when the target app was hooked

The first crash report was a native `SIGBUS`:

```text
signal 7 (SIGBUS), code 1 (BUS_ADRALN)
pc 001f022058000051
backtrace:
  #00 pc 0000002058000051 <unknown>
  #01 pc 0000000000029bd4 <anonymous:760b477000>
```

The crash occurred when the target app was hooked, even without width/font
replacement settings. This suggested an unsafe hook path or wrong native timing.
Later changes stopped the crash, so the remaining bug is now "no Flutter UI font
scaling", not "target app crashes".

### 2. Android Resources and WebView behavior were separated

Observed behavior:

- Ads, splash, banner, or WebView-like text scaled visibly.
- The Flutter homepage UI text mostly did not scale.
- DP replacement continued to work in the app.

Interpretation:

- WebView/platform text domains can be affected by existing DPIS routes.
- Flutter scene text is a separate rendering domain and cannot be proven by
  WebView ad text scaling.

### 3. Font-domain arbitration was introduced

New file:

- `app/src/main/java/com/dpis/module/FontHookArbitration.java`

Current domain plan shape:

- `resourcesFontEnabled`
- `webViewTextZoomEnabled`
- `textViewHooksEnabled`
- `textViewSpRewriteEnabled`
- `textViewAbsoluteRewriteEnabled`
- `paintFallbackEnabled`
- `flutterSettingsEnabled`

Design goal:

- Stop treating every font hook as an independent multiplier.
- Keep font behavior internally scheduled by rendering domain.
- Avoid double scaling when `scaledDensity` or `Configuration.fontScale` already
  represents the target font scale.

Current behavior:

- When font replacement is active, Flutter settings hooks are enabled by the
  domain plan.
- For field rewrite mode, Resources/WebView/TextView/Flutter domains are planned
  together, but TextView SP and Paint fallback are currently disabled to reduce
  double-scaling risk.

### 4. Flutter semantic Java route was added

New file:

- `app/src/main/java/com/dpis/module/FlutterSettingsFontHookInstaller.java`

Main attempted semantic boundaries:

- `ClassLoader.loadClass`
- `dalvik.system.BaseDexClassLoader.findClass`
- `android.app.LoadedApk.getClassLoader`
- `ContentProvider.attachInfo`
- `Application.attach`
- `Application.onCreate`
- `io.flutter.embedding.android.FlutterView.attachToFlutterEngine`
- `io.flutter.embedding.android.FlutterFragment.onViewCreated`
- `android.view.View.onAttachedToWindow`
- `Activity.onResume`
- `ViewRootImpl.performTraversals`
- active activity record scans
- Flutter `SettingsChannel$MessageBuilder`
- Flutter `SettingsChannel`
- Flutter `FlutterJNI.dispatchPlatformMessage`

Expected successful evidence would include at least one of:

```text
BaseDex findClass hit
view found
resend user settings
settings override
fragment view created
```

Latest evidence did not show those logs.

### 5. Native generic Flutter route was attempted

Changed file:

- `app/src/main/cpp/dpis_native.cpp`

Important current symbols/routes:

- `kGenericFlutterLibrary = "libflutter.so"`
- `kGenericFlutterAppLibrary = "libapp.so"`
- `GenericFlutterFontRoute`
- `GENERIC_PUSH_STYLE_D11`
- `kGenericParagraphBuilderPushStyleOffset = 0x82d470`
- `dpis_generic_push_style_font_size_input(...)`

Expected successful evidence would include:

```text
Generic Flutter ParagraphBuilder::pushStyle hook result=...
Generic Flutter ParagraphBuilder::pushStyle fontSize override: ...
pushStyleCalls=...
GENERIC_PUSH_STYLE_D11
```

Latest evidence did not show those logs.

Risk:

- This route uses a hardcoded native offset.
- It may be valid only for a specific Flutter engine build.
- It should not become the general strategy unless the offset is derived from
  robust engine/symbol/signature evidence.

### 6. Multiple device/version checks exposed the same maintainability problem

The route that depended on a native `libflutter.so` shape did not generalize
cleanly across devices/app versions. The user explicitly rejected "adding more
fingerprints" as a solution.

Current agreed direction:

- First priority is to make Flutter font scaling work.
- Do not bind the logic to one app.
- Prefer generic semantic evidence or a generic native mechanism derived from
  actual engine structure, not per-version constants.

## Current Code Entry Points

Primary app-process wiring:

- `app/src/main/java/com/dpis/module/AppProcessHookInstaller.java`
  - Resolves `FontHookPlan`.
  - Resolves `FontHookArbitration.FontDomainPlan`.
  - Installs Resources, ActivityThread, TextView, Flutter settings, WebView, and
    viewport hooks according to the plan.

Flutter semantic route:

- `app/src/main/java/com/dpis/module/FlutterSettingsFontHookInstaller.java`
  - Installs classloader/lifecycle/view/settings probes and attempted rewrites.
  - Logs bridge-visible `DPIS_FONT Flutter semantic ...` evidence.

Native Flutter/HyperOS route:

- `app/src/main/java/com/dpis/module/HyperOsFlutterFontHookInstaller.java`
  - Loads `dpis_native`.
  - Configures native font percent.
  - Adds debug-only runtime probes for Flutter library load and UI timing.

- `app/src/main/cpp/dpis_native.cpp`
  - Contains HyperOS-specific Flutter hooks.
  - Contains generic `libflutter.so` probing and attempted `pushStyle` hook.
  - Contains current hardcoded generic Flutter pushStyle offset route.

Related tests:

- `app/src/test/java/com/dpis/module/FlutterSettingsFontHookInstallerTest.java`
- `app/src/test/java/com/dpis/module/HyperOsFlutterFontHookConfigTest.java`
- `app/src/test/java/com/dpis/module/FontHookArbitrationTest.java`
- `app/src/test/java/com/dpis/module/AppProcessHookInstallerTest.java`
- `app/src/test/java/com/dpis/module/ForceTextSizeRegressionReferenceTest.java`

Related notes/plans:

- `docs/font-domain-arbitration-notes.md`
- `docs/superpowers/plans/2026-05-17-flutter-font-semantic-route.md`

## Route Evidence Table

| Route | Expected success log/evidence | Latest observed evidence | Status |
| --- | --- | --- | --- |
| App-process hook install | `Flutter semantic install active` | Present | Installed |
| Android Resources font config | `mCurrentConfig={2.0 ...}` in target activity | Present | Reaches process |
| Flutter UI detection | `FlutterFragment`, `FlutterView` in `dumpsys activity top` | Present | Target UI is Flutter |
| LoadedApk classloader | `loadedApk classloader:` | Missing; only `hook ready` | No hit |
| Application classloader | `application classloader:` | Missing; only `hook ready` | No hit |
| ContentProvider classloader | `content provider classloader:` | Missing; only `hook ready` | No hit |
| Application.onCreate classloader | `application onCreate classloader:` | Missing; only `hook ready` | No hit |
| BaseDex findClass | `BaseDex findClass hit` | Missing; only `hook ready` | No hit |
| FlutterFragment.onViewCreated | `fragment view created` | Missing | No hit |
| FlutterView scan | `view found` | Missing | No hit |
| Resend settings | `resend user settings` | Missing | No hit |
| Flutter settings rewrite | `settings override` | Missing | No hit |
| Native generic pushStyle | `Generic Flutter ParagraphBuilder::pushStyle ...` | Missing | No hit |
| User-visible result | Homepage text visibly larger | User reports no change | Failed |

## Commands Used During Latest Verification

Build/test commands that previously passed:

```powershell
./gradlew :app:testModern101DebugUnitTest --tests com.dpis.module.HyperOsFlutterFontHookConfigTest --tests com.dpis.module.FlutterSettingsFontHookInstallerTest
./gradlew :app:assembleModern101Debug
adb -s d7121fb5 install -r "app/build/outputs/apk/modern101/debug/app-modern101-debug.apk"
adb -s d7121fb5 logcat -c
adb -s d7121fb5 reboot
```

Serial install/reboot flow requested by the user:

```powershell
./gradlew :app:assembleModern101Debug
adb -s d7121fb5 install -r "app/build/outputs/apk/modern101/debug/app-modern101-debug.apk"
adb -s d7121fb5 logcat -c
adb -s d7121fb5 reboot
```

After reboot, stop and ask the user to unlock, skip ads, and return to the
target homepage before collecting UI/runtime evidence.

## Important Debugging Lessons

1. Visible WebView/ad text scaling does not prove Flutter text scaling.
2. `mCurrentConfig={2.0 ...}` proves Android config state only.
3. Hook-ready logs are not enough. A route needs a real hit or override log.
4. Adding more lifecycle/classloader probes has diminishing value unless a new
   observation explains why the existing probes do not hit.
5. Native offset patching can produce movement on one engine build but is not a
   maintainable general solution.

## Suggested Next Investigation

Recommended next direction:

1. Treat the current failure as "Flutter text route not reached", not "wrong
   multiplier".
2. Stop adding app-specific or version-specific fingerprints.
3. Use APK/JADX or runtime class inspection to confirm how this Flutter app
   embeds or loads Flutter:
   - cached engine or custom engine group
   - custom shell or plugin loader
   - split APK/dynamic feature classloader
   - obfuscated or relocated embedding classes
   - AOT/native path that bypasses Java settings resend
4. Confirm whether the target process actually loads the expected Java Flutter
   embedding classes through the hooked loaders.
5. If the semantic settings path is still pursued, identify a deterministic
   boundary where the app sends or receives `flutter/settings`.
6. If the native path is pursued, derive the target function from symbols,
   string references, disassembly signatures, or Flutter engine version
   evidence. Do not use blind offsets as the product route.
7. Build a minimal standalone Flutter test APK with a known engine version and
   textScale-sensitive UI. Use it to verify DPIS Flutter hooks independently of
   the target app.

## Quick Checklist For The Next Agent

Before claiming success, require at least one of these logs:

```text
settings override
view found
resend user settings
BaseDex findClass hit
fragment view created
Generic Flutter ParagraphBuilder::pushStyle fontSize override
```

Then require a user-visible or screenshot-confirmed result on the Flutter UI,
not just ads/WebView/platform text.

Before changing strategy, answer these questions with evidence:

- Why did `dumpsys` see `FlutterView` while active activity scans reported
  `records=0`?
- Are the Flutter embedding classes loaded before DPIS installs hooks, loaded
  by a different classloader, or absent from the app classpath?
- Does the app use the standard Android embedding `FlutterView` instance in a
  way that exposes `sendUserSettingsToFlutter`, or does it bypass that route?
- Does any `flutter/settings` message exist after DPIS installs?
- Is `libflutter.so` loaded in the target process at the time native probes run?

## Worktree Warning

The worktree is dirty and contains many runtime changes, tests, docs, and debug
artifacts. Do not revert unrelated files unless explicitly instructed.

Current notable dirty files include:

- `.gitignore`
- `app/build.gradle.kts`
- `app/src/main/cpp/dpis_native.cpp`
- `app/src/main/java/com/dpis/module/AppProcessHookInstaller.java`
- `app/src/main/java/com/dpis/module/DpisLog.java`
- `app/src/main/java/com/dpis/module/FontScaleOverride.java`
- `app/src/main/java/com/dpis/module/ForceTextSizeHookInstaller.java`
- `app/src/main/java/com/dpis/module/HyperOsFlutterFontHookInstaller.java`
- `app/src/main/java/com/dpis/module/FlutterSettingsFontHookInstaller.java`
- `app/src/main/java/com/dpis/module/FontHookArbitration.java`
- multiple unit tests under `app/src/test/java/com/dpis/module/`
- debug screenshots and temporary output directories

The next agent should inspect diffs before editing and avoid mixing cleanup with
new diagnosis unless the cleanup directly improves the feedback loop.
