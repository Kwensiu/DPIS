# App Config Prefill Session Implementation Plan

> Archived 2026-09-08. Implemented and squash-merged to `main` as
> `ff93ee1a` (PR #123). Keep this file for history; do not treat it as the
> current editor contract.

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the per-app editor own a session that distinguishes persisted configuration, a frozen global-prefill snapshot, and the current draft, then derive exactly one Prefill or Unsaved chip from that session.

**Architecture:** Introduce `AppConfigEditorSession` as the Compose editor's session owner. Capture `persistedBaseline`, optional `prefillSnapshot`, mutable `draft`, and `resetToDefault` when the sheet opens. Never reload the snapshot while the sheet stays open. Presentation reads `session.chip`; `AppListItem.previewFromGlobalPrefill` is display data only. Saving always persists the draft as a real package configuration.

**Tech Stack:** Kotlin, Jetpack Compose Material 3, JUnit4, existing `EditorDraft` / `AppConfigPrefillPreview` / `AppConfigSaveHandler`.

**Workspace:** Implemented on `fix/template-config-runtime` and merged to `main`.

---

## File map

- Create: `app/src/main/java/com/dpis/module/appconfig/editor/AppConfigEditorSession.kt`
- Create: `app/src/test/java/com/dpis/module/appconfig/AppConfigEditorSessionTest.kt`
- Modify: `app/src/main/java/com/dpis/module/appconfig/editor/EditorPresentation.kt`
- Modify: `app/src/main/java/com/dpis/module/appconfig/editor/EditorPresentationFactory.kt`
- Modify: `app/src/main/java/com/dpis/module/appconfig/editor/EditorActions.kt`
- Modify: `app/src/main/java/com/dpis/module/appconfig/editor/ComposeAppEditorController.kt`
- Modify: `app/src/main/java/com/dpis/module/appconfig/EditorSessionResolver.java` → Kotlin
- Modify: `app/src/main/java/com/dpis/module/MainViewModel.kt`
- Modify: `app/src/main/java/com/dpis/module/appconfig/AppConfigSaveHandler.kt`
- Modify: `app/src/main/java/com/dpis/module/appconfig/presentation/AppConfigEditorContent.kt`
- Modify: `app/src/main/java/com/dpis/module/MainWorkspacePresentationCoordinator.kt`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-zh-rCN/strings.xml`
- Modify: legacy View consumers of `previewFromGlobalPrefill` chip visibility
- Modify: tests and source smoke tests that currently treat prefill as Unsaved

### Task 1: Session model and chip derivation

**Files:**
- Create: `app/src/main/java/com/dpis/module/appconfig/editor/AppConfigEditorSession.kt`
- Test: `app/src/test/java/com/dpis/module/appconfig/AppConfigEditorSessionTest.kt`

- [ ] Write failing tests for open/prefill/mutate/reset/save/reopen/snapshot stability/one-chip precedence
- [ ] Implement `AppConfigEditorSession` and `AppConfigEditorChip`
- [ ] Run the new tests until they pass
- [ ] Commit `feat: add app config editor prefill session model`

### Task 2: Compose editor owns the session

- [ ] Store the session on `MainViewModel` / `ComposeAppEditorController`
- [ ] Resolve prefill once on open; keep the snapshot across refreshes
- [ ] Reset sets `resetToDefault` and does not write package config
- [ ] Close discards the session; the next open resolves a new snapshot

### Task 3: Save persists a real package configuration

- [ ] Remove `isUnchangedGlobalPrefillPreview` save-skip/clear
- [ ] After save, replace `persistedBaseline`, clear `prefillSnapshot` and `resetToDefault`
- [ ] Tests: save from prefill; reset then save

### Task 4: Chip presentation

- [ ] Add `sheet_prefill_badge` = `Prefill` / `预填`
- [ ] Header and sheet chrome derive one chip from the session
- [ ] Prefill uses the info/tertiary color role with an outline
- [ ] Unsaved keeps primary container with an outline
- [ ] Legacy View surfaces consume the same derived chip

### Task 5: Validation

- [ ] Update source smoke tests that still assert `dirty || previewFromGlobalPrefill`
- [ ] Run `:app:testAllDebugUnitTests` and assemble Modern Debug
