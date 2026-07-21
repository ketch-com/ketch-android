package com.ketch.android

import android.app.Activity
import android.app.Application
import android.content.Context
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import com.ketch.android.api.HeadlessApiClient
import com.ketch.android.api.KetchDataCenter
import com.ketch.android.data.Consent
import com.ketch.android.data.ConsentConfig
import com.ketch.android.data.ConsentUpdate
import com.ketch.android.data.ContentDisplay
import com.ketch.android.data.FullConfigurationRequest
import com.ketch.android.data.HeadlessConfiguration
import com.ketch.android.data.HideExperienceStatus
import com.ketch.android.data.InvokeRightRequest
import com.ketch.android.data.KetchConfig
import com.ketch.android.data.LocationResponse
import com.ketch.android.data.PreferenceQRRequest
import com.ketch.android.data.SubscriptionsRequest
import com.ketch.android.data.SubscriptionsResponse
import com.ketch.android.data.WillShowExperienceType
import com.ketch.android.data.configPathSegment
import com.ketch.android.data.jurisdictionCode
import com.ketch.android.data.normalizedHash
import com.ketch.android.data.toRegionCode
import com.ketch.android.ui.KetchDialogFragment
import com.ketch.android.ui.KetchWebView
import org.json.JSONObject
import java.lang.ref.WeakReference

/**
 * Main Ketch SDK class
 **/
@Suppress("unused")
class Ketch private constructor(
    context: Context,
    seedActivity: FragmentActivity?,
    seedFragmentManager: FragmentManager?,
    private val orgCode: String,
    private val property: String,
    private val environment: String?,
    private val listener: Listener?,
    private val ketchUrl: String?,
    private val dataCenter: KetchDataCenter,
    private val logLevel: LogLevel,
    private val headlessApiClient: HeadlessApiClient,
) {
    // Falls back to the CDN region's base URL when no explicit ketchUrl override is provided.
    private val effectiveKetchUrl: String = ketchUrl ?: dataCenter.baseUrl

    // Use application context for non-UI operations to avoid memory leaks
    private val context: Context = context.applicationContext

    private val application = context.applicationContext as? Application

    // Optional seeds from deprecated create(context, fragmentManager, ...) for backward compatibility
    private var seedActivity: WeakReference<FragmentActivity>? =
        seedActivity?.let { WeakReference(it) }
    private var seedFragmentManager: WeakReference<FragmentManager>? =
        seedFragmentManager?.let { WeakReference(it) }

    private val tracker: KetchLifecycleTracker? = application?.let { app ->
        KetchLifecycleTracker(this, app).also { lifecycleTracker ->
            app.registerActivityLifecycleCallbacks(lifecycleTracker)
            if (seedActivity != null) {
                lifecycleTracker.seedCurrent(seedActivity)
            }
        }
    }

    private var identities: Map<String, String> = emptyMap()
    private var language: String? = null
    private var jurisdiction: String? = null
    private var region: String? = null
    private var cssStyle: String? = null
    private var age: Int? = null
    private var ageLower: Int? = null
    private var ageUpper: Int? = null

    // Flag to prevent multiple overlapping experiences
    @Volatile
    private var isShowingExperience = false

    // Reference to the active fragment to do cleanup
    private var activeDialogFragment: WeakReference<KetchDialogFragment>? = null

    // The Activity that currently hosts the shown dialog. Used to auto-dismiss the
    // experience when the integrator navigates to a different Activity while it is showing.
    private var dialogHost: WeakReference<FragmentActivity>? = null

    // Reference to the active webView to prevent multiple webView instances existence
    private var activeWebView: KetchWebView? = null

    // The Activity whose context built activeWebView, if any. Used by onHostDestroyed
    // to avoid tearing down a WebView owned by an Activity other than the one being destroyed.
    private var activeWebViewHost: WeakReference<FragmentActivity>? = null

    // Cache key for the config/presentation inputs baked into the boot HTML (excludes show type/tabs)
    private var loadedSignature: String? = null

    // Headless getFullConfiguration() cache — avoids re-fetching when the config URL path is unchanged
    private var cachedConfig: HeadlessConfiguration? = null
    private var cachedConfigKey: String? = null

    // Headless getLocation() cache — GET /ip takes no path params, so it never needs invalidation
    private var cachedLocation: LocationResponse? = null

    // When true, the next dialog dismissal retains the WebView instead of destroying it
    private var retainWebViewOnDismiss = false

    // Deferred trigger() call, fired once a cold-booted WebView's tag finishes loading
    @Volatile
    private var pendingTrigger: PendingTrigger? = null

    // Lock object for synchronization
    private val lock = Any()

    /**
     * Retrieve a String value from the preferences.
     *
     * @param key The name of the preference to retrieve.
     *
     * @return Returns the preference value if it exists
     */
    fun getSavedString(key: String) = getPreferences().getSavedValue(key)

    /**
     * Retrieve IABTCF_TCString value from the preferences.
     *
     * @return Returns the preference value if it exists
     */
    fun getTCFTCString() = getPreferences().getSavedValue(KetchSharedPreferences.IAB_TCF_TC_STRING)

    /**
     * Retrieve IABUSPrivacy_String value from the preferences.
     *
     * @return Returns the preference value if it exists
     */
    fun getUSPrivacyString() =
        getPreferences().getSavedValue(KetchSharedPreferences.IAB_US_PRIVACY_STRING)

    /**
     * Retrieve IABGPP_HDR_GppString value from the preferences.
     *
     * @return Returns the preference value if it exists
     */
    fun getGPPHDRGppString() =
        getPreferences().getSavedValue(KetchSharedPreferences.IAB_GPP_HDR_GPP_STRING)

    /**
     * Loads a web page and shows a popup if necessary
     *
     * @param bottomPadding: Pixels of padding to add to the bottom of the experience (if shown)
     */
    fun load(
        shouldRetry: Boolean = false,
        synchronousPreferences: Boolean = false,
        bottomPadding: Int = 0,
        topPadding: Int = 0,
    ): Boolean {
        if (isShowingExperience) {
            Log.d(TAG, "Not loading as an experience is already being shown")
            return false
        }

        val signature = buildLoadSignature(bottomPadding, topPadding)
        if (tryWarmWebView(signature) != null) {
            Log.d(TAG, "WebView already loaded with matching signature; skipping reload")
            return true
        }

        val webView = prepareColdWebView(shouldRetry, synchronousPreferences, signature) ?: return false
        webView.load(
            orgCode,
            property,
            language,
            jurisdiction,
            region,
            environment,
            identities,
            null,
            emptyList(),
            null,
            effectiveKetchUrl,
            logLevel,
            age,
            ageLower,
            ageUpper,
            bottomPadding,
            topPadding,
            cssStyle
        )
        return true
    }

    /** CDN region used for headless and WebView API calls. */
    fun getDataCenter(): KetchDataCenter = dataCenter

    /**
     * Combined ISO region code (e.g. "US-CA"). Returns the value from [setRegion] if one is set
     * on this instance; otherwise falls back to GeoIP (`GET /ip`), cached on this instance.
     */
    fun getRegion(callback: (Result<String?>) -> Unit) {
        region?.let {
            callback(Result.success(it))
            return
        }
        synchronized(lock) {
            cachedLocation?.let {
                callback(Result.success(it.location?.toRegionCode()))
                return
            }
        }
        headlessApiClient.getLocation { result ->
            result.onSuccess { location -> synchronized(lock) { cachedLocation = location } }
            callback(result.map { it.location?.toRegionCode() })
        }
    }

    suspend fun getRegion(): String? {
        region?.let { return it }
        synchronized(lock) { cachedLocation?.let { return it.location?.toRegionCode() } }
        val location = headlessApiClient.getLocation()
        synchronized(lock) { cachedLocation = location }
        return location.location?.toRegionCode()
    }

    /** Minimal config (`GET .../boot.json`). */
    fun getBootstrapConfiguration(
        callback: (Result<HeadlessConfiguration>) -> Unit,
    ) {
        headlessApiClient.getBootstrapConfiguration(orgCode, property, callback)
    }

    suspend fun getBootstrapConfiguration(): HeadlessConfiguration =
        headlessApiClient.getBootstrapConfiguration(orgCode, property)

    /**
     * Full config with optional env / jurisdiction / language and hash query param.
     *
     * Cached on this instance, keyed on the request's URL-path-affecting fields. A cache hit
     * skips the network call entirely; [setJurisdiction] and [setLanguage] clear the cache since
     * they change the config URL path.
     */
    fun getFullConfiguration(
        request: FullConfigurationRequest,
        callback: (Result<HeadlessConfiguration>) -> Unit,
    ) {
        val key = buildConfigCacheKey(request)
        synchronized(lock) {
            if (cachedConfigKey == key) {
                cachedConfig?.let {
                    callback(Result.success(it))
                    return
                }
            }
        }
        headlessApiClient.getFullConfiguration(request) { result ->
            result.onSuccess { config -> synchronized(lock) { cachedConfig = config; cachedConfigKey = key } }
            callback(result)
        }
    }

    suspend fun getFullConfiguration(request: FullConfigurationRequest): HeadlessConfiguration {
        val key = buildConfigCacheKey(request)
        synchronized(lock) {
            if (cachedConfigKey == key) {
                cachedConfig?.let { return it }
            }
        }
        val config = headlessApiClient.getFullConfiguration(request)
        synchronized(lock) { cachedConfig = config; cachedConfigKey = key }
        return config
    }

    /**
     * Resolved jurisdiction code. Returns the value from [setJurisdiction] if one is set on this
     * instance; otherwise falls back to the server-resolved code from [getFullConfiguration].
     */
    fun getJurisdiction(callback: (Result<String?>) -> Unit) {
        jurisdiction?.let {
            callback(Result.success(it))
            return
        }
        getFullConfiguration(buildJurisdictionConfigRequest()) { result ->
            callback(result.map { it.jurisdictionCode() })
        }
    }

    suspend fun getJurisdiction(): String? {
        jurisdiction?.let { return it }
        return getFullConfiguration(buildJurisdictionConfigRequest()).jurisdictionCode()
    }

    private fun buildJurisdictionConfigRequest(): FullConfigurationRequest =
        FullConfigurationRequest(
            organizationCode = orgCode,
            propertyCode = property,
            environmentCode = environment,
            jurisdictionCode = jurisdiction,
            languageCode = language,
        )

    /** Server consent including `protocols` (`POST .../consent/{org}/get`). */
    fun getConsent(
        config: ConsentConfig,
        callback: (Result<Consent>) -> Unit,
    ) {
        headlessApiClient.getConsent(config, callback)
    }

    suspend fun getConsent(config: ConsentConfig): Consent =
        headlessApiClient.getConsent(config)

    /** Updates consent; returns server response with computed `protocols`. */
    fun setConsent(
        update: ConsentUpdate,
        callback: (Result<Consent>) -> Unit,
    ) {
        headlessApiClient.setConsent(update.withoutProtocols(), callback)
    }

    suspend fun setConsent(update: ConsentUpdate): Consent =
        headlessApiClient.setConsent(update.withoutProtocols())

    /** Invokes a data subject right (`POST .../rights/{org}/invoke`). */
    fun invokeRight(
        request: InvokeRightRequest,
        callback: (Result<Unit>) -> Unit,
    ) {
        headlessApiClient.invokeRight(request, callback)
    }

    suspend fun invokeRight(request: InvokeRightRequest) = headlessApiClient.invokeRight(request)

    /** Gets subscription topics/controls (`POST .../subscriptions/{org}/get`). */
    fun getSubscriptions(
        request: SubscriptionsRequest,
        callback: (Result<SubscriptionsResponse>) -> Unit,
    ) {
        headlessApiClient.getSubscriptions(request, callback)
    }

    suspend fun getSubscriptions(request: SubscriptionsRequest): SubscriptionsResponse =
        headlessApiClient.getSubscriptions(request)

    /** Updates subscription topics/controls (`POST .../subscriptions/{org}/update`). */
    fun setSubscriptions(
        request: SubscriptionsRequest,
        callback: (Result<Unit>) -> Unit,
    ) {
        headlessApiClient.setSubscriptions(request, callback)
    }

    suspend fun setSubscriptions(request: SubscriptionsRequest) =
        headlessApiClient.setSubscriptions(request)

    fun getPreferenceQRUrl(request: PreferenceQRRequest): String =
        headlessApiClient.getPreferenceQRUrl(request)

    /**
     * Display the consent, adding the fragment dialog to the given FragmentManager.
     *
     * @param bottomPadding: Pixels of padding to add to the bottom of the experience
     */
    fun showConsent(
        shouldRetry: Boolean = false,
        synchronousPreferences: Boolean = false,
        bottomPadding: Int = 0,
        topPadding: Int = 0,
    ): Boolean {
        if (isShowingExperience) {
            Log.d(TAG, "Not showing consent as an experience is already being shown")
            return false
        }

        val signature = buildLoadSignature(bottomPadding, topPadding)
        tryWarmWebView(signature, "showConsent")?.let { webView ->
            webView.showConsentExperience()
            return true
        }

        Log.d(TAG, "Cold WebView load for showConsent")
        val webView = prepareColdWebView(shouldRetry, synchronousPreferences, signature) ?: return false
        webView.load(
            orgCode,
            property,
            language,
            jurisdiction,
            region,
            environment,
            identities,
            KetchWebView.ExperienceType.CONSENT,
            emptyList(),
            null,
            effectiveKetchUrl,
            logLevel,
            age,
            ageLower,
            ageUpper,
            bottomPadding,
            topPadding,
            cssStyle
        )
        return true
    }

    /**
     * Display the preferences, adding the fragment dialog to the given FragmentManager.
     *
     * @param bottomPadding: Pixels of padding to add to the bottom of the experience
     */
    fun showPreferences(
        shouldRetry: Boolean = false,
        synchronousPreferences: Boolean = false,
        bottomPadding: Int = 0,
        topPadding: Int = 0,
    ): Boolean {
        if (isShowingExperience) {
            Log.d(TAG, "Not showing preferences as an experience is already being shown")
            return false
        }

        val signature = buildLoadSignature(bottomPadding, topPadding)
        tryWarmWebView(signature, "showPreferences")?.let { webView ->
            webView.showPreferenceExperience(buildPreferenceOptionsJson())
            return true
        }

        Log.d(TAG, "Cold WebView load for showPreferences")
        val webView = prepareColdWebView(shouldRetry, synchronousPreferences, signature) ?: return false
        webView.load(
            orgCode,
            property,
            language,
            jurisdiction,
            region,
            environment,
            identities,
            KetchWebView.ExperienceType.PREFERENCES,
            emptyList(),
            null,
            effectiveKetchUrl,
            logLevel,
            age,
            ageLower,
            ageUpper,
            bottomPadding,
            topPadding,
            cssStyle
        )
        return true
    }

    /**
     * Display the preferences tab, adding the fragment dialog to the given FragmentManager.
     *
     * @param tabs: list of preferences tab
     * @param tab: the current tab
     * @param bottomPadding: Pixels of padding to add to the bottom of the experience
     */
    fun showPreferencesTab(
        tabs: List<PreferencesTab>,
        tab: PreferencesTab,
        shouldRetry: Boolean = false,
        synchronousPreferences: Boolean = false,
        bottomPadding: Int = 0,
        topPadding: Int = 0,
    ): Boolean {
        if (isShowingExperience) {
            Log.d(TAG, "Not showing preferences tab as an experience is already being shown")
            return false
        }

        val signature = buildLoadSignature(bottomPadding, topPadding)
        tryWarmWebView(signature, "showPreferencesTab(tab=${tab.name})")?.let { webView ->
            webView.showPreferenceExperience(buildPreferenceOptionsJson(tabs, tab))
            return true
        }

        Log.d(TAG, "Cold WebView load for showPreferencesTab(tab=${tab.name})")
        val webView = prepareColdWebView(shouldRetry, synchronousPreferences, signature) ?: return false
        webView.load(
            orgCode,
            property,
            language,
            jurisdiction,
            region,
            environment,
            identities,
            KetchWebView.ExperienceType.PREFERENCES,
            tabs,
            tab,
            effectiveKetchUrl,
            logLevel,
            age,
            ageLower,
            ageUpper,
            bottomPadding,
            topPadding,
            cssStyle
        )
        return true
    }

    /**
     * Fire a custom-function (`onFunction`) rule trigger. If a matching backend rule shows an
     * experience, the fragment dialog is displayed automatically.
     *
     * @param triggerName: the trigger name; `TriggerName.CUSTOM` is the only supported value today
     * @param functionName: the custom function name configured on the backend rule
     * @param options: optional key/value trigger arguments
     */
    fun trigger(
        triggerName: TriggerName,
        functionName: String,
        options: Map<String, Any?> = emptyMap(),
    ): Boolean {
        val triggerNameValue = when (triggerName) {
            TriggerName.CUSTOM -> triggerName.value
        }
        val safeName = validateFunctionName(functionName) ?: return false

        if (isShowingExperience) {
            Log.d(TAG, "Not triggering '$safeName' as an experience is already being shown")
            return false
        }

        val optionsJson = JSONObject(options).toString()
        val signature = buildLoadSignature(0, 0)

        tryWarmWebView(signature, "trigger($safeName)")?.let { webView ->
            // Supersede any deferred cold trigger so onConfigUpdated does not re-fire it.
            pendingTrigger = null
            webView.trigger(triggerNameValue, safeName, optionsJson)
            return true
        }

        Log.d(TAG, "Cold WebView load for trigger($safeName)")
        val webView = prepareColdWebView(false, false, signature) ?: return false
        pendingTrigger = PendingTrigger(triggerNameValue, safeName, optionsJson)
        webView.load(
            orgCode,
            property,
            language,
            jurisdiction,
            region,
            environment,
            identities,
            null,
            emptyList(),
            null,
            effectiveKetchUrl,
            logLevel,
            age,
            ageLower,
            ageUpper,
            0,
            0,
            cssStyle
        )
        return true
    }

    /**
     * Dismiss the dialog
     */
    fun dismissDialog() {
        synchronized(lock) {
            retainWebViewOnDismiss = false
            val fragment = findDialogFragment()
            if (fragment != null) {
                try {
                    (fragment as? KetchDialogFragment)?.dismissAllowingStateLoss()
                } catch (e: Exception) {
                    Log.e(TAG, "Error dismissing dialog: ${e.message}")
                } finally {
                    Log.d(TAG, "onDismiss source=dismissDialog status=None")
                    resetShowingState(HideExperienceStatus.None)
                }
            } else if (isShowingExperience || activeWebView != null) {
                Log.d(TAG, "onDismiss source=dismissDialog status=None")
                resetShowingState(HideExperienceStatus.None)
            }
        }
    }

    /**
     * Set identities
     *
     * @param identities: Map<String, String>
     */
    fun setIdentities(identities: Map<String, String>) {
        this.identities = identities
    }

    /**
     * Set the language
     *
     * @param language: a language name (EN, FR, etc.)
     */
    fun setLanguage(language: String?) {
        this.language = language
        clearConfigCache()
    }

    /**
     * Set the jurisdiction
     *
     * @param jurisdiction: the jurisdiction value
     */
    fun setJurisdiction(jurisdiction: String?) {
        this.jurisdiction = jurisdiction
        clearConfigCache()
    }

    /**
     * Set Region
     *
     * @param region: the region name
     */
    fun setRegion(region: String?) {
        this.region = region
        // Not a config URL path component (see buildConfigCacheKey) — the config cache is
        // intentionally left intact.
    }

    // Invalidates the getFullConfiguration() cache; called whenever a config URL path
    // component (jurisdiction, language) changes on this instance.
    private fun clearConfigCache() {
        synchronized(lock) {
            cachedConfig = null
            cachedConfigKey = null
        }
    }

    /**
     * Set CSS Style
     *
     * @param cssStyle: the string with css style
     */
    fun setCssStyle(cssStyle: String?) {
        this.cssStyle = validateCssStyle(cssStyle)
    }

    /**
     * Set the exact age of the user.
     * Used for age band resolution to determine the appropriate legal basis for each purpose.
     *
     * @param age: the user's exact age
     */
    fun setAge(age: Int?) {
        this.age = age
    }

    /**
     * Set the lower bound of the user's age range.
     * Used for age band resolution when an exact age is not known.
     *
     * @param ageLower: the lower bound of the user's age range
     */
    fun setAgeLower(ageLower: Int?) {
        this.ageLower = ageLower
    }

    /**
     * Set the upper bound of the user's age range.
     * Used for age band resolution when an exact age is not known.
     *
     * @param ageUpper: the upper bound of the user's age range
     */
    fun setAgeUpper(ageUpper: Int?) {
        this.ageUpper = ageUpper
    }

    private fun validateCssStyle(cssStyle: String?): String? {
        if (containsHTMLTags(cssStyle)) {
            Log.w(TAG, "[Ketch] CSS override rejected: must not contain HTML tags!")
            return null
        }

        if (!isWithin1kb(cssStyle)) {
            Log.w(TAG, "[Ketch] CSS override rejected: CSS too long (>1kb limit)!")
            return null
        }

        return cssStyle
    }

    private fun containsHTMLTags(css: String?): Boolean = css?.contains(Regex("<[a-zA-Z]")) == true

    private fun isWithin1kb(css: String?): Boolean = (css?.toByteArray(Charsets.UTF_8)?.size ?: 0) <= 1024

    private fun validateFunctionName(functionName: String): String? {
        if (!isValidTriggerFunctionName(functionName)) {
            Log.w(
                TAG,
                "[Ketch] trigger rejected: functionName must be non-blank and contain only " +
                    "letters, digits, '_', '-', or '.'",
            )
            return null
        }
        return functionName
    }

    init {
        getPreferences()

        // Ensure any existing dialog fragments are properly cleaned up
        synchronized(lock) {
            resolveFragmentManager()?.findFragmentByTag(KetchDialogFragment.TAG)?.let { existingFragment ->
                try {
                    (existingFragment as? KetchDialogFragment)?.dismissAllowingStateLoss()
                    this@Ketch.listener?.onDismiss(HideExperienceStatus.None)
                } catch (e: Exception) {
                    Log.e(TAG, "Error dismissing existing dialog in init: ${e.message}")
                }
            }
        }
    }

    private fun resolveHost(): FragmentActivity? =
        tracker?.current?.get() ?: seedActivity?.get()

    private fun resolveFragmentManager(): FragmentManager? =
        resolveHost()?.supportFragmentManager ?: seedFragmentManager?.get()

    private fun reportNoHostError() {
        isShowingExperience = false
        Log.e(TAG, "No active Activity to host the Ketch experience")
        this@Ketch.listener?.onError("No active Activity to host the Ketch experience")
    }

    /**
     * Re-binds [dialogHost] and [activeDialogFragment] after a configuration change recreates
     * the host Activity instance while the dialog is still showing.
     */
    internal fun onHostResumed(activity: FragmentActivity) {
        if (!isShowingExperience) return
        val fragment = activity.supportFragmentManager
            .findFragmentByTag(KetchDialogFragment.TAG) as? KetchDialogFragment ?: return
        synchronized(lock) {
            activeDialogFragment = WeakReference(fragment)
            dialogHost = WeakReference(activity)
        }
    }

    /**
     * Invoked by [KetchLifecycleTracker] when an Activity is stopped. If the stopped Activity
     * is the one hosting a currently-shown experience AND a different Activity is now in the
     * foreground (i.e. the integrator navigated to another screen), the orphaned experience is
     * auto-dismissed so the SDK does not get stuck and the new Activity can show experiences.
     *
     * Backgrounding (Home/recents) is intentionally ignored: in that case the foreground
     * Activity is still the host, so the dialog is preserved for when the user returns.
     * Configuration changes (e.g. rotation) are also ignored.
     *
     * When auto-dismiss fires, [Listener.onDismiss] is called with [HideExperienceStatus.ActivityChanged].
     */
    internal fun onHostStopped(stoppedActivity: Activity, isChangingConfigurations: Boolean) {
        if (isChangingConfigurations) return
        if (!isShowingExperience) return
        val host = dialogHost?.get() ?: return
        if (host !== stoppedActivity) return

        val foreground = tracker?.current?.get()
        if (foreground != null && foreground !== host) {
            Log.d(TAG, "Host Activity left the foreground while showing; auto-dismissing experience")
            autoDismissOnHostGone()
        }
    }

    // Dismiss the active dialog directly via the tracked fragment reference (not via the
    // resolved FragmentManager, which now points at the new foreground Activity) and reset state.
    private fun autoDismissOnHostGone() {
        synchronized(lock) {
            if (!isShowingExperience && activeDialogFragment?.get() == null) return
            try {
                activeDialogFragment?.get()?.dismissAllowingStateLoss()
            } catch (e: Exception) {
                Log.e(TAG, "Error auto-dismissing dialog on host change: ${e.message}")
            } finally {
                resetShowingState(HideExperienceStatus.ActivityChanged)
            }
        }
    }

    /**
     * Invoked when the Activity hosting a shown experience is destroyed (e.g. [Activity.finish]).
     * Configuration changes are ignored so rotation does not reset state.
     * Does not report [HideExperienceStatus.ActivityChanged] — the host was destroyed, not replaced.
     */
    internal fun onHostDestroyed(destroyedActivity: Activity, isChangingConfigurations: Boolean) {
        if (isChangingConfigurations) {
            if (!isShowingExperience) {
                synchronized(lock) {
                    tearDownWebViewIfOwnedBy(destroyedActivity)
                }
            }
            return
        }

        if (!isShowingExperience) {
            synchronized(lock) {
                tearDownWebViewIfOwnedBy(destroyedActivity)
            }
            return
        }

        val host = dialogHost?.get() ?: return
        if (host !== destroyedActivity) return
        synchronized(lock) {
            resetShowingState(HideExperienceStatus.None)
        }
    }

    // An unrelated Activity's destroy must not kill a WebView tied to a different, still-alive host
    private fun tearDownWebViewIfOwnedBy(destroyedActivity: Activity) {
        val host = activeWebViewHost?.get() ?: return
        if (host !== destroyedActivity) return
        Log.d(TAG, "onHostDestroyed: tearing down WebView owned by ${host.javaClass.simpleName}")
        cleanupWebView()
    }

    private fun resetShowingState(dismissStatus: HideExperienceStatus) {
        retainWebViewOnDismiss = false
        cleanupWebView()
        isShowingExperience = false
        activeDialogFragment = null
        dialogHost = null
        this@Ketch.listener?.onDismiss(dismissStatus)
    }

    private fun handleDialogDismissed() {
        val retain = retainWebViewOnDismiss
        retainWebViewOnDismiss = false
        isShowingExperience = false
        activeDialogFragment = null
        dialogHost = null
        if (retain) {
            activeWebView?.detachFromParent()
        } else {
            cleanupWebView()
        }
    }

    private fun buildLoadSignature(bottomPadding: Int, topPadding: Int): String =
        listOf(
            orgCode,
            property,
            effectiveKetchUrl,
            environment ?: "",
            language ?: "",
            jurisdiction ?: "",
            region ?: "",
            identities.entries.sortedBy { it.key }.joinToString(",") { "${it.key}=${it.value}" },
            age?.toString() ?: "",
            ageLower?.toString() ?: "",
            ageUpper?.toString() ?: "",
            cssStyle ?: "",
            bottomPadding.toString(),
            topPadding.toString(),
            logLevel.name,
        ).joinToString("|")

    // Cache key for getFullConfiguration() — mirrors HeadlessApiClient's path-building exactly
    // (blank treated as absent) via configPathSegment()/normalizedHash(), so requests that hit
    // the same URL always share a key and requests that hit different URLs never collide.
    private fun buildConfigCacheKey(request: FullConfigurationRequest): String {
        val (env, jurisdiction, language) = request.configPathSegment() ?: Triple("", "", "")
        return listOf(
            request.organizationCode,
            request.propertyCode,
            env,
            jurisdiction,
            language,
            request.normalizedHash() ?: "",
        ).joinToString("|")
    }

    private fun buildPreferenceOptionsJson(
        tabs: List<PreferencesTab> = emptyList(),
        tab: PreferencesTab? = null,
    ): String {
        val json = JSONObject()
        tab?.let { json.put("tab", it.getUrlParameter()) }
        if (tabs.isNotEmpty()) {
            json.put("showOverviewTab", PreferencesTab.OVERVIEW in tabs)
            json.put("showConsentsTab", PreferencesTab.CONSENTS in tabs)
            json.put("showRightsTab", PreferencesTab.RIGHTS in tabs)
            json.put("showSubscriptionsTab", PreferencesTab.SUBSCRIPTIONS in tabs)
        }
        return json.toString()
    }

    private fun tryWarmWebView(signature: String, operation: String? = null): KetchWebView? {
        synchronized(lock) {
            val cached = activeWebView
            if (cached != null && loadedSignature == signature) {
                if (operation != null) {
                    Log.d(
                        TAG,
                        "Using warm WebView for $operation " +
                            "(identity=${System.identityHashCode(cached)}, pageLoadCount=${cached.pageLoadCount})",
                    )
                }
                return cached
            }
            return null
        }
    }

    private fun prepareColdWebView(
        shouldRetry: Boolean,
        synchronousPreferences: Boolean,
        signature: String,
    ): KetchWebView? {
        synchronized(lock) {
            if (isShowingExperience || findDialogFragment() != null) {
                Log.d(TAG, "Not preparing WebView as experience is already being shown")
                return null
            }
            cleanupWebView()
            loadedSignature = signature
            return createWebView(shouldRetry, synchronousPreferences)
        }
    }

    // Get the singleton KetchSharedPreferences object
    private fun getPreferences(): KetchSharedPreferences =
        // Initialize will create KetchSharedPreferences if it doesn't already exist
        KetchSharedPreferences.apply { initialize(context) }

    // Tear down the active WebView to release the Activity context it holds,
    // preventing memory leaks when Ketch outlives the Activity.
    private fun cleanupWebView() {
        activeWebView?.kill()
        activeWebView = null
        activeWebViewHost = null
        loadedSignature = null
        pendingTrigger = null
    }

    private fun createWebView(shouldRetry: Boolean = false, synchronousPreferences: Boolean = false): KetchWebView? {
        synchronized(lock) {
            if (isShowingExperience || findDialogFragment() != null) {
                Log.d(TAG, "Not creating WebView as experience is already being shown")
                return null
            }

            if (activeWebView != null) {
                return activeWebView
            }

            // Use Activity context for WebView so native popups (e.g., <select> dropdowns)
            // can obtain a valid window token. Falls back to applicationContext when no
            // foreground Activity is tracked (callbacks still resolve; display needs a host).
            val webViewHost = resolveHost()
            val webViewContext = webViewHost ?: context
            val webView = KetchWebView(webViewContext, shouldRetry)

            // Enable debug mode
            if (logLevel === LogLevel.DEBUG) {
                webView.setDebugMode()
            }

            webView.listener = object : KetchWebView.WebViewListener {

                private var config: KetchConfig? = null
                private var showConsent: Boolean = false

                override fun showConsent() {
                    if (config == null) {
                        showConsent = true
                        return
                    }
                    showConsentPopup()
                }

                override fun showPreferences() {
                    synchronized(lock) {
                        if (isShowingExperience || findDialogFragment() != null) {
                            Log.d(TAG, "Not showing as dialog already exists")
                            return
                        }

                        try {
                            resolveFragmentManager()?.let { fm ->
                                if (!fm.isDestroyed) {
                                    val dialog = KetchDialogFragment.newInstance(ketchWebView = webView) {
                                        handleDialogDismissed()
                                    }
                                    dialog.show(manager = fm)
                                    isShowingExperience = true
                                    activeDialogFragment = WeakReference(dialog)
                                    dialogHost = resolveHost()?.let { WeakReference(it) }
                                    this@Ketch.listener?.onShow()
                                } else {
                                    isShowingExperience = false
                                    Log.e(TAG, "FragmentManager is destroyed, cannot show dialog")
                                    this@Ketch.listener?.onError("FragmentManager is destroyed, cannot show dialog")
                                }
                            } ?: run {
                                reportNoHostError()
                            }
                        } catch (e: Exception) {
                            isShowingExperience = false
                            Log.e(TAG, "Error showing dialog: ${e.message}")
                            this@Ketch.listener?.onError("Error showing dialog: ${e.message}")
                        }
                    }
                }

                override fun onUSPrivacyUpdated(values: Map<String, Any?>) {
                    getPreferences().saveValues(values, "USPrivacy", synchronousPreferences)
                    this@Ketch.listener?.onUSPrivacyUpdated(values)
                }

                override fun onTCFUpdated(values: Map<String, Any?>) {
                    getPreferences().saveValues(values, "TCF", synchronousPreferences)
                    this@Ketch.listener?.onTCFUpdated(values)
                }

                override fun onGPPUpdated(values: Map<String, Any?>) {
                    getPreferences().saveValues(values, "GPP", synchronousPreferences)
                    this@Ketch.listener?.onGPPUpdated(values)
                }

                override fun onConfigUpdated(config: KetchConfig?) {
                    this.config = config

                    this@Ketch.listener?.onConfigUpdated(config)

                    pendingTrigger?.let { pending ->
                        pendingTrigger = null
                        webView.trigger(pending.triggerName, pending.functionName, pending.optionsJson)
                    }

                    if (!showConsent) {
                        return
                    }
                    showConsentPopup()
                }

                override fun onEnvironmentUpdated(environment: String?) {
                    this@Ketch.listener?.onEnvironmentUpdated(environment)
                }

                override fun onRegionInfoUpdated(regionInfo: String?) {
                    this@Ketch.listener?.onRegionInfoUpdated(regionInfo)
                }

                override fun onJurisdictionUpdated(jurisdiction: String?) {
                    this@Ketch.listener?.onJurisdictionUpdated(jurisdiction)
                }

                override fun onIdentitiesUpdated(identities: String?) {
                    this@Ketch.listener?.onIdentitiesUpdated(identities)
                }

                override fun onConsentUpdated(consent: Consent) {
                    this@Ketch.listener?.onConsentUpdated(consent)
                }

                override fun onError(errMsg: String?) {
                    this@Ketch.listener?.onError(errMsg)
                }

                override fun changeDialog(display: ContentDisplay) {
                    findDialogFragment()?.let {
                        (it as? KetchDialogFragment)?.apply {
                            isCancelable = getDisposableContentInteractions(display)
                        }
                    }
                }

                override fun onClose(status: HideExperienceStatus, source: String, retainWebView: Boolean) {
                    synchronized(lock) {
                        if (!retainWebView) {
                            activeWebView = null
                            activeWebViewHost = null
                            loadedSignature = null
                        } else {
                            retainWebViewOnDismiss = true
                        }

                        isShowingExperience = false
                        activeDialogFragment = null
                        dialogHost = null

                        val fragment = findDialogFragment()
                        if (fragment != null) {
                            try {
                                (fragment as? KetchDialogFragment)?.dismissAllowingStateLoss()
                            } catch (e: Exception) {
                                Log.e(TAG, "Error dismissing dialog: ${e.message}")
                                handleDialogDismissed()
                            }
                        } else {
                            handleDialogDismissed()
                        }
                        Log.d(TAG, "onDismiss source=$source status=${status.name}")
                        this@Ketch.listener?.onDismiss(status)
                    }
                }

                override fun onWillShowExperience(experienceType: WillShowExperienceType) {
                    this@Ketch.listener?.onWillShowExperience(experienceType)
                }

                override fun onHasShownExperience() {
                    this@Ketch.listener?.onHasShownExperience()
                }

                private fun showConsentPopup() {
                    synchronized(lock) {
                        if (isShowingExperience || findDialogFragment() != null) {
                            Log.d(TAG, "Not showing as already showing an experience")
                            return
                        }

                        try {
                            val dialog = KetchDialogFragment.newInstance(ketchWebView = webView) {
                                handleDialogDismissed()
                            }.apply {
                                val disableContentInteractions = getDisposableContentInteractions(
                                    config?.experiences?.consent?.display ?: ContentDisplay.Banner
                                )
                                isCancelable = !disableContentInteractions
                            }

                            resolveFragmentManager()?.let { fm ->
                                if (!fm.isDestroyed) {
                                    dialog.show(manager = fm)
                                    isShowingExperience = true
                                    activeDialogFragment = WeakReference(dialog)
                                    dialogHost = resolveHost()?.let { WeakReference(it) }
                                    this@Ketch.listener?.onShow()
                                } else {
                                    isShowingExperience = false
                                    Log.e(TAG, "FragmentManager is destroyed, cannot show dialog")
                                    this@Ketch.listener?.onError("FragmentManager is destroyed, cannot show dialog")
                                }
                            } ?: run {
                                reportNoHostError()
                            }
                        } catch (e: Exception) {
                            isShowingExperience = false
                            Log.e(TAG, "Error showing dialog: ${e.message}")
                            this@Ketch.listener?.onError("Error showing dialog: ${e.message}")
                        }

                        showConsent = false
                    }
                }

                private fun getDisposableContentInteractions(display: ContentDisplay): Boolean =
                    config?.let {
                        when (display) {
                            ContentDisplay.Modal -> {
                                it.theme?.modal?.container?.backdrop?.disableContentInteractions == true
                            }

                            ContentDisplay.Banner -> {
                                it.theme?.modal?.container?.backdrop?.disableContentInteractions == true
                            }
                        }
                    } ?: false
            }

            activeWebView = webView
            activeWebViewHost = webViewHost?.let { WeakReference(it) }

            return activeWebView
        }
    }

    private fun findDialogFragment(): Fragment? {
        // First check our active reference, which is faster than searching
        val activeFragment = activeDialogFragment?.get()
        if (activeFragment != null && activeFragment.isAdded && !activeFragment.isDetached) {
            return activeFragment
        }

        // Fall back to searching by tag
        return resolveFragmentManager()?.findFragmentByTag(KetchDialogFragment.TAG)
    }

    enum class PreferencesTab {
        OVERVIEW,
        RIGHTS,
        CONSENTS,
        SUBSCRIPTIONS;

        fun getUrlParameter(): String = when (this) {
            OVERVIEW -> "overviewTab"
            RIGHTS -> "rightsTab"
            CONSENTS -> "consentsTab"
            SUBSCRIPTIONS -> "subscriptionsTab"
        }
    }

    enum class LogLevel {
        TRACE, DEBUG, INFO, WARN, ERROR
    }

    interface Listener {
        /**
         * Called when a dialog is displayed
         */
        fun onShow()

        /**
         * Called when a dialog is dismissed
         */
        fun onDismiss(status: HideExperienceStatus)

        /**
         * Called when the config is updated
         */
        fun onConfigUpdated(config: KetchConfig?)

        /**
         * Called when the environment is updated.
         */
        fun onEnvironmentUpdated(environment: String?)

        /**
         * Called when the region is updated.
         */
        fun onRegionInfoUpdated(regionInfo: String?)

        /**
         * Called when the jurisdiction is updated.
         */
        fun onJurisdictionUpdated(jurisdiction: String?)

        /**
         * Called when the identities is updated.
         */
        fun onIdentitiesUpdated(identities: String?)

        /**
         * Called when the consent is updated.
         */
        fun onConsentUpdated(consent: Consent)

        /**
         * Called on error.
         */
        fun onError(errMsg: String?)

        /**
         * Called when USPrivacy is updated.
         */
        fun onUSPrivacyUpdated(values: Map<String, Any?>)

        /**
         * Called when TCF is updated.
         */
        fun onTCFUpdated(values: Map<String, Any?>)

        /**
         * Called when GPP is updated.
         */
        fun onGPPUpdated(values: Map<String, Any?>)

        /**
         * Called when an experience will show, if there is one.
         */
        fun onWillShowExperience(type: WillShowExperienceType)

        /**
         * Called when an experience has shown
         */
        fun onHasShownExperience()
    }

    companion object {
        val TAG = Ketch::class.java.simpleName

        fun create(
            context: Context,
            orgCode: String,
            property: String,
            environment: String?,
            listener: Listener?,
            ketchUrl: String?,
            dataCenter: KetchDataCenter = KetchDataCenter.US,
            logLevel: LogLevel,
        ) = Ketch(
            context = context,
            seedActivity = context as? FragmentActivity,
            seedFragmentManager = null,
            orgCode = orgCode,
            property = property,
            environment = environment,
            listener = listener,
            ketchUrl = ketchUrl,
            dataCenter = dataCenter,
            logLevel = logLevel,
            headlessApiClient = HeadlessApiClient(dataCenter),
        )

        fun create(
            context: Context,
            fragmentManager: FragmentManager,
            orgCode: String,
            property: String,
            environment: String?,
            listener: Listener?,
            ketchUrl: String?,
            dataCenter: KetchDataCenter = KetchDataCenter.US,
            logLevel: LogLevel,
        ) = Ketch(
            context = context,
            seedActivity = context as? FragmentActivity,
            seedFragmentManager = fragmentManager,
            orgCode = orgCode,
            property = property,
            environment = environment,
            listener = listener,
            ketchUrl = ketchUrl,
            dataCenter = dataCenter,
            logLevel = logLevel,
            headlessApiClient = HeadlessApiClient(dataCenter),
        )
    }
}

private fun ConsentUpdate.withoutProtocols(): ConsentUpdate =
    copy(protocols = null)

private data class PendingTrigger(val triggerName: String, val functionName: String, val optionsJson: String)

private val TRIGGER_FUNCTION_NAME_REGEX = Regex("^[A-Za-z0-9_.-]+$")

internal fun isValidTriggerFunctionName(functionName: String): Boolean =
    TRIGGER_FUNCTION_NAME_REGEX.matches(functionName)
