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

PRs cannot include binary files
- Leave the Wrapper files untouched in the branch.
- If binary files are required in any PRs, confirm in PR description anything which needs to be uploaded manually.


# Build sources policy: always use online repositories

**Intent:** Agents must resolve plugins and dependencies from official public repositories, not from local mirrors. This avoids multi-GB blobs and keeps CI fast and reproducible.

## Required repository configuration

Add these exactly, and nothing else:

```kotlin
// settings.gradle.kts
pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
```

## CI expectations

* Workflows must **not** use `--offline`.
* Use the official Gradle cache action.

```yaml
# .github/workflows/android-debug.yml (excerpt)
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { distribution: temurin, java-version: 17 }
      - uses: gradle/actions/setup-gradle@v3
      - run: ./gradlew clean lint test :app:assembleDebug
```

## Prohibited repositories and patterns

Agents must not introduce:

* `local-plugin-repo`, `flatDir { dirs(...) }`, `mavenLocal()`, file-based Maven repos
* `--offline` in CI
* Binary zips of Gradle or plugin caches committed to the repo

Add this guard to `.gitignore`:

```
local-plugin-repo/
```

## Pull request checklist for agents

* [ ] `settings.gradle.kts` contains only `gradlePluginPortal()`, `google()`, `mavenCentral()`
* [ ] No `flatDir` or `mavenLocal()` anywhere in the project
* [ ] CI workflow builds without `--offline`
* [ ] Gradle wrapper version is pinned and matches `gradle-wrapper.properties`
* [ ] No large binary artefacts added outside `app/build/` outputs

## Automatic guard in CI

Add a quick check to fail builds if a local repo slips in:

```bash
# ci/guards/check-repos.sh
grep -R --line-number -E "local-plugin-repo|flatDir\\s*\\(|mavenLocal\\s*\\(" . && {
  echo "Disallowed repository detected"; exit 1; } || exit 0
```

Then call it before the Gradle step:

```yaml
- run: bash ci/guards/check-repos.sh
```

## Rationale

* Online repos with caching are smaller, faster and easier to keep secure
* Eliminates accidental vendoring of GB-scale plugin jars
* Keeps builds reproducible across developer machines and CI

**Acceptance:** Any PR that adds a local repository, offline flags, or binary plugin mirrors is rejected by the CI guard and must be corrected.

