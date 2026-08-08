package com.dpis.module.quickconfig;

/** Decides whether Quick Config can open a target or must request Usage Access first. */
public final class QuickConfigTargetDecision {
    public enum Kind {
        TARGET,
        REQUEST_USAGE_ACCESS,
        UNAVAILABLE
    }

    public record Result(Kind kind, String packageName) {
    }

    private QuickConfigTargetDecision() {
    }

    public static Result decide(
            String explicitPackageName,
            boolean usageAccessGranted,
            String resolvedPackageName) {
        if (hasPackageName(explicitPackageName)) {
            return new Result(Kind.TARGET, explicitPackageName);
        }
        if (!usageAccessGranted) {
            return new Result(Kind.REQUEST_USAGE_ACCESS, null);
        }
        if (hasPackageName(resolvedPackageName)) {
            return new Result(Kind.TARGET, resolvedPackageName);
        }
        return new Result(Kind.UNAVAILABLE, null);
    }

    private static boolean hasPackageName(String packageName) {
        return packageName != null && !packageName.isBlank();
    }
}
