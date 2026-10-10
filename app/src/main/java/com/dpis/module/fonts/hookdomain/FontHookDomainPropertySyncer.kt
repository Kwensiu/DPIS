package com.dpis.module.fonts.hookdomain

import com.dpis.module.config.DpisConfigStore
import com.dpis.module.hooks.HookDomainOverrideStore
import com.dpis.module.runtime.transport.RootCommandRunner

object FontHookDomainPropertySyncer {
    @JvmStatic
    fun publishTargetAsync(packageName: String?, enabledKnownDomains: Set<String>?) {
        if (packageName.isNullOrBlank()) return
        Thread({
            runRootCommand(
                FontHookDomainPropertyCommands.buildPublish(
                    packageName,
                    enabledKnownDomains.orEmpty()
                )
            )
        }, "DPIS-font-hook-domain-publisher").apply { isDaemon = true }.start()
    }

    @JvmStatic
    fun publishFromStoreAsync(packageName: String?, store: DpisConfigStore?) {
        if (packageName.isNullOrBlank() || store == null) return
        val override = HookDomainOverrideStore(store).read(packageName)
        if (!override.customPathEnabled) clearTargetAsync(packageName) else publishTargetAsync(
            packageName,
            override.enabledKnownDomains
        )
    }

    @JvmStatic
    fun clearTargetAsync(packageName: String?) {
        if (packageName.isNullOrBlank()) return
        Thread(
            { runRootCommand(FontHookDomainPropertyCommands.buildClear(packageName)) },
            "DPIS-font-hook-domain-cleaner"
        ).apply { isDaemon = true }.start()
    }

    @JvmStatic
    fun syncConfiguredTargetsAsync(store: DpisConfigStore?) {
        if (store == null) return
        val packages = LinkedHashSet(store.getConfiguredPackages())
        if (packages.isEmpty()) return
        Thread({
            val overrides = HookDomainOverrideStore(store)
            val command = packages.joinToString("; ") { packageName ->
                if (packageName == null) return@joinToString ""
                val override = overrides.read(packageName)
                if (override.customPathEnabled) FontHookDomainPropertyCommands.buildPublish(
                    packageName,
                    override.enabledKnownDomains
                )
                else FontHookDomainPropertyCommands.buildClear(packageName)
            }
            if (command.isNotEmpty()) runRootCommand(command)
        }, "DPIS-font-hook-domain-syncer").apply { isDaemon = true }.start()
    }

    private fun runRootCommand(command: String) {
        RootCommandRunner.run(command)
    }
}
