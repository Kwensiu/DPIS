# Hook Chain Dialog UI Design

## Context

The per-app sheet currently exposes a `Hook 链路` entry. Its dialog title and
content are still centered around font compatibility, even though viewport apply
strategy is now managed there too.

## Decisions

- Keep the sheet button label as `Hook 链路`.
- Rename the dialog title from `字体兼容模式` to `自定义 Hook 链路`.
- Add a two-tab selector at the top of the dialog:
  - `界面`
  - `字体`
- The `界面` tab is shown by default.
- The current viewport apply strategy rows move under the `界面` tab.
- Rename the viewport apply section title to `界面比例应用策略`.
- The current font hook domain controls move under the `字体` tab without
  changing their behavior.

## Copy Notes

User-facing copy should avoid `视口` where possible. In this UI, use:

- `界面` for the high-level tab.
- `界面比例` for the concrete scale-related setting.
- Keep internal code names such as `viewport` unchanged unless a later refactor
  explicitly targets domain terminology.

## Out Of Scope

- No runtime hook behavior changes.
- No migration changes.
- No changes to the main sheet's viewport input semantics.
