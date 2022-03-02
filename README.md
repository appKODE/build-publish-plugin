# build-publish-plugin

A configurable plugin to generate changelog from tags and publish results into Firebase App Distribution 
and send changelog to Telegram or/and Slack (on your choice)

## How use
1. Apply required plugins (com.android.application, com.google.firebase.appdistribution) 
   and this plugin (ru.kode.android.build-publish-plugin)
```kotlin

plugins {
    id("com.android.application")
    id("com.google.firebase.appdistribution")
    id("ru.kode.android.build-publish-plugin")
}
```
1. Add configuration for the plugin
```kotlin

buildPublish {
    // Message key to collect interested commits
    commitMessageKey.set("CHANGELOG")
    // Name of CI env variable, holding Firebase service key
    distributionServiceKey.set("FIREBASE_DISTRIBUTION_SERVICE_KEY")
    // Internal test groups for app distribution
    distributionTesterGroups.set(setOf("android-testers"))

    // Application bundle name for changelog
    baseOutputFileName.set("example-base-project-android")
    // Address of task tracker
    issueUrlPrefix.set("https://jira.exmaple.ru/browse/")
    // How task number formatted
    issueNumberPattern.set("BASE-\\d+")
    // Config for Telegram changelog sender (optional)
    tgConfig.set(
        mapOf(
            "webhook_url" to "https://api.telegram.org/%s/sendMessage?chat_id=%s&text=%s&parse_mode=MarkdownV2",
            "bot_id" to "TELEGRAM_BUILD_BOT_ID",
            "chat_id" to "CHAT_ID"
        )
    )
    // List of mentioning users for Telegram, can be empty or null
    tgUserMentions.set(
        setOf(
            "@ivan",
            "@roman",
            "@serega",
        )
    )
    // Config for Slack changelog sender (optional)
    slackConfig.set(
        mapOf(
            "webhook_url" to "https://hooks.slack.com/services/111111111/AAAAAAA/DDDDDDD",
            "icon_url" to "https://i.imgur.com/HQTF5FK.png",
        )
    )
    // List of mentioning users for Slack, can be empty or null
    slackUserMentions.set(
        setOf(
            "@aa",
            "@bb",
            "@cc"
        )
    )
}
```