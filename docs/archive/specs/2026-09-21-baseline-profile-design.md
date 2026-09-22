# Baseline Profile Ship, Measure, And Workspace Journeys

Date: 2026-09-21

## Goal

Make DPIS Baseline Profiles actually reach release APKs, record them from
locale-stable UI, measure that they help cold start and Apps list frames, and
cover the five main workspace destinations. Optimize only the DPIS manager
process (`io.github.kwensiu.dpis`), never hooked target apps.

## Non-Goals

- Do not merge, split, or add `app/src/test`, `app/src/testLegacy`, or
  `app/src/androidTest`.
- Do not add `connectedAndroidTest` or profile generation to GitHub Actions.
- Do not enable `automaticGenerationDuringBuild` (CI has no API 33+ device).
- Do not record app-config editors, font library, About, or Wear-only flows.
  Those depend on installed packages or a compact round window.
- Do not treat Baseline Profiles as a substitute for JVM unit tests.

## Current State

`:baselineprofile` already exists. `DpisBaselineProfileGenerator` lives under
`baselineprofile/src/androidTest`, hard-codes package name
`io.github.kwensiu.dpis`, and taps/swipes pixel coordinates. It sets
`includeInStartupProfile = true` on that mixed journey.

`:app` applies `androidx.baselineprofile` and depends on
`baselineProfile(project(":baselineprofile"))`, but:

- there is no `androidx.profileinstaller` library dependency
- no `baseline-prof.txt` / `startup-prof.txt` is committed
- release builds do not generate profiles
- there is no Macrobenchmark that compares compilation modes

Workspace navigation already sets translated `contentDescription` labels. There
is no locale-independent `testTag`. `ComposeDesignSystem` does not set
`testTagsAsResourceId`, so UiAutomator cannot see Compose test tags as
resource ids.

Startup overlays that can cover navigation:

- disclaimer: tags `startup-disclaimer-agreement` and `startup-disclaimer-accept`
- module reload notice: acknowledge button in `RuntimeReloadNoticeContent` has
  no test tag

## Terminology

**Baseline Profile**

A list of hot classes and methods packaged with the APK. ART AOT-compiles them
after install so first-session code is not only JIT.

**Startup Profile**

A stricter subset used for DEX layout (`includeInStartupProfile = true`). Only
the true cold-start path belongs here.

**Manager process**

The DPIS UI APK. Sideloaded LSPosed modules usually never receive Play Cloud
Profiles, so the in-APK profile is the day-0 path.

## Design

### 1. Locale-stable navigation tags

Add `testTag` on `WorkspaceDestination`:

| Destination | Tag                      |
|-------------|--------------------------|
| APP         | `workspace-nav-app`      |
| TEMPLATE    | `workspace-nav-template` |
| HOME        | `workspace-nav-home`     |
| TOOLS       | `workspace-nav-tools`    |
| SETTINGS    | `workspace-nav-settings` |

Apply `Modifier.testTag(destination.testTag)` on every navigation item:

- `NavigationBarItem`
- `NavigationRailItem`
- `NavigationDrawerItem`
- Wear expanded destination `WearButton`

Wear collapsed opener uses `workspace-nav-menu` only so a watch capture can
open the list. Phone generation does not require it.

`ComposeDesignSystem` wraps content with
`Modifier.semantics { testTagsAsResourceId = true }` so UiAutomator `By.res(tag)`
resolves Compose test tags. Dialogs hosted through that root inherit the flag.

Reload-notice acknowledge button gets `runtime-reload-notice-ack`.

### 2. Generator module layout

Move generators into `baselineprofile/src/main/java/...` (the
`com.android.test` main source set). Delete the androidTest copy.

Apply `org.jetbrains.kotlin.android` on `:baselineprofile` if it is not already
applied.

Inject `targetAppId` from the tested APK, matching the InstallerX-Revived
`androidComponents` pattern. Never hard-code the application id.

Shared UiAutomator steps live in one helper (`DpisProfileJourneys`), not in
each `@Test`.

Assumptions for generation: default workspace visibility (all five destinations
shown) and a phone-sized window (bottom bar, rail, or drawer). Hidden-workspace
settings are not a generation matrix.

### 3. Journeys

| Test       | Interaction                                           | `includeInStartupProfile` |
|------------|-------------------------------------------------------|---------------------------|
| Cold start | `pressHome`, `startActivityAndWait`, dismiss overlays | true                      |
| Apps       | open Apps, swipe the list                             | false                     |
| Templates  | open Templates, swipe the list                        | false                     |
| Settings   | open Settings, swipe one screen                       | false                     |
| Tools      | open Tools                                            | false                     |

Dismiss overlays before the first navigation click:

1. If `startup-disclaimer-agreement` exists: click it, then
   `startup-disclaimer-accept`.
2. If `runtime-reload-notice-ack` exists: click it.
3. Wait until `workspace-nav-app` (or the journey's destination tag) is present.

Do not click pixel coordinates. Do not match translated labels.

### 4. Measurement

`DpisStartupBenchmarks`:

- `CompilationMode.None()` vs `CompilationMode.Partial(BaselineProfileMode.Require)`
- `StartupMode.COLD`
- `StartupTimingMetric`
- 10 iterations
- measure block: start activity, dismiss overlays, wait idle

`DpisAppsScrollBenchmarks`:

- `FrameTimingMetric` on the Apps list swipe after compilation with baseline
  profiles
- not a CI gate

Run on a physical device. Emulators are not evidence.

### 5. Shipping profiles

Add `implementation(libs.androidx.profileinstaller)` to `:app`.

Keep `useConnectedDevices = true` on `:baselineprofile`. Do not turn on
automatic generation during CI builds.

Local generation (API 33+ or rooted API 28+):

```text
./gradlew :app:generateModernReleaseBaselineProfile
./gradlew :app:generateLegacyReleaseBaselineProfile
```

Commit the plugin output:

- `app/src/modernRelease/generated/baselineProfiles/`
- `app/src/legacyRelease/generated/baselineProfiles/`

Each tree must contain `baseline-prof.txt` and `startup-prof.txt`.

Without those files, a release APK does not carry this optimization. Code can
land before the first device capture; the capture is a required follow-up, not
optional product behavior.

### 6. Tests and docs

JVM source smoke (in `app/src/test`, not androidTest):

- every `WorkspaceDestination` tag string exists and is applied in all four
  navigation layouts
- `ComposeDesignSystem` sets `testTagsAsResourceId = true`
- reload notice exposes `runtime-reload-notice-ack`
- generator sources live under `baselineprofile/src/main`
- generator has no `APPS_NAV_X` / pixel fields
- `includeInStartupProfile = true` appears only on the cold-start collect call
- `:app` depends on `profileinstaller`

Do not add a large set of string-presence assertions that restate the same
wiring. One semantic test per contract is enough.

`CONTRIBUTING.md` documents the two generate tasks, the commit paths, and that
CI does not run them.

## Verification

Without a device:

```text
./gradlew :app:testAllDebugUnitTests
./gradlew :app:assembleModernDebug :app:assembleLegacyDebug
```

With a connected API 33+ device, run the generate tasks and confirm:

- `baseline-prof.txt` lists DPIS manager types such as `MainActivity` and
  workspace composables
- `startup-prof.txt` is smaller than `baseline-prof.txt`
- startup benchmark `Partial` is faster than `None` on that device

## Out of scope later work

These are valid follow-ups, not this change:

- recording the first app-config editor on a seeded catalog
- Wear compact capture on a round window
- Play Cloud Profile monitoring (sideload distribution)
- CI device farm generation
