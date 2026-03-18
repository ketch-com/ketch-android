# Ketch Android SDK — Sample App

A minimal sample app demonstrating Ketch SDK integration with an `AppCompatActivity`.

## Running

```bash
./gradlew :sample-app-standard:installDebug
adb shell am start -n com.ketch.android.sample.standard/.MainActivity
```

Or open the project in Android Studio and run the `sample-app-standard` configuration.

## Configuration

Edit `MainActivity.kt` — update the companion object constants with your Ketch account details:

```kotlin
companion object {
    private const val ORG_CODE = "your_organization_code"
    private const val PROPERTY = "your_property"
    private const val ENVIRONMENT = "production"
}
```

Set your identity key and value in `initializeKetch()`:

```kotlin
ketch.setIdentities(mapOf("your_identity_key" to "your_identity_value"))
```

Find your organization code at [app.ketch.com/settings/organization](https://app.ketch.com/settings/organization) and property code at [app.ketch.com/deployment/applications](https://app.ketch.com/deployment/applications).
