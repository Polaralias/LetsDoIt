#!/usr/bin/env bash
set -euo pipefail
if grep -R --line-number --include="build.gradle.kts" -E 'org.jetbrains.kotlin\.(android|kapt).*\bversion\b' .; then
  echo "Do not specify Kotlin plugin versions in subprojects"
  exit 1
fi
