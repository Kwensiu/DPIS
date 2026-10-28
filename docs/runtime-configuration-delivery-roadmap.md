# Runtime Configuration Delivery Decisions

Status: product semantics confirmed through 2026-10-20; font property identity
implementation updated on 2026-10-22. Baseline sections retain historical gaps.

This document records the confirmed user-facing rules and the implementation
order. It does not assume that each storage or transport field needs a new key;
first establish which boundaries are already isolated by Android/Xposed.

## Confirmed Semantics

1. **Xposed readiness gates package-config editing.** The UI must not save app
   configuration unless the active flavor's Xposed config path is confirmed
   available. This is independent of Root authorization. UI treatment of the
   disconnected/read-only state is deferred to later UX design.
2. **Local package configuration is the sole user-owned source of truth.**
   Modern remote preferences are a one-way runtime projection from the local
   package-config store. Remote data and property mirrors must never overwrite
   existing local package config. For the old connected-UI migration, remote
   runtime keys may seed local keys that are absent; existing local values win,
   and a per-user completion marker prevents later remote-to-local reimports.
   Reconnection publishes the canonical local snapshot outward.
3. **Keep a system_server property fallback, but design it first.** The
   system_server route needs the property path when its remote config source is
   unavailable. Before changing behavior, define how the mirror proves payload
   freshness, configuration revision, package identity, and target Android
   user. No precedence change or property cleanup is authorized until that
   design and its validation are complete.
4. **Legacy may use a different delivery mechanism from Modern.** Investigate
   and design Legacy independently. It must follow the same local-source,
   Xposed-readiness, no-UI-Root-save semantics; it must not import LibXposed
   APIs solely for symmetry.
5. **Defer centralized cross-user config management.** Each DPIS installation's
   UI manages only the applications and private archive of its current Android
   user. The same package in another Android user is managed by that user's DPIS
   installation. This matches the per-user LSPosed app-scope boundary: on PJZ110
   scope rows are keyed by `user_id`, while `XposedService` scope APIs accept no
   explicit target-user argument. System_server may still select a target user's
   config by UID without target-app injection; Compat and other app-process-only
   routes require app scope in the target user's LSPosed context. Cross-user
   archive access and primary-UI editing are deferred to a separate experiment.

## Terminology

- **Local package config:** DPIS UI's private, user-editable canonical archive
  for saved package settings. This decision does not include unrelated local
  stores such as the font library.
- **Remote preferences:** framework-backed LibXposed preferences. "Remote"
  means remote from the DPIS UI process, not cloud storage or a second archive.
  Modern UI publishes local config through `XposedService`; Modern hook
  processes read it through `XposedInterface`.
- **Property mirror:** a system property used as runtime transport/fallback.
  It is not a user-editable archive and cannot write back to local config.
- **Xposed-ready:** the UI process has positive evidence that its active flavor's
  module configuration endpoint is available for accepting package-config
  writes. This is a UI write gate, not proof that a target process consumed the
  expected config. In Legacy, self-hook activation proves only that the DPIS UI
  process was entered by Legacy Xposed; target `XSharedPreferences` readability
  remains a separate, unverified delivery question. Root availability is not
  Xposed readiness.
- **User-scoped package identity:** saved package values and snapshot lookups
  are keyed by `(packageName, userId)`. Each DPIS installation's app list shows
  only its current Android user's installation and configuration. Cross-user
  centralized editing is deferred; it is not represented by a read-only row.
  Runtime properties now carry userId for viewport, WeChat DPI, font scale/mode,
  typeface, hook-domain and HyperOS native font/binary values. Old unscoped font
  keys are ignored rather than attributed to user 0; each DPIS user republishes
  its canonical private archive. The October 22 Modern route record contains
  Chrome launch evidence (user 0 fontScale=2.0, user 999 fontScale=0.6). Existing
  native proxies need refresh; Legacy/native app effectiveness remains separate.

## Baseline Gaps (Before P1)

- The bound Modern UI store currently selects remote preferences as primary.
  `ConfigPreferenceAccess.commitBoth` writes package config only to that primary
  store; it does not update the local package-config archive. The local-to-remote
  `publishRuntimeConfig` method has no callers. Binding imports the local XML
  only when remote has no user-visible package config. This permits remote to be
  stale or newer than local and violates decision 2.
- **Historical runtime property identity gap (superseded by the October 22
  font transport repair below):** viewport and WeChat DPI properties carry
  userId, but font scale/typeface, font hook-domain, and HyperOS native font
  properties are still package-hash-only. Xposed-backed app-process snapshots
  remain owner-user checked, but these shared property routes can be overwritten
  by another DPIS user configuring the same package. Do not claim complete
  per-user font/native fallback isolation until these property contracts and
  their native consumers migrate together.
- DPIS has a Legacy self-hook activation probe and a Modern `XposedService`
  connection signal. Readiness must be represented per flavor and must gate
  app-config mutation/save entry points, not only the home-page status.
- Runtime property writers currently invoke `su` from the UI/application
  process. That Root dependency must not block the canonical local save.

## Implementation Order

### P0: Confirmed semantics and behavior locks

- Per-flavor UI write readiness and denied-write behavior are implemented and
  behavior-tested. Modern requires an available libxposed remote preference
  store; Legacy requires positive Legacy self-hook activation in the DPIS UI
  process only. That Legacy signal does not prove target-process
  `XSharedPreferences` reads. Editor saves, DPIS-enabled toggles, batch package
  mutations, quick-template application, and backup import reject writes while
  the UI-side flavor signal is not ready. Batch tasks and backup imports also
  re-check readiness when queued work executes, before any package-config write.
  UI presentation remains deferred.
- Keep UI presentation out of this phase.

### P1: Local-authoritative Modern delivery (device-verified)

- The UI store is local before and after service bind.
- On first remote connection, runtime keys absent locally are seeded once from
  the legacy remote snapshot; local conflicts win. The per-user marker is local
  only and excluded from runtime projection and backup.
- Bind and post-save reload replace the remote runtime snapshot from local.
- Remote store absence or write failure does not roll back local configuration.
- Behavior tests cover conflict resolution, remote-only seed, one-time migration,
  local-only state exclusion, and failed/unavailable publication.
- Device verification confirmed local-to-remote publication and preservation of
  user 0's local Chrome setting. A controlled cold-start probe with each user's
  transient and persistent property mirror temporarily disabled showed user 0
  Chrome reading `relative_scale:80000` and user 999 Chrome reading
  `relative_scale:160000` from the Modern remote preference path; both app
  processes applied the corresponding Resources metrics. The property values
  were restored, and the per-user local archives were unchanged. The
  system_server route remains separately unverified.

### P2: User-scoped package identity

The identity flow, field classification, conflict rules, and migration retry
contract are recorded in [
`docs/package-identity-migration-matrix.md`](package-identity-migration-matrix.md).

- Audit findings: the UI's `SharedPreferences` archives and quick-template
  store are Android-user-private, while their package keys and current
  `ConfigSnapshot`/`PackageConfigSnapshot` map are package-only. Modern remote
  preferences were device-verified to return independent Chrome values when
  read from user 0 and user 999 app processes. Runtime viewport and WeChat DPI
  property names already include user ID; system-server UID selection also
  resolves the target user. Backups are portable package-key snapshots. Each
  DPIS installation's app list remains scoped to its current Android user;
  centralized cross-user editing is deferred as a separate experiment.
- Introduce the smallest identity abstraction needed to carry package name
  and Android user through shared package snapshots and runtime lookups. Do not
  mechanically prefix every key before deciding migration semantics for
  existing package-only data.
- **Completed private archive stage:** the local store factory derives owner
  userId from the DPIS application's UID. Existing package fields are copied
  independently into owner-scoped staging keys in that archive; existing staged
  values win per field. The owner marker and staged values share one archive
  transaction. Re-running for the same owner discovers newly written legacy
  fields; a different owner or a failed commit cannot mark migration complete.
  Invalid/unknown source fields remain in place, and valid conflicting legacy
  fields are preserved as archive-local residuals. Staged/residual keys are
  excluded from runtime delivery and backup. The owner-aware store boundary is
  now active: reads prefer scoped values with legacy compatibility fallback,
  and successful package writes update both representations atomically. Runtime
  snapshot identity cutover remains partial and is tracked below.
- Migration semantics are now evidence-based: package-only values in each
  Android user's DPIS-private `dpi_config` archive belong to that archive's
  owning user. Assign them to `(packageName, ownerUserId)` once; never infer
  ownership by copying the package config to every user where the target app is
  installed. Existing user-specific values win conflicts. Runtime properties
  are mirrors, not a migration source.
- Backups remain portable: importing a package config applies it to the active
  DPIS user's identity, not the exporting user's ID. Quick templates and
  package configs stored in the DPIS-private template/config stores likewise
  belong to the owning DPIS Android user. Runtime snapshots and system_server
  lookups must carry the target app's `(packageName, userId)` explicitly.
- User-scoped write validation is covered by owner-user migration, same-package
  isolation, conflict preservation, portable backup rebinding, current-user
  list/editor selection, and no cross-user runtime fallback tests. Keep
  `target_packages` migration-only and preserve contradictory or incomplete
  package-local residue.
- Store/editor/list and portable-backup boundary work is now complete: normal
  package writes mirror the active archive owner's scoped and package-only keys
  atomically; identity-aware store reads consume staging, and backup payloads
  remain portable while restore rebinds to the local archive owner. Backup
  rollback restores managed package data, scoped identity data, and the owner
  marker, while rollback commit failure is surfaced as a distinct failure. Templates
  and batch mutations continue through this owner-bound store writer. Runtime
  snapshot identity cutover is partially implemented: snapshots now retain the
  owner user that built them, package lookup requires the requested user to
  match that owner, Modern app-process stores derive their owner from
  `Process.myUid()`, and user-scoped property fallback is constructed with the
  requested target user. App-process and shared-source tests cover same-package
  user isolation and missing-target fail-closed behavior. The shared config
  source now requires explicit user identity; owner-matched snapshots provide
  viewport config for any user, and target-user properties retain precedence.
  Invalid UID conversion fails closed. HyperOS Rust process rewriting likewise
  requires UID evidence associated with its package arguments. Existing
  system_server UID-to-user selection paths are retained and verified
  separately; full system_server snapshot construction and device effectiveness
  remain open.

### P3: Modern runtime source and system_server fallback design

- Use LibXposed remote preferences as the direct Modern runtime source where
  available; verify both app-process and system_server config construction.
- Design the required system_server property fallback with revision, package,
  and user identity validation, plus an explicit publish/failure state. The
  existing properties, including the October 22 user-scoped font migration,
  establish target-user selection, not payload revision/freshness. The migration
  covers font/typeface/hook-domain writers, readers, clear/recovery paths and
  native consumers together. It ignores old unscoped keys and does not change
  source ordering or claim revision/atomic publication guarantees.
- Only after tests and device evidence pass, change the current source ordering.
- Preserve properties still needed by confirmed compatibility routes.
- **Historical feasibility experiment (2026-10-20):** a temporary
  `UserPackageRuntimeGate` was wired into Modern system_server to exercise a
  root-published `(packageName, targetUserId)` deny property. That implementation
  and its experiment-only tests have since been removed; it was not a product
  management control and had no user-facing writer. The experiment showed that
  a target-user deny prevented both snapshot and property-fallback lookup while
  leaving each archive and viewport property untouched. User 999 gate was
  restored to explicit `1` and user 0 remained `1`; Android persistent
  properties were not deleted. These values are no longer read by DPIS. The
  remaining system_server identity route still resolves the target UID/user,
  rejects owner-mismatched snapshots, and uses target-user property fallback.

### P4: Legacy delivery design

- Independently determine whether current `XSharedPreferences` can reliably
  serve user-scoped local config without DPIS UI Root. The P0 Legacy readiness
  signal proves only DPIS UI-process self-hook activation; it is not evidence
  that a target process read the expected package config.
- If not, design a Legacy-owned transport that preserves the classic Xposed
  boundary. Do not reuse Modern service APIs or change Legacy semantics by
  analogy.

### P5: Root as a separate capability layer

- Root remains a separate capability layer for privileged diagnostics,
  authoritative package discovery, and runtime property delivery. A historical
  Modern gate probe validated root-published runtime selection, but its
  experiment-only code was removed. Cross-user archive ownership or a primary-UI
  editor is not in the current implementation plan; either requires a separate
  product decision and a verified LSPosed scope/persistence design.

## Verification Gates

A runtime delivery change is not verified by config save or hook-ready logs
alone. Verify:

1. local save succeeds and remains canonical;
2. selected Xposed config path and source revision are recorded;
3. target Android user is resolved from the target app identity;
4. the intended process reads the expected user-scoped config;
5. hook callback and mutation are observed;
6. cold-start or visible target behavior matches the selected setting;
7. failures of remote/property/root delivery do not erase local config.

For no-UI-Root verification, keep Xposed injection active, deny only the DPIS
UI's `su` authorization, save through an Xposed-ready UI session, cold-start the
target explicitly, and capture which source revision/user the hook consumed.

## Deferred UX

- How to present Xposed-not-ready state.
- Whether to show a read-only app list, hide it, or show an explanation before
  the list.

These are presentation decisions; they do not change the confirmed save gate.
