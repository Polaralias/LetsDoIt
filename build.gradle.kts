// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.jetbrainsKotlinAndroid) apply false
    alias(libs.plugins.hiltAndroid) apply false
    alias(libs.plugins.ksp) apply false
}

tasks.register("buildDebugApk") {
    group = "build"
    description = "Assemble the debug APK and copy it to dist/apk/letsdoit-debug.apk."
    dependsOn(":app:assembleDebug")

    doLast {
        val apkPath = rootProject.file("app/build/outputs/apk/debug/app-debug.apk")
        if (!apkPath.exists()) {
            error("Debug APK not found at ${apkPath.path}. Expected output from :app:assembleDebug.")
        }

        val outputDir = rootProject.file("dist/apk")
        if (!outputDir.exists() && !outputDir.mkdirs()) {
            error("Unable to create output directory at ${outputDir.path}.")
        }

        val outputFile = outputDir.resolve("letsdoit-debug.apk")
        apkPath.copyTo(outputFile, overwrite = true)
        println("Debug APK copied to: ${outputFile.path}")
    }
}
