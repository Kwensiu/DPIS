# MultiApp User Investigation

## Scope

This investigation concerns only the device's OS-provided `MultiApp` user
space. It does not concern app-container virtualization. The primary user is
shown as `Kevin`.

This is an evidence ledger, not a compatibility promise. Separate observed
facts from hypotheses; update it when a controlled scope-change reproduction
is captured.

## Device Identity

Device used for the 2026-10-18 investigation:

- ADB serial: `192.168.5.130:5555`
- Product/model: `PJZ110`
- Android foreground user at initial inspection: `0`
- `pm list users`: `UserInfo{0:Kevin:...}` and
  `UserInfo{999:MultiApp:...}`
- `com.tencent.mm` installed for user 0 with UID `10343`
- `com.tencent.mm` installed for user 999 with UID `99910343`
- DPIS is installed for user 0 with UID `10837` and user 999 with UID
  `99910837`.

Commands used for read-only identity checks:

```sh
adb shell pm list users
adb shell am get-current-user
adb shell pm list packages --user 0 -U com.tencent.mm
adb shell pm list packages --user 999 -U com.tencent.mm
adb shell pm list packages --user 999 -U io.github.kwensiu.dpis
adb shell ps -A -o USER,UID,PID,NAME
```

## Confirmed Runtime Evidence

### Explicit MultiApp launch

The MultiApp WeChat launcher activity resolves as
`com.tencent.mm/.ui.LauncherUI` for user 999:

```sh
adb shell cmd package resolve-activity --brief --user 999 \
  -a android.intent.action.MAIN \
  -c android.intent.category.LAUNCHER com.tencent.mm
```

Launching it explicitly with `am start --user 999` put
`u999 com.tencent.mm/.ui.LauncherUI` in the foreground. The main-user WeChat
task remained a separate, background task under user 0. This confirms that
explicit user selection starts the clone rather than the primary app.

### LSPosed injection and app-process behavior

LSPosed's module log at
`/data/adb/lspd/log/modules_2026-10-18T07:29:03.121365.log` records DPIS
loading in user 999's WeChat process (`uid=99910343`). It also records DPIS
loading in the primary WeChat process (`uid=10343`). The log contains entries
for the main, `:push`, and `:appbrand` processes.

- On the explicit MultiApp launch at approximately `20:02:49`, the LSPosed
  log records a DisplayMetrics callback and mutation
  `densityDpi=432 -> 500` (`density=2.7 -> 3.125`).

This is positive evidence that LSPosed injects DPIS in MultiApp and the
app-process WeChat DPI hook can mutate a runtime object there. It does not
prove that the system-server viewport route is effective for MultiApp.

### Older diagnostic ZIPs

The two user-provided diagnostic packs were analyzed with
`tools/analyze_diagnostic_pack.py`:

- `dpis-diagnostic-com.tencent.mm-主空间160比例系统.zip`
- `dpis-diagnostic-com.tencent.mm-MultiApp 160 比例系统.zip`

Both packs say viewport `scale=160%`, `mode=system`, with `scopeKnown=true` and
`inScope=true`. The primary pack says `systemHooksEnabled=true`. The MultiApp
pack says `systemHooksEnabled=false`; its DPIS log has
`desired=true, serviceAvailable=true, scopeSelected=false, effective=false`.
The primary pack instead has `scopeSelected=true, effective=true`.

Both packs contain app-process viewport applied events. The MultiApp pack
therefore does not support a claim that all DPIS scaling failed in that
capture. App-process effects do not prove that the system-server route ran.

The MultiApp pack's `lsposed-log.txt` and diagnostic window do not provide a
controlled before/after record of removing MultiApp scope. Neither pack
contains the user-visible action that reportedly caused primary WeChat to
stop responding to its hooks.

### WeChat property snapshot

The package hash used by DPIS for `com.tencent.mm` is `c5fe9776`. After
explicitly launching MultiApp, the device property snapshot showed the
following runtime values for that hash as `off`/`0` in both `debug.*` and
`persist.debug.*` variants (including viewport, viewport type/scale/mode,
font mode/force value, typeface, and hook domains). The device-wide
`persist.debug.dpis.system_server_hooks_enabled` value was `1`.

At the same time, the MultiApp LSPosed runtime log resolved `targetDpi=500`
from the dedicated WeChat runtime property and observed the `432 -> 500`
mutation. It did not prove that user 999 had a matching stored preference.
The snapshot alone does not establish which user last published the shared
property.

A later read-only snapshot, with both WeChat UIDs running, showed the same
split more clearly:

- user 0: `10343 com.tencent.mm`
- user 999: `99910343 com.tencent.mm`
- both user processes continued to produce LSPosed/DPIS `targetDpi=500` and
  `densityDpi=432->500` entries in the module log
- `debug.dpis.vp.c5fe9776=0`, `debug.dpis.vpmode.c5fe9776=off`, and
  `persist.debug.dpis.vp.c5fe9776=0`
- `debug.dpis.wechat.dpi.c5fe9776=500` and
  `persist.debug.dpis.wechat.dpi.c5fe9776=500`
- `persist.debug.dpis.system_server_hooks_enabled=1`

This confirms that the dedicated WeChat DPI bridge is not the same state as
the generic viewport property bridge. It also shows that a generic viewport
property value of `off` cannot by itself be used to conclude that the WeChat
DPI hook is disabled.

The read-only LSPosed database query for `module_configs` returned
`package_config.com.tencent.mm.app.wechat_dpi=500` for DPIS user 0 and no
matching `wechat_dpi` key for DPIS user 999. The `target_packages` and other
module config records are also stored per user in this database.

Code inspection at investigation time showed the Modern dedicated WeChat
installer calling `WechatDpiRoutePolicy.targetDpi(packageName)`, which called
`WechatDpiPropertyBridge.readDpi(packageName)`. Both paths have since been
updated to carry the target user's identity.

Confirmed conclusion: persisted configuration is per DPIS user, but the
runtime WeChat DPI delivery channel is keyed only by package hash and is
shared across Android users. MultiApp consumed the same published WeChat DPI
value as the primary profile even though its own module config had no
`wechat_dpi` value. The key omitted userId, so user-specific sync or cleanup
could change what the other user's WeChat process read. The implementation
below changes this key; the scope-toggle report remains unlinked to this path.

### User-specific property verification

After installing the Modern Debug APK on 2026-10-18, DPIS recovery was run
explicitly as user 999 and then user 0. The resulting properties were:

| key                                                | value |
|----------------------------------------------------|------:|
| `debug.dpis.wechat.dpi.c5fe9776` (old key)         | `500` |
| `persist.debug.dpis.wechat.dpi.c5fe9776` (old key) | `500` |
| `debug.dpis.wechat.dpi.0.c5fe9776`                 | `500` |
| `persist.debug.dpis.wechat.dpi.0.c5fe9776`         | `500` |
| `debug.dpis.wechat.dpi.999.c5fe9776`               |   `0` |
| `persist.debug.dpis.wechat.dpi.999.c5fe9776`       |   `0` |

Starting user 0 DPIS did not alter user 999's values; starting either WeChat
user afterward preserved the same split. LSPosed logs recorded
`targetDpi=500` for UID `10343` and `targetDpi=null` for UID `99910343`.
This verifies that old package-only `500` is ignored and the app-process
readers now resolve their own user's key. The experiment did not toggle LSPosed
scope; the reported scope-removal trigger remains unverified.

A short, explicit-user device experiment was run on 2026-10-18, before the
user-specific property key change. It did not change LSPosed scope, saved
configuration, installed packages, or the module APK. Before the test, both
`debug.dpis.wechat.dpi.c5fe9776` and
`persist.debug.dpis.wechat.dpi.c5fe9776` were `500`.

1. Started the DPIS launcher explicitly as user 999 with
   `am start --user 999 ... io.github.kwensiu.dpis/com.dpis.module.MainActivityLauncher`.
2. Polled both properties once per second. The first sample was still `500`;
   by the next sample, both were `0` and stayed `0` for the remaining samples.
3. Started the DPIS launcher explicitly as user 0. Android brought its
   existing task to the front. The first sample was still `0`; by the next
   one-second sample both properties were `500` and stayed `500`.

This directly verifies that the shared property is overwritten and later
restored through DPIS startup recovery. It can explain the dedicated WeChat
DPI hook disappearing and returning. The report's specific scope-toggle
action has not yet been tested as the trigger.

The implementation prevents the reproduced startup-recovery overwrite by
writing to each user's own WeChat DPI property. It does not yet prove that the
reported scope-toggle action is the trigger, nor does it claim to fix other
package-hash-keyed runtime properties.

### LSPosed Configuration Database Evidence

A read-only copy of the device database
`/data/adb/lspd/config/modules_config.db` was inspected at
`E:/System/Cache/dpis-modules_config.db`. The schema contains:

```sql
scope(
  module_pkg_name TEXT,
  app_pkg_name TEXT,
  user_id INTEGER,
  PRIMARY KEY (module_pkg_name, app_pkg_name, user_id)
)
```

It also contains `modules_state` keyed by `(module_pkg_name, user_id)`.
The database is therefore not merely a package-global scope list; the
persisted scope ownership includes Android `user_id`.

Current rows for DPIS (`io.github.kwensiu.dpis`) are:

| user | app scope        | system scope | module enabled |
|-----:|------------------|--------------|----------------|
|    0 | `com.tencent.mm` | `system`     | `1`            |
|  999 | `com.tencent.mm` | absent       | `1`            |

The complete DPIS app-scope query returned these user-0 entries:
`com.android.chrome`, `com.github.android`, `com.lalamove.huolala.driver`,
`com.larus.nova`, `com.tencent.mm`, `sh.paseo`, `system`, and
`tv.danmaku.bili`. User 999 contains `com.android.chrome` and
`com.tencent.mm` only.

This is direct evidence that, at the time of the read, removing
`com.tencent.mm` through the user-999 DPIS service would target a database row
whose primary key is user 999. It does not support the theory that the same
operation directly deletes the user-0 WeChat row. It does not yet prove that
all LSPosed runtime caches and callbacks obey the database key, so a controlled
remove-and-restart test is still useful.

The LSPosed Manager installed on the device is `org.lsposed.manager` version
`2.2.1` (version code `7912`). The database copy was obtained read-only; the
device database was not modified.

## API And Scope Boundary Findings

The project resolves `io.github.libxposed:service:101.0.0`. Its local source
for `XposedService` and `IXposedService` defines:

- `getScope()` with no user argument
- `requestScope(List<String> packages, callback)` with no user argument
- `removeScope(List<String> packages)` with no user argument
- no user ID or scope-owner field in the API 101 service contract

The service object is obtained in the DPIS app process through the framework
connection, so the API shape alone cannot determine whether the backend
isolates data by the calling Android user. It does establish that DPIS cannot
ask API 101 to remove a package scope for an explicitly named user. The
server-side LSPosed implementation and this device's Manager behavior remain
unverified.

## Code Findings

- `RootAppProcessLauncher` previously used `am start --user current` and an
  unqualified `am force-stop`. It now derives the caller's user from
  `context.applicationInfo.uid / 100000` and passes that explicit user to both
  commands. This fixes the identified launch-target ambiguity for actions
  initiated by DPIS installed in user 999.
- Related behavior tests assert that force-stop and launcher-start commands
  target the supplied user ID. Modern and Legacy targeted tests passed when
  this change was made.
- `PackageUidResolver` already accepts user ID, keys its cache by
  `(packageName, userId)`, and resolves package UIDs per user in
  `system_server` paths.
- `ViewportRuntimeMarkerBridge` also had a package-only key. It now prefixes
  the package hash with the current Android user ID. On PJZ110 after installing
  Modern Debug, user 0 published `debug.dpis.vprtm.0.0f493ae6`; the matching
  user-999 key remained empty, so MultiApp no longer consumed user 0's marker.
  This verifies namespace isolation, not viewport effectiveness: MultiApp Chrome
  logs still show `system hook resolve ... scopeSelected=false`, and its first
  resource callbacks had invalid zero-sized configuration and were skipped.
- `SystemScopeCoordinator` resolves effective system-hook state from the
  current `XposedService.scope`; the primary and MultiApp diagnostic packs
  returned different answers from their respective DPIS user contexts. This
  does not show that LSPosed removed the primary user's scope.
- App-scope removal calls `removeScope(listOf(packageName))`; system scope is
  separate. The API call has no user parameter, so the calling-user mapping
  must be kept distinct from the persisted DB key.
- API 101 `getScope`, `requestScope`, and `removeScope` expose no user ID.
  This is a client API limitation, not proof of a globally shared backend.
- The live LSPosed database snapshot had user 0 scope rows for
  `com.tencent.mm` and `system`, and user 999 had `com.tencent.mm` without
  `system`; both module states were enabled. Scope storage is not merely a
  package-only global list, but the schema alone does not prove runtime cache
  invalidation behavior.
- After the user removed only DPIS's user-999 Chrome scope, the DB/WAL snapshot
  contained Chrome scope for user 0 but not user 999; user 0 `system` scope
  remained. User 999 Chrome viewport properties were `off`, while user 0 stayed
  at 80%. At 00:09:55, ActivityTaskManager identified the launch as `u999`, and
  in the same second system_server applied `288x634 @ 600dpi` (the primary
  user's 80% target) to the Chrome launch configuration. This is direct
  evidence that a user-0 snapshot fallback leaked its viewport into user 999
  after the user-999 runtime target was cleared.
- Both WeChat UIDs were observed running and mutating to DPI 500 in one log
  window. Generic viewport `off/0` is separate from dedicated WeChat DPI.

- `WechatDpiPropertySyncer` derives its value from the supplied store and
  writes it to the property pair for the DPIS process's Android user. An
  absent/invalid DPI or disabled target clears only that user's pair.
- `DpisApplication.onServiceBind` and `onServiceDied` enqueue recovery from
  their active store; batch package reset also clears the same user's pair.
  Thus a user-999 recovery cannot overwrite user 0's WeChat DPI property.

The implementation files changed for the explicit-user app restart are:

- `app/src/main/java/com/dpis/module/root/RootAppProcessLauncher.kt`
- `app/src/test/java/com/dpis/module/root/RootAppProcessLauncherTest.kt`
- `app/src/test/java/com/dpis/module/process/ProcessActionHandlerSourceSmokeTest.java`

## Current Conclusions

1. MultiApp is a real Android user/profile space, not a renamed package or
   virtual app container. The same package name has a different UID per user.
2. DPIS is installed and injected in both users. MultiApp WeChat's
   app-process-specific DPI mutation was directly observed.
3. The old “no system framework” / ineffective-system-route indication in
   MultiApp must not be read as proof that the device lacks system_server or
   that app-process hooks cannot work. The LSPosed database snapshot has
   user 0 WeChat and `system` scope, but only user 999 WeChat scope.
4. Saved scope and module configuration are per user; WeChat DPI properties
   are now keyed by Android user and package hash. User 999 without a saved
   DPI no longer consumes user 0's published `500`.
5. A controlled DPIS launch/recovery experiment reproduced the old shared-key
   overwrite. The new user-specific keys prevent this cross-user write path,
   though that changed behavior still needs device verification.
6. Generic viewport property names and runtime marker names now include Android
   user ID as well as the package hash. system_server selects the target user's
   config from Activity/owner UID; when UID evidence is unavailable, it skips
   the candidate rather than falling back to user 0. On PJZ110, Modern Debug
   produced distinct 80% and 160% Chrome properties for users 0 and 999 and
   separate marker namespaces. MultiApp Chrome read the 160% config, but its
   system hook remained ineffective (`scopeSelected=false`) and zero-sized
   initial resource callbacks were skipped. The system_server viewport route
   remains unverified; the later app-process remote-snapshot resource mutation
   is recorded in conclusion 9.
7. Removing DPIS's user-999 Chrome scope clears that user's runtime viewport
   property but does not alter user 0's Chrome or `system` scope rows. With the
   user-999 property off, system_server has repeatedly applied user 0's 80%
   target (`288x634 @ 600dpi`) to explicitly identified `u999` Chrome launches.
   The config source was falling back to its user-0 package snapshot when a
   secondary user's runtime target was absent. A code change now fails closed
   for viewport targets on non-zero users unless that user's runtime target
   exists; its behavior test passes. Device results remain inconsistent: one
   fresh-task run showed `360x792 @ 480dpi`, but later fresh-task runs again
   showed `288x634 @ 600dpi`. Current system_server logs still publish the old
   package-only marker key `debug.dpis.vprtm.0f493ae6`, while the installed APK
   uses user-prefixed keys. This suggests system_server may still hold the
   pre-update module code; the code fix is not device-verified until that
   process reloads and the fresh-task test passes consistently.
8. The user's reported loss after removing MultiApp WeChat scope remains
   unverified. The database snapshot does not support direct deletion of the
   user-0 WeChat row by a user-999 package-scope removal. Whether that removal
   triggers the previously reproduced WeChat recovery path still needs a
   separate capture.
9. A later PJZ110 snapshot found independent Chrome settings in both DPIS
   private archives: user 0 has `relative_scale:80000/system`, and user 999
   has `relative_scale:160000/auto`. With only user 999's transient and
   persistent `vpscale` mirrors disabled, a cold user-999 Chrome process
   logged `module-loaded-fallback`, consumed `relative_scale:160000`, and
   mutated Resources to `576x1267 @ 300dpi`. Restoring the property to
   `160000` did not alter either config archive. This confirms independent
   per-user saved values and user-scoped Modern remote preference reads.
10. The earlier 2026-10-18 observation that user 999 had no Chrome package
    config is a historical snapshot, not the current device state. The
    persistence migration must preserve each user's existing archive and must
    not replicate package-only data across users.

## Status

**Per-user UI and scope ownership**

The current product model is one DPIS installation per Android user context:
each instance lists and edits only applications installed for its own user, and
its private archive remains that user's configuration source. The same package
in another user is managed by that user's DPIS instance.

Runtime execution is a separate boundary. The system_server route can resolve
the target UID and apply that target user's runtime values without loading DPIS
inside the target process. Compat viewport and other app-process-only routes
still require the target package to be in that Android user's LSPosed scope.
On PJZ110, scope rows are keyed by `user_id`; the service APIs expose no explicit
user argument, so user 0 DPIS cannot request user 999 scope through the current
API.

A single primary-space UI that reads and writes all users' archives remains a
deferred experiment. It is not the current product contract, and the current app
list does not display locked duplicate rows for other users.

The MultiApp Chrome cross-user viewport leak is reproduced. The source change
that prevents non-primary users from inheriting user 0's saved viewport has a
passing behavior test, and the full unit suite plus both Debug builds passed.
However, on-device verification is not stable: one fresh task used
`360x792 @ 480dpi`, while later fresh tasks again received user 0's
`288x634 @ 600dpi`. The running system_server still logs the old package-only
viewport marker key, unlike the installed APK's user-prefixed key. A full
system_server/module reload is needed before treating the source fix as
verified on device. Do not remove user 0's `system` scope during this check.
The independent WeChat scope-removal report remains unverified.

## Next Controlled Reproduction

The property overwrite is reproduced. The remaining experiment must isolate
whether the reported scope removal triggers it. Preserve LSPosed module and
verbose logs. Do not change WeChat DPI settings, `system` scope, font hook
domains, or other app scopes during the run.

1. Record user-specific values:
   `debug.dpis.wechat.dpi.0.c5fe9776`,
   `persist.debug.dpis.wechat.dpi.0.c5fe9776`,
   `debug.dpis.wechat.dpi.999.c5fe9776`, and
   `persist.debug.dpis.wechat.dpi.999.c5fe9776`.
2. Confirm user 0's WeChat app scope and `system` scope rows, and user 999's
   WeChat scope row, from a read-only LSPosed database copy.
3. Remove only the user-999 DPIS app scope for `com.tencent.mm` through the
   user-999 Manager context.
4. Immediately re-query both scope rows and poll the user-specific property
   pairs listed above. Do not launch DPIS in either user during this first
   observation window.
5. Restart user 0 WeChat explicitly and record whether its hook callback and
   `432->500` mutation occur.
6. Only after capturing the immediate state, explicitly open DPIS in user 999
   and user 0 separately, recording when the shared property changes and
   whether WeChat recovers.
7. If primary WeChat stops responding, capture the LSPosed log slices and
   diagnostics before any further restart or toggle.

Useful device commands:

```sh
adb shell pm list users
adb shell pm list packages --user 0 -U com.tencent.mm
adb shell pm list packages --user 999 -U com.tencent.mm
adb shell am start --user 0 -a android.intent.action.MAIN \
  -c android.intent.category.LAUNCHER -n com.tencent.mm/.ui.LauncherUI
adb shell am start --user 999 -a android.intent.action.MAIN \
  -c android.intent.category.LAUNCHER -n com.tencent.mm/.ui.LauncherUI
adb shell ps -A -o USER,UID,PID,NAME
adb shell su -c grep com.tencent.mm /data/adb/lspd/log/modules_*.log
```

## Evidence Limits

- The controlled startup/recovery experiment changed no scope, saved config,
  installed package, or module APK. It did start DPIS in users 999 and 0 and
  briefly changed their shared WeChat DPI property between `0` and `500`.
- On PJZ110, `pm list packages --user 999` returned 11 packages while the
  user-999 DPIS process received 851 entries from both public and explicit-user
  `IPackageManager.getInstalledPackages` queries. HyperOS exposes a shared
  visible package set through those APIs rather than the user's installed set.
  The app catalog now intersects that result with root `pm list packages --user
  <currentUserId>` output when root is available. After the fix, user 999's
  catalog contained 11 raw packages and 10 visible rows after excluding DPIS;
  primary-user-only applications no longer appeared. When Root is unavailable,
  PackageManager results are filtered by each record's `ApplicationInfo.uid`;
  records without an attributable UID are omitted. This prevents an unverified
  cross-user row from entering the fallback catalog but can make the catalog
  incomplete on devices that expose shared package records without user-specific
  UIDs. A later user-999 DPIS cold start through the refactored source confirmed
  the same root-authoritative result: 11 raw packages and 10 visible rows.
  Launcher fallback is also filtered before entering the non-root catalog.
- The LSPosed database schema stores scope with `user_id`, but it does not
  prove immediate in-memory runtime cache invalidation after `removeScope`.
- No system-server route effectiveness for user 999 was established by the
  two uploaded diagnostic packs.
- The old shared property pair remains on the device but is intentionally
  ignored. User 999 currently has no saved WeChat DPI and resolves its own
  property value as unset; user 0 resolves its own value as `500`.
- The original scope-toggle-to-recovery sequence remains untested.

## 2026-10-21 Chrome Per-User Visual Verification

Status: complete for Chrome viewport behavior on PJZ110. The user confirmed
that the visual result is effective for both Android users. This entry
supersedes the earlier Chrome-specific statements above that device
verification was pending or inconsistent; those statements remain as historical
observations from earlier APK/process states.

Two diagnostic archives were captured on the same device with Chrome
`155.0.8059.84`:

- User 0 archive: `dpis-diagnostic-com.android.chrome-user0.zip`, captured at
  `12:06:08`.
- User 999 archive: `dpis-diagnostic-com.android.chrome-user999.zip`, captured
  at `12:06:57`.

The selected saved configurations were distinct and matched their Android
users: user 0 used relative scale `80%` in Compat mode; user 999 used relative
scale `160%` in System mode. Both had font scale `60%`.

Runtime evidence agrees with the visual confirmation:

- User 0's initial system-server activity-start, config-dispatch, and
  relayout-dispatch entries lacked UID evidence and were skipped. The
  launch-activity-item route then resolved Chrome UID `10488` to user `0` from
  `ActivityInfo.applicationInfo` and applied `288x634 @ 600dpi`. The Chrome app
  process logged viewport mutations from `360x792 @ 480dpi` to
  `288x634 @ 600dpi`; subsequent ResourcesManager marker probes hit the
  resulting target.
- User 999's launch-activity-item route resolved UID `99910488` to user `999`
  and applied `576x1267 @ 300dpi`. The system-server display-manager route
  changed logical density from `480` to `300`; app-process resource and display
  metric routes also logged applied results for the `160%` target.

This confirms separate per-user configuration selection and effective Chrome
viewport application in both System and Compat modes. It does not change the
separate finding that user-999 LSPosed package scope is required for
app-process-only routes, nor does it establish independent isolation for
package-hash-only font/native property channels. The reported WeChat
scope-removal-to-recovery sequence remains unverified.
