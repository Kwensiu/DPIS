package com.dpis.module;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

final class QuickTemplateApplyCoordinator {
    interface ConfigWriter {
        boolean hasRealPackageConfig(String packageName);

        boolean writePackageTemplateConfigValue(String packageName, TemplateConfigValue value);
    }

    interface RuntimePublisher {
        void publish(String packageName, TemplateConfigValue value);
    }

    interface TargetPackageFilter {
        boolean isAllowed(String packageName);
    }

    private final ConfigWriter configWriter;
    private final RuntimePublisher runtimePublisher;

    QuickTemplateApplyCoordinator(DpisConfigStore store) {
        this(new StoreConfigWriter(store), new StoreRuntimePublisher(store));
    }

    QuickTemplateApplyCoordinator(ConfigWriter configWriter, RuntimePublisher runtimePublisher) {
        this.configWriter = configWriter;
        this.runtimePublisher = runtimePublisher;
    }

    Plan plan(QuickTemplateStore.QuickTemplate template) {
        return plan(template, null);
    }

    Plan plan(QuickTemplateStore.QuickTemplate template, TargetPackageFilter targetPackageFilter) {
        LinkedHashSet<String> targets = sanitizePackages(
                template != null ? template.selectedPackages : null);
        targets = filterPackages(targets, targetPackageFilter);
        int overwriteCount = 0;
        if (configWriter != null) {
            for (String packageName : targets) {
                if (configWriter.hasRealPackageConfig(packageName)) {
                    overwriteCount++;
                }
            }
        }
        return new Plan(targets.size(), overwriteCount);
    }

    Result apply(QuickTemplateStore.QuickTemplate template) {
        return apply(template, null);
    }

    Result apply(QuickTemplateStore.QuickTemplate template, TargetPackageFilter targetPackageFilter) {
        if (configWriter == null || template == null) {
            return Result.failure(Collections.emptyList(), Collections.emptyList());
        }
        LinkedHashSet<String> targets = sanitizePackages(template.selectedPackages);
        targets = filterPackages(targets, targetPackageFilter);
        if (targets.isEmpty()) {
            return Result.emptySelection();
        }
        ArrayList<String> successfulPackages = new ArrayList<>();
        ArrayList<String> failedPackages = new ArrayList<>();
        for (String packageName : targets) {
            boolean saved = configWriter.writePackageTemplateConfigValue(
                    packageName, template.configValue);
            if (saved) {
                successfulPackages.add(packageName);
                if (runtimePublisher != null) {
                    runtimePublisher.publish(packageName, template.configValue);
                }
            } else {
                failedPackages.add(packageName);
            }
        }
        return new Result(false, successfulPackages, failedPackages);
    }

    private static LinkedHashSet<String> sanitizePackages(Set<String> packageNames) {
        LinkedHashSet<String> sanitized = new LinkedHashSet<>();
        if (packageNames == null) {
            return sanitized;
        }
        for (String packageName : packageNames) {
            if (packageName == null) {
                continue;
            }
            String trimmed = packageName.trim();
            if (!trimmed.isEmpty()) {
                sanitized.add(trimmed);
            }
        }
        return sanitized;
    }

    private static LinkedHashSet<String> filterPackages(
            LinkedHashSet<String> packageNames,
            TargetPackageFilter targetPackageFilter) {
        if (targetPackageFilter == null || packageNames == null || packageNames.isEmpty()) {
            return packageNames != null ? packageNames : new LinkedHashSet<>();
        }
        LinkedHashSet<String> filtered = new LinkedHashSet<>();
        for (String packageName : packageNames) {
            if (targetPackageFilter.isAllowed(packageName)) {
                filtered.add(packageName);
            }
        }
        return filtered;
    }

    static final class Plan {
        final int targetCount;
        final int overwriteCount;

        Plan(int targetCount, int overwriteCount) {
            this.targetCount = targetCount;
            this.overwriteCount = overwriteCount;
        }
    }

    static final class Result {
        final boolean emptySelection;
        final List<String> successfulPackages;
        final List<String> failedPackages;

        private Result(boolean emptySelection,
                List<String> successfulPackages,
                List<String> failedPackages) {
            this.emptySelection = emptySelection;
            this.successfulPackages = Collections.unmodifiableList(new ArrayList<>(
                    successfulPackages != null ? successfulPackages : Collections.emptyList()));
            this.failedPackages = Collections.unmodifiableList(new ArrayList<>(
                    failedPackages != null ? failedPackages : Collections.emptyList()));
        }

        static Result emptySelection() {
            return new Result(true, Collections.emptyList(), Collections.emptyList());
        }

        static Result failure(List<String> successfulPackages, List<String> failedPackages) {
            return new Result(false, successfulPackages, failedPackages);
        }

        int successCount() {
            return successfulPackages.size();
        }

        int failureCount() {
            return failedPackages.size();
        }
    }

    private static final class StoreConfigWriter implements ConfigWriter {
        private final PackageConfigRepository packageConfigRepository;

        StoreConfigWriter(DpisConfigStore store) {
            this.packageConfigRepository = new PackageConfigRepository(store);
        }

        @Override
        public boolean hasRealPackageConfig(String packageName) {
            return packageConfigRepository.hasRealPackageConfig(packageName);
        }

        @Override
        public boolean writePackageTemplateConfigValue(String packageName, TemplateConfigValue value) {
            return packageConfigRepository.writePackageTemplateConfigValue(packageName, value);
        }
    }

    static final class RuntimePublishPlan {
        final boolean publishViewport;
        final ViewportTargetSpec viewportTargetSpec;
        final String viewportApplyMode;
        final boolean publishFontScale;
        final Integer fontScalePercent;
        final String fontApplyMode;
        final boolean hyperOsNativeFontHookEnabled;
        final String typefaceId;
        final boolean publishFontHookDomains;
        final Set<String> fontHookDomains;

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

        static RuntimePublishPlan from(
                TemplateConfigValue value,
                HookDomainOverride hookDomainOverride,
                boolean hyperOsNativeFontHookEnabled) {
            TemplateConfigValue normalized = value != null ? value : TemplateConfigValue.EMPTY;
            HookDomainOverride override = hookDomainOverride != null
                    ? hookDomainOverride
                    : HookDomainOverride.automatic();
            boolean viewportEnabled = normalized.viewportTargetSpec.isEnabled();
            boolean fontScaleEnabled = normalized.fontScalePercent != null;
            return new RuntimePublishPlan(
                    viewportEnabled,
                    normalized.viewportTargetSpec,
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

    private static final class StoreRuntimePublisher implements RuntimePublisher {
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
            FontRuntimePropertySyncer.publishTypefaceTargetAsync(
                    packageName, plan.typefaceId);
            if (plan.publishFontHookDomains) {
                FontHookDomainPropertySyncer.publishTargetAsync(packageName, plan.fontHookDomains);
            } else {
                FontHookDomainPropertySyncer.clearTargetAsync(packageName);
            }
        }
    }
}
