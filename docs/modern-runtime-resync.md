# Modern Runtime Resync

This is the living tracker for the Modern viewport/runtime investigation.

## Living Document Rules

- Read this document before adding, changing, or removing any Modern
  viewport/runtime route. If the change touches shared runtime code under
  `app/src/main/java/com/dpis/module/`, also read
  `docs/legacy-runtime-resync.md`. See `docs/private/` for
  app-specific investigation notes (not committed).
- Record every new route exploration, route detail adjustment, failed attempt,
  unused path, and important runtime finding here. Treat experiments as
  accumulated evidence, not disposable notes.
- Do not delete historical route records unless they are duplicated or
  misleading. Prefer marking them `active`, `inactive`, `superseded`,
  `rejected`, or `unknown`, with a short reason and the evidence that changed
  the decision.
- Keep the tree and ledger aligned: when a route moves between active and
  inactive use, update both the route tree and the experiment ledger.

## Current Question

How does the Modern APK route viewport and font runtime changes through
libxposed, and how do we keep future Legacy experiments from accidentally
changing Modern behavior?

## 102 Coexistence Plan

The Modern codebase stays single-track. API 102 does not get a separate business
tree; it adds lifecycle and hook-management capabilities on top of the existing
101 runtime routes.

Naming rule:

- user-facing terminology stays `modern`;
- internal capability boundaries may be named by concrete libxposed API
  versions such as `101`, `102`, and later `103`/`104`;
- shared routing, planner, and runtime semantics should not be renamed to
  `modern101` / `modern102` because they still describe one Modern route tree.

Planned shape:

- keep the current shared app-process and system_server routing model;
- add `onHotReloading()` / `onHotReloaded()` in the Modern entry only;
- treat `replaceHook()` and hook ids as the preferred 102-level maintenance
  path for hooks that already have stable identities;
- keep 101 behavior as the default install path and fall back to full reinstall
  when a hook is not yet id-stable;
- avoid splitting installer logic into `101` and `102` copies unless a route
  proves it needs different runtime behavior.

Hot reload implementation notes:

- when a hook can be given a stable id, assign it once at install time instead
  of adding a second replacement path later;
- for stable app-process resource hooks, let API 102 rebuild the hook with the
  same executable and id so the framework replaces it atomically;
- when a hook is not yet id-stable, keep it on the restart-required path until
  the route has a proven reload owner;
- start with the hot paths that already have stable ownership boundaries:
  `ModuleMain`, `AppProcessHookInstaller`, and the system_server installer
  entry, then expand only when a real reload path is needed.
- verify hot reload with LSPosed bridge logs first. A successful module-side
  reload should show `DPIS hot reload begin`, `DPIS hot reload replay`, and
  `DPIS hot reload end` in `modules_*.log` or `verbose_*.log`. Feedback
  diagnostics are useful as supporting context only, because reinstall-driven
  reload can end the diagnostic session before packaging. System-server replay
  is not part of the current 102 hot-reload surface and still depends on the
  next normal install path.
- if LSPosed reports `Auto hot reload failed ... status=3, message=null` and
  there is no `DPIS hot reload begin`, the reload did not reach the new replay
  path. The common first-update case is that the already-running target process
  still holds an older DPIS generation whose default `onHotReloading()` rejects
  reload. Restart the target process once after installing the 102-capable
  build, then use the next install/update to validate the hot-reload path.
- LSPosed's notification progress may lag behind the install moment for target
  processes that are stopped, stale, or not immediately schedulable. In the
  2026-06-24 device export, old `Auto hot reload failed` lines clustered around
  04:34-04:44, while later user-launched/active processes produced fresh
  `DPIS hot reload begin -> replay -> end` evidence at 12:20 and 12:35. Treat
  those later bridge logs as the replay truth for that process instead of
  treating the notification progress as a DPIS save/config failure.
- API 102 still does not replay package-ready callbacks automatically. DPIS
  carries the last package-ready package/classloader/applicationInfo through
  `setSavedInstanceState(...)` and, after generic module-loaded replay, retries
  the package-ready supplement routes that need the app classloader: WeChat DPI,
  typeface replacement, and Flutter settings. This supplement replay is
  intentionally app-process only; system_server uses the narrower replay path
  below.
- system_server replay is narrower and more valuable than broad app-process
  replay. On API 102 hot reload, only the system process clears the
  system_server install gate and re-enters the existing system_server installer
  with stable hook ids. App processes continue to use best-effort replay and may
  still need a target app restart when frozen or cached runtime objects remain.

Practical boundary:

- 101 remains the baseline compatibility line for the shared route code;
- 102 is used to simplify lifecycle cleanup and hot-reload replay in Modern;
- Legacy stays on its own 100 surface and is not part of the 102 migration.
- version-specific capability code should stay explicit (`101`, `102`) rather
  than introducing vague tiers like `baseline` / `enhanced`.

Current structure note:

- `system_server` hook entry definitions now live in a dedicated catalog so the
  shared installer no longer owns raw entry arrays inline;
- 102 may assign stable hook ids for those entries as future maintenance
  anchors, but `system_server` still does not advertise replay/hot-reload
  support until install ownership is narrowed beyond the current process-scoped
  one-shot gate.

## Route Map

```text
Viewport mode
  auto
    -> system hooks enabled  => system
    -> system hooks disabled => compat

  system
    -> libxposed system_server route
    -> SystemServerDisplayEnvironmentInstaller owns system-side viewport mutation
    -> target selection is field-aware for each system_server entry
    -> app-process Resources bridge remains installed for resource sync/fallback
    -> app-process Display / WindowMetrics supplement is skipped

  compat
    -> libxposed app-process route
    -> AppProcessHookInstaller owns Resources / Display / WindowMetrics hooks

Font mode
  system
    -> internal system_server_font domain
    -> SystemServerDisplayEnvironmentInstaller writes Configuration.fontScale
       only at launch-activity-item
    -> internal app-process semantic supplements:
       activity_thread_font, resources_font, webview_text_zoom

  compat
    -> app-process field-rewrite route
    -> custom hook-chain UI controls this mode only
```

## Feedback Diagnostic Runtime Timeline

Status: active first pass.

Feedback diagnostic sessions now merge three best-effort runtime sources into
`diagnostic.txt`:

- UI-process `DpisLog` mirror while the session is active.
- LSPosed log lines parsed inside the diagnostic time window. Raw LSPosed
  evidence remains in `lsposed-log.txt`; `diagnostic.txt` only receives
  structured `source=lsposed-log` events.
- An experimental append-only runtime transport using a root-prepared marker and
  event file under `/data/local/tmp/dpis-feedback-diagnostic`. Hook processes
  try to append centralized `DpisLog` events when the marker is readable. Android
  sandbox or SELinux failures are silent at runtime and exported as
  `source=runtime-transport` notes.

The collector is scoped to the requested package and classifies centralized log
messages into config, viewport, font, typeface, route, skipped,
unexpected-route, and repeated-write events. This remains a first-pass
diagnostic aid and does not add new per-hook behavior.

Runtime hotpath fallback now emits `DPIS_DIAG_HOTPATH` only while a diagnostic
marker is active. First covered font field-rewrite points are TextAppearance,
TextView `setTextSize` SP/absolute rewrites, TextView current-px attach/setText
reinforcement, TextView span rewrite, Paint/TextPaint fallback, Android WebView
textZoom, and X5 WebView textZoom. These events are intended to prove callback
and mutation timing; they are not a user-visible summary layer.

As of 2026-06-24, `ForceTextSizeHookInstaller` also emits LSPosed bridge-window
evidence for API 102-friendly field-rewrite hooks. Runtime diagnostics can now
look for `DPIS_FONT ForceTextSize hook ready` plus first-hit
`... override applied` bridge lines to distinguish install success from actual
rewrite callbacks.

## Full Tree

```text
DPIS modern target package
  |
  +-- entry
  |     |
  |     +-- app/src/modern/java/com/dpis/module/ModuleMain.java
  |           |
  |           +-- onModuleLoaded
  |           |     |
  |           |     +-- record current process
  |           |     +-- initialize host config store
  |           |     +-- maybeInstallAppProcessFromModuleLoaded
  |           |
  |           +-- onSystemServerStarting
  |           |     |
  |           |     +-- maybeInstallSystemServerHooks (official system_server entry)
  |           |
  |           +-- onPackageReady
  |                 |
  |                 +-- maybeInstallSystemServerHooks (package-ready fallback)
  |                 +-- installAppProcessHooksIfConfigured
  |                 +-- retryTypefaceHooksWithPackageReady
  |                 +-- retryFlutterHooksWithAppClassLoader
  |
  +-- system_server route
  |     |
  |     +-- guard:
  |     |     SystemServerMutationPolicy.shouldInstallSystemServerHooks
  |     |
  |     +-- installer:
  |     |     SystemServerDisplayEnvironmentInstaller.install
  |     |
  |     +-- source:
  |     |     PerAppDisplayConfigSource
  |     |
  |     +-- important entries:
  |           |
  |           +-- activity-start
  |           +-- launch-activity-item
  |           +-- config-dispatch
  |           +-- display-content-config
  |           +-- display-manager-info
  |           +-- relayout-dispatch
  |           +-- display-policy-layout
  |           +-- hyperos-rust-process
  |                 HyperOS native font process environment route
  |
  |     +-- mutation fields:
  |           |
  |           +-- VIEWPORT
  |           |     multi-entry system-side lifecycle maintenance
  |           |
  |           +-- FONT_SCALE
  |                 launch-activity-item only; later config-dispatch writes can
  |                 surface as CONFIG_FONT_SCALE relaunches
  |
  |     +-- entry selection:
  |           |
  |           +-- font-only configs are selected only for launch-activity-item
  |           +-- viewport configs remain selected for viewport lifecycle entries
  |
  +-- app-process route
        |
        +-- guard:
        |     ModulePackagePlan.resolve
        |
        +-- planner:
        |     HookExecutionPlanner.buildPlan
        |
        +-- installer:
              AppProcessHookInstaller.install
                |
                +-- ResourcesManagerHookInstaller
                |     ResourcesManager.applyConfigurationToResources
                |     ResourcesManager.updateResourcesForActivity
                |     ResourcesManager create/get resource methods
                |     ResourcesKey override fill
                |
                +-- ResourcesImplHookInstaller
                |     ResourcesImpl.updateConfiguration
                |
                +-- ResourcesReadHookInstaller
                |     Resources.getConfiguration
                |     Resources.getDisplayMetrics
                |     Resources.getSystem
                |
                +-- DisplayHookInstaller
                |     Display.getMetrics
                |     Display.getRealMetrics
                |     Display.getSize
                |     Display.getRealSize
                |     Display.getDisplayInfo
                |
                +-- WindowMetricsHookInstaller
                |     WindowMetrics.getBounds
                |
                +-- system-font semantic supplements
                |     ActivityThreadFontHookInstaller
                |       ActivityThread.handleBindApplication
                |     ResourcesManagerHookInstaller
                |     ResourcesImplHookInstaller
                |     ResourcesReadHookInstaller
                |     WebViewFontHookInstaller
                |
                +-- compat-font field-rewrite routes
                |     ForceTextSizeHookInstaller
                |     WebViewFontHookInstaller
                |
                +-- optional typeface / cross-runtime font routes
                      FlutterSettingsFontHookInstaller
                      HyperOsFlutterFontHookInstaller
                      TypefaceOverrideHookInstaller
```

## Mode Tree

```text
requested mode
  |
  +-- system
  |     |
  |     +-- EffectiveModeResolver => system
  |     +-- expects system_server scope and hook installation
  |     +-- app-process resources may observe and publish records
  |     +-- app-process Display / WindowMetrics supplement is skipped when resolved mode is system
  |
  +-- compat
  |     |
  |     +-- EffectiveModeResolver => compat
  |     +-- app-process resources apply configuration directly
  |     +-- Display / WindowMetrics supplement is installed
  |
  +-- auto
        |
        +-- system hooks enabled
        |     |
        |     +-- EffectiveModeResolver => system
        |     +-- must be debugged as system unless a guarded fallback is designed
        |
        +-- system hooks disabled
              |
              +-- EffectiveModeResolver => compat
```

## Font Mode Tree

```text
requested font mode
  |
  +-- system
  |     |
  |     +-- EffectiveModeResolver => system
  |     +-- system_server_font is an internal scheduler domain
  |     +-- FONT_SCALE may write only at launch-activity-item
  |     +-- app-process semantic supplements remain available for fallback
  |     +-- optional Flutter/HyperOS native supplements remain package/config gated
  |     +-- custom hook-chain UI state is ignored
  |
  +-- compat
  |     |
  |     +-- EffectiveModeResolver => compat
  |     +-- custom hook-chain UI state can select field-rewrite domains
  |     +-- Resources / TextView / Paint / WebView field routes apply in app process
  |
  +-- off
        |
        +-- no font route
```

## WeChat Route Notes

Detailed app-specific runtime evidence lives in
`docs/private/wechat-dpi-runtime-notes.md`.

- 2026-06-22 active: WeChat independent DPI participates in feedback
  diagnostics as `route=wechat_dpi` for WeChat targets. A saved WeChat DPI config
  makes callback and mutation evidence expected; without that config, callback
  or mutation hits are unexpected route activity. The modern route emits
  structured diagnostic evidence for package-ready, application-attach retry,
  DisplayMetrics hook, and bottom-tab icon hook stages so diagnostic packages
  can distinguish hook ready, callback, applied mutation, and skipped locator
  paths. Package-loaded and broad module-loaded class-loader probes remain
  removed after user reports of crashes/jank in newer builds, but a narrower
  application-attach retry was restored after 8.0.74 validation proved WeChat
  can move the live density-manager class to Tinker `DelegateLastClassLoader`.
- 2026-06-22 debug-only crash triage: modern debug builds temporarily ran a
  WeChat bisection route for user crash isolation. For 8.0.74 / versionCode
  3120, the confirmed stable independent route keeps static DisplayMetrics
  getters `j65.f#d` and `j65.f#e`, keeps the narrow
  `Application.attach(Context)` classloader retry, and keeps bottom-tab icon
  compensation on. The older static-table target-field / mutator roles
  `g/k/l` remain only as commented historical reference and are not active.
- 2026-06-23 active: feedback diagnostics for the 8.0.74 route now also emit
  the selected route plan (`versionCode`, locator source, `d/e` metrics
  targets, bottom-tab enabled state, and retired `g/k/l` status), the
  `Application.attach(Context)` retry install result, the first callback method
  short name, and the applied method short name. These details stay on the
  feedback-diagnostic hot path only; they do not widen normal always-on probes.
- 2026-06-17 superseded: WeChat independent DPI route tested a module-loaded
  `ClassLoader.loadClass(String, boolean)` probe in the WeChat main process and
  an `Application.attach(Context)` retry from the runtime `Context` classloader.
  The broad `ClassLoader.loadClass` probe was removed on 2026-06-22 as the
  highest-risk startup-time expansion. The `Application.attach(Context)` retry
  was later restored as a narrow WeChat-only classloader retry after 8.0.74
  validation showed the live density-manager class can come from Tinker
  `DelegateLastClassLoader`.
- 2026-06-17 superseded: WeChat bottom-tab icon compensation was scoped to
  `com.tencent.mm.ui.TabIconView`, not to a WeChat version range. The route
  hooked the 4-argument bottom-tab icon init method when that
  structure exists and writes the non-static float scale field using
  `dpi * 1.1666666 / 400`, matching the upstream behavior while keeping chat,
  article, and other DPI effects owned by the density-manager route. The write
  intentionally happens before the matched init method, following the upstream
  route where the init path consumes the prepared scale.
- 2026-06-17 rejected: WeChat module-loaded
  `BaseDexClassLoader.findClass(String)` probe. It installed successfully but
  did not produce a density-manager class hit during validation. The route is
  too broad for production or continued diagnosis.
- 2026-06-17 active: WeChat target-field setter hook now proceeds exactly once.
  Reason: the previous interceptor called `chain.proceed()` before checking the
  setter/getter branches, which could write the original value and then write
  the configured value again.

## 101 / 100 Boundary

```text
101-only
  |
  +-- app/src/modern/java/com/dpis/module/ModuleMain.java
  +-- libxposed XposedModule lifecycle
  +-- SystemServerDisplayEnvironmentInstaller installation through XposedInterface
  +-- 102 hot-reload callbacks are only enabled when the Modern entry is running
      on an API 102-capable framework

100-only
  |
  +-- app/src/legacy/java/com/dpis/module/LegacyModuleHook.java
  +-- legacy IXposedHookLoadPackage / IXposedHookZygoteInit lifecycle
  +-- LegacySystemServerHookInstaller
  +-- Legacy-specific WebView layout JS supplement

shared
  |
  +-- AppProcessHookInstaller
  +-- HookExecutionPlanner
  +-- ResourcesManagerHookInstaller
  +-- ResourcesImplHookInstaller
  +-- ResourcesReadHookInstaller
  +-- DisplayHookInstaller / WindowMetricsHookInstaller
  +-- ViewportModePolicy / EffectiveModeResolver
```

## Experiment Ledger

Keep every trial here. Do not delete failed attempts. Mark them as inactive or
superseded.

| Date | Route | Change | Status | Evidence | Notes |
| --- | --- | --- | --- | --- | --- |
| 2026-06-01 | system | `launch-activity-item` system route restored in 101 work | active baseline | commit history contains launch route restoration | Keep separate from legacy experiments |
| 2026-06-01 | shared app-process | ResourcesKey empty override fill | active / shared | unit test covers empty override fill | Shared path; check 101 tests when changing |
| 2026-06-04 | WeChat target-field | Keep app-specific route alongside generic hooks and add the required write-side companion route for versions that need it | superseded | Detailed version-specific evidence lives in `docs/private/wechat-target-field.md` | Replaced by the WeChat DisplayMetrics DPI route; do not reintroduce target-field hooks without fresh evidence |
| 2026-06-04 | WeChat 8.0.71 target-field | Replace stale constructor-field route with the verified getter/setter route shape | superseded | Detailed evidence lives in `docs/private/wechat-target-field.md` | Kept as locator history only |
| 2026-06-07 | font system emulation | Add `system_server_font` as an explicit internal domain for `Configuration.fontScale` | active / superseded fallback | Douyin and Bilibili repros stopped flickering when only system_server font mutation was skipped; app-process font domains still scaled text | Kept as planner/runtime diagnostic state, not a compat custom-chain switch |
| 2026-06-07 | font system emulation | Route `FONT_SCALE` through field-level system_server scheduling and allow it only at `launch-activity-item` | active | Unit policy tests cover viewport multi-entry scheduling and font launch-only scheduling | Avoids later config-dispatch writes that can surface as `CONFIG_FONT_SCALE` relaunches |
| 2026-06-07 | font system emulation | Make modern system_server package selection field-aware per entry | active | Unit policy tests cover font-only launch selection and non-launch skip | Keeps font-only packages out of non-launch hot paths while preserving viewport multi-entry scheduling |
| 2026-06-07 | shared app-process viewport | Preserve small-window geometry for relative-scale app-process borrow targets while applying target density through ResourcesImpl / ResourcesRead metrics | active | Quetta small-window route isolation showed disabling ResourcesImpl stops flicker but loses Chromium scaling; focused unit tests cover window density compensation | Keeps DPIS unified scheduling active without publishing app-process borrow targets or forcing small-window Configuration width/height to the display target |
| 2026-06-08 | WeChat DPI | Replace the old target-field route with the WeKit-style DisplayMetrics post-processing route as the official WeChat independent path | active / adjusted | Runtime check confirmed property publication, hook installation, and mutation callback on `q35.f` for 8.0.71; later 8.0.74 validation required restoring version-scoped target-field/mutator roles and bottom-tab support through the static method table | DPIS primarily mutates returned `DisplayMetrics`; selected versions may also enable table-owned target-field, mutator, and bottom-tab supplements |
| 2026-06-09 | WeChat DPI | Move method discovery to a shared WeKit-style DexKit locator with a static version table | active / adjusted | Unit/source tests cover the DexKit rule, static ownership, and shared runtime mutation formula | Locator matches the `MMDensityManager` / `screenResolution_target_field` signature and logs whether `dexkit` or `static-route` installed hooks |
| 2026-06-09 | WeChat DPI | Add extracted-native-library fallback for DexKit inside the LSPosed module classloader | active | Real-device WeChat 8.0.74 / versionCode 3120 showed `System.loadLibrary("dexkit")` failed in `LspModuleClassLoader`; after fallback, logs reached `hook ready`, `callback hit`, and `applied` on `j65.f#e`, and visual effect was confirmed without uninstalling DPIS | Keep this as a module-loading fix, not a config-reset workaround; 3120 / `j65.f` is also in the static fallback table |
| 2026-06-17 | WeChat DPI | Prefer verified static routes before DexKit discovery for known WeChat versions | active | Runtime validation showed the static route installs earlier than DexKit discovery and avoids missing early one-shot metrics reads | Static route keeps known versions on the shortest install path; DexKit remains the automatic adaptation path for unknown versions |
| 2026-06-17 | WeChat DPI | Test density-manager constructor and static `DisplayMetrics` cache correction | rejected | Decompilation and runtime validation did not prove a stable cache mutation point | Do not keep constructor/cache retry as production behavior without mutation evidence |
| 2026-06-17 | WeChat DPI | Expand the independent route from getter-only hooks to the `Configuration + DisplayMetrics` mutator inside `MMDensityManager` | superseded / narrowed | Full-dex analysis showed WeChat resources can forward configuration updates into the same density-manager class while constructor/cache probes produced no mutation evidence | Broad locator-driven expansion was removed 2026-06-22 after crash/jank reports; exact mutator or target-field roles may only return as verified static-table entries for a specific version |
| 2026-06-17 | WeChat DPI | Move the independent route's first install attempt to package-loaded | superseded | Runtime validation showed package-loaded is useful for timing but not sufficient by itself | Package-loaded and module-loaded class probes remain removed; package-ready is the main app-specific entry, with only a narrow `Application.attach(Context)` retry for WeChat runtime classloader recovery |
| 2026-06-22 | WeChat DPI | Remove high-risk new route expansion, then reintroduce only the runtime pieces proven necessary on 8.0.74 | active / adjusted | User reports indicated newer builds could crash/jank while v1.12.3 behavior was best; later device validation showed 8.0.74 needs `package-ready` plus `Application.attach` retry to reach Tinker `DelegateLastClassLoader`, and versionCode 3120 uses static method roles `d/e/g/k/l` with bottom-tab enabled | Keep static route preferred for known versions, DexKit as fallback for unknown versions, and express version-specific extras through the static table instead of broad route expansion |
| 2026-06-18 | modern system_server | Move the first system_server installer attempt to libxposed's `onSystemServerStarting` callback | active | LSPosed_20260617_235835 showed preconfigured unrestricted apps launching at boot before DPIS UI, while the system process had only `module-loaded app hook install skipped system process` and no system_server hook/callback evidence | `onPackageReady` remains a de-duplicated fallback; this is a lifecycle timing fix, not an app-specific package recommendation |
| 2026-06-15 | shared app-process font | Add an event-gated `resources_font` scheduler for Resources read-path font conflicts | active / shared | Bilibili `resources_font`-only repro showed `Configuration.fontScale` alternating between base and target while `getDisplayMetrics` recomputed `scaledDensity`; after the event gate, `scaledDensity=3.0` and `1.4 -> 1.0` disappeared, and read metrics logging dropped sharply after idempotent writes. TapTap system-font repro later showed no config churn after disabling read-side configuration writes, but `getDisplayMetrics` could still downgrade target metrics from `4.2` to `3.9` when the system config stayed at `1.3` | Read-conflict target suppression outranks Compose base suppression; non-Compose observations must not clear an established read-conflict target state. Compat `resources_font` uses `ResourcesImpl` as a low-frequency metrics seed plus `ResourcesRead` fallback; when `ResourcesRead` is installed only for font it skips viewport target resolution and `VirtualDisplayState` reuse while keeping metrics density synchronized with configuration; system font emulation does not let `ResourcesRead(getConfiguration)` force target `fontScale` on every read, but `ResourcesRead(getDisplayMetrics)` may fill `scaledDensity` from the target factor so read-side metrics do not downgrade an already-targeted font scale; Compose diagnostics can detach after the read-conflict target event is established; `ResourcesManager` write-side hooks remain for viewport and system font emulation |

## Safety Rules

- Changes under `app/src/main/java/` are shared and must be reviewed for both
  100 and 101.
- Any change to `ResourcesManagerHookInstaller`, `ResourcesImplHookInstaller`,
  `ResourcesReadHookInstaller`, `DisplayHookInstaller`,
  `WindowMetricsHookInstaller`, `HookExecutionPlanner`, or
  `ViewportModePolicy` is a 101-impacting change.
- For 101 system route, require system_server install evidence plus
  callback/mutation evidence. `hook ready` alone is not enough.
- Treat Bilibili/Douyin flicker findings as evidence for the generic
  `FONT_SCALE` field policy, not as package-name recommendations. Hook-chain
  restore default clears the compat custom override and returns to the compat
  recommended template; it must not grow a Bilibili/Douyin default list.

## Update Log

- 2026-06-23: hot reload validation now treats LSPosed bridge logs as the
  primary evidence source. `ModuleMain` emits `hot reload begin/replay/end`
  through the libxposed log channel so a future reinstall can distinguish
  framework-level reload failure from module replay failure.
- 2026-06-24: feedback diagnostics and the in-app LSPosed log page now retain
  LSPosedService hot-reload warnings that explicitly mention DPIS. These lines
  are framework outcomes, not module-emitted hook evidence, and are classified
  separately as `route=hot_reload stage=skipped`.
- 2026-06-23: removed the unused Modern hook handle registry. API 102 hook
  identity is represented by `setId(...)`; API 101 compatibility remains the
  existing install-and-restart path and does not share this hot-reload surface.
- 2026-06-23: completed the first API 102 hook-id pass for app-process
  resources hooks. `ResourcesManager` fixed hooks now use stable ids, and the
  dynamic resource-creation / `createResourcesImpl` overload hooks derive ids
  from their method signatures so hot reload can replace them instead of
  stacking id-less duplicates.
- 2026-06-23: Modern runtime now plans for API 102 hot reload by keeping 101
  as the baseline route, assigning stable IDs to replaceable resource hooks,
  and treating `replaceHook()` as a possible path only for hooks that already
  have a known executable owner. This is a lifecycle addition, not a new
  viewport or font semantics route.
- 2026-06-24: shared Modern hook code now routes version-specific hook
  capabilities through explicit `101` / `102` capability helpers instead of
  hardcoding `setId(...)` directly in every installer. The external product
  name remains `modern`; the version split is an internal code capability
  boundary.
- 2026-06-21: feedback diagnostic LSPosed timeline now preserves semantic
  stage ordering for same-timestamp runtime events (`begin` before
  `applied`/`skipped`, then `end`), and explicit `DPIS_VIEWPORT*` messages no
  longer fall through to the generic font classifier when they mention
  `fontScale`. Diagnostic runtime hotpath evidence now also records selected
  compat viewport/resources boundaries such as runtime marker observation,
  `ResourcesManager` config override, `ResourcesImpl` observe/override/stable
  target, and `ResourcesRead` configuration/display-metrics overrides.
- 2026-06-21: shared app-process viewport diagnostics now also emit first-hit
  plus counted-sample runtime-hotpath evidence from `DisplayHookInstaller` and
  `WindowMetricsHookInstaller`. Repeated callback evidence includes `hitCount`
  and `suppressedCount`, so rapid-scrolling repros can distinguish callback hit,
  stable-target/no-record skip, and actual display/window mutation without
  flooding LSPosed logs.
- 2026-06-21: shared diagnostic log behavior now carries
  `diagnosticLogFingerprint=diag-log-2026-06-21-counted-hotpath-v1` in the app
  hook plan and Display/WindowMetrics supplement readiness/probe evidence. Bump
  `RuntimeDiagnosticLogFingerprint.VALUE` whenever runtime diagnostic log
  semantics change.
- 2026-06-01: initial tracker created.
- 2026-06-04: WeChat target-field route no longer suppresses generic app-process
  hooks; target-field runtime property publication now mirrors volatile and
  persistent properties, and hook reads use persistent fallback.
- 2026-06-08: WeChat target-field hooks were superseded by the official WeChat
  DPI route. The app-specific route now hooks no-arg `DisplayMetrics` methods
  on the version-specific WeChat density-manager class and post-processes
  `density`, `densityDpi`, and `scaledDensity` from the configured DPI.
- 2026-06-07: diagnostic overrides showed that skipping only system_server
  `Configuration.fontScale` removes Douyin and Bilibili flicker while
  app-process font domains can still scale text. The route is now represented
  as the explicit internal `system_server_font` domain for planner/runtime
  scheduling evidence, not as part of the compat custom-chain switch group.
- 2026-06-07: field-level system_server mutation scheduling now keeps viewport
  multi-entry behavior but narrows `FONT_SCALE` to launch-time configuration
  mutation. This moves the Bilibili/Douyin relaunch mitigation into DPIS
  scheduling instead of relying on users to know which sub-route to disable.
- 2026-06-07: documented the semantic boundary between requested hook domains
  and effective system_server execution. Bilibili/Douyin remain reproduction
  evidence for package-neutral scheduling; they are not built-in recommended
  hook-chain targets.
- 2026-06-07: restored the product boundary that custom font hook domains edit
  only the compat/field-rewrite chain. System-mode font routes remain internal
  scheduled routes and no longer share the custom-chain switch state.
- 2026-06-17: compat/field-rewrite automatic font domains no longer include
  `resources_font`; the custom-chain font page can still enable it manually.
  System-mode `resources_font` remains an internal semantic supplement.
- 2026-06-22: Paint/TextPaint field-rewrite fallback now uses the libxposed
  argument-replacement path instead of a post-call `setTextSize` rewrite. The
  route remains an independent custom-chain domain, but observes TextView/layout
  owned writes and relies on Paint provenance before applying fallback writes.
  The Paint provenance fallback decision is resolved in one tracker pass to
  avoid repeated hotpath drift/known-applied/scale lookups for each Paint write.
  Active diagnostics include a Paint fallback caller summary so repeated Paint
  writes can be classified as self-drawn fallback evidence or as candidates for
  stronger TextView/layout ownership.
- 2026-06-07: updated the route map to show system font mode explicitly:
  `system_server_font` is launch-only for `FONT_SCALE`, while
  `activity_thread_font`, `resources_font`, and `webview_text_zoom` remain
  internal app-process semantic supplements.
- 2026-06-07: system_server package selection is now field-aware per entry.
  Font-only configs are selected for `launch-activity-item`, but skipped for
  non-launch hot paths such as `config-dispatch` and `display-manager-info`.
- 2026-06-07: relative-scale app-process borrow targets now treat small-window
  geometry as owned by the window manager. `ResourcesImpl` and
  `ResourcesRead(getDisplayMetrics)` still derive the target density locally,
  but keep the current window dp size and do not publish display baselines from
  that borrowed result. This records the Quetta/Chromium small-window finding
  without adding package-specific behavior.
- 2026-06-08: Quetta small-window validation exposed a mixed configuration
  during flexible-window transitions: `widthDp` / `heightDp` still described
  the small window while `smallestWidthDp` already matched the relative-scale
  target and `densityDpi` was still the source density. Relative-scale
  app-process consumers now treat matching local or target records as borrow
  targets, so the stable record density is reused instead of falling back to
  the source density.
- 2026-06-09: WeChat 8.0.74 / versionCode 3120 validation found that LSPosed
  exposes module native libraries through `base.apk!/lib/...`, while DPIS keeps
  legacy extracted native packaging for existing HyperOS native proxy behavior.
  The WeChat DexKit locator now falls back to loading the extracted
  `/data/app/.../lib/<abi>/libdexkit.so` path parsed from the module
  classloader. Covered evidence: `hook ready`, `callback hit`, `applied`, and
  user-confirmed visual effect after overlay install without uninstalling DPIS.
- 2026-06-09: review follow-up tightened WeChat DPI recovery behavior. Legacy
  `wekit_dpi` migration no longer overwrites an official value found through a
  mirror store, 3120 / `j65.f` is recorded as a static fallback, save failures
  no longer publish runtime properties, and recovery always clears the fixed
  WeChat property pair when no enabled WeChat DPI value exists.
- 2026-06-13: config-source ownership is now local-authoritative for the
  module app. LSPosed remote preferences remain a runtime delivery copy for
  hook processes, while `createLocalUiModuleConfigStore` reads only local
  `dpi_config` and never treats remote values as UI, backup, migration, or
  app-list input. Runtime-only fallback stores such as system properties plus
  XSharedPreferences remain explicit hook-side compatibility inputs.
- 2026-06-13: real config saves now route through a single runtime delivery
  resync action after successful local persistence. Per-field system property
  publishers still provide immediate hot-path mirrors, while
  `RuntimeConfigDelivery.publishLocalSnapshotAfterSave()` republishes the local
  authoritative snapshot to LSPosed remote preferences for hook-process startup
  and reconnection.
- 2026-06-14: boot-time always-running app processes must not treat global
  runtime debug mirrors as package configuration. `RuntimePropertyConfigPreferences`
  now marks `target_packages` only when a package-level viewport, font, or
  typeface mirror is present, so `ModuleMain` can fall back to
  LSPosed remote preferences when volatile per-package properties are empty
  after reboot but before DPIS has replayed runtime mirrors.
