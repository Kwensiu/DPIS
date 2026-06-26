# WebAPK Owner Routing Design

Date: 2026-06-26

## Goal

Add a first-class DPIS runtime routing design for Chrome Android WebAPKs so a
configured WebAPK package can drive DPIS runtime behavior even when the final
visible page is carried by Chrome.

The immediate goal is not "support every PWA runtime path". The immediate goal
is narrower:

- preserve WebAPK as the user-facing configured package;
- let system_server routes select WebAPK config when Chrome is only the runtime
  carrier for that launch;
- keep Chrome ordinary tabs and unrelated Chrome activity launches outside that
  WebAPK config.

## Non-Goals

- Do not treat `com.android.chrome` as a global alias of every
  `org.chromium.webapk.*` package.
- Do not introduce app-specific hardcoded handling for one WebAPK hash package
  or one website.
- Do not promise a full Legacy app-process WebAPK carrier route in this phase.
- Do not add a new DPIS UI model such as a separate "carrier app" config page.
- Do not change package storage semantics away from package-keyed config.

## Product Semantics

This feature introduces an explicit distinction between two roles:

- `WebAPK owner package`: the configured app identity, usually
  `org.chromium.webapk.*`
- `Runtime carrier package`: the package that actually executes a given launch
  stage, for example `com.android.chrome`

In WebAPK launches, DPIS should reason about them this way:

1. The user configures the WebAPK package.
2. WebAPK splash/runtime bootstrap may run inside the WebAPK package itself.
3. The final web content activity may run inside Chrome.
4. When DPIS can prove that the current Chrome launch belongs to one WebAPK,
   DPIS may use the WebAPK package as the config owner for that launch.

This is a launch-scoped routing rule, not a package alias rule.

## Evidence

Observed runtime evidence on device:

- WebAPK packages are real Android packages such as
  `org.chromium.webapk.a5e359e2ce8b830bb_v2`.
- Launch begins through WebAPK shell activities such as
  `org.chromium.webapk.shell_apk.h2o.H2OOpaqueMainActivity` and splash routes.
- Final foreground content activity is carried by
  `com.android.chrome/org.chromium.chrome.browser.webapps.SameTaskWebApkActivity`.
- Chrome launch intents include extras such as:
  `org.chromium.chrome.browser.webapk_package_name=<webapk package>`.

Observed DPIS evidence:

- App-process hooks can install inside the WebAPK package and mutate splash
  resources correctly.
- Final page content is still carried by Chrome activity/resources, so WebAPK
  package-only routing is not sufficient for the final visible page.
- Current system_server candidate resolution already supports fallback from one
  primary package to another configured package discovered from candidate text.

## Current Code Boundaries

### Package config truth

DPIS config remains package keyed. See
`docs/adr/0001-package-config-source-of-truth.md`.

This design does not change package storage or package ownership.

### system_server package resolution

Current package selection happens in:

- `SystemServerDisplayEnvironmentInstaller.resolveConfiguredPackage(...)`

Current behavior:

- resolve a primary package from objects/fields/methods;
- collect text package candidates from args/object summaries;
- if the primary package has no config, fall back to the first configured
  candidate package.

This is the correct layer for WebAPK owner routing in phase 1.

### app-process package resolution

Current app-process planning resolves package config from the current process or
package:

- `ModuleMain.maybeInstallAppProcessFromModuleLoaded(...)`
- `ModuleMain.installAppProcessHooksIfConfigured(...)`
- `ModulePackagePlan.resolve(...)`

This path is intentionally package-pure today. It should remain package-pure in
phase 1.

## Design

## Phase 1: system_server WebAPK owner routing

### Intent

Allow system_server routes to select a configured WebAPK owner package when the
launch is visibly carried by Chrome.

### Core rule

When a system_server entry can prove that a Chrome activity launch belongs to a
specific WebAPK, use the WebAPK package for config lookup while keeping Chrome
as the runtime carrier.

### Implementation shape

Add a shared helper such as:

- `WebApkCarrierResolver`

Responsibilities:

1. Detect whether a launch object/intent/extras represent a Chrome-carried
   WebAPK launch.
2. Extract the WebAPK owner package when present.
3. Validate that the extracted package looks like a real WebAPK owner package.

Recommended stable signals:

- owner package prefix: `org.chromium.webapk.`
- Chrome extra:
  `org.chromium.chrome.browser.webapk_package_name`
- WebAPK deep link data:
  `webapp://webapk-<package>`
- Chrome carrier components:
  - `org.chromium.chrome.browser.webapps.SameTaskWebApkActivity`
  - `org.chromium.chrome.browser.webapps.WebappLauncherActivity`

The resolver should be conservative:

- if proof is missing, return no owner package;
- never guess a WebAPK owner from Chrome package name alone;
- never scan installed packages to infer the current launch owner.

### Resolver integration

Integrate owner resolution before or during candidate package collection in
`SystemServerDisplayEnvironmentInstaller.resolveConfiguredPackage(...)`.

Recommended semantics:

1. Resolve `primaryPackage` as today.
2. Ask `WebApkCarrierResolver` for an owner package from the current
   `self/args/intent` context.
3. If present, insert the owner package into the candidate set with a stronger
   rank than generic text-package extraction.
4. Preserve `primaryPackage` as the carrier/fallback source in diagnostics.

The selected package in `ResolvedPackage.packageName` remains the config owner.

### Scope

Phase 1 applies only where current entry objects can carry sufficient evidence:

- `activity-start`
- `config-dispatch`
- other entries only if object/intent evidence is present and proven by tests

Do not force owner routing into `launch-activity-item` unless real framework
objects at that entry expose stable WebAPK owner evidence.

## Phase 2: modern app-process carrier support

### Intent

Only after phase 1 proves useful on real devices, consider a modern-only
runtime-target bridge so Chrome app-process hooks can use a WebAPK owner config
for the current launch.

### Why it is separate

Modern app-process entry currently knows only:

- process/package name at module-loaded time
- package-ready package/classloader

That is not enough by itself to safely distinguish:

- a normal Chrome tab
- a Chrome PWA page launched for one WebAPK
- a different WebAPK carried by the same Chrome process

Therefore phase 2 requires an explicit owner bridge, not silent aliasing.

### Candidate direction

Possible future direction:

1. system_server proves WebAPK owner at launch.
2. DPIS publishes a bounded runtime owner marker for the current Chrome launch.
3. modern Chrome app-process hooks read that owner marker and resolve config
   through the owner package for the current task/activity only.

This is explicitly out of scope for phase 1.

## Legacy Scope

Legacy should not promise the same WebAPK carrier behavior in this phase.

Reasons:

- Legacy system_server route is narrower and does not install the shared modern
  config-dispatch/display-manager-info coverage.
- Legacy launch-time objects may not expose enough stable owner evidence for a
  safe owner/carrier split.
- Legacy app-process routing is intentionally package/process pure.

Allowed phase-1 Legacy behavior:

- shared helper compiles under Legacy shared code;
- WebAPK package itself may still receive its own splash-layer app-process
  hooks;
- no claim that Chrome content activity becomes WebAPK-owner-aware under
  Legacy.

## Diagnostics

Diagnostics must explain owner/carrier separation directly.

Recommended logs:

- owner detected:
  `webapk owner resolved: carrier=com.android.chrome, owner=org.chromium.webapk...`
- config fallback:
  `system_server config fallback: entry=..., fromPackage=com.android.chrome, selectedPackage=org.chromium.webapk..., targetCandidates=com.android.chrome|org.chromium.webapk...`

Do not log WebAPK owner routing as if Chrome itself were the configured target.

If the owner evidence exists but the owner package has no config, log that
plainly. Do not silently rewrite to Chrome config unless that is the explicit
fallback policy for the entry.

## Tests

### New unit tests

- `WebApkCarrierResolverTest`
  - extracts owner from Chrome extras
  - extracts owner from `webapp://webapk-...`
  - rejects non-WebAPK prefixes
  - rejects empty or malformed owner strings

### Extend system_server tests

- `SystemServerDisplayDiagnosticsTest`
  - primary package is `com.android.chrome`
  - WebAPK owner package is present in extras
  - configured package resolution selects the WebAPK owner
  - fallback/config-miss logs preserve Chrome as the carrier source

- `SystemServerDisplayEnvironmentInstallerMutationPolicyTest`
  - owner routing does not change field policy
  - `FONT_SCALE` remains launch-only
  - viewport remains multi-entry

### Hot-entry gating tests

If hot-entry quick gates depend on configured package hints or candidate text,
add tests so WebAPK-owner launches are not prematurely skipped when the visible
carrier text is Chrome-centric.

### Guard app-process purity

Add a test that phase 1 does not silently make app-process `com.android.chrome`
resolve a WebAPK owner package without an explicit owner bridge.

## Real Device Verification

For the GitHub WebAPK case:

1. Add LSPosed scope to WebAPK and Chrome as needed for the tested route.
2. Configure only the WebAPK package in DPIS.
3. Launch the WebAPK from launcher.
4. Verify:
   - system_server logs show owner resolution or owner-based config fallback;
   - Chrome `SameTaskWebApkActivity` launch is associated with the WebAPK owner;
   - ordinary Chrome tab launches do not receive the WebAPK config.

## Rejected Approaches

### Global Chrome alias

Rejected: automatically apply every WebAPK config to `com.android.chrome`.

Why:

- multiple WebAPKs share the same carrier;
- ordinary Chrome tabs would be polluted;
- package ownership semantics would become ambiguous.

### Hardcoded package or site list

Rejected: treat one WebAPK hash package or one site like `github.com` as a
special route.

Why:

- this is a generic Chrome WebAPK carrier pattern;
- hash packages are instance-level outputs, not product semantics.

### Launch-item guessing

Rejected for phase 1: infer owner package from `launch-activity-item` without a
proven owner field/extra.

Why:

- guessing at that entry creates silent misrouting;
- phase 1 should require direct owner evidence.

## Documentation Follow-up

If implemented:

- add a new ADR that defines `owner package` and `runtime carrier package`;
- update `docs/modern-runtime-resync.md` with the system_server WebAPK owner
  route;
- update `docs/legacy-runtime-resync.md` to state the unsupported or
  partial-support boundary clearly.
