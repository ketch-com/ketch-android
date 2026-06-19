# Ketch SDK Integration Tests

This module contains integration tests for the Ketch Android SDK. The tests validate SDK functionality in a real Android environment.

**ATT is N/A on Android** — App Tracking Transparency is iOS-only. For ATT testing see [mobile-att-testing.md](../../ketch-tag/docs/design/mobile-att-testing.md#ketch-android). For headless CDN tests see [mobile-headless-api-testing.md](../../ketch-tag/docs/design/mobile-headless-api-testing.md#ketch-android).

## Overview

The integration tests consist of:

- **Sample App**: A minimal Android application that uses the Ketch SDK
- **Instrumented Tests**: Espresso-based UI tests that validate SDK functionality

## Structure

```
integration-tests/
├── src/
│   ├── main/                    # Sample app code
│   │   ├── java/               # MainActivity and related classes
│   │   ├── res/                # App resources (layouts, themes, etc.)
│   │   └── AndroidManifest.xml # App manifest
│   └── androidTest/            # Integration tests
│       └── java/               # Test classes
├── build.gradle                # Module build configuration
└── README.md                   # This file
```

## Running the Tests

### Prerequisites

- Android Studio or Android SDK command line tools
- An Android device or emulator running API 28 or higher
- The Ketch SDK module (`ketchsdk`) must be built successfully

### Headless integration tests

Live CDN round-trip (no WebView): `KetchHeadlessIntegrationTest` uses org `ketch_samples` / property `android`.

```bash
./gradlew :integration-tests:connectedAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.ketch.android.integration.tests.KetchHeadlessIntegrationTest
```

### WebView integration tests

```bash
# From the ketch-android directory
./gradlew :integration-tests:connectedAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.ketch.android.integration.tests.KetchSdkIntegrationTest
```

## Running Tests Locally

#### From Android Studio

1. Open the `ketch-android` project in Android Studio
2. Connect an Android device or start an emulator
3. Navigate to `integration-tests/src/androidTest/java/com/ketch/android/integration/tests/`
4. Right-click on the desired test class and select Run

#### From Command Line

From the repo root (works in bash, zsh, fish, etc.):

```bash
make test-integration
```

Other targets:

```bash
make run-integration-test-app       # install, launch, and stream harness app logs
make test-integration-class CLASS=com.ketch.android.integration.tests.ZAutoDismissOnNavigationTest
make help
```

Or with gradlew directly:

```bash
# All integration tests
./gradlew :integration-tests:connectedAndroidTest
```

Reports: `integration-tests/build/reports/androidTests/connected/`.

## Test Coverage

| Suite | Class | What it validates |
| ----- | ----- | ----------------- |
| **Headless CDN** | `KetchHeadlessIntegrationTest` | `fetchLocation`, bootstrap, full config, consent get/set |
| **WebView** | `KetchSdkIntegrationTest` | SDK init, UI buttons, WebView experience load |

The current test suite covers:

- **SDK Initialization**: Verifies the SDK initializes correctly
- **UI Interactions**: Tests all buttons and their status updates
- **Method Calls**: Validates that SDK methods are called without errors
- **State Management**: Checks that the app displays initial state correctly
- **Cross-Activity Display**: Verifies Ketch initialized in Application/MainActivity can show experiences from a different Activity, including WebView content validation (`ketch-consent-banner`, `ketch-preferences`)
- **Auto-Dismiss on Navigation**: Verifies that navigating to a different Activity while an experience is showing automatically dismisses the orphaned experience (no integrator call needed), reports `onDismiss(HideExperienceStatus.ActivityChanged)`, and leaves the new Activity able to show experiences
- **Benign Lifecycle (No Dismiss)**: Verifies rotation and backgrounding do NOT auto-dismiss an active experience or kill the WebView

## Sample App Features

The sample app includes:

- Buttons to trigger all major SDK methods (`load`, `showConsent`, `showPreferences`, etc.)
- **Open Second Activity** button to demonstrate cross-activity display with a shared Ketch instance
- Status display for SDK state and privacy framework values (TCF, US Privacy, GPP)
- Real-time updates from SDK callbacks

## Configuration

The sample app uses test configuration values:

- **Organization Code**: `test_org_code`
- **Property**: `test_property`
- **Environment**: `test`

To test with real values, update the constants in `MainActivity.kt`.

## Adding New Tests

1. Create test methods in the appropriate `*IntegrationTest.kt` class
2. Use Espresso matchers and assertions
3. Follow existing test patterns
4. Keep headless tests in `KetchHeadlessIntegrationTest`; WebView tests in `KetchSdkIntegrationTest`

## Troubleshooting

| Symptom | Likely cause |
| ------- | ------------ |
| Tests fail with "No activities found" | Sample app did not build; emulator not running |
| SDK initialization errors | Invalid test configuration; missing SDK dependency |
| UI tests fail intermittently | Missing wait for async WebView load |

## Dependencies

- **Espresso** — UI testing
- **AndroidX Test** — test infrastructure
- **JUnit 4** — test framework

All dependencies are managed through `build.gradle`.
