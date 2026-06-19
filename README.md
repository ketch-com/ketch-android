# Ketch Mobile SDK for Android

The Ketch Mobile SDK allows to manage and collect a visitor's consent preferences for an organization on the mobile platforms.

## Requirements

The minimum Android API version supported is 26.

The use of the Mobile SDK requires an [Ketch organization account](https://app.ketch.com/settings/organization)
with the [application property](https://app.ketch.com/deployment/applications) configured.

## Adding KetchSDK in your project

### 1. Using sources

1. Copy and paste ketchsdk module to your project
2. Add "include ':ketchsdk'" to settings.graddle
3. Add dependency into your main module:

```gradle
       implementation project(':ketchsdk')
```

### 2. Using .aar lib

1. Add it in your root build.gradle at the end of repositories:

```gradle
        repositories {
           ...
           maven { url 'https://jitpack.io' }
       }
```

2. Add the dependency:

```gradle
        implementation 'com.github.ketch-com:ketch-android:main-SNAPSHOT'
```

If you want you can use our [Sample](https://github.com/ketch-sdk/ketch-samples)

### 3. Add user-permissions to AndroidManifest.xml

```xml
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="com.google.android.gms.permission.AD_ID" />
```

### 4. Add listener and create Ketch in your Application class

Create a single `Ketch` instance early in the application lifecycle (typically in an `Application` subclass). The SDK automatically tracks the foreground `FragmentActivity` so you can call `showConsent()` / `showPreferences()` from any Activity without passing a `FragmentManager`.

Feel free to skip the listeners you don't really need.

```kotlin
   import android.app.Application
   import android.util.Log
   import com.ketch.android.Ketch
   import com.ketch.android.KetchSdk
   import com.ketch.android.data.Consent
   import com.ketch.android.data.HideExperienceStatus
   import com.ketch.android.data.KetchConfig
   import com.ketch.android.data.WillShowExperienceType
   // ...
   class MyApplication : Application() {

       lateinit var ketch: Ketch
           private set

       private val listener = object : Ketch.Listener {
        override fun onShow() {
            Log.d("KetchApp", "Dialog shown") // Called when a consent or preferences dialog is displayed
        }

        override fun onDismiss(status: HideExperienceStatus) {
            Log.d("KetchApp", "Dialog dismissed: $status") // Called when a dialog is dismissed
        }

        override fun onConfigUpdated(config: KetchConfig?) {
            Log.d("KetchApp", "Config updated")
        }

        override fun onEnvironmentUpdated(environment: String?) {
            Log.d("KetchApp", "Environment updated: $environment") // Called when the environment is updated
        }

        override fun onRegionInfoUpdated(regionInfo: String?) {
            Log.d("KetchApp", "Region info updated: $regionInfo") // Called when region info is updated
        }

        override fun onJurisdictionUpdated(jurisdiction: String?) {
            Log.d("KetchApp", "Jurisdiction updated: $jurisdiction") // Called when jurisdiction is updated
        }

        override fun onIdentitiesUpdated(identities: String?) {
            Log.d("KetchApp", "Identities updated: $identities") // Called when identities are updated
        }

        override fun onConsentUpdated(consent: Consent) {
            Log.d("KetchApp", "Consent updated") // Called when consent preferences are updated
            
            // Here you can handle consent changes for your app features
            // Example: Enable/disable tracking based on consent
            val hasAnalyticsConsent = consent.purposes["analytics"] == true
            val hasAdvertisingConsent = consent.purposes["advertising"] == true
            
            if (hasAnalyticsConsent) {
                // Enable analytics tracking
            } else {
                // Disable analytics tracking
            }
            
            if (hasAdvertisingConsent) {
                // Enable advertising features
            } else {
                // Disable advertising features
            }
        }

        override fun onError(errMsg: String?) {
            Log.e("KetchApp", "Error: $errMsg") // Called when an error occurs
        }

        override fun onUSPrivacyUpdated(values: Map<String, Any?>) {
            Log.d("KetchApp", "US Privacy updated") // Called when US Privacy values are updated
            
            // You can access the US Privacy string
            val privacyString = values["IABUSPrivacy_String"] as? String
            Log.d("KetchApp", "US Privacy String: $privacyString")
        }

        override fun onTCFUpdated(values: Map<String, Any?>) {
            Log.d("KetchApp", "TCF updated") // Called when TCF values are updated
            val tcString = values["IABTCF_TCString"] as? String // You can access the TC string
            Log.d("KetchApp", "TCF TC String: $tcString")
        }

        override fun onGPPUpdated(values: Map<String, Any?>) {
            Log.d("KetchApp", "GPP updated") // Called when GPP values are updated
            
            val gppString = values["IABGPP_HDR_GppString"] as? String // You can access the GPP string
            Log.d("KetchApp", "GPP String: $gppString")
        }

        override fun onWillShowExperience(type: WillShowExperienceType) {
            Log.d("KetchApp", "Will show experience: $type")
        }

        override fun onHasShownExperience() {
            Log.d("KetchApp", "Experience has shown")
        }
    }

       override fun onCreate() {
           super.onCreate()
           ketch = KetchSdk.create(
               context = this,
               organization = ORG_CODE,
               property = PROPERTY,
               environment = ENVIRONMENT,
               listener = listener,
               ketchUrl = null,
               logLevel = Ketch.LogLevel.DEBUG,
           )
       }

       companion object {
           private const val ORG_CODE = "<your organization code>"
           private const val PROPERTY = "<property>"
           private const val ENVIRONMENT = "production"
       }
   }
```

Register your `Application` subclass in `AndroidManifest.xml`:

```xml
    <application
        android:name=".MyApplication"
        ... >
```

### 5. Add your user identities and call load() from an Activity

In any `FragmentActivity` (for example `MainActivity`), use the shared instance:

```kotlin
    class MainActivity : AppCompatActivity() {

        private lateinit var ketch: Ketch

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            ketch = (application as MyApplication).ketch

            with(ketch) {
                setIdentities(
                    mapOf(
                        "aaid" to advertisingIdCode,
                        "email" to "user@mywebsite.com",
                        "account_id" to "1234"
                    )
                )
                load()
            }
        }
    }
```

The same `ketch` instance can be used from other Activities in your app. When the user navigates away while an experience is showing, the SDK automatically dismisses it and reports `onDismiss(HideExperienceStatus.ActivityChanged)`. Rotation and backgrounding do not dismiss the experience.

## Headless API (web/v3, pre-WebView)

Use native HTTP for cold-start flows **before** loading the WebView—location, config, and consent with `protocols` from the CDN. Contract: [mobile-headless-api.md](https://github.com/ketch-com/ketch-tag/blob/main/docs/design/mobile-headless-api.md).

Testing: [mobile-headless-api-testing.md](https://github.com/ketch-com/ketch-tag/blob/main/docs/design/mobile-headless-api-testing.md). (App Tracking Transparency is iOS-only — not applicable on Android.)

Pass `dataCenter` when creating the SDK (`KetchDataCenter.US`, `EU`, or `UAT`). Instance methods use the SDK's data center; static `KetchSdk` methods accept an optional `dataCenter` parameter.

```kotlin
val ketch = KetchSdk.create(
    activity = this,
    fragmentManager = supportFragmentManager,
    organization = ORG_CODE,
    property = PROPERTY,
    environment = ENVIRONMENT,
    listener = listener,
    dataCenter = KetchDataCenter.US,
)

// Recommended cold-start order
ketch.fetchLocation { result -> /* jurisdiction hint */ }
ketch.fetchBootstrapConfiguration { result -> /* boot.json */ }
ketch.fetchFullConfiguration(
    FullConfigurationRequest(
        organizationCode = ORG_CODE,
        propertyCode = PROPERTY,
        environmentCode = ENVIRONMENT,
        jurisdictionCode = "us-ca",
        languageCode = "en-US",
        hash = hashFromBootstrap,
    )
) { result -> /* full config */ }

ketch.fetchConsent(consentConfig) { result ->
    // Consent includes purposes and protocols (GPP, TCF, US Privacy, …)
}
ketch.setConsent(consentUpdate) { result ->
    // Server-computed protocols in response; request omits protocols
}

// Rights, profile, subscriptions
ketch.invokeRight(invokeRightRequest) { }
ketch.getProfile(profileRequest) { }
ketch.putProfile(putProfileRequest) { }
ketch.getSubscriptions(subscriptionsRequest) { }
ketch.setSubscriptions(subscriptionsRequest) { }

// Subscriptions config, QR URL, telemetry
ketch.fetchSubscriptionsConfiguration(subConfigRequest) { }
val qrUrl = ketch.preferenceQRUrl(
    PreferenceQRRequest(
        organizationCode = ORG_CODE,
        propertyCode = PROPERTY,
        environmentCode = ENVIRONMENT,
        imageSize = 1024,
    )
)
ketch.webReport("mychannel", reportRequest) { }
```

`getConsent()` on the WebView listener path is unchanged. Headless calls do not require `load()`.

## Local Development Setup

If you're developing or modifying the SDK and want to test your changes with the sample app, you can use Gradle's composite builds feature to link them together.

### Setting up the Sample App for Local Development

1. Clone both repositories:
   - Ketch Android SDK: `git clone https://github.com/ketch-com/ketch-android.git`
   - Ketch Samples: `git clone https://github.com/ketch-sdk/ketch-samples.git`

2. In the sample app's `settings.gradle` file, add the following:

```gradle
// Include the Ketch SDK from the local repository
includeBuild('../../../../ketch-android') {
    dependencySubstitution {
        substitute module('com.github.ketch-com:ketch-android') using project(':ketchsdk')
    }
}
```

We use relative path here under assumption that both repositories are in the same parent directory.
If using a different structure, adjust the path accordingly.

### Troubleshooting

Make sure you're rebuilding the project after making changes to the SDK.
If the sample app isn't picking up the local SDK, try running `./gradlew clean` in both projects.

### Reverting to Remote Dependencies

To revert back to using the remote GitHub dependency:

1. Remove or comment out the `includeBuild` section in the sample app's `settings.gradle` file
2. Rebuild the sample app

## Developer's Documentations

### com.ketch.android.Ketch

**class Ketch** - Main class where the SDK functionality resides.

#### Methods:

```kotlin
    /**
     * Loads a web page and shows a popup if necessary
     */
    fun load()

    /**
     * Retrieve a String value from the preferences.
     *
     * @param key The name of the preference to retrieve.
     *
     * @return Returns the preference value if it exists
     */
    fun getSavedString(key: String): String?

    /**
     * Retrieve IABTCF_TCString value from the preferences.
     *
     * @return Returns the preference value if it exists
     */
    fun getTCFTCString(): String?

    /**
     * Retrieve IABUSPrivacy_String value from the preferences.
     *
     * @return Returns the preference value if it exists
     */
    fun getUSPrivacyString(): String?

    /**
     * Retrieve IABGPP_HDR_GppString value from the preferences.
     *
     * @return Returns the preference value if it exists
     */
    fun getGPPHDRGppString(): String?

    /**
     * Display the consent experience in the foreground Activity.
     */
    fun showConsent()

    /**
     * Display the preferences experience in the foreground Activity.
     */
    fun showPreferences()

    /**
     * Display the preferences tab, adding the fragment dialog to the given FragmentManager.
     *
     * @param tabs: list of preferences tab
     * @param tab: the current tab
     */
    fun showPreferencesTab(tabs: List<PreferencesTab>, tab: PreferencesTab)

    /**
     * Dismiss the dialog
     */
    fun dismissDialog()

    /**
     * Set identities
     *
     * @param identities: Map<String, String>
     */
    fun setIdentities(identities: Map<String, String>)

    /**
     * Set the language
     *
     * @param language: a language name (EN, FR, etc.)
     */
    fun setLanguage(language: String)

    /**
     * Set the jurisdiction
     *
     * @param jurisdiction: the jurisdiction value
     */
    fun setJurisdiction(jurisdiction: String?)

    /**
     * Set Region
     *
     * @param region: the region name
     */
    fun setRegion(region: String?)
```

### com.ketch.android.KetchSdk

**class KetchSdk** - Class used to initialize the Ketch SDK

#### Methods:

```kotlin
    /**
     * Creates the Ketch instance. The SDK automatically tracks the foreground
     * FragmentActivity to display experiences.
     *
     * @param context - Application or Activity context
     * @param organization - your organization code
     * @param property - the property name
     * @param environment - the environment name. Optional
     * @param listener - Ketch.Listener. Optional
     * @param ketchUrl - Overrides the ketch url. Optional
     * @param logLevel - the log level, can be TRACE, DEBUG, INFO, WARN, ERROR. Default is ERROR
     */
    fun create(
        context: Context,
        organization: String,
        property: String,
        environment: String? = null,
        listener: Ketch.Listener? = null,
        ketchUrl: String? = null,
        logLevel: Ketch.LogLevel = Ketch.LogLevel.ERROR
    ): Ketch

    /**
     * @deprecated Ketch now tracks the foreground Activity automatically.
     * Use create(context, organization, property, ...) instead.
     */
    fun create(
        context: Context,
        fragmentManager: FragmentManager,
        organization: String,
        property: String,
        environment: String? = null,
        listener: Ketch.Listener? = null,
        ketchUrl: String? = null,
        logLevel: Ketch.LogLevel = Ketch.LogLevel.ERROR
    ): Ketch
```

### com.ketch.android.Ketch.Listener

**interface Ketch.Listener** - Interface used to list events from the sdk.

#### Methods:

```kotlin
        /**
         * Called when a dialog is displayed
         */
        fun onShow()

        /**
         * Called when a dialog is dismissed
         */
        fun onDismiss(status: HideExperienceStatus)

        /**
         * Called when the config is updated.
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
```

## Sample apps

This repository includes sample apps demonstrating single-instance initialization and cross-Activity display:

- `sample-app-standard/` — View-based sample with `MainActivity` and `SecondActivity`
- `sample-app-compose/` — Jetpack Compose sample with the same pattern

We also provide a complete sample app in our samples repository: [here](https://github.com/ketch-sdk/ketch-samples/tree/main/ketch-android/Android%20Native%20SDK%20Sample)
