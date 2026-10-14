package com.dpis.module

import com.dpis.module.config.PerAppDisplayConfigSource
import com.dpis.module.diagnostics.DpisLog
import com.dpis.module.runtime.hyperos.HyperOsRustProcessHookInstaller
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import java.lang.reflect.Modifier

internal object LegacyRustProcessHookInstaller {
    private const val RUST_PROCESS_IMPL = "android.os.RustProcessImpl"
    private const val START_RUST_PROCESS = "startRustProcess"

    @JvmStatic
    fun install(source: PerAppDisplayConfigSource?): Boolean {
        val clazz = resolveClass(RUST_PROCESS_IMPL)
        if (clazz == null) {
            DpisLog.i(
                "DPIS_FONT HyperOS Rust process legacy hook missing: class=$RUST_PROCESS_IMPL",
            )
            return false
        }
        var hooked = false
        for (method in clazz.declaredMethods) {
            if (START_RUST_PROCESS != method.name || Modifier.isAbstract(method.modifiers)) {
                continue
            }
            XposedBridge.hookMethod(method, object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    try {
                        val args = param.args.toList()
                        HyperOsRustProcessHookInstaller.logTargetArgumentProbeForLegacy(args)
                        val updatedArgs =
                            HyperOsRustProcessHookInstaller.applyEnvironmentArgsForLegacy(
                                source,
                                args
                            )
                        if (updatedArgs != null) {
                            param.args = updatedArgs
                        }
                    } catch (throwable: Throwable) {
                        DpisLog.e(
                            "DPIS_FONT HyperOS Rust process legacy env hook failed",
                            throwable,
                        )
                    }
                }
            })
            hooked = true
        }
        if (hooked) {
            DpisLog.i(
                "DPIS_FONT HyperOS Rust process legacy hook ready: " +
                        "class=$RUST_PROCESS_IMPL, method=$START_RUST_PROCESS",
            )
        } else {
            DpisLog.i(
                "DPIS_FONT HyperOS Rust process legacy hook missing: method=$START_RUST_PROCESS",
            )
        }
        return hooked
    }

    private fun resolveClass(className: String): Class<*>? = try {
        Class.forName(className)
    } catch (_: Throwable) {
        null
    }
}
