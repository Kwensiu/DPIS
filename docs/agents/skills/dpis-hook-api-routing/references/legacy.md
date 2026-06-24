# Legacy Hook Route

Use this reference for work rooted in `app/src/legacy/java/`.

## Intent

Keep Legacy behavior pure to the old Xposed contract.

- Do not retrofit libxposed naming or lifecycle assumptions into Legacy code.
- Do not create a fake shared abstraction when the Legacy and Modern entry
  points are structurally different.
- Treat Legacy as a first-class supported route, not as a degraded fallback.

## Legacy Workflow

1. Locate the Legacy entry owner and the actual package/process scope.
2. Confirm whether the hook is method-based, resource-based, or hybrid.
3. Keep hook registration in Legacy-owned code paths.
4. Use shared helpers only for stable product semantics such as config parsing,
   package-state resolution, or mutation policy.
5. Verify install and callback evidence with Legacy logs or behavior checks.

## Legacy Primitive Preference

Use the narrowest Legacy primitive that clearly expresses the target:

- Prefer `XposedHelpers.findAndHookMethod(...)` when the target can be named
  directly and reflective lookup does not need special handling.
- Prefer `XposedBridge.hookMethod(...)` when the method must be resolved first
  and then hooked explicitly.
- Prefer direct reflective lookup only when class/method discovery needs custom
  control that the helper path does not provide cleanly.

Do not add extra wrapper layers just to make these shapes look like Modern.

## Keep Shared Code Out Unless It Is Truly Shared

Appropriate shared candidates:

- package config reading
- route planner policy
- value normalization
- package matching
- pure data objects

Poor shared candidates:

- framework-specific hook registration
- Legacy callback wrappers that mimic libxposed style
- compatibility shims whose only purpose is aesthetic unification

Litmus test:

- If the helper mentions `XC_LoadPackage`, `XC_MethodHook`, `MethodHookParam`,
  `XposedInterface`, or hook registration, it is framework-shaped and should
  stay flavor-owned.

## Explicit Bans

- Do not import `io.github.libxposed.*` from `app/src/legacy/java/`.
- Do not add API 101 or 102 capability gates to Legacy flavor code.
- Do not port Modern `Application.attach(...)` timing assumptions into Legacy
  without naming the different Legacy-owned lifecycle boundary first.

## Legacy Review Questions

- Is the new logic genuinely required in `legacy`, or was it copied from
  `modern` out of convenience?
- Does the change preserve Legacy semantics even if Modern evolves further?
- Would a Legacy maintainer be able to read this without knowing libxposed API
  101 or 102?
- Is the fallback behavior explicit when a shared planner chooses different
  behavior for Legacy and Modern?
- Has the lifecycle mismatch between Modern and Legacy entry timing been named
  instead of papered over?
