# libxposed Capability Route

Use this reference for work rooted in `app/src/modern/java/`.

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
3. Determine whether the behavior is expressible on API 101.
4. If the behavior benefits from a newer capability, define the exact gated
   delta.
5. Implement capability detection before higher-capability calls.
6. Keep the baseline path operational, testable, and visible in the code
   layout.

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

Risky patterns:

- reflection or layered wrappers used only to hide that 102 is optional
- replacing the 101 path entirely when the value of 102 is incremental
- feature detection scattered across unrelated hook classes
- enabling a higher-capability path based only on compile-time assumptions or UI
  labels

## Review Questions

- Can the 101 path still be understood on its own?
- Does the 102 path add a real capability, safety improvement, or correctness
  fix?
- Is capability detection centralized enough to audit?
- Would the code still be maintainable if a later framework revision adds a
  third branch?
