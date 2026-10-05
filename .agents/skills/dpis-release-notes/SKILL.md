---
name: dpis-release-notes
description: >
  Rewrite DPIS release notes for ordinary users. Use when the user pastes
  release-please output, asks for a changelog, 更新日志, or 发布说明, or asks
  to write the latest GitHub release notes. Also use for /dpis-release-notes.
---

# DPIS release notes

Write the GitHub release body that ordinary users read. Release-please output is a candidate list, not the wording.

## Gather

1. Take the version range from the release-please draft, or from the previous release tag to the current branch tip.
2. Read each commit's full message, including the body. A title alone is not enough.
3. Also check commits the draft omitted, including `chore` commits, for a user-visible result such as a donation-list update.
4. When a user-facing name is unclear, use the label in `app/src/main/res/values-zh-rCN/strings.xml`. Prefer 界面比例, 最小宽度, 系统模式, 兼容模式, 字体缩放, 微信 DPI, 小窗, and 分屏. Keep DPIS, DPI, and Material names as the product already shows them.

## What to keep

- A new capability or a visible page change goes under 新增功能. State the result, such as faster startup or an updated page.
- A bug fix names the problem that was fixed, in one line. Use 部分场景 when the fix does not cover every case.
- Put the performance cost of an area in that area's group. Use 继续降低 when the work continues an earlier optimization.
- Merge symptoms that are one problem for the user. Content past the window edge and an empty gap are one small-window item.
- Smaller adjustments go under 其他: WeChat timing across multiple scale settings, a WeChat version-list entry that speeds initialization, app-list selection, and a donation-list update.
- Drop pure refactors, language migrations, diagnostic scripts, and benchmark harnesses. If a maintainer change has a user-visible result, write only that result.

## Voice

One line per item. Name the result or the problem. Do not explain the corrected behavior, the mechanism, hook paths, densities, or before/after numbers.

Write:

```markdown
- 修复系统模式部分场景重复缩放问题
```

Avoid:

```markdown
- 系统模式下，全屏界面比例只计算一次。例如 120% 时，360dp 停留在 432dp。
```

When the user has already rewritten lines, keep that wording and only add or translate what they asked for.

## Output

Return one fenced `markdown` block and nothing else around it that the user must delete before pasting.

- Chinese first. Then a blank line, `---`, a blank line, and the English mirror.
- `###` for 新增功能 and 修复. English mirrors are `### Features` and `### Fixes`.
- `####` for groups such as 输入, 界面比例, 字体缩放, and 其他.
- Leave a blank line between bullets.
- Chinese lines have no final period. English lines are complete sentences with a final period.
- Same groups, order, and count in both languages.
- No commit hashes, pull-request numbers, or compare links.
- Do not edit `CHANGELOG.md` or publish a release unless the user asks.
