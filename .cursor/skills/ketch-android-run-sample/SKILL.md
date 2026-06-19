---
name: ketch-android-run-sample
description: Configures the in-repo Android sample apps (Compose or standard) for either the published com.ketch.android:ketchsdk Maven artifact or the local :ketchsdk module, boots an emulator when needed, builds, installs, launches, and streams filtered logcat. Use when the user runs /ketch-android-run-sample or wants manual SDK Health Dashboard QA on Android.
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

## Manual testing basics

**Needs:** Android Studio or SDK, emulator or USB device, `adb` on PATH, network for CDN/headless steps.

**Launch:** `bash .cursor/skills/ketch-android-run-sample/scripts/run-sample-app.sh compose local` from the `ketch-android` repo root.

**In the app:** **SDK Health Dashboard** is the first section in the scroll view. Connection row uses org `ethansch061226`, property `website_smart_tag`, environment `production`. Use dashboard panels and event log — logcat from the script is optional for smoke QA.

**Smoke flow:** **Load** → privacy rows update → **Show Consent** → visibility/dismiss rows change → **Fetch Bootstrap** under Headless. ATT does not apply on Android.

## Other options

```bash
DEVICE_ID=emulator-5554 bash .cursor/skills/ketch-android-run-sample/scripts/run-sample-app.sh compose local
AVD_NAME="Pixel_7_API_34" bash .cursor/skills/ketch-android-run-sample/scripts/run-sample-app.sh standard local
KETCH_ANDROID_SDK_VERSION=3.0 bash .cursor/skills/ketch-android-run-sample/scripts/run-sample-app.sh compose
bash .cursor/skills/ketch-android-run-sample/scripts/run-sample-app.sh compose local --build-only
bash .cursor/skills/ketch-android-run-sample/scripts/run-sample-app.sh standard local --full-system-logs
```

## Manual QA checklist (SDK Health Dashboard)

Verify on-screen panels (no logcat required):

1. **Connection** — Init/status; connection row shows `ethansch061226 / website_smart_tag / production`.
2. **Load** — Tap **Load** → Load row `loading`; environment/consent fields update from callbacks.
3. **WebView / Experience** — **Show Consent** → visibility/dismiss rows change (ATT N/A on Android).
4. **Privacy / Consent** — Environment, jurisdiction, region, consent, US Privacy / TCF / GPP populate after load.
5. **Headless** — **Fetch Location**, **Fetch Bootstrap**, **Cold Start** show inline OK/Error.
6. **Event log** — Timestamped trace (~50 lines max).

## Notes

- `integration-tests` is the Espresso automation host; use **compose** or **standard** samples for manual QA.
- Headless steps require network access to the live Ketch CDN.
- Remote mode needs the pinned Maven artifact to be resolvable from your configured repositories.
