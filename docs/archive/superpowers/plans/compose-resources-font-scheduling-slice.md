# Compose Resources Font Scheduling Slice

Goal: connect Compose/resources_font runtime evidence to font scheduling so a Compose-heavy page does not keep reusing a Resources font scale that is already reflected in process metrics.

This slice deliberately does not disable TextView/Paint fallback globally when a Compose-heavy root is observed. It also does not introduce package-specific behavior or a Compose-native font backend. The runtime behavior change is narrow: when DPIS has current-root evidence that `resources_font` already reached Compose through `Configuration.fontScale` and `scaledDensity / density`, Resources font writes are suppressed back to the inferred base font scale for a short process-local TTL.

Implemented behavior:

1. `ComposeResourcesFontEvidence` emits a structured summary: domain enabled, fontScale match, scaledDensity ratio match, current-root Compose classification, and final resources-handled status.
2. `ComposeFontRuntimeDiagnosticsInstaller` observes the current Activity decor root from lifecycle callbacks and layout changes, then feeds matching evidence into `ComposeResourcesFontScheduler`.
3. `ComposeResourcesFontScheduler` stores only process-local state with a 30 second TTL. It records the inferred base font scale, not a persistent user preference.
4. Resources configuration and metrics read paths call the scheduler through `FontScaleOverride.resolveForResources(...)` and `maybeSuppressMetricsFontScale(...)`.
5. TextView and Paint arbitration stays independent; Compose evidence suppresses only Resources fontScale/scaledDensity writes.

Non-goals:

- No package-specific allowlist or denylist.
- No global shutdown of TextView current-px fallback.
- No default enablement of Paint fallback.
- No compensation that silently changes the user's configured target percentage outside the proven Compose/resources case.
