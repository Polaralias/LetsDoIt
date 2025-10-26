buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.5.1")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.24")
        classpath("com.google.dagger:hilt-android-gradle-plugin:2.51.1")
        classpath("androidx.baselineprofile:androidx.baselineprofile.gradle.plugin:1.2.4")
        classpath("androidx.benchmark:benchmark-gradle-plugin:1.2.4")
    }
}
