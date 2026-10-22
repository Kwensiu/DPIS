package com.dpis.module.fonts.hookdomain

import com.dpis.module.config.DpisConfigStore
import com.dpis.module.hooks.HookDomainOverrideStore
import com.dpis.module.runtime.transport.RootCommandRunner

object FontHookDomainPropertySyncer {
    @JvmStatic
    fun publishTargetAsync(packageName: String?, enabledKnownDomains: Set<String>?, userId: Int) {
        if (packageName.isNullOrBlank()) return
        Thread({
            runRootCommand(
                FontHookDomainPropertyCommands.buildPublish(
                    packageName,
                    enabledKnownDomains.orEmpty(),
                    userId,
                )
            )
        }, "DPIS-font-hook-domain-publisher").apply { isDaemon = true }.start()
    }

    @JvmStatic
    fun publishFromStoreAsync(packageName: String?, store: DpisConfigStore?) {
        if (packageName.isNullOrBlank() || store == null) return
        val override = HookDomainOverrideStore(store).read(packageName)
        if (!override.customPathEnabled) clearTargetAsync(
            packageName,
            store.packageConfigOwnerUserId()
        ) else publishTargetAsync(
            packageName,
            override.enabledKnownDomains,
            store.packageConfigOwnerUserId(),
        )
    }

    @JvmStatic
    fun clearTargetAsync(packageName: String?, userId: Int) {
        if (packageName.isNullOrBlank()) return
        Thread(
            { runRootCommand(FontHookDomainPropertyCommands.buildClear(packageName, userId)) },
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
                    override.enabledKnownDomains,
                    store.packageConfigOwnerUserId(),
                )
                else FontHookDomainPropertyCommands.buildClear(
                    packageName,
                    store.packageConfigOwnerUserId()
                )
            }
            if (command.isNotEmpty()) runRootCommand(command)
        }, "DPIS-font-hook-domain-syncer").apply { isDaemon = true }.start()
    }

    private fun runRootCommand(command: String) {
        RootCommandRunner.run(command)
    }
}
