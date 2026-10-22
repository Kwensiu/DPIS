package com.dpis.module.viewport

import android.os.Process
import com.dpis.module.config.DpisConfigStore
import com.dpis.module.runtime.systemserver.selection.PackageUidResolver
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
        val userId = currentUserId() ?: return
        Thread(
            { runRootCommand(buildCompatConfigCommand(packageName, targetSpec, mode, userId)) },
            "DPIS-viewport-property-publisher"
        ).apply { isDaemon = true }.start()
    }

    @JvmStatic
    fun clearTargetAsync(packageName: String?) {
        if (packageName.isNullOrBlank()) return
        val userId = currentUserId() ?: return
        Thread(
            { runRootCommand(buildClearCommand(packageName, userId)) },
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
        val userId = currentUserId() ?: return
        runRootCommand(
            buildCompatConfigCommand(
                packageName,
                if (store.isTargetDpisEnabled(packageName)) store.getTargetViewportSpec(packageName) else ViewportTargetSpec.off(),
                store.getTargetViewportApplyMode(packageName),
                userId,
            ),
        )
    }

    @JvmStatic
    fun buildSetCommandForTest(property: String, widthDp: Int) = buildSetCommand(property, widthDp)

    @JvmStatic
    fun buildCompatConfigCommandForTest(packageName: String, widthDp: Int, mode: String) =
        buildCompatConfigCommand(packageName, ViewportTargetSpec.absoluteDp(widthDp), mode, 0)

    @JvmStatic
    fun buildCompatConfigCommandForTest(
        packageName: String,
        targetSpec: ViewportTargetSpec,
        mode: String,
        userId: Int,
    ) = buildCompatConfigCommand(packageName, targetSpec, mode, userId)


    private fun buildCompatConfigCommand(packageName: String, widthDp: Int, mode: String): String {
        val userId = currentUserId() ?: return ""
        return buildCompatConfigCommand(
            packageName,
            ViewportTargetSpec.absoluteDp(widthDp),
            mode,
            userId
        )
    }

    private fun buildCompatConfigCommand(
        packageName: String,
        targetSpec: ViewportTargetSpec,
        mode: String?,
        userId: Int,
    ): String {
        val enc = ViewportPropertyProjection.encode(targetSpec, mode)
        fun pair(property: String, persistent: String, value: String) =
            "${buildSetCommand(property, value)}; ${buildSetCommand(persistent, value)}"
        return listOf(
            pair(
                ViewportPropertyBridge.propertyNameForPackage(packageName, userId),
                ViewportPropertyBridge.persistentPropertyNameForPackage(packageName, userId),
                enc.systemEmulationValue.toString()
            ),
            pair(
                ViewportPropertyBridge.targetTypePropertyNameForPackage(packageName, userId),
                ViewportPropertyBridge.persistentTargetTypePropertyNameForPackage(
                    packageName,
                    userId
                ),
                enc.targetType
            ),
            pair(
                ViewportPropertyBridge.scalePropertyNameForPackage(packageName, userId),
                ViewportPropertyBridge.persistentScalePropertyNameForPackage(packageName, userId),
                enc.scaleMilliPercent.toString()
            ),
            pair(
                ViewportPropertyBridge.compatConfigPropertyNameForPackage(packageName, userId),
                ViewportPropertyBridge.persistentCompatConfigPropertyNameForPackage(
                    packageName,
                    userId
                ),
                enc.compatConfigValue.toString()
            ),
            pair(
                ViewportPropertyBridge.compatModePropertyNameForPackage(packageName, userId),
                ViewportPropertyBridge.persistentCompatModePropertyNameForPackage(
                    packageName,
                    userId
                ),
                enc.compatMode
            ),
        ).joinToString("; ")
    }

    private fun buildConfiguredTargetsCommand(store: DpisConfigStore): String {
        val userId = currentUserId() ?: return ""
        return store.getConfiguredPackages().joinToString("; ") { packageName ->
            val name = packageName ?: ""
            buildCompatConfigCommand(
                name,
                if (store.isTargetDpisEnabled(name)) store.getTargetViewportSpec(name) else ViewportTargetSpec.off(),
                store.getTargetViewportApplyMode(name),
                userId,
            )
        }
    }

    private fun currentUserId(): Int = runCatching {
        PackageUidResolver.userIdFromUid(Process.myUid())
    }.getOrNull() ?: 0

    private fun buildClearCommand(packageName: String, userId: Int) =
        buildCompatConfigCommand(
            packageName,
            ViewportTargetSpec.off(),
            ViewportApplyMode.OFF,
            userId
        )

    private fun buildSetCommand(property: String, widthDp: Int) =
        buildSetCommand(property, widthDp.toString())

    private fun buildSetCommand(property: String, value: String) =
        "setprop ${shellQuote(property)} ${shellQuote(value)}"

    private fun runRootCommand(command: String) = RootCommandRunner.run(command)
    private fun shellQuote(value: String) =
        if (value.isEmpty()) "''" else "'${value.replace("'", "'\\''")}'"
}
