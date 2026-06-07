# compat100 Runtime Resync

This is the living tracker for the 100-line viewport/runtime investigation.

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
    -> system_server mutation route
    -> app-process Resources bridge remains installed for resource sync/fallback
    -> app-process Display / WindowMetrics supplement is skipped by design

  compat
    -> app-process resource/display/window route
    -> intended fallback when system route is unavailable or not chosen

Font mode
  system
    -> internal system_server_font domain
    -> compat100 system_server launch-activity-item writes
       Configuration.fontScale only
    -> internal app-process semantic supplements:
       activity_thread_font, resources_font, webview_text_zoom

  compat
    -> app-process field-rewrite route
    -> custom hook-chain UI controls this mode only
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
  |           |       |     status: applies FONT_SCALE only for system font mode
  |           |       |
  |           |       +-- rust-process
  |           |       |     status: HyperOS native font environment route;
  |           |       |             unrelated to viewport
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
  |                 +-- ActivityThread.handleBindApplication
  |                 |     system-font semantic bind-time supplement
  |                 |
  |                 +-- Resources / WebView semantic font routes
  |                 |     resources_font and webview_text_zoom for system font mode
  |                 |
  |                 +-- TextView / Paint / WebView field-rewrite routes
  |                 |     custom hook-chain UI controls these in compat font mode
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
        |     +-- app-process Resources bridge remains installed
        |     +-- app-process Display / WindowMetrics supplement skipped
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

The compat100 absolute-width viewport route is this coverage model:

```text
compat100 auto/system absolute viewport
  |
  +-- app-process Resources bridge
  |     -> covers Java resource reads and ResourcesKey creation during navigation
  |
  +-- app-process Display / WindowMetrics supplements
  |     -> covers app-local display metric reads when resolved mode is compat;
  |        skipped when resolved mode is system
  |
  +-- app-process FlutterJNI viewport metrics bridge
        -> covers Flutter/mixed shells that consume engine viewport DPR
```

compat100 does not currently install the shared modern101
`SystemServerDisplayEnvironmentInstaller`, so `config-dispatch` and
`display-manager-info` are not active compat100 system_server routes. The
compat100 system_server installer keeps only the launch-time Configuration
route and the HyperOS Rust process environment route.

Per-app guards (splash startup filter, Display/WindowMetrics disable,
Flutter activity scope) are configured independently per target package.
See `docs/private/` for app-specific investigation notes.

## System Font Route Decision

```text
compat100 system font mode
  |
  +-- system_server launch-activity-item
  |     -> writes Configuration.fontScale only at launch time
  |
  +-- app-process ActivityThread bind supplement
  |     -> rewrites AppBindData.config.fontScale before app bind proceeds
  |
  +-- app-process Resources / WebView semantic supplements
  |     -> preserve fontScale reads and WebView text zoom without TextView field rewrite
  |
  +-- optional Flutter/HyperOS native supplements
        -> remain package/config gated
```

`system_server_font` and `activity_thread_font` are internal scheduler domains.
They are not saved in the custom hook-chain override, and restoring the hook
chain returns only to the compat/field-rewrite recommended template.

## Experiment Ledger

| Date | Route | Change | Status | Notes |
| --- | --- | --- | --- | --- |
| 2026-06-01 | system | Restore launch-activity-item config mutation | rejected | Did not hold final viewport state |
| 2026-06-01 | system | ActivityRecord config-dispatch hook | shared-modern evidence | Required for apps whose layout depends on Activity-level config, but not currently installed by compat100 |
| 2026-06-01 | system | DisplayManagerInfo UID-gated hook | shared-modern evidence | Required for display metric consumers bypassing app-process Display, but not currently installed by compat100 |
| 2026-06-01 | compat | ResourcesKey empty override fill | active | Shared helper; preserves viewport in resource key path |
| 2026-06-01 | compat | App-process Display / WindowMetrics supplement skipped for system mode | current design | Resources bridge still installs; the design gate only applies to display/window supplement hooks |
| 2026-06-01 | compat100 | FlutterJNI viewport metrics bridge | active, per-app guarded | Covers Flutter/mixed shells; guard scope per target |
| 2026-06-02 | compat100 | LaunchActivityItem post-construction object mutation | active | Aligns legacy launch delivery with modern101 |
| 2026-06-04 | WeChat target-field | Keep app-specific route alongside generic hooks, share target-field runtime property handling, and add the required write-side companion route for versions that need it | active | Public record keeps only the reusable route decision; detailed version-specific evidence lives in `docs/private/wechat-target-field.md` | Do not add or change version-specific WeChat routes without fresh evidence |
| 2026-06-04 | WeChat 8.0.71 target-field | Replace stale constructor-field route with the verified current route shape | active | Shared route registry decision; detailed evidence lives in `docs/private/wechat-target-field.md` | Do not reintroduce constructor-field route without fresh version-specific evidence |
| 2026-06-07 | font system emulation | Add `system_server_font` as an explicit internal domain for `Configuration.fontScale` | active / superseded fallback | Douyin and Bilibili repros stopped flickering when only system_server font mutation was skipped; app-process font domains still scaled text | Kept as planner/runtime diagnostic state, not a compat custom-chain switch |
| 2026-06-07 | font system emulation | Route `FONT_SCALE` through field-level system_server scheduling and allow it only at `launch-activity-item` | active | Unit policy tests cover viewport multi-entry scheduling and font launch-only scheduling | Avoids later config-dispatch writes that can surface as `CONFIG_FONT_SCALE` relaunches |
| 2026-06-07 | shared app-process viewport | Relative-scale app-process borrow targets preserve small-window dp geometry while applying target density in ResourcesImpl / ResourcesRead metrics | active | Shared unit tests cover ResourcesImpl and ResourcesRead window density compensation | This is package-neutral shared behavior; compat100 inherits it through `app/src/main/java` |

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
- Treat Bilibili/Douyin flicker findings as evidence for the generic
  `FONT_SCALE` field policy, not as package-name recommendations. Hook-chain
  restore default clears the compat custom override and returns to the compat
  recommended template; it must not grow a Bilibili/Douyin default list.

## Update Log

- 2026-06-01: initial tracker created.
- 2026-06-01: shared modern route evidence confirmed auto absolute-width route
  needs ActivityRecord config-dispatch plus DisplayManagerInfo in addition to
  app-process Resources/Display supplements. compat100 does not currently
  install these two shared modern system_server entries.
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
- 2026-06-07: updated the route map to show system font mode explicitly and to
  split the app-process Resources bridge from the Display / WindowMetrics
  supplement that is skipped when viewport resolves to system.
- 2026-06-07: shared modern101 system_server package selection is now
  field-aware per entry. This records shared-code behavior for 100/101 review;
  compat100 still does not install the shared modern system_server entries.
- 2026-06-07: shared app-process relative-scale borrow handling now keeps
  small-window width/height dp owned by the window manager while deriving target
  density locally for `ResourcesImpl` and `ResourcesRead(getDisplayMetrics)`.
  This avoids using compat app-process routes to publish display baselines from
  a borrowed small-window result.
- 2026-06-08: shared relative-scale app-process consumers now classify matching
  local or target runtime records as borrow targets. This covers flexible-window
  mixed configurations where `smallestWidthDp` has reached the target but
  width/height and density still describe the source window.
