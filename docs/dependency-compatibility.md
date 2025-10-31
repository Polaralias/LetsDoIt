# Dependency compatibility assessment

## Tooling stack
- **Gradle Wrapper 8.10.2** works with **Android Gradle Plugin (AGP) 8.5.2**, covering all modules and enabling the latest Java 17 toolchain support.
- **Kotlin 1.9.22** (Android and KAPT plugins) matches the binary metadata produced by the **Jetpack Compose compiler extension 1.5.14**, which avoids the "Could not load module <Error module>" crashes emitted by kapt.
- **Jetpack Compose BOM 2024.05.00** keeps UI artifacts (Material 3, tooling, icons) on the same release train that the 1.5.14 compiler targets.

## Dependency families
- **Dependency Injection**: Dagger/Hilt runtime and compiler are both set to 2.51.1, while the AndroidX Hilt integrations (navigation, work, compiler) stay at 1.2.0 for API parity.
- **Persistence**: Room runtime/ktx/compiler are unified at 2.6.1, ensuring schema processors match the runtime.
- **Concurrency**: kotlinx-coroutines core/android/test all use 1.8.1, avoiding binary incompatibilities between dispatchers.
- **Networking**: Retrofit (2.11.0), Moshi (1.15.1), and OkHttp (4.12.0) form a validated trio with no transitive downgrades.
- **Background work**: WorkManager runtime/testing remain at 2.9.0, matching the AndroidX Hilt work artifact.
- **Testing stack**: AndroidX Test JUnit, runner, rules, and UIAutomator libraries are aligned on the 1.5.x family, keeping instrumentation behavior consistent.

## Observations
- The Compose compiler/KAPT crash was traced to running Kotlin 1.9.24 with the 1.5.14 Compose compiler. Keeping Kotlin on 1.9.22 prevents the metadata mismatch that previously broke annotation processing.
- No additional version conflicts were observed across module dependency graphs after aligning the tooling stack.
