# Ketch Mobile SDK for Android

The Ketch Mobile SDK allows to manage and collect a visitor's consent preferences for an organization on the mobile platforms.

## Requirements

Minimum Android API version supported is 28

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

### 4. Add constants to companion object of your activity

```kotlin
        private const val ORG_CODE = "<your organization code>"
        private const val PROPERTY = "<property>"
        private const val ENVIRONMENT = "production"
```

### 5. Add listener and Ketch to your activity :

```kotlin
   private val listener = object : Ketch.Listener {
        ...
   }
```

### 6. Create the Ketch Object:

```kotlin
    /**
     * Creates the Ketch
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
    private val ketch: Ketch by lazy {
        KetchSdk.create(
            this,
            supportFragmentManager,
            ORG_CODE,
            PROPERTY,
            ENVIRONMENT,
            listener,
            ketchUrl,
            Ketch.LogLevel.DEBUG
        )
    }
```

### 7. Add your user identities and call load() method

```kotlin
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
```

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
     * Display the consent, adding the fragment dialog to the given FragmentManager.
     */
    fun showConsent()

    /**
     * Display the preferences, adding the fragment dialog to the given FragmentManager.
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
     * Creates the Ketch
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
    fun create(
        context: Context,
        fragmentManager: FragmentManager,
        organization: String,
        property: String,
        environment: String?,
        listener: Ketch.Listener?,
        ketchUrl: String?,
        logLevel: Ketch.LogLevel
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
        fun onDismiss()

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
```

## Sample app

We provide a complete sample app to illustrate the integration: [here](https://github.com/ketch-sdk/ketch-samples/tree/main/ketch-android/Android%20Native%20SDK%20Sample)
