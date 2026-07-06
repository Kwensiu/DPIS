# Package Classification Plan

This note records a low-risk plan for classifying the flat
`com.dpis.module` package. It is a maintenance guide and migration ledger. Do
not treat candidate groups as migrated unless they are listed in
"Migrated Packages".

## Goal

Improve source navigation without changing runtime behavior. Avoid widening
class interfaces unless a moved feature already uses the collaborator as a
shared protocol and the public surface is explicitly reviewed.

The current main source package contains many feature-specific classes in one
directory. That makes code search and ownership harder, but moving everything at
once would be risky because most top-level classes are package-private and rely
on the shared `com.dpis.module` package.

## Current Decision

Do not perform a broad package migration as a standalone cleanup.

Instead, classify code incrementally when a feature area is already being
changed. Each migration should be small enough that reviewers can tell whether
the diff is mostly package movement or behavior change.

## Migration Rules

- Prefer behavior-preserving package moves.
- Do not rename classes in the same change unless the feature work already
  requires that semantic correction.
- Do not make package-private classes `public` only to make a move compile.
  If a class must become public, treat that as an interface decision and justify
  it in the change.
- Keep each migrated group internally coherent. A package should represent a
  feature area or runtime layer, not a filename prefix alone.
- Update tests and source smoke tests in the same change when moved classes,
  layout owners, or helper names affect their assertions.
- For viewport, runtime hooks, font routing, package state, log pages, and
  feedback diagnostics, follow `CONTEXT.md` and the relevant living route
  documents before moving route-owned classes.

## Candidate Packages

These are starting points for future migration. They may need adjustment when
the real dependency graph is inspected.

| Candidate package | Likely contents | Notes |
| --- | --- | --- |
| `diagnostics` | `FeedbackDiagnostic*`, `DiagnosticLogGate`, `DpisLog*`, `LsposedLogReader`, runtime diagnostic parsers | Keep diagnostic package file-role semantics aligned with `CONTEXT.md`. |
| `updates` | `Update*`, `ReleaseNotes*`, `GitHubReleaseNotesFetcher`, startup update check classes | Preserve the rule that update availability is not cached. |
| `templates` | `QuickTemplate*`, `Template*`, `GlobalPrefill*` | Watch for shared app-config summary and typeface resolution logic. |
| `appconfig` | `AppConfig*`, config input binders, save semantics, dialog coordinators | Keep sheet draft semantics separate from persisted package state. |
| `applist` | `AppList*`, installed catalog classes, app status formatting | Keep user-visible configured package semantics aligned across home and configured lists. |
| `process` | process start/restart/stop UI actions and launch helpers | Keep user-triggered process actions separate from runtime hook routes. |
| `home` | `Home*`, workspace status binders, activation state | Treat this as UI assembly, not package configuration ownership. |
| `settings` | settings workspace, system hook toggles, launcher icon visibility, startup disclaimer | Split further if it starts mixing unrelated settings flows. |
| `fonts` | `Font*`, `Typeface*`, `SystemFont*`, HyperOS font classes, font debug stats | Do not fold typeface replacement, font scale, and hook domains into one concept. |
| `viewport` | `Viewport*`, `DensityOverride`, viewport target/resolution classes | Keep `ViewportTargetSpec` as the authoritative target representation. |
| `runtime` | runtime property delivery/recovery, runtime markers, hot path evidence | Consider subpackages only after route ownership is clear. |
| `runtime.systemserver` | `SystemServer*`, system route policies, mutation fields, process checks | Read both runtime resync documents before moving shared route code. |
| `runtime.appprocess` | `AppProcess*`, `ActivityThread*`, `Resources*`, `Display*`, `Window*`, WebView and Chromium hooks | Moving these may expose hidden route coupling; inspect before splitting. |
| `quirks` | app-specific special tuning such as `WechatDpi*` | Keep app-specific route evidence visible in diagnostics; package by "special tuning" rather than by one app name. |
| `backup` | `ConfigBackupCodec`, snapshot load/refresh helpers if they remain backup-specific | Do not mix backup serialization with package-state ownership unless the model is being redesigned. |

## Migrated Packages

| Package | Contents | Notes |
| --- | --- | --- |
| `backup` | `ConfigBackupCodec` | First small migration. The codec is now an explicit backup package interface used by settings import/export. |
| `viewport` | `DpiConfig` | Low-risk utility move. Keeps compat viewport seed rules near viewport semantics. |
| `runtime.systemserver` | `LegacySystemServerGate` | Small legacy gate move for system-server installation policy. |
| `runtime.systemserver` | `ConfigSnapshotRefreshPolicy`, `ReflectionProbeCache`, `SystemServerPackageUidResolver` | System-server route support helpers with narrow callers. |
| `runtime.systemserver` | `PerAppDisplayConfig`, `SystemServerDisplayDiagnostics`, `SystemServerDisplayEnvironmentInstaller`, `SystemServerEntryRoute`, `SystemServerHookCatalog`, `SystemServerHookLogGate`, `SystemServerHookSpec`, `SystemServerHotPathInspector`, `SystemServerMutationField`, `SystemServerMutationPolicy`, `SystemServerProcess` | System-server route display value, installation, diagnostics, route catalog, log gating, mutation policy, and process checks. Flavor entries keep calling the same public install/diagnostic protocols; route behavior is unchanged. |
| `runtime` | `CallerTrace` | Runtime probe caller formatting helper shared by app-process probe hooks. |
| `runtime` | `DebugPackageOverride` | Debug-only package override matcher shared by app-process, modern, and system-server debug gates. |
| `runtime` | `RuntimeDiagnosticLogFingerprint` | Stable runtime log fingerprint helper used by app-process display/window hook logs. Moved separately because it has no diagnostic package file-role ownership. |
| `runtime` | `RuntimeClock`, `RuntimeHotPathEvidenceSampler` | Runtime timing and hot-path evidence sampling helpers shared by viewport/display/window hook routes. |
| `runtime` | `ProcessScopedInstallGate` | Runtime hook idempotency gate shared by app-process and system-server installers. |
| `runtime` | `RootCommandRunner` | Shared root command transport used by runtime property syncers. |
| `runtime` | `XSharedPreferencesAdapter` | Read-only legacy Xposed preference adapter used by runtime config-store factories. Public surface is limited to constructors and the `SharedPreferences` contract; reload/snapshot details remain internal. |
| `runtime` | `ModuleRuntimeReloadAdvisor`, `ModuleRuntimeStateReporter`, `RuntimeConfigDelivery`, `RuntimePropertyRecoveryCoordinator`, `XposedSelfActivation` | Small runtime lifecycle helpers for module reload advice, system-server load markers, local snapshot delivery, runtime-property resync, and self-package activation. Public surface is limited to the existing entry methods used by app startup and flavor entry points. |
| `runtime.appprocess` | `ResourcesReadHookPolicy`, `WindowFrameOverride` | App-process route policy/value helpers used by hook planning and frame mutation routes. |
| `runtime.font` | `DebugFontOverride` | Debug-only font override value used by hook planning and app-process font routes. |
| `viewport` | `DisplayOverridePipeline`, `EffectiveModeResolver`, `PerAppDisplayOverrideCalculator` | Viewport strategy and display-environment calculation helpers shared by UI planning, app-process routes, and system-server routes. |
| `root` | `RootAccessProbe` | Shared root availability probe used by status UI, log page, process actions, and feedback diagnostics. |
| `root` | `RootAppProcessLauncher` | Shared root app start/stop/restart launcher used by process actions and feedback diagnostics. The launcher API is public; `ShellResult` exposes immutable accessors instead of public fields. |
| `process` | `ProcessActionHandler` | User-triggered app process start/restart/stop UI handler. Root activities wire the public handler/action protocol; root command execution remains behind `RootAppProcessLauncher`. |
| `fonts` | `HyperOsNativeProxyStatus` | Small status/value object move for HyperOS native proxy detection. |
| `fonts` | `FontFileInspector`, `FontFileKind`, `TtcFontCollectionParser` | Font-file identification cluster with no package-private root dependencies. |
| `fonts` | `ComposeFontRuntimeClassifier`, `FontFieldRewriteMath`, `FontTypefaceLoader`, `PublishedFontFileResolver`, `TextViewFontProvenanceTracker`, `HyperOsNativeAppDetector` | Focused font runtime/file helpers. The package move did not change font route ownership or mode semantics. |
| `fonts` | `SystemFontRegistry`, `SystemFontEntry` | System font discovery and immutable system-font option value. Registry exposes only production lookup/load entry points; test-only ID builders stay package-private inside `fonts`. |
| `fonts` | `PaintProvenanceTracker` | Paint text-size provenance helper shared by font fallback hooks. Production fallback methods are public for hook installers; snapshot state remains package-private in `fonts` tests. |
| `fonts` | `FontDebugDataDiagnostics`, `FontDebugLogcatBridge`, `FontDebugOverlayService`, `FontDebugStats*` | Font-debug overlay, stats ingestion, provider/receiver/service fallbacks, transport, file bridge, store, reporter, and schema. Manifest component ownership moved to `.fonts.*`; root callers use public debug-stat transport/store/config seams while implementation details remain in `fonts`. |
| `fonts` | `HyperOsNativeProxyBindMounter` | HyperOS native proxy mount command planner/executor used by root-owned UI flows. Production callers use public plan/result accessors; command construction helpers stay package-private in `fonts` tests. |
| `fonts` | `HyperOsNativeProxyAssetExporter` | HyperOS native proxy asset export helper used by app startup and package lifecycle refresh. Logging stays root-owned through a narrow `Logger` callback, so `DpisLog` remains package-private. |
| `fonts` | `FontLibraryEntry`, `FontLibraryStore` | Imported-font library metadata and storage. `FontLibraryActivity` stays in the root manifest/component boundary; delete reference checks are supplied by root callers so `DpisConfigStore` remains package-private. |
| `fonts` | `FontApplyMode` | Font apply-mode value helper. It moved in a dedicated wave because config storage, app lists, diagnostics, runtime delivery, and tests all consume it. |
| `fonts.hookdomain` | `FontHookArbitration`, `FontHookDomain*`, `PackageFontHookDomainDefaults` | Font hook-domain planning, presentation, runtime-property bridge, and package defaults. Cross-package callers use the existing domain/plan protocol; hook installer behavior is unchanged. |
| `runtime.font` | `FontRuntimePropertySyncer`, `CompatFontPropertySyncer`, `HyperOsNativeFontPropertySyncer`, `ComposeResourcesFontEvidence`, `ResourcesFontScheduler`, `FontScaleOverride` | Font runtime property delivery, resources-font evidence/scheduling, and scale override helpers. This groups runtime font support without changing route ownership. |
| `runtime.font` | `ActivityThreadFontHookInstaller`, `ComposeFontRuntimeDiagnosticsInstaller`, `ForceTextSizeHookInstaller`, `FlutterSettingsFontHookInstaller`, `HyperOsFlutterFontHookInstaller`, `PaintTextSizeFallbackHookInstaller`, `TypefaceOverrideHookInstaller`, `WebViewFontHookInstaller` | App-process font hook installers moved as a route-owned runtime package. Flavor entries and root app-process orchestrators keep calling the same install/reset protocols; runtime behavior is unchanged. |
| `updates` | `GitHubReleaseNotesFetcher`, `ReleaseNotesMarkdownLite`, `ReleaseNotesMarkdownRenderer`, `ReleaseNotesCacheStore`, `ReleaseNotesController`, `StartupUpdateCheckCoordinator`, `StartupUpdateCheckOnce`, `StartupUpdateDownloadExecutor`, `StartupUpdateManifest`, `StartupUpdatePackageHandler`, `UpdateAvailableDialog`, `UpdateCoordinator`, `UpdateDownloadCoordinator`, `UpdateManifestFetcher`, `UpdatePackageInstaller`, `UpdatePromptDialogCoordinator`, `UpdateStateStore` | Full update feature package. Public surface is limited to existing `MainActivity`, `AboutActivity`, cache cleanup, and test entry points. |
| `home` | `HomePrimaryStatusClusterLayout`, `HomeUpdateUiState`, `HomeWorkspaceBinder` | Home workspace view/status helpers. `HomeWorkspaceBinder.State` and `Actions` are the public MainActivity-to-home UI contract; Activity components stay in the manifest-owned root package. |
| `home` | `DonateActivity` | Donate/supporter page component moved with the home donation entry. Manifest ownership now points at `.home.DonateActivity`; callers continue using `DonateActivity.createIntent`. |
| `home` | `ModeHelpActivity`, `ModeGuideActivity` | Home mode help/guide components moved with the home workspace help entry. Manifest ownership now points at `.home.*` components. |
| `home` | `HomeActivationStateResolver` | Home activation is now a pure home-owned resolver. Root-owned `MainActivity` supplies Xposed service/self-load evidence and owns diagnostic logging, so `DpisApplication` and `DpisLog` internals stay root-local. |
| `appconfig` | `AppConfigSheetWizardStore`, `AppConfigDialogCoordinator`, `ConfigValueInputErrorBinder`, `UnsavedBadgeBinder` | App-config sheet helpers with narrow callers and no root package-private dependencies. |
| `appconfig` | `AppConfigDialogBinder`, `AppConfigInputValidation`, `AppConfigPrefillPreview`, `AppConfigSaveHandler`, `AppConfigSheetActionBinder`, `AppConfigSheetInteractions`, `AppConfigSheetModeValidationBinder`, `ConfigDraftSaveSemantics` | App-config dialog binding, validation, save handling, prefill preview, and draft-save semantics now live with the sheet owner. Public seams are the existing dialog/save APIs used by main, quick, and template flows; sheet action/validation binders remain package-private. |
| `appconfig` | `WechatDpiConfig` | WeChat DPI package/id and input normalization helper. It is public because root UI, config storage, and flavor hook routers share the same package predicate and DPI bounds; parsing and normalization remain behaviorally unchanged. |
| `applist` | `AppLoadCoordinator`, `AppIconMemoryCache`, `ForegroundPackageResolver`, `InstalledAppCatalogItem` | App-list loading/version coordination, foreground resolution, and catalog value helpers. |
| `applist` | `AppListFilterState`, `AppListFilterStateStore` | App-list filter value and preference persistence. Root UI code consumes this as a narrow public filter-state contract with accessors; preference keys and storage details stay inside `applist`. |
| `templates` | `QuickTemplateApplyConfirmationMessage`, `QuickTemplateTargetCarrierState`, `QuickTemplateTargetSelectionContract` | Pure quick-template state/message helpers and the public target-selection navigation/result contract. Broader template UI binders remain with their current UI state cluster. |
| `templates` | `BatchScopeRequestCoordinator` | Quick-template batch scope request coordinator. It uses the existing public Xposed service accessor and exposes only the host/request entry protocol needed by root UI assembly. |
| `templates` | `QuickTemplateApplyCoordinator` | Seam-first quick-template apply coordinator. The templates package owns target sanitizing, overwrite planning, and apply result semantics through generic writer/publisher contracts; root-owned `QuickTemplateApplyAdapters` keeps `DpisConfigStore`, package repository, and runtime property side effects out of the templates package. |
| `templates` | `TemplateConfigValue`, `TemplateConfigPreferences`, `TemplateCustomSemantics`, `TemplateConfigSummaryFormatter`, `QuickTemplateStore`, `QuickTemplateSaveHandler`, `GlobalPrefillStore`, `GlobalPrefillSaveHandler` | Template config intent, preference codec, summary, store, and save semantics. The template package owns primitive/string draft parsing and persistence; root-owned `TemplateConfigValueAdapters` maps template intent to viewport/package strong types so app-config/runtime boundaries stay package-private. |
| `templates` | `TemplateSummaryChipBinder`, `QuickTemplateListAdapter`, `QuickTemplateSortDialog`, `TemplateTypefaceResolver` | Template presentation helpers that depend on Android resources, public UI utilities, template models, and imported-typeface resolution. |
| `templates` | `TemplateWorkspaceBinder`, `GlobalPrefillSheetDialog`, `GlobalPrefillEditorBinder`, `QuickTemplateEditSheetDialog`, `QuickTemplateEditorBinder`, `QuickTemplateTargetsBinder`, `QuickTemplateTargetAdapter`, `QuickTemplateApplyAdapters`, `TemplateConfigValueAdapters`, `QuickTemplateTargetSelectionActivity` | Quick-template and global-prefill workspace/editor/target/application flows now live with the rest of template ownership. This one-step closure required making the existing app-config, app-list, viewport, font-hook, runtime-sync, and logging protocols public where templates already reused them; the public surface is a compatibility seam for the migrated feature, not a behavior change. Manifest ownership for `QuickTemplateTargetSelectionActivity` moved to `.templates.QuickTemplateTargetSelectionActivity`. |
| `settings` | `AppLocaleManager` | Manual language selector helper moved without changing locale tags or string semantics. |
| `settings` | `ExperimentalSettingsActivity`, `ExperimentalSettingsStore` | Experimental settings moved behind a feature store seam. `ConfigStoreFactory` adapts root-owned `DpisConfigStore` TTC methods to the settings store instead of making those store internals public. |
| `settings` | `ToolsWorkspaceBinder`, `SystemFontScaleToolBinder`, `SystemFontScaleSettingsGateway`, `SystemFontScaleToolState`, `SystemFontScaleWriter` | Tools workspace system-font-scale tool cluster. `ToolsWorkspaceBinder.Host` keeps root-owned diagnostics, haptics, and inset helpers package-private while system-font-scale internals stay package-private inside `settings`. |
| `settings` | `AppUiScaleManager`, `InterfaceScaleStore`, `LauncherIconVisibilityStore`, `SafeCacheCleaner`, `StartupDisclaimerStore` | UI preference/settings stores and cache cleanup tooling. Legacy `dpi_config` key reads stay behind a settings-local compatibility helper so `DpisConfigStore` remains package-private. |
| `settings` | `SystemHookState`, `SystemHookStateResolver`, `SystemHookEffectiveView` | Pure system-hook settings/status resolution values. Root-owned toggle controllers and scope gateways stay in root because they still own `DpisConfigStore`, service, and scope side effects. |
| `ui` | `MaxHeightNestedScrollView` | Public custom view used by XML inflation and dynamic dialog content. The move only changes the fully qualified widget tag/imports; measurement behavior and styleable names stay unchanged. |
| `ui` | `FormInputFocusBinder` | Shared form focus/IME helper used by app-config sheet binders. Public surface is limited to existing static focus helpers; hit-testing and IME details remain internal UI behavior. |
| `ui` | `DialogWindowSizer` | Shared AlertDialog width helper. Existing compact/standard/large sizing entry points are public UI API; preset resolution and width math remain internal except the already-tested calculation helper. |
| `ui` | `WindowInsetsBinder` | Shared edge-to-edge inset helper for activity and workspace surfaces. Public surface is limited to existing padding/margin binding methods; inset listener implementation stays internal. |
| `ui` | `TouchFeedbackBinder` | Shared touch feedback helper for haptic and press-scale affordances. Public surface is limited to existing binding methods; animation and haptic constant selection stay internal. |
| `diagnostics` | `FeedbackDiagnosticForegroundAppReader` | Root-backed foreground package snapshot reader for feedback diagnostics. Only `readForegroundPackage()` is public for the coordinator; parser details remain package-private in the diagnostics package. |
| `diagnostics` | `FeedbackDiagnosticAppLauncher` | Root-backed app restart adapter used by feedback diagnostic flows. Public surface is limited to construction and `restartForDiagnostic`; root command details stay behind `RootAppProcessLauncher`. |
| `diagnostics` | `LogReadResult`, `LsposedLogReader` | LSPosed log read result and current-log reader. The reader exposes only `readLsposedDpisCurrent()`; result formatting takes an Android `Context` so diagnostics does not depend on root-owned activity subclasses. |
| `diagnostics` | `DpisAppLogStore`, `DpisLogEntry`, `DpisLogParser` | Central app-log store, diagnostic log value, and LSPosed DPIS parser. Root startup registers the app-log store through the package-private `DpisLog.AppLogSink` seam without exposing that sink outside the root package. |
| `diagnostics` | `FeedbackDiagnosticSessionWindow` | Diagnostic log-window value shared by feedback export and LSPosed timeline parsing. Public surface is limited to `around`, timestamp accessors, and `contains`; margin constants remain diagnostics-local. |
| `diagnostics` | `FeedbackDiagnosticTimelineClassifier` | Runtime/LSPosed diagnostic event classifier. Root callers map feedback requests into a diagnostics `Context`, so the classifier does not depend on root-owned request internals. Event state is exposed through accessors, not public fields. |
| `diagnostics` | `FeedbackDiagnosticSummaryBuilder` | Feedback summary text formatter. Root coordinator maps its request into a diagnostics `Input`, so summary formatting does not depend on root-owned request or viewport models. |
| `diagnostics` | `FeedbackDiagnosticLsposedTimelineParser` | LSPosed feedback timeline parser and windowed raw-log formatter. Root export code maps coordinator state into a diagnostics `Input`, so parser ownership moved without exposing app-list, config-store, viewport, or font-mode internals. |
| `diagnostics` | `FeedbackDiagnosticCoordinator`, `FeedbackDiagnosticExportBuilder`, `FeedbackDiagnosticResultSheet`, `FeedbackDiagnosticRuntimeEvents`, `FeedbackDiagnosticRuntimeHotPathEvents`, `FeedbackDiagnosticRuntimeSelfTest`, `FeedbackDiagnosticRuntimeTransport` | Feedback orchestration, package export, result presentation, and runtime capture moved behind the diagnostics package seam. Root UI calls the public coordinator/export/result-sheet interfaces; runtime routes call the public event recorders. Diagnostic internals remain package-local. |
| `runtime` | `RuntimeDebugPropertyBridge`, `RuntimeDebugPropertySyncer` | Runtime debug system-property bridge and publisher. Root callers use public read/publish entry points; property names, command construction, and root execution stay package-private inside `runtime`. |
| `runtime` | `DpisPackageLifecycleReceiver` | Package lifecycle receiver moved with runtime recovery ownership. `ConfigStoreFactory.createPackageLifecycleConfigStore` is the narrow store seam for this manifest component, instead of exposing generic local-store construction. |
| `runtime` | `WechatDpiPropertyBridge` | WeChat DPI runtime property-name and read bridge used by syncer and flavor hook installers. Public surface is limited to property-name/read helpers; property lookup and parsing remain private. This is a package move only, not a route behavior change. |
| `quirks` | `WechatDpiMethodLocator`, `WechatDpiPropertySyncer`, `WechatDpiRoutes`, `WechatDpiRuntime`, `WechatDpiSheetBinder` | WeChat DPI special-tuning cluster. The package is deliberately named for app-specific quirks instead of `wechat`, so future per-app special cases can share the same boundary. `WechatDpiSheetBinder.bind` takes a package name instead of `AppListItem` so app-list models stay root-owned. |
| `runtime.hookapi` | `ModernApiCapabilities`, `ModernApi101Capabilities`, `ModernApi102Capabilities`, `ModernApiCapabilitiesResolver` | Modern libxposed capability gating and stable-hook-id helpers. The package owns framework capability detection; root hook installers consume the public capability protocol. |
| `applist` | `AppListFilter`, `AppListItem`, `AppListPage`, `AppListPagerAdapter`, `AppListVisibleSections` | App-list row model, filtering, paging, and visibility rules. This closes the remaining `AppList*` cluster; root UI still assembles pages through public adapter/model APIs. |
| `applist` | `AppStatusFormatter` | App-list status formatter moved with row/status presentation semantics. It still consumes public viewport/font mode protocols, but no longer keeps status formatting in the root package. |
| `hooks` | `HookDomainOverride`, `HookDomainOverrideStore`, `HookDomainPlan`, `HookExecutionPlan`, `HookExecutionPlanner`, `HookRuntimePolicy` | Hook-domain override storage and hook execution planning. Existing plan/value fields remain public because root and flavor installers already treat them as direct plan protocols. |
| `viewport` | `Viewport*`, `DensityOverride`, `VirtualDisplay*`, `PerAppDisplayEnvironment`, `TargetViewportWidthResolver`, `AppProcessViewportStateSeeder` | Viewport target model, property bridge, runtime marker state, and shared viewport geometry helpers. Route installers still live in root/flavor entry packages; they consume viewport protocols without changing route behavior. |
| `runtime.appprocess` | `ChromiumViewportProbeHookInstaller` | Chromium/WebAPK viewport probe installer moved with app-process runtime ownership. The public install/reset methods are the existing hook entry protocol used by `ModuleMain` and hot-reload reset. |
| `runtime.appprocess` | `AppProcessHookInstaller`, `AppProcessHotReloadResetter`, `WebApkCarrierResolver`, `WebApkRuntimeOwnerBridge`, `Resources*HookInstaller`, `DisplayHookInstaller`, `Window*HookInstaller`, `ViewRootProbeHookInstaller` | Shared app-process route orchestration and Resources/Display/Window/WebAPK hook helpers moved under the app-process runtime owner. Flavor entry points still call the same install/reset/apply protocols; route behavior is unchanged. |

## Deferred Candidates

These classes looked low-risk by reference count, but should stay in
`com.dpis.module` until their adjacent collaborators move together.

### Quick/Template Closure Decision

The quick-template and global-prefill cluster is now closed under
`com.dpis.module.templates`. The move was broader than the original
seam-first preference because the cleanup goal changed to one-round closure for
the remaining files. Reviewers should treat newly public root collaborators as
existing cross-package protocols used by the migrated template workflows.

The same caution still applies to other large clusters: `applist`, `appconfig`, main UI
state, font hook-domain, diagnostics, and config snapshot helpers are real
future packages, but each needs a narrow public contract before a broad move.
Treat a direct move that forces root models public as a failed migration shape,
not as progress toward package cleanup.

| Class | Reason |
| --- | --- |
| `PackageFontHookDomainDefaults` | Still root-owned because it bridges package-name defaults with font-domain registry semantics. It is public only as a helper consumed by the migrated hook planner. |
| `HyperOsNativeFontPropertySyncer` | Tied to HyperOS font bridges, root command publishing, config store semantics, and tests; move with the related font property bridge cluster. |
| `MainUiAction`, `MainUiState`, `MainViewModel` | This is a coherent main UI state machine and should not be moved piecemeal. The former single-case effect file was merged into `MainViewModel.AppsLoadRequest`, and the former workspace-mode enum file was merged into `MainUiState.WorkspaceMode`. |
| `ConfigSnapshot`, `ConfigSnapshotLoader`, `PackageConfigSnapshot`, `PerAppDisplayConfigSource` | Config snapshot/source ownership still needs a dedicated seam wave. The former refreshing snapshot provider file was merged into `PerAppDisplayConfigSource.RefreshingSnapshotProvider` because it is only a TTL implementation of that source's provider protocol. |
| `CompatFontPropertySyncer` | Looks unused by production callers, but its command builder depends on package-private `DpisConfigStore`, `FontApplyMode`, and HyperOS font bridge helpers. Move with the font property bridge/config boundary, not as a standalone test helper. |
| `HyperOsNativeProxyRefreshCoordinator` | Semantically adjacent to the native proxy mounter, but it depends on package-private `DpisConfigStore`, `DpisLog`, and startup/package-update side-effect rules. Keep it root-owned until that refresh policy has an explicit public seam. |
| `SystemServerSettingsPageController`, `SystemHooksToggleController` | Settings UI slice is coherent, but the current page controller still owns `DpisConfigStore`, service/scope gateways, haptics, cache cleanup, and package-private settings helpers. The former settings workspace forwarding binder was merged into `MainActivity` because it only lazy-created and forwarded lifecycle events to the page controller. |
| `PerAppDisplayConfigSource` | This is still the runtime config-source seam for system-server and legacy fallbacks. Move only when config-source ownership is isolated from root store construction. |
| `FontMode`, `PlanReason` | Hook-planner support values now live with `HookExecutionPlan` and `HookExecutionPlanner` under `hooks`. |
| Public Android components such as `MainActivity`, `QuickConfigActivity`, activities, services, providers, and receivers | Package moves would change manifest component class names and may affect shortcuts, QS tiles, or external component references. Handle in a component-routing migration, not this package cleanup. |

## Naming Cleanup

Some class names may no longer describe the best domain concept. Do not fix that
as part of the first package classification pass.

When a name looks wrong:

1. Keep the class name during the package move.
2. Add a TODO in the feature change or follow-up issue explaining the mismatch.
3. Rename later in a focused semantic cleanup with tests.

This keeps package movement separate from concept correction and makes
regressions easier to isolate.

## Suggested First Moves

Start with packages that have clearer feature ownership and lower runtime risk:

1. `updates`
2. `templates`
3. `diagnostics`, only if the change is already touching diagnostic packaging
4. `appconfig` or `applist`, only when updating the related UI/test smoke checks

Delay broad runtime and font package moves until the relevant route or font
work is already in progress. Those areas carry product semantics beyond file
organization.

## Done Criteria For Each Migration

- The moved group compiles without broad visibility widening.
- Existing behavior tests and relevant source/layout smoke tests pass.
- The package name matches the owning feature or runtime layer.
- Any uncertain or misleading names are recorded for follow-up instead of being
  silently changed.
- The final diff does not mix unrelated package moves with behavior changes.
