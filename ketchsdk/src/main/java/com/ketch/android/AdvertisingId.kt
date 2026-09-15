package com.ketch.android

import android.content.Context
import android.util.Log
import androidx.annotation.VisibleForTesting
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.atomic.AtomicReference

/**
 * The `ketchNativeResolve` key for the Android advertising ID (AAID).
 */
internal const val KEY_AAID = "ketch_aaid"

private const val GMS_AD_ID_CLIENT_CLASS = "com.google.android.gms.ads.identifier.AdvertisingIdClient"
private const val ZEROED_AAID = "00000000-0000-0000-0000-000000000000"

/**
 * Reads the platform advertising ID. Returns null when unavailable or when the user has limited
 * ad tracking — a zeroed ID must never be treated as a value.
 */
internal interface AaidReader {
    fun read(context: Context): String?
}

// Play Services returns the zeroed UUID both when the user has opted out (isLimitAdTrackingEnabled)
// and, separately, when the app hasn't declared the AD_ID permission — in the latter case the flag
// itself can read false. Check the value directly rather than trusting the flag alone.
internal fun aaidOrNull(id: String?, isLimitAdTrackingEnabled: Boolean): String? =
    if (isLimitAdTrackingEnabled || id == null || id == ZEROED_AAID) null else id

internal object GmsAaidReader : AaidReader {
    private val TAG = GmsAaidReader::class.java.simpleName

    override fun read(context: Context): String? = try {
        val info = AdvertisingIdClient.getAdvertisingIdInfo(context)
        aaidOrNull(info.id, info.isLimitAdTrackingEnabled)
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
 * and runs off the calling thread; [resolve] waits a bounded time for it, so the first request
 * returns the value rather than null. It backs a synchronous `@JavascriptInterface` call, which
 * runs on the WebView's JavaBridge thread, so that wait stalls only the tag's JS — never the UI.
 */
internal object AaidResolver {
    private const val RESOLVE_TIMEOUT_MS = 500L

    private sealed class State {
        object NotStarted : State()
        data class InFlight(val job: Deferred<String?>) : State()
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
     * Returns the AAID, waiting up to [timeoutMs] for a first read. Runs on the WebView's
     * JavaBridge thread - never the main thread - so the bounded wait stalls only the tag's JS.
     */
    fun resolve(context: Context, timeoutMs: Long = RESOLVE_TIMEOUT_MS): String? {
        while (true) {
            when (val current = state.get()) {
                is State.Resolved -> return current.value
                is State.InFlight -> return current.publishIfDone(timeoutMs)
                State.NotStarted -> {
                    if (!isAvailable()) {
                        state.set(State.Resolved(null))
                        return null
                    }
                    val appContext = context.applicationContext ?: context
                    // LAZY so a caller that loses the race below never starts a second read:
                    // an eagerly-started job would already be in Play Services before cancel().
                    val job = scope.async(start = CoroutineStart.LAZY) { reader.read(appContext) }
                    val inFlight = State.InFlight(job)
                    if (state.compareAndSet(current, inFlight)) {
                        job.start()
                        return inFlight.publishIfDone(timeoutMs)
                    }
                    job.cancel()
                }
            }
        }
    }

    /**
     * Waits up to [timeoutMs] for this read, and promotes the state to [State.Resolved] once it
     * lands. Without the promotion a call that timed out would leave the state on [State.InFlight]
     * for good, and [cachedValue] — which getIdentities() reads through — would never see the AAID.
     */
    private fun State.InFlight.publishIfDone(timeoutMs: Long): String? {
        val value = runBlocking { withTimeoutOrNull(timeoutMs) { job.await() } }
        if (job.isCompleted) {
            state.compareAndSet(this, State.Resolved(job.getCompleted()))
        }
        return value
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
