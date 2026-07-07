#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<'USAGE'
Usage: run-sample-app.sh <compose|standard> [local] [--build-only] [--no-logs] [--full-system-logs]

  compose|standard     Required. Which sample app module to build and launch.
  (no local)           Use the published com.ketch.android:ketchsdk Maven artifact.
  local                Use the in-repo :ketchsdk project (default for day-to-day SDK work).

Options:
  --build-only         Build and install the APK; do not launch or stream logs.
  --no-logs            Alias for --build-only.
  --full-system-logs   Stream unfiltered logcat for the sample process.

Environment:
  DEVICE_ID            adb device serial (default: first connected emulator/device).
  AVD_NAME             Emulator AVD to boot when no device is connected.
  KETCH_ANDROID_SDK_VERSION
                        Pin remote Maven version (default: 3.0).

Builds :sample-app-compose or :sample-app-standard, installs via adb, launches
MainActivity, and streams filtered logcat (Ketch SDK + sample tags) unless --build-only.

For the plain local-source case (no Maven toggle needed), scripts/run-sample.sh at the
repo root (or `make run-standard` / `make run-compose` / `make run-integration-test-app`)
is the simpler entry point.
USAGE
}

if [[ $# -lt 1 ]]; then
  usage >&2
  exit 2
fi

SAMPLE_VARIANT="$1"
shift

case "$SAMPLE_VARIANT" in
  compose|standard)
    GRADLE_MODULE=":sample-app-${SAMPLE_VARIANT}"
    PACKAGE_ID="com.ketch.android.sample.${SAMPLE_VARIANT}"
    SAMPLE_TAG="KetchCompose"
    if [[ "$SAMPLE_VARIANT" == "standard" ]]; then
      SAMPLE_TAG="KetchSample"
    fi
    ;;
  -h|--help)
    usage
    exit 0
    ;;
  *)
    echo "First argument must be compose or standard, got: $SAMPLE_VARIANT" >&2
    usage >&2
    exit 2
    ;;
esac

PACKAGE_MODE="remote"
if [[ "${1:-}" == "local" ]]; then
  PACKAGE_MODE="local"
  shift
fi

build_only=0
stream_logs=1
full_system_logs=0

while [[ $# -gt 0 ]]; do
  case "$1" in
    --build-only|--no-logs)
      build_only=1
      stream_logs=0
      shift
      ;;
    --full-system-logs)
      full_system_logs=1
      shift
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "Unknown argument: $1" >&2
      usage >&2
      exit 2
      ;;
  esac
done

require_command() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "Required command not found: $1" >&2
    exit 1
  fi
}

require_command adb
require_command python3

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../../../.." && pwd)"
GRADLEW="$REPO_ROOT/gradlew"
APK_PATH="$REPO_ROOT/sample-app-${SAMPLE_VARIANT}/build/outputs/apk/debug/sample-app-${SAMPLE_VARIANT}-debug.apk"

if [[ ! -x "$GRADLEW" ]]; then
  echo "Gradle wrapper not found: $GRADLEW" >&2
  exit 1
fi

python3 "$SCRIPT_DIR/configure-sample-dependency.py" "$PACKAGE_MODE" "$REPO_ROOT"

adb_device_ready() {
  adb devices 2>/dev/null | awk 'NR > 1 && $2 == "device" { print $1; exit }'
}

boot_emulator_if_needed() {
  local serial
  serial="$(adb_device_ready)"
  if [[ -n "$serial" ]]; then
    echo "$serial"
    return
  fi

  if ! command -v emulator >/dev/null 2>&1; then
    echo "No adb device connected and emulator command not on PATH." >&2
    echo "Start an Android emulator in Android Studio, or connect a device." >&2
    exit 1
  fi

  local avd="${AVD_NAME:-}"
  if [[ -z "$avd" ]]; then
    avd="$(emulator -list-avds 2>/dev/null | head -n 1 || true)"
  fi
  if [[ -z "$avd" ]]; then
    echo "No AVD found. Create an emulator in Android Studio or set AVD_NAME." >&2
    exit 1
  fi

  echo "No device connected. Booting emulator AVD: $avd"
  emulator -avd "$avd" -no-snapshot-load >/dev/null 2>&1 &
  adb wait-for-device
  # Wait until boot completes
  adb shell 'while [[ -z $(getprop sys.boot_completed) ]]; do sleep 1; done' 2>/dev/null || sleep 15
  adb_device_ready
}

DEVICE_ID="${DEVICE_ID:-}"
if [[ -z "$DEVICE_ID" ]]; then
  DEVICE_ID="$(boot_emulator_if_needed)"
fi

echo "Using device: $DEVICE_ID"
adb -s "$DEVICE_ID" devices

if [[ "$PACKAGE_MODE" == "local" ]]; then
  echo "Building $GRADLE_MODULE using local :ketchsdk at $REPO_ROOT"
else
  echo "Building $GRADLE_MODULE using Maven com.ketch.android:ketchsdk (${KETCH_ANDROID_SDK_VERSION:-3.0})"
fi

(cd "$REPO_ROOT" && ./gradlew "${GRADLE_MODULE}:assembleDebug" --quiet)

if [[ ! -f "$APK_PATH" ]]; then
  echo "Built APK not found: $APK_PATH" >&2
  exit 1
fi

echo "Installing $APK_PATH"
adb -s "$DEVICE_ID" install -r "$APK_PATH" >/dev/null

if [[ "$build_only" -eq 1 ]]; then
  echo "Build/install complete (--build-only)."
  exit 0
fi

echo "Launching $PACKAGE_ID/.MainActivity"
adb -s "$DEVICE_ID" shell am force-stop "$PACKAGE_ID" >/dev/null 2>&1 || true
adb -s "$DEVICE_ID" shell am start -n "$PACKAGE_ID/.MainActivity" >/dev/null

log_stream_pid=""
cleanup() {
  if [[ -n "$log_stream_pid" ]] && kill -0 "$log_stream_pid" >/dev/null 2>&1; then
    kill "$log_stream_pid" >/dev/null 2>&1 || true
  fi
}
trap cleanup EXIT INT TERM

if [[ "$stream_logs" -eq 1 ]]; then
  if [[ "$full_system_logs" -eq 1 ]]; then
    echo "Streaming logcat for $PACKAGE_ID (full). Press Ctrl-C to stop."
    adb -s "$DEVICE_ID" logcat --pid="$(adb -s "$DEVICE_ID" shell pidof -s "$PACKAGE_ID" 2>/dev/null || true)" &
  else
    echo "Streaming logcat (Ketch, $SAMPLE_TAG). Press Ctrl-C to stop."
    adb -s "$DEVICE_ID" logcat -v brief Ketch:D "$SAMPLE_TAG":D '*:S' &
  fi
  log_stream_pid="$!"
  wait "$log_stream_pid" || true
else
  echo "Sample launched. Info panel is at the top of the app scroll view."
fi
