package com.dpis.module.fonts

import java.util.ArrayDeque

/**
 * Synchronous arbitration and same-thread transaction context for field-rewrite font hooks.
 * Framework adapters own the actual setter; this module owns only the decision contract.
 */
object FontMutationScheduler {
    enum class Action {
        KEEP_CURRENT,
        APPLY,
        OBSERVE,
        PASS_THROUGH,
    }

    class Decision internal constructor(
        private val actionValue: Action,
        private val targetPxValue: Float,
    ) {
        fun action(): Action = actionValue

        fun targetPx(): Float = targetPxValue
    }

    private data class Frame(val targetPx: Float)

    private val transactionStack: ThreadLocal<ArrayDeque<Frame>> =
        ThreadLocal.withInitial<ArrayDeque<Frame>> { ArrayDeque() }

    @JvmStatic
    fun decide(
        incomingPx: Float,
        currentPx: Float,
        targetPx: Float,
        factor: Float,
        strongerDomainOwns: Boolean,
    ): Decision = decide(
        incomingPx,
        currentPx,
        targetPx,
        factor,
        strongerDomainOwns,
        null,
        false,
    )

    @JvmStatic
    fun decide(
        incomingPx: Float,
        currentPx: Float,
        targetPx: Float,
        factor: Float,
        strongerDomainOwns: Boolean,
        transactionTargetPx: Float?,
        alreadyApplied: Boolean,
    ): Decision {
        if (!isScaleFactorActive(factor)
            || incomingPx <= 0f
            || currentPx <= 0f
            || targetPx <= 0f
        ) {
            return Decision(Action.OBSERVE, 0f)
        }
        if (transactionTargetPx != null
            && FontFieldRewriteMath.approximatelyEqual(incomingPx, transactionTargetPx)
        ) {
            return Decision(Action.PASS_THROUGH, 0f)
        }
        if (strongerDomainOwns) {
            return Decision(Action.OBSERVE, 0f)
        }
        if (alreadyApplied) {
            return if (FontFieldRewriteMath.approximatelyEqual(currentPx, incomingPx)) {
                Decision(Action.KEEP_CURRENT, 0f)
            } else {
                Decision(Action.OBSERVE, 0f)
            }
        }
        if (FontFieldRewriteMath.approximatelyEqual(currentPx, targetPx)) {
            return Decision(Action.KEEP_CURRENT, 0f)
        }
        if (FontFieldRewriteMath.approximatelyEqual(incomingPx, targetPx)) {
            return Decision(Action.OBSERVE, 0f)
        }
        return Decision(Action.APPLY, targetPx)
    }

    @JvmStatic
    fun currentTransactionTarget(): Float? = stack().peekLast()?.targetPx

    @Suppress("UNUSED_PARAMETER")
    fun <T> withMutation(targetPx: Float, factor: Float, block: () -> T): T {
        val stack = stack()
        stack.addLast(Frame(targetPx))
        return try {
            block()
        } finally {
            stack.removeLast()
            if (stack.isEmpty()) {
                transactionStack.remove()
            }
        }
    }

    private fun stack(): ArrayDeque<Frame> {
        val frames = transactionStack.get()
        if (frames != null) {
            return frames
        }
        val created = ArrayDeque<Frame>()
        transactionStack.set(created)
        return created
    }

    @JvmStatic
    fun resetForHotReload() {
        transactionStack.remove()
    }

    private fun isScaleFactorActive(factor: Float): Boolean =
        factor > 0f && factor != 1.0f
}
