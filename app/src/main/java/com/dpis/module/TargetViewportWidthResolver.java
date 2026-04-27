package com.dpis.module;

final class TargetViewportWidthResolver {
    private TargetViewportWidthResolver() {
    }

    static Integer resolve(DpiConfigStore store, String packageName) {
        if (store == null || packageName == null || packageName.isEmpty()) {
            return null;
        }
        Integer runtimeOverride = ViewportPropertyBridge.readTargetWidthDp(packageName);
        if (runtimeOverride != null) {
            return runtimeOverride > 0 ? runtimeOverride : null;
        }
        String requestedMode = store.getTargetViewportApplyMode(packageName);
        String mode = EffectiveModeResolver.resolveViewportMode(
                requestedMode,
                store.isSystemServerHooksEnabled());
        if (ViewportApplyMode.SYSTEM_EMULATION.equals(ViewportApplyMode.normalize(requestedMode))
                && ViewportApplyMode.OFF.equals(mode)) {
            return null;
        }
        Integer targetViewportWidthDp = store.getTargetViewportWidthDp(packageName);
        if (targetViewportWidthDp == null || targetViewportWidthDp <= 0) {
            return null;
        }
        return targetViewportWidthDp;
    }
}
