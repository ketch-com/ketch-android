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

get_app_uid() {
  local package_id="$1"
  local uid=""

  uid="$(
    adb shell pm list packages -U "${package_id}" 2>/dev/null \
      | tr -d '\r' \
      | sed -n 's/^package:.* uid:\([0-9]*\).*$/\1/p' \
      | head -1
  )"

  if [[ -z "${uid}" ]]; then
    echo "error: could not resolve UID for ${package_id}. Is the app installed?" >&2
    exit 1
  fi

  echo "${uid}"
}

wait_for_pid() {
  local package_id="$1"
  local pid=""

  for _ in $(seq 1 20); do
    pid="$(adb shell pidof -s "${package_id}" 2>/dev/null | tr -d '\r' || true)"
    if [[ -n "${pid}" ]]; then
      echo "${pid}"
      return 0
    fi
    sleep 0.25
  done

  echo "error: could not find process for ${package_id}. Is the app running?" >&2
  exit 1
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

if ! adb get-state >/dev/null 2>&1; then
  echo "error: no Android device or emulator connected." >&2
  exit 1
fi

clear

echo "Installing ${APP_LABEL}..."
./gradlew "${GRADLE_MODULE}"

echo "Launching ${PACKAGE_ID}..."
adb shell am start -n "${PACKAGE_ID}/.MainActivity" >/dev/null

wait_for_pid "${PACKAGE_ID}" >/dev/null

APP_UID="$(get_app_uid "${PACKAGE_ID}")"
echo "Streaming logs for ${PACKAGE_ID} (uid ${APP_UID}; Ctrl+C to stop)..."

adb logcat -c
adb logcat --uid="${APP_UID}" \
  "${SDK_LOG_TAGS[@]}" \
  "${LISTENER_TAG}" \
  '*:S'
