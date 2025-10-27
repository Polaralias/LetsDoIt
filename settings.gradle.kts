pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
    plugins {
        id("com.android.application") version "8.5.1"
        id("com.android.test") version "8.5.1"
        id("org.jetbrains.kotlin.android") version "1.9.24"
        id("org.jetbrains.kotlin.kapt") version "1.9.24"
        id("com.google.dagger.hilt.android") version "2.51.1"
        id("androidx.baselineprofile") version "1.2.4"
        id("androidx.benchmark") version "1.2.4"
    }
    resolutionStrategy {
        eachPlugin {
            when (requested.id.id) {
                "com.android.application", "com.android.test" ->
                    useModule("com.android.tools.build:gradle:${requested.version}")
                "org.jetbrains.kotlin.android", "org.jetbrains.kotlin.kapt" ->
                    useModule("org.jetbrains.kotlin:kotlin-gradle-plugin:${requested.version}")
                "com.google.dagger.hilt.android" ->
                    useModule("com.google.dagger:hilt-android-gradle-plugin:${requested.version}")
                "androidx.baselineprofile" ->
                    useModule("androidx.baselineprofile:androidx.baselineprofile.gradle.plugin:${requested.version}")
                "androidx.benchmark" ->
                    useModule("androidx.benchmark:benchmark-gradle-plugin:${requested.version}")
            }
        }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "letsdoit"
include(":app")
include(":baselineprofile")
include(":benchmark")
