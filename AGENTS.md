# AGENTS.md

## Cursor Cloud specific instructions

This is the **Ketch Android SDK** — a Gradle multi-module Android library project (not a monorepo).

### Modules

| Module | Type | Purpose |
|---|---|---|
| `ketchsdk` | Android Library | Core SDK (the publishable artifact) |
| `integration-tests` | Android App | Instrumented tests (require emulator/device) |
| `sample-app-standard` | Android App | Demo using Android Views + ViewBinding |
| `sample-app-compose` | Android App | Demo using Jetpack Compose + Material 3 |

### Prerequisites

- **JDK 17** (`openjdk-17-jdk-headless`) — required by Gradle and AGP 9.x
- **Android SDK** with platform `android-36`, `build-tools;36.0.0`, and `platform-tools`
- Environment variables: `JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64`, `ANDROID_HOME=/opt/android-sdk`

### Key commands

See `build.gradle` and per-module `build.gradle` files for full configuration. Quick reference:

| Task | Command |
|---|---|
| Build all modules (debug) | `./gradlew assembleDebug` |
| Build SDK only | `./gradlew :ketchsdk:assembleDebug` |
| Unit tests | `./gradlew :ketchsdk:test` |
| Ktlint | `./gradlew ktlintCheck` |
| Android Lint | `./gradlew :ketchsdk:lintDebug` |
| Instrumented tests | `./gradlew :integration-tests:connectedAndroidTest` (requires emulator) |
| Clean | `./gradlew clean` |

### Gotchas

- The default JDK on the VM is JDK 21. You **must** set `JAVA_HOME` to the JDK 17 path, otherwise Gradle will fail with compatibility errors (AGP 9.0.0 + Kotlin 2.1.20 target JDK 17).
- Instrumented/integration tests (`connectedAndroidTest`) require a running Android emulator or physical device. These cannot run headlessly in this Cloud VM environment.
- The `GITHUB_RUN_NUMBER` env var is referenced in `build.gradle` for versioning; it defaults to `null` in local builds which is fine.
- Gradle deprecation warnings about `android.*` properties are expected and non-blocking; they warn about defaults changing in AGP 10.
