package com.dpis.module;

import android.graphics.Rect;
import android.view.View;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;

import io.github.libxposed.api.XposedInterface;

final class ViewRootProbeHookInstaller {
    private static final int MAX_LOGS = 8;
    private static final int MEASURE_SPEC_MODE_MASK = 0x3 << 30;
    private static final int MEASURE_SPEC_EXACTLY = 0x1 << 30;
    private static final int MEASURE_SPEC_AT_MOST = 0x2 << 30;
    private static final AtomicInteger LOG_COUNT = new AtomicInteger();
    private static final AtomicInteger MEASURE_LOG_COUNT = new AtomicInteger();
    private static final AtomicInteger FRAME_LOG_COUNT = new AtomicInteger();
    private static final AtomicInteger RELAYOUT_LOG_COUNT = new AtomicInteger();
    private static final AtomicInteger RESIZED_LOG_COUNT = new AtomicInteger();
    private static volatile boolean hookInstalled;
    private static volatile String lastMeasureLog;
    private static volatile String lastFrameLog;
    private static volatile String lastRelayoutLog;
    private static volatile String lastResizedLog;

    private ViewRootProbeHookInstaller() {
    }

    static void install(XposedInterface xposed) throws ReflectiveOperationException {
        if (hookInstalled) {
            return;
        }
        synchronized (ViewRootProbeHookInstaller.class) {
            if (hookInstalled) {
                return;
            }
            ClassLoader bootClassLoader = ClassLoader.getSystemClassLoader();
            Class<?> viewRootImplClass = Class.forName("android.view.ViewRootImpl", false,
                    bootClassLoader);
            Method performTraversalsMethod = viewRootImplClass.getDeclaredMethod("performTraversals");
            Method performMeasureMethod = viewRootImplClass.getDeclaredMethod(
                    "performMeasure", int.class, int.class);
            Method setFrameMethod = viewRootImplClass.getDeclaredMethod(
                    "setFrame", Rect.class, boolean.class);
            hookNamedMethods(xposed, viewRootImplClass, "relayoutWindow", true);
            hookNamedMethods(xposed, viewRootImplClass, "handleResized", false);
            xposed.hook(performTraversalsMethod)
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        Object thisObject = chain.getThisObject();
                        if (LOG_COUNT.incrementAndGet() <= MAX_LOGS) {
                            DpisLog.i(buildPerformTraversalsLog(thisObject));
                            String rootViewLog = buildRootViewLog(thisObject);
                            if (rootViewLog != null) {
                                DpisLog.i(rootViewLog);
                            }
                        }
                        return chain.proceed();
                    });
            xposed.hook(performMeasureMethod)
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        if (MEASURE_LOG_COUNT.incrementAndGet() <= MAX_LOGS) {
                            java.util.List<Object> args = chain.getArgs();
                            if (args.size() >= 2
                                    && args.get(0) instanceof Integer widthMeasureSpec
                                    && args.get(1) instanceof Integer heightMeasureSpec) {
                                String message = buildMeasureLog(widthMeasureSpec,
                                        heightMeasureSpec);
                                if (!message.equals(lastMeasureLog)) {
                                    lastMeasureLog = message;
                                    DpisLog.i(message);
                                }
                            }
                        }
                        return chain.proceed();
                    });
            xposed.hook(setFrameMethod)
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        if (FRAME_LOG_COUNT.incrementAndGet() <= MAX_LOGS) {
                            java.util.List<Object> args = chain.getArgs();
                            if (args.size() >= 2
                                    && args.get(0) instanceof Rect frame
                                    && args.get(1) instanceof Boolean withinRelayout) {
                                maybeOverrideSetFrame(chain.getThisObject(), frame);
                                String message = buildSetFrameLog(chain.getThisObject(), frame,
                                        withinRelayout);
                                if (!message.equals(lastFrameLog)) {
                                    lastFrameLog = message;
                                    DpisLog.i(message);
                                }
                            }
                        }
                        return chain.proceed();
                    });
            hookInstalled = true;
            DpisLog.i("ViewRoot probe hook ready");
        }
    }

    static String buildPerformTraversalsLog(Object viewRootImpl) {
        return "ViewRoot probe(performTraversals): width="
                + readIntField(viewRootImpl, "mWidth")
                + ", height=" + readIntField(viewRootImpl, "mHeight");
    }

    static String buildViewLog(int width, int height) {
        return "ViewRoot probe(rootView): width=" + width + ", height=" + height;
    }

    static String buildMeasureLog(int widthMeasureSpec, int heightMeasureSpec) {
        return "ViewRoot probe(performMeasure): widthSpec="
                + describeMeasureSpec(widthMeasureSpec)
                + ", heightSpec=" + describeMeasureSpec(heightMeasureSpec);
    }

    static String buildSetFrameLog(int oldWidth, int oldHeight, Rect frame, boolean withinRelayout) {
        int newWidth = frame != null ? frame.width() : -1;
        int newHeight = frame != null ? frame.height() : -1;
        return buildSetFrameLog(oldWidth, oldHeight, newWidth, newHeight, withinRelayout);
    }

    static String buildSetFrameLog(int oldWidth, int oldHeight, int newWidth, int newHeight,
                                   boolean withinRelayout) {
        return "ViewRoot probe(setFrame): withinRelayout=" + withinRelayout
                + ", frame=" + newWidth + "x" + newHeight
                + ", oldWinFrame=" + oldWidth + "x" + oldHeight;
    }

    static String buildRelayoutWindowLog(String stage, int result,
                                         int relayoutFrameWidth, int relayoutFrameHeight,
                                         int tmpFrameWidth, int tmpFrameHeight,
                                         int winFrameWidth, int winFrameHeight) {
        return "ViewRoot probe(relayoutWindow:" + stage + "): result=" + result
                + ", relayoutFrame=" + relayoutFrameWidth + "x" + relayoutFrameHeight
                + ", tmpFrame=" + tmpFrameWidth + "x" + tmpFrameHeight
                + ", winFrame=" + winFrameWidth + "x" + winFrameHeight;
    }

    static String buildHandleResizedLog(int frameWidth, int frameHeight) {
        return "ViewRoot probe(handleResized): frame=" + frameWidth + "x" + frameHeight;
    }

    static boolean shouldOverrideSetFrame(int relayoutFrameWidth, int relayoutFrameHeight,
                                          int frameWidth, int frameHeight,
                                          int targetWidth, int targetHeight) {
        return WindowFrameOverride.isEnabled()
                && WindowFrameOverride.shouldApply(relayoutFrameWidth, relayoutFrameHeight,
                frameWidth, frameHeight, targetWidth, targetHeight);
    }

    private static String buildRootViewLog(Object viewRootImpl) {
        Object rootView = readField(viewRootImpl, "mView");
        if (!(rootView instanceof View view)) {
            return null;
        }
        int width = view.getWidth();
        int height = view.getHeight();
        if (width <= 0 || height <= 0) {
            return null;
        }
        return buildViewLog(width, height);
    }

    private static String buildSetFrameLog(Object viewRootImpl, Rect frame, boolean withinRelayout) {
        return buildSetFrameLog(
                readRectWidth(viewRootImpl, "mWinFrame"),
                readRectHeight(viewRootImpl, "mWinFrame"),
                frame,
                withinRelayout);
    }

    private static void maybeOverrideSetFrame(Object viewRootImpl, Rect frame) {
        if (viewRootImpl == null || frame == null) {
            return;
        }
        VirtualDisplayOverride.Result override = VirtualDisplayState.get();
        if (override == null) {
            return;
        }
        int relayoutFrameWidth = readFirstRectWidth(readField(viewRootImpl, "mRelayoutResult"));
        int relayoutFrameHeight = readFirstRectHeight(readField(viewRootImpl, "mRelayoutResult"));
        if (!shouldOverrideSetFrame(relayoutFrameWidth, relayoutFrameHeight,
                frame.width(), frame.height(), override.widthPx, override.heightPx)) {
            return;
        }
        WindowFrameOverride.apply(frame, override.widthPx, override.heightPx);
    }

    private static void hookNamedMethods(XposedInterface xposed, Class<?> targetClass,
                                         String methodName, boolean logAfter)
            throws ReflectiveOperationException {
        for (Method method : targetClass.getDeclaredMethods()) {
            if (!methodName.equals(method.getName())) {
                continue;
            }
            xposed.hook(method)
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        if (logAfter) {
                            Object result = chain.proceed();
                            logRelayoutWindow(chain.getThisObject(), result);
                            return result;
                        }
                        logHandleResized(chain.getArgs());
                        return chain.proceed();
                    });
        }
    }

    private static void logRelayoutWindow(Object viewRootImpl, Object result) {
        if (RELAYOUT_LOG_COUNT.incrementAndGet() > MAX_LOGS) {
            return;
        }
        String message = buildRelayoutWindowLog(
                "after",
                result instanceof Integer value ? value : -1,
                readFirstRectWidth(readField(viewRootImpl, "mRelayoutResult")),
                readFirstRectHeight(readField(viewRootImpl, "mRelayoutResult")),
                readNestedRectWidth(viewRootImpl, "mTmpFrames", "frame"),
                readNestedRectHeight(viewRootImpl, "mTmpFrames", "frame"),
                readRectWidth(viewRootImpl, "mWinFrame"),
                readRectHeight(viewRootImpl, "mWinFrame"));
        if (!message.equals(lastRelayoutLog)) {
            lastRelayoutLog = message;
            DpisLog.i(message);
        }
    }

    private static void logHandleResized(java.util.List<Object> args) {
        if (RESIZED_LOG_COUNT.incrementAndGet() > MAX_LOGS) {
            return;
        }
        int frameWidth = -1;
        int frameHeight = -1;
        for (Object arg : args) {
            if (arg instanceof Rect rect) {
                frameWidth = rect.width();
                frameHeight = rect.height();
                break;
            }
            int nestedWidth = readNestedRectWidth(arg, "frame");
            int nestedHeight = readNestedRectHeight(arg, "frame");
            if (nestedWidth >= 0 || nestedHeight >= 0) {
                frameWidth = nestedWidth;
                frameHeight = nestedHeight;
                break;
            }
        }
        String message = buildHandleResizedLog(frameWidth, frameHeight);
        if (!message.equals(lastResizedLog)) {
            lastResizedLog = message;
            DpisLog.i(message);
        }
    }

    private static String describeMeasureSpec(int measureSpec) {
        int mode = measureSpec & MEASURE_SPEC_MODE_MASK;
        int size = measureSpec & ~MEASURE_SPEC_MODE_MASK;
        String modeLabel;
        if (mode == MEASURE_SPEC_EXACTLY) {
            modeLabel = "EXACTLY";
        } else if (mode == MEASURE_SPEC_AT_MOST) {
            modeLabel = "AT_MOST";
        } else {
            modeLabel = "UNSPECIFIED";
        }
        return modeLabel + "(" + size + ")";
    }

    private static int readIntField(Object target, String fieldName) {
        if (target == null) {
            return -1;
        }
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.getInt(target);
        } catch (ReflectiveOperationException ignored) {
            return -1;
        }
    }

    private static Object readField(Object target, String fieldName) {
        if (target == null) {
            return null;
        }
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(target);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static int readRectWidth(Object target, String fieldName) {
        Object value = readField(target, fieldName);
        return value instanceof Rect rect ? rect.width() : -1;
    }

    private static int readRectHeight(Object target, String fieldName) {
        Object value = readField(target, fieldName);
        return value instanceof Rect rect ? rect.height() : -1;
    }

    private static int readNestedRectWidth(Object target, String... fieldNames) {
        Object value = readNestedField(target, fieldNames);
        return value instanceof Rect rect ? rect.width() : -1;
    }

    private static int readNestedRectHeight(Object target, String... fieldNames) {
        Object value = readNestedField(target, fieldNames);
        return value instanceof Rect rect ? rect.height() : -1;
    }

    private static int readFirstRectWidth(Object target) {
        Rect rect = findFirstRect(target, 0);
        return rect != null ? rect.width() : -1;
    }

    private static int readFirstRectHeight(Object target) {
        Rect rect = findFirstRect(target, 0);
        return rect != null ? rect.height() : -1;
    }

    private static Object readNestedField(Object target, String... fieldNames) {
        Object current = target;
        for (String fieldName : fieldNames) {
            current = readField(current, fieldName);
            if (current == null) {
                return null;
            }
        }
        return current;
    }

    private static Rect findFirstRect(Object target, int depth) {
        if (target == null || depth > 4) {
            return null;
        }
        if (target instanceof Rect rect) {
            return rect;
        }
        for (Field field : target.getClass().getDeclaredFields()) {
            if (field.getType().isPrimitive() || field.getType().isEnum()) {
                continue;
            }
            if ("this$0".equals(field.getName())) {
                continue;
            }
            try {
                field.setAccessible(true);
                Object value = field.get(target);
                if (value == null || value == target) {
                    continue;
                }
                Rect nestedRect = findFirstRect(value, depth + 1);
                if (nestedRect != null) {
                    return nestedRect;
                }
            } catch (ReflectiveOperationException ignored) {
                // Probe only.
            }
        }
        return null;
    }

}
