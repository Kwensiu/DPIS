# 2026-04-23 Help Tutorial Dialog Card Design

## Goal

Rebuild the `MainActivity` help tutorial dialog as a dedicated two-card explanation sheet so the content reads as quick-scan mode guidance instead of one long markdown block.

## Scope

In scope:
- Only the help dialog opened from the list page help FAB.
- Dedicated layout, strings, and binding logic for this dialog.
- Preserve a single primary close button with the `确认` label.

Out of scope:
- Reworking `RichTextDialog` or `RichTextRenderer`.
- Redesigning other dialogs.
- Turning the help dialog into a mode selector.

## Approved Structure

The dialog body has three regions, top to bottom:
1. `伪装模式` explanation card.
2. `替换模式` explanation card.
3. Bottom confirmation button.

There is no title row and no introductory copy inside the custom layout.

## Card Content

### Card 1: `伪装模式`
- Badge: `更接近原生`
- Main sentence: `通过系统层链路伪装相关参数，显示效果通常更自然。`
- Bullets:
  - `需开启系统层 Hook`
  - `需勾选目标应用`
  - `需目标应用支持`

### Card 2: `替换模式`
- Badge: `覆盖更强`
- Main sentence: `通过字段重写直接覆盖缩放，生效更直接。`
- Bullets:
  - `仅需勾选目标应用`
  - `更容易出现布局错位或缩放异常`

## Visual Direction

- Two vertically stacked rounded cards.
- `伪装模式` uses a light cool background.
- `替换模式` uses a light warm background.
- Each card exposes four levels: mode title, badge, main sentence, bullet list.
- The primary button remains visually secondary to the cards.
- The dialog should stay readable on one screen when possible and scroll only on smaller displays or large font settings.

## Implementation Notes

- Replace the current help entry path so `MainActivity` opens a dedicated help dialog helper instead of a markdown-style rich text dialog.
- Keep the implementation focused on the dedicated helper/layout path; do not introduce a shared rich text abstraction for this change.
- Split help strings into card-specific resources rather than a single markdown blob.

## Validation

- Help FAB opens the dedicated card layout.
- The dialog shows two cards and one `确认` button.
- The old markdown tutorial string is no longer required by the help entry flow.
- The layout remains scrollable on constrained screens.
