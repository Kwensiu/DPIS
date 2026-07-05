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
| `home` | `Home*`, workspace status binders, activation state | Treat this as UI assembly, not package configuration ownership. |
| `settings` | settings workspace, system hook toggles, launcher icon visibility, startup disclaimer | Split further if it starts mixing unrelated settings flows. |
| `fonts` | `Font*`, `Typeface*`, `SystemFont*`, HyperOS font classes, font debug stats | Do not fold typeface replacement, font scale, and hook domains into one concept. |
| `viewport` | `Viewport*`, `DensityOverride`, viewport target/resolution classes | Keep `ViewportTargetSpec` as the authoritative target representation. |
| `runtime` | runtime property delivery/recovery, runtime markers, hot path evidence | Consider subpackages only after route ownership is clear. |
| `runtime.systemserver` | `SystemServer*`, system route policies, mutation fields, process checks | Read both runtime resync documents before moving shared route code. |
| `runtime.appprocess` | `AppProcess*`, `ActivityThread*`, `Resources*`, `Display*`, `Window*`, WebView and Chromium hooks | Moving these may expose hidden route coupling; inspect before splitting. |
| `wechat` | `WechatDpi*` | Keep app-specific route evidence visible in diagnostics. |
| `backup` | `ConfigBackupCodec`, snapshot load/refresh helpers if they remain backup-specific | Do not mix backup serialization with package-state ownership unless the model is being redesigned. |

## Migrated Packages

| Package | Contents | Notes |
| --- | --- | --- |
| `backup` | `ConfigBackupCodec` | First small migration. The codec is now an explicit backup package interface used by settings import/export. |
| `viewport` | `DpiConfig` | Low-risk utility move. Keeps compat viewport seed rules near viewport semantics. |
| `runtime.systemserver` | `LegacySystemServerGate` | Small legacy gate move for system-server installation policy. |
| `runtime.systemserver` | `ConfigSnapshotRefreshPolicy`, `ReflectionProbeCache`, `SystemServerPackageUidResolver` | System-server route support helpers with narrow callers. |
| `runtime` | `CallerTrace` | Runtime probe caller formatting helper shared by app-process probe hooks. |
| `runtime` | `DebugPackageOverride` | Debug-only package override matcher shared by app-process, modern, and system-server debug gates. |
| `runtime` | `RuntimeDiagnosticLogFingerprint` | Stable runtime log fingerprint helper used by app-process display/window hook logs. Moved separately because it has no diagnostic package file-role ownership. |
| `runtime` | `RuntimeClock`, `RuntimeHotPathEvidenceSampler` | Runtime timing and hot-path evidence sampling helpers shared by viewport/display/window hook routes. |
| `runtime` | `ProcessScopedInstallGate` | Runtime hook idempotency gate shared by app-process and system-server installers. |
| `runtime` | `RootCommandRunner` | Shared root command transport used by runtime property syncers. |
| `runtime` | `XSharedPreferencesAdapter` | Read-only legacy Xposed preference adapter used by runtime config-store factories. Public surface is limited to constructors and the `SharedPreferences` contract; reload/snapshot details remain internal. |
| `root` | `RootAccessProbe` | Shared root availability probe used by status UI, log page, process actions, and feedback diagnostics. |
| `root` | `RootAppProcessLauncher` | Shared root app start/stop/restart launcher used by process actions and feedback diagnostics. The launcher API is public; `ShellResult` exposes immutable accessors instead of public fields. |
| `fonts` | `HyperOsNativeProxyStatus` | Small status/value object move for HyperOS native proxy detection. |
| `fonts` | `FontFileInspector`, `FontFileKind`, `TtcFontCollectionParser` | Font-file identification cluster with no package-private root dependencies. |
| `fonts` | `ComposeFontRuntimeClassifier`, `FontFieldRewriteMath`, `FontTypefaceLoader`, `PublishedFontFileResolver`, `TextViewFontProvenanceTracker`, `HyperOsNativeAppDetector` | Focused font runtime/file helpers. The package move did not change font route ownership or mode semantics. |
| `fonts` | `SystemFontRegistry`, `SystemFontEntry` | System font discovery and immutable system-font option value. Registry exposes only production lookup/load entry points; test-only ID builders stay package-private inside `fonts`. |
| `fonts` | `PaintProvenanceTracker` | Paint text-size provenance helper shared by font fallback hooks. Production fallback methods are public for hook installers; snapshot state remains package-private in `fonts` tests. |
| `fonts` | `FontDebugStatsSchema` | Font debug stats schema and preference/extra conversion helper. The schema is a deliberate public constants/static-method API shared by root-owned debug components; field tables and parsing helpers remain internal. |
| `fonts` | `HyperOsNativeProxyBindMounter` | HyperOS native proxy mount command planner/executor used by root-owned UI flows. Production callers use public plan/result accessors; command construction helpers stay package-private in `fonts` tests. |
| `fonts` | `HyperOsNativeProxyAssetExporter` | HyperOS native proxy asset export helper used by app startup and package lifecycle refresh. Logging stays root-owned through a narrow `Logger` callback, so `DpisLog` remains package-private. |
| `updates` | `GitHubReleaseNotesFetcher`, `ReleaseNotesMarkdownLite`, `ReleaseNotesMarkdownRenderer`, `ReleaseNotesCacheStore`, `ReleaseNotesController`, `StartupUpdateCheckCoordinator`, `StartupUpdateCheckOnce`, `StartupUpdateDownloadExecutor`, `StartupUpdateManifest`, `StartupUpdatePackageHandler`, `UpdateAvailableDialog`, `UpdateCoordinator`, `UpdateDownloadCoordinator`, `UpdateManifestFetcher`, `UpdatePackageInstaller`, `UpdatePromptDialogCoordinator`, `UpdateStateStore` | Full update feature package. Public surface is limited to existing `MainActivity`, `AboutActivity`, cache cleanup, and test entry points. |
| `home` | `HomePrimaryStatusClusterLayout`, `HomeUpdateUiState`, `HomeWorkspaceBinder` | Home workspace view/status helpers. `HomeWorkspaceBinder.State` and `Actions` are the public MainActivity-to-home UI contract; Activity components stay in the manifest-owned root package. |
| `appconfig` | `AppConfigSheetWizardStore`, `AppConfigDialogCoordinator`, `ConfigValueInputErrorBinder`, `UnsavedBadgeBinder` | App-config sheet helpers with narrow callers and no root package-private dependencies. |
| `appconfig` | `WechatDpiConfig` | WeChat DPI package/id and input normalization helper. It is public because root UI, config storage, and flavor hook routers share the same package predicate and DPI bounds; parsing and normalization remain behaviorally unchanged. |
| `applist` | `AppLoadCoordinator`, `AppIconMemoryCache`, `ForegroundPackageResolver`, `InstalledAppCatalogItem` | App-list loading/version coordination, foreground resolution, and catalog value helpers. |
| `applist` | `AppListFilterState`, `AppListFilterStateStore` | App-list filter value and preference persistence. Root UI code consumes this as a narrow public filter-state contract with accessors; preference keys and storage details stay inside `applist`. |
| `templates` | `QuickTemplateApplyConfirmationMessage`, `QuickTemplateTargetCarrierState`, `QuickTemplateTargetSelectionContract` | Pure quick-template state/message helpers and the public target-selection navigation/result contract. Broader template UI binders remain with their current UI state cluster. |
| `templates` | `QuickTemplateApplyCoordinator` | Seam-first quick-template apply coordinator. The templates package owns target sanitizing, overwrite planning, and apply result semantics through generic writer/publisher contracts; root-owned `QuickTemplateApplyAdapters` keeps `DpisConfigStore`, package repository, and runtime property side effects out of the templates package. |
| `templates` | `TemplateConfigValue`, `TemplateConfigPreferences`, `TemplateCustomSemantics`, `TemplateConfigSummaryFormatter`, `QuickTemplateStore`, `QuickTemplateSaveHandler`, `GlobalPrefillStore`, `GlobalPrefillSaveHandler` | Template config intent, preference codec, summary, store, and save semantics. The template package owns primitive/string draft parsing and persistence; root-owned `TemplateConfigValueAdapters` maps template intent to viewport/package strong types so app-config/runtime boundaries stay package-private. |
| `templates` | `TemplateSummaryChipBinder`, `QuickTemplateListAdapter`, `QuickTemplateSortDialog`, `TemplateTypefaceResolver` | Template presentation helpers that depend on Android resources, public UI utilities, template models, and imported-typeface resolution. |
| `templates` | `TemplateWorkspaceBinder`, `GlobalPrefillSheetDialog`, `GlobalPrefillEditorBinder`, `QuickTemplateEditSheetDialog`, `QuickTemplateEditorBinder`, `QuickTemplateTargetsBinder`, `QuickTemplateTargetAdapter`, `QuickTemplateApplyAdapters`, `TemplateConfigValueAdapters`, `QuickTemplateTargetSelectionActivity` | Quick-template and global-prefill workspace/editor/target/application flows now live with the rest of template ownership. This one-step closure required making the existing app-config, app-list, viewport, font-hook, runtime-sync, and logging protocols public where templates already reused them; the public surface is a compatibility seam for the migrated feature, not a behavior change. Manifest ownership for `QuickTemplateTargetSelectionActivity` moved to `.templates.QuickTemplateTargetSelectionActivity`. |
| `settings` | `AppLocaleManager` | Manual language selector helper moved without changing locale tags or string semantics. |
| `settings` | `ToolsWorkspaceBinder`, `SystemFontScaleToolBinder`, `SystemFontScaleSettingsGateway`, `SystemFontScaleToolState`, `SystemFontScaleWriter` | Tools workspace system-font-scale tool cluster. `ToolsWorkspaceBinder.Host` keeps root-owned diagnostics, haptics, and inset helpers package-private while system-font-scale internals stay package-private inside `settings`. |
| `settings` | `AppUiScaleManager`, `InterfaceScaleStore`, `LauncherIconVisibilityStore`, `StartupDisclaimerStore` | UI preference/settings stores. Legacy `dpi_config` key reads stay behind a settings-local compatibility helper so `DpisConfigStore` remains package-private. |
| `settings` | `SystemHookState`, `SystemHookStateResolver`, `SystemHookEffectiveView` | Pure system-hook settings/status resolution values. Root-owned toggle controllers and scope gateways stay in root because they still own `DpisConfigStore`, service, and scope side effects. |
| `ui` | `MaxHeightNestedScrollView` | Public custom view used by XML inflation and dynamic dialog content. The move only changes the fully qualified widget tag/imports; measurement behavior and styleable names stay unchanged. |
| `ui` | `FormInputFocusBinder` | Shared form focus/IME helper used by app-config sheet binders. Public surface is limited to existing static focus helpers; hit-testing and IME details remain internal UI behavior. |
| `ui` | `DialogWindowSizer` | Shared AlertDialog width helper. Existing compact/standard/large sizing entry points are public UI API; preset resolution and width math remain internal except the already-tested calculation helper. |
| `ui` | `WindowInsetsBinder` | Shared edge-to-edge inset helper for activity and workspace surfaces. Public surface is limited to existing padding/margin binding methods; inset listener implementation stays internal. |
| `ui` | `TouchFeedbackBinder` | Shared touch feedback helper for haptic and press-scale affordances. Public surface is limited to existing binding methods; animation and haptic constant selection stay internal. |
| `diagnostics` | `FeedbackDiagnosticForegroundAppReader` | Root-backed foreground package snapshot reader for feedback diagnostics. Only `readForegroundPackage()` is public for the coordinator; parser details remain package-private in the diagnostics package. |
| `diagnostics` | `FeedbackDiagnosticAppLauncher` | Root-backed app restart adapter used by feedback diagnostic flows. Public surface is limited to construction and `restartForDiagnostic`; root command details stay behind `RootAppProcessLauncher`. |
| `diagnostics` | `LogReadResult`, `LsposedLogReader` | LSPosed log read result and current-log reader. The reader exposes only `readLsposedDpisCurrent()`; result formatting takes an Android `Context` so diagnostics does not depend on root-owned activity subclasses. |
| `diagnostics` | `DpisLogEntry`, `DpisLogParser` | Central diagnostic log value and LSPosed DPIS parser. The parser exposes only `parseLsposedDpis`; parsing helpers stay private. `DpisAppLogStore` remains root-owned because it implements the package-private `DpisLog.AppLogSink`. |
| `diagnostics` | `FeedbackDiagnosticSessionWindow` | Diagnostic log-window value shared by feedback export and LSPosed timeline parsing. Public surface is limited to `around`, timestamp accessors, and `contains`; margin constants remain diagnostics-local. |
| `diagnostics` | `FeedbackDiagnosticTimelineClassifier` | Runtime/LSPosed diagnostic event classifier. Root callers map feedback requests into a diagnostics `Context`, so the classifier does not depend on root-owned request internals. Event state is exposed through accessors, not public fields. |
| `diagnostics` | `FeedbackDiagnosticSummaryBuilder` | Feedback summary text formatter. Root coordinator maps its request into a diagnostics `Input`, so summary formatting does not depend on root-owned request or viewport models. |
| `diagnostics` | `FeedbackDiagnosticLsposedTimelineParser` | LSPosed feedback timeline parser and windowed raw-log formatter. Root export code maps coordinator state into a diagnostics `Input`, so parser ownership moved without exposing app-list, config-store, viewport, or font-mode internals. |
| `runtime` | `RuntimeDebugPropertyBridge`, `RuntimeDebugPropertySyncer` | Runtime debug system-property bridge and publisher. Root callers use public read/publish entry points; property names, command construction, and root execution stay package-private inside `runtime`. |
| `runtime` | `WechatDpiPropertyBridge` | WeChat DPI runtime property-name and read bridge used by syncer and flavor hook installers. Public surface is limited to property-name/read helpers; property lookup and parsing remain private. This is a package move only, not a route behavior change. |

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
| `PackageFontHookDomainDefaults` | Depends on package-private `FontHookDomainRegistry`; moving it alone would force unrelated font registry visibility changes. |
| `HyperOsNativeFontPropertySyncer` | Tied to HyperOS font bridges, root command publishing, config store semantics, and tests; move with the related font property bridge cluster. |
| `BatchScopeRequestCoordinator` | A trial move to `templates` required exposing `DpisApplication.getXposedService()`. Keep it in the root package until scope-service access is isolated behind a public seam. |
| `FontApplyMode` | Pure value helper but extremely high fan-out across config storage, app lists, diagnostics, runtime delivery, and tests. Move only in a dedicated font configuration model wave. |
| `FontMode`, `PlanReason` | Small types, but owned by the hook planner cluster (`HookExecutionPlanner`, `HookExecutionPlan`). Move them with the planner rather than as isolated public enums. |
| `MainUiAction`, `MainUiEffect`, `MainUiState`, `MainViewModel`, `MainWorkspaceMode` | This is a coherent main UI state machine and should not be moved piecemeal. Moving it as a group is still blocked by package-private app-list state (`AppListItem`, `AppListPage`, `AppListVisibleSections`). |
| `AppListFilter`, `AppListPage`, `AppListVisibleSections` | These are app-list domain candidates, but `AppListFilter` still depends on package-private `FontApplyMode`, `AppListPage` exposes the filter tab, and `AppListVisibleSections` depends on package-private `AppListItem`. Move them after the app-list/app-config status model boundary is explicit. |
| `AppStatusFormatter` | Pure formatter, but its input model depends on root-owned viewport/font apply-mode types. Move only after the app-list/app-config status model has a package boundary, not by exposing those shared model types just for the formatter. |
| `CompatFontPropertySyncer` | Looks unused by production callers, but its command builder depends on package-private `DpisConfigStore`, `FontApplyMode`, and HyperOS font bridge helpers. Move with the font property bridge/config boundary, not as a standalone test helper. |
| `HyperOsNativeProxyRefreshCoordinator` | Semantically adjacent to the native proxy mounter, but it depends on package-private `DpisConfigStore`, `DpisLog`, and startup/package-update side-effect rules. Keep it root-owned until that refresh policy has an explicit public seam. |
| `SettingsWorkspaceBinder`, `SystemServerSettingsPageController`, `SystemHooksToggleController` | Settings UI slice is coherent, but the current page controller still owns `DpisConfigStore`, service/scope gateways, haptics, cache cleanup, and package-private settings helpers. Move as a settings page wave, not as isolated binders. |
| `SafeCacheCleaner` | Cache cleanup looked standalone, but it clears font-debug stats through package-private `FontDebugStatsStore` and `FontDebugStatsFileBridge`. Move only with the font-debug cache/storage boundary or keep it root-owned by the settings page. |
| `FeedbackDiagnosticCoordinator` | Feedback diagnostic orchestration is still the root-owned bridge from `MainActivity`/`QuickConfigActivity` into diagnostics. Its request factory depends on package-private `AppListItem`, `AppConfigDialogBinder.AppConfigDialogState`, `DpisConfigStore`, viewport target specs, and font mode normalization; moving it now would force broad public model exposure. |
| `FeedbackDiagnosticExportBuilder` | Diagnostic packaging belongs conceptually to `diagnostics`, but it still takes `FeedbackDiagnosticCoordinator.Result`, reads package-private `DpisAppLogStore`, and reports package-private runtime self-test state. Move after a narrow export input/result contract and app-log reader seam exist. |
| `FeedbackDiagnosticResultSheet` | Result presentation is an Activity-owned UI sheet tied to the root export package model and root save/share actions. Keep it in the manifest/UI root cluster until diagnostic export presentation has a dedicated public contract. |
| `FeedbackDiagnosticRuntimeEvents`, `FeedbackDiagnosticRuntimeHotPathEvents`, `FeedbackDiagnosticRuntimeSelfTest`, `FeedbackDiagnosticRuntimeTransport` | These are runtime capture and hook hot-path pieces used directly by app-process/system-server route installers and runtime tests. Moving them would require publishing hook-facing internals across package boundaries; keep them root-owned until runtime route ownership is reviewed. |
| `ModernApiCapabilities`, `ModernApi101Capabilities`, `ModernApi102Capabilities`, `ModernApiCapabilitiesResolver` | Capability gating is a good future hook API package, but the 102 path logs through package-private `DpisLog` and is referenced by shared hook installers. Move only with a logging seam and hook API routing review. |
| `PerAppDisplayConfigSource`, `PerAppDisplayEnvironment`, `PerAppDisplayOverrideCalculator`, `DisplayOverridePipeline`, `VirtualDisplayPlan` | These are viewport/system-server route model helpers, not ordinary UI helpers. Moving them touches runtime route ownership and source-smoke evidence; handle under the runtime route playbook. |
| `DebugFontOverride` | Small value object, but it is part of app-process font hook decision flow through `AppProcessHookInstaller` and `FontHookDomainDecision`. Move with the font hook planner/runtime boundary, not alone. |
| `ConfigDraftSaveSemantics`, `AppConfigInputValidation` | App-config parsing helpers depend on shared viewport target/apply-mode and font-mode semantics. Move with an app-config model boundary so draft save rules do not become a scattered public utility API. |
| `ResourcesReadHookPolicy`, `SystemServerHookSpec`, `SystemServerMutationField`, `SystemServerProcess`, `WechatDpiRoutes`, `WindowFrameOverride` | Runtime route-owned policy/value types. Move only with the relevant route document review and route-focused tests. |
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
