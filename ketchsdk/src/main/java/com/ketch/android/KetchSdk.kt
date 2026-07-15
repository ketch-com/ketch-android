package com.ketch.android

import android.content.Context
import androidx.fragment.app.FragmentManager
import com.ketch.android.api.HeadlessApiClient
import com.ketch.android.api.KetchDataCenter
import com.ketch.android.data.Consent
import com.ketch.android.data.ConsentConfig
import com.ketch.android.data.ConsentUpdate
import com.ketch.android.data.FullConfigurationRequest
import com.ketch.android.data.HeadlessConfiguration
import com.ketch.android.data.InvokeRightRequest
import com.ketch.android.data.LocationResponse
import com.ketch.android.data.PreferenceQRRequest
import com.ketch.android.data.SubscriptionsRequest
import com.ketch.android.data.SubscriptionsResponse

/**
 * Factory to create the Ketch object.
 *
 *         KetchSdk.create(
 *               applicationContext,
 *               ORG_CODE,
 *               PROPERTY,
 *               ENVIRONMENT,
 *               listener,
 *               dataCenter = KetchDataCenter.US,
 *               logLevel = Ketch.LogLevel.DEBUG
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
     * @param ketchUrl - Overrides the ketch url. Optional; defaults to [dataCenter] base URL.
     * @param dataCenter - CDN region for headless and WebView API calls. Default US.
     * @param logLevel - the log level, can be TRACE, DEBUG, INFO, WARN, ERROR. Default is ERROR
     */
    fun create(
        context: Context,
        organization: String,
        property: String,
        environment: String? = null,
        listener: Ketch.Listener? = null,
        ketchUrl: String? = null,
        dataCenter: KetchDataCenter = KetchDataCenter.US,
        logLevel: Ketch.LogLevel = Ketch.LogLevel.ERROR
    ): Ketch {
        return Ketch.create(
            context = context,
            orgCode = organization,
            property = property,
            environment = environment,
            listener = listener,
            ketchUrl = ketchUrl,
            dataCenter = dataCenter,
            logLevel = logLevel,
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
     * @param ketchUrl - Overrides the ketch url. Optional; defaults to [dataCenter] base URL.
     * @param dataCenter - CDN region for headless and WebView API calls. Default US.
     * @param logLevel - the log level, can be TRACE, DEBUG, INFO, WARN, ERROR. Default is ERROR
     */
    @Deprecated(
        message = "Ketch now tracks the foreground Activity automatically; the FragmentManager " +
            "argument is no longer required. Use create(context, organization, property, ...).",
        replaceWith = ReplaceWith(
            "KetchSdk.create(context, organization, property, environment, listener, ketchUrl, dataCenter, logLevel)"
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
        dataCenter: KetchDataCenter = KetchDataCenter.US,
        logLevel: Ketch.LogLevel = Ketch.LogLevel.ERROR,
    ): Ketch {
        return Ketch.create(
            context = context,
            fragmentManager = fragmentManager,
            orgCode = organization,
            property = property,
            environment = environment,
            listener = listener,
            ketchUrl = ketchUrl,
            dataCenter = dataCenter,
            logLevel = logLevel,
        )
    }

    // MARK: - Headless API (static, web/v3)

    /** GeoIP / jurisdiction hint (`GET /ip`). */
    fun getLocation(
        dataCenter: KetchDataCenter = KetchDataCenter.US,
        callback: (Result<LocationResponse>) -> Unit,
    ) {
        HeadlessApiClient(dataCenter).getLocation(callback)
    }

    /** Minimal config (`GET .../boot.json`). */
    fun getBootstrapConfiguration(
        organization: String,
        property: String,
        dataCenter: KetchDataCenter = KetchDataCenter.US,
        callback: (Result<HeadlessConfiguration>) -> Unit,
    ) {
        HeadlessApiClient(dataCenter).getBootstrapConfiguration(organization, property, callback)
    }

    /** Full config with optional env / jurisdiction / language and hash query param. */
    fun getFullConfiguration(
        request: FullConfigurationRequest,
        dataCenter: KetchDataCenter = KetchDataCenter.US,
        callback: (Result<HeadlessConfiguration>) -> Unit,
    ) {
        HeadlessApiClient(dataCenter).getFullConfiguration(request, callback)
    }

    /** Server consent including `protocols` (`POST .../consent/{org}/get`). */
    fun getConsent(
        config: ConsentConfig,
        dataCenter: KetchDataCenter = KetchDataCenter.US,
        callback: (Result<Consent>) -> Unit,
    ) {
        HeadlessApiClient(dataCenter).getConsent(config, callback)
    }

    /** Updates consent; returns server response with computed `protocols`. */
    fun setConsent(
        update: ConsentUpdate,
        dataCenter: KetchDataCenter = KetchDataCenter.US,
        callback: (Result<Consent>) -> Unit,
    ) {
        HeadlessApiClient(dataCenter).setConsent(
            update.copy(protocols = null),
            callback,
        )
    }

    fun invokeRight(
        request: InvokeRightRequest,
        dataCenter: KetchDataCenter = KetchDataCenter.US,
        callback: (Result<Unit>) -> Unit,
    ) {
        HeadlessApiClient(dataCenter).invokeRight(request, callback)
    }

    fun getSubscriptions(
        request: SubscriptionsRequest,
        dataCenter: KetchDataCenter = KetchDataCenter.US,
        callback: (Result<SubscriptionsResponse>) -> Unit,
    ) {
        HeadlessApiClient(dataCenter).getSubscriptions(request, callback)
    }

    fun setSubscriptions(
        request: SubscriptionsRequest,
        dataCenter: KetchDataCenter = KetchDataCenter.US,
        callback: (Result<Unit>) -> Unit,
    ) {
        HeadlessApiClient(dataCenter).setSubscriptions(request, callback)
    }

    fun preferenceQRUrl(
        request: PreferenceQRRequest,
        dataCenter: KetchDataCenter = KetchDataCenter.US,
    ): String = HeadlessApiClient(dataCenter).preferenceQRUrl(request)
}
