package com.dpis.module.runtime.probe

import java.util.concurrent.ConcurrentHashMap

class RuntimeHotPathEvidenceSampler {
    private val states = ConcurrentHashMap<String, State>()

    fun sample(key: String, detail: String?): Sample {
        val now = RuntimeClock.elapsedRealtimeMillis()
        if (!states.containsKey(key) && states.size >= MAX_KEYS) {
            states.clear()
        }
        val state = states.computeIfAbsent(key) { State() }
        synchronized(state) {
            state.hitCount++
            val shouldLog = state.hitCount == 1L ||
                    now - state.lastLoggedAtMillis >= LOG_MIN_INTERVAL_MILLIS ||
                    state.hitCount % LOG_EVERY_HITS == 0L
            if (!shouldLog) {
                state.suppressedSinceLastLog++
                return Sample.skip()
            }
            val suppressedCount = state.suppressedSinceLastLog
            state.suppressedSinceLastLog = 0L
            state.lastLoggedAtMillis = now
            return Sample.emit(
                "$detail, hitCount=${state.hitCount}, suppressedCount=$suppressedCount",
            )
        }
    }

    fun resetForTest() {
        states.clear()
    }

    class Sample private constructor(
        @JvmField val emit: Boolean,
        @JvmField val detail: String,
    ) {
        companion object {
            @JvmStatic
            fun emit(detail: String): Sample = Sample(true, detail)

            @JvmStatic
            fun skip(): Sample = Sample(false, "")
        }
    }

    private class State {
        var hitCount = 0L
        var lastLoggedAtMillis = 0L
        var suppressedSinceLastLog = 0L
    }

    private companion object {
        const val LOG_MIN_INTERVAL_MILLIS = 2_000L
        const val LOG_EVERY_HITS = 50L
        const val MAX_KEYS = 128
    }
}
