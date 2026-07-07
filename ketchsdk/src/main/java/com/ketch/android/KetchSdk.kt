package com.ketch.android

import android.content.Context
import androidx.fragment.app.FragmentManager

/**
 * Factory to create the Ketch object.
 *
 *         KetchSdk.create(
 *               applicationContext,
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
     * Creates the Ketch instance. The SDK automatically tracks the foreground
     * [androidx.fragment.app.FragmentActivity] to display experiences.
     *
     * @param context - Application or Activity context
     * @param organization - your organization code
     * @param property - the property name
     * @param environment - the environment name.
     * @param listener - Ketch.Listener. Optional
     * @param ketchUrl - Overrides the ketch url. Optional
     * @param logLevel - the log level, can be TRACE, DEBUG, INFO, WARN, ERROR. Default is ERROR
     * @param webResourceUrlOverrides - redirects exact-match WebView resource URLs (e.g. dev/staging
     *   tag scripts) to local dev servers. Optional
     */
    fun create(
        context: Context,
        organization: String,
        property: String,
        environment: String? = null,
        listener: Ketch.Listener? = null,
        ketchUrl: String? = null,
        logLevel: Ketch.LogLevel = Ketch.LogLevel.ERROR,
        webResourceUrlOverrides: Map<String, String> = emptyMap(),
    ): Ketch {
        return Ketch.create(
            context = context,
            orgCode = organization,
            property = property,
            environment = environment,
            listener = listener,
            ketchUrl = ketchUrl,
            logLevel = logLevel,
            webResourceUrlOverrides = webResourceUrlOverrides,
        )
    }

    /**
     * Creates the Ketch instance using an explicit [FragmentManager].
     *
     * @param context - an Activity Context to access application assets
     * @param fragmentManager - The FragmentManager this KetchDialogFragment will be added to.
     * @param organization - your organization code
     * @param property - the property name
     * @param environment - the environment name.
     * @param listener - Ketch.Listener. Optional
     * @param ketchUrl - Overrides the ketch url. Optional
     * @param logLevel - the log level, can be TRACE, DEBUG, INFO, WARN, ERROR. Default is ERROR
     */
    @Deprecated(
        message = "Ketch now tracks the foreground Activity automatically; the FragmentManager " +
            "argument is no longer required. Use create(context, organization, property, ...).",
        replaceWith = ReplaceWith(
            "KetchSdk.create(context, organization, property, environment, listener, ketchUrl, logLevel)"
        ),
        level = DeprecationLevel.WARNING,
    )
    fun create(
        context: Context,
        fragmentManager: FragmentManager,
        organization: String,
        property: String,
        environment: String? = null,
        listener: Ketch.Listener? = null,
        ketchUrl: String? = null,
        logLevel: Ketch.LogLevel = Ketch.LogLevel.ERROR,
        webResourceUrlOverrides: Map<String, String> = emptyMap(),
    ): Ketch {
        return Ketch.create(
            context = context,
            fragmentManager = fragmentManager,
            orgCode = organization,
            property = property,
            environment = environment,
            listener = listener,
            ketchUrl = ketchUrl,
            logLevel = logLevel,
            webResourceUrlOverrides = webResourceUrlOverrides,
        )
    }
}
