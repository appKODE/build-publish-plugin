[← Documentation](../README.md)

# Installation

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
buildpublish-nextcloud = { id = "ru.kode.android.build-publish-novo.nextcloud", version.ref = "build-publish" }
```

Then apply them in an app module.

Kotlin DSL (`build.gradle.kts`):

```kotlin
plugins {
    id("com.android.application")
    alias(libs.plugins.buildpublish.foundation)
    alias(libs.plugins.buildpublish.telegram)
    alias(libs.plugins.buildpublish.confluence)
    alias(libs.plugins.buildpublish.nextcloud)
}
```

Groovy DSL (`build.gradle`):

```groovy
plugins {
    id 'com.android.application'
    alias(libs.plugins.buildpublish.foundation)
    alias(libs.plugins.buildpublish.telegram)
    alias(libs.plugins.buildpublish.confluence)
    alias(libs.plugins.buildpublish.nextcloud)
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
buildpublish-nextcloud-plugin = { module = "ru.kode.android.build-publish-novo.nextcloud:ru.kode.android.build-publish-novo.nextcloud.gradle.plugin", version.ref = "build-publish" }
```

#### 3.2) Add dependencies in your convention plugins module

In `build-logic/build.gradle.kts` (or your `build-conventions` module), add plugin artifacts as dependencies:

```kotlin
dependencies {
    implementation(libs.buildpublish.core)

    implementation(libs.buildpublish.foundation.plugin)
    implementation(libs.buildpublish.telegram.plugin)
    implementation(libs.buildpublish.confluence.plugin)
    implementation(libs.buildpublish.nextcloud.plugin)
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
        project.pluginManager.apply("ru.kode.android.build-publish-novo.nextcloud")
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
import ru.kode.android.build.publish.plugin.core.entity.BuildVariant
import ru.kode.android.build.publish.plugin.core.entity.Tag
import ru.kode.android.build.publish.plugin.core.strategy.BuildVersionNumberNameStrategy
import ru.kode.android.build.publish.plugin.core.strategy.DEFAULT_VERSION_CODE
import ru.kode.android.build.publish.plugin.core.strategy.VersionCodeStrategy

plugins {
  id("com.android.application")
  id("ru.kode.android.build-publish-novo.foundation")
  id("ru.kode.android.build-publish-novo.telegram")
  id("ru.kode.android.build-publish-novo.confluence")
  id("ru.kode.android.build-publish-novo.nextcloud")
}

buildPublishFoundation {
  verboseLogging.set(
    providers.environmentVariable("BUILD_VERBOSE_LOGGING")
      .map { toBoolean() }
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
    commitMessageKey = "CHANGELOG"
    // Single source: no surrounding issueSources { } block needed
    issueSource("project") {
      numberPattern = "PROJECT-\\d+"
      urlPrefix = "https://jira.com/browse/"
    }
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
            if (isBlank()) {
              throw GradleException("no TELEGRAM_CHANGELOGGER_BOT_ID defined for telegram reports")
            }
            it
          }
          .orElse("")
      )

      botServerBaseUrl.set(
        providers.environmentVariable("BUILD_REPORT_TELEGRAM_BOT_BASE_URL")
          .map {
            if (isBlank()) {
              throw GradleException("no BUILD_REPORT_TELEGRAM_BOT_BASE_URL defined for telegram reports")
            }
            it
          }
          .orElse("")
      )
      botServerAuth.username.set(
        providers.environmentVariable("BUILD_REPORT_TELEGRAM_BOT_AUTH_USERNAME")
          .map {
            if (isBlank()) {
              throw GradleException("no BUILD_REPORT_TELEGRAM_BOT_AUTH_USERNAME defined for telegram reports")
            }
            it
          }
          .orElse("")
      )
      botServerAuth.password.set(
        providers.environmentVariable("BUILD_REPORT_TELEGRAM_BOT_AUTH_PASSWORD")
          .map {
            if (isBlank()) {
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
              if (isBlank()) {
                throw GradleException("no BUILD_REPORT_TELEGRAM_CHAT_ID defined for telegram reports")
              }
              it
            }
            .orElse("")
        )
        topicId.set(
          providers.environmentVariable("BUILD_REPORT_TELEGRAM_TOPIC_ID")
            .map {
              if (isBlank()) {
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
            if (isBlank()) {
              throw GradleException("no BUILD_REPORT_TELEGRAM_USER_MENTIONS defined for telegram reports")
            }
            trim().split(",").toList()
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

buildPublishNextcloud {
  auth {
    common {
      baseUrl.set("https://cloud.example.com")
      credentials.username.set(providers.environmentVariable("NEXTCLOUD_USER_NAME"))
      credentials.password.set(providers.environmentVariable("NEXTCLOUD_USER_PASSWORD"))
    }
  }

  distribution {
    common {
      remotePath.set(providers.environmentVariable("NEXTCLOUD_REMOTE_PATH"))
      compressed.set(false)
      shareMode.set(ru.kode.android.build.publish.plugin.nextcloud.config.NextcloudShareMode.INTERNAL_RECIPIENTS)
      userRecipients(providers.environmentVariable("NEXTCLOUD_USER_RECIPIENT"))
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

