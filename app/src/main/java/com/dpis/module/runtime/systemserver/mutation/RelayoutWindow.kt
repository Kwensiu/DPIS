package com.dpis.module.runtime.systemserver.mutation

import android.content.pm.ActivityInfo
import android.os.IBinder
import com.dpis.module.runtime.systemserver.diagnostics.HotPathInspector
import com.dpis.module.runtime.systemserver.hook.EntryRoute
import com.dpis.module.runtime.systemserver.reflection.Reflection
import com.dpis.module.runtime.systemserver.selection.PackageNames

/**
 * The window a relayout dispatch is about, and whether that window may take a
 * viewport write. A system window is left alone unless the target package is a
 * system owner and no other system owner is on the same window.
 */
object RelayoutWindow {
    private val SYSTEM_WINDOW_OWNER_PACKAGES = setOf(
        "android",
        "system",
        "com.android.systemui",
    )

    @JvmStatic
    fun target(windowManagerService: Any?, args: List<*>?): Any? {
        val client = firstWindowClient(args)
        val mappedWindow = findWindowStateByClient(windowManagerService, client)
        return mappedWindow ?: windowManagerService
    }

    @JvmStatic
    fun shouldInspect(
        entryName: String?,
        self: Any?,
        args: List<*>?,
        resolvedTargetObject: Boolean,
        configuredPackages: Set<String>?,
    ): Boolean {
        if (resolvedTargetObject && EntryRoute.isRelayoutDispatch(entryName)) {
            // relayoutWindow often resolves a concrete WindowState from WMS.mWindowMap.
            // Keep the relayout hotpath cheap by requiring a light package hit
            // on that resolved target before entering the full resolver.
            return hasConfiguredPackageHint(self, entryName, configuredPackages)
        }
        return HotPathInspector.shouldInspectHotEntry(
            entryName, self, args, configuredPackages,
        )
    }

    @JvmStatic
    fun canApply(entryName: String?, target: Any?, packageName: String?): Boolean {
        val identity = identity(entryName, target) ?: return false
        if (packageName.isNullOrBlank()) {
            return false
        }
        if (!identity.ownerPackages.contains(packageName)) {
            return false
        }
        if (isSystemWindowType(identity.windowType) && !isSystemWindowOwnerPackage(packageName)) {
            return false
        }
        return !containsDifferentSystemWindowOwner(identity.ownerPackages, packageName)
    }

    @JvmStatic
    fun shouldInspectHotEntryForTest(
        entryName: String?,
        self: Any?,
        configuredPackages: Set<String>?,
    ): Boolean = shouldInspect(entryName, self, emptyList<Any?>(), false, configuredPackages)

    @JvmStatic
    fun shouldInspectResolvedRelayoutTargetForTest(
        self: Any?,
        configuredPackages: Set<String>?,
    ): Boolean = shouldInspect(
        "relayout-dispatch",
        self,
        emptyList<Any?>(),
        true,
        configuredPackages,
    )

    @JvmStatic
    fun canApplyRelayoutMutationForTest(target: Any?, packageName: String?): Boolean =
        canApply("relayout-dispatch", target, packageName)

    private fun hasConfiguredPackageHint(
        target: Any?,
        entryName: String?,
        configuredPackages: Set<String>?,
    ): Boolean {
        if (target == null || configuredPackages.isNullOrEmpty()) {
            return false
        }
        val identity = identity(entryName, target) ?: collect(target)
        for (candidate in identity.ownerPackages) {
            if (configuredPackages.contains(candidate)) {
                return true
            }
        }
        return HotPathInspector.shouldInspectHotEntry(
            "relayout-dispatch", target, emptyList<Any>(), configuredPackages,
        )
    }

    private fun identity(entryName: String?, target: Any?): Identity? {
        if (!EntryRoute.isRelayoutDispatch(entryName)) {
            return null
        }
        return collect(target)
    }

    private fun collect(target: Any?): Identity {
        if (target == null) {
            return Identity(emptySet(), null)
        }
        val attrs = Reflection.readField(target, "mAttrs")
        val ownerPackages = LinkedHashSet<String>()
        addCandidate(ownerPackages, directPackageName(target))
        addCandidate(ownerPackages, directPackageName(attrs))
        addCandidate(
            ownerPackages,
            directPackageName(Reflection.readField(target, "mActivityRecord")),
        )
        addCandidate(
            ownerPackages,
            directPackageName(Reflection.readField(target, "activityRecord")),
        )
        addCandidate(
            ownerPackages,
            directPackageName(Reflection.readField(target, "mToken")),
        )
        return Identity(ownerPackages, readInt(attrs, "type"))
    }

    private fun isSystemWindowType(windowType: Int?): Boolean {
        // WindowManager.LayoutParams.FIRST_SYSTEM_WINDOW = 2000.
        return windowType != null && windowType >= 2000
    }

    private fun containsDifferentSystemWindowOwner(
        packageNames: Set<String>?,
        targetPackageName: String,
    ): Boolean {
        if (packageNames.isNullOrEmpty()) {
            return false
        }
        for (packageName in packageNames) {
            if (isSystemWindowOwnerPackage(packageName) && packageName != targetPackageName) {
                return true
            }
        }
        return false
    }

    private fun isSystemWindowOwnerPackage(packageName: String?): Boolean {
        if (packageName == null) {
            return false
        }
        val normalized = packageName.trim()
        return SYSTEM_WINDOW_OWNER_PACKAGES.contains(normalized) || normalized.endsWith(".systemui")
    }

    private fun addCandidate(candidates: MutableSet<String>, packageName: String?) {
        if (packageName.isNullOrBlank()) {
            return
        }
        candidates.add(packageName)
    }

    private fun directPackageName(target: Any?): String? {
        if (target == null) {
            return null
        }
        if (target is ActivityInfo && PackageNames.isLikely(target.packageName)) {
            return target.packageName
        }
        val packageName = Reflection.readField(target, "packageName")
        if (packageName is String && PackageNames.isLikely(packageName)) {
            return packageName
        }
        val packageField = Reflection.readField(target, "mPackageName")
        if (packageField is String && PackageNames.isLikely(packageField)) {
            return packageField
        }
        val component = Reflection.readField(target, "mActivityComponent")
        val componentPackage = Reflection.invokeStringMethod(
            component, "getPackageName",
        )
        if (componentPackage != null) {
            return componentPackage
        }
        val info = Reflection.readField(target, "info")
        if (info is ActivityInfo && PackageNames.isLikely(info.packageName)) {
            return info.packageName
        }
        val activityInfo = Reflection.readField(target, "mInfo")
        if (activityInfo is ActivityInfo &&
            PackageNames.isLikely(activityInfo.packageName)
        ) {
            return activityInfo.packageName
        }
        return null
    }

    private fun firstWindowClient(args: List<*>?): Any? {
        if (args == null) {
            return null
        }
        if (args.size > 1 && invokeBinder(args[1]) != null) {
            return args[1]
        }
        for (arg in args) {
            if (isWindowClientObject(arg)) {
                return arg
            }
        }
        for (arg in args) {
            if (arg != null && invokeBinder(arg) != null) {
                return arg
            }
        }
        return null
    }

    private fun isWindowClientObject(value: Any?): Boolean {
        if (value == null) {
            return false
        }
        val type = value.javaClass
        if (isWindowClientTypeName(type.name)) {
            return true
        }
        for (interfaceClass in type.interfaces) {
            if (isWindowClientTypeName(interfaceClass.name)) {
                return true
            }
        }
        return false
    }

    private fun isWindowClientTypeName(name: String?): Boolean =
        name != null && name.contains("android.view.IWindow") && !name.contains("IWindowSession")

    private fun findWindowStateByClient(windowManagerService: Any?, client: Any?): Any? {
        if (windowManagerService == null || client == null) {
            return null
        }
        val windowMap = Reflection.readField(
            windowManagerService, "mWindowMap",
        )
        if (windowMap !is Map<*, *>) {
            return null
        }
        windowMap[client]?.let { return it }
        val binder = invokeBinder(client)
        if (binder != null) {
            windowMap[binder]?.let { return it }
        }
        for ((key, value) in windowMap) {
            val clientField = Reflection.readField(value, "mClient")
            if (client == key || binder == key || client == clientField || binder == invokeBinder(
                    clientField
                )
            ) {
                return value
            }
        }
        return null
    }

    private fun invokeBinder(value: Any?): IBinder? {
        if (value == null) {
            return null
        }
        return try {
            val method = Reflection.findNoArgMethod(
                value.javaClass, "asBinder",
            ) ?: return null
            method.isAccessible = true
            method.invoke(value) as? IBinder
        } catch (_: Exception) {
            null
        }
    }

    private fun readInt(target: Any?, fieldName: String): Int? =
        Reflection.readField(target, fieldName) as? Int

    private class Identity(
        val ownerPackages: Set<String>,
        val windowType: Int?,
    )
}
