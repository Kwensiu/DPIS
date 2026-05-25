# HyperOS Native Proxy 替代方案评估

本文档独立记录 HyperOS Gallery/Weather 字体 hook 中，除了“把新版 `libdpis_native.so` 落到目标应用 native 目录”之外的替代方案、风险和取舍。

## 背景

当前 HyperOS Gallery 的 Rust/Flutter 进程不是普通 Java View/TextView 链路。DPIS 的有效链路是：

1. `system_server` 中 hook `android.os.RustProcessImpl.startRustProcess`。
2. 把原本的 Rust binary `/data/app/MIUIGallery/lib/arm64/libapp_gallery.so` 改成 sibling proxy：`/data/app/MIUIGallery/lib/arm64/libdpis_native.so`。
3. `libdpis_native.so` 再读取 `debug.dpis.rustbin.<hash>` 并 `dlopen()` 原始 `libapp_gallery.so`。
4. proxy 在目标进程内 hook `libhyper_os_flutter.so` 的 paragraph 入口。

因此，只要改动了 `app/src/main/cpp/dpis_native.cpp`，目标应用实际加载的 native 代码就必须更新。覆盖安装 DPIS APK 只能更新 APK 内的 so，不会自动更新已经落在 Gallery native 目录里的 sibling so。

## 当前确定方案：更新 sibling `libdpis_native.so`

路径示例：

```text
/data/app/MIUIGallery/lib/arm64/libdpis_native.so
```

优点：

- 与当前 HyperOS spawner/linker namespace 行为匹配。
- 已实机验证可启动、可 hook、可读取新 `debug.dpis.forcefont.<hash>`。
- 生效路径确定：Gallery 启动时实际加载的就是这个文件。
- 可做成可检测、可校验、可回滚的流程。

缺点：

- 需要 root 写目标应用安装目录。
- 改 native 代码后必须同步这个文件。
- 目标应用更新后可能覆盖或移动 native 目录。
- 固定 offset 仍可能随 Gallery/HyperOS 更新失效。

建议产品化方式：

- DPIS UI 检测目标目录是否存在 sibling proxy。
- 比较 APK 内新版 proxy 与目标目录 proxy 的 hash/size。
- 不一致时提示用户“需要更新 HyperOS native proxy”。
- 操作前 force-stop 目标应用。
- root 复制到临时路径，再复制到目标 native 目录。
- 复制后校验 hash/size。
- 提供一键回滚：删除目标目录中的 `libdpis_native.so`。

## 替代方案 1：只靠 system_server 传 env/property

做法：不更新 native proxy，只让 `system_server` 通过 env 或 system property 传递新字体百分比。

结论：只能更新配置，不能更新 native 代码。

适用场景：

- 只改字体百分比、开关、实验参数。
- 目标应用已经加载了支持这些配置的新版 proxy。

不适用场景：

- 修改了 `dpis_native.cpp` 的读取逻辑、hook 逻辑、offset 处理、trampoline 代码。
- 目标目录里的 proxy 仍是旧版本。

本轮实机证据：

- DPIS 可以写 `debug.dpis.forcefont.a55b5fe1=300`。
- 旧 `system_server` 仍会把 `debug.dpis.font.a55b5fe1` 覆盖为 `50`。
- 只有当目标目录里的新版 proxy 支持优先读取 `debug.dpis.forcefont.<hash>` 后，Gallery 才绕过旧值并得到 `multiplier=2.0`。

## 替代方案 2：让目标进程直接加载 DPIS APK 里的 so

做法：不复制到 Gallery 目录，尝试从 DPIS APK 或 DPIS 私有目录加载 `libdpis_native.so`。

结论：理论上更干净，但当前不稳定，不作为主线。

问题：

- HyperOS Rust spawner/linker namespace 对可见 library path 有限制。
- Gallery Rust 进程更容易接受自身 native 目录下的 sibling so。
- DPIS APK 私有目录通常不在 Gallery namespace 的可加载路径内。
- 即使拿到绝对路径，依赖库、namespace、SELinux 和 linker 规则也可能失败。

适用价值：

- 可作为未来研究方向。
- 如果能稳定把 DPIS APK 内 so 暴露到 Gallery namespace，就能避免写目标应用目录。

当前判断：不作为近期方案。

## 替代方案 3：LSPosed native_init / 普通 app 进程注入

做法：依赖 LSPosed native_init 或普通 app 进程 hook，在 Gallery 进程内加载 native hook。

结论：对普通 Android app 更适合，对 HyperOS Gallery Rust 主进程不稳定。

问题：

- Gallery 主界面走 HyperOS Rust/Flutter 启动模型，不完全等同普通 zygote Java app。
- 早期日志显示普通 Java/Xposed 链路能影响部分配置，但不能完全接管最终 Flutter 文本。
- 如果 native 注入时机晚于 `libhyper_os_flutter.so` 初始化，需要额外处理已加载库和 paragraph cache。

适用价值：

- 可作为辅助探针。
- 不建议替代当前 RustProcess sibling proxy 主线。

## 替代方案 4：ptrace / injector 动态注入

做法：目标 app 启动后，用 root/ptrace 或其他 injector 把 DPIS native so 注入进 Gallery 进程。

结论：能绕开文件落地，但复杂度和风险更高。

优点：

- 理论上不需要修改目标应用安装目录。
- 可以在运行时更新注入逻辑。

问题：

- 需要维护额外 native injector。
- 受 SELinux、ptrace 限制、进程时机、Android 版本差异影响。
- 失败模式复杂，容易导致目标进程崩溃。
- 仍要解决 linker namespace 和依赖库可见性。

当前判断：不适合当前阶段。

## 替代方案 5：替换原始 `libapp_gallery.so`

做法：直接改名/替换 Gallery 原始 Rust binary，把 DPIS proxy 伪装成原始 so。

结论：不建议。

风险：

- 破坏目标应用原始文件，回滚成本高。
- 目标应用更新、完整性校验、路径变化都会放大风险。
- 一旦 proxy 失败，Gallery 可能完全无法启动。
- 比新增 sibling `libdpis_native.so` 更侵入。

当前判断：禁止作为主线。

## 替代方案 6：把 native 逻辑尽量配置化

做法：减少以后修改 `dpis_native.cpp` 的次数，把更多行为改成 property/file 配置：

- 字体百分比。
- 是否启用 pushStyle。
- 是否启用某些实验 hook。
- 目标包策略。
- 日志预算。

结论：推荐作为中期优化，但不能替代首次部署 proxy。

优点：

- 一旦目标目录里的 proxy 足够通用，后续大部分调参不需要再复制 so。
- 可以降低“改 native 后必须同步目标目录文件”的频率。

限制：

- 如果 hook 入口、trampoline、ABI 处理、offset 解析有变化，仍要更新 native so。
- 旧 proxy 不会自动拥有新配置能力。

当前状态：已经开始采用，例如 `debug.dpis.forcefont.<hash>`。

## 替代方案 7：动态 offset / 签名扫描

做法：native proxy 不再依赖固定 offset，而是扫描 `libhyper_os_flutter.so` 中的签名或符号特征定位 hook 点。

结论：可提升跨版本稳定性，但仍需要 proxy 文件本身部署到目标目录。

优点：

- Gallery 更新后不一定需要重新编译 offset。
- 可降低崩溃和无效 hook 的概率。

问题：

- 签名扫描实现复杂。
- 误匹配风险高。
- 不解决“目标进程要加载新版 proxy”的问题。

当前判断：后续可做，但不是替代 sibling proxy 更新的方案。

## 结论

目前唯一确定、已验证、可解释的 native 代码更新方式是：更新目标应用 native 目录中的 sibling `libdpis_native.so`。

但需要区分两类变更：

- 只改配置值：用 `debug.dpis.forcefont.<hash>` 等运行时配置即可，不需要复制 so。
- 改 native 代码：必须更新目标目录里的 sibling proxy，否则目标应用继续运行旧 native 逻辑。

因此，后续 DPIS 应该把 sibling proxy 更新做成显式功能，而不是隐藏行为：检测、提示、复制、校验、回滚，全流程可见。


## Current Decision

- Keep sibling native proxy as the supported path.
- Do not keep the external-path or `LD_PRELOAD` experiment branches in normal hook code.
- Keep runtime system properties for repeated value updates after the proxy path is prepared.
- Keep detailed experiment logs out of active docs; archive them only when they are still useful for future research.
