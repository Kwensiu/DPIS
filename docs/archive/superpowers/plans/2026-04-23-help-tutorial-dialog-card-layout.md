# Help Tutorial Dialog Card Layout Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the list-page help markdown dialog with a dedicated two-card help dialog that reads cleanly and keeps the existing `确认` close action.

**Architecture:** Add a dedicated helper and layout for the help tutorial dialog, wire `MainActivity` to that helper, and express the copy as individual string resources so layout structure rather than markdown controls the hierarchy. Avoid introducing a shared rich text abstraction for this change.

**Tech Stack:** Java 17, Android XML layouts, Material 3 components, JUnit4 source/layout smoke tests.

---

### Task 1: Lock the new dialog contract in smoke tests

**Files:**
- Modify: `app/src/test/java/com/dpis/module/MainActivitySourceSmokeTest.java`
- Create: `app/src/test/java/com/dpis/module/HelpTutorialDialogLayoutSmokeTest.java`

- [ ] **Step 1: Write the failing test**

```java
@Test
public void helpFabUsesDedicatedHelpTutorialDialog() throws IOException {
    String source = read("src/main/java/com/dpis/module/MainActivity.java");

    assertTrue(source.contains("HelpTutorialDialog.show(this);"));
    assertTrue(!source.contains("RichTextDialog.show("));
    assertTrue(!source.contains("R.string.help_tutorial_message"));
}
```

```java
@Test
public void helpTutorialDialogLayoutContainsTwoCardsAndConfirmButton() throws IOException {
    String layout = read("src/main/res/layout/dialog_help_tutorial.xml");
    String strings = read("src/main/res/values/strings.xml");

    assertTrue(layout.contains("@+id/help_tutorial_emulation_card"));
    assertTrue(layout.contains("@+id/help_tutorial_replace_card"));
    assertTrue(layout.contains("@+id/help_tutorial_confirm_button"));
    assertTrue(strings.contains("help_tutorial_emulation_badge"));
    assertTrue(strings.contains("help_tutorial_replace_badge"));
    assertTrue(!strings.contains("name=\"help_tutorial_message\""));
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests com.dpis.module.MainActivitySourceSmokeTest --tests com.dpis.module.HelpTutorialDialogLayoutSmokeTest`
Expected: FAIL because `HelpTutorialDialog` and `dialog_help_tutorial.xml` do not exist yet and `help_tutorial_message` is still present.

- [ ] **Step 3: Write minimal implementation**

Create the dedicated helper/layout/strings referenced by the tests and update `MainActivity` to call the helper.

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests com.dpis.module.MainActivitySourceSmokeTest --tests com.dpis.module.HelpTutorialDialogLayoutSmokeTest`
Expected: PASS with both smoke test classes green.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/dpis/module/MainActivity.java app/src/main/java/com/dpis/module/HelpTutorialDialog.java app/src/main/res/layout/dialog_help_tutorial.xml app/src/main/res/values/strings.xml app/src/test/java/com/dpis/module/MainActivitySourceSmokeTest.java app/src/test/java/com/dpis/module/HelpTutorialDialogLayoutSmokeTest.java
git commit -m "feat: redesign help tutorial dialog"
```

### Task 2: Match the approved card hierarchy and styling

**Files:**
- Create: `app/src/main/res/drawable/help_tutorial_emulation_card_background.xml`
- Create: `app/src/main/res/drawable/help_tutorial_replace_card_background.xml`
- Modify: `app/src/main/res/layout/dialog_help_tutorial.xml`
- Modify: `app/src/main/res/values/strings.xml`

- [ ] **Step 1: Write the failing test**

```java
@Test
public void helpTutorialDialogCardsUseBadgeSummaryAndBulletHierarchy() throws IOException {
    String layout = read("src/main/res/layout/dialog_help_tutorial.xml");
    String strings = read("src/main/res/values/strings.xml");

    assertTrue(layout.contains("@id/help_tutorial_emulation_badge"));
    assertTrue(layout.contains("@id/help_tutorial_emulation_summary"));
    assertTrue(layout.contains("@id/help_tutorial_replace_badge"));
    assertTrue(layout.contains("@id/help_tutorial_replace_summary"));
    assertTrue(strings.contains("通过系统层链路伪装相关参数"));
    assertTrue(strings.contains("通过字段重写直接覆盖缩放"));
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests com.dpis.module.HelpTutorialDialogLayoutSmokeTest`
Expected: FAIL until the layout exposes the approved card hierarchy and final card copy.

- [ ] **Step 3: Write minimal implementation**

Add the two card backgrounds, badge/summary/bullet rows, and the final approved strings with the `确认` action copy.

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests com.dpis.module.HelpTutorialDialogLayoutSmokeTest`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/res/layout/dialog_help_tutorial.xml app/src/main/res/drawable/help_tutorial_emulation_card_background.xml app/src/main/res/drawable/help_tutorial_replace_card_background.xml app/src/main/res/values/strings.xml app/src/test/java/com/dpis/module/HelpTutorialDialogLayoutSmokeTest.java
git commit -m "feat: add card-based help tutorial dialog layout"
```

### Task 3: Verify the list page integration still holds

**Files:**
- Modify: `app/src/test/java/com/dpis/module/MainActivityLayoutSmokeTest.java`
- Modify: `app/src/test/java/com/dpis/module/MainActivitySourceSmokeTest.java`

- [ ] **Step 1: Write the failing test**

```java
@Test
public void activityStatusLayoutStillContainsHelpFabEntry() throws IOException {
    String layout = read("src/main/res/layout/activity_status.xml");

    assertTrue(layout.contains("android:id=\"@+id/help_fab\""));
    assertTrue(layout.contains("app:srcCompat=\"@drawable/ic_info_outline_24\""));
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests com.dpis.module.MainActivityLayoutSmokeTest --tests com.dpis.module.MainActivitySourceSmokeTest`
Expected: FAIL if the wiring regressed while switching help dialog implementations.

- [ ] **Step 3: Write minimal implementation**

Keep the existing help FAB wiring and update the source smoke test to assert the dedicated helper path instead of the markdown dialog path.

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests com.dpis.module.MainActivityLayoutSmokeTest --tests com.dpis.module.MainActivitySourceSmokeTest`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/test/java/com/dpis/module/MainActivityLayoutSmokeTest.java app/src/test/java/com/dpis/module/MainActivitySourceSmokeTest.java
git commit -m "test: cover dedicated help dialog wiring"
```
