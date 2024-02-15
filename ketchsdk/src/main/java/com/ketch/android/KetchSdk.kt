package com.ketch.android

import android.content.Context
import androidx.fragment.app.FragmentManager

/**
 * Factory to create the Ketch singleton.
 *
 *         KetchSdk.create(
 *               this,
 *               supportFragmentManager,
 *               ORG_CODE,
 *               PROPERTY,
 *               ENVIRONMENT,
 *               listener,
 *               TEST_URL,
 *               Ketch.LogLevel.DEBUG
 *           ).build()
 **/
object KetchSdk {
    /**
     * Creates the Ketch Builder
     *
     * @param context - an Activity Context to access application assets
     * @param fragmentManager - The FragmentManager this KetchDialogFragment will be added to.
     * @param organization - your organization code
     * @param property - the property name
     * @param environment - the environment name.
     * @param listener - Ketch.Listener
     * @param ketchUrl - Overrides the ketch url
     * @param logLevel - the log level, can be TRACE, DEBUG, INFO, WARN, ERROR
     */
    fun create(
        context: Context,
        fragmentManager: FragmentManager,
        organization: String,
        property: String,
        environment: String? = null,
        listener: Ketch.Listener,
        ketchUrl: String? = null,
        logLevel: Ketch.LogLevel = Ketch.LogLevel.ERROR
    ): Ketch.Builder {
        return Ketch.Builder.create(
            context = context,
            fragmentManager = fragmentManager,
            orgCode = organization,
            property = property,
            environment = environment,
            listener = listener,
            ketchUrl = ketchUrl,
            logLevel = logLevel
        )
    }
}