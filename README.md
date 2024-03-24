# build-publish-plugin

A configurable plugin to generate changelogs from tags and publish results into Firebase App
Distribution and send changelog to Telegram and/or Slack (as you choose).

Plugin will create a full set of gradle tasks and pre-configure `versionName` and `versionCode` according to the last tag.
Tags should be formatted as `v<minor_version>.<major_version>.<version_code>-<build_type>`,
for example `v1.0.666-debug` or `v1.0.666-release`.

Several different tasks will be created for each build type and flavor. But the main ones are:
1. `processBuildPublish<build_type>` (for example, `processBuildPublishDebug`) - prepare a changelog between 2 last tags, 
   send apk to Firebase App Distribution and send a changelog to Slack and/or Telegram (if configured)
2. `sendChangelog<build_type>` (for example, `sendChangelogDebug`) - just prepare a changelog between 2 last tags 
   and send it to Slack and/or Telegram (if configured)

## How to apply

Using the plugins DSL:
```groovy
   plugins {
        id "ru.kode.android.build-publish" version "1.0.0"
   }
```
Using legacy plugin application:
```groovy
   buildscript {
      repositories {
         maven {
            url "https://plugins.gradle.org/m2/"
         }
      }
      dependencies {
        classpath "ru.kode.android:build-publish:1.0.0"
      }
   }
   apply plugin: "ru.kode.android.build-publish"
```
Using jar:
```groovy
   // Add plugin to classpath in the root gradle project
   dependencies {
    //...
    classpath files('plugin/build-publish-plugin-1.0.0.jar')
    //...
   }
    
   // Apply required plugins (com.android.application, com.google.firebase.appdistribution)
   // and this plugin (ru.kode.android.build-publish)
   plugins {
      id("com.android.application")
      id("ru.kode.android.build-publish")
   }
```
## How to configure

```kotlin
buildPublish {
    /**
     * Message key to collect interested commits
     * For exmaple: CHANGELOG
     */
    commitMessageKey.set("CHANGELOG")

    /**
     * The path to your service account private key JSON file for Firebase App Distribution
     */
    distributionServiceCredentialsFilePath.set("test-test")
    /**
     * Test groups for app distribution
     *
     * For example: [android-testers]
     */
    distributionTesterGroups.set(setOf("android-testers"))
    /**
     * Artifact type for app distribution (optional)
     * Values: APK, AAB
     */
    distributionArtifactType.set("APK")
    /**
     * Custom app id for Firebase App Distribution to override google-services.json
     */
    distributionAppId.set("ru.kode.test.app")

    /**
     * Application bundle name for changelog
     * For example: example-base-project-android
     */
    baseOutputFileName.set("example-base-project-android")

    /**
     * Address of task tracker
     * For example: "https://jira.example.ru/browse/"
     */
    issueUrlPrefix.set("https://jira.exmaple.ru/browse/")

    /**
     * How task number formatted
     * For example:  "BASE-\\d+"
     */
    issueNumberPattern.set("BASE-\\d+")

    /**
     * Config for Telegram changelog sender
     *
     * For example:
     *  bot_id: "TELEGRAM_BUILD_BOT_ID"
     *  chat_id: "CHAT_ID"
     *  topic_id: "OPTIONAL_TOPIC_ID"
     */
    tgConfig.set(
        mapOf(
            "bot_id" to "TELEGRAM_BUILD_BOT_ID",
            "chat_id" to "CHAT_ID",
            "topic_id" to "OPTIONAL_TOPIC_ID"
        )
    )

    /**
     * List of mentioning users for Telegram, can be empty or null
     * For example: ["@serega", "@valisily"]
     */
    tgUserMentions.set(
        setOf(
            "@ivan",
            "@roman",
            "@serega",
        )
    )

    /**
     * Config for Slack changelog sender
     *
     * For example:
     *  webhook_url: "https://hooks.slack.com/services/111111111/AAAAAAA/DDDDDDD"
     *  icon_url: "https://i.imgur.com/HQTF5FK.png"
     */
    slackConfig.set(
        mapOf(
            "webhook_url" to "https://hooks.slack.com/services/111111111/AAAAAAA/DDDDDDD",
            "icon_url" to "https://i.imgur.com/HQTF5FK.png",
        )
    )

    /**
     * List of mentioning users for Slack, can be empty or null
     * For example: ["@aa", "@bb", "@ccc"]
     */
    slackUserMentions.set(
        setOf(
            "@aa",
            "@bb",
            "@cc"
        )
    )
}
```
In the output plugin will set `versionName and versionCode` and create tasks to publish and send changelogs
