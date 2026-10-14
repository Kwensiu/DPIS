package com.dpis.module.runtime.font

import java.util.Collections
import java.util.WeakHashMap

object ResourcesFontScheduler {
    private const val MATCH_TOLERANCE = 0.001f
    private const val MIN_BASE_FONT_SCALE = 0.5f
    private const val MAX_BASE_FONT_SCALE = 2.0f

    private val resourceStates = Collections.synchronizedMap(WeakHashMap<Any, State>())

    // Keyed by packageName|round(targetFactor*1000). Suppression is cleared by
    // event transitions (base<->target observations), not by a time-based TTL.
    private val packageStates = Collections.synchronizedMap(HashMap<String, State>())

    @JvmStatic
    fun observe(
        packageName: String?,
        evidence: ComposeResourcesFontEvidence.Summary?,
        observedFontScale: Float,
        targetFactor: Float,
        nowMs: Long,
    ) {
        // Package-only observations cannot safely suppress Resources reads.
        // Suppression requires a concrete Resources owner observed from a root.
    }

    @JvmStatic
    fun observe(
        packageName: String?,
        scopeKey: String?,
        resourceScope: Any?,
        evidence: ComposeResourcesFontEvidence.Summary?,
        observedFontScale: Float,
        targetFactor: Float,
        nowMs: Long,
    ) {
        if (packageName.isNullOrBlank() || resourceScope == null || evidence == null) return
        if (!evidence.composeHeavyCurrentRoot) {
            synchronized(resourceStates) {
                val current = resourceStates[resourceScope]
                if (!isTargetSuppression(current, packageName, targetFactor)) {
                    resourceStates.remove(resourceScope)
                }
                if (targetFactor > 0f) {
                    val packageKey = packageStateKey(packageName, targetFactor)
                    val packageState = packageStates[packageKey]
                    if (!isTargetSuppression(packageState, packageName, targetFactor)) {
                        packageStates.remove(packageKey)
                    }
                }
            }
            return
        }
        if (!evidence.resourcesHandled || targetFactor <= 0f || observedFontScale <= 0f) return
        val baseFontScale = observedFontScale / targetFactor
        if (!isValidBaseFontScale(baseFontScale)) return
        val state =
            State.suppressedToBase(packageName, baseFontScale, targetFactor, observedFontScale)
        synchronized(resourceStates) {
            val current = resourceStates[resourceScope]
            if (isTargetSuppression(current, packageName, targetFactor)) return
            val packageState = packageStates[packageStateKey(packageName, targetFactor)]
            if (isTargetSuppression(packageState, packageName, targetFactor)) {
                resourceStates[resourceScope] = packageState
                return
            }
            resourceStates[resourceScope] = state
            packageStates[packageStateKey(packageName, targetFactor)] = state
        }
    }

    @JvmStatic
    fun observeResourcesFontScale(
        resourceScope: Any?,
        packageName: String?,
        observedFontScale: Float,
        targetFactor: Float,
    ) {
        if (packageName.isNullOrBlank() || resourceScope == null ||
            observedFontScale <= 0f || targetFactor <= 0f
        ) return
        synchronized(resourceStates) {
            val state = resourceStates[resourceScope]
            if (state == null || state.packageName != packageName ||
                !factorsMatch(state.targetFactor, targetFactor)
            ) {
                resourceStates[resourceScope] =
                    State.observed(packageName, targetFactor, observedFontScale)
                return
            }
            val conflictBase = resolveConflictBaseFontScale(
                state.lastObservedFontScale,
                observedFontScale,
                targetFactor,
            )
            if (conflictBase != null && isValidBaseFontScale(conflictBase)) {
                val suppressed = State.suppressedToTarget(
                    packageName,
                    conflictBase,
                    targetFactor,
                    observedFontScale,
                )
                resourceStates[resourceScope] = suppressed
                packageStates[packageStateKey(packageName, targetFactor)] = suppressed
                return
            }
            resourceStates[resourceScope] = state.withLastObserved(observedFontScale)
        }
    }

    @JvmStatic
    fun maybeSuppressResourcesFont(
        packageName: String?,
        result: FontScaleOverride.Result,
    ): FontScaleOverride.Result = result

    @JvmStatic
    fun maybeSuppressResourcesFont(
        resourceScope: Any?,
        packageName: String?,
        result: FontScaleOverride.Result,
    ): FontScaleOverride.Result = maybeSuppressResourcesFont(
        resourceScope, packageName, result, System.currentTimeMillis(),
    )

    @JvmStatic
    fun maybeSuppressResourcesFont(
        packageName: String?,
        result: FontScaleOverride.Result,
        nowMs: Long,
    ): FontScaleOverride.Result = result

    @JvmStatic
    fun maybeSuppressResourcesFont(
        resourceScope: Any?,
        packageName: String?,
        result: FontScaleOverride.Result,
        nowMs: Long,
    ): FontScaleOverride.Result {
        if (packageName.isNullOrBlank()) return result
        val targetFactor = targetFactor(result)
        val state = activeState(resourceScope, packageName, targetFactor)
            ?: activePackageState(packageName, targetFactor)
            ?: return result
        return FontScaleOverride.Result(
            result.original,
            state.effectiveFontScale,
            result.targetPercent,
            kotlin.math.abs(state.effectiveFontScale - result.original) > FontScaleOverride.EPSILON,
        )
    }

    @JvmStatic
    fun maybeSuppressMetricsFontScale(packageName: String?, currentFontScale: Float): Float =
        if (currentFontScale > 0f) currentFontScale else 1.0f

    @JvmStatic
    fun maybeSuppressMetricsFontScale(
        resourceScope: Any?,
        packageName: String?,
        currentFontScale: Float,
    ): Float = maybeSuppressMetricsFontScale(
        resourceScope, packageName, currentFontScale, System.currentTimeMillis(),
    )

    @JvmStatic
    fun maybeSuppressMetricsFontScale(
        packageName: String?,
        currentFontScale: Float,
        nowMs: Long,
    ): Float = if (currentFontScale > 0f) currentFontScale else 1.0f

    @JvmStatic
    fun maybeSuppressMetricsFontScale(
        resourceScope: Any?,
        packageName: String?,
        currentFontScale: Float,
        nowMs: Long,
    ): Float = maybeSuppressMetricsFontScale(resourceScope, packageName, currentFontScale, 0f)

    @JvmStatic
    fun maybeSuppressMetricsFontScale(
        resourceScope: Any?,
        packageName: String?,
        currentFontScale: Float,
        targetFactor: Float,
    ): Float {
        val original = if (currentFontScale > 0f) currentFontScale else 1.0f
        if (packageName.isNullOrBlank()) return original
        val state = activeState(resourceScope, packageName, targetFactor)
            ?: activePackageState(packageName, targetFactor)
            ?: return original
        return state.effectiveFontScale
    }

    @JvmStatic
    fun clearForTest() {
        resourceStates.clear()
        packageStates.clear()
    }

    @JvmStatic
    fun isPackageSuppressed(packageName: String?, targetFactor: Float): Boolean =
        activePackageState(packageName, targetFactor) != null

    @JvmStatic
    fun isPackageTargetSuppressed(packageName: String?, targetFactor: Float): Boolean {
        val state = activePackageState(packageName, targetFactor)
        return isTargetSuppression(state, packageName, targetFactor)
    }

    private fun activeState(resourceScope: Any?, packageName: String, targetFactor: Float): State? {
        if (resourceScope == null) return null
        val state = resourceStates[resourceScope] ?: return null
        if (state.packageName != packageName || !state.suppressed) return null
        if (targetFactor > 0f && !factorsMatch(state.targetFactor, targetFactor)) return null
        return state
    }

    private fun activePackageState(packageName: String?, targetFactor: Float): State? {
        if (packageName.isNullOrBlank() || targetFactor <= 0f) return null
        val state = packageStates[packageStateKey(packageName, targetFactor)] ?: return null
        if (!state.suppressed || state.packageName != packageName) return null
        if (!factorsMatch(state.targetFactor, targetFactor)) return null
        return state
    }

    private fun resolveConflictBaseFontScale(
        previous: Float,
        current: Float,
        targetFactor: Float
    ): Float? {
        if (previous <= 0f || current <= 0f || targetFactor <= 0f || factorsMatch(
                previous,
                current
            )
        ) {
            return null
        }
        if (factorsMatch(previous * targetFactor, current)) return previous
        if (factorsMatch(current * targetFactor, previous)) return current
        return null
    }

    private fun isValidBaseFontScale(baseFontScale: Float): Boolean =
        baseFontScale >= MIN_BASE_FONT_SCALE && baseFontScale <= MAX_BASE_FONT_SCALE

    private fun targetFactor(result: FontScaleOverride.Result): Float =
        if (result.targetPercent == null || result.targetPercent <= 0) {
            0f
        } else {
            result.targetPercent / 100.0f
        }

    private fun factorsMatch(first: Float, second: Float): Boolean =
        first > 0f && second > 0f && kotlin.math.abs(first - second) <= MATCH_TOLERANCE

    private fun isTargetSuppression(
        state: State?,
        packageName: String?,
        targetFactor: Float
    ): Boolean =
        state != null && state.suppressed && state.packageName == packageName &&
                factorsMatch(state.targetFactor, targetFactor) &&
                !factorsMatch(state.effectiveFontScale, state.baseFontScale)

    private fun packageStateKey(packageName: String, targetFactor: Float): String =
        "$packageName|${kotlin.math.round(targetFactor * 1000f).toInt()}"

    private class State(
        val packageName: String,
        val baseFontScale: Float,
        val effectiveFontScale: Float,
        val targetFactor: Float,
        val lastObservedFontScale: Float,
        val suppressed: Boolean,
    ) {
        fun withLastObserved(observedFontScale: Float) = State(
            packageName,
            baseFontScale,
            effectiveFontScale,
            targetFactor,
            observedFontScale,
            suppressed,
        )

        companion object {
            fun observed(packageName: String, targetFactor: Float, observedFontScale: Float) =
                State(
                    packageName,
                    observedFontScale,
                    observedFontScale,
                    targetFactor,
                    observedFontScale,
                    false,
                )

            fun suppressedToBase(
                packageName: String,
                baseFontScale: Float,
                targetFactor: Float,
                observedFontScale: Float,
            ) = State(
                packageName, baseFontScale, baseFontScale, targetFactor, observedFontScale, true,
            )

            fun suppressedToTarget(
                packageName: String,
                baseFontScale: Float,
                targetFactor: Float,
                observedFontScale: Float,
            ) = State(
                packageName,
                baseFontScale,
                baseFontScale * targetFactor,
                targetFactor,
                observedFontScale,
                true,
            )
        }
    }
}
