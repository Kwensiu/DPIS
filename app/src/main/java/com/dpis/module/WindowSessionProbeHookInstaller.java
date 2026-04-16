package com.dpis.module;

import android.graphics.Rect;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import io.github.libxposed.api.XposedInterface;

final class WindowSessionProbeHookInstaller {
    private static final int MAX_LOGS = 12;
    private static final AtomicInteger LOG_COUNT = new AtomicInteger();
    private static volatile boolean hookInstalled;
    private static volatile String lastLog;

    private WindowSessionProbeHookInstaller() {
    }

    static void install(XposedInterface xposed) throws ReflectiveOperationException {
        if (hookInstalled) {
            return;
        }
        synchronized (WindowSessionProbeHookInstaller.class) {
            if (hookInstalled) {
                return;
            }
            ClassLoader bootClassLoader = ClassLoader.getSystemClassLoader();
            for (Class<?> clazz : resolveSessionClasses(bootClassLoader)) {
                hookNamedMethods(xposed, clazz, "relayout");
                hookNamedMethods(xposed, clazz, "relayoutAsync");
            }
            hookInstalled = true;
            DpisLog.i("WindowSession probe hook ready");
        }
    }

    static String buildLog(String methodName, String stage, int result, int frameWidth,
                           int frameHeight) {
        if ("before".equals(stage)) {
            return "WindowSession probe(" + methodName + ":" + stage + "): frame="
                    + frameWidth + "x" + frameHeight;
        }
        return "WindowSession probe(" + methodName + ":" + stage + "): result=" + result
                + ", frame=" + frameWidth + "x" + frameHeight;
    }

    private static Set<Class<?>> resolveSessionClasses(ClassLoader classLoader)
            throws ReflectiveOperationException {
        Set<Class<?>> classes = new LinkedHashSet<>();
        classes.add(Class.forName("android.view.IWindowSession$Stub$Proxy", false, classLoader));
        return classes;
    }

    private static void hookNamedMethods(XposedInterface xposed, Class<?> targetClass,
                                         String methodName) throws ReflectiveOperationException {
        for (Method method : targetClass.getDeclaredMethods()) {
            if (!methodName.equals(method.getName())) {
                continue;
            }
            if (Modifier.isAbstract(method.getModifiers())) {
                continue;
            }
            xposed.hook(method)
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        log(methodName, "before", -1, chain.getArgs());
                        Object result = chain.proceed();
                        log(methodName, "after",
                                result instanceof Integer value ? value : -1,
                                chain.getArgs());
                        return result;
                    });
        }
    }

    private static void log(String methodName, String stage, int result,
                            List<Object> args) {
        if (LOG_COUNT.incrementAndGet() > MAX_LOGS) {
            return;
        }
        Rect frame = findFrame(args);
        int width = frame != null ? frame.width() : -1;
        int height = frame != null ? frame.height() : -1;
        String message = buildLog(methodName, stage, result, width, height);
        if (!message.equals(lastLog)) {
            lastLog = message;
            DpisLog.i(message);
        }
    }

    static Rect findFrameForTest(List<Object> args) {
        return findFrame(args);
    }

    private static Rect findFrame(List<Object> args) {
        for (Object arg : args) {
            Rect frame = findRectRecursive(arg, 0);
            if (frame != null) {
                return frame;
            }
        }
        return null;
    }

    private static Rect findRectRecursive(Object target, int depth) {
        if (target == null || depth > 4) {
            return null;
        }
        if (target instanceof Rect rect) {
            return rect;
        }
        Rect directFrame = readRectField(target, "frame");
        if (directFrame != null) {
            return directFrame;
        }
        for (String fieldName : new String[]{"frames", "windowFrames", "outFrames", "result"}) {
            Object nested = readField(target, fieldName);
            Rect nestedRect = findRectRecursive(nested, depth + 1);
            if (nestedRect != null) {
                return nestedRect;
            }
        }
        for (Field field : target.getClass().getDeclaredFields()) {
            if (field.getType().isPrimitive() || field.getType().isEnum()) {
                continue;
            }
            try {
                field.setAccessible(true);
                Object nested = field.get(target);
                if (nested == null || nested == target) {
                    continue;
                }
                if (Objects.equals(field.getName(), "this$0")) {
                    continue;
                }
                Rect nestedRect = findRectRecursive(nested, depth + 1);
                if (nestedRect != null) {
                    return nestedRect;
                }
            } catch (ReflectiveOperationException ignored) {
                // Keep probing other fields.
            }
        }
        return null;
    }

    private static Rect readRectField(Object target, String fieldName) {
        Object value = readField(target, fieldName);
        return value instanceof Rect rect ? rect : null;
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
}
