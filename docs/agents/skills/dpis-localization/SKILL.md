---
name: dpis-localization
description: Project-local DPIS localization workflow. Use when adding a new language, machine-translating or reviewing Android string resources, changing existing translations, adjusting AppLocaleManager language options, updating l10n scripts, or reviewing localization-related branch changes.
---

# DPIS Localization

This is a project-local skill bundle. It turns DPIS localization work into a
repeatable process for new languages, existing-language maintenance, and review.

## Trigger

Use this skill before work involving:

- adding or removing `app/src/main/res/values-*/strings.xml`
- translating, machine-translating, reviewing, or normalizing strings
- changing language names, locale tags, or manual language choices
- editing `AppLocaleManager` or the language selector UI
- changing `scripts/l10n/**`, `crowdin.yml`, or localization docs
- reviewing a branch whose main surface is Android localization

## Required Reads

Read only what applies to the task:

- Always read `AGENTS.md`.
- For string changes, read `app/src/main/res/values/strings.xml` and the touched
  locale file.
- For manual language choices, read
  `app/src/main/java/com/dpis/module/AppLocaleManager.java` and the language
  selector tests.
- For script or workflow changes, read `scripts/l10n/README.md` and the touched
  script.
- For review, inspect the diff against the target branch before conclusions.

## Translation Frame

DPIS is a technical Android module for per-app DPI, interface scale, font size,
and runtime hook configuration. Translate for Android users who understand apps,
settings, and basic module concepts, but may not understand every hook detail.

Keep UI copy direct, calm, compact, and practical. Do not add claims, warnings,
or explanations that are not present in the source string.

Avoid:

- marketing claims, slogans, jokes, or slang
- over-promising compatibility, stability, or visible effect
- translating internal identifiers or hook domains
- changing the meaning of safety, unsupported, unavailable, or failed states

## Technical Terms

Keep these terms in English unless the source string clearly calls for a natural
language explanation around them:

- DPIS
- LSPosed
- Xposed
- Android
- WebView
- TextView
- ActivityThread
- Resources
- DisplayMetrics
- Flutter
- HyperOS
- DPI
- dp
- sp
- px
- APK
- API
- GitHub
- WeChat Pay
- Alipay

Follow existing nearby translation style for surrounding text. Do not force a
glossary term when it makes the target-language UI unnatural.

## Invariants

Preserve exactly:

- Android placeholders such as `%1$s`, `%2$d`, `%1$d%%`, and `%%`
- escaped newlines such as `\n`
- URLs, package names, resource-like identifiers, and domain-like identifiers
- strings marked `translatable="false"` in the default resource file

Keep placeholder order unless the target language requires reordering. If
reordering is needed, keep numbered placeholder forms.

Current internal font domains are not user-facing hook-chain switches:

- `system_server_font`
- `activity_thread_font`

## New Language Workflow

1. Identify the target locale and inspect existing resource qualifiers before
   choosing the directory name. Prefer matching the repository's current Android
   qualifier style.
2. Generate or normalize from `app/src/main/res/values/strings.xml`; do not
   include default strings marked `translatable="false"` unless they are
   intentionally used as local language names in the default file.
3. Preserve placeholder sets, escapes, URLs, package names, and technical terms.
4. Decide whether the language is manually selectable:
   - If it is reviewed or intentionally offered, add the locale tag and label to
     `AppLocaleManager`.
   - If it is machine-translated or not reviewed, either leave it reachable only
     through "Follow system" or label the manual option honestly, such as
     `日本語（未校正）`.
5. Add or update tests that cover resource parity and language selector wiring
   when the selector changes.
6. Run a focused resource test, then run the full unit suite before submitting
   unless the user explicitly accepts narrower verification.

## Existing Language Workflow

1. Compare the touched locale against default strings by key, placeholder set,
   and escape usage.
2. Check that translations remain concise and match nearby project wording.
3. Remove stale localized entries for keys that are no longer translatable or no
   longer exist.
4. Prefer fallback to English over low-confidence translations for risky
   technical or safety wording.
5. If the change comes from machine translation, mark review state in the user
   facing label or project docs when the language is manually selectable.

## Review Checklist

For localization branch review, check:

- New locale files are reachable as intended: follow-system only or manual
  selector.
- `AppLocaleManager` tags match Android resource qualifiers and user-facing
  labels.
- Machine-translated languages are not silently presented as fully reviewed.
- Placeholder, newline, URL, and package-name parity is intact.
- `translatable="false"` strings are not duplicated into locale resources unless
  there is an explicit product reason.
- `scripts/l10n/README.md` commands match real Gradle tasks.
- Tests cover changed wiring instead of only preserving stale string literals.

## Validation Commands

Use real task names from the current Gradle project. Common checks:

```bash
./gradlew :app:testModernDebugUnitTest --tests com.dpis.module.StringResourceParityTest
./gradlew :app:testAllDebugUnitTests
```

For script changes, also run the touched script on a representative input or use
its `--help` output if no safe fixture exists.

## Output

When finishing localization work or review, report:

- languages and locale qualifiers touched
- whether each language is reviewed, machine-translated, or intentionally
  follow-system only
- language selector changes, if any
- scripts or docs changed
- tests or validation commands run
