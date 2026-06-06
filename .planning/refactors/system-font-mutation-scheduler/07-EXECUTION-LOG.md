# System Font Mutation Scheduler Execution Log

## 2026-06-07

- Created route-first planning artifacts.
- No source files edited in this planning step.
- Current recommended mode: untangle-first.
- First proposed implementation wave: behavior lock and vocabulary only.
- Wave 1 approved by user.
- Added field-level scheduler TODO anchors and characterization tests for
  independent viewport/font system_server mutation selection.
- Verification passed:
  - `./gradlew :app:testModern101DebugUnitTest --tests com.dpis.module.SystemServerDisplayEnvironmentInstallerMutationPolicyTest`
  - `./gradlew :app:testCompat100DebugUnitTest --tests com.dpis.module.SystemServerDisplayEnvironmentInstallerMutationPolicyTest`
- Wave 2 started.
- Added `SystemServerMutationField` and routed viewport/font writes through
  `SystemServerMutationPolicy.shouldApplyMutationField(...)` while preserving
  existing field coverage.
- Wave 2 verification passed:
  - `./gradlew :app:testModern101DebugUnitTest --tests com.dpis.module.SystemServerDisplayEnvironmentInstallerMutationPolicyTest`
  - `./gradlew :app:testCompat100DebugUnitTest --tests com.dpis.module.SystemServerDisplayEnvironmentInstallerMutationPolicyTest`
- Wave 3 started.
- Narrowed system_server `FONT_SCALE` mutation to `launch-activity-item` while
  preserving viewport multi-entry scheduling.
- Updated modern101 and compat100 route docs to record that the relaunch
  mitigation now lives in DPIS field scheduling rather than per-app user
  disablement guidance.
- Wave 3 verification passed:
  - `./gradlew :app:testModern101DebugUnitTest --tests com.dpis.module.SystemServerDisplayEnvironmentInstallerMutationPolicyTest`
  - `./gradlew :app:testCompat100DebugUnitTest --tests com.dpis.module.SystemServerDisplayEnvironmentInstallerMutationPolicyTest`
  - `./gradlew :app:testModern101DebugUnitTest --tests com.dpis.module.SystemServerDisplayEnvironmentInstallerMutationPolicyTest --tests com.dpis.module.HookExecutionPlannerTest --tests com.dpis.module.HookDomainPlanTest --tests com.dpis.module.FontHookDomainPropertyBridgeTest --tests com.dpis.module.HookDomainOverrideStoreTest --tests com.dpis.module.MainActivitySourceSmokeTest --tests com.dpis.module.AppConfigDialogBinderSourceSmokeTest`
  - `./gradlew :app:testCompat100DebugUnitTest --tests com.dpis.module.SystemServerDisplayEnvironmentInstallerMutationPolicyTest --tests com.dpis.module.HookExecutionPlannerTest --tests com.dpis.module.HookDomainPlanTest --tests com.dpis.module.FontHookDomainPropertyBridgeTest --tests com.dpis.module.HookDomainOverrideStoreTest --tests com.dpis.module.MainActivitySourceSmokeTest --tests com.dpis.module.AppConfigDialogBinderSourceSmokeTest`
- Wave 4 completed as a naming audit.
- `SystemServerMutationField`, `FONT_SCALE`, `VIEWPORT`, and
  `shouldApplyMutationField` now provide the intended grep path for scheduler
  changes.
- Deferred broad renames of existing viewport variables such as
  `preEnvironment` and `effectiveEnvironment`; those names describe older
  viewport computation state and changing them now would widen the refactor
  beyond the approved field scheduling boundary.
- Static review found a hook-domain mask compatibility regression plus minor
  help-text and EOF issues.
- Review fix keeps the legacy v2 mask order for the original customizable
  domains, appends `activity_thread_font` and `system_server_font`, updates
  custom-hook help text to 10 routes, and removes extra planning EOF blanks.
- Added explicit docs and comments for the requested-domain versus effective
  scheduler boundary. Bilibili/Douyin are recorded as reproduction evidence for
  generic `FONT_SCALE` scheduling, not as built-in recommended hook-chain
  targets.
