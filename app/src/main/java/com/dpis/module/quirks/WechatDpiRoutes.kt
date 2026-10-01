package com.dpis.module.quirks

object WechatDpiRoutes {
    private val routes = arrayOf(
        route(
            3180L,
            "8.0.78",
            "le5.f",
            MethodTarget.displayMetricsGetter("d"),
            MethodTarget.displayMetricsGetter("e")
        ),
        // Historical 8.0.74 route shape kept for reference only.
        routeWithBottomTabIconScale(
            3120L,
            "8.0.74",
            "j65.f",
            MethodTarget.displayMetricsGetter("d"),
            MethodTarget.displayMetricsGetter("e")
        ),
        route(3100L, "8.0.72", "w45.f"),
        route(3080L, "8.0.71", "q35.f"),
        route(3060L, "8.0.70", "d25.f"),
        route(3040L, "8.0.69", "az4.f"),
        route(2460L, "8.0.42", "hy3.d"),
    )

    @JvmStatic
    fun forVersionCode(versionCode: Long): Route? =
        routes.firstOrNull { it.versionCode == versionCode }

    @JvmStatic
    fun supportsVersionCode(versionCode: Long): Boolean = forVersionCode(versionCode) != null

    @JvmStatic
    fun all(): List<Route> = routes.toList()

    @JvmStatic
    fun matchesClassName(className: String?): Boolean =
        !className.isNullOrBlank() && routes.any { it.className == className }

    private fun route(
        versionCode: Long,
        versionName: String,
        className: String,
        vararg targets: MethodTarget
    ) =
        Route(versionCode, versionName, className, false, targets)

    private fun routeWithBottomTabIconScale(
        versionCode: Long,
        versionName: String,
        className: String,
        vararg targets: MethodTarget
    ) =
        Route(versionCode, versionName, className, true, targets)

    class Route internal constructor(
        @JvmField val versionCode: Long,
        @JvmField val versionName: String,
        @JvmField val className: String,
        @JvmField val bottomTabIconScaleEnabled: Boolean,
        targets: Array<out MethodTarget>,
    ) {
        @JvmField
        val densityMethodTargets: Array<MethodTarget> = targets.toList().toTypedArray()
        fun routeKey(): String = className
        fun hasDensityMethodTargets(): Boolean = densityMethodTargets.isNotEmpty()
    }

    class MethodTarget private constructor(
        @JvmField val methodName: String,
        @JvmField val kind: Kind,
    ) {
        enum class Kind { DISPLAY_METRICS_GETTER, DISPLAY_METRICS_MUTATOR, TARGET_FIELD_GETTER, TARGET_FIELD_SETTER }

        companion object {
            @JvmStatic
            fun displayMetricsGetter(methodName: String) =
                MethodTarget(methodName, Kind.DISPLAY_METRICS_GETTER)

            @JvmStatic
            fun displayMetricsMutator(methodName: String) =
                MethodTarget(methodName, Kind.DISPLAY_METRICS_MUTATOR)

            @JvmStatic
            fun targetFieldGetter(methodName: String) =
                MethodTarget(methodName, Kind.TARGET_FIELD_GETTER)

            @JvmStatic
            fun targetFieldSetter(methodName: String) =
                MethodTarget(methodName, Kind.TARGET_FIELD_SETTER)
        }
    }
}
