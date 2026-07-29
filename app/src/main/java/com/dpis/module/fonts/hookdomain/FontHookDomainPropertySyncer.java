package com.dpis.module.fonts.hookdomain;

import com.dpis.module.DpisConfigStore;


import com.dpis.module.hooks.HookDomainOverride;
import com.dpis.module.hooks.HookDomainOverrideStore;

import com.dpis.module.runtime.RootCommandRunner;

import java.util.LinkedHashSet;
import java.util.Set;

public final class FontHookDomainPropertySyncer {
    private FontHookDomainPropertySyncer() {
    }

    public static void publishTargetAsync(String packageName, Set<String> enabledKnownDomains) {
        if (packageName == null || packageName.isBlank()) {
            return;
        }
        Thread publisherThread = new Thread(
                () -> runRootCommand(buildPublishCommand(packageName, enabledKnownDomains)),
                "DPIS-font-hook-domain-publisher");
        publisherThread.setDaemon(true);
        publisherThread.start();
    }

    public static void publishFromStoreAsync(String packageName, DpisConfigStore store) {
        if (packageName == null || packageName.isBlank() || store == null) {
            return;
        }
        HookDomainOverride override = new HookDomainOverrideStore(store).read(packageName);
        if (!override.customPathEnabled) {
            clearTargetAsync(packageName);
            return;
        }
        publishTargetAsync(packageName, override.enabledKnownDomains);
    }

    public static void clearTargetAsync(String packageName) {
        if (packageName == null || packageName.isBlank()) {
            return;
        }
        Thread cleanerThread = new Thread(
                () -> runRootCommand(buildClearCommand(packageName)),
                "DPIS-font-hook-domain-cleaner");
        cleanerThread.setDaemon(true);
        cleanerThread.start();
    }

    public static void syncConfiguredTargetsAsync(DpisConfigStore store) {
        if (store == null) {
            return;
        }
        LinkedHashSet<String> packages = new LinkedHashSet<>(store.getConfiguredPackages());
        if (packages.isEmpty()) {
            return;
        }
        Thread syncThread = new Thread(() -> {
            HookDomainOverrideStore overrideStore = new HookDomainOverrideStore(store);
            StringBuilder command = new StringBuilder();
            for (String packageName : packages) {
                HookDomainOverride override = overrideStore.read(packageName);
                appendCommand(command, override.customPathEnabled
                        ? buildPublishCommand(packageName, override.enabledKnownDomains)
                        : buildClearCommand(packageName));
            }
            if (command.length() > 0) {
                runRootCommand(command.toString());
            }
        }, "DPIS-font-hook-domain-syncer");
        syncThread.setDaemon(true);
        syncThread.start();
    }

    private static String buildPublishCommand(String packageName, Set<String> enabledKnownDomains) {
        return FontHookDomainPropertyCommands.buildPublish(packageName, enabledKnownDomains);
    }

    private static String buildClearCommand(String packageName) {
        return FontHookDomainPropertyCommands.buildClear(packageName);
    }

    private static void appendCommand(StringBuilder command, String fragment) {
        if (fragment == null || fragment.isEmpty()) {
            return;
        }
        if (command.length() > 0) {
            command.append("; ");
        }
        command.append(fragment);
    }

    private static void runRootCommand(String command) {
        RootCommandRunner.run(command);
    }
}
