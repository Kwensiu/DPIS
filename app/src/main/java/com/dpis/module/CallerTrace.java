package com.dpis.module;

import java.util.List;

final class CallerTrace {
    private CallerTrace() {
    }

    static String findRelevantCaller(List<StackTraceElement> frames, String targetPackage) {
        if (frames == null || frames.isEmpty()) {
            return null;
        }
        for (StackTraceElement frame : frames) {
            String className = frame.getClassName();
            if (className == null || isIgnored(className)) {
                continue;
            }
            if (targetPackage != null && !targetPackage.isEmpty()
                    && className.startsWith(targetPackage)) {
                return format(frame);
            }
        }
        for (StackTraceElement frame : frames) {
            String className = frame.getClassName();
            if (className == null || isIgnored(className)) {
                continue;
            }
            return format(frame);
        }
        return null;
    }

    static String capture(String targetPackage) {
        return findRelevantCaller(List.of(new Throwable().getStackTrace()), targetPackage);
    }

    private static boolean isIgnored(String className) {
        return className.startsWith("com.dpis.module.")
                || className.startsWith("android.")
                || className.startsWith("com.android.")
                || className.startsWith("java.")
                || className.startsWith("javax.")
                || className.startsWith("kotlin.")
                || className.startsWith("dalvik.")
                || className.startsWith("sun.")
                || className.startsWith("libcore.")
                || className.startsWith("io.github.libxposed.");
    }

    private static String format(StackTraceElement frame) {
        return frame.getClassName() + "#" + frame.getMethodName() + ":" + frame.getLineNumber();
    }
}
