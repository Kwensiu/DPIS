# Modern Scope Backup Design

## Goal

Modern backups preserve the source device's selected ordinary application
packages in the LSPosed scope. After a successful restore, DPIS offers to
request only the recorded packages that are installed on the destination and
are not already in its scope.

## Boundaries

Scope is framework-owned state, not package configuration. It is serialized in
the backup document as a top-level optional `moduleScope` array and is never
written into `DpisConfigStore`. The feature is Modern-only. Legacy neither
exports nor consumes the field. Backups without the field remain valid.

The export snapshot contains installed application packages, including
preinstalled system applications. It excludes `android`, `system_server`, the
DPIS package, and any non-application scope target. Import treats the document
as untrusted: it normalizes package names, removes special targets, deduplicates
them, and persists the sanitized snapshot separately from restored DPIS
configuration.

## Restore Flow

After the configuration and templates restore successfully, the sanitized scope
snapshot becomes a one-shot restore prompt. The prompt remains pending until
the Modern service can read the destination scope. Its candidate set is the
intersection of the stored snapshot, installed destination applications, and
packages absent from the destination scope. An empty, readable candidate set is
consumed without UI. A user dismissal or a successfully started batch request
also consumes it. The existing presentation can render this state, but the
backend contract is complete without a visual redesign.

## Failure Handling

An invalid `moduleScope` payload invalidates the backup before any store is
replaced. Scope snapshot persistence happens only after configuration and
template restore succeeds. If that persistence fails, the restore reports a
failure and retains the existing local configuration through the coordinator's
rollback path. A scope service read failure leaves the prompt pending and never
guesses that a package is outside the scope.

## Verification

JVM tests cover codec round-tripping and malformed scope data, special-target
filtering, restore atomicity, and candidate selection. Existing scope-request
tests cover the framework request gate. Modern and Legacy debug builds verify
flavor boundaries; the Modern build is installed on the connected device for a
manual export/import check.
