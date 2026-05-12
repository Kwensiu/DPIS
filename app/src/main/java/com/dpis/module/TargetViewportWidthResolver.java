package com.dpis.module;

final class TargetViewportWidthResolver {
    private TargetViewportWidthResolver() {
    }

    static Integer resolve(DpiConfigStore store, String packageName) {
        if (store == null || packageName == null || packageName.isEmpty()) {
            return null;
        }
        Integer runtimeOverride = ViewportPropertyBridge.readTargetWidthDp(packageName);
        return resolve(store, packageName, runtimeOverride);
    }

    static Integer resolveForTest(DpiConfigStore store, String packageName, Integer runtimeOverride) {
        return resolve(store, packageName, runtimeOverride);
    }

    static Integer resolve(Integer targetViewportWidthDp,
                           String requestedMode,
                           boolean systemServerHooksEnabled,
                           Integer runtimeOverride) {
        if (runtimeOverride != null) {
            if (runtimeOverride > 0) {
                return runtimeOverride;
            }
            if (!ViewportApplyMode.FIELD_REWRITE.equals(
                    ViewportApplyMode.normalize(requestedMode))) {
                return null;
            }
        }
        String mode = EffectiveModeResolver.resolveViewportMode(
                requestedMode,
                systemServerHooksEnabled);
        if (ViewportApplyMode.SYSTEM_EMULATION.equals(ViewportApplyMode.normalize(requestedMode))
                && ViewportApplyMode.OFF.equals(mode)) {
            return null;
        }
        if (targetViewportWidthDp == null || targetViewportWidthDp <= 0) {
            return null;
        }
        return targetViewportWidthDp;
    }

    private static Integer resolve(DpiConfigStore store, String packageName, Integer runtimeOverride) {
        if (runtimeOverride != null) {
            if (runtimeOverride > 0) {
                return runtimeOverride;
            }
            if (!ViewportApplyMode.FIELD_REWRITE.equals(
                    ViewportApplyMode.normalize(store.getTargetViewportApplyMode(packageName)))) {
                return null;
            }
        }
        return resolve(
                store.getTargetViewportWidthDp(packageName),
                store.getTargetViewportApplyMode(packageName),
                store.isSystemServerHooksEnabled(),
                runtimeOverride);
    }
}
