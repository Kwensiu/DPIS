package com.dpis.module.viewport.window

/**
 * Optional ColorOS task evidence. All dependencies are resolved reflectively so the common
 * runtime remains usable on AOSP and other vendor frameworks.
 */
object ColorOsWindowScopeDetector : WindowScopeDetector {
    override fun detect(input: WindowScopeInput): WindowScopeDecision? {
        val taskInfo = input.taskInfo ?: return null
        if (isFlexibleFloatingWindow(taskInfo)) {
            return WindowScopeDecision(
                kind = WindowScopeKind.WINDOW,
                evidence = WindowScopeEvidence.COLOROS_TASK_UTILS,
                mode = WindowMode.FREEFORM,
            )
        }
        val mode = readTaskWindowingMode(taskInfo)
        if (mode != null && mode > 1) {
            return WindowScopeDecision(
                kind = WindowScopeKind.WINDOW,
                evidence = WindowScopeEvidence.COLOROS_WINDOWING_MODE,
                mode = if (mode == 5) WindowMode.FREEFORM else WindowMode.SPLIT_SCREEN,
            )
        }
        return null
    }

    private fun isFlexibleFloatingWindow(taskInfo: Any): Boolean {
        val taskUtils = runCatching {
            Class.forName(
                "com.android.systemui.shared.recents.utilities.TaskUtils",
                false,
                taskInfo.javaClass.classLoader,
            )
        }.getOrNull()
        if (taskUtils != null) {
            val method = taskUtils.methods.firstOrNull {
                it.name == "isFlexibleFloatingWindow" && it.parameterTypes.size == 1
            }
            val result = runCatching { method?.invoke(null, taskInfo) }.getOrNull()
            if (result is Boolean && result) return true
        }
        if (readTaskWindowingMode(taskInfo) == 5) return true
        val config = readObject(taskInfo, "getConfiguration") ?: return false
        val windowConfiguration = readObject(config, "getWindowConfiguration") ?: return false
        return readInt(windowConfiguration, "getWindowingMode") == 5
    }

    private fun readTaskWindowingMode(taskInfo: Any): Int? {
        val direct = readInt(taskInfo, "getWindowingMode")
        if (direct != null) return direct
        val config = readObject(taskInfo, "getConfiguration") ?: return null
        val windowConfiguration = readObject(config, "getWindowConfiguration") ?: return null
        return readInt(windowConfiguration, "getWindowingMode")
    }

    private fun readObject(target: Any, methodName: String): Any? = runCatching {
        target.javaClass.getDeclaredMethod(methodName).also { it.isAccessible = true }
            .invoke(target)
    }.getOrNull()

    private fun readInt(target: Any, methodName: String): Int? =
        readObject(target, methodName) as? Int
}
