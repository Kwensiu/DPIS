package com.dpis.module.runtime.hyperos

import android.content.Context
import android.os.Build
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object HyperOsNativeProxyAssetExporter {
    private const val NATIVE_ASSET_ROOT = "native"
    private const val NATIVE_PROXY_LIBRARY_NAME = "libdpis_native.so"

    fun interface Logger {
        fun error(message: String, throwable: Throwable?)
    }

    @JvmStatic
    fun exportBundledNativeProxyLibrary(context: Context?, logger: Logger?) {
        if (context == null) return
        val assetPath = resolveNativeProxyAssetPath(
            listNativeProxyAssetPaths(context),
            Build.SUPPORTED_ABIS,
        )
        if (assetPath == null) {
            logError(
                logger,
                "DPIS_FONT HyperOS proxy library export failed: no matching native asset",
                null
            )
            return
        }
        val output = File(context.filesDir, NATIVE_PROXY_LIBRARY_NAME)
        try {
            context.assets.open(assetPath).use { input ->
                val temp = File(context.filesDir, "$NATIVE_PROXY_LIBRARY_NAME.tmp")
                FileOutputStream(temp).use { stream ->
                    val buffer = ByteArray(16 * 1024)
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        stream.write(buffer, 0, read)
                    }
                }
                if (!temp.renameTo(output)) {
                    throw IOException("rename failed: $temp -> $output")
                }
                output.setReadable(true, false)
                output.setExecutable(true, false)
            }
        } catch (throwable: IOException) {
            logError(logger, "DPIS_FONT HyperOS proxy library export failed", throwable)
        }
    }

    private fun listNativeProxyAssetPaths(context: Context): Set<String> {
        val paths = LinkedHashSet<String>()
        try {
            val abis = context.assets.list(NATIVE_ASSET_ROOT) ?: return paths
            for (abi in abis) {
                if (abi.isBlank()) continue
                val assetPath = "$NATIVE_ASSET_ROOT/$abi/$NATIVE_PROXY_LIBRARY_NAME"
                try {
                    context.assets.open(assetPath).use { paths.add(assetPath) }
                } catch (_: IOException) {
                }
            }
        } catch (_: IOException) {
        }
        return paths
    }

    @JvmStatic
    fun resolveNativeProxyAssetPath(
        availableAssetPaths: Set<String>?,
        supportedAbis: Array<String>?,
    ): String? {
        if (availableAssetPaths.isNullOrEmpty()) return null
        supportedAbis?.forEach { abi ->
            if (abi.isBlank()) return@forEach
            val assetPath = "$NATIVE_ASSET_ROOT/$abi/$NATIVE_PROXY_LIBRARY_NAME"
            if (assetPath in availableAssetPaths) return assetPath
        }
        val arm64Path = "$NATIVE_ASSET_ROOT/arm64-v8a/$NATIVE_PROXY_LIBRARY_NAME"
        return if (arm64Path in availableAssetPaths) arm64Path else availableAssetPaths.first()
    }

    private fun logError(logger: Logger?, message: String, throwable: Throwable?) {
        logger?.error(message, throwable)
    }
}
