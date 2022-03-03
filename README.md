# build-publish-plugin

A configurable plugin to generate changelog from tags and publish results into Firebase App
Distribution and send changelog to Telegram or/and Slack (on your choice)

## How use

1. Download jar and put somewhere, for example $PROJECT_ROOT/plugin. Skip if added remote repository
1. Add plugin to classpath in the root gradle project. If jar downloaded:

```groovy
  dependencies {
    //...
    classpath files('plugin/build-publish-plugin-1.0.0.jar')
    //...
}
```

1. Apply required plugins (com.android.application, com.google.firebase.appdistribution)
   and this plugin (ru.kode.android.build-publish)

```kotlin

plugins {
    id("com.android.application")
    id("com.google.firebase.appdistribution")
    id("ru.kode.android.build-publish")
}
```

1. Add configuration for the plugin

```kotlin
buildPublish {
    /**
     * Message key to collect interested commits
     * For exmaple: CHANGELOG
     */
    commitMessageKey.set("CHANGELOG")

    /**
     * File holding Firebase App Distribution credentials
     * For example:
     * ```
     * {
     *      "type": "service_account",
     *      "project_id": "example",
     *      "private_key_id": "0000",
     *      "private_key": "-----BEGIN PRIVATE KEY-----...-----END PRIVATE KEY-----\n",
     *      "client_email": "0000",
     *      "client_id": "0000",
     *      "auth_uri": "https://accounts.google.com/o/oauth2/auth",
     *      "token_uri": "https://oauth2.googleapis.com/token",
     *      "auth_provider_x509_cert_url": "https://www.googleapis.com/oauth2/v1/certs",
     *      "client_x509_cert_url": "https://www.googleapis.com/..."
     * }
     */
    distributionServiceCredentialsFile.set("test-test")
    /**
     * Test groups for app distribution
     *
     * For example: [android-testers]
     */
    distributionTesterGroups.set(setOf("android-testers"))

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
     *  webhook_url: "https://api.telegram.org/%s/sendMessage?chat_id=%s&text=%s&parse_mode=MarkdownV2"
     *  bot_id: "TELEGRAM_BUILD_BOT_ID"
     *  chat_id: "CHAT_ID"
     */
    tgConfig.set(
        mapOf(
            "webhook_url" to "https://api.telegram.org/%s/sendMessage?chat_id=%s&text=%s&parse_mode=MarkdownV2",
            "bot_id" to "TELEGRAM_BUILD_BOT_ID",
            "chat_id" to "CHAT_ID"
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
