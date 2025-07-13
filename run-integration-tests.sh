#!/bin/bash

# Ketch SDK Integration Tests Runner
# This script runs the integration tests for the Ketch Android SDK

set -e

echo "🚀 Starting Ketch SDK Integration Tests..."

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${GREEN}ℹ️  $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
}

# Check if Android SDK is available
if ! command -v adb &> /dev/null; then
    print_error "adb command not found. Please ensure Android SDK is installed and in PATH."
    exit 1
fi

# Check if devices are available
DEVICES=$(adb devices | grep -v "List of devices attached" | grep -E "device|emulator" | wc -l)
if [ "$DEVICES" -eq 0 ]; then
    print_error "No Android devices or emulators found. Please connect a device or start an emulator."
    exit 1
fi

print_status "Found $DEVICES device(s) connected"

# Clean and build the project
print_status "Cleaning and building project..."
./gradlew clean

# Build the SDK module first
print_status "Building Ketch SDK module..."
./gradlew :ketchsdk:build

# Build the integration tests
print_status "Building integration tests..."
./gradlew :integration-tests:build

# Run the integration tests
print_status "Running integration tests..."
./gradlew :integration-tests:connectedAndroidTest

# Check if tests passed
if [ $? -eq 0 ]; then
    print_status "✅ All integration tests passed!"
else
    print_error "❌ Integration tests failed!"
    exit 1
fi

# Optional: Generate test report
print_status "Generating test report..."
./gradlew :integration-tests:connectedAndroidTest --info

echo ""
print_status "🎉 Integration tests completed successfully!"
print_status "Test reports can be found in: integration-tests/build/reports/androidTests/connected/"

echo ""
echo "You can also run individual test commands:"
echo "  - Install sample app: ./gradlew :integration-tests:installDebug"
echo "  - Run specific test: ./gradlew :integration-tests:connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.ketch.android.integration.tests.KetchSdkIntegrationTest#testSpecificMethod" 