# Modern / Legacy Semantic Cleanup Plan

## Goal

Clean up DPIS build and runtime terminology so user-facing, contributor-facing,
CI, documentation, and code semantics consistently describe the two APK tracks
as `Modern` and `Legacy`.

The original implementation exposed historical names such as `modern101`
and `compat100`. Those names describe implementation details that should not be
the product language.

## Current Preferred Product Semantics

- `Modern`: the primary APK track for current LSPosed/libxposed environments.
- `Legacy`: the fallback APK track for classic Xposed-compatible environments.

## Terminology Decisions

- Use `Modern` and `Legacy` as DPIS APK track names.
- Describe `Modern` as the primary DPIS APK for LSPosed/libxposed modern API
  environments. `Modern APK` is a DPIS product term, backed by the upstream
  "Modern Xposed API" ecosystem wording.
- Describe `Legacy` as the fallback DPIS APK for classic Xposed-compatible
  frameworks.
- Do not use `Xposed-Init framework` as the user-facing environment name.
  `xposed_init` is an entrypoint mechanism, not the framework family.
- Keep `Classic Xposed` / `传统 Xposed` as the external framework/API family
  description, while `Legacy` remains the DPIS APK track name.
- Do not use `XposedBridge build` as the user-facing APK name. Technical notes
  may say the Legacy APK uses the traditional `xposed_init` /
  `XposedBridge-style` module entrypoint.

## Likely Cleanup Areas

### User-Facing Documentation

- `README.md`
- `docs/README.en.md`
- `CONTRIBUTING.md`

Clean up `Standard`, `标准版`, `兼容版`, `modern101`, and `compat100` so the
visible APK tracks are `Modern` and `Legacy`.

### Live Route Documentation

Completed rename targets:

- `docs/modern101-runtime-resync.md` -> `docs/modern-runtime-resync.md`
- `docs/compat100-runtime-resync.md` -> `docs/legacy-runtime-resync.md`

Update references in:

- `docs/README.md`
- `docs/agents/domain.md`
- `docs/agents/skills/dpis-runtime-route-diagnose/SKILL.md`
- `CONTRIBUTING.md`
- `CONTEXT.md`

### CI And Local Scripts

Use `modern` and `legacy` in summaries, artifact names, human-facing script
messages, and current Gradle task/output paths.

Likely files:

- `.github/workflows/ci.yml`
- `.github/workflows/debug-build.yml`
- `.github/workflows/release.yml`
- `.github/workflows/telegram-preview.yml`
- `scripts/test-telegram-preview-local.sh`
- `scripts/telegram-preview-media.py`

### Gradle Flavor Rename

Completed implementation rename:

- `modern101` -> `modern`
- `compat100` -> `legacy`

Expected task/path fallout:

- `assembleModern101Debug` -> `assembleModernDebug`
- `assembleCompat100Debug` -> `assembleLegacyDebug`
- `testModern101DebugUnitTest` -> `testModernDebugUnitTest`
- `testCompat100DebugUnitTest` -> `testLegacyDebugUnitTest`
- `lintModern101Debug` -> `lintModernDebug`
- `app/src/modern101` -> `app/src/modern`
- `app/src/compat100` -> `app/src/legacy`
- `app/build/outputs/apk/modern101` -> `app/build/outputs/apk/modern`
- `app/build/outputs/apk/compat100` -> `app/build/outputs/apk/legacy`

### Java Class And Method Names

Completed semantic cleanup:

- `Compat100LegacyModuleHook` -> `LegacyModuleHook`
- `Compat100...Installer` -> `Legacy...Installer`
- `Modern101AppSpecificRouteInstaller` -> `ModernAppSpecificRouteInstaller`
- `shouldInstallCompat100LegacyHooks()` -> `shouldInstallLegacyHooks()`
- `createForCompat100Host()` -> `createForLegacyHost()`
- `withCompat100RuntimePropertyFallback()` -> `withLegacyRuntimePropertyFallback()`
- `setTargetPackageNameForCompat100()` -> `setTargetPackageNameForLegacy()`

## Terms Not To Blindly Replace

- `compat mode` / `兼容模式`: app configuration route semantics, not the legacy
  APK track.
- `legacy persisted values`: old data migration semantics, not the legacy APK
  track. May be reworded to `old persisted values` if it improves clarity.
- `xposed-legacy-api` / `legacy-xposed-api`: external dependency naming.
- `libxposed`: real upstream technology name.

## Suggested Execution Order

1. Decide terminology for the legacy runtime environment and `Modern` naming.
2. Update user-facing docs and contributor docs.
3. Rename live route docs and agent references.
4. Rename Gradle flavors and source sets.
5. Rename Java classes, methods, and smoke tests.
6. Run targeted source smoke tests, then `./gradlew :app:testAllDebugUnitTests`.

## Progress

- 2026-06-13: terminology decisions recorded.
- 2026-06-13: user-facing README variant names updated from
  Standard/Compatibility wording to Modern/Legacy.
- 2026-06-13: contributor and agent route-document references updated to
  `docs/modern-runtime-resync.md` and `docs/legacy-runtime-resync.md`.
- 2026-06-13: living route documents renamed and their document headers/rules
  updated. Historical ledger entries still retain old implementation names when
  they describe past evidence.
- 2026-06-13: local Telegram preview script help text updated to say
  Modern/Legacy while retaining current Gradle task and output paths.
- 2026-06-13: Gradle flavors and source sets renamed to `modern` and `legacy`;
  CI, release, Telegram preview, and local preview paths now use the new task
  and APK output names.
- 2026-06-13: Legacy and Modern route class/helper names updated where they
  exposed `Compat100` / `Modern101` implementation language.
- 2026-06-13: static sub-agent review found no critical issues. Important
  finding fixed: flavor-specific unit test source set moved from unrecognized
  `legacyTest` to AGP-recognized `testLegacy`. Telegram preview caption remains
  intentionally attached to the last media item so files stay above the message
  text in the Telegram media group.

Remaining work:

- Re-run targeted `LegacyXposedSelfActivationTest` discovery and full debug
  unit tests after the `testLegacy` move.
- Review remaining historical docs/specs before deciding whether to rewrite old
  verification commands or leave them as historical evidence.
