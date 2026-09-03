package com.ketch.android

import android.content.SharedPreferences

/**
 * In-memory SharedPreferences so the singleton can be exercised without an Android runtime.
 * Shared by every test that binds KetchSharedPreferences.
 */
internal class MemorySharedPreferences : SharedPreferences {
    private val store = linkedMapOf<String, String?>()

    override fun getAll(): Map<String, *> = store.toMap()

    override fun getString(key: String, defValue: String?): String? = store[key] ?: defValue

    override fun getStringSet(key: String, defValues: MutableSet<String>?): MutableSet<String>? = null

    override fun getInt(key: String, defValue: Int): Int = defValue

    override fun getLong(key: String, defValue: Long): Long = defValue

    override fun getFloat(key: String, defValue: Float): Float = defValue

    override fun getBoolean(key: String, defValue: Boolean): Boolean = defValue

    override fun contains(key: String): Boolean = store.containsKey(key)

    override fun edit(): SharedPreferences.Editor = EditorImpl()

    override fun registerOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener?,
    ) = Unit

    override fun unregisterOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener?,
    ) = Unit

    private inner class EditorImpl : SharedPreferences.Editor {
        private val pending = linkedMapOf<String, String?>()

        override fun putString(key: String?, value: String?): SharedPreferences.Editor = apply {
            if (key != null) pending[key] = value
        }

        override fun putStringSet(
            key: String?,
            values: MutableSet<String>?,
        ): SharedPreferences.Editor = this

        override fun putInt(key: String?, value: Int): SharedPreferences.Editor = this

        override fun putLong(key: String?, value: Long): SharedPreferences.Editor = this

        override fun putFloat(key: String?, value: Float): SharedPreferences.Editor = this

        override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor = this

        override fun remove(key: String?): SharedPreferences.Editor = apply {
            if (key != null) pending[key] = null
        }

        override fun clear(): SharedPreferences.Editor = apply { pending.clear() }

        override fun commit(): Boolean {
            applyPending()
            return true
        }

        override fun apply() {
            applyPending()
        }

        private fun applyPending() {
            pending.forEach { (key, value) ->
                if (value == null) {
                    store.remove(key)
                } else {
                    store[key] = value
                }
            }
            pending.clear()
        }
    }
}
