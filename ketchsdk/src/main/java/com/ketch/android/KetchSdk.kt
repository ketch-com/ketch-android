package com.ketch.android

import android.app.Application
import android.content.Context
import androidx.fragment.app.FragmentManager

/**
 * Factory to create the Ketch object.
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
 *           )
 **/
object KetchSdk {
    /**
     * Creates the Ketch
     *
     * @param application - Application Context
     * @param organization - your organization code
     * @param property - the property name
     * @param environment - the environment name.
     * @param listener - Ketch.Listener. Optional
     * @param ketchUrl - Overrides the ketch url. Optional
     * @param logLevel - the log level, can be TRACE, DEBUG, INFO, WARN, ERROR. Default is ERROR
     */
    fun create(
        application: Application,
        organization: String,
        property: String,
        environment: String? = null,
        listener: Ketch.Listener? = null,
        ketchUrl: String? = null,
        logLevel: Ketch.LogLevel = Ketch.LogLevel.ERROR,
        shouldRetry: Boolean = false,
        synchronousPreferences: Boolean = false
    ): Ketch {
        return Ketch.create(
            application = application,
            orgCode = organization,
            property = property,
            environment = environment,
            listener = listener,
            ketchUrl = ketchUrl,
            logLevel = logLevel,
            shouldRetry = shouldRetry,
            synchronousPreferences = synchronousPreferences
        )
    }
}