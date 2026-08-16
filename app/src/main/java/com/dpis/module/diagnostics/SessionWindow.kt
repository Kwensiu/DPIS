package com.dpis.module.diagnostics

/** Log-selection window around one feedback diagnostic session. */
class SessionWindow private constructor(
    private val startMillisValue: Long,
    private val endMillisValue: Long,
) {
    fun startMillis(): Long = startMillisValue

    fun endMillis(): Long = endMillisValue

    fun contains(timestampMillis: Long): Boolean =
        timestampMillis >= startMillisValue && timestampMillis <= endMillisValue

    companion object {
        @JvmField
        val START_LOG_MARGIN_MS: Long = 5_000L

        @JvmField
        val END_LOG_MARGIN_MS: Long = 15_000L

        @JvmStatic
        fun around(startedAtMillis: Long, finishedAtMillis: Long): SessionWindow {
            val start = maxOf(0L, startedAtMillis - START_LOG_MARGIN_MS)
            val end = maxOf(start, finishedAtMillis + END_LOG_MARGIN_MS)
            return SessionWindow(start, end)
        }
    }
}
