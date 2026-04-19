# 字体独立引擎重构方案（可与 DPI 叠加）

## 目标
- 字体配置永远独立于 DPI 配置，可单独生效。
- 允许 DPI 与字体叠加，不做“总效果去重”。
- 不新增设置心智：仍是每应用一个字体百分比。

## 约束
- 不要求 `system_server hooks` 参与字体主路径。
- 字体主路径放在目标应用进程。
- `TextView/Paint/WebView/X5` 仅作兜底，不作为唯一主路径。

## 新架构（双引擎）
- `ViewportEngine`：只改 `density/width/height`（现有链路）。
- `FontEngine`：只改 `fontScale/scaledDensity`。
- 两者都可开启，按“先配置后渲染”自然叠加。

## 字体模式（三态）
- `系统伪装`：仅启用 FontEngine 主路径（`fontScale/scaledDensity`）。
- `字段替换`：仅启用单条备用线（当前统一为 `Paint#setTextSize`）。
- `关闭`：不启用任何字体改动。
- 模式按应用单独保存；历史仅有字体百分比配置自动迁移为 `系统伪装`。

## FontEngine 主路径
1. `ActivityThread.handleBindApplication`：写入 `AppBindData.config.fontScale`。
2. `ResourcesImpl.updateConfiguration`：保证 `fontScale` 和 `scaledDensity` 同步。
3. `Resources.getConfiguration/getDisplayMetrics/getSystem`：读路径一致化。

## FontEngine 兜底路径
- `TextView#setTextSize`、`Paint/TextPaint#setTextSize`。
- `WebView/X5 WebSettings#setTextZoom` 与 H5 CSS 注入。

## 本轮实施（第一批）
- [x] 文档落地。
- [x] 新增 `ActivityThreadFontHookInstaller` 并接入字体主线。
- [x] 字体策略改为按应用三态：`系统伪装` / `字段替换` / `关闭`。
- [x] 备用线收敛为单条 `Paint#setTextSize`。
- [x] 单测与全量编译通过。

## 验收标准
- 仅设置字体（不设 DPI）时：目标应用文字明显变化。
- 同时设置 DPI 与字体时：允许叠加放大。
- 字体配置在更新后保留，不被覆盖为默认值。
