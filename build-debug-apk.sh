#!/usr/bin/env sh
set -e

./gradlew buildDebugApk
echo "APK available at dist/apk/letsdoit-debug.apk"
