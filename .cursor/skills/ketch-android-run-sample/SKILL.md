---
name: ketch-android-run-sample
description: Configures the in-repo Android sample apps (Compose or standard) for either the published com.ketch.android:ketchsdk Maven artifact or the local :ketchsdk module, boots an emulator when needed, builds, installs, launches, and streams filtered logcat. Use when the user runs /ketch-android-run-sample or wants manual sample-app QA on Android.
---

# ketch-android-run-sample

## Instructions

When the user invokes **`/ketch-android-run-sample`** (or asks to run the Android sample with production / local SDK):

1. `cd` to the `ketch-android` repository root.

2. Run the helper script with a **required** sample variant:

**Local SDK (default for SDK development)** — sample `build.gradle` uses `project(':ketchsdk')`:

```bash
bash .cursor/skills/ketch-android-run-sample/scripts/run-sample-app.sh compose local
bash .cursor/skills/ketch-android-run-sample/scripts/run-sample-app.sh standard local
```

**Published Maven artifact** — sample depends on `com.ketch.android:ketchsdk` (version from `KETCH_ANDROID_SDK_VERSION` or `3.0`):

```bash
bash .cursor/skills/ketch-android-run-sample/scripts/run-sample-app.sh compose
bash .cursor/skills/ketch-android-run-sample/scripts/run-sample-app.sh standard
```

The script boots an emulator when no `adb` device is connected, runs `./gradlew :sample-app-*:assembleDebug`, installs the APK, launches `MainActivity`, and streams filtered logcat (`Ketch`, sample tag). Stop with `Ctrl-C`.

For plain local-source runs (no Maven toggle needed), `scripts/run-sample.sh` at the repo root
(or `make run-standard` / `make run-compose` / `make run-integration-test-app`) is the simpler,
non-agent entry point — see `.cursor/rules/development-workflow.mdc`.

## Manual testing basics

**Needs:** Android Studio or SDK, emulator or USB device, `adb` on PATH, network for CDN.

**Launch:** `bash .cursor/skills/ketch-android-run-sample/scripts/run-sample-app.sh compose local` from the `ketch-android` repo root.

**In the app:** the **Info** section is at the top of the scroll view — org `ketch_samples`, property `android`, environment `production`, language `en`, plus live Jurisdiction/Region populated after `Load`.

**Smoke flow:** **Load** (Actions) → Jurisdiction/Region populate in Info → **Consent** (Actions cards row) shows the consent banner → **Preferences** opens the Privacy Center honoring the checked tabs / initial tab from Preference Options.

## Other options

```bash
DEVICE_ID=emulator-5554 bash .cursor/skills/ketch-android-run-sample/scripts/run-sample-app.sh compose local
AVD_NAME="Pixel_7_API_34" bash .cursor/skills/ketch-android-run-sample/scripts/run-sample-app.sh standard local
KETCH_ANDROID_SDK_VERSION=3.0 bash .cursor/skills/ketch-android-run-sample/scripts/run-sample-app.sh compose
bash .cursor/skills/ketch-android-run-sample/scripts/run-sample-app.sh compose local --build-only
bash .cursor/skills/ketch-android-run-sample/scripts/run-sample-app.sh standard local --full-system-logs
```

## Manual QA checklist

Verify on-screen panels (no logcat required):

1. **Info** — Org Code/Property/Environment/Language show `ketch_samples`/`android`/`production`/`en`.
2. **Preference Options** — toggling allowed-tab checkboxes and the initial-tab radio changes what
   **Preferences** opens to.
3. **Actions** — **Load** (Info's Jurisdiction/Region populate from callbacks), **Consent** (banner
   shows), **Preferences** (Privacy Center honors tab selection), **Reload**, **Apply CSS**.
4. **Privacy Strings** — **Log Values** dumps IAB TCF/US-Privacy/GPP strings persisted by the SDK.
5. **Event Log** — timestamped callback trace (jurisdiction/region/consent/error/etc).

## Notes

- `integration-tests` is the Espresso automation host; use **compose** or **standard** samples for
  manual QA.
- Remote mode needs the pinned Maven artifact to be resolvable from your configured repositories.
- To point the SDK's tag script at a local dev server instead of the CDN, flip
  `DevUrlOverrides.ENABLED` in the sample's `DevUrlOverrides.kt`.
