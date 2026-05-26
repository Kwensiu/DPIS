# DPIS Context

This document records DPIS domain language for agents and maintainers. It is not
a user guide and does not replace `README.md`.

## Product Boundary

DPIS is an LSPosed/Xposed module for per-app display and font behavior. A change
should stay package-scoped unless the code is explicitly part of `system_server`
route installation, runtime property recovery, or debug tooling.

## Viewport Model

`ViewportTargetSpec` is the authoritative viewport target representation.

- `relative_scale`: interface scale, stored as permille. User-facing range is
  30-300%.
- `absolute_dp`: fixed smallest-width target in dp.
- `off`: no viewport target.

Do not treat `width_dp` as the universal source of truth. It is a legacy value
and an adapter value for fixed-width targets.

Viewport target type and viewport apply strategy are separate concepts:

- Target type answers what to apply: relative scale or fixed width.
- Apply strategy answers where to apply it: auto, system, compat, or off.

## Apply Strategies

Viewport apply strategies:

- `auto`: prefer the system route when system hooks are available; fall back to
  compat only when system hooks are unavailable.
- `system`: system-server route only.
- `compat`: app-process compatibility route only.
- `off`: disabled.

Legacy persisted values still exist:

- `system_emulation` normalizes to `system`.
- `field_rewrite` normalizes to `compat`.

New code should use `ViewportApplyMode.AUTO`, `SYSTEM`, `COMPAT`, and `OFF`
unless it is explicitly testing legacy compatibility.

## Runtime Routes

System route:

- Runs in `system_server`.
- Mutates package-scoped launch/config/display data.
- Is required for `auto` when system hooks are available.
- In safe mode, keep only package-gated low-risk entries. `launch-activity-item`
  is needed for apps whose activity-start path does not expose a mutable
  configuration.

Compat route:

- Runs inside the target app process.
- Uses Resources/DisplayMetrics compatibility hooks.
- Can fix apps not reached by system route, but has higher risk of layout or
  process instability.

Do not re-enable app-process viewport hooks for `auto` just to fix one app. If
`auto` fails, first verify the system route, safe-mode entries, package scope,
and runtime property projection.

## Runtime Properties

Runtime properties mirror per-app configuration for cross-process readers and
compat100 fallback.

Viewport properties carry target type, active target value, compat config value,
and apply mode. Relative scale may legitimately have `vp=0` or `vpcfg=0`; check
`vptype` and `vpscale` before assuming the target is off.

Persistent `persist.debug.dpis.*` properties are fallbacks. Network, update, and
manifest decisions must not depend on cached runtime properties.

## Font Model

Font scale, typeface replacement, and hook domains are distinct concerns.

- Font scale changes `Configuration.fontScale` or app-process text-size paths.
- Typeface replacement changes font files or system-family selection.
- Hook domains select which font compatibility hooks are enabled per app.

Do not fold typeface replacement into font scale mode. Do not reintroduce old
global Flutter/HyperOS font switches; per-app hook domains own that decision.

## App Config Sheet

The app config sheet stores viewport scale and width drafts independently. The
active target type decides which draft is effective; the inactive draft must not
be cleared by saving the other mode.

UI labels may intentionally hide internal route names. For example, app-list
status uses one `Interface` segment for both relative scale and fixed width, with
the value showing `%` or `dp`.

## Debugging Checklist

Before changing route logic:

- Confirm the exact package name and Java hash used by runtime properties.
- Check target type, target value, and apply mode separately.
- Check whether system hooks and system safe mode are enabled.
- Confirm whether the target app process is in LSPosed scope before expecting
  compat hooks to load.
- Treat `hook ready` as installation evidence only; require an apply log,
  callback, mutation, process metric, or visible result before calling a route
  effective.

When a system-route app does not respond:

- Inspect `system_server` load time after installing a new module build.
- Reboot or restart `system_server` before judging changes to hook installation.
- Look for launch/config/display apply evidence, not only app-process logs.
- Avoid package-specific runtime exceptions unless a general route fix is ruled
  out.
