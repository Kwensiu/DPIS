# Font File Replacement Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build MVP per-app font file replacement with a global DPIS font library and ordinary Java `Typeface` hooks.

**Architecture:** Add a new typeface-replacement path parallel to DP/viewport and font-size paths. Store imported font metadata in a focused `FontLibraryStore`, store per-package selections in `DpiConfigStore`, and install a dedicated `TypefaceOverrideHookInstaller` only when a package has a selected font file. Do not reuse `FontApplyMode`, `FontScaleOverride`, or field-rewrite font-size logic for font file replacement.

**Tech Stack:** Android Java 17, SharedPreferences, Material Components XML UI, JUnit4, libxposed API 101, legacy compat100 source smoke checks.

---

## File Structure

- Create `app/src/main/java/com/dpis/module/FontLibraryEntry.java`
  Immutable record for imported fonts.
- Create `app/src/main/java/com/dpis/module/FontLibraryStore.java`
  Owns font metadata persistence, duplicate detection, referenced-font deletion checks, and readable font file paths.
- Create `app/src/main/java/com/dpis/module/TypefaceOverrideHookInstaller.java`
  App-process hook installer for ordinary Java text typeface replacement.
- Create `app/src/main/res/layout/dialog_font_library.xml`
  DPIS settings dialog for font import/list/delete.
- Modify `app/src/main/java/com/dpis/module/DpiConfigStore.java`
  Add `font.<package>.typeface_id` read/write/clear and package-membership rules.
- Modify `app/src/main/java/com/dpis/module/PackageConfigSnapshot.java`
  Add `targetTypefaceId`.
- Modify `app/src/main/java/com/dpis/module/ConfigSnapshotLoader.java`
  Populate `targetTypefaceId`.
- Modify `app/src/main/java/com/dpis/module/ModulePackagePlan.java`
  Add `typefaceActive` and `typefaceEnabled`; typeface-only config installs hooks.
- Modify `app/src/main/java/com/dpis/module/AppProcessHookInstaller.java`
  Install `TypefaceOverrideHookInstaller` independently of font-size hooks.
- Modify `app/src/main/java/com/dpis/module/AppListItem.java`
  Carry selected typeface ID for UI/status/filter decisions.
- Modify `app/src/main/java/com/dpis/module/AppLoadCoordinator.java` and `app/src/main/java/com/dpis/module/MainActivity.java` for `AppListItem` constructor and filter/status call sites.
- Modify `app/src/main/java/com/dpis/module/AppConfigDialogBinder.java`
  Bind the app-level font selector.
- Modify `app/src/main/java/com/dpis/module/AppConfigSaveHandler.java`
  Save/clear typeface ID independently of font scale.
- Modify `app/src/main/java/com/dpis/module/SystemServerSettingsActivity.java`
  Add font library entry, import flow, list, and delete behavior.
- Modify `app/src/main/res/layout/activity_system_server_settings.xml`
  Add row for Font library.
- Modify `app/src/main/res/layout/dialog_app_config.xml`
  Add per-app font selector separate from font-size controls.
- Modify `app/src/main/res/values/strings.xml` and `app/src/main/res/values-zh-rCN/strings.xml`
  Add labels/messages.
- Test `app/src/test/java/com/dpis/module/DpiConfigStoreTest.java`
- Test `app/src/test/java/com/dpis/module/FontLibraryStoreTest.java`
- Test `app/src/test/java/com/dpis/module/TypefaceOverrideHookInstallerTest.java`
- Test `app/src/test/java/com/dpis/module/ModulePackagePlanTest.java`
- Update source smoke tests for App config and settings wiring.

## Scope Guard

This plan intentionally does not implement HyperOS Flutter/native font file replacement. It also does not make backup JSON include font binary files. Runtime logs should make missing/unreadable font files visible without crashing the target app.

---

### Task 1: Add Typeface Config to `DpiConfigStore`

**Files:**
- Modify: `app/src/main/java/com/dpis/module/DpiConfigStore.java`
- Test: `app/src/test/java/com/dpis/module/DpiConfigStoreTest.java`

- [ ] **Step 1: Write failing store tests**

Add these tests near the existing font-scale tests in `DpiConfigStoreTest.java`:

```java
@Test
public void updatesTypefaceIdForConfiguredPackage() {
    FakePrefs prefs = new FakePrefs();
    DpiConfigStore store = new DpiConfigStore(prefs);

    assertTrue(store.setTargetTypefaceId("bin.mt.plus.canary", "font_abcd1234"));

    assertEquals("font_abcd1234", store.getTargetTypefaceId("bin.mt.plus.canary"));
    assertTrue(store.hasPrimaryTargetTypefaceId("bin.mt.plus.canary"));
    assertTrue(store.getConfiguredPackages().contains("bin.mt.plus.canary"));
}

@Test
public void clearsTypefaceIdAndRemovesPackageWhenItIsOnlyConfig() {
    FakePrefs prefs = new FakePrefs();
    DpiConfigStore store = new DpiConfigStore(prefs);
    assertTrue(store.setTargetTypefaceId("bin.mt.plus.canary", "font_abcd1234"));

    assertTrue(store.clearTargetTypefaceId("bin.mt.plus.canary"));

    assertNull(store.getTargetTypefaceId("bin.mt.plus.canary"));
    assertFalse(store.hasPrimaryTargetTypefaceId("bin.mt.plus.canary"));
    assertFalse(store.getConfiguredPackages().contains("bin.mt.plus.canary"));
}

@Test
public void keepsPackageConfiguredWhenClearingTypefaceButViewportExists() {
    FakePrefs prefs = new FakePrefs();
    DpiConfigStore store = new DpiConfigStore(prefs);
    assertTrue(store.setTargetViewportWidthDp("bin.mt.plus.canary", 360));
    assertTrue(store.setTargetTypefaceId("bin.mt.plus.canary", "font_abcd1234"));

    assertTrue(store.clearTargetTypefaceId("bin.mt.plus.canary"));

    assertEquals(Integer.valueOf(360), store.getTargetViewportWidthDp("bin.mt.plus.canary"));
    assertTrue(store.getConfiguredPackages().contains("bin.mt.plus.canary"));
}

@Test
public void keepsPackageConfiguredWhenClearingViewportButTypefaceExists() {
    FakePrefs prefs = new FakePrefs();
    DpiConfigStore store = new DpiConfigStore(prefs);
    assertTrue(store.setTargetViewportWidthDp("bin.mt.plus.canary", 360));
    assertTrue(store.setTargetTypefaceId("bin.mt.plus.canary", "font_abcd1234"));

    assertTrue(store.clearTargetViewportWidthDp("bin.mt.plus.canary"));

    assertNull(store.getTargetViewportWidthDp("bin.mt.plus.canary"));
    assertEquals("font_abcd1234", store.getTargetTypefaceId("bin.mt.plus.canary"));
    assertTrue(store.getConfiguredPackages().contains("bin.mt.plus.canary"));
}

@Test
public void clearTargetPackageConfigRemovesTypefaceId() {
    FakePrefs prefs = new FakePrefs();
    DpiConfigStore store = new DpiConfigStore(prefs);
    assertTrue(store.setTargetTypefaceId("bin.mt.plus.canary", "font_abcd1234"));

    assertTrue(store.clearTargetPackageConfig("bin.mt.plus.canary"));

    assertNull(store.getTargetTypefaceId("bin.mt.plus.canary"));
    assertFalse(store.getConfiguredPackages().contains("bin.mt.plus.canary"));
}
```

- [ ] **Step 2: Run tests and verify failure**

Run:

```powershell
./gradlew :app:testModern101DebugUnitTest --tests com.dpis.module.DpiConfigStoreTest
```

Expected: compile fails because `setTargetTypefaceId`, `getTargetTypefaceId`, `hasPrimaryTargetTypefaceId`, and `clearTargetTypefaceId` do not exist.

- [ ] **Step 3: Implement typeface config methods**

In `DpiConfigStore.java`, add these public package-private methods near the font-scale methods:

```java
String getTargetTypefaceId(String packageName) {
    String key = keyForTypefaceId(packageName);
    if (!contains(key)) {
        return null;
    }
    String value = getString(key, null);
    return normalizeTypefaceId(value);
}

boolean setTargetTypefaceId(String packageName, String typefaceId) {
    String normalized = normalizeTypefaceId(typefaceId);
    if (normalized == null) {
        return clearTargetTypefaceId(packageName);
    }
    LinkedHashSet<String> packages = new LinkedHashSet<>(getConfiguredPackages());
    packages.add(packageName);
    return commitBoth(editor -> editor
            .putStringSet(KEY_TARGET_PACKAGES, packages)
            .putString(keyForTypefaceId(packageName), normalized));
}

boolean clearTargetTypefaceId(String packageName) {
    LinkedHashSet<String> packages = new LinkedHashSet<>(getConfiguredPackages());
    if (getTargetViewportWidthDp(packageName) == null
            && getTargetFontScalePercent(packageName) == null
            && !contains(keyForViewportMode(packageName))
            && !contains(keyForFontMode(packageName))) {
        packages.remove(packageName);
    }
    return commitBoth(editor -> editor
            .putStringSet(KEY_TARGET_PACKAGES, packages)
            .remove(keyForTypefaceId(packageName)));
}

boolean hasPrimaryTargetTypefaceId(String packageName) {
    return containsInPrimary(keyForTypefaceId(packageName));
}
```

Add helpers near existing key helpers:

```java
private static String normalizeTypefaceId(String typefaceId) {
    if (typefaceId == null) {
        return null;
    }
    String trimmed = typefaceId.trim();
    if (trimmed.isEmpty()) {
        return null;
    }
    return trimmed;
}

private static String keyForTypefaceId(String packageName) {
    return "font." + packageName + ".typeface_id";
}
```

Update existing package-removal checks:

```java
// clearTargetViewportWidthDp
if (getTargetFontScalePercent(packageName) == null
        && getTargetTypefaceId(packageName) == null
        && !contains(keyForFontMode(packageName))) {
    packages.remove(packageName);
}

// setTargetViewportApplyMode OFF branch
if (getTargetViewportWidthDp(packageName) == null
        && getTargetFontScalePercent(packageName) == null
        && getTargetTypefaceId(packageName) == null
        && !contains(keyForFontMode(packageName))) {
    packages.remove(packageName);
}

// setTargetFontApplyMode OFF branch
if (getTargetViewportWidthDp(packageName) == null
        && getTargetFontScalePercent(packageName) == null
        && getTargetTypefaceId(packageName) == null) {
    packages.remove(packageName);
}

// clearTargetFontScalePercent
if (getTargetViewportWidthDp(packageName) == null
        && getTargetTypefaceId(packageName) == null
        && !contains(keyForFontMode(packageName))) {
    packages.remove(packageName);
}

// setTargetDpisEnabled enabled branch
if (getTargetViewportWidthDp(packageName) == null
        && getTargetFontScalePercent(packageName) == null
        && getTargetTypefaceId(packageName) == null
        && !contains(keyForViewportMode(packageName))
        && !contains(keyForFontMode(packageName))) {
    packages.remove(packageName);
    return commitBoth(editor -> editor
            .putStringSet(KEY_TARGET_PACKAGES, packages)
            .remove(keyForDpisEnabled(packageName)));
}
```

Update `clearTargetPackageConfig`:

```java
return commitBoth(editor -> editor
        .putStringSet(KEY_TARGET_PACKAGES, packages)
        .remove(keyForViewportWidth(packageName))
        .remove(keyForViewportMode(packageName))
        .remove(keyForFontScale(packageName))
        .remove(keyForFontMode(packageName))
        .remove(keyForTypefaceId(packageName))
        .remove(keyForDpisEnabled(packageName)));
```

- [ ] **Step 4: Run targeted tests**

Run:

```powershell
./gradlew :app:testModern101DebugUnitTest --tests com.dpis.module.DpiConfigStoreTest
```

Expected: `DpiConfigStoreTest` passes.

- [ ] **Step 5: Commit**

```powershell
git add app/src/main/java/com/dpis/module/DpiConfigStore.java app/src/test/java/com/dpis/module/DpiConfigStoreTest.java
git commit -m "feat: add per-app typeface config"
```

---

### Task 2: Add Snapshot and Package Plan Support

**Files:**
- Modify: `app/src/main/java/com/dpis/module/PackageConfigSnapshot.java`
- Modify: `app/src/main/java/com/dpis/module/ConfigSnapshotLoader.java`
- Modify: `app/src/main/java/com/dpis/module/ModulePackagePlan.java`
- Test: `app/src/test/java/com/dpis/module/ModulePackagePlanTest.java`
- Test: `app/src/test/java/com/dpis/module/ConfigSnapshotTest.java`

- [ ] **Step 1: Write failing plan tests**

Add to `ModulePackagePlanTest.java`:

```java
@Test
public void installsTypefaceHooksForTypefaceOnlyPackage() {
    DpiConfigStore store = new DpiConfigStore(new FakePrefs());
    store.setTargetTypefaceId("com.example.app", "font_abcd1234");

    ModulePackagePlan plan = ModulePackagePlan.resolve(store, "com.example.app");

    assertTrue(plan.shouldInstallHooks());
    assertFalse(plan.viewportConfigured);
    assertFalse(plan.fontScaleActive);
    assertFalse(plan.fontEnabled);
    assertTrue(plan.typefaceActive);
    assertTrue(plan.typefaceEnabled);
}

@Test
public void compat100LegacyDoesNotInstallForTypefaceOnlyPackage() {
    DpiConfigStore store = new DpiConfigStore(new FakePrefs());
    store.setTargetTypefaceId("com.example.app", "font_abcd1234");

    ModulePackagePlan plan = ModulePackagePlan.resolve(store, "com.example.app");

    assertTrue(plan.shouldInstallHooks());
    assertFalse(plan.shouldInstallCompat100LegacyHooks());
}

@Test
public void skipsTypefacePackageDisabledByTargetToggle() {
    DpiConfigStore store = new DpiConfigStore(new FakePrefs());
    store.setTargetTypefaceId("com.example.app", "font_abcd1234");
    store.setTargetDpisEnabled("com.example.app", false);

    ModulePackagePlan plan = ModulePackagePlan.resolve(store, "com.example.app");

    assertFalse(plan.shouldInstallHooks());
    assertTrue(plan.typefaceActive);
    assertFalse(plan.typefaceEnabled);
}
```

Add to `ConfigSnapshotTest.java`:

```java
@Test
public void snapshotIncludesTypefaceId() {
    FakePrefs prefs = new FakePrefs();
    DpiConfigStore store = new DpiConfigStore(prefs);
    store.setTargetTypefaceId("com.example.app", "font_abcd1234");

    ConfigSnapshot snapshot = ConfigSnapshotLoader.fromStore(store);
    PackageConfigSnapshot packageConfig = snapshot.getPackage("com.example.app");

    assertEquals("font_abcd1234", packageConfig.targetTypefaceId);
}
```

- [ ] **Step 2: Run tests and verify failure**

Run:

```powershell
./gradlew :app:testModern101DebugUnitTest --tests com.dpis.module.ModulePackagePlanTest --tests com.dpis.module.ConfigSnapshotTest
```

Expected: compile fails because `targetTypefaceId`, `typefaceActive`, and `typefaceEnabled` do not exist.

- [ ] **Step 3: Extend snapshot classes**

Update `PackageConfigSnapshot.java` constructor and fields:

```java
final String targetTypefaceId;

PackageConfigSnapshot(String packageName,
                      boolean dpisEnabled,
                      Integer targetViewportWidthDp,
                      String targetViewportMode,
                      Integer targetFontScalePercent,
                      String targetFontMode,
                      String targetTypefaceId,
                      boolean hyperOsFlutterFontHookEnabled) {
    this.packageName = packageName;
    this.dpisEnabled = dpisEnabled;
    this.targetViewportWidthDp = targetViewportWidthDp;
    this.targetViewportMode = ViewportApplyMode.normalize(targetViewportMode);
    this.targetFontScalePercent = targetFontScalePercent;
    this.targetFontMode = FontApplyMode.normalize(targetFontMode);
    this.targetTypefaceId = targetTypefaceId;
    this.hyperOsFlutterFontHookEnabled = hyperOsFlutterFontHookEnabled;
}
```

Update `ConfigSnapshotLoader.java` constructor call:

```java
packages.put(packageName, new PackageConfigSnapshot(
        packageName,
        store.isTargetDpisEnabled(packageName),
        store.getTargetViewportWidthDp(packageName),
        store.getTargetViewportApplyMode(packageName),
        store.getTargetFontScalePercent(packageName),
        store.getTargetFontApplyMode(packageName),
        store.getTargetTypefaceId(packageName),
        store.isHyperOsFlutterFontHookEnabled()));
```

- [ ] **Step 4: Extend `ModulePackagePlan`**

Add fields:

```java
final String targetTypefaceId;
final boolean typefaceActive;
final boolean typefaceEnabled;
```

Update constructor to accept and assign them. In `resolve(ConfigSnapshot snapshot, String packageName)`, read:

```java
String targetTypefaceId = packageConfig.targetTypefaceId;
boolean typefaceActive = targetTypefaceId != null && !targetTypefaceId.isBlank();
```

Change inactive condition:

```java
if (!targetDpisEnabled || (targetViewportWidthDp == null && !fontScaleActive && !typefaceActive)) {
    return new ModulePackagePlan(
            packageName,
            targetViewportWidthDp,
            targetViewportMode,
            targetFontScalePercent,
            targetFontMode,
            targetTypefaceId,
            targetDpisEnabled,
            targetViewportWidthDp != null,
            false,
            fontScaleActive,
            false,
            typefaceActive,
            false);
}
```

When active, return:

```java
return new ModulePackagePlan(
        packageName,
        targetViewportWidthDp,
        targetViewportMode,
        targetFontScalePercent,
        targetFontMode,
        targetTypefaceId,
        true,
        viewportConfigured,
        viewportEnabled,
        fontScaleActive,
        fontHookPlan.emulationEnabled || fontHookPlan.fieldRewriteEnabled,
        typefaceActive,
        typefaceActive);
```

Update `shouldInstallHooks()`:

```java
boolean shouldInstallHooks() {
    return targetDpisEnabled && (viewportEnabled || fontEnabled || typefaceEnabled);
}
```

Do not include `typefaceEnabled` in `shouldInstallCompat100LegacyHooks()`.

- [ ] **Step 5: Run targeted tests**

Run:

```powershell
./gradlew :app:testModern101DebugUnitTest --tests com.dpis.module.ModulePackagePlanTest --tests com.dpis.module.ConfigSnapshotTest
```

Expected: tests pass.

- [ ] **Step 6: Commit**

```powershell
git add app/src/main/java/com/dpis/module/PackageConfigSnapshot.java app/src/main/java/com/dpis/module/ConfigSnapshotLoader.java app/src/main/java/com/dpis/module/ModulePackagePlan.java app/src/test/java/com/dpis/module/ModulePackagePlanTest.java app/src/test/java/com/dpis/module/ConfigSnapshotTest.java
git commit -m "feat: include typeface config in package planning"
```

---

### Task 3: Build Font Library Store

**Files:**
- Create: `app/src/main/java/com/dpis/module/FontLibraryEntry.java`
- Create: `app/src/main/java/com/dpis/module/FontLibraryStore.java`
- Test: `app/src/test/java/com/dpis/module/FontLibraryStoreTest.java`

- [ ] **Step 1: Write failing `FontLibraryStoreTest`**

Create `FontLibraryStoreTest.java`:

```java
package com.dpis.module;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public final class FontLibraryStoreTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void registersImportedFontMetadataFromCopiedFile() throws Exception {
        FakePrefs prefs = new FakePrefs();
        File dir = temporaryFolder.newFolder("fonts");
        FontLibraryStore store = new FontLibraryStore(prefs, dir);
        File source = writeFile("Example.ttf", "fake-font-data");

        FontLibraryEntry entry = store.registerCopiedFontForTest(source, "Example.ttf", 1234L);

        assertNotNull(entry);
        assertTrue(entry.id.startsWith("font_"));
        assertEquals("Example.ttf", entry.displayName);
        assertEquals("Example.ttf", entry.sourceFileName);
        assertTrue(new File(dir, entry.storedFileName).isFile());
        assertEquals(List.of(entry), store.listFonts());
    }

    @Test
    public void reusesExistingFontForDuplicateHash() throws Exception {
        FakePrefs prefs = new FakePrefs();
        File dir = temporaryFolder.newFolder("fonts");
        FontLibraryStore store = new FontLibraryStore(prefs, dir);
        File first = writeFile("First.ttf", "same-data");
        File second = writeFile("Second.ttf", "same-data");

        FontLibraryEntry firstEntry = store.registerCopiedFontForTest(first, "First.ttf", 100L);
        FontLibraryEntry secondEntry = store.registerCopiedFontForTest(second, "Second.ttf", 200L);

        assertEquals(firstEntry.id, secondEntry.id);
        assertEquals(1, store.listFonts().size());
    }

    @Test
    public void deletesUnusedFont() throws Exception {
        FakePrefs prefs = new FakePrefs();
        DpiConfigStore configStore = new DpiConfigStore(new FakePrefs());
        File dir = temporaryFolder.newFolder("fonts");
        FontLibraryStore store = new FontLibraryStore(prefs, dir);
        FontLibraryEntry entry = store.registerCopiedFontForTest(
                writeFile("Example.ttf", "fake-font-data"), "Example.ttf", 1234L);

        FontLibraryStore.DeleteResult result = store.deleteFont(entry.id, configStore);

        assertEquals(FontLibraryStore.DeleteResult.DELETED, result);
        assertTrue(store.listFonts().isEmpty());
        assertFalse(new File(dir, entry.storedFileName).exists());
    }

    @Test
    public void refusesDeleteWhenFontIsReferenced() throws Exception {
        FakePrefs prefs = new FakePrefs();
        DpiConfigStore configStore = new DpiConfigStore(new FakePrefs());
        File dir = temporaryFolder.newFolder("fonts");
        FontLibraryStore store = new FontLibraryStore(prefs, dir);
        FontLibraryEntry entry = store.registerCopiedFontForTest(
                writeFile("Example.ttf", "fake-font-data"), "Example.ttf", 1234L);
        configStore.setTargetTypefaceId("com.example.app", entry.id);

        FontLibraryStore.DeleteResult result = store.deleteFont(entry.id, configStore);

        assertEquals(FontLibraryStore.DeleteResult.IN_USE, result);
        assertEquals(1, store.listFonts().size());
    }

    @Test
    public void resolvesExistingFontFile() throws Exception {
        FakePrefs prefs = new FakePrefs();
        File dir = temporaryFolder.newFolder("fonts");
        FontLibraryStore store = new FontLibraryStore(prefs, dir);
        FontLibraryEntry entry = store.registerCopiedFontForTest(
                writeFile("Example.ttf", "fake-font-data"), "Example.ttf", 1234L);

        File resolved = store.resolveFontFile(entry.id);

        assertNotNull(resolved);
        assertTrue(resolved.isFile());
    }

    private File writeFile(String name, String content) throws Exception {
        File file = temporaryFolder.newFile(name);
        try (FileOutputStream output = new FileOutputStream(file)) {
            output.write(content.getBytes(StandardCharsets.UTF_8));
        }
        return file;
    }
}
```

- [ ] **Step 2: Run tests and verify failure**

Run:

```powershell
./gradlew :app:testModern101DebugUnitTest --tests com.dpis.module.FontLibraryStoreTest
```

Expected: compile fails because `FontLibraryStore` and `FontLibraryEntry` do not exist.

- [ ] **Step 3: Create `FontLibraryEntry`**

Create `FontLibraryEntry.java`:

```java
package com.dpis.module;

import java.util.Objects;

final class FontLibraryEntry {
    final String id;
    final String displayName;
    final String sourceFileName;
    final String storedFileName;
    final String storedPath;
    final String sha256;
    final long importedAtEpochMs;

    FontLibraryEntry(String id,
                     String displayName,
                     String sourceFileName,
                     String storedFileName,
                     String storedPath,
                     String sha256,
                     long importedAtEpochMs) {
        this.id = id;
        this.displayName = displayName;
        this.sourceFileName = sourceFileName;
        this.storedFileName = storedFileName;
        this.storedPath = storedPath;
        this.sha256 = sha256;
        this.importedAtEpochMs = importedAtEpochMs;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof FontLibraryEntry that)) {
            return false;
        }
        return importedAtEpochMs == that.importedAtEpochMs
                && Objects.equals(id, that.id)
                && Objects.equals(displayName, that.displayName)
                && Objects.equals(sourceFileName, that.sourceFileName)
                && Objects.equals(storedFileName, that.storedFileName)
                && Objects.equals(storedPath, that.storedPath)
                && Objects.equals(sha256, that.sha256);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, displayName, sourceFileName, storedFileName,
                storedPath, sha256, importedAtEpochMs);
    }
}
```

- [ ] **Step 4: Create `FontLibraryStore`**

Create `FontLibraryStore.java`:

```java
package com.dpis.module;

import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

final class FontLibraryStore {
    private static final String KEY_FONT_LIBRARY_ENTRIES = "font.library.entries";
    private static final int ID_HASH_CHARS = 12;

    enum DeleteResult {
        DELETED,
        NOT_FOUND,
        IN_USE,
        DELETE_FAILED
    }

    private final SharedPreferences preferences;
    private final File fontDirectory;

    FontLibraryStore(SharedPreferences preferences, File fontDirectory) {
        this.preferences = preferences;
        this.fontDirectory = fontDirectory;
    }

    List<FontLibraryEntry> listFonts() {
        List<FontLibraryEntry> entries = readEntries();
        entries.sort(Comparator
                .comparing((FontLibraryEntry entry) -> entry.displayName, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(entry -> entry.id));
        return entries;
    }

    FontLibraryEntry findById(String id) {
        if (id == null || id.isBlank()) {
            return null;
        }
        for (FontLibraryEntry entry : readEntries()) {
            if (id.equals(entry.id)) {
                return entry;
            }
        }
        return null;
    }

    File resolveFontFile(String id) {
        FontLibraryEntry entry = findById(id);
        if (entry == null || entry.storedPath == null || entry.storedPath.isBlank()) {
            return null;
        }
        File file = new File(entry.storedPath);
        return file.isFile() ? file : null;
    }

    DeleteResult deleteFont(String id, DpiConfigStore configStore) {
        FontLibraryEntry target = findById(id);
        if (target == null) {
            return DeleteResult.NOT_FOUND;
        }
        if (isReferenced(id, configStore)) {
            return DeleteResult.IN_USE;
        }
        File file = new File(fontDirectory, target.storedFileName);
        if (file.exists() && !file.delete()) {
            return DeleteResult.DELETE_FAILED;
        }
        List<FontLibraryEntry> remaining = new ArrayList<>();
        for (FontLibraryEntry entry : readEntries()) {
            if (!target.id.equals(entry.id)) {
                remaining.add(entry);
            }
        }
        return writeEntries(remaining) ? DeleteResult.DELETED : DeleteResult.DELETE_FAILED;
    }

    FontLibraryEntry registerCopiedFontForTest(File sourceFile,
                                               String sourceFileName,
                                               long importedAtEpochMs) throws IOException {
        byte[] bytes = readAllBytes(sourceFile);
        String sha256 = sha256(bytes);
        String id = idFromHash(sha256);
        for (FontLibraryEntry entry : readEntries()) {
            if (sha256.equals(entry.sha256)) {
                return entry;
            }
        }
        if (!fontDirectory.exists() && !fontDirectory.mkdirs()) {
            throw new IOException("Unable to create font directory: " + fontDirectory);
        }
        String extension = extensionFor(sourceFileName);
        String storedFileName = id + extension;
        File targetFile = new File(fontDirectory, storedFileName);
        try (FileOutputStream output = new FileOutputStream(targetFile)) {
            output.write(bytes);
        }
        targetFile.setReadable(true, false);
        FontLibraryEntry entry = new FontLibraryEntry(
                id,
                sanitizeDisplayName(sourceFileName),
                sourceFileName,
                storedFileName,
                targetFile.getAbsolutePath(),
                sha256,
                importedAtEpochMs);
        List<FontLibraryEntry> entries = new ArrayList<>(readEntries());
        entries.add(entry);
        if (!writeEntries(entries)) {
            throw new IOException("Unable to write font library metadata");
        }
        return entry;
    }

    private boolean isReferenced(String id, DpiConfigStore configStore) {
        if (id == null || configStore == null) {
            return false;
        }
        for (String packageName : configStore.getConfiguredPackages()) {
            if (id.equals(configStore.getTargetTypefaceId(packageName))) {
                return true;
            }
        }
        return false;
    }

    private List<FontLibraryEntry> readEntries() {
        String raw = preferences.getString(KEY_FONT_LIBRARY_ENTRIES, "[]");
        if (raw == null || raw.isBlank()) {
            return Collections.emptyList();
        }
        try {
            JSONArray array = new JSONArray(raw);
            List<FontLibraryEntry> entries = new ArrayList<>();
            Set<String> seenIds = new LinkedHashSet<>();
            for (int i = 0; i < array.length(); i++) {
                JSONObject object = array.optJSONObject(i);
                FontLibraryEntry entry = decodeEntry(object);
                if (entry != null && seenIds.add(entry.id)) {
                    entries.add(entry);
                }
            }
            return entries;
        } catch (JSONException ignored) {
            return Collections.emptyList();
        }
    }

    private boolean writeEntries(List<FontLibraryEntry> entries) {
        JSONArray array = new JSONArray();
        for (FontLibraryEntry entry : entries) {
            array.put(encodeEntry(entry));
        }
        return preferences.edit().putString(KEY_FONT_LIBRARY_ENTRIES, array.toString()).commit();
    }

    private static JSONObject encodeEntry(FontLibraryEntry entry) {
        JSONObject object = new JSONObject();
        try {
            object.put("id", entry.id);
            object.put("displayName", entry.displayName);
            object.put("sourceFileName", entry.sourceFileName);
            object.put("storedFileName", entry.storedFileName);
            object.put("storedPath", entry.storedPath);
            object.put("sha256", entry.sha256);
            object.put("importedAtEpochMs", entry.importedAtEpochMs);
        } catch (JSONException ignored) {
        }
        return object;
    }

    private static FontLibraryEntry decodeEntry(JSONObject object) {
        if (object == null) {
            return null;
        }
        String id = object.optString("id", "");
        String storedFileName = object.optString("storedFileName", "");
        String storedPath = object.optString("storedPath", "");
        String sha256 = object.optString("sha256", "");
        if (id.isBlank() || storedFileName.isBlank() || storedPath.isBlank() || sha256.isBlank()) {
            return null;
        }
        return new FontLibraryEntry(
                id,
                object.optString("displayName", storedFileName),
                object.optString("sourceFileName", storedFileName),
                storedFileName,
                storedPath,
                sha256,
                object.optLong("importedAtEpochMs", 0L));
    }

    private static String sanitizeDisplayName(String sourceFileName) {
        if (sourceFileName == null || sourceFileName.isBlank()) {
            return "Imported font";
        }
        return sourceFileName;
    }

    private static String extensionFor(String sourceFileName) {
        if (sourceFileName == null) {
            return ".ttf";
        }
        String lower = sourceFileName.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".otf")) {
            return ".otf";
        }
        return ".ttf";
    }

    private static String idFromHash(String sha256) {
        return "font_" + sha256.substring(0, Math.min(ID_HASH_CHARS, sha256.length()));
    }

    private static byte[] readAllBytes(File file) throws IOException {
        try (FileInputStream input = new FileInputStream(file)) {
            return input.readAllBytes();
        }
    }

    private static String sha256(byte[] bytes) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(bytes);
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte value : hash) {
                builder.append(String.format(Locale.ROOT, "%02x", value));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("SHA-256 unavailable", e);
        }
    }
}
```

- [ ] **Step 5: Run targeted tests**

Run:

```powershell
./gradlew :app:testModern101DebugUnitTest --tests com.dpis.module.FontLibraryStoreTest
```

Expected: tests pass.

- [ ] **Step 6: Commit**

```powershell
git add app/src/main/java/com/dpis/module/FontLibraryEntry.java app/src/main/java/com/dpis/module/FontLibraryStore.java app/src/test/java/com/dpis/module/FontLibraryStoreTest.java
git commit -m "feat: add font library store"
```

---

### Task 4: Add Typeface Runtime Hook Installer

**Files:**
- Create: `app/src/main/java/com/dpis/module/TypefaceOverrideHookInstaller.java`
- Modify: `app/src/main/java/com/dpis/module/AppProcessHookInstaller.java`
- Modify: `app/src/main/java/com/dpis/module/ConfigStoreFactory.java`
- Modify: `app/src/modern101/java/com/dpis/module/ModuleMain.java`
- Test: `app/src/test/java/com/dpis/module/TypefaceOverrideHookInstallerTest.java`
- Test: `app/src/test/java/com/dpis/module/AppProcessHookInstallerTest.java`

- [ ] **Step 1: Write failing style-resolution tests**

Create `TypefaceOverrideHookInstallerTest.java`:

```java
package com.dpis.module;

import android.graphics.Typeface;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public final class TypefaceOverrideHookInstallerTest {
    @Test
    public void explicitStyleWinsWhenPresent() {
        int style = TypefaceOverrideHookInstaller.resolveStyleForTest(Typeface.ITALIC, Typeface.BOLD);

        assertEquals(Typeface.BOLD, style);
    }

    @Test
    public void originalTypefaceStyleIsUsedWhenExplicitStyleMissing() {
        int style = TypefaceOverrideHookInstaller.resolveStyleForTest(Typeface.BOLD_ITALIC, null);

        assertEquals(Typeface.BOLD_ITALIC, style);
    }

    @Test
    public void normalStyleIsUsedWhenNoStyleExists() {
        int style = TypefaceOverrideHookInstaller.resolveStyleForTest(null, null);

        assertEquals(Typeface.NORMAL, style);
    }

    @Test
    public void targetTypefaceFallsBackToBaseWhenStyleCreationFails() {
        Typeface result = TypefaceOverrideHookInstaller.resolveReplacementForTest(null, null);

        assertEquals(null, result);
    }
}
```

Add a source smoke to `AppProcessHookInstallerTest.java` or extend existing assertions:

```java
@Test
public void typefacePlanDoesNotEnableFontScaleHooks() {
    DpiConfigStore store = new DpiConfigStore(new FakePrefs());
    store.setTargetTypefaceId("com.example.app", "font_abcd1234");

    ModulePackagePlan plan = ModulePackagePlan.resolve(store, "com.example.app");
    AppProcessHookInstaller.FontHookPlan fontHookPlan =
            AppProcessHookInstaller.resolveFontHookPlan(null, plan.fontScaleActive, plan.targetFontMode);

    assertFalse(fontHookPlan.emulationEnabled);
    assertFalse(fontHookPlan.fieldRewriteEnabled);
    assertTrue(plan.typefaceEnabled);
}
```

- [ ] **Step 2: Run tests and verify failure**

Run:

```powershell
./gradlew :app:testModern101DebugUnitTest --tests com.dpis.module.TypefaceOverrideHookInstallerTest --tests com.dpis.module.AppProcessHookInstallerTest
```

Expected: compile fails because `TypefaceOverrideHookInstaller` does not exist.

- [ ] **Step 3: Create hook installer**

Create `TypefaceOverrideHookInstaller.java`:

```java
package com.dpis.module;

import android.graphics.Paint;
import android.graphics.Typeface;
import android.widget.TextView;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.github.libxposed.api.XposedInterface;

final class TypefaceOverrideHookInstaller {
    private static final String LOG_PREFIX = "DPIS_FONT_STYLE ";
    private static volatile boolean hookInstalled;
    private static final Map<String, String> LAST_MESSAGES = new ConcurrentHashMap<>();
    private static final ThreadLocal<Boolean> INTERNAL_UPDATE =
            ThreadLocal.withInitial(() -> Boolean.FALSE);

    private TypefaceOverrideHookInstaller() {
    }

    static void install(XposedInterface xposed,
                        String packageName,
                        DpiConfigStore store,
                        FontLibraryStore fontLibraryStore) throws ReflectiveOperationException {
        if (hookInstalled) {
            return;
        }
        synchronized (TypefaceOverrideHookInstaller.class) {
            if (hookInstalled) {
                return;
            }
            Typeface baseTypeface = loadTargetTypeface(packageName, store, fontLibraryStore);
            if (baseTypeface == null) {
                return;
            }
            ClassLoader bootClassLoader = ClassLoader.getSystemClassLoader();
            Class<?> textViewClass = Class.forName("android.widget.TextView", false, bootClassLoader);
            Method setTypeface = textViewClass.getDeclaredMethod("setTypeface", Typeface.class);
            xposed.hook(setTypeface)
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        if (Boolean.TRUE.equals(INTERNAL_UPDATE.get())) {
                            return chain.proceed();
                        }
                        Typeface original = (Typeface) chain.getArg(0);
                        Typeface replacement = resolveReplacement(baseTypeface, original, null);
                        if (replacement == null) {
                            return chain.proceed();
                        }
                        chain.setArg(0, replacement);
                        return chain.proceed();
                    });

            Method setTypefaceWithStyle =
                    textViewClass.getDeclaredMethod("setTypeface", Typeface.class, int.class);
            xposed.hook(setTypefaceWithStyle)
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        if (Boolean.TRUE.equals(INTERNAL_UPDATE.get())) {
                            return chain.proceed();
                        }
                        Typeface original = (Typeface) chain.getArg(0);
                        Integer style = (Integer) chain.getArg(1);
                        Typeface replacement = resolveReplacement(baseTypeface, original, style);
                        if (replacement == null) {
                            return chain.proceed();
                        }
                        chain.setArg(0, replacement);
                        return chain.proceed();
                    });

            Method paintSetTypeface = Paint.class.getDeclaredMethod("setTypeface", Typeface.class);
            xposed.hook(paintSetTypeface)
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        if (Boolean.TRUE.equals(INTERNAL_UPDATE.get())) {
                            return chain.proceed();
                        }
                        Typeface original = (Typeface) chain.getArg(0);
                        Typeface replacement = resolveReplacement(baseTypeface, original, null);
                        if (replacement == null) {
                            return chain.proceed();
                        }
                        chain.setArg(0, replacement);
                        return chain.proceed();
                    });
            hookInstalled = true;
            DpisLog.i(LOG_PREFIX + "hook ready for " + packageName);
        }
    }

    private static Typeface loadTargetTypeface(String packageName,
                                               DpiConfigStore store,
                                               FontLibraryStore fontLibraryStore) {
        String typefaceId = store != null ? store.getTargetTypefaceId(packageName) : null;
        if (typefaceId == null || fontLibraryStore == null) {
            return null;
        }
        File file = fontLibraryStore.resolveFontFile(typefaceId);
        if (file == null || !file.canRead()) {
            logIfChanged(packageName + ":unreadable",
                    LOG_PREFIX + "font file unreadable: package=" + packageName
                            + ", typefaceId=" + typefaceId);
            return null;
        }
        try {
            return Typeface.createFromFile(file);
        } catch (Throwable throwable) {
            logIfChanged(packageName + ":load-failed",
                    LOG_PREFIX + "font load failed: package=" + packageName
                            + ", typefaceId=" + typefaceId
                            + ", error=" + throwable.getClass().getSimpleName());
            return null;
        }
    }

    static int resolveStyleForTest(Integer originalStyle, Integer explicitStyle) {
        return resolveStyle(originalStyle, explicitStyle);
    }

    static Typeface resolveReplacementForTest(Typeface baseTypeface, Typeface original) {
        return resolveReplacement(baseTypeface, original, null);
    }

    private static Typeface resolveReplacement(Typeface baseTypeface,
                                               Typeface original,
                                               Integer explicitStyle) {
        if (baseTypeface == null) {
            return original;
        }
        int originalStyle = original != null ? original.getStyle() : Typeface.NORMAL;
        int style = resolveStyle(originalStyle, explicitStyle);
        try {
            Typeface styled = Typeface.create(baseTypeface, style);
            return styled != null ? styled : baseTypeface;
        } catch (Throwable ignored) {
            return baseTypeface;
        }
    }

    private static int resolveStyle(Integer originalStyle, Integer explicitStyle) {
        if (explicitStyle != null) {
            return explicitStyle;
        }
        return originalStyle != null ? originalStyle : Typeface.NORMAL;
    }

    private static void logIfChanged(String key, String message) {
        String previous = LAST_MESSAGES.put(key, message);
        if (!message.equals(previous)) {
            DpisLog.i(message);
        }
    }
}
```

- [ ] **Step 4: Add font library factory methods**

In `ConfigStoreFactory.java`, add:

```java
static FontLibraryStore createFontLibraryForModuleApp(Context context) {
    SharedPreferences preferences =
            context.getSharedPreferences(DpiConfigStore.GROUP, Context.MODE_PRIVATE);
    return new FontLibraryStore(preferences, new java.io.File(context.getFilesDir(), "fonts"));
}

static FontLibraryStore createFontLibraryForModuleApp(Context context, XposedService service) {
    SharedPreferences localPreferences =
            context.getSharedPreferences(DpiConfigStore.GROUP, Context.MODE_PRIVATE);
    SharedPreferences preferences = localPreferences;
    if (service != null) {
        try {
            SharedPreferences remotePreferences = service.getRemotePreferences(DpiConfigStore.GROUP);
            if (remotePreferences != null) {
                preferences = remotePreferences;
            }
        } catch (Throwable ignored) {
            preferences = localPreferences;
        }
    }
    return new FontLibraryStore(preferences, new java.io.File(context.getFilesDir(), "fonts"));
}

static FontLibraryStore createFontLibraryForXposedHost(XposedInterface xposed) {
    SharedPreferences remotePreferences = null;
    if (xposed != null) {
        try {
            remotePreferences = xposed.getRemotePreferences(DpiConfigStore.GROUP);
        } catch (Throwable ignored) {
        }
    }
    if (remotePreferences != null) {
        return new FontLibraryStore(remotePreferences, null);
    }
    return new FontLibraryStore(
            new XSharedPreferencesAdapter(BuildConfig.APPLICATION_ID, DpiConfigStore.GROUP),
            null);
}
```

- [ ] **Step 5: Wire installer into app-process plan**

Modify `AppProcessHookInstaller.install(...)` signature to accept `boolean typefaceActive`, matching `ModuleMain` call sites after Task 2. The intended call is:

```java
if (typefaceActive) {
    TypefaceOverrideHookInstaller.install(
            xposed,
            packageName,
            store,
            ConfigStoreFactory.createFontLibraryForXposedHost(xposed));
}
```

- [ ] **Step 6: Run targeted tests**

Run:

```powershell
./gradlew :app:testModern101DebugUnitTest --tests com.dpis.module.TypefaceOverrideHookInstallerTest --tests com.dpis.module.AppProcessHookInstallerTest
```

Expected: tests pass with `TypefaceOverrideHookInstaller` receiving `FontLibraryStore` from `ConfigStoreFactory.createFontLibraryForXposedHost(xposed)`.

- [ ] **Step 7: Commit**

```powershell
git add app/src/main/java/com/dpis/module/TypefaceOverrideHookInstaller.java app/src/main/java/com/dpis/module/AppProcessHookInstaller.java app/src/main/java/com/dpis/module/ConfigStoreFactory.java app/src/modern101/java/com/dpis/module/ModuleMain.java app/src/test/java/com/dpis/module/TypefaceOverrideHookInstallerTest.java app/src/test/java/com/dpis/module/AppProcessHookInstallerTest.java
git commit -m "feat: add typeface override hook installer"
```

---

### Task 5: Add Settings Font Library UI and Import Flow

**Files:**
- Create: `app/src/main/res/layout/dialog_font_library.xml`
- Modify: `app/src/main/res/layout/activity_system_server_settings.xml`
- Modify: `app/src/main/java/com/dpis/module/SystemServerSettingsActivity.java`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-zh-rCN/strings.xml`
- Test: `app/src/test/java/com/dpis/module/SystemServerSettingsLayoutSmokeTest.java`
- Test: `app/src/test/java/com/dpis/module/SystemServerSettingsActivityFontLibrarySourceTest.java`

- [ ] **Step 1: Add failing source smoke tests**

Create `SystemServerSettingsActivityFontLibrarySourceTest.java`:

```java
package com.dpis.module;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertTrue;

public final class SystemServerSettingsActivityFontLibrarySourceTest {
    @Test
    public void settingsActivityContainsFontLibraryEntryAndPicker() throws Exception {
        String source = Files.readString(
                Path.of("src/main/java/com/dpis/module/SystemServerSettingsActivity.java"),
                StandardCharsets.UTF_8);

        assertTrue(source.contains("row_font_library"));
        assertTrue(source.contains("showFontLibraryDialog"));
        assertTrue(source.contains("Intent.ACTION_OPEN_DOCUMENT"));
        assertTrue(source.contains("font/ttf"));
        assertTrue(source.contains("font/otf"));
    }

    @Test
    public void settingsLayoutContainsFontLibraryRow() throws Exception {
        String layout = Files.readString(
                Path.of("src/main/res/layout/activity_system_server_settings.xml"),
                StandardCharsets.UTF_8);

        assertTrue(layout.contains("@+id/row_font_library"));
    }
}
```

- [ ] **Step 2: Run tests and verify failure**

Run:

```powershell
./gradlew :app:testModern101DebugUnitTest --tests com.dpis.module.SystemServerSettingsActivityFontLibrarySourceTest
```

Expected: tests fail because UI row and dialog methods do not exist.

- [ ] **Step 3: Add strings**

Add to `app/src/main/res/values/strings.xml`:

```xml
<string name="settings_font_library_label">Font library</string>
<string name="settings_font_library_hint">Import and manage fonts for per-app replacement.</string>
<string name="font_library_dialog_title">Font library</string>
<string name="font_library_import_action">Import font</string>
<string name="font_library_empty">No imported fonts</string>
<string name="font_library_import_success">Imported %1$s</string>
<string name="font_library_import_failed">Failed to import font</string>
<string name="font_library_delete_failed">Failed to delete font</string>
<string name="font_library_delete_in_use">Font is used by an app. Clear it before deleting.</string>
<string name="font_library_picker_failed">Unable to open file picker</string>
```

Add equivalent Chinese strings to `values-zh-rCN/strings.xml`.

- [ ] **Step 4: Add dialog layout**

Create `dialog_font_library.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="vertical"
    android:paddingStart="20dp"
    android:paddingTop="20dp"
    android:paddingEnd="20dp"
    android:paddingBottom="20dp">

    <com.google.android.material.textview.MaterialTextView
        android:id="@+id/font_library_title"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="@string/font_library_dialog_title"
        android:textAppearance="@style/TextAppearance.Material3.TitleLarge"
        android:textStyle="bold" />

    <com.google.android.material.button.MaterialButton
        android:id="@+id/font_library_import_button"
        style="@style/Widget.Dpis.DialogActionButton.Filled"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="14dp"
        android:text="@string/font_library_import_action" />

    <com.google.android.material.textview.MaterialTextView
        android:id="@+id/font_library_empty"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="14dp"
        android:text="@string/font_library_empty"
        android:textAppearance="@style/TextAppearance.Material3.BodyMedium" />

    <LinearLayout
        android:id="@+id/font_library_list"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="10dp"
        android:orientation="vertical" />
</LinearLayout>
```

- [ ] **Step 5: Add settings row**

In `activity_system_server_settings.xml`, add this row in the General card after `row_font_debug_overlay` or before HyperOS Flutter switch:

```xml
<com.google.android.material.divider.MaterialDivider
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_marginStart="16dp"
    android:layout_marginEnd="16dp" />

<include
    android:id="@+id/row_font_library"
    layout="@layout/item_settings_entry" />
```

- [ ] **Step 6: Wire `SystemServerSettingsActivity`**

Add fields:

```java
private static final int REQUEST_IMPORT_FONT = 3003;
private View fontLibraryEntryRow;
private BottomSheetDialog fontLibraryDialog;
```

Bind row in `onCreate` near other entries:

```java
fontLibraryEntryRow = bindEntryRow(
        R.id.row_font_library,
        R.drawable.ic_log_24,
        R.string.settings_font_library_label,
        R.string.settings_font_library_hint,
        v -> showFontLibraryDialog());
```

Handle activity result alongside backup import/export:

```java
if (requestCode == REQUEST_IMPORT_FONT && resultCode == RESULT_OK && data != null) {
    importFont(data.getData());
    return;
}
```

Add picker:

```java
private void openFontImportPicker() {
    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT)
            .addCategory(Intent.CATEGORY_OPENABLE)
            .setType("*/*")
            .putExtra(Intent.EXTRA_MIME_TYPES, new String[] {
                    "font/ttf",
                    "font/otf",
                    "application/x-font-ttf",
                    "application/vnd.ms-opentype"
            });
    try {
        startActivityForResult(intent, REQUEST_IMPORT_FONT);
    } catch (Throwable ignored) {
        showToast(R.string.font_library_picker_failed);
    }
}
```

Add import implementation using `FontLibraryStore`. The import must copy to a temporary file, validate with `Typeface.createFromFile(tempFile)`, then call the store registration method that writes metadata.

- [ ] **Step 7: Run targeted tests**

Run:

```powershell
./gradlew :app:testModern101DebugUnitTest --tests com.dpis.module.SystemServerSettingsActivityFontLibrarySourceTest
```

Expected: tests pass.

- [ ] **Step 8: Commit**

```powershell
git add app/src/main/res/layout/dialog_font_library.xml app/src/main/res/layout/activity_system_server_settings.xml app/src/main/java/com/dpis/module/SystemServerSettingsActivity.java app/src/main/res/values/strings.xml app/src/main/res/values-zh-rCN/strings.xml app/src/test/java/com/dpis/module/SystemServerSettingsActivityFontLibrarySourceTest.java
git commit -m "feat: add font library settings UI"
```

---

### Task 6: Add Per-App Typeface Selector

**Files:**
- Modify: `app/src/main/res/layout/dialog_app_config.xml`
- Modify: `app/src/main/java/com/dpis/module/AppConfigDialogBinder.java`
- Modify: `app/src/main/java/com/dpis/module/AppConfigSaveHandler.java`
- Modify: `app/src/main/java/com/dpis/module/AppListItem.java`
- Modify: app list construction in `MainActivity.java` or `AppLoadCoordinator.java`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-zh-rCN/strings.xml`
- Test: `app/src/test/java/com/dpis/module/AppConfigDialogBinderSourceSmokeTest.java`

- [ ] **Step 1: Add failing source smoke**

Add to `AppConfigDialogBinderSourceSmokeTest.java`:

```java
@Test
public void dialogBindsTypefaceSelectorSeparatelyFromFontScaleMode() throws Exception {
    String layout = read("src/main/res/layout/dialog_app_config.xml");
    String binder = read("src/main/java/com/dpis/module/AppConfigDialogBinder.java");
    String saveHandler = read("src/main/java/com/dpis/module/AppConfigSaveHandler.java");

    assertTrue(layout.contains("dialog_typeface_selector_button"));
    assertTrue(layout.contains("@string/dialog_typeface_default"));
    assertTrue(binder.contains("bindTypefaceSelector"));
    assertTrue(saveHandler.contains("setTargetTypefaceId"));
    assertTrue(saveHandler.contains("clearTargetTypefaceId"));
}
```

- [ ] **Step 2: Run test and verify failure**

Run:

```powershell
./gradlew :app:testModern101DebugUnitTest --tests com.dpis.module.AppConfigDialogBinderSourceSmokeTest
```

Expected: new smoke test fails.

- [ ] **Step 3: Add strings**

Add to `strings.xml`:

```xml
<string name="dialog_typeface_label">Font file</string>
<string name="dialog_typeface_default">System default</string>
<string name="dialog_typeface_missing">Selected font is unavailable</string>
```

Add equivalent Chinese strings.

- [ ] **Step 4: Add selector row to app config layout**

Insert below the existing font-scale row in `dialog_app_config.xml`:

```xml
<LinearLayout
    android:id="@+id/dialog_typeface_row"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_marginTop="8dp"
    android:gravity="center_vertical"
    android:orientation="horizontal">

    <com.google.android.material.textview.MaterialTextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="@string/dialog_typeface_label"
        android:textAppearance="@style/TextAppearance.Material3.BodyMedium" />

    <com.google.android.material.button.MaterialButton
        android:id="@+id/dialog_typeface_selector_button"
        style="@style/Widget.Dpis.DialogActionButton.Outlined"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:layout_marginStart="12dp"
        android:layout_weight="1"
        android:minWidth="0dp"
        android:text="@string/dialog_typeface_default" />
</LinearLayout>
```

- [ ] **Step 5: Extend item and load path**

Add `final String typefaceId;` to `AppListItem`, update its constructor, and set `this.typefaceId = typefaceId;`.

In the code that constructs `AppListItem`, pass `store.getTargetTypefaceId(packageName)` or the corresponding snapshot value.

- [ ] **Step 6: Bind selector**

In `AppConfigDialogBinder`, add a selected typeface ID to the dialog state or a local holder:

```java
final class TypefaceSelection {
    String selectedTypefaceId;
}
```

Bind button text:

```java
private void bindTypefaceSelector(MaterialButton button,
                                  AppListItem item,
                                  FontLibraryStore fontLibraryStore,
                                  TypefaceSelection selection) {
    selection.selectedTypefaceId = item.typefaceId;
    updateTypefaceButton(button, fontLibraryStore, selection.selectedTypefaceId);
    button.setOnClickListener(v -> showTypefaceSelectionDialog(button, fontLibraryStore, selection));
}
```

Selection dialog should list:

- System default, represented by `null`.
- Each `FontLibraryEntry.displayName`, represented by `entry.id`.

Keep this UI compact; do not mix it with font-size mode toggles.

- [ ] **Step 7: Save typeface selection**

Extend the save handler call to pass `selectedTypefaceId`. In `AppConfigSaveHandler`, after font-scale save logic:

```java
if (selectedTypefaceId == null || selectedTypefaceId.isBlank()) {
    saved = store.clearTargetTypefaceId(item.packageName) && saved;
} else {
    saved = store.setTargetTypefaceId(item.packageName, selectedTypefaceId) && saved;
}
```

This must not call `setTargetFontApplyMode` and must not change font-size mode.

- [ ] **Step 8: Run targeted tests**

Run:

```powershell
./gradlew :app:testModern101DebugUnitTest --tests com.dpis.module.AppConfigDialogBinderSourceSmokeTest
```

Expected: tests pass.

- [ ] **Step 9: Commit**

```powershell
git add app/src/main/res/layout/dialog_app_config.xml app/src/main/java/com/dpis/module/AppConfigDialogBinder.java app/src/main/java/com/dpis/module/AppConfigSaveHandler.java app/src/main/java/com/dpis/module/AppListItem.java app/src/main/java/com/dpis/module/MainActivity.java app/src/main/java/com/dpis/module/AppLoadCoordinator.java app/src/main/res/values/strings.xml app/src/main/res/values-zh-rCN/strings.xml app/src/test/java/com/dpis/module/AppConfigDialogBinderSourceSmokeTest.java
git commit -m "feat: add per-app font file selector"
```

---

### Task 7: Status, Filtering, and Backup Consistency

**Files:**
- Modify: `app/src/main/java/com/dpis/module/AppListFilter.java`
- Modify: `app/src/main/java/com/dpis/module/AppStatusFormatter.java`
- Modify: `app/src/main/java/com/dpis/module/PerAppDisplayConfigSource.java`
- Modify: `app/src/main/java/com/dpis/module/PerAppDisplayConfig.java` only if system_server config needs to carry typeface as inactive metadata.
- Test: `app/src/test/java/com/dpis/module/AppListFilterTest.java`
- Test: `app/src/test/java/com/dpis/module/AppStatusFormatterTest.java`
- Test: `app/src/test/java/com/dpis/module/ConfigBackupCodecSourceSmokeTest.java`

- [ ] **Step 1: Add failing filter/status tests**

Add to `AppListFilterTest.java`:

```java
@Test
public void fontConfiguredFilterIncludesTypefaceOnlyApps() {
    AppListFilterState state = new AppListFilterState("", false, true);

    assertTrue(AppListFilter.matches(
            state,
            "Example",
            "com.example.app",
            false,
            null,
            ViewportApplyMode.OFF,
            null,
            FontApplyMode.OFF,
            "font_abcd1234"));
}
```

Add to `AppStatusFormatterTest.java`:

```java
@Test
public void formatsTypefaceOnlyStatusAsFontFileConfigured() {
    String status = AppStatusFormatter.format(
            null,
            ViewportApplyMode.OFF,
            null,
            FontApplyMode.OFF,
            "font_abcd1234",
            true);

    assertTrue(status.contains("Font"));
}
```

Adjust signatures to match existing helpers exactly.

- [ ] **Step 2: Run tests and verify failure**

Run:

```powershell
./gradlew :app:testModern101DebugUnitTest --tests com.dpis.module.AppListFilterTest --tests com.dpis.module.AppStatusFormatterTest
```

Expected: compile or assertion failure until typeface parameters are supported.

- [ ] **Step 3: Update filtering**

Update `AppListFilter.matches(...)` to include `String typefaceId`.

The font-configured predicate should be:

```java
boolean typefaceConfigured = typefaceId != null && !typefaceId.isBlank();
boolean fontConfigured = (fontScalePercent != null
        && FontApplyMode.isEnabled(FontApplyMode.normalize(fontMode)))
        || typefaceConfigured;
```

- [ ] **Step 4: Update status formatting**

Add typeface-only status text without implying font-size mode:

```java
if (typefaceId != null && !typefaceId.isBlank()) {
    parts.add(context.getString(R.string.app_status_typeface_configured));
}
```

Add strings:

```xml
<string name="app_status_typeface_configured">Font file</string>
```

Add Chinese equivalent.

- [ ] **Step 5: Confirm backup behavior**

Because `ConfigBackupCodec` encodes all primitive SharedPreferences entries, `font.<pkg>.typeface_id` is automatically included as a string. Add or update source smoke:

```java
@Test
public void backupCodecCanCarryTypefaceIdStringEntry() throws Exception {
    Map<String, Object> entries = new LinkedHashMap<>();
    entries.put("font.com.example.app.typeface_id", "font_abcd1234");

    String encoded = ConfigBackupCodec.encode(entries);
    Map<String, Object> decoded = ConfigBackupCodec.decode(encoded);

    assertEquals("font_abcd1234", decoded.get("font.com.example.app.typeface_id"));
}
```

- [ ] **Step 6: Run targeted tests**

Run:

```powershell
./gradlew :app:testModern101DebugUnitTest --tests com.dpis.module.AppListFilterTest --tests com.dpis.module.AppStatusFormatterTest --tests com.dpis.module.ConfigBackupCodecSourceSmokeTest
```

Expected: tests pass.

- [ ] **Step 7: Commit**

```powershell
git add app/src/main/java/com/dpis/module/AppListFilter.java app/src/main/java/com/dpis/module/AppStatusFormatter.java app/src/main/res/values/strings.xml app/src/main/res/values-zh-rCN/strings.xml app/src/test/java/com/dpis/module/AppListFilterTest.java app/src/test/java/com/dpis/module/AppStatusFormatterTest.java app/src/test/java/com/dpis/module/ConfigBackupCodecSourceSmokeTest.java
git commit -m "feat: surface font file configuration state"
```

---

### Task 8: Final Verification and Device Smoke Checklist

**Files:**
- Create: `docs/font-file-replacement-validation.md`
- Run tests/builds

- [ ] **Step 1: Run full unit tests**

Run:

```powershell
./gradlew :app:testAllDebugUnitTests
```

Expected: all unit tests pass.

- [ ] **Step 2: Build debug APKs**

Run:

```powershell
./gradlew :app:assembleModern101Debug :app:assembleCompat100Debug
```

Expected: both debug APK tasks succeed.

- [ ] **Step 3: Add validation doc**

Create `docs/font-file-replacement-validation.md`:

```markdown
# Font File Replacement Validation

## Automated

- `./gradlew :app:testAllDebugUnitTests`
- `./gradlew :app:assembleModern101Debug :app:assembleCompat100Debug`

## Real Device

1. Install `app/build/outputs/apk/modern101/debug/app-modern101-debug.apk`.
2. Enable DPIS for a normal Java Android app in LSPosed.
3. Open DPIS settings, import a valid `.ttf` or `.otf`.
4. Open the target app config and select the imported font under Font file.
5. Leave DP width and font size empty.
6. Save and restart the target app.
7. Confirm ordinary `TextView` text changes font.
8. Check logcat for `DPIS_FONT_STYLE hook ready`.
9. Confirm no target app crash.
10. Clear the app font file selection, save, restart target app, and confirm system font behavior returns.

## Out of Scope

- HyperOS Gallery/Weather Flutter/native text is not expected to change in this MVP.
- Backup JSON does not include font binary files.
```

- [ ] **Step 4: Run source formatting checks through normal build**

Run:

```powershell
./gradlew :app:testAllDebugUnitTests
```

Expected: all tests still pass after docs-only addition.

- [ ] **Step 5: Commit**

```powershell
git add docs/font-file-replacement-validation.md
git commit -m "docs: add font file replacement validation"
```

## Self-Review Checklist

- Every spec requirement has a task:
  - global font library: Task 3 and Task 5
  - per-app selector: Task 6
  - independent replacement path: Task 2, Task 4, Task 6
  - Java `Typeface` hooks: Task 4
  - blocked delete while referenced: Task 3 and Task 5
  - no native/Flutter promise: Scope Guard and Task 8
  - no font binary backup: Task 7 and Task 8
- No task asks workers to alter `FontApplyMode` for typeface replacement.
- No task asks workers to route typeface replacement through `FontScaleOverride`.
- Compat100 legacy hooks remain unchanged for typeface-only config.
