# Reference Sources

本目录存放与项目直接相关的本地参考资料，优先用于离线查阅和 `rg` 检索。

## 目录说明

- `LSPosed.wiki/`
  - 来源：`https://github.com/LSPosed/LSPosed.wiki.git`
  - 用途：现代 Xposed API 使用说明、模块作用域、Native Hook、配置共享说明。
- `libxposed-api/`
  - 来源：`https://github.com/libxposed/api.git`
  - 用途：主 API 源码、Javadoc、本地接口定义。
- `libxposed-example/`
  - 来源：`https://github.com/libxposed/example.git`
  - 用途：最小模块结构、入口声明、Gradle 配置、现代 API 用法示例。
- `libxposed-helper/`
  - 来源：`https://github.com/libxposed/helper.git`
  - 用途：现代 API 的辅助封装，后续用于降低反射和样板代码成本。
- `libxposed-service/`
  - 来源：`https://github.com/libxposed/service.git`
  - 用途：模块 app 与 Hook 进程之间的现代配置/文件/偏好通信能力。
- `aosp-android16/`
  - 来源：Android 16 QPR2 AOSP Gitiles 指定文件快照
  - 用途：核对 `densityDpi`、`ResourcesManager`、`ResourcesImpl`、`ActivityThread` 的实际框架链路。

## 当前重点文件

- `aosp-android16/frameworks/base/core/java/android/app/ActivityThread.java`
- `aosp-android16/frameworks/base/core/java/android/app/ResourcesManager.java`
- `aosp-android16/frameworks/base/core/java/android/content/res/ResourcesImpl.java`
- `aosp-android16/frameworks/base/core/java/android/content/res/Configuration.java`
- `aosp-android16/frameworks/base/core/java/android/util/DisplayMetrics.java`

## 更新建议

- Git 仓库类参考直接执行 `git -C <path> pull --ff-only`
- `libxposed-api` 更新后重新执行 `.\gradlew :api:androidJavadoc`
- AOSP 快照仅在需要重新对齐 Android 16 版本时再更新
