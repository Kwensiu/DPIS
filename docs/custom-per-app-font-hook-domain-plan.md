# Per-App Font Hook Domain Plan

Date: 2026-05-18

Status: implemented in `26b37bd` and hardened in `9b8bb18`.

## Purpose

Add a per-app editor for the effective hook path used by font compatibility mode.
This is not a new font mode. It only decides which font hook domains participate
when an app runs through `field_rewrite`.

The main motivation is to keep the safe default path small while still allowing
advanced users to opt into fallback domains for apps that need them.

## Scope

In scope:

- App-process font hook planning.
- Runtime behavior for resolved font mode `field_rewrite`.
- Per-package UI, persistence, backup/export, and logs for font hook domains.
- Built-in package defaults that add known domains to the automatic path.

Out of scope:

- Viewport/display hooks.
- `system_server` mutation policy.
- A new persisted "custom mode" below the UI layer.
- Reproduction-target-specific runtime special cases.

## Current Baseline

After the calculator popup regression diagnosis, the stable automatic path for
`field_rewrite` is:

- `resources_font`
- `webview_text_zoom`

The default path deliberately excludes downstream TextView/Paint fallbacks:

- `textview_sp_rewrite`
- `textview_absolute_rewrite`
- `textview_current_px_fallback`
- `paint_text_size_fallback`

Reason: downstream TextView/Paint boundaries cannot reliably know whether an
incoming text size has already been scaled by `Resources`, `scaledDensity`, or a
UI library. Calculator's COUI popup demonstrated double scaling when
`textview_absolute_rewrite` was enabled together with `Resources` fontScale.

## Domain Registry

Create a font-only domain registry as the single source of truth for:

- Stable id.
- User-facing title.
- Subtitle/internal constant name.
- Group.
- Display and planner order.
- Mapping to and from `HookExecutionPlan` and `FontDomainPlan`.

Initial known domains, in priority/display order:

| Id | Group |
| --- | --- |
| `resources_font` | 资源配置 |
| `activity_thread_font` | 资源配置 |
| `textview_sp_rewrite` | 文本视图回退 |
| `textview_absolute_rewrite` | 文本视图回退 |
| `textview_current_px_fallback` | 文本视图回退 |
| `paint_text_size_fallback` | 文本视图回退 |
| `webview_text_zoom` | 网页 |
| `flutter_settings` | 跨运行时 |
| `hyperos_native_flutter` | 跨运行时 |

These ids describe functional domains, not individual method hooks. For example,
`textview_current_px_fallback` may be backed by multiple internal hooks, but UI
and storage treat it as one domain.

`textViewHooksEnabled` is not a separate UI/storage domain. It is derived from
the selected TextView/Paint child domains.

The installer does not need to split every internal hook into independent
installers. TextView/Paint domains can continue to be installed through
`ForceTextSizeHookInstaller` with a domain plan.

Domain notes:

- `resources_font` is the main Resources/ResourcesManager fontScale path.
- `activity_thread_font` is an ActivityThread bind/configuration injection path.
  It is an earlier resource-configuration boundary than ordinary TextView/Paint
  fallbacks and is not part of the current automatic default path.

## Storage

Use one per-package key that stores the enabled domain id set.

Key semantics:

| State | Meaning |
| --- | --- |
| Key missing | Automatic/recommended planner path |
| Key present with empty value | Custom path with zero enabled known domains |
| Key present with CSV ids | Custom path with those enabled domains |

The raw key must be read and written only through a dedicated helper, for
example `HookDomainOverrideStore`. Business logic should not parse the raw
string directly.

Unknown ids:

- Preserve unknown ids when reading and saving.
- Show unknown saved ids as disabled rows in an `未知链路` group.
- Place `未知链路` near the bottom of the dialog, above restore.
- Clear unknown ids only when the user restores the recommended path.
- Do not provide per-row delete actions for unknown ids in the first version.

This avoids version-by-version migration logic. New domains are naturally absent
from old custom paths. Removed domains can remain in storage, be ignored at
runtime, and survive export/import. Unknown ids are a preservation mechanism, not
an editing surface.

## Planner Semantics

The user's saved custom path is the final persistent decision.

Planner order:

1. Build the automatic path for the resolved font mode.
2. Add built-in package defaults, if any.
3. Apply non-user debug/diagnostic shaping, if any.
4. If a user custom path exists and the resolved font mode is `field_rewrite`,
   replace final domains with the saved custom domain id set.
5. Derive final installer booleans.

For non-`field_rewrite` modes, runtime ignores custom hook domains. The UI row
can still open the editor because the editor only reads and writes configuration.

Saving a custom path does not affect an already-running target app process. The
new hook path takes effect when the target app process starts again. This is
normal LSPosed/Xposed module behavior; DPIS does not show an extra in-app prompt
for it.

Restore recommended config only clears the custom hook-domain key. It does not
change font percent, font mode, viewport config, debug properties, or other app
settings.

## Built-In Defaults

Add an internal exact-package default list for domains that should be part of the
automatic path for specific packages.

Rules:

- Java source table, not JSON/raw resource and not a `switch`.
- Exact package names only in the first version.
- No prefix, wildcard, or regex matching.
- Easy to extend by adding package-to-domain entries.
- Only adds domains to the automatic path.
- Never removes base automatic domains.
- Applies only when the app has no saved custom path.
- First use case: `hyperos_native_flutter`.

If exact package matching becomes too expensive to maintain, add a new matching
strategy deliberately. Do not preemptively add prefix, wildcard, or regex
behavior in the first implementation.

Current detector support:

- DPIS can detect likely HyperOS native proxy candidates from app metadata:
  `hyperos_package`, `hyperos_app_lib_name`, and `hyperos_application_entry`.
- This is candidate evidence, not proof that the hook domain is required.
- Candidate detection does not decide whether a domain appears in the path
  editor.

UI behavior:

- Do not hide `hyperos_native_flutter`.
- Do not show whitelist/candidate labels.
- Do not add disabled or special explanatory states.
- If a package is in built-in defaults, the dialog simply shows the domain
  checked as part of the recommended path.

Open item:

- Initial exact package list for `hyperos_native_flutter`.

## App Detail UI

In the app detail sheet advanced action area, show one long row.

Title rules:

- Recommended path: `自定义链路`
- Custom path: `自定义链路(x/总数)`
- `x`: count of enabled known domains.
- `总数`: current code-level known domain count.
- Unknown saved ids are ignored by both `x` and `总数`.
- No subtitle.

The row is always clickable and always opens the dialog, regardless of current
font mode. The sheet must not expose individual domain switches.

## Dialog UI

Dialog title:

```text
字体兼容模式
```

Dialog behavior:

- Shows the current effective path on open.
- If no custom path exists, shows the automatic/recommended path.
- If a custom path exists, shows the stored custom path.
- Acts as an immediate editor.
- Has no Cancel button.
- Has no Save button.
- Has no high-risk confirmation.
- Toggling a known domain saves configuration immediately.
- Do not add debounce in the first version; each toggle may write the current
  value directly.
- If the edited known-domain set equals the current automatic path, clear the
  custom key immediately.
- If the edited known-domain set differs from the current automatic path, save
  the full selected id set immediately.
- Turning off every known domain is valid and stores an empty custom path.
- Does not restart or force-stop the target app.

Restore action:

- Located as the last item in the list.
- Long warning-outline button.
- Text: `恢复推荐配置`.
- Clears the custom key immediately and returns the UI to the recommended path.

Suggested structure:

```text
字体兼容模式

资源配置
  [x] 资源字体缩放
      resources_font
  [ ] ActivityThread 字体配置
      activity_thread_font

文本视图回退
  [ ] TextView sp 重写
      textview_sp_rewrite
  [ ] TextView 绝对字号重写
      textview_absolute_rewrite
  [ ] TextView 当前像素回退
      textview_current_px_fallback
  [ ] Paint 字号回退
      paint_text_size_fallback

网页
  [x] WebView 文字缩放
      webview_text_zoom

跨运行时
  [ ] Flutter 设置链路
      flutter_settings
  [ ] HyperOS 原生 Flutter 链路
      hyperos_native_flutter

未知链路
  [-] removed_domain_id

[ 恢复推荐配置 ]
```

## Experimental Settings Cleanup

When per-app custom domains land, remove the existing global experimental hook
switches from the experimental settings page:

- Flutter supplement master switch.
- Flutter settings hook switch.
- HyperOS native text hook switch.

No migration strategy is required.

Do not convert old global switch values into per-app custom hook paths. Do not
batch-write custom hook paths for configured apps. Old SharedPreferences values
may remain, but should stop participating in the new planner.

Post-change behavior:

- Apps without custom paths use automatic path plus built-in defaults.
- Apps with custom paths use their saved custom domain id set.
- Non-whitelisted apps that previously depended on a global experimental switch
  require the user to enable the relevant domain in that app's custom path.

Keep the experimental settings page. If no experimental functions remain, show
only a centered subdued title:

```text
暂无实验功能
```

Do not show an in-app migration notice on this page. Release notes can mention
that the old global experimental Flutter/HyperOS switches moved to per-app
custom hook domains.

## Backup And Export

Use lowest-risk preservation behavior:

- Preserve empty custom path values.
- Preserve unknown ids.
- Do not drop the custom key because its value is empty.
- Do not rewrite unknown ids during import/export.
- Only explicit restore-recommended behavior clears the custom path key.

## Logging

Add hook-domain-level logging to the app hook plan and installed summary.

Recommended fields:

- `hookDomains=...`
- `hookDomainSource=auto|custom`
- `builtinDomains=...`
- `unknownCustomDomains=...`

The logs should expose the final domain id set directly, so diagnosis does not
depend on inferring behavior from individual booleans.

## Implementation Notes

Release-note points:

- The old global Flutter/HyperOS experimental switches were removed from the UI
  and no longer participate in app-process font hook planning.
- Apps that depended on those switches should enable the corresponding
  per-app custom chain entry instead.
- `field_rewrite` uses the conservative default path described above; TextView
  and Paint fallback domains are opt-in per app.

Suggested implementation order:

1. Add the font hook domain registry.
2. Add the per-package hook-domain override store.
3. Add built-in exact-package defaults.
4. Extend package snapshots and planner input.
5. Apply custom domains in `HookExecutionPlanner`.
6. Add app detail row and dialog UI.
7. Update backup/export behavior.
8. Remove global experimental hook switches and add empty state.
9. Add logs and tests.

Suggested tests:

- Missing key uses automatic path.
- Empty key stores custom zero-domain path.
- Unknown ids are preserved on read/save/export/import.
- Restore clears custom key and unknown ids.
- Custom path overrides debug shaping only for `field_rewrite`.
- Saved path equal to automatic path clears the custom key.
- Built-in defaults apply only when no custom path exists.
- Experimental settings page shows `暂无实验功能` when empty.
