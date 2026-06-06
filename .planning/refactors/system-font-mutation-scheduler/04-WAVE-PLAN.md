# System Font Mutation Scheduler Wave Plan

## Mode

Untangle-first.

## Safe Boundary

Only system_server mutation scheduling semantics. Do not redesign the hook-chain
dialog, app-process font hooks, or viewport calculation algorithm in this
refactor package.

## Wave 1: Behavior Lock And Vocabulary

Goal: make the existing semantics searchable and testable before moving code.

- Add tests that prove viewport mutation still allows lifecycle multi-point
  application.
- Add tests that prove fontScale is a distinct mutation field from viewport.
- Add TODO comments near the current mixed mutation method documenting the
  intended scheduler shape:
  `MutationField`, baseline policy, allowed entries, relaunch risk.
- No runtime behavior change.

Verification:

- `./gradlew :app:testModern101DebugUnitTest --tests com.dpis.module.SystemServerDisplayEnvironmentInstallerMutationPolicyTest`
- `./gradlew :app:testCompat100DebugUnitTest --tests com.dpis.module.SystemServerDisplayEnvironmentInstallerMutationPolicyTest`

## Wave 2: Extract Field-Level Policy

Goal: move from entry-only policy to field-aware policy without changing
behavior.

- Introduce a small internal policy shape for system mutations.
- Keep current viewport allowed entries unchanged.
- Keep current font allowed entries unchanged in this wave.
- Route `applyEnvironment` through the policy so future changes are localized.

Verification:

- Targeted policy tests.
- Existing hook execution planner tests.
- Existing source smoke tests that cover hook domains.

## Wave 3: Narrow Font System Mutation

Goal: resolve the flicker root cause through DPIS scheduling, not user-facing
manual warnings.

- Make `FONT_SCALE` avoid config-dispatch style relaunch-prone writes.
- Prefer launch-time or app-process fallback for font where system relaunch risk
  is known.
- Keep `system_server_font` as an explicit domain, but have unified scheduling
  decide whether and where it is allowed to write.
- Update route docs with the rationale.

Verification:

- Unit tests for allowed entries.
- Install modern101 debug APK.
- Reboot to reload system_server LSPosed code.
- Reproduce Bilibili/Douyin gestures and verify logs do not show
  `CONFIG_FONT_SCALE` relaunch caused by DPIS font mutation.

## Wave 4: Naming Cleanup

Goal: make future changes grep-able.

- Rename only inside the approved boundary.
- Prefer terms:
  - `MutationField.VIEWPORT`
  - `MutationField.FONT_SCALE`
  - `sourceSnapshot`
  - `targetViewportEnvironment`
  - `effectiveViewportEnvironment`
  - `targetFontScale`
- Avoid broad file splitting unless Wave 2 shows a clean seam.

