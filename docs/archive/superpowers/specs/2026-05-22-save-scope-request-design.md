# Save-Time Scope Request Design

Date: 2026-05-22

## Goal

When a user saves per-app DPIS configuration for an app that is not yet in the
LSPosed module scope, DPIS should automatically request adding that app to the
scope.

This removes the extra manual step of opening the app detail sheet actions and
tapping "Add to scope" after saving a useful configuration.

## Non-Goals

- Do not silently modify LSPosed scope without its normal request/approval flow.
- Do not change the existing manual "Add to scope" and "Remove" sheet actions.
- Do not request scope when DPIS cannot reliably read current scope state.
- Do not change parameter save semantics, runtime property publishing, or hook
  behavior.
- Do not add a new persistent "auto scope" setting.

## User Flow

1. The user opens an app detail sheet.
2. The user edits width, font scale, font mode, or font file settings.
3. The user taps "Save".
4. If the save succeeds and DPIS knows the app is not in scope, DPIS immediately
   triggers the same scope request path used by the "Add to scope" button.
5. LSPosed still owns the approval flow. DPIS shows the existing request notice
   and later shows the existing success or failure toast.

If the app is already in scope, saving only saves configuration. If scope state
is unknown, saving only saves configuration.

## Behavior Rules

- Trigger only after a successful save result.
- Trigger only when `scopeKnown == true` and `scopeSelected == false`.
- Reuse `SystemScopeCoordinator.toggleScope(..., currentlyInScope = false, ...)`
  or a small semantic wrapper around the same backend call.
- Track a non-persistent `scopeRequestPending` state in the open sheet after a
  save-time request is sent. While it is pending, later saves from the same sheet
  must not send another request for the same app.
- On approval, update the sheet state to "in scope" when the sheet is still
  attached, and refresh the app list. If the sheet has already been dismissed,
  skip the sheet-state update and still let the coordinator refresh the app list.
- On failure, keep the saved app configuration and show the existing scope failure
  toast. Clear the transient pending state if the sheet is still attached.
- Do not repeat the request for apps already marked in scope.
- Do not request scope after validation failure or save failure.

## Architecture

The feature belongs in the app detail dialog layer, not the parameter save
handler.

`AppConfigSaveHandler` remains responsible for:

- validating and saving per-app parameters;
- publishing or clearing runtime property targets;
- returning save success and user-facing save hints.

`AppConfigDialogBinder` already has the active dialog state needed for this
feature: `scopeKnown`, `scopeSelected`, transient `scopeRequestPending`, and the
save result. After a successful save, it should ask its host to request scope if
needed. The host delegates to the existing `SystemScopeCoordinator` path used by
the manual scope button.

This keeps the LSPosed scope side effect separate from parameter persistence
while still making the behavior part of the save button user experience.

## Error Handling

Save errors keep the current behavior and never request scope.

Scope request errors do not roll back saved parameters. The user may still tap
"Add to scope" manually later or retry saving. Existing scope failure strings
should be reused unless implementation needs a more specific message.

When `DpisApplication.getXposedService()` is unavailable, the existing scope
coordinator behavior applies: no request is sent and the saved parameters remain.

The async scope approval callback may arrive after the app detail sheet has been
dismissed. That callback must not require the old dialog views to be alive. It
may update the sheet only when still attached, while the app-list refresh remains
owned by the coordinator path.

Save success should keep using the existing inline save-button feedback. This
feature should not add another save-success toast; the only extra user-facing
message is the existing scope request notice and any existing scope result toast.

## Testing Strategy

Add source or behavior coverage for the dialog save path:

- save success plus known missing scope calls the scope request host path;
- repeated save while `scopeRequestPending` is true does not send a duplicate
  request;
- save success plus already-in-scope does not request scope;
- save success plus unknown scope does not request scope;
- save failure does not request scope;
- dismissed-sheet async callback does not require updating dead dialog views;
- the request path reuses existing `SystemScopeCoordinator` behavior.

Run targeted dialog/scope tests during implementation, then run
`:app:testAllDebugUnitTests` before submitting.

## Acceptance Criteria

- Saving config for a known non-scoped app triggers an LSPosed scope request.
- Re-saving from the same sheet while a request is pending does not trigger a
  duplicate request.
- Saving config for a scoped app does not trigger another request.
- Saving config when scope state is unknown does not trigger a request.
- Failed or invalid saves do not trigger a request.
- Scope approval/failure after sheet dismissal does not crash and does not depend
  on updating closed UI.
- Manual scope add/remove behavior remains unchanged.
