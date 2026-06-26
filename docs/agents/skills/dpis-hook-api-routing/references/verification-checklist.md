# Verification Checklist

Use this checklist after implementation or during diagnosis.

## Always Prove

1. Correct flavor chosen: `legacy`, `modern`, or shared.
2. Correct hook family chosen for that flavor.
3. Entry point reached.
4. Hook installed.
5. Callback or equivalent runtime action fired.
6. Target package or process resolved correctly.
7. Target classloader or attached runtime context resolved correctly when app
   classes are involved.
8. Visible runtime effect changed as intended.

## Additional Proof For Modern/libxposed

Also prove:

1. Which capability boundary was assumed: 101-safe or 102-enhanced.
2. How that boundary was detected or justified.
3. Which exact higher-capability feature required the 102 path.
4. What happens when the higher capability is absent.
5. Whether the 101 path still produces the required product behavior.

## Additional Proof For Shared Changes

Also prove:

1. Why the shared code is actually framework-independent.
2. Which flavor-owned code paths still exist after the change.
3. That the refactor did not silently move framework logic into common code.

## Evidence Sources

Prefer the smallest credible set:

- focused unit tests
- source smoke tests when wiring changes
- LSPosed or module logs proving entry/install/callback
- behavior checks tied to the target app or route
- APK/module metadata checks when entry registration or packaging changes
- code comments when explicit non-support is intentional

## Finish Gate

Do not call the task finished until you can explain:

- why this flavor owns the behavior
- why this API level was sufficient
- why the fallback is correct when the higher capability is unavailable
- why any shared helper remains framework-independent
