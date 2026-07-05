package com.dpis.module.runtime.systemserver;

import com.dpis.module.*;
import com.dpis.module.runtime.appprocess.WebApkCarrierResolver;

import java.util.List;
import java.util.Set;

public final class SystemServerHotPathInspector {
    private SystemServerHotPathInspector() {
    }

    static boolean shouldInspectHotEntry(String entryName,
                                         Object self,
                                         List<Object> args,
                                         Set<String> configuredPackages) {
        if (!SystemServerEntryRoute.isHotEntry(entryName)) {
            return true;
        }
        if (configuredPackages == null || configuredPackages.isEmpty()) {
            return false;
        }
        if (containsConfiguredPackageHint(String.valueOf(self), configuredPackages)) {
            return true;
        }
        if (args == null || args.isEmpty()) {
            return false;
        }
        int maxArgs = Math.min(args.size(), maxArgsToInspect(entryName));
        for (int i = 0; i < maxArgs; i++) {
            if (containsConfiguredPackageHint(String.valueOf(args.get(i)), configuredPackages)) {
                return true;
            }
        }
        return false;
    }

    private static int maxArgsToInspect(String entryName) {
        if (SystemServerEntryRoute.isRelayoutDispatch(entryName)) {
            return 8;
        }
        return 3;
    }

    private static boolean containsConfiguredPackageHint(String text, Set<String> configuredPackages) {
        if (text == null || text.isEmpty() || configuredPackages == null || configuredPackages.isEmpty()) {
            return false;
        }
        for (String webApkOwner : WebApkCarrierResolver.collectOwnerPackagesFromText(
                text, configuredPackages.size())) {
            if (configuredPackages.contains(webApkOwner)) {
                return true;
            }
        }
        for (String configuredPackage : configuredPackages) {
            if (configuredPackage != null
                    && !configuredPackage.isEmpty()
                    && text.contains(configuredPackage)) {
                return true;
            }
        }
        return false;
    }
}
