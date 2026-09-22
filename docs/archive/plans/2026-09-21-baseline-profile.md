# Baseline Profile Ship And Measure Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (
> recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use
> checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ship locale-stable Baseline Profiles for the DPIS manager APK, measure cold start and Apps
list frames, and record the five workspace destinations.

**Architecture:** `WorkspaceDestination.testTag` plus `testTagsAsResourceId` make UiAutomator see
navigation without translated labels. `:baselineprofile` `src/main` owns journeys, startup-only DEX
layout, and Macrobenchmarks. `:app` depends on `profileinstaller` and commits modern/legacy release
profile files after a local device generate.

**Tech Stack:** androidx.baselineprofile 1.5.0, benchmark-macro-junit4, UiAutomator,
profileinstaller, JUnit4 source smoke, Gradle product flavors `modern` / `legacy`.

**Spec:** `docs/superpowers/specs/2026-09-21-baseline-profile-design.md`

---

## File map

- Create: `app/src/test/java/com/dpis/module/ui/presentation/BaselineProfileSourceSmokeTest.kt`
- Modify: `app/src/main/java/com/dpis/module/ui/presentation/workspace/WorkspaceShell.kt`
- Modify: `app/src/main/java/com/dpis/module/ui/presentation/design/ComposeDesignSystem.kt`
- Modify: `app/src/main/java/com/dpis/module/tools/presentation/LocalToolDialogs.kt`
- Modify:
  `app/src/test/java/com/dpis/module/runtime/presentation/RuntimeReloadNoticeSourceSmokeTest.kt`
- Modify: `gradle/libs.versions.toml`
- Modify: `app/build.gradle.kts`
- Modify: `baselineprofile/build.gradle.kts`
- Create: `baselineprofile/src/main/java/com/dpis/module/baselineprofile/DpisProfileJourneys.kt`
- Create:
  `baselineprofile/src/main/java/com/dpis/module/baselineprofile/DpisBaselineProfileGenerator.kt`
- Create: `baselineprofile/src/main/java/com/dpis/module/baselineprofile/DpisStartupBenchmarks.kt`
- Create:
  `baselineprofile/src/main/java/com/dpis/module/baselineprofile/DpisAppsScrollBenchmarks.kt`
- Delete:
  `baselineprofile/src/androidTest/java/com/dpis/module/baselineprofile/DpisBaselineProfileGenerator.kt`
- Modify: `CONTRIBUTING.md`
- Create after device capture: `app/src/modernRelease/generated/baselineProfiles/baseline-prof.txt`
- Create after device capture: `app/src/modernRelease/generated/baselineProfiles/startup-prof.txt`
- Create after device capture: `app/src/legacyRelease/generated/baselineProfiles/baseline-prof.txt`
- Create after device capture: `app/src/legacyRelease/generated/baselineProfiles/startup-prof.txt`

Do not change `app/src/testLegacy` or `app/src/androidTest` layout. Do not add connected tests to
`.github/workflows`.

---

### Task 1: Lock navigation and overlay contracts in source smoke

**Files:**

- Create: `app/src/test/java/com/dpis/module/ui/presentation/BaselineProfileSourceSmokeTest.kt`

- [ ] **Step 1: Write the failing smoke test**

```kotlin
package com.dpis.module.ui.presentation

import com.dpis.module.SourceSmokeTestPaths
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BaselineProfileSourceSmokeTest {
    @Test
    fun workspaceNavigationExposesLocaleIndependentTags() {
        val shell = readApp("src/main/java/com/dpis/module/ui/presentation/workspace/WorkspaceShell.kt")
        val design = readApp(
            "src/main/java/com/dpis/module/ui/presentation/design/ComposeDesignSystem.kt",
        )
        val notice = readApp(
            "src/main/java/com/dpis/module/tools/presentation/LocalToolDialogs.kt",
        )

        assertTrue(shell.contains("APP(R.string.workspace_app, R.drawable.ic_apps_24, \"workspace-nav-app\")"))
        assertTrue(shell.contains("TEMPLATE(R.string.workspace_template, R.drawable.ic_template_24, \"workspace-nav-template\")"))
        assertTrue(shell.contains("HOME(R.string.workspace_home, R.drawable.ic_home_24, \"workspace-nav-home\")"))
        assertTrue(shell.contains("TOOLS(R.string.workspace_tools, R.drawable.ic_build_24, \"workspace-nav-tools\")"))
        assertTrue(shell.contains("SETTINGS(R.string.workspace_settings, R.drawable.ic_settings_24, \"workspace-nav-settings\")"))
        assertTrue(shell.contains("val testTag: String"))
        assertTrue(shell.contains("Modifier.testTag(destination.testTag)"))
        assertTrue(shell.contains("testTag(\"workspace-nav-menu\")"))
        assertTrue(design.contains("testTagsAsResourceId = true"))
        assertTrue(notice.contains("testTag(\"runtime-reload-notice-ack\")"))
    }

    @Test
    fun generatorRecordsStableJourneysWithoutPixelTaps() {
        val generator = SourceSmokeTestPaths.readRepositoryRoot(
            "baselineprofile/src/main/java/com/dpis/module/baselineprofile/DpisBaselineProfileGenerator.kt",
        )
        val journeys = SourceSmokeTestPaths.readRepositoryRoot(
            "baselineprofile/src/main/java/com/dpis/module/baselineprofile/DpisProfileJourneys.kt",
        )
        val appGradle = SourceSmokeTestPaths.readRepositoryRoot("app/build.gradle.kts")
        val catalog = SourceSmokeTestPaths.readRepositoryRoot("gradle/libs.versions.toml")

        assertTrue(journeys.contains("By.res(tag)"))
        assertTrue(journeys.contains("startup-disclaimer-agreement"))
        assertTrue(journeys.contains("startup-disclaimer-accept"))
        assertTrue(journeys.contains("runtime-reload-notice-ack"))
        assertTrue(journeys.contains("workspace-nav-app"))
        assertTrue(generator.contains("includeInStartupProfile = true"))
        assertTrue(generator.contains("openWorkspace(TAG_NAV_APP)"))
        assertTrue(generator.contains("openWorkspace(TAG_NAV_TEMPLATE)"))
        assertTrue(generator.contains("openWorkspace(TAG_NAV_SETTINGS)"))
        assertTrue(generator.contains("openWorkspace(TAG_NAV_TOOLS)"))
        assertFalse(generator.contains("APPS_NAV_X"))
        assertFalse(generator.contains("io.github.kwensiu.dpis"))
        assertTrue(generator.indexOf("includeInStartupProfile = true") ==
            generator.lastIndexOf("includeInStartupProfile = true"))
        assertTrue(appGradle.contains("implementation(libs.androidx.profileinstaller)"))
        assertTrue(catalog.contains("androidx-profileinstaller"))
    }

    private fun readApp(relativePath: String): String = SourceSmokeTestPaths.read(relativePath)
}
```

- [ ] **Step 2: Run the smoke and confirm it fails**

Run:

```text
./gradlew :app:testModernDebugUnitTest --tests com.dpis.module.ui.presentation.BaselineProfileSourceSmokeTest
```

Expected: compile or assertion failure because tags, generator `src/main` files, and
`profileinstaller` are missing.

- [ ] **Step 3: Stop after red. Implementation starts in Task 2.**

---

### Task 2: Navigation tags, UiAutomator resource ids, reload-notice tag

**Files:**

- Modify: `app/src/main/java/com/dpis/module/ui/presentation/workspace/WorkspaceShell.kt`
- Modify: `app/src/main/java/com/dpis/module/ui/presentation/design/ComposeDesignSystem.kt`
- Modify: `app/src/main/java/com/dpis/module/tools/presentation/LocalToolDialogs.kt`
- Modify:
  `app/src/test/java/com/dpis/module/runtime/presentation/RuntimeReloadNoticeSourceSmokeTest.kt`

- [ ] **Step 1: Extend `WorkspaceDestination` and apply tags**

In `WorkspaceShell.kt` add:

```kotlin
import androidx.compose.ui.platform.testTag
```

Replace the enum with:

```kotlin
enum class WorkspaceDestination(
    @param:StringRes val labelRes: Int,
    @param:DrawableRes val iconRes: Int,
    val testTag: String,
) {
    APP(R.string.workspace_app, R.drawable.ic_apps_24, "workspace-nav-app"),
    TEMPLATE(R.string.workspace_template, R.drawable.ic_template_24, "workspace-nav-template"),
    HOME(R.string.workspace_home, R.drawable.ic_home_24, "workspace-nav-home"),
    TOOLS(R.string.workspace_tools, R.drawable.ic_build_24, "workspace-nav-tools"),
    SETTINGS(R.string.workspace_settings, R.drawable.ic_settings_24, "workspace-nav-settings")
}
```

On `NavigationBarItem`, `NavigationRailItem`, and `NavigationDrawerItem`, add
`modifier = Modifier.testTag(destination.testTag)` (drawer already has a
padding modifier: chain `.testTag(destination.testTag)` on that modifier).

On expanded Wear destination `WearButton`, chain
`.testTag(destination.testTag)` onto the existing modifier.

On the collapsed Wear `WearCompactButton`, chain
`.testTag("workspace-nav-menu")`.

- [ ] **Step 2: Expose Compose test tags to UiAutomator**

In `ComposeDesignSystem.kt` add:

```kotlin
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
```

Replace the `content = content` argument of `MaterialExpressiveTheme` with:

```kotlin
content = {
    Box(Modifier.semantics { testTagsAsResourceId = true }) {
        content()
    }
},
```

Do not add `fillMaxSize()` on that Box. Dialogs must keep wrap-content height.

- [ ] **Step 3: Tag the reload-notice button**

In `LocalToolDialogs.kt` add `import androidx.compose.ui.platform.testTag` and
change the acknowledge `Button` to:

```kotlin
Button(
    onClick = rememberClickAction(onAcknowledge),
    modifier = Modifier.fillMaxWidth().testTag("runtime-reload-notice-ack"),
) {
    Text(stringResource(R.string.module_runtime_reload_ack_button))
}
```

In `RuntimeReloadNoticeSourceSmokeTest.kt` add:

```kotlin
assertTrue(dialog.contains("testTag(\"runtime-reload-notice-ack\")"))
```

- [ ] **Step 4: Re-run the first smoke method only**

Run:

```text
./gradlew :app:testModernDebugUnitTest --tests com.dpis.module.ui.presentation.BaselineProfileSourceSmokeTest.workspaceNavigationExposesLocaleIndependentTags --tests com.dpis.module.runtime.presentation.RuntimeReloadNoticeSourceSmokeTest
```

Expected: `workspaceNavigationExposesLocaleIndependentTags` PASS.
`generatorRecordsStableJourneysWithoutPixelTaps` still FAIL.

- [ ] **Step 5: Commit**

```text
git add app/src/main/java/com/dpis/module/ui/presentation/workspace/WorkspaceShell.kt app/src/main/java/com/dpis/module/ui/presentation/design/ComposeDesignSystem.kt app/src/main/java/com/dpis/module/tools/presentation/LocalToolDialogs.kt app/src/test/java/com/dpis/module/ui/presentation/BaselineProfileSourceSmokeTest.kt app/src/test/java/com/dpis/module/runtime/presentation/RuntimeReloadNoticeSourceSmokeTest.kt
git commit -m "feat: expose locale-stable workspace tags for baseline profiles"
```

---

### Task 3: Profileinstaller and baselineprofile Gradle wiring

**Files:**

- Modify: `gradle/libs.versions.toml`
- Modify: `app/build.gradle.kts`
- Modify: `baselineprofile/build.gradle.kts`

- [ ] **Step 1: Catalog and app dependency**

In `gradle/libs.versions.toml` `[versions]` add:

```toml
profileinstaller = "1.4.1"
```

In `[libraries]` add:

```toml
androidx-profileinstaller = { group = "androidx.profileinstaller", name = "profileinstaller", version.ref = "profileinstaller" }
```

In `app/build.gradle.kts` `dependencies` next to `baselineProfile(project(":baselineprofile"))`:

```kotlin
implementation(libs.androidx.profileinstaller)
```

- [ ] **Step 2: Rewrite `baselineprofile/build.gradle.kts`**

```kotlin
plugins {
    alias(libs.plugins.agp.test)
    alias(libs.plugins.androidx.baselineprofile)
}

android {
    namespace = "com.dpis.module.baselineprofile"
    compileSdk = 37

    defaultConfig {
        minSdk = 28
        targetSdk = 36
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    targetProjectPath = ":app"

    flavorDimensions += "xposedApi"
    productFlavors {
        create("modern") {
            dimension = "xposedApi"
        }
        create("legacy") {
            dimension = "xposedApi"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

baselineProfile {
    useConnectedDevices = true
}

dependencies {
    implementation(libs.androidx.benchmark.macro.junit4)
    implementation(libs.androidx.test.ext.junit)
    implementation(libs.androidx.test.core)
    implementation(libs.androidx.test.runner)
    implementation(libs.androidx.uiautomator)
}

androidComponents {
    onVariants { variant ->
        val artifactsLoader = variant.artifacts.getBuiltArtifactsLoader()
        variant.instrumentationRunnerArguments.put(
            "targetAppId",
            variant.testedApks.map { artifactsLoader.load(it)?.applicationId },
        )
    }
}
```

AGP 9 built-in Kotlin rejects `org.jetbrains.kotlin.android` on this module.
Keep Java 17 `compileOptions` only, same as `:app`.

- [ ] **Step 3: Commit**

```text
git add gradle/libs.versions.toml app/build.gradle.kts baselineprofile/build.gradle.kts
git commit -m "build: wire profileinstaller and baselineprofile targetAppId"
```

---

### Task 4: Journeys, generator, and delete pixel taps

**Files:**

- Create: `baselineprofile/src/main/java/com/dpis/module/baselineprofile/DpisProfileJourneys.kt`
- Create:
  `baselineprofile/src/main/java/com/dpis/module/baselineprofile/DpisBaselineProfileGenerator.kt`
- Delete:
  `baselineprofile/src/androidTest/java/com/dpis/module/baselineprofile/DpisBaselineProfileGenerator.kt`

- [ ] **Step 1: Write `DpisProfileJourneys.kt`**

```kotlin
package com.dpis.module.baselineprofile

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until

internal object DpisProfileJourneys {
    const val TAG_NAV_APP = "workspace-nav-app"
    const val TAG_NAV_TEMPLATE = "workspace-nav-template"
    const val TAG_NAV_HOME = "workspace-nav-home"
    const val TAG_NAV_TOOLS = "workspace-nav-tools"
    const val TAG_NAV_SETTINGS = "workspace-nav-settings"
    const val TAG_DISCLAIMER_AGREEMENT = "startup-disclaimer-agreement"
    const val TAG_DISCLAIMER_ACCEPT = "startup-disclaimer-accept"
    const val TAG_RELOAD_ACK = "runtime-reload-notice-ack"

    private const val UI_TIMEOUT_MS = 8_000L
    private const val OVERLAY_TIMEOUT_MS = 2_000L

    fun targetPackageName(): String {
        return InstrumentationRegistry.getArguments().getString("targetAppId")
            ?: throw IllegalStateException("targetAppId not passed as instrumentation runner arg")
    }

    fun MacrobenchmarkScope.startManagerAndDismissOverlays() {
        pressHome()
        startActivityAndWait()
        dismissOverlays()
        device.waitForIdle()
    }

    fun MacrobenchmarkScope.dismissOverlays() {
        clickIfPresent(TAG_DISCLAIMER_AGREEMENT)
        clickIfPresent(TAG_DISCLAIMER_ACCEPT)
        clickIfPresent(TAG_RELOAD_ACK)
        device.waitForIdle()
    }

    fun MacrobenchmarkScope.openWorkspace(tag: String) {
        device.wait(Until.hasObject(By.res(tag)), UI_TIMEOUT_MS)
        val node = device.findObject(By.res(tag))
            ?: error("workspace destination not found: $tag")
        node.click()
        device.waitForIdle()
    }

    fun MacrobenchmarkScope.swipeContentUp() {
        val width = device.displayWidth
        val height = device.displayHeight
        val x = width / 2
        val startY = (height * 0.72).toInt()
        val endY = (height * 0.28).toInt()
        device.swipe(x, startY, x, endY, 18)
        device.waitForIdle()
    }

    private fun MacrobenchmarkScope.clickIfPresent(tag: String) {
        if (device.wait(Until.hasObject(By.res(tag)), OVERLAY_TIMEOUT_MS) == true) {
            device.findObject(By.res(tag))?.click()
            device.waitForIdle()
        }
    }
}
```

Relative display fractions for list swipe are allowed. Named pixel fields such
as `APPS_NAV_X` are not.

- [ ] **Step 2: Write `DpisBaselineProfileGenerator.kt`**

```kotlin
package com.dpis.module.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.dpis.module.baselineprofile.DpisProfileJourneys.TAG_NAV_APP
import com.dpis.module.baselineprofile.DpisProfileJourneys.TAG_NAV_SETTINGS
import com.dpis.module.baselineprofile.DpisProfileJourneys.TAG_NAV_TEMPLATE
import com.dpis.module.baselineprofile.DpisProfileJourneys.TAG_NAV_TOOLS
import com.dpis.module.baselineprofile.DpisProfileJourneys.openWorkspace
import com.dpis.module.baselineprofile.DpisProfileJourneys.startManagerAndDismissOverlays
import com.dpis.module.baselineprofile.DpisProfileJourneys.swipeContentUp
import com.dpis.module.baselineprofile.DpisProfileJourneys.targetPackageName
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class DpisBaselineProfileGenerator {
    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    @Test
    fun coldStart() = collect(includeInStartupProfile = true) {
        startManagerAndDismissOverlays()
    }

    @Test
    fun appsListScroll() = collect(includeInStartupProfile = false) {
        startManagerAndDismissOverlays()
        openWorkspace(TAG_NAV_APP)
        swipeContentUp()
    }

    @Test
    fun templatesListScroll() = collect(includeInStartupProfile = false) {
        startManagerAndDismissOverlays()
        openWorkspace(TAG_NAV_TEMPLATE)
        swipeContentUp()
    }

    @Test
    fun settingsScroll() = collect(includeInStartupProfile = false) {
        startManagerAndDismissOverlays()
        openWorkspace(TAG_NAV_SETTINGS)
        swipeContentUp()
    }

    @Test
    fun openTools() = collect(includeInStartupProfile = false) {
        startManagerAndDismissOverlays()
        openWorkspace(TAG_NAV_TOOLS)
    }

    private fun collect(
        includeInStartupProfile: Boolean,
        block: androidx.benchmark.macro.MacrobenchmarkScope.() -> Unit,
    ) {
        baselineProfileRule.collect(
            packageName = targetPackageName(),
            includeInStartupProfile = includeInStartupProfile,
        ) {
            block()
        }
    }
}
```

Match the current generator: `collect(packageName, includeInStartupProfile)` plus
a trailing lambda. If named-argument `block =` does not compile, use the
trailing-lambda form at each `@Test` instead of a helper.

- [ ] **Step 3: Delete the androidTest generator**

Delete
`baselineprofile/src/androidTest/java/com/dpis/module/baselineprofile/DpisBaselineProfileGenerator.kt`.
Remove empty directories left behind.

- [ ] **Step 4: Run JVM smoke**

```text
./gradlew :app:testModernDebugUnitTest --tests com.dpis.module.ui.presentation.BaselineProfileSourceSmokeTest
```

Expected: both methods PASS.

- [ ] **Step 5: Commit**

```text
git add baselineprofile app/src/test/java/com/dpis/module/ui/presentation/BaselineProfileSourceSmokeTest.kt
git commit -m "feat: record workspace journeys for baseline profiles"
```

---

### Task 5: Startup and Apps-scroll benchmarks

**Files:**

- Create: `baselineprofile/src/main/java/com/dpis/module/baselineprofile/DpisStartupBenchmarks.kt`
- Create:
  `baselineprofile/src/main/java/com/dpis/module/baselineprofile/DpisAppsScrollBenchmarks.kt`

- [ ] **Step 1: Write `DpisStartupBenchmarks.kt`**

```kotlin
package com.dpis.module.baselineprofile

import androidx.benchmark.macro.BaselineProfileMode
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.dpis.module.baselineprofile.DpisProfileJourneys.startManagerAndDismissOverlays
import com.dpis.module.baselineprofile.DpisProfileJourneys.targetPackageName
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class DpisStartupBenchmarks {
    @get:Rule
    val rule = MacrobenchmarkRule()

    @Test
    fun startupCompilationNone() = benchmark(CompilationMode.None())

    @Test
    fun startupCompilationBaselineProfiles() =
        benchmark(CompilationMode.Partial(BaselineProfileMode.Require))

    private fun benchmark(compilationMode: CompilationMode) {
        rule.measureRepeated(
            packageName = targetPackageName(),
            metrics = listOf(StartupTimingMetric()),
            compilationMode = compilationMode,
            startupMode = StartupMode.COLD,
            iterations = 10,
            setupBlock = { pressHome() },
            measureBlock = { startManagerAndDismissOverlays() },
        )
    }
}
```

- [ ] **Step 2: Write `DpisAppsScrollBenchmarks.kt`**

```kotlin
package com.dpis.module.baselineprofile

import androidx.benchmark.macro.BaselineProfileMode
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.dpis.module.baselineprofile.DpisProfileJourneys.TAG_NAV_APP
import com.dpis.module.baselineprofile.DpisProfileJourneys.openWorkspace
import com.dpis.module.baselineprofile.DpisProfileJourneys.startManagerAndDismissOverlays
import com.dpis.module.baselineprofile.DpisProfileJourneys.swipeContentUp
import com.dpis.module.baselineprofile.DpisProfileJourneys.targetPackageName
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class DpisAppsScrollBenchmarks {
    @get:Rule
    val rule = MacrobenchmarkRule()

    @Test
    fun appsScrollWithBaselineProfiles() {
        rule.measureRepeated(
            packageName = targetPackageName(),
            metrics = listOf(FrameTimingMetric()),
            compilationMode = CompilationMode.Partial(BaselineProfileMode.Require),
            startupMode = StartupMode.COLD,
            iterations = 10,
            setupBlock = { pressHome() },
            measureBlock = {
                startManagerAndDismissOverlays()
                openWorkspace(TAG_NAV_APP)
                swipeContentUp()
            },
        )
    }
}
```

These classes are not JVM tests. Do not add them to `:app:testAllDebugUnitTests`.
Do not add them to CI.

- [ ] **Step 3: Commit**

```text
git add baselineprofile/src/main/java/com/dpis/module/baselineprofile/DpisStartupBenchmarks.kt baselineprofile/src/main/java/com/dpis/module/baselineprofile/DpisAppsScrollBenchmarks.kt
git commit -m "test: measure cold start and apps scroll with baseline profiles"
```

---

### Task 6: Contributing commands

**Files:**

- Modify: `CONTRIBUTING.md`

- [ ] **Step 1: Add a Baseline Profile section after the existing Gradle commands**

Insert after the `testModernDebugUnitTest --tests` example. CONTRIBUTING already
uses ` ```bash ` fences, so this new section must use a longer outer fence when
shown in the plan. In the real file, use normal triple-backtick bash fences
inside the Markdown headings, same as the existing Gradle examples.

Text to insert:

~~~~markdown
### Baseline Profiles

Baseline Profiles optimize the DPIS manager UI process only. They do not
speed up hooked target apps. GitHub Actions does not generate them.

After changing cold-start or workspace UI, connect an API 33+ (or rooted
API 28+) device with default workspace visibility (all five destinations
shown) and run:

```bash
./gradlew :app:generateModernReleaseBaselineProfile
./gradlew :app:generateLegacyReleaseBaselineProfile
```

Commit the generated files:

- `app/src/modernRelease/generated/baselineProfiles/`
- `app/src/legacyRelease/generated/baselineProfiles/`

To measure on the same device:

```bash
./gradlew :baselineprofile:connectedModernReleaseAndroidTest
```

A release APK without those generated files does not include this
optimization.
~~~~

Keep English/Chinese style consistent with the file: bilingual short intro is
fine; commands stay in one language.

- [ ] **Step 2: Commit**

```text
git add CONTRIBUTING.md
git commit -m "docs: document baseline profile generation"
```

---

### Task 7: Host verification without a device

**Files:** none beyond what previous tasks changed.

- [ ] **Step 1: Full debug unit tests**

```text
./gradlew :app:testAllDebugUnitTests
```

Expected: PASS, including `BaselineProfileSourceSmokeTest`.

- [ ] **Step 2: Flavor debug APKs**

```text
./gradlew :app:assembleModernDebug :app:assembleLegacyDebug
```

Expected: both assemble.

- [ ] **Step 3: Confirm androidTest and testLegacy were not restructured**

`app/src/androidTest/java` and `app/src/testLegacy/java` still exist with the
same roles. No workflow file under `.github/workflows` gained
`connectedAndroidTest` or `generate*BaselineProfile`.

---

### Task 8: Device capture (required for shipping, needs hardware)

**Files:**

- Create: `app/src/modernRelease/generated/baselineProfiles/baseline-prof.txt`
- Create: `app/src/modernRelease/generated/baselineProfiles/startup-prof.txt`
- Create: `app/src/legacyRelease/generated/baselineProfiles/baseline-prof.txt`
- Create: `app/src/legacyRelease/generated/baselineProfiles/startup-prof.txt`

- [ ] **Step 1: Connect an API 33+ physical device. Do not use an emulator as evidence.**

- [ ] **Step 2: Generate**

```text
./gradlew :app:generateModernReleaseBaselineProfile
./gradlew :app:generateLegacyReleaseBaselineProfile
```

Expected: both tasks succeed; the four files above exist and are non-empty.

- [ ] **Step 3: Spot-check**

`startup-prof.txt` must be smaller than `baseline-prof.txt` for the same
flavor. `baseline-prof.txt` must mention manager types (for example
`MainActivity` or workspace composables). It must not be a copy of a target
app's package.

- [ ] **Step 4: Optional measurement on the same device**

```text
./gradlew :baselineprofile:connectedModernReleaseAndroidTest
```

Record that `startupCompilationBaselineProfiles` is faster than
`startupCompilationNone`. Frame metrics are informational.

- [ ] **Step 5: Commit generated profiles**

```text
git add app/src/modernRelease/generated/baselineProfiles app/src/legacyRelease/generated/baselineProfiles
git commit -m "chore: commit generated modern and legacy baseline profiles"
```

If no device is available, stop after Task 7 and leave this task open. Do not
fabricate profile files.

---

## Spec coverage

| Spec section                           | Task |
|----------------------------------------|------|
| Locale-stable navigation tags          | 1, 2 |
| `testTagsAsResourceId`                 | 2    |
| Reload-notice tag                      | 2    |
| Generator in `src/main`, `targetAppId` | 3, 4 |
| Five journeys, startup-only DEX layout | 4    |
| No pixel taps                          | 4    |
| Startup + Apps frame benchmarks        | 5    |
| `profileinstaller`                     | 3    |
| CONTRIBUTING generate commands         | 6    |
| JVM smoke, no CI devices               | 1, 7 |
| Commit generated profiles              | 8    |
| Do not restructure test directories    | 7    |

## Execution notes

Task 8 is hardware-gated. Tasks 1–7 are the software change. Do not mark the
feature shipped until Task 8 lands or the user explicitly accepts a generator-
only PR.
