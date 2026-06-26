# ADR 0002: WebAPK Owner And Runtime Carrier Routing

## Status

Accepted

## Context

DPIS configuration is package keyed. That model works when the configured app
package and the runtime carrier package are the same.

Chrome Android WebAPK launches break that assumption:

- the user-visible installed app is a WebAPK package such as
  `org.chromium.webapk.*`;
- splash/bootstrap may run in that WebAPK package;
- final visible page activity may run in `com.android.chrome`.

If DPIS reasons only about the carrier package, WebAPK config is ignored at the
final content stage. If DPIS reasons only about the WebAPK package, Chrome
content activity is missed.

## Decision

DPIS distinguishes two package roles for WebAPK launches:

- `Owner package`: the configured package identity whose DPIS config should be
  used for this launch
- `Runtime carrier package`: the package actually executing the current route
  or activity stage

For Chrome WebAPK launches:

- the owner package is the WebAPK package;
- the carrier package may be `com.android.chrome`.

This distinction is launch-scoped evidence, not a package alias.

DPIS must not treat `com.android.chrome` as a global alias of
`org.chromium.webapk.*`.

## Consequences

### Positive

- preserves per-package configuration semantics;
- allows DPIS to support Chrome-carried WebAPK launches without making Chrome
  globally inherit WebAPK config;
- gives diagnostics a clear vocabulary for "configured owner" versus
  "executing carrier".

### Negative

- runtime target resolution becomes more complex than plain package matching;
- some routes may support owner/carrier separation earlier than others;
- Legacy may remain partial until a stable owner evidence source exists there.

## Implementation Direction

- system_server may select owner package config from proven WebAPK launch
  evidence while preserving carrier package diagnostics;
- app-process package resolution remains package-pure unless an explicit owner
  bridge exists for that carrier process;
- diagnostics and tests must surface owner/carrier separation directly.
