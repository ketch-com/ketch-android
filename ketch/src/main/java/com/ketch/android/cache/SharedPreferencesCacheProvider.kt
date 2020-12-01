package com.ketch.android.cache

import android.content.Context
import android.content.SharedPreferences

/**
 * Implementation of {@link CacheProvider} interface based on Android {@link SharedPreferences} mechanism
 * @param context — Context with which SharedPreferences will be created
 */
class SharedPreferencesCacheProvider(context: Context) : CacheProvider {
    private val sharedPreferences: SharedPreferences

    init {
        sharedPreferences = context.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
    }

    override fun obtain(key: String): String? = sharedPreferences.getString(key, null)

    override fun store(key: String, value: String) {
        sharedPreferences.edit().putString(key, value).commit()
    }

    companion object {
        private const val SHARED_PREFERENCES_NAME = "cache"
    }
}
