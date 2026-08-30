package com.dpis.module.ui.compose

import java.util.function.BooleanSupplier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StartupDisclaimerGateTest {
    @Test
    fun retainsOnePendingRequestAcrossHostRecreationAndClearsAfterAcceptance() {
        var accepted = 0
        val firstHost = RecordingPresenter()
        val recreatedHost = RecordingPresenter()

        assertTrue(
            StartupDisclaimerGate.show(
                markAccepted = BooleanSupplier { true },
                onSaveFailed = {},
                onAccepted = { accepted++ },
                onBack = {},
            )
        )

        StartupDisclaimerGate.bind(firstHost)
        assertEquals(1, firstHost.showCount)

        // A second startup trigger reuses the active request instead of stacking a dialog.
        StartupDisclaimerGate.show(
            markAccepted = BooleanSupplier { true },
            onSaveFailed = {},
            onAccepted = { accepted++ },
            onBack = {},
        )
        assertEquals(1, firstHost.showCount)

        // Binding a replacement host models configuration recreation.
        StartupDisclaimerGate.bind(recreatedHost)
        assertEquals(1, recreatedHost.showCount)
        recreatedHost.accept()
        assertEquals(1, accepted)

        // Once accepted, a later request is a new dialog rather than a stale re-show.
        StartupDisclaimerGate.show(
            markAccepted = BooleanSupplier { true },
            onSaveFailed = {},
            onAccepted = {},
            onBack = {},
        )
        assertEquals(2, recreatedHost.showCount)
        recreatedHost.accept()
        StartupDisclaimerGate.clear(recreatedHost)
    }

    private class RecordingPresenter : StartupDisclaimerGate.Presenter {
        var showCount = 0
        private var onAccepted: (() -> Unit)? = null

        override fun show(
            markAccepted: BooleanSupplier,
            onSaveFailed: () -> Unit,
            onAccepted: () -> Unit,
            onBack: () -> Unit,
        ): Boolean {
            showCount++
            this.onAccepted = onAccepted
            return true
        }

        fun accept() {
            onAccepted?.invoke()
        }
    }
}
