# Ketch Android SDK — Jetpack Compose Sample App

A sample app demonstrating Ketch SDK integration in a Jetpack Compose application using Material 3 and `AppCompatActivity`.

## Running

From the repo root (works in bash, zsh, fish, etc.):

```bash
make run-compose
```

Clears the terminal, installs the app, launches it, and streams logcat filtered to Ketch SDK tags plus `KetchCompose`.

Or open the project in Android Studio and run the `sample-app-compose` configuration.

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

## Notes

The activity extends `AppCompatActivity`, which provides the `supportFragmentManager` required by the SDK. `AppCompatActivity` is fully compatible with Compose's `setContent {}` — no XML layouts are used.
