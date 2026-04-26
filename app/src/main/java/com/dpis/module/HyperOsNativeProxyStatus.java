package com.dpis.module;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import java.io.File;

final class HyperOsNativeProxyStatus {
    private static final String NATIVE_PROXY_LIBRARY_NAME = "libdpis_native.so";

    enum State {
        PRESENT,
        MISSING,
        UNKNOWN
    }

    final State state;
    final String nativeLibraryDir;

    private HyperOsNativeProxyStatus(State state, String nativeLibraryDir) {
        this.state = state;
        this.nativeLibraryDir = nativeLibraryDir;
    }

    static HyperOsNativeProxyStatus inspect(Context context, String packageName) {
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

    static HyperOsNativeProxyStatus inspectNativeLibraryDir(String nativeLibraryDir) {
        if (nativeLibraryDir == null || nativeLibraryDir.isBlank()) {
            return new HyperOsNativeProxyStatus(State.UNKNOWN, nativeLibraryDir);
        }
        File nativeDir = new File(nativeLibraryDir);
        File proxy = new File(nativeDir, NATIVE_PROXY_LIBRARY_NAME);
        if (proxy.isFile()) {
            return new HyperOsNativeProxyStatus(State.PRESENT, nativeLibraryDir);
        }
        if (nativeDir.isDirectory()) {
            return new HyperOsNativeProxyStatus(State.MISSING, nativeLibraryDir);
        }
        return new HyperOsNativeProxyStatus(State.UNKNOWN, nativeLibraryDir);
    }

    boolean isPresent() {
        return state == State.PRESENT;
    }
}
