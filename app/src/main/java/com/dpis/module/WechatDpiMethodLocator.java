package com.dpis.module;

import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.util.DisplayMetrics;

import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.enums.MatchType;
import org.luckypray.dexkit.query.matchers.ClassMatcher;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.MethodData;
import org.luckypray.dexkit.result.MethodDataList;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class WechatDpiMethodLocator {
    private WechatDpiMethodLocator() {
    }

    static Result locate(ClassLoader classLoader, ApplicationInfo applicationInfo,
            long versionCode) {
        Result dexKitResult = locateByDexKit(classLoader, applicationInfo);
        if (!dexKitResult.methods.isEmpty()) {
            return dexKitResult;
        }
        Result routeResult = locateByStaticRoute(classLoader, versionCode);
        if (!routeResult.methods.isEmpty()) {
            return routeResult;
        }
        return dexKitResult.failure != null ? dexKitResult : routeResult;
    }

    private static Result locateByDexKit(ClassLoader classLoader,
            ApplicationInfo applicationInfo) {
        if (classLoader == null || applicationInfo == null
                || applicationInfo.sourceDir == null
                || applicationInfo.sourceDir.isBlank()) {
            return Result.failed(Source.DEXKIT, "missing application sourceDir");
        }
        try {
            loadDexKitLibrary();
        } catch (Throwable throwable) {
            return Result.failed(Source.DEXKIT,
                    throwable.getClass().getName() + ": " + throwable.getMessage());
        }
        try (DexKitBridge bridge = DexKitBridge.create(applicationInfo.sourceDir)) {
            FindMethod query = FindMethod.create()
                    .matcher(MethodMatcher.create()
                            .declaredClass(ClassMatcher.create()
                                    .usingEqStrings(
                                            "MicroMsg.MMDensityManager",
                                            "screenResolution_target_field"))
                            .modifiers(Modifier.PUBLIC, MatchType.Contains)
                            .returnType(DisplayMetrics.class)
                            .paramCount(0)
                            .addInvoke(MethodMatcher.create()
                                    .returnType("boolean")));
            MethodDataList methodDataList = bridge.findMethod(query);
            ArrayList<Method> methods = new ArrayList<>();
            if (methodDataList != null) {
                for (MethodData methodData : methodDataList) {
                    Method method = methodData.getMethodInstance(classLoader);
                    if (isDisplayMetricsGetter(method)) {
                        method.setAccessible(true);
                        methods.add(method);
                    }
                }
            }
            return Result.resolved(Source.DEXKIT, methods);
        } catch (Throwable throwable) {
            return Result.failed(Source.DEXKIT,
                    throwable.getClass().getName() + ": " + throwable.getMessage());
        }
    }

    private static Result locateByStaticRoute(ClassLoader classLoader, long versionCode) {
        WechatDpiRoutes.Route route = WechatDpiRoutes.forVersionCode(versionCode);
        if (route == null) {
            return Result.failed(Source.STATIC_ROUTE,
                    "unsupported versionCode=" + versionCode);
        }
        try {
            Class<?> densityManagerClass = Class.forName(route.className, false, classLoader);
            ArrayList<Method> methods = new ArrayList<>();
            for (Method method : densityManagerClass.getDeclaredMethods()) {
                if (isDisplayMetricsGetter(method)) {
                    method.setAccessible(true);
                    methods.add(method);
                }
            }
            return Result.resolved(Source.STATIC_ROUTE, methods);
        } catch (Throwable throwable) {
            return Result.failed(Source.STATIC_ROUTE,
                    route.routeKey() + ": " + throwable.getClass().getName()
                            + ": " + throwable.getMessage());
        }
    }

    private static boolean isDisplayMetricsGetter(Method method) {
        return method != null
                && method.getParameterTypes().length == 0
                && method.getReturnType() == DisplayMetrics.class;
    }

    private static void loadDexKitLibrary() {
        try {
            System.loadLibrary("dexkit");
            return;
        } catch (UnsatisfiedLinkError firstError) {
            String path = resolveExtractedNativeLibraryPath();
            if (path == null) {
                throw firstError;
            }
            try {
                System.load(path);
            } catch (UnsatisfiedLinkError secondError) {
                secondError.addSuppressed(firstError);
                throw secondError;
            }
        }
    }

    private static String resolveExtractedNativeLibraryPath() {
        String moduleApkPath = resolveModuleApkPath();
        if (moduleApkPath == null || moduleApkPath.isBlank()) {
            return null;
        }
        File installDir = new File(moduleApkPath).getParentFile();
        if (installDir == null) {
            return null;
        }
        for (String abi : Build.SUPPORTED_ABIS) {
            String[] nativeDirs = nativeDirectoryNamesForAbi(abi);
            for (String nativeDir : nativeDirs) {
                File candidate = new File(new File(new File(installDir, "lib"), nativeDir),
                        "libdexkit.so");
                if (candidate.isFile()) {
                    return candidate.getAbsolutePath();
                }
            }
        }
        return null;
    }

    private static String resolveModuleApkPath() {
        ClassLoader classLoader = WechatDpiMethodLocator.class.getClassLoader();
        if (classLoader == null) {
            return null;
        }
        return parseModuleApkPathForTest(classLoader.toString());
    }

    static String parseModuleApkPathForTest(String classLoaderText) {
        if (classLoaderText == null || classLoaderText.isEmpty()) {
            return null;
        }
        String marker = "module=";
        int start = classLoaderText.indexOf(marker);
        if (start < 0) {
            return null;
        }
        start += marker.length();
        int end = classLoaderText.indexOf(',', start);
        if (end < 0) {
            end = classLoaderText.indexOf(']', start);
        }
        if (end < 0) {
            end = classLoaderText.length();
        }
        String path = classLoaderText.substring(start, end).trim();
        return path.endsWith(".apk") ? path : null;
    }

    static String[] nativeDirectoryNamesForAbi(String abi) {
        if ("arm64-v8a".equals(abi)) {
            return new String[] {"arm64", "arm64-v8a"};
        }
        if ("armeabi-v7a".equals(abi)) {
            return new String[] {"arm", "armeabi-v7a"};
        }
        return new String[] {abi};
    }

    enum Source {
        DEXKIT("dexkit"),
        STATIC_ROUTE("static-route");

        final String logName;

        Source(String logName) {
            this.logName = logName;
        }
    }

    static final class Result {
        final Source source;
        final List<Method> methods;
        final String failure;

        private Result(Source source, List<Method> methods, String failure) {
            this.source = source;
            this.methods = Collections.unmodifiableList(methods);
            this.failure = failure;
        }

        private static Result resolved(Source source, List<Method> methods) {
            return new Result(source, new ArrayList<>(methods), null);
        }

        private static Result failed(Source source, String failure) {
            return new Result(source, Collections.emptyList(), failure);
        }
    }
}
