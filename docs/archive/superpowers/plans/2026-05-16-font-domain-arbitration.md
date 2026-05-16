# Font Domain Arbitration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rework `field_rewrite` font handling into a rendering-domain plan that enables Resources font scaling for mixed apps while preventing double scaling.

**Architecture:** `FontHookArbitration` produces a `FontDomainPlan` with explicit domain and unit semantics. `AppProcessHookInstaller` consumes that plan to install Resources, WebView, TextView, and Paint hooks independently. `ForceTextSizeHookInstaller` uses the plan to skip SP rewrites when Resources/scaledDensity already owns SP semantics.

**Tech Stack:** Java 17, Android/Xposed hooks, JUnit4, Gradle modern101 unit tests.

---

### Task 1: Domain Plan Model

**Files:**
- Modify: `app/src/main/java/com/dpis/module/FontHookArbitration.java`
- Create: `app/src/test/java/com/dpis/module/FontHookArbitrationTest.java`

- [ ] **Step 1: Write tests for the default `field_rewrite` plan**

Add tests asserting that `field_rewrite` enables Resources, WebView, and TextView, disables TextView SP rewrite, keeps absolute TextView rewrite, and disables Paint fallback.

- [ ] **Step 2: Implement `FontDomainPlan`**

Replace fallback-only fields with explicit domain fields:
`resourcesFontEnabled`, `webViewTextZoomEnabled`, `textViewHooksEnabled`,
`textViewSpRewriteEnabled`, `textViewAbsoluteRewriteEnabled`, and
`paintFallbackEnabled`.

- [ ] **Step 3: Preserve disabled behavior**

When field rewrite is disabled, all domain fields must be false and the reason
must explain `field-rewrite-disabled`.

### Task 2: Installer Wiring

**Files:**
- Modify: `app/src/main/java/com/dpis/module/AppProcessHookInstaller.java`
- Modify: `app/src/test/java/com/dpis/module/AppProcessHookInstallerTest.java`

- [ ] **Step 1: Add tests for Resources font installation eligibility**

Assert that `FIELD_REWRITE + fontScaleActive` makes Resources hooks eligible
even when viewport replacement and system emulation are both off.

- [ ] **Step 2: Split WebView from TextView**

Use `webViewTextZoomEnabled` for `WebViewFontHookInstaller.install(...)` and
`textViewHooksEnabled` for `ForceTextSizeHookInstaller.install(...)`.

- [ ] **Step 3: Keep ActivityThread font emulation separate**

Only `SYSTEM_EMULATION` should install `ActivityThreadFontHookInstaller`.
`FIELD_REWRITE` Resources font hooks are process-local field reads/writes, not
system emulation.

### Task 3: TextView Unit Semantics

**Files:**
- Modify: `app/src/main/java/com/dpis/module/ForceTextSizeHookInstaller.java`
- Modify or create focused tests under `app/src/test/java/com/dpis/module/`

- [ ] **Step 1: Make SP rewrite plan-controlled**

Skip `TextView.setTextSize(... SP ...)` and `setTextSize(float)` multiplication
when `textViewSpRewriteEnabled` is false.

- [ ] **Step 2: Preserve absolute TextView gap filling**

Keep PX and non-SP handling when `textViewAbsoluteRewriteEnabled` is true.
Do not route this through Paint fallback.

- [ ] **Step 3: Document Paint suppression at the call site**

Add a concise comment explaining that Paint/TextPaint fallback is disabled when
clearer domain primaries exist because it cannot reliably know whether incoming
sizes were already scaled.

### Task 4: Verification

**Files:**
- No production changes expected beyond Tasks 1-3.

- [ ] **Step 1: Run targeted unit tests**

Run:
`./gradlew :app:testModern101DebugUnitTest --tests com.dpis.module.FontHookArbitrationTest --tests com.dpis.module.AppProcessHookInstallerTest`

- [ ] **Step 2: Run relevant existing font tests**

Run:
`./gradlew :app:testModern101DebugUnitTest --tests com.dpis.module.FontScaleOverrideTest --tests com.dpis.module.ActivityThreadFontHookInstallerTest`

- [ ] **Step 3: Defer device validation**

Device validation is blocked because the test device is temporarily unavailable.
Do not install or reboot until the user says the device is available.
