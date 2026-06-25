# Template Workspace UI Polish Design

## Context

The template workspace currently uses a simple Material card layout in `template_workspace.xml`. Each card shows a title, a multiline text summary, and text buttons. This works functionally, but the summary is harder to scan as template configuration grows because viewport, font, typeface, route, hook-chain, and missing-font states all compete in one text block.

This design keeps the existing DPIS UI direction from `docs/ui-guidelines.md`: reuse the current XML layout style, avoid a new page pattern, and keep the change local to the template workspace.

## Goals

- Keep the current card-based template workspace structure.
- Make configured template values easier to scan.
- Reduce action row clutter without turning the page into a dense management console.
- Keep implementation scope limited to the template workspace and template cards.

## Non-Goals

- Do not redesign `activity_quick_template_edit.xml`.
- Do not redesign `activity_quick_template_targets.xml`.
- Do not change template save, apply, selection, or global prefill business rules.
- Do not introduce a broad app-wide chip or icon-button design system.

## Selected Direction

Use the current card layout as the base, but replace multiline summaries with low-emphasis chip rows that show only configured values.

The visual character should stay close to the existing page: Material cards, existing spacing rhythm, restrained surfaces, and simple actions. The page should not become a table, dashboard, or dense console.

## Summary Chips

`TemplateWorkspaceBinder.buildSummary()` should move from returning one multiline string to producing structured summary items. Each item represents one configured value or state.

Render chips only for configured values:

- Viewport scale or width.
- Viewport apply route.
- Font scale.
- Font route.
- Typeface.
- Custom font hook chain.
- Missing font warning.

Unconfigured dimensions must not render placeholder chips. If a card has no configured values, show a light empty state instead of listing every missing dimension.

Chip treatment:

- Low-saturation surface by default.
- Slight emphasis for primary configuration values such as viewport or font scale.
- Warning treatment for missing font.
- Text remains localized through existing string resources or narrowly-scoped new strings.

## Actions

Global prefill card:

- Keep only one round icon button using `ic_edit_24`.
- The button opens global prefill editing.
- Remove the workspace-level reset action. Reset remains available inside the global prefill edit flow.

Template card action row, left to right:

- Round icon button with `ic_checklist_rtl_24`: open selected target apps for the template.
- Round icon button with `ic_edit_24`: edit the template.
- Spacer.
- Right-aligned Apply text button, kept as the primary action and restyled as a more rounded capsule.

All icon buttons must have stable touch target sizing and `contentDescription` strings. If tooltips are already supported by local patterns, they may mirror the content descriptions, but accessibility text is required.

## Implementation Shape

Keep the XML hierarchy close to the current `template_workspace.xml` and `item_quick_template_card.xml`.

Expected implementation units:

- A small summary item model local to template workspace binding code.
- A summary-chip rendering helper that fills a horizontal/wrapping container.
- Updated global prefill card actions in `template_workspace.xml`.
- Updated quick template card actions in `item_quick_template_card.xml`.
- Scoped drawables/styles/dimens for round icon buttons, chip surfaces, and the capsule apply button where existing resources do not fit.

Avoid shared abstractions unless the same component is needed in at least two local places in this feature.

## Testing

Add or update focused tests around summary item generation:

- No configured values produces the empty state.
- Each configured viewport/font/typeface/hook-chain value produces the expected localized item.
- Missing font produces a warning item.
- Unconfigured dimensions are omitted.

Update layout/source smoke tests only where they intentionally guard IDs, strings, or structural wiring for the template workspace.

Manual verification should include:

- Light and dark theme visual scan.
- English and Simplified Chinese strings.
- Cards with no values, one value, several values, and missing-font warning.
- Narrow screen behavior for wrapping chip rows and the icon/action row.

## Open Decisions

No open product decisions remain for this polish pass.
