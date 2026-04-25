# i18n English Default and Language Switch Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make English the default fallback language, preserve Simplified Chinese resources, and add a settings language switch.

**Architecture:** Android resource fallback handles default English. A locale-specific `values-zh-rCN` directory preserves Chinese. `SystemServerSettingsActivity` owns the language entry and applies choices using AppCompat per-app locales.

**Tech Stack:** Java 17, Android resources, AppCompat 1.7.1, Material dialogs, JUnit4 source/resource smoke tests.

---

### Task 1: Resource Locales

**Files:**
- Modify: `app/src/main/res/values/strings.xml`
- Create: `app/src/main/res/values-zh-rCN/strings.xml`

- [ ] Copy current Chinese strings into `values-zh-rCN/strings.xml`.
- [ ] Replace default `values/strings.xml` values with English translations while keeping every `name` unchanged.
- [ ] Add language selector string keys to both files.

### Task 2: Settings Language Row

**Files:**
- Modify: `app/src/main/res/layout/activity_system_server_settings.xml`
- Modify: `app/src/main/java/com/dpis/module/SystemServerSettingsActivity.java`

- [ ] Add `row_language` under the Settings > Other card.
- [ ] Bind the row with language title/subtitle strings.
- [ ] Show a single-choice dialog for follow system, English, and Simplified Chinese.
- [ ] Apply the selected option with `AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(...))`.

### Task 3: Resource Parity Test

**Files:**
- Create: `app/src/test/java/com/dpis/module/StringResourceParityTest.java`

- [ ] Parse default and Chinese `strings.xml` files.
- [ ] Assert both files expose the same string names.
- [ ] Run the targeted test and then full app unit tests.

### Task 4: Build Verification

**Files:**
- No source files.

- [ ] Run `./gradlew :app:testDebugUnitTest`.
- [ ] Run `./gradlew :app:assembleDebug`.
