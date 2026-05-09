package com.dpis.module;

import android.content.Context;
import android.os.Build;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashSet;
import java.util.Set;

final class HyperOsNativeProxyAssetExporter {
    private static final String NATIVE_ASSET_ROOT = "native";
    private static final String NATIVE_PROXY_LIBRARY_NAME = "libdpis_native.so";

    private HyperOsNativeProxyAssetExporter() {
    }

    static void exportBundledNativeProxyLibrary(Context context) {
        if (context == null) {
            return;
        }
        String assetPath = resolveNativeProxyAssetPath(
                listNativeProxyAssetPaths(context), Build.SUPPORTED_ABIS);
        if (assetPath == null) {
            DpisLog.e("DPIS_FONT HyperOS proxy library export failed: no matching native asset", null);
            return;
        }
        File output = new File(context.getFilesDir(), NATIVE_PROXY_LIBRARY_NAME);
        try (InputStream input = context.getAssets().open(assetPath)) {
            File temp = new File(context.getFilesDir(), NATIVE_PROXY_LIBRARY_NAME + ".tmp");
            try (FileOutputStream stream = new FileOutputStream(temp)) {
                byte[] buffer = new byte[16 * 1024];
                int read;
                while ((read = input.read(buffer)) != -1) {
                    stream.write(buffer, 0, read);
                }
            }
            if (!temp.renameTo(output)) {
                throw new IOException("rename failed: " + temp + " -> " + output);
            }
            output.setReadable(true, false);
            output.setExecutable(true, false);
        } catch (IOException throwable) {
            DpisLog.e("DPIS_FONT HyperOS proxy library export failed", throwable);
        }
    }

    private static Set<String> listNativeProxyAssetPaths(Context context) {
        LinkedHashSet<String> paths = new LinkedHashSet<>();
        try {
            String[] abis = context.getAssets().list(NATIVE_ASSET_ROOT);
            if (abis == null) {
                return paths;
            }
            for (String abi : abis) {
                if (abi == null || abi.isBlank()) {
                    continue;
                }
                String assetPath = NATIVE_ASSET_ROOT + "/" + abi + "/" + NATIVE_PROXY_LIBRARY_NAME;
                try (InputStream ignored = context.getAssets().open(assetPath)) {
                    paths.add(assetPath);
                } catch (IOException ignored) {
                }
            }
        } catch (IOException ignored) {
        }
        return paths;
    }

    static String resolveNativeProxyAssetPath(Set<String> availableAssetPaths, String[] supportedAbis) {
        if (availableAssetPaths == null || availableAssetPaths.isEmpty()) {
            return null;
        }
        if (supportedAbis != null) {
            for (String abi : supportedAbis) {
                if (abi == null || abi.isBlank()) {
                    continue;
                }
                String assetPath = NATIVE_ASSET_ROOT + "/" + abi + "/" + NATIVE_PROXY_LIBRARY_NAME;
                if (availableAssetPaths.contains(assetPath)) {
                    return assetPath;
                }
            }
        }
        String arm64Path = NATIVE_ASSET_ROOT + "/arm64-v8a/" + NATIVE_PROXY_LIBRARY_NAME;
        if (availableAssetPaths.contains(arm64Path)) {
            return arm64Path;
        }
        return availableAssetPaths.iterator().next();
    }
}
