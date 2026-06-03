# modern101 Runtime Resync

This document records modern101 runtime route decisions present in this branch.

## WeChat Native Target Field

Status: active.

Scope: `com.tencent.mm` main WeChat process only. The route does not target
AppBrand, XWeb, TBS, or mini-program processes.

Mechanism:

- modern101 `ModuleMain` treats WeChat as a dedicated route. The main process
  installs `WechatTargetFieldModernHookInstaller`; secondary WeChat processes
  return without generic viewport/font hooks.
- Runtime selection uses the shared `WechatTargetFieldRoutes.java` registry
  keyed by exact WeChat `versionCode`. All entries are peers; there is no
  primary route and no probing fallback for unlisted versions.
- `PackageReadyParam.getApplicationInfo()` is the preferred source for
  `longVersionCode`, with application/system context `PackageManager` lookups
  as fallbacks.
- Getter routes use libxposed `intercept` to return the configured target value
  directly. Constructor-field routes run the original constructor first, then
  write `screenResolution_target_field` on `chain.getThisObject()`.
- The target value is read from the same DPIS runtime property used by
  compat100. Invalid, missing, or out-of-range values leave WeChat untouched.

Runtime finding:

- API 101 constructor hook usage matches the existing system-server pattern in
  `SystemServerDisplayEnvironmentInstaller`: call `chain.proceed()`, then read
  `chain.getThisObject()`. The WeChat `8.0.71` constructor-field route should
  still be verified on-device before treating API 101 behavior as fully proven.
