package com.dpis.module.templates;

import com.dpis.module.*;



import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public final class QuickTemplateApplyAdapters {
    private QuickTemplateApplyAdapters() {
    }

    public static QuickTemplateApplyCoordinator<TemplateConfigValue> from(DpisConfigStore store) {
        return new QuickTemplateApplyCoordinator<>(
                new StoreConfigWriter(store),
                new StoreRuntimePublisher(store));
    }

    private static final class StoreConfigWriter
            implements QuickTemplateApplyCoordinator.ConfigWriter<TemplateConfigValue> {
        private final PackageConfigRepository packageConfigRepository;

        StoreConfigWriter(DpisConfigStore store) {
            this.packageConfigRepository = new PackageConfigRepository(store);
        }

        @Override
        public boolean hasRealPackageConfig(String packageName) {
            return packageConfigRepository.hasRealPackageConfig(packageName);
        }

        @Override
        public boolean writePackageTemplateConfigValue(
                String packageName,
                TemplateConfigValue value) {
            return packageConfigRepository.writePackageTemplateConfigValue(packageName, value);
        }
    }

    public static final class RuntimePublishPlan {
        public final boolean publishViewport;
        public final ViewportTargetSpec viewportTargetSpec;
        public final String viewportApplyMode;
        public final boolean publishFontScale;
        public final Integer fontScalePercent;
        public final String fontApplyMode;
        public final boolean hyperOsNativeFontHookEnabled;
        public final String typefaceId;
        public final boolean publishFontHookDomains;
        public final Set<String> fontHookDomains;

        private RuntimePublishPlan(
                boolean publishViewport,
                ViewportTargetSpec viewportTargetSpec,
                String viewportApplyMode,
                boolean publishFontScale,
                Integer fontScalePercent,
                String fontApplyMode,
                boolean hyperOsNativeFontHookEnabled,
                String typefaceId,
                boolean publishFontHookDomains,
                Set<String> fontHookDomains) {
            this.publishViewport = publishViewport;
            this.viewportTargetSpec = viewportTargetSpec != null
                    ? viewportTargetSpec
                    : ViewportTargetSpec.off();
            this.viewportApplyMode = ViewportApplyMode.normalize(viewportApplyMode);
            this.publishFontScale = publishFontScale;
            this.fontScalePercent = fontScalePercent;
            this.fontApplyMode = FontApplyMode.normalize(fontApplyMode);
            this.hyperOsNativeFontHookEnabled = hyperOsNativeFontHookEnabled;
            this.typefaceId = typefaceId;
            this.publishFontHookDomains = publishFontHookDomains;
            this.fontHookDomains = Collections.unmodifiableSet(new LinkedHashSet<>(
                    fontHookDomains != null ? fontHookDomains : Collections.emptySet()));
        }

        public static RuntimePublishPlan from(
                TemplateConfigValue value,
                HookDomainOverride hookDomainOverride,
                boolean hyperOsNativeFontHookEnabled) {
            TemplateConfigValue normalized = value != null ? value : TemplateConfigValue.EMPTY;
            HookDomainOverride override = hookDomainOverride != null
                    ? hookDomainOverride
                    : HookDomainOverride.automatic();
            ViewportTargetSpec viewportTargetSpec =
                    TemplateConfigValueAdapters.toViewportTargetSpec(normalized);
            boolean viewportEnabled = viewportTargetSpec.isEnabled();
            boolean fontScaleEnabled = normalized.fontScalePercent != null;
            return new RuntimePublishPlan(
                    viewportEnabled,
                    viewportTargetSpec,
                    viewportEnabled ? normalized.viewportApplyMode : ViewportApplyMode.OFF,
                    fontScaleEnabled,
                    normalized.fontScalePercent,
                    fontScaleEnabled ? normalized.fontApplyMode : FontApplyMode.OFF,
                    hyperOsNativeFontHookEnabled,
                    normalized.typefaceId,
                    override.customPathEnabled,
                    override.enabledKnownDomains);
        }
    }

    private static final class StoreRuntimePublisher
            implements QuickTemplateApplyCoordinator.RuntimePublisher<TemplateConfigValue> {
        private final DpisConfigStore store;

        StoreRuntimePublisher(DpisConfigStore store) {
            this.store = store;
        }

        @Override
        public void publish(String packageName, TemplateConfigValue value) {
            RuntimePublishPlan plan = RuntimePublishPlan.from(
                    value,
                    new HookDomainOverrideStore(store).read(packageName),
                    FontHookDomainDecision.isHyperOsNativeFlutterEnabled(store, packageName));
            if (plan.publishViewport) {
                ViewportPropertySyncer.publishTargetAsync(
                        packageName,
                        plan.viewportTargetSpec,
                        plan.viewportApplyMode);
            } else {
                ViewportPropertySyncer.clearTargetAsync(packageName);
            }
            if (plan.publishFontScale) {
                FontRuntimePropertySyncer.publishTargetAsync(
                        packageName,
                        plan.fontScalePercent,
                        plan.fontApplyMode,
                        plan.hyperOsNativeFontHookEnabled);
            } else {
                FontRuntimePropertySyncer.clearFontScaleTargetAsync(packageName);
            }
            FontRuntimePropertySyncer.publishTypefaceTargetAsync(packageName, plan.typefaceId);
            if (plan.publishFontHookDomains) {
                FontHookDomainPropertySyncer.publishTargetAsync(packageName, plan.fontHookDomains);
            } else {
                FontHookDomainPropertySyncer.clearTargetAsync(packageName);
            }
        }
    }
}
