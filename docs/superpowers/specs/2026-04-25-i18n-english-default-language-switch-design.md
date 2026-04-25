# i18n English Default and Language Switch Design

## Goal
Resolve GitHub issue #16 by making English the default fallback language, preserving Simplified Chinese for Chinese locales, and adding an in-app language switch.

## Context
DPIS currently stores user-visible UI strings in `app/src/main/res/values/strings.xml`, and the default resource set is Simplified Chinese. Android uses `values/` as the fallback for all unmatched locales, so non-Chinese users see Chinese. The project already uses AppCompat `1.7.1`, which supports per-app locales through `AppCompatDelegate.setApplicationLocales`.

## Chosen Approach
Use English as the default Android fallback and move the current Chinese strings into a locale-specific resource directory. Add a language row to the existing settings screen so users can choose system default, English, or Simplified Chinese without changing device language.

## Resource Design
- `app/src/main/res/values/strings.xml` becomes the English default resource file.
- `app/src/main/res/values-zh-rCN/strings.xml` contains the existing Simplified Chinese strings.
- Resource names remain unchanged to avoid broad Java/XML churn.
- New language-switch strings are added to both locales.

## UI Design
- Add a `Language` entry row in the Settings screen under the Other section.
- Tapping the row opens a single-choice Material dialog with:
  - Follow system
  - English
  - 简体中文
- Selecting an item applies the locale immediately through AppCompat and dismisses the dialog.

## Scope
Included:
- App UI strings backed by Android resources.
- Settings language selector.
- Resource parity test to prevent missing translations.

Excluded:
- Hook logs and internal diagnostics that are not part of ordinary UI.
- Full README translation.
- Runtime validation on a physical device.

## Verification
- Unit test checks default and Chinese string resources have matching names.
- Debug build validates Android resource compilation.
- Existing unit test suite validates no Java-side regressions.
