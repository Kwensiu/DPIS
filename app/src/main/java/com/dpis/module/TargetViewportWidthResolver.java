package com.dpis.module;

final class TargetViewportWidthResolver {
    private TargetViewportWidthResolver() {
    }

    static Integer resolve(DpiConfigStore store, String packageName) {
        if (store == null || packageName == null || packageName.isEmpty()) {
            return null;
        }
        Integer targetViewportWidthDp = store.getTargetViewportWidthDp(packageName);
        if (targetViewportWidthDp == null || targetViewportWidthDp <= 0) {
            return null;
        }
        return targetViewportWidthDp;
    }
}
