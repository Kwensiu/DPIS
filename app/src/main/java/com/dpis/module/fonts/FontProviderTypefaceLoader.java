package com.dpis.module.fonts;

import android.content.Context;
import android.graphics.Typeface;
import android.os.ParcelFileDescriptor;

import java.lang.reflect.Method;

/**
 * Loads one imported face in the target app process through DPIS's guarded provider.
 * The provider enforces the package-to-face authorization; this class has no file-path access.
 */
public final class FontProviderTypefaceLoader {
    private FontProviderTypefaceLoader() {
    }

    public static Typeface load(String typefaceId, int ttcIndex) {
        Context context = resolveCurrentApplication();
        if (context == null || typefaceId == null || typefaceId.isBlank()) {
            return null;
        }
        try (ParcelFileDescriptor descriptor = context.getContentResolver().openFileDescriptor(
                FontFileProvider.buildFaceUri(typefaceId), "r")) {
            return descriptor != null
                    ? FontTypefaceLoader.load(descriptor.getFileDescriptor(), ttcIndex)
                    : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Context resolveCurrentApplication() {
        try {
            Class<?> activityThread = Class.forName("android.app.ActivityThread", false,
                    ClassLoader.getSystemClassLoader());
            Method currentApplication = activityThread.getDeclaredMethod("currentApplication");
            Object application = currentApplication.invoke(null);
            return application instanceof Context context ? context : null;
        } catch (Throwable ignored) {
            return null;
        }
    }
}
