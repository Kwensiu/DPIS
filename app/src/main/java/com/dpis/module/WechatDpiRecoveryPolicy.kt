package com.dpis.module

/** Pure decision boundary for independent WeChat DisplayMetrics recovery. */
object WechatDpiRecoveryPolicy {
    @JvmStatic
    fun decision(targetDpi: Int, observedDpi: Int): String = when {
        targetDpi <= 0 -> "config_missing"
        observedDpi <= 0 -> "metrics_invalid"
        observedDpi == targetDpi -> "confirmed"
        else -> "reapplied"
    }
}
