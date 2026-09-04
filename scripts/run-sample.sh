#!/usr/bin/env bash
# Install, launch, and stream filtered logcat for a Ketch Android sample app.
# Runs under bash so fish/zsh users avoid shell-specific globbing (e.g. *:S).
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

SDK_LOG_TAGS=(
  Ketch:D
  KetchDialogFragment:D
  KetchWebView:D
  KetchSharedPreferences:D
)

usage() {
  cat <<EOF
Usage: $(basename "$0") <standard|compose|integration>

Installs the app, launches MainActivity, and streams SDK + listener logs.

Examples:
  $(basename "$0") standard
  $(basename "$0") compose
  $(basename "$0") integration

Or from the repo root:
  make run-standard
  make run-compose
  make run-integration-test-app
EOF
}

require_command() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "error: '$1' not found. Ensure Android SDK platform-tools are on PATH." >&2
    exit 1
  fi
}

wait_for_device_ready() {
  echo "Waiting for device..."
  adb wait-for-device

  local attempt
  for attempt in $(seq 1 60); do
    if [[ "$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r' || true)" == "1" ]] \
      && adb shell true >/dev/null 2>&1; then
      return 0
    fi
    sleep 1
  done

  cat >&2 <<'EOF'
error: device is connected but adb is not responding.

Try:
  adb kill-server && adb start-server
  # then cold boot the emulator from Android Studio Device Manager
EOF
  exit 1
}

restart_adb() {
  echo "Restarting adb..."
  adb kill-server >/dev/null 2>&1 || true
  adb start-server >/dev/null
  wait_for_device_ready
}

install_app() {
  if ./gradlew "${GRADLE_MODULE}"; then
    return 0
  fi

  echo "Install failed; restarting adb and retrying once..." >&2
  restart_adb
  ./gradlew "${GRADLE_MODULE}"
}

SAMPLE="${1:-}"

case "${SAMPLE}" in
  standard)
    GRADLE_MODULE=":sample-app-standard:installDebug"
    PACKAGE_ID="com.ketch.android.sample.standard"
    LISTENER_TAG="KetchSample:D"
    APP_LABEL="standard sample"
    ;;
  compose)
    GRADLE_MODULE=":sample-app-compose:installDebug"
    PACKAGE_ID="com.ketch.android.sample.compose"
    LISTENER_TAG="KetchCompose:D"
    APP_LABEL="compose sample"
    ;;
  integration)
    GRADLE_MODULE=":integration-tests:installDebug"
    PACKAGE_ID="com.ketch.android.integration.tests"
    LISTENER_TAG="KetchIntegrationTests:D"
    APP_LABEL="integration-tests harness"
    ;;
  -h | --help | help)
    usage
    exit 0
    ;;
  *)
    usage >&2
    exit 1
    ;;
esac

require_command adb

cd "${REPO_ROOT}"

wait_for_device_ready

clear || true

echo "Installing ${APP_LABEL}..."
install_app

echo "Launching ${PACKAGE_ID}..."
adb shell am start -n "${PACKAGE_ID}/.MainActivity" >/dev/null
sleep 1

echo "Streaming logs for ${PACKAGE_ID} (Ctrl+C to stop)..."
adb logcat -c
exec adb logcat \
  "${SDK_LOG_TAGS[@]}" \
  "${LISTENER_TAG}" \
  '*:S'
