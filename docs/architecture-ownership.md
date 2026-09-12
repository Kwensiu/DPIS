# DPIS presentation ownership

Binding short rules live in `CONTEXT.md` under **Presentation And Code
Ownership**. This document expands those rules and lists remaining migration
slices.

## Ownership

### Activity is a shell

An Activity may own lifecycle, intent, Compose/`ComposeView` install,
permission and result callbacks, and wiring into a focused host. It must not
own feature workflows, dialogs, exporters, diagnostics, or other state
machines. `MainActivity` is limited to app-shell startup and event dispatch.

### One screen, one state machine

Compose renders and dispatches. It does not write stores, choose runtime
routes, persist package config, or run long I/O. Durable work lives in a
coordinator, presenter, store, or ViewModel that the screen already uses.

Workspace destination selection stays on `MainUiState` / `MainUiAction`. Do not
replace that with a second navigation stack unless a later task explicitly
migrates the shell.

### Policy versus host

JVM-testable rules (filter, sort, restore normalization, prompt decisions,
byte formatting) stay outside `presentation/`, `ui/`, `runtime/`, and flavor
trees, and ship with a behavior test.

Android dialog, Activity, binder, and installer glue goes under the feature
`presentation/` directory so coverage exclusions follow the directory
contract. Do not add one-off Sonar file exclusions.

Stores, codecs, and coordinators already in this repository are the domain
layer. Do not introduce a repository or use-case type per action unless the
touched feature already has that shape.

### One UI toolkit

New screens, dialogs, sheets, list rows, and controls are Compose. Do not add
XML View binders. Convert a Java host in the same change when this task
already materially edits it, unless reflection, JNI, a flavor Xposed entry, or
an externally observed JVM signature makes that unsafe.

### Where files live

`ui/` is shell, tokens, scaffold, and shared chrome only. Feature screens
belong in the feature package (`about/presentation`, `settings/presentation`,
`applist`, `templates/presentation`, and so on). Do not add a new About,
Settings, or editor screen under `ui/`.

Physical directory and Kotlin package must match. Do not leave a feature
composable in `about/presentation` (or another feature tree) with a
`ui.compose` package.

### Java keep-list

Java remains only where the contract requires it: flavor Xposed entrypoints,
reflection or JNI boundaries, the pinned Quick Settings tile FQCN
`com.dpis.module.QuickConfigTileService`, or another externally observed JVM
signature. Record that reason in the change.

When extracting a host, keep the Java-callable surface stable (`@JvmStatic`,
explicit `Host` interfaces, nullable types Java callers still pass).

## How a change should land

Apply in this order when a file is already in the change. Do not pause a
feature PR to rewrite the whole tree.

1. **Stop growing `MainActivity`.** New workflows go to a focused class under
   the feature package. If this change already edits a nested workflow in
   `MainActivity`, extract it in the same change.
2. **Policy then host.** If a host contains a pure rule, extract the policy,
   add a behavior test, then move the Android host to `presentation/` and
   convert Java when the class is in the Kotlin/Compose boundary.
3. **Fix directory and package together.** Feature composables belong in
   `feature/presentation` with that package. Shared scaffolds stay in `ui/`.
4. **Do not add XML.** Convert or replace View binders when the change already
   owns them.
5. **Coverage.** Measurable policy stays outside excluded globs. Untestable
   glue goes in `presentation/`, `ui/`, `runtime/`, `root/`, or flavor trees.
   Never a one-off file exclusion.

## Next slices

Do these as separate, complete cuts. Do not mix a package rename with a
Compose rewrite, and do not grow `MainActivity` while extracting a host.

1. **Align feature Compose packages with their directories.** Files under
   `about/presentation`, `settings/presentation`, `applist/presentation`,
   `appconfig/presentation`, `fonts/presentation`, `diagnostics/presentation`,
   `tools/presentation`, and `quickconfig/presentation` must not keep
   `package com.dpis.module.ui.compose`. Shared scaffolds under `ui/` may keep
   the historical `ui.compose` package until that tree is renamed as its own
   cut. Prefer one feature tree per PR so the rename is complete.
2. **Move Settings backup picker/thread** out of
   `SystemServerSettingsPageController` into a backup presentation host.
   Policy and I/O already live in `ConfigBackupRestorePolicy` and
   `backup/presentation/ConfigBackupCoordinator`.
3. **Move About update check** out of `AboutActivity` methods into the
   existing prompt/download coordinators (partially `AboutUpdatePromptState`).
4. **Peel `MainActivity` Host implementations** (update, disclaimer, download,
   workspace `Content`) into those hosts. Keep the Activity as lifecycle and
   dispatch wiring only.
5. **Replace remaining XML View binders with Compose** only when that surface
   is already the change. Do not convert the whole editor in one PR.
6. **Drop `activity_status` as a coordinator assembly source** once no Java
   host still reads that inflated tree (`docs/compose-workspace-migration.md`).
