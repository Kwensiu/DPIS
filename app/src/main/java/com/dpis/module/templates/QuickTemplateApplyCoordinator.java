package com.dpis.module.templates;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Coordinates applying a saved quick-template value to selected packages.
 *
 * <p>The coordinator is intentionally generic so template apply semantics can
 * live in the template package without knowing root-owned package config,
 * runtime publishing, or app-list models.</p>
 */
@SuppressWarnings("java:S1845")
public final class QuickTemplateApplyCoordinator<T> {
    public interface Template<T> {
        Set<String> selectedPackages();

        T configValue();
    }

    public interface ConfigWriter<T> {
        boolean hasRealPackageConfig(String packageName);

        boolean writePackageTemplateConfigValue(String packageName, T value);
    }

    public interface RuntimePublisher<T> {
        void publish(String packageName, T value);
    }

    public interface TargetPackageFilter {
        boolean isAllowed(String packageName);
    }

    private final ConfigWriter<T> configWriter;
    private final RuntimePublisher<T> runtimePublisher;

    public QuickTemplateApplyCoordinator(
            ConfigWriter<T> configWriter,
            RuntimePublisher<T> runtimePublisher) {
        this.configWriter = configWriter;
        this.runtimePublisher = runtimePublisher;
    }

    public Plan plan(Template<T> template) {
        return plan(template, null);
    }

    public Plan plan(Template<T> template, TargetPackageFilter targetPackageFilter) {
        LinkedHashSet<String> targets = sanitizePackages(
                template != null ? template.selectedPackages() : null);
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

    public Result apply(Template<T> template) {
        return apply(template, null);
    }

    public Result apply(Template<T> template, TargetPackageFilter targetPackageFilter) {
        if (configWriter == null || template == null) {
            return Result.failure(Collections.emptyList(), Collections.emptyList());
        }
        LinkedHashSet<String> targets = sanitizePackages(template.selectedPackages());
        targets = filterPackages(targets, targetPackageFilter);
        if (targets.isEmpty()) {
            return Result.noSelection();
        }
        ArrayList<String> successfulPackages = new ArrayList<>();
        ArrayList<String> failedPackages = new ArrayList<>();
        T configValue = template.configValue();
        for (String packageName : targets) {
            boolean saved = configWriter.writePackageTemplateConfigValue(packageName, configValue);
            if (saved) {
                successfulPackages.add(packageName);
                if (runtimePublisher != null) {
                    runtimePublisher.publish(packageName, configValue);
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

    public static final class Plan {
        public final int targetCount;
        public final int overwriteCount;

        private Plan(int targetCount, int overwriteCount) {
            this.targetCount = targetCount;
            this.overwriteCount = overwriteCount;
        }
    }

    public static final class Result {
        public final boolean emptySelection;
        public final List<String> successfulPackages;
        public final List<String> failedPackages;

        private Result(boolean emptySelection,
                List<String> successfulPackages,
                List<String> failedPackages) {
            this.emptySelection = emptySelection;
            this.successfulPackages = Collections.unmodifiableList(new ArrayList<>(
                    successfulPackages != null ? successfulPackages : Collections.emptyList()));
            this.failedPackages = Collections.unmodifiableList(new ArrayList<>(
                    failedPackages != null ? failedPackages : Collections.emptyList()));
        }

        public static Result noSelection() {
            return new Result(true, Collections.emptyList(), Collections.emptyList());
        }

        public static Result failure(List<String> successfulPackages, List<String> failedPackages) {
            return new Result(false, successfulPackages, failedPackages);
        }

        public int successCount() {
            return successfulPackages.size();
        }

        public int failureCount() {
            return failedPackages.size();
        }
    }
}
