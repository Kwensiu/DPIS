# Compose Workspace Migration Baseline

Themes 1-5 establish the baseline for the DPIS phone and tablet main-workspace
migration to Compose Material3. This document defines the boundary for later
standalone and compact-device work; it is not a new product design proposal.

## Completed Scope

- Theme 1: DPIS Compose design system and adaptive shell.
- Theme 2: standalone support pages and shared support-sheet presentation.
- Theme 3: Home, Tools, and Settings workspaces.
- Theme 4: App workspace list/detail presentation.
- Theme 5: App configuration plus Template and global-prefill editors.
- Standalone About and open-source-license pages, with update and license
  workflows still owned by their existing Java coordinators.
- Standalone font library and font-detail pages, including Compose-native font
  previews and app-reference presentation while import, repair, rename, and
  deletion workflows remain Java-owned.
- Standalone log page, including virtualized entries, expansion/copy actions,
  source switching, sort/refresh controls, and export presentation while log
  reads, Root probing, ZIP creation, and Activity results remain Java-owned.
- Quick Config's translucent bottom-anchored editor now reuses the same Compose
  app editor and Hook-chain destinations as the main workspace while foreground
  package resolution, persistence, process actions, and diagnostics remain
  Activity/controller-owned.
- Phone/tablet settings, support, local-tool, font-management, diagnostics,
  update, release-notes, and ordinary confirmation/message dialogs now use the
  shared DPIS Compose dialog language. Their Java coordinators continue to own
  persistence, downloads, export, and lifecycle-sensitive callbacks.

The Compose shell owns phone bottom navigation and expanded navigation chrome.
`MainUiState` and `MainUiAction` remain the sole source of truth for workspace
selection. App and Template content now render through their Compose
presentations. `MainComposeShellHost` no longer has an `AndroidView` fallback;
the inflated Activity layout is retained only as the Java coordinator assembly
source while that wiring is incrementally extracted.

## Ownership Rules

Compose owns layout, transient expansion/sheet state, ripple, and rendering.
Existing domain owners retain configuration writes, system settings writes,
permissions, update installation, cache work, logs, backups, dialogs, activity
results, and runtime-facing state.

Use a focused immutable UI state plus action boundary when a Compose workspace
needs observable controller state. Do not read stores directly from a composable
or make a composable the owner of a persisted draft.

Compact watch and round devices use a dedicated Wear OS Compose Material3
presentation. `MainWorkspacePresentationCoordinator.renderWear` reuses the same
Java-owned state and action boundaries as phone/tablet while the Wear layer owns
round-screen layout. Wear lists use `ScreenScaffold`, `TransformingLazyColumn`,
transformed item height, and Wear buttons/switches. The Wear color scheme is
derived from the active DPIS light or dark theme instead of accepting the Wear
library's black default background. Validate both theme modes on a round Wear OS
AVD in addition to phone/tablet verification.

## Insets And Interaction

Each migrated workspace has one Compose inset owner. It consumes shell content
padding and must not also apply the legacy `WindowInsetsBinder` route. Legacy
workspaces keep their existing View insets until they migrate.

Use Material3 components for cards, rows, switches, sliders, navigation, and
buttons. A rounded clickable surface must use the component's `onClick` API or
clip its indication so ripple remains inside the visible shape.

Discrete successful actions use `rememberDpisConfirmAction`. This covers
navigation changes, cards, entries, switches, buttons, and compact icon actions.
Do not emit haptics for disabled actions or passive information. Sliders are
different: preserve their existing domain step feedback rather than treating
every motion event as a click.

## Preserved Tool Semantics

The system-font-scale tool remains a global `Settings.System.font_scale` tool,
not an Xposed font route. Its Compose surface preserves permission, unavailable,
pending/apply/restore, one-percent adjustment, log-gate, and target-relative
preview semantics. The preview must not multiply the device's current font scale
into the pending target.

The interface-scale settings slider uses the 60-120% range with one-percent
choices. It may hide dense visual tick marks, but interaction values must still
snap to whole percentages and emit feedback only on a choice transition.

## Follow-up Migration Checklist

Before migrating a remaining standalone Activity or compact-device workflow:

1. Identify the existing domain owner and every activity/dialog/result contract.
2. Decide which state is transient Compose state and which must remain in a
   controller/presenter because it survives a surface, save, restore, or rotation.
3. Preserve compact-watch routing, or explicitly replace it with an equivalent
   Wear OS route, and establish exactly one inset owner.
4. Carry over action availability, haptics, slider/value semantics, and dialog
   confirmation behavior before visual polish.
5. Add behavior tests for extracted state rules and source smoke tests only for
   Compose wiring; run the full two-flavor unit suite before committing.

## Remaining Scope

The active phone/tablet and compact Wear presentation routes are Compose-owned.
`activity_status` and legacy workspace binders remain as the current Activity
assembly and wide-layout compatibility boundary; compact Wear does not consume
them.

Wear and compact-round UI is a separate product surface. It may use Wear OS
Compose Material3 components and round-screen layouts instead of imitating the
phone shell, but it must retain `WatchUiMode` classification until the new route
has equivalent navigation, inset, accessibility, and device-test coverage.

The compact workspace selector and workspace content use Wear Compose Material3
`AppScaffold`, `ScreenScaffold`, `TransformingLazyColumn`, Wear buttons, and Wear
switches. App and template editors are full-screen Wear routes; their typeface
and Hook-chain child pages remain in the same draft session. The startup
disclaimer is Compose-owned on every form factor.
