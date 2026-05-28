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

    private final ConfigWriter configWriter;
    private final RuntimePublisher runtimePublisher;

    QuickTemplateApplyCoordinator(DpiConfigStore store) {
        this(new StoreConfigWriter(store), new FontRuntimePublisher(store));
    }

    QuickTemplateApplyCoordinator(ConfigWriter configWriter, RuntimePublisher runtimePublisher) {
        this.configWriter = configWriter;
        this.runtimePublisher = runtimePublisher;
    }

    Plan plan(QuickTemplateStore.QuickTemplate template) {
        LinkedHashSet<String> targets = sanitizePackages(
                template != null ? template.selectedPackages : null);
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
        if (configWriter == null || template == null) {
            return Result.failure(Collections.emptyList(), Collections.emptyList());
        }
        LinkedHashSet<String> targets = sanitizePackages(template.selectedPackages);
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
        private final DpiConfigStore store;

        StoreConfigWriter(DpiConfigStore store) {
            this.store = store;
        }

        @Override
        public boolean hasRealPackageConfig(String packageName) {
            return store != null && store.hasRealPackageConfig(packageName);
        }

        @Override
        public boolean writePackageTemplateConfigValue(String packageName, TemplateConfigValue value) {
            return store != null && store.writePackageTemplateConfigValue(packageName, value);
        }
    }

    private static final class FontRuntimePublisher implements RuntimePublisher {
        private final DpiConfigStore store;

        FontRuntimePublisher(DpiConfigStore store) {
            this.store = store;
        }

        @Override
        public void publish(String packageName, TemplateConfigValue value) {
            TemplateConfigValue normalized = value != null ? value : TemplateConfigValue.EMPTY;
            if (normalized.fontScalePercent != null) {
                FontRuntimePropertySyncer.publishTargetAsync(
                        packageName,
                        normalized.fontScalePercent,
                        normalized.fontApplyMode,
                        FontHookDomainDecision.isHyperOsNativeFlutterEnabled(store, packageName));
            } else {
                FontRuntimePropertySyncer.clearFontScaleTargetAsync(packageName);
            }
            FontRuntimePropertySyncer.publishTypefaceTargetAsync(
                    packageName, normalized.typefaceId);
        }
    }
}
