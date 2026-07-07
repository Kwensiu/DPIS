package com.dpis.module.runtime.font;

import com.dpis.module.DpisConfigStore;

import com.dpis.module.fonts.FontApplyMode;

import com.dpis.module.BuildConfig;
import com.dpis.module.DpisLog;

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.app.Activity;
import android.view.Choreographer;
import android.view.View;

import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import io.github.libxposed.api.XposedInterface;

public final class HyperOsFlutterFontHookInstaller {
    private static final boolean DEBUG_PROBES = BuildConfig.DEBUG;
    private static final ScheduledExecutorService FLUTTER_PROBE_EXECUTOR =
            Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
                @Override
                public Thread newThread(Runnable runnable) {
                    Thread thread = new Thread(runnable, "dpis-flutter-probe");
                    thread.setDaemon(true);
                    return thread;
                }
            });
    private static final int MAX_FLUTTER_VIEW_ATTACH_PROBES = 8;
    private static final int MAX_FRAME_PROBES = 12;
    private static final AtomicInteger FLUTTER_VIEW_ATTACH_PROBE_BUDGET =
            new AtomicInteger(MAX_FLUTTER_VIEW_ATTACH_PROBES);
    private static final AtomicInteger FRAME_PROBE_BUDGET =
            new AtomicInteger(MAX_FRAME_PROBES);
    private static final int MAX_VIEW_ROOT_PROBES = 16;
    private static final AtomicInteger VIEW_ROOT_PROBE_BUDGET =
            new AtomicInteger(MAX_VIEW_ROOT_PROBES);
    private static final AtomicBoolean VIEW_ROOT_PROBE_INSTALLED = new AtomicBoolean();
    private static final int MAX_HANDLER_PROBES = 12;
    private static final AtomicInteger HANDLER_PROBE_BUDGET =
            new AtomicInteger(MAX_HANDLER_PROBES);
    private static final AtomicBoolean HANDLER_PROBE_INSTALLED = new AtomicBoolean();

    private HyperOsFlutterFontHookInstaller() {
    }

    public static void install(XposedInterface xposed, String packageName, DpisConfigStore store) {
        if (store == null) {
            return;
        }
        Integer targetFontScalePercent = store.getTargetFontScalePercent(packageName);
        if (targetFontScalePercent == null || targetFontScalePercent <= 0) {
            return;
        }
        String targetFontMode = store.getTargetFontApplyMode(packageName);
        if (!FontApplyMode.isEnabled(targetFontMode)) {
            return;
        }
        try {
            loadNativeLibrary();
            configure(packageName, targetFontScalePercent, true);
            installRuntimeLibraryProbe(xposed, packageName);
            installFlutterViewAttachProbe(xposed, packageName);
            installDebugOnlyProbes(xposed, packageName);
            if (DEBUG_PROBES) {
                logGenericFlutterProbe(packageName, "post-configure");
                scheduleDelayedGenericFlutterProbe(packageName);
                logGenericFlutterProbe(packageName, "post-install");
                DpisLog.i("DPIS_FONT Flutter native font probe configured: package="
                        + packageName + ", targetFontScalePercent=" + targetFontScalePercent
                        + ", hyperOsHookEnabled=" + store.isHyperOsFlutterFontHookEnabled());
            }
        } catch (Throwable throwable) {
            DpisLog.e("DPIS_FONT Flutter native font probe configure failed: package="
                    + packageName, throwable);
        }
    }

    private static void installDebugOnlyProbes(XposedInterface xposed, String packageName) {
        if (!DEBUG_PROBES) {
            return;
        }
        scheduleMainThreadGenericFlutterProbe(packageName);
        scheduleOneShotThreadGenericFlutterProbe(packageName);
        scheduleLateMapsProbe(packageName);
        installActivityResumeProbe(xposed, packageName);
        installFrameProbe(xposed, packageName);
        installViewRootTraversalProbe(xposed, packageName);
        installHandlerDispatchProbe(xposed, packageName);
    }

    private static void installHandlerDispatchProbe(XposedInterface xposed, String packageName) {
        if (xposed == null || !HANDLER_PROBE_INSTALLED.compareAndSet(false, true)) {
            return;
        }
        try {
            Method method = Handler.class.getDeclaredMethod("dispatchMessage", Message.class);
            xposed.hook(method)
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        Object result = chain.proceed();
                        int remaining = HANDLER_PROBE_BUDGET.getAndDecrement();
                        if (remaining > 0) {
                            logGenericFlutterProbe(packageName, "handler-" + remaining);
                        }
                        return result;
                    });
            DpisLog.i("DPIS_FONT Handler dispatch Flutter probe ready for " + packageName);
        } catch (Throwable throwable) {
            HANDLER_PROBE_INSTALLED.set(false);
            DpisLog.e("DPIS_FONT Handler dispatch Flutter probe failed for "
                    + packageName, throwable);
        }
    }

    @SuppressLint("SoonBlockedPrivateApi")
    private static void installViewRootTraversalProbe(XposedInterface xposed, String packageName) {
        if (xposed == null || !VIEW_ROOT_PROBE_INSTALLED.compareAndSet(false, true)) {
            return;
        }
        try {
            Class<?> viewRootImplClass = Class.forName("android.view.ViewRootImpl", false,
                    ClassLoader.getSystemClassLoader());
            Method method = viewRootImplClass.getDeclaredMethod("performTraversals");
            xposed.hook(method)
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        Object result = chain.proceed();
                        int remaining = VIEW_ROOT_PROBE_BUDGET.getAndDecrement();
                        if (remaining > 0) {
                            logGenericFlutterProbe(packageName, "view-root-" + remaining);
                        }
                        return result;
                    });
            DpisLog.i("DPIS_FONT ViewRoot traversal Flutter probe ready for " + packageName);
        } catch (Throwable throwable) {
            VIEW_ROOT_PROBE_INSTALLED.set(false);
            DpisLog.e("DPIS_FONT ViewRoot traversal Flutter probe failed for "
                    + packageName, throwable);
        }
    }

    private static void installFrameProbe(XposedInterface xposed, String packageName) {
        if (xposed == null) {
            return;
        }
        for (Method method : Choreographer.class.getDeclaredMethods()) {
            if (!isChoreographerFrameMethod(method)) {
                continue;
            }
            try {
                xposed.hook(method)
                        .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                        .intercept(chain -> {
                            Object result = chain.proceed();
                            int remaining = FRAME_PROBE_BUDGET.getAndDecrement();
                            if (remaining > 0) {
                                logGenericFlutterProbe(packageName, "frame-" + remaining);
                            }
                            return result;
                        });
                DpisLog.i("DPIS_FONT Choreographer frame Flutter probe ready: method="
                        + method.getName() + " for " + packageName);
                return;
            } catch (Throwable throwable) {
                DpisLog.e("DPIS_FONT Choreographer frame Flutter probe failed: method="
                        + method.getName() + " for " + packageName, throwable);
            }
        }
    }

    private static boolean isChoreographerFrameMethod(Method method) {
        if (!"doFrame".equals(method.getName())) {
            return false;
        }
        Class<?>[] parameterTypes = method.getParameterTypes();
        return parameterTypes.length >= 1 && parameterTypes[0] == long.class;
    }

    private static void installActivityResumeProbe(XposedInterface xposed, String packageName) {
        if (xposed == null) {
            return;
        }
        try {
            Method method = Activity.class.getDeclaredMethod("onResume");
            xposed.hook(method)
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        Object result = chain.proceed();
                        logGenericFlutterProbe(packageName, "activity-resume");
                        return result;
                    });
            DpisLog.i("DPIS_FONT Activity resume Flutter probe ready for " + packageName);
        } catch (Throwable throwable) {
            DpisLog.e("DPIS_FONT Activity resume Flutter probe failed for "
                    + packageName, throwable);
        }
    }

    private static void installFlutterViewAttachProbe(XposedInterface xposed, String packageName) {
        if (xposed == null) {
            return;
        }
        try {
            Method method = View.class.getDeclaredMethod("onAttachedToWindow");
            xposed.hook(method)
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        Object result = chain.proceed();
                        Object view = chain.getThisObject();
                        if (view != null && isFlutterViewClassName(view.getClass().getName())
                                && FLUTTER_VIEW_ATTACH_PROBE_BUDGET.getAndDecrement() > 0) {
                            logGenericFlutterProbe(packageName,
                                    "flutter-view-attached " + view.getClass().getName());
                        }
                        return result;
                    });
            DpisLog.i("DPIS_FONT Flutter view attach probe ready for " + packageName);
        } catch (Throwable throwable) {
            DpisLog.e("DPIS_FONT Flutter view attach probe failed for "
                    + packageName, throwable);
        }
    }

    public static boolean isFlutterViewClassNameForTest(String className) {
        return isFlutterViewClassName(className);
    }

    private static boolean isFlutterViewClassName(String className) {
        if (className == null || className.isEmpty()) {
            return false;
        }
        String lower = className.toLowerCase(java.util.Locale.ROOT);
        return lower.contains("flutter");
    }

    private static void loadNativeLibrary() {
        try {
            System.loadLibrary("dpis_native");
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
        if (moduleApkPath == null || moduleApkPath.isEmpty()) {
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
                        "libdpis_native.so");
                if (candidate.isFile()) {
                    return candidate.getAbsolutePath();
                }
            }
        }
        return null;
    }

    private static String resolveModuleApkPath() {
        ClassLoader classLoader = HyperOsFlutterFontHookInstaller.class.getClassLoader();
        if (classLoader == null) {
            return null;
        }
        return parseModuleApkPathForTest(classLoader.toString());
    }

    public static String parseModuleApkPathForTest(String classLoaderText) {
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

    public static String[] nativeDirectoryNamesForAbi(String abi) {
        if ("arm64-v8a".equals(abi)) {
            return new String[] {"arm64", "arm64-v8a"};
        }
        if ("armeabi-v7a".equals(abi)) {
            return new String[] {"arm", "armeabi-v7a"};
        }
        return new String[] {abi};
    }

    private static void installRuntimeLibraryProbe(XposedInterface xposed, String packageName) {
        if (xposed == null) {
            return;
        }
        for (Method method : Runtime.class.getDeclaredMethods()) {
            if (!isRuntimeLoadMethod(method)) {
                continue;
            }
            try {
                xposed.hook(method)
                        .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                        .intercept(chain -> {
                            Object result = chain.proceed();
                            String loadedName = findLoadedLibraryName(chain.getArgs());
                            if (isFlutterLibraryName(loadedName)) {
                                onRuntimeLibraryLoaded(packageName, loadedName);
                                logGenericFlutterProbe(packageName, "runtime-load " + loadedName);
                            }
                            return result;
                        });
                DpisLog.i("DPIS_FONT Flutter runtime library probe ready: method="
                        + method.getName() + " for " + packageName);
            } catch (Throwable throwable) {
                DpisLog.e("DPIS_FONT Flutter runtime library probe failed: method="
                        + method.getName() + " for " + packageName, throwable);
            }
        }
    }

    private static boolean isRuntimeLoadMethod(Method method) {
        String name = method.getName();
        if (!"loadLibrary0".equals(name) && !"load0".equals(name)) {
            return false;
        }
        Class<?>[] parameterTypes = method.getParameterTypes();
        return parameterTypes.length >= 2
                && parameterTypes[parameterTypes.length - 1] == String.class;
    }

    private static String findLoadedLibraryName(List<Object> args) {
        if (args == null) {
            return "";
        }
        for (int i = args.size() - 1; i >= 0; i--) {
            if (args.get(i) instanceof String value) {
                return value;
            }
        }
        return "";
    }

    public static boolean isFlutterLibraryNameForTest(String name) {
        return isFlutterLibraryName(name);
    }

    private static boolean isFlutterLibraryName(String name) {
        if (name == null || name.isEmpty()) {
            return false;
        }
        return "flutter".equals(name)
                || name.endsWith("/libflutter.so")
                || name.endsWith("\\libflutter.so")
                || "libflutter.so".equals(name)
                || name.endsWith("/libhyper_os_flutter.so")
                || name.endsWith("\\libhyper_os_flutter.so")
                || "libhyper_os_flutter.so".equals(name);
    }

    private static void logGenericFlutterProbe(String packageName, String source) {
        try {
            String status = genericFlutterProbeStatus(packageName, source);
            if (parseFlutterBaseForTest(status) == 0L) {
                long mapsBase = findMappedLibraryBaseForTest("libflutter.so");
                if (mapsBase != 0L) {
                    status += " javaMapsBase=" + mapsBase;
                }
            }
            DpisLog.i("DPIS_FONT " + status);
        } catch (Throwable throwable) {
            DpisLog.e("DPIS_FONT Generic Flutter probe status failed: package="
                    + packageName + ", source=" + source, throwable);
        }
    }

    private static void scheduleDelayedGenericFlutterProbe(String packageName) {
        long[] delays = {500L, 1500L, 3500L, 7000L};
        for (long delay : delays) {
            FLUTTER_PROBE_EXECUTOR.schedule(
                    () -> logGenericFlutterProbe(packageName, "delayed-" + delay + "ms"),
                    delay,
                    TimeUnit.MILLISECONDS);
        }
    }

    private static void scheduleOneShotThreadGenericFlutterProbe(String packageName) {
        Thread thread = new Thread(() -> {
            try {
                Thread.sleep(8000L);
                logGenericFlutterProbe(packageName, "thread-delayed-8000ms");
            } catch (InterruptedException interruptedException) {
                Thread.currentThread().interrupt();
                DpisLog.e("DPIS_FONT thread-delayed Flutter probe interrupted: package="
                        + packageName, interruptedException);
            } catch (Throwable throwable) {
                DpisLog.e("DPIS_FONT thread-delayed Flutter probe failed: package="
                        + packageName, throwable);
            }
        }, "dpis-flutter-status");
        thread.setDaemon(true);
        thread.start();
        DpisLog.i("DPIS_FONT thread-delayed Flutter probe scheduled for " + packageName);
    }

    private static void scheduleLateMapsProbe(String packageName) {
        long[] delays = {8000L, 15000L, 25000L};
        for (long delay : delays) {
            FLUTTER_PROBE_EXECUTOR.schedule(
                    () -> logMappedLibraries(packageName, "maps-delayed-" + delay + "ms"),
                    delay,
                    TimeUnit.MILLISECONDS);
        }
    }

    private static void logMappedLibraries(String packageName, String source) {
        try {
            boolean app = findMappedLibraryBaseForTest("libapp.so") != 0L;
            boolean flutter = findMappedLibraryBaseForTest("libflutter.so") != 0L;
            boolean dpis = findMappedLibraryBaseForTest("libdpis_native.so") != 0L;
            boolean webview = findMappedLibraryBaseForTest("libwebviewchromium") != 0L
                    || findMappedLibraryBaseForTest("webview") != 0L;
            DpisLog.i("DPIS_FONT Flutter late maps probe: package=" + packageName
                    + ", source=" + source
                    + ", libapp=" + app
                    + ", libflutter=" + flutter
                    + ", libdpisNative=" + dpis
                    + ", webview=" + webview);
        } catch (Throwable throwable) {
            DpisLog.e("DPIS_FONT Flutter late maps probe failed: package="
                    + packageName + ", source=" + source, throwable);
        }
    }

    private static void scheduleMainThreadGenericFlutterProbe(String packageName) {
        try {
            Handler handler = new Handler(Looper.getMainLooper());
            long[] delays = {500L, 1500L, 3500L, 7000L, 12000L};
            for (long delay : delays) {
                handler.postDelayed(
                        () -> logGenericFlutterProbe(packageName, "main-delayed-" + delay + "ms"),
                        delay);
            }
        } catch (Throwable throwable) {
            DpisLog.e("DPIS_FONT main-thread Flutter probe schedule failed: package="
                    + packageName, throwable);
        }
    }

    public static long parseFlutterBaseForTest(String status) {
        if (status == null || status.isEmpty()) {
            return 0L;
        }
        String marker = " base=";
        int start = status.indexOf(marker);
        if (start < 0) {
            return 0L;
        }
        start += marker.length();
        int end = status.indexOf(' ', start);
        if (end < 0) {
            end = status.length();
        }
        try {
            return Long.parseLong(status.substring(start, end));
        } catch (NumberFormatException ignored) {
            return 0L;
        }
    }

    public static long findMappedLibraryBaseForTest(String libraryName) {
        if (libraryName == null || libraryName.isEmpty()) {
            return 0L;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader("/proc/self/maps"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.contains(libraryName) || !line.contains("r-xp")) {
                    continue;
                }
                return parseMapsStartAddressForTest(line);
            }
        } catch (IOException ignored) {
        }
        return 0L;
    }

    public static long parseMapsStartAddressForTest(String line) {
        if (line == null || line.isEmpty()) {
            return 0L;
        }
        int separator = line.indexOf('-');
        if (separator <= 0) {
            return 0L;
        }
        try {
            return Long.parseUnsignedLong(line.substring(0, separator), 16);
        } catch (NumberFormatException ignored) {
            return 0L;
        }
    }

    private static native void configure(String packageName, int targetFontScalePercent, boolean enabled);

    private static native void onRuntimeLibraryLoaded(String packageName, String libraryName);

    private static native String genericFlutterProbeStatus(String packageName, String source);
}
