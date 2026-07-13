# Compose Workspace Migration Baseline

Themes 1-3 established the baseline for migrating the remaining DPIS workspaces
to Compose Material3. This document defines the boundary that Themes 4 and 5
must preserve; it is not a new product design proposal.

## Completed Scope

- Theme 1: DPIS Compose design system and adaptive shell.
- Theme 2: standalone support pages and shared support-sheet presentation.
- Theme 3: Home, Tools, and Settings workspaces.

The Compose shell owns phone bottom navigation and expanded navigation chrome.
`MainUiState` and `MainUiAction` remain the sole source of truth for workspace
selection. The legacy View root remains only for App and Template destinations
until their dedicated themes migrate them.

## Ownership Rules

Compose owns layout, transient expansion/sheet state, ripple, and rendering.
Existing domain owners retain configuration writes, system settings writes,
permissions, update installation, cache work, logs, backups, dialogs, activity
results, and runtime-facing state.

Use a focused immutable UI state plus action boundary when a Compose workspace
needs observable controller state. Do not read stores directly from a composable
or make a composable the owner of a persisted draft.

Compact watch and round devices continue to use the existing `WatchUiMode` View
route. Do not broaden a phone/tablet Compose migration into a watch migration
without equivalent layout and device verification.

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

## Future Theme Checklist

Before migrating a Theme 4 or 5 workflow:

1. Identify the existing domain owner and every activity/dialog/result contract.
2. Decide which state is transient Compose state and which must remain in a
   controller/presenter because it survives a surface, save, restore, or rotation.
3. Preserve compact-watch routing and establish exactly one inset owner.
4. Carry over action availability, haptics, slider/value semantics, and dialog
   confirmation behavior before visual polish.
5. Add behavior tests for extracted state rules and source smoke tests only for
   Compose wiring; run the full two-flavor unit suite before committing.
