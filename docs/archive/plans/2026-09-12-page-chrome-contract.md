# Page Chrome Contract Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make every phone/tablet page and landscape split pane share one fixed-height chrome slot, with the large title as translating content (primary starts expanded, secondary starts collapsed).

**Architecture:** Keep the current translation collapse (compact `TopAppBar` never changes height; expanded title is content). Add one chrome token for that compact slot. Secondary entry applies a full collapse offset on first measure instead of flashing expanded. Landscape App/Template detail uses an in-pane header locked to the same token, not a second page scaffold. Do not migrate App list scroll out of `MainUiState` in this change, and do not edit `MainActivity` / filter-sheet files owned by other in-flight work.

**Tech Stack:** Jetpack Compose Material3, JUnit4 JVM tests, existing `PageScaffold` / `PageCollapseScrollConnection`.

**Spec:** `docs/superpowers/specs/2026-09-12-page-chrome-contract.md`

---

## File map

| File | Responsibility |
|---|---|
| Create `app/src/main/java/com/dpis/module/ui/presentation/workspace/PageChrome.kt` | Compact-slot token (64.dp) and search-card math (6+52+6). |
| Modify `PageCollapseScroll.kt` | Pure `initialCollapsePx` used when the expanded title first measures. |
| Modify `PageScrollPositionStore.kt` | Nullable stored collapse override (`Boolean?`) so “never stored” ≠ expanded. |
| Modify `PageScaffold.kt` | `startCollapsed`; measure-then-collapse for secondary; `SecondaryPageScaffold` starts collapsed; `PrimaryPageScaffold` is collapsing + expanded. |
| Modify `PageTopBar.kt` | `SplitPaneHeader`: in-flow 64.dp bar, no status-bar insets (parent already padded). |
| Modify `AppWorkspaceContent.kt` | Search slot uses the token; landscape detail column uses `SplitPaneHeader`. |
| Modify `TemplateUiTokens.kt` | Search paddings alias `PageChromeTokens` instead of a private 64.dp copy. |
| Modify `TemplateWorkspaceContent.kt` | Landscape editor/empty pane uses `SplitPaneHeader`. |
| Modify `QuickTemplateTargetsContent.kt` | Landscape in-flow bar stays compact-slot height (no extra status inset on top of a padded pane). |
| Test `PageChromeTest.kt` | Token math. |
| Test `PageCollapseScrollTest.kt` | Initial offset policy. |
| Test `PageScrollPositionStore` coverage in the workspace test package | Stored override vs default start. |
| Modify `ComposeShellSourceSmokeTest.kt` | Wiring: startCollapsed, SplitPaneHeader, token, no `TwoRowsTopAppBar`. |

Out of scope: Wear, sheets, `MainActivity`, App filter sheet, App list `MainUiState` scroll migration, XML `LandAppDetail*` binders.

---

### Task 1: Compact chrome token

**Files:**
- Create: `app/src/main/java/com/dpis/module/ui/presentation/workspace/PageChrome.kt`
- Test: `app/src/test/java/com/dpis/module/ui/presentation/workspace/PageChromeTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
package com.dpis.module.ui.compose

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class PageChromeTest {
    @Test
    fun searchCardFitsTheCompactChromeSlot() {
        assertEquals(
            PageChromeTokens.CompactSlotHeight,
            PageChromeTokens.SearchVerticalPadding * 2
                + PageChromeTokens.SearchCardHeight,
        )
        assertEquals(64.dp, PageChromeTokens.CompactSlotHeight)
        assertEquals(52.dp, PageChromeTokens.SearchCardHeight)
        assertEquals(6.dp, PageChromeTokens.SearchVerticalPadding)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testModernDebugUnitTest --tests com.dpis.module.ui.compose.PageChromeTest`

Expected: FAIL — `PageChromeTokens` unresolved.

- [ ] **Step 3: Write minimal implementation**

```kotlin
package com.dpis.module.ui.compose

import androidx.compose.ui.unit.dp

/** Shared compact top-slot used by page bars, App/Template search, and split-pane headers. */
internal object PageChromeTokens {
    val CompactSlotHeight = 64.dp
    val SearchCardHeight = 52.dp
    val SearchVerticalPadding = 6.dp
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :app:testModernDebugUnitTest --tests com.dpis.module.ui.compose.PageChromeTest`

Expected: PASS.

---

### Task 2: Initial collapse offset policy

**Files:**
- Modify: `app/src/main/java/com/dpis/module/ui/presentation/workspace/PageCollapseScroll.kt`
- Modify: `app/src/main/java/com/dpis/module/ui/presentation/workspace/PageScrollPositionStore.kt`
- Test: `app/src/test/java/com/dpis/module/ui/presentation/workspace/PageCollapseScrollTest.kt`
- Test: `app/src/test/java/com/dpis/module/ui/presentation/workspace/PageScrollPositionStoreTest.kt`

- [ ] **Step 1: Write the failing tests**

```kotlin
@Test
fun secondaryStartsFullyCollapsedWhenNothingIsStored() {
    assertEquals(80f, initialCollapsePx(80f, startCollapsed = true, storedCollapsed = null), 0.01f)
}

@Test
fun primaryStartsExpandedWhenNothingIsStored() {
    assertEquals(0f, initialCollapsePx(80f, startCollapsed = false, storedCollapsed = null), 0.01f)
}

@Test
fun storedCollapseOverridesThePageDefault() {
    assertEquals(80f, initialCollapsePx(80f, startCollapsed = false, storedCollapsed = true), 0.01f)
    assertEquals(0f, initialCollapsePx(80f, startCollapsed = true, storedCollapsed = false), 0.01f)
}

@Test
fun unknownRangeDoesNotInventAnOffset() {
    assertEquals(0f, initialCollapsePx(0f, startCollapsed = true, storedCollapsed = null), 0.01f)
}
```

Store tests:

```kotlin
@Test
fun missingTopBarEntryIsUnknownNotExpanded() {
    val store = PageScrollPositionStore()
    assertEquals(null, store.storedTopBarCollapsed("about"))
}

@Test
fun storedTopBarCollapsedRoundTrips() {
    val store = PageScrollPositionStore()
    store.updateTopBar("about", true)
    assertEquals(true, store.storedTopBarCollapsed("about"))
    store.updateTopBar("about", false)
    assertEquals(false, store.storedTopBarCollapsed("about"))
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :app:testModernDebugUnitTest --tests com.dpis.module.ui.compose.PageCollapseScrollTest --tests com.dpis.module.ui.compose.PageScrollPositionStoreTest`

Expected: FAIL — `initialCollapsePx` / `storedTopBarCollapsed` unresolved.

- [ ] **Step 3: Write minimal implementation**

```kotlin
internal fun initialCollapsePx(
    rangePx: Float,
    startCollapsed: Boolean,
    storedCollapsed: Boolean?,
): Float {
    if (rangePx <= 0f) return 0f
    val collapsed = storedCollapsed ?: startCollapsed
    return if (collapsed) rangePx else 0f
}
```

```kotlin
fun storedTopBarCollapsed(key: String): Boolean? =
    positions["@topbar:$key"]?.let { it.index == 1 }
```

Keep `topBarCollapsedFor` as `storedTopBarCollapsed(key) == true` so existing callers stay valid.

- [ ] **Step 4: Run tests to verify they pass**

Expected: PASS.

---

### Task 3: PageScaffold applies startCollapsed without a first-frame jump

**Files:**
- Modify: `app/src/main/java/com/dpis/module/ui/presentation/workspace/PageScaffold.kt`
- Modify: `app/src/test/java/com/dpis/module/ui/presentation/ComposeShellSourceSmokeTest.kt`

- [ ] **Step 1: Add `startCollapsed` and wire the two scaffolds**

`SecondaryPageScaffold` → `PageScaffold(..., pageBar = Collapsing, startCollapsed = true)`.

`PrimaryPageScaffold` → `PageScaffold(..., pageBar = Collapsing, startCollapsed = false)` (today it is `Pinned` and unused; pinned remains for Log and other compact-only pages).

- [ ] **Step 2: First measure applies `initialCollapsePx`**

When `startCollapsed` and range is still 0, compose a 0-height unbounded measurer of `CollapsingExpandedTitle` that writes range + initial offset in `onSizeChanged`. Only after range > 0 put the real title into the translating column.

Collapsed-start first frame: list fills the viewport (already the collapsed look). Second frame: title exists above the list with `translationY = -range` (still collapsed). Expanded-start keeps today’s path (title in the column immediately).

Replace the restore `LaunchedEffect` that only reads `topBarCollapsedFor` with `storedTopBarCollapsed` so a stored *expanded* secondary does not get forced collapsed, and a stored *collapsed* primary restores.

- [ ] **Step 3: Update smoke wiring**

In `standaloneSettingsPagesUseSharedSecondaryPageChrome`:

- `startCollapsed = true` on both `SecondaryPageScaffold` overloads
- `PageBarBehavior.Collapsing` on `PrimaryPageScaffold`
- keep `translationY` / `PageCollapseScrollConnection` / no `TwoRowsTopAppBar`

- [ ] **Step 4: Run tests**

Run: `./gradlew :app:testModernDebugUnitTest --tests com.dpis.module.ComposeShellSourceSmokeTest.standaloneSettingsPagesUseSharedSecondaryPageChrome --tests com.dpis.module.ui.compose.PageCollapseScrollTest`

Expected: PASS.

---

### Task 4: App / Template search consume the token

**Files:**
- Modify: `app/src/main/java/com/dpis/module/applist/presentation/AppWorkspaceContent.kt`
- Modify: `app/src/main/java/com/dpis/module/templates/presentation/TemplateUiTokens.kt`
- Modify: `app/src/main/java/com/dpis/module/templates/presentation/TemplateWorkspaceList.kt` (only if still inlining 6/52/6)

- [ ] **Step 1: Replace local 6/52/6**

App search `Modifier`:

```kotlin
.padding(
    top = PageChromeTokens.SearchVerticalPadding,
    bottom = PageChromeTokens.SearchVerticalPadding,
)
.height(PageChromeTokens.SearchCardHeight)
```

Wrap the search card in `Modifier.height(PageChromeTokens.CompactSlotHeight)` if the outer slot must be exactly 64.dp including padding (padding is inside the 64.dp slot: 6+52+6).

Template:

```kotlin
val SearchTopPadding = PageChromeTokens.SearchVerticalPadding
val SearchBottomPadding = PageChromeTokens.SearchVerticalPadding
val SearchCardHeight = PageChromeTokens.SearchCardHeight
```

Do not add a large title to App/Template.

- [ ] **Step 2: Smoke / token test still pass**

Expected: PASS.

---

### Task 5: Landscape split-pane header = left chrome slot

**Files:**
- Modify: `app/src/main/java/com/dpis/module/ui/presentation/workspace/PageTopBar.kt`
- Modify: `app/src/main/java/com/dpis/module/applist/presentation/AppWorkspaceContent.kt`
- Modify: `app/src/main/java/com/dpis/module/templates/presentation/TemplateWorkspaceContent.kt`
- Modify: `app/src/main/java/com/dpis/module/templates/presentation/QuickTemplateTargetsContent.kt`
- Modify: `ComposeShellSourceSmokeTest.kt`

- [ ] **Step 1: Add `SplitPaneHeader`**

In-flow `TopAppBar` with `windowInsets = WindowInsets(0, 0, 0, 0)` and `Modifier.height(PageChromeTokens.CompactSlotHeight)`. Parent columns already apply `statusBars` padding, matching the left list (`padding(top = topSafePadding)` + 64.dp search).

`InFlowPageHeader` / `SecondaryPageTopBar` used inside an already padded pane should call `SplitPaneHeader` (or pass zero insets). Do not stack status-bar insets on the bar itself.

- [ ] **Step 2: App landscape detail**

Right pane column:

```kotlin
Column(Modifier.fillMaxHeight().padding(top = topSafePadding)) {
    SplitPaneHeader(
        onBack = null,
        title = {
            if (editorState != null) {
                Text(editorState.item.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        },
    )
    Box(Modifier.weight(1f)) {
        // extraTopPadding = 0.dp; child destinations keep their own sheet headers
        ...
    }
}
```

Empty detail uses the same 64.dp slot (title empty) so the chrome line matches the search field.

- [ ] **Step 3: Template landscape detail**

Same column around `editorBody()` / `TemplateDetailEmptyState`. Pass `topSafePadding = 0.dp` into `TemplateEditorSurface` when the pane column already consumed status + chrome. Embedded targets: use `SplitPaneHeader` instead of `SecondaryPageTopBar` with status insets.

- [ ] **Step 4: Smoke**

Assert `SplitPaneHeader(` and `PageChromeTokens.CompactSlotHeight` exist; App/Template landscape panes call `SplitPaneHeader(`.

---

### Task 6: Full JVM verification

- [ ] **Step 1: Run workspace + shell tests**

```
./gradlew :app:testModernDebugUnitTest --tests com.dpis.module.ui.compose.PageChromeTest --tests com.dpis.module.ui.compose.PageCollapseScrollTest --tests com.dpis.module.ui.compose.PageScrollPositionStoreTest --tests com.dpis.module.ComposeShellSourceSmokeTest
```

Expected: PASS.

- [ ] **Step 2: Run full unit suite if those pass**

```
./gradlew :app:testAllDebugUnitTests
```

Do not commit unless asked. Do not install APK unless the suite is green and device work is requested.

---

## Spec coverage

| Spec rule | Task |
|---|---|
| Compact slot height never changes | 1, 3, 4 (translation collapse already landed) |
| Large title is content | 3 (unchanged mechanic) |
| Primary expanded / secondary collapsed on entry | 2, 3 |
| App/Template = compact slot, no large title | 4 |
| Landscape right header height = left slot | 5 |
| Host stays nav-only | no WorkspaceShell title work |
| No TwoRows / LargeFlexible height morph | smoke in 3/5 |
| App list scroll not moved this wave | explicit out of scope |
