# DPIS Yuki 迁移状态（归档）

## 本轮完成

`DPIS` 主应用已开始将 `system` 路径迁移到 YukiHookAPI。本轮已完成：

- 在 `:app` 增加 YukiHookAPI、KSP、KavaRef 依赖。
- 新增 `com.dpis.module.HookEntry`，并使用 `@InjectYukiHookWithXposed`。
- 增加 `loadSystem` 入口标记：`dpis-yuki-system-v1`。
- 移除主应用中的旧入口 `app/src/main/assets/xposed_init`，避免继续把 legacy 入口作为主路径。
- 将主应用配置存储切换为模块进程内本地 `SharedPreferences`。
- 新增 `XSharedPreferencesAdapter`，让 Xposed/Yuki 宿主侧可读取同一配置文件。

## 仍待完成

`SystemServerDisplayEnvironmentInstaller` 的 system hook 安装逻辑仍依赖 libxposed 的 `XposedInterface` 拦截模型。

这意味着本轮只完成了“入口迁移”和“配置读取迁移”，尚未完成最终的 hook 注册迁移。

下一步：

- 将 `SystemServerDisplayEnvironmentInstaller` 重构为与 hook 框架无关的核心层。
- 为同一组目标补齐 Yuki `loadSystem` 的注册入口。
- 在 `ModuleMain` 中下线旧的 libxposed system 分发路径。

## 验证目标

安装新版本 `DPIS` 后，LSPosed 日志应显示 `com.dpis.module` 通过 Yuki 生成入口在 `system` 执行，并输出 `dpis-yuki-system-v1` 标记。
