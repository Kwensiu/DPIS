package com.dpis.module.runtime.hyperos

import android.content.Context
import android.content.pm.PackageManager
import java.io.File

class HyperOsNativeProxyStatus private constructor(
    @JvmField val state: State,
    @JvmField val nativeLibraryDir: String?,
) {
    enum class State {
        PRESENT,
        MISSING,
        UNKNOWN,
    }

    fun isPresent(): Boolean = state == State.PRESENT

    companion object {
        private const val NATIVE_PROXY_LIBRARY_NAME = "libdpis_native.so"

        @JvmStatic
        fun inspect(context: Context?, packageName: String?): HyperOsNativeProxyStatus {
            if (context == null || packageName.isNullOrBlank()) {
                return HyperOsNativeProxyStatus(State.UNKNOWN, null)
            }
            return try {
                val applicationInfo = context.packageManager.getApplicationInfo(packageName, 0)
                inspectNativeLibraryDir(applicationInfo.nativeLibraryDir)
            } catch (_: PackageManager.NameNotFoundException) {
                HyperOsNativeProxyStatus(State.UNKNOWN, null)
            } catch (_: RuntimeException) {
                HyperOsNativeProxyStatus(State.UNKNOWN, null)
            }
        }

        @JvmStatic
        fun inspectNativeLibraryDir(nativeLibraryDir: String?): HyperOsNativeProxyStatus {
            if (nativeLibraryDir.isNullOrBlank()) {
                return HyperOsNativeProxyStatus(State.UNKNOWN, nativeLibraryDir)
            }
            val nativeDir = File(nativeLibraryDir)
            val proxy = File(nativeDir, NATIVE_PROXY_LIBRARY_NAME)
            return when {
                proxy.isFile && proxy.length() > 0 ->
                    HyperOsNativeProxyStatus(State.PRESENT, nativeLibraryDir)

                nativeDir.isDirectory ->
                    HyperOsNativeProxyStatus(State.MISSING, nativeLibraryDir)

                else ->
                    HyperOsNativeProxyStatus(State.UNKNOWN, nativeLibraryDir)
            }
        }
    }
}
