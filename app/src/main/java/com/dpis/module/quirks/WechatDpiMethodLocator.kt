package com.dpis.module.quirks

import android.content.pm.ApplicationInfo
import android.content.res.Configuration
import android.os.Build
import android.util.DisplayMetrics
import org.luckypray.dexkit.DexKitBridge
import org.luckypray.dexkit.query.FindMethod
import org.luckypray.dexkit.query.enums.MatchType
import org.luckypray.dexkit.query.matchers.ClassMatcher
import org.luckypray.dexkit.query.matchers.MethodMatcher
import java.io.File
import java.lang.reflect.Method
import java.lang.reflect.Modifier

object WechatDpiMethodLocator {
    @JvmStatic
    fun locate(
        classLoader: ClassLoader?,
        applicationInfo: ApplicationInfo?,
        versionCode: Long
    ): Result =
        locate(classLoader, applicationInfo, versionCode, true)

    @JvmStatic
    fun locate(
        classLoader: ClassLoader?,
        applicationInfo: ApplicationInfo?,
        versionCode: Long,
        allowDexKit: Boolean
    ): Result {
        val routeResult = locateByStaticRoute(classLoader, versionCode)
        if (routeResult.methods.isNotEmpty() || !allowDexKit) return routeResult
        val dexKitResult = locateByDexKit(classLoader, applicationInfo)
        return if (dexKitResult.methods.isNotEmpty() || dexKitResult.failure != null) dexKitResult else routeResult
    }

    private fun locateByDexKit(
        classLoader: ClassLoader?,
        applicationInfo: ApplicationInfo?
    ): Result {
        val sourceDir = applicationInfo?.sourceDir
        if (classLoader == null || sourceDir.isNullOrBlank()) return Result.failed(
            Source.DEXKIT,
            "missing application sourceDir"
        )
        try {
            loadDexKitLibrary()
        } catch (throwable: Throwable) {
            return Result.failed(Source.DEXKIT, "${throwable.javaClass.name}: ${throwable.message}")
        }
        return try {
            DexKitBridge.create(sourceDir).use { bridge ->
                val query = FindMethod.create().matcher(
                    MethodMatcher.create()
                        .declaredClass(
                            ClassMatcher.create().usingEqStrings(
                                "MicroMsg.MMDensityManager",
                                "screenResolution_target_field"
                            )
                        )
                        .modifiers(Modifier.PUBLIC, MatchType.Contains)
                        .returnType(DisplayMetrics::class.java)
                        .paramCount(0)
                        .addInvoke(MethodMatcher.create().returnType("boolean")),
                )
                val methods = ArrayList<Method>()
                bridge.findMethod(query)?.forEach { data ->
                    val method = data.getMethodInstance(classLoader)
                    if (isDisplayMetricsGetter(method)) methods += densityManagerMethods(method.declaringClass)
                }
                Result.resolved(Source.DEXKIT, methods)
            }
        } catch (throwable: Throwable) {
            Result.failed(Source.DEXKIT, "${throwable.javaClass.name}: ${throwable.message}")
        }
    }

    private fun locateByStaticRoute(classLoader: ClassLoader?, versionCode: Long): Result {
        val route = WechatDpiRoutes.forVersionCode(versionCode)
            ?: return Result.failed(Source.STATIC_ROUTE, "unsupported versionCode=$versionCode")
        return try {
            val densityManagerClass = Class.forName(route.className, false, classLoader)
            Result.resolved(Source.STATIC_ROUTE, staticRouteMethods(densityManagerClass, route))
        } catch (throwable: Throwable) {
            Result.failed(
                Source.STATIC_ROUTE,
                "${route.routeKey()}: ${throwable.javaClass.name}: ${throwable.message}"
            )
        }
    }

    private fun isDisplayMetricsGetter(method: Method?): Boolean =
        method != null && method.parameterTypes.isEmpty() && method.returnType == DisplayMetrics::class.java

    @JvmStatic
    fun densityManagerMethods(densityManagerClass: Class<*>?): List<Method> {
        if (densityManagerClass == null) return emptyList()
        return densityManagerClass.declaredMethods.filter { method ->
            isDisplayMetricsGetter(method).also { if (it) method.isAccessible = true }
        }
    }

    private fun staticRouteMethods(
        densityManagerClass: Class<*>?,
        route: WechatDpiRoutes.Route?
    ): List<Method> {
        if (densityManagerClass == null) return emptyList()
        if (route == null || !route.hasDensityMethodTargets()) return densityManagerMethods(
            densityManagerClass
        )
        return buildSet {
            route.densityMethodTargets.forEach { target ->
                if (target.methodName.isBlank()) return@forEach
                densityManagerClass.declaredMethods.filter { method ->
                    method.name == target.methodName && matchesStaticMethodTarget(
                        method,
                        target.kind
                    )
                }.forEach { method -> method.isAccessible = true; add(method) }
            }
        }.toList()
    }

    private fun matchesStaticMethodTarget(
        method: Method,
        kind: WechatDpiRoutes.MethodTarget.Kind
    ): Boolean = when (kind) {
        WechatDpiRoutes.MethodTarget.Kind.DISPLAY_METRICS_GETTER -> isDisplayMetricsGetter(method)
        WechatDpiRoutes.MethodTarget.Kind.DISPLAY_METRICS_MUTATOR -> method.returnType == Void.TYPE && method.parameterTypes.contentEquals(
            arrayOf(Configuration::class.java, DisplayMetrics::class.java)
        )

        WechatDpiRoutes.MethodTarget.Kind.TARGET_FIELD_GETTER -> Modifier.isStatic(method.modifiers) && method.parameterTypes.isEmpty() && method.returnType == Integer.TYPE
        WechatDpiRoutes.MethodTarget.Kind.TARGET_FIELD_SETTER -> Modifier.isStatic(method.modifiers) && method.returnType == Void.TYPE && method.parameterTypes.contentEquals(
            arrayOf(Integer.TYPE)
        )
    }

    private fun loadDexKitLibrary() {
        try {
            System.loadLibrary("dexkit")
            return
        } catch (firstError: UnsatisfiedLinkError) {
            val path = resolveExtractedNativeLibraryPath() ?: throw firstError
            try {
                System.load(path)
            } catch (secondError: UnsatisfiedLinkError) {
                secondError.addSuppressed(firstError)
                throw secondError
            }
        }
    }

    private fun resolveExtractedNativeLibraryPath(): String? {
        val moduleApkPath = resolveModuleApkPath() ?: return null
        val installDir = File(moduleApkPath).parentFile ?: return null
        Build.SUPPORTED_ABIS.forEach { abi ->
            nativeDirectoryNamesForAbi(abi).forEach { nativeDir ->
                val candidate = File(File(File(installDir, "lib"), nativeDir), "libdexkit.so")
                if (candidate.isFile) return candidate.absolutePath
            }
        }
        return null
    }

    private fun resolveModuleApkPath(): String? =
        WechatDpiMethodLocator::class.java.classLoader?.let { parseModuleApkPathForTest(it.toString()) }

    @JvmStatic
    fun parseModuleApkPathForTest(classLoaderText: String?): String? {
        if (classLoaderText.isNullOrEmpty()) return null
        val marker = "module="
        val startIndex = classLoaderText.indexOf(marker)
        if (startIndex < 0) return null
        val start = startIndex + marker.length
        val end = listOf(
            classLoaderText.indexOf(',', start),
            classLoaderText.indexOf(']', start),
            classLoaderText.length
        )
            .filter { it >= 0 }.minOrNull() ?: return null
        return classLoaderText.substring(start, end).trim().takeIf { it.endsWith(".apk") }
    }

    @JvmStatic
    fun nativeDirectoryNamesForAbi(abi: String?): Array<String> = when (abi) {
        "arm64-v8a" -> arrayOf("arm64", "arm64-v8a")
        "armeabi-v7a" -> arrayOf("arm", "armeabi-v7a")
        else -> arrayOf(abi ?: "")
    }

    enum class Source(@JvmField val logName: String) { DEXKIT("dexkit"), STATIC_ROUTE("static-route") }

    class Result private constructor(
        @JvmField val source: Source,
        methods: List<Method>,
        @JvmField val failure: String?,
    ) {
        @JvmField
        val methods: List<Method> = methods.toList()

        companion object {
            @JvmStatic
            fun resolved(source: Source, methods: List<Method>) = Result(source, methods, null)
            @JvmStatic
            fun failed(source: Source, failure: String) = Result(source, emptyList(), failure)
        }
    }
}
