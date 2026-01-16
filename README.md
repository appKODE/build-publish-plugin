# Android Build Publish Plugin

A comprehensive Gradle plugin suite for automating Android build publishing workflows. This plugin provides:

- Version management through Git tags
- Automated changelog generation
- Firebase App Distribution
- Google Play Store publishing
- Jira integration
- Telegram notifications
- Custom plugin support

## Efficiency and other key advantages

This plugin suite is designed to be "build friendly" and behave well in CI/CD environments.

- **Lazy configuration (Providers / configuration avoidance)**
  - Most values are modeled via Gradle `Property` / `Provider` APIs and are resolved late.
  - Tasks are registered using Gradle’s configuration avoidance APIs (so they don’t get realized unless needed).

- **Conditional task creation**
  - Many tasks are registered only when the corresponding configuration is present.
  - Examples:
    - Jira: `jiraAutomation<Variant>` is created only if at least one automation action is enabled.
    - ClickUp: `clickUpAutomation<Variant>` is created only if tag/fixVersion automation is enabled.
    - Slack/Telegram: distribution tasks are skipped when destinations are not configured.

- **No network calls at configuration time**
  - Network operations are executed only during task execution (not during Gradle configuration).

- **Worker API for heavy work**
  - Network uploads and message sending are delegated to Gradle Worker API work actions where applicable.
  - This keeps task execution responsive and avoids blocking the main build thread.

- **Shared services for network clients**
  - External integrations (Slack/Telegram/Jira/Confluence/ClickUp/etc.) use Gradle Shared Build Services.
  - This avoids re-creating HTTP clients for each task and improves stability/throughput.

- **Variant-aware wiring via Android Components**
  - Tasks are wired per Android build variant, with a predictable naming scheme and clear dependencies.

## Table of Contents

- [Tag-based automation (core concepts)](#tag-based-automation-core-concepts)
- [Migration to build-publish-novo](#migration-to-build-publish-novo)
- [Installation](#installation)
- [Examples](#examples)
  - [Example Project](#1-example-project)
  - [Example Plugin](#2-example-plugin)
  - [Plugin Tests](#3-plugin-tests)
- [Available Plugins](#available-plugins)
  - [Foundation Plugin](#1-foundation-plugin-rukodeandroidbuild-publish-novofoundation)
  - [Firebase Plugin](#2-firebase-plugin-rukodeandroidbuild-publish-novofirebase)
  - [Play Store Plugin](#3-play-store-plugin-rukodeandroidbuild-publish-novoplay)
  - [Slack Plugin](#4-slack-plugin-rukodeandroidbuild-publish-novoslack)
  - [Telegram Plugin](#5-telegram-plugin-rukodeandroidbuild-publish-novotelegram)
  - [Jira Plugin](#6-jira-plugin-rukodeandroidbuild-publish-novojira)
  - [Confluence Plugin](#7-confluence-plugin-rukodeandroidbuild-publish-novoconfluence)
  - [ClickUp Plugin](#8-clickup-plugin-rukodeandroidbuild-publish-novoclickup)
- [Custom Plugin Development](#custom-plugin-development)
- [Troubleshooting](#troubleshooting)
- [Contributing](#contributing)
- [License](#license)

## Tag-based automation (core concepts)

The core idea behind this plugin suite is **tag-based automation**.

Instead of storing version/build metadata in Gradle properties or CI variables, the plugin uses **Git tags** as the
single source of truth for:

- **Build number** (used as `versionCode` by default)
- **Build version** (used as `versionName` by default)
- **Changelog generation window** (diff between the last matching tag and HEAD)

### Why tags?

- **Deterministic and reproducible**
  - Tags are part of Git history and can be fetched in any environment (`git fetch --tags`).

- **Decoupled from branches and CI**
  - The same commit always has the same tag metadata, regardless of which CI system runs it.

- **Flexible automation foundation**
  - Multiple plugins (Jira/ClickUp/Slack/Telegram/Play/etc.) can rely on the same tag snapshot and changelog,
    which keeps automation consistent.

### How tag parsing works

The parser treats the **last numeric part before `-<variant>`** as the build number, and the preceding numeric parts
as the build version.

Examples:

```
v1.0.100-debug         -> buildVersion = 1.0,   buildNumber = 100
v1.2.3.42-release      -> buildVersion = 1.2.3, buildNumber = 42
app.2024.15-staging    -> buildVersion = 2024,  buildNumber = 15
```

### Default tag matching pattern

By default, the foundation plugin uses this regex template:

`DEFAULT_TAG_PATTERN = ".+\\.(\\d+)-%s"`

Where `%s` is replaced with the Android build variant name.

This means:

- Tags must include the build variant suffix (e.g. `-debug`, `-release`).
- Tags must contain at least one numeric group.

### Variant-specific tags (debug/release) and multiple tags per commit

Tag matching is **variant-aware**: each Android variant has its own tag stream because `%s` is replaced with the
variant name.

This enables a workflow where you can tag the **same commit** multiple times — once per build type/flavor:

```
v1.2.3.10-debug
v1.2.3.10-release
```

Both tags can point to the same commit SHA and are still treated as independent streams, because each variant uses
its own pattern and selection.

### How the plugin finds the “last tag” (`getLastTagSnapshot<Variant>`)

For each variant, the foundation plugin computes a regex from `buildTagPattern` (or `DEFAULT_TAG_PATTERN`) and then:

- lists all Git tags
- filters tags by the regex
- sorts tags primarily by commit order/time and then by extracted build number
- picks:
  - **`current`**: the first tag in the sorted list
  - **`previousInOrder`**: the second tag in the list (if present)
  - **`previousOnDifferentCommit`**: the first tag that points to a different commit (useful when multiple tags
    point to the same commit)

These values are stored in the tag snapshot JSON and reused by other tasks.

### Changelog range selection (why it uses the previous *commit* tag)

Changelog generation uses **`previousOnDifferentCommit`** (exposed as `snapshot.previous`) as the start of the commit
range, not `previousInOrder`.

Reason:

- It is valid to have multiple tags pointing to the **same commit for the same variant** (for example, you restart a
  CI build or re-run a release job and create a new tag without any new commits).
- In that situation, `previousInOrder` may point to a tag on the same commit, and using it as a range start would
  produce an empty/duplicate changelog.

By selecting the previous tag on a **different commit**, the changelog reflects the actual changes since the last
code change, while still allowing tag messages/metadata to be attached to the current build.

### Build number requirements (why `versionCode` must increase)

By default, `buildNumber` extracted from the tag is used as `versionCode`.
To keep versioning stable and monotonic:

- **Build numbers must be positive**
  - The plugin treats `0` and negative build numbers as invalid.

- **Build numbers must increase within the same variant tag stream**
  - The tag selection logic validates the last tags to ensure build numbers and commit chronology are consistent.
  - If the plugin detects that a “newer” tag has a build number that is not greater than the previous one,
    it fails with a detailed Gradle error.

This is one of the reasons tags are used as a core automation primitive: they provide a single, auditable,
monotonically-increasing sequence per variant.

### Customizing the tag pattern (`buildTagPattern`)

Configure `buildPublishFoundation.output.common.buildTagPattern { ... }` to match your tag naming convention.

Kotlin DSL (`build.gradle.kts`):

```kotlin
buildPublishFoundation {
    output {
        common {
            buildTagPattern {
                literal("v")
                separator(".")
                buildVersion()
                separator("-")
                buildVariantName()
            }
        }
    }
}
```

Groovy DSL (`build.gradle`):

```groovy
buildPublishFoundation {
    output {
        common {
            it.buildTagPattern {
                literal('v')
                separator('.')
                buildVersion()
                separator('-')
                buildVariantName()
            }
        }
    }
}
```

### How automation consumes tags

- Foundation produces a tag snapshot via `getLastTagSnapshot<Variant>`.
- Other tasks/plugins read that snapshot to:
  - compute `versionName` / `versionCode`
  - generate a changelog (`generateChangelog<Variant>`)
  - attach version info to uploads / notifications

### Fallback behavior

If no matching tag is found, the foundation plugin can fall back to stub/default values.
This is controlled by `output.useStubsForTagAsFallback` and `output.useDefaultsForVersionsAsFallback`.

## Migration to build-publish-novo

If you are migrating from an older/legacy version of this plugin suite to the `*-novo` line, treat it as a
**breaking change** and do a quick audit of plugin IDs, dependencies, and your tag/versioning setup.

High-level changes introduced in the `novo` line:

- The plugin is now **modular**: each integration is a separate Gradle plugin (`foundation`, `slack`, `telegram`, `jira`, `confluence`, `clickup`, `play`, `firebase`).
- Common logic is extracted into a shared core library (`ru.kode.android:build-publish-novo-core`).
- Tag-based automation is **variant-aware** by default and validates tag ordering and build numbers.
- **AppCenter integration was removed** (if you used it previously, delete related configuration/tasks and replace with another distribution channel).

### 1) Update plugin IDs

Update all plugin IDs to the `ru.kode.android.build-publish-novo.*` namespace.

Recommendation:

- Search your build logic for `build-publish` and update IDs/artifacts accordingly.

### 2) Update dependency coordinates (Version Catalog / build-logic)

If you apply Build Publish plugins from a convention module (`build-logic` / `build-conventions`), make sure you use
the **novo** artifacts, for example:

- `ru.kode.android:build-publish-novo-core:...`
- `ru.kode.android.build-publish-novo.<plugin>:ru.kode.android.build-publish-novo.<plugin>.gradle.plugin:...`

Also note that there is no single “all-in-one” plugin anymore: if your old setup had one plugin that configured
multiple integrations, you now add/apply the exact set of `novo` plugins you need.

### 2.1) Update extension blocks (DSL)

The configuration is split into per-plugin extensions.

- Old setup (legacy): typically one root extension or a combined configuration block.
- New setup (novo): configure each integration via its own extension:
  - `buildPublishFoundation { ... }`
  - `buildPublishSlack { ... }`
  - `buildPublishTelegram { ... }`
  - `buildPublishJira { ... }`
  - `buildPublishConfluence { ... }`
  - `buildPublishClickUp { ... }`
  - `buildPublishPlay { ... }`
  - `buildPublishFirebase { ... }`

### 3) Ensure the foundation plugin is applied

All other plugins rely on the foundation plugin to run variant configuration.
Make sure it is applied in every Android **application** module that uses any publishing/integration plugin:

- `ru.kode.android.build-publish-novo.foundation`

### 4) Verify tag-based versioning still matches your repository

The `novo` line is strongly built around tag-based automation.
Before running CI, verify that tags exist and match your variant(s):

```bash
./gradlew getLastTagSnapshotRelease
./gradlew printLastIncreasedTagRelease
```

Breaking change note: in the `novo` line the legacy `getLastTag<Variant>` task is renamed to
`getLastTagSnapshot<Variant>`.

Important behavior changes to account for:

- **Build numbers must be positive** (`0` and negative values are treated as invalid).
- **Build numbers must increase within the same variant tag stream**.
- Tag selection is variant-aware (tags typically end with `-debug`, `-release`, etc.).

What the foundation plugin does per variant:

- resolves a tag regex from `buildTagPattern` (or the default)
- picks the latest matching tag and writes a JSON snapshot file
- downstream tasks use that snapshot for `versionCode` / `versionName` / changelog / uploads

If your previous setup used a different tag naming scheme, configure `buildPublishFoundation.output.common.buildTagPattern { ... }`.

### 4.1) Breaking change: `versionName` strategy

In the `novo` line, the default `versionName` is derived from the parsed tag **build version** only
(for example `1.2` or `1.2.3`).

If in the legacy version you relied on `versionName` including the build number from the tag, configure a different
strategy explicitly, for example `BuildVersionNumberNameStrategy`.

Kotlin DSL (`build.gradle.kts`):

```kotlin
import ru.kode.android.build.publish.plugin.core.strategy.BuildVersionNumberNameStrategy

buildPublishFoundation {
    output {
        buildVariant("internal") {
            baseFileName = "ceb-android"
            versionNameStrategy { BuildVersionNumberNameStrategy }
        }
    }
}
```

Groovy DSL (`build.gradle`): **strategies must be instantiated** (use `new ...()`):

```groovy
import ru.kode.android.build.publish.plugin.core.strategy.BuildVersionNumberNameStrategy

buildPublishFoundation {
  output {
    buildVariant('internal') {
      baseFileName = 'ceb-android'
      versionNameStrategy { new BuildVersionNumberNameStrategy() }
    }
  }
}
```

If you configure this from a convention module (`build-logic` / `build-conventions`), make sure the module has access
to the core classes by adding `ru.kode.android:build-publish-novo-core` to its dependencies (see Installation section).

### 5) Re-check secrets handling

When migrating, re-check all credentials configuration:

- Prefer CI/CD secret variables or `local.properties` for local development.
- For string secrets, use `providers.environmentVariable("...")`.
- For some file-based secrets used by Worker API / shared services, resolve file paths eagerly (see the secrets section).

If your legacy setup relied on checked-in secret files, migrate them to CI secret variables. For GitHub Actions,
store file content as base64 in a secret and decode it in a pre-step (see the secrets section).

## Installation

This repository publishes multiple Gradle plugins. The published plugin IDs follow the pattern:

`ru.kode.android.build-publish-novo.<plugin>`

For example:

- `ru.kode.android.build-publish-novo.foundation`
- `ru.kode.android.build-publish-novo.firebase`
- `ru.kode.android.build-publish-novo.play`

### 1) Configure plugin repositories

In `settings.gradle.kts` make sure you have a plugin repository that contains the plugin artifacts.

```kotlin
pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
        // If you publish to a private Maven repository, add it here.
        // maven("https://your-maven-repo.com")
    }
}
```

In `settings.gradle` (Groovy DSL) the equivalent looks like:

```groovy
// settings.gradle
pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
        // If you publish to a private Maven repository, add it here.
        // maven { url 'https://your-maven-repo.com' }
    }
}
```

### 2) Apply plugins in an Android application module

Apply plugins in the Android **application** module (the foundation plugin fails fast for library
modules and unsupported AGP versions).

```kotlin
// app/build.gradle.kts
plugins {
    id("com.android.application")
    id("ru.kode.android.build-publish-novo.foundation") version "x.y.z"
}
```

#### 2.1) Apply plugins via Version Catalog aliases in the `plugins { ... }` block

If you use the Version Catalog (`libs.versions.toml`), you can declare **plugin aliases** and then apply them
in the `plugins { ... }` block.

Add plugin aliases to `libs.versions.toml`:

```toml
[plugins]
buildpublish-foundation = { id = "ru.kode.android.build-publish-novo.foundation", version.ref = "build-publish" }
buildpublish-telegram = { id = "ru.kode.android.build-publish-novo.telegram", version.ref = "build-publish" }
buildpublish-confluence = { id = "ru.kode.android.build-publish-novo.confluence", version.ref = "build-publish" }
```

Then apply them in an app module.

Kotlin DSL (`build.gradle.kts`):

```kotlin
plugins {
    id("com.android.application")
    alias(libs.plugins.buildpublish.foundation)
    alias(libs.plugins.buildpublish.telegram)
    alias(libs.plugins.buildpublish.confluence)
}
```

Groovy DSL (`build.gradle`):

```groovy
plugins {
    id 'com.android.application'
    alias(libs.plugins.buildpublish.foundation)
    alias(libs.plugins.buildpublish.telegram)
    alias(libs.plugins.buildpublish.confluence)
}
```

### 3) Apply Build Publish plugins from convention plugins (`build-logic` / `build-conventions`) using `libs`

If you use a `build-logic` / `build-conventions` module with convention plugins, you can add the Build Publish
plugins to the convention plugin classpath using the **Gradle Version Catalog** (`libs`) and apply them from your
convention plugin.

#### 3.1) Add plugin artifacts to `libs.versions.toml`

Example entries (based on the published artifacts):

```toml
[libraries]
buildpublish-core = { group = "ru.kode.android", name = "build-publish-novo-core", version.ref = "build-publish-core" }
buildpublish-foundation-plugin = { module = "ru.kode.android.build-publish-novo.foundation:ru.kode.android.build-publish-novo.foundation.gradle.plugin", version.ref = "build-publish" }
buildpublish-telegram-plugin = { module = "ru.kode.android.build-publish-novo.telegram:ru.kode.android.build-publish-novo.telegram.gradle.plugin", version.ref = "build-publish" }
buildpublish-confluence-plugin = { module = "ru.kode.android.build-publish-novo.confluence:ru.kode.android.build-publish-novo.confluence.gradle.plugin", version.ref = "build-publish" }
```

#### 3.2) Add dependencies in your convention plugins module

In `build-logic/build.gradle.kts` (or your `build-conventions` module), add plugin artifacts as dependencies:

```kotlin
dependencies {
    implementation(libs.buildpublish.core)

    implementation(libs.buildpublish.foundation.plugin)
    implementation(libs.buildpublish.telegram.plugin)
    implementation(libs.buildpublish.confluence.plugin)
}
```

#### 3.3) Apply plugins from a convention plugin

Example convention plugin (Kotlin):

```kotlin
import org.gradle.api.Plugin
import org.gradle.api.Project

class AndroidBuildPublishConventionPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.pluginManager.apply("ru.kode.android.build-publish-novo.foundation")
        project.pluginManager.apply("ru.kode.android.build-publish-novo.telegram")
        project.pluginManager.apply("ru.kode.android.build-publish-novo.confluence")
    }
}
```

Then in your app module you apply only your convention plugin:

```kotlin
plugins {
    id("your.convention.build-publish")
}
```

#### 3.4) Precompiled script convention plugins (`src/main/kotlin/*.gradle.kts`)

Another common approach is using **precompiled script plugins** inside your `build-logic` / `build-conventions`
module.

In that approach you create files like:

- `build-conventions/src/main/kotlin/your.convention.build-publish.gradle.kts`

Gradle will compile this script and **generate a plugin** automatically.
The plugin id is derived from the file name (`your.convention.build-publish`).

Example precompiled script plugin:

```kotlin
import org.gradle.api.GradleException
import ru.kode.android.build.publish.plugin.core.enity.BuildVariant
import ru.kode.android.build.publish.plugin.core.enity.Tag
import ru.kode.android.build.publish.plugin.core.strategy.BuildVersionNumberNameStrategy
import ru.kode.android.build.publish.plugin.core.strategy.DEFAULT_VERSION_CODE
import ru.kode.android.build.publish.plugin.core.strategy.VersionCodeStrategy

plugins {
  id("com.android.application")
  id("ru.kode.android.build-publish-novo.foundation")
  id("ru.kode.android.build-publish-novo.telegram")
  id("ru.kode.android.build-publish-novo.confluence")
}

buildPublishFoundation {
  verboseLogging.set(
    providers.environmentVariable("BUILD_VERBOSE_LOGGING")
      .map { it.toBoolean() }
      .orElse(false)
  )

  output {
    common {
      baseFileName = "android"
    }

    buildVariant("debug") {
      baseFileName = "android"
      useVersionsFromTag = false
    }

    buildVariant("internal") {
      baseFileName = "android"
      versionNameStrategy { BuildVersionNumberNameStrategy }
    }

    buildVariant("release") {
      baseFileName = "android"
      versionNameStrategy { BuildVersionNumberNameStrategy }
      versionCodeStrategy { ReleaseCodeStrategy }
    }
  }

  changelogCommon {
    issueNumberPattern = "PROJECT-\\d+"
    issueUrlPrefix = "https://jira.com/browse/"
    commitMessageKey = "CHANGELOG"
  }
}

private object ReleaseCodeStrategy : VersionCodeStrategy {
  override fun build(
    buildVariant: BuildVariant,
    tag: Tag.Build?,
  ): Int {
    return if (tag != null) {
        val major = tag.buildVersion.substringBefore(".").toInt()
        val minor = tag.buildVersion.substringAfter(".").toInt()
      (major * 1000 + minor) * 1000 + tag.buildNumber
    } else DEFAULT_VERSION_CODE
  }
}

buildPublishTelegram {
  botsCommon {
    bot("changelogger") {
      botId.set(
        providers.environmentVariable("TELEGRAM_CHANGELOGGER_BOT_ID")
          .map {
            if (it.isBlank()) {
              throw GradleException("no TELEGRAM_CHANGELOGGER_BOT_ID defined for telegram reports")
            }
            it
          }
          .orElse("")
      )

      botServerBaseUrl.set(
        providers.environmentVariable("BUILD_REPORT_TELEGRAM_BOT_BASE_URL")
          .map {
            if (it.isBlank()) {
              throw GradleException("no BUILD_REPORT_TELEGRAM_BOT_BASE_URL defined for telegram reports")
            }
            it
          }
          .orElse("")
      )
      botServerAuth.username.set(
        providers.environmentVariable("BUILD_REPORT_TELEGRAM_BOT_AUTH_USERNAME")
          .map {
            if (it.isBlank()) {
              throw GradleException("no BUILD_REPORT_TELEGRAM_BOT_AUTH_USERNAME defined for telegram reports")
            }
            it
          }
          .orElse("")
      )
      botServerAuth.password.set(
        providers.environmentVariable("BUILD_REPORT_TELEGRAM_BOT_AUTH_PASSWORD")
          .map {
            if (it.isBlank()) {
              throw GradleException("no BUILD_REPORT_TELEGRAM_BOT_AUTH_PASSWORD defined for telegram reports")
            }
            it
          }
          .orElse("")
      )

      chat("builds") {
        chatId.set(
          providers.environmentVariable("BUILD_REPORT_TELEGRAM_CHAT_ID")
            .map {
              if (it.isBlank()) {
                throw GradleException("no BUILD_REPORT_TELEGRAM_CHAT_ID defined for telegram reports")
              }
              it
            }
            .orElse("")
        )
        topicId.set(
          providers.environmentVariable("BUILD_REPORT_TELEGRAM_TOPIC_ID")
            .map {
              if (it.isBlank()) {
                throw GradleException("no BUILD_REPORT_TELEGRAM_TOPIC_ID defined for telegram reports")
              }
              it
            }
            .orElse("")
        )
      }
    }

    changelogCommon {
      userMentions(
        providers.environmentVariable("BUILD_REPORT_TELEGRAM_USER_MENTIONS")
          .map {
            if (it.isBlank()) {
              throw GradleException("no BUILD_REPORT_TELEGRAM_USER_MENTIONS defined for telegram reports")
            }
            it.trim().split(",").toList()
          }
          .orElse(emptyList())
      )

      destinationBot {
        botName = "changelogger"
        chatNames("builds")
      }
    }

    distributionCommon {
      destinationBot {
        botName = "changelogger"
        chatNames("builds")
      }
    }
  }
}

buildPublishConfluence {
  auth {
    common {
      baseUrl.set("https://confluence.com")
      credentials.username.set(providers.environmentVariable("CONFLUENCE_USER_NAME"))
      credentials.password.set(providers.environmentVariable("CONFLUENCE_USER_PASSWORD"))
    }
  }

  distribution {
    common {
      pageId.set(providers.environmentVariable("CONFLUENCE_PAGE_ID"))
    }
  }
}
```

Then in the app module you apply the generated convention plugin:

```kotlin
plugins {
  id("your.convention.build-publish")
}
```

### 4) Configuring secrets (tokens, passwords) via environment variables

For CI/CD it is recommended to configure credentials via **environment variables** and wire them into plugin
configuration using Gradle’s `ProviderFactory`:

- `providers.environmentVariable("...")` is lazy (safe for configuration avoidance).
- You can validate values early and fail the build with a clear message.

#### 4.1) Where to store secrets (including files)

For security reasons:

- **Do not commit secrets** (tokens, passwords, service-account JSON, etc.) into the repository.
- **Local development**: store secrets in `local.properties` (gitignored) or environment variables.
- **CI/CD**: store secrets in your CI/CD secret variables store.

GitHub Actions does not support “secret files” directly. For files (for example JSON credentials), a common
approach is to store the file content in a secret as **base64** and decode it at runtime.

#### Kotlin DSL (`build.gradle.kts`)

String secret (for example, bot token):

```kotlin
val telegramBotIdProvider =
    providers.environmentVariable("TELEGRAM_CHANGELOGGER_BOT_ID")
        .map {
            if (it.isBlank()) {
                throw GradleException("no TELEGRAM_CHANGELOGGER_BOT_ID defined for telegram reports")
            }
            it
        }
        .orElse("")

buildPublishTelegram {
    botsCommon {
        bot("changelogger") {
            botId.set(telegramBotIdProvider)
        }
    }
}
```

File secret (env var contains file path):

```kotlin
buildPublishClickUp {
    auth {
        common {
            apiTokenFile.set(
                providers.environmentVariable("CLICKUP_TOKEN_FILE")
                    .map {
                        if (it.isBlank()) {
                            throw GradleException("no CLICKUP_TOKEN_FILE env var provided")
                        }
                        layout.projectDirectory.file(it)
                    }
            )
        }
    }
}
```

#### Groovy DSL (`build.gradle`)

```groovy
def telegramBotIdProvider = providers.environmentVariable('TELEGRAM_CHANGELOGGER_BOT_ID')
    .map {
        if (it.isBlank()) {
            throw new GradleException('no TELEGRAM_CHANGELOGGER_BOT_ID defined for telegram reports')
        }
        it
    }
    .orElse('')

buildPublishTelegram {
    botsCommon {
        bot('changelogger') {
            it.botId.set(telegramBotIdProvider)
        }
    }
}
```

#### Note about file-based token properties and Worker API isolation

Some integrations use Gradle Worker API / shared services under the hood. For **file-based secrets** (for example
Slack `uploadApiTokenFile`) it can be safer to resolve the environment variable **eagerly** into a concrete file
path and set the property to an actual file (instead of relying on lazy `Provider` mapping).

Groovy DSL example:

```groovy
// NOTE: Need to get it eagerly, because it cannot be resolved correctly in isolated environment
def slackApiTokenFilePath = System.getenv("SLACK_API_KEY") ?: "${rootProject.projectDir}/slack-token.txt"

buildPublishSlack {
    bot {
        common {
            it.uploadApiTokenFile.set(project.file(slackApiTokenFilePath))
        }
    }
}
```

Kotlin DSL example:

```kotlin
// NOTE: Need to get it eagerly, because it cannot be resolved correctly in isolated environment
val slackApiTokenFilePath = System.getenv("SLACK_API_KEY") ?: "${rootProject.projectDir}/slack-token.txt"

buildPublishSlack {
    bot {
        common {
            uploadApiTokenFile.set(project.file(slackApiTokenFilePath))
        }
    }
}
```

#### 4.2) GitHub Actions: store secret files as base64 and decode in a pre-step

Example (service account JSON stored in `PLAY_ACCOUNT_JSON_B64`):

1) Encode locally:

```bash
base64 -w 0 play-account.json
```

2) Save the resulting value in GitHub repository secrets (e.g. `PLAY_ACCOUNT_JSON_B64`).

3) Decode it in workflow before Gradle runs:

```yaml
- name: Decode Play service account
  shell: bash
  run: |
    echo "${{ secrets.PLAY_ACCOUNT_JSON_B64 }}" | base64 --decode > play-account.json
```

Then reference the generated file from Gradle, for example:

```kotlin
buildPublishPlay {
    auth {
        common {
            apiTokenFile.set(file("play-account.json"))
        }
    }
}
```

## Examples

The project includes several examples to help you get started:

### 1. Example Project
Located in `example-project/`, this is a complete Android application demonstrating how to use the plugin in a real-world scenario. It includes:
- Multiple build types and flavors
- Integration with Firebase and Play Store
- Example of version management
- Sample build configurations

To use the example project:
1. Navigate to the `example-project` directory
2. Run `./gradlew tasks` to see available tasks
3. Try building different variants: `./gradlew assembleDebug` or `./gradlew assembleRelease`

### 2. Example Plugin
Found in `example-plugin/`, this demonstrates how to create a custom plugin that extends the build publish functionality. It includes:
- A simple plugin that prints the current Git tag
- Basic plugin structure and configuration
- Integration with the main plugin system

### 3. Plugin Tests
In the `plugin-test/` directory, you'll find test implementations for all major plugin features:
- Firebase App Distribution
- Google Play Store publishing
- Jira automation
- Slack notifications and distribution
- Telegram notifications and distribution
- ClickUp task management
- Confluence distribution

These test modules serve as practical references for implementing specific features in your project.

## Available Plugins

### 1. Foundation Plugin (`ru.kode.android.build-publish-novo.foundation`)

The core plugin that provides essential functionality for build publishing, version management, and changelog generation. This plugin must be applied to all modules that will use any of the publishing plugins.

This plugin supports only:

- Android **application** modules (`com.android.application`)
- Android Gradle Plugin **7.4+**

#### Key Features
- Automatic version management using Git tags
- Changelog generation from commit history
- Build variant support (flavors and build types)
- Customizable version code and name strategies
- Support for multiple output formats (APK, AAB)

#### Minimum Setup

##### Kotlin DSL (`build.gradle.kts`)

```kotlin
// app/build.gradle.kts
plugins {
   id("com.android.application")
   id("ru.kode.android.build-publish-novo.foundation")
}

buildPublishFoundation {
   output {
      common {
         baseFileName.set("app")
         useVersionsFromTag.set(true)

         // Matches tags like:
         // - v1.0.100-debug
         // - v1.2.3.42-release
         // The last numeric part is treated as the build number.
         buildTagPattern {
            literal("v")
            separator(".")
            buildVersion()
            optionalSeparator(".")
            anyOptionalSymbols()
            separator("-")
            buildVariantName()
         }
      }
   }
}
```

##### Groovy DSL (`build.gradle`)

```groovy
// app/build.gradle
plugins {
    id 'com.android.application'
    id 'ru.kode.android.build-publish-novo.foundation'
}

buildPublishFoundation {
    output {
        common {
            it.baseFileName.set('app')
            it.useVersionsFromTag.set(true)

            it.buildTagPattern {
                literal('v')
                separator('.')
                buildVersion()
                optionalSeparator('.')
                anyOptionalSymbols()
                separator('-')
                buildVariantName()
            }
        }
    }
}
```

#### Full Configuration

##### Kotlin DSL (`build.gradle.kts`)

```kotlin
buildPublishFoundation {
   verboseLogging.set(false)
   bodyLogging.set(false)

   output {
      common {
         baseFileName.set("app")
         useVersionsFromTag.set(true)
         useDefaultsForVersionsAsFallback.set(true)

         versionNameStrategy {
            ru.kode.android.build.publish.plugin.core.strategy.BuildVersionNumberVariantNameStrategy
         }

         versionCodeStrategy {
            ru.kode.android.build.publish.plugin.core.strategy.BuildVersionCodeStrategy
         }

         outputApkNameStrategy {
            ru.kode.android.build.publish.plugin.core.strategy.VersionedApkNamingStrategy
         }
      }

      buildVariant("debug") {
         useStubsForTagAsFallback.set(true)
      }
   }

   changelog {
      common {
         issueNumberPattern.set("#(\\d+)")
         issueUrlPrefix.set("https://your-issue-tracker.com/issue/")
         commitMessageKey.set("message")
         excludeMessageKey.set(true)
      }
   }
}
```

##### Groovy DSL (`build.gradle`)

```groovy
buildPublishFoundation {
    verboseLogging.set(false)
    bodyLogging.set(false)

    output {
        common {
            it.baseFileName.set('app')
            it.useVersionsFromTag.set(true)
            it.useDefaultsForVersionsAsFallback.set(true)

            it.versionNameStrategy {
              new ru.kode.android.build.publish.plugin.core.strategy.BuildVersionNumberVariantNameStrategy()
            }

            it.versionCodeStrategy {
                new ru.kode.android.build.publish.plugin.core.strategy.BuildVersionCodeStrategy()
            }

            it.outputApkNameStrategy {
                new ru.kode.android.build.publish.plugin.core.strategy.VersionedApkNamingStrategy()
            }
        }

        buildVariant('debug') {
            it.useStubsForTagAsFallback.set(true)
        }
    }

    changelog {
        common {
            it.issueNumberPattern.set('#(\\d+)')
            it.issueUrlPrefix.set('https://your-issue-tracker.com/issue/')
            it.commitMessageKey.set('message')
            it.excludeMessageKey.set(true)
        }
    }
}
```

#### Configuration Reference

##### Root properties (`buildPublishFoundation { ... }`)

- **`verboseLogging`**
  - **What it does**: Enables extra informational logging from build-publish plugins.
  - **Why you need it**: Useful for debugging why a particular config (for example `common` vs `buildVariant(...)`) was chosen and what tasks were configured.
  - **When to enable**: CI troubleshooting, local debugging.

- **`bodyLogging`**
  - **What it does**: Enables logging of HTTP request/response bodies for plugins that talk to external APIs.
  - **Why you need it**: Helps troubleshoot API failures or unexpected responses.
  - **Warning**: Can print sensitive data. Prefer keeping it disabled in CI.

##### Output (`buildPublishFoundation { output { ... } }`)

Output configuration is defined per Android build variant using:

- `common { ... }` for defaults applied to all variants
- `buildVariant("debug") { ... }` to override for a single variant

Properties (applies to each `OutputConfig`):

- **`baseFileName`** *(required)*
  - **What it does**: Base name used by output file naming strategy.
  - **Why you need it**: Ensures produced APKs are easy to recognize (for example `app-release-...apk`).

- **`useVersionsFromTag`** *(default: `true`)*
  - **What it does**: Reads version info from Git tags.
  - **Why you need it**: Single source of truth for `versionName`/`versionCode` across builds.
  - **If disabled**: Version values are taken from fallbacks (defaults or Android DSL depending on other settings).

- **`useStubsForTagAsFallback`** *(default: `true`)*
  - **What it does**: If no matching Git tag is found, allows the build to continue using stub tag values.
  - **Why you need it**: Useful for first CI runs / new branches where tags aren’t present yet.
  - **If disabled**: Missing tags typically cause the tag snapshot task to fail.

- **`useDefaultsForVersionsAsFallback`** *(default: `true`)*
  - **What it does**: Uses default version values when tag-derived values are unavailable.
  - **Why you need it**: Lets builds proceed even when tag parsing is temporarily unavailable.

- **`buildTagPattern { ... }`**
  - **What it does**: Builds a regex template that is used to find the “latest” tag for each variant.
  - **Why you need it**: Your repository’s tag format must match what the plugin expects.
  - **Important**: The pattern must contain:
    - at least one `buildVersion()` group (`(\\d+)`)
    - `buildVariantName()` (`%s`) so each variant can have its own tag stream

- **`versionNameStrategy { ... }`**
  - **What it does**: Defines how `versionName` is computed from a resolved tag.
  - **Why you need it**: Different projects encode different info into tags.
  - **Groovy DSL note**: In Groovy (`build.gradle`) the strategy must be created with `new`, for example
    `versionNameStrategy { new BuildVersionNumberNameStrategy() }`.
  - **Common choices**:
    - `ru.kode.android.build.publish.plugin.core.strategy.BuildVersionNameStrategy`
    - `ru.kode.android.build.publish.plugin.core.strategy.BuildVersionNumberNameStrategy`
    - `ru.kode.android.build.publish.plugin.core.strategy.BuildVersionNumberVariantNameStrategy`
    - `ru.kode.android.build.publish.plugin.core.strategy.BuildVersionVariantNameStrategy`
    - `ru.kode.android.build.publish.plugin.core.strategy.TagRawNameStrategy`

- **`versionCodeStrategy { ... }`**
  - **What it does**: Defines how `versionCode` is computed from a resolved tag.
  - **Why you need it**: Allows you to encode semantic versioning or fixed version code rules.
  - **Groovy DSL note**: Use `new` when the strategy is a class, for example
    `versionCodeStrategy { new BuildVersionCodeStrategy() }`.
  - **Common choices**:
    - `ru.kode.android.build.publish.plugin.core.strategy.BuildVersionCodeStrategy`
    - `ru.kode.android.build.publish.plugin.core.strategy.SemanticVersionFlattenedCodeStrategy`
    - `ru.kode.android.build.publish.plugin.core.strategy.FixedVersionCodeStrategy { ... }`

- **`outputApkNameStrategy { ... }`**
  - **What it does**: Defines how the final APK file name is computed.
  - **Why you need it**: Produces consistent artifact names for distribution/upload steps.
  - **Groovy DSL note**: Use `new` when the strategy is a class, for example
    `outputApkNameStrategy { new VersionedApkNamingStrategy() }`.
  - **Common choices**:
    - `ru.kode.android.build.publish.plugin.core.strategy.VersionedApkNamingStrategy`
    - `ru.kode.android.build.publish.plugin.core.strategy.SimpleApkNamingStrategy`
    - `ru.kode.android.build.publish.plugin.core.strategy.FixedApkNamingStrategy { ... }`

##### Changelog (`buildPublishFoundation { changelog { ... } }`)

Changelog config defines how commit messages are filtered and how issue links are rendered.

- **`issueNumberPattern`**
  - **What it does**: Regex that finds issue identifiers in commit messages.
  - **Why you need it**: Enables clickable issue references in generated changelog.

- **`issueUrlPrefix`**
  - **What it does**: Prefix for creating issue URLs.
  - **Why you need it**: Converts issue IDs into full links.

- **`commitMessageKey`**
  - **What it does**: Marker used to include only selected commits into changelog.
  - **Why you need it**: Keeps changelog clean (only user-facing changes).

- **`excludeMessageKey`** *(default: `true`)*
  - **What it does**: Removes the marker key from the final changelog text.
  - **Why you need it**: Lets you keep markers in Git history without exposing them to end users.

#### Tasks

| Task Name | Description | Depends On |
|-----------|-------------|------------|
| `getLastTagSnapshot<Variant>` | Finds the last matching Git tag and writes a JSON snapshot file | - |
| `computeVersionCode<Variant>` | Computes `versionCode` (from tag or fallback) and writes it to a file | `getLastTagSnapshot<Variant>` |
| `computeVersionName<Variant>` | Computes `versionName` (from tag or fallback) and writes it to a file | `getLastTagSnapshot<Variant>` |
| `computeApkOutputFileName<Variant>` | Computes the final APK output file name and writes it to a file | `getLastTagSnapshot<Variant>` |
| `renameApk<Variant>` | AGP artifact transform: copies/renames the produced APK to the computed output name | `computeApkOutputFileName<Variant>` |
| `printLastIncreasedTag<Variant>` | Prints the next tag name (build number increment) based on the snapshot | `getLastTagSnapshot<Variant>` |
| `generateChangelog<Variant>` | Generates a changelog between last tag and HEAD | `getLastTagSnapshot<Variant>` |

#### Task Usage Examples

```bash
# Get last tag for debug variant
./gradlew getLastTagSnapshotDebug

# Generate changelog for release variant
./gradlew generateChangelogRelease

# Print last increased tag for staging variant
./gradlew printLastIncreasedTagStaging
```

---

### 2. Firebase Plugin (`ru.kode.android.build-publish-novo.firebase`)

Publish builds to Firebase App Distribution with support for multiple variants and tester groups.

#### Key Features
- Publish APK/AAB to Firebase App Distribution
- Support for multiple build variants
- Tester group management
- Release notes from changelog
- Integration with Firebase service accounts

#### Tasks

| Task Name | Description | Depends On |
|-----------|-------------|------------|
| `appDistributionUpload<Variant>` | Uploads the current variant artifact to Firebase App Distribution | Created by the official `com.google.firebase.appdistribution` plugin |

#### Task Usage Examples

```bash
# Upload debug build to Firebase
./gradlew appDistributionUploadDebug
```

#### Minimum Setup

1. Add Firebase configuration to your project:
   - Add `google-services.json` to your app module
   - Add Firebase App Distribution plugin to your root build script (so the plugin is on the classpath):

     Kotlin DSL:
     ```kotlin
     plugins {
         id("com.google.firebase.appdistribution") version "<your-firebase-appdistribution-version>"
     }
     ```

     Groovy DSL:
     ```groovy
     plugins {
         id 'com.google.firebase.appdistribution' version '<your-firebase-appdistribution-version>'
     }
     ```

2. Configure the plugin:
   
##### Kotlin DSL (`build.gradle.kts`)

   ```kotlin
   // app/build.gradle.kts
   plugins {
       id("com.android.application")
       id("ru.kode.android.build-publish-novo.foundation")
       id("ru.kode.android.build-publish-novo.firebase")
   }
   
   buildPublishFirebase {
       distribution {
           common {
               appId.set("your-firebase-app-id")
               serviceCredentialsFile.set(file("path/to/service-account.json"))
               artifactType.set(ArtifactType.Bundle)
               testerGroup("qa-team")
           }
       }
   }
   ```

##### Groovy DSL (`build.gradle`)

```groovy
// app/build.gradle
plugins {
    id 'com.android.application'
    id 'ru.kode.android.build-publish-novo.foundation'
    id 'ru.kode.android.build-publish-novo.firebase'
}

buildPublishFirebase {
    distribution {
        common {
            it.appId.set('your-firebase-app-id')
            it.serviceCredentialsFile.set(file('path/to/service-account.json'))
            it.artifactType.set(ru.kode.android.build.publish.plugin.firebase.config.ArtifactType.Bundle)
            it.testerGroup('qa-team')
        }
    }
}
```

The Firebase plugin configures the official Firebase App Distribution Gradle plugin. Upload tasks are
created by Firebase itself and typically look like:

- `appDistributionUpload<Variant>`

#### Full Configuration

##### Kotlin DSL (`build.gradle.kts`)

```kotlin
buildPublishFirebase {
   distribution {
      common {
         appId.set("your-firebase-app-id")
         serviceCredentialsFile.set(file("path/to/service-account.json"))
         artifactType.set(ArtifactType.Bundle)
         testerGroups("qa-team", "developers")
      }

      buildVariant("release") {
         testerGroup("beta-testers")
      }
   }
}
```

##### Groovy DSL (`build.gradle`)

```groovy
buildPublishFirebase {
    distribution {
        common {
            it.appId.set('your-firebase-app-id')
            it.serviceCredentialsFile.set(file('path/to/service-account.json'))
            it.artifactType.set(ru.kode.android.build.publish.plugin.firebase.config.ArtifactType.Bundle)
            it.testerGroups('qa-team', 'developers')
        }

        buildVariant('release') {
            it.testerGroup('beta-testers')
        }
    }
}
```

#### Configuration Reference

##### Important behavior / nuances

- **Foundation plugin is required**
  - `buildPublishFirebase` wires Firebase `releaseNotesFile` from the foundation changelog output.
  - Apply `ru.kode.android.build-publish-novo.foundation` and configure `buildPublishFoundation { changelog { ... } }` if you want meaningful release notes.

- **The Firebase App Distribution Gradle plugin is applied conditionally**
  - This plugin applies the official `com.google.firebase.appdistribution` plugin **only if** at least one `distribution { ... }` config is declared.

- **Distribution config must exist per-variant**
  - During variant configuration, if there is no `common { ... }` (or no matching `buildVariant("<name>") { ... }`) the build fails with an explicit error asking you to add distribution config.

##### Distribution (`buildPublishFirebase { distribution { ... } }`)

Configure distribution per Android variant using:

- `common { ... }` for defaults applied to all variants
- `buildVariant("release") { ... }` to override for a single variant

Properties (applies to each `FirebaseDistributionConfig`):

- **`appId`** *(required)*
  - **What it does**: Firebase App ID to upload to.
  - **Why you need it**: Firebase App Distribution requires a target application.
  - **Where to get it**: Firebase Console `Project settings -> General` (format like `1:1234567890:android:...`).

- **`serviceCredentialsFile`** *(required)*
  - **What it does**: Service account JSON used to authenticate uploads.
  - **Why you need it**: Upload requires server-side credentials.
  - **How to use**: Store outside VCS and pass via `file("...")`.

- **`artifactType`** *(required)*
  - **What it does**: Chooses which artifact to upload.
  - **Values**:
    - `ArtifactType.Apk`
    - `ArtifactType.Bundle`
  - **Why you need it**: Firebase needs to know whether to upload APK or AAB.

- **`testerGroup("...")` / `testerGroups(...)`** *(optional)*
  - **What it does**: Defines which Firebase tester groups receive the release.
  - **Why you need it**: Automates targeting QA/beta groups.
  - **Notes**: Groups must exist in Firebase Console.

---

### 3. Play Store Plugin (`ru.kode.android.build-publish-novo.play`)

Publish builds to Google Play Store with support for multiple tracks and release types.

This integration is based on ideas and implementation details from the community plugin
https://github.com/Triple-T/gradle-play-publisher, but the logic is adapted to this repository’s
variant-driven build-publish flow and could not be used as-is.

#### Key Features
- Publish to Google Play Console
- Support for multiple tracks (internal, alpha, beta, production)
- Release management (draft, in progress, completed)
- Support for release notes in multiple languages
- Integration with Google Play service account

#### Tasks

| Task Name | Description | Depends On |
|-----------|-------------|------------|
| `playUpload<Variant>` | Uploads a bundle (`.aab`) to Google Play | `bundle<Variant>`, `getLastTagSnapshot<Variant>` |

#### Task Usage Examples

```bash
# Upload release bundle to internal testing track
./gradlew playUploadRelease

# Override track via CLI options
./gradlew playUploadRelease --trackId=internal --updatePriority=0
```

#### Minimum Setup

1. Create a service account in Google Play Console
2. Download the JSON key file and add it to your project
3. Configure the plugin:

##### Kotlin DSL (`build.gradle.kts`)

   ```kotlin
   // app/build.gradle.kts
   plugins {
       id("com.android.application")
       id("ru.kode.android.build-publish-novo.foundation")
       id("ru.kode.android.build-publish-novo.play")
   }
   
   buildPublishPlay {
       auth {
           common {
               appId.set("com.example.app")
               apiTokenFile.set(file("play-account.json"))
           }
       }

       distribution {
           common {
               trackId.set("internal")
               updatePriority.set(0)
           }
       }
   }
   ```

#### Full Configuration

##### Kotlin DSL (`build.gradle.kts`)

```kotlin
buildPublishPlay {
    auth {
        common {
            appId.set("com.example.app")
            apiTokenFile.set(file("play-account.json"))
        }
    }

    distribution {
        common {
            trackId.set("internal")
            updatePriority.set(0)
        }

        buildVariant("release") {
            trackId.set("production")
            updatePriority.set(1)
        }
    }
}
```

##### Groovy DSL (`build.gradle`)

```groovy
buildPublishPlay {
    auth {
        common {
            it.appId.set('com.example.app')
            it.apiTokenFile.set(file('play-account.json'))
        }
    }

    distribution {
        common {
            it.trackId.set('internal')
            it.updatePriority.set(0)
        }

        buildVariant('release') {
            it.trackId.set('production')
            it.updatePriority.set(1)
        }
    }
}
```

#### Configuration Reference

##### Important behavior / nuances

- **Foundation plugin is required**
  - The Play plugin registers the `playUpload<Variant>` task from `BuildPublishPlayExtension.configure(...)`.
  - The foundation plugin is responsible for invoking `configure(...)` for all `BuildPublishConfigurableExtension` instances, so Play tasks appear only if `ru.kode.android.build-publish-novo.foundation` is applied.

- **Both `auth` and `distribution` must be configured**
  - `auth` is used to create Play API network services.
  - `distribution` is required to configure track/priority.
  - If there is no matching `common { ... }` (the internal common name is `default`) or no matching `buildVariant("<name>") { ... }`, the build fails fast with a “required configuration not found” error.

- **Uploads only support AAB**
  - `playUpload<Variant>` uploads an Android App Bundle (`.aab`). If the input file is not `.aab`, the task fails.

##### Auth (`buildPublishPlay { auth { ... } }`)

Configure Play authentication per variant using:

- `common { ... }` for defaults applied to all variants
- `buildVariant("release") { ... }` to override for a single variant

Properties (applies to each `PlayAuthConfig`):

- **`appId`** *(required)*
  - **What it does**: The applicationId / package name of the app in Google Play Console.
  - **Why you need it**: Used to target the correct app when calling Play Developer API.

- **`apiTokenFile`** *(required)*
  - **What it does**: Service account JSON key file.
  - **Why you need it**: Required to authenticate Play Developer API requests.
  - **How to use**: Store outside VCS and pass via `file("...")`.

##### Distribution (`buildPublishPlay { distribution { ... } }`)

Configure distribution per variant using:

- `common { ... }` for defaults applied to all variants
- `buildVariant("release") { ... }` to override for a single variant

Properties (applies to each `PlayDistributionConfig`):

- **`trackId`** *(default: `internal`)*
  - **What it does**: Target track to publish to.
  - **Why you need it**: Different tracks are used for internal/alpha/beta/production flows.
  - **Typical values**: `internal`, `alpha`, `beta`, `production`.

- **`updatePriority`** *(default: `0`)*
  - **What it does**: In-app update priority (`0..5`) sent with the release.
  - **Why you need it**: Allows controlling update urgency for supported update flows.

##### Task options (`playUpload<Variant>`)

The upload task supports overriding some inputs via CLI (Gradle task options):

- `--trackId=internal`
- `--updatePriority=0`

The task is wired by default to:

- `getLastTagSnapshot<Variant>` (to compute release name metadata)

Note: `playUpload<Variant>` consumes the bundle output via AGP artifacts (`SingleArtifact.BUNDLE`).

---

### 4. Slack Plugin (`ru.kode.android.build-publish-novo.slack`)

Send build notifications to Slack channels with detailed build information.

#### Key Features
- Send build notifications to Slack
- Customizable message templates
- Support for multiple channels
- Build status and download links
- Changelog preview

#### Tasks

| Task Name | Description | Depends On |
|-----------|-------------|------------|
| `sendSlackChangelog<Variant>` | Sends the generated changelog to Slack | `generateChangelog<Variant>`, `getLastTagSnapshot<Variant>` |
| `slackDistributionUpload<Variant>` | Uploads APK to Slack channels | `getLastTagSnapshot<Variant>` |
| `slackDistributionUploadBundle<Variant>` | Uploads bundle (`.aab`) to Slack channels | `getLastTagSnapshot<Variant>` |

#### Task Usage Examples

```bash
# Send changelog to Slack
./gradlew sendSlackChangelogRelease

# Upload APK to Slack
./gradlew slackDistributionUploadDebug
```

#### Minimum Setup

1. Create a Slack webhook URL:
   - Go to https://api.slack.com/apps
   - Create a new app and enable Incoming Webhooks
   - Add the webhook to your workspace

2. Configure the plugin:

##### Kotlin DSL (`build.gradle.kts`)

   ```kotlin
   // app/build.gradle.kts
   plugins {
       id("com.android.application")
       id("ru.kode.android.build-publish-novo.foundation")
       id("ru.kode.android.build-publish-novo.slack")
   }
   
   buildPublishSlack {
       bot {
           common {
               webhookUrl.set("https://hooks.slack.com/services/...")
               uploadApiTokenFile.set(file("slack-upload-token.txt"))
               iconUrl.set("https://example.com/bot.png")
           }
       }

       distribution {
           common {
               destinationChannel("#releases")
           }
       }
   }
   ```

##### Groovy DSL (`build.gradle`)

```groovy
// app/build.gradle
plugins {
    id 'com.android.application'
    id 'ru.kode.android.build-publish-novo.foundation'
    id 'ru.kode.android.build-publish-novo.slack'
}

buildPublishSlack {
    bot {
        common {
            it.webhookUrl.set('https://hooks.slack.com/services/...')
            it.uploadApiTokenFile.set(file('slack-upload-token.txt'))
            it.iconUrl.set('https://example.com/bot.png')
        }
    }

    distribution {
        common {
            it.destinationChannel('#releases')
        }
    }
}
```

#### Full Configuration

##### Kotlin DSL (`build.gradle.kts`)

```kotlin
buildPublishSlack {
    bot {
        common {
            webhookUrl.set("https://hooks.slack.com/services/...")
            uploadApiTokenFile.set(file("slack-upload-token.txt"))
            iconUrl.set("https://example.com/bot.png")
        }

        buildVariant("release") {
            webhookUrl.set("https://hooks.slack.com/services/...")
            uploadApiTokenFile.set(file("slack-upload-token.txt"))
            iconUrl.set("https://example.com/release-bot.png")
        }
    }

    changelog {
        common {
            attachmentColor.set("#36a64f")
            userMention("@here")
        }

        buildVariant("release") {
            attachmentColor.set("#3aa3e3")
            userMentions("@channel")
        }
    }

    distribution {
        common {
            destinationChannels("#releases")
        }

        buildVariant("debug") {
            destinationChannels("#android-team")
        }
    }
}
```

##### Groovy DSL (`build.gradle`)

```groovy
buildPublishSlack {
    bot {
        common {
            it.webhookUrl.set('https://hooks.slack.com/services/...')
            it.uploadApiTokenFile.set(file('slack-upload-token.txt'))
            it.iconUrl.set('https://example.com/bot.png')
        }

        buildVariant('release') {
            it.webhookUrl.set('https://hooks.slack.com/services/...')
            it.uploadApiTokenFile.set(file('slack-upload-token.txt'))
            it.iconUrl.set('https://example.com/release-bot.png')
        }
    }

    changelog {
        common {
            it.attachmentColor.set('#36a64f')
            it.userMention('@here')
        }

        buildVariant('release') {
            it.attachmentColor.set('#3aa3e3')
            it.userMentions('@channel')
        }
    }

    distribution {
        common {
            it.destinationChannels('#releases')
        }

        buildVariant('debug') {
            it.destinationChannels('#android-team')
        }
    }
}
```

#### Configuration Reference

##### Important behavior / nuances

- **Foundation plugin is required**
  - The Slack plugin fails fast if `ru.kode.android.build-publish-novo.foundation` is not applied.

- **A bot configuration is always required**
  - For each variant, Slack requires `bot.common { ... }` (internally `default`) or `bot.buildVariant("<name>") { ... }`.
  - If bot config is missing for a variant, configuration fails.

- **At least one of `changelog` or `distribution` must be configured**
  - If both are missing for a variant, configuration fails.

- **Distribution tasks are registered only when channels are configured**
  - `slackDistributionUpload<Variant>` / `slackDistributionUploadBundle<Variant>` are created only when `distribution { ... }` has at least one destination channel.

- **`uploadApiTokenFile` is required when you run distribution uploads**
  - The plugin may still register distribution tasks without a token file, but execution will fail when the Slack API token is missing.

- **Slack upload can time out but still succeed**
  - Slack’s API may return a timeout even if the file is uploaded successfully; the plugin logs a warning for this case.

##### Bot (`buildPublishSlack { bot { ... } }`)

Configure bot connection details per variant.

Properties (applies to each `SlackBotConfig`):

- **`webhookUrl`** *(required)*
  - **What it does**: Slack Incoming Webhook URL used to post changelog messages.
  - **Why you need it**: Used by `sendSlackChangelog<Variant>`.

- **`uploadApiTokenFile`** *(required for file uploads)*
  - **What it does**: File containing a Slack bot/user token for file uploads.
  - **Why you need it**: Required by `slackDistributionUpload*` tasks.

- **`iconUrl`** *(required for changelog messages)*
  - **What it does**: Icon URL for Slack message sender.
  - **Why you need it**: Used by `sendSlackChangelog<Variant>`.

##### Changelog (`buildPublishSlack { changelog { ... } }`)

Properties (applies to each `SlackChangelogConfig`):

- **`attachmentColor`** *(required)*
  - **What it does**: Hex color used for Slack attachment stripe (e.g. `#36a64f`).
  - **Why you need it**: Helps visually identify message type.

- **`userMention(...)` / `userMentions(...)`**
  - **What it does**: Adds mentions (e.g. `@here`, `@channel`, `@username`) to the message.
  - **Why you need it**: Notifies specific people/groups about a release.

##### Distribution (`buildPublishSlack { distribution { ... } }`)

Properties (applies to each `SlackDistributionConfig`):

- **`destinationChannel(...)` / `destinationChannels(...)`** *(required to create upload tasks)*
  - **What it does**: Sets channels where artifacts will be shared.
  - **Common values**: `#releases`, `#android-team`.

##### Task options

- **`sendSlackChangelog<Variant>`** supports (CLI options):
  - `--changelogFile=/abs/path/to/changelog.md`
  - `--buildTagSnapshotFile=/abs/path/to/tag.json`
  - `--baseOutputFileName=MyApp`
  - `--issueUrlPrefix=https://tracker/browse/`
  - `--issueNumberPattern=([A-Z]+-\\d+)`
  - `--iconUrl=https://example.com/icon.png`
  - `--userMentions=@here`
  - `--attachmentColor=#36a64f`

- **`slackDistributionUpload<Variant>` / `slackDistributionUploadBundle<Variant>`** supports (CLI options):
  - `--distributionFile=/abs/path/to/app.apk` (or `.aab`)
  - `--buildTagSnapshotFile=/abs/path/to/tag.json`
  - `--baseOutputFileName=MyApp`
  - `--channels=#releases`

---

### 5. Telegram Plugin (`ru.kode.android.build-publish-novo.telegram`)

Send build notifications to Telegram channels or groups.

#### Key Features
- Send build notifications to Telegram
- Support for both public and private channels
- Custom message formatting
- Download links and build information
- Changelog preview

#### Tasks

| Task Name | Description | Depends On |
|-----------|-------------|------------|
| `sendTelegramChangelog<Variant>` | Sends generated changelog to Telegram | `generateChangelog<Variant>`, `getLastTagSnapshot<Variant>` |
| `telegramDistributionUpload<Variant>` | Sends APK distribution notification to Telegram | - |
| `telegramDistributionUploadBundle<Variant>` | Sends bundle distribution notification to Telegram | - |
| `telegramLookup<Variant>` | Helps discover chat/topic identifiers via configured bot | - |

#### Task Usage Examples

```bash
# Send changelog
./gradlew sendTelegramChangelogRelease

# Lookup chat/topic IDs (if lookup is configured)
./gradlew telegramLookupDebug
```

#### Minimum Setup

1. Create a Telegram bot if it is not exists:
   - Message @BotFather on Telegram
   - Use `/newbot` command and follow instructions
   - Get your bot token

2. Discover `chatId` and `topicId` using `telegramLookup<Variant>`:
   - Add your bot to the target channel/group/topic
   - Send **any** message to that chat/topic from your Telegram account
   - Configure `buildPublishTelegram.lookup { ... }` (see Full Configuration below) with:
     - `botName` = your configured bot name (for example `main`)
     - `chatName` = part of the chat title to match
     - `topicName` = (optional) part of the topic title to match
   - Run the lookup task, for example:
     - `./gradlew telegramLookupDebug`
   - The task prints `Chat ID` and `Topic ID` in the build output. Copy those values into:
     - `bots { ... bot("...") { chat("...") { chatId.set("..."); topicId.set("...") } } }`

3. Configure the plugin:

##### Kotlin DSL (`build.gradle.kts`)

   ```kotlin
   // app/build.gradle.kts
   plugins {
       id("com.android.application")
       id("ru.kode.android.build-publish-novo.foundation")
       id("ru.kode.android.build-publish-novo.telegram")
   }
   
   buildPublishTelegram {
       bots {
           common {
               bot("main") {
                   botId.set("your-bot-token")
                   chat("releases") {
                       chatId.set("@your-channel")
                   }
               }
           }
       }

       changelog {
           common {
               destinationBot {
                   botName.set("main")
                   chatName("releases")
               }
           }
       }
   }
   ```

##### Groovy DSL (`build.gradle`)

```groovy
// app/build.gradle
plugins {
    id 'com.android.application'
    id 'ru.kode.android.build-publish-novo.foundation'
    id 'ru.kode.android.build-publish-novo.telegram'
}

buildPublishTelegram {
    bots {
        common {
            bot('main') {
                it.botId.set('your-bot-token')

                chat('releases') {
                    it.chatId.set('@your-channel')
                }
            }
        }
    }

    changelog {
        common {
            destinationBot {
                it.botName.set('main')
                it.chatName('releases')
            }
        }
    }
}
```

#### Full Configuration

##### Kotlin DSL (`build.gradle.kts`)

```kotlin
buildPublishTelegram {
    bots {
        common {
            bot("main") {
                botId.set("your-bot-token")
              
                // Optional: for self-hosted Bot API
                // botServerBaseUrl.set('https://telegram-bot-api.your-company.net')
                // botServerAuth.username.set(providers.environmentVariable('TELEGRAM_AUTH_USER'))
                // botServerAuth.password.set(providers.environmentVariable('TELEGRAM_AUTH_PASSWORD'))
  
                chat("releases") {
                      chatId.set("@your-channel")
                      topicId.set("123")
                }
            }
        }
    }

    // Optional helper task configuration (enables telegramLookup<Variant>)
    lookup {
        botName.set("main")
        chatName.set("releases")
        topicName.set("Android releases")
    }

    changelog {
        common {
            userMention("@dev1")
            destinationBot {
                botName.set("main")
                chatName("releases")
            }
        }
    }

    distribution {
        common {
            destinationBot {
                botName.set("main")
                chatName("releases")
            }
        }
    }
}
```

##### Groovy DSL (`build.gradle`)

```groovy
buildPublishTelegram {
    bots {
        common {
            it.bot('main') {
                it.botId.set('your-bot-token')

                // Optional: for self-hosted Bot API
                // it.botServerBaseUrl.set('https://telegram-bot-api.your-company.net')
                // it.botServerAuth.username.set(providers.environmentVariable('TELEGRAM_AUTH_USER'))
                // it.botServerAuth.password.set(providers.environmentVariable('TELEGRAM_AUTH_PASSWORD'))

                chat('releases') {
                    it.chatId.set('@your-channel')
                    it.topicId.set('123')
                }
            }
        }
    }

    // Optional helper task configuration (enables telegramLookup<Variant>)
    lookup {
        it.botName.set('main')
        it.chatName.set('releases')
        it.topicName.set('Android releases')
    }

    changelog {
        common {
            it.userMention('@dev1')
            it.userMentions('@qa_team')

            it.destinationBot {
                it.botName.set('main')
                it.chatName('releases')
            }
        }
    }

    distribution {
        common {
            it.destinationBot {
                it.botName.set('main')
                it.chatName('releases')
            }
        }
    }
}
```

#### Configuration Reference

##### Important behavior / nuances

- **Foundation plugin is required**
  - The Telegram plugin fails fast if `ru.kode.android.build-publish-novo.foundation` is not applied.

- **`bots { ... }` is always required**
  - If no bots are configured, plugin configuration fails.

- **At least one of `changelog`, `distribution`, or `lookup` must be configured**
  - If all three blocks are missing, plugin configuration fails.

- **Distribution tasks are registered only when destination bots exist**
  - `telegramDistributionUpload<Variant>` / `telegramDistributionUploadBundle<Variant>` are registered only when `distribution { ... }` has at least one `destinationBot { ... }`.

- **`telegramLookup<Variant>` exists only when `lookup { ... }` is configured**

##### Bots (`buildPublishTelegram { bots { ... } }`)

The Telegram plugin uses a two-level DSL:

1. **Select a build variant** via `common { ... }` or `buildVariant("release") { ... }`.
2. Inside that, register one or more bots with `bot("<name>") { ... }`.

Properties (applies to each `TelegramBotConfig`):

- **`botId`** *(required)*
  - **What it does**: Telegram bot token.
  - **Why you need it**: Used to authenticate Bot API calls.
  - **How to get**: Create bot via `@BotFather`.

- **`botServerBaseUrl`** *(optional)*
  - **What it does**: Base URL for a self-hosted Telegram Bot API server.
  - **Why you need it**: Only if you’re not using the official `https://api.telegram.org`.
  - **When it can be needed**:
    - If you need to upload large artifacts (for example, `.apk`/`.aab` files that are bigger than typical Bot API limits).
    - Telegram’s official Bot API docs explicitly state that a local Bot API server enables:
      - Uploads up to **2000 MB**
      - Downloads without a size limit
    - Docs: https://core.telegram.org/bots/api#using-a-local-bot-api-server
    - Server implementation (TDLib): https://github.com/tdlib/telegram-bot-api

- **`botServerAuth.username` / `botServerAuth.password`** *(optional)*
  - **What it does**: HTTP Basic Auth credentials for a protected self-hosted Bot API server.
  - **Why you need it**: Only if your Bot API endpoint requires basic auth.

Chats (inside `bot("...") { chat("...") { ... } }`):

- **`chatId`** *(required)*
  - **What it does**: Telegram chat identifier.
  - **Typical values**: `@channelusername`, `-1001234567890`, `123456789`.

- **`topicId`** *(optional)*
  - **What it does**: Thread/topic id in a forum-style chat.
  - **Why you need it**: To send messages to a specific topic.

##### Changelog (`buildPublishTelegram { changelog { ... } }`)

Properties (applies to each `TelegramChangelogConfig`):

- **`userMention(...)` / `userMentions(...)`**
  - **What it does**: Adds mentions (for example `@dev1`) to the message.
  - **Why you need it**: Notifies specific users.

- **`destinationBot { ... }`** *(required to actually send changelog)*
  - **What it does**: Selects which configured bot sends, and which named chats receive messages.
  - **How to use**:
    - `botName.set("main")`
    - `chatName("releases")` (repeat or use `chatNames(...)` for multiple)

##### Distribution (`buildPublishTelegram { distribution { ... } }`)

Properties (applies to each `TelegramDistributionConfig`):

- **`destinationBot { ... }`** *(required to create upload tasks)*
  - **What it does**: Selects bot + chats where the artifact will be uploaded.
  - **Note**: The task uploads the file you provide (APK or AAB) to all configured destinations.

##### Lookup (`buildPublishTelegram { lookup { ... } }`)

Lookup is an optional helper task to debug/verify your bot/chat/topic configuration.

Properties (applies to `TelegramLookupConfig`):

- **`botName`** *(required)*
- **`chatName`** *(required)*
- **`topicName`** *(optional)*

##### Task options

- **`sendTelegramChangelog<Variant>`** supports (CLI options):
  - `--changelogFile=/abs/path/to/changelog.md`
  - `--buildTagSnapshotFile=/abs/path/to/tag.json`
  - `--baseOutputFileName=MyApp`
  - `--issueUrlPrefix=https://tracker/browse/`
  - `--issueNumberPattern=([A-Z]+-\\d+)`
  - `--userMentions=@dev1`
  - `--destinationBots=<json>`

- **`telegramDistributionUpload<Variant>` / `telegramDistributionUploadBundle<Variant>`** supports (CLI options):
  - `--distributionFile=/abs/path/to/app.apk` (or `.aab`)
  - `--destinationBots=<json>`

- **`telegramLookup<Variant>`** supports (CLI options):
  - `--botName=main`
  - `--chatName=releases`
  - `--topicName=Android releases`

---

### 6. Jira Plugin (`ru.kode.android.build-publish-novo.jira`)

Update Jira tickets with build information.

#### Tasks

| Task Name | Description | Depends On |
|-----------|-------------|------------|
| `jiraAutomation<Variant>` | Applies Jira automation based on issues found in the changelog | `generateChangelog<Variant>`, `getLastTagSnapshot<Variant>` |

#### Task Usage Examples

```bash
# Run Jira automation for the release variant
./gradlew jiraAutomationRelease
```

#### Minimum Setup

1. Configure Jira credentials (Jira Cloud: use an API token as the password)
2. Configure at least one automation feature (label, fix version, or status transition)

##### Kotlin DSL (`build.gradle.kts`)

```kotlin
// app/build.gradle.kts
plugins {
    id("com.android.application")
    id("ru.kode.android.build-publish-novo.foundation")
    id("ru.kode.android.build-publish-novo.jira")
}

buildPublishJira {
    auth {
        common {
            baseUrl.set("https://your-domain.atlassian.net")
            credentials.username.set("your-email@example.com")
            credentials.password.set(providers.environmentVariable("JIRA_API_TOKEN"))
        }
    }

    automation {
        common {
            projectKey.set("PROJ")

            // Enable at least one automation action
            targetStatusName.set("Ready for QA")
        }
    }
}
```

##### Groovy DSL (`build.gradle`)

```groovy
// app/build.gradle
plugins {
    id 'com.android.application'
    id 'ru.kode.android.build-publish-novo.foundation'
    id 'ru.kode.android.build-publish-novo.jira'
}

buildPublishJira {
    auth {
        common {
            it.baseUrl.set('https://your-domain.atlassian.net')
            it.credentials.username.set('your-email@example.com')
            it.credentials.password.set(providers.environmentVariable('JIRA_API_TOKEN'))
        }
    }

    automation {
        common {
            it.projectKey.set('PROJ')

            // Enable at least one automation action
            it.targetStatusName.set('Ready for QA')
        }
    }
}
```

#### Full Configuration

##### Kotlin DSL (`build.gradle.kts`)

```kotlin
buildPublishJira {
    auth {
        common {
            baseUrl.set("https://your-domain.atlassian.net")
            credentials.username.set("your-email@example.com")
            credentials.password.set(providers.environmentVariable("JIRA_API_TOKEN"))
        }
    }

    automation {
        common {
            projectKey.set("PROJ")

            // Label / fix version patterns use String.format(...)
            // format args order: buildVersion, buildNumber, buildVariant
            labelPattern.set("android-%s-%d")
            fixVersionPattern.set("%s")

            targetStatusName.set("Ready for QA")
        }

        buildVariant("release") {
            projectKey.set("PROJ")
            labelPattern.set("release-%s")
            fixVersionPattern.set("%s")
            targetStatusName.set("Ready for Release")
        }
    }
}
```

##### Groovy DSL (`build.gradle`)

```groovy
buildPublishJira {
    auth {
        common {
            it.baseUrl.set('https://your-domain.atlassian.net')
            it.credentials.username.set('your-email@example.com')
            it.credentials.password.set(providers.environmentVariable('JIRA_API_TOKEN'))
        }
    }

    automation {
        common {
            it.projectKey.set('PROJ')

            // format args order: buildVersion, buildNumber, buildVariant
            it.labelPattern.set('android-%s-%d')
            it.fixVersionPattern.set('%s')

            it.targetStatusName.set('Ready for QA')
        }

        buildVariant('release') {
            it.projectKey.set('PROJ')
            it.labelPattern.set('release-%s')
            it.fixVersionPattern.set('%s')
            it.targetStatusName.set('Ready for Release')
        }
    }
}
```

#### Configuration Reference

##### Important behavior / nuances

- **Foundation plugin is required**
  - The Jira plugin fails fast if `ru.kode.android.build-publish-novo.foundation` is not applied.

- **Auth configuration is required**
  - At least `auth.common { ... }` (internal common name is `default`) must be configured.

- **Automation configuration is required per variant**
  - If there is no matching `automation.common { ... }` or `automation.buildVariant("<name>") { ... }`, configuration fails.

- **The task is created only if at least one automation action is enabled**
  - `jiraAutomation<Variant>` is registered only if at least one of:
    - `labelPattern`
    - `fixVersionPattern`
    - `targetStatusName`
    is set.

- **Issue keys are extracted from the changelog**
  - The task scans the generated changelog file using the foundation `issueNumberPattern`.
  - If no issues are found, the task logs an info message and does nothing.

##### Auth (`buildPublishJira { auth { ... } }`)

Properties (applies to each `JiraAuthConfig`):

- **`baseUrl`** *(required)*
  - **What it does**: Base URL of your Jira instance.
  - **Examples**:
    - `https://your-domain.atlassian.net` (Cloud)
    - `https://jira.your-company.com` (Server/Data Center)

- **`credentials.username`** *(required)*
  - **What it does**: Username/email used for authentication.

- **`credentials.password`** *(required)*
  - **What it does**: Password or API token.
  - **Recommendation**: For Jira Cloud use an API token.

##### Automation (`buildPublishJira { automation { ... } }`)

Properties (applies to each `JiraAutomationConfig`):

- **`projectKey`** *(required)*
  - **What it does**: Jira project key (e.g. `PROJ`).
  - **Why you need it**: Required for version management and status transition lookup.

- **`labelPattern`** *(optional)*
  - **What it does**: Adds a computed label to each issue found in the changelog.
  - **How it works**: Uses `String.format(pattern, buildVersion, buildNumber, buildVariant)`.
  - **Example**: `android-%s-%d`.

- **`fixVersionPattern`** *(optional)*
  - **What it does**: Sets (and creates if needed) a fix version on each issue.
  - **How it works**: Uses `String.format(pattern, buildVersion, buildNumber, buildVariant)`.
  - **Example**: `%s` (use only version), or `android-%s`.

- **`targetStatusName`** *(optional)*
  - **What it does**: Transitions issues to the given status (by looking up a matching transition).
  - **Example**: `Ready for QA`, `Ready for Release`.

##### Task options (`jiraAutomation<Variant>`)

The task supports overriding inputs via CLI options:

- `--buildTagSnapshotFile=/abs/path/to/tag.json`
- `--changelogFile=/abs/path/to/changelog.md`
- `--issueNumberPattern=([A-Z]+-\\d+)`
- `--projectKey=PROJ`
- `--labelPattern=android-%s-%d`
- `--fixVersionPattern=%s`
- `--targetStatusName=Ready for QA`

---

### 7. Confluence Plugin (`ru.kode.android.build-publish-novo.confluence`)

Update Confluence pages with release notes.

#### Tasks

| Task Name | Description | Depends On |
|-----------|-------------|------------|
| `confluenceDistributionUpload<Variant>` | Uploads APK to a Confluence page as an attachment and adds a comment | - |
| `confluenceDistributionUploadBundle<Variant>` | Uploads bundle (`.aab`) to a Confluence page as an attachment and adds a comment | - |

#### Task Usage Examples

```bash
# Upload APK for the release variant
./gradlew confluenceDistributionUploadRelease

# Upload bundle for the release variant
./gradlew confluenceDistributionUploadBundleRelease
```

#### Minimum Setup

1. Create Confluence API token (Confluence Cloud) or use your account password (Server/Data Center)
2. Find the Confluence page id (it is part of the page URL)
3. Configure the plugin:

##### Kotlin DSL (`build.gradle.kts`)

```kotlin
// app/build.gradle.kts
plugins {
    id("com.android.application")
    id("ru.kode.android.build-publish-novo.foundation")
    id("ru.kode.android.build-publish-novo.confluence")
}

buildPublishConfluence {
    auth {
        common {
            baseUrl.set("https://your-domain.atlassian.net/wiki")
            credentials.username.set("your-email@example.com")
            credentials.password.set(providers.environmentVariable("CONFLUENCE_API_TOKEN"))
        }
    }

    distribution {
        common {
            pageId.set("12345678")
        }
    }
}
```

##### Groovy DSL (`build.gradle`)

```groovy
// app/build.gradle
plugins {
    id 'com.android.application'
    id 'ru.kode.android.build-publish-novo.foundation'
    id 'ru.kode.android.build-publish-novo.confluence'
}

buildPublishConfluence {
    auth {
        common {
            it.baseUrl.set('https://your-domain.atlassian.net/wiki')
            it.credentials.username.set('your-email@example.com')
            it.credentials.password.set(providers.environmentVariable('CONFLUENCE_API_TOKEN'))
        }
    }

    distribution {
        common {
            it.pageId.set('12345678')
        }
    }
}
```

#### Full Configuration

##### Kotlin DSL (`build.gradle.kts`)

```kotlin
buildPublishConfluence {
    auth {
        common {
            baseUrl.set("https://your-domain.atlassian.net/wiki")
            credentials.username.set("your-email@example.com")
            credentials.password.set(providers.environmentVariable("CONFLUENCE_API_TOKEN"))
        }
    }

    distribution {
        common {
            pageId.set("12345678")
        }

        buildVariant("release") {
            pageId.set("87654321")
        }
    }
}
```

##### Groovy DSL (`build.gradle`)

```groovy
buildPublishConfluence {
    auth {
        common {
            it.baseUrl.set('https://your-domain.atlassian.net/wiki')
            it.credentials.username.set('your-email@example.com')
            it.credentials.password.set(providers.environmentVariable('CONFLUENCE_API_TOKEN'))
        }
    }

    distribution {
        common {
            it.pageId.set('12345678')
        }

        buildVariant('release') {
            it.pageId.set('87654321')
        }
    }
}
```

#### Configuration Reference

##### Important behavior / nuances

- **Foundation plugin is required**
  - The Confluence plugin fails fast if `ru.kode.android.build-publish-novo.foundation` is not applied.

- **Auth and distribution configuration are required**
  - `auth` must be configured (at least `auth.common { ... }`, internal common name is `default`).
  - `distribution` must be configured for each variant (`common { ... }` or `buildVariant("<name>") { ... }`).

- **The task uploads a file and adds a comment**
  - After successful upload, the plugin also posts a comment with the uploaded file name.

- **Ensure the artifact exists**
  - The task uses the variant output produced by the Android build. If the artifact was not built yet, run `assemble<Variant>` / `bundle<Variant>` first.

##### Auth (`buildPublishConfluence { auth { ... } }`)

Properties (applies to each `ConfluenceAuthConfig`):

- **`baseUrl`** *(required)*
  - **What it does**: Base URL of your Confluence instance.
  - **Common values**:
    - Cloud: `https://your-domain.atlassian.net/wiki`
    - Server/Data Center: `https://confluence.your-company.com`

- **`credentials.username`** *(required)*
  - **What it does**: Username/email used for authentication.

- **`credentials.password`** *(required)*
  - **What it does**: Password or API token.
  - **Recommendation**: For Confluence Cloud use an API token.

##### Distribution (`buildPublishConfluence { distribution { ... } }`)

Properties (applies to each `ConfluenceDistributionConfig`):

- **`pageId`** *(required)*
  - **What it does**: Id of the Confluence page where the file should be uploaded.
  - **How to get**: It is part of the page URL, for example:
    - `.../wiki/spaces/SPACE/pages/12345678/Page+Title` → `pageId = 12345678`

##### Task options

- **`confluenceDistributionUpload<Variant>` / `confluenceDistributionUploadBundle<Variant>`** supports (CLI options):
  - `--distributionFile=/abs/path/to/app.apk` (or `.aab`)
  - `--pageId=12345678`

---

### 8. ClickUp Plugin (`ru.kode.android.build-publish-novo.clickup`)

Update ClickUp tasks with build information.

#### Tasks

| Task Name | Description | Depends On |
|-----------|-------------|------------|
| `clickUpAutomation<Variant>` | Updates ClickUp tasks referenced in the changelog (tags / fix version custom field) | `generateChangelog<Variant>`, `getLastTagSnapshot<Variant>` |

#### Task Usage Examples

```bash
# Apply automation for the release variant
./gradlew clickUpAutomationRelease
```

#### Minimum Setup

1. Create a ClickUp API token (store it in a local file, don’t commit it)
2. Configure the plugin:

##### Kotlin DSL (`build.gradle.kts`)

```kotlin
// app/build.gradle.kts
plugins {
    id("com.android.application")
    id("ru.kode.android.build-publish-novo.foundation")
    id("ru.kode.android.build-publish-novo.clickup")
}

buildPublishClickUp {
    auth {
        common {
            apiTokenFile.set(file("clickup-token.txt"))
        }
    }

    automation {
        common {
            workspaceName.set("Your Workspace")

            // Enable at least one automation action
            tagPattern.set("%s")
        }
    }
}
```

##### Groovy DSL (`build.gradle`)

```groovy
// app/build.gradle
plugins {
    id 'com.android.application'
    id 'ru.kode.android.build-publish-novo.foundation'
    id 'ru.kode.android.build-publish-novo.clickup'
}

buildPublishClickUp {
    auth {
        common {
            it.apiTokenFile.set(file('clickup-token.txt'))
        }
    }

    automation {
        common {
            it.workspaceName.set('Your Workspace')

            // Enable at least one automation action
            it.tagPattern.set('%s')
        }
    }
}
```

#### Full Configuration

##### Kotlin DSL (`build.gradle.kts`)

```kotlin
buildPublishClickUp {
    auth {
        common {
            apiTokenFile.set(file("clickup-token.txt"))
        }
    }

    automation {
        common {
            workspaceName.set("Your Workspace")

            // Patterns use String.format(pattern, buildVersion, buildNumber, buildVariant)
            // Example: v1.2.3 / 42 / release
            tagPattern.set("release-%s")

            // Fix version automation requires BOTH properties below
            fixVersionPattern.set("%s")
            fixVersionFieldName.set("Fix Version")
        }

        buildVariant("release") {
            workspaceName.set("Your Workspace")
            tagPattern.set("%s")
            fixVersionPattern.set("%s")
            fixVersionFieldName.set("Fix Version")
        }
    }
}
```

##### Groovy DSL (`build.gradle`)

```groovy
buildPublishClickUp {
    auth {
        common {
            it.apiTokenFile.set(file('clickup-token.txt'))
        }
    }

    automation {
        common {
            it.workspaceName.set('Your Workspace')

            // format args order: buildVersion, buildNumber, buildVariant
            it.tagPattern.set('release-%s')

            // Fix version automation requires BOTH properties below
            it.fixVersionPattern.set('%s')
            it.fixVersionFieldName.set('Fix Version')
        }

        buildVariant('release') {
            it.workspaceName.set('Your Workspace')
            it.tagPattern.set('%s')
            it.fixVersionPattern.set('%s')
            it.fixVersionFieldName.set('Fix Version')
        }
    }
}
```

#### Configuration Reference

##### Important behavior / nuances

- **Foundation plugin is required**
  - The ClickUp plugin fails fast if `ru.kode.android.build-publish-novo.foundation` is not applied.

- **Auth configuration is required**
  - At least `auth.common { ... }` (internal common name is `default`) must be configured.

- **Automation configuration is required per variant**
  - If there is no matching `automation.common { ... }` or `automation.buildVariant("<name>") { ... }`, configuration fails.

- **The task is created only if at least one automation action is enabled**
  - `clickUpAutomation<Variant>` is registered only if:
    - `tagPattern` is set, or
    - both `fixVersionPattern` and `fixVersionFieldName` are set.

- **Fix version settings must be provided together**
  - If you set only one of `fixVersionPattern` / `fixVersionFieldName`, configuration fails.

- **Issue/task IDs are extracted from the changelog**
  - The task scans the generated changelog file using the foundation `issueNumberPattern`.
  - If no issues are found, the task logs an info message and does nothing.

##### Auth (`buildPublishClickUp { auth { ... } }`)

Properties (applies to each `ClickUpAuthConfig`):

- **`apiTokenFile`** *(required)*
  - **What it does**: File containing your ClickUp API token.
  - **Why you need it**: Used to authenticate ClickUp API requests.
  - **Notes**: Keep it out of VCS.

##### Automation (`buildPublishClickUp { automation { ... } }`)

Properties (applies to each `ClickUpAutomationConfig`):

- **`workspaceName`** *(required)*
  - **What it does**: ClickUp workspace name.
  - **Why you need it**: Used to resolve custom field ids for fix version updates.

- **`tagPattern`** *(optional)*
  - **What it does**: Adds a tag to each ClickUp task referenced in the changelog.
  - **How it works**: Uses `String.format(pattern, buildVersion, buildNumber, buildVariant)`.
  - **Example**: `release-%s`.

- **`fixVersionPattern`** *(optional, requires `fixVersionFieldName`)*
  - **What it does**: Computes a fix version value for each task.
  - **How it works**: Uses `String.format(pattern, buildVersion, buildNumber, buildVariant)`.
  - **Example**: `%s`.

- **`fixVersionFieldName`** *(optional, requires `fixVersionPattern`)*
  - **What it does**: Name of the ClickUp custom field where fix version will be written.
  - **Example**: `Fix Version`.

##### Task options (`clickUpAutomation<Variant>`)

The task supports overriding inputs via CLI options:

- `--workspaceName=Your Workspace`
- `--buildTagSnapshotFile=/abs/path/to/tag.json`
- `--changelogFile=/abs/path/to/changelog.md`
- `--issueNumberPattern=#(\\d+)`
- `--fixVersionPattern=%s`
- `--fixVersionFieldName=Fix Version`
- `--tagPattern=release-%s`

## Custom Plugin Development

The foundation plugin will automatically configure any Gradle extension that:

- is registered on the project via `project.extensions.create(...)`, and
- has a type that extends `ru.kode.android.build.publish.plugin.core.api.extension.BuildPublishConfigurableExtension`.

That mechanism is the intended extension point for adding custom behaviour.

### Creating a custom plugin that participates in variant configuration

1) Create a Gradle plugin (standard `java-gradle-plugin` setup).

2) Create an extension that extends `BuildPublishConfigurableExtension`:

```kotlin
abstract class BuildPublishMyExtension : BuildPublishConfigurableExtension() {
    abstract val enabled: Property<Boolean>

    override fun configure(project: Project, input: ExtensionInput, variant: ApplicationVariant) {
        if (!enabled.get()) return
        // Register tasks for input.buildVariant.name and wire them to input.output/input.changelog.
    }
}
```

3) Register the extension in your plugin and require the foundation plugin:

```kotlin
class BuildPublishMyPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.pluginManager.apply("ru.kode.android.build-publish-novo.foundation")
        project.extensions.create("buildPublishMy", BuildPublishMyExtension::class.java)
    }
}
```

### Attaching a custom plugin to a local project (separate module / separate repo)

If you develop your own Build Publish-compatible plugin in a separate module (or even a separate repository),
you can attach it to the main project without publishing to a remote repository.

#### Option A: `build-logic` / `build-conventions` (recommended for project-local conventions)

- Put your convention plugin in `build-logic` / `build-conventions`.
- Add Build Publish plugin artifacts to the convention module dependencies (see Installation section above).
- Apply your convention plugin from app modules.

This is best when the plugin is project-specific and you want it versioned together with the app.

#### Option B: Composite build (`includeBuild`) (recommended for reusable local plugins)

If your custom plugin is a standalone Gradle plugin project (uses `java-gradle-plugin`), you can attach it as a
**composite build**.

Kotlin DSL (`settings.gradle.kts`):

```kotlin
pluginManagement {
    includeBuild("../my-build-publish-plugin")
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}
```

Groovy DSL (`settings.gradle`):

```groovy
pluginManagement {
    includeBuild("../my-build-publish-plugin")
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}
```

After that you can apply the custom plugin by its id in any module:

```kotlin
plugins {
    id("com.android.application")
    id("ru.kode.android.build-publish-novo.foundation")
    id("your.custom.build-publish-plugin")
}
```

#### Option C: Publish to `mavenLocal()` (quick local testing)

If your custom plugin project has `maven-publish` configured, you can publish it locally:

- `./gradlew publishToMavenLocal`

Then add `mavenLocal()` to the repository list (either `pluginManagement.repositories` or normal `repositories`)
and apply the plugin by id.

This approach is convenient for local testing, but composite builds are usually a better long-term workflow.

## Troubleshooting

### Common Issues

#### Firebase Authentication Errors
```
> Failed to authenticate with Firebase
```
**Solution:**
1. Verify your `google-services.json` is in the correct location
2. Ensure the service account has the necessary permissions in Firebase Console
3. Check that the `appId` in your configuration matches your Firebase project

#### Play Store Upload Failures
```
> Failed to upload to Play Store: 403 Forbidden
```
**Solution:**
1. Verify your service account has the correct permissions in Google Play Console
2. Check that the package name in your app matches the one in Play Console
3. Ensure the service account is added to your app in Play Console

#### Changelog Generation Issues
```
> No Git tags found for changelog generation
```
**Solution:**
1. Make sure you have at least one Git tag
2. Verify your tag format matches the pattern in `buildTagPattern`
3. Run `git fetch --tags` to ensure all tags are available locally

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

### Development Setup

1. Clone the repository
2. Open in Android Studio or your favorite IDE
3. Run `./gradlew build` to build the project
4. Use `./gradlew publishToMavenLocal` to test your changes locally

## License

This project is licensed under the [MIT License](LICENSE).
