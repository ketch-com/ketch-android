package com.ketch.android

import android.app.Activity
import android.app.Application
import androidx.fragment.app.FragmentActivity
import java.lang.ref.WeakReference

/**
 * Tracks the foreground [FragmentActivity] so [Ketch] can display experiences
 * without requiring the integrator to pass a [FragmentManager] at construction time.
 */
internal class KetchLifecycleTracker(
    ketch: Ketch,
    private val app: Application,
) : Application.ActivityLifecycleCallbacks {

    private val ketchRef = WeakReference(ketch)

    @Volatile
    var current: WeakReference<FragmentActivity> = WeakReference(null)
        internal set

    fun seedCurrent(activity: FragmentActivity) {
        current = WeakReference(activity)
    }

    private fun aliveOrUnregister(): Ketch? =
        ketchRef.get() ?: run {
            app.unregisterActivityLifecycleCallbacks(this)
            null
        }

    override fun onActivityResumed(activity: Activity) {
        val ketch = aliveOrUnregister() ?: return
        if (activity is FragmentActivity) {
            current = WeakReference(activity)
            ketch.onHostResumed(activity)
        }
    }

    override fun onActivityPaused(activity: Activity) {
        aliveOrUnregister()
    }

    override fun onActivityDestroyed(activity: Activity) {
        if (current.get() === activity) {
            current = WeakReference(null)
        }
        aliveOrUnregister()?.onHostDestroyed(activity, activity.isChangingConfigurations)
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: android.os.Bundle?) {
        aliveOrUnregister()
    }

    override fun onActivityStarted(activity: Activity) {
        aliveOrUnregister()
    }

    override fun onActivityStopped(activity: Activity) {
        val ketch = aliveOrUnregister() ?: return
        ketch.onHostStopped(activity, activity.isChangingConfigurations)
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: android.os.Bundle) {
        aliveOrUnregister()
    }
}
