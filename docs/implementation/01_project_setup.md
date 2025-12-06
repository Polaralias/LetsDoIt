# Implementation Phase 1: Project Setup

This document details the steps to set up the Android project with the required architecture and dependencies.

## Goal
Initialize a new Android project with Jetpack Compose, Hilt, Room, and Retrofit, following Clean Architecture principles.

## Prompts

### 1. Initialize Project
> Create a new Android project using Android Studio.
> *   **App Name**: Lets Do It
> *   **Package Name**: `com.letsdoit.app`
> *   **Minimum SDK**: API 26 (Android 8.0)
> *   **Language**: Kotlin
> *   **Build Configuration Language**: Kotlin DSL (build.gradle.kts)
> *   **Interface**: Jetpack Compose (Material 3)

### 2. Configure Build Dependencies (libs.versions.toml)
> Update `gradle/libs.versions.toml` to include versions and libraries for:
> *   **Core**: Kotlin 1.9+, Core KTX, Lifecycle Runtime KTX.
> *   **Compose**: Activity Compose, UI, UI Graphics, UI Tooling Preview, Material 3.
> *   **Navigation**: Navigation Compose.
> *   **DI**: Hilt (Dagger Hilt), Hilt Compiler.
> *   **Database**: Room Runtime, Room KTX, Room Compiler.
> *   **Network**: Retrofit, OkHttp, Gson Converter (or Moshi).
> *   **Async**: Coroutines Core, Coroutines Android.
> *   **Testing**: JUnit, Espresso, Compose UI Test.
>
> *Action*: Create the version catalog entries and sync the project.

### 3. Apply Dependencies in App Module
> Update `app/build.gradle.kts`:
> *   Apply plugins: `com.android.application`, `org.jetbrains.kotlin.android`, `com.google.dagger.hilt.android`, `com.google.devtools.ksp` (for Room).
> *   Add implementation dependencies referencing the version catalog.
> *   Ensure `kapt` or `ksp` is configured for Hilt and Room compilers.
> *   Enable Java 17 or 1.8 compatibility as needed by the Kotlin version.

### 4. Setup Project Structure (Package Layout)
> Create the following package structure in `app/src/main/java/com/letsdoit/app/`:
> *   `core`
>     *   `di` (Dependency Injection modules)
>     *   `util` (Utility classes)
> *   `data`
>     *   `local` (Room DB, DAOs, Entities)
>     *   `remote` (Retrofit service, DTOs)
>     *   `repository` (Repository implementations)
> *   `domain`
>     *   `model` (Domain models)
>     *   `repository` (Repository interfaces)
>     *   `usecase` (Business logic)
> *   `presentation`
>     *   `theme` (Compose theme)
>     *   `home` (Home screen feature)
>     *   `taskdetails` (Task details feature)
>     *   `components` (Shared UI components)

### 5. Setup Hilt
> 1.  Create an Application class `LetsDoItApp.kt` in the root package.
> 2.  Annotate it with `@HiltAndroidApp`.
> 3.  Register the Application class in `AndroidManifest.xml` under the `android:name` attribute.
> 4.  Create a `MainActivity.kt` entry point annotated with `@AndroidEntryPoint`.

### 6. Setup Navigation
> 1.  In `presentation/MainActivity.kt`, set up a `NavHost` using `rememberNavController`.
> 2.  Define a sealed class `Screen` in `presentation/navigation/Screen.kt` with routes for:
>     *   `Home`
>     *   `TaskDetails` (accepting a `taskId` argument)
>     *   `Settings`

### 7. Verification
> Run the app on an emulator. It should display the default "Hello Android" text (or a blank screen) without crashing, confirming that Hilt and Compose are correctly configured.
