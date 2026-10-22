package com.dpis.module

import android.content.SharedPreferences
import java.util.ArrayDeque

class FakePrefs : SharedPreferences {
    private val values = HashMap<String, Any?>()
    private var commitResult = true
    private val commitResults = ArrayDeque<Boolean>()

    fun setCommitResult(commitResult: Boolean) {
        this.commitResult = commitResult
        commitResults.clear()
    }

    fun setCommitResults(vararg results: Boolean) {
        commitResults.clear()
        results.forEach(commitResults::addLast)
    }

    override fun getAll(): MutableMap<String, *> = HashMap(values)

    override fun getString(key: String?, defValue: String?): String? =
        values[key] as? String ?: defValue

    @Suppress("UNCHECKED_CAST")
    override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? =
        (values[key] as? Set<String>)?.toMutableSet() ?: defValues

    override fun getInt(key: String?, defValue: Int): Int = values[key] as? Int ?: defValue

    override fun getLong(key: String?, defValue: Long): Long = values[key] as? Long ?: defValue

    override fun getFloat(key: String?, defValue: Float): Float = values[key] as? Float ?: defValue

    override fun getBoolean(key: String?, defValue: Boolean): Boolean =
        values[key] as? Boolean ?: defValue

    override fun contains(key: String?): Boolean = values.containsKey(key)

    override fun edit(): SharedPreferences.Editor = EditorImpl()

    override fun registerOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener?,
    ) = Unit

    override fun unregisterOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener?,
    ) = Unit

    private inner class EditorImpl : SharedPreferences.Editor {
        private val updates = HashMap<String, Any?>()
        private val removals = HashSet<String>()
        private var clear = false

        override fun putString(key: String?, value: String?): SharedPreferences.Editor =
            update(key, value)

        override fun putStringSet(
            key: String?,
            values: MutableSet<String>?,
        ): SharedPreferences.Editor = update(key, values?.toMutableSet())

        override fun putInt(key: String?, value: Int): SharedPreferences.Editor = update(key, value)

        override fun putLong(key: String?, value: Long): SharedPreferences.Editor =
            update(key, value)

        override fun putFloat(key: String?, value: Float): SharedPreferences.Editor =
            update(key, value)

        override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor =
            update(key, value)

        override fun remove(key: String?): SharedPreferences.Editor = apply {
            if (key != null) {
                removals.add(key)
                updates.remove(key)
            }
        }

        override fun clear(): SharedPreferences.Editor = apply {
            clear = true
            updates.clear()
            removals.clear()
        }

        override fun commit(): Boolean {
            val result = if (commitResults.isEmpty()) commitResult else commitResults.removeFirst()
            if (result) applyChanges()
            return result
        }

        override fun apply() = applyChanges()

        private fun update(key: String?, value: Any?): SharedPreferences.Editor = apply {
            if (key != null) {
                updates[key] = value
                removals.remove(key)
            }
        }

        private fun applyChanges() {
            if (clear) values.clear()
            removals.forEach(values::remove)
            updates.forEach { (key, value) ->
                if (value == null) {
                    values.remove(key)
                } else {
                    values[key] = if (value is Set<*>) value.toMutableSet() else value
                }
            }
        }
    }
}
