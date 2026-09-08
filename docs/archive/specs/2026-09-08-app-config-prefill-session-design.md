# App Configuration Prefill Session Design

> Archived 2026-09-08. The session/chip contract landed in PR #123
> (`ff93ee1a`). Keep this file for history; current behavior is in
> `app/src/main/java/com/dpis/module/appconfig/editor/`.

## Purpose

Make the per-app editor distinguish a persisted application configuration from a
global-prefill preview and from edits made during the current sheet lifetime.
The distinction is session state, not a presentation-only flag.

## Session Model

Each opened editor owns three immutable comparison baselines plus its mutable
draft:

- `persistedBaseline`: the app's saved configuration, or the empty default when
  the app has no saved configuration.
- `prefillSnapshot`: the global prefill resolved when this sheet opens. It is
  absent when the app already has a configuration or no global prefill exists.
- `draft`: the current editable configuration.
- `resetToDefault`: records that Reset intentionally replaced a prefill preview
  with the empty default for this session.

The sheet never reloads `prefillSnapshot`. Imports, another process, or a future
multi-window flow can change the stored global prefill, but that change affects
only a later editor session.

## Chip State

The header derives exactly one chip from the session; it does not inspect an
`AppListItem` preview flag directly.

| Condition | Chip |
| --- | --- |
| No saved app config, a prefill snapshot exists, draft equals that snapshot, and reset was not requested | `Prefill` |
| Draft differs from its persisted baseline and the previous row does not apply | `Unsaved` |
| Otherwise | none |

`Prefill` uses the info color role. Both `Prefill` and `Unsaved` have an outline.
The English resource value is `Prefill`.

## Commands

Opening an unconfigured app resolves the global prefill once and initializes the
draft from it when it has values. Closing without saving discards the whole
session, including its draft and `resetToDefault` state. The next open resolves a
new snapshot.

Reset always replaces the draft with the empty default, sets `resetToDefault`,
and never writes a package configuration. The resulting chip is absent. A later
edit of that default draft produces `Unsaved`.

Saving persists the draft as a real package configuration. The session then
replaces `persistedBaseline` with the saved draft, clears `prefillSnapshot`, and
clears `resetToDefault`; the chip disappears. All later openings use the saved
package configuration rather than a global prefill.

## Compatibility and Ownership

The Compose editor is the primary owner of this session model. Legacy View
surfaces consume the same derived state instead of retaining their own
empty-signature and preview-flag interpretation. `AppListItem` may still carry
display data for list rendering, but it is not an editor-session state owner.

## Tests

Behavior tests cover:

- initial prefill, mutation, and reset chip transitions;
- unconfigured close-and-reopen resolving the prefill again;
- reset followed by save creating a real app configuration;
- save from a prefill session removing prefill semantics;
- snapshot stability while the sheet remains open;
- one-chip precedence and outlined visual-state mapping.
