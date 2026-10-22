package com.dpis.module.runtime.hyperos

import com.dpis.module.config.PerAppDisplayConfigSource
import com.dpis.module.diagnostics.DpisLog
import com.dpis.module.hyperos.HyperOsRustProcessArgPolicy
import com.dpis.module.runtime.font.HyperOsFlutterFontBridge
import com.dpis.module.runtime.systemserver.selection.PackageNames
import com.dpis.module.runtime.systemserver.selection.PackageUidResolver
import io.github.libxposed.api.XposedInterface
import java.io.File
import java.lang.reflect.Modifier

class HyperOsRustProcessHookInstaller private constructor() {
    companion object {
        private const val RUST_PROCESS_IMPL = "android.os.RustProcessImpl"
        private const val START_RUST_PROCESS = "startRustProcess"
        private const val NATIVE_LIBRARY_NAME = "libdpis_native.so"

        @JvmStatic
        fun install(
            xposed: XposedInterface,
            source: PerAppDisplayConfigSource?,
        ): Boolean {
            val clazz = resolveClass(RUST_PROCESS_IMPL)
            if (clazz == null) {
                DpisLog.i("DPIS_FONT HyperOS Rust process hook missing: class=$RUST_PROCESS_IMPL")
                return false
            }
            var hooked = false
            for (method in clazz.declaredMethods) {
                if (START_RUST_PROCESS != method.name || Modifier.isAbstract(method.modifiers)) {
                    continue
                }
                xposed.hook(method)
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                    .intercept { chain ->
                        try {
                            val args = chain.args
                            logTargetArgumentProbe(args)
                            val updatedArgs = applyEnvironmentArgs(source, args)
                            if (updatedArgs != null) {
                                return@intercept chain.proceed(updatedArgs)
                            }
                        } catch (throwable: Throwable) {
                            DpisLog.e("DPIS_FONT HyperOS Rust process env hook failed", throwable)
                        }
                        chain.proceed()
                    }
                hooked = true
            }
            if (hooked) {
                DpisLog.i(
                    "DPIS_FONT HyperOS Rust process hook ready: " +
                            "class=$RUST_PROCESS_IMPL, method=$START_RUST_PROCESS",
                )
            } else {
                DpisLog.i(
                    "DPIS_FONT HyperOS Rust process hook missing: method=$START_RUST_PROCESS",
                )
            }
            return hooked
        }

        @JvmStatic
        fun resolveProxyLibraryPathForTest(originalBinaryPath: String?): String? =
            resolveProxyLibraryPath(originalBinaryPath)

        @JvmStatic
        fun applyEnvironmentArgsForLegacy(
            source: PerAppDisplayConfigSource?,
            args: List<Any?>,
        ): Array<Any?>? = applyEnvironmentArgs(source, args)

        @JvmStatic
        fun logTargetArgumentProbeForLegacy(args: List<Any?>) {
            logTargetArgumentProbe(args)
        }

        private fun applyEnvironmentArgs(
            source: PerAppDisplayConfigSource?,
            args: List<Any?>,
        ): Array<Any?>? {
            if (source == null) {
                return null
            }
            val parsed = HyperOsRustProcessArgPolicy.parse(args) ?: return null
            val uid = PackageNames.findUid(parsed.packageName, null, args) ?: return null
            val userId = PackageUidResolver.userIdFromUid(uid) ?: return null
            val config = source.get(parsed.packageName, userId) ?: return null
            val fontScalePercent = config.targetFontScalePercent
            if (fontScalePercent == null ||
                !HyperOsRustProcessArgPolicy.shouldRewrite(
                    fontScalePercent,
                    config.hyperOsFlutterFontHookEnabled,
                )
            ) {
                return null
            }
            val updated = HyperOsRustProcessArgPolicy.appendEnvironment(
                parsed.existingEnvironments,
                parsed.packageName,
                fontScalePercent,
                parsed.binaryPath,
            )
            // This property is diagnostic/fallback only. Full /data/app/... Rust binary
            // paths can exceed Android's system property limit; the environment remains
            // available to the native proxy at process start.
            HyperOsFlutterFontBridge.publishRustBinaryPath(parsed.packageName, parsed.binaryPath)
            HyperOsFlutterFontBridge.publishRustProxyTarget(parsed.packageName, config)
            val proxyLibraryPath = resolveProxyLibraryPath(parsed.binaryPath)
            if (proxyLibraryPath.isNullOrEmpty()) {
                DpisLog.i(
                    "DPIS_FONT HyperOS Rust process proxy missing: package=${parsed.packageName}",
                )
                return null
            }
            DpisLog.i(
                "DPIS_FONT HyperOS Rust process env apply: package=${parsed.packageName}, " +
                        "binary=${parsed.binaryPath}, proxy=$proxyLibraryPath, envs=$updated",
            )
            return HyperOsRustProcessArgPolicy.withProxyAndEnvironment(
                args,
                proxyLibraryPath,
                updated,
            )
        }

        private fun logTargetArgumentProbe(args: List<Any?>) {
            val summary = HyperOsRustProcessArgPolicy.buildArgumentProbeSummary(args)
            if (summary != null) {
                DpisLog.i(summary)
            }
        }

        private fun resolveProxyLibraryPath(originalBinaryPath: String?): String? =
            resolveSiblingProxyLibraryPath(originalBinaryPath)

        private fun resolveSiblingProxyLibraryPath(originalBinaryPath: String?): String? {
            if (originalBinaryPath.isNullOrEmpty()) {
                return null
            }
            val parent = File(originalBinaryPath).parentFile ?: return null
            val proxy = File(parent, NATIVE_LIBRARY_NAME)
            return proxy.takeIf { it.isFile && it.length() > 0 }?.absolutePath
        }

        private fun resolveClass(className: String): Class<*>? = try {
            Class.forName(className)
        } catch (_: Throwable) {
            null
        }
    }
}
