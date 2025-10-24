# AGENTS.md

Purpose
Rules for automated contributors and code generation agents.

Non-negotiable files
Keep these files exactly as committed:
- gradlew
- gradlew.bat
- gradle/wrapper/gradle-wrapper.jar
- gradle/wrapper/gradle-wrapper.properties

Do not
- Do not delete, rename or move any of the files listed above.
- Do not modify their contents unless the explicit task is “Upgrade the Gradle Wrapper”.
- Do not add .gitignore entries that exclude these files or their folders.

When upgrading the Gradle Wrapper (only if explicitly asked)
- Update `distributionUrl` to the requested Gradle version in `gradle/wrapper/gradle-wrapper.properties`.
- Update `distributionSha256Sum` to the official SHA-256 for that exact `gradle-<version>-bin.zip`.
- Leave `gradlew`, `gradlew.bat` and `gradle/wrapper/gradle-wrapper.jar` tracked in git.

Build rule
- Always build and test with `./gradlew`, not a preinstalled `gradle`.

If a PR cannot include binary files
- Leave the Wrapper files untouched in the branch.
- State in the PR description that the Wrapper is intentionally unchanged and must remain tracked.
