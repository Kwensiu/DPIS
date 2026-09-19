# App Filter Structure and Inclusion Semantics Implementation Plan

> For agentic workers: use the executing-plans or subagent-driven-development skill to execute this
> plan task by task. Steps use checkbox syntax.

**Goal:** Simplify app-list filter structure and make the contract explicit: app types and
configuration predicates combine with OR, while name/package search uses contains matching.

**Architecture:** Keep MainUiState authoritative and keep filter policy in com.dpis.module.applist.
Replace the boolean-heavy state API with explicit selected collections, preserve existing preference
keys, and make the Sheet dispatch named state transitions. Keep AppListVisibleSections as the
filter-then-sort derived-list boundary; do not add repository/use-case/framework layers.

**Tech Stack:** Kotlin/Java Android, Jetpack Compose, JUnit4, SharedPreferences, Gradle.

---

## Scope and file map

Modify or migrate:

- app/src/main/java/com/dpis/module/applist/AppListFilterState.java -> AppListFilterState.kt:
  immutable selected app types, selected configuration filters, sort order, reverse order, and named
  transitions.
- app/src/main/java/com/dpis/module/applist/AppListFilter.java -> AppListFilter.kt: one matcher
  entry point for search, page, type, and configuration predicates.
- app/src/main/java/com/dpis/module/applist/AppListVisibleSections.java: retain the existing Java
  boundary and keep only filter-then-sort composition.
- app/src/main/java/com/dpis/module/applist/AppListFilterStateStore.java: convert old preference
  keys to the new state and write the same keys.
- app/src/main/java/com/dpis/module/applist/AppListPage.java and
  app/src/main/java/com/dpis/module/ui/MainUiState.java: update policy calls only.
- app/src/main/java/com/dpis/module/ui/presentation/MainStartupSession.kt: update
  saved-instance-state construction without changing STATE_FILTER_* keys.
- app/src/main/java/com/dpis/module/ui/presentation/wear/WearWorkspaceContent.kt: remove
  four-argument state construction and use shared transitions.
- app/src/main/java/com/dpis/module/applist/presentation/AppFilterSheet.kt: remove repeated
  boolean-argument assembly and unused helpers.
- Related tests: AppListFilterTest.java, AppListFilterStateStoreTest.java, MainViewModelTest.kt,
  AppWorkspacePresentationTest.java, MainActivitySourceSmokeTest.java,
  MainActivityFilterStateSourceSmokeTest.kt, and any source test reported by exhaustive search.

Delete after callers are migrated:

- AppListFilterState.java
- AppListFilter.java
- Unused AppTypeChip and BooleanChip
- Obsolete constructors, overloads, and aliases with no live callers

Do not create a second Wear state model, generic filter framework, repository/use-case layer, or
duplicate matcher.

## Task 1: Characterize the behavior contract

**Files:** app/src/test/java/com/dpis/module/applist/AppListFilterTest.java and
app/src/test/java/com/dpis/module/shell/MainViewModelTest.kt

- [ ] Add a test selecting user and system types and assert both a user app and a system app match.
  Use the new named factory or collection constructor; do not add another six-boolean constructor.
- [ ] Add a test selecting injected and font-configured and assert: an app satisfying both matches;
  injected-only matches; font-only matches; an app satisfying neither fails.
- [ ] Add explicit search tests for a label substring, package-name substring, case-insensitive
  input, and a non-match. Preserve existing configured-page coverage.
- [ ] Add a derived-list test asserting filtering changes membership, reverse sorting changes order
  only, and batch selection still uses the unfiltered package universe.
- [ ] Run:

~~~~powershell
./gradlew :app:testAllDebugUnitTests --tests com.dpis.module.AppListFilterTest --tests com.dpis.module.shell.MainViewModelTest
~~~~

Expected: current contains/search behavior remains green; any failures identify old construction
points to migrate, not a behavior change.

## Task 2: Replace the boolean-heavy state model

**Files:** AppListFilterState.kt, AppListFilterStateStore.java, MainStartupSession.kt,
WearWorkspaceContent.kt, and all callers found by the searches below.

- [ ] Create the Kotlin state with this public shape:

~~~~kotlin
class AppListFilterState(
    val selectedAppTypes: Set<AppType>,
    val selectedConfigurationFilters: Set<ConfigurationFilter>,
    val sortOrder: SortOrder,
    val reverseOrder: Boolean,
) {
    enum class AppType { USER, SYSTEM, ALL }
    enum class ConfigurationFilter { ALL, INJECTED, DISABLED, VIEWPORT, FONT, TYPEFACE, HOOK }
    enum class SortOrder { NAME, UPDATED, INSTALLED }

    fun toggleAppType(type: AppType): AppListFilterState
    fun selectAllAppTypes(): AppListFilterState
    fun toggleConfiguration(filter: ConfigurationFilter): AppListFilterState
    fun clearConfigurationFilters(): AppListFilterState
    fun withSortOrder(value: SortOrder): AppListFilterState
    fun withReverseOrder(value: Boolean): AppListFilterState
}
~~~~

Use one representation for all types throughout. The recommended representation is an empty
selected-type set meaning no type restriction. Configuration ALL must not coexist with concrete
configuration filters. Add only defaultState() and noAdditionalConstraints() as default factories.
Keep temporary read accessors only where Java interop requires them, then remove unused accessors.

- [ ] Update AppListFilterStateStore.java to read the existing keys all_apps_selected,
  user_apps_selected, system_apps_selected, injected_only, disabled_only, width_configured_only,
  font_configured_only, typeface_configured_only, hook_configured_only, sort_order, and
  reverse_order. Retain the old app_type / show_system_apps fallback only when independent type keys
  are absent. Save the new state to the same keys and delete obsolete state-writing branches.
- [ ] Update MainStartupSession.kt to preserve all existing STATE_FILTER_* keys while constructing
  the new state through named factories/collections.
- [ ] Update Wear to call toggleAppType and toggleConfiguration; remove every four-argument
  AppListFilterState(...) call.
- [ ] Run:

~~~~powershell
rg -n "new AppListFilterState|AppListFilterState\(" app/src/main app/src/test
rg -n "withConfiguration\(|withAppTypes\(|withAppType\(" app/src/main app/src/test
~~~~

Delete every obsolete constructor or transition exposed by this search; do not add compatibility
aliases.

- [ ] Run:

~~~~powershell
./gradlew :app:testAllDebugUnitTests --tests com.dpis.module.AppListFilterStateStoreTest --tests com.dpis.module.shell.MainViewModelTest
~~~~

Expected: defaults, saved preferences, restored state, and filter dispatch pass.

## Task 3: Consolidate matching and retain one derived-list boundary

**Files:** AppListFilter.kt, AppListVisibleSections.java, AppListPage.java, MainUiState.java, and
AppListFilterTest.java.

- [ ] Replace the three Java matcher overloads with one Kotlin @JvmStatic entry point receiving all
  current app facts and AppListFilterState. Keep these meanings: configured controls the configured
  page, installed controls the all-apps page, inScope controls injected, false dpisEnabled controls
  disabled, and font/typeface/hook presence controls their positive filters.
- [ ] Implement the matcher as explicit predicates:

~~~~kotlin
return matchesQuery(query, label, packageName) &&
    matchesTab(tab, configured, installed) &&
    state.matchesAppType(systemApp) &&
    (state.selectedConfigurationFilters.isEmpty() ||
        state.selectedConfigurationFilters.any { it.matches(appFacts) })
~~~~

Normalize query, label, and package name before using contains; do not add a second matcher for Wear
or configured apps.

- [ ] Keep AppListVisibleSections.filter(source, query, page, state) as the sole visible-list
  boundary. It calls the matcher, sorts the resulting list by name/updated/installed, reverses when
  requested, and returns the list. Keep any sorting helper private to this file/package.
- [ ] Update Java callers and tests, then delete the Java matcher. Verify with:

~~~~powershell
rg -n "AppListFilter\.matches" app/src
~~~~

Only the single matcher entry point may remain.

- [ ] Run:

~~~~powershell
./gradlew :app:testAllDebugUnitTests --tests com.dpis.module.AppListFilterTest --tests com.dpis.module.AppListFilterStateStoreTest --tests com.dpis.module.shell.MainViewModelTest
~~~~

## Task 4: Simplify Sheet transitions and remove stale UI code

**Files:** AppFilterSheet.kt and related source smoke tests.

- [ ] Replace every repeated configuration argument list with a small helper that calls
  state.toggleConfiguration(filter); the Sheet remains stateless and presentation-only.
- [ ] Replace type boolean inversion with toggleAppType and the all chip with selectAllAppTypes.
  Preserve user/system multi-select.
- [ ] Remove AppTypeChip, BooleanChip, and any unused helper exposed by rg -n "
  AppTypeChip|BooleanChip" app/src.
- [ ] Keep preference key names with Only suffixes for compatibility, but rename internal symbols
  only where live callers exist and the positive meaning is clearer.
- [ ] Update source smoke tests to assert the changeFilters -> save -> dispatch boundary, not
  implementation-specific boolean syntax.
- [ ] Run:

~~~~powershell
./gradlew :app:testAllDebugUnitTests --tests com.dpis.module.shell.MainActivitySourceSmokeTest --tests com.dpis.module.shell.MainActivityFilterStateSourceSmokeTest
~~~~

## Task 5: Audit, test, build, install, and review

- [ ] Audit all references:

~~~~powershell
rg -n "AppListFilterState|AppListFilter|filterState|changeFilters|visibleItems\(" app/src/main app/src/test
~~~~

Classify each result as state, rendering, persistence, policy, or test. Remove stale adapters and
old aliases. Confirm replacements stay under com.dpis.module.applist.

- [ ] Run the full unit suite:

~~~~powershell
./gradlew :app:testAllDebugUnitTests
~~~~

Expected: BUILD SUCCESSFUL.

- [ ] Review:

~~~~powershell
git diff --check
git status --short
git diff --stat
~~~~

Remove unrelated formatting churn. The final diff must be limited to filter state/policy, Sheet/Wear
callers, related tests, and required docs.

- [ ] Build and install:

~~~~powershell
./gradlew :app:assembleModernDebug
./gradlew :app:installModernDebug
~~~~

Verify on the device: User + System shows both categories; Injected + Font Scale requires both;
partial label/package search matches; sorting changes order only; filters restore after restart;
hidden selected apps remain in batch selection.

- [ ] Before delivery, run the project pre-commit review skill and report test/build/install
  evidence. Do not commit implementation changes until the final diff and full suite are clean and
  the user chooses the delivery path.

## Suggested checkpoints

If executing inline, use focused Conventional Commits only after each coherent checkpoint:

1. test: characterize app filter inclusion semantics
2. refactor: simplify app filter state model
3. refactor: consolidate app filter matching
4. refactor: simplify app filter sheet actions
5. test: validate app filter integration

Do not create commits containing only formatting or compatibility aliases. The design baseline is
commit b0625c97.

