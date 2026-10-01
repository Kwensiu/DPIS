package com.dpis.module.viewport

import com.dpis.module.config.DpisConfigStore
import com.dpis.module.runtime.transport.RootCommandRunner

object ViewportPropertySyncer {
    @JvmStatic
    fun publishTargetAsync(packageName: String, widthDp: Int) =
        publishTargetAsync(packageName, widthDp, ViewportApplyMode.SYSTEM_EMULATION)

    @JvmStatic
    fun publishTargetAsync(packageName: String, widthDp: Int, mode: String) =
        publishTargetAsync(packageName, ViewportTargetSpec.absoluteDp(widthDp), mode)

    @JvmStatic
    fun publishTargetAsync(packageName: String?, targetSpec: ViewportTargetSpec?, mode: String?) {
        if (packageName.isNullOrBlank() || targetSpec == null || !targetSpec.isEnabled()) return
        Thread(
            { runRootCommand(buildCompatConfigCommand(packageName, targetSpec, mode)) },
            "DPIS-viewport-property-publisher"
        ).apply { isDaemon = true }.start()
    }

    @JvmStatic
    fun clearTargetAsync(packageName: String?) {
        if (packageName.isNullOrBlank()) return
        Thread(
            { runRootCommand(buildClearCommand(packageName)) },
            "DPIS-viewport-property-cleaner"
        ).apply { isDaemon = true }.start()
    }

    @JvmStatic
    fun syncConfiguredTargetsAsync(store: DpisConfigStore?) {
        if (store == null) return
        Thread({ syncConfiguredTargets(store) }, "DPIS-viewport-property-syncer").apply {
            isDaemon = true
        }.start()
    }

    @JvmStatic
    fun syncConfiguredTargets(store: DpisConfigStore?) {
        if (store == null) return
        buildConfiguredTargetsCommand(store).takeIf { it.isNotEmpty() }?.let(::runRootCommand)
    }

    @JvmStatic
    fun syncTarget(packageName: String?, store: DpisConfigStore?) {
        if (packageName.isNullOrBlank() || store == null) return
        runRootCommand(
            buildCompatConfigCommand(
                packageName,
                if (store.isTargetDpisEnabled(packageName)) store.getTargetViewportSpec(packageName) else ViewportTargetSpec.off(),
                store.getTargetViewportApplyMode(packageName)
            )
        )
    }

    @JvmStatic
    fun buildSetCommandForTest(property: String, widthDp: Int) = buildSetCommand(property, widthDp)
    @JvmStatic
    fun buildCompatConfigCommandForTest(packageName: String, widthDp: Int, mode: String) =
        buildCompatConfigCommand(packageName, ViewportTargetSpec.absoluteDp(widthDp), mode)

    @JvmStatic
    fun buildCompatConfigCommandForTest(
        packageName: String,
        targetSpec: ViewportTargetSpec,
        mode: String
    ) = buildCompatConfigCommand(packageName, targetSpec, mode)

    private fun buildCompatConfigCommand(packageName: String, widthDp: Int, mode: String) =
        buildCompatConfigCommand(packageName, ViewportTargetSpec.absoluteDp(widthDp), mode)

    private fun buildCompatConfigCommand(
        packageName: String,
        targetSpec: ViewportTargetSpec,
        mode: String?
    ): String {
        val enc = ViewportPropertyProjection.encode(targetSpec, mode)
        fun pair(property: String, persistent: String, value: String) =
            "${buildSetCommand(property, value)}; ${buildSetCommand(persistent, value)}"
        return listOf(
            pair(
                ViewportPropertyBridge.propertyNameForPackage(packageName),
                ViewportPropertyBridge.persistentPropertyNameForPackage(packageName),
                enc.systemEmulationValue.toString()
            ),
            pair(
                ViewportPropertyBridge.targetTypePropertyNameForPackage(packageName),
                ViewportPropertyBridge.persistentTargetTypePropertyNameForPackage(packageName),
                enc.targetType
            ),
            pair(
                ViewportPropertyBridge.scalePropertyNameForPackage(packageName),
                ViewportPropertyBridge.persistentScalePropertyNameForPackage(packageName),
                enc.scaleMilliPercent.toString()
            ),
            pair(
                ViewportPropertyBridge.compatConfigPropertyNameForPackage(packageName),
                ViewportPropertyBridge.persistentCompatConfigPropertyNameForPackage(packageName),
                enc.compatConfigValue.toString()
            ),
            pair(
                ViewportPropertyBridge.compatModePropertyNameForPackage(packageName),
                ViewportPropertyBridge.persistentCompatModePropertyNameForPackage(packageName),
                enc.compatMode
            ),
        ).joinToString("; ")
    }

    private fun buildConfiguredTargetsCommand(store: DpisConfigStore): String =
        store.getConfiguredPackages().joinToString("; ") { packageName ->
            val name = packageName ?: ""
            buildCompatConfigCommand(
                name,
                if (store.isTargetDpisEnabled(name)) store.getTargetViewportSpec(name) else ViewportTargetSpec.off(),
                store.getTargetViewportApplyMode(name)
            )
        }

    private fun buildClearCommand(packageName: String) =
        buildCompatConfigCommand(packageName, 0, ViewportApplyMode.OFF)

    private fun buildSetCommand(property: String, widthDp: Int) =
        buildSetCommand(property, widthDp.toString())

    private fun buildSetCommand(property: String, value: String) =
        "setprop ${shellQuote(property)} ${shellQuote(value)}"

    private fun runRootCommand(command: String) = RootCommandRunner.run(command)
    private fun shellQuote(value: String) =
        if (value.isEmpty()) "''" else "'${value.replace("'", "'\\''")}'"
}
