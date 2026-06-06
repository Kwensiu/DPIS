# compat100 Runtime Resync

This is the living tracker for the 100-line viewport investigation.

## Living Document Rules

- Read this document before adding, changing, or removing any compat100
  viewport/runtime route. If the change touches shared runtime code under
  `app/src/main/java/com/dpis/module/`, also read
  `docs/modern101-runtime-resync.md`.
- For WeChat-specific investigation notes and the native target-field route,
  see `docs/private/` (not committed).
- For LSPosed log diagnostics, see `docs/lsposed-diagnostics.md`.
- Record every new route exploration, route detail adjustment, failed attempt,
  unused path, and important runtime finding here. Treat experiments as
  accumulated evidence, not disposable notes.
- Do not delete historical route records unless they are duplicated or
  misleading. Prefer marking them `active`, `inactive`, `superseded`,
  `rejected`, or `unknown`, with a short reason and the evidence that changed
  the decision.
- Keep the tree and ledger aligned: when a route moves between active and
  inactive use, update both the route tree and the experiment ledger.

## Route Map

```text
Viewport mode
  auto
    -> system hooks enabled  => system
    -> system hooks disabled => compat

  system
    -> system_server route only
    -> app-process viewport supplement is currently skipped by design

  compat
    -> app-process resource/display/window route
    -> intended fallback when system route is unavailable or not chosen
```

## Full Tree

```text
DPIS viewport target package
  |
  +-- flavor boundary
  |     |
  |     +-- modern101
  |     |     |
  |     |     +-- entry: app/src/modern101/java/com/dpis/module/ModuleMain.java
  |     |     +-- system_server route:
  |     |     |     SystemServerDisplayEnvironmentInstaller
  |     |     +-- app-process route:
  |     |           AppProcessHookInstaller
  |     |           ResourcesManagerHookInstaller
  |     |           ResourcesImplHookInstaller
  |     |           ResourcesReadHookInstaller
  |     |           DisplayHookInstaller / WindowMetricsHookInstaller
  |     |
  |     +-- compat100
  |           |
  |           +-- entry: app/src/compat100/java/com/dpis/module/Compat100LegacyModuleHook.java
  |           +-- system_server route:
  |           |     Compat100SystemServerHookInstaller
  |           |       |
  |           |       +-- launch-activity-item
  |           |       |     status: computes target env; currently applies fontScale only
  |           |       |
  |           |       +-- rust-process
  |           |       |     status: unrelated to viewport
  |           |       |
  |           |       +-- display-manager-info
  |           |       |     status: active for auto/system absolute viewport
  |           |       |     evidence: UID-gated callback changes DisplayInfo density
  |           |       |
  |           |       +-- config-dispatch
  |           |             status: active for auto/system absolute viewport
  |           |             evidence: callback changes full/resolved/merged configs
  |           |
  |           +-- app-process route:
  |                 |
  |                 +-- ResourcesImpl.updateConfiguration
  |                 |     writes Configuration / DisplayMetrics
  |                 |
  |                 +-- ResourcesManager.applyConfigurationToResources
  |                 |     writes process resource configuration
  |                 |
  |                 +-- ResourcesManager.updateResourcesForActivity
  |                 |     status: active for navigation refresh
  |                 |
  |                 +-- ResourcesManager.createResourcesImpl(ResourcesKey)
  |                 |     status: active for key override fill
  |                 |
  |                 +-- Resources.getConfiguration / getDisplayMetrics / getSystem
  |                 |     read-time compensation
  |                 |
  |                 +-- Display.getMetrics / getRealMetrics / getSize / getRealSize / getDisplayInfo
  |                 |     app-process display supplement
  |                 |
  |                 +-- WindowMetrics.getBounds
  |                 |     app-process window supplement
  |                 |
  |                 +-- FlutterJNI.setViewportMetrics
  |                 |     status: active for Flutter/mixed shells
  |                 |     aligns Flutter devicePixelRatio to DPIS target density
  |                 |
  |                 +-- WebView / XWeb / TBS layout JS supplement
  |                       status: per-app configurable; disabled for unstable targets
  |
  +-- requested mode
        |
        +-- system
        |     +-- EffectiveModeResolver => system
        |     +-- system_server hooks required
        |     +-- app-process viewport supplement skipped
        |
        +-- compat
        |     +-- EffectiveModeResolver => compat
        |     +-- app-process hooks required
        |
        +-- auto
              +-- system hooks enabled
              |     +-- EffectiveModeResolver => system
              +-- system hooks disabled
                    +-- EffectiveModeResolver => compat
```

## Current Route Decision

The compat100 absolute-width viewport route is a four-layer coverage model:

```text
compat100 auto/system absolute viewport
  |
  +-- system_server ActivityRecord config-dispatch
  |     -> keeps reported/current Activity configuration at target values
  |
  +-- system_server DisplayManagerInfo
  |     -> keeps Binder-returned DisplayInfo density for the target UID
  |
  +-- app-process Resources / Display / WindowMetrics supplements
  |     -> covers Java resource reads, ResourcesKey creation, and app-local
  |        display metric reads during navigation
  |
  +-- app-process FlutterJNI viewport metrics bridge
        -> covers Flutter/mixed shells that consume engine viewport DPR
```

Per-app guards (splash startup filter, Display/WindowMetrics disable,
Flutter activity scope) are configured independently per target package.
See `docs/private/` for app-specific investigation notes.

## Experiment Ledger

| Date | Route | Change | Status | Notes |
| --- | --- | --- | --- | --- |
| 2026-06-01 | system | Restore launch-activity-item config mutation | rejected | Did not hold final viewport state |
| 2026-06-01 | system | ActivityRecord config-dispatch hook | active | Required for apps whose layout depends on Activity-level config |
| 2026-06-01 | system | DisplayManagerInfo UID-gated hook | active | Required for display metric consumers bypassing app-process Display |
| 2026-06-01 | compat | ResourcesKey empty override fill | active | Shared helper; preserves viewport in resource key path |
| 2026-06-01 | compat | App-process viewport supplement skipped for system mode | current design | Design gate, not a bug |
| 2026-06-01 | compat100 | FlutterJNI viewport metrics bridge | active, per-app guarded | Covers Flutter/mixed shells; guard scope per target |
| 2026-06-02 | compat100 | LaunchActivityItem post-construction object mutation | active | Aligns legacy launch delivery with modern101 |
| 2026-06-04 | WeChat target-field | Keep app-specific route alongside generic hooks, share target-field runtime property handling, and add the required write-side companion route for versions that need it | active | Public record keeps only the reusable route decision; detailed version-specific evidence lives in `docs/private/wechat-target-field.md` | Do not add or change version-specific WeChat routes without fresh evidence |
| 2026-06-04 | WeChat 8.0.71 target-field | Replace stale constructor-field route with the verified current route shape | active | Shared route registry decision; detailed evidence lives in `docs/private/wechat-target-field.md` | Do not reintroduce constructor-field route without fresh version-specific evidence |
| 2026-06-07 | font system emulation | Add `system_server_font` as an explicit hook-chain domain for `Configuration.fontScale` | active | Douyin and Bilibili repros stopped flickering when only system_server font mutation was skipped; app-process font domains still scaled text | Disable this domain per app when `CONFIG_FONT_SCALE` relaunch flicker appears |

## Safety Rules

- Changes under `app/src/modern101/java/` should not be described as compat100
  behavior.
- Changes under `app/src/main/java/` are shared and must be reviewed for both
  100 and 101.
- For compat100 system route, require system_server install evidence plus
  callback/mutation evidence. `hook ready` alone is not enough.
- Per-app stability guards (e.g., splash filter, Display/WindowMetrics disable,
  WebView supplement skip, Flutter activity scope) are configured per target
  package. Do not change default boundaries without per-target evidence.

## Update Log

- 2026-06-01: initial tracker created.
- 2026-06-01: confirmed auto absolute-width route needs ActivityRecord
  config-dispatch plus DisplayManagerInfo in addition to app-process
  Resources/Display supplements.
- 2026-06-01: added FlutterJNI viewport metrics bridge for Flutter/mixed
  shells; ViewRoot probing is inactive diagnostic evidence.
- 2026-06-02: added LaunchActivityItem post-construction object mutation to
  align legacy launch delivery with modern101.
- 2026-06-03: consolidated app-process viewport policy into a domain switch
  with per-app guard configuration.
- 2026-06-04: WeChat target-field route no longer suppresses generic app-process
  hooks; shared target-field runtime property handling now writes and reads with
  persistent fallback. Compat100 shares the write-side companion route support
  used by versions that need it.
- 2026-06-07: diagnostic overrides showed that skipping only system_server
  `Configuration.fontScale` removes Douyin and Bilibili flicker while
  app-process font domains can still scale text. The route is now represented
  as the explicit `system_server_font` hook-chain domain, so affected apps can
  keep system mode while disabling only this font sub-route.
