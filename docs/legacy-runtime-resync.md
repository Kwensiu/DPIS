# Legacy Runtime Resync

This is the living tracker for the Legacy viewport/runtime investigation.

## Living Document Rules

- Read this document before adding, changing, or removing any Legacy
  viewport/runtime route. If the change touches shared runtime code under
  `app/src/main/java/com/dpis/module/`, also read
  `docs/modern-runtime-resync.md`.
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
    -> system hooks unavailable or ineffective => compat

  system
    -> system_server mutation route
    -> app-process Resources bridge remains installed for resource sync/fallback
    -> app-process Display / WindowMetrics supplement is skipped by design

  compat
    -> app-process resource/display/window route
    -> explicit app-process route when selected

Font mode
  system
    -> internal system_server_font domain
    -> legacy system_server launch-activity-item writes
       Configuration.fontScale only
    -> internal app-process semantic supplements:
       activity_thread_font, resources_font, webview_text_zoom

  compat
    -> app-process field-rewrite route
    -> automatic defaults exclude resources_font
    -> resources_font remains manually selectable for read-path value rewrite
    -> custom hook-chain UI controls this mode only
```

## Feedback Diagnostic Runtime Timeline

Status: active first pass.

Feedback diagnostic sessions merge selected centralized `DpisLog` messages,
LSPosed lines parsed inside the diagnostic time window, and an experimental
append-only runtime transport into `diagnostic.txt`. Legacy routes are observed
through the same centralized logging boundary rather than new per-hook behavior.
Raw LSPosed evidence remains in `lsposed-log.txt`; structured parsed events are
marked `source=lsposed-log`.

The transport path is intentionally small: root prepares a marker and event file
under `/data/local/tmp/dpis-feedback-diagnostic`, then hook processes attempt to
append events when the marker is readable. Android sandbox or SELinux failures
are silent in hooks and exported as `source=runtime-transport` notes. This is a
first-pass feasibility probe, not a replacement for LSPosed diagnostics.

Runtime hotpath fallback currently covers the shared font field-rewrite paths
that also serve Legacy app-process hooks: TextAppearance, TextView `setTextSize`
SP/absolute rewrites, TextView current-px attach/setText reinforcement,
TextView span rewrite, Paint/TextPaint fallback, Android WebView textZoom, and
X5 WebView textZoom. The events are diagnostic-only and gated by the active
feedback-diagnostic marker.

As of 2026-08-13, diagnostic ZIPs include `timeline.tsv` and
`module-effects.tsv`. `module-effects.tsv` emits a `source=diagnostic-plan`
zero row for any selected route that has no observed effect in the session
window. Treat this as a route-evidence gap, not as evidence that the Legacy route
executed or mutated the target process.

As of 2026-08-14, the shared Modern `system_server` installer publishes
target-package-scoped, rate-limited `runtime-transport` events for its
viewport mutation and selected skip paths. Legacy does not install that shared
viewport installer, so its own launch-only font route is not represented by
these events. Transport absence remains an evidence gap, not proof that a
system-side route did not execute.

As of 2026-08-14, Legacy and shared app-process hotpath diagnostics enqueue
transport and LSPosed bridge records away from the hook callback thread. The
transport batches short writes and the DPIS stop path flushes its local queue;
the bridge queue is bounded so diagnostic capture cannot grow without limit.
Queue overflow is an evidence-completeness limitation, not evidence that the
Legacy route did not execute.

The shared app-process font route implementation now lives under
`runtime.font`. Legacy flavor entry points still call the same install/reset
protocols; the move only classifies ActivityThread, Resources font scheduling,
TextView/Paint field rewrite, WebView, typeface replacement, Flutter, and
diagnostic font routes under their runtime owner package.
The shared app-process viewport/window route implementation now lives under
`runtime.appprocess`. Flavor entry points still call the same
install/reset/apply protocols; the move is package classification only and does
not change route selection, mutation policy, or evidence semantics.

- 2026-07-12: Legacy imported typeface diagnostics now match Modern: Provider
  or fallback source, hook installation, first replacement hit per source, and
  load failure are emitted as stable `typeface` timeline stages. This is
  observational only and preserves Legacy hook behavior.


As of 2026-06-22, the Legacy WeChat DPI route participates in feedback
diagnostics as `route=wechat_dpi` for WeChat targets. A saved WeChat DPI config
makes callback and mutation evidence expected; without that config, callback or
mutation hits are unexpected route activity. The legacy load-package route and
DisplayMetrics hook emit structured evidence for route entry, install attempt,
hook ready, callback, applied mutation, and skipped or failed locator paths.

## Full Tree

```text
DPIS viewport target package
  |
  +-- flavor boundary
  |     |
  |     +-- modern
  |     |     |
  |     |     +-- entry: app/src/modern/java/com/dpis/module/ModuleMain.java
  |     |     +-- system_server route:
  |     |     |     runtime.systemserver.SystemServerDisplayEnvironmentInstaller
  |     |     +-- app-process route:
  |     |           runtime.appprocess.AppProcessHookInstaller
  |     |           runtime.appprocess.ResourcesManagerHookInstaller
  |     |           runtime.appprocess.ResourcesImplHookInstaller
  |     |           runtime.appprocess.ResourcesReadHookInstaller
  |     |           runtime.appprocess.DisplayHookInstaller / runtime.appprocess.WindowMetricsHookInstaller
  |     |
  |     +-- legacy
  |           |
  |           +-- entry: app/src/legacy/java/com/dpis/module/LegacyModuleHook.java
  |           +-- system_server route:
  |           |     LegacySystemServerHookInstaller
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
  |                 |     custom hook-chain UI controls these in compat font mode;
  |                 |     resources_font is manual, not automatic
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

The legacy absolute-width viewport route is this coverage model when the
resolved route is `system` or `compat`:

```text
legacy auto/system absolute viewport
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

Legacy main-process auto treats any enabled viewport target as an app-process
compat projection route. This keeps relative-scale targets from falling back to
off when system hooks are unavailable.

legacy does not currently install the shared modern
`runtime.systemserver.SystemServerDisplayEnvironmentInstaller`, so `config-dispatch` and
`display-manager-info` are not active legacy system_server routes. The
legacy system_server installer keeps only the launch-time Configuration
route and the HyperOS Rust process environment route.

Per-app guards (splash startup filter, Display/WindowMetrics disable,
Flutter activity scope) are configured independently per target package.
See `docs/private/` for app-specific investigation notes.

## System Font Route Decision

```text
legacy system font mode
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


System-server route implementation classes now live under `runtime.systemserver`. Flavor entry points still use the same install,
diagnostic, policy, and process-check protocols; this is package
classification only, not a route behavior change.

## Experiment Ledger

| Date | Route | Change | Status | Notes |
| --- | --- | --- | --- | --- |
| 2026-06-01 | system | Restore launch-activity-item config mutation | rejected | Did not hold final viewport state |
| 2026-06-01 | system | ActivityRecord config-dispatch hook | shared-modern evidence | Required for apps whose layout depends on Activity-level config, but not currently installed by legacy |
| 2026-06-01 | system | DisplayManagerInfo UID-gated hook | shared-modern evidence | Required for display metric consumers bypassing app-process Display, but not currently installed by legacy |
| 2026-06-01 | compat | ResourcesKey empty override fill | active | Shared helper; preserves viewport in resource key path |
| 2026-06-01 | compat | App-process Display / WindowMetrics supplement skipped for system mode | current design | Resources bridge still installs; the design gate only applies to display/window supplement hooks |
| 2026-06-01 | legacy | FlutterJNI viewport metrics bridge | active, per-app guarded | Covers Flutter/mixed shells; guard scope per target |
| 2026-06-02 | legacy | LaunchActivityItem post-construction object mutation | active | Aligns legacy launch delivery with modern |
| 2026-06-04 | WeChat target-field | Keep app-specific route alongside generic hooks, share target-field runtime property handling, and add the required write-side companion route for versions that need it | superseded | Detailed version-specific evidence lives in `docs/private/wechat-target-field.md` | Replaced by the WeChat DisplayMetrics DPI route; do not reintroduce target-field hooks without fresh evidence |
| 2026-06-04 | WeChat 8.0.71 target-field | Replace stale constructor-field route with the verified getter/setter route shape | superseded | Shared route registry evidence lives in `docs/private/wechat-target-field.md` | Kept as locator history only |
| 2026-06-07 | font system emulation | Add `system_server_font` as an explicit internal domain for `Configuration.fontScale` | active / superseded fallback | Douyin and Bilibili repros stopped flickering when only system_server font mutation was skipped; app-process font domains still scaled text | Kept as planner/runtime diagnostic state, not a compat custom-chain switch |
| 2026-06-07 | font system emulation | Route `FONT_SCALE` through field-level system_server scheduling and allow it only at `launch-activity-item` | active | Unit policy tests cover viewport multi-entry scheduling and font launch-only scheduling | Avoids later config-dispatch writes that can surface as `CONFIG_FONT_SCALE` relaunches |
| 2026-06-07 | shared app-process viewport | Relative-scale app-process borrow targets preserve small-window dp geometry while applying target density in ResourcesImpl / ResourcesRead metrics | active | Shared unit tests cover ResourcesImpl and ResourcesRead window density compensation | This is package-neutral shared behavior; legacy inherits it through `app/src/main/java` |
| 2026-06-08 | WeChat DPI | Replace the old target-field route with the WeKit-style DisplayMetrics post-processing route as the official WeChat independent path | active | Runtime check confirmed property publication, hook installation, and mutation callback on `q35.f` for 8.0.71; TabIconView supplement was rejected as disproportionate at DPIS custom values; details in `docs/private/wechat-target-field.md` | DPIS now only mutates returned `DisplayMetrics` |
| 2026-06-09 | WeChat DPI | Move method discovery to a shared WeKit-style DexKit locator with the static version table as fallback only | active | Unit/source tests cover the DexKit rule, fallback ownership, and shared runtime mutation formula | Locator matches the `MMDensityManager` / `screenResolution_target_field` signature and logs whether `dexkit` or `static-route` installed hooks |
| 2026-06-09 | WeChat DPI | Shared DexKit locator now falls back to extracted module native library paths when LSPosed cannot load `libdexkit.so` from `base.apk!/lib/...` | active / shared | Modern real-device WeChat 8.0.74 / versionCode 3120 reached `hook ready`, `callback hit`, and `applied` after this shared fix; legacy has source/unit coverage but no separate device run in this note | Keep as shared locator behavior, not a compat-specific route change; 3120 / `j65.f` is also in the static fallback table |
| 2026-06-15 | shared app-process font | Add an event-gated `resources_font` scheduler for Resources read-path font conflicts | active / shared | Bilibili `resources_font`-only repro on Modern showed base/target font-scale alternation in shared Resources read paths; unit tests cover the shared scheduler semantics and Legacy inherits the fix through `app/src/main/java`. TapTap system-font repro later showed the same shared metrics-read boundary: read-side configuration writes must stay off, while metrics should not be downgraded from the target font factor by a lower system config | Read-conflict target suppression outranks Compose base suppression; non-Compose observations must not clear an established read-conflict target state. Compat `resources_font` uses `ResourcesImpl` as a low-frequency metrics seed plus `ResourcesRead` fallback; when `ResourcesRead` is installed only for font it skips viewport target resolution and `VirtualDisplayState` reuse while keeping metrics density synchronized with configuration; system font emulation does not let `ResourcesRead(getConfiguration)` force target `fontScale` on every read, but `ResourcesRead(getDisplayMetrics)` may fill `scaledDensity` from the target factor so read-side metrics do not downgrade an already-targeted font scale; Compose diagnostics can detach after the read-conflict target event is established; `ResourcesManager` write-side hooks remain for viewport and system font emulation |
| 2026-06-26 | Chrome WebAPK owner routing | Shared WebAPK owner extraction exists, but Legacy does not claim Chrome content app-process carrier support | active / partial | Shared unit tests cover conservative owner parsing; Modern device validation proves Android Resources and Chromium Java `WindowAndroid` can see WebAPK owner scale in Chrome, but that probe is installed from Modern package-ready libxposed routing | WebAPK splash package can still receive normal package hooks; Legacy does not add a separate Chrome WebAPK carrier route or promise final renderer scaling |
| 2026-06-27 | Chrome WebAPK logging policy | Keep WebAPK owner evidence low-volume and gate detailed Chromium probes | shared logging rule | Modern now installs the Chromium Java viewport probe only when `debug.dpis.webapk.chromium_probe_package` explicitly matches Chrome in a debug build; shared owner bridge logs only state changes and value changes | Legacy inherits the shared owner parser/log wording but still does not claim Chrome content carrier support |
| 2026-06-28 | Legacy Chrome system marker handoff | Legacy `system` mode on Chrome can publish a fresh system marker, but the visible app-process effect remains metrics-only | active / Chrome-specific evidence | On `192.168.5.131:5555`, Chrome saved as `viewport.mode=system` first logged `LegacyResourcesRead(getConfiguration)` marker miss `reason=stale`, then the same launch observed `result=hit, ageMs=24` plus `LegacyResourcesManager (metrics) override`. `dumpsys activity` still showed Chrome `CurrentConfiguration` at the platform `sw462dp / 374dpi`, so this route proved marker publication/app-process handoff rather than launch-time Activity `Configuration` rewrite | Treat “Legacy Chrome system mode not effective” as “launch-time system marker is consumed by the app-process bridge, but legacy does not currently provide a stronger Chrome system viewport mutation path” rather than as a pure scope/config failure |
| 2026-06-28 | Legacy Chrome stronger launch-time system viewport | Let legacy `launch-activity-item` commit viewport `Configuration` for resolved system-mode packages, not only publish a marker | active | `PerAppDisplayConfigSource` already suppresses compat/off viewport targets in system_server, so `applyConfiguration(configuration, environment)` at `launch-activity-item` strengthens only `system` / `auto->system` launches. Focused unit tests cover `system` rewriting `462x1001@374dpi` to `924x2002@187dpi` and `compat` staying unchanged | This is intentionally stronger than the earlier marker-only legacy behavior. If Chrome still ignores content scaling after this, the remaining boundary is Chromium content consumption rather than launch-time Activity `Configuration` handoff |
| 2026-06-28 | Legacy Chrome launch-item object mutation follow-up | Mirror the modern two-stage launch path in legacy by mutating constructed `LaunchActivityItem` fields (`mCurConfig`, `mOverrideConfig`) after constructor proceed, not only raw constructor args before proceed | active | On `192.168.5.131:5555`, Chrome kept consuming fresh system markers in app-process logs while no legacy `launch-activity-item apply` evidence appeared from the constructor-args path. The likely missing boundary was post-construction object fields on platform builds where the effective launch config is held on the item instance | Keep logs low-frequency and package-scoped. This route is meant to prove whether legacy can hand the stronger launch configuration to Chrome before blaming Chromium renderer consumption |

## Safety Rules

- Changes under `app/src/modern/java/` should not be described as legacy
  behavior.
- Changes under `app/src/main/java/` are shared and must be reviewed for both
  100 and 101.
- For legacy system route, require system_server install evidence plus
  callback/mutation evidence. `hook ready` alone is not enough.
- Per-app stability guards (e.g., splash filter, Display/WindowMetrics disable,
  WebView supplement skip, Flutter activity scope) are configured per target
  package. Do not change default boundaries without per-target evidence.
- Treat Bilibili/Douyin flicker findings as evidence for the generic
  `FONT_SCALE` field policy, not as package-name recommendations. Hook-chain
  restore default clears the compat custom override and returns to the compat
  recommended template; it must not grow a Bilibili/Douyin default list.

## Update Log

- 2026-06-21: shared feedback diagnostic LSPosed timeline semantics now keep
  same-timestamp runtime events in stage order (`begin`, `applied`/`skipped`,
  `end`), and explicit `DPIS_VIEWPORT*` evidence no longer misclassifies to the
  generic font route when the message also mentions `fontScale`. Shared
  diagnostic runtime hotpath evidence now covers selected compat
  viewport/resources boundaries including runtime marker observation,
  `ResourcesManager` override, `ResourcesImpl` observe/override/stable target,
  and `ResourcesRead` configuration/display-metrics overrides.
- 2026-06-21: shared app-process viewport diagnostics now include first-hit plus
  counted-sample `runtime.appprocess.DisplayHookInstaller` and `runtime.appprocess.WindowMetricsHookInstaller`
  runtime-hotpath evidence. Repeated callback evidence carries `hitCount` and
  `suppressedCount`, aimed at rapid-scrolling repros where callback hit versus
  skipped supplement behavior needs to be separated before drawing conclusions.
- 2026-06-21: shared diagnostic log behavior now carries
  `diagnosticLogFingerprint=diag-log-2026-06-21-counted-hotpath-v1` in the app
  hook plan and Display/WindowMetrics supplement readiness/probe evidence. Bump
  `RuntimeDiagnosticLogFingerprint.VALUE` whenever runtime diagnostic log
  semantics change.
- 2026-06-01: initial tracker created.
- 2026-06-01: shared modern route evidence confirmed auto absolute-width route
  needs ActivityRecord config-dispatch plus DisplayManagerInfo in addition to
  app-process Resources/Display supplements. legacy does not currently
  install these two shared modern system_server entries.
- 2026-06-01: added FlutterJNI viewport metrics bridge for Flutter/mixed
  shells; ViewRoot probing is inactive diagnostic evidence.
- 2026-06-02: added LaunchActivityItem post-construction object mutation to
  align legacy launch delivery with modern.
- 2026-06-03: consolidated app-process viewport policy into a domain switch
  with per-app guard configuration.
- 2026-06-04: WeChat target-field route no longer suppresses generic app-process
  hooks; shared target-field runtime property handling now writes and reads with
  persistent fallback. Legacy shares the write-side companion route support
  used by versions that need it.
- 2026-06-08: WeChat target-field hooks were superseded by the official WeChat
  DPI route. The compat app-specific route now hooks no-arg `DisplayMetrics`
  methods on the version-specific WeChat density-manager class and
  post-processes `density`, `densityDpi`, and `scaledDensity` from the
  configured DPI.
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
- 2026-06-22: shared Paint/TextPaint field-rewrite fallback now uses libxposed
  argument replacement instead of a post-call `setTextSize` rewrite. It remains
  independently selectable, but observes TextView/layout owned writes and keeps
  Paint provenance as the fallback write authority. Paint fallback provenance is
  resolved in one tracker pass per write. Active diagnostics include Paint
  fallback caller summaries so shared Legacy/Modern field-rewrite evidence can
  distinguish self-drawn fallback use from stronger TextView/layout candidates.
- 2026-06-07: updated the route map to show system font mode explicitly and to
  split the app-process Resources bridge from the Display / WindowMetrics
  supplement that is skipped when viewport resolves to system.
- 2026-06-07: shared modern system_server package selection is now
  field-aware per entry. This records shared-code behavior for 100/101 review;
  legacy still does not install the shared modern system_server entries.
- 2026-06-07: shared app-process relative-scale borrow handling now keeps
  small-window width/height dp owned by the window manager while deriving target
  density locally for `ResourcesImpl` and `ResourcesRead(getDisplayMetrics)`.
  This avoids using compat app-process routes to publish display baselines from
  a borrowed small-window result.
- 2026-06-08: shared relative-scale app-process consumers now classify matching
  local or target runtime records as borrow targets. This covers flexible-window
  mixed configurations where `smallestWidthDp` has reached the target but
  width/height and density still describe the source window.
- 2026-06-09: shared WeChat DexKit locator gained an extracted native library
  fallback for LSPosed module classloaders. This preserves DPIS legacy native
  packaging used by existing HyperOS native proxy paths while allowing DexKit
  to load from `/data/app/.../lib/<abi>/libdexkit.so` when
  `System.loadLibrary("dexkit")` cannot resolve `base.apk!/lib/...`.
- 2026-06-09: shared review follow-up tightened WeChat DPI config recovery.
  Legacy `wekit_dpi` migration no longer overwrites an official mirror value,
  3120 / `j65.f` is recorded as a static fallback, save failures no longer
  publish runtime properties, and recovery always clears the fixed WeChat
  property pair when no enabled WeChat DPI value exists.
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
- 2026-06-14: shared runtime property fallback no longer lets global debug
  mirrors mark a package as configured. This keeps boot-time app-process hooks
  able to fall back to persisted LSPosed delivery when always-running targets
  start before DPIS has replayed volatile per-package runtime mirrors.
- 2026-06-26: shared app-process Resources and Display hooks can now resolve a
  Chrome WebAPK owner only when the carrier process is `com.android.chrome` and
  launch/lifecycle evidence identifies `SameTaskWebApkActivity` with an
  `org.chromium.webapk.*` owner. The hot path reads that owner's runtime
  property store; Legacy does not add a separate WebAPK carrier route. Modern's
  Chrome package-ready Chromium Java probe is diagnostic evidence for Modern
  only and does not change Legacy support claims.
