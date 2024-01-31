# Ketch Mobile SDK for Android

The Ketch Mobile SDK allows to manage and collect a visitor's consent preferences for an organization on the mobile platforms.

## Requirements

Minimum Android API version supported is 31

The use of the Mobile SDK requires an [Ketch organization account](https://app.ketch.com/settings/organization)
with the [application property](https://app.ketch.com/deployment/applications)  configured.

## Quick Start

1. Copy and paste websdk module to your project
2. Add "include ':websdk'" to settings.graddle
3. Add dependency into your main module:
```kotlin
   "implementation project(':websdk')"
```
4. Add constants to companion object of your activity
```kotlin
        private const val ORG_CODE = "<your organization code>"
        private const val PROPERTY = "<property>"
        private const val ADVERTISING_ID_CODE = "aaid"
```
4. Add listener and Ketch to your activity:
```kotlin
   private val listener = object : Ketch.Listener {

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
            listener
        ).build()
   }
```
5. Add advertising loading code and set it in Ketch object and call load() method
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

## Adding WebSDK in your project

## Ketch methods:
1. load() - loads web content and displays a dialog if necessary
1. getSavedString(key: String) - returns a saved TCF/USPrivacy/Gpp strings and other protocol parameters by key
2. getTCFTCString() - returns a saved TCF string
3. getUSPrivacyString() - returns a saved USPrivacy string
4. getGPPHDRGppString() - returns a saved GPP string
5. forceShowConsent() - loads web content and forces Consent Dialog (Banner or Modal) 
6. showPreferences() - loads web content and displays Preferences Dialog

## Dialog Position and animation
Ketch automatically uses the dialog position and animation from your configuration.
If you want to use a different position, you can set it using these methods:
```kotlin
   setBannerWindowPosition(position: WindowPosition?)
   setModalWindowPosition(position: WindowPosition?)
```

## Dialog Sizes resources:
```xml
<resources>
   ...
    <style name="KetchBannerTopBottom">
        <item name="android:layout_width">match_parent</item>
        <item name="android:layout_height">620dp</item>
    </style>
    <style name="KetchBannerLeftRightCenter">
        <item name="android:layout_width">match_parent</item>
        <item name="android:layout_height">620dp</item>
    </style>

    <style name="KetchModalTopBottom">
        <item name="android:layout_width">match_parent</item>
        <item name="android:layout_height">720dp</item>
    </style>
    <style name="KetchModalLeftRightCenter">
        <item name="android:layout_width">match_parent</item>
        <item name="android:layout_height">720dp</item>
    </style>

</resources>
```
## Dialog Animations resources:
fade_in_center.xml
slide_from_bottom.xml
slide_from_left.xml
slide_from_right.xml
slide_from_top.xml

```xml
<?xml version="1.0" encoding="utf-8"?>
<set xmlns:android="http://schemas.android.com/apk/res/android">
    <alpha
        android:duration="600"
        android:fromAlpha="0.0"
        android:startOffset="500"
        android:toAlpha="1.0" />
</set>
```

## Running the Sample app

### Prerequisites
- [Android Studio](https://developer.android.com/studio) + follow the setup wizard to install SDK and Emulator

### Step 1. Clone the repository

```
git clone git@github.com:ketch-com/ketch-android.git
cd ketch-android
git checkout sdk-3
```

### Step 2. Run the app in Android Studio

Open the project directory `ketch-android` in the Android Studio.

Add your organization code, property code to
`ketch-android/app/src/main/java/com/ketch/sample/MainActivity.kt`:

```kotlin
private const val ORG_CODE = "????????????????"
private const val PROPERTY = "????????????????"
```

Click Run to build and run the app on the simulator or a device.
