# libxposed Capability Route

Use this reference for work rooted in `app/src/modern/java/`.

For the verified API and Maven version details, see
[Verified API Facts](#verified-api-facts-checked-2026-10-19) below.

## Verified API Facts (Checked 2026-10-19)

Maven Central metadata currently lists API and service `102.0.0` as latest.
DPIS declares `io.github.libxposed:api:102.0.0` and
`io.github.libxposed:service:101.0.0`; the service dependency is one release
behind, but do not upgrade it unless the needed service API requires it.

`XposedInterface.getRemotePreferences(String)` returns a framework-backed,
read-only `SharedPreferences` view shared across processes. The framework must
expose the remote-preferences capability (`PROP_CAP_REMOTE`). The module
lifecycle exposes `ModuleLoadedParam.isSystemServer()`, so Modern can identify
its system_server load context. These API facts establish that direct Modern
hook-side config reads are supported; they do not prove vendor SELinux policy
allows system_server to write arbitrary DPIS properties.

The service-side `XposedService.getRemotePreferences(String)` is a separate
writable service API. DPIS UI already uses it to publish its authoritative
configuration; Modern hook runtimes use `XposedInterface.getRemotePreferences`
to read that shared snapshot.

Modern should read remote preferences as its primary config source. Property
mirrors are separate compatibility/runtime transport and should only remain
where an owning route is proven to require them. Legacy continues using its
classic `XSharedPreferences` boundary; do not import LibXposed types into
Legacy.

Upstream references:

- Maven API metadata: https://repo1.maven.org/maven2/io/github/libxposed/api/maven-metadata.xml
- Maven service
  metadata: https://repo1.maven.org/maven2/io/github/libxposed/service/maven-metadata.xml
- API 102 Javadoc: https://javadoc.io/doc/io.github.libxposed/api/102.0.0/
- Service 102 Javadoc: https://javadoc.io/doc/io.github.libxposed/service/102.0.0/
- API reference: https://github.com/libxposed/api/tree/master/_autodocs
- Service reference: https://github.com/libxposed/service/tree/master/_autodocs
- Android system property
  APIs: https://source.android.com/docs/core/architecture/configuration/sysprops-apis
- Android property
  permissions: https://source.android.com/docs/core/architecture/configuration/add-system-properties

The same official API references are indexed in Context7 as `/libxposed/api`
and `/libxposed/service`. Check Maven metadata and versioned Javadocs again
before changing these version claims.

## Intent

Treat libxposed as one evolving family with explicit capability selection
inside that family.

- Default to the lowest API surface that can express the behavior.
- Upgrade to newer API usage only after proving the framework capability is
  present and relevant.
- Preserve a readable fallback for the current baseline environment when
  introducing a higher-capability improvement.

## Decision Chain

1. Define the target behavior, not the preferred API.
2. Identify the minimum libxposed feature set required.
3. Identify whether the target app classes require the attached application
   context or process classloader.
4. Determine whether the behavior is expressible on API 101.
5. If the behavior benefits from a newer capability, define the exact gated
   delta.
6. Implement capability detection before higher-capability calls.
7. Keep the baseline path operational, testable, and visible in the code
   layout.

## Classloader Boundary

Do not resolve target app classes with the module classloader. If a hook needs
target app classes that are only safe after `Application.attach(...)` or another
runtime context boundary, name that boundary and use the attached `Context` or
target process classloader.

## Hard Rule For New API Usage

Before using a newer API, prove:

- the current framework exposes the needed API level or capability
- the exact higher-capability feature is named, not implied
- the new path provides real product value, not just stylistic novelty
- the lower path is insufficient, less correct, or materially harder to
  validate

If those proofs are weak, stay on the older path.

Version number alone is enough only when the higher behavior is defined solely
by that boundary. If the relevant framework feature has its own runtime probe
or capability signal, use it and keep the check near the installer.

## Recommended Modern Shapes

Good patterns:

- one Modern owner with a small capability gate around the higher API behavior
- one shared planner decision feeding separate baseline and enhanced hook
  registration branches
- comments that explain why the enhanced path exists and what the baseline path
  preserves
- one installer-local gate when the delta is small and readability matters
- app-class hooks that name whether they use the package classloader,
  `Application.attach(...)`, or another runtime context boundary

Risky patterns:

- reflection or layered wrappers used only to hide that 102 is optional
- replacing the 101 path entirely when the value of 102 is incremental
- feature detection scattered across unrelated hook classes
- enabling a higher-capability path based only on compile-time assumptions or UI
  labels
- resolving target app classes with the module classloader

## Review Questions

- Can the 101 path still be understood on its own?
- Does the 102 path add a real capability, safety improvement, or correctness
  fix?
- Is the target classloader source named and verified?
- Is capability detection centralized enough to audit?
- Would the code still be maintainable if a later framework revision adds a
  third branch?
