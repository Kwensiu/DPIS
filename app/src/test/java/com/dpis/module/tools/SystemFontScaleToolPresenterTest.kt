package com.dpis.module.tools

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SystemFontScaleToolPresenterTest {
    @Test
    fun refreshPublishesGatewayValuesAndKeepsUserPending() {
        val gateway = FakeGateway(percent = 115)
        val listener = RecordingListener()
        val presenter = SystemFontScaleToolPresenter(gateway, listener)

        presenter.refresh()
        assertEquals(115, presenter.state?.currentPercent)
        assertEquals(115, presenter.state?.pendingPercent)
        assertFalse(presenter.state!!.userSelectedPending)

        presenter.selectPendingPercent(130)
        gateway.percent = 90
        presenter.refresh()

        assertEquals(90, presenter.state?.currentPercent)
        assertEquals(130, presenter.state?.pendingPercent)
        assertTrue(presenter.state!!.userSelectedPending)
        assertEquals(3, listener.states.size)
    }

    @Test
    fun applyWritesPendingPercentAndRefresh() {
        val gateway = FakeGateway(percent = 100)
        val presenter = SystemFontScaleToolPresenter(gateway, RecordingListener())
        presenter.refresh()
        presenter.selectPendingPercent(125)

        presenter.apply()

        assertEquals(1, gateway.writes)
        assertEquals(125, gateway.percent)
        assertEquals(125, presenter.state?.currentPercent)
        assertEquals(125, presenter.state?.pendingPercent)
    }

    @Test
    fun applyNotifiesWhenWriteFailsAndSkipsWhenNotApplicable() {
        val gateway = FakeGateway(canWriteValue = false, percent = 100, writeSucceeds = false)
        val listener = RecordingListener()
        val presenter = SystemFontScaleToolPresenter(gateway, listener)
        presenter.refresh()
        presenter.apply()
        assertEquals(0, gateway.writes)
        assertEquals(0, listener.failures)

        gateway.canWriteValue = true
        presenter.refresh()
        presenter.selectPendingPercent(140)
        presenter.apply()
        assertEquals(1, gateway.writes)
        assertEquals(1, listener.failures)
        assertEquals(140, presenter.state?.pendingPercent)
    }

    @Test
    fun restoreDefaultWritesOneHundredWhenRestoreIsAvailable() {
        val gateway = FakeGateway(percent = 115)
        val presenter = SystemFontScaleToolPresenter(gateway, null)
        presenter.refresh()
        presenter.restoreDefault()
        assertEquals(1, gateway.writes)
        assertEquals(100, gateway.percent)
        assertEquals(100, presenter.state?.currentPercent)
    }

    @Test
    fun selectPendingPercentRefreshesWhenNoStateExistsYet() {
        val gateway = FakeGateway(percent = 110)
        val presenter = SystemFontScaleToolPresenter(gateway, RecordingListener())
        presenter.selectPendingPercent(140)
        assertEquals(110, presenter.state?.currentPercent)
        assertEquals(140, presenter.state?.pendingPercent)
        assertTrue(presenter.state!!.userSelectedPending)
    }

    @Test
    fun restoreDoesNothingWhenCurrentAndPendingAreDefault() {
        val gateway = FakeGateway(percent = 100)
        val presenter = SystemFontScaleToolPresenter(gateway, RecordingListener())
        presenter.refresh()
        presenter.restoreDefault()
        assertEquals(0, gateway.writes)
    }

    private class FakeGateway(
        var canWriteValue: Boolean = true,
        var percent: Int? = 100,
        var writeSucceeds: Boolean = true,
    ) : SystemFontScaleToolPresenter.Gateway {
        var writes = 0

        override fun readPercent(): Int? = percent

        override fun canWrite(): Boolean = canWriteValue

        override fun writePercent(percent: Int): Boolean {
            writes++
            if (!writeSucceeds) return false
            this.percent = percent
            return true
        }
    }

    private class RecordingListener : SystemFontScaleToolPresenter.Listener {
        val states = mutableListOf<SystemFontScaleToolState>()
        var failures = 0

        override fun onStateChanged(state: SystemFontScaleToolState) {
            states.add(state)
        }

        override fun onWriteFailed() {
            failures++
        }
    }
}
