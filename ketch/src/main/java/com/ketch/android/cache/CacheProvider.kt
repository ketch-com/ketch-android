package com.ketch.android.cache

import java.security.MessageDigest

/**
 * Interface for components that provide storing/retrieving data from the cache
 */
interface CacheProvider {

    fun obtain(key: String): String?
    fun store(key: String, value: String)

    /**
     * Generages a basic SHA-256 hash for the provided string
     */
    private fun hash(text: String): String {
        val bytes = text.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.fold("", { str, it -> str + "%02x".format(it) })
    }

    /**
     * Returns a unique string for a set of input parameters
     * Should be used for generating unique key to cache data for a certain request
     */
    fun generateSalt(vararg parameters: Any?): String =
        hash(parameters.joinToString { it.toString() })
}
