pluginManagement {
    repositories {
        maven(url = uri("${rootDir}/local-plugin-repo"))
        gradlePluginPortal()
        maven(url = uri("https://dl.google.com/dl/android/maven2/")) {
            metadataSources {
                mavenPom()
                artifact()
            }
        }
        maven(url = uri("https://maven.google.com"))
        google()
        mavenCentral()
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
