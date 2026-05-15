# Companion Display Tool Compose Phase 2 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add the first Compose scene group to the companion display tool while preserving the phase 1 native scene behavior, package name, broadcast contract, and log prefix.

**Architecture:** Keep the existing Java-first companion app structure. Add the minimum Kotlin/Compose support needed for Compose scene hosting, and route Compose scenes through the existing scene registry, run orchestrator, adb control flow, and `DPIS_TEST` log formatter.

**Tech Stack:** Android app module `:companion-display-tool`, Java 17, Kotlin Android plugin for Compose-only files, Jetpack Compose UI/Foundation, JUnit4, existing adb broadcast control surface.

---

## Pre-Execution Workspace Requirement

Do not implement this plan in the current repository checkout.

Create a separate worktree under the project directory before implementation. The current `app/displaytool` branch contains a temporary local commit:

```text
e4e6f4aaa954c9e241801e488e511925e49ea7ca
```

Implementation work must happen in a separate worktree based on `app/displaytool`, and that temporary commit must be removed in the implementation worktree before code changes begin.

Recommended setup from the repository root:

```powershell
git worktree add -b app/displaytool-compose-phase2 .worktrees\displaytool-compose-phase2 app/displaytool
Set-Location .worktrees\displaytool-compose-phase2
git reset --soft origin/app/displaytool
```

After the reset, keep the Compose phase 2 design document changes as uncommitted work if they are still needed in the branch, then commit them with the implementation or as a separate docs commit according to the branch owner's preference.

Verify before implementation:

```powershell
git status --short --branch
git log --oneline -3
```

Expected:
- branch is the new worktree's `app/displaytool-compose-phase2` checkout
- `HEAD` is `origin/app/displaytool` after the soft reset
- the temporary commit is no longer in the branch history

## Spec References

Implementation must follow:
- `docs/2026-05-13-companion-display-tool-design.md`
- `docs/companion-display-tool.md`
- `docs/2026-05-13-companion-display-tool-compose-phase2-design.md`

Key requirements:
- package stays `io.github.kwensiu.dpis.displaytool`
- plain `action=run_all` remains phase 1 native-only
- normal app launch runs phase 1 native cold start, then the Compose cold-start subset
- `action=run_all group=compose` runs only:
  - `compose_baseline_text normal`
  - `compose_lazy_list_text normal`
- all four Compose scenes support `run_scene` with `variant=normal`
- Compose scenes reject `variant=fragile`
- Compose logs reuse the phase 1 scene-event prefix exactly
- Compose-specific fields are appended after the shared prefix
- WebView remains unimplemented

## File Structure

Modify:
- `gradle/libs.versions.toml` - add Compose versions and libraries.
- `companion-display-tool/build.gradle.kts` - apply Kotlin Android plugin and enable Compose for this module only.
- `companion-display-tool/src/main/java/com/dpis/displaytool/CompanionContract.java` - add group extra and Compose group constant.
- `companion-display-tool/src/main/java/com/dpis/displaytool/CompanionLog.java` - add Compose event formatting while preserving native formatting.
- `companion-display-tool/src/main/java/com/dpis/displaytool/RunOrchestrator.java` - add grouped run support and Compose presentation logging path.
- `companion-display-tool/src/main/java/com/dpis/displaytool/MainActivity.java` - run Compose cold-start subset after native cold start and handle `group=compose`.
- `companion-display-tool/src/main/java/com/dpis/displaytool/ControlReceiver.java` - forward the group extra.
- `companion-display-tool/src/main/java/com/dpis/displaytool/scene/DisplayScene.java` - extend the interface only if required for group metadata.
- `companion-display-tool/src/main/java/com/dpis/displaytool/scene/ScenePresentation.java` - extend presentation only if required for Compose logging callback.
- `companion-display-tool/src/main/java/com/dpis/displaytool/scene/SceneRegistry.java` - add Compose scene registration and explicit scene lists.
- `companion-display-tool/src/main/res/values/strings.xml` - add simple Native and Compose group labels when grouping is shown in the UI.
- `companion-display-tool/src/test/java/com/dpis/displaytool/CompanionLogTest.java` - lock Compose log field order.
- `companion-display-tool/src/test/java/com/dpis/displaytool/scene/SceneRegistryTest.java` - lock Compose scene order and normal-only variants.
- `docs/companion-display-tool.md` - document Compose phase 2 commands and logs.

Create:
- `companion-display-tool/src/main/java/com/dpis/displaytool/ComposeRunFields.java` - immutable Java value object for Compose log fields.
- `companion-display-tool/src/main/java/com/dpis/displaytool/scene/SceneGroups.java` - group constants if this keeps registry code cleaner.
- `companion-display-tool/src/main/java/com/dpis/displaytool/scene/ComposeTextSceneSupport.java` or `.kt` equivalent - shared Compose scene host helpers.
- `companion-display-tool/src/main/java/com/dpis/displaytool/scene/ComposeBaselineTextScene.kt`
- `companion-display-tool/src/main/java/com/dpis/displaytool/scene/ComposeNestedScrollTextScene.kt`
- `companion-display-tool/src/main/java/com/dpis/displaytool/scene/ComposeLazyListTextScene.kt`
- `companion-display-tool/src/main/java/com/dpis/displaytool/scene/ComposeStyledTextScene.kt`
- `companion-display-tool/src/test/java/com/dpis/displaytool/ComposeContractSourceTest.java` - source/build-level checks for no fragile Compose support and no WebView dependency.

Avoid:
- moving existing phase 1 scene ids
- changing `CompanionLog.formatSceneEvent(...)` output for native scenes
- changing `applicationId`
- adding Material Compose unless plain Compose UI/Foundation cannot satisfy the scenes
- adding WebView dependencies

---

### Task 1: Add Compose Build Support To The Companion Module

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `companion-display-tool/build.gradle.kts`

- [ ] **Step 1: Add Compose version catalog entries**

Edit `gradle/libs.versions.toml`:

```toml
[versions]
composeBom = "2026.04.01"

[plugins]
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }

[libraries]
androidx-compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "composeBom" }
androidx-compose-ui = { group = "androidx.compose.ui", name = "ui" }
androidx-compose-foundation = { group = "androidx.compose.foundation", name = "foundation" }
androidx-compose-runtime = { group = "androidx.compose.runtime", name = "runtime" }
```

Use `2026.04.01` for the Compose BOM. Do not add Compose Material.

- [ ] **Step 2: Enable Kotlin and Compose only for companion module**

Edit `companion-display-tool/build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.agp.app)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.dpis.displaytool"
    compileSdk = 36

    defaultConfig {
        applicationId = "io.github.kwensiu.dpis.displaytool"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.recyclerview)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.runtime)
    testImplementation(libs.junit4)
}
```

Keep `applicationId` unchanged and do not add `applicationIdSuffix`.

- [ ] **Step 3: Verify Gradle configuration**

Run:

```powershell
.\gradlew.bat :companion-display-tool:tasks --all
```

Expected:
- Gradle configures successfully.
- No changes to `:app` are required.

---

### Task 2: Lock The Compose Scene Registry Contract With Tests

**Files:**
- Modify: `companion-display-tool/src/main/java/com/dpis/displaytool/CompanionContract.java`
- Modify: `companion-display-tool/src/main/java/com/dpis/displaytool/scene/SceneRegistry.java`
- Test: `companion-display-tool/src/test/java/com/dpis/displaytool/scene/SceneRegistryTest.java`

- [ ] **Step 1: Write failing tests for Compose scenes and variants**

Append tests to `SceneRegistryTest.java`:

```java
@Test
public void composeScenesStayInPhase2Order() {
    SceneRegistry registry = SceneRegistry.create();

    List<String> ids = Arrays.asList(
            registry.composeColdStartScenes().get(0).id(),
            registry.composeColdStartScenes().get(1).id()
    );

    assertEquals(Arrays.asList(
            "compose_baseline_text",
            "compose_lazy_list_text"
    ), ids);

    List<String> allComposeIds = Arrays.asList(
            registry.composeScenes().get(0).id(),
            registry.composeScenes().get(1).id(),
            registry.composeScenes().get(2).id(),
            registry.composeScenes().get(3).id()
    );

    assertEquals(Arrays.asList(
            "compose_baseline_text",
            "compose_nested_scroll_text",
            "compose_lazy_list_text",
            "compose_styled_text"
    ), allComposeIds);
}

@Test
public void composeScenesSupportOnlyNormalVariant() {
    SceneRegistry registry = SceneRegistry.create();

    assertSupportsOnlyNormal(registry, "compose_baseline_text");
    assertSupportsOnlyNormal(registry, "compose_nested_scroll_text");
    assertSupportsOnlyNormal(registry, "compose_lazy_list_text");
    assertSupportsOnlyNormal(registry, "compose_styled_text");
}
```

Update existing tests from `SceneRegistry.createPhase1()` to `SceneRegistry.create()` only after adding that method in the implementation step.

- [ ] **Step 2: Run tests and verify failure**

Run:

```powershell
.\gradlew.bat :companion-display-tool:testDebugUnitTest --tests com.dpis.displaytool.scene.SceneRegistryTest
```

Expected:
- FAIL because `SceneRegistry.create()`, `composeScenes()`, and `composeColdStartScenes()` do not exist yet.

- [ ] **Step 3: Add group constants**

Update `CompanionContract.java`:

```java
public static final String EXTRA_GROUP = "group";
public static final String GROUP_NATIVE = "native";
public static final String GROUP_COMPOSE = "compose";
```

- [ ] **Step 4: Update registry API**

Update `SceneRegistry.java` so it preserves phase 1 behavior and adds Compose lists:

```java
private final List<DisplayScene> coreScenes;
private final List<DisplayScene> composeScenes;
private final List<DisplayScene> composeColdStartScenes;

private SceneRegistry(
        List<DisplayScene> coreScenes,
        List<DisplayScene> composeScenes,
        List<DisplayScene> composeColdStartScenes
) {
    this.coreScenes = Collections.unmodifiableList(new ArrayList<>(coreScenes));
    this.composeScenes = Collections.unmodifiableList(new ArrayList<>(composeScenes));
    this.composeColdStartScenes = Collections.unmodifiableList(new ArrayList<>(composeColdStartScenes));
}

public static SceneRegistry create() {
    List<DisplayScene> coreScenes = createPhase1Scenes();
    List<DisplayScene> composeScenes = createComposeScenes();
    List<DisplayScene> composeColdStartScenes = Arrays.asList(
            composeScenes.get(0),
            composeScenes.get(2)
    );
    return new SceneRegistry(coreScenes, composeScenes, composeColdStartScenes);
}

public static SceneRegistry createPhase1() {
    return new SceneRegistry(createPhase1Scenes(), Collections.emptyList(), Collections.emptyList());
}
```

Add helper methods using the existing phase 1 scenes and the Compose scene classes from Task 4. If executing tasks strictly in order, defer the final registry wiring assertions until Task 4 creates the Compose scene classes; do not commit placeholder scene implementations.

Update `findById` to search both native and Compose lists.

- [ ] **Step 5: Run registry tests**

Run:

```powershell
.\gradlew.bat :companion-display-tool:testDebugUnitTest --tests com.dpis.displaytool.scene.SceneRegistryTest
```

Expected:
- PASS after Compose scene classes exist.
- If this task is implemented before Task 4, keep the failing test committed only after Task 4 completes. Do not leave uncompilable stubs in final code.

---

### Task 3: Add Compose Log Formatting Contract

**Files:**
- Create: `companion-display-tool/src/main/java/com/dpis/displaytool/ComposeRunFields.java`
- Modify: `companion-display-tool/src/main/java/com/dpis/displaytool/CompanionLog.java`
- Test: `companion-display-tool/src/test/java/com/dpis/displaytool/CompanionLogTest.java`

- [ ] **Step 1: Write failing test for Compose log field order**

Add to `CompanionLogTest.java`:

```java
@Test
public void composeSceneEventKeepsPrefixBeforeComposeFields() {
    String line = CompanionLog.formatComposeSceneEvent(new CompanionLog.SceneEventFields(
            "123_2",
            "compose_baseline_text",
            "normal",
            "compose_first_text_layout",
            "io.github.kwensiu.dpis.displaytool",
            1.0f,
            346,
            2.1625f,
            360,
            792,
            2.1625f,
            1080,
            2376,
            499.4f,
            1098.7f,
            "compose_text_primary",
            30.3f,
            14f,
            30.3f,
            1.0f,
            1,
            240,
            48,
            SceneAnomaly.NONE
    ), new ComposeRunFields(
            2.1625f,
            1.0f,
            14f,
            30.3f,
            1,
            240,
            48,
            1.0f,
            -1,
            -1,
            "baseline",
            "none"
    ));

    assertTrue(line.startsWith(
            "stage=phase1 run_id=123_2 scene=compose_baseline_text variant=normal "
                    + "event=compose_first_text_layout pkg=io.github.kwensiu.dpis.displaytool "
                    + "font_scale=1.00 density_dpi=346 scaled_density=2.16 "
                    + "width_dp=360 height_dp=792 "
    ));
    assertTrue(line.contains("surface=compose "));
    assertTrue(line.contains("compose_density=2.16 "));
    assertTrue(line.contains("compose_text_sp=14.0 "));
    assertTrue(line.contains("compose_rendered_scale=1.00 "));
    assertFalse(line.contains("item_index=-1 "));
    assertFalse(line.contains("lazy_first_visible_index=-1 "));
    assertAscii(line);
}
```

- [ ] **Step 2: Run test and verify failure**

Run:

```powershell
.\gradlew.bat :companion-display-tool:testDebugUnitTest --tests com.dpis.displaytool.CompanionLogTest
```

Expected:
- FAIL because `ComposeRunFields` and `formatComposeSceneEvent` do not exist.

- [ ] **Step 3: Add ComposeRunFields**

Create `ComposeRunFields.java`:

```java
package com.dpis.displaytool;

final class ComposeRunFields {
    final float composeDensity;
    final float composeFontScale;
    final float composeTextSp;
    final float composeTextPx;
    final int composeLineCount;
    final int composeLayoutW;
    final int composeLayoutH;
    final float composeRenderedScale;
    final int itemIndex;
    final int lazyFirstVisibleIndex;
    final String styleSource;
    final String container;

    ComposeRunFields(
            float composeDensity,
            float composeFontScale,
            float composeTextSp,
            float composeTextPx,
            int composeLineCount,
            int composeLayoutW,
            int composeLayoutH,
            float composeRenderedScale,
            int itemIndex,
            int lazyFirstVisibleIndex,
            String styleSource,
            String container
    ) {
        this.composeDensity = composeDensity;
        this.composeFontScale = composeFontScale;
        this.composeTextSp = composeTextSp;
        this.composeTextPx = composeTextPx;
        this.composeLineCount = composeLineCount;
        this.composeLayoutW = composeLayoutW;
        this.composeLayoutH = composeLayoutH;
        this.composeRenderedScale = composeRenderedScale;
        this.itemIndex = itemIndex;
        this.lazyFirstVisibleIndex = lazyFirstVisibleIndex;
        this.styleSource = styleSource;
        this.container = container;
    }
}
```

- [ ] **Step 4: Add Compose log formatter**

Add to `CompanionLog.java`:

```java
static String formatComposeSceneEvent(SceneEventFields fields, ComposeRunFields composeFields) {
    String line = formatSceneEvent(fields)
            + field("surface", CompanionContract.GROUP_COMPOSE)
            + field("compose_density", two(composeFields.composeDensity))
            + field("compose_font_scale", two(composeFields.composeFontScale))
            + field("compose_text_sp", one(composeFields.composeTextSp))
            + field("compose_text_px", one(composeFields.composeTextPx))
            + field("compose_line_count", composeFields.composeLineCount)
            + field("compose_layout_w", composeFields.composeLayoutW)
            + field("compose_layout_h", composeFields.composeLayoutH)
            + field("compose_rendered_scale", two(composeFields.composeRenderedScale));
    if (composeFields.itemIndex >= 0) {
        line += field("item_index", composeFields.itemIndex);
    }
    if (composeFields.lazyFirstVisibleIndex >= 0) {
        line += field("lazy_first_visible_index", composeFields.lazyFirstVisibleIndex);
    }
    line += optionalField("style_source", composeFields.styleSource);
    line += optionalField("container", composeFields.container);
    return line;
}
```

If `two`, `one`, or `optionalField` are private, keep this method inside `CompanionLog` so it can use them. Do not change native `formatSceneEvent(...)` output.

- [ ] **Step 5: Run log tests**

Run:

```powershell
.\gradlew.bat :companion-display-tool:testDebugUnitTest --tests com.dpis.displaytool.CompanionLogTest
```

Expected:
- PASS.

---

### Task 4: Implement Compose Scene Hosting And Four Normal Scenes

**Files:**
- Create: `companion-display-tool/src/main/java/com/dpis/displaytool/scene/ComposeTextSceneSupport.kt`
- Create: `companion-display-tool/src/main/java/com/dpis/displaytool/scene/ComposeBaselineTextScene.kt`
- Create: `companion-display-tool/src/main/java/com/dpis/displaytool/scene/ComposeNestedScrollTextScene.kt`
- Create: `companion-display-tool/src/main/java/com/dpis/displaytool/scene/ComposeLazyListTextScene.kt`
- Create: `companion-display-tool/src/main/java/com/dpis/displaytool/scene/ComposeStyledTextScene.kt`
- Modify: `companion-display-tool/src/main/java/com/dpis/displaytool/scene/ScenePresentation.java`
- Modify: `companion-display-tool/src/main/java/com/dpis/displaytool/scene/SceneRegistry.java`

- [ ] **Step 1: Extend ScenePresentation for Compose logging**

Add an optional compose field callback without changing existing native callers:

```java
public interface ComposeFieldsProvider {
    boolean isReady();

    ComposeRunFields fields(float androidScaledDensity);
}
```

If package visibility makes `ComposeRunFields` inaccessible, make `ComposeRunFields` public or move the provider interface to `com.dpis.displaytool`.

Add a nullable provider field and getter to `ScenePresentation`, plus a factory:

```java
public static ScenePresentation composeView(
        View view,
        float baseSp,
        String viewName,
        String event,
        ComposeFieldsProvider composeFieldsProvider
) {
    return new ScenePresentation(
            Kind.VIEW,
            view,
            null,
            null,
            baseSp,
            viewName,
            event,
            composeFieldsProvider
    );
}
```

Keep existing `view(...)` and `dialog(...)` factories source-compatible.

- [ ] **Step 2: Add Compose shared support**

Create `ComposeTextSceneSupport.kt` with:

```kotlin
package com.dpis.displaytool.scene

import android.view.Choreographer
import androidx.compose.ui.platform.ComposeView
import com.dpis.displaytool.CompanionContract

internal const val COMPOSE_BASE_SP = 14f
internal const val EVENT_COMPOSE_FIRST_TEXT_LAYOUT = "compose_first_text_layout"
internal const val EVENT_COMPOSE_LAZY_FIRST_SCREEN_STABLE = "compose_lazy_first_screen_stable"

internal fun supportsComposeVariant(variant: String): Boolean {
    return variant == CompanionContract.VARIANT_NORMAL
}

internal fun runAfterNextFrame(block: () -> Unit) {
    Choreographer.getInstance().postFrameCallback { block() }
}
```

The actual helper can evolve, but keep these constants centralized and ASCII-only.

- [ ] **Step 3: Implement compose_baseline_text**

Create `ComposeBaselineTextScene.kt` implementing `DisplayScene`.

Requirements:
- `id()` returns `compose_baseline_text`
- `supportsVariant()` returns true only for `normal`
- content is a `ComposeView`
- primary `Text` uses `14.sp`
- `onTextLayout` captures `TextLayoutResult`
- provider emits:
  - `compose_density`
  - `compose_font_scale`
  - `compose_text_sp=14.0`
  - `compose_text_px`
  - `compose_line_count`
  - `compose_layout_w`
  - `compose_layout_h`
  - `compose_rendered_scale`
  - `style_source=baseline`

Use plain Compose UI/Foundation. Do not use Material.

- [ ] **Step 4: Implement compose_nested_scroll_text**

Create `ComposeNestedScrollTextScene.kt`.

Requirements:
- `id()` returns `compose_nested_scroll_text`
- normal-only
- content has a vertical scroll container and at least one nested child before primary `Text`
- event is `compose_first_text_layout`
- provider includes `container=vertical_scroll`

- [ ] **Step 5: Implement compose_lazy_list_text**

Create `ComposeLazyListTextScene.kt`.

Requirements:
- `id()` returns `compose_lazy_list_text`
- normal-only
- content uses `LazyColumn`
- logged row is `item_index=0`
- `lazy_first_visible_index` must be `0`
- event is `compose_lazy_first_screen_stable`
- emit only one stable event per run
- use a next-frame callback after `TextLayoutResult` before reporting ready

- [ ] **Step 6: Implement compose_styled_text**

Create `ComposeStyledTextScene.kt`.

Requirements:
- `id()` returns `compose_styled_text`
- normal-only
- content uses local text style or parent style provider
- effective requested text size is `14.sp`
- provider includes `style_source=local_text_style`

- [ ] **Step 7: Register Compose scenes**

Update `SceneRegistry.java` to construct:

```java
private static List<DisplayScene> createComposeScenes() {
    List<DisplayScene> scenes = new ArrayList<>();
    scenes.add(new ComposeBaselineTextScene());
    scenes.add(new ComposeNestedScrollTextScene());
    scenes.add(new ComposeLazyListTextScene());
    scenes.add(new ComposeStyledTextScene());
    return scenes;
}
```

Update `create()` and `findById()` as described in Task 2.

- [ ] **Step 8: Run focused build**

Run:

```powershell
.\gradlew.bat :companion-display-tool:compileDebugJavaWithJavac :companion-display-tool:compileDebugKotlin
```

Expected:
- PASS.

---

### Task 5: Integrate Compose Runs Into Orchestrator And Control Flow

**Files:**
- Modify: `companion-display-tool/src/main/java/com/dpis/displaytool/RunOrchestrator.java`
- Modify: `companion-display-tool/src/main/java/com/dpis/displaytool/MainActivity.java`
- Modify: `companion-display-tool/src/main/java/com/dpis/displaytool/ControlReceiver.java`
- Modify: `companion-display-tool/src/main/java/com/dpis/displaytool/CompanionContract.java`

- [ ] **Step 1: Forward group extra from broadcast receiver**

In `ControlReceiver.java`, forward:

```java
activityIntent.putExtra(
        CompanionContract.EXTRA_GROUP,
        intent.getStringExtra(CompanionContract.EXTRA_GROUP)
);
```

- [ ] **Step 2: Add grouped run methods**

In `RunOrchestrator.java`, add:

```java
void runComposeColdStart(String trigger) {
    startRun(trigger, CompanionContract.VARIANT_MODE_NORMAL_ONLY,
            runsFor(registry.composeColdStartScenes(), CompanionContract.VARIANT_NORMAL));
}

private List<SceneRun> runsFor(List<DisplayScene> scenes, String variant) {
    List<SceneRun> runs = new ArrayList<>();
    for (DisplayScene scene : scenes) {
        runs.add(new SceneRun(scene, variant));
    }
    return runs;
}
```

Refactor `runAll` to use `runsFor(registry.coreScenes(), normal)`. Plain `runAll` remains native-only.

- [ ] **Step 3: Log Compose scene events through Compose formatter**

In `logAfterLayout(...)`, detect whether `presentation.composeFieldsProvider()` exists.

Required behavior:
- native scene: keep current `log.sceneEvent(...)`
- Compose scene: wait until provider is ready, then log a Compose scene event with `formatComposeSceneEvent`
- if provider is not ready at first global layout, post a bounded retry on the main handler rather than logging missing fields

Use a small bounded retry count, for example 8 attempts with `mainHandler.post(...)`. If not ready after retries, increment `errorTotal` and continue.

- [ ] **Step 4: Run Compose subset after native cold start on app launch**

In `MainActivity.scheduleColdStartRun()`, preserve the current native cold start and then schedule Compose subset. One acceptable shape is:

```java
detailHost.post(() -> runOrchestrator.runAll(CompanionContract.TRIGGER_COLD_START));
detailHost.postDelayed(
        () -> runOrchestrator.runComposeColdStart(CompanionContract.TRIGGER_COLD_START),
        500L
);
```

Prefer a callback from native run completion if it is easy to add without broad refactor. If using a delay, keep it local and document why the first pass uses a conservative delay.

- [ ] **Step 5: Handle action=run_all group=compose**

In `MainActivity.handleControlIntent(...)`, read:

```java
String group = intent.getStringExtra(CompanionContract.EXTRA_GROUP);
```

Update `ACTION_RUN_ALL` handling:

```java
if (CompanionContract.GROUP_COMPOSE.equals(group)) {
    runOrchestrator.runComposeColdStart(trigger);
} else {
    runOrchestrator.runAll(trigger);
}
```

Do not add Compose scenes to plain `run_all`.

- [ ] **Step 6: Run control-related tests**

Run:

```powershell
.\gradlew.bat :companion-display-tool:testDebugUnitTest
```

Expected:
- PASS.

---

### Task 6: Add Compose Scene UI Group

**Files:**
- Modify: `companion-display-tool/src/main/java/com/dpis/displaytool/MainActivity.java`
- Modify: `companion-display-tool/src/main/res/values/strings.xml`
- Optional Modify: `companion-display-tool/src/main/res/layout/activity_companion_display_tool.xml`

- [ ] **Step 1: Add simple group labels**

In `strings.xml`, add:

```xml
<string name="scene_group_native">Native</string>
<string name="scene_group_compose">Compose</string>
```

- [ ] **Step 2: Show Compose scenes in the existing scene list**

Update `MainActivity.bindSceneList()` to add rows for:
- all `registry.coreScenes()`
- all `registry.composeScenes()`

Keep click behavior unchanged:

```java
row.setOnClickListener(view -> showScene(scene.id(), CompanionContract.VARIANT_NORMAL));
```

If adding visual group labels requires layout churn, skip labels and list Compose scenes after native scenes. The scene ids already carry the `compose_` prefix.

- [ ] **Step 3: Verify no show_scene behavior drift**

Run:

```powershell
.\gradlew.bat :companion-display-tool:testDebugUnitTest
```

Expected:
- PASS.

---

### Task 7: Add Source Guards And Documentation Updates

**Files:**
- Create: `companion-display-tool/src/test/java/com/dpis/displaytool/ComposeContractSourceTest.java`
- Modify: `docs/companion-display-tool.md`

- [ ] **Step 1: Add source test for no WebView and no fragile Compose support**

Create `ComposeContractSourceTest.java`:

```java
package com.dpis.displaytool;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class ComposeContractSourceTest {
    @Test
    public void composePhase2DoesNotAddWebViewDependency() throws IOException {
        String build = read("build.gradle.kts");

        assertFalse(build.contains("androidx.webkit"));
        assertFalse(build.contains("WebView"));
    }

    @Test
    public void composeScenesRejectFragileVariant() throws IOException {
        String registryTest = read("src/test/java/com/dpis/displaytool/scene/SceneRegistryTest.java");

        assertTrue(registryTest.contains("composeScenesSupportOnlyNormalVariant"));
        assertTrue(registryTest.contains("compose_baseline_text"));
        assertTrue(registryTest.contains("compose_styled_text"));
    }

    private static String read(String relativePath) throws IOException {
        return Files.readString(Path.of(relativePath), StandardCharsets.UTF_8);
    }
}
```

This is a smoke guard, not a replacement for behavior tests.

- [ ] **Step 2: Update companion tool docs**

Update `docs/companion-display-tool.md` with:

````markdown
## Compose Phase 2

Compose phase 2 adds four normal-only scenes:

- `compose_baseline_text`
- `compose_nested_scroll_text`
- `compose_lazy_list_text`
- `compose_styled_text`

Cold start samples only:

- `compose_baseline_text`
- `compose_lazy_list_text`

Plain `run_all` remains native-only. To rerun the Compose cold-start subset:

```powershell
adb -s <device> shell am broadcast `
  -a io.github.kwensiu.dpis.displaytool.CONTROL `
  -n io.github.kwensiu.dpis.displaytool/com.dpis.displaytool.ControlReceiver `
  --es action run_all `
  --es group compose
```

Run one Compose scene:

```powershell
adb -s <device> shell am broadcast `
  -a io.github.kwensiu.dpis.displaytool.CONTROL `
  -n io.github.kwensiu.dpis.displaytool/com.dpis.displaytool.ControlReceiver `
  --es action run_scene `
  --es scene compose_baseline_text `
  --es variant normal
```

Compose logs keep the native prefix and append fields such as:

- `surface=compose`
- `compose_density`
- `compose_font_scale`
- `compose_text_sp`
- `compose_text_px`
- `compose_rendered_scale`
````

- [ ] **Step 3: Run documentation/source tests**

Run:

```powershell
.\gradlew.bat :companion-display-tool:testDebugUnitTest
```

Expected:
- PASS.

---

### Task 8: Final Verification

**Files:**
- No new implementation files unless fixing issues found during verification.

- [ ] **Step 1: Run companion unit tests**

Run:

```powershell
.\gradlew.bat :companion-display-tool:testDebugUnitTest
```

Expected:
- PASS.

- [ ] **Step 2: Build companion APK**

Run:

```powershell
.\gradlew.bat :companion-display-tool:assembleDebug
```

Expected:
- PASS.

- [ ] **Step 3: Run main app unit tests**

Run:

```powershell
.\gradlew.bat :app:testAllDebugUnitTests
```

Expected:
- PASS.

- [ ] **Step 4: Device smoke, if a device is available**

Install:

```powershell
adb -s <device> install -r "companion-display-tool\build\outputs\apk\debug\companion-display-tool-debug.apk"
```

Clear logs and launch:

```powershell
adb -s <device> logcat -c
adb -s <device> shell monkey -p io.github.kwensiu.dpis.displaytool 1
```

Collect logs:

```powershell
adb -s <device> logcat -d -v raw | Select-String -Pattern "DPIS_TEST"
```

Expected:
- phase 1 native cold start still emits native run boundaries
- Compose cold-start subset emits `compose_baseline_text` and `compose_lazy_list_text`
- no automatic `compose_nested_scroll_text`
- no automatic `compose_styled_text`
- Compose lines include `surface=compose` after the shared prefix

Run explicit Compose scene:

```powershell
adb -s <device> shell am broadcast `
  -a io.github.kwensiu.dpis.displaytool.CONTROL `
  -n io.github.kwensiu.dpis.displaytool/com.dpis.displaytool.ControlReceiver `
  --es action run_scene `
  --es scene compose_styled_text `
  --es variant normal
```

Expected:
- a `compose_styled_text` scene event is logged
- no WebView logs appear

- [ ] **Step 5: Commit**

Use a scoped commit message:

```powershell
git add companion-display-tool docs gradle
git commit -m "feat: add compose phase 2 display tool scenes"
```

---

## Self-Review Checklist

- Spec coverage:
  - Four Compose scenes are planned.
  - Normal-only Compose variants are planned.
  - Cold-start Compose subset is planned.
  - `run_all group=compose` is planned and plain `run_all` remains native-only.
  - Compose log fields and field order are planned.
  - WebView remains out of scope.

- Placeholder scan:
  - No `TODO` or `TBD` tasks should remain in implementation.
  - No generic command interpreter should be introduced.

- Type consistency:
  - `CompanionContract.EXTRA_GROUP` is used by `ControlReceiver` and `MainActivity`.
  - `SceneRegistry.create()` is used by `MainActivity`.
  - Existing `SceneRegistry.createPhase1()` remains available for compatibility if tests need it.
  - Native `formatSceneEvent(...)` output remains unchanged.
