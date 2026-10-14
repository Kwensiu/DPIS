package com.dpis.module.runtime.systemserver.selection

import com.dpis.module.runtime.systemserver.reflection.Reflection
import com.dpis.module.runtime.webapk.WebApkCarrierResolver

/** Package names found on system_server hook arguments. */
object PackageNames {
    private const val MAX_RECURSION_DEPTH = 5
    private val STRING_METHOD_NAMES = arrayOf(
        "getOwningPackage",
        "getPackageName",
        "getPackage",
        "getOpPackageName",
    )
    private val OBJECT_METHOD_NAMES = arrayOf(
        "getIntent",
        "getComponent",
        "getActivityInfo",
        "getApplicationInfo",
        "getRequest",
        "getTargetActivity",
        "getOrigActivity",
        "getRealActivity",
    )
    private val STRING_FIELD_NAMES = arrayOf(
        "packageName", "mPackageName", "package", "launchedFromPackage",
    )
    private val OBJECT_FIELD_NAMES = arrayOf(
        "intent",
        "mIntent",
        "component",
        "mComponent",
        "activityInfo",
        "applicationInfo",
        "request",
        "mRequest",
        "targetActivity",
        "origActivity",
        "realActivity",
    )

    @JvmStatic
    fun find(self: Any?, args: List<*>): String? {
        findRecursive(self, 0)?.let { return it }
        for (arg in args) {
            findRecursive(arg, 0)?.let { return it }
        }
        return null
    }

    @JvmStatic
    fun findPackageNameForTest(self: Any?, vararg args: Any?): String? {
        val values = ArrayList<Any?>()
        values.addAll(args)
        return find(self, values)
    }

    @JvmStatic
    fun collectWebApkOwners(value: String?, output: MutableSet<String>?, maxCount: Int) {
        if (output == null || output.size >= maxCount) {
            return
        }
        for (owner in WebApkCarrierResolver.collectOwnerPackagesFromText(
            value, maxCount - output.size,
        )) {
            if (owner != null) {
                output.add(owner)
            }
            if (output.size >= maxCount) {
                return
            }
        }
    }

    @JvmStatic
    fun collectFromText(value: String?, output: MutableSet<String>?, maxCount: Int) {
        if (value.isNullOrEmpty() || output == null || maxCount <= 0) {
            return
        }
        var index = value.indexOf("com.")
        while (index in 0 until value.length) {
            var end = index
            while (end < value.length) {
                val c = value[end]
                if (c.isLetterOrDigit() || c == '_' || c == '.') {
                    end++
                    continue
                }
                break
            }
            if (end > index) {
                val candidate = value.substring(index, end)
                if (isLikely(candidate)) {
                    output.add(candidate)
                    if (output.size >= maxCount) {
                        return
                    }
                }
            }
            index = value.indexOf("com.", index + 4)
        }
    }

    @JvmStatic
    fun isLikely(value: String?): Boolean {
        if (value == null) {
            return false
        }
        val trimmed = value.trim()
        if (trimmed.isEmpty() ||
            trimmed.length > 256 ||
            !trimmed.contains(".") ||
            trimmed.contains(" ") ||
            trimmed.contains("/") ||
            trimmed.contains("{") ||
            trimmed.contains("}")
        ) {
            return false
        }
        return trimmed[0].isLowerCase()
    }

    private fun findRecursive(target: Any?, depth: Int): String? {
        if (target == null || depth > MAX_RECURSION_DEPTH) {
            return null
        }
        if (target is String && isLikely(target)) {
            return target
        }
        for (methodName in STRING_METHOD_NAMES) {
            val fromMethod = Reflection.invokeStringMethod(
                target, methodName,
            )
            if (fromMethod != null) {
                return fromMethod
            }
        }
        for (methodName in OBJECT_METHOD_NAMES) {
            val nestedPackage = findRecursive(
                Reflection.invokeObjectMethod(target, methodName),
                depth + 1,
            )
            if (nestedPackage != null) {
                return nestedPackage
            }
        }
        for (fieldName in STRING_FIELD_NAMES) {
            val value = Reflection.readField(target, fieldName)
            if (value is String && isLikely(value)) {
                return value
            }
        }
        for (fieldName in OBJECT_FIELD_NAMES) {
            val nestedPackage = findRecursive(
                Reflection.readField(target, fieldName),
                depth + 1,
            )
            if (nestedPackage != null) {
                return nestedPackage
            }
        }
        for (field in Reflection.getAllFields(target.javaClass)) {
            if (field.type.isPrimitive || field.type.isEnum) {
                continue
            }
            try {
                field.isAccessible = true
                val nested = field.get(target)
                if (nested == null || nested === target || field.name == "this\$0") {
                    continue
                }
                val value = findRecursive(nested, depth + 1)
                if (value != null) {
                    return value
                }
            } catch (_: Exception) {
                // Continue probing inaccessible or platform-owned fields.
            }
        }
        return extractFromText(target.toString())
    }

    private fun extractFromText(value: String?): String? {
        val webApkOwner = WebApkCarrierResolver.ownerPackageFromText(value)
        if (webApkOwner != null) {
            return webApkOwner
        }
        val candidates = LinkedHashSet<String>()
        collectFromText(value, candidates, 1)
        return candidates.firstOrNull()
    }
}
