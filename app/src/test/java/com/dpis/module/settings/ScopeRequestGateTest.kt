package com.dpis.module.settings

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ScopeRequestGateTest {
    @Test
    fun rejectsNewRequestUntilActiveRequestFinishes() {
        val gate = ScopeRequestGate()
        val first = gate.tryStart("manual", listOf("one"))

        assertNotNull(first)
        assertNull(gate.tryStart("save", listOf("two")))

        first!!.finish("approved")
        assertNotNull(gate.tryStart("save", listOf("two")))
    }

    @Test
    fun staleCallbackCannotFinishLaterRequest() {
        val gate = ScopeRequestGate()
        val first = gate.tryStart("manual", listOf("one"))!!
        first.finish("failed")
        val second = gate.tryStart("save", listOf("two"))!!

        first.finish("late")

        assertNull(gate.tryStart("template", listOf("three")))
        second.finish("approved")
        assertNotNull(gate.tryStart("template", listOf("three")))
    }
}
