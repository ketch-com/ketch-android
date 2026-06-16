---
name: fix-dependabot-vulnerabilities
description: >-
  Checks for open Dependabot security alerts on ketch-com/ketch-android, applies
  dependency version fixes, runs local builds and tests to verify, then delegates
  to /commit-and-pr to create a PR. Use when asked to "fix dependabot", "patch
  vulnerabilities", "update vulnerable deps", or "fix security alerts".
---

# Fix Dependabot Vulnerabilities

Discover open Dependabot security alerts, patch the vulnerable dependencies, verify locally with builds and tests, and open a PR via `/commit-and-pr`.

## Step 1: Discover Open Vulnerabilities

Run these in parallel:

- Fetch all open Dependabot alerts:

  ```bash
  gh api repos/ketch-com/ketch-android/dependabot/alerts \
    --paginate \
    --jq '.[] | select(.state=="open") | {
      number,
      severity: .security_vulnerability.severity,
      package: .security_vulnerability.package.name,
      ecosystem: .security_vulnerability.package.ecosystem,
      vulnerable_range: .security_vulnerability.vulnerable_version_range,
      patched: .security_vulnerability.first_patched_version.identifier,
      summary: .security_advisory.summary
    }'
  ```

- Check for existing Dependabot PRs to avoid duplicating work:

  ```bash
  gh pr list --repo ketch-com/ketch-android --author app/dependabot --state open
  ```

Build a fix plan from the results. For each alert record: alert number, package name, current version in the repo, patched version, and severity.

If a Dependabot PR already covers an alert, skip that dependency and note it in the PR description later.

**If no open alerts exist**, inform the user that all dependencies are current and stop.

## Step 2: Create a Branch

```bash
git checkout main && git pull origin main
git checkout -b fix/dependabot-vulnerabilities-$(date +%Y%m%d)
```

## Step 3: Apply Fixes

This project uses **Groovy Gradle** (`build.gradle`). Dependency versions live in two places — read the relevant files before editing.

### Centralized versions — root `build.gradle`

Inside `buildscript { }`:

- **`ext.versions`** map — keys: `kotlin`, `ktx`, `appcompat`, `dokka`, `junit`, `gson`, `webkit`
- **`classpath` dependencies** — AGP, kotlin-gradle-plugin, android-junit5, gradle-versions-plugin, ktlint-gradle, dokka-gradle-plugin

### Hardcoded versions — module `build.gradle` files

| Module | File | Dependencies with hardcoded versions |
|--------|------|--------------------------------------|
| ketchsdk | `ketchsdk/build.gradle` | preference, test junit ext, espresso-core |
| integration-tests | `integration-tests/build.gradle` | ConstraintLayout, Material, Lifecycle, Fragment, ActivityKtx, Espresso, UIAutomator |
| sample-app-standard | `sample-app-standard/build.gradle` | Material, ConstraintLayout |
| sample-app-compose | `sample-app-compose/build.gradle` | Compose BOM, Material3, Activity-Compose |

### For each vulnerable dependency:

1. Read the file where its version is defined
2. Update the version string to the **patched version** from the Dependabot alert
3. If the alert recommends a range, use the lowest version that resolves the vulnerability
4. Read back the file to confirm correct syntax

**Skip and flag** any dependency that requires a major version bump with breaking API changes. Note it in the PR description as "requires manual migration."

## Step 4: Ensure Emulator is Running

An emulator **must** be running before verification — no steps may be skipped.

1. Check for a connected device or emulator:

   ```bash
   adb devices | grep -w "device"
   ```

2. If no device is listed, start the emulator in the background and wait for it to boot:

   ```bash
   emulator -avd Pixel_9a -no-window -no-audio &
   adb wait-for-device
   adb shell getprop sys.boot_completed  # poll until this returns "1"
   ```

   Poll `sys.boot_completed` every 5 seconds (up to 120 seconds). If the emulator fails to boot, report the error and stop — do not proceed without a device.

3. Once `adb devices` shows a device in `device` state, continue to Step 5.

## Step 5: Verify Locally

**Every step below is mandatory. No step may be skipped.** Run them sequentially — each must pass before proceeding to the next. If a step fails, analyze whether the failure is caused by the dependency update and attempt to fix it. If unfixable, revert that specific update and flag it.

1. Clean build the SDK:

   ```bash
   ./gradlew clean :ketchsdk:assembleDebug
   ```

2. Unit tests:

   ```bash
   ./gradlew :ketchsdk:test
   ```

3. Lint:

   ```bash
   ./gradlew :ketchsdk:lint
   ```

4. Build sample apps (catches API-breaking changes):

   ```bash
   ./gradlew :sample-app-standard:assembleDebug :sample-app-compose:assembleDebug
   ```

5. Build integration tests:

   ```bash
   ./gradlew :integration-tests:assembleDebug
   ```

6. Run instrumented tests on the emulator:

   ```bash
   ./gradlew :integration-tests:connectedAndroidTest
   ```

Report the pass/fail result of each step to the user before proceeding.

## Step 6: Commit and PR

Invoke the `/commit-and-pr` skill with the scope `deps`. The commit type is `fix`.

All commits on this branch should use the format: `fix(deps): <summary>`.

The PR description should include a "Vulnerabilities resolved" table:

| Alert # | Package | Old Version | New Version | Severity |
|---------|---------|-------------|-------------|----------|

And a "Verification results" section listing pass/fail for each build and test step from Step 5. All steps must show PASS.

If any dependencies were skipped, include a "Skipped" section explaining why.

## Constraints

- Do NOT upgrade dependencies beyond what is needed to fix each vulnerability.
- Do NOT migrate from Groovy Gradle to Kotlin DSL.
- Do NOT refactor any code beyond version string changes.
- Do NOT modify `ext.buildConfig` (minSdk, targetSdk, compileSdk, versionCode, versionName).
- Do NOT introduce a TOML version catalog — preserve the `ext.versions` pattern.
- Never commit directly to main — always use a dedicated branch.
