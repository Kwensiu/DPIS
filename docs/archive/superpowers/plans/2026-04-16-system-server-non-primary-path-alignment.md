# System Server 非主路径对齐 实施计划

> **给代理执行者：** 必选子技能：使用 `superpowers:subagent-driven-development`（推荐）或 `superpowers:executing-plans` 按任务逐项执行本计划。步骤使用复选框（`- [ ]`）语法跟踪。

**目标：** 让已配置应用在 `activity-start` 与 `config-dispatch` 中持续上报 `actual` 与 `target` 对齐，并在设备上完成验证。

**架构：** 将行为集中在 `SystemServerDisplayEnvironmentInstaller`，通过小型测试接缝覆盖变更策略与环境选择。先用聚焦单测锁定行为，再验证真机构建的运行时日志。若运行时仍有偏差，仅对两个目标入口做窄范围字段级回退。

**技术栈：** Java、Android Gradle Plugin、JUnit4、LSPosed 运行时日志

---

### 任务 1： 用单元测试锁定变更时机行为

**文件：**
- 修改： `singleapk/src/main/java/com/dpis/module/SystemServerDisplayEnvironmentInstaller.java`
- 新增： `singleapk/src/test/java/com/dpis/module/SystemServerDisplayEnvironmentInstallerMutationPolicyTest.java`
- 测试： `singleapk/src/test/java/com/dpis/module/SystemServerDisplayEnvironmentInstallerMutationPolicyTest.java`

- [ ] **步骤 1： 编写失败测试**

```java
package com.dpis.module;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SystemServerDisplayEnvironmentInstallerMutationPolicyTest {
    @Test
    public void preProceedEnabledForConfigDispatchAndActivityStartOnly() {
        assertTrue(SystemServerDisplayEnvironmentInstaller
                .shouldApplyPreProceedMutationsForTest("config-dispatch"));
        assertTrue(SystemServerDisplayEnvironmentInstaller
                .shouldApplyPreProceedMutationsForTest("activity-start"));
        assertFalse(SystemServerDisplayEnvironmentInstaller
                .shouldApplyPreProceedMutationsForTest("display-policy-layout"));
    }

    @Test
    public void postProceedEnabledForAllEntries() {
        assertTrue(SystemServerDisplayEnvironmentInstaller
                .shouldApplyPostProceedMutationsForTest("config-dispatch"));
        assertTrue(SystemServerDisplayEnvironmentInstaller
                .shouldApplyPostProceedMutationsForTest("activity-start"));
        assertTrue(SystemServerDisplayEnvironmentInstaller
                .shouldApplyPostProceedMutationsForTest("display-content-config"));
    }
}
```

- [ ] **步骤 2： 运行测试并确认失败**

运行： `.\gradlew :singleapk:testDebugUnitTest --tests com.dpis.module.SystemServerDisplayEnvironmentInstallerMutationPolicyTest`  
预期： 失败 ，报 unresolved `shouldApplyPreProceedMutationsForTest` / `shouldApplyPostProceedMutationsForTest`。

- [ ] **步骤 3： 编写最小实现**

```java
// In SystemServerDisplayEnvironmentInstaller.java
static boolean shouldApplyPreProceedMutationsForTest(String entryName) {
    return shouldApplyPreProceedMutations(entryName);
}

static boolean shouldApplyPostProceedMutationsForTest(String entryName) {
    return shouldApplyPostProceedMutations(entryName);
}
```

- [ ] **步骤 4： 运行测试并确认通过**

运行： `.\gradlew :singleapk:testDebugUnitTest --tests com.dpis.module.SystemServerDisplayEnvironmentInstallerMutationPolicyTest`  
预期： 通过.

- [ ] **步骤 5： 提交**

```bash
git add singleapk/src/main/java/com/dpis/module/SystemServerDisplayEnvironmentInstaller.java singleapk/src/test/java/com/dpis/module/SystemServerDisplayEnvironmentInstallerMutationPolicyTest.java
git commit -m "test(singleapk): lock pre/post mutation policy for non-primary paths"
```

### 任务 2： 为有效环境选择补充回归测试

**文件：**
- 修改： `singleapk/src/main/java/com/dpis/module/SystemServerDisplayEnvironmentInstaller.java`
- 新增： `singleapk/src/test/java/com/dpis/module/SystemServerDisplayEnvironmentInstallerEnvironmentSelectionTest.java`
- 测试： `singleapk/src/test/java/com/dpis/module/SystemServerDisplayEnvironmentInstallerEnvironmentSelectionTest.java`

- [ ] **步骤 1： 编写失败测试**

```java
package com.dpis.module;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SystemServerDisplayEnvironmentInstallerEnvironmentSelectionTest {
    @Test
    public void keepsPreEnvironmentWhenPostEnvironmentMissing() {
        String selected = SystemServerDisplayEnvironmentInstaller.selectEnvironmentSourceForTest(
                true,  // hasPre
                false  // hasPost
        );
        assertEquals("pre", selected);
    }

    @Test
    public void prefersPostEnvironmentWhenAvailable() {
        String selected = SystemServerDisplayEnvironmentInstaller.selectEnvironmentSourceForTest(
                true,
                true
        );
        assertEquals("post", selected);
    }
}
```

- [ ] **步骤 2： 运行测试并确认失败**

运行： `.\gradlew :singleapk:testDebugUnitTest --tests com.dpis.module.SystemServerDisplayEnvironmentInstallerEnvironmentSelectionTest`  
预期： 失败 ，报 unresolved `selectEnvironmentSourceForTest`。

- [ ] **步骤 3： 编写最小实现**

```java
// In SystemServerDisplayEnvironmentInstaller.java
static String selectEnvironmentSourceForTest(boolean hasPre, boolean hasPost) {
    if (hasPost) {
        return "post";
    }
    return hasPre ? "pre" : "none";
}
```

- [ ] **步骤 4： 运行测试并确认通过**

运行： `.\gradlew :singleapk:testDebugUnitTest --tests com.dpis.module.SystemServerDisplayEnvironmentInstallerEnvironmentSelectionTest`  
预期： 通过.

- [ ] **步骤 5： 提交**

```bash
git add singleapk/src/main/java/com/dpis/module/SystemServerDisplayEnvironmentInstaller.java singleapk/src/test/java/com/dpis/module/SystemServerDisplayEnvironmentInstallerEnvironmentSelectionTest.java
git commit -m "test(singleapk): add effective environment selection regression tests"
```

### 任务 3： 设备验证：`activity-start` 与 `config-dispatch`

**文件：**
- 修改： `singleapk/src/main/java/com/dpis/module/SystemServerDisplayDiagnostics.java`
- 新增： `docs/system-smoke-status.md`
- 测试： 基于 LSPosed 输出的运行时日志验证

- [ ] **步骤 1： 为本轮验证增加构建标记**

```java
// In SystemServerDisplayDiagnostics.java
static final String BUILD_MARKER = "2026-04-16-non-primary-alignment-v9";
```

- [ ] **步骤 2： 构建并安装**

运行： `.\gradlew :singleapk:assembleDebug`  
预期： `构建成功`.

运行（示例）： `adb -s aeec529f install -r singleapk\build\outputs\apk\debug\singleapk-debug.apk`  
预期： `成功`.

- [ ] **步骤 3： 启动目标应用后抓取运行日志**

运行： `adb -s aeec529f logcat -d | findstr /c:"system_server probe:" /c:"system_server config fallback:" /c:"marker=" > logs\system-server-v9.txt`  
预期： 日志文件包含 `marker=2026-04-16-non-primary-alignment-v9`。

- [ ] **步骤 4： 验证对齐准则**

运行： `findstr /c:"entry=activity-start" /c:"entry=config-dispatch" logs\system-server-v9.txt`  
预期： 每条选中应用记录都显示 `actual=` 与 `target=` 对齐（配置维度）；当主包缺少配置时，回退记录仍然有效。

- [ ] **步骤 5： 更新冒烟报告并提交**

```markdown
# docs/system-smoke-status.md
- Date: 2026-04-16
- Build marker: 2026-04-16-non-primary-alignment-v9
- Checks:
  - activity-start actual vs target: PASS/FAIL
  - config-dispatch actual vs target: PASS/FAIL
  - config fallback presence: PASS/FAIL
```

```bash
git add singleapk/src/main/java/com/dpis/module/SystemServerDisplayDiagnostics.java docs/system-smoke-status.md logs/system-server-v9.txt
git commit -m "chore(singleapk): validate non-primary path alignment on device"
```



