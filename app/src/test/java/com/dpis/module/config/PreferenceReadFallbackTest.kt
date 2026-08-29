package com.dpis.module.config

import android.content.SharedPreferences
import com.dpis.module.DpisConfigStore
import com.dpis.module.FakePrefs
import org.junit.Assert.assertNull
import org.junit.Test

class PreferenceReadFallbackTest {
    @Test
    fun fallsBackToDefaultsWhenIntReadFails() {
        val viewportKey = "viewport.bin.mt.plus.canary.width_dp"
        val fontKey = "font.bin.mt.plus.canary.scale_percent"
        val prefs = ThrowingIntReadPrefs(setOf(viewportKey, fontKey))
        prefs.edit()
            .putString(viewportKey, "not_an_int")
            .putString(fontKey, "not_an_int")
            .commit()

        val store = DpisConfigStore(prefs)

        assertNull(store.getTargetViewportWidthDp("bin.mt.plus.canary"))
        assertNull(store.getTargetFontScalePercent("bin.mt.plus.canary"))
    }

    private class ThrowingIntReadPrefs(
        private val intReadFailureKeys: Set<String>,
        private val delegate: FakePrefs = FakePrefs(),
    ) : SharedPreferences {
        override fun getAll(): Map<String, *> = delegate.all

        override fun getString(key: String, defValue: String?): String? =
            delegate.getString(key, defValue)

        override fun getStringSet(key: String, defValues: Set<String>?): Set<String>? =
            delegate.getStringSet(key, defValues)

        override fun getInt(key: String, defValue: Int): Int {
            if (key in intReadFailureKeys) {
                throw ClassCastException("forced int read failure for test")
            }
            return delegate.getInt(key, defValue)
        }

        override fun getLong(key: String, defValue: Long): Long = delegate.getLong(key, defValue)

        override fun getFloat(key: String, defValue: Float): Float = delegate.getFloat(key, defValue)

        override fun getBoolean(key: String, defValue: Boolean): Boolean =
            delegate.getBoolean(key, defValue)

        override fun contains(key: String): Boolean = delegate.contains(key)

        override fun edit(): SharedPreferences.Editor = delegate.edit()

        override fun registerOnSharedPreferenceChangeListener(
            listener: SharedPreferences.OnSharedPreferenceChangeListener,
        ) = delegate.registerOnSharedPreferenceChangeListener(listener)

        override fun unregisterOnSharedPreferenceChangeListener(
            listener: SharedPreferences.OnSharedPreferenceChangeListener,
        ) = delegate.unregisterOnSharedPreferenceChangeListener(listener)
    }
}
