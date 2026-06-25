## `ResourcesImpl updateConfiguration` Hook 方案

### 背景
模块已经在目标包内安装了 `ResourcesManager.applyConfigurationToResources` 的 Hook。第二个 Hook 也必须在 `onPackageReady` 阶段安装，保持相同的包过滤条件，并且严格限定在本次请求的最小 PoC 范围内。新 Hook 需要在原始 `ResourcesImpl.updateConfiguration(Configuration, DisplayMetrics, CompatibilityInfo)` 调用 _之前_ 执行，并按现有覆盖逻辑对传入的 configuration/metrics 做同类调整。

### 方案选项

1. **复用现有 `ResourcesManager` hook 模式，新增独立安装器。** 创建 `ResourcesImplHookInstaller`，通过反射定位 `ResourcesImpl.updateConfiguration(...)`，并像第一个 hook 一样通过 Xposed 拦截。在拦截器内部调整 `Configuration.densityDpi`，并在 `DisplayMetrics` 非空时同步 `densityDpi`、`density`、`scaledDensity`。
2. **在更高层入口 hook（例如 `ResourcesImpl.getDisplayMetrics`）。** 不拦截 `updateConfiguration`，而是 hook 其他入口并在读取时修补 metrics。这样会把逻辑分散到更多调用点，使 PoC 超出本次需求范围。
3. **复用 `ResourcesManager` Hook 触发标记，仅在检测到标记时修补 `ResourcesImpl`。** 会引入不必要的状态跟踪，对本任务没有额外收益。

**推荐：** 方案 1。它能保持实现聚焦、复用当前安装器模式，并满足“在 `updateConfiguration` 执行前安装第二个 Hook”的要求。

### 选定设计

1. **新增安装器。** `ResourcesImplHookInstaller` 将镜像 `ResourcesManagerHookInstaller` 的结构。它会加载 `android.content.res.ResourcesImpl` 和 `CompatibilityInfo`，Hook `updateConfiguration` 并拦截调用。进入 `proceed` 前调用辅助方法：解析配置中的 density 覆盖值，改写 `Configuration.densityDpi`，并在存在 `DisplayMetrics` 时刷新 `DisplayMetrics.densityDpi`、`density`、`scaledDensity`。同时记录覆盖日志以便排查。
2. **安装时机。** `ModuleMain.onPackageReady` 保持原包过滤逻辑，并在同一个 `try` 块内调用两个安装器，保证包进入 ready 状态前两个 Hook 都完成注册。任一 Hook 失败都通过 `DpisLog.e` 记录，但不导致宿主崩溃。
3. **辅助方法可见性。** 修改 `Configuration`/`DisplayMetrics` 的 helper 使用 package-private，可供单元测试直接覆盖，不引入新的公开 API。

### 测试

1. 新增 `ResourcesImplHookInstallerTest`，覆盖辅助方法：
   * 验证覆盖生效时 `Configuration.densityDpi` 被改写，且 `DisplayMetrics` 为空时保持不变。
   * 验证传入非空 metrics 时，`DisplayMetrics.densityDpi`、`density`、`scaledDensity` 被同步更新。
2. 执行 `./gradlew :app:testDebugUnitTest`，确认单元测试可基于 Android stubs 编译并通过。

### 备注

- 本次改动仅限请求中的 Hook 范围；不会引入额外 Hook 或重构。
- density 调整复用 `DensityOverride`，确保两个 Hook 行为一致。
- 该规范默认目标包仍为 `com.android.settings`。即使配置变化，hook 边界也保持不变。

该规范已写入并保存到 `docs/superpowers/specs/2026-04-14-resourcesimpl-hook-design.md`。请先审阅，如需调整我再修改文案后进入实现细节规划。

