# Font Domain Arbitration Notes

This note records the current design decisions for the `field_rewrite` font
pipeline. It is an implementation guide, not user-facing documentation.

## Problem

Mixed-rendering apps can expose several font rendering domains in the same
process:

- Flutter main UI uses `FlutterView` / `FlutterSurfaceView`.
- Ads and banners may use WebView platform views.
- Android platform text can still appear through TextView or spans.

Applying every font hook as an independent multiplier can double-scale text.
For example, if `scaledDensity` already reflects a 300% font scale, a
`TextView.setTextSize(SP, ...)` rewrite that multiplies the converted pixel
value by 3.0 can produce an effective 900% scale.

## Decisions

- Keep the user model as font mode plus percentage. Do not add user-facing
  TextView/WebView/Flutter/Paint switches.
- Treat `field_rewrite` as an internal font-domain scheduler.
- Enable the Resources/Configuration font domain for configured
  `field_rewrite` apps without waiting for Flutter evidence. This domain only
  changes font fields such as `Configuration.fontScale` and
  `DisplayMetrics.scaledDensity`; it must not pull viewport width or dp logic
  into the font path.
- Keep WebView independent. Its primary font source is `WebSettings.textZoom`.
  It must not be tied to TextView fallback policy.
- Keep TextView hooks, but downgrade them to a gap-filling TextView domain when
  Resources font handling is active.
- Do not multiply SP input again when Resources/scaledDensity is already the
  active font source.
- Use Paint/TextPaint only as a last fallback. Default it off when a clearer
  domain primary exists, because it is the highest-risk double-scaling path.

## Implementation Shape

Replace the current fallback-oriented policy with a domain plan. The plan should
use names that carry rendering-domain and unit semantics, for example:

- `resourcesFontEnabled`
- `webViewTextZoomEnabled`
- `textViewHooksEnabled`
- `textViewSpRewriteEnabled`
- `textViewAbsoluteRewriteEnabled`
- `paintFallbackEnabled`

The plan is produced by `FontHookArbitration` and consumed by
`AppProcessHookInstaller` and the individual installers.

## Anti Double-Scaling Strategy

Use a combination of low-cost mechanisms:

- Domain plan first: avoid installing high-frequency fallback hooks unless the
  plan needs them.
- Idempotent target values inside a domain: compute target size from a stable
  base and skip writes when the current value is already close enough.
- `ThreadLocal` guards only for hook reentrancy.
- `WeakHashMap` state only for object-local base values or fallback state.

Do not introduce a global "all font objects already scaled" tag system as the
first implementation. Object lifetimes differ across TextView, Paint, WebView,
and Flutter, so a global marker would be easy to miss or misapply.

## Flutter Finding

Some app main UIs are Flutter/Skia-rendered. Android Resources and WebView font
hooks work for platform/WebView text, but Flutter scene text may not consume
those values directly.

Native Flutter offset patching is not a viable default route. `libflutter.so`
instruction windows vary across engine builds and application packages, so a
working offset for one build does not provide a general DPIS mechanism. The
default Flutter path should be the semantic Android embedding boundary:
`flutter/settings`, `textScaleFactor`, `FlutterJNI.dispatchPlatformMessage`,
and `FlutterView.sendUserSettingsToFlutter`.

## Open Questions

- How narrow should the first TextView gap-filling pass be: PX-only, or PX plus
  other non-SP units?
- Should Paint fallback remain globally disabled in `field_rewrite` once
  Resources font handling is active, or should a later diagnostic-only route
  enable it for known custom Canvas text cases?
- Which regression tests should be added before changing installer wiring?

## Flutter Evidence

The current working hypothesis for Flutter text scaling is not native offset
patching. The evidence points to a semantic path:

- The app process is a Flutter UI process: `dumpsys activity top` shows
  `FlutterFragment` and `io.flutter.embedding.android.FlutterView`.
- The module reaches the target process: LSPosed logs show
  `DPIS module-loaded app hook probe enter: process=com.mfcloudcalculate.networkdisk`.
- The app process already reports the expected Android-side font configuration
  (`mCurrentConfig={2.0 ...}`) while the Flutter UI still needs a semantic
  settings rewrite.
- The code already targets the relevant Flutter boundaries:
  `flutter/settings`, `SettingsChannel$MessageBuilder`,
  `FlutterJNI.dispatchPlatformMessage`, `FlutterView.attachToFlutterEngine`,
  `Activity.onResume`, and `ViewRootImpl.performTraversals`.

This means Flutter font scaling is theoretically achievable through runtime
settings injection, but not through per-version `libflutter.so` fingerprints as
the primary route.

## libxposed API Utilization Notes

The current code already uses the important libxposed-side hooks for this
problem:

- `XposedModule.onModuleLoaded` and `onPackageReady` for early and late
  install entry points.
- `XposedInterface.hook(...).intercept(...)` with `ExceptionMode.PROTECTIVE`
  for framework and app-process hooks.
- `getRemotePreferences(...)` where available, with `XSharedPreferences` as a
  fallback bridge.

The main gap is not missing libxposed API surface. It is that the Flutter route
still mixes semantic hooks with a deprecated native fingerprint fallback. The
next cleanup should simplify the decision tree so libxposed is used to install
semantic hooks only, while the native Flutter probe remains observation-only.
