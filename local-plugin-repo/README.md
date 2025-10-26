# Local plugin repository

The committed metadata files describe the plugin coordinates that must be placed in this repository for offline builds. Upload the original plugin artefacts to the matching paths below:

| Path | Maven coordinate |
| --- | --- |
| `androidx/baselineprofile/androidx.baselineprofile.gradle.plugin/1.2.4/androidx.baselineprofile.gradle.plugin-1.2.4.jar` | `androidx.baselineprofile:androidx.baselineprofile.gradle.plugin:1.2.4` |
| `com/android/tools/build/gradle/8.5.1/gradle-8.5.1.jar` | `com.android.tools.build:gradle:8.5.1` |
| `com/google/dagger/hilt-android-gradle-plugin/2.51.1/hilt-android-gradle-plugin-2.51.1.jar` | `com.google.dagger:hilt-android-gradle-plugin:2.51.1` |
| `org/jetbrains/kotlin/android/org.jetbrains.kotlin.android.gradle.plugin/1.9.24/org.jetbrains.kotlin.android.gradle.plugin-1.9.24.jar` | `org.jetbrains.kotlin.android:org.jetbrains.kotlin.android.gradle.plugin:1.9.24` |
| `org/jetbrains/kotlin/kotlin-gradle-plugin/1.9.24/kotlin-gradle-plugin-1.9.24.jar` | `org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.24` |

Place each `.jar` file in the exact directory shown so that Gradle can resolve the plugin once copied into `local-plugin-repo`.
