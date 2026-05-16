# Review Response: Native Flutter Diff

This note separates verified fixes from review points that are not accurate as
stated.

## Accepted And Fixed

### modern101 native init entry

The review is correct. libxposed API 101 discovers native entries through
`META-INF/xposed/native_init.list`. Removing
`app/src/modern101/resources/META-INF/xposed/native_init.list` would orphan
`native_init()` and prevent the LSPosed `on_library_loaded` callback path from
running in modern101 builds.

Fix:
- Restored `app/src/modern101/resources/META-INF/xposed/native_init.list`.
- Updated tests to require the modern101 native init list.
- Kept legacy `app/src/compat100/assets/native_init` for compat100.

### Native log bridge publication

The review is directionally correct. `g_dpis_log_class` and
`g_dpis_log_info_method` are read from native worker/probe paths and should not
be published independently without synchronization.

Fix:
- Added a mutex around native log bridge initialization and snapshot reads.
- Publish the Java log class and method only after both are available.

### Debug probe threads

The review is correct that probe/status helper threads do not need to be
non-daemon.

Fix:
- Changed Flutter probe executor and one-shot status thread to daemon threads.

## Not Accepted As Stated

### Push-style trampoline d13-d15 claim

The review says C calls are free to clobber `d13`-`d15`. That is not accurate
under AAPCS64: the low 64 bits of `v8`-`v15` are callee-saved. A conforming C
callee must preserve `d13`-`d15`.

This does not make the generic push-style route low-risk. The route is still an
experimental native offset/register hook and should be treated carefully. The
concern should be framed as route fragility and hardcoded offset risk, not as a
callee-saved register ABI violation in the current trampoline.

### GetScaledFontSize argument mismatch claim

The review says the trampoline has an argument passing mismatch. In the current
assembly, `d0` and `w0` are still the original incoming argument registers when
`dpis_generic_scaled_font_size_input(double, int)` is called, so the call shape
is not mismatched in the way described.

The better criticism is that this route is currently unused/dead code. It should
be removed or explicitly marked experimental if it remains uninstalled.

## Open Follow-up

The native Flutter route still needs a separate architecture pass. In
particular, HyperOS/native Flutter installation should be brought under the same
font-domain arbitration model as Resources, TextView, WebView, and Flutter
settings so native paths do not bypass unified font dispatch.
