# Ketch Mobile SDK for Android

The Ketch Mobile SDK allows to manage and collect a visitor's consent preferences for an organization on the mobile platforms.

## Mobile SDK Beta Program Disclaimer

Thank you for your interest in our Mobile SDK Beta Program!

Before proceeding, please note that this version of the software library is in its
BETA stage. While we have made efforts to ensure functionality and stability,
there may still be bugs or incomplete features present.

To ensure a smooth experience and access to the full capabilities of the SDK,
we kindly request that all users contact our customer support team
to enroll their organization in the Beta program.

Once approved, you will receive necessary credentials and information to begin
using the SDK effectively.

Please reach out to [customer support](mailto:support@ketch.com) to initiate the enrollment process.

Your feedback during this Beta period is invaluable to us as we work towards the official release.
Thank you for your collaboration and understanding.

## Requirements

Minimum Android API version supported is 31

The use of the Mobile SDK requires an [Ketch organization account](https://app.ketch.com/settings/organization)
with the [application property](https://app.ketch.com/deployment/applications)  configured.

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

### 3. Add constants to companion object of your activity
```kotlin
        private const val ORG_CODE = "<your organization code>"
        private const val PROPERTY = "<property>"
        private const val ADVERTISING_ID_CODE = "aaid"
        private const val ENVIRONMENT = "production"
```
### 4. Add listener and Ketch to your activity:
```kotlin
   private val listener = object : Ketch.Listener {
        
        /**
        * Called when the page is loaded by the sdk
        */
        override fun onLoad() {
            
        }
        
        /**
        * Called when a dialog is displayed
        */
        override fun onShow() {
            
        }
        
        /**
        * Called when a dialog is dismissed
        */
        override fun onDismiss() {
            
        }
        
        /**
        * Called when the environment is updated.
        */
        override fun onEnvironmentUpdated(environment: String?) {
            
        }
        
        /**
        * Called when the region is updated.
        */
        override fun onRegionInfoUpdated(regionInfo: String?) {
            
        }
        
        /**
        * Called when the jurisdiction is updated.
        */
        override fun onJurisdictionUpdated(jurisdiction: String?) {
            
        }
        
        /**
        * Called when the identities is updated.
        */
        override fun onIdentitiesUpdated(identities: String?) {
            
        }
        
        /**
        * Called when the consent is updated.
        */
        override fun onConsentUpdated(consent: Consent) {
            
        }
        
        /**
        * Called on error.
        */
        override fun onError(errMsg: String?) {
            
        }
        
        /**
        * Called when USPrivacy is updated.
        */
        override fun onUSPrivacyUpdated(values: Map<String, Any?>) {
            
        }
        
        /**
        * Called when TCF is updated.
        */
        override fun onTCFUpdated(values: Map<String, Any?>) {
            
        }
        
        /**
        * Called when GPP is updated.
        */
        override fun onGPPUpdated(values: Map<String, Any?>) {
            
        }
   }
```

### 5. Create the Ketch Object:
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
     * @param override url
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
            TEST_URL,
            Ketch.LogLevel.DEBUG
        ).build()
    }
```

### 6. Add advertising loading code and set it in Ketch object and call load() method
```kotlin
    with(ketch) {
        setIdentities(mapOf(ADVERTISING_ID_CODE to advertisingIdCode))
        load()
    }
```

## Developer's Documentations
### com.ketch.android.Ketch
**Ketch** - Main class where the SDK functionality resides.
#### Ketch Methods:
   _load()_
        - loads web content and displays a dialog if necessary

   _getSavedString(key: String)_ 
        - returns a saved TCF/USPrivacy/Gpp strings and other protocol parameters by key

   _getTCFTCString()_ 
        - returns a saved TCF string
   
   _getUSPrivacyString()_
        - returns a saved USPrivacy string

   _getGPPHDRGppString()_ 
        - returns a saved GPP string

   _showConsent()_ 
        - loads web content and forces Consent Dialog (Banner or Modal)
   
   _showPreferences()_
        - loads web content and displays Preferences Dialog

   _showPreferencesTab(tabs: List<PreferencesTab>, tab: PreferencesTab)_
        - displays the preferences tab, adding the fragment dialog to the given FragmentManager

   _dismissDialog() 
        - dismiss the dialog

   _setIdentities(identities: Map<String, String>)_ 
        - set identifies

   _setLanguage(language: String)_
        - set the language. Default value is EN
   
   _setJurisdiction(jurisdiction: String?)_ 
        - set the jurisdiction
   
   _setRegion(region: String?)_ 
        - set the region

### com.ketch.android.KetchSdk
**KetchSdk** - Class used to initialize the Ketch SDK 
#### KetchSdk Methods: 
_create(
    context: Context,
    fragmentManager: FragmentManager,
    organization: String,
    property: String,
    environment: String? = null,
    listener: Ketch.Listener,
    ketchUrl: String? = null,
    logLevel: Ketch.LogLevel
)_ 
      - Creates the Ketch Builder. 
         Parameters:
            context - an Activity Context to access application assets
            fragmentManager - The FragmentManager this KetchDialogFragment will be added to.
            organization - your organization code
            property - the property name
            environment - the environment name.
            listener - Ketch.Listener
            ketchUrl - Overrides url
            logLevel - the log level, can be TRACE, DEBUG, INFO, WARN, ERROR

### com.ketch.android.Ketch.Listener
**Ketch.Listener** - Interface used to list events from the sdk.
#### Listener Methods:
  _onLoad()_ 
        - Called when the page is loaded by the sdk

  _onShow()_
        - Called when a dialog is displayed

  _onDismiss()_
        - Called when a dialog is dismissed

  _onEnvironmentUpdated(environment: String?)_
        - Called when the environment is updated.

  _onRegionInfoUpdated(regionInfo: String?)_
        - Called when the region is updated.

  _onJurisdictionUpdated(jurisdiction: String?)_
        - Called when the jurisdiction is updated.

  _onIdentitiesUpdated(identities: String?)_
        - Called when the identities is updated.

  _onConsentUpdated(consent: Consent)_
        - Called when the consent is updated.

  _onError(errMsg: String?)_
        - Called on error.

  _onUSPrivacyUpdated(values: Map<String, Any?>)_
        - Called when USPrivacy is updated.

  _onTCFUpdated(values: Map<String, Any?>)_
        - Called when TCF is updated.

  _onGPPUpdated(values: Map<String, Any?>)_
        - Called when GPP is updated.

## [the Sample app](https://github.com/ketch-sdk/ketch-samples)

### Prerequisites
- [Android Studio](https://developer.android.com/studio) + follow the setup wizard to install SDK and Emulator

### Step 1. Clone the repository

```
git clone git@github.com:ketch-sdk/ketch-samples.git
git checkout main
cd ketch-android/Android Native SDK Sample
```

### Step 2. Run the app in Android Studio

Open the project directory `Android Native SDK Sample` in the Android Studio.

Add your organization code, property code to
`ketch-android/app/src/main/java/com/ketch/sample/MainActivity.kt`:

```kotlin
private const val ORG_CODE = "????????????????"
private const val PROPERTY = "????????????????"
```

Click Run to build and run the app on the simulator or a device.
