pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
    plugins {
        id("com.android.application") version "8.5.1"
        id("com.android.test") version "8.5.1"
        id("org.jetbrains.kotlin.android") version "1.9.22"
        id("org.jetbrains.kotlin.kapt") version "1.9.22"
        id("com.google.dagger.hilt.android") version "2.51.1"
        id("androidx.baselineprofile") version "1.2.4"
        id("androidx.benchmark") version "1.2.4"
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
