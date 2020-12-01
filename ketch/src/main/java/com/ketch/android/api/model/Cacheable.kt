package com.ketch.android.api.model

interface Cacheable {
    // timestamp in UNIX format that represents when the object was cached
    val cachedAt: Long?

    /**
     * Creates a copy of the object with timestamp provided
     */
    fun cacheableCopy(timestamp: Long): Cacheable

    /**
     * Flag that can be used to determine if the object was retrieved from cache
     */
    fun isCached(): Boolean = cachedAt != null
}
