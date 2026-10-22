package com.dpis.module.templates

import com.dpis.module.config.DpisConfigStore
import com.dpis.module.config.PackageConfigRepository
import com.dpis.module.fonts.FontApplyMode
import com.dpis.module.fonts.hookdomain.FontHookDomainDecision
import com.dpis.module.fonts.hookdomain.FontHookDomainPropertySyncer
import com.dpis.module.hooks.HookDomainOverride
import com.dpis.module.hooks.HookDomainOverrideStore
import com.dpis.module.runtime.font.FontRuntimePropertySyncer
import com.dpis.module.viewport.ViewportApplyMode
import com.dpis.module.viewport.ViewportPropertySyncer
import com.dpis.module.viewport.ViewportTargetSpec
import java.util.Collections

object QuickTemplateApplyAdapters {
    fun from(
        store: DpisConfigStore,
        xposedConfigReady: () -> Boolean,
    ): QuickTemplateApplyCoordinator<TemplateConfigValue> = QuickTemplateApplyCoordinator(
        StoreConfigWriter(store),
        StoreRuntimePublisher(store),
        xposedConfigReady,
    )

    private class StoreConfigWriter(store: DpisConfigStore) :
        QuickTemplateApplyCoordinator.ConfigWriter<TemplateConfigValue?> {
        private val packageConfigRepository = PackageConfigRepository(store)

        override fun hasRealPackageConfig(packageName: String?): Boolean =
            packageConfigRepository.hasRealPackageConfig(packageName)

        override fun writePackageTemplateConfigValue(
            packageName: String?,
            value: TemplateConfigValue?,
        ): Boolean = packageConfigRepository.writePackageTemplateConfigValue(packageName, value)
    }

    class RuntimePublishPlan private constructor(
        val publishViewport: Boolean,
        viewportTargetSpec: ViewportTargetSpec?,
        viewportApplyMode: String?,
        val publishFontScale: Boolean,
        val fontScalePercent: Int?,
        fontApplyMode: String?,
        val hyperOsNativeFontHookEnabled: Boolean,
        val typefaceId: String?,
        val publishFontHookDomains: Boolean,
        fontHookDomains: Set<String>?,
    ) {
        val viewportTargetSpec = viewportTargetSpec ?: ViewportTargetSpec.off()
        val viewportApplyMode = ViewportApplyMode.normalize(viewportApplyMode)
        val fontApplyMode = FontApplyMode.normalize(fontApplyMode)
        val fontHookDomains: Set<String> = Collections.unmodifiableSet(
            LinkedHashSet(fontHookDomains.orEmpty()),
        )

        companion object {
            fun from(
                value: TemplateConfigValue?,
                hookDomainOverride: HookDomainOverride?,
                hyperOsNativeFontHookEnabled: Boolean,
            ): RuntimePublishPlan {
                val normalized = value ?: TemplateConfigValue.EMPTY
                val override = hookDomainOverride ?: HookDomainOverride.automatic()
                val target = TemplateConfigValueAdapters.toViewportTargetSpec(normalized)
                val viewportEnabled = target.isEnabled
                val fontScaleEnabled = normalized.fontScalePercent != null
                return RuntimePublishPlan(
                    publishViewport = viewportEnabled,
                    viewportTargetSpec = target,
                    viewportApplyMode = if (viewportEnabled) {
                        normalized.viewportApplyMode
                    } else {
                        ViewportApplyMode.OFF
                    },
                    publishFontScale = fontScaleEnabled,
                    fontScalePercent = normalized.fontScalePercent,
                    fontApplyMode = if (fontScaleEnabled) {
                        normalized.fontApplyMode
                    } else {
                        FontApplyMode.OFF
                    },
                    hyperOsNativeFontHookEnabled = hyperOsNativeFontHookEnabled,
                    typefaceId = normalized.typefaceId,
                    publishFontHookDomains = override.customPathEnabled,
                    fontHookDomains = override.enabledKnownDomains,
                )
            }
        }
    }

    private class StoreRuntimePublisher(
        private val store: DpisConfigStore,
    ) : QuickTemplateApplyCoordinator.RuntimePublisher<TemplateConfigValue?> {
        override fun publish(packageName: String?, value: TemplateConfigValue?) {
            val plan = RuntimePublishPlan.from(
                value,
                HookDomainOverrideStore(store).read(packageName),
                FontHookDomainDecision.isHyperOsNativeFlutterEnabled(store, packageName),
            )
            if (plan.publishViewport) {
                ViewportPropertySyncer.publishTargetAsync(
                    packageName,
                    plan.viewportTargetSpec,
                    plan.viewportApplyMode,
                )
            } else {
                ViewportPropertySyncer.clearTargetAsync(packageName)
            }
            if (plan.publishFontScale) {
                FontRuntimePropertySyncer.publishTargetAsync(
                    packageName,
                    checkNotNull(plan.fontScalePercent),
                    plan.fontApplyMode,
                    plan.hyperOsNativeFontHookEnabled,
                    store.packageConfigOwnerUserId(),
                )
            } else {
                FontRuntimePropertySyncer.clearFontScaleTargetAsync(
                    packageName,
                    store.packageConfigOwnerUserId()
                )
            }
            FontRuntimePropertySyncer.publishTypefaceTargetAsync(
                packageName,
                plan.typefaceId,
                store.packageConfigOwnerUserId()
            )
            if (plan.publishFontHookDomains) {
                FontHookDomainPropertySyncer.publishTargetAsync(
                    packageName,
                    plan.fontHookDomains,
                    store.packageConfigOwnerUserId()
                )
            } else {
                FontHookDomainPropertySyncer.clearTargetAsync(
                    packageName,
                    store.packageConfigOwnerUserId()
                )
            }
        }
    }
}
