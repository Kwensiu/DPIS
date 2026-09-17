# Modern Scope Backup Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Export a Modern-only snapshot of ordinary LSPosed application scope and offer restoring only those missing installed applications after import.

**Architecture:** Extend the portable backup document with an optional top-level scope section that stays outside DPIS configuration entries. A focused scope snapshot policy validates and filters data at both document boundaries, while an Android adapter reads framework scope and the existing prompt store persists the restored snapshot and one-shot state.

**Tech Stack:** Kotlin, `org.json`, JUnit4, libxposed service API, Jetpack Compose presentation.

---

## File Structure

- Create: `app/src/main/java/com/dpis/module/backup/ModuleScopeSnapshotPolicy.kt` - pure package normalization and candidate selection.
- Modify: `app/src/main/java/com/dpis/module/backup/ConfigBackupCodec.kt` - optional `moduleScope` JSON section and typed backup document.
- Modify: `app/src/main/java/com/dpis/module/backup/presentation/ConfigBackupCoordinator.kt` - atomically export/import a scope snapshot through explicit ports.
- Modify: `app/src/main/java/com/dpis/module/backup/presentation/ConfigBackupHost.kt` - supply Modern framework scope and persist the restored snapshot after successful restore.
- Modify: `app/src/main/java/com/dpis/module/applist/RestoreScopePromptStore.kt` - persist the sanitized package snapshot with its pending marker.
- Modify: `app/src/main/java/com/dpis/module/ui/presentation/MainHostWiringSession.kt` and `MainWorkspaceSession.kt` - resolve candidates from the restored snapshot instead of all configured apps.
- Modify: existing backup and prompt tests; create `ModuleScopeSnapshotPolicyTest.kt`.

### Task 1: Define the portable snapshot contract

**Files:**
- Create: `app/src/main/java/com/dpis/module/backup/ModuleScopeSnapshotPolicy.kt`
- Create: `app/src/test/java/com/dpis/module/backup/ModuleScopeSnapshotPolicyTest.kt`

- [ ] **Step 1: Write failing normalization tests**

```kotlin
@Test fun retainsInstalledAppPackageNamesAndDropsSpecialTargets() {
    assertThat(ModuleScopeSnapshotPolicy.normalize(setOf("com.android.settings", "android", "system_server", BuildConfig.APPLICATION_ID)))
        .containsExactly("com.android.settings")
}
```

- [ ] **Step 2: Run the focused test**

Run: `./gradlew :app:testModernDebugUnitTest --tests com.dpis.module.backup.ModuleScopeSnapshotPolicyTest`

Expected: FAIL because `ModuleScopeSnapshotPolicy` does not exist.

- [ ] **Step 3: Implement the pure policy**

```kotlin
object ModuleScopeSnapshotPolicy {
    fun normalize(packageNames: Iterable<String>): List<String>
    fun candidates(snapshot: Iterable<String>, installed: Set<String>, currentScope: Set<String>): List<String>
}
```

The implementation accepts valid dotted package identifiers, sorts and deduplicates them, and excludes `android`, `system_server`, and `BuildConfig.APPLICATION_ID`. Android installation checks remain outside this pure policy.

- [ ] **Step 4: Run focused tests**

Run: `./gradlew :app:testAllDebugUnitTests --tests com.dpis.module.backup.ModuleScopeSnapshotPolicyTest`

Expected: PASS.

### Task 2: Serialize the optional Modern scope section

**Files:**
- Modify: `app/src/main/java/com/dpis/module/backup/ConfigBackupCodec.kt`
- Modify: `app/src/test/java/com/dpis/module/backup/ConfigBackupCodecTest.kt`

- [ ] **Step 1: Write failing codec tests**

```kotlin
@Test fun encodeDocumentPreservesOptionalModuleScopeOutsideEntries() {
    val document = ConfigBackupCodec.decodeDocument(ConfigBackupCodec.encode(entries, listOf("com.android.settings")))
    assertThat(document.moduleScope).containsExactly("com.android.settings")
}
```

- [ ] **Step 2: Run codec tests**

Run: `./gradlew :app:testAllDebugUnitTests --tests com.dpis.module.backup.ConfigBackupCodecTest`

Expected: FAIL because the codec has no scope parameter or document field.

- [ ] **Step 3: Implement schema-compatible optional data**

Add `moduleScope` as a top-level JSON array, keep absent fields valid for historical backups, reject non-string array entries, and return a typed `BackupDocument` containing both entries and scope packages. Do not add this data to `entries` or `BackupKeyPolicy`.

- [ ] **Step 4: Run codec tests**

Run: `./gradlew :app:testAllDebugUnitTests --tests com.dpis.module.backup.ConfigBackupCodecTest`

Expected: PASS.

### Task 3: Wire Modern framework scope into backup transactions

**Files:**
- Modify: `app/src/main/java/com/dpis/module/backup/presentation/ConfigBackupCoordinator.kt`
- Modify: `app/src/main/java/com/dpis/module/backup/presentation/ConfigBackupHost.kt`
- Modify: `app/src/test/java/com/dpis/module/backup/presentation/ConfigBackupCoordinatorTest.kt`

- [ ] **Step 1: Write failing coordinator tests**

```kotlin
@Test fun restorePersistsSnapshotOnlyAfterConfigAndTemplatesSucceed() {
    coordinator.restore(uri)
    assertThat(scopeSnapshotStore.saved).containsExactly("com.android.settings")
}
```

- [ ] **Step 2: Run coordinator tests**

Run: `./gradlew :app:testAllDebugUnitTests --tests com.dpis.module.backup.presentation.ConfigBackupCoordinatorTest`

Expected: FAIL because the coordinator has no snapshot port.

- [ ] **Step 3: Add narrow ports and transactional ordering**

Pass an exporter that reads `DpisApplication.xposedService?.scope` only on Modern builds, filters it through `ModuleScopeSnapshotPolicy`, and passes it to the codec. On restore, return the sanitized decoded snapshot in the coordinator result; only the host persists it after a successful restore. Fail malformed input before replacing config or templates.

- [ ] **Step 4: Run coordinator tests**

Run: `./gradlew :app:testAllDebugUnitTests --tests com.dpis.module.backup.presentation.ConfigBackupCoordinatorTest`

Expected: PASS.

### Task 4: Replace the old prompt candidate source

**Files:**
- Modify: `app/src/main/java/com/dpis/module/applist/RestoreScopePromptStore.kt`
- Modify: `app/src/main/java/com/dpis/module/applist/RestoreScopePromptPolicy.kt`
- Modify: `app/src/main/java/com/dpis/module/ui/presentation/MainHostWiringSession.kt`
- Modify: `app/src/main/java/com/dpis/module/ui/presentation/MainWorkspaceSession.kt`
- Modify: `app/src/test/java/com/dpis/module/applist/RestoreScopePromptPolicyTest.kt`
- Modify: `app/src/test/java/com/dpis/module/applist/RestoreScopePromptStoreTest.kt`

- [ ] **Step 1: Write failing restore-prompt tests**

```kotlin
@Test fun candidatesUseImportedScopeNotAllConfiguredPackages() {
    assertThat(policy.candidatePackages(snapshot, importedScope)).containsExactly("com.android.settings")
}
```

- [ ] **Step 2: Run prompt tests**

Run: `./gradlew :app:testAllDebugUnitTests --tests com.dpis.module.applist.RestoreScopePromptPolicyTest --tests com.dpis.module.applist.RestoreScopePromptStoreTest`

Expected: FAIL because the prompt only stores a Boolean and computes all configured applications.

- [ ] **Step 3: Persist and consume imported scope exactly once**

Store the sanitized snapshot with the pending marker. Compute candidates from that snapshot intersected with installed catalog items that have a readable, absent scope state. Keep the prompt pending while scope state is unknown; consume it for a readable empty set, dismissal, or a successfully started batch request.

- [ ] **Step 4: Run prompt tests**

Run: `./gradlew :app:testAllDebugUnitTests --tests com.dpis.module.applist.RestoreScopePromptPolicyTest --tests com.dpis.module.applist.RestoreScopePromptStoreTest`

Expected: PASS.

### Task 5: Validate the integrated flavor behavior

**Files:**
- Modify: affected `*SourceSmokeTest.kt` files only where public wiring changes.

- [ ] **Step 1: Update semantic smoke anchors**

Assert that a successful Modern restore stores the returned snapshot and that Legacy does not create or consume scope snapshot data. Remove stale assertions for the all-configured candidate rule.

- [ ] **Step 2: Run focused suite and builds**

Run: `./gradlew :app:testAllDebugUnitTests`

Expected: PASS.

Run: `./gradlew :app:assembleModernDebug :app:assembleLegacyDebug`

Expected: both APKs assemble successfully.

- [ ] **Step 3: Install and inspect Modern behavior**

Run: `./gradlew :app:installModernDebug`

Expected: the connected device receives the APK. Export a backup with ordinary in-scope apps, import it, then confirm the prompt never includes `android`, `system_server`, or DPIS.
