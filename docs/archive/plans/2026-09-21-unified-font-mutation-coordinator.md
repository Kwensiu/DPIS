# Unified Font Mutation Scheduler Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Stop TextView and Paint field-rewrite from scaling the same px twice, by deepening the existing `FontMutationScheduler` instead of adding a parallel request/engine/coordinator stack.

**Architecture:** One JVM module in `fonts/` owns the pure `decide` function and a ThreadLocal mutation stack. Hook installers stay adapters. `PaintProvenanceTracker` remains a 4-slot **stateful candidate resolver** for a single Paint identity, not a second policy engine. Cross-object delayed Paint writes are recognized only through the same Paint object (`textView.paint` + slots) or the existing layout-stack `strongerDomainOwns` input. Do not add a process-global `(px, factor)` ring, `FontMutationRequest`, `FontMutationDecisionEngine`, or `FontMutationCoordinator`.

**Tech Stack:** Kotlin (Java 17 bytecode), existing JUnit4 tests, `FontFieldRewriteMath` tolerances, `TextViewFontProvenanceTracker`, `PaintProvenanceTracker`, `RuntimeHotPathEvents`.

---

## Product Semantics

These four rules are the work. Package layout is not.

1. One TextView size mutation must not cause a nested or later Paint/TextPaint write of the **same object**'s already-applied target to multiply `factor` again.
2. A genuinely new Paint value still scales, including a **different** object whose unscaled incoming happens to equal another view's applied px (`TextView A: 30 -> 28.2` must not suppress `TextView B` / independent Paint `setTextSize(28.2)`).
3. Stronger Resources/WebView/TextView provenance may `OBSERVE`; a weaker fallback must not overwrite it.
4. Diagnostic outcomes stay distinct: `applied`, `kept`, and `skipped`. Nested Paint during a TextView apply must not increment `applied` a second time.

Do not add package-specific WeChat behavior. WeChat message-screen evidence (`28.2 -> 26.508`) is a regression signature, not a route.

## Why The Previous Split Is Rejected

The current arbitration is already split across `FontMutationScheduler`, `PaintProvenanceTracker.resolveFallback`, `PaintFallbackResolver`, `PaintFallbackModels`, `ForceTextSizeHookRuntime` (`LAST_TARGET_TEXT_SIZES`, depth, stack walk), and installer-local `originalPx * factor`.

Adding `FontMutationRequest` + `DecisionEngine` + `Coordinator` would rename that split. A process-global published-target ring would replace double-scale with a worse mis-scale: common px values such as `28.2` would be treated as already rewritten for every later Paint in the process.

## Already-Applied Recognition Model

Authoritative state is **the current object**, after drift invalidation. Nothing process-global may return `KEEP_CURRENT` / `OBSERVE` / `PASS_THROUGH`.

```text
1. Current-object drift
   PaintProvenanceTracker.invalidateIfDrifted(paint, currentPx)
   runs first. If currentPx is no longer a recorded applied px, that
   slot's applied mark is cleared. New incoming is a new request.

2. Same-object 4-slot provenance
   isKnownApplied(paint, incomingPx, factor) walks all 4 slots.
   Do not add appliedTargetForFactor(paint, factor).

3. In-flight nested write (same thread, stack still active)
   withMutation(targetPx, factor) exposes currentTransactionTarget().
   incoming ~= that target -> PASS_THROUGH original chain args, then
   recordApplied on THIS paint into a slot.

4. Same TextView paint identity
   On TextView APPLY, recordApplied(textView.paint, targetPx, factor)
   when paint is non-null. Later setTextSize on that same object is
   layer 2. A copied TextPaint is a different object.

5. Layout-stack stronger owner (existing heuristic)
   isPaintSizeOwnedByTextLayout remains an installer input. It may
   force OBSERVE for StaticLayout / TextLine / span copies that are
   not the TextView's paint. It is not replaced by a px ring.

6. Forbidden
   Process-level (px, factor) publication must not set alreadyApplied
   and must not change decide(). If a later change wants evidence of
   recent TextView targets, that is diagnostics only.
```

Priority when assembling the `decide` arguments (installer, not `decide` itself):

```text
current object drift / slot invalidation
    > same-object applied provenance          -> alreadyApplied
    > in-flight transaction target            -> transactionTargetPx
    > layout-stack / depth stronger owner     -> strongerDomainOwns
    > never: global published px ring
```

Caller assembly for Paint:

```kotlin
PaintProvenanceTracker.invalidateIfDrifted(paint, currentPx)
val alreadyApplied = PaintProvenanceTracker.isKnownApplied(paint, incoming, factor)
val stronger = ForceTextSizeHookRuntime.isInsideTextViewSetTextSize
val targetPx = PaintProvenanceTracker.resolveScaled(paint, incoming, factor)
var decision = FontMutationScheduler.decide(
    incoming, currentPx, targetPx, factor, stronger,
    FontMutationScheduler.currentTransactionTarget(),
    alreadyApplied,
)
if (decision.action() == FontMutationScheduler.Action.APPLY
    && ForceTextSizeHookRuntime.isPaintSizeOwnedByTextLayout(Thread.currentThread().stackTrace)
) {
    decision = FontMutationScheduler.decide(
        incoming, currentPx, targetPx, factor, true,
        FontMutationScheduler.currentTransactionTarget(),
        alreadyApplied,
    )
}
```

Residual risk (accepted, not papered over with a ring): a layout `TextPaint` copy whose stack does not match `isPaintSizeOwnedByTextLayout` can still double-scale. Fix that heuristic with device evidence, or associate the copy to a TextView paint. Do not match bare px.

Required tests for this model:

- Same Paint after TextView apply of `28.2` at `0.94` → not `APPLY`.
- Different Paint incoming `28.2` at `0.94` with `stronger=false` and empty slots → `APPLY` (`26.508`). This is the collision the ring would have gotten wrong.
- Drift: slot recorded `28.2`, current now `30`, incoming `28.2` → not `KEEP_CURRENT` from stale applied.
- Multi-slot: `reusedPaintKeepsMultipleBaseSizesFromAmplifyingEachOther` stays green.

## Out Of Scope

- Rewriting `TextViewAppearanceHookInstaller`, `TextViewAttachHookInstaller`, or `TextViewSetTextHookInstaller` as a first wave. Helpers they call must delegate to `decide`.
- Moving the rest of `fonts/` or re-litigating `fonts` vs `runtime.font` for unrelated files.
- Async mutation, `CONFIG_FONT_SCALE` writes, or system-mode scheduling.
- A process-global applied-target cache, even as a “hint” that changes `decide`.

## Decision Contract

`decide` is a pure function of its arguments. It must not read ThreadLocal state, Android objects, logs, stores, or any published-px table. Tests and hooks pass `transactionTargetPx` and `alreadyApplied` explicitly.

```text
invalid factor or non-positive sizes -> OBSERVE
transactionTargetPx present and incoming ~= transactionTargetPx -> PASS_THROUGH
strongerDomainOwns -> OBSERVE
alreadyApplied -> if current ~= incoming then KEEP_CURRENT else OBSERVE
current ~= targetPx -> KEEP_CURRENT
incoming ~= targetPx -> OBSERVE
otherwise -> APPLY(targetPx)
```

`targetPx` is supplied by the caller. For Paint, that caller is `PaintProvenanceTracker.resolveScaled`, which is a **stateful candidate resolver**: it may create an entry, match/create a slot, promote, and update base. It is not “storage only”. It must not call `FontMutationScheduler.decide`. `decide` remains the only apply/keep/observe/pass-through policy. Do not split `resolveScaled` into `peek`/`record` in this plan unless a later change needs a read-only peek; keep the existing slot tests.

### Java-facing `decide` (no `@JvmOverloads`)

Two explicit `@JvmStatic` methods. The 5-argument method is the stable Java entry and does not read the mutation stack.

```kotlin
@JvmStatic
fun decide(
    incomingPx: Float,
    currentPx: Float,
    targetPx: Float,
    factor: Float,
    strongerDomainOwns: Boolean,
): Decision = decide(incomingPx, currentPx, targetPx, factor, strongerDomainOwns, null, false)

@JvmStatic
fun decide(
    incomingPx: Float,
    currentPx: Float,
    targetPx: Float,
    factor: Float,
    strongerDomainOwns: Boolean,
    transactionTargetPx: Float?,
    alreadyApplied: Boolean,
): Decision
```

`Decision` keeps `action()` and `targetPx()`, not `getAction()` / `getTargetPx()`. `Action` is a nested enum including `PASS_THROUGH`. Java tests stay in Java so this surface is actually compiled.

### `withMutation` is Kotlin-only

Do not add a Java `Callable` overload or a `MutationBlock` interface. `withMutation` is used by Kotlin hook adapters and by a Kotlin test source.

- Production: `TextViewTextSizeHookInstaller` (Kotlin).
- Tests: add `FontMutationSchedulerStackTest.kt` for stack restore / nested PASS_THROUGH.
- `FontMutationSchedulerTest.java` covers 5-arg/7-arg `decide`, `action()`, `targetPx()`, and overload count only.

### Hook execution and diagnostics

`RuntimeHotPathEvents.kept` already does `calls++` and `kept++`, and is aggregate-only (no timeline/transport record). `applied` writes timeline `applied`. `skipped` writes timeline `skipped` and `calls++`.

| Action | Framework call | Provenance | Aggregate | Timeline |
| --- | --- | --- | --- | --- |
| `APPLY` | override chain args with `decision.targetPx()` | `recordApplied` on hooked object; TextView also `recordApplied(textView.paint, …)` when non-null | `begin` → `calls++`; `applied` → `applied++` | `begin` + `applied` + `end` |
| `KEEP_CURRENT` | do not call the setter (`return null`) | unchanged | `kept` → `calls++`, `kept++` | none |
| `OBSERVE` | `chain.proceed()` original args | no new applied mark | `skipped` when a named route was considered (`reason=…`); silent proceed only for stronger-owner nested unscaled writes during original `chain.proceed()` | `skipped` when `skipped()` is called |
| `PASS_THROUGH` | **`chain.proceed()` original args only**. No `proceed(canonicalTarget)`. Ignore `decision.targetPx()`. | `recordApplied(thisPaint, incomingPx, factor)` into a slot | **`kept`**: `calls++`, `kept++`, `applied += 0` | none (`kept` is aggregate-only; no `mutation_applied`) |

`PASS_THROUGH.targetPx()` is `0f` so a mistaken rewrite is obvious in tests.

`withMutation` is a stack, restored in `finally`.

During the original TextView `proceedInsideTextViewSetTextSize`, nested Paint incoming is often unscaled. That path keeps `strongerDomainOwns = true` via depth. `PASS_THROUGH` is only for incoming that already equals the applied target.

`INTERNAL_UPDATE` may skip Paint rewrite during the PX setter. Same-object recovery is `recordApplied(textView.paint)`, not a px ring.

## Paint installer call-site audit (closed)

Production install of `Paint#setTextSize` is **one** path:

```text
AppProcessHookInstaller
  -> ForceTextSizeHookInstaller.install
  -> ForceTextSizeHookRuntime.install
  -> PaintTextSizeHookInstaller.installPaintTextSizeHooks
```

`PaintTextSizeFallbackHookInstaller.install` has **no production callers**. Live uses of that type:

- `resetForHotReload()` from `AppProcessHotReloadResetter`
- `resolveFieldRewriteFactor` from `ForceTextSizeHookRuntime` and `LegacyModuleHook`

This plan does **not** install the fallback hook. Do not hook `Paint#setTextSize` twice. After `PaintTextSizeHookInstaller` uses `decide`, either delete `PaintTextSizeFallbackHookInstaller.install` or leave the object as reset/factor helpers only. Implementation Task 3 starts with `rg PaintTextSizeFallbackHookInstaller` and fails the task if a new `install(` caller appeared.

## File Map

- Convert: `FontMutationScheduler.java` → `FontMutationScheduler.kt` — two `decide` overloads, `PASS_THROUGH`, Kotlin-only `withMutation`, `resetForHotReload`. No published-target API.
- Modify: `PaintProvenanceTracker.java` — keep `MAX_SLOTS`, `invalidateIfDrifted`, `isKnownApplied`, `resolveScaled`, `recordApplied`. Stop calling `decide` from `resolveFallback` once hooks do not use it.
- Modify: `TextViewFontProvenanceTracker.java` — storage only.
- Modify: `TextViewTextSizeHookInstaller.kt`, `PaintTextSizeHookInstaller.kt`, `ForceTextSizeHookRuntime.kt`.
- Delete after callers are gone: `PaintFallbackResolver.kt`, `PaintFallbackModels.kt`.
- `PaintTextSizeFallbackHookInstaller.kt` — no `install` from this change; keep reset/factor until unused.
- Tests: Java `FontMutationSchedulerTest`, Kotlin `FontMutationSchedulerStackTest`, existing `PaintProvenanceTrackerTest`, paint-fallback tests, regression reference.
- Docs: `docs/font-routing.md`, `docs/modern-runtime-resync.md`, `docs/legacy-runtime-resync.md`.

---

### Task 1: Lock Current Decisions On The Existing Scheduler

**Files:**
- Modify: `app/src/test/java/com/dpis/module/fonts/FontMutationSchedulerTest.java`

- [ ] **Step 1: Keep the three existing 5-argument tests. Add 7-argument tests. Include the collision case that a global ring would fail.**

  ```java
  @Test
  public void nestedIncomingEqualToTransactionTargetPassesThrough() {
      FontMutationScheduler.Decision decision = FontMutationScheduler.decide(
              28.2f, 28.2f, 28.2f, 0.94f, false, 28.2f, false);
      assertEquals(FontMutationScheduler.Action.PASS_THROUGH, decision.action());
      assertEquals(0f, decision.targetPx(), 0.0001f);
  }

  @Test
  public void sameObjectAlreadyAppliedDoesNotApplyAgain() {
      FontMutationScheduler.Decision decision = FontMutationScheduler.decide(
              28.2f, 28.2f, 26.508f, 0.94f, false, null, true);
      assertEquals(FontMutationScheduler.Action.KEEP_CURRENT, decision.action());
  }

  @Test
  public void differentObjectMatchingAnotherViewsTargetStillApplies() {
      FontMutationScheduler.Decision decision = FontMutationScheduler.decide(
              28.2f, 28.2f, 26.508f, 0.94f, false, null, false);
      assertEquals(FontMutationScheduler.Action.APPLY, decision.action());
      assertEquals(26.508f, decision.targetPx(), 0.0001f);
  }

  @Test
  public void independentIncomingStillAppliesScaledTarget() {
      FontMutationScheduler.Decision decision = FontMutationScheduler.decide(
              30f, 30f, 28.2f, 0.94f, false, null, false);
      assertEquals(FontMutationScheduler.Action.APPLY, decision.action());
      assertEquals(28.2f, decision.targetPx(), 0.0001f);
  }
  ```

- [ ] **Step 2: Run baseline tests.**

  ```powershell
  ./gradlew :app:testModernDebugUnitTest --tests com.dpis.module.fonts.FontMutationSchedulerTest --tests com.dpis.module.fonts.PaintProvenanceTrackerTest --tests com.dpis.module.fonts.ForceTextSizeHookInstallerPaintFallbackTest
  ```

  Expected: historical 5-arg tests PASS; new tests fail to compile; `reusedPaintKeepsMultipleBaseSizesFromAmplifyingEachOther` PASS.

---

### Task 2: Deepen FontMutationScheduler In Place

**Files:**
- Create: `app/src/main/java/com/dpis/module/fonts/FontMutationScheduler.kt`
- Delete: `app/src/main/java/com/dpis/module/fonts/FontMutationScheduler.java`
- Create: `app/src/test/java/com/dpis/module/fonts/FontMutationSchedulerStackTest.kt`

- [ ] **Step 1: Convert to Kotlin with two explicit `decide` overloads and Kotlin-only `withMutation`. No `@JvmOverloads`. No `publishAppliedTarget`.**

  ```kotlin
  object FontMutationScheduler {
      enum class Action { KEEP_CURRENT, APPLY, OBSERVE, PASS_THROUGH }

      class Decision internal constructor(
          private val actionValue: Action,
          private val targetPxValue: Float,
      ) {
          fun action(): Action = actionValue
          fun targetPx(): Float = targetPxValue
      }

      private data class Frame(val targetPx: Float, val factor: Float)
      private val stack: ThreadLocal<ArrayDeque<Frame>> =
          ThreadLocal.withInitial { ArrayDeque() }

      @JvmStatic
      fun decide(
          incomingPx: Float,
          currentPx: Float,
          targetPx: Float,
          factor: Float,
          strongerDomainOwns: Boolean,
      ): Decision = decide(
          incomingPx, currentPx, targetPx, factor, strongerDomainOwns, null, false,
      )

      @JvmStatic
      fun decide(
          incomingPx: Float,
          currentPx: Float,
          targetPx: Float,
          factor: Float,
          strongerDomainOwns: Boolean,
          transactionTargetPx: Float?,
          alreadyApplied: Boolean,
      ): Decision {
          if (strongerDomainOwns || !isScaleFactorActive(factor)
              || incomingPx <= 0f || currentPx <= 0f || targetPx <= 0f
          ) {
              return Decision(Action.OBSERVE, 0f)
          }
          if (transactionTargetPx != null
              && FontFieldRewriteMath.approximatelyEqual(incomingPx, transactionTargetPx)
          ) {
              return Decision(Action.PASS_THROUGH, 0f)
          }
          if (alreadyApplied) {
              return if (FontFieldRewriteMath.approximatelyEqual(currentPx, incomingPx)) {
                  Decision(Action.KEEP_CURRENT, 0f)
              } else {
                  Decision(Action.OBSERVE, 0f)
              }
          }
          if (FontFieldRewriteMath.approximatelyEqual(currentPx, targetPx)) {
              return Decision(Action.KEEP_CURRENT, 0f)
          }
          if (FontFieldRewriteMath.approximatelyEqual(incomingPx, targetPx)) {
              return Decision(Action.OBSERVE, 0f)
          }
          return Decision(Action.APPLY, targetPx)
      }

      fun currentTransactionTarget(): Float? = stack.get().lastOrNull()?.targetPx

      fun <T> withMutation(targetPx: Float, factor: Float, block: () -> T): T {
          val frames = stack.get()
          frames.addLast(Frame(targetPx, factor))
          return try {
              block()
          } finally {
              frames.removeLast()
              if (frames.isEmpty()) {
                  stack.remove()
              }
          }
      }

      @JvmStatic
      fun resetForHotReload() {
          stack.remove()
      }

      private fun isScaleFactorActive(factor: Float): Boolean =
          factor > 0f && factor != 1.0f
  }
  ```

- [ ] **Step 2: Java test proves `action()` / `targetPx()` and exactly two `decide` methods.**

  ```java
  @Test
  public void javaCallersUseActionAndTargetPxMethods() {
      FontMutationScheduler.Decision decision = FontMutationScheduler.decide(
              18f, 18f, 16.74f, 0.93f, false);
      assertEquals(FontMutationScheduler.Action.APPLY, decision.action());
      assertEquals(16.74f, decision.targetPx(), 0.0001f);
      long decideCount = java.util.Arrays.stream(FontMutationScheduler.class.getDeclaredMethods())
              .filter(method -> "decide".equals(method.getName()))
              .count();
      assertEquals(2L, decideCount);
  }
  ```

- [ ] **Step 3: Kotlin stack tests. Do not call `withMutation` from Java.**

  ```kotlin
  @Test
  fun withMutationExposesTargetToNestedDecideAndRestoresAfterward() {
      val nested = FontMutationScheduler.withMutation(28.2f, 0.94f) {
          FontMutationScheduler.decide(
              28.2f, 28.2f, 28.2f, 0.94f, false,
              FontMutationScheduler.currentTransactionTarget(),
              false,
          )
      }
      assertEquals(FontMutationScheduler.Action.PASS_THROUGH, nested.action())
      assertEquals(null, FontMutationScheduler.currentTransactionTarget())
  }

  @Test
  fun withMutationRestoresStackWhenBlockThrows() {
      try {
          FontMutationScheduler.withMutation(28.2f, 0.94f) {
              throw IllegalStateException("setter")
          }
      } catch (_: IllegalStateException) {
      }
      assertEquals(null, FontMutationScheduler.currentTransactionTarget())
  }

  @Test
  fun nestedIncomingDifferentFromTransactionTargetIsNotPassThrough() {
      val nested = FontMutationScheduler.withMutation(28.2f, 0.94f) {
          FontMutationScheduler.decide(
              30f, 30f, 28.2f, 0.94f, false,
              FontMutationScheduler.currentTransactionTarget(),
              false,
          )
      }
      assertEquals(FontMutationScheduler.Action.APPLY, nested.action())
      assertEquals(28.2f, nested.targetPx(), 0.0001f)
  }
  ```

- [ ] **Step 4: Tracker tests for drift vs slot vs different object.**

  Add to `PaintProvenanceTrackerTest` (or a small Kotlin test next to it):

  ```java
  @Test
  public void differentPaintDoesNotInheritAnotherPaintsAppliedTarget() {
      Object textViewPaint = new Object();
      Object otherPaint = new Object();
      PaintProvenanceTracker.recordApplied(textViewPaint, 28.2f, 0.94f);
      assertTrue(PaintProvenanceTracker.isKnownApplied(textViewPaint, 28.2f, 0.94f));
      assertFalse(PaintProvenanceTracker.isKnownApplied(otherPaint, 28.2f, 0.94f));
  }

  @Test
  public void driftedCurrentSizeClearsAppliedMark() {
      Object paint = new Object();
      PaintProvenanceTracker.recordApplied(paint, 28.2f, 0.94f);
      PaintProvenanceTracker.invalidateIfDrifted(paint, 30f);
      assertFalse(PaintProvenanceTracker.isKnownApplied(paint, 28.2f, 0.94f));
  }
  ```

- [ ] **Step 5: Run tests.**

  ```powershell
  ./gradlew :app:testModernDebugUnitTest --tests com.dpis.module.fonts.FontMutationSchedulerTest --tests com.dpis.module.fonts.FontMutationSchedulerStackTest --tests com.dpis.module.fonts.PaintProvenanceTrackerTest
  ```

  Expected: PASS.

---

### Task 3: Route TextView setTextSize And The Live Paint Installer Through The Scheduler

**Files:**
- Modify: `TextViewTextSizeHookInstaller.kt`, `PaintTextSizeHookInstaller.kt`, `PaintProvenanceTracker.java`, `ForceTextSizeHookRuntime.kt`
- Audit: `PaintTextSizeFallbackHookInstaller.kt`

- [ ] **Step 1: Re-run the call-site audit before editing installers.**

  Search production sources for `PaintTextSizeFallbackHookInstaller.install(`. Expected: zero matches outside `PaintTextSizeFallbackHookInstaller.kt` itself. If a caller exists, stop and update this plan; do not hook `setTextSize` twice.

- [ ] **Step 2: TextView PX rewrite uses `withMutation`. Record the TextView paint, not a global px.**

  Keep `proceedInsideTextViewSetTextSize` as stronger-owner for unscaled nested paints.

  On `APPLY`:

  ```kotlin
  FontMutationScheduler.withMutation(decision.targetPx(), factor) {
      ForceTextSizeHookRuntime.INTERNAL_UPDATE.set(true)
      try {
          thisObject.setTextSize(TypedValue.COMPLEX_UNIT_PX, decision.targetPx())
          thisObject.paint?.let { paint ->
              PaintProvenanceTracker.recordApplied(paint, decision.targetPx(), factor)
          }
          // existing TextView provenance / rewrite bookkeeping
      } finally {
          ForceTextSizeHookRuntime.INTERNAL_UPDATE.remove()
      }
  }
  ```

  No `publishAppliedTarget`.

- [ ] **Step 3: Live Paint installer maps actions as in the diagnostics table. `PASS_THROUGH` calls `RuntimeHotPathEvents.kept` then `chain.proceed()`.**

  Defer `isPaintSizeOwnedByTextLayout` until a write is being considered.

- [ ] **Step 4: Stop `resolveFallback` from calling `decide` once unused. Keep `resolveScaled` as the stateful candidate resolver.**

- [ ] **Step 5: PASS_THROUGH aggregate test — not a hand-rolled `int[]`.**

  Drive `RuntimeHotPathEvents.kept` / `applied` through the existing performance snapshot used by `ProcessPerformanceTest`, or a narrow helper that records the same four counters:

  ```text
  after TextView APPLY + nested PASS_THROUGH:
    textview route applied == 1
    paint route applied == 0
    paint route kept >= 1
    paint route calls >= kept
  ```

  Source smoke: Paint `PASS_THROUGH` / `OBSERVE` branches do not call `RuntimeHotPathEvents.applied`. One assertion.

- [ ] **Step 6: Run focused tests.**

  ```powershell
  ./gradlew :app:testAllDebugUnitTests --tests com.dpis.module.fonts.FontMutationSchedulerTest --tests com.dpis.module.fonts.FontMutationSchedulerStackTest --tests com.dpis.module.fonts.PaintProvenanceTrackerTest --tests com.dpis.module.fonts.ForceTextSizeHookInstallerPaintFallbackTest --tests com.dpis.module.ForceTextSizeRegressionReferenceTest --tests com.dpis.module.diagnostics.ProcessPerformanceTest
  ```

---

### Task 4: Delete The Shallow Paint Fallback Layer

- Delete `PaintFallbackResolver.kt` and `PaintFallbackModels.kt` after `rg` shows no production callers.
- Rewrite `ForceTextSizeHookInstallerPaintFallbackTest` onto `decide` + `isKnownApplied`.
- Drop `PaintFallbackResolver.resolve(` source-shape assertions.
- Leave `PaintTextSizeFallbackHookInstaller` without an install caller. If `install` is unused, delete that method in this task; keep `resolveFieldRewriteFactor` / `resetForHotReload` while they still have callers.

  ```powershell
  ./gradlew :app:testAllDebugUnitTests --tests '*Font*Test' --tests '*PaintFallback*Test' --tests '*HotReload*Test' --tests com.dpis.module.fonts.PaintProvenanceTrackerTest
  ```

---

### Task 5: Document And Verify

- `docs/font-routing.md`: three behavioral layers (slots, in-flight stack, layout-stack stronger owner). Explicitly reject process-global px publication as a rewrite gate.
- Dated Modern/Legacy route-detail entries. Keep `28.2 -> 26.508` as the regression signature, and record that a later independent `28.2` must still rewrite.
- `./gradlew :app:assembleModernDebug :app:assembleLegacyDebug`
- Install Modern Debug without Studio deployment optimization. Restart with an explicit launcher intent. No `monkey`.
- WeChat message-screen session while the page stays visible:

  - no new `28.2 -> 26.508` on the **same** paint identity / layout-stack owned copy
  - independent Paint values, including a later `28.2` that is not that object’s applied slot, still scale once
  - one `applied` for the TextView mutation; nested Paint is `kept` in aggregate, not a second `applied`
  - no unexpected `CONFIG_FONT_SCALE` / Activity relaunch

- Pre-commit review skill before any commit. Do not commit until the user asks.

---

## Follow-Up (Not This Plan)

Appearance, attach, and setText installers. Do not split `ForceTextSizeHookRuntime` wholesale. Do not introduce a published-px ring later without an object or generation association.

## Self-Review

- Different-object `28.2` still `APPLY`s; the global ring is gone.
- Drift invalidation outranks same-object applied marks.
- Paint 4-slot provenance is retained.
- `resolveScaled` is named as a stateful candidate resolver.
- `PASS_THROUGH` uses original `chain.proceed()`, `kept` aggregates, no second `applied`.
- Fallback `install` is audited as unused; live hook is `PaintTextSizeHookInstaller` only.
- `withMutation` is Kotlin-only.
- Java sees two `decide` methods, `action()`, and `targetPx()`.
- No WeChat-specific code path.
- No commit is part of this document.
