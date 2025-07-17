# Ketch SDK Integration Tests

This module contains integration tests for the Ketch Android SDK. The tests are designed to validate the SDK's functionality in a real Android environment.

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

### Running Tests Locally

#### From Android Studio

1. Open the `ketch-android` project in Android Studio
2. Connect an Android device or start an emulator
3. Navigate to `integration-tests/src/androidTest/java/com/ketch/android/integration/tests/`
4. Right-click on `KetchSdkIntegrationTest` and select "Run 'KetchSdkIntegrationTest'"

#### From Command Line

```bash
# From the ketch-android directory
./gradlew :integration-tests:connectedAndroidTest
```

### Running the Sample App

You can also run the sample app directly to manually test the SDK:

```bash
./gradlew :integration-tests:installDebug
```

## Test Coverage

The current test suite covers:

- **SDK Initialization**: Verifies the SDK initializes correctly
- **UI Interactions**: Tests all buttons and their status updates
- **Method Calls**: Validates that SDK methods are called without errors
- **State Management**: Checks that the app displays initial state correctly

## Sample App Features

The sample app includes:

- Buttons to trigger all major SDK methods (`load`, `showConsent`, `showPreferences`, etc.)
- Status display to show current SDK state
- Real-time updates from SDK callbacks
- Display of privacy framework values (TCF, US Privacy, GPP)

## Configuration

The sample app uses test configuration values:

- **Organization Code**: `test_org_code`
- **Property**: `test_property`
- **Environment**: `test`

To test with real values, update the constants in `MainActivity.kt`:

```kotlin
private const val ORG_CODE = "your_real_org_code"
private const val PROPERTY = "your_real_property"
private const val ENVIRONMENT = "production"
```

## Adding New Tests

To add new integration tests:

1. Create test methods in `KetchSdkIntegrationTest.kt`
2. Use Espresso matchers and assertions
3. Follow the existing test patterns
4. Add any necessary helper methods

Example test structure:

```kotlin
@Test
fun testNewFeature() {
    // Arrange
    // Set up test conditions

    // Act
    // Perform actions (button clicks, etc.)

    // Assert
    // Verify expected outcomes
}
```

## Future Enhancements

Planned improvements:

- [ ] Tests for dialog display and interaction
- [ ] Network request mocking for controlled testing
- [ ] Performance testing
- [ ] Accessibility testing
- [ ] Tests for different Android versions and devices
- [ ] Integration with CI/CD pipelines

## Troubleshooting

Common issues and solutions:

**Tests fail with "No activities found"**

- Ensure the sample app builds successfully
- Check that the device/emulator is running

**SDK initialization errors**

- Verify the SDK module is included in dependencies
- Check that test configuration values are valid

**UI tests fail intermittently**

- Add appropriate wait conditions for async operations
- Ensure tests are running on a clean app state

## Dependencies

The integration tests use:

- **Espresso**: For UI testing
- **AndroidX Test**: For test infrastructure
- **JUnit 4**: For test framework
- **Awaitility**: For async test conditions (if needed)

All dependencies are automatically managed through the `build.gradle` file.
