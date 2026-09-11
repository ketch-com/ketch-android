package com.ketch.android

import android.content.Context
import android.util.Log
import androidx.annotation.VisibleForTesting
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicReference

/**
 * The `ketchNativeResolve` key for the Android advertising ID (AAID).
 */
internal const val KEY_AAID = "ketch_aaid"

private const val GMS_AD_ID_CLIENT_CLASS = "com.google.android.gms.ads.identifier.AdvertisingIdClient"

/**
 * Reads the platform advertising ID. Returns null when unavailable or when the user has limited
 * ad tracking — a zeroed ID must never be treated as a value.
 */
internal interface AaidReader {
    fun read(context: Context): String?
}

internal object GmsAaidReader : AaidReader {
    private val TAG = GmsAaidReader::class.java.simpleName

    override fun read(context: Context): String? = try {
        val info = AdvertisingIdClient.getAdvertisingIdInfo(context)
        if (info.isLimitAdTrackingEnabled) null else info.id
    } catch (ex: Throwable) {
        // Throwable, not Exception: also guards NoClassDefFoundError, in case only part of the
        // compileOnly artifact is present at runtime despite the Class.forName gate passing.
        Log.e(TAG, "AAID read failed", ex)
        null
    }
}

/**
 * Resolves and caches the AAID for the process lifetime only — it must never be persisted, so a
 * user's ad ID reset is honored on next launch. Resolution is lazy (first request for [KEY_AAID])
 * and always off the calling thread: [resolve] never blocks, since it backs a synchronous
 * `@JavascriptInterface` call the tag's JS is waiting on.
 */
internal object AaidResolver {
    private sealed class State {
        object NotStarted : State()
        object InFlight : State()
        data class Resolved(val value: String?) : State()
    }

    private val state = AtomicReference<State>(State.NotStarted)

    @VisibleForTesting
    internal var reader: AaidReader = GmsAaidReader

    @VisibleForTesting
    internal var isAvailable: () -> Boolean = ::isAaidClassAvailable

    @VisibleForTesting
    internal var scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Returns the cached AAID, or null and (if not already in flight) kicks off a background
     * resolve. Always returns immediately.
     */
    fun resolve(context: Context): String? {
        return when (val current = state.get()) {
            is State.Resolved -> current.value
            State.InFlight -> null
            State.NotStarted -> {
                if (!state.compareAndSet(State.NotStarted, State.InFlight)) {
                    // Lost the race to another caller; this poll just returns null too.
                    return (state.get() as? State.Resolved)?.value
                }
                if (!isAvailable()) {
                    state.set(State.Resolved(null))
                    return null
                }
                val appContext = context.applicationContext ?: context
                scope.launch {
                    val value = reader.read(appContext)
                    state.set(State.Resolved(value))
                }
                null
            }
        }
    }

    /**
     * Forgets the cached AAID so the next [resolve] re-reads it. Local cache only — never touches
     * Play Services or device settings; a device ad ID reset is the user's action, not ours.
     */
    fun reset() {
        state.set(State.NotStarted)
    }

    /** The cached AAID, or null if nothing has resolved yet. */
    fun cachedValue(): String? = (state.get() as? State.Resolved)?.value

    @VisibleForTesting
    internal fun resetForTesting() {
        state.set(State.NotStarted)
        reader = GmsAaidReader
        isAvailable = ::isAaidClassAvailable
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
}

private fun isAaidClassAvailable(): Boolean = try {
    Class.forName(GMS_AD_ID_CLIENT_CLASS)
    true
} catch (_: ClassNotFoundException) {
    false
}
