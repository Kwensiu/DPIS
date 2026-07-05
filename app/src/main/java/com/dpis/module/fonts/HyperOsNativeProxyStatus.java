package com.dpis.module.fonts;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import java.io.File;

public final class HyperOsNativeProxyStatus {
    private static final String NATIVE_PROXY_LIBRARY_NAME = "libdpis_native.so";

    public enum State {
        PRESENT,
        MISSING,
        UNKNOWN
    }

    public final State state;
    public final String nativeLibraryDir;

    private HyperOsNativeProxyStatus(State state, String nativeLibraryDir) {
        this.state = state;
        this.nativeLibraryDir = nativeLibraryDir;
    }

    public static HyperOsNativeProxyStatus inspect(Context context, String packageName) {
        if (context == null || packageName == null || packageName.isBlank()) {
            return new HyperOsNativeProxyStatus(State.UNKNOWN, null);
        }
        try {
            ApplicationInfo applicationInfo = context.getPackageManager()
                    .getApplicationInfo(packageName, 0);
            return inspectNativeLibraryDir(applicationInfo.nativeLibraryDir);
        } catch (PackageManager.NameNotFoundException | RuntimeException ignored) {
            return new HyperOsNativeProxyStatus(State.UNKNOWN, null);
        }
    }

    public static HyperOsNativeProxyStatus inspectNativeLibraryDir(String nativeLibraryDir) {
        if (nativeLibraryDir == null || nativeLibraryDir.isBlank()) {
            return new HyperOsNativeProxyStatus(State.UNKNOWN, nativeLibraryDir);
        }
        File nativeDir = new File(nativeLibraryDir);
        File proxy = new File(nativeDir, NATIVE_PROXY_LIBRARY_NAME);
        if (proxy.isFile() && proxy.length() > 0) {
            return new HyperOsNativeProxyStatus(State.PRESENT, nativeLibraryDir);
        }
        if (nativeDir.isDirectory()) {
            return new HyperOsNativeProxyStatus(State.MISSING, nativeLibraryDir);
        }
        return new HyperOsNativeProxyStatus(State.UNKNOWN, nativeLibraryDir);
    }

    public boolean isPresent() {
        return state == State.PRESENT;
    }
}
