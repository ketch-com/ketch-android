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

## Quick Start

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
```
### 4. Add listener and Ketch to your activity:
```kotlin
   private val listener = object : Ketch.Listener {
        
        override fun onLoad() {
            Log.d(TAG, "onLoad")
        }
    
        override fun onEnvironmentUpdated(environment: String?) {
            Log.d(TAG, "onEnvironmentUpdated: environment = $environment")
        }

        override fun onRegionInfoUpdated(regionInfo: String?) {
            Log.d(TAG, "onRegionInfoUpdated: regionInfo = $regionInfo")
        }

        override fun onJurisdictionUpdated(jurisdiction: String?) {
            Log.d(TAG, "onJurisdictionUpdated: jurisdiction = $jurisdiction")
        }

        override fun onIdentitiesUpdated(identities: String?) {
            Log.d(TAG, "onIdentitiesUpdated: identities = $identities")
        }

        override fun onConsentUpdated(consent: Consent) {
            val consentJson = Gson().toJson(consent)
            Log.d(TAG, "onConsentUpdated: consent = $consentJson")
        }

        override fun onError(errMsg: String?) {
            Log.e(TAG, "onError: errMsg = $errMsg")
        }

        override fun onUSPrivacyUpdated(values: Map<String, Any?>) {
            Log.d(TAG, "onUSPrivacyUpdated: $values")
        }

        override fun onTCFUpdated(values: Map<String, Any?>) {
            Log.d(TAG, "onTCFUpdated: $values")
        }

        override fun onGPPUpdated(values: Map<String, Any?>) {
            Log.d(TAG, "onGPPUpdated: $values")
        }
   }

   private val ketch: Ketch by lazy {
        KetchSdk.create(
            this,
            supportFragmentManager,
            ORG_CODE,
            PROPERTY,
            listener,
            url
        ).build()
   }
```

### 5. Add advertising loading code and set it in Ketch object and call load() method
```kotlin
    with(ketch) {
        setIdentities(mapOf(ADVERTISING_ID_CODE to advertisingIdCode))
        load()
    }
```

# Developer's Documentations

SDK includes Ketch, KetchSdk, KetchSharedPreferences, KetchWebView, KetchDialogFragment classes 
Ketch - Is the where the SDK functionality resides. 
KetchSdk - Class used to initialize the Ketch SDK 
KetchSharedPreferences - SharedPreferences class is used for saving the TCF/USPrivacy/GPP strings

## Adding KetchSDK in your project

## Ketch methods:
1. load() - loads web content and displays a dialog if necessary
2. getSavedString(key: String) - returns a saved TCF/USPrivacy/Gpp strings and other protocol parameters by key
3. getTCFTCString() - returns a saved TCF string
4. getUSPrivacyString() - returns a saved USPrivacy string
5. getGPPHDRGppString() - returns a saved GPP string
6. forceShowConsent() - loads web content and forces Consent Dialog (Banner or Modal) 
7. showPreferences() - loads web content and displays Preferences Dialog
8. showPreferencesTab(tabs: List<PreferencesTab>, tab: PreferencesTab) - displays the preferences tab, adding the fragment dialog to the given FragmentManager
9. dismissDialog() - dismiss the dialog
10. setIdentities(identities: Map<String, String>) - set identifies
11. setLanguage(language: String) - set the language
12. setJurisdiction(jurisdiction: String?) - set the jurisdiction
13. setRegion(region: String?) - set Region


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
