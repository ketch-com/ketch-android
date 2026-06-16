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

```bash
# All integration tests
./gradlew :integration-tests:connectedAndroidTest
```

### Running the Sample App

```bash
./gradlew :integration-tests:installDebug
```

## Test Coverage

| Suite | Class | What it validates |
| ----- | ----- | ----------------- |
| **Headless CDN** | `KetchHeadlessIntegrationTest` | `fetchLocation`, bootstrap, full config, consent get/set |
| **WebView** | `KetchSdkIntegrationTest` | SDK init, UI buttons, WebView experience load |

## Sample App Features

The sample app includes:

- Buttons to trigger SDK methods (`load`, `showConsent`, `showPreferences`, etc.)
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
