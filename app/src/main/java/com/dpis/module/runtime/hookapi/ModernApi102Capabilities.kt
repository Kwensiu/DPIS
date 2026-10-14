package com.dpis.module.runtime.hookapi

import com.dpis.module.diagnostics.DpisLog
import io.github.libxposed.api.XposedInterface
import java.lang.reflect.Method

object ModernApi102Capabilities : ModernApiCapabilities {
    private val setIdMethod: Method? = findSetIdMethod()

    override fun apiVersion(): Int = ModernApiCapabilitiesResolver.API_102

    override fun supportsStableHookIds(): Boolean = setIdMethod != null

    override fun supportsHotReloadCallbacks(): Boolean = true

    @Suppress("UNCHECKED_CAST")
    override fun <T : XposedInterface.HookBuilder> applyStableHookId(
        builder: T,
        hookId: String?,
    ): T {
        if (hookId.isNullOrBlank() || setIdMethod == null) {
            return builder
        }
        return try {
            val result = setIdMethod.invoke(builder, hookId)
            if (result is XposedInterface.HookBuilder) result as T else builder
        } catch (throwable: ReflectiveOperationException) {
            DpisLog.e("modern api 102 setId failed: hookId=$hookId", throwable)
            builder
        } catch (throwable: RuntimeException) {
            DpisLog.e("modern api 102 setId failed: hookId=$hookId", throwable)
            builder
        }
    }

    private fun findSetIdMethod(): Method? = try {
        XposedInterface.HookBuilder::class.java.getMethod("setId", String::class.java)
    } catch (_: NoSuchMethodException) {
        null
    }
}
