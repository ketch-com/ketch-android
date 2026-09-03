package com.ketch.android

import android.util.Log
import androidx.annotation.VisibleForTesting
import com.ketch.android.api.HeadlessApiClient
import com.ketch.android.data.IdentityConfiguration
import kotlinx.coroutines.CompletableDeferred
import java.util.UUID

/**
 * The Ketch-managed identifier (`swb_*`), minted and owned by the SDK rather than by the tag.
 *
 * Process-wide rather than per-[Ketch] because KetchSdk's static headless methods run with no
 * Ketch instance, and a consent record written without the identifier would create a second
 * consent subject for the same device.
 *
 * Inert unless config declares the space as `queryString`. Live config still says
 * `managedCookie`, in which case nothing is minted and nothing is supplied.
 */
internal object ManagedIdentity {
    const val STORAGE_KEY = "ketch_managed_identity"
    const val MINTED_AT_KEY = "ketch_managed_identity_minted_at"
    const val CODE_PREFIX = "swb_"

    /** Matches the web tag's cookie default. Used whenever config carries no usable ttl. */
    const val DEFAULT_TTL_SECONDS = 400L * 86_400L

    private const val QUERY_STRING_TYPE = "querystring"
    private const val INITIAL_RETRY_DELAY_MS = 1_000L
    private const val MAX_RETRY_DELAY_MS = 60_000L

    private val TAG = ManagedIdentity::class.java.simpleName
    private val lock = Any()

    /** [code] keys headless request bodies; [variable] names the WebView query parameter. */
    data class Resolved(val code: String, val variable: String, val value: String)

    private data class Descriptor(val code: String, val variable: String, val ttlSeconds: Long)

    // A key present means resolved; a null value means the property declares no managed identity.
    private val resolved = mutableMapOf<String, Resolved?>()
    private val waiters = mutableMapOf<String, MutableList<(Resolved?) -> Unit>>()
    private val retryAtMs = mutableMapOf<String, Long>()
    private val retryDelayMs = mutableMapOf<String, Long>()

    // Bumped by clear() so a fetch already in flight cannot repopulate what was just deleted.
    private var generation = 0

    // Only for requests that carry no property code and so cannot name a space.
    private var lastResolved: Resolved? = null

    @VisibleForTesting
    internal var clock: () -> Long = { System.currentTimeMillis() }

    fun key(organizationCode: String, propertyCode: String) = "$organizationCode/$propertyCode"

    /** Cached answer only. Never hits the network, so a WebView load stays synchronous. */
    fun peek(organizationCode: String, propertyCode: String): Resolved? =
        synchronized(lock) { resolved[key(organizationCode, propertyCode)] }

    /** Whatever resolved most recently, for requests with no property code to look one up by. */
    fun lastResolved(): Resolved? = synchronized(lock) { lastResolved }

    /**
     * Resolves for one property, fetching config at most once and joining callers that arrive
     * while a fetch is in flight. [callback] runs inline on a cache hit.
     */
    fun resolve(
        organizationCode: String,
        propertyCode: String,
        client: HeadlessApiClient,
        callback: (Resolved?) -> Unit = {},
    ) {
        val cacheKey = key(organizationCode, propertyCode)
        var generationAtStart = 0
        var answerNow: Resolved? = null
        var hasAnswer = false
        var driveFetch = false
        synchronized(lock) {
            val retryAt = retryAtMs[cacheKey]
            val existing = waiters[cacheKey]
            when {
                resolved.containsKey(cacheKey) -> {
                    answerNow = resolved[cacheKey]
                    hasAnswer = true
                }
                retryAt != null && clock() < retryAt -> hasAnswer = true
                existing != null -> existing.add(callback)
                else -> {
                    waiters[cacheKey] = mutableListOf(callback)
                    generationAtStart = generation
                    driveFetch = true
                }
            }
        }
        // Outside the lock: an arbitrary caller callback must not stall every other thread.
        if (hasAnswer) {
            callback(answerNow)
            return
        }
        if (!driveFetch) return
        client.getIdentityConfiguration(organizationCode, propertyCode) { result ->
            result.fold(
                onSuccess = { onFetched(cacheKey, generationAtStart, it) },
                onFailure = { onFetchFailed(cacheKey, it) },
            )
        }
    }

    /**
     * Suspending form, for callers already inside a coroutine. Shares the cache, the backoff and
     * the in-flight waiter list with [resolve].
     */
    suspend fun await(
        organizationCode: String,
        propertyCode: String,
        client: HeadlessApiClient,
    ): Resolved? {
        val cacheKey = key(organizationCode, propertyCode)
        var generationAtStart = 0
        var joined: CompletableDeferred<Resolved?>? = null
        synchronized(lock) {
            if (resolved.containsKey(cacheKey)) return resolved[cacheKey]
            val retryAt = retryAtMs[cacheKey]
            if (retryAt != null && clock() < retryAt) return null
            val existing = waiters[cacheKey]
            if (existing != null) {
                // A fetch is already running; wait on it rather than starting a second one.
                val pending = CompletableDeferred<Resolved?>()
                existing.add { pending.complete(it) }
                joined = pending
            } else {
                waiters[cacheKey] = mutableListOf()
                generationAtStart = generation
            }
        }
        joined?.let { return it.await() }
        return try {
            val config = client.getIdentityConfiguration(organizationCode, propertyCode)
            onFetched(cacheKey, generationAtStart, config)
            synchronized(lock) { resolved[cacheKey] }
        } catch (error: Exception) {
            onFetchFailed(cacheKey, error)
            null
        }
    }

    private fun onFetched(cacheKey: String, generationAtStart: Int, config: IdentityConfiguration) {
        val callbacks: List<(Resolved?) -> Unit>
        var value: Resolved? = null
        synchronized(lock) {
            callbacks = waiters.remove(cacheKey).orEmpty()
            if (generationAtStart != generation) {
                Log.d(TAG, "discarding a resolution that was in flight when identities were cleared")
            } else {
                val descriptor = findDescriptor(config)
                value = descriptor?.let {
                    val stored = readOrMint(it)
                    if (stored == null) null else Resolved(it.code, it.variable, stored)
                }
                resolved[cacheKey] = value
                retryAtMs.remove(cacheKey)
                retryDelayMs.remove(cacheKey)
                value?.let { lastResolved = it }
            }
        }
        callbacks.forEach { it(value) }
    }

    private fun onFetchFailed(cacheKey: String, error: Throwable) {
        val callbacks: List<(Resolved?) -> Unit>
        synchronized(lock) {
            callbacks = waiters.remove(cacheKey).orEmpty()
            // Back off rather than caching the miss, so a flaky network cannot turn every
            // load/show call into another config fetch.
            val delay = (retryDelayMs[cacheKey]?.times(2) ?: INITIAL_RETRY_DELAY_MS)
                .coerceAtMost(MAX_RETRY_DELAY_MS)
            retryDelayMs[cacheKey] = delay
            retryAtMs[cacheKey] = clock() + delay
        }
        Log.w(TAG, "managed identity resolution failed, retrying later: " + error.message)
        callbacks.forEach { it(null) }
    }

    /**
     * Reads the descriptor for the managed space, or null when the property declares none or
     * still sources it from a cookie.
     */
    private fun findDescriptor(config: IdentityConfiguration): Descriptor? {
        val identities = config.identities
        if (identities == null) {
            // An unrecognised include value comes back 200 with the key absent, which otherwise
            // reads the same as a property declaring no identities.
            Log.w(TAG, "identity configuration response contained no identities key")
            return null
        }
        for ((code, identity) in identities) {
            if (!code.startsWith(CODE_PREFIX)) continue
            if (identity.type?.trim()?.lowercase() != QUERY_STRING_TYPE) {
                Log.d(TAG, "managed identity " + code + " is '" + identity.type + "', not queryString; supplying nothing")
                continue
            }
            val ttl = identity.ttl?.takeIf { it > 0 } ?: DEFAULT_TTL_SECONDS
            val variable = identity.variable?.takeIf { it.isNotBlank() } ?: code
            return Descriptor(code, variable, ttl)
        }
        return null
    }

    /**
     * One identifier per app, surfaced under whatever space code each property names. Expiry is
     * fixed from the mint rather than sliding, matching the web cookie, which is never rewritten.
     */
    private fun readOrMint(descriptor: Descriptor): String? {
        if (!KetchSharedPreferences.isReady) {
            // Reachable through KetchSdk's static headless methods, which run without a Ketch
            // instance and so without storage. Supplying nothing beats crashing the caller.
            Log.w(TAG, "storage is not initialized; no managed identity will be supplied")
            return null
        }
        val now = clock()
        val stored = KetchSharedPreferences.getSavedValue(STORAGE_KEY)
        if (!stored.isNullOrBlank()) {
            val mintedAt = KetchSharedPreferences.getSavedValue(MINTED_AT_KEY)?.toLongOrNull()
            val expired = mintedAt == null || now - mintedAt >= descriptor.ttlSeconds * 1_000
            if (!expired) return stored
        }
        // Already lowercase, and backed by a cryptographically strong generator.
        val minted = UUID.randomUUID().toString()
        KetchSharedPreferences.writeAll(
            mapOf(STORAGE_KEY to minted, MINTED_AT_KEY to now.toString()),
        )
        return minted
    }

    /** Drops the stored identifier and every cached resolution of it. */
    fun clear() {
        synchronized(lock) {
            generation++
            resolved.clear()
            retryAtMs.clear()
            retryDelayMs.clear()
            lastResolved = null
            if (KetchSharedPreferences.isReady) {
                KetchSharedPreferences.remove(STORAGE_KEY)
                KetchSharedPreferences.remove(MINTED_AT_KEY)
            }
        }
    }

    /** Pre-seeds a resolution so a test exercising something else does not trigger a config fetch. */
    @VisibleForTesting
    internal fun markResolvedForTesting(
        organizationCode: String,
        propertyCode: String,
        value: Resolved?,
    ) {
        synchronized(lock) { resolved[key(organizationCode, propertyCode)] = value }
    }

    @VisibleForTesting
    internal fun resetForTesting() {
        synchronized(lock) {
            generation++
            resolved.clear()
            waiters.clear()
            retryAtMs.clear()
            retryDelayMs.clear()
            lastResolved = null
            clock = { System.currentTimeMillis() }
        }
    }
}
