# ketch
Mobile SDK for Android

SDK includes core, ccpa, tcf and core-ui modules 
Core - Base SDK module. It icludes all necessary request to work with our backend
CCPA and TCF - Specific protocol plugins. 
Core UI - UI Module. It includes all predefined dialogs for users.

## Build SDK
1. Add your jfrog credential (ARTIFACTORY_USERNAME and ARTIFACTORY_PASSWORD) and sdk version (GITHUB_RUN_NUMBER) to environment values
2. Build Core ./gradlew core:build
3. Build CCPA Plugin ./gradlew ccpa:build
4. Build TCF Plugin ./gradlew tcf:build
5. Build Core UI ./gradlew core-ui:build
6. Publish .aar libraries to jfrog ./gradlew artifactoryPublish

## Core Module
KetchSdk - Factory to create the Ketch singleton. It creates `KetchApi` using `retrofit` and `KetchApi::class.java`, 
`Repository` and usecases (`OrganizationConfigUseCase`, `ConsentUseCase`, `RightsUseCase`)

Ketch - Main Ketch SDK class. This class includes all methods to work with the backend. It using `config` and `consent` StateFlow calls
`configLoaded` and `consentChanged` methods in all attached plugins.

KetchApi - describes all API requests.

com.ketch.android.api.request package - includes API requests
com.ketch.android.api.response package - includes API responses

com.ketch.android.api.usecase package - includes `OrganizationConfigUseCase`, `ConsentUseCase`, `RightsUseCase`

com.ketch.android.plugin.Plugin - base class for plugins. If you want to create your plugin you should extend this abstract class:
```kotlin
class CustomPlugin(listener: (encodedString: String?, applied: Boolean) -> Unit) : Plugin(listener) {
    
    override fun isApplied(): Boolean = 
        configuration?.regulations?.contains(REGULATION) == true

    override fun consentChanged(consent: Consent) {
        ...
        listener.invoke(encodedString, applied)
    }

    override fun hashCode(): Int {
        return REGULATION.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return REGULATION.equals(other)
    }

    companion object {
        private const val REGULATION = "<your regulation>"
    }
}

```

## CCPA module
CCPA Plugin. It extends Plugin class and can encode CCPA string from `config` and `consent`

## TCF module
TCF Plugin. It extends Plugin class and can encode TCF string from `config` and `consent`

## Core UI Module
KetchUI - main class of core-ui. It contains methods to show `Banner`, `Modal`, `Just In Time` and `Preferences` Popups.
com.ketch.android.ui.dialog package includes all dialog ui implementations.
com.ketch.android.ui.adapter package contains adapters for used lists (`DataCategory`, `Purposes`, `Rights` and `Vendors`).
com.ketch.android.ui.view package contains `DataCategoriesView`, `PurposesView` and `VendorsView`
com.ketch.android.ui.extension.ThemeBindingAdapter.kt includes binding adapters to work with the color theme
```kotlin
@ColorInt
private fun getColor(hex: String): Int = try {
    Color.parseColor(hex)
} catch (ex: IllegalArgumentException) {
    android.R.color.transparent
}

@BindingAdapter("backgroundColor")
fun setBackgroundColor(view: View, color: String?) {
    color?.let {
        view.setBackgroundColor(getColor(it))
    }
}
...
```

```xml
<?xml version="1.0" encoding="utf-8"?>
<layout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools">

    <data>

        <variable
            name="theme"
            type="com.ketch.android.ui.theme.ColorTheme" />
    </data>

    <androidx.constraintlayout.widget.ConstraintLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:paddingHorizontal="18dp"
        app:backgroundColor="@{theme.bodyBackgroundColor}">
        
        ...
```

